package net.evan.magic.mixin;

import net.evan.magic.magic.ability.MagicAbilityManager;
import net.evan.magic.magic.ability.GreedRuntime;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
	@Inject(method = "damage", at = @At("HEAD"), cancellable = true)
	private void magic$blockLoveDomainPlayerDamage(
		ServerWorld world,
		DamageSource source,
		float amount,
		CallbackInfoReturnable<Boolean> cir
	) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (self instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
			if (MagicAbilityManager.onPlayerDamaged(serverPlayer, source, amount)) {
				cir.setReturnValue(true);
				return;
			}
		}
		if (MagicAbilityManager.isEntityCapturedByLoveDomain(self) || MagicAbilityManager.isDomainClashParticipantInvincible(self)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "takeShieldHit", at = @At("RETURN"))
	private void magic$recordGreedShieldDisable(ServerWorld world, LivingEntity attacker, CallbackInfo ci) {
		if (attacker instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
			GreedRuntime.onShieldDisabled(serverPlayer);
		}
	}

	@Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
	private void magic$applyComedicRewrite(ServerWorld world, DamageSource source, float amount, CallbackInfo ci) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (!(self instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) || self.isInvulnerableTo(world, source)) {
			return;
		}

		LivingEntityDamageInvokerMixin damageInvoker = (LivingEntityDamageInvokerMixin) self;
		float reducedDamage = damageInvoker.magic$invokeApplyArmorToDamage(source, amount);
		float modifiedDamage = damageInvoker.magic$invokeModifyAppliedDamage(source, reducedDamage);
		float finalDamage = Math.max(modifiedDamage - self.getAbsorptionAmount(), 0.0F);
		if (MagicAbilityManager.onPlayerPreApplyDamage(serverPlayer, world, source, amount, finalDamage)) {
			ci.cancel();
		}
	}

	@Inject(method = "damage", at = @At("RETURN"))
	private void magic$finishComedicRewrite(
		ServerWorld world,
		DamageSource source,
		float amount,
		CallbackInfoReturnable<Boolean> cir
	) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (self instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
			MagicAbilityManager.onPlayerDamageResolved(serverPlayer);
		}
	}
}
