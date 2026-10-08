package de.jan.tennis.item;

import de.jan.tennis.Msg;
import de.jan.tennis.court.Court;
import de.jan.tennis.entity.BallListener;
import de.jan.tennis.entity.TennisBallEntity;
import de.jan.tennis.logic.CourtGeometry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Linienrichter beim freien Training: zeigt nach jedem Schlag, ob der Ball im Feld war
 * (bei Aufschlägen bezogen auf das diagonale Aufschlagfeld).
 */
public class PracticeJudge implements BallListener {
	private static final int GREEN = 0x33FF55;
	private static final int RED = 0xFF3333;
	private @Nullable ServerPlayer hitter;
	private boolean serve;
	private @Nullable Court court;
	private int bounces;
	private Vec3 hitFrom = Vec3.ZERO;

	public PracticeJudge(ServerPlayer owner, boolean serve, @Nullable Court court) {
		this.hitter = owner;
		this.serve = serve;
		this.court = court;
	}

	@Override
	public void onHit(TennisBallEntity ball, ServerPlayer player) {
		serve = ball.isServeToss();
		hitter = player;
		bounces = 0;
		hitFrom = player.position();
		court = ball.getCourt();
	}

	@Override
	public void onBounce(TennisBallEntity ball, Vec3 pos) {
		bounces++;
		if (bounces != 1 || hitter == null || court == null || hitFrom == Vec3.ZERO) {
			return;
		}
		double u = court.u(pos);
		double v = court.v(pos);
		int hitterSide = court.u(hitFrom) < 0 ? -1 : 1;
		boolean in;
		String what;
		if (serve) {
			int halfSign = court.v(hitFrom) >= 0 ? 1 : -1;
			in = CourtGeometry.inServiceBox(u, v, -hitterSide, -halfSign);
			what = "Aufschlag";
		} else {
			in = CourtGeometry.inSinglesHalf(u, v, -hitterSide);
			what = "Ball";
		}
		ServerLevel level = (ServerLevel) ball.level();
		level.sendParticles(new DustParticleOptions(in ? GREEN : RED, 1.5F), pos.x, pos.y + 0.05, pos.z, 12, 0.08, 0.02, 0.08, 0);
		Msg.actionBar(hitter, in ? "§a" + what + " im Feld ✔" : "§c" + what + " im Aus ✘");
	}

	@Override
	public void onNetTouch(TennisBallEntity ball) {
	}

	@Override
	public void onObstacle(TennisBallEntity ball) {
	}

	@Override
	public void onDead(TennisBallEntity ball) {
	}
}
