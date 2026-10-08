package de.jan.tennis.logic;

/**
 * Physik eines Tennisballs in Minecraft-Einheiten (1 Block = 1 m, 1 Tick = 1/20 s).
 * <p>
 * Berücksichtigt Schwerkraft, Luftwiderstand (c_w ≈ 0,55) und den Magnus-Effekt durch Spin.
 * Ein Ball (57 g, Ø 6,7 cm) ergibt k = ½·ρ·c_w·A/m ≈ 0,0204 pro Meter; in Block/Tick-Einheiten bleibt der Wert gleich.
 */
public final class BallPhysics {
	/** 9,81 m/s² in Blöcken pro Tick². */
	public static final double GRAVITY = 9.81 / 400.0;
	/** Luftwiderstand: Geschwindigkeitsverlust pro Tick = DRAG · v². */
	public static final double DRAG = 0.0204;
	/** Magnus-Faktor: Beschleunigung = MAGNUS · |v| · (ω × v), |ω| entspricht dem Auftriebsbeiwert. */
	public static final double MAGNUS = 0.0371;
	/** Spin baut sich in der Luft langsam ab. */
	public static final double SPIN_DECAY = 0.996;
	public static final double BALL_RADIUS = 0.0335;
	/** Umrechnung Blöcke/Tick → km/h. */
	public static final double TO_KMH = 20.0 * 3.6;

	private BallPhysics() {
	}

	/** Beschleunigung für einen Tick. */
	public static V3 acceleration(V3 vel, V3 spin) {
		double speed = vel.length();
		V3 drag = vel.scale(-DRAG * speed);
		V3 magnus = spin.cross(vel).scale(MAGNUS * speed);
		return drag.add(magnus).add(new V3(0, -GRAVITY, 0));
	}

	/** Neue Geschwindigkeit nach einem Tick (semi-implizites Euler-Verfahren). */
	public static V3 stepVelocity(V3 vel, V3 spin) {
		return vel.add(acceleration(vel, spin));
	}

	/** Topspin-Achse für eine horizontale Flugrichtung (Magnus drückt den Ball nach unten). */
	public static V3 topspinAxis(V3 direction) {
		return V3.UP.cross(direction.horizontal().normalize());
	}

	/** Anteil Topspin (positiv) oder Unterschnitt (negativ) relativ zur Flugrichtung. */
	public static double topspinComponent(V3 vel, V3 spin) {
		V3 axis = topspinAxis(vel);
		return spin.dot(axis);
	}

	/**
	 * Geschwindigkeit nach einem Bodenkontakt.
	 * Topspin lässt den Ball nach vorne "springen", Slice bleibt flach und wird langsamer.
	 */
	public static V3 bounce(V3 vel, V3 spin, Surface surface) {
		double top = topspinComponent(vel, spin);
		double restitution = surface.restitution * (1.0 + 0.25 * top);
		double friction = surface.friction * (1.0 + 0.6 * top);
		restitution = Math.clamp(restitution, 0.45, 0.9);
		friction = Math.clamp(friction, 0.45, 0.97);
		double vy = -vel.y() * restitution;
		if (vy < 0.035) {
			vy = 0; // rollt nur noch
		}
		return new V3(vel.x() * friction, vy, vel.z() * friction);
	}

	/** Spin nach dem Aufsprung (ein Großteil geht durch die Reibung verloren). */
	public static V3 spinAfterBounce(V3 spin) {
		return spin.scale(0.45);
	}

	/** Ergebnis einer Flugbahn-Vorhersage. */
	public record Landing(V3 position, int ticks, double maxHeight) {
	}

	/** Simuliert die Flugbahn bis zum Boden (groundY = Oberkante des Bodens). */
	public static Landing simulate(V3 start, V3 vel, V3 spin, double groundY, int maxTicks) {
		V3 pos = start;
		double maxY = start.y();
		for (int t = 1; t <= maxTicks; t++) {
			vel = stepVelocity(vel, spin);
			spin = spin.scale(SPIN_DECAY);
			V3 next = pos.add(vel);
			if (next.y() <= groundY + BALL_RADIUS && vel.y() < 0) {
				double f = (pos.y() - (groundY + BALL_RADIUS)) / Math.max(1.0e-6, pos.y() - next.y());
				f = Math.clamp(f, 0.0, 1.0);
				return new Landing(pos.add(next.sub(pos).scale(f)), t, maxY);
			}
			pos = next;
			maxY = Math.max(maxY, pos.y());
		}
		return new Landing(pos, maxTicks, maxY);
	}

	/** Ergebnis der Zielberechnung. */
	public record Shot(V3 velocity, V3 spin, boolean reachable) {
	}

	/**
	 * Bestimmt den Abschlagwinkel, mit dem ein Ball mit gegebener Geschwindigkeit und gegebenem Spin
	 * möglichst nahe am Zielpunkt aufkommt. Es wird immer die flachste passende Flugbahn gewählt.
	 *
	 * @param spinFactory erzeugt den Spinvektor aus der horizontalen Flugrichtung
	 */
	public static Shot solve(V3 start, V3 target, double speed, java.util.function.Function<V3, V3> spinFactory, double groundY) {
		return solve(start, target, speed, spinFactory, groundY, false);
	}

	/**
	 * Wie {@link #solve(V3, V3, double, java.util.function.Function, double)}, mit {@code high = true} wird
	 * die hohe Flugbahn (Lob) gewählt.
	 */
	public static Shot solve(V3 start, V3 target, double speed, java.util.function.Function<V3, V3> spinFactory, double groundY, boolean high) {
		V3 flat = target.sub(start).horizontal();
		double targetDist = flat.length();
		V3 dir = flat.normalize();
		if (targetDist < 1.0e-3) {
			dir = new V3(1, 0, 0);
		}
		double yawCorrection = 0;
		Shot best = null;
		for (int iteration = 0; iteration < 3; iteration++) {
			V3 aimDir = rotateY(dir, yawCorrection);
			V3 spin = spinFactory.apply(aimDir);
			double bestPitch = Double.NaN;
			double prevDist = -1;
			double prevPitch = 0;
			double maxDist = -1;
			double maxPitch = 0;
			double step = high ? -0.5 : 0.5;
			for (double pitch = high ? 85 : -25; high ? pitch >= -25 : pitch <= 70; pitch += step) {
				V3 vel = launch(aimDir, pitch, speed);
				Landing l = simulate(start, vel, spin, groundY, 400);
				double dist = l.position().sub(start).horizontal().dot(dir);
				if (dist > maxDist) {
					maxDist = dist;
					maxPitch = pitch;
				}
				if (dist >= targetDist) {
					if (prevDist < 0) {
						bestPitch = pitch;
					} else {
						bestPitch = bisect(start, aimDir, dir, spin, speed, groundY, prevPitch, pitch, targetDist);
					}
					break;
				}
				prevDist = dist;
				prevPitch = pitch;
			}
			boolean reachable = !Double.isNaN(bestPitch);
			double pitch = reachable ? bestPitch : maxPitch;
			V3 vel = launch(aimDir, pitch, speed);
			best = new Shot(vel, spin, reachable);
			// seitliche Abweichung durch Seitenspin ausgleichen
			Landing l = simulate(start, vel, spin, groundY, 400);
			V3 landed = l.position().sub(start).horizontal();
			double side = landed.cross(flat).y();
			if (landed.length() < 1.0e-3 || targetDist < 1.0e-3) {
				break;
			}
			double angleError = Math.asin(Math.clamp(side / (landed.length() * targetDist), -1.0, 1.0));
			if (Math.abs(angleError) < 0.002) {
				break;
			}
			yawCorrection -= angleError;
		}
		return best;
	}

	/** Verfeinert den Abschlagwinkel zwischen zwei Winkeln, deren Landepunkte das Ziel einschließen. */
	private static double bisect(V3 start, V3 aimDir, V3 dir, V3 spin, double speed, double groundY, double shortPitch, double longPitch, double targetDist) {
		for (int i = 0; i < 24; i++) {
			double mid = (shortPitch + longPitch) / 2;
			Landing l = simulate(start, launch(aimDir, mid, speed), spin, groundY, 400);
			if (l.position().sub(start).horizontal().dot(dir) >= targetDist) {
				longPitch = mid;
			} else {
				shortPitch = mid;
			}
		}
		return (shortPitch + longPitch) / 2;
	}

	/** Startgeschwindigkeit aus Richtung, Höhenwinkel (Grad, positiv = nach oben) und Betrag. */
	public static V3 launch(V3 horizontalDir, double pitchDeg, double speed) {
		double p = Math.toRadians(pitchDeg);
		V3 d = horizontalDir.horizontal().normalize();
		return new V3(d.x() * Math.cos(p), Math.sin(p), d.z() * Math.cos(p)).scale(speed);
	}

	/** Dreht einen Vektor um die Y-Achse (Bogenmaß, gegen den Uhrzeigersinn von oben gesehen). */
	public static V3 rotateY(V3 v, double angle) {
		double c = Math.cos(angle);
		double s = Math.sin(angle);
		return new V3(v.x() * c - v.z() * s, v.y(), v.x() * s + v.z() * c);
	}
}
