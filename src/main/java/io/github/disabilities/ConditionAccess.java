package io.github.disabilities;

import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ConditionAccess {
	private static final Map<UUID, Set<ConditionType>> CLIENT_CACHE = new ConcurrentHashMap<>();

	private ConditionAccess() {
	}

	public static boolean has(Player player, ConditionType condition) {
		if (player == null) {
			return false;
		}
		if (player.level().isClientSide()) {
			return clientHas(player.getUUID(), condition);
		}
		return ConditionStore.has(player.getUUID(), condition);
	}

	public static boolean clientHas(UUID playerId, ConditionType condition) {
		Set<ConditionType> set = CLIENT_CACHE.get(playerId);
		return set != null && set.contains(condition);
	}

	public static Set<ConditionType> clientConditions(UUID playerId) {
		Set<ConditionType> set = CLIENT_CACHE.get(playerId);
		return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
	}

	public static void replaceClientCache(Map<UUID, EnumSet<ConditionType>> snapshot) {
		CLIENT_CACHE.clear();
		for (Map.Entry<UUID, EnumSet<ConditionType>> entry : snapshot.entrySet()) {
			CLIENT_CACHE.put(entry.getKey(), EnumSet.copyOf(entry.getValue()));
		}
	}

	public static void clearClientCache() {
		CLIENT_CACHE.clear();
	}

	public static long encodeMask(Set<ConditionType> conditions) {
		long mask = 0L;
		for (ConditionType condition : conditions) {
			mask |= (1L << condition.ordinal());
		}
		return mask;
	}

	public static EnumSet<ConditionType> decodeMask(long mask) {
		EnumSet<ConditionType> result = EnumSet.noneOf(ConditionType.class);
		for (ConditionType condition : ConditionType.values()) {
			if ((mask & (1L << condition.ordinal())) != 0L) {
				result.add(condition);
			}
		}
		return result;
	}
}
