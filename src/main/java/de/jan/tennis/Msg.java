package de.jan.tennis;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/** Kleine Helfer für Meldungen an Spieler. */
public final class Msg {
	private Msg() {
	}

	public static void actionBar(ServerPlayer player, String text) {
		player.sendOverlayMessage(Component.literal(text));
	}

	public static void chat(ServerPlayer player, String text) {
		player.sendSystemMessage(Component.literal(text));
	}

	public static void title(ServerPlayer player, String title, String subtitle, ChatFormatting color) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(4, 30, 8));
		player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle).withStyle(ChatFormatting.WHITE)));
		player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(color, ChatFormatting.BOLD)));
	}
}
