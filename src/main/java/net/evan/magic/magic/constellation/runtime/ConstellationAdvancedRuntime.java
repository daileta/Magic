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


abstract class ConstellationAdvancedRuntime extends ConstellationEffectRuntime {
	static void applyOrionsGambit(ServerPlayerEntity caster, int currentTick) {
		OrionGambitState state = ORIONS_GAMBIT_STATES.get(caster.getUuid());
		if (state == null) {
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		if (state.stage == OrionGambitStage.WAITING) {
			ServerPlayerEntity target = findPlayerTargetInLineOfSight(caster, ORIONS_GAMBIT_TARGET_RANGE);
			if (target != null && target != caster) {
				lockOrionsGambit(caster, target, currentTick);
			}
			return;
		}

		ServerPlayerEntity target = resolveOrionsGambitTarget(caster.getEntityWorld().getServer(), state);
		if (target == null || !target.isAlive() || target.isSpectator()) {
			endOrionsGambit(caster, OrionGambitEndReason.TARGET_INVALID, currentTick, false);
			return;
		}

		if (currentTick >= state.benefitEndTick) {
			endOrionsGambit(caster, OrionGambitEndReason.EXPIRED, currentTick, true);
			return;
		}

		if (currentTick % ORIONS_GAMBIT_TARGET_PARTICLE_INTERVAL_TICKS == 0) {
			spawnOrionsGambitTargetParticles(target);
		}
		applyOrionsGambitPenaltyEffects(caster);
	}

	static void updateOrionsGambitStates(MinecraftServer server, int currentTick) {
		List<OrionPendingEndState> pendingEnds = new ArrayList<>();
		Iterator<Map.Entry<UUID, OrionGambitState>> iterator = ORIONS_GAMBIT_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, OrionGambitState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			if (caster == null) {
				clearOrionsGambitTargetEffects(entry.getValue(), server);
				releaseOrionsGambitTarget(entry.getValue());
				iterator.remove();
				continue;
			}
			if (!caster.isAlive()) {
				pendingEnds.add(new OrionPendingEndState(caster, OrionGambitEndReason.CASTER_DIED, false));
				continue;
			}

			if (activeAbility(caster) != MagicAbility.ORIONS_GAMBIT) {
				pendingEnds.add(new OrionPendingEndState(caster, OrionGambitEndReason.MANUAL_CANCEL, false));
				continue;
			}

			if (entry.getValue().stage == OrionGambitStage.LINKED && currentTick >= entry.getValue().benefitEndTick) {
				pendingEnds.add(new OrionPendingEndState(caster, OrionGambitEndReason.EXPIRED, true));
			}
		}

		for (OrionPendingEndState pendingEnd : pendingEnds) {
			endOrionsGambit(pendingEnd.caster, pendingEnd.reason, currentTick, pendingEnd.sendFeedback);
		}
	}

	static void lockOrionsGambit(ServerPlayerEntity caster, ServerPlayerEntity target, int currentTick) {
		UUID existingCasterId = ORIONS_GAMBIT_CASTER_BY_TARGET.get(target.getUuid());
		if (existingCasterId != null && !existingCasterId.equals(caster.getUuid())) {
			caster.sendMessage(Text.translatable("message.magic.ability.target_controlled"), true);
			return;
		}

		OrionGambitState state = ORIONS_GAMBIT_STATES.get(caster.getUuid());
		if (state == null) {
			return;
		}

		state.stage = OrionGambitStage.LINKED;
		state.targetDimension = target.getEntityWorld().getRegistryKey();
		state.targetId = target.getUuid();
		state.benefitEndTick = currentTick + ORIONS_GAMBIT_LINK_DURATION_TICKS;
		state.usedTargetAbilities.clear();
		state.usedTargetCooldownOverrides.clear();
		ORIONS_GAMBIT_CASTER_BY_TARGET.put(target.getUuid(), caster.getUuid());
		if (ORIONS_GAMBIT_CLEAR_TARGET_COOLDOWNS_ON_LOCK) {
			resetAllCooldowns(target);
		}
		applyOrionsGambitPenaltyEffects(caster);
		caster.sendMessage(Text.translatable("message.magic.constellation.orion_locked", target.getDisplayName()), true);
		target.sendMessage(Text.translatable("message.magic.constellation.orion_targeted", caster.getDisplayName()), true);
	}

	static ServerPlayerEntity resolveOrionsGambitTarget(MinecraftServer server, OrionGambitState state) {
		if (state == null || state.stage != OrionGambitStage.LINKED || state.targetDimension == null || state.targetId == null) {
			return null;
		}

		ServerWorld world = server.getWorld(state.targetDimension);
		if (world == null) {
			return null;
		}

		Entity entity = world.getEntity(state.targetId);
		return entity instanceof ServerPlayerEntity targetPlayer ? targetPlayer : null;
	}

	static void endOrionsGambit(
		ServerPlayerEntity caster,
		OrionGambitEndReason reason,
		int currentTick,
		boolean sendFeedback
	) {
		UUID casterId = caster.getUuid();
		OrionGambitState state = ORIONS_GAMBIT_STATES.remove(casterId);
		ORIONS_GAMBIT_DRAIN_BUFFER.remove(casterId);
		if (state == null) {
			if (activeAbility(caster) == MagicAbility.ORIONS_GAMBIT) {
				setActiveAbility(caster, MagicAbility.NONE);
			}
			return;
		}

		boolean linked = state.stage == OrionGambitStage.LINKED;
		if (linked && ORIONS_GAMBIT_APPLY_USED_TARGET_COOLDOWNS_ON_END) {
			applyOrionsGambitTargetCooldowns(state, caster.getEntityWorld().getServer(), currentTick);
		}
		clearOrionsGambitTargetEffects(state, caster.getEntityWorld().getServer());
		releaseOrionsGambitTarget(state);
		if (linked && ORIONS_GAMBIT_RESET_CASTER_COOLDOWNS_ON_END) {
			applyOrionsGambitCasterCooldownReset(casterId, currentTick);
		}

		int cooldownTicks = reason == OrionGambitEndReason.WAITING_CANCEL
			? ORIONS_GAMBIT_WAIT_CANCEL_COOLDOWN_TICKS
			: ORIONS_GAMBIT_COOLDOWN_TICKS;
		if (cooldownTicks > 0) {
			ORIONS_GAMBIT_COOLDOWN_END_TICK.put(
				casterId,
				currentTick + adjustedCooldownTicks(casterId, MagicAbility.ORIONS_GAMBIT, cooldownTicks, currentTick)
			);
		}

		if (activeAbility(caster) == MagicAbility.ORIONS_GAMBIT) {
			setActiveAbility(caster, MagicAbility.NONE);
		}

		if (linked && state.benefitEndTick > currentTick) {
			ORIONS_GAMBIT_PENALTIES.put(casterId, new OrionPenaltyState(state.benefitEndTick));
		} else {
			clearOrionsGambitPenaltyEffects(caster);
		}

		if (sendFeedback) {
			caster.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.ORIONS_GAMBIT.displayName()), true);
		}
	}

	static void releaseOrionsGambitTarget(OrionGambitState state) {
		if (state != null && state.targetId != null) {
			ORIONS_GAMBIT_CASTER_BY_TARGET.remove(state.targetId);
		}
	}

	static void clearOrionsGambitTargetEffects(OrionGambitState state, MinecraftServer server) {
		if (
			state == null
			|| server == null
			|| !ORIONS_GAMBIT_RESET_GREED_TARGET_COINS_ON_END
			|| state.targetId == null
			|| state.targetDimension == null
		) {
			return;
		}

		ServerPlayerEntity target = resolveOrionsGambitTarget(server, state);
		if (target != null && MagicPlayerData.getSchool(target) == MagicSchool.GREED) {
			MagicPlayerData.setGreedCoinUnits(target, 0);
		}
	}

	static void enforceOrionsGambitGreedTargetCoins(MinecraftServer server) {
		if (server == null || ORIONS_GAMBIT_GREED_TARGET_COIN_UNITS <= 0) {
			return;
		}

		for (OrionGambitState state : ORIONS_GAMBIT_STATES.values()) {
			if (state.stage != OrionGambitStage.LINKED) {
				continue;
			}

			ServerPlayerEntity target = resolveOrionsGambitTarget(server, state);
			if (target != null && MagicPlayerData.getSchool(target) == MagicSchool.GREED) {
				MagicPlayerData.setGreedCoinUnits(target, ORIONS_GAMBIT_GREED_TARGET_COIN_UNITS);
			}
		}
	}

	static void updateOrionsGambitPenalties(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, OrionPenaltyState>> iterator = ORIONS_GAMBIT_PENALTIES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, OrionPenaltyState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			if (caster == null || !caster.isAlive() || currentTick >= entry.getValue().endTick) {
				if (caster != null) {
					clearOrionsGambitPenaltyEffects(caster);
				}
				iterator.remove();
				continue;
			}

			applyOrionsGambitPenaltyEffects(caster);
		}
	}

	static void applyOrionsGambitPenaltyEffects(ServerPlayerEntity caster) {
		refreshStatusEffect(
			caster,
			StatusEffects.WEAKNESS,
			ORIONS_GAMBIT_CASTER_PENALTY_REFRESH_TICKS,
			ORIONS_GAMBIT_CASTER_WEAKNESS_AMPLIFIER,
			true,
			false,
			true
		);
		refreshStatusEffect(
			caster,
			StatusEffects.SLOWNESS,
			ORIONS_GAMBIT_CASTER_PENALTY_REFRESH_TICKS,
			ORIONS_GAMBIT_CASTER_SLOWNESS_AMPLIFIER,
			true,
			false,
			true
		);
		double cappedHealthPoints = ORIONS_GAMBIT_CASTER_MAX_HEALTH_HEARTS * 2.0;
		EntityAttributeInstance maxHealth = caster.getAttributeInstance(EntityAttributes.MAX_HEALTH);
		if (maxHealth != null) {
			EntityAttributeModifier existing = maxHealth.getModifier(ORIONS_GAMBIT_MAX_HEALTH_MODIFIER_ID);
			double baseWithoutModifier = caster.getMaxHealth() - (existing == null ? 0.0 : existing.value());
			double modifierValue = Math.min(0.0, cappedHealthPoints - baseWithoutModifier);
			setSignedAttributeModifier(
				maxHealth,
				ORIONS_GAMBIT_MAX_HEALTH_MODIFIER_ID,
				modifierValue,
				EntityAttributeModifier.Operation.ADD_VALUE
			);
			if (caster.getHealth() > caster.getMaxHealth()) {
				caster.setHealth(caster.getMaxHealth());
			}
		}
	}

	static void clearOrionsGambitPenaltyEffects(ServerPlayerEntity caster) {
		removeAttributeModifier(caster.getAttributeInstance(EntityAttributes.MAX_HEALTH), ORIONS_GAMBIT_MAX_HEALTH_MODIFIER_ID);
		if (caster.getHealth() > caster.getMaxHealth()) {
			caster.setHealth(caster.getMaxHealth());
		}
	}

	static void applyOrionsGambitCasterCooldownReset(UUID casterId, int currentTick) {
		for (MagicAbility ability : abilitiesForSchool(MagicSchool.CONSTELLATION)) {
			startAbilityCooldownFromNow(casterId, ability, currentTick);
		}
	}

	static void applyOrionsGambitTargetCooldowns(OrionGambitState state, MinecraftServer server, int currentTick) {
		if (state == null || state.targetId == null) {
			return;
		}

		for (MagicAbility ability : state.usedTargetAbilities) {
			if (ability == MagicAbility.WITTY_ONE_LINER && state.usedTargetCooldownOverrides.containsKey(ability)) {
				int cooldownTicks = Math.max(0, state.usedTargetCooldownOverrides.get(ability));
				if (cooldownTicks > 0) {
					WITTY_ONE_LINER_COOLDOWN_END_TICK.put(
						state.targetId,
						currentTick + adjustedCooldownTicks(state.targetId, ability, cooldownTicks, currentTick)
					);
				} else {
					WITTY_ONE_LINER_COOLDOWN_END_TICK.remove(state.targetId);
				}
				continue;
			}
			if (ability == MagicAbility.COMEDIC_ASSISTANT && state.usedTargetCooldownOverrides.containsKey(ability)) {
				int cooldownTicks = Math.max(0, state.usedTargetCooldownOverrides.get(ability));
				if (cooldownTicks > 0) {
					COMEDIC_ASSISTANT_COOLDOWN_END_TICK.put(
						state.targetId,
						currentTick + adjustedCooldownTicks(state.targetId, ability, cooldownTicks, currentTick)
					);
				} else {
					COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(state.targetId);
				}
				continue;
			}
			if (ability == MagicAbility.PLUS_ULTRA && state.usedTargetCooldownOverrides.containsKey(ability)) {
				int cooldownTicks = Math.max(0, state.usedTargetCooldownOverrides.get(ability));
				if (cooldownTicks > 0) {
					PLUS_ULTRA_COOLDOWN_END_TICK.put(
						state.targetId,
						currentTick + adjustedCooldownTicks(state.targetId, ability, cooldownTicks, currentTick)
					);
				} else {
					PLUS_ULTRA_COOLDOWN_END_TICK.remove(state.targetId);
				}
				continue;
			}
			startAbilityCooldownFromNow(state.targetId, ability, currentTick);
		}

		ServerPlayerEntity target = server.getPlayerManager().getPlayer(state.targetId);
		if (target != null && !target.isAlive()) {
			clearOrionsGambitPenaltyEffects(target);
		}
	}

	static boolean isOrionsGambitManaCostSuppressed(ServerPlayerEntity player) {
		return ORIONS_GAMBIT_SUPPRESS_TARGET_MANA_COSTS && ORIONS_GAMBIT_CASTER_BY_TARGET.containsKey(player.getUuid());
	}

	static boolean isOrionsGambitManaDrainSuppressed(ServerPlayerEntity player) {
		return ORIONS_GAMBIT_SUPPRESS_TARGET_MANA_COSTS && ORIONS_GAMBIT_CASTER_BY_TARGET.containsKey(player.getUuid());
	}

	static boolean isOrionsGambitCooldownSuppressed(ServerPlayerEntity player) {
		return ORIONS_GAMBIT_SUPPRESS_TARGET_COOLDOWNS && ORIONS_GAMBIT_CASTER_BY_TARGET.containsKey(player.getUuid());
	}

	static boolean areOrionsGambitTargetCooldownsSuppressed(ServerPlayerEntity player) {
		return isOrionsGambitCooldownSuppressed(player);
	}

	static boolean hasActiveOrionsGambitPenalty(ServerPlayerEntity player) {
		OrionGambitState activeState = ORIONS_GAMBIT_STATES.get(player.getUuid());
		if (activeState != null && activeState.stage == OrionGambitStage.LINKED) {
			return true;
		}
		return ORIONS_GAMBIT_PENALTIES.containsKey(player.getUuid());
	}

	static void applyAstralCataclysm(ServerPlayerEntity caster, int currentTick) {
		if (!ASTRAL_CATACLYSM_DOMAIN_STATES.containsKey(caster.getUuid()) && !DOMAIN_EXPANSIONS.containsKey(caster.getUuid())) {
			setActiveAbility(caster, MagicAbility.NONE);
		}
	}

	static void updateAstralCataclysmDomainStates(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, ConstellationDomainState>> iterator = ASTRAL_CATACLYSM_DOMAIN_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ConstellationDomainState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			DomainExpansionState domain = DOMAIN_EXPANSIONS.get(entry.getKey());
			if (caster == null || !caster.isAlive() || domain == null || domain.ability != MagicAbility.ASTRAL_CATACLYSM) {
				iterator.remove();
				continue;
			}

			ConstellationDomainState state = entry.getValue();
			sendAstralCataclysmExpiryWarnings(caster, state, domain, currentTick);
			if (state.phase == ConstellationDomainPhase.CHARGING && currentTick >= state.chargeCompleteTick) {
				state.phase = ConstellationDomainPhase.READY;
				caster.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.ready"), true);
				continue;
			}

			if (state.phase != ConstellationDomainPhase.ACQUIRING) {
				continue;
			}

			if (currentTick >= state.acquireEndTick) {
				iterator.remove();
				deactivateDomainExpansion(caster);
				setActiveAbility(caster, MagicAbility.NONE);
				caster.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.no_target"), true);
				continue;
			}

			LivingEntity target = findAstralCataclysmTargetInLineOfSight(caster, ASTRAL_CATACLYSM_TARGET_RANGE);
			if (target != null) {
				iterator.remove();
				triggerAstralCataclysmExecution(caster, target, currentTick);
			}
		}
	}

	static void sendAstralCataclysmExpiryWarnings(
		ServerPlayerEntity caster,
		ConstellationDomainState state,
		DomainExpansionState domain,
		int currentTick
	) {
		if (domain.expiresTick == Integer.MAX_VALUE) {
			return;
		}

		int remainingTicks = Math.max(0, domain.expiresTick - currentTick);
		if (remainingTicks <= 0) {
			return;
		}

		for (int warningTicks : ASTRAL_CATACLYSM_EXPIRY_WARNING_TICKS) {
			if (remainingTicks > warningTicks || !state.announcedExpiryWarnings.add(warningTicks)) {
				continue;
			}

			int remainingSeconds = Math.max(1, MathHelper.ceil(remainingTicks / (float) TICKS_PER_SECOND));
			caster.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.expiring", remainingSeconds), true);
		}
	}

	static void triggerAstralCataclysmExecution(ServerPlayerEntity caster, LivingEntity target, int currentTick) {
		DomainExpansionState domain = DOMAIN_EXPANSIONS.get(caster.getUuid());
		Vec3d beamOrigin = new Vec3d(target.getX(), target.getY(), target.getZ());
		float restoreYaw = target.getYaw();
		float restorePitch = target.getPitch();
		if (domain != null) {
			DomainCapturedEntityState capturedState = domain.capturedEntities.get(target.getUuid());
			if (capturedState != null) {
				beamOrigin = capturedState.position;
				restoreYaw = capturedState.yaw;
				restorePitch = capturedState.pitch;
			}
		}

		if (
			target instanceof ServerPlayerEntity targetPlayer
			&& ASTRAL_CATACLYSM_CANCEL_BEAM_ON_PROTECTED_BEACON_CORE_HOLDER
			&& isBeaconCoreProtected(targetPlayer)
		) {
			caster.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.beacon_core_blocked.caster", target.getDisplayName()), true);
			targetPlayer.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.beacon_core_blocked.target"), true);
			deactivateDomainExpansion(caster);
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		ASTRAL_EXECUTION_BEAMS.put(
			target.getUuid(),
			new AstralExecutionBeamState(
				target.getEntityWorld().getRegistryKey(),
				caster.getUuid(),
				beamOrigin.x,
				beamOrigin.y,
				beamOrigin.z,
				beamOrigin.x,
				beamOrigin.y,
				beamOrigin.z,
				restoreYaw,
				restorePitch,
				currentTick,
				currentTick
			)
		);
		target.stopUsingItem();
		target.setVelocity(0.0, 0.0, 0.0);
		target.setOnGround(true);
		if (target instanceof MobEntity mob) {
			mob.getNavigation().stop();
		}
		caster.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.locked", target.getDisplayName()), true);
		if (target instanceof ServerPlayerEntity targetPlayer) {
			targetPlayer.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.targeted"), true);
		}
		deactivateDomainExpansion(caster);
		setActiveAbility(caster, MagicAbility.NONE);
	}

	static void updateAstralExecutionBeams(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, AstralExecutionBeamState>> iterator = ASTRAL_EXECUTION_BEAMS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, AstralExecutionBeamState> entry = iterator.next();
			AstralExecutionBeamState state = entry.getValue();
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
			if (target instanceof PlayerEntity player && player.isSpectator()) {
				iterator.remove();
				continue;
			}
			if (
				target instanceof ServerPlayerEntity targetPlayer
				&& ASTRAL_CATACLYSM_CANCEL_BEAM_ON_PROTECTED_BEACON_CORE_HOLDER
				&& isBeaconCoreProtected(targetPlayer)
			) {
				restoreAstralExecutionBeamTarget(target, state);
				iterator.remove();
				ServerPlayerEntity caster = server.getPlayerManager().getPlayer(state.casterId);
				if (caster != null) {
					caster.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.beacon_core_blocked.caster", target.getDisplayName()), true);
				}
				targetPlayer.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.beacon_core_blocked.target"), true);
				continue;
			}

			target.stopUsingItem();
			target.setVelocity(0.0, 0.0, 0.0);
			target.setOnGround(true);
			double risePerTick = ASTRAL_CATACLYSM_BEAM_RISE_BLOCKS_PER_SECOND / TICKS_PER_SECOND;
			double maxRiseY = Math.max(state.baseY, world.getTopYInclusive() + 1.0 - target.getHeight());
			state.currentY = Math.min(state.currentY + risePerTick, maxRiseY);
			teleportDomainEntity(target, state.lockedX, state.currentY, state.lockedZ, target.getYaw(), target.getPitch());
			if (target instanceof MobEntity mob) {
				mob.getNavigation().stop();
			}
			spawnAstralExecutionBeamParticles(world, state, currentTick);
			if (currentTick >= state.nextSoundTick) {
				playAstralExecutionBeamSounds(world, state);
				state.nextSoundTick = currentTick + ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_INTERVAL_TICKS;
			}
			if (currentTick >= state.nextDamageTick) {
				applyAstralExecutionDamage(target, state);
				state.nextDamageTick = currentTick + ASTRAL_CATACLYSM_BEAM_DAMAGE_INTERVAL_TICKS;
			}

			if (!target.isAlive()) {
				iterator.remove();
			}
		}
	}

	static void applyAstralExecutionDamage(LivingEntity target, AstralExecutionBeamState state) {
		if (!(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		if (!ASTRAL_CATACLYSM_IGNORE_TOTEMS) {
			dealTrackedMagicDamage(
				target,
				state.casterId,
				createTrueMagicDamageSource(world.getServer(), world, state.casterId),
				ASTRAL_CATACLYSM_BEAM_TRUE_DAMAGE_PER_INTERVAL
			);
			return;
		}

		float nextHealth = target.getHealth() - ASTRAL_CATACLYSM_BEAM_TRUE_DAMAGE_PER_INTERVAL;
		if (nextHealth <= 0.0F) {
			target.kill(world);
			return;
		}

		target.setHealth(nextHealth);
	}

	static void spawnAstralExecutionBeamParticles(ServerWorld world, AstralExecutionBeamState state, int currentTick) {
		double topY = getAstralExecutionBeamTopY(world, state);
		AstralCataclysmBeamParticleEffect coreEffect = new AstralCataclysmBeamParticleEffect(
			ASTRAL_CATACLYSM_BEAM_CORE_COLOR_RGB,
			ASTRAL_CATACLYSM_BEAM_CORE_INTENSITY,
			ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_SCALE,
			ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_LIFETIME_TICKS
		);
		AstralCataclysmBeamParticleEffect outerEffect = new AstralCataclysmBeamParticleEffect(
			ASTRAL_CATACLYSM_BEAM_OUTER_COLOR_RGB,
			0.68F,
			ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_SCALE,
			ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_LIFETIME_TICKS
		);
		AstralCataclysmDownflowParticleEffect downflowEffect = new AstralCataclysmDownflowParticleEffect(
			ASTRAL_CATACLYSM_BEAM_OUTER_COLOR_RGB,
			0.82F,
			ASTRAL_CATACLYSM_BEAM_FALLING_PARTICLE_SCALE,
			ASTRAL_CATACLYSM_BEAM_FALLING_PARTICLE_LIFETIME_TICKS,
			ASTRAL_CATACLYSM_BEAM_DESCENT_BLOCKS_PER_SECOND / TICKS_PER_SECOND
		);
		double beamY = state.baseY;
		while (beamY <= topY) {
			spawnAstralExecutionBeamSlice(world, state.lockedX, beamY, state.lockedZ, currentTick, coreEffect, outerEffect);
			beamY += ASTRAL_CATACLYSM_BEAM_PARTICLE_STEP;
		}
		spawnAstralExecutionDownflow(world, state.lockedX, state.baseY, topY, state.lockedZ, downflowEffect);
		spawnAstralExecutionSpiral(world, state.lockedX, state.baseY, topY, state.lockedZ, currentTick);
	}

	static void playAstralExecutionBeamSounds(ServerWorld world, AstralExecutionBeamState state) {
		world.playSound(
			null,
			state.lockedX,
			state.currentY,
			state.lockedZ,
			SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
			SoundCategory.PLAYERS,
			ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_VOLUME,
			ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_PITCH
		);
		world.playSound(
			null,
			state.lockedX,
			state.currentY,
			state.lockedZ,
			SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM,
			SoundCategory.PLAYERS,
			ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_VOLUME * 0.8F,
			ASTRAL_CATACLYSM_BEAM_HEAVENLY_SOUND_PITCH + 0.2F
		);
	}

	static void restoreAstralExecutionBeamTarget(LivingEntity target, AstralExecutionBeamState state) {
		teleportDomainEntity(target, state.restoreX, state.restoreY, state.restoreZ, state.restoreYaw, state.restorePitch);
		target.setVelocity(0.0, 0.0, 0.0);
		target.setOnGround(true);
	}

	static double getAstralExecutionBeamTopY(ServerWorld world, AstralExecutionBeamState state) {
		return Math.max(world.getTopYInclusive() + 1.0, state.currentY + 12.0);
	}

	static void spawnAstralExecutionBeamSlice(
		ServerWorld world,
		double x,
		double y,
		double z,
		int currentTick,
		AstralCataclysmBeamParticleEffect coreEffect,
		AstralCataclysmBeamParticleEffect outerEffect
	) {
		if (ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_COUNT > 0) {
			world.spawnParticles(coreEffect, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
			double coreSpread = Math.max(0.18, ASTRAL_CATACLYSM_BEAM_RADIUS * 0.24);
			for (int index = 1; index < ASTRAL_CATACLYSM_BEAM_CORE_PARTICLE_COUNT; index++) {
				double angle = world.random.nextDouble() * (Math.PI * 2.0);
				double radius = Math.sqrt(world.random.nextDouble()) * coreSpread;
				double particleX = x + Math.cos(angle) * radius;
				double particleY = y + (world.random.nextDouble() - 0.5) * ASTRAL_CATACLYSM_BEAM_PARTICLE_STEP * 0.45;
				double particleZ = z + Math.sin(angle) * radius;
				world.spawnParticles(coreEffect, particleX, particleY, particleZ, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}

		if (ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_COUNT <= 0) {
			return;
		}

		double angularOffset = currentTick * 0.09;
		for (int index = 0; index < ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_COUNT; index++) {
			double normalized = (index + 0.5) / ASTRAL_CATACLYSM_BEAM_OUTER_PARTICLE_COUNT;
			double angle = angularOffset + normalized * Math.PI * 2.0 + world.random.nextDouble() * 0.25;
			double radius = MathHelper.lerp(world.random.nextDouble(), ASTRAL_CATACLYSM_BEAM_RADIUS * 0.56, ASTRAL_CATACLYSM_BEAM_RADIUS * 0.92);
			double particleX = x + Math.cos(angle) * radius;
			double particleY = y + (world.random.nextDouble() - 0.5) * ASTRAL_CATACLYSM_BEAM_PARTICLE_STEP * 0.7;
			double particleZ = z + Math.sin(angle) * radius;
			world.spawnParticles(outerEffect, particleX, particleY, particleZ, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	static void spawnAstralExecutionDownflow(
		ServerWorld world,
		double x,
		double baseY,
		double topY,
		double z,
		AstralCataclysmDownflowParticleEffect effect
	) {
		if (ASTRAL_CATACLYSM_BEAM_FALLING_PARTICLE_COUNT <= 0) {
			return;
		}

		double beamHeight = Math.max(1.0, topY - baseY);
		double descentTravel = Math.max(8.0, effect.descentPerTick() * ASTRAL_CATACLYSM_BEAM_FALLING_PARTICLE_LIFETIME_TICKS * 1.6);
		double spawnMinY = Math.max(baseY + beamHeight * 0.35, topY - descentTravel);
		for (int index = 0; index < ASTRAL_CATACLYSM_BEAM_FALLING_PARTICLE_COUNT; index++) {
			double angle = world.random.nextDouble() * (Math.PI * 2.0);
			double radiusFactor = world.random.nextDouble() < 0.7
				? Math.sqrt(world.random.nextDouble()) * 0.45
				: MathHelper.lerp(world.random.nextDouble(), 0.45, 0.92);
			double radius = Math.max(0.12, ASTRAL_CATACLYSM_BEAM_RADIUS * radiusFactor);
			double particleX = x + Math.cos(angle) * radius;
			double particleY = MathHelper.lerp(world.random.nextDouble(), spawnMinY, topY);
			double particleZ = z + Math.sin(angle) * radius;
			world.spawnParticles(effect, particleX, particleY, particleZ, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	static void spawnAstralExecutionSpiral(ServerWorld world, double x, double baseY, double topY, double z, int currentTick) {
		if (ASTRAL_CATACLYSM_BEAM_SPIRAL_PARTICLE_COUNT <= 0 || ASTRAL_CATACLYSM_BEAM_SPIRAL_COLOR_RGBS.isEmpty()) {
			return;
		}

		double beamHeight = Math.max(1.0, topY - baseY);
		double twistPerBlock = (Math.PI * 2.0) / ASTRAL_CATACLYSM_BEAM_SPIRAL_VERTICAL_SPACING;
		double descentPerTick = (ASTRAL_CATACLYSM_BEAM_DESCENT_BLOCKS_PER_SECOND / TICKS_PER_SECOND) * 0.18;
		double descentOffset = beamHeight <= descentPerTick ? 0.0 : (currentTick * descentPerTick * 0.9) % beamHeight;
		int colorCount = ASTRAL_CATACLYSM_BEAM_SPIRAL_COLOR_RGBS.size();
		for (int index = 0; index < ASTRAL_CATACLYSM_BEAM_SPIRAL_PARTICLE_COUNT; index++) {
			int colorIndex = index % colorCount;
			double normalized = (index + 0.5) / (double) ASTRAL_CATACLYSM_BEAM_SPIRAL_PARTICLE_COUNT;
			double localY = (normalized * beamHeight + descentOffset) % beamHeight;
			double angle = currentTick * ASTRAL_CATACLYSM_BEAM_SPIRAL_ANGULAR_SPEED_RADIANS_PER_TICK
				+ ((Math.PI * 2.0 * colorIndex) / colorCount);
			AstralCataclysmSpiralParticleEffect effect = new AstralCataclysmSpiralParticleEffect(
				x,
				baseY,
				z,
				localY,
				angle,
				ASTRAL_CATACLYSM_BEAM_SPIRAL_ANGULAR_SPEED_RADIANS_PER_TICK,
				descentPerTick,
				ASTRAL_CATACLYSM_BEAM_SPIRAL_ORBIT_RADIUS,
				twistPerBlock,
				ASTRAL_CATACLYSM_BEAM_SPIRAL_COLOR_RGBS.get(colorIndex),
				0.84F,
				ASTRAL_CATACLYSM_BEAM_SPIRAL_PARTICLE_SCALE,
				ASTRAL_CATACLYSM_BEAM_SPIRAL_PARTICLE_LIFETIME_TICKS
			);
			world.spawnParticles(effect, x, baseY + localY, z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	static void spawnHerculesImpactBurst(LivingEntity target) {
		if (!(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		double x = target.getX();
		double y = target.getY() + 0.05;
		double z = target.getZ();
		if (HERCULES_ACTIVATION_IMPACT_DIRT_PARTICLE_COUNT > 0) {
			world.spawnParticles(
				HERCULES_ACTIVATION_DIRT_PARTICLE,
				x,
				y,
				z,
				HERCULES_ACTIVATION_IMPACT_DIRT_PARTICLE_COUNT,
				0.45,
				0.12,
				0.45,
				HERCULES_ACTIVATION_IMPACT_DIRT_PARTICLE_SPEED
			);
		}
		world.playSound(
			null,
			x,
			y,
			z,
			SoundEvents.BLOCK_ANVIL_LAND,
			SoundCategory.PLAYERS,
			HERCULES_ACTIVATION_IMPACT_SOUND_VOLUME,
			HERCULES_ACTIVATION_IMPACT_SOUND_PITCH
		);
	}

	static void spawnHerculesBurdenParticles(LivingEntity target) {
		if (!(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		world.spawnParticles(ParticleTypes.GLOW, target.getX(), target.getBodyY(0.55), target.getZ(), 10, 0.35, 0.45, 0.35, 0.01);
		world.spawnParticles(ParticleTypes.END_ROD, target.getX(), target.getBodyY(0.55), target.getZ(), 8, 0.25, 0.35, 0.25, 0.0);
	}

	static void activateManipulation(ServerPlayerEntity caster, ServerPlayerEntity target) {
		activateManipulation(caster, target, caster.getEntityWorld().getServer().getTicks());
	}

	static void applyManipulation(ServerPlayerEntity caster) {
		ManipulationState state = MANIPULATION_STATES.get(caster.getUuid());
		if (state == null) {
			debugManipulation("{} empty embrace state missing during tick; canceling ability", debugName(caster));
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		int currentTick = caster.getEntityWorld().getServer().getTicks();
		if (state.stage == ManipulationStage.WAITING) {
			tryAcquireManipulationTarget(caster, currentTick);
			return;
		}

		ServerPlayerEntity targetPlayer = resolveManipulationTarget(caster.getEntityWorld().getServer(), state);
		if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.isSpectator()) {
			deactivateManipulation(caster, true, "target missing or dead");
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		if (targetPlayer.getEntityWorld() != caster.getEntityWorld()) {
			deactivateManipulation(caster, true, "target changed dimension");
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		double breakRangeSquared = MANIPULATION_BREAK_RANGE * (double) MANIPULATION_BREAK_RANGE;
		if (caster.squaredDistanceTo(targetPlayer) > breakRangeSquared) {
			deactivateManipulation(caster, true, "target moved out of range");
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		spawnManipulationTargetParticles(targetPlayer);
	}

	static void cleanupManipulationStates(MinecraftServer server) {
		Iterator<Map.Entry<UUID, ManipulationState>> iterator = MANIPULATION_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ManipulationState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());

			if (caster != null && caster.isAlive() && activeAbility(caster) == MagicAbility.MANIPULATION) {
				continue;
			}

			debugManipulation(
				"{} manipulation cleanup: casterMissingOrInactive={}, casterAlive={}, activeAbility={}",
				caster == null ? entry.getKey() : debugName(caster),
				caster == null,
				caster != null && caster.isAlive(),
				caster == null ? "null" : activeAbility(caster)
			);
			releaseManipulationState(entry.getKey(), entry.getValue(), server, caster);
			startAbilityCooldownFromNow(entry.getKey(), MagicAbility.MANIPULATION, server.getTicks());
			MANIPULATION_LAST_CLAMP_LOG_TICK.remove(entry.getKey());
			MANIPULATION_INTERACTION_PROXY.remove(entry.getKey());
			MANIPULATION_INPUT_BY_CASTER.remove(entry.getKey());
			MANIPULATION_LOOK_BY_CASTER.remove(entry.getKey());
			debugManipulation(
				"{} manipulation cleanup finalized: cooldownEndTick={}, remainingStates={}",
				caster == null ? entry.getKey() : debugName(caster),
				server.getTicks() + MANIPULATION_COOLDOWN_TICKS,
				MANIPULATION_STATES.size() - 1
			);
			iterator.remove();
		}
	}

	static void syncManipulationSuppressionTags(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (isMagicSuppressed(player)) {
				applyManipulationSuppressionTags(player);
				continue;
			}

			clearManipulationSuppressionTags(player);
		}
	}

	static void applyManipulationSuppressionTags(ServerPlayerEntity target) {
		target.addCommandTag(EMPTY_EMBRACE_TAG);
		setManipulationSuppressionTag(target, EMPTY_EMBRACE_ARTIFACT_POWERS_TAG, MANIPULATION_DISABLE_ARTIFACT_POWERS);
		setManipulationSuppressionTag(target, EMPTY_EMBRACE_ARTIFACT_SUMMONS_TAG, MANIPULATION_DISMISS_ARTIFACT_SUMMONS);
		setManipulationSuppressionTag(target, EMPTY_EMBRACE_ARTIFACT_ARMOR_TAG, MANIPULATION_DISABLE_ARTIFACT_ARMOR_EFFECTS);
		setManipulationSuppressionTag(target, EMPTY_EMBRACE_INFESTED_SILVERFISH_TAG, MANIPULATION_DISABLE_INFESTED_SILVERFISH_ENHANCEMENTS);
	}

	static void clearManipulationSuppressionTags(ServerPlayerEntity target) {
		target.removeCommandTag(EMPTY_EMBRACE_TAG);
		target.removeCommandTag(EMPTY_EMBRACE_ARTIFACT_POWERS_TAG);
		target.removeCommandTag(EMPTY_EMBRACE_ARTIFACT_SUMMONS_TAG);
		target.removeCommandTag(EMPTY_EMBRACE_ARTIFACT_ARMOR_TAG);
		target.removeCommandTag(EMPTY_EMBRACE_INFESTED_SILVERFISH_TAG);
	}

	static void setManipulationSuppressionTag(ServerPlayerEntity target, String tag, boolean enabled) {
		if (enabled) {
			target.addCommandTag(tag);
			return;
		}

		target.removeCommandTag(tag);
	}

	static void cleanupPlanckHeatStates(MinecraftServer server) {
		Iterator<Map.Entry<UUID, PlanckHeatState>> iterator = PLANCK_HEAT_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, PlanckHeatState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());

			if (caster != null && caster.isAlive() && activeAbility(caster) == MagicAbility.PLANCK_HEAT) {
				continue;
			}

			PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.remove(entry.getKey());
			iterator.remove();
		}
	}

	static ServerPlayerEntity resolveManipulationTarget(MinecraftServer server, ManipulationState state) {
		if (state == null || state.stage != ManipulationStage.LINKED || state.targetDimension == null || state.targetId == null) {
			return null;
		}

		ServerWorld world = server.getWorld(state.targetDimension);
		if (world == null) {
			debugManipulation(
				"empty embrace target resolve failed: dimension {} was not loaded for targetId={}",
				state.targetDimension.getValue(),
				state.targetId
			);
			return null;
		}

		Entity entity = world.getEntity(state.targetId);
		if (entity instanceof ServerPlayerEntity targetPlayer && targetPlayer.isAlive() && !targetPlayer.isSpectator()) {
			return targetPlayer;
		}
		debugManipulation(
			"empty embrace target resolve failed: targetId={} entityType={}, alive={}, spectator={}",
			state.targetId,
			entity == null ? "null" : entity.getClass().getSimpleName(),
			entity instanceof LivingEntity living && living.isAlive(),
			entity instanceof PlayerEntity player && player.isSpectator()
		);

		return null;
	}
}


