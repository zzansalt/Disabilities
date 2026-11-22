package io.github.disabilities.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class UniformMobRenderer {
	private static final Identifier STEVE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");
	private static PlayerModel cachedModel;

	private UniformMobRenderer() {
	}

	public static void submitAsSteve(LivingEntityRenderState source, PoseStack poseStack, SubmitNodeCollector collector) {
		Minecraft mc = Minecraft.getInstance();
		if (cachedModel == null) {
			cachedModel = new PlayerModel(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
		}

		AvatarRenderState state = new AvatarRenderState();
		state.ageInTicks = source.ageInTicks;
		state.walkAnimationPos = source.walkAnimationPos;
		state.walkAnimationSpeed = source.walkAnimationSpeed;
		state.yRot = source.yRot;
		state.xRot = source.xRot;
		state.bodyRot = source.bodyRot;
		state.lightCoords = source.lightCoords;
		state.isBaby = source.isBaby;
		state.hasRedOverlay = source.hasRedOverlay;

		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, 180.0f - source.bodyRot);
		float scale = source.isBaby ? 0.5f : 0.9375f;
		poseStack.scale(-scale, -scale, scale);
		poseStack.translate(0.0, -1.501, 0.0);

		int overlay = source.hasRedOverlay ? OverlayTexture.pack(OverlayTexture.u(0.0f), OverlayTexture.v(true)) : OverlayTexture.NO_OVERLAY;
		collector.submitModel(
			cachedModel,
			state,
			poseStack,
			RenderTypes.entityCutout(STEVE_TEXTURE),
			source.lightCoords,
			overlay,
			source.outlineColor
		);
		poseStack.popPose();
	}
}
