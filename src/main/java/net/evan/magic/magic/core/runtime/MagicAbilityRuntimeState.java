package net.evan.magic.magic.ability;
import net.evan.magic.magic.core.ability.MagicAbility;

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


abstract class MagicAbilityRuntimeState extends PersistentRuntimeTypes {
	static final int TICKS_PER_SECOND = 20;
	static final int COOLDOWN_MESSAGE_DEBOUNCE_TICKS = 20;
	static final float DEFAULT_PLAYER_FLY_SPEED = 0.05F;
	static final RegistryKey<DamageType> TRUE_MAGIC_DAMAGE_TYPE = RegistryKey.of(
		RegistryKeys.DAMAGE_TYPE,
		Identifier.of(Magic.MOD_ID, "true_magic")
	);
	static MagicConfig.FrostConfig FROST_CONFIG = new MagicConfig.FrostConfig();
	static MagicConfig.BurningPassionConfig BURNING_PASSION_CONFIG = new MagicConfig.BurningPassionConfig();
	static MagicConfig.ConstellationCelestialAlignmentConfig CELESTIAL_ALIGNMENT_CONFIG = new MagicConfig.ConstellationCelestialAlignmentConfig();
	static int BELOW_FREEZING_COOLDOWN_TICKS = 30 * TICKS_PER_SECOND;
	static int FROST_ASCENT_COOLDOWN_TICKS = 0;
	static int BELOW_FREEZING_MANA_DRAIN_PER_SECOND = 5;
	static int ABSOLUTE_ZERO_MANA_DRAIN_PER_SECOND = 20;
	static int LOVE_AT_FIRST_SIGHT_IDLE_DRAIN_PER_SECOND = 2;
	static int LOVE_AT_FIRST_SIGHT_ACTIVE_DRAIN_PER_SECOND = 5;
	static double MARTYRS_FLAME_DRAIN_PERCENT_PER_SECOND = 1.0;
	static double TILL_DEATH_DO_US_PART_DRAIN_PERCENT_PER_SECOND = 3.0;
	static int MANIPULATION_ACTIVATION_MANA_COST = 0;
	static double MANIPULATION_MANA_DRAIN_PER_SECOND = 2.5;
	static int PASSIVE_MANA_REGEN_PER_SECOND = 2;
	static int DEPLETED_RECOVERY_REGEN_PER_SECOND = 1;
	static int EFFECT_REFRESH_TICKS = 40;
	static double SPOTLIGHT_DETECTION_RANGE = 48.0;
	static int SPOTLIGHT_EFFECT_REFRESH_TICKS = 10;
	static SpotlightStageSettings SPOTLIGHT_STAGE_ONE = SpotlightStageSettings.defaults(2, 200, 100, 200, 2.0, 0.08, 1.15, 0, 0, 2.0);
	static SpotlightStageSettings SPOTLIGHT_STAGE_TWO = SpotlightStageSettings.defaults(5, 100, 100, 200, 4.0, 0.16, 1.3, 1, 1, 5.0);
	static SpotlightStageSettings SPOTLIGHT_STAGE_THREE = SpotlightStageSettings.defaults(8, 100, 100, 200, 6.0, 0.24, 1.5, 2, 2, 10.0);
	static int SPOTLIGHT_MAX_HISTORY_TICKS = 200;
	static int WITTY_ONE_LINER_ACTIVATION_COST_PERCENT = 50;
	static int WITTY_ONE_LINER_RANGE = 32;
	static int WITTY_ONE_LINER_OVERLAY_FADE_IN_TICKS = 4;
	static int WITTY_ONE_LINER_OVERLAY_STAY_TICKS = 50;
	static int WITTY_ONE_LINER_OVERLAY_FADE_OUT_TICKS = 10;
	static double COMEDIC_REWRITE_BASE_PROC_CHANCE_PERCENT = 50.0;
	static double COMEDIC_REWRITE_SEVERITY_PROC_BONUS_PERCENT = 0.0;
	static double COMEDIC_REWRITE_LETHAL_PROC_BONUS_PERCENT = 0.0;
	static double COMEDIC_REWRITE_MAX_PROC_CHANCE_PERCENT = 50.0;
	static float COMEDIC_REWRITE_DANGEROUS_DAMAGE_THRESHOLD = 8.0F;
	static double COMEDIC_REWRITE_DANGEROUS_HEALTH_FRACTION_THRESHOLD = 0.45;
	static float COMEDIC_REWRITE_SEVERITY_DAMAGE_CAP = 20.0F;
	static double COMEDIC_REWRITE_MANA_COST_PERCENT = 20.0;
	static int COMEDIC_REWRITE_COOLDOWN_TICKS = 25 * TICKS_PER_SECOND;
	static int COMEDIC_REWRITE_IMMUNITY_TICKS = TICKS_PER_SECOND;
	static int COMEDIC_REWRITE_FALL_PROTECTION_TICKS = 4 * TICKS_PER_SECOND;
	static boolean COMEDIC_REWRITE_EXTINGUISH_ON_REWRITE = true;
	static double COMEDIC_REWRITE_MIN_SAVED_HEALTH_HEARTS = 1.0;
	static double COMEDIC_REWRITE_MAX_SAVED_HEALTH_HEARTS = 4.0;
	static int COMEDIC_REWRITE_SAFE_SEARCH_RADIUS = 14;
	static int COMEDIC_REWRITE_SAFE_SEARCH_VERTICAL_RANGE = 10;
	static int COMEDIC_REWRITE_UNSAFE_Y_BUFFER_BLOCKS = 16;
	static double COMEDIC_REWRITE_VISUAL_VIEW_DISTANCE = 48.0;
	static ComedicRewriteLaunchSettings COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE = ComedicRewriteLaunchSettings.defaults(
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
	static ComedicRewriteRavagerSettings COMEDIC_REWRITE_RAVAGER_BIT = ComedicRewriteRavagerSettings.defaults(
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
	static ComedicRewriteParrotSettings COMEDIC_REWRITE_PARROT_RESCUE = ComedicRewriteParrotSettings.defaults(
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
	static double COMEDIC_ASSISTANT_ACTIVATION_COST_PERCENT = 25.0;
	static int COMEDIC_ASSISTANT_ARMED_DURATION_TICKS = 15 * TICKS_PER_SECOND;
	static int COMEDIC_ASSISTANT_PROC_COOLDOWN_TICKS = 15 * TICKS_PER_SECOND;
	static int COMEDIC_ASSISTANT_CANCEL_COOLDOWN_TICKS = 3 * TICKS_PER_SECOND;
	static boolean COMEDIC_ASSISTANT_ALLOW_PLAYER_TARGETS = true;
	static boolean COMEDIC_ASSISTANT_ALLOW_MOB_TARGETS = true;
	static int COMEDIC_ASSISTANT_ARMED_INDICATOR_REFRESH_TICKS = TICKS_PER_SECOND;
	static int COMEDIC_ASSISTANT_ARMED_PARTICLE_COUNT = 4;
	static int COMEDIC_ASSISTANT_OVERLAY_FADE_IN_TICKS = 4;
	static int COMEDIC_ASSISTANT_OVERLAY_STAY_TICKS = 30;
	static int COMEDIC_ASSISTANT_OVERLAY_FADE_OUT_TICKS = 8;
	static final int COMEDIC_ASSISTANT_OVERLAY_COLOR_RGB = 0xFFD54A;
	static ComedicAssistantSlimeSettings COMEDIC_ASSISTANT_GIANT_SLIME_SLAM = ComedicAssistantSlimeSettings.defaults(
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
	static ComedicAssistantPandaSettings COMEDIC_ASSISTANT_PANDA_BOWLING_BALL = ComedicAssistantPandaSettings.defaults(
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
	static ComedicAssistantParrotSettings COMEDIC_ASSISTANT_PARROT_KIDNAPPING = ComedicAssistantParrotSettings.defaults(
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
	static ComedicAssistantDivineSettings COMEDIC_ASSISTANT_DIVINE_OVERREACTION = ComedicAssistantDivineSettings.defaults(
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
	static ComedicAssistantAcmeSettings COMEDIC_ASSISTANT_ACME_DROP = ComedicAssistantAcmeSettings.defaults(
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
	static ComedicAssistantPieSettings COMEDIC_ASSISTANT_PIE_TO_THE_FACE = ComedicAssistantPieSettings.defaults(
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
	static ComedicAssistantCaneSettings COMEDIC_ASSISTANT_GIANT_CANE_YANK = ComedicAssistantCaneSettings.defaults(
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
	static double PLUS_ULTRA_ACTIVATION_COST_PERCENT = 80.0;
	static int PLUS_ULTRA_DURATION_TICKS = 90 * TICKS_PER_SECOND;
	static double PLUS_ULTRA_OUTLINE_RADIUS = 150.0;
	static int PLUS_ULTRA_OUTLINE_REFRESH_TICKS = 10;
	static double PLUS_ULTRA_INCOMING_DAMAGE_MULTIPLIER = 0.1;
	static boolean PLUS_ULTRA_FLIGHT_ENABLED = true;
	static boolean PLUS_ULTRA_ELYTRA_POSE_WHILE_FLYING = true;
	static boolean PLUS_ULTRA_OVERHEAD_TEXT_ENABLED = true;
	static String PLUS_ULTRA_OVERHEAD_TEXT = "No More Jokes.";
	static int PLUS_ULTRA_OVERHEAD_TEXT_REFRESH_TICKS = 2;
	static int PLUS_ULTRA_OVERHEAD_TEXT_DURATION_TICKS = 3 * TICKS_PER_SECOND;
	static double PLUS_ULTRA_OVERHEAD_TEXT_VERTICAL_OFFSET = 2.85;
	static float PLUS_ULTRA_FLIGHT_FLY_SPEED = 0.16F;
	static double PLUS_ULTRA_FLIGHT_SPRINT_SPEED_MULTIPLIER = 1.75;
	static double PLUS_ULTRA_FLIGHT_ACCELERATION = 0.28;
	static double PLUS_ULTRA_FLIGHT_MAX_SPEED = 3.75;
	static double PLUS_ULTRA_FLIGHT_VERTICAL_ACCELERATION = 0.16;
	static double PLUS_ULTRA_FLIGHT_VERTICAL_MAX_SPEED = 1.35;
	static double PLUS_ULTRA_FLIGHT_DRAG = 0.95;
	static float PLUS_ULTRA_MELEE_BONUS_DAMAGE = 6.0F;
	static boolean PLUS_ULTRA_ALLOW_PLAYER_TARGETS = true;
	static boolean PLUS_ULTRA_ALLOW_MOB_TARGETS = true;
	static double PLUS_ULTRA_FLING_HORIZONTAL_STRENGTH = 5.75;
	static double PLUS_ULTRA_FLING_VERTICAL_STRENGTH = 1.05;
	static int PLUS_ULTRA_IMPACT_TRACKING_TICKS = 35;
	static double PLUS_ULTRA_IMPACT_VELOCITY_THRESHOLD = 1.1;
	static double PLUS_ULTRA_IMPACT_DAMAGE_MULTIPLIER = 3.0;
	static double PLUS_ULTRA_IMPACT_DAMAGE_MAX = 16.0;
	static int PLUS_ULTRA_SMOKE_PARTICLE_COUNT = 16;
	static double PLUS_ULTRA_SMOKE_PARTICLE_SPREAD = 0.4;
	static double PLUS_ULTRA_SMOKE_PARTICLE_SPEED = 0.05;
	static int PLUS_ULTRA_HIT_DUST_PARTICLE_COUNT = 10;
	static double PLUS_ULTRA_HIT_DUST_PARTICLE_SPREAD = 0.28;
	static double PLUS_ULTRA_HIT_DUST_PARTICLE_SPEED = 0.02;
	static int PLUS_ULTRA_IMPACT_PARTICLE_COUNT = 28;
	static double PLUS_ULTRA_IMPACT_PARTICLE_SPREAD = 0.55;
	static double PLUS_ULTRA_IMPACT_PARTICLE_SPEED = 0.12;
	static int PLUS_ULTRA_IMPACT_DUST_PARTICLE_COUNT = 20;
	static double PLUS_ULTRA_IMPACT_DUST_PARTICLE_SPREAD = 0.4;
	static double PLUS_ULTRA_IMPACT_DUST_PARTICLE_SPEED = 0.05;
	static float PLUS_ULTRA_IMPACT_SOUND_VOLUME = 1.0F;
	static float PLUS_ULTRA_IMPACT_SOUND_PITCH = 0.8F;
	static int PLUS_ULTRA_EARLY_CANCEL_PENALTY_DURATION_TICKS = 15 * TICKS_PER_SECOND;
	static int PLUS_ULTRA_EARLY_CANCEL_SLOWNESS_AMPLIFIER = 0;
	static int PLUS_ULTRA_EARLY_CANCEL_WEAKNESS_AMPLIFIER = 0;
	static int PLUS_ULTRA_FULL_END_PENALTY_DURATION_TICKS = 30 * TICKS_PER_SECOND;
	static int PLUS_ULTRA_FULL_END_SLOWNESS_AMPLIFIER = 1;
	static int PLUS_ULTRA_FULL_END_WEAKNESS_AMPLIFIER = 1;
	static int PLUS_ULTRA_EARLY_CANCEL_COOLDOWN_TICKS = 15 * 60 * TICKS_PER_SECOND;
	static int PLUS_ULTRA_FULL_END_COOLDOWN_TICKS = 20 * 60 * TICKS_PER_SECOND;
	static double CASSIOPEIA_DETECTION_RADIUS = 64.0;
	static int CASSIOPEIA_OUTLINE_REFRESH_TICKS = 10;
	static double HERCULES_TARGET_RANGE = 64.0;
	static double HERCULES_SPLASH_RADIUS = 5.0;
	static int HERCULES_EFFECT_DURATION_TICKS = 15 * TICKS_PER_SECOND;
	static int HERCULES_WARNING_FADE_IN_TICKS = 5;
	static int HERCULES_WARNING_STAY_TICKS = 50;
	static int HERCULES_WARNING_FADE_OUT_TICKS = 5;
	static float HERCULES_WARNING_SCALE = 0.9F;
	static double HERCULES_ACTIVATION_COST_PERCENT = 30.0;
	static int HERCULES_COOLDOWN_TICKS = 45 * TICKS_PER_SECOND;
	static float HERCULES_TRUE_DAMAGE = 5.0F;
	static int HERCULES_SLOWNESS_AMPLIFIER = 255;
	static int HERCULES_RESISTANCE_AMPLIFIER = 0;
	static int HERCULES_PARTICLE_INTERVAL_TICKS = 5;
	static int HERCULES_WARNING_COLOR_RGB = 0x39B7FF;
	static boolean HERCULES_DISABLE_MANA_REGEN_WHILE_ACTIVE = true;
	static boolean HERCULES_PREVENT_CASTER_DIRECT_DAMAGE = true;
	static boolean HERCULES_INTERRUPT_TARGET_ITEM_USE = false;
	static int HERCULES_ACTIVATION_IMPACT_DIRT_PARTICLE_COUNT = 18;
	static double HERCULES_ACTIVATION_IMPACT_DIRT_PARTICLE_SPEED = 0.08;
	static float HERCULES_ACTIVATION_IMPACT_SOUND_VOLUME = 1.2F;
	static float HERCULES_ACTIVATION_IMPACT_SOUND_PITCH = 0.65F;
	static double ORIONS_GAMBIT_TARGET_RANGE = 64.0;
	static int ORIONS_GAMBIT_WAIT_CANCEL_COOLDOWN_TICKS = 3 * TICKS_PER_SECOND;
	static int ORIONS_GAMBIT_LINK_DURATION_TICKS = 40 * TICKS_PER_SECOND;
	static double ORIONS_GAMBIT_MANA_DRAIN_PERCENT_PER_SECOND = 10.0;
	static boolean ORIONS_GAMBIT_DRAIN_MANA_WHILE_WAITING_FOR_TARGET = false;
	static boolean ORIONS_GAMBIT_DISABLE_MANA_REGEN_WHILE_WAITING_FOR_TARGET = true;
	static boolean ORIONS_GAMBIT_DISABLE_MANA_REGEN_WHILE_LINKED = true;
	static int ORIONS_GAMBIT_COOLDOWN_TICKS = 10 * 60 * TICKS_PER_SECOND;
	static int ORIONS_GAMBIT_CASTER_PENALTY_REFRESH_TICKS = TICKS_PER_SECOND;
	static int ORIONS_GAMBIT_CASTER_WEAKNESS_AMPLIFIER = 2;
	static int ORIONS_GAMBIT_CASTER_SLOWNESS_AMPLIFIER = 2;
	static double ORIONS_GAMBIT_CASTER_MAX_HEALTH_HEARTS = 5.0;
	static double ORIONS_GAMBIT_CASTER_INCOMING_DAMAGE_MULTIPLIER = 1.5;
	static boolean ORIONS_GAMBIT_CLEAR_TARGET_COOLDOWNS_ON_LOCK = true;
	static boolean ORIONS_GAMBIT_SUPPRESS_TARGET_MANA_COSTS = true;
	static boolean ORIONS_GAMBIT_SUPPRESS_TARGET_COOLDOWNS = true;
	static boolean ORIONS_GAMBIT_RESET_CASTER_COOLDOWNS_ON_END = true;
	static boolean ORIONS_GAMBIT_APPLY_USED_TARGET_COOLDOWNS_ON_END = true;
	static int ORIONS_GAMBIT_TARGET_PARTICLE_INTERVAL_TICKS = 4;
	static int ORIONS_GAMBIT_TARGET_PARTICLE_BURST_COUNT = 12;
	static double ORIONS_GAMBIT_TARGET_PARTICLE_SPAWN_RADIUS = 0.28;
	static double ORIONS_GAMBIT_TARGET_PARTICLE_VERTICAL_VELOCITY = 0.22;
	static double ORIONS_GAMBIT_TARGET_PARTICLE_FORWARD_VELOCITY = 0.09;
	static double ORIONS_GAMBIT_TARGET_PARTICLE_SIDE_VELOCITY = 0.05;
	static int ORIONS_GAMBIT_GREED_TARGET_COIN_UNITS = 18 * MagicPlayerData.GREED_COIN_UNITS_PER_COIN;
	static boolean ORIONS_GAMBIT_RESET_GREED_TARGET_COINS_ON_END = true;
	static int ASTRAL_CATACLYSM_CHARGE_DURATION_TICKS = 30 * TICKS_PER_SECOND;
	static int ASTRAL_CATACLYSM_ACQUIRE_WINDOW_TICKS = 60 * TICKS_PER_SECOND;
	static int ASTRAL_CATACLYSM_DURATION_TICKS = 2 * 60 * TICKS_PER_SECOND;
	static List<Integer> ASTRAL_CATACLYSM_EXPIRY_WARNING_TICKS = List.of(30 * TICKS_PER_SECOND, 10 * TICKS_PER_SECOND, 5 * TICKS_PER_SECOND);
	static boolean ASTRAL_CATACLYSM_ALLOW_MOB_TARGETS = false;
	static double ASTRAL_CATACLYSM_TARGET_RANGE = 64.0;
	static double ASTRAL_CATACLYSM_BEAM_RADIUS = 3.0;
	static double ASTRAL_CATACLYSM_BEAM_RISE_BLOCKS_PER_SECOND = 2.0;
	static double ASTRAL_CATACLYSM_BEAM_PARTICLE_STEP = 1.5;
	static double ASTRAL_CATACLYSM_BEAM_DESCENT_BLOCKS_PER_SECOND = 18.0;
	static int ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_COUNT = 2;
	static int ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_COUNT = 2;
	static int ASTRAL_CATACLYSM_BEAM_FALLING_PARTICLE_COUNT = 18;
	static int ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_LIFETIME_TICKS = 5;
	static int ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_LIFETIME_TICKS = 6;
	static int ASTRAL_CATACLYSM_BEAM_FALLING_PARTICLE_LIFETIME_TICKS = 12;
	static float ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_SCALE = 1.2F;
	static float ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_SCALE = 0.78F;
	static float ASTRAL_CATACLYSM_BEAM_FALLING_PARTICLE_SCALE = 0.92F;
	static int ASTRAL_CATACLYSM_BEAM_CORE_COLOR_RGB = 0xFFF6D6;
	static int ASTRAL_CATACLYSM_BEAM_OUTER_COLOR_RGB = 0x8AD7FF;
	static float ASTRAL_CATACLYSM_BEAM_CORE_INTENSITY = 0.95F;
	static int ASTRAL_CATACLYSM_BEAM_SPIRAL_PARTICLE_COUNT = 16;
	static double ASTRAL_CATACLYSM_BEAM_SPIRAL_ORBIT_RADIUS = 3.25;
	static double ASTRAL_CATACLYSM_BEAM_SPIRAL_ANGULAR_SPEED_RADIANS_PER_TICK = Math.toRadians(14.0);
	static double ASTRAL_CATACLYSM_BEAM_SPIRAL_VERTICAL_SPACING = 9.0;
	static List<Integer> ASTRAL_CATACLYSM_BEAM_SPIRAL_COLOR_RGBS = List.of(0x8CD8FF, 0xFFDFA2, 0xC4A6FF);
	static int ASTRAL_CATACLYSM_BEAM_SPIRAL_PARTICLE_LIFETIME_TICKS = 18;
	static float ASTRAL_CATACLYSM_BEAM_SPIRAL_PARTICLE_SCALE = 0.7F;
	static int ASTRAL_CATACLYSM_BEAM_DAMAGE_INTERVAL_TICKS = TICKS_PER_SECOND;
	static float ASTRAL_CATACLYSM_BEAM_TRUE_DAMAGE_PER_INTERVAL = 4.0F;
	static int ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_INTERVAL_TICKS = TICKS_PER_SECOND;
	static float ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_VOLUME = 1.0F;
	static float ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_PITCH = 1.2F;
	static int ASTRAL_CATACLYSM_COOLDOWN_TICKS = 15 * 60 * TICKS_PER_SECOND;
	static int ASTRAL_CATACLYSM_FREEZE_REFRESH_TICKS = 5;
	static boolean ASTRAL_CATACLYSM_DISABLE_MANA_REGEN_WHILE_ACTIVE = true;
	static boolean ASTRAL_CATACLYSM_IGNORE_TOTEMS = true;
	static boolean ASTRAL_CATACLYSM_PRESERVE_BEACON_ANCHOR = true;
	static Identifier ASTRAL_CATACLYSM_BEACON_ANCHOR_BLOCK_ID = Identifier.of("evanpack", "beacon_anchor");
	static Identifier ASTRAL_CATACLYSM_BEACON_CORE_ITEM_ID = Identifier.of("evanpack", "beacon_core");
	static String ASTRAL_CATACLYSM_BEACON_CORE_ANCHOR_STATE_ID = "evanpack_beacon_core_anchor";
	static double ASTRAL_CATACLYSM_BEACON_CORE_PROTECTION_RADIUS = 75.0;
	static boolean ASTRAL_CATACLYSM_CANCEL_BEAM_ON_PROTECTED_BEACON_CORE_HOLDER = true;
	static WittyOneLinerTierSettings WITTY_ONE_LINER_LOW_TIER = WittyOneLinerTierSettings.defaults(
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
	static WittyOneLinerTierSettings WITTY_ONE_LINER_MID_TIER = WittyOneLinerTierSettings.defaults(
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
	static WittyOneLinerTierSettings WITTY_ONE_LINER_HIGH_TIER = WittyOneLinerTierSettings.defaults(
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

	static int FROSTBITE_DURATION_TICKS = 120;
	static int FROSTBITE_DAMAGE_INTERVAL_TICKS = 20;
	static float FROSTBITE_DAMAGE = 2.0F;
	static int FROSTBITE_SLOWNESS_AMPLIFIER = 3;
	static int BELOW_FREEZING_AURA_SLOWNESS_AMPLIFIER = 3;
	static int BELOW_FREEZING_RESISTANCE_AMPLIFIER = 0;
	static int ABSOLUTE_ZERO_RESISTANCE_AMPLIFIER = 2;
	static int PLANCK_HEAT_ABSORPTION_AMPLIFIER = 3;
	static int PLANCK_HEAT_STRENGTH_AMPLIFIER = 1;
	static int PLANCK_HEAT_FIRE_RESISTANCE_AMPLIFIER = 0;
	static int PLANCK_HEAT_FIRE_PHASE_HUNGER_AMPLIFIER = 1;
	static int PLANCK_HEAT_FIRE_PHASE_NAUSEA_AMPLIFIER = 1;
	static int FROST_VISUAL_TICKS = 180;
	static boolean POWDERED_SNOW_DAMAGE_ON_INITIAL_APPLICATION = true;

	static int BELOW_FREEZING_AURA_RADIUS = 1;
	static int ABSOLUTE_ZERO_AURA_RADIUS = 4;
	static int ABSOLUTE_ZERO_AURA_DAMAGE_INTERVAL_TICKS = 50;
	static float ABSOLUTE_ZERO_AURA_DAMAGE = 4.0F;
	static int ABSOLUTE_ZERO_AURA_SLOWNESS_AMPLIFIER = 2;
	static int ABSOLUTE_ZERO_COOLDOWN_TICKS = 60 * TICKS_PER_SECOND;
	static int MARTYRS_FLAME_COOLDOWN_TICKS = 30 * TICKS_PER_SECOND;
	static int MARTYRS_FLAME_FIRE_DURATION_TICKS = 4 * TICKS_PER_SECOND;
	static float MARTYRS_FLAME_RETALIATION_DAMAGE = 2.0F;
	static int PLANCK_HEAT_ACTIVATION_MANA_COST = 75;
	static int PLANCK_HEAT_COOLDOWN_TICKS = 10 * 60 * TICKS_PER_SECOND;
	static int PLANCK_HEAT_AURA_RADIUS = 8;
	static int PLANCK_HEAT_FROST_PHASE_TICKS = 6 * TICKS_PER_SECOND;
	static int PLANCK_HEAT_FIRE_PHASE_TICKS = 15 * TICKS_PER_SECOND;
	static int PLANCK_HEAT_FROST_DAMAGE_INTERVAL_TICKS = 20;
	static float PLANCK_HEAT_FROST_DAMAGE = 14.0F;
	static int PLANCK_HEAT_FROST_SLOWNESS_AMPLIFIER = 4;
	static int PLANCK_HEAT_ATTACK_ENHANCED_FIRE_DURATION_TICKS = 8 * TICKS_PER_SECOND;
	static int PLANCK_HEAT_AURA_ENHANCED_FIRE_DURATION_TICKS = 4 * TICKS_PER_SECOND;
	static int PLANCK_HEAT_ENHANCED_FIRE_DAMAGE_INTERVAL_TICKS = 20;
	static float PLANCK_HEAT_ENHANCED_FIRE_DAMAGE = 6.0F;
	static int PLANCK_HEAT_ABSORPTION_DURATION_TICKS = 2 * 60 * TICKS_PER_SECOND;
	static int PLANCK_HEAT_STRENGTH_DURATION_TICKS = 2 * 60 * TICKS_PER_SECOND;
	static int PLANCK_HEAT_FIRE_RESISTANCE_DURATION_TICKS = 8 * 60 * TICKS_PER_SECOND;
	static int LOVE_GAZE_RANGE = 32;
	static int LOVE_LOCK_EFFECT_TICKS = 5;
	static int LOVE_LOCK_SLOWNESS_AMPLIFIER = 255;
	static int LOVE_LOCK_MINING_FATIGUE_AMPLIFIER = 255;
	static int LOVE_AT_FIRST_SIGHT_RESISTANCE_AMPLIFIER = 0;
	static boolean LOVE_AT_FIRST_SIGHT_BLOCK_ITEM_USE = false;
	static boolean LOVE_AT_FIRST_SIGHT_BLOCK_ATTACKS = true;
	static boolean LOVE_AT_FIRST_SIGHT_PREVENT_CASTER_DIRECT_DAMAGE = true;
	static boolean MARTYRS_FLAME_APPLY_GLOWING_EFFECT = true;
	static boolean MARTYRS_FLAME_DISABLE_MANA_REGEN_WHILE_ACTIVE = true;
	static boolean MARTYRS_FLAME_FIRE_IGNORES_NORMAL_EXTINGUISH = true;
	static int MARTYRS_FLAME_FIRE_PARTICLE_INTERVAL_TICKS = 4;
	static int MARTYRS_FLAME_FIRE_FLAME_PARTICLE_COUNT = 6;
	static int MARTYRS_FLAME_FIRE_SMOKE_PARTICLE_COUNT = 4;
	static int DOMAIN_CLASH_REGENERATION_AMPLIFIER = 9;
	static int DOMAIN_CLASH_INSTANT_HEALTH_AMPLIFIER = 9;
	static int DOMAIN_CLASH_RESISTANCE_AMPLIFIER = 5;
	static int TILL_DEATH_DO_US_PART_LINK_DURATION_TICKS = 30 * TICKS_PER_SECOND;
	static int TILL_DEATH_DO_US_PART_COOLDOWN_TICKS = 150 * TICKS_PER_SECOND;
	static int MANIPULATION_COOLDOWN_TICKS = 6000;
	static int MANIPULATION_REQUEST_DEBOUNCE_TICKS = 6;
	static double MANIPULATION_MAX_INPUT_DELTA_PER_TICK = 0.45;
	static double MANIPULATION_VERTICAL_INPUT_DELTA_PER_TICK = 0.35;
	static int MANIPULATION_CLAMP_LOG_INTERVAL_TICKS = 20;
	static int MANIPULATION_ACQUIRE_RANGE = 100;
	static int MANIPULATION_BREAK_RANGE = 100;
	static boolean MANIPULATION_DEACTIVATE_TARGET_MAGIC = true;
	static boolean MANIPULATION_DISABLE_ARTIFACT_POWERS = true;
	static boolean MANIPULATION_DISMISS_ARTIFACT_SUMMONS = true;
	static boolean MANIPULATION_DISABLE_ARTIFACT_ARMOR_EFFECTS = true;
	static boolean MANIPULATION_DISABLE_INFESTED_SILVERFISH_ENHANCEMENTS = true;
	static boolean MANIPULATION_BLOCK_ARTIFACT_USE_CLICKS = true;
	static boolean MANIPULATION_BLOCK_ARTIFACT_ATTACK_CLICKS = true;
	static boolean MANIPULATION_DEBUG_LOGGING = true;
	static int DOMAIN_EXPANSION_ACTIVATION_MANA_COST = 0;
	static int DOMAIN_EXPANSION_DURATION_TICKS = 60 * TICKS_PER_SECOND;
	static int FROST_DOMAIN_COOLDOWN_TICKS = 30 * 60 * TICKS_PER_SECOND;
	static int LOVE_DOMAIN_COOLDOWN_TICKS = 30 * 60 * TICKS_PER_SECOND;
	static int LOVE_AT_FIRST_SIGHT_PARTICLE_INTERVAL_TICKS = 8;
	static int LOVE_AT_FIRST_SIGHT_HEART_PARTICLES = 1;
	static int LOVE_AT_FIRST_SIGHT_HAPPY_VILLAGER_PARTICLES = 0;
	static int DOMAIN_EXPANSION_RADIUS = 25;
	static int DOMAIN_EXPANSION_HEIGHT = 25;
	static int DOMAIN_EXPANSION_SHELL_THICKNESS = 1;
	static boolean DOMAIN_CLASH_ENABLED = true;
	static int DOMAIN_CLASH_SIMULTANEOUS_WINDOW_TICKS = 1;
	static int DOMAIN_CLASH_MIN_EXTERIOR_DISTANCE = 20;
	static int DOMAIN_CLASH_TITLE_FADE_IN_TICKS = 8;
	static int DOMAIN_CLASH_TITLE_STAY_TICKS = 44;
	static int DOMAIN_CLASH_TITLE_FADE_OUT_TICKS = 8;
	static int DOMAIN_CLASH_INSTRUCTIONS_DURATION_TICKS = 15 * TICKS_PER_SECOND;
	static int DOMAIN_CLASH_INSTRUCTIONS_FADE_OUT_TICKS = TICKS_PER_SECOND;
	static int DOMAIN_CLASH_REGENERATION_REFRESH_TICKS = 40;
	static double DOMAIN_CLASH_DAMAGE_TO_WIN = 250.0;
	static int DOMAIN_CLASH_LOSER_MANA_DRAIN_PERCENT = 50;
	static double DOMAIN_CLASH_POST_CLASH_COOLDOWN_MULTIPLIER = 0.5;
	static int DOMAIN_CLASH_PARTICLES_PER_TICK = 120;
	static int DOMAIN_CLASH_SPLIT_PATTERN_MODULO = 2;
	static boolean DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS = false;
	static boolean DOMAIN_CLASH_FORCE_LOOK = true;
	static boolean DOMAIN_CLASH_PARTICIPANTS_INVINCIBLE = false;
	static boolean DOMAIN_CLASH_PAUSE_TIMED_DOMAIN_COLLAPSE_TIMERS = true;
	static boolean DOMAIN_CLASH_PAUSE_ASTRAL_CATACLYSM_PHASE_TIMERS = true;
	static boolean DOMAIN_CONTROL_BLOCK_TELEPORT_ACROSS_BOUNDARIES = true;
	static boolean DOMAIN_CONTROL_INTERIOR_LIGHTING_ENABLED = true;
	static int DOMAIN_CONTROL_INTERIOR_LIGHT_HORIZONTAL_SPACING = 6;
	static int DOMAIN_CONTROL_INTERIOR_LIGHT_VERTICAL_SPACING = 6;
	static int DOMAIN_CONTROL_INTERIOR_LIGHT_START_Y_OFFSET = 2;
	static final int DOMAIN_BLOCK_PLACE_FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE_AND_SKIP_CALLBACKS_AND_DROPS;
	static final int DOMAIN_BLOCK_RESTORE_FLAGS = Block.NOTIFY_ALL | Block.FORCE_STATE_AND_SKIP_CALLBACKS_AND_DROPS;
	static final BlockState DOMAIN_SHELL_BLOCK_STATE = Blocks.BLACK_CONCRETE.getDefaultState();
	static final BlockState DOMAIN_INTERIOR_BLOCK_STATE = Blocks.AIR.getDefaultState();
	static BlockState DOMAIN_INTERIOR_LIGHT_STATE = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15);
	static final ParticleEffect HERCULES_ACTIVATION_DIRT_PARTICLE = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DIRT.getDefaultState());
	static final Identifier SPOTLIGHT_ATTACK_DAMAGE_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "spotlight_attack_damage");
	static final Identifier SPOTLIGHT_MOVEMENT_SPEED_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "spotlight_movement_speed");
	static final Identifier SPOTLIGHT_MAX_HEALTH_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "spotlight_max_health");
	static final Identifier SPOTLIGHT_SCALE_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "spotlight_scale");
	static final Identifier CELESTIAL_ALIGNMENT_BOOTES_SPEED_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "celestial_alignment_bootes_speed");
	static final Identifier ORIONS_GAMBIT_MAX_HEALTH_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "orions_gambit_max_health");
	static final Identifier FROST_STAGE_MAX_HEALTH_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "frost_stage_max_health");
	static final Identifier BURNING_PASSION_STEP_HEIGHT_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "burning_passion_step_height");
	static final Identifier BURNING_PASSION_ENGINE_HEART_ATTACK_DAMAGE_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "burning_passion_engine_heart_attack_damage");
	static final ParticleEffect FROST_SHARD_PARTICLE = new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(ModItems.FROST_SHARD));
	static final double DOMAIN_TELEPORT_POSITION_EPSILON_SQUARED = 4.0E-4;
	static final float DOMAIN_TELEPORT_ROTATION_EPSILON_DEGREES = 0.5F;

	static final List<net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect>> COMMON_NEGATIVE_EFFECTS = List.of(
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

	static boolean initialized;
	static final Set<UUID> CASSIOPEIA_PASSIVE_ENABLED = new HashSet<>();
	static final Map<UUID, Set<UUID>> CASSIOPEIA_LAST_OUTLINED_PLAYERS = new HashMap<>();
	static final Map<UUID, FrostbiteState> FROSTBITTEN_TARGETS = new HashMap<>();
	static final Map<UUID, FrostbiteState> FROST_DOMAIN_FROSTBITTEN_TARGETS = new HashMap<>();
	static final Map<UUID, FrostDomainPulseStatusState> FROST_DOMAIN_PULSE_STATUS_TARGETS = new HashMap<>();
	static final Map<UUID, FrostStageState> FROST_STAGE_STATES = new HashMap<>();
	static final Map<UUID, FrostMaximumState> FROST_MAXIMUM_STATES = new HashMap<>();
	static final List<FrostSpikeWaveState> FROST_SPIKE_WAVES = new ArrayList<>();
	static final Map<UUID, FrostFreezeState> FROST_FROZEN_TARGETS = new HashMap<>();
	static final Map<UUID, FrostFreezeState> FROST_DOMAIN_FROZEN_TARGETS = new HashMap<>();
	static final Map<UUID, FrostFearState> FROST_MAXIMUM_FEAR_TARGETS = new HashMap<>();
	static final Map<UUID, FrostHelplessState> FROST_HELPLESS_TARGETS = new HashMap<>();
	static final Map<UUID, FrostPackedIceState> FROST_PACKED_ICE_TARGETS = new HashMap<>();
	static final Map<UUID, Integer> BELOW_FREEZING_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> FROST_ASCENT_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> FROST_MANA_REGEN_BLOCKED_END_TICK = new HashMap<>();
	static final Map<UUID, Double> MANA_REGEN_BUFFER = new HashMap<>();
	static final Map<UUID, Map<UUID, Integer>> ABSOLUTE_ZERO_NEXT_DAMAGE_TICK = new HashMap<>();
	static final Map<UUID, Integer> ABSOLUTE_ZERO_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> PLANCK_HEAT_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, PlanckHeatState> PLANCK_HEAT_STATES = new HashMap<>();
	static final Map<UUID, Map<UUID, Integer>> PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK = new HashMap<>();
	static final Map<UUID, EnhancedFireState> ENHANCED_FIRE_TARGETS = new HashMap<>();
	static final Map<UUID, LoveLockState> LOVE_LOCKED_TARGETS = new HashMap<>();
	static final Map<UUID, Boolean> LOVE_POWER_ACTIVE_THIS_SECOND = new HashMap<>();
	static final Set<UUID> MARTYRS_FLAME_PASSIVE_ENABLED = new HashSet<>();
	static final Set<UUID> SPOTLIGHT_PASSIVE_ENABLED = new HashSet<>();
	static final Map<UUID, SpotlightState> SPOTLIGHT_STATES = new HashMap<>();
	static final Map<UUID, Integer> MARTYRS_FLAME_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> WITTY_ONE_LINER_COOLDOWN_END_TICK = new HashMap<>();
	static final Set<UUID> COMEDIC_REWRITE_PASSIVE_ENABLED = new HashSet<>();
	static final Map<UUID, Integer> COMEDIC_REWRITE_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> COMEDIC_REWRITE_IMMUNITY_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> COMEDIC_REWRITE_FALL_PROTECTION_END_TICK = new HashMap<>();
	static final Map<UUID, PendingComedicRewriteState> COMEDIC_REWRITE_PENDING_STATES = new HashMap<>();
	static final List<ComedicRewriteVisualCameo> COMEDIC_REWRITE_VISUAL_CAMEOS = new ArrayList<>();
	static final Map<UUID, ComedicAssistantArmedState> COMEDIC_ASSISTANT_ARMED_STATES = new HashMap<>();
	static final Map<UUID, Integer> COMEDIC_ASSISTANT_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, ComedicAssistantParrotCarryState> COMEDIC_ASSISTANT_PARROT_CARRY_STATES = new HashMap<>();
	static final Map<UUID, ComedicAssistantAcmeVisualState> COMEDIC_ASSISTANT_ACME_VISUALS = new HashMap<>();
	static final Map<UUID, ComedicAssistantCaneImpactState> COMEDIC_ASSISTANT_CANE_IMPACT_STATES = new HashMap<>();
	static final Map<UUID, Integer> PLUS_ULTRA_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, PlusUltraState> PLUS_ULTRA_STATES = new HashMap<>();
	static final Map<UUID, Set<UUID>> PLUS_ULTRA_LAST_OUTLINED_PLAYERS = new HashMap<>();
	static final Map<UUID, PlusUltraImpactState> PLUS_ULTRA_IMPACT_STATES = new HashMap<>();
	static final Set<UUID> PLUS_ULTRA_DAMAGE_SCALING_GUARD = new HashSet<>();
	static final Map<UUID, Double> MARTYRS_FLAME_DRAIN_BUFFER = new HashMap<>();
	static final Map<UUID, MartyrsFlameBurnState> MARTYRS_FLAME_BURNING_TARGETS = new HashMap<>();
	static final Map<UUID, BurningPassionIgnitionState> BURNING_PASSION_IGNITION_STATES = new HashMap<>();
	static final Map<UUID, BurningPassionAuraFireState> BURNING_PASSION_AURA_FIRE_TARGETS = new HashMap<>();
	static final Map<UUID, BurningPassionAuraFireState> BURNING_PASSION_SELF_FIRE_TARGETS = new HashMap<>();
	static final Map<UUID, BurningPassionSearingDashState> BURNING_PASSION_SEARING_DASH_STATES = new HashMap<>();
	static final List<BurningPassionTrailLineState> BURNING_PASSION_TRAIL_LINES = new ArrayList<>();
	static final Map<UUID, BurningPassionCinderMarkArmedState> BURNING_PASSION_CINDER_MARK_ARMED_STATES = new HashMap<>();
	static final List<BurningPassionCinderMarkState> BURNING_PASSION_CINDER_MARKS = new ArrayList<>();
	static final Map<UUID, BurningPassionEngineHeartState> BURNING_PASSION_ENGINE_HEART_STATES = new HashMap<>();
	static final Map<UUID, BurningPassionHudNotificationState> BURNING_PASSION_HUD_NOTIFICATION_STATES = new HashMap<>();
	static final Map<UUID, BurningPassionPendingMeleeImpactState> BURNING_PASSION_PENDING_MELEE_IMPACTS = new HashMap<>();
	static final Map<UUID, OverrideMeteorState> OVERRIDE_METEOR_STATES = new HashMap<>();
	static final Map<UUID, Integer> IGNITION_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> SEARING_DASH_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> CINDER_MARK_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> ENGINE_HEART_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> OVERRIDE_COOLDOWN_END_TICK = new HashMap<>();
	static final Set<UUID> TILL_DEATH_DO_US_PART_PASSIVE_ENABLED = new HashSet<>();
	static final Map<UUID, TillDeathDoUsPartState> TILL_DEATH_DO_US_PART_STATES = new HashMap<>();
	static final Map<UUID, Integer> TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Double> TILL_DEATH_DO_US_PART_DRAIN_BUFFER = new HashMap<>();
	static final Map<UUID, Integer> TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK = new HashMap<>();
	static final Map<UUID, Double> MANIPULATION_DRAIN_BUFFER = new HashMap<>();
	static final Map<UUID, HerculesBurdenState> HERCULES_STATES = new HashMap<>();
	static final Map<UUID, AstralBurdenTargetState> HERCULES_TARGETS = new HashMap<>();
	static final Map<UUID, Integer> HERCULES_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, CelestialAlignmentSessionState> SAGITTARIUS_STATES = new HashMap<>();
	static final Map<UUID, Integer> SAGITTARIUS_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Double> SAGITTARIUS_DRAIN_BUFFER = new HashMap<>();
	static final Map<UUID, CelestialGamaRayState> CELESTIAL_GAMA_RAY_STATES = new HashMap<>();
	static final List<CelestialGeminiReplayState> CELESTIAL_GEMINI_REPLAYS = new ArrayList<>();
	static final Map<UUID, List<DelayedCelestialAction>> CELESTIAL_DELAYED_ACTIONS = new HashMap<>();
	static final Set<UUID> CELESTIAL_DAMAGE_REDIRECT_GUARD = new HashSet<>();
	static final Set<UUID> CELESTIAL_DAMAGE_SCALE_GUARD = new HashSet<>();
	static final ThreadLocal<Integer> CELESTIAL_DELAY_BYPASS_DEPTH = ThreadLocal.withInitial(() -> 0);
	static final ThreadLocal<Integer> CELESTIAL_TRANSFORM_BYPASS_DEPTH = ThreadLocal.withInitial(() -> 0);
	static final Map<UUID, OrionGambitState> ORIONS_GAMBIT_STATES = new HashMap<>();
	static final Map<UUID, OrionPenaltyState> ORIONS_GAMBIT_PENALTIES = new HashMap<>();
	static final Map<UUID, UUID> ORIONS_GAMBIT_CASTER_BY_TARGET = new HashMap<>();
	static final Map<UUID, Integer> ORIONS_GAMBIT_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Double> ORIONS_GAMBIT_DRAIN_BUFFER = new HashMap<>();
	static final Set<UUID> ORIONS_GAMBIT_DAMAGE_SCALING_GUARD = new HashSet<>();
	static final Map<UUID, ConstellationDomainState> ASTRAL_CATACLYSM_DOMAIN_STATES = new HashMap<>();
	static final Map<UUID, AstralExecutionBeamState> ASTRAL_EXECUTION_BEAMS = new HashMap<>();
	static final Map<UUID, Integer> ASTRAL_CATACLYSM_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> GREED_DOMAIN_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, ManipulationState> MANIPULATION_STATES = new HashMap<>();
	static final Map<UUID, UUID> MANIPULATION_CASTER_BY_TARGET = new HashMap<>();
	static final Map<UUID, PlayerInput> MANIPULATION_INPUT_BY_CASTER = new HashMap<>();
	static final Map<UUID, ManipulationLookState> MANIPULATION_LOOK_BY_CASTER = new HashMap<>();
	static final Map<UUID, Integer> MANIPULATION_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> MANIPULATION_NEXT_REQUEST_TICK = new HashMap<>();
	static final Map<UUID, Integer> MANIPULATION_LAST_CLAMP_LOG_TICK = new HashMap<>();
	static final Map<UUID, ManipulationProxyState> MANIPULATION_INTERACTION_PROXY = new HashMap<>();
	static final Map<UUID, DomainExpansionState> DOMAIN_EXPANSIONS = new HashMap<>();
	static final Map<UUID, DomainClashState> DOMAIN_CLASHES_BY_OWNER = new HashMap<>();
	static final Map<UUID, UUID> DOMAIN_CLASH_OWNER_BY_PARTICIPANT = new HashMap<>();
	static final Map<UUID, DomainClashPendingDamageState> DOMAIN_CLASH_PENDING_DAMAGE = new HashMap<>();
	static final Map<UUID, UUID> MAGIC_DAMAGE_PENDING_ATTACKER = new HashMap<>();
	static final Map<UUID, Integer> FROST_DOMAIN_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> LOVE_DOMAIN_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, DomainPendingReturnState> DOMAIN_PENDING_RETURNS = new HashMap<>();
	static final RegistryEntryLookup<Block> BLOCK_ENTRY_LOOKUP = Registries.createEntryLookup(Registries.BLOCK);
	static DomainRuntimePersistentState domainRuntimePersistentState;
	static final String DOMAIN_PERSISTENCE_DOMAINS_KEY = "domains";
	static final String DOMAIN_PERSISTENCE_LOVE_COOLDOWNS_KEY = "loveDomainCooldowns";
	static final String DOMAIN_PERSISTENCE_FROST_COOLDOWNS_KEY = "frostDomainCooldowns";
	static final String DOMAIN_PERSISTENCE_PENDING_RETURNS_KEY = "pendingDomainReturns";
	static final String EMPTY_EMBRACE_TAG = "magic.empty_embrace";
	static final String EMPTY_EMBRACE_ARTIFACT_POWERS_TAG = "magic.empty_embrace.artifact_powers";
	static final String EMPTY_EMBRACE_ARTIFACT_SUMMONS_TAG = "magic.empty_embrace.artifact_summons";
	static final String EMPTY_EMBRACE_ARTIFACT_ARMOR_TAG = "magic.empty_embrace.artifact_armor";
	static final String EMPTY_EMBRACE_INFESTED_SILVERFISH_TAG = "magic.empty_embrace.infested_silverfish";
	static final String ARTIFACT_ITEM_NAMESPACE = "evanpack";
	static final ThreadLocal<Integer> DOMAIN_TELEPORT_BYPASS_DEPTH = ThreadLocal.withInitial(() -> 0);
	static final int BEACON_CORE_ANCHOR_CACHE_REFRESH_TICKS = 20;
	static BeaconCoreAnchorState cachedBeaconCoreAnchor;
	static int cachedBeaconCoreAnchorTick = Integer.MIN_VALUE;
	static final Set<UUID> TEST_MODE_PLAYERS = new HashSet<>();

}


