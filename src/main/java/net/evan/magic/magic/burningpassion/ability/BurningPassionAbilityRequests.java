package net.evan.magic.magic.ability;
import net.evan.magic.magic.core.ability.MagicAbility;
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


abstract class BurningPassionAbilityRequests extends BurningPassionEffectRuntime {
	static void handleMartyrsFlameRequest(ServerPlayerEntity player) {
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

	static void handleIgnitionRequest(ServerPlayerEntity player, int currentTick) {
		BurningPassionIgnitionState activeState = BURNING_PASSION_IGNITION_STATES.get(player.getUuid());
		if (activeAbility(player) == MagicAbility.IGNITION && activeState != null) {
			if (!BURNING_PASSION_CONFIG.ignition.allowManualCancel) {
				player.sendMessage(Text.translatable("message.magic.burning_passion.ignition.cancel_disabled"), true);
				return;
			}
			endIgnition(
				player,
				currentTick,
				BurningPassionIgnitionEndReason.MANUAL,
				BURNING_PASSION_CONFIG.ignition.manualCancelStartsCooldown,
				true
			);
			return;
		}

		int remainingTicks = ignitionCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.IGNITION, remainingTicks, false);
			return;
		}

		if (!isOrionsGambitManaCostSuppressed(player) && MagicPlayerData.getMana(player) <= 0) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		BURNING_PASSION_IGNITION_STATES.put(
			player.getUuid(),
			new BurningPassionIgnitionState(player.getEntityWorld().getRegistryKey(), 1, currentTick, 0.0)
		);
		setActiveAbility(player, MagicAbility.IGNITION);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		IGNITION_COOLDOWN_END_TICK.remove(player.getUuid());
		spawnIgnitionActivationEffects(player);
		syncBurningPassionHud(player);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.IGNITION.displayName()), true);
		recordOrionsGambitAbilityUse(player, MagicAbility.IGNITION);
	}

	static void handleSearingDashRequest(ServerPlayerEntity player, int currentTick) {
		BurningPassionIgnitionState ignitionState = burningPassionIgnitionState(player);
		if (ignitionState == null || ignitionState.currentStage != 1) {
			player.sendMessage(Text.translatable("message.magic.burning_passion.ignition.required"), true);
			return;
		}

		int remainingTicks = searingDashCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.SEARING_DASH, remainingTicks, false);
			return;
		}
		if (!canPayBurningPassionHealthDemand(player, ignitionState, MagicAbility.SEARING_DASH)) {
			return;
		}
		if (!(player.getEntityWorld() instanceof ServerWorld world) || BURNING_PASSION_SEARING_DASH_STATES.containsKey(player.getUuid())) {
			return;
		}
		if (!tryPayBurningPassionManaCost(player, ignitionState, MagicAbility.SEARING_DASH, BURNING_PASSION_CONFIG.searingDash.manaCostPercent)) {
			return;
		}
		payBurningPassionHealthDemand(player, ignitionState, MagicAbility.SEARING_DASH);
		ignitionState.heatPercent = clampBurningPassionHeat(ignitionState.heatPercent + BURNING_PASSION_CONFIG.searingDash.heatGainPercent);
		BurningPassionTrailLineState trailState = new BurningPassionTrailLineState(
			player.getUuid(),
			world.getRegistryKey(),
			currentTick + BURNING_PASSION_CONFIG.searingDash.trailDurationTicks
		);
		trailState.points.add(new Vec3d(player.getX(), player.getBodyY(0.05), player.getZ()));
		BURNING_PASSION_TRAIL_LINES.add(trailState);
		BURNING_PASSION_SEARING_DASH_STATES.put(
			player.getUuid(),
			new BurningPassionSearingDashState(
				world.getRegistryKey(),
				normalizedHorizontalDirection(player.getRotationVector(), player),
				currentTick + BURNING_PASSION_CONFIG.searingDash.dashTicks,
				trailState,
				new Vec3d(player.getX(), player.getBodyY(0.05), player.getZ())
			)
		);
		if (BURNING_PASSION_CONFIG.searingDash.cooldownTicks > 0) {
			startAbilityCooldownFromNow(player.getUuid(), MagicAbility.SEARING_DASH, currentTick);
		}
		spawnSearingDashStartEffects(world, player);
		syncBurningPassionHud(player);
		recordOrionsGambitAbilityUse(player, MagicAbility.SEARING_DASH);
	}

	static void handleCinderMarkRequest(ServerPlayerEntity player, int currentTick, boolean detonateInsteadOfReady) {
		BurningPassionIgnitionState ignitionState = burningPassionIgnitionState(player);
		if (ignitionState == null || ignitionState.currentStage < 2) {
			player.sendMessage(Text.translatable("message.magic.burning_passion.cinder_mark.stage_required"), true);
			return;
		}
		if (!canPayBurningPassionHealthDemand(player, ignitionState, MagicAbility.CINDER_MARK)) {
			return;
		}
		if (detonateInsteadOfReady) {
			int manualDetonationCount = countBurningPassionCinderMarks(player.getUuid());
			if (manualDetonationCount <= 0) {
				return;
			}
			if (
				!tryPayBurningPassionManaCost(
					player,
					ignitionState,
					MagicAbility.CINDER_MARK,
					BURNING_PASSION_CONFIG.cinderMark.manualDetonationManaCostPercentPerMark * manualDetonationCount
				)
			) {
				return;
			}
			detonateAllCinderMarks(player, currentTick, BURNING_PASSION_CONFIG.cinderMark.manualDetonationTrueDamage);
			syncBurningPassionHud(player);
			return;
		}

		int remainingTicks = cinderMarkCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.CINDER_MARK, remainingTicks, false);
			return;
		}

		RegistryKey<World> dimension = player.getEntityWorld().getRegistryKey();
		int expiresTick = BURNING_PASSION_CONFIG.cinderMark.readyTimeoutTicks <= 0
			? Integer.MAX_VALUE
			: currentTick + BURNING_PASSION_CONFIG.cinderMark.readyTimeoutTicks;
		BURNING_PASSION_CINDER_MARK_ARMED_STATES.put(player.getUuid(), new BurningPassionCinderMarkArmedState(dimension, expiresTick));
		player.sendMessage(Text.translatable("message.magic.burning_passion.cinder_mark.readied"), true);
	}

	static void handleOverrideRequest(ServerPlayerEntity player, int currentTick) {
		tryStartOverride(player, currentTick, false);
	}

	static boolean tryStartOverride(ServerPlayerEntity player, int currentTick, boolean bypassDomainRequirement) {
		if (OVERRIDE_METEOR_STATES.containsKey(player.getUuid())) {
			player.sendMessage(Text.translatable("message.magic.burning_passion.override.already_active"), true);
			return false;
		}

		int remainingTicks = overrideCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.OVERRIDE, remainingTicks, false);
			return false;
		}
		if (BURNING_PASSION_CONFIG.override.requiresFullMana && !hasFullManaBar(player)) {
			player.sendMessage(Text.translatable("message.magic.burning_passion.full_mana_required", MagicAbility.OVERRIDE.displayName()), true);
			return false;
		}

		BurningPassionIgnitionState ignitionState = burningPassionIgnitionState(player);
		if (!canPayBurningPassionHealthDemand(player, ignitionState, MagicAbility.OVERRIDE)) {
			return false;
		}
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return false;
		}

		Vec3d impactCenter = resolveOverrideImpactCenter(world, player);
		double impactRadius = burningPassionOverrideImpactRadius();
		List<UUID> qualifyingDomains = qualifyingOverrideDomains(player, impactCenter, impactRadius);
		if (!bypassDomainRequirement && qualifyingDomains.isEmpty()) {
			player.sendMessage(Text.translatable("message.magic.burning_passion.override.no_valid_domain"), true);
			return false;
		}

		if (BURNING_PASSION_CONFIG.override.consumeFullMana) {
			consumeFullManaBar(player);
		}
		payBurningPassionHealthDemand(player, ignitionState, MagicAbility.OVERRIDE);
		startAbilityCooldownFromNow(player.getUuid(), MagicAbility.OVERRIDE, currentTick);

		OverrideMeteorState meteorState = createOverrideMeteorState(player, world, impactCenter, impactRadius, qualifyingDomains, currentTick);
		if (meteorState == null) {
			OVERRIDE_COOLDOWN_END_TICK.remove(player.getUuid());
			player.sendMessage(Text.translatable("message.magic.burning_passion.override.cast_failed"), true);
			return false;
		}

		OVERRIDE_METEOR_STATES.put(player.getUuid(), meteorState);
		recordOrionsGambitAbilityUse(player, MagicAbility.OVERRIDE);
		return true;
	}

	static void applyIgnition(ServerPlayerEntity player, int currentTick) {
		BurningPassionIgnitionState state = BURNING_PASSION_IGNITION_STATES.get(player.getUuid());
		if (state == null || state.dimension != player.getEntityWorld().getRegistryKey()) {
			endIgnition(player, currentTick, BurningPassionIgnitionEndReason.INVALID, false, false);
			return;
		}
		if (isLockedBurningPassionStage(player, state.currentStage)) {
			endIgnition(player, currentTick, BurningPassionIgnitionEndReason.LOCKED, false, false);
			return;
		}

		int previousStage = state.currentStage;
		while (true) {
			int stageDuration = burningPassionStageDurationTicks(state.currentStage);
			if (currentTick - state.stageStartTick < stageDuration) {
				break;
			}
			if (state.currentStage >= 3) {
				endIgnition(
					player,
					currentTick,
					BurningPassionIgnitionEndReason.NATURAL,
					BURNING_PASSION_CONFIG.ignition.naturalEndStartsCooldown,
					true
				);
				return;
			}
			if (!MagicConfig.get().abilityAccess.isStageProgressionUnlocked(player.getUuid(), MagicSchool.BURNING_PASSION, state.currentStage + 1)) {
				endIgnition(player, currentTick, BurningPassionIgnitionEndReason.LOCKED, false, false);
				return;
			}
			state.stageStartTick += stageDuration;
			state.currentStage++;
		}

		MagicConfig.BurningPassionStageConfig stageConfig = burningPassionStageConfig(state.currentStage);
		if (previousStage != state.currentStage || !stageConfig.auraEnabled) {
			state.auraPlayersInside.clear();
		}

		applyBurningPassionStageBuffs(player, stageConfig);
		if (state.currentStage < 2) {
			BURNING_PASSION_CINDER_MARK_ARMED_STATES.remove(player.getUuid());
		}
		applyBurningPassionEngineHeartState(player, state, currentTick);
		if (!BURNING_PASSION_IGNITION_STATES.containsKey(player.getUuid())) {
			return;
		}

		if (stageConfig.auraEnabled && stageConfig.auraRadius > 0.0) {
			applyBurningPassionAura(player, state, stageConfig, currentTick);
		} else {
			state.auraPlayersInside.clear();
		}

		double passiveHeatPerTick = stageConfig.passiveHeatPerSecond / TICKS_PER_SECOND;
		if (Math.abs(passiveHeatPerTick) > 1.0E-6) {
			state.heatPercent = MathHelper.clamp(
				state.heatPercent + passiveHeatPerTick,
				0.0,
				BURNING_PASSION_CONFIG.heat.overheatThresholdPercent
			);
		}
		if (stageConfig.waterHeatPerSecond != 0.0 && isBurningPassionCoolingInWater(player)) {
			state.heatPercent = MathHelper.clamp(
				state.heatPercent + stageConfig.waterHeatPerSecond / TICKS_PER_SECOND,
				0.0,
				BURNING_PASSION_CONFIG.heat.overheatThresholdPercent
			);
		}

		if (state.heatPercent >= BURNING_PASSION_CONFIG.heat.selfFireThresholdPercent) {
			applyOrRefreshBurningPassionSelfFire(player, state.currentStage, currentTick);
		} else if (BURNING_PASSION_SELF_FIRE_TARGETS.remove(player.getUuid()) != null) {
			player.extinguish();
		}

		spawnBurningPassionStageParticles(player, stageConfig, state.currentStage);
		syncBurningPassionHud(player);
		if (state.heatPercent >= BURNING_PASSION_CONFIG.heat.overheatThresholdPercent) {
			triggerBurningPassionOverheat(player, currentTick);
		}
	}

	static void endIgnition(
		ServerPlayerEntity player,
		int currentTick,
		BurningPassionIgnitionEndReason reason,
		boolean startCooldown,
		boolean sendDeactivatedMessage
	) {
		UUID playerId = player.getUuid();
		BurningPassionIgnitionState existingState = BURNING_PASSION_IGNITION_STATES.get(playerId);
		boolean punishStageThreeExit = existingState != null
			&& existingState.currentStage == 3
			&& (
				reason == BurningPassionIgnitionEndReason.MANUAL
					|| reason == BurningPassionIgnitionEndReason.NATURAL
					|| reason == BurningPassionIgnitionEndReason.LOCKED
					|| (reason == BurningPassionIgnitionEndReason.INVALID && startCooldown)
			);
		BURNING_PASSION_IGNITION_STATES.remove(playerId);
		clearBurningPassionSearingDashState(playerId);
		BURNING_PASSION_CINDER_MARK_ARMED_STATES.remove(playerId);
		clearBurningPassionAuraFireByCaster(playerId);
		clearBurningPassionEngineHeartState(player);
		BURNING_PASSION_PENDING_MELEE_IMPACTS.remove(playerId);
		if (BURNING_PASSION_SELF_FIRE_TARGETS.remove(playerId) != null) {
			player.extinguish();
		}
		removeBurningPassionStageBuffs(player);
		if (activeAbility(player) == MagicAbility.IGNITION) {
			setActiveAbility(player, MagicAbility.NONE);
		}
		if (punishStageThreeExit) {
			startBurningPassionCooldown(
				playerId,
				MagicAbility.IGNITION,
				currentTick,
				BURNING_PASSION_CONFIG.ignition.stageThreePunishmentCooldownTicks,
				IGNITION_COOLDOWN_END_TICK
			);
			if (BURNING_PASSION_CONFIG.ignition.stageThreePunishmentUsesOverheatEffects) {
				applyBurningPassionOverheatPunishmentEffects(player);
			}
		} else if (startCooldown) {
			startAbilityCooldownFromNow(playerId, MagicAbility.IGNITION, currentTick);
		}
		MagicPlayerData.clearBurningPassionHud(player);
		if (reason == BurningPassionIgnitionEndReason.MANUAL && player.getEntityWorld() instanceof ServerWorld world) {
			playConfiguredSound(
				world,
				new Vec3d(player.getX(), player.getBodyY(0.45), player.getZ()),
				SoundEvents.BLOCK_FIRE_EXTINGUISH,
				BURNING_PASSION_CONFIG.ignition.cancelSoundVolume,
				BURNING_PASSION_CONFIG.ignition.cancelSoundPitch
			);
		}
		if (reason == BurningPassionIgnitionEndReason.OVERHEAT) {
			player.sendMessage(Text.translatable("message.magic.burning_passion.overheat"), true);
		} else if (sendDeactivatedMessage) {
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.IGNITION.displayName()), true);
		}
	}

	static void applyBurningPassionAura(
		ServerPlayerEntity player,
		BurningPassionIgnitionState state,
		MagicConfig.BurningPassionStageConfig stageConfig,
		int currentTick
	) {
		double radius = Math.max(0.0, stageConfig.auraRadius);
		double radiusSq = radius * radius;
		Set<UUID> seenPlayers = new HashSet<>();
		MinecraftServer server = player.getEntityWorld().getServer();
		for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
			if (other == player || other.isSpectator() || other.getEntityWorld().getRegistryKey() != state.dimension) {
				continue;
			}

			UUID targetId = other.getUuid();
			seenPlayers.add(targetId);
			boolean inside = player.squaredDistanceTo(other) <= radiusSq;
			boolean previouslyInside = state.auraPlayersInside.getOrDefault(targetId, false);
			if (inside != previouslyInside) {
				int cooldownEndTick = state.boundaryCooldownEndTickByTarget.getOrDefault(targetId, Integer.MIN_VALUE);
				if (currentTick >= cooldownEndTick && stageConfig.boundaryHeatGainPercent > 0.0) {
					state.heatPercent = Math.min(
						BURNING_PASSION_CONFIG.heat.overheatThresholdPercent,
						state.heatPercent + stageConfig.boundaryHeatGainPercent
					);
					state.boundaryCooldownEndTickByTarget.put(
						targetId,
						currentTick + Math.max(0, stageConfig.boundaryTriggerCooldownTicks)
					);
				}
				state.auraPlayersInside.put(targetId, inside);
			}
			if (!inside) {
				continue;
			}

			applyOrRefreshBurningPassionAuraFire(
				other,
				player.getUuid(),
				currentTick,
				stageConfig.fireDamagePerTick,
				stageConfig.fireResistantTargetDamageMultiplier,
				stageConfig.fireDamageIgnoresFireResistance,
				stageConfig.fireDamageIntervalTicks,
				stageConfig.fireRefreshTicks,
				stageConfig.persistentFireUntilExtinguished,
				stageConfig.extinguishInWater,
				stageConfig.extinguishInRain
			);
		}

		state.auraPlayersInside.keySet().removeIf(targetId -> !seenPlayers.contains(targetId));
		state.boundaryCooldownEndTickByTarget.keySet().removeIf(targetId -> !seenPlayers.contains(targetId));
	}

	static void applyBurningPassionStageBuffs(ServerPlayerEntity player, MagicConfig.BurningPassionStageConfig stageConfig) {
		int refreshTicks = Math.max(1, BURNING_PASSION_CONFIG.ignition.effectRefreshTicks);
		if (stageConfig.speedAmplifier >= 0) {
			refreshStatusEffect(player, StatusEffects.SPEED, refreshTicks, stageConfig.speedAmplifier, true, false, true);
		}
		if (stageConfig.strengthAmplifier >= 0) {
			refreshStatusEffect(player, StatusEffects.STRENGTH, refreshTicks, stageConfig.strengthAmplifier, true, false, true);
		}
		if (stageConfig.regenerationAmplifier >= 0) {
			refreshStatusEffect(player, StatusEffects.REGENERATION, refreshTicks, stageConfig.regenerationAmplifier, true, false, true);
		}
		overwriteAttributeModifier(
			player.getAttributeInstance(EntityAttributes.STEP_HEIGHT),
			BURNING_PASSION_STEP_HEIGHT_MODIFIER_ID,
			stageConfig.extraStepHeightBlocks,
			EntityAttributeModifier.Operation.ADD_VALUE
		);
	}

	static void removeBurningPassionStageBuffs(ServerPlayerEntity player) {
		removeStatusEffectIfMatching(player, StatusEffects.SPEED, burningPassionStageAmplifiers(StatusEffects.SPEED));
		removeStatusEffectIfMatching(player, StatusEffects.STRENGTH, burningPassionStageAmplifiers(StatusEffects.STRENGTH));
		removeStatusEffectIfMatching(player, StatusEffects.REGENERATION, burningPassionStageAmplifiers(StatusEffects.REGENERATION));
		removeAttributeModifier(player.getAttributeInstance(EntityAttributes.STEP_HEIGHT), BURNING_PASSION_STEP_HEIGHT_MODIFIER_ID);
	}

	static Set<Integer> burningPassionStageAmplifiers(RegistryEntry<StatusEffect> effect) {
		Set<Integer> amplifiers = new HashSet<>();
		if (effect == StatusEffects.SPEED) {
			if (BURNING_PASSION_CONFIG.stageOne.speedAmplifier >= 0) {
				amplifiers.add(BURNING_PASSION_CONFIG.stageOne.speedAmplifier);
			}
			if (BURNING_PASSION_CONFIG.stageTwo.speedAmplifier >= 0) {
				amplifiers.add(BURNING_PASSION_CONFIG.stageTwo.speedAmplifier);
			}
			if (BURNING_PASSION_CONFIG.stageThree.speedAmplifier >= 0) {
				amplifiers.add(BURNING_PASSION_CONFIG.stageThree.speedAmplifier);
			}
			return amplifiers;
		}
		if (effect == StatusEffects.STRENGTH) {
			if (BURNING_PASSION_CONFIG.stageOne.strengthAmplifier >= 0) {
				amplifiers.add(BURNING_PASSION_CONFIG.stageOne.strengthAmplifier);
			}
			if (BURNING_PASSION_CONFIG.stageTwo.strengthAmplifier >= 0) {
				amplifiers.add(BURNING_PASSION_CONFIG.stageTwo.strengthAmplifier);
			}
			if (BURNING_PASSION_CONFIG.stageThree.strengthAmplifier >= 0) {
				amplifiers.add(BURNING_PASSION_CONFIG.stageThree.strengthAmplifier);
			}
			return amplifiers;
		}
		if (effect == StatusEffects.REGENERATION) {
			if (BURNING_PASSION_CONFIG.stageOne.regenerationAmplifier >= 0) {
				amplifiers.add(BURNING_PASSION_CONFIG.stageOne.regenerationAmplifier);
			}
			if (BURNING_PASSION_CONFIG.stageTwo.regenerationAmplifier >= 0) {
				amplifiers.add(BURNING_PASSION_CONFIG.stageTwo.regenerationAmplifier);
			}
			if (BURNING_PASSION_CONFIG.stageThree.regenerationAmplifier >= 0) {
				amplifiers.add(BURNING_PASSION_CONFIG.stageThree.regenerationAmplifier);
			}
		}
		return amplifiers;
	}

	static void triggerBurningPassionOverheat(ServerPlayerEntity player, int currentTick) {
		applyBurningPassionOverheatPunishmentEffects(player);
		endIgnition(player, currentTick, BurningPassionIgnitionEndReason.OVERHEAT, false, false);
		startBurningPassionOverheatCooldowns(player.getUuid(), currentTick);
	}

	static void startBurningPassionOverheatCooldowns(UUID playerId, int currentTick) {
		int lockoutTicks = Math.max(0, BURNING_PASSION_CONFIG.overheat.stagedAbilityCooldownTicks);
		startBurningPassionCooldown(playerId, MagicAbility.IGNITION, currentTick, lockoutTicks, IGNITION_COOLDOWN_END_TICK);
		startBurningPassionCooldown(playerId, MagicAbility.SEARING_DASH, currentTick, lockoutTicks, SEARING_DASH_COOLDOWN_END_TICK);
		startBurningPassionCooldown(playerId, MagicAbility.CINDER_MARK, currentTick, lockoutTicks, CINDER_MARK_COOLDOWN_END_TICK);
		startBurningPassionCooldown(playerId, MagicAbility.ENGINE_HEART, currentTick, lockoutTicks, ENGINE_HEART_COOLDOWN_END_TICK);
		if (BURNING_PASSION_CONFIG.overheat.includeOverrideInLockout) {
			startBurningPassionCooldown(playerId, MagicAbility.OVERRIDE, currentTick, lockoutTicks, OVERRIDE_COOLDOWN_END_TICK);
		}
	}

	static void startBurningPassionCooldown(
		UUID playerId,
		MagicAbility ability,
		int currentTick,
		int cooldownTicks,
		Map<UUID, Integer> cooldownMap
	) {
		int safeCooldownTicks = Math.max(0, cooldownTicks);
		if (isCooldownDeferredByOrionsGambit(playerId, ability)) {
			trackOrionsGambitCooldownOverride(playerId, ability, safeCooldownTicks);
			cooldownMap.remove(playerId);
			return;
		}
		if (safeCooldownTicks <= 0) {
			cooldownMap.remove(playerId);
			return;
		}
		cooldownMap.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, safeCooldownTicks, currentTick));
	}

	static BurningPassionIgnitionState burningPassionIgnitionState(ServerPlayerEntity player) {
		BurningPassionIgnitionState state = BURNING_PASSION_IGNITION_STATES.get(player.getUuid());
		if (state == null) {
			return null;
		}
		return activeAbility(player) == MagicAbility.IGNITION && state.dimension == player.getEntityWorld().getRegistryKey() ? state : null;
	}

	static MagicConfig.BurningPassionStageConfig burningPassionStageConfig(int stage) {
		return switch (stage) {
			case 1 -> BURNING_PASSION_CONFIG.stageOne;
			case 2 -> BURNING_PASSION_CONFIG.stageTwo;
			default -> BURNING_PASSION_CONFIG.stageThree;
		};
	}

	static int burningPassionStageDurationTicks(int stage) {
		return switch (stage) {
			case 1 -> Math.max(1, BURNING_PASSION_CONFIG.ignition.stageOneDurationTicks);
			case 2 -> Math.max(1, BURNING_PASSION_CONFIG.ignition.stageTwoDurationTicks);
			default -> Math.max(1, BURNING_PASSION_CONFIG.ignition.stageThreeDurationTicks);
		};
	}

	static void syncBurningPassionHud(ServerPlayerEntity player) {
		BurningPassionIgnitionState state = burningPassionIgnitionState(player);
		if (state == null) {
			MagicPlayerData.setBurningPassionEngineHeartAfterimageState(player, false, 0);
			MagicPlayerData.clearBurningPassionHud(player);
			return;
		}

		int currentTick = player.getEntityWorld().getServer().getTicks();
		int stageDuration = burningPassionStageDurationTicks(state.currentStage);
		int remainingTicks = Math.max(0, stageDuration - Math.max(0, currentTick - state.stageStartTick));
		int heatTenths = (int) Math.round(MathHelper.clamp(state.heatPercent, 0.0, 100.0) * 10.0);
		MagicPlayerData.setBurningPassionHud(player, true, state.currentStage, remainingTicks, heatTenths / 10.0);
		syncBurningPassionHudNotification(player, currentTick);
	}

	static boolean shouldKeepIgnitionActiveOnManaDepletion() {
		return BURNING_PASSION_CONFIG.heat.ignitionPersistsOnManaDepletion;
	}

	static double burningPassionManaRecoveryUnlockThresholdPercent() {
		return MathHelper.clamp(
			BURNING_PASSION_CONFIG.heat.manaRecoveryUnlockThresholdPercent,
			0.0,
			BURNING_PASSION_CONFIG.heat.overheatThresholdPercent
		);
	}

	static void markBurningPassionManaDepleted(ServerPlayerEntity player, BurningPassionIgnitionState ignitionState) {
		if (player == null || ignitionState == null) {
			return;
		}

		ignitionState.manaRecoveryUnlocked = ignitionState.heatPercent <= burningPassionManaRecoveryUnlockThresholdPercent();
		MagicPlayerData.setDepletedRecoveryMode(player, true);
	}

	static boolean isBurningPassionManaRecoveryBlocked(ServerPlayerEntity player) {
		if (player == null || !shouldKeepIgnitionActiveOnManaDepletion() || MagicPlayerData.getMana(player) > 0) {
			return false;
		}

		BurningPassionIgnitionState ignitionState = burningPassionIgnitionState(player);
		if (ignitionState == null) {
			return false;
		}

		if (ignitionState.manaRecoveryUnlocked) {
			return false;
		}

		if (ignitionState.heatPercent <= burningPassionManaRecoveryUnlockThresholdPercent()) {
			ignitionState.manaRecoveryUnlocked = true;
			return false;
		}

		return true;
	}

	static double clampBurningPassionHeat(double heatPercent) {
		return MathHelper.clamp(heatPercent, 0.0, BURNING_PASSION_CONFIG.heat.overheatThresholdPercent);
	}

	static boolean tryPayBurningPassionManaCost(
		ServerPlayerEntity player,
		BurningPassionIgnitionState ignitionState,
		MagicAbility ability,
		double manaCostPercent
	) {
		int adjustedManaCost = Math.max(
			0,
			GreedDomainRuntime.adjustManaCost(player, (int) Math.round(MathHelper.clamp(manaCostPercent, 0.0, 100.0)))
		);
		if (isTestingMode(player)) {
			MagicPlayerData.setMana(player, MagicPlayerData.MAX_MANA);
			MagicPlayerData.setDepletedRecoveryMode(player, false);
			return true;
		}
		if (isOrionsGambitManaCostSuppressed(player) || adjustedManaCost <= 0) {
			MagicPlayerData.setDepletedRecoveryMode(player, false);
			return true;
		}

		int currentMana = Math.max(0, MagicPlayerData.getMana(player));
		if (currentMana >= adjustedManaCost) {
			MagicPlayerData.setMana(player, currentMana - adjustedManaCost);
			MagicPlayerData.setDepletedRecoveryMode(player, false);
			return true;
		}

		MagicConfig.BurningPassionManaOverflowConfig overflowConfig = BURNING_PASSION_CONFIG.manaOverflow;
		boolean usingZeroManaOverflow = currentMana <= 0;
		boolean usingPartialOverflow = currentMana > 0 && currentMana < adjustedManaCost;
		if (
			ignitionState == null
			|| !overflowConfig.enabled
			|| (usingZeroManaOverflow && !overflowConfig.allowZeroManaSubstitution)
			|| (usingPartialOverflow && !overflowConfig.allowPartialSubstitution)
		) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return false;
		}

		int missingMana = Math.max(0, adjustedManaCost - currentMana);
		MagicPlayerData.setMana(player, 0);
		if (missingMana > 0) {
			ignitionState.heatPercent = clampBurningPassionHeat(
				ignitionState.heatPercent + missingMana * overflowConfig.heatPercentPerMissingManaPercent
			);
		}
		markBurningPassionManaDepleted(player, ignitionState);
		return true;
	}

	static void showBurningPassionHudNotification(
		ServerPlayerEntity player,
		String text,
		int currentTick,
		int durationTicks
	) {
		if (player == null) {
			return;
		}

		String resolvedText = text == null ? "" : text;
		if (resolvedText.isBlank() || durationTicks <= 0) {
			BURNING_PASSION_HUD_NOTIFICATION_STATES.remove(player.getUuid());
			MagicPlayerData.setBurningPassionHudNotification(player, "");
			return;
		}

		BURNING_PASSION_HUD_NOTIFICATION_STATES.put(
			player.getUuid(),
			new BurningPassionHudNotificationState(resolvedText, currentTick + Math.max(1, durationTicks))
		);
		MagicPlayerData.setBurningPassionHudNotification(player, resolvedText);
	}

	static void syncBurningPassionHudNotification(ServerPlayerEntity player, int currentTick) {
		BurningPassionHudNotificationState state = BURNING_PASSION_HUD_NOTIFICATION_STATES.get(player.getUuid());
		if (state == null || currentTick > state.expiresTick) {
			BURNING_PASSION_HUD_NOTIFICATION_STATES.remove(player.getUuid());
			MagicPlayerData.setBurningPassionHudNotification(player, "");
			return;
		}
		MagicPlayerData.setBurningPassionHudNotification(player, state.text);
	}

	static void cleanupBurningPassionTransientStates(MinecraftServer server, int currentTick) {
		BURNING_PASSION_CINDER_MARK_ARMED_STATES.entrySet().removeIf(entry ->
			entry.getValue().expiresTick != Integer.MAX_VALUE && currentTick > entry.getValue().expiresTick
		);

		Iterator<Map.Entry<UUID, BurningPassionHudNotificationState>> notificationIterator = BURNING_PASSION_HUD_NOTIFICATION_STATES.entrySet().iterator();
		while (notificationIterator.hasNext()) {
			Map.Entry<UUID, BurningPassionHudNotificationState> entry = notificationIterator.next();
			if (currentTick <= entry.getValue().expiresTick) {
				continue;
			}
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			if (player != null) {
				MagicPlayerData.setBurningPassionHudNotification(player, "");
			}
			notificationIterator.remove();
		}

		BURNING_PASSION_PENDING_MELEE_IMPACTS.entrySet().removeIf(entry -> currentTick > entry.getValue().expiresTick);
	}

	static void applyBurningPassionOverheatPunishmentEffects(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}

		int durationTicks = Math.max(0, BURNING_PASSION_CONFIG.overheat.punishmentDurationTicks);
		if (durationTicks > 0) {
			if (BURNING_PASSION_CONFIG.overheat.slownessAmplifier >= 0) {
				player.addStatusEffect(
					new StatusEffectInstance(StatusEffects.SLOWNESS, durationTicks, BURNING_PASSION_CONFIG.overheat.slownessAmplifier, true, false, true)
				);
			}
			if (BURNING_PASSION_CONFIG.overheat.weaknessAmplifier >= 0) {
				player.addStatusEffect(
					new StatusEffectInstance(StatusEffects.WEAKNESS, durationTicks, BURNING_PASSION_CONFIG.overheat.weaknessAmplifier, true, false, true)
				);
			}
		}
		if (player.getEntityWorld() instanceof ServerWorld world) {
			Vec3d center = new Vec3d(player.getX(), player.getBodyY(0.45), player.getZ());
			world.spawnParticles(
				ParticleTypes.FLAME,
				center.x,
				center.y,
				center.z,
				Math.max(0, BURNING_PASSION_CONFIG.overheat.flameBurstParticleCount),
				0.35,
				0.35,
				0.35,
				0.06
			);
			world.spawnParticles(
				ParticleTypes.CAMPFIRE_COSY_SMOKE,
				center.x,
				center.y,
				center.z,
				Math.max(0, BURNING_PASSION_CONFIG.overheat.smokeParticleCount),
				0.3,
				0.3,
				0.3,
				0.01
			);
			playConfiguredSound(
				world,
				center,
				SoundEvents.BLOCK_FIRE_EXTINGUISH,
				BURNING_PASSION_CONFIG.overheat.soundVolume,
				BURNING_PASSION_CONFIG.overheat.soundPitch
			);
		}
	}

	static boolean canPayBurningPassionHealthDemand(
		ServerPlayerEntity player,
		BurningPassionIgnitionState ignitionState,
		MagicAbility ability
	) {
		return canPayBurningPassionHealthDemand(player, ignitionState, ability, true);
	}

	static boolean canPayBurningPassionHealthDemand(
		ServerPlayerEntity player,
		BurningPassionIgnitionState ignitionState,
		MagicAbility ability,
		boolean sendFailureMessage
	) {
		double threshold = BURNING_PASSION_CONFIG.heat.healthDemandThresholdPercent;
		if (ignitionState == null || ignitionState.heatPercent < threshold || isTestingMode(player)) {
			return true;
		}

		double healthCostHearts = burningPassionHealthCostHearts(ability);
		if (healthCostHearts <= 0.0) {
			return true;
		}
		double currentHealthHearts = player.getHealth() / 2.0;
		double remainingHealthHearts = currentHealthHearts - healthCostHearts;
		if (remainingHealthHearts + 1.0E-6 < BURNING_PASSION_CONFIG.heat.minimumRemainingHealthHearts) {
			if (sendFailureMessage) {
				player.sendMessage(Text.translatable("message.magic.burning_passion.health_too_low"), true);
			}
			return false;
		}
		return true;
	}

	static void payBurningPassionHealthDemand(
		ServerPlayerEntity player,
		BurningPassionIgnitionState ignitionState,
		MagicAbility ability
	) {
		double threshold = BURNING_PASSION_CONFIG.heat.healthDemandThresholdPercent;
		if (ignitionState == null || ignitionState.heatPercent < threshold || isTestingMode(player)) {
			return;
		}

		double healthCostHearts = burningPassionHealthCostHearts(ability);
		if (healthCostHearts <= 0.0) {
			return;
		}
		player.setHealth((float) Math.max(1.0, player.getHealth() - healthCostHearts * 2.0));
	}
}


