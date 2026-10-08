package de.jan.tennis.logic;

/**
 * Platzbeläge mit realistischen Absprungwerten.
 * <p>
 * {@code restitution}: vertikaler Stoßkoeffizient (ITF: Ball aus 2,54 m springt 1,35–1,47 m hoch, also etwa 0,73–0,76).
 * {@code friction}: wie viel horizontale Geschwindigkeit nach dem Aufsprung erhalten bleibt (Rasen schnell, Sand langsam).
 */
public enum Surface {
	RASEN("rasen", "Rasen", 0.70, 0.82),
	SAND("sand", "Sand", 0.79, 0.66),
	HARTPLATZ("hartplatz", "Hartplatz", 0.75, 0.74);

	public final String id;
	public final String displayName;
	public final double restitution;
	public final double friction;

	Surface(String id, String displayName, double restitution, double friction) {
		this.id = id;
		this.displayName = displayName;
		this.restitution = restitution;
		this.friction = friction;
	}

	public static Surface byId(String id) {
		for (Surface s : values()) {
			if (s.id.equalsIgnoreCase(id)) {
				return s;
			}
		}
		return HARTPLATZ;
	}
}
