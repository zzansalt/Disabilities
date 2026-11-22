package io.github.disabilities.client.mixin;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.util.debug.DebugValueAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class MobHitboxMixin {
	@Inject(method = "emitGizmos", at = @At("HEAD"), cancellable = true)
	private void disabilities$hideHitboxesForProsopagnosia(double camX, double camY, double camZ, DebugValueAccess debugAccess, Frustum frustum, float partialTick, CallbackInfo ci) {
		if (ClientConditionState.hasLocal(ConditionType.PROSOPAGNOSIA)) {
			ci.cancel();
		}
	}
}
