package com.mavencraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

/** Server-wide toggle for creeper terrain damage, persisted in the world's data directory. */
public final class CreeperBlockDamage {
    private static final Logger LOGGER = MavenCraftMod.LOGGER;
    private static final String FILE_NAME = "mavencraft-creeper.properties";

    public enum Mode {
        VANILLA,
        NO_BLOCK_DAMAGE,
        NO_DAMAGE
    }

    private static volatile Mode mode = Mode.NO_BLOCK_DAMAGE;
    private static Path file;

    private CreeperBlockDamage() {}

    public static Mode getMode() {
        return mode;
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE_NAME);
            mode = load(file);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save());
    }

    public static void registerCommand(
            com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
            net.minecraft.commands.CommandBuildContext registryAccess,
            Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("mavencraft-server")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("creepers")
                        .then(Commands.literal("vanilla").executes(context -> set(context.getSource(), Mode.VANILLA)))
                        .then(Commands.literal("no-block-damage")
                                .executes(context -> set(context.getSource(), Mode.NO_BLOCK_DAMAGE)))
                        .then(Commands.literal("no-damage")
                                .executes(context -> set(context.getSource(), Mode.NO_DAMAGE)))
                        .then(Commands.literal("status").executes(context -> {
                            showMode(context.getSource().getEntity(), false);
                            return 1;
                        }))));
    }

    private static int set(CommandSourceStack source, Mode value) {
        mode = value;
        save();
        showMode(source.getEntity(), true);
        return 1;
    }

    private static void showMode(net.minecraft.world.entity.Entity entity, boolean changed) {
        if (!(entity instanceof ServerPlayer player)) return;
        String label = switch (mode) {
            case VANILLA -> "Vanilla explosions";
            case NO_BLOCK_DAMAGE -> "No block damage";
            case NO_DAMAGE -> "No damage";
        };
        ChatFormatting color = switch (mode) {
            case VANILLA -> ChatFormatting.GREEN;
            case NO_BLOCK_DAMAGE -> ChatFormatting.GOLD;
            case NO_DAMAGE -> ChatFormatting.RED;
        };
        Component message = Component.literal(changed ? "Creeper mode set: " : "Creeper mode: ")
                .append(MavenCraftHud.value(label, color));
        MavenCraftHud.show(player, message);
    }

    private static Mode load(Path path) {
        if (path == null || !Files.isRegularFile(path)) return Mode.NO_BLOCK_DAMAGE;
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path)) {
            properties.load(reader);
            return Mode.valueOf(properties.getProperty("mode", "NO_BLOCK_DAMAGE"));
        } catch (IOException e) {
            LOGGER.warn("Could not read creeper block damage setting from {}", path, e);
            return Mode.NO_BLOCK_DAMAGE;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ignoring invalid creeper explosion mode in {}", path, e);
            return Mode.NO_BLOCK_DAMAGE;
        }
    }

    private static synchronized void save() {
        if (file == null) return;
        Properties properties = new Properties();
        properties.setProperty("mode", mode.name());
        try {
            Files.createDirectories(file.getParent());
            try (var writer = Files.newBufferedWriter(file)) {
                properties.store(writer, "MavenCraft creeper block damage");
            }
        } catch (IOException e) {
            LOGGER.warn("Could not save creeper block damage setting to {}", file, e);
        }
    }
}
