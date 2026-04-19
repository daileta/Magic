package net.evan.magic.mixin;

import net.evan.magic.magic.ability.GreedAbilityRuntime;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndCrystalEntity.class)
public abstract class EndCrystalEntityMixin {
	@Inject(method = "damage", at = @At("RETURN"))
	private void magic$recordGreedEndCrystalDestroy(
		ServerWorld world,
		DamageSource source,
		float amount,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (cir.getReturnValueZ()) {
			GreedAbilityRuntime.onEndCrystalDestroyed(source);
		}
	}
}

