package com.mavencraft;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;

import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;

import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ProjectilePredictor {

    public static boolean enabled = true;

    private static final int STEPS = 120;


    private ProjectilePredictor() {
    }

    public static void register() {

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> tick());
    }

    private static void tick() {

        ProjectilePath.POINTS.clear();

        if (!enabled) {
            return;
        }

        ProjectilePath.IMPACT = null;

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            return;
        }

        var stack =
                mc.player.getMainHandItem();

        boolean supported =
                stack.getItem() instanceof BowItem
                || stack.getItem() instanceof net.minecraft.world.item.CrossbowItem
                || stack.getItem() instanceof SnowballItem
                || stack.getItem() instanceof EnderpearlItem
                || stack.getItem() instanceof ThrowablePotionItem
                || stack.getItem() instanceof ExperienceBottleItem;

        if (!supported) {
            return;
        }

        if (stack.getItem() instanceof BowItem
                && !mc.player.isUsingItem()) {
            return;
        }

        if (stack.getItem() instanceof net.minecraft.world.item.CrossbowItem
                && !net.minecraft.world.item.CrossbowItem.isCharged(stack)) {
            return;
        }

        Vec3 pos =
                mc.player
                        .getEyePosition()
                        .add(
                                mc.player
                                        .getLookAngle()
                                        .scale(
                                                stack.getItem() instanceof net.minecraft.world.item.CrossbowItem
                                                        ? 0.28
                                                        : 0.16));

        Vec3 velocity =
                mc.player
                        .getLookAngle()
                        .normalize();

        if (stack.getItem() instanceof BowItem) {

            int useTicks =
                    mc.player.getTicksUsingItem();

            int charge =
                    useTicks;

            float t =
                    charge / 20.0f;

            float power =
                    (t * t + t * 2.0f)
                    / 3.0f;

            power =
                    Math.min(power, 1.0f);

            velocity =
                    velocity.scale(
                            power * 3.0f);

        } else if (stack.getItem() instanceof net.minecraft.world.item.CrossbowItem) {

            velocity =
                    velocity.scale(3.15);

            Vec3 move =
                    mc.player.getDeltaMovement();

            velocity =
                    velocity.add(
                            move.x * 0.15,
                            0.0,
                            move.z * 0.15);

        } else {

            velocity =
                    velocity.scale(1.5);
        }

        for (int i = 0; i < STEPS; i++) {

            ProjectilePath.POINTS.add(pos);

            Vec3 next =
                    pos.add(velocity);

            HitResult hit =
                    mc.level.clip(
                            new net.minecraft.world.level.ClipContext(
                                    pos,
                                    next,
                                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                                    mc.player));

            if (hit.getType() != HitResult.Type.MISS) {

                ProjectilePath.POINTS.add(
                        hit.getLocation());

                ProjectilePath.IMPACT =
                        hit.getLocation();

                break;
            }

            pos = next;

            if (stack.getItem() instanceof BowItem) {

                velocity =
                        velocity.scale(0.99);

                velocity =
                        velocity.add(
                                0.0,
                                -0.05,
                                0.0);

            } else {

                velocity =
                        velocity.scale(0.99);

                velocity =
                        velocity.add(
                                0.0,
                                -0.03,
                                0.0);
            }
        }
    }
}
