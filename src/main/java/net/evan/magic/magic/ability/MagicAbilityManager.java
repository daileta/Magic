package net.evan.magic.magic.ability;

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
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.MagicSchool;
import net.evan.magic.mixin.ArmorStandEntityAccessorMixin;
import net.evan.magic.mixin.BlockDisplayEntityAccessorMixin;
import net.evan.magic.mixin.DisplayEntityAccessorMixin;
import net.evan.magic.network.payload.ConstellationOutlinePayload;
import net.evan.magic.network.payload.ConstellationWarningOverlayPayload;
import net.evan.magic.network.payload.JesterJokeOverlayPayload;
import net.evan.magic.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
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
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
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

public final class MagicAbilityManager {
	private static final int TICKS_PER_SECOND = 20;
	private static final int COOLDOWN_MESSAGE_DEBOUNCE_TICKS = 20;
	private static final float DEFAULT_PLAYER_FLY_SPEED = 0.05F;
	private static MagicConfig.FrostConfig FROST_CONFIG = new MagicConfig.FrostConfig();
	private static int BELOW_FREEZING_COOLDOWN_TICKS = 30 * TICKS_PER_SECOND;
	private static int FROST_ASCENT_COOLDOWN_TICKS = 0;
	private static int BELOW_FREEZING_MANA_DRAIN_PER_SECOND = 5;
	private static int ABSOLUTE_ZERO_MANA_DRAIN_PER_SECOND = 20;
	private static int LOVE_AT_FIRST_SIGHT_IDLE_DRAIN_PER_SECOND = 2;
	private static int LOVE_AT_FIRST_SIGHT_ACTIVE_DRAIN_PER_SECOND = 5;
	private static double MARTYRS_FLAME_DRAIN_PERCENT_PER_SECOND = 1.0;
	private static double TILL_DEATH_DO_US_PART_DRAIN_PERCENT_PER_SECOND = 3.0;
	private static int MANIPULATION_ACTIVATION_MANA_COST = 0;
	private static int MANIPULATION_MANA_DRAIN_PER_SECOND = 0;
	private static int PASSIVE_MANA_REGEN_PER_SECOND = 10;
	private static int DEPLETED_RECOVERY_REGEN_PER_SECOND = 5;
	private static int EFFECT_REFRESH_TICKS = 40;
	private static double SPOTLIGHT_DETECTION_RANGE = 48.0;
	private static int SPOTLIGHT_EFFECT_REFRESH_TICKS = 10;
	private static SpotlightStageSettings SPOTLIGHT_STAGE_ONE = SpotlightStageSettings.defaults(2, 200, 100, 200, 2.0, 0.08, 1.15, 0, 0, 2.0);
	private static SpotlightStageSettings SPOTLIGHT_STAGE_TWO = SpotlightStageSettings.defaults(5, 100, 100, 200, 4.0, 0.16, 1.3, 1, 1, 5.0);
	private static SpotlightStageSettings SPOTLIGHT_STAGE_THREE = SpotlightStageSettings.defaults(8, 100, 100, 200, 6.0, 0.24, 1.5, 2, 2, 10.0);
	private static int SPOTLIGHT_MAX_HISTORY_TICKS = 200;
	private static int WITTY_ONE_LINER_ACTIVATION_COST_PERCENT = 50;
	private static int WITTY_ONE_LINER_RANGE = 32;
	private static int WITTY_ONE_LINER_OVERLAY_FADE_IN_TICKS = 4;
	private static int WITTY_ONE_LINER_OVERLAY_STAY_TICKS = 50;
	private static int WITTY_ONE_LINER_OVERLAY_FADE_OUT_TICKS = 10;
	private static double COMEDIC_REWRITE_BASE_PROC_CHANCE_PERCENT = 50.0;
	private static double COMEDIC_REWRITE_SEVERITY_PROC_BONUS_PERCENT = 0.0;
	private static double COMEDIC_REWRITE_LETHAL_PROC_BONUS_PERCENT = 0.0;
	private static double COMEDIC_REWRITE_MAX_PROC_CHANCE_PERCENT = 50.0;
	private static float COMEDIC_REWRITE_DANGEROUS_DAMAGE_THRESHOLD = 8.0F;
	private static double COMEDIC_REWRITE_DANGEROUS_HEALTH_FRACTION_THRESHOLD = 0.45;
	private static float COMEDIC_REWRITE_SEVERITY_DAMAGE_CAP = 20.0F;
	private static double COMEDIC_REWRITE_MANA_COST_PERCENT = 20.0;
	private static int COMEDIC_REWRITE_COOLDOWN_TICKS = 25 * TICKS_PER_SECOND;
	private static int COMEDIC_REWRITE_IMMUNITY_TICKS = TICKS_PER_SECOND;
	private static int COMEDIC_REWRITE_FALL_PROTECTION_TICKS = 4 * TICKS_PER_SECOND;
	private static boolean COMEDIC_REWRITE_EXTINGUISH_ON_REWRITE = true;
	private static double COMEDIC_REWRITE_MIN_SAVED_HEALTH_HEARTS = 1.0;
	private static double COMEDIC_REWRITE_MAX_SAVED_HEALTH_HEARTS = 4.0;
	private static int COMEDIC_REWRITE_SAFE_SEARCH_RADIUS = 14;
	private static int COMEDIC_REWRITE_SAFE_SEARCH_VERTICAL_RANGE = 10;
	private static int COMEDIC_REWRITE_UNSAFE_Y_BUFFER_BLOCKS = 16;
	private static double COMEDIC_REWRITE_VISUAL_VIEW_DISTANCE = 48.0;
	private static ComedicRewriteLaunchSettings COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE = ComedicRewriteLaunchSettings.defaults(
		50,
		1.6,
		1.2,
		0.6,
		0.35,
		40,
		2,
		18,
		8
	);
	private static ComedicRewriteRavagerSettings COMEDIC_REWRITE_RAVAGER_BIT = ComedicRewriteRavagerSettings.defaults(
		25,
		1.35,
		1.05,
		0.5,
		0.25,
		60,
		3,
		16,
		20,
		true,
		38,
		2.0,
		0.0,
		2.0,
		0.45,
		6
	);
	private static ComedicRewriteParrotSettings COMEDIC_REWRITE_PARROT_RESCUE = ComedicRewriteParrotSettings.defaults(
		25,
		5.0,
		3.0,
		0.35,
		0.2,
		60,
		12,
		18,
		true,
		32,
		3,
		0.9,
		0.2,
		true,
		0.15
	);
	private static double COMEDIC_ASSISTANT_ACTIVATION_COST_PERCENT = 25.0;
	private static int COMEDIC_ASSISTANT_ARMED_DURATION_TICKS = 15 * TICKS_PER_SECOND;
	private static int COMEDIC_ASSISTANT_PROC_COOLDOWN_TICKS = 15 * TICKS_PER_SECOND;
	private static int COMEDIC_ASSISTANT_CANCEL_COOLDOWN_TICKS = 3 * TICKS_PER_SECOND;
	private static boolean COMEDIC_ASSISTANT_ALLOW_PLAYER_TARGETS = true;
	private static boolean COMEDIC_ASSISTANT_ALLOW_MOB_TARGETS = true;
	private static int COMEDIC_ASSISTANT_ARMED_INDICATOR_REFRESH_TICKS = TICKS_PER_SECOND;
	private static int COMEDIC_ASSISTANT_ARMED_PARTICLE_COUNT = 4;
	private static int COMEDIC_ASSISTANT_OVERLAY_FADE_IN_TICKS = 4;
	private static int COMEDIC_ASSISTANT_OVERLAY_STAY_TICKS = 30;
	private static int COMEDIC_ASSISTANT_OVERLAY_FADE_OUT_TICKS = 8;
	private static final int COMEDIC_ASSISTANT_OVERLAY_COLOR_RGB = 0xFFD54A;
	private static ComedicAssistantSlimeSettings COMEDIC_ASSISTANT_GIANT_SLIME_SLAM = ComedicAssistantSlimeSettings.defaults(
		true,
		1,
		4.0F,
		60,
		1,
		30 * TICKS_PER_SECOND,
		0,
		30,
		6.0,
		1.3,
		6,
		24,
		0.85F,
		0.8F,
		1.15F,
		0.85F
	);
	private static ComedicAssistantPandaSettings COMEDIC_ASSISTANT_PANDA_BOWLING_BALL = ComedicAssistantPandaSettings.defaults(
		true,
		1,
		5.0F,
		3.3,
		0.8,
		30,
		0,
		18,
		3.0,
		2.6,
		18,
		1.0F,
		0.95F,
		1.0F,
		0.8F
	);
	private static ComedicAssistantParrotSettings COMEDIC_ASSISTANT_PARROT_KIDNAPPING = ComedicAssistantParrotSettings.defaults(
		true,
		1,
		2.0F,
		35.0,
		1.55,
		30,
		1.2,
		true,
		30,
		28,
		10,
		1.35,
		0.35,
		28,
		6,
		0.8F,
		1.25F
	);
	private static ComedicAssistantDivineSettings COMEDIC_ASSISTANT_DIVINE_OVERREACTION = ComedicAssistantDivineSettings.defaults(
		true,
		1,
		10.0F,
		20,
		1.5,
		20,
		20,
		0,
		40,
		0,
		40,
		0.8F,
		1.0F
	);
	private static ComedicAssistantAcmeSettings COMEDIC_ASSISTANT_ACME_DROP = ComedicAssistantAcmeSettings.defaults(
		true,
		1,
		6.0F,
		30,
		5,
		60,
		0,
		18,
		10.0,
		20,
		1.25F,
		0.75F
	);
	private static ComedicAssistantPieSettings COMEDIC_ASSISTANT_PIE_TO_THE_FACE = ComedicAssistantPieSettings.defaults(
		true,
		1,
		2.0F,
		5 * TICKS_PER_SECOND,
		0,
		60,
		0,
		24,
		0.9F,
		1.15F
	);
	private static ComedicAssistantCaneSettings COMEDIC_ASSISTANT_GIANT_CANE_YANK = ComedicAssistantCaneSettings.defaults(
		true,
		1,
		3.0F,
		15.2,
		0.9,
		6,
		45,
		0.75,
		3.5,
		24.0,
		40,
		0,
		14,
		4.75,
		11.2,
		12,
		0.95F,
		0.95F
	);
	private static double PLUS_ULTRA_ACTIVATION_COST_PERCENT = 80.0;
	private static int PLUS_ULTRA_DURATION_TICKS = 30 * TICKS_PER_SECOND;
	private static double PLUS_ULTRA_OUTLINE_RADIUS = 150.0;
	private static int PLUS_ULTRA_OUTLINE_REFRESH_TICKS = 10;
	private static double PLUS_ULTRA_INCOMING_DAMAGE_MULTIPLIER = 0.1;
	private static boolean PLUS_ULTRA_FLIGHT_ENABLED = true;
	private static boolean PLUS_ULTRA_ELYTRA_POSE_WHILE_FLYING = true;
	private static boolean PLUS_ULTRA_OVERHEAD_TEXT_ENABLED = true;
	private static String PLUS_ULTRA_OVERHEAD_TEXT = "No More Jokes.";
	private static int PLUS_ULTRA_OVERHEAD_TEXT_REFRESH_TICKS = 2;
	private static int PLUS_ULTRA_OVERHEAD_TEXT_DURATION_TICKS = 3 * TICKS_PER_SECOND;
	private static double PLUS_ULTRA_OVERHEAD_TEXT_VERTICAL_OFFSET = 2.85;
	private static float PLUS_ULTRA_FLIGHT_FLY_SPEED = 0.12F;
	private static double PLUS_ULTRA_FLIGHT_ACCELERATION = 0.28;
	private static double PLUS_ULTRA_FLIGHT_MAX_SPEED = 3.75;
	private static double PLUS_ULTRA_FLIGHT_VERTICAL_ACCELERATION = 0.16;
	private static double PLUS_ULTRA_FLIGHT_VERTICAL_MAX_SPEED = 1.35;
	private static double PLUS_ULTRA_FLIGHT_DRAG = 0.95;
	private static float PLUS_ULTRA_MELEE_BONUS_DAMAGE = 6.0F;
	private static boolean PLUS_ULTRA_ALLOW_PLAYER_TARGETS = true;
	private static boolean PLUS_ULTRA_ALLOW_MOB_TARGETS = true;
	private static double PLUS_ULTRA_FLING_HORIZONTAL_STRENGTH = 5.75;
	private static double PLUS_ULTRA_FLING_VERTICAL_STRENGTH = 1.05;
	private static int PLUS_ULTRA_IMPACT_TRACKING_TICKS = 35;
	private static double PLUS_ULTRA_IMPACT_VELOCITY_THRESHOLD = 1.1;
	private static double PLUS_ULTRA_IMPACT_DAMAGE_MULTIPLIER = 3.0;
	private static double PLUS_ULTRA_IMPACT_DAMAGE_MAX = 16.0;
	private static int PLUS_ULTRA_SMOKE_PARTICLE_COUNT = 16;
	private static double PLUS_ULTRA_SMOKE_PARTICLE_SPREAD = 0.4;
	private static double PLUS_ULTRA_SMOKE_PARTICLE_SPEED = 0.05;
	private static int PLUS_ULTRA_HIT_DUST_PARTICLE_COUNT = 10;
	private static double PLUS_ULTRA_HIT_DUST_PARTICLE_SPREAD = 0.28;
	private static double PLUS_ULTRA_HIT_DUST_PARTICLE_SPEED = 0.02;
	private static int PLUS_ULTRA_IMPACT_PARTICLE_COUNT = 28;
	private static double PLUS_ULTRA_IMPACT_PARTICLE_SPREAD = 0.55;
	private static double PLUS_ULTRA_IMPACT_PARTICLE_SPEED = 0.12;
	private static int PLUS_ULTRA_IMPACT_DUST_PARTICLE_COUNT = 20;
	private static double PLUS_ULTRA_IMPACT_DUST_PARTICLE_SPREAD = 0.4;
	private static double PLUS_ULTRA_IMPACT_DUST_PARTICLE_SPEED = 0.05;
	private static float PLUS_ULTRA_IMPACT_SOUND_VOLUME = 1.0F;
	private static float PLUS_ULTRA_IMPACT_SOUND_PITCH = 0.8F;
	private static int PLUS_ULTRA_EARLY_CANCEL_PENALTY_DURATION_TICKS = 15 * TICKS_PER_SECOND;
	private static int PLUS_ULTRA_EARLY_CANCEL_SLOWNESS_AMPLIFIER = 0;
	private static int PLUS_ULTRA_EARLY_CANCEL_WEAKNESS_AMPLIFIER = 0;
	private static int PLUS_ULTRA_FULL_END_PENALTY_DURATION_TICKS = 30 * TICKS_PER_SECOND;
	private static int PLUS_ULTRA_FULL_END_SLOWNESS_AMPLIFIER = 1;
	private static int PLUS_ULTRA_FULL_END_WEAKNESS_AMPLIFIER = 1;
	private static int PLUS_ULTRA_EARLY_CANCEL_COOLDOWN_TICKS = 15 * 60 * TICKS_PER_SECOND;
	private static int PLUS_ULTRA_FULL_END_COOLDOWN_TICKS = 20 * 60 * TICKS_PER_SECOND;
	private static double CASSIOPEIA_DETECTION_RADIUS = 64.0;
	private static int CASSIOPEIA_OUTLINE_REFRESH_TICKS = 10;
	private static double HERCULES_TARGET_RANGE = 64.0;
	private static double HERCULES_SPLASH_RADIUS = 5.0;
	private static int HERCULES_EFFECT_DURATION_TICKS = 15 * TICKS_PER_SECOND;
	private static int HERCULES_WARNING_FADE_IN_TICKS = 5;
	private static int HERCULES_WARNING_STAY_TICKS = 50;
	private static int HERCULES_WARNING_FADE_OUT_TICKS = 5;
	private static float HERCULES_WARNING_SCALE = 0.9F;
	private static double HERCULES_ACTIVATION_COST_PERCENT = 40.0;
	private static int HERCULES_COOLDOWN_TICKS = 45 * TICKS_PER_SECOND;
	private static float HERCULES_TRUE_DAMAGE = 5.0F;
	private static int HERCULES_SLOWNESS_AMPLIFIER = 255;
	private static int HERCULES_RESISTANCE_AMPLIFIER = 0;
	private static int HERCULES_PARTICLE_INTERVAL_TICKS = 5;
	private static int HERCULES_WARNING_COLOR_RGB = 0x39B7FF;
	private static boolean HERCULES_DISABLE_MANA_REGEN_WHILE_ACTIVE = true;
	private static boolean HERCULES_PREVENT_CASTER_DIRECT_DAMAGE = true;
	private static boolean HERCULES_INTERRUPT_TARGET_ITEM_USE = false;
	private static int HERCULES_ACTIVATION_IMPACT_DIRT_PARTICLE_COUNT = 18;
	private static double HERCULES_ACTIVATION_IMPACT_DIRT_PARTICLE_SPEED = 0.08;
	private static float HERCULES_ACTIVATION_IMPACT_SOUND_VOLUME = 1.2F;
	private static float HERCULES_ACTIVATION_IMPACT_SOUND_PITCH = 0.65F;
	private static double SAGITTARIUS_BEAM_RANGE = 32.0;
	private static double SAGITTARIUS_BEAM_RADIUS = 1.5;
	private static int SAGITTARIUS_WINDUP_TICKS = 3 * TICKS_PER_SECOND;
	private static double SAGITTARIUS_WINDUP_MOVEMENT_SPEED_MULTIPLIER = 0.5;
	private static int SAGITTARIUS_CHARGE_GLOW_PARTICLE_COUNT = 1;
	private static int SAGITTARIUS_CHARGE_BEAM_PARTICLE_COUNT = 0;
	private static double SAGITTARIUS_ACTIVATION_COST_PERCENT = 70.0;
	private static int SAGITTARIUS_COOLDOWN_TICKS = 2 * 60 * TICKS_PER_SECOND;
	private static double SAGITTARIUS_CLOSE_RANGE_THRESHOLD = 5.0;
	private static double SAGITTARIUS_MID_RANGE_THRESHOLD = 10.0;
	private static float SAGITTARIUS_CLOSE_RANGE_TRUE_DAMAGE = 10.0F;
	private static float SAGITTARIUS_MID_RANGE_TRUE_DAMAGE = 6.0F;
	private static float SAGITTARIUS_FAR_RANGE_TRUE_DAMAGE = 2.0F;
	private static boolean SAGITTARIUS_DISABLE_MANA_REGEN_DURING_WINDUP = true;
	private static double ORIONS_GAMBIT_TARGET_RANGE = 64.0;
	private static int ORIONS_GAMBIT_WAIT_CANCEL_COOLDOWN_TICKS = 3 * TICKS_PER_SECOND;
	private static int ORIONS_GAMBIT_LINK_DURATION_TICKS = 40 * TICKS_PER_SECOND;
	private static double ORIONS_GAMBIT_MANA_DRAIN_PERCENT_PER_SECOND = 10.0;
	private static boolean ORIONS_GAMBIT_DRAIN_MANA_WHILE_WAITING_FOR_TARGET = false;
	private static boolean ORIONS_GAMBIT_DISABLE_MANA_REGEN_WHILE_WAITING_FOR_TARGET = true;
	private static boolean ORIONS_GAMBIT_DISABLE_MANA_REGEN_WHILE_LINKED = true;
	private static int ORIONS_GAMBIT_COOLDOWN_TICKS = 10 * 60 * TICKS_PER_SECOND;
	private static int ORIONS_GAMBIT_CASTER_PENALTY_REFRESH_TICKS = TICKS_PER_SECOND;
	private static int ORIONS_GAMBIT_CASTER_WEAKNESS_AMPLIFIER = 2;
	private static int ORIONS_GAMBIT_CASTER_SLOWNESS_AMPLIFIER = 2;
	private static double ORIONS_GAMBIT_CASTER_MAX_HEALTH_HEARTS = 5.0;
	private static double ORIONS_GAMBIT_CASTER_INCOMING_DAMAGE_MULTIPLIER = 1.5;
	private static boolean ORIONS_GAMBIT_CLEAR_TARGET_COOLDOWNS_ON_LOCK = true;
	private static boolean ORIONS_GAMBIT_SUPPRESS_TARGET_MANA_COSTS = true;
	private static boolean ORIONS_GAMBIT_SUPPRESS_TARGET_COOLDOWNS = true;
	private static boolean ORIONS_GAMBIT_RESET_CASTER_COOLDOWNS_ON_END = true;
	private static boolean ORIONS_GAMBIT_APPLY_USED_TARGET_COOLDOWNS_ON_END = true;
	private static int ORIONS_GAMBIT_TARGET_PARTICLE_INTERVAL_TICKS = 4;
	private static int ORIONS_GAMBIT_TARGET_PARTICLE_BURST_COUNT = 12;
	private static double ORIONS_GAMBIT_TARGET_PARTICLE_SPAWN_RADIUS = 0.28;
	private static double ORIONS_GAMBIT_TARGET_PARTICLE_VERTICAL_VELOCITY = 0.22;
	private static double ORIONS_GAMBIT_TARGET_PARTICLE_FORWARD_VELOCITY = 0.09;
	private static double ORIONS_GAMBIT_TARGET_PARTICLE_SIDE_VELOCITY = 0.05;
	private static int ORIONS_GAMBIT_GREED_TARGET_COIN_UNITS = 18 * MagicPlayerData.GREED_COIN_UNITS_PER_COIN;
	private static boolean ORIONS_GAMBIT_RESET_GREED_TARGET_COINS_ON_END = true;
	private static int ASTRAL_CATACLYSM_CHARGE_DURATION_TICKS = 30 * TICKS_PER_SECOND;
	private static int ASTRAL_CATACLYSM_ACQUIRE_WINDOW_TICKS = 60 * TICKS_PER_SECOND;
	private static int ASTRAL_CATACLYSM_DURATION_TICKS = 2 * 60 * TICKS_PER_SECOND;
	private static List<Integer> ASTRAL_CATACLYSM_EXPIRY_WARNING_TICKS = List.of(30 * TICKS_PER_SECOND, 10 * TICKS_PER_SECOND, 5 * TICKS_PER_SECOND);
	private static boolean ASTRAL_CATACLYSM_ALLOW_MOB_TARGETS = false;
	private static double ASTRAL_CATACLYSM_TARGET_RANGE = 64.0;
	private static double ASTRAL_CATACLYSM_BEAM_RADIUS = 5.0;
	private static double ASTRAL_CATACLYSM_BEAM_RISE_BLOCKS_PER_SECOND = 2.0;
	private static double ASTRAL_CATACLYSM_BEAM_PARTICLE_STEP = 0.5;
	private static int ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_COUNT = 18;
	private static int ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_COUNT = 10;
	private static int ASTRAL_CATACLYSM_BEAM_SPARK_PARTICLE_COUNT = 6;
	private static int ASTRAL_CATACLYSM_BEAM_RING_POINTS_PER_STEP = 8;
	private static int ASTRAL_CATACLYSM_BEAM_DAMAGE_INTERVAL_TICKS = TICKS_PER_SECOND;
	private static float ASTRAL_CATACLYSM_BEAM_TRUE_DAMAGE_PER_INTERVAL = 4.0F;
	private static int ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_INTERVAL_TICKS = TICKS_PER_SECOND;
	private static float ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_VOLUME = 1.0F;
	private static float ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_PITCH = 1.2F;
	private static int ASTRAL_CATACLYSM_COOLDOWN_TICKS = 15 * 60 * TICKS_PER_SECOND;
	private static int ASTRAL_CATACLYSM_FREEZE_REFRESH_TICKS = 5;
	private static boolean ASTRAL_CATACLYSM_DISABLE_MANA_REGEN_WHILE_ACTIVE = true;
	private static boolean ASTRAL_CATACLYSM_IGNORE_TOTEMS = true;
	private static boolean ASTRAL_CATACLYSM_PRESERVE_BEACON_ANCHOR = true;
	private static Identifier ASTRAL_CATACLYSM_BEACON_ANCHOR_BLOCK_ID = Identifier.of("evanpack", "beacon_anchor");
	private static Identifier ASTRAL_CATACLYSM_BEACON_CORE_ITEM_ID = Identifier.of("evanpack", "beacon_core");
	private static String ASTRAL_CATACLYSM_BEACON_CORE_ANCHOR_STATE_ID = "evanpack_beacon_core_anchor";
	private static double ASTRAL_CATACLYSM_BEACON_CORE_PROTECTION_RADIUS = 75.0;
	private static boolean ASTRAL_CATACLYSM_CANCEL_BEAM_ON_PROTECTED_BEACON_CORE_HOLDER = true;
	private static WittyOneLinerTierSettings WITTY_ONE_LINER_LOW_TIER = WittyOneLinerTierSettings.defaults(
		70,
		600,
		600,
		0xFFFFFF,
		0,
		0,
		-1,
		-1,
		-1,
		-1,
		true,
		List.of(
			"I'd tell a sharper joke, but safety scissors won.",
			"That was my warm-up heckle.",
			"I clown around professionally.",
			"I brought jokes and poor decisions.",
			"You're lucky that was the discount punchline."
		)
	);
	private static WittyOneLinerTierSettings WITTY_ONE_LINER_MID_TIER = WittyOneLinerTierSettings.defaults(
		20,
		900,
		900,
		0xFFD54A,
		2,
		1,
		-1,
		1,
		-1,
		-1,
		true,
		List.of(
			"You just got roasted by a part-time fool.",
			"I've seen mannequins dodge better.",
			"That glare says the joke landed harder than you did.",
			"I'm billing you for emotional damage and stage lights.",
			"Even my backup punchline hits harder than that."
		)
	);
	private static WittyOneLinerTierSettings WITTY_ONE_LINER_HIGH_TIER = WittyOneLinerTierSettings.defaults(
		10,
		1200,
		1200,
		0x39B7FF,
		4,
		4,
		1,
		1,
		0,
		0,
		true,
		List.of(
			"The crowd gasped, the lights hit, and your confidence left the building.",
			"I didn't steal the show. You dropped it.",
			"That silence is your reputation filing for leave.",
			"You're not bombed by the joke. You're bombed by the timing.",
			"Smile for the spotlight. This is the part where you lose gracefully."
		)
	);

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
	private static int MARTYRS_FLAME_COOLDOWN_TICKS = 30 * TICKS_PER_SECOND;
	private static int MARTYRS_FLAME_FIRE_DURATION_TICKS = 4 * TICKS_PER_SECOND;
	private static float MARTYRS_FLAME_RETALIATION_DAMAGE = 2.0F;
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
	private static int LOVE_AT_FIRST_SIGHT_RESISTANCE_AMPLIFIER = 0;
	private static boolean LOVE_AT_FIRST_SIGHT_BLOCK_ITEM_USE = false;
	private static boolean LOVE_AT_FIRST_SIGHT_BLOCK_ATTACKS = true;
	private static boolean LOVE_AT_FIRST_SIGHT_PREVENT_CASTER_DIRECT_DAMAGE = true;
	private static boolean MARTYRS_FLAME_APPLY_GLOWING_EFFECT = true;
	private static boolean MARTYRS_FLAME_DISABLE_MANA_REGEN_WHILE_ACTIVE = true;
	private static boolean MARTYRS_FLAME_FIRE_IGNORES_NORMAL_EXTINGUISH = true;
	private static int MARTYRS_FLAME_FIRE_PARTICLE_INTERVAL_TICKS = 4;
	private static int MARTYRS_FLAME_FIRE_FLAME_PARTICLE_COUNT = 6;
	private static int MARTYRS_FLAME_FIRE_SMOKE_PARTICLE_COUNT = 4;
	private static int DOMAIN_CLASH_REGENERATION_AMPLIFIER = 9;
	private static int DOMAIN_CLASH_INSTANT_HEALTH_AMPLIFIER = 9;
	private static int DOMAIN_CLASH_RESISTANCE_AMPLIFIER = 5;
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
	private static boolean DOMAIN_CLASH_PAUSE_TIMED_DOMAIN_COLLAPSE_TIMERS = true;
	private static boolean DOMAIN_CLASH_PAUSE_ASTRAL_CATACLYSM_PHASE_TIMERS = true;
	private static boolean DOMAIN_CONTROL_BLOCK_TELEPORT_ACROSS_BOUNDARIES = true;
	private static final int DOMAIN_BLOCK_PLACE_FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE_AND_SKIP_CALLBACKS_AND_DROPS;
	private static final int DOMAIN_BLOCK_RESTORE_FLAGS = Block.NOTIFY_ALL | Block.FORCE_STATE_AND_SKIP_CALLBACKS_AND_DROPS;
	private static final BlockState DOMAIN_SHELL_BLOCK_STATE = Blocks.BLACK_CONCRETE.getDefaultState();
	private static final BlockState DOMAIN_INTERIOR_BLOCK_STATE = Blocks.AIR.getDefaultState();
	private static final BlockState LOVE_DOMAIN_LIGHT_STATE = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15);
	private static final ParticleEffect HERCULES_ACTIVATION_DIRT_PARTICLE = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DIRT.getDefaultState());
	private static final Identifier SPOTLIGHT_ATTACK_DAMAGE_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "spotlight_attack_damage");
	private static final Identifier SPOTLIGHT_MOVEMENT_SPEED_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "spotlight_movement_speed");
	private static final Identifier SPOTLIGHT_MAX_HEALTH_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "spotlight_max_health");
	private static final Identifier SPOTLIGHT_SCALE_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "spotlight_scale");
	private static final Identifier SAGITTARIUS_WINDUP_SPEED_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "sagittarius_windup_speed");
	private static final Identifier ORIONS_GAMBIT_MAX_HEALTH_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "orions_gambit_max_health");
	private static final Identifier FROST_STAGE_MAX_HEALTH_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "frost_stage_max_health");
	private static final ParticleEffect SAGITTARIUS_BEAM_PARTICLE = ParticleTypes.END_ROD;
	private static final ParticleEffect ASTRAL_EXECUTION_BEAM_PARTICLE = ParticleTypes.GLOW;
	private static final ParticleEffect ASTRAL_EXECUTION_BEAM_OUTER_PARTICLE = ParticleTypes.END_ROD;
	private static final ParticleEffect ASTRAL_EXECUTION_BEAM_SPARK_PARTICLE = ParticleTypes.ELECTRIC_SPARK;
	private static final ParticleEffect FROST_SHARD_PARTICLE = new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(ModItems.FROST_SHARD));
	private static final double DOMAIN_TELEPORT_POSITION_EPSILON_SQUARED = 4.0E-4;
	private static final float DOMAIN_TELEPORT_ROTATION_EPSILON_DEGREES = 0.5F;

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
	private static final Set<UUID> CASSIOPEIA_PASSIVE_ENABLED = new HashSet<>();
	private static final Map<UUID, Set<UUID>> CASSIOPEIA_LAST_OUTLINED_PLAYERS = new HashMap<>();
	private static final Map<UUID, FrostbiteState> FROSTBITTEN_TARGETS = new HashMap<>();
	private static final Map<UUID, FrostStageState> FROST_STAGE_STATES = new HashMap<>();
	private static final Map<UUID, FrostMaximumState> FROST_MAXIMUM_STATES = new HashMap<>();
	private static final List<FrostSpikeWaveState> FROST_SPIKE_WAVES = new ArrayList<>();
	private static final Map<UUID, FrostFreezeState> FROST_FROZEN_TARGETS = new HashMap<>();
	private static final Map<UUID, FrostFearState> FROST_MAXIMUM_FEAR_TARGETS = new HashMap<>();
	private static final Map<UUID, FrostHelplessState> FROST_HELPLESS_TARGETS = new HashMap<>();
	private static final Map<UUID, FrostPackedIceState> FROST_PACKED_ICE_TARGETS = new HashMap<>();
	private static final Map<UUID, Integer> BELOW_FREEZING_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> FROST_ASCENT_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> FROST_MANA_REGEN_BLOCKED_END_TICK = new HashMap<>();
	private static final Map<UUID, Map<UUID, Integer>> ABSOLUTE_ZERO_NEXT_DAMAGE_TICK = new HashMap<>();
	private static final Map<UUID, Integer> ABSOLUTE_ZERO_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> PLANCK_HEAT_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, PlanckHeatState> PLANCK_HEAT_STATES = new HashMap<>();
	private static final Map<UUID, Map<UUID, Integer>> PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK = new HashMap<>();
	private static final Map<UUID, EnhancedFireState> ENHANCED_FIRE_TARGETS = new HashMap<>();
	private static final Map<UUID, LoveLockState> LOVE_LOCKED_TARGETS = new HashMap<>();
	private static final Map<UUID, Boolean> LOVE_POWER_ACTIVE_THIS_SECOND = new HashMap<>();
	private static final Set<UUID> MARTYRS_FLAME_PASSIVE_ENABLED = new HashSet<>();
	private static final Set<UUID> SPOTLIGHT_PASSIVE_ENABLED = new HashSet<>();
	private static final Map<UUID, SpotlightState> SPOTLIGHT_STATES = new HashMap<>();
	private static final Map<UUID, Integer> MARTYRS_FLAME_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> WITTY_ONE_LINER_COOLDOWN_END_TICK = new HashMap<>();
	private static final Set<UUID> COMEDIC_REWRITE_PASSIVE_ENABLED = new HashSet<>();
	private static final Map<UUID, Integer> COMEDIC_REWRITE_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> COMEDIC_REWRITE_IMMUNITY_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> COMEDIC_REWRITE_FALL_PROTECTION_END_TICK = new HashMap<>();
	private static final Map<UUID, PendingComedicRewriteState> COMEDIC_REWRITE_PENDING_STATES = new HashMap<>();
	private static final List<ComedicRewriteVisualCameo> COMEDIC_REWRITE_VISUAL_CAMEOS = new ArrayList<>();
	private static final Map<UUID, ComedicAssistantArmedState> COMEDIC_ASSISTANT_ARMED_STATES = new HashMap<>();
	private static final Map<UUID, Integer> COMEDIC_ASSISTANT_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, ComedicAssistantParrotCarryState> COMEDIC_ASSISTANT_PARROT_CARRY_STATES = new HashMap<>();
	private static final Map<UUID, ComedicAssistantAcmeVisualState> COMEDIC_ASSISTANT_ACME_VISUALS = new HashMap<>();
	private static final Map<UUID, ComedicAssistantCaneImpactState> COMEDIC_ASSISTANT_CANE_IMPACT_STATES = new HashMap<>();
	private static final Map<UUID, Integer> PLUS_ULTRA_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, PlusUltraState> PLUS_ULTRA_STATES = new HashMap<>();
	private static final Map<UUID, Set<UUID>> PLUS_ULTRA_LAST_OUTLINED_PLAYERS = new HashMap<>();
	private static final Map<UUID, PlusUltraImpactState> PLUS_ULTRA_IMPACT_STATES = new HashMap<>();
	private static final Set<UUID> PLUS_ULTRA_DAMAGE_SCALING_GUARD = new HashSet<>();
	private static final Map<UUID, Double> MARTYRS_FLAME_DRAIN_BUFFER = new HashMap<>();
	private static final Map<UUID, MartyrsFlameBurnState> MARTYRS_FLAME_BURNING_TARGETS = new HashMap<>();
	private static final Set<UUID> TILL_DEATH_DO_US_PART_PASSIVE_ENABLED = new HashSet<>();
	private static final Map<UUID, TillDeathDoUsPartState> TILL_DEATH_DO_US_PART_STATES = new HashMap<>();
	private static final Map<UUID, Integer> TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Double> TILL_DEATH_DO_US_PART_DRAIN_BUFFER = new HashMap<>();
	private static final Map<UUID, Integer> TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK = new HashMap<>();
	private static final Map<UUID, HerculesBurdenState> HERCULES_STATES = new HashMap<>();
	private static final Map<UUID, AstralBurdenTargetState> HERCULES_TARGETS = new HashMap<>();
	private static final Map<UUID, Integer> HERCULES_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, SagittariusWindupState> SAGITTARIUS_STATES = new HashMap<>();
	private static final Map<UUID, Integer> SAGITTARIUS_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, OrionGambitState> ORIONS_GAMBIT_STATES = new HashMap<>();
	private static final Map<UUID, OrionPenaltyState> ORIONS_GAMBIT_PENALTIES = new HashMap<>();
	private static final Map<UUID, UUID> ORIONS_GAMBIT_CASTER_BY_TARGET = new HashMap<>();
	private static final Map<UUID, Integer> ORIONS_GAMBIT_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Double> ORIONS_GAMBIT_DRAIN_BUFFER = new HashMap<>();
	private static final Set<UUID> ORIONS_GAMBIT_DAMAGE_SCALING_GUARD = new HashSet<>();
	private static final Map<UUID, ConstellationDomainState> ASTRAL_CATACLYSM_DOMAIN_STATES = new HashMap<>();
	private static final Map<UUID, AstralExecutionBeamState> ASTRAL_EXECUTION_BEAMS = new HashMap<>();
	private static final Map<UUID, Integer> ASTRAL_CATACLYSM_COOLDOWN_END_TICK = new HashMap<>();
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
	private static final Map<UUID, DomainClashPendingDamageState> DOMAIN_CLASH_PENDING_DAMAGE = new HashMap<>();
	private static final Map<UUID, UUID> MAGIC_DAMAGE_PENDING_ATTACKER = new HashMap<>();
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
	private static final ThreadLocal<Integer> DOMAIN_TELEPORT_BYPASS_DEPTH = ThreadLocal.withInitial(() -> 0);
	private static final int BEACON_CORE_ANCHOR_CACHE_REFRESH_TICKS = 20;
	private static BeaconCoreAnchorState cachedBeaconCoreAnchor;
	private static int cachedBeaconCoreAnchorTick = Integer.MIN_VALUE;
	private static final Set<UUID> TEST_MODE_PLAYERS = new HashSet<>();

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

		BELOW_FREEZING_MANA_DRAIN_PER_SECOND = 0;
		ABSOLUTE_ZERO_MANA_DRAIN_PER_SECOND = 0;
		LOVE_AT_FIRST_SIGHT_IDLE_DRAIN_PER_SECOND = config.mana.loveAtFirstSightIdleDrainPerSecond;
		LOVE_AT_FIRST_SIGHT_ACTIVE_DRAIN_PER_SECOND = config.mana.loveAtFirstSightActiveDrainPerSecond;
		MARTYRS_FLAME_DRAIN_PERCENT_PER_SECOND = MathHelper.clamp(config.mana.martyrsFlameDrainPercentPerSecond, 0.0, 100.0);
		TILL_DEATH_DO_US_PART_DRAIN_PERCENT_PER_SECOND = MathHelper.clamp(config.mana.tillDeathDoUsPartDrainPercentPerSecond, 0.0, 100.0);
		MANIPULATION_ACTIVATION_MANA_COST = Math.max(0, config.mana.emptyEmbraceActivationCost);
		MANIPULATION_MANA_DRAIN_PER_SECOND = Math.max(0, config.mana.emptyEmbraceDrainPerSecond);
		DOMAIN_EXPANSION_ACTIVATION_MANA_COST = config.mana.domainExpansionActivationCost;
		PASSIVE_MANA_REGEN_PER_SECOND = config.mana.passiveRegenPerSecond;
		DEPLETED_RECOVERY_REGEN_PER_SECOND = config.mana.depletedRecoveryRegenPerSecond;
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
		SAGITTARIUS_BEAM_RANGE = Math.max(1.0, config.constellationSagittarius.beamRange);
		SAGITTARIUS_BEAM_RADIUS = Math.max(0.1, config.constellationSagittarius.beamRadius);
		SAGITTARIUS_WINDUP_TICKS = Math.max(1, config.constellationSagittarius.windupTicks);
		SAGITTARIUS_WINDUP_MOVEMENT_SPEED_MULTIPLIER = MathHelper.clamp(config.constellationSagittarius.windupMovementSpeedMultiplier, 0.0, 1.0);
		SAGITTARIUS_CHARGE_GLOW_PARTICLE_COUNT = Math.max(0, config.constellationSagittarius.chargeGlowParticleCount);
		SAGITTARIUS_CHARGE_BEAM_PARTICLE_COUNT = Math.max(0, config.constellationSagittarius.chargeBeamParticleCount);
		SAGITTARIUS_ACTIVATION_COST_PERCENT = MathHelper.clamp(config.constellationSagittarius.activationCostPercent, 0.0, 100.0);
		SAGITTARIUS_COOLDOWN_TICKS = Math.max(0, config.constellationSagittarius.cooldownTicks);
		SAGITTARIUS_CLOSE_RANGE_THRESHOLD = Math.max(0.0, config.constellationSagittarius.closeRangeThreshold);
		SAGITTARIUS_MID_RANGE_THRESHOLD = Math.max(SAGITTARIUS_CLOSE_RANGE_THRESHOLD, config.constellationSagittarius.midRangeThreshold);
		SAGITTARIUS_CLOSE_RANGE_TRUE_DAMAGE = Math.max(0.0F, config.constellationSagittarius.closeRangeTrueDamage);
		SAGITTARIUS_MID_RANGE_TRUE_DAMAGE = Math.max(0.0F, config.constellationSagittarius.midRangeTrueDamage);
		SAGITTARIUS_FAR_RANGE_TRUE_DAMAGE = Math.max(0.0F, config.constellationSagittarius.farRangeTrueDamage);
		SAGITTARIUS_DISABLE_MANA_REGEN_DURING_WINDUP = config.constellationSagittarius.disableManaRegenDuringWindup;
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
		ASTRAL_CATACLYSM_BEAM_PARTICLE_STEP = Math.max(0.1, config.constellationDomain.beamParticleStep);
		ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_COUNT = Math.max(0, config.constellationDomain.beamCoreParticleCount);
		ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_COUNT = Math.max(0, config.constellationDomain.beamOuterParticleCount);
		ASTRAL_CATACLYSM_BEAM_SPARK_PARTICLE_COUNT = Math.max(0, config.constellationDomain.beamSparkParticleCount);
		ASTRAL_CATACLYSM_BEAM_RING_POINTS_PER_STEP = Math.max(0, config.constellationDomain.beamRingPointsPerStep);
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
			player.sendMessage(Text.translatable("message.magic.constellation.still_charging", MagicAbility.SAGITTARIUS_ASTRAL_ARROW.displayName()), true);
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

		if (requestedAbility == MagicAbility.MANIPULATION && isManipulationRequestDebounced(player, currentTick)) {
			return;
		}

		if (GreedRuntime.isAbilityUseLocked(player, currentTick)) {
			player.sendMessage(Text.translatable("message.magic.greed.ability_locked"), true);
			return;
		}

		if (GreedRuntime.handleAbilityRequest(player, requestedAbility)) {
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

	private static void handleMartyrsFlameRequest(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		int currentTick = player.getEntityWorld().getServer().getTicks();
		if (MARTYRS_FLAME_PASSIVE_ENABLED.contains(playerId)) {
			deactivateMartyrsFlame(player, true);
			player.sendMessage(Text.translatable("message.magic.ability.passive_disabled", MagicAbility.MARTYRS_FLAME.displayName()), false);
			return;
		}

		int remainingTicks = martyrsFlameCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.MARTYRS_FLAME, remainingTicks, false);
			return;
		}

		if (!isOrionsGambitManaCostSuppressed(player) && MagicPlayerData.getMana(player) <= 0) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		MARTYRS_FLAME_PASSIVE_ENABLED.add(playerId);
		MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		player.sendMessage(Text.translatable("message.magic.ability.passive_enabled", MagicAbility.MARTYRS_FLAME.displayName()), false);
		recordOrionsGambitAbilityUse(player, MagicAbility.MARTYRS_FLAME);
	}

	private static void handleSpotlightRequest(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		if (SPOTLIGHT_PASSIVE_ENABLED.contains(playerId)) {
			deactivateSpotlight(player, true);
			player.sendMessage(Text.translatable("message.magic.ability.passive_disabled", MagicAbility.SPOTLIGHT.displayName()), false);
			return;
		}

		SPOTLIGHT_PASSIVE_ENABLED.add(playerId);
		SPOTLIGHT_STATES.computeIfAbsent(playerId, ignored -> new SpotlightState());
		player.sendMessage(Text.translatable("message.magic.ability.passive_enabled", MagicAbility.SPOTLIGHT.displayName()), false);
		recordOrionsGambitAbilityUse(player, MagicAbility.SPOTLIGHT);
	}

	private static void handleCassiopeiaRequest(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		if (CASSIOPEIA_PASSIVE_ENABLED.contains(playerId)) {
			deactivateCassiopeia(player, true);
			player.sendMessage(Text.translatable("message.magic.ability.passive_disabled", MagicAbility.CASSIOPEIA.displayName()), false);
			return;
		}

		CASSIOPEIA_PASSIVE_ENABLED.add(playerId);
		CASSIOPEIA_LAST_OUTLINED_PLAYERS.remove(playerId);
		player.sendMessage(Text.translatable("message.magic.ability.passive_enabled", MagicAbility.CASSIOPEIA.displayName()), false);
		recordOrionsGambitAbilityUse(player, MagicAbility.CASSIOPEIA);
	}

	private static void handleComedicRewriteRequest(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		if (COMEDIC_REWRITE_PASSIVE_ENABLED.contains(playerId)) {
			deactivateComedicRewrite(player);
			player.sendMessage(Text.translatable("message.magic.ability.passive_disabled", MagicAbility.COMEDIC_REWRITE.displayName()), false);
			return;
		}

		COMEDIC_REWRITE_PASSIVE_ENABLED.add(playerId);
		player.sendMessage(Text.translatable("message.magic.ability.passive_enabled", MagicAbility.COMEDIC_REWRITE.displayName()), false);
		recordOrionsGambitAbilityUse(player, MagicAbility.COMEDIC_REWRITE);
	}

	private static void handleComedicAssistantRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		ComedicAssistantArmedState existingState = COMEDIC_ASSISTANT_ARMED_STATES.get(player.getUuid());
		if (existingState != null) {
			deactivateComedicAssistant(player, currentTick, COMEDIC_ASSISTANT_CANCEL_COOLDOWN_TICKS, true, false);
			return;
		}

		int remainingTicks = comedicAssistantCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.COMEDIC_ASSISTANT, remainingTicks, false);
			return;
		}

		if (!hasEnabledComedicAssistantOutcomes()) {
			player.sendMessage(Text.translatable("message.magic.jester.comedic_assistant.no_outcomes"), true);
			return;
		}

		int manaCost = (int) Math.ceil(manaFromPercentExact(COMEDIC_ASSISTANT_ACTIVATION_COST_PERCENT));
		if (!canSpendAbilityCost(player, manaCost)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		spendAbilityCost(player, manaCost);
		COMEDIC_ASSISTANT_ARMED_STATES.put(
			player.getUuid(),
			new ComedicAssistantArmedState(
				player.getEntityWorld().getRegistryKey(),
				currentTick + COMEDIC_ASSISTANT_ARMED_DURATION_TICKS,
				currentTick + COMEDIC_ASSISTANT_ARMED_INDICATOR_REFRESH_TICKS
			)
		);
		sendComedicAssistantArmedIndicator(player);
		spawnComedicAssistantArmedParticles(player);
		recordOrionsGambitAbilityUse(player, MagicAbility.COMEDIC_ASSISTANT);
	}

	private static void handlePlusUltraRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		PlusUltraState activeState = PLUS_ULTRA_STATES.get(player.getUuid());
		if (activeState != null) {
			PlusUltraEndMode endMode = currentTick < activeState.halfwayTick() ? PlusUltraEndMode.MANUAL_EARLY : PlusUltraEndMode.FULL;
			endPlusUltra(player, currentTick, endMode, true);
			return;
		}

		int remainingTicks = plusUltraCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.PLUS_ULTRA, remainingTicks, false);
			return;
		}

		int manaCost = (int) Math.ceil(manaFromPercentExact(PLUS_ULTRA_ACTIVATION_COST_PERCENT));
		if (!canSpendAbilityCost(player, manaCost)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		spendAbilityCost(player, manaCost);
		activatePlusUltra(player, currentTick);
		recordOrionsGambitAbilityUse(player, MagicAbility.PLUS_ULTRA);
	}

	private static void activatePlusUltra(ServerPlayerEntity player, int currentTick) {
		UUID playerId = player.getUuid();
		PlusUltraState previousState = PLUS_ULTRA_STATES.remove(playerId);
		if (previousState != null) {
			removePlusUltraOverheadText(player.getEntityWorld().getServer(), previousState);
		}

		boolean hadAllowFlying = player.getAbilities().allowFlying;
		boolean hadFlying = player.getAbilities().flying;
		float hadFlySpeed = player.getAbilities().getFlySpeed();
		int overheadTextEndTick = currentTick + Math.max(0, PLUS_ULTRA_OVERHEAD_TEXT_DURATION_TICKS);
		UUID overheadTextEntityId = PLUS_ULTRA_OVERHEAD_TEXT_ENABLED && PLUS_ULTRA_OVERHEAD_TEXT_DURATION_TICKS > 0 ? spawnPlusUltraOverheadText(player) : null;
		PLUS_ULTRA_STATES.put(
			playerId,
			new PlusUltraState(
				player.getEntityWorld().getRegistryKey(),
				currentTick,
				currentTick + Math.max(1, PLUS_ULTRA_DURATION_TICKS / 2),
				currentTick + PLUS_ULTRA_DURATION_TICKS,
				currentTick,
				currentTick,
				hadAllowFlying,
				hadFlying,
				hadFlySpeed,
				overheadTextEndTick,
				overheadTextEntityId
			)
		);
		MagicPlayerData.setPlusUltraActive(player, true);
		PLUS_ULTRA_LAST_OUTLINED_PLAYERS.remove(playerId);
		setActiveAbility(player, MagicAbility.PLUS_ULTRA);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		ensurePlusUltraFlightState(player);
		syncPlusUltraOutlines(player);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.PLUS_ULTRA.displayName()), true);
	}

	private static void applyPlusUltra(ServerPlayerEntity player, int currentTick) {
		PlusUltraState state = PLUS_ULTRA_STATES.get(player.getUuid());
		if (state == null) {
			clearPlusUltraRuntimeState(player, true);
			return;
		}

		if (!shouldKeepPlusUltraEnabled(player) || player.getEntityWorld().getRegistryKey() != state.dimension()) {
			endPlusUltra(player, currentTick, PlusUltraEndMode.FULL, false);
			return;
		}

		if (currentTick >= state.endTick()) {
			endPlusUltra(player, currentTick, PlusUltraEndMode.FULL, true);
			return;
		}

		ensurePlusUltraFlightState(player);
		if (currentTick >= state.nextOutlineTick()) {
			syncPlusUltraOutlines(player);
			state.nextOutlineTick(currentTick + PLUS_ULTRA_OUTLINE_REFRESH_TICKS);
		}
		if (state.overheadTextEntityId() != null && currentTick >= state.overheadTextEndTick()) {
			removePlusUltraOverheadText(player.getEntityWorld().getServer(), state);
			state.overheadTextEntityId(null);
		}
		if (currentTick < state.overheadTextEndTick() && currentTick >= state.nextTextTick()) {
			syncPlusUltraOverheadText(player, state, currentTick);
			state.nextTextTick(currentTick + PLUS_ULTRA_OVERHEAD_TEXT_REFRESH_TICKS);
		}
	}

	private static void endPlusUltra(ServerPlayerEntity player, int currentTick, PlusUltraEndMode endMode, boolean sendFeedback) {
		clearPlusUltraRuntimeState(player, true);
		if (endMode != PlusUltraEndMode.ADMIN_CLEAR) {
			if (endMode == PlusUltraEndMode.MANUAL_EARLY) {
				applyPlusUltraPenalty(
					player,
					PLUS_ULTRA_EARLY_CANCEL_PENALTY_DURATION_TICKS,
					PLUS_ULTRA_EARLY_CANCEL_SLOWNESS_AMPLIFIER,
					PLUS_ULTRA_EARLY_CANCEL_WEAKNESS_AMPLIFIER
				);
				startPlusUltraCooldown(player.getUuid(), currentTick, PLUS_ULTRA_EARLY_CANCEL_COOLDOWN_TICKS);
			} else {
				applyPlusUltraPenalty(
					player,
					PLUS_ULTRA_FULL_END_PENALTY_DURATION_TICKS,
					PLUS_ULTRA_FULL_END_SLOWNESS_AMPLIFIER,
					PLUS_ULTRA_FULL_END_WEAKNESS_AMPLIFIER
				);
				startPlusUltraCooldown(player.getUuid(), currentTick, PLUS_ULTRA_FULL_END_COOLDOWN_TICKS);
			}
		}

		if (sendFeedback) {
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.PLUS_ULTRA.displayName()), true);
		}
	}

	private static void applyPlusUltraPenalty(ServerPlayerEntity player, int durationTicks, int slownessAmplifier, int weaknessAmplifier) {
		if (!player.isAlive()) {
			return;
		}

		addConfiguredStatusEffect(player, StatusEffects.SLOWNESS, durationTicks, slownessAmplifier);
		addConfiguredStatusEffect(player, StatusEffects.WEAKNESS, durationTicks, weaknessAmplifier);
	}

	private static void ensurePlusUltraFlightState(ServerPlayerEntity player) {
		if (!PLUS_ULTRA_FLIGHT_ENABLED || player.isSpectator()) {
			updatePlusUltraPose(player, false);
			return;
		}

		boolean changed = false;
		if (!player.getAbilities().allowFlying) {
			player.getAbilities().allowFlying = true;
			changed = true;
		}
		if (Math.abs(player.getAbilities().getFlySpeed() - PLUS_ULTRA_FLIGHT_FLY_SPEED) > 1.0E-4F) {
			player.getAbilities().setFlySpeed(PLUS_ULTRA_FLIGHT_FLY_SPEED);
			changed = true;
		}
		if (changed) {
			player.sendAbilitiesUpdate();
		}
		applyPlusUltraFlightMovement(player);
		updatePlusUltraPose(player, player.getAbilities().flying);
	}

	private static void applyPlusUltraFlightMovement(ServerPlayerEntity player) {
		if (!player.getAbilities().flying) {
			return;
		}

		Vec3d look = player.getRotationVector();
		Vec3d normalizedLook = look.lengthSquared() > 1.0E-5 ? look.normalize() : Vec3d.ZERO;
		Vec3d horizontalLook = new Vec3d(look.x, 0.0, look.z);
		if (horizontalLook.lengthSquared() <= 1.0E-5) {
			float yawRadians = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
			horizontalLook = new Vec3d(-MathHelper.sin(yawRadians), 0.0, MathHelper.cos(yawRadians));
		}
		Vec3d normalizedHorizontalLook = horizontalLook.normalize();
		Vec3d sideways = new Vec3d(-normalizedHorizontalLook.z, 0.0, normalizedHorizontalLook.x);
		double forwardInput = MathHelper.clamp(player.forwardSpeed, -1.0F, 1.0F);
		double sidewaysInput = MathHelper.clamp(player.sidewaysSpeed, -1.0F, 1.0F);
		Vec3d acceleration = Vec3d.ZERO;
		if (Math.abs(forwardInput) > 0.01) {
			acceleration = acceleration.add(normalizedLook.multiply(forwardInput * PLUS_ULTRA_FLIGHT_ACCELERATION));
		}
		if (Math.abs(sidewaysInput) > 0.01) {
			acceleration = acceleration.add(sideways.multiply(sidewaysInput * PLUS_ULTRA_FLIGHT_ACCELERATION * 0.75));
		}
		if (player.isJumping()) {
			acceleration = acceleration.add(0.0, PLUS_ULTRA_FLIGHT_VERTICAL_ACCELERATION, 0.0);
		}
		if (player.isSneaking()) {
			acceleration = acceleration.add(0.0, -PLUS_ULTRA_FLIGHT_VERTICAL_ACCELERATION, 0.0);
		}

		Vec3d nextVelocity = player.getVelocity().multiply(PLUS_ULTRA_FLIGHT_DRAG).add(acceleration);
		player.setVelocity(clampPlusUltraFlightVelocity(nextVelocity));
	}

	private static Vec3d clampPlusUltraFlightVelocity(Vec3d velocity) {
		double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
		double clampedX = velocity.x;
		double clampedZ = velocity.z;
		if (PLUS_ULTRA_FLIGHT_MAX_SPEED > 0.0 && horizontalSpeed > PLUS_ULTRA_FLIGHT_MAX_SPEED) {
			double scale = PLUS_ULTRA_FLIGHT_MAX_SPEED / horizontalSpeed;
			clampedX *= scale;
			clampedZ *= scale;
		}
		double clampedY = MathHelper.clamp(velocity.y, -PLUS_ULTRA_FLIGHT_VERTICAL_MAX_SPEED, PLUS_ULTRA_FLIGHT_VERTICAL_MAX_SPEED);
		return new Vec3d(clampedX, clampedY, clampedZ);
	}

	private static void updatePlusUltraPose(ServerPlayerEntity player, boolean shouldUseFlyingPose) {
	}

	private static PlusUltraState clearPlusUltraRuntimeState(ServerPlayerEntity player, boolean clearOutlinePayload) {
		UUID playerId = player.getUuid();
		PlusUltraState state = PLUS_ULTRA_STATES.remove(playerId);
		MagicPlayerData.setPlusUltraActive(player, false);
		PLUS_ULTRA_LAST_OUTLINED_PLAYERS.remove(playerId);
		clearPlusUltraImpactStates(playerId);
		if (clearOutlinePayload) {
			sendCassiopeiaOutlinePayload(player, List.of());
		}

		if (state != null) {
			restorePlusUltraFlight(player, state.hadAllowFlying(), state.hadFlying(), state.hadFlySpeed());
			removePlusUltraOverheadText(player.getEntityWorld().getServer(), state);
		} else {
			restorePlusUltraFlightFallback(player);
		}

		if (activeAbility(player) == MagicAbility.PLUS_ULTRA || MagicPlayerData.getActiveAbilitySlot(player) == MagicAbility.PLUS_ULTRA.slot()) {
			setActiveAbility(player, MagicAbility.NONE);
		}
		return state;
	}

	private static void clearPlusUltraImpactStates(UUID playerId) {
		PLUS_ULTRA_IMPACT_STATES.entrySet().removeIf(entry -> entry.getKey().equals(playerId) || playerId.equals(entry.getValue().casterId()));
	}

	private static void restorePlusUltraFlight(ServerPlayerEntity player, boolean allowFlying, boolean flying, float flySpeed) {
		boolean nextAllowFlying = allowFlying || player.isCreative() || player.isSpectator();
		boolean nextFlying = nextAllowFlying && (flying || player.isCreative() || player.isSpectator());
		boolean changed = player.getAbilities().allowFlying != nextAllowFlying
			|| player.getAbilities().flying != nextFlying
			|| Math.abs(player.getAbilities().getFlySpeed() - flySpeed) > 1.0E-4F;
		player.getAbilities().allowFlying = nextAllowFlying;
		player.getAbilities().flying = nextFlying;
		player.getAbilities().setFlySpeed(flySpeed);
		if (changed) {
			player.sendAbilitiesUpdate();
		}
		updatePlusUltraPose(player, false);
	}

	private static void restorePlusUltraFlightFallback(ServerPlayerEntity player) {
		if (player.isCreative() || player.isSpectator()) {
			return;
		}

		boolean changed = player.getAbilities().allowFlying || player.getAbilities().flying;
		player.getAbilities().allowFlying = false;
		player.getAbilities().flying = false;
		if (Math.abs(player.getAbilities().getFlySpeed() - DEFAULT_PLAYER_FLY_SPEED) > 1.0E-4F) {
			player.getAbilities().setFlySpeed(DEFAULT_PLAYER_FLY_SPEED);
			changed = true;
		}
		if (changed) {
			player.sendAbilitiesUpdate();
		}
		updatePlusUltraPose(player, false);
	}

	private static boolean shouldKeepPlusUltraEnabled(ServerPlayerEntity player) {
		return player.isAlive()
			&& MagicPlayerData.hasMagic(player)
			&& MagicPlayerData.getSchool(player) == MagicSchool.JESTER
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.PLUS_ULTRA);
	}

	private static boolean hasActivePlusUltra(ServerPlayerEntity player) {
		return player != null && activeAbility(player) == MagicAbility.PLUS_ULTRA && PLUS_ULTRA_STATES.containsKey(player.getUuid());
	}

	public static boolean shouldRenderPlusUltraFlightPose(PlayerEntity player) {
		return player != null
			&& PLUS_ULTRA_ELYTRA_POSE_WHILE_FLYING
			&& MagicPlayerData.isPlusUltraActive(player)
			&& player.getAbilities().flying
			&& !player.isOnGround()
			&& !player.isSpectator();
	}

	private static void syncPlusUltraOutlines(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		Set<UUID> outlinedPlayers = new HashSet<>();
		if (PLUS_ULTRA_OUTLINE_RADIUS > 0.0) {
			Box area = player.getBoundingBox().expand(PLUS_ULTRA_OUTLINE_RADIUS);
			for (ServerPlayerEntity target : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
				if (target == player || !target.isAlive() || target.isSpectator() || target.getEntityWorld() != player.getEntityWorld()) {
					continue;
				}

				if (target.getBoundingBox().intersects(area)) {
					outlinedPlayers.add(target.getUuid());
				}
			}
		}

		Set<UUID> lastSent = PLUS_ULTRA_LAST_OUTLINED_PLAYERS.get(playerId);
		if (lastSent != null && lastSent.equals(outlinedPlayers)) {
			return;
		}

		PLUS_ULTRA_LAST_OUTLINED_PLAYERS.put(playerId, Set.copyOf(outlinedPlayers));
		sendCassiopeiaOutlinePayload(player, outlinedPlayers);
	}

	private static void syncPlusUltraOverheadText(ServerPlayerEntity player, PlusUltraState state, int currentTick) {
		if (!PLUS_ULTRA_OVERHEAD_TEXT_ENABLED || currentTick >= state.overheadTextEndTick()) {
			if (state.overheadTextEntityId() != null) {
				removePlusUltraOverheadText(player.getEntityWorld().getServer(), state);
				state.overheadTextEntityId(null);
			}
			return;
		}

		ArmorStandEntity textEntity = null;
		if (state.overheadTextEntityId() != null) {
			Entity existing = player.getEntityWorld().getServer().getWorld(state.dimension()) == null
				? null
				: player.getEntityWorld().getServer().getWorld(state.dimension()).getEntity(state.overheadTextEntityId());
			if (existing instanceof ArmorStandEntity armorStand && armorStand.isAlive()) {
				textEntity = armorStand;
			}
		}

		if (textEntity == null) {
			state.overheadTextEntityId(spawnPlusUltraOverheadText(player));
			if (state.overheadTextEntityId() == null) {
				return;
			}
			Entity existing = player.getEntityWorld().getServer().getWorld(player.getEntityWorld().getRegistryKey()).getEntity(state.overheadTextEntityId());
			if (existing instanceof ArmorStandEntity armorStand && armorStand.isAlive()) {
				textEntity = armorStand;
			}
		}

		if (textEntity == null) {
			return;
		}

		textEntity.setCustomName(plusUltraOverheadText());
		textEntity.setCustomNameVisible(true);
		textEntity.refreshPositionAndAngles(
			player.getX(),
			player.getY() + PLUS_ULTRA_OVERHEAD_TEXT_VERTICAL_OFFSET,
			player.getZ(),
			0.0F,
			0.0F
		);
	}

	private static UUID spawnPlusUltraOverheadText(ServerPlayerEntity player) {
		if (!PLUS_ULTRA_OVERHEAD_TEXT_ENABLED || PLUS_ULTRA_OVERHEAD_TEXT.isBlank() || PLUS_ULTRA_OVERHEAD_TEXT_DURATION_TICKS <= 0) {
			return null;
		}

		ServerWorld world = (ServerWorld) player.getEntityWorld();
		ArmorStandEntity textEntity = new ArmorStandEntity(world, player.getX(), player.getY() + PLUS_ULTRA_OVERHEAD_TEXT_VERTICAL_OFFSET, player.getZ());
		textEntity.setInvisible(true);
		textEntity.setInvulnerable(true);
		textEntity.setNoGravity(true);
		((ArmorStandEntityAccessorMixin) textEntity).magic$setMarker(true);
		textEntity.setSilent(true);
		textEntity.setCustomName(plusUltraOverheadText());
		textEntity.setCustomNameVisible(true);
		if (!world.spawnEntity(textEntity)) {
			return null;
		}
		return textEntity.getUuid();
	}

	private static void removePlusUltraOverheadText(MinecraftServer server, PlusUltraState state) {
		if (server == null || state == null || state.overheadTextEntityId() == null) {
			return;
		}

		ServerWorld world = server.getWorld(state.dimension());
		if (world == null) {
			return;
		}

		Entity entity = world.getEntity(state.overheadTextEntityId());
		if (entity != null) {
			entity.discard();
		}
	}

	private static Text plusUltraOverheadText() {
		return Text.literal(PLUS_ULTRA_OVERHEAD_TEXT).formatted(Formatting.RED, Formatting.BOLD);
	}

	private static void handleHerculesBurdenRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		if (activeAbility(player) == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			deactivateHerculesBurden(player, true, true);
			return;
		}

		int remainingTicks = herculesBurdenCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.HERCULES_BURDEN_OF_THE_SKY, remainingTicks, false);
			return;
		}

		int manaCost = (int) Math.ceil(manaFromPercentExact(HERCULES_ACTIVATION_COST_PERCENT));
		if (!canSpendAbilityCost(player, manaCost)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		LivingEntity primaryTarget = findLivingTargetInLineOfSight(player, HERCULES_TARGET_RANGE);
		if (primaryTarget == null) {
			player.sendMessage(Text.translatable("message.magic.ability.no_target", MagicAbility.HERCULES_BURDEN_OF_THE_SKY.displayName()), true);
			return;
		}

		List<LivingEntity> targets = findLivingTargetsAround(primaryTarget, HERCULES_SPLASH_RADIUS);
		targets.removeIf(target -> target == player);
		if (targets.isEmpty()) {
			player.sendMessage(Text.translatable("message.magic.ability.no_target", MagicAbility.HERCULES_BURDEN_OF_THE_SKY.displayName()), true);
			return;
		}

		spendAbilityCost(player, manaCost);
		activateHerculesBurden(player, targets, currentTick);
		recordOrionsGambitAbilityUse(player, MagicAbility.HERCULES_BURDEN_OF_THE_SKY);
	}

	private static void handleSagittariusAstralArrowRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		if (activeAbility(player) == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			player.sendMessage(Text.translatable("message.magic.constellation.still_charging", MagicAbility.SAGITTARIUS_ASTRAL_ARROW.displayName()), true);
			return;
		}

		int remainingTicks = sagittariusAstralArrowCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.SAGITTARIUS_ASTRAL_ARROW, remainingTicks, false);
			return;
		}

		int manaCost = (int) Math.ceil(manaFromPercentExact(SAGITTARIUS_ACTIVATION_COST_PERCENT));
		if (!canSpendAbilityCost(player, manaCost)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		spendAbilityCost(player, manaCost);
		setActiveAbility(player, MagicAbility.SAGITTARIUS_ASTRAL_ARROW);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		SAGITTARIUS_STATES.put(player.getUuid(), new SagittariusWindupState(currentTick + SAGITTARIUS_WINDUP_TICKS));
		applySagittariusWindupModifier(player);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.SAGITTARIUS_ASTRAL_ARROW.displayName()), true);
		recordOrionsGambitAbilityUse(player, MagicAbility.SAGITTARIUS_ASTRAL_ARROW);
	}

	private static void handleOrionsGambitRequest(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		int currentTick = player.getEntityWorld().getServer().getTicks();
		OrionGambitState existingState = ORIONS_GAMBIT_STATES.get(playerId);
		if (existingState != null) {
			if (existingState.stage == OrionGambitStage.WAITING) {
				endOrionsGambit(player, OrionGambitEndReason.WAITING_CANCEL, currentTick, true);
			} else {
				endOrionsGambit(player, OrionGambitEndReason.MANUAL_CANCEL, currentTick, true);
			}
			return;
		}

		int remainingTicks = orionsGambitCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.ORIONS_GAMBIT, remainingTicks, false);
			return;
		}

		setActiveAbility(player, MagicAbility.ORIONS_GAMBIT);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		ORIONS_GAMBIT_STATES.put(playerId, OrionGambitState.waiting());
		player.sendMessage(Text.translatable("message.magic.constellation.orion_waiting"), true);
		recordOrionsGambitAbilityUse(player, MagicAbility.ORIONS_GAMBIT);
	}

	private static void handleAstralCataclysmRequest(ServerPlayerEntity player, int currentTick) {
		ConstellationDomainState state = ASTRAL_CATACLYSM_DOMAIN_STATES.get(player.getUuid());
		if (state == null) {
			deactivateDomainExpansion(player);
			setActiveAbility(player, MagicAbility.NONE);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.ASTRAL_CATACLYSM.displayName()), true);
			return;
		}

		if (state.phase == ConstellationDomainPhase.CHARGING && currentTick < state.chargeCompleteTick) {
			player.sendMessage(Text.translatable("message.magic.constellation.still_charging", MagicAbility.ASTRAL_CATACLYSM.displayName()), true);
			return;
		}

		if (state.phase == ConstellationDomainPhase.CHARGING || state.phase == ConstellationDomainPhase.READY) {
			state.phase = ConstellationDomainPhase.ACQUIRING;
			state.acquireEndTick = currentTick + ASTRAL_CATACLYSM_ACQUIRE_WINDOW_TICKS;
			player.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.hunting"), true);
			return;
		}

		deactivateDomainExpansion(player);
		setActiveAbility(player, MagicAbility.NONE);
		player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.ASTRAL_CATACLYSM.displayName()), true);
	}

	private static void handleWittyOneLinerRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		int remainingTicks = wittyOneLinerCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.WITTY_ONE_LINER, remainingTicks, false);
			return;
		}

		int manaCost = (int) Math.ceil(manaFromPercentExact(WITTY_ONE_LINER_ACTIVATION_COST_PERCENT));
		if (!canSpendAbilityCost(player, manaCost)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		ServerPlayerEntity target = findPlayerTargetInLineOfSight(player, WITTY_ONE_LINER_RANGE);
		if (target == null) {
			player.sendMessage(Text.translatable("message.magic.ability.no_target", MagicAbility.WITTY_ONE_LINER.displayName()), true);
			return;
		}

		WittyOneLinerTierSettings selectedTier = selectWittyOneLinerTier(player);
		String joke = selectRandomJoke(player, selectedTier.jokes());
		if (joke == null || joke.isBlank()) {
			joke = MagicAbility.WITTY_ONE_LINER.displayName().getString();
		}

		spendAbilityCost(player, manaCost);
		if (!isCooldownDeferredByOrionsGambit(player.getUuid(), MagicAbility.WITTY_ONE_LINER) && selectedTier.cooldownTicks() > 0) {
			WITTY_ONE_LINER_COOLDOWN_END_TICK.put(player.getUuid(), currentTick + selectedTier.cooldownTicks());
		}
		applyWittyOneLinerEffects(target, selectedTier);
		sendWittyOneLinerOverlay(player, target, joke, selectedTier.colorRgb());
		recordOrionsGambitAbilityUse(player, MagicAbility.WITTY_ONE_LINER, selectedTier.cooldownTicks());
	}

	private static void handleBelowFreezingRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		if (activeAbility(player) == MagicAbility.BELOW_FREEZING && FROST_STAGE_STATES.containsKey(player.getUuid())) {
			endFrostStagedMode(player, currentTick, FrostStageEndReason.MANUAL, true, true);
			return;
		}

		int remainingTicks = belowFreezingCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.BELOW_FREEZING, remainingTicks, false);
			return;
		}

		if (!isOrionsGambitManaCostSuppressed(player) && MagicPlayerData.getMana(player) <= 0) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		activateFrostStagedMode(player, currentTick);
		recordOrionsGambitAbilityUse(player, MagicAbility.BELOW_FREEZING);
	}

	private static void handleFrostAscentRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		if (activeAbility(player) != MagicAbility.BELOW_FREEZING || !FROST_STAGE_STATES.containsKey(player.getUuid())) {
			return;
		}

		int remainingTicks = frostAscentCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.FROST_ASCENT, remainingTicks, false);
			return;
		}

		if (!advanceFrostStage(player, currentTick)) {
			player.sendMessage(Text.translatable("message.magic.frost.next_stage_locked"), true);
		}
	}

	private static void handleAbsoluteZeroRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		if (activeAbility(player) == MagicAbility.ABSOLUTE_ZERO && FROST_MAXIMUM_STATES.containsKey(player.getUuid())) {
			player.sendMessage(Text.translatable("message.magic.frost.maximum_locked"), true);
			return;
		}

		int remainingTicks = absoluteZeroCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.ABSOLUTE_ZERO, remainingTicks, false);
			return;
		}

		if (!FROST_CONFIG.maximum.enabled) {
			player.sendMessage(Text.translatable("message.magic.frost.maximum_disabled"), true);
			return;
		}

		activateFrostMaximum(player, currentTick);
		recordOrionsGambitAbilityUse(player, MagicAbility.ABSOLUTE_ZERO);
	}

	private static void handlePlanckHeatRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		if (activeAbility(player) != MagicAbility.BELOW_FREEZING || !FROST_STAGE_STATES.containsKey(player.getUuid())) {
			player.sendMessage(Text.translatable("message.magic.frost.staged_mode_required"), true);
			return;
		}

		int remainingTicks = planckHeatCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.PLANCK_HEAT, remainingTicks, false);
			return;
		}

		if (player.isSneaking()) {
			if (!castFrostSlam(player, currentTick)) {
				player.sendMessage(Text.translatable("message.magic.frost.slam_locked"), true);
			}
			return;
		}

		castFrostRangedAttack(player, currentTick);
		recordOrionsGambitAbilityUse(player, MagicAbility.PLANCK_HEAT);
	}

	private static void activateFrostStagedMode(ServerPlayerEntity player, int currentTick) {
		UUID playerId = player.getUuid();
		FrostStageState state = FROST_STAGE_STATES.get(playerId);
		if (state == null) {
			state = new FrostStageState(player.getEntityWorld().getRegistryKey(), 1, 1, 0);
			FROST_STAGE_STATES.put(playerId, state);
		} else {
			state.dimension = player.getEntityWorld().getRegistryKey();
			state.currentStage = 1;
		}
		setActiveAbility(player, MagicAbility.BELOW_FREEZING);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		if (belowFreezingCooldownRemaining(player, currentTick) <= 0) {
			BELOW_FREEZING_COOLDOWN_END_TICK.remove(playerId);
		}
		syncFrostStageHud(player);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.BELOW_FREEZING.displayName()), true);
	}

	private static void applyFrostStagedMode(ServerPlayerEntity player, int currentTick) {
		FrostStageState state = FROST_STAGE_STATES.get(player.getUuid());
		if (state == null || state.dimension != player.getEntityWorld().getRegistryKey()) {
			endFrostStagedMode(player, currentTick, FrostStageEndReason.INVALID, false, false);
			return;
		}

		if (isBelowFrostThreshold(player)) {
			endFrostStagedMode(player, currentTick, FrostStageEndReason.FORCED_THRESHOLD, true, false);
			player.sendMessage(Text.translatable("message.magic.frost.threshold_shutdown"), true);
			return;
		}

		MagicConfig.FrostStageConfig stageConfig = frostStageConfig(state.currentStage);
		if (stageConfig.cleanseCommonNegatives) {
			cleanseCommonNegativeEffects(player);
		}
		if (stageConfig.resistanceAmplifier >= 0) {
			refreshStatusEffect(player, StatusEffects.RESISTANCE, EFFECT_REFRESH_TICKS, stageConfig.resistanceAmplifier, true, false, true);
		}
		if (stageConfig.slownessAmplifier >= 0) {
			refreshStatusEffect(player, StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, stageConfig.slownessAmplifier, true, false, true);
		}
		applyFrostStageMaxHealth(player, stageConfig.maxHealthMultiplier, stageConfig.enableMaxHealthMultiplier);
		if (state.currentStage == 1) {
			applyFrostStageOneAura(player, currentTick);
		}
		spawnFrostStageParticles(player, state.currentStage);
		progressFrostStageUnlocks(player, state);
		syncFrostStageHud(player);
	}

	private static void endFrostStagedMode(
		ServerPlayerEntity player,
		int currentTick,
		FrostStageEndReason reason,
		boolean startCooldown,
		boolean sendDeactivatedMessage
	) {
		UUID playerId = player.getUuid();
		FrostStageState state = FROST_STAGE_STATES.get(playerId);
		if (state != null) {
			state.currentStage = 1;
			state.progressTicks = 0;
			state.dimension = player.getEntityWorld().getRegistryKey();
			if (FROST_CONFIG.progression.clearUnlocksOnEnd || reason == FrostStageEndReason.FORCED_THRESHOLD || reason == FrostStageEndReason.OVERCAST) {
				FROST_STAGE_STATES.remove(playerId);
			}
		}
		clearFrostEffectsByCaster(playerId, false, player.getEntityWorld().getServer());
		removeFrostStageCasterBuffs(player);
		if (activeAbility(player) == MagicAbility.BELOW_FREEZING) {
			setActiveAbility(player, MagicAbility.NONE);
		}
		if (startCooldown) {
			startAbilityCooldownFromNow(playerId, MagicAbility.BELOW_FREEZING, currentTick);
		}
		MagicPlayerData.clearFrostStageHud(player);
		if (sendDeactivatedMessage) {
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.BELOW_FREEZING.displayName()), true);
		}
	}

	private static boolean advanceFrostStage(ServerPlayerEntity player, int currentTick) {
		FrostStageState state = FROST_STAGE_STATES.get(player.getUuid());
		if (state == null || state.currentStage >= 3 || state.highestUnlockedStage <= state.currentStage) {
			return false;
		}
		state.currentStage = Math.min(3, state.currentStage + 1);
		state.progressTicks = 0;
		if (FROST_ASCENT_COOLDOWN_TICKS > 0) {
			startAbilityCooldownFromNow(player.getUuid(), MagicAbility.FROST_ASCENT, currentTick);
		}
		syncFrostStageHud(player);
		player.sendMessage(Text.translatable("message.magic.frost.stage_advanced", state.currentStage), true);
		return true;
	}

	private static void castFrostRangedAttack(ServerPlayerEntity player, int currentTick) {
		FrostStageState state = FROST_STAGE_STATES.get(player.getUuid());
		if (state == null) {
			return;
		}
		boolean overcast = player.getRandom().nextDouble() * 100.0 < FROST_CONFIG.rangedAttack.overcastChancePercent;
		int manaCost = (int) Math.ceil(manaFromPercentExact(
			overcast ? FROST_CONFIG.rangedAttack.overcastManaCostPercent : FROST_CONFIG.rangedAttack.normalManaCostPercent
		));
		if (!canSpendAbilityCost(player, manaCost)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		Vec3d direction = player.getRotationVector();
		direction = new Vec3d(direction.x, 0.0, direction.z);
		if (direction.lengthSquared() <= 1.0E-6) {
			float yawRadians = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
			direction = new Vec3d(-MathHelper.sin(yawRadians), 0.0, MathHelper.cos(yawRadians));
		}
		direction = direction.normalize();
		spendAbilityCost(player, manaCost);
		if (PLANCK_HEAT_COOLDOWN_TICKS > 0) {
			startAbilityCooldownFromNow(player.getUuid(), MagicAbility.PLANCK_HEAT, currentTick);
		}
		FROST_SPIKE_WAVES.add(
			new FrostSpikeWaveState(
				player.getUuid(),
				player.getEntityWorld().getRegistryKey(),
				new Vec3d(player.getX(), player.getY() + 0.1, player.getZ()),
				direction,
				FROST_CONFIG.rangedAttack.range,
				(overcast ? FROST_CONFIG.rangedAttack.overcastSpeedBlocksPerSecond : FROST_CONFIG.rangedAttack.speedBlocksPerSecond) / TICKS_PER_SECOND,
				FROST_CONFIG.rangedAttack.width,
				FROST_CONFIG.rangedAttack.baseDamage,
				state.currentStage,
				overcast
			)
		);
		if (player.getEntityWorld() instanceof ServerWorld world) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8F, overcast ? 0.5F : 0.9F);
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.9F, 1.6F);
		}
		if (!overcast && player.getRandom().nextDouble() * 100.0 < FROST_CONFIG.rangedAttack.setbackChancePercent) {
			applyFrostSetback(player, state.currentStage - 1);
		}
		if (overcast) {
			endFrostStagedMode(player, currentTick, FrostStageEndReason.OVERCAST, true, false);
		} else if (isBelowFrostThreshold(player)) {
			endFrostStagedMode(player, currentTick, FrostStageEndReason.FORCED_THRESHOLD, true, false);
		}
	}

	private static boolean castFrostSlam(ServerPlayerEntity player, int currentTick) {
		FrostStageState state = FROST_STAGE_STATES.get(player.getUuid());
		if (state == null || state.currentStage <= 1) {
			return false;
		}

		int slamStage = MathHelper.clamp(state.currentStage, 2, 3);
		MagicConfig.FrostSlamConfig slamConfig = slamStage == 3 ? FROST_CONFIG.stageThreeSlam : FROST_CONFIG.stageTwoSlam;
		float slamDamage = slamConfig.trueDamage;
		int manaCost = (int) Math.ceil(manaFromPercentExact(slamConfig.manaCostPercent));
		if (!canSpendAbilityCost(player, manaCost)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return true;
		}

		spendAbilityCost(player, manaCost);
		if (PLANCK_HEAT_COOLDOWN_TICKS > 0) {
			startAbilityCooldownFromNow(player.getUuid(), MagicAbility.PLANCK_HEAT, currentTick);
		}
		if (player.getEntityWorld() instanceof ServerWorld world) {
			spawnFrostSlamParticles(world, player, slamConfig, slamStage == 3);
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.1F, slamStage == 3 ? 0.45F : 0.8F);
		}
		for (LivingEntity target : findLivingTargetsAround(player, slamConfig.radius)) {
			if (target == player || !target.isAlive()) {
				continue;
			}
			applyFrostActionDamage(player, target, slamDamage, false, FrostKillProgressType.SLAM);
			applyFrostStageHitEffects(player, target, currentTick);
			applyFrostFreeze(target, player.getUuid(), currentTick + slamConfig.freezeDurationTicks);
		}
		if (slamStage == 2 && player.getRandom().nextDouble() * 100.0 < slamConfig.setbackChancePercent) {
			applyFrostSetback(player, 1);
		}
		if (isBelowFrostThreshold(player)) {
			endFrostStagedMode(player, currentTick, FrostStageEndReason.FORCED_THRESHOLD, true, false);
		}
		return true;
	}

	private static void activateFrostMaximum(ServerPlayerEntity player, int currentTick) {
		if (activeAbility(player) == MagicAbility.BELOW_FREEZING) {
			endFrostStagedMode(player, currentTick, FrostStageEndReason.MAXIMUM, false, false);
		}
		clearFrostMaximumState(player, false);
		setActiveAbility(player, MagicAbility.ABSOLUTE_ZERO);
		MagicPlayerData.setMana(player, 0);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		if (FROST_CONFIG.maximum.manaRegenBlockedTicks > 0) {
			FROST_MANA_REGEN_BLOCKED_END_TICK.put(player.getUuid(), currentTick + FROST_CONFIG.maximum.manaRegenBlockedTicks);
		} else {
			FROST_MANA_REGEN_BLOCKED_END_TICK.remove(player.getUuid());
		}
		for (MagicAbility ability : abilitiesForSchool(MagicSchool.FROST)) {
			startAbilityCooldownFromNow(player.getUuid(), ability, currentTick);
		}
		FROST_MAXIMUM_STATES.put(
			player.getUuid(),
			new FrostMaximumState(
				player.getEntityWorld().getRegistryKey(),
				player.getX(),
				player.getY(),
				player.getZ(),
				currentTick + FROST_CONFIG.maximum.windupDurationTicks
			)
		);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.ABSOLUTE_ZERO.displayName()), true);
	}

	private static void applyFrostMaximum(ServerPlayerEntity player, int currentTick) {
		FrostMaximumState state = FROST_MAXIMUM_STATES.get(player.getUuid());
		if (state == null || state.dimension != player.getEntityWorld().getRegistryKey()) {
			clearFrostMaximumState(player, false);
			return;
		}
		if (state.phase == FrostMaximumPhase.WINDUP) {
			applyFrostMaximumWindup(player, state, currentTick);
			return;
		}
		if (state.phase == FrostMaximumPhase.ENCASE && currentTick >= state.encaseEndTick) {
			finishFrostMaximum(player, state, currentTick);
		}
	}

	private static void applyFrostMaximumWindup(ServerPlayerEntity player, FrostMaximumState state, int currentTick) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		double targetY = state.baseY + FROST_CONFIG.maximum.floatHeightBlocks;
		double risePerTick = FROST_CONFIG.maximum.windupDurationTicks <= 0
			? FROST_CONFIG.maximum.floatHeightBlocks
			: FROST_CONFIG.maximum.floatHeightBlocks / Math.max(1, FROST_CONFIG.maximum.windupDurationTicks);
		double nextY = Math.min(targetY, player.getY() + risePerTick);
		teleportDomainEntity(player, player.getX(), nextY, player.getZ(), player.getYaw(), player.getPitch());
		spawnFrostMaximumParticles(world, player, state, currentTick);
		double fearRadius = FROST_CONFIG.domain.radius * FROST_CONFIG.maximum.fearRadiusMultiplierFromDomainRadius;
		Box fearBox = player.getBoundingBox().expand(fearRadius);
		Predicate<Entity> filter = entity -> entity instanceof LivingEntity living && living.isAlive() && entity != player;
		for (Entity entity : player.getEntityWorld().getOtherEntities(player, fearBox, filter)) {
			if (entity instanceof LivingEntity living && isFrostEnemy(player, living)) {
				FROST_MAXIMUM_FEAR_TARGETS.put(
					living.getUuid(),
					new FrostFearState(
						living.getEntityWorld().getRegistryKey(),
						player.getUuid(),
						living.getX(),
						living.getY(),
						living.getZ(),
						state.windupEndTick
					)
				);
			}
		}
		if (currentTick >= state.windupEndTick) {
			beginFrostMaximumBurst(player, state, currentTick);
		}
	}

	private static void beginFrostMaximumBurst(ServerPlayerEntity player, FrostMaximumState state, int currentTick) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		double burstRadius = FROST_CONFIG.domain.radius * FROST_CONFIG.maximum.burstRadiusMultiplierFromDomainRadius;
		state.phase = FrostMaximumPhase.ENCASE;
		state.encaseEndTick = currentTick + FROST_CONFIG.maximum.packedIceDurationTicks;
		state.pendingHelplessTargets.clear();
		Box burstBox = player.getBoundingBox().expand(burstRadius);
		Predicate<Entity> filter = entity -> entity instanceof LivingEntity living && living.isAlive() && entity != player;
		for (Entity entity : player.getEntityWorld().getOtherEntities(player, burstBox, filter)) {
			if (entity instanceof LivingEntity livingTarget) {
				if (livingTarget instanceof PlayerEntity spectatorCheck && spectatorCheck.isSpectator()) {
					continue;
				}
				FrostPackedIceState packedIceState = new FrostPackedIceState(
					livingTarget.getEntityWorld().getRegistryKey(),
					player.getUuid(),
					livingTarget.getX(),
					livingTarget.getY(),
					livingTarget.getZ(),
					currentTick + FROST_CONFIG.maximum.packedIceDurationTicks,
					currentTick,
					Math.max(0.7, livingTarget.getWidth() * 0.85),
					Math.max(1.4, livingTarget.getHeight() + 0.4)
				);
				spawnFrostPackedIceEncasement(world, livingTarget, packedIceState);
				FROST_PACKED_ICE_TARGETS.put(livingTarget.getUuid(), packedIceState);
				world.spawnParticles(
					new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
					livingTarget.getX(),
					livingTarget.getBodyY(0.5),
					livingTarget.getZ(),
					22,
					0.38,
					0.72,
					0.38,
					0.05
				);
			}
			if (entity instanceof LivingEntity living && isFrostEnemy(player, living)) {
				state.pendingHelplessTargets.put(
					living.getUuid(),
					new FrostLockedTargetSeed(living.getEntityWorld().getRegistryKey(), living.getX(), living.getY(), living.getZ())
				);
			}
		}
		spawnFrostMaximumBurstParticles(world, player, burstRadius);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.4F, 0.4F);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1.1F, 0.55F);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.6F);
	}

	private static void finishFrostMaximum(ServerPlayerEntity player, FrostMaximumState state, int currentTick) {
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server == null) {
			return;
		}
		for (Map.Entry<UUID, FrostLockedTargetSeed> entry : state.pendingHelplessTargets.entrySet()) {
			ServerWorld targetWorld = server.getWorld(entry.getValue().dimension);
			if (targetWorld == null) {
				continue;
			}
			Entity entity = targetWorld.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
				continue;
			}
			FROST_HELPLESS_TARGETS.put(
				target.getUuid(),
				new FrostHelplessState(
					target.getEntityWorld().getRegistryKey(),
					player.getUuid(),
					entry.getValue().x,
					entry.getValue().y,
					entry.getValue().z,
					currentTick + FROST_CONFIG.maximum.postBurstFreezeDurationTicks
				)
			);
		}
		FROST_MAXIMUM_STATES.remove(player.getUuid());
		FROST_MAXIMUM_FEAR_TARGETS.entrySet().removeIf(entry -> player.getUuid().equals(entry.getValue().casterId));
		if (activeAbility(player) == MagicAbility.ABSOLUTE_ZERO) {
			setActiveAbility(player, MagicAbility.NONE);
		}
	}

	private static void clearFrostMaximumState(ServerPlayerEntity player, boolean clearCooldown) {
		if (player == null) {
			return;
		}
		MinecraftServer server = player.getEntityWorld().getServer();
		if (clearCooldown) {
			startAbilityCooldownFromNow(player.getUuid(), MagicAbility.ABSOLUTE_ZERO, player.getEntityWorld().getServer().getTicks());
		}
		FROST_MAXIMUM_STATES.remove(player.getUuid());
		FROST_MAXIMUM_FEAR_TARGETS.entrySet().removeIf(entry -> player.getUuid().equals(entry.getValue().casterId));
		Iterator<Map.Entry<UUID, FrostPackedIceState>> iterator = FROST_PACKED_ICE_TARGETS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, FrostPackedIceState> entry = iterator.next();
			if (!player.getUuid().equals(entry.getValue().casterId)) {
				continue;
			}
			clearFrostPackedIceEncasement(server, entry.getValue(), false);
			iterator.remove();
		}
		FROST_HELPLESS_TARGETS.entrySet().removeIf(entry -> player.getUuid().equals(entry.getValue().casterId));
		if (activeAbility(player) == MagicAbility.ABSOLUTE_ZERO) {
			setActiveAbility(player, MagicAbility.NONE);
		}
	}

	private static void progressFrostStageUnlocks(ServerPlayerEntity player, FrostStageState state) {
		int requirement = frostProgressRequirementTicks(state.currentStage);
		if (requirement <= 0 || state.highestUnlockedStage > state.currentStage) {
			return;
		}
		state.progressTicks = Math.min(requirement, state.progressTicks + 1);
		if (state.progressTicks >= requirement && state.highestUnlockedStage == state.currentStage) {
			state.highestUnlockedStage = Math.min(3, state.currentStage + 1);
			player.sendMessage(Text.translatable("message.magic.frost.stage_unlocked", state.highestUnlockedStage), true);
		}
	}

	private static void applyFrostStageOneAura(ServerPlayerEntity player, int currentTick) {
		spawnCasterAuraParticles(player);
		double auraRadius = Math.max(0.0, FROST_CONFIG.stageOne.auraRadius);
		if (auraRadius <= 0.0) {
			return;
		}

		int durationTicks = Math.max(0, FROST_CONFIG.stageOne.onHitDebuffDurationTicks);
		float damagePerTick = FROST_CONFIG.stageOne.onHitFrostDamagePerTick > 0.0F
			? FROST_CONFIG.stageOne.onHitFrostDamagePerTick
			: FROST_CONFIG.debuff.baseDamagePerTick;
		if (durationTicks <= 0 || damagePerTick <= 0.0F) {
			return;
		}

		for (LivingEntity target : findLivingTargetsAround(player, auraRadius)) {
			if (target == player || !isFrostEnemy(player, target)) {
				continue;
			}
			applyOrRefreshFrostbite(
				target,
				player.getUuid(),
				currentTick,
				durationTicks,
				damagePerTick,
				FROST_CONFIG.debuff.refreshDurationOnReapply,
				FROST_CONFIG.debuff.stackDamageOnReapply
			);
		}
	}

	private static void applyFrostSetback(ServerPlayerEntity player, int targetStage) {
		FrostStageState state = FROST_STAGE_STATES.get(player.getUuid());
		if (state == null) {
			return;
		}
		int nextProgress = FROST_CONFIG.progression.resetProgressOnSetback
			? 0
			: Math.min(state.progressTicks, Math.max(0, frostProgressRequirementTicks(Math.max(1, targetStage))));
		if (targetStage <= 1) {
			state.currentStage = 1;
			state.highestUnlockedStage = 1;
			state.progressTicks = nextProgress;
		} else {
			state.currentStage = Math.min(3, targetStage);
			state.highestUnlockedStage = Math.min(state.currentStage, 3);
			state.progressTicks = nextProgress;
		}
		syncFrostStageHud(player);
		player.sendMessage(Text.translatable("message.magic.frost.stage_setback", state.currentStage), true);
	}

	private static void applyFrostStageHitEffects(ServerPlayerEntity caster, LivingEntity target, int currentTick) {
		FrostStageState state = FROST_STAGE_STATES.get(caster.getUuid());
		if (state == null) {
			return;
		}
		MagicConfig.FrostStageConfig stageConfig = frostStageConfig(state.currentStage);
		if (stageConfig.onHitDebuffDurationTicks <= 0) {
			return;
		}
		if (stageConfig.onHitSlownessAmplifier >= 0) {
			refreshStatusEffect(
				target,
				StatusEffects.SLOWNESS,
				stageConfig.onHitDebuffDurationTicks,
				stageConfig.onHitSlownessAmplifier,
				true,
				true,
				true
			);
		}
		if (stageConfig.onHitBlindnessAmplifier >= 0) {
			refreshStatusEffect(
				target,
				StatusEffects.BLINDNESS,
				stageConfig.onHitDebuffDurationTicks,
				stageConfig.onHitBlindnessAmplifier,
				true,
				true,
				true
			);
		}
		float frostDamagePerTick = stageConfig.onHitFrostDamagePerTick > 0.0F
			? stageConfig.onHitFrostDamagePerTick
			: FROST_CONFIG.debuff.baseDamagePerTick;
		if (frostDamagePerTick > 0.0F) {
			applyOrRefreshFrostbite(
				target,
				caster.getUuid(),
				currentTick,
				stageConfig.onHitDebuffDurationTicks,
				frostDamagePerTick,
				FROST_CONFIG.debuff.refreshDurationOnReapply,
				FROST_CONFIG.debuff.stackDamageOnReapply
			);
		}
	}

	private static void applyFrostActionDamage(
		ServerPlayerEntity caster,
		LivingEntity target,
		float damage,
		boolean overcast,
		FrostKillProgressType progressType
	) {
		if (!(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		if (overcast && FROST_CONFIG.rangedAttack.instantKillEnabled) {
			dealTrackedMagicDamage(target, caster.getUuid(), world.getDamageSources().freeze(), Float.MAX_VALUE);
		} else if (damage > 0.0F) {
			dealTrackedMagicDamage(target, caster.getUuid(), world.getDamageSources().genericKill(), damage);
		}
		if (target.isAlive()) {
			return;
		}
		grantFrostKillProgress(caster, progressType);
	}

	private static void grantFrostKillProgress(ServerPlayerEntity caster, FrostKillProgressType progressType) {
		FrostStageState state = FROST_STAGE_STATES.get(caster.getUuid());
		if (state == null) {
			return;
		}
		int requirement = frostProgressRequirementTicks(state.currentStage);
		if (requirement <= 0 || state.highestUnlockedStage > state.currentStage) {
			return;
		}
		int bonus = progressType == FrostKillProgressType.RANGED
			? FROST_CONFIG.progression.rangedKillProgressTicks
			: FROST_CONFIG.progression.slamKillProgressTicks;
		if (bonus <= 0) {
			return;
		}
		if (FROST_CONFIG.progression.discardExcessProgress) {
			state.progressTicks = Math.min(requirement, state.progressTicks + bonus);
		} else {
			state.progressTicks += bonus;
		}
		if (state.progressTicks >= requirement) {
			state.progressTicks = requirement;
			state.highestUnlockedStage = Math.min(3, state.currentStage + 1);
			caster.sendMessage(Text.translatable("message.magic.frost.stage_unlocked", state.highestUnlockedStage), true);
		}
		syncFrostStageHud(caster);
	}

	private static void applyFrostFreeze(LivingEntity target, UUID casterId, int expiresTick) {
		FROST_FROZEN_TARGETS.put(
			target.getUuid(),
			new FrostFreezeState(
				target.getEntityWorld().getRegistryKey(),
				casterId,
				target.getX(),
				target.getY(),
				target.getZ(),
				expiresTick
			)
		);
	}

	private static void updateFrostSpikeWaves(MinecraftServer server, int currentTick) {
		Iterator<FrostSpikeWaveState> iterator = FROST_SPIKE_WAVES.iterator();
		while (iterator.hasNext()) {
			FrostSpikeWaveState state = iterator.next();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null) {
				iterator.remove();
				continue;
			}
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(state.casterId);
			if (caster == null || !caster.isAlive()) {
				iterator.remove();
				continue;
			}
			double previousTravel = state.travelled;
			double intendedTravel = Math.min(state.range, previousTravel + state.speedPerTick);
			state.travelled = intendedTravel;
			double segmentSpacing = Math.max(
				FROST_CONFIG.rangedAttack.terrainFollowStepSize,
				FROST_CONFIG.rangedAttack.eruptionSegmentSpacing
			);
			while (state.nextEruptionDistance <= intendedTravel + 1.0E-6) {
				Vec3d samplePoint = new Vec3d(
					state.origin.x + state.direction.x * state.nextEruptionDistance,
					state.terrainSearchY,
					state.origin.z + state.direction.z * state.nextEruptionDistance
				);
				Vec3d groundAnchor = findFrostGroundAnchor(
					world,
					samplePoint,
					FROST_CONFIG.rangedAttack.groundSearchUpBlocks,
					FROST_CONFIG.rangedAttack.groundSearchDownBlocks
				);
				if (groundAnchor == null) {
					state.consecutiveGroundMisses++;
					state.nextEruptionDistance += segmentSpacing;
					if (state.consecutiveGroundMisses >= 5) {
						state.travelled = state.range;
						break;
					}
					continue;
				}
				state.consecutiveGroundMisses = 0;
				state.terrainSearchY = groundAnchor.y + 0.8;
				spawnFrostGroundSpikeEruption(world, groundAnchor, state.direction, state.width, state.overcast, state.stage);
				Vec3d hitStart = groundAnchor.add(0.0, 0.45, 0.0).subtract(state.direction.multiply(segmentSpacing * 0.5));
				Vec3d hitEnd = groundAnchor.add(0.0, 0.45, 0.0).add(state.direction.multiply(segmentSpacing * 0.5));
				for (LivingEntity target : collectLivingEntitiesAlongBeam(caster, hitStart, hitEnd, Math.max(0.4, state.width * 0.5))) {
					if (!state.hitTargets.add(target.getUuid())) {
						continue;
					}
					applyFrostActionDamage(caster, target, state.damage, state.overcast, FrostKillProgressType.RANGED);
					if (!state.overcast) {
						applyFrostStageHitEffects(caster, target, currentTick);
					}
				}
				state.nextEruptionDistance += segmentSpacing;
			}
			if (state.travelled >= state.range || state.nextEruptionDistance > state.range + segmentSpacing) {
				iterator.remove();
			}
		}
	}

	private static void updateFrostFrozenTargets(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, FrostFreezeState>> iterator = FROST_FROZEN_TARGETS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, FrostFreezeState> entry = iterator.next();
			FrostFreezeState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null || currentTick > state.expiresTick) {
				iterator.remove();
				continue;
			}
			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				iterator.remove();
				continue;
			}
			teleportDomainEntity(living, state.lockedX, state.lockedY, state.lockedZ, living.getYaw(), living.getPitch());
			if (living instanceof MobEntity mob) {
				mob.getNavigation().stop();
			}
			spawnTargetSnowParticles(living);
		}
	}

	private static void updateFrostMaximumFearTargets(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, FrostFearState>> iterator = FROST_MAXIMUM_FEAR_TARGETS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, FrostFearState> entry = iterator.next();
			FrostFearState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null || currentTick > state.expiresTick) {
				iterator.remove();
				continue;
			}
			Entity entity = world.getEntity(entry.getKey());
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(state.casterId);
			if (!(entity instanceof LivingEntity target) || !target.isAlive() || caster == null || !caster.isAlive()) {
				iterator.remove();
				continue;
			}
			float[] facing = computeFacingAngles(target, caster.getEyePos());
			teleportDomainEntity(target, state.lockedX, state.lockedY, state.lockedZ, facing[0], facing[1]);
			if (target instanceof MobEntity mob) {
				mob.getNavigation().stop();
			}
		}
	}

	private static void updateFrostPackedIceTargets(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, FrostPackedIceState>> iterator = FROST_PACKED_ICE_TARGETS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, FrostPackedIceState> entry = iterator.next();
			FrostPackedIceState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null) {
				clearFrostPackedIceEncasement(server, state, false);
				iterator.remove();
				continue;
			}
			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
				clearFrostPackedIceEncasement(world, state, false);
				iterator.remove();
				continue;
			}
			if (currentTick >= state.expiresTick) {
				clearFrostPackedIceEncasement(world, state, true);
				world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.9F, 0.7F);
				iterator.remove();
				continue;
			}
			teleportDomainEntity(target, state.lockedX, state.lockedY, state.lockedZ, target.getYaw(), target.getPitch());
			if (target instanceof MobEntity mob) {
				mob.getNavigation().stop();
			}
			world.spawnParticles(
				new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
				target.getX(),
				target.getBodyY(0.5),
				target.getZ(),
				8,
				0.32,
				0.65,
				0.32,
				0.01
			);
			if (currentTick >= state.nextDamageTick) {
				dealTrackedMagicDamage(target, state.casterId, world.getDamageSources().inWall(), FROST_CONFIG.maximum.suffocationDamagePerInterval);
				state.nextDamageTick = currentTick + FROST_CONFIG.maximum.suffocationDamageIntervalTicks;
			}
		}
	}

	private static void updateFrostHelplessTargets(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, FrostHelplessState>> iterator = FROST_HELPLESS_TARGETS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, FrostHelplessState> entry = iterator.next();
			FrostHelplessState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null || currentTick > state.expiresTick) {
				iterator.remove();
				continue;
			}
			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
				iterator.remove();
				continue;
			}
			teleportDomainEntity(target, state.lockedX, state.lockedY, state.lockedZ, target.getYaw(), target.getPitch());
			target.stopUsingItem();
			if (target instanceof MobEntity mob) {
				mob.getNavigation().stop();
				mob.setTarget(null);
			}
			spawnTargetSnowParticles(target);
		}
	}

	private static void clearFrostEffectsByCaster(UUID casterId, boolean clearMaximumEffects, MinecraftServer server) {
		FROST_SPIKE_WAVES.removeIf(state -> casterId.equals(state.casterId));
		FROSTBITTEN_TARGETS.entrySet().removeIf(entry -> casterId.equals(entry.getValue().casterId));
		FROST_FROZEN_TARGETS.entrySet().removeIf(entry -> casterId.equals(entry.getValue().casterId));
		if (clearMaximumEffects) {
			FROST_MAXIMUM_FEAR_TARGETS.entrySet().removeIf(entry -> casterId.equals(entry.getValue().casterId));
			Iterator<Map.Entry<UUID, FrostPackedIceState>> iterator = FROST_PACKED_ICE_TARGETS.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<UUID, FrostPackedIceState> entry = iterator.next();
				if (!casterId.equals(entry.getValue().casterId)) {
					continue;
				}
				clearFrostPackedIceEncasement(server, entry.getValue(), false);
				iterator.remove();
			}
			FROST_HELPLESS_TARGETS.entrySet().removeIf(entry -> casterId.equals(entry.getValue().casterId));
		}
	}

	private static void clearFrostControlledTargetState(UUID targetId, MinecraftServer server) {
		FROST_FROZEN_TARGETS.remove(targetId);
		FROST_MAXIMUM_FEAR_TARGETS.remove(targetId);
		FROST_HELPLESS_TARGETS.remove(targetId);
		FrostPackedIceState packedIceState = FROST_PACKED_ICE_TARGETS.remove(targetId);
		if (packedIceState != null) {
			clearFrostPackedIceEncasement(server, packedIceState, false);
		}
	}

	private static void removeFrostStageCasterBuffs(ServerPlayerEntity player) {
		removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), FROST_STAGE_MAX_HEALTH_MODIFIER_ID);
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		removeStatusEffectIfMatching(player, StatusEffects.SLOWNESS, frostPassiveAmplifiers(true));
		removeStatusEffectIfMatching(player, StatusEffects.RESISTANCE, frostPassiveAmplifiers(false));
	}

	private static void applyFrostStageMaxHealth(ServerPlayerEntity player, double multiplier, boolean enabled) {
		EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		double value = enabled ? Math.max(0.0, multiplier - 1.0) : 0.0;
		EntityAttributeModifier existing = maxHealth.getModifier(FROST_STAGE_MAX_HEALTH_MODIFIER_ID);
		double previousValue = existing == null ? 0.0 : existing.value();
		overwriteAttributeModifier(
			maxHealth,
			FROST_STAGE_MAX_HEALTH_MODIFIER_ID,
			value,
			EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);
		if (value > previousValue) {
			double previousMax = value >= 0.0 ? player.getMaxHealth() / (1.0 + value) * (1.0 + previousValue) : player.getMaxHealth();
			double addedHealth = Math.max(0.0, player.getMaxHealth() - previousMax);
			if (addedHealth > 0.0) {
				player.setHealth((float) Math.min(player.getMaxHealth(), player.getHealth() + addedHealth));
			}
		} else if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	private static int frostProgressRequirementTicks(int currentStage) {
		return switch (currentStage) {
			case 1 -> FROST_CONFIG.progression.stageTwoUnlockTicks;
			case 2 -> FROST_CONFIG.progression.stageThreeUnlockTicks;
			default -> 0;
		};
	}

	private static MagicConfig.FrostStageConfig frostStageConfig(int stage) {
		return switch (stage) {
			case 2 -> FROST_CONFIG.stageTwo;
			case 3 -> FROST_CONFIG.stageThree;
			default -> FROST_CONFIG.stageOne;
		};
	}

	private static boolean isBelowFrostThreshold(ServerPlayerEntity player) {
		double threshold = MagicPlayerData.MAX_MANA * (FROST_CONFIG.stagedModeForceEndThresholdPercent / 100.0);
		return MagicPlayerData.getMana(player) < Math.ceil(threshold);
	}

	private static void spawnFrostStageParticles(ServerPlayerEntity player, int stage) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		double x = player.getX();
		double y = player.getBodyY(0.5);
		double z = player.getZ();
		int ashCount = stage == 1 ? 4 : stage == 2 ? 7 : 10;
		int snowCount = stage == 1 ? 2 : stage == 2 ? 5 : 8;
		world.spawnParticles(ParticleTypes.WHITE_ASH, x, y, z, ashCount, 0.35 + stage * 0.08, 0.5, 0.35 + stage * 0.08, 0.01);
		world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, snowCount, 0.4 + stage * 0.1, 0.6, 0.4 + stage * 0.1, 0.015);
	}

	private static void spawnFrostSlamParticles(
		ServerWorld world,
		LivingEntity caster,
		MagicConfig.FrostSlamConfig slamConfig,
		boolean erupting
	) {
		if (slamConfig.particleCount <= 0 && slamConfig.hazeDensity <= 0) {
			return;
		}

		Vec3d centerAnchor = findFrostGroundAnchor(world, new Vec3d(caster.getX(), caster.getY() + 1.0, caster.getZ()), 3, 6);
		if (centerAnchor == null) {
			centerAnchor = new Vec3d(caster.getX(), caster.getY(), caster.getZ());
		}
		double ringRadius = slamConfig.groundRingRadius > 0.0 ? slamConfig.groundRingRadius : slamConfig.radius;
		spawnFrostGroundHaze(world, centerAnchor, Math.max(1.0, ringRadius), slamConfig.hazeDensity);
		spawnFrostGroundShockRing(world, centerAnchor, Math.max(0.75, ringRadius), erupting ? 26 : 18, 0.08);
		int anchors = Math.max(10, slamConfig.particleCount / 2);
		for (int i = 0; i < anchors; i++) {
			double angle = Math.PI * 2.0 * i / anchors;
			double radiusFraction = erupting ? Math.sqrt((i + 1.0) / anchors) : (0.35 + 0.65 * ((i + 1.0) / anchors));
			double sampleRadius = Math.min(slamConfig.radius, ringRadius) * radiusFraction;
			Vec3d samplePoint = new Vec3d(
				caster.getX() + Math.cos(angle) * sampleRadius,
				centerAnchor.y + 0.8,
				caster.getZ() + Math.sin(angle) * sampleRadius
			);
			Vec3d anchor = findFrostGroundAnchor(world, samplePoint, 3, 7);
			if (anchor == null) {
				continue;
			}
			spawnFrostSlamEruptionAt(world, anchor, angle, slamConfig.shardVelocity, erupting ? 4 : 3, erupting);
		}
		int fanCount = erupting ? 10 : 7;
		for (int i = 0; i < fanCount; i++) {
			double angle = Math.PI * 2.0 * i / fanCount;
			Vec3d fanAnchor = findFrostGroundAnchor(
				world,
				new Vec3d(
					caster.getX() + Math.cos(angle) * Math.max(0.7, ringRadius * 0.55),
					centerAnchor.y + 0.6,
					caster.getZ() + Math.sin(angle) * Math.max(0.7, ringRadius * 0.55)
				),
				3,
				7
			);
			if (fanAnchor == null) {
				continue;
			}
			spawnFrostShardFan(world, fanAnchor, angle, slamConfig.shardVelocity * 1.2, erupting);
		}
	}

	private static void spawnFrostMaximumParticles(
		ServerWorld world,
		LivingEntity caster,
		FrostMaximumState state,
		int currentTick
	) {
		if (FROST_CONFIG.maximum.whiteParticleCount > 0) {
			world.spawnParticles(
				ParticleTypes.SNOWFLAKE,
				caster.getX(),
				caster.getBodyY(0.5),
				caster.getZ(),
				FROST_CONFIG.maximum.whiteParticleCount,
				0.5,
				0.85,
				0.5,
				0.03
			);
			world.spawnParticles(
				ParticleTypes.WHITE_ASH,
				caster.getX(),
				caster.getBodyY(0.5),
				caster.getZ(),
				FROST_CONFIG.maximum.whiteParticleCount,
				0.4,
				0.8,
				0.4,
				0.02
			);
		}
		if (!FROST_CONFIG.maximum.ascentVortexEnabled) {
			return;
		}

		double elapsedTicks = Math.max(0.0, currentTick - (state.windupEndTick - Math.max(1, FROST_CONFIG.maximum.windupDurationTicks)));
		double progress = FROST_CONFIG.maximum.windupDurationTicks <= 0
			? 1.0
			: MathHelper.clamp(elapsedTicks / Math.max(1.0, FROST_CONFIG.maximum.windupDurationTicks), 0.0, 1.0);
		double vortexHeight = Math.max(0.8, Math.min(FROST_CONFIG.maximum.vortexHeightBlocks, (caster.getY() - state.baseY) + 1.2));
		int ringCount = Math.max(1, FROST_CONFIG.maximum.vortexRingCount);
		double spinBase = currentTick * 0.24;
		for (int ringIndex = 0; ringIndex < ringCount; ringIndex++) {
			double heightFraction = ringCount == 1 ? 0.0 : ringIndex / (double) (ringCount - 1);
			double localY = vortexHeight * heightFraction;
			double radius = MathHelper.lerp(
				heightFraction,
				FROST_CONFIG.maximum.vortexBaseRadius * (0.35 + progress * 0.65),
				FROST_CONFIG.maximum.vortexTopRadius * Math.max(0.2, progress)
			);
			int points = 6 + ringIndex * 2;
			for (int pointIndex = 0; pointIndex < points; pointIndex++) {
				double angle = spinBase * (1.0 + heightFraction * 0.8) + Math.PI * 2.0 * pointIndex / points + ringIndex * 0.42;
				double x = caster.getX() + Math.cos(angle) * radius;
				double y = state.baseY + 0.1 + localY;
				double z = caster.getZ() + Math.sin(angle) * radius;
				double swirlVelocity = 0.04 + 0.02 * heightFraction;
				double velocityX = -Math.sin(angle) * swirlVelocity;
				double velocityY = 0.04 + 0.03 * progress;
				double velocityZ = Math.cos(angle) * swirlVelocity;
				world.spawnParticles(ParticleTypes.WHITE_ASH, x, y, z, 0, velocityX * 0.55, velocityY * 0.7, velocityZ * 0.55, 1.0);
				if ((pointIndex + ringIndex) % 2 == 0) {
					world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 0, velocityX * 0.8, velocityY, velocityZ * 0.8, 1.0);
				}
				if (heightFraction > 0.2) {
					world.spawnParticles(FROST_SHARD_PARTICLE, x, y, z, 0, velocityX * 0.4, velocityY * 1.1, velocityZ * 0.4, 1.0);
				}
			}
		}
		Vec3d groundAnchor = findFrostGroundAnchor(world, new Vec3d(caster.getX(), state.baseY + 1.0, caster.getZ()), 2, 5);
		if (groundAnchor != null) {
			spawnFrostGroundHaze(world, groundAnchor, Math.max(1.0, FROST_CONFIG.maximum.vortexBaseRadius + progress), FROST_CONFIG.maximum.whiteParticleCount / 2);
		}
	}

	private static void syncFrostStageHud(ServerPlayerEntity player) {
		if (player == null || MagicPlayerData.getSchool(player) != MagicSchool.FROST) {
			if (player != null) {
				MagicPlayerData.clearFrostStageHud(player);
			}
			return;
		}

		FrostStageState state = FROST_STAGE_STATES.get(player.getUuid());
		if (state == null || activeAbility(player) != MagicAbility.BELOW_FREEZING || state.dimension != player.getEntityWorld().getRegistryKey()) {
			MagicPlayerData.clearFrostStageHud(player);
			return;
		}

		int requiredTicks = frostProgressRequirementTicks(state.currentStage);
		int progressTicks = requiredTicks > 0 ? Math.min(state.progressTicks, requiredTicks) : 0;
		MagicPlayerData.setFrostStageHud(
			player,
			true,
			state.currentStage,
			state.highestUnlockedStage,
			progressTicks,
			requiredTicks
		);
	}

	private static FrostStageState ensureAdminFrostStageState(ServerPlayerEntity player) {
		if (player == null || MagicPlayerData.getSchool(player) != MagicSchool.FROST) {
			return null;
		}

		UUID playerId = player.getUuid();
		clearFrostMaximumState(player, false);
		FROST_MANA_REGEN_BLOCKED_END_TICK.remove(playerId);
		BELOW_FREEZING_COOLDOWN_END_TICK.remove(playerId);
		FROST_ASCENT_COOLDOWN_END_TICK.remove(playerId);
		PLANCK_HEAT_COOLDOWN_END_TICK.remove(playerId);
		FrostStageState state = FROST_STAGE_STATES.get(playerId);
		if (state == null) {
			state = new FrostStageState(player.getEntityWorld().getRegistryKey(), 1, 1, 0);
			FROST_STAGE_STATES.put(playerId, state);
		}
		state.dimension = player.getEntityWorld().getRegistryKey();
		state.currentStage = MathHelper.clamp(state.currentStage, 1, 3);
		state.highestUnlockedStage = MathHelper.clamp(Math.max(state.currentStage, state.highestUnlockedStage), 1, 3);
		state.progressTicks = Math.max(0, state.progressTicks);
		removeFrostStageCasterBuffs(player);
		setActiveAbility(player, MagicAbility.BELOW_FREEZING);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		return state;
	}

	private static Vec3d findFrostGroundAnchor(ServerWorld world, Vec3d samplePoint, int searchUpBlocks, int searchDownBlocks) {
		BlockPos basePos = BlockPos.ofFloored(samplePoint.x, samplePoint.y, samplePoint.z);
		for (int offset = searchUpBlocks; offset >= -searchDownBlocks; offset--) {
			BlockPos floorPos = basePos.up(offset);
			VoxelShape floorShape = world.getBlockState(floorPos).getCollisionShape(world, floorPos);
			if (floorShape.isEmpty()) {
				continue;
			}
			BlockPos abovePos = floorPos.up();
			if (!world.getBlockState(abovePos).getCollisionShape(world, abovePos).isEmpty()) {
				continue;
			}
			double surfaceY = floorPos.getY() + floorShape.getMax(Direction.Axis.Y);
			return new Vec3d(samplePoint.x, surfaceY, samplePoint.z);
		}
		return null;
	}

	private static void spawnFrostGroundSpikeEruption(
		ServerWorld world,
		Vec3d groundAnchor,
		Vec3d direction,
		double width,
		boolean overcast,
		int stage
	) {
		Vec3d forward = new Vec3d(direction.x, 0.0, direction.z);
		if (forward.lengthSquared() <= 1.0E-6) {
			forward = new Vec3d(1.0, 0.0, 0.0);
		} else {
			forward = forward.normalize();
		}
		Vec3d side = new Vec3d(-forward.z, 0.0, forward.x);
		double halfWidth = Math.max(0.16, width * 0.45);
		double lateralSpacing = Math.max(0.08, FROST_CONFIG.rangedAttack.particleSpacing * 0.5);
		int columns = overcast ? 5 : stage >= 3 ? 4 : 3;
		world.spawnParticles(
			new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
			groundAnchor.x,
			groundAnchor.y + 0.03,
			groundAnchor.z,
			overcast ? 8 : 5,
			halfWidth * 0.4,
			0.03,
			halfWidth * 0.4,
			0.04
		);
		spawnFrostGroundShockRing(world, groundAnchor, Math.max(0.35, halfWidth), 6, 0.03);
		for (int column = 0; column < columns; column++) {
			double offsetFraction = columns == 1 ? 0.0 : column / (double) (columns - 1) * 2.0 - 1.0;
			Vec3d start = groundAnchor.add(side.multiply(offsetFraction * Math.min(halfWidth * 0.55, lateralSpacing * Math.max(1, columns - 1))));
			double lift = (overcast ? 0.42 : 0.24) + stage * 0.03 + Math.abs(offsetFraction) * 0.02;
			world.spawnParticles(FROST_SHARD_PARTICLE, start.x, start.y + 0.04, start.z, 0, forward.x * 0.04, lift, forward.z * 0.04, 1.0);
			world.spawnParticles(ParticleTypes.SNOWFLAKE, start.x, start.y + 0.03, start.z, 0, forward.x * 0.025, lift * 0.85, forward.z * 0.025, 1.0);
			world.spawnParticles(ParticleTypes.WHITE_ASH, start.x, start.y + 0.02, start.z, 0, forward.x * 0.015, lift * 0.55, forward.z * 0.015, 1.0);
		}
	}

	private static void spawnFrostSlamEruptionAt(
		ServerWorld world,
		Vec3d anchor,
		double angle,
		double shardVelocity,
		int shardBursts,
		boolean erupting
	) {
		Vec3d outward = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
		Vec3d side = new Vec3d(-outward.z, 0.0, outward.x);
		world.spawnParticles(
			new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
			anchor.x,
			anchor.y + 0.03,
			anchor.z,
			erupting ? 6 : 4,
			0.12,
			0.03,
			0.12,
			0.03
		);
		for (int burst = 0; burst < shardBursts; burst++) {
			double sideOffset = shardBursts == 1 ? 0.0 : burst / (double) (shardBursts - 1) * 2.0 - 1.0;
			Vec3d start = anchor.add(side.multiply(sideOffset * 0.14));
			double velocityX = outward.x * (0.05 + shardVelocity * 0.35) + side.x * sideOffset * 0.05;
			double velocityY = shardVelocity + (erupting ? 0.18 : 0.12) + burst * 0.02;
			double velocityZ = outward.z * (0.05 + shardVelocity * 0.35) + side.z * sideOffset * 0.05;
			world.spawnParticles(FROST_SHARD_PARTICLE, start.x, start.y + 0.04, start.z, 0, velocityX, velocityY, velocityZ, 1.0);
			world.spawnParticles(ParticleTypes.SNOWFLAKE, start.x, start.y + 0.03, start.z, 0, velocityX * 0.72, velocityY * 0.85, velocityZ * 0.72, 1.0);
			world.spawnParticles(ParticleTypes.WHITE_ASH, start.x, start.y + 0.02, start.z, 0, velocityX * 0.45, velocityY * 0.55, velocityZ * 0.45, 1.0);
		}
	}

	private static void spawnFrostShardFan(ServerWorld world, Vec3d anchor, double angle, double speed, boolean erupting) {
		for (int index = -1; index <= 1; index++) {
			double spreadAngle = angle + index * 0.28;
			double velocityX = Math.cos(spreadAngle) * speed * 0.42;
			double velocityY = speed + (erupting ? 0.14 : 0.08) + Math.abs(index) * 0.02;
			double velocityZ = Math.sin(spreadAngle) * speed * 0.42;
			world.spawnParticles(FROST_SHARD_PARTICLE, anchor.x, anchor.y + 0.04, anchor.z, 0, velocityX, velocityY, velocityZ, 1.0);
			world.spawnParticles(ParticleTypes.SNOWFLAKE, anchor.x, anchor.y + 0.03, anchor.z, 0, velocityX * 0.7, velocityY * 0.8, velocityZ * 0.7, 1.0);
		}
	}

	private static void spawnFrostGroundShockRing(ServerWorld world, Vec3d center, double radius, int points, double liftVelocity) {
		if (radius <= 0.0 || points <= 0) {
			return;
		}
		for (int pointIndex = 0; pointIndex < points; pointIndex++) {
			double angle = Math.PI * 2.0 * pointIndex / points;
			double x = center.x + Math.cos(angle) * radius;
			double z = center.z + Math.sin(angle) * radius;
			double velocityX = Math.cos(angle) * 0.08;
			double velocityZ = Math.sin(angle) * 0.08;
			world.spawnParticles(ParticleTypes.WHITE_ASH, x, center.y + 0.03, z, 0, velocityX, liftVelocity, velocityZ, 1.0);
			if (pointIndex % 2 == 0) {
				world.spawnParticles(ParticleTypes.SNOWFLAKE, x, center.y + 0.03, z, 0, velocityX * 0.9, liftVelocity * 0.8, velocityZ * 0.9, 1.0);
			}
		}
	}

	private static void spawnFrostGroundHaze(ServerWorld world, Vec3d center, double radius, int density) {
		if (density <= 0) {
			return;
		}
		world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + 0.05, center.z, density, radius * 0.35, 0.06, radius * 0.35, 0.01);
		world.spawnParticles(ParticleTypes.WHITE_ASH, center.x, center.y + 0.04, center.z, density, radius * 0.38, 0.08, radius * 0.38, 0.008);
	}

	private static void spawnFrostMaximumBurstParticles(ServerWorld world, LivingEntity caster, double burstRadius) {
		double centerX = caster.getX();
		double centerY = caster.getBodyY(0.5);
		double centerZ = caster.getZ();
		if (FROST_CONFIG.maximum.burstHazeDensity > 0) {
			world.spawnParticles(
				ParticleTypes.WHITE_ASH,
				centerX,
				centerY,
				centerZ,
				FROST_CONFIG.maximum.burstHazeDensity,
				burstRadius * 0.22,
				1.25,
				burstRadius * 0.22,
				0.12
			);
			world.spawnParticles(
				ParticleTypes.CLOUD,
				centerX,
				centerY,
				centerZ,
				Math.max(12, FROST_CONFIG.maximum.burstHazeDensity / 2),
				burstRadius * 0.18,
				0.75,
				burstRadius * 0.18,
				0.08
			);
		}
		if (FROST_CONFIG.maximum.burstParticleCount > 0) {
			world.spawnParticles(
				ParticleTypes.SNOWFLAKE,
				centerX,
				centerY,
				centerZ,
				FROST_CONFIG.maximum.burstParticleCount,
				burstRadius * 0.24,
				1.15,
				burstRadius * 0.24,
				0.14
			);
			world.spawnParticles(
				new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
				centerX,
				centerY,
				centerZ,
				Math.max(24, FROST_CONFIG.maximum.burstParticleCount / 5),
				burstRadius * 0.12,
				0.8,
				burstRadius * 0.12,
				0.18
			);
		}
		world.spawnParticles(ParticleTypes.GLOW, centerX, centerY, centerZ, 24, 0.2, 0.2, 0.2, 0.02);
		int spokes = Math.max(18, FROST_CONFIG.maximum.burstParticleCount / 4);
		for (int spokeIndex = 0; spokeIndex < spokes; spokeIndex++) {
			double angle = Math.PI * 2.0 * spokeIndex / spokes;
			double startRadius = 0.45 + (spokeIndex % 4) * 0.08;
			double startX = centerX + Math.cos(angle) * startRadius;
			double startZ = centerZ + Math.sin(angle) * startRadius;
			double shardSpeed = FROST_CONFIG.maximum.burstShardSpeed * (0.8 + (spokeIndex % 3) * 0.15);
			double velocityX = Math.cos(angle) * shardSpeed;
			double velocityY = 0.12 + (spokeIndex % 4) * 0.03;
			double velocityZ = Math.sin(angle) * shardSpeed;
			world.spawnParticles(FROST_SHARD_PARTICLE, startX, centerY, startZ, 0, velocityX, velocityY, velocityZ, 1.0);
			world.spawnParticles(ParticleTypes.SNOWFLAKE, startX, centerY, startZ, 0, velocityX * 0.75, velocityY * 0.8, velocityZ * 0.75, 1.0);
			world.spawnParticles(ParticleTypes.WHITE_ASH, startX, centerY, startZ, 0, velocityX * 0.55, velocityY * 0.55, velocityZ * 0.55, 1.0);
		}
		Vec3d groundAnchor = findFrostGroundAnchor(world, new Vec3d(centerX, caster.getY() + 1.0, centerZ), 3, 7);
		if (groundAnchor != null) {
			double ringRadius = Math.min(burstRadius, FROST_CONFIG.maximum.groundShockRingRadius);
			spawnFrostGroundHaze(world, groundAnchor, Math.max(1.0, ringRadius), Math.max(14, FROST_CONFIG.maximum.burstHazeDensity / 3));
			spawnFrostGroundShockRing(world, groundAnchor, Math.max(1.0, ringRadius), 40, 0.05);
			world.spawnParticles(
				new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
				groundAnchor.x,
				groundAnchor.y + 0.03,
				groundAnchor.z,
				30,
				ringRadius * 0.45,
				0.05,
				ringRadius * 0.45,
				0.06
			);
		}
	}

	private static void spawnFrostPackedIceEncasement(ServerWorld world, LivingEntity target, FrostPackedIceState state) {
		if (world == null) {
			return;
		}
		state.encasementDisplayIds.clear();
		double centerX = state.lockedX;
		double centerY = state.lockedY + state.shellHeight * 0.5;
		double centerZ = state.lockedZ;
		double sideRadius = state.shellRadius;
		double lowerY = -state.shellHeight * 0.28;
		double middleY = 0.0;
		double upperY = state.shellHeight * 0.28;
		double topY = state.shellHeight * 0.56;
		Vec3d[] offsets = {
			new Vec3d(0.0, lowerY, 0.0),
			new Vec3d(0.0, topY, 0.0),
			new Vec3d(0.0, middleY, sideRadius),
			new Vec3d(0.0, middleY, -sideRadius),
			new Vec3d(sideRadius, middleY, 0.0),
			new Vec3d(-sideRadius, middleY, 0.0),
			new Vec3d(sideRadius * 0.72, upperY, sideRadius * 0.72),
			new Vec3d(-sideRadius * 0.72, upperY, sideRadius * 0.72),
			new Vec3d(sideRadius * 0.72, upperY, -sideRadius * 0.72),
			new Vec3d(-sideRadius * 0.72, upperY, -sideRadius * 0.72),
			new Vec3d(sideRadius * 0.72, lowerY * 0.35, sideRadius * 0.72),
			new Vec3d(-sideRadius * 0.72, lowerY * 0.35, sideRadius * 0.72),
			new Vec3d(sideRadius * 0.72, lowerY * 0.35, -sideRadius * 0.72),
			new Vec3d(-sideRadius * 0.72, lowerY * 0.35, -sideRadius * 0.72)
		};
		float displayScale = (float) MathHelper.clamp(Math.max(0.92, Math.min(1.18, Math.max(target.getWidth(), 0.9))), 0.92, 1.18);
		for (Vec3d offset : offsets) {
			DisplayEntity.BlockDisplayEntity display = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
			display.refreshPositionAndAngles(centerX + offset.x, centerY + offset.y, centerZ + offset.z, 0.0F, 0.0F);
			((BlockDisplayEntityAccessorMixin) display).magic$setBlockState(Blocks.PACKED_ICE.getDefaultState());
			((DisplayEntityAccessorMixin) display).magic$setTeleportDuration(1);
			((DisplayEntityAccessorMixin) display).magic$setInterpolationDuration(1);
			((DisplayEntityAccessorMixin) display).magic$setDisplayWidth(displayScale);
			((DisplayEntityAccessorMixin) display).magic$setDisplayHeight(displayScale);
			((DisplayEntityAccessorMixin) display).magic$setViewRange(2.5F);
			display.setNoGravity(true);
			display.setSilent(true);
			display.setInvulnerable(true);
			if (world.spawnEntity(display)) {
				state.encasementDisplayIds.add(display.getUuid());
			}
		}
	}

	private static void clearFrostPackedIceEncasement(MinecraftServer server, FrostPackedIceState state, boolean spawnBreakEffects) {
		if (server == null || state == null) {
			return;
		}
		ServerWorld world = server.getWorld(state.dimension);
		if (world != null) {
			clearFrostPackedIceEncasement(world, state, spawnBreakEffects);
		}
	}

	private static void clearFrostPackedIceEncasement(ServerWorld world, FrostPackedIceState state, boolean spawnBreakEffects) {
		if (world == null || state == null) {
			return;
		}
		boolean spawnedBreak = false;
		for (UUID displayId : state.encasementDisplayIds) {
			Entity displayEntity = world.getEntity(displayId);
			if (displayEntity == null) {
				continue;
			}
			if (spawnBreakEffects) {
				world.spawnParticles(
					new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
					displayEntity.getX(),
					displayEntity.getY(),
					displayEntity.getZ(),
					10,
					0.12,
					0.12,
					0.12,
					0.06
				);
				spawnedBreak = true;
			}
			displayEntity.discard();
		}
		if (spawnBreakEffects && !spawnedBreak) {
			world.spawnParticles(
				new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
				state.lockedX,
				state.lockedY + state.shellHeight * 0.5,
				state.lockedZ,
				24,
				state.shellRadius * 0.8,
				state.shellHeight * 0.4,
				state.shellRadius * 0.8,
				0.08
			);
		}
		state.encasementDisplayIds.clear();
	}

	private static boolean isFrostEnemy(ServerPlayerEntity caster, LivingEntity target) {
		return target != caster && !caster.isTeammate(target);
	}

	private static void removeStatusEffectIfMatching(
		LivingEntity entity,
		RegistryEntry<StatusEffect> effect,
		Set<Integer> allowedAmplifiers
	) {
		StatusEffectInstance existing = entity.getStatusEffect(effect);
		if (existing != null && allowedAmplifiers.contains(existing.getAmplifier())) {
			entity.removeStatusEffect(effect);
		}
	}

	private static Set<Integer> frostPassiveAmplifiers(boolean slowness) {
		Set<Integer> amplifiers = new HashSet<>();
		if (slowness) {
			if (FROST_CONFIG.stageTwo.slownessAmplifier >= 0) {
				amplifiers.add(FROST_CONFIG.stageTwo.slownessAmplifier);
			}
			if (FROST_CONFIG.stageThree.slownessAmplifier >= 0) {
				amplifiers.add(FROST_CONFIG.stageThree.slownessAmplifier);
			}
			return amplifiers;
		}
		if (FROST_CONFIG.stageOne.resistanceAmplifier >= 0) {
			amplifiers.add(FROST_CONFIG.stageOne.resistanceAmplifier);
		}
		if (FROST_CONFIG.stageTwo.resistanceAmplifier >= 0) {
			amplifiers.add(FROST_CONFIG.stageTwo.resistanceAmplifier);
		}
		if (FROST_CONFIG.stageThree.resistanceAmplifier >= 0) {
			amplifiers.add(FROST_CONFIG.stageThree.resistanceAmplifier);
		}
		return amplifiers;
	}

	private static boolean sendFrostAbilityBlockedMessage(ServerPlayerEntity player) {
		if (player == null) {
			return false;
		}
		if (FROST_MAXIMUM_FEAR_TARGETS.containsKey(player.getUuid())) {
			if (!FROST_CONFIG.maximum.windupMagicBlockedMessage.isBlank()) {
				player.sendMessage(Text.literal(FROST_CONFIG.maximum.windupMagicBlockedMessage), true);
			}
			return true;
		}
		if (FROST_HELPLESS_TARGETS.containsKey(player.getUuid())) {
			if (!FROST_CONFIG.maximum.postBurstMagicBlockedMessage.isBlank()) {
				player.sendMessage(Text.literal(FROST_CONFIG.maximum.postBurstMagicBlockedMessage), true);
			}
			return true;
		}
		return false;
	}

	private static boolean isFrostMaximumInvulnerable(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return false;
		}
		FrostMaximumState state = FROST_MAXIMUM_STATES.get(player.getUuid());
		if (state == null || state.phase != FrostMaximumPhase.WINDUP || state.dimension != player.getEntityWorld().getRegistryKey()) {
			return false;
		}
		return !source.isOf(DamageTypes.OUT_OF_WORLD) && !source.isOf(DamageTypes.GENERIC_KILL) && !source.isOf(DamageTypes.OUTSIDE_BORDER);
	}

	private static boolean isFrostHelplessAttacker(DamageSource source) {
		Entity attacker = source.getAttacker();
		if (attacker instanceof LivingEntity attackerLiving && isFrostHelpless(attackerLiving)) {
			return true;
		}
		return source.getSource() instanceof LivingEntity sourceLiving && isFrostHelpless(sourceLiving);
	}

	private static boolean isFrostTeleportBlocked(Entity entity) {
		if (!(entity instanceof LivingEntity living)) {
			return false;
		}
		return isFrostFrozen(living) || isFrostMaximumFearLocked(living) || isFrostPackedIceTarget(living) || isFrostHelpless(living);
	}

	private static boolean isFrostTeleportItemBlocked(PlayerEntity player, ItemStack stack) {
		if (player == null || stack == null || !isFrostTeleportBlocked(player)) {
			return false;
		}
		return stack.isOf(Items.ENDER_PEARL) || stack.isOf(Items.CHORUS_FRUIT) || stack.isOf(Items.WIND_CHARGE);
	}

	private static boolean isFrostFrozen(LivingEntity entity) {
		FrostFreezeState state = FROST_FROZEN_TARGETS.get(entity.getUuid());
		return state != null && state.dimension == entity.getEntityWorld().getRegistryKey();
	}

	private static boolean isFrostMaximumFearLocked(Entity entity) {
		FrostFearState state = FROST_MAXIMUM_FEAR_TARGETS.get(entity.getUuid());
		return state != null && state.dimension == entity.getEntityWorld().getRegistryKey();
	}

	private static boolean isFrostPackedIceTarget(LivingEntity entity) {
		FrostPackedIceState state = FROST_PACKED_ICE_TARGETS.get(entity.getUuid());
		return state != null && state.dimension == entity.getEntityWorld().getRegistryKey();
	}

	private static boolean isFrostHelpless(Entity entity) {
		FrostHelplessState state = FROST_HELPLESS_TARGETS.get(entity.getUuid());
		return state != null && state.dimension == entity.getEntityWorld().getRegistryKey();
	}

	private static boolean frostManaRegenBlocked(ServerPlayerEntity player, int currentTick) {
		int endTick = FROST_MANA_REGEN_BLOCKED_END_TICK.getOrDefault(player.getUuid(), 0);
		if (currentTick < endTick) {
			return true;
		}
		FROST_MANA_REGEN_BLOCKED_END_TICK.remove(player.getUuid());
		return false;
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

		if (!isOrionsGambitManaCostSuppressed(player) && MagicPlayerData.getMana(player) <= 0) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		setActiveAbility(player, MagicAbility.LOVE_AT_FIRST_SIGHT);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		LOVE_POWER_ACTIVE_THIS_SECOND.put(player.getUuid(), false);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.LOVE_AT_FIRST_SIGHT.displayName()), true);
		recordOrionsGambitAbilityUse(player, MagicAbility.LOVE_AT_FIRST_SIGHT);
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
		recordOrionsGambitAbilityUse(player, MagicAbility.TILL_DEATH_DO_US_PART);
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

		if (!canSpendAbilityCost(player, MANIPULATION_ACTIVATION_MANA_COST)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			debugManipulation("{} empty embrace denied: activation cost {}", debugName(player), MANIPULATION_ACTIVATION_MANA_COST);
			return;
		}

		spendAbilityCost(player, MANIPULATION_ACTIVATION_MANA_COST);
		setActiveAbility(player, MagicAbility.MANIPULATION);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		MANIPULATION_STATES.put(player.getUuid(), ManipulationState.waiting(currentTick, new Vec3d(player.getX(), player.getY(), player.getZ())));
		player.sendMessage(Text.translatable("message.magic.empty_embrace.waiting"), true);
		tryAcquireManipulationTarget(player, currentTick);
		recordOrionsGambitAbilityUse(player, MagicAbility.MANIPULATION);
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

	static void cancelMagicOnTarget(ServerPlayerEntity target, int currentTick) {
		cancelOwnedNonDomainMagicOnTarget(target, currentTick);
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

	static void cancelOwnedNonDomainMagicOnTarget(ServerPlayerEntity target, int currentTick) {
		MagicAbility targetAbility = activeAbility(target);
		boolean targetOwnsDomain = DOMAIN_EXPANSIONS.containsKey(target.getUuid()) || isDomainExpansion(targetAbility);
		if (targetAbility == MagicAbility.BELOW_FREEZING) {
			endFrostStagedMode(target, currentTick, FrostStageEndReason.INVALID, true, false);
		}
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
		if (targetAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			deactivateHerculesBurden(target, true, false);
		}
		if (targetAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			clearSagittariusWindup(target);
		}
		if (targetAbility == MagicAbility.ORIONS_GAMBIT) {
			endOrionsGambit(target, OrionGambitEndReason.MANUAL_CANCEL, currentTick, false);
		}
		clearComedicAssistantState(target.getUuid());
		GreedRuntime.cancelActiveAbilities(target, currentTick);

		if (!targetOwnsDomain) {
			setActiveAbility(target, MagicAbility.NONE);
		}
	}

	static void cancelOwnedMagicOnTargetForBankruptcy(
		ServerPlayerEntity target,
		int currentTick,
		boolean preserveMaximumAbilities,
		boolean preserveDomainAbilities
	) {
		UUID targetId = target.getUuid();
		MagicAbility targetAbility = activeAbility(target);
		boolean protectedActiveAbility = isBankruptcyProtectedAbility(targetAbility, preserveMaximumAbilities, preserveDomainAbilities);

		if (!isBankruptcyProtectedAbility(MagicAbility.MARTYRS_FLAME, preserveMaximumAbilities, preserveDomainAbilities)) {
			MARTYRS_FLAME_PASSIVE_ENABLED.remove(targetId);
			MARTYRS_FLAME_DRAIN_BUFFER.remove(targetId);
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.SPOTLIGHT, preserveMaximumAbilities, preserveDomainAbilities)) {
			deactivateSpotlight(target, true);
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.CASSIOPEIA, preserveMaximumAbilities, preserveDomainAbilities)) {
			deactivateCassiopeia(target, true);
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.COMEDIC_REWRITE, preserveMaximumAbilities, preserveDomainAbilities)) {
			deactivateComedicRewrite(target);
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.COMEDIC_ASSISTANT, preserveMaximumAbilities, preserveDomainAbilities)) {
			clearComedicAssistantState(targetId);
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.PLUS_ULTRA, preserveMaximumAbilities, preserveDomainAbilities) && targetAbility == MagicAbility.PLUS_ULTRA) {
			endPlusUltra(target, currentTick, PlusUltraEndMode.FULL, false);
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.TILL_DEATH_DO_US_PART, preserveMaximumAbilities, preserveDomainAbilities)) {
			if (targetAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
				deactivateTillDeathDoUsPart(target, TillDeathDoUsPartEndReason.MANUAL_CANCEL, true);
			} else {
				TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.remove(targetId);
			}
		}

		if (!protectedActiveAbility) {
			if (targetAbility == MagicAbility.BELOW_FREEZING) {
				endFrostStagedMode(target, currentTick, FrostStageEndReason.LOCKED, true, false);
			}
			if (targetAbility == MagicAbility.ABSOLUTE_ZERO) {
				deactivateAbsoluteZero(target);
			}
			if (targetAbility == MagicAbility.PLANCK_HEAT) {
				deactivatePlanckHeat(target);
			}
			if (targetAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
				deactivateLoveAtFirstSight(target);
			}
			if (targetAbility == MagicAbility.MANIPULATION) {
				deactivateManipulation(target, true, "suppressed by bankruptcy");
			}
			if (targetAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
				deactivateHerculesBurden(target, true, false);
			}
			if (targetAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
				clearSagittariusWindup(target);
			}
			if (targetAbility == MagicAbility.ORIONS_GAMBIT) {
				endOrionsGambit(target, OrionGambitEndReason.MANUAL_CANCEL, currentTick, false);
			}
			if (isDomainExpansion(targetAbility) && !preserveDomainAbilities) {
				deactivateDomainExpansion(target);
			}
		}

		GreedRuntime.cancelActiveAbilities(target, currentTick);

		if (!protectedActiveAbility && targetAbility != MagicAbility.NONE) {
			setActiveAbility(target, MagicAbility.NONE);
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
		if (!canSpendAbilityCost(player, DOMAIN_EXPANSION_ACTIVATION_MANA_COST)) {
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
				spendAbilityCost(player, DOMAIN_EXPANSION_ACTIVATION_MANA_COST);
				setActiveAbility(player, requestedAbility);
				recordOrionsGambitAbilityUse(player, requestedAbility);
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
		spendAbilityCost(player, DOMAIN_EXPANSION_ACTIVATION_MANA_COST);
		setActiveAbility(player, requestedAbility);
		recordOrionsGambitAbilityUse(player, requestedAbility);
		if (requestedAbility == MagicAbility.ASTRAL_CATACLYSM) {
			ASTRAL_CATACLYSM_DOMAIN_STATES.put(player.getUuid(), new ConstellationDomainState(ConstellationDomainPhase.CHARGING, currentTick + ASTRAL_CATACLYSM_CHARGE_DURATION_TICKS));
			player.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.charging"), true);
			return;
		}
		player.sendMessage(Text.translatable("message.magic.ability.activated", requestedAbility.displayName()), true);
	}

	private static void prepareForDomainActivation(ServerPlayerEntity player, MagicAbility activeAbility) {
		if (activeAbility == MagicAbility.BELOW_FREEZING) {
			endFrostStagedMode(player, player.getEntityWorld().getServer().getTicks(), FrostStageEndReason.MANUAL, false, false);
		}
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
		if (activeAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			deactivateHerculesBurden(player, true, false);
		}
		if (activeAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			clearSagittariusWindup(player);
		}
		if (activeAbility == MagicAbility.ORIONS_GAMBIT) {
			endOrionsGambit(player, OrionGambitEndReason.MANUAL_CANCEL, player.getEntityWorld().getServer().getTicks(), false);
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

	private static boolean isPositionInsideAnyDomain(RegistryKey<World> dimension, double x, double y, double z) {
		for (DomainExpansionState state : DOMAIN_EXPANSIONS.values()) {
			if (!state.dimension.equals(dimension)) {
				continue;
			}

			double dx = x - state.centerX;
			double dz = z - state.centerZ;
			double horizontalDistanceSq = dx * dx + dz * dz;
			double relativeY = y - state.baseY;
			if (isInsideDomainInterior(horizontalDistanceSq, relativeY, state.innerRadius, state.innerHeight)) {
				return true;
			}
		}

		return false;
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

		int radius = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? Math.max(1, FROST_CONFIG.domain.radius)
			: Math.max(1, DOMAIN_EXPANSION_RADIUS);
		int height = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? Math.max(1, FROST_CONFIG.domain.height)
			: Math.max(1, DOMAIN_EXPANSION_HEIGHT);
		int shellThickness = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? Math.max(1, FROST_CONFIG.domain.shellThickness)
			: Math.max(1, DOMAIN_EXPANSION_SHELL_THICKNESS);
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
						savedBlocks.put(immutablePos, new DomainSavedBlockState(currentState, blockEntityNbt));

						if (shouldPreserveBeaconAnchor(currentState)) {
							protectedShellStates.put(immutablePos, currentState);
							continue;
						}

						if (hasProtectedDecorationEntity(world, immutablePos)) {
							if (shell || targetState.isOf(Blocks.LIGHT)) {
								protectedShellStates.put(immutablePos, currentState);
							}
							continue;
						}

						if (currentState.equals(targetState)) {
							continue;
						}

						if (!setDomainBlockState(world, immutablePos, targetState, DOMAIN_BLOCK_PLACE_FLAGS)) {
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

		int expiresTick = domainExpiresTick(ability, currentTick);
		int effectEndTick = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? currentTick + Math.max(0, FROST_CONFIG.domain.startupTicks) + Math.max(1, FROST_CONFIG.domain.totalDurationTicks)
			: expiresTick;
		int nextFrostPulseTick = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? currentTick + Math.max(0, FROST_CONFIG.domain.startupTicks) + Math.max(1, FROST_CONFIG.domain.pulseIntervalTicks)
			: Integer.MAX_VALUE;

		DomainExpansionState state = new DomainExpansionState(
			world.getRegistryKey(),
			ability,
			expiresTick,
			effectEndTick,
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
			0,
			nextFrostPulseTick,
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
		boolean clashActive = DOMAIN_CLASHES_BY_OWNER.containsKey(player.getUuid());
		ASTRAL_CATACLYSM_DOMAIN_STATES.remove(player.getUuid());
		endOwnedDomain(player.getUuid(), state, server, server.getTicks(), !clashActive);
		persistDomainRuntimeState(server);
		applyDomainInstabilityPenalty(player);
		return true;
	}

	private static void endOwnedDomain(
		UUID ownerId,
		DomainExpansionState state,
		MinecraftServer server,
		int currentTick,
		boolean applySchoolCooldowns
	) {
		cancelDomainClash(ownerId, server);
		restoreDomainExpansion(server, state);
		applyDomainEndCooldowns(ownerId, state.ability, currentTick, state.cooldownMultiplier, applySchoolCooldowns);
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

			if (!setDomainBlockState(world, pos, saved.blockState, DOMAIN_BLOCK_RESTORE_FLAGS)) {
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
				if (!(clashActive && DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS) && state.ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
					changed |= updateFrostDomain(world, ownerId, state, currentTick);
				}
				if (!(clashActive && DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS)) {
					enforceCapturedEntitiesInsideDomain(world, state);
				}
				maintainDomainShell(world, state);
				continue;
			}

			if (state.ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
				executeFrostDomainFinale(world, ownerId, state);
			}
			cancelDomainClash(ownerId, server);
			restoreDomainExpansion(server, state);
			applyDomainEndCooldowns(ownerId, state.ability, currentTick, state.cooldownMultiplier, true);
			ASTRAL_CATACLYSM_DOMAIN_STATES.remove(ownerId);
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

	private static boolean updateFrostDomain(ServerWorld world, UUID ownerId, DomainExpansionState state, int currentTick) {
		if (currentTick > state.effectEndTick || state.nextFrostPulseTick == Integer.MAX_VALUE) {
			return false;
		}

		boolean changed = false;
		while (currentTick >= state.nextFrostPulseTick && state.nextFrostPulseTick <= state.effectEndTick) {
			state.frostPulseCount++;
			applyFrostDomainPulse(world, ownerId, state, currentTick);
			state.nextFrostPulseTick += Math.max(1, FROST_CONFIG.domain.pulseIntervalTicks);
			changed = true;
		}
		return changed;
	}

	private static void applyFrostDomainPulse(ServerWorld world, UUID ownerId, DomainExpansionState state, int currentTick) {
		int pulseDurationTicks = Math.max(0, FROST_CONFIG.domain.pulseDurationTicks);
		int blindnessDurationTicks = Math.max(0, FROST_CONFIG.domain.blindnessPulseDurationTicks);
		int freezeDurationTicks = Math.max(0, FROST_CONFIG.domain.freezePulseDurationTicks);
		int pulseAmplifier = MathHelper.clamp(
			FROST_CONFIG.domain.slownessBaseAmplifier + Math.max(0, state.frostPulseCount - 1) * FROST_CONFIG.domain.slownessAmplifierPerPulse,
			-1,
			255
		);

		for (LivingEntity target : collectLivingEntitiesInsideDomain(world, state, ownerId)) {
			if (FROST_CONFIG.domain.applySlowness && pulseDurationTicks > 0 && pulseAmplifier >= 0) {
				refreshStatusEffect(target, StatusEffects.SLOWNESS, pulseDurationTicks, pulseAmplifier, true, true, true);
			}
			if (FROST_CONFIG.domain.applyFrost && pulseDurationTicks > 0 && FROST_CONFIG.domain.frostDamagePerTick > 0.0F) {
				applyOrRefreshFrostbite(
					target,
					ownerId,
					currentTick,
					pulseDurationTicks,
					FROST_CONFIG.domain.frostDamagePerTick,
					FROST_CONFIG.debuff.refreshDurationOnReapply,
					FROST_CONFIG.debuff.stackDamageOnReapply
				);
			}
			if (FROST_CONFIG.domain.applyBlindness && blindnessDurationTicks > 0) {
				refreshStatusEffect(target, StatusEffects.BLINDNESS, blindnessDurationTicks, 0, true, true, true);
			}
			if (FROST_CONFIG.domain.applyFreeze && freezeDurationTicks > 0) {
				applyFrostFreeze(target, ownerId, currentTick + freezeDurationTicks);
			}
			world.spawnParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.3, 0.6, 0.3, 0.03);
			world.spawnParticles(ParticleTypes.WHITE_ASH, target.getX(), target.getBodyY(0.5), target.getZ(), 6, 0.25, 0.5, 0.25, 0.02);
		}
	}

	private static void executeFrostDomainFinale(ServerWorld world, UUID ownerId, DomainExpansionState state) {
		if (!FROST_CONFIG.domain.finalExecutionEnabled) {
			return;
		}

		for (LivingEntity target : collectLivingEntitiesInsideDomain(world, state, ownerId)) {
			world.spawnParticles(
				new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
				target.getX(),
				target.getBodyY(0.5),
				target.getZ(),
				FROST_CONFIG.domain.executionParticleCount,
				0.45,
				0.9,
				0.45,
				0.06
			);
			world.spawnParticles(ParticleTypes.WHITE_ASH, target.getX(), target.getBodyY(0.5), target.getZ(), 24, 0.35, 0.8, 0.35, 0.03);
			world.playSound(
				null,
				target.getX(),
				target.getY(),
				target.getZ(),
				SoundEvents.BLOCK_GLASS_BREAK,
				SoundCategory.PLAYERS,
				FROST_CONFIG.domain.executionSoundVolume,
				FROST_CONFIG.domain.executionSoundPitch
			);
			if (FROST_CONFIG.domain.finalExecutionBypassTotems) {
				target.kill(world);
			} else {
				dealTrackedMagicDamage(target, ownerId, world.getDamageSources().freeze(), Float.MAX_VALUE);
			}
		}
	}

	private static List<LivingEntity> collectLivingEntitiesInsideDomain(ServerWorld world, DomainExpansionState state, UUID ownerId) {
		Box searchBox = new Box(
			state.centerX - state.innerRadius,
			state.baseY + 1.0,
			state.centerZ - state.innerRadius,
			state.centerX + state.innerRadius + 1.0,
			state.baseY + state.innerHeight + 1.0,
			state.centerZ + state.innerRadius + 1.0
		);
		List<LivingEntity> targets = new ArrayList<>();
		for (LivingEntity entity : world.getEntitiesByClass(
			LivingEntity.class,
			searchBox,
			living -> living.isAlive() && (!(living instanceof PlayerEntity player) || !player.isSpectator())
		)) {
			if (ownerId.equals(entity.getUuid()) || !isLivingEntityInsideDomainInterior(entity, state)) {
				continue;
			}
			targets.add(entity);
		}
		return targets;
	}

	private static boolean isLivingEntityInsideDomainInterior(LivingEntity entity, DomainExpansionState state) {
		double dx = entity.getX() - state.centerX;
		double dz = entity.getZ() - state.centerZ;
		double horizontalDistanceSq = dx * dx + dz * dz;
		double relativeY = entity.getY() - state.baseY;
		return isInsideDomainInterior(horizontalDistanceSq, relativeY, state.innerRadius, state.innerHeight);
	}

	private static void resetDomainTimingForAbility(DomainExpansionState state, MagicAbility ability, int currentTick) {
		state.ability = ability;
		state.expiresTick = domainExpiresTick(ability, currentTick);
		state.effectEndTick = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? currentTick + Math.max(0, FROST_CONFIG.domain.startupTicks) + Math.max(1, FROST_CONFIG.domain.totalDurationTicks)
			: state.expiresTick;
		state.frostPulseCount = 0;
		state.nextFrostPulseTick = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? currentTick + Math.max(0, FROST_CONFIG.domain.startupTicks) + Math.max(1, FROST_CONFIG.domain.pulseIntervalTicks)
			: Integer.MAX_VALUE;
		state.activationTick = currentTick;
	}

	private static void syncDomainTimerOverlays(MinecraftServer server, int currentTick) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			DomainExpansionState domain = DOMAIN_EXPANSIONS.get(player.getUuid());
			if (domain == null || domain.ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
				MagicPlayerData.clearDomainTimer(player);
				continue;
			}

			int remainingTicks = Math.max(0, domain.expiresTick - currentTick);
			int remainingSeconds = Math.max(0, MathHelper.ceil(remainingTicks / (float) TICKS_PER_SECOND));
			int secondarySeconds = 0;
			if (domain.ability == MagicAbility.ASTRAL_CATACLYSM) {
				ConstellationDomainState state = ASTRAL_CATACLYSM_DOMAIN_STATES.get(player.getUuid());
				if (state != null && state.phase == ConstellationDomainPhase.CHARGING) {
					int chargeTicksRemaining = Math.max(0, state.chargeCompleteTick - currentTick);
					secondarySeconds = Math.max(0, MathHelper.ceil(chargeTicksRemaining / (float) TICKS_PER_SECOND));
				}
			}

			if (remainingSeconds <= 0 && secondarySeconds <= 0) {
				MagicPlayerData.clearDomainTimer(player);
				continue;
			}

			MagicPlayerData.setDomainTimer(player, domain.ability.id(), remainingSeconds, secondarySeconds);
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

		spendAbilityCost(challenger, DOMAIN_EXPANSION_ACTIVATION_MANA_COST);
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
		int clashSafetyTick = currentTick + domainClashIntroLockTicks() + TICKS_PER_SECOND;
		state.expiresTick = Math.max(state.expiresTick, clashSafetyTick);
		state.effectEndTick = Math.max(state.effectEndTick, clashSafetyTick);
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
			if (DOMAIN_CLASH_PAUSE_TIMED_DOMAIN_COLLAPSE_TIMERS && domain.expiresTick != Integer.MAX_VALUE) {
				domain.expiresTick++;
				domain.effectEndTick++;
				if (DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS && domain.ability == MagicAbility.FROST_DOMAIN_EXPANSION && domain.nextFrostPulseTick != Integer.MAX_VALUE) {
					domain.nextFrostPulseTick++;
				}
			}
			if (DOMAIN_CLASH_PAUSE_ASTRAL_CATACLYSM_PHASE_TIMERS && domain.ability == MagicAbility.ASTRAL_CATACLYSM) {
				pauseAstralCataclysmDomainStateForClash(ownerId);
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

	private static void pauseAstralCataclysmDomainStateForClash(UUID ownerId) {
		ConstellationDomainState state = ASTRAL_CATACLYSM_DOMAIN_STATES.get(ownerId);
		if (state == null) {
			return;
		}

		if (state.phase == ConstellationDomainPhase.CHARGING) {
			state.chargeCompleteTick++;
			return;
		}

		if (state.phase == ConstellationDomainPhase.ACQUIRING) {
			state.acquireEndTick++;
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
			resetDomainTimingForAbility(domain, clash.challengerAbility, currentTick);
			domain.cooldownMultiplier = DOMAIN_CLASH_POST_CLASH_COOLDOWN_MULTIPLIER;
			DOMAIN_EXPANSIONS.remove(ownerId);
			DOMAIN_EXPANSIONS.put(clash.challengerId, domain);
			ASTRAL_CATACLYSM_DOMAIN_STATES.remove(ownerId);
			if (clash.challengerAbility == MagicAbility.ASTRAL_CATACLYSM) {
				ASTRAL_CATACLYSM_DOMAIN_STATES.put(
					clash.challengerId,
					new ConstellationDomainState(ConstellationDomainPhase.CHARGING, currentTick + ASTRAL_CATACLYSM_CHARGE_DURATION_TICKS)
				);
			} else {
				ASTRAL_CATACLYSM_DOMAIN_STATES.remove(clash.challengerId);
			}
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

	private static void captureDomainClashDamage(ServerPlayerEntity damagedPlayer, DamageSource source, float amount) {
		DomainClashState clash = domainClashStateForParticipant(damagedPlayer.getUuid());
		if (clash == null) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(damagedPlayer.getUuid());
			return;
		}

		MinecraftServer server = damagedPlayer.getEntityWorld().getServer();
		if (server == null || !isDomainClashCombatActive(clash, server.getTicks())) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(damagedPlayer.getUuid());
			return;
		}

		UUID attackerId = resolveDomainClashAttackerId(clash, damagedPlayer.getUuid(), source, server);
		if (attackerId == null) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(damagedPlayer.getUuid());
			return;
		}

		DOMAIN_CLASH_PENDING_DAMAGE.put(
			damagedPlayer.getUuid(),
			new DomainClashPendingDamageState(attackerId, damagedPlayer.getHealth(), amount)
		);
	}

	private static void onPlayerAfterDamage(ServerPlayerEntity damagedPlayer, DamageSource source, float damageTaken) {
		GreedRuntime.onPlayerAfterDamage(damagedPlayer, source, damageTaken);
		DomainClashPendingDamageState pendingDamage = DOMAIN_CLASH_PENDING_DAMAGE.remove(damagedPlayer.getUuid());
		if (pendingDamage == null) {
			return;
		}

		DomainClashState clash = domainClashStateForParticipant(damagedPlayer.getUuid());
		if (clash == null) {
			return;
		}

		MinecraftServer server = damagedPlayer.getEntityWorld().getServer();
		if (server == null || !isDomainClashCombatActive(clash, server.getTicks())) {
			return;
		}

		UUID attackerId = resolveDomainClashAttackerId(clash, damagedPlayer.getUuid(), source, server);
		if (attackerId == null || !attackerId.equals(pendingDamage.attackerId)) {
			return;
		}

		float clashDamage = Math.max(0.0F, pendingDamage.healthBefore - damagedPlayer.getHealth());
		if (clashDamage <= 0.0F) {
			clashDamage = Math.max(0.0F, damageTaken);
		}
		if (clashDamage <= 0.0F) {
			clashDamage = Math.max(0.0F, pendingDamage.incomingAmount);
		}
		if (clashDamage <= 0.0F) {
			return;
		}

		addDomainClashDamage(clash, attackerId, clashDamage, server, server.getTicks());
	}

	private static void onLivingEntityAfterDamage(LivingEntity entity, DamageSource source, float damageTaken) {
		if (entity == null || source == null || damageTaken <= 0.0F || entity.getEntityWorld().isClient()) {
			return;
		}

		ServerPlayerEntity attacker = directMeleePlayerAttackerFrom(source);
		if (attacker == null || attacker == entity || !attacker.isAlive() || attacker.isSpectator()) {
			return;
		}

		if (!isValidComedicAssistantTarget(entity)) {
			return;
		}

		tryTriggerComedicAssistant(attacker, entity, damageTaken);
		tryTriggerPlusUltraHit(attacker, entity, damageTaken);
	}

	private static ServerPlayerEntity directMeleePlayerAttackerFrom(DamageSource source) {
		Entity attacker = source.getAttacker();
		Entity directSource = source.getSource();
		if (attacker instanceof ServerPlayerEntity attackerPlayer && directSource == attackerPlayer && attackerPlayer.isAlive() && !attackerPlayer.isSpectator()) {
			return attackerPlayer;
		}
		if (directSource instanceof ServerPlayerEntity sourcePlayer && (attacker == null || attacker == sourcePlayer) && sourcePlayer.isAlive() && !sourcePlayer.isSpectator()) {
			return sourcePlayer;
		}
		return null;
	}

	private static UUID resolveDomainClashAttackerId(
		DomainClashState clash,
		UUID damagedId,
		DamageSource source,
		MinecraftServer server
	) {
		Entity attacker = source.getAttacker();
		UUID attackerId = null;
		if (attacker instanceof ServerPlayerEntity attackerPlayer && attackerPlayer.isAlive() && !attackerPlayer.isSpectator()) {
			attackerId = attackerPlayer.getUuid();
		}

		if (attackerId == null && source.getSource() instanceof ServerPlayerEntity sourcePlayer && sourcePlayer.isAlive() && !sourcePlayer.isSpectator()) {
			attackerId = sourcePlayer.getUuid();
		}

		if (attackerId == null) {
			UUID pendingAttackerId = MAGIC_DAMAGE_PENDING_ATTACKER.get(damagedId);
			if (pendingAttackerId != null) {
				ServerPlayerEntity pendingAttacker = server.getPlayerManager().getPlayer(pendingAttackerId);
				if (pendingAttacker != null && pendingAttacker.isAlive() && !pendingAttacker.isSpectator()) {
					attackerId = pendingAttackerId;
				}
			}
		}

		if (attackerId == null) {
			return null;
		}

		boolean ownerHitChallenger = attackerId.equals(clash.ownerId) && damagedId.equals(clash.challengerId);
		boolean challengerHitOwner = attackerId.equals(clash.challengerId) && damagedId.equals(clash.ownerId);
		return ownerHitChallenger || challengerHitOwner ? attackerId : null;
	}

	private static void lockDomainClashParticipant(ServerPlayerEntity player, Vec3d lockedPos, Vec3d opponentEyePos) {
		float yaw = player.getYaw();
		float pitch = player.getPitch();
		if (DOMAIN_CLASH_FORCE_LOOK) {
			float[] facing = computeFacingAngles(player, opponentEyePos);
			yaw = facing[0];
			pitch = facing[1];
		}

		teleportDomainEntity(player, lockedPos.x, lockedPos.y, lockedPos.z, yaw, pitch);
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
		player.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("title.magic.domain_clash").formatted(Formatting.BOLD, Formatting.RED)));
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

			if (hasProtectedDecorationEntity(world, pos)) {
				continue;
			}
			setDomainBlockState(world, pos, targetState, DOMAIN_BLOCK_PLACE_FLAGS);
		}
	}

	private static void applyDomainVisualForAbility(ServerWorld world, DomainExpansionState state, MagicAbility ability) {
		int centerX = MathHelper.floor(state.centerX);
		int centerZ = MathHelper.floor(state.centerZ);
		Map<BlockPos, BlockState> refreshedShell = new HashMap<>();
		for (Map.Entry<BlockPos, DomainSavedBlockState> savedEntry : state.savedBlocks.entrySet()) {
			BlockPos pos = savedEntry.getKey();
			int relativeY = pos.getY() - state.baseY;
			if (relativeY < 0 || relativeY > state.height) {
				continue;
			}

			int horizontalDistanceSq = horizontalDistanceSq(pos.getX(), centerX, pos.getZ(), centerZ);
			if (relativeY > 0 && !isInsideDomainDome(horizontalDistanceSq, relativeY, state.radius, state.height)) {
				continue;
			}

			if (shouldPreserveBeaconAnchor(savedEntry.getValue().blockState)) {
				refreshedShell.put(pos, savedEntry.getValue().blockState);
				setDomainBlockState(world, pos, savedEntry.getValue().blockState, DOMAIN_BLOCK_PLACE_FLAGS);
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
			if (hasProtectedDecorationEntity(world, pos)) {
				if (shell || targetState.isOf(Blocks.LIGHT)) {
					refreshedShell.put(pos, savedEntry.getValue().blockState);
				}
				continue;
			}
			if (shell || targetState.isOf(Blocks.LIGHT)) {
				refreshedShell.put(pos, targetState);
			}
			setDomainBlockState(world, pos, targetState, DOMAIN_BLOCK_PLACE_FLAGS);
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
				setDomainBlockState(world, pos, expectedState, DOMAIN_BLOCK_PLACE_FLAGS);
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

		runWithDomainTeleportBypass(() -> {
			boolean positionChanged = entity.squaredDistanceTo(x, y, z) > DOMAIN_TELEPORT_POSITION_EPSILON_SQUARED;
			boolean rotationChanged = angleDeltaDegrees(entity.getYaw(), yaw) > DOMAIN_TELEPORT_ROTATION_EPSILON_DEGREES
				|| angleDeltaDegrees(entity.getPitch(), pitch) > DOMAIN_TELEPORT_ROTATION_EPSILON_DEGREES;
			if (positionChanged) {
				entity.requestTeleport(x, y, z);
			}
			entity.setVelocity(0.0, 0.0, 0.0);
			entity.setOnGround(true);
			entity.setYaw(yaw);
			entity.setPitch(pitch);
			entity.setHeadYaw(yaw);
			entity.setBodyYaw(yaw);

			if (entity instanceof ServerPlayerEntity player && (positionChanged || rotationChanged)) {
				player.networkHandler.requestTeleport(x, y, z, yaw, pitch);
			}
		});
	}

	private static float angleDeltaDegrees(float current, float target) {
		return Math.abs(MathHelper.wrapDegrees(target - current));
	}

	private static void runWithDomainTeleportBypass(Runnable action) {
		int depth = DOMAIN_TELEPORT_BYPASS_DEPTH.get();
		DOMAIN_TELEPORT_BYPASS_DEPTH.set(depth + 1);
		try {
			action.run();
		} finally {
			if (depth <= 0) {
				DOMAIN_TELEPORT_BYPASS_DEPTH.remove();
			} else {
				DOMAIN_TELEPORT_BYPASS_DEPTH.set(depth);
			}
		}
	}

	private static boolean isDomainTeleportBypassActive() {
		return DOMAIN_TELEPORT_BYPASS_DEPTH.get() > 0;
	}

	public static boolean shouldBlockPlayerTeleport(ServerPlayerEntity player, ServerWorld targetWorld, double x, double y, double z) {
		if (
			player == null
			|| targetWorld == null
			|| isDomainTeleportBypassActive()
			|| !DOMAIN_CONTROL_BLOCK_TELEPORT_ACROSS_BOUNDARIES
		) {
			return false;
		}
		if (isFrostTeleportBlocked(player)) {
			player.sendMessage(Text.translatable("message.magic.frost.teleport_blocked"), true);
			return true;
		}

		if (isEntityCapturedByDomain(player)) {
			player.sendMessage(Text.translatable("message.magic.domain.teleport_blocked"), true);
			return true;
		}

		if (isPositionInsideAnyDomain(targetWorld.getRegistryKey(), x, y, z)) {
			player.sendMessage(Text.translatable("message.magic.domain.teleport_blocked"), true);
			return true;
		}

		return false;
	}

	private static boolean shouldPreserveBeaconAnchor(BlockState state) {
		return ASTRAL_CATACLYSM_PRESERVE_BEACON_ANCHOR && isBeaconAnchorBlock(state);
	}

	private static boolean isBeaconAnchorBlock(BlockState state) {
		return state != null && Registries.BLOCK.getId(state.getBlock()).equals(ASTRAL_CATACLYSM_BEACON_ANCHOR_BLOCK_ID);
	}

	private static boolean isHoldingBeaconCore(PlayerEntity player) {
		return player != null && (
			Registries.ITEM.getId(player.getMainHandStack().getItem()).equals(ASTRAL_CATACLYSM_BEACON_CORE_ITEM_ID) ||
			Registries.ITEM.getId(player.getOffHandStack().getItem()).equals(ASTRAL_CATACLYSM_BEACON_CORE_ITEM_ID)
		);
	}

	private static boolean isBeaconCoreProtected(ServerPlayerEntity player) {
		if (
			player == null
			|| !isHoldingBeaconCore(player)
			|| !(player.getEntityWorld() instanceof ServerWorld world)
			|| ASTRAL_CATACLYSM_BEACON_CORE_PROTECTION_RADIUS <= 0.0
		) {
			return false;
		}

		BeaconCoreAnchorState anchor = currentBeaconCoreAnchor(world.getServer());
		if (anchor == null || !anchor.dimension.equals(world.getRegistryKey())) {
			return false;
		}

		ServerWorld anchorWorld = world.getServer().getWorld(anchor.dimension);
		if (anchorWorld == null || !isBeaconAnchorBlock(anchorWorld.getBlockState(anchor.pos))) {
			return false;
		}

		double protectionRadiusSquared = ASTRAL_CATACLYSM_BEACON_CORE_PROTECTION_RADIUS * ASTRAL_CATACLYSM_BEACON_CORE_PROTECTION_RADIUS;
		return player.squaredDistanceTo(anchor.pos.toCenterPos()) <= protectionRadiusSquared;
	}

	private static BeaconCoreAnchorState currentBeaconCoreAnchor(MinecraftServer server) {
		if (server == null) {
			return null;
		}

		int currentTick = server.getTicks();
		if (
			cachedBeaconCoreAnchorTick != Integer.MIN_VALUE &&
			currentTick - cachedBeaconCoreAnchorTick < BEACON_CORE_ANCHOR_CACHE_REFRESH_TICKS
		) {
			return cachedBeaconCoreAnchor;
		}

		ServerWorld overworld = server.getOverworld();
		if (overworld == null) {
			cachedBeaconCoreAnchor = null;
			cachedBeaconCoreAnchorTick = currentTick;
			return null;
		}

		BeaconCoreAnchorPersistentState state = overworld.getPersistentStateManager().getOrCreate(
			new PersistentStateType<>(
				ASTRAL_CATACLYSM_BEACON_CORE_ANCHOR_STATE_ID,
				BeaconCoreAnchorPersistentState::new,
				BeaconCoreAnchorPersistentState.CODEC,
				DataFixTypes.SAVED_DATA_COMMAND_STORAGE
			)
		);
		cachedBeaconCoreAnchor = state == null ? null : state.getAnchor();
		cachedBeaconCoreAnchorTick = currentTick;
		return cachedBeaconCoreAnchor;
	}

	private static boolean isDomainCapturable(LivingEntity entity) {
		if (entity instanceof PlayerEntity player) {
			return !player.isSpectator();
		}

		return entity instanceof MobEntity;
	}

	private static void onServerStarted(MinecraftServer server) {
		domainRuntimePersistentState = null;
		cachedBeaconCoreAnchor = null;
		cachedBeaconCoreAnchorTick = Integer.MIN_VALUE;
		DOMAIN_EXPANSIONS.clear();
		DOMAIN_CLASHES_BY_OWNER.clear();
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.clear();
		DOMAIN_CLASH_PENDING_DAMAGE.clear();
		FROST_DOMAIN_COOLDOWN_END_TICK.clear();
		LOVE_DOMAIN_COOLDOWN_END_TICK.clear();
		MAGIC_DAMAGE_PENDING_ATTACKER.clear();
		DOMAIN_PENDING_RETURNS.clear();
		FROST_STAGE_STATES.clear();
		FROST_MAXIMUM_STATES.clear();
		FROST_SPIKE_WAVES.clear();
		FROST_FROZEN_TARGETS.clear();
		FROST_MAXIMUM_FEAR_TARGETS.clear();
		FROST_HELPLESS_TARGETS.clear();
		FROST_PACKED_ICE_TARGETS.clear();
		BELOW_FREEZING_COOLDOWN_END_TICK.clear();
		FROST_ASCENT_COOLDOWN_END_TICK.clear();
		FROST_MANA_REGEN_BLOCKED_END_TICK.clear();
		MARTYRS_FLAME_PASSIVE_ENABLED.clear();
		MARTYRS_FLAME_COOLDOWN_END_TICK.clear();
		MARTYRS_FLAME_DRAIN_BUFFER.clear();
		MARTYRS_FLAME_BURNING_TARGETS.clear();
		TEST_MODE_PLAYERS.clear();
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
			MagicPlayerData.clearDomainTimer(player);
		}

		DOMAIN_CLASHES_BY_OWNER.clear();
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.clear();
		DOMAIN_CLASH_PENDING_DAMAGE.clear();
		MAGIC_DAMAGE_PENDING_ATTACKER.clear();
		persistDomainRuntimeState(server);
		domainRuntimePersistentState = null;
		cachedBeaconCoreAnchor = null;
		cachedBeaconCoreAnchorTick = Integer.MIN_VALUE;
		TEST_MODE_PLAYERS.clear();
		MARTYRS_FLAME_PASSIVE_ENABLED.clear();
		MARTYRS_FLAME_COOLDOWN_END_TICK.clear();
		MARTYRS_FLAME_DRAIN_BUFFER.clear();
		MARTYRS_FLAME_BURNING_TARGETS.clear();
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
			int effectRemainingTicks = state.effectEndTick == Integer.MAX_VALUE ? -1 : Math.max(0, state.effectEndTick - currentTick);
			domainTag.putInt("effectEndRemainingTicks", effectRemainingTicks);
			int nextPulseRemainingTicks = state.nextFrostPulseTick == Integer.MAX_VALUE ? -1 : Math.max(0, state.nextFrostPulseTick - currentTick);
			domainTag.putInt("nextFrostPulseRemainingTicks", nextPulseRemainingTicks);
			domainTag.putInt("frostPulseCount", state.frostPulseCount);
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
			int effectEndRemainingTicks = domainTag.getInt("effectEndRemainingTicks", expiresRemainingTicks);
			int effectEndTick = effectEndRemainingTicks < 0 ? Integer.MAX_VALUE : currentTick + Math.max(0, effectEndRemainingTicks);
			int nextFrostPulseRemainingTicks = domainTag.getInt("nextFrostPulseRemainingTicks", -1);
			int nextFrostPulseTick = nextFrostPulseRemainingTicks < 0 ? Integer.MAX_VALUE : currentTick + Math.max(0, nextFrostPulseRemainingTicks);
			int frostPulseCount = Math.max(0, domainTag.getInt("frostPulseCount", 0));

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
					effectEndTick,
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
					frostPulseCount,
					nextFrostPulseTick,
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
			applyPassiveAbilityEffects(player, currentTick);
			applyActiveAbilityEffects(player, currentTick, absoluteZeroAuraSeenThisTick);
		}

		updateLoveLockedTargets(server, currentTick);
		updateHerculesBurdenStates(server, currentTick);
		updateHerculesBurdenTargets(server, currentTick);
		updateSagittariusWindups(server, currentTick);
		updateOrionsGambitStates(server, currentTick);
		updateOrionsGambitPenalties(server, currentTick);
		updateAstralCataclysmDomainStates(server, currentTick);
		updateAstralExecutionBeams(server, currentTick);
		cleanupManipulationStates(server);
		syncManipulationSuppressionTags(server);
		cleanupPlanckHeatStates(server);
		trimAbsoluteZeroAuraTimers(absoluteZeroAuraSeenThisTick);
		updateFrostSpikeWaves(server, currentTick);
		updateFrostbittenTargets(server, currentTick);
		updateFrostFrozenTargets(server, currentTick);
		updateFrostMaximumFearTargets(server, currentTick);
		updateFrostPackedIceTargets(server, currentTick);
		updateFrostHelplessTargets(server, currentTick);
		updateEnhancedFireTargets(server, currentTick);
		updateMartyrsFlameBurningTargets(server, currentTick);
		updateComedicAssistantCarries(server, currentTick);
		updateDomainClashes(server, currentTick);
		updateDomainExpansions(server, currentTick);
		syncDomainTimerOverlays(server, currentTick);
		updateComedicRewriteVisualCameos(server, currentTick);
		updateComedicAssistantAcmeVisuals(server, currentTick);
		updateComedicAssistantCaneImpactStates(server, currentTick);
		updatePlusUltraImpactStates(server, currentTick);

		if (currentTick % TICKS_PER_SECOND == 0) {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				updateManaOncePerSecond(player);
			}
		}
		GreedRuntime.onEndServerTick(server, currentTick);
		enforceOrionsGambitGreedTargetCoins(server);
	}

	private static void updateComedicRewriteVisualCameos(MinecraftServer server, int currentTick) {
		if (COMEDIC_REWRITE_VISUAL_CAMEOS.isEmpty()) {
			return;
		}

		Iterator<ComedicRewriteVisualCameo> iterator = COMEDIC_REWRITE_VISUAL_CAMEOS.iterator();
		while (iterator.hasNext()) {
			ComedicRewriteVisualCameo cameo = iterator.next();
			syncComedicRewriteVisualCameo(server, cameo);
			if (!cameo.chargeStopped() && cameo.chargeEndTick() > 0 && cameo.chargeEndTick() <= currentTick) {
				stopComedicRewriteVisualCameoCharge(server, cameo);
			}
			if (cameo.endTick() > currentTick) {
				continue;
			}

			EntitiesDestroyS2CPacket destroyPacket = new EntitiesDestroyS2CPacket(cameo.entityIds());
			for (UUID viewerId : cameo.viewerIds()) {
				ServerPlayerEntity viewer = server.getPlayerManager().getPlayer(viewerId);
				if (viewer != null) {
					viewer.networkHandler.sendPacket(destroyPacket);
				}
			}
			iterator.remove();
		}
	}

	private static void syncComedicRewriteVisualCameo(MinecraftServer server, ComedicRewriteVisualCameo cameo) {
		if (cameo.followState() == null && cameo.rotationState() == null) {
			return;
		}

		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(server, cameo.viewerIds());
		if (viewers.isEmpty()) {
			return;
		}

		ComedicRewriteVisualFollowState followState = cameo.followState();
		Entity anchor = null;
		Vec3d anchorTop = null;
		if (followState != null) {
			ServerWorld anchorWorld = server.getWorld(followState.dimension());
			if (anchorWorld != null) {
				anchor = anchorWorld.getEntity(followState.anchorEntityId());
			}
			if (anchor != null && anchor.isAlive()) {
				anchorTop = new Vec3d(anchor.getX(), anchor.getY() + anchor.getHeight(), anchor.getZ());
			} else {
				followState = null;
			}
		}

		Entity[] entities = cameo.entities();
		for (int index = 0; index < entities.length; index++) {
			Entity entity = entities[index];
			boolean changed = false;
			if (followState != null && index < followState.offsets().length) {
				Vec3d offset = followState.offsets()[index];
				Vec3d targetPos = followState.mode() == ComedicRewriteVisualFollowMode.DROP_TO_ENTITY_TOP
					? comedicRewriteDropVisualTargetPosition(entity, anchorTop, offset, followState.verticalSpeed())
					: anchorTop.add(offset);
				entity.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, entity.getYaw(), entity.getPitch());
				entity.setVelocity(Vec3d.ZERO);
				changed = true;
			} else if (cameo.rotationState() != null && !cameo.chargeStopped() && entity.getVelocity().lengthSquared() > 1.0E-5) {
				entity.refreshPositionAndAngles(
					entity.getX() + entity.getVelocity().x,
					entity.getY() + entity.getVelocity().y,
					entity.getZ() + entity.getVelocity().z,
					entity.getYaw(),
					entity.getPitch()
				);
				changed = true;
			}
			if (cameo.rotationState() != null) {
				applyComedicRewriteVisualRotation(entity, cameo.rotationState());
				changed = true;
			}
			if (!changed) {
				continue;
			}
			EntityPositionSyncS2CPacket positionPacket = EntityPositionSyncS2CPacket.create(entity);
			EntityVelocityUpdateS2CPacket velocityPacket = entity.getVelocity().lengthSquared() <= 1.0E-5
				? null
				: new EntityVelocityUpdateS2CPacket(entity.getId(), entity.getVelocity());
			for (ServerPlayerEntity viewer : viewers) {
				viewer.networkHandler.sendPacket(positionPacket);
				if (velocityPacket != null) {
					viewer.networkHandler.sendPacket(velocityPacket);
				}
			}
		}
	}

	private static Vec3d comedicRewriteDropVisualTargetPosition(Entity entity, Vec3d anchorTop, Vec3d offset, double verticalSpeed) {
		double targetY = anchorTop.y + offset.y;
		double nextY = entity.getY();
		if (nextY > targetY) {
			nextY = Math.max(targetY, nextY - Math.max(0.01, verticalSpeed));
		} else {
			nextY = targetY;
		}
		return new Vec3d(anchorTop.x + offset.x, nextY, anchorTop.z + offset.z);
	}

	private static void applyComedicRewriteVisualRotation(Entity entity, ComedicRewriteVisualRotationState rotationState) {
		if (rotationState.alignYawToVelocity()) {
			Vec3d horizontalVelocity = new Vec3d(entity.getVelocity().x, 0.0, entity.getVelocity().z);
			if (horizontalVelocity.lengthSquared() > 1.0E-5) {
				float yaw = horizontalYawFromDirection(horizontalVelocity) + rotationState.yawOffsetDegrees();
				entity.setYaw(yaw);
				entity.setBodyYaw(yaw);
				entity.setHeadYaw(yaw);
			}
		}
		if (rotationState.pitchPerTick() != 0.0F) {
			entity.setPitch(MathHelper.wrapDegrees(entity.getPitch() + rotationState.pitchPerTick()));
		}
	}

	private static void stopComedicRewriteVisualCameoCharge(MinecraftServer server, ComedicRewriteVisualCameo cameo) {
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(server, cameo.viewerIds());
		if (viewers.isEmpty()) {
			cameo.markChargeStopped();
			return;
		}

		for (Entity entity : cameo.entities()) {
			entity.setVelocity(Vec3d.ZERO);
			EntityVelocityUpdateS2CPacket velocityPacket = new EntityVelocityUpdateS2CPacket(entity.getId(), entity.getVelocity());
			for (ServerPlayerEntity viewer : viewers) {
				viewer.networkHandler.sendPacket(velocityPacket);
			}
		}
		cameo.markChargeStopped();
	}

	private static void applyPassiveAbilityEffects(ServerPlayerEntity player, int currentTick) {
		UUID playerId = player.getUuid();

		if (CASSIOPEIA_PASSIVE_ENABLED.contains(playerId)) {
			if (!shouldKeepCassiopeiaEnabled(player)) {
				deactivateCassiopeia(player, true);
			} else if (currentTick % CASSIOPEIA_OUTLINE_REFRESH_TICKS == 0) {
				syncCassiopeiaOutlines(player);
			}
		}

		if (SPOTLIGHT_PASSIVE_ENABLED.contains(playerId)) {
			if (!shouldKeepSpotlightEnabled(player)) {
				deactivateSpotlight(player, true);
			} else {
				applySpotlight(player, currentTick);
			}
		}

		if (COMEDIC_REWRITE_PASSIVE_ENABLED.contains(playerId) && !shouldKeepComedicRewriteEnabled(player)) {
			deactivateComedicRewrite(player);
		}
		updateComedicAssistantState(player, currentTick);

		if (COMEDIC_REWRITE_IMMUNITY_END_TICK.getOrDefault(playerId, Integer.MIN_VALUE) <= currentTick) {
			COMEDIC_REWRITE_IMMUNITY_END_TICK.remove(playerId);
		}
		if (COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.getOrDefault(playerId, Integer.MIN_VALUE) <= currentTick) {
			COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.remove(playerId);
		}
		if (COMEDIC_REWRITE_COOLDOWN_END_TICK.getOrDefault(playerId, Integer.MIN_VALUE) <= currentTick) {
			COMEDIC_REWRITE_COOLDOWN_END_TICK.remove(playerId);
		}

		if (!MARTYRS_FLAME_PASSIVE_ENABLED.contains(playerId)) {
			return;
		}

		if (!shouldKeepMartyrsFlameEnabled(player)) {
			deactivateMartyrsFlame(player, true);
			return;
		}

		if (MARTYRS_FLAME_APPLY_GLOWING_EFFECT) {
			refreshStatusEffect(player, StatusEffects.GLOWING, EFFECT_REFRESH_TICKS, 0, true, false, true);
		}
		if (currentTick % MARTYRS_FLAME_FIRE_PARTICLE_INTERVAL_TICKS == 0) {
			spawnMartyrsFlameCasterParticles(player);
		}
	}

	private static void onPlayerDeath(ServerPlayerEntity player) {
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server == null) {
			return;
		}

		UUID playerId = player.getUuid();
		MARTYRS_FLAME_BURNING_TARGETS.remove(playerId);
		clearDeathAbilityState(player);
		MagicPlayerData.clearDomainTimer(player);
		deactivateCassiopeia(player, true);
		resetSpotlightTracking(player);
		COMEDIC_REWRITE_IMMUNITY_END_TICK.remove(playerId);
		COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.remove(playerId);
		COMEDIC_REWRITE_PENDING_STATES.remove(playerId);
		COMEDIC_ASSISTANT_ARMED_STATES.remove(playerId);
		COMEDIC_ASSISTANT_PARROT_CARRY_STATES.remove(playerId);
		clearComedicAssistantCaneImpactState(playerId);
		PLUS_ULTRA_LAST_OUTLINED_PLAYERS.remove(playerId);
		PLUS_ULTRA_DAMAGE_SCALING_GUARD.remove(playerId);
		DOMAIN_CLASH_PENDING_DAMAGE.remove(playerId);
		MAGIC_DAMAGE_PENDING_ATTACKER.remove(playerId);
		MAGIC_DAMAGE_PENDING_ATTACKER.entrySet().removeIf(entry -> entry.getValue().equals(playerId));

		boolean domainStateChanged = false;
		if (domainClashStateForParticipant(playerId) != null) {
			cancelDomainClashParticipant(playerId, server);
		} else {
			DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.remove(playerId);
			if (ownedDomain != null) {
				endOwnedDomain(playerId, ownedDomain, server, server.getTicks(), true);
				domainStateChanged = true;
			}
		}

		if (clearCapturedDomainState(playerId)) {
			domainStateChanged = true;
		}

		GreedRuntime.clearAllRuntimeState(player);

		if (domainStateChanged) {
			persistDomainRuntimeState(server);
		}

		GreedRuntime.onPlayerDeath(player);
	}

	private static void updateManaOncePerSecond(ServerPlayerEntity player) {
		if (!MagicPlayerData.hasMagic(player)) {
			return;
		}

		if (isTestingMode(player)) {
			UUID playerId = player.getUuid();
			TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
			MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
			ORIONS_GAMBIT_DRAIN_BUFFER.remove(playerId);
			LOVE_POWER_ACTIVE_THIS_SECOND.put(playerId, false);
			MagicPlayerData.setMana(player, MagicPlayerData.MAX_MANA);
			MagicPlayerData.setDepletedRecoveryMode(player, false);
			return;
		}

		MagicAbility activeAbility = activeAbility(player);
		int mana = MagicPlayerData.getMana(player);
		UUID playerId = player.getUuid();
		boolean orionsGambitManaSuppressed = isOrionsGambitManaDrainSuppressed(player);
		if (isDomainExpansion(activeAbility)) {
			DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.get(playerId);
			if (ownedDomain == null || ownedDomain.ability != activeAbility) {
				setActiveAbility(player, MagicAbility.NONE);
				activeAbility = MagicAbility.NONE;
			}
		}

		if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			LOVE_POWER_ACTIVE_THIS_SECOND.put(playerId, false);
			if (!TILL_DEATH_DO_US_PART_STATES.containsKey(playerId)) {
				TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
				setActiveAbility(player, MagicAbility.NONE);
				return;
			}

			if (orionsGambitManaSuppressed) {
				TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
				regenerateMana(player, mana);
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

		boolean martyrsFlameActive = MARTYRS_FLAME_PASSIVE_ENABLED.contains(playerId);
		if (martyrsFlameActive && !shouldKeepMartyrsFlameEnabled(player)) {
			deactivateMartyrsFlame(player, true);
			martyrsFlameActive = false;
		}

		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
		int martyrsFlameDrain = 0;
		if (martyrsFlameActive && !orionsGambitManaSuppressed) {
			double bufferedDrain = MARTYRS_FLAME_DRAIN_BUFFER.getOrDefault(playerId, 0.0)
				+ manaFromPercentExact(MARTYRS_FLAME_DRAIN_PERCENT_PER_SECOND);
			martyrsFlameDrain = Math.max(0, (int) Math.floor(bufferedDrain + 1.0E-7));
			double remainingDrain = Math.max(0.0, bufferedDrain - martyrsFlameDrain);
			if (remainingDrain > 1.0E-7) {
				MARTYRS_FLAME_DRAIN_BUFFER.put(playerId, remainingDrain);
			} else {
				MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
			}
		} else {
			MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
		}

		int activeAbilityDrain = switch (activeAbility) {
				case ABSOLUTE_ZERO -> 0;
				case BELOW_FREEZING -> 0;
				case PLANCK_HEAT -> 0;
				case LOVE_AT_FIRST_SIGHT -> isLovePowerActiveThisSecond(player)
					? LOVE_AT_FIRST_SIGHT_ACTIVE_DRAIN_PER_SECOND
					: LOVE_AT_FIRST_SIGHT_IDLE_DRAIN_PER_SECOND;
				case MANIPULATION -> MANIPULATION_MANA_DRAIN_PER_SECOND;
				case ORIONS_GAMBIT -> 0;
				case FROST_DOMAIN_EXPANSION, LOVE_DOMAIN_EXPANSION -> 0;
				default -> 0;
			};
		LOVE_POWER_ACTIVE_THIS_SECOND.put(playerId, false);

		if (activeAbility == MagicAbility.ORIONS_GAMBIT) {
			OrionGambitState orionsState = ORIONS_GAMBIT_STATES.get(playerId);
			if (orionsState == null) {
				ORIONS_GAMBIT_DRAIN_BUFFER.remove(playerId);
				setActiveAbility(player, MagicAbility.NONE);
				return;
			}

			if (orionsState.stage == OrionGambitStage.WAITING) {
				if (!ORIONS_GAMBIT_DRAIN_MANA_WHILE_WAITING_FOR_TARGET || orionsGambitManaSuppressed) {
					ORIONS_GAMBIT_DRAIN_BUFFER.remove(playerId);
					if (ORIONS_GAMBIT_DISABLE_MANA_REGEN_WHILE_WAITING_FOR_TARGET) {
						return;
					}
					regenerateMana(player, mana);
					return;
				}
			}

			if (!orionsGambitManaSuppressed) {
				double bufferedDrain = ORIONS_GAMBIT_DRAIN_BUFFER.getOrDefault(playerId, 0.0)
					+ manaFromPercentExact(ORIONS_GAMBIT_MANA_DRAIN_PERCENT_PER_SECOND);
				int manaDrain = Math.max(0, (int) Math.floor(bufferedDrain + 1.0E-7));
				double remainingDrain = Math.max(0.0, bufferedDrain - manaDrain);
				if (remainingDrain > 1.0E-7) {
					ORIONS_GAMBIT_DRAIN_BUFFER.put(playerId, remainingDrain);
				} else {
					ORIONS_GAMBIT_DRAIN_BUFFER.remove(playerId);
				}
				int nextMana = Math.max(0, mana - manaDrain);
				MagicPlayerData.setMana(player, nextMana);
				if (nextMana == 0 && manaDrain > 0) {
					ORIONS_GAMBIT_DRAIN_BUFFER.remove(playerId);
					endOrionsGambit(player, OrionGambitEndReason.MANA_DEPLETED, player.getEntityWorld().getServer().getTicks(), false);
					MagicPlayerData.setDepletedRecoveryMode(player, true);
					player.sendMessage(Text.translatable("message.magic.ability.out_of_mana", activeAbility.displayName()), true);
					return;
				}
			}

			if (orionsState.stage == OrionGambitStage.LINKED && !ORIONS_GAMBIT_DISABLE_MANA_REGEN_WHILE_LINKED) {
				regenerateMana(player, MagicPlayerData.getMana(player));
				return;
			}
			return;
		}

		if (orionsGambitManaSuppressed) {
			regenerateMana(player, mana);
			return;
		}

		if (activeAbility != MagicAbility.NONE || martyrsFlameActive) {
			int manaDrain = activeAbilityDrain + martyrsFlameDrain;
			int nextMana = Math.max(0, mana - manaDrain);
			MagicPlayerData.setMana(player, nextMana);

			if (nextMana == 0 && manaDrain > 0) {
				if (activeAbility == MagicAbility.BELOW_FREEZING) {
					endFrostStagedMode(player, player.getEntityWorld().getServer().getTicks(), FrostStageEndReason.FORCED_THRESHOLD, true, false);
				}
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
				if (activeAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
					deactivateHerculesBurden(player, true, false);
				}
				if (activeAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
					clearSagittariusWindup(player);
				}
				if (martyrsFlameActive) {
					deactivateMartyrsFlame(player, true);
				}

				setActiveAbility(player, MagicAbility.NONE);
				MagicPlayerData.setDepletedRecoveryMode(player, true);
				Text depletedAbilityName = activeAbility != MagicAbility.NONE
					? activeAbility.displayName()
					: MagicAbility.MARTYRS_FLAME.displayName();
				player.sendMessage(Text.translatable("message.magic.ability.out_of_mana", depletedAbilityName), true);
			}

			if (activeAbilityPreventsManaRegen(activeAbility) || (martyrsFlameActive && MARTYRS_FLAME_DISABLE_MANA_REGEN_WHILE_ACTIVE) || manaDrain > 0) {
				return;
			}
		}

		regenerateMana(player, mana);
	}

	private static void regenerateMana(ServerPlayerEntity player, int currentMana) {
		if (GreedRuntime.blocksManaRegen(player)) {
			return;
		}
		if (player.getEntityWorld().getServer() != null && frostManaRegenBlocked(player, player.getEntityWorld().getServer().getTicks())) {
			return;
		}

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
			clearDeathAbilityState(player);
			return;
		}

		if (activeAbility.school() != MagicPlayerData.getSchool(player)) {
			if (activeAbility == MagicAbility.BELOW_FREEZING) {
				endFrostStagedMode(player, currentTick, FrostStageEndReason.INVALID, false, false);
			}
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
			if (activeAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
				deactivateHerculesBurden(player, true, false);
			}
			if (activeAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
				clearSagittariusWindup(player);
			}
			if (activeAbility == MagicAbility.ORIONS_GAMBIT) {
				endOrionsGambit(player, OrionGambitEndReason.SCHOOL_CHANGED, currentTick, false);
			}
			if (activeAbility == MagicAbility.PLUS_ULTRA) {
				endPlusUltra(player, currentTick, PlusUltraEndMode.FULL, false);
			}

			setActiveAbility(player, MagicAbility.NONE);
			return;
		}

		if (activeAbility == MagicAbility.BELOW_FREEZING) {
			applyBelowFreezing(player, currentTick);
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
			return;
		}

		if (activeAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			applyHerculesBurden(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			applySagittariusAstralArrow(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.ORIONS_GAMBIT) {
			applyOrionsGambit(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.PLUS_ULTRA) {
			applyPlusUltra(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.ASTRAL_CATACLYSM) {
			applyAstralCataclysm(player, currentTick);
		}
	}

	private static void applyBelowFreezing(ServerPlayerEntity player, int currentTick) {
		applyFrostStagedMode(player, currentTick);
	}

	private static void applyAbsoluteZero(
		ServerPlayerEntity player,
		int currentTick,
		Map<UUID, Set<UUID>> absoluteZeroAuraSeenThisTick
	) {
		applyFrostMaximum(player, currentTick);
	}

	private static void applyPlanckHeat(ServerPlayerEntity player, int currentTick) {
		setActiveAbility(player, MagicAbility.NONE);
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
		refreshStatusEffect(caster, StatusEffects.RESISTANCE, EFFECT_REFRESH_TICKS, ABSOLUTE_ZERO_RESISTANCE_AMPLIFIER, true, false, true);
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
				refreshStatusEffect(target, StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, PLANCK_HEAT_FROST_SLOWNESS_AMPLIFIER, true, true, true);
				spawnHitboxShardParticles(target, 2);

				int nextDamageTick = nextDamageTicksByTarget.getOrDefault(targetId, currentTick + PLANCK_HEAT_FROST_DAMAGE_INTERVAL_TICKS);
				if (currentTick >= nextDamageTick) {
					dealPlanckHeatFrostDamage(caster.getUuid(), target);
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
				applyOrRefreshEnhancedFire(target, caster.getUuid(), currentTick, PLANCK_HEAT_AURA_ENHANCED_FIRE_DURATION_TICKS);
				refreshStatusEffect(target, StatusEffects.HUNGER, EFFECT_REFRESH_TICKS, PLANCK_HEAT_FIRE_PHASE_HUNGER_AMPLIFIER, true, true, true);
				refreshStatusEffect(target, StatusEffects.NAUSEA, EFFECT_REFRESH_TICKS, PLANCK_HEAT_FIRE_PHASE_NAUSEA_AMPLIFIER, true, true, true);
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

	private static boolean onAllowLivingEntityDamage(LivingEntity entity, DamageSource source, float amount) {
		if (amount <= 0.0F) {
			return true;
		}
		if (isFrostMaximumInvulnerable(entity, source)) {
			return false;
		}
		if (isFrostHelplessAttacker(source)) {
			return false;
		}

		int currentTick = entity.getEntityWorld().getServer().getTicks();
		ServerPlayerEntity directAttacker = attackingPlayerFrom(source);
		if (directAttacker != null && isDirectCasterDamageBlocked(directAttacker, entity, currentTick)) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(entity.getUuid());
			return false;
		}

		if (!(entity instanceof ServerPlayerEntity player)) {
			return true;
		}

		UUID playerId = player.getUuid();
		if (hasActiveComedicRewriteImmunity(playerId, currentTick)) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(playerId);
			return false;
		}

		if (source.isIn(DamageTypeTags.IS_FALL) && hasActiveComedicRewriteFallProtection(playerId, currentTick)) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(playerId);
			return false;
		}

		if (
			hasActivePlusUltra(player)
			&& !source.isOf(DamageTypes.OUT_OF_WORLD)
			&& !source.isOf(DamageTypes.GENERIC_KILL)
			&& !source.isOf(DamageTypes.OUTSIDE_BORDER)
		) {
			if (!PLUS_ULTRA_DAMAGE_SCALING_GUARD.add(playerId)) {
				return true;
			}

			try {
				float scaledAmount = (float) (amount * PLUS_ULTRA_INCOMING_DAMAGE_MULTIPLIER);
				if (Math.abs(scaledAmount - amount) <= 1.0E-6F) {
					return true;
				}

				if (!(player.getEntityWorld() instanceof ServerWorld world)) {
					return true;
				}

				player.damage(world, source, scaledAmount);
			} finally {
				PLUS_ULTRA_DAMAGE_SCALING_GUARD.remove(playerId);
			}

			return false;
		}

		if (!hasActiveOrionsGambitPenalty(player)) {
			return true;
		}

		if (!ORIONS_GAMBIT_DAMAGE_SCALING_GUARD.add(playerId)) {
			return true;
		}

		try {
			float scaledAmount = (float) (amount * ORIONS_GAMBIT_CASTER_INCOMING_DAMAGE_MULTIPLIER);
			if (Math.abs(scaledAmount - amount) <= 1.0E-6F) {
				return true;
			}

			if (!(player.getEntityWorld() instanceof ServerWorld world)) {
				return true;
			}

			player.damage(world, source, scaledAmount);
		} finally {
			ORIONS_GAMBIT_DAMAGE_SCALING_GUARD.remove(playerId);
		}

		return false;
	}

	public static boolean onPlayerDamaged(ServerPlayerEntity damagedPlayer, DamageSource source, float amount) {
		if (source == null || amount <= 0.0F || damagedPlayer.getEntityWorld().isClient()) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(damagedPlayer.getUuid());
			return false;
		}

		captureDomainClashDamage(damagedPlayer, source, amount);

		if (isMagicSuppressed(damagedPlayer)) {
			return false;
		}

		if (shouldTriggerMartyrsFlameRetaliation(damagedPlayer)) {
			ServerPlayerEntity martyrsFlameAttacker = attackingPlayerFrom(source);
			if (martyrsFlameAttacker != null && martyrsFlameAttacker != damagedPlayer) {
				retaliateWithMartyrsFlame(damagedPlayer, martyrsFlameAttacker, damagedPlayer.getEntityWorld().getServer().getTicks());
			}
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

		if (!isOrionsGambitManaCostSuppressed(damagedPlayer) && MagicPlayerData.getMana(damagedPlayer) <= 0) {
			return false;
		}

		activateTillDeathDoUsPartLink(damagedPlayer, attackerPlayer, currentTick);
		return false;
	}

	public static boolean onPlayerPreApplyDamage(
		ServerPlayerEntity player,
		ServerWorld world,
		DamageSource source,
		float incomingAmount,
		float finalDamage
	) {
		if (player == null || world == null || source == null || world.isClient() || player.isDead() || finalDamage <= 0.0F) {
			return false;
		}

		UUID playerId = player.getUuid();
		if (
			COMEDIC_REWRITE_PENDING_STATES.containsKey(playerId) ||
			isMagicSuppressed(player) ||
			!COMEDIC_REWRITE_PASSIVE_ENABLED.contains(playerId)
		) {
			return false;
		}

		if (!shouldKeepComedicRewriteEnabled(player)) {
			deactivateComedicRewrite(player);
			return false;
		}

		int currentTick = world.getServer().getTicks();
		if (comedicRewriteCooldownRemaining(player, currentTick) > 0) {
			return false;
		}

		float currentHealth = player.getHealth();
		if (currentHealth <= 0.0F) {
			return false;
		}

		boolean lethal = finalDamage >= currentHealth;
		double healthFraction = finalDamage / Math.max(0.0001F, currentHealth);
		if (
			!lethal &&
			finalDamage < COMEDIC_REWRITE_DANGEROUS_DAMAGE_THRESHOLD &&
			healthFraction < COMEDIC_REWRITE_DANGEROUS_HEALTH_FRACTION_THRESHOLD
		) {
			return false;
		}

		int manaCost = (int) Math.ceil(manaFromPercentExact(COMEDIC_REWRITE_MANA_COST_PERCENT));
		if (!canSpendAbilityCost(player, manaCost)) {
			return false;
		}

		double severity = comedicRewriteSeverity(currentHealth, finalDamage, lethal);
		double procChancePercent = MathHelper.clamp(
			COMEDIC_REWRITE_BASE_PROC_CHANCE_PERCENT
				+ severity * COMEDIC_REWRITE_SEVERITY_PROC_BONUS_PERCENT
				+ (lethal ? COMEDIC_REWRITE_LETHAL_PROC_BONUS_PERCENT : 0.0),
			0.0,
			COMEDIC_REWRITE_MAX_PROC_CHANCE_PERCENT
		);
		if (player.getRandom().nextDouble() * 100.0 >= procChancePercent) {
			return false;
		}

		boolean unsafeScene = isComedicRewriteUnsafeScene(player, world, source);
		Vec3d safePos = findBestComedicRewriteSafePosition(player, world);
		ComedicRewriteOutcome outcome = selectComedicRewriteOutcome(player, unsafeScene, safePos);
		if (outcome == null) {
			return false;
		}

		spendAbilityCost(player, manaCost);
		startComedicRewriteCooldown(playerId, currentTick);
		if (COMEDIC_REWRITE_IMMUNITY_TICKS > 0) {
			COMEDIC_REWRITE_IMMUNITY_END_TICK.put(playerId, currentTick + COMEDIC_REWRITE_IMMUNITY_TICKS);
		}
		if (COMEDIC_REWRITE_FALL_PROTECTION_TICKS > 0) {
			COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.put(playerId, currentTick + COMEDIC_REWRITE_FALL_PROTECTION_TICKS);
		}

		player.setHealth(comedicRewriteSavedHealthPoints(severity));
		player.fallDistance = 0.0F;
		if (COMEDIC_REWRITE_EXTINGUISH_ON_REWRITE) {
			player.extinguish();
		}

		DOMAIN_CLASH_PENDING_DAMAGE.remove(playerId);
		COMEDIC_REWRITE_PENDING_STATES.put(
			playerId,
			new PendingComedicRewriteState(
				outcome,
				severity,
				resolveComedicRewriteLaunchDirection(player, source),
				safePos
			)
		);
		return true;
	}

	public static void onPlayerDamageResolved(ServerPlayerEntity player) {
		PendingComedicRewriteState state = COMEDIC_REWRITE_PENDING_STATES.remove(player.getUuid());
		if (state == null || player.isDead() || !(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		if (state.outcome() == ComedicRewriteOutcome.LAUNCHED_THROUGH_THE_SCENE) {
			applyComedicRewriteLaunchOutcome(player, world, state);
			return;
		}
		if (state.outcome() == ComedicRewriteOutcome.RAVAGER_BIT) {
			applyComedicRewriteRavagerOutcome(player, world, state);
			return;
		}
		applyComedicRewriteParrotOutcome(player, world, state);
	}

	private static void applyComedicRewriteLaunchOutcome(
		ServerPlayerEntity player,
		ServerWorld world,
		PendingComedicRewriteState state
	) {
		double horizontalVelocity = COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.baseHorizontalVelocity()
			+ state.severity() * COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.horizontalVelocityPerSeverity();
		double verticalVelocity = COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.baseVerticalVelocity()
			+ state.severity() * COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.verticalVelocityPerSeverity();
		Vec3d direction = normalizedHorizontalDirection(state.launchDirection(), player);
		player.setVelocity(direction.x * horizontalVelocity, verticalVelocity, direction.z * horizontalVelocity);
		player.fallDistance = 0.0F;
		applyComedicRewriteSlowness(
			player,
			COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.slownessDurationTicks(),
			COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.slownessAmplifier()
		);
		world.spawnParticles(
			ParticleTypes.CLOUD,
			player.getX(),
			player.getBodyY(0.5),
			player.getZ(),
			COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.smokeParticleCount(),
			0.45,
			0.25,
			0.45,
			0.03
		);
		world.spawnParticles(
			ParticleTypes.POOF,
			player.getX(),
			player.getBodyY(0.5),
			player.getZ(),
			COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.poofParticleCount(),
			0.35,
			0.25,
			0.35,
			0.02
		);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.9F, 1.5F);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.7F, 1.6F);
	}

	private static void applyComedicRewriteRavagerOutcome(
		ServerPlayerEntity player,
		ServerWorld world,
		PendingComedicRewriteState state
	) {
		double horizontalVelocity = COMEDIC_REWRITE_RAVAGER_BIT.baseHorizontalVelocity()
			+ state.severity() * COMEDIC_REWRITE_RAVAGER_BIT.horizontalVelocityPerSeverity();
		double verticalVelocity = COMEDIC_REWRITE_RAVAGER_BIT.baseVerticalVelocity()
			+ state.severity() * COMEDIC_REWRITE_RAVAGER_BIT.verticalVelocityPerSeverity();
		Vec3d direction = normalizedHorizontalDirection(state.launchDirection(), player);
		player.setVelocity(direction.x * horizontalVelocity, verticalVelocity, direction.z * horizontalVelocity);
		player.fallDistance = 0.0F;
		applyComedicRewriteSlowness(
			player,
			COMEDIC_REWRITE_RAVAGER_BIT.slownessDurationTicks(),
			COMEDIC_REWRITE_RAVAGER_BIT.slownessAmplifier()
		);
		world.spawnParticles(
			new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DIRT.getDefaultState()),
			player.getX(),
			player.getY(),
			player.getZ(),
			COMEDIC_REWRITE_RAVAGER_BIT.dustParticleCount(),
			0.55,
			0.15,
			0.55,
			0.08
		);
		world.spawnParticles(
			ParticleTypes.CAMPFIRE_COSY_SMOKE,
			player.getX(),
			player.getBodyY(0.5),
			player.getZ(),
			COMEDIC_REWRITE_RAVAGER_BIT.smokeParticleCount(),
			0.4,
			0.25,
			0.4,
			0.01
		);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 1.1F, 0.85F);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.8F, 0.75F);
		spawnComedicRewriteRavagerVisual(player, world, direction, horizontalVelocity);
	}

	private static void applyComedicRewriteParrotOutcome(
		ServerPlayerEntity player,
		ServerWorld world,
		PendingComedicRewriteState state
	) {
		Vec3d safePos = state.safePos();
		if (safePos == null) {
			applyComedicRewriteLaunchOutcome(player, world, state.withOutcome(ComedicRewriteOutcome.LAUNCHED_THROUGH_THE_SCENE));
			return;
		}

		double carryHeight = COMEDIC_REWRITE_PARROT_RESCUE.carryHeight()
			+ state.severity() * COMEDIC_REWRITE_PARROT_RESCUE.carryHeightPerSeverity();
		double liftedY = Math.min(
			safePos.y + carryHeight,
			world.getTopYInclusive() + 1.0 - player.getHeight()
		);
		teleportComedicRewritePlayer(player, world, new Vec3d(safePos.x, liftedY, safePos.z));
		double sideVelocity = COMEDIC_REWRITE_PARROT_RESCUE.sideVelocity()
			+ state.severity() * COMEDIC_REWRITE_PARROT_RESCUE.sideVelocityPerSeverity();
		Vec3d direction = normalizedHorizontalDirection(state.launchDirection(), player);
		player.setVelocity(direction.z * sideVelocity, 0.15, -direction.x * sideVelocity);
		player.fallDistance = 0.0F;
		if (COMEDIC_REWRITE_PARROT_RESCUE.levitationDurationTicks() > 0) {
			player.addStatusEffect(
				new StatusEffectInstance(
					StatusEffects.LEVITATION,
					COMEDIC_REWRITE_PARROT_RESCUE.levitationDurationTicks(),
					0,
					true,
					false,
					true
				)
			);
		}
		if (COMEDIC_REWRITE_PARROT_RESCUE.slowFallingDurationTicks() > 0) {
			player.addStatusEffect(
				new StatusEffectInstance(
					StatusEffects.SLOW_FALLING,
					COMEDIC_REWRITE_PARROT_RESCUE.slowFallingDurationTicks(),
					0,
					true,
					false,
					true
				)
			);
		}
		world.spawnParticles(
			new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.FEATHER)),
			player.getX(),
			player.getBodyY(0.7),
			player.getZ(),
			COMEDIC_REWRITE_PARROT_RESCUE.featherParticleCount(),
			0.45,
			0.35,
			0.45,
			0.03
		);
		world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getBodyY(0.4), player.getZ(), 8, 0.3, 0.25, 0.3, 0.01);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PARROT_AMBIENT, SoundCategory.PLAYERS, 1.0F, 1.2F);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PARROT_FLY, SoundCategory.PLAYERS, 0.9F, 1.0F);
		spawnComedicRewriteParrotVisual(player, world, direction);
	}

	private static void applyComedicRewriteSlowness(ServerPlayerEntity player, int durationTicks, int amplifier) {
		if (durationTicks <= 0 || amplifier < 0) {
			return;
		}

		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, durationTicks, amplifier, true, false, true));
	}

	private static void spawnComedicRewriteRavagerVisual(
		ServerPlayerEntity player,
		ServerWorld world,
		Vec3d direction,
		double playerHorizontalVelocity
	) {
		if (!COMEDIC_REWRITE_RAVAGER_BIT.showVisualCameo() || COMEDIC_REWRITE_RAVAGER_BIT.visualDurationTicks() <= 0) {
			return;
		}

		Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(world, playerPos);
		if (viewers.isEmpty()) {
			return;
		}

		Vec3d spawnDirection = normalizedHorizontalDirection(direction, player);
		Vec3d spawnPos = playerPos
			.add(0.0, COMEDIC_REWRITE_RAVAGER_BIT.visualVerticalOffset(), 0.0)
			.subtract(spawnDirection.multiply(COMEDIC_REWRITE_RAVAGER_BIT.visualSpawnDistance()));
		float yaw = horizontalYawFromDirection(spawnDirection);
		RavagerEntity ravager = new RavagerEntity(EntityType.RAVAGER, world);
		ravager.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, yaw, 0.0F);
		ravager.setBodyYaw(yaw);
		ravager.setHeadYaw(yaw);
		ravager.setOnGround(player.isOnGround());
		ravager.setAiDisabled(true);
		ravager.setSilent(true);
		double chargeVelocity = Math.max(
			COMEDIC_REWRITE_RAVAGER_BIT.visualChargeVelocity(),
			Math.max(0.0, playerHorizontalVelocity) + COMEDIC_REWRITE_RAVAGER_BIT.visualChargeVelocityBuffer()
		);
		ravager.setVelocity(spawnDirection.multiply(chargeVelocity));
		queueComedicRewriteVisualCameo(
			world.getServer(),
			COMEDIC_REWRITE_RAVAGER_BIT.visualDurationTicks(),
			viewers,
			COMEDIC_REWRITE_RAVAGER_BIT.visualChargeDurationTicks(),
			null,
			ravager
		);
	}

	private static void spawnComedicRewriteParrotVisual(ServerPlayerEntity player, ServerWorld world, Vec3d direction) {
		if (!COMEDIC_REWRITE_PARROT_RESCUE.showVisualCameo()
			|| COMEDIC_REWRITE_PARROT_RESCUE.visualDurationTicks() <= 0
			|| COMEDIC_REWRITE_PARROT_RESCUE.visualCount() <= 0) {
			return;
		}

		Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(world, playerPos);
		if (viewers.isEmpty()) {
			return;
		}

		Vec3d carryDirection = normalizedHorizontalDirection(direction, player);
		Vec3d carryVelocity = player.getVelocity().add(0.0, COMEDIC_REWRITE_PARROT_RESCUE.visualLiftVelocity(), 0.0);
		float yaw = horizontalYawFromDirection(carryVelocity.lengthSquared() > 1.0E-5 ? carryVelocity : carryDirection);
		List<Entity> parrots = new ArrayList<>();
		Vec3d[] headOffsets = new Vec3d[COMEDIC_REWRITE_PARROT_RESCUE.visualCount()];
		for (int index = 0; index < COMEDIC_REWRITE_PARROT_RESCUE.visualCount(); index++) {
			double angle = (Math.PI * 2.0 * index) / COMEDIC_REWRITE_PARROT_RESCUE.visualCount();
			double offsetX = Math.cos(angle) * COMEDIC_REWRITE_PARROT_RESCUE.visualRadius();
			double offsetZ = Math.sin(angle) * COMEDIC_REWRITE_PARROT_RESCUE.visualRadius();
			Vec3d headOffset = new Vec3d(
				offsetX,
				COMEDIC_REWRITE_PARROT_RESCUE.visualVerticalOffset() + (index % 2 == 0 ? 0.1 : 0.0),
				offsetZ
			);
			headOffsets[index] = headOffset;
			Vec3d spawnPos = new Vec3d(player.getX(), player.getY() + player.getHeight(), player.getZ()).add(headOffset);
			ParrotEntity parrot = new ParrotEntity(EntityType.PARROT, world);
			parrot.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, yaw, 0.0F);
			parrot.setBodyYaw(yaw);
			parrot.setHeadYaw(yaw);
			parrot.setAiDisabled(true);
			parrot.setSilent(true);
			parrot.setNoGravity(true);
			parrot.setComponent(
				DataComponentTypes.PARROT_VARIANT,
				ParrotEntity.Variant.byIndex(player.getRandom().nextInt(ParrotEntity.Variant.values().length))
			);
			parrot.setVelocity(
				COMEDIC_REWRITE_PARROT_RESCUE.visualFollowPlayerHead()
					? Vec3d.ZERO
					: carryVelocity.add(offsetX * 0.08, 0.0, offsetZ * 0.08)
			);
			parrots.add(parrot);
		}
		queueComedicRewriteVisualCameo(
			world.getServer(),
			COMEDIC_REWRITE_PARROT_RESCUE.visualDurationTicks(),
			viewers,
			0,
			COMEDIC_REWRITE_PARROT_RESCUE.visualFollowPlayerHead()
				? new ComedicRewriteVisualFollowState(
					world.getRegistryKey(),
					player.getUuid(),
					headOffsets,
					ComedicRewriteVisualFollowMode.ENTITY_TOP,
					0.0
				)
				: null,
			parrots.toArray(Entity[]::new)
		);
	}

	private static boolean hasEnabledComedicAssistantOutcomes() {
		return COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.enabled()
			|| COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.enabled()
			|| COMEDIC_ASSISTANT_PARROT_KIDNAPPING.enabled()
			|| COMEDIC_ASSISTANT_DIVINE_OVERREACTION.enabled()
			|| COMEDIC_ASSISTANT_ACME_DROP.enabled()
			|| COMEDIC_ASSISTANT_PIE_TO_THE_FACE.enabled()
			|| COMEDIC_ASSISTANT_GIANT_CANE_YANK.enabled();
	}

	private static boolean isValidComedicAssistantTarget(LivingEntity target) {
		if (target == null || !target.isAlive()) {
			return false;
		}
		if (target instanceof ServerPlayerEntity playerTarget) {
			return COMEDIC_ASSISTANT_ALLOW_PLAYER_TARGETS && !playerTarget.isSpectator();
		}
		return COMEDIC_ASSISTANT_ALLOW_MOB_TARGETS && target instanceof MobEntity;
	}

	private static void tryTriggerComedicAssistant(ServerPlayerEntity attacker, LivingEntity target, float damageTaken) {
		ComedicAssistantArmedState state = COMEDIC_ASSISTANT_ARMED_STATES.get(attacker.getUuid());
		if (state == null || !(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		int currentTick = world.getServer().getTicks();
		if (!shouldKeepComedicAssistantEnabled(attacker) || attacker.getEntityWorld().getRegistryKey() != state.dimension()) {
			clearComedicAssistantState(attacker.getUuid());
			return;
		}
		if (currentTick >= state.expiresTick()) {
			deactivateComedicAssistant(attacker, currentTick, COMEDIC_ASSISTANT_CANCEL_COOLDOWN_TICKS, false, true);
			return;
		}

		ComedicAssistantOutcome outcome = selectComedicAssistantOutcome(attacker);
		if (outcome == null) {
			clearComedicAssistantState(attacker.getUuid());
			attacker.sendMessage(Text.translatable("message.magic.jester.comedic_assistant.no_outcomes"), true);
			return;
		}

		clearComedicAssistantState(attacker.getUuid());
		startComedicAssistantCooldown(attacker.getUuid(), currentTick, COMEDIC_ASSISTANT_PROC_COOLDOWN_TICKS);
		attacker.sendMessage(Text.translatable("message.magic.jester.comedic_assistant.proc", outcome.displayName()), true);
		sendComedicAssistantProcOverlay(attacker, target, outcome);
		applyComedicAssistantOutcome(outcome, attacker, target, world, currentTick, damageTaken);
	}

	private static void tryTriggerPlusUltraHit(ServerPlayerEntity attacker, LivingEntity target, float damageTaken) {
		if (!hasActivePlusUltra(attacker) || !(target.getEntityWorld() instanceof ServerWorld world) || !isValidPlusUltraTarget(target)) {
			return;
		}

		PlusUltraState state = PLUS_ULTRA_STATES.get(attacker.getUuid());
		if (state == null || attacker.getEntityWorld().getRegistryKey() != state.dimension() || !shouldKeepPlusUltraEnabled(attacker)) {
			return;
		}

		Vec3d hitPos = new Vec3d(target.getX(), target.getBodyY(0.45), target.getZ());
		world.spawnParticles(
			ParticleTypes.CLOUD,
			hitPos.x,
			hitPos.y,
			hitPos.z,
			PLUS_ULTRA_SMOKE_PARTICLE_COUNT,
			PLUS_ULTRA_SMOKE_PARTICLE_SPREAD,
			PLUS_ULTRA_SMOKE_PARTICLE_SPREAD * 0.6,
			PLUS_ULTRA_SMOKE_PARTICLE_SPREAD,
			PLUS_ULTRA_SMOKE_PARTICLE_SPEED
		);
		world.spawnParticles(
			ParticleTypes.DUST_PLUME,
			hitPos.x,
			hitPos.y,
			hitPos.z,
			PLUS_ULTRA_HIT_DUST_PARTICLE_COUNT,
			PLUS_ULTRA_HIT_DUST_PARTICLE_SPREAD,
			PLUS_ULTRA_HIT_DUST_PARTICLE_SPREAD * 0.5,
			PLUS_ULTRA_HIT_DUST_PARTICLE_SPREAD,
			PLUS_ULTRA_HIT_DUST_PARTICLE_SPEED
		);
		applyComedicAssistantBonusDamage(attacker, target, world, PLUS_ULTRA_MELEE_BONUS_DAMAGE);
		if (!target.isAlive()) {
			return;
		}

		launchTarget(target, resolvePlusUltraLaunchDirection(attacker, target), PLUS_ULTRA_FLING_HORIZONTAL_STRENGTH, PLUS_ULTRA_FLING_VERTICAL_STRENGTH);
		trackPlusUltraImpact(attacker, target, world);
	}

	private static boolean isValidPlusUltraTarget(LivingEntity target) {
		if (target == null || !target.isAlive()) {
			return false;
		}
		if (target instanceof ServerPlayerEntity) {
			return PLUS_ULTRA_ALLOW_PLAYER_TARGETS;
		}
		return PLUS_ULTRA_ALLOW_MOB_TARGETS && target instanceof MobEntity;
	}

	private static Vec3d resolvePlusUltraLaunchDirection(ServerPlayerEntity attacker, LivingEntity target) {
		Vec3d facing = attacker.getRotationVector();
		Vec3d horizontalFacing = new Vec3d(facing.x, 0.0, facing.z);
		if (horizontalFacing.lengthSquared() > 1.0E-5) {
			return horizontalFacing.normalize();
		}
		return directionFromAttackerToTarget(attacker, target);
	}

	private static void trackPlusUltraImpact(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		MinecraftServer server = world.getServer();
		if (server == null || PLUS_ULTRA_IMPACT_TRACKING_TICKS <= 0 || PLUS_ULTRA_IMPACT_DAMAGE_MAX <= 0.0) {
			return;
		}

		int currentTick = server.getTicks();
		PLUS_ULTRA_IMPACT_STATES.put(
			target.getUuid(),
			new PlusUltraImpactState(
				world.getRegistryKey(),
				attacker.getUuid(),
				currentTick,
				currentTick + PLUS_ULTRA_IMPACT_TRACKING_TICKS,
				entityPosition(target),
				target.getVelocity(),
				target.getVelocity().length(),
				false
			)
		);
	}

	private static void deactivateComedicAssistant(
		ServerPlayerEntity player,
		int currentTick,
		int cooldownTicks,
		boolean manualCancel,
		boolean expired
	) {
		if (COMEDIC_ASSISTANT_ARMED_STATES.remove(player.getUuid()) == null) {
			return;
		}

		startComedicAssistantCooldown(player.getUuid(), currentTick, cooldownTicks);
		if (manualCancel) {
			player.sendMessage(Text.translatable("message.magic.jester.comedic_assistant.cancelled"), true);
			return;
		}
		if (expired) {
			player.sendMessage(Text.translatable("message.magic.jester.comedic_assistant.expired"), true);
		}
	}

	private static void clearComedicAssistantState(UUID playerId) {
		COMEDIC_ASSISTANT_ARMED_STATES.remove(playerId);
	}

	private static boolean clearComedicAssistantCaneImpactState(UUID playerId) {
		boolean removed = COMEDIC_ASSISTANT_CANE_IMPACT_STATES.remove(playerId) != null;
		removed |= COMEDIC_ASSISTANT_CANE_IMPACT_STATES.entrySet().removeIf(entry -> entry.getValue().casterId().equals(playerId));
		return removed;
	}

	private static ComedicAssistantOutcome selectComedicAssistantOutcome(ServerPlayerEntity player) {
		int slimeWeight = COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.enabled() ? Math.max(0, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.weight()) : 0;
		int pandaWeight = COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.enabled() ? Math.max(0, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.weight()) : 0;
		int parrotWeight = COMEDIC_ASSISTANT_PARROT_KIDNAPPING.enabled() ? Math.max(0, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.weight()) : 0;
		int divineWeight = COMEDIC_ASSISTANT_DIVINE_OVERREACTION.enabled() ? Math.max(0, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.weight()) : 0;
		int acmeWeight = COMEDIC_ASSISTANT_ACME_DROP.enabled() ? Math.max(0, COMEDIC_ASSISTANT_ACME_DROP.weight()) : 0;
		int pieWeight = COMEDIC_ASSISTANT_PIE_TO_THE_FACE.enabled() ? Math.max(0, COMEDIC_ASSISTANT_PIE_TO_THE_FACE.weight()) : 0;
		int caneWeight = COMEDIC_ASSISTANT_GIANT_CANE_YANK.enabled() ? Math.max(0, COMEDIC_ASSISTANT_GIANT_CANE_YANK.weight()) : 0;
		int totalWeight = slimeWeight + pandaWeight + parrotWeight + divineWeight + acmeWeight + pieWeight + caneWeight;
		if (totalWeight <= 0) {
			return null;
		}

		int roll = player.getRandom().nextInt(totalWeight);
		if (roll < slimeWeight) {
			return ComedicAssistantOutcome.GIANT_SLIME_SLAM;
		}
		roll -= slimeWeight;
		if (roll < pandaWeight) {
			return ComedicAssistantOutcome.PANDA_BOWLING_BALL;
		}
		roll -= pandaWeight;
		if (roll < parrotWeight) {
			return ComedicAssistantOutcome.PARROT_KIDNAPPING;
		}
		roll -= parrotWeight;
		if (roll < divineWeight) {
			return ComedicAssistantOutcome.DIVINE_OVERREACTION;
		}
		roll -= divineWeight;
		if (roll < acmeWeight) {
			return ComedicAssistantOutcome.ACME_DROP;
		}
		roll -= acmeWeight;
		if (roll < pieWeight) {
			return ComedicAssistantOutcome.PIE_TO_THE_FACE;
		}
		return ComedicAssistantOutcome.GIANT_CANE_YANK;
	}

	private static void sendComedicAssistantProcOverlay(ServerPlayerEntity attacker, LivingEntity target, ComedicAssistantOutcome outcome) {
		JesterJokeOverlayPayload payload = new JesterJokeOverlayPayload(
			outcome.displayName().getString(),
			COMEDIC_ASSISTANT_OVERLAY_COLOR_RGB,
			COMEDIC_ASSISTANT_OVERLAY_FADE_IN_TICKS,
			COMEDIC_ASSISTANT_OVERLAY_STAY_TICKS,
			COMEDIC_ASSISTANT_OVERLAY_FADE_OUT_TICKS
		);
		ServerPlayNetworking.send(attacker, payload);
		if (target instanceof ServerPlayerEntity targetPlayer && targetPlayer != attacker) {
			ServerPlayNetworking.send(targetPlayer, payload);
		}
	}

	private static void applyComedicAssistantOutcome(
		ComedicAssistantOutcome outcome,
		ServerPlayerEntity attacker,
		LivingEntity target,
		ServerWorld world,
		int currentTick,
		float damageTaken
	) {
		switch (outcome) {
			case GIANT_SLIME_SLAM -> applyComedicAssistantSlimeSlam(attacker, target, world);
			case PANDA_BOWLING_BALL -> applyComedicAssistantPandaBowlingBall(attacker, target, world);
			case PARROT_KIDNAPPING -> applyComedicAssistantParrotKidnapping(attacker, target, world, currentTick);
			case DIVINE_OVERREACTION -> applyComedicAssistantDivineOverreaction(attacker, target, world, damageTaken);
			case ACME_DROP -> applyComedicAssistantAcmeDrop(attacker, target, world);
			case PIE_TO_THE_FACE -> applyComedicAssistantPieToTheFace(attacker, target, world);
			case GIANT_CANE_YANK -> applyComedicAssistantGiantCaneYank(attacker, target, world);
		}
	}

	private static void applyComedicAssistantSlimeSlam(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		Vec3d impactPos = new Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ());
		playConfiguredSound(world, impactPos.add(0.0, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualSpawnHeight(), 0.0), SoundEvents.ENTITY_SLIME_JUMP, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.spawnSoundVolume(), COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.spawnSoundPitch());
		playConfiguredSound(world, impactPos, SoundEvents.ENTITY_SLIME_SQUISH, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.impactSoundVolume(), COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.impactSoundPitch());
		spawnComedicAssistantBlockParticles(world, Blocks.SLIME_BLOCK.getDefaultState(), impactPos, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.particleCount(), 0.45, 0.1);
		spawnComedicAssistantSlimeVisual(target, world);
		if (!target.isAlive()) {
			return;
		}

		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.bonusDamage());
		addConfiguredStatusEffect(target, StatusEffects.SLOWNESS, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.slownessDurationTicks(), COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.slownessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.OOZING, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.oozingDurationTicks(), COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.oozingAmplifier());
	}

	private static void applyComedicAssistantPandaBowlingBall(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		Vec3d launchDirection = directionFromAttackerToTarget(attacker, target);
		Vec3d impactPos = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.45, target.getZ());
		playConfiguredSound(world, impactPos, SoundEvents.ENTITY_PANDA_BITE, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.rollSoundVolume(), COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.rollSoundPitch());
		playConfiguredSound(world, impactPos, SoundEvents.ENTITY_PANDA_HURT, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.impactSoundVolume(), COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.impactSoundPitch());
		spawnComedicAssistantItemParticles(world, new ItemStack(Items.BAMBOO), impactPos, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.particleCount(), 0.35, 0.14);
		spawnComedicAssistantPandaVisual(target, world, launchDirection);
		if (!target.isAlive()) {
			return;
		}

		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.bonusDamage());
		launchTarget(target, launchDirection, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.horizontalLaunch(), COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.verticalLaunch());
		addConfiguredStatusEffect(target, StatusEffects.SLOWNESS, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.slownessDurationTicks(), COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.slownessAmplifier());
	}

	private static void applyComedicAssistantParrotKidnapping(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world, int currentTick) {
		Vec3d impactPos = new Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ());
		playConfiguredSound(world, impactPos, SoundEvents.ENTITY_PARROT_FLY, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.soundVolume(), COMEDIC_ASSISTANT_PARROT_KIDNAPPING.soundPitch());
		spawnComedicAssistantItemParticles(world, new ItemStack(Items.FEATHER), impactPos, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.particleCount(), 0.55, 0.12);
		spawnComedicAssistantParrotVisual(target, world);
		if (!target.isAlive()) {
			return;
		}

		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.bonusDamage());
		double targetY = resolveComedicAssistantCarryTargetY(target, world, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.liftHeight());
		if (targetY > target.getY() + 1.0 && COMEDIC_ASSISTANT_PARROT_KIDNAPPING.maxCarryTicks() > 0) {
			COMEDIC_ASSISTANT_PARROT_CARRY_STATES.put(
				target.getUuid(),
				new ComedicAssistantParrotCarryState(
					world.getRegistryKey(),
					targetY,
					currentTick + COMEDIC_ASSISTANT_PARROT_KIDNAPPING.maxCarryTicks(),
					currentTick,
					currentTick,
					COMEDIC_ASSISTANT_PARROT_KIDNAPPING
				)
			);
			target.fallDistance = 0.0F;
		} else {
			launchTarget(target, new Vec3d(0.0, 1.0, 0.0), 0.0, Math.max(0.6, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.upwardVelocity()));
		}
		if (COMEDIC_ASSISTANT_PARROT_KIDNAPPING.applyGlowing()) {
			addConfiguredStatusEffect(target, StatusEffects.GLOWING, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.glowingDurationTicks(), 0);
		}
	}

	private static void applyComedicAssistantDivineOverreaction(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world, float damageTaken) {
		Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ());
		playConfiguredSound(world, targetPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.soundVolume(), COMEDIC_ASSISTANT_DIVINE_OVERREACTION.soundPitch());
		playConfiguredSound(world, targetPos, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.soundVolume() * 0.85F, Math.max(0.1F, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.soundPitch() + 0.15F));
		int strikeCount = Math.max(1, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.strikeCount());
		spawnComedicAssistantDivineLightningVisual(target, world, strikeCount);
		for (int index = 0; index < strikeCount; index++) {
			double angle = (Math.PI * 2.0 * index) / strikeCount;
			double radius = target.getRandom().nextDouble() * COMEDIC_ASSISTANT_DIVINE_OVERREACTION.strikeRadius();
			double x = targetPos.x + Math.cos(angle) * radius;
			double z = targetPos.z + Math.sin(angle) * radius;
			world.spawnParticles(
				ParticleTypes.ELECTRIC_SPARK,
				x,
				targetPos.y + target.getRandom().nextDouble() * Math.max(1.0, target.getHeight()),
				z,
				Math.max(1, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.particleCount() / Math.max(1, strikeCount / 2)),
				0.08,
				0.5,
				0.08,
				0.25
			);
			world.spawnParticles(ParticleTypes.GLOW, x, targetPos.y + 0.2, z, 2, 0.02, 0.2, 0.02, 0.01);
		}
		world.spawnParticles(
			ParticleTypes.POOF,
			targetPos.x,
			targetPos.y,
			targetPos.z,
			Math.max(6, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.particleCount() / 2),
			0.45,
			0.4,
			0.45,
			0.06
		);
		if (!target.isAlive()) {
			return;
		}

		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.bonusDamage());
		addConfiguredStatusEffect(target, StatusEffects.GLOWING, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.glowingDurationTicks(), 0);
		addConfiguredStatusEffect(target, StatusEffects.BLINDNESS, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.blindnessDurationTicks(), COMEDIC_ASSISTANT_DIVINE_OVERREACTION.blindnessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.NAUSEA, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.nauseaDurationTicks(), COMEDIC_ASSISTANT_DIVINE_OVERREACTION.nauseaAmplifier());
	}

	private static void applyComedicAssistantAcmeDrop(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ());
		playConfiguredSound(world, targetPos, SoundEvents.BLOCK_ANVIL_LAND, COMEDIC_ASSISTANT_ACME_DROP.soundVolume(), COMEDIC_ASSISTANT_ACME_DROP.soundPitch());
		spawnComedicAssistantBlockParticles(world, Blocks.ANVIL.getDefaultState(), targetPos, COMEDIC_ASSISTANT_ACME_DROP.particleCount(), 0.35, 0.12);
		world.spawnParticles(
			ParticleTypes.CAMPFIRE_COSY_SMOKE,
			targetPos.x,
			target.getY() + 0.05,
			targetPos.z,
			Math.max(4, COMEDIC_ASSISTANT_ACME_DROP.particleCount() / 2),
			0.35,
			0.05,
			0.35,
			0.02
		);
		spawnComedicAssistantAcmeVisual(target, world);
		if (!target.isAlive()) {
			return;
		}

		target.setVelocity(0.0, Math.min(0.0, target.getVelocity().y), 0.0);
		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_ACME_DROP.bonusDamage());
		addConfiguredStatusEffect(target, StatusEffects.SLOWNESS, COMEDIC_ASSISTANT_ACME_DROP.slownessDurationTicks(), COMEDIC_ASSISTANT_ACME_DROP.slownessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.WEAKNESS, COMEDIC_ASSISTANT_ACME_DROP.weaknessDurationTicks(), COMEDIC_ASSISTANT_ACME_DROP.weaknessAmplifier());
	}

	private static void applyComedicAssistantPieToTheFace(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		Vec3d facePos = new Vec3d(target.getX(), target.getEyeY(), target.getZ());
		playConfiguredSound(world, facePos, SoundEvents.ENTITY_GENERIC_SPLASH, COMEDIC_ASSISTANT_PIE_TO_THE_FACE.soundVolume(), COMEDIC_ASSISTANT_PIE_TO_THE_FACE.soundPitch());
		spawnComedicAssistantItemParticles(world, new ItemStack(Items.PUMPKIN_PIE), facePos, COMEDIC_ASSISTANT_PIE_TO_THE_FACE.particleCount(), 0.16, 0.08);
		if (!target.isAlive()) {
			return;
		}

		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_PIE_TO_THE_FACE.bonusDamage());
		addConfiguredStatusEffect(target, StatusEffects.BLINDNESS, COMEDIC_ASSISTANT_PIE_TO_THE_FACE.blindnessDurationTicks(), COMEDIC_ASSISTANT_PIE_TO_THE_FACE.blindnessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.NAUSEA, COMEDIC_ASSISTANT_PIE_TO_THE_FACE.nauseaDurationTicks(), COMEDIC_ASSISTANT_PIE_TO_THE_FACE.nauseaAmplifier());
	}

	private static void applyComedicAssistantGiantCaneYank(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		Vec3d yankDirection = sidewaysDirectionFromAttackerToTarget(attacker, target);
		Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.4, target.getZ());
		playConfiguredSound(world, targetPos, SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE, COMEDIC_ASSISTANT_GIANT_CANE_YANK.soundVolume(), COMEDIC_ASSISTANT_GIANT_CANE_YANK.soundPitch());
		spawnComedicAssistantItemParticles(world, new ItemStack(Items.STICK), targetPos, COMEDIC_ASSISTANT_GIANT_CANE_YANK.particleCount(), 0.28, 0.12);
		world.spawnParticles(
			ParticleTypes.CLOUD,
			targetPos.x,
			targetPos.y,
			targetPos.z,
			Math.max(2, COMEDIC_ASSISTANT_GIANT_CANE_YANK.particleCount() / 2),
			0.25,
			0.2,
			0.25,
			0.03
		);
		spawnComedicAssistantCaneVisual(target, world, yankDirection);
		if (!target.isAlive()) {
			return;
		}

		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_GIANT_CANE_YANK.bonusDamage());
		launchTarget(target, yankDirection, COMEDIC_ASSISTANT_GIANT_CANE_YANK.horizontalLaunch(), COMEDIC_ASSISTANT_GIANT_CANE_YANK.verticalLaunch());
		trackComedicAssistantCaneImpact(attacker, target, world);
		addConfiguredStatusEffect(target, StatusEffects.SLOWNESS, COMEDIC_ASSISTANT_GIANT_CANE_YANK.slownessDurationTicks(), COMEDIC_ASSISTANT_GIANT_CANE_YANK.slownessAmplifier());
	}

	private static void applyComedicAssistantBonusDamage(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world, float bonusDamage) {
		if (bonusDamage <= 0.0F || !target.isAlive()) {
			return;
		}
		dealTrackedMagicDamage(target, attacker.getUuid(), world.getDamageSources().genericKill(), bonusDamage);
	}

	private static void launchTarget(LivingEntity target, Vec3d direction, double horizontalVelocity, double verticalVelocity) {
		Vec3d launchDirection = normalizedHorizontalDirection(direction, target instanceof ServerPlayerEntity player ? player : null);
		applyForcedVelocity(target, new Vec3d(launchDirection.x * horizontalVelocity, verticalVelocity, launchDirection.z * horizontalVelocity));
	}

	private static void applyForcedVelocity(LivingEntity target, Vec3d velocity) {
		target.setVelocity(velocity);
		target.setOnGround(false);
		target.fallDistance = 0.0F;
		target.velocityDirty = true;
		if (target instanceof ServerPlayerEntity playerTarget) {
			playerTarget.networkHandler.requestTeleport(playerTarget.getX(), playerTarget.getY(), playerTarget.getZ(), playerTarget.getYaw(), playerTarget.getPitch());
			playerTarget.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(playerTarget.getId(), playerTarget.getVelocity()));
		}
	}

	private static void forceTargetPositionAndVelocity(ServerWorld world, LivingEntity target, Vec3d targetPos, Vec3d velocity) {
		if (target.hasVehicle()) {
			target.stopRiding();
		}

		if (target instanceof ServerPlayerEntity playerTarget) {
			playerTarget.teleport(world, targetPos.x, targetPos.y, targetPos.z, Set.<PositionFlag>of(), playerTarget.getYaw(), playerTarget.getPitch(), false);
		} else {
			target.requestTeleport(targetPos.x, targetPos.y, targetPos.z);
		}
		applyForcedVelocity(target, velocity);
	}

	private static Vec3d entityPosition(Entity entity) {
		return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
	}

	private static void trackComedicAssistantCaneImpact(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		MinecraftServer server = world.getServer();
		if (server == null || COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageTrackingTicks() <= 0 || COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageMax() <= 0.0) {
			return;
		}

		int currentTick = server.getTicks();
		COMEDIC_ASSISTANT_CANE_IMPACT_STATES.put(
			target.getUuid(),
			new ComedicAssistantCaneImpactState(
				world.getRegistryKey(),
				attacker.getUuid(),
				currentTick,
				currentTick + COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageTrackingTicks(),
				currentTick + COMEDIC_ASSISTANT_GIANT_CANE_YANK.launchControlTicks(),
				target.getVelocity(),
				entityPosition(target),
				target.getVelocity(),
				target.getVelocity().length()
			)
		);
	}

	private static Vec3d directionFromAttackerToTarget(ServerPlayerEntity attacker, LivingEntity target) {
		Vec3d direction = new Vec3d(target.getX() - attacker.getX(), 0.0, target.getZ() - attacker.getZ());
		if (direction.lengthSquared() > 1.0E-5) {
			return direction.normalize();
		}
		Vec3d facing = attacker.getRotationVector();
		return normalizedHorizontalDirection(new Vec3d(facing.x, 0.0, facing.z), attacker);
	}

	private static Vec3d sidewaysDirectionFromAttackerToTarget(ServerPlayerEntity attacker, LivingEntity target) {
		Vec3d baseDirection = directionFromAttackerToTarget(attacker, target);
		Vec3d sideways = new Vec3d(-baseDirection.z, 0.0, baseDirection.x);
		if (target.getRandom().nextBoolean()) {
			sideways = sideways.multiply(-1.0);
		}
		return sideways.normalize();
	}

	private static void spawnComedicAssistantBlockParticles(
		ServerWorld world,
		BlockState state,
		Vec3d center,
		int count,
		double spread,
		double speed
	) {
		if (count <= 0) {
			return;
		}
		world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), center.x, center.y, center.z, count, spread, spread, spread, speed);
	}

	private static void spawnComedicAssistantItemParticles(
		ServerWorld world,
		ItemStack stack,
		Vec3d center,
		int count,
		double spread,
		double speed
	) {
		if (count <= 0) {
			return;
		}
		world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stack), center.x, center.y, center.z, count, spread, spread, spread, speed);
	}

	private static void playConfiguredSound(ServerWorld world, Vec3d pos, SoundEvent sound, float volume, float pitch) {
		if (world == null || sound == null || volume <= 0.0F || pitch <= 0.0F) {
			return;
		}
		world.playSound(null, pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, volume, pitch);
	}

	private static void spawnComedicAssistantSlimeVisual(LivingEntity target, ServerWorld world) {
		if (COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualDurationTicks() <= 0 || COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualSpawnHeight() <= 0.0) {
			return;
		}

		Vec3d basePos = new Vec3d(target.getX(), target.getY(), target.getZ());
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(world, basePos);
		if (viewers.isEmpty()) {
			return;
		}

		SlimeEntity slime = new SlimeEntity(EntityType.SLIME, world);
		double startY = target.getY() + target.getHeight() + COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualSpawnHeight();
		slime.refreshPositionAndAngles(target.getX(), startY, target.getZ(), target.getYaw(), 0.0F);
		slime.setBodyYaw(target.getYaw());
		slime.setHeadYaw(target.getYaw());
		slime.setAiDisabled(true);
		slime.setSilent(true);
		slime.setNoGravity(true);
		slime.setSize(COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualSize(), false);
		slime.setVelocity(Vec3d.ZERO);
		queueComedicRewriteVisualCameo(
			world.getServer(),
			COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualDurationTicks(),
			viewers,
			0,
			new ComedicRewriteVisualFollowState(
				world.getRegistryKey(),
				target.getUuid(),
				new Vec3d[] { Vec3d.ZERO },
				ComedicRewriteVisualFollowMode.DROP_TO_ENTITY_TOP,
				COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualFallSpeed()
			),
			slime
		);
	}

	private static void spawnComedicAssistantPandaVisual(LivingEntity target, ServerWorld world, Vec3d direction) {
		if (COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualDurationTicks() <= 0) {
			return;
		}

		Vec3d basePos = new Vec3d(target.getX(), target.getY(), target.getZ());
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(world, basePos);
		if (viewers.isEmpty()) {
			return;
		}

		Vec3d travelDirection = normalizedHorizontalDirection(direction, null);
		Vec3d spawnPos = basePos.add(0.0, 0.1, 0.0).subtract(travelDirection.multiply(COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualSpawnDistance()));
		float yaw = horizontalYawFromDirection(travelDirection);
		PandaEntity panda = new PandaEntity(EntityType.PANDA, world);
		panda.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, yaw, 0.0F);
		panda.setBodyYaw(yaw);
		panda.setHeadYaw(yaw);
		panda.setAiDisabled(true);
		panda.setSilent(true);
		panda.setNoGravity(true);
		panda.setVelocity(travelDirection.multiply(COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualChargeVelocity()));
		int chargeTicks = COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualChargeVelocity() <= 0.0
			? Math.max(1, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualDurationTicks() / 2)
			: Math.max(1, Math.min(COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualDurationTicks(), (int) Math.ceil((COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualSpawnDistance() * 2.0 + 1.0) / COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualChargeVelocity())));
		panda.setMainGene(PandaEntity.Gene.PLAYFUL);
		panda.setHiddenGene(PandaEntity.Gene.PLAYFUL);
		panda.setPlaying(true);
		panda.playingTicks = chargeTicks;
		queueComedicRewriteVisualCameo(world.getServer(), COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualDurationTicks(), viewers, chargeTicks, null, panda);
	}

	private static void spawnComedicAssistantParrotVisual(LivingEntity target, ServerWorld world) {
		if (COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualDurationTicks() <= 0 || COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualCount() <= 0) {
			return;
		}

		Vec3d basePos = new Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ());
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(world, basePos);
		if (viewers.isEmpty()) {
			return;
		}

		List<Entity> parrots = new ArrayList<>();
		Vec3d[] offsets = new Vec3d[COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualCount()];
		for (int index = 0; index < COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualCount(); index++) {
			double angle = (Math.PI * 2.0 * index) / COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualCount();
			double offsetX = Math.cos(angle) * COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualRadius();
			double offsetZ = Math.sin(angle) * COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualRadius();
			Vec3d offset = new Vec3d(offsetX, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualVerticalOffset() + (index % 2 == 0 ? 0.12 : 0.0), offsetZ);
			offsets[index] = offset;
			Vec3d spawnPos = basePos.add(offset);
			ParrotEntity parrot = new ParrotEntity(EntityType.PARROT, world);
			parrot.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, target.getYaw(), 0.0F);
			parrot.setBodyYaw(target.getYaw());
			parrot.setHeadYaw(target.getYaw());
			parrot.setAiDisabled(true);
			parrot.setSilent(true);
			parrot.setNoGravity(true);
			parrot.setVelocity(Vec3d.ZERO);
			parrot.setComponent(
				DataComponentTypes.PARROT_VARIANT,
				ParrotEntity.Variant.byIndex(target.getRandom().nextInt(ParrotEntity.Variant.values().length))
			);
			parrots.add(parrot);
		}
		queueComedicRewriteVisualCameo(
			world.getServer(),
			Math.max(COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualDurationTicks(), COMEDIC_ASSISTANT_PARROT_KIDNAPPING.maxCarryTicks()),
			viewers,
			0,
			new ComedicRewriteVisualFollowState(
				world.getRegistryKey(),
				target.getUuid(),
				offsets,
				ComedicRewriteVisualFollowMode.ENTITY_TOP,
				0.0
			),
			parrots.toArray(Entity[]::new)
		);
	}

	private static void spawnComedicAssistantAcmeVisual(LivingEntity target, ServerWorld world) {
		if (COMEDIC_ASSISTANT_ACME_DROP.visualDurationTicks() <= 0 || COMEDIC_ASSISTANT_ACME_DROP.visualDropHeight() <= 0.0) {
			return;
		}

		MinecraftServer server = world.getServer();
		if (server == null) {
			return;
		}

		DisplayEntity.BlockDisplayEntity anvil = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
		double startY = target.getY() + target.getHeight() + COMEDIC_ASSISTANT_ACME_DROP.visualDropHeight();
		anvil.refreshPositionAndAngles(target.getX(), startY, target.getZ(), 0.0F, 0.0F);
		((BlockDisplayEntityAccessorMixin) anvil).magic$setBlockState(Blocks.ANVIL.getDefaultState());
		((DisplayEntityAccessorMixin) anvil).magic$setTeleportDuration(1);
		((DisplayEntityAccessorMixin) anvil).magic$setInterpolationDuration(1);
		((DisplayEntityAccessorMixin) anvil).magic$setDisplayWidth(1.25F);
		((DisplayEntityAccessorMixin) anvil).magic$setDisplayHeight(1.25F);
		((DisplayEntityAccessorMixin) anvil).magic$setViewRange(1.0F);
		anvil.setNoGravity(true);
		anvil.setSilent(true);
		if (!world.spawnEntity(anvil)) {
			return;
		}
		COMEDIC_ASSISTANT_ACME_VISUALS.put(
			anvil.getUuid(),
			new ComedicAssistantAcmeVisualState(
				world.getRegistryKey(),
				anvil.getUuid(),
				target.getUuid(),
				server.getTicks() + COMEDIC_ASSISTANT_ACME_DROP.visualDurationTicks(),
				comedicAssistantAcmeDropSpeed()
			)
		);
	}

	private static void spawnComedicAssistantCaneVisual(LivingEntity target, ServerWorld world, Vec3d direction) {
		if (COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualDurationTicks() <= 0) {
			return;
		}

		Vec3d basePos = new Vec3d(target.getX(), target.getY(), target.getZ());
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(world, basePos);
		if (viewers.isEmpty()) {
			return;
		}

		Vec3d travelDirection = normalizedHorizontalDirection(direction, null);
		Vec3d spawnPos = basePos.add(0.0, target.getHeight() * 0.45, 0.0).subtract(travelDirection.multiply(COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualSpawnDistance()));
		float yaw = horizontalYawFromDirection(travelDirection);
		ItemEntity cane = new ItemEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, new ItemStack(Items.STICK));
		cane.setYaw(yaw);
		cane.setBodyYaw(yaw);
		cane.setHeadYaw(yaw);
		cane.setPitch(90.0F);
		cane.setNoGravity(true);
		cane.setSilent(true);
		cane.setVelocity(travelDirection.multiply(COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualChargeVelocity()));
		int chargeTicks = COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualChargeVelocity() <= 0.0
			? Math.max(1, COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualDurationTicks() / 2)
			: Math.max(1, Math.min(COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualDurationTicks(), (int) Math.ceil((COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualSpawnDistance() * 2.0 + 0.5) / COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualChargeVelocity())));
		queueComedicRewriteVisualCameo(
			world.getServer(),
			COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualDurationTicks(),
			viewers,
			chargeTicks,
			null,
			new ComedicRewriteVisualRotationState(true, 0.0F, 30.0F),
			cane
		);
	}

	private static void spawnComedicAssistantDivineLightningVisual(LivingEntity target, ServerWorld world, int strikeCount) {
		int visibleStrikeCount = Math.max(1, Math.min(4, strikeCount));
		for (int index = 0; index < visibleStrikeCount; index++) {
			double angle = (Math.PI * 2.0 * index) / visibleStrikeCount;
			double radius = target.getRandom().nextDouble() * COMEDIC_ASSISTANT_DIVINE_OVERREACTION.strikeRadius();
			double x = target.getX() + Math.cos(angle) * radius;
			double z = target.getZ() + Math.sin(angle) * radius;
			LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
			lightning.setCosmetic(true);
			lightning.refreshPositionAfterTeleport(x, target.getY(), z);
			world.spawnEntity(lightning);
		}
	}

	private static double comedicAssistantAcmeDropSpeed() {
		if (COMEDIC_ASSISTANT_ACME_DROP.visualDropHeight() <= 0.0) {
			return 0.0;
		}
		double durationBasedSpeed = COMEDIC_ASSISTANT_ACME_DROP.visualDurationTicks() <= 1
			? COMEDIC_ASSISTANT_ACME_DROP.visualDropHeight()
			: COMEDIC_ASSISTANT_ACME_DROP.visualDropHeight() / Math.max(1.0, COMEDIC_ASSISTANT_ACME_DROP.visualDurationTicks() * 0.45);
		return Math.max(1.35, durationBasedSpeed);
	}

	private static void updateComedicAssistantAcmeVisuals(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, ComedicAssistantAcmeVisualState>> iterator = COMEDIC_ASSISTANT_ACME_VISUALS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ComedicAssistantAcmeVisualState> entry = iterator.next();
			ComedicAssistantAcmeVisualState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension());
			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity visualEntity = world.getEntity(state.visualEntityId());
			if (!(visualEntity instanceof DisplayEntity.BlockDisplayEntity anvil) || !anvil.isAlive()) {
				iterator.remove();
				continue;
			}

			if (currentTick >= state.endTick()) {
				anvil.discard();
				iterator.remove();
				continue;
			}

			Entity targetEntity = world.getEntity(state.targetEntityId());
			if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
				anvil.discard();
				iterator.remove();
				continue;
			}

			double targetY = target.getY() + target.getHeight();
			double nextY = Math.max(targetY, anvil.getY() - state.dropSpeed());
			anvil.refreshPositionAfterTeleport(target.getX(), nextY, target.getZ());
		}
	}

	private static void updateComedicAssistantCaneImpactStates(MinecraftServer server, int currentTick) {
		List<Map.Entry<UUID, ComedicAssistantCaneImpactState>> entries = new ArrayList<>(COMEDIC_ASSISTANT_CANE_IMPACT_STATES.entrySet());
		for (Map.Entry<UUID, ComedicAssistantCaneImpactState> entry : entries) {
			ComedicAssistantCaneImpactState state = entry.getValue();
			if (COMEDIC_ASSISTANT_CANE_IMPACT_STATES.get(entry.getKey()) != state) {
				continue;
			}
			ServerWorld world = server.getWorld(state.dimension());
			if (world == null) {
				COMEDIC_ASSISTANT_CANE_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
				COMEDIC_ASSISTANT_CANE_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			if (currentTick > state.endTick()) {
				COMEDIC_ASSISTANT_CANE_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			if (currentTick <= state.startTick()) {
				state.lastPosition(entityPosition(target));
				state.lastVelocity(target.getVelocity());
				state.lastSpeed(target.getVelocity().length());
				continue;
			}

			PlusUltraImpactHit impact = detectVelocityImpact(
				target,
				world,
				state.lastPosition(),
				state.lastVelocity(),
				state.lastSpeed(),
				COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageThreshold()
			);
			if (impact != null) {
				double impactSpeed = trackedImpactSpeed(target, state.lastPosition(), state.lastVelocity(), state.lastSpeed());
				double damageAmount = Math.min(
					COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageMax(),
					Math.max(0.0, impactSpeed - COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageThreshold()) * COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageMultiplier()
				);
				if (damageAmount > 0.0) {
					dealTrackedMagicDamage(target, state.casterId(), world.getDamageSources().flyIntoWall(), (float) damageAmount);
					spawnComedicAssistantCaneImpactParticles(world, impact);
					playConfiguredSound(
						world,
						impact.position(),
						SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY,
						Math.max(0.0F, COMEDIC_ASSISTANT_GIANT_CANE_YANK.soundVolume() * 0.9F),
						Math.max(0.1F, COMEDIC_ASSISTANT_GIANT_CANE_YANK.soundPitch() * 0.85F)
					);
				}
				COMEDIC_ASSISTANT_CANE_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			if (currentTick <= state.forceEndTick()) {
				applyForcedVelocity(target, state.forcedVelocity());
			}
			state.lastPosition(entityPosition(target));
			state.lastVelocity(target.getVelocity());
			state.lastSpeed(target.getVelocity().length());
		}
	}

	private static void updatePlusUltraImpactStates(MinecraftServer server, int currentTick) {
		List<Map.Entry<UUID, PlusUltraImpactState>> entries = new ArrayList<>(PLUS_ULTRA_IMPACT_STATES.entrySet());
		for (Map.Entry<UUID, PlusUltraImpactState> entry : entries) {
			PlusUltraImpactState state = entry.getValue();
			if (PLUS_ULTRA_IMPACT_STATES.get(entry.getKey()) != state) {
				continue;
			}
			ServerWorld world = server.getWorld(state.dimension());
			if (world == null) {
				PLUS_ULTRA_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
				PLUS_ULTRA_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			if (currentTick > state.endTick()) {
				PLUS_ULTRA_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			if (state.impacted()) {
				PLUS_ULTRA_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			if (currentTick <= state.startTick()) {
				state.lastPosition(entityPosition(target));
				state.lastVelocity(target.getVelocity());
				state.lastSpeed(target.getVelocity().length());
				continue;
			}

			PlusUltraImpactHit impact = detectPlusUltraImpact(target, world, state);
			if (impact != null) {
				double impactSpeed = trackedImpactSpeed(target, state.lastPosition(), state.lastVelocity(), state.lastSpeed());
				double damageAmount = Math.min(
					PLUS_ULTRA_IMPACT_DAMAGE_MAX,
					Math.max(0.0, impactSpeed - PLUS_ULTRA_IMPACT_VELOCITY_THRESHOLD) * PLUS_ULTRA_IMPACT_DAMAGE_MULTIPLIER
				);
				if (damageAmount > 0.0) {
					dealTrackedMagicDamage(target, state.casterId(), world.getDamageSources().flyIntoWall(), (float) damageAmount);
					spawnPlusUltraImpactParticles(world, impact);
					playConfiguredSound(world, impact.position(), SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY, PLUS_ULTRA_IMPACT_SOUND_VOLUME, PLUS_ULTRA_IMPACT_SOUND_PITCH);
				}
				state.impacted(true);
				PLUS_ULTRA_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			state.lastPosition(entityPosition(target));
			state.lastVelocity(target.getVelocity());
			state.lastSpeed(target.getVelocity().length());
		}
	}

	private static PlusUltraImpactHit detectPlusUltraImpact(LivingEntity target, ServerWorld world, PlusUltraImpactState state) {
		return detectVelocityImpact(target, world, state.lastPosition(), state.lastVelocity(), state.lastSpeed(), PLUS_ULTRA_IMPACT_VELOCITY_THRESHOLD);
	}

	private static double trackedImpactSpeed(LivingEntity target, Vec3d lastPosition, Vec3d lastVelocity, double lastSpeed) {
		Vec3d targetPosition = entityPosition(target);
		return Math.max(
			Math.max(lastSpeed, lastVelocity.length()),
			Math.max(target.getVelocity().length(), targetPosition.subtract(lastPosition).length())
		);
	}

	private static PlusUltraImpactHit detectVelocityImpact(
		LivingEntity target,
		ServerWorld world,
		Vec3d lastPosition,
		Vec3d lastVelocity,
		double lastSpeed,
		double impactVelocityThreshold
	) {
		Vec3d targetPosition = entityPosition(target);
		Vec3d movement = targetPosition.subtract(lastPosition);
		Vec3d sweepVector = movement.lengthSquared() > 1.0E-4 ? movement : lastVelocity;
		if (sweepVector.lengthSquared() <= 1.0E-4) {
			return null;
		}

		double impactSpeed = trackedImpactSpeed(target, lastPosition, lastVelocity, lastSpeed);
		if (impactSpeed < impactVelocityThreshold) {
			return null;
		}

		double currentSpeed = Math.max(target.getVelocity().length(), movement.length());
		boolean collidedWithBlock = target.isInsideWall() || target.horizontalCollision || target.verticalCollision || target.groundCollision;
		boolean slowedHard = lastSpeed >= impactVelocityThreshold
			&& currentSpeed <= Math.max(0.2, lastSpeed * 0.35);
		Vec3d traceEndPos = movement.lengthSquared() > 1.0E-4 ? targetPosition : lastPosition.add(sweepVector);
		BlockHitResult raycastHit = findPlusUltraImpactBlockHit(world, target, lastPosition, traceEndPos, sweepVector);
		if (raycastHit != null) {
			return plusUltraImpactHit(world, raycastHit);
		}

		PlusUltraImpactHit sweptImpact = sweptPlusUltraImpactHit(target, world, lastPosition, traceEndPos, sweepVector);
		if (sweptImpact != null && (collidedWithBlock || slowedHard || movement.lengthSquared() > 1.0E-4)) {
			return sweptImpact;
		}

		if (!collidedWithBlock) {
			Box previousBox = target.getBoundingBox().offset(lastPosition.subtract(targetPosition));
			Box sweptBox = previousBox.stretch(traceEndPos.subtract(lastPosition)).expand(0.15);
			if (!world.getBlockCollisions(target, sweptBox).iterator().hasNext()) {
				if (!slowedHard) {
					return null;
				}
				PlusUltraImpactHit nearbyImpact = nearbyPlusUltraImpactHit(target, world, sweepVector);
				if (nearbyImpact == null) {
					return null;
				}
				return nearbyImpact;
			}
		}

		return fallbackPlusUltraImpactHit(target, world, sweepVector);
	}

	private static PlusUltraImpactHit nearbyPlusUltraImpactHit(LivingEntity target, ServerWorld world, Vec3d sweepVector) {
		return bestPlusUltraImpactHitInBox(world, target.getBoundingBox().expand(0.35), target.getBoundingBox().getCenter(), sweepVector);
	}

	private static PlusUltraImpactHit sweptPlusUltraImpactHit(LivingEntity target, ServerWorld world, Vec3d startPos, Vec3d endPos, Vec3d sweepVector) {
		Vec3d currentPosition = entityPosition(target);
		Box previousBox = target.getBoundingBox().offset(startPos.subtract(currentPosition));
		Box sweptBox = previousBox.stretch(endPos.subtract(startPos)).expand(0.2);
		return bestPlusUltraImpactHitInBox(world, sweptBox, target.getBoundingBox().getCenter(), sweepVector);
	}

	private static PlusUltraImpactHit bestPlusUltraImpactHitInBox(ServerWorld world, Box searchBox, Vec3d referencePos, Vec3d sweepVector) {
		BlockPos minPos = BlockPos.ofFloored(searchBox.minX, searchBox.minY, searchBox.minZ);
		BlockPos maxPos = BlockPos.ofFloored(searchBox.maxX, searchBox.maxY, searchBox.maxZ);
		Vec3d direction = sweepVector.lengthSquared() > 1.0E-4 ? sweepVector.normalize() : Vec3d.ZERO;
		PlusUltraImpactHit bestHit = null;
		double bestDistanceSquared = Double.MAX_VALUE;
		for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
			for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
				for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
					BlockPos candidatePos = new BlockPos(x, y, z);
					BlockState candidateState = world.getBlockState(candidatePos);
					if (candidateState.isAir() || candidateState.getCollisionShape(world, candidatePos).isEmpty()) {
						continue;
					}

					PlusUltraImpactHit candidateHit = plusUltraImpactHit(candidatePos, candidateState, referencePos, direction);
					Vec3d toCandidate = candidateHit.position().subtract(referencePos);
					if (direction.lengthSquared() > 1.0E-4 && toCandidate.lengthSquared() > 1.0E-4 && direction.dotProduct(toCandidate.normalize()) < -0.35) {
						continue;
					}

					double distanceSquared = referencePos.squaredDistanceTo(candidateHit.position());
					if (distanceSquared < bestDistanceSquared) {
						bestDistanceSquared = distanceSquared;
						bestHit = candidateHit;
					}
				}
			}
		}
		return bestHit;
	}

	private static BlockHitResult findPlusUltraImpactBlockHit(
		ServerWorld world,
		LivingEntity target,
		Vec3d startPos,
		Vec3d endPos,
		Vec3d sweepVector
	) {
		double lowerSampleY = 0.1;
		double middleSampleY = MathHelper.clamp(target.getHeight() * 0.5, 0.2, Math.max(0.2, target.getHeight() - 0.15));
		double upperSampleY = Math.max(middleSampleY, target.getHeight() - 0.15);
		Vec3d horizontalSweep = new Vec3d(sweepVector.x, 0.0, sweepVector.z);
		Vec3d sideOffset = horizontalSweep.lengthSquared() > 1.0E-4
			? new Vec3d(-horizontalSweep.z, 0.0, horizontalSweep.x).normalize().multiply(Math.max(0.2, target.getWidth() * 0.45))
			: Vec3d.ZERO;

		BlockHitResult bestHit = null;
		bestHit = closerPlusUltraImpactHit(bestHit, plusUltraImpactRaycast(world, target, startPos, endPos, lowerSampleY, Vec3d.ZERO), startPos);
		bestHit = closerPlusUltraImpactHit(bestHit, plusUltraImpactRaycast(world, target, startPos, endPos, middleSampleY, Vec3d.ZERO), startPos);
		bestHit = closerPlusUltraImpactHit(bestHit, plusUltraImpactRaycast(world, target, startPos, endPos, middleSampleY, sideOffset), startPos);
		bestHit = closerPlusUltraImpactHit(bestHit, plusUltraImpactRaycast(world, target, startPos, endPos, middleSampleY, sideOffset.multiply(-1.0)), startPos);
		bestHit = closerPlusUltraImpactHit(bestHit, plusUltraImpactRaycast(world, target, startPos, endPos, upperSampleY, Vec3d.ZERO), startPos);
		return bestHit;
	}

	private static BlockHitResult closerPlusUltraImpactHit(BlockHitResult currentBest, BlockHitResult candidate, Vec3d startPos) {
		if (candidate == null) {
			return currentBest;
		}
		if (currentBest == null) {
			return candidate;
		}
		return startPos.squaredDistanceTo(candidate.getPos()) < startPos.squaredDistanceTo(currentBest.getPos()) ? candidate : currentBest;
	}

	private static BlockHitResult plusUltraImpactRaycast(
		ServerWorld world,
		Entity entity,
		Vec3d startPos,
		Vec3d endPos,
		double yOffset,
		Vec3d lateralOffset
	) {
		Vec3d start = startPos.add(lateralOffset).add(0.0, yOffset, 0.0);
		Vec3d end = endPos.add(lateralOffset).add(0.0, yOffset, 0.0);
		if (start.squaredDistanceTo(end) <= 1.0E-6) {
			return null;
		}

		BlockHitResult hitResult = world.raycast(
			new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity)
		);
		return hitResult.getType() == HitResult.Type.BLOCK ? hitResult : null;
	}

	private static PlusUltraImpactHit plusUltraImpactHit(ServerWorld world, BlockHitResult hitResult) {
		BlockPos blockPos = hitResult.getBlockPos();
		BlockState blockState = world.getBlockState(blockPos);
		if (blockState.isAir()) {
			BlockPos offsetPos = blockPos.offset(hitResult.getSide());
			BlockState offsetState = world.getBlockState(offsetPos);
			if (!offsetState.isAir()) {
				blockPos = offsetPos;
				blockState = offsetState;
			}
		}

		if (blockState.isAir()) {
			blockState = Blocks.DIRT.getDefaultState();
		}
		return plusUltraImpactHit(blockPos, blockState, hitResult.getPos(), hitResult.getSide());
	}

	private static PlusUltraImpactHit fallbackPlusUltraImpactHit(LivingEntity target, ServerWorld world, Vec3d sweepVector) {
		Vec3d center = target.getBoundingBox().getCenter();
		Vec3d direction = sweepVector.lengthSquared() > 1.0E-4 ? sweepVector.normalize() : Vec3d.ZERO;
		double probeDistance = Math.max(0.35, target.getWidth() * 0.5);
		PlusUltraImpactHit searchedHit = bestPlusUltraImpactHitInBox(
			world,
			target.getBoundingBox().stretch(direction.multiply(probeDistance)).expand(0.35),
			center,
			sweepVector
		);
		if (searchedHit != null) {
			return searchedHit;
		}

		Direction side = plusUltraImpactSide(center, BlockPos.ofFloored(center), direction);
		Vec3d impactPos = center.add(Vec3d.of(side.getVector()).multiply(Math.max(0.2, target.getWidth() * 0.45)));
		return new PlusUltraImpactHit(impactPos, Blocks.DIRT.getDefaultState(), side);
	}

	private static PlusUltraImpactHit plusUltraImpactHit(BlockPos blockPos, BlockState blockState, Vec3d referencePos, Direction side) {
		BlockState resolvedState = blockState.isAir() ? Blocks.DIRT.getDefaultState() : blockState;
		Vec3d impactPos = plusUltraImpactSurfacePosition(blockPos, side, referencePos);
		return new PlusUltraImpactHit(impactPos, resolvedState, side);
	}

	private static PlusUltraImpactHit plusUltraImpactHit(BlockPos blockPos, BlockState blockState, Vec3d referencePos, Vec3d sweepDirection) {
		return plusUltraImpactHit(blockPos, blockState, referencePos, plusUltraImpactSide(referencePos, blockPos, sweepDirection));
	}

	private static Direction plusUltraImpactSide(Vec3d referencePos, BlockPos blockPos, Vec3d sweepDirection) {
		if (sweepDirection.lengthSquared() > 1.0E-4) {
			return Direction.getFacing(sweepDirection.x, sweepDirection.y, sweepDirection.z).getOpposite();
		}

		Vec3d blockCenter = Vec3d.ofCenter(blockPos);
		Vec3d outward = referencePos.subtract(blockCenter);
		if (outward.lengthSquared() > 1.0E-4) {
			return Direction.getFacing(outward.x, outward.y, outward.z);
		}
		return Direction.UP;
	}

	private static Vec3d plusUltraImpactSurfacePosition(BlockPos blockPos, Direction side, Vec3d referencePos) {
		double minX = blockPos.getX();
		double minY = blockPos.getY();
		double minZ = blockPos.getZ();
		double maxX = minX + 1.0;
		double maxY = minY + 1.0;
		double maxZ = minZ + 1.0;
		double impactX = MathHelper.clamp(referencePos.x, minX, maxX);
		double impactY = MathHelper.clamp(referencePos.y, minY, maxY);
		double impactZ = MathHelper.clamp(referencePos.z, minZ, maxZ);
		switch (side) {
			case WEST -> impactX = minX;
			case EAST -> impactX = maxX;
			case DOWN -> impactY = minY;
			case UP -> impactY = maxY;
			case NORTH -> impactZ = minZ;
			case SOUTH -> impactZ = maxZ;
		}
		return new Vec3d(impactX, impactY, impactZ);
	}

	private static void spawnPlusUltraImpactParticles(ServerWorld world, PlusUltraImpactHit impact) {
		spawnVelocityImpactParticles(
			world,
			impact,
			PLUS_ULTRA_IMPACT_PARTICLE_COUNT,
			PLUS_ULTRA_IMPACT_PARTICLE_SPREAD,
			PLUS_ULTRA_IMPACT_PARTICLE_SPEED,
			PLUS_ULTRA_IMPACT_DUST_PARTICLE_COUNT,
			PLUS_ULTRA_IMPACT_DUST_PARTICLE_SPREAD,
			PLUS_ULTRA_IMPACT_DUST_PARTICLE_SPEED
		);
	}

	private static void spawnComedicAssistantCaneImpactParticles(ServerWorld world, PlusUltraImpactHit impact) {
		spawnVelocityImpactParticles(
			world,
			impact,
			Math.max(10, COMEDIC_ASSISTANT_GIANT_CANE_YANK.particleCount() * 2),
			0.46,
			0.12,
			Math.max(8, COMEDIC_ASSISTANT_GIANT_CANE_YANK.particleCount() + 4),
			0.34,
			0.06
		);
	}

	private static void spawnVelocityImpactParticles(
		ServerWorld world,
		PlusUltraImpactHit impact,
		int blockParticleCount,
		double blockParticleSpread,
		double blockParticleSpeed,
		int dustParticleCount,
		double dustParticleSpread,
		double dustParticleSpeed
	) {
		if (impact == null) {
			return;
		}

		BlockState particleState = impact.blockState().isAir() ? Blocks.DIRT.getDefaultState() : impact.blockState();
		Vec3d normal = Vec3d.of(impact.side().getVector());
		Vec3d emissionCenter = impact.position().add(normal.multiply(0.08));
		double impactX = emissionCenter.x;
		double impactY = emissionCenter.y;
		double impactZ = emissionCenter.z;
		double spreadX = impact.side().getAxis() == Direction.Axis.X ? 0.08 : blockParticleSpread;
		double spreadY = impact.side().getAxis() == Direction.Axis.Y ? 0.08 : Math.max(0.12, blockParticleSpread * 0.55);
		double spreadZ = impact.side().getAxis() == Direction.Axis.Z ? 0.08 : blockParticleSpread;
		world.spawnParticles(
			new BlockStateParticleEffect(ParticleTypes.BLOCK, particleState),
			impactX,
			impactY,
			impactZ,
			blockParticleCount,
			spreadX,
			spreadY,
			spreadZ,
			blockParticleSpeed
		);
		world.spawnParticles(
			ParticleTypes.CLOUD,
			impactX,
			impactY,
			impactZ,
			Math.max(4, blockParticleCount / 4),
			Math.max(0.04, spreadX * 0.85),
			Math.max(0.08, spreadY * 0.85),
			Math.max(0.04, spreadZ * 0.85),
			Math.max(0.02, blockParticleSpeed * 0.35)
		);
		world.spawnParticles(
			ParticleTypes.DUST_PLUME,
			impactX,
			impactY,
			impactZ,
			dustParticleCount,
			impact.side().getAxis() == Direction.Axis.X ? 0.08 : dustParticleSpread,
			impact.side().getAxis() == Direction.Axis.Y ? 0.08 : Math.max(0.12, dustParticleSpread * 0.45),
			impact.side().getAxis() == Direction.Axis.Z ? 0.08 : dustParticleSpread,
			dustParticleSpeed
		);
	}

	private static void updateComedicAssistantCarries(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, ComedicAssistantParrotCarryState>> iterator = COMEDIC_ASSISTANT_PARROT_CARRY_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ComedicAssistantParrotCarryState> entry = iterator.next();
			ComedicAssistantParrotCarryState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension());
			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
				iterator.remove();
				continue;
			}

			double remainingLift = Math.max(0.0, state.targetY() - target.getY());
			double upwardVelocity = Math.min(state.settings().upwardVelocity(), Math.max(0.45, remainingLift));
			double liftStep = Math.min(1.5, Math.max(0.2, upwardVelocity));
			if (
				currentTick >= state.endTick()
				|| target.getY() >= state.targetY() - 0.75
				|| !world.isSpaceEmpty(target, target.getBoundingBox().offset(0.0, liftStep, 0.0))
			) {
				releaseComedicAssistantCarry(target, state.settings());
				iterator.remove();
				continue;
			}

			Vec3d nextVelocity = new Vec3d(target.getVelocity().x * 0.18, upwardVelocity, target.getVelocity().z * 0.18);
			double nextY = Math.min(state.targetY(), target.getY() + upwardVelocity);
			forceTargetPositionAndVelocity(world, target, new Vec3d(target.getX(), nextY, target.getZ()), nextVelocity);
			target.fallDistance = 0.0F;
			if (state.settings().applyGlowing()) {
				refreshStatusEffect(target, StatusEffects.GLOWING, Math.max(2, state.settings().glowingDurationTicks()), 0, true, false, true);
			}
			if (currentTick >= state.nextParticleTick()) {
				spawnComedicAssistantItemParticles(
					world,
					new ItemStack(Items.FEATHER),
					new Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ()),
					state.settings().particleCount(),
					0.45,
					0.08
				);
				state.nextParticleTick(currentTick + 4);
			}
			if (currentTick >= state.nextSoundTick()) {
				playConfiguredSound(
					world,
					new Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ()),
					SoundEvents.ENTITY_PARROT_FLY,
					state.settings().soundVolume(),
					state.settings().soundPitch()
				);
				state.nextSoundTick(currentTick + state.settings().flapSoundIntervalTicks());
			}
		}
	}

	private static void releaseComedicAssistantCarry(LivingEntity target, ComedicAssistantParrotSettings settings) {
		double downwardVelocity = Math.max(0.0, settings.releaseDownwardVelocity());
		if (downwardVelocity <= 0.0) {
			return;
		}
		applyForcedVelocity(target, new Vec3d(target.getVelocity().x * 0.12, -downwardVelocity, target.getVelocity().z * 0.12));
	}

	private static double resolveComedicAssistantCarryTargetY(LivingEntity target, ServerWorld world, double liftHeight) {
		double maxLiftHeight = Math.max(0.0, liftHeight);
		double topLimit = world.getTopYInclusive() - 1.0;
		double startingY = target.getY();
		double bestY = startingY;
		Box box = target.getBoundingBox();
		for (double offset = 1.0; offset <= maxLiftHeight; offset += 1.0) {
			double candidateY = Math.min(topLimit, startingY + offset);
			if (candidateY <= bestY) {
				break;
			}
			if (!world.isSpaceEmpty(target, box.offset(0.0, candidateY - startingY, 0.0))) {
				break;
			}
			bestY = candidateY;
			if (candidateY >= topLimit) {
				break;
			}
		}
		return bestY;
	}

	private static List<ServerPlayerEntity> comedicRewriteVisualViewers(ServerWorld world, Vec3d origin) {
		List<ServerPlayerEntity> viewers = new ArrayList<>();
		double maxDistanceSquared = COMEDIC_REWRITE_VISUAL_VIEW_DISTANCE * COMEDIC_REWRITE_VISUAL_VIEW_DISTANCE;
		for (ServerPlayerEntity viewer : world.getPlayers()) {
			if (viewer.squaredDistanceTo(origin) <= maxDistanceSquared) {
				viewers.add(viewer);
			}
		}
		return viewers;
	}

	private static List<ServerPlayerEntity> comedicRewriteVisualViewers(MinecraftServer server, List<UUID> viewerIds) {
		List<ServerPlayerEntity> viewers = new ArrayList<>(viewerIds.size());
		for (UUID viewerId : viewerIds) {
			ServerPlayerEntity viewer = server.getPlayerManager().getPlayer(viewerId);
			if (viewer != null) {
				viewers.add(viewer);
			}
		}
		return viewers;
	}

	private static void queueComedicRewriteVisualCameo(
		MinecraftServer server,
		int durationTicks,
		List<ServerPlayerEntity> viewers,
		int chargeDurationTicks,
		ComedicRewriteVisualFollowState followState,
		Entity... entities
	) {
		queueComedicRewriteVisualCameo(server, durationTicks, viewers, chargeDurationTicks, followState, null, entities);
	}

	private static void queueComedicRewriteVisualCameo(
		MinecraftServer server,
		int durationTicks,
		List<ServerPlayerEntity> viewers,
		int chargeDurationTicks,
		ComedicRewriteVisualFollowState followState,
		ComedicRewriteVisualRotationState rotationState,
		Entity... entities
	) {
		if (server == null || durationTicks <= 0 || viewers.isEmpty() || entities.length == 0) {
			return;
		}

		int[] entityIds = new int[entities.length];
		for (int index = 0; index < entities.length; index++) {
			entityIds[index] = spawnPacketOnlyEntity(viewers, entities[index]);
		}

		List<UUID> viewerIds = new ArrayList<>(viewers.size());
		for (ServerPlayerEntity viewer : viewers) {
			viewerIds.add(viewer.getUuid());
		}
		int chargeEndTick = chargeDurationTicks > 0 && chargeDurationTicks < durationTicks ? server.getTicks() + chargeDurationTicks : 0;
		COMEDIC_REWRITE_VISUAL_CAMEOS.add(
			new ComedicRewriteVisualCameo(
				server.getTicks() + durationTicks,
				chargeEndTick,
				entityIds,
				List.copyOf(viewerIds),
				followState,
				rotationState,
				entities
			)
		);
	}

	private static int spawnPacketOnlyEntity(List<ServerPlayerEntity> viewers, Entity entity) {
		EntitySpawnS2CPacket spawnPacket = new EntitySpawnS2CPacket(
			entity.getId(),
			entity.getUuid(),
			entity.getX(),
			entity.getY(),
			entity.getZ(),
			entity.getPitch(),
			entity.getYaw(),
			entity.getType(),
			0,
			entity.getVelocity(),
			entity.getHeadYaw()
		);
		List<DataTracker.SerializedEntry<?>> trackedValues = entity.getDataTracker().getChangedEntries();
		EntityTrackerUpdateS2CPacket trackerPacket = trackedValues == null || trackedValues.isEmpty()
			? null
			: new EntityTrackerUpdateS2CPacket(entity.getId(), trackedValues);
		EntityVelocityUpdateS2CPacket velocityPacket = entity.getVelocity().lengthSquared() <= 1.0E-5
			? null
			: new EntityVelocityUpdateS2CPacket(entity.getId(), entity.getVelocity());

		for (ServerPlayerEntity viewer : viewers) {
			viewer.networkHandler.sendPacket(spawnPacket);
			if (trackerPacket != null) {
				viewer.networkHandler.sendPacket(trackerPacket);
			}
			if (velocityPacket != null) {
				viewer.networkHandler.sendPacket(velocityPacket);
			}
		}
		return entity.getId();
	}

	private static float horizontalYawFromDirection(Vec3d direction) {
		return MathHelper.wrapDegrees((float) (Math.toDegrees(MathHelper.atan2(direction.z, direction.x)) - 90.0));
	}

	private static ComedicRewriteOutcome selectComedicRewriteOutcome(
		ServerPlayerEntity player,
		boolean unsafeScene,
		Vec3d safePos
	) {
		if (unsafeScene && safePos == null) {
			return null;
		}
		if (unsafeScene) {
			return ComedicRewriteOutcome.PARROT_RESCUE;
		}

		int launchWeight = Math.max(0, COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.weight());
		int ravagerWeight = Math.max(0, COMEDIC_REWRITE_RAVAGER_BIT.weight());
		int parrotWeight = safePos != null ? Math.max(0, COMEDIC_REWRITE_PARROT_RESCUE.weight()) : 0;
		int totalWeight = launchWeight + ravagerWeight + parrotWeight;
		if (totalWeight <= 0) {
			return safePos != null ? ComedicRewriteOutcome.PARROT_RESCUE : ComedicRewriteOutcome.LAUNCHED_THROUGH_THE_SCENE;
		}

		int roll = player.getRandom().nextInt(totalWeight);
		if (roll < launchWeight) {
			return ComedicRewriteOutcome.LAUNCHED_THROUGH_THE_SCENE;
		}
		roll -= launchWeight;
		if (roll < ravagerWeight) {
			return ComedicRewriteOutcome.RAVAGER_BIT;
		}
		return ComedicRewriteOutcome.PARROT_RESCUE;
	}

	private static double comedicRewriteSeverity(float currentHealth, float finalDamage, boolean lethal) {
		double damageSeverity = COMEDIC_REWRITE_SEVERITY_DAMAGE_CAP <= COMEDIC_REWRITE_DANGEROUS_DAMAGE_THRESHOLD
			? 1.0
			: (finalDamage - COMEDIC_REWRITE_DANGEROUS_DAMAGE_THRESHOLD)
				/ (COMEDIC_REWRITE_SEVERITY_DAMAGE_CAP - COMEDIC_REWRITE_DANGEROUS_DAMAGE_THRESHOLD);
		double fractionSeverity = COMEDIC_REWRITE_DANGEROUS_HEALTH_FRACTION_THRESHOLD >= 1.0
			? 1.0
			: (finalDamage / Math.max(0.0001F, currentHealth) - COMEDIC_REWRITE_DANGEROUS_HEALTH_FRACTION_THRESHOLD)
				/ Math.max(0.0001, 1.0 - COMEDIC_REWRITE_DANGEROUS_HEALTH_FRACTION_THRESHOLD);
		double severity = Math.max(MathHelper.clamp(damageSeverity, 0.0, 1.0), MathHelper.clamp(fractionSeverity, 0.0, 1.0));
		if (lethal) {
			double lethalSeverity = (finalDamage - currentHealth) / Math.max(1.0F, currentHealth);
			severity = Math.max(severity, MathHelper.clamp(0.65 + lethalSeverity, 0.65, 1.0));
		}
		return MathHelper.clamp(severity, 0.0, 1.0);
	}

	private static float comedicRewriteSavedHealthPoints(double severity) {
		double hearts = MathHelper.lerp(severity, COMEDIC_REWRITE_MAX_SAVED_HEALTH_HEARTS, COMEDIC_REWRITE_MIN_SAVED_HEALTH_HEARTS);
		return Math.max(1.0F, (float) (MathHelper.clamp(hearts, COMEDIC_REWRITE_MIN_SAVED_HEALTH_HEARTS, COMEDIC_REWRITE_MAX_SAVED_HEALTH_HEARTS) * 2.0));
	}

	private static boolean isComedicRewriteUnsafeScene(ServerPlayerEntity player, ServerWorld world, DamageSource source) {
		return source.isOf(DamageTypes.OUT_OF_WORLD)
			|| source.isOf(DamageTypes.IN_WALL)
			|| source.isOf(DamageTypes.LAVA)
			|| source.isOf(DamageTypes.DROWN)
			|| source.isOf(DamageTypes.OUTSIDE_BORDER)
			|| player.isInsideWall()
			|| player.isInLava()
			|| player.getY() <= world.getBottomY() + COMEDIC_REWRITE_UNSAFE_Y_BUFFER_BLOCKS
			|| !world.getWorldBorder().contains(player.getBlockPos());
	}

	private static Vec3d findBestComedicRewriteSafePosition(ServerPlayerEntity player, ServerWorld world) {
		Vec3d safePos = findComedicRewriteSafePosition(
			player,
			world,
			COMEDIC_REWRITE_SAFE_SEARCH_RADIUS,
			COMEDIC_REWRITE_SAFE_SEARCH_VERTICAL_RANGE
		);
		if (safePos != null) {
			return safePos;
		}

		safePos = findComedicRewriteSafePosition(
			player,
			world,
			COMEDIC_REWRITE_SAFE_SEARCH_RADIUS * 2,
			COMEDIC_REWRITE_SAFE_SEARCH_VERTICAL_RANGE * 2
		);
		if (safePos != null) {
			return safePos;
		}

		return findComedicRewriteSafePosition(
			player,
			world,
			0,
			Math.max(COMEDIC_REWRITE_SAFE_SEARCH_VERTICAL_RANGE * 4, COMEDIC_REWRITE_UNSAFE_Y_BUFFER_BLOCKS + 16)
		);
	}

	private static Vec3d findComedicRewriteSafePosition(
		ServerPlayerEntity player,
		ServerWorld world,
		int radius,
		int verticalRange
	) {
		BlockPos origin = player.getBlockPos();
		int originY = MathHelper.clamp(origin.getY(), world.getBottomY() + 1, world.getTopYInclusive() - 1);

		for (int distance = 0; distance <= radius; distance++) {
			Vec3d sameColumn = findComedicRewriteSafePositionInColumn(player, world, origin.getX(), origin.getZ(), originY, verticalRange);
			if (distance == 0 && sameColumn != null) {
				return sameColumn;
			}
			if (distance == 0) {
				continue;
			}

			for (int dx = -distance; dx <= distance; dx++) {
				Vec3d north = findComedicRewriteSafePositionInColumn(
					player,
					world,
					origin.getX() + dx,
					origin.getZ() - distance,
					originY,
					verticalRange
				);
				if (north != null) {
					return north;
				}

				Vec3d south = findComedicRewriteSafePositionInColumn(
					player,
					world,
					origin.getX() + dx,
					origin.getZ() + distance,
					originY,
					verticalRange
				);
				if (south != null) {
					return south;
				}
			}

			for (int dz = -distance + 1; dz <= distance - 1; dz++) {
				Vec3d west = findComedicRewriteSafePositionInColumn(
					player,
					world,
					origin.getX() - distance,
					origin.getZ() + dz,
					originY,
					verticalRange
				);
				if (west != null) {
					return west;
				}

				Vec3d east = findComedicRewriteSafePositionInColumn(
					player,
					world,
					origin.getX() + distance,
					origin.getZ() + dz,
					originY,
					verticalRange
				);
				if (east != null) {
					return east;
				}
			}
		}

		return null;
	}

	private static Vec3d findComedicRewriteSafePositionInColumn(
		ServerPlayerEntity player,
		ServerWorld world,
		int x,
		int z,
		int originY,
		int verticalRange
	) {
		BlockPos chunkCheck = new BlockPos(x, originY, z);
		if (!world.isChunkLoaded(chunkCheck) || !world.getWorldBorder().contains(chunkCheck)) {
			return null;
		}

		int maxY = Math.min(world.getTopYInclusive() - 1, originY + verticalRange);
		int minY = Math.max(world.getBottomY() + 1, originY - verticalRange);
		for (int y = maxY; y >= minY; y--) {
			BlockPos feetPos = new BlockPos(x, y, z);
			if (isComedicRewriteSafePosition(player, world, feetPos)) {
				return new Vec3d(x + 0.5, y, z + 0.5);
			}
		}

		return null;
	}

	private static boolean isComedicRewriteSafePosition(ServerPlayerEntity player, ServerWorld world, BlockPos feetPos) {
		BlockPos belowPos = feetPos.down();
		BlockPos headPos = feetPos.up();
		BlockState belowState = world.getBlockState(belowPos);
		BlockState feetState = world.getBlockState(feetPos);
		BlockState headState = world.getBlockState(headPos);
		if (!belowState.blocksMovement()) {
			return false;
		}
		if (
			isComedicRewriteUnsafeBlock(belowState) ||
			isComedicRewriteUnsafeBlock(feetState) ||
			isComedicRewriteUnsafeBlock(headState)
		) {
			return false;
		}
		if (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()) {
			return false;
		}
		if (feetState.blocksMovement() || headState.blocksMovement()) {
			return false;
		}

		Vec3d targetPos = new Vec3d(feetPos.getX() + 0.5, feetPos.getY(), feetPos.getZ() + 0.5);
		Box targetBox = player.getBoundingBox().offset(targetPos.x - player.getX(), targetPos.y - player.getY(), targetPos.z - player.getZ());
		return world.isSpaceEmpty(player, targetBox) && !world.containsFluid(targetBox);
	}

	private static boolean isComedicRewriteUnsafeBlock(BlockState state) {
		return state.isOf(Blocks.LAVA)
			|| state.isOf(Blocks.FIRE)
			|| state.isOf(Blocks.SOUL_FIRE)
			|| state.isOf(Blocks.CAMPFIRE)
			|| state.isOf(Blocks.SOUL_CAMPFIRE)
			|| state.isOf(Blocks.CACTUS)
			|| state.isOf(Blocks.SWEET_BERRY_BUSH)
			|| state.isOf(Blocks.MAGMA_BLOCK);
	}

	private static void teleportComedicRewritePlayer(ServerPlayerEntity player, ServerWorld world, Vec3d targetPos) {
		if (player.hasVehicle()) {
			player.stopRiding();
		}

		player.teleport(world, targetPos.x, targetPos.y, targetPos.z, Set.<PositionFlag>of(), player.getYaw(), player.getPitch(), false);
		player.setOnGround(false);
		player.fallDistance = 0.0F;
	}

	private static Vec3d resolveComedicRewriteLaunchDirection(ServerPlayerEntity player, DamageSource source) {
		Vec3d sourcePos = source.getPosition();
		if (sourcePos != null) {
			Vec3d direction = new Vec3d(player.getX() - sourcePos.x, 0.0, player.getZ() - sourcePos.z);
			if (direction.lengthSquared() > 1.0E-5) {
				return direction.normalize();
			}
		}

		Vec3d backwards = player.getRotationVector().multiply(-1.0, 0.0, -1.0);
		if (backwards.lengthSquared() > 1.0E-5) {
			return backwards.normalize();
		}
		return randomHorizontalDirection(player);
	}

	private static Vec3d normalizedHorizontalDirection(Vec3d direction, ServerPlayerEntity player) {
		if (direction != null) {
			Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z);
			if (horizontal.lengthSquared() > 1.0E-5) {
				return horizontal.normalize();
			}
		}
		if (player != null) {
			return randomHorizontalDirection(player);
		}
		return new Vec3d(1.0, 0.0, 0.0);
	}

	private static Vec3d randomHorizontalDirection(ServerPlayerEntity player) {
		double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
		return new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
	}

	private static boolean shouldTriggerMartyrsFlameRetaliation(ServerPlayerEntity damagedPlayer) {
		UUID playerId = damagedPlayer.getUuid();
		return MARTYRS_FLAME_PASSIVE_ENABLED.contains(playerId)
			&& MagicPlayerData.getSchool(damagedPlayer) == MagicSchool.BURNING_PASSION
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(playerId, MagicAbility.MARTYRS_FLAME)
			&& (MagicPlayerData.getMana(damagedPlayer) > 0 || isOrionsGambitManaCostSuppressed(damagedPlayer));
	}

	private static ServerPlayerEntity attackingPlayerFrom(DamageSource source) {
		Entity attacker = source.getAttacker();
		if (attacker instanceof ServerPlayerEntity attackerPlayer && attackerPlayer.isAlive() && !attackerPlayer.isSpectator()) {
			return attackerPlayer;
		}

		if (source.getSource() instanceof ServerPlayerEntity sourcePlayer && sourcePlayer.isAlive() && !sourcePlayer.isSpectator()) {
			return sourcePlayer;
		}

		return null;
	}

	private static void retaliateWithMartyrsFlame(ServerPlayerEntity defender, ServerPlayerEntity attacker, int currentTick) {
		if (MARTYRS_FLAME_RETALIATION_DAMAGE > 0.0F && attacker.getEntityWorld() instanceof ServerWorld world) {
			dealTrackedMagicDamage(attacker, defender.getUuid(), world.getDamageSources().genericKill(), MARTYRS_FLAME_RETALIATION_DAMAGE);
		}

		applyMartyrsFlameFire(attacker, currentTick);
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
		recordOrionsGambitAbilityUse(caster, MagicAbility.TILL_DEATH_DO_US_PART);
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

	private static SpotlightStageSettings spotlightStageSettings(MagicConfig.JesterSpotlightStageConfig config) {
		return SpotlightStageSettings.defaults(
			Math.max(1, config.viewersRequired),
			Math.max(1, config.activationWindowTicks),
			Math.max(0, config.downgradeGraceTicks),
			Math.max(1, config.fallbackWindowTicks),
			Math.max(0.0, config.attackDamageBonus),
			Math.max(0.0, config.movementSpeedMultiplier),
			Math.max(1.0, config.scale),
			Math.max(-1, config.jumpBoostAmplifier),
			Math.max(-1, config.resistanceAmplifier),
			Math.max(0.0, config.maxHealthBonusHearts)
		);
	}

	private static WittyOneLinerTierSettings wittyOneLinerTierSettings(
		MagicConfig.JesterWittyOneLinerTierConfig config,
		int fallbackColor
	) {
		List<String> jokes = new ArrayList<>(config.jokes);
		jokes.removeIf(joke -> joke == null || joke.isBlank());
		if (jokes.isEmpty()) {
			jokes = List.of(MagicAbility.WITTY_ONE_LINER.displayName().getString());
		}

		return WittyOneLinerTierSettings.defaults(
			Math.max(0, config.selectionWeight),
			Math.max(0, config.cooldownTicks),
			Math.max(0, config.effectDurationTicks),
			parseColorRgb(config.textColorHex, fallbackColor),
			Math.max(-1, config.slownessAmplifier),
			Math.max(-1, config.weaknessAmplifier),
			Math.max(-1, config.blindnessAmplifier),
			Math.max(-1, config.nauseaAmplifier),
			Math.max(-1, config.darknessAmplifier),
			Math.max(-1, config.weavingAmplifier),
			config.applyGlowing,
			List.copyOf(jokes)
		);
	}

	private static ComedicRewriteLaunchSettings comedicRewriteLaunchSettings(MagicConfig.JesterComedicRewriteLaunchConfig config) {
		return ComedicRewriteLaunchSettings.defaults(
			Math.max(0, config.weight),
			Math.max(0.0, config.baseHorizontalVelocity),
			Math.max(0.0, config.horizontalVelocityPerSeverity),
			Math.max(0.0, config.baseVerticalVelocity),
			Math.max(0.0, config.verticalVelocityPerSeverity),
			Math.max(0, config.slownessDurationTicks),
			Math.max(-1, config.slownessAmplifier),
			Math.max(0, config.smokeParticleCount),
			Math.max(0, config.poofParticleCount)
		);
	}

	private static ComedicRewriteRavagerSettings comedicRewriteRavagerSettings(MagicConfig.JesterComedicRewriteRavagerConfig config) {
		return ComedicRewriteRavagerSettings.defaults(
			Math.max(0, config.weight),
			Math.max(0.0, config.baseHorizontalVelocity),
			Math.max(0.0, config.horizontalVelocityPerSeverity),
			Math.max(0.0, config.baseVerticalVelocity),
			Math.max(0.0, config.verticalVelocityPerSeverity),
			Math.max(0, config.slownessDurationTicks),
			Math.max(-1, config.slownessAmplifier),
			Math.max(0, config.smokeParticleCount),
			Math.max(0, config.dustParticleCount),
			config.showVisualCameo,
			Math.max(0, config.visualDurationTicks),
			Math.max(0.0, config.visualSpawnDistance),
			config.visualVerticalOffset,
			Math.max(0.0, config.visualChargeVelocity),
			Math.max(0.0, config.visualChargeVelocityBuffer),
			Math.max(0, config.visualChargeDurationTicks)
		);
	}

	private static ComedicRewriteParrotSettings comedicRewriteParrotSettings(MagicConfig.JesterComedicRewriteParrotConfig config) {
		return ComedicRewriteParrotSettings.defaults(
			Math.max(0, config.weight),
			Math.max(0.0, config.carryHeight),
			Math.max(0.0, config.carryHeightPerSeverity),
			Math.max(0.0, config.sideVelocity),
			Math.max(0.0, config.sideVelocityPerSeverity),
			Math.max(0, config.slowFallingDurationTicks),
			Math.max(0, config.levitationDurationTicks),
			Math.max(0, config.featherParticleCount),
			config.showVisualCameo,
			Math.max(0, config.visualDurationTicks),
			Math.max(0, config.visualCount),
			Math.max(0.0, config.visualRadius),
			config.visualVerticalOffset,
			config.visualFollowPlayerHead,
			Math.max(0.0, config.visualLiftVelocity)
		);
	}

	private static ComedicAssistantSlimeSettings comedicAssistantSlimeSettings(MagicConfig.JesterComedicAssistantSlimeConfig config) {
		return ComedicAssistantSlimeSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0, config.slownessDurationTicks),
			Math.max(-1, config.slownessAmplifier),
			Math.max(0, config.oozingDurationTicks),
			Math.max(-1, config.oozingAmplifier),
			Math.max(0, config.visualDurationTicks),
			Math.max(0.0, config.visualSpawnHeight),
			Math.max(0.0, config.visualFallSpeed),
			MathHelper.clamp(config.visualSize, 1, 127),
			Math.max(0, config.particleCount),
			Math.max(0.0F, config.spawnSoundVolume),
			Math.max(0.0F, config.spawnSoundPitch),
			Math.max(0.0F, config.impactSoundVolume),
			Math.max(0.0F, config.impactSoundPitch)
		);
	}

	private static ComedicAssistantPandaSettings comedicAssistantPandaSettings(MagicConfig.JesterComedicAssistantPandaConfig config) {
		return ComedicAssistantPandaSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0.0, config.horizontalLaunch),
			Math.max(0.0, config.verticalLaunch),
			Math.max(0, config.slownessDurationTicks),
			Math.max(-1, config.slownessAmplifier),
			Math.max(0, config.visualDurationTicks),
			Math.max(0.0, config.visualSpawnDistance),
			Math.max(0.0, config.visualChargeVelocity),
			Math.max(0, config.particleCount),
			Math.max(0.0F, config.rollSoundVolume),
			Math.max(0.0F, config.rollSoundPitch),
			Math.max(0.0F, config.impactSoundVolume),
			Math.max(0.0F, config.impactSoundPitch)
		);
	}

	private static ComedicAssistantParrotSettings comedicAssistantParrotSettings(MagicConfig.JesterComedicAssistantParrotConfig config) {
		return ComedicAssistantParrotSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0.0, config.liftHeight),
			Math.max(0.0, config.upwardVelocity),
			Math.max(0, config.maxCarryTicks),
			Math.max(0.0, config.releaseDownwardVelocity),
			config.applyGlowing,
			Math.max(0, config.glowingDurationTicks),
			Math.max(0, config.visualDurationTicks),
			Math.max(0, config.visualCount),
			Math.max(0.0, config.visualRadius),
			config.visualVerticalOffset,
			Math.max(0, config.particleCount),
			Math.max(1, config.flapSoundIntervalTicks),
			Math.max(0.0F, config.soundVolume),
			Math.max(0.0F, config.soundPitch)
		);
	}

	private static ComedicAssistantDivineSettings comedicAssistantDivineSettings(MagicConfig.JesterComedicAssistantDivineConfig config) {
		return ComedicAssistantDivineSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0, config.strikeCount),
			Math.max(0.0, config.strikeRadius),
			Math.max(0, config.glowingDurationTicks),
			Math.max(0, config.blindnessDurationTicks),
			Math.max(-1, config.blindnessAmplifier),
			Math.max(0, config.nauseaDurationTicks),
			Math.max(-1, config.nauseaAmplifier),
			Math.max(0, config.particleCount),
			Math.max(0.0F, config.soundVolume),
			Math.max(0.0F, config.soundPitch)
		);
	}

	private static ComedicAssistantAcmeSettings comedicAssistantAcmeSettings(MagicConfig.JesterComedicAssistantAcmeConfig config) {
		return ComedicAssistantAcmeSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0, config.slownessDurationTicks),
			Math.max(-1, config.slownessAmplifier),
			Math.max(0, config.weaknessDurationTicks),
			Math.max(-1, config.weaknessAmplifier),
			Math.max(0, config.visualDurationTicks),
			Math.max(0.0, config.visualDropHeight),
			Math.max(0, config.particleCount),
			Math.max(0.0F, config.soundVolume),
			Math.max(0.0F, config.soundPitch)
		);
	}

	private static ComedicAssistantPieSettings comedicAssistantPieSettings(MagicConfig.JesterComedicAssistantPieConfig config) {
		return ComedicAssistantPieSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0, config.blindnessDurationTicks),
			Math.max(-1, config.blindnessAmplifier),
			Math.max(0, config.nauseaDurationTicks),
			Math.max(-1, config.nauseaAmplifier),
			Math.max(0, config.particleCount),
			Math.max(0.0F, config.soundVolume),
			Math.max(0.0F, config.soundPitch)
		);
	}

	private static ComedicAssistantCaneSettings comedicAssistantCaneSettings(MagicConfig.JesterComedicAssistantCaneConfig config) {
		return ComedicAssistantCaneSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0.0, config.horizontalLaunch),
			Math.max(0.0, config.verticalLaunch),
			Math.max(0, config.launchControlTicks),
			Math.max(0, config.velocityDamageTrackingTicks),
			Math.max(0.0, config.velocityDamageThreshold),
			Math.max(0.0, config.velocityDamageMultiplier),
			Math.max(0.0, config.velocityDamageMax),
			Math.max(0, config.slownessDurationTicks),
			Math.max(-1, config.slownessAmplifier),
			Math.max(0, config.visualDurationTicks),
			Math.max(0.0, config.visualSpawnDistance),
			Math.max(0.0, config.visualChargeVelocity),
			Math.max(0, config.particleCount),
			Math.max(0.0F, config.soundVolume),
			Math.max(0.0F, config.soundPitch)
		);
	}

	private static int parseColorRgb(String rawColor, int fallbackColor) {
		if (rawColor == null || rawColor.isBlank()) {
			return fallbackColor;
		}

		String normalized = rawColor.trim();
		if (normalized.startsWith("#")) {
			normalized = normalized.substring(1);
		}
		if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
			normalized = normalized.substring(2);
		}

		try {
			return Integer.parseInt(normalized, 16) & 0x00FFFFFF;
		} catch (NumberFormatException exception) {
			Magic.LOGGER.warn("Invalid Jester color '{}'; using fallback {}.", rawColor, fallbackColor);
			return fallbackColor;
		}
	}

	private static Identifier parseIdentifierOrFallback(String rawId, Identifier fallbackId) {
		if (rawId == null || rawId.isBlank()) {
			return fallbackId;
		}

		Identifier parsed = Identifier.tryParse(rawId.trim());
		if (parsed == null) {
			Magic.LOGGER.warn("Invalid identifier '{}'; using fallback {}.", rawId, fallbackId);
			return fallbackId;
		}

		return parsed;
	}

	private static boolean shouldKeepSpotlightEnabled(ServerPlayerEntity player) {
		return MagicPlayerData.hasMagic(player)
			&& MagicPlayerData.getSchool(player) == MagicSchool.JESTER
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.SPOTLIGHT);
	}

	private static boolean shouldKeepComedicRewriteEnabled(ServerPlayerEntity player) {
		return MagicPlayerData.hasMagic(player)
			&& MagicPlayerData.getSchool(player) == MagicSchool.JESTER
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.COMEDIC_REWRITE);
	}

	private static boolean shouldKeepComedicAssistantEnabled(ServerPlayerEntity player) {
		return MagicPlayerData.hasMagic(player)
			&& MagicPlayerData.getSchool(player) == MagicSchool.JESTER
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.COMEDIC_ASSISTANT);
	}

	private static void updateComedicAssistantState(ServerPlayerEntity player, int currentTick) {
		ComedicAssistantArmedState state = COMEDIC_ASSISTANT_ARMED_STATES.get(player.getUuid());
		if (state == null) {
			if (COMEDIC_ASSISTANT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), Integer.MIN_VALUE) <= currentTick) {
				COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(player.getUuid());
			}
			return;
		}

		if (!player.isAlive() || !shouldKeepComedicAssistantEnabled(player) || player.getEntityWorld().getRegistryKey() != state.dimension()) {
			clearComedicAssistantState(player.getUuid());
			return;
		}

		if (currentTick >= state.expiresTick()) {
			deactivateComedicAssistant(player, currentTick, COMEDIC_ASSISTANT_CANCEL_COOLDOWN_TICKS, false, true);
			return;
		}

		if (currentTick >= state.nextIndicatorTick()) {
			sendComedicAssistantArmedIndicator(player);
			spawnComedicAssistantArmedParticles(player);
			COMEDIC_ASSISTANT_ARMED_STATES.put(
				player.getUuid(),
				new ComedicAssistantArmedState(state.dimension(), state.expiresTick(), currentTick + COMEDIC_ASSISTANT_ARMED_INDICATOR_REFRESH_TICKS)
			);
		}

		if (COMEDIC_ASSISTANT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), Integer.MIN_VALUE) <= currentTick) {
			COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(player.getUuid());
		}
	}

	private static void sendComedicAssistantArmedIndicator(ServerPlayerEntity player) {
		player.sendMessage(Text.translatable("message.magic.jester.comedic_assistant.ready"), true);
	}

	private static void spawnComedicAssistantArmedParticles(ServerPlayerEntity player) {
		if (!(player.getEntityWorld() instanceof ServerWorld world) || COMEDIC_ASSISTANT_ARMED_PARTICLE_COUNT <= 0) {
			return;
		}

		world.spawnParticles(
			ParticleTypes.GLOW,
			player.getX(),
			player.getBodyY(0.75),
			player.getZ(),
			COMEDIC_ASSISTANT_ARMED_PARTICLE_COUNT,
			0.3,
			0.25,
			0.3,
			0.02
		);
	}

	private static void applySpotlight(ServerPlayerEntity player, int currentTick) {
		UUID playerId = player.getUuid();
		SpotlightState state = SPOTLIGHT_STATES.computeIfAbsent(playerId, ignored -> new SpotlightState());
		if (!player.isAlive()) {
			resetSpotlightTracking(player);
			return;
		}

		if (SPOTLIGHT_MAX_HISTORY_TICKS > 0) {
			state.viewerLastLookTicks().entrySet().removeIf(entry -> currentTick - entry.getValue() > SPOTLIGHT_MAX_HISTORY_TICKS);
		}

		int currentViewers = 0;
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server != null) {
			for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
				if (!isSpotlightViewerLookingAtTarget(viewer, player)) {
					continue;
				}

				state.viewerLastLookTicks().put(viewer.getUuid(), currentTick);
				currentViewers++;
			}
		}

		int activationStage = highestSpotlightActivationStage(state, currentTick);
		if (activationStage > state.currentStage()) {
			state.currentStage(activationStage);
			state.downgradeDeadlineTick(-1);
		}

		if (state.currentStage() > 0) {
			SpotlightStageSettings currentSettings = spotlightStageSettingsFor(state.currentStage());
			if (currentViewers >= currentSettings.viewersRequired()) {
				state.downgradeDeadlineTick(-1);
			} else if (state.downgradeDeadlineTick() < 0) {
				state.downgradeDeadlineTick(currentTick + currentSettings.downgradeGraceTicks());
			} else if (currentTick >= state.downgradeDeadlineTick()) {
				state.currentStage(resolveSpotlightFallbackStage(state, currentTick, state.currentStage() - 1));
				state.downgradeDeadlineTick(-1);
			}
		}

		applySpotlightBuffs(player, state);
	}

	private static boolean isSpotlightViewerLookingAtTarget(ServerPlayerEntity viewer, ServerPlayerEntity target) {
		return viewer != target
			&& viewer.isAlive()
			&& !viewer.isSpectator()
			&& viewer.getEntityWorld() == target.getEntityWorld()
			&& findLivingTargetInLineOfSight(viewer, SPOTLIGHT_DETECTION_RANGE) == target;
	}

	private static int highestSpotlightActivationStage(SpotlightState state, int currentTick) {
		if (countRecentSpotlightViewers(state, currentTick, SPOTLIGHT_STAGE_THREE.activationWindowTicks()) >= SPOTLIGHT_STAGE_THREE.viewersRequired()) {
			return 3;
		}
		if (countRecentSpotlightViewers(state, currentTick, SPOTLIGHT_STAGE_TWO.activationWindowTicks()) >= SPOTLIGHT_STAGE_TWO.viewersRequired()) {
			return 2;
		}
		if (countRecentSpotlightViewers(state, currentTick, SPOTLIGHT_STAGE_ONE.activationWindowTicks()) >= SPOTLIGHT_STAGE_ONE.viewersRequired()) {
			return 1;
		}
		return 0;
	}

	private static int resolveSpotlightFallbackStage(SpotlightState state, int currentTick, int maxStage) {
		for (int stage = Math.max(0, maxStage); stage >= 1; stage--) {
			SpotlightStageSettings settings = spotlightStageSettingsFor(stage);
			if (countRecentSpotlightViewers(state, currentTick, settings.fallbackWindowTicks()) >= settings.viewersRequired()) {
				return stage;
			}
		}
		return 0;
	}

	private static int countRecentSpotlightViewers(SpotlightState state, int currentTick, int windowTicks) {
		int earliestTick = currentTick - Math.max(0, windowTicks) + 1;
		int viewers = 0;
		for (int lastLookTick : state.viewerLastLookTicks().values()) {
			if (lastLookTick >= earliestTick) {
				viewers++;
			}
		}
		return viewers;
	}

	private static void applySpotlightBuffs(ServerPlayerEntity player, SpotlightState state) {
		SpotlightStageSettings stageSettings = spotlightStageSettingsFor(state.currentStage());
		if (stageSettings == null) {
			removeSpotlightBuffs(player, state);
			return;
		}

		overwriteAttributeModifier(
			player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE),
			SPOTLIGHT_ATTACK_DAMAGE_MODIFIER_ID,
			stageSettings.attackDamageBonus(),
			EntityAttributeModifier.Operation.ADD_VALUE
		);
		overwriteAttributeModifier(
			player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED),
			SPOTLIGHT_MOVEMENT_SPEED_MODIFIER_ID,
			stageSettings.movementSpeedMultiplier(),
			EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);
		overwriteAttributeModifier(
			player.getAttributeInstance(EntityAttributes.MAX_HEALTH),
			SPOTLIGHT_MAX_HEALTH_MODIFIER_ID,
			stageSettings.maxHealthBonusPoints(),
			EntityAttributeModifier.Operation.ADD_VALUE
		);
		overwriteAttributeModifier(
			player.getAttributeInstance(EntityAttributes.SCALE),
			SPOTLIGHT_SCALE_MODIFIER_ID,
			stageSettings.scale() - 1.0,
			EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);

		if (stageSettings.jumpBoostAmplifier() >= 0) {
			refreshStatusEffect(player, StatusEffects.JUMP_BOOST, SPOTLIGHT_EFFECT_REFRESH_TICKS, stageSettings.jumpBoostAmplifier(), true, false, true);
		}
		if (stageSettings.resistanceAmplifier() >= 0) {
			refreshStatusEffect(player, StatusEffects.RESISTANCE, SPOTLIGHT_EFFECT_REFRESH_TICKS, stageSettings.resistanceAmplifier(), true, false, true);
		}

		double newBonusPoints = stageSettings.maxHealthBonusPoints();
		double previousBonusPoints = state.appliedMaxHealthBonusPoints();
		if (newBonusPoints > previousBonusPoints) {
			player.setHealth((float) Math.min(player.getMaxHealth(), player.getHealth() + (newBonusPoints - previousBonusPoints)));
		} else if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		state.appliedMaxHealthBonusPoints(newBonusPoints);
	}

	private static void overwriteAttributeModifier(
		EntityAttributeInstance attributeInstance,
		Identifier modifierId,
		double value,
		EntityAttributeModifier.Operation operation
	) {
		if (attributeInstance == null) {
			return;
		}

		EntityAttributeModifier existing = attributeInstance.getModifier(modifierId);
		if (value <= 0.0) {
			if (existing != null) {
				attributeInstance.removeModifier(modifierId);
			}
			return;
		}

		if (existing != null && existing.operation() == operation && Math.abs(existing.value() - value) <= 1.0E-6) {
			return;
		}

		if (existing != null) {
			attributeInstance.removeModifier(modifierId);
		}

		attributeInstance.addTemporaryModifier(new EntityAttributeModifier(modifierId, value, operation));
	}

	private static void setSignedAttributeModifier(
		EntityAttributeInstance attributeInstance,
		Identifier modifierId,
		double value,
		EntityAttributeModifier.Operation operation
	) {
		if (attributeInstance == null) {
			return;
		}

		EntityAttributeModifier existing = attributeInstance.getModifier(modifierId);
		if (Math.abs(value) <= 1.0E-6) {
			if (existing != null) {
				attributeInstance.removeModifier(modifierId);
			}
			return;
		}

		if (existing != null && existing.operation() == operation && Math.abs(existing.value() - value) <= 1.0E-6) {
			return;
		}

		if (existing != null) {
			attributeInstance.removeModifier(modifierId);
		}

		attributeInstance.addTemporaryModifier(new EntityAttributeModifier(modifierId, value, operation));
	}

	private static void removeSpotlightBuffs(ServerPlayerEntity player, SpotlightState state) {
		removeAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), SPOTLIGHT_ATTACK_DAMAGE_MODIFIER_ID);
		removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), SPOTLIGHT_MOVEMENT_SPEED_MODIFIER_ID);
		removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), SPOTLIGHT_MAX_HEALTH_MODIFIER_ID);
		removeAttributeModifier(player.getAttributeInstance(EntityAttributes.SCALE), SPOTLIGHT_SCALE_MODIFIER_ID);
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		state.appliedMaxHealthBonusPoints(0.0);
	}

	private static void removeAttributeModifier(EntityAttributeInstance attributeInstance, Identifier modifierId) {
		if (attributeInstance != null) {
			attributeInstance.removeModifier(modifierId);
		}
	}

	private static void resetSpotlightTracking(ServerPlayerEntity player) {
		SpotlightState state = SPOTLIGHT_STATES.get(player.getUuid());
		if (state == null) {
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), SPOTLIGHT_ATTACK_DAMAGE_MODIFIER_ID);
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), SPOTLIGHT_MOVEMENT_SPEED_MODIFIER_ID);
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), SPOTLIGHT_MAX_HEALTH_MODIFIER_ID);
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.SCALE), SPOTLIGHT_SCALE_MODIFIER_ID);
			if (player.getHealth() > player.getMaxHealth()) {
				player.setHealth(player.getMaxHealth());
			}
			return;
		}

		state.viewerLastLookTicks().clear();
		state.currentStage(0);
		state.downgradeDeadlineTick(-1);
		removeSpotlightBuffs(player, state);
	}

	private static void deactivateSpotlight(ServerPlayerEntity player, boolean disablePassive) {
		if (disablePassive) {
			SPOTLIGHT_PASSIVE_ENABLED.remove(player.getUuid());
		}

		SpotlightState state = SPOTLIGHT_STATES.get(player.getUuid());
		if (state == null) {
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), SPOTLIGHT_ATTACK_DAMAGE_MODIFIER_ID);
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), SPOTLIGHT_MOVEMENT_SPEED_MODIFIER_ID);
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), SPOTLIGHT_MAX_HEALTH_MODIFIER_ID);
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.SCALE), SPOTLIGHT_SCALE_MODIFIER_ID);
			if (player.getHealth() > player.getMaxHealth()) {
				player.setHealth(player.getMaxHealth());
			}
			return;
		}

		state.viewerLastLookTicks().clear();
		state.currentStage(0);
		state.downgradeDeadlineTick(-1);
		removeSpotlightBuffs(player, state);
		if (disablePassive) {
			SPOTLIGHT_STATES.remove(player.getUuid());
		}
	}

	private static void deactivateComedicRewrite(ServerPlayerEntity player) {
		COMEDIC_REWRITE_PASSIVE_ENABLED.remove(player.getUuid());
	}

	private static SpotlightStageSettings spotlightStageSettingsFor(int stage) {
		return switch (stage) {
			case 1 -> SPOTLIGHT_STAGE_ONE;
			case 2 -> SPOTLIGHT_STAGE_TWO;
			case 3 -> SPOTLIGHT_STAGE_THREE;
			default -> null;
		};
	}

	private static int wittyOneLinerCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, WITTY_ONE_LINER_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int comedicRewriteCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, COMEDIC_REWRITE_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int comedicAssistantCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, COMEDIC_ASSISTANT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int plusUltraCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, PLUS_ULTRA_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static void startComedicRewriteCooldown(UUID playerId, int currentTick) {
		if (isCooldownDeferredByOrionsGambit(playerId, MagicAbility.COMEDIC_REWRITE) || COMEDIC_REWRITE_COOLDOWN_TICKS <= 0) {
			COMEDIC_REWRITE_COOLDOWN_END_TICK.remove(playerId);
			return;
		}
		COMEDIC_REWRITE_COOLDOWN_END_TICK.put(playerId, currentTick + COMEDIC_REWRITE_COOLDOWN_TICKS);
	}

	private static void startComedicAssistantCooldown(UUID playerId, int currentTick, int cooldownTicks) {
		int safeCooldownTicks = Math.max(0, cooldownTicks);
		if (isCooldownDeferredByOrionsGambit(playerId, MagicAbility.COMEDIC_ASSISTANT)) {
			trackOrionsGambitCooldownOverride(playerId, MagicAbility.COMEDIC_ASSISTANT, safeCooldownTicks);
			COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(playerId);
			return;
		}
		if (safeCooldownTicks <= 0) {
			COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(playerId);
			return;
		}
		COMEDIC_ASSISTANT_COOLDOWN_END_TICK.put(playerId, currentTick + safeCooldownTicks);
	}

	private static void startPlusUltraCooldown(UUID playerId, int currentTick, int cooldownTicks) {
		int safeCooldownTicks = Math.max(0, cooldownTicks);
		if (isCooldownDeferredByOrionsGambit(playerId, MagicAbility.PLUS_ULTRA)) {
			trackOrionsGambitCooldownOverride(playerId, MagicAbility.PLUS_ULTRA, safeCooldownTicks);
			PLUS_ULTRA_COOLDOWN_END_TICK.remove(playerId);
			return;
		}
		if (safeCooldownTicks <= 0) {
			PLUS_ULTRA_COOLDOWN_END_TICK.remove(playerId);
			return;
		}
		PLUS_ULTRA_COOLDOWN_END_TICK.put(playerId, currentTick + safeCooldownTicks);
	}

	private static boolean hasActiveComedicRewriteImmunity(UUID playerId, int currentTick) {
		int endTick = COMEDIC_REWRITE_IMMUNITY_END_TICK.getOrDefault(playerId, 0);
		if (endTick <= currentTick) {
			COMEDIC_REWRITE_IMMUNITY_END_TICK.remove(playerId);
			return false;
		}
		return true;
	}

	private static boolean hasActiveComedicRewriteFallProtection(UUID playerId, int currentTick) {
		int endTick = COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.getOrDefault(playerId, 0);
		if (endTick <= currentTick) {
			COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.remove(playerId);
			return false;
		}
		return true;
	}

	private static WittyOneLinerTierSettings selectWittyOneLinerTier(ServerPlayerEntity player) {
		int lowWeight = Math.max(0, WITTY_ONE_LINER_LOW_TIER.selectionWeight());
		int midWeight = Math.max(0, WITTY_ONE_LINER_MID_TIER.selectionWeight());
		int highWeight = Math.max(0, WITTY_ONE_LINER_HIGH_TIER.selectionWeight());
		int totalWeight = lowWeight + midWeight + highWeight;
		if (totalWeight <= 0) {
			return WITTY_ONE_LINER_LOW_TIER;
		}

		int roll = player.getRandom().nextInt(totalWeight);
		if (roll < lowWeight) {
			return WITTY_ONE_LINER_LOW_TIER;
		}
		roll -= lowWeight;
		if (roll < midWeight) {
			return WITTY_ONE_LINER_MID_TIER;
		}
		return WITTY_ONE_LINER_HIGH_TIER;
	}

	private static String selectRandomJoke(ServerPlayerEntity player, List<String> jokes) {
		if (jokes == null || jokes.isEmpty()) {
			return null;
		}

		return jokes.get(player.getRandom().nextInt(jokes.size()));
	}

	private static void applyWittyOneLinerEffects(ServerPlayerEntity target, WittyOneLinerTierSettings settings) {
		addConfiguredStatusEffect(target, StatusEffects.SLOWNESS, settings.effectDurationTicks(), settings.slownessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.WEAKNESS, settings.effectDurationTicks(), settings.weaknessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.BLINDNESS, settings.effectDurationTicks(), settings.blindnessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.NAUSEA, settings.effectDurationTicks(), settings.nauseaAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.DARKNESS, settings.effectDurationTicks(), settings.darknessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.WEAVING, settings.effectDurationTicks(), settings.weavingAmplifier());
		if (settings.applyGlowing()) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, settings.effectDurationTicks(), 0, true, true, true));
		}
	}

	private static void addConfiguredStatusEffect(
		LivingEntity target,
		net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect,
		int durationTicks,
		int amplifier
	) {
		if (durationTicks <= 0 || amplifier < 0) {
			return;
		}

		target.addStatusEffect(new StatusEffectInstance(effect, durationTicks, amplifier, true, true, true));
	}

	private static void sendWittyOneLinerOverlay(ServerPlayerEntity caster, ServerPlayerEntity target, String joke, int colorRgb) {
		JesterJokeOverlayPayload payload = new JesterJokeOverlayPayload(
			joke,
			colorRgb,
			WITTY_ONE_LINER_OVERLAY_FADE_IN_TICKS,
			WITTY_ONE_LINER_OVERLAY_STAY_TICKS,
			WITTY_ONE_LINER_OVERLAY_FADE_OUT_TICKS
		);
		ServerPlayNetworking.send(caster, payload);
		if (target != caster) {
			ServerPlayNetworking.send(target, payload);
		}
	}

	private static boolean canSpendAbilityCost(ServerPlayerEntity player, int manaCost) {
		return isTestingMode(player) || isOrionsGambitManaCostSuppressed(player) || MagicPlayerData.getMana(player) >= Math.max(0, manaCost);
	}

	private static void spendAbilityCost(ServerPlayerEntity player, int manaCost) {
		if (isTestingMode(player)) {
			MagicPlayerData.setMana(player, MagicPlayerData.MAX_MANA);
			MagicPlayerData.setDepletedRecoveryMode(player, false);
			return;
		}

		if (!isOrionsGambitManaCostSuppressed(player) && manaCost > 0) {
			MagicPlayerData.setMana(player, Math.max(0, MagicPlayerData.getMana(player) - manaCost));
		}
		MagicPlayerData.setDepletedRecoveryMode(player, false);
	}

	private static boolean isTestingMode(ServerPlayerEntity player) {
		return player != null && TEST_MODE_PLAYERS.contains(player.getUuid());
	}

	public static boolean setTestingMode(ServerPlayerEntity player, boolean enabled) {
		if (player == null) {
			return false;
		}

		boolean changed = enabled ? TEST_MODE_PLAYERS.add(player.getUuid()) : TEST_MODE_PLAYERS.remove(player.getUuid());
		if (enabled) {
			MagicPlayerData.setMana(player, MagicPlayerData.MAX_MANA);
			MagicPlayerData.setDepletedRecoveryMode(player, false);
		}
		resetAllCooldowns(player);
		return changed;
	}

	public static boolean setMagicSchool(ServerPlayerEntity player, MagicSchool school) {
		if (player == null || school == null || !school.isMagic()) {
			return false;
		}

		clearAllRuntimeState(player);
		MagicPlayerData.clear(player);
		MagicPlayerData.unlock(player, school);
		MagicPlayerData.refillMana(player);
		return true;
	}

	public static int setFrostStage(ServerPlayerEntity player, int stage) {
		FrostStageState state = ensureAdminFrostStageState(player);
		if (state == null) {
			return 0;
		}

		int clampedStage = MathHelper.clamp(stage, 1, 3);
		state.currentStage = clampedStage;
		state.highestUnlockedStage = clampedStage;
		state.progressTicks = 0;
		syncFrostStageHud(player);
		return 1;
	}

	public static int clearFrostStage(ServerPlayerEntity player) {
		if (player == null || MagicPlayerData.getSchool(player) != MagicSchool.FROST || player.getEntityWorld().getServer() == null) {
			return 0;
		}

		endFrostStagedMode(player, player.getEntityWorld().getServer().getTicks(), FrostStageEndReason.CLEAR_ALL, false, false);
		return 1;
	}

	public static int advanceFrostStageForTesting(ServerPlayerEntity player) {
		FrostStageState state = ensureAdminFrostStageState(player);
		if (state == null || state.currentStage >= 3) {
			return 0;
		}

		state.currentStage = Math.min(3, state.currentStage + 1);
		state.highestUnlockedStage = Math.max(state.highestUnlockedStage, state.currentStage);
		state.progressTicks = 0;
		syncFrostStageHud(player);
		return 1;
	}

	public static int setFrostStageProgressSeconds(ServerPlayerEntity player, int seconds) {
		FrostStageState state = ensureAdminFrostStageState(player);
		if (state == null) {
			return 0;
		}

		int requirement = frostProgressRequirementTicks(state.currentStage);
		if (requirement <= 0) {
			state.progressTicks = 0;
			state.highestUnlockedStage = Math.max(state.highestUnlockedStage, state.currentStage);
			syncFrostStageHud(player);
			return 1;
		}

		int progressTicks = MathHelper.clamp(Math.max(0, seconds) * TICKS_PER_SECOND, 0, requirement);
		state.progressTicks = progressTicks;
		if (progressTicks >= requirement) {
			state.highestUnlockedStage = Math.min(3, state.currentStage + 1);
		} else {
			state.highestUnlockedStage = state.currentStage;
		}
		syncFrostStageHud(player);
		return 1;
	}

	private static boolean activeAbilityPreventsManaRegen(MagicAbility activeAbility) {
		return switch (activeAbility) {
			case NONE -> false;
			case BELOW_FREEZING -> false;
			case HERCULES_BURDEN_OF_THE_SKY -> HERCULES_DISABLE_MANA_REGEN_WHILE_ACTIVE;
			case SAGITTARIUS_ASTRAL_ARROW -> SAGITTARIUS_DISABLE_MANA_REGEN_DURING_WINDUP;
			case ASTRAL_CATACLYSM -> ASTRAL_CATACLYSM_DISABLE_MANA_REGEN_WHILE_ACTIVE;
			default -> true;
		};
	}

	private static void recordOrionsGambitAbilityUse(ServerPlayerEntity player, MagicAbility ability) {
		recordOrionsGambitAbilityUse(player, ability, -1);
	}

	private static void recordOrionsGambitAbilityUse(ServerPlayerEntity player, MagicAbility ability, int trackedCooldownTicks) {
		UUID casterId = ORIONS_GAMBIT_CASTER_BY_TARGET.get(player.getUuid());
		if (casterId == null) {
			return;
		}

		OrionGambitState state = ORIONS_GAMBIT_STATES.get(casterId);
		if (state == null || state.stage != OrionGambitStage.LINKED || !player.getUuid().equals(state.targetId)) {
			return;
		}

		state.usedTargetAbilities.add(ability);
		if (trackedCooldownTicks >= 0) {
			state.usedTargetCooldownOverrides.merge(ability, trackedCooldownTicks, Math::max);
		}
	}

	private static void trackOrionsGambitCooldownOverride(UUID playerId, MagicAbility ability, int trackedCooldownTicks) {
		if (trackedCooldownTicks < 0) {
			return;
		}

		UUID casterId = ORIONS_GAMBIT_CASTER_BY_TARGET.get(playerId);
		if (casterId == null) {
			return;
		}

		OrionGambitState state = ORIONS_GAMBIT_STATES.get(casterId);
		if (state == null || state.stage != OrionGambitStage.LINKED || !playerId.equals(state.targetId)) {
			return;
		}

		state.usedTargetAbilities.add(ability);
		state.usedTargetCooldownOverrides.merge(ability, trackedCooldownTicks, Math::max);
	}

	private static boolean shouldKeepCassiopeiaEnabled(ServerPlayerEntity player) {
		return MagicPlayerData.hasMagic(player)
			&& MagicPlayerData.getSchool(player) == MagicSchool.CONSTELLATION
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.CASSIOPEIA);
	}

	private static void deactivateCassiopeia(ServerPlayerEntity player, boolean sendEmptyOutline) {
		UUID playerId = player.getUuid();
		CASSIOPEIA_PASSIVE_ENABLED.remove(playerId);
		CASSIOPEIA_LAST_OUTLINED_PLAYERS.remove(playerId);
		if (sendEmptyOutline) {
			sendCassiopeiaOutlinePayload(player, List.of());
		}
	}

	private static void syncCassiopeiaOutlines(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		Set<UUID> outlinedPlayers = new HashSet<>();
		Box area = player.getBoundingBox().expand(CASSIOPEIA_DETECTION_RADIUS);
		for (ServerPlayerEntity target : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
			if (target == player || !target.isAlive() || target.isSpectator() || target.getEntityWorld() != player.getEntityWorld()) {
				continue;
			}

			if (target.getBoundingBox().intersects(area)) {
				outlinedPlayers.add(target.getUuid());
			}
		}

		Set<UUID> lastSent = CASSIOPEIA_LAST_OUTLINED_PLAYERS.get(playerId);
		if (lastSent != null && lastSent.equals(outlinedPlayers)) {
			return;
		}

		CASSIOPEIA_LAST_OUTLINED_PLAYERS.put(playerId, Set.copyOf(outlinedPlayers));
		sendCassiopeiaOutlinePayload(player, outlinedPlayers);
	}

	private static void sendCassiopeiaOutlinePayload(ServerPlayerEntity player, Iterable<UUID> outlinedPlayers) {
		List<UUID> uuids = new ArrayList<>();
		for (UUID outlinedPlayer : outlinedPlayers) {
			uuids.add(outlinedPlayer);
		}
		ServerPlayNetworking.send(player, new ConstellationOutlinePayload(List.copyOf(uuids)));
	}

	private static List<LivingEntity> findLivingTargetsAround(LivingEntity center, double radius) {
		List<LivingEntity> targets = new ArrayList<>();
		if (center == null || !center.isAlive()) {
			return targets;
		}

		targets.add(center);
		if (radius <= 0.0) {
			return targets;
		}

		Box area = center.getBoundingBox().expand(radius);
		Predicate<Entity> filter = entity ->
			entity instanceof LivingEntity living
				&& living.isAlive()
				&& entity != center
				&& (!(entity instanceof PlayerEntity player) || !player.isSpectator());
		for (Entity entity : center.getEntityWorld().getOtherEntities(center, area, filter)) {
			if (entity instanceof LivingEntity livingTarget && center.squaredDistanceTo(livingTarget) <= radius * radius) {
				targets.add(livingTarget);
			}
		}
		return targets;
	}

	private static boolean shouldKeepMartyrsFlameEnabled(ServerPlayerEntity player) {
		return player.isAlive()
			&& MagicPlayerData.hasMagic(player)
			&& MagicPlayerData.getSchool(player) == MagicSchool.BURNING_PASSION
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.MARTYRS_FLAME);
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

	private static LivingEntity findAstralCataclysmTargetInLineOfSight(ServerPlayerEntity caster, double range) {
		LivingEntity target = findLivingTargetInLineOfSight(caster, range);
		if (target == null || target == caster) {
			return null;
		}

		if (target instanceof ServerPlayerEntity targetPlayer && targetPlayer.isAlive() && !targetPlayer.isSpectator()) {
			return targetPlayer;
		}

		if (ASTRAL_CATACLYSM_ALLOW_MOB_TARGETS && target instanceof MobEntity) {
			return target;
		}

		return null;
	}

	private static void activateHerculesBurden(ServerPlayerEntity caster, List<LivingEntity> targets, int currentTick) {
		UUID casterId = caster.getUuid();
		int endTick = currentTick + HERCULES_EFFECT_DURATION_TICKS;
		HerculesBurdenState state = new HerculesBurdenState(endTick, new HashSet<>());
		HERCULES_STATES.put(casterId, state);
		setActiveAbility(caster, MagicAbility.HERCULES_BURDEN_OF_THE_SKY);
		MagicPlayerData.setDepletedRecoveryMode(caster, false);

		for (LivingEntity target : targets) {
			bindHerculesTarget(casterId, state, target, currentTick, endTick);
			applyHerculesInitialImpact(caster, target);
		}

		caster.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.HERCULES_BURDEN_OF_THE_SKY.displayName()), true);
	}

	private static void bindHerculesTarget(UUID casterId, HerculesBurdenState state, LivingEntity target, int currentTick, int endTick) {
		AstralBurdenTargetState existing = HERCULES_TARGETS.get(target.getUuid());
		if (existing != null && !existing.casterId.equals(casterId)) {
			HerculesBurdenState previousState = HERCULES_STATES.get(existing.casterId);
			if (previousState != null) {
				previousState.targetIds.remove(target.getUuid());
			}
		}

		HERCULES_TARGETS.put(
			target.getUuid(),
			new AstralBurdenTargetState(
				target.getEntityWorld().getRegistryKey(),
				casterId,
				target.getX(),
				target.getY(),
				target.getZ(),
				endTick,
				currentTick
			)
		);
		state.targetIds.add(target.getUuid());
	}

	private static void applyHerculesInitialImpact(ServerPlayerEntity caster, LivingEntity target) {
		target.setVelocity(0.0, 0.0, 0.0);
		target.setOnGround(true);
		if (HERCULES_INTERRUPT_TARGET_ITEM_USE) {
			target.stopUsingItem();
		}
		if (target instanceof MobEntity mob) {
			mob.getNavigation().stop();
		}
		target.addStatusEffect(
			new StatusEffectInstance(StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, HERCULES_SLOWNESS_AMPLIFIER, true, false, false)
		);
		refreshStatusEffect(target, StatusEffects.RESISTANCE, EFFECT_REFRESH_TICKS, HERCULES_RESISTANCE_AMPLIFIER, true, false, false);
		spawnHerculesImpactBurst(target);
		spawnHerculesBurdenParticles(target);
		if (HERCULES_TRUE_DAMAGE > 0.0F && target.getEntityWorld() instanceof ServerWorld world) {
			dealTrackedMagicDamage(target, caster.getUuid(), world.getDamageSources().genericKill(), HERCULES_TRUE_DAMAGE);
		}
		if (target instanceof ServerPlayerEntity targetPlayer) {
			sendHerculesWarning(targetPlayer);
		}
	}

	private static void sendHerculesWarning(ServerPlayerEntity target) {
		ConstellationWarningOverlayPayload payload = new ConstellationWarningOverlayPayload(
			Text.translatable("message.magic.constellation.hercules_warning").getString(),
			HERCULES_WARNING_COLOR_RGB,
			HERCULES_WARNING_SCALE,
			HERCULES_WARNING_FADE_IN_TICKS,
			HERCULES_WARNING_STAY_TICKS,
			HERCULES_WARNING_FADE_OUT_TICKS
		);
		ServerPlayNetworking.send(target, payload);
	}

	private static void applyHerculesBurden(ServerPlayerEntity caster, int currentTick) {
		HerculesBurdenState state = HERCULES_STATES.get(caster.getUuid());
		if (state == null) {
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		if (currentTick >= state.endTick) {
			deactivateHerculesBurden(caster, true, true);
		}
	}

	private static void updateHerculesBurdenStates(MinecraftServer server, int currentTick) {
		List<UUID> expiredCasters = new ArrayList<>();
		Iterator<Map.Entry<UUID, HerculesBurdenState>> iterator = HERCULES_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, HerculesBurdenState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			if (caster == null || !caster.isAlive() || activeAbility(caster) != MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
				releaseHerculesTargets(entry.getKey());
				iterator.remove();
				continue;
			}

			if (currentTick >= entry.getValue().endTick) {
				expiredCasters.add(entry.getKey());
			}
		}

		for (UUID casterId : expiredCasters) {
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(casterId);
			if (caster != null) {
				deactivateHerculesBurden(caster, true, true);
			}
		}
	}

	private static void updateHerculesBurdenTargets(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, AstralBurdenTargetState>> iterator = HERCULES_TARGETS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, AstralBurdenTargetState> entry = iterator.next();
			AstralBurdenTargetState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !target.isAlive() || currentTick >= state.endTick) {
				iterator.remove();
				continue;
			}

			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(state.casterId);
			if (caster == null || !caster.isAlive() || activeAbility(caster) != MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
				iterator.remove();
				continue;
			}

			teleportDomainEntity(target, state.lockedX, state.lockedY, state.lockedZ, target.getYaw(), target.getPitch());
			if (HERCULES_INTERRUPT_TARGET_ITEM_USE) {
				target.stopUsingItem();
			}
			refreshStatusEffect(target, StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, HERCULES_SLOWNESS_AMPLIFIER, true, false, false);
			refreshStatusEffect(target, StatusEffects.RESISTANCE, EFFECT_REFRESH_TICKS, HERCULES_RESISTANCE_AMPLIFIER, true, false, false);
			if (target instanceof MobEntity mob) {
				mob.getNavigation().stop();
			}
			if (currentTick - state.lastParticleTick >= HERCULES_PARTICLE_INTERVAL_TICKS) {
				spawnHerculesBurdenParticles(target);
				state.lastParticleTick = currentTick;
			}
		}
	}

	private static void deactivateHerculesBurden(ServerPlayerEntity caster, boolean startCooldown, boolean sendFeedback) {
		UUID casterId = caster.getUuid();
		HerculesBurdenState state = HERCULES_STATES.remove(casterId);
		releaseHerculesTargets(casterId);
		if (startCooldown) {
			startAbilityCooldownFromNow(casterId, MagicAbility.HERCULES_BURDEN_OF_THE_SKY, caster.getEntityWorld().getServer().getTicks());
		}
		if (activeAbility(caster) == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			setActiveAbility(caster, MagicAbility.NONE);
		}
		if (sendFeedback && state != null) {
			caster.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.HERCULES_BURDEN_OF_THE_SKY.displayName()), true);
		}
	}

	private static void releaseHerculesTargets(UUID casterId) {
		HERCULES_TARGETS.entrySet().removeIf(entry -> entry.getValue().casterId.equals(casterId));
	}

	private static void applySagittariusWindupModifier(ServerPlayerEntity caster) {
		setSignedAttributeModifier(
			caster.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED),
			SAGITTARIUS_WINDUP_SPEED_MODIFIER_ID,
			SAGITTARIUS_WINDUP_MOVEMENT_SPEED_MULTIPLIER - 1.0,
			EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);
	}

	private static void clearSagittariusWindup(ServerPlayerEntity caster) {
		SAGITTARIUS_STATES.remove(caster.getUuid());
		removeAttributeModifier(caster.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), SAGITTARIUS_WINDUP_SPEED_MODIFIER_ID);
		if (activeAbility(caster) == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			setActiveAbility(caster, MagicAbility.NONE);
		}
	}

	private static void applySagittariusAstralArrow(ServerPlayerEntity caster, int currentTick) {
		SagittariusWindupState state = SAGITTARIUS_STATES.get(caster.getUuid());
		if (state == null) {
			clearSagittariusWindup(caster);
			return;
		}

		applySagittariusWindupModifier(caster);
		if (currentTick >= state.fireTick) {
			fireSagittariusAstralArrow(caster, currentTick);
		}
	}

	private static void updateSagittariusWindups(MinecraftServer server, int currentTick) {
		List<UUID> readyToFire = new ArrayList<>();
		List<UUID> staleWindups = new ArrayList<>();
		Iterator<Map.Entry<UUID, SagittariusWindupState>> iterator = SAGITTARIUS_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, SagittariusWindupState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			if (caster == null || !caster.isAlive() || activeAbility(caster) != MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
				staleWindups.add(entry.getKey());
				continue;
			}

			if (currentTick >= entry.getValue().fireTick) {
				readyToFire.add(entry.getKey());
			} else {
				spawnSagittariusChargeParticles(caster);
			}
		}

		for (UUID casterId : readyToFire) {
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(casterId);
			if (caster != null && caster.isAlive()) {
				fireSagittariusAstralArrow(caster, currentTick);
			}
		}

		for (UUID casterId : staleWindups) {
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(casterId);
			if (caster != null) {
				clearSagittariusWindup(caster);
				continue;
			}

			SAGITTARIUS_STATES.remove(casterId);
		}
	}

	private static void fireSagittariusAstralArrow(ServerPlayerEntity caster, int currentTick) {
		SAGITTARIUS_STATES.remove(caster.getUuid());
		removeAttributeModifier(caster.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), SAGITTARIUS_WINDUP_SPEED_MODIFIER_ID);
		Vec3d start = caster.getEyePos();
		Vec3d direction = caster.getRotationVec(1.0F);
		Vec3d end = start.add(direction.multiply(SAGITTARIUS_BEAM_RANGE));
		BlockHitResult blockHit = caster.getEntityWorld().raycast(
			new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, caster)
		);
		if (blockHit.getType() == HitResult.Type.BLOCK) {
			end = blockHit.getPos();
		}

		for (LivingEntity target : collectLivingEntitiesAlongBeam(caster, start, end, SAGITTARIUS_BEAM_RADIUS)) {
			float damage = sagittariusDamageForDistance(Math.sqrt(caster.squaredDistanceTo(target)));
			if (damage > 0.0F && caster.getEntityWorld() instanceof ServerWorld world) {
				dealTrackedMagicDamage(target, caster.getUuid(), world.getDamageSources().genericKill(), damage);
			}
		}

		if (caster.getEntityWorld() instanceof ServerWorld world) {
			spawnParticleBeam(world, start, end, SAGITTARIUS_BEAM_PARTICLE, 0.6);
			world.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.2F, 1.7F);
			world.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.8F, 1.15F);
		}

		setActiveAbility(caster, MagicAbility.NONE);
		startAbilityCooldownFromNow(caster.getUuid(), MagicAbility.SAGITTARIUS_ASTRAL_ARROW, currentTick);
	}

	private static List<LivingEntity> collectLivingEntitiesAlongBeam(
		ServerPlayerEntity caster,
		Vec3d start,
		Vec3d end,
		double radius
	) {
		double minX = Math.min(start.x, end.x);
		double minY = Math.min(start.y, end.y);
		double minZ = Math.min(start.z, end.z);
		double maxX = Math.max(start.x, end.x);
		double maxY = Math.max(start.y, end.y);
		double maxZ = Math.max(start.z, end.z);
		Box searchBox = new Box(minX, minY, minZ, maxX, maxY, maxZ).expand(radius);
		List<LivingEntity> targets = new ArrayList<>();
		Predicate<Entity> filter = entity ->
			entity instanceof LivingEntity living
				&& living.isAlive()
				&& entity != caster
				&& (!(entity instanceof PlayerEntity player) || !player.isSpectator());
		for (Entity entity : caster.getEntityWorld().getOtherEntities(caster, searchBox, filter)) {
			if (entity instanceof LivingEntity livingTarget && livingTarget.getBoundingBox().expand(radius).raycast(start, end).isPresent()) {
				targets.add(livingTarget);
			}
		}
		return targets;
	}

	private static float sagittariusDamageForDistance(double distance) {
		if (distance <= SAGITTARIUS_CLOSE_RANGE_THRESHOLD) {
			return SAGITTARIUS_CLOSE_RANGE_TRUE_DAMAGE;
		}
		if (distance <= SAGITTARIUS_MID_RANGE_THRESHOLD) {
			return SAGITTARIUS_MID_RANGE_TRUE_DAMAGE;
		}
		return SAGITTARIUS_FAR_RANGE_TRUE_DAMAGE;
	}

	private static void spawnParticleBeam(ServerWorld world, Vec3d start, Vec3d end, ParticleEffect particle, double spacing) {
		Vec3d delta = end.subtract(start);
		double length = delta.length();
		if (length <= 1.0E-6) {
			world.spawnParticles(particle, start.x, start.y, start.z, 1, 0.0, 0.0, 0.0, 0.0);
			return;
		}

		Vec3d step = delta.normalize().multiply(Math.max(0.1, spacing));
		int steps = Math.max(1, (int) Math.ceil(length / Math.max(0.1, spacing)));
		Vec3d current = start;
		for (int i = 0; i <= steps; i++) {
			world.spawnParticles(particle, current.x, current.y, current.z, 2, 0.08, 0.08, 0.08, 0.0);
			current = current.add(step);
		}
	}

	private static void applyOrionsGambit(ServerPlayerEntity caster, int currentTick) {
		OrionGambitState state = ORIONS_GAMBIT_STATES.get(caster.getUuid());
		if (state == null) {
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		if (state.stage == OrionGambitStage.WAITING) {
			ServerPlayerEntity target = findPlayerTargetInLineOfSight(caster, ORIONS_GAMBIT_TARGET_RANGE);
			if (target != null && target != caster) {
				lockOrionsGambit(caster, target, currentTick);
			}
			return;
		}

		ServerPlayerEntity target = resolveOrionsGambitTarget(caster.getEntityWorld().getServer(), state);
		if (target == null || !target.isAlive() || target.isSpectator()) {
			endOrionsGambit(caster, OrionGambitEndReason.TARGET_INVALID, currentTick, false);
			return;
		}

		if (currentTick >= state.benefitEndTick) {
			endOrionsGambit(caster, OrionGambitEndReason.EXPIRED, currentTick, true);
			return;
		}

		if (currentTick % ORIONS_GAMBIT_TARGET_PARTICLE_INTERVAL_TICKS == 0) {
			spawnOrionsGambitTargetParticles(target);
		}
		applyOrionsGambitPenaltyEffects(caster);
	}

	private static void updateOrionsGambitStates(MinecraftServer server, int currentTick) {
		List<OrionPendingEndState> pendingEnds = new ArrayList<>();
		Iterator<Map.Entry<UUID, OrionGambitState>> iterator = ORIONS_GAMBIT_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, OrionGambitState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			if (caster == null) {
				clearOrionsGambitTargetEffects(entry.getValue(), server);
				releaseOrionsGambitTarget(entry.getValue());
				iterator.remove();
				continue;
			}
			if (!caster.isAlive()) {
				pendingEnds.add(new OrionPendingEndState(caster, OrionGambitEndReason.CASTER_DIED, false));
				continue;
			}

			if (activeAbility(caster) != MagicAbility.ORIONS_GAMBIT) {
				pendingEnds.add(new OrionPendingEndState(caster, OrionGambitEndReason.MANUAL_CANCEL, false));
				continue;
			}

			if (entry.getValue().stage == OrionGambitStage.LINKED && currentTick >= entry.getValue().benefitEndTick) {
				pendingEnds.add(new OrionPendingEndState(caster, OrionGambitEndReason.EXPIRED, true));
			}
		}

		for (OrionPendingEndState pendingEnd : pendingEnds) {
			endOrionsGambit(pendingEnd.caster, pendingEnd.reason, currentTick, pendingEnd.sendFeedback);
		}
	}

	private static void lockOrionsGambit(ServerPlayerEntity caster, ServerPlayerEntity target, int currentTick) {
		UUID existingCasterId = ORIONS_GAMBIT_CASTER_BY_TARGET.get(target.getUuid());
		if (existingCasterId != null && !existingCasterId.equals(caster.getUuid())) {
			caster.sendMessage(Text.translatable("message.magic.ability.target_controlled"), true);
			return;
		}

		OrionGambitState state = ORIONS_GAMBIT_STATES.get(caster.getUuid());
		if (state == null) {
			return;
		}

		state.stage = OrionGambitStage.LINKED;
		state.targetDimension = target.getEntityWorld().getRegistryKey();
		state.targetId = target.getUuid();
		state.benefitEndTick = currentTick + ORIONS_GAMBIT_LINK_DURATION_TICKS;
		state.usedTargetAbilities.clear();
		state.usedTargetCooldownOverrides.clear();
		ORIONS_GAMBIT_CASTER_BY_TARGET.put(target.getUuid(), caster.getUuid());
		if (ORIONS_GAMBIT_CLEAR_TARGET_COOLDOWNS_ON_LOCK) {
			resetAllCooldowns(target);
		}
		applyOrionsGambitPenaltyEffects(caster);
		caster.sendMessage(Text.translatable("message.magic.constellation.orion_locked", target.getDisplayName()), true);
		target.sendMessage(Text.translatable("message.magic.constellation.orion_targeted", caster.getDisplayName()), true);
	}

	private static ServerPlayerEntity resolveOrionsGambitTarget(MinecraftServer server, OrionGambitState state) {
		if (state == null || state.stage != OrionGambitStage.LINKED || state.targetDimension == null || state.targetId == null) {
			return null;
		}

		ServerWorld world = server.getWorld(state.targetDimension);
		if (world == null) {
			return null;
		}

		Entity entity = world.getEntity(state.targetId);
		return entity instanceof ServerPlayerEntity targetPlayer ? targetPlayer : null;
	}

	private static void endOrionsGambit(
		ServerPlayerEntity caster,
		OrionGambitEndReason reason,
		int currentTick,
		boolean sendFeedback
	) {
		UUID casterId = caster.getUuid();
		OrionGambitState state = ORIONS_GAMBIT_STATES.remove(casterId);
		ORIONS_GAMBIT_DRAIN_BUFFER.remove(casterId);
		if (state == null) {
			if (activeAbility(caster) == MagicAbility.ORIONS_GAMBIT) {
				setActiveAbility(caster, MagicAbility.NONE);
			}
			return;
		}

		boolean linked = state.stage == OrionGambitStage.LINKED;
		if (linked && ORIONS_GAMBIT_APPLY_USED_TARGET_COOLDOWNS_ON_END) {
			applyOrionsGambitTargetCooldowns(state, caster.getEntityWorld().getServer(), currentTick);
		}
		clearOrionsGambitTargetEffects(state, caster.getEntityWorld().getServer());
		releaseOrionsGambitTarget(state);
		if (linked && ORIONS_GAMBIT_RESET_CASTER_COOLDOWNS_ON_END) {
			applyOrionsGambitCasterCooldownReset(casterId, currentTick);
		}

		int cooldownTicks = reason == OrionGambitEndReason.WAITING_CANCEL
			? ORIONS_GAMBIT_WAIT_CANCEL_COOLDOWN_TICKS
			: ORIONS_GAMBIT_COOLDOWN_TICKS;
		if (cooldownTicks > 0) {
			ORIONS_GAMBIT_COOLDOWN_END_TICK.put(casterId, currentTick + cooldownTicks);
		}

		if (activeAbility(caster) == MagicAbility.ORIONS_GAMBIT) {
			setActiveAbility(caster, MagicAbility.NONE);
		}

		if (linked && state.benefitEndTick > currentTick) {
			ORIONS_GAMBIT_PENALTIES.put(casterId, new OrionPenaltyState(state.benefitEndTick));
		} else {
			clearOrionsGambitPenaltyEffects(caster);
		}

		if (sendFeedback) {
			caster.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.ORIONS_GAMBIT.displayName()), true);
		}
	}

	private static void releaseOrionsGambitTarget(OrionGambitState state) {
		if (state != null && state.targetId != null) {
			ORIONS_GAMBIT_CASTER_BY_TARGET.remove(state.targetId);
		}
	}

	private static void clearOrionsGambitTargetEffects(OrionGambitState state, MinecraftServer server) {
		if (
			state == null
			|| server == null
			|| !ORIONS_GAMBIT_RESET_GREED_TARGET_COINS_ON_END
			|| state.targetId == null
			|| state.targetDimension == null
		) {
			return;
		}

		ServerPlayerEntity target = resolveOrionsGambitTarget(server, state);
		if (target != null && MagicPlayerData.getSchool(target) == MagicSchool.GREED) {
			MagicPlayerData.setGreedCoinUnits(target, 0);
		}
	}

	private static void enforceOrionsGambitGreedTargetCoins(MinecraftServer server) {
		if (server == null || ORIONS_GAMBIT_GREED_TARGET_COIN_UNITS <= 0) {
			return;
		}

		for (OrionGambitState state : ORIONS_GAMBIT_STATES.values()) {
			if (state.stage != OrionGambitStage.LINKED) {
				continue;
			}

			ServerPlayerEntity target = resolveOrionsGambitTarget(server, state);
			if (target != null && MagicPlayerData.getSchool(target) == MagicSchool.GREED) {
				MagicPlayerData.setGreedCoinUnits(target, ORIONS_GAMBIT_GREED_TARGET_COIN_UNITS);
			}
		}
	}

	private static void updateOrionsGambitPenalties(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, OrionPenaltyState>> iterator = ORIONS_GAMBIT_PENALTIES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, OrionPenaltyState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			if (caster == null || !caster.isAlive() || currentTick >= entry.getValue().endTick) {
				if (caster != null) {
					clearOrionsGambitPenaltyEffects(caster);
				}
				iterator.remove();
				continue;
			}

			applyOrionsGambitPenaltyEffects(caster);
		}
	}

	private static void applyOrionsGambitPenaltyEffects(ServerPlayerEntity caster) {
		refreshStatusEffect(
			caster,
			StatusEffects.WEAKNESS,
			ORIONS_GAMBIT_CASTER_PENALTY_REFRESH_TICKS,
			ORIONS_GAMBIT_CASTER_WEAKNESS_AMPLIFIER,
			true,
			false,
			true
		);
		refreshStatusEffect(
			caster,
			StatusEffects.SLOWNESS,
			ORIONS_GAMBIT_CASTER_PENALTY_REFRESH_TICKS,
			ORIONS_GAMBIT_CASTER_SLOWNESS_AMPLIFIER,
			true,
			false,
			true
		);
		double cappedHealthPoints = ORIONS_GAMBIT_CASTER_MAX_HEALTH_HEARTS * 2.0;
		EntityAttributeInstance maxHealth = caster.getAttributeInstance(EntityAttributes.MAX_HEALTH);
		if (maxHealth != null) {
			EntityAttributeModifier existing = maxHealth.getModifier(ORIONS_GAMBIT_MAX_HEALTH_MODIFIER_ID);
			double baseWithoutModifier = caster.getMaxHealth() - (existing == null ? 0.0 : existing.value());
			double modifierValue = Math.min(0.0, cappedHealthPoints - baseWithoutModifier);
			setSignedAttributeModifier(
				maxHealth,
				ORIONS_GAMBIT_MAX_HEALTH_MODIFIER_ID,
				modifierValue,
				EntityAttributeModifier.Operation.ADD_VALUE
			);
			if (caster.getHealth() > caster.getMaxHealth()) {
				caster.setHealth(caster.getMaxHealth());
			}
		}
	}

	private static void clearOrionsGambitPenaltyEffects(ServerPlayerEntity caster) {
		removeAttributeModifier(caster.getAttributeInstance(EntityAttributes.MAX_HEALTH), ORIONS_GAMBIT_MAX_HEALTH_MODIFIER_ID);
		if (caster.getHealth() > caster.getMaxHealth()) {
			caster.setHealth(caster.getMaxHealth());
		}
	}

	private static void applyOrionsGambitCasterCooldownReset(UUID casterId, int currentTick) {
		for (MagicAbility ability : abilitiesForSchool(MagicSchool.CONSTELLATION)) {
			startAbilityCooldownFromNow(casterId, ability, currentTick);
		}
	}

	private static void applyOrionsGambitTargetCooldowns(OrionGambitState state, MinecraftServer server, int currentTick) {
		if (state == null || state.targetId == null) {
			return;
		}

		for (MagicAbility ability : state.usedTargetAbilities) {
			if (ability == MagicAbility.WITTY_ONE_LINER && state.usedTargetCooldownOverrides.containsKey(ability)) {
				int cooldownTicks = Math.max(0, state.usedTargetCooldownOverrides.get(ability));
				if (cooldownTicks > 0) {
					WITTY_ONE_LINER_COOLDOWN_END_TICK.put(state.targetId, currentTick + cooldownTicks);
				} else {
					WITTY_ONE_LINER_COOLDOWN_END_TICK.remove(state.targetId);
				}
				continue;
			}
			if (ability == MagicAbility.COMEDIC_ASSISTANT && state.usedTargetCooldownOverrides.containsKey(ability)) {
				int cooldownTicks = Math.max(0, state.usedTargetCooldownOverrides.get(ability));
				if (cooldownTicks > 0) {
					COMEDIC_ASSISTANT_COOLDOWN_END_TICK.put(state.targetId, currentTick + cooldownTicks);
				} else {
					COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(state.targetId);
				}
				continue;
			}
			if (ability == MagicAbility.PLUS_ULTRA && state.usedTargetCooldownOverrides.containsKey(ability)) {
				int cooldownTicks = Math.max(0, state.usedTargetCooldownOverrides.get(ability));
				if (cooldownTicks > 0) {
					PLUS_ULTRA_COOLDOWN_END_TICK.put(state.targetId, currentTick + cooldownTicks);
				} else {
					PLUS_ULTRA_COOLDOWN_END_TICK.remove(state.targetId);
				}
				continue;
			}
			startAbilityCooldownFromNow(state.targetId, ability, currentTick);
		}

		ServerPlayerEntity target = server.getPlayerManager().getPlayer(state.targetId);
		if (target != null && !target.isAlive()) {
			clearOrionsGambitPenaltyEffects(target);
		}
	}

	private static boolean isOrionsGambitManaCostSuppressed(ServerPlayerEntity player) {
		return ORIONS_GAMBIT_SUPPRESS_TARGET_MANA_COSTS && ORIONS_GAMBIT_CASTER_BY_TARGET.containsKey(player.getUuid());
	}

	private static boolean isOrionsGambitManaDrainSuppressed(ServerPlayerEntity player) {
		return ORIONS_GAMBIT_SUPPRESS_TARGET_MANA_COSTS && ORIONS_GAMBIT_CASTER_BY_TARGET.containsKey(player.getUuid());
	}

	private static boolean isOrionsGambitCooldownSuppressed(ServerPlayerEntity player) {
		return ORIONS_GAMBIT_SUPPRESS_TARGET_COOLDOWNS && ORIONS_GAMBIT_CASTER_BY_TARGET.containsKey(player.getUuid());
	}

	static boolean areOrionsGambitTargetCooldownsSuppressed(ServerPlayerEntity player) {
		return isOrionsGambitCooldownSuppressed(player);
	}

	private static boolean hasActiveOrionsGambitPenalty(ServerPlayerEntity player) {
		OrionGambitState activeState = ORIONS_GAMBIT_STATES.get(player.getUuid());
		if (activeState != null && activeState.stage == OrionGambitStage.LINKED) {
			return true;
		}
		return ORIONS_GAMBIT_PENALTIES.containsKey(player.getUuid());
	}

	private static void applyAstralCataclysm(ServerPlayerEntity caster, int currentTick) {
		if (!ASTRAL_CATACLYSM_DOMAIN_STATES.containsKey(caster.getUuid()) && !DOMAIN_EXPANSIONS.containsKey(caster.getUuid())) {
			setActiveAbility(caster, MagicAbility.NONE);
		}
	}

	private static void updateAstralCataclysmDomainStates(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, ConstellationDomainState>> iterator = ASTRAL_CATACLYSM_DOMAIN_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ConstellationDomainState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			DomainExpansionState domain = DOMAIN_EXPANSIONS.get(entry.getKey());
			if (caster == null || !caster.isAlive() || domain == null || domain.ability != MagicAbility.ASTRAL_CATACLYSM) {
				iterator.remove();
				continue;
			}

			ConstellationDomainState state = entry.getValue();
			sendAstralCataclysmExpiryWarnings(caster, state, domain, currentTick);
			if (state.phase == ConstellationDomainPhase.CHARGING && currentTick >= state.chargeCompleteTick) {
				state.phase = ConstellationDomainPhase.READY;
				caster.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.ready"), true);
				continue;
			}

			if (state.phase != ConstellationDomainPhase.ACQUIRING) {
				continue;
			}

			if (currentTick >= state.acquireEndTick) {
				iterator.remove();
				deactivateDomainExpansion(caster);
				setActiveAbility(caster, MagicAbility.NONE);
				caster.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.no_target"), true);
				continue;
			}

			LivingEntity target = findAstralCataclysmTargetInLineOfSight(caster, ASTRAL_CATACLYSM_TARGET_RANGE);
			if (target != null) {
				iterator.remove();
				triggerAstralCataclysmExecution(caster, target, currentTick);
			}
		}
	}

	private static void sendAstralCataclysmExpiryWarnings(
		ServerPlayerEntity caster,
		ConstellationDomainState state,
		DomainExpansionState domain,
		int currentTick
	) {
		if (domain.expiresTick == Integer.MAX_VALUE) {
			return;
		}

		int remainingTicks = Math.max(0, domain.expiresTick - currentTick);
		if (remainingTicks <= 0) {
			return;
		}

		for (int warningTicks : ASTRAL_CATACLYSM_EXPIRY_WARNING_TICKS) {
			if (remainingTicks > warningTicks || !state.announcedExpiryWarnings.add(warningTicks)) {
				continue;
			}

			int remainingSeconds = Math.max(1, MathHelper.ceil(remainingTicks / (float) TICKS_PER_SECOND));
			caster.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.expiring", remainingSeconds), true);
		}
	}

	private static void triggerAstralCataclysmExecution(ServerPlayerEntity caster, LivingEntity target, int currentTick) {
		DomainExpansionState domain = DOMAIN_EXPANSIONS.get(caster.getUuid());
		Vec3d beamOrigin = new Vec3d(target.getX(), target.getY(), target.getZ());
		float restoreYaw = target.getYaw();
		float restorePitch = target.getPitch();
		if (domain != null) {
			DomainCapturedEntityState capturedState = domain.capturedEntities.get(target.getUuid());
			if (capturedState != null) {
				beamOrigin = capturedState.position;
				restoreYaw = capturedState.yaw;
				restorePitch = capturedState.pitch;
			}
		}

		if (
			target instanceof ServerPlayerEntity targetPlayer
			&& ASTRAL_CATACLYSM_CANCEL_BEAM_ON_PROTECTED_BEACON_CORE_HOLDER
			&& isBeaconCoreProtected(targetPlayer)
		) {
			caster.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.beacon_core_blocked.caster", target.getDisplayName()), true);
			targetPlayer.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.beacon_core_blocked.target"), true);
			deactivateDomainExpansion(caster);
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		ASTRAL_EXECUTION_BEAMS.put(
			target.getUuid(),
			new AstralExecutionBeamState(
				target.getEntityWorld().getRegistryKey(),
				caster.getUuid(),
				beamOrigin.x,
				beamOrigin.y,
				beamOrigin.z,
				beamOrigin.x,
				beamOrigin.y,
				beamOrigin.z,
				restoreYaw,
				restorePitch,
				currentTick,
				currentTick
			)
		);
		target.stopUsingItem();
		target.setVelocity(0.0, 0.0, 0.0);
		target.setOnGround(true);
		if (target instanceof MobEntity mob) {
			mob.getNavigation().stop();
		}
		caster.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.locked", target.getDisplayName()), true);
		if (target instanceof ServerPlayerEntity targetPlayer) {
			targetPlayer.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.targeted"), true);
		}
		deactivateDomainExpansion(caster);
		setActiveAbility(caster, MagicAbility.NONE);
	}

	private static void updateAstralExecutionBeams(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, AstralExecutionBeamState>> iterator = ASTRAL_EXECUTION_BEAMS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, AstralExecutionBeamState> entry = iterator.next();
			AstralExecutionBeamState state = entry.getValue();
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
			if (target instanceof PlayerEntity player && player.isSpectator()) {
				iterator.remove();
				continue;
			}
			if (
				target instanceof ServerPlayerEntity targetPlayer
				&& ASTRAL_CATACLYSM_CANCEL_BEAM_ON_PROTECTED_BEACON_CORE_HOLDER
				&& isBeaconCoreProtected(targetPlayer)
			) {
				restoreAstralExecutionBeamTarget(target, state);
				iterator.remove();
				ServerPlayerEntity caster = server.getPlayerManager().getPlayer(state.casterId);
				if (caster != null) {
					caster.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.beacon_core_blocked.caster", target.getDisplayName()), true);
				}
				targetPlayer.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.beacon_core_blocked.target"), true);
				continue;
			}

			target.stopUsingItem();
			target.setVelocity(0.0, 0.0, 0.0);
			target.setOnGround(true);
			double risePerTick = ASTRAL_CATACLYSM_BEAM_RISE_BLOCKS_PER_SECOND / TICKS_PER_SECOND;
			double maxRiseY = Math.max(state.baseY, world.getTopYInclusive() + 1.0 - target.getHeight());
			state.currentY = Math.min(state.currentY + risePerTick, maxRiseY);
			teleportDomainEntity(target, state.lockedX, state.currentY, state.lockedZ, target.getYaw(), target.getPitch());
			if (target instanceof MobEntity mob) {
				mob.getNavigation().stop();
			}
			spawnAstralExecutionBeamParticles(world, state);
			if (currentTick >= state.nextSoundTick) {
				playAstralExecutionBeamSounds(world, state);
				state.nextSoundTick = currentTick + ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_INTERVAL_TICKS;
			}
			if (currentTick >= state.nextDamageTick) {
				applyAstralExecutionDamage(target, state);
				state.nextDamageTick = currentTick + ASTRAL_CATACLYSM_BEAM_DAMAGE_INTERVAL_TICKS;
			}

			if (!target.isAlive()) {
				iterator.remove();
			}
		}
	}

	private static void applyAstralExecutionDamage(LivingEntity target, AstralExecutionBeamState state) {
		if (!(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		if (!ASTRAL_CATACLYSM_IGNORE_TOTEMS) {
			dealTrackedMagicDamage(target, state.casterId, world.getDamageSources().genericKill(), ASTRAL_CATACLYSM_BEAM_TRUE_DAMAGE_PER_INTERVAL);
			return;
		}

		float nextHealth = target.getHealth() - ASTRAL_CATACLYSM_BEAM_TRUE_DAMAGE_PER_INTERVAL;
		if (nextHealth <= 0.0F) {
			target.kill(world);
			return;
		}

		target.setHealth(nextHealth);
	}

	private static void spawnAstralExecutionBeamParticles(ServerWorld world, AstralExecutionBeamState state) {
		double topY = state.currentY + 32.0;
		double beamY = state.baseY;
		while (beamY <= topY) {
			if (ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_COUNT > 0) {
				world.spawnParticles(
					ASTRAL_EXECUTION_BEAM_PARTICLE,
					state.lockedX,
					beamY,
					state.lockedZ,
					ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_COUNT,
					ASTRAL_CATACLYSM_BEAM_RADIUS * 0.07,
					0.12,
					ASTRAL_CATACLYSM_BEAM_RADIUS * 0.07,
					0.0
				);
			}
			if (ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_COUNT > 0) {
				world.spawnParticles(
					ASTRAL_EXECUTION_BEAM_OUTER_PARTICLE,
					state.lockedX,
					beamY,
					state.lockedZ,
					ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_COUNT,
					ASTRAL_CATACLYSM_BEAM_RADIUS * 0.18,
					0.14,
					ASTRAL_CATACLYSM_BEAM_RADIUS * 0.18,
					0.0
				);
			}
			if (ASTRAL_CATACLYSM_BEAM_SPARK_PARTICLE_COUNT > 0) {
				world.spawnParticles(
					ASTRAL_EXECUTION_BEAM_SPARK_PARTICLE,
					state.lockedX,
					beamY,
					state.lockedZ,
					ASTRAL_CATACLYSM_BEAM_SPARK_PARTICLE_COUNT,
					ASTRAL_CATACLYSM_BEAM_RADIUS * 0.11,
					0.1,
					ASTRAL_CATACLYSM_BEAM_RADIUS * 0.11,
					0.0
				);
			}
			spawnAstralExecutionBeamRing(world, state.lockedX, beamY, state.lockedZ);
			beamY += ASTRAL_CATACLYSM_BEAM_PARTICLE_STEP;
		}
	}

	private static void playAstralExecutionBeamSounds(ServerWorld world, AstralExecutionBeamState state) {
		world.playSound(
			null,
			state.lockedX,
			state.currentY,
			state.lockedZ,
			SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
			SoundCategory.PLAYERS,
			ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_VOLUME,
			ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_PITCH
		);
		world.playSound(
			null,
			state.lockedX,
			state.currentY,
			state.lockedZ,
			SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM,
			SoundCategory.PLAYERS,
			ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_VOLUME * 0.8F,
			ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_PITCH + 0.2F
		);
	}

	private static void restoreAstralExecutionBeamTarget(LivingEntity target, AstralExecutionBeamState state) {
		teleportDomainEntity(target, state.restoreX, state.restoreY, state.restoreZ, state.restoreYaw, state.restorePitch);
		target.setVelocity(0.0, 0.0, 0.0);
		target.setOnGround(true);
	}

	private static void spawnAstralExecutionBeamRing(ServerWorld world, double x, double y, double z) {
		if (ASTRAL_CATACLYSM_BEAM_RING_POINTS_PER_STEP <= 0) {
			return;
		}

		double ringRadius = Math.max(0.2, ASTRAL_CATACLYSM_BEAM_RADIUS * 0.42);
		for (int index = 0; index < ASTRAL_CATACLYSM_BEAM_RING_POINTS_PER_STEP; index++) {
			double angle = (Math.PI * 2.0 * index) / ASTRAL_CATACLYSM_BEAM_RING_POINTS_PER_STEP;
			double ringX = x + Math.cos(angle) * ringRadius;
			double ringZ = z + Math.sin(angle) * ringRadius;
			world.spawnParticles(ASTRAL_EXECUTION_BEAM_OUTER_PARTICLE, ringX, y, ringZ, 1, 0.03, 0.08, 0.03, 0.0);
		}
	}

	private static void spawnHerculesImpactBurst(LivingEntity target) {
		if (!(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = target.getX();
		double y = target.getY() + 0.05;
		double z = target.getZ();
		if (HERCULES_ACTIVATION_IMPACT_DIRT_PARTICLE_COUNT > 0) {
			world.spawnParticles(
				HERCULES_ACTIVATION_DIRT_PARTICLE,
				x,
				y,
				z,
				HERCULES_ACTIVATION_IMPACT_DIRT_PARTICLE_COUNT,
				0.45,
				0.12,
				0.45,
				HERCULES_ACTIVATION_IMPACT_DIRT_PARTICLE_SPEED
			);
		}
		world.playSound(
			null,
			x,
			y,
			z,
			SoundEvents.BLOCK_ANVIL_LAND,
			SoundCategory.PLAYERS,
			HERCULES_ACTIVATION_IMPACT_SOUND_VOLUME,
			HERCULES_ACTIVATION_IMPACT_SOUND_PITCH
		);
	}

	private static void spawnHerculesBurdenParticles(LivingEntity target) {
		if (!(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		world.spawnParticles(ParticleTypes.GLOW, target.getX(), target.getBodyY(0.55), target.getZ(), 10, 0.35, 0.45, 0.35, 0.01);
		world.spawnParticles(SAGITTARIUS_BEAM_PARTICLE, target.getX(), target.getBodyY(0.55), target.getZ(), 8, 0.25, 0.35, 0.25, 0.0);
	}

	private static void spawnSagittariusChargeParticles(LivingEntity caster) {
		if (!(caster.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		if (SAGITTARIUS_CHARGE_GLOW_PARTICLE_COUNT > 0) {
			world.spawnParticles(
				ParticleTypes.GLOW,
				caster.getX(),
				caster.getBodyY(0.7),
				caster.getZ(),
				SAGITTARIUS_CHARGE_GLOW_PARTICLE_COUNT,
				0.18,
				0.22,
				0.18,
				0.01
			);
		}
		if (SAGITTARIUS_CHARGE_BEAM_PARTICLE_COUNT > 0) {
			world.spawnParticles(
				SAGITTARIUS_BEAM_PARTICLE,
				caster.getX(),
				caster.getBodyY(0.7),
				caster.getZ(),
				SAGITTARIUS_CHARGE_BEAM_PARTICLE_COUNT,
				0.12,
				0.18,
				0.12,
				0.0
			);
		}
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
			startAbilityCooldownFromNow(entry.getKey(), MagicAbility.MANIPULATION, server.getTicks());
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
		float[] forcedAngles = computeFacingAngles(target, caster.getEyePos());
		teleportDomainEntity(target, state.lockedX, state.lockedY, state.lockedZ, forcedAngles[0], forcedAngles[1]);
		if (LOVE_AT_FIRST_SIGHT_BLOCK_ITEM_USE) {
			target.stopUsingItem();
		}
		refreshStatusEffect(target, StatusEffects.SLOWNESS, LOVE_LOCK_EFFECT_TICKS, LOVE_LOCK_SLOWNESS_AMPLIFIER, true, false, false);
		refreshStatusEffect(
			target,
			StatusEffects.MINING_FATIGUE,
			LOVE_LOCK_EFFECT_TICKS,
			LOVE_LOCK_MINING_FATIGUE_AMPLIFIER,
			true,
			false,
			false
		);
		refreshStatusEffect(target, StatusEffects.RESISTANCE, LOVE_LOCK_EFFECT_TICKS, LOVE_AT_FIRST_SIGHT_RESISTANCE_AMPLIFIER, true, false, false);
		if (currentTick - state.lastParticleTick >= LOVE_AT_FIRST_SIGHT_PARTICLE_INTERVAL_TICKS) {
			spawnLoveAtFirstSightTargetParticles(target);
			state.lastParticleTick = currentTick;
		}

		if (target instanceof MobEntity mob) {
			mob.getNavigation().stop();
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
				refreshStatusEffect(livingTarget, StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, slownessAmplifier, true, true, true);
				spawnHitboxShardParticles(livingTarget, shardCount);
			}
		}
	}

	private static ActionResult onUseItem(PlayerEntity player, World world, Hand hand) {
		if (shouldBlockArtifactUse(player, hand)) {
			return ActionResult.FAIL;
		}

		ItemStack stack = player.getStackInHand(hand);
		if (isFrostTeleportItemBlocked(player, stack)) {
			if (player instanceof ServerPlayerEntity serverPlayer) {
				serverPlayer.sendMessage(Text.translatable("message.magic.frost.teleport_blocked"), true);
			}
			return ActionResult.FAIL;
		}
		if (isLoveItemUseBlocked(player)) {
			return ActionResult.FAIL;
		}

		if (isDomainClashParticipantFrozen(player) && !canUseWindChargeWhileLocked(player, stack)) {
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
		if (isItemUseBlocked(player)) {
			return ActionResult.FAIL;
		}
		if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
			GreedRuntime.onUseBlock(serverPlayer, world, hitResult.getBlockPos(), world.getBlockState(hitResult.getBlockPos()), player.getStackInHand(hand));
		}
		return ActionResult.PASS;
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
		if (isProtectedDomainDecoration(entity)) {
			sendDomainProtectedMessage(player);
			return ActionResult.FAIL;
		}
		return isItemUseBlocked(player) ? ActionResult.FAIL : ActionResult.PASS;
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
		return isAttackBlocked(player) ? ActionResult.FAIL : ActionResult.PASS;
	}

	private static boolean onBeforeBlockBreak(World world, PlayerEntity player, BlockPos pos) {
		if (shouldBlockArtifactAttack(player)) {
			return false;
		}
		if (isAttackBlocked(player)) {
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
		if (isAttackBlocked(player)) {
			return ActionResult.FAIL;
		}
		if (shouldBlockArtifactAttack(player)) {
			return ActionResult.FAIL;
		}

		if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return ActionResult.PASS;
		}

		if (!(entity instanceof LivingEntity target) || !target.isAlive() || target == player) {
			if (isProtectedDomainDecoration(entity)) {
				sendDomainProtectedMessage(serverPlayer);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		}

		if (isDirectCasterDamageBlocked(serverPlayer, target, serverPlayer.getEntityWorld().getServer().getTicks())) {
			return ActionResult.FAIL;
		}

		GreedRuntime.onAttackEntity(serverPlayer, entity);

		if (MagicPlayerData.getSchool(serverPlayer) != MagicSchool.FROST) {
			return ActionResult.PASS;
		}

		int currentTick = serverPlayer.getEntityWorld().getServer().getTicks();
		if (activeAbility(serverPlayer) != MagicAbility.BELOW_FREEZING) {
			return ActionResult.PASS;
		}

		applyFrostStageHitEffects(serverPlayer, target, currentTick);
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

	private static void applyOrRefreshFrostbite(LivingEntity target, UUID casterId, int currentTick) {
		applyOrRefreshFrostbite(
			target,
			casterId,
			currentTick,
			FROSTBITE_DURATION_TICKS,
			FROSTBITE_DAMAGE,
			FROST_CONFIG.debuff.refreshDurationOnReapply,
			FROST_CONFIG.debuff.stackDamageOnReapply
		);
	}

	private static void applyOrRefreshFrostbite(
		LivingEntity target,
		UUID casterId,
		int currentTick,
		int durationTicks,
		float damagePerTick,
		boolean refreshDuration,
		boolean stackDamage
	) {
		UUID targetId = target.getUuid();
		RegistryKey<World> dimension = target.getEntityWorld().getRegistryKey();
		int expiresTick = currentTick + Math.max(0, durationTicks);
		FrostbiteState state = FROSTBITTEN_TARGETS.get(targetId);
		boolean newApplication = false;
		if (state == null || state.dimension != dimension) {
			state = new FrostbiteState(
				dimension,
				casterId,
				expiresTick,
				currentTick + FROSTBITE_DAMAGE_INTERVAL_TICKS,
				Math.max(0.0F, damagePerTick)
			);
			FROSTBITTEN_TARGETS.put(targetId, state);
			newApplication = true;
		} else {
			state.casterId = casterId;
			state.damagePerTick = stackDamage ? state.damagePerTick + Math.max(0.0F, damagePerTick) : Math.max(state.damagePerTick, Math.max(0.0F, damagePerTick));
			if (refreshDuration) {
				state.expiresTick = expiresTick;
			} else {
				state.expiresTick = Math.max(state.expiresTick, expiresTick);
			}
		}

		applyFrostbiteControlEffects(target);
		spawnTargetSnowParticles(target);
		spawnHitboxShardParticles(target, 5);
		if (newApplication && POWDERED_SNOW_DAMAGE_ON_INITIAL_APPLICATION && state.damagePerTick > 0.0F) {
			dealFrostbiteDamage(casterId, target, state.damagePerTick);
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

			if (state.damagePerTick > 0.0F && currentTick >= state.nextDamageTick) {
				dealFrostbiteDamage(state.casterId, target, state.damagePerTick);
				state.nextDamageTick = currentTick + FROSTBITE_DAMAGE_INTERVAL_TICKS;
			}
		}
	}

	private static void applyOrRefreshEnhancedFire(LivingEntity target, UUID casterId, int currentTick, int durationTicks) {
		UUID targetId = target.getUuid();
		RegistryKey<World> dimension = target.getEntityWorld().getRegistryKey();
		int expiresTick = currentTick + durationTicks;
		EnhancedFireState existingState = ENHANCED_FIRE_TARGETS.get(targetId);

		if (existingState == null || !existingState.dimension.equals(dimension)) {
			ENHANCED_FIRE_TARGETS.put(
				targetId,
				new EnhancedFireState(dimension, casterId, expiresTick, currentTick + PLANCK_HEAT_ENHANCED_FIRE_DAMAGE_INTERVAL_TICKS)
			);
		} else {
			existingState.casterId = casterId;
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
				dealEnhancedFireDamage(state.casterId, target);
				state.nextDamageTick = currentTick + PLANCK_HEAT_ENHANCED_FIRE_DAMAGE_INTERVAL_TICKS;
			}
		}
	}

	private static void applyMartyrsFlameFire(LivingEntity target, int currentTick) {
		if (MARTYRS_FLAME_FIRE_DURATION_TICKS <= 0) {
			return;
		}

		target.setOnFireForTicks(MARTYRS_FLAME_FIRE_DURATION_TICKS);
		if (!MARTYRS_FLAME_FIRE_IGNORES_NORMAL_EXTINGUISH) {
			return;
		}

		int expiresTick = currentTick + MARTYRS_FLAME_FIRE_DURATION_TICKS;
		RegistryKey<World> dimension = target.getEntityWorld().getRegistryKey();
		MartyrsFlameBurnState state = MARTYRS_FLAME_BURNING_TARGETS.get(target.getUuid());
		if (state == null || !state.dimension.equals(dimension)) {
			MARTYRS_FLAME_BURNING_TARGETS.put(target.getUuid(), new MartyrsFlameBurnState(dimension, expiresTick));
		} else {
			state.expiresTick = Math.max(state.expiresTick, expiresTick);
		}

		target.setFireTicks(Math.max(target.getFireTicks(), MARTYRS_FLAME_FIRE_DURATION_TICKS));
	}

	private static void updateMartyrsFlameBurningTargets(MinecraftServer server, int currentTick) {
		if (!MARTYRS_FLAME_FIRE_IGNORES_NORMAL_EXTINGUISH) {
			MARTYRS_FLAME_BURNING_TARGETS.clear();
			return;
		}

		Iterator<Map.Entry<UUID, MartyrsFlameBurnState>> iterator = MARTYRS_FLAME_BURNING_TARGETS.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, MartyrsFlameBurnState> entry = iterator.next();
			MartyrsFlameBurnState state = entry.getValue();
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
				target.extinguish();
				iterator.remove();
				continue;
			}

			int remainingTicks = Math.max(1, state.expiresTick - currentTick + 1);
			target.setFireTicks(Math.max(target.getFireTicks(), remainingTicks));
		}
	}

	private static void applyFrostbiteControlEffects(LivingEntity target) {
		target.setFrozenTicks(Math.max(target.getFrozenTicks(), FROST_VISUAL_TICKS));
	}

	private static void dealFrostbiteDamage(UUID casterId, LivingEntity target, float amount) {
		if (target.getEntityWorld() instanceof ServerWorld world) {
			if (FROST_CONFIG.debuff.bypassTotems) {
				float nextHealth = target.getHealth() - amount;
				if (nextHealth <= 0.0F) {
					target.kill(world);
					return;
				}
				target.setHealth(nextHealth);
				return;
			}
			dealTrackedMagicDamage(target, casterId, world.getDamageSources().genericKill(), amount);
		}
	}

	private static void dealAbsoluteZeroAuraDamage(UUID casterId, LivingEntity target) {
		if (target.getEntityWorld() instanceof ServerWorld world) {
			dealTrackedMagicDamage(target, casterId, world.getDamageSources().genericKill(), ABSOLUTE_ZERO_AURA_DAMAGE);
		}
	}

	private static void dealPlanckHeatFrostDamage(UUID casterId, LivingEntity target) {
		if (target.getEntityWorld() instanceof ServerWorld world) {
			dealTrackedMagicDamage(target, casterId, world.getDamageSources().freeze(), PLANCK_HEAT_FROST_DAMAGE);
		}
	}

	private static void dealEnhancedFireDamage(UUID casterId, LivingEntity target) {
		if (target.getEntityWorld() instanceof ServerWorld world) {
			dealTrackedMagicDamage(target, casterId, world.getDamageSources().onFire(), PLANCK_HEAT_ENHANCED_FIRE_DAMAGE);
		}
	}

	private static void dealTrackedMagicDamage(LivingEntity target, UUID casterId, DamageSource source, float amount) {
		if (!(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		UUID targetId = target.getUuid();
		MAGIC_DAMAGE_PENDING_ATTACKER.put(targetId, casterId);
		try {
			target.damage(world, source, amount);
		} finally {
			MAGIC_DAMAGE_PENDING_ATTACKER.remove(targetId, casterId);
		}
	}

	private static void cleanseCommonNegativeEffects(LivingEntity entity) {
		for (var effect : COMMON_NEGATIVE_EFFECTS) {
			entity.removeStatusEffect(effect);
		}
	}

	private static void refreshStatusEffect(
		LivingEntity entity,
		RegistryEntry<StatusEffect> effect,
		int durationTicks,
		int amplifier,
		boolean ambient,
		boolean showParticles,
		boolean showIcon
	) {
		if (durationTicks <= 0 || amplifier < 0) {
			return;
		}

		StatusEffectInstance existing = entity.getStatusEffect(effect);
		if (existing != null) {
			int refreshThreshold = Math.max(1, durationTicks / 2);
			if (existing.getAmplifier() > amplifier && existing.getDuration() > refreshThreshold) {
				return;
			}
			if (
				existing.getAmplifier() == amplifier
				&& existing.isAmbient() == ambient
				&& existing.shouldShowParticles() == showParticles
				&& existing.shouldShowIcon() == showIcon
				&& existing.getDuration() > refreshThreshold
			) {
				return;
			}
		}

		entity.addStatusEffect(new StatusEffectInstance(effect, durationTicks, amplifier, ambient, showParticles, showIcon));
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

	private static void spawnMartyrsFlameCasterParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getY() + 0.15;
		double z = entity.getZ();
		if (MARTYRS_FLAME_FIRE_FLAME_PARTICLE_COUNT > 0) {
			world.spawnParticles(
				ParticleTypes.FLAME,
				x,
				y,
				z,
				MARTYRS_FLAME_FIRE_FLAME_PARTICLE_COUNT,
				0.35,
				0.6,
				0.35,
				0.01
			);
		}
		if (MARTYRS_FLAME_FIRE_SMOKE_PARTICLE_COUNT > 0) {
			world.spawnParticles(
				ParticleTypes.SMALL_FLAME,
				x,
				y + 0.15,
				z,
				MARTYRS_FLAME_FIRE_SMOKE_PARTICLE_COUNT,
				0.25,
				0.5,
				0.25,
				0.005
			);
		}
	}

	private static void spawnOrionsGambitTargetParticles(LivingEntity target) {
		if (!(target.getEntityWorld() instanceof ServerWorld world) || ORIONS_GAMBIT_TARGET_PARTICLE_BURST_COUNT <= 0) {
			return;
		}

		Vec3d forward = target.getRotationVec(1.0F);
		Vec3d horizontalForward = new Vec3d(forward.x, 0.0, forward.z);
		if (horizontalForward.lengthSquared() < 1.0E-6) {
			horizontalForward = new Vec3d(0.8, 0.0, 0.2);
		} else {
			horizontalForward = horizontalForward.normalize();
		}

		Vec3d side = new Vec3d(-horizontalForward.z, 0.0, horizontalForward.x);
		for (int index = 0; index < ORIONS_GAMBIT_TARGET_PARTICLE_BURST_COUNT; index++) {
			double feetX = target.getX() + target.getRandom().nextDouble() * ORIONS_GAMBIT_TARGET_PARTICLE_SPAWN_RADIUS * 2.0 - ORIONS_GAMBIT_TARGET_PARTICLE_SPAWN_RADIUS;
			double feetY = target.getY() + target.getRandom().nextDouble() * 0.18;
			double feetZ = target.getZ() + target.getRandom().nextDouble() * ORIONS_GAMBIT_TARGET_PARTICLE_SPAWN_RADIUS * 2.0 - ORIONS_GAMBIT_TARGET_PARTICLE_SPAWN_RADIUS;
			double sideDirection = target.getRandom().nextBoolean() ? 1.0 : -1.0;
			double velocityX = horizontalForward.x * ORIONS_GAMBIT_TARGET_PARTICLE_FORWARD_VELOCITY + side.x * ORIONS_GAMBIT_TARGET_PARTICLE_SIDE_VELOCITY * sideDirection;
			double velocityY = ORIONS_GAMBIT_TARGET_PARTICLE_VERTICAL_VELOCITY + target.getRandom().nextDouble() * 0.04;
			double velocityZ = horizontalForward.z * ORIONS_GAMBIT_TARGET_PARTICLE_FORWARD_VELOCITY + side.z * ORIONS_GAMBIT_TARGET_PARTICLE_SIDE_VELOCITY * sideDirection;
			world.spawnParticles(ParticleTypes.END_ROD, feetX, feetY, feetZ, 0, velocityX, velocityY, velocityZ, 1.0);
			world.spawnParticles(ParticleTypes.ENCHANT, feetX, feetY, feetZ, 0, velocityX * 0.55, velocityY * 0.75, velocityZ * 0.55, 1.0);
			world.spawnParticles(ParticleTypes.GLOW, feetX, feetY, feetZ, 0, velocityX * 0.35, velocityY * 0.6, velocityZ * 0.35, 1.0);
		}
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
		for (int i = 0; i < count; i++) {
			double x = box.minX + entity.getRandom().nextDouble() * box.getLengthX();
			double y = box.minY + entity.getRandom().nextDouble() * box.getLengthY();
			double z = box.minZ + entity.getRandom().nextDouble() * box.getLengthZ();
			world.spawnParticles(FROST_SHARD_PARTICLE, x, y, z, 1, 0.08, 0.12, 0.08, 0.005);
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
				MagicAbility.FROST_ASCENT,
				MagicAbility.PLANCK_HEAT,
				MagicAbility.ABSOLUTE_ZERO,
				MagicAbility.FROST_DOMAIN_EXPANSION
			);
			case LOVE -> List.of(
				MagicAbility.LOVE_AT_FIRST_SIGHT,
				MagicAbility.TILL_DEATH_DO_US_PART,
				MagicAbility.MANIPULATION,
				MagicAbility.LOVE_DOMAIN_EXPANSION
			);
			case JESTER -> List.of(
				MagicAbility.SPOTLIGHT,
				MagicAbility.WITTY_ONE_LINER,
				MagicAbility.COMEDIC_REWRITE,
				MagicAbility.COMEDIC_ASSISTANT,
				MagicAbility.PLUS_ULTRA
			);
			case CONSTELLATION -> List.of(
				MagicAbility.CASSIOPEIA,
				MagicAbility.HERCULES_BURDEN_OF_THE_SKY,
				MagicAbility.SAGITTARIUS_ASTRAL_ARROW,
				MagicAbility.ORIONS_GAMBIT,
				MagicAbility.ASTRAL_CATACLYSM
			);
			case BURNING_PASSION -> List.of(MagicAbility.MARTYRS_FLAME);
			case GREED -> List.of(
				MagicAbility.APPRAISERS_MARK,
				MagicAbility.TOLLKEEPERS_CLAIM,
				MagicAbility.KINGS_DUES,
				MagicAbility.BANKRUPTCY
			);
			default -> List.of();
		};
	}

	private static int cooldownRemaining(ServerPlayerEntity player, MagicAbility ability, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}

		return switch (ability) {
			case MARTYRS_FLAME -> martyrsFlameCooldownRemaining(player, currentTick);
			case BELOW_FREEZING -> belowFreezingCooldownRemaining(player, currentTick);
			case FROST_ASCENT -> frostAscentCooldownRemaining(player, currentTick);
			case WITTY_ONE_LINER -> wittyOneLinerCooldownRemaining(player, currentTick);
			case COMEDIC_REWRITE -> comedicRewriteCooldownRemaining(player, currentTick);
			case COMEDIC_ASSISTANT -> comedicAssistantCooldownRemaining(player, currentTick);
			case PLUS_ULTRA -> plusUltraCooldownRemaining(player, currentTick);
			case ABSOLUTE_ZERO -> absoluteZeroCooldownRemaining(player, currentTick);
			case PLANCK_HEAT -> planckHeatCooldownRemaining(player, currentTick);
			case TILL_DEATH_DO_US_PART -> tillDeathDoUsPartCooldownRemaining(player, currentTick);
			case MANIPULATION -> manipulationCooldownRemaining(player, currentTick);
			case HERCULES_BURDEN_OF_THE_SKY -> herculesBurdenCooldownRemaining(player, currentTick);
			case SAGITTARIUS_ASTRAL_ARROW -> sagittariusAstralArrowCooldownRemaining(player, currentTick);
			case ORIONS_GAMBIT -> orionsGambitCooldownRemaining(player, currentTick);
			case APPRAISERS_MARK, TOLLKEEPERS_CLAIM, KINGS_DUES, BANKRUPTCY -> GreedRuntime.cooldownRemaining(player, ability, currentTick);
			case FROST_DOMAIN_EXPANSION, LOVE_DOMAIN_EXPANSION, ASTRAL_CATACLYSM -> domainCooldownRemaining(player, ability, currentTick);
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

	private static boolean isLockedByForeignLoveDomain(PlayerEntity player) {
		return isEntityCapturedByLoveDomain(player) && !ownsLoveDomain(player.getUuid());
	}

	private static boolean canUseWindChargeWhileLocked(PlayerEntity player, ItemStack stack) {
		if (!stack.isOf(Items.WIND_CHARGE)) {
			return false;
		}

		UUID playerId = player.getUuid();
		return ownsLoveDomain(playerId) || domainClashStateForParticipant(playerId) != null;
	}

	private static void applyDomainClashStartEffects(ServerPlayerEntity player) {
		applyDomainClashCombatEffects(player);
	}

	private static void applyDomainClashCombatEffects(ServerPlayerEntity player) {
		player.addStatusEffect(
			new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, DOMAIN_CLASH_INSTANT_HEALTH_AMPLIFIER, true, false, true)
		);
		refreshStatusEffect(
			player,
			StatusEffects.REGENERATION,
			DOMAIN_CLASH_REGENERATION_REFRESH_TICKS,
			DOMAIN_CLASH_REGENERATION_AMPLIFIER,
			true,
			false,
			true
		);
		refreshStatusEffect(
			player,
			StatusEffects.RESISTANCE,
			DOMAIN_CLASH_REGENERATION_REFRESH_TICKS,
			DOMAIN_CLASH_RESISTANCE_AMPLIFIER,
			true,
			false,
			true
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

		runWithDomainTeleportBypass(() ->
			player.teleport(
				targetWorld,
				pendingReturn.x,
				pendingReturn.y,
				pendingReturn.z,
				Set.<PositionFlag>of(),
				pendingReturn.yaw,
				pendingReturn.pitch,
				false
			)
		);
		player.setVelocity(0.0, 0.0, 0.0);
		player.setOnGround(true);
		persistDomainRuntimeState(server);
	}

	private static int absoluteZeroCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, ABSOLUTE_ZERO_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int belowFreezingCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player) || isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, BELOW_FREEZING_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int frostAscentCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player) || isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, FROST_ASCENT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int martyrsFlameCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, MARTYRS_FLAME_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int planckHeatCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, PLANCK_HEAT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int tillDeathDoUsPartCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int domainCooldownRemaining(ServerPlayerEntity player, MagicAbility ability, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}

		UUID playerId = player.getUuid();
		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			return Math.max(0, FROST_DOMAIN_COOLDOWN_END_TICK.getOrDefault(playerId, 0) - currentTick);
		}
		if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return Math.max(0, LOVE_DOMAIN_COOLDOWN_END_TICK.getOrDefault(playerId, 0) - currentTick);
		}
		if (ability == MagicAbility.ASTRAL_CATACLYSM) {
			return Math.max(0, ASTRAL_CATACLYSM_COOLDOWN_END_TICK.getOrDefault(playerId, 0) - currentTick);
		}
		return 0;
	}

	private static void startDomainCooldown(UUID playerId, MagicAbility ability, int currentTick, double multiplier) {
		if (isCooldownDeferredByOrionsGambit(playerId, ability)) {
			return;
		}

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
			return;
		}

		if (ability == MagicAbility.ASTRAL_CATACLYSM) {
			ASTRAL_CATACLYSM_COOLDOWN_END_TICK.put(playerId, currentTick + cooldownTicks);
		}
	}

	private static void applyDomainEndCooldowns(
		UUID playerId,
		MagicAbility domainAbility,
		int currentTick,
		double domainCooldownMultiplier,
		boolean applySchoolCooldowns
	) {
		if (!applySchoolCooldowns) {
			startDomainCooldown(playerId, domainAbility, currentTick, domainCooldownMultiplier);
			return;
		}

		for (MagicAbility ability : abilitiesForSchool(domainAbility.school())) {
			if (ability == domainAbility) {
				startDomainCooldown(playerId, ability, currentTick, domainCooldownMultiplier);
				continue;
			}
			startAbilityCooldownFromNow(playerId, ability, currentTick);
		}
	}

	private static int domainCooldownTicks(MagicAbility ability) {
		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			return Math.max(0, FROST_DOMAIN_COOLDOWN_TICKS);
		}
		if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return Math.max(0, LOVE_DOMAIN_COOLDOWN_TICKS);
		}
		if (ability == MagicAbility.ASTRAL_CATACLYSM) {
			return Math.max(0, ASTRAL_CATACLYSM_COOLDOWN_TICKS);
		}
		return 0;
	}

	private static int domainExpiresTick(MagicAbility ability, int currentTick) {
		if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return Integer.MAX_VALUE;
		}
		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			int durationTicks = Math.max(0, FROST_CONFIG.domain.startupTicks) + Math.max(1, FROST_CONFIG.domain.totalDurationTicks);
			if (FROST_CONFIG.domain.finalExecutionEnabled) {
				durationTicks += Math.max(0, FROST_CONFIG.domain.finalExecutionDelayTicks);
			}
			return currentTick + Math.max(TICKS_PER_SECOND, durationTicks);
		}

		int durationTicks = ability == MagicAbility.ASTRAL_CATACLYSM
			? ASTRAL_CATACLYSM_DURATION_TICKS
			: DOMAIN_EXPANSION_DURATION_TICKS;
		return currentTick + Math.max(TICKS_PER_SECOND, durationTicks);
	}

	private static void startAbilityCooldownFromNow(UUID playerId, MagicAbility ability, int currentTick) {
		if (ability == MagicAbility.BELOW_FREEZING) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || BELOW_FREEZING_COOLDOWN_TICKS <= 0) {
				BELOW_FREEZING_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			BELOW_FREEZING_COOLDOWN_END_TICK.put(playerId, currentTick + BELOW_FREEZING_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.FROST_ASCENT) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || FROST_ASCENT_COOLDOWN_TICKS <= 0) {
				FROST_ASCENT_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			FROST_ASCENT_COOLDOWN_END_TICK.put(playerId, currentTick + FROST_ASCENT_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.MARTYRS_FLAME) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || MARTYRS_FLAME_COOLDOWN_TICKS <= 0) {
				MARTYRS_FLAME_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			MARTYRS_FLAME_COOLDOWN_END_TICK.put(playerId, currentTick + MARTYRS_FLAME_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.ABSOLUTE_ZERO) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || ABSOLUTE_ZERO_COOLDOWN_TICKS <= 0) {
				ABSOLUTE_ZERO_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			ABSOLUTE_ZERO_COOLDOWN_END_TICK.put(playerId, currentTick + ABSOLUTE_ZERO_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.PLANCK_HEAT) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || PLANCK_HEAT_COOLDOWN_TICKS <= 0) {
				PLANCK_HEAT_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			PLANCK_HEAT_COOLDOWN_END_TICK.put(playerId, currentTick + PLANCK_HEAT_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.WITTY_ONE_LINER) {
			WittyOneLinerTierSettings tier = WITTY_ONE_LINER_HIGH_TIER;
			int cooldownTicks = Math.max(
				WITTY_ONE_LINER_LOW_TIER.cooldownTicks(),
				Math.max(WITTY_ONE_LINER_MID_TIER.cooldownTicks(), tier.cooldownTicks())
			);
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || cooldownTicks <= 0) {
				WITTY_ONE_LINER_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			WITTY_ONE_LINER_COOLDOWN_END_TICK.put(playerId, currentTick + cooldownTicks);
			return;
		}

		if (ability == MagicAbility.COMEDIC_REWRITE) {
			startComedicRewriteCooldown(playerId, currentTick);
			return;
		}

		if (ability == MagicAbility.COMEDIC_ASSISTANT) {
			startComedicAssistantCooldown(playerId, currentTick, COMEDIC_ASSISTANT_PROC_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.PLUS_ULTRA) {
			startPlusUltraCooldown(playerId, currentTick, PLUS_ULTRA_FULL_END_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.TILL_DEATH_DO_US_PART) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || TILL_DEATH_DO_US_PART_COOLDOWN_TICKS <= 0) {
				TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.put(playerId, currentTick + TILL_DEATH_DO_US_PART_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.MANIPULATION) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || MANIPULATION_COOLDOWN_TICKS <= 0) {
				MANIPULATION_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			MANIPULATION_COOLDOWN_END_TICK.put(playerId, currentTick + MANIPULATION_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || HERCULES_COOLDOWN_TICKS <= 0) {
				HERCULES_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			HERCULES_COOLDOWN_END_TICK.put(playerId, currentTick + HERCULES_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || SAGITTARIUS_COOLDOWN_TICKS <= 0) {
				SAGITTARIUS_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			SAGITTARIUS_COOLDOWN_END_TICK.put(playerId, currentTick + SAGITTARIUS_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.ORIONS_GAMBIT) {
			if (ORIONS_GAMBIT_COOLDOWN_TICKS <= 0) {
				ORIONS_GAMBIT_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			ORIONS_GAMBIT_COOLDOWN_END_TICK.put(playerId, currentTick + ORIONS_GAMBIT_COOLDOWN_TICKS);
			return;
		}

		if (
			ability == MagicAbility.APPRAISERS_MARK
			|| ability == MagicAbility.TOLLKEEPERS_CLAIM
			|| ability == MagicAbility.KINGS_DUES
			|| ability == MagicAbility.BANKRUPTCY
		) {
			GreedRuntime.startAbilityCooldownFromNow(playerId, ability, currentTick);
			return;
		}

		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION || ability == MagicAbility.LOVE_DOMAIN_EXPANSION || ability == MagicAbility.ASTRAL_CATACLYSM) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability)) {
				if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
					FROST_DOMAIN_COOLDOWN_END_TICK.remove(playerId);
				} else if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
					LOVE_DOMAIN_COOLDOWN_END_TICK.remove(playerId);
				} else {
					ASTRAL_CATACLYSM_COOLDOWN_END_TICK.remove(playerId);
				}
				return;
			}
			startDomainCooldown(playerId, ability, currentTick, 1.0);
		}
	}

	private static boolean isCooldownDeferredByOrionsGambit(UUID playerId, MagicAbility ability) {
		UUID casterId = ORIONS_GAMBIT_CASTER_BY_TARGET.get(playerId);
		if (casterId == null) {
			return false;
		}

		OrionGambitState state = ORIONS_GAMBIT_STATES.get(casterId);
		if (state == null || state.stage != OrionGambitStage.LINKED || !playerId.equals(state.targetId)) {
			return false;
		}

		state.usedTargetAbilities.add(ability);
		return ORIONS_GAMBIT_SUPPRESS_TARGET_COOLDOWNS;
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
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, MANIPULATION_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int herculesBurdenCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, HERCULES_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int sagittariusAstralArrowCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, SAGITTARIUS_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	private static int orionsGambitCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		return Math.max(0, ORIONS_GAMBIT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
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
		return ability == MagicAbility.FROST_DOMAIN_EXPANSION
			|| ability == MagicAbility.LOVE_DOMAIN_EXPANSION
			|| ability == MagicAbility.ASTRAL_CATACLYSM;
	}

	private static boolean isMaximumAbility(MagicAbility ability) {
		return ability == MagicAbility.PLUS_ULTRA || ability == MagicAbility.ABSOLUTE_ZERO;
	}

	private static boolean isBankruptcyProtectedAbility(
		MagicAbility ability,
		boolean preserveMaximumAbilities,
		boolean preserveDomainAbilities
	) {
		if (ability == MagicAbility.NONE) {
			return false;
		}
		if (preserveDomainAbilities && isDomainExpansion(ability)) {
			return true;
		}
		return preserveMaximumAbilities && isMaximumAbility(ability);
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

	private static boolean hasProtectedDecorationEntity(ServerWorld world, BlockPos pos) {
		Box searchBox = new Box(
			pos.getX(),
			pos.getY(),
			pos.getZ(),
			pos.getX() + 1.0,
			pos.getY() + 1.0,
			pos.getZ() + 1.0
		).expand(0.75);
		return !world.getEntitiesByClass(
			BlockAttachedEntity.class,
			searchBox,
			decoration -> decoration.getAttachedBlockPos().equals(pos)
		).isEmpty();
	}

	private static boolean isProtectedDomainDecoration(Entity entity) {
		if (!(entity instanceof BlockAttachedEntity decoration)) {
			return false;
		}

		RegistryKey<World> dimension = entity.getEntityWorld().getRegistryKey();
		return isDomainBlockProtected(dimension, decoration.getAttachedBlockPos())
			|| isPositionInsideAnyDomain(dimension, entity.getX(), entity.getY(), entity.getZ());
	}

	private static void sendDomainProtectedMessage(PlayerEntity player) {
		if (player instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.sendMessage(Text.translatable("message.magic.domain.unbreakable"), true);
		}
	}

	private static boolean setDomainBlockState(ServerWorld world, BlockPos pos, BlockState targetState, int flags) {
		BlockState previousState = world.getBlockState(pos);
		boolean changed = world.setBlockState(pos, targetState, flags);
		if (changed && (previousState.isOf(Blocks.LIGHT) || targetState.isOf(Blocks.LIGHT))) {
			world.getLightingProvider().checkBlock(pos);
			world.getChunkManager().markForUpdate(pos);
		}
		return changed;
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
		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			return resolveFrostDomainState(shell, pos, centerX, centerZ, baseY, radius, height);
		}

		if (ability == MagicAbility.ASTRAL_CATACLYSM) {
			return resolveConstellationDomainState(shell, pos, centerX, centerZ, baseY, height, innerRadius, innerHeight);
		}

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

	private static BlockState resolveFrostDomainState(
		boolean shell,
		BlockPos pos,
		int centerX,
		int centerZ,
		int baseY,
		int radius,
		int height
	) {
		int relativeY = pos.getY() - baseY;
		if (relativeY == 0) {
			return resolveFrostDomainFloorState(pos, centerX, centerZ, radius);
		}
		if (!shell) {
			return resolveFrostDomainInteriorState(pos, relativeY, centerX, centerZ, radius, height);
		}
		return resolveFrostDomainShellState(pos, centerX, centerZ, relativeY, height);
	}

	private static BlockState resolveFrostDomainShellState(
		BlockPos pos,
		int centerX,
		int centerZ,
		int relativeY,
		int height
	) {
		int dx = Math.abs(pos.getX() - centerX);
		int dz = Math.abs(pos.getZ() - centerZ);
		int variant = Math.floorMod(decorHash(pos) + dx * 3 + dz * 5 + relativeY * 7, 12);
		boolean topLayer = relativeY >= height - 2;
		if (topLayer) {
			if (variant < 5) {
				return Blocks.BLUE_ICE.getDefaultState();
			}
			if (variant < 9) {
				return Blocks.PACKED_ICE.getDefaultState();
			}
			return Blocks.ICE.getDefaultState();
		}
		if ((dx + dz + relativeY) % 7 == 0) {
			return Blocks.BLUE_ICE.getDefaultState();
		}
		if (variant < 2) {
			return Blocks.BLUE_ICE.getDefaultState();
		}
		if (variant < 8) {
			return Blocks.PACKED_ICE.getDefaultState();
		}
		return Blocks.ICE.getDefaultState();
	}

	private static BlockState resolveFrostDomainFloorState(BlockPos pos, int centerX, int centerZ, int radius) {
		int dx = Math.abs(pos.getX() - centerX);
		int dz = Math.abs(pos.getZ() - centerZ);
		if (isFrostDomainSnowflakeFloor(dx, dz, radius)) {
			if (Math.max(dx, dz) <= 1) {
				return Blocks.BLUE_ICE.getDefaultState();
			}
			if ((dx + dz) % 6 == 0 || dx == dz) {
				return Blocks.WHITE_CONCRETE.getDefaultState();
			}
			return Blocks.SNOW_BLOCK.getDefaultState();
		}
		int variant = Math.floorMod(decorHash(pos) + dx * 3 + dz * 5, 8);
		if (variant == 0) {
			return Blocks.BLUE_ICE.getDefaultState();
		}
		if (variant <= 4) {
			return Blocks.PACKED_ICE.getDefaultState();
		}
		return Blocks.ICE.getDefaultState();
	}

	private static BlockState resolveFrostDomainInteriorState(
		BlockPos pos,
		int relativeY,
		int centerX,
		int centerZ,
		int innerRadius,
		int innerHeight
	) {
		if (shouldPlaceFrostDomainLight(pos, relativeY, centerX, centerZ, innerRadius, innerHeight)) {
			return LOVE_DOMAIN_LIGHT_STATE;
		}
		return DOMAIN_INTERIOR_BLOCK_STATE;
	}

	private static boolean isFrostDomainSnowflakeFloor(int dx, int dz, int radius) {
		int armReach = Math.max(4, radius - 3);
		int branchReach = Math.max(2, radius / 2);
		boolean hub = dx + dz <= 2;
		boolean axialArm = (dx == 0 && dz <= armReach) || (dz == 0 && dx <= armReach);
		boolean diagonalArm = dx == dz && dx <= armReach - 1;
		boolean axialBranch = (
			dx <= 1
				&& dz > 1
				&& dz <= branchReach
				&& dz % 3 == 0
		) || (
			dz <= 1
				&& dx > 1
				&& dx <= branchReach
				&& dx % 3 == 0
		);
		boolean diagonalBranch = Math.abs(dx - dz) <= 1 && Math.max(dx, dz) <= branchReach && Math.max(dx, dz) % 3 == 0;
		int radiusSq = dx * dx + dz * dz;
		boolean ringAccent = radiusSq >= Math.max(4, (branchReach - 1) * (branchReach - 1))
			&& radiusSq <= branchReach * branchReach
			&& (dx == 0 || dz == 0 || dx == dz);
		return hub || axialArm || diagonalArm || axialBranch || diagonalBranch || ringAccent;
	}

	private static boolean shouldPlaceFrostDomainLight(
		BlockPos pos,
		int relativeY,
		int centerX,
		int centerZ,
		int innerRadius,
		int innerHeight
	) {
		if (relativeY < 1 || relativeY > Math.max(1, innerHeight - 2)) {
			return false;
		}
		if (relativeY != 1 && relativeY % 6 != 1) {
			return false;
		}

		int dx = Math.abs(pos.getX() - centerX);
		int dz = Math.abs(pos.getZ() - centerZ);
		if (dx + dz <= 1) {
			return true;
		}
		if (dx % 6 != 0 || dz % 6 != 0) {
			return false;
		}

		int horizontalDistanceSq = horizontalDistanceSq(pos.getX(), centerX, pos.getZ(), centerZ);
		return isInsideDomainDome(horizontalDistanceSq, relativeY, innerRadius, innerHeight);
	}

	private static BlockState resolveConstellationDomainState(
		boolean shell,
		BlockPos pos,
		int centerX,
		int centerZ,
		int baseY,
		int height,
		int innerRadius,
		int innerHeight
	) {
		int relativeY = pos.getY() - baseY;
		if (relativeY == 0) {
			return shouldDecorate(pos, 11) ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.BLACK_CONCRETE.getDefaultState();
		}

		if (shell) {
			if (relativeY >= height - 1) {
				return shouldDecorate(pos, 5) ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.BLACK_CONCRETE.getDefaultState();
			}
			return shouldDecorate(pos, 9) ? Blocks.BLUE_CONCRETE.getDefaultState() : Blocks.BLACK_CONCRETE.getDefaultState();
		}

		if (shouldPlaceConstellationDomainLight(pos, relativeY, centerX, centerZ, innerRadius, innerHeight)) {
			return LOVE_DOMAIN_LIGHT_STATE;
		}
		return DOMAIN_INTERIOR_BLOCK_STATE;
	}

	private static boolean shouldPlaceConstellationDomainLight(
		BlockPos pos,
		int relativeY,
		int centerX,
		int centerZ,
		int innerRadius,
		int innerHeight
	) {
		if (relativeY < 2 || relativeY >= innerHeight) {
			return false;
		}

		if (!shouldDecorate(pos, 12)) {
			return false;
		}

		int horizontalDistanceSq = horizontalDistanceSq(pos.getX(), centerX, pos.getZ(), centerZ);
		return isInsideDomainDome(horizontalDistanceSq, relativeY, innerRadius, innerHeight);
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

	private static void clearDeathAbilityState(ServerPlayerEntity player) {
		clearFrostControlledTargetState(player.getUuid(), player.getEntityWorld().getServer());
		MagicAbility activeAbility = activeAbility(player);
		boolean martyrsFlameActive = MARTYRS_FLAME_PASSIVE_ENABLED.contains(player.getUuid());
		if (activeAbility == MagicAbility.NONE && !martyrsFlameActive) {
			return;
		}

		if (martyrsFlameActive) {
			deactivateMartyrsFlame(player, true);
		}
		if (activeAbility == MagicAbility.BELOW_FREEZING) {
			endFrostStagedMode(player, player.getEntityWorld().getServer().getTicks(), FrostStageEndReason.CASTER_DIED, true, false);
		}
		if (activeAbility == MagicAbility.ABSOLUTE_ZERO) {
			clearFrostMaximumState(player, true);
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
		if (activeAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			deactivateHerculesBurden(player, true, false);
		}
		if (activeAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			clearSagittariusWindup(player);
		}
		if (activeAbility == MagicAbility.ORIONS_GAMBIT) {
			endOrionsGambit(player, OrionGambitEndReason.CASTER_DIED, player.getEntityWorld().getServer().getTicks(), false);
		}
		if (activeAbility == MagicAbility.PLUS_ULTRA) {
			endPlusUltra(player, player.getEntityWorld().getServer().getTicks(), PlusUltraEndMode.FULL, false);
		}

		setActiveAbility(player, MagicAbility.NONE);
	}

	private static void debugManipulation(String message, Object... args) {
	}

	public static void debugManipulationPacket(String message, Object... args) {
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
		return isLoveLocked(player)
			|| isDomainClashParticipantFrozen(player)
			|| isAstralExecutionTarget(player)
			|| isFrostMaximumFearLocked(player)
			|| isFrostHelpless(player);
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

	private static boolean isLoveLocked(PlayerEntity player) {
		return LOVE_LOCKED_TARGETS.containsKey(player.getUuid());
	}

	private static boolean isAstralExecutionTarget(PlayerEntity player) {
		return ASTRAL_EXECUTION_BEAMS.containsKey(player.getUuid());
	}

	private static boolean isDirectCasterDamageBlocked(ServerPlayerEntity attacker, LivingEntity target, int currentTick) {
		return isLoveCasterDamageBlocked(attacker, target, currentTick) || isHerculesCasterDamageBlocked(attacker, target, currentTick);
	}

	private static boolean isLoveCasterDamageBlocked(ServerPlayerEntity attacker, LivingEntity target, int currentTick) {
		if (!LOVE_AT_FIRST_SIGHT_PREVENT_CASTER_DIRECT_DAMAGE) {
			return false;
		}

		LoveLockState state = LOVE_LOCKED_TARGETS.get(target.getUuid());
		if (state == null || !attacker.getUuid().equals(state.casterId) || state.lastSeenTick != currentTick) {
			return false;
		}

		return activeAbility(attacker) == MagicAbility.LOVE_AT_FIRST_SIGHT;
	}

	private static boolean isHerculesCasterDamageBlocked(ServerPlayerEntity attacker, LivingEntity target, int currentTick) {
		if (!HERCULES_PREVENT_CASTER_DIRECT_DAMAGE) {
			return false;
		}

		AstralBurdenTargetState state = HERCULES_TARGETS.get(target.getUuid());
		return state != null && attacker.getUuid().equals(state.casterId) && currentTick < state.endTick;
	}

	private static boolean isLoveItemUseBlocked(PlayerEntity player) {
		return LOVE_AT_FIRST_SIGHT_BLOCK_ITEM_USE && isLoveLocked(player);
	}

	private static boolean isItemUseBlocked(PlayerEntity player) {
		return isLoveItemUseBlocked(player) || isDomainClashParticipantFrozen(player) || isAstralExecutionTarget(player) || isFrostHelpless(player);
	}

	private static boolean isAttackBlocked(PlayerEntity player) {
		return (LOVE_AT_FIRST_SIGHT_BLOCK_ATTACKS && isLoveLocked(player))
			|| isDomainClashParticipantFrozen(player)
			|| isAstralExecutionTarget(player)
			|| isFrostHelpless(player);
	}

	public static boolean isPlayerControlLocked(ServerPlayerEntity player) {
		return isControlLocked(player);
	}

	public static boolean shouldBlockHealing(LivingEntity entity) {
		return entity != null && isFrostHelpless(entity);
	}

	public static boolean shouldBlockFrostTeleport(Entity entity) {
		return entity != null && isFrostTeleportBlocked(entity);
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

		if (
			ability == MagicAbility.APPRAISERS_MARK ||
			ability == MagicAbility.TOLLKEEPERS_CLAIM ||
			ability == MagicAbility.KINGS_DUES ||
			ability == MagicAbility.BANKRUPTCY
		) {
			return GreedRuntime.resetCooldown(player, ability);
		}

		if (ability == MagicAbility.MARTYRS_FLAME) {
			MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
			return MARTYRS_FLAME_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.BELOW_FREEZING) {
			boolean removed = BELOW_FREEZING_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= FROST_STAGE_STATES.remove(playerId) != null;
			clearFrostEffectsByCaster(playerId, false, player.getEntityWorld().getServer());
			removeFrostStageCasterBuffs(player);
			if (activeAbility(player) == MagicAbility.BELOW_FREEZING) {
				setActiveAbility(player, MagicAbility.NONE);
				removed = true;
			}
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.FROST_ASCENT) {
			return FROST_ASCENT_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.SPOTLIGHT) {
			SpotlightState state = SPOTLIGHT_STATES.get(playerId);
			boolean changed = state != null && (state.currentStage() > 0 || !state.viewerLastLookTicks().isEmpty());
			resetSpotlightTracking(player);
			return changed ? 1 : 0;
		}

		if (ability == MagicAbility.WITTY_ONE_LINER) {
			return WITTY_ONE_LINER_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.COMEDIC_REWRITE) {
			boolean removed = COMEDIC_REWRITE_COOLDOWN_END_TICK.remove(playerId) != null;
			COMEDIC_REWRITE_IMMUNITY_END_TICK.remove(playerId);
			COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.remove(playerId);
			COMEDIC_REWRITE_PENDING_STATES.remove(playerId);
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.COMEDIC_ASSISTANT) {
			boolean removed = COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= COMEDIC_ASSISTANT_ARMED_STATES.remove(playerId) != null;
			COMEDIC_ASSISTANT_PARROT_CARRY_STATES.remove(playerId);
			removed |= clearComedicAssistantCaneImpactState(playerId);
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.PLUS_ULTRA) {
			boolean removed = PLUS_ULTRA_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= PLUS_ULTRA_STATES.containsKey(playerId) || activeAbility(player) == MagicAbility.PLUS_ULTRA;
			clearPlusUltraRuntimeState(player, true);
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			return HERCULES_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			return SAGITTARIUS_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.ORIONS_GAMBIT) {
			return ORIONS_GAMBIT_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.ABSOLUTE_ZERO) {
			boolean removed = ABSOLUTE_ZERO_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= FROST_MAXIMUM_STATES.remove(playerId) != null;
			clearFrostMaximumState(player, false);
			FROST_MANA_REGEN_BLOCKED_END_TICK.remove(playerId);
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.PLANCK_HEAT) {
			boolean removed = PLANCK_HEAT_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= FROST_SPIKE_WAVES.removeIf(state -> playerId.equals(state.casterId));
			return removed ? 1 : 0;
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

		if (ability == MagicAbility.ASTRAL_CATACLYSM) {
			boolean removed = ASTRAL_CATACLYSM_COOLDOWN_END_TICK.remove(playerId) != null;
			if (removed) {
				persistDomainRuntimeState(player.getEntityWorld().getServer());
			}
			return removed ? 1 : 0;
		}

		return 0;
	}

	public static int resetAllCooldowns(ServerPlayerEntity player) {
		return resetCooldown(player, MagicAbility.BELOW_FREEZING)
			+ resetCooldown(player, MagicAbility.FROST_ASCENT)
			+ resetCooldown(player, MagicAbility.MARTYRS_FLAME)
			+ resetCooldown(player, MagicAbility.APPRAISERS_MARK)
			+ resetCooldown(player, MagicAbility.TOLLKEEPERS_CLAIM)
			+ resetCooldown(player, MagicAbility.KINGS_DUES)
			+ resetCooldown(player, MagicAbility.BANKRUPTCY)
			+ resetCooldown(player, MagicAbility.SPOTLIGHT)
			+ resetCooldown(player, MagicAbility.WITTY_ONE_LINER)
			+ resetCooldown(player, MagicAbility.COMEDIC_REWRITE)
			+ resetCooldown(player, MagicAbility.COMEDIC_ASSISTANT)
			+ resetCooldown(player, MagicAbility.PLUS_ULTRA)
			+ resetCooldown(player, MagicAbility.HERCULES_BURDEN_OF_THE_SKY)
			+ resetCooldown(player, MagicAbility.SAGITTARIUS_ASTRAL_ARROW)
			+ resetCooldown(player, MagicAbility.ORIONS_GAMBIT)
			+ resetCooldown(player, MagicAbility.ABSOLUTE_ZERO)
			+ resetCooldown(player, MagicAbility.PLANCK_HEAT)
			+ resetCooldown(player, MagicAbility.TILL_DEATH_DO_US_PART)
			+ resetCooldown(player, MagicAbility.MANIPULATION)
			+ resetCooldown(player, MagicAbility.FROST_DOMAIN_EXPANSION)
			+ resetCooldown(player, MagicAbility.LOVE_DOMAIN_EXPANSION)
			+ resetCooldown(player, MagicAbility.ASTRAL_CATACLYSM);
	}

	public static void clearLockedAbilityState(ServerPlayerEntity player, MagicAbility ability) {
		if (player == null) {
			return;
		}

		if (ability == MagicAbility.BELOW_FREEZING || ability == MagicAbility.FROST_ASCENT || ability == MagicAbility.PLANCK_HEAT) {
			endFrostStagedMode(player, player.getEntityWorld().getServer().getTicks(), FrostStageEndReason.LOCKED, false, false);
			PLANCK_HEAT_COOLDOWN_END_TICK.remove(player.getUuid());
			FROST_ASCENT_COOLDOWN_END_TICK.remove(player.getUuid());
			BELOW_FREEZING_COOLDOWN_END_TICK.remove(player.getUuid());
			return;
		}

		if (ability == MagicAbility.ABSOLUTE_ZERO) {
			clearFrostMaximumState(player, false);
			ABSOLUTE_ZERO_COOLDOWN_END_TICK.remove(player.getUuid());
			FROST_MANA_REGEN_BLOCKED_END_TICK.remove(player.getUuid());
			return;
		}

		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			if (DOMAIN_EXPANSIONS.containsKey(player.getUuid()) && activeAbility(player) == MagicAbility.FROST_DOMAIN_EXPANSION) {
				deactivateDomainExpansion(player);
				setActiveAbility(player, MagicAbility.NONE);
			}
			return;
		}

		if (ability == MagicAbility.COMEDIC_ASSISTANT) {
			clearComedicAssistantState(player.getUuid());
			COMEDIC_ASSISTANT_PARROT_CARRY_STATES.remove(player.getUuid());
			clearComedicAssistantCaneImpactState(player.getUuid());
			return;
		}

		if (ability == MagicAbility.PLUS_ULTRA) {
			clearPlusUltraRuntimeState(player, true);
		}
	}

	public static void clearAllRuntimeState(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server == null) {
			return;
		}

		clearPlusUltraRuntimeState(player, true);
		clearFrostMaximumState(player, false);
		endFrostStagedMode(player, server.getTicks(), FrostStageEndReason.CLEAR_ALL, false, false);
		setActiveAbility(player, MagicAbility.NONE);
		player.setCameraEntity(player);
		MagicPlayerData.clearDomainClashUi(player);
		clearManipulationSuppressionTags(player);
		deactivateCassiopeia(player, true);
		deactivateSpotlight(player, true);
		deactivateHerculesBurden(player, false, false);
		clearSagittariusWindup(player);
		endOrionsGambit(player, OrionGambitEndReason.MANUAL_CANCEL, server.getTicks(), false);
		MARTYRS_FLAME_PASSIVE_ENABLED.remove(playerId);
		MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
		if (MARTYRS_FLAME_BURNING_TARGETS.remove(playerId) != null) {
			player.extinguish();
		}

		boolean domainStateChanged = false;

		DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.remove(playerId);
		if (ownedDomain != null) {
			cancelDomainClash(playerId, server);
			restoreDomainExpansion(server, ownedDomain);
			domainStateChanged = true;
		}

		MARTYRS_FLAME_COOLDOWN_END_TICK.remove(playerId);
		BELOW_FREEZING_COOLDOWN_END_TICK.remove(playerId);
		FROST_ASCENT_COOLDOWN_END_TICK.remove(playerId);
		WITTY_ONE_LINER_COOLDOWN_END_TICK.remove(playerId);
		COMEDIC_REWRITE_COOLDOWN_END_TICK.remove(playerId);
		COMEDIC_REWRITE_IMMUNITY_END_TICK.remove(playerId);
		COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.remove(playerId);
		COMEDIC_REWRITE_PENDING_STATES.remove(playerId);
		COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(playerId);
		COMEDIC_ASSISTANT_ARMED_STATES.remove(playerId);
		COMEDIC_ASSISTANT_PARROT_CARRY_STATES.remove(playerId);
		clearComedicAssistantCaneImpactState(playerId);
		PLUS_ULTRA_COOLDOWN_END_TICK.remove(playerId);
		PLUS_ULTRA_LAST_OUTLINED_PLAYERS.remove(playerId);
		PLUS_ULTRA_DAMAGE_SCALING_GUARD.remove(playerId);
		HERCULES_COOLDOWN_END_TICK.remove(playerId);
		SAGITTARIUS_COOLDOWN_END_TICK.remove(playerId);
		ORIONS_GAMBIT_COOLDOWN_END_TICK.remove(playerId);
		ABSOLUTE_ZERO_COOLDOWN_END_TICK.remove(playerId);
		FROST_MANA_REGEN_BLOCKED_END_TICK.remove(playerId);
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
		FROST_STAGE_STATES.remove(playerId);
		FROST_MAXIMUM_STATES.remove(playerId);
		FROST_SPIKE_WAVES.removeIf(state -> playerId.equals(state.casterId));
		clearFrostEffectsByCaster(playerId, true, server);
		clearFrostControlledTargetState(playerId, server);
		DOMAIN_CLASH_PENDING_DAMAGE.remove(playerId);
		MAGIC_DAMAGE_PENDING_ATTACKER.remove(playerId);
		MAGIC_DAMAGE_PENDING_ATTACKER.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
		ABSOLUTE_ZERO_NEXT_DAMAGE_TICK.remove(playerId);
		PLANCK_HEAT_STATES.remove(playerId);
		PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.remove(playerId);
		LOVE_POWER_ACTIVE_THIS_SECOND.remove(playerId);
		TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.remove(playerId);
		CASSIOPEIA_PASSIVE_ENABLED.remove(playerId);
		CASSIOPEIA_LAST_OUTLINED_PLAYERS.remove(playerId);
		HERCULES_STATES.remove(playerId);
		HERCULES_TARGETS.remove(playerId);
		SAGITTARIUS_STATES.remove(playerId);
		ORIONS_GAMBIT_STATES.remove(playerId);
		ORIONS_GAMBIT_PENALTIES.remove(playerId);
		ORIONS_GAMBIT_CASTER_BY_TARGET.remove(playerId);
		ORIONS_GAMBIT_DRAIN_BUFFER.remove(playerId);
		ORIONS_GAMBIT_DAMAGE_SCALING_GUARD.remove(playerId);
		ASTRAL_CATACLYSM_DOMAIN_STATES.remove(playerId);
		ASTRAL_EXECUTION_BEAMS.remove(playerId);
		SPOTLIGHT_STATES.remove(playerId);
		SPOTLIGHT_PASSIVE_ENABLED.remove(playerId);
		COMEDIC_REWRITE_PASSIVE_ENABLED.remove(playerId);
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
		if (ASTRAL_CATACLYSM_COOLDOWN_END_TICK.remove(playerId) != null) {
			domainStateChanged = true;
		}
		if (clearCapturedDomainState(playerId)) {
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
		HERCULES_TARGETS.entrySet().removeIf(entry -> entry.getValue().casterId.equals(playerId));
		ORIONS_GAMBIT_CASTER_BY_TARGET.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
		ASTRAL_EXECUTION_BEAMS.entrySet().removeIf(entry -> entry.getValue().casterId.equals(playerId));

		if (domainStateChanged) {
			persistDomainRuntimeState(server);
		}
	}

	private static void onPlayerDisconnect(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}

		MinecraftServer server = player.getEntityWorld().getServer();
		if (server == null) {
			return;
		}

		clearFrostControlledTargetState(player.getUuid(), server);

		if (PLUS_ULTRA_STATES.containsKey(player.getUuid()) || activeAbility(player) == MagicAbility.PLUS_ULTRA) {
			endPlusUltra(player, server.getTicks(), PlusUltraEndMode.FULL, false);
			return;
		}

		endFrostStagedMode(player, server.getTicks(), FrostStageEndReason.INVALID, false, false);
		clearFrostMaximumState(player, false);
		clearPlusUltraRuntimeState(player, false);
	}

	private static boolean clearCapturedDomainState(UUID playerId) {
		boolean changed = DOMAIN_PENDING_RETURNS.remove(playerId) != null;
		for (DomainExpansionState state : DOMAIN_EXPANSIONS.values()) {
			if (state.capturedEntities.remove(playerId) != null) {
				changed = true;
			}
		}
		return changed;
	}

	private static void deactivateMartyrsFlame(ServerPlayerEntity player, boolean startCooldown) {
		UUID playerId = player.getUuid();
		MARTYRS_FLAME_PASSIVE_ENABLED.remove(playerId);
		MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
		player.removeStatusEffect(StatusEffects.GLOWING);
		if (!startCooldown || MARTYRS_FLAME_COOLDOWN_TICKS <= 0) {
			MARTYRS_FLAME_COOLDOWN_END_TICK.remove(playerId);
			return;
		}

		startAbilityCooldownFromNow(playerId, MagicAbility.MARTYRS_FLAME, player.getEntityWorld().getServer().getTicks());
	}

	private static void deactivateAbsoluteZero(ServerPlayerEntity player) {
		clearFrostMaximumState(player, true);
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
		startAbilityCooldownFromNow(playerId, MagicAbility.TILL_DEATH_DO_US_PART, currentTick);
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
				startAbilityCooldownFromNow(casterId, MagicAbility.MANIPULATION, caster.getEntityWorld().getServer().getTicks());
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
			startAbilityCooldownFromNow(casterId, MagicAbility.MANIPULATION, caster.getEntityWorld().getServer().getTicks());
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

	private static final class BeaconCoreAnchorPersistentState extends PersistentState {
		private static final Codec<BeaconCoreAnchorPersistentState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("has_anchor", false).forGetter(state -> state.anchor != null),
			Codec.STRING.optionalFieldOf("dimension", "").forGetter(state ->
				state.anchor == null ? "" : state.anchor.dimension.getValue().toString()
			),
			Codec.INT.optionalFieldOf("x", 0).forGetter(state -> state.anchor == null ? 0 : state.anchor.pos.getX()),
			Codec.INT.optionalFieldOf("y", 0).forGetter(state -> state.anchor == null ? 0 : state.anchor.pos.getY()),
			Codec.INT.optionalFieldOf("z", 0).forGetter(state -> state.anchor == null ? 0 : state.anchor.pos.getZ())
		).apply(instance, BeaconCoreAnchorPersistentState::fromCodec));

		private BeaconCoreAnchorState anchor;

		private BeaconCoreAnchorPersistentState() {
		}

		private static BeaconCoreAnchorPersistentState fromCodec(boolean hasAnchor, String dimensionValue, int x, int y, int z) {
			BeaconCoreAnchorPersistentState state = new BeaconCoreAnchorPersistentState();
			if (!hasAnchor || dimensionValue.isBlank()) {
				return state;
			}

			Identifier dimensionId = Identifier.tryParse(dimensionValue);
			if (dimensionId == null) {
				return state;
			}

			state.anchor = new BeaconCoreAnchorState(
				RegistryKey.of(RegistryKeys.WORLD, dimensionId),
				new BlockPos(x, y, z)
			);
			return state;
		}

		private BeaconCoreAnchorState getAnchor() {
			return anchor;
		}
	}

	private static final class BeaconCoreAnchorState {
		private final RegistryKey<World> dimension;
		private final BlockPos pos;

		private BeaconCoreAnchorState(RegistryKey<World> dimension, BlockPos pos) {
			this.dimension = dimension;
			this.pos = pos;
		}
	}

	private static final class HerculesBurdenState {
		private final int endTick;
		private final Set<UUID> targetIds;

		private HerculesBurdenState(int endTick, Set<UUID> targetIds) {
			this.endTick = endTick;
			this.targetIds = targetIds;
		}
	}

	private static final class AstralBurdenTargetState {
		private final RegistryKey<World> dimension;
		private final UUID casterId;
		private final double lockedX;
		private final double lockedY;
		private final double lockedZ;
		private final int endTick;
		private int lastParticleTick;

		private AstralBurdenTargetState(
			RegistryKey<World> dimension,
			UUID casterId,
			double lockedX,
			double lockedY,
			double lockedZ,
			int endTick,
			int lastParticleTick
		) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.lockedX = lockedX;
			this.lockedY = lockedY;
			this.lockedZ = lockedZ;
			this.endTick = endTick;
			this.lastParticleTick = lastParticleTick;
		}
	}

	private static final class SagittariusWindupState {
		private final int fireTick;

		private SagittariusWindupState(int fireTick) {
			this.fireTick = fireTick;
		}
	}

	private enum OrionGambitStage {
		WAITING,
		LINKED
	}

	private enum OrionGambitEndReason {
		WAITING_CANCEL,
		MANUAL_CANCEL,
		EXPIRED,
		TARGET_INVALID,
		MANA_DEPLETED,
		SCHOOL_CHANGED,
		CASTER_DIED
	}

	private static final class OrionGambitState {
		private OrionGambitStage stage;
		private RegistryKey<World> targetDimension;
		private UUID targetId;
		private int benefitEndTick;
		private final Set<MagicAbility> usedTargetAbilities = new HashSet<>();
		private final Map<MagicAbility, Integer> usedTargetCooldownOverrides = new HashMap<>();

		private OrionGambitState(OrionGambitStage stage) {
			this.stage = stage;
		}

		private static OrionGambitState waiting() {
			return new OrionGambitState(OrionGambitStage.WAITING);
		}
	}

	private static final class OrionPenaltyState {
		private final int endTick;

		private OrionPenaltyState(int endTick) {
			this.endTick = endTick;
		}
	}

	private static final class OrionPendingEndState {
		private final ServerPlayerEntity caster;
		private final OrionGambitEndReason reason;
		private final boolean sendFeedback;

		private OrionPendingEndState(ServerPlayerEntity caster, OrionGambitEndReason reason, boolean sendFeedback) {
			this.caster = caster;
			this.reason = reason;
			this.sendFeedback = sendFeedback;
		}
	}

	private enum PlusUltraEndMode {
		FULL,
		MANUAL_EARLY,
		ADMIN_CLEAR
	}

	private static final class PlusUltraState {
		private final RegistryKey<World> dimension;
		private final int startTick;
		private final int halfwayTick;
		private final int endTick;
		private int nextOutlineTick;
		private int nextTextTick;
		private final boolean hadAllowFlying;
		private final boolean hadFlying;
		private final float hadFlySpeed;
		private final int overheadTextEndTick;
		private UUID overheadTextEntityId;

		private PlusUltraState(
			RegistryKey<World> dimension,
			int startTick,
			int halfwayTick,
			int endTick,
			int nextOutlineTick,
			int nextTextTick,
			boolean hadAllowFlying,
			boolean hadFlying,
			float hadFlySpeed,
			int overheadTextEndTick,
			UUID overheadTextEntityId
		) {
			this.dimension = dimension;
			this.startTick = startTick;
			this.halfwayTick = halfwayTick;
			this.endTick = endTick;
			this.nextOutlineTick = nextOutlineTick;
			this.nextTextTick = nextTextTick;
			this.hadAllowFlying = hadAllowFlying;
			this.hadFlying = hadFlying;
			this.hadFlySpeed = hadFlySpeed;
			this.overheadTextEndTick = overheadTextEndTick;
			this.overheadTextEntityId = overheadTextEntityId;
		}

		private RegistryKey<World> dimension() {
			return dimension;
		}

		private int startTick() {
			return startTick;
		}

		private int halfwayTick() {
			return halfwayTick;
		}

		private int endTick() {
			return endTick;
		}

		private int nextOutlineTick() {
			return nextOutlineTick;
		}

		private void nextOutlineTick(int nextOutlineTick) {
			this.nextOutlineTick = nextOutlineTick;
		}

		private int nextTextTick() {
			return nextTextTick;
		}

		private void nextTextTick(int nextTextTick) {
			this.nextTextTick = nextTextTick;
		}

		private boolean hadAllowFlying() {
			return hadAllowFlying;
		}

		private boolean hadFlying() {
			return hadFlying;
		}

		private float hadFlySpeed() {
			return hadFlySpeed;
		}

		private int overheadTextEndTick() {
			return overheadTextEndTick;
		}

		private UUID overheadTextEntityId() {
			return overheadTextEntityId;
		}

		private void overheadTextEntityId(UUID overheadTextEntityId) {
			this.overheadTextEntityId = overheadTextEntityId;
		}
	}

	private static final class PlusUltraImpactState {
		private final RegistryKey<World> dimension;
		private final UUID casterId;
		private final int startTick;
		private final int endTick;
		private Vec3d lastPosition;
		private Vec3d lastVelocity;
		private double lastSpeed;
		private boolean impacted;

		private PlusUltraImpactState(
			RegistryKey<World> dimension,
			UUID casterId,
			int startTick,
			int endTick,
			Vec3d lastPosition,
			Vec3d lastVelocity,
			double lastSpeed,
			boolean impacted
		) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.startTick = startTick;
			this.endTick = endTick;
			this.lastPosition = lastPosition;
			this.lastVelocity = lastVelocity;
			this.lastSpeed = Math.max(0.0, lastSpeed);
			this.impacted = impacted;
		}

		private RegistryKey<World> dimension() {
			return dimension;
		}

		private UUID casterId() {
			return casterId;
		}

		private int startTick() {
			return startTick;
		}

		private int endTick() {
			return endTick;
		}

		private Vec3d lastPosition() {
			return lastPosition;
		}

		private void lastPosition(Vec3d lastPosition) {
			this.lastPosition = lastPosition;
		}

		private Vec3d lastVelocity() {
			return lastVelocity;
		}

		private void lastVelocity(Vec3d lastVelocity) {
			this.lastVelocity = lastVelocity;
		}

		private double lastSpeed() {
			return lastSpeed;
		}

		private void lastSpeed(double lastSpeed) {
			this.lastSpeed = Math.max(0.0, lastSpeed);
		}

		private boolean impacted() {
			return impacted;
		}

		private void impacted(boolean impacted) {
			this.impacted = impacted;
		}
	}

	private record PlusUltraImpactHit(Vec3d position, BlockState blockState, Direction side) {
	}

	private enum ConstellationDomainPhase {
		CHARGING,
		READY,
		ACQUIRING
	}

	private static final class ConstellationDomainState {
		private ConstellationDomainPhase phase;
		private int chargeCompleteTick;
		private int acquireEndTick;
		private final Set<Integer> announcedExpiryWarnings = new HashSet<>();

		private ConstellationDomainState(ConstellationDomainPhase phase, int chargeCompleteTick) {
			this.phase = phase;
			this.chargeCompleteTick = chargeCompleteTick;
		}
	}

	private static final class AstralExecutionBeamState {
		private final RegistryKey<World> dimension;
		private final UUID casterId;
		private final double lockedX;
		private final double baseY;
		private final double lockedZ;
		private final double restoreX;
		private final double restoreY;
		private final double restoreZ;
		private final float restoreYaw;
		private final float restorePitch;
		private double currentY;
		private int nextDamageTick;
		private int nextSoundTick;

		private AstralExecutionBeamState(
			RegistryKey<World> dimension,
			UUID casterId,
			double lockedX,
			double baseY,
			double lockedZ,
			double restoreX,
			double restoreY,
			double restoreZ,
			float restoreYaw,
			float restorePitch,
			int nextDamageTick,
			int nextSoundTick
		) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.lockedX = lockedX;
			this.baseY = baseY;
			this.lockedZ = lockedZ;
			this.restoreX = restoreX;
			this.restoreY = restoreY;
			this.restoreZ = restoreZ;
			this.restoreYaw = restoreYaw;
			this.restorePitch = restorePitch;
			this.currentY = baseY;
			this.nextDamageTick = nextDamageTick;
			this.nextSoundTick = nextSoundTick;
		}
	}

	private static final class MartyrsFlameBurnState {
		private final RegistryKey<World> dimension;
		private int expiresTick;

		private MartyrsFlameBurnState(RegistryKey<World> dimension, int expiresTick) {
			this.dimension = dimension;
			this.expiresTick = expiresTick;
		}
	}

	private enum FrostMaximumPhase {
		WINDUP,
		ENCASE
	}

	private enum FrostStageEndReason {
		MANUAL,
		FORCED_THRESHOLD,
		OVERCAST,
		MAXIMUM,
		INVALID,
		LOCKED,
		CLEAR_ALL,
		CASTER_DIED
	}

	private enum FrostKillProgressType {
		RANGED,
		SLAM
	}

	private static final class FrostStageState {
		private RegistryKey<World> dimension;
		private int currentStage;
		private int highestUnlockedStage;
		private int progressTicks;

		private FrostStageState(RegistryKey<World> dimension, int currentStage, int highestUnlockedStage, int progressTicks) {
			this.dimension = dimension;
			this.currentStage = currentStage;
			this.highestUnlockedStage = highestUnlockedStage;
			this.progressTicks = progressTicks;
		}
	}

	private static final class FrostMaximumState {
		private final RegistryKey<World> dimension;
		private final double baseX;
		private final double baseY;
		private final double baseZ;
		private FrostMaximumPhase phase;
		private final int windupEndTick;
		private int encaseEndTick;
		private final Map<UUID, FrostLockedTargetSeed> pendingHelplessTargets;

		private FrostMaximumState(RegistryKey<World> dimension, double baseX, double baseY, double baseZ, int windupEndTick) {
			this.dimension = dimension;
			this.baseX = baseX;
			this.baseY = baseY;
			this.baseZ = baseZ;
			this.phase = FrostMaximumPhase.WINDUP;
			this.windupEndTick = windupEndTick;
			this.encaseEndTick = windupEndTick;
			this.pendingHelplessTargets = new HashMap<>();
		}
	}

	private static final class FrostSpikeWaveState {
		private final UUID casterId;
		private final RegistryKey<World> dimension;
		private final Vec3d origin;
		private final Vec3d direction;
		private final double range;
		private final double speedPerTick;
		private final double width;
		private final float damage;
		private final int stage;
		private final boolean overcast;
		private double travelled;
		private double nextEruptionDistance;
		private double terrainSearchY;
		private int consecutiveGroundMisses;
		private final Set<UUID> hitTargets;

		private FrostSpikeWaveState(
			UUID casterId,
			RegistryKey<World> dimension,
			Vec3d origin,
			Vec3d direction,
			double range,
			double speedPerTick,
			double width,
			float damage,
			int stage,
			boolean overcast
		) {
			this.casterId = casterId;
			this.dimension = dimension;
			this.origin = origin;
			this.direction = direction.normalize();
			this.range = range;
			this.speedPerTick = speedPerTick;
			this.width = width;
			this.damage = damage;
			this.stage = stage;
			this.overcast = overcast;
			this.travelled = 0.0;
			this.nextEruptionDistance = 0.0;
			this.terrainSearchY = origin.y + 0.8;
			this.consecutiveGroundMisses = 0;
			this.hitTargets = new HashSet<>();
		}
	}

	private static final class FrostFreezeState {
		private final RegistryKey<World> dimension;
		private final UUID casterId;
		private final double lockedX;
		private final double lockedY;
		private final double lockedZ;
		private final int expiresTick;

		private FrostFreezeState(RegistryKey<World> dimension, UUID casterId, double lockedX, double lockedY, double lockedZ, int expiresTick) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.lockedX = lockedX;
			this.lockedY = lockedY;
			this.lockedZ = lockedZ;
			this.expiresTick = expiresTick;
		}
	}

	private static final class FrostFearState {
		private final RegistryKey<World> dimension;
		private final UUID casterId;
		private final double lockedX;
		private final double lockedY;
		private final double lockedZ;
		private final int expiresTick;

		private FrostFearState(RegistryKey<World> dimension, UUID casterId, double lockedX, double lockedY, double lockedZ, int expiresTick) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.lockedX = lockedX;
			this.lockedY = lockedY;
			this.lockedZ = lockedZ;
			this.expiresTick = expiresTick;
		}
	}

	private static final class FrostHelplessState {
		private final RegistryKey<World> dimension;
		private final UUID casterId;
		private final double lockedX;
		private final double lockedY;
		private final double lockedZ;
		private final int expiresTick;

		private FrostHelplessState(RegistryKey<World> dimension, UUID casterId, double lockedX, double lockedY, double lockedZ, int expiresTick) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.lockedX = lockedX;
			this.lockedY = lockedY;
			this.lockedZ = lockedZ;
			this.expiresTick = expiresTick;
		}
	}

	private static final class FrostPackedIceState {
		private final RegistryKey<World> dimension;
		private final UUID casterId;
		private final double lockedX;
		private final double lockedY;
		private final double lockedZ;
		private final int expiresTick;
		private int nextDamageTick;
		private final double shellRadius;
		private final double shellHeight;
		private final List<UUID> encasementDisplayIds;

		private FrostPackedIceState(
			RegistryKey<World> dimension,
			UUID casterId,
			double lockedX,
			double lockedY,
			double lockedZ,
			int expiresTick,
			int nextDamageTick,
			double shellRadius,
			double shellHeight
		) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.lockedX = lockedX;
			this.lockedY = lockedY;
			this.lockedZ = lockedZ;
			this.expiresTick = expiresTick;
			this.nextDamageTick = nextDamageTick;
			this.shellRadius = shellRadius;
			this.shellHeight = shellHeight;
			this.encasementDisplayIds = new ArrayList<>();
		}
	}

	private static final class FrostLockedTargetSeed {
		private final RegistryKey<World> dimension;
		private final double x;
		private final double y;
		private final double z;

		private FrostLockedTargetSeed(RegistryKey<World> dimension, double x, double y, double z) {
			this.dimension = dimension;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private static final class FrostbiteState {
		private final RegistryKey<World> dimension;
		private UUID casterId;
		private int expiresTick;
		private int nextDamageTick;
		private float damagePerTick;

		private FrostbiteState(RegistryKey<World> dimension, UUID casterId, int expiresTick, int nextDamageTick, float damagePerTick) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.expiresTick = expiresTick;
			this.nextDamageTick = nextDamageTick;
			this.damagePerTick = damagePerTick;
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
		private UUID casterId;
		private int expiresTick;
		private int nextDamageTick;

		private EnhancedFireState(RegistryKey<World> dimension, UUID casterId, int expiresTick, int nextDamageTick) {
			this.dimension = dimension;
			this.casterId = casterId;
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
		private int effectEndTick;
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
		private int frostPulseCount;
		private int nextFrostPulseTick;
		private final Map<BlockPos, DomainSavedBlockState> savedBlocks;
		private final Map<BlockPos, BlockState> protectedShellStates;
		private final Map<UUID, DomainCapturedEntityState> capturedEntities;

		private DomainExpansionState(
			RegistryKey<World> dimension,
			MagicAbility ability,
			int expiresTick,
			int effectEndTick,
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
			int frostPulseCount,
			int nextFrostPulseTick,
			Map<BlockPos, DomainSavedBlockState> savedBlocks,
			Map<BlockPos, BlockState> protectedShellStates,
			Map<UUID, DomainCapturedEntityState> capturedEntities
		) {
			this.dimension = dimension;
			this.ability = ability;
			this.expiresTick = expiresTick;
			this.effectEndTick = effectEndTick;
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
			this.frostPulseCount = frostPulseCount;
			this.nextFrostPulseTick = nextFrostPulseTick;
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

	private static final class DomainClashPendingDamageState {
		private final UUID attackerId;
		private final float healthBefore;
		private final float incomingAmount;

		private DomainClashPendingDamageState(UUID attackerId, float healthBefore, float incomingAmount) {
			this.attackerId = attackerId;
			this.healthBefore = healthBefore;
			this.incomingAmount = incomingAmount;
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

	private static final class SpotlightState {
		private final Map<UUID, Integer> viewerLastLookTicks = new HashMap<>();
		private int currentStage;
		private int downgradeDeadlineTick = -1;
		private double appliedMaxHealthBonusPoints;

		private Map<UUID, Integer> viewerLastLookTicks() {
			return viewerLastLookTicks;
		}

		private int currentStage() {
			return currentStage;
		}

		private void currentStage(int currentStage) {
			this.currentStage = Math.max(0, currentStage);
		}

		private int downgradeDeadlineTick() {
			return downgradeDeadlineTick;
		}

		private void downgradeDeadlineTick(int downgradeDeadlineTick) {
			this.downgradeDeadlineTick = downgradeDeadlineTick;
		}

		private double appliedMaxHealthBonusPoints() {
			return appliedMaxHealthBonusPoints;
		}

		private void appliedMaxHealthBonusPoints(double appliedMaxHealthBonusPoints) {
			this.appliedMaxHealthBonusPoints = Math.max(0.0, appliedMaxHealthBonusPoints);
		}
	}

	private record SpotlightStageSettings(
		int viewersRequired,
		int activationWindowTicks,
		int downgradeGraceTicks,
		int fallbackWindowTicks,
		double attackDamageBonus,
		double movementSpeedMultiplier,
		double scale,
		int jumpBoostAmplifier,
		int resistanceAmplifier,
		double maxHealthBonusHearts
	) {
		private static SpotlightStageSettings defaults(
			int viewersRequired,
			int activationWindowTicks,
			int downgradeGraceTicks,
			int fallbackWindowTicks,
			double attackDamageBonus,
			double movementSpeedMultiplier,
			double scale,
			int jumpBoostAmplifier,
			int resistanceAmplifier,
			double maxHealthBonusHearts
		) {
			return new SpotlightStageSettings(
				viewersRequired,
				activationWindowTicks,
				downgradeGraceTicks,
				fallbackWindowTicks,
				attackDamageBonus,
				movementSpeedMultiplier,
				scale,
				jumpBoostAmplifier,
				resistanceAmplifier,
				maxHealthBonusHearts
			);
		}

		private int maxRelevantWindowTicks() {
			return Math.max(activationWindowTicks, fallbackWindowTicks);
		}

		private double maxHealthBonusPoints() {
			return maxHealthBonusHearts * 2.0;
		}
	}

	private record WittyOneLinerTierSettings(
		int selectionWeight,
		int cooldownTicks,
		int effectDurationTicks,
		int colorRgb,
		int slownessAmplifier,
		int weaknessAmplifier,
		int blindnessAmplifier,
		int nauseaAmplifier,
		int darknessAmplifier,
		int weavingAmplifier,
		boolean applyGlowing,
		List<String> jokes
	) {
		private static WittyOneLinerTierSettings defaults(
			int selectionWeight,
			int cooldownTicks,
			int effectDurationTicks,
			int colorRgb,
			int slownessAmplifier,
			int weaknessAmplifier,
			int blindnessAmplifier,
			int nauseaAmplifier,
			int darknessAmplifier,
			int weavingAmplifier,
			boolean applyGlowing,
			List<String> jokes
		) {
			return new WittyOneLinerTierSettings(
				selectionWeight,
				cooldownTicks,
				effectDurationTicks,
				colorRgb,
				slownessAmplifier,
				weaknessAmplifier,
				blindnessAmplifier,
				nauseaAmplifier,
				darknessAmplifier,
				weavingAmplifier,
				applyGlowing,
				jokes
			);
		}
	}

	private enum ComedicAssistantOutcome {
		GIANT_SLIME_SLAM("giant_slime_slam"),
		PANDA_BOWLING_BALL("panda_bowling_ball"),
		PARROT_KIDNAPPING("parrot_kidnapping"),
		DIVINE_OVERREACTION("divine_overreaction"),
		ACME_DROP("acme_drop"),
		PIE_TO_THE_FACE("pie_to_the_face"),
		GIANT_CANE_YANK("giant_cane_yank");

		private final String id;

		ComedicAssistantOutcome(String id) {
			this.id = id;
		}

		private Text displayName() {
			return Text.translatable("magic.jester_gag." + id);
		}
	}

	private record ComedicAssistantArmedState(
		RegistryKey<World> dimension,
		int expiresTick,
		int nextIndicatorTick
	) {
	}

	private static final class ComedicAssistantParrotCarryState {
		private final RegistryKey<World> dimension;
		private final double targetY;
		private final int endTick;
		private int nextParticleTick;
		private int nextSoundTick;
		private final ComedicAssistantParrotSettings settings;

		private ComedicAssistantParrotCarryState(
			RegistryKey<World> dimension,
			double targetY,
			int endTick,
			int nextParticleTick,
			int nextSoundTick,
			ComedicAssistantParrotSettings settings
		) {
			this.dimension = dimension;
			this.targetY = targetY;
			this.endTick = endTick;
			this.nextParticleTick = nextParticleTick;
			this.nextSoundTick = nextSoundTick;
			this.settings = settings;
		}

		private RegistryKey<World> dimension() {
			return dimension;
		}

		private double targetY() {
			return targetY;
		}

		private int endTick() {
			return endTick;
		}

		private int nextParticleTick() {
			return nextParticleTick;
		}

		private void nextParticleTick(int nextParticleTick) {
			this.nextParticleTick = nextParticleTick;
		}

		private int nextSoundTick() {
			return nextSoundTick;
		}

		private void nextSoundTick(int nextSoundTick) {
			this.nextSoundTick = nextSoundTick;
		}

		private ComedicAssistantParrotSettings settings() {
			return settings;
		}
	}

	private record ComedicAssistantAcmeVisualState(
		RegistryKey<World> dimension,
		UUID visualEntityId,
		UUID targetEntityId,
		int endTick,
		double dropSpeed
	) {
	}

	private static final class ComedicAssistantCaneImpactState {
		private final RegistryKey<World> dimension;
		private final UUID casterId;
		private final int startTick;
		private final int endTick;
		private final int forceEndTick;
		private final Vec3d forcedVelocity;
		private Vec3d lastPosition;
		private Vec3d lastVelocity;
		private double lastSpeed;

		private ComedicAssistantCaneImpactState(
			RegistryKey<World> dimension,
			UUID casterId,
			int startTick,
			int endTick,
			int forceEndTick,
			Vec3d forcedVelocity,
			Vec3d lastPosition,
			Vec3d lastVelocity,
			double lastSpeed
		) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.startTick = startTick;
			this.endTick = endTick;
			this.forceEndTick = forceEndTick;
			this.forcedVelocity = forcedVelocity;
			this.lastPosition = lastPosition;
			this.lastVelocity = lastVelocity;
			this.lastSpeed = Math.max(0.0, lastSpeed);
		}

		private RegistryKey<World> dimension() {
			return dimension;
		}

		private UUID casterId() {
			return casterId;
		}

		private int startTick() {
			return startTick;
		}

		private int endTick() {
			return endTick;
		}

		private int forceEndTick() {
			return forceEndTick;
		}

		private Vec3d forcedVelocity() {
			return forcedVelocity;
		}

		private Vec3d lastPosition() {
			return lastPosition;
		}

		private void lastPosition(Vec3d lastPosition) {
			this.lastPosition = lastPosition;
		}

		private Vec3d lastVelocity() {
			return lastVelocity;
		}

		private void lastVelocity(Vec3d lastVelocity) {
			this.lastVelocity = lastVelocity;
		}

		private double lastSpeed() {
			return lastSpeed;
		}

		private void lastSpeed(double lastSpeed) {
			this.lastSpeed = Math.max(0.0, lastSpeed);
		}
	}

	private record ComedicAssistantSlimeSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		int slownessDurationTicks,
		int slownessAmplifier,
		int oozingDurationTicks,
		int oozingAmplifier,
		int visualDurationTicks,
		double visualSpawnHeight,
		double visualFallSpeed,
		int visualSize,
		int particleCount,
		float spawnSoundVolume,
		float spawnSoundPitch,
		float impactSoundVolume,
		float impactSoundPitch
	) {
		private static ComedicAssistantSlimeSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			int slownessDurationTicks,
			int slownessAmplifier,
			int oozingDurationTicks,
			int oozingAmplifier,
			int visualDurationTicks,
			double visualSpawnHeight,
			double visualFallSpeed,
			int visualSize,
			int particleCount,
			float spawnSoundVolume,
			float spawnSoundPitch,
			float impactSoundVolume,
			float impactSoundPitch
		) {
			return new ComedicAssistantSlimeSettings(
				enabled,
				weight,
				bonusDamage,
				slownessDurationTicks,
				slownessAmplifier,
				oozingDurationTicks,
				oozingAmplifier,
				visualDurationTicks,
				visualSpawnHeight,
				visualFallSpeed,
				visualSize,
				particleCount,
				spawnSoundVolume,
				spawnSoundPitch,
				impactSoundVolume,
				impactSoundPitch
			);
		}
	}

	private record ComedicAssistantPandaSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		double horizontalLaunch,
		double verticalLaunch,
		int slownessDurationTicks,
		int slownessAmplifier,
		int visualDurationTicks,
		double visualSpawnDistance,
		double visualChargeVelocity,
		int particleCount,
		float rollSoundVolume,
		float rollSoundPitch,
		float impactSoundVolume,
		float impactSoundPitch
	) {
		private static ComedicAssistantPandaSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			double horizontalLaunch,
			double verticalLaunch,
			int slownessDurationTicks,
			int slownessAmplifier,
			int visualDurationTicks,
			double visualSpawnDistance,
			double visualChargeVelocity,
			int particleCount,
			float rollSoundVolume,
			float rollSoundPitch,
			float impactSoundVolume,
			float impactSoundPitch
		) {
			return new ComedicAssistantPandaSettings(
				enabled,
				weight,
				bonusDamage,
				horizontalLaunch,
				verticalLaunch,
				slownessDurationTicks,
				slownessAmplifier,
				visualDurationTicks,
				visualSpawnDistance,
				visualChargeVelocity,
				particleCount,
				rollSoundVolume,
				rollSoundPitch,
				impactSoundVolume,
				impactSoundPitch
			);
		}
	}

	private record ComedicAssistantParrotSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		double liftHeight,
		double upwardVelocity,
		int maxCarryTicks,
		double releaseDownwardVelocity,
		boolean applyGlowing,
		int glowingDurationTicks,
		int visualDurationTicks,
		int visualCount,
		double visualRadius,
		double visualVerticalOffset,
		int particleCount,
		int flapSoundIntervalTicks,
		float soundVolume,
		float soundPitch
	) {
		private static ComedicAssistantParrotSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			double liftHeight,
			double upwardVelocity,
			int maxCarryTicks,
			double releaseDownwardVelocity,
			boolean applyGlowing,
			int glowingDurationTicks,
			int visualDurationTicks,
			int visualCount,
			double visualRadius,
			double visualVerticalOffset,
			int particleCount,
			int flapSoundIntervalTicks,
			float soundVolume,
			float soundPitch
		) {
			return new ComedicAssistantParrotSettings(
				enabled,
				weight,
				bonusDamage,
				liftHeight,
				upwardVelocity,
				maxCarryTicks,
				releaseDownwardVelocity,
				applyGlowing,
				glowingDurationTicks,
				visualDurationTicks,
				visualCount,
				visualRadius,
				visualVerticalOffset,
				particleCount,
				flapSoundIntervalTicks,
				soundVolume,
				soundPitch
			);
		}
	}

	private record ComedicAssistantDivineSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		int strikeCount,
		double strikeRadius,
		int glowingDurationTicks,
		int blindnessDurationTicks,
		int blindnessAmplifier,
		int nauseaDurationTicks,
		int nauseaAmplifier,
		int particleCount,
		float soundVolume,
		float soundPitch
	) {
		private static ComedicAssistantDivineSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			int strikeCount,
			double strikeRadius,
			int glowingDurationTicks,
			int blindnessDurationTicks,
			int blindnessAmplifier,
			int nauseaDurationTicks,
			int nauseaAmplifier,
			int particleCount,
			float soundVolume,
			float soundPitch
		) {
			return new ComedicAssistantDivineSettings(
				enabled,
				weight,
				bonusDamage,
				strikeCount,
				strikeRadius,
				glowingDurationTicks,
				blindnessDurationTicks,
				blindnessAmplifier,
				nauseaDurationTicks,
				nauseaAmplifier,
				particleCount,
				soundVolume,
				soundPitch
			);
		}
	}

	private record ComedicAssistantAcmeSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		int slownessDurationTicks,
		int slownessAmplifier,
		int weaknessDurationTicks,
		int weaknessAmplifier,
		int visualDurationTicks,
		double visualDropHeight,
		int particleCount,
		float soundVolume,
		float soundPitch
	) {
		private static ComedicAssistantAcmeSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			int slownessDurationTicks,
			int slownessAmplifier,
			int weaknessDurationTicks,
			int weaknessAmplifier,
			int visualDurationTicks,
			double visualDropHeight,
			int particleCount,
			float soundVolume,
			float soundPitch
		) {
			return new ComedicAssistantAcmeSettings(
				enabled,
				weight,
				bonusDamage,
				slownessDurationTicks,
				slownessAmplifier,
				weaknessDurationTicks,
				weaknessAmplifier,
				visualDurationTicks,
				visualDropHeight,
				particleCount,
				soundVolume,
				soundPitch
			);
		}
	}

	private record ComedicAssistantPieSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		int blindnessDurationTicks,
		int blindnessAmplifier,
		int nauseaDurationTicks,
		int nauseaAmplifier,
		int particleCount,
		float soundVolume,
		float soundPitch
	) {
		private static ComedicAssistantPieSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			int blindnessDurationTicks,
			int blindnessAmplifier,
			int nauseaDurationTicks,
			int nauseaAmplifier,
			int particleCount,
			float soundVolume,
			float soundPitch
		) {
			return new ComedicAssistantPieSettings(
				enabled,
				weight,
				bonusDamage,
				blindnessDurationTicks,
				blindnessAmplifier,
				nauseaDurationTicks,
				nauseaAmplifier,
				particleCount,
				soundVolume,
				soundPitch
			);
		}
	}

	private record ComedicAssistantCaneSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		double horizontalLaunch,
		double verticalLaunch,
		int launchControlTicks,
		int velocityDamageTrackingTicks,
		double velocityDamageThreshold,
		double velocityDamageMultiplier,
		double velocityDamageMax,
		int slownessDurationTicks,
		int slownessAmplifier,
		int visualDurationTicks,
		double visualSpawnDistance,
		double visualChargeVelocity,
		int particleCount,
		float soundVolume,
		float soundPitch
	) {
		private static ComedicAssistantCaneSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			double horizontalLaunch,
			double verticalLaunch,
			int launchControlTicks,
			int velocityDamageTrackingTicks,
			double velocityDamageThreshold,
			double velocityDamageMultiplier,
			double velocityDamageMax,
			int slownessDurationTicks,
			int slownessAmplifier,
			int visualDurationTicks,
			double visualSpawnDistance,
			double visualChargeVelocity,
			int particleCount,
			float soundVolume,
			float soundPitch
		) {
			return new ComedicAssistantCaneSettings(
				enabled,
				weight,
				bonusDamage,
				horizontalLaunch,
				verticalLaunch,
				launchControlTicks,
				velocityDamageTrackingTicks,
				velocityDamageThreshold,
				velocityDamageMultiplier,
				velocityDamageMax,
				slownessDurationTicks,
				slownessAmplifier,
				visualDurationTicks,
				visualSpawnDistance,
				visualChargeVelocity,
				particleCount,
				soundVolume,
				soundPitch
			);
		}
	}

	private enum ComedicRewriteOutcome {
		LAUNCHED_THROUGH_THE_SCENE,
		RAVAGER_BIT,
		PARROT_RESCUE
	}

	private record PendingComedicRewriteState(
		ComedicRewriteOutcome outcome,
		double severity,
		Vec3d launchDirection,
		Vec3d safePos
	) {
		private PendingComedicRewriteState withOutcome(ComedicRewriteOutcome replacementOutcome) {
			return new PendingComedicRewriteState(replacementOutcome, severity, launchDirection, safePos);
		}
	}

	private static final class ComedicRewriteVisualCameo {
		private final int endTick;
		private final int chargeEndTick;
		private final int[] entityIds;
		private final List<UUID> viewerIds;
		private final ComedicRewriteVisualFollowState followState;
		private final ComedicRewriteVisualRotationState rotationState;
		private final Entity[] entities;
		private boolean chargeStopped;

		private ComedicRewriteVisualCameo(
			int endTick,
			int chargeEndTick,
			int[] entityIds,
			List<UUID> viewerIds,
			ComedicRewriteVisualFollowState followState,
			ComedicRewriteVisualRotationState rotationState,
			Entity[] entities
		) {
			this.endTick = endTick;
			this.chargeEndTick = chargeEndTick;
			this.entityIds = entityIds;
			this.viewerIds = viewerIds;
			this.followState = followState;
			this.rotationState = rotationState;
			this.entities = entities;
		}

		private int endTick() {
			return endTick;
		}

		private int chargeEndTick() {
			return chargeEndTick;
		}

		private int[] entityIds() {
			return entityIds;
		}

		private List<UUID> viewerIds() {
			return viewerIds;
		}

		private ComedicRewriteVisualFollowState followState() {
			return followState;
		}

		private ComedicRewriteVisualRotationState rotationState() {
			return rotationState;
		}

		private Entity[] entities() {
			return entities;
		}

		private boolean chargeStopped() {
			return chargeStopped;
		}

		private void markChargeStopped() {
			chargeStopped = true;
		}
	}

	private record ComedicRewriteVisualFollowState(
		RegistryKey<World> dimension,
		UUID anchorEntityId,
		Vec3d[] offsets,
		ComedicRewriteVisualFollowMode mode,
		double verticalSpeed
	) {
	}

	private enum ComedicRewriteVisualFollowMode {
		ENTITY_TOP,
		DROP_TO_ENTITY_TOP
	}

	private record ComedicRewriteVisualRotationState(
		boolean alignYawToVelocity,
		float yawOffsetDegrees,
		float pitchPerTick
	) {
	}

	private record ComedicRewriteLaunchSettings(
		int weight,
		double baseHorizontalVelocity,
		double horizontalVelocityPerSeverity,
		double baseVerticalVelocity,
		double verticalVelocityPerSeverity,
		int slownessDurationTicks,
		int slownessAmplifier,
		int smokeParticleCount,
		int poofParticleCount
	) {
		private static ComedicRewriteLaunchSettings defaults(
			int weight,
			double baseHorizontalVelocity,
			double horizontalVelocityPerSeverity,
			double baseVerticalVelocity,
			double verticalVelocityPerSeverity,
			int slownessDurationTicks,
			int slownessAmplifier,
			int smokeParticleCount,
			int poofParticleCount
		) {
			return new ComedicRewriteLaunchSettings(
				weight,
				baseHorizontalVelocity,
				horizontalVelocityPerSeverity,
				baseVerticalVelocity,
				verticalVelocityPerSeverity,
				slownessDurationTicks,
				slownessAmplifier,
				smokeParticleCount,
				poofParticleCount
			);
		}
	}

	private record ComedicRewriteRavagerSettings(
		int weight,
		double baseHorizontalVelocity,
		double horizontalVelocityPerSeverity,
		double baseVerticalVelocity,
		double verticalVelocityPerSeverity,
		int slownessDurationTicks,
		int slownessAmplifier,
		int smokeParticleCount,
		int dustParticleCount,
		boolean showVisualCameo,
		int visualDurationTicks,
		double visualSpawnDistance,
		double visualVerticalOffset,
		double visualChargeVelocity,
		double visualChargeVelocityBuffer,
		int visualChargeDurationTicks
	) {
		private static ComedicRewriteRavagerSettings defaults(
			int weight,
			double baseHorizontalVelocity,
			double horizontalVelocityPerSeverity,
			double baseVerticalVelocity,
			double verticalVelocityPerSeverity,
			int slownessDurationTicks,
			int slownessAmplifier,
			int smokeParticleCount,
			int dustParticleCount,
			boolean showVisualCameo,
			int visualDurationTicks,
			double visualSpawnDistance,
			double visualVerticalOffset,
			double visualChargeVelocity,
			double visualChargeVelocityBuffer,
			int visualChargeDurationTicks
		) {
			return new ComedicRewriteRavagerSettings(
				weight,
				baseHorizontalVelocity,
				horizontalVelocityPerSeverity,
				baseVerticalVelocity,
				verticalVelocityPerSeverity,
				slownessDurationTicks,
				slownessAmplifier,
				smokeParticleCount,
				dustParticleCount,
				showVisualCameo,
				visualDurationTicks,
				visualSpawnDistance,
				visualVerticalOffset,
				visualChargeVelocity,
				visualChargeVelocityBuffer,
				visualChargeDurationTicks
			);
		}
	}

	private record ComedicRewriteParrotSettings(
		int weight,
		double carryHeight,
		double carryHeightPerSeverity,
		double sideVelocity,
		double sideVelocityPerSeverity,
		int slowFallingDurationTicks,
		int levitationDurationTicks,
		int featherParticleCount,
		boolean showVisualCameo,
		int visualDurationTicks,
		int visualCount,
		double visualRadius,
		double visualVerticalOffset,
		boolean visualFollowPlayerHead,
		double visualLiftVelocity
	) {
		private static ComedicRewriteParrotSettings defaults(
			int weight,
			double carryHeight,
			double carryHeightPerSeverity,
			double sideVelocity,
			double sideVelocityPerSeverity,
			int slowFallingDurationTicks,
			int levitationDurationTicks,
			int featherParticleCount,
			boolean showVisualCameo,
			int visualDurationTicks,
			int visualCount,
			double visualRadius,
			double visualVerticalOffset,
			boolean visualFollowPlayerHead,
			double visualLiftVelocity
		) {
			return new ComedicRewriteParrotSettings(
				weight,
				carryHeight,
				carryHeightPerSeverity,
				sideVelocity,
				sideVelocityPerSeverity,
				slowFallingDurationTicks,
				levitationDurationTicks,
				featherParticleCount,
				showVisualCameo,
				visualDurationTicks,
				visualCount,
				visualRadius,
				visualVerticalOffset,
				visualFollowPlayerHead,
				visualLiftVelocity
			);
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
