package de.jan.tennis;

import de.jan.tennis.court.CourtManager;
import de.jan.tennis.match.MatchManager;
import de.jan.tennis.outfit.OutfitManager;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/** Hält den Zustand der Mod für den laufenden Server (bzw. die Einzelspielerwelt). */
public final class TennisServer {
	private static @Nullable TennisServer instance;

	public final MinecraftServer server;
	public final CourtManager courts;
	public final MatchManager matches;
	public final OutfitManager outfits;

	private TennisServer(MinecraftServer server) {
		this.server = server;
		this.courts = new CourtManager(server);
		this.matches = new MatchManager(this);
		this.outfits = new OutfitManager(server);
	}

	public static void start(MinecraftServer server) {
		instance = new TennisServer(server);
	}

	public static void stop() {
		if (instance != null) {
			instance.matches.endAll();
		}
		instance = null;
	}

	public static @Nullable TennisServer get() {
		return instance;
	}
}
