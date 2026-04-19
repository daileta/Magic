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


abstract class PersistentRuntimeTypes extends CelestialRuntimeTypes {
	static final class DomainRuntimePersistentState extends PersistentState {
		static final PersistentStateType<DomainRuntimePersistentState> TYPE = new PersistentStateType<>(
			"magic_domain_runtime",
			DomainRuntimePersistentState::new,
			NbtCompound.CODEC.xmap(DomainRuntimePersistentState::fromNbt, DomainRuntimePersistentState::toNbt),
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
		);

		NbtCompound data;

		DomainRuntimePersistentState() {
			this(new NbtCompound());
		}

		DomainRuntimePersistentState(NbtCompound data) {
			this.data = data;
		}

		static DomainRuntimePersistentState fromNbt(NbtCompound nbt) {
			return new DomainRuntimePersistentState(nbt.copy());
		}

		static NbtCompound toNbt(DomainRuntimePersistentState state) {
			return state.data.copy();
		}

		NbtCompound getDataCopy() {
			return data.copy();
		}

		void setData(NbtCompound data) {
			this.data = data.copy();
			markDirty();
		}
	}

	static final class BeaconCoreAnchorPersistentState extends PersistentState {
		static final Codec<BeaconCoreAnchorPersistentState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("has_anchor", false).forGetter(state -> state.anchor != null),
			Codec.STRING.optionalFieldOf("dimension", "").forGetter(state ->
				state.anchor == null ? "" : state.anchor.dimension.getValue().toString()
			),
			Codec.INT.optionalFieldOf("x", 0).forGetter(state -> state.anchor == null ? 0 : state.anchor.pos.getX()),
			Codec.INT.optionalFieldOf("y", 0).forGetter(state -> state.anchor == null ? 0 : state.anchor.pos.getY()),
			Codec.INT.optionalFieldOf("z", 0).forGetter(state -> state.anchor == null ? 0 : state.anchor.pos.getZ())
		).apply(instance, BeaconCoreAnchorPersistentState::fromCodec));

		BeaconCoreAnchorState anchor;

		BeaconCoreAnchorPersistentState() {
		}

		static BeaconCoreAnchorPersistentState fromCodec(boolean hasAnchor, String dimensionValue, int x, int y, int z) {
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

		BeaconCoreAnchorState getAnchor() {
			return anchor;
		}
	}

	static final class BeaconCoreAnchorState {
		final RegistryKey<World> dimension;
		final BlockPos pos;

		BeaconCoreAnchorState(RegistryKey<World> dimension, BlockPos pos) {
			this.dimension = dimension;
			this.pos = pos;
		}
	}

	static final class HerculesBurdenState {
		final int endTick;
		final Set<UUID> targetIds;

		HerculesBurdenState(int endTick, Set<UUID> targetIds) {
			this.endTick = endTick;
			this.targetIds = targetIds;
		}
	}

	static final class AstralBurdenTargetState {
		final RegistryKey<World> dimension;
		final UUID casterId;
		final double lockedX;
		final double lockedY;
		final double lockedZ;
		final int endTick;
		int lastParticleTick;

		AstralBurdenTargetState(
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
}


