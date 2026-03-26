package net.evan.magic.mixin;

import net.evan.magic.magic.ability.GreedRuntime;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntMinecartEntity.class)
public abstract class TntMinecartEntityMixin {
	@Inject(method = "prime", at = @At("HEAD"))
	private void magic$recordGreedTntMinecartPrime(DamageSource source, CallbackInfo ci) {
		GreedRuntime.onTntMinecartPrimed(source);
	}
}
