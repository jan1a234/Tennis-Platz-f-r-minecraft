package de.jan.tennis.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Zählweise nach den Tennisregeln: 0/15/30/40, Einstand und Vorteil, Spiele, Sätze und Tiebreak bei 6:6.
 * Spieler werden über den Index 0 oder 1 angesprochen.
 */
public final class TennisScore {
	public enum Result {
		POINT, GAME, SET, MATCH
	}

	private final int setsToWin;
	private final int[] points = new int[2];
	private final int[] games = new int[2];
	private final int[] sets = new int[2];
	private final List<int[]> finishedSets = new ArrayList<>();
	private boolean tiebreak;
	private int gameServer;
	private int tiebreakFirstServer;
	private int winner = -1;
	private boolean changeEnds;

	public TennisScore(int setsToWin, int firstServer) {
		this.setsToWin = Math.max(1, setsToWin);
		this.gameServer = firstServer;
	}

	/** Vergibt einen Punkt und liefert, was dadurch entschieden wurde. */
	public Result pointWonBy(int player) {
		if (winner >= 0) {
			return Result.MATCH;
		}
		changeEnds = false;
		int other = 1 - player;
		points[player]++;
		if (tiebreak) {
			int total = points[0] + points[1];
			if (points[player] >= 7 && points[player] - points[other] >= 2) {
				games[player]++;
				return finishSet(player, 1 - tiebreakFirstServer);
			}
			if (total % 6 == 0) {
				changeEnds = true;
			}
			return Result.POINT;
		}
		if (points[player] >= 4 && points[player] - points[other] >= 2) {
			games[player]++;
			points[0] = 0;
			points[1] = 0;
			gameServer = 1 - gameServer;
			if (games[player] >= 6 && games[player] - games[other] >= 2) {
				return finishSet(player, gameServer);
			}
			if (games[0] == 6 && games[1] == 6) {
				tiebreak = true;
				tiebreakFirstServer = gameServer;
			}
			changeEnds = (games[0] + games[1]) % 2 == 1;
			return Result.GAME;
		}
		return Result.POINT;
	}

	private Result finishSet(int player, int nextServer) {
		int totalGames = games[0] + games[1];
		finishedSets.add(new int[]{games[0], games[1]});
		sets[player]++;
		games[0] = 0;
		games[1] = 0;
		points[0] = 0;
		points[1] = 0;
		tiebreak = false;
		gameServer = nextServer;
		changeEnds = totalGames % 2 == 1;
		if (sets[player] >= setsToWin) {
			winner = player;
			return Result.MATCH;
		}
		return Result.SET;
	}

	/** Wer den nächsten Punkt aufschlägt. Im Tiebreak wechselt der Aufschlag nach dem ersten und dann alle zwei Punkte. */
	public int server() {
		if (!tiebreak) {
			return gameServer;
		}
		int k = points[0] + points[1];
		if (k == 0) {
			return tiebreakFirstServer;
		}
		return ((k - 1) / 2) % 2 == 0 ? 1 - tiebreakFirstServer : tiebreakFirstServer;
	}

	/** Aufschlag von rechts (Einstandsseite), wenn die Summe der Punkte im Spiel gerade ist. */
	public boolean deuceCourt() {
		return (points[0] + points[1]) % 2 == 0;
	}

	/** Ob nach dem zuletzt vergebenen Punkt die Seiten gewechselt werden. */
	public boolean changeEnds() {
		return changeEnds;
	}

	public boolean isTiebreak() {
		return tiebreak;
	}

	public int winner() {
		return winner;
	}

	public int games(int player) {
		return games[player];
	}

	public int sets(int player) {
		return sets[player];
	}

	public int points(int player) {
		return points[player];
	}

	public List<int[]> finishedSets() {
		return finishedSets;
	}

	/** Breakball, Satzball oder Matchball für den Rückschläger bzw. Spieler? Liefert eine Beschriftung oder null. */
	public String pressureLabel() {
		for (int p = 0; p < 2; p++) {
			if (wouldWinGame(p)) {
				boolean setPoint = wouldWinSetWithGame(p);
				if (setPoint && sets[p] + 1 >= setsToWin) {
					return "Matchball";
				}
				if (setPoint) {
					return "Satzball";
				}
				if (!tiebreak && p != server()) {
					return "Breakball";
				}
			}
		}
		return null;
	}

	private boolean wouldWinGame(int p) {
		int o = 1 - p;
		if (tiebreak) {
			return points[p] + 1 >= 7 && points[p] + 1 - points[o] >= 2;
		}
		return points[p] + 1 >= 4 && points[p] + 1 - points[o] >= 2;
	}

	private boolean wouldWinSetWithGame(int p) {
		if (tiebreak) {
			return true;
		}
		int g = games[p] + 1;
		return g >= 6 && g - games[1 - p] >= 2;
	}

	/** Punktestand des laufenden Spiels aus Sicht des Aufschlägers, z.B. "30:15", "Einstand", "Vorteil Jan". */
	public String gameText(String name0, String name1) {
		if (tiebreak) {
			return "Tiebreak " + points[0] + ":" + points[1];
		}
		int a = points[0];
		int b = points[1];
		if (a >= 3 && b >= 3) {
			if (a == b) {
				return "Einstand";
			}
			return "Vorteil " + (a > b ? name0 : name1);
		}
		return label(a) + ":" + label(b);
	}

	private static String label(int p) {
		return switch (p) {
			case 0 -> "0";
			case 1 -> "15";
			case 2 -> "30";
			default -> "40";
		};
	}

	/** Satzergebnisse, z.B. "6:4 3:6 2:1". */
	public String setsText() {
		StringBuilder sb = new StringBuilder();
		for (int[] s : finishedSets) {
			sb.append(s[0]).append(':').append(s[1]).append(' ');
		}
		if (winner < 0) {
			sb.append(games[0]).append(':').append(games[1]);
		}
		return sb.toString().trim();
	}
}
