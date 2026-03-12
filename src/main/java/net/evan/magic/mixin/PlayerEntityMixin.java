package net.evan.magic.mixin;

import net.evan.magic.magic.ability.MagicAbilityManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
