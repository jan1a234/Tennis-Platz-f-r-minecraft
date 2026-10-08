package de.jan.tennis.court;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.jan.tennis.TennisMod;
import de.jan.tennis.logic.Surface;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

/** Speichert alle Plätze einer Welt in {@code <welt>/tennis/courts.json}. */
public final class CourtManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final List<Court> courts = new ArrayList<>();
	private final Path file;

	public CourtManager(MinecraftServer server) {
		this.file = server.getWorldPath(new LevelResource("tennis")).resolve("courts.json");
		load();
	}

	public List<Court> all() {
		return courts;
	}

	public void add(Court court) {
		courts.removeIf(c -> c.name().equals(court.name()));
		courts.add(court);
		save();
	}

	public boolean remove(Court court) {
		boolean removed = courts.remove(court);
		save();
		return removed;
	}

	/** Platz, auf dem die Position liegt, oder der nächste im Umkreis. */
	public Optional<Court> find(Level level, Vec3 pos, double maxDistance) {
		Court best = null;
		double bestDist = maxDistance * maxDistance;
		for (Court c : courts) {
			if (!c.isIn(level)) {
				continue;
			}
			if (c.contains(level, pos)) {
				return Optional.of(c);
			}
			double d = c.distanceSqr(pos);
			if (d < bestDist) {
				bestDist = d;
				best = c;
			}
		}
		return Optional.ofNullable(best);
	}

	public String nextName() {
		int i = 1;
		while (true) {
			String name = "Platz " + i;
			if (courts.stream().noneMatch(c -> c.name().equals(name))) {
				return name;
			}
			i++;
		}
	}

	private void load() {
		courts.clear();
		if (!Files.exists(file)) {
			return;
		}
		try {
			JsonArray array = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonArray();
			for (var element : array) {
				JsonObject o = element.getAsJsonObject();
				courts.add(new Court(o.get("name").getAsString(), o.get("dimension").getAsString(), o.get("cx").getAsDouble(),
					o.get("groundY").getAsInt(), o.get("cz").getAsDouble(), o.get("alongX").getAsBoolean(), Surface.byId(o.get("surface").getAsString())));
			}
		} catch (IOException | RuntimeException e) {
			TennisMod.LOGGER.error("Konnte Tennisplätze nicht laden", e);
		}
	}

	private void save() {
		JsonArray array = new JsonArray();
		for (Court c : courts) {
			JsonObject o = new JsonObject();
			o.addProperty("name", c.name());
			o.addProperty("dimension", c.dimension());
			o.addProperty("cx", c.cx());
			o.addProperty("groundY", c.groundY());
			o.addProperty("cz", c.cz());
			o.addProperty("alongX", c.alongX());
			o.addProperty("surface", c.surface().id);
			array.add(o);
		}
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(array), StandardCharsets.UTF_8);
		} catch (IOException e) {
			TennisMod.LOGGER.error("Konnte Tennisplätze nicht speichern", e);
		}
	}
}
