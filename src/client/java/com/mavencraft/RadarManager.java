package com.mavencraft;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RadarManager {

  public static final List<RadarArc> ARCS = new ArrayList<>();

  private static final List<Threat> THREATS = new ArrayList<>();

  public static boolean enabled = true;

  public static float range = 48.0f;

  public static int maxThreats = 5;

  public static float smoothing = 0.18f;

  private RadarManager() {
  }

  public static void register() {

    ClientTickEvents.END_CLIENT_TICK.register(
        client -> tick());
  }

  private static void tick() {

    if (!enabled) {
      ARCS.clear();
      return;
    }

    Minecraft mc = Minecraft.getInstance();

    if (mc.player == null || mc.level == null) {
      return;
    }

    THREATS.clear();
    ARCS.clear();

    List<Entity> enemies = mc.level.getEntities(
        mc.player,
        new AABB(
            mc.player.blockPosition())
            .inflate(range),
        entity -> entity instanceof Enemy
            && entity.isAlive());

    Vec3 playerPos = mc.player.position();

    float yaw = mc.player.getYRot();

    double yawRad = Math.toRadians(yaw);

    double sin = Math.sin(yawRad);
    double cos = Math.cos(yawRad);

    for (Entity entity : enemies) {

      if (!entity.isAlive()) {
        continue;
      }

      Vec3 delta = entity.position().subtract(playerPos);

      double localX = -(delta.x * cos + delta.z * sin);

      double localZ = delta.z * cos - delta.x * sin;

      float angle = (float) Math.atan2(localX, localZ) + (float)org.joml.Math.PI_OVER_2;

      float dist = (float) delta.length();

      THREATS.add(
          new Threat(
              angle,
              dist));
    }

    THREATS.sort(
        Comparator.comparingDouble(
            t -> t.distance));

    THREATS.sort(
        Comparator.comparingDouble(
            t -> t.smoothedAngle));

    for (Threat threat : THREATS) {

      threat.smoothedAngle = Mth.rotLerp(
          smoothing,
          threat.smoothedAngle,
          threat.targetAngle);

      float width = 0.10f
          + (1.0f
              - (threat.distance / range))
              * 0.35f;

      float alpha = 1.0f
          - (threat.distance / range);

      float size = 2.0f
          + (1.0f - (threat.distance / range))
              * 5.0f;

      ARCS.add(
          new RadarArc(
              threat.smoothedAngle,
              size,
              alpha,
              threat.distance));
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

  private static final class Threat {

    float targetAngle;

    float smoothedAngle;

    float distance;

    Threat(
        float angle,
        float distance) {

      this.targetAngle = angle;
      this.smoothedAngle = angle;
      this.distance = distance;
    }
  }
}
