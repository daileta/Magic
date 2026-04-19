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
import net.evan.magic.magic.core.MagicPlayerData;
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

abstract class GreedDomainPenaltyLogic extends GreedDomainSupport {
	static void updateFrozenTributeDisplays(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<OwnerTargetKey, FrozenTributeState>> iterator = FROZEN_TRIBUTES.entrySet().iterator();
		while (iterator.hasNext()) {
			FrozenTributeState state = iterator.next().getValue();
			if (currentTick >= state.expiresTick) {
				removeDisplayEntity(server, state.dimension, state.displayEntityId);
				iterator.remove();
				continue;
			}

			ServerWorld world = server.getWorld(state.dimension);
			if (world == null) {
				continue;
			}

			Entity entity = world.getEntity(state.targetId);
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
				removeDisplayEntity(server, state.dimension, state.displayEntityId);
				iterator.remove();
				continue;
			}

			state.displayEntityId = syncTributeDisplay(world, target, state.displayEntityId, state.tributeUnits, state.displayPrefix);
		}
	}

	static void cleanupExpiredDebtPenalties(int currentTick) {
		Iterator<Map.Entry<UUID, ArrayList<DebtPenaltyState>>> iterator = DEBT_PENALTIES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ArrayList<DebtPenaltyState>> entry = iterator.next();
			entry.getValue().removeIf(state -> state.isExpired(currentTick));
			if (entry.getValue().isEmpty()) {
				iterator.remove();
			}
		}
	}

	static void cleanupPersistentMarksForLowMana(MinecraftServer server) {
		for (Map.Entry<UUID, Map<UUID, PersistentMarkState>> ownerEntry : PERSISTENT_MARKS_BY_OWNER.entrySet()) {
			ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerEntry.getKey());
			if (owner == null || !owner.isAlive()) {
				continue;
			}

			double manaPercent = MagicPlayerData.getMana(owner) * 100.0 / MagicPlayerData.MAX_MANA;
			Iterator<Map.Entry<UUID, PersistentMarkState>> iterator = ownerEntry.getValue().entrySet().iterator();
			while (iterator.hasNext()) {
				PersistentMarkState markState = iterator.next().getValue();
				if (markState.indebted && manaPercent <= markState.lowManaCleanupThresholdPercent) {
					markState.indebted = false;
				}
				if (!markState.indebted && !markState.collected) {
					iterator.remove();
				}
			}
		}
		PERSISTENT_MARKS_BY_OWNER.entrySet().removeIf(entry -> entry.getValue().isEmpty());
	}

	static void syncPenaltyAttributeModifiers(MinecraftServer server, int currentTick) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			AggregatedPenaltyState penaltyState = aggregatePenaltyState(player.getUuid(), currentTick);
			applyPenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), GREED_ATTACK_DAMAGE_MODIFIER_ID, penaltyState.outgoingDamageMultiplier - 1.0);
			applyPenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.ARMOR), GREED_ARMOR_MODIFIER_ID, penaltyState.armorEffectivenessMultiplier - 1.0);
			applyPenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.ARMOR_TOUGHNESS), GREED_ARMOR_TOUGHNESS_MODIFIER_ID, penaltyState.armorEffectivenessMultiplier - 1.0);
			applyPenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), GREED_MOVEMENT_SPEED_MODIFIER_ID, penaltyState.movementSpeedMultiplier - 1.0);
			applyPenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_SPEED), GREED_ATTACK_SPEED_MODIFIER_ID, penaltyState.attackSpeedMultiplier - 1.0);
			applyPenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), GREED_MAX_HEALTH_MODIFIER_ID, penaltyState.maxHealthMultiplier - 1.0);
			applyPenaltyAttributeModifier(
				player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE),
				GREED_KNOCKBACK_RESISTANCE_MODIFIER_ID,
				penaltyState.knockbackResistanceMultiplier - 1.0
			);
			applyPenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.SCALE), GREED_SCALE_MODIFIER_ID, penaltyState.scaleMultiplier - 1.0);
			if (player.getHealth() > player.getMaxHealth()) {
				player.setHealth(player.getMaxHealth());
			}
		}
	}

	static AggregatedPenaltyState aggregatePenaltyState(UUID targetId, int currentTick) {
		ArrayList<DebtPenaltyState> penalties = DEBT_PENALTIES.get(targetId);
		if (penalties == null || penalties.isEmpty()) {
			return AggregatedPenaltyState.NONE;
		}

		double manaRegenMultiplier = 1.0;
		double manaCostMultiplier = 1.0;
		double cooldownMultiplier = 1.0;
		double outgoingDamageMultiplier = 1.0;
		double armorEffectivenessMultiplier = 1.0;
		double movementSpeedMultiplier = 1.0;
		double attackSpeedMultiplier = 1.0;
		double maxHealthMultiplier = 1.0;
		double knockbackResistanceMultiplier = 1.0;
		double scaleMultiplier = 1.0;
		double successChance = 1.0;
		boolean sealed = false;
		BackfireState backfireState = null;

		for (DebtPenaltyState penalty : penalties) {
			if (currentTick < penalty.manaRegenEndTick) {
				manaRegenMultiplier *= penalty.manaRegenMultiplier;
			}
			if (currentTick < penalty.manaCostEndTick) {
				manaCostMultiplier *= penalty.manaCostMultiplier;
			}
			if (currentTick < penalty.cooldownEndTick) {
				cooldownMultiplier *= penalty.cooldownMultiplier;
			}
			if (currentTick < penalty.statPenaltyEndTick) {
				outgoingDamageMultiplier *= penalty.outgoingDamageMultiplier;
				armorEffectivenessMultiplier *= penalty.armorEffectivenessMultiplier;
				movementSpeedMultiplier *= penalty.movementSpeedMultiplier;
				attackSpeedMultiplier *= penalty.attackSpeedMultiplier;
				maxHealthMultiplier *= penalty.maxHealthMultiplier;
				knockbackResistanceMultiplier *= penalty.knockbackResistanceMultiplier;
				scaleMultiplier *= penalty.scaleMultiplier;
			}
			if (currentTick < penalty.castFailureEndTick) {
				successChance *= 1.0 - MathHelper.clamp(penalty.castFailureChance, 0.0, 1.0);
				if (penalty.backfire != null && (backfireState == null || penalty.backfire.chance > backfireState.chance)) {
					backfireState = penalty.backfire;
				}
			}
			if (currentTick < penalty.magicSealEndTick) {
				sealed = true;
			}
		}

		return new AggregatedPenaltyState(
			Math.max(0.0, manaRegenMultiplier),
			Math.max(0.0, manaCostMultiplier),
			Math.max(0.0, cooldownMultiplier),
			Math.max(0.0, outgoingDamageMultiplier),
			Math.max(0.0, armorEffectivenessMultiplier),
			Math.max(0.0, movementSpeedMultiplier),
			Math.max(0.0, attackSpeedMultiplier),
			Math.max(0.0, maxHealthMultiplier),
			Math.max(0.0, knockbackResistanceMultiplier),
			Math.max(0.1, scaleMultiplier),
			MathHelper.clamp(1.0 - successChance, 0.0, 1.0),
			sealed,
			backfireState
		);
	}

	static void addDebtPenalty(UUID targetId, DebtPenaltyState penaltyState) {
		if (penaltyState == null || !penaltyState.hasAnyTimedEffect()) {
			return;
		}
		DEBT_PENALTIES.computeIfAbsent(targetId, ignored -> new ArrayList<>()).add(penaltyState);
	}

	static PersistentMarkState persistentMarkState(UUID ownerId, UUID targetId) {
		Map<UUID, PersistentMarkState> marks = PERSISTENT_MARKS_BY_OWNER.get(ownerId);
		return marks == null ? null : marks.get(targetId);
	}

	static void removePersistentMarksOwnedBy(UUID ownerId) {
		PERSISTENT_MARKS_BY_OWNER.remove(ownerId);
	}

	static void applyDebtCollection(MinecraftServer server, UUID casterId, UUID targetId, int frozenTributeUnits, int currentTick) {
		ServerPlayerEntity caster = server.getPlayerManager().getPlayer(casterId);
		ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetId);
		if (caster == null || target == null || !target.isAlive()) {
			return;
		}

		int wholeTribute = frozenTributeUnits / COIN_UNITS_PER_COIN;
		MagicConfig.GreedDomainConfig config = MagicConfig.get().greed.domain;
		if (wholeTribute <= 0) {
			applyTotalCollection(caster, target, config.totalCollection, currentTick);
			return;
		}
		if (wholeTribute == 1) {
			applyFinalCollection(caster, target, config.finalCollection, currentTick);
			return;
		}
		if (wholeTribute == 2) {
			applyBankruptcyTier(caster, target, config.bankruptcyTier, currentTick);
			return;
		}
		if (wholeTribute <= 4) {
			applySevereCollection(caster, target, config.severeCollection, currentTick);
			return;
		}
		if (wholeTribute <= 8) {
			applyHeavyDebt(caster, target, config.heavyDebt, currentTick);
			return;
		}
		if (wholeTribute <= 13) {
			applyStandardDebt(target, config.standardDebt, currentTick);
			return;
		}
		applyLightDebt(target, config.lightDebt, currentTick);
	}

	static void applyLightDebt(ServerPlayerEntity target, MagicConfig.GreedDomainLightDebtConfig config, int currentTick) {
		applyManaSicknessRange(target, config.manaSicknessAmplifierRange, config.manaSicknessDurationTicks);
		applyDurabilityLoss(target, stack -> GreedItemCategoryHelper.isArmor(stack) || GreedItemCategoryHelper.isWeapon(stack), config.durabilityLossFraction);
		DebtPenaltyState penaltyState = new DebtPenaltyState();
		penaltyState.manaRegenMultiplier = config.manaRegenMultiplier;
		penaltyState.manaRegenEndTick = currentTick + config.manaRegenPenaltyDurationTicks;
		penaltyState.castFailureChance = config.castFailureChance;
		penaltyState.castFailureEndTick = currentTick + config.castFailureDurationTicks;
		penaltyState.outgoingDamageMultiplier = config.weaponDamageMultiplier;
		penaltyState.armorEffectivenessMultiplier = config.armorEffectivenessMultiplier;
		penaltyState.statPenaltyEndTick = currentTick + config.statPenaltyDurationTicks;
		addDebtPenalty(target.getUuid(), penaltyState);
	}

	static boolean isIntroActive(ActiveDomainState state, int currentTick) {
		return state != null && currentTick < state.introEndTick;
	}

	static ActiveDomainState activeIntroStateForTarget(
		UUID targetId,
		net.minecraft.registry.RegistryKey<World> dimension,
		int currentTick
	) {
		for (ActiveDomainState state : ACTIVE_DOMAINS.values()) {
			if (!state.dimension.equals(dimension) || !isIntroActive(state, currentTick) || !state.tributeByTarget.containsKey(targetId)) {
				continue;
			}
			return state;
		}
		return null;
	}

	static void captureIntroFreezeTarget(ActiveDomainState state, ServerPlayerEntity player) {
		state.introFreezeByTarget.putIfAbsent(player.getUuid(), IntroFreezeState.capture(player));
	}

	static void enforceIntroFreeze(ServerWorld world, ActiveDomainState state) {
		Iterator<Map.Entry<UUID, IntroFreezeState>> iterator = state.introFreezeByTarget.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, IntroFreezeState> entry = iterator.next();
			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof ServerPlayerEntity player) || !player.isAlive() || player.isSpectator()) {
				iterator.remove();
				continue;
			}
			enforceIntroFreeze(player, entry.getValue());
		}
	}

	static void enforceIntroFreeze(ServerPlayerEntity player, IntroFreezeState freezeState) {
		if (player.hasVehicle()) {
			player.stopRiding();
		}
		player.stopUsingItem();
		player.setSprinting(false);
		player.setVelocity(Vec3d.ZERO);
		player.setOnGround(true);
		boolean positionChanged = player.squaredDistanceTo(freezeState.x, freezeState.y, freezeState.z) > INTRO_FREEZE_POSITION_EPSILON_SQUARED;
		boolean rotationChanged = angleDeltaDegrees(player.getYaw(), freezeState.yaw) > INTRO_FREEZE_ROTATION_EPSILON_DEGREES
			|| angleDeltaDegrees(player.getPitch(), freezeState.pitch) > INTRO_FREEZE_ROTATION_EPSILON_DEGREES;
		if (positionChanged) {
			player.requestTeleport(freezeState.x, freezeState.y, freezeState.z);
		}
		player.setYaw(freezeState.yaw);
		player.setPitch(freezeState.pitch);
		player.setHeadYaw(freezeState.yaw);
		player.setBodyYaw(freezeState.yaw);
		if (positionChanged || rotationChanged) {
			player.networkHandler.requestTeleport(freezeState.x, freezeState.y, freezeState.z, freezeState.yaw, freezeState.pitch);
		}
	}

	static float angleDeltaDegrees(float current, float target) {
		return Math.abs(MathHelper.wrapDegrees(target - current));
	}

	static void applyStandardDebt(ServerPlayerEntity target, MagicConfig.GreedDomainStandardDebtConfig config, int currentTick) {
		applyManaSicknessRange(target, config.manaSicknessAmplifierRange, config.manaSicknessDurationTicks);
		if (config.artifactBurden.enabled && config.artifactBurden.durationTicks > 0) {
			int artifactCount = countArtifacts(target);
			int slownessAmplifier = artifactBurdenAmplifier(config.artifactBurden, artifactCount);
			if (artifactCount > 0 && slownessAmplifier >= 0) {
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, config.artifactBurden.durationTicks, slownessAmplifier, true, true, true));
			}
		}
		applyRandomDurabilityLoss(target, stack -> matchesDurabilityTarget(config.qualifyingItemCategories, stack), config.durabilityLossFractionRange);
		DebtPenaltyState penaltyState = new DebtPenaltyState();
		penaltyState.manaCostMultiplier = config.manaCostMultiplier;
		penaltyState.manaCostEndTick = currentTick + config.manaCostPenaltyDurationTicks;
		addDebtPenalty(target.getUuid(), penaltyState);
	}

	static void applyHeavyDebt(ServerPlayerEntity caster, ServerPlayerEntity target, MagicConfig.GreedDomainHeavyDebtConfig config, int currentTick) {
		applyManaSicknessRange(target, config.manaSicknessAmplifierRange, config.manaSicknessDurationTicks);
		DebtPenaltyState penaltyState = new DebtPenaltyState();
		penaltyState.castFailureChance = config.castFailureChance;
		penaltyState.castFailureEndTick = currentTick + config.castFailureDurationTicks;
		penaltyState.cooldownMultiplier = config.cooldownMultiplier;
		penaltyState.cooldownEndTick = currentTick + config.cooldownPenaltyDurationTicks;
		addDebtPenalty(target.getUuid(), penaltyState);
		int seizureCount = randomInt(config.forcedTaxationItemCountRange.min, config.forcedTaxationItemCountRange.max, target.getRandom());
		ArrayList<ItemStack> seizedItems = seizeRandomInventoryItems(target, config.allowedSeizureCategories, seizureCount);
		transferItemsToCaster(caster, seizedItems);
		int convertedCoinUnits = Math.max(0, (int) Math.round(seizedItems.size() * GreedAbilityRuntime.coinUnitsFromConfiguredCoinsValue(config.coinConversion.coinsPerSeizedStack)));
		if (convertedCoinUnits > 0) {
			GreedAbilityRuntime.grantAnonymousCoinUnits(caster, convertedCoinUnits, currentTick, config.coinConversion.allowOverflowBeyondPouchCap);
		}
	}

	static void applySevereCollection(ServerPlayerEntity caster, ServerPlayerEntity target, MagicConfig.GreedDomainSevereCollectionConfig config, int currentTick) {
		applyManaSicknessLevel(target, config.manaSicknessAmplifier, config.manaSicknessDurationTicks);
		DebtPenaltyState penaltyState = new DebtPenaltyState();
		penaltyState.castFailureChance = config.castFailureChance;
		penaltyState.castFailureEndTick = currentTick + config.castFailureDurationTicks;
		penaltyState.backfire = BackfireState.fromConfig(config.backfire);
		addDebtPenalty(target.getUuid(), penaltyState);
		transferItemsToCaster(caster, List.of(seizeSingleMajorItem(target, config.seizureItemCategorySelectionRules)));
	}

	static void applyBankruptcyTier(ServerPlayerEntity caster, ServerPlayerEntity target, MagicConfig.GreedDomainBankruptcyTierConfig config, int currentTick) {
		DebtPenaltyState penaltyState = new DebtPenaltyState();
		penaltyState.magicSealEndTick = currentTick + config.magicSealDurationTicks;
		penaltyState.outgoingDamageMultiplier = config.statSuppressionAttributeMultipliers.outgoingDamageMultiplier;
		penaltyState.armorEffectivenessMultiplier = config.statSuppressionAttributeMultipliers.armorEffectivenessMultiplier;
		penaltyState.movementSpeedMultiplier = config.statSuppressionAttributeMultipliers.movementSpeedMultiplier;
		penaltyState.attackSpeedMultiplier = config.statSuppressionAttributeMultipliers.attackSpeedMultiplier;
		penaltyState.maxHealthMultiplier = config.statSuppressionAttributeMultipliers.maxHealthMultiplier;
		penaltyState.knockbackResistanceMultiplier = config.statSuppressionAttributeMultipliers.knockbackResistanceMultiplier;
		penaltyState.scaleMultiplier = config.statSuppressionAttributeMultipliers.scaleMultiplier;
		penaltyState.statPenaltyEndTick = currentTick + config.statSuppressionDurationTicks;
		addDebtPenalty(target.getUuid(), penaltyState);
		transferItemsToCaster(
			caster,
			seizeRandomInventoryItems(target, config.seizureCategoryFilters, randomInt(config.seizureItemCountRange.min, config.seizureItemCountRange.max, target.getRandom()))
		);
		applyIndebtedMark(caster.getUuid(), target.getUuid(), config.indebted);
	}

	static void applyFinalCollection(ServerPlayerEntity caster, ServerPlayerEntity target, MagicConfig.GreedDomainFinalCollectionConfig config, int currentTick) {
		DebtPenaltyState penaltyState = new DebtPenaltyState();
		penaltyState.magicSealEndTick = currentTick + config.magicSealDurationTicks;
		penaltyState.outgoingDamageMultiplier = config.statSuppressionAttributeMultipliers.outgoingDamageMultiplier;
		penaltyState.armorEffectivenessMultiplier = config.statSuppressionAttributeMultipliers.armorEffectivenessMultiplier;
		penaltyState.movementSpeedMultiplier = config.statSuppressionAttributeMultipliers.movementSpeedMultiplier;
		penaltyState.attackSpeedMultiplier = config.statSuppressionAttributeMultipliers.attackSpeedMultiplier;
		penaltyState.maxHealthMultiplier = config.statSuppressionAttributeMultipliers.maxHealthMultiplier;
		penaltyState.knockbackResistanceMultiplier = config.statSuppressionAttributeMultipliers.knockbackResistanceMultiplier;
		penaltyState.scaleMultiplier = config.statSuppressionAttributeMultipliers.scaleMultiplier;
		penaltyState.statPenaltyEndTick = currentTick + config.statSuppressionDurationTicks;
		addDebtPenalty(target.getUuid(), penaltyState);
		ArrayList<ItemStack> seizedItems = new ArrayList<>(seizeArmorForFinalCollection(target, config));
		seizedItems.addAll(
			seizeRandomInventoryItems(
				target,
				config.randomItemSeizureFilters,
				randomInt(config.randomItemSeizureCountRange.min, config.randomItemSeizureCountRange.max, target.getRandom())
			)
		);
		ItemStack weapon = seizePreferredWeapon(target, config.preferMainHandWeapon, config.allowInventoryWeaponFallback);
		if (!weapon.isEmpty()) {
			seizedItems.add(weapon);
		}
		if (target.getRandom().nextDouble() < config.artifactSeizureChance) {
			ItemStack artifact = seizeSingleArtifact(target);
			if (!artifact.isEmpty()) {
				seizedItems.add(artifact);
			}
		}
		transferItemsToCaster(caster, seizedItems);
		applyIndebtedMark(caster.getUuid(), target.getUuid(), config.indebted);
	}

	static void applyTotalCollection(ServerPlayerEntity caster, ServerPlayerEntity target, MagicConfig.GreedDomainTotalCollectionConfig config, int currentTick) {
		DebtPenaltyState penaltyState = new DebtPenaltyState();
		penaltyState.magicSealEndTick = currentTick + config.magicSealDurationTicks;
		penaltyState.outgoingDamageMultiplier = config.severeStatSuppressionAttributeMultipliers.outgoingDamageMultiplier;
		penaltyState.armorEffectivenessMultiplier = config.severeStatSuppressionAttributeMultipliers.armorEffectivenessMultiplier;
		penaltyState.movementSpeedMultiplier = config.severeStatSuppressionAttributeMultipliers.movementSpeedMultiplier;
		penaltyState.attackSpeedMultiplier = config.severeStatSuppressionAttributeMultipliers.attackSpeedMultiplier;
		penaltyState.maxHealthMultiplier = config.severeStatSuppressionAttributeMultipliers.maxHealthMultiplier;
		penaltyState.knockbackResistanceMultiplier = config.severeStatSuppressionAttributeMultipliers.knockbackResistanceMultiplier;
		penaltyState.scaleMultiplier = config.severeStatSuppressionAttributeMultipliers.scaleMultiplier;
		penaltyState.statPenaltyEndTick = currentTick + config.severeStatSuppressionDurationTicks;
		addDebtPenalty(target.getUuid(), penaltyState);
		ArrayList<ItemStack> armorItems = seizeAllMatchingItems(target, GreedItemCategoryHelper::isArmor, false);
		ArrayList<ItemStack> seizedItems = new ArrayList<>(armorItems);
		ArrayList<ItemStack> weaponItems = seizeAllMatchingItems(target, GreedItemCategoryHelper::isWeapon, false);
		seizedItems.addAll(weaponItems);
		seizedItems.addAll(seizeHalfInventory(target, config.halfInventorySeizureFraction, config.halfInventorySeizureFilters));
		seizedItems.addAll(seizeArtifactsForTotalCollection(target, config));
		transferItemsToCaster(caster, seizedItems);
		int seizedEquipmentCount = 0;
		for (ItemStack stack : armorItems) {
			if (!stack.isEmpty() && GreedItemCategoryHelper.isEquipment(stack)) {
				seizedEquipmentCount++;
			}
		}
		for (ItemStack stack : weaponItems) {
			if (!stack.isEmpty() && GreedItemCategoryHelper.isEquipment(stack)) {
				seizedEquipmentCount++;
			}
		}
		int equipmentCoinUnits = Math.max(0, seizedEquipmentCount * GreedAbilityRuntime.coinUnitsFromConfiguredCoinsValue(config.equipmentCoinRewardPerSeizedItem));
		if (equipmentCoinUnits > 0) {
			GreedAbilityRuntime.grantAnonymousCoinUnits(caster, equipmentCoinUnits, currentTick, config.equipmentCoinRewardUsesTemporaryOverflowStack);
		}
		applyIndebtedMark(caster.getUuid(), target.getUuid(), config.indebted);
		applyCollectedMark(caster.getUuid(), target.getUuid(), config.collected);
	}

	static void applyIndebtedMark(UUID ownerId, UUID targetId, MagicConfig.GreedDomainIndebtedConfig config) {
		if (!config.enabled) {
			return;
		}
		PersistentMarkState state = PERSISTENT_MARKS_BY_OWNER.computeIfAbsent(ownerId, ignored -> new HashMap<>()).computeIfAbsent(targetId, ignored -> new PersistentMarkState());
		state.indebted = true;
		state.lowManaCleanupThresholdPercent = config.casterLowManaCleanupThresholdPercent;
		state.uniqueTargetCapBypassEnabled = config.uniqueTargetCapBypassEnabled;
	}

	static void applyCollectedMark(UUID ownerId, UUID targetId, MagicConfig.GreedDomainCollectedConfig config) {
		if (!config.enabled) {
			return;
		}
		PersistentMarkState state = PERSISTENT_MARKS_BY_OWNER.computeIfAbsent(ownerId, ignored -> new HashMap<>()).computeIfAbsent(targetId, ignored -> new PersistentMarkState());
		state.collected = true;
		state.futureReducedTributeUnits = config.futureReducedTributeStartingValue * COIN_UNITS_PER_COIN;
		state.coinExtractionMultiplier = config.coinExtractionMultiplier;
		state.tributeLossMultiplier = config.tributeLossMultiplier;
		state.residualExtractionEnabled = config.residualExtractionEnabled;
		state.residualExtractionTriggerCoinUnits = new HashMap<>();
		for (Map.Entry<String, Double> entry : config.residualExtractionTriggers.entrySet()) {
			state.residualExtractionTriggerCoinUnits.put(entry.getKey(), GreedAbilityRuntime.coinUnitsFromConfiguredCoinsValue(entry.getValue()));
		}
	}

	static int startingTributeUnits(UUID ownerId, UUID targetId) {
		int defaultUnits = MagicConfig.get().greed.domain.tribute.initialTribute * COIN_UNITS_PER_COIN;
		PersistentMarkState markState = persistentMarkState(ownerId, targetId);
		if (markState == null || !markState.collected || markState.futureReducedTributeUnits <= 0) {
			return defaultUnits;
		}
		return Math.min(defaultUnits, markState.futureReducedTributeUnits);
	}

	static int adjustTributeLossUnits(UUID actorId, UUID ownerId, int configuredCoinUnits, int currentTick) {
		PersistentMarkState markState = persistentMarkState(ownerId, actorId);
		if (markState == null || !markState.collected) {
			return configuredCoinUnits;
		}
		return Math.max(0, (int) Math.round(configuredCoinUnits * markState.tributeLossMultiplier));
	}

	static void applyManaSicknessRange(ServerPlayerEntity target, MagicConfig.GreedDomainIntRangeConfig levelRange, int durationTicks) {
		if (durationTicks <= 0) {
			return;
		}
		applyManaSicknessLevel(target, randomInt(levelRange.min, levelRange.max, target.getRandom()), durationTicks);
	}

	static void applyManaSicknessLevel(ServerPlayerEntity target, int level, int durationTicks) {
		if (durationTicks <= 0 || level <= 0) {
			return;
		}
		target.addStatusEffect(new StatusEffectInstance(ModStatusEffects.manaSicknessEntry(), durationTicks, Math.max(0, level - 1), true, true, true));
	}
}



