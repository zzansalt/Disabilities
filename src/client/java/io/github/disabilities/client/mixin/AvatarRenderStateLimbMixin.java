package io.github.disabilities.client.mixin;

import io.github.disabilities.client.util.LimbVisibilityDuck;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateLimbMixin implements LimbVisibilityDuck {
	@Unique
	private boolean disabilities$offhandMissing;
	@Unique
	private boolean disabilities$legMissing;
	@Unique
	private boolean disabilities$rightMainHand = true;

	@Override
	public boolean disabilities$isOffhandMissing() {
		return this.disabilities$offhandMissing;
	}

	@Override
	public void disabilities$setOffhandMissing(boolean missing) {
		this.disabilities$offhandMissing = missing;
	}

	@Override
	public boolean disabilities$isLegMissing() {
		return this.disabilities$legMissing;
	}

	@Override
	public void disabilities$setLegMissing(boolean missing) {
		this.disabilities$legMissing = missing;
	}

	@Override
	public boolean disabilities$isRightMainHand() {
		return this.disabilities$rightMainHand;
	}

	@Override
	public void disabilities$setRightMainHand(boolean rightMain) {
		this.disabilities$rightMainHand = rightMain;
	}
}
