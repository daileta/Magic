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

abstract class MagicConfigFrostAccessModels extends MagicConfigGreedModelsC {
	public static final class MagicConfigData {
		public ManaConfig mana = new ManaConfig();
		public TimingConfig timing = new TimingConfig();
		public DamageConfig damage = new DamageConfig();
		public RadiiConfig radii = new RadiiConfig();
		public PotionEffectsConfig potionEffects = new PotionEffectsConfig();
		public ParticlesConfig particles = new ParticlesConfig();
		public LoveAtFirstSightConfig loveAtFirstSight = new LoveAtFirstSightConfig();
		public MartyrsFlameConfig martyrsFlame = new MartyrsFlameConfig();
		public BurningPassionConfig burningPassion = new BurningPassionConfig();
		public DomainControlConfig domainControl = new DomainControlConfig();
		@SerializedName(value = "emptyEmbrace", alternate = { "manipulation" })
		public EmptyEmbraceConfig emptyEmbrace = new EmptyEmbraceConfig();
		public JesterSpotlightConfig jesterSpotlight = new JesterSpotlightConfig();
		public JesterWittyOneLinerConfig jesterWittyOneLiner = new JesterWittyOneLinerConfig();
		public JesterComedicRewriteConfig jesterComedicRewrite = new JesterComedicRewriteConfig();
		public JesterComedicAssistantConfig jesterComedicAssistant = new JesterComedicAssistantConfig();
		public JesterPlusUltraConfig jesterPlusUltra = new JesterPlusUltraConfig();
		public ConstellationCassiopeiaConfig constellationCassiopeia = new ConstellationCassiopeiaConfig();
		public ConstellationHerculesConfig constellationHercules = new ConstellationHerculesConfig();
		@SerializedName("constellationCelestialAlignment")
		public ConstellationCelestialAlignmentConfig constellationCelestialAlignment = new ConstellationCelestialAlignmentConfig();
		public ConstellationOrionConfig constellationOrion = new ConstellationOrionConfig();
		public ConstellationDomainConfig constellationDomain = new ConstellationDomainConfig();
		public GreedConfig greed = new GreedConfig();
		public FrostConfig frost = new FrostConfig();
		public PowderedSnowEffectConfig powderedSnowEffect = new PowderedSnowEffectConfig();
		public DomainClashConfig domainClash = new DomainClashConfig();
		public AbilityAccessConfig abilityAccess = new AbilityAccessConfig();

		void normalize() {
			if (mana == null) {
				mana = new ManaConfig();
			}
			if (timing == null) {
				timing = new TimingConfig();
			}
			if (damage == null) {
				damage = new DamageConfig();
			}
			if (radii == null) {
				radii = new RadiiConfig();
			}
			if (potionEffects == null) {
				potionEffects = new PotionEffectsConfig();
			}
			if (particles == null) {
				particles = new ParticlesConfig();
			}
			if (loveAtFirstSight == null) {
				loveAtFirstSight = new LoveAtFirstSightConfig();
			}
			if (martyrsFlame == null) {
				martyrsFlame = new MartyrsFlameConfig();
			}
			if (burningPassion == null) {
				burningPassion = new BurningPassionConfig();
			}
			if (domainControl == null) {
				domainControl = new DomainControlConfig();
			}
			if (emptyEmbrace == null) {
				emptyEmbrace = new EmptyEmbraceConfig();
			}
			if (jesterSpotlight == null) {
				jesterSpotlight = new JesterSpotlightConfig();
			}
			if (jesterWittyOneLiner == null) {
				jesterWittyOneLiner = new JesterWittyOneLinerConfig();
			}
			if (jesterComedicRewrite == null) {
				jesterComedicRewrite = new JesterComedicRewriteConfig();
			}
			if (jesterComedicAssistant == null) {
				jesterComedicAssistant = new JesterComedicAssistantConfig();
			}
			if (jesterPlusUltra == null) {
				jesterPlusUltra = new JesterPlusUltraConfig();
			}
			if (constellationCassiopeia == null) {
				constellationCassiopeia = new ConstellationCassiopeiaConfig();
			}
			if (constellationHercules == null) {
				constellationHercules = new ConstellationHerculesConfig();
			}
			if (constellationCelestialAlignment == null) {
				constellationCelestialAlignment = new ConstellationCelestialAlignmentConfig();
			}
			if (constellationOrion == null) {
				constellationOrion = new ConstellationOrionConfig();
			}
			if (constellationDomain == null) {
				constellationDomain = new ConstellationDomainConfig();
			}
			if (greed == null) {
				greed = new GreedConfig();
			}
			if (frost == null) {
				frost = new FrostConfig();
			}
			if (powderedSnowEffect == null) {
				powderedSnowEffect = new PowderedSnowEffectConfig();
			}
			if (domainClash == null) {
				domainClash = new DomainClashConfig();
			}
			if (abilityAccess == null) {
				abilityAccess = new AbilityAccessConfig();
			}

			mana.normalize();
			martyrsFlame.normalize();
			burningPassion.normalize();
			loveAtFirstSight.normalize();
			jesterSpotlight.normalize();
			jesterWittyOneLiner.normalize();
			jesterComedicRewrite.normalize();
			jesterComedicAssistant.normalize();
			jesterPlusUltra.normalize();
			constellationCassiopeia.normalize();
			constellationHercules.normalize();
			constellationCelestialAlignment.normalize();
			constellationOrion.normalize();
			constellationDomain.normalize();
			greed.normalize();
			frost.normalize();
			domainControl.normalize();
			domainClash.normalize();
			abilityAccess.normalize();
		}
	}

	public static final class PowderedSnowEffectConfig {
		public int damageIntervalTicks = 20;
		public boolean dealDamageOnInitialApplication = true;
	}

	public static final class FrostConfig {
		public double stagedModeForceEndThresholdPercent = 30.0;
		public int stagedModeCooldownTicks = 600;
		public int stageAdvanceCooldownTicks = 0;
		public int stageAttackCooldownTicks = 0;
		public FrostHudConfig hud = new FrostHudConfig();
		public FrostProgressConfig progression = new FrostProgressConfig();
		public FrostStageConfig stageOne = defaultStageOne();
		public FrostStageConfig stageTwo = defaultStageTwo();
		public FrostStageConfig stageThree = defaultStageThree();
		public FrostRangedAttackConfig rangedAttack = new FrostRangedAttackConfig();
		public FrostSlamConfig stageTwoSlam = defaultStageTwoSlam();
		public FrostSlamConfig stageThreeSlam = defaultStageThreeSlam();
		public FrostDebuffConfig debuff = new FrostDebuffConfig();
		public FrostMaximumConfig maximum = new FrostMaximumConfig();
		public FrostDomainConfig domain = new FrostDomainConfig();

		void normalize() {
			if (hud == null) {
				hud = new FrostHudConfig();
			}
			if (progression == null) {
				progression = new FrostProgressConfig();
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
			if (rangedAttack == null) {
				rangedAttack = new FrostRangedAttackConfig();
			}
			if (stageTwoSlam == null) {
				stageTwoSlam = defaultStageTwoSlam();
			}
			if (stageThreeSlam == null) {
				stageThreeSlam = defaultStageThreeSlam();
			}
			if (debuff == null) {
				debuff = new FrostDebuffConfig();
			}
			if (maximum == null) {
				maximum = new FrostMaximumConfig();
			}
			if (domain == null) {
				domain = new FrostDomainConfig();
			}

			stagedModeForceEndThresholdPercent = MathHelper.clamp(stagedModeForceEndThresholdPercent, 0.0, 100.0);
			stagedModeCooldownTicks = Math.max(0, stagedModeCooldownTicks);
			stageAdvanceCooldownTicks = Math.max(0, stageAdvanceCooldownTicks);
			stageAttackCooldownTicks = Math.max(0, stageAttackCooldownTicks);
			hud.normalize();
			progression.normalize();
			stageOne.normalize();
			stageTwo.normalize();
			stageThree.normalize();
			rangedAttack.normalize();
			stageTwoSlam.normalize();
			stageThreeSlam.normalize();
			debuff.normalize();
			maximum.normalize();
			domain.normalize();
		}

		private static FrostStageConfig defaultStageOne() {
			FrostStageConfig config = new FrostStageConfig();
			config.auraRadius = 1.0;
			config.onHitDebuffDurationTicks = 3 * 20;
			config.resistanceAmplifier = 0;
			config.onHitFrostDamagePerTick = 2.0F;
			return config;
		}

		private static FrostStageConfig defaultStageTwo() {
			FrostStageConfig config = new FrostStageConfig();
			config.onHitDebuffDurationTicks = 5 * 20;
			config.resistanceAmplifier = 2;
			config.slownessAmplifier = 2;
			config.onHitSlownessAmplifier = 2;
			config.onHitFrostDamagePerTick = 1.5F;
			return config;
		}

		private static FrostStageConfig defaultStageThree() {
			FrostStageConfig config = new FrostStageConfig();
			config.onHitDebuffDurationTicks = 7 * 20;
			config.resistanceAmplifier = 2;
			config.slownessAmplifier = 3;
			config.onHitSlownessAmplifier = 3;
			config.onHitBlindnessAmplifier = 0;
			config.onHitFrostDamagePerTick = 2.0F;
			config.enableMaxHealthMultiplier = true;
			config.maxHealthMultiplier = 2.0;
			return config;
		}

		private static FrostSlamConfig defaultStageTwoSlam() {
			FrostSlamConfig config = new FrostSlamConfig();
			config.radius = 5.0;
			config.freezeDurationTicks = 2 * 20;
			config.trueDamage = 3.0F;
			config.manaCostPercent = 45.0;
			config.setbackChancePercent = 10.0;
			config.setbackProgressPercent = 50.0;
			config.particleCount = 42;
			config.groundRingRadius = 5.0;
			config.hazeDensity = 24;
			config.shardVelocity = 0.26;
			return config;
		}

		private static FrostSlamConfig defaultStageThreeSlam() {
			FrostSlamConfig config = new FrostSlamConfig();
			config.radius = 10.0;
			config.freezeDurationTicks = 5 * 20;
			config.trueDamage = 7.0F;
			config.manaCostPercent = 45.0;
			config.setbackChancePercent = 0.0;
			config.setbackProgressPercent = 0.0;
			config.particleCount = 68;
			config.groundRingRadius = 10.0;
			config.hazeDensity = 36;
			config.shardVelocity = 0.38;
			return config;
		}
	}

	public static final class FrostHudConfig {
		public boolean timerEnabled = true;
		public boolean readyTextEnabled = true;
		public boolean showFinalStageText = true;
		public String timerLabelFormat = "Stage %stage% in %time%";
		public String readyLabelFormat = "Stage %stage% Ready";
		public String maximumTimerLabelFormat = "Maximum in %time%";
		public String maximumReadyLabelFormat = "Maximum Ready";
		public String finalStageText = "Final Stage";
		public String textColorHex = "#FFFFFF";
		public String outlineColorHex = "#000000";

		void normalize() {
			if (timerLabelFormat == null) {
				timerLabelFormat = "Stage %stage% in %time%";
			}
			if (readyLabelFormat == null) {
				readyLabelFormat = "Stage %stage% Ready";
			}
			if (maximumTimerLabelFormat == null) {
				maximumTimerLabelFormat = "Maximum in %time%";
			}
			if (maximumReadyLabelFormat == null) {
				maximumReadyLabelFormat = "Maximum Ready";
			}
			if (finalStageText == null) {
				finalStageText = "Final Stage";
			}
			if (textColorHex == null) {
				textColorHex = "#FFFFFF";
			}
			if (outlineColorHex == null) {
				outlineColorHex = "#000000";
			}
		}
	}

	public static final class FrostProgressConfig {
		public int stageTwoUnlockTicks = 60 * 20;
		public int stageThreeUnlockTicks = 90 * 20;
		public int maximumUnlockTicks = 5 * 60 * 20;
		public int rangedKillProgressTicks = 2 * 60 * 20;
		public int slamKillProgressTicks = 60 * 20;
		public boolean clearUnlocksOnEnd = true;
		public boolean discardExcessProgress = true;
		public boolean resetProgressOnSetback = true;

		void normalize() {
			stageTwoUnlockTicks = Math.max(1, stageTwoUnlockTicks);
			stageThreeUnlockTicks = Math.max(1, stageThreeUnlockTicks);
			maximumUnlockTicks = Math.max(1, maximumUnlockTicks);
			rangedKillProgressTicks = Math.max(0, rangedKillProgressTicks);
			slamKillProgressTicks = Math.max(0, slamKillProgressTicks);
		}
	}

	public static final class FrostStageConfig {
		public double auraRadius = 0.0;
		public int onHitDebuffDurationTicks = 0;
		public int resistanceAmplifier = -1;
		public int slownessAmplifier = -1;
		public int onHitSlownessAmplifier = -1;
		public int onHitBlindnessAmplifier = -1;
		public float onHitFrostDamagePerTick = 0.0F;
		public boolean cleanseCommonNegatives = true;
		public boolean enableMaxHealthMultiplier = false;
		public double maxHealthMultiplier = 1.0;

		void normalize() {
			auraRadius = Math.max(0.0, auraRadius);
			onHitDebuffDurationTicks = Math.max(0, onHitDebuffDurationTicks);
			resistanceAmplifier = Math.max(-1, resistanceAmplifier);
			slownessAmplifier = Math.max(-1, slownessAmplifier);
			onHitSlownessAmplifier = Math.max(-1, onHitSlownessAmplifier);
			onHitBlindnessAmplifier = Math.max(-1, onHitBlindnessAmplifier);
			onHitFrostDamagePerTick = Math.max(0.0F, onHitFrostDamagePerTick);
			maxHealthMultiplier = Math.max(1.0, maxHealthMultiplier);
		}
	}

	public static final class FrostRangedAttackConfig {
		public double width = 1.0;
		public double range = 40.0;
		public double speedBlocksPerSecond = 10;
		public float baseDamage = 5.0F;
		public double normalManaCostPercent = 25.0;
		public double overcastChancePercent = 20.0;
		public double overcastManaCostPercent = 75.0;
		public double overcastSpeedBlocksPerSecond = 20;
		public boolean instantKillEnabled = true;
		public double setbackChancePercent = 10.0;
		public double particleSpacing = 0.35;
		public double terrainFollowStepSize = 0.25;
		public int groundSearchDownBlocks = 5;
		public int groundSearchUpBlocks = 2;
		public double eruptionSegmentSpacing = 0.25;

		void normalize() {
			width = Math.max(0.1, width);
			range = Math.max(1.0, range);
			speedBlocksPerSecond = Math.max(0.1, speedBlocksPerSecond);
			baseDamage = Math.max(0.0F, baseDamage);
			normalManaCostPercent = MathHelper.clamp(normalManaCostPercent, 0.0, 100.0);
			overcastChancePercent = MathHelper.clamp(overcastChancePercent, 0.0, 100.0);
			overcastManaCostPercent = MathHelper.clamp(overcastManaCostPercent, 0.0, 100.0);
			overcastSpeedBlocksPerSecond = Math.max(0.1, overcastSpeedBlocksPerSecond);
			setbackChancePercent = MathHelper.clamp(setbackChancePercent, 0.0, 100.0);
			particleSpacing = Math.max(0.1, particleSpacing);
			terrainFollowStepSize = Math.max(0.05, terrainFollowStepSize);
			groundSearchDownBlocks = Math.max(0, groundSearchDownBlocks);
			groundSearchUpBlocks = Math.max(0, groundSearchUpBlocks);
			eruptionSegmentSpacing = Math.max(0.1, eruptionSegmentSpacing);
		}
	}

	public static final class FrostSlamConfig {
		public double radius = 0.0;
		public int freezeDurationTicks = 0;
		public float trueDamage = 0.0F;
		public double manaCostPercent = 0.0;
		public double setbackChancePercent = 0.0;
		public double setbackProgressPercent = 0.0;
		public int particleCount = 18;
		public double groundRingRadius = 0.0;
		public int hazeDensity = 12;
		public double shardVelocity = 0.24;

		void normalize() {
			radius = Math.max(0.0, radius);
			freezeDurationTicks = Math.max(0, freezeDurationTicks);
			trueDamage = Math.max(0.0F, trueDamage);
			manaCostPercent = MathHelper.clamp(manaCostPercent, 0.0, 100.0);
			setbackChancePercent = MathHelper.clamp(setbackChancePercent, 0.0, 100.0);
			setbackProgressPercent = MathHelper.clamp(setbackProgressPercent, 0.0, 100.0);
			particleCount = Math.max(0, particleCount);
			groundRingRadius = Math.max(0.0, groundRingRadius);
			hazeDensity = Math.max(0, hazeDensity);
			shardVelocity = Math.max(0.0, shardVelocity);
		}
	}

	public static final class FrostDebuffConfig {
		public int intervalTicks = 20;
		public float baseDamagePerTick = 2.0F;
		public boolean refreshDurationOnReapply = true;
		public boolean stackDamageOnReapply = false;
		public boolean damageOnInitialApplication = true;
		public boolean bypassTotems = false;
		public int visualFrozenTicks = 180;

		void normalize() {
			intervalTicks = Math.max(1, intervalTicks);
			baseDamagePerTick = Math.max(0.0F, baseDamagePerTick);
			visualFrozenTicks = Math.max(0, visualFrozenTicks);
		}
	}

	public static final class FrostMaximumConfig {
		public boolean enabled = true;
		public int windupDurationTicks = 10 * 20;
		public double fearRadiusMultiplierFromDomainRadius = 1.0;
		public double burstRadiusMultiplierFromDomainRadius = 2.0;
		public double floatHeightBlocks = 5.0;
		public int packedIceDurationTicks = 3 * 20;
		public int suffocationDamageIntervalTicks = 20;
		public float suffocationDamagePerInterval = 4.0F;
		public int postBurstFreezeDurationTicks = 30 * 20;
		public String windupMagicBlockedMessage = "The White Haze forming freezes you in fear";
		public String postBurstMagicBlockedMessage = "You are so cold a white haze clouds your mind";
		public int cooldownTicks = 30 * 60 * 20;
		public int manaRegenBlockedTicks = 30 * 60 * 20;
		public int whiteParticleCount = 18;
		public boolean ascentVortexEnabled = true;
		public double vortexHeightBlocks = 5.5;
		public double vortexBaseRadius = 0.4;
		public double vortexTopRadius = 2.25;
		public int vortexRingCount = 6;
		public int burstParticleCount = 140;
		public double burstShardSpeed = 0.42;
		public int burstHazeDensity = 96;
		public double groundShockRingRadius = 12.0;

		void normalize() {
			windupDurationTicks = Math.max(0, windupDurationTicks);
			fearRadiusMultiplierFromDomainRadius = Math.max(0.0, fearRadiusMultiplierFromDomainRadius);
			burstRadiusMultiplierFromDomainRadius = Math.max(0.0, burstRadiusMultiplierFromDomainRadius);
			floatHeightBlocks = Math.max(0.0, floatHeightBlocks);
			packedIceDurationTicks = Math.max(0, packedIceDurationTicks);
			suffocationDamageIntervalTicks = Math.max(1, suffocationDamageIntervalTicks);
			suffocationDamagePerInterval = Math.max(0.0F, suffocationDamagePerInterval);
			postBurstFreezeDurationTicks = Math.max(0, postBurstFreezeDurationTicks);
			if (windupMagicBlockedMessage == null) {
				windupMagicBlockedMessage = "";
			}
			if (postBurstMagicBlockedMessage == null) {
				postBurstMagicBlockedMessage = "";
			}
			cooldownTicks = Math.max(0, cooldownTicks);
			manaRegenBlockedTicks = Math.max(0, manaRegenBlockedTicks);
			whiteParticleCount = Math.max(0, whiteParticleCount);
			vortexHeightBlocks = Math.max(0.1, vortexHeightBlocks);
			vortexBaseRadius = Math.max(0.0, vortexBaseRadius);
			vortexTopRadius = Math.max(vortexBaseRadius, vortexTopRadius);
			vortexRingCount = Math.max(1, vortexRingCount);
			burstParticleCount = Math.max(0, burstParticleCount);
			burstShardSpeed = Math.max(0.0, burstShardSpeed);
			burstHazeDensity = Math.max(0, burstHazeDensity);
			groundShockRingRadius = Math.max(0.0, groundShockRingRadius);
		}
	}

	public static final class FrostDomainConfig {
		public int radius = 25;
		public int height = 25;
		public int shellThickness = 1;
		public int cooldownTicks = 30 * 60 * 20;
		public int startupTicks = 0;
		public int pulseIntervalTicks = 6 * 20;
		public int pulseDurationTicks = 6 * 20;
		public int blindnessPulseDurationTicks = 20;
		public int freezePulseDurationTicks = 20;
		public int totalDurationTicks = 60 * 20;
		public int finalExecutionDelayTicks = 0;
		public boolean applySlowness = true;
		public int slownessBaseAmplifier = 0;
		public int slownessAmplifierPerPulse = 1;
		public int casterResistanceAmplifier = 2;
		public int casterSlownessAmplifier = 2;
		public boolean applyFrost = true;
		public float frostDamagePerTick = 2.0F;
		public boolean applyBlindness = true;
		public boolean applyFreeze = true;
		public boolean finalExecutionEnabled = true;
		public boolean finalExecutionBypassTotems = true;
		public int executionParticleCount = 32;
		public float executionSoundVolume = 1.0F;
		public float executionSoundPitch = 0.9F;

		void normalize() {
			radius = Math.max(1, radius);
			height = Math.max(1, height);
			shellThickness = Math.max(1, shellThickness);
			cooldownTicks = Math.max(0, cooldownTicks);
			startupTicks = Math.max(0, startupTicks);
			pulseIntervalTicks = Math.max(1, pulseIntervalTicks);
			pulseDurationTicks = Math.max(0, pulseDurationTicks);
			blindnessPulseDurationTicks = Math.max(0, blindnessPulseDurationTicks);
			freezePulseDurationTicks = Math.max(0, freezePulseDurationTicks);
			totalDurationTicks = Math.max(1, totalDurationTicks);
			finalExecutionDelayTicks = Math.max(0, finalExecutionDelayTicks);
			slownessBaseAmplifier = Math.max(-1, slownessBaseAmplifier);
			slownessAmplifierPerPulse = Math.max(0, slownessAmplifierPerPulse);
			casterResistanceAmplifier = Math.max(-1, casterResistanceAmplifier);
			casterSlownessAmplifier = Math.max(-1, casterSlownessAmplifier);
			frostDamagePerTick = Math.max(0.0F, frostDamagePerTick);
			executionParticleCount = Math.max(0, executionParticleCount);
			executionSoundVolume = Math.max(0.0F, executionSoundVolume);
			executionSoundPitch = Math.max(0.0F, executionSoundPitch);
		}
	}

	public static final class DomainClashConfig {
		public boolean enabled = true;
		public int simultaneousCastWindowTicks = 1;
		public int minimumExteriorDistance = 20;
		public int titleFadeInTicks = 8;
		public int titleStayTicks = 44;
		public int titleFadeOutTicks = 8;
		public int instructionsDurationTicks = 300;
		public int instructionsFadeOutTicks = 20;
		public double damageToWin = 250.0;
		public int loserManaDrainPercent = 50;
		@SerializedName(value = "postClashDomainCooldownMultiplier", alternate = { "loserCooldownMultiplier" })
		public double postClashDomainCooldownMultiplier = 0.5;
		public int particlesPerTick = 120;
		public int splitPatternModulo = 2;
		public boolean disableDomainEffectsDuringClash = true;
		public boolean forceLookAtOpponent = true;
		public boolean participantsInvincible = false;
		public boolean pauseTimedDomainCollapseTimers = true;
		public boolean pauseAstralCataclysmPhaseTimers = true;

		void normalize() {
			simultaneousCastWindowTicks = Math.max(0, simultaneousCastWindowTicks);
			minimumExteriorDistance = Math.max(0, minimumExteriorDistance);
			titleFadeInTicks = Math.max(0, titleFadeInTicks);
			titleStayTicks = Math.max(0, titleStayTicks);
			titleFadeOutTicks = Math.max(0, titleFadeOutTicks);
			instructionsDurationTicks = Math.max(0, instructionsDurationTicks);
			instructionsFadeOutTicks = Math.max(0, instructionsFadeOutTicks);
			damageToWin = Math.max(1.0, damageToWin);
			loserManaDrainPercent = Math.max(0, Math.min(100, loserManaDrainPercent));
			postClashDomainCooldownMultiplier = Math.max(0.0, Math.min(10.0, postClashDomainCooldownMultiplier));
			particlesPerTick = Math.max(0, particlesPerTick);
			splitPatternModulo = Math.max(2, splitPatternModulo);
		}
	}

	public static final class AbilityAccessConfig {
		public boolean enabled = true;
		public List<String> defaultLockedAbilities = new ArrayList<>();
		public Map<String, List<String>> lockedAbilitiesByPlayer = new HashMap<>();
		public Map<String, List<String>> unlockedAbilitiesByPlayer = new HashMap<>();
		public Map<String, List<String>> lockedStageProgressionByPlayer = new HashMap<>();
		public Map<String, List<String>> unlockedStageProgressionByPlayer = new HashMap<>();

		private transient Set<MagicAbility> resolvedDefaultLockedAbilities = Set.of();
		private transient Map<UUID, Set<MagicAbility>> resolvedLockedAbilitiesByPlayer = Map.of();
		private transient Map<UUID, Set<MagicAbility>> resolvedUnlockedAbilitiesByPlayer = Map.of();
		private transient Map<UUID, Set<StageProgressionLock>> resolvedLockedStageProgressionByPlayer = Map.of();
		private transient Map<UUID, Set<StageProgressionLock>> resolvedUnlockedStageProgressionByPlayer = Map.of();

		void normalize() {
			if (defaultLockedAbilities == null) {
				defaultLockedAbilities = new ArrayList<>();
			}
			if (lockedAbilitiesByPlayer == null) {
				lockedAbilitiesByPlayer = new HashMap<>();
			}
			if (unlockedAbilitiesByPlayer == null) {
				unlockedAbilitiesByPlayer = new HashMap<>();
			}
			if (lockedStageProgressionByPlayer == null) {
				lockedStageProgressionByPlayer = new HashMap<>();
			}
			if (unlockedStageProgressionByPlayer == null) {
				unlockedStageProgressionByPlayer = new HashMap<>();
			}

			resolvedDefaultLockedAbilities = parseAbilityIds(defaultLockedAbilities, "abilityAccess.defaultLockedAbilities");
			resolvedLockedAbilitiesByPlayer = parsePlayerAbilityIds(lockedAbilitiesByPlayer, "abilityAccess.lockedAbilitiesByPlayer");
			resolvedUnlockedAbilitiesByPlayer = parsePlayerAbilityIds(unlockedAbilitiesByPlayer, "abilityAccess.unlockedAbilitiesByPlayer");
			resolvedLockedStageProgressionByPlayer = parsePlayerStageProgressionIds(
				lockedStageProgressionByPlayer,
				"abilityAccess.lockedStageProgressionByPlayer"
			);
			resolvedUnlockedStageProgressionByPlayer = parsePlayerStageProgressionIds(
				unlockedStageProgressionByPlayer,
				"abilityAccess.unlockedStageProgressionByPlayer"
			);
		}

		boolean setPlayerAbilityLocked(UUID playerId, MagicAbility ability, boolean locked) {
			String playerKey = playerId.toString();
			if (locked) {
				boolean changed = addAbilityOverride(lockedAbilitiesByPlayer, playerKey, ability.id());
				changed |= removeAbilityOverride(unlockedAbilitiesByPlayer, playerKey, ability.id());
				return changed;
			}

			boolean changed = addAbilityOverride(unlockedAbilitiesByPlayer, playerKey, ability.id());
			changed |= removeAbilityOverride(lockedAbilitiesByPlayer, playerKey, ability.id());
			return changed;
		}

		boolean clearPlayerOverrides(UUID playerId) {
			String playerKey = playerId.toString();
			boolean changed = lockedAbilitiesByPlayer.remove(playerKey) != null;
			changed |= unlockedAbilitiesByPlayer.remove(playerKey) != null;
			changed |= lockedStageProgressionByPlayer.remove(playerKey) != null;
			changed |= unlockedStageProgressionByPlayer.remove(playerKey) != null;
			return changed;
		}

		boolean setPlayerStageProgressionLocked(UUID playerId, MagicSchool school, int stage, boolean locked) {
			if (!isStageProgressionStageSupported(school, stage)) {
				return false;
			}

			String playerKey = playerId.toString();
			String stageId = stageProgressionId(school, stage);
			if (locked) {
				boolean changed = addAbilityOverride(lockedStageProgressionByPlayer, playerKey, stageId);
				changed |= removeAbilityOverride(unlockedStageProgressionByPlayer, playerKey, stageId);
				return changed;
			}

			boolean changed = addAbilityOverride(unlockedStageProgressionByPlayer, playerKey, stageId);
			changed |= removeAbilityOverride(lockedStageProgressionByPlayer, playerKey, stageId);
			return changed;
		}

		public boolean isAbilityUnlocked(UUID playerId, MagicAbility ability) {
			if (!enabled || ability == MagicAbility.NONE || !ability.school().isMagic()) {
				return true;
			}

			EnumSet<MagicAbility> unlockedForSchool = EnumSet.noneOf(MagicAbility.class);
			for (MagicAbility candidate : MagicAbility.values()) {
				if (candidate != MagicAbility.NONE && candidate.school() == ability.school()) {
					unlockedForSchool.add(candidate);
				}
			}

			unlockedForSchool.removeAll(resolvedDefaultLockedAbilities);

			Set<MagicAbility> playerLocked = resolvedLockedAbilitiesByPlayer.get(playerId);
			if (playerLocked != null) {
				unlockedForSchool.removeAll(playerLocked);
			}

			Set<MagicAbility> playerUnlocked = resolvedUnlockedAbilitiesByPlayer.get(playerId);
			if (playerUnlocked != null) {
				for (MagicAbility candidate : playerUnlocked) {
					if (candidate.school() == ability.school()) {
						unlockedForSchool.add(candidate);
					}
				}
			}

			return unlockedForSchool.contains(ability);
		}

		public boolean isStageProgressionUnlocked(UUID playerId, MagicSchool school, int stage) {
			if (!enabled || !isStageProgressionStageSupported(school, stage)) {
				return true;
			}

			StageProgressionLock requestedLock = new StageProgressionLock(school, stage);
			boolean unlocked = true;
			Set<StageProgressionLock> playerLocked = resolvedLockedStageProgressionByPlayer.get(playerId);
			if (playerLocked != null && playerLocked.contains(requestedLock)) {
				unlocked = false;
			}

			Set<StageProgressionLock> playerUnlocked = resolvedUnlockedStageProgressionByPlayer.get(playerId);
			if (playerUnlocked != null && playerUnlocked.contains(requestedLock)) {
				unlocked = true;
			}

			return unlocked;
		}

		private static boolean addAbilityOverride(Map<String, List<String>> overrides, String playerKey, String abilityId) {
			List<String> abilities = overrides.computeIfAbsent(playerKey, ignored -> new ArrayList<>());
			for (String existing : abilities) {
				if (abilityId.equalsIgnoreCase(existing)) {
					return false;
				}
			}

			abilities.add(abilityId);
			return true;
		}

		private static boolean removeAbilityOverride(Map<String, List<String>> overrides, String playerKey, String abilityId) {
			List<String> abilities = overrides.get(playerKey);
			if (abilities == null) {
				return false;
			}

			boolean removed = abilities.removeIf(existing -> abilityId.equalsIgnoreCase(existing));
			if (abilities.isEmpty()) {
				overrides.remove(playerKey);
			}
			return removed;
		}
	}
}

