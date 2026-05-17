package com.mavencraft;

import java.util.List;

/**
 * Central color configuration for highlighted ores.
 *
 * Each entry maps a fully-qualified block ID to an ARGB highlight color.
 * Adjust alpha (last argument) to control visibility — 180 is a good balance
 * between visibility and not being completely opaque through solid rock.
 *
 * To add modded ores, just append entries to ORE_COLORS.
 */
public final class MavenCraftConfig {

  private MavenCraftConfig() {
  }

  /** Alpha shared across all default entries (0-255). */
  private static final int A = 200;

  public static final List<OreColor> ORE_COLORS = List.of(
      // Overworld ores
      OreColor.of("minecraft:coal_ore", 50, 50, 50, A),
      OreColor.of("minecraft:deepslate_coal_ore", 70, 70, 70, A),
      OreColor.of("minecraft:iron_ore", 210, 150, 100, A),
      OreColor.of("minecraft:deepslate_iron_ore", 190, 130, 90, A),
      OreColor.of("minecraft:copper_ore", 210, 110, 50, A),
      OreColor.of("minecraft:deepslate_copper_ore", 190, 90, 40, A),
      OreColor.of("minecraft:gold_ore", 255, 220, 0, A),
      OreColor.of("minecraft:deepslate_gold_ore", 230, 200, 0, A),
      OreColor.of("minecraft:redstone_ore", 220, 20, 20, A),
      OreColor.of("minecraft:deepslate_redstone_ore", 200, 10, 10, A),
      OreColor.of("minecraft:lapis_ore", 30, 80, 200, A),
      OreColor.of("minecraft:deepslate_lapis_ore", 20, 60, 180, A),
      OreColor.of("minecraft:diamond_ore", 80, 230, 230, A),
      OreColor.of("minecraft:deepslate_diamond_ore", 50, 210, 210, A),
      OreColor.of("minecraft:emerald_ore", 30, 210, 80, A),
      OreColor.of("minecraft:deepslate_emerald_ore", 20, 190, 60, A),

      // Nether ores
      OreColor.of("minecraft:nether_gold_ore", 255, 200, 20, A),
      OreColor.of("minecraft:nether_quartz_ore", 230, 230, 210, A),
      OreColor.of("minecraft:ancient_debris", 130, 70, 50, A));
}
