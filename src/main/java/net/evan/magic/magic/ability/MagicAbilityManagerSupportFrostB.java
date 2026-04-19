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


abstract class MagicAbilityManagerSupportFrostB extends MagicAbilityManagerSupportFrostA {
	static void handleBelowFreezingRequest(ServerPlayerEntity player) {
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

	static void handleFrostAscentRequest(ServerPlayerEntity player) {
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

	static void handleAbsoluteZeroRequest(ServerPlayerEntity player) {
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

		FrostStageState stageState = FROST_STAGE_STATES.get(player.getUuid());
		if (
			activeAbility(player) != MagicAbility.BELOW_FREEZING
			|| stageState == null
			|| stageState.dimension != player.getEntityWorld().getRegistryKey()
			|| stageState.currentStage != 3
			|| frostMaximumUnlockRemainingTicks(stageState) > 0
		) {
			player.sendMessage(Text.translatable("message.magic.frost.maximum_not_unlocked"), true);
			return;
		}

		activateFrostMaximum(player, currentTick);
		recordOrionsGambitAbilityUse(player, MagicAbility.ABSOLUTE_ZERO);
	}

	static void handlePlanckHeatRequest(ServerPlayerEntity player) {
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

	static void activateFrostStagedMode(ServerPlayerEntity player, int currentTick) {
		UUID playerId = player.getUuid();
		FrostStageState state = FROST_STAGE_STATES.get(playerId);
		if (state == null) {
			state = new FrostStageState(player.getEntityWorld().getRegistryKey(), 1, 1, 0, 0);
			FROST_STAGE_STATES.put(playerId, state);
		} else {
			state.dimension = player.getEntityWorld().getRegistryKey();
			state.currentStage = 1;
			state.stageThreeHoldTicks = 0;
		}
		setActiveAbility(player, MagicAbility.BELOW_FREEZING);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		if (belowFreezingCooldownRemaining(player, currentTick) <= 0) {
			BELOW_FREEZING_COOLDOWN_END_TICK.remove(playerId);
		}
		syncFrostStageHud(player);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.BELOW_FREEZING.displayName()), true);
	}

	static void applyFrostStagedMode(ServerPlayerEntity player, int currentTick) {
		FrostStageState state = FROST_STAGE_STATES.get(player.getUuid());
		if (state == null || state.dimension != player.getEntityWorld().getRegistryKey()) {
			endFrostStagedMode(player, currentTick, FrostStageEndReason.INVALID, false, false);
			return;
		}
		if (isLockedFrostStage(player, state.currentStage)) {
			endFrostStagedMode(player, currentTick, FrostStageEndReason.LOCKED, false, false);
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
		progressFrostMaximumUnlock(state);
		syncFrostStageHud(player);
	}

	static void endFrostStagedMode(
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
			state.stageThreeHoldTicks = 0;
			state.dimension = player.getEntityWorld().getRegistryKey();
			if (
				FROST_CONFIG.progression.clearUnlocksOnEnd
					|| reason == FrostStageEndReason.FORCED_THRESHOLD
					|| reason == FrostStageEndReason.OVERCAST
					|| reason == FrostStageEndReason.INVALID
					|| reason == FrostStageEndReason.LOCKED
			) {
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

	static boolean advanceFrostStage(ServerPlayerEntity player, int currentTick) {
		FrostStageState state = FROST_STAGE_STATES.get(player.getUuid());
		if (
			state == null
				|| state.currentStage >= 3
				|| state.highestUnlockedStage <= state.currentStage
				|| !MagicConfig.get().abilityAccess.isStageProgressionUnlocked(player.getUuid(), MagicSchool.FROST, state.currentStage + 1)
		) {
			return false;
		}
		state.currentStage = Math.min(3, state.currentStage + 1);
		state.progressTicks = 0;
		state.stageThreeHoldTicks = 0;
		if (FROST_ASCENT_COOLDOWN_TICKS > 0) {
			startAbilityCooldownFromNow(player.getUuid(), MagicAbility.FROST_ASCENT, currentTick);
		}
		syncFrostStageHud(player);
		player.sendMessage(Text.translatable("message.magic.frost.stage_advanced", state.currentStage), true);
		return true;
	}

	static void castFrostRangedAttack(ServerPlayerEntity player, int currentTick) {
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
		FrostSpikeWaveState waveState = new FrostSpikeWaveState(
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
		);
		FROST_SPIKE_WAVES.add(waveState);
		if (player.getEntityWorld() instanceof ServerWorld world) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8F, overcast ? 0.5F : 0.9F);
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.9F, 1.6F);
		}
		if (!overcast && player.getRandom().nextDouble() * 100.0 < FROST_CONFIG.rangedAttack.setbackChancePercent) {
			applyFrostSetback(player, state.currentStage - 1);
		}
		if (overcast) {
			endFrostStagedMode(player, currentTick, FrostStageEndReason.OVERCAST, true, false);
			preserveFrostSpikeWave(waveState);
		} else if (isBelowFrostThreshold(player)) {
			endFrostStagedMode(player, currentTick, FrostStageEndReason.FORCED_THRESHOLD, true, false);
			preserveFrostSpikeWave(waveState);
		}
	}

	static boolean castFrostSlam(ServerPlayerEntity player, int currentTick) {
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
			applyFrostActionDamage(player, target, slamDamage, true, false, FrostKillProgressType.SLAM);
			applyFrostStageHitEffects(player, target, currentTick);
			applyFrostFreeze(target, player.getUuid(), currentTick + slamConfig.freezeDurationTicks);
		}
		if (slamStage == 2 && player.getRandom().nextDouble() * 100.0 < slamConfig.setbackChancePercent) {
			applyFrostSetback(player, 1, slamConfig.setbackProgressPercent);
		}
		return true;
	}

	static void activateFrostMaximum(ServerPlayerEntity player, int currentTick) {
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

	static void applyFrostMaximum(ServerPlayerEntity player, int currentTick) {
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

	static void applyFrostMaximumWindup(ServerPlayerEntity player, FrostMaximumState state, int currentTick) {
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
		Predicate<Entity> filter = entity -> isMagicTargetableEntity(entity) && entity != player;
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

	static void beginFrostMaximumBurst(ServerPlayerEntity player, FrostMaximumState state, int currentTick) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		double burstRadius = FROST_CONFIG.domain.radius * FROST_CONFIG.maximum.burstRadiusMultiplierFromDomainRadius;
		state.phase = FrostMaximumPhase.ENCASE;
		state.encaseEndTick = currentTick + FROST_CONFIG.maximum.packedIceDurationTicks;
		state.pendingHelplessTargets.clear();
		Box burstBox = player.getBoundingBox().expand(burstRadius);
		Predicate<Entity> filter = entity -> isMagicTargetableEntity(entity) && entity != player;
		for (Entity entity : player.getEntityWorld().getOtherEntities(player, burstBox, filter)) {
			if (entity instanceof LivingEntity livingTarget) {
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

	static void finishFrostMaximum(ServerPlayerEntity player, FrostMaximumState state, int currentTick) {
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
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
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

	static void clearFrostMaximumState(ServerPlayerEntity player, boolean clearCooldown) {
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

	static void progressFrostStageUnlocks(ServerPlayerEntity player, FrostStageState state) {
		int requirement = frostProgressRequirementTicks(state.currentStage);
		if (requirement <= 0 || state.highestUnlockedStage > state.currentStage) {
			return;
		}
		state.progressTicks = Math.min(requirement, state.progressTicks + 1);
		if (state.progressTicks >= requirement && state.highestUnlockedStage == state.currentStage) {
			if (!MagicConfig.get().abilityAccess.isStageProgressionUnlocked(player.getUuid(), MagicSchool.FROST, state.currentStage + 1)) {
				endFrostStagedMode(player, player.getEntityWorld().getServer().getTicks(), FrostStageEndReason.LOCKED, false, false);
				return;
			}
			state.highestUnlockedStage = Math.min(3, state.currentStage + 1);
			player.sendMessage(Text.translatable("message.magic.frost.stage_unlocked", state.highestUnlockedStage), true);
		}
	}

	static void progressSuspendedFrostStageDuringDomainClash(ServerPlayerEntity player) {
		if (player == null || MagicPlayerData.getSchool(player) != MagicSchool.FROST || activeAbility(player) == MagicAbility.BELOW_FREEZING) {
			return;
		}
		FrostStageState state = FROST_STAGE_STATES.get(player.getUuid());
		if (state == null || state.dimension != player.getEntityWorld().getRegistryKey() || !MagicPlayerData.isDomainClashActive(player)) {
			return;
		}
		progressFrostStageUnlocks(player, state);
		progressFrostMaximumUnlock(state);
	}

	static void applyFrostStageOneAura(ServerPlayerEntity player, int currentTick) {
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

	static void applyFrostSetback(ServerPlayerEntity player, int targetStage) {
		FrostStageState state = FROST_STAGE_STATES.get(player.getUuid());
		if (state == null) {
			return;
		}
		int nextProgress = FROST_CONFIG.progression.resetProgressOnSetback
			? 0
			: Math.min(state.progressTicks, Math.max(0, frostProgressRequirementTicks(Math.max(1, targetStage))));
		applyFrostSetback(player, targetStage, nextProgress);
	}

	static void applyFrostSetback(ServerPlayerEntity player, int targetStage, double progressPercent) {
		int requirement = Math.max(0, frostProgressRequirementTicks(Math.max(1, targetStage)));
		int nextProgress = requirement <= 0
			? 0
			: MathHelper.clamp((int) Math.round(requirement * (MathHelper.clamp(progressPercent, 0.0, 100.0) / 100.0)), 0, requirement);
		applyFrostSetback(player, targetStage, nextProgress);
	}

	static void applyFrostSetback(ServerPlayerEntity player, int targetStage, int nextProgress) {
		FrostStageState state = FROST_STAGE_STATES.get(player.getUuid());
		if (state == null) {
			return;
		}
		if (targetStage <= 1) {
			state.currentStage = 1;
			state.highestUnlockedStage = 1;
			state.progressTicks = nextProgress;
		} else {
			state.currentStage = Math.min(3, targetStage);
			state.highestUnlockedStage = Math.min(state.currentStage, 3);
			state.progressTicks = nextProgress;
		}
		state.stageThreeHoldTicks = 0;
		syncFrostStageHud(player);
		player.sendMessage(Text.translatable("message.magic.frost.stage_setback", state.currentStage), true);
	}

	static void applyFrostStageHitEffects(ServerPlayerEntity caster, LivingEntity target, int currentTick) {
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

	static void applyFrostActionDamage(
		ServerPlayerEntity caster,
		LivingEntity target,
		float damage,
		boolean trueDamage,
		boolean overcast,
		FrostKillProgressType progressType
	) {
		if (!(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		if (overcast && FROST_CONFIG.rangedAttack.instantKillEnabled) {
			dealTrackedMagicDamage(target, caster.getUuid(), world.getDamageSources().freeze(), Float.MAX_VALUE);
		} else if (damage > 0.0F) {
			dealTrackedMagicDamage(
				target,
				caster.getUuid(),
				trueDamage ? createTrueMagicDamageSource(world, caster) : world.getDamageSources().magic(),
				damage
			);
		}
		if (target.isAlive() || !(target instanceof ServerPlayerEntity)) {
			return;
		}
		grantFrostKillProgress(caster, progressType);
	}

	static void grantFrostKillProgress(ServerPlayerEntity caster, FrostKillProgressType progressType) {
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
			if (!MagicConfig.get().abilityAccess.isStageProgressionUnlocked(caster.getUuid(), MagicSchool.FROST, state.currentStage + 1)) {
				endFrostStagedMode(caster, caster.getEntityWorld().getServer().getTicks(), FrostStageEndReason.LOCKED, false, false);
				return;
			}
			state.highestUnlockedStage = Math.min(3, state.currentStage + 1);
			caster.sendMessage(Text.translatable("message.magic.frost.stage_unlocked", state.highestUnlockedStage), true);
		}
		syncFrostStageHud(caster);
	}

	static void progressFrostMaximumUnlock(FrostStageState state) {
		if (state == null) {
			return;
		}
		if (state.currentStage != 3) {
			state.stageThreeHoldTicks = 0;
			return;
		}
		state.stageThreeHoldTicks = Math.min(
			FROST_CONFIG.progression.maximumUnlockTicks,
			Math.max(0, state.stageThreeHoldTicks) + 1
		);
	}

	static void applyFrostFreeze(LivingEntity target, UUID casterId, int expiresTick) {
		storeFrostFreezeState(FROST_FROZEN_TARGETS, target, casterId, expiresTick);
	}

	static void applyDomainFrostFreeze(LivingEntity target, UUID casterId, int expiresTick) {
		storeFrostFreezeState(FROST_DOMAIN_FROZEN_TARGETS, target, casterId, expiresTick);
	}

	static void storeFrostFreezeState(
		Map<UUID, FrostFreezeState> targetStates,
		LivingEntity target,
		UUID casterId,
		int expiresTick
	) {
		targetStates.put(
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

	static void updateFrostSpikeWaves(MinecraftServer server, int currentTick) {
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
					applyFrostActionDamage(caster, target, state.damage, false, state.overcast, FrostKillProgressType.RANGED);
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

	static void updateFrostFrozenTargets(MinecraftServer server, int currentTick) {
		updateFrostFrozenTargetStates(server, currentTick, FROST_FROZEN_TARGETS, false);
		updateFrostFrozenTargetStates(server, currentTick, FROST_DOMAIN_FROZEN_TARGETS, true);
	}

	static void updateFrostFrozenTargetStates(
		MinecraftServer server,
		int currentTick,
		Map<UUID, FrostFreezeState> targetStates,
		boolean suppressDuringClash
	) {
		Iterator<Map.Entry<UUID, FrostFreezeState>> iterator = targetStates.entrySet().iterator();
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
			if (suppressDuringClash && isDomainExpansionEffectSuppressedByClash(state.casterId)) {
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

	static void updateFrostMaximumFearTargets(MinecraftServer server, int currentTick) {
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
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target) || caster == null || !caster.isAlive()) {
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

	static void updateFrostPackedIceTargets(MinecraftServer server, int currentTick) {
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
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
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

	static void updateFrostHelplessTargets(MinecraftServer server, int currentTick) {
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
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
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

	static void clearFrostEffectsByCaster(UUID casterId, boolean clearMaximumEffects, MinecraftServer server) {
		FROST_SPIKE_WAVES.removeIf(state -> casterId.equals(state.casterId));
		FROSTBITTEN_TARGETS.entrySet().removeIf(entry -> casterId.equals(entry.getValue().casterId));
		FROST_DOMAIN_FROSTBITTEN_TARGETS.entrySet().removeIf(entry -> casterId.equals(entry.getValue().casterId));
		FROST_FROZEN_TARGETS.entrySet().removeIf(entry -> casterId.equals(entry.getValue().casterId));
		FROST_DOMAIN_FROZEN_TARGETS.entrySet().removeIf(entry -> casterId.equals(entry.getValue().casterId));
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

	static void preserveFrostSpikeWave(FrostSpikeWaveState waveState) {
		if (waveState == null || waveState.travelled >= waveState.range || FROST_SPIKE_WAVES.contains(waveState)) {
			return;
		}
		FROST_SPIKE_WAVES.add(waveState);
	}

	static void clearFrostControlledTargetState(UUID targetId, MinecraftServer server) {
		FROST_FROZEN_TARGETS.remove(targetId);
		FROST_DOMAIN_FROZEN_TARGETS.remove(targetId);
		FROST_DOMAIN_PULSE_STATUS_TARGETS.remove(targetId);
		FROST_MAXIMUM_FEAR_TARGETS.remove(targetId);
		FROST_HELPLESS_TARGETS.remove(targetId);
		FrostPackedIceState packedIceState = FROST_PACKED_ICE_TARGETS.remove(targetId);
		if (packedIceState != null) {
			clearFrostPackedIceEncasement(server, packedIceState, false);
		}
	}

	static void removeFrostStageCasterBuffs(ServerPlayerEntity player) {
		removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), FROST_STAGE_MAX_HEALTH_MODIFIER_ID);
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		removeStatusEffectIfMatching(player, StatusEffects.SLOWNESS, frostPassiveAmplifiers(true));
		removeStatusEffectIfMatching(player, StatusEffects.RESISTANCE, frostPassiveAmplifiers(false));
	}

	static void applyFrostStageMaxHealth(ServerPlayerEntity player, double multiplier, boolean enabled) {
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

	static int frostProgressRequirementTicks(int currentStage) {
		return switch (currentStage) {
			case 1 -> FROST_CONFIG.progression.stageTwoUnlockTicks;
			case 2 -> FROST_CONFIG.progression.stageThreeUnlockTicks;
			default -> 0;
		};
	}

	static int frostMaximumUnlockRemainingTicks(FrostStageState state) {
		if (state == null || state.currentStage != 3) {
			return Math.max(0, FROST_CONFIG.progression.maximumUnlockTicks);
		}
		return Math.max(0, FROST_CONFIG.progression.maximumUnlockTicks - Math.max(0, state.stageThreeHoldTicks));
	}

	static MagicConfig.FrostStageConfig frostStageConfig(int stage) {
		return switch (stage) {
			case 2 -> FROST_CONFIG.stageTwo;
			case 3 -> FROST_CONFIG.stageThree;
			default -> FROST_CONFIG.stageOne;
		};
	}

	static boolean isBelowFrostThreshold(ServerPlayerEntity player) {
		double threshold = MagicPlayerData.MAX_MANA * (FROST_CONFIG.stagedModeForceEndThresholdPercent / 100.0);
		return MagicPlayerData.getMana(player) < Math.ceil(threshold);
	}

	static String formatTicksAsMinutesSeconds(int ticks) {
		int totalSeconds = Math.max(0, (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
		int minutes = totalSeconds / 60;
		int seconds = totalSeconds % 60;
		return minutes + ":" + (seconds < 10 ? "0" + seconds : Integer.toString(seconds));
	}
}

