package io.github.disabilities.client.mixin;

import io.github.disabilities.ConditionType;
import io.github.disabilities.Disabilities;
import io.github.disabilities.client.ClientConditionState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GameRenderer.class)
public abstract class ColorBlindPostMixin {
	@Unique
	private static final Identifier MONOCHROME_EFFECT = Disabilities.id("monochrome");
	@Unique
	private static final Identifier DIPLOPIA_EFFECT = Disabilities.id("diplopia");
	@Unique
	private static final Identifier MYOPIA_EFFECT = Disabilities.id("myopia");

	@Shadow
	@Final
	private List<Identifier> requestedPostEffects;

	@Inject(method = "update", at = @At("TAIL"))
	private void disabilities$appendGrayscaleShader(DeltaTracker deltaTracker, CallbackInfo ci) {
		if (ClientConditionState.hasLocal(ConditionType.MYOPIA) && !this.requestedPostEffects.contains(MYOPIA_EFFECT)) {
			this.requestedPostEffects.add(MYOPIA_EFFECT);
		}
		if (ClientConditionState.hasLocal(ConditionType.DIPLOPIA) && !this.requestedPostEffects.contains(DIPLOPIA_EFFECT)) {
			this.requestedPostEffects.add(DIPLOPIA_EFFECT);
		}
		if (ClientConditionState.hasLocal(ConditionType.COLOR_BLIND) && !this.requestedPostEffects.contains(MONOCHROME_EFFECT)) {
			this.requestedPostEffects.add(MONOCHROME_EFFECT);
		}
	}
}
