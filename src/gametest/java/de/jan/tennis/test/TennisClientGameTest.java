package de.jan.tennis.test;

import de.jan.tennis.TennisServer;
import de.jan.tennis.court.Court;
import de.jan.tennis.entity.BallListener;
import de.jan.tennis.entity.TennisBallEntity;
import de.jan.tennis.item.ShotMaker;
import de.jan.tennis.logic.CourtGeometry;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Spieltest mit echtem Dedicated Server und verbundenem Client (wie beim Spielen mit einem Kumpel):
 * Platz bauen, Outfit prüfen, Ball hochwerfen und aufschlagen.
 */
public class TennisClientGameTest implements FabricClientGameTest {
	private static volatile TennisBallEntity ball;
	private static volatile Vec3 firstBounce;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestDedicatedServerContext server = context.worldBuilder().setUseConsistentSettings(true).createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();

			// 1. Platz bauen
			server.runCommand("execute as @a at @s run tennis platz bauen rasen");
			server.waitFor(s -> TennisServer.get() != null && TennisServer.get().courts.all().size() == 1, 100);
			context.waitTicks(40);
			connection.waitForChunksRender();
			context.takeScreenshot("tennis-1-platz");

			// 2. Outfit: der erste Spieler wird automatisch zum Rotfuchs
			String skin = context.computeOnClient(mc -> mc.player.getSkin().body().texturePath().toString());
			if (!skin.equals("tennis:textures/skin/rotfuchs.png")) {
				throw new AssertionError("Erwartet Tennis-Outfit, war " + skin);
			}
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(10);
			context.takeScreenshot("tennis-2-outfit");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));

			// 3. Aufschlag von rechts ins diagonale Aufschlagfeld
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				Court c = TennisServer.get().courts.all().getFirst();
				Vec3 pos = c.world(-13.6, 2.2, c.groundY());
				Vec3 eye = pos.add(0, p.getEyeHeight(), 0);
				Vec3 target = c.world(4.5, -2.3, c.groundY());
				Vec3 d = target.subtract(eye);
				float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
				float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
				p.teleportTo(p.level(), pos.x, pos.y, pos.z, Set.of(), yaw, pitch, false);
			});
			context.waitTicks(10);
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				Court c = TennisServer.get().courts.all().getFirst();
				firstBounce = null;
				ball = ShotMaker.toss(p, c);
				ball.setListener(new Recorder());
			});
			// Ball erreicht nach etwa 11 Ticks den höchsten Punkt
			context.waitTicks(11);
			server.runOnServer(s -> ShotMaker.hit(player(s), ball, 0.8));
			context.waitTicks(3);
			context.takeScreenshot("tennis-3-aufschlag");
			server.waitFor(s -> firstBounce != null, 100);
			Vec3 bounce = firstBounce;
			boolean in = server.computeOnServer(s -> {
				Court c = TennisServer.get().courts.all().getFirst();
				double u = c.u(bounce);
				double v = c.v(bounce);
				System.out.println("[Tennis-Test] Aufschlag landet bei u=" + u + " v=" + v);
				if (u <= 0) {
					throw new AssertionError("Aufschlag ist nicht übers Netz gekommen: u=" + u);
				}
				return CourtGeometry.inServiceBox(u, v, 1, -1);
			});
			System.out.println("[Tennis-Test] Aufschlag " + (in ? "im Feld" : "im Aus"));
			context.waitTicks(10);
			context.takeScreenshot("tennis-4-nach-aufschlag");
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static final class Recorder implements BallListener {
		@Override
		public void onHit(TennisBallEntity b, ServerPlayer player) {
		}

		@Override
		public void onBounce(TennisBallEntity b, Vec3 pos) {
			if (firstBounce == null) {
				firstBounce = pos;
			}
		}

		@Override
		public void onNetTouch(TennisBallEntity b) {
		}

		@Override
		public void onObstacle(TennisBallEntity b) {
		}

		@Override
		public void onDead(TennisBallEntity b) {
		}
	}
}
