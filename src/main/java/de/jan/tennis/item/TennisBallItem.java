package de.jan.tennis.item;

import de.jan.tennis.TennisServer;
import de.jan.tennis.court.Court;
import de.jan.tennis.entity.TennisBallEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Tennisball: Rechtsklick wirft einen Ball sanft zu (zum Üben von Grundschlägen mit einem Partner). */
public class TennisBallItem extends Item {
	public TennisBallItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
			Vec3 look = player.getLookAngle();
			Vec3 start = player.getEyePosition().add(look.scale(0.6)).add(0, -0.3, 0);
			Vec3 vel = look.scale(0.65).add(0, 0.18, 0);
			TennisServer ts = TennisServer.get();
			Court court = ts == null ? null : ts.courts.find(level, player.position(), 30).orElse(null);
			TennisBallEntity ball = TennisBallEntity.spawn(serverLevel, start, vel, court);
			ball.setListener(new PracticeJudge(serverPlayer, false, court));
			if (!player.hasInfiniteMaterials()) {
				player.getItemInHand(hand).shrink(1);
			}
			player.swing(hand, true);
		}
		return InteractionResult.SUCCESS;
	}
}
