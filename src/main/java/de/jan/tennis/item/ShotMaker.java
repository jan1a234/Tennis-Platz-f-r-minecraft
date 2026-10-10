package de.jan.tennis.item;

import de.jan.tennis.Msg;
import de.jan.tennis.TennisServer;
import de.jan.tennis.court.Court;
import de.jan.tennis.entity.TennisBallEntity;
import de.jan.tennis.logic.BallPhysics;
import de.jan.tennis.logic.CourtGeometry;
import de.jan.tennis.logic.V3;
import de.jan.tennis.match.TennisMatch;
import de.jan.tennis.registry.ModItems;
import de.jan.tennis.registry.ModSounds;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Berechnet Schläge: Der Spieler zielt mit dem Fadenkreuz auf den Punkt, an dem der Ball aufkommen soll.
 * Ausholdauer bestimmt das Tempo, Treffpunkt und Tempo bestimmen die Genauigkeit.
 */
public final class ShotMaker {
	/** Reichweite vom Körper aus (Arm + Schläger + Ausfallschritt). */
	public static final double REACH = 2.9;
	private static final Map<UUID, TennisBallEntity> PRACTICE_BALLS = new HashMap<>();

	public enum ShotType {
		SERVE("Aufschlag"), FOREHAND("Vorhand"), BACKHAND("Rückhand"), VOLLEY("Volley"), SMASH("Schmetterball"), LOB("Lob");

		public final String label;

		ShotType(String label) {
			this.label = label;
		}
	}

	private ShotMaker() {
	}

	/** Rechtsklick gedrückt: ohne Ball in Reichweite wird ein Ball zum Aufschlag hochgeworfen. */
	public static void startSwing(ServerPlayer player) {
		if (findBall(player).isPresent()) {
			return;
		}
		TennisServer ts = TennisServer.get();
		TennisMatch match = ts == null ? null : ts.matches.byPlayer(player);
		if (match != null) {
			match.tryToss(player);
			return;
		}
		TennisBallEntity old = PRACTICE_BALLS.remove(player.getUUID());
		if (old != null && !old.isRemoved()) {
			old.discard();
		}
		Court court = ts == null ? null : ts.courts.find(player.level(), player.position(), 30).orElse(null);
		TennisBallEntity ball = toss(player, court);
		ball.setListener(new PracticeJudge(player, true, court));
		PRACTICE_BALLS.put(player.getUUID(), ball);
	}

	/** Wirft den Ball für den Aufschlag hoch (Ballwurf etwa 1,5 m über die Hand, wie bei den Profis). */
	public static TennisBallEntity toss(ServerPlayer player, @Nullable Court court) {
		Vec3 look = horizontal(player.getLookAngle());
		Vec3 right = new Vec3(-look.z, 0, look.x);
		Vec3 start = player.position().add(0, 1.55, 0).add(look.scale(0.45)).add(right.scale(0.2));
		Vec3 vel = new Vec3(0, 0.26, 0).add(look.scale(0.012));
		TennisBallEntity ball = TennisBallEntity.spawn((ServerLevel) player.level(), start, vel, court);
		ball.setServeToss(true);
		ball.playSound(ModSounds.BALL_BOUNCE, 0.2F, 1.6F);
		return ball;
	}

	/** Rechtsklick losgelassen. */
	public static void swing(ServerPlayer player, int chargeTicks) {
		player.swing(InteractionHand.MAIN_HAND, true);
		findBall(player).ifPresent(ball -> {
			// Beim Aufschlag fällt volle Ausholzeit mit dem höchsten Punkt des Ballwurfs zusammen
			double power = ball.isServeToss() ? Math.clamp((chargeTicks - 2) / 9.0, 0.0, 1.0) : Math.clamp((chargeTicks - 2) / 14.0, 0.0, 1.0);
			hit(player, ball, power);
		});
	}

	/** Linksklick auf den Ball: kurzer Schlag ohne Ausholen. */
	public static void quickHit(ServerPlayer player, TennisBallEntity ball) {
		if (!player.getMainHandItem().is(ModItems.TENNIS_RACKET)) {
			return;
		}
		if (player.position().add(0, 1.0, 0).distanceTo(ball.position()) > REACH + 0.8) {
			return;
		}
		hit(player, ball, 0.45 * player.getAttackStrengthScale(0.5F));
	}

	public static Optional<TennisBallEntity> findBall(ServerPlayer player) {
		Vec3 body = player.position().add(0, 1.0, 0);
		Vec3 look = horizontal(player.getLookAngle());
		AABB box = player.getBoundingBox().inflate(REACH + 1.0, REACH + 1.5, REACH + 1.0);
		return player.level().getEntitiesOfClass(TennisBallEntity.class, box, b -> !b.isDead()).stream()
			.filter(b -> b.position().distanceTo(body) <= REACH || b.position().distanceTo(player.getEyePosition()) <= REACH)
			.filter(b -> horizontal(b.position().subtract(body)).dot(look) > -0.35)
			.filter(b -> !b.recentlyHitBy(player, 8))
			.min(Comparator.comparingDouble(b -> b.position().distanceTo(body)));
	}

	public static void hit(ServerPlayer player, TennisBallEntity ball, double power) {
		if (ball.recentlyHitBy(player, 8)) {
			return;
		}
		TennisServer ts = TennisServer.get();
		TennisMatch match = ts == null ? null : ts.matches.byPlayer(player);
		if (match != null && !match.mayHit(player, ball)) {
			return;
		}
		Court court = ball.getCourt();
		if (court == null && ts != null) {
			court = ts.courts.find(player.level(), ball.position(), 30).orElse(null);
			ball.setCourt(court);
		}
		RandomSource random = player.getRandom();
		Vec3 ballPos = ball.position();
		double groundY = court != null ? court.groundY() : player.getY();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 lookFlat = horizontal(look);
		Vec3 right = new Vec3(-lookFlat.z, 0, lookFlat.x);
		boolean slice = player.isShiftKeyDown();
		boolean lob = player.getXRot() < -12.0F;
		double heightOverFeet = ballPos.y - player.getY();
		Vec3 toBall = new Vec3(ballPos.x - player.getX(), 0, ballPos.z - player.getZ());
		double sideOffset = toBall.dot(right);

		ShotType type;
		if (ball.isServeToss()) {
			type = ShotType.SERVE;
		} else if (lob) {
			type = ShotType.LOB;
		} else if (heightOverFeet > 2.3) {
			type = ShotType.SMASH;
		} else if (!ball.onGround() && court != null && Math.abs(court.u(player.position())) < 8.0 && heightOverFeet > 0.6) {
			type = ShotType.VOLLEY;
		} else {
			type = sideOffset >= 0 ? ShotType.FOREHAND : ShotType.BACKHAND;
		}

		// Treffqualität: wie sauber der Ball im idealen Treffpunkt getroffen wurde (0 … 1)
		double quality;
		if (type == ShotType.SERVE || type == ShotType.SMASH) {
			double heightError = Math.abs(heightOverFeet - 2.75) / 1.8;
			double apexError = Math.abs(ball.getDeltaMovement().y) / 0.45;
			quality = 1.0 - 0.6 * Math.min(1, heightError) - 0.4 * Math.min(1, apexError);
		} else {
			double distError = Math.abs(toBall.length() - 1.35) / 2.2;
			double heightError = Math.abs(heightOverFeet - 1.0) / 2.0;
			quality = 1.0 - 0.6 * Math.min(1, distError) - 0.4 * Math.min(1, heightError);
		}
		quality = Math.clamp(quality, 0.0, 1.0);

		// Zielpunkt: dort, wo das Fadenkreuz den Boden trifft
		Vec3 target;
		if (!lob && look.y < -0.03) {
			double t = (groundY - eye.y) / look.y;
			target = eye.add(look.scale(Math.min(t, 40)));
		} else {
			target = ballPos.add(lookFlat.scale(lob ? 20 : 25));
		}
		Vec3 toTarget = target.subtract(ballPos);
		if (toTarget.horizontalDistance() < 4.0) {
			target = ballPos.add(lookFlat.scale(4.0));
		}
		if (court != null) {
			target = aimAssist(court, type, player.position(), target);
		}

		double speed;
		double topspin;
		double sidespin = 0;
		double errorDeg;
		switch (type) {
			case SERVE -> {
				speed = lerp(1.2, 1.75, power) * (0.85 + 0.15 * quality);
				topspin = slice ? 0.16 : 0.06;
				sidespin = slice ? 0.12 : 0.0;
				if (slice) {
					speed *= 0.9;
				}
				errorDeg = 0.2 + 1.0 * (1 - quality) + 0.4 * power;
			}
			case SMASH -> {
				speed = lerp(1.2, 1.6, power) * (0.85 + 0.15 * quality);
				topspin = 0.05;
				errorDeg = 0.3 + 1.2 * (1 - quality) + 0.5 * power;
			}
			case LOB -> {
				speed = lerp(0.85, 1.1, power);
				topspin = slice ? -0.08 : 0.1;
				errorDeg = 0.4 + 1.0 * (1 - quality);
			}
			case VOLLEY -> {
				speed = lerp(0.8, 1.15, power) * (0.85 + 0.15 * quality);
				topspin = slice ? -0.1 : 0.03;
				errorDeg = 0.3 + 1.2 * (1 - quality) + 0.5 * power;
			}
			default -> {
				speed = lerp(0.9, 1.3, power) * (0.85 + 0.15 * quality);
				topspin = slice ? -0.12 : 0.12 + 0.08 * power;
				if (slice) {
					speed *= 0.9;
				}
				errorDeg = 0.3 + 1.4 * (1 - quality) + 0.6 * power;
			}
		}
		final double top = topspin;
		final double side = sidespin;
		Function<V3, V3> spinFactory = dir -> BallPhysics.topspinAxis(dir).scale(top).add(V3.UP.scale(side));
		V3 start = TennisBallEntity.toV3(ballPos);
		V3 aim = TennisBallEntity.toV3(target);
		BallPhysics.Shot shot = BallPhysics.solve(start, aim, speed, spinFactory, groundY, type == ShotType.LOB);
		// Zu langsam, um das Ziel zu erreichen: etwas mehr Tempo, statt den Ball zu kurz zu spielen
		for (int i = 0; i < 8 && !shot.reachable(); i++) {
			speed *= 1.08;
			shot = BallPhysics.solve(start, aim, speed, spinFactory, groundY, type == ShotType.LOB);
		}
		// Wer flach und hart ins nahe Feld zielt, muss Tempo rausnehmen, sonst landet der Ball sicher im Netz
		if (court != null && Math.signum(court.u(ballPos)) != Math.signum(court.u(target))) {
			for (int i = 0; i < 10 && netMargin(court, start, shot.velocity(), shot.spin(), groundY) < 0.12; i++) {
				speed *= 0.93;
				shot = BallPhysics.solve(start, aim, speed, spinFactory, groundY, type == ShotType.LOB);
			}
		}

		// Streuung: je härter und unsauberer, desto ungenauer
		V3 vel = shot.velocity();
		double yawError = Math.toRadians(random.nextGaussian() * errorDeg);
		double pitchError = Math.toRadians(random.nextGaussian() * errorDeg * 0.5);
		vel = BallPhysics.rotateY(vel, yawError);
		vel = tilt(vel, pitchError);

		ball.setCourt(court);
		ball.hit(player, TennisBallEntity.toVec(vel), shot.spin());

		ServerLevel level = (ServerLevel) player.level();
		float pitch = (float) (0.85 + 0.35 * power + random.nextFloat() * 0.1);
		level.playSound(null, ballPos.x, ballPos.y, ballPos.z, ModSounds.RACKET_HIT, SoundSource.PLAYERS, (float) (0.7 + 0.5 * power), pitch);
		if (power > 0.7 && quality > 0.6) {
			level.sendParticles(ParticleTypes.CRIT, ballPos.x, ballPos.y, ballPos.z, 8, 0.1, 0.1, 0.1, 0.3);
		}
		int kmh = (int) Math.round(vel.length() * BallPhysics.TO_KMH);
		String spinText = type == ShotType.SERVE ? (slice ? " (Slice)" : "") : (slice ? " Slice" : "");
		String qualityText = quality > 0.85 ? " – perfekt getroffen!" : quality < 0.35 ? " – Rahmentreffer" : "";
		Msg.actionBar(player, "§e" + type.label + spinText + " §f" + kmh + " km/h§7" + qualityText);
		if (match != null) {
			match.onShot(player, type, kmh);
		}
	}

	/**
	 * Zielhilfe: Das Ziel wird ins gegnerische Feld gezogen, beim Aufschlag ins diagonale Aufschlagfeld.
	 * Die Streuung kann den Ball trotzdem ins Aus tragen, aber grobes Verzielen wird verziehen.
	 */
	static Vec3 aimAssist(Court court, ShotType type, Vec3 playerPos, Vec3 target) {
		double pu = court.u(playerPos);
		double pv = court.v(playerPos);
		int otherSide = pu < 0 ? 1 : -1;
		double u = court.u(target);
		double v = court.v(target);
		if (type == ShotType.SERVE) {
			int boxSign = pv < 0 ? 1 : -1;
			u = otherSide * Math.clamp(otherSide * u, 2.0, CourtGeometry.SERVICE_LINE - 1.2);
			v = boxSign * Math.clamp(boxSign * v, 0.6, CourtGeometry.SINGLES_HALF_WIDTH - 1.0);
		} else {
			u = otherSide * Math.clamp(otherSide * u, 2.5, CourtGeometry.HALF_LENGTH - 1.8);
			v = Math.clamp(v, -(CourtGeometry.SINGLES_HALF_WIDTH - 1.0), CourtGeometry.SINGLES_HALF_WIDTH - 1.0);
		}
		return court.world(u, v, target.y);
	}

	/** Abstand des Balls über der Netzkante, wenn er die Netzebene überquert (negativ: Netz). */
	static double netMargin(Court court, V3 start, V3 vel, V3 spin, double groundY) {
		V3 pos = start;
		double side = Math.signum(court.u(start.x(), start.z()));
		double netTop = groundY + 1.0 + BallPhysics.BALL_RADIUS;
		for (int t = 0; t < 200; t++) {
			vel = BallPhysics.stepVelocity(vel, spin);
			spin = spin.scale(BallPhysics.SPIN_DECAY);
			V3 next = pos.add(vel);
			if (next.y() < groundY) {
				return Double.MAX_VALUE;
			}
			double u0 = court.u(pos.x(), pos.z());
			double u1 = court.u(next.x(), next.z());
			if (Math.signum(u1) != side) {
				double f = u0 / (u0 - u1);
				return pos.y() + (next.y() - pos.y()) * f - netTop;
			}
			pos = next;
		}
		return Double.MAX_VALUE;
	}

	private static V3 tilt(V3 v, double angle) {
		double h = v.horizontalLength();
		double pitch = Math.atan2(v.y(), h) + angle;
		double speed = v.length();
		V3 dir = v.horizontal().normalize();
		return new V3(dir.x() * Math.cos(pitch) * speed, Math.sin(pitch) * speed, dir.z() * Math.cos(pitch) * speed);
	}

	private static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	public static Vec3 horizontal(Vec3 v) {
		Vec3 h = new Vec3(v.x, 0, v.z);
		double l = h.length();
		return l < 1.0e-6 ? new Vec3(1, 0, 0) : h.scale(1.0 / l);
	}
}
