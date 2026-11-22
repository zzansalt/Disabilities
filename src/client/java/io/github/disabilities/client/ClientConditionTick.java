package io.github.disabilities.client;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.mixin.MinecraftActionInvoker;
import io.github.disabilities.client.render.HallucinationMobManager;
import io.github.disabilities.client.render.PhantomRenderer;
import io.github.disabilities.client.util.RandomWordGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Random;

public final class ClientConditionTick {
	private static final Random RANDOM = new Random();

	private static final SoundEvent[] PHANTOM_STEPS = {
		SoundEvents.STONE_STEP, SoundEvents.GRASS_STEP, SoundEvents.WOOD_STEP,
		SoundEvents.GRAVEL_STEP, SoundEvents.SAND_STEP
	};
	private static final SoundEvent[] PHANTOM_MOBS = {
		SoundEvents.CREEPER_PRIMED, SoundEvents.ZOMBIE_AMBIENT, SoundEvents.SKELETON_AMBIENT,
		SoundEvents.ENDERMAN_STARE, SoundEvents.SPIDER_AMBIENT, SoundEvents.TNT_PRIMED,
		SoundEvents.WITCH_AMBIENT
	};

	private static float targetParkYaw = 0.0f;
	private static float targetParkPitch = 0.0f;

	private ClientConditionTick() {
	}

	public static void tick(Minecraft mc) {
		if (mc.isPaused() || mc.player == null || mc.level == null) {
			return;
		}

		ClientConditionState.decrementTimers();
		LocalPlayer player = mc.player;

		if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
			ClientConditionState.parkinsonsYaw *= 0.8f;
			ClientConditionState.parkinsonsPitch *= 0.8f;
			return;
		}

		tickParkinsons(player);
		tickSeizureCamera(player);
		tickAcrophobia(player);
		tickCerebralPalsy(mc);
		tickNarcolepsy();
		tickAdhd(player);
		tickTourettes(mc, player);
		tickSchizophrenia(mc, player);
		PhantomRenderer.tick(mc);
		HallucinationMobManager.tick(mc);
	}

	private static void tickParkinsons(LocalPlayer player) {
		if (!ClientConditionState.hasLocal(ConditionType.PARKINSONS)) {
			ClientConditionState.parkinsonsYaw *= 0.7f;
			ClientConditionState.parkinsonsPitch *= 0.7f;
			return;
		}
		if (RANDOM.nextInt(3) == 0) {
			targetParkYaw = (RANDOM.nextFloat() * 2.0f - 1.0f) * 3.8f;
			targetParkPitch = (RANDOM.nextFloat() * 2.0f - 1.0f) * 3.0f;
		}
		ClientConditionState.parkinsonsYaw += (targetParkYaw - ClientConditionState.parkinsonsYaw) * 0.45f;
		ClientConditionState.parkinsonsPitch += (targetParkPitch - ClientConditionState.parkinsonsPitch) * 0.45f;
	}

	private static void tickSeizureCamera(LocalPlayer player) {
		if (!ClientConditionState.isSeizing()) {
			return;
		}
		float jitterYaw = (RANDOM.nextFloat() * 2.0f - 1.0f) * 14.0f;
		float jitterPitch = (RANDOM.nextFloat() * 2.0f - 1.0f) * 10.0f;
		player.setYRot(player.getYRot() + jitterYaw);
		player.setXRot(Math.clamp(player.getXRot() + jitterPitch, -90.0f, 90.0f));
	}

	private static void tickAcrophobia(LocalPlayer player) {
		if (!ClientConditionState.hasLocal(ConditionType.ACROPHOBIA) || player.getY() < 60.0) {
			return;
		}
		float intensity = Math.min(2.8f, 0.9f + (float) ((player.getY() - 60.0) * 0.03));
		float shakeYaw = (RANDOM.nextFloat() * 2.0f - 1.0f) * intensity;
		float shakePitch = (RANDOM.nextFloat() * 2.0f - 1.0f) * intensity * 0.7f;
		player.setYRot(player.getYRot() + shakeYaw);
		player.setXRot(Math.clamp(player.getXRot() + shakePitch, -90.0f, 90.0f));
	}

	private static void tickCerebralPalsy(Minecraft mc) {
		if (!ClientConditionState.hasLocal(ConditionType.CEREBRAL_PALSY) || ClientConditionState.isSeizing() || ClientConditionState.isAsleep()) {
			return;
		}
		if (ClientConditionState.cpMoveTicks <= 0 && RANDOM.nextInt(90) == 0) {
			ClientConditionState.cpMoveDir = RANDOM.nextInt(4);
			ClientConditionState.cpMoveTicks = 8 + RANDOM.nextInt(16);
		}
		if (RANDOM.nextInt(140) == 0) {
			ClientConditionState.cpJump = true;
		}
		if (RANDOM.nextInt(160) == 0) {
			ClientConditionState.cpSneak = true;
		}
		if (mc.gui.screen() == null && RANDOM.nextInt(180) == 0) {
			MinecraftActionInvoker invoker = (MinecraftActionInvoker) mc;
			if (RANDOM.nextBoolean()) {
				invoker.disabilities$invokeStartAttack();
			} else {
				invoker.disabilities$invokeStartUseItem();
			}
		}
	}

	private static void tickNarcolepsy() {
		if (!ClientConditionState.hasLocal(ConditionType.NARCOLEPSY)) {
			ClientConditionState.narcolepsyTicks = 0;
			return;
		}
		if (ClientConditionState.narcolepsyTicks <= 0 && RANDOM.nextInt(1400) == 0) {
			int duration = 80 + RANDOM.nextInt(80); // 4-8 seconds
			ClientConditionState.narcolepsyMaxTicks = duration;
			ClientConditionState.narcolepsyTicks = duration;
		}
	}

	private static void tickAdhd(LocalPlayer player) {
		if (!ClientConditionState.hasLocal(ConditionType.ADHD)) {
			return;
		}
		if (RANDOM.nextInt(140) == 0) {
			int current = player.getInventory().getSelectedSlot();
			int next = RANDOM.nextInt(9);
			if (next == current) {
				next = (next + 1) % 9;
			}
			player.getInventory().setSelectedSlot(next);
		}
	}

	private static void tickTourettes(Minecraft mc, LocalPlayer player) {
		if (!ClientConditionState.hasLocal(ConditionType.TOURETTES)) {
			return;
		}
		if (RANDOM.nextInt(130) == 0) {
			float jerkYaw = (RANDOM.nextFloat() * 2.0f - 1.0f) * 35.0f;
			float jerkPitch = (RANDOM.nextFloat() * 2.0f - 1.0f) * 20.0f;
			player.setYRot(player.getYRot() + jerkYaw);
			player.setXRot(Math.clamp(player.getXRot() + jerkPitch, -90.0f, 90.0f));
		}
		if (RANDOM.nextInt(400) == 0 && mc.getConnection() != null && !ClientConditionState.hasLocal(ConditionType.MUTE)) {
			String outburst = RandomWordGenerator.pickWord(mc, RANDOM);
			if (outburst != null && !outburst.isBlank()) {
				mc.getConnection().sendChat(outburst);
			}
		}
	}

	private static void tickSchizophrenia(Minecraft mc, LocalPlayer player) {
		if (!ClientConditionState.hasLocal(ConditionType.SCHIZOPHRENIA) || ClientConditionState.hasLocal(ConditionType.DEAF)) {
			return;
		}
		if (RANDOM.nextInt(160) == 0) {
			double angle = RANDOM.nextDouble() * Math.PI * 2.0;
			double dist = 2.5 + RANDOM.nextDouble() * 5.0;
			double sx = player.getX() + Math.cos(angle) * dist;
			double sy = player.getY();
			double sz = player.getZ() + Math.sin(angle) * dist;
			SoundEvent event = RANDOM.nextBoolean()
				? PHANTOM_STEPS[RANDOM.nextInt(PHANTOM_STEPS.length)]
				: PHANTOM_MOBS[RANDOM.nextInt(PHANTOM_MOBS.length)];
			mc.level.playLocalSound(sx, sy, sz, event, SoundSource.HOSTILE, 0.7f, 0.85f + RANDOM.nextFloat() * 0.3f, false);
		}
	}
}
