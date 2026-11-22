package io.github.disabilities.client.mixin;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class BlindNoSprintMixin {
	@Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
	private void disabilities$preventSprintingWhenBlind(boolean sprinting, CallbackInfo ci) {
		if (!sprinting) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if ((Object) this == mc.player && ClientConditionState.hasLocal(ConditionType.BLIND)) {
			ci.cancel();
		}
	}
}
