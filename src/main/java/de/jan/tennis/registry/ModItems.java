package de.jan.tennis.registry;

import de.jan.tennis.TennisMod;
import de.jan.tennis.item.TennisBallItem;
import de.jan.tennis.item.TennisRacketItem;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.UseEffects;

public final class ModItems {
	public static final Item TENNIS_RACKET = register("tennis_racket", TennisRacketItem::new, new Item.Properties()
		.stacksTo(1)
		// Mit dem Schläger kann man beim Ausholen weiterlaufen und sprinten
		.component(DataComponents.USE_EFFECTS, new UseEffects(true, false, 0.9F))
		// Etwas mehr Reichweite und eine großzügige Trefferfläche für den kleinen Ball
		.component(DataComponents.ATTACK_RANGE, new AttackRange(0.0F, 3.6F, 0.0F, 5.0F, 0.6F, 1.0F)));
	public static final Item TENNIS_BALL = register("tennis_ball", TennisBallItem::new, new Item.Properties().stacksTo(16));

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TennisMod.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(TENNIS_RACKET);
			output.accept(TENNIS_BALL);
		});
	}
}
