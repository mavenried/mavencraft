package com.mavencraft.mixin;

import com.mavencraft.WingsPersistence;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clears the saved wing only when Fantastic Wings successfully applies its consume-to-remove effect. */
@Pseudo
@Mixin(targets = "fuzs.fantasticwings.common.world.item.consume_effects.TakeWingsConsumeEffect")
public abstract class FantasticWingsTakeBloodMixin {
    @Inject(method = "apply", at = @At("RETURN"))
    private void mavencraft$clearSavedWingsAfterBlood(
            Level level, ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && entity instanceof ServerPlayer player) {
            WingsPersistence.onBatBloodConsumed(player);
        }
    }
}
