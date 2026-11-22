package io.github.disabilities.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.disabilities.ConditionType;
import io.github.disabilities.Disabilities;
import io.github.disabilities.client.ClientConditionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public final class PhantomRenderer {
	private static final Identifier PHANTOM_TEXTURE = Disabilities.id("textures/entity/phantom_silhouette.png");
	private static final Random RANDOM = new Random();

	private static PlayerModel cachedModel;
	private static boolean active = false;
	private static double px;
	private static double py;
	private static double pz;
	private static int ageTicks = 0;
	private static int maxAgeTicks = 120;

	private PhantomRenderer() {
	}

	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || !ClientConditionState.hasLocal(ConditionType.SCHIZOPHRENIA)) {
			active = false;
			return;
		}

		if (active) {
			ageTicks++;
			double dx = px - player.getX();
			double dz = pz - player.getZ();
			double distSq = dx * dx + dz * dz;
			if (ageTicks >= maxAgeTicks || distSq < 4.5 || distSq > 900.0) {
				active = false;
			}
			return;
		}

		if (RANDOM.nextInt(320) == 0) {
			double yawRad = Math.toRadians(player.getYRot()) + (RANDOM.nextBoolean() ? 1.0 : -1.0) * (0.9 + RANDOM.nextDouble() * 1.6);
			double dist = 9.0 + RANDOM.nextDouble() * 10.0;
			px = player.getX() - Math.sin(yawRad) * dist;
			py = player.getY();
			pz = player.getZ() + Math.cos(yawRad) * dist;
			ageTicks = 0;
			maxAgeTicks = 80 + RANDOM.nextInt(120);
			active = true;
		}
	}

	public static void submit(CameraRenderState camera, SubmitNodeCollector collector) {
		if (!active || !ClientConditionState.hasLocal(ConditionType.SCHIZOPHRENIA)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		if (cachedModel == null) {
			cachedModel = new PlayerModel(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
		}

		Vec3 camPos = camera.pos;
		PoseStack poseStack = new PoseStack();
		poseStack.pushPose();
		poseStack.translate(px - camPos.x, py - camPos.y, pz - camPos.z);

		double lookDx = mc.player.getX() - px;
		double lookDz = mc.player.getZ() - pz;
		float facingDeg = (float) (Math.toDegrees(Math.atan2(lookDz, lookDx)) - 90.0);
		poseStack.rotateDegrees(Axis.YP, 180.0f - facingDeg);
		poseStack.scale(-0.9375f, -0.9375f, 0.9375f);
		poseStack.translate(0.0, -1.501, 0.0);

		AvatarRenderState state = new AvatarRenderState();
		state.ageInTicks = ageTicks;
		state.xRot = 0.0f;
		state.yRot = 0.0f;

		collector.submitModel(
			cachedModel,
			state,
			poseStack,
			RenderTypes.entityTranslucent(PHANTOM_TEXTURE),
			0x00F000F0,
			OverlayTexture.NO_OVERLAY,
			0
		);
		poseStack.popPose();
	}
}
