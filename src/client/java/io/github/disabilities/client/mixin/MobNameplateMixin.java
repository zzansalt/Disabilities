package io.github.disabilities.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import io.github.disabilities.client.util.MobFlagDuck;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MobNameplateMixin {
	@Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
	private void disabilities$hideMobNameplate(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if (ClientConditionState.hasLocal(ConditionType.PROSOPAGNOSIA) && ((MobFlagDuck) state).disabilities$isLivingMob()) {
			ci.cancel();
		}
	}
}
