package net.evan.magic.registry;

import net.evan.magic.Magic;
import net.evan.magic.effect.GildedBurdenStatusEffect;
import net.evan.magic.effect.ManaSicknessStatusEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ModStatusEffects {
	public static final RegistryKey<StatusEffect> MANA_SICKNESS_KEY = RegistryKey.of(
		RegistryKeys.STATUS_EFFECT,
		Identifier.of(Magic.MOD_ID, "mana_sickness")
	);
	public static final RegistryKey<StatusEffect> GILDED_BURDEN_KEY = RegistryKey.of(
		RegistryKeys.STATUS_EFFECT,
		Identifier.of(Magic.MOD_ID, "gilded_burden")
	);

	public static final StatusEffect MANA_SICKNESS = register(MANA_SICKNESS_KEY, new ManaSicknessStatusEffect());
	public static final StatusEffect GILDED_BURDEN = register(GILDED_BURDEN_KEY, new GildedBurdenStatusEffect());

	private ModStatusEffects() {
	}

	private static StatusEffect register(RegistryKey<StatusEffect> key, StatusEffect effect) {
		return Registry.register(Registries.STATUS_EFFECT, key.getValue(), effect);
	}

	public static RegistryEntry<StatusEffect> manaSicknessEntry() {
		return Registries.STATUS_EFFECT.getEntry(MANA_SICKNESS);
	}

	public static RegistryEntry<StatusEffect> gildedBurdenEntry() {
		return Registries.STATUS_EFFECT.getEntry(GILDED_BURDEN);
	}

	public static void register() {
	}
}

