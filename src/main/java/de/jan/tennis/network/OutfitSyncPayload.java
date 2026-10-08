package de.jan.tennis.network;

import de.jan.tennis.TennisMod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → Client: welcher Spieler welches Outfit trägt. */
public record OutfitSyncPayload(Map<UUID, String> outfits) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OutfitSyncPayload> TYPE = new CustomPacketPayload.Type<>(TennisMod.id("outfits"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OutfitSyncPayload> CODEC =
		ByteBufCodecs.<RegistryFriendlyByteBuf, UUID, String, Map<UUID, String>>map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.STRING_UTF8)
			.map(OutfitSyncPayload::new, OutfitSyncPayload::outfits);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
