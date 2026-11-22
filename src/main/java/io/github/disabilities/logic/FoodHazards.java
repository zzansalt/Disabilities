package io.github.disabilities.logic;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public final class FoodHazards {
	public static final float SUGAR_SEVERE = 20.0f;
	public static final float SUGAR_MEDIUM = 10.0f;
	public static final float SUGAR_MILD = 4.0f;
	public static final float SUGAR_LIGHT = 1.0f;

	private static final Map<Item, Float> SUGAR_DAMAGE = new HashMap<>();

	static {
		registerSugar(SUGAR_SEVERE,
			Items.HONEY_BOTTLE, Items.COOKIE, Items.PUMPKIN_PIE);
		registerSugar(SUGAR_MEDIUM,
			Items.APPLE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE,
			Items.MELON_SLICE, Items.CHORUS_FRUIT, Items.BEETROOT);
		registerSugar(SUGAR_MILD,
			Items.CARROT, Items.GOLDEN_CARROT, Items.SWEET_BERRIES, Items.GLOW_BERRIES,
			Items.BREAD, Items.MILK_BUCKET, Items.BEETROOT_SOUP, Items.RABBIT_STEW);
		registerSugar(SUGAR_LIGHT,
			Items.POTATO, Items.BAKED_POTATO, Items.POISONOUS_POTATO,
			Items.MUSHROOM_STEW, Items.SUSPICIOUS_STEW, Items.DRIED_KELP);
	}

	private FoodHazards() {
	}

	private static void registerSugar(float damage, Item... items) {
		for (Item item : items) {
			SUGAR_DAMAGE.put(item, damage);
		}
	}

	public static float sugarDamage(Item item) {
		if (item == null) {
			return 0.0f;
		}
		return SUGAR_DAMAGE.getOrDefault(item, 0.0f);
	}

	public static boolean isDairy(Item item) {
		return item == Items.MILK_BUCKET || item == Items.CAKE;
	}
}
