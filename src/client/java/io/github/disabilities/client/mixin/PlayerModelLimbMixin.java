package io.github.disabilities.client.mixin;

import io.github.disabilities.client.util.LimbVisibilityDuck;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelLimbMixin extends HumanoidModel<AvatarRenderState> {
	@Shadow
	@Final
	public ModelPart leftSleeve;
	@Shadow
	@Final
	public ModelPart rightSleeve;
	@Shadow
	@Final
	public ModelPart leftPants;
	@Shadow
	@Final
	public ModelPart rightPants;

	protected PlayerModelLimbMixin(ModelPart root) {
		super(root);
	}

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void disabilities$hideAmputatedLimbs(AvatarRenderState state, CallbackInfo ci) {
		LimbVisibilityDuck duck = (LimbVisibilityDuck) state;
		if (duck.disabilities$isOffhandMissing()) {
			if (duck.disabilities$isRightMainHand()) {
				this.leftArm.visible = false;
				this.leftSleeve.visible = false;
			} else {
				this.rightArm.visible = false;
				this.rightSleeve.visible = false;
			}
		}
		if (duck.disabilities$isLegMissing()) {
			this.rightLeg.visible = false;
			this.rightPants.visible = false;
		}
	}
}
