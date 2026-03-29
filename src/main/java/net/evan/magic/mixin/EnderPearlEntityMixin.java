package net.evan.magic.mixin;

import net.evan.magic.magic.ability.MagicAbilityManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderPearlEntity.class)
public abstract class EnderPearlEntityMixin {
	@Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
	private void magic$cancelDomainEscapeTeleport(HitResult hitResult, CallbackInfo ci) {
		EnderPearlEntity self = (EnderPearlEntity) (Object) this;
		Entity owner = self.getOwner();
		if (!(owner instanceof LivingEntity livingOwner)) {
			return;
		}
		if (!MagicAbilityManager.isEntityCapturedByDomain(livingOwner) && !MagicAbilityManager.shouldBlockFrostTeleport(livingOwner)) {
			return;
		}

		if (!self.getEntityWorld().isClient()) {
			self.discard();
		}
		ci.cancel();
	}
}
