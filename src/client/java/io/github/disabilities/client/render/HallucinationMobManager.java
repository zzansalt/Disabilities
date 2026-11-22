package io.github.disabilities.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class HallucinationMobManager {
	private static final Random RANDOM = new Random();
	private static final String[] FAKE_TYPE_IDS = {
		"zombie",
		"creeper",
		"skeleton",
		"spider",
		"enderman"
	};

	private static final class FakeMob {
		final Mob entity;
		int ageTicks = 0;
		final int maxAgeTicks;

		FakeMob(Mob entity, int maxAgeTicks) {
			this.entity = entity;
			this.maxAgeTicks = maxAgeTicks;
		}
	}

	private static final List<FakeMob> ACTIVE = new ArrayList<>();
	private static int nextFakeEntityId = -900000;

	private HallucinationMobManager() {
	}

	public static void clear() {
		ACTIVE.clear();
	}

	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (player == null || level == null || !ClientConditionState.hasLocal(ConditionType.HALLUCINATION)) {
			ACTIVE.clear();
			return;
		}

		// Update existing fake mobs
		Iterator<FakeMob> it = ACTIVE.iterator();
		while (it.hasNext()) {
			FakeMob fm = it.next();
			Mob mob = fm.entity;
			if (mob.level() != level) {
				it.remove();
				continue;
			}

			fm.ageTicks++;
			double dx = player.getX() - mob.getX();
			double dz = player.getZ() - mob.getZ();
			double horizDist = Math.sqrt(dx * dx + dz * dz);

			if (fm.ageTicks >= fm.maxAgeTicks || horizDist > 28.0) {
				it.remove();
				continue;
			}

			mob.xo = mob.getX();
			mob.yo = mob.getY();
			mob.zo = mob.getZ();
			mob.yRotO = mob.getYRot();
			mob.yHeadRotO = mob.yHeadRot;
			mob.yBodyRotO = mob.yBodyRot;
			mob.tickCount++;

			float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
			mob.setYRot(targetYaw);
			mob.yHeadRot = targetYaw;
			mob.yBodyRot = targetYaw;

			if (horizDist > 1.8) {
				double speed = 0.085;
				double nx = mob.getX() + (dx / horizDist) * speed;
				double nz = mob.getZ() + (dz / horizDist) * speed;
				double ny = findGroundY(level, nx, mob.getY(), nz);
				mob.setPos(nx, ny, nz);
				mob.walkAnimation.update(0.6f, 0.4f, 1.0f);
			} else {
				mob.walkAnimation.update(0.0f, 0.4f, 1.0f);
			}
		}

		// Spawn new hallucination mob periodically (up to 3 active)
		if (ACTIVE.size() < 3 && RANDOM.nextInt(140) == 0) {
			spawnOne(level, player);
		}
	}

	private static void spawnOne(ClientLevel level, LocalPlayer player) {
		String typeId = FAKE_TYPE_IDS[RANDOM.nextInt(FAKE_TYPE_IDS.length)];
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(typeId));
		if (type == null) {
			return;
		}
		Entity created = type.create(level, EntitySpawnReason.EVENT);
		if (!(created instanceof Mob mob)) {
			return;
		}
		if (nextFakeEntityId >= 0) {
			nextFakeEntityId = -900000;
		}
		mob.setId(nextFakeEntityId--);
		double angle = RANDOM.nextDouble() * Math.PI * 2.0;
		double dist = 7.0 + RANDOM.nextDouble() * 8.0;
		double sx = player.getX() + Math.cos(angle) * dist;
		double sz = player.getZ() + Math.sin(angle) * dist;
		double sy = findGroundY(level, sx, player.getY(), sz);

		mob.setPos(sx, sy, sz);
		mob.xo = sx;
		mob.yo = sy;
		mob.zo = sz;
		float yaw = (float) (Math.toDegrees(Math.atan2(player.getZ() - sz, player.getX() - sx)) - 90.0);
		mob.setYRot(yaw);
		mob.yRotO = yaw;
		mob.yHeadRot = yaw;
		mob.yHeadRotO = yaw;
		mob.yBodyRot = yaw;
		mob.yBodyRotO = yaw;

		ACTIVE.add(new FakeMob(mob, 300 + RANDOM.nextInt(260)));
	}

	private static double findGroundY(ClientLevel level, double x, double refY, double z) {
		BlockPos base = BlockPos.containing(x, refY, z);
		// Search small vertical window first (works indoors and in caves too)
		for (int dy = 2; dy >= -3; dy--) {
			BlockPos pos = base.offset(0, dy, 0);
			if (level.getBlockState(pos).isAir() && !level.getBlockState(pos.below()).isAir()) {
				return pos.getY();
			}
		}
		int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX(), base.getZ());
		if (Math.abs(topY - refY) <= 8) {
			return topY;
		}
		return refY;
	}

	public static boolean tryHitHallucination(Minecraft mc) {
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (player == null || level == null || ACTIVE.isEmpty()) {
			return false;
		}

		Vec3 eye = player.getEyePosition(1.0f);
		Vec3 look = player.getLookAngle();
		double reach = 4.5;
		Vec3 end = eye.add(look.scale(reach));

		FakeMob closest = null;
		double closestDistSq = reach * reach;

		for (FakeMob fm : ACTIVE) {
			AABB box = fm.entity.getBoundingBox().inflate(0.35);
			Optional<Vec3> hit = box.clip(eye, end);
			if (hit.isPresent()) {
				double dSq = eye.distanceToSqr(hit.get());
				if (dSq < closestDistSq) {
					closestDistSq = dSq;
					closest = fm;
				}
			}
		}

		if (closest == null) {
			return false;
		}

		Mob mob = closest.entity;
		ACTIVE.remove(closest);
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);

		for (int i = 0; i < 14; i++) {
			double ox = (RANDOM.nextDouble() - 0.5) * 0.9;
			double oy = RANDOM.nextDouble() * mob.getBbHeight();
			double oz = (RANDOM.nextDouble() - 0.5) * 0.9;
			level.addParticle(ParticleTypes.POOF, mob.getX() + ox, mob.getY() + oy, mob.getZ() + oz, 0.0, 0.04, 0.0);
		}
		if (!ClientConditionState.hasLocal(ConditionType.DEAF)) {
			level.playLocalSound(mob.getX(), mob.getY(), mob.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.45f, 1.4f, false);
		}
		return true;
	}

	public static void submit(CameraRenderState camera, SubmitNodeCollector collector, float partialTick) {
		if (ACTIVE.isEmpty() || !ClientConditionState.hasLocal(ConditionType.HALLUCINATION)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
		Vec3 camPos = camera.pos;
		PoseStack poseStack = new PoseStack();

		for (FakeMob fm : ACTIVE) {
			Mob mob = fm.entity;
			double rx = mob.xo + (mob.getX() - mob.xo) * partialTick - camPos.x;
			double ry = mob.yo + (mob.getY() - mob.yo) * partialTick - camPos.y;
			double rz = mob.zo + (mob.getZ() - mob.zo) * partialTick - camPos.z;
			try {
				EntityRenderState state = dispatcher.extractEntity(mob, partialTick);
				dispatcher.submit(state, camera, rx, ry, rz, poseStack, collector);
			} catch (Throwable ignored) {
			}
		}
	}
}
