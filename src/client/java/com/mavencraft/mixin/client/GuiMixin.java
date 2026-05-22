package com.mavencraft.mixin.client;

import com.mavencraft.RadarHudRenderer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

  @Inject(method = "extractRenderState", at = @At("TAIL"))
  private void mavencraft$renderRadar(
      GuiGraphicsExtractor ctx,
      DeltaTracker deltaTracker,
      CallbackInfo ci) {

    ctx.nextStratum();
    RadarHudRenderer.render(ctx);
  }
}
