package io.github.disabilities.client.mixin;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class AstigmatismRenderDistanceMixin {
	@Inject(method = "getEffectiveRenderDistance", at = @At("RETURN"), cancellable = true)
	private void disabilities$forceOneChunkForAstigmatism(CallbackInfoReturnable<Integer> cir) {
		if (ClientConditionState.hasLocal(ConditionType.ASTIGMATISM)) {
			cir.setReturnValue(1);
		}
	}
}
