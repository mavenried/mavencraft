package com.mavencraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class RadarHudRenderer {

    private static final float RADIUS = 42.0f;

    private RadarHudRenderer() {}

    public static void render(GuiGraphicsExtractor ctx) {

        if (!RadarManager.enabled) {
            return;
        }

        float cx = ctx.guiWidth() * 0.5f;

        float cy = ctx.guiHeight() * 0.5f;

        for (RadarArc arc : RadarManager.ARCS) {

            float angle = wrap(arc.angle - Mth.HALF_PI);

            float px = cx + Mth.sin(angle) * (RADIUS - arc.width);

            float py = cy - Mth.cos(angle) * (RADIUS - arc.width);

            renderChevron(ctx, px, py, angle, arc.width, arc.alpha);
        }
    }

    private static void renderChevron(
            GuiGraphicsExtractor ctx, float px, float py, float angle, float size, float alpha) {

        int color = ((int) (alpha * 255.0f) << 24) | MavenCraftRenderer.getRadarColor();

        float forwardX = Mth.sin(angle);

        float forwardY = -Mth.cos(angle);

        float rightX = -forwardY;

        float rightY = forwardX;
        float strokeWidth = Mth.clamp(size * 0.18f, 0.9f, 1.5f);

        float tipX = forwardX * size * 0.55f;
        float tipY = forwardY * size * 0.55f;
        float backX = -forwardX * size * 0.45f;
        float backY = -forwardY * size * 0.45f;
        float wingX = rightX * size * 0.45f;
        float wingY = rightY * size * 0.45f;

        ctx.pose().pushMatrix();
        ctx.pose().translate(px, py);
        float leftX = backX + wingX;
        float leftY = backY + wingY;
        float rightPX = backX - wingX;
        float rightPY = backY - wingY;

        drawArm(ctx, leftX, leftY, tipX, tipY, strokeWidth, color);
        drawArm(ctx, rightPX, rightPY, tipX, tipY, strokeWidth, color);
        drawCap(ctx, leftX, leftY, strokeWidth * 0.5f, color);
        drawCap(ctx, rightPX, rightPY, strokeWidth * 0.5f, color);
        drawCap(ctx, tipX, tipY, strokeWidth * 0.5f, color);
        ctx.pose().popMatrix();
    }

    private static void drawArm(
            GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, float strokeWidth, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = Mth.sqrt(dx * dx + dy * dy);
        float angle = (float) Mth.atan2(dy, dx);

        ctx.pose().pushMatrix();
        ctx.pose().translate((x1 + x2) * 0.5f, (y1 + y2) * 0.5f);
        ctx.pose().rotate(angle);
        ctx.pose().scale(length / 12.0f, strokeWidth * 0.5f);
        ctx.fill(-6, -1, 6, 1, color);
        ctx.pose().popMatrix();
    }

    private static void drawCap(GuiGraphicsExtractor ctx, float x, float y, float radius, int color) {
        int minX = Mth.floor(x - radius - 0.5f);
        int maxX = Mth.ceil(x + radius - 0.5f);
        int minY = Mth.floor(y - radius - 0.5f);
        int maxY = Mth.ceil(y + radius - 0.5f);
        int sourceAlpha = color >>> 24;
        int rgb = color & 0x00FFFFFF;
        final int samples = 4;
        final int sampleCount = samples * samples;

        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                int inside = 0;
                for (int sy = 0; sy < samples; sy++) {
                    for (int sx = 0; sx < samples; sx++) {
                        float sampleX = px + (sx + 0.5f) / samples;
                        float sampleY = py + (sy + 0.5f) / samples;
                        float dx = sampleX - x;
                        float dy = sampleY - y;
                        if (dx * dx + dy * dy <= radius * radius) {
                            inside++;
                        }
                    }
                }
                if (inside > 0) {
                    int alpha = sourceAlpha * inside / sampleCount;
                    ctx.fill(px, py, px + 1, py + 1, (alpha << 24) | rgb);
                }
            }
        }
    }

    private static float wrap(float angle) {

        while (angle > Math.PI) {
            angle -= (float) (Math.PI * 2.0);
        }

        while (angle < -Math.PI) {
            angle += (float) (Math.PI * 2.0);
        }

        return angle;
    }
}
