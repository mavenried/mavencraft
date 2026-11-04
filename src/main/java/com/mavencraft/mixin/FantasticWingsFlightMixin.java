package com.mavencraft.mixin;

import com.mavencraft.FlightMultiplierConfig;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Increases Fantastic Wings' forward-flight acceleration while the player is sprinting and providing
 * forward input. Normal wing flight, non-sprinting flight, and sideways or
 * backward movement retain their original acceleration.
 */
@Pseudo
@Mixin(targets = "fuzs.fantasticwings.common.flight.Flight")
public abstract class FantasticWingsFlightMixin {

    @ModifyArgs(
            method = "onWornUpdate",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;",
                            ordinal = 0),
            require = 1)
    private void mavencraft$boostHorizontalSprintFlightAcceleration(Args args, Player player) {
        if (!player.isSprinting() || player.zza <= 0.0F) return;

        double multiplier = FlightMultiplierConfig.getMultiplier();
        args.set(0, (double) args.get(0) * multiplier);
        args.set(2, (double) args.get(2) * multiplier);
    }
}
