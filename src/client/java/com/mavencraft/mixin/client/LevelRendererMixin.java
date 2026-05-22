package com.mavencraft.mixin.client;

import com.mavencraft.ProjectilePath;

import net.minecraft.client.renderer.LevelRenderer;

import net.minecraft.gizmos.Gizmos;

import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
            method = "collectPerFrameGizmos",
            at = @At("TAIL"))
    private void mavencraft$trajectory(
            CallbackInfoReturnable<Gizmos.TemporaryCollection> cir) {

        for (int i = 1; i < ProjectilePath.POINTS.size(); i++) {

            Vec3 p1 =
                    ProjectilePath.POINTS.get(i - 1);

            Vec3 p2 =
                    ProjectilePath.POINTS.get(i);

            Gizmos.line(
                    p1,
                    p2,
                    0xFFFFFFFF,
                    2.0f);
        }

        if (ProjectilePath.IMPACT != null) {
            Vec3 c =
                    ProjectilePath.IMPACT;

            double s = 0.20;

            Gizmos.line(
                    c.add(-s, 0.0, 0.0),
                    c.add( s, 0.0, 0.0),
                    0xFFFF3030,
                    3.0f);

            Gizmos.line(
                    c.add(0.0, -s, 0.0),
                    c.add(0.0,  s, 0.0),
                    0xFFFF3030,
                    3.0f);

            Gizmos.line(
                    c.add(0.0, 0.0, -s),
                    c.add(0.0, 0.0,  s),
                    0xFFFF3030,
                    3.0f);

        }
    }
}
