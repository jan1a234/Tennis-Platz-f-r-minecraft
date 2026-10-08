package de.jan.tennis.mixin.client;

import de.jan.tennis.client.ClientOutfits;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ersetzt den Skin eines Spielers durch sein Tennis-Outfit. */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void tennis$applyOutfit(CallbackInfoReturnable<PlayerSkin> cir) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		PlayerSkin outfit = ClientOutfits.apply(self.getUUID(), cir.getReturnValue());
		if (outfit != null) {
			cir.setReturnValue(outfit);
		}
	}
}
