package net.evan.magic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.evan.magic.Magic;
import net.evan.magic.magic.core.MagicSchool;
import net.evan.magic.magic.core.ability.MagicAbility;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

public abstract class GreedDomainConfigModels extends GreedAbilityConfigModels {
	public static final class GreedDomainConfig {
		public int radius = 25;
		public int height = 25;
		public int shellThickness = 1;
		public int cooldownTicks = 36000;
		public int durationTicks = 90 * 20;
		public GreedDomainStructureConfig structure = new GreedDomainStructureConfig();
		public GreedDomainPassiveIncomeConfig passiveIncome = new GreedDomainPassiveIncomeConfig();
		public GreedDomainTributeConfig tribute = new GreedDomainTributeConfig();
		public GreedDomainActivationWarningConfig activationWarning = new GreedDomainActivationWarningConfig();
		public GreedDomainDetectionConfig detection = new GreedDomainDetectionConfig();
		public GreedDomainLightDebtConfig lightDebt = new GreedDomainLightDebtConfig();
		public GreedDomainStandardDebtConfig standardDebt = new GreedDomainStandardDebtConfig();
		public GreedDomainHeavyDebtConfig heavyDebt = new GreedDomainHeavyDebtConfig();
		public GreedDomainSevereCollectionConfig severeCollection = new GreedDomainSevereCollectionConfig();
		public GreedDomainBankruptcyTierConfig bankruptcyTier = new GreedDomainBankruptcyTierConfig();
		public GreedDomainFinalCollectionConfig finalCollection = new GreedDomainFinalCollectionConfig();
		public GreedDomainTotalCollectionConfig totalCollection = new GreedDomainTotalCollectionConfig();

		public void normalize() {
			if (structure == null) {
				structure = new GreedDomainStructureConfig();
			}
			if (passiveIncome == null) {
				passiveIncome = new GreedDomainPassiveIncomeConfig();
			}
			if (tribute == null) {
				tribute = new GreedDomainTributeConfig();
			}
			if (activationWarning == null) {
				activationWarning = new GreedDomainActivationWarningConfig();
			}
			if (detection == null) {
				detection = new GreedDomainDetectionConfig();
			}
			if (lightDebt == null) {
				lightDebt = new GreedDomainLightDebtConfig();
			}
			if (standardDebt == null) {
				standardDebt = new GreedDomainStandardDebtConfig();
			}
			if (heavyDebt == null) {
				heavyDebt = new GreedDomainHeavyDebtConfig();
			}
			if (severeCollection == null) {
				severeCollection = new GreedDomainSevereCollectionConfig();
			}
			if (bankruptcyTier == null) {
				bankruptcyTier = new GreedDomainBankruptcyTierConfig();
			}
			if (finalCollection == null) {
				finalCollection = new GreedDomainFinalCollectionConfig();
			}
			if (totalCollection == null) {
				totalCollection = new GreedDomainTotalCollectionConfig();
			}

			radius = Math.max(6, radius);
			height = Math.max(6, height);
			shellThickness = Math.max(1, Math.min(shellThickness, Math.max(1, Math.min(radius - 2, height - 2))));
			cooldownTicks = Math.max(0, cooldownTicks);
			durationTicks = Math.max(1, durationTicks);
			structure.normalize(radius, height, shellThickness);
			passiveIncome.normalize();
			tribute.normalize();
			activationWarning.normalize();
			detection.normalize();
			lightDebt.normalize();
			standardDebt.normalize();
			heavyDebt.normalize();
			severeCollection.normalize();
			bankruptcyTier.normalize();
			finalCollection.normalize();
			totalCollection.normalize(tribute.initialTribute);
		}
	}

	public static final class GreedDomainStructureConfig {
		public List<GreedWeightedBlockEntry> shellPalette = defaultShellPalette();
		public String carpetBlockId = "minecraft:red_carpet";
		public List<String> throneBlockIds = new ArrayList<>(List.of("minecraft:gold_block"));
		public List<String> pillarBlockIds = new ArrayList<>(List.of("minecraft:gold_block"));
		public int pillarCount = 8;
		public double pillarPlacementRadius = 9.0;
		public double pillarSpacingDegrees = 45.0;
		public int pillarHeight = 23;
		public int pillarWidth = 3;
		public int carpetWidth = 3;
		public int carpetExtensionPastCenter = 10;
		public String throneSide = "south";
		public int throneOffset = 12;
		public int throneWidth = 7;
		public int throneDepth = 5;
		public int throneHeight = 7;
		public boolean protectInteriorStructures = true;

		public void normalize(int radius, int height, int shellThickness) {
			if (shellPalette == null) {
				shellPalette = defaultShellPalette();
			}
			ArrayList<GreedWeightedBlockEntry> normalizedPalette = new ArrayList<>();
			for (GreedWeightedBlockEntry entry : shellPalette) {
				if (entry == null) {
					continue;
				}
				entry.normalize();
				if (!entry.blockId.isBlank() && entry.weight > 0) {
					normalizedPalette.add(entry);
				}
			}
			if (normalizedPalette.isEmpty()) {
				normalizedPalette = new ArrayList<>(defaultShellPalette());
			}
			shellPalette = normalizedPalette;
			if (carpetBlockId == null || carpetBlockId.isBlank()) {
				carpetBlockId = "minecraft:red_carpet";
			}
			throneBlockIds = normalizeBlockIdList(throneBlockIds, List.of("minecraft:gold_block"));
			pillarBlockIds = normalizeBlockIdList(pillarBlockIds, List.of("minecraft:gold_block"));
			pillarCount = Math.max(0, pillarCount);
			int maxPillarWidth = Math.max(1, Math.max(1, radius - shellThickness - 2) * 2 - 1);
			pillarWidth = Math.max(1, Math.min(pillarWidth, maxPillarWidth));
			if (pillarWidth % 2 == 0) {
				pillarWidth = Math.max(1, pillarWidth - 1);
			}
			double maxPlacementRadius = Math.max(1.0, radius - shellThickness - (pillarWidth / 2.0 + 2.0));
			pillarPlacementRadius = MathHelper.clamp(pillarPlacementRadius, 1.0, maxPlacementRadius);
			pillarSpacingDegrees = MathHelper.clamp(pillarSpacingDegrees, 1.0, 360.0);
			pillarHeight = Math.max(2, Math.min(pillarHeight, Math.max(2, height - shellThickness - 1)));
			carpetWidth = Math.max(1, Math.min(carpetWidth, Math.max(1, radius * 2 - 3)));
			carpetExtensionPastCenter = MathHelper.clamp(carpetExtensionPastCenter, 0, Math.max(0, radius - shellThickness - 2));
			throneSide = normalizeSide(throneSide);
			int maxThroneOffset = Math.max(2, radius - shellThickness - 2);
			throneOffset = MathHelper.clamp(throneOffset, 2, maxThroneOffset);
			throneWidth = Math.max(3, Math.min(throneWidth, Math.max(3, radius * 2 - 3)));
			throneDepth = Math.max(2, Math.min(throneDepth, Math.max(2, radius)));
			throneHeight = Math.max(3, Math.min(throneHeight, Math.max(3, height - shellThickness)));
		}

		private static ArrayList<GreedWeightedBlockEntry> defaultShellPalette() {
			ArrayList<GreedWeightedBlockEntry> defaults = new ArrayList<>();
			defaults.add(new GreedWeightedBlockEntry("minecraft:gilded_blackstone", 6));
			defaults.add(new GreedWeightedBlockEntry("minecraft:blackstone", 5));
			defaults.add(new GreedWeightedBlockEntry("minecraft:polished_blackstone_bricks", 5));
			defaults.add(new GreedWeightedBlockEntry("minecraft:chiseled_polished_blackstone", 2));
			defaults.add(new GreedWeightedBlockEntry("minecraft:gold_block", 2));
			return defaults;
		}
	}

	public static final class GreedWeightedBlockEntry {
		public String blockId = "";
		public int weight = 1;

		private GreedWeightedBlockEntry() {
		}

		private GreedWeightedBlockEntry(String blockId, int weight) {
			this.blockId = blockId;
			this.weight = weight;
		}

		public void normalize() {
			if (blockId == null) {
				blockId = "";
			}
			blockId = blockId.trim().toLowerCase(java.util.Locale.ROOT);
			weight = Math.max(0, weight);
		}
	}

	public static final class GreedDomainPassiveIncomeConfig {
		public double passiveCoinsPerSecondPerOtherPlayer = 1.0;
		public int passiveCoinIntervalTicks = 20;
		public boolean countMobs = false;

		public void normalize() {
			passiveCoinsPerSecondPerOtherPlayer = normalizeCoinAmount(passiveCoinsPerSecondPerOtherPlayer);
			passiveCoinIntervalTicks = Math.max(1, passiveCoinIntervalTicks);
		}
	}

	public static final class GreedDomainTributeConfig {
		public int initialTribute = 18;
		public int tributeDisplayPersistAfterEndTicks = 60;
		public int activeWeaknessAmplifier = 0;
		public int activeManaDrainLevel = 1;
		public boolean applyOnlyToPlayers = true;
		public String displayPrefix = "Tribute";

		public void normalize() {
			initialTribute = Math.max(0, initialTribute);
			tributeDisplayPersistAfterEndTicks = Math.max(0, tributeDisplayPersistAfterEndTicks);
			activeWeaknessAmplifier = Math.max(-1, activeWeaknessAmplifier);
			activeManaDrainLevel = Math.max(0, activeManaDrainLevel);
			if (displayPrefix == null || displayPrefix.isBlank()) {
				displayPrefix = "Tribute";
			}
		}
	}

	public static final class GreedDomainActivationWarningConfig {
		public boolean enabled = true;
		public int durationTicks = 25 * 20;
		public boolean pauseDomainTimerDuringDisplay = true;
		public boolean freezePlayersDuringDisplay = true;
		public boolean preventDamageDuringDisplay = true;
		public float scale = 0.95F;
		public int lineSpacing = 11;
		public String textColorHex = "#FFE7A4";
		public String outlineColorHex = "#000000";
		public List<String> lines = defaultLines();

		public void normalize() {
			durationTicks = Math.max(0, durationTicks);
			scale = MathHelper.clamp(scale, 0.5F, 2.5F);
			lineSpacing = Math.max(8, lineSpacing);
			if (textColorHex == null || textColorHex.isBlank()) {
				textColorHex = "#FFE7A4";
			}
			if (outlineColorHex == null || outlineColorHex.isBlank()) {
				outlineColorHex = "#000000";
			}
			lines = normalizeDisplayTextLines(lines, defaultLines(), 32, 160);
		}

		private static ArrayList<String> defaultLines() {
			return new ArrayList<>(List.of(
				"(A VOICE RINGS OUT IN YOUR HEAD)",
				"",
				"!ATTENTION!",
				"",
				"THIS DOMAIN HAS NO SURE HIT AFFECT",
				"",
				"CERTAIN ACTIONS PERTAINING TO PVP WILL LOWER YOUR TRIBUTES TO THE USER OF THE DOMAIN",
				"",
				"THE LESS TRIBUTES YOU HAVE THE MORE YOU \"OWE\" TO THE USER OF THE DOMAIN",
				"",
				"MAGIC, ARMOR, WEAPONS, ITEMS, AND ARTIFACTS ARE ALL AT RISK DEPENDING ON SEVERITY",
				"",
				"DO NOT LOSE ALL TRIBUTES!!!! WORK TOGETHER TO MKAE SURE NO ONE RUNS OUT!!!",
				"",
				"If you are alone, kill the user, it wont matter if you have 0 tributes if the user is dead... Goodluck from Dominus"
			));
		}
	}

	public static final class GreedDomainDetectionConfig {
		public List<String> armorTags = new ArrayList<>(List.of(
			"minecraft:head_armor",
			"minecraft:chest_armor",
			"minecraft:leg_armor",
			"minecraft:foot_armor"
		));
		public List<String> armorItemIds = new ArrayList<>();
		public List<String> weaponTags = new ArrayList<>(List.of("minecraft:swords", "minecraft:axes"));
		public List<String> weaponItemIds = new ArrayList<>();
		public List<String> toolTags = new ArrayList<>(List.of("minecraft:axes", "minecraft:pickaxes", "minecraft:shovels", "minecraft:hoes"));
		public List<String> toolItemIds = new ArrayList<>();
		public List<String> equipmentTags = new ArrayList<>();
		public List<String> equipmentItemIds = new ArrayList<>();
		public List<String> artifactTags = new ArrayList<>();
		public List<String> artifactItemIds = new ArrayList<>();
		public List<String> artifactNamespaces = new ArrayList<>(List.of("evanpack"));
		public boolean detectArmorByClass = true;
		public boolean detectWeaponsByClass = true;
		public boolean detectToolsByClass = true;
		public boolean includeShieldAsEquipment = true;

		public void normalize() {
			armorTags = normalizeStringList(armorTags, List.of(
				"minecraft:head_armor",
				"minecraft:chest_armor",
				"minecraft:leg_armor",
				"minecraft:foot_armor"
			));
			armorItemIds = normalizeStringList(armorItemIds, List.of());
			weaponTags = normalizeStringList(weaponTags, List.of("minecraft:swords", "minecraft:axes"));
			weaponItemIds = normalizeStringList(weaponItemIds, List.of());
			toolTags = normalizeStringList(toolTags, List.of("minecraft:axes", "minecraft:pickaxes", "minecraft:shovels", "minecraft:hoes"));
			toolItemIds = normalizeStringList(toolItemIds, List.of());
			equipmentTags = normalizeStringList(equipmentTags, List.of());
			equipmentItemIds = normalizeStringList(equipmentItemIds, List.of());
			artifactTags = normalizeStringList(artifactTags, List.of());
			artifactItemIds = normalizeStringList(artifactItemIds, List.of());
			artifactNamespaces = normalizeStringList(artifactNamespaces, List.of("evanpack"));
		}
	}

	public static final class GreedDomainIntRangeConfig {
		public int min = 0;
		public int max = 0;

		private GreedDomainIntRangeConfig() {
		}

		private GreedDomainIntRangeConfig(int min, int max) {
			this.min = min;
			this.max = max;
		}

		public void normalize(int minimumValue) {
			min = Math.max(minimumValue, min);
			max = Math.max(min, max);
		}
	}

	public static final class GreedDomainDoubleRangeConfig {
		public double min = 0.0;
		public double max = 0.0;

		private GreedDomainDoubleRangeConfig() {
		}

		private GreedDomainDoubleRangeConfig(double min, double max) {
			this.min = min;
			this.max = max;
		}

		public void normalize(double minimumValue, double maximumValue) {
			min = MathHelper.clamp(min, minimumValue, maximumValue);
			max = MathHelper.clamp(max, min, maximumValue);
		}
	}

	public static final class GreedDomainDurabilityTargetConfig {
		public boolean includeArmor = true;
		public boolean includeWeapons = true;
		public boolean includeTools = true;
		public boolean includeArtifacts = true;
		public boolean includeOtherDamageables = false;

		public void normalize() {
		}
	}

	public static final class GreedDomainSeizureFilterConfig {
		public boolean includeMainInventory = true;
		public boolean includeHotbar = true;
		public boolean includeOffhand = false;
		public boolean includeArmor = false;
		public boolean includeWeapons = false;
		public boolean includeTools = false;
		public boolean includeArtifacts = true;
		public boolean includeDamageable = true;
		public boolean includeUndamageable = true;

		public void normalize() {
		}
	}

	public static final class GreedDomainStatSuppressionConfig {
		public double outgoingDamageMultiplier = 0.75;
		public double armorEffectivenessMultiplier = 0.75;
		public double movementSpeedMultiplier = 0.85;
		public double attackSpeedMultiplier = 0.85;
		public double maxHealthMultiplier = 0.85;
		public double knockbackResistanceMultiplier = 0.75;
		public double scaleMultiplier = 0.9;

		private GreedDomainStatSuppressionConfig() {
		}

		private GreedDomainStatSuppressionConfig(
			double outgoingDamageMultiplier,
			double armorEffectivenessMultiplier,
			double movementSpeedMultiplier,
			double attackSpeedMultiplier,
			double maxHealthMultiplier,
			double knockbackResistanceMultiplier,
			double scaleMultiplier
		) {
			this.outgoingDamageMultiplier = outgoingDamageMultiplier;
			this.armorEffectivenessMultiplier = armorEffectivenessMultiplier;
			this.movementSpeedMultiplier = movementSpeedMultiplier;
			this.attackSpeedMultiplier = attackSpeedMultiplier;
			this.maxHealthMultiplier = maxHealthMultiplier;
			this.knockbackResistanceMultiplier = knockbackResistanceMultiplier;
			this.scaleMultiplier = scaleMultiplier;
		}

		public void normalize() {
			outgoingDamageMultiplier = MathHelper.clamp(outgoingDamageMultiplier, 0.0, 4.0);
			armorEffectivenessMultiplier = MathHelper.clamp(armorEffectivenessMultiplier, 0.0, 4.0);
			movementSpeedMultiplier = MathHelper.clamp(movementSpeedMultiplier, 0.0, 4.0);
			attackSpeedMultiplier = MathHelper.clamp(attackSpeedMultiplier, 0.0, 4.0);
			maxHealthMultiplier = MathHelper.clamp(maxHealthMultiplier, 0.0, 4.0);
			knockbackResistanceMultiplier = MathHelper.clamp(knockbackResistanceMultiplier, 0.0, 4.0);
			scaleMultiplier = MathHelper.clamp(scaleMultiplier, 0.1, 4.0);
		}
	}

	public static final class GreedDomainArtifactBurdenConfig {
		public boolean enabled = true;
		public int durationTicks = 60 * 20;
		public Map<String, Integer> slownessByArtifactCount = defaultArtifactSlownessByCount();

		public void normalize() {
			durationTicks = Math.max(0, durationTicks);
			if (slownessByArtifactCount == null) {
				slownessByArtifactCount = defaultArtifactSlownessByCount();
			}
			LinkedHashMap<String, Integer> normalized = new LinkedHashMap<>();
			for (Map.Entry<String, Integer> entry : slownessByArtifactCount.entrySet()) {
				if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
					continue;
				}
				try {
					int count = Math.max(1, Integer.parseInt(entry.getKey().trim()));
					normalized.put(Integer.toString(count), Math.max(-1, entry.getValue()));
				} catch (NumberFormatException ignored) {
				}
			}
			if (normalized.isEmpty()) {
				normalized = defaultArtifactSlownessByCount();
			}
			slownessByArtifactCount = normalized;
		}

		private static LinkedHashMap<String, Integer> defaultArtifactSlownessByCount() {
			LinkedHashMap<String, Integer> defaults = new LinkedHashMap<>();
			defaults.put("1", 1);
			defaults.put("2", 2);
			defaults.put("3", 3);
			defaults.put("4", 3);
			defaults.put("5", 4);
			return defaults;
		}
	}

	public static final class GreedDomainCoinConversionConfig {
		public double coinsPerSeizedStack = 1.0;
		public boolean allowOverflowBeyondPouchCap = true;

		public void normalize() {
			coinsPerSeizedStack = normalizeCoinAmount(coinsPerSeizedStack);
		}
	}

	public static final class GreedDomainBackfireConfig {
		public double chance = 0.25;
		public float damage = 10.0F;
		public double horizontalLaunch = 1.2;
		public double verticalLaunch = 0.55;
		public String primaryParticleId = "minecraft:explosion";
		public String secondaryParticleId = "minecraft:crit";
		public int primaryParticleCount = 1;
		public int secondaryParticleCount = 16;
		public String soundId = "minecraft:entity.generic.explode";
		public float soundVolume = 1.0F;
		public float soundPitch = 1.0F;

		public void normalize() {
			chance = MathHelper.clamp(chance, 0.0, 1.0);
			damage = Math.max(0.0F, damage);
			horizontalLaunch = Math.max(0.0, horizontalLaunch);
			verticalLaunch = Math.max(0.0, verticalLaunch);
			if (primaryParticleId == null || primaryParticleId.isBlank()) {
				primaryParticleId = "minecraft:explosion";
			}
			if (secondaryParticleId == null || secondaryParticleId.isBlank()) {
				secondaryParticleId = "minecraft:crit";
			}
			primaryParticleCount = Math.max(0, primaryParticleCount);
			secondaryParticleCount = Math.max(0, secondaryParticleCount);
			if (soundId == null || soundId.isBlank()) {
				soundId = "minecraft:entity.generic.explode";
			}
			soundVolume = Math.max(0.0F, soundVolume);
			soundPitch = Math.max(0.0F, soundPitch);
		}
	}

	public static final class GreedDomainMajorSeizureConfig {
		public boolean preferEquippedWeapon = true;
		public boolean preferEquippedArmor = true;
		public boolean allowInventoryFallback = true;

		public void normalize() {
		}
	}

	public static final class GreedDomainIndebtedConfig {
		public boolean enabled = true;
		public double casterLowManaCleanupThresholdPercent = 20.0;
		public boolean uniqueTargetCapBypassEnabled = true;

		public void normalize() {
			casterLowManaCleanupThresholdPercent = MathHelper.clamp(casterLowManaCleanupThresholdPercent, 0.0, 100.0);
		}
	}

	public static final class GreedDomainCollectedConfig {
		public boolean enabled = true;
		public int futureReducedTributeStartingValue = 12;
		public double coinExtractionMultiplier = 1.25;
		public double tributeLossMultiplier = 1.5;
		public boolean residualExtractionEnabled = true;
		public Map<String, Double> residualExtractionTriggers = defaultResidualExtractionTriggers();

		public void normalize(int initialTribute) {
			futureReducedTributeStartingValue = MathHelper.clamp(futureReducedTributeStartingValue, 0, Math.max(0, initialTribute));
			coinExtractionMultiplier = MathHelper.clamp(coinExtractionMultiplier, 0.0, 10.0);
			tributeLossMultiplier = MathHelper.clamp(tributeLossMultiplier, 0.0, 10.0);
			if (residualExtractionTriggers == null) {
				residualExtractionTriggers = defaultResidualExtractionTriggers();
			}
			LinkedHashMap<String, Double> normalized = new LinkedHashMap<>();
			for (Map.Entry<String, Double> entry : residualExtractionTriggers.entrySet()) {
				if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
					continue;
				}
				normalized.put(entry.getKey().trim().toLowerCase(java.util.Locale.ROOT), normalizeCoinAmount(entry.getValue()));
			}
			if (normalized.isEmpty()) {
				normalized = defaultResidualExtractionTriggers();
			}
			residualExtractionTriggers = normalized;
		}

		private static LinkedHashMap<String, Double> defaultResidualExtractionTriggers() {
			LinkedHashMap<String, Double> defaults = new LinkedHashMap<>();
			defaults.put("critical_hit", 0.25);
			defaults.put("disable_shield", 0.25);
			defaults.put("spear_lunge", 0.1);
			defaults.put("attribute_swapping", 0.1);
			return defaults;
		}
	}

	public static final class GreedDomainLightDebtConfig {
		public GreedDomainIntRangeConfig manaSicknessAmplifierRange = new GreedDomainIntRangeConfig(1, 3);
		public int manaSicknessDurationTicks = 15 * 20;
		public double manaRegenMultiplier = 0.5;
		public int manaRegenPenaltyDurationTicks = 60 * 20;
		public double castFailureChance = 0.1;
		public int castFailureDurationTicks = 60 * 20;
		public double durabilityLossFraction = 0.25;
		public double armorEffectivenessMultiplier = 0.75;
		public double weaponDamageMultiplier = 0.75;
		public int statPenaltyDurationTicks = 30 * 20;

		public void normalize() {
			manaSicknessAmplifierRange.normalize(1);
			manaSicknessDurationTicks = Math.max(0, manaSicknessDurationTicks);
			manaRegenMultiplier = MathHelper.clamp(manaRegenMultiplier, 0.0, 10.0);
			manaRegenPenaltyDurationTicks = Math.max(0, manaRegenPenaltyDurationTicks);
			castFailureChance = MathHelper.clamp(castFailureChance, 0.0, 1.0);
			castFailureDurationTicks = Math.max(0, castFailureDurationTicks);
			durabilityLossFraction = MathHelper.clamp(durabilityLossFraction, 0.0, 1.0);
			armorEffectivenessMultiplier = MathHelper.clamp(armorEffectivenessMultiplier, 0.0, 10.0);
			weaponDamageMultiplier = MathHelper.clamp(weaponDamageMultiplier, 0.0, 10.0);
			statPenaltyDurationTicks = Math.max(0, statPenaltyDurationTicks);
		}
	}

	public static final class GreedDomainStandardDebtConfig {
		public GreedDomainIntRangeConfig manaSicknessAmplifierRange = new GreedDomainIntRangeConfig(4, 6);
		public int manaSicknessDurationTicks = 15 * 20;
		public double manaCostMultiplier = 1.25;
		public int manaCostPenaltyDurationTicks = 60 * 20;
		public GreedDomainArtifactBurdenConfig artifactBurden = new GreedDomainArtifactBurdenConfig();
		public GreedDomainDoubleRangeConfig durabilityLossFractionRange = new GreedDomainDoubleRangeConfig(0.25, 0.75);
		public GreedDomainDurabilityTargetConfig qualifyingItemCategories = new GreedDomainDurabilityTargetConfig();

		public void normalize() {
			if (artifactBurden == null) {
				artifactBurden = new GreedDomainArtifactBurdenConfig();
			}
			if (qualifyingItemCategories == null) {
				qualifyingItemCategories = new GreedDomainDurabilityTargetConfig();
			}
			manaSicknessAmplifierRange.normalize(1);
			manaSicknessDurationTicks = Math.max(0, manaSicknessDurationTicks);
			manaCostMultiplier = MathHelper.clamp(manaCostMultiplier, 0.0, 10.0);
			manaCostPenaltyDurationTicks = Math.max(0, manaCostPenaltyDurationTicks);
			artifactBurden.normalize();
			durabilityLossFractionRange.normalize(0.0, 1.0);
			qualifyingItemCategories.normalize();
		}
	}

	public static final class GreedDomainHeavyDebtConfig {
		public GreedDomainIntRangeConfig manaSicknessAmplifierRange = new GreedDomainIntRangeConfig(7, 8);
		public int manaSicknessDurationTicks = 10 * 20;
		public double castFailureChance = 0.25;
		public int castFailureDurationTicks = 2 * 60 * 20;
		public double cooldownMultiplier = 1.25;
		public int cooldownPenaltyDurationTicks = 4 * 60 * 20;
		public GreedDomainIntRangeConfig forcedTaxationItemCountRange = new GreedDomainIntRangeConfig(2, 4);
		public GreedDomainSeizureFilterConfig allowedSeizureCategories = defaultForcedTaxationSeizureFilters();
		public GreedDomainCoinConversionConfig coinConversion = new GreedDomainCoinConversionConfig();

		public void normalize() {
			if (allowedSeizureCategories == null) {
				allowedSeizureCategories = defaultForcedTaxationSeizureFilters();
			}
			if (coinConversion == null) {
				coinConversion = new GreedDomainCoinConversionConfig();
			}
			manaSicknessAmplifierRange.normalize(1);
			manaSicknessDurationTicks = Math.max(0, manaSicknessDurationTicks);
			castFailureChance = MathHelper.clamp(castFailureChance, 0.0, 1.0);
			castFailureDurationTicks = Math.max(0, castFailureDurationTicks);
			cooldownMultiplier = MathHelper.clamp(cooldownMultiplier, 0.0, 10.0);
			cooldownPenaltyDurationTicks = Math.max(0, cooldownPenaltyDurationTicks);
			forcedTaxationItemCountRange.normalize(0);
			allowedSeizureCategories.normalize();
			coinConversion.normalize();
		}

		private static GreedDomainSeizureFilterConfig defaultForcedTaxationSeizureFilters() {
			GreedDomainSeizureFilterConfig filters = new GreedDomainSeizureFilterConfig();
			filters.includeArtifacts = false;
			return filters;
		}
	}

	public static final class GreedDomainSevereCollectionConfig {
		public int manaSicknessAmplifier = 9;
		public int manaSicknessDurationTicks = 8 * 20;
		public double castFailureChance = 0.5;
		public int castFailureDurationTicks = 3 * 60 * 20;
		public GreedDomainBackfireConfig backfire = new GreedDomainBackfireConfig();
		public GreedDomainMajorSeizureConfig seizureItemCategorySelectionRules = new GreedDomainMajorSeizureConfig();

		public void normalize() {
			if (backfire == null) {
				backfire = new GreedDomainBackfireConfig();
			}
			if (seizureItemCategorySelectionRules == null) {
				seizureItemCategorySelectionRules = new GreedDomainMajorSeizureConfig();
			}
			manaSicknessAmplifier = Math.max(0, manaSicknessAmplifier);
			manaSicknessDurationTicks = Math.max(0, manaSicknessDurationTicks);
			castFailureChance = MathHelper.clamp(castFailureChance, 0.0, 1.0);
			castFailureDurationTicks = Math.max(0, castFailureDurationTicks);
			backfire.normalize();
			seizureItemCategorySelectionRules.normalize();
		}
	}

	public static final class GreedDomainBankruptcyTierConfig {
		public int magicSealDurationTicks = 60 * 20;
		public GreedDomainIntRangeConfig seizureItemCountRange = new GreedDomainIntRangeConfig(2, 6);
		public GreedDomainSeizureFilterConfig seizureCategoryFilters = defaultBankruptcySeizureFilters();
		public int statSuppressionDurationTicks = 2 * 60 * 20;
		public GreedDomainStatSuppressionConfig statSuppressionAttributeMultipliers = new GreedDomainStatSuppressionConfig(0.55, 0.55, 0.65, 0.65, 0.72, 0.45, 0.9);
		public GreedDomainIndebtedConfig indebted = new GreedDomainIndebtedConfig();

		public void normalize() {
			if (seizureCategoryFilters == null) {
				seizureCategoryFilters = defaultBankruptcySeizureFilters();
			}
			if (statSuppressionAttributeMultipliers == null) {
				statSuppressionAttributeMultipliers = new GreedDomainStatSuppressionConfig(0.55, 0.55, 0.65, 0.65, 0.72, 0.45, 0.9);
			}
			if (indebted == null) {
				indebted = new GreedDomainIndebtedConfig();
			}
			magicSealDurationTicks = Math.max(0, magicSealDurationTicks);
			seizureItemCountRange.normalize(0);
			seizureCategoryFilters.normalize();
			statSuppressionDurationTicks = Math.max(0, statSuppressionDurationTicks);
			statSuppressionAttributeMultipliers.normalize();
			indebted.normalize();
		}

		private static GreedDomainSeizureFilterConfig defaultBankruptcySeizureFilters() {
			GreedDomainSeizureFilterConfig filters = new GreedDomainSeizureFilterConfig();
			filters.includeArmor = false;
			filters.includeWeapons = true;
			filters.includeTools = true;
			filters.includeArtifacts = false;
			filters.includeOffhand = true;
			return filters;
		}
	}

	public static final class GreedDomainFinalCollectionConfig {
		public int magicSealDurationTicks = 60 * 20;
		public int statSuppressionDurationTicks = 2 * 60 * 20;
		public GreedDomainStatSuppressionConfig statSuppressionAttributeMultipliers = new GreedDomainStatSuppressionConfig(0.42, 0.42, 0.55, 0.5, 0.6, 0.25, 0.82);
		public boolean removeWornArmorFirst = true;
		public boolean removeArmorFromInventoryIfNoArmorWorn = true;
		public boolean preferMainHandWeapon = true;
		public boolean allowInventoryWeaponFallback = true;
		public GreedDomainIntRangeConfig randomItemSeizureCountRange = new GreedDomainIntRangeConfig(2, 6);
		public GreedDomainSeizureFilterConfig randomItemSeizureFilters = defaultRandomItemSeizureFilters();
		public double artifactSeizureChance = 0.5;
		public GreedDomainIndebtedConfig indebted = new GreedDomainIndebtedConfig();

		public void normalize() {
			if (statSuppressionAttributeMultipliers == null) {
				statSuppressionAttributeMultipliers = new GreedDomainStatSuppressionConfig(0.42, 0.42, 0.55, 0.5, 0.6, 0.25, 0.82);
			}
			if (randomItemSeizureFilters == null) {
				randomItemSeizureFilters = defaultRandomItemSeizureFilters();
			}
			if (indebted == null) {
				indebted = new GreedDomainIndebtedConfig();
			}
			magicSealDurationTicks = Math.max(0, magicSealDurationTicks);
			statSuppressionDurationTicks = Math.max(0, statSuppressionDurationTicks);
			statSuppressionAttributeMultipliers.normalize();
			randomItemSeizureCountRange.normalize(0);
			randomItemSeizureFilters.normalize();
			artifactSeizureChance = MathHelper.clamp(artifactSeizureChance, 0.0, 1.0);
			indebted.normalize();
		}

		private static GreedDomainSeizureFilterConfig defaultRandomItemSeizureFilters() {
			GreedDomainSeizureFilterConfig filters = new GreedDomainSeizureFilterConfig();
			filters.includeWeapons = true;
			filters.includeTools = true;
			filters.includeArtifacts = false;
			filters.includeOffhand = true;
			return filters;
		}
	}

	public static final class GreedDomainTotalCollectionConfig {
		public int magicSealDurationTicks = 2 * 60 * 20;
		public int severeStatSuppressionDurationTicks = 3 * 60 * 20;
		public GreedDomainStatSuppressionConfig severeStatSuppressionAttributeMultipliers = new GreedDomainStatSuppressionConfig(0.22, 0.22, 0.38, 0.32, 0.45, 0.1, 0.68);
		public double halfInventorySeizureFraction = 0.5;
		public GreedDomainSeizureFilterConfig halfInventorySeizureFilters = defaultHalfInventorySeizureFilters();
		public double equipmentCoinRewardPerSeizedItem = 2.0;
		public boolean equipmentCoinRewardUsesTemporaryOverflowStack = true;
		public double artifactSeizeChance = 0.75;
		public int minimumArtifactSeizureCountIfMultiple = 1;
		public GreedDomainIndebtedConfig indebted = new GreedDomainIndebtedConfig();
		public GreedDomainCollectedConfig collected = new GreedDomainCollectedConfig();

		public void normalize(int initialTribute) {
			if (severeStatSuppressionAttributeMultipliers == null) {
				severeStatSuppressionAttributeMultipliers = new GreedDomainStatSuppressionConfig(0.22, 0.22, 0.38, 0.32, 0.45, 0.1, 0.68);
			}
			if (halfInventorySeizureFilters == null) {
				halfInventorySeizureFilters = defaultHalfInventorySeizureFilters();
			}
			if (indebted == null) {
				indebted = new GreedDomainIndebtedConfig();
			}
			if (collected == null) {
				collected = new GreedDomainCollectedConfig();
			}
			magicSealDurationTicks = Math.max(0, magicSealDurationTicks);
			severeStatSuppressionDurationTicks = Math.max(0, severeStatSuppressionDurationTicks);
			severeStatSuppressionAttributeMultipliers.normalize();
			halfInventorySeizureFraction = MathHelper.clamp(halfInventorySeizureFraction, 0.0, 1.0);
			halfInventorySeizureFilters.normalize();
			equipmentCoinRewardPerSeizedItem = normalizeCoinAmount(equipmentCoinRewardPerSeizedItem);
			artifactSeizeChance = MathHelper.clamp(artifactSeizeChance, 0.0, 1.0);
			minimumArtifactSeizureCountIfMultiple = Math.max(0, minimumArtifactSeizureCountIfMultiple);
			indebted.normalize();
			collected.normalize(initialTribute);
		}

		private static GreedDomainSeizureFilterConfig defaultHalfInventorySeizureFilters() {
			GreedDomainSeizureFilterConfig filters = new GreedDomainSeizureFilterConfig();
			filters.includeArmor = false;
			filters.includeWeapons = false;
			filters.includeTools = false;
			filters.includeArtifacts = false;
			filters.includeOffhand = true;
			return filters;
		}
	}
}


