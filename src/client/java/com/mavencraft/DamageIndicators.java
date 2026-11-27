package com.mavencraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DamageIndicators {
    private static final long POPUP_DURATION_NANOS = 500_000_000L;
    private static final List<Popup> POPUPS = new ArrayList<>();
    private static final List<PendingDamage> PENDING = new ArrayList<>();
    private static final Map<Integer, Float> KNOWN_HEALTH = new HashMap<>();
    private static ClientLevel trackedLevel;

    private record Popup(Vec3 position, float damage, long createdAtNanos) {}
    private record PendingDamage(LivingEntity target, float healthBefore, int age) {}

    private DamageIndicators() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(DamageIndicators::tick);
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> render());
    }

    public static void onDamageEvent(ClientboundDamageEventPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || packet.sourceCauseId() != client.player.getId()) {
            return;
        }
        if (client.level.getEntity(packet.entityId()) instanceof LivingEntity target && target != client.player) {
            float healthBefore = KNOWN_HEALTH.getOrDefault(target.getId(), target.getHealth());
            PENDING.add(new PendingDamage(target, healthBefore, 0));
        }
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            POPUPS.clear();
            PENDING.clear();
            KNOWN_HEALTH.clear();
            trackedLevel = null;
            return;
        }
        if (trackedLevel != client.level) {
            POPUPS.clear();
            PENDING.clear();
            KNOWN_HEALTH.clear();
            trackedLevel = client.level;
        }

        long now = System.nanoTime();
        POPUPS.removeIf(popup -> now - popup.createdAtNanos() >= POPUP_DURATION_NANOS);

        List<PendingDamage> remaining = new ArrayList<>();
        Set<LivingEntity> resolvedTargets = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (PendingDamage pending : PENDING) {
            LivingEntity target = pending.target();
            if (resolvedTargets.contains(target) || target.level() != client.level || pending.age() >= 8) {
                continue;
            }
            if (target.getHealth() < pending.healthBefore()) {
                float damage = pending.healthBefore() - target.getHealth();
                ThreadLocalRandom random = ThreadLocalRandom.current();
                Vec3 position = target.position().add(
                        random.nextDouble(-0.3, 0.3),
                        target.getBbHeight() + random.nextDouble(0.15, 0.5),
                        random.nextDouble(-0.3, 0.3));
                POPUPS.add(new Popup(position, damage, now));
                resolvedTargets.add(target);
            } else {
                remaining.add(new PendingDamage(target, pending.healthBefore(), pending.age() + 1));
            }
        }
        PENDING.clear();
        PENDING.addAll(remaining);

        for (var entity : client.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living) {
                KNOWN_HEALTH.put(living.getId(), living.getHealth());
            }
        }
    }

    private static void render() {
        long now = System.nanoTime();
        for (Popup popup : POPUPS) {
            float progress = Math.min(1.0f, (float) (now - popup.createdAtNanos()) / POPUP_DURATION_NANOS);
            int alpha = Math.max(0, Math.min(255, (int) (255 * (1.0f - progress))));
            int color = (alpha << 24) | MavenCraftRenderer.getDamageColor();
            int outlineColor = (alpha << 24) | MavenCraftRenderer.getDamageOutlineColor();
            Vec3 position = popup.position().add(0.0, progress * 0.8, 0.0);
            Gizmos.billboardText(formatDamage(popup.damage()), position,
                            TextGizmo.Style.forColorAndCentered(outlineColor).withScale(0.76f))
                    .setAlwaysOnTop();
            Gizmos.billboardText(formatDamage(popup.damage()), position,
                            TextGizmo.Style.forColorAndCentered(color).withScale(0.65f))
                    .setAlwaysOnTop();
        }
    }

    private static String formatDamage(float damage) {
        return Integer.toString(Math.round(damage));
    }
}
