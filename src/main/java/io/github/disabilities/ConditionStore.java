package io.github.disabilities;

import io.github.disabilities.config.ModConfig;
import io.github.disabilities.network.ModPackets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ConditionStore {
	public static final int DEFAULT_INTERVAL_TICKS = 10 * 60 * 20; // 10 minutes
	private static final Random RANDOM = new Random();

	private static final Map<UUID, EnumSet<ConditionType>> ACTIVE_CONDITIONS = new ConcurrentHashMap<>();
	private static final Map<UUID, Double> CANCER_HP_LOST = new ConcurrentHashMap<>();

	private static int intervalTicks = DEFAULT_INTERVAL_TICKS;
	private static int remainingTicks = DEFAULT_INTERVAL_TICKS;
	private static boolean timerEnabled = true;

	private static Path storagePath;
	private static boolean dirty = false;
	private static int syncTicker = 0;
	private static int saveTicker = 0;

	private ConditionStore() {
	}

	public static boolean has(UUID playerId, ConditionType condition) {
		EnumSet<ConditionType> set = ACTIVE_CONDITIONS.get(playerId);
		return set != null && set.contains(condition);
	}

	public static Set<ConditionType> getActive(UUID playerId) {
		EnumSet<ConditionType> set = ACTIVE_CONDITIONS.get(playerId);
		return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
	}

	private static EnumSet<ConditionType> getOrCreate(UUID playerId) {
		return ACTIVE_CONDITIONS.computeIfAbsent(playerId, id -> EnumSet.noneOf(ConditionType.class));
	}

	public static double getCancerLoss(UUID playerId) {
		return CANCER_HP_LOST.getOrDefault(playerId, 0.0);
	}

	public static void setCancerLoss(UUID playerId, double loss) {
		if (loss <= 0.0) {
			CANCER_HP_LOST.remove(playerId);
		} else {
			CANCER_HP_LOST.put(playerId, loss);
		}
		markDirty();
	}

	public static int getRemainingTicks() {
		return remainingTicks;
	}

	public static boolean isTimerEnabled() {
		return timerEnabled;
	}

	public static void setTimerEnabled(boolean enabled) {
		timerEnabled = enabled;
		markDirty();
	}

	public static void setIntervalMinutes(double minutes) {
		int ticks = Math.max(20, (int) Math.round(minutes * 60.0 * 20.0));
		intervalTicks = ticks;
		remainingTicks = ticks;
		timerEnabled = true;
		markDirty();
	}

	public static boolean grant(UUID playerId, ConditionType condition) {
		boolean added = getOrCreate(playerId).add(condition);
		if (added) {
			markDirty();
		}
		return added;
	}

	public static int grantAll(UUID playerId) {
		EnumSet<ConditionType> set = getOrCreate(playerId);
		int added = 0;
		for (ConditionType type : ConditionType.values()) {
			if (set.add(type)) {
				added++;
			}
		}
		if (added > 0) {
			markDirty();
		}
		return added;
	}

	public static ConditionType grantRandom(UUID playerId) {
		EnumSet<ConditionType> current = getOrCreate(playerId);
		List<ConditionType> available = new ArrayList<>();
		for (ConditionType type : ConditionType.values()) {
			if (!current.contains(type)) {
				available.add(type);
			}
		}
		if (available.isEmpty()) {
			return null;
		}
		ConditionType chosen = available.get(RANDOM.nextInt(available.size()));
		current.add(chosen);
		markDirty();
		return chosen;
	}

	public static boolean revoke(UUID playerId, ConditionType condition) {
		EnumSet<ConditionType> set = ACTIVE_CONDITIONS.get(playerId);
		if (set == null || !set.remove(condition)) {
			return false;
		}
		if (condition == ConditionType.CANCER) {
			CANCER_HP_LOST.remove(playerId);
		}
		markDirty();
		return true;
	}

	public static int revokeAll(UUID playerId) {
		EnumSet<ConditionType> set = ACTIVE_CONDITIONS.get(playerId);
		if (set == null || set.isEmpty()) {
			return 0;
		}
		int count = set.size();
		set.clear();
		CANCER_HP_LOST.remove(playerId);
		markDirty();
		return count;
	}

	public static ConditionType revokeRandom(UUID playerId) {
		EnumSet<ConditionType> set = ACTIVE_CONDITIONS.get(playerId);
		if (set == null || set.isEmpty()) {
			return null;
		}
		List<ConditionType> list = new ArrayList<>(set);
		ConditionType chosen = list.get(RANDOM.nextInt(list.size()));
		revoke(playerId, chosen);
		return chosen;
	}

	public static void resetAll() {
		ACTIVE_CONDITIONS.clear();
		CANCER_HP_LOST.clear();
		intervalTicks = DEFAULT_INTERVAL_TICKS;
		remainingTicks = DEFAULT_INTERVAL_TICKS;
		timerEnabled = true;
		markDirty();
	}

	public static void markDirty() {
		dirty = true;
	}

	public static void notifyPlayer(ServerPlayer player, ConditionType condition, String verb) {
		if (has(player.getUUID(), ConditionType.ANOSOGNOSIA)) {
			return;
		}
		Component msg;
		if (ModConfig.isKorean()) {
			msg = Component.literal("[Disabilities] ")
				.withStyle(ChatFormatting.DARK_RED)
				.append(Component.literal(condition.getDisplayName()).withStyle(condition.color, ChatFormatting.BOLD))
				.append(Component.literal(" 장애가 발현되었습니다 - " + condition.getDescription()).withStyle(ChatFormatting.GRAY));
		} else {
			msg = Component.literal("[Disabilities] ")
				.withStyle(ChatFormatting.DARK_RED)
				.append(Component.literal("You " + verb + " ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(condition.getDisplayName()).withStyle(condition.color, ChatFormatting.BOLD))
				.append(Component.literal(" - " + condition.getDescription()).withStyle(ChatFormatting.GRAY));
		}
		player.sendSystemMessage(msg);
	}

	public static void tick(MinecraftServer server) {
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		if (timerEnabled && !players.isEmpty()) {
			remainingTicks--;
			if (remainingTicks <= 0) {
				remainingTicks = intervalTicks;
				for (ServerPlayer player : players) {
					ConditionType granted = grantRandom(player.getUUID());
					if (granted != null) {
						notifyPlayer(player, granted, "developed");
					}
				}
				syncAll(server);
			}
		}

		syncTicker++;
		if (syncTicker >= 20) {
			syncTicker = 0;
			syncAll(server);
		}

		saveTicker++;
		if (saveTicker >= 400) {
			saveTicker = 0;
			if (dirty) {
				save();
			}
		}
	}

	public static void syncAll(MinecraftServer server) {
		List<ServerPlayer> online = server.getPlayerList().getPlayers();
		if (online.isEmpty()) {
			return;
		}
		List<UUID> ids = new ArrayList<>(ACTIVE_CONDITIONS.size());
		List<Long> masks = new ArrayList<>(ACTIVE_CONDITIONS.size());
		for (Map.Entry<UUID, EnumSet<ConditionType>> entry : ACTIVE_CONDITIONS.entrySet()) {
			long mask = ConditionAccess.encodeMask(entry.getValue());
			if (mask != 0L) {
				ids.add(entry.getKey());
				masks.add(mask);
			}
		}
		ModPackets.SyncStatePacket packet = new ModPackets.SyncStatePacket(ids, masks, remainingTicks, timerEnabled);
		for (ServerPlayer player : online) {
			ServerPlayNetworking.send(player, packet);
		}
	}

	public static void load(MinecraftServer server) {
		ACTIVE_CONDITIONS.clear();
		CANCER_HP_LOST.clear();
		intervalTicks = DEFAULT_INTERVAL_TICKS;
		remainingTicks = DEFAULT_INTERVAL_TICKS;
		timerEnabled = true;
		dirty = false;
		syncTicker = 0;
		saveTicker = 0;

		storagePath = server.getWorldPath(LevelResource.ROOT).resolve("disabilities_state.txt");
		if (!Files.exists(storagePath)) {
			return;
		}

		try {
			List<String> lines = Files.readAllLines(storagePath, StandardCharsets.UTF_8);
			for (String raw : lines) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				int eq = line.indexOf('=');
				if (eq < 0) {
					continue;
				}
				String key = line.substring(0, eq).trim();
				String val = line.substring(eq + 1).trim();
				switch (key) {
					case "interval" -> intervalTicks = Math.max(20, parseInt(val, DEFAULT_INTERVAL_TICKS));
					case "remaining" -> remainingTicks = Math.max(1, parseInt(val, intervalTicks));
					case "enabled" -> timerEnabled = Boolean.parseBoolean(val);
					case "player" -> loadPlayerEntry(val);
					default -> {
					}
				}
			}
		} catch (IOException e) {
			Disabilities.LOGGER.error("Failed to read disabilities state file", e);
		}
	}

	private static void loadPlayerEntry(String value) {
		String[] parts = value.split("\\|", -1);
		if (parts.length < 2) {
			return;
		}
		try {
			UUID uuid = UUID.fromString(parts[0].trim());
			EnumSet<ConditionType> set = EnumSet.noneOf(ConditionType.class);
			if (!parts[1].isBlank()) {
				for (String token : parts[1].split(",")) {
					ConditionType type = ConditionType.parse(token);
					if (type != null) {
						set.add(type);
					}
				}
			}
			if (!set.isEmpty()) {
				ACTIVE_CONDITIONS.put(uuid, set);
			}
			if (parts.length >= 3 && !parts[2].isBlank()) {
				double loss = Double.parseDouble(parts[2].trim());
				if (loss > 0.0) {
					CANCER_HP_LOST.put(uuid, loss);
				}
			}
		} catch (IllegalArgumentException ignored) {
		}
	}

	public static void save() {
		if (storagePath == null) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("interval=").append(intervalTicks).append('\n');
		sb.append("remaining=").append(remainingTicks).append('\n');
		sb.append("enabled=").append(timerEnabled).append('\n');
		for (Map.Entry<UUID, EnumSet<ConditionType>> entry : ACTIVE_CONDITIONS.entrySet()) {
			EnumSet<ConditionType> set = entry.getValue();
			double cancerLoss = CANCER_HP_LOST.getOrDefault(entry.getKey(), 0.0);
			if (set.isEmpty() && cancerLoss <= 0.0) {
				continue;
			}
			String joined = set.stream().map(Enum::name).collect(Collectors.joining(","));
			sb.append("player=").append(entry.getKey()).append('|').append(joined).append('|').append(cancerLoss).append('\n');
		}
		try {
			Files.writeString(storagePath, sb.toString(), StandardCharsets.UTF_8);
			dirty = false;
		} catch (IOException e) {
			Disabilities.LOGGER.error("Failed to save disabilities state file", e);
		}
	}

	private static int parseInt(String raw, int fallback) {
		try {
			return Integer.parseInt(raw);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
