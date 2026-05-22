package com.mavencraft;

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
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;

public class MavenCraftClient
    implements ClientModInitializer {

  private static KeyMapping toggleKey;

  private void toggleEnabled() {

    MavenCraftRenderer.enabled = !MavenCraftRenderer.enabled;

    MavenCraftRenderer.forceRescan();

    if (!MavenCraftRenderer.enabled) {

      MavenCraftRenderer.invalidateCache();

    } else {

      MavenCraftRenderer.forceRescan();
    }
  }

  @Override

  public void onInitializeClient() {

    RadarManager.register();

    ProjectilePredictor.register();

    MavenCraftRenderer.register();

    KeyMapping.Category category = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(
            "mavencraft",
            "general"));

    toggleKey = KeyMappingHelper.registerKeyMapping(
        new KeyMapping(
            "key.mavencraft.xray_toggle",
            GLFW.GLFW_KEY_Y,
            category));

    ClientTickEvents.END_CLIENT_TICK.register(client -> {

      while (toggleKey.consumeClick()) {

        toggleEnabled();

        client.gui.setOverlayMessage(
            net.minecraft.network.chat.Component.literal(
                "[§bMavenCraft§r] X-ray "
                    + (MavenCraftRenderer.enabled
                        ? "Enabled"
                        : "Disabled")),
            false);
      }
    });

    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess) -> {

          dispatcher.register(

              LiteralArgumentBuilder
                  .<FabricClientCommandSource>literal(
                      "mavencraft")

                  .then(
                      LiteralArgumentBuilder
                          .<FabricClientCommandSource>literal(
                              "xray")

                          .then(
                              LiteralArgumentBuilder
                                  .<FabricClientCommandSource>literal(
                                      "enable")

                                  .then(
                                      RequiredArgumentBuilder
                                          .<FabricClientCommandSource, String>argument(
                                              "id",
                                              StringArgumentType.string())

                                          .executes(ctx -> {

                                            String id = StringArgumentType
                                                .getString(
                                                    ctx,
                                                    "id");

                                            MavenCraftRenderer
                                                .enableOre(id);

                                            MavenCraftRenderer
                                                .invalidateCache();

                                            MavenCraftRenderer
                                                .forceRescan();

                                            return 1;
                                          })))

                          .then(
                              LiteralArgumentBuilder
                                  .<FabricClientCommandSource>literal(
                                      "disable")

                                  .then(
                                      RequiredArgumentBuilder
                                          .<FabricClientCommandSource, String>argument(
                                              "id",
                                              StringArgumentType.string())

                                          .executes(ctx -> {

                                            String id = StringArgumentType
                                                .getString(
                                                    ctx,
                                                    "id");

                                            MavenCraftRenderer
                                                .disableOre(id);

                                            MavenCraftRenderer
                                                .invalidateCache();

                                            MavenCraftRenderer
                                                .forceRescan();

                                            return 1;
                                          })))

                          .then(
                              LiteralArgumentBuilder
                                  .<FabricClientCommandSource>literal(
                                      "only")

                                  .then(
                                      RequiredArgumentBuilder
                                          .<FabricClientCommandSource, String>argument(
                                              "id",
                                              StringArgumentType.string())

                                          .executes(ctx -> {

                                            String id = StringArgumentType
                                                .getString(
                                                    ctx,
                                                    "id");

                                            MavenCraftRenderer
                                                .onlyOre(id);

                                            MavenCraftRenderer
                                                .invalidateCache();

                                            MavenCraftRenderer
                                                .forceRescan();

                                            return 1;
                                          })))

                          .then(
                              LiteralArgumentBuilder
                                  .<FabricClientCommandSource>literal(
                                      "all")

                                  .executes(ctx -> {

                                    MavenCraftRenderer
                                        .enableAllOres();

                                    MavenCraftRenderer
                                        .invalidateCache();

                                    MavenCraftRenderer
                                        .forceRescan();

                                    return 1;
                                  }))

                          .then(
                              LiteralArgumentBuilder
                                  .<FabricClientCommandSource>literal(
                                      "none")

                                  .executes(ctx -> {

                                    MavenCraftRenderer
                                        .disableAllOres();

                                    MavenCraftRenderer
                                        .invalidateCache();

                                    MavenCraftRenderer
                                        .forceRescan();

                                    return 1;
                                  }))

                          .then(
                              LiteralArgumentBuilder
                                  .<FabricClientCommandSource>literal(
                                      "radius")

                                  .then(
                                      RequiredArgumentBuilder
                                          .<FabricClientCommandSource, Integer>argument(
                                              "blocks",
                                              IntegerArgumentType.integer(
                                                  16,
                                                  256))

                                          .executes(ctx -> {

                                            int radius = IntegerArgumentType
                                                .getInteger(
                                                    ctx,
                                                    "blocks");

                                            MavenCraftRenderer
                                                .setScanRadius(
                                                    radius);

                                            return 1;
                                          })))

                          .then(
                              LiteralArgumentBuilder
                                  .<FabricClientCommandSource>literal(
                                      "toggle")

                                  .executes(ctx -> {

                                    toggleEnabled();

                                    return 1;
                                  })))

                  .then(
                      LiteralArgumentBuilder
                          .<FabricClientCommandSource>literal(
                              "radar")

                          .then(
                              LiteralArgumentBuilder
                                  .<FabricClientCommandSource>literal(
                                      "enable")

                                  .executes(ctx -> {

                                    RadarManager.enabled = true;

                                    return 1;
                                  }))

                          .then(
                              LiteralArgumentBuilder
                                  .<FabricClientCommandSource>literal(
                                      "disable")

                                  .executes(ctx -> {

                                    RadarManager.enabled = false;

                                    return 1;
                                  })))

                  .then(
                      LiteralArgumentBuilder
                          .<FabricClientCommandSource>literal(
                              "aim")

                          .then(
                              LiteralArgumentBuilder
                                  .<FabricClientCommandSource>literal(
                                      "enable")

                                  .executes(ctx -> {

                                    ProjectilePredictor.enabled = true;

                                    return 1;
                                  }))

                          .then(
                              LiteralArgumentBuilder
                                  .<FabricClientCommandSource>literal(
                                      "disable")

                                  .executes(ctx -> {

                                    ProjectilePredictor.enabled = false;

                                    return 1;
                                  }))));
        });
  }
}
