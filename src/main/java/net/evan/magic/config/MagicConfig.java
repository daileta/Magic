package net.evan.magic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.evan.magic.Magic;
import net.evan.magic.magic.ability.MagicAbility;
import net.fabricmc.loader.api.FabricLoader;

public final class MagicConfig {
	private static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.disableHtmlEscaping()
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
		@SerializedName(value = "emptyEmbrace", alternate = { "manipulation" })
		public EmptyEmbraceConfig emptyEmbrace = new EmptyEmbraceConfig();
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
			if (emptyEmbrace == null) {
				emptyEmbrace = new EmptyEmbraceConfig();
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

			abilityAccess.normalize();
		}
	}

	public static final class ManaConfig {
		public int belowFreezingDrainPerSecond = 5;
		public int absoluteZeroDrainPerSecond = 20;
		public int loveAtFirstSightIdleDrainPerSecond = 2;
		public int loveAtFirstSightActiveDrainPerSecond = 5;
		public double tillDeathDoUsPartDrainPercentPerSecond = 3.0;
		@SerializedName(value = "emptyEmbraceActivationCost", alternate = { "manipulationActivationCost" })
		public int emptyEmbraceActivationCost = 0;
		@SerializedName(value = "emptyEmbraceDrainPerSecond", alternate = { "manipulationDrainPerSecond" })
		public int emptyEmbraceDrainPerSecond = 0;
		public int domainExpansionActivationCost = 0;
		public int passiveRegenPerSecond = 10;
		public int depletedRecoveryRegenPerSecond = 5;
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
		public int tillDeathDoUsPartLinkDurationTicks = 600;
		public int tillDeathDoUsPartCooldownTicks = 3000;
		@SerializedName(value = "emptyEmbraceCooldownTicks", alternate = { "manipulationCooldownTicks" })
		public int emptyEmbraceCooldownTicks = 6000;
		@SerializedName(value = "emptyEmbraceRequestDebounceTicks", alternate = { "manipulationRequestDebounceTicks" })
		public int emptyEmbraceRequestDebounceTicks = 6;
		public int domainExpansionDurationTicks = 1200;
		public int frostDomainCooldownTicks = 1200;
		public int loveDomainCooldownTicks = 36000;
		public int domainClashRegenerationRefreshTicks = 40;
	}

	public static final class DamageConfig {
		public float frostbiteDamage = 2.0F;
		public float absoluteZeroAuraDamage = 4.0F;
		public float planckHeatFrostDamage = 14.0F;
		public float planckHeatEnhancedFireDamage = 6.0F;
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

	public static final class PowderedSnowEffectConfig {
		public int damageIntervalTicks = 20;
		public boolean dealDamageOnInitialApplication = true;
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
