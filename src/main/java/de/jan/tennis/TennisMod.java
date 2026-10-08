package de.jan.tennis;

import de.jan.tennis.command.TennisCommands;
import de.jan.tennis.network.OutfitSyncPayload;
import de.jan.tennis.registry.ModEntities;
import de.jan.tennis.registry.ModItems;
import de.jan.tennis.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TennisMod implements ModInitializer {
	public static final String MOD_ID = "tennis";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModSounds.init();
		ModItems.init();
		ModEntities.init();
		PayloadTypeRegistry.clientboundPlay().register(OutfitSyncPayload.TYPE, OutfitSyncPayload.CODEC);

		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> TennisCommands.register(dispatcher));
		ServerLifecycleEvents.SERVER_STARTED.register(TennisServer::start);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> TennisServer.stop());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			TennisServer ts = TennisServer.get();
			if (ts != null) {
				ts.matches.tick();
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			TennisServer ts = TennisServer.get();
			if (ts != null) {
				ts.outfits.onJoin(handler.getPlayer());
			}
		});
		LOGGER.info("Tennisplatz geladen – Spiel, Satz und Sieg!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
