package net.evan.magic.mixin;

import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderState.class)
public interface WorldRenderStateAccessorMixin {
	@Accessor("cameraRenderState")
	CameraRenderState magic$getCameraRenderState();
}
