package net.evan.magic.mixin;

import net.evan.magic.client.render.CelestialGamaRayPresentationManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GameRenderer.class, priority = 900)
public abstract class GameRendererMixin {
	@Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F", at = @At("RETURN"), cancellable = true)
	private void magic$applyCelestialGamaRayFovPulse(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		float multiplier = CelestialGamaRayPresentationManager.getLocalFovMultiplier(tickProgress);
		if (multiplier == 1.0F) {
			return;
		}

		cir.setReturnValue(cir.getReturnValueF() * multiplier);
	}

	@Inject(method = "bobView(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("TAIL"))
	private void magic$applyCelestialGamaRayCameraShake(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
		float shake = CelestialGamaRayPresentationManager.getLocalCameraShake(tickProgress);
		if (shake <= 0.0F) {
			return;
		}

		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.sin(tickProgress * 7.0F) * shake * 0.65F));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) Math.cos(tickProgress * 5.0F) * shake * 0.35F));
		matrices.translate(Math.sin(tickProgress * 6.0F) * shake * 0.0015F, Math.cos(tickProgress * 4.0F) * shake * 0.0012F, 0.0F);
	}
}

