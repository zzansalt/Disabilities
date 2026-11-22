package io.github.disabilities.client.mixin;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Style.class)
public abstract class DyslexiaStyleMixin {
	@Inject(method = "isObfuscated", at = @At("RETURN"), cancellable = true)
	private void disabilities$scrambleTextForDyslexia(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()
			&& !ClientConditionState.isDyslexiaExempt()
			&& ClientConditionState.hasLocal(ConditionType.DYSLEXIA)) {
			cir.setReturnValue(true);
		}
	}
}
