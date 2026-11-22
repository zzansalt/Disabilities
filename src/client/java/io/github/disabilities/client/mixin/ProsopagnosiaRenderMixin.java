package io.github.disabilities.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import io.github.disabilities.client.render.UniformMobRenderer;
import io.github.disabilities.client.util.MobFlagDuck;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class ProsopagnosiaRenderMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
	private void disabilities$markMobState(T entity, S state, float partialTick, CallbackInfo ci) {
		((MobFlagDuck) state).disabilities$setLivingMob(entity instanceof Mob);
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
	private void disabilities$replaceMobWithSteve(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if (ClientConditionState.hasLocal(ConditionType.PROSOPAGNOSIA) && ((MobFlagDuck) state).disabilities$isLivingMob()) {
			UniformMobRenderer.submitAsSteve(state, poseStack, collector);
			ci.cancel();
		}
	}
}
