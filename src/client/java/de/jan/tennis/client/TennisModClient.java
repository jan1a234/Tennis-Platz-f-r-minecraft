package de.jan.tennis.client;

import de.jan.tennis.network.OutfitSyncPayload;
import de.jan.tennis.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class TennisModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.TENNIS_BALL, context -> new ThrownItemRenderer<>(context, 1.0F, false));
		ClientPlayNetworking.registerGlobalReceiver(OutfitSyncPayload.TYPE, (payload, context) -> ClientOutfits.set(payload.outfits()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientOutfits.clear());
	}
}
