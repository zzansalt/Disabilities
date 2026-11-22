package io.github.disabilities.logic;

import io.github.disabilities.Disabilities;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
	public static final ResourceKey<DamageType> ASTHMA = key("asthma");
	public static final ResourceKey<DamageType> DIABETES = key("diabetes");
	public static final ResourceKey<DamageType> EATING_DISORDER = key("eating_disorder");
	public static final ResourceKey<DamageType> LACTOSE = key("lactose");
	public static final ResourceKey<DamageType> OCD = key("ocd");
	public static final ResourceKey<DamageType> OCD_OVERDID = key("ocd_overdid");
	public static final ResourceKey<DamageType> SEIZURE = key("seizure");
	public static final ResourceKey<DamageType> CLAUSTROPHOBIA = key("claustrophobia");
	public static final ResourceKey<DamageType> NYCTOPHOBIA = key("nyctophobia");
	public static final ResourceKey<DamageType> AQUAPHOBIA = key("aquaphobia");

	private ModDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, Disabilities.id(name));
	}

	public static DamageSource create(ServerLevel level, ResourceKey<DamageType> key) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key));
	}
}
