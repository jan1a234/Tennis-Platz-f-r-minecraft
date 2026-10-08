package de.jan.tennis.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * Tennisschläger.
 * <ul>
 *   <li>Rechtsklick halten = ausholen, loslassen = schlagen. Je länger, desto härter.</li>
 *   <li>Ist kein Ball in Reichweite, wird ein Ball zum Aufschlag hochgeworfen.</li>
 *   <li>Linksklick auf den Ball = schneller Schlag ohne Ausholen.</li>
 *   <li>Schleichen = Slice (Unterschnitt), sonst Topspin. Nach oben schauen = Lob.</li>
 * </ul>
 */
public class TennisRacketItem extends Item {
	public static final int MAX_USE = 72000;

	public TennisRacketItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			ShotMaker.startSwing(serverPlayer);
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
		if (entity instanceof ServerPlayer player) {
			ShotMaker.swing(player, MAX_USE - remainingTime);
		}
		return true;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.TRIDENT;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return MAX_USE;
	}
}
