package net.evan.magic.mixin;

import net.evan.magic.client.render.BurningPassionAfterimageRenderer;
import net.evan.magic.magic.ability.MagicAbilityManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
	private void magic$applyPlusUltraFlightPose(PlayerLikeEntity playerLikeEntity, PlayerEntityRenderState renderState, float tickDelta, CallbackInfo ci) {
		if (!(playerLikeEntity instanceof PlayerEntity player)) {
			return;
		}

		BurningPassionAfterimageRenderer.capture(player, renderState);
		if (!MagicAbilityManager.shouldRenderPlusUltraFlightPose(player)) {
			return;
		}

		renderState.isGliding = true;
		renderState.pose = EntityPose.GLIDING;
		renderState.glidingTicks = Math.max(renderState.glidingTicks, 10.0F);
		Vec3d look = playerLikeEntity.getRotationVec(tickDelta);
		Vec3d velocity = playerLikeEntity.getVelocity();
		if (velocity.horizontalLengthSquared() > 1.0E-5 && look.horizontalLengthSquared() > 1.0E-5) {
			renderState.applyFlyingRotation = true;
			Vec3d horizontalVelocity = velocity.getHorizontal().normalize();
			Vec3d horizontalLook = look.getHorizontal().normalize();
			double dot = horizontalVelocity.dotProduct(horizontalLook);
			double cross = velocity.x * look.z - velocity.z * look.x;
			renderState.flyingRotation = (float) (Math.signum(cross) * Math.acos(Math.min(1.0, Math.abs(dot))));
			return;
		}

		renderState.applyFlyingRotation = false;
		renderState.flyingRotation = 0.0F;
	}

	@Inject(method = "shouldRenderFeatures(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)Z", at = @At("HEAD"), cancellable = true)
	private void magic$suppressAfterimageFeatures(PlayerEntityRenderState renderState, CallbackInfoReturnable<Boolean> cir) {
		if (BurningPassionAfterimageRenderer.shouldSuppressFeatures(renderState)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
	private void magic$suppressAfterimageLabel(PlayerEntityRenderState renderState, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
		if (BurningPassionAfterimageRenderer.shouldSuppressLabels(renderState)) {
			ci.cancel();
		}
	}
}

