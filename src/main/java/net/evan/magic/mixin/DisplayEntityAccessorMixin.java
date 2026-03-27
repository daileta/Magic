package net.evan.magic.mixin;

import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DisplayEntity.class)
public interface DisplayEntityAccessorMixin {
	@Invoker("setTeleportDuration")
	void magic$setTeleportDuration(int teleportDuration);

	@Invoker("setInterpolationDuration")
	void magic$setInterpolationDuration(int interpolationDuration);

	@Invoker("setDisplayWidth")
	void magic$setDisplayWidth(float width);

	@Invoker("setDisplayHeight")
	void magic$setDisplayHeight(float height);

	@Invoker("setViewRange")
	void magic$setViewRange(float viewRange);
}
