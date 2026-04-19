package net.evan.magic.mixin;

import net.evan.magic.magic.ability.GreedAbilityRuntime;
import net.evan.magic.magic.ability.MagicAbilityManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
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
		if (self instanceof ServerPlayerEntity serverPlayer) {
			GreedAbilityRuntime.onPlayerJump(serverPlayer);
		}
		cir.setReturnValue(GreedAbilityRuntime.modifyJumpVelocity(self, cir.getReturnValueF()));
	}

	@Inject(method = "heal(F)V", at = @At("HEAD"), cancellable = true)
	private void magic$blockFrostHelplessHealing(float amount, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (MagicAbilityManager.shouldBlockHealing(self)) {
			ci.cancel();
		}
	}

	@Inject(method = "tryUseDeathProtector", at = @At("RETURN"))
	private void magic$recordGreedTotemTrigger(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!cir.getReturnValueZ()) {
			return;
		}
		MagicAbilityManager.onLivingEntityDeathProtectorTriggered(self);
		if (self instanceof ServerPlayerEntity serverPlayer) {
			GreedAbilityRuntime.onPlayerDeathProtectorTriggered(serverPlayer, source);
		}
	}
}

