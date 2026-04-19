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


abstract class CombatEffectService extends CooldownService {
	static void updateLoveLockedTargets(MinecraftServer server, int currentTick) {
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
			if (!(targetEntity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
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

	static void enforceLoveLock(ServerPlayerEntity caster, LivingEntity target, LoveLockState state, int currentTick) {
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

	static float[] computeFacingAngles(LivingEntity source, Vec3d lookTargetPos) {
		double dx = lookTargetPos.x - source.getX();
		double dy = lookTargetPos.y - source.getEyePos().y;
		double dz = lookTargetPos.z - source.getZ();
		double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
		float yaw = MathHelper.wrapDegrees((float) (Math.toDegrees(MathHelper.atan2(dz, dx)) - 90.0));
		float pitch = MathHelper.wrapDegrees((float) (-Math.toDegrees(MathHelper.atan2(dy, horizontalDistance))));
		return new float[] { yaw, pitch };
	}

	static void trimAbsoluteZeroAuraTimers(Map<UUID, Set<UUID>> seenThisTick) {
		ABSOLUTE_ZERO_NEXT_DAMAGE_TICK.entrySet().removeIf(entry -> {
			Set<UUID> seenTargets = seenThisTick.get(entry.getKey());
			if (seenTargets == null) {
				return true;
			}

			entry.getValue().entrySet().removeIf(targetEntry -> !seenTargets.contains(targetEntry.getKey()));
			return entry.getValue().isEmpty();
		});
	}

	static void applyAuraSlowness(ServerPlayerEntity caster, int radius, int slownessAmplifier, int shardCount) {
		Box area = caster.getBoundingBox().expand(radius);
		Predicate<Entity> filter = entity -> isMagicTargetableEntity(entity) && entity != caster;

		for (Entity entity : caster.getEntityWorld().getOtherEntities(caster, area, filter)) {
			if (entity instanceof LivingEntity livingTarget) {
				refreshStatusEffect(livingTarget, StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, slownessAmplifier, true, true, true);
				spawnHitboxShardParticles(livingTarget, shardCount);
			}
		}
	}

	static ActionResult onUseItem(PlayerEntity player, World world, Hand hand) {
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
		if (isConstellationTeleportItemBlocked(serverPlayer, stack)) {
			serverPlayer.sendMessage(Text.translatable("message.magic.constellation.celestial_alignment.teleport_blocked"), true);
			return ActionResult.FAIL;
		}
		if (isEntityCapturedByCelestialGamaRayBeam(serverPlayer) && isTeleportEscapeItem(stack)) {
			serverPlayer.sendMessage(Text.translatable("message.magic.constellation.celestial_gama_ray.teleport_blocked"), true);
			return ActionResult.FAIL;
		}
		if (isEntityCapturedByDomain(serverPlayer)) {
			if (isTeleportEscapeItem(stack)) {
				serverPlayer.sendMessage(Text.translatable("message.magic.domain.teleport_blocked"), true);
				return ActionResult.FAIL;
			}
		}
		GreedAbilityRuntime.onUseItem(serverPlayer, stack);
		return ActionResult.PASS;
	}

	static ActionResult onUseBlock(
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
			GreedAbilityRuntime.onUseBlock(serverPlayer, world, hitResult.getBlockPos(), world.getBlockState(hitResult.getBlockPos()), player.getStackInHand(hand));
		}
		return ActionResult.PASS;
	}

	static ActionResult onUseEntity(
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

	static ActionResult onAttackBlock(
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

	static boolean onBeforeBlockBreak(World world, PlayerEntity player, BlockPos pos) {
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

	static ActionResult onAttackEntity(
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

		if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target) || target == player) {
			if (isProtectedDomainDecoration(entity)) {
				sendDomainProtectedMessage(serverPlayer);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		}

		if (isDirectCasterDamageBlocked(serverPlayer, target, serverPlayer.getEntityWorld().getServer().getTicks())) {
			return ActionResult.FAIL;
		}

		GreedAbilityRuntime.onAttackEntity(serverPlayer, entity);

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

	static boolean shouldBlockArtifactUse(PlayerEntity player, Hand hand) {
		if (!MANIPULATION_BLOCK_ARTIFACT_USE_CLICKS || !player.getCommandTags().contains(EMPTY_EMBRACE_ARTIFACT_POWERS_TAG)) {
			return false;
		}

		return isArtifactItem(player.getStackInHand(hand));
	}

	static boolean shouldBlockArtifactAttack(PlayerEntity player) {
		if (!MANIPULATION_BLOCK_ARTIFACT_ATTACK_CLICKS || !player.getCommandTags().contains(EMPTY_EMBRACE_ARTIFACT_POWERS_TAG)) {
			return false;
		}

		return isArtifactItem(player.getMainHandStack());
	}

	static boolean isArtifactItem(ItemStack stack) {
		return GreedDomainRuntime.isArtifactItem(stack);
	}

	static void applyOrRefreshFrostbite(LivingEntity target, UUID casterId, int currentTick) {
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

	static void applyOrRefreshFrostbite(
		LivingEntity target,
		UUID casterId,
		int currentTick,
		int durationTicks,
		float damagePerTick,
		boolean refreshDuration,
		boolean stackDamage
	) {
		storeOrRefreshFrostbiteState(
			FROSTBITTEN_TARGETS,
			target,
			casterId,
			currentTick,
			durationTicks,
			damagePerTick,
			refreshDuration,
			stackDamage
		);
	}

	static void applyOrRefreshDomainFrostbite(
		LivingEntity target,
		UUID casterId,
		int currentTick,
		int durationTicks,
		float damagePerTick,
		boolean refreshDuration,
		boolean stackDamage
	) {
		storeOrRefreshFrostbiteState(
			FROST_DOMAIN_FROSTBITTEN_TARGETS,
			target,
			casterId,
			currentTick,
			durationTicks,
			damagePerTick,
			refreshDuration,
			stackDamage
		);
	}

	static void storeOrRefreshFrostbiteState(
		Map<UUID, FrostbiteState> targetStates,
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
		FrostbiteState state = targetStates.get(targetId);
		boolean newApplication = false;
		if (state == null || state.dimension != dimension) {
			state = new FrostbiteState(
				dimension,
				casterId,
				expiresTick,
				currentTick + FROSTBITE_DAMAGE_INTERVAL_TICKS,
				Math.max(0.0F, damagePerTick)
			);
			targetStates.put(targetId, state);
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

	static void updateFrostbittenTargets(MinecraftServer server, int currentTick) {
		updateFrostbiteTargetStates(server, currentTick, FROSTBITTEN_TARGETS, false);
		updateFrostbiteTargetStates(server, currentTick, FROST_DOMAIN_FROSTBITTEN_TARGETS, true);
	}

	static void updateFrostbiteTargetStates(
		MinecraftServer server,
		int currentTick,
		Map<UUID, FrostbiteState> targetStates,
		boolean suppressDuringClash
	) {
		Iterator<Map.Entry<UUID, FrostbiteState>> iterator = targetStates.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, FrostbiteState> entry = iterator.next();
			FrostbiteState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);

			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
				iterator.remove();
				continue;
			}

			if (currentTick > state.expiresTick) {
				iterator.remove();
				continue;
			}
			if (suppressDuringClash && isDomainExpansionEffectSuppressedByClash(state.casterId)) {
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

	static void updateFrostDomainPulseStatusTargets(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, FrostDomainPulseStatusState>> iterator = FROST_DOMAIN_PULSE_STATUS_TARGETS.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, FrostDomainPulseStatusState> entry = iterator.next();
			FrostDomainPulseStatusState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);

			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
				iterator.remove();
				continue;
			}

			boolean slownessActive = currentTick <= state.slownessExpiresTick;
			boolean blindnessActive = currentTick <= state.blindnessExpiresTick;
			if (!slownessActive && !blindnessActive) {
				iterator.remove();
				continue;
			}

			if (!isDomainExpansionEffectSuppressedByClash(state.casterId)) {
				continue;
			}

			removeTrackedFrostDomainStatusEffect(
				target,
				StatusEffects.SLOWNESS,
				state.slownessAmplifier,
				Math.max(0, state.slownessExpiresTick - currentTick)
			);
			removeTrackedFrostDomainStatusEffect(
				target,
				StatusEffects.BLINDNESS,
				state.blindnessAmplifier,
				Math.max(0, state.blindnessExpiresTick - currentTick)
			);
			iterator.remove();
		}
	}

	static boolean isDomainExpansionEffectSuppressedByClash(UUID casterId) {
		return DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS && domainClashStateForParticipant(casterId) != null;
	}

	static void refreshTrackedFrostDomainSlowness(
		LivingEntity target,
		UUID casterId,
		int currentTick,
		int durationTicks,
		int amplifier
	) {
		refreshStatusEffect(target, StatusEffects.SLOWNESS, durationTicks, amplifier, true, true, true);
		StatusEffectInstance existing = target.getStatusEffect(StatusEffects.SLOWNESS);
		if (
			existing == null
			|| existing.getAmplifier() != amplifier
			|| !existing.isAmbient()
			|| !existing.shouldShowParticles()
			|| !existing.shouldShowIcon()
			|| existing.getDuration() > durationTicks + 2
			|| existing.getDuration() < Math.max(0, durationTicks - 2)
		) {
			return;
		}
		FrostDomainPulseStatusState state = getOrCreateFrostDomainPulseStatusState(target, casterId);
		state.slownessAmplifier = amplifier;
		state.slownessExpiresTick = currentTick + durationTicks;
	}

	static void refreshTrackedFrostDomainBlindness(
		LivingEntity target,
		UUID casterId,
		int currentTick,
		int durationTicks
	) {
		refreshStatusEffect(target, StatusEffects.BLINDNESS, durationTicks, 0, true, true, true);
		StatusEffectInstance existing = target.getStatusEffect(StatusEffects.BLINDNESS);
		if (
			existing == null
			|| existing.getAmplifier() != 0
			|| !existing.isAmbient()
			|| !existing.shouldShowParticles()
			|| !existing.shouldShowIcon()
			|| existing.getDuration() > durationTicks + 2
			|| existing.getDuration() < Math.max(0, durationTicks - 2)
		) {
			return;
		}
		FrostDomainPulseStatusState state = getOrCreateFrostDomainPulseStatusState(target, casterId);
		state.blindnessAmplifier = 0;
		state.blindnessExpiresTick = currentTick + durationTicks;
	}

	static FrostDomainPulseStatusState getOrCreateFrostDomainPulseStatusState(LivingEntity target, UUID casterId) {
		UUID targetId = target.getUuid();
		RegistryKey<World> dimension = target.getEntityWorld().getRegistryKey();
		FrostDomainPulseStatusState state = FROST_DOMAIN_PULSE_STATUS_TARGETS.get(targetId);
		if (state == null || state.dimension != dimension || !state.casterId.equals(casterId)) {
			state = new FrostDomainPulseStatusState(dimension, casterId);
			FROST_DOMAIN_PULSE_STATUS_TARGETS.put(targetId, state);
		}
		return state;
	}

	static void removeTrackedFrostDomainStatusEffect(
		LivingEntity target,
		RegistryEntry<StatusEffect> effect,
		int amplifier,
		int expectedRemainingDuration
	) {
		if (amplifier < 0) {
			return;
		}
		StatusEffectInstance existing = target.getStatusEffect(effect);
		if (
			existing == null
			|| existing.getAmplifier() != amplifier
			|| !existing.isAmbient()
			|| !existing.shouldShowParticles()
			|| !existing.shouldShowIcon()
			|| existing.getDuration() > expectedRemainingDuration + 2
		) {
			return;
		}
		target.removeStatusEffect(effect);
	}

	static void applyOrRefreshEnhancedFire(LivingEntity target, UUID casterId, int currentTick, int durationTicks) {
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

	static void updateEnhancedFireTargets(MinecraftServer server, int currentTick) {
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
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
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

	static void applyMartyrsFlameFire(LivingEntity target, UUID casterId, int currentTick) {
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
			MARTYRS_FLAME_BURNING_TARGETS.put(
				target.getUuid(),
				new MartyrsFlameBurnState(
					dimension,
					casterId,
					expiresTick,
					currentTick + Math.max(1, MagicConfig.get().martyrsFlame.fireDamageIntervalTicks),
					Math.max(0.0F, MagicConfig.get().martyrsFlame.fireDamagePerTick),
					Math.max(0.0, MagicConfig.get().martyrsFlame.fireResistantTargetDamageMultiplier),
					MagicConfig.get().martyrsFlame.fireDamageIgnoresFireResistance,
					Math.max(1, MagicConfig.get().martyrsFlame.fireDamageIntervalTicks),
					MagicConfig.get().martyrsFlame.extinguishInWater,
					MagicConfig.get().martyrsFlame.extinguishInRain
				)
			);
		} else {
			state.casterId = casterId;
			state.expiresTick = Math.max(state.expiresTick, expiresTick);
			state.damagePerTick = Math.max(state.damagePerTick, Math.max(0.0F, MagicConfig.get().martyrsFlame.fireDamagePerTick));
			state.fireResistantTargetDamageMultiplier = Math.max(
				state.fireResistantTargetDamageMultiplier,
				Math.max(0.0, MagicConfig.get().martyrsFlame.fireResistantTargetDamageMultiplier)
			);
			state.fireDamageIgnoresFireResistance = state.fireDamageIgnoresFireResistance || MagicConfig.get().martyrsFlame.fireDamageIgnoresFireResistance;
			state.damageIntervalTicks = Math.max(1, MagicConfig.get().martyrsFlame.fireDamageIntervalTicks);
			state.extinguishInWater = MagicConfig.get().martyrsFlame.extinguishInWater;
			state.extinguishInRain = MagicConfig.get().martyrsFlame.extinguishInRain;
		}

		target.setFireTicks(Math.max(target.getFireTicks(), MARTYRS_FLAME_FIRE_DURATION_TICKS));
	}

	static void updateMartyrsFlameBurningTargets(MinecraftServer server, int currentTick) {
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
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
				iterator.remove();
				continue;
			}

			if (isBurningPassionFireExtinguished(target, state.extinguishInWater, state.extinguishInRain)) {
				target.extinguish();
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
			if (state.damagePerTick > 0.0F && currentTick >= state.nextDamageTick) {
				dealBurningPassionFireDamage(
					target,
					state.casterId,
					world,
					state.damagePerTick,
					state.fireResistantTargetDamageMultiplier,
					state.fireDamageIgnoresFireResistance
				);
				state.nextDamageTick = currentTick + state.damageIntervalTicks;
			}
		}
	}

	static void applyFrostbiteControlEffects(LivingEntity target) {
		target.setFrozenTicks(Math.max(target.getFrozenTicks(), FROST_VISUAL_TICKS));
	}

	static void dealFrostbiteDamage(UUID casterId, LivingEntity target, float amount) {
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
			dealTrackedMagicDamage(target, casterId, world.getDamageSources().magic(), amount);
		}
	}

	static void dealAbsoluteZeroAuraDamage(UUID casterId, LivingEntity target) {
		if (target.getEntityWorld() instanceof ServerWorld world) {
			dealTrackedMagicDamage(target, casterId, world.getDamageSources().magic(), ABSOLUTE_ZERO_AURA_DAMAGE);
		}
	}

	static void dealPlanckHeatFrostDamage(UUID casterId, LivingEntity target) {
		if (target.getEntityWorld() instanceof ServerWorld world) {
			dealTrackedMagicDamage(target, casterId, world.getDamageSources().freeze(), PLANCK_HEAT_FROST_DAMAGE);
		}
	}

	static void dealEnhancedFireDamage(UUID casterId, LivingEntity target) {
		if (target.getEntityWorld() instanceof ServerWorld world) {
			dealTrackedMagicDamage(target, casterId, world.getDamageSources().onFire(), PLANCK_HEAT_ENHANCED_FIRE_DAMAGE);
		}
	}

	static void dealTrackedMagicDamage(LivingEntity target, UUID casterId, DamageSource source, float amount) {
		if (!(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		DamageSource resolvedSource = resolveTrackedMagicDamageSource(target, source, world);
		UUID targetId = target.getUuid();
		MAGIC_DAMAGE_PENDING_ATTACKER.put(targetId, casterId);
		try {
			target.damage(world, resolvedSource, amount);
		} finally {
			MAGIC_DAMAGE_PENDING_ATTACKER.remove(targetId, casterId);
		}
	}

	static DamageSource resolveTrackedMagicDamageSource(LivingEntity target, DamageSource source, ServerWorld world) {
		if (!(target instanceof ServerPlayerEntity) || !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return source;
		}
		return world.getDamageSources().magic();
	}

	static void cleanseCommonNegativeEffects(LivingEntity entity) {
		for (var effect : COMMON_NEGATIVE_EFFECTS) {
			entity.removeStatusEffect(effect);
		}
	}

	static void refreshStatusEffect(
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

	static void cleanseAbsoluteZeroNegatives(LivingEntity entity) {
		for (StatusEffectInstance statusEffect : List.copyOf(entity.getStatusEffects())) {
			var effectType = statusEffect.getEffectType();
			boolean harmful = effectType.value().getCategory() == StatusEffectCategory.HARMFUL;
			boolean forceRemove = effectType == StatusEffects.SLOW_FALLING;

			if (harmful || forceRemove) {
				entity.removeStatusEffect(effectType);
			}
		}
	}

	static void spawnCasterAuraParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.WHITE_ASH, x, y, z, 3, 0.30, 0.45, 0.30, 0.004);
	}

	static void playDomainActivationSounds(ServerWorld world, LivingEntity entity) {
		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.playSound(null, x, y, z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.2F, 0.55F);
		world.playSound(null, x, y, z, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.0F, 0.75F);
		world.playSound(null, x, y, z, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.8F, 1.6F);
	}

	static void spawnAbsoluteZeroCasterParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.WHITE_ASH, x, y, z, 6, 0.40, 0.55, 0.40, 0.007);
		world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 6, 0.45, 0.60, 0.45, 0.012);
	}

	static void spawnPlanckHeatFrostCasterParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.WHITE_ASH, x, y, z, 8, 0.55, 0.65, 0.55, 0.012);
		world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 10, 0.60, 0.75, 0.60, 0.02);
	}

	static void spawnPlanckHeatFireCasterParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.55);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 8, 0.55, 0.65, 0.55, 0.02);
	}

	static void spawnMartyrsFlameCasterParticles(LivingEntity entity) {
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

	static void spawnOrionsGambitTargetParticles(LivingEntity target) {
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

	static void spawnTargetSnowParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 3, 0.25, 0.35, 0.25, 0.008);
	}

	static void spawnEnhancedFireTargetParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 4, 0.30, 0.45, 0.30, 0.01);
	}

	static void spawnLoveAtFirstSightTargetParticles(LivingEntity entity) {
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

	static void spawnManipulationTargetParticles(LivingEntity entity) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(ParticleTypes.ENCHANT, x, y, z, 6, 0.28, 0.40, 0.28, 0.02);
		world.spawnParticles(ParticleTypes.PORTAL, x, y, z, 2, 0.20, 0.30, 0.20, 0.03);
	}

	static void spawnHitboxShardParticles(LivingEntity entity, int count) {
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
}


