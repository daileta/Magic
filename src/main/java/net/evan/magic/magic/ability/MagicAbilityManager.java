package net.evan.magic.magic.ability;
import net.evan.magic.magic.core.ability.MagicAbility;
import net.evan.magic.magic.ability.GreedAbilityRuntime;
import net.evan.magic.magic.ability.GreedDomainRuntime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.evan.magic.magic.core.MagicPlayerData;
import net.evan.magic.magic.core.MagicSchool;
import net.evan.magic.mixin.ArmorStandEntityAccessorMixin;
import net.evan.magic.mixin.BlockDisplayEntityAccessorMixin;
import net.evan.magic.mixin.DisplayEntityAccessorMixin;
import net.evan.magic.network.payload.CelestialGamaRayTraceOverlayPayload;
import net.evan.magic.network.payload.ConstellationOutlinePayload;
import net.evan.magic.network.payload.ConstellationWarningOverlayPayload;
import net.evan.magic.network.payload.CelestialGamaRayVisualPayload;
import net.evan.magic.network.payload.JesterJokeOverlayPayload;
import net.evan.magic.particle.AstralCataclysmBeamParticleEffect;
import net.evan.magic.particle.AstralCataclysmDownflowParticleEffect;
import net.evan.magic.particle.AstralCataclysmSpiralParticleEffect;
import net.evan.magic.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityPosition;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
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
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;


public final class MagicAbilityManager extends RuntimeResetService {
	private MagicAbilityManager() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		reloadConfigValues();
		ServerLifecycleEvents.SERVER_STARTED.register(MagicAbilityManager::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(MagicAbilityManager::onServerStopping);
		ServerChunkEvents.CHUNK_LOAD.register(GreedDomainRuntime::onChunkLoad);
		ServerTickEvents.END_SERVER_TICK.register(MagicAbilityManager::onEndServerTick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(MagicAbilityManager::onAllowLivingEntityDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayerEntity player) {
				onPlayerDeath(player);
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, damageSource, baseDamageTaken, damageTaken, blocked) -> {
			onLivingEntityAfterDamage(entity, damageSource, damageTaken);
			if (entity instanceof ServerPlayerEntity player) {
				onPlayerAfterDamage(player, damageSource, damageTaken);
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> applyPendingDomainReturn(handler.player, server));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onPlayerDisconnect(handler.player));
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
		FROST_CONFIG = config.frost;
		BURNING_PASSION_CONFIG = config.burningPassion;

		BELOW_FREEZING_MANA_DRAIN_PER_SECOND = 0;
		ABSOLUTE_ZERO_MANA_DRAIN_PER_SECOND = 0;
		LOVE_AT_FIRST_SIGHT_IDLE_DRAIN_PER_SECOND = config.mana.loveAtFirstSightIdleDrainPerSecond;
		LOVE_AT_FIRST_SIGHT_ACTIVE_DRAIN_PER_SECOND = config.mana.loveAtFirstSightActiveDrainPerSecond;
		MARTYRS_FLAME_DRAIN_PERCENT_PER_SECOND = MathHelper.clamp(config.mana.martyrsFlameDrainPercentPerSecond, 0.0, 100.0);
		TILL_DEATH_DO_US_PART_DRAIN_PERCENT_PER_SECOND = MathHelper.clamp(config.mana.tillDeathDoUsPartDrainPercentPerSecond, 0.0, 100.0);
		MANIPULATION_ACTIVATION_MANA_COST = Math.max(0, config.mana.emptyEmbraceActivationCost);
		MANIPULATION_MANA_DRAIN_PER_SECOND = Math.max(0.0, config.mana.emptyEmbraceDrainPerSecond);
		DOMAIN_EXPANSION_ACTIVATION_MANA_COST = config.mana.domainExpansionActivationCost;
		PASSIVE_MANA_REGEN_PER_SECOND = MathHelper.clamp(config.mana.passiveRegenPerSecond, 0, 100);
		DEPLETED_RECOVERY_REGEN_PER_SECOND = MathHelper.clamp(config.mana.depletedRecoveryRegenPerSecond, 0, 100);
		PLANCK_HEAT_ACTIVATION_MANA_COST = 0;
		BELOW_FREEZING_COOLDOWN_TICKS = FROST_CONFIG.stagedModeCooldownTicks;
		FROST_ASCENT_COOLDOWN_TICKS = FROST_CONFIG.stageAdvanceCooldownTicks;

		EFFECT_REFRESH_TICKS = config.timing.effectRefreshTicks;
		FROSTBITE_DURATION_TICKS = config.timing.frostbiteDurationTicks;
		FROST_VISUAL_TICKS = FROST_CONFIG.debuff.visualFrozenTicks;
		ABSOLUTE_ZERO_AURA_DAMAGE_INTERVAL_TICKS = Math.max(1, config.timing.absoluteZeroAuraDamageIntervalTicks);
		ABSOLUTE_ZERO_COOLDOWN_TICKS = FROST_CONFIG.maximum.cooldownTicks;
		MARTYRS_FLAME_COOLDOWN_TICKS = Math.max(0, config.timing.martyrsFlameCooldownTicks);
		MARTYRS_FLAME_FIRE_DURATION_TICKS = Math.max(0, config.timing.martyrsFlameFireDurationTicks);
		PLANCK_HEAT_COOLDOWN_TICKS = FROST_CONFIG.stageAttackCooldownTicks;
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
		FROST_DOMAIN_COOLDOWN_TICKS = Math.max(0, FROST_CONFIG.domain.cooldownTicks);
		LOVE_DOMAIN_COOLDOWN_TICKS = config.timing.loveDomainCooldownTicks;
		DOMAIN_CLASH_REGENERATION_REFRESH_TICKS = Math.max(1, config.timing.domainClashRegenerationRefreshTicks);

		FROSTBITE_DAMAGE_INTERVAL_TICKS = FROST_CONFIG.debuff.intervalTicks;
		POWDERED_SNOW_DAMAGE_ON_INITIAL_APPLICATION = FROST_CONFIG.debuff.damageOnInitialApplication;

		FROSTBITE_DAMAGE = FROST_CONFIG.debuff.baseDamagePerTick;
		ABSOLUTE_ZERO_AURA_DAMAGE = Math.max(0.0F, config.damage.absoluteZeroAuraDamage);
		MARTYRS_FLAME_RETALIATION_DAMAGE = Math.max(0.0F, config.damage.martyrsFlameRetaliationDamage);
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
		DOMAIN_CLASH_RESISTANCE_AMPLIFIER = Math.max(0, config.potionEffects.domainClashResistanceAmplifier);

		LOVE_AT_FIRST_SIGHT_PARTICLE_INTERVAL_TICKS = Math.max(1, config.particles.loveAtFirstSightParticleIntervalTicks);
		LOVE_AT_FIRST_SIGHT_HEART_PARTICLES = Math.max(0, config.particles.loveAtFirstSightHeartParticles);
		LOVE_AT_FIRST_SIGHT_HAPPY_VILLAGER_PARTICLES = Math.max(0, config.particles.loveAtFirstSightHappyVillagerParticles);
		LOVE_AT_FIRST_SIGHT_RESISTANCE_AMPLIFIER = Math.max(-1, config.loveAtFirstSight.resistanceAmplifier);
		LOVE_AT_FIRST_SIGHT_BLOCK_ITEM_USE = config.loveAtFirstSight.blockItemUse;
		LOVE_AT_FIRST_SIGHT_BLOCK_ATTACKS = config.loveAtFirstSight.blockAttacks;
		LOVE_AT_FIRST_SIGHT_PREVENT_CASTER_DIRECT_DAMAGE = config.loveAtFirstSight.preventCasterDirectDamage;
		MARTYRS_FLAME_APPLY_GLOWING_EFFECT = config.martyrsFlame.applyGlowingEffect;
		MARTYRS_FLAME_DISABLE_MANA_REGEN_WHILE_ACTIVE = config.martyrsFlame.disableManaRegenWhileActive;
		MARTYRS_FLAME_FIRE_IGNORES_NORMAL_EXTINGUISH = config.martyrsFlame.fireIgnoresNormalExtinguish;
		MARTYRS_FLAME_FIRE_PARTICLE_INTERVAL_TICKS = Math.max(1, config.martyrsFlame.fireParticleIntervalTicks);
		MARTYRS_FLAME_FIRE_FLAME_PARTICLE_COUNT = Math.max(0, config.martyrsFlame.fireFlameParticleCount);
		MARTYRS_FLAME_FIRE_SMOKE_PARTICLE_COUNT = Math.max(0, config.martyrsFlame.fireSmokeParticleCount);
		DOMAIN_CONTROL_BLOCK_TELEPORT_ACROSS_BOUNDARIES = config.domainControl.blockTeleportAcrossDomainBoundaries;
		DOMAIN_CONTROL_INTERIOR_LIGHTING_ENABLED = config.domainControl.interiorLightingEnabled;
		DOMAIN_CONTROL_INTERIOR_LIGHT_HORIZONTAL_SPACING = Math.max(1, config.domainControl.interiorLightHorizontalSpacing);
		DOMAIN_CONTROL_INTERIOR_LIGHT_VERTICAL_SPACING = Math.max(1, config.domainControl.interiorLightVerticalSpacing);
		DOMAIN_CONTROL_INTERIOR_LIGHT_START_Y_OFFSET = Math.max(1, config.domainControl.interiorLightStartYOffset);
		DOMAIN_INTERIOR_LIGHT_STATE = Blocks.LIGHT.getDefaultState().with(
			LightBlock.LEVEL_15,
			MathHelper.clamp(config.domainControl.interiorLightLevel, 0, 15)
		);
		SPOTLIGHT_DETECTION_RANGE = Math.max(1.0, config.jesterSpotlight.detectionRange);
		SPOTLIGHT_EFFECT_REFRESH_TICKS = Math.max(1, config.jesterSpotlight.effectRefreshTicks);
		SPOTLIGHT_STAGE_ONE = spotlightStageSettings(config.jesterSpotlight.stageOne);
		SPOTLIGHT_STAGE_TWO = spotlightStageSettings(config.jesterSpotlight.stageTwo);
		SPOTLIGHT_STAGE_THREE = spotlightStageSettings(config.jesterSpotlight.stageThree);
		SPOTLIGHT_MAX_HISTORY_TICKS = Math.max(
			SPOTLIGHT_STAGE_ONE.maxRelevantWindowTicks(),
			Math.max(SPOTLIGHT_STAGE_TWO.maxRelevantWindowTicks(), SPOTLIGHT_STAGE_THREE.maxRelevantWindowTicks())
		);
		WITTY_ONE_LINER_ACTIVATION_COST_PERCENT = MathHelper.clamp(config.jesterWittyOneLiner.activationCostPercent, 0, 100);
		WITTY_ONE_LINER_RANGE = Math.max(1, config.jesterWittyOneLiner.targetRange);
		WITTY_ONE_LINER_OVERLAY_FADE_IN_TICKS = Math.max(0, config.jesterWittyOneLiner.overlayFadeInTicks);
		WITTY_ONE_LINER_OVERLAY_STAY_TICKS = Math.max(0, config.jesterWittyOneLiner.overlayStayTicks);
		WITTY_ONE_LINER_OVERLAY_FADE_OUT_TICKS = Math.max(0, config.jesterWittyOneLiner.overlayFadeOutTicks);
		WITTY_ONE_LINER_LOW_TIER = wittyOneLinerTierSettings(config.jesterWittyOneLiner.lowTier, 0xFFFFFF);
		WITTY_ONE_LINER_MID_TIER = wittyOneLinerTierSettings(config.jesterWittyOneLiner.midTier, 0xFFD54A);
		WITTY_ONE_LINER_HIGH_TIER = wittyOneLinerTierSettings(config.jesterWittyOneLiner.highTier, 0x39B7FF);
		COMEDIC_REWRITE_BASE_PROC_CHANCE_PERCENT = MathHelper.clamp(config.jesterComedicRewrite.baseProcChancePercent, 0.0, 100.0);
		COMEDIC_REWRITE_SEVERITY_PROC_BONUS_PERCENT = MathHelper.clamp(config.jesterComedicRewrite.severityProcBonusPercent, 0.0, 100.0);
		COMEDIC_REWRITE_LETHAL_PROC_BONUS_PERCENT = MathHelper.clamp(config.jesterComedicRewrite.lethalProcBonusPercent, 0.0, 100.0);
		COMEDIC_REWRITE_MAX_PROC_CHANCE_PERCENT = MathHelper.clamp(config.jesterComedicRewrite.maxProcChancePercent, 0.0, 100.0);
		COMEDIC_REWRITE_DANGEROUS_DAMAGE_THRESHOLD = Math.max(0.0F, config.jesterComedicRewrite.dangerousDamageThreshold);
		COMEDIC_REWRITE_DANGEROUS_HEALTH_FRACTION_THRESHOLD = MathHelper.clamp(
			config.jesterComedicRewrite.dangerousHealthFractionThreshold,
			0.0,
			1.0
		);
		COMEDIC_REWRITE_SEVERITY_DAMAGE_CAP = Math.max(
			COMEDIC_REWRITE_DANGEROUS_DAMAGE_THRESHOLD,
			config.jesterComedicRewrite.severityDamageCap
		);
		COMEDIC_REWRITE_MANA_COST_PERCENT = MathHelper.clamp(config.jesterComedicRewrite.manaCostPercent, 0.0, 100.0);
		COMEDIC_REWRITE_COOLDOWN_TICKS = Math.max(0, config.jesterComedicRewrite.cooldownTicks);
		COMEDIC_REWRITE_IMMUNITY_TICKS = Math.max(0, config.jesterComedicRewrite.postRewriteImmunityTicks);
		COMEDIC_REWRITE_FALL_PROTECTION_TICKS = Math.max(0, config.jesterComedicRewrite.postRewriteFallProtectionTicks);
		COMEDIC_REWRITE_EXTINGUISH_ON_REWRITE = config.jesterComedicRewrite.extinguishOnRewrite;
		COMEDIC_REWRITE_MIN_SAVED_HEALTH_HEARTS = Math.max(0.5, config.jesterComedicRewrite.minSavedHealthHearts);
		COMEDIC_REWRITE_MAX_SAVED_HEALTH_HEARTS = Math.max(
			COMEDIC_REWRITE_MIN_SAVED_HEALTH_HEARTS,
			config.jesterComedicRewrite.maxSavedHealthHearts
		);
		COMEDIC_REWRITE_SAFE_SEARCH_RADIUS = Math.max(1, config.jesterComedicRewrite.safeSearchRadius);
		COMEDIC_REWRITE_SAFE_SEARCH_VERTICAL_RANGE = Math.max(1, config.jesterComedicRewrite.safeSearchVerticalRange);
		COMEDIC_REWRITE_UNSAFE_Y_BUFFER_BLOCKS = Math.max(0, config.jesterComedicRewrite.unsafeYBufferBlocks);
		COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE = comedicRewriteLaunchSettings(config.jesterComedicRewrite.launchedThroughTheScene);
		COMEDIC_REWRITE_RAVAGER_BIT = comedicRewriteRavagerSettings(config.jesterComedicRewrite.ravagerBit);
		COMEDIC_REWRITE_PARROT_RESCUE = comedicRewriteParrotSettings(config.jesterComedicRewrite.parrotRescue);
		COMEDIC_ASSISTANT_ACTIVATION_COST_PERCENT = MathHelper.clamp(config.jesterComedicAssistant.activationCostPercent, 0.0, 100.0);
		COMEDIC_ASSISTANT_ARMED_DURATION_TICKS = Math.max(1, config.jesterComedicAssistant.armedDurationTicks);
		COMEDIC_ASSISTANT_PROC_COOLDOWN_TICKS = Math.max(0, config.jesterComedicAssistant.procCooldownTicks);
		COMEDIC_ASSISTANT_CANCEL_COOLDOWN_TICKS = Math.max(0, config.jesterComedicAssistant.cancelCooldownTicks);
		COMEDIC_ASSISTANT_ALLOW_PLAYER_TARGETS = config.jesterComedicAssistant.allowPlayerTargets;
		COMEDIC_ASSISTANT_ALLOW_MOB_TARGETS = config.jesterComedicAssistant.allowMobTargets;
		COMEDIC_ASSISTANT_ARMED_INDICATOR_REFRESH_TICKS = Math.max(1, config.jesterComedicAssistant.armedIndicatorRefreshTicks);
		COMEDIC_ASSISTANT_ARMED_PARTICLE_COUNT = Math.max(0, config.jesterComedicAssistant.armedParticleCount);
		COMEDIC_ASSISTANT_OVERLAY_FADE_IN_TICKS = Math.max(0, config.jesterComedicAssistant.overlayFadeInTicks);
		COMEDIC_ASSISTANT_OVERLAY_STAY_TICKS = Math.max(0, config.jesterComedicAssistant.overlayStayTicks);
		COMEDIC_ASSISTANT_OVERLAY_FADE_OUT_TICKS = Math.max(0, config.jesterComedicAssistant.overlayFadeOutTicks);
		COMEDIC_ASSISTANT_GIANT_SLIME_SLAM = comedicAssistantSlimeSettings(config.jesterComedicAssistant.giantSlimeSlam);
		COMEDIC_ASSISTANT_PANDA_BOWLING_BALL = comedicAssistantPandaSettings(config.jesterComedicAssistant.pandaBowlingBall);
		COMEDIC_ASSISTANT_PARROT_KIDNAPPING = comedicAssistantParrotSettings(config.jesterComedicAssistant.parrotKidnapping);
		COMEDIC_ASSISTANT_DIVINE_OVERREACTION = comedicAssistantDivineSettings(config.jesterComedicAssistant.divineOverreaction);
		COMEDIC_ASSISTANT_ACME_DROP = comedicAssistantAcmeSettings(config.jesterComedicAssistant.acmeDrop);
		COMEDIC_ASSISTANT_PIE_TO_THE_FACE = comedicAssistantPieSettings(config.jesterComedicAssistant.pieToTheFace);
		COMEDIC_ASSISTANT_GIANT_CANE_YANK = comedicAssistantCaneSettings(config.jesterComedicAssistant.giantCaneYank);
		PLUS_ULTRA_ACTIVATION_COST_PERCENT = MathHelper.clamp(config.jesterPlusUltra.activationCostPercent, 0.0, 100.0);
		PLUS_ULTRA_DURATION_TICKS = Math.max(1, config.jesterPlusUltra.durationTicks);
		PLUS_ULTRA_OUTLINE_RADIUS = Math.max(0.0, config.jesterPlusUltra.outlineRadius);
		PLUS_ULTRA_OUTLINE_REFRESH_TICKS = Math.max(1, config.jesterPlusUltra.outlineRefreshTicks);
		PLUS_ULTRA_INCOMING_DAMAGE_MULTIPLIER = MathHelper.clamp(config.jesterPlusUltra.incomingDamageMultiplier, 0.0, 1.0);
		PLUS_ULTRA_FLIGHT_ENABLED = config.jesterPlusUltra.flightEnabled;
		PLUS_ULTRA_ELYTRA_POSE_WHILE_FLYING = config.jesterPlusUltra.elytraPoseWhileFlying;
		PLUS_ULTRA_OVERHEAD_TEXT_ENABLED = config.jesterPlusUltra.overheadTextEnabled;
		PLUS_ULTRA_OVERHEAD_TEXT = config.jesterPlusUltra.overheadText == null || config.jesterPlusUltra.overheadText.isBlank()
			? "No More Jokes."
			: config.jesterPlusUltra.overheadText.trim();
		PLUS_ULTRA_OVERHEAD_TEXT_REFRESH_TICKS = Math.max(1, config.jesterPlusUltra.overheadTextRefreshTicks);
		PLUS_ULTRA_OVERHEAD_TEXT_DURATION_TICKS = Math.max(0, config.jesterPlusUltra.overheadTextDurationTicks);
		PLUS_ULTRA_OVERHEAD_TEXT_VERTICAL_OFFSET = Math.max(0.0, config.jesterPlusUltra.overheadTextVerticalOffset);
		PLUS_ULTRA_FLIGHT_FLY_SPEED = Math.max(0.0F, config.jesterPlusUltra.flightFlySpeed);
		PLUS_ULTRA_FLIGHT_SPRINT_SPEED_MULTIPLIER = Math.max(1.0, config.jesterPlusUltra.flightSprintSpeedMultiplier);
		PLUS_ULTRA_FLIGHT_ACCELERATION = Math.max(0.0, config.jesterPlusUltra.flightAcceleration);
		PLUS_ULTRA_FLIGHT_MAX_SPEED = Math.max(0.0, config.jesterPlusUltra.flightMaxSpeed);
		PLUS_ULTRA_FLIGHT_VERTICAL_ACCELERATION = Math.max(0.0, config.jesterPlusUltra.flightVerticalAcceleration);
		PLUS_ULTRA_FLIGHT_VERTICAL_MAX_SPEED = Math.max(0.0, config.jesterPlusUltra.flightVerticalMaxSpeed);
		PLUS_ULTRA_FLIGHT_DRAG = MathHelper.clamp(config.jesterPlusUltra.flightDrag, 0.0, 1.0);
		PLUS_ULTRA_MELEE_BONUS_DAMAGE = Math.max(0.0F, config.jesterPlusUltra.meleeBonusDamage);
		PLUS_ULTRA_ALLOW_PLAYER_TARGETS = config.jesterPlusUltra.allowPlayerTargets;
		PLUS_ULTRA_ALLOW_MOB_TARGETS = config.jesterPlusUltra.allowMobTargets;
		PLUS_ULTRA_FLING_HORIZONTAL_STRENGTH = Math.max(0.0, config.jesterPlusUltra.flingHorizontalStrength);
		PLUS_ULTRA_FLING_VERTICAL_STRENGTH = Math.max(0.0, config.jesterPlusUltra.flingVerticalStrength);
		PLUS_ULTRA_IMPACT_TRACKING_TICKS = Math.max(0, config.jesterPlusUltra.impactTrackingTicks);
		PLUS_ULTRA_IMPACT_VELOCITY_THRESHOLD = Math.max(0.0, config.jesterPlusUltra.impactVelocityThreshold);
		PLUS_ULTRA_IMPACT_DAMAGE_MULTIPLIER = Math.max(0.0, config.jesterPlusUltra.impactDamageMultiplier);
		PLUS_ULTRA_IMPACT_DAMAGE_MAX = Math.max(0.0, config.jesterPlusUltra.impactDamageMax);
		PLUS_ULTRA_SMOKE_PARTICLE_COUNT = Math.max(0, config.jesterPlusUltra.smokeParticleCount);
		PLUS_ULTRA_SMOKE_PARTICLE_SPREAD = Math.max(0.0, config.jesterPlusUltra.smokeParticleSpread);
		PLUS_ULTRA_SMOKE_PARTICLE_SPEED = Math.max(0.0, config.jesterPlusUltra.smokeParticleSpeed);
		PLUS_ULTRA_HIT_DUST_PARTICLE_COUNT = Math.max(0, config.jesterPlusUltra.hitDustParticleCount);
		PLUS_ULTRA_HIT_DUST_PARTICLE_SPREAD = Math.max(0.0, config.jesterPlusUltra.hitDustParticleSpread);
		PLUS_ULTRA_HIT_DUST_PARTICLE_SPEED = Math.max(0.0, config.jesterPlusUltra.hitDustParticleSpeed);
		PLUS_ULTRA_IMPACT_PARTICLE_COUNT = Math.max(0, config.jesterPlusUltra.impactParticleCount);
		PLUS_ULTRA_IMPACT_PARTICLE_SPREAD = Math.max(0.0, config.jesterPlusUltra.impactParticleSpread);
		PLUS_ULTRA_IMPACT_PARTICLE_SPEED = Math.max(0.0, config.jesterPlusUltra.impactParticleSpeed);
		PLUS_ULTRA_IMPACT_DUST_PARTICLE_COUNT = Math.max(0, config.jesterPlusUltra.impactDustParticleCount);
		PLUS_ULTRA_IMPACT_DUST_PARTICLE_SPREAD = Math.max(0.0, config.jesterPlusUltra.impactDustParticleSpread);
		PLUS_ULTRA_IMPACT_DUST_PARTICLE_SPEED = Math.max(0.0, config.jesterPlusUltra.impactDustParticleSpeed);
		PLUS_ULTRA_IMPACT_SOUND_VOLUME = Math.max(0.0F, config.jesterPlusUltra.impactSoundVolume);
		PLUS_ULTRA_IMPACT_SOUND_PITCH = Math.max(0.0F, config.jesterPlusUltra.impactSoundPitch);
		PLUS_ULTRA_EARLY_CANCEL_PENALTY_DURATION_TICKS = Math.max(0, config.jesterPlusUltra.earlyCancelPenaltyDurationTicks);
		PLUS_ULTRA_EARLY_CANCEL_SLOWNESS_AMPLIFIER = Math.max(-1, config.jesterPlusUltra.earlyCancelSlownessAmplifier);
		PLUS_ULTRA_EARLY_CANCEL_WEAKNESS_AMPLIFIER = Math.max(-1, config.jesterPlusUltra.earlyCancelWeaknessAmplifier);
		PLUS_ULTRA_FULL_END_PENALTY_DURATION_TICKS = Math.max(0, config.jesterPlusUltra.fullEndPenaltyDurationTicks);
		PLUS_ULTRA_FULL_END_SLOWNESS_AMPLIFIER = Math.max(-1, config.jesterPlusUltra.fullEndSlownessAmplifier);
		PLUS_ULTRA_FULL_END_WEAKNESS_AMPLIFIER = Math.max(-1, config.jesterPlusUltra.fullEndWeaknessAmplifier);
		PLUS_ULTRA_EARLY_CANCEL_COOLDOWN_TICKS = Math.max(0, config.jesterPlusUltra.earlyCancelCooldownTicks);
		PLUS_ULTRA_FULL_END_COOLDOWN_TICKS = Math.max(0, config.jesterPlusUltra.fullEndCooldownTicks);
		CASSIOPEIA_DETECTION_RADIUS = Math.max(1.0, config.constellationCassiopeia.detectionRadius);
		CASSIOPEIA_OUTLINE_REFRESH_TICKS = Math.max(1, config.constellationCassiopeia.outlineRefreshTicks);
		HERCULES_TARGET_RANGE = Math.max(1.0, config.constellationHercules.targetRange);
		HERCULES_SPLASH_RADIUS = Math.max(0.0, config.constellationHercules.splashRadius);
		HERCULES_EFFECT_DURATION_TICKS = Math.max(1, config.constellationHercules.effectDurationTicks);
		HERCULES_WARNING_FADE_IN_TICKS = Math.max(0, config.constellationHercules.warningFadeInTicks);
		HERCULES_WARNING_STAY_TICKS = Math.max(0, config.constellationHercules.warningStayTicks);
		HERCULES_WARNING_FADE_OUT_TICKS = Math.max(0, config.constellationHercules.warningFadeOutTicks);
		HERCULES_WARNING_SCALE = MathHelper.clamp(config.constellationHercules.warningScale, 0.5F, 3.0F);
		HERCULES_ACTIVATION_COST_PERCENT = MathHelper.clamp(config.constellationHercules.activationCostPercent, 0.0, 100.0);
		HERCULES_COOLDOWN_TICKS = Math.max(0, config.constellationHercules.cooldownTicks);
		HERCULES_TRUE_DAMAGE = Math.max(0.0F, config.constellationHercules.trueDamage);
		HERCULES_SLOWNESS_AMPLIFIER = Math.max(0, config.constellationHercules.slownessAmplifier);
		HERCULES_RESISTANCE_AMPLIFIER = Math.max(-1, config.constellationHercules.resistanceAmplifier);
		HERCULES_PARTICLE_INTERVAL_TICKS = Math.max(1, config.constellationHercules.particleIntervalTicks);
		HERCULES_WARNING_COLOR_RGB = parseColorRgb(config.constellationHercules.warningColorHex, 0x39B7FF);
		HERCULES_DISABLE_MANA_REGEN_WHILE_ACTIVE = config.constellationHercules.disableManaRegenWhileActive;
		HERCULES_PREVENT_CASTER_DIRECT_DAMAGE = config.constellationHercules.preventCasterDirectDamage;
		HERCULES_INTERRUPT_TARGET_ITEM_USE = config.constellationHercules.interruptTargetItemUse;
		HERCULES_ACTIVATION_IMPACT_DIRT_PARTICLE_COUNT = Math.max(0, config.constellationHercules.activationImpactDirtParticleCount);
		HERCULES_ACTIVATION_IMPACT_DIRT_PARTICLE_SPEED = Math.max(0.0, config.constellationHercules.activationImpactDirtParticleSpeed);
		HERCULES_ACTIVATION_IMPACT_SOUND_VOLUME = Math.max(0.0F, config.constellationHercules.activationImpactSoundVolume);
		HERCULES_ACTIVATION_IMPACT_SOUND_PITCH = Math.max(0.0F, config.constellationHercules.activationImpactSoundPitch);
		CELESTIAL_ALIGNMENT_CONFIG = config.constellationCelestialAlignment;
		ORIONS_GAMBIT_TARGET_RANGE = Math.max(1.0, config.constellationOrion.targetRange);
		ORIONS_GAMBIT_WAIT_CANCEL_COOLDOWN_TICKS = Math.max(0, config.constellationOrion.waitCancelCooldownTicks);
		ORIONS_GAMBIT_LINK_DURATION_TICKS = Math.max(1, config.constellationOrion.linkDurationTicks);
		ORIONS_GAMBIT_MANA_DRAIN_PERCENT_PER_SECOND = MathHelper.clamp(config.constellationOrion.manaDrainPercentPerSecond, 0.0, 100.0);
		ORIONS_GAMBIT_DRAIN_MANA_WHILE_WAITING_FOR_TARGET = config.constellationOrion.drainManaWhileWaitingForTarget;
		ORIONS_GAMBIT_DISABLE_MANA_REGEN_WHILE_WAITING_FOR_TARGET = config.constellationOrion.disableManaRegenWhileWaitingForTarget;
		ORIONS_GAMBIT_DISABLE_MANA_REGEN_WHILE_LINKED = config.constellationOrion.disableManaRegenWhileLinked;
		ORIONS_GAMBIT_COOLDOWN_TICKS = Math.max(0, config.constellationOrion.cooldownTicks);
		ORIONS_GAMBIT_CASTER_PENALTY_REFRESH_TICKS = Math.max(1, config.constellationOrion.casterPenaltyRefreshTicks);
		ORIONS_GAMBIT_CASTER_WEAKNESS_AMPLIFIER = Math.max(0, config.constellationOrion.casterWeaknessAmplifier);
		ORIONS_GAMBIT_CASTER_SLOWNESS_AMPLIFIER = Math.max(0, config.constellationOrion.casterSlownessAmplifier);
		ORIONS_GAMBIT_CASTER_MAX_HEALTH_HEARTS = Math.max(1.0, config.constellationOrion.casterMaxHealthHearts);
		ORIONS_GAMBIT_CASTER_INCOMING_DAMAGE_MULTIPLIER = Math.max(1.0, config.constellationOrion.casterIncomingDamageMultiplier);
		ORIONS_GAMBIT_CLEAR_TARGET_COOLDOWNS_ON_LOCK = config.constellationOrion.clearTargetCooldownsOnLock;
		ORIONS_GAMBIT_SUPPRESS_TARGET_MANA_COSTS = config.constellationOrion.suppressTargetManaCosts;
		ORIONS_GAMBIT_SUPPRESS_TARGET_COOLDOWNS = config.constellationOrion.suppressTargetCooldowns;
		ORIONS_GAMBIT_RESET_CASTER_COOLDOWNS_ON_END = config.constellationOrion.resetCasterCooldownsOnEnd;
		ORIONS_GAMBIT_APPLY_USED_TARGET_COOLDOWNS_ON_END = config.constellationOrion.applyUsedTargetCooldownsOnEnd;
		ORIONS_GAMBIT_GREED_TARGET_COIN_UNITS = Math.max(
			0,
			(int) Math.round(config.constellationOrion.greedTargetCoinAmount * MagicPlayerData.GREED_COIN_UNITS_PER_COIN)
		);
		ORIONS_GAMBIT_RESET_GREED_TARGET_COINS_ON_END = config.constellationOrion.resetGreedTargetCoinsOnEnd;
		ORIONS_GAMBIT_TARGET_PARTICLE_INTERVAL_TICKS = Math.max(1, config.constellationOrion.targetParticleIntervalTicks);
		ORIONS_GAMBIT_TARGET_PARTICLE_BURST_COUNT = Math.max(0, config.constellationOrion.targetParticleBurstCount);
		ORIONS_GAMBIT_TARGET_PARTICLE_SPAWN_RADIUS = Math.max(0.0, config.constellationOrion.targetParticleSpawnRadius);
		ORIONS_GAMBIT_TARGET_PARTICLE_VERTICAL_VELOCITY = Math.max(0.0, config.constellationOrion.targetParticleVerticalVelocity);
		ORIONS_GAMBIT_TARGET_PARTICLE_FORWARD_VELOCITY = Math.max(0.0, config.constellationOrion.targetParticleForwardVelocity);
		ORIONS_GAMBIT_TARGET_PARTICLE_SIDE_VELOCITY = Math.max(0.0, config.constellationOrion.targetParticleSideVelocity);
		ASTRAL_CATACLYSM_CHARGE_DURATION_TICKS = Math.max(1, config.constellationDomain.chargeDurationTicks);
		ASTRAL_CATACLYSM_ACQUIRE_WINDOW_TICKS = Math.max(1, config.constellationDomain.acquireWindowTicks);
		ASTRAL_CATACLYSM_DURATION_TICKS = Math.max(1, config.constellationDomain.durationTicks);
		ASTRAL_CATACLYSM_EXPIRY_WARNING_TICKS = List.copyOf(config.constellationDomain.expiryWarningTicks);
		ASTRAL_CATACLYSM_ALLOW_MOB_TARGETS = config.constellationDomain.allowMobTargets;
		ASTRAL_CATACLYSM_TARGET_RANGE = Math.max(1.0, config.constellationDomain.targetRange);
		ASTRAL_CATACLYSM_BEAM_RADIUS = Math.max(0.5, config.constellationDomain.beamRadius);
		ASTRAL_CATACLYSM_BEAM_RISE_BLOCKS_PER_SECOND = Math.max(0.0, config.constellationDomain.beamRiseBlocksPerSecond);
		ASTRAL_CATACLYSM_BEAM_PARTICLE_STEP = Math.max(0.25, config.constellationDomain.beamParticleStep);
		ASTRAL_CATACLYSM_BEAM_DESCENT_BLOCKS_PER_SECOND = Math.max(0.0, config.constellationDomain.beamDescentBlocksPerSecond);
		ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_COUNT = Math.max(0, config.constellationDomain.beamCoreParticleCount);
		ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_COUNT = Math.max(0, config.constellationDomain.beamOuterParticleCount);
		ASTRAL_CATACLYSM_BEAM_FALLING_PARTICLE_COUNT = Math.max(0, config.constellationDomain.beamFallingParticleCount);
		ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_LIFETIME_TICKS = Math.max(1, config.constellationDomain.beamCoreParticleLifetimeTicks);
		ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_LIFETIME_TICKS = Math.max(1, config.constellationDomain.beamOuterParticleLifetimeTicks);
		ASTRAL_CATACLYSM_BEAM_FALLING_PARTICLE_LIFETIME_TICKS = Math.max(1, config.constellationDomain.beamFallingParticleLifetimeTicks);
		ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_SCALE = Math.max(0.1F, config.constellationDomain.beamCoreParticleScale);
		ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_SCALE = Math.max(0.1F, config.constellationDomain.beamOuterParticleScale);
		ASTRAL_CATACLYSM_BEAM_FALLING_PARTICLE_SCALE = Math.max(0.1F, config.constellationDomain.beamFallingParticleScale);
		ASTRAL_CATACLYSM_BEAM_CORE_COLOR_RGB = parseColorRgb(config.constellationDomain.beamCoreColorHex, 0xFFF6D6, "Astral Cataclysm core");
		ASTRAL_CATACLYSM_BEAM_OUTER_COLOR_RGB = parseColorRgb(config.constellationDomain.beamOuterColorHex, 0x8AD7FF, "Astral Cataclysm outer");
		ASTRAL_CATACLYSM_BEAM_CORE_INTENSITY = MathHelper.clamp(config.constellationDomain.beamCoreIntensity, 0.05F, 1.0F);
		ASTRAL_CATACLYSM_BEAM_SPIRAL_PARTICLE_COUNT = Math.max(0, config.constellationDomain.beamSpiralParticleCount);
		ASTRAL_CATACLYSM_BEAM_SPIRAL_ORBIT_RADIUS = Math.max(0.0, config.constellationDomain.beamSpiralOrbitRadius);
		ASTRAL_CATACLYSM_BEAM_SPIRAL_ANGULAR_SPEED_RADIANS_PER_TICK = Math.toRadians(
			MathHelper.clamp(config.constellationDomain.beamSpiralAngularSpeedDegreesPerTick, -60.0, 60.0)
		);
		ASTRAL_CATACLYSM_BEAM_SPIRAL_VERTICAL_SPACING = Math.max(0.5, config.constellationDomain.beamSpiralVerticalSpacing);
		ASTRAL_CATACLYSM_BEAM_SPIRAL_COLOR_RGBS = parseColorRgbList(
			config.constellationDomain.beamSpiralColorHexes,
			List.of(0x8CD8FF, 0xFFDFA2, 0xC4A6FF),
			"Astral Cataclysm spiral"
		);
		ASTRAL_CATACLYSM_BEAM_SPIRAL_PARTICLE_LIFETIME_TICKS = Math.max(1, config.constellationDomain.beamSpiralParticleLifetimeTicks);
		ASTRAL_CATACLYSM_BEAM_SPIRAL_PARTICLE_SCALE = Math.max(0.1F, config.constellationDomain.beamSpiralParticleScale);
		ASTRAL_CATACLYSM_BEAM_DAMAGE_INTERVAL_TICKS = Math.max(1, config.constellationDomain.beamDamageIntervalTicks);
		ASTRAL_CATACLYSM_BEAM_TRUE_DAMAGE_PER_INTERVAL = Math.max(0.0F, config.constellationDomain.beamTrueDamagePerInterval);
		ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_INTERVAL_TICKS = Math.max(1, config.constellationDomain.beamHeavenlySoundIntervalTicks);
		ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_VOLUME = Math.max(0.0F, config.constellationDomain.beamHeavenlySoundVolume);
		ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_PITCH = Math.max(0.0F, config.constellationDomain.beamHeavenlySoundPitch);
		ASTRAL_CATACLYSM_COOLDOWN_TICKS = Math.max(0, config.constellationDomain.cooldownTicks);
		ASTRAL_CATACLYSM_FREEZE_REFRESH_TICKS = Math.max(1, config.constellationDomain.freezeRefreshTicks);
		ASTRAL_CATACLYSM_DISABLE_MANA_REGEN_WHILE_ACTIVE = config.constellationDomain.disableManaRegenWhileActive;
		ASTRAL_CATACLYSM_IGNORE_TOTEMS = config.constellationDomain.ignoreTotems;
		ASTRAL_CATACLYSM_PRESERVE_BEACON_ANCHOR = config.constellationDomain.preserveBeaconAnchor;
		ASTRAL_CATACLYSM_BEACON_ANCHOR_BLOCK_ID = parseIdentifierOrFallback(
			config.constellationDomain.beaconAnchorBlockId,
			Identifier.of("evanpack", "beacon_anchor")
		);
		ASTRAL_CATACLYSM_BEACON_CORE_ITEM_ID = parseIdentifierOrFallback(
			config.constellationDomain.beaconCoreItemId,
			Identifier.of("evanpack", "beacon_core")
		);
		ASTRAL_CATACLYSM_BEACON_CORE_ANCHOR_STATE_ID = config.constellationDomain.beaconCoreAnchorStateId;
		ASTRAL_CATACLYSM_BEACON_CORE_PROTECTION_RADIUS = Math.max(0.0, config.constellationDomain.beaconCoreProtectionRadius);
		ASTRAL_CATACLYSM_CANCEL_BEAM_ON_PROTECTED_BEACON_CORE_HOLDER = config.constellationDomain.cancelBeamOnProtectedBeaconCoreHolder;
		cachedBeaconCoreAnchor = null;
		cachedBeaconCoreAnchorTick = Integer.MIN_VALUE;

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
		DOMAIN_CLASH_PAUSE_TIMED_DOMAIN_COLLAPSE_TIMERS = config.domainClash.pauseTimedDomainCollapseTimers;
		DOMAIN_CLASH_PAUSE_ASTRAL_CATACLYSM_PHASE_TIMERS = config.domainClash.pauseAstralCataclysmPhaseTimers;
		GreedDomainRuntime.reloadConfigValues();
	}

	public static void onAbilityRequested(ServerPlayerEntity player, int abilitySlot) {
		if (sendFrostAbilityBlockedMessage(player)) {
			return;
		}

		if (isControlLocked(player)) {
			debugManipulation("{} ability request ignored: player is control-locked", debugName(player));
			return;
		}

		if (!MagicPlayerData.hasMagic(player)) {
			player.sendMessage(Text.translatable("message.magic.no_access"), true);
			return;
		}

		MagicSchool school = MagicPlayerData.getSchool(player);
		MagicAbility requestedAbility = resolveRequestedAbility(player, school, abilitySlot);
		if (requestedAbility == MagicAbility.NONE) {
			player.sendMessage(Text.translatable("message.magic.ability.not_implemented", abilitySlot), true);
			return;
		}
		if (isMagicSuppressed(player) && !isDomainExpansion(requestedAbility)) {
			player.sendMessage(Text.translatable("message.magic.empty_embrace.magic_blocked"), true);
			return;
		}
		if (queueCelestialAbilityRequest(player, abilitySlot)) {
			return;
		}

		UUID playerId = player.getUuid();
		int currentTick = player.getEntityWorld().getServer().getTicks();
		DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.get(playerId);
		boolean inDomainClash = domainClashStateForParticipant(playerId) != null;
		if (inDomainClash && isDomainExpansion(requestedAbility)) {
			player.sendMessage(Text.translatable("message.magic.domain.clash.active"), true);
			return;
		}

		if (isLockedByForeignLoveDomain(player) && !isDomainExpansion(requestedAbility)) {
			player.sendMessage(Text.translatable("message.magic.love_domain.abilities_locked"), true);
			return;
		}

		if (ownedDomain != null && requestedAbility == ownedDomain.ability && !inDomainClash) {
			if (requestedAbility == MagicAbility.ASTRAL_CATACLYSM) {
				handleAstralCataclysmRequest(player, currentTick);
				return;
			}

			deactivateDomainExpansion(player);
			setActiveAbility(player, MagicAbility.NONE);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", ownedDomain.ability.displayName()), true);
			return;
		}

		MagicAbility currentActiveAbility = activeAbility(player);

		if (currentActiveAbility == MagicAbility.ABSOLUTE_ZERO && requestedAbility != MagicAbility.ABSOLUTE_ZERO) {
			player.sendMessage(Text.translatable("message.magic.frost.maximum_locked"), true);
			return;
		}

		if (currentActiveAbility == MagicAbility.ORIONS_GAMBIT && requestedAbility != MagicAbility.ORIONS_GAMBIT) {
			player.sendMessage(Text.translatable("message.magic.ability.orions_gambit_locked"), true);
			return;
		}

		if (currentActiveAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW && requestedAbility != MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			player.sendMessage(Text.translatable("message.magic.constellation.celestial_alignment.active"), true);
			return;
		}

		if (currentActiveAbility == MagicAbility.PLUS_ULTRA && requestedAbility != MagicAbility.PLUS_ULTRA) {
			player.sendMessage(Text.translatable("message.magic.ability.plus_ultra_locked"), true);
			return;
		}

		if (!MagicConfig.get().abilityAccess.isAbilityUnlocked(playerId, requestedAbility)) {
			player.sendMessage(Text.translatable("message.magic.ability.locked", requestedAbility.displayName()), true);
			return;
		}

		if (GreedDomainRuntime.isMagicSealed(player, currentTick)) {
			player.sendMessage(Text.translatable("message.magic.greed.magic_sealed"), true);
			return;
		}

		if (currentActiveAbility != requestedAbility && GreedDomainRuntime.beforeAbilityUse(player, requestedAbility, currentTick)) {
			return;
		}

		if (requestedAbility == MagicAbility.MANIPULATION && isManipulationRequestDebounced(player, currentTick)) {
			return;
		}

		if (GreedAbilityRuntime.isAbilityUseLocked(player, currentTick)) {
			player.sendMessage(Text.translatable("message.magic.greed.ability_locked"), true);
			return;
		}

		if (GreedAbilityRuntime.handleAbilityRequest(player, requestedAbility)) {
			recordOrionsGambitAbilityUse(player, requestedAbility);
			return;
		}

		if (isDomainExpansion(requestedAbility)) {
			handleDomainExpansionRequest(player, requestedAbility, currentTick);
			return;
		}

		if (requestedAbility == MagicAbility.MARTYRS_FLAME) {
			handleMartyrsFlameRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.IGNITION) {
			handleIgnitionRequest(player, currentTick);
			return;
		}

		if (requestedAbility == MagicAbility.SEARING_DASH) {
			handleSearingDashRequest(player, currentTick);
			return;
		}

		if (requestedAbility == MagicAbility.CINDER_MARK) {
			handleCinderMarkRequest(player, currentTick, abilitySlot == 3);
			return;
		}

		if (requestedAbility == MagicAbility.OVERRIDE) {
			handleOverrideRequest(player, currentTick);
			return;
		}

		if (requestedAbility == MagicAbility.SPOTLIGHT) {
			handleSpotlightRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.CASSIOPEIA) {
			handleCassiopeiaRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			handleHerculesBurdenRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			handleSagittariusAstralArrowRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.ORIONS_GAMBIT) {
			handleOrionsGambitRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.WITTY_ONE_LINER) {
			handleWittyOneLinerRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.COMEDIC_REWRITE) {
			handleComedicRewriteRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.COMEDIC_ASSISTANT) {
			handleComedicAssistantRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.PLUS_ULTRA) {
			handlePlusUltraRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.BELOW_FREEZING) {
			handleBelowFreezingRequest(player);
			return;
		}

		if (requestedAbility == MagicAbility.FROST_ASCENT) {
			handleFrostAscentRequest(player);
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

	private static MagicAbility resolveRequestedAbility(ServerPlayerEntity player, MagicSchool school, int abilitySlot) {
		if (school != MagicSchool.BURNING_PASSION) {
			return MagicAbility.fromSlotForSchool(abilitySlot, school);
		}

		if (abilitySlot == 3) {
			BurningPassionIgnitionState ignitionState = burningPassionIgnitionState(player);
			return ignitionState != null && ignitionState.currentStage >= 2 ? MagicAbility.CINDER_MARK : MagicAbility.SEARING_DASH;
		}
		if (abilitySlot == 4) {
			return MagicAbility.CINDER_MARK;
		}
		return MagicAbility.fromSlotForSchool(abilitySlot, school);
	}
}

