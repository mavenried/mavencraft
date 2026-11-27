package com.mavencraft.mixin.client;

import com.mavencraft.DamageIndicators;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void mavencraft$damageIndicator(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        DamageIndicators.onDamageEvent(packet);
    }
}
