package io.github.disabilities.logic;

import io.github.disabilities.ConditionStore;
import io.github.disabilities.ConditionType;
import io.github.disabilities.Disabilities;
import io.github.disabilities.network.ModPackets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ConditionTickHandler {
	private static final Random RANDOM = new Random();
	private static final Map<UUID, PlayerRuntime> RUNTIMES = new ConcurrentHashMap<>();

	private static final Identifier MOD_SHORT_SCALE = Disabilities.id("short_scale");
	private static final Identifier MOD_SHORT_BLOCK_REACH = Disabilities.id("short_block_reach");
	private static final Identifier MOD_SHORT_ENTITY_REACH = Disabilities.id("short_entity_reach");
	private static final Identifier MOD_SHORT_JUMP = Disabilities.id("short_jump");
	private static final Identifier MOD_LEG_GROUND_SPEED = Disabilities.id("amputated_leg_speed");
	private static final Identifier MOD_ARTHRITIS_ATTACK = Disabilities.id("arthritis_attack");
	private static final Identifier MOD_ARTHRITIS_MINE = Disabilities.id("arthritis_mine");
	private static final Identifier MOD_BRITTLE_MULT = Disabilities.id("brittle_fall_mult");
	private static final Identifier MOD_BRITTLE_SAFE = Disabilities.id("brittle_safe_fall");
	private static final Identifier MOD_CANCER_HP = Disabilities.id("cancer_max_hp");

	public static final class PlayerRuntime {
		final CompulsionTracker ocd = new CompulsionTracker();
		String lastOcdSent = "";

		boolean wasUsingItem = false;
		int lastUseRemaining = 0;
		Item lastUsedItem = null;
		Item consumedThisTick = null;
		int lastCakeSlices = -1;
		boolean ateCakeThisTick = false;

		double lungAir = 300.0;
		int asthmaDamageTicker = 0;
		int pollenTicker = 0;
		int tetanusTicker = 0;
		int claustroTicker = 0;
		int nyctoTicker = 0;
		int aquaTicker = 0;
		int pyroTicker = 0;
		int agoraExposureTicks = 0;
		int acroTicker = 0;
		int arachnoTicker = 0;
		int cancerTicker = 0;
		int seizureTicks = 0;
		int seizureDamageTicker = 0;
		int seizureGraceAfterDeath = 0;
		int vertigoTicks = 0;
		int dementiaTicker = 0;

		long concealedSlotMask = 0L;
		long lastSentConcealedMask = 0L;
		final Item[] concealedSnapshot = new Item[36];
	}

	private ConditionTickHandler() {
	}

	public static PlayerRuntime runtime(UUID playerId) {
		return RUNTIMES.computeIfAbsent(playerId, id -> new PlayerRuntime());
	}

	public static void removePlayer(UUID playerId) {
		RUNTIMES.remove(playerId);
	}

	public static void onBlockBroken(ServerPlayer player, net.minecraft.world.level.block.Block block) {
		runtime(player.getUUID()).ocd.onBlockBroken(block);
	}

	public static void tickServer(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerLevel level = player.level();
			PlayerRuntime rt = runtime(player.getUUID());

			trackFoodConsumption(player, rt);
			applyShort(player);
			applyArthritis(player);
			applyBrittleBones(player);
			applyAmputatedLeg(player);
			applyAmputatedOffhand(player, level);
			applyBlind(player);
			applyCancer(player, rt);
			applyAsthma(player, level, rt);
			applyPollenAllergy(player, level, rt);
			applyTetanus(player, rt);
			applyDiabetes(player, level, rt);
			applyEatingDisorder(player, level, rt);
			applyLactoseIntolerance(player, level, rt);
			applyVertigo(player, rt);
			applySeizures(player, level, rt);
			applyDementia(player, rt);
			applyClaustrophobia(player, level, rt);
			applyNyctophobia(player, level, rt);
			applyAquaphobia(player, level, rt);
			applyPyrophobia(player, level, rt);
			applyAgoraphobia(player, level, rt);
			applyAcrophobia(player, rt);
			applyArachnophobia(player, level, rt);

			rt.ocd.tick(player, level);
			syncOcdLine(player, rt);
		}
	}

	public static void onAfterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}
		if (damageTaken <= 0.0f || blocked || player.isCreative() || player.isSpectator()) {
			return;
		}
		if (!ConditionStore.has(player.getUUID(), ConditionType.SEIZURES)) {
			return;
		}
		if (source.is(ModDamageTypes.SEIZURE)) {
			return;
		}
		PlayerRuntime rt = runtime(player.getUUID());
		if (rt.seizureTicks > 0 || rt.seizureGraceAfterDeath > 0) {
			return;
		}
		double chance = damageTaken >= 4.0f ? 0.25 : 0.12;
		if (RANDOM.nextDouble() < chance) {
			triggerSeizure(player, rt);
		}
	}

	public static void onContainerSlotClicked(ServerPlayer player, AbstractContainerMenu menu, int slotIndex, int button, ContainerInput input) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.DEMENTIA)) {
			return;
		}
		PlayerRuntime rt = runtime(player.getUUID());
		if (slotIndex >= 0 && slotIndex < menu.slots.size()) {
			Slot slot = menu.slots.get(slotIndex);
			if (slot.container == player.getInventory()) {
				revealSlot(rt, slot.getContainerSlot());
			}
		}
		if (input == ContainerInput.SWAP && button >= 0 && button < 9) {
			revealSlot(rt, button);
		}
		syncConcealedSlots(player, rt);
	}

	private static void trackFoodConsumption(ServerPlayer player, PlayerRuntime rt) {
		rt.consumedThisTick = null;
		rt.ateCakeThisTick = false;

		int cakeSlices = player.getStats().getValue(Stats.CUSTOM.get(Stats.EAT_CAKE_SLICE));
		if (rt.lastCakeSlices >= 0 && cakeSlices > rt.lastCakeSlices) {
			rt.ateCakeThisTick = true;
		}
		rt.lastCakeSlices = cakeSlices;

		boolean usingNow = player.isUsingItem();
		if (usingNow) {
			rt.wasUsingItem = true;
			rt.lastUseRemaining = player.getUseItemRemainingTicks();
			rt.lastUsedItem = player.getUseItem().getItem();
		} else {
			if (rt.wasUsingItem && rt.lastUseRemaining <= 2 && rt.lastUsedItem != null) {
				rt.consumedThisTick = rt.lastUsedItem;
			}
			rt.wasUsingItem = false;
			rt.lastUseRemaining = 0;
			rt.lastUsedItem = null;
		}
	}

	private static void applyShort(ServerPlayer player) {
		boolean active = ConditionStore.has(player.getUUID(), ConditionType.SHORT);
		syncModifier(player.getAttribute(Attributes.SCALE), MOD_SHORT_SCALE, active, -0.1667, AttributeModifier.Operation.ADD_VALUE);
		syncModifier(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), MOD_SHORT_BLOCK_REACH, active, -0.5, AttributeModifier.Operation.ADD_VALUE);
		syncModifier(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), MOD_SHORT_ENTITY_REACH, active, -0.5, AttributeModifier.Operation.ADD_VALUE);
		syncModifier(player.getAttribute(Attributes.JUMP_STRENGTH), MOD_SHORT_JUMP, active, -0.38, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	private static void applyArthritis(ServerPlayer player) {
		boolean active = ConditionStore.has(player.getUUID(), ConditionType.ARTHRITIS);
		syncModifier(player.getAttribute(Attributes.ATTACK_SPEED), MOD_ARTHRITIS_ATTACK, active, -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		syncModifier(player.getAttribute(Attributes.BLOCK_BREAK_SPEED), MOD_ARTHRITIS_MINE, active, -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	private static void applyBrittleBones(ServerPlayer player) {
		boolean active = ConditionStore.has(player.getUUID(), ConditionType.BRITTLE_BONES);
		syncModifier(player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER), MOD_BRITTLE_MULT, active, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		syncModifier(player.getAttribute(Attributes.SAFE_FALL_DISTANCE), MOD_BRITTLE_SAFE, active, -1.5, AttributeModifier.Operation.ADD_VALUE);
	}

	private static void applyAmputatedLeg(ServerPlayer player) {
		boolean active = ConditionStore.has(player.getUUID(), ConditionType.AMPUTATED_LEG);
		boolean groundLocked = active && player.onGround() && !player.isInWater() && !player.isPassenger() && !player.getAbilities().flying;
		syncModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOD_LEG_GROUND_SPEED, groundLocked, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	private static void applyAmputatedOffhand(ServerPlayer player, ServerLevel level) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.AMPUTATED_OFFHAND)) {
			return;
		}
		ItemStack offhand = player.getOffhandItem();
		if (offhand.isEmpty()) {
			return;
		}
		ItemStack copy = offhand.copy();
		player.getInventory().setItem(Inventory.SLOT_OFFHAND, ItemStack.EMPTY);
		if (!player.getInventory().add(copy)) {
			ItemEntity dropped = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), copy);
			dropped.setPickUpDelay(40);
			level.addFreshEntity(dropped);
		}
	}

	private static void applyBlind(ServerPlayer player) {
		if (ConditionStore.has(player.getUUID(), ConditionType.BLIND)) {
			player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0, true, false, false));
			if (player.isSprinting()) {
				player.setSprinting(false);
			}
		}
	}

	private static void applyCancer(ServerPlayer player, PlayerRuntime rt) {
		boolean active = ConditionStore.has(player.getUUID(), ConditionType.CANCER);
		AttributeInstance maxHp = player.getAttribute(Attributes.MAX_HEALTH);
		if (!active) {
			syncModifier(maxHp, MOD_CANCER_HP, false, 0.0, AttributeModifier.Operation.ADD_VALUE);
			rt.cancerTicker = 0;
			return;
		}

		double lost = ConditionStore.getCancerLoss(player.getUUID());
		if (player.isAlive() && !player.isCreative() && !player.isSpectator()) {
			rt.cancerTicker++;
			if (rt.cancerTicker >= 2700) { // ~2.25 minutes per 1 HP lost
				rt.cancerTicker = 0;
				double maxLoss = 14.0; // Floor at 6.0 HP (3 hearts)
				if (lost < maxLoss) {
					lost = Math.min(maxLoss, lost + 1.0);
					ConditionStore.setCancerLoss(player.getUUID(), lost);
				}
			}
		}

		syncModifier(maxHp, MOD_CANCER_HP, lost > 0.0, -lost, AttributeModifier.Operation.ADD_VALUE);
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	private static void applyAsthma(ServerPlayer player, ServerLevel level, PlayerRuntime rt) {
		int maxAir = player.getMaxAirSupply();
		if (!ConditionStore.has(player.getUUID(), ConditionType.ASTHMA) || player.isCreative() || player.isSpectator()) {
			rt.lungAir = maxAir;
			rt.asthmaDamageTicker = 0;
			return;
		}

		if (player.isUnderWater()) {
			rt.lungAir = player.getAirSupply();
			return;
		}

		if (player.isSprinting()) {
			rt.lungAir = Math.max(0.0, rt.lungAir - 2.5);
		} else {
			rt.lungAir = Math.min(maxAir, rt.lungAir + 1.5);
		}

		if (rt.lungAir < maxAir) {
			player.setAirSupply((int) Math.round(rt.lungAir));
		}

		if (rt.lungAir <= 0.0) {
			rt.asthmaDamageTicker++;
			if (rt.asthmaDamageTicker >= 20) {
				rt.asthmaDamageTicker = 0;
				player.hurtServer(level, ModDamageTypes.create(level, ModDamageTypes.ASTHMA), 2.0f);
			}
		} else {
			rt.asthmaDamageTicker = 0;
		}
	}

	private static void applyPollenAllergy(ServerPlayer player, ServerLevel level, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.POLLEN_ALLERGY) || player.isCreative() || player.isSpectator()) {
			rt.pollenTicker = 0;
			return;
		}
		rt.pollenTicker++;
		if (rt.pollenTicker < 10) {
			return;
		}
		rt.pollenTicker = 0;

		BlockPos center = player.blockPosition();
		boolean nearPollen = false;
		outer:
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					BlockState state = level.getBlockState(center.offset(dx, dy, dz));
					if (state.is(BlockTags.FLOWERS) || state.is(Blocks.SPORE_BLOSSOM) || state.is(Blocks.FLOWERING_AZALEA)
						|| state.is(Blocks.FLOWERING_AZALEA_LEAVES) || state.is(Blocks.BEE_NEST) || state.is(Blocks.BEEHIVE)) {
						nearPollen = true;
						break outer;
					}
				}
			}
		}
		if (nearPollen) {
			player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true, true));
		}
	}

	private static void applyDiabetes(ServerPlayer player, ServerLevel level, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.DIABETES) || player.isCreative() || player.isSpectator()) {
			return;
		}
		float damage = 0.0f;
		if (rt.ateCakeThisTick) {
			damage = FoodHazards.SUGAR_SEVERE;
		} else if (rt.consumedThisTick != null) {
			damage = FoodHazards.sugarDamage(rt.consumedThisTick);
		}
		if (damage > 0.0f) {
			player.hurtServer(level, ModDamageTypes.create(level, ModDamageTypes.DIABETES), damage);
		}
	}

	private static void applyTetanus(ServerPlayer player, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.TETANUS) || player.isCreative() || player.isSpectator()) {
			rt.tetanusTicker = 0;
			return;
		}
		rt.tetanusTicker++;
		if (rt.tetanusTicker < 10) {
			return;
		}
		rt.tetanusTicker = 0;

		if (isRawOreOrMineral(player.getMainHandItem()) || isRawOreOrMineral(player.getOffhandItem())) {
			player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true, true));
		}
	}

	private static boolean isRawOreOrMineral(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		Item item = stack.getItem();
		if (item == Items.RAW_IRON || item == Items.RAW_COPPER || item == Items.RAW_GOLD
			|| item == Items.RAW_IRON_BLOCK || item == Items.RAW_COPPER_BLOCK || item == Items.RAW_GOLD_BLOCK
			|| item == Items.NETHER_QUARTZ_ORE || item == Items.ANCIENT_DEBRIS) {
			return true;
		}
		return stack.is(ItemTags.IRON_ORES)
			|| stack.is(ItemTags.GOLD_ORES)
			|| stack.is(ItemTags.COPPER_ORES)
			|| stack.is(ItemTags.COAL_ORES)
			|| stack.is(ItemTags.DIAMOND_ORES)
			|| stack.is(ItemTags.EMERALD_ORES)
			|| stack.is(ItemTags.LAPIS_ORES)
			|| stack.is(ItemTags.REDSTONE_ORES);
	}

	private static void applyEatingDisorder(ServerPlayer player, ServerLevel level, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.EATING_DISORDER) || player.isCreative() || player.isSpectator()) {
			return;
		}
		boolean ateFood = rt.ateCakeThisTick
			|| (rt.consumedThisTick != null && rt.consumedThisTick.components().has(DataComponents.FOOD));
		if (ateFood) {
			player.hurtServer(level, ModDamageTypes.create(level, ModDamageTypes.EATING_DISORDER), 3.0f);
		}
	}

	private static void applyClaustrophobia(ServerPlayer player, ServerLevel level, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.CLAUSTROPHOBIA) || !player.isAlive() || player.isCreative() || player.isSpectator()) {
			rt.claustroTicker = 0;
			return;
		}
		BlockPos pos = player.blockPosition();
		boolean noSky = !level.canSeeSky(pos.above());
		int blockedSides = countBlockedSides(level, pos.above(), 2);
		if (noSky || blockedSides >= 3) {
			rt.claustroTicker++;
			if (rt.claustroTicker >= 25) {
				rt.claustroTicker = 0;
				player.hurtServer(level, ModDamageTypes.create(level, ModDamageTypes.CLAUSTROPHOBIA), 1.0f);
			}
		} else {
			rt.claustroTicker = 0;
		}
	}

	private static int countBlockedSides(ServerLevel level, BlockPos center, int range) {
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		int blocked = 0;
		for (int[] d : dirs) {
			for (int step = 1; step <= range; step++) {
				BlockPos check = center.offset(d[0] * step, 0, d[1] * step);
				if (!level.getBlockState(check).isAir()) {
					blocked++;
					break;
				}
			}
		}
		return blocked;
	}

	private static void applyNyctophobia(ServerPlayer player, ServerLevel level, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.NYCTOPHOBIA) || !player.isAlive() || player.isCreative() || player.isSpectator()) {
			rt.nyctoTicker = 0;
			return;
		}
		int light = level.getMaxLocalRawBrightness(player.blockPosition());
		if (light <= 4) {
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, true, false, true));
			rt.nyctoTicker++;
			if (rt.nyctoTicker >= 30) {
				rt.nyctoTicker = 0;
				player.hurtServer(level, ModDamageTypes.create(level, ModDamageTypes.NYCTOPHOBIA), 1.0f);
			}
		} else {
			rt.nyctoTicker = 0;
		}
	}

	private static void applyAquaphobia(ServerPlayer player, ServerLevel level, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.AQUAPHOBIA) || !player.isAlive() || player.isCreative() || player.isSpectator()) {
			rt.aquaTicker = 0;
			return;
		}
		if (player.isInWaterOrRain()) {
			rt.aquaTicker++;
			if (rt.aquaTicker >= 20) {
				rt.aquaTicker = 0;
				player.hurtServer(level, ModDamageTypes.create(level, ModDamageTypes.AQUAPHOBIA), 1.5f);
			}
		} else {
			rt.aquaTicker = 0;
		}
	}

	private static void applyPyrophobia(ServerPlayer player, ServerLevel level, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.PYROPHOBIA) || !player.isAlive() || player.isCreative() || player.isSpectator()) {
			rt.pyroTicker = 0;
			return;
		}
		rt.pyroTicker++;
		if (rt.pyroTicker < 10) {
			return;
		}
		rt.pyroTicker = 0;

		boolean nearFire = player.isOnFire();
		if (!nearFire) {
			BlockPos center = player.blockPosition();
			outer:
			for (int dx = -4; dx <= 4; dx++) {
				for (int dy = -2; dy <= 3; dy++) {
					for (int dz = -4; dz <= 4; dz++) {
						BlockState state = level.getBlockState(center.offset(dx, dy, dz));
						if (state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES)
							|| state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)
							|| state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
							|| state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
							|| state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH)
							|| state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)) {
							nearFire = true;
							break outer;
						}
					}
				}
			}
		}

		if (nearFire) {
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, true, true));
			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, true, true));
			player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0, false, true, true));
		}
	}

	private static void applyAgoraphobia(ServerPlayer player, ServerLevel level, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.AGORAPHOBIA) || !player.isAlive() || player.isCreative() || player.isSpectator()) {
			rt.agoraExposureTicks = 0;
			return;
		}
		BlockPos pos = player.blockPosition();
		boolean openOutdoors = level.canSeeSky(pos.above()) && countBlockedSides(level, pos.above(), 3) <= 1;
		if (openOutdoors) {
			rt.agoraExposureTicks = Math.min(200, rt.agoraExposureTicks + 1);
			if (rt.agoraExposureTicks >= 100 && rt.agoraExposureTicks % 20 == 0) {
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, true, true));
				player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true, true));
				player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0, false, true, true));
			}
		} else {
			rt.agoraExposureTicks = Math.max(0, rt.agoraExposureTicks - 4);
		}
	}

	private static void applyAcrophobia(ServerPlayer player, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.ACROPHOBIA) || !player.isAlive() || player.isCreative() || player.isSpectator()) {
			rt.acroTicker = 0;
			return;
		}
		rt.acroTicker++;
		if (rt.acroTicker < 15) {
			return;
		}
		rt.acroTicker = 0;
		if (player.getY() >= 65.0) {
			player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0, true, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, true, false, true));
		}
	}

	private static void applyArachnophobia(ServerPlayer player, ServerLevel level, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.ARACHNOPHOBIA) || !player.isAlive() || player.isCreative() || player.isSpectator()) {
			rt.arachnoTicker = 0;
			return;
		}
		rt.arachnoTicker++;
		if (rt.arachnoTicker < 10) {
			return;
		}
		rt.arachnoTicker = 0;

		List<Spider> nearby = level.getEntitiesOfClass(Spider.class, player.getBoundingBox().inflate(10.0), Spider::isAlive);
		if (!nearby.isEmpty()) {
			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, true, true));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, true, true));
		}
	}

	private static void applyLactoseIntolerance(ServerPlayer player, ServerLevel level, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.LACTOSE_INTOLERANCE) || player.isCreative() || player.isSpectator()) {
			return;
		}
		if (rt.ateCakeThisTick || (rt.consumedThisTick != null && FoodHazards.isDairy(rt.consumedThisTick))) {
			player.hurtServer(level, ModDamageTypes.create(level, ModDamageTypes.LACTOSE), Float.MAX_VALUE);
		}
	}

	private static void applyVertigo(ServerPlayer player, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.VERTIGO) || !player.isAlive()) {
			if (rt.vertigoTicks > 0) {
				rt.vertigoTicks = 0;
				ServerPlayNetworking.send(player, new ModPackets.EpisodePacket(ModPackets.EpisodePacket.TYPE_VERTIGO, 0, 1));
			}
			return;
		}

		if (rt.vertigoTicks > 0) {
			rt.vertigoTicks--;
			if (rt.vertigoTicks % 40 == 0) {
				player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0, true, false, false));
			}
			return;
		}

		// Roughly once every 75 seconds on average
		if (RANDOM.nextInt(1500) == 0) {
			int duration = 160 + RANDOM.nextInt(160); // 8-16 seconds
			int dir = RANDOM.nextBoolean() ? 1 : -1;
			rt.vertigoTicks = duration;
			player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, duration + 40, 0, true, false, false));
			ServerPlayNetworking.send(player, new ModPackets.EpisodePacket(ModPackets.EpisodePacket.TYPE_VERTIGO, duration, dir));
		}
	}

	private static void applySeizures(ServerPlayer player, ServerLevel level, PlayerRuntime rt) {
		if (!player.isAlive()) {
			if (rt.seizureTicks > 0) {
				stopSeizure(player, rt);
			}
			rt.seizureGraceAfterDeath = 100;
			return;
		}
		if (rt.seizureGraceAfterDeath > 0) {
			rt.seizureGraceAfterDeath--;
		}

		if (!ConditionStore.has(player.getUUID(), ConditionType.SEIZURES) || player.isCreative() || player.isSpectator()) {
			if (rt.seizureTicks > 0) {
				stopSeizure(player, rt);
			}
			return;
		}

		if (rt.seizureTicks > 0) {
			rt.seizureTicks--;
			rt.seizureDamageTicker++;
			if (rt.seizureDamageTicker >= 20) {
				rt.seizureDamageTicker = 0;
				player.hurtServer(level, ModDamageTypes.create(level, ModDamageTypes.SEIZURE), 1.5f);
			}
			if (rt.seizureTicks == 0) {
				stopSeizure(player, rt);
			}
			return;
		}

		if (rt.seizureGraceAfterDeath > 0) {
			return;
		}

		boolean lowHunger = player.getFoodData().getFoodLevel() <= 10;
		boolean harmfulEffect = hasHarmfulStatus(player);
		if ((lowHunger || harmfulEffect) && RANDOM.nextInt(200) == 0) {
			triggerSeizure(player, rt);
		}
	}

	private static boolean hasHarmfulStatus(ServerPlayer player) {
		for (MobEffectInstance inst : player.getActiveEffects()) {
			if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				return true;
			}
		}
		return false;
	}

	private static void triggerSeizure(ServerPlayer player, PlayerRuntime rt) {
		int duration = 80 + RANDOM.nextInt(80); // 4-8 seconds
		rt.seizureTicks = duration;
		rt.seizureDamageTicker = 0;
		ServerPlayNetworking.send(player, new ModPackets.EpisodePacket(ModPackets.EpisodePacket.TYPE_SEIZURE, duration, 0));
	}

	private static void stopSeizure(ServerPlayer player, PlayerRuntime rt) {
		rt.seizureTicks = 0;
		rt.seizureDamageTicker = 0;
		ServerPlayNetworking.send(player, new ModPackets.EpisodePacket(ModPackets.EpisodePacket.TYPE_SEIZURE, 0, 0));
	}

	private static void applyDementia(ServerPlayer player, PlayerRuntime rt) {
		if (!ConditionStore.has(player.getUUID(), ConditionType.DEMENTIA) || player.isCreative() || player.isSpectator()) {
			if (rt.concealedSlotMask != 0L) {
				rt.concealedSlotMask = 0L;
				syncConcealedSlots(player, rt);
			}
			rt.dementiaTicker = 0;
			return;
		}

		Inventory inv = player.getInventory();
		// Reveal any hidden slot whose item changed
		for (int i = 0; i < 36; i++) {
			if ((rt.concealedSlotMask & (1L << i)) != 0L) {
				Item current = inv.getItem(i).getItem();
				if (inv.getItem(i).isEmpty() || current != rt.concealedSnapshot[i]) {
					revealSlot(rt, i);
				}
			}
		}

		rt.dementiaTicker++;
		if (rt.dementiaTicker >= 1200) { // Every 60 seconds
			rt.dementiaTicker = 0;
			shuffleAndConceal(inv, rt);
		}

		syncConcealedSlots(player, rt);
	}

	private static void shuffleAndConceal(Inventory inv, PlayerRuntime rt) {
		List<Integer> occupied = new ArrayList<>();
		for (int i = 0; i < 36; i++) {
			if (!inv.getItem(i).isEmpty()) {
				occupied.add(i);
			}
		}
		if (occupied.size() < 2) {
			return;
		}

		List<ItemStack> stacks = new ArrayList<>(occupied.size());
		for (int idx : occupied) {
			stacks.add(inv.getItem(idx).copy());
		}
		Collections.shuffle(stacks, RANDOM);

		for (int i = 0; i < occupied.size(); i++) {
			int slot = occupied.get(i);
			ItemStack stack = stacks.get(i);
			inv.setItem(slot, stack);
			if (RANDOM.nextDouble() < 0.65) {
				rt.concealedSlotMask |= (1L << slot);
				rt.concealedSnapshot[slot] = stack.getItem();
			}
		}
		inv.setChanged();
	}

	private static void revealSlot(PlayerRuntime rt, int slot) {
		if (slot >= 0 && slot < 36) {
			rt.concealedSlotMask &= ~(1L << slot);
			rt.concealedSnapshot[slot] = null;
		}
	}

	private static void syncConcealedSlots(ServerPlayer player, PlayerRuntime rt) {
		if (rt.concealedSlotMask != rt.lastSentConcealedMask) {
			rt.lastSentConcealedMask = rt.concealedSlotMask;
			ServerPlayNetworking.send(player, new ModPackets.ConcealedSlotsPacket(rt.concealedSlotMask));
		}
	}

	private static void syncOcdLine(ServerPlayer player, PlayerRuntime rt) {
		String line = rt.ocd.getDisplayLine();
		if (!line.equals(rt.lastOcdSent)) {
			rt.lastOcdSent = line;
			ServerPlayNetworking.send(player, new ModPackets.CompulsionPacket(line));
		}
	}

	private static void syncModifier(AttributeInstance attr, Identifier id, boolean enabled, double amount, AttributeModifier.Operation op) {
		if (attr == null) {
			return;
		}
		AttributeModifier existing = attr.getModifier(id);
		if (!enabled) {
			if (existing != null) {
				attr.removeModifier(id);
			}
			return;
		}
		if (existing == null || Double.compare(existing.amount(), amount) != 0 || existing.operation() != op) {
			attr.removeModifier(id);
			attr.addTransientModifier(new AttributeModifier(id, amount, op));
		}
	}
}
