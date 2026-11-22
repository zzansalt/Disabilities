package io.github.disabilities.client.mixin;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class DeafSoundMixin {
	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void disabilities$silenceAllSounds(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		if (ClientConditionState.hasLocal(ConditionType.DEAF)) {
			cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
		}
	}
}
