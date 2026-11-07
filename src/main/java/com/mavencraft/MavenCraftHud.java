package com.mavencraft;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;

/** Shared MavenCraft actionbar styling for server-side feature feedback. */
public final class MavenCraftHud {
    private MavenCraftHud() {}

    public static void show(ServerPlayer player, Component message) {
        Component text = Component.literal("[")
                .append(Component.literal("MavenCraft").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.RESET))
                .append(message);
        player.connection.send(new ClientboundSetActionBarTextPacket(text));
    }

    public static Component value(String value, ChatFormatting color) {
        return Component.literal(value).withStyle(color);
    }
}
