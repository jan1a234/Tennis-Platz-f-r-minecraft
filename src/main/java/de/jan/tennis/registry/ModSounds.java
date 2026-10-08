package de.jan.tennis.registry;

import de.jan.tennis.TennisMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent RACKET_HIT = register("racket_hit");
	public static final SoundEvent BALL_BOUNCE = register("ball_bounce");
	public static final SoundEvent NET_HIT = register("net_hit");
	public static final SoundEvent APPLAUSE = register("applause");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, TennisMod.id(name), SoundEvent.createVariableRangeEvent(TennisMod.id(name)));
	}

	public static void init() {
	}
}
