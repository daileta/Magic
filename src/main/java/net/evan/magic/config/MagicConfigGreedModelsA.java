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
import net.evan.magic.magic.MagicSchool;
import net.evan.magic.magic.ability.MagicAbility;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

abstract class MagicConfigGreedModelsA extends MagicConfigConstellationModels {
	public static final class ManaSicknessConfig {
		public int tickIntervalTicks = 20;
		public double levelOneDrainPercentPerSecond = 1.0;
		public double levelTwoDrainPercentPerSecond = 2.0;
		public double levelThreeDrainPercentPerSecond = 5.0;
		public double levelFourDrainPercentPerSecond = 10.0;
		public double levelFiveDrainPercentPerSecond = 15.0;
		public double levelSixDrainPercentPerSecond = 20.0;
		public double levelSevenDrainPercentPerSecond = 25.0;
		public double levelEightDrainPercentPerSecond = 50.0;
		public double levelNinePlusDrainPercentPerSecond = 100.0;

		void normalize() {
			tickIntervalTicks = Math.max(1, tickIntervalTicks);
			levelOneDrainPercentPerSecond = MathHelper.clamp(levelOneDrainPercentPerSecond, 0.0, 100.0);
			levelTwoDrainPercentPerSecond = MathHelper.clamp(levelTwoDrainPercentPerSecond, 0.0, 100.0);
			levelThreeDrainPercentPerSecond = MathHelper.clamp(levelThreeDrainPercentPerSecond, 0.0, 100.0);
			levelFourDrainPercentPerSecond = MathHelper.clamp(levelFourDrainPercentPerSecond, 0.0, 100.0);
			levelFiveDrainPercentPerSecond = MathHelper.clamp(levelFiveDrainPercentPerSecond, 0.0, 100.0);
			levelSixDrainPercentPerSecond = MathHelper.clamp(levelSixDrainPercentPerSecond, 0.0, 100.0);
			levelSevenDrainPercentPerSecond = MathHelper.clamp(levelSevenDrainPercentPerSecond, 0.0, 100.0);
			levelEightDrainPercentPerSecond = MathHelper.clamp(levelEightDrainPercentPerSecond, 0.0, 100.0);
			levelNinePlusDrainPercentPerSecond = MathHelper.clamp(levelNinePlusDrainPercentPerSecond, 0.0, 100.0);
		}
	}

	public static final class AppraisersMarkConfig {
		public double markedDrainPercentPerSecond = 1.0;
		public int cooldownTicks = 30 * 20;
		public double markedActionRange = 32.0;
		public int maxTrackedPlayers = 3;
		public double maxCoins = 18.0;
		public int inactivityWipeTicks = 5 * 60 * 20;
		public int contributionLifetimeTicks = 10 * 60 * 20;
		public int markParticleIntervalTicks = 4;
		public int markParticleCount = 3;
		public double markParticleHorizontalSpread = 0.18;
		public double markParticleVerticalSpread = 0.28;
		public boolean playCoinGainSound = true;
		public float coinGainSoundVolume = 0.35F;
		public float coinGainSoundPitch = 1.45F;
		public Map<String, Double> coinTriggers = defaultCoinTriggers();

		void normalize() {
			markedDrainPercentPerSecond = MathHelper.clamp(markedDrainPercentPerSecond, 0.0, 100.0);
			cooldownTicks = Math.max(0, cooldownTicks);
			markedActionRange = Math.max(1.0, markedActionRange);
			maxTrackedPlayers = Math.max(1, maxTrackedPlayers);
			maxCoins = normalizeCoinAmount(maxCoins);
			inactivityWipeTicks = Math.max(1, inactivityWipeTicks);
			contributionLifetimeTicks = Math.max(1, contributionLifetimeTicks);
			markParticleIntervalTicks = Math.max(1, markParticleIntervalTicks);
			markParticleCount = Math.max(0, markParticleCount);
			markParticleHorizontalSpread = Math.max(0.0, markParticleHorizontalSpread);
			markParticleVerticalSpread = Math.max(0.0, markParticleVerticalSpread);
			coinGainSoundVolume = Math.max(0.0F, coinGainSoundVolume);
			coinGainSoundPitch = Math.max(0.0F, coinGainSoundPitch);
			if (coinTriggers == null) {
				coinTriggers = defaultCoinTriggers();
			}
			if (coinTriggers.isEmpty()) {
				coinTriggers = defaultCoinTriggers();
			}
			LinkedHashMap<String, Double> normalized = defaultCoinTriggers();
			for (Map.Entry<String, Double> entry : coinTriggers.entrySet()) {
				String key = entry.getKey();
				Double value = entry.getValue();
				if (key == null || key.isBlank() || value == null) {
					continue;
				}
				normalized.put(key.trim().toLowerCase(), normalizeCoinAmount(value));
			}
			coinTriggers = normalized;
		}

		private static LinkedHashMap<String, Double> defaultCoinTriggers() {
			LinkedHashMap<String, Double> defaults = new LinkedHashMap<>();
			defaults.put("falling_mace_attack", 2.5);
			defaults.put("destroy_end_crystal", 3.0);
			defaults.put("explode_respawn_anchor", 3.0);
			defaults.put("full_charge_bow_shot", 1.5);
			defaults.put("firework_shot", 1.0);
			defaults.put("tipped_arrow_shot", 1.0);
			defaults.put("ignite_tnt", 2.0);
			defaults.put("ignite_tnt_minecart", 3.0);
			defaults.put("attribute_swapping", 2.0);
			defaults.put("disable_shield", 1.5);
			defaults.put("raise_shield", 0.25);
			defaults.put("elytra_equip", 2.0);
			defaults.put("use_ender_pearl", 2.0);
			defaults.put("use_potion", 0.5);
			defaults.put("eat_food", 0.1);
			defaults.put("eat_golden_apple", 1.0);
			defaults.put("eat_enchanted_golden_apple", 2.0);
			defaults.put("normal_hit", 0.1);
			defaults.put("pop_player_totem", 3.0);
			defaults.put("start_sprinting", 0.1);
			defaults.put("jump", 0.1);
			defaults.put("magic_ability_1", 3.0);
			defaults.put("magic_ability_2", 5.0);
			defaults.put("magic_ability_3", 7.0);
			defaults.put("magic_ability_4", 9.0);
			defaults.put("magic_ability_maximum_or_domain", 18.0);
			defaults.put("spear_lunge", 0.5);
			defaults.put("spear_charge_attack", 1.5);
			defaults.put("critical_hit", 0.5);
			return defaults;
		}

	}

	public static final class TollkeepersClaimConfig {
		public int minCoins = 1;
		public int maxCoins = 5;
		public int maxActiveZonesPerCaster = 0;
		public double placementRange = 20.0;
		public double zoneRadius = 4.0;
		public int baseDurationTicks = 4 * 20;
		public int durationPerCoinTicks = 2 * 20;
		public int burdenRefreshTicks = 10;
		public double reducedJumpVelocityMultiplier = 0.55;
		public int rootSlownessAmplifier = 255;
		public int rootMiningFatigueAmplifier = 5;
		public boolean allowEnderPearlEscapeWhileRooted = true;
		public double markedExitBonusCoins = 1.0;
		public int vortexParticleIntervalTicks = 2;
		public int vortexParticleLifetimeTicks = 24;
		public float vortexParticleScale = 0.28F;
		public double vortexTotalHeight = 3.0;
		public double vortexStraightHeight = 1.0;
		public double vortexOutwardCurve = 0.22;
		public double vortexSpinDegreesPerTick = 18.0;
		public double vortexTwistDegreesPerBlock = 120.0;
		public int shimmerSoundIntervalTicks = 20;
		public TollkeepersClaimStageConfig stageOne = defaultStageOne();
		public TollkeepersClaimStageConfig stageTwo = defaultStageTwo();
		public TollkeepersClaimStageConfig stageThree = defaultStageThree();
		public TollkeepersClaimStageConfig stageFour = defaultStageFour();
		public TollkeepersClaimStageConfig stageFive = defaultStageFive();

		void normalize() {
			if (stageOne == null) {
				stageOne = defaultStageOne();
			}
			if (stageTwo == null) {
				stageTwo = defaultStageTwo();
			}
			if (stageThree == null) {
				stageThree = defaultStageThree();
			}
			if (stageFour == null) {
				stageFour = defaultStageFour();
			}
			if (stageFive == null) {
				stageFive = defaultStageFive();
			}

			minCoins = Math.max(1, minCoins);
			maxCoins = Math.max(minCoins, maxCoins);
			maxActiveZonesPerCaster = Math.max(0, maxActiveZonesPerCaster);
			placementRange = Math.max(1.0, placementRange);
			zoneRadius = Math.max(0.5, zoneRadius);
			baseDurationTicks = Math.max(1, baseDurationTicks);
			durationPerCoinTicks = Math.max(0, durationPerCoinTicks);
			burdenRefreshTicks = Math.max(1, burdenRefreshTicks);
			reducedJumpVelocityMultiplier = MathHelper.clamp(reducedJumpVelocityMultiplier, 0.0, 1.0);
			rootSlownessAmplifier = Math.max(0, rootSlownessAmplifier);
			rootMiningFatigueAmplifier = Math.max(0, rootMiningFatigueAmplifier);
			markedExitBonusCoins = normalizeCoinAmount(markedExitBonusCoins);
			vortexParticleIntervalTicks = Math.max(1, vortexParticleIntervalTicks);
			vortexParticleLifetimeTicks = Math.max(1, vortexParticleLifetimeTicks);
			vortexParticleScale = Math.max(0.01F, vortexParticleScale);
			vortexTotalHeight = Math.max(0.1, vortexTotalHeight);
			vortexStraightHeight = MathHelper.clamp(vortexStraightHeight, 0.0, vortexTotalHeight);
			vortexOutwardCurve = Math.max(0.0, vortexOutwardCurve);
			vortexSpinDegreesPerTick = Math.max(0.0, vortexSpinDegreesPerTick);
			vortexTwistDegreesPerBlock = Math.max(0.0, vortexTwistDegreesPerBlock);
			shimmerSoundIntervalTicks = Math.max(1, shimmerSoundIntervalTicks);
			stageOne.normalize();
			stageTwo.normalize();
			stageThree.normalize();
			stageFour.normalize();
			stageFive.normalize();
		}

		private static TollkeepersClaimStageConfig defaultStageOne() {
			TollkeepersClaimStageConfig config = new TollkeepersClaimStageConfig();
			config.slownessAmplifier = 0;
			config.rootDurationTicks = 20;
			return config;
		}

		private static TollkeepersClaimStageConfig defaultStageTwo() {
			TollkeepersClaimStageConfig config = new TollkeepersClaimStageConfig();
			config.slownessAmplifier = 0;
			config.reduceJumpHeight = true;
			config.rootDurationTicks = 20;
			return config;
		}

		private static TollkeepersClaimStageConfig defaultStageThree() {
			TollkeepersClaimStageConfig config = new TollkeepersClaimStageConfig();
			config.slownessAmplifier = 1;
			config.reduceJumpHeight = true;
			config.rootDurationTicks = 40;
			return config;
		}

		private static TollkeepersClaimStageConfig defaultStageFour() {
			TollkeepersClaimStageConfig config = new TollkeepersClaimStageConfig();
			config.slownessAmplifier = 1;
			config.reduceJumpHeight = true;
			config.disableSprint = true;
			config.radiusBonusBlocks = 2.0;
			config.casterResistanceAmplifier = 0;
			config.rootDurationTicks = 40;
			return config;
		}

		private static TollkeepersClaimStageConfig defaultStageFive() {
			TollkeepersClaimStageConfig config = new TollkeepersClaimStageConfig();
			config.slownessAmplifier = 1;
			config.reduceJumpHeight = true;
			config.disableSprint = true;
			config.radiusBonusBlocks = 3.0;
			config.casterResistanceAmplifier = 1;
			config.rootDurationTicks = 60;
			return config;
		}
	}

	public static final class TollkeepersClaimStageConfig {
		public int slownessAmplifier = -1;
		public boolean reduceJumpHeight = false;
		public boolean disableSprint = false;
		public double radiusBonusBlocks = 0.0;
		public int casterResistanceAmplifier = -1;
		public int rootDurationTicks = 20;

		void normalize() {
			slownessAmplifier = Math.max(-1, slownessAmplifier);
			radiusBonusBlocks = Math.max(0.0, radiusBonusBlocks);
			casterResistanceAmplifier = Math.max(-1, casterResistanceAmplifier);
			rootDurationTicks = Math.max(1, rootDurationTicks);
		}
	}

	public static final class KingsDuesConfig {
		public int minCoins = 1;
		public int maxCoins = 6;
		public double targetRange = 20.0;
		public boolean requireLineOfSight = true;
		public int cooldownTicks = 40 * 20;
		public int particleBurstCount = 12;
		public KingsDuesStageConfig stageOne = defaultStageOne();
		public KingsDuesStageConfig stageTwo = defaultStageTwo();
		public KingsDuesStageConfig stageThree = defaultStageThree();
		public KingsDuesStageConfig stageFour = defaultStageFour();
		public KingsDuesStageConfig stageFive = defaultStageFive();
		public KingsDuesStageConfig stageSix = defaultStageSix();

		void normalize() {
			if (stageOne == null) {
				stageOne = defaultStageOne();
			}
			if (stageTwo == null) {
				stageTwo = defaultStageTwo();
			}
			if (stageThree == null) {
				stageThree = defaultStageThree();
			}
			if (stageFour == null) {
				stageFour = defaultStageFour();
			}
			if (stageFive == null) {
				stageFive = defaultStageFive();
			}
			if (stageSix == null) {
				stageSix = defaultStageSix();
			}

			minCoins = Math.max(1, minCoins);
			maxCoins = Math.max(minCoins, maxCoins);
			targetRange = Math.max(1.0, targetRange);
			cooldownTicks = Math.max(0, cooldownTicks);
			particleBurstCount = Math.max(0, particleBurstCount);
			stageOne.normalize();
			stageTwo.normalize();
			stageThree.normalize();
			stageFour.normalize();
			stageFive.normalize();
			stageSix.normalize();
		}

		private static KingsDuesStageConfig defaultStageOne() {
			KingsDuesStageConfig config = new KingsDuesStageConfig();
			config.weaknessAmplifier = 0;
			config.weaknessDurationTicks = 5 * 20;
			return config;
		}

		private static KingsDuesStageConfig defaultStageTwo() {
			KingsDuesStageConfig config = new KingsDuesStageConfig();
			config.weaknessAmplifier = 0;
			config.weaknessDurationTicks = 6 * 20;
			config.glowingDurationTicks = 6 * 20;
			return config;
		}

		private static KingsDuesStageConfig defaultStageThree() {
			KingsDuesStageConfig config = new KingsDuesStageConfig();
			config.weaknessAmplifier = 1;
			config.weaknessDurationTicks = 7 * 20;
			config.slownessAmplifier = 0;
			config.slownessDurationTicks = 7 * 20;
			config.glowingDurationTicks = 7 * 20;
			return config;
		}

		private static KingsDuesStageConfig defaultStageFour() {
			KingsDuesStageConfig config = new KingsDuesStageConfig();
			config.weaknessAmplifier = 1;
			config.weaknessDurationTicks = 10 * 20;
			config.slownessAmplifier = 0;
			config.slownessDurationTicks = 10 * 20;
			config.glowingDurationTicks = 10 * 20;
			config.shieldLockTicks = 5 * 20;
			return config;
		}

		private static KingsDuesStageConfig defaultStageFive() {
			KingsDuesStageConfig config = new KingsDuesStageConfig();
			config.weaknessAmplifier = 1;
			config.weaknessDurationTicks = 10 * 20;
			config.slownessAmplifier = 1;
			config.slownessDurationTicks = 10 * 20;
			config.glowingDurationTicks = 10 * 20;
			config.sprintLockTicks = 5 * 20;
			config.shieldLockTicks = 5 * 20;
			return config;
		}

		private static KingsDuesStageConfig defaultStageSix() {
			KingsDuesStageConfig config = new KingsDuesStageConfig();
			config.weaknessAmplifier = 2;
			config.weaknessDurationTicks = 15 * 20;
			config.slownessAmplifier = 2;
			config.slownessDurationTicks = 15 * 20;
			config.glowingDurationTicks = 15 * 20;
			config.sprintLockTicks = 10 * 20;
			config.shieldLockTicks = 10 * 20;
			config.attackSpeedLockTicks = 5 * 20;
			config.attackSpeedModifierAmount = -0.35;
			return config;
		}
	}

	public static final class KingsDuesStageConfig {
		public int weaknessAmplifier = -1;
		public int weaknessDurationTicks = 0;
		public int slownessAmplifier = -1;
		public int slownessDurationTicks = 0;
		public int glowingDurationTicks = 0;
		public int sprintLockTicks = 0;
		public int shieldLockTicks = 0;
		public int attackSpeedLockTicks = 0;
		public double attackSpeedModifierAmount = 0.0;

		void normalize() {
			weaknessAmplifier = Math.max(-1, weaknessAmplifier);
			weaknessDurationTicks = Math.max(0, weaknessDurationTicks);
			slownessAmplifier = Math.max(-1, slownessAmplifier);
			slownessDurationTicks = Math.max(0, slownessDurationTicks);
			glowingDurationTicks = Math.max(0, glowingDurationTicks);
			sprintLockTicks = Math.max(0, sprintLockTicks);
			shieldLockTicks = Math.max(0, shieldLockTicks);
			attackSpeedLockTicks = Math.max(0, attackSpeedLockTicks);
		}
	}

	public static final class BankruptcyConfig {
		public int minCoins = 2;
		public int maxCoins = 8;
		public double targetRange = 20.0;
		public boolean requireLineOfSight = true;
		public int cooldownTicks = 90 * 20;
		public BankruptcyStageConfig stageTwo = defaultStageTwo();
		public BankruptcyStageConfig stageThree = defaultStageThree();
		public BankruptcyStageConfig stageFour = defaultStageFour();
		public BankruptcyStageConfig stageFive = defaultStageFive();
		public BankruptcyStageConfig stageSix = defaultStageSix();
		public BankruptcyStageConfig stageSeven = defaultStageSeven();
		public BankruptcyStageConfig stageEight = defaultStageEight();

		void normalize() {
			if (stageTwo == null) {
				stageTwo = defaultStageTwo();
			}
			if (stageThree == null) {
				stageThree = defaultStageThree();
			}
			if (stageFour == null) {
				stageFour = defaultStageFour();
			}
			if (stageFive == null) {
				stageFive = defaultStageFive();
			}
			if (stageSix == null) {
				stageSix = defaultStageSix();
			}
			if (stageSeven == null) {
				stageSeven = defaultStageSeven();
			}
			if (stageEight == null) {
				stageEight = defaultStageEight();
			}

			minCoins = Math.max(1, minCoins);
			maxCoins = Math.max(minCoins, maxCoins);
			targetRange = Math.max(1.0, targetRange);
			cooldownTicks = Math.max(0, cooldownTicks);
			stageTwo.normalize();
			stageThree.normalize();
			stageFour.normalize();
			stageFive.normalize();
			stageSix.normalize();
			stageSeven.normalize();
			stageEight.normalize();
		}

		private static BankruptcyStageConfig defaultStageTwo() {
			BankruptcyStageConfig config = new BankruptcyStageConfig();
			config.manaSicknessAmplifier = 0;
			config.durationTicks = 6 * 20;
			return config;
		}

		private static BankruptcyStageConfig defaultStageThree() {
			BankruptcyStageConfig config = new BankruptcyStageConfig();
			config.manaSicknessAmplifier = 1;
			config.durationTicks = 6 * 20;
			return config;
		}

		private static BankruptcyStageConfig defaultStageFour() {
			BankruptcyStageConfig config = new BankruptcyStageConfig();
			config.manaSicknessAmplifier = 2;
			config.durationTicks = 5 * 20;
			config.instantManaDrainPercent = 20.0;
			return config;
		}

		private static BankruptcyStageConfig defaultStageFive() {
			BankruptcyStageConfig config = new BankruptcyStageConfig();
			config.manaSicknessAmplifier = 3;
			config.durationTicks = 5 * 20;
			config.instantManaDrainPercent = 20.0;
			return config;
		}

		private static BankruptcyStageConfig defaultStageSix() {
			BankruptcyStageConfig config = new BankruptcyStageConfig();
			config.manaSicknessAmplifier = 4;
			config.durationTicks = 4 * 20;
			config.instantManaDrainPercent = 20.0;
			return config;
		}

		private static BankruptcyStageConfig defaultStageSeven() {
			BankruptcyStageConfig config = new BankruptcyStageConfig();
			config.manaSicknessAmplifier = 5;
			config.durationTicks = 3 * 20;
			config.instantManaDrainPercent = 20.0;
			config.abilityLockTicks = 3 * 20;
			config.cancelActiveAbilities = true;
			return config;
		}

		private static BankruptcyStageConfig defaultStageEight() {
			BankruptcyStageConfig config = new BankruptcyStageConfig();
			config.manaSicknessAmplifier = 6;
			config.durationTicks = 3 * 20;
			config.instantManaDrainPercent = 20.0;
			config.abilityLockTicks = 10 * 20;
			config.cancelActiveAbilities = true;
			return config;
		}
	}

	public static final class BankruptcyStageConfig {
		public int manaSicknessAmplifier = -1;
		public int durationTicks = 0;
		public double instantManaDrainPercent = 0.0;
		public int abilityLockTicks = 0;
		public boolean cancelActiveAbilities = false;
		public boolean preserveMaximumAbilities = true;
		public boolean preserveDomainAbilities = true;

		void normalize() {
			manaSicknessAmplifier = Math.max(-1, manaSicknessAmplifier);
			durationTicks = Math.max(0, durationTicks);
			instantManaDrainPercent = MathHelper.clamp(instantManaDrainPercent, 0.0, 100.0);
			abilityLockTicks = Math.max(0, abilityLockTicks);
		}
	}
}

