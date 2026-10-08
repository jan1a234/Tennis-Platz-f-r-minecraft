package de.jan.tennis.match;

import de.jan.tennis.Msg;
import de.jan.tennis.TennisServer;
import de.jan.tennis.court.Court;
import de.jan.tennis.entity.BallListener;
import de.jan.tennis.entity.TennisBallEntity;
import de.jan.tennis.item.ShotMaker;
import de.jan.tennis.logic.CourtGeometry;
import de.jan.tennis.logic.TennisScore;
import de.jan.tennis.registry.ModSounds;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ein Einzel zwischen zwei Spielern mit Schiedsrichter: Aufschlagfelder, erster und zweiter Aufschlag,
 * Netzaufschlag (Let), Aus, Doppelfehler, Ass, Seitenwechsel, Tiebreak und Statistik.
 */
public class TennisMatch implements BallListener {
	private enum Phase {
		WAIT_SERVE, TOSSED, SERVE_FLIGHT, RALLY, POINT_OVER, FINISHED
	}

	private static final int GREEN = 0x33FF55;
	private static final int RED = 0xFF3333;

	private final TennisServer ts;
	public final Court court;
	private final UUID[] ids = new UUID[2];
	private final String[] names = new String[2];
	private final int[] side = {-1, 1};
	private final TennisScore score;
	private final ServerBossEvent bossBar;

	private Phase phase = Phase.WAIT_SERVE;
	private int serveNumber = 1;
	private @Nullable TennisBallEntity ball;
	private int lastHitter = -1;
	private int bounces;
	private boolean netTouched;
	private int rallyLength;
	private int delay;
	private boolean resetPositions = true;
	private int ticks;

	private final int[] aces = new int[2];
	private final int[] doubleFaults = new int[2];
	private final int[] fastestServe = new int[2];
	private final int[] pointsWon = new int[2];
	private int longestRally;

	public TennisMatch(TennisServer ts, Court court, ServerPlayer a, ServerPlayer b, int setsToWin, int firstServer) {
		this.ts = ts;
		this.court = court;
		ids[0] = a.getUUID();
		ids[1] = b.getUUID();
		names[0] = a.getGameProfile().name();
		names[1] = b.getGameProfile().name();
		this.score = new TennisScore(setsToWin, firstServer);
		this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal("Tennis"), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
		bossBar.setProgress(1.0F);
		bossBar.addPlayer(a);
		bossBar.addPlayer(b);
		String format = switch (setsToWin) {
			case 1 -> "einen Gewinnsatz";
			case 2 -> "zwei Gewinnsätze";
			default -> "drei Gewinnsätze";
		};
		for (ServerPlayer p : players()) {
			Msg.title(p, "Spiel beginnt!", names[0] + " gegen " + names[1], ChatFormatting.GOLD);
			Msg.chat(p, "§6[Tennis] §fMatch auf " + court.name() + " (" + court.surface().displayName + ") über " + format + ". §7"
				+ names[firstServer] + " schlägt zuerst auf.");
		}
		updateBossBar();
	}

	// ------------------------------------------------------------------ Ablauf

	public void tick() {
		ticks++;
		if (phase == Phase.FINISHED) {
			return;
		}
		ServerPlayer a = player(0);
		ServerPlayer b = player(1);
		if (a == null || b == null) {
			ServerPlayer left = a == null ? b : a;
			if (left != null) {
				Msg.chat(left, "§6[Tennis] §fDein Gegner hat das Spiel verlassen. Das Match ist beendet.");
			}
			end();
			return;
		}
		if (delay > 0 && --delay == 0) {
			if (phase == Phase.POINT_OVER) {
				setupPoint();
			}
		}
		if (phase == Phase.WAIT_SERVE && ticks % 20 == 0) {
			ServerPlayer server = player(score.server());
			if (server != null) {
				showServeSpot(server);
			}
		}
	}

	private void setupPoint() {
		discardBall();
		phase = Phase.WAIT_SERVE;
		lastHitter = -1;
		bounces = 0;
		netTouched = false;
		rallyLength = 0;
		int s = score.server();
		int r = 1 - s;
		boolean deuce = score.deuceCourt();
		if (resetPositions) {
			int half = CourtGeometry.serverHalfSign(side[s], deuce);
			teleport(player(s), court.world(side[s] * 13.6, half * 2.2, 0));
			teleport(player(r), court.world(side[r] * 13.0, -half * 3.0, 0));
			resetPositions = false;
		}
		String which = serveNumber == 1 ? "1. Aufschlag" : "2. Aufschlag";
		String from = deuce ? "von rechts" : "von links";
		for (ServerPlayer p : players()) {
			Msg.actionBar(p, "§e" + names[s] + " §f– " + which + " " + from);
		}
		updateBossBar();
	}

	private void teleport(@Nullable ServerPlayer player, Vec3 pos) {
		if (player == null) {
			return;
		}
		int idx = indexOf(player);
		player.teleportTo((ServerLevel) player.level(), pos.x, court.groundY(), pos.z, Set.of(), court.yawTowardNet(side[idx]), 0.0F, false);
	}

	private void showServeSpot(ServerPlayer server) {
		int s = score.server();
		int half = CourtGeometry.serverHalfSign(side[s], score.deuceCourt());
		Vec3 spot = court.world(side[s] * 13.0, half * 2.2, court.groundY() + 0.1);
		((ServerLevel) server.level()).sendParticles(server, new DustParticleOptions(0xFFFF55, 1.2F), true, false, spot.x, spot.y, spot.z, 6, 0.3, 0.0, 0.3, 0);
	}

	// ------------------------------------------------------------------ Aufschlag und Schläge

	/** Spieler drückt Rechtsklick ohne Ball in Reichweite. */
	public void tryToss(ServerPlayer player) {
		int idx = indexOf(player);
		if (phase != Phase.WAIT_SERVE && phase != Phase.TOSSED) {
			return;
		}
		if (idx != score.server()) {
			Msg.actionBar(player, "§7Aufschlag hat §e" + names[score.server()]);
			return;
		}
		if (phase == Phase.TOSSED && ball != null && !ball.isRemoved()) {
			return;
		}
		boolean deuce = score.deuceCourt();
		if (!CourtGeometry.validServePosition(court.u(player.position()), court.v(player.position()), side[idx], deuce)) {
			Msg.actionBar(player, "§cStell dich hinter die Grundlinie, " + (deuce ? "rechts" : "links") + " von der Mitte (gelbe Markierung)");
			showServeSpot(player);
			return;
		}
		discardBall();
		ball = ShotMaker.toss(player, court);
		ball.setListener(this);
		phase = Phase.TOSSED;
	}

	/** Darf der Spieler diesen Ball jetzt schlagen? */
	public boolean mayHit(ServerPlayer player, TennisBallEntity target) {
		if (target != ball) {
			return target.getListener() == null || !(target.getListener() instanceof TennisMatch);
		}
		int idx = indexOf(player);
		return switch (phase) {
			case TOSSED -> idx == score.server();
			case SERVE_FLIGHT -> idx != lastHitter;
			case RALLY -> idx != lastHitter;
			default -> false;
		};
	}

	public void onShot(ServerPlayer player, ShotMaker.ShotType type, int kmh) {
		int idx = indexOf(player);
		if (type == ShotMaker.ShotType.SERVE) {
			fastestServe[idx] = Math.max(fastestServe[idx], kmh);
			ServerPlayer other = player(1 - idx);
			if (other != null) {
				Msg.actionBar(other, "§e" + names[idx] + " §fschlägt auf: §e" + kmh + " km/h");
			}
		}
	}

	@Override
	public void onHit(TennisBallEntity hitBall, ServerPlayer player) {
		if (hitBall != ball) {
			return;
		}
		int idx = indexOf(player);
		switch (phase) {
			case TOSSED -> {
				phase = Phase.SERVE_FLIGHT;
				lastHitter = idx;
				bounces = 0;
				netTouched = false;
				rallyLength = 1;
			}
			case SERVE_FLIGHT -> {
				// Rückschläger nimmt den Aufschlag direkt aus der Luft: Punkt für den Aufschläger
				pointTo(lastHitter, "Volley-Return", "Der Aufschlag muss erst aufspringen");
			}
			case RALLY -> {
				lastHitter = idx;
				bounces = 0;
				netTouched = false;
				rallyLength++;
			}
			default -> {
			}
		}
	}

	@Override
	public void onBounce(TennisBallEntity bouncing, Vec3 pos) {
		if (bouncing != ball) {
			return;
		}
		double u = court.u(pos);
		double v = court.v(pos);
		switch (phase) {
			case TOSSED -> {
				for (ServerPlayer p : players()) {
					Msg.actionBar(p, "§7Ballwurf wiederholen");
				}
				discardBall();
				phase = Phase.WAIT_SERVE;
			}
			case SERVE_FLIGHT -> {
				int s = lastHitter;
				boolean deuce = score.deuceCourt();
				boolean in = CourtGeometry.inServiceBox(u, v, side[1 - s], CourtGeometry.targetBoxSign(side[s], deuce));
				mark(pos, in);
				if (in && netTouched) {
					let();
				} else if (in) {
					phase = Phase.RALLY;
					bounces = 1;
				} else {
					fault(netTouched && Math.signum(u) == side[s] ? "Netz" : "Aus");
				}
			}
			case RALLY -> {
				int hitter = lastHitter;
				int opp = 1 - hitter;
				if (bounces == 0) {
					boolean in = CourtGeometry.inSinglesHalf(u, v, side[opp]);
					mark(pos, in);
					if (in) {
						bounces = 1;
					} else if (Math.signum(u) == side[hitter]) {
						pointTo(opp, "Netz", null);
					} else {
						pointTo(opp, "Aus!", null);
					}
				} else {
					boolean ace = rallyLength == 1;
					if (ace) {
						aces[hitter]++;
					}
					pointTo(hitter, ace ? "Ass!" : "Winner!", null);
				}
			}
			default -> {
			}
		}
	}

	@Override
	public void onNetTouch(TennisBallEntity touched) {
		if (touched == ball) {
			netTouched = true;
		}
	}

	@Override
	public void onObstacle(TennisBallEntity hitBall) {
		if (hitBall != ball) {
			return;
		}
		if (phase == Phase.SERVE_FLIGHT) {
			fault("Aus");
		} else if (phase == Phase.RALLY) {
			if (bounces == 0) {
				pointTo(1 - lastHitter, "Aus!", null);
			} else {
				pointTo(lastHitter, "Winner!", null);
			}
		}
	}

	@Override
	public void onDead(TennisBallEntity deadBall) {
		if (deadBall != ball) {
			return;
		}
		switch (phase) {
			case TOSSED -> phase = Phase.WAIT_SERVE;
			case SERVE_FLIGHT -> fault("Netz");
			case RALLY -> {
				if (bounces == 0) {
					pointTo(1 - lastHitter, netTouched ? "Netz" : "Aus!", null);
				} else {
					pointTo(lastHitter, "Winner!", null);
				}
			}
			default -> {
			}
		}
	}

	// ------------------------------------------------------------------ Entscheidungen

	private void let() {
		for (ServerPlayer p : players()) {
			Msg.title(p, "Netz!", "Aufschlag wiederholen", ChatFormatting.YELLOW);
		}
		finishRally(30);
	}

	private void fault(String reason) {
		int s = score.server();
		if (serveNumber == 1) {
			serveNumber = 2;
			for (ServerPlayer p : players()) {
				Msg.title(p, "Fehler", reason + " – zweiter Aufschlag", ChatFormatting.RED);
			}
			finishRally(35);
		} else {
			doubleFaults[s]++;
			pointTo(1 - s, "Doppelfehler", null);
		}
	}

	private void pointTo(int winner, String call, @Nullable String detail) {
		if (phase == Phase.POINT_OVER || phase == Phase.FINISHED) {
			return;
		}
		pointsWon[winner]++;
		longestRally = Math.max(longestRally, rallyLength);
		serveNumber = 1;
		TennisScore.Result result = score.pointWonBy(winner);
		String headline = call;
		String sub = (detail != null ? detail + " – " : "") + "Punkt " + names[winner];
		ChatFormatting color = ChatFormatting.WHITE;
		boolean applause = call.startsWith("Ass") || rallyLength >= 8;
		switch (result) {
			case GAME -> {
				headline = "Spiel " + names[winner];
				sub = call + " – Spiele " + score.games(0) + ":" + score.games(1);
				color = ChatFormatting.GOLD;
				applause = true;
			}
			case SET -> {
				headline = "Satz " + names[winner];
				sub = "Sätze " + score.sets(0) + ":" + score.sets(1) + " (" + score.setsText() + ")";
				color = ChatFormatting.GOLD;
				applause = true;
			}
			case MATCH -> {
				headline = "Spiel, Satz und Sieg!";
				sub = names[winner] + " gewinnt " + score.setsText();
				color = ChatFormatting.GOLD;
				applause = true;
			}
			default -> {
				String pressure = score.pressureLabel();
				sub = sub + " (" + score.gameText(names[0], names[1]) + ")" + (pressure != null ? " – " + pressure : "");
			}
		}
		for (ServerPlayer p : players()) {
			Msg.title(p, headline, sub, color);
		}
		if (applause) {
			ServerPlayer p = player(winner);
			if (p != null) {
				p.level().playSound(null, court.cx(), court.groundY() + 2, court.cz(), ModSounds.APPLAUSE, SoundSource.PLAYERS, 1.0F, 1.0F);
			}
		}
		if (result == TennisScore.Result.MATCH) {
			phase = Phase.FINISHED;
			showStats(winner);
			releaseBall();
			end();
			return;
		}
		if (score.changeEnds()) {
			side[0] = -side[0];
			side[1] = -side[1];
			for (ServerPlayer p : players()) {
				Msg.chat(p, "§6[Tennis] §fSeitenwechsel!");
			}
		}
		resetPositions = true;
		finishRally(60);
		updateBossBar();
	}

	private void finishRally(int delayTicks) {
		phase = Phase.POINT_OVER;
		delay = delayTicks;
		releaseBall();
	}

	private void mark(Vec3 pos, boolean in) {
		if (ball != null && ball.level() instanceof ServerLevel level) {
			level.sendParticles(new DustParticleOptions(in ? GREEN : RED, 1.6F), pos.x, pos.y + 0.05, pos.z, 14, 0.08, 0.02, 0.08, 0);
		}
	}

	private void showStats(int winner) {
		for (ServerPlayer p : players()) {
			Msg.chat(p, "§6§l[Tennis] Matchstatistik §7(" + score.setsText() + ")");
			for (int i = 0; i < 2; i++) {
				Msg.chat(p, "§e" + names[i] + (i == winner ? " §6🏆" : "") + "§f: " + pointsWon[i] + " Punkte, " + aces[i] + " Asse, "
					+ doubleFaults[i] + " Doppelfehler, schnellster Aufschlag " + fastestServe[i] + " km/h");
			}
			Msg.chat(p, "§7Längster Ballwechsel: " + longestRally + " Schläge");
		}
	}

	private void updateBossBar() {
		int s = score.server();
		String serveMark0 = s == 0 ? "§e● " : "";
		String serveMark1 = s == 1 ? " §e●" : "";
		String text = serveMark0 + "§f" + names[0] + "  §b" + score.gameText(names[0], names[1]) + "  §f" + names[1] + serveMark1
			+ "   §7Spiele §f" + score.games(0) + ":" + score.games(1)
			+ "   §7Sätze §f" + score.sets(0) + ":" + score.sets(1);
		bossBar.setName(Component.literal(text));
	}

	// ------------------------------------------------------------------ Verwaltung

	public void forfeit(ServerPlayer player) {
		int idx = indexOf(player);
		for (ServerPlayer p : players()) {
			Msg.title(p, names[1 - idx] + " gewinnt", names[idx] + " gibt auf", ChatFormatting.GOLD);
		}
		end();
	}

	public void end() {
		phase = Phase.FINISHED;
		discardBall();
		bossBar.removeAllPlayers();
		ts.matches.remove(this);
	}

	private void releaseBall() {
		if (ball != null) {
			ball.setListener(null);
		}
	}

	private void discardBall() {
		if (ball != null) {
			ball.setListener(null);
			if (!ball.isRemoved()) {
				ball.discard();
			}
			ball = null;
		}
	}

	public String describe() {
		return names[0] + " " + score.gameText(names[0], names[1]) + " " + names[1] + " | Spiele " + score.games(0) + ":" + score.games(1)
			+ " | Sätze " + score.setsText();
	}

	public boolean involves(UUID id) {
		return ids[0].equals(id) || ids[1].equals(id);
	}

	public int indexOf(ServerPlayer player) {
		return ids[0].equals(player.getUUID()) ? 0 : 1;
	}

	private @Nullable ServerPlayer player(int idx) {
		ServerPlayer p = ts.server.getPlayerList().getPlayer(ids[idx]);
		return p != null && !p.isRemoved() ? p : null;
	}

	private java.util.List<ServerPlayer> players() {
		java.util.List<ServerPlayer> list = new java.util.ArrayList<>(2);
		for (int i = 0; i < 2; i++) {
			ServerPlayer p = player(i);
			if (p != null) {
				list.add(p);
			}
		}
		return list;
	}
}
