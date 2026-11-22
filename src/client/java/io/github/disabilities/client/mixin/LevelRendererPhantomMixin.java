package io.github.disabilities.client.mixin;

import io.github.disabilities.client.render.HallucinationMobManager;
import io.github.disabilities.client.render.PhantomRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererPhantomMixin {
	@Inject(method = "submitFeatures", at = @At("TAIL"))
	private void disabilities$submitSchizophreniaPhantom(LevelRenderState renderState, SubmitNodeCollector collector, boolean translucent, CallbackInfo ci) {
		PhantomRenderer.submit(renderState.cameraRenderState, collector);
		HallucinationMobManager.submit(renderState.cameraRenderState, collector, renderState.worldPartialTicks);
	}
}
