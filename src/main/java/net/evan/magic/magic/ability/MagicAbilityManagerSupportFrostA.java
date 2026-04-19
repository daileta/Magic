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


abstract class MagicAbilityManagerSupportFrostA extends MagicAbilityManagerSupportBurningC {
	static void spawnFrostStageParticles(ServerPlayerEntity player, int stage) {
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

	static void spawnFrostSlamParticles(
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

	static void spawnFrostMaximumParticles(
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

	static void syncFrostStageHud(ServerPlayerEntity player) {
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

		int requiredTicks;
		int progressTicks;
		if (state.currentStage >= 3 && FROST_CONFIG.maximum.enabled) {
			requiredTicks = Math.max(0, FROST_CONFIG.progression.maximumUnlockTicks);
			progressTicks = requiredTicks > 0 ? Math.min(Math.max(0, state.stageThreeHoldTicks), requiredTicks) : 0;
		} else {
			requiredTicks = frostProgressRequirementTicks(state.currentStage);
			progressTicks = requiredTicks > 0 ? Math.min(state.progressTicks, requiredTicks) : 0;
		}
		MagicPlayerData.setFrostStageHud(
			player,
			true,
			state.currentStage,
			state.highestUnlockedStage,
			progressTicks,
			requiredTicks
		);
	}

	static FrostStageState ensureAdminFrostStageState(ServerPlayerEntity player) {
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
			state = new FrostStageState(player.getEntityWorld().getRegistryKey(), 1, 1, 0, 0);
			FROST_STAGE_STATES.put(playerId, state);
		}
		state.dimension = player.getEntityWorld().getRegistryKey();
		state.currentStage = MathHelper.clamp(state.currentStage, 1, 3);
		state.highestUnlockedStage = MathHelper.clamp(Math.max(state.currentStage, state.highestUnlockedStage), 1, 3);
		state.progressTicks = Math.max(0, state.progressTicks);
		state.stageThreeHoldTicks = Math.max(0, state.stageThreeHoldTicks);
		removeFrostStageCasterBuffs(player);
		setActiveAbility(player, MagicAbility.BELOW_FREEZING);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		return state;
	}

	static BurningPassionIgnitionState ensureAdminBurningPassionIgnitionState(ServerPlayerEntity player, int currentTick) {
		if (
			player == null
			|| MagicPlayerData.getSchool(player) != MagicSchool.BURNING_PASSION
			|| player.getEntityWorld().getServer() == null
		) {
			return null;
		}

		UUID playerId = player.getUuid();
		MinecraftServer server = player.getEntityWorld().getServer();
		BurningPassionIgnitionState state = BURNING_PASSION_IGNITION_STATES.get(playerId);
		if (state == null || state.dimension != player.getEntityWorld().getRegistryKey()) {
			double heatPercent = state == null
				? 0.0
				: MathHelper.clamp(state.heatPercent, 0.0, BURNING_PASSION_CONFIG.heat.overheatThresholdPercent);
			endIgnition(player, currentTick, BurningPassionIgnitionEndReason.CLEAR_ALL, false, false);
			state = new BurningPassionIgnitionState(player.getEntityWorld().getRegistryKey(), 1, currentTick, heatPercent);
			BURNING_PASSION_IGNITION_STATES.put(playerId, state);
		} else {
			clearBurningPassionAuraFireByCaster(playerId);
			clearBurningPassionSearingDashState(playerId);
			clearBurningPassionEngineHeartState(player);
			if (BURNING_PASSION_SELF_FIRE_TARGETS.remove(playerId) != null) {
				player.extinguish();
			}
			removeBurningPassionStageBuffs(player);
			state.currentStage = MathHelper.clamp(state.currentStage, 1, 3);
			state.stageStartTick = Math.min(state.stageStartTick, currentTick);
			state.heatPercent = MathHelper.clamp(state.heatPercent, 0.0, BURNING_PASSION_CONFIG.heat.overheatThresholdPercent);
			state.auraPlayersInside.clear();
			state.boundaryCooldownEndTickByTarget.clear();
		}

		clearOverrideMeteorState(playerId, server);
		IGNITION_COOLDOWN_END_TICK.remove(playerId);
		SEARING_DASH_COOLDOWN_END_TICK.remove(playerId);
		CINDER_MARK_COOLDOWN_END_TICK.remove(playerId);
		ENGINE_HEART_COOLDOWN_END_TICK.remove(playerId);
		OVERRIDE_COOLDOWN_END_TICK.remove(playerId);
		setActiveAbility(player, MagicAbility.IGNITION);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		return state;
	}

	static Vec3d findFrostGroundAnchor(ServerWorld world, Vec3d samplePoint, int searchUpBlocks, int searchDownBlocks) {
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

	static void spawnFrostGroundSpikeEruption(
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

	static void spawnFrostSlamEruptionAt(
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

	static void spawnFrostShardFan(ServerWorld world, Vec3d anchor, double angle, double speed, boolean erupting) {
		for (int index = -1; index <= 1; index++) {
			double spreadAngle = angle + index * 0.28;
			double velocityX = Math.cos(spreadAngle) * speed * 0.42;
			double velocityY = speed + (erupting ? 0.14 : 0.08) + Math.abs(index) * 0.02;
			double velocityZ = Math.sin(spreadAngle) * speed * 0.42;
			world.spawnParticles(FROST_SHARD_PARTICLE, anchor.x, anchor.y + 0.04, anchor.z, 0, velocityX, velocityY, velocityZ, 1.0);
			world.spawnParticles(ParticleTypes.SNOWFLAKE, anchor.x, anchor.y + 0.03, anchor.z, 0, velocityX * 0.7, velocityY * 0.8, velocityZ * 0.7, 1.0);
		}
	}

	static void spawnFrostGroundShockRing(ServerWorld world, Vec3d center, double radius, int points, double liftVelocity) {
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

	static void spawnFrostGroundHaze(ServerWorld world, Vec3d center, double radius, int density) {
		if (density <= 0) {
			return;
		}
		world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + 0.05, center.z, density, radius * 0.35, 0.06, radius * 0.35, 0.01);
		world.spawnParticles(ParticleTypes.WHITE_ASH, center.x, center.y + 0.04, center.z, density, radius * 0.38, 0.08, radius * 0.38, 0.008);
	}

	static void spawnFrostMaximumBurstParticles(ServerWorld world, LivingEntity caster, double burstRadius) {
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

	static void spawnFrostPackedIceEncasement(ServerWorld world, LivingEntity target, FrostPackedIceState state) {
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

	static void clearFrostPackedIceEncasement(MinecraftServer server, FrostPackedIceState state, boolean spawnBreakEffects) {
		if (server == null || state == null) {
			return;
		}
		ServerWorld world = server.getWorld(state.dimension);
		if (world != null) {
			clearFrostPackedIceEncasement(world, state, spawnBreakEffects);
		}
	}

	static void clearFrostPackedIceEncasement(ServerWorld world, FrostPackedIceState state, boolean spawnBreakEffects) {
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

	static boolean isFrostEnemy(ServerPlayerEntity caster, LivingEntity target) {
		return target != caster && !caster.isTeammate(target);
	}

	static void removeStatusEffectIfMatching(
		LivingEntity entity,
		RegistryEntry<StatusEffect> effect,
		Set<Integer> allowedAmplifiers
	) {
		StatusEffectInstance existing = entity.getStatusEffect(effect);
		if (existing != null && allowedAmplifiers.contains(existing.getAmplifier())) {
			entity.removeStatusEffect(effect);
		}
	}

	static Set<Integer> frostPassiveAmplifiers(boolean slowness) {
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

	static boolean sendFrostAbilityBlockedMessage(ServerPlayerEntity player) {
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

	static boolean isFrostMaximumInvulnerable(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return false;
		}
		FrostMaximumState state = FROST_MAXIMUM_STATES.get(player.getUuid());
		if (state == null || state.phase != FrostMaximumPhase.WINDUP || state.dimension != player.getEntityWorld().getRegistryKey()) {
			return false;
		}
		return !source.isOf(DamageTypes.OUT_OF_WORLD) && !source.isOf(DamageTypes.GENERIC_KILL) && !source.isOf(DamageTypes.OUTSIDE_BORDER);
	}

	static boolean isFrostHelplessAttacker(DamageSource source) {
		Entity attacker = source.getAttacker();
		if (attacker instanceof LivingEntity attackerLiving && isFrostHelpless(attackerLiving)) {
			return true;
		}
		return source.getSource() instanceof LivingEntity sourceLiving && isFrostHelpless(sourceLiving);
	}

	static boolean isFrostTeleportBlocked(Entity entity) {
		if (!(entity instanceof LivingEntity living)) {
			return false;
		}
		return isFrostFrozen(living) || isFrostMaximumFearLocked(living) || isFrostPackedIceTarget(living) || isFrostHelpless(living);
	}

	static boolean isFrostTeleportItemBlocked(PlayerEntity player, ItemStack stack) {
		if (player == null || stack == null || !isFrostTeleportBlocked(player)) {
			return false;
		}
		return stack.isOf(Items.ENDER_PEARL) || stack.isOf(Items.CHORUS_FRUIT) || stack.isOf(Items.WIND_CHARGE);
	}

	static boolean isFrostFrozen(LivingEntity entity) {
		FrostFreezeState state = FROST_FROZEN_TARGETS.get(entity.getUuid());
		if (state == null || state.dimension != entity.getEntityWorld().getRegistryKey()) {
			state = FROST_DOMAIN_FROZEN_TARGETS.get(entity.getUuid());
		}
		return state != null && state.dimension == entity.getEntityWorld().getRegistryKey();
	}

	static boolean isFrostMaximumFearLocked(Entity entity) {
		FrostFearState state = FROST_MAXIMUM_FEAR_TARGETS.get(entity.getUuid());
		return state != null && state.dimension == entity.getEntityWorld().getRegistryKey();
	}

	static boolean isFrostPackedIceTarget(LivingEntity entity) {
		FrostPackedIceState state = FROST_PACKED_ICE_TARGETS.get(entity.getUuid());
		return state != null && state.dimension == entity.getEntityWorld().getRegistryKey();
	}

	static boolean isFrostHelpless(Entity entity) {
		FrostHelplessState state = FROST_HELPLESS_TARGETS.get(entity.getUuid());
		return state != null && state.dimension == entity.getEntityWorld().getRegistryKey();
	}

	static boolean frostManaRegenBlocked(ServerPlayerEntity player, int currentTick) {
		int endTick = FROST_MANA_REGEN_BLOCKED_END_TICK.getOrDefault(player.getUuid(), 0);
		if (currentTick < endTick) {
			return true;
		}
		FROST_MANA_REGEN_BLOCKED_END_TICK.remove(player.getUuid());
		return false;
	}
}

