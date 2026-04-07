package net.evan.magic.mixin;

import net.evan.magic.client.BurningPassionAfterimageRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"))
	@SuppressWarnings("unchecked")
	private void magic$renderBurningPassionAfterimages(LivingEntityRenderState renderState, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
		if (!(renderState instanceof PlayerEntityRenderState playerRenderState) || !((Object) this instanceof PlayerEntityRenderer)) {
			return;
		}

		BurningPassionAfterimageRenderer.renderAfterimages(
			(LivingEntityRenderer<?, PlayerEntityRenderState, ?>) (Object) this,
			playerRenderState,
			matrices,
			queue,
			camera
		);
	}

	@Inject(method = "getMixColor(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;)I", at = @At("HEAD"), cancellable = true)
	private void magic$overrideAfterimageMixColor(LivingEntityRenderState renderState, CallbackInfoReturnable<Integer> cir) {
		if (!(renderState instanceof PlayerEntityRenderState playerRenderState)) {
			return;
		}

		Integer overrideColor = BurningPassionAfterimageRenderer.mixColorOverride(playerRenderState);
		if (overrideColor != null) {
			cir.setReturnValue(overrideColor);
		}
	}

	@Inject(method = "getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;", at = @At("HEAD"), cancellable = true)
	@SuppressWarnings("unchecked")
	private void magic$forceAfterimageRenderLayer(
		LivingEntityRenderState renderState,
		boolean showBody,
		boolean translucent,
		boolean showOutline,
		CallbackInfoReturnable<RenderLayer> cir
	) {
		if (!(renderState instanceof PlayerEntityRenderState playerRenderState)) {
			return;
		}
		if (!BurningPassionAfterimageRenderer.isAfterimageRenderState(playerRenderState)) {
			return;
		}

		Identifier texture = ((LivingEntityRenderer<?, PlayerEntityRenderState, ?>) (Object) this).getTexture(playerRenderState);
		cir.setReturnValue(RenderLayers.entityTranslucent(texture));
	}
}
