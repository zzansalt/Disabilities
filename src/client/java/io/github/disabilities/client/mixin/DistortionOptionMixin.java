package io.github.disabilities.client.mixin;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptionInstance.class)
public abstract class DistortionOptionMixin<T> {
	@SuppressWarnings("unchecked")
	@Inject(method = "get", at = @At("RETURN"), cancellable = true)
	private void disabilities$overrideOptions(CallbackInfoReturnable<T> cir) {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.options == null) {
			return;
		}
		if (ClientConditionState.getVertigoTicks() > 0 && (Object) this == mc.options.screenEffectScale()) {
			cir.setReturnValue((T) Double.valueOf(1.0));
			return;
		}
		if (ClientConditionState.hasLocal(ConditionType.ASTIGMATISM)
			&& (Object) this == mc.options.renderDistance()
			&& (mc.gui == null || !(mc.gui.screen() instanceof OptionsScreen))) {
			cir.setReturnValue((T) Integer.valueOf(1));
		}
	}
}
