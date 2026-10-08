package de.jan.tennis.client;

import de.jan.tennis.TennisMod;
import de.jan.tennis.outfit.Outfit;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.Nullable;

/** Outfits aller Spieler, wie vom Server gemeldet. */
public final class ClientOutfits {
	private static volatile Map<UUID, String> outfits = Map.of();

	private ClientOutfits() {
	}

	public static void set(Map<UUID, String> newOutfits) {
		outfits = Map.copyOf(newOutfits);
	}

	public static void clear() {
		outfits = Map.of();
	}

	/** Liefert den Tennis-Skin für den Spieler oder null, wenn er seinen eigenen Skin behält. */
	public static @Nullable PlayerSkin apply(UUID player, PlayerSkin original) {
		String id = outfits.get(player);
		if (id == null || Outfit.byId(id) == null) {
			return null;
		}
		ClientAsset.ResourceTexture body = new ClientAsset.ResourceTexture(TennisMod.id("skin/" + id));
		return PlayerSkin.insecure(body, original.cape(), original.elytra(), PlayerModelType.WIDE);
	}
}
