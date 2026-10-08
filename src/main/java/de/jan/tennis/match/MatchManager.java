package de.jan.tennis.match;

import de.jan.tennis.TennisServer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/** Alle laufenden Matches eines Servers. */
public final class MatchManager {
	private final TennisServer ts;
	private final List<TennisMatch> matches = new ArrayList<>();

	public MatchManager(TennisServer ts) {
		this.ts = ts;
	}

	public void add(TennisMatch match) {
		matches.add(match);
	}

	public void remove(TennisMatch match) {
		matches.remove(match);
	}

	public @Nullable TennisMatch byPlayer(ServerPlayer player) {
		for (TennisMatch m : matches) {
			if (m.involves(player.getUUID())) {
				return m;
			}
		}
		return null;
	}

	public boolean courtBusy(de.jan.tennis.court.Court court) {
		return matches.stream().anyMatch(m -> m.court.equals(court));
	}

	public void tick() {
		for (TennisMatch m : List.copyOf(matches)) {
			m.tick();
		}
	}

	public void endAll() {
		for (TennisMatch m : List.copyOf(matches)) {
			m.end();
		}
	}

	public TennisServer server() {
		return ts;
	}
}
