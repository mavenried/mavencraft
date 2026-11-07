package com.mavencraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

/** Persists Fantastic Wings selections across death and reconnects when that optional mod is installed. */
public final class WingsPersistence {
    private static final Logger LOGGER = MavenCraftMod.LOGGER;
    private static final String UNKNOWN_WING_STATE = "\u0000";
    private static final Map<UUID, PlayerState> PLAYERS = new HashMap<>();
    private static Path file;
    private static boolean warnedAboutApi;

    private WingsPersistence() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("mavencraft-wings.properties");
            load();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save());
        ServerPlayerEvents.JOIN.register(WingsPersistence::onJoin);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> onReturn(newPlayer));
        ServerPlayerEvents.LEAVE.register(player -> save());
        ServerTickEvents.END_SERVER_TICK.register(WingsPersistence::tick);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success && isAvailable()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) announce(player);
            }
        });
    }

    private static void onJoin(ServerPlayer player) {
        if (!isAvailable()) return;
        PlayerState state = PLAYERS.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
        String current = currentWing(player);
        if (current == UNKNOWN_WING_STATE) return;
        if (current == null && state.wing != null) restore(player, state, state.wing);
        state.graceTicks = 40;
        state.removalArmed = false;
        announce(player);
    }

    private static void onReturn(ServerPlayer player) {
        if (!isAvailable()) return;
        PlayerState state = PLAYERS.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
        String current = currentWing(player);
        if (current == UNKNOWN_WING_STATE) return;
        if (current == null && state.wing != null) restore(player, state, state.wing);
        state.graceTicks = 40;
        state.removalArmed = false;
    }

    private static void tick(MinecraftServer server) {
        if (!isAvailable()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerState state = PLAYERS.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
            String current = currentWing(player);
            if (current == UNKNOWN_WING_STATE) continue;
            if (current != null) {
                if (!current.equals(state.wing)) {
                    state.wing = current;
                    MavenCraftHud.show(
                            player,
                            Component.literal("Saved ")
                                    .append(MavenCraftHud.value(
                                            displayName(current), net.minecraft.ChatFormatting.GREEN))
                                    .append(" wings."));
                    save();
                }
                if (state.graceTicks == 0) state.removalArmed = true;
            } else if (state.graceTicks == 0 && state.removalArmed && state.wing != null) {
                restore(player, state, state.wing);
                state.removalArmed = false;
                state.graceTicks = 40;
            }
            if (state.graceTicks > 0) state.graceTicks--;
        }
    }

    private static void announce(ServerPlayer player) {
        String current = currentWing(player);
        if (current == UNKNOWN_WING_STATE) return;
        MavenCraftHud.show(
                player,
                current == null
                        ? Component.literal("Active. No wings are set.")
                        : Component.literal("Active. Current wings: ")
                                .append(MavenCraftHud.value(displayName(current), net.minecraft.ChatFormatting.GREEN))
                                .append("."));
    }

    private static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("fantasticwings");
    }

    private static String currentWing(ServerPlayer player) {
        if (!isAvailable()) return null;
        try {
            return (String) Class.forName("com.mavencraft.FantasticWingsBridge")
                    .getMethod("currentWing", ServerPlayer.class)
                    .invoke(null, player);
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!warnedAboutApi) {
                warnedAboutApi = true;
                LOGGER.warn("Could not read Fantastic Wings attachment; wing persistence is disabled", e);
            }
            return UNKNOWN_WING_STATE;
        }
    }

    private static void restore(ServerPlayer player, PlayerState state, String wing) {
        if (!wing.matches("[a-z0-9_]+")) return;
        try {
            Class.forName("com.mavencraft.FantasticWingsBridge")
                    .getMethod("restore", ServerPlayer.class, String.class)
                    .invoke(null, player, wing);
            state.removalArmed = false;
            state.graceTicks = 40;
            MavenCraftHud.show(
                    player,
                    Component.literal("Restored your ")
                            .append(MavenCraftHud.value(displayName(wing), net.minecraft.ChatFormatting.GREEN))
                            .append(" wings."));
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn(
                    "Could not restore {} wings for {}",
                    wing,
                    player.getGameProfile().name(),
                    e);
        }
    }

    /** Called only after Fantastic Wings successfully applies its wing-removal consumption effect. */
    public static void onBatBloodConsumed(ServerPlayer player) {
        PlayerState state = PLAYERS.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
        state.wing = null;
        state.removalArmed = false;
        state.graceTicks = 40;
        save();
        MavenCraftHud.show(
                player, Component.literal("You have forsaken the skies.").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    private static String displayName(String id) {
        return id.replace('_', ' ');
    }

    private static synchronized void load() {
        PLAYERS.clear();
        if (file == null || !Files.isRegularFile(file)) return;
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
            for (String key : properties.stringPropertyNames()) {
                try {
                    String value = properties.getProperty(key);
                    if (value.isEmpty() || value.matches("[a-z0-9_]+")) {
                        PlayerState state = new PlayerState();
                        state.wing = value.isEmpty() ? null : value;
                        PLAYERS.put(UUID.fromString(key), state);
                    }
                } catch (IllegalArgumentException ignored) {
                    LOGGER.warn("Ignoring invalid Fantastic Wings tracking entry for {}", key);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read Fantastic Wings tracking data from {}", file, e);
        }
    }

    private static synchronized void save() {
        if (file == null) return;
        Properties properties = new Properties();
        PLAYERS.forEach((uuid, state) -> properties.setProperty(uuid.toString(), state.wing == null ? "" : state.wing));
        try {
            Files.createDirectories(file.getParent());
            try (var writer = Files.newBufferedWriter(file)) {
                properties.store(writer, "MavenCraft Fantastic Wings selections");
            }
        } catch (IOException e) {
            LOGGER.warn("Could not save Fantastic Wings tracking data to {}", file, e);
        }
    }

    private static final class PlayerState {
        private String wing;
        private int graceTicks;
        private boolean removalArmed;
    }
}
