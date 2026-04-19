package net.evan.magic.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityDamageInvokerMixin {
	@Invoker("applyArmorToDamage")
	float magic$invokeApplyArmorToDamage(DamageSource source, float amount);

	@Invoker("modifyAppliedDamage")
	float magic$invokeModifyAppliedDamage(DamageSource source, float amount);
}

