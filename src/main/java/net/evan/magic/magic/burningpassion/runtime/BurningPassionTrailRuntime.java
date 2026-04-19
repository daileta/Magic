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


abstract class BurningPassionTrailRuntime extends AbilityAdminAndPassiveSupport {
	static boolean clearBurningPassionCinderMarksByCaster(UUID casterId, MinecraftServer server) {
		boolean removed = false;
		Iterator<BurningPassionCinderMarkState> iterator = BURNING_PASSION_CINDER_MARKS.iterator();
		while (iterator.hasNext()) {
			BurningPassionCinderMarkState state = iterator.next();
			if (!casterId.equals(state.casterId)) {
				continue;
			}
			ServerWorld world = server == null ? null : server.getWorld(state.dimension);
			if (world != null) {
				discardBurningPassionCinderMarkDisplay(world, state);
			}
			iterator.remove();
			removed = true;
		}
		return removed;
	}

	static boolean clearBurningPassionCinderMarksForPlayer(UUID playerId, MinecraftServer server) {
		boolean removed = false;
		Iterator<BurningPassionCinderMarkState> iterator = BURNING_PASSION_CINDER_MARKS.iterator();
		while (iterator.hasNext()) {
			BurningPassionCinderMarkState state = iterator.next();
			if (!playerId.equals(state.casterId) && !playerId.equals(state.targetId)) {
				continue;
			}
			ServerWorld world = server == null ? null : server.getWorld(state.dimension);
			if (world != null) {
				discardBurningPassionCinderMarkDisplay(world, state);
			}
			iterator.remove();
			removed = true;
		}
		return removed;
	}

	static void spawnBurningPassionFreeMarkBurst(ServerWorld world, LivingEntity target) {
		Vec3d burstPos = new Vec3d(target.getX(), target.getBodyY(0.45), target.getZ());
		world.spawnParticles(
			ParticleTypes.FLAME,
			burstPos.x,
			burstPos.y,
			burstPos.z,
			Math.max(0, BURNING_PASSION_CONFIG.cinderMark.freeMarkBurstParticleCount),
			0.16,
			0.16,
			0.16,
			0.025
		);
	}

	static void spawnSearingDashStartEffects(ServerWorld world, ServerPlayerEntity player) {
		Vec3d center = new Vec3d(player.getX(), player.getBodyY(0.4), player.getZ());
		world.spawnParticles(
			ParticleTypes.FLAME,
			center.x,
			center.y,
			center.z,
			Math.max(0, BURNING_PASSION_CONFIG.searingDash.dashFlameParticleCount),
			BURNING_PASSION_CONFIG.searingDash.particleSpread,
			0.18,
			BURNING_PASSION_CONFIG.searingDash.particleSpread,
			0.04
		);
		world.spawnParticles(
			ParticleTypes.CAMPFIRE_COSY_SMOKE,
			center.x,
			center.y,
			center.z,
			Math.max(0, BURNING_PASSION_CONFIG.searingDash.dashSmokeParticleCount),
			BURNING_PASSION_CONFIG.searingDash.particleSpread,
			0.12,
			BURNING_PASSION_CONFIG.searingDash.particleSpread,
			0.01
		);
		playConfiguredSound(world, center, SoundEvents.ENTITY_BLAZE_SHOOT, BURNING_PASSION_CONFIG.searingDash.dashSoundVolume, BURNING_PASSION_CONFIG.searingDash.dashSoundPitch);
	}

	static void spawnBurningPassionSearingDashParticles(ServerPlayerEntity player) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		Vec3d velocity = player.getVelocity();
		Vec3d reverse = velocity.lengthSquared() > 1.0E-5
			? velocity.normalize().multiply(-1.0)
			: normalizedHorizontalDirection(player.getRotationVector(), player).multiply(-1.0);
		Vec3d center = new Vec3d(player.getX(), player.getBodyY(0.3), player.getZ()).add(reverse.multiply(0.25));
		world.spawnParticles(
			ParticleTypes.FLAME,
			center.x,
			center.y,
			center.z,
			Math.max(0, Math.max(2, BURNING_PASSION_CONFIG.searingDash.dashFlameParticleCount / 3)),
			BURNING_PASSION_CONFIG.searingDash.particleSpread,
			0.08,
			BURNING_PASSION_CONFIG.searingDash.particleSpread,
			0.02
		);
	}

	static double squaredDistanceToSegment(Vec3d point, Vec3d start, Vec3d end) {
		Vec3d segment = end.subtract(start);
		double segmentLengthSq = segment.lengthSquared();
		if (segmentLengthSq <= 1.0E-6) {
			return point.squaredDistanceTo(start);
		}
		double projection = MathHelper.clamp(point.subtract(start).dotProduct(segment) / segmentLengthSq, 0.0, 1.0);
		Vec3d projected = start.add(segment.multiply(projection));
		return point.squaredDistanceTo(projected);
	}

	static double horizontalDistance(Vec3d first, Vec3d second) {
		double dx = first.x - second.x;
		double dz = first.z - second.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	static void spawnBurningPassionEngineHeartParticles(ServerPlayerEntity player, BurningPassionEngineHeartState state, int currentTick) {
		if (!BURNING_PASSION_CONFIG.engineHeart.particlesEnabled || !(player.getEntityWorld() instanceof ServerWorld world) || state.currentTier <= 0) {
			return;
		}

		Vec3d momentum = state.currentMomentum;
		Vec3d reverse = momentum.lengthSquared() > 1.0E-5
			? momentum.normalize().multiply(-1.0)
			: normalizedHorizontalDirection(player.getRotationVector(), player).multiply(-1.0);
		if (state.currentTier >= 3) {
			spawnBurningPassionEngineHeartSoundBarrierWake(world, player, reverse, currentTick);
		}
	}

	static void spawnBurningPassionEngineHeartSoundBarrierWake(
		ServerWorld world,
		ServerPlayerEntity player,
		Vec3d reverse,
		int currentTick
	) {
		if (!BURNING_PASSION_CONFIG.engineHeart.tierThreeSoundBarrierEnabled) {
			return;
		}

		Vec3d left = burningPassionEngineHeartLeftDirection(reverse, player);
		Vec3d center = new Vec3d(player.getX(), player.getBodyY(0.42), player.getZ()).add(reverse.multiply(BURNING_PASSION_CONFIG.engineHeart.tierThreeSoundBarrierBackOffsetBlocks));
		int ringParticleCount = BURNING_PASSION_CONFIG.engineHeart.tierThreeSoundBarrierRingParticleCount;
		double ringRadius = BURNING_PASSION_CONFIG.engineHeart.tierThreeSoundBarrierRingRadius;
		double verticalAmplitude = BURNING_PASSION_CONFIG.engineHeart.tierThreeSoundBarrierVerticalAmplitude;
		double animationOffset = currentTick * 0.18;
		for (int index = 0; index < ringParticleCount; index++) {
			double angle = animationOffset + (Math.PI * 2.0 * index / Math.max(1, ringParticleCount));
			Vec3d ringOffset = left.multiply(Math.cos(angle) * ringRadius)
				.add(reverse.multiply(Math.sin(angle) * ringRadius * 0.35))
				.add(0.0, Math.sin(angle * 2.0) * verticalAmplitude, 0.0);
			Vec3d point = center.add(ringOffset);
			world.spawnParticles(ParticleTypes.CLOUD, point.x, point.y, point.z, 1, 0.01, 0.01, 0.01, 0.0);
			if ((index & 1) == 0) {
				world.spawnParticles(ParticleTypes.WHITE_ASH, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}

		int wakeParticleCount = BURNING_PASSION_CONFIG.engineHeart.tierThreeSoundBarrierWakeParticleCount;
		double wakeLength = BURNING_PASSION_CONFIG.engineHeart.tierThreeSoundBarrierWakeLengthBlocks;
		for (int index = 0; index < wakeParticleCount; index++) {
			double progress = wakeParticleCount <= 1 ? 1.0 : (double) index / (wakeParticleCount - 1);
			Vec3d point = center.add(reverse.multiply(progress * wakeLength));
			world.spawnParticles(ParticleTypes.WHITE_ASH, point.x, point.y, point.z, 1, 0.03, 0.06, 0.03, 0.0);
			if ((index & 1) == 0) {
				world.spawnParticles(ParticleTypes.CLOUD, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
			}
		}
	}

	static void spawnBurningPassionEngineHeartTierThreeBreakthroughBurst(
		ServerPlayerEntity player,
		BurningPassionEngineHeartState state
	) {
		if (
			!BURNING_PASSION_CONFIG.engineHeart.tierThreeSoundBarrierEnabled
			|| !(player.getEntityWorld() instanceof ServerWorld world)
		) {
			return;
		}

		Vec3d momentum = state.currentMomentum.lengthSquared() > 1.0E-5
			? state.currentMomentum
			: normalizedHorizontalDirection(player.getRotationVector(), player).multiply(-1.0);
		Vec3d reverse = momentum.lengthSquared() > 1.0E-5
			? momentum.normalize().multiply(-1.0)
			: normalizedHorizontalDirection(player.getRotationVector(), player).multiply(-1.0);
		Vec3d left = burningPassionEngineHeartLeftDirection(reverse, player);
		Vec3d center = new Vec3d(player.getX(), player.getBodyY(0.42), player.getZ()).add(reverse.multiply(BURNING_PASSION_CONFIG.engineHeart.tierThreeSoundBarrierBackOffsetBlocks * 0.6));
		int burstParticleCount = BURNING_PASSION_CONFIG.engineHeart.tierThreeSoundBarrierUnlockBurstParticleCount;
		double burstRadius = BURNING_PASSION_CONFIG.engineHeart.tierThreeSoundBarrierRingRadius * 1.15;
		double verticalAmplitude = Math.max(0.08, BURNING_PASSION_CONFIG.engineHeart.tierThreeSoundBarrierVerticalAmplitude);
		for (int index = 0; index < burstParticleCount; index++) {
			double angle = Math.PI * 2.0 * index / Math.max(1, burstParticleCount);
			Vec3d ringOffset = left.multiply(Math.cos(angle) * burstRadius)
				.add(reverse.multiply(Math.sin(angle) * burstRadius * 0.45))
				.add(0.0, Math.sin(angle * 2.0) * verticalAmplitude, 0.0);
			Vec3d point = center.add(ringOffset);
			world.spawnParticles(ParticleTypes.CLOUD, point.x, point.y, point.z, 1, 0.015, 0.015, 0.015, 0.0);
			world.spawnParticles(ParticleTypes.WHITE_ASH, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	static Vec3d burningPassionEngineHeartLeftDirection(Vec3d reverse, ServerPlayerEntity player) {
		Vec3d horizontalReverse = new Vec3d(reverse.x, 0.0, reverse.z);
		if (horizontalReverse.lengthSquared() <= 1.0E-5) {
			Vec3d facing = normalizedHorizontalDirection(player.getRotationVector(), player);
			horizontalReverse = new Vec3d(-facing.x, 0.0, -facing.z);
		}
		Vec3d left = new Vec3d(-horizontalReverse.z, 0.0, horizontalReverse.x);
		return left.lengthSquared() <= 1.0E-5 ? new Vec3d(1.0, 0.0, 0.0) : left.normalize();
	}

	static void notifyBurningPassionEngineHeartTierUnlocked(ServerPlayerEntity player, int tier, int currentTick) {
		if (!BURNING_PASSION_CONFIG.engineHeart.notificationsEnabled) {
			return;
		}

		String key = switch (tier) {
			case 1 -> "overlay.magic.burning_passion.engine_heart.tier_one";
			case 2 -> "overlay.magic.burning_passion.engine_heart.tier_two";
			default -> "overlay.magic.burning_passion.engine_heart.tier_three";
		};
		showBurningPassionHudNotification(
			player,
			Text.translatable(key).getString(),
			currentTick,
			BURNING_PASSION_CONFIG.engineHeart.notificationDurationTicks
		);
		if (player.getEntityWorld() instanceof ServerWorld world) {
			playConfiguredSound(
				world,
				new Vec3d(player.getX(), player.getBodyY(0.45), player.getZ()),
				SoundEvents.ENTITY_BLAZE_AMBIENT,
				BURNING_PASSION_CONFIG.engineHeart.tierUnlockSoundVolume,
				BURNING_PASSION_CONFIG.engineHeart.tierUnlockSoundPitch
			);
		}
	}

	static int resolveBurningPassionEngineHeartTier(int sustainedTicks) {
		if (sustainedTicks >= BURNING_PASSION_CONFIG.engineHeart.tierThreeThresholdTicks) {
			return 3;
		}
		if (sustainedTicks >= BURNING_PASSION_CONFIG.engineHeart.tierTwoThresholdTicks) {
			return 2;
		}
		return sustainedTicks >= BURNING_PASSION_CONFIG.engineHeart.tierOneThresholdTicks ? 1 : 0;
	}

	static int resolveBurningPassionEngineHeartTierAfterSlowdown(int startingTier, int slowdownTicks) {
		if (slowdownTicks > BURNING_PASSION_CONFIG.engineHeart.fullResetTicks) {
			return 0;
		}
		if (slowdownTicks > BURNING_PASSION_CONFIG.engineHeart.decayTwoTiersTicks) {
			return Math.max(0, startingTier - 2);
		}
		if (slowdownTicks > BURNING_PASSION_CONFIG.engineHeart.decayOneTierTicks) {
			return Math.max(0, startingTier - 1);
		}
		return startingTier;
	}

	static int burningPassionEngineHeartThresholdTicksForTier(int tier) {
		return switch (tier) {
			case 1 -> BURNING_PASSION_CONFIG.engineHeart.tierOneThresholdTicks;
			case 2 -> BURNING_PASSION_CONFIG.engineHeart.tierTwoThresholdTicks;
			case 3 -> BURNING_PASSION_CONFIG.engineHeart.tierThreeThresholdTicks;
			default -> 0;
		};
	}

	static Vec3d burningPassionEngineHeartMomentum(ServerPlayerEntity player, BurningPassionEngineHeartState state) {
		Vec3d currentPosition = entityPosition(player);
		Vec3d displacement = state.lastPosition == null ? Vec3d.ZERO : currentPosition.subtract(state.lastPosition);
		state.lastPosition = currentPosition;
		Vec3d velocity = player.getVelocity();
		Vec3d velocityMomentum = new Vec3d(velocity.x, 0.0, velocity.z);
		Vec3d displacementMomentum = new Vec3d(displacement.x, 0.0, displacement.z);
		state.currentMomentum = displacementMomentum.lengthSquared() > velocityMomentum.lengthSquared() ? displacementMomentum : velocityMomentum;
		return state.currentMomentum;
	}

	static boolean isBurningPassionEngineHeartMomentumActive(ServerPlayerEntity player, BurningPassionEngineHeartState state) {
		Vec3d momentum = burningPassionEngineHeartMomentum(player, state);
		double currentSpeed = momentum.length();
		if (BURNING_PASSION_CONFIG.engineHeart.requiresSprinting && !isBurningPassionEngineHeartSprintActive(player)) {
			return false;
		}
		return currentSpeed + BURNING_PASSION_CONFIG.engineHeart.momentumToleranceBlocksPerTick
			>= BURNING_PASSION_CONFIG.engineHeart.minimumSpeedBlocksPerTick;
	}

	static boolean isBurningPassionEngineHeartSprintActive(ServerPlayerEntity player) {
		if (player.isSprinting()) {
			return true;
		}

		PlayerInput input = MANIPULATION_INPUT_BY_CASTER.get(player.getUuid());
		return input != null && input.sprint();
	}

	static void clearBurningPassionEngineHeartState(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}

		BURNING_PASSION_ENGINE_HEART_STATES.remove(player.getUuid());
		BURNING_PASSION_HUD_NOTIFICATION_STATES.remove(player.getUuid());
		MagicPlayerData.setBurningPassionHudNotification(player, "");
		MagicPlayerData.setBurningPassionEngineHeartAfterimageState(player, false, 0);
		removeAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), BURNING_PASSION_ENGINE_HEART_ATTACK_DAMAGE_MODIFIER_ID);
	}

	static BurningPassionSearingDashState clearBurningPassionSearingDashState(UUID playerId) {
		return BURNING_PASSION_SEARING_DASH_STATES.remove(playerId);
	}

	static boolean canApplyCinderMarkToTarget(LivingEntity target) {
		if (target == null || !target.isAlive()) {
			return false;
		}
		if (target instanceof ServerPlayerEntity playerTarget) {
			return !playerTarget.isSpectator() && BURNING_PASSION_CONFIG.cinderMark.affectPlayers;
		}
		return BURNING_PASSION_CONFIG.cinderMark.affectMobs && target instanceof MobEntity;
	}

	static Vec3d resolveBurningPassionMeleeImpactPosition(ServerPlayerEntity attacker, LivingEntity target) {
		if (attacker == null || target == null) {
			return Vec3d.ZERO;
		}

		MinecraftServer server = attacker.getEntityWorld().getServer();
		int currentTick = server == null ? 0 : server.getTicks();
		BurningPassionPendingMeleeImpactState pendingState = BURNING_PASSION_PENDING_MELEE_IMPACTS.remove(attacker.getUuid());
		if (
			pendingState != null
			&& currentTick <= pendingState.expiresTick
			&& pendingState.dimension == target.getEntityWorld().getRegistryKey()
			&& target.getUuid().equals(pendingState.targetId)
		) {
			return pendingState.impactPos;
		}

		Vec3d eyePos = attacker.getEyePos();
		Vec3d center = new Vec3d(target.getX(), target.getBodyY(0.5), target.getZ());
		Box hitBox = target.getBoundingBox().expand(0.2);
		Vec3d lookHit = hitBox.raycast(eyePos, eyePos.add(attacker.getRotationVec(1.0F).multiply(Math.max(3.0, attacker.distanceTo(target) + 1.5)))).orElse(null);
		if (lookHit != null) {
			return lookHit;
		}
		Vec3d centerHit = hitBox.raycast(eyePos, center).orElse(null);
		return centerHit != null ? centerHit : center;
	}

	static boolean isBurningPassionFireExtinguished(LivingEntity target, boolean extinguishInWater, boolean extinguishInRain) {
		BlockPos pos = BlockPos.ofFloored(target.getX(), target.getY(), target.getZ());
		if (extinguishInWater && (target.isTouchingWater() || target.isSubmergedInWater())) {
			return true;
		}
		return extinguishInRain && target.getEntityWorld().hasRain(pos);
	}

	static boolean isBurningPassionCoolingInWater(LivingEntity target) {
		return target.isTouchingWater() || target.isSubmergedInWater();
	}

	static void updateBurningPassionSearingDashStates(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, BurningPassionSearingDashState>> iterator = BURNING_PASSION_SEARING_DASH_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, BurningPassionSearingDashState> entry = iterator.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			BurningPassionSearingDashState state = entry.getValue();
			if (
				player == null
				|| !player.isAlive()
				|| player.getEntityWorld().getRegistryKey() != state.dimension
				|| currentTick > state.endTick
			) {
				iterator.remove();
				continue;
			}

			BurningPassionIgnitionState ignitionState = burningPassionIgnitionState(player);
			if (ignitionState == null || ignitionState.currentStage != 1) {
				iterator.remove();
				continue;
			}

			Vec3d dashStartPos = entityPosition(player);
			Vec3d dashEndPos = dashStartPos.add(state.direction.multiply(BURNING_PASSION_CONFIG.searingDash.dashSpeedBlocksPerTick));
			applyForcedVelocity(
				player,
				new Vec3d(
					state.direction.x * BURNING_PASSION_CONFIG.searingDash.dashSpeedBlocksPerTick,
					Math.max(0.05, player.getVelocity().y),
					state.direction.z * BURNING_PASSION_CONFIG.searingDash.dashSpeedBlocksPerTick
				)
			);
			appendBurningPassionTrailPoints(player, state);
			spawnBurningPassionSearingDashParticles(player);

			LivingEntity impactedTarget = findBurningPassionSearingDashCollisionTarget(player, dashStartPos, dashEndPos);
			if (impactedTarget != null) {
				triggerBurningPassionSearingDashImpact(player, impactedTarget);
				iterator.remove();
			}
		}
	}

	static void appendBurningPassionTrailPoints(ServerPlayerEntity player, BurningPassionSearingDashState state) {
		Vec3d currentPoint = new Vec3d(player.getX(), player.getBodyY(0.05), player.getZ());
		double spacing = Math.max(0.05, BURNING_PASSION_CONFIG.searingDash.trailPointSpacingBlocks);
		Vec3d delta = currentPoint.subtract(state.lastTrailPoint);
		double distance = delta.length();
		if (distance < spacing) {
			return;
		}

		Vec3d direction = delta.normalize();
		Vec3d lastPoint = state.lastTrailPoint;
		for (double travelled = spacing; travelled <= distance + 1.0E-6; travelled += spacing) {
			lastPoint = state.lastTrailPoint.add(direction.multiply(travelled));
			state.trailState.points.add(lastPoint);
		}
		state.lastTrailPoint = lastPoint;
	}

	static void updateBurningPassionTrailLines(MinecraftServer server, int currentTick) {
		Iterator<BurningPassionTrailLineState> iterator = BURNING_PASSION_TRAIL_LINES.iterator();
		while (iterator.hasNext()) {
			BurningPassionTrailLineState state = iterator.next();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null || currentTick > state.expiresTick) {
				iterator.remove();
				continue;
			}

			spawnBurningPassionTrailParticles(world, state);
			for (ServerPlayerEntity target : world.getPlayers()) {
				if (
					target.isSpectator()
					|| !target.isAlive()
					|| target.getUuid().equals(state.casterId)
					|| (BURNING_PASSION_CONFIG.searingDash.trailDamagesEachTargetOncePerLine && state.damagedTargetIds.contains(target.getUuid()))
				) {
					continue;
				}
				if (!isTargetTouchingBurningPassionTrailLine(target, state)) {
					continue;
				}

				dealTrackedMagicDamage(
					target,
					state.casterId,
					createTrueMagicDamageSource(server, world, state.casterId),
					BURNING_PASSION_CONFIG.searingDash.trailTrueDamage
				);
				state.damagedTargetIds.add(target.getUuid());
				world.spawnParticles(
					ParticleTypes.FLAME,
					target.getX(),
					target.getBodyY(0.2),
					target.getZ(),
					8,
					0.12,
					0.12,
					0.12,
					0.02
				);
			}
		}
	}

	static void spawnBurningPassionTrailParticles(ServerWorld world, BurningPassionTrailLineState state) {
		for (Vec3d point : state.points) {
			world.spawnParticles(
				ParticleTypes.FLAME,
				point.x,
				point.y,
				point.z,
				Math.max(0, BURNING_PASSION_CONFIG.searingDash.trailFlameParticleCount),
				0.08,
				0.04,
				0.08,
				0.01
			);
			world.spawnParticles(
				ParticleTypes.WHITE_ASH,
				point.x,
				point.y,
				point.z,
				Math.max(0, BURNING_PASSION_CONFIG.searingDash.trailEmberParticleCount),
				0.08,
				0.02,
				0.08,
				0.006
			);
			world.spawnParticles(
				ParticleTypes.CAMPFIRE_COSY_SMOKE,
				point.x,
				point.y + 0.02,
				point.z,
				Math.max(0, BURNING_PASSION_CONFIG.searingDash.trailSmokeParticleCount),
				0.08,
				0.04,
				0.08,
				0.004
			);
		}
	}

	static LivingEntity findBurningPassionSearingDashCollisionTarget(ServerPlayerEntity player, Vec3d dashStartPos, Vec3d dashEndPos) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return null;
		}

		double radius = Math.max(0.1, BURNING_PASSION_CONFIG.searingDash.collisionRadius);
		Box hitBox = player.getBoundingBox().stretch(dashEndPos.subtract(dashStartPos)).expand(radius);
		LivingEntity closestTarget = null;
		double closestDistanceSquared = Double.MAX_VALUE;
		for (Entity other : world.getOtherEntities(player, hitBox)) {
			if (!(other instanceof LivingEntity target) || !canApplyBurningPassionSearingDashImpactToTarget(target)) {
				continue;
			}
			if (!hitBox.intersects(target.getBoundingBox())) {
				continue;
			}

			double distanceSquared = target.squaredDistanceTo(player);
			if (distanceSquared < closestDistanceSquared) {
				closestDistanceSquared = distanceSquared;
				closestTarget = target;
			}
		}
		return closestTarget;
	}

	static boolean canApplyBurningPassionSearingDashImpactToTarget(LivingEntity target) {
		if (target == null || !target.isAlive()) {
			return false;
		}
		if (target instanceof ServerPlayerEntity playerTarget) {
			return !playerTarget.isSpectator() && BURNING_PASSION_CONFIG.searingDash.affectPlayers;
		}
		return BURNING_PASSION_CONFIG.searingDash.affectMobs && target instanceof MobEntity;
	}

	static void triggerBurningPassionSearingDashImpact(ServerPlayerEntity player, LivingEntity target) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		Vec3d impactPos = new Vec3d(target.getX(), target.getBodyY(0.35), target.getZ());
		world.spawnParticles(
			ParticleTypes.FLAME,
			impactPos.x,
			impactPos.y,
			impactPos.z,
			Math.max(0, BURNING_PASSION_CONFIG.searingDash.impactExplosionParticleCount),
			0.24,
			0.24,
			0.24,
			0.05
		);
		world.spawnParticles(ParticleTypes.EXPLOSION, impactPos.x, impactPos.y, impactPos.z, 1, 0.0, 0.0, 0.0, 0.0);
		playConfiguredSound(world, impactPos, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), BURNING_PASSION_CONFIG.searingDash.impactSoundVolume, BURNING_PASSION_CONFIG.searingDash.impactSoundPitch);
		if (BURNING_PASSION_CONFIG.searingDash.impactExplosionDamage > 0.0F) {
			dealTrackedMagicDamage(
				target,
				player.getUuid(),
				world.getDamageSources().explosion(player, player),
				BURNING_PASSION_CONFIG.searingDash.impactExplosionDamage
			);
		}
		Vec3d knockbackDirection = normalizedHorizontalDirection(
			new Vec3d(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ()),
			player
		);
		applyForcedVelocity(
			target,
			new Vec3d(
				knockbackDirection.x * BURNING_PASSION_CONFIG.searingDash.impactKnockbackHorizontalVelocity,
				BURNING_PASSION_CONFIG.searingDash.impactKnockbackVerticalVelocity,
				knockbackDirection.z * BURNING_PASSION_CONFIG.searingDash.impactKnockbackHorizontalVelocity
			)
		);
		applyForcedVelocity(
			player,
			new Vec3d(
				-knockbackDirection.x * BURNING_PASSION_CONFIG.searingDash.impactKnockbackHorizontalVelocity,
				BURNING_PASSION_CONFIG.searingDash.impactKnockbackVerticalVelocity,
				-knockbackDirection.z * BURNING_PASSION_CONFIG.searingDash.impactKnockbackHorizontalVelocity
			)
		);
	}

	static boolean isTargetTouchingBurningPassionTrailLine(ServerPlayerEntity target, BurningPassionTrailLineState state) {
		double radius = Math.max(0.1, BURNING_PASSION_CONFIG.searingDash.collisionRadius);
		double radiusSq = radius * radius;
		Box collisionBox = target.getBoundingBox().expand(radius);
		Vec3d targetFeetPos = new Vec3d(target.getX(), target.getY() + 0.05, target.getZ());
		for (int index = 1; index < state.points.size(); index++) {
			Vec3d start = state.points.get(index - 1);
			Vec3d end = state.points.get(index);
			if (collisionBox.raycast(start, end).isPresent() || squaredDistanceToSegment(targetFeetPos, start, end) <= radiusSq) {
				return true;
			}
		}
		return false;
	}

	static void updateOverrideMeteors(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, OverrideMeteorState>> iterator = OVERRIDE_METEOR_STATES.entrySet().iterator();
		boolean domainStateChanged = false;
		while (iterator.hasNext()) {
			Map.Entry<UUID, OverrideMeteorState> entry = iterator.next();
			OverrideMeteorState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null) {
				iterator.remove();
				continue;
			}

			updateOverrideMeteorVisual(world, state, currentTick);
			if (currentTick < state.impactTick) {
				continue;
			}

			impactOverrideMeteor(server, world, state, currentTick);
			discardOverrideMeteorDisplay(server, state);
			iterator.remove();
			domainStateChanged = true;
		}
		if (domainStateChanged) {
			persistDomainRuntimeState(server);
		}
	}

	static void impactOverrideMeteor(
		MinecraftServer server,
		ServerWorld world,
		OverrideMeteorState state,
		int currentTick
	) {
		int flashParticles = Math.max(0, BURNING_PASSION_CONFIG.override.impactFlashParticleCount);
		int debrisParticles = Math.max(0, BURNING_PASSION_CONFIG.override.debrisParticleCount);
		int radialBurstParticles = Math.max(0, BURNING_PASSION_CONFIG.override.radialFireBurstParticleCount);
		world.spawnParticles(
			ParticleTypes.FLAME,
			state.impactCenter.x,
			state.impactCenter.y + 0.1,
			state.impactCenter.z,
			flashParticles,
			state.impactRadius * 0.24,
			0.45,
			state.impactRadius * 0.24,
			0.18
		);
		world.spawnParticles(
			ParticleTypes.LAVA,
			state.impactCenter.x,
			state.impactCenter.y + 0.18,
			state.impactCenter.z,
			Math.max(1, radialBurstParticles / 3),
			state.impactRadius * 0.16,
			0.22,
			state.impactRadius * 0.16,
			0.12
		);
		world.spawnParticles(
			ParticleTypes.EXPLOSION,
			state.impactCenter.x,
			state.impactCenter.y + 0.2,
			state.impactCenter.z,
			Math.max(2, flashParticles / 18),
			state.impactRadius * 0.18,
			0.18,
			state.impactRadius * 0.18,
			0.02
		);
		world.spawnParticles(
			ParticleTypes.EXPLOSION_EMITTER,
			state.impactCenter.x,
			state.impactCenter.y + 0.2,
			state.impactCenter.z,
			Math.max(2, flashParticles / 70),
			state.impactRadius * 0.1,
			0.08,
			state.impactRadius * 0.1,
			0.0
		);
		world.spawnParticles(
			ParticleTypes.WHITE_ASH,
			state.impactCenter.x,
			state.impactCenter.y + 0.1,
			state.impactCenter.z,
			debrisParticles,
			state.impactRadius * 0.26,
			0.5,
			state.impactRadius * 0.26,
			0.05
		);
		world.spawnParticles(
			ParticleTypes.ASH,
			state.impactCenter.x,
			state.impactCenter.y + 0.08,
			state.impactCenter.z,
			Math.max(1, debrisParticles / 2),
			state.impactRadius * 0.22,
			0.35,
			state.impactRadius * 0.22,
			0.04
		);
		spawnOverrideImpactRings(world, state);
		playConfiguredSound(world, state.impactCenter, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), BURNING_PASSION_CONFIG.override.impactSoundVolume, BURNING_PASSION_CONFIG.override.impactSoundPitch);
		playConfiguredSound(world, state.impactCenter, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, Math.max(0.0F, BURNING_PASSION_CONFIG.override.impactSoundVolume * 0.9F), Math.max(0.1F, BURNING_PASSION_CONFIG.override.impactSoundPitch * 1.15F));

		for (UUID ownerId : state.targetDomainOwnerIds) {
			DomainExpansionState domainState = DOMAIN_EXPANSIONS.remove(ownerId);
			if (domainState == null) {
				continue;
			}
			endOwnedDomain(ownerId, domainState, server, currentTick);
			ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerId);
			if (owner != null) {
				if (activeAbility(owner) == domainState.ability) {
					setActiveAbility(owner, MagicAbility.NONE);
				}
				applyDomainInstabilityPenalty(owner);
			}
		}

		Box impactBox = new Box(
			state.impactCenter.x - state.impactRadius,
			world.getBottomY(),
			state.impactCenter.z - state.impactRadius,
			state.impactCenter.x + state.impactRadius,
			world.getTopYInclusive() + 1.0,
			state.impactCenter.z + state.impactRadius
		);
		Predicate<Entity> filter = entity ->
			isMagicTargetableEntity(entity)
				&& entity.getUuid() != state.casterId
				&& (
					(entity instanceof ServerPlayerEntity && BURNING_PASSION_CONFIG.override.affectPlayers)
					|| (!(entity instanceof PlayerEntity) && BURNING_PASSION_CONFIG.override.affectMobs)
				);
		for (Entity entity : world.getOtherEntities(null, impactBox, filter)) {
			if (!(entity instanceof LivingEntity target)) {
				continue;
			}
			double dx = target.getX() - state.impactCenter.x;
			double dz = target.getZ() - state.impactCenter.z;
			if (dx * dx + dz * dz > state.impactRadius * state.impactRadius) {
				continue;
			}
			dealTrackedMagicDamage(target, state.casterId, world.getDamageSources().genericKill(), BURNING_PASSION_CONFIG.override.impactDamage);
		}
	}

	static void spawnOverrideImpactRings(ServerWorld world, OverrideMeteorState state) {
		int shockwaveParticles = Math.max(0, BURNING_PASSION_CONFIG.override.shockwaveParticleCount);
		for (int index = 0; index < shockwaveParticles; index++) {
			double angle = (Math.PI * 2.0 * index) / Math.max(1, shockwaveParticles);
			double x = state.impactCenter.x + Math.cos(angle) * state.impactRadius;
			double z = state.impactCenter.z + Math.sin(angle) * state.impactRadius;
			double velocityX = Math.cos(angle) * 0.18;
			double velocityZ = Math.sin(angle) * 0.18;
			world.spawnParticles(ParticleTypes.CLOUD, x, state.impactCenter.y + 0.05, z, 0, velocityX, 0.02, velocityZ, 1.0);
			world.spawnParticles(ParticleTypes.EXPLOSION, x, state.impactCenter.y + 0.08, z, 0, velocityX * 0.15, 0.01, velocityZ * 0.15, 1.0);
			world.spawnParticles(ParticleTypes.WHITE_ASH, x, state.impactCenter.y + 0.03, z, 0, velocityX * 0.9, 0.05, velocityZ * 0.9, 1.0);
		}
		int radialFireParticles = Math.max(0, BURNING_PASSION_CONFIG.override.radialFireBurstParticleCount);
		for (int index = 0; index < radialFireParticles; index++) {
			double angle = (Math.PI * 2.0 * index) / Math.max(1, radialFireParticles);
			double velocityX = Math.cos(angle) * (0.08 + state.impactRadius * 0.01);
			double velocityZ = Math.sin(angle) * (0.08 + state.impactRadius * 0.01);
			world.spawnParticles(ParticleTypes.FLAME, state.impactCenter.x, state.impactCenter.y + 0.18, state.impactCenter.z, 0, velocityX, 0.08, velocityZ, 1.0);
			world.spawnParticles(ParticleTypes.LAVA, state.impactCenter.x, state.impactCenter.y + 0.16, state.impactCenter.z, 0, velocityX * 0.85, 0.12, velocityZ * 0.85, 1.0);
			world.spawnParticles(ParticleTypes.WHITE_ASH, state.impactCenter.x, state.impactCenter.y + 0.12, state.impactCenter.z, 0, velocityX * 0.7, 0.04, velocityZ * 0.7, 1.0);
			world.spawnParticles(ParticleTypes.ASH, state.impactCenter.x, state.impactCenter.y + 0.1, state.impactCenter.z, 0, velocityX * 0.55, 0.03, velocityZ * 0.55, 1.0);
		}
	}

	static void updateOverrideMeteorVisual(ServerWorld world, OverrideMeteorState state, int currentTick) {
		double progress = MathHelper.clamp((currentTick - state.startTick) / (double) Math.max(1, state.impactTick - state.startTick), 0.0, 1.0);
		double currentY = state.spawnY - (state.spawnY - state.impactCenter.y) * progress;
		if (state.displayEntityId != null) {
			Entity entity = world.getEntity(state.displayEntityId);
			if (entity != null) {
				entity.refreshPositionAndAngles(state.impactCenter.x, currentY, state.impactCenter.z, 0.0F, 0.0F);
			}
		}
		if (
			BURNING_PASSION_CONFIG.override.warningSoundIntervalTicks > 0
			&& currentTick >= state.nextWarningSoundTick
		) {
			playConfiguredSound(
				world,
				new Vec3d(state.impactCenter.x, currentY, state.impactCenter.z),
				SoundEvents.ENTITY_BLAZE_SHOOT,
				BURNING_PASSION_CONFIG.override.warningSoundVolume,
				BURNING_PASSION_CONFIG.override.warningSoundPitch
			);
			state.nextWarningSoundTick = currentTick + BURNING_PASSION_CONFIG.override.warningSoundIntervalTicks;
		}
		spawnOverrideTelegraphParticles(world, state, currentY);
	}

	static void spawnOverrideTelegraphParticles(ServerWorld world, OverrideMeteorState state, double currentY) {
		double meteorScale = Math.max(0.5, BURNING_PASSION_CONFIG.override.meteorCoreDisplayScale);
		double coreSpread = Math.max(0.45, meteorScale * 0.14);
		double smokeSpread = Math.max(0.55, meteorScale * 0.16);
		double trailSpread = Math.max(0.2, meteorScale * 0.08);
		int telegraphParticles = Math.max(0, BURNING_PASSION_CONFIG.override.telegraphParticleCount);
		for (int index = 0; index < telegraphParticles; index++) {
			double angle = (Math.PI * 2.0 * index) / Math.max(1, telegraphParticles);
			double x = state.impactCenter.x + Math.cos(angle) * state.impactRadius;
			double z = state.impactCenter.z + Math.sin(angle) * state.impactRadius;
			world.spawnParticles(ParticleTypes.FLAME, x, state.impactCenter.y + 0.04, z, 0, 0.0, 0.02, 0.0, 1.0);
		}
		world.spawnParticles(
			ParticleTypes.FLAME,
			state.impactCenter.x,
			currentY,
			state.impactCenter.z,
			BURNING_PASSION_CONFIG.override.meteorCoreParticleCount,
			coreSpread,
			coreSpread,
			coreSpread,
			0.03
		);
		world.spawnParticles(
			ParticleTypes.LARGE_SMOKE,
			state.impactCenter.x,
			currentY,
			state.impactCenter.z,
			BURNING_PASSION_CONFIG.override.meteorSmokeParticleCount,
			smokeSpread,
			smokeSpread,
			smokeSpread,
			0.02
		);
		double trailHeight = Math.max(0.0, state.spawnY - currentY);
		int trailSteps = Math.max(1, (int) Math.ceil(trailHeight / 2.0));
		for (int step = 0; step < trailSteps; step++) {
			double y = currentY + step * 2.0;
			world.spawnParticles(
				ParticleTypes.FLAME,
				state.impactCenter.x,
				y,
				state.impactCenter.z,
				Math.max(0, BURNING_PASSION_CONFIG.override.meteorTrailParticleCount / trailSteps),
				trailSpread,
				trailSpread + 0.1,
				trailSpread,
				0.01
			);
			world.spawnParticles(
				ParticleTypes.WHITE_ASH,
				state.impactCenter.x,
				y,
				state.impactCenter.z,
				Math.max(0, BURNING_PASSION_CONFIG.override.meteorEmberParticleCount / trailSteps),
				trailSpread + 0.05,
				trailSpread + 0.15,
				trailSpread + 0.05,
				0.008
			);
		}
	}

	static OverrideMeteorState createOverrideMeteorState(
		ServerPlayerEntity player,
		ServerWorld world,
		Vec3d impactCenter,
		double impactRadius,
		List<UUID> targetDomainOwnerIds,
		int currentTick
	) {
		UUID displayId = null;
		DisplayEntity.BlockDisplayEntity display = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
		display.refreshPositionAndAngles(
			impactCenter.x,
			impactCenter.y + BURNING_PASSION_CONFIG.override.meteorSpawnHeight,
			impactCenter.z,
			0.0F,
			0.0F
		);
		((BlockDisplayEntityAccessorMixin) display).magic$setBlockState(Blocks.MAGMA_BLOCK.getDefaultState());
		((DisplayEntityAccessorMixin) display).magic$setTeleportDuration(1);
		((DisplayEntityAccessorMixin) display).magic$setInterpolationDuration(1);
		float meteorScale = (float) Math.max(0.5, BURNING_PASSION_CONFIG.override.meteorCoreDisplayScale);
		((DisplayEntityAccessorMixin) display).magic$setTransformation(
			new AffineTransformation(
				new Vector3f(-meteorScale * 0.5F, 0.0F, -meteorScale * 0.5F),
				new Quaternionf(),
				new Vector3f(meteorScale, meteorScale, meteorScale),
				new Quaternionf()
			)
		);
		((DisplayEntityAccessorMixin) display).magic$setDisplayWidth(meteorScale);
		((DisplayEntityAccessorMixin) display).magic$setDisplayHeight(meteorScale);
		((DisplayEntityAccessorMixin) display).magic$setViewRange(Math.max(2.5F, meteorScale * 1.75F));
		display.setNoGravity(true);
		display.setSilent(true);
		if (world.spawnEntity(display)) {
			displayId = display.getUuid();
		}
		return new OverrideMeteorState(
			world.getRegistryKey(),
			player.getUuid(),
			impactCenter,
			impactCenter.y + BURNING_PASSION_CONFIG.override.meteorSpawnHeight,
			impactRadius,
			currentTick,
			currentTick + BURNING_PASSION_CONFIG.override.fallDurationTicks,
			currentTick,
			displayId,
			targetDomainOwnerIds
		);
	}

	static List<UUID> qualifyingOverrideDomains(ServerPlayerEntity player, Vec3d impactCenter, double impactRadius) {
		List<UUID> qualifyingOwners = new ArrayList<>();
		double bestDistanceSq = Double.MAX_VALUE;
		UUID nearestOwner = null;
		RegistryKey<World> dimension = player.getEntityWorld().getRegistryKey();
		for (Map.Entry<UUID, DomainExpansionState> entry : DOMAIN_EXPANSIONS.entrySet()) {
			DomainExpansionState domainState = entry.getValue();
			if (!domainState.dimension.equals(dimension)) {
				continue;
			}

			boolean qualifies = false;
			if (BURNING_PASSION_CONFIG.override.allowCastInsideContainingDomain && isInsideDomainInterior(player, domainState)) {
				qualifies = true;
			}
			if (!qualifies && BURNING_PASSION_CONFIG.override.allowCastWhenDomainFullyContained) {
				double dx = domainState.centerX - impactCenter.x;
				double dz = domainState.centerZ - impactCenter.z;
				double centerDistance = Math.sqrt(dx * dx + dz * dz);
				qualifies = centerDistance + domainState.radius <= impactRadius;
			}
			if (!qualifies) {
				continue;
			}

			double dx = domainState.centerX - impactCenter.x;
			double dz = domainState.centerZ - impactCenter.z;
			double distanceSq = dx * dx + dz * dz;
			if (distanceSq < bestDistanceSq) {
				bestDistanceSq = distanceSq;
				nearestOwner = entry.getKey();
			}
			if (BURNING_PASSION_CONFIG.override.destroyAllQualifyingDomains) {
				qualifyingOwners.add(entry.getKey());
			}
		}
		if (!BURNING_PASSION_CONFIG.override.destroyAllQualifyingDomains && nearestOwner != null) {
			qualifyingOwners.add(nearestOwner);
		}
		return qualifyingOwners;
	}

	static Vec3d resolveOverrideImpactCenter(ServerWorld world, ServerPlayerEntity player) {
		Vec3d groundAnchor = findFrostGroundAnchor(world, new Vec3d(player.getX(), player.getY(), player.getZ()), 4, 8);
		if (groundAnchor != null) {
			return groundAnchor;
		}
		return new Vec3d(player.getX(), player.getY() - 0.1, player.getZ());
	}

	static double burningPassionOverrideImpactRadius() {
		return Math.max(1.0, DOMAIN_EXPANSION_RADIUS * BURNING_PASSION_CONFIG.override.impactRadiusMultiplierFromStandardDomainRadius);
	}

	static boolean clearOverrideMeteorState(UUID playerId, MinecraftServer server) {
		OverrideMeteorState state = OVERRIDE_METEOR_STATES.remove(playerId);
		if (state == null) {
			return false;
		}
		discardOverrideMeteorDisplay(server, state);
		return true;
	}

	static void discardOverrideMeteorDisplay(MinecraftServer server, OverrideMeteorState state) {
		if (server == null || state == null || state.displayEntityId == null) {
			return;
		}
		ServerWorld world = server.getWorld(state.dimension);
		if (world == null) {
			return;
		}
		Entity entity = world.getEntity(state.displayEntityId);
		if (entity != null) {
			entity.discard();
		}
	}
}


