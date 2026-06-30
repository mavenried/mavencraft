package com.mavencraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class MavenCraftRenderer {

    private static int scanRadiusBlocks = 64;
    private static int scanRadiusChunks = (scanRadiusBlocks + 15) >> 4;

    private static final int SCAN_INTERVAL = 1200;
    private static final int Y_VISIBILITY_RANGE = 24;

    public static boolean enabled = true;

    private static final MavenCraftState STATE = MavenCraftState.load();

    private static final Map<String, OreColor> COLOR_MAP = new HashMap<>();

    public static final Set<String> enabledOres = new HashSet<>();

    static {
        for (OreColor oc : MavenCraftConfig.ORE_COLORS) {
            COLOR_MAP.put(oc.blockId(), oc);
        }

        enabled = STATE.enabled;

        scanRadiusBlocks = STATE.radius;

        scanRadiusChunks = (scanRadiusBlocks + 15) >> 4;

        if (STATE.ores.isEmpty()) {

            enabledOres.addAll(COLOR_MAP.keySet());

            saveState();

        } else {

            enabledOres.addAll(STATE.ores);
        }
    }

    private static final List<FoundOre> foundOres = new ArrayList<>();

    private static int ticksSinceScan = SCAN_INTERVAL;

    private static int lastScanChunkX = Integer.MIN_VALUE;
    private static int lastScanChunkZ = Integer.MIN_VALUE;
    private static int lastScanY = Integer.MIN_VALUE;

    private record FoundOre(BlockPos pos, OreColor color) {}

    private static final float STROKE_WIDTH = 2.0f;

    public static final Map<String, List<String>> ITEM_ALIAS_MAP = Map.ofEntries(
            // Overworld ores — alias covers both stone and deepslate variants
            Map.entry("coal", List.of("minecraft:coal_ore", "minecraft:deepslate_coal_ore")),
            Map.entry("iron", List.of("minecraft:iron_ore", "minecraft:deepslate_iron_ore")),
            Map.entry("copper", List.of("minecraft:copper_ore", "minecraft:deepslate_copper_ore")),
            Map.entry("gold", List.of("minecraft:gold_ore", "minecraft:deepslate_gold_ore")),
            Map.entry("redstone", List.of("minecraft:redstone_ore", "minecraft:deepslate_redstone_ore")),
            Map.entry("lapis", List.of("minecraft:lapis_ore", "minecraft:deepslate_lapis_ore")),
            Map.entry("diamond", List.of("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore")),
            Map.entry("emerald", List.of("minecraft:emerald_ore", "minecraft:deepslate_emerald_ore")),
            // Deepslate-only aliases
            Map.entry("deep_coal", List.of("minecraft:deepslate_coal_ore")),
            Map.entry("deep_iron", List.of("minecraft:deepslate_iron_ore")),
            Map.entry("deep_copper", List.of("minecraft:deepslate_copper_ore")),
            Map.entry("deep_gold", List.of("minecraft:deepslate_gold_ore")),
            Map.entry("deep_redstone", List.of("minecraft:deepslate_redstone_ore")),
            Map.entry("deep_lapis", List.of("minecraft:deepslate_lapis_ore")),
            Map.entry("deep_diamond", List.of("minecraft:deepslate_diamond_ore")),
            Map.entry("deep_emerald", List.of("minecraft:deepslate_emerald_ore")),
            // Nether ores
            Map.entry("quartz", List.of("minecraft:nether_quartz_ore")),
            Map.entry("nether_gold", List.of("minecraft:nether_gold_ore")),
            Map.entry("netherite", List.of("minecraft:ancient_debris")));

    private static List<String> resolveOreIds(String id) {
        List<String> aliased = ITEM_ALIAS_MAP.get(id.toLowerCase());
        if (aliased != null) return aliased;

        // Fallback: accept a raw block id, namespace it if needed
        if (!id.contains(":")) id = "minecraft:" + id;
        if (!id.endsWith("_ore") && !id.endsWith("_deepslate_ore")) id += "_ore";

        // Auto-pair overworld stone+deepslate when given a bare block id
        if (id.startsWith("minecraft:") && id.endsWith("_ore") && !id.contains("deepslate") && !id.contains("nether")) {
            return List.of(id, id.replace("minecraft:", "minecraft:deepslate_"));
        }

        return List.of(id);
    }

    public static void enableOre(String id) {
        for (String blockId : resolveOreIds(id)) enabledOres.add(blockId);
        saveState();
    }

    public static void disableOre(String id) {
        for (String blockId : resolveOreIds(id)) enabledOres.remove(blockId);
        saveState();
    }

    public static void enableAllOres() {
        enabledOres.clear();

        saveState();
        enabledOres.addAll(COLOR_MAP.keySet());

        saveState();
    }

    public static void disableAllOres() {
        enabledOres.clear();

        saveState();
    }

    public static void onlyOre(String id) {
        enabledOres.clear();
        for (String blockId : resolveOreIds(id)) enabledOres.add(blockId);
        saveState();
    }

    public static void setScanRadius(int blocks) {

        scanRadiusBlocks = Math.max(16, blocks);

        scanRadiusChunks = (scanRadiusBlocks + 15) >> 4;

        saveState();

        invalidateCache();
        forceRescan();
    }

    public static void forceRescan() {

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        scanChunks(mc.level, mc.player.blockPosition());
    }

    private static void saveState() {

        STATE.enabled = enabled;
        STATE.radius = scanRadiusBlocks;

        STATE.ores.clear();
        STATE.ores.addAll(enabledOres);

        STATE.save();
    }

    public static void invalidateCache() {
        ticksSinceScan = SCAN_INTERVAL;
        foundOres.clear();
    }

    public static void register() {
        LevelRenderEvents.BEFORE_GIZMOS.register(MavenCraftRenderer::onRender);
    }

    private static void onRender(LevelRenderContext ctx) {

        if (!enabled) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        int chunkX = mc.player.getBlockX() >> 4;
        int chunkZ = mc.player.getBlockZ() >> 4;
        int y = mc.player.getBlockY();

        boolean movedChunk = chunkX != lastScanChunkX || chunkZ != lastScanChunkZ;

        boolean movedY = Math.abs(y - lastScanY) >= 8;

        ticksSinceScan++;

        if (movedChunk || movedY || ticksSinceScan >= SCAN_INTERVAL) {

            lastScanChunkX = chunkX;
            lastScanChunkZ = chunkZ;
            lastScanY = y;

            ticksSinceScan = 0;

            scanChunks(mc.level, mc.player.blockPosition());
        }

        for (FoundOre ore : foundOres) {
            Gizmos.cuboid(ore.pos(), GizmoStyle.stroke(ore.color().argb(), STROKE_WIDTH))
                    .setAlwaysOnTop();
        }
    }

    private static void scanChunks(Level level, BlockPos center) {

        foundOres.clear();

        int ccx = center.getX() >> 4;
        int ccz = center.getZ() >> 4;

        int minY = level.getMinY();
        int maxY = level.getMaxY();

        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();

        for (int cx = ccx - scanRadiusChunks; cx <= ccx + scanRadiusChunks; cx++) {

            for (int cz = ccz - scanRadiusChunks; cz <= ccz + scanRadiusChunks; cz++) {

                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);

                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }

                var sections = chunk.getSections();

                for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {

                    var section = sections[sectionIndex];

                    if (section == null || section.hasOnlyAir()) {
                        continue;
                    }

                    int baseY = level.getMinY() + (sectionIndex << 4);

                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {

                            int wx = (cx << 4) + lx;
                            int wz = (cz << 4) + lz;

                            for (int ly = 0; ly < 16; ly++) {

                                int y = baseY + ly;

                                if (y < minY || y >= maxY) {
                                    continue;
                                }

                                if (Math.abs(y - center.getY()) > Y_VISIBILITY_RANGE) {

                                    continue;
                                }

                                bp.set(wx, y, wz);

                                BlockState state = chunk.getBlockState(bp);

                                Block block = state.getBlock();

                                String id =
                                        BuiltInRegistries.BLOCK.getKey(block).toString();

                                OreColor color = COLOR_MAP.get(id);

                                if (color != null && enabledOres.contains(id)) {

                                    foundOres.add(new FoundOre(bp.immutable(), color));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
