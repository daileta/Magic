package net.evan.magic.mixin;

import net.evan.magic.magic.ability.MagicAbilityManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
	private void magic$applyPlusUltraFlightPose(PlayerLikeEntity playerLikeEntity, PlayerEntityRenderState renderState, float tickDelta, CallbackInfo ci) {
		if (!(playerLikeEntity instanceof PlayerEntity player) || !MagicAbilityManager.shouldRenderPlusUltraFlightPose(player)) {
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
}
