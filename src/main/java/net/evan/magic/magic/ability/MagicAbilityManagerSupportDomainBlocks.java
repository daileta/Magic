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


abstract class MagicAbilityManagerSupportDomainBlocks extends MagicAbilityManagerState {
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

	public static boolean isEntityCapturedByCelestialGamaRayBeam(Entity entity) {
		if (
			entity == null
			|| !CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.blockTeleportForHitTargets
			|| CELESTIAL_GAMA_RAY_STATES.isEmpty()
		) {
			return false;
		}

		RegistryKey<World> dimension = entity.getEntityWorld().getRegistryKey();
		UUID entityId = entity.getUuid();
		for (CelestialGamaRayState state : CELESTIAL_GAMA_RAY_STATES.values()) {
			if (
				state.phase != CelestialGamaRayPhase.FIRING
				|| !state.dimension.equals(dimension)
				|| entityId.equals(state.casterId)
				|| state.beamDirection.lengthSquared() <= 1.0E-6
			) {
				continue;
			}
			if (isInsideCelestialGamaRayBeam(entity, state)) {
				return true;
			}
		}

		return false;
	}

	static boolean isInsideCelestialGamaRayBeam(Entity entity, CelestialGamaRayState state) {
		Vec3d samplePoint = new Vec3d(entity.getX(), entity.getBodyY(0.5), entity.getZ());
		Vec3d relative = samplePoint.subtract(state.beamOrigin);
		double projection = relative.dotProduct(state.beamDirection);
		if (projection < 0.0 || projection > CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.range) {
			return false;
		}
		Vec3d nearest = state.beamOrigin.add(state.beamDirection.multiply(projection));
		double radius = CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.radius;
		return samplePoint.squaredDistanceTo(nearest) <= radius * radius;
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

	static boolean isDomainBlockProtected(RegistryKey<World> dimension, BlockPos pos) {
		for (Map.Entry<UUID, DomainExpansionState> entry : DOMAIN_EXPANSIONS.entrySet()) {
			DomainExpansionState state = entry.getValue();
			if (state.dimension.equals(dimension) && state.protectedShellStates.containsKey(pos)) {
				return true;
			}
		}

		return false;
	}

	static boolean hasProtectedDecorationEntity(ServerWorld world, BlockPos pos) {
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

	static boolean isProtectedDomainDecoration(Entity entity) {
		if (!(entity instanceof BlockAttachedEntity decoration)) {
			return false;
		}

		RegistryKey<World> dimension = entity.getEntityWorld().getRegistryKey();
		return isDomainBlockProtected(dimension, decoration.getAttachedBlockPos())
			|| isPositionInsideAnyDomain(dimension, entity.getX(), entity.getY(), entity.getZ());
	}

	static void sendDomainProtectedMessage(PlayerEntity player) {
		if (player instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.sendMessage(Text.translatable("message.magic.domain.unbreakable"), true);
		}
	}

	static boolean setDomainBlockState(ServerWorld world, BlockPos pos, BlockState targetState, int flags) {
		BlockState previousState = world.getBlockState(pos);
		boolean changed = world.setBlockState(pos, targetState, flags);
		if (changed && (previousState.isOf(Blocks.LIGHT) || targetState.isOf(Blocks.LIGHT))) {
			world.getLightingProvider().checkBlock(pos);
			world.getChunkManager().markForUpdate(pos);
		}
		return changed;
	}

	static boolean isInsideDomainDome(int horizontalDistanceSq, int y, int radius, int height) {
		if (radius <= 0 || height <= 0 || y < 0) {
			return false;
		}

		double horizontalTerm = horizontalDistanceSq / (double) (radius * radius);
		double verticalTerm = (double) (y * y) / (double) (height * height);
		return horizontalTerm + verticalTerm <= 1.0;
	}

	static BlockState resolveDomainTargetState(
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
			return resolveFrostDomainState(shell, pos, centerX, centerZ, baseY, radius, height, innerRadius, innerHeight);
		}

		if (ability == MagicAbility.ASTRAL_CATACLYSM) {
			return resolveConstellationDomainState(shell, pos, centerX, centerZ, baseY, height, innerRadius, innerHeight);
		}

		if (ability == MagicAbility.GREED_DOMAIN_EXPANSION) {
			return GreedDomainRuntime.resolveDomainState(shell, centerX, centerZ, baseY, radius, height, innerRadius, innerHeight, pos);
		}

		if (ability != MagicAbility.LOVE_DOMAIN_EXPANSION) {
			BlockState sharedLightState = resolveSharedDomainInteriorLightState(pos, baseY, centerX, centerZ, innerRadius, innerHeight);
			return shell ? DOMAIN_SHELL_BLOCK_STATE : sharedLightState != null ? sharedLightState : DOMAIN_INTERIOR_BLOCK_STATE;
		}

		boolean roseSide = pos.getX() >= centerX;
		int relativeY = pos.getY() - baseY;
		if (shell) {
			return resolveLoveDomainShellState(pos, roseSide, relativeY, height);
		}

		return resolveLoveDomainInteriorState(pos, roseSide, relativeY, baseY, centerX, centerZ, innerRadius, innerHeight);
	}

	static BlockState resolveFrostDomainState(
		boolean shell,
		BlockPos pos,
		int centerX,
		int centerZ,
		int baseY,
		int radius,
		int height,
		int innerRadius,
		int innerHeight
	) {
		int relativeY = pos.getY() - baseY;
		if (relativeY == 0) {
			return resolveFrostDomainFloorState(pos, centerX, centerZ, radius);
		}
		if (!shell) {
			return resolveFrostDomainInteriorState(pos, relativeY, baseY, centerX, centerZ, innerRadius, innerHeight);
		}
		return resolveFrostDomainShellState(pos, centerX, centerZ, relativeY, height);
	}

	static BlockState resolveFrostDomainShellState(
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
			return Blocks.PACKED_ICE.getDefaultState();
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
		return Blocks.PACKED_ICE.getDefaultState();
	}

	static BlockState resolveFrostDomainFloorState(BlockPos pos, int centerX, int centerZ, int radius) {
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
		return Blocks.PACKED_ICE.getDefaultState();
	}

	static BlockState resolveFrostDomainInteriorState(
		BlockPos pos,
		int relativeY,
		int baseY,
		int centerX,
		int centerZ,
		int innerRadius,
		int innerHeight
	) {
		if (shouldPlaceFrostDomainLight(pos, relativeY, centerX, centerZ, innerRadius, innerHeight)) {
			return DOMAIN_INTERIOR_LIGHT_STATE;
		}
		BlockState sharedLightState = resolveSharedDomainInteriorLightState(pos, baseY, centerX, centerZ, innerRadius, innerHeight);
		if (sharedLightState != null) {
			return sharedLightState;
		}
		return DOMAIN_INTERIOR_BLOCK_STATE;
	}

	static boolean isFrostDomainSnowflakeFloor(int dx, int dz, int radius) {
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

	static boolean shouldPlaceFrostDomainLight(
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

	static BlockState resolveConstellationDomainState(
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
			return DOMAIN_INTERIOR_LIGHT_STATE;
		}
		BlockState sharedLightState = resolveSharedDomainInteriorLightState(pos, baseY, centerX, centerZ, innerRadius, innerHeight);
		if (sharedLightState != null) {
			return sharedLightState;
		}
		return DOMAIN_INTERIOR_BLOCK_STATE;
	}

	static boolean shouldPlaceConstellationDomainLight(
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

	static BlockState resolveLoveDomainShellState(BlockPos pos, boolean roseSide, int relativeY, int height) {
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

	static BlockState resolveLoveDomainInteriorState(
		BlockPos pos,
		boolean roseSide,
		int relativeY,
		int baseY,
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
			return DOMAIN_INTERIOR_LIGHT_STATE;
		}
		BlockState sharedLightState = resolveSharedDomainInteriorLightState(pos, baseY, centerX, centerZ, innerRadius, innerHeight);
		if (sharedLightState != null) {
			return sharedLightState;
		}

		if (roseSide && relativeY > 1) {
			BlockState vineState = resolveLoveDomainVineState(pos, relativeY, centerX, centerZ, innerRadius, innerHeight);
			if (vineState != null) {
				return vineState;
			}
		}

		return DOMAIN_INTERIOR_BLOCK_STATE;
	}

	static boolean shouldPlaceLoveDomainLight(
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

	static BlockState resolveSharedDomainInteriorLightState(
		BlockPos pos,
		int baseY,
		int centerX,
		int centerZ,
		int innerRadius,
		int innerHeight
	) {
		if (!shouldPlaceSharedDomainInteriorLight(pos, pos.getY() - baseY, centerX, centerZ, innerRadius, innerHeight)) {
			return null;
		}
		return DOMAIN_INTERIOR_LIGHT_STATE;
	}

	static boolean shouldPlaceSharedDomainInteriorLight(
		BlockPos pos,
		int relativeY,
		int centerX,
		int centerZ,
		int innerRadius,
		int innerHeight
	) {
		if (!DOMAIN_CONTROL_INTERIOR_LIGHTING_ENABLED || relativeY < DOMAIN_CONTROL_INTERIOR_LIGHT_START_Y_OFFSET || relativeY >= innerHeight) {
			return false;
		}
		if (
			relativeY != DOMAIN_CONTROL_INTERIOR_LIGHT_START_Y_OFFSET
				&& (relativeY - DOMAIN_CONTROL_INTERIOR_LIGHT_START_Y_OFFSET) % DOMAIN_CONTROL_INTERIOR_LIGHT_VERTICAL_SPACING != 0
		) {
			return false;
		}
		int dx = Math.abs(pos.getX() - centerX);
		int dz = Math.abs(pos.getZ() - centerZ);
		if (dx + dz <= 1) {
			return true;
		}
		if (dx % DOMAIN_CONTROL_INTERIOR_LIGHT_HORIZONTAL_SPACING != 0 || dz % DOMAIN_CONTROL_INTERIOR_LIGHT_HORIZONTAL_SPACING != 0) {
			return false;
		}
		int horizontalDistanceSq = horizontalDistanceSq(pos.getX(), centerX, pos.getZ(), centerZ);
		return isInsideDomainDome(horizontalDistanceSq, relativeY, innerRadius, innerHeight);
	}

	static BlockState resolveLoveDomainVineState(
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

	static int horizontalDistanceSq(int x, int centerX, int z, int centerZ) {
		int dx = x - centerX;
		int dz = z - centerZ;
		return dx * dx + dz * dz;
	}

	static boolean shouldDecorate(BlockPos pos, int modulo) {
		return Math.floorMod(decorHash(pos), modulo) == 0;
	}

	static int decorHash(BlockPos pos) {
		return pos.getX() * 73428767 ^ pos.getY() * 912367 ^ pos.getZ() * 42317861;
	}

	static String debugName(ServerPlayerEntity player) {
		return player.getName().getString() + "(" + player.getUuidAsString() + ")";
	}

	static double round3(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}
}

