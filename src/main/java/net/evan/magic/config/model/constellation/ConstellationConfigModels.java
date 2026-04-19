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

public abstract class ConstellationConfigModels extends JesterConfigModels {
	public static final class ConstellationCassiopeiaConfig {
		public double detectionRadius = 64.0;
		public int outlineRefreshTicks = 10;

		public void normalize() {
			detectionRadius = Math.max(1.0, detectionRadius);
			outlineRefreshTicks = Math.max(1, outlineRefreshTicks);
		}
	}

	public static final class ConstellationHerculesConfig {
		public double targetRange = 64.0;
		public double splashRadius = 5.0;
		public int effectDurationTicks = 15 * 20;
		public int warningFadeInTicks = 5;
		public int warningStayTicks = 50;
		public int warningFadeOutTicks = 5;
		public float warningScale = 2.1F;
		public double activationCostPercent = 30.0;
		public int cooldownTicks = 45 * 20;
		public float trueDamage = 5.0F;
		public int slownessAmplifier = 255;
		public int resistanceAmplifier = 0;
		public int particleIntervalTicks = 5;
		public String warningColorHex = "#39B7FF";
		public boolean disableManaRegenWhileActive = true;
		public boolean preventCasterDirectDamage = true;
		public boolean interruptTargetItemUse = false;
		public int activationImpactDirtParticleCount = 18;
		public double activationImpactDirtParticleSpeed = 0.08;
		public float activationImpactSoundVolume = 1.2F;
		public float activationImpactSoundPitch = 0.65F;

		public void normalize() {
			targetRange = Math.max(1.0, targetRange);
			splashRadius = Math.max(0.0, splashRadius);
			effectDurationTicks = Math.max(1, effectDurationTicks);
			warningFadeInTicks = Math.max(0, warningFadeInTicks);
			warningStayTicks = Math.max(0, warningStayTicks);
			warningFadeOutTicks = Math.max(0, warningFadeOutTicks);
			warningScale = Math.max(0.5F, warningScale);
			activationCostPercent = Math.max(0.0, activationCostPercent);
			cooldownTicks = Math.max(0, cooldownTicks);
			trueDamage = Math.max(0.0F, trueDamage);
			slownessAmplifier = Math.max(0, slownessAmplifier);
			resistanceAmplifier = Math.max(-1, resistanceAmplifier);
			particleIntervalTicks = Math.max(1, particleIntervalTicks);
			if (warningColorHex == null || warningColorHex.isBlank()) {
				warningColorHex = "#39B7FF";
			}
			activationImpactDirtParticleCount = Math.max(0, activationImpactDirtParticleCount);
			activationImpactDirtParticleSpeed = Math.max(0.0, activationImpactDirtParticleSpeed);
			activationImpactSoundVolume = Math.max(0.0F, activationImpactSoundVolume);
			activationImpactSoundPitch = Math.max(0.0F, activationImpactSoundPitch);
		}
	}

	public static final class ConstellationCelestialAlignmentConfig {
		public double activationCostPercent = 15.0;
		public int normalCooldownTicks = 2 * 60 * 20;
		public int gamaRayCooldownTicks = 7 * 60 * 20;
		public double targetPlacementRange = 15.0;
		public double columnHeight = 10.0;
		public int maxActiveConstellations = 3;
		public double manaDrainPercentPerSecondPerActiveConstellation = 1.0;
		public boolean disableManaRegenWhileConstellationsActive = true;
		public boolean overlapAllowed = true;
		public boolean shiftCancelEnabled = true;
		public boolean autoEndOnManaDepletion = true;
		public BannerConfig banner = new BannerConfig();
		public VisualConfig visuals = new VisualConfig();
		public CraterConfig crater = new CraterConfig();
		public SagittaConfig sagitta = new SagittaConfig();
		public GeminiConfig gemini = new GeminiConfig();
		public AquilaConfig aquila = new AquilaConfig();
		public ScorpiusConfig scorpius = new ScorpiusConfig();
		public LibraConfig libra = new LibraConfig();
		public GamaRayConfig gamaRay = new GamaRayConfig();

		public void normalize() {
			activationCostPercent = MathHelper.clamp(activationCostPercent, 0.0, 100.0);
			normalCooldownTicks = Math.max(0, normalCooldownTicks);
			gamaRayCooldownTicks = Math.max(0, gamaRayCooldownTicks);
			targetPlacementRange = Math.max(1.0, targetPlacementRange);
			columnHeight = Math.max(1.0, columnHeight);
			maxActiveConstellations = MathHelper.clamp(maxActiveConstellations, 1, 3);
			manaDrainPercentPerSecondPerActiveConstellation = MathHelper.clamp(manaDrainPercentPerSecondPerActiveConstellation, 0.0, 100.0);
			if (banner == null) {
				banner = new BannerConfig();
			}
			if (visuals == null) {
				visuals = new VisualConfig();
			}
			if (crater == null) {
				crater = new CraterConfig();
			}
			if (sagitta == null) {
				sagitta = new SagittaConfig();
			}
			if (gemini == null) {
				gemini = new GeminiConfig();
			}
			if (aquila == null) {
				aquila = new AquilaConfig();
			}
			if (scorpius == null) {
				scorpius = new ScorpiusConfig();
			}
			if (libra == null) {
				libra = new LibraConfig();
			}
			if (gamaRay == null) {
				gamaRay = new GamaRayConfig();
			}
			banner.normalize();
			visuals.normalize();
			crater.normalize();
			sagitta.normalize();
			gemini.normalize();
			aquila.normalize();
			scorpius.normalize();
			libra.normalize();
			gamaRay.normalize();
		}
	}

	public static class ConstellationCelestialAlignmentEntryConfig {
		public double radius = 15.0;
		public String colorHex = "#FFFFFF";

		protected void normalizeBase() {
			radius = Math.max(0.5, radius);
			colorHex = normalizeColorHex(colorHex, "#FFFFFF");
		}
	}

	public static final class VisualConfig {
		public boolean usePlayerAnimations = true;
		public boolean useGeckoLibAquilaProjectile = false;
		public boolean useGeckoLibGamaRayChargeOrb = false;
		public boolean reducedFirstPersonEffects = false;
		public boolean reducedBeamDensity = false;
		public boolean reducedConstellationAmbientParticles = false;
		public double maxVisibleConstellationEffectsDistance = 64.0;
		public double maxVisibleBeamEffectsDistance = 96.0;
		public boolean allowCameraShake = true;
		public boolean allowFovPulse = true;
		public boolean lowFxFallbackMode = false;
		public int placementBuildInTicks = 18;
		public int nodePulseIntervalTicks = 14;
		public float nodePulseScale = 1.15F;
		public String lineDrawMode = "segment";
		public double lineTravelSpeed = 0.12;
		public int ambientParticleIntervalTicks = 6;
		public int ambientParticleCount = 4;
		public int verticalThreadCount = 10;
		public float verticalThreadOpacity = 0.42F;
		public boolean ringShimmerEnabled = true;
		public int ringShimmerIntervalTicks = 10;
		public boolean summonFlourishEnabled = true;
		public int summonFlourishDurationTicks = 10;
		public int groundRingParticleCount = 56;
		public int topRingParticleCount = 40;
		public int starNodeParticleCount = 5;
		public int connectionLineParticleCount = 12;
		public float ringParticleScale = 1.15F;
		public float starNodeParticleScale = 1.0F;
		public float connectionParticleScale = 0.82F;
		public float verticalThreadParticleScale = 0.5F;
		public String groundRingPrimaryColorHex = "#CFE7FF";
		public String groundRingSecondaryColorHex = "#FFFFFF";
		public String topRingPrimaryColorHex = "#D9F2FF";
		public String topRingSecondaryColorHex = "#9FD4FF";
		public String verticalThreadColorHex = "#D3ECFF";
		public String shimmerColorHex = "#FFFFFF";

		public void normalize() {
			maxVisibleConstellationEffectsDistance = Math.max(8.0, maxVisibleConstellationEffectsDistance);
			maxVisibleBeamEffectsDistance = Math.max(8.0, maxVisibleBeamEffectsDistance);
			placementBuildInTicks = Math.max(1, placementBuildInTicks);
			nodePulseIntervalTicks = Math.max(1, nodePulseIntervalTicks);
			nodePulseScale = Math.max(0.1F, nodePulseScale);
			if (lineDrawMode == null || lineDrawMode.isBlank()) {
				lineDrawMode = "segment";
			} else {
				lineDrawMode = lineDrawMode.trim().toLowerCase(java.util.Locale.ROOT);
			}
			lineTravelSpeed = Math.max(0.01, lineTravelSpeed);
			ambientParticleIntervalTicks = Math.max(1, ambientParticleIntervalTicks);
			ambientParticleCount = Math.max(0, ambientParticleCount);
			verticalThreadCount = Math.max(0, verticalThreadCount);
			verticalThreadOpacity = MathHelper.clamp(verticalThreadOpacity, 0.0F, 1.0F);
			ringShimmerIntervalTicks = Math.max(1, ringShimmerIntervalTicks);
			summonFlourishDurationTicks = Math.max(1, summonFlourishDurationTicks);
			groundRingParticleCount = Math.max(0, groundRingParticleCount);
			topRingParticleCount = Math.max(0, topRingParticleCount);
			starNodeParticleCount = Math.max(0, starNodeParticleCount);
			connectionLineParticleCount = Math.max(0, connectionLineParticleCount);
			ringParticleScale = Math.max(0.1F, ringParticleScale);
			starNodeParticleScale = Math.max(0.1F, starNodeParticleScale);
			connectionParticleScale = Math.max(0.1F, connectionParticleScale);
			verticalThreadParticleScale = Math.max(0.1F, verticalThreadParticleScale);
			groundRingPrimaryColorHex = normalizeColorHex(groundRingPrimaryColorHex, "#CFE7FF");
			groundRingSecondaryColorHex = normalizeColorHex(groundRingSecondaryColorHex, "#FFFFFF");
			topRingPrimaryColorHex = normalizeColorHex(topRingPrimaryColorHex, "#D9F2FF");
			topRingSecondaryColorHex = normalizeColorHex(topRingSecondaryColorHex, "#9FD4FF");
			verticalThreadColorHex = normalizeColorHex(verticalThreadColorHex, "#D3ECFF");
			shimmerColorHex = normalizeColorHex(shimmerColorHex, "#FFFFFF");
		}
	}

	public static final class BannerConfig {
		public boolean enabled = true;
		public float scale = 1.2F;
		public int fadeInTicks = 4;
		public int stayTicks = 60;
		public int fadeOutTicks = 8;
		public int screenXOffset = 0;
		public int screenYOffset = 0;

		public void normalize() {
			scale = Math.max(0.5F, scale);
			fadeInTicks = Math.max(0, fadeInTicks);
			stayTicks = Math.max(0, stayTicks);
			fadeOutTicks = Math.max(0, fadeOutTicks);
		}
	}

	public static final class CraterConfig extends ConstellationCelestialAlignmentEntryConfig {
		public boolean blockNaturalHealing = true;
		public boolean blockPotionHealing = true;
		public boolean blockEffectHealing = true;
		public boolean blockMagicHealing = true;
		public int siphonRippleIntervalTicks = 14;
		public int siphonParticleCount = 8;

		public CraterConfig() {
			radius = 15.0;
			colorHex = "#6F8AA5";
		}

		public void normalize() {
			normalizeBase();
			siphonRippleIntervalTicks = Math.max(1, siphonRippleIntervalTicks);
			siphonParticleCount = Math.max(0, siphonParticleCount);
		}
	}

	public static final class SagittaConfig extends ConstellationCelestialAlignmentEntryConfig {
		public int beamStartDelayTicks = 20;
		public int beamIntervalTicks = 10;
		public int beamsPerInterval = 1;
		public int telegraphTicks = 10;
		public double telegraphRingSize = 1.5;
		public float previewShaftOpacity = 0.45F;
		public int impactBurstDensity = 12;
		public float strikeDamage = 2.0F;
		public double strikeHitRadius = 1.0;
		public float strikeSoundVolume = 1.0F;
		public float strikeSoundPitch = 1.15F;
		public boolean layeredStrikeSound = true;

		public SagittaConfig() {
			radius = 18.0;
			colorHex = "#63C8FF";
		}

		public void normalize() {
			normalizeBase();
			beamStartDelayTicks = Math.max(0, beamStartDelayTicks);
			beamIntervalTicks = Math.max(1, beamIntervalTicks);
			beamsPerInterval = Math.max(0, beamsPerInterval);
			telegraphTicks = Math.max(0, telegraphTicks);
			telegraphRingSize = Math.max(0.1, telegraphRingSize);
			previewShaftOpacity = MathHelper.clamp(previewShaftOpacity, 0.0F, 1.0F);
			impactBurstDensity = Math.max(0, impactBurstDensity);
			strikeDamage = Math.max(0.0F, strikeDamage);
			strikeHitRadius = Math.max(0.1, strikeHitRadius);
			strikeSoundVolume = Math.max(0.0F, strikeSoundVolume);
			strikeSoundPitch = Math.max(0.0F, strikeSoundPitch);
		}
	}

	public static final class GeminiConfig extends ConstellationCelestialAlignmentEntryConfig {
		public int echoMarkerDurationTicks = 18;
		public int maxVisibleMarkers = 5;
		public int replayBurstSpacingTicks = 1;
		public float replayFlashIntensity = 0.8F;

		public GeminiConfig() {
			radius = 20.0;
			colorHex = "#F6D6FF";
		}

		public void normalize() {
			normalizeBase();
			echoMarkerDurationTicks = Math.max(1, echoMarkerDurationTicks);
			maxVisibleMarkers = Math.max(1, maxVisibleMarkers);
			replayBurstSpacingTicks = Math.max(0, replayBurstSpacingTicks);
			replayFlashIntensity = Math.max(0.0F, replayFlashIntensity);
		}
	}

	public static final class AquilaConfig extends ConstellationCelestialAlignmentEntryConfig {
		public boolean useGeckoLibProjectile = false;
		public int projectileSpawnIntervalTicks = 8;
		public int projectilesPerInterval = 1;
		public float projectileDamage = 4.0F;
		public double projectileSpeed = 1.25;
		public double projectileScale = 0.75;
		public int trailParticleCount = 3;
		public int trailLength = 6;
		public int impactSparkCount = 12;
		public double homingTurnRate = 0.32;
		public double projectileGlowPulseSpeed = 0.18;
		public double projectileSpinSpeed = 0.24;
		public double hitRadius = 0.9;
		public int maxLifetimeTicks = 60;

		public AquilaConfig() {
			radius = 15.0;
			colorHex = "#FFE58E";
		}

		public void normalize() {
			normalizeBase();
			projectileSpawnIntervalTicks = Math.max(1, projectileSpawnIntervalTicks);
			projectilesPerInterval = Math.max(0, projectilesPerInterval);
			projectileDamage = Math.max(0.0F, projectileDamage);
			projectileSpeed = Math.max(0.0, projectileSpeed);
			projectileScale = Math.max(0.1, projectileScale);
			trailParticleCount = Math.max(0, trailParticleCount);
			trailLength = Math.max(0, trailLength);
			impactSparkCount = Math.max(0, impactSparkCount);
			homingTurnRate = Math.max(0.0, homingTurnRate);
			projectileGlowPulseSpeed = Math.max(0.0, projectileGlowPulseSpeed);
			projectileSpinSpeed = Math.max(0.0, projectileSpinSpeed);
			hitRadius = Math.max(0.1, hitRadius);
			maxLifetimeTicks = Math.max(1, maxLifetimeTicks);
		}
	}

	public static final class ScorpiusConfig extends ConstellationCelestialAlignmentEntryConfig {
		public double incomingDamageMultiplier = 2.0;
		public int stingAccentParticleCount = 4;

		public ScorpiusConfig() {
			radius = 18.0;
			colorHex = "#FF7C8A";
		}

		public void normalize() {
			normalizeBase();
			incomingDamageMultiplier = Math.max(1.0, incomingDamageMultiplier);
			stingAccentParticleCount = Math.max(0, stingAccentParticleCount);
		}
	}

	public static final class LibraConfig extends ConstellationCelestialAlignmentEntryConfig {
		public boolean includeOriginalTarget = true;
		public int minimumEligibleTargets = 1;
		public double casterIncomingDamageMultiplier = 0.5;
		public int tetherParticleCount = 6;
		public int centerPulseParticleCount = 8;

		public LibraConfig() {
			radius = 20.0;
			colorHex = "#CBE6A4";
		}

		public void normalize() {
			normalizeBase();
			minimumEligibleTargets = Math.max(1, minimumEligibleTargets);
			casterIncomingDamageMultiplier = MathHelper.clamp(casterIncomingDamageMultiplier, 0.0, 1.0);
			tetherParticleCount = Math.max(0, tetherParticleCount);
			centerPulseParticleCount = Math.max(0, centerPulseParticleCount);
		}
	}

	public static final class GamaRayConfig {
		public double minimumStartManaPercent = 80.0;
		public double beamFireManaCostPercent = 100.0;
		public TracingConfig tracing = new TracingConfig();
		public ChargeConfig charge = new ChargeConfig();
		public BeamConfig beam = new BeamConfig();
		public PlayerAnimationConfig playerAnimation = new PlayerAnimationConfig();
		public ChargeConstructConfig chargeConstruct = new ChargeConstructConfig();
		public BeamVisualConfig beamVisual = new BeamVisualConfig();
		public PresentationConfig presentation = new PresentationConfig();

		public void normalize() {
			minimumStartManaPercent = MathHelper.clamp(minimumStartManaPercent, 0.0, 100.0);
			beamFireManaCostPercent = MathHelper.clamp(beamFireManaCostPercent, 0.0, 100.0);
			if (tracing == null) {
				tracing = new TracingConfig();
			}
			if (charge == null) {
				charge = new ChargeConfig();
			}
			if (beam == null) {
				beam = new BeamConfig();
			}
			if (playerAnimation == null) {
				playerAnimation = new PlayerAnimationConfig();
			}
			if (chargeConstruct == null) {
				chargeConstruct = new ChargeConstructConfig();
			}
			if (beamVisual == null) {
				beamVisual = new BeamVisualConfig();
			}
			if (presentation == null) {
				presentation = new PresentationConfig();
			}
			tracing.normalize();
			charge.normalize();
			beam.normalize();
			playerAnimation.normalize();
			chargeConstruct.normalize();
			beamVisual.normalize();
			presentation.normalize();
		}
	}

	public static final class TracingConfig {
		public double toleranceRadius = 34.0;
		public double segmentCompletionRadius = 30.0;
		public double inputScale = 1.0;
		public int resetTimeoutTicks = 8 * 20;
		public int cancelCooldownTicks = 30 * 20;
		public float overlayScale = 1.6F;
		public int overlayXOffset = 0;
		public int overlayYOffset = -12;
		public int lineThickness = 4;
		public int nodeRadius = 4;
		public int cursorRadius = 4;
		public boolean lockCameraWhileTracing = true;
		public String promptColorHex = "#7FD4FF";
		public String pathColorHex = "#86B6FF";
		public String progressColorHex = "#FFF1B0";
		public String activeSegmentColorHex = "#FFFFFF";
		public String startNodeColorHex = "#9CFFDE";
		public String endNodeColorHex = "#FFA0B4";
		public String successColorHex = "#9CFFDE";
		public String failColorHex = "#FF8080";
		public float activeSegmentGlowAlpha = 0.12F;
		public int activeSegmentGlowThickness = 5;
		public float progressGlowAlpha = 0.08F;
		public float backgroundGlowAlpha = 0.03F;
		public int nodeConfirmationFlashTicks = 6;
		public int successBurstTicks = 12;
		public float successBurstScale = 1.85F;

		public void normalize() {
			toleranceRadius = Math.max(1.0, toleranceRadius);
			segmentCompletionRadius = Math.max(1.0, segmentCompletionRadius);
			inputScale = Math.max(0.1, inputScale);
			resetTimeoutTicks = Math.max(1, resetTimeoutTicks);
			cancelCooldownTicks = Math.max(0, cancelCooldownTicks);
			overlayScale = Math.max(0.25F, overlayScale);
			lineThickness = Math.max(1, lineThickness);
			nodeRadius = Math.max(1, nodeRadius);
			cursorRadius = Math.max(1, cursorRadius);
			promptColorHex = normalizeColorHex(promptColorHex, "#7FD4FF");
			pathColorHex = normalizeColorHex(pathColorHex, "#86B6FF");
			progressColorHex = normalizeColorHex(progressColorHex, "#FFF1B0");
			activeSegmentColorHex = normalizeColorHex(activeSegmentColorHex, "#FFFFFF");
			startNodeColorHex = normalizeColorHex(startNodeColorHex, "#9CFFDE");
			endNodeColorHex = normalizeColorHex(endNodeColorHex, "#FFA0B4");
			successColorHex = normalizeColorHex(successColorHex, "#9CFFDE");
			failColorHex = normalizeColorHex(failColorHex, "#FF8080");
			activeSegmentGlowAlpha = MathHelper.clamp(activeSegmentGlowAlpha, 0.0F, 1.0F);
			activeSegmentGlowThickness = Math.max(1, activeSegmentGlowThickness);
			progressGlowAlpha = MathHelper.clamp(progressGlowAlpha, 0.0F, 1.0F);
			backgroundGlowAlpha = MathHelper.clamp(backgroundGlowAlpha, 0.0F, 1.0F);
			nodeConfirmationFlashTicks = Math.max(0, nodeConfirmationFlashTicks);
			successBurstTicks = Math.max(0, successBurstTicks);
			successBurstScale = Math.max(0.1F, successBurstScale);
		}
	}

	public static final class PlayerAnimationConfig {
		public boolean enablePlayerAnimationPose = true;
		public boolean reduceFirstPersonPoseIntensity = true;
		public boolean tracePoseEnabled = true;
		public boolean windupPoseEnabled = true;
		public boolean firingPoseEnabled = true;
		public double sidewaysTurnDegrees = 28.0;
		public double torsoLeanDegrees = 11.0;
		public double firingArmExtensionAmount = 0.95;
		public String offArmBraceMode = "chest";
		public boolean recoilPulseEnabled = true;
		public double recoilPulseAmplitude = 3.2;
		public double recoilPulseSpeed = 0.42;
		public int recoveryDurationTicks = 12;

		public void normalize() {
			sidewaysTurnDegrees = MathHelper.clamp(sidewaysTurnDegrees, -80.0, 80.0);
			torsoLeanDegrees = MathHelper.clamp(torsoLeanDegrees, -45.0, 45.0);
			firingArmExtensionAmount = MathHelper.clamp(firingArmExtensionAmount, 0.0, 1.5);
			if (offArmBraceMode == null || offArmBraceMode.isBlank()) {
				offArmBraceMode = "chest";
			} else {
				offArmBraceMode = offArmBraceMode.trim().toLowerCase(java.util.Locale.ROOT);
			}
			recoilPulseAmplitude = Math.max(0.0, recoilPulseAmplitude);
			recoilPulseSpeed = Math.max(0.0, recoilPulseSpeed);
			recoveryDurationTicks = Math.max(0, recoveryDurationTicks);
		}
	}

	public static final class ChargeConfig {
		public int readyDelayTicks = 3 * 20;
		public boolean useGeckoLibChargeOrb = false;
		public double chargeOrbScale = 1.4;
		public int chargeParticleCount = 12;
		public int chargeParticleIntervalTicks = 2;
		public int chargeSoundIntervalTicks = 10;
		public float chargeSoundVolume = 1.0F;
		public float chargeSoundPitch = 0.85F;

		public void normalize() {
			readyDelayTicks = Math.max(0, readyDelayTicks);
			chargeOrbScale = Math.max(0.1, chargeOrbScale);
			chargeParticleCount = Math.max(0, chargeParticleCount);
			chargeParticleIntervalTicks = Math.max(1, chargeParticleIntervalTicks);
			chargeSoundIntervalTicks = Math.max(1, chargeSoundIntervalTicks);
			chargeSoundVolume = Math.max(0.0F, chargeSoundVolume);
			chargeSoundPitch = Math.max(0.0F, chargeSoundPitch);
		}
	}

	public static final class ChargeConstructConfig {
		public boolean useGeckoLibChargeConstruct = true;
		public double constructScale = 1.45;
		public double constructOffsetForward = 1.2;
		public double constructOffsetUp = -0.18;
		public double constructSpinSpeed = 2.2;
		public int constructShardCount = 6;
		public int constructRingCount = 3;
		public float constructOpacity = 0.9F;
		public double constructPulseSpeed = 0.2;
		public double constructBrightnessIntensity = 1.15;
		public boolean constructPersistDuringBeam = false;
		public boolean constructCollapseOnFire = true;
		public int constructCleanupFadeTicks = 10;

		public void normalize() {
			constructScale = Math.max(0.1, constructScale);
			constructOffsetForward = MathHelper.clamp(constructOffsetForward, -4.0, 8.0);
			constructOffsetUp = MathHelper.clamp(constructOffsetUp, -4.0, 8.0);
			constructSpinSpeed = Math.max(0.0, constructSpinSpeed);
			constructShardCount = Math.max(0, constructShardCount);
			constructRingCount = Math.max(0, constructRingCount);
			constructOpacity = MathHelper.clamp(constructOpacity, 0.0F, 1.0F);
			constructPulseSpeed = Math.max(0.0, constructPulseSpeed);
			constructBrightnessIntensity = Math.max(0.0, constructBrightnessIntensity);
			constructCleanupFadeTicks = Math.max(0, constructCleanupFadeTicks);
		}
	}

	public static final class BeamConfig {
		public double range = 70.0;
		public double radius = 5.0;
		public int durationTicks = 20 * 20;
		public int damageIntervalTicks = 20;
		public float damagePerInterval = 4.0F;
		public boolean blockTeleportForHitTargets = true;
		public boolean immobilizeTargets = true;
		public boolean immobilizeCaster = true;
		public boolean aimLockDuringBeam = true;
		public int particleIntervalTicks = 2;
		public double sliceSpacing = 1.5;
		public int coreParticleCount = 3;
		public int outerParticleCount = 8;
		public int orbitingAccentCount = 6;
		public int travelingFleckCount = 8;
		public int muzzleBurstParticleCount = 32;
		public int impactPressureParticleCount = 6;
		public int soundIntervalTicks = 20;
		public float soundVolume = 1.15F;
		public float soundPitch = 0.7F;
		public String coreColorHex = "#FFD36B";
		public String outerColorHex = "#58C8FF";
		public String accentColorHex = "#7BA7FF";

		public void normalize() {
			range = Math.max(1.0, range);
			radius = Math.max(0.5, radius);
			durationTicks = Math.max(1, durationTicks);
			damageIntervalTicks = Math.max(1, damageIntervalTicks);
			damagePerInterval = Math.max(0.0F, damagePerInterval);
			particleIntervalTicks = Math.max(1, particleIntervalTicks);
			sliceSpacing = Math.max(0.25, sliceSpacing);
			coreParticleCount = Math.max(0, coreParticleCount);
			outerParticleCount = Math.max(0, outerParticleCount);
			orbitingAccentCount = Math.max(0, orbitingAccentCount);
			travelingFleckCount = Math.max(0, travelingFleckCount);
			muzzleBurstParticleCount = Math.max(0, muzzleBurstParticleCount);
			impactPressureParticleCount = Math.max(0, impactPressureParticleCount);
			soundIntervalTicks = Math.max(1, soundIntervalTicks);
			soundVolume = Math.max(0.0F, soundVolume);
			soundPitch = Math.max(0.0F, soundPitch);
			coreColorHex = normalizeColorHex(coreColorHex, "#FFF1C7");
			outerColorHex = normalizeColorHex(outerColorHex, "#7FD6FF");
			accentColorHex = normalizeColorHex(accentColorHex, "#AFA1FF");
		}
	}

	public static final class BeamVisualConfig {
		public double beamStartBackOffsetBlocks = 1.0;
		public boolean beamCoreEnabled = true;
		public double beamCoreThickness = 4.8;
		public double beamCoreBrightness = 1.95;
		public boolean beamOuterShellEnabled = true;
		public double beamOuterShellThickness = 9.8;
		public float beamOuterShellOpacity = 0.84F;
		public boolean beamRibbonEnabled = true;
		public int beamRibbonCount = 7;
		public double beamRibbonOrbitRadius = 4.2;
		public double beamRibbonAngularSpeed = 0.18;
		public double beamRibbonThickness = 0.8;
		public boolean beamFlowEnabled = false;
		public double beamFlowSpeed = 1.1;
		public int beamFlowDensity = 0;
		public boolean beamOriginBurstEnabled = false;
		public double beamOriginBurstRadius = 6.0;
		public int beamOriginBurstParticleCount = 24;
		public boolean beamOriginBurstShockRingEnabled = true;
		public boolean beamImpactPressureEffectsEnabled = true;
		public boolean beamTargetBindVisualsEnabled = true;
		public double beamMaxVisibleDistance = 128.0;
		public double beamSliceStep = 0.35;
		public int beamDecorativeDensity = 104;
		public int beamReducedFxDensity = 30;
		public int beamReducedFirstPersonDensity = 18;
		public boolean beamInteriorParticlesEnabled = true;
		public int beamInteriorParticleCount = 28;
		public double beamInteriorParticleRadiusFactor = 0.72;
		public double beamInteriorParticleSize = 0.18;
		public float beamInteriorParticleOpacity = 0.24F;
		public double beamInteriorParticleScrollSpeed = 1.05;
		public boolean beamInteriorVanillaParticlesEnabled = true;
		public int beamInteriorVanillaParticleIntervalTicks = 2;
		public int beamInteriorVanillaParticleCount = 18;
		public double beamInteriorVanillaParticleScale = 1.15;
		public double beamInteriorVanillaParticleSpeed = 0.1;

		public void normalize() {
			beamStartBackOffsetBlocks = Math.max(0.0, beamStartBackOffsetBlocks);
			beamCoreThickness = Math.max(0.1, beamCoreThickness);
			beamCoreBrightness = Math.max(0.0, beamCoreBrightness);
			beamOuterShellThickness = Math.max(0.1, beamOuterShellThickness);
			beamOuterShellOpacity = MathHelper.clamp(beamOuterShellOpacity, 0.0F, 1.0F);
			beamRibbonCount = Math.max(0, beamRibbonCount);
			beamRibbonOrbitRadius = Math.max(0.0, beamRibbonOrbitRadius);
			beamRibbonAngularSpeed = Math.max(0.0, beamRibbonAngularSpeed);
			beamRibbonThickness = Math.max(0.05, beamRibbonThickness);
			beamFlowSpeed = Math.max(0.0, beamFlowSpeed);
			beamFlowDensity = Math.max(0, beamFlowDensity);
			beamOriginBurstRadius = Math.max(0.0, beamOriginBurstRadius);
			beamOriginBurstParticleCount = Math.max(0, beamOriginBurstParticleCount);
			beamMaxVisibleDistance = Math.max(8.0, beamMaxVisibleDistance);
			beamSliceStep = Math.max(0.25, beamSliceStep);
			beamDecorativeDensity = Math.max(0, beamDecorativeDensity);
			beamReducedFxDensity = Math.max(0, beamReducedFxDensity);
			beamReducedFirstPersonDensity = Math.max(0, beamReducedFirstPersonDensity);
			beamInteriorParticleCount = Math.max(0, beamInteriorParticleCount);
			beamInteriorParticleRadiusFactor = Math.max(0.0, beamInteriorParticleRadiusFactor);
			beamInteriorParticleSize = Math.max(0.01, beamInteriorParticleSize);
			beamInteriorParticleOpacity = MathHelper.clamp(beamInteriorParticleOpacity, 0.0F, 1.0F);
			beamInteriorParticleScrollSpeed = Math.max(0.0, beamInteriorParticleScrollSpeed);
			beamInteriorVanillaParticleIntervalTicks = Math.max(1, beamInteriorVanillaParticleIntervalTicks);
			beamInteriorVanillaParticleCount = Math.max(0, beamInteriorVanillaParticleCount);
			beamInteriorVanillaParticleScale = Math.max(0.1, beamInteriorVanillaParticleScale);
			beamInteriorVanillaParticleSpeed = Math.max(0.0, beamInteriorVanillaParticleSpeed);
		}
	}

	public static final class PresentationConfig {
		public boolean allowScreenFlash = true;
		public float screenFlashStrength = 0.42F;
		public boolean allowCameraShake = true;
		public float cameraShakeStartStrength = 0.75F;
		public float cameraShakeSustainStrength = 0.18F;
		public boolean allowFovPulse = true;
		public float fovPulseAmount = 0.05F;
		public boolean windupSigilsEnabled = true;
		public int windupSigilCount = 3;
		public double windupSigilRadius = 1.9;
		public boolean chargeSoundLayeringEnabled = true;
		public boolean firingSoundLayeringEnabled = true;

		public void normalize() {
			screenFlashStrength = MathHelper.clamp(screenFlashStrength, 0.0F, 1.0F);
			cameraShakeStartStrength = Math.max(0.0F, cameraShakeStartStrength);
			cameraShakeSustainStrength = Math.max(0.0F, cameraShakeSustainStrength);
			fovPulseAmount = Math.max(0.0F, fovPulseAmount);
			windupSigilCount = Math.max(0, windupSigilCount);
			windupSigilRadius = Math.max(0.0, windupSigilRadius);
		}
	}

	public static final class ConstellationOrionConfig {
		public double targetRange = 64.0;
		public int waitCancelCooldownTicks = 3 * 20;
		public int linkDurationTicks = 40 * 20;
		public double manaDrainPercentPerSecond = 10.0;
		public boolean drainManaWhileWaitingForTarget = false;
		public boolean disableManaRegenWhileWaitingForTarget = true;
		public boolean disableManaRegenWhileLinked = true;
		public int cooldownTicks = 10 * 60 * 20;
		public int casterPenaltyRefreshTicks = 20;
		public int casterWeaknessAmplifier = 2;
		public int casterSlownessAmplifier = 2;
		public double casterMaxHealthHearts = 5.0;
		public double casterIncomingDamageMultiplier = 1.5;
		public boolean clearTargetCooldownsOnLock = true;
		public boolean suppressTargetManaCosts = true;
		public boolean suppressTargetCooldowns = true;
		public boolean resetCasterCooldownsOnEnd = true;
		public boolean applyUsedTargetCooldownsOnEnd = true;
		public double greedTargetCoinAmount = 18.0;
		public boolean resetGreedTargetCoinsOnEnd = true;
		public int targetParticleIntervalTicks = 4;
		public int targetParticleBurstCount = 12;
		public double targetParticleSpawnRadius = 0.28;
		public double targetParticleVerticalVelocity = 0.22;
		public double targetParticleForwardVelocity = 0.09;
		public double targetParticleSideVelocity = 0.05;

		public void normalize() {
			targetRange = Math.max(1.0, targetRange);
			waitCancelCooldownTicks = Math.max(0, waitCancelCooldownTicks);
			linkDurationTicks = Math.max(1, linkDurationTicks);
			manaDrainPercentPerSecond = Math.max(0.0, manaDrainPercentPerSecond);
			cooldownTicks = Math.max(0, cooldownTicks);
			casterPenaltyRefreshTicks = Math.max(1, casterPenaltyRefreshTicks);
			casterWeaknessAmplifier = Math.max(0, casterWeaknessAmplifier);
			casterSlownessAmplifier = Math.max(0, casterSlownessAmplifier);
			casterMaxHealthHearts = Math.max(1.0, casterMaxHealthHearts);
			casterIncomingDamageMultiplier = Math.max(1.0, casterIncomingDamageMultiplier);
			greedTargetCoinAmount = normalizeCoinAmount(greedTargetCoinAmount);
			targetParticleIntervalTicks = Math.max(1, targetParticleIntervalTicks);
			targetParticleBurstCount = Math.max(0, targetParticleBurstCount);
			targetParticleSpawnRadius = Math.max(0.0, targetParticleSpawnRadius);
			targetParticleVerticalVelocity = Math.max(0.0, targetParticleVerticalVelocity);
			targetParticleForwardVelocity = Math.max(0.0, targetParticleForwardVelocity);
			targetParticleSideVelocity = Math.max(0.0, targetParticleSideVelocity);
		}
	}

	public static final class ConstellationDomainConfig {
		public int chargeDurationTicks = 30 * 20;
		public int acquireWindowTicks = 60 * 20;
		public int durationTicks = 2 * 60 * 20;
		public List<Integer> expiryWarningTicks = new ArrayList<>(List.of(30 * 20, 10 * 20, 5 * 20));
		public boolean allowMobTargets = false;
		public double targetRange = 64.0;
		public double beamRadius = 3.0;
		public double beamRiseBlocksPerSecond = 2.0;
		public double beamParticleStep = 1.5;
		public double beamDescentBlocksPerSecond = 18.0;
		public int beamCoreParticleCount = 2;
		public int beamOuterParticleCount = 2;
		@SerializedName(value = "beamFallingParticleCount", alternate = { "beamSparkParticleCount" })
		public int beamFallingParticleCount = 18;
		public int beamCoreParticleLifetimeTicks = 5;
		public int beamOuterParticleLifetimeTicks = 6;
		public int beamFallingParticleLifetimeTicks = 12;
		public float beamCoreParticleScale = 1.2F;
		public float beamOuterParticleScale = 0.78F;
		public float beamFallingParticleScale = 0.92F;
		public String beamCoreColorHex = "#FFF6D6";
		public String beamOuterColorHex = "#8AD7FF";
		public float beamCoreIntensity = 0.95F;
		@SerializedName(value = "beamSpiralParticleCount", alternate = { "beamRingPointsPerStep" })
		public int beamSpiralParticleCount = 16;
		public double beamSpiralOrbitRadius = 3.25;
		public double beamSpiralAngularSpeedDegreesPerTick = 14.0;
		public double beamSpiralVerticalSpacing = 9.0;
		public List<String> beamSpiralColorHexes = new ArrayList<>(List.of("#8CD8FF", "#FFDFA2", "#C4A6FF"));
		public int beamSpiralParticleLifetimeTicks = 18;
		public float beamSpiralParticleScale = 0.7F;
		public int beamDamageIntervalTicks = 20;
		public float beamTrueDamagePerInterval = 4.0F;
		public int beamHeavenlySoundIntervalTicks = 20;
		public float beamHeavenlySoundVolume = 1.0F;
		public float beamHeavenlySoundPitch = 1.2F;
		public int cooldownTicks = 15 * 60 * 20;
		public int freezeRefreshTicks = 5;
		public boolean disableManaRegenWhileActive = true;
		public boolean ignoreTotems = true;
		public boolean preserveBeaconAnchor = true;
		public String beaconAnchorBlockId = "evanpack:beacon_anchor";
		public String beaconCoreItemId = "evanpack:beacon_core";
		public String beaconCoreAnchorStateId = "evanpack_beacon_core_anchor";
		public double beaconCoreProtectionRadius = 75.0;
		public boolean cancelBeamOnProtectedBeaconCoreHolder = true;

		public void normalize() {
			chargeDurationTicks = Math.max(1, chargeDurationTicks);
			acquireWindowTicks = Math.max(1, acquireWindowTicks);
			durationTicks = Math.max(1, durationTicks);
			if (expiryWarningTicks == null) {
				expiryWarningTicks = new ArrayList<>(List.of(30 * 20, 10 * 20, 5 * 20));
			}
			expiryWarningTicks.removeIf(ticks -> ticks == null || ticks <= 0);
			if (expiryWarningTicks.isEmpty()) {
				expiryWarningTicks = new ArrayList<>(List.of(30 * 20, 10 * 20, 5 * 20));
			}
			targetRange = Math.max(1.0, targetRange);
			beamRadius = Math.max(0.5, beamRadius);
			beamRiseBlocksPerSecond = Math.max(0.0, beamRiseBlocksPerSecond);
			beamParticleStep = Math.max(0.25, beamParticleStep);
			beamDescentBlocksPerSecond = Math.max(0.0, beamDescentBlocksPerSecond);
			beamCoreParticleCount = Math.max(0, beamCoreParticleCount);
			beamOuterParticleCount = Math.max(0, beamOuterParticleCount);
			beamFallingParticleCount = Math.max(0, beamFallingParticleCount);
			beamCoreParticleLifetimeTicks = Math.max(1, beamCoreParticleLifetimeTicks);
			beamOuterParticleLifetimeTicks = Math.max(1, beamOuterParticleLifetimeTicks);
			beamFallingParticleLifetimeTicks = Math.max(1, beamFallingParticleLifetimeTicks);
			beamCoreParticleScale = Math.max(0.1F, beamCoreParticleScale);
			beamOuterParticleScale = Math.max(0.1F, beamOuterParticleScale);
			beamFallingParticleScale = Math.max(0.1F, beamFallingParticleScale);
			if (beamCoreColorHex == null || beamCoreColorHex.isBlank()) {
				beamCoreColorHex = "#FFF6D6";
			}
			if (beamOuterColorHex == null || beamOuterColorHex.isBlank()) {
				beamOuterColorHex = "#8AD7FF";
			}
			beamCoreIntensity = MathHelper.clamp(beamCoreIntensity, 0.05F, 1.0F);
			beamSpiralParticleCount = Math.max(0, beamSpiralParticleCount);
			beamSpiralOrbitRadius = Math.max(0.0, beamSpiralOrbitRadius);
			beamSpiralAngularSpeedDegreesPerTick = MathHelper.clamp(beamSpiralAngularSpeedDegreesPerTick, -60.0, 60.0);
			beamSpiralVerticalSpacing = Math.max(0.5, beamSpiralVerticalSpacing);
			if (beamSpiralColorHexes == null) {
				beamSpiralColorHexes = new ArrayList<>(List.of("#8CD8FF", "#FFDFA2", "#C4A6FF"));
			}
			beamSpiralColorHexes.removeIf(color -> color == null || color.isBlank());
			if (beamSpiralColorHexes.isEmpty()) {
				beamSpiralColorHexes = new ArrayList<>(List.of("#8CD8FF", "#FFDFA2", "#C4A6FF"));
			}
			beamSpiralParticleLifetimeTicks = Math.max(1, beamSpiralParticleLifetimeTicks);
			beamSpiralParticleScale = Math.max(0.1F, beamSpiralParticleScale);
			beamDamageIntervalTicks = Math.max(1, beamDamageIntervalTicks);
			beamTrueDamagePerInterval = Math.max(0.0F, beamTrueDamagePerInterval);
			beamHeavenlySoundIntervalTicks = Math.max(1, beamHeavenlySoundIntervalTicks);
			beamHeavenlySoundVolume = Math.max(0.0F, beamHeavenlySoundVolume);
			beamHeavenlySoundPitch = Math.max(0.0F, beamHeavenlySoundPitch);
			cooldownTicks = Math.max(0, cooldownTicks);
			freezeRefreshTicks = Math.max(1, freezeRefreshTicks);
			if (beaconAnchorBlockId == null || beaconAnchorBlockId.isBlank()) {
				beaconAnchorBlockId = "evanpack:beacon_anchor";
			}
			if (beaconCoreItemId == null || beaconCoreItemId.isBlank()) {
				beaconCoreItemId = "evanpack:beacon_core";
			}
			if (beaconCoreAnchorStateId == null || beaconCoreAnchorStateId.isBlank()) {
				beaconCoreAnchorStateId = "evanpack_beacon_core_anchor";
			}
			beaconCoreProtectionRadius = Math.max(0.0, beaconCoreProtectionRadius);
		}
	}
}


