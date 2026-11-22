package io.github.disabilities.client.mixin;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import io.github.disabilities.client.util.LimbVisibilityDuck;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererLimbMixin<T extends Avatar & ClientAvatarEntity> {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void disabilities$extractMissingLimbs(T entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		LimbVisibilityDuck duck = (LimbVisibilityDuck) state;
		duck.disabilities$setOffhandMissing(ClientConditionState.hasFor(entity.getUUID(), ConditionType.AMPUTATED_OFFHAND));
		duck.disabilities$setLegMissing(ClientConditionState.hasFor(entity.getUUID(), ConditionType.AMPUTATED_LEG));
		duck.disabilities$setRightMainHand(entity.getMainArm() == HumanoidArm.RIGHT);
	}
}
