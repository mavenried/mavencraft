package com.mavencraft;

import java.util.ArrayDeque;
import java.util.Deque;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BreadcrumbTrail {

    private static final int MAX_MARKERS = 1024;
    private static final double BASE_MARKER_SPACING = 4.0;
    private static final double MAX_MARKER_SPACING = 32.0;
    private static final double SPACING_PER_BLOCK_PER_TICK = 8.0;
    private static final double TURN_MARKER_MIN_DISTANCE = 2.0;
    private static final double TURN_DOT_THRESHOLD = 0.85;
    private static final int TRAIL_COLOR = 0x9058DDF2;
    private static final int MARKER_FILL = 0xB838CDE8;
    private static final int MARKER_OUTLINE = 0xFFE4FCFF;
    private static final float TRAIL_WIDTH = 3.0f;
    private static final float MARKER_OUTLINE_WIDTH = 1.0f;
    private static final Deque<Marker> MARKERS = new ArrayDeque<>();

    public static boolean enabled;

    private static ClientLevel trackedLevel;
    private static Vec3 lastMarker;
    private static Vec3 previousPosition;
    private static boolean wasRecording;

    private record Marker(Vec3 position, boolean connectFromPrevious) {}

    private BreadcrumbTrail() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(BreadcrumbTrail::tick);
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> render());
    }

    public static void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        clear();
    }

    public static void clear() {
        MARKERS.clear();
        lastMarker = null;
        previousPosition = null;
        wasRecording = false;
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            trackedLevel = null;
            clear();
            return;
        }

        if (trackedLevel != client.level) {
            clear();
            trackedLevel = client.level;
        }

        Vec3 position = client.player.position();
        if (!enabled) {
            previousPosition = position;
            wasRecording = false;
            return;
        }

        if (!wasRecording || lastMarker == null || previousPosition == null) {
            addMarker(position, false);
            previousPosition = position;
            wasRecording = true;
            return;
        }

        Vec3 movement = position.subtract(previousPosition);
        double movementLengthSquared = movement.lengthSqr();
        double distanceFromLastMarkerSquared = position.distanceToSqr(lastMarker);
        double speed = client.player.getDeltaMovement().length();
        double spacing = Math.min(MAX_MARKER_SPACING, BASE_MARKER_SPACING + speed * SPACING_PER_BLOCK_PER_TICK);
        boolean reachedSpacing = distanceFromLastMarkerSquared >= spacing * spacing;
        boolean turned = false;

        if (movementLengthSquared > 1.0E-6
                && distanceFromLastMarkerSquared >= TURN_MARKER_MIN_DISTANCE * TURN_MARKER_MIN_DISTANCE) {
            Vec3 direction = movement.normalize();
            Vec3 trailDirection = position.subtract(lastMarker).normalize();
            turned = direction.dot(trailDirection) < TURN_DOT_THRESHOLD;
        }

        if (reachedSpacing || turned) {
            addMarker(position, true);
        }

        previousPosition = position;
    }

    private static void addMarker(Vec3 position, boolean connectFromPrevious) {
        MARKERS.addLast(new Marker(position, connectFromPrevious));
        lastMarker = position;
        while (MARKERS.size() > MAX_MARKERS) {
            MARKERS.removeFirst();
        }
    }

    private static void render() {
        if (!enabled) return;

        Marker previous = null;
        for (Marker marker : MARKERS) {
            Vec3 position = marker.position();
            if (previous != null && marker.connectFromPrevious()) {
                Vec3 previousTrailPoint = previous.position().add(0.0, 0.2, 0.0);
                Vec3 currentTrailPoint = position.add(0.0, 0.2, 0.0);
                Gizmos.line(previousTrailPoint, currentTrailPoint, TRAIL_COLOR, TRAIL_WIDTH)
                        .setAlwaysOnTop();
            }

            double x = position.x;
            double y = position.y + 0.2;
            double z = position.z;
            double half = 0.11;
            Gizmos.cuboid(
                            new AABB(x - half, y - half, z - half, x + half, y + half, z + half),
                            GizmoStyle.strokeAndFill(MARKER_FILL, MARKER_OUTLINE_WIDTH, MARKER_OUTLINE))
                    .setAlwaysOnTop();
            previous = marker;
        }
    }
}
