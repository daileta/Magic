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


abstract class MagicAbilityManagerSupportServerTick extends MagicAbilityManagerSupportServerTickEffects {
	static void onEndServerTick(MinecraftServer server) {
		int currentTick = server.getTicks();
		Map<UUID, Set<UUID>> absoluteZeroAuraSeenThisTick = new HashMap<>();

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			applyPassiveAbilityEffects(player, currentTick);
			applyActiveAbilityEffects(player, currentTick, absoluteZeroAuraSeenThisTick);
		}

		updateLoveLockedTargets(server, currentTick);
		updateHerculesBurdenStates(server, currentTick);
		updateHerculesBurdenTargets(server, currentTick);
		updateSagittariusWindups(server, currentTick);
		updateCelestialDelayedActions(server, currentTick);
		updateOrionsGambitStates(server, currentTick);
		updateOrionsGambitPenalties(server, currentTick);
		updateAstralCataclysmDomainStates(server, currentTick);
		updateAstralExecutionBeams(server, currentTick);
		cleanupManipulationStates(server);
		syncManipulationSuppressionTags(server);
		cleanupPlanckHeatStates(server);
		trimAbsoluteZeroAuraTimers(absoluteZeroAuraSeenThisTick);
		updateFrostSpikeWaves(server, currentTick);
		updateFrostbittenTargets(server, currentTick);
		updateFrostFrozenTargets(server, currentTick);
		updateFrostDomainPulseStatusTargets(server, currentTick);
		updateFrostMaximumFearTargets(server, currentTick);
		updateFrostPackedIceTargets(server, currentTick);
		updateFrostHelplessTargets(server, currentTick);
		updateEnhancedFireTargets(server, currentTick);
		updateMartyrsFlameBurningTargets(server, currentTick);
		cleanupBurningPassionTransientStates(server, currentTick);
		updateBurningPassionAuraFireTargets(server, currentTick);
		updateBurningPassionSearingDashStates(server, currentTick);
		updateBurningPassionTrailLines(server, currentTick);
		updateBurningPassionCinderMarks(server, currentTick);
		updateOverrideMeteors(server, currentTick);
		updateComedicAssistantCarries(server, currentTick);
		updateDomainClashes(server, currentTick);
		updateDomainExpansions(server, currentTick);
		syncDomainTimerOverlays(server, currentTick);
		updateComedicRewriteVisualCameos(server, currentTick);
		updateComedicAssistantAcmeVisuals(server, currentTick);
		updateComedicAssistantCaneImpactStates(server, currentTick);
		updatePlusUltraImpactStates(server, currentTick);

		if (currentTick % TICKS_PER_SECOND == 0) {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				updateManaOncePerSecond(player);
			}
		}
		syncStageProgressionHuds(server);
		GreedRuntime.onEndServerTick(server, currentTick);
		GreedDomainRuntime.onEndServerTick(server, currentTick);
		enforceOrionsGambitGreedTargetCoins(server);
	}

	static void syncStageProgressionHuds(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			syncFrostStageHud(player);
			syncBurningPassionHud(player);
		}
	}

	static void updateComedicRewriteVisualCameos(MinecraftServer server, int currentTick) {
		if (COMEDIC_REWRITE_VISUAL_CAMEOS.isEmpty()) {
			return;
		}

		Iterator<ComedicRewriteVisualCameo> iterator = COMEDIC_REWRITE_VISUAL_CAMEOS.iterator();
		while (iterator.hasNext()) {
			ComedicRewriteVisualCameo cameo = iterator.next();
			syncComedicRewriteVisualCameo(server, cameo);
			if (!cameo.chargeStopped() && cameo.chargeEndTick() > 0 && cameo.chargeEndTick() <= currentTick) {
				stopComedicRewriteVisualCameoCharge(server, cameo);
			}
			if (cameo.endTick() > currentTick) {
				continue;
			}

			EntitiesDestroyS2CPacket destroyPacket = new EntitiesDestroyS2CPacket(cameo.entityIds());
			for (UUID viewerId : cameo.viewerIds()) {
				ServerPlayerEntity viewer = server.getPlayerManager().getPlayer(viewerId);
				if (viewer != null) {
					viewer.networkHandler.sendPacket(destroyPacket);
				}
			}
			iterator.remove();
		}
	}

	static void syncComedicRewriteVisualCameo(MinecraftServer server, ComedicRewriteVisualCameo cameo) {
		if (cameo.followState() == null && cameo.rotationState() == null) {
			return;
		}

		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(server, cameo.viewerIds());
		if (viewers.isEmpty()) {
			return;
		}

		ComedicRewriteVisualFollowState followState = cameo.followState();
		Entity anchor = null;
		Vec3d anchorTop = null;
		if (followState != null) {
			ServerWorld anchorWorld = server.getWorld(followState.dimension());
			if (anchorWorld != null) {
				anchor = anchorWorld.getEntity(followState.anchorEntityId());
			}
			if (anchor != null && anchor.isAlive()) {
				anchorTop = new Vec3d(anchor.getX(), anchor.getY() + anchor.getHeight(), anchor.getZ());
			} else {
				followState = null;
			}
		}

		Entity[] entities = cameo.entities();
		for (int index = 0; index < entities.length; index++) {
			Entity entity = entities[index];
			boolean changed = false;
			if (followState != null && index < followState.offsets().length) {
				Vec3d offset = followState.offsets()[index];
				Vec3d targetPos = followState.mode() == ComedicRewriteVisualFollowMode.DROP_TO_ENTITY_TOP
					? comedicRewriteDropVisualTargetPosition(entity, anchorTop, offset, followState.verticalSpeed())
					: anchorTop.add(offset);
				entity.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, entity.getYaw(), entity.getPitch());
				entity.setVelocity(Vec3d.ZERO);
				changed = true;
			} else if (cameo.rotationState() != null && !cameo.chargeStopped() && entity.getVelocity().lengthSquared() > 1.0E-5) {
				entity.refreshPositionAndAngles(
					entity.getX() + entity.getVelocity().x,
					entity.getY() + entity.getVelocity().y,
					entity.getZ() + entity.getVelocity().z,
					entity.getYaw(),
					entity.getPitch()
				);
				changed = true;
			}
			if (cameo.rotationState() != null) {
				applyComedicRewriteVisualRotation(entity, cameo.rotationState());
				changed = true;
			}
			if (!changed) {
				continue;
			}
			EntityPositionSyncS2CPacket positionPacket = EntityPositionSyncS2CPacket.create(entity);
			EntityVelocityUpdateS2CPacket velocityPacket = entity.getVelocity().lengthSquared() <= 1.0E-5
				? null
				: new EntityVelocityUpdateS2CPacket(entity.getId(), entity.getVelocity());
			for (ServerPlayerEntity viewer : viewers) {
				viewer.networkHandler.sendPacket(positionPacket);
				if (velocityPacket != null) {
					viewer.networkHandler.sendPacket(velocityPacket);
				}
			}
		}
	}

	static Vec3d comedicRewriteDropVisualTargetPosition(Entity entity, Vec3d anchorTop, Vec3d offset, double verticalSpeed) {
		double targetY = anchorTop.y + offset.y;
		double nextY = entity.getY();
		if (nextY > targetY) {
			nextY = Math.max(targetY, nextY - Math.max(0.01, verticalSpeed));
		} else {
			nextY = targetY;
		}
		return new Vec3d(anchorTop.x + offset.x, nextY, anchorTop.z + offset.z);
	}

	static void applyComedicRewriteVisualRotation(Entity entity, ComedicRewriteVisualRotationState rotationState) {
		if (rotationState.alignYawToVelocity()) {
			Vec3d horizontalVelocity = new Vec3d(entity.getVelocity().x, 0.0, entity.getVelocity().z);
			if (horizontalVelocity.lengthSquared() > 1.0E-5) {
				float yaw = horizontalYawFromDirection(horizontalVelocity) + rotationState.yawOffsetDegrees();
				entity.setYaw(yaw);
				entity.setBodyYaw(yaw);
				entity.setHeadYaw(yaw);
			}
		}
		if (rotationState.pitchPerTick() != 0.0F) {
			entity.setPitch(MathHelper.wrapDegrees(entity.getPitch() + rotationState.pitchPerTick()));
		}
	}

	static void stopComedicRewriteVisualCameoCharge(MinecraftServer server, ComedicRewriteVisualCameo cameo) {
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(server, cameo.viewerIds());
		if (viewers.isEmpty()) {
			cameo.markChargeStopped();
			return;
		}

		for (Entity entity : cameo.entities()) {
			entity.setVelocity(Vec3d.ZERO);
			EntityVelocityUpdateS2CPacket velocityPacket = new EntityVelocityUpdateS2CPacket(entity.getId(), entity.getVelocity());
			for (ServerPlayerEntity viewer : viewers) {
				viewer.networkHandler.sendPacket(velocityPacket);
			}
		}
		cameo.markChargeStopped();
	}

	static void applyPassiveAbilityEffects(ServerPlayerEntity player, int currentTick) {
		UUID playerId = player.getUuid();

		if (CASSIOPEIA_PASSIVE_ENABLED.contains(playerId)) {
			if (!shouldKeepCassiopeiaEnabled(player)) {
				deactivateCassiopeia(player, true);
			} else if (currentTick % CASSIOPEIA_OUTLINE_REFRESH_TICKS == 0) {
				syncCassiopeiaOutlines(player);
			}
		}

		if (SPOTLIGHT_PASSIVE_ENABLED.contains(playerId)) {
			if (!shouldKeepSpotlightEnabled(player)) {
				deactivateSpotlight(player, true);
			} else {
				applySpotlight(player, currentTick);
			}
		}

		if (COMEDIC_REWRITE_PASSIVE_ENABLED.contains(playerId) && !shouldKeepComedicRewriteEnabled(player)) {
			deactivateComedicRewrite(player);
		}
		updateComedicAssistantState(player, currentTick);

		if (COMEDIC_REWRITE_IMMUNITY_END_TICK.getOrDefault(playerId, Integer.MIN_VALUE) <= currentTick) {
			COMEDIC_REWRITE_IMMUNITY_END_TICK.remove(playerId);
		}
		if (COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.getOrDefault(playerId, Integer.MIN_VALUE) <= currentTick) {
			COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.remove(playerId);
		}
		if (COMEDIC_REWRITE_COOLDOWN_END_TICK.getOrDefault(playerId, Integer.MIN_VALUE) <= currentTick) {
			COMEDIC_REWRITE_COOLDOWN_END_TICK.remove(playerId);
		}

		if (!MARTYRS_FLAME_PASSIVE_ENABLED.contains(playerId)) {
			return;
		}

		if (!shouldKeepMartyrsFlameEnabled(player)) {
			deactivateMartyrsFlame(player, true);
			return;
		}

		if (MARTYRS_FLAME_APPLY_GLOWING_EFFECT) {
			refreshStatusEffect(player, StatusEffects.GLOWING, EFFECT_REFRESH_TICKS, 0, true, false, true);
		}
		if (currentTick % MARTYRS_FLAME_FIRE_PARTICLE_INTERVAL_TICKS == 0) {
			spawnMartyrsFlameCasterParticles(player);
		}
	}

	static void onPlayerDeath(ServerPlayerEntity player) {
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server == null) {
			return;
		}

		UUID playerId = player.getUuid();
		MARTYRS_FLAME_BURNING_TARGETS.remove(playerId);
		MANA_REGEN_BUFFER.remove(playerId);
		clearDeathAbilityState(player);
		MagicPlayerData.clearDomainTimer(player);
		deactivateCassiopeia(player, true);
		resetSpotlightTracking(player);
		COMEDIC_REWRITE_IMMUNITY_END_TICK.remove(playerId);
		COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.remove(playerId);
		COMEDIC_REWRITE_PENDING_STATES.remove(playerId);
		COMEDIC_ASSISTANT_ARMED_STATES.remove(playerId);
		COMEDIC_ASSISTANT_PARROT_CARRY_STATES.remove(playerId);
		clearComedicAssistantCaneImpactState(playerId);
		PLUS_ULTRA_LAST_OUTLINED_PLAYERS.remove(playerId);
		PLUS_ULTRA_DAMAGE_SCALING_GUARD.remove(playerId);
		DOMAIN_CLASH_PENDING_DAMAGE.remove(playerId);
		MAGIC_DAMAGE_PENDING_ATTACKER.remove(playerId);
		MAGIC_DAMAGE_PENDING_ATTACKER.entrySet().removeIf(entry -> entry.getValue().equals(playerId));

		boolean domainStateChanged = false;
		if (domainClashStateForParticipant(playerId) != null) {
			cancelDomainClashParticipant(playerId, server);
		} else {
			DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.remove(playerId);
			if (ownedDomain != null) {
				endOwnedDomain(playerId, ownedDomain, server, server.getTicks());
				domainStateChanged = true;
			}
		}

		if (clearCapturedDomainState(playerId)) {
			domainStateChanged = true;
		}

		GreedRuntime.clearAllRuntimeState(player);

		if (domainStateChanged) {
			persistDomainRuntimeState(server);
		}

		GreedDomainRuntime.onPlayerDeath(player);
		GreedRuntime.onPlayerDeath(player);
	}

	static void updateManaOncePerSecond(ServerPlayerEntity player) {
		if (!MagicPlayerData.hasMagic(player)) {
			return;
		}

		if (isTestingMode(player)) {
			UUID playerId = player.getUuid();
			TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
			MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
			ORIONS_GAMBIT_DRAIN_BUFFER.remove(playerId);
			MANA_REGEN_BUFFER.remove(playerId);
			LOVE_POWER_ACTIVE_THIS_SECOND.put(playerId, false);
			MagicPlayerData.setMana(player, MagicPlayerData.MAX_MANA);
			MagicPlayerData.setDepletedRecoveryMode(player, false);
			return;
		}

		MagicAbility activeAbility = activeAbility(player);
		int mana = MagicPlayerData.getMana(player);
		UUID playerId = player.getUuid();
		boolean orionsGambitManaSuppressed = isOrionsGambitManaDrainSuppressed(player);
		if (isDomainExpansion(activeAbility)) {
			DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.get(playerId);
			if (ownedDomain == null || ownedDomain.ability != activeAbility) {
				setActiveAbility(player, MagicAbility.NONE);
				activeAbility = MagicAbility.NONE;
			}
		}

		if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			LOVE_POWER_ACTIVE_THIS_SECOND.put(playerId, false);
			if (!TILL_DEATH_DO_US_PART_STATES.containsKey(playerId)) {
				TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
				setActiveAbility(player, MagicAbility.NONE);
				return;
			}

			if (orionsGambitManaSuppressed) {
				TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
				regenerateMana(player, mana);
				return;
			}

			double bufferedDrain = TILL_DEATH_DO_US_PART_DRAIN_BUFFER.getOrDefault(playerId, 0.0)
				+ manaFromPercentExact(TILL_DEATH_DO_US_PART_DRAIN_PERCENT_PER_SECOND);
			int manaDrain = Math.max(0, (int) Math.floor(bufferedDrain + 1.0E-7));
			double remainingDrain = Math.max(0.0, bufferedDrain - manaDrain);
			if (remainingDrain > 1.0E-7) {
				TILL_DEATH_DO_US_PART_DRAIN_BUFFER.put(playerId, remainingDrain);
			} else {
				TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
			}
			int nextMana = Math.max(0, mana - manaDrain);
			MagicPlayerData.setMana(player, nextMana);

			if (nextMana == 0 && manaDrain > 0) {
				TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
				deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.MANA_DEPLETED, false);
				MagicPlayerData.setDepletedRecoveryMode(player, true);
				player.sendMessage(Text.translatable("message.magic.ability.out_of_mana", activeAbility.displayName()), true);
			}
			return;
		}

		boolean martyrsFlameActive = MARTYRS_FLAME_PASSIVE_ENABLED.contains(playerId);
		if (martyrsFlameActive && !shouldKeepMartyrsFlameEnabled(player)) {
			deactivateMartyrsFlame(player, true);
			martyrsFlameActive = false;
		}

		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
		int martyrsFlameDrain = 0;
		if (martyrsFlameActive && !orionsGambitManaSuppressed) {
			double bufferedDrain = MARTYRS_FLAME_DRAIN_BUFFER.getOrDefault(playerId, 0.0)
				+ manaFromPercentExact(MARTYRS_FLAME_DRAIN_PERCENT_PER_SECOND);
			martyrsFlameDrain = Math.max(0, (int) Math.floor(bufferedDrain + 1.0E-7));
			double remainingDrain = Math.max(0.0, bufferedDrain - martyrsFlameDrain);
			if (remainingDrain > 1.0E-7) {
				MARTYRS_FLAME_DRAIN_BUFFER.put(playerId, remainingDrain);
			} else {
				MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
			}
		} else {
			MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
		}

		int manipulationManaDrain = 0;
		if (activeAbility == MagicAbility.MANIPULATION && MANIPULATION_MANA_DRAIN_PER_SECOND > 0.0) {
			double bufferedDrain = MANIPULATION_DRAIN_BUFFER.getOrDefault(playerId, 0.0) + MANIPULATION_MANA_DRAIN_PER_SECOND;
			manipulationManaDrain = Math.max(0, (int) Math.floor(bufferedDrain + 1.0E-7));
			double remainingDrain = Math.max(0.0, bufferedDrain - manipulationManaDrain);
			if (remainingDrain > 1.0E-7) {
				MANIPULATION_DRAIN_BUFFER.put(playerId, remainingDrain);
			} else {
				MANIPULATION_DRAIN_BUFFER.remove(playerId);
			}
		} else {
			MANIPULATION_DRAIN_BUFFER.remove(playerId);
		}

		int activeAbilityDrain = switch (activeAbility) {
				case ABSOLUTE_ZERO -> 0;
				case BELOW_FREEZING -> 0;
				case PLANCK_HEAT -> 0;
				case LOVE_AT_FIRST_SIGHT -> isLovePowerActiveThisSecond(player)
					? LOVE_AT_FIRST_SIGHT_ACTIVE_DRAIN_PER_SECOND
					: LOVE_AT_FIRST_SIGHT_IDLE_DRAIN_PER_SECOND;
				case MANIPULATION -> manipulationManaDrain;
				case ORIONS_GAMBIT -> 0;
				case FROST_DOMAIN_EXPANSION, LOVE_DOMAIN_EXPANSION -> 0;
				default -> 0;
			};
		LOVE_POWER_ACTIVE_THIS_SECOND.put(playerId, false);

		if (activeAbility == MagicAbility.ORIONS_GAMBIT) {
			OrionGambitState orionsState = ORIONS_GAMBIT_STATES.get(playerId);
			if (orionsState == null) {
				ORIONS_GAMBIT_DRAIN_BUFFER.remove(playerId);
				setActiveAbility(player, MagicAbility.NONE);
				return;
			}

			if (orionsState.stage == OrionGambitStage.WAITING) {
				if (!ORIONS_GAMBIT_DRAIN_MANA_WHILE_WAITING_FOR_TARGET || orionsGambitManaSuppressed) {
					ORIONS_GAMBIT_DRAIN_BUFFER.remove(playerId);
					if (ORIONS_GAMBIT_DISABLE_MANA_REGEN_WHILE_WAITING_FOR_TARGET) {
						return;
					}
					regenerateMana(player, mana);
					return;
				}
			}

			if (!orionsGambitManaSuppressed) {
				double bufferedDrain = ORIONS_GAMBIT_DRAIN_BUFFER.getOrDefault(playerId, 0.0)
					+ manaFromPercentExact(ORIONS_GAMBIT_MANA_DRAIN_PERCENT_PER_SECOND);
				int manaDrain = Math.max(0, (int) Math.floor(bufferedDrain + 1.0E-7));
				double remainingDrain = Math.max(0.0, bufferedDrain - manaDrain);
				if (remainingDrain > 1.0E-7) {
					ORIONS_GAMBIT_DRAIN_BUFFER.put(playerId, remainingDrain);
				} else {
					ORIONS_GAMBIT_DRAIN_BUFFER.remove(playerId);
				}
				int nextMana = Math.max(0, mana - manaDrain);
				MagicPlayerData.setMana(player, nextMana);
				if (nextMana == 0 && manaDrain > 0) {
					ORIONS_GAMBIT_DRAIN_BUFFER.remove(playerId);
					endOrionsGambit(player, OrionGambitEndReason.MANA_DEPLETED, player.getEntityWorld().getServer().getTicks(), false);
					MagicPlayerData.setDepletedRecoveryMode(player, true);
					player.sendMessage(Text.translatable("message.magic.ability.out_of_mana", activeAbility.displayName()), true);
					return;
				}
			}

			if (orionsState.stage == OrionGambitStage.LINKED && !ORIONS_GAMBIT_DISABLE_MANA_REGEN_WHILE_LINKED) {
				regenerateMana(player, MagicPlayerData.getMana(player));
				return;
			}
			return;
		}

		if (orionsGambitManaSuppressed) {
			regenerateMana(player, mana);
			return;
		}

		if (activeAbility != MagicAbility.NONE || martyrsFlameActive) {
			int manaDrain = activeAbilityDrain + martyrsFlameDrain;
			if (activeAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
				CelestialAlignmentSessionState session = SAGITTARIUS_STATES.get(playerId);
				if (session != null && !session.constellations.isEmpty()) {
					double bufferedDrain = SAGITTARIUS_DRAIN_BUFFER.getOrDefault(playerId, 0.0)
						+ manaFromPercentExact(
							CELESTIAL_ALIGNMENT_CONFIG.manaDrainPercentPerSecondPerActiveConstellation
								* session.constellations.size()
						);
					int sessionDrain = Math.max(0, (int) Math.floor(bufferedDrain + 1.0E-7));
					double remainingDrain = Math.max(0.0, bufferedDrain - sessionDrain);
					if (remainingDrain > 1.0E-7) {
						SAGITTARIUS_DRAIN_BUFFER.put(playerId, remainingDrain);
					} else {
						SAGITTARIUS_DRAIN_BUFFER.remove(playerId);
					}
					manaDrain += sessionDrain;
				} else {
					SAGITTARIUS_DRAIN_BUFFER.remove(playerId);
				}
			}
			int nextMana = Math.max(0, mana - manaDrain);
			MagicPlayerData.setMana(player, nextMana);

			if (nextMana == 0 && manaDrain > 0) {
				if (activeAbility == MagicAbility.BELOW_FREEZING) {
					endFrostStagedMode(player, player.getEntityWorld().getServer().getTicks(), FrostStageEndReason.FORCED_THRESHOLD, true, false);
				}
				boolean keepIgnitionActive = false;
				if (activeAbility == MagicAbility.IGNITION) {
					BurningPassionIgnitionState ignitionState = burningPassionIgnitionState(player);
					if (ignitionState != null && shouldKeepIgnitionActiveOnManaDepletion()) {
						markBurningPassionManaDepleted(player, ignitionState);
						keepIgnitionActive = true;
					} else {
						endIgnition(player, player.getEntityWorld().getServer().getTicks(), BurningPassionIgnitionEndReason.INVALID, true, false);
					}
				}
				if (activeAbility == MagicAbility.ABSOLUTE_ZERO) {
					deactivateAbsoluteZero(player);
				}
				if (activeAbility == MagicAbility.PLANCK_HEAT) {
					deactivatePlanckHeat(player);
				}
				if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
					deactivateLoveAtFirstSight(player);
				}
				if (activeAbility == MagicAbility.MANIPULATION) {
					MANIPULATION_DRAIN_BUFFER.remove(playerId);
					deactivateManipulation(player, true, "mana depleted");
				}
				if (activeAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
					deactivateHerculesBurden(player, true, false);
				}
				if (activeAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
					CelestialAlignmentSessionState session = SAGITTARIUS_STATES.get(playerId);
					if (session != null && !session.constellations.isEmpty() && CELESTIAL_ALIGNMENT_CONFIG.autoEndOnManaDepletion) {
						endCelestialAlignmentSession(player, CelestialAlignmentSessionEndReason.MANA_DEPLETED, player.getEntityWorld().getServer().getTicks(), false, true);
					}
					clearCelestialGamaRayState(player, false, true);
					clearSagittariusWindup(player);
				}
				if (martyrsFlameActive) {
					deactivateMartyrsFlame(player, true);
				}

				if (!keepIgnitionActive) {
					setActiveAbility(player, MagicAbility.NONE);
				}
				MagicPlayerData.setDepletedRecoveryMode(player, true);
				Text depletedAbilityName = activeAbility != MagicAbility.NONE
					? activeAbility.displayName()
					: MagicAbility.MARTYRS_FLAME.displayName();
				player.sendMessage(Text.translatable("message.magic.ability.out_of_mana", depletedAbilityName), true);
			}

			if (activeAbilityPreventsManaRegen(activeAbility) || (martyrsFlameActive && MARTYRS_FLAME_DISABLE_MANA_REGEN_WHILE_ACTIVE) || manaDrain > 0) {
				return;
			}
		}

		regenerateMana(player, mana);
	}

	static void regenerateMana(ServerPlayerEntity player, int currentMana) {
		if (GreedRuntime.blocksManaRegen(player)) {
			return;
		}
		if (player.getEntityWorld().getServer() != null && frostManaRegenBlocked(player, player.getEntityWorld().getServer().getTicks())) {
			return;
		}
		if (isBurningPassionManaRecoveryBlocked(player)) {
			return;
		}

		UUID playerId = player.getUuid();
		boolean depletedMode = MagicPlayerData.isInDepletedRecoveryMode(player);
		if (currentMana >= MagicPlayerData.MAX_MANA) {
			MANA_REGEN_BUFFER.remove(playerId);
			if (depletedMode) {
				MagicPlayerData.setDepletedRecoveryMode(player, false);
			}
			return;
		}

		double regenPercent = depletedMode ? DEPLETED_RECOVERY_REGEN_PER_SECOND : PASSIVE_MANA_REGEN_PER_SECOND;
		regenPercent *= GreedDomainRuntime.manaRegenMultiplier(player, player.getEntityWorld().getServer().getTicks());
		if (regenPercent <= 0.0) {
			return;
		}
		double bufferedRegen = MANA_REGEN_BUFFER.getOrDefault(playerId, 0.0) + manaFromPercentExact(regenPercent);
		int regenAmount = Math.max(0, (int) Math.floor(bufferedRegen + 1.0E-7));
		double remainingRegen = Math.max(0.0, bufferedRegen - regenAmount);
		if (remainingRegen > 1.0E-7) {
			MANA_REGEN_BUFFER.put(playerId, remainingRegen);
		} else {
			MANA_REGEN_BUFFER.remove(playerId);
		}
		int nextMana = Math.min(MagicPlayerData.MAX_MANA, currentMana + regenAmount);
		MagicPlayerData.setMana(player, nextMana);

		if (depletedMode && nextMana >= MagicPlayerData.MAX_MANA) {
			MANA_REGEN_BUFFER.remove(playerId);
			MagicPlayerData.setDepletedRecoveryMode(player, false);
		}
	}
}

