package de.jan.tennis.entity;

import net.minecraft.world.phys.Vec3;

/** Wird serverseitig über alles informiert, was mit dem Ball passiert (Schiedsrichter). */
public interface BallListener {
	void onHit(TennisBallEntity ball, net.minecraft.server.level.ServerPlayer player);

	void onBounce(TennisBallEntity ball, Vec3 pos);

	void onNetTouch(TennisBallEntity ball);

	/** Ball trifft Zaun, Wand oder anderes Hindernis. */
	void onObstacle(TennisBallEntity ball);

	/** Ball liegt still, ist aus der Anlage geflogen oder im Wasser gelandet. */
	void onDead(TennisBallEntity ball);
}
