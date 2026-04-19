package net.evan.magic.magic.ability;
import net.evan.magic.magic.core.ability.MagicAbility;
import net.evan.magic.magic.ability.GreedAbilityRuntime;

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


abstract class LoveAbilityRequests extends FrostAbilityRequests {
	static void handleLoveAtFirstSightRequest(ServerPlayerEntity player) {
		MagicAbility activeAbility = activeAbility(player);
		if (activeAbility == MagicAbility.MANIPULATION) {
			deactivateManipulation(player, true, "switched to Love At First Sight");
		}

		if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.MANUAL_CANCEL, false);
		}

		if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			deactivateLoveAtFirstSight(player);
			setActiveAbility(player, MagicAbility.NONE);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.LOVE_AT_FIRST_SIGHT.displayName()), true);
			return;
		}

		if (!isOrionsGambitManaCostSuppressed(player) && MagicPlayerData.getMana(player) <= 0) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		setActiveAbility(player, MagicAbility.LOVE_AT_FIRST_SIGHT);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		LOVE_POWER_ACTIVE_THIS_SECOND.put(player.getUuid(), false);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.LOVE_AT_FIRST_SIGHT.displayName()), true);
		recordOrionsGambitAbilityUse(player, MagicAbility.LOVE_AT_FIRST_SIGHT);
	}

	static void handleTillDeathDoUsPartRequest(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		int currentTick = player.getEntityWorld().getServer().getTicks();
		if (TILL_DEATH_DO_US_PART_STATES.containsKey(playerId)) {
			deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.MANUAL_CANCEL, true);
			return;
		}

		if (TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.contains(playerId)) {
			TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.remove(playerId);
			player.sendMessage(
				Text.translatable("message.magic.ability.passive_disabled", MagicAbility.TILL_DEATH_DO_US_PART.displayName()),
				false
			);
			return;
		}

		TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.add(playerId);
		player.sendMessage(
			Text.translatable("message.magic.ability.passive_enabled", MagicAbility.TILL_DEATH_DO_US_PART.displayName()),
			false
		);
		recordOrionsGambitAbilityUse(player, MagicAbility.TILL_DEATH_DO_US_PART);
		int remainingTicks = tillDeathDoUsPartCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.TILL_DEATH_DO_US_PART, remainingTicks, false);
		}
	}

	static void handleManipulationRequest(ServerPlayerEntity player) {
		MagicAbility activeAbility = activeAbility(player);
		int currentTick = player.getEntityWorld().getServer().getTicks();
		debugManipulation(
			"{} empty embrace request: activeAbility={}, mana={}, cooldownRemainingTicks={}",
			debugName(player),
			activeAbility,
			MagicPlayerData.getMana(player),
			manipulationCooldownRemaining(player, currentTick)
		);

		if (activeAbility == MagicAbility.MANIPULATION) {
			debugManipulation("{} empty embrace toggle requested while active; deactivating", debugName(player));
			deactivateManipulation(player, true, "manual toggle via ability key");
			setActiveAbility(player, MagicAbility.NONE);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.MANIPULATION.displayName()), true);
			return;
		}

		if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			debugManipulation("{} empty embrace request: deactivating prior LOVE_AT_FIRST_SIGHT", debugName(player));
			deactivateLoveAtFirstSight(player);
		}

		if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			debugManipulation("{} empty embrace request: deactivating prior TILL_DEATH_DO_US_PART", debugName(player));
			deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.MANUAL_CANCEL, false);
		}

		int remainingTicks = manipulationCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			int secondsRemaining = (int) Math.ceil(remainingTicks / 20.0);
			player.sendMessage(
				Text.translatable("message.magic.ability.cooldown", MagicAbility.MANIPULATION.displayName(), secondsRemaining),
				true
			);
			debugManipulation("{} empty embrace denied: cooldown {} ticks", debugName(player), remainingTicks);
			return;
		}

		if (!canSpendAbilityCost(player, MANIPULATION_ACTIVATION_MANA_COST)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			debugManipulation("{} empty embrace denied: activation cost {}", debugName(player), MANIPULATION_ACTIVATION_MANA_COST);
			return;
		}

		spendAbilityCost(player, MANIPULATION_ACTIVATION_MANA_COST);
		setActiveAbility(player, MagicAbility.MANIPULATION);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		MANIPULATION_STATES.put(player.getUuid(), ManipulationState.waiting(currentTick, new Vec3d(player.getX(), player.getY(), player.getZ())));
		player.sendMessage(Text.translatable("message.magic.empty_embrace.waiting"), true);
		tryAcquireManipulationTarget(player, currentTick);
		recordOrionsGambitAbilityUse(player, MagicAbility.MANIPULATION);
	}

	static void tryAcquireManipulationTarget(ServerPlayerEntity caster, int currentTick) {
		ManipulationState state = MANIPULATION_STATES.get(caster.getUuid());
		if (state == null || state.stage != ManipulationStage.WAITING) {
			return;
		}

		ServerPlayerEntity target = findPlayerTargetInLineOfSight(caster, MANIPULATION_ACQUIRE_RANGE);
		if (target == null) {
			return;
		}

		UUID existingCasterId = MANIPULATION_CASTER_BY_TARGET.get(target.getUuid());
		if (existingCasterId != null && !existingCasterId.equals(caster.getUuid())) {
			caster.sendMessage(Text.translatable("message.magic.ability.target_controlled"), true);
			deactivateManipulation(caster, true, "target already affected by empty embrace");
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		activateManipulation(caster, target, currentTick);
	}

	static boolean targetHasActiveDomain(ServerPlayerEntity target) {
		return DOMAIN_EXPANSIONS.containsKey(target.getUuid()) || isDomainExpansion(activeAbility(target));
	}

	static void activateManipulation(ServerPlayerEntity caster, ServerPlayerEntity target, int currentTick) {
		UUID casterId = caster.getUuid();
		ManipulationState state = MANIPULATION_STATES.computeIfAbsent(
			casterId,
			ignored -> ManipulationState.waiting(currentTick, new Vec3d(caster.getX(), caster.getY(), caster.getZ()))
		);
		state.stage = ManipulationStage.LINKED;
		state.targetDimension = target.getEntityWorld().getRegistryKey();
		state.targetId = target.getUuid();
		state.lockedCasterPos = new Vec3d(caster.getX(), caster.getY(), caster.getZ());
		state.controlledPos = new Vec3d(target.getX(), target.getY(), target.getZ());
		MANIPULATION_CASTER_BY_TARGET.put(target.getUuid(), casterId);
		clearManipulationSuppressionTags(target);
		debugManipulation(
			"{} empty embrace locked target {} ({}) at [{}, {}, {}]",
			debugName(caster),
			target.getName().getString(),
			target.getUuid(),
			round3(target.getX()),
			round3(target.getY()),
			round3(target.getZ())
		);
		caster.sendMessage(Text.translatable("message.magic.empty_embrace.locked", target.getDisplayName()), true);
		target.sendMessage(Text.translatable("message.magic.empty_embrace.target_status", caster.getDisplayName()), true);
	}

	static void cancelMagicOnTarget(ServerPlayerEntity target, int currentTick) {
		cancelOwnedNonDomainMagicOnTarget(target, currentTick);
		LOVE_LOCKED_TARGETS.remove(target.getUuid());

		List<UUID> linkedTillDeathCasters = new ArrayList<>();
		for (Map.Entry<UUID, TillDeathDoUsPartState> entry : TILL_DEATH_DO_US_PART_STATES.entrySet()) {
			if (target.getUuid().equals(entry.getValue().linkedPlayerId)) {
				linkedTillDeathCasters.add(entry.getKey());
			}
		}
		for (UUID casterId : linkedTillDeathCasters) {
			ServerPlayerEntity linkedCaster = target.getEntityWorld().getServer().getPlayerManager().getPlayer(casterId);
			if (linkedCaster != null) {
				deactivateTillDeathDoUsPart(linkedCaster, TillDeathDoUsPartEndReason.LINK_TARGET_INVALID, false);
			} else {
				TILL_DEATH_DO_US_PART_STATES.remove(casterId);
				startTillDeathDoUsPartCooldown(casterId, currentTick);
			}
		}
	}

	static void cancelOwnedNonDomainMagicOnTarget(ServerPlayerEntity target, int currentTick) {
		MagicAbility targetAbility = activeAbility(target);
		boolean targetOwnsDomain = DOMAIN_EXPANSIONS.containsKey(target.getUuid()) || isDomainExpansion(targetAbility);
		if (targetAbility == MagicAbility.IGNITION) {
			endIgnition(target, currentTick, BurningPassionIgnitionEndReason.INVALID, true, false);
		}
		if (targetAbility == MagicAbility.BELOW_FREEZING) {
			endFrostStagedMode(target, currentTick, FrostStageEndReason.INVALID, true, false);
		}
		if (targetAbility == MagicAbility.ABSOLUTE_ZERO) {
			deactivateAbsoluteZero(target);
		}
		if (targetAbility == MagicAbility.PLANCK_HEAT) {
			deactivatePlanckHeat(target);
		}
		if (targetAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			deactivateLoveAtFirstSight(target);
		}
		if (targetAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			deactivateTillDeathDoUsPart(target, TillDeathDoUsPartEndReason.MANUAL_CANCEL, true);
		}
		if (targetAbility == MagicAbility.MANIPULATION) {
			deactivateManipulation(target, true, "suppressed by empty embrace");
		}
		if (targetAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			deactivateHerculesBurden(target, true, false);
		}
		if (targetAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			clearSagittariusWindup(target);
		}
		if (targetAbility == MagicAbility.ORIONS_GAMBIT) {
			endOrionsGambit(target, OrionGambitEndReason.MANUAL_CANCEL, currentTick, false);
		}
		clearOverrideMeteorState(target.getUuid(), target.getEntityWorld().getServer());
		clearComedicAssistantState(target.getUuid());
		GreedAbilityRuntime.cancelActiveAbilities(target, currentTick);

		if (!targetOwnsDomain) {
			setActiveAbility(target, MagicAbility.NONE);
		}
	}

	static void cancelOwnedMagicOnTargetForBankruptcy(
		ServerPlayerEntity target,
		int currentTick,
		boolean preserveMaximumAbilities,
		boolean preserveDomainAbilities
	) {
		UUID targetId = target.getUuid();
		MagicAbility targetAbility = activeAbility(target);
		boolean protectedActiveAbility = isBankruptcyProtectedAbility(targetAbility, preserveMaximumAbilities, preserveDomainAbilities);

		if (!isBankruptcyProtectedAbility(MagicAbility.MARTYRS_FLAME, preserveMaximumAbilities, preserveDomainAbilities)) {
			MARTYRS_FLAME_PASSIVE_ENABLED.remove(targetId);
			MARTYRS_FLAME_DRAIN_BUFFER.remove(targetId);
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.SPOTLIGHT, preserveMaximumAbilities, preserveDomainAbilities)) {
			deactivateSpotlight(target, true);
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.CASSIOPEIA, preserveMaximumAbilities, preserveDomainAbilities)) {
			deactivateCassiopeia(target, true);
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.COMEDIC_REWRITE, preserveMaximumAbilities, preserveDomainAbilities)) {
			deactivateComedicRewrite(target);
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.COMEDIC_ASSISTANT, preserveMaximumAbilities, preserveDomainAbilities)) {
			clearComedicAssistantState(targetId);
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.PLUS_ULTRA, preserveMaximumAbilities, preserveDomainAbilities) && targetAbility == MagicAbility.PLUS_ULTRA) {
			endPlusUltra(target, currentTick, PlusUltraEndMode.FULL, false);
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.OVERRIDE, preserveMaximumAbilities, preserveDomainAbilities)) {
			clearOverrideMeteorState(targetId, target.getEntityWorld().getServer());
		}
		if (!isBankruptcyProtectedAbility(MagicAbility.TILL_DEATH_DO_US_PART, preserveMaximumAbilities, preserveDomainAbilities)) {
			if (targetAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
				deactivateTillDeathDoUsPart(target, TillDeathDoUsPartEndReason.MANUAL_CANCEL, true);
			} else {
				TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.remove(targetId);
			}
		}

		if (!protectedActiveAbility) {
			if (targetAbility == MagicAbility.IGNITION) {
				endIgnition(target, currentTick, BurningPassionIgnitionEndReason.LOCKED, true, false);
			}
			if (targetAbility == MagicAbility.BELOW_FREEZING) {
				endFrostStagedMode(target, currentTick, FrostStageEndReason.LOCKED, true, false);
			}
			if (targetAbility == MagicAbility.ABSOLUTE_ZERO) {
				deactivateAbsoluteZero(target);
			}
			if (targetAbility == MagicAbility.PLANCK_HEAT) {
				deactivatePlanckHeat(target);
			}
			if (targetAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
				deactivateLoveAtFirstSight(target);
			}
			if (targetAbility == MagicAbility.MANIPULATION) {
				deactivateManipulation(target, true, "suppressed by bankruptcy");
			}
			if (targetAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
				deactivateHerculesBurden(target, true, false);
			}
			if (targetAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
				clearSagittariusWindup(target);
			}
			if (targetAbility == MagicAbility.ORIONS_GAMBIT) {
				endOrionsGambit(target, OrionGambitEndReason.MANUAL_CANCEL, currentTick, false);
			}
			if (isDomainExpansion(targetAbility) && !preserveDomainAbilities) {
				deactivateDomainExpansion(target);
			}
		}

		GreedAbilityRuntime.cancelActiveAbilities(target, currentTick);

		if (!protectedActiveAbility && targetAbility != MagicAbility.NONE) {
			setActiveAbility(target, MagicAbility.NONE);
		}
	}
}


