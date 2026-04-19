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

public abstract class BurningPassionConfigModels extends CoreConfigModels {
	public static final class BurningPassionConfig {
		public BurningPassionHudConfig hud = new BurningPassionHudConfig();
		public BurningPassionIgnitionConfig ignition = new BurningPassionIgnitionConfig();
		public BurningPassionHeatConfig heat = new BurningPassionHeatConfig();
		public BurningPassionOverheatConfig overheat = new BurningPassionOverheatConfig();
		public BurningPassionStageConfig stageOne = defaultStageOne();
		public BurningPassionStageConfig stageTwo = defaultStageTwo();
		public BurningPassionStageConfig stageThree = defaultStageThree();
		public SearingDashConfig searingDash = new SearingDashConfig();
		public CinderMarkConfig cinderMark = new CinderMarkConfig();
		public EngineHeartConfig engineHeart = new EngineHeartConfig();
		public BurningPassionManaOverflowConfig manaOverflow = new BurningPassionManaOverflowConfig();
		public OverrideConfig override = new OverrideConfig();

		public void normalize() {
			if (hud == null) {
				hud = new BurningPassionHudConfig();
			}
			if (ignition == null) {
				ignition = new BurningPassionIgnitionConfig();
			}
			if (heat == null) {
				heat = new BurningPassionHeatConfig();
			}
			if (overheat == null) {
				overheat = new BurningPassionOverheatConfig();
			}
			if (stageOne == null) {
				stageOne = defaultStageOne();
			}
			if (stageTwo == null) {
				stageTwo = defaultStageTwo();
			}
			if (stageThree == null) {
				stageThree = defaultStageThree();
			}
			if (searingDash == null) {
				searingDash = new SearingDashConfig();
			}
			if (cinderMark == null) {
				cinderMark = new CinderMarkConfig();
			}
			if (engineHeart == null) {
				engineHeart = new EngineHeartConfig();
			}
			if (manaOverflow == null) {
				manaOverflow = new BurningPassionManaOverflowConfig();
			}
			if (override == null) {
				override = new OverrideConfig();
			}

			hud.normalize();
			ignition.normalize();
			heat.normalize();
			overheat.normalize();
			stageOne.normalize(defaultStageOne());
			stageTwo.normalize(defaultStageTwo());
			stageThree.normalize(defaultStageThree());
			searingDash.normalize();
			cinderMark.normalize();
			engineHeart.normalize();
			manaOverflow.normalize();
			override.normalize();
		}

		private static BurningPassionStageConfig defaultStageOne() {
			BurningPassionStageConfig config = new BurningPassionStageConfig();
			config.auraEnabled = true;
			config.auraRadius = 3.0;
			config.fireRefreshTicks = 8 * 20;
			config.fireDamageIntervalTicks = 20;
			config.fireDamagePerTick = 1.0F;
			config.fireResistantTargetDamageMultiplier = 0.0;
			config.extinguishInWater = true;
			config.extinguishInRain = true;
			config.speedAmplifier = 1;
			config.boundaryHeatGainPercent = 1.5;
			config.boundaryTriggerCooldownTicks = 2 * 20;
			config.passiveHeatPerSecond = -0.5;
			config.waterHeatPerSecond = -0.5;
			return config;
		}

		private static BurningPassionStageConfig defaultStageTwo() {
			BurningPassionStageConfig config = new BurningPassionStageConfig();
			config.auraEnabled = true;
			config.auraRadius = 3.0;
			config.fireRefreshTicks = 8 * 20;
			config.fireDamageIntervalTicks = 20;
			config.fireDamagePerTick = 2.0F;
			config.fireResistantTargetDamageMultiplier = 0.5;
			config.extinguishInWater = true;
			config.extinguishInRain = false;
			config.speedAmplifier = 4;
			config.strengthAmplifier = 1;
			config.regenerationAmplifier = 0;
			config.boundaryHeatGainPercent = 5.0;
			config.boundaryTriggerCooldownTicks = 2 * 20;
			config.persistentFireUntilExtinguished = false;
			config.waterHeatPerSecond = -1.5;
			config.extraStepHeightBlocks = 1.0;
			return config;
		}

		private static BurningPassionStageConfig defaultStageThree() {
			BurningPassionStageConfig config = new BurningPassionStageConfig();
			config.auraEnabled = false;
			config.auraRadius = 0.0;
			config.fireRefreshTicks = 8 * 20;
			config.fireDamageIntervalTicks = 20;
			config.fireDamagePerTick = 2.5F;
			config.fireDamageIgnoresFireResistance = true;
			config.extinguishInWater = false;
			config.extinguishInRain = false;
			config.speedAmplifier = 14;
			config.strengthAmplifier = 2;
			config.regenerationAmplifier = 1;
			config.passiveHeatPerSecond = 0.5;
			config.extraStepHeightBlocks = 2.0;
			return config;
		}
	}

	public static final class BurningPassionHudConfig {
		public boolean timerEnabled = true;
		public boolean heatTextEnabled = true;
		public boolean heatBarEnabled = true;
		public boolean hideDuringDomainClash = true;
		public boolean timerBottomRightAnchor = true;
		public String stageOneTimerLabelFormat = "Stage 2 in %time%";
		public String stageTwoTimerLabelFormat = "Stage 3 in %time%";
		public String stageThreeTimerLabelFormat = "Ignition ends in %time%";
		public String heatLabelFormat = "HEAT: %heat%%";
		public String textColorHex = "#FFFFFF";
		public String heatTextColorHex = "#FFB56B";
		public String outlineColorHex = "#000000";
		public String heatBarStartColorHex = "#FFB347";
		public String heatBarEndColorHex = "#FF3B1F";
		public int lineSpacing = 12;
		public int heatBarYOffset = 13;
		public int stageTextYOffset = 33;
		public int heatTextYOffset = 26;
		public int heatBarHeight = 4;

		public void normalize() {
			if (stageOneTimerLabelFormat == null) {
				stageOneTimerLabelFormat = "Stage 2 in %time%";
			}
			if (stageTwoTimerLabelFormat == null) {
				stageTwoTimerLabelFormat = "Stage 3 in %time%";
			}
			if (stageThreeTimerLabelFormat == null) {
				stageThreeTimerLabelFormat = "Ignition ends in %time%";
			}
			if (heatLabelFormat == null) {
				heatLabelFormat = "HEAT: %heat%%";
			}
			if (textColorHex == null) {
				textColorHex = "#FFFFFF";
			}
			if (heatTextColorHex == null) {
				heatTextColorHex = "#FFB56B";
			}
			if (outlineColorHex == null) {
				outlineColorHex = "#000000";
			}
			if (heatBarStartColorHex == null) {
				heatBarStartColorHex = "#FFB347";
			}
			if (heatBarEndColorHex == null) {
				heatBarEndColorHex = "#FF3B1F";
			}
			lineSpacing = Math.max(8, lineSpacing);
			heatBarYOffset = Math.max(4, heatBarYOffset);
			stageTextYOffset = Math.max(heatBarYOffset + 8, stageTextYOffset);
			heatTextYOffset = Math.max(heatBarYOffset + 6, heatTextYOffset);
			heatBarHeight = Math.max(2, heatBarHeight);
		}
	}

	public static final class BurningPassionIgnitionConfig {
		public int cooldownTicks = 30 * 20;
		public boolean allowManualCancel = true;
		public boolean manualCancelStartsCooldown = true;
		public boolean naturalEndStartsCooldown = false;
		public int stageThreePunishmentCooldownTicks = 60 * 20;
		public boolean stageThreePunishmentUsesOverheatEffects = true;
		public int stageOneDurationTicks = 90 * 20;
		public int stageTwoDurationTicks = 2 * 60 * 20;
		public int stageThreeDurationTicks = 5 * 60 * 20;
		public int effectRefreshTicks = 40;
		public int activationFlameParticleCount = 28;
		public int activationSmokeParticleCount = 1;
		public int activationBurstParticleCount = 14;
		public float activationSoundVolume = 1.0F;
		public float activationSoundPitch = 0.85F;
		public float cancelSoundVolume = 0.9F;
		public float cancelSoundPitch = 0.7F;

		public void normalize() {
			cooldownTicks = Math.max(0, cooldownTicks);
			stageThreePunishmentCooldownTicks = Math.max(0, stageThreePunishmentCooldownTicks);
			stageOneDurationTicks = Math.max(1, stageOneDurationTicks);
			stageTwoDurationTicks = Math.max(1, stageTwoDurationTicks);
			stageThreeDurationTicks = Math.max(1, stageThreeDurationTicks);
			effectRefreshTicks = Math.max(1, effectRefreshTicks);
			activationFlameParticleCount = Math.max(0, activationFlameParticleCount);
			activationSmokeParticleCount = Math.max(0, activationSmokeParticleCount);
			activationBurstParticleCount = Math.max(0, activationBurstParticleCount);
			activationSoundVolume = Math.max(0.0F, activationSoundVolume);
			activationSoundPitch = Math.max(0.0F, activationSoundPitch);
			cancelSoundVolume = Math.max(0.0F, cancelSoundVolume);
			cancelSoundPitch = Math.max(0.0F, cancelSoundPitch);
		}
	}

	public static final class BurningPassionHeatConfig {
		public double selfFireThresholdPercent = 50.0;
		public double healthDemandThresholdPercent = 75.0;
		public double overheatThresholdPercent = 100.0;
		public boolean ignitionPersistsOnManaDepletion = true;
		public double manaRecoveryUnlockThresholdPercent = 25.0;
		public int selfFireDurationTicks = 40;
		public int selfFireDamageIntervalTicks = 10;
		public float selfFireDamagePerTick = 0.5F;
		public double selfFireResistantDamageMultiplier = 1.0;
		public boolean selfFireIgnoresFireResistance = true;
		public boolean selfFireExtinguishesWhenWetBeforeStageThree = true;
		public double minimumRemainingHealthHearts = 1.0;
		public double ignitionHealthCostHearts = 0.0;
		public double searingDashHealthCostHearts = 0.0;
		public double cinderMarkHealthCostHearts = 0.0;
		public double engineHeartHealthCostHearts = 0.0;
		public double overrideHealthCostHearts = 0.0;

		public void normalize() {
			selfFireThresholdPercent = MathHelper.clamp(selfFireThresholdPercent, 0.0, 100.0);
			healthDemandThresholdPercent = MathHelper.clamp(healthDemandThresholdPercent, selfFireThresholdPercent, 100.0);
			overheatThresholdPercent = MathHelper.clamp(overheatThresholdPercent, healthDemandThresholdPercent, 100.0);
			manaRecoveryUnlockThresholdPercent = MathHelper.clamp(manaRecoveryUnlockThresholdPercent, 0.0, overheatThresholdPercent);
			selfFireDurationTicks = Math.max(1, selfFireDurationTicks);
			selfFireDamageIntervalTicks = Math.max(1, selfFireDamageIntervalTicks);
			selfFireDamagePerTick = Math.max(0.0F, selfFireDamagePerTick);
			selfFireResistantDamageMultiplier = Math.max(0.0, selfFireResistantDamageMultiplier);
			minimumRemainingHealthHearts = Math.max(0.5, minimumRemainingHealthHearts);
			ignitionHealthCostHearts = Math.max(0.0, ignitionHealthCostHearts);
			searingDashHealthCostHearts = Math.max(0.0, searingDashHealthCostHearts);
			cinderMarkHealthCostHearts = Math.max(0.0, cinderMarkHealthCostHearts);
			engineHeartHealthCostHearts = Math.max(0.0, engineHeartHealthCostHearts);
			overrideHealthCostHearts = Math.max(0.0, overrideHealthCostHearts);
		}
	}

	public static final class BurningPassionOverheatConfig {
		public int stagedAbilityCooldownTicks = 30 * 20;
		public boolean includeOverrideInLockout = false;
		public int punishmentDurationTicks = 8 * 20;
		public int slownessAmplifier = 4;
		public int weaknessAmplifier = 2;
		public int flameBurstParticleCount = 64;
		public int smokeParticleCount = 42;
		public float soundVolume = 1.2F;
		public float soundPitch = 0.6F;

		public void normalize() {
			stagedAbilityCooldownTicks = Math.max(0, stagedAbilityCooldownTicks);
			punishmentDurationTicks = Math.max(0, punishmentDurationTicks);
			slownessAmplifier = Math.max(-1, slownessAmplifier);
			weaknessAmplifier = Math.max(-1, weaknessAmplifier);
			flameBurstParticleCount = Math.max(0, flameBurstParticleCount);
			smokeParticleCount = Math.max(0, smokeParticleCount);
			soundVolume = Math.max(0.0F, soundVolume);
			soundPitch = Math.max(0.0F, soundPitch);
		}
	}

	public static final class BurningPassionStageConfig {
		public boolean auraEnabled = false;
		public double auraRadius = 0.0;
		public int fireRefreshTicks = 40;
		public int fireDamageIntervalTicks = 10;
		public float fireDamagePerTick = 0.0F;
		public double fireResistantTargetDamageMultiplier = 1.0;
		public boolean fireDamageIgnoresFireResistance = false;
		public boolean persistentFireUntilExtinguished = false;
		public boolean extinguishInWater = true;
		public boolean extinguishInRain = true;
		public int speedAmplifier = -1;
		public int strengthAmplifier = -1;
		public int regenerationAmplifier = -1;
		public double boundaryHeatGainPercent = 0.0;
		public int boundaryTriggerCooldownTicks = 0;
		public Double passiveHeatPerSecond = null;
		public int auraFlameParticleCount = 12;
		public int auraSmokeParticleCount = 0;
		public Double waterHeatPerSecond = null;
		public double extraStepHeightBlocks = 0.0;

		public void normalize(BurningPassionStageConfig defaults) {
			auraRadius = Math.max(0.0, auraRadius);
			fireRefreshTicks = Math.max(1, fireRefreshTicks);
			fireDamageIntervalTicks = Math.max(1, fireDamageIntervalTicks);
			fireDamagePerTick = Math.max(0.0F, fireDamagePerTick);
			fireResistantTargetDamageMultiplier = Math.max(0.0, fireResistantTargetDamageMultiplier);
			speedAmplifier = Math.max(-1, speedAmplifier);
			strengthAmplifier = Math.max(-1, strengthAmplifier);
			regenerationAmplifier = Math.max(-1, regenerationAmplifier);
			boundaryHeatGainPercent = Math.max(0.0, boundaryHeatGainPercent);
			boundaryTriggerCooldownTicks = Math.max(0, boundaryTriggerCooldownTicks);
			double defaultPassiveHeatPerSecond = defaults == null || defaults.passiveHeatPerSecond == null ? 0.0 : defaults.passiveHeatPerSecond;
			passiveHeatPerSecond = MathHelper.clamp(passiveHeatPerSecond == null ? defaultPassiveHeatPerSecond : passiveHeatPerSecond, -100.0, 100.0);
			auraFlameParticleCount = Math.max(0, auraFlameParticleCount);
			auraSmokeParticleCount = Math.max(0, auraSmokeParticleCount);
			double defaultWaterHeatPerSecond = defaults == null || defaults.waterHeatPerSecond == null ? 0.0 : defaults.waterHeatPerSecond;
			waterHeatPerSecond = MathHelper.clamp(waterHeatPerSecond == null ? defaultWaterHeatPerSecond : waterHeatPerSecond, -100.0, 100.0);
			extraStepHeightBlocks = MathHelper.clamp(extraStepHeightBlocks, 0.0, 10.0);
		}
	}

	public static final class SearingDashConfig {
		public double manaCostPercent = 15.0;
		public double heatGainPercent = 5.0;
		public int cooldownTicks = 2 * 20;
		public int dashTicks = 4;
		public double dashSpeedBlocksPerTick = 1.85;
		public double collisionRadius = 0.85;
		public boolean affectPlayers = true;
		public boolean affectMobs = true;
		public int trailDurationTicks = 2 * 20;
		public double trailPointSpacingBlocks = 0.5;
		public boolean trailDamagesEachTargetOncePerLine = true;
		public float trailTrueDamage = 4.0F;
		public float impactExplosionDamage = 10.0F;
		public double impactKnockbackHorizontalVelocity = 1.25;
		public double impactKnockbackVerticalVelocity = 0.45;
		public int dashFlameParticleCount = 18;
		public int dashSmokeParticleCount = 4;
		public int trailFlameParticleCount = 10;
		public int trailSmokeParticleCount = 0;
		public int trailEmberParticleCount = 6;
		public int impactExplosionParticleCount = 18;
		public double particleSpread = 0.22;
		public float dashSoundVolume = 1.0F;
		public float dashSoundPitch = 1.2F;
		public float impactSoundVolume = 0.95F;
		public float impactSoundPitch = 0.9F;

		public void normalize() {
			manaCostPercent = MathHelper.clamp(manaCostPercent, 0.0, 100.0);
			heatGainPercent = MathHelper.clamp(heatGainPercent, -100.0, 100.0);
			cooldownTicks = Math.max(0, cooldownTicks);
			dashTicks = Math.max(1, dashTicks);
			dashSpeedBlocksPerTick = Math.max(0.1, dashSpeedBlocksPerTick);
			collisionRadius = Math.max(0.1, collisionRadius);
			trailDurationTicks = Math.max(1, trailDurationTicks);
			trailPointSpacingBlocks = Math.max(0.05, trailPointSpacingBlocks);
			trailTrueDamage = Math.max(0.0F, trailTrueDamage);
			impactExplosionDamage = Math.max(0.0F, impactExplosionDamage);
			impactKnockbackHorizontalVelocity = Math.max(0.0, impactKnockbackHorizontalVelocity);
			impactKnockbackVerticalVelocity = Math.max(0.0, impactKnockbackVerticalVelocity);
			dashFlameParticleCount = Math.max(0, dashFlameParticleCount);
			dashSmokeParticleCount = Math.max(0, dashSmokeParticleCount);
			trailFlameParticleCount = Math.max(0, trailFlameParticleCount);
			trailSmokeParticleCount = Math.max(0, trailSmokeParticleCount);
			trailEmberParticleCount = Math.max(0, trailEmberParticleCount);
			impactExplosionParticleCount = Math.max(0, impactExplosionParticleCount);
			particleSpread = Math.max(0.0, particleSpread);
			dashSoundVolume = Math.max(0.0F, dashSoundVolume);
			dashSoundPitch = Math.max(0.0F, dashSoundPitch);
			impactSoundVolume = Math.max(0.0F, impactSoundVolume);
			impactSoundPitch = Math.max(0.0F, impactSoundPitch);
		}
	}

	public static final class CinderMarkConfig {
		public double manaCostPercent = 15.0;
		public double heatReductionPercent = 5.0;
		public double selfHealthCostHearts = 2.0;
		public int cooldownTicks = 7 * 20;
		public int readyTimeoutTicks = 0;
		public int autoExplodeTicks = 30 * 20;
		public float autoExplodeTrueDamage = 4.0F;
		public float manualDetonationTrueDamage = 4.0F;
		public double manualDetonationManaCostPercentPerMark = 5.0;
		public int armedHitMarkCount = 1;
		public int engineHeartFreeMarkCount = 1;
		public boolean affectPlayers = true;
		public boolean affectMobs = true;
		public double latchScale = 0.32;
		public int latchFlameParticleCount = 6;
		public int latchSmokeParticleCount = 0;
		public int autoExplodeParticleCount = 12;
		public int manualDetonationParticleCount = 18;
		public int freeMarkBurstParticleCount = 10;
		public float soundVolume = 1.0F;
		public float soundPitch = 1.05F;

		public void normalize() {
			manaCostPercent = MathHelper.clamp(manaCostPercent, 0.0, 100.0);
			heatReductionPercent = MathHelper.clamp(heatReductionPercent, 0.0, 100.0);
			selfHealthCostHearts = Math.max(0.0, selfHealthCostHearts);
			cooldownTicks = Math.max(0, cooldownTicks);
			readyTimeoutTicks = Math.max(0, readyTimeoutTicks);
			autoExplodeTicks = Math.max(1, autoExplodeTicks);
			autoExplodeTrueDamage = Math.max(0.0F, autoExplodeTrueDamage);
			manualDetonationTrueDamage = Math.max(0.0F, manualDetonationTrueDamage);
			manualDetonationManaCostPercentPerMark = MathHelper.clamp(manualDetonationManaCostPercentPerMark, 0.0, 100.0);
			armedHitMarkCount = Math.max(0, armedHitMarkCount);
			engineHeartFreeMarkCount = Math.max(0, engineHeartFreeMarkCount);
			latchScale = MathHelper.clamp(latchScale, 0.05, 2.0);
			latchFlameParticleCount = Math.max(0, latchFlameParticleCount);
			latchSmokeParticleCount = Math.max(0, latchSmokeParticleCount);
			autoExplodeParticleCount = Math.max(0, autoExplodeParticleCount);
			manualDetonationParticleCount = Math.max(0, manualDetonationParticleCount);
			freeMarkBurstParticleCount = Math.max(0, freeMarkBurstParticleCount);
			soundVolume = Math.max(0.0F, soundVolume);
			soundPitch = Math.max(0.0F, soundPitch);
		}
	}

	public static final class EngineHeartConfig {
		public boolean requiresSprinting = true;
		public boolean notificationsEnabled = true;
		public int notificationDurationTicks = 40;
		public int tierOneThresholdTicks = 3 * 20;
		public int tierTwoThresholdTicks = 7 * 20;
		public int tierThreeThresholdTicks = 12 * 20;
		public int decayOneTierTicks = 15;
		public int decayTwoTiersTicks = 30;
		public int fullResetTicks = 45;
		public double minimumSpeedBlocksPerTick = 0.18;
		public double momentumToleranceBlocksPerTick = 0.03;
		public double tierOneHeatReductionPerSecond = 1.5;
		public double tierTwoAttackBonus = 2.0;
		public float tierThreeSpecialAttackDamage = 85.0F;
		public double tierThreeSpecialHeatGainPercent = 30.0;
		public double specialKnockbackHorizontalVelocity = 1.6;
		public double specialKnockbackVerticalVelocity = 0.5;
		public boolean particlesEnabled = true;
		public boolean tierThreeSoundBarrierEnabled = true;
		public int tierThreeSoundBarrierRingParticleCount = 16;
		public double tierThreeSoundBarrierRingRadius = 0.85;
		public double tierThreeSoundBarrierBackOffsetBlocks = 0.55;
		public double tierThreeSoundBarrierVerticalAmplitude = 0.16;
		public int tierThreeSoundBarrierWakeParticleCount = 12;
		public double tierThreeSoundBarrierWakeLengthBlocks = 1.8;
		public int tierThreeSoundBarrierUnlockBurstParticleCount = 26;
		@SerializedName(value = "afterimageTrail", alternate = { "tierThreeAfterimageTrail" })
		public EngineHeartAfterimageConfig afterimageTrail = new EngineHeartAfterimageConfig();
		public int tierThreeBurstParticleCount = 28;
		public int tierThreeExplosionParticleCount = 32;
		public double particleSpread = 0.2;
		public float tierUnlockSoundVolume = 0.9F;
		public float tierUnlockSoundPitch = 1.2F;
		public float specialSoundVolume = 1.15F;
		public float specialSoundPitch = 0.85F;

		public void normalize() {
			notificationDurationTicks = Math.max(0, notificationDurationTicks);
			tierOneThresholdTicks = Math.max(1, tierOneThresholdTicks);
			tierTwoThresholdTicks = Math.max(tierOneThresholdTicks, tierTwoThresholdTicks);
			tierThreeThresholdTicks = Math.max(tierTwoThresholdTicks, tierThreeThresholdTicks);
			decayOneTierTicks = Math.max(1, decayOneTierTicks);
			decayTwoTiersTicks = Math.max(decayOneTierTicks, decayTwoTiersTicks);
			fullResetTicks = Math.max(decayTwoTiersTicks, fullResetTicks);
			minimumSpeedBlocksPerTick = Math.max(0.0, minimumSpeedBlocksPerTick);
			momentumToleranceBlocksPerTick = Math.max(0.0, momentumToleranceBlocksPerTick);
			tierOneHeatReductionPerSecond = Math.max(0.0, tierOneHeatReductionPerSecond);
			tierTwoAttackBonus = Math.max(0.0, tierTwoAttackBonus);
			tierThreeSpecialAttackDamage = Math.max(0.0F, tierThreeSpecialAttackDamage);
			tierThreeSpecialHeatGainPercent = MathHelper.clamp(tierThreeSpecialHeatGainPercent, -100.0, 100.0);
			specialKnockbackHorizontalVelocity = Math.max(0.0, specialKnockbackHorizontalVelocity);
			specialKnockbackVerticalVelocity = Math.max(0.0, specialKnockbackVerticalVelocity);
			tierThreeSoundBarrierRingParticleCount = Math.max(0, tierThreeSoundBarrierRingParticleCount);
			tierThreeSoundBarrierRingRadius = Math.max(0.0, tierThreeSoundBarrierRingRadius);
			tierThreeSoundBarrierBackOffsetBlocks = Math.max(0.0, tierThreeSoundBarrierBackOffsetBlocks);
			tierThreeSoundBarrierVerticalAmplitude = Math.max(0.0, tierThreeSoundBarrierVerticalAmplitude);
			tierThreeSoundBarrierWakeParticleCount = Math.max(0, tierThreeSoundBarrierWakeParticleCount);
			tierThreeSoundBarrierWakeLengthBlocks = Math.max(0.0, tierThreeSoundBarrierWakeLengthBlocks);
			tierThreeSoundBarrierUnlockBurstParticleCount = Math.max(0, tierThreeSoundBarrierUnlockBurstParticleCount);
			if (afterimageTrail == null) {
				afterimageTrail = new EngineHeartAfterimageConfig();
			}
			afterimageTrail.normalize();
			tierThreeBurstParticleCount = Math.max(0, tierThreeBurstParticleCount);
			tierThreeExplosionParticleCount = Math.max(0, tierThreeExplosionParticleCount);
			particleSpread = Math.max(0.0, particleSpread);
			tierUnlockSoundVolume = Math.max(0.0F, tierUnlockSoundVolume);
			tierUnlockSoundPitch = Math.max(0.0F, tierUnlockSoundPitch);
			specialSoundVolume = Math.max(0.0F, specialSoundVolume);
			specialSoundPitch = Math.max(0.0F, specialSoundPitch);
		}
	}

	public static final class EngineHeartAfterimageConfig {
		public boolean enabled = true;
		public int tierOneMaxSnapshots = 3;
		public int tierTwoMaxSnapshots = 4;
		public int tierThreeMaxSnapshots = 5;
		public int snapshotIntervalTicks = 2;
		public int snapshotLifetimeTicks = 10;
		public double minimumSpeedThreshold = 0.08;
		public double opacityStart = 0.42;
		public double opacityEnd = 0.0;
		public double tierOneOpacityMultiplier = 0.72;
		public double tierTwoOpacityMultiplier = 0.86;
		public double tierThreeOpacityMultiplier = 1.0;
		public List<String> tintColorHexes = new ArrayList<>(List.of("#FFF2D1", "#FFB063", "#FF7A3E"));
		public double scaleMultiplier = 1.0;
		public boolean distortionLayerEnabled = true;
		public double distortionIntensity = 0.18;
		public boolean bakedAccentEnabled = false;
		public int accentIntervalTicks = 4;
		public int emberSupportParticleCount = 0;
		public double emberSpeed = 0.05;
		public boolean firstPersonReducedMode = true;
		public boolean thirdPersonOnly = false;
		public double renderDistanceLimit = 48.0;

		public void normalize() {
			tierOneMaxSnapshots = MathHelper.clamp(tierOneMaxSnapshots, 1, 12);
			tierTwoMaxSnapshots = MathHelper.clamp(tierTwoMaxSnapshots, tierOneMaxSnapshots, 12);
			tierThreeMaxSnapshots = MathHelper.clamp(tierThreeMaxSnapshots, tierTwoMaxSnapshots, 12);
			snapshotIntervalTicks = Math.max(1, snapshotIntervalTicks);
			snapshotLifetimeTicks = Math.max(1, snapshotLifetimeTicks);
			minimumSpeedThreshold = Math.max(0.0, minimumSpeedThreshold);
			opacityStart = MathHelper.clamp(opacityStart, 0.0, 1.0);
			opacityEnd = MathHelper.clamp(opacityEnd, 0.0, opacityStart);
			tierOneOpacityMultiplier = MathHelper.clamp(tierOneOpacityMultiplier, 0.0, 2.0);
			tierTwoOpacityMultiplier = MathHelper.clamp(tierTwoOpacityMultiplier, 0.0, 2.0);
			tierThreeOpacityMultiplier = MathHelper.clamp(tierThreeOpacityMultiplier, 0.0, 2.0);
			if (tintColorHexes == null) {
				tintColorHexes = new ArrayList<>(List.of("#FFF2D1", "#FFB063", "#FF7A3E"));
			}
			tintColorHexes.removeIf(color -> color == null || color.isBlank());
			if (tintColorHexes.isEmpty()) {
				tintColorHexes = new ArrayList<>(List.of("#FFF2D1", "#FFB063", "#FF7A3E"));
			}
			scaleMultiplier = MathHelper.clamp(scaleMultiplier, 0.5, 2.0);
			distortionIntensity = MathHelper.clamp(distortionIntensity, 0.0, 2.0);
			accentIntervalTicks = Math.max(1, accentIntervalTicks);
			emberSupportParticleCount = Math.max(0, emberSupportParticleCount);
			emberSpeed = Math.max(0.0, emberSpeed);
			renderDistanceLimit = Math.max(8.0, renderDistanceLimit);
		}
	}

	public static final class BurningPassionManaOverflowConfig {
		public boolean enabled = true;
		public boolean allowPartialSubstitution = true;
		public boolean allowZeroManaSubstitution = true;
		public double heatPercentPerMissingManaPercent = 1.0;

		public void normalize() {
			heatPercentPerMissingManaPercent = Math.max(0.0, heatPercentPerMissingManaPercent);
		}
	}

	public static final class OverrideConfig {
		public boolean requiresFullMana = true;
		public boolean consumeFullMana = true;
		public int cooldownTicks = 30 * 60 * 20;
		public double meteorSpawnHeight = 35.0;
		public int fallDurationTicks = 60;
		public double impactRadiusMultiplierFromStandardDomainRadius = 2.0;
		public boolean allowCastInsideContainingDomain = true;
		public boolean allowCastWhenDomainFullyContained = true;
		public boolean destroyAllQualifyingDomains = true;
		public float impactDamage = 2048.0F;
		public boolean affectPlayers = true;
		public boolean affectMobs = true;
		public double meteorCoreDisplayScale = 8.0;
		public int telegraphParticleCount = 72;
		public int meteorCoreParticleCount = 56;
		public int meteorTrailParticleCount = 72;
		public int meteorSmokeParticleCount = 1;
		public int meteorEmberParticleCount = 48;
		public int impactFlashParticleCount = 520;
		public int shockwaveParticleCount = 560;
		public int radialFireBurstParticleCount = 420;
		public int debrisParticleCount = 180;
		public int warningSoundIntervalTicks = 10;
		public float warningSoundVolume = 0.95F;
		public float warningSoundPitch = 0.6F;
		public float impactSoundVolume = 1.4F;
		public float impactSoundPitch = 0.55F;

		public void normalize() {
			cooldownTicks = Math.max(0, cooldownTicks);
			meteorSpawnHeight = Math.max(1.0, meteorSpawnHeight);
			fallDurationTicks = Math.max(1, fallDurationTicks);
			impactRadiusMultiplierFromStandardDomainRadius = Math.max(0.25, impactRadiusMultiplierFromStandardDomainRadius);
			impactDamage = Math.max(0.0F, impactDamage);
			meteorCoreDisplayScale = Math.max(0.5, meteorCoreDisplayScale);
			telegraphParticleCount = Math.max(0, telegraphParticleCount);
			meteorCoreParticleCount = Math.max(0, meteorCoreParticleCount);
			meteorTrailParticleCount = Math.max(0, meteorTrailParticleCount);
			meteorSmokeParticleCount = Math.max(0, meteorSmokeParticleCount);
			meteorEmberParticleCount = Math.max(0, meteorEmberParticleCount);
			impactFlashParticleCount = Math.max(0, impactFlashParticleCount);
			shockwaveParticleCount = Math.max(0, shockwaveParticleCount);
			radialFireBurstParticleCount = Math.max(0, radialFireBurstParticleCount);
			debrisParticleCount = Math.max(0, debrisParticleCount);
			warningSoundIntervalTicks = Math.max(1, warningSoundIntervalTicks);
			warningSoundVolume = Math.max(0.0F, warningSoundVolume);
			warningSoundPitch = Math.max(0.0F, warningSoundPitch);
			impactSoundVolume = Math.max(0.0F, impactSoundVolume);
			impactSoundPitch = Math.max(0.0F, impactSoundPitch);
		}
	}

	public static final class DomainControlConfig {
		public boolean blockTeleportAcrossDomainBoundaries = true;
		public boolean interiorLightingEnabled = true;
		public int interiorLightLevel = 15;
		public int interiorLightHorizontalSpacing = 6;
		public int interiorLightVerticalSpacing = 6;
		public int interiorLightStartYOffset = 2;

		public void normalize() {
			interiorLightLevel = MathHelper.clamp(interiorLightLevel, 0, 15);
			interiorLightHorizontalSpacing = Math.max(1, interiorLightHorizontalSpacing);
			interiorLightVerticalSpacing = Math.max(1, interiorLightVerticalSpacing);
			interiorLightStartYOffset = Math.max(1, interiorLightStartYOffset);
		}
	}

	public static final class EmptyEmbraceConfig {
		public boolean deactivateTargetMagic = true;
		public boolean disableArtifactPowers = true;
		public boolean dismissArtifactSummons = true;
		public boolean disableArtifactArmorEffects = true;
		public boolean disableInfestedSilverfishEnhancements = true;
		public boolean blockArtifactUseClicks = true;
		public boolean blockArtifactAttackClicks = true;
		public boolean debugLogging = true;
	}
}


