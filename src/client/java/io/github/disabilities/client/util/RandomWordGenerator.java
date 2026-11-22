package io.github.disabilities.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

public final class RandomWordGenerator {
	private RandomWordGenerator() {
	}

	public static String pickWord(Minecraft mc, Random rng) {
		int bucket = rng.nextInt(5);
		return switch (bucket) {
			case 0 -> sample(BuiltInRegistries.ITEM, i -> i != Items.AIR, i -> i.getName(i.getDefaultInstance()), rng);
			case 1 -> sample(BuiltInRegistries.BLOCK, b -> b != Blocks.AIR, Block::getName, rng);
			case 2 -> sample(BuiltInRegistries.ENTITY_TYPE, t -> t.getCategory() != MobCategory.MISC, EntityType::getDescription, rng);
			case 3 -> sample(BuiltInRegistries.MOB_EFFECT, e -> true, MobEffect::getDisplayName, rng);
			default -> {
				if (mc.level != null) {
					Registry<Biome> biomes = mc.level.registryAccess().lookupOrThrow(Registries.BIOME);
					yield sample(biomes, b -> true, b -> {
						Identifier key = biomes.getKey(b);
						return key == null ? Component.literal("biome") : Component.literal(humanize(key.getPath()));
					}, rng);
				}
				yield "dirt";
			}
		};
	}

	private static <T> String sample(Registry<T> registry, Predicate<T> filter, Function<T, Component> namer, Random rng) {
		List<T> list = new ArrayList<>();
		for (T entry : registry) {
			if (filter.test(entry)) {
				list.add(entry);
			}
		}
		if (list.isEmpty()) {
			return "minecraft";
		}
		T chosen = list.get(rng.nextInt(list.size()));
		String text = namer.apply(chosen).getString();
		if (text == null || text.isBlank() || text.contains(".")) {
			Identifier id = registry.getKey(chosen);
			text = id == null ? "block" : humanize(id.getPath());
		}
		return rng.nextBoolean() ? text.toUpperCase() + "!" : text;
	}

	private static String humanize(String path) {
		return path.replace('_', ' ');
	}
}
