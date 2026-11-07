package com.mavencraft;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.resources.Identifier;

public class MavenCraftClient implements ClientModInitializer {

    private static KeyMapping toggleKey;
    private static KeyMapping breadcrumbToggleKey;

    private void toggleEnabled() {
        MavenCraftRenderer.setEnabled(!MavenCraftRenderer.enabled);
    }

    @Override
    public void onInitializeClient() {

        RadarManager.register();

        ProjectilePredictor.register();

        MavenCraftRenderer.register();

        BreadcrumbTrail.register();

        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("mavencraft", "general"));

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mavencraft.xray_toggle", 89, category));
        breadcrumbToggleKey =
                KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mavencraft.breadcrumb_toggle", 66, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {

                toggleEnabled();

                showHud(client, "X-ray " + (MavenCraftRenderer.enabled ? "Enabled" : "Disabled"),
                        MavenCraftRenderer.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
            }

            while (breadcrumbToggleKey.consumeClick()) {
                BreadcrumbTrail.setEnabled(!BreadcrumbTrail.enabled);
                showHud(client, "Breadcrumbs " + (BreadcrumbTrail.enabled ? "Enabled" : "Disabled"),
                        BreadcrumbTrail.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("mavencraft")
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("xray")
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("enable")
                                    .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument(
                                                    "id", StringArgumentType.string())
                                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                    MavenCraftRenderer.ITEM_ALIAS_MAP.keySet(), builder))
                                            .executes(ctx -> {
                                                String id = StringArgumentType.getString(ctx, "id");

                                                MavenCraftRenderer.enableOre(id);

                                                MavenCraftRenderer.invalidateCache();

                                                MavenCraftRenderer.forceRescan();
                                                showHud(ctx.getSource().getClient(), "X-ray enabled for " + id, ChatFormatting.GREEN);

                                                return 1;
                                            })))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("disable")
                                    .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument(
                                                    "id", StringArgumentType.string())
                                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                    MavenCraftRenderer.ITEM_ALIAS_MAP.keySet(), builder))
                                            .executes(ctx -> {
                                                String id = StringArgumentType.getString(ctx, "id");

                                                MavenCraftRenderer.disableOre(id);

                                                MavenCraftRenderer.invalidateCache();

                                                MavenCraftRenderer.forceRescan();
                                                showHud(ctx.getSource().getClient(), "X-ray disabled for " + id, ChatFormatting.RED);

                                                return 1;
                                            })))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("only")
                                    .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument(
                                                    "id", StringArgumentType.string())
                                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                    MavenCraftRenderer.ITEM_ALIAS_MAP.keySet(), builder))
                                            .executes(ctx -> {
                                                String id = StringArgumentType.getString(ctx, "id");

                                                MavenCraftRenderer.onlyOre(id);

                                                MavenCraftRenderer.invalidateCache();

                                                MavenCraftRenderer.forceRescan();
                                                showHud(ctx.getSource().getClient(), "Showing only " + id, ChatFormatting.GOLD);

                                                return 1;
                                            })))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("all")
                                    .executes(ctx -> {
                                        MavenCraftRenderer.enableAllOres();

                                        MavenCraftRenderer.invalidateCache();

                                        MavenCraftRenderer.forceRescan();
                                        showHud(ctx.getSource().getClient(), "Showing all ores", ChatFormatting.GREEN);

                                        return 1;
                                    }))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("none")
                                    .executes(ctx -> {
                                        MavenCraftRenderer.disableAllOres();

                                        MavenCraftRenderer.invalidateCache();

                                        MavenCraftRenderer.forceRescan();
                                        showHud(ctx.getSource().getClient(), "Showing no ores", ChatFormatting.RED);

                                        return 1;
                                    }))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("radius")
                                    .then(RequiredArgumentBuilder.<FabricClientCommandSource, Integer>argument(
                                                    "blocks", IntegerArgumentType.integer(16, 256))
                                            .executes(ctx -> {
                                                int radius = IntegerArgumentType.getInteger(ctx, "blocks");

                                                MavenCraftRenderer.setScanRadius(radius);
                                                showHud(ctx.getSource().getClient(), "X-ray radius set to " + radius + " blocks", ChatFormatting.AQUA);

                                                return 1;
                                            })))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("toggle")
                                    .executes(ctx -> {
                                        toggleEnabled();
                                        showHud(ctx.getSource().getClient(), "X-ray " + (MavenCraftRenderer.enabled ? "Enabled" : "Disabled"),
                                                MavenCraftRenderer.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);

                                        return 1;
                                    })))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("radar")
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("enable")
                                    .executes(ctx -> {
                                        RadarManager.enabled = true;
                                        showHud(ctx.getSource().getClient(), "Radar Enabled", ChatFormatting.GREEN);

                                        return 1;
                                    }))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("disable")
                                    .executes(ctx -> {
                                        RadarManager.enabled = false;
                                        showHud(ctx.getSource().getClient(), "Radar Disabled", ChatFormatting.RED);

                                        return 1;
                                    })))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("aim")
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("enable")
                                    .executes(ctx -> {
                                        ProjectilePredictor.enabled = true;
                                        showHud(ctx.getSource().getClient(), "Projectile aim Enabled", ChatFormatting.GREEN);

                                        return 1;
                                    }))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("disable")
                                    .executes(ctx -> {
                                        ProjectilePredictor.enabled = false;
                                        showHud(ctx.getSource().getClient(), "Projectile aim Disabled", ChatFormatting.RED);

                                        return 1;
                                    })))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("flight")
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("multiplier")
                                    .executes(ctx -> {
                                        showHud(ctx.getSource().getClient(), "Sprint flight multiplier: "
                                                + FlightMultiplierConfig.getMultiplier() + "x", ChatFormatting.AQUA);
                                        return 1;
                                    })
                                    .then(RequiredArgumentBuilder.<FabricClientCommandSource, Float>argument(
                                                    "value",
                                                    FloatArgumentType.floatArg(
                                                            FlightMultiplierConfig.MIN_MULTIPLIER,
                                                            FlightMultiplierConfig.MAX_MULTIPLIER))
                                            .executes(ctx -> {
                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                FlightMultiplierConfig.setMultiplier(value);
                                                showHud(ctx.getSource().getClient(), "Sprint flight multiplier set to "
                                                        + value + "x", ChatFormatting.GREEN);
                                                return 1;
                                            }))))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("creepers")
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("vanilla")
                                    .executes(ctx -> sendCreeperCommand(ctx.getSource(), "vanilla")))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("no-block-damage")
                                    .executes(ctx -> sendCreeperCommand(ctx.getSource(), "no-block-damage")))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("no-damage")
                                    .executes(ctx -> sendCreeperCommand(ctx.getSource(), "no-damage")))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("status")
                                    .executes(ctx -> sendCreeperCommand(ctx.getSource(), "status"))))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("breadcrumbs")
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("enable")
                                    .executes(ctx -> {
                                        BreadcrumbTrail.setEnabled(true);
                                        showHud(ctx.getSource().getClient(), "Breadcrumbs Enabled", ChatFormatting.GREEN);
                                        return 1;
                                    }))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("disable")
                                    .executes(ctx -> {
                                        BreadcrumbTrail.setEnabled(false);
                                        showHud(ctx.getSource().getClient(), "Breadcrumbs Disabled", ChatFormatting.RED);
                                        return 1;
                                    }))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("toggle")
                                    .executes(ctx -> {
                                        BreadcrumbTrail.setEnabled(!BreadcrumbTrail.enabled);
                                        showHud(ctx.getSource().getClient(), "Breadcrumbs "
                                                + (BreadcrumbTrail.enabled ? "Enabled" : "Disabled"),
                                                BreadcrumbTrail.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
                                        return 1;
                                    }))
                            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("clear")
                                    .executes(ctx -> {
                                        BreadcrumbTrail.clear();
                                        showHud(ctx.getSource().getClient(), "Breadcrumbs cleared", ChatFormatting.AQUA);
                                        return 1;
                                    }))));
        });
    }

    private static int sendCreeperCommand(FabricClientCommandSource source, String mode) {
        var connection = source.getClient().getConnection();
        if (connection != null) {
            connection.getConnection().send(new ServerboundChatCommandPacket("mavencraft-server creepers " + mode));
        }
        return 1;
    }

    private static void showHud(net.minecraft.client.Minecraft client, String message, ChatFormatting color) {
        Component text = Component.literal("[")
                .append(Component.literal("MavenCraft").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.RESET))
                .append(Component.literal(message).withStyle(color));
        client.gui.hud.setOverlayMessage(text, false);
    }
}
