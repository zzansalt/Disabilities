package io.github.disabilities.logic;

import io.github.disabilities.ConditionStore;
import io.github.disabilities.ConditionType;
import io.github.disabilities.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class CompulsionTracker {
	private enum TaskType {
		PLACE,
		MINE,
		JUMP,
		CROUCH
	}

	private static final Random RANDOM = new Random();
	private static final Set<Block> MINEABLE_TARGETS = Set.of(
		Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.STONE, Blocks.COBBLESTONE,
		Blocks.DEEPSLATE, Blocks.SAND, Blocks.GRAVEL, Blocks.NETHERRACK,
		Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.SPRUCE_LOG, Blocks.JUNGLE_LOG,
		Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES,
		Blocks.SPRUCE_LEAVES, Blocks.SNOW_BLOCK, Blocks.TERRACOTTA, Blocks.CLAY
	);

	private String displayLine = "";
	private boolean wasConditionActive = false;
	private int cooldownTicks = 0;
	private int violationFlashTicks = 0;

	private TaskType currentTask = null;
	private Item placeItem = null;
	private Block mineBlock = null;
	private int requiredCount = 0;
	private int baselineStat = 0;
	private int remainingTicks = 0;
	private int holdGraceTicks = 0;
	private boolean punishingFailure = false;
	private int punishDamageTicker = 0;

	private int brokenCount = 0;
	private int crouchCount = 0;
	private boolean wasCrouching = false;

	public String getDisplayLine() {
		return displayLine;
	}

	public void onBlockBroken(Block block) {
		if (currentTask == TaskType.MINE && block == mineBlock) {
			brokenCount++;
		}
	}

	public void tick(ServerPlayer player, ServerLevel level) {
		boolean active = ConditionStore.has(player.getUUID(), ConditionType.OCD);
		if (!active) {
			if (wasConditionActive) {
				clearTask();
				displayLine = "";
				wasConditionActive = false;
			}
			return;
		}
		if (!wasConditionActive) {
			wasConditionActive = true;
			scheduleCooldown();
		}

		if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
			clearTask();
			displayLine = "";
			return;
		}

		if (violationFlashTicks > 0) {
			violationFlashTicks--;
			if (violationFlashTicks == 0) {
				displayLine = "";
			}
			return;
		}

		boolean crouchingNow = player.isShiftKeyDown();
		if (currentTask == TaskType.CROUCH && crouchingNow && !wasCrouching) {
			crouchCount++;
		}
		wasCrouching = crouchingNow;

		if (currentTask == null) {
			if (cooldownTicks > 0) {
				cooldownTicks--;
				displayLine = "";
				return;
			}
			if (!assignRandomTask(player, level)) {
				cooldownTicks = 100;
				displayLine = "";
				return;
			}
		}

		int progress = currentProgress(player);

		if (progress > requiredCount) {
			// Over-completed the compulsion
			player.hurtServer(level, ModDamageTypes.create(level, ModDamageTypes.OCD_OVERDID), 6.0f);
			clearTask();
			displayLine = ModConfig.isKorean() ? "강박장애: 횟수를 초과했습니다!" : "OCD: You did too much!";
			violationFlashTicks = 60;
			scheduleCooldown();
			return;
		}

		if (progress == requiredCount) {
			punishingFailure = false;
			if (holdGraceTicks < 40) {
				holdGraceTicks++;
				displayLine = buildHudText(progress, true);
				return;
			}
			// Successfully held exact count without overdoing
			clearTask();
			displayLine = "";
			scheduleCooldown();
			return;
		}

		holdGraceTicks = 0;

		if (remainingTicks > 0) {
			remainingTicks--;
			displayLine = buildHudText(progress, false);
		} else {
			punishingFailure = true;
			punishDamageTicker++;
			displayLine = (ModConfig.isKorean() ? "강박 실패! " : "OCD FAILED! ") + buildHudText(progress, false);
			if (punishDamageTicker >= 20) {
				punishDamageTicker = 0;
				player.hurtServer(level, ModDamageTypes.create(level, ModDamageTypes.OCD), 2.0f);
			}
		}
	}

	private int currentProgress(ServerPlayer player) {
		if (currentTask == null) {
			return 0;
		}
		return switch (currentTask) {
			case PLACE -> Math.max(0, player.getStats().getValue(Stats.ITEM_USED.get(placeItem)) - baselineStat);
			case MINE -> brokenCount;
			case JUMP -> Math.max(0, player.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP)) - baselineStat);
			case CROUCH -> crouchCount;
		};
	}

	private String buildHudText(int progress, boolean holding) {
		boolean ko = ModConfig.isKorean();
		String action = switch (currentTask) {
			case PLACE -> (ko ? "설치: " : "Place ") + placeItem.getName(new ItemStack(placeItem)).getString();
			case MINE -> (ko ? "채굴: " : "Mine ") + mineBlock.getName().getString();
			case JUMP -> ko ? "점프" : "Jump";
			case CROUCH -> ko ? "웅크리기" : "Crouch";
		};
		String prefix = ko ? "강박: " : "OCD: ";
		if (holding) {
			return prefix + action + " (" + progress + "/" + requiredCount + ") - " + (ko ? "멈추세요!" : "STOP!");
		}
		int seconds = (remainingTicks + 19) / 20;
		return prefix + action + " (" + progress + "/" + requiredCount + ") [" + seconds + (ko ? "초]" : "s]");
	}

	private boolean assignRandomTask(ServerPlayer player, ServerLevel level) {
		List<TaskType> candidates = new ArrayList<>(List.of(TaskType.PLACE, TaskType.MINE, TaskType.JUMP, TaskType.CROUCH));
		while (!candidates.isEmpty()) {
			TaskType pick = candidates.remove(RANDOM.nextInt(candidates.size()));
			boolean ok = switch (pick) {
				case PLACE -> setupPlaceTask(player);
				case MINE -> setupMineTask(player, level);
				case JUMP -> setupJumpTask(player);
				case CROUCH -> setupCrouchTask();
			};
			if (ok) {
				return true;
			}
		}
		return false;
	}

	private boolean setupPlaceTask(ServerPlayer player) {
		Inventory inv = player.getInventory();
		List<Item> placeables = new ArrayList<>();
		for (int i = 0; i < 36; i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty() && stack.getCount() >= 3 && stack.getItem() instanceof BlockItem) {
				placeables.add(stack.getItem());
			}
		}
		if (placeables.isEmpty()) {
			return false;
		}
		Item chosen = placeables.get(RANDOM.nextInt(placeables.size()));
		int available = 0;
		for (int i = 0; i < 36; i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.is(chosen)) {
				available += stack.getCount();
			}
		}
		int maxReq = Math.min(6, available);
		if (maxReq < 2) {
			return false;
		}
		currentTask = TaskType.PLACE;
		placeItem = chosen;
		requiredCount = 2 + RANDOM.nextInt(maxReq - 1);
		baselineStat = player.getStats().getValue(Stats.ITEM_USED.get(chosen));
		remainingTicks = 25 * 20;
		holdGraceTicks = 0;
		return true;
	}

	private boolean setupMineTask(ServerPlayer player, ServerLevel level) {
		BlockPos origin = player.blockPosition();
		Set<Block> nearby = new HashSet<>();
		for (int dx = -6; dx <= 6; dx++) {
			for (int dy = -3; dy <= 4; dy++) {
				for (int dz = -6; dz <= 6; dz++) {
					BlockState state = level.getBlockState(origin.offset(dx, dy, dz));
					if (MINEABLE_TARGETS.contains(state.getBlock())) {
						nearby.add(state.getBlock());
					}
				}
			}
		}
		if (nearby.isEmpty()) {
			return false;
		}
		List<Block> list = new ArrayList<>(nearby);
		currentTask = TaskType.MINE;
		mineBlock = list.get(RANDOM.nextInt(list.size()));
		requiredCount = 2 + RANDOM.nextInt(4);
		brokenCount = 0;
		remainingTicks = 30 * 20;
		holdGraceTicks = 0;
		return true;
	}

	private boolean setupJumpTask(ServerPlayer player) {
		currentTask = TaskType.JUMP;
		requiredCount = 3 + RANDOM.nextInt(5);
		baselineStat = player.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP));
		remainingTicks = 20 * 20;
		holdGraceTicks = 0;
		return true;
	}

	private boolean setupCrouchTask() {
		currentTask = TaskType.CROUCH;
		requiredCount = 3 + RANDOM.nextInt(6);
		crouchCount = 0;
		remainingTicks = 20 * 20;
		holdGraceTicks = 0;
		return true;
	}

	private void scheduleCooldown() {
		cooldownTicks = 400 + RANDOM.nextInt(600); // 20-50 seconds
	}

	private void clearTask() {
		currentTask = null;
		placeItem = null;
		mineBlock = null;
		requiredCount = 0;
		baselineStat = 0;
		remainingTicks = 0;
		holdGraceTicks = 0;
		punishingFailure = false;
		punishDamageTicker = 0;
		brokenCount = 0;
		crouchCount = 0;
	}
}
