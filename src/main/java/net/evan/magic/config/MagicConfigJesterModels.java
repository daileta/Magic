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

abstract class MagicConfigJesterModels extends MagicConfigBurningPassionModels {
	public static final class JesterSpotlightConfig {
		public double detectionRange = 48.0;
		public int effectRefreshTicks = 10;
		public JesterSpotlightStageConfig stageOne = defaultStageOne();
		public JesterSpotlightStageConfig stageTwo = defaultStageTwo();
		public JesterSpotlightStageConfig stageThree = defaultStageThree();

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

			stageOne.normalize();
			stageTwo.normalize();
			stageThree.normalize();
		}

		private static JesterSpotlightStageConfig defaultStageOne() {
			JesterSpotlightStageConfig config = new JesterSpotlightStageConfig();
			config.viewersRequired = 2;
			config.activationWindowTicks = 200;
			config.downgradeGraceTicks = 100;
			config.fallbackWindowTicks = 200;
			config.attackDamageBonus = 2.0;
			config.movementSpeedMultiplier = 0.08;
			config.scale = 1.15;
			config.jumpBoostAmplifier = 0;
			config.resistanceAmplifier = 0;
			config.maxHealthBonusHearts = 2.0;
			return config;
		}

		private static JesterSpotlightStageConfig defaultStageTwo() {
			JesterSpotlightStageConfig config = new JesterSpotlightStageConfig();
			config.viewersRequired = 5;
			config.activationWindowTicks = 100;
			config.downgradeGraceTicks = 100;
			config.fallbackWindowTicks = 200;
			config.attackDamageBonus = 4.0;
			config.movementSpeedMultiplier = 0.16;
			config.scale = 1.3;
			config.jumpBoostAmplifier = 1;
			config.resistanceAmplifier = 1;
			config.maxHealthBonusHearts = 5.0;
			return config;
		}

		private static JesterSpotlightStageConfig defaultStageThree() {
			JesterSpotlightStageConfig config = new JesterSpotlightStageConfig();
			config.viewersRequired = 8;
			config.activationWindowTicks = 100;
			config.downgradeGraceTicks = 100;
			config.fallbackWindowTicks = 200;
			config.attackDamageBonus = 6.0;
			config.movementSpeedMultiplier = 0.24;
			config.scale = 1.5;
			config.jumpBoostAmplifier = 2;
			config.resistanceAmplifier = 2;
			config.maxHealthBonusHearts = 10.0;
			return config;
		}
	}

	public static final class JesterSpotlightStageConfig {
		public int viewersRequired = 1;
		public int activationWindowTicks = 100;
		public int downgradeGraceTicks = 100;
		public int fallbackWindowTicks = 200;
		public double attackDamageBonus = 0.0;
		public double movementSpeedMultiplier = 0.0;
		public double scale = 1.0;
		public int jumpBoostAmplifier = -1;
		public int resistanceAmplifier = -1;
		public double maxHealthBonusHearts = 0.0;

		void normalize() {
			viewersRequired = Math.max(1, viewersRequired);
			activationWindowTicks = Math.max(1, activationWindowTicks);
			downgradeGraceTicks = Math.max(0, downgradeGraceTicks);
			fallbackWindowTicks = Math.max(1, fallbackWindowTicks);
			attackDamageBonus = Math.max(0.0, attackDamageBonus);
			movementSpeedMultiplier = Math.max(0.0, movementSpeedMultiplier);
			scale = Math.max(1.0, scale);
			jumpBoostAmplifier = Math.max(-1, jumpBoostAmplifier);
			resistanceAmplifier = Math.max(-1, resistanceAmplifier);
			maxHealthBonusHearts = Math.max(0.0, maxHealthBonusHearts);
		}
	}

	public static final class JesterWittyOneLinerConfig {
		public int activationCostPercent = 50;
		public int targetRange = 32;
		public int overlayFadeInTicks = 4;
		public int overlayStayTicks = 50;
		public int overlayFadeOutTicks = 10;
		public JesterWittyOneLinerTierConfig lowTier = defaultLowTier();
		public JesterWittyOneLinerTierConfig midTier = defaultMidTier();
		public JesterWittyOneLinerTierConfig highTier = defaultHighTier();

		void normalize() {
			if (lowTier == null) {
				lowTier = defaultLowTier();
			}
			if (midTier == null) {
				midTier = defaultMidTier();
			}
			if (highTier == null) {
				highTier = defaultHighTier();
			}

			activationCostPercent = Math.max(0, activationCostPercent);
			targetRange = Math.max(1, targetRange);
			overlayFadeInTicks = Math.max(0, overlayFadeInTicks);
			overlayStayTicks = Math.max(0, overlayStayTicks);
			overlayFadeOutTicks = Math.max(0, overlayFadeOutTicks);
			lowTier.normalize(defaultLowTier());
			midTier.normalize(defaultMidTier());
			highTier.normalize(defaultHighTier());
		}

		private static JesterWittyOneLinerTierConfig defaultLowTier() {
			JesterWittyOneLinerTierConfig config = new JesterWittyOneLinerTierConfig();
			config.selectionWeight = 70;
			config.cooldownTicks = 600;
			config.effectDurationTicks = 600;
			config.textColorHex = "#FFFFFF";
			config.slownessAmplifier = 0;
			config.weaknessAmplifier = 0;
			config.jokes = new ArrayList<>(List.of(
				"I'd tell a sharper joke, but safety scissors won.",
				"That was my warm-up heckle.",
				"I clown around professionally.",
				"I brought jokes and poor decisions.",
				"You're lucky that was the discount punchline."
			));
			return config;
		}

		private static JesterWittyOneLinerTierConfig defaultMidTier() {
			JesterWittyOneLinerTierConfig config = new JesterWittyOneLinerTierConfig();
			config.selectionWeight = 20;
			config.cooldownTicks = 900;
			config.effectDurationTicks = 900;
			config.textColorHex = "#FFD54A";
			config.slownessAmplifier = 2;
			config.weaknessAmplifier = 1;
			config.nauseaAmplifier = 1;
			config.jokes = new ArrayList<>(List.of(
				"You just got roasted by a part-time fool.",
				"I've seen mannequins dodge better.",
				"That glare says the joke landed harder than you did.",
				"I'm billing you for emotional damage and stage lights.",
				"Even my backup punchline hits harder than that."
			));
			return config;
		}

		private static JesterWittyOneLinerTierConfig defaultHighTier() {
			JesterWittyOneLinerTierConfig config = new JesterWittyOneLinerTierConfig();
			config.selectionWeight = 10;
			config.cooldownTicks = 1200;
			config.effectDurationTicks = 1200;
			config.textColorHex = "#39B7FF";
			config.slownessAmplifier = 4;
			config.weaknessAmplifier = 4;
			config.blindnessAmplifier = 1;
			config.nauseaAmplifier = 1;
			config.darknessAmplifier = 0;
			config.weavingAmplifier = 0;
			config.jokes = new ArrayList<>(List.of(
				"The crowd gasped, the lights hit, and your confidence left the building.",
				"I didn't steal the show. You dropped it.",
				"That silence is your reputation filing for leave.",
				"You're not bombed by the joke. You're bombed by the timing.",
				"Smile for the spotlight. This is the part where you lose gracefully."
			));
			return config;
		}
	}

	public static final class JesterWittyOneLinerTierConfig {
		public int selectionWeight = 1;
		public int cooldownTicks = 0;
		public int effectDurationTicks = 0;
		public String textColorHex = "#FFFFFF";
		public int slownessAmplifier = -1;
		public int weaknessAmplifier = -1;
		public int blindnessAmplifier = -1;
		public int nauseaAmplifier = -1;
		public int darknessAmplifier = -1;
		public int weavingAmplifier = -1;
		public boolean applyGlowing = true;
		public List<String> jokes = new ArrayList<>();

		void normalize(JesterWittyOneLinerTierConfig defaults) {
			selectionWeight = Math.max(0, selectionWeight);
			cooldownTicks = Math.max(0, cooldownTicks);
			effectDurationTicks = Math.max(0, effectDurationTicks);
			slownessAmplifier = Math.max(-1, slownessAmplifier);
			weaknessAmplifier = Math.max(-1, weaknessAmplifier);
			blindnessAmplifier = Math.max(-1, blindnessAmplifier);
			nauseaAmplifier = Math.max(-1, nauseaAmplifier);
			darknessAmplifier = Math.max(-1, darknessAmplifier);
			weavingAmplifier = Math.max(-1, weavingAmplifier);
			if (textColorHex == null || textColorHex.isBlank()) {
				textColorHex = defaults.textColorHex;
			}
			if (jokes == null) {
				jokes = new ArrayList<>(defaults.jokes);
			}
			jokes.removeIf(joke -> joke == null || joke.isBlank());
			if (jokes.isEmpty()) {
				jokes = new ArrayList<>(defaults.jokes);
			}
		}
	}

	public static final class JesterComedicRewriteConfig {
		public double baseProcChancePercent = 50.0;
		public double severityProcBonusPercent = 0.0;
		public double lethalProcBonusPercent = 0.0;
		public double maxProcChancePercent = 50.0;
		public float dangerousDamageThreshold = 8.0F;
		public double dangerousHealthFractionThreshold = 0.45;
		public float severityDamageCap = 20.0F;
		public double manaCostPercent = 20.0;
		public int cooldownTicks = 25 * 20;
		public int postRewriteImmunityTicks = 20;
		public int postRewriteFallProtectionTicks = 80;
		public boolean extinguishOnRewrite = true;
		public double minSavedHealthHearts = 1.0;
		public double maxSavedHealthHearts = 4.0;
		public int safeSearchRadius = 14;
		public int safeSearchVerticalRange = 10;
		public int unsafeYBufferBlocks = 16;
		public JesterComedicRewriteLaunchConfig launchedThroughTheScene = defaultLaunchedThroughTheScene();
		public JesterComedicRewriteRavagerConfig ravagerBit = defaultRavagerBit();
		public JesterComedicRewriteParrotConfig parrotRescue = defaultParrotRescue();

		void normalize() {
			if (launchedThroughTheScene == null) {
				launchedThroughTheScene = defaultLaunchedThroughTheScene();
			}
			if (ravagerBit == null) {
				ravagerBit = defaultRavagerBit();
			}
			if (parrotRescue == null) {
				parrotRescue = defaultParrotRescue();
			}

			baseProcChancePercent = MathHelper.clamp(baseProcChancePercent, 0.0, 100.0);
			severityProcBonusPercent = MathHelper.clamp(severityProcBonusPercent, 0.0, 100.0);
			lethalProcBonusPercent = MathHelper.clamp(lethalProcBonusPercent, 0.0, 100.0);
			maxProcChancePercent = MathHelper.clamp(maxProcChancePercent, 0.0, 100.0);
			dangerousDamageThreshold = Math.max(0.0F, dangerousDamageThreshold);
			dangerousHealthFractionThreshold = MathHelper.clamp(dangerousHealthFractionThreshold, 0.0, 1.0);
			severityDamageCap = Math.max(dangerousDamageThreshold, severityDamageCap);
			manaCostPercent = MathHelper.clamp(manaCostPercent, 0.0, 100.0);
			cooldownTicks = Math.max(0, cooldownTicks);
			postRewriteImmunityTicks = Math.max(0, postRewriteImmunityTicks);
			postRewriteFallProtectionTicks = Math.max(0, postRewriteFallProtectionTicks);
			minSavedHealthHearts = Math.max(0.5, minSavedHealthHearts);
			maxSavedHealthHearts = Math.max(minSavedHealthHearts, maxSavedHealthHearts);
			safeSearchRadius = Math.max(1, safeSearchRadius);
			safeSearchVerticalRange = Math.max(1, safeSearchVerticalRange);
			unsafeYBufferBlocks = Math.max(0, unsafeYBufferBlocks);
			launchedThroughTheScene.normalize();
			ravagerBit.normalize();
			parrotRescue.normalize();
		}

		private static JesterComedicRewriteLaunchConfig defaultLaunchedThroughTheScene() {
			JesterComedicRewriteLaunchConfig config = new JesterComedicRewriteLaunchConfig();
			config.weight = 50;
			config.baseHorizontalVelocity = 1.6;
			config.horizontalVelocityPerSeverity = 1.2;
			config.baseVerticalVelocity = 0.6;
			config.verticalVelocityPerSeverity = 0.35;
			config.slownessDurationTicks = 40;
			config.slownessAmplifier = 2;
			config.smokeParticleCount = 18;
			config.poofParticleCount = 8;
			return config;
		}

		private static JesterComedicRewriteRavagerConfig defaultRavagerBit() {
			JesterComedicRewriteRavagerConfig config = new JesterComedicRewriteRavagerConfig();
			config.weight = 25;
			config.baseHorizontalVelocity = 1.35;
			config.horizontalVelocityPerSeverity = 1.05;
			config.baseVerticalVelocity = 0.5;
			config.verticalVelocityPerSeverity = 0.25;
			config.slownessDurationTicks = 60;
			config.slownessAmplifier = 3;
			config.smokeParticleCount = 16;
			config.dustParticleCount = 20;
			config.showVisualCameo = true;
			config.visualDurationTicks = 38;
			config.visualSpawnDistance = 2.0;
			config.visualVerticalOffset = 0.0;
			config.visualChargeVelocity = 2.0;
			config.visualChargeVelocityBuffer = 0.45;
			config.visualChargeDurationTicks = 6;
			return config;
		}

		private static JesterComedicRewriteParrotConfig defaultParrotRescue() {
			JesterComedicRewriteParrotConfig config = new JesterComedicRewriteParrotConfig();
			config.weight = 25;
			config.carryHeight = 5.0;
			config.carryHeightPerSeverity = 3.0;
			config.sideVelocity = 0.35;
			config.sideVelocityPerSeverity = 0.2;
			config.slowFallingDurationTicks = 60;
			config.levitationDurationTicks = 12;
			config.featherParticleCount = 18;
			config.showVisualCameo = true;
			config.visualDurationTicks = 32;
			config.visualCount = 3;
			config.visualRadius = 0.9;
			config.visualVerticalOffset = 0.2;
			config.visualFollowPlayerHead = true;
			config.visualLiftVelocity = 0.15;
			return config;
		}
	}

	public static final class JesterComedicRewriteLaunchConfig {
		public int weight = 1;
		public double baseHorizontalVelocity = 1.0;
		public double horizontalVelocityPerSeverity = 0.0;
		public double baseVerticalVelocity = 0.4;
		public double verticalVelocityPerSeverity = 0.0;
		public int slownessDurationTicks = 20;
		public int slownessAmplifier = 0;
		public int smokeParticleCount = 0;
		public int poofParticleCount = 0;

		void normalize() {
			weight = Math.max(0, weight);
			baseHorizontalVelocity = Math.max(0.0, baseHorizontalVelocity);
			horizontalVelocityPerSeverity = Math.max(0.0, horizontalVelocityPerSeverity);
			baseVerticalVelocity = Math.max(0.0, baseVerticalVelocity);
			verticalVelocityPerSeverity = Math.max(0.0, verticalVelocityPerSeverity);
			slownessDurationTicks = Math.max(0, slownessDurationTicks);
			slownessAmplifier = Math.max(-1, slownessAmplifier);
			smokeParticleCount = Math.max(0, smokeParticleCount);
			poofParticleCount = Math.max(0, poofParticleCount);
		}
	}

	public static final class JesterComedicRewriteRavagerConfig {
		public int weight = 1;
		public double baseHorizontalVelocity = 1.0;
		public double horizontalVelocityPerSeverity = 0.0;
		public double baseVerticalVelocity = 0.4;
		public double verticalVelocityPerSeverity = 0.0;
		public int slownessDurationTicks = 20;
		public int slownessAmplifier = 0;
		public int smokeParticleCount = 0;
		public int dustParticleCount = 0;
		public boolean showVisualCameo = true;
		public int visualDurationTicks = 0;
		public double visualSpawnDistance = 0.0;
		public double visualVerticalOffset = 0.0;
		public double visualChargeVelocity = 0.0;
		public double visualChargeVelocityBuffer = 0.0;
		public int visualChargeDurationTicks = 0;

		void normalize() {
			weight = Math.max(0, weight);
			baseHorizontalVelocity = Math.max(0.0, baseHorizontalVelocity);
			horizontalVelocityPerSeverity = Math.max(0.0, horizontalVelocityPerSeverity);
			baseVerticalVelocity = Math.max(0.0, baseVerticalVelocity);
			verticalVelocityPerSeverity = Math.max(0.0, verticalVelocityPerSeverity);
			slownessDurationTicks = Math.max(0, slownessDurationTicks);
			slownessAmplifier = Math.max(-1, slownessAmplifier);
			smokeParticleCount = Math.max(0, smokeParticleCount);
			dustParticleCount = Math.max(0, dustParticleCount);
			visualDurationTicks = Math.max(0, visualDurationTicks);
			visualSpawnDistance = Math.max(0.0, visualSpawnDistance);
			visualChargeVelocity = Math.max(0.0, visualChargeVelocity);
			visualChargeVelocityBuffer = Math.max(0.0, visualChargeVelocityBuffer);
			visualChargeDurationTicks = Math.max(0, visualChargeDurationTicks);
		}
	}

	public static final class JesterComedicRewriteParrotConfig {
		public int weight = 1;
		public double carryHeight = 4.0;
		public double carryHeightPerSeverity = 0.0;
		public double sideVelocity = 0.2;
		public double sideVelocityPerSeverity = 0.0;
		public int slowFallingDurationTicks = 40;
		public int levitationDurationTicks = 0;
		public int featherParticleCount = 0;
		public boolean showVisualCameo = true;
		public int visualDurationTicks = 0;
		public int visualCount = 0;
		public double visualRadius = 0.0;
		public double visualVerticalOffset = 0.0;
		public boolean visualFollowPlayerHead = true;
		public double visualLiftVelocity = 0.0;

		void normalize() {
			weight = Math.max(0, weight);
			carryHeight = Math.max(0.0, carryHeight);
			carryHeightPerSeverity = Math.max(0.0, carryHeightPerSeverity);
			sideVelocity = Math.max(0.0, sideVelocity);
			sideVelocityPerSeverity = Math.max(0.0, sideVelocityPerSeverity);
			slowFallingDurationTicks = Math.max(0, slowFallingDurationTicks);
			levitationDurationTicks = Math.max(0, levitationDurationTicks);
			featherParticleCount = Math.max(0, featherParticleCount);
			visualDurationTicks = Math.max(0, visualDurationTicks);
			visualCount = Math.max(0, visualCount);
			visualRadius = Math.max(0.0, visualRadius);
			visualLiftVelocity = Math.max(0.0, visualLiftVelocity);
		}
	}

	public static final class JesterComedicAssistantConfig {
		public double activationCostPercent = 25.0;
		public int armedDurationTicks = 15 * 20;
		public int procCooldownTicks = 15 * 20;
		public int cancelCooldownTicks = 3 * 20;
		public boolean allowPlayerTargets = true;
		public boolean allowMobTargets = true;
		public int armedIndicatorRefreshTicks = 20;
		public int armedParticleCount = 4;
		public int overlayFadeInTicks = 4;
		public int overlayStayTicks = 30;
		public int overlayFadeOutTicks = 8;
		public JesterComedicAssistantSlimeConfig giantSlimeSlam = defaultGiantSlimeSlam();
		public JesterComedicAssistantPandaConfig pandaBowlingBall = defaultPandaBowlingBall();
		public JesterComedicAssistantParrotConfig parrotKidnapping = defaultParrotKidnapping();
		public JesterComedicAssistantDivineConfig divineOverreaction = defaultDivineOverreaction();
		public JesterComedicAssistantAcmeConfig acmeDrop = defaultAcmeDrop();
		public JesterComedicAssistantPieConfig pieToTheFace = defaultPieToTheFace();
		public JesterComedicAssistantCaneConfig giantCaneYank = defaultGiantCaneYank();

		void normalize() {
			if (giantSlimeSlam == null) {
				giantSlimeSlam = defaultGiantSlimeSlam();
			}
			if (pandaBowlingBall == null) {
				pandaBowlingBall = defaultPandaBowlingBall();
			}
			if (parrotKidnapping == null) {
				parrotKidnapping = defaultParrotKidnapping();
			}
			if (divineOverreaction == null) {
				divineOverreaction = defaultDivineOverreaction();
			}
			if (acmeDrop == null) {
				acmeDrop = defaultAcmeDrop();
			}
			if (pieToTheFace == null) {
				pieToTheFace = defaultPieToTheFace();
			}
			if (giantCaneYank == null) {
				giantCaneYank = defaultGiantCaneYank();
			}

			activationCostPercent = MathHelper.clamp(activationCostPercent, 0.0, 100.0);
			armedDurationTicks = Math.max(1, armedDurationTicks);
			procCooldownTicks = Math.max(0, procCooldownTicks);
			cancelCooldownTicks = Math.max(0, cancelCooldownTicks);
			armedIndicatorRefreshTicks = Math.max(1, armedIndicatorRefreshTicks);
			armedParticleCount = Math.max(0, armedParticleCount);
			overlayFadeInTicks = Math.max(0, overlayFadeInTicks);
			overlayStayTicks = Math.max(0, overlayStayTicks);
			overlayFadeOutTicks = Math.max(0, overlayFadeOutTicks);
			giantSlimeSlam.normalize();
			pandaBowlingBall.normalize();
			parrotKidnapping.normalize();
			divineOverreaction.normalize();
			acmeDrop.normalize();
			pieToTheFace.normalize();
			giantCaneYank.normalize();
		}

		private static JesterComedicAssistantSlimeConfig defaultGiantSlimeSlam() {
			JesterComedicAssistantSlimeConfig config = new JesterComedicAssistantSlimeConfig();
			config.enabled = true;
			config.weight = 1;
			config.bonusDamage = 4.0F;
			config.slownessDurationTicks = 60;
			config.slownessAmplifier = 1;
			config.oozingDurationTicks = 30 * 20;
			config.oozingAmplifier = 0;
			config.visualDurationTicks = 30;
			config.visualSpawnHeight = 6.0;
			config.visualFallSpeed = 1.3;
			config.visualSize = 6;
			config.particleCount = 24;
			config.spawnSoundVolume = 0.85F;
			config.spawnSoundPitch = 0.8F;
			config.impactSoundVolume = 1.15F;
			config.impactSoundPitch = 0.85F;
			return config;
		}

		private static JesterComedicAssistantPandaConfig defaultPandaBowlingBall() {
			JesterComedicAssistantPandaConfig config = new JesterComedicAssistantPandaConfig();
			config.enabled = true;
			config.weight = 1;
			config.bonusDamage = 5.0F;
			config.horizontalLaunch = 3.3;
			config.verticalLaunch = 0.8;
			config.slownessDurationTicks = 30;
			config.slownessAmplifier = 0;
			config.visualDurationTicks = 18;
			config.visualSpawnDistance = 3.0;
			config.visualChargeVelocity = 2.6;
			config.particleCount = 18;
			config.rollSoundVolume = 1.0F;
			config.rollSoundPitch = 0.95F;
			config.impactSoundVolume = 1.0F;
			config.impactSoundPitch = 0.8F;
			return config;
		}

		private static JesterComedicAssistantParrotConfig defaultParrotKidnapping() {
			JesterComedicAssistantParrotConfig config = new JesterComedicAssistantParrotConfig();
			config.enabled = true;
			config.weight = 1;
			config.bonusDamage = 2.0F;
			config.liftHeight = 35.0;
			config.upwardVelocity = 1.55;
			config.maxCarryTicks = 30;
			config.releaseDownwardVelocity = 1.2;
			config.applyGlowing = true;
			config.glowingDurationTicks = 30;
			config.visualDurationTicks = 28;
			config.visualCount = 10;
			config.visualRadius = 1.35;
			config.visualVerticalOffset = 0.35;
			config.particleCount = 28;
			config.flapSoundIntervalTicks = 6;
			config.soundVolume = 0.8F;
			config.soundPitch = 1.25F;
			return config;
		}

		private static JesterComedicAssistantDivineConfig defaultDivineOverreaction() {
			JesterComedicAssistantDivineConfig config = new JesterComedicAssistantDivineConfig();
			config.enabled = true;
			config.weight = 1;
			config.bonusDamage = 10.0F;
			config.strikeCount = 20;
			config.strikeRadius = 1.5;
			config.glowingDurationTicks = 20;
			config.blindnessDurationTicks = 20;
			config.blindnessAmplifier = 0;
			config.nauseaDurationTicks = 40;
			config.nauseaAmplifier = 0;
			config.particleCount = 40;
			config.soundVolume = 0.8F;
			config.soundPitch = 1.0F;
			return config;
		}

		private static JesterComedicAssistantAcmeConfig defaultAcmeDrop() {
			JesterComedicAssistantAcmeConfig config = new JesterComedicAssistantAcmeConfig();
			config.enabled = true;
			config.weight = 1;
			config.bonusDamage = 6.0F;
			config.slownessDurationTicks = 30;
			config.slownessAmplifier = 5;
			config.weaknessDurationTicks = 60;
			config.weaknessAmplifier = 0;
			config.visualDurationTicks = 18;
			config.visualDropHeight = 10.0;
			config.particleCount = 20;
			config.soundVolume = 1.25F;
			config.soundPitch = 0.75F;
			return config;
		}

		private static JesterComedicAssistantPieConfig defaultPieToTheFace() {
			JesterComedicAssistantPieConfig config = new JesterComedicAssistantPieConfig();
			config.enabled = true;
			config.weight = 1;
			config.bonusDamage = 2.0F;
			config.blindnessDurationTicks = 5 * 20;
			config.blindnessAmplifier = 0;
			config.nauseaDurationTicks = 60;
			config.nauseaAmplifier = 0;
			config.particleCount = 24;
			config.soundVolume = 0.9F;
			config.soundPitch = 1.15F;
			return config;
		}

		private static JesterComedicAssistantCaneConfig defaultGiantCaneYank() {
			JesterComedicAssistantCaneConfig config = new JesterComedicAssistantCaneConfig();
			config.enabled = true;
			config.weight = 1;
			config.bonusDamage = 3.0F;
			config.horizontalLaunch = 15.2;
			config.verticalLaunch = 0.9;
			config.launchControlTicks = 6;
			config.velocityDamageTrackingTicks = 45;
			config.velocityDamageThreshold = 0.75;
			config.velocityDamageMultiplier = 3.5;
			config.velocityDamageMax = 24.0;
			config.slownessDurationTicks = 40;
			config.slownessAmplifier = 0;
			config.visualDurationTicks = 14;
			config.visualSpawnDistance = 4.75;
			config.visualChargeVelocity = 11.2;
			config.particleCount = 12;
			config.soundVolume = 0.95F;
			config.soundPitch = 0.95F;
			return config;
		}
	}

	public static final class JesterComedicAssistantSlimeConfig {
		public boolean enabled = true;
		public int weight = 1;
		public float bonusDamage = 0.0F;
		public int slownessDurationTicks = 0;
		public int slownessAmplifier = -1;
		public int oozingDurationTicks = 0;
		public int oozingAmplifier = -1;
		public int visualDurationTicks = 0;
		public double visualSpawnHeight = 0.0;
		public double visualFallSpeed = 0.0;
		public int visualSize = 1;
		public int particleCount = 0;
		public float spawnSoundVolume = 0.0F;
		public float spawnSoundPitch = 0.0F;
		public float impactSoundVolume = 0.0F;
		public float impactSoundPitch = 0.0F;

		void normalize() {
			weight = Math.max(0, weight);
			bonusDamage = Math.max(0.0F, bonusDamage);
			slownessDurationTicks = Math.max(0, slownessDurationTicks);
			slownessAmplifier = Math.max(-1, slownessAmplifier);
			oozingDurationTicks = Math.max(0, oozingDurationTicks);
			oozingAmplifier = Math.max(-1, oozingAmplifier);
			visualDurationTicks = Math.max(0, visualDurationTicks);
			visualSpawnHeight = Math.max(0.0, visualSpawnHeight);
			visualFallSpeed = Math.max(0.0, visualFallSpeed);
			visualSize = MathHelper.clamp(visualSize, 1, 127);
			particleCount = Math.max(0, particleCount);
			spawnSoundVolume = Math.max(0.0F, spawnSoundVolume);
			spawnSoundPitch = Math.max(0.0F, spawnSoundPitch);
			impactSoundVolume = Math.max(0.0F, impactSoundVolume);
			impactSoundPitch = Math.max(0.0F, impactSoundPitch);
		}
	}

	public static final class JesterComedicAssistantPandaConfig {
		public boolean enabled = true;
		public int weight = 1;
		public float bonusDamage = 0.0F;
		public double horizontalLaunch = 0.0;
		public double verticalLaunch = 0.0;
		public int slownessDurationTicks = 0;
		public int slownessAmplifier = -1;
		public int visualDurationTicks = 0;
		public double visualSpawnDistance = 0.0;
		public double visualChargeVelocity = 0.0;
		public int particleCount = 0;
		public float rollSoundVolume = 0.0F;
		public float rollSoundPitch = 0.0F;
		public float impactSoundVolume = 0.0F;
		public float impactSoundPitch = 0.0F;

		void normalize() {
			weight = Math.max(0, weight);
			bonusDamage = Math.max(0.0F, bonusDamage);
			horizontalLaunch = Math.max(0.0, horizontalLaunch);
			verticalLaunch = Math.max(0.0, verticalLaunch);
			slownessDurationTicks = Math.max(0, slownessDurationTicks);
			slownessAmplifier = Math.max(-1, slownessAmplifier);
			visualDurationTicks = Math.max(0, visualDurationTicks);
			visualSpawnDistance = Math.max(0.0, visualSpawnDistance);
			visualChargeVelocity = Math.max(0.0, visualChargeVelocity);
			particleCount = Math.max(0, particleCount);
			rollSoundVolume = Math.max(0.0F, rollSoundVolume);
			rollSoundPitch = Math.max(0.0F, rollSoundPitch);
			impactSoundVolume = Math.max(0.0F, impactSoundVolume);
			impactSoundPitch = Math.max(0.0F, impactSoundPitch);
		}
	}

	public static final class JesterComedicAssistantParrotConfig {
		public boolean enabled = true;
		public int weight = 1;
		public float bonusDamage = 0.0F;
		public double liftHeight = 0.0;
		public double upwardVelocity = 0.0;
		public int maxCarryTicks = 0;
		public double releaseDownwardVelocity = 0.0;
		public boolean applyGlowing = false;
		public int glowingDurationTicks = 0;
		public int visualDurationTicks = 0;
		public int visualCount = 0;
		public double visualRadius = 0.0;
		public double visualVerticalOffset = 0.0;
		public int particleCount = 0;
		public int flapSoundIntervalTicks = 1;
		public float soundVolume = 0.0F;
		public float soundPitch = 0.0F;

		void normalize() {
			weight = Math.max(0, weight);
			bonusDamage = Math.max(0.0F, bonusDamage);
			liftHeight = Math.max(0.0, liftHeight);
			upwardVelocity = Math.max(0.0, upwardVelocity);
			maxCarryTicks = Math.max(0, maxCarryTicks);
			releaseDownwardVelocity = Math.max(0.0, releaseDownwardVelocity);
			glowingDurationTicks = Math.max(0, glowingDurationTicks);
			visualDurationTicks = Math.max(0, visualDurationTicks);
			visualCount = Math.max(0, visualCount);
			visualRadius = Math.max(0.0, visualRadius);
			particleCount = Math.max(0, particleCount);
			flapSoundIntervalTicks = Math.max(1, flapSoundIntervalTicks);
			soundVolume = Math.max(0.0F, soundVolume);
			soundPitch = Math.max(0.0F, soundPitch);
		}
	}

	public static final class JesterComedicAssistantDivineConfig {
		public boolean enabled = true;
		public int weight = 1;
		public float bonusDamage = 0.0F;
		public int strikeCount = 0;
		public double strikeRadius = 0.0;
		public int glowingDurationTicks = 0;
		public int blindnessDurationTicks = 0;
		public int blindnessAmplifier = -1;
		public int nauseaDurationTicks = 0;
		public int nauseaAmplifier = -1;
		public int particleCount = 0;
		public float soundVolume = 0.0F;
		public float soundPitch = 0.0F;

		void normalize() {
			weight = Math.max(0, weight);
			bonusDamage = Math.max(0.0F, bonusDamage);
			strikeCount = Math.max(0, strikeCount);
			strikeRadius = Math.max(0.0, strikeRadius);
			glowingDurationTicks = Math.max(0, glowingDurationTicks);
			blindnessDurationTicks = Math.max(0, blindnessDurationTicks);
			blindnessAmplifier = Math.max(-1, blindnessAmplifier);
			nauseaDurationTicks = Math.max(0, nauseaDurationTicks);
			nauseaAmplifier = Math.max(-1, nauseaAmplifier);
			particleCount = Math.max(0, particleCount);
			soundVolume = Math.max(0.0F, soundVolume);
			soundPitch = Math.max(0.0F, soundPitch);
		}
	}

	public static final class JesterComedicAssistantAcmeConfig {
		public boolean enabled = true;
		public int weight = 1;
		public float bonusDamage = 0.0F;
		public int slownessDurationTicks = 0;
		public int slownessAmplifier = -1;
		public int weaknessDurationTicks = 0;
		public int weaknessAmplifier = -1;
		public int visualDurationTicks = 0;
		public double visualDropHeight = 0.0;
		public int particleCount = 0;
		public float soundVolume = 0.0F;
		public float soundPitch = 0.0F;

		void normalize() {
			weight = Math.max(0, weight);
			bonusDamage = Math.max(0.0F, bonusDamage);
			slownessDurationTicks = Math.max(0, slownessDurationTicks);
			slownessAmplifier = Math.max(-1, slownessAmplifier);
			weaknessDurationTicks = Math.max(0, weaknessDurationTicks);
			weaknessAmplifier = Math.max(-1, weaknessAmplifier);
			visualDurationTicks = Math.max(0, visualDurationTicks);
			visualDropHeight = Math.max(0.0, visualDropHeight);
			particleCount = Math.max(0, particleCount);
			soundVolume = Math.max(0.0F, soundVolume);
			soundPitch = Math.max(0.0F, soundPitch);
		}
	}

	public static final class JesterComedicAssistantPieConfig {
		public boolean enabled = true;
		public int weight = 1;
		public float bonusDamage = 0.0F;
		public int blindnessDurationTicks = 0;
		public int blindnessAmplifier = -1;
		public int nauseaDurationTicks = 0;
		public int nauseaAmplifier = -1;
		public int particleCount = 0;
		public float soundVolume = 0.0F;
		public float soundPitch = 0.0F;

		void normalize() {
			weight = Math.max(0, weight);
			bonusDamage = Math.max(0.0F, bonusDamage);
			blindnessDurationTicks = Math.max(0, blindnessDurationTicks);
			blindnessAmplifier = Math.max(-1, blindnessAmplifier);
			nauseaDurationTicks = Math.max(0, nauseaDurationTicks);
			nauseaAmplifier = Math.max(-1, nauseaAmplifier);
			particleCount = Math.max(0, particleCount);
			soundVolume = Math.max(0.0F, soundVolume);
			soundPitch = Math.max(0.0F, soundPitch);
		}
	}

	public static final class JesterComedicAssistantCaneConfig {
		public boolean enabled = true;
		public int weight = 1;
		public float bonusDamage = 0.0F;
		public double horizontalLaunch = 0.0;
		public double verticalLaunch = 0.0;
		public int launchControlTicks = 0;
		public int velocityDamageTrackingTicks = 0;
		public double velocityDamageThreshold = 0.0;
		public double velocityDamageMultiplier = 0.0;
		public double velocityDamageMax = 0.0;
		public int slownessDurationTicks = 0;
		public int slownessAmplifier = -1;
		public int visualDurationTicks = 0;
		public double visualSpawnDistance = 0.0;
		public double visualChargeVelocity = 0.0;
		public int particleCount = 0;
		public float soundVolume = 0.0F;
		public float soundPitch = 0.0F;

		void normalize() {
			weight = Math.max(0, weight);
			bonusDamage = Math.max(0.0F, bonusDamage);
			horizontalLaunch = Math.max(0.0, horizontalLaunch);
			verticalLaunch = Math.max(0.0, verticalLaunch);
			launchControlTicks = Math.max(0, launchControlTicks);
			velocityDamageTrackingTicks = Math.max(0, velocityDamageTrackingTicks);
			velocityDamageThreshold = Math.max(0.0, velocityDamageThreshold);
			velocityDamageMultiplier = Math.max(0.0, velocityDamageMultiplier);
			velocityDamageMax = Math.max(0.0, velocityDamageMax);
			slownessDurationTicks = Math.max(0, slownessDurationTicks);
			slownessAmplifier = Math.max(-1, slownessAmplifier);
			visualDurationTicks = Math.max(0, visualDurationTicks);
			visualSpawnDistance = Math.max(0.0, visualSpawnDistance);
			visualChargeVelocity = Math.max(0.0, visualChargeVelocity);
			particleCount = Math.max(0, particleCount);
			soundVolume = Math.max(0.0F, soundVolume);
			soundPitch = Math.max(0.0F, soundPitch);
		}
	}

	public static final class JesterPlusUltraConfig {
		public double activationCostPercent = 80.0;
		public int durationTicks = 90 * 20;
		public double outlineRadius = 150.0;
		public int outlineRefreshTicks = 10;
		public double incomingDamageMultiplier = 0.1;
		public boolean flightEnabled = true;
		public boolean elytraPoseWhileFlying = true;
		public boolean overheadTextEnabled = true;
		public String overheadText = "No More Jokes.";
		public int overheadTextRefreshTicks = 2;
		public int overheadTextDurationTicks = 3 * 20;
		public double overheadTextVerticalOffset = 2.85;
		public float flightFlySpeed = 0.16F;
		public double flightSprintSpeedMultiplier = 1.75;
		public double flightAcceleration = 0.28;
		public double flightMaxSpeed = 3.75;
		public double flightVerticalAcceleration = 0.16;
		public double flightVerticalMaxSpeed = 1.35;
		public double flightDrag = 0.95;
		public float meleeBonusDamage = 6.0F;
		public boolean allowPlayerTargets = true;
		public boolean allowMobTargets = true;
		public double flingHorizontalStrength = 5.75;
		public double flingVerticalStrength = 1.05;
		public int impactTrackingTicks = 35;
		public double impactVelocityThreshold = 1.1;
		public double impactDamageMultiplier = 3.0;
		public double impactDamageMax = 16.0;
		public int smokeParticleCount = 16;
		public double smokeParticleSpread = 0.4;
		public double smokeParticleSpeed = 0.05;
		public int hitDustParticleCount = 10;
		public double hitDustParticleSpread = 0.28;
		public double hitDustParticleSpeed = 0.02;
		public int impactParticleCount = 28;
		public double impactParticleSpread = 0.55;
		public double impactParticleSpeed = 0.12;
		public int impactDustParticleCount = 20;
		public double impactDustParticleSpread = 0.4;
		public double impactDustParticleSpeed = 0.05;
		public float impactSoundVolume = 1.0F;
		public float impactSoundPitch = 0.8F;
		public int earlyCancelPenaltyDurationTicks = 15 * 20;
		public int earlyCancelSlownessAmplifier = 0;
		public int earlyCancelWeaknessAmplifier = 0;
		public int fullEndPenaltyDurationTicks = 30 * 20;
		public int fullEndSlownessAmplifier = 1;
		public int fullEndWeaknessAmplifier = 1;
		public int earlyCancelCooldownTicks = 15 * 60 * 20;
		public int fullEndCooldownTicks = 20 * 60 * 20;

		void normalize() {
			activationCostPercent = MathHelper.clamp(activationCostPercent, 0.0, 100.0);
			durationTicks = Math.max(1, durationTicks);
			outlineRadius = Math.max(0.0, outlineRadius);
			outlineRefreshTicks = Math.max(1, outlineRefreshTicks);
			incomingDamageMultiplier = MathHelper.clamp(incomingDamageMultiplier, 0.0, 1.0);
			if (overheadText == null || overheadText.isBlank()) {
				overheadText = "No More Jokes.";
			} else {
				overheadText = overheadText.trim();
			}
			overheadTextRefreshTicks = Math.max(1, overheadTextRefreshTicks);
			overheadTextDurationTicks = Math.max(0, overheadTextDurationTicks);
			overheadTextVerticalOffset = Math.max(0.0, overheadTextVerticalOffset);
			flightFlySpeed = Math.max(0.0F, flightFlySpeed);
			flightSprintSpeedMultiplier = Math.max(1.0, flightSprintSpeedMultiplier);
			flightAcceleration = Math.max(0.0, flightAcceleration);
			flightMaxSpeed = Math.max(0.0, flightMaxSpeed);
			flightVerticalAcceleration = Math.max(0.0, flightVerticalAcceleration);
			flightVerticalMaxSpeed = Math.max(0.0, flightVerticalMaxSpeed);
			flightDrag = MathHelper.clamp(flightDrag, 0.0, 1.0);
			meleeBonusDamage = Math.max(0.0F, meleeBonusDamage);
			flingHorizontalStrength = Math.max(0.0, flingHorizontalStrength);
			flingVerticalStrength = Math.max(0.0, flingVerticalStrength);
			impactTrackingTicks = Math.max(0, impactTrackingTicks);
			impactVelocityThreshold = Math.max(0.0, impactVelocityThreshold);
			impactDamageMultiplier = Math.max(0.0, impactDamageMultiplier);
			impactDamageMax = Math.max(0.0, impactDamageMax);
			smokeParticleCount = Math.max(0, smokeParticleCount);
			smokeParticleSpread = Math.max(0.0, smokeParticleSpread);
			smokeParticleSpeed = Math.max(0.0, smokeParticleSpeed);
			hitDustParticleCount = Math.max(0, hitDustParticleCount);
			hitDustParticleSpread = Math.max(0.0, hitDustParticleSpread);
			hitDustParticleSpeed = Math.max(0.0, hitDustParticleSpeed);
			impactParticleCount = Math.max(0, impactParticleCount);
			impactParticleSpread = Math.max(0.0, impactParticleSpread);
			impactParticleSpeed = Math.max(0.0, impactParticleSpeed);
			impactDustParticleCount = Math.max(0, impactDustParticleCount);
			impactDustParticleSpread = Math.max(0.0, impactDustParticleSpread);
			impactDustParticleSpeed = Math.max(0.0, impactDustParticleSpeed);
			impactSoundVolume = Math.max(0.0F, impactSoundVolume);
			impactSoundPitch = Math.max(0.0F, impactSoundPitch);
			earlyCancelPenaltyDurationTicks = Math.max(0, earlyCancelPenaltyDurationTicks);
			earlyCancelSlownessAmplifier = Math.max(-1, earlyCancelSlownessAmplifier);
			earlyCancelWeaknessAmplifier = Math.max(-1, earlyCancelWeaknessAmplifier);
			fullEndPenaltyDurationTicks = Math.max(0, fullEndPenaltyDurationTicks);
			fullEndSlownessAmplifier = Math.max(-1, fullEndSlownessAmplifier);
			fullEndWeaknessAmplifier = Math.max(-1, fullEndWeaknessAmplifier);
			earlyCancelCooldownTicks = Math.max(0, earlyCancelCooldownTicks);
			fullEndCooldownTicks = Math.max(0, fullEndCooldownTicks);
		}
	}
}

