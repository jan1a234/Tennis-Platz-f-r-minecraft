package de.jan.tennis.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BallPhysicsTest {
	@Test
	void freeFallMatchesGravity() {
		// 1 Sekunde freier Fall ohne Luftwiderstand-Einfluss nennenswert: etwa 4,9 m
		BallPhysics.Landing l = BallPhysics.simulate(new V3(0, 10, 0), V3.ZERO, V3.ZERO, 0, 200);
		assertTrue(l.ticks() > 25 && l.ticks() < 32, "Fall aus 10 m dauert ca. 1,4 s, war " + l.ticks());
	}

	@Test
	void topspinDipsFasterThanSlice() {
		V3 start = new V3(0, 1, 0);
		V3 vel = BallPhysics.launch(new V3(1, 0, 0), 8, 1.5);
		V3 top = BallPhysics.topspinAxis(new V3(1, 0, 0)).scale(0.25);
		BallPhysics.Landing withTop = BallPhysics.simulate(start, vel, top, 0, 400);
		BallPhysics.Landing withSlice = BallPhysics.simulate(start, vel, top.scale(-1), 0, 400);
		assertTrue(withTop.position().x() < withSlice.position().x(), "Topspin landet kürzer");
	}

	@Test
	void solverHitsTarget() {
		V3 start = new V3(0, 1.2, 0);
		V3 target = new V3(20, 0, 2);
		BallPhysics.Shot shot = BallPhysics.solve(start, target, 1.6, d -> BallPhysics.topspinAxis(d).scale(0.2), 0);
		assertTrue(shot.reachable());
		BallPhysics.Landing l = BallPhysics.simulate(start, shot.velocity(), shot.spin(), 0, 400);
		assertEquals(20, l.position().x(), 0.3);
		assertEquals(2, l.position().z(), 0.3);
	}

	@Test
	void solverCorrectsSidespin() {
		V3 start = new V3(0, 2.7, 0);
		V3 target = new V3(18, 0, -3);
		BallPhysics.Shot shot = BallPhysics.solve(start, target, 2.0, d -> V3.UP.scale(0.2), 0);
		BallPhysics.Landing l = BallPhysics.simulate(start, shot.velocity(), shot.spin(), 0, 400);
		assertEquals(18, l.position().x(), 0.4);
		assertEquals(-3, l.position().z(), 0.4);
	}

	@Test
	void bounceLosesHeight() {
		V3 v = BallPhysics.bounce(new V3(1, -0.5, 0), V3.ZERO, Surface.HARTPLATZ);
		assertEquals(0.375, v.y(), 1.0e-9);
		assertTrue(v.x() < 1);
		V3 grass = BallPhysics.bounce(new V3(1, -0.5, 0), V3.ZERO, Surface.RASEN);
		V3 clay = BallPhysics.bounce(new V3(1, -0.5, 0), V3.ZERO, Surface.SAND);
		assertTrue(grass.x() > clay.x(), "Rasen ist schneller als Sand");
		assertTrue(grass.y() < clay.y(), "Sand springt höher ab");
	}

	@Test
	void serveSpeedDropsInFlight() {
		V3 start = new V3(0, 2.7, 0);
		V3 vel = BallPhysics.launch(new V3(1, 0, 0), -3, 2.6);
		V3 v = vel;
		for (int i = 0; i < 10; i++) {
			v = BallPhysics.stepVelocity(v, V3.ZERO);
		}
		double kmhStart = vel.length() * BallPhysics.TO_KMH;
		double kmhAfter = v.length() * BallPhysics.TO_KMH;
		assertTrue(kmhStart > 180);
		assertTrue(kmhAfter < kmhStart * 0.75, "Ein Aufschlag verliert in der Luft deutlich an Tempo");
	}

	@Test
	void lobFliesHigherThanDrive() {
		V3 start = new V3(0, 1.0, 0);
		V3 target = new V3(18, 0, 0);
		java.util.function.Function<V3, V3> spin = d -> BallPhysics.topspinAxis(d).scale(0.1);
		BallPhysics.Shot drive = BallPhysics.solve(start, target, 1.2, spin, 0, false);
		BallPhysics.Shot lob = BallPhysics.solve(start, target, 1.2, spin, 0, true);
		BallPhysics.Landing l = BallPhysics.simulate(start, lob.velocity(), lob.spin(), 0, 400);
		assertTrue(lob.velocity().y() > drive.velocity().y());
		assertTrue(l.maxHeight() > 6, "Lob steigt hoch, war " + l.maxHeight());
		assertEquals(18, l.position().x(), 0.4);
	}
}
