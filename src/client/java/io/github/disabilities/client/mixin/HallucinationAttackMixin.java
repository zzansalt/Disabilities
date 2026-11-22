package io.github.disabilities.client.mixin;

import io.github.disabilities.client.render.HallucinationMobManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class HallucinationAttackMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void disabilities$vanishHallucinationOnHit(CallbackInfoReturnable<Boolean> cir) {
		if (HallucinationMobManager.tryHitHallucination((Minecraft) (Object) this)) {
			cir.setReturnValue(true);
		}
	}
}
