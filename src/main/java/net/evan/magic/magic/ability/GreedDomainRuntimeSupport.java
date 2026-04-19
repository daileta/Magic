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

abstract class GreedDomainRuntimeSupport extends GreedDomainRuntimeState {
	static boolean isArtifactItem(ItemStack stack) {
		return GreedItemCategoryHelper.isArtifact(stack);
	}

	static void applyActiveDomainEffects(LivingEntity target) {
		MagicConfig.GreedDomainTributeConfig tributeConfig = MagicConfig.get().greed.domain.tribute;
		if (tributeConfig.activeWeaknessAmplifier >= 0) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, DISPLAY_REFRESH_TICKS, tributeConfig.activeWeaknessAmplifier, true, true, true));
		}
		if (tributeConfig.activeManaDrainLevel > 0) {
			target.addStatusEffect(
				new StatusEffectInstance(ModStatusEffects.manaSicknessEntry(), DISPLAY_REFRESH_TICKS, Math.max(0, tributeConfig.activeManaDrainLevel - 1), true, true, true)
			);
		}
	}

	static int countArtifacts(ServerPlayerEntity target) {
		int count = 0;
		for (int slot = 0; slot < target.getInventory().size(); slot++) {
			ItemStack stack = target.getInventory().getStack(slot);
			if (!stack.isEmpty() && GreedItemCategoryHelper.isArtifact(stack)) {
				count++;
			}
		}
		return count;
	}

	static int artifactBurdenAmplifier(MagicConfig.GreedDomainArtifactBurdenConfig config, int artifactCount) {
		int amplifier = -1;
		for (Map.Entry<String, Integer> entry : config.slownessByArtifactCount.entrySet()) {
			int requiredCount = Integer.parseInt(entry.getKey());
			if (artifactCount >= requiredCount) {
				amplifier = Math.max(amplifier, entry.getValue());
			}
		}
		return amplifier;
	}

	static boolean matchesDurabilityTarget(MagicConfig.GreedDomainDurabilityTargetConfig config, ItemStack stack) {
		if (stack.isEmpty() || !stack.isDamageable()) {
			return false;
		}
		if (GreedItemCategoryHelper.isArmor(stack)) {
			return config.includeArmor;
		}
		if (GreedItemCategoryHelper.isWeapon(stack)) {
			return config.includeWeapons;
		}
		if (GreedItemCategoryHelper.isTool(stack)) {
			return config.includeTools;
		}
		if (GreedItemCategoryHelper.isArtifact(stack)) {
			return config.includeArtifacts;
		}
		return config.includeOtherDamageables;
	}

	static void applyDurabilityLoss(ServerPlayerEntity target, StackSelector selector, double lossFraction) {
		if (lossFraction <= 0.0) {
			return;
		}
		forEachInventoryStack(target, slotRef -> {
			ItemStack stack = slotRef.stack(target);
			if (!stack.isEmpty() && selector.matches(stack)) {
				damageStack(stack, lossFraction);
			}
		});
		target.currentScreenHandler.sendContentUpdates();
	}

	static void applyRandomDurabilityLoss(ServerPlayerEntity target, StackSelector selector, MagicConfig.GreedDomainDoubleRangeConfig range) {
		forEachInventoryStack(target, slotRef -> {
			ItemStack stack = slotRef.stack(target);
			if (!stack.isEmpty() && selector.matches(stack)) {
				damageStack(stack, randomDouble(range.min, range.max, target.getRandom()));
			}
		});
		target.currentScreenHandler.sendContentUpdates();
	}

	static void damageStack(ItemStack stack, double lossFraction) {
		if (stack.isEmpty() || !stack.isDamageable() || lossFraction <= 0.0) {
			return;
		}
		int maxDamage = stack.getMaxDamage();
		if (maxDamage <= 1) {
			return;
		}
		int loss = Math.max(1, (int) Math.round(maxDamage * MathHelper.clamp(lossFraction, 0.0, 1.0)));
		stack.setDamage(Math.min(maxDamage - 1, stack.getDamage() + loss));
	}

	static ArrayList<ItemStack> seizeRandomInventoryItems(ServerPlayerEntity target, MagicConfig.GreedDomainSeizureFilterConfig filters, int itemCount) {
		ArrayList<InventorySlotRef> candidates = inventoryCandidates(target, filters);
		ArrayList<ItemStack> seizedItems = new ArrayList<>();
		for (int index = 0; index < itemCount && !candidates.isEmpty(); index++) {
			InventorySlotRef slotRef = candidates.remove(target.getRandom().nextInt(candidates.size()));
			ItemStack removedStack = slotRef.remove(target);
			if (!removedStack.isEmpty()) {
				seizedItems.add(removedStack);
			}
		}
		target.currentScreenHandler.sendContentUpdates();
		return seizedItems;
	}

	static ItemStack seizeSingleMajorItem(ServerPlayerEntity target, MagicConfig.GreedDomainMajorSeizureConfig config) {
		ArrayList<InventorySlotRef> candidates = new ArrayList<>();
		candidates.addAll(majorWeaponCandidates(target, config.preferEquippedWeapon, config.allowInventoryFallback));
		candidates.addAll(majorArmorCandidates(target, config.preferEquippedArmor, config.allowInventoryFallback));
		if (candidates.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack removed = candidates.get(target.getRandom().nextInt(candidates.size())).remove(target);
		target.currentScreenHandler.sendContentUpdates();
		return removed;
	}

	static ArrayList<ItemStack> seizeArmorForFinalCollection(ServerPlayerEntity target, MagicConfig.GreedDomainFinalCollectionConfig config) {
		ArrayList<ItemStack> seizedItems = new ArrayList<>();
		if (config.removeWornArmorFirst) {
			for (int slot = ARMOR_SLOT_START; slot < ARMOR_SLOT_END; slot++) {
				ItemStack removed = new InventorySlotRef(slot, InventorySlotType.ARMOR).remove(target);
				if (!removed.isEmpty()) {
					seizedItems.add(removed);
				}
			}
		}
		if (seizedItems.isEmpty() && config.removeArmorFromInventoryIfNoArmorWorn) {
			seizedItems.addAll(seizeAllMatchingItems(target, GreedItemCategoryHelper::isArmor, false));
		}
		target.currentScreenHandler.sendContentUpdates();
		return seizedItems;
	}

	static ItemStack seizePreferredWeapon(ServerPlayerEntity target, boolean preferMainHandWeapon, boolean allowInventoryWeaponFallback) {
		if (preferMainHandWeapon && GreedItemCategoryHelper.isWeapon(target.getMainHandStack())) {
			ItemStack removed = target.getMainHandStack().copy();
			target.getInventory().setStack(target.getInventory().getSelectedSlot(), ItemStack.EMPTY);
			target.currentScreenHandler.sendContentUpdates();
			return removed;
		}
		ArrayList<InventorySlotRef> candidates = majorWeaponCandidates(target, false, allowInventoryWeaponFallback);
		if (candidates.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack removed = candidates.getFirst().remove(target);
		target.currentScreenHandler.sendContentUpdates();
		return removed;
	}

	static ArrayList<ItemStack> seizeHalfInventory(
		ServerPlayerEntity target,
		double fraction,
		MagicConfig.GreedDomainSeizureFilterConfig filters
	) {
		ArrayList<InventorySlotRef> candidates = inventoryCandidates(target, filters);
		candidates.removeIf(slotRef -> GreedItemCategoryHelper.isEquipment(slotRef.stack(target)));
		int seizeCount = Math.max(0, (int) Math.floor(candidates.size() * MathHelper.clamp(fraction, 0.0, 1.0)));
		ArrayList<ItemStack> seizedItems = new ArrayList<>();
		for (int index = 0; index < seizeCount && !candidates.isEmpty(); index++) {
			InventorySlotRef slotRef = candidates.remove(target.getRandom().nextInt(candidates.size()));
			ItemStack removed = slotRef.remove(target);
			if (!removed.isEmpty()) {
				seizedItems.add(removed);
			}
		}
		target.currentScreenHandler.sendContentUpdates();
		return seizedItems;
	}

	static ArrayList<ItemStack> seizeArtifactsForTotalCollection(ServerPlayerEntity target, MagicConfig.GreedDomainTotalCollectionConfig config) {
		ArrayList<InventorySlotRef> candidates = new ArrayList<>();
		forEachInventoryStack(target, slotRef -> {
			if (GreedItemCategoryHelper.isArtifact(slotRef.stack(target))) {
				candidates.add(slotRef);
			}
		});
		if (candidates.isEmpty()) {
			return new ArrayList<>();
		}
		int seizeCount = 0;
		if (candidates.size() > 1) {
			seizeCount = MathHelper.clamp(Math.max(1, config.minimumArtifactSeizureCountIfMultiple), 1, candidates.size());
		} else if (target.getRandom().nextDouble() < config.artifactSeizeChance) {
			seizeCount = 1;
		}
		ArrayList<ItemStack> seizedItems = new ArrayList<>();
		for (int index = 0; index < seizeCount && !candidates.isEmpty(); index++) {
			InventorySlotRef slotRef = candidates.remove(target.getRandom().nextInt(candidates.size()));
			ItemStack removed = slotRef.remove(target);
			if (!removed.isEmpty()) {
				seizedItems.add(removed);
			}
		}
		target.currentScreenHandler.sendContentUpdates();
		return seizedItems;
	}

	static ItemStack seizeSingleArtifact(ServerPlayerEntity target) {
		ArrayList<InventorySlotRef> candidates = new ArrayList<>();
		forEachInventoryStack(target, slotRef -> {
			if (GreedItemCategoryHelper.isArtifact(slotRef.stack(target))) {
				candidates.add(slotRef);
			}
		});
		if (candidates.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack removed = candidates.get(target.getRandom().nextInt(candidates.size())).remove(target);
		target.currentScreenHandler.sendContentUpdates();
		return removed;
	}

	static ArrayList<ItemStack> seizeAllMatchingItems(ServerPlayerEntity target, StackSelector selector, boolean armorSlotsOnly) {
		ArrayList<ItemStack> seizedItems = new ArrayList<>();
		forEachInventoryStack(target, slotRef -> {
			if (armorSlotsOnly && slotRef.type != InventorySlotType.ARMOR) {
				return;
			}
			ItemStack stack = slotRef.stack(target);
			if (!stack.isEmpty() && selector.matches(stack)) {
				ItemStack removed = slotRef.remove(target);
				if (!removed.isEmpty()) {
					seizedItems.add(removed);
				}
			}
		});
		target.currentScreenHandler.sendContentUpdates();
		return seizedItems;
	}

	static ArrayList<InventorySlotRef> inventoryCandidates(ServerPlayerEntity target, MagicConfig.GreedDomainSeizureFilterConfig filters) {
		ArrayList<InventorySlotRef> candidates = new ArrayList<>();
		forEachInventoryStack(target, slotRef -> {
			if (matchesSeizureFilters(slotRef, target, filters)) {
				candidates.add(slotRef);
			}
		});
		return candidates;
	}

	static boolean matchesSeizureFilters(InventorySlotRef slotRef, ServerPlayerEntity target, MagicConfig.GreedDomainSeizureFilterConfig filters) {
		if (slotRef.type == InventorySlotType.HOTBAR && !filters.includeHotbar) {
			return false;
		}
		if (slotRef.type == InventorySlotType.MAIN && !filters.includeMainInventory) {
			return false;
		}
		if (slotRef.type == InventorySlotType.ARMOR && !filters.includeArmor) {
			return false;
		}
		if (slotRef.type == InventorySlotType.OFFHAND && !filters.includeOffhand) {
			return false;
		}
		ItemStack stack = slotRef.stack(target);
		if (stack.isEmpty()) {
			return false;
		}
		if (stack.isDamageable() && !filters.includeDamageable) {
			return false;
		}
		if (!stack.isDamageable() && !filters.includeUndamageable) {
			return false;
		}
		if (GreedItemCategoryHelper.isArtifact(stack) && !filters.includeArtifacts) {
			return false;
		}
		if (GreedItemCategoryHelper.isWeapon(stack) && !filters.includeWeapons) {
			return false;
		}
		if (GreedItemCategoryHelper.isTool(stack) && !filters.includeTools) {
			return false;
		}
		if (GreedItemCategoryHelper.isArmor(stack) && !filters.includeArmor) {
			return false;
		}
		return true;
	}

	static ArrayList<InventorySlotRef> majorWeaponCandidates(ServerPlayerEntity target, boolean preferEquipped, boolean allowInventoryFallback) {
		ArrayList<InventorySlotRef> candidates = new ArrayList<>();
		if (preferEquipped) {
			if (GreedItemCategoryHelper.isWeapon(target.getMainHandStack())) {
				candidates.add(new InventorySlotRef(target.getInventory().getSelectedSlot(), InventorySlotType.HOTBAR));
				return candidates;
			}
			if (GreedItemCategoryHelper.isWeapon(target.getOffHandStack())) {
				candidates.add(new InventorySlotRef(OFFHAND_SLOT, InventorySlotType.OFFHAND));
				return candidates;
			}
		}
		if (!allowInventoryFallback) {
			return candidates;
		}
		forEachInventoryStack(target, slotRef -> {
			if (slotRef.type != InventorySlotType.ARMOR && GreedItemCategoryHelper.isWeapon(slotRef.stack(target))) {
				candidates.add(slotRef);
			}
		});
		return candidates;
	}

	static ArrayList<InventorySlotRef> majorArmorCandidates(ServerPlayerEntity target, boolean preferEquipped, boolean allowInventoryFallback) {
		ArrayList<InventorySlotRef> candidates = new ArrayList<>();
		if (preferEquipped) {
			for (int slot = ARMOR_SLOT_START; slot < ARMOR_SLOT_END; slot++) {
				InventorySlotRef slotRef = new InventorySlotRef(slot, InventorySlotType.ARMOR);
				if (GreedItemCategoryHelper.isArmor(slotRef.stack(target))) {
					candidates.add(slotRef);
				}
			}
			if (!candidates.isEmpty()) {
				return candidates;
			}
		}
		if (!allowInventoryFallback) {
			return candidates;
		}
		forEachInventoryStack(target, slotRef -> {
			if (GreedItemCategoryHelper.isArmor(slotRef.stack(target))) {
				candidates.add(slotRef);
			}
		});
		return candidates;
	}

	static void transferItemsToCaster(ServerPlayerEntity caster, List<ItemStack> items) {
		for (ItemStack stack : items) {
			if (stack == null || stack.isEmpty()) {
				continue;
			}
			ItemStack remainder = stack.copy();
			caster.getInventory().insertStack(remainder);
			if (!remainder.isEmpty()) {
				caster.dropItem(remainder, false);
			}
		}
		caster.currentScreenHandler.sendContentUpdates();
	}

	static void transferItemsBackToTarget(ServerPlayerEntity target, List<ItemStack> items) {
		for (ItemStack stack : items) {
			if (stack == null || stack.isEmpty()) {
				continue;
			}
			ItemStack remainder = stack.copy();
			target.getInventory().insertStack(remainder);
			if (!remainder.isEmpty()) {
				target.dropItem(remainder, false);
			}
		}
		target.currentScreenHandler.sendContentUpdates();
	}

	static void applyBackfire(ServerPlayerEntity player, BackfireState state) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		player.damage(world, player.getDamageSources().magic(), state.damage);
		Vec3d launch = player.getRotationVec(1.0F).multiply(-state.horizontalLaunch).add(0.0, state.verticalLaunch, 0.0);
		player.setVelocity(launch);
		if (state.primaryParticleCount > 0) {
			world.spawnParticles(resolveParticleEffect(state.primaryParticleId, ParticleTypes.EXPLOSION), player.getX(), player.getBodyY(0.5), player.getZ(), state.primaryParticleCount, 0.0, 0.0, 0.0, 0.0);
		}
		if (state.secondaryParticleCount > 0) {
			world.spawnParticles(resolveParticleEffect(state.secondaryParticleId, ParticleTypes.CRIT), player.getX(), player.getBodyY(0.5), player.getZ(), state.secondaryParticleCount, 0.3, 0.5, 0.3, 0.02);
		}
		SoundEvent soundEvent = resolveSoundEvent(state.soundId, null);
		if (soundEvent != null) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, SoundCategory.PLAYERS, state.soundVolume, state.soundPitch);
		}
	}

	static void removeDisplaysForTarget(MinecraftServer server, UUID targetId) {
		if (server == null) {
			return;
		}
		for (ActiveDomainState state : ACTIVE_DOMAINS.values()) {
			TributeState tributeState = state.tributeByTarget.remove(targetId);
			if (tributeState != null) {
				removeDisplayEntity(server, state.dimension, tributeState.displayEntityId);
			}
		}
		Iterator<Map.Entry<OwnerTargetKey, FrozenTributeState>> iterator = FROZEN_TRIBUTES.entrySet().iterator();
		while (iterator.hasNext()) {
			FrozenTributeState state = iterator.next().getValue();
			if (!state.targetId.equals(targetId)) {
				continue;
			}
			removeDisplayEntity(server, state.dimension, state.displayEntityId);
			iterator.remove();
		}
	}

	static void removeDisplaysOwnedBy(MinecraftServer server, UUID ownerId) {
		ActiveDomainState activeState = ACTIVE_DOMAINS.remove(ownerId);
		if (activeState != null && server != null) {
			for (TributeState tributeState : activeState.tributeByTarget.values()) {
				removeDisplayEntity(server, activeState.dimension, tributeState.displayEntityId);
			}
		}
		Iterator<Map.Entry<OwnerTargetKey, FrozenTributeState>> iterator = FROZEN_TRIBUTES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<OwnerTargetKey, FrozenTributeState> entry = iterator.next();
			if (!entry.getKey().ownerId.equals(ownerId)) {
				continue;
			}
			if (server != null) {
				removeDisplayEntity(server, entry.getValue().dimension, entry.getValue().displayEntityId);
			}
			iterator.remove();
		}
	}

	static void clearDeathEndedDebtPenalties(UUID targetId) {
		ArrayList<DebtPenaltyState> penalties = DEBT_PENALTIES.get(targetId);
		if (penalties == null || penalties.isEmpty()) {
			return;
		}
		penalties.removeIf(state -> {
			state.clearStatPenalty();
			return !state.hasAnyTimedEffect();
		});
		if (penalties.isEmpty()) {
			DEBT_PENALTIES.remove(targetId);
		}
	}

	static void removePenaltyAttributeModifiers(ServerPlayerEntity player) {
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), GREED_ATTACK_DAMAGE_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.ARMOR), GREED_ARMOR_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.ARMOR_TOUGHNESS), GREED_ARMOR_TOUGHNESS_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), GREED_MOVEMENT_SPEED_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_SPEED), GREED_ATTACK_SPEED_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), GREED_MAX_HEALTH_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE), GREED_KNOCKBACK_RESISTANCE_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.SCALE), GREED_SCALE_MODIFIER_ID);
	}

	static void applyPenaltyAttributeModifier(EntityAttributeInstance attributeInstance, Identifier modifierId, double value) {
		if (attributeInstance == null) {
			return;
		}
		EntityAttributeModifier existing = attributeInstance.getModifier(modifierId);
		if (Math.abs(value) <= 1.0E-6) {
			if (existing != null) {
				attributeInstance.removeModifier(modifierId);
			}
			return;
		}
		if (existing != null && existing.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL && Math.abs(existing.value() - value) <= 1.0E-6) {
			return;
		}
		if (existing != null) {
			attributeInstance.removeModifier(modifierId);
		}
		attributeInstance.addTemporaryModifier(new EntityAttributeModifier(modifierId, value, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	static void removePenaltyAttributeModifier(EntityAttributeInstance attributeInstance, Identifier modifierId) {
		if (attributeInstance != null) {
			attributeInstance.removeModifier(modifierId);
		}
	}

	static int intervalCoinUnits(double coinsPerSecond, int intervalTicks) {
		return GreedRuntime.coinUnitsFromConfiguredCoinsValue(Math.max(0.0, coinsPerSecond) * intervalTicks / 20.0);
	}

	static int countPassiveIncomeTargets(ServerWorld world, UUID casterId, double centerX, double centerZ, int baseY, int innerRadius, int innerHeight, boolean countMobs) {
		int count = 0;
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.getUuid().equals(casterId) || !player.isAlive() || player.isSpectator() || MagicPlayerData.isDomainClashActive(player)) {
				continue;
			}
			if (isInsideDomainInterior(player, centerX, centerZ, baseY, innerRadius, innerHeight)) {
				count++;
			}
		}
		if (!countMobs) {
			return count;
		}
		Box box = new Box(centerX - innerRadius, baseY, centerZ - innerRadius, centerX + innerRadius, baseY + innerHeight, centerZ + innerRadius);
		for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box, LivingEntity::isAlive)) {
			if (isInsideDomainInterior(mob, centerX, centerZ, baseY, innerRadius, innerHeight)) {
				count++;
			}
		}
		return count;
	}

	static List<LivingEntity> collectTributeTargetsInsideDomain(ServerWorld world, UUID casterId, double centerX, double centerZ, int baseY, int innerRadius, int innerHeight) {
		ArrayList<LivingEntity> targets = new ArrayList<>();
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.getUuid().equals(casterId) || !player.isAlive() || player.isSpectator() || MagicPlayerData.isDomainClashActive(player)) {
				continue;
			}
			if (isInsideDomainInterior(player, centerX, centerZ, baseY, innerRadius, innerHeight)) {
				targets.add(player);
			}
		}
		if (!MagicConfig.get().greed.domain.tribute.applyOnlyToPlayers) {
			Box box = new Box(centerX - innerRadius, baseY, centerZ - innerRadius, centerX + innerRadius, baseY + innerHeight, centerZ + innerRadius);
			for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box, LivingEntity::isAlive)) {
				if (isInsideDomainInterior(mob, centerX, centerZ, baseY, innerRadius, innerHeight)) {
					targets.add(mob);
				}
			}
		}
		return targets;
	}

	static boolean isInsideDomainInterior(LivingEntity entity, double centerX, double centerZ, int baseY, int innerRadius, int innerHeight) {
		double dx = entity.getX() - centerX;
		double dz = entity.getZ() - centerZ;
		double horizontalDistanceSq = dx * dx + dz * dz;
		double relativeY = entity.getY() - baseY;
		if (relativeY < 0.0 || relativeY > innerHeight || horizontalDistanceSq > innerRadius * innerRadius) {
			return false;
		}
		double horizontalTerm = horizontalDistanceSq / Math.max(1.0, innerRadius * innerRadius);
		double verticalTerm = relativeY <= 0.0 ? 0.0 : relativeY * relativeY / Math.max(1.0, innerHeight * innerHeight);
		return horizontalTerm + verticalTerm <= 1.0;
	}

	static boolean isValidTributeTarget(LivingEntity living) {
		return !MagicConfig.get().greed.domain.tribute.applyOnlyToPlayers || living instanceof ServerPlayerEntity;
	}

	static UUID syncTributeDisplay(ServerWorld world, LivingEntity target, UUID displayEntityId, int tributeUnits, String prefix) {
		ArmorStandEntity displayEntity = null;
		if (displayEntityId != null) {
			Entity existing = world.getEntity(displayEntityId);
			if (existing instanceof ArmorStandEntity armorStand && armorStand.isAlive()) {
				displayEntity = armorStand;
			}
		}
		if (displayEntity == null) {
			displayEntityId = spawnTributeDisplay(world, target, tributeUnits, prefix);
			if (displayEntityId == null) {
				return null;
			}
			Entity existing = world.getEntity(displayEntityId);
			if (existing instanceof ArmorStandEntity armorStand && armorStand.isAlive()) {
				displayEntity = armorStand;
			}
		}
		if (displayEntity == null) {
			return null;
		}
		displayEntity.addCommandTag(TRIBUTE_DISPLAY_TAG);
		displayEntity.setCustomName(tributeText(prefix, tributeUnits));
		displayEntity.setCustomNameVisible(true);
		displayEntity.refreshPositionAndAngles(target.getX(), target.getY() + DISPLAY_VERTICAL_OFFSET, target.getZ(), 0.0F, 0.0F);
		return displayEntity.getUuid();
	}

	static UUID spawnTributeDisplay(ServerWorld world, LivingEntity target, int tributeUnits, String prefix) {
		ArmorStandEntity displayEntity = new ArmorStandEntity(world, target.getX(), target.getY() + DISPLAY_VERTICAL_OFFSET, target.getZ());
		displayEntity.setInvisible(true);
		displayEntity.setInvulnerable(true);
		displayEntity.setNoGravity(true);
		((ArmorStandEntityAccessorMixin) displayEntity).magic$setMarker(true);
		displayEntity.setSilent(true);
		displayEntity.addCommandTag(TRIBUTE_DISPLAY_TAG);
		displayEntity.setCustomName(tributeText(prefix, tributeUnits));
		displayEntity.setCustomNameVisible(true);
		if (!world.spawnEntity(displayEntity)) {
			return null;
		}
		return displayEntity.getUuid();
	}

	static Text tributeText(String prefix, int tributeUnits) {
		return Text.literal(prefix + ": " + formatCoinUnits(tributeUnits)).formatted(Formatting.GOLD, Formatting.BOLD);
	}

	static void cleanupOrphanTributeDisplays(MinecraftServer server) {
		if (server == null) {
			return;
		}
		String tributeNamePrefix = tributeNamePrefix(MagicConfig.get().greed.domain.tribute.displayPrefix);
		for (ServerWorld world : server.getWorlds()) {
			cleanupOrphanTributeDisplays(world, new Box(-3.0E7, world.getBottomY(), -3.0E7, 3.0E7, world.getTopYInclusive() + 1.0, 3.0E7), tributeNamePrefix);
		}
	}

	static void cleanupOrphanTributeDisplays(ServerWorld world, Box bounds) {
		if (world == null || bounds == null) {
			return;
		}
		cleanupOrphanTributeDisplays(world, bounds, tributeNamePrefix(MagicConfig.get().greed.domain.tribute.displayPrefix));
	}

	static void cleanupOrphanTributeDisplays(ServerWorld world, Box bounds, String tributeNamePrefix) {
		for (ArmorStandEntity armorStand : world.getEntitiesByClass(ArmorStandEntity.class, bounds, armorStand -> !isTrackedTributeDisplay(armorStand.getUuid()))) {
			if (isGreedTributeDisplay(armorStand, tributeNamePrefix)) {
				armorStand.discard();
			}
		}
	}

	static Box chunkBounds(ServerWorld world, WorldChunk chunk) {
		int minX = chunk.getPos().getStartX();
		int minZ = chunk.getPos().getStartZ();
		return new Box(minX, world.getBottomY(), minZ, minX + 16, world.getTopYInclusive() + 1.0, minZ + 16);
	}

	static boolean isTrackedTributeDisplay(UUID entityId) {
		for (ActiveDomainState state : ACTIVE_DOMAINS.values()) {
			for (TributeState tributeState : state.tributeByTarget.values()) {
				if (entityId.equals(tributeState.displayEntityId)) {
					return true;
				}
			}
		}
		for (FrozenTributeState state : FROZEN_TRIBUTES.values()) {
			if (entityId.equals(state.displayEntityId)) {
				return true;
			}
		}
		return false;
	}

	static boolean isGreedTributeDisplay(ArmorStandEntity armorStand, String tributeNamePrefix) {
		if (armorStand.getCommandTags().contains(TRIBUTE_DISPLAY_TAG)) {
			return true;
		}
		Text customName = armorStand.getCustomName();
		return armorStand.isMarker()
			&& armorStand.isInvisible()
			&& armorStand.hasNoGravity()
			&& armorStand.isSilent()
			&& armorStand.isCustomNameVisible()
			&& customName != null
			&& customName.getString().startsWith(tributeNamePrefix);
	}

	static String tributeNamePrefix(String prefix) {
		String normalizedPrefix = prefix == null || prefix.isBlank() ? "Tribute" : prefix.trim();
		return normalizedPrefix + ": ";
	}

	static void removeDisplayEntity(MinecraftServer server, net.minecraft.registry.RegistryKey<World> dimension, UUID entityId) {
		if (server == null || entityId == null || dimension == null) {
			return;
		}
		ServerWorld world = server.getWorld(dimension);
		if (world == null) {
			return;
		}
		Entity entity = world.getEntity(entityId);
		if (entity != null) {
			entity.discard();
		}
	}

	static void forEachInventoryStack(ServerPlayerEntity target, SlotConsumer consumer) {
		PlayerInventory inventory = target.getInventory();
		for (int slot = 0; slot < inventory.size(); slot++) {
			InventorySlotType slotType = slot < HOTBAR_SLOT_COUNT
				? InventorySlotType.HOTBAR
				: slot < MAIN_INVENTORY_SLOT_END
					? InventorySlotType.MAIN
					: slot < ARMOR_SLOT_END
						? InventorySlotType.ARMOR
						: InventorySlotType.OFFHAND;
			consumer.accept(new InventorySlotRef(slot, slotType));
		}
	}
}

