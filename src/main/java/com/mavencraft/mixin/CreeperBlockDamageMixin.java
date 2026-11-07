package com.mavencraft.mixin;

import com.mavencraft.CreeperBlockDamage;
import com.mavencraft.MavenCraftHud;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperBlockDamageMixin {
    @Inject(method = "explodeCreeper", at = @At("HEAD"))
    private void mavencraft$announcePreventedDamage(CallbackInfo ci) {
        CreeperBlockDamage.Mode mode = CreeperBlockDamage.getMode();
        if (mode == CreeperBlockDamage.Mode.VANILLA) return;

        Creeper creeper = (Creeper) (Object) this;
        if (!(creeper.level() instanceof ServerLevel level)) return;

        boolean noDamage = mode == CreeperBlockDamage.Mode.NO_DAMAGE;
        Component message = Component.literal(
                        noDamage
                                ? "Creeper explosion prevented: no damage"
                                : "Creeper explosion prevented: no block damage")
                .withStyle(noDamage ? ChatFormatting.RED : ChatFormatting.GOLD);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(creeper) <= 32.0 * 32.0) {
                MavenCraftHud.show(player, message);
            }
        }
    }

    @ModifyArg(
            method = "explodeCreeper",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"),
            index = 4)
    private float mavencraft$disableAllExplosionDamage(float radius) {
        return CreeperBlockDamage.getMode() == CreeperBlockDamage.Mode.NO_DAMAGE ? 0.0F : radius;
    }

    @ModifyArg(
            method = "explodeCreeper",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"),
            index = 5)
    private Level.ExplosionInteraction mavencraft$disableBlockDamage(Level.ExplosionInteraction interaction) {
        return CreeperBlockDamage.getMode() == CreeperBlockDamage.Mode.VANILLA
                ? interaction
                : Level.ExplosionInteraction.NONE;
    }
}
