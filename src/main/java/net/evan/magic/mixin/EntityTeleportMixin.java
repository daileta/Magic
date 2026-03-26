package net.evan.magic.mixin;

import java.util.Set;
import net.evan.magic.magic.ability.MagicAbilityManager;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityTeleportMixin {
	@Inject(method = "requestTeleport(DDD)V", at = @At("HEAD"), cancellable = true)
	private void magic$blockDomainRequestTeleport(double x, double y, double z, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (
			self instanceof ServerPlayerEntity player &&
			player.getEntityWorld() instanceof ServerWorld world &&
			MagicAbilityManager.shouldBlockPlayerTeleport(player, world, x, y, z)
		) {
			ci.cancel();
		}
	}

	@Inject(
		method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FFZ)Z",
		at = @At("HEAD"),
		cancellable = true
	)
	private void magic$blockDomainTeleport(
		ServerWorld targetWorld,
		double x,
		double y,
		double z,
		Set<PositionFlag> flags,
		float yaw,
		float pitch,
		boolean resetCamera,
		CallbackInfoReturnable<Boolean> cir
	) {
		Entity self = (Entity) (Object) this;
		if (
			self instanceof ServerPlayerEntity player &&
			MagicAbilityManager.shouldBlockPlayerTeleport(player, targetWorld, x, y, z)
		) {
			cir.setReturnValue(false);
		}
	}
}
