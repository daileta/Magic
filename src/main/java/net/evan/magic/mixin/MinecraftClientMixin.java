package net.evan.magic.mixin;

import net.evan.magic.client.networking.MagicClientNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "hasOutline", at = @At("RETURN"), cancellable = true)
	private void magic$addConstellationOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && MagicClientNetworking.shouldRenderConstellationOutline(entity)) {
			cir.setReturnValue(true);
		}
	}
}

