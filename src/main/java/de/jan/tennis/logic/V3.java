package de.jan.tennis.logic;

/** Kleiner, unveränderlicher 3D-Vektor für die Ballphysik (unabhängig von Minecraft-Klassen). */
public record V3(double x, double y, double z) {
	public static final V3 ZERO = new V3(0, 0, 0);
	public static final V3 UP = new V3(0, 1, 0);

	public V3 add(V3 o) {
		return new V3(x + o.x, y + o.y, z + o.z);
	}

	public V3 sub(V3 o) {
		return new V3(x - o.x, y - o.y, z - o.z);
	}

	public V3 scale(double s) {
		return new V3(x * s, y * s, z * s);
	}

	public double dot(V3 o) {
		return x * o.x + y * o.y + z * o.z;
	}

	public V3 cross(V3 o) {
		return new V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
	}

	public double length() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	public double horizontalLength() {
		return Math.sqrt(x * x + z * z);
	}

	public V3 normalize() {
		double l = length();
		return l < 1.0e-9 ? ZERO : scale(1.0 / l);
	}

	public V3 horizontal() {
		return new V3(x, 0, z);
	}
}
