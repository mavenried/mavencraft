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

    private static String normalizeOreId(String id) {

        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }

        if (!id.endsWith("_ore") && !id.endsWith("_deepslate_ore")) {

            id += "_ore";
        }

        return id;
    }

    public static void enableOre(String id) {

        String normalized = normalizeOreId(id);

        enabledOres.add(normalized);

        saveState();

        if (normalized.startsWith("minecraft:") && normalized.endsWith("_ore") && !normalized.contains("deepslate")) {

            enabledOres.add(normalized.replace("minecraft:", "minecraft:deepslate_"));
        }
    }

    public static void disableOre(String id) {

        String normalized = normalizeOreId(id);

        enabledOres.remove(normalized);

        saveState();

        if (normalized.startsWith("minecraft:") && normalized.endsWith("_ore") && !normalized.contains("deepslate")) {

            enabledOres.remove(normalized.replace("minecraft:", "minecraft:deepslate_"));
        }
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

        saveState();
        enableOre(id);
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
