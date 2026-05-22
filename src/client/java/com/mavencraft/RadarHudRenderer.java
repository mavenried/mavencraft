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

        int color = ((int) (alpha * 255.0f) << 24) | 0xFF4040;

        float forwardX = Mth.sin(angle);

        float forwardY = -Mth.cos(angle);

        float rightX = -forwardY;

        float rightY = forwardX;

        float tipX = px + forwardX * size * 0.55f;

        float tipY = py + forwardY * size * 0.55f;

        float leftX = px - forwardX * size * 0.45f + rightX * size * 0.45f;

        float leftY = py - forwardY * size * 0.45f + rightY * size * 0.45f;

        float rightPX = px - forwardX * size * 0.45f - rightX * size * 0.45f;

        float rightPY = py - forwardY * size * 0.45f - rightY * size * 0.45f;

        drawLine(ctx, (int) leftX, (int) leftY, (int) tipX, (int) tipY, color);

        drawLine(ctx, (int) rightPX, (int) rightPY, (int) tipX, (int) tipY, color);
    }

    private static void drawLine(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color) {

        int dx = Math.abs(x2 - x1);

        int dy = Math.abs(y2 - y1);

        int sx = x1 < x2 ? 1 : -1;

        int sy = y1 < y2 ? 1 : -1;

        int err = dx - dy;

        while (true) {

            ctx.fill(x1, y1, x1 + 1, y1 + 1, color);

            if (x1 == x2 && y1 == y2) {
                break;
            }

            int e2 = err * 2;

            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }

            if (e2 < dx) {
                err += dx;
                y1 += sy;
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
