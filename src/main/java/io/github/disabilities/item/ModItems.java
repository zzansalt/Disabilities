package io.github.disabilities.item;

import io.github.disabilities.Disabilities;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
	public static final ResourceKey<Item> CURE_KEY = ResourceKey.create(Registries.ITEM, Disabilities.id("cure"));
	public static final Item CURE = Registry.register(
		BuiltInRegistries.ITEM,
		CURE_KEY,
		new CureItem(new Item.Properties()
			.setId(CURE_KEY)
			.stacksTo(16)
			.rarity(Rarity.RARE))
	);

	private ModItems() {
	}

	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS)
			.register(entries -> entries.accept(CURE));
	}
}
