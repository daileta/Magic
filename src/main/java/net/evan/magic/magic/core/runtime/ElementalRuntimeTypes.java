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


abstract class ElementalRuntimeTypes extends LoveAndDomainRuntimeTypes {
	static final class MartyrsFlameBurnState {
		final RegistryKey<World> dimension;
		UUID casterId;
		int expiresTick;
		int nextDamageTick;
		float damagePerTick;
		double fireResistantTargetDamageMultiplier;
		boolean fireDamageIgnoresFireResistance;
		int damageIntervalTicks;
		boolean extinguishInWater;
		boolean extinguishInRain;

		MartyrsFlameBurnState(
			RegistryKey<World> dimension,
			UUID casterId,
			int expiresTick,
			int nextDamageTick,
			float damagePerTick,
			double fireResistantTargetDamageMultiplier,
			boolean fireDamageIgnoresFireResistance,
			int damageIntervalTicks,
			boolean extinguishInWater,
			boolean extinguishInRain
		) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.expiresTick = expiresTick;
			this.nextDamageTick = nextDamageTick;
			this.damagePerTick = damagePerTick;
			this.fireResistantTargetDamageMultiplier = fireResistantTargetDamageMultiplier;
			this.fireDamageIgnoresFireResistance = fireDamageIgnoresFireResistance;
			this.damageIntervalTicks = damageIntervalTicks;
			this.extinguishInWater = extinguishInWater;
			this.extinguishInRain = extinguishInRain;
		}
	}

	enum BurningPassionIgnitionEndReason {
		MANUAL,
		NATURAL,
		OVERHEAT,
		INVALID,
		LOCKED,
		CLEAR_ALL,
		CASTER_DIED
	}

	static final class BurningPassionIgnitionState {
		final RegistryKey<World> dimension;
		int currentStage;
		int stageStartTick;
		double heatPercent;
		boolean manaRecoveryUnlocked;
		final Map<UUID, Boolean> auraPlayersInside = new HashMap<>();
		final Map<UUID, Integer> boundaryCooldownEndTickByTarget = new HashMap<>();

		BurningPassionIgnitionState(RegistryKey<World> dimension, int currentStage, int stageStartTick, double heatPercent) {
			this.dimension = dimension;
			this.currentStage = currentStage;
			this.stageStartTick = stageStartTick;
			this.heatPercent = Math.max(0.0, heatPercent);
			this.manaRecoveryUnlocked = true;
		}
	}

	static final class BurningPassionAuraFireState {
		final RegistryKey<World> dimension;
		UUID casterId;
		int expiresTick;
		int nextDamageTick;
		float damagePerTick;
		double fireResistantTargetDamageMultiplier;
		boolean fireDamageIgnoresFireResistance;
		int damageIntervalTicks;
		int refreshTicks;
		boolean persistent;
		boolean extinguishInWater;
		boolean extinguishInRain;

		BurningPassionAuraFireState(
			RegistryKey<World> dimension,
			UUID casterId,
			int expiresTick,
			int nextDamageTick,
			float damagePerTick,
			double fireResistantTargetDamageMultiplier,
			boolean fireDamageIgnoresFireResistance,
			int damageIntervalTicks,
			int refreshTicks,
			boolean persistent,
			boolean extinguishInWater,
			boolean extinguishInRain
		) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.expiresTick = expiresTick;
			this.nextDamageTick = nextDamageTick;
			this.damagePerTick = damagePerTick;
			this.fireResistantTargetDamageMultiplier = fireResistantTargetDamageMultiplier;
			this.fireDamageIgnoresFireResistance = fireDamageIgnoresFireResistance;
			this.damageIntervalTicks = damageIntervalTicks;
			this.refreshTicks = refreshTicks;
			this.persistent = persistent;
			this.extinguishInWater = extinguishInWater;
			this.extinguishInRain = extinguishInRain;
		}
	}

	static final class BurningPassionSearingDashState {
		final RegistryKey<World> dimension;
		final Vec3d direction;
		final int endTick;
		final BurningPassionTrailLineState trailState;
		Vec3d lastTrailPoint;

		BurningPassionSearingDashState(
			RegistryKey<World> dimension,
			Vec3d direction,
			int endTick,
			BurningPassionTrailLineState trailState,
			Vec3d lastTrailPoint
		) {
			this.dimension = dimension;
			this.direction = direction;
			this.endTick = endTick;
			this.trailState = trailState;
			this.lastTrailPoint = lastTrailPoint;
		}
	}

	static final class BurningPassionTrailLineState {
		final UUID casterId;
		final RegistryKey<World> dimension;
		final int expiresTick;
		final List<Vec3d> points = new ArrayList<>();
		final Set<UUID> damagedTargetIds = new HashSet<>();

		BurningPassionTrailLineState(UUID casterId, RegistryKey<World> dimension, int expiresTick) {
			this.casterId = casterId;
			this.dimension = dimension;
			this.expiresTick = expiresTick;
		}
	}

	static final class BurningPassionCinderMarkArmedState {
		final RegistryKey<World> dimension;
		final int expiresTick;

		BurningPassionCinderMarkArmedState(RegistryKey<World> dimension, int expiresTick) {
			this.dimension = dimension;
			this.expiresTick = expiresTick;
		}
	}

	static final class BurningPassionCinderMarkState {
		final UUID casterId;
		final UUID targetId;
		final RegistryKey<World> dimension;
		final int expiresTick;
		final Vec3d anchorOffset;
		final UUID displayEntityId;

		BurningPassionCinderMarkState(
			UUID casterId,
			UUID targetId,
			RegistryKey<World> dimension,
			int expiresTick,
			Vec3d anchorOffset,
			UUID displayEntityId
		) {
			this.casterId = casterId;
			this.targetId = targetId;
			this.dimension = dimension;
			this.expiresTick = expiresTick;
			this.anchorOffset = anchorOffset;
			this.displayEntityId = displayEntityId;
		}
	}

	static final class BurningPassionEngineHeartState {
		final RegistryKey<World> dimension;
		Vec3d lastPosition;
		Vec3d currentMomentum;
		int lastAfterimageTick;
		int sustainedTicks;
		int currentTier;
		int slowdownTicks;
		int tierBeforeSlowdown;
		boolean specialAttackReady;

		BurningPassionEngineHeartState(RegistryKey<World> dimension, Vec3d lastPosition) {
			this.dimension = dimension;
			this.lastPosition = lastPosition;
			this.currentMomentum = Vec3d.ZERO;
			this.lastAfterimageTick = Integer.MIN_VALUE / 4;
			this.sustainedTicks = 0;
			this.currentTier = 0;
			this.slowdownTicks = 0;
			this.tierBeforeSlowdown = 0;
			this.specialAttackReady = false;
		}
	}

	static final class BurningPassionHudNotificationState {
		final String text;
		final int expiresTick;

		BurningPassionHudNotificationState(String text, int expiresTick) {
			this.text = text;
			this.expiresTick = expiresTick;
		}
	}

	static final class BurningPassionPendingMeleeImpactState {
		final RegistryKey<World> dimension;
		final UUID targetId;
		final Vec3d impactPos;
		final int expiresTick;

		BurningPassionPendingMeleeImpactState(
			RegistryKey<World> dimension,
			UUID targetId,
			Vec3d impactPos,
			int expiresTick
		) {
			this.dimension = dimension;
			this.targetId = targetId;
			this.impactPos = impactPos;
			this.expiresTick = expiresTick;
		}
	}

	static final class OverrideMeteorState {
		final RegistryKey<World> dimension;
		final UUID casterId;
		final Vec3d impactCenter;
		final double spawnY;
		final double impactRadius;
		final int startTick;
		final int impactTick;
		int nextWarningSoundTick;
		final UUID displayEntityId;
		final List<UUID> targetDomainOwnerIds;

		OverrideMeteorState(
			RegistryKey<World> dimension,
			UUID casterId,
			Vec3d impactCenter,
			double spawnY,
			double impactRadius,
			int startTick,
			int impactTick,
			int nextWarningSoundTick,
			UUID displayEntityId,
			List<UUID> targetDomainOwnerIds
		) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.impactCenter = impactCenter;
			this.spawnY = spawnY;
			this.impactRadius = impactRadius;
			this.startTick = startTick;
			this.impactTick = impactTick;
			this.nextWarningSoundTick = nextWarningSoundTick;
			this.displayEntityId = displayEntityId;
			this.targetDomainOwnerIds = List.copyOf(targetDomainOwnerIds);
		}
	}

	enum FrostMaximumPhase {
		WINDUP,
		ENCASE
	}

	enum FrostStageEndReason {
		MANUAL,
		FORCED_THRESHOLD,
		OVERCAST,
		MAXIMUM,
		INVALID,
		LOCKED,
		CLEAR_ALL,
		CASTER_DIED
	}

	enum FrostKillProgressType {
		RANGED,
		SLAM
	}

	static final class FrostStageState {
		RegistryKey<World> dimension;
		int currentStage;
		int highestUnlockedStage;
		int progressTicks;
		int stageThreeHoldTicks;

		FrostStageState(RegistryKey<World> dimension, int currentStage, int highestUnlockedStage, int progressTicks, int stageThreeHoldTicks) {
			this.dimension = dimension;
			this.currentStage = currentStage;
			this.highestUnlockedStage = highestUnlockedStage;
			this.progressTicks = progressTicks;
			this.stageThreeHoldTicks = stageThreeHoldTicks;
		}
	}

	static final class FrostMaximumState {
		final RegistryKey<World> dimension;
		final double baseX;
		final double baseY;
		final double baseZ;
		FrostMaximumPhase phase;
		final int windupEndTick;
		int encaseEndTick;
		final Map<UUID, FrostLockedTargetSeed> pendingHelplessTargets;

		FrostMaximumState(RegistryKey<World> dimension, double baseX, double baseY, double baseZ, int windupEndTick) {
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

	static final class FrostSpikeWaveState {
		final UUID casterId;
		final RegistryKey<World> dimension;
		final Vec3d origin;
		final Vec3d direction;
		final double range;
		final double speedPerTick;
		final double width;
		final float damage;
		final int stage;
		final boolean overcast;
		double travelled;
		double nextEruptionDistance;
		double terrainSearchY;
		int consecutiveGroundMisses;
		final Set<UUID> hitTargets;

		FrostSpikeWaveState(
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

	static final class FrostFreezeState {
		final RegistryKey<World> dimension;
		final UUID casterId;
		final double lockedX;
		final double lockedY;
		final double lockedZ;
		final int expiresTick;

		FrostFreezeState(RegistryKey<World> dimension, UUID casterId, double lockedX, double lockedY, double lockedZ, int expiresTick) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.lockedX = lockedX;
			this.lockedY = lockedY;
			this.lockedZ = lockedZ;
			this.expiresTick = expiresTick;
		}
	}

	static final class FrostFearState {
		final RegistryKey<World> dimension;
		final UUID casterId;
		final double lockedX;
		final double lockedY;
		final double lockedZ;
		final int expiresTick;

		FrostFearState(RegistryKey<World> dimension, UUID casterId, double lockedX, double lockedY, double lockedZ, int expiresTick) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.lockedX = lockedX;
			this.lockedY = lockedY;
			this.lockedZ = lockedZ;
			this.expiresTick = expiresTick;
		}
	}

	static final class FrostHelplessState {
		final RegistryKey<World> dimension;
		final UUID casterId;
		final double lockedX;
		final double lockedY;
		final double lockedZ;
		final int expiresTick;

		FrostHelplessState(RegistryKey<World> dimension, UUID casterId, double lockedX, double lockedY, double lockedZ, int expiresTick) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.lockedX = lockedX;
			this.lockedY = lockedY;
			this.lockedZ = lockedZ;
			this.expiresTick = expiresTick;
		}
	}

	static final class FrostPackedIceState {
		final RegistryKey<World> dimension;
		final UUID casterId;
		final double lockedX;
		final double lockedY;
		final double lockedZ;
		final int expiresTick;
		int nextDamageTick;
		final double shellRadius;
		final double shellHeight;
		final List<UUID> encasementDisplayIds;

		FrostPackedIceState(
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

	static final class FrostLockedTargetSeed {
		final RegistryKey<World> dimension;
		final double x;
		final double y;
		final double z;

		FrostLockedTargetSeed(RegistryKey<World> dimension, double x, double y, double z) {
			this.dimension = dimension;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	static final class FrostbiteState {
		final RegistryKey<World> dimension;
		UUID casterId;
		int expiresTick;
		int nextDamageTick;
		float damagePerTick;

		FrostbiteState(RegistryKey<World> dimension, UUID casterId, int expiresTick, int nextDamageTick, float damagePerTick) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.expiresTick = expiresTick;
			this.nextDamageTick = nextDamageTick;
			this.damagePerTick = damagePerTick;
		}
	}

	static final class FrostDomainPulseStatusState {
		final RegistryKey<World> dimension;
		final UUID casterId;
		int slownessAmplifier;
		int slownessExpiresTick;
		int blindnessAmplifier;
		int blindnessExpiresTick;

		FrostDomainPulseStatusState(RegistryKey<World> dimension, UUID casterId) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.slownessAmplifier = -1;
			this.slownessExpiresTick = Integer.MIN_VALUE;
			this.blindnessAmplifier = -1;
			this.blindnessExpiresTick = Integer.MIN_VALUE;
		}
	}

	static final class PlanckHeatState {
		final int frostPhaseEndTick;
		final int firePhaseEndTick;

		PlanckHeatState(int frostPhaseEndTick, int firePhaseEndTick) {
			this.frostPhaseEndTick = frostPhaseEndTick;
			this.firePhaseEndTick = firePhaseEndTick;
		}
	}

	static final class EnhancedFireState {
		final RegistryKey<World> dimension;
		UUID casterId;
		int expiresTick;
		int nextDamageTick;

		EnhancedFireState(RegistryKey<World> dimension, UUID casterId, int expiresTick, int nextDamageTick) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.expiresTick = expiresTick;
			this.nextDamageTick = nextDamageTick;
		}
	}
}


