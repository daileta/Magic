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


abstract class MagicAbilityManagerLoveDomainTypes extends MagicAbilityManagerJesterTypes {
	static final class LoveLockState {
		final RegistryKey<World> dimension;
		final UUID casterId;
		final double lockedX;
		final double lockedY;
		final double lockedZ;
		int lastSeenTick;
		int lastParticleTick;

		LoveLockState(
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

	enum TillDeathDoUsPartEndReason {
		MANUAL_CANCEL,
		LINK_EXPIRED,
		LINK_TARGET_INVALID,
		LINK_TARGET_DIED,
		CASTER_DIED,
		MANA_DEPLETED,
		SCHOOL_CHANGED
	}

	static final class TillDeathDoUsPartState {
		final int linkEndTick;
		final UUID linkedPlayerId;
		float sharedHealth;

		TillDeathDoUsPartState(int linkEndTick, UUID linkedPlayerId, float sharedHealth) {
			this.linkEndTick = linkEndTick;
			this.linkedPlayerId = linkedPlayerId;
			this.sharedHealth = sharedHealth;
		}
	}

	static final class DomainExpansionState {
		final RegistryKey<World> dimension;
		MagicAbility ability;
		int expiresTick;
		int effectEndTick;
		final double centerX;
		final double centerZ;
		final int baseY;
		final int radius;
		final int height;
		final int innerRadius;
		final int innerHeight;
		int activationTick;
		boolean clashOccurred;
		double cooldownMultiplier;
		int frostPulseCount;
		int nextFrostPulseTick;
		final Map<BlockPos, DomainSavedBlockState> savedBlocks;
		final Map<BlockPos, BlockState> protectedShellStates;
		final Map<UUID, DomainCapturedEntityState> capturedEntities;

		DomainExpansionState(
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

	static final class DomainClashState {
		final UUID ownerId;
		final MagicAbility ownerAbility;
		final UUID challengerId;
		final MagicAbility challengerAbility;
		final Vec3d ownerLockedPos;
		final Vec3d challengerLockedPos;
		final int startTick;
		final int titleEndTick;
		final int instructionsFadeStartTick;
		final int combatStartTick;
		double ownerDamageDealt;
		double challengerDamageDealt;

		DomainClashState(
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

	static final class DomainOwnerState {
		final UUID ownerId;
		final DomainExpansionState state;

		DomainOwnerState(UUID ownerId, DomainExpansionState state) {
			this.ownerId = ownerId;
			this.state = state;
		}
	}

	static final class DomainClashPendingDamageState {
		final UUID attackerId;
		final float healthBefore;
		final float incomingAmount;

		DomainClashPendingDamageState(UUID attackerId, float healthBefore, float incomingAmount) {
			this.attackerId = attackerId;
			this.healthBefore = healthBefore;
			this.incomingAmount = incomingAmount;
		}
	}

	static final class DomainClashResolution {
		final UUID ownerId;
		final UUID winnerId;

		DomainClashResolution(UUID ownerId, UUID winnerId) {
			this.ownerId = ownerId;
			this.winnerId = winnerId;
		}
	}

	static final class DomainSavedBlockState {
		final BlockState blockState;
		final NbtCompound blockEntityNbt;

		DomainSavedBlockState(BlockState blockState, NbtCompound blockEntityNbt) {
			this.blockState = blockState;
			this.blockEntityNbt = blockEntityNbt;
		}
	}

	static final class DomainPendingReturnState {
		final RegistryKey<World> dimension;
		final double x;
		final double y;
		final double z;
		final float yaw;
		final float pitch;

		DomainPendingReturnState(RegistryKey<World> dimension, double x, double y, double z, float yaw, float pitch) {
			this.dimension = dimension;
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}

	static final class DomainCapturedEntityState {
		final UUID entityId;
		final boolean playerEntity;
		final Vec3d position;
		final float yaw;
		final float pitch;
		Vec3d lastSafePos;
		float lastSafeYaw;
		float lastSafePitch;

		DomainCapturedEntityState(UUID entityId, boolean playerEntity, Vec3d position, float yaw, float pitch) {
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
}

