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

abstract class CoreConfigModels extends ConfigModelSupport {
	public static final class ManaConfig {
		public int belowFreezingDrainPerSecond = 5;
		public int absoluteZeroDrainPerSecond = 20;
		public int loveAtFirstSightIdleDrainPerSecond = 2;
		public int loveAtFirstSightActiveDrainPerSecond = 5;
		public double martyrsFlameDrainPercentPerSecond = 1.0;
		public double tillDeathDoUsPartDrainPercentPerSecond = 3.0;
		@SerializedName(value = "emptyEmbraceActivationCost", alternate = { "manipulationActivationCost" })
		public int emptyEmbraceActivationCost = 0;
		@SerializedName(value = "emptyEmbraceDrainPerSecond", alternate = { "manipulationDrainPerSecond" })
		public double emptyEmbraceDrainPerSecond = 2.5;
		public int domainExpansionActivationCost = 0;
		public int passiveRegenPerSecond = 2;
		public int depletedRecoveryRegenPerSecond = 1;
		public int planckHeatActivationCost = 75;

		void normalize() {
			belowFreezingDrainPerSecond = Math.max(0, belowFreezingDrainPerSecond);
			absoluteZeroDrainPerSecond = Math.max(0, absoluteZeroDrainPerSecond);
			loveAtFirstSightIdleDrainPerSecond = Math.max(0, loveAtFirstSightIdleDrainPerSecond);
			loveAtFirstSightActiveDrainPerSecond = Math.max(0, loveAtFirstSightActiveDrainPerSecond);
			martyrsFlameDrainPercentPerSecond = MathHelper.clamp(martyrsFlameDrainPercentPerSecond, 0.0, 100.0);
			tillDeathDoUsPartDrainPercentPerSecond = MathHelper.clamp(tillDeathDoUsPartDrainPercentPerSecond, 0.0, 100.0);
			emptyEmbraceActivationCost = Math.max(0, emptyEmbraceActivationCost);
			emptyEmbraceDrainPerSecond = Math.max(0.0, emptyEmbraceDrainPerSecond);
			domainExpansionActivationCost = Math.max(0, domainExpansionActivationCost);
			passiveRegenPerSecond = MathHelper.clamp(passiveRegenPerSecond, 0, 100);
			depletedRecoveryRegenPerSecond = MathHelper.clamp(depletedRecoveryRegenPerSecond, 0, 100);
			planckHeatActivationCost = Math.max(0, planckHeatActivationCost);
		}
	}

	public static final class TimingConfig {
		public int effectRefreshTicks = 40;
		public int frostbiteDurationTicks = 120;
		public int frostVisualTicks = 180;
		public int absoluteZeroAuraDamageIntervalTicks = 50;
		public int absoluteZeroCooldownTicks = 1200;
		public int planckHeatCooldownTicks = 12000;
		public int planckHeatFrostPhaseTicks = 120;
		public int planckHeatFirePhaseTicks = 300;
		public int planckHeatFrostDamageIntervalTicks = 20;
		public int planckHeatEnhancedFireDamageIntervalTicks = 20;
		public int planckHeatAttackEnhancedFireDurationTicks = 160;
		public int planckHeatAuraEnhancedFireDurationTicks = 80;
		public int planckHeatAbsorptionDurationTicks = 2400;
		public int planckHeatStrengthDurationTicks = 2400;
		public int planckHeatFireResistanceDurationTicks = 9600;
		public int loveLockEffectTicks = 5;
		public int martyrsFlameCooldownTicks = 600;
		public int martyrsFlameFireDurationTicks = 80;
		public int tillDeathDoUsPartLinkDurationTicks = 600;
		public int tillDeathDoUsPartCooldownTicks = 3000;
		@SerializedName(value = "emptyEmbraceCooldownTicks", alternate = { "manipulationCooldownTicks" })
		public int emptyEmbraceCooldownTicks = 6000;
		@SerializedName(value = "emptyEmbraceRequestDebounceTicks", alternate = { "manipulationRequestDebounceTicks" })
		public int emptyEmbraceRequestDebounceTicks = 6;
		public int domainExpansionDurationTicks = 1200;
		public int loveDomainCooldownTicks = 36000;
		public int domainClashRegenerationRefreshTicks = 40;
	}

	public static final class DamageConfig {
		public float frostbiteDamage = 2.0F;
		public float absoluteZeroAuraDamage = 4.0F;
		public float planckHeatFrostDamage = 14.0F;
		public float planckHeatEnhancedFireDamage = 6.0F;
		public float martyrsFlameRetaliationDamage = 2.0F;
	}

	public static final class RadiiConfig {
		public int belowFreezingAuraRadius = 1;
		public int absoluteZeroAuraRadius = 4;
		public int planckHeatAuraRadius = 8;
		public int loveGazeRange = 32;
		@SerializedName(value = "emptyEmbraceAcquireRange", alternate = { "manipulationAcquireRange" })
		public int emptyEmbraceAcquireRange = 100;
		@SerializedName(value = "emptyEmbraceBreakRange", alternate = { "manipulationBreakRange" })
		public int emptyEmbraceBreakRange = 100;
		public int domainExpansionRadius = 25;
		public int domainExpansionHeight = 25;
		public int domainExpansionShellThickness = 1;
	}

	public static final class PotionEffectsConfig {
		public int frostbiteSlownessAmplifier = 3;
		public int belowFreezingAuraSlownessAmplifier = 3;
		public int belowFreezingResistanceAmplifier = 0;
		public int absoluteZeroResistanceAmplifier = 2;
		public int absoluteZeroAuraSlownessAmplifier = 2;
		public int planckHeatFrostSlownessAmplifier = 4;
		public int planckHeatAbsorptionAmplifier = 3;
		public int planckHeatStrengthAmplifier = 1;
		public int planckHeatFireResistanceAmplifier = 0;
		public int planckHeatFirePhaseHungerAmplifier = 1;
		public int planckHeatFirePhaseNauseaAmplifier = 1;
		public int loveLockSlownessAmplifier = 255;
		public int loveLockMiningFatigueAmplifier = 255;
		public int domainClashRegenerationAmplifier = 9;
		public int domainClashInstantHealthAmplifier = 9;
		public int domainClashResistanceAmplifier = 5;
	}

	public static final class ParticlesConfig {
		public int loveAtFirstSightParticleIntervalTicks = 8;
		public int loveAtFirstSightHeartParticles = 1;
		public int loveAtFirstSightHappyVillagerParticles = 0;
	}

	public static final class LoveAtFirstSightConfig {
		public boolean blockItemUse = false;
		public boolean blockAttacks = true;
		public int resistanceAmplifier = 0;
		public boolean preventCasterDirectDamage = true;

		void normalize() {
			resistanceAmplifier = Math.max(-1, resistanceAmplifier);
		}
	}

	public static final class MartyrsFlameConfig {
		public boolean applyGlowingEffect = true;
		public boolean disableManaRegenWhileActive = true;
		public boolean fireIgnoresNormalExtinguish = true;
		public int fireDamageIntervalTicks = 20;
		public float fireDamagePerTick = 1.0F;
		public double fireResistantTargetDamageMultiplier = 1.0;
		public boolean fireDamageIgnoresFireResistance = true;
		public boolean extinguishInWater = false;
		public boolean extinguishInRain = false;
		public int fireParticleIntervalTicks = 4;
		public int fireFlameParticleCount = 6;
		public int fireSmokeParticleCount = 4;

		void normalize() {
			fireDamageIntervalTicks = Math.max(1, fireDamageIntervalTicks);
			fireDamagePerTick = Math.max(0.0F, fireDamagePerTick);
			fireResistantTargetDamageMultiplier = Math.max(0.0, fireResistantTargetDamageMultiplier);
			fireParticleIntervalTicks = Math.max(1, fireParticleIntervalTicks);
			fireFlameParticleCount = Math.max(0, fireFlameParticleCount);
			fireSmokeParticleCount = Math.max(0, fireSmokeParticleCount);
		}
	}
}


