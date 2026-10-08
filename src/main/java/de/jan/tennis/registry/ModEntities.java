package de.jan.tennis.registry;

import de.jan.tennis.TennisMod;
import de.jan.tennis.entity.TennisBallEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	private static final ResourceKey<EntityType<?>> TENNIS_BALL_KEY = ResourceKey.create(Registries.ENTITY_TYPE, TennisMod.id("tennis_ball"));

	public static final EntityType<TennisBallEntity> TENNIS_BALL = Registry.register(BuiltInRegistries.ENTITY_TYPE, TENNIS_BALL_KEY,
		EntityType.Builder.<TennisBallEntity>of(TennisBallEntity::new, MobCategory.MISC)
			.sized(0.2F, 0.2F)
			.clientTrackingRange(8)
			// Jede Bewegung sofort an die Spieler schicken, der Ball ist schnell
			.updateInterval(1)
			.noSave()
			.noLootTable()
			.build(TENNIS_BALL_KEY));

	private ModEntities() {
	}

	public static void init() {
	}
}
