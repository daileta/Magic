package net.evan.magic.mixin;

import net.evan.magic.magic.ability.GreedRuntime;
import net.evan.magic.magic.ability.MagicAbilityManager;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "getJumpVelocity(F)F", at = @At("RETURN"), cancellable = true)
	private void magic$modifyGreedJumpVelocity(float strength, CallbackInfoReturnable<Float> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		cir.setReturnValue(GreedRuntime.modifyJumpVelocity(self, cir.getReturnValueF()));
	}

	@Inject(method = "heal(F)V", at = @At("HEAD"), cancellable = true)
	private void magic$blockFrostHelplessHealing(float amount, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (MagicAbilityManager.shouldBlockHealing(self)) {
			ci.cancel();
		}
	}
}
