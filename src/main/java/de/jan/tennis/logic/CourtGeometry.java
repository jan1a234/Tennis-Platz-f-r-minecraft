package de.jan.tennis.logic;

/**
 * Maße des Platzes in Platzkoordinaten.
 * <p>
 * {@code u} läuft entlang der Längsachse (Netz bei u = 0), {@code v} quer dazu. Eine Hälfte wird über
 * ihr Vorzeichen {@code side} (-1 oder +1) angesprochen. Die Linien sind einen Block breit und zählen zum Feld,
 * damit die Regeln genau dem entsprechen, was man im Spiel sieht.
 * <pre>
 *   Grundlinie   |u| = 11,5 … 12,5   (Platzlänge 25 Blöcke, echt 23,77 m)
 *   T-Linie      |u| =  6,5 …  7,5
 *   Einzel-Seitenlinie |v| = 3,5 … 4,5   (9 Blöcke, echt 8,23 m)
 *   Doppel-Seitenlinie |v| = 5,5 … 6,5
 *   Mittellinie  |v| ≤ 0,5
 * </pre>
 */
public final class CourtGeometry {
	public static final double HALF_LENGTH = 12.5;
	public static final double BASELINE_INNER = 11.5;
	public static final double SERVICE_LINE = 7.5;
	public static final double SERVICE_LINE_INNER = 6.5;
	public static final double SINGLES_HALF_WIDTH = 4.5;
	public static final double SINGLES_LINE_INNER = 3.5;
	public static final double DOUBLES_HALF_WIDTH = 6.5;
	public static final double DOUBLES_LINE_INNER = 5.5;
	public static final double CENTER_LINE_HALF = 0.5;
	public static final double NET_HALF_THICKNESS = 0.5;
	public static final double NET_POST = 7.5;
	public static final double AREA_HALF_LENGTH = 18.5;
	public static final double AREA_HALF_WIDTH = 10.5;
	/** Ball berührt die Linie, solange sein Rand noch auf ihr liegt. */
	public static final double TOLERANCE = BallPhysics.BALL_RADIUS;

	private CourtGeometry() {
	}

	/** Liegt der Aufsprung im Einzelfeld der Hälfte {@code side}? */
	public static boolean inSinglesHalf(double u, double v, int side) {
		double depth = side * u;
		return depth > 0 && depth <= HALF_LENGTH + TOLERANCE && Math.abs(v) <= SINGLES_HALF_WIDTH + TOLERANCE;
	}

	/**
	 * Liegt der Aufsprung im Aufschlagfeld?
	 *
	 * @param receiverSide Hälfte des Rückschlägers
	 * @param boxSign      Vorzeichen von v im Zielfeld
	 */
	public static boolean inServiceBox(double u, double v, int receiverSide, int boxSign) {
		double depth = receiverSide * u;
		double width = boxSign * v;
		return depth > 0 && depth <= SERVICE_LINE + TOLERANCE
			&& width >= -CENTER_LINE_HALF - TOLERANCE && width <= SINGLES_HALF_WIDTH + TOLERANCE;
	}

	/**
	 * Auf welcher Seite der Mittellinie (Vorzeichen von v) der Aufschläger steht.
	 * Von rechts (Einstandsseite) aus gesehen in Blickrichtung zum Netz.
	 */
	public static int serverHalfSign(int serverSide, boolean deuceCourt) {
		return deuceCourt ? -serverSide : serverSide;
	}

	/** Zielfeld liegt diagonal gegenüber. */
	public static int targetBoxSign(int serverSide, boolean deuceCourt) {
		return -serverHalfSign(serverSide, deuceCourt);
	}

	/** Steht der Aufschläger hinter der Grundlinie und auf der richtigen Seite der Mittelmarkierung? */
	public static boolean validServePosition(double u, double v, int serverSide, boolean deuceCourt) {
		double depth = serverSide * u;
		double width = serverHalfSign(serverSide, deuceCourt) * v;
		return depth >= HALF_LENGTH - 0.3 && depth <= AREA_HALF_LENGTH && width >= 0.2 && width <= DOUBLES_HALF_WIDTH + 1.0;
	}

	/** Ist der Punkt noch innerhalb der eingezäunten Anlage? */
	public static boolean inArea(double u, double v) {
		return Math.abs(u) <= AREA_HALF_LENGTH + 0.5 && Math.abs(v) <= AREA_HALF_WIDTH + 0.5;
	}
}
