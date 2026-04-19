package net.evan.magic.magic.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.evan.magic.Magic;
import net.evan.magic.config.MagicConfig;
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.mixin.ArmorStandEntityAccessorMixin;
import net.evan.magic.network.payload.GreedDomainWarningOverlayPayload;
import net.evan.magic.registry.ModStatusEffects;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

final class GreedDomainRuntime extends GreedDomainRuntimeLogic {
	private GreedDomainRuntime() {
	}

	static void onDomainActivated(
		ServerPlayerEntity caster,
		ServerWorld world,
		double centerX,
		double centerZ,
		int baseY,
		int innerRadius,
		int innerHeight,
		Iterable<UUID> capturedEntityIds,
		int currentTick
	) {
		ensureInitialized();
		MagicConfig.GreedDomainConfig config = MagicConfig.get().greed.domain;
		MagicConfig.GreedDomainActivationWarningConfig warningConfig = config.activationWarning;
		boolean introEnabled = warningConfig.enabled && warningConfig.durationTicks > 0;
		ActiveDomainState state = new ActiveDomainState(world.getRegistryKey(), centerX, centerZ, baseY, innerRadius, innerHeight);
		state.nextPassiveIncomeTick = currentTick + Math.max(1, config.passiveIncome.passiveCoinIntervalTicks);
		state.introEndTick = introEnabled ? currentTick + warningConfig.durationTicks : currentTick;
		state.pauseDomainTimerDuringIntro = introEnabled && warningConfig.pauseDomainTimerDuringDisplay;
		state.freezePlayersDuringIntro = introEnabled && warningConfig.freezePlayersDuringDisplay;
		state.preventDamageDuringIntro = introEnabled && warningConfig.preventDamageDuringDisplay;
		for (UUID entityId : capturedEntityIds) {
			Entity entity = world.getEntity(entityId);
			if (entity instanceof ServerPlayerEntity targetPlayer && targetPlayer != caster && targetPlayer.isAlive() && !targetPlayer.isSpectator()) {
				if (introEnabled) {
					sendActivationWarning(targetPlayer);
				}
				if (state.freezePlayersDuringIntro) {
					captureIntroFreezeTarget(state, targetPlayer);
				}
			}
			if (!(entity instanceof LivingEntity living) || living == caster || !living.isAlive() || !isValidTributeTarget(living)) {
				continue;
			}
			state.tributeByTarget.put(living.getUuid(), new TributeState(startingTributeUnits(caster.getUuid(), living.getUuid()), null));
		}
		ACTIVE_DOMAINS.put(caster.getUuid(), state);
	}

	private static void sendActivationWarning(ServerPlayerEntity target) {
		MagicConfig.GreedDomainActivationWarningConfig config = MagicConfig.get().greed.domain.activationWarning;
		if (!config.enabled || config.durationTicks <= 0) {
			return;
		}
		GreedDomainWarningOverlayPayload payload = new GreedDomainWarningOverlayPayload(
			String.join("\n", config.lines),
			parseHexColor(config.textColorHex, 0xFFE7A4),
			parseHexColor(config.outlineColorHex, 0x000000),
			config.scale,
			config.durationTicks,
			config.lineSpacing
		);
		ServerPlayNetworking.send(target, payload);
	}

	static void updateActiveDomain(
		MinecraftServer server,
		ServerWorld world,
		UUID casterId,
		double centerX,
		double centerZ,
		int baseY,
		int innerRadius,
		int innerHeight,
		int currentTick
	) {
		ensureInitialized();
		ServerPlayerEntity caster = server.getPlayerManager().getPlayer(casterId);
		if (caster == null || !caster.isAlive() || caster.isSpectator()) {
			return;
		}

		MagicConfig.GreedDomainConfig config = MagicConfig.get().greed.domain;
		ActiveDomainState state = ACTIVE_DOMAINS.computeIfAbsent(
			casterId,
			ignored -> new ActiveDomainState(world.getRegistryKey(), centerX, centerZ, baseY, innerRadius, innerHeight)
		);
		state.dimension = world.getRegistryKey();
		state.centerX = centerX;
		state.centerZ = centerZ;
		state.baseY = baseY;
		state.innerRadius = innerRadius;
		state.innerHeight = innerHeight;
		if (state.nextPassiveIncomeTick <= 0) {
			state.nextPassiveIncomeTick = currentTick + Math.max(1, config.passiveIncome.passiveCoinIntervalTicks);
		}
		boolean introActive = isIntroActive(state, currentTick);

		HashSet<UUID> visibleTributeTargets = new HashSet<>();
		for (LivingEntity target : collectTributeTargetsInsideDomain(world, casterId, centerX, centerZ, baseY, innerRadius, innerHeight)) {
			visibleTributeTargets.add(target.getUuid());
			TributeState tributeState = state.tributeByTarget.computeIfAbsent(
				target.getUuid(),
				ignored -> new TributeState(startingTributeUnits(casterId, target.getUuid()), null)
			);
			tributeState.displayEntityId = syncTributeDisplay(world, target, tributeState.displayEntityId, tributeState.tributeUnits, config.tribute.displayPrefix);
			if (!introActive) {
				applyActiveDomainEffects(target);
			} else if (state.freezePlayersDuringIntro && target instanceof ServerPlayerEntity targetPlayer) {
				captureIntroFreezeTarget(state, targetPlayer);
			}
		}

		for (Map.Entry<UUID, TributeState> entry : state.tributeByTarget.entrySet()) {
			if (visibleTributeTargets.contains(entry.getKey())) {
				continue;
			}
			removeDisplayEntity(server, state.dimension, entry.getValue().displayEntityId);
			entry.getValue().displayEntityId = null;
		}
		if (introActive) {
			if (state.freezePlayersDuringIntro) {
				enforceIntroFreeze(world, state);
			}
			state.nextPassiveIncomeTick = currentTick + Math.max(1, config.passiveIncome.passiveCoinIntervalTicks);
			return;
		}

		int passiveIntervalTicks = Math.max(1, config.passiveIncome.passiveCoinIntervalTicks);
		int passiveCoinUnitsPerEntity = intervalCoinUnits(config.passiveIncome.passiveCoinsPerSecondPerOtherPlayer, passiveIntervalTicks);
		while (currentTick >= state.nextPassiveIncomeTick) {
			if (passiveCoinUnitsPerEntity > 0) {
				int passiveTargetCount = countPassiveIncomeTargets(world, casterId, centerX, centerZ, baseY, innerRadius, innerHeight, config.passiveIncome.countMobs);
				if (passiveTargetCount > 0) {
					GreedRuntime.grantAnonymousCoinUnits(caster, passiveTargetCount * passiveCoinUnitsPerEntity, state.nextPassiveIncomeTick, false);
				}
			}
			state.nextPassiveIncomeTick += passiveIntervalTicks;
		}
	}

	static boolean shouldPauseDomainTimer(UUID casterId, int currentTick) {
		ensureInitialized();
		ActiveDomainState state = ACTIVE_DOMAINS.get(casterId);
		return state != null && state.pauseDomainTimerDuringIntro && isIntroActive(state, currentTick);
	}

	static boolean isPlayerFrozenDuringIntro(ServerPlayerEntity player, int currentTick) {
		if (player == null) {
			return false;
		}
		if (MagicPlayerData.isDomainClashActive(player)) {
			return false;
		}
		ActiveDomainState state = activeIntroStateForTarget(player.getUuid(), player.getEntityWorld().getRegistryKey(), currentTick);
		return state != null && state.freezePlayersDuringIntro && state.introFreezeByTarget.containsKey(player.getUuid());
	}

	static boolean isPlayerInvulnerableDuringIntro(ServerPlayerEntity player, int currentTick) {
		if (player == null) {
			return false;
		}
		if (MagicPlayerData.isDomainClashActive(player)) {
			return false;
		}
		ActiveDomainState state = activeIntroStateForTarget(player.getUuid(), player.getEntityWorld().getRegistryKey(), currentTick);
		return state != null && state.preventDamageDuringIntro;
	}

	static void onDomainEnded(
		MinecraftServer server,
		UUID casterId,
		ServerWorld world,
		double centerX,
		double centerZ,
		int baseY,
		int innerRadius,
		int innerHeight,
		int currentTick
	) {
		ensureInitialized();
		ActiveDomainState state = ACTIVE_DOMAINS.remove(casterId);
		if (state == null) {
			return;
		}

		int displayExpireTick = currentTick + Math.max(0, MagicConfig.get().greed.domain.tribute.tributeDisplayPersistAfterEndTicks);
		for (Map.Entry<UUID, TributeState> entry : state.tributeByTarget.entrySet()) {
			FROZEN_TRIBUTES.put(
				new OwnerTargetKey(casterId, entry.getKey()),
				new FrozenTributeState(
					state.dimension,
					entry.getKey(),
					entry.getValue().tributeUnits,
					entry.getValue().displayEntityId,
					displayExpireTick,
					MagicConfig.get().greed.domain.tribute.displayPrefix
				)
			);
			applyDebtCollection(server, casterId, entry.getKey(), entry.getValue().tributeUnits, currentTick);
		}
	}

	static void onEndServerTick(MinecraftServer server, int currentTick) {
		updateFrozenTributeDisplays(server, currentTick);
		cleanupExpiredDebtPenalties(currentTick);
		cleanupPersistentMarksForLowMana(server);
		syncPenaltyAttributeModifiers(server, currentTick);
	}

	static void onServerStarted(MinecraftServer server) {
		ACTIVE_DOMAINS.clear();
		FROZEN_TRIBUTES.clear();
		DEBT_PENALTIES.clear();
		PERSISTENT_MARKS_BY_OWNER.clear();
		cleanupOrphanTributeDisplays(server);
	}

	static void onServerStopping(MinecraftServer server) {
		ACTIVE_DOMAINS.clear();
		FROZEN_TRIBUTES.clear();
		DEBT_PENALTIES.clear();
		PERSISTENT_MARKS_BY_OWNER.clear();
	}

	static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
		if (world == null || chunk == null) {
			return;
		}
		cleanupOrphanTributeDisplays(world, chunkBounds(world, chunk));
	}

	static void onPlayerDeath(ServerPlayerEntity player) {
		MinecraftServer server = player.getEntityWorld().getServer();
		removeDisplaysForTarget(server, player.getUuid());
		removeDisplaysOwnedBy(server, player.getUuid());
		clearDeathEndedDebtPenalties(player.getUuid());
		removePersistentMarksOwnedBy(player.getUuid());
		removePenaltyAttributeModifiers(player);
	}

	static void clearAllRuntimeState(ServerPlayerEntity player) {
		MinecraftServer server = player.getEntityWorld().getServer();
		removeDisplaysForTarget(server, player.getUuid());
		removeDisplaysOwnedBy(server, player.getUuid());
		ACTIVE_DOMAINS.remove(player.getUuid());
		DEBT_PENALTIES.remove(player.getUuid());
		removePersistentMarksOwnedBy(player.getUuid());
		removePenaltyAttributeModifiers(player);
	}

	static void onCoinTrigger(
		MinecraftServer server,
		ServerPlayerEntity actor,
		String triggerId,
		UUID directTargetId,
		int configuredCoinUnits,
		int currentTick,
		Set<UUID> alreadyGrantedCasterIds
	) {
		if (configuredCoinUnits <= 0) {
			return;
		}

		for (Map.Entry<UUID, ActiveDomainState> entry : ACTIVE_DOMAINS.entrySet()) {
			UUID casterId = entry.getKey();
			if (casterId.equals(actor.getUuid())) {
				continue;
			}

			TributeState tributeState = entry.getValue().tributeByTarget.get(actor.getUuid());
			if (tributeState == null) {
				continue;
			}

			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(casterId);
			if (caster == null || !caster.isAlive() || caster.isSpectator() || caster.getEntityWorld() != actor.getEntityWorld()) {
				continue;
			}

			int adjustedCoinUnits = adjustCoinTriggerCoinUnits(actor.getUuid(), casterId, triggerId, configuredCoinUnits, currentTick);
			if (!alreadyGrantedCasterIds.contains(casterId) && adjustedCoinUnits > 0) {
				GreedRuntime.grantCoinUnits(caster, actor.getUuid(), adjustedCoinUnits, currentTick, false);
				alreadyGrantedCasterIds.add(casterId);
			}

			int tributeLossUnits = adjustTributeLossUnits(actor.getUuid(), casterId, configuredCoinUnits, currentTick);
			if (tributeLossUnits > 0) {
				tributeState.tributeUnits = Math.max(0, tributeState.tributeUnits - tributeLossUnits);
			}
		}

		for (Map.Entry<UUID, Map<UUID, PersistentMarkState>> ownerEntry : PERSISTENT_MARKS_BY_OWNER.entrySet()) {
			PersistentMarkState markState = ownerEntry.getValue().get(actor.getUuid());
			if (markState == null || !markState.collected || !markState.residualExtractionEnabled) {
				continue;
			}
			if (!allowsCoinTrigger(actor.getUuid(), ownerEntry.getKey(), directTargetId, currentTick)) {
				continue;
			}
			int residualCoinUnits = markState.residualExtractionTriggerCoinUnits.getOrDefault(triggerId, 0);
			if (residualCoinUnits <= 0) {
				continue;
			}
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(ownerEntry.getKey());
			if (caster == null || !caster.isAlive() || caster.isSpectator()) {
				continue;
			}
			GreedRuntime.grantCoinUnits(
				caster,
				actor.getUuid(),
				Math.max(0, (int) Math.round(residualCoinUnits * markState.coinExtractionMultiplier)),
				currentTick,
				false
			);
		}
	}

	static boolean beforeAbilityUse(ServerPlayerEntity player, MagicAbility ability, int currentTick) {
		AggregatedPenaltyState penaltyState = aggregatePenaltyState(player.getUuid(), currentTick);
		if (penaltyState.castFailureChance <= 0.0 || player.getRandom().nextDouble() >= penaltyState.castFailureChance) {
			return false;
		}

		MagicAbilityManager.forceAbilityCooldown(player.getUuid(), ability, currentTick);
		player.sendMessage(Text.translatable("message.magic.greed.cast_failed"), true);
		if (penaltyState.backfire != null && player.getRandom().nextDouble() < penaltyState.backfire.chance) {
			applyBackfire(player, penaltyState.backfire);
		}
		return true;
	}

	static int adjustManaCost(ServerPlayerEntity player, int baseCost) {
		if (baseCost <= 0) {
			return Math.max(0, baseCost);
		}
		return Math.max(0, (int) Math.ceil(baseCost * aggregatePenaltyState(player.getUuid(), currentTick(player)).manaCostMultiplier));
	}

	static double manaRegenMultiplier(ServerPlayerEntity player, int currentTick) {
		return aggregatePenaltyState(player.getUuid(), currentTick).manaRegenMultiplier;
	}

	static int adjustCooldownTicks(ServerPlayerEntity player, MagicAbility ability, int baseTicks, int currentTick) {
		return adjustCooldownTicks(player.getUuid(), ability, baseTicks, currentTick);
	}

	static int adjustCooldownTicks(UUID playerId, MagicAbility ability, int baseTicks, int currentTick) {
		if (baseTicks <= 0) {
			return Math.max(0, baseTicks);
		}
		return Math.max(0, (int) Math.ceil(baseTicks * aggregatePenaltyState(playerId, currentTick).cooldownMultiplier));
	}

	static boolean isMagicSealed(ServerPlayerEntity player, int currentTick) {
		return aggregatePenaltyState(player.getUuid(), currentTick).sealed;
	}

	static boolean handleIncomingDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return false;
	}

	static boolean allowsCoinTrigger(UUID actorId, UUID ownerId, UUID directTargetId, int currentTick) {
		PersistentMarkState markState = persistentMarkState(ownerId, actorId);
		return markState == null || !markState.indebted || ownerId.equals(directTargetId);
	}

	static int adjustCoinTriggerCoinUnits(UUID actorId, UUID ownerId, String triggerId, int configuredCoinUnits, int currentTick) {
		PersistentMarkState markState = persistentMarkState(ownerId, actorId);
		if (markState == null || !markState.collected) {
			return configuredCoinUnits;
		}
		return Math.max(0, (int) Math.round(configuredCoinUnits * markState.coinExtractionMultiplier));
	}

	static boolean countsTowardUniqueTargetCap(UUID ownerId, UUID targetId) {
		PersistentMarkState markState = persistentMarkState(ownerId, targetId);
		return markState == null || !markState.indebted || !markState.uniqueTargetCapBypassEnabled;
	}
}
