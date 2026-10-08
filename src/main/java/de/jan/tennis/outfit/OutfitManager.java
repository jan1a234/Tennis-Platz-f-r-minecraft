package de.jan.tennis.outfit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.jan.tennis.TennisMod;
import de.jan.tennis.network.OutfitSyncPayload;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Merkt sich das Outfit jedes Spielers ({@code <welt>/tennis/outfits.json}) und schickt die Zuordnung an alle Clients.
 * Neue Spieler bekommen automatisch das nächste noch freie Outfit.
 */
public final class OutfitManager {
	private final MinecraftServer server;
	private final Path file;
	private final Map<UUID, String> outfits = new HashMap<>();

	public OutfitManager(MinecraftServer server) {
		this.server = server;
		this.file = server.getWorldPath(new LevelResource("tennis")).resolve("outfits.json");
		load();
	}

	public void onJoin(ServerPlayer player) {
		if (!outfits.containsKey(player.getUUID())) {
			outfits.put(player.getUUID(), firstFree().id);
			save();
		}
		broadcast();
	}

	public void set(ServerPlayer player, String outfit) {
		outfits.put(player.getUUID(), outfit);
		save();
		broadcast();
	}

	public String get(ServerPlayer player) {
		return outfits.getOrDefault(player.getUUID(), Outfit.NONE);
	}

	private Outfit firstFree() {
		for (Outfit o : Outfit.values()) {
			if (!outfits.containsValue(o.id)) {
				return o;
			}
		}
		return Outfit.values()[outfits.size() % Outfit.values().length];
	}

	public void broadcast() {
		OutfitSyncPayload payload = new OutfitSyncPayload(Map.copyOf(outfits));
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(p, OutfitSyncPayload.TYPE)) {
				ServerPlayNetworking.send(p, payload);
			}
		}
	}

	private void load() {
		if (!Files.exists(file)) {
			return;
		}
		try {
			JsonObject o = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			for (var e : o.entrySet()) {
				outfits.put(UUID.fromString(e.getKey()), e.getValue().getAsString());
			}
		} catch (IOException | RuntimeException e) {
			TennisMod.LOGGER.error("Konnte Outfits nicht laden", e);
		}
	}

	private void save() {
		JsonObject o = new JsonObject();
		outfits.forEach((id, outfit) -> o.addProperty(id.toString(), outfit));
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, new Gson().toJson(o), StandardCharsets.UTF_8);
		} catch (IOException e) {
			TennisMod.LOGGER.error("Konnte Outfits nicht speichern", e);
		}
	}
}
