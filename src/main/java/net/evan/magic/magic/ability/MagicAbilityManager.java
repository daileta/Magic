package net.evan.magic.magic.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.evan.magic.Magic;
import net.evan.magic.config.MagicConfig;
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.MagicSchool;
import net.evan.magic.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext;

public final class MagicAbilityManager {
	private static final int TICKS_PER_SECOND = 20;
	private static final int COOLDOWN_MESSAGE_DEBOUNCE_TICKS = 20;
	private static int BELOW_FREEZING_MANA_DRAIN_PER_SECOND = 5;
	private static int ABSOLUTE_ZERO_MANA_DRAIN_PER_SECOND = 20;
	private static int LOVE_AT_FIRST_SIGHT_IDLE_DRAIN_PER_SECOND = 2;
	private static int LOVE_AT_FIRST_SIGHT_ACTIVE_DRAIN_PER_SECOND = 5;
	private static double TILL_DEATH_DO_US_PART_DRAIN_PERCENT_PER_SECOND = 3.0;
	private static int MANIPULATION_ACTIVATION_MANA_COST = 0;
	private static int MANIPULATION_MANA_DRAIN_PER_SECOND = 0;
	private static int PASSIVE_MANA_REGEN_PER_SECOND = 10;
	private static int DEPLETED_RECOVERY_REGEN_PER_SECOND = 5;
	private static int EFFECT_REFRESH_TICKS = 40;

	private static int FROSTBITE_DURATION_TICKS = 120;
	private static int FROSTBITE_DAMAGE_INTERVAL_TICKS = 20;
	private static float FROSTBITE_DAMAGE = 2.0F;
	private static int FROSTBITE_SLOWNESS_AMPLIFIER = 3;
	private static int BELOW_FREEZING_AURA_SLOWNESS_AMPLIFIER = 3;
	private static int BELOW_FREEZING_RESISTANCE_AMPLIFIER = 0;
	private static int ABSOLUTE_ZERO_RESISTANCE_AMPLIFIER = 2;
	private static int PLANCK_HEAT_ABSORPTION_AMPLIFIER = 3;
	private static int PLANCK_HEAT_STRENGTH_AMPLIFIER = 1;
	private static int PLANCK_HEAT_FIRE_RESISTANCE_AMPLIFIER = 0;
	private static int PLANCK_HEAT_FIRE_PHASE_HUNGER_AMPLIFIER = 1;
	private static int PLANCK_HEAT_FIRE_PHASE_NAUSEA_AMPLIFIER = 1;
	private static int FROST_VISUAL_TICKS = 180;
	private static boolean POWDERED_SNOW_DAMAGE_ON_INITIAL_APPLICATION = true;

	private static int BELOW_FREEZING_AURA_RADIUS = 1;
	private static int ABSOLUTE_ZERO_AURA_RADIUS = 4;
	private static int ABSOLUTE_ZERO_AURA_DAMAGE_INTERVAL_TICKS = 50;
	private static float ABSOLUTE_ZERO_AURA_DAMAGE = 4.0F;
	private static int ABSOLUTE_ZERO_AURA_SLOWNESS_AMPLIFIER = 2;
	private static int ABSOLUTE_ZERO_COOLDOWN_TICKS = 60 * TICKS_PER_SECOND;
	private static int PLANCK_HEAT_ACTIVATION_MANA_COST = 75;
	private static int PLANCK_HEAT_COOLDOWN_TICKS = 10 * 60 * TICKS_PER_SECOND;
	private static int PLANCK_HEAT_AURA_RADIUS = 8;
	private static int PLANCK_HEAT_FROST_PHASE_TICKS = 6 * TICKS_PER_SECOND;
	private static int PLANCK_HEAT_FIRE_PHASE_TICKS = 15 * TICKS_PER_SECOND;
	private static int PLANCK_HEAT_FROST_DAMAGE_INTERVAL_TICKS = 20;
	private static float PLANCK_HEAT_FROST_DAMAGE = 14.0F;
	private static int PLANCK_HEAT_FROST_SLOWNESS_AMPLIFIER = 4;
	private static int PLANCK_HEAT_ATTACK_ENHANCED_FIRE_DURATION_TICKS = 8 * TICKS_PER_SECOND;
	private static int PLANCK_HEAT_AURA_ENHANCED_FIRE_DURATION_TICKS = 4 * TICKS_PER_SECOND;
	private static int PLANCK_HEAT_ENHANCED_FIRE_DAMAGE_INTERVAL_TICKS = 20;
	private static float PLANCK_HEAT_ENHANCED_FIRE_DAMAGE = 6.0F;
	private static int PLANCK_HEAT_ABSORPTION_DURATION_TICKS = 2 * 60 * TICKS_PER_SECOND;
	private static int PLANCK_HEAT_STRENGTH_DURATION_TICKS = 2 * 60 * TICKS_PER_SECOND;
	private static int PLANCK_HEAT_FIRE_RESISTANCE_DURATION_TICKS = 8 * 60 * TICKS_PER_SECOND;
	private static int LOVE_GAZE_RANGE = 32;
	private static int LOVE_LOCK_EFFECT_TICKS = 5;
	private static int LOVE_LOCK_SLOWNESS_AMPLIFIER = 255;
	private static int LOVE_LOCK_MINING_FATIGUE_AMPLIFIER = 255;
	private static int DOMAIN_CLASH_REGENERATION_AMPLIFIER = 9;
	private static int DOMAIN_CLASH_INSTANT_HEALTH_AMPLIFIER = 9;
	private static int TILL_DEATH_DO_US_PART_LINK_DURATION_TICKS = 30 * TICKS_PER_SECOND;
	private static int TILL_DEATH_DO_US_PART_COOLDOWN_TICKS = 150 * TICKS_PER_SECOND;
	private static int MANIPULATION_COOLDOWN_TICKS = 6000;
	private static int MANIPULATION_REQUEST_DEBOUNCE_TICKS = 6;
	private static double MANIPULATION_MAX_INPUT_DELTA_PER_TICK = 0.45;
	private static double MANIPULATION_VERTICAL_INPUT_DELTA_PER_TICK = 0.35;
	private static int MANIPULATION_CLAMP_LOG_INTERVAL_TICKS = 20;
	private static int MANIPULATION_ACQUIRE_RANGE = 100;
	private static int MANIPULATION_BREAK_RANGE = 100;
	private static boolean MANIPULATION_DEACTIVATE_TARGET_MAGIC = true;
	private static boolean MANIPULATION_DISABLE_ARTIFACT_POWERS = true;
	private static boolean MANIPULATION_DISMISS_ARTIFACT_SUMMONS = true;
	private static boolean MANIPULATION_DISABLE_ARTIFACT_ARMOR_EFFECTS = true;
	private static boolean MANIPULATION_DISABLE_INFESTED_SILVERFISH_ENHANCEMENTS = true;
	private static boolean MANIPULATION_BLOCK_ARTIFACT_USE_CLICKS = true;
	private static boolean MANIPULATION_BLOCK_ARTIFACT_ATTACK_CLICKS = true;
	private static boolean MANIPULATION_DEBUG_LOGGING = true;
	private static int DOMAIN_EXPANSION_ACTIVATION_MANA_COST = 0;
	private static int DOMAIN_EXPANSION_DURATION_TICKS = 60 * TICKS_PER_SECOND;
	private static int FROST_DOMAIN_COOLDOWN_TICKS = 60 * TICKS_PER_SECOND;
	private static int LOVE_DOMAIN_COOLDOWN_TICKS = 30 * 60 * TICKS_PER_SECOND;
	private static int LOVE_AT_FIRST_SIGHT_PARTICLE_INTERVAL_TICKS = 8;
	private static int LOVE_AT_FIRST_SIGHT_HEART_PARTICLES = 1;
	private static int LOVE_AT_FIRST_SIGHT_HAPPY_VILLAGER_PARTICLES = 0;
	private static int DOMAIN_EXPANSION_RADIUS = 25;
	private static int DOMAIN_EXPANSION_HEIGHT = 25;
	private static int DOMAIN_EXPANSION_SHELL_THICKNESS = 1;
	private static boolean DOMAIN_CLASH_ENABLED = true;
	private static int DOMAIN_CLASH_SIMULTANEOUS_WINDOW_TICKS = 1;
	private static int DOMAIN_CLASH_MIN_EXTERIOR_DISTANCE = 20;
	private static int DOMAIN_CLASH_TITLE_FADE_IN_TICKS = 8;
	private static int DOMAIN_CLASH_TITLE_STAY_TICKS = 44;
	private static int DOMAIN_CLASH_TITLE_FADE_OUT_TICKS = 8;
	private static int DOMAIN_CLASH_INSTRUCTIONS_DURATION_TICKS = 15 * TICKS_PER_SECOND;
	private static int DOMAIN_CLASH_INSTRUCTIONS_FADE_OUT_TICKS = TICKS_PER_SECOND;
	private static int DOMAIN_CLASH_REGENERATION_REFRESH_TICKS = 40;
	private static double DOMAIN_CLASH_DAMAGE_TO_WIN = 250.0;
	private static int DOMAIN_CLASH_LOSER_MANA_DRAIN_PERCENT = 50;
	private static double DOMAIN_CLASH_POST_CLASH_COOLDOWN_MULTIPLIER = 0.5;
	private static int DOMAIN_CLASH_PARTICLES_PER_TICK = 120;
	private static int DOMAIN_CLASH_SPLIT_PATTERN_MODULO = 2;
	private static boolean DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS = false;
	private static boolean DOMAIN_CLASH_FORCE_LOOK = true;
	private static boolean DOMAIN_CLASH_PARTICIPANTS_INVINCIBLE = false;
	private static final int DOMAIN_BLOCK_PLACE_FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE_AND_SKIP_CALLBACKS_AND_DROPS;
	private static final int DOMAIN_BLOCK_RESTORE_FLAGS = Block.NOTIFY_ALL | Block.FORCE_STATE_AND_SKIP_CALLBACKS_AND_DROPS;
	private static final BlockState DOMAIN_SHELL_BLOCK_STATE = Blocks.BLACK_CONCRETE.getDefaultState();
	private static final BlockState DOMAIN_INTERIOR_BLOCK_STATE = Blocks.AIR.getDefaultState();
	private static final BlockState LOVE_DOMAIN_LIGHT_STATE = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15);

	private static final List<net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect>> COMMON_NEGATIVE_EFFECTS = List.of(
		StatusEffects.SLOWNESS,
		StatusEffects.MINING_FATIGUE,
		StatusEffects.NAUSEA,
		StatusEffects.BLINDNESS,
		StatusEffects.HUNGER,
		StatusEffects.WEAKNESS,
		StatusEffects.POISON,
		StatusEffects.WITHER,
		StatusEffects.LEVITATION,
		StatusEffects.UNLUCK,
		StatusEffects.BAD_OMEN,
		StatusEffects.TRIAL_OMEN,
		StatusEffects.RAID_OMEN,
		StatusEffects.DARKNESS
	);

	private static boolean initialized;
	private static final Map<UUID, FrostbiteState> FROSTBITTEN_TARGETS = new HashMap<>();
	private static final Map<UUID, Map<UUID, Integer>> ABSOLUTE_ZERO_NEXT_DAMAGE_TICK = new HashMap<>();
	private static final Map<UUID, Integer> ABSOLUTE_ZERO_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> PLANCK_HEAT_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, PlanckHeatState> PLANCK_HEAT_STATES = new HashMap<>();
	private static final Map<UUID, Map<UUID, Integer>> PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK = new HashMap<>();
	private static final Map<UUID, EnhancedFireState> ENHANCED_FIRE_TARGETS = new HashMap<>();
	private static final Map<UUID, LoveLockState> LOVE_LOCKED_TARGETS = new HashMap<>();
	private static final Map<UUID, Boolean> LOVE_POWER_ACTIVE_THIS_SECOND = new HashMap<>();
	private static final Set<UUID> TILL_DEATH_DO_US_PART_PASSIVE_ENABLED = new HashSet<>();
	private static final Map<UUID, TillDeathDoUsPartState> TILL_DEATH_DO_US_PART_STATES = new HashMap<>();
	private static final Map<UUID, Integer> TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Double> TILL_DEATH_DO_US_PART_DRAIN_BUFFER = new HashMap<>();
	private static final Map<UUID, Integer> TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK = new HashMap<>();
	private static final Map<UUID, ManipulationState> MANIPULATION_STATES = new HashMap<>();
	private static final Map<UUID, UUID> MANIPULATION_CASTER_BY_TARGET = new HashMap<>();
	private static final Map<UUID, PlayerInput> MANIPULATION_INPUT_BY_CASTER = new HashMap<>();
	private static final Map<UUID, ManipulationLookState> MANIPULATION_LOOK_BY_CASTER = new HashMap<>();
	private static final Map<UUID, Integer> MANIPULATION_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> MANIPULATION_NEXT_REQUEST_TICK = new HashMap<>();
	private static final Map<UUID, Integer> MANIPULATION_LAST_CLAMP_LOG_TICK = new HashMap<>();
	private static final Map<UUID, ManipulationProxyState> MANIPULATION_INTERACTION_PROXY = new HashMap<>();
	private static final Map<UUID, DomainExpansionState> DOMAIN_EXPANSIONS = new HashMap<>();
	private static final Map<UUID, DomainClashState> DOMAIN_CLASHES_BY_OWNER = new HashMap<>();
	private static final Map<UUID, UUID> DOMAIN_CLASH_OWNER_BY_PARTICIPANT = new HashMap<>();
	private static final Map<UUID, Integer> FROST_DOMAIN_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> LOVE_DOMAIN_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, DomainPendingReturnState> DOMAIN_PENDING_RETURNS = new HashMap<>();
	private static final RegistryEntryLookup<Block> BLOCK_ENTRY_LOOKUP = Registries.createEntryLookup(Registries.BLOCK);
	private static DomainRuntimePersistentState domainRuntimePersistentState;
	private static final String DOMAIN_PERSISTENCE_DOMAINS_KEY = "domains";
	private static final String DOMAIN_PERSISTENCE_LOVE_COOLDOWNS_KEY = "loveDomainCooldowns";
	private static final String DOMAIN_PERSISTENCE_FROST_COOLDOWNS_KEY = "frostDomainCooldowns";
	private static final String DOMAIN_PERSISTENCE_PENDING_RETURNS_KEY = "pendingDomainReturns";
	private static final String EMPTY_EMBRACE_TAG = "magic.empty_embrace";
	private static final String EMPTY_EMBRACE_ARTIFACT_POWERS_TAG = "magic.empty_embrace.artifact_powers";
	private static final String EMPTY_EMBRACE_ARTIFACT_SUMMONS_TAG = "magic.empty_embrace.artifact_summons";
	private static final String EMPTY_EMBRACE_ARTIFACT_ARMOR_TAG = "magic.empty_embrace.artifact_armor";
	private static final String EMPTY_EMBRACE_INFESTED_SILVERFISH_TAG = "magic.empty_embrace.infested_silverfish";
	private static final String ARTIFACT_ITEM_NAMESPACE = "evanpack";

	private MagicAbilityManager() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		reloadConfigValues();
		ServerLifecycleEvents.SERVER_STARTED.register(MagicAbilityManager::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(MagicAbilityManager::onServerStopping);
		ServerTickEvents.END_SERVER_TICK.register(MagicAbilityManager::onEndServerTick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> applyPendingDomainReturn(handler.player, server));
		AttackEntityCallback.EVENT.register(MagicAbilityManager::onAttackEntity);
		UseItemCallback.EVENT.register(MagicAbilityManager::onUseItem);
		UseBlockCallback.EVENT.register(MagicAbilityManager::onUseBlock);
		UseEntityCallback.EVENT.register(MagicAbilityManager::onUseEntity);
		AttackBlockCallback.EVENT.register(MagicAbilityManager::onAttackBlock);
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> onBeforeBlockBreak(world, player, pos));
		initialized = true;
	}

	public static void reloadConfigValues() {
		MagicConfig.MagicConfigData config = MagicConfig.get();

		BELOW_FREEZING_MANA_DRAIN_PER_SECOND = config.mana.belowFreezingDrainPerSecond;
		ABSOLUTE_ZERO_MANA_DRAIN_PER_SECOND = config.mana.absoluteZeroDrainPerSecond;
		LOVE_AT_FIRST_SIGHT_IDLE_DRAIN_PER_SECOND = config.mana.loveAtFirstSightIdleDrainPerSecond;
		LOVE_AT_FIRST_SIGHT_ACTIVE_DRAIN_PER_SECOND = config.mana.loveAtFirstSightActiveDrainPerSecond;
		TILL_DEATH_DO_US_PART_DRAIN_PERCENT_PER_SECOND = MathHelper.clamp(config.mana.tillDeathDoUsPartDrainPercentPerSecond, 0.0, 100.0);
		MANIPULATION_ACTIVATION_MANA_COST = Math.max(0, config.mana.emptyEmbraceActivationCost);
		MANIPULATION_MANA_DRAIN_PER_SECOND = Math.max(0, config.mana.emptyEmbraceDrainPerSecond);
		DOMAIN_EXPANSION_ACTIVATION_MANA_COST = config.mana.domainExpansionActivationCost;
		PASSIVE_MANA_REGEN_PER_SECOND = config.mana.passiveRegenPerSecond;
		DEPLETED_RECOVERY_REGEN_PER_SECOND = config.mana.depletedRecoveryRegenPerSecond;
		PLANCK_HEAT_ACTIVATION_MANA_COST = config.mana.planckHeatActivationCost;

		EFFECT_REFRESH_TICKS = config.timing.effectRefreshTicks;
		FROSTBITE_DURATION_TICKS = config.timing.frostbiteDurationTicks;
		FROST_VISUAL_TICKS = config.timing.frostVisualTicks;
		ABSOLUTE_ZERO_AURA_DAMAGE_INTERVAL_TICKS = Math.max(1, config.timing.absoluteZeroAuraDamageIntervalTicks);
		ABSOLUTE_ZERO_COOLDOWN_TICKS = config.timing.absoluteZeroCooldownTicks;
		PLANCK_HEAT_COOLDOWN_TICKS = config.timing.planckHeatCooldownTicks;
		PLANCK_HEAT_FROST_PHASE_TICKS = config.timing.planckHeatFrostPhaseTicks;
		PLANCK_HEAT_FIRE_PHASE_TICKS = config.timing.planckHeatFirePhaseTicks;
		PLANCK_HEAT_FROST_DAMAGE_INTERVAL_TICKS = config.timing.planckHeatFrostDamageIntervalTicks;
		PLANCK_HEAT_ENHANCED_FIRE_DAMAGE_INTERVAL_TICKS = config.timing.planckHeatEnhancedFireDamageIntervalTicks;
		PLANCK_HEAT_ATTACK_ENHANCED_FIRE_DURATION_TICKS = config.timing.planckHeatAttackEnhancedFireDurationTicks;
		PLANCK_HEAT_AURA_ENHANCED_FIRE_DURATION_TICKS = config.timing.planckHeatAuraEnhancedFireDurationTicks;
		PLANCK_HEAT_ABSORPTION_DURATION_TICKS = config.timing.planckHeatAbsorptionDurationTicks;
		PLANCK_HEAT_STRENGTH_DURATION_TICKS = config.timing.planckHeatStrengthDurationTicks;
		PLANCK_HEAT_FIRE_RESISTANCE_DURATION_TICKS = config.timing.planckHeatFireResistanceDurationTicks;
		LOVE_LOCK_EFFECT_TICKS = config.timing.loveLockEffectTicks;
		TILL_DEATH_DO_US_PART_LINK_DURATION_TICKS = Math.max(1, config.timing.tillDeathDoUsPartLinkDurationTicks);
		TILL_DEATH_DO_US_PART_COOLDOWN_TICKS = Math.max(0, config.timing.tillDeathDoUsPartCooldownTicks);
		MANIPULATION_COOLDOWN_TICKS = Math.max(0, config.timing.emptyEmbraceCooldownTicks);
		MANIPULATION_REQUEST_DEBOUNCE_TICKS = Math.max(0, config.timing.emptyEmbraceRequestDebounceTicks);
		DOMAIN_EXPANSION_DURATION_TICKS = config.timing.domainExpansionDurationTicks;
		FROST_DOMAIN_COOLDOWN_TICKS = Math.max(0, config.timing.frostDomainCooldownTicks);
		LOVE_DOMAIN_COOLDOWN_TICKS = config.timing.loveDomainCooldownTicks;
		DOMAIN_CLASH_REGENERATION_REFRESH_TICKS = Math.max(1, config.timing.domainClashRegenerationRefreshTicks);

		FROSTBITE_DAMAGE_INTERVAL_TICKS = config.powderedSnowEffect.damageIntervalTicks;
		POWDERED_SNOW_DAMAGE_ON_INITIAL_APPLICATION = config.powderedSnowEffect.dealDamageOnInitialApplication;

		FROSTBITE_DAMAGE = config.damage.frostbiteDamage;
		ABSOLUTE_ZERO_AURA_DAMAGE = Math.max(0.0F, config.damage.absoluteZeroAuraDamage);
		PLANCK_HEAT_FROST_DAMAGE = config.damage.planckHeatFrostDamage;
		PLANCK_HEAT_ENHANCED_FIRE_DAMAGE = config.damage.planckHeatEnhancedFireDamage;

		BELOW_FREEZING_AURA_RADIUS = config.radii.belowFreezingAuraRadius;
		ABSOLUTE_ZERO_AURA_RADIUS = config.radii.absoluteZeroAuraRadius;
		PLANCK_HEAT_AURA_RADIUS = config.radii.planckHeatAuraRadius;
		LOVE_GAZE_RANGE = config.radii.loveGazeRange;
		MANIPULATION_ACQUIRE_RANGE = Math.max(1, config.radii.emptyEmbraceAcquireRange);
		MANIPULATION_BREAK_RANGE = Math.max(1, config.radii.emptyEmbraceBreakRange);
		DOMAIN_EXPANSION_RADIUS = config.radii.domainExpansionRadius;
		DOMAIN_EXPANSION_HEIGHT = config.radii.domainExpansionHeight;
		DOMAIN_EXPANSION_SHELL_THICKNESS = Math.max(1, config.radii.domainExpansionShellThickness);

		FROSTBITE_SLOWNESS_AMPLIFIER = config.potionEffects.frostbiteSlownessAmplifier;
		BELOW_FREEZING_AURA_SLOWNESS_AMPLIFIER = config.potionEffects.belowFreezingAuraSlownessAmplifier;
		BELOW_FREEZING_RESISTANCE_AMPLIFIER = config.potionEffects.belowFreezingResistanceAmplifier;
		ABSOLUTE_ZERO_RESISTANCE_AMPLIFIER = config.potionEffects.absoluteZeroResistanceAmplifier;
		ABSOLUTE_ZERO_AURA_SLOWNESS_AMPLIFIER = config.potionEffects.absoluteZeroAuraSlownessAmplifier;
		PLANCK_HEAT_FROST_SLOWNESS_AMPLIFIER = config.potionEffects.planckHeatFrostSlownessAmplifier;
		PLANCK_HEAT_ABSORPTION_AMPLIFIER = config.potionEffects.planckHeatAbsorptionAmplifier;
		PLANCK_HEAT_STRENGTH_AMPLIFIER = config.potionEffects.planckHeatStrengthAmplifier;
		PLANCK_HEAT_FIRE_RESISTANCE_AMPLIFIER = config.potionEffects.planckHeatFireResistanceAmplifier;
		PLANCK_HEAT_FIRE_PHASE_HUNGER_AMPLIFIER = config.potionEffects.planckHeatFirePhaseHungerAmplifier;
		PLANCK_HEAT_FIRE_PHASE_NAUSEA_AMPLIFIER = config.potionEffects.planckHeatFirePhaseNauseaAmplifier;
		LOVE_LOCK_SLOWNESS_AMPLIFIER = config.potionEffects.loveLockSlownessAmplifier;
		LOVE_LOCK_MINING_FATIGUE_AMPLIFIER = config.potionEffects.loveLockMiningFatigueAmplifier;
		DOMAIN_CLASH_REGENERATION_AMPLIFIER = Math.max(0, config.potionEffects.domainClashRegenerationAmplifier);
		DOMAIN_CLASH_INSTANT_HEALTH_AMPLIFIER = Math.max(0, config.potionEffects.domainClashInstantHealthAmplifier);

		LOVE_AT_FIRST_SIGHT_PARTICLE_INTERVAL_TICKS = Math.max(1, config.particles.loveAtFirstSightParticleIntervalTicks);
		LOVE_AT_FIRST_SIGHT_HEART_PARTICLES = Math.max(0, config.particles.loveAtFirstSightHeartParticles);
		LOVE_AT_FIRST_SIGHT_HAPPY_VILLAGER_PARTICLES = Math.max(0, config.particles.loveAtFirstSightHappyVillagerParticles);

		MANIPULATION_DEACTIVATE_TARGET_MAGIC = config.emptyEmbrace.deactivateTargetMagic;
		MANIPULATION_DISABLE_ARTIFACT_POWERS = config.emptyEmbrace.disableArtifactPowers;
		MANIPULATION_DISMISS_ARTIFACT_SUMMONS = config.emptyEmbrace.dismissArtifactSummons;
		MANIPULATION_DISABLE_ARTIFACT_ARMOR_EFFECTS = config.emptyEmbrace.disableArtifactArmorEffects;
		MANIPULATION_DISABLE_INFESTED_SILVERFISH_ENHANCEMENTS = config.emptyEmbrace.disableInfestedSilverfishEnhancements;
		MANIPULATION_BLOCK_ARTIFACT_USE_CLICKS = config.emptyEmbrace.blockArtifactUseClicks;
		MANIPULATION_BLOCK_ARTIFACT_ATTACK_CLICKS = config.emptyEmbrace.blockArtifactAttackClicks;
		MANIPULATION_DEBUG_LOGGING = config.emptyEmbrace.debugLogging;

		DOMAIN_CLASH_ENABLED = config.domainClash.enabled;
		DOMAIN_CLASH_SIMULTANEOUS_WINDOW_TICKS = Math.max(0, config.domainClash.simultaneousCastWindowTicks);
		DOMAIN_CLASH_MIN_EXTERIOR_DISTANCE = Math.max(0, config.domainClash.minimumExteriorDistance);
		DOMAIN_CLASH_TITLE_FADE_IN_TICKS = Math.max(0, config.domainClash.titleFadeInTicks);
		DOMAIN_CLASH_TITLE_STAY_TICKS = Math.max(0, config.domainClash.titleStayTicks);
		DOMAIN_CLASH_TITLE_FADE_OUT_TICKS = Math.max(0, config.domainClash.titleFadeOutTicks);
		DOMAIN_CLASH_INSTRUCTIONS_DURATION_TICKS = Math.max(0, config.domainClash.instructionsDurationTicks);
		DOMAIN_CLASH_INSTRUCTIONS_FADE_OUT_TICKS = Math.max(0, config.domainClash.instructionsFadeOutTicks);
		DOMAIN_CLASH_DAMAGE_TO_WIN = Math.max(1.0, config.domainClash.damageToWin);
		DOMAIN_CLASH_LOSER_MANA_DRAIN_PERCENT = MathHelper.clamp(config.domainClash.loserManaDrainPercent, 0, 100);
		DOMAIN_CLASH_POST_CLASH_COOLDOWN_MULTIPLIER = MathHelper.clamp(config.domainClash.postClashDomainCooldownMultiplier, 0.0, 10.0);
		DOMAIN_CLASH_PARTICLES_PER_TICK = Math.max(0, config.domainClash.particlesPerTick);
		DOMAIN_CLASH_SPLIT_PATTERN_MODULO = Math.max(2, config.domainClash.splitPatternModulo);
		DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS = config.domainClash.disableDomainEffectsDuringClash;
		DOMAIN_CLASH_FORCE_LOOK = config.domainClash.forceLookAtOpponent;
		DOMAIN_CLASH_PARTICIPANTS_INVINCIBLE = config.domainClash.participantsInvincible;
	}

	public static void onAbilityRequested(ServerPlayerEntity player, int abilitySlot) {
		if (isControlLocked(player)) {
			debugManipulation("{} ability request ignored: player is control-locked", debugName(player));
			return;
		}

		if (!MagicPlayerData.hasMagic(player)) {
			player.sendMessage(Text.translatable("message.magic.no_access"), true);
			return;
		}

		if (isMagicSuppressed(player)) {
			player.sendMessage(Text.translatable("message.magic.empty_embrace.magic_blocked"), true);
			return;
		}

		MagicSchool school = MagicPlayerData.getSchool(player);
		MagicAbility requestedAbility = MagicAbility.fromSlotForSchool(abilitySlot, school);
		if (requestedAbility == MagicAbility.NONE) {
			player.sendMessage(Text.translatable("message.magic.ability.not_implemented", abilitySlot), true);
			return;
		}

		UUID playerId = player.getUuid();
		DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.get(playerId);
		boolean inDomainClash = domainClashStateForParticipant(playerId) != null;
		boolean ownsLoveDomain = ownsLoveDomain(playerId);
		if (inDomainClash && isDomainExpansion(requestedAbility)) {
			player.sendMessage(Text.translatable("message.magic.domain.clash.active"), true);
			return;
		}

		if (ownedDomain != null && requestedAbility == ownedDomain.ability && !inDomainClash) {
			deactivateDomainExpansion(player);
			setActiveAbility(player, MagicAbility.NONE);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", ownedDomain.ability.displayName()), true);
			return;
		}

		if (ownedDomain != null && !inDomainClash && !ownsLoveDomain) {
			player.sendMessage(Text.translatable("message.magic.domain.already_active", ownedDomain.ability.displayName()), true);
			return;
		}

		MagicAbility currentActiveAbility = activeAbility(player);
		if (currentActiveAbility == MagicAbility.LOVE_DOMAIN_EXPANSION && requestedAbility != MagicAbility.LOVE_DOMAIN_EXPANSION && !inDomainClash && !ownsLoveDomain) {
			player.sendMessage(Text.translatable("message.magic.love_domain.abilities_locked"), true);
			return;
		}

		if (currentActiveAbility == MagicAbility.FROST_DOMAIN_EXPANSION && !isDomainExpansion(requestedAbility) && !inDomainClash) {
			deactivateDomainExpansion(player);
			setActiveAbility(player, MagicAbility.NONE);
			currentActiveAbility = MagicAbility.NONE;
		}

		if (currentActiveAbility == MagicAbility.PLANCK_HEAT && !isDomainExpansion(requestedAbility)) {
			player.sendMessage(Text.translatable("message.magic.ability.planck_heat_locked"), true);
			return;
		}

		if (!MagicConfig.get().abilityAccess.isAbilityUnlocked(playerId, requestedAbility)) {
			player.sendMessage(Text.translatable("message.magic.ability.locked", requestedAbility.displayName()), true);
			return;
		}

		int currentTick = player.getEntityWorld().getServer().getTicks();
		if (requestedAbility == MagicAbility.MANIPULATION && isManipulationRequestDebounced(player, currentTick)) {
			return;
		}

		if (isDomainExpansion(requestedAbility)) {
			handleDomainExpansionRequest(player, requestedAbility, currentTick);
			return;
		}

		if (requestedAbility == MagicAbility.BELOW_FREEZING) {
			handleBelowFreezingRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.ABSOLUTE_ZERO) {
			handleAbsoluteZeroRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.PLANCK_HEAT) {
			handlePlanckHeatRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			handleLoveAtFirstSightRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			handleTillDeathDoUsPartRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.MANIPULATION) {
			handleManipulationRequest(player);
			return;
		}

		player.sendMessage(Text.translatable("message.magic.ability.not_implemented", abilitySlot), true);
	}

	private static void handleBelowFreezingRequest(ServerPlayerEntity player) {
		MagicAbility activeAbility = activeAbility(player);

		if (activeAbility == MagicAbility.ABSOLUTE_ZERO) {
			deactivateAbsoluteZero(player);
			setActiveAbility(player, MagicAbility.NONE);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.ABSOLUTE_ZERO.displayName()), true);
			return;
		}

		if (activeAbility == MagicAbility.BELOW_FREEZING) {
			setActiveAbility(player, MagicAbility.NONE);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.BELOW_FREEZING.displayName()), true);
			return;
		}

		if (MagicPlayerData.getMana(player) <= 0) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		setActiveAbility(player, MagicAbility.BELOW_FREEZING);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.BELOW_FREEZING.displayName()), true);
	}

	private static void handleAbsoluteZeroRequest(ServerPlayerEntity player) {
		MagicAbility activeAbility = activeAbility(player);
		int currentTick = player.getEntityWorld().getServer().getTicks();

		if (activeAbility == MagicAbility.ABSOLUTE_ZERO) {
			deactivateAbsoluteZero(player);

			if (MagicPlayerData.getMana(player) > 0) {
				setActiveAbility(player, MagicAbility.BELOW_FREEZING);
			} else {
				setActiveAbility(player, MagicAbility.NONE);
			}

			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.ABSOLUTE_ZERO.displayName()), true);
			return;
		}

		if (activeAbility != MagicAbility.BELOW_FREEZING) {
			player.sendMessage(Text.translatable("message.magic.ability.requires_below_freezing"), true);
			return;
		}

		int remainingTicks = absoluteZeroCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			int secondsRemaining = (int) Math.ceil(remainingTicks / 20.0);
			player.sendMessage(
				Text.translatable("message.magic.ability.cooldown", MagicAbility.ABSOLUTE_ZERO.displayName(), secondsRemaining),
				true
			);
			return;
		}

		if (MagicPlayerData.getMana(player) <= 0) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		setActiveAbility(player, MagicAbility.ABSOLUTE_ZERO);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.ABSOLUTE_ZERO.displayName()), true);
	}

	private static void handlePlanckHeatRequest(ServerPlayerEntity player) {
		MagicAbility activeAbility = activeAbility(player);
		if (activeAbility != MagicAbility.ABSOLUTE_ZERO) {
			player.sendMessage(Text.translatable("message.magic.ability.requires_absolute_zero"), true);
			return;
		}

		int currentTick = player.getEntityWorld().getServer().getTicks();
		int remainingTicks = planckHeatCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			int secondsRemaining = (int) Math.ceil(remainingTicks / 20.0);
			player.sendMessage(
				Text.translatable("message.magic.ability.cooldown", MagicAbility.PLANCK_HEAT.displayName(), secondsRemaining),
				true
			);
			return;
		}

		int mana = MagicPlayerData.getMana(player);
		if (mana < PLANCK_HEAT_ACTIVATION_MANA_COST) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		int frostPhaseEndTick = currentTick + PLANCK_HEAT_FROST_PHASE_TICKS;
		int firePhaseEndTick = frostPhaseEndTick + PLANCK_HEAT_FIRE_PHASE_TICKS;
		UUID playerId = player.getUuid();

		deactivateAbsoluteZero(player);
		MagicPlayerData.setMana(player, mana - PLANCK_HEAT_ACTIVATION_MANA_COST);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		setActiveAbility(player, MagicAbility.PLANCK_HEAT);

		PLANCK_HEAT_COOLDOWN_END_TICK.put(playerId, currentTick + PLANCK_HEAT_COOLDOWN_TICKS);
		PLANCK_HEAT_STATES.put(playerId, new PlanckHeatState(frostPhaseEndTick, firePhaseEndTick));
		PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.remove(playerId);
		applyPlanckHeatCasterBuffs(player);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.PLANCK_HEAT.displayName()), true);
	}

	private static void handleLoveAtFirstSightRequest(ServerPlayerEntity player) {
		MagicAbility activeAbility = activeAbility(player);
		if (activeAbility == MagicAbility.MANIPULATION) {
			deactivateManipulation(player, true, "switched to Love At First Sight");
		}

		if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.MANUAL_CANCEL, false);
		}

		if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			deactivateLoveAtFirstSight(player);
			setActiveAbility(player, MagicAbility.NONE);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.LOVE_AT_FIRST_SIGHT.displayName()), true);
			return;
		}

		if (MagicPlayerData.getMana(player) <= 0) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		setActiveAbility(player, MagicAbility.LOVE_AT_FIRST_SIGHT);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		LOVE_POWER_ACTIVE_THIS_SECOND.put(player.getUuid(), false);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.LOVE_AT_FIRST_SIGHT.displayName()), true);
	}

	private static void handleTillDeathDoUsPartRequest(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		int currentTick = player.getEntityWorld().getServer().getTicks();
		if (TILL_DEATH_DO_US_PART_STATES.containsKey(playerId)) {
			deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.MANUAL_CANCEL, true);
			return;
		}

		if (TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.contains(playerId)) {
			TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.remove(playerId);
			player.sendMessage(
				Text.translatable("message.magic.ability.passive_disabled", MagicAbility.TILL_DEATH_DO_US_PART.displayName()),
				false
			);
			return;
		}

		TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.add(playerId);
		player.sendMessage(
			Text.translatable("message.magic.ability.passive_enabled", MagicAbility.TILL_DEATH_DO_US_PART.displayName()),
			false
		);
		int remainingTicks = tillDeathDoUsPartCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.TILL_DEATH_DO_US_PART, remainingTicks, false);
		}
	}

	private static void handleManipulationRequest(ServerPlayerEntity player) {
		MagicAbility activeAbility = activeAbility(player);
		int currentTick = player.getEntityWorld().getServer().getTicks();
		debugManipulation(
			"{} empty embrace request: activeAbility={}, mana={}, cooldownRemainingTicks={}",
			debugName(player),
			activeAbility,
			MagicPlayerData.getMana(player),
			manipulationCooldownRemaining(player, currentTick)
		);

		if (activeAbility == MagicAbility.MANIPULATION) {
			debugManipulation("{} empty embrace toggle requested while active; deactivating", debugName(player));
			deactivateManipulation(player, true, "manual toggle via ability key");
			setActiveAbility(player, MagicAbility.NONE);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.MANIPULATION.displayName()), true);
			return;
		}

		if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			debugManipulation("{} empty embrace request: deactivating prior LOVE_AT_FIRST_SIGHT", debugName(player));
			deactivateLoveAtFirstSight(player);
		}

		if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			debugManipulation("{} empty embrace request: deactivating prior TILL_DEATH_DO_US_PART", debugName(player));
			deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.MANUAL_CANCEL, false);
		}

		int remainingTicks = manipulationCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			int secondsRemaining = (int) Math.ceil(remainingTicks / 20.0);
			player.sendMessage(
				Text.translatable("message.magic.ability.cooldown", MagicAbility.MANIPULATION.displayName(), secondsRemaining),
				true
			);
			debugManipulation("{} empty embrace denied: cooldown {} ticks", debugName(player), remainingTicks);
			return;
		}

		if (MagicPlayerData.getMana(player) < MANIPULATION_ACTIVATION_MANA_COST) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			debugManipulation("{} empty embrace denied: activation cost {}", debugName(player), MANIPULATION_ACTIVATION_MANA_COST);
			return;
		}

		if (MANIPULATION_ACTIVATION_MANA_COST > 0) {
			MagicPlayerData.setMana(player, Math.max(0, MagicPlayerData.getMana(player) - MANIPULATION_ACTIVATION_MANA_COST));
		}

		setActiveAbility(player, MagicAbility.MANIPULATION);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		MANIPULATION_STATES.put(player.getUuid(), ManipulationState.waiting(currentTick, new Vec3d(player.getX(), player.getY(), player.getZ())));
		player.sendMessage(Text.translatable("message.magic.empty_embrace.waiting"), true);
		tryAcquireManipulationTarget(player, currentTick);
	}

	private static void tryAcquireManipulationTarget(ServerPlayerEntity caster, int currentTick) {
		ManipulationState state = MANIPULATION_STATES.get(caster.getUuid());
		if (state == null || state.stage != ManipulationStage.WAITING) {
			return;
		}

		ServerPlayerEntity target = findPlayerTargetInLineOfSight(caster, MANIPULATION_ACQUIRE_RANGE);
		if (target == null) {
			return;
		}

		UUID existingCasterId = MANIPULATION_CASTER_BY_TARGET.get(target.getUuid());
		if (existingCasterId != null && !existingCasterId.equals(caster.getUuid())) {
			caster.sendMessage(Text.translatable("message.magic.ability.target_controlled"), true);
			deactivateManipulation(caster, true, "target already affected by empty embrace");
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		if (targetHasActiveDomain(target)) {
			caster.sendMessage(Text.translatable("message.magic.empty_embrace.domain_overwhelmed", target.getDisplayName()), true);
			deactivateManipulation(caster, true, "target domain overwhelmed empty embrace");
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		activateManipulation(caster, target, currentTick);
	}

	private static boolean targetHasActiveDomain(ServerPlayerEntity target) {
		return DOMAIN_EXPANSIONS.containsKey(target.getUuid()) || isDomainExpansion(activeAbility(target));
	}

	private static void activateManipulation(ServerPlayerEntity caster, ServerPlayerEntity target, int currentTick) {
		UUID casterId = caster.getUuid();
		ManipulationState state = MANIPULATION_STATES.computeIfAbsent(
			casterId,
			ignored -> ManipulationState.waiting(currentTick, new Vec3d(caster.getX(), caster.getY(), caster.getZ()))
		);
		state.stage = ManipulationStage.LINKED;
		state.targetDimension = target.getEntityWorld().getRegistryKey();
		state.targetId = target.getUuid();
		state.lockedCasterPos = new Vec3d(caster.getX(), caster.getY(), caster.getZ());
		state.controlledPos = new Vec3d(target.getX(), target.getY(), target.getZ());
		MANIPULATION_CASTER_BY_TARGET.put(target.getUuid(), casterId);
		if (MANIPULATION_DEACTIVATE_TARGET_MAGIC) {
			cancelMagicOnTarget(target, currentTick);
		}
		applyManipulationSuppressionTags(target);
		debugManipulation(
			"{} empty embrace locked target {} ({}) at [{}, {}, {}]",
			debugName(caster),
			target.getName().getString(),
			target.getUuid(),
			round3(target.getX()),
			round3(target.getY()),
			round3(target.getZ())
		);
		caster.sendMessage(Text.translatable("message.magic.empty_embrace.locked", target.getDisplayName()), true);
		target.sendMessage(Text.translatable("message.magic.empty_embrace.target_status", caster.getDisplayName()), true);
	}

	private static void cancelMagicOnTarget(ServerPlayerEntity target, int currentTick) {
		MagicAbility targetAbility = activeAbility(target);
		if (targetAbility == MagicAbility.ABSOLUTE_ZERO) {
			deactivateAbsoluteZero(target);
		}
		if (targetAbility == MagicAbility.PLANCK_HEAT) {
			deactivatePlanckHeat(target);
		}
		if (targetAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			deactivateLoveAtFirstSight(target);
		}
		if (targetAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			deactivateTillDeathDoUsPart(target, TillDeathDoUsPartEndReason.MANUAL_CANCEL, true);
		}
		if (targetAbility == MagicAbility.MANIPULATION) {
			deactivateManipulation(target, true, "suppressed by empty embrace");
		}

		setActiveAbility(target, MagicAbility.NONE);
		LOVE_LOCKED_TARGETS.remove(target.getUuid());

		List<UUID> linkedTillDeathCasters = new ArrayList<>();
		for (Map.Entry<UUID, TillDeathDoUsPartState> entry : TILL_DEATH_DO_US_PART_STATES.entrySet()) {
			if (target.getUuid().equals(entry.getValue().linkedPlayerId)) {
				linkedTillDeathCasters.add(entry.getKey());
			}
		}
		for (UUID casterId : linkedTillDeathCasters) {
			ServerPlayerEntity linkedCaster = target.getEntityWorld().getServer().getPlayerManager().getPlayer(casterId);
			if (linkedCaster != null) {
				deactivateTillDeathDoUsPart(linkedCaster, TillDeathDoUsPartEndReason.LINK_TARGET_INVALID, false);
			} else {
				TILL_DEATH_DO_US_PART_STATES.remove(casterId);
				startTillDeathDoUsPartCooldown(casterId, currentTick);
			}
		}
	}

	private static void handleDomainExpansionRequest(ServerPlayerEntity player, MagicAbility requestedAbility, int currentTick) {
		MagicAbility activeAbility = activeAbility(player);
		if (isDomainExpansion(activeAbility)) {
			deactivateDomainExpansion(player);
			setActiveAbility(player, MagicAbility.NONE);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", activeAbility.displayName()), true);
			return;
		}

		int remainingTicks = domainCooldownRemaining(player, requestedAbility, currentTick);
		if (remainingTicks > 0) {
			int secondsRemaining = (int) Math.ceil(remainingTicks / 20.0);
			player.sendMessage(
				Text.translatable("message.magic.ability.cooldown", requestedAbility.displayName(), secondsRemaining),
				true
			);
			return;
		}

		int mana = MagicPlayerData.getMana(player);
		if (mana < DOMAIN_EXPANSION_ACTIVATION_MANA_COST) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		DomainOwnerState containingDomain = findContainingForeignDomain(player);
		if (containingDomain != null) {
			if (DOMAIN_CLASHES_BY_OWNER.containsKey(containingDomain.ownerId)) {
				player.sendMessage(Text.translatable("message.magic.domain.clash.active"), true);
				return;
			}

			if (containingDomain.state.clashOccurred) {
				player.sendMessage(Text.translatable("message.magic.domain.clash.used"), true);
				return;
			}

			if (!DOMAIN_CLASH_ENABLED) {
				player.sendMessage(Text.translatable("message.magic.domain.too_close", DOMAIN_CLASH_MIN_EXTERIOR_DISTANCE), true);
				return;
			}

			int ticksSinceActivation = Math.max(0, currentTick - containingDomain.state.activationTick);
			if (ticksSinceActivation <= DOMAIN_CLASH_SIMULTANEOUS_WINDOW_TICKS) {
				resolveSimultaneousDomainCast(player, requestedAbility, currentTick, containingDomain.ownerId, containingDomain.state);
				return;
			}

			prepareForDomainActivation(player, activeAbility);
			if (startDomainClash(player, requestedAbility, containingDomain.ownerId, containingDomain.state, currentTick)) {
				MagicPlayerData.setMana(player, mana - DOMAIN_EXPANSION_ACTIVATION_MANA_COST);
				MagicPlayerData.setDepletedRecoveryMode(player, false);
				setActiveAbility(player, requestedAbility);
				return;
			}

			player.sendMessage(Text.translatable("message.magic.domain.clash.failed"), true);
			return;
		}

		if (isTooCloseToForeignDomainExterior(player, DOMAIN_CLASH_MIN_EXTERIOR_DISTANCE)) {
			player.sendMessage(Text.translatable("message.magic.domain.too_close", DOMAIN_CLASH_MIN_EXTERIOR_DISTANCE), true);
			return;
		}

		prepareForDomainActivation(player, activeAbility);
		activateDomainExpansion(player, requestedAbility, currentTick);
		MagicPlayerData.setMana(player, mana - DOMAIN_EXPANSION_ACTIVATION_MANA_COST);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		setActiveAbility(player, requestedAbility);
		player.sendMessage(Text.translatable("message.magic.ability.activated", requestedAbility.displayName()), true);
	}

	private static void prepareForDomainActivation(ServerPlayerEntity player, MagicAbility activeAbility) {
		if (activeAbility == MagicAbility.ABSOLUTE_ZERO) {
			deactivateAbsoluteZero(player);
		}
		if (activeAbility == MagicAbility.PLANCK_HEAT) {
			deactivatePlanckHeat(player);
		}
		if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			deactivateLoveAtFirstSight(player);
		}
		if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.MANUAL_CANCEL, false);
		}
		if (activeAbility == MagicAbility.MANIPULATION) {
			deactivateManipulation(player, true, "switched to Domain Expansion");
		}
	}

	private static DomainOwnerState findContainingForeignDomain(ServerPlayerEntity player) {
		RegistryKey<World> dimension = player.getEntityWorld().getRegistryKey();
		UUID playerId = player.getUuid();
		for (Map.Entry<UUID, DomainExpansionState> entry : DOMAIN_EXPANSIONS.entrySet()) {
			if (entry.getKey().equals(playerId)) {
				continue;
			}

			DomainExpansionState state = entry.getValue();
			if (!state.dimension.equals(dimension)) {
				continue;
			}

			if (isInsideDomainInterior(player, state)) {
				return new DomainOwnerState(entry.getKey(), state);
			}
		}

		return null;
	}

	private static boolean isTooCloseToForeignDomainExterior(ServerPlayerEntity player, int minimumDistance) {
		if (minimumDistance <= 0) {
			return false;
		}

		RegistryKey<World> dimension = player.getEntityWorld().getRegistryKey();
		UUID playerId = player.getUuid();
		for (Map.Entry<UUID, DomainExpansionState> entry : DOMAIN_EXPANSIONS.entrySet()) {
			if (entry.getKey().equals(playerId)) {
				continue;
			}

			DomainExpansionState state = entry.getValue();
			if (!state.dimension.equals(dimension) || isInsideDomainInterior(player, state)) {
				continue;
			}

			Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
			if (distanceToDomainExterior(playerPos, state) <= minimumDistance) {
				return true;
			}
		}

		return false;
	}

	private static boolean isInsideDomainInterior(ServerPlayerEntity player, DomainExpansionState state) {
		double dx = player.getX() - state.centerX;
		double dz = player.getZ() - state.centerZ;
		double horizontalDistanceSq = dx * dx + dz * dz;
		double relativeY = player.getY() - state.baseY;
		return isInsideDomainInterior(horizontalDistanceSq, relativeY, state.innerRadius, state.innerHeight);
	}

	private static double distanceToDomainExterior(Vec3d position, DomainExpansionState state) {
		double dx = position.x - state.centerX;
		double dz = position.z - state.centerZ;
		double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
		return Math.abs(horizontalDistance - state.radius);
	}

	private static void activateDomainExpansion(ServerPlayerEntity player, MagicAbility ability, int currentTick) {
		deactivateDomainExpansion(player);
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		int radius = Math.max(1, DOMAIN_EXPANSION_RADIUS);
		int height = Math.max(1, DOMAIN_EXPANSION_HEIGHT);
		int shellThickness = Math.max(1, DOMAIN_EXPANSION_SHELL_THICKNESS);
		int innerRadius = Math.max(0, radius - shellThickness);
		int innerHeight = Math.max(1, height - shellThickness);

		int centerX = MathHelper.floor(player.getX());
		int centerZ = MathHelper.floor(player.getZ());
		int baseY = MathHelper.floor(player.getY()) - 1;
		double centerDx = centerX + 0.5;
		double centerDz = centerZ + 0.5;

		Map<BlockPos, DomainSavedBlockState> savedBlocks = new HashMap<>();
		Map<BlockPos, BlockState> protectedShellStates = new HashMap<>();
		Map<UUID, DomainCapturedEntityState> capturedEntities = captureDomainEntities(world, centerDx, centerDz, baseY, radius, height);
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				int horizontalDistanceSq = dx * dx + dz * dz;
				if (horizontalDistanceSq > radius * radius) {
					continue;
				}

				for (int y = 0; y <= height; y++) {
					if (y > 0 && !isInsideDomainDome(horizontalDistanceSq, y, radius, height)) {
						continue;
					}

						boolean shell = y == 0 || !isInsideDomainDome(horizontalDistanceSq, y, innerRadius, innerHeight);

						mutablePos.set(centerX + dx, baseY + y, centerZ + dz);
						if (!world.isInBuildLimit(mutablePos)) {
							continue;
						}

						BlockPos immutablePos = mutablePos.toImmutable();
						BlockState targetState = resolveDomainTargetState(
							ability,
							shell,
							centerX,
							centerZ,
							baseY,
							radius,
							height,
							innerRadius,
							innerHeight,
							immutablePos
						);
						if (shell || targetState.isOf(Blocks.LIGHT)) {
							protectedShellStates.put(immutablePos, targetState);
						}

						BlockState currentState = world.getBlockState(mutablePos);
						BlockEntity blockEntity = world.getBlockEntity(mutablePos);
						NbtCompound blockEntityNbt = blockEntity == null ? null : blockEntity.createNbtWithIdentifyingData(world.getRegistryManager());
						// Snapshot every in-domain position so any mid-domain mutations (like vine spread) are restored.
						savedBlocks.put(immutablePos, new DomainSavedBlockState(currentState, blockEntityNbt));

						if (currentState.equals(targetState)) {
							continue;
						}

						if (!world.setBlockState(mutablePos, targetState, DOMAIN_BLOCK_PLACE_FLAGS)) {
							continue;
						}
					}
				}
			}

		moveCapturedEntitiesIntoDomain(
			world,
			capturedEntities.values(),
			centerDx,
			centerDz,
			baseY,
			innerRadius,
			height
		);
		playDomainActivationSounds(world, player);

		int expiresTick = ability == MagicAbility.LOVE_DOMAIN_EXPANSION
			? Integer.MAX_VALUE
			: currentTick + Math.max(TICKS_PER_SECOND, DOMAIN_EXPANSION_DURATION_TICKS);

		DomainExpansionState state = new DomainExpansionState(
			world.getRegistryKey(),
			ability,
			expiresTick,
			centerDx,
			centerDz,
			baseY,
			radius,
			height,
			innerRadius,
			innerHeight,
			currentTick,
			false,
			1.0,
			savedBlocks,
			protectedShellStates,
			capturedEntities
		);
		DOMAIN_EXPANSIONS.put(player.getUuid(), state);
		persistDomainRuntimeState(world.getServer());
	}

	private static boolean deactivateDomainExpansion(ServerPlayerEntity player) {
		DomainExpansionState state = DOMAIN_EXPANSIONS.remove(player.getUuid());
		if (state == null) {
			return false;
		}

		MinecraftServer server = player.getEntityWorld().getServer();
		cancelDomainClash(player.getUuid(), server);
		restoreDomainExpansion(server, state);
		startDomainCooldown(player.getUuid(), state.ability, server.getTicks(), state.cooldownMultiplier);
		persistDomainRuntimeState(server);
		applyDomainInstabilityPenalty(player);
		return true;
	}

	private static void restoreDomainExpansion(MinecraftServer server, DomainExpansionState state) {
		ServerWorld world = server.getWorld(state.dimension);
		if (world == null) {
			return;
		}

		for (Map.Entry<BlockPos, DomainSavedBlockState> entry : state.savedBlocks.entrySet()) {
			BlockPos pos = entry.getKey();
			DomainSavedBlockState saved = entry.getValue();
			if (!world.isInBuildLimit(pos)) {
				continue;
			}

			if (!world.setBlockState(pos, saved.blockState, DOMAIN_BLOCK_RESTORE_FLAGS)) {
				continue;
			}

			world.removeBlockEntity(pos);
			if (saved.blockEntityNbt == null || !saved.blockState.hasBlockEntity()) {
				continue;
			}

			BlockEntity restored = BlockEntity.createFromNbt(pos, saved.blockState, saved.blockEntityNbt.copy(), world.getRegistryManager());
			if (restored == null) {
				continue;
			}

			world.getWorldChunk(pos).setBlockEntity(restored);
			restored.markDirty();
		}

		restoreCapturedEntities(server, state);
	}

	private static void updateDomainExpansions(MinecraftServer server, int currentTick) {
		boolean changed = false;
		Iterator<Map.Entry<UUID, DomainExpansionState>> iterator = DOMAIN_EXPANSIONS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, DomainExpansionState> entry = iterator.next();
			DomainExpansionState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null) {
				continue;
			}

			UUID ownerId = entry.getKey();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(ownerId);
			boolean clashActive = DOMAIN_CLASHES_BY_OWNER.containsKey(ownerId);
			boolean expired = !clashActive && currentTick >= state.expiresTick;

			if (!expired) {
				if (caster != null && caster.isAlive() && activeAbility(caster) != state.ability) {
					setActiveAbility(caster, state.ability);
				}
				if (!(clashActive && DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS)) {
					enforceCapturedEntitiesInsideDomain(world, state);
				}
				maintainDomainShell(world, state);
				continue;
			}

			cancelDomainClash(ownerId, server);
			restoreDomainExpansion(server, state);
			startDomainCooldown(ownerId, state.ability, currentTick, state.cooldownMultiplier);
			iterator.remove();
			changed = true;

			if (caster != null && caster.isAlive() && activeAbility(caster) == state.ability) {
				MagicPlayerData.setActiveAbilitySlot(caster, MagicAbility.NONE.slot());
				caster.sendMessage(Text.translatable("message.magic.ability.deactivated", state.ability.displayName()), true);
			}

			if (caster != null && caster.isAlive()) {
				applyDomainInstabilityPenalty(caster);
			}
		}

		if (changed) {
			persistDomainRuntimeState(server);
		}
	}

	private static void resolveSimultaneousDomainCast(
		ServerPlayerEntity challenger,
		MagicAbility challengerAbility,
		int currentTick,
		UUID ownerId,
		DomainExpansionState ownerState
	) {
		MinecraftServer server = challenger.getEntityWorld().getServer();
		if (server == null) {
			return;
		}

		MagicPlayerData.setMana(challenger, Math.max(0, MagicPlayerData.getMana(challenger) - DOMAIN_EXPANSION_ACTIVATION_MANA_COST));
		MagicPlayerData.setDepletedRecoveryMode(challenger, false);
		setActiveAbility(challenger, MagicAbility.NONE);
		startDomainCooldown(challenger.getUuid(), challengerAbility, currentTick, 1.0);

		DOMAIN_EXPANSIONS.remove(ownerId);
		cancelDomainClash(ownerId, server);
		restoreDomainExpansion(server, ownerState);
		startDomainCooldown(ownerId, ownerState.ability, currentTick, 1.0);

		ServerPlayerEntity ownerPlayer = server.getPlayerManager().getPlayer(ownerId);
		if (ownerPlayer != null && activeAbility(ownerPlayer) == ownerState.ability) {
			setActiveAbility(ownerPlayer, MagicAbility.NONE);
		}

		if (ownerPlayer != null) {
			ownerPlayer.sendMessage(Text.translatable("message.magic.domain.clash.simultaneous"), true);
		}
		challenger.sendMessage(Text.translatable("message.magic.domain.clash.simultaneous"), true);
		persistDomainRuntimeState(server);
	}

	private static boolean startDomainClash(
		ServerPlayerEntity challenger,
		MagicAbility challengerAbility,
		UUID ownerId,
		DomainExpansionState state,
		int currentTick
	) {
		if (!(challenger.getEntityWorld() instanceof ServerWorld world)) {
			return false;
		}

		MinecraftServer server = world.getServer();
		if (server == null || DOMAIN_CLASHES_BY_OWNER.containsKey(ownerId)) {
			return false;
		}

		ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerId);
		if (owner == null || !owner.isAlive() || owner.isSpectator()) {
			return false;
		}

		state.clashOccurred = true;
		int titleEndTick = currentTick + domainClashTitleDurationTicks();
		int instructionsFadeStartTick = titleEndTick + DOMAIN_CLASH_INSTRUCTIONS_DURATION_TICKS;
		int combatStartTick = instructionsFadeStartTick + DOMAIN_CLASH_INSTRUCTIONS_FADE_OUT_TICKS;
		state.expiresTick = Math.max(state.expiresTick, currentTick + domainClashIntroLockTicks() + TICKS_PER_SECOND);
		DomainClashState clashState = new DomainClashState(
			ownerId,
			state.ability,
			challenger.getUuid(),
			challengerAbility,
			new Vec3d(owner.getX(), owner.getY(), owner.getZ()),
			new Vec3d(challenger.getX(), challenger.getY(), challenger.getZ()),
			currentTick,
			titleEndTick,
			instructionsFadeStartTick,
			combatStartTick
		);

		DOMAIN_CLASHES_BY_OWNER.put(ownerId, clashState);
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.put(ownerId, ownerId);
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.put(challenger.getUuid(), ownerId);

		MagicPlayerData.setDomainClashActive(owner, true);
		MagicPlayerData.setDomainClashProgress(owner, 0);
		MagicPlayerData.setDomainClashPromptKey(owner, 0);
		MagicPlayerData.setDomainClashInstructionVisibility(owner, 0);
		MagicPlayerData.setDomainClashActive(challenger, true);
		MagicPlayerData.setDomainClashProgress(challenger, 0);
		MagicPlayerData.setDomainClashPromptKey(challenger, 0);
		MagicPlayerData.setDomainClashInstructionVisibility(challenger, 0);

		showDomainClashTitle(owner);
		showDomainClashTitle(challenger);
		applySplitDomainInteriorVisuals(world, state, state.ability, challengerAbility);
		applyDomainClashStartEffects(owner);
		applyDomainClashStartEffects(challenger);
		owner.sendMessage(Text.translatable("message.magic.domain.clash.started"), true);
		challenger.sendMessage(Text.translatable("message.magic.domain.clash.started"), true);
		persistDomainRuntimeState(server);
		return true;
	}

	private static void updateDomainClashes(MinecraftServer server, int currentTick) {
		List<DomainClashResolution> pendingResolutions = new ArrayList<>();
		Iterator<Map.Entry<UUID, DomainClashState>> iterator = DOMAIN_CLASHES_BY_OWNER.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, DomainClashState> entry = iterator.next();
			UUID ownerId = entry.getKey();
			DomainClashState clash = entry.getValue();
			DomainExpansionState domain = DOMAIN_EXPANSIONS.get(ownerId);
			if (domain == null) {
				clearDomainClashUiFor(clash, server);
				DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(clash.ownerId);
				DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(clash.challengerId);
				iterator.remove();
				continue;
			}

			ServerWorld world = server.getWorld(domain.dimension);
			if (world == null) {
				continue;
			}

			ServerPlayerEntity owner = server.getPlayerManager().getPlayer(clash.ownerId);
			ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(clash.challengerId);
			if (owner == null || !owner.isAlive() || owner.isSpectator()) {
				pendingResolutions.add(new DomainClashResolution(ownerId, clash.challengerId));
				continue;
			}
			if (challenger == null || !challenger.isAlive() || challenger.isSpectator()) {
				pendingResolutions.add(new DomainClashResolution(ownerId, clash.ownerId));
				continue;
			}

			if (isDomainClashFrozen(clash, currentTick)) {
				lockDomainClashParticipant(owner, clash.ownerLockedPos, challenger.getEyePos());
				lockDomainClashParticipant(challenger, clash.challengerLockedPos, owner.getEyePos());
			}
			applyDomainClashCombatEffects(owner);
			applyDomainClashCombatEffects(challenger);
			spawnDomainClashParticles(world, domain, DOMAIN_CLASH_PARTICLES_PER_TICK);
			int ownerProgressPercent = domainClashProgressPercent(clash.ownerDamageDealt);
			int challengerProgressPercent = domainClashProgressPercent(clash.challengerDamageDealt);
			int instructionVisibilityPercent = domainClashInstructionVisibilityPercent(clash, currentTick);

			MagicPlayerData.setDomainClashActive(owner, true);
			MagicPlayerData.setDomainClashProgress(owner, ownerProgressPercent);
			MagicPlayerData.setDomainClashPromptKey(owner, 0);
			MagicPlayerData.setDomainClashInstructionVisibility(owner, instructionVisibilityPercent);
			MagicPlayerData.setDomainClashActive(challenger, true);
			MagicPlayerData.setDomainClashProgress(challenger, challengerProgressPercent);
			MagicPlayerData.setDomainClashPromptKey(challenger, 0);
			MagicPlayerData.setDomainClashInstructionVisibility(challenger, instructionVisibilityPercent);
		}

		for (DomainClashResolution resolution : pendingResolutions) {
			resolveDomainClash(server, resolution.ownerId, resolution.winnerId, currentTick);
		}
	}

	public static void onDomainClashInput(ServerPlayerEntity player, int keyCode) {
	}

	private static void resolveDomainClash(MinecraftServer server, UUID ownerId, UUID winnerId, int currentTick) {
		DomainClashState clash = DOMAIN_CLASHES_BY_OWNER.remove(ownerId);
		if (clash == null) {
			return;
		}

		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(clash.ownerId);
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(clash.challengerId);

		DomainExpansionState domain = DOMAIN_EXPANSIONS.get(ownerId);
		ServerPlayerEntity owner = server.getPlayerManager().getPlayer(clash.ownerId);
		ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(clash.challengerId);

		if (owner != null) {
			MagicPlayerData.clearDomainClashUi(owner);
		}
		if (challenger != null) {
			MagicPlayerData.clearDomainClashUi(challenger);
		}

		if (domain == null) {
			return;
		}

		ServerWorld world = server.getWorld(domain.dimension);
		if (world == null) {
			return;
		}

		boolean challengerWon = winnerId.equals(clash.challengerId);

		if (challengerWon) {
			applyDomainVisualForAbility(world, domain, clash.challengerAbility);
			int expiresTick = clash.challengerAbility == MagicAbility.LOVE_DOMAIN_EXPANSION
				? Integer.MAX_VALUE
				: currentTick + Math.max(TICKS_PER_SECOND, DOMAIN_EXPANSION_DURATION_TICKS);
			domain.ability = clash.challengerAbility;
			domain.expiresTick = expiresTick;
			domain.activationTick = currentTick;
			domain.cooldownMultiplier = DOMAIN_CLASH_POST_CLASH_COOLDOWN_MULTIPLIER;
			DOMAIN_EXPANSIONS.remove(ownerId);
			DOMAIN_EXPANSIONS.put(clash.challengerId, domain);
			startDomainCooldown(clash.ownerId, clash.ownerAbility, currentTick, DOMAIN_CLASH_POST_CLASH_COOLDOWN_MULTIPLIER);
			if (owner != null) {
				setActiveAbility(owner, MagicAbility.NONE);
				applyDomainClashLoserManaPenalty(owner);
				owner.sendMessage(Text.translatable("message.magic.domain.clash.lost"), true);
			}
			if (challenger != null) {
				setActiveAbility(challenger, clash.challengerAbility);
				challenger.sendMessage(Text.translatable("message.magic.domain.clash.won"), true);
			}
		} else {
			applyDomainVisualForAbility(world, domain, clash.ownerAbility);
			domain.cooldownMultiplier = DOMAIN_CLASH_POST_CLASH_COOLDOWN_MULTIPLIER;
			startDomainCooldown(clash.challengerId, clash.challengerAbility, currentTick, DOMAIN_CLASH_POST_CLASH_COOLDOWN_MULTIPLIER);
			if (challenger != null) {
				setActiveAbility(challenger, MagicAbility.NONE);
				applyDomainClashLoserManaPenalty(challenger);
				challenger.sendMessage(Text.translatable("message.magic.domain.clash.lost"), true);
			}
			if (owner != null) {
				setActiveAbility(owner, clash.ownerAbility);
				owner.sendMessage(Text.translatable("message.magic.domain.clash.won"), true);
			}
		}

		persistDomainRuntimeState(server);
	}

	private static void clearDomainClashUiFor(DomainClashState clash, MinecraftServer server) {
		ServerPlayerEntity owner = server.getPlayerManager().getPlayer(clash.ownerId);
		ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(clash.challengerId);
		if (owner != null) {
			MagicPlayerData.clearDomainClashUi(owner);
		}
		if (challenger != null) {
			MagicPlayerData.clearDomainClashUi(challenger);
		}
	}

	private static void cancelDomainClash(UUID ownerId, MinecraftServer server) {
		DomainClashState clash = DOMAIN_CLASHES_BY_OWNER.remove(ownerId);
		if (clash == null) {
			return;
		}

		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(clash.ownerId);
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(clash.challengerId);
		clearDomainClashUiFor(clash, server);
	}

	private static void cancelDomainClashParticipant(UUID playerId, MinecraftServer server) {
		UUID ownerId = DOMAIN_CLASH_OWNER_BY_PARTICIPANT.get(playerId);
		if (ownerId == null) {
			return;
		}

		DomainClashState clash = DOMAIN_CLASHES_BY_OWNER.get(ownerId);
		if (clash == null) {
			DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(playerId);
			return;
		}

		UUID winnerId = playerId.equals(clash.ownerId) ? clash.challengerId : clash.ownerId;
		resolveDomainClash(server, ownerId, winnerId, server.getTicks());
	}

	private static void applyDomainClashLoserManaPenalty(ServerPlayerEntity loser) {
		int manaDrain = (int) Math.ceil(MagicPlayerData.MAX_MANA * (DOMAIN_CLASH_LOSER_MANA_DRAIN_PERCENT / 100.0));
		MagicPlayerData.setMana(loser, Math.max(0, MagicPlayerData.getMana(loser) - manaDrain));
	}

	private static int domainClashTitleDurationTicks() {
		return DOMAIN_CLASH_TITLE_FADE_IN_TICKS + DOMAIN_CLASH_TITLE_STAY_TICKS + DOMAIN_CLASH_TITLE_FADE_OUT_TICKS;
	}

	private static int domainClashIntroLockTicks() {
		return domainClashTitleDurationTicks() + DOMAIN_CLASH_INSTRUCTIONS_DURATION_TICKS + DOMAIN_CLASH_INSTRUCTIONS_FADE_OUT_TICKS;
	}

	private static boolean isDomainClashFrozen(DomainClashState clash, int currentTick) {
		return currentTick < clash.combatStartTick;
	}

	private static boolean isDomainClashCombatActive(DomainClashState clash, int currentTick) {
		return currentTick >= clash.combatStartTick;
	}

	private static int domainClashProgressPercent(double damageDealt) {
		return MathHelper.clamp((int) Math.round((damageDealt / DOMAIN_CLASH_DAMAGE_TO_WIN) * 100.0), 0, 100);
	}

	private static int domainClashInstructionVisibilityPercent(DomainClashState clash, int currentTick) {
		if (currentTick < clash.titleEndTick || currentTick >= clash.combatStartTick) {
			return 0;
		}

		if (currentTick < clash.instructionsFadeStartTick || DOMAIN_CLASH_INSTRUCTIONS_FADE_OUT_TICKS <= 0) {
			return 100;
		}

		int remainingFadeTicks = Math.max(0, clash.combatStartTick - currentTick);
		return MathHelper.clamp(
			(int) Math.round(remainingFadeTicks * 100.0 / Math.max(1, DOMAIN_CLASH_INSTRUCTIONS_FADE_OUT_TICKS)),
			0,
			100
		);
	}

	private static DomainClashState domainClashStateForParticipant(UUID playerId) {
		UUID ownerId = DOMAIN_CLASH_OWNER_BY_PARTICIPANT.get(playerId);
		if (ownerId == null) {
			return null;
		}

		return DOMAIN_CLASHES_BY_OWNER.get(ownerId);
	}

	private static boolean addDomainClashDamage(DomainClashState clash, UUID attackerId, float amount, MinecraftServer server, int currentTick) {
		UUID winnerId = null;
		if (attackerId.equals(clash.ownerId)) {
			clash.ownerDamageDealt = Math.min(DOMAIN_CLASH_DAMAGE_TO_WIN, clash.ownerDamageDealt + amount);
			if (clash.ownerDamageDealt >= DOMAIN_CLASH_DAMAGE_TO_WIN) {
				winnerId = clash.ownerId;
			}
		} else if (attackerId.equals(clash.challengerId)) {
			clash.challengerDamageDealt = Math.min(DOMAIN_CLASH_DAMAGE_TO_WIN, clash.challengerDamageDealt + amount);
			if (clash.challengerDamageDealt >= DOMAIN_CLASH_DAMAGE_TO_WIN) {
				winnerId = clash.challengerId;
			}
		}

		if (winnerId != null) {
			resolveDomainClash(server, clash.ownerId, winnerId, currentTick);
			return true;
		}

		return false;
	}

	private static boolean handleDomainClashDamage(ServerPlayerEntity damagedPlayer, DamageSource source, float amount) {
		DomainClashState clash = domainClashStateForParticipant(damagedPlayer.getUuid());
		if (clash == null) {
			return false;
		}

		MinecraftServer server = damagedPlayer.getEntityWorld().getServer();
		if (server == null || !isDomainClashCombatActive(clash, server.getTicks())) {
			return false;
		}

		Entity attacker = source.getAttacker();
		if (!(attacker instanceof ServerPlayerEntity attackerPlayer) || !attackerPlayer.isAlive() || attackerPlayer.isSpectator()) {
			return false;
		}

		UUID damagedId = damagedPlayer.getUuid();
		UUID attackerId = attackerPlayer.getUuid();
		boolean ownerHitChallenger = attackerId.equals(clash.ownerId) && damagedId.equals(clash.challengerId);
		boolean challengerHitOwner = attackerId.equals(clash.challengerId) && damagedId.equals(clash.ownerId);
		if (!ownerHitChallenger && !challengerHitOwner) {
			return false;
		}

		boolean resolved = addDomainClashDamage(clash, attackerId, amount, server, server.getTicks());
		return resolved || DOMAIN_CLASH_PARTICIPANTS_INVINCIBLE;
	}

	private static void lockDomainClashParticipant(ServerPlayerEntity player, Vec3d lockedPos, Vec3d opponentEyePos) {
		float yaw = player.getYaw();
		float pitch = player.getPitch();
		if (DOMAIN_CLASH_FORCE_LOOK) {
			float[] facing = computeFacingAngles(player, opponentEyePos);
			yaw = facing[0];
			pitch = facing[1];
		}

		player.setVelocity(0.0, 0.0, 0.0);
		player.networkHandler.requestTeleport(lockedPos.x, lockedPos.y, lockedPos.z, yaw, pitch);
	}

	private static void spawnDomainClashParticles(ServerWorld world, DomainExpansionState state, int particlesPerTick) {
		if (particlesPerTick <= 0) {
			return;
		}

		double centerY = state.baseY + (state.height * 0.5);
		world.spawnParticles(
			ParticleTypes.END_ROD,
			state.centerX,
			centerY,
			state.centerZ,
			particlesPerTick,
			state.innerRadius * 0.65,
			state.height * 0.35,
			state.innerRadius * 0.65,
			0.02
		);
	}

	private static void showDomainClashTitle(ServerPlayerEntity player) {
		player.networkHandler.sendPacket(
			new TitleFadeS2CPacket(DOMAIN_CLASH_TITLE_FADE_IN_TICKS, DOMAIN_CLASH_TITLE_STAY_TICKS, DOMAIN_CLASH_TITLE_FADE_OUT_TICKS)
		);
		player.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("title.magic.domain_clash").formatted(Formatting.BOLD)));
	}

	private static void applySplitDomainInteriorVisuals(
		ServerWorld world,
		DomainExpansionState state,
		MagicAbility ownerAbility,
		MagicAbility challengerAbility
	) {
		int centerX = MathHelper.floor(state.centerX);
		int centerZ = MathHelper.floor(state.centerZ);
		int centerLineX = MathHelper.floor(state.centerX);
		int modulo = Math.max(2, DOMAIN_CLASH_SPLIT_PATTERN_MODULO);

		for (BlockPos pos : state.savedBlocks.keySet()) {
			int relativeY = pos.getY() - state.baseY;
			if (relativeY <= 0 || relativeY > state.height) {
				continue;
			}

			int horizontalDistanceSq = horizontalDistanceSq(pos.getX(), centerX, pos.getZ(), centerZ);
			if (!isInsideDomainDome(horizontalDistanceSq, relativeY, state.radius, state.height)) {
				continue;
			}

			boolean shell = relativeY == 0 || !isInsideDomainDome(horizontalDistanceSq, relativeY, state.innerRadius, state.innerHeight);
			if (shell) {
				continue;
			}

			MagicAbility targetAbility;
			if (pos.getX() < centerLineX) {
				targetAbility = ownerAbility;
			} else if (pos.getX() > centerLineX) {
				targetAbility = challengerAbility;
			} else {
				// Split the center seam deterministically when x is exactly on the divide.
				targetAbility = Math.floorMod(pos.getZ() + pos.getY(), modulo) == 0 ? challengerAbility : ownerAbility;
			}
			BlockState targetState = resolveDomainTargetState(
				targetAbility,
				false,
				centerX,
				centerZ,
				state.baseY,
				state.radius,
				state.height,
				state.innerRadius,
				state.innerHeight,
				pos
			);

			world.setBlockState(pos, targetState, DOMAIN_BLOCK_PLACE_FLAGS);
		}
	}

	private static void applyDomainVisualForAbility(ServerWorld world, DomainExpansionState state, MagicAbility ability) {
		int centerX = MathHelper.floor(state.centerX);
		int centerZ = MathHelper.floor(state.centerZ);
		Map<BlockPos, BlockState> refreshedShell = new HashMap<>();
		for (BlockPos pos : state.savedBlocks.keySet()) {
			int relativeY = pos.getY() - state.baseY;
			if (relativeY < 0 || relativeY > state.height) {
				continue;
			}

			int horizontalDistanceSq = horizontalDistanceSq(pos.getX(), centerX, pos.getZ(), centerZ);
			if (relativeY > 0 && !isInsideDomainDome(horizontalDistanceSq, relativeY, state.radius, state.height)) {
				continue;
			}

			boolean shell = relativeY == 0 || !isInsideDomainDome(horizontalDistanceSq, relativeY, state.innerRadius, state.innerHeight);
			BlockState targetState = resolveDomainTargetState(
				ability,
				shell,
				centerX,
				centerZ,
				state.baseY,
				state.radius,
				state.height,
				state.innerRadius,
				state.innerHeight,
				pos
			);
			if (shell || targetState.isOf(Blocks.LIGHT)) {
				refreshedShell.put(pos, targetState);
			}
			world.setBlockState(pos, targetState, DOMAIN_BLOCK_PLACE_FLAGS);
		}

		state.protectedShellStates.clear();
		state.protectedShellStates.putAll(refreshedShell);
	}

	public static boolean isDomainClashParticipantInvincible(Entity entity) {
		return DOMAIN_CLASH_PARTICIPANTS_INVINCIBLE && domainClashStateForParticipant(entity.getUuid()) != null;
	}

	private static void maintainDomainShell(ServerWorld world, DomainExpansionState state) {
		for (Map.Entry<BlockPos, BlockState> entry : state.protectedShellStates.entrySet()) {
			BlockPos pos = entry.getKey();
			BlockState expectedState = entry.getValue();
			if (!world.isInBuildLimit(pos)) {
				continue;
			}

			if (!world.getBlockState(pos).equals(expectedState)) {
				world.setBlockState(pos, expectedState, DOMAIN_BLOCK_PLACE_FLAGS);
			}
		}
	}

	private static void enforceCapturedEntitiesInsideDomain(ServerWorld world, DomainExpansionState state) {
		for (DomainCapturedEntityState captured : state.capturedEntities.values()) {
			Entity entity = world.getEntity(captured.entityId);
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				continue;
			}

			double dx = living.getX() - state.centerX;
			double dz = living.getZ() - state.centerZ;
			double horizontalDistanceSq = dx * dx + dz * dz;
			double relativeY = living.getY() - state.baseY;

			if (isInsideDomainInterior(horizontalDistanceSq, relativeY, state.innerRadius, state.innerHeight)) {
				captured.lastSafePos = new Vec3d(living.getX(), living.getY(), living.getZ());
				captured.lastSafeYaw = living.getYaw();
				captured.lastSafePitch = living.getPitch();
				continue;
			}

			Vec3d safePos = captured.lastSafePos;
			teleportDomainEntity(living, safePos.x, safePos.y, safePos.z, captured.lastSafeYaw, captured.lastSafePitch);
		}
	}

	private static boolean isInsideDomainInterior(double horizontalDistanceSq, double y, int innerRadius, int innerHeight) {
		if (innerRadius <= 0 || innerHeight <= 1) {
			return false;
		}
		if (y < 1.0 || y > innerHeight - 0.2) {
			return false;
		}

		double horizontalTerm = horizontalDistanceSq / (double) (innerRadius * innerRadius);
		double verticalTerm = (y * y) / (double) (innerHeight * innerHeight);
		return horizontalTerm + verticalTerm <= 1.0;
	}

	private static void syncOwnedDomainAbilityState(ServerPlayerEntity player) {
		DomainExpansionState state = DOMAIN_EXPANSIONS.get(player.getUuid());
		if (state == null) {
			return;
		}

		if (state.ability == MagicAbility.LOVE_DOMAIN_EXPANSION || domainClashStateForParticipant(player.getUuid()) != null) {
			return;
		}

		if (activeAbility(player) != state.ability) {
			setActiveAbility(player, state.ability);
		}
	}

	private static Map<UUID, DomainCapturedEntityState> captureDomainEntities(
		ServerWorld world,
		double centerX,
		double centerZ,
		int baseY,
		int radius,
		int height
	) {
		Map<UUID, DomainCapturedEntityState> captured = new HashMap<>();
		Box captureBox = new Box(
			centerX - radius,
			baseY - 3,
			centerZ - radius,
			centerX + radius + 1.0,
			baseY + height + 4.0,
			centerZ + radius + 1.0
		);

		List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, captureBox, entity -> entity.isAlive() && isDomainCapturable(entity));
		for (LivingEntity entity : entities) {
			double dx = entity.getX() - centerX;
			double dz = entity.getZ() - centerZ;
			if (dx * dx + dz * dz > radius * radius) {
				continue;
			}

			captured.put(
				entity.getUuid(),
				new DomainCapturedEntityState(
					entity.getUuid(),
					entity instanceof PlayerEntity,
					new Vec3d(entity.getX(), entity.getY(), entity.getZ()),
					entity.getYaw(),
					entity.getPitch()
				)
			);
		}

		return captured;
	}

	private static void moveCapturedEntitiesIntoDomain(
		ServerWorld world,
		Iterable<DomainCapturedEntityState> capturedEntities,
		double centerX,
		double centerZ,
		int baseY,
		int innerRadius,
		int height
	) {
		double maxRadius = Math.max(0.0, innerRadius - 1.5);
		double minY = baseY + 1.0;
		double maxY = baseY + Math.max(2, height - 2);

		for (DomainCapturedEntityState captured : capturedEntities) {
			Entity entity = world.getEntity(captured.entityId);
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				continue;
			}

			double targetX = captured.position.x;
			double targetZ = captured.position.z;
			double targetY = MathHelper.clamp(captured.position.y, minY, maxY);
			double dx = targetX - centerX;
			double dz = targetZ - centerZ;
			double distance = Math.sqrt(dx * dx + dz * dz);
			if (maxRadius > 0.0 && distance > maxRadius) {
				double scale = maxRadius / Math.max(distance, 1.0E-7);
				targetX = centerX + dx * scale;
				targetZ = centerZ + dz * scale;
			}

			teleportDomainEntity(living, targetX, targetY, targetZ, captured.yaw, captured.pitch);
			captured.lastSafePos = new Vec3d(targetX, targetY, targetZ);
			captured.lastSafeYaw = captured.yaw;
			captured.lastSafePitch = captured.pitch;
		}
	}

	private static void restoreCapturedEntities(MinecraftServer server, DomainExpansionState state) {
		ServerWorld world = server.getWorld(state.dimension);
		if (world == null) {
			return;
		}

		for (DomainCapturedEntityState captured : state.capturedEntities.values()) {
			Entity entity = world.getEntity(captured.entityId);
			if (entity == null) {
				if (captured.playerEntity) {
					DOMAIN_PENDING_RETURNS.put(
						captured.entityId,
						new DomainPendingReturnState(
							state.dimension,
							captured.position.x,
							captured.position.y,
							captured.position.z,
							captured.yaw,
							captured.pitch
						)
					);
				}
				continue;
			}

			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				continue;
			}

			teleportDomainEntity(
				living,
				captured.position.x,
				captured.position.y,
				captured.position.z,
				captured.yaw,
				captured.pitch
			);
		}
	}

	private static void teleportDomainEntity(LivingEntity entity, double x, double y, double z, float yaw, float pitch) {
		if (entity.hasVehicle()) {
			entity.stopRiding();
		}

		entity.requestTeleport(x, y, z);
		entity.setVelocity(0.0, 0.0, 0.0);
		entity.setOnGround(true);
		entity.setYaw(yaw);
		entity.setPitch(pitch);
		entity.setHeadYaw(yaw);
		entity.setBodyYaw(yaw);

		if (entity instanceof ServerPlayerEntity player) {
			player.networkHandler.requestTeleport(x, y, z, yaw, pitch);
		}
	}

	private static boolean isDomainCapturable(LivingEntity entity) {
		if (entity instanceof PlayerEntity player) {
			return !player.isSpectator();
		}

		return entity instanceof MobEntity;
	}

	private static void onServerStarted(MinecraftServer server) {
		domainRuntimePersistentState = null;
		DOMAIN_EXPANSIONS.clear();
		DOMAIN_CLASHES_BY_OWNER.clear();
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.clear();
		FROST_DOMAIN_COOLDOWN_END_TICK.clear();
		LOVE_DOMAIN_COOLDOWN_END_TICK.clear();
		DOMAIN_PENDING_RETURNS.clear();
		TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.clear();
		TILL_DEATH_DO_US_PART_STATES.clear();
		TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.clear();
		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.clear();
		TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK.clear();
		loadPersistedDomainRuntimeState(server);
	}

	private static void onServerStopping(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			MagicPlayerData.clearDomainClashUi(player);
		}

		DOMAIN_CLASHES_BY_OWNER.clear();
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.clear();
		persistDomainRuntimeState(server);
		domainRuntimePersistentState = null;
		TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.clear();
		TILL_DEATH_DO_US_PART_STATES.clear();
		TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.clear();
		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.clear();
		TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK.clear();
		DOMAIN_PENDING_RETURNS.clear();
	}

	private static void loadPersistedDomainRuntimeState(MinecraftServer server) {
		DomainRuntimePersistentState persistentState = domainRuntimePersistentState(server);
		if (persistentState == null) {
			return;
		}

		deserializeDomainRuntimeState(server, persistentState.getDataCopy());
	}

	private static void persistDomainRuntimeState(MinecraftServer server) {
		DomainRuntimePersistentState persistentState = domainRuntimePersistentState(server);
		if (persistentState == null) {
			return;
		}

		persistentState.setData(serializeDomainRuntimeState(server));
	}

	private static DomainRuntimePersistentState domainRuntimePersistentState(MinecraftServer server) {
		if (domainRuntimePersistentState != null) {
			return domainRuntimePersistentState;
		}

		ServerWorld overworld = server.getOverworld();
		if (overworld == null) {
			return null;
		}

		domainRuntimePersistentState = overworld.getPersistentStateManager().getOrCreate(DomainRuntimePersistentState.TYPE);
		return domainRuntimePersistentState;
	}

	private static NbtCompound serializeDomainRuntimeState(MinecraftServer server) {
		NbtCompound root = new NbtCompound();
		NbtList domains = new NbtList();
		int currentTick = server.getTicks();

		for (Map.Entry<UUID, DomainExpansionState> entry : DOMAIN_EXPANSIONS.entrySet()) {
			DomainExpansionState state = entry.getValue();
			NbtCompound domainTag = new NbtCompound();
			domainTag.putString("owner", entry.getKey().toString());
			domainTag.putString("dimension", state.dimension.getValue().toString());
			domainTag.putString("ability", state.ability.id());
			int remainingTicks = state.expiresTick == Integer.MAX_VALUE ? -1 : Math.max(0, state.expiresTick - currentTick);
			domainTag.putInt("expiresRemainingTicks", remainingTicks);
			domainTag.putDouble("centerX", state.centerX);
			domainTag.putDouble("centerZ", state.centerZ);
			domainTag.putInt("baseY", state.baseY);
			domainTag.putInt("radius", state.radius);
			domainTag.putInt("height", state.height);
			domainTag.putInt("innerRadius", state.innerRadius);
			domainTag.putInt("innerHeight", state.innerHeight);
			domainTag.putBoolean("clashOccurred", state.clashOccurred);
			domainTag.putDouble("cooldownMultiplier", state.cooldownMultiplier);

			NbtList savedBlocks = new NbtList();
			for (Map.Entry<BlockPos, DomainSavedBlockState> savedEntry : state.savedBlocks.entrySet()) {
				NbtCompound blockTag = new NbtCompound();
				blockTag.putLong("pos", savedEntry.getKey().asLong());
				blockTag.put("state", NbtHelper.fromBlockState(savedEntry.getValue().blockState));
				if (savedEntry.getValue().blockEntityNbt != null) {
					blockTag.put("blockEntity", savedEntry.getValue().blockEntityNbt.copy());
				}
				savedBlocks.add(blockTag);
			}
			domainTag.put("savedBlocks", savedBlocks);

			NbtList protectedShell = new NbtList();
			for (Map.Entry<BlockPos, BlockState> shellEntry : state.protectedShellStates.entrySet()) {
				NbtCompound blockTag = new NbtCompound();
				blockTag.putLong("pos", shellEntry.getKey().asLong());
				blockTag.put("state", NbtHelper.fromBlockState(shellEntry.getValue()));
				protectedShell.add(blockTag);
			}
			domainTag.put("protectedShell", protectedShell);

			NbtList capturedEntities = new NbtList();
			for (DomainCapturedEntityState captured : state.capturedEntities.values()) {
				NbtCompound capturedTag = new NbtCompound();
				capturedTag.putString("entityId", captured.entityId.toString());
				capturedTag.putBoolean("playerEntity", captured.playerEntity);
				capturedTag.putDouble("x", captured.position.x);
				capturedTag.putDouble("y", captured.position.y);
				capturedTag.putDouble("z", captured.position.z);
				capturedTag.putFloat("yaw", captured.yaw);
				capturedTag.putFloat("pitch", captured.pitch);
				Vec3d lastSafePos = captured.lastSafePos == null ? captured.position : captured.lastSafePos;
				capturedTag.putDouble("lastSafeX", lastSafePos.x);
				capturedTag.putDouble("lastSafeY", lastSafePos.y);
				capturedTag.putDouble("lastSafeZ", lastSafePos.z);
				capturedTag.putFloat("lastSafeYaw", captured.lastSafeYaw);
				capturedTag.putFloat("lastSafePitch", captured.lastSafePitch);
				capturedEntities.add(capturedTag);
			}
			domainTag.put("capturedEntities", capturedEntities);
			domains.add(domainTag);
		}

		root.put(DOMAIN_PERSISTENCE_DOMAINS_KEY, domains);

		NbtList loveCooldowns = new NbtList();
		for (Map.Entry<UUID, Integer> entry : LOVE_DOMAIN_COOLDOWN_END_TICK.entrySet()) {
			int remainingTicks = Math.max(0, entry.getValue() - currentTick);
			if (remainingTicks <= 0) {
				continue;
			}

			NbtCompound cooldownTag = new NbtCompound();
			cooldownTag.putString("owner", entry.getKey().toString());
			cooldownTag.putInt("remainingTicks", remainingTicks);
			loveCooldowns.add(cooldownTag);
		}
		root.put(DOMAIN_PERSISTENCE_LOVE_COOLDOWNS_KEY, loveCooldowns);

		NbtList frostCooldowns = new NbtList();
		for (Map.Entry<UUID, Integer> entry : FROST_DOMAIN_COOLDOWN_END_TICK.entrySet()) {
			int remainingTicks = Math.max(0, entry.getValue() - currentTick);
			if (remainingTicks <= 0) {
				continue;
			}

			NbtCompound cooldownTag = new NbtCompound();
			cooldownTag.putString("owner", entry.getKey().toString());
			cooldownTag.putInt("remainingTicks", remainingTicks);
			frostCooldowns.add(cooldownTag);
		}
		root.put(DOMAIN_PERSISTENCE_FROST_COOLDOWNS_KEY, frostCooldowns);

		NbtList pendingReturns = new NbtList();
		for (Map.Entry<UUID, DomainPendingReturnState> entry : DOMAIN_PENDING_RETURNS.entrySet()) {
			DomainPendingReturnState state = entry.getValue();
			NbtCompound returnTag = new NbtCompound();
			returnTag.putString("playerId", entry.getKey().toString());
			returnTag.putString("dimension", state.dimension.getValue().toString());
			returnTag.putDouble("x", state.x);
			returnTag.putDouble("y", state.y);
			returnTag.putDouble("z", state.z);
			returnTag.putFloat("yaw", state.yaw);
			returnTag.putFloat("pitch", state.pitch);
			pendingReturns.add(returnTag);
		}
		root.put(DOMAIN_PERSISTENCE_PENDING_RETURNS_KEY, pendingReturns);

		return root;
	}

	private static void deserializeDomainRuntimeState(MinecraftServer server, NbtCompound root) {
		DOMAIN_EXPANSIONS.clear();
		DOMAIN_CLASHES_BY_OWNER.clear();
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.clear();
		FROST_DOMAIN_COOLDOWN_END_TICK.clear();
		LOVE_DOMAIN_COOLDOWN_END_TICK.clear();
		DOMAIN_PENDING_RETURNS.clear();
		int currentTick = server.getTicks();

		NbtList domains = root.getListOrEmpty(DOMAIN_PERSISTENCE_DOMAINS_KEY);
		for (NbtElement element : domains) {
			if (!(element instanceof NbtCompound domainTag)) {
				continue;
			}

			UUID ownerId = readUuid(domainTag, "owner");
			if (ownerId == null) {
				continue;
			}

			Identifier dimensionId = Identifier.tryParse(domainTag.getString("dimension", ""));
			if (dimensionId == null) {
				continue;
			}

			RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
			if (server.getWorld(dimension) == null) {
				continue;
			}

			MagicAbility ability = MagicAbility.fromId(domainTag.getString("ability", ""));
			if (!isDomainExpansion(ability)) {
				continue;
			}

			int expiresRemainingTicks = domainTag.getInt("expiresRemainingTicks", 0);
			int expiresTick = expiresRemainingTicks < 0 ? Integer.MAX_VALUE : currentTick + Math.max(0, expiresRemainingTicks);

			double centerX = domainTag.getDouble("centerX", 0.0);
			double centerZ = domainTag.getDouble("centerZ", 0.0);
			int baseY = domainTag.getInt("baseY", 0);
			int radius = Math.max(1, domainTag.getInt("radius", Math.max(1, DOMAIN_EXPANSION_RADIUS)));
			int height = Math.max(1, domainTag.getInt("height", Math.max(1, DOMAIN_EXPANSION_HEIGHT)));
			int innerRadius = Math.max(0, domainTag.getInt("innerRadius", 0));
			int innerHeight = Math.max(1, domainTag.getInt("innerHeight", 1));
			boolean clashOccurred = domainTag.getBoolean("clashOccurred", false);
			double cooldownMultiplier = MathHelper.clamp(domainTag.getDouble("cooldownMultiplier", 1.0), 0.0, 10.0);

			Map<BlockPos, DomainSavedBlockState> savedBlocks = new HashMap<>();
			for (NbtElement savedElement : domainTag.getListOrEmpty("savedBlocks")) {
				if (!(savedElement instanceof NbtCompound blockTag)) {
					continue;
				}

				BlockPos pos = BlockPos.fromLong(blockTag.getLong("pos", 0L));
				BlockState blockState = readPersistedBlockState(blockTag.getCompoundOrEmpty("state"), Blocks.AIR.getDefaultState());
				NbtCompound blockEntityNbt = blockTag.getCompound("blockEntity").map(NbtCompound::copy).orElse(null);
				savedBlocks.put(pos, new DomainSavedBlockState(blockState, blockEntityNbt));
			}

			Map<BlockPos, BlockState> protectedShellStates = new HashMap<>();
			for (NbtElement shellElement : domainTag.getListOrEmpty("protectedShell")) {
				if (!(shellElement instanceof NbtCompound blockTag)) {
					continue;
				}

				BlockPos pos = BlockPos.fromLong(blockTag.getLong("pos", 0L));
				BlockState blockState = readPersistedBlockState(blockTag.getCompoundOrEmpty("state"), DOMAIN_SHELL_BLOCK_STATE);
				protectedShellStates.put(pos, blockState);
			}

			Map<UUID, DomainCapturedEntityState> capturedEntities = new HashMap<>();
			for (NbtElement capturedElement : domainTag.getListOrEmpty("capturedEntities")) {
				if (!(capturedElement instanceof NbtCompound capturedTag)) {
					continue;
				}

				UUID entityId = readUuid(capturedTag, "entityId");
				if (entityId == null) {
					continue;
				}

				boolean playerEntity = capturedTag.getBoolean("playerEntity", false);
				Vec3d position = new Vec3d(
					capturedTag.getDouble("x", 0.0),
					capturedTag.getDouble("y", 0.0),
					capturedTag.getDouble("z", 0.0)
				);
				float yaw = capturedTag.getFloat("yaw", 0.0F);
				float pitch = capturedTag.getFloat("pitch", 0.0F);
				DomainCapturedEntityState capturedState = new DomainCapturedEntityState(entityId, playerEntity, position, yaw, pitch);
				capturedState.lastSafePos = new Vec3d(
					capturedTag.getDouble("lastSafeX", position.x),
					capturedTag.getDouble("lastSafeY", position.y),
					capturedTag.getDouble("lastSafeZ", position.z)
				);
				capturedState.lastSafeYaw = capturedTag.getFloat("lastSafeYaw", yaw);
				capturedState.lastSafePitch = capturedTag.getFloat("lastSafePitch", pitch);
				capturedEntities.put(entityId, capturedState);
			}

			DOMAIN_EXPANSIONS.put(
				ownerId,
				new DomainExpansionState(
					dimension,
					ability,
					expiresTick,
					centerX,
					centerZ,
					baseY,
					radius,
					height,
					innerRadius,
					innerHeight,
					currentTick - DOMAIN_CLASH_SIMULTANEOUS_WINDOW_TICKS - 1,
					clashOccurred,
					cooldownMultiplier,
					savedBlocks,
					protectedShellStates,
					capturedEntities
				)
			);
		}

		NbtList loveCooldowns = root.getListOrEmpty(DOMAIN_PERSISTENCE_LOVE_COOLDOWNS_KEY);
		for (NbtElement element : loveCooldowns) {
			if (!(element instanceof NbtCompound cooldownTag)) {
				continue;
			}

			UUID ownerId = readUuid(cooldownTag, "owner");
			if (ownerId == null) {
				continue;
			}

			int remainingTicks = cooldownTag.getInt("remainingTicks", 0);
			if (remainingTicks <= 0) {
				continue;
			}

			LOVE_DOMAIN_COOLDOWN_END_TICK.put(ownerId, currentTick + remainingTicks);
		}

		NbtList frostCooldowns = root.getListOrEmpty(DOMAIN_PERSISTENCE_FROST_COOLDOWNS_KEY);
		for (NbtElement element : frostCooldowns) {
			if (!(element instanceof NbtCompound cooldownTag)) {
				continue;
			}

			UUID ownerId = readUuid(cooldownTag, "owner");
			if (ownerId == null) {
				continue;
			}

			int remainingTicks = cooldownTag.getInt("remainingTicks", 0);
			if (remainingTicks <= 0) {
				continue;
			}

			FROST_DOMAIN_COOLDOWN_END_TICK.put(ownerId, currentTick + remainingTicks);
		}

		NbtList pendingReturns = root.getListOrEmpty(DOMAIN_PERSISTENCE_PENDING_RETURNS_KEY);
		for (NbtElement element : pendingReturns) {
			if (!(element instanceof NbtCompound returnTag)) {
				continue;
			}

			UUID playerId = readUuid(returnTag, "playerId");
			if (playerId == null) {
				continue;
			}

			Identifier dimensionId = Identifier.tryParse(returnTag.getString("dimension", ""));
			if (dimensionId == null) {
				continue;
			}

			RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
			if (server.getWorld(dimension) == null) {
				continue;
			}

			DOMAIN_PENDING_RETURNS.put(
				playerId,
				new DomainPendingReturnState(
					dimension,
					returnTag.getDouble("x", 0.0),
					returnTag.getDouble("y", 0.0),
					returnTag.getDouble("z", 0.0),
					returnTag.getFloat("yaw", 0.0F),
					returnTag.getFloat("pitch", 0.0F)
				)
			);
		}
	}

	private static BlockState readPersistedBlockState(NbtCompound stateTag, BlockState fallback) {
		if (stateTag.isEmpty()) {
			return fallback;
		}

		try {
			return NbtHelper.toBlockState(BLOCK_ENTRY_LOOKUP, stateTag);
		} catch (Exception exception) {
			return fallback;
		}
	}

	private static UUID readUuid(NbtCompound compound, String key) {
		String raw = compound.getString(key, "");
		if (raw.isBlank()) {
			return null;
		}

		try {
			return UUID.fromString(raw);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static void onEndServerTick(MinecraftServer server) {
		int currentTick = server.getTicks();
		Map<UUID, Set<UUID>> absoluteZeroAuraSeenThisTick = new HashMap<>();

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			syncOwnedDomainAbilityState(player);
			applyActiveAbilityEffects(player, currentTick, absoluteZeroAuraSeenThisTick);
		}

		updateLoveLockedTargets(server, currentTick);
		cleanupManipulationStates(server);
		syncManipulationSuppressionTags(server);
		cleanupPlanckHeatStates(server);
		trimAbsoluteZeroAuraTimers(absoluteZeroAuraSeenThisTick);
		updateFrostbittenTargets(server, currentTick);
		updateEnhancedFireTargets(server, currentTick);
		updateDomainClashes(server, currentTick);
		updateDomainExpansions(server, currentTick);

		if (currentTick % TICKS_PER_SECOND != 0) {
			return;
		}

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			updateManaOncePerSecond(player);
		}
	}

	private static void updateManaOncePerSecond(ServerPlayerEntity player) {
		if (!MagicPlayerData.hasMagic(player)) {
			return;
		}

		MagicAbility activeAbility = activeAbility(player);
		int mana = MagicPlayerData.getMana(player);
		UUID playerId = player.getUuid();

		if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			LOVE_POWER_ACTIVE_THIS_SECOND.put(playerId, false);
			if (!TILL_DEATH_DO_US_PART_STATES.containsKey(playerId)) {
				TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
				setActiveAbility(player, MagicAbility.NONE);
				return;
			}

			double bufferedDrain = TILL_DEATH_DO_US_PART_DRAIN_BUFFER.getOrDefault(playerId, 0.0)
				+ manaFromPercentExact(TILL_DEATH_DO_US_PART_DRAIN_PERCENT_PER_SECOND);
			int manaDrain = Math.max(0, (int) Math.floor(bufferedDrain + 1.0E-7));
			double remainingDrain = Math.max(0.0, bufferedDrain - manaDrain);
			if (remainingDrain > 1.0E-7) {
				TILL_DEATH_DO_US_PART_DRAIN_BUFFER.put(playerId, remainingDrain);
			} else {
				TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
			}
			int nextMana = Math.max(0, mana - manaDrain);
			MagicPlayerData.setMana(player, nextMana);

			if (nextMana == 0 && manaDrain > 0) {
				TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
				deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.MANA_DEPLETED, false);
				MagicPlayerData.setDepletedRecoveryMode(player, true);
				player.sendMessage(Text.translatable("message.magic.ability.out_of_mana", activeAbility.displayName()), true);
			}
			return;
		}

		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
		if (activeAbility != MagicAbility.NONE) {
			int manaDrain = switch (activeAbility) {
				case ABSOLUTE_ZERO -> ABSOLUTE_ZERO_MANA_DRAIN_PER_SECOND;
				case BELOW_FREEZING -> BELOW_FREEZING_MANA_DRAIN_PER_SECOND;
				case PLANCK_HEAT -> 0;
				case LOVE_AT_FIRST_SIGHT -> isLovePowerActiveThisSecond(player)
					? LOVE_AT_FIRST_SIGHT_ACTIVE_DRAIN_PER_SECOND
					: LOVE_AT_FIRST_SIGHT_IDLE_DRAIN_PER_SECOND;
				case MANIPULATION -> MANIPULATION_MANA_DRAIN_PER_SECOND;
				case FROST_DOMAIN_EXPANSION, LOVE_DOMAIN_EXPANSION -> 0;
				default -> 0;
			};
			LOVE_POWER_ACTIVE_THIS_SECOND.put(playerId, false);

			int nextMana = Math.max(0, mana - manaDrain);
			MagicPlayerData.setMana(player, nextMana);

			if (nextMana == 0 && manaDrain > 0) {
				if (activeAbility == MagicAbility.ABSOLUTE_ZERO) {
					deactivateAbsoluteZero(player);
				}
				if (activeAbility == MagicAbility.PLANCK_HEAT) {
					deactivatePlanckHeat(player);
				}
				if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
					deactivateLoveAtFirstSight(player);
				}
				if (activeAbility == MagicAbility.MANIPULATION) {
					deactivateManipulation(player, true, "mana depleted");
				}

				setActiveAbility(player, MagicAbility.NONE);
				MagicPlayerData.setDepletedRecoveryMode(player, true);
				player.sendMessage(Text.translatable("message.magic.ability.out_of_mana", activeAbility.displayName()), true);
			}

			return;
		}

		LOVE_POWER_ACTIVE_THIS_SECOND.put(playerId, false);
		regenerateMana(player, mana);
	}

	private static void regenerateMana(ServerPlayerEntity player, int currentMana) {
		boolean depletedMode = MagicPlayerData.isInDepletedRecoveryMode(player);
		int regenAmount = depletedMode ? DEPLETED_RECOVERY_REGEN_PER_SECOND : PASSIVE_MANA_REGEN_PER_SECOND;
		int nextMana = Math.min(MagicPlayerData.MAX_MANA, currentMana + regenAmount);
		MagicPlayerData.setMana(player, nextMana);

		if (depletedMode && nextMana >= MagicPlayerData.MAX_MANA) {
			MagicPlayerData.setDepletedRecoveryMode(player, false);
		}
	}

	private static void applyActiveAbilityEffects(
		ServerPlayerEntity player,
		int currentTick,
		Map<UUID, Set<UUID>> absoluteZeroAuraSeenThisTick
	) {
		MagicAbility activeAbility = activeAbility(player);
		if (activeAbility == MagicAbility.NONE) {
			return;
		}

		if (!player.isAlive()) {
			if (activeAbility == MagicAbility.ABSOLUTE_ZERO) {
				deactivateAbsoluteZero(player);
			}
			if (activeAbility == MagicAbility.PLANCK_HEAT) {
				deactivatePlanckHeat(player);
			}
			if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
				deactivateLoveAtFirstSight(player);
			}
			if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
				deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.CASTER_DIED, false);
			}
			if (activeAbility == MagicAbility.MANIPULATION) {
				deactivateManipulation(player, true, "caster died");
			}

			setActiveAbility(player, MagicAbility.NONE);
			return;
		}

		if (activeAbility.school() != MagicPlayerData.getSchool(player)) {
			if (activeAbility == MagicAbility.ABSOLUTE_ZERO) {
				deactivateAbsoluteZero(player);
			}
			if (activeAbility == MagicAbility.PLANCK_HEAT) {
				deactivatePlanckHeat(player);
			}
			if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
				deactivateLoveAtFirstSight(player);
			}
			if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
				deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.SCHOOL_CHANGED, false);
			}
			if (activeAbility == MagicAbility.MANIPULATION) {
				deactivateManipulation(player, true, "school changed or invalid");
			}

			setActiveAbility(player, MagicAbility.NONE);
			return;
		}

		if (activeAbility == MagicAbility.BELOW_FREEZING) {
			applyBelowFreezing(player);
			return;
		}

		if (activeAbility == MagicAbility.ABSOLUTE_ZERO) {
			applyAbsoluteZero(player, currentTick, absoluteZeroAuraSeenThisTick);
			return;
		}

		if (activeAbility == MagicAbility.PLANCK_HEAT) {
			applyPlanckHeat(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			applyLoveAtFirstSight(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			applyTillDeathDoUsPart(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.MANIPULATION) {
			applyManipulation(player);
		}
	}

	private static void applyBelowFreezing(ServerPlayerEntity player) {
		cleanseCommonNegativeEffects(player);
		player.addStatusEffect(
			new StatusEffectInstance(StatusEffects.RESISTANCE, EFFECT_REFRESH_TICKS, BELOW_FREEZING_RESISTANCE_AMPLIFIER, true, false, true)
		);
		spawnCasterAuraParticles(player);
		applyAuraSlowness(player, BELOW_FREEZING_AURA_RADIUS, BELOW_FREEZING_AURA_SLOWNESS_AMPLIFIER, 1);
	}

	private static void applyAbsoluteZero(
		ServerPlayerEntity player,
		int currentTick,
		Map<UUID, Set<UUID>> absoluteZeroAuraSeenThisTick
	) {
		cleanseAbsoluteZeroNegatives(player);
		player.addStatusEffect(
			new StatusEffectInstance(StatusEffects.RESISTANCE, EFFECT_REFRESH_TICKS, ABSOLUTE_ZERO_RESISTANCE_AMPLIFIER, true, false, true)
		);
		spawnAbsoluteZeroCasterParticles(player);
		Map<UUID, Integer> nextDamageTicksByTarget = ABSOLUTE_ZERO_NEXT_DAMAGE_TICK.computeIfAbsent(player.getUuid(), ignored -> new HashMap<>());
		Set<UUID> seenTargets = absoluteZeroAuraSeenThisTick.computeIfAbsent(player.getUuid(), ignored -> new HashSet<>());

		Box area = player.getBoundingBox().expand(ABSOLUTE_ZERO_AURA_RADIUS);
		Predicate<Entity> filter = entity -> entity instanceof LivingEntity living && living.isAlive() && entity != player;

		for (Entity entity : player.getEntityWorld().getOtherEntities(player, area, filter)) {
			if (entity instanceof PlayerEntity otherPlayer && otherPlayer.isSpectator()) {
				continue;
			}

			if (entity instanceof LivingEntity target) {
				seenTargets.add(target.getUuid());
				target.removeStatusEffect(StatusEffects.SLOWNESS);
				target.addStatusEffect(
					new StatusEffectInstance(StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, ABSOLUTE_ZERO_AURA_SLOWNESS_AMPLIFIER, true, true, true)
				);

				spawnHitboxShardParticles(target, 1);

				int nextDamageTick = nextDamageTicksByTarget.getOrDefault(
					target.getUuid(),
					currentTick + ABSOLUTE_ZERO_AURA_DAMAGE_INTERVAL_TICKS
				);

				if (currentTick >= nextDamageTick) {
					dealAbsoluteZeroAuraDamage(target);
					nextDamageTick = currentTick + ABSOLUTE_ZERO_AURA_DAMAGE_INTERVAL_TICKS;
				}

				nextDamageTicksByTarget.put(target.getUuid(), nextDamageTick);
			}
		}
	}

	private static void applyPlanckHeat(ServerPlayerEntity player, int currentTick) {
		PlanckHeatState state = PLANCK_HEAT_STATES.get(player.getUuid());
		if (state == null) {
			setActiveAbility(player, MagicAbility.NONE);
			return;
		}

		if (currentTick < state.frostPhaseEndTick) {
			applyPlanckHeatFrostPhase(player, currentTick);
			return;
		}

		PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.remove(player.getUuid());

		if (currentTick < state.firePhaseEndTick) {
			applyPlanckHeatFirePhase(player, currentTick);
			return;
		}

		deactivatePlanckHeat(player);
		setActiveAbility(player, MagicAbility.NONE);
		player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.PLANCK_HEAT.displayName()), true);
	}

	private static void applyPlanckHeatCasterBuffs(ServerPlayerEntity caster) {
		caster.addStatusEffect(
			new StatusEffectInstance(
				StatusEffects.ABSORPTION,
				PLANCK_HEAT_ABSORPTION_DURATION_TICKS,
				PLANCK_HEAT_ABSORPTION_AMPLIFIER,
				true,
				true,
				true
			)
		);
		caster.addStatusEffect(
			new StatusEffectInstance(
				StatusEffects.STRENGTH,
				PLANCK_HEAT_STRENGTH_DURATION_TICKS,
				PLANCK_HEAT_STRENGTH_AMPLIFIER,
				true,
				true,
				true
			)
		);
		caster.addStatusEffect(
			new StatusEffectInstance(
				StatusEffects.FIRE_RESISTANCE,
				PLANCK_HEAT_FIRE_RESISTANCE_DURATION_TICKS,
				PLANCK_HEAT_FIRE_RESISTANCE_AMPLIFIER,
				true,
				true,
				true
			)
		);
	}

	private static void applyPlanckHeatFrostPhase(ServerPlayerEntity caster, int currentTick) {
		cleanseAbsoluteZeroNegatives(caster);
		caster.addStatusEffect(
			new StatusEffectInstance(StatusEffects.RESISTANCE, EFFECT_REFRESH_TICKS, ABSOLUTE_ZERO_RESISTANCE_AMPLIFIER, true, false, true)
		);
		spawnPlanckHeatFrostCasterParticles(caster);

		UUID casterId = caster.getUuid();
		Map<UUID, Integer> nextDamageTicksByTarget = PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.computeIfAbsent(casterId, ignored -> new HashMap<>());
		Set<UUID> seenTargets = new HashSet<>();
		Box area = caster.getBoundingBox().expand(PLANCK_HEAT_AURA_RADIUS);
		Predicate<Entity> filter = entity -> entity instanceof LivingEntity living && living.isAlive() && entity != caster;

		for (Entity entity : caster.getEntityWorld().getOtherEntities(caster, area, filter)) {
			if (entity instanceof PlayerEntity otherPlayer && otherPlayer.isSpectator()) {
				continue;
			}

			if (entity instanceof LivingEntity target) {
				UUID targetId = target.getUuid();
				seenTargets.add(targetId);
				target.removeStatusEffect(StatusEffects.SLOWNESS);
				target.addStatusEffect(
					new StatusEffectInstance(StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, PLANCK_HEAT_FROST_SLOWNESS_AMPLIFIER, true, true, true)
				);
				spawnHitboxShardParticles(target, 2);

				int nextDamageTick = nextDamageTicksByTarget.getOrDefault(targetId, currentTick + PLANCK_HEAT_FROST_DAMAGE_INTERVAL_TICKS);
				if (currentTick >= nextDamageTick) {
					dealPlanckHeatFrostDamage(target);
					nextDamageTick = currentTick + PLANCK_HEAT_FROST_DAMAGE_INTERVAL_TICKS;
				}

				nextDamageTicksByTarget.put(targetId, nextDamageTick);
			}
		}

		nextDamageTicksByTarget.entrySet().removeIf(entry -> !seenTargets.contains(entry.getKey()));
		if (nextDamageTicksByTarget.isEmpty()) {
			PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.remove(casterId);
		}
	}

	private static void applyPlanckHeatFirePhase(ServerPlayerEntity caster, int currentTick) {
		spawnPlanckHeatFireCasterParticles(caster);

		Box area = caster.getBoundingBox().expand(PLANCK_HEAT_AURA_RADIUS);
		Predicate<Entity> filter = entity -> entity instanceof LivingEntity living && living.isAlive() && entity != caster;

		for (Entity entity : caster.getEntityWorld().getOtherEntities(caster, area, filter)) {
			if (entity instanceof PlayerEntity otherPlayer && otherPlayer.isSpectator()) {
				continue;
			}

			if (entity instanceof LivingEntity target) {
				applyOrRefreshEnhancedFire(target, currentTick, PLANCK_HEAT_AURA_ENHANCED_FIRE_DURATION_TICKS);
				target.addStatusEffect(
					new StatusEffectInstance(
						StatusEffects.HUNGER,
						EFFECT_REFRESH_TICKS,
						PLANCK_HEAT_FIRE_PHASE_HUNGER_AMPLIFIER,
						true,
						true,
						true
					)
				);
				target.addStatusEffect(
					new StatusEffectInstance(
						StatusEffects.NAUSEA,
						EFFECT_REFRESH_TICKS,
						PLANCK_HEAT_FIRE_PHASE_NAUSEA_AMPLIFIER,
						true,
						true,
						true
					)
				);
			}
		}
	}

	private static void applyLoveAtFirstSight(ServerPlayerEntity caster, int currentTick) {
		LivingEntity target = findLivingTargetInLineOfSight(caster);
		if (target == null) {
			return;
		}

		LOVE_POWER_ACTIVE_THIS_SECOND.put(caster.getUuid(), true);
		LoveLockState existingState = LOVE_LOCKED_TARGETS.get(target.getUuid());
		if (existingState == null || !existingState.casterId.equals(caster.getUuid())) {
			LOVE_LOCKED_TARGETS.put(
				target.getUuid(),
				new LoveLockState(
					target.getEntityWorld().getRegistryKey(),
					caster.getUuid(),
					target.getX(),
					target.getY(),
					target.getZ(),
					currentTick
				)
			);
			return;
		}

		existingState.lastSeenTick = currentTick;
	}

	private static void applyTillDeathDoUsPart(ServerPlayerEntity caster, int currentTick) {
		TillDeathDoUsPartState state = TILL_DEATH_DO_US_PART_STATES.get(caster.getUuid());
		if (state == null) {
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		ServerPlayerEntity linkedPlayer = caster.getEntityWorld().getServer().getPlayerManager().getPlayer(state.linkedPlayerId);
		if (linkedPlayer == null || linkedPlayer.isSpectator()) {
			deactivateTillDeathDoUsPart(caster, TillDeathDoUsPartEndReason.LINK_TARGET_INVALID, true);
			return;
		}
		if (!linkedPlayer.isAlive()) {
			caster.setHealth(0.0F);
			deactivateTillDeathDoUsPart(caster, TillDeathDoUsPartEndReason.LINK_TARGET_DIED, false);
			return;
		}

		if (currentTick > state.linkEndTick) {
			deactivateTillDeathDoUsPart(caster, TillDeathDoUsPartEndReason.LINK_EXPIRED, true);
			return;
		}

		syncTillDeathDoUsPartHealth(caster, linkedPlayer, state);
	}

	public static boolean onPlayerDamaged(ServerPlayerEntity damagedPlayer, DamageSource source, float amount) {
		if (source == null || amount <= 0.0F || damagedPlayer.getEntityWorld().isClient()) {
			return false;
		}

		if (handleDomainClashDamage(damagedPlayer, source, amount)) {
			return true;
		}

		if (isMagicSuppressed(damagedPlayer)) {
			return false;
		}

		UUID playerId = damagedPlayer.getUuid();
		if (!TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.contains(playerId) || TILL_DEATH_DO_US_PART_STATES.containsKey(playerId)) {
			return false;
		}

		if (MagicPlayerData.getSchool(damagedPlayer) != MagicSchool.LOVE) {
			return false;
		}

		if (!MagicConfig.get().abilityAccess.isAbilityUnlocked(playerId, MagicAbility.TILL_DEATH_DO_US_PART)) {
			return false;
		}

		DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.get(playerId);
		if (ownedDomain != null && ownedDomain.ability != MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return false;
		}

		Entity attacker = source.getAttacker();
		if (!(attacker instanceof ServerPlayerEntity attackerPlayer) || attackerPlayer == damagedPlayer || !attackerPlayer.isAlive() || attackerPlayer.isSpectator()) {
			return false;
		}

		int currentTick = damagedPlayer.getEntityWorld().getServer().getTicks();
		int remainingTicks = tillDeathDoUsPartCooldownRemaining(damagedPlayer, currentTick);
		if (remainingTicks > 0) {
			if (shouldSendTillDeathCooldownMessage(playerId, currentTick)) {
				sendAbilityCooldownMessage(damagedPlayer, MagicAbility.TILL_DEATH_DO_US_PART, remainingTicks, true);
			}
			return false;
		}

		if (MagicPlayerData.getMana(damagedPlayer) <= 0) {
			return false;
		}

		activateTillDeathDoUsPartLink(damagedPlayer, attackerPlayer, currentTick);
		return false;
	}

	private static void activateTillDeathDoUsPartLink(ServerPlayerEntity caster, ServerPlayerEntity linkedPlayer, int currentTick) {
		UUID casterId = caster.getUuid();
		if (!TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.contains(casterId) || TILL_DEATH_DO_US_PART_STATES.containsKey(casterId)) {
			return;
		}

		MagicAbility currentAbility = activeAbility(caster);
		DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.get(casterId);
		if (ownedDomain != null && ownedDomain.ability != MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return;
		}

		float averagedHealth = (float) Math.ceil((caster.getHealth() + linkedPlayer.getHealth()) / 2.0F);
		float maxSharedHealth = Math.min(caster.getMaxHealth(), linkedPlayer.getMaxHealth());
		float sharedHealth = MathHelper.clamp(averagedHealth, 0.0F, maxSharedHealth);

		if (currentAbility == MagicAbility.MANIPULATION) {
			deactivateManipulation(caster, true, "interrupted by Till Death Do Us Part");
		}

		if (currentAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			deactivateLoveAtFirstSight(caster);
		}

		TILL_DEATH_DO_US_PART_STATES.put(
			casterId,
			new TillDeathDoUsPartState(currentTick + TILL_DEATH_DO_US_PART_LINK_DURATION_TICKS, linkedPlayer.getUuid(), sharedHealth)
		);
		setActiveAbility(caster, MagicAbility.TILL_DEATH_DO_US_PART);
		MagicPlayerData.setDepletedRecoveryMode(caster, false);
		caster.setHealth(sharedHealth);
		linkedPlayer.setHealth(sharedHealth);
		caster.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.TILL_DEATH_DO_US_PART.displayName()), true);
	}

	private static void syncTillDeathDoUsPartHealth(
		ServerPlayerEntity caster,
		ServerPlayerEntity linkedPlayer,
		TillDeathDoUsPartState state
	) {
		float maxSharedHealth = Math.min(caster.getMaxHealth(), linkedPlayer.getMaxHealth());
		float sharedHealth = MathHelper.clamp(state.sharedHealth, 0.0F, maxSharedHealth);
		float casterHealth = caster.getHealth();
		float linkedHealth = linkedPlayer.getHealth();
		float epsilon = 0.001F;

		if (casterHealth < sharedHealth - epsilon || linkedHealth < sharedHealth - epsilon) {
			sharedHealth = Math.min(casterHealth, linkedHealth);
		} else if (casterHealth > sharedHealth + epsilon || linkedHealth > sharedHealth + epsilon) {
			sharedHealth = Math.max(casterHealth, linkedHealth);
		}

		sharedHealth = MathHelper.clamp(sharedHealth, 0.0F, maxSharedHealth);
		state.sharedHealth = sharedHealth;

		if (Math.abs(casterHealth - sharedHealth) > epsilon) {
			caster.setHealth(sharedHealth);
		}
		if (Math.abs(linkedHealth - sharedHealth) > epsilon) {
			linkedPlayer.setHealth(sharedHealth);
		}
	}

	private static double manaFromPercentExact(double percent) {
		double normalizedPercent = MathHelper.clamp(percent, 0.0, 100.0);
		return MagicPlayerData.MAX_MANA * (normalizedPercent / 100.0);
	}

	private static LivingEntity findLivingTargetInLineOfSight(ServerPlayerEntity caster) {
		return findLivingTargetInLineOfSight(caster, LOVE_GAZE_RANGE);
	}

	private static LivingEntity findLivingTargetInLineOfSight(ServerPlayerEntity caster, double range) {
		Vec3d eyePos = caster.getEyePos();
		Vec3d look = caster.getRotationVec(1.0F);
		double clampedRange = Math.max(1.0, range);
		Vec3d end = eyePos.add(look.multiply(clampedRange));
		Box area = caster.getBoundingBox().stretch(look.multiply(clampedRange)).expand(1.0);
		Predicate<Entity> filter = entity ->
			entity instanceof LivingEntity living &&
			living.isAlive() &&
			entity != caster &&
			(!(entity instanceof PlayerEntity player) || !player.isSpectator());

		EntityHitResult hitResult = ProjectileUtil.raycast(caster, eyePos, end, area, filter, clampedRange * clampedRange);
		if (hitResult == null || !(hitResult.getEntity() instanceof LivingEntity target)) {
			return null;
		}

		if (!caster.canSee(target)) {
			return null;
		}

		BlockHitResult blockHitResult = caster.getEntityWorld().raycast(
			new RaycastContext(
				eyePos,
				hitResult.getPos(),
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				caster
			)
		);

		if (blockHitResult.getType() == HitResult.Type.BLOCK) {
			double blockDistance = eyePos.squaredDistanceTo(blockHitResult.getPos());
			double targetDistance = eyePos.squaredDistanceTo(hitResult.getPos());
			if (blockDistance < targetDistance - 1.0E-6) {
				return null;
			}
		}

		return target;
	}

	private static ServerPlayerEntity findPlayerTargetInLineOfSight(ServerPlayerEntity caster) {
		return findPlayerTargetInLineOfSight(caster, LOVE_GAZE_RANGE);
	}

	private static ServerPlayerEntity findPlayerTargetInLineOfSight(ServerPlayerEntity caster, double range) {
		LivingEntity target = findLivingTargetInLineOfSight(caster, range);
		if (target == null) {
			debugManipulation("{} manipulation target search: no living target found", debugName(caster));
			return null;
		}

		if (target instanceof ServerPlayerEntity targetPlayer && targetPlayer.isAlive() && !targetPlayer.isSpectator()) {
			debugManipulation(
				"{} manipulation target search: resolved player target {} ({})",
				debugName(caster),
				targetPlayer.getName().getString(),
				targetPlayer.getUuid()
			);
			return targetPlayer;
		}

		debugManipulation(
			"{} manipulation target search: living target {} ({}) was not a valid player target (type={}, alive={}, spectator={})",
			debugName(caster),
			target.getName().getString(),
			target.getUuid(),
			target.getClass().getSimpleName(),
			target.isAlive(),
			target instanceof PlayerEntity player && player.isSpectator()
		);
		return null;
	}

	private static void activateManipulation(ServerPlayerEntity caster, ServerPlayerEntity target) {
		activateManipulation(caster, target, caster.getEntityWorld().getServer().getTicks());
	}

	private static void applyManipulation(ServerPlayerEntity caster) {
		ManipulationState state = MANIPULATION_STATES.get(caster.getUuid());
		if (state == null) {
			debugManipulation("{} empty embrace state missing during tick; canceling ability", debugName(caster));
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		int currentTick = caster.getEntityWorld().getServer().getTicks();
		if (state.stage == ManipulationStage.WAITING) {
			tryAcquireManipulationTarget(caster, currentTick);
			return;
		}

		ServerPlayerEntity targetPlayer = resolveManipulationTarget(caster.getEntityWorld().getServer(), state);
		if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.isSpectator()) {
			deactivateManipulation(caster, true, "target missing or dead");
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		if (targetPlayer.getEntityWorld() != caster.getEntityWorld()) {
			deactivateManipulation(caster, true, "target changed dimension");
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		double breakRangeSquared = MANIPULATION_BREAK_RANGE * (double) MANIPULATION_BREAK_RANGE;
		if (caster.squaredDistanceTo(targetPlayer) > breakRangeSquared) {
			deactivateManipulation(caster, true, "target moved out of range");
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		spawnManipulationTargetParticles(targetPlayer);
	}

	private static void cleanupManipulationStates(MinecraftServer server) {
		Iterator<Map.Entry<UUID, ManipulationState>> iterator = MANIPULATION_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ManipulationState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());

			if (caster != null && caster.isAlive() && activeAbility(caster) == MagicAbility.MANIPULATION) {
				continue;
			}

			debugManipulation(
				"{} manipulation cleanup: casterMissingOrInactive={}, casterAlive={}, activeAbility={}",
				caster == null ? entry.getKey() : debugName(caster),
				caster == null,
				caster != null && caster.isAlive(),
				caster == null ? "null" : activeAbility(caster)
			);
			releaseManipulationState(entry.getKey(), entry.getValue(), server, caster);
			MANIPULATION_COOLDOWN_END_TICK.put(entry.getKey(), server.getTicks() + MANIPULATION_COOLDOWN_TICKS);
			MANIPULATION_LAST_CLAMP_LOG_TICK.remove(entry.getKey());
			MANIPULATION_INTERACTION_PROXY.remove(entry.getKey());
			MANIPULATION_INPUT_BY_CASTER.remove(entry.getKey());
			MANIPULATION_LOOK_BY_CASTER.remove(entry.getKey());
			debugManipulation(
				"{} manipulation cleanup finalized: cooldownEndTick={}, remainingStates={}",
				caster == null ? entry.getKey() : debugName(caster),
				server.getTicks() + MANIPULATION_COOLDOWN_TICKS,
				MANIPULATION_STATES.size() - 1
			);
			iterator.remove();
		}
	}

	private static void syncManipulationSuppressionTags(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (isMagicSuppressed(player)) {
				applyManipulationSuppressionTags(player);
				continue;
			}

			clearManipulationSuppressionTags(player);
		}
	}

	private static void applyManipulationSuppressionTags(ServerPlayerEntity target) {
		target.addCommandTag(EMPTY_EMBRACE_TAG);
		setManipulationSuppressionTag(target, EMPTY_EMBRACE_ARTIFACT_POWERS_TAG, MANIPULATION_DISABLE_ARTIFACT_POWERS);
		setManipulationSuppressionTag(target, EMPTY_EMBRACE_ARTIFACT_SUMMONS_TAG, MANIPULATION_DISMISS_ARTIFACT_SUMMONS);
		setManipulationSuppressionTag(target, EMPTY_EMBRACE_ARTIFACT_ARMOR_TAG, MANIPULATION_DISABLE_ARTIFACT_ARMOR_EFFECTS);
		setManipulationSuppressionTag(target, EMPTY_EMBRACE_INFESTED_SILVERFISH_TAG, MANIPULATION_DISABLE_INFESTED_SILVERFISH_ENHANCEMENTS);
	}

	private static void clearManipulationSuppressionTags(ServerPlayerEntity target) {
		target.removeCommandTag(EMPTY_EMBRACE_TAG);
		target.removeCommandTag(EMPTY_EMBRACE_ARTIFACT_POWERS_TAG);
		target.removeCommandTag(EMPTY_EMBRACE_ARTIFACT_SUMMONS_TAG);
		target.removeCommandTag(EMPTY_EMBRACE_ARTIFACT_ARMOR_TAG);
		target.removeCommandTag(EMPTY_EMBRACE_INFESTED_SILVERFISH_TAG);
	}

	private static void setManipulationSuppressionTag(ServerPlayerEntity target, String tag, boolean enabled) {
		if (enabled) {
			target.addCommandTag(tag);
			return;
		}

		target.removeCommandTag(tag);
	}

	private static void cleanupPlanckHeatStates(MinecraftServer server) {
		Iterator<Map.Entry<UUID, PlanckHeatState>> iterator = PLANCK_HEAT_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, PlanckHeatState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());

			if (caster != null && caster.isAlive() && activeAbility(caster) == MagicAbility.PLANCK_HEAT) {
				continue;
			}

			PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.remove(entry.getKey());
			iterator.remove();
		}
	}

	private static ServerPlayerEntity resolveManipulationTarget(MinecraftServer server, ManipulationState state) {
		if (state == null || state.stage != ManipulationStage.LINKED || state.targetDimension == null || state.targetId == null) {
			return null;
		}

		ServerWorld world = server.getWorld(state.targetDimension);
		if (world == null) {
			debugManipulation(
				"empty embrace target resolve failed: dimension {} was not loaded for targetId={}",
				state.targetDimension.getValue(),
				state.targetId
			);
			return null;
		}

		Entity entity = world.getEntity(state.targetId);
		if (entity instanceof ServerPlayerEntity targetPlayer && targetPlayer.isAlive() && !targetPlayer.isSpectator()) {
			return targetPlayer;
		}
		debugManipulation(
			"empty embrace target resolve failed: targetId={} entityType={}, alive={}, spectator={}",
			state.targetId,
			entity == null ? "null" : entity.getClass().getSimpleName(),
			entity instanceof LivingEntity living && living.isAlive(),
			entity instanceof PlayerEntity player && player.isSpectator()
		);

		return null;
	}

	private static void updateLoveLockedTargets(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, LoveLockState>> iterator = LOVE_LOCKED_TARGETS.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, LoveLockState> entry = iterator.next();
			LoveLockState state = entry.getValue();

			if (state.lastSeenTick != currentTick) {
				iterator.remove();
				continue;
			}

			ServerWorld world = server.getWorld(state.dimension);
			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity targetEntity = world.getEntity(entry.getKey());
			if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
				iterator.remove();
				continue;
			}

			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(state.casterId);
			if (caster == null || !caster.isAlive() || activeAbility(caster) != MagicAbility.LOVE_AT_FIRST_SIGHT) {
				iterator.remove();
				continue;
			}

			enforceLoveLock(caster, target, state, currentTick);
		}
	}

	private static void enforceLoveLock(ServerPlayerEntity caster, LivingEntity target, LoveLockState state, int currentTick) {
		target.requestTeleport(state.lockedX, state.lockedY, state.lockedZ);
		target.setVelocity(0.0, 0.0, 0.0);
		target.setOnGround(true);
		target.stopUsingItem();
		target.addStatusEffect(
			new StatusEffectInstance(StatusEffects.SLOWNESS, LOVE_LOCK_EFFECT_TICKS, LOVE_LOCK_SLOWNESS_AMPLIFIER, true, false, false)
		);
		target.addStatusEffect(
			new StatusEffectInstance(
				StatusEffects.MINING_FATIGUE,
				LOVE_LOCK_EFFECT_TICKS,
				LOVE_LOCK_MINING_FATIGUE_AMPLIFIER,
				true,
				false,
				false
			)
		);
		if (currentTick - state.lastParticleTick >= LOVE_AT_FIRST_SIGHT_PARTICLE_INTERVAL_TICKS) {
			spawnLoveAtFirstSightTargetParticles(target);
			state.lastParticleTick = currentTick;
		}

		if (target instanceof MobEntity mob) {
			mob.getNavigation().stop();
		}

		float[] forcedAngles = computeFacingAngles(target, caster.getEyePos());
		target.setYaw(forcedAngles[0]);
		target.setPitch(forcedAngles[1]);
		target.setHeadYaw(forcedAngles[0]);
		target.setBodyYaw(forcedAngles[0]);

		if (target instanceof ServerPlayerEntity targetPlayer) {
			targetPlayer.networkHandler.requestTeleport(state.lockedX, state.lockedY, state.lockedZ, forcedAngles[0], forcedAngles[1]);
		}
	}

	private static float[] computeFacingAngles(LivingEntity source, Vec3d lookTargetPos) {
		double dx = lookTargetPos.x - source.getX();
		double dy = lookTargetPos.y - source.getEyePos().y;
		double dz = lookTargetPos.z - source.getZ();
		double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
		float yaw = MathHelper.wrapDegrees((float) (Math.toDegrees(MathHelper.atan2(dz, dx)) - 90.0));
		float pitch = MathHelper.wrapDegrees((float) (-Math.toDegrees(MathHelper.atan2(dy, horizontalDistance))));
		return new float[] { yaw, pitch };
	}

	private static void trimAbsoluteZeroAuraTimers(Map<UUID, Set<UUID>> seenThisTick) {
		ABSOLUTE_ZERO_NEXT_DAMAGE_TICK.entrySet().removeIf(entry -> {
			Set<UUID> seenTargets = seenThisTick.get(entry.getKey());
			if (seenTargets == null) {
				return true;
			}

			entry.getValue().entrySet().removeIf(targetEntry -> !seenTargets.contains(targetEntry.getKey()));
			return entry.getValue().isEmpty();
		});
	}

	private static void applyAuraSlowness(ServerPlayerEntity caster, int radius, int slownessAmplifier, int shardCount) {
		Box area = caster.getBoundingBox().expand(radius);
		Predicate<Entity> filter = entity -> entity instanceof LivingEntity living && living.isAlive() && entity != caster;

		for (Entity entity : caster.getEntityWorld().getOtherEntities(caster, area, filter)) {
			if (entity instanceof PlayerEntity otherPlayer && otherPlayer.isSpectator()) {
				continue;
			}

			if (entity instanceof LivingEntity livingTarget) {
				livingTarget.addStatusEffect(
					new StatusEffectInstance(StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, slownessAmplifier, true, true, true)
				);
				spawnHitboxShardParticles(livingTarget, shardCount);
			}
		}
	}

	private static ActionResult onUseItem(PlayerEntity player, World world, Hand hand) {
		if (shouldBlockArtifactUse(player, hand)) {
			return ActionResult.FAIL;
		}

		ItemStack stack = player.getStackInHand(hand);
		if (!canUseWindChargeWhileLocked(player, stack) && isActionLocked(player)) {
			return ActionResult.FAIL;
		}

		if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return ActionResult.PASS;
		}

		if (!isEntityCapturedByDomain(serverPlayer)) {
			return ActionResult.PASS;
		}

		if (stack.isOf(Items.CHORUS_FRUIT) || stack.isOf(Items.ENDER_PEARL)) {
			serverPlayer.sendMessage(Text.translatable("message.magic.domain.teleport_blocked"), true);
			return ActionResult.FAIL;
		}

		return ActionResult.PASS;
	}

	private static ActionResult onUseBlock(
		PlayerEntity player,
		World world,
		Hand hand,
		net.minecraft.util.hit.BlockHitResult hitResult
	) {
		if (shouldBlockArtifactUse(player, hand)) {
			return ActionResult.FAIL;
		}
		return isActionLocked(player) ? ActionResult.FAIL : ActionResult.PASS;
	}

	private static ActionResult onUseEntity(
		PlayerEntity player,
		World world,
		Hand hand,
		Entity entity,
		net.minecraft.util.hit.EntityHitResult hitResult
	) {
		if (shouldBlockArtifactUse(player, hand)) {
			return ActionResult.FAIL;
		}
		return isActionLocked(player) ? ActionResult.FAIL : ActionResult.PASS;
	}

	private static ActionResult onAttackBlock(
		PlayerEntity player,
		World world,
		Hand hand,
		net.minecraft.util.math.BlockPos pos,
		net.minecraft.util.math.Direction direction
	) {
		if (shouldBlockArtifactAttack(player)) {
			return ActionResult.FAIL;
		}
		return isActionLocked(player) ? ActionResult.FAIL : ActionResult.PASS;
	}

	private static boolean onBeforeBlockBreak(World world, PlayerEntity player, BlockPos pos) {
		if (shouldBlockArtifactAttack(player)) {
			return false;
		}
		if (world.isClient()) {
			return true;
		}

		if (!isDomainBlockProtected(world.getRegistryKey(), pos)) {
			return true;
		}

		if (player instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.sendMessage(Text.translatable("message.magic.domain.unbreakable"), true);
		}
		return false;
	}

	private static ActionResult onAttackEntity(
		PlayerEntity player,
		World world,
		Hand hand,
		Entity entity,
		EntityHitResult hitResult
	) {
		if (isActionLocked(player)) {
			return ActionResult.FAIL;
		}
		if (shouldBlockArtifactAttack(player)) {
			return ActionResult.FAIL;
		}

		if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return ActionResult.PASS;
		}

		if (!(entity instanceof LivingEntity target) || !target.isAlive() || target == player) {
			return ActionResult.PASS;
		}

		if (MagicPlayerData.getSchool(serverPlayer) != MagicSchool.FROST) {
			return ActionResult.PASS;
		}

		MagicAbility activeAbility = activeAbility(serverPlayer);
		int currentTick = serverPlayer.getEntityWorld().getServer().getTicks();
		if (activeAbility == MagicAbility.PLANCK_HEAT) {
			applyOrRefreshEnhancedFire(target, currentTick, PLANCK_HEAT_ATTACK_ENHANCED_FIRE_DURATION_TICKS);
			return ActionResult.PASS;
		}

		if (activeAbility != MagicAbility.BELOW_FREEZING && activeAbility != MagicAbility.ABSOLUTE_ZERO) {
			return ActionResult.PASS;
		}

		applyOrRefreshFrostbite(target, currentTick);
		return ActionResult.PASS;
	}

	private static boolean shouldBlockArtifactUse(PlayerEntity player, Hand hand) {
		if (!MANIPULATION_BLOCK_ARTIFACT_USE_CLICKS || !isMagicSuppressed(player)) {
			return false;
		}

		return isArtifactItem(player.getStackInHand(hand));
	}

	private static boolean shouldBlockArtifactAttack(PlayerEntity player) {
		if (!MANIPULATION_BLOCK_ARTIFACT_ATTACK_CLICKS || !isMagicSuppressed(player)) {
			return false;
		}

		return isArtifactItem(player.getMainHandStack());
	}

	private static boolean isArtifactItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		Identifier itemId = Registries.ITEM.getId(stack.getItem());
		return ARTIFACT_ITEM_NAMESPACE.equals(itemId.getNamespace());
	}

	private static void applyOrRefreshFrostbite(LivingEntity target, int currentTick) {
		UUID targetId = target.getUuid();
		RegistryKey<World> dimension = target.getEntityWorld().getRegistryKey();
		FrostbiteState state = new FrostbiteState(
			dimension,
			currentTick + FROSTBITE_DURATION_TICKS,
			currentTick + FROSTBITE_DAMAGE_INTERVAL_TICKS
		);
		FROSTBITTEN_TARGETS.put(targetId, state);

		applyFrostbiteControlEffects(target);
		spawnTargetSnowParticles(target);
		spawnHitboxShardParticles(target, 5);
		if (POWDERED_SNOW_DAMAGE_ON_INITIAL_APPLICATION) {
			dealFrostbiteDamage(target);
		}
	}

	private static void updateFrostbittenTargets(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, FrostbiteState>> iterator = FROSTBITTEN_TARGETS.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, FrostbiteState> entry = iterator.next();
			FrostbiteState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);

			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
				iterator.remove();
				continue;
			}

			if (currentTick > state.expiresTick) {
				iterator.remove();
				continue;
			}

			applyFrostbiteControlEffects(target);
			spawnTargetSnowParticles(target);

			if (currentTick >= state.nextDamageTick) {
				dealFrostbiteDamage(target);
				state.nextDamageTick = currentTick + FROSTBITE_DAMAGE_INTERVAL_TICKS;
			}
		}
	}

	private static void applyOrRefreshEnhancedFire(LivingEntity target, int currentTick, int durationTicks) {
		UUID targetId = target.getUuid();
		RegistryKey<World> dimension = target.getEntityWorld().getRegistryKey();
		int expiresTick = currentTick + durationTicks;
		EnhancedFireState existingState = ENHANCED_FIRE_TARGETS.get(targetId);

		if (existingState == null || !existingState.dimension.equals(dimension)) {
			ENHANCED_FIRE_TARGETS.put(
				targetId,
				new EnhancedFireState(dimension, expiresTick, currentTick + PLANCK_HEAT_ENHANCED_FIRE_DAMAGE_INTERVAL_TICKS)
			);
		} else {
			existingState.expiresTick = Math.max(existingState.expiresTick, expiresTick);
		}

		target.extinguish();
		spawnEnhancedFireTargetParticles(target);
	}

	private static void updateEnhancedFireTargets(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, EnhancedFireState>> iterator = ENHANCED_FIRE_TARGETS.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, EnhancedFireState> entry = iterator.next();
			EnhancedFireState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);

			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
				iterator.remove();
				continue;
			}

			if (currentTick > state.expiresTick) {
				iterator.remove();
				continue;
			}

			target.extinguish();
			spawnEnhancedFireTargetParticles(target);

			if (currentTick >= state.nextDamageTick) {
				dealEnhancedFireDamage(target);
				state.nextDamageTick = currentTick + PLANCK_HEAT_ENHANCED_FIRE_DAMAGE_INTERVAL_TICKS;
			}
		}
	}

	private static void applyFrostbiteControlEffects(LivingEntity target) {
		target.addStatusEffect(
			new StatusEffectInstance(StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, FROSTBITE_SLOWNESS_AMPLIFIER, true, true, true)
		);
		target.setFrozenTicks(Math.max(target.getFrozenTicks(), FROST_VISUAL_TICKS));
	}

	private static void dealFrostbiteDamage(LivingEntity target) {
		if (target.getEntityWorld() instanceof ServerWorld world) {
			target.damage(world, world.getDamageSources().freeze(), FROSTBITE_DAMAGE);
		}
	}

	private static void dealAbsoluteZeroAuraDamage(LivingEntity target) {
		if (target.getEntityWorld() instanceof ServerWorld world) {
			target.damage(world, world.getDamageSources().genericKill(), ABSOLUTE_ZERO_AURA_DAMAGE);
		}
	}

	private static void dealPlanckHeatFrostDamage(LivingEntity target) {
		if (target.getEntityWorld() instanceof ServerWorld world) {
			target.damage(world, world.getDamageSources().freeze(), PLANCK_HEAT_FROST_DAMAGE);
		}
	}

	private static void dealEnhancedFireDamage(LivingEntity target) {
		if (target.getEntityWorld() instanceof ServerWorld world) {
			target.damage(world, world.getDamageSources().onFire(), PLANCK_HEAT_ENHANCED_FIRE_DAMAGE);
		}
	}

	private static void cleanseCommonNegativeEffects(LivingEntity entity) {
		for (var effect : COMMON_NEGATIVE_EFFECTS) {
			entity.removeStatusEffect(effect);
		}
	}

	private static void cleanseAbsoluteZeroNegatives(LivingEntity entity) {
		for (StatusEffectInstance statusEffect : List.copyOf(entity.getStatusEffects())) {
			var effectType = statusEffect.getEffectType();
			boolean harmful = effectType.value().getCategory() == StatusEffectCategory.HARMFUL;
			boolean forceRemove = effectType == StatusEffects.SLOW_FALLING;

			if (harmful || forceRemove) {
				entity.removeStatusEffect(effectType);
			}
		}
	}

	private static void spawnCasterAuraParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.WHITE_ASH, x, y, z, 3, 0.30, 0.45, 0.30, 0.004);
	}

	private static void playDomainActivationSounds(ServerWorld world, LivingEntity entity) {
		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.playSound(null, x, y, z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.2F, 0.55F);
		world.playSound(null, x, y, z, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.0F, 0.75F);
		world.playSound(null, x, y, z, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.8F, 1.6F);
	}

	private static void spawnAbsoluteZeroCasterParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.WHITE_ASH, x, y, z, 6, 0.40, 0.55, 0.40, 0.007);
		world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 6, 0.45, 0.60, 0.45, 0.012);
	}

	private static void spawnPlanckHeatFrostCasterParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.WHITE_ASH, x, y, z, 8, 0.55, 0.65, 0.55, 0.012);
		world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 10, 0.60, 0.75, 0.60, 0.02);
	}

	private static void spawnPlanckHeatFireCasterParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.55);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 8, 0.55, 0.65, 0.55, 0.02);
	}

	private static void spawnTargetSnowParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 3, 0.25, 0.35, 0.25, 0.008);
	}

	private static void spawnEnhancedFireTargetParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 4, 0.30, 0.45, 0.30, 0.01);
	}

	private static void spawnLoveAtFirstSightTargetParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.7);
		double z = entity.getZ();
		if (LOVE_AT_FIRST_SIGHT_HEART_PARTICLES > 0) {
			world.spawnParticles(ParticleTypes.HEART, x, y, z, LOVE_AT_FIRST_SIGHT_HEART_PARTICLES, 0.24, 0.30, 0.24, 0.0);
		}
		if (LOVE_AT_FIRST_SIGHT_HAPPY_VILLAGER_PARTICLES > 0) {
			world.spawnParticles(
				ParticleTypes.HAPPY_VILLAGER,
				x,
				y,
				z,
				LOVE_AT_FIRST_SIGHT_HAPPY_VILLAGER_PARTICLES,
				0.28,
				0.35,
				0.28,
				0.01
			);
		}
	}

	private static void spawnManipulationTargetParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.ENCHANT, x, y, z, 6, 0.28, 0.40, 0.28, 0.02);
		world.spawnParticles(ParticleTypes.PORTAL, x, y, z, 2, 0.20, 0.30, 0.20, 0.03);
	}

	private static void spawnHitboxShardParticles(LivingEntity entity, int count) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		Box box = entity.getBoundingBox();
		ItemStackParticleEffect shardParticle = new ItemStackParticleEffect(
			ParticleTypes.ITEM,
			new ItemStack(ModItems.FROST_SHARD)
		);

		for (int i = 0; i < count; i++) {
			double x = box.minX + entity.getRandom().nextDouble() * box.getLengthX();
			double y = box.minY + entity.getRandom().nextDouble() * box.getLengthY();
			double z = box.minZ + entity.getRandom().nextDouble() * box.getLengthZ();
			world.spawnParticles(shardParticle, x, y, z, 1, 0.08, 0.12, 0.08, 0.005);
		}
	}

	public static void onCooldownCheckRequested(ServerPlayerEntity player) {
		if (!MagicPlayerData.hasMagic(player)) {
			player.sendMessage(Text.translatable("message.magic.no_access"), false);
			return;
		}

		int currentTick = player.getEntityWorld().getServer().getTicks();
		boolean foundCooldown = false;
		for (MagicAbility ability : abilitiesForSchool(MagicPlayerData.getSchool(player))) {
			int remainingTicks = cooldownRemaining(player, ability, currentTick);
			if (remainingTicks <= 0) {
				continue;
			}

			sendAbilityCooldownMessage(player, ability, remainingTicks, false);
			foundCooldown = true;
		}

		if (!foundCooldown) {
			player.sendMessage(Text.translatable("message.magic.cooldown.none"), false);
		}
	}

	private static List<MagicAbility> abilitiesForSchool(MagicSchool school) {
		return switch (school) {
			case FROST -> List.of(
				MagicAbility.BELOW_FREEZING,
				MagicAbility.ABSOLUTE_ZERO,
				MagicAbility.PLANCK_HEAT,
				MagicAbility.FROST_DOMAIN_EXPANSION
			);
			case LOVE -> List.of(
				MagicAbility.LOVE_AT_FIRST_SIGHT,
				MagicAbility.TILL_DEATH_DO_US_PART,
				MagicAbility.MANIPULATION,
				MagicAbility.LOVE_DOMAIN_EXPANSION
			);
			default -> List.of();
		};
	}

	private static int cooldownRemaining(ServerPlayerEntity player, MagicAbility ability, int currentTick) {
		return switch (ability) {
			case ABSOLUTE_ZERO -> absoluteZeroCooldownRemaining(player, currentTick);
			case PLANCK_HEAT -> planckHeatCooldownRemaining(player, currentTick);
			case TILL_DEATH_DO_US_PART -> tillDeathDoUsPartCooldownRemaining(player, currentTick);
			case MANIPULATION -> manipulationCooldownRemaining(player, currentTick);
			case FROST_DOMAIN_EXPANSION, LOVE_DOMAIN_EXPANSION -> domainCooldownRemaining(player, ability, currentTick);
			default -> 0;
		};
	}

	private static void sendAbilityCooldownMessage(
		ServerPlayerEntity player,
		MagicAbility ability,
		int remainingTicks,
		boolean actionBar
	) {
		int secondsRemaining = (int) Math.ceil(remainingTicks / 20.0);
		player.sendMessage(Text.translatable("message.magic.ability.cooldown", ability.displayName(), secondsRemaining), actionBar);
	}

	private static boolean shouldSendTillDeathCooldownMessage(UUID playerId, int currentTick) {
		int nextAllowedTick = TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK.getOrDefault(playerId, 0);
		if (currentTick < nextAllowedTick) {
			return false;
		}

		TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK.put(playerId, currentTick + COOLDOWN_MESSAGE_DEBOUNCE_TICKS);
		return true;
	}

	private static boolean ownsLoveDomain(UUID playerId) {
		DomainExpansionState state = DOMAIN_EXPANSIONS.get(playerId);
		return state != null && state.ability == MagicAbility.LOVE_DOMAIN_EXPANSION;
	}

	private static boolean canUseWindChargeWhileLocked(PlayerEntity player, ItemStack stack) {
		if (!stack.isOf(Items.WIND_CHARGE)) {
			return false;
		}

		UUID playerId = player.getUuid();
		return ownsLoveDomain(playerId) || domainClashStateForParticipant(playerId) != null;
	}

	private static void applyDomainClashStartEffects(ServerPlayerEntity player) {
		player.addStatusEffect(
			new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, DOMAIN_CLASH_INSTANT_HEALTH_AMPLIFIER, true, false, true)
		);
		applyDomainClashCombatEffects(player);
	}

	private static void applyDomainClashCombatEffects(ServerPlayerEntity player) {
		player.addStatusEffect(
			new StatusEffectInstance(
				StatusEffects.REGENERATION,
				DOMAIN_CLASH_REGENERATION_REFRESH_TICKS,
				DOMAIN_CLASH_REGENERATION_AMPLIFIER,
				true,
				false,
				true
			)
		);
	}

	private static void applyPendingDomainReturn(ServerPlayerEntity player, MinecraftServer server) {
		DomainPendingReturnState pendingReturn = DOMAIN_PENDING_RETURNS.remove(player.getUuid());
		if (pendingReturn == null) {
			return;
		}

		ServerWorld targetWorld = server.getWorld(pendingReturn.dimension);
		if (targetWorld == null) {
			DOMAIN_PENDING_RETURNS.put(player.getUuid(), pendingReturn);
			return;
		}

		player.teleport(
			targetWorld,
			pendingReturn.x,
			pendingReturn.y,
			pendingReturn.z,
			Set.<PositionFlag>of(),
			pendingReturn.yaw,
			pendingReturn.pitch,
			false
		);
		player.setVelocity(0.0, 0.0, 0.0);
		player.setOnGround(true);
		persistDomainRuntimeState(server);
	}

	private static int absoluteZeroCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return Math.max(0, ABSOLUTE_ZERO_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int planckHeatCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return Math.max(0, PLANCK_HEAT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int tillDeathDoUsPartCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return Math.max(0, TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int domainCooldownRemaining(ServerPlayerEntity player, MagicAbility ability, int currentTick) {
		UUID playerId = player.getUuid();
		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			return Math.max(0, FROST_DOMAIN_COOLDOWN_END_TICK.getOrDefault(playerId, 0) - currentTick);
		}
		if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return Math.max(0, LOVE_DOMAIN_COOLDOWN_END_TICK.getOrDefault(playerId, 0) - currentTick);
		}
		return 0;
	}

	private static void startDomainCooldown(UUID playerId, MagicAbility ability, int currentTick, double multiplier) {
		int baseTicks = domainCooldownTicks(ability);
		if (baseTicks <= 0) {
			return;
		}

		double safeMultiplier = MathHelper.clamp(multiplier, 0.0, 10.0);
		int cooldownTicks = (int) Math.ceil(baseTicks * safeMultiplier);
		if (cooldownTicks <= 0) {
			return;
		}

		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			FROST_DOMAIN_COOLDOWN_END_TICK.put(playerId, currentTick + cooldownTicks);
			return;
		}

		if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
			LOVE_DOMAIN_COOLDOWN_END_TICK.put(playerId, currentTick + cooldownTicks);
		}
	}

	private static int domainCooldownTicks(MagicAbility ability) {
		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			return Math.max(0, FROST_DOMAIN_COOLDOWN_TICKS);
		}
		if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return Math.max(0, LOVE_DOMAIN_COOLDOWN_TICKS);
		}
		return 0;
	}

	private static void applyDomainInstabilityPenalty(ServerPlayerEntity player) {
		MagicPlayerData.setMana(player, 0);
		player.sendMessage(Text.translatable("message.magic.domain.unstable"), true);
	}

	private static boolean isManipulationRequestDebounced(ServerPlayerEntity player, int currentTick) {
		UUID playerId = player.getUuid();
		int nextAllowedTick = MANIPULATION_NEXT_REQUEST_TICK.getOrDefault(playerId, 0);
		if (currentTick < nextAllowedTick) {
			debugManipulation(
				"{} manipulation request debounced at tick {} (nextAllowedTick={})",
				debugName(player),
				currentTick,
				nextAllowedTick
			);
			return true;
		}

		MANIPULATION_NEXT_REQUEST_TICK.put(playerId, currentTick + MANIPULATION_REQUEST_DEBOUNCE_TICKS);
		debugManipulation(
			"{} manipulation request accepted at tick {} (nextAllowedTick set to {})",
			debugName(player),
			currentTick,
			currentTick + MANIPULATION_REQUEST_DEBOUNCE_TICKS
		);
		return false;
	}

	private static boolean shouldLogManipulationClamp(ServerPlayerEntity player, int currentTick) {
		UUID playerId = player.getUuid();
		int nextAllowedTick = MANIPULATION_LAST_CLAMP_LOG_TICK.getOrDefault(playerId, 0);
		if (currentTick < nextAllowedTick) {
			return false;
		}

		MANIPULATION_LAST_CLAMP_LOG_TICK.put(playerId, currentTick + MANIPULATION_CLAMP_LOG_INTERVAL_TICKS);
		return true;
	}

	private static int manipulationCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return Math.max(0, MANIPULATION_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static Vec3d clampVector(Vec3d vector, double maxLength) {
		double lengthSquared = vector.lengthSquared();
		double maxLengthSquared = maxLength * maxLength;
		if (lengthSquared <= maxLengthSquared) {
			return vector;
		}

		double length = Math.sqrt(lengthSquared);
		if (length <= 1.0E-7) {
			return Vec3d.ZERO;
		}

		double scale = maxLength / length;
		return vector.multiply(scale);
	}

	private static Vec3d manipulationHorizontalDelta(Vec3d rawInput, float yaw, double maxLengthPerTick) {
		Vec3d clampedInput = clampVector(rawInput, 1.0);
		double yawRadians = Math.toRadians(yaw);
		double sin = Math.sin(yawRadians);
		double cos = Math.cos(yawRadians);
		double worldX = clampedInput.x * cos - clampedInput.z * sin;
		double worldZ = clampedInput.z * cos + clampedInput.x * sin;
		return clampVector(new Vec3d(worldX, 0.0, worldZ), maxLengthPerTick);
	}

	private static boolean isDomainExpansion(MagicAbility ability) {
		return ability == MagicAbility.FROST_DOMAIN_EXPANSION || ability == MagicAbility.LOVE_DOMAIN_EXPANSION;
	}

	public static boolean isEntityCapturedByDomain(Entity entity) {
		RegistryKey<World> dimension = entity.getEntityWorld().getRegistryKey();
		UUID entityId = entity.getUuid();
		for (Map.Entry<UUID, DomainExpansionState> entry : DOMAIN_EXPANSIONS.entrySet()) {
			if (DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS && DOMAIN_CLASHES_BY_OWNER.containsKey(entry.getKey())) {
				continue;
			}

			DomainExpansionState state = entry.getValue();
			if (state.dimension.equals(dimension) && state.capturedEntities.containsKey(entityId)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isEntityCapturedByLoveDomain(Entity entity) {
		if (domainClashStateForParticipant(entity.getUuid()) != null) {
			return false;
		}

		RegistryKey<World> dimension = entity.getEntityWorld().getRegistryKey();
		UUID entityId = entity.getUuid();
		for (Map.Entry<UUID, DomainExpansionState> entry : DOMAIN_EXPANSIONS.entrySet()) {
			if (DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS && DOMAIN_CLASHES_BY_OWNER.containsKey(entry.getKey())) {
				continue;
			}

			DomainExpansionState state = entry.getValue();
			if (state.ability != MagicAbility.LOVE_DOMAIN_EXPANSION) {
				continue;
			}

			if (state.dimension.equals(dimension) && state.capturedEntities.containsKey(entityId)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isDomainBlockProtected(RegistryKey<World> dimension, BlockPos pos) {
		for (Map.Entry<UUID, DomainExpansionState> entry : DOMAIN_EXPANSIONS.entrySet()) {
			DomainExpansionState state = entry.getValue();
			if (state.dimension.equals(dimension) && state.protectedShellStates.containsKey(pos)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isInsideDomainDome(int horizontalDistanceSq, int y, int radius, int height) {
		if (radius <= 0 || height <= 0 || y < 0) {
			return false;
		}

		double horizontalTerm = horizontalDistanceSq / (double) (radius * radius);
		double verticalTerm = (double) (y * y) / (double) (height * height);
		return horizontalTerm + verticalTerm <= 1.0;
	}

	private static BlockState resolveDomainTargetState(
		MagicAbility ability,
		boolean shell,
		int centerX,
		int centerZ,
		int baseY,
		int radius,
		int height,
		int innerRadius,
		int innerHeight,
		BlockPos pos
	) {
		if (ability != MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return shell ? DOMAIN_SHELL_BLOCK_STATE : DOMAIN_INTERIOR_BLOCK_STATE;
		}

		boolean roseSide = pos.getX() >= centerX;
		int relativeY = pos.getY() - baseY;
		if (shell) {
			return resolveLoveDomainShellState(pos, roseSide, relativeY, height);
		}

		return resolveLoveDomainInteriorState(pos, roseSide, relativeY, centerX, centerZ, innerRadius, innerHeight);
	}

	private static BlockState resolveLoveDomainShellState(BlockPos pos, boolean roseSide, int relativeY, int height) {
		if (roseSide) {
			if (relativeY == 0) {
				return Blocks.GRASS_BLOCK.getDefaultState();
			}
			if (relativeY >= height - 1) {
				return shouldDecorate(pos, 4) ? Blocks.RED_TERRACOTTA.getDefaultState() : Blocks.MOSS_BLOCK.getDefaultState();
			}

			return shouldDecorate(pos, 5) ? Blocks.RED_CONCRETE.getDefaultState() : Blocks.MOSS_BLOCK.getDefaultState();
		}

		if (relativeY == 0) {
			return Blocks.SOUL_SOIL.getDefaultState();
		}
		if (relativeY >= height - 1) {
			return shouldDecorate(pos, 4) ? Blocks.CRYING_OBSIDIAN.getDefaultState() : Blocks.POLISHED_BLACKSTONE.getDefaultState();
		}

		return shouldDecorate(pos, 5) ? Blocks.BLACKSTONE.getDefaultState() : Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
	}

	private static BlockState resolveLoveDomainInteriorState(
		BlockPos pos,
		boolean roseSide,
		int relativeY,
		int centerX,
		int centerZ,
		int innerRadius,
		int innerHeight
	) {
		if (relativeY == 1) {
			if (!roseSide) {
				return shouldDecorate(pos, 3) ? Blocks.WITHER_ROSE.getDefaultState() : DOMAIN_INTERIOR_BLOCK_STATE;
			}

			int variant = Math.floorMod(decorHash(pos), 7);
			if (variant <= 1) {
				return Blocks.POPPY.getDefaultState();
			}
			if (variant == 2) {
				return Blocks.RED_TULIP.getDefaultState();
			}
			if (variant == 3) {
				return Blocks.SHORT_GRASS.getDefaultState();
			}
			return DOMAIN_INTERIOR_BLOCK_STATE;
		}

		if (shouldPlaceLoveDomainLight(pos, relativeY, centerX, centerZ, innerRadius, innerHeight)) {
			return LOVE_DOMAIN_LIGHT_STATE;
		}

		if (roseSide && relativeY > 1) {
			BlockState vineState = resolveLoveDomainVineState(pos, relativeY, centerX, centerZ, innerRadius, innerHeight);
			if (vineState != null) {
				return vineState;
			}
		}

		return DOMAIN_INTERIOR_BLOCK_STATE;
	}

	private static boolean shouldPlaceLoveDomainLight(
		BlockPos pos,
		int relativeY,
		int centerX,
		int centerZ,
		int innerRadius,
		int innerHeight
	) {
		if (relativeY < 2 || relativeY > innerHeight - 2) {
			return false;
		}

		if (relativeY != 2 && relativeY % 6 != 0) {
			return false;
		}

		int dx = Math.abs(pos.getX() - centerX);
		int dz = Math.abs(pos.getZ() - centerZ);
		if (dx % 6 != 0 || dz % 6 != 0) {
			return false;
		}

		int horizontalDistanceSq = horizontalDistanceSq(pos.getX(), centerX, pos.getZ(), centerZ);
		return isInsideDomainDome(horizontalDistanceSq, relativeY, innerRadius, innerHeight);
	}

	private static BlockState resolveLoveDomainVineState(
		BlockPos pos,
		int relativeY,
		int centerX,
		int centerZ,
		int innerRadius,
		int innerHeight
	) {
		if (!shouldDecorate(pos, 8)) {
			return null;
		}

		BlockState vine = Blocks.VINE.getDefaultState();
		boolean attached = false;

		for (Direction direction : Direction.Type.HORIZONTAL) {
			int neighborX = pos.getX() + direction.getOffsetX();
			int neighborZ = pos.getZ() + direction.getOffsetZ();
			int horizontalDistanceSq = horizontalDistanceSq(neighborX, centerX, neighborZ, centerZ);
			if (!isInsideDomainDome(horizontalDistanceSq, relativeY, innerRadius, innerHeight)) {
				if (direction == Direction.NORTH) {
					vine = vine.with(VineBlock.NORTH, true);
				}
				if (direction == Direction.SOUTH) {
					vine = vine.with(VineBlock.SOUTH, true);
				}
				if (direction == Direction.EAST) {
					vine = vine.with(VineBlock.EAST, true);
				}
				if (direction == Direction.WEST) {
					vine = vine.with(VineBlock.WEST, true);
				}
				attached = true;
				break;
			}
		}

		if (!attached) {
			int horizontalDistanceSq = horizontalDistanceSq(pos.getX(), centerX, pos.getZ(), centerZ);
			if (!isInsideDomainDome(horizontalDistanceSq, relativeY + 1, innerRadius, innerHeight)) {
				vine = vine.with(VineBlock.UP, true);
				attached = true;
			}
		}

		return attached ? vine : null;
	}

	private static int horizontalDistanceSq(int x, int centerX, int z, int centerZ) {
		int dx = x - centerX;
		int dz = z - centerZ;
		return dx * dx + dz * dz;
	}

	private static boolean shouldDecorate(BlockPos pos, int modulo) {
		return Math.floorMod(decorHash(pos), modulo) == 0;
	}

	private static int decorHash(BlockPos pos) {
		return pos.getX() * 73428767 ^ pos.getY() * 912367 ^ pos.getZ() * 42317861;
	}

	private static String debugName(ServerPlayerEntity player) {
		return player.getName().getString() + "(" + player.getUuidAsString() + ")";
	}

	private static double round3(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}

	private static void debugManipulation(String message, Object... args) {
		if (!MANIPULATION_DEBUG_LOGGING) {
			return;
		}

		Magic.LOGGER.info("[manipulation-debug] " + message, args);
	}

	public static void debugManipulationPacket(String message, Object... args) {
		debugManipulation(message, args);
	}

	private static boolean isManipulationPacketServerThread(ServerPlayerEntity player) {
		MinecraftServer server = player.getEntityWorld().getServer();
		return server != null && server.isOnThread();
	}

	public static void onManipulationInputPacket(ServerPlayerEntity player, PlayerInput input) {
		if (!isManipulationPacketServerThread(player)) {
			return;
		}

		UUID playerId = player.getUuid();
		MANIPULATION_INPUT_BY_CASTER.put(playerId, input);
		if (!isManipulatingCaster(player) && !isManipulationControlledTarget(player)) {
			return;
		}

		debugManipulation(
			"{} player input packet: forward={}, backward={}, left={}, right={}, jump={}, sneak={}, sprint={}",
			debugName(player),
			input.forward(),
			input.backward(),
			input.left(),
			input.right(),
			input.jump(),
			input.sneak(),
			input.sprint()
		);
	}

	public static void onManipulationLookPacket(ServerPlayerEntity player, float yaw, float pitch) {
		if (!isManipulationPacketServerThread(player)) {
			return;
		}

		UUID playerId = player.getUuid();
		MANIPULATION_LOOK_BY_CASTER.put(playerId, new ManipulationLookState(yaw, pitch));
		if (!isManipulatingCaster(player) && !isManipulationControlledTarget(player)) {
			return;
		}

		debugManipulation(
			"{} look packet: yaw={}, pitch={}",
			debugName(player),
			round3(yaw),
			round3(pitch)
		);
	}

	private static boolean isLovePowerActiveThisSecond(ServerPlayerEntity player) {
		return LOVE_POWER_ACTIVE_THIS_SECOND.getOrDefault(player.getUuid(), false);
	}

	private static boolean isControlLocked(PlayerEntity player) {
		UUID playerId = player.getUuid();
		return LOVE_LOCKED_TARGETS.containsKey(playerId)
			|| isDomainClashParticipantFrozen(player);
	}

	private static boolean isMagicSuppressed(PlayerEntity player) {
		return MANIPULATION_CASTER_BY_TARGET.containsKey(player.getUuid());
	}

	private static boolean isDomainClashParticipantFrozen(PlayerEntity player) {
		DomainClashState clash = domainClashStateForParticipant(player.getUuid());
		if (clash == null) {
			return false;
		}

		MinecraftServer server = player.getEntityWorld().getServer();
		return server != null && isDomainClashFrozen(clash, server.getTicks());
	}

	private static boolean isActionLocked(PlayerEntity player) {
		return isControlLocked(player);
	}

	public static boolean isPlayerControlLocked(ServerPlayerEntity player) {
		return isActionLocked(player);
	}

	public static boolean isManipulatingCaster(ServerPlayerEntity player) {
		return MANIPULATION_STATES.containsKey(player.getUuid()) && activeAbility(player) == MagicAbility.MANIPULATION;
	}

	public static boolean isManipulationControlledTarget(ServerPlayerEntity player) {
		return MANIPULATION_CASTER_BY_TARGET.containsKey(player.getUuid());
	}

	public static void beginManipulationMovementProxy(ServerPlayerEntity caster) {
		if (isManipulatingCaster(caster)) {
			debugManipulation(
				"{} movement proxy begin (noop): casterPos=[{}, {}, {}], yaw={}, pitch={}",
				debugName(caster),
				round3(caster.getX()),
				round3(caster.getY()),
				round3(caster.getZ()),
				round3(caster.getYaw()),
				round3(caster.getPitch())
			);
		}
	}

	public static void endManipulationMovementProxy(ServerPlayerEntity caster) {
		if (isManipulatingCaster(caster)) {
			debugManipulation(
				"{} movement proxy end (noop): casterPos=[{}, {}, {}], velocity=[{}, {}, {}]",
				debugName(caster),
				round3(caster.getX()),
				round3(caster.getY()),
				round3(caster.getZ()),
				round3(caster.getVelocity().x),
				round3(caster.getVelocity().y),
				round3(caster.getVelocity().z)
			);
		}
	}

	public static void beginManipulationInteractionProxy(ServerPlayerEntity caster) {
		if (!isManipulationPacketServerThread(caster)) {
			return;
		}

		if (!isManipulatingCaster(caster)) {
			return;
		}

		UUID casterId = caster.getUuid();
		if (MANIPULATION_INTERACTION_PROXY.containsKey(casterId)) {
			debugManipulation("{} interaction proxy begin skipped: already active", debugName(caster));
			return;
		}

		ManipulationState state = MANIPULATION_STATES.get(casterId);
		if (state == null) {
			debugManipulation("{} interaction proxy begin skipped: state missing", debugName(caster));
			return;
		}

		ServerPlayerEntity target = resolveManipulationTarget(caster.getEntityWorld().getServer(), state);
		if (target == null) {
			deactivateManipulation(caster, true, "target missing during interaction proxy");
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		MANIPULATION_INTERACTION_PROXY.put(casterId, new ManipulationProxyState(new Vec3d(caster.getX(), caster.getY(), caster.getZ()), caster.getYaw(), caster.getPitch()));
		caster.setPosition(target.getX(), target.getY(), target.getZ());
		caster.setYaw(target.getYaw());
		caster.setPitch(target.getPitch());
		caster.setHeadYaw(target.getHeadYaw());
		caster.setBodyYaw(target.getBodyYaw());
		debugManipulation(
			"{} interaction proxy begin: moved caster to target {} at [{}, {}, {}] yaw={} pitch={}",
			debugName(caster),
			target.getUuid(),
			round3(target.getX()),
			round3(target.getY()),
			round3(target.getZ()),
			round3(target.getYaw()),
			round3(target.getPitch())
		);
	}

	public static void endManipulationInteractionProxy(ServerPlayerEntity caster) {
		if (!isManipulationPacketServerThread(caster)) {
			return;
		}

		UUID casterId = caster.getUuid();
		ManipulationProxyState proxyState = MANIPULATION_INTERACTION_PROXY.remove(casterId);
		if (proxyState == null) {
			debugManipulation("{} interaction proxy end skipped: no active proxy", debugName(caster));
			return;
		}

		ManipulationState manipulationState = MANIPULATION_STATES.get(casterId);
		Vec3d restorePos = manipulationState == null ? proxyState.originalPos : manipulationState.lockedCasterPos;
		caster.setPosition(restorePos.x, restorePos.y, restorePos.z);
		caster.setYaw(proxyState.yaw);
		caster.setPitch(proxyState.pitch);
		caster.setHeadYaw(proxyState.yaw);
		caster.setBodyYaw(proxyState.yaw);
		debugManipulation(
			"{} interaction proxy end: restoredPos=[{}, {}, {}], yaw={}, pitch={}",
			debugName(caster),
			round3(restorePos.x),
			round3(restorePos.y),
			round3(restorePos.z),
			round3(proxyState.yaw),
			round3(proxyState.pitch)
		);
	}

	public static boolean shouldCancelManipulationEntityAttack(ServerPlayerEntity caster, PlayerInteractEntityC2SPacket packet) {
		if (!isManipulatingCaster(caster) || !isAttackInteraction(packet)) {
			return false;
		}

		Entity entity = packet.getEntity(caster.getEntityWorld());
		if (entity == null || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity == caster) {
			debugManipulation(
				"{} manipulation entity attack canceled: invalid entity target (entity={}, self={})",
				debugName(caster),
				entity == null ? "null" : entity.getClass().getSimpleName(),
				entity == caster
			);
			return true;
		}

		if (entity instanceof PersistentProjectileEntity projectile && !projectile.isAttackable()) {
			debugManipulation("{} manipulation entity attack canceled: non-attackable projectile {}", debugName(caster), projectile.getUuid());
			return true;
		}

		ManipulationState state = MANIPULATION_STATES.get(caster.getUuid());
		boolean cancel = state != null && entity.getUuid().equals(state.targetId);
		if (cancel) {
			debugManipulation("{} manipulation entity attack canceled: attempted to attack controlled target {}", debugName(caster), state.targetId);
		} else {
			debugManipulation("{} manipulation entity attack allowed: targetEntity={}", debugName(caster), entity.getUuid());
		}
		return cancel;
	}

	public static int resetCooldown(ServerPlayerEntity player, MagicAbility ability) {
		UUID playerId = player.getUuid();

		if (ability == MagicAbility.ABSOLUTE_ZERO) {
			return ABSOLUTE_ZERO_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.PLANCK_HEAT) {
			return PLANCK_HEAT_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.TILL_DEATH_DO_US_PART) {
			return TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.MANIPULATION) {
			boolean removed = MANIPULATION_COOLDOWN_END_TICK.remove(playerId) != null;
			MANIPULATION_NEXT_REQUEST_TICK.remove(playerId);
			MANIPULATION_LAST_CLAMP_LOG_TICK.remove(playerId);
			MANIPULATION_INTERACTION_PROXY.remove(playerId);
			MANIPULATION_INPUT_BY_CASTER.remove(playerId);
			MANIPULATION_LOOK_BY_CASTER.remove(playerId);
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			boolean removed = FROST_DOMAIN_COOLDOWN_END_TICK.remove(playerId) != null;
			if (removed) {
				persistDomainRuntimeState(player.getEntityWorld().getServer());
			}
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
			boolean removed = LOVE_DOMAIN_COOLDOWN_END_TICK.remove(playerId) != null;
			if (removed) {
				persistDomainRuntimeState(player.getEntityWorld().getServer());
			}
			return removed ? 1 : 0;
		}

		return 0;
	}

	public static int resetAllCooldowns(ServerPlayerEntity player) {
		return resetCooldown(player, MagicAbility.ABSOLUTE_ZERO)
			+ resetCooldown(player, MagicAbility.PLANCK_HEAT)
			+ resetCooldown(player, MagicAbility.TILL_DEATH_DO_US_PART)
			+ resetCooldown(player, MagicAbility.MANIPULATION)
			+ resetCooldown(player, MagicAbility.FROST_DOMAIN_EXPANSION)
			+ resetCooldown(player, MagicAbility.LOVE_DOMAIN_EXPANSION);
	}

	public static void clearAllRuntimeState(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server == null) {
			return;
		}

		setActiveAbility(player, MagicAbility.NONE);
		player.setCameraEntity(player);
		MagicPlayerData.clearDomainClashUi(player);
		clearManipulationSuppressionTags(player);

		boolean domainStateChanged = false;

		DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.remove(playerId);
		if (ownedDomain != null) {
			cancelDomainClash(playerId, server);
			restoreDomainExpansion(server, ownedDomain);
			domainStateChanged = true;
		}

		ABSOLUTE_ZERO_COOLDOWN_END_TICK.remove(playerId);
		PLANCK_HEAT_COOLDOWN_END_TICK.remove(playerId);
		TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.remove(playerId);
		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
		TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK.remove(playerId);
		MANIPULATION_COOLDOWN_END_TICK.remove(playerId);
		MANIPULATION_NEXT_REQUEST_TICK.remove(playerId);
		MANIPULATION_LAST_CLAMP_LOG_TICK.remove(playerId);
		MANIPULATION_INTERACTION_PROXY.remove(playerId);
		MANIPULATION_INPUT_BY_CASTER.remove(playerId);
		MANIPULATION_LOOK_BY_CASTER.remove(playerId);
		ABSOLUTE_ZERO_NEXT_DAMAGE_TICK.remove(playerId);
		PLANCK_HEAT_STATES.remove(playerId);
		PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.remove(playerId);
		LOVE_POWER_ACTIVE_THIS_SECOND.remove(playerId);
		TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.remove(playerId);
		FROSTBITTEN_TARGETS.remove(playerId);
		ENHANCED_FIRE_TARGETS.remove(playerId);
		LOVE_LOCKED_TARGETS.remove(playerId);
		TILL_DEATH_DO_US_PART_STATES.remove(playerId);
		cancelDomainClashParticipant(playerId, server);
		if (FROST_DOMAIN_COOLDOWN_END_TICK.remove(playerId) != null) {
			domainStateChanged = true;
		}
		if (LOVE_DOMAIN_COOLDOWN_END_TICK.remove(playerId) != null) {
			domainStateChanged = true;
		}
		if (DOMAIN_PENDING_RETURNS.remove(playerId) != null) {
			domainStateChanged = true;
		}

		LOVE_LOCKED_TARGETS.entrySet().removeIf(entry -> entry.getValue().casterId.equals(playerId));
		List<UUID> linkedTillDeathCasters = new ArrayList<>();
		for (Map.Entry<UUID, TillDeathDoUsPartState> entry : TILL_DEATH_DO_US_PART_STATES.entrySet()) {
			if (playerId.equals(entry.getValue().linkedPlayerId)) {
				linkedTillDeathCasters.add(entry.getKey());
			}
		}
		for (UUID casterId : linkedTillDeathCasters) {
			ServerPlayerEntity linkedCaster = server.getPlayerManager().getPlayer(casterId);
			if (linkedCaster != null) {
				deactivateTillDeathDoUsPart(linkedCaster, TillDeathDoUsPartEndReason.LINK_TARGET_INVALID, false);
				continue;
			}

			TillDeathDoUsPartState state = TILL_DEATH_DO_US_PART_STATES.remove(casterId);
			if (state == null) {
				continue;
			}

			startTillDeathDoUsPartCooldown(casterId, server.getTicks());
		}

		ManipulationState ownManipulation = MANIPULATION_STATES.remove(playerId);
		if (ownManipulation != null) {
			releaseManipulationState(playerId, ownManipulation, server, player);
		}
		MANIPULATION_CASTER_BY_TARGET.entrySet().removeIf(entry -> entry.getValue().equals(playerId));

		UUID manipulatorId = MANIPULATION_CASTER_BY_TARGET.remove(playerId);
		if (manipulatorId != null) {
			ManipulationState manipulatorState = MANIPULATION_STATES.remove(manipulatorId);
			ServerPlayerEntity manipulator = server.getPlayerManager().getPlayer(manipulatorId);
			if (manipulatorState != null) {
				releaseManipulationState(manipulatorId, manipulatorState, server, manipulator);
			}
			MANIPULATION_LAST_CLAMP_LOG_TICK.remove(manipulatorId);
			MANIPULATION_INTERACTION_PROXY.remove(manipulatorId);
			MANIPULATION_INPUT_BY_CASTER.remove(manipulatorId);
			MANIPULATION_LOOK_BY_CASTER.remove(manipulatorId);
			if (manipulator != null && activeAbility(manipulator) == MagicAbility.MANIPULATION) {
				setActiveAbility(manipulator, MagicAbility.NONE);
				manipulator.setCameraEntity(manipulator);
			}
		}

		ABSOLUTE_ZERO_NEXT_DAMAGE_TICK.values().forEach(targets -> targets.remove(playerId));
		ABSOLUTE_ZERO_NEXT_DAMAGE_TICK.entrySet().removeIf(entry -> entry.getValue().isEmpty());
		PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.values().forEach(targets -> targets.remove(playerId));
		PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.entrySet().removeIf(entry -> entry.getValue().isEmpty());

		for (DomainExpansionState state : DOMAIN_EXPANSIONS.values()) {
			if (state.capturedEntities.remove(playerId) != null) {
				domainStateChanged = true;
			}
		}

		if (domainStateChanged) {
			persistDomainRuntimeState(server);
		}
	}

	private static void deactivateAbsoluteZero(ServerPlayerEntity player) {
		int cooldownEndTick = player.getEntityWorld().getServer().getTicks() + ABSOLUTE_ZERO_COOLDOWN_TICKS;
		ABSOLUTE_ZERO_COOLDOWN_END_TICK.put(player.getUuid(), cooldownEndTick);
		ABSOLUTE_ZERO_NEXT_DAMAGE_TICK.remove(player.getUuid());
	}

	private static void deactivatePlanckHeat(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		PLANCK_HEAT_STATES.remove(playerId);
		PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.remove(playerId);
	}

	private static void deactivateLoveAtFirstSight(ServerPlayerEntity player) {
		UUID casterId = player.getUuid();
		LOVE_POWER_ACTIVE_THIS_SECOND.put(casterId, false);
		LOVE_LOCKED_TARGETS.entrySet().removeIf(entry -> entry.getValue().casterId.equals(casterId));
	}

	private static void deactivateTillDeathDoUsPart(
		ServerPlayerEntity player,
		TillDeathDoUsPartEndReason reason,
		boolean sendFeedback
	) {
		UUID casterId = player.getUuid();
		TillDeathDoUsPartState state = TILL_DEATH_DO_US_PART_STATES.remove(casterId);
		if (state == null) {
			if (activeAbility(player) == MagicAbility.TILL_DEATH_DO_US_PART) {
				setActiveAbility(player, MagicAbility.NONE);
			}
			return;
		}

		if (reason == TillDeathDoUsPartEndReason.CASTER_DIED) {
			ServerPlayerEntity linkedPlayer = player.getEntityWorld().getServer().getPlayerManager().getPlayer(state.linkedPlayerId);
			if (linkedPlayer != null && linkedPlayer.isAlive()) {
				linkedPlayer.setHealth(0.0F);
			}
		}

		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(casterId);
		startTillDeathDoUsPartCooldown(casterId, player.getEntityWorld().getServer().getTicks());
		setActiveAbility(player, MagicAbility.NONE);

		if (sendFeedback) {
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.TILL_DEATH_DO_US_PART.displayName()), true);
		}
	}

	private static void startTillDeathDoUsPartCooldown(UUID playerId, int currentTick) {
		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
		if (TILL_DEATH_DO_US_PART_COOLDOWN_TICKS <= 0) {
			TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.remove(playerId);
			return;
		}

		TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.put(playerId, currentTick + TILL_DEATH_DO_US_PART_COOLDOWN_TICKS);
	}

	private static void deactivateManipulation(ServerPlayerEntity caster, boolean startCooldown) {
		deactivateManipulation(caster, startCooldown, "unspecified");
	}

	private static void deactivateManipulation(ServerPlayerEntity caster, boolean startCooldown, String reason) {
		UUID casterId = caster.getUuid();
		ManipulationState state = MANIPULATION_STATES.remove(casterId);
		MANIPULATION_LAST_CLAMP_LOG_TICK.remove(casterId);
		MANIPULATION_INTERACTION_PROXY.remove(casterId);
		MANIPULATION_INPUT_BY_CASTER.remove(casterId);
		MANIPULATION_LOOK_BY_CASTER.remove(casterId);
		if (state == null) {
			debugManipulation(
				"{} manipulation deactivate skipped (no state). reason={}, startCooldown={}",
				debugName(caster),
				reason,
				startCooldown
			);
			if (startCooldown) {
				MANIPULATION_COOLDOWN_END_TICK.put(
					casterId,
					caster.getEntityWorld().getServer().getTicks() + MANIPULATION_COOLDOWN_TICKS
				);
				debugManipulation(
					"{} manipulation deactivate(no-state) cooldown applied until tick {}",
					debugName(caster),
					caster.getEntityWorld().getServer().getTicks() + MANIPULATION_COOLDOWN_TICKS
				);
			}
			return;
		}

		debugManipulation(
			"{} manipulation deactivated. reason={}, startCooldown={}, targetId={}",
			debugName(caster),
			reason,
			startCooldown,
			state.targetId
		);
		releaseManipulationState(casterId, state, caster.getEntityWorld().getServer(), caster);
		if (startCooldown) {
			MANIPULATION_COOLDOWN_END_TICK.put(
				casterId,
				caster.getEntityWorld().getServer().getTicks() + MANIPULATION_COOLDOWN_TICKS
			);
			debugManipulation(
				"{} manipulation cooldown applied until tick {}",
				debugName(caster),
				caster.getEntityWorld().getServer().getTicks() + MANIPULATION_COOLDOWN_TICKS
			);
		}
		debugManipulation(
			"{} manipulation deactivation finalized: remainingStates={}, remainingTargets={}",
			debugName(caster),
			MANIPULATION_STATES.size(),
			MANIPULATION_CASTER_BY_TARGET.size()
		);
	}

	private static void releaseManipulationState(
		UUID casterId,
		ManipulationState state,
		MinecraftServer server,
		ServerPlayerEntity caster
	) {
		boolean removedMapping = state.targetId != null && MANIPULATION_CASTER_BY_TARGET.remove(state.targetId, casterId);
		MANIPULATION_INTERACTION_PROXY.remove(casterId);
		debugManipulation(
			"{} manipulation state release: targetId={}, mappingRemoved={}, serverTick={}",
			caster == null ? casterId : debugName(caster),
			state.targetId,
			removedMapping,
			server.getTicks()
		);

		if (state.targetId != null) {
			ServerPlayerEntity target = server.getPlayerManager().getPlayer(state.targetId);
			if (target != null) {
				clearManipulationSuppressionTags(target);
			}
		}

		if (caster != null) {
			debugManipulation("{} empty embrace state release complete", debugName(caster));
		}
	}

	private static MagicAbility activeAbility(PlayerEntity player) {
		return MagicAbility.fromSlotForSchool(
			MagicPlayerData.getActiveAbilitySlot(player),
			MagicPlayerData.getSchool(player)
		);
	}

	private static void setActiveAbility(ServerPlayerEntity player, MagicAbility ability) {
		MagicPlayerData.setActiveAbilitySlot(player, ability.slot());
	}

	private static boolean isAttackInteraction(PlayerInteractEntityC2SPacket packet) {
		final boolean[] attack = { false };
		packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
			@Override
			public void interact(Hand hand) {
			}

			@Override
			public void interactAt(Hand hand, Vec3d pos) {
			}

			@Override
			public void attack() {
				attack[0] = true;
			}
		});
		return attack[0];
	}

	private static final class DomainRuntimePersistentState extends PersistentState {
		private static final PersistentStateType<DomainRuntimePersistentState> TYPE = new PersistentStateType<>(
			"magic_domain_runtime",
			DomainRuntimePersistentState::new,
			NbtCompound.CODEC.xmap(DomainRuntimePersistentState::fromNbt, DomainRuntimePersistentState::toNbt),
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
		);

		private NbtCompound data;

		private DomainRuntimePersistentState() {
			this(new NbtCompound());
		}

		private DomainRuntimePersistentState(NbtCompound data) {
			this.data = data;
		}

		private static DomainRuntimePersistentState fromNbt(NbtCompound nbt) {
			return new DomainRuntimePersistentState(nbt.copy());
		}

		private static NbtCompound toNbt(DomainRuntimePersistentState state) {
			return state.data.copy();
		}

		private NbtCompound getDataCopy() {
			return data.copy();
		}

		private void setData(NbtCompound data) {
			this.data = data.copy();
			markDirty();
		}
	}

	private static final class FrostbiteState {
		private final RegistryKey<World> dimension;
		private final int expiresTick;
		private int nextDamageTick;

		private FrostbiteState(RegistryKey<World> dimension, int expiresTick, int nextDamageTick) {
			this.dimension = dimension;
			this.expiresTick = expiresTick;
			this.nextDamageTick = nextDamageTick;
		}
	}

	private static final class PlanckHeatState {
		private final int frostPhaseEndTick;
		private final int firePhaseEndTick;

		private PlanckHeatState(int frostPhaseEndTick, int firePhaseEndTick) {
			this.frostPhaseEndTick = frostPhaseEndTick;
			this.firePhaseEndTick = firePhaseEndTick;
		}
	}

	private static final class EnhancedFireState {
		private final RegistryKey<World> dimension;
		private int expiresTick;
		private int nextDamageTick;

		private EnhancedFireState(RegistryKey<World> dimension, int expiresTick, int nextDamageTick) {
			this.dimension = dimension;
			this.expiresTick = expiresTick;
			this.nextDamageTick = nextDamageTick;
		}
	}

	private static final class LoveLockState {
		private final RegistryKey<World> dimension;
		private final UUID casterId;
		private final double lockedX;
		private final double lockedY;
		private final double lockedZ;
		private int lastSeenTick;
		private int lastParticleTick;

		private LoveLockState(
			RegistryKey<World> dimension,
			UUID casterId,
			double lockedX,
			double lockedY,
			double lockedZ,
			int lastSeenTick
		) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.lockedX = lockedX;
			this.lockedY = lockedY;
			this.lockedZ = lockedZ;
			this.lastSeenTick = lastSeenTick;
			this.lastParticleTick = lastSeenTick;
		}
	}

	private enum TillDeathDoUsPartEndReason {
		MANUAL_CANCEL,
		LINK_EXPIRED,
		LINK_TARGET_INVALID,
		LINK_TARGET_DIED,
		CASTER_DIED,
		MANA_DEPLETED,
		SCHOOL_CHANGED
	}

	private static final class TillDeathDoUsPartState {
		private final int linkEndTick;
		private final UUID linkedPlayerId;
		private float sharedHealth;

		private TillDeathDoUsPartState(int linkEndTick, UUID linkedPlayerId, float sharedHealth) {
			this.linkEndTick = linkEndTick;
			this.linkedPlayerId = linkedPlayerId;
			this.sharedHealth = sharedHealth;
		}
	}

	private static final class DomainExpansionState {
		private final RegistryKey<World> dimension;
		private MagicAbility ability;
		private int expiresTick;
		private final double centerX;
		private final double centerZ;
		private final int baseY;
		private final int radius;
		private final int height;
		private final int innerRadius;
		private final int innerHeight;
		private int activationTick;
		private boolean clashOccurred;
		private double cooldownMultiplier;
		private final Map<BlockPos, DomainSavedBlockState> savedBlocks;
		private final Map<BlockPos, BlockState> protectedShellStates;
		private final Map<UUID, DomainCapturedEntityState> capturedEntities;

		private DomainExpansionState(
			RegistryKey<World> dimension,
			MagicAbility ability,
			int expiresTick,
			double centerX,
			double centerZ,
			int baseY,
			int radius,
			int height,
			int innerRadius,
			int innerHeight,
			int activationTick,
			boolean clashOccurred,
			double cooldownMultiplier,
			Map<BlockPos, DomainSavedBlockState> savedBlocks,
			Map<BlockPos, BlockState> protectedShellStates,
			Map<UUID, DomainCapturedEntityState> capturedEntities
		) {
			this.dimension = dimension;
			this.ability = ability;
			this.expiresTick = expiresTick;
			this.centerX = centerX;
			this.centerZ = centerZ;
			this.baseY = baseY;
			this.radius = radius;
			this.height = height;
			this.innerRadius = innerRadius;
			this.innerHeight = innerHeight;
			this.activationTick = activationTick;
			this.clashOccurred = clashOccurred;
			this.cooldownMultiplier = cooldownMultiplier;
			this.savedBlocks = savedBlocks;
			this.protectedShellStates = protectedShellStates;
			this.capturedEntities = capturedEntities;
		}
	}

	private static final class DomainClashState {
		private final UUID ownerId;
		private final MagicAbility ownerAbility;
		private final UUID challengerId;
		private final MagicAbility challengerAbility;
		private final Vec3d ownerLockedPos;
		private final Vec3d challengerLockedPos;
		private final int startTick;
		private final int titleEndTick;
		private final int instructionsFadeStartTick;
		private final int combatStartTick;
		private double ownerDamageDealt;
		private double challengerDamageDealt;

		private DomainClashState(
			UUID ownerId,
			MagicAbility ownerAbility,
			UUID challengerId,
			MagicAbility challengerAbility,
			Vec3d ownerLockedPos,
			Vec3d challengerLockedPos,
			int startTick,
			int titleEndTick,
			int instructionsFadeStartTick,
			int combatStartTick
		) {
			this.ownerId = ownerId;
			this.ownerAbility = ownerAbility;
			this.challengerId = challengerId;
			this.challengerAbility = challengerAbility;
			this.ownerLockedPos = ownerLockedPos;
			this.challengerLockedPos = challengerLockedPos;
			this.startTick = startTick;
			this.titleEndTick = titleEndTick;
			this.instructionsFadeStartTick = instructionsFadeStartTick;
			this.combatStartTick = combatStartTick;
		}
	}

	private static final class DomainOwnerState {
		private final UUID ownerId;
		private final DomainExpansionState state;

		private DomainOwnerState(UUID ownerId, DomainExpansionState state) {
			this.ownerId = ownerId;
			this.state = state;
		}
	}

	private static final class DomainClashResolution {
		private final UUID ownerId;
		private final UUID winnerId;

		private DomainClashResolution(UUID ownerId, UUID winnerId) {
			this.ownerId = ownerId;
			this.winnerId = winnerId;
		}
	}

	private static final class DomainSavedBlockState {
		private final BlockState blockState;
		private final NbtCompound blockEntityNbt;

		private DomainSavedBlockState(BlockState blockState, NbtCompound blockEntityNbt) {
			this.blockState = blockState;
			this.blockEntityNbt = blockEntityNbt;
		}
	}

	private static final class DomainPendingReturnState {
		private final RegistryKey<World> dimension;
		private final double x;
		private final double y;
		private final double z;
		private final float yaw;
		private final float pitch;

		private DomainPendingReturnState(RegistryKey<World> dimension, double x, double y, double z, float yaw, float pitch) {
			this.dimension = dimension;
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}

	private static final class DomainCapturedEntityState {
		private final UUID entityId;
		private final boolean playerEntity;
		private final Vec3d position;
		private final float yaw;
		private final float pitch;
		private Vec3d lastSafePos;
		private float lastSafeYaw;
		private float lastSafePitch;

		private DomainCapturedEntityState(UUID entityId, boolean playerEntity, Vec3d position, float yaw, float pitch) {
			this.entityId = entityId;
			this.playerEntity = playerEntity;
			this.position = position;
			this.yaw = yaw;
			this.pitch = pitch;
			this.lastSafePos = position;
			this.lastSafeYaw = yaw;
			this.lastSafePitch = pitch;
		}
	}

	private enum ManipulationStage {
		WAITING,
		LINKED
	}

	private static final class ManipulationState {
		private ManipulationStage stage;
		private RegistryKey<World> targetDimension;
		private UUID targetId;
		private Vec3d lockedCasterPos;
		private Vec3d controlledPos;
		private final int startedTick;

		private ManipulationState(
			ManipulationStage stage,
			RegistryKey<World> targetDimension,
			UUID targetId,
			Vec3d lockedCasterPos,
			Vec3d controlledPos,
			int startedTick
		) {
			this.stage = stage;
			this.targetDimension = targetDimension;
			this.targetId = targetId;
			this.lockedCasterPos = lockedCasterPos;
			this.controlledPos = controlledPos;
			this.startedTick = startedTick;
		}

		private static ManipulationState waiting(int startedTick, Vec3d casterPos) {
			return new ManipulationState(
				ManipulationStage.WAITING,
				null,
				null,
				casterPos,
				Vec3d.ZERO,
				startedTick
			);
		}
	}

	private static final class ManipulationProxyState {
		private final Vec3d originalPos;
		private final float yaw;
		private final float pitch;

		private ManipulationProxyState(Vec3d originalPos, float yaw, float pitch) {
			this.originalPos = originalPos;
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}

	private static final class ManipulationLookState {
		private final float yaw;
		private final float pitch;

		private ManipulationLookState(float yaw, float pitch) {
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}
}
