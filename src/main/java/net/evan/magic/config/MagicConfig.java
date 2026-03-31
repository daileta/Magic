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
import net.evan.magic.magic.ability.MagicAbility;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

public final class MagicConfig {
	private static final TypeAdapter<Integer> FLEXIBLE_INTEGER_ADAPTER = new TypeAdapter<>() {
		@Override
		public void write(JsonWriter out, Integer value) throws IOException {
			if (value == null) {
				out.nullValue();
				return;
			}

			out.value(value);
		}

		@Override
		public Integer read(JsonReader in) throws IOException {
			JsonToken token = in.peek();
			if (token == JsonToken.NULL) {
				in.nextNull();
				return 0;
			}
			if (token != JsonToken.NUMBER && token != JsonToken.STRING) {
				throw new IOException("Expected numeric config value but found " + token);
			}

			String raw = in.nextString();
			try {
				BigDecimal rounded = new BigDecimal(raw).setScale(0, RoundingMode.HALF_UP);
				if (rounded.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
					return Integer.MAX_VALUE;
				}
				if (rounded.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0) {
					return Integer.MIN_VALUE;
				}
				return rounded.intValueExact();
			} catch (NumberFormatException | ArithmeticException exception) {
				throw new IOException("Invalid integer config value: " + raw, exception);
			}
		}
	};

	private static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.registerTypeAdapter(Integer.class, FLEXIBLE_INTEGER_ADAPTER)
		.registerTypeAdapter(int.class, FLEXIBLE_INTEGER_ADAPTER)
		.create();

	private static final Path CONFIG_PATH = FabricLoader.getInstance()
		.getConfigDir()
		.resolve(Magic.MOD_ID)
		.resolve("magic-server-config.json");

	private static volatile MagicConfigData data = defaultData();

	private MagicConfig() {
	}

	public static void initialize() {
		reload();
	}

	public static boolean reload() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());

			if (!Files.exists(CONFIG_PATH)) {
				MagicConfigData defaults = defaultData();
				write(defaults);
				data = defaults;
				Magic.LOGGER.info("Created default magic config at {}", CONFIG_PATH);
				return true;
			}

			try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
				MagicConfigData loaded = GSON.fromJson(reader, MagicConfigData.class);
				if (loaded == null) {
					throw new IllegalStateException("Config file is empty.");
				}

				loaded.normalize();
				data = loaded;
				return true;
			}
		} catch (Exception exception) {
			Magic.LOGGER.error("Failed to load magic config at {}. Keeping previous values.", CONFIG_PATH, exception);
			return false;
		}
	}

	public static MagicConfigData get() {
		return data;
	}

	public static Path path() {
		return CONFIG_PATH;
	}

	public static synchronized boolean setPlayerAbilityLocked(Collection<UUID> playerIds, MagicAbility ability, boolean locked) {
		if (playerIds == null || playerIds.isEmpty() || ability == null || ability == MagicAbility.NONE || !ability.school().isMagic()) {
			return false;
		}

		try {
			MagicConfigData updated = copyOf(data);
			boolean changed = false;

			for (UUID playerId : playerIds) {
				if (playerId == null) {
					continue;
				}
				changed |= updated.abilityAccess.setPlayerAbilityLocked(playerId, ability, locked);
			}

			if (!changed) {
				return true;
			}

			updated.normalize();
			write(updated);
			data = updated;
			return true;
		} catch (Exception exception) {
			Magic.LOGGER.error("Failed to persist ability {}={} overrides.", ability.id(), locked ? "locked" : "unlocked", exception);
			return false;
		}
	}

	public static synchronized boolean clearPlayerAbilityOverrides(Collection<UUID> playerIds) {
		if (playerIds == null || playerIds.isEmpty()) {
			return false;
		}

		try {
			MagicConfigData updated = copyOf(data);
			boolean changed = false;

			for (UUID playerId : playerIds) {
				if (playerId == null) {
					continue;
				}
				changed |= updated.abilityAccess.clearPlayerOverrides(playerId);
			}

			if (!changed) {
				return true;
			}

			updated.normalize();
			write(updated);
			data = updated;
			return true;
		} catch (Exception exception) {
			Magic.LOGGER.error("Failed to persist player ability override cleanup.", exception);
			return false;
		}
	}

	public static synchronized boolean setAstralCataclysmAllowMobTargets(boolean allowMobTargets) {
		try {
			MagicConfigData updated = copyOf(data);
			if (updated.constellationDomain.allowMobTargets == allowMobTargets) {
				return true;
			}

			updated.constellationDomain.allowMobTargets = allowMobTargets;
			updated.normalize();
			write(updated);
			data = updated;
			return true;
		} catch (Exception exception) {
			Magic.LOGGER.error("Failed to persist Astral Cataclysm mob targeting={} toggle.", allowMobTargets, exception);
			return false;
		}
	}

	private static MagicConfigData defaultData() {
		MagicConfigData defaults = new MagicConfigData();
		defaults.normalize();
		return defaults;
	}

	private static MagicConfigData copyOf(MagicConfigData source) {
		MagicConfigData copy = GSON.fromJson(GSON.toJson(source), MagicConfigData.class);
		if (copy == null) {
			copy = new MagicConfigData();
		}
		copy.normalize();
		return copy;
	}

	private static void write(MagicConfigData config) throws IOException {
		try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
			GSON.toJson(config, writer);
		}
	}

	private static double normalizeCoinAmount(double value) {
		double clamped = Math.max(0.0, value);
		return Math.round(clamped * 100.0) / 100.0;
	}

	private static Set<MagicAbility> parseAbilityIds(List<String> ids, String context) {
		EnumSet<MagicAbility> abilities = EnumSet.noneOf(MagicAbility.class);
		if (ids == null) {
			return abilities;
		}

		for (String id : ids) {
			if (id == null) {
				continue;
			}

			String normalized = id.trim().toLowerCase();
			if (normalized.isEmpty()) {
				continue;
			}

			MagicAbility ability = MagicAbility.fromId(normalized);
			if (ability == MagicAbility.NONE) {
				if (!"none".equals(normalized)) {
					Magic.LOGGER.warn("Ignoring unknown ability id '{}' in {}.", id, context);
				}
				continue;
			}

			abilities.add(ability);
		}

		return abilities;
	}

	private static Map<UUID, Set<MagicAbility>> parsePlayerAbilityIds(Map<String, List<String>> raw, String context) {
		Map<UUID, Set<MagicAbility>> resolved = new HashMap<>();
		if (raw == null) {
			return resolved;
		}

		for (Map.Entry<String, List<String>> entry : raw.entrySet()) {
			String uuidString = entry.getKey();
			if (uuidString == null || uuidString.isBlank()) {
				continue;
			}

			try {
				UUID playerId = UUID.fromString(uuidString.trim());
				Set<MagicAbility> abilities = parseAbilityIds(entry.getValue(), context + "." + uuidString);
				resolved.put(playerId, abilities);
			} catch (IllegalArgumentException exception) {
				Magic.LOGGER.warn("Ignoring invalid UUID '{}' in {}.", uuidString, context);
			}
		}

		return resolved;
	}

	public static final class MagicConfigData {
		public ManaConfig mana = new ManaConfig();
		public TimingConfig timing = new TimingConfig();
		public DamageConfig damage = new DamageConfig();
		public RadiiConfig radii = new RadiiConfig();
		public PotionEffectsConfig potionEffects = new PotionEffectsConfig();
		public ParticlesConfig particles = new ParticlesConfig();
		public LoveAtFirstSightConfig loveAtFirstSight = new LoveAtFirstSightConfig();
		public MartyrsFlameConfig martyrsFlame = new MartyrsFlameConfig();
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
		public ConstellationSagittariusConfig constellationSagittarius = new ConstellationSagittariusConfig();
		public ConstellationOrionConfig constellationOrion = new ConstellationOrionConfig();
		public ConstellationDomainConfig constellationDomain = new ConstellationDomainConfig();
		public GreedConfig greed = new GreedConfig();
		public FrostConfig frost = new FrostConfig();
		public PowderedSnowEffectConfig powderedSnowEffect = new PowderedSnowEffectConfig();
		public DomainClashConfig domainClash = new DomainClashConfig();
		public AbilityAccessConfig abilityAccess = new AbilityAccessConfig();

		private void normalize() {
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
			if (constellationSagittarius == null) {
				constellationSagittarius = new ConstellationSagittariusConfig();
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

			martyrsFlame.normalize();
			loveAtFirstSight.normalize();
			jesterSpotlight.normalize();
			jesterWittyOneLiner.normalize();
			jesterComedicRewrite.normalize();
			jesterComedicAssistant.normalize();
			jesterPlusUltra.normalize();
			constellationCassiopeia.normalize();
			constellationHercules.normalize();
			constellationSagittarius.normalize();
			constellationOrion.normalize();
			constellationDomain.normalize();
			greed.normalize();
			frost.normalize();
			domainControl.normalize();
			domainClash.normalize();
			abilityAccess.normalize();
		}
	}

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
		public int emptyEmbraceDrainPerSecond = 0;
		public int domainExpansionActivationCost = 0;
		public int passiveRegenPerSecond = 2;
		public int depletedRecoveryRegenPerSecond = 1;
		public int planckHeatActivationCost = 75;
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
		public int frostDomainCooldownTicks = 36000;
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

		private void normalize() {
			resistanceAmplifier = Math.max(-1, resistanceAmplifier);
		}
	}

	public static final class MartyrsFlameConfig {
		public boolean applyGlowingEffect = true;
		public boolean disableManaRegenWhileActive = true;
		public boolean fireIgnoresNormalExtinguish = true;
		public int fireParticleIntervalTicks = 4;
		public int fireFlameParticleCount = 6;
		public int fireSmokeParticleCount = 4;

		private void normalize() {
			fireParticleIntervalTicks = Math.max(1, fireParticleIntervalTicks);
			fireFlameParticleCount = Math.max(0, fireFlameParticleCount);
			fireSmokeParticleCount = Math.max(0, fireSmokeParticleCount);
		}
	}

	public static final class DomainControlConfig {
		public boolean blockTeleportAcrossDomainBoundaries = true;

		private void normalize() {
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

	public static final class JesterSpotlightConfig {
		public double detectionRange = 48.0;
		public int effectRefreshTicks = 10;
		public JesterSpotlightStageConfig stageOne = defaultStageOne();
		public JesterSpotlightStageConfig stageTwo = defaultStageTwo();
		public JesterSpotlightStageConfig stageThree = defaultStageThree();

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize(JesterWittyOneLinerTierConfig defaults) {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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
		public int durationTicks = 3 * 60 * 20;
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
		public float flightFlySpeed = 0.12F;
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

		private void normalize() {
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

	public static final class ConstellationCassiopeiaConfig {
		public double detectionRadius = 64.0;
		public int outlineRefreshTicks = 10;

		private void normalize() {
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
		public double activationCostPercent = 40.0;
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

		private void normalize() {
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

	public static final class ConstellationSagittariusConfig {
		public double beamRange = 32.0;
		public double beamRadius = 1.5;
		public int windupTicks = 3 * 20;
		public double windupMovementSpeedMultiplier = 0.5;
		public int chargeGlowParticleCount = 1;
		public int chargeBeamParticleCount = 0;
		public double activationCostPercent = 70.0;
		public int cooldownTicks = 2 * 60 * 20;
		public double closeRangeThreshold = 5.0;
		public double midRangeThreshold = 10.0;
		public float closeRangeTrueDamage = 10.0F;
		public float midRangeTrueDamage = 6.0F;
		public float farRangeTrueDamage = 2.0F;
		public boolean disableManaRegenDuringWindup = true;

		private void normalize() {
			beamRange = Math.max(1.0, beamRange);
			beamRadius = Math.max(0.1, beamRadius);
			windupTicks = Math.max(1, windupTicks);
			windupMovementSpeedMultiplier = Math.max(0.0, windupMovementSpeedMultiplier);
			chargeGlowParticleCount = Math.max(0, chargeGlowParticleCount);
			chargeBeamParticleCount = Math.max(0, chargeBeamParticleCount);
			activationCostPercent = Math.max(0.0, activationCostPercent);
			cooldownTicks = Math.max(0, cooldownTicks);
			closeRangeThreshold = Math.max(0.0, closeRangeThreshold);
			midRangeThreshold = Math.max(closeRangeThreshold, midRangeThreshold);
			closeRangeTrueDamage = Math.max(0.0F, closeRangeTrueDamage);
			midRangeTrueDamage = Math.max(0.0F, midRangeTrueDamage);
			farRangeTrueDamage = Math.max(0.0F, farRangeTrueDamage);
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

		private void normalize() {
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
		public double beamRadius = 5.0;
		public double beamRiseBlocksPerSecond = 2.0;
		public double beamParticleStep = 0.5;
		public int beamCoreParticleCount = 18;
		public int beamOuterParticleCount = 10;
		public int beamSparkParticleCount = 6;
		public int beamRingPointsPerStep = 8;
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

		private void normalize() {
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
			beamParticleStep = Math.max(0.1, beamParticleStep);
			beamCoreParticleCount = Math.max(0, beamCoreParticleCount);
			beamOuterParticleCount = Math.max(0, beamOuterParticleCount);
			beamSparkParticleCount = Math.max(0, beamSparkParticleCount);
			beamRingPointsPerStep = Math.max(0, beamRingPointsPerStep);
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

	public static final class GreedConfig {
		public ManaSicknessConfig manaSickness = new ManaSicknessConfig();
		public AppraisersMarkConfig appraisersMark = new AppraisersMarkConfig();
		public TollkeepersClaimConfig tollkeepersClaim = new TollkeepersClaimConfig();
		public KingsDuesConfig kingsDues = new KingsDuesConfig();
		public BankruptcyConfig bankruptcy = new BankruptcyConfig();

		private void normalize() {
			if (manaSickness == null) {
				manaSickness = new ManaSicknessConfig();
			}
			if (appraisersMark == null) {
				appraisersMark = new AppraisersMarkConfig();
			}
			if (tollkeepersClaim == null) {
				tollkeepersClaim = new TollkeepersClaimConfig();
			}
			if (kingsDues == null) {
				kingsDues = new KingsDuesConfig();
			}
			if (bankruptcy == null) {
				bankruptcy = new BankruptcyConfig();
			}

			manaSickness.normalize();
			appraisersMark.normalize();
			tollkeepersClaim.normalize();
			kingsDues.normalize();
			bankruptcy.normalize();
		}
	}

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

		private void normalize() {
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

		private void normalize() {
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
			Map<String, Double> normalized = new LinkedHashMap<>();
			for (Map.Entry<String, Double> entry : coinTriggers.entrySet()) {
				String key = entry.getKey();
				Double value = entry.getValue();
				if (key == null || key.isBlank() || value == null) {
					continue;
				}
				normalized.put(key.trim().toLowerCase(), normalizeCoinAmount(value));
			}
			if (normalized.isEmpty()) {
				normalized = defaultCoinTriggers();
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
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

		private void normalize() {
			manaSicknessAmplifier = Math.max(-1, manaSicknessAmplifier);
			durationTicks = Math.max(0, durationTicks);
			instantManaDrainPercent = MathHelper.clamp(instantManaDrainPercent, 0.0, 100.0);
			abilityLockTicks = Math.max(0, abilityLockTicks);
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

		private void normalize() {
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
		public String finalStageText = "Final Stage";
		public String textColorHex = "#FFFFFF";
		public String outlineColorHex = "#000000";

		private void normalize() {
			if (timerLabelFormat == null) {
				timerLabelFormat = "Stage %stage% in %time%";
			}
			if (readyLabelFormat == null) {
				readyLabelFormat = "Stage %stage% Ready";
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
		public int stageTwoUnlockTicks = 2 * 60 * 20;
		public int stageThreeUnlockTicks = 3 * 60 * 20;
		public int maximumUnlockTicks = 5 * 60 * 20;
		public int rangedKillProgressTicks = 2 * 60 * 20;
		public int slamKillProgressTicks = 60 * 20;
		public boolean clearUnlocksOnEnd = true;
		public boolean discardExcessProgress = true;
		public boolean resetProgressOnSetback = true;

		private void normalize() {
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

		private void normalize() {
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
		public double speedBlocksPerSecond = 5;
		public float baseDamage = 5.0F;
		public double normalManaCostPercent = 25.0;
		public double overcastChancePercent = 20.0;
		public double overcastManaCostPercent = 75.0;
		public double overcastSpeedBlocksPerSecond = 15;
		public boolean instantKillEnabled = true;
		public double setbackChancePercent = 10.0;
		public double particleSpacing = 0.35;
		public double terrainFollowStepSize = 0.25;
		public int groundSearchDownBlocks = 5;
		public int groundSearchUpBlocks = 2;
		public double eruptionSegmentSpacing = 0.25;

		private void normalize() {
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
		public int particleCount = 18;
		public double groundRingRadius = 0.0;
		public int hazeDensity = 12;
		public double shardVelocity = 0.24;

		private void normalize() {
			radius = Math.max(0.0, radius);
			freezeDurationTicks = Math.max(0, freezeDurationTicks);
			trueDamage = Math.max(0.0F, trueDamage);
			manaCostPercent = MathHelper.clamp(manaCostPercent, 0.0, 100.0);
			setbackChancePercent = MathHelper.clamp(setbackChancePercent, 0.0, 100.0);
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

		private void normalize() {
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

		private void normalize() {
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
		public int cooldownTicks = 60 * 20;
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
		public boolean applyFrost = true;
		public float frostDamagePerTick = 2.0F;
		public boolean applyBlindness = true;
		public boolean applyFreeze = true;
		public boolean finalExecutionEnabled = true;
		public boolean finalExecutionBypassTotems = true;
		public int executionParticleCount = 32;
		public float executionSoundVolume = 1.0F;
		public float executionSoundPitch = 0.9F;

		private void normalize() {
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
		public boolean disableDomainEffectsDuringClash = false;
		public boolean forceLookAtOpponent = true;
		public boolean participantsInvincible = false;
		public boolean pauseTimedDomainCollapseTimers = true;
		public boolean pauseAstralCataclysmPhaseTimers = true;

		private void normalize() {
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

		private transient Set<MagicAbility> resolvedDefaultLockedAbilities = Set.of();
		private transient Map<UUID, Set<MagicAbility>> resolvedLockedAbilitiesByPlayer = Map.of();
		private transient Map<UUID, Set<MagicAbility>> resolvedUnlockedAbilitiesByPlayer = Map.of();

		private void normalize() {
			if (defaultLockedAbilities == null) {
				defaultLockedAbilities = new ArrayList<>();
			}
			if (lockedAbilitiesByPlayer == null) {
				lockedAbilitiesByPlayer = new HashMap<>();
			}
			if (unlockedAbilitiesByPlayer == null) {
				unlockedAbilitiesByPlayer = new HashMap<>();
			}

			resolvedDefaultLockedAbilities = parseAbilityIds(defaultLockedAbilities, "abilityAccess.defaultLockedAbilities");
			resolvedLockedAbilitiesByPlayer = parsePlayerAbilityIds(lockedAbilitiesByPlayer, "abilityAccess.lockedAbilitiesByPlayer");
			resolvedUnlockedAbilitiesByPlayer = parsePlayerAbilityIds(unlockedAbilitiesByPlayer, "abilityAccess.unlockedAbilitiesByPlayer");
		}

		private boolean setPlayerAbilityLocked(UUID playerId, MagicAbility ability, boolean locked) {
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

		private boolean clearPlayerOverrides(UUID playerId) {
			String playerKey = playerId.toString();
			boolean changed = lockedAbilitiesByPlayer.remove(playerKey) != null;
			changed |= unlockedAbilitiesByPlayer.remove(playerKey) != null;
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
