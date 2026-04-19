package net.evan.magic.magic.ability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.evan.magic.config.MagicConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

final class GreedItemCategoryHelper {
	private static Set<Identifier> armorItemIds = Set.of();
	private static Set<Identifier> weaponItemIds = Set.of();
	private static Set<Identifier> toolItemIds = Set.of();
	private static Set<Identifier> equipmentItemIds = Set.of();
	private static Set<Identifier> artifactItemIds = Set.of();
	private static Set<String> artifactNamespaces = Set.of("evanpack");
	private static List<TagKey<Item>> armorTags = List.of();
	private static List<TagKey<Item>> weaponTags = List.of();
	private static List<TagKey<Item>> toolTags = List.of();
	private static List<TagKey<Item>> equipmentTags = List.of();
	private static List<TagKey<Item>> artifactTags = List.of();
	private static boolean detectArmorByClass = true;
	private static boolean detectWeaponsByClass = true;
	private static boolean detectToolsByClass = true;
	private static boolean includeShieldAsEquipment = true;
	private static boolean initialized;

	private GreedItemCategoryHelper() {
	}

	static void reloadConfigValues() {
		MagicConfig.GreedDomainDetectionConfig config = MagicConfig.get().greed.domain.detection;
		armorItemIds = parseIdentifiers(config.armorItemIds);
		weaponItemIds = parseIdentifiers(config.weaponItemIds);
		toolItemIds = parseIdentifiers(config.toolItemIds);
		equipmentItemIds = parseIdentifiers(config.equipmentItemIds);
		artifactItemIds = parseIdentifiers(config.artifactItemIds);
		artifactNamespaces = parseNamespaces(config.artifactNamespaces);
		armorTags = parseItemTags(config.armorTags);
		weaponTags = parseItemTags(config.weaponTags);
		toolTags = parseItemTags(config.toolTags);
		equipmentTags = parseItemTags(config.equipmentTags);
		artifactTags = parseItemTags(config.artifactTags);
		detectArmorByClass = config.detectArmorByClass;
		detectWeaponsByClass = config.detectWeaponsByClass;
		detectToolsByClass = config.detectToolsByClass;
		includeShieldAsEquipment = config.includeShieldAsEquipment;
		initialized = true;
	}

	static boolean isArmor(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		ensureInitialized();
		Identifier itemId = Registries.ITEM.getId(stack.getItem());
		var equippableComponent = stack.get(DataComponentTypes.EQUIPPABLE);
		return armorItemIds.contains(itemId)
			|| matchesAnyTag(stack, armorTags)
			|| detectArmorByClass && equippableComponent != null && equippableComponent.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
	}

	static boolean isWeapon(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		ensureInitialized();
		Identifier itemId = Registries.ITEM.getId(stack.getItem());
		Item item = stack.getItem();
		return weaponItemIds.contains(itemId)
			|| matchesAnyTag(stack, weaponTags)
			|| detectWeaponsByClass && (
				item instanceof AxeItem
					|| item instanceof MaceItem
					|| item instanceof TridentItem
					|| item instanceof BowItem
					|| item instanceof CrossbowItem
			);
	}

	static boolean isTool(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		ensureInitialized();
		Identifier itemId = Registries.ITEM.getId(stack.getItem());
		return toolItemIds.contains(itemId)
			|| matchesAnyTag(stack, toolTags)
			|| detectToolsByClass && stack.get(DataComponentTypes.TOOL) != null;
	}

	static boolean isEquipment(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		ensureInitialized();
		Identifier itemId = Registries.ITEM.getId(stack.getItem());
		Item item = stack.getItem();
		return equipmentItemIds.contains(itemId)
			|| matchesAnyTag(stack, equipmentTags)
			|| isArmor(stack)
			|| isWeapon(stack)
			|| isTool(stack)
			|| includeShieldAsEquipment && item instanceof ShieldItem;
	}

	static boolean isArtifact(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		ensureInitialized();
		Identifier itemId = Registries.ITEM.getId(stack.getItem());
		return artifactItemIds.contains(itemId)
			|| matchesAnyTag(stack, artifactTags)
			|| artifactNamespaces.contains(itemId.getNamespace());
	}

	private static void ensureInitialized() {
		if (!initialized) {
			reloadConfigValues();
		}
	}

	private static boolean matchesAnyTag(ItemStack stack, List<TagKey<Item>> tags) {
		for (TagKey<Item> tag : tags) {
			if (stack.isIn(tag)) {
				return true;
			}
		}
		return false;
	}

	private static Set<Identifier> parseIdentifiers(List<String> values) {
		Set<Identifier> parsed = new HashSet<>();
		if (values == null) {
			return parsed;
		}
		for (String value : values) {
			Identifier id = Identifier.tryParse(value);
			if (id != null) {
				parsed.add(id);
			}
		}
		return parsed;
	}

	private static Set<String> parseNamespaces(List<String> values) {
		Set<String> parsed = new HashSet<>();
		if (values == null) {
			return parsed;
		}
		for (String value : values) {
			if (value == null) {
				continue;
			}
			String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
			if (!normalized.isBlank()) {
				parsed.add(normalized);
			}
		}
		return parsed;
	}

	private static List<TagKey<Item>> parseItemTags(List<String> values) {
		ArrayList<TagKey<Item>> parsed = new ArrayList<>();
		if (values == null) {
			return parsed;
		}
		for (String value : values) {
			Identifier id = Identifier.tryParse(value);
			if (id != null) {
				parsed.add(TagKey.of(RegistryKeys.ITEM, id));
			}
		}
		return parsed;
	}
}


