package io.github.disabilities.client;

import io.github.disabilities.ConditionAccess;
import io.github.disabilities.ConditionType;
import io.github.disabilities.client.render.HallucinationMobManager;
import io.github.disabilities.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ClientConditionState {
	private static int remainingTicks = 0;
	private static boolean timerEnabled = true;
	private static String compulsionLine = "";
	private static int seizureTicks = 0;
	private static int vertigoTicks = 0;
	private static int vertigoDirection = 1;
	private static long concealedSlotMask = 0L;

	private static boolean listVisible = true;
	private static boolean dyslexiaExempt = false;

	// Server-synced config values
	private static boolean serverConfigSynced = false;
	private static int serverAcrophobiaY = ModConfig.DEFAULT_ACROPHOBIA_Y;
	private static List<String> serverTouretteMessages = List.of();

	// Cerebral Palsy involuntary inputs
	static int cpMoveDir = -1; // 0=fwd, 1=back, 2=left, 3=right
	static int cpMoveTicks = 0;
	static boolean cpJump = false;
	static boolean cpSneak = false;

	// Narcolepsy blackout episode
	static int narcolepsyTicks = 0;
	static int narcolepsyMaxTicks = 100;

	// Parkinson's continuous tremor offsets (in degrees)
	static float parkinsonsYaw = 0.0f;
	static float parkinsonsPitch = 0.0f;

	private ClientConditionState() {
	}

	public static void setTimer(int ticks, boolean enabled) {
		remainingTicks = Math.max(0, ticks);
		timerEnabled = enabled;
	}

	public static int getRemainingTicks() {
		return remainingTicks;
	}

	public static boolean isTimerEnabled() {
		return timerEnabled;
	}

	public static void setCompulsionLine(String text) {
		compulsionLine = text == null ? "" : text;
	}

	public static String getCompulsionLine() {
		return compulsionLine;
	}

	public static void setSeizureTicks(int ticks) {
		seizureTicks = Math.max(0, ticks);
	}

	public static boolean isSeizing() {
		return seizureTicks > 0;
	}

	public static void setVertigo(int ticks, int dir) {
		vertigoTicks = Math.max(0, ticks);
		vertigoDirection = dir == 0 ? 1 : dir;
	}

	public static int getVertigoTicks() {
		return vertigoTicks;
	}

	public static int getVertigoDirection() {
		return vertigoDirection;
	}

	public static void setConcealedSlotMask(long mask) {
		concealedSlotMask = mask;
	}

	public static boolean isSlotConcealed(Container container, int slotIndex) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || container != mc.player.getInventory()) {
			return false;
		}
		if (!hasLocal(ConditionType.DEMENTIA) || slotIndex < 0 || slotIndex >= 36) {
			return false;
		}
		return (concealedSlotMask & (1L << slotIndex)) != 0L;
	}

	public static boolean hasLocal(ConditionType condition) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return false;
		}
		return ConditionAccess.clientHas(mc.player.getUUID(), condition);
	}

	public static boolean hasFor(UUID playerId, ConditionType condition) {
		return ConditionAccess.clientHas(playerId, condition);
	}

	public static Set<ConditionType> localConditions() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return Collections.emptySet();
		}
		return ConditionAccess.clientConditions(mc.player.getUUID());
	}

	public static boolean isListVisible() {
		return listVisible;
	}

	public static void toggleListVisible() {
		listVisible = !listVisible;
	}

	public static boolean isDyslexiaExempt() {
		return dyslexiaExempt;
	}

	public static void setDyslexiaExempt(boolean exempt) {
		dyslexiaExempt = exempt;
	}

	public static boolean isCpForward() {
		return cpMoveTicks > 0 && cpMoveDir == 0;
	}

	public static boolean isCpBackward() {
		return cpMoveTicks > 0 && cpMoveDir == 1;
	}

	public static boolean isCpLeft() {
		return cpMoveTicks > 0 && cpMoveDir == 2;
	}

	public static boolean isCpRight() {
		return cpMoveTicks > 0 && cpMoveDir == 3;
	}

	public static boolean pollCpJump() {
		boolean v = cpJump;
		cpJump = false;
		return v;
	}

	public static boolean pollCpSneak() {
		boolean v = cpSneak;
		cpSneak = false;
		return v;
	}

	public static boolean isAsleep() {
		return narcolepsyTicks > 0;
	}

	public static float getNarcolepsyOverlayAlpha() {
		if (narcolepsyTicks <= 0) {
			return 0.0f;
		}
		int elapsed = narcolepsyMaxTicks - narcolepsyTicks;
		if (elapsed < 15) {
			return elapsed / 15.0f;
		}
		if (narcolepsyTicks < 15) {
			return narcolepsyTicks / 15.0f;
		}
		return 1.0f;
	}

	public static float getParkinsonsYaw() {
		return parkinsonsYaw;
	}

	public static float getParkinsonsPitch() {
		return parkinsonsPitch;
	}

	static void decrementTimers() {
		if (seizureTicks > 0) {
			seizureTicks--;
		}
		if (vertigoTicks > 0) {
			vertigoTicks--;
		}
		if (cpMoveTicks > 0) {
			cpMoveTicks--;
		}
		if (narcolepsyTicks > 0) {
			narcolepsyTicks--;
		}
	}

	public static void setServerConfig(int acrophobiaY, List<String> touretteMessages) {
		serverConfigSynced = true;
		serverAcrophobiaY = acrophobiaY;
		serverTouretteMessages = touretteMessages == null ? List.of() : List.copyOf(touretteMessages);
	}

	public static int getEffectiveAcrophobiaY() {
		return serverConfigSynced ? serverAcrophobiaY : ModConfig.getAcrophobiaY();
	}

	public static List<String> getEffectiveTouretteMessages() {
		if (serverConfigSynced && !serverTouretteMessages.isEmpty()) {
			return serverTouretteMessages;
		}
		return ModConfig.getTouretteMessages();
	}

	public static void reset() {
		ConditionAccess.clearClientCache();
		HallucinationMobManager.clear();
		serverConfigSynced = false;
		serverAcrophobiaY = ModConfig.DEFAULT_ACROPHOBIA_Y;
		serverTouretteMessages = List.of();
		remainingTicks = 0;
		timerEnabled = true;
		compulsionLine = "";
		seizureTicks = 0;
		vertigoTicks = 0;
		vertigoDirection = 1;
		concealedSlotMask = 0L;
		cpMoveDir = -1;
		cpMoveTicks = 0;
		cpJump = false;
		cpSneak = false;
		narcolepsyTicks = 0;
		parkinsonsYaw = 0.0f;
		parkinsonsPitch = 0.0f;
	}
}
