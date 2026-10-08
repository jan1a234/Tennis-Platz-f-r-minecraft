package de.jan.tennis.court;

import de.jan.tennis.logic.CourtGeometry;
import de.jan.tennis.logic.Surface;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ein gebauter Tennisplatz.
 *
 * @param cx      Weltkoordinate der Netzmitte (Blockmitte)
 * @param groundY Höhe der Spielfläche (Oberkante des Bodenblocks)
 * @param alongX  true, wenn die Längsachse entlang X verläuft, sonst entlang Z
 */
public record Court(String name, String dimension, double cx, int groundY, double cz, boolean alongX, Surface surface) {
	public double u(double x, double z) {
		return alongX ? x - cx : z - cz;
	}

	public double v(double x, double z) {
		return alongX ? z - cz : -(x - cx);
	}

	public double u(Vec3 pos) {
		return u(pos.x, pos.z);
	}

	public double v(Vec3 pos) {
		return v(pos.x, pos.z);
	}

	public Vec3 world(double u, double v, double y) {
		return alongX ? new Vec3(cx + u, y, cz + v) : new Vec3(cx - v, y, cz + u);
	}

	/** Blickrichtung (Minecraft-Yaw) zum Netz für einen Spieler auf Seite {@code side}. */
	public float yawTowardNet(int side) {
		Vec3 dir = world(-side, 0, 0).subtract(world(0, 0, 0));
		return (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
	}

	public boolean contains(Level level, Vec3 pos) {
		return isIn(level) && CourtGeometry.inArea(u(pos), v(pos)) && pos.y > groundY - 3 && pos.y < groundY + 30;
	}

	public boolean isIn(Level level) {
		return level.dimension().identifier().toString().equals(dimension);
	}

	public double distanceSqr(Vec3 pos) {
		double dx = pos.x - cx;
		double dz = pos.z - cz;
		double dy = pos.y - groundY;
		return dx * dx + dy * dy + dz * dz;
	}
}
