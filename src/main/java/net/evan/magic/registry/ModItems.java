package net.evan.magic.registry;

import net.evan.magic.Magic;
import net.evan.magic.item.MagicAffinityAppleItem;
import net.evan.magic.magic.MagicSchool;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.type.FoodComponents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModItems {
	public static final Item FROST_APPLE = register(
		"frost_apple",
		new MagicAffinityAppleItem(MagicSchool.FROST, settings("frost_apple").food(FoodComponents.APPLE))
	);

	public static final Item LOVE_APPLE = register(
		"love_apple",
		new MagicAffinityAppleItem(MagicSchool.LOVE, settings("love_apple").food(FoodComponents.APPLE))
	);

	public static final Item BURNING_PASSION_APPLE = register(
		"burning_passion_apple",
		new MagicAffinityAppleItem(MagicSchool.BURNING_PASSION, settings("burning_passion_apple").food(FoodComponents.APPLE))
	);

	public static final Item JESTER_APPLE = register(
		"jester_apple",
		new MagicAffinityAppleItem(MagicSchool.JESTER, settings("jester_apple").food(FoodComponents.APPLE))
	);

	public static final Item CONSTELLATION_APPLE = register(
		"constellation_apple",
		new MagicAffinityAppleItem(MagicSchool.CONSTELLATION, settings("constellation_apple").food(FoodComponents.APPLE))
	);

	public static final Item GREED_APPLE = register(
		"greed_apple",
		new MagicAffinityAppleItem(MagicSchool.GREED, settings("greed_apple").food(FoodComponents.APPLE))
	);

	public static final Item FROST_SHARD = register(
		"frost_shard",
		new Item(settings("frost_shard"))
	);

	private ModItems() {
	}

	private static Item register(String path, Item item) {
		return Registry.register(Registries.ITEM, Identifier.of(Magic.MOD_ID, path), item);
	}

	private static Item.Settings settings(String path) {
		Identifier id = Identifier.of(Magic.MOD_ID, path);
		return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id));
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
			entries.add(FROST_APPLE);
			entries.add(LOVE_APPLE);
			entries.add(BURNING_PASSION_APPLE);
			entries.add(JESTER_APPLE);
			entries.add(CONSTELLATION_APPLE);
			entries.add(GREED_APPLE);
		});
	}
}
