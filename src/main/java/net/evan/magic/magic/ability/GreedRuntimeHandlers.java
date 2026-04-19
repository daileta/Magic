package net.evan.magic.magic.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.evan.magic.Magic;
import net.evan.magic.config.MagicConfig;
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.MagicSchool;
import net.evan.magic.particle.TollkeepersClaimVortexParticleEffect;
import net.evan.magic.registry.ModStatusEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

abstract class GreedRuntimeHandlers extends GreedRuntimeSupport {
	public static void cancelActiveAbilities(ServerPlayerEntity player, int currentTick) {
		endAppraisersMark(player, currentTick, true);
		removeTollkeeperZones(player.getUuid());
	}

	public static void onAttackEntity(ServerPlayerEntity attacker, Entity entity) {
		if (!(entity instanceof ServerPlayerEntity target) || attacker == target || !target.isAlive() || target.isSpectator()) {
			return;
		}

		AppraisersMarkState state = APPRAISERS_MARK_STATES.get(attacker.getUuid());
		if (state == null || state.stage != AppraisersMarkStage.WAITING) {
			return;
		}

		state.stage = AppraisersMarkStage.MARKED;
		state.markedTargetId = target.getUuid();
		state.lastParticleTick = Integer.MIN_VALUE;
		state.drainBuffer = 0.0;
		attacker.sendMessage(Text.translatable("message.magic.greed.appraisers_mark.marked", target.getDisplayName()), true);
		target.sendMessage(Text.translatable("message.magic.greed.appraisers_mark.targeted", attacker.getDisplayName()), true);
	}

	public static void onUseBlock(ServerPlayerEntity player, World world, BlockPos pos, BlockState state, ItemStack stack) {
		if (world.isClient()) {
			return;
		}

		if (state.isOf(Blocks.TNT) && (stack.isOf(Items.FLINT_AND_STEEL) || stack.isOf(Items.FIRE_CHARGE))) {
			recordActionFromPlayer(player, "ignite_tnt", null);
			return;
		}

		if (!(world instanceof ServerWorld serverWorld) || !state.isOf(Blocks.RESPAWN_ANCHOR)) {
			return;
		}

		int charges = state.getOrEmpty(RespawnAnchorBlock.CHARGES).orElse(0);
		if (charges > 0 && !RespawnAnchorBlock.isUsable(serverWorld, pos)) {
			recordActionFromPlayer(player, "explode_respawn_anchor", null);
		}
	}

	public static void onPlayerAfterDamage(ServerPlayerEntity damagedPlayer, DamageSource source, float damageTaken) {
		ServerPlayerEntity attacker = attackingPlayerFrom(source);
		if (attacker == null || attacker == damagedPlayer || !attacker.isAlive() || attacker.isSpectator() || damageTaken <= 0.0F) {
			return;
		}

		if (isFallingMaceAttack(source)) {
			recordActionFromPlayer(attacker, "falling_mace_attack", damagedPlayer.getUuid());
			return;
		}
		if (isFireworkShot(source)) {
			recordActionFromPlayer(attacker, "firework_shot", damagedPlayer.getUuid());
			return;
		}
		if (isTippedArrowShot(source)) {
			recordActionFromPlayer(attacker, "tipped_arrow_shot", damagedPlayer.getUuid());
			return;
		}
		if (isSpearChargeAttack(attacker, source)) {
			recordActionFromPlayer(attacker, "spear_charge_attack", damagedPlayer.getUuid());
			return;
		}
		if (isSpearLunge(source)) {
			recordActionFromPlayer(attacker, "spear_lunge", damagedPlayer.getUuid());
			return;
		}
		if (isFullChargeBowShot(source)) {
			recordActionFromPlayer(attacker, "full_charge_bow_shot", damagedPlayer.getUuid());
			return;
		}
		recordActionFromPlayer(attacker, "normal_hit", damagedPlayer.getUuid());
	}

	public static void onShieldDisabled(ServerPlayerEntity attacker, ServerPlayerEntity target) {
		recordActionFromPlayer(attacker, "disable_shield", target == null ? null : target.getUuid());
	}

	public static void onShieldRaised(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		recordActionFromPlayer(player, "raise_shield", null);
	}

	public static void onPlayerDeathProtectorTriggered(ServerPlayerEntity target, DamageSource source) {
		if (target == null) {
			return;
		}
		ServerPlayerEntity attacker = attackingPlayerFrom(source);
		if (attacker == null || attacker == target || !attacker.isAlive() || attacker.isSpectator()) {
			return;
		}
		recordActionFromPlayer(attacker, "pop_player_totem", target.getUuid());
	}

	public static void onEndCrystalDestroyed(DamageSource source) {
		ServerPlayerEntity attacker = attackingPlayerFrom(source);
		if (attacker != null) {
			recordActionFromPlayer(attacker, "destroy_end_crystal", null);
		}
	}

	public static void onTntMinecartPrimed(DamageSource source) {
		ServerPlayerEntity attacker = attackingPlayerFrom(source);
		if (attacker != null) {
			recordActionFromPlayer(attacker, "ignite_tnt_minecart", null);
		}
	}

	public static void recordExternalAction(ServerPlayerEntity actor, String triggerId) {
		recordActionFromPlayer(actor, triggerId, null);
	}

	public static void recordExternalAction(ServerPlayerEntity actor, String triggerId, UUID directTargetId) {
		recordActionFromPlayer(actor, triggerId, directTargetId);
	}

	public static void onUseItem(ServerPlayerEntity player, ItemStack stack) {
		if (player == null || stack == null || stack.isEmpty()) {
			return;
		}
		if (stack.get(DataComponentTypes.BLOCKS_ATTACKS) != null) {
			recordActionFromPlayer(player, "raise_shield", null);
			return;
		}
		if (stack.isOf(Items.ENDER_PEARL)) {
			releaseRootForEnderPearlEscape(player);
			recordActionFromPlayer(player, "use_ender_pearl", null);
			return;
		}
		if (stack.isOf(Items.POTION) || stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION)) {
			recordActionFromPlayer(player, "use_potion", null);
			return;
		}
		if (stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
			recordActionFromPlayer(player, "eat_enchanted_golden_apple", null);
			return;
		}
		if (stack.isOf(Items.GOLDEN_APPLE)) {
			recordActionFromPlayer(player, "eat_golden_apple", null);
			return;
		}
		if (stack.get(DataComponentTypes.FOOD) != null) {
			recordActionFromPlayer(player, "eat_food", null);
		}
	}

	public static void onStartSprinting(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		int currentTick = currentTick(player);
		if (currentTick == Integer.MIN_VALUE) {
			return;
		}
		Integer lastTriggerTick = LAST_SPRINT_TRIGGER_TICK.put(player.getUuid(), currentTick);
		if (lastTriggerTick != null && lastTriggerTick == currentTick) {
			return;
		}
		recordActionFromPlayer(player, "start_sprinting", null);
	}

	public static void onPlayerJump(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		int currentTick = currentTick(player);
		if (currentTick == Integer.MIN_VALUE) {
			return;
		}
		Integer lastTriggerTick = LAST_JUMP_TRIGGER_TICK.put(player.getUuid(), currentTick);
		if (lastTriggerTick != null && lastTriggerTick == currentTick) {
			return;
		}
		recordActionFromPlayer(player, "jump", null);
	}

	public static void recordSuccessfulMagicAbilityUse(ServerPlayerEntity player, MagicAbility ability) {
		if (player == null || ability == null || ability == MagicAbility.NONE || isPassiveGreedCoinAbility(ability)) {
			return;
		}
		String triggerId = greedCoinTriggerIdForAbility(ability);
		if (triggerId != null) {
			recordActionFromPlayer(player, triggerId, null);
		}
	}

	public static int addCoins(ServerPlayerEntity player, double coins) {
		if (player == null || MagicPlayerData.getSchool(player) != MagicSchool.GREED) {
			return 0;
		}

		int currentTick = currentTick(player);
		if (currentTick == Integer.MIN_VALUE) {
			return 0;
		}

		int requestedCoinUnits = coinUnitsFromConfiguredCoins(coins);
		if (requestedCoinUnits <= 0) {
			return 0;
		}

		int previousCoinUnits = MagicPlayerData.getGreedCoinUnits(player);
		addCoinUnits(player, COMMAND_COIN_SOURCE_ID, requestedCoinUnits, currentTick);
		return Math.max(0, MagicPlayerData.getGreedCoinUnits(player) - previousCoinUnits);
	}

	static int coinUnitsFromConfiguredCoinsValue(double coins) {
		return coinUnitsFromConfiguredCoins(coins);
	}

	static void grantCoinUnits(ServerPlayerEntity caster, UUID sourcePlayerId, int coinUnits, int currentTick, boolean allowOverflowBeyondCap) {
		addCoinUnits(caster, sourcePlayerId, coinUnits, currentTick, allowOverflowBeyondCap);
	}

	static void grantAnonymousCoinUnits(ServerPlayerEntity caster, int coinUnits, int currentTick, boolean allowOverflowBeyondCap) {
		addCoinUnits(caster, COMMAND_COIN_SOURCE_ID, coinUnits, currentTick, allowOverflowBeyondCap);
	}

	public static void onEndServerTick(MinecraftServer server, int currentTick) {
		updateAppraisersMarks(server, currentTick);
		updateCoinStorages(server, currentTick);
		updateTollkeepersClaimZones(server, currentTick);
		updateRoots(server, currentTick);
		updateSprintLocks(server, currentTick);
		updateShieldLocks(currentTick);
		updateAbilityLocks(currentTick);
		updateAttackSpeedLocks(server, currentTick);
		updateElytraEquipActions(server);

		if (currentTick % TICKS_PER_SECOND == 0) {
			updateAppraisersMarkMana(server, currentTick);
		}

		int manaSicknessInterval = Math.max(1, MagicConfig.get().greed.manaSickness.tickIntervalTicks);
		if (currentTick % manaSicknessInterval == 0) {
			updateManaSickness(server, currentTick, manaSicknessInterval);
		}
	}

	public static void onPlayerDeath(ServerPlayerEntity player) {
		int currentTick = currentTick(player);
		cancelActiveAbilities(player, currentTick);
		clearCoins(player);
		ROOTED_PLAYERS.remove(player.getUuid());
		SPRINT_LOCK_END_TICK.remove(player.getUuid());
		SHIELD_LOCK_END_TICK.remove(player.getUuid());
		ABILITY_LOCK_END_TICK.remove(player.getUuid());
		ATTACK_SPEED_LOCKS.remove(player.getUuid());
		MANA_SICKNESS_DRAIN_BUFFER.remove(player.getUuid());
		LAST_ELYTRA_EQUIPPED_STATE.remove(player.getUuid());
		LAST_SPRINT_TRIGGER_TICK.remove(player.getUuid());
		LAST_JUMP_TRIGGER_TICK.remove(player.getUuid());
		removeAttackSpeedModifier(player);
	}

	public static void clearAllRuntimeState(ServerPlayerEntity player) {
		APPRAISERS_MARK_STATES.remove(player.getUuid());
		APPRAISERS_MARK_COOLDOWN_END_TICK.remove(player.getUuid());
		GREED_COIN_STORAGES.remove(player.getUuid());
		MagicPlayerData.setGreedCoinUnits(player, 0);
		removeTollkeeperZones(player.getUuid());
		ROOTED_PLAYERS.remove(player.getUuid());
		SPRINT_LOCK_END_TICK.remove(player.getUuid());
		SHIELD_LOCK_END_TICK.remove(player.getUuid());
		ABILITY_LOCK_END_TICK.remove(player.getUuid());
		ATTACK_SPEED_LOCKS.remove(player.getUuid());
		KINGS_DUES_COOLDOWN_END_TICK.remove(player.getUuid());
		BANKRUPTCY_COOLDOWN_END_TICK.remove(player.getUuid());
		MANA_SICKNESS_DRAIN_BUFFER.remove(player.getUuid());
		LAST_ELYTRA_EQUIPPED_STATE.remove(player.getUuid());
		LAST_SPRINT_TRIGGER_TICK.remove(player.getUuid());
		LAST_JUMP_TRIGGER_TICK.remove(player.getUuid());
		removeAttackSpeedModifier(player);
		player.removeStatusEffect(ModStatusEffects.gildedBurdenEntry());
		player.removeStatusEffect(ModStatusEffects.manaSicknessEntry());
	}

	public static int cooldownRemaining(ServerPlayerEntity player, MagicAbility ability, int currentTick) {
		if (MagicAbilityManager.areOrionsGambitTargetCooldownsSuppressed(player)) {
			return 0;
		}

		UUID playerId = player.getUuid();
		if (ability == MagicAbility.APPRAISERS_MARK) {
			return Math.max(0, APPRAISERS_MARK_COOLDOWN_END_TICK.getOrDefault(playerId, 0) - currentTick);
		}
		if (ability == MagicAbility.KINGS_DUES) {
			return Math.max(0, KINGS_DUES_COOLDOWN_END_TICK.getOrDefault(playerId, 0) - currentTick);
		}
		if (ability == MagicAbility.BANKRUPTCY) {
			return Math.max(0, BANKRUPTCY_COOLDOWN_END_TICK.getOrDefault(playerId, 0) - currentTick);
		}
		return 0;
	}

	public static int resetCooldown(ServerPlayerEntity player, MagicAbility ability) {
		UUID playerId = player.getUuid();
		if (ability == MagicAbility.APPRAISERS_MARK) {
			boolean changed = APPRAISERS_MARK_COOLDOWN_END_TICK.remove(playerId) != null;
			changed |= APPRAISERS_MARK_STATES.remove(playerId) != null;
			return changed ? 1 : 0;
		}
		if (ability == MagicAbility.TOLLKEEPERS_CLAIM) {
			return removeTollkeeperZones(playerId);
		}
		if (ability == MagicAbility.KINGS_DUES) {
			return KINGS_DUES_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}
		if (ability == MagicAbility.BANKRUPTCY) {
			return BANKRUPTCY_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}
		return 0;
	}

	public static boolean isAbilityUseLocked(ServerPlayerEntity player, int currentTick) {
		return currentTick < ABILITY_LOCK_END_TICK.getOrDefault(player.getUuid(), Integer.MIN_VALUE);
	}

	public static boolean blocksManaRegen(PlayerEntity player) {
		if (player.hasStatusEffect(ModStatusEffects.manaSicknessEntry())) {
			return true;
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			return hasMarkedAppraisersTarget(serverPlayer);
		}
		return false;
	}

	public static boolean hasMarkedAppraisersTarget(ServerPlayerEntity player) {
		AppraisersMarkState state = APPRAISERS_MARK_STATES.get(player.getUuid());
		return state != null && state.stage == AppraisersMarkStage.MARKED;
	}

	public static boolean isSprintBlocked(PlayerEntity player) {
		if (player == null) {
			return false;
		}

		int currentTick = currentTick(player);
		if (currentTick == Integer.MIN_VALUE) {
			return false;
		}

		if (ROOTED_PLAYERS.containsKey(player.getUuid()) || currentTick < SPRINT_LOCK_END_TICK.getOrDefault(player.getUuid(), Integer.MIN_VALUE)) {
			return true;
		}

		StatusEffectInstance burden = player.getStatusEffect(ModStatusEffects.gildedBurdenEntry());
		if (burden == null) {
			return false;
		}

		return tollkeepersStage(MathHelper.clamp(burden.getAmplifier() + 1, 1, 5)).disableSprint;
	}

	public static boolean isOffhandBlocked(PlayerEntity player) {
		if (player == null) {
			return false;
		}

		RootState state = ROOTED_PLAYERS.get(player.getUuid());
		if (state == null) {
			return false;
		}

		int currentTick = currentTick(player);
		return currentTick != Integer.MIN_VALUE && currentTick < state.endTick;
	}

	public static boolean canUseEnderPearlWhileRooted(PlayerEntity player, ItemStack stack) {
		return player != null
			&& stack != null
			&& stack.isOf(Items.ENDER_PEARL)
			&& MagicConfig.get().greed.tollkeepersClaim.allowEnderPearlEscapeWhileRooted
			&& isRooted(player);
	}

	public static boolean isShieldLocked(PlayerEntity player) {
		if (player == null) {
			return false;
		}

		int currentTick = currentTick(player);
		return currentTick != Integer.MIN_VALUE && currentTick < SHIELD_LOCK_END_TICK.getOrDefault(player.getUuid(), Integer.MIN_VALUE);
	}

	public static float modifyJumpVelocity(LivingEntity entity, float originalVelocity) {
		if (!(entity instanceof PlayerEntity player)) {
			return originalVelocity;
		}

		StatusEffectInstance burden = player.getStatusEffect(ModStatusEffects.gildedBurdenEntry());
		if (burden == null) {
			return originalVelocity;
		}

		MagicConfig.TollkeepersClaimStageConfig stage = tollkeepersStage(MathHelper.clamp(burden.getAmplifier() + 1, 1, 5));
		if (!stage.reduceJumpHeight) {
			return originalVelocity;
		}

		return originalVelocity * (float) MagicConfig.get().greed.tollkeepersClaim.reducedJumpVelocityMultiplier;
	}

	public static void onBlockedSprintAttempt(ServerPlayerEntity player) {
		spawnChainParticles(player);
		playChainSound(player);
	}

	public static void onBlockedShieldUse(ServerPlayerEntity player) {
		spawnChainParticles(player);
		playChainSound(player);
	}

	static void handleAppraisersMarkRequest(ServerPlayerEntity player, int currentTick) {
		AppraisersMarkState existingState = APPRAISERS_MARK_STATES.get(player.getUuid());
		if (existingState != null) {
			boolean wasMarked = existingState.stage == AppraisersMarkStage.MARKED;
			endAppraisersMark(player, currentTick, true);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.APPRAISERS_MARK.displayName()), true);
			if (!wasMarked) {
				player.sendMessage(Text.translatable("message.magic.greed.appraisers_mark.canceled"), true);
			}
			return;
		}

		int remainingTicks = cooldownRemaining(player, MagicAbility.APPRAISERS_MARK, currentTick);
		if (remainingTicks > 0) {
			sendCooldownMessage(player, MagicAbility.APPRAISERS_MARK, remainingTicks);
			return;
		}

		if (MagicPlayerData.getMana(player) <= 0) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		APPRAISERS_MARK_STATES.put(player.getUuid(), AppraisersMarkState.waiting());
		recordSuccessfulMagicAbilityUse(player, MagicAbility.APPRAISERS_MARK);
		player.sendMessage(Text.translatable("message.magic.greed.appraisers_mark.waiting"), true);
	}

	static void handleTollkeepersClaimRequest(ServerPlayerEntity player, int currentTick) {
		MagicConfig.TollkeepersClaimConfig config = MagicConfig.get().greed.tollkeepersClaim;
		int spend = spendAmount(player, config.minCoins, config.maxCoins);
		if (spend <= 0) {
			player.sendMessage(Text.translatable("message.magic.greed.not_enough_coins", config.minCoins), true);
			return;
		}

		BlockHitResult hitResult = findTargetedBlock(player, config.placementRange);
		if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
			player.sendMessage(Text.translatable("message.magic.greed.invalid_zone_placement"), true);
			return;
		}

		BlockPos blockPos = hitResult.getBlockPos();
		double centerX = blockPos.getX() + 0.5;
		double centerY = blockPos.getY() + 0.05;
		double centerZ = blockPos.getZ() + 0.5;
		int durationTicks = Math.max(1, config.baseDurationTicks + spend * config.durationPerCoinTicks);
		double zoneRadius = tollkeepersZoneRadius(spend);
		if (config.maxActiveZonesPerCaster > 0) {
			trimOldestTollkeeperZones(player.getUuid(), config.maxActiveZonesPerCaster - 1);
		}
		TollkeepersClaimZoneState zone = new TollkeepersClaimZoneState(
			player.getUuid(),
			player.getEntityWorld().getRegistryKey(),
			centerX,
			centerY,
			centerZ,
			zoneRadius,
			spend,
			currentTick,
			currentTick + durationTicks
		);
		TOLLKEEPER_ZONES.put(UUID.randomUUID(), zone);
		removeCoins(player, spend);
		if (player.getEntityWorld() instanceof ServerWorld serverWorld) {
			spawnZoneParticles(serverWorld, zone, currentTick);
			zone.nextParticleTick = currentTick + config.vortexParticleIntervalTicks;
			zone.nextSoundTick = currentTick + config.shimmerSoundIntervalTicks;
			serverWorld.playSound(
				null,
				centerX,
				centerY,
				centerZ,
				SoundEvents.ITEM_ARMOR_EQUIP_GOLD,
				SoundCategory.PLAYERS,
				0.8F,
				1.2F
			);
		}
		recordSuccessfulMagicAbilityUse(player, MagicAbility.TOLLKEEPERS_CLAIM);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.TOLLKEEPERS_CLAIM.displayName()), true);
	}

	static void handleKingsDuesRequest(ServerPlayerEntity player, int currentTick) {
		int remainingTicks = cooldownRemaining(player, MagicAbility.KINGS_DUES, currentTick);
		if (remainingTicks > 0) {
			sendCooldownMessage(player, MagicAbility.KINGS_DUES, remainingTicks);
			return;
		}

		MagicConfig.KingsDuesConfig config = MagicConfig.get().greed.kingsDues;
		int spend = spendAmount(player, config.minCoins, config.maxCoins);
		if (spend <= 0) {
			player.sendMessage(Text.translatable("message.magic.greed.not_enough_coins", config.minCoins), true);
			return;
		}

		ServerPlayerEntity target = findPlayerTargetInLineOfSight(player, config.targetRange, config.requireLineOfSight);
		if (target == null || target == player) {
			player.sendMessage(Text.translatable("message.magic.greed.no_valid_player_target"), true);
			return;
		}

		MagicConfig.KingsDuesStageConfig stage = kingsDuesStage(spend);
		removeCoins(player, spend);
		applyStageEffect(target, StatusEffects.WEAKNESS, stage.weaknessDurationTicks, stage.weaknessAmplifier);
		applyStageEffect(target, StatusEffects.SLOWNESS, stage.slownessDurationTicks, stage.slownessAmplifier);
		if (stage.glowingDurationTicks > 0) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, stage.glowingDurationTicks, 0, true, true, true));
		}
		if (stage.sprintLockTicks > 0) {
			SPRINT_LOCK_END_TICK.put(target.getUuid(), Math.max(SPRINT_LOCK_END_TICK.getOrDefault(target.getUuid(), 0), currentTick + stage.sprintLockTicks));
			target.setSprinting(false);
		}
		if (stage.shieldLockTicks > 0) {
			SHIELD_LOCK_END_TICK.put(target.getUuid(), Math.max(SHIELD_LOCK_END_TICK.getOrDefault(target.getUuid(), 0), currentTick + stage.shieldLockTicks));
		}
		if (stage.attackSpeedLockTicks > 0 && Math.abs(stage.attackSpeedModifierAmount) > 1.0E-6) {
			applyAttackSpeedLock(target, currentTick + stage.attackSpeedLockTicks, stage.attackSpeedModifierAmount);
		}
		KINGS_DUES_COOLDOWN_END_TICK.put(
			player.getUuid(),
			currentTick + GreedDomainRuntime.adjustCooldownTicks(player.getUuid(), MagicAbility.KINGS_DUES, config.cooldownTicks, currentTick)
		);
		applyKingsDuesVisuals(target, spend);
		recordSuccessfulMagicAbilityUse(player, MagicAbility.KINGS_DUES);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.KINGS_DUES.displayName()), true);
	}

	static void handleBankruptcyRequest(ServerPlayerEntity player, int currentTick) {
		int remainingTicks = cooldownRemaining(player, MagicAbility.BANKRUPTCY, currentTick);
		if (remainingTicks > 0) {
			sendCooldownMessage(player, MagicAbility.BANKRUPTCY, remainingTicks);
			return;
		}

		MagicConfig.BankruptcyConfig config = MagicConfig.get().greed.bankruptcy;
		int spend = spendAmount(player, config.minCoins, config.maxCoins);
		if (spend <= 0) {
			player.sendMessage(Text.translatable("message.magic.greed.not_enough_coins", config.minCoins), true);
			return;
		}

		ServerPlayerEntity target = findPlayerTargetInLineOfSight(player, config.targetRange, config.requireLineOfSight);
		if (target == null || target == player) {
			player.sendMessage(Text.translatable("message.magic.greed.no_valid_player_target"), true);
			return;
		}

		MagicConfig.BankruptcyStageConfig stage = bankruptcyStage(spend);
		removeCoins(player, spend);
		if (stage.durationTicks > 0 && stage.manaSicknessAmplifier >= 0) {
			target.addStatusEffect(
				new StatusEffectInstance(
					ModStatusEffects.manaSicknessEntry(),
					stage.durationTicks,
					stage.manaSicknessAmplifier,
					true,
					true,
					true
				)
			);
		}
		if (stage.instantManaDrainPercent > 0.0) {
			int currentMana = MagicPlayerData.getMana(target);
			int manaDrain = Math.max(0, (int) Math.floor(currentMana * stage.instantManaDrainPercent / 100.0 + 1.0E-7));
			int nextMana = Math.max(0, currentMana - manaDrain);
			MagicPlayerData.setMana(target, nextMana);
			if (nextMana == 0 && manaDrain > 0) {
				MagicPlayerData.setDepletedRecoveryMode(target, true);
			}
		}
		if (stage.abilityLockTicks > 0) {
			ABILITY_LOCK_END_TICK.put(target.getUuid(), Math.max(ABILITY_LOCK_END_TICK.getOrDefault(target.getUuid(), 0), currentTick + stage.abilityLockTicks));
		}
		if (stage.cancelActiveAbilities) {
			MagicAbilityManager.cancelOwnedMagicOnTargetForBankruptcy(
				target,
				currentTick,
				stage.preserveMaximumAbilities,
				stage.preserveDomainAbilities
			);
		}
		BANKRUPTCY_COOLDOWN_END_TICK.put(
			player.getUuid(),
			currentTick + GreedDomainRuntime.adjustCooldownTicks(player.getUuid(), MagicAbility.BANKRUPTCY, config.cooldownTicks, currentTick)
		);
		applyBankruptcyVisuals(target);
		recordSuccessfulMagicAbilityUse(player, MagicAbility.BANKRUPTCY);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.BANKRUPTCY.displayName()), true);
	}

	static void recordActionFromPlayer(ServerPlayerEntity actor, String triggerId, UUID directTargetId) {
		if (actor == null || triggerId == null || triggerId.isBlank()) {
			return;
		}

		String normalizedTriggerId = triggerId.trim().toLowerCase();
		Double configuredCoins = MagicConfig.get().greed.appraisersMark.coinTriggers.get(normalizedTriggerId);
		if (configuredCoins == null) {
			return;
		}
		int configuredCoinUnits = coinUnitsFromConfiguredCoins(configuredCoins.doubleValue());
		if (configuredCoinUnits <= 0) {
			return;
		}

		MinecraftServer server = actor.getEntityWorld().getServer();
		if (server == null) {
			return;
		}

		int currentTick = server.getTicks();
		HashSet<UUID> grantedCasterIds = new HashSet<>();
		for (Map.Entry<UUID, AppraisersMarkState> entry : APPRAISERS_MARK_STATES.entrySet()) {
			AppraisersMarkState state = entry.getValue();
			if (state.stage != AppraisersMarkStage.MARKED || !actor.getUuid().equals(state.markedTargetId)) {
				continue;
			}

			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			if (caster == null || !caster.isAlive() || caster.isSpectator()) {
				continue;
			}
			if (caster.getEntityWorld() != actor.getEntityWorld()) {
				continue;
			}
			if (caster.squaredDistanceTo(actor) > MagicConfig.get().greed.appraisersMark.markedActionRange * MagicConfig.get().greed.appraisersMark.markedActionRange) {
				continue;
			}
			if (!GreedDomainRuntime.allowsCoinTrigger(actor.getUuid(), caster.getUuid(), directTargetId, currentTick)) {
				continue;
			}

			int adjustedCoinUnits = GreedDomainRuntime.adjustCoinTriggerCoinUnits(
				actor.getUuid(),
				caster.getUuid(),
				normalizedTriggerId,
				configuredCoinUnits,
				currentTick
			);
			if (adjustedCoinUnits <= 0) {
				continue;
			}

			addCoinUnits(caster, actor.getUuid(), adjustedCoinUnits, currentTick);
			grantedCasterIds.add(caster.getUuid());
		}
		GreedDomainRuntime.onCoinTrigger(server, actor, normalizedTriggerId, directTargetId, configuredCoinUnits, currentTick, grantedCasterIds);
	}

	static void updateAppraisersMarks(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, AppraisersMarkState>> iterator = APPRAISERS_MARK_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, AppraisersMarkState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			AppraisersMarkState state = entry.getValue();
			if (caster == null || !caster.isAlive() || caster.isSpectator() || MagicPlayerData.getSchool(caster) != MagicSchool.GREED) {
				iterator.remove();
				continue;
			}
			if (state.stage != AppraisersMarkStage.MARKED) {
				continue;
			}

			ServerPlayerEntity target = server.getPlayerManager().getPlayer(state.markedTargetId);
			if (target == null || !target.isAlive() || target.isSpectator()) {
				iterator.remove();
				startAbilityCooldownFromNow(caster.getUuid(), MagicAbility.APPRAISERS_MARK, currentTick);
				continue;
			}
			if (currentTick < state.lastParticleTick + MagicConfig.get().greed.appraisersMark.markParticleIntervalTicks) {
				continue;
			}
			if (target.getEntityWorld() instanceof ServerWorld serverWorld) {
				spawnMarkedParticles(serverWorld, target);
			}
			state.lastParticleTick = currentTick;
		}
	}

	static void updateCoinStorages(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, GreedCoinStorage>> iterator = GREED_COIN_STORAGES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, GreedCoinStorage> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			GreedCoinStorage storage = entry.getValue();
			if (caster == null || !caster.isAlive()) {
				iterator.remove();
				continue;
			}

			expireContributions(storage, currentTick);
			if (
				storage.lastGainTick != Integer.MIN_VALUE &&
				currentTick - storage.lastGainTick >= MagicConfig.get().greed.appraisersMark.inactivityWipeTicks
			) {
				storage.contributions.clear();
				storage.lastGainTick = Integer.MIN_VALUE;
			}
			if (pouchCoinUnits(storage) <= 0 && storage.temporaryOverflowCoinUnits <= 0) {
				iterator.remove();
				MagicPlayerData.setGreedCoinUnits(caster, 0);
				continue;
			}

			MagicPlayerData.setGreedCoinUnits(caster, totalCoinUnits(storage));
		}
	}

	static void updateTollkeepersClaimZones(MinecraftServer server, int currentTick) {
		HashSet<UUID> rootedThisTick = new HashSet<>();
		Iterator<Map.Entry<UUID, TollkeepersClaimZoneState>> iterator = TOLLKEEPER_ZONES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, TollkeepersClaimZoneState> entry = iterator.next();
			TollkeepersClaimZoneState zone = entry.getValue();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(zone.casterId);
			ServerWorld world = server.getWorld(zone.dimension);
			if (caster == null || !caster.isAlive() || world == null || currentTick >= zone.expiresTick) {
				iterator.remove();
				continue;
			}

			if (currentTick >= zone.nextParticleTick) {
				spawnZoneParticles(world, zone, currentTick);
				zone.nextParticleTick = currentTick + MagicConfig.get().greed.tollkeepersClaim.vortexParticleIntervalTicks;
			}
			if (currentTick >= zone.nextSoundTick) {
				world.playSound(
					null,
					zone.centerX,
					zone.centerY,
					zone.centerZ,
					SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
					SoundCategory.PLAYERS,
					0.45F,
					1.45F
				);
				zone.nextSoundTick = currentTick + MagicConfig.get().greed.tollkeepersClaim.shimmerSoundIntervalTicks;
			}

			if (isInsideZone(zone, caster)) {
				applyCasterZoneBuff(caster, zone.coinsSpent);
			}

			Set<UUID> newInsidePlayerIds = new HashSet<>();
			for (ServerPlayerEntity player : world.getPlayers()) {
				if (player == caster || !player.isAlive() || player.isSpectator()) {
					continue;
				}

				boolean insideZone = isInsideZone(zone, player);
				if (insideZone) {
					newInsidePlayerIds.add(player.getUuid());
					zone.rootConsumedPlayerIds.remove(player.getUuid());
					applyGildedBurden(player, zone.coinsSpent);
					if (player.isSprinting() && isSprintBlocked(player)) {
						player.setSprinting(false);
					}
					continue;
				}

				if (zone.insidePlayerIds.contains(player.getUuid())) {
					boolean firstExitSinceReentry = zone.rootConsumedPlayerIds.add(player.getUuid());
					if (firstExitSinceReentry) {
						if (!isRooted(player) && rootedThisTick.add(player.getUuid())) {
							applyRoot(player, zone, currentTick);
						}
						if (isMarkedTarget(caster, player) && MagicConfig.get().greed.tollkeepersClaim.markedExitBonusCoins > 0.0) {
							addCoinUnits(
								caster,
								player.getUuid(),
								coinUnitsFromConfiguredCoins(MagicConfig.get().greed.tollkeepersClaim.markedExitBonusCoins),
								currentTick
							);
						}
					}
				}
			}

			zone.insidePlayerIds.clear();
			zone.insidePlayerIds.addAll(newInsidePlayerIds);
		}
	}

	static void updateRoots(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, RootState>> iterator = ROOTED_PLAYERS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, RootState> entry = iterator.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			RootState state = entry.getValue();
			if (player == null || !player.isAlive() || currentTick >= state.endTick || player.getEntityWorld().getRegistryKey() != state.dimension) {
				iterator.remove();
				continue;
			}

			player.setSprinting(false);
			player.setVelocity(0.0, 0.0, 0.0);
			if (player.squaredDistanceTo(state.lockedX, state.lockedY, state.lockedZ) > 0.0004) {
				player.requestTeleport(state.lockedX, state.lockedY, state.lockedZ);
			}
		}
	}

	static boolean isRooted(PlayerEntity player) {
		if (player == null) {
			return false;
		}
		RootState state = ROOTED_PLAYERS.get(player.getUuid());
		if (state == null) {
			return false;
		}
		int currentTick = currentTick(player);
		return currentTick != Integer.MIN_VALUE && currentTick < state.endTick && player.getEntityWorld().getRegistryKey() == state.dimension;
	}

	static void releaseRootForEnderPearlEscape(ServerPlayerEntity player) {
		if (!canUseEnderPearlWhileRooted(player, player.getMainHandStack()) && !canUseEnderPearlWhileRooted(player, player.getOffHandStack())) {
			return;
		}
		ROOTED_PLAYERS.remove(player.getUuid());
		removeRootStatusEffect(player, StatusEffects.SLOWNESS, MagicConfig.get().greed.tollkeepersClaim.rootSlownessAmplifier);
		removeRootStatusEffect(player, StatusEffects.MINING_FATIGUE, MagicConfig.get().greed.tollkeepersClaim.rootMiningFatigueAmplifier);
	}

	static void removeRootStatusEffect(
		ServerPlayerEntity player,
		net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect,
		int amplifier
	) {
		StatusEffectInstance instance = player.getStatusEffect(effect);
		if (instance != null && instance.getAmplifier() == amplifier) {
			player.removeStatusEffect(effect);
		}
	}

	static void updateSprintLocks(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, Integer>> iterator = SPRINT_LOCK_END_TICK.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Integer> entry = iterator.next();
			if (currentTick >= entry.getValue()) {
				iterator.remove();
				continue;
			}

			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			if (player == null || !player.isAlive()) {
				iterator.remove();
				continue;
			}
			player.setSprinting(false);
		}
	}

	static void updateShieldLocks(int currentTick) {
		SHIELD_LOCK_END_TICK.entrySet().removeIf(entry -> currentTick >= entry.getValue());
	}

	static void updateAbilityLocks(int currentTick) {
		ABILITY_LOCK_END_TICK.entrySet().removeIf(entry -> currentTick >= entry.getValue());
	}

	static void updateAttackSpeedLocks(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, AttackSpeedLockState>> iterator = ATTACK_SPEED_LOCKS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, AttackSpeedLockState> entry = iterator.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			if (player == null || !player.isAlive() || currentTick >= entry.getValue().endTick) {
				if (player != null) {
					removeAttackSpeedModifier(player);
				}
				iterator.remove();
				continue;
			}

			applyAttackSpeedModifier(player, entry.getValue().amount);
		}
	}

	static void updateAppraisersMarkMana(MinecraftServer server, int currentTick) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (!hasMarkedAppraisersTarget(player)) {
				continue;
			}

			int manaDrain = appraisersMarkManaDrain(player, TICKS_PER_SECOND);
			if (manaDrain <= 0) {
				continue;
			}

			int nextMana = Math.max(0, MagicPlayerData.getMana(player) - manaDrain);
			MagicPlayerData.setMana(player, nextMana);
			if (nextMana == 0) {
				MagicPlayerData.setDepletedRecoveryMode(player, true);
				endAppraisersMark(player, currentTick, true);
				player.sendMessage(Text.translatable("message.magic.ability.out_of_mana", MagicAbility.APPRAISERS_MARK.displayName()), true);
			}
		}
	}

	static void updateElytraEquipActions(MinecraftServer server) {
		HashSet<UUID> onlinePlayerIds = new HashSet<>();
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			UUID playerId = player.getUuid();
			onlinePlayerIds.add(playerId);
			boolean currentlyEquipped = player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
			Boolean previousState = LAST_ELYTRA_EQUIPPED_STATE.put(playerId, currentlyEquipped);
			if (previousState != null && !previousState && currentlyEquipped) {
				recordActionFromPlayer(player, "elytra_equip", null);
			}
		}
		LAST_ELYTRA_EQUIPPED_STATE.entrySet().removeIf(entry -> !onlinePlayerIds.contains(entry.getKey()));
		LAST_SPRINT_TRIGGER_TICK.entrySet().removeIf(entry -> !onlinePlayerIds.contains(entry.getKey()));
		LAST_JUMP_TRIGGER_TICK.entrySet().removeIf(entry -> !onlinePlayerIds.contains(entry.getKey()));
	}

	static void updateManaSickness(MinecraftServer server, int currentTick, int intervalTicks) {
		Set<UUID> activePlayers = new HashSet<>();
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (!player.hasStatusEffect(ModStatusEffects.manaSicknessEntry())) {
				MANA_SICKNESS_DRAIN_BUFFER.remove(player.getUuid());
				continue;
			}

			activePlayers.add(player.getUuid());
			double bufferedDrain = MANA_SICKNESS_DRAIN_BUFFER.getOrDefault(player.getUuid(), 0.0) + manaSicknessDrainExact(player, intervalTicks);
			int manaDrain = Math.max(0, (int) Math.floor(bufferedDrain + 1.0E-7));
			double remainingBuffer = Math.max(0.0, bufferedDrain - manaDrain);
			if (remainingBuffer > 1.0E-7) {
				MANA_SICKNESS_DRAIN_BUFFER.put(player.getUuid(), remainingBuffer);
			} else {
				MANA_SICKNESS_DRAIN_BUFFER.remove(player.getUuid());
			}

			if (manaDrain <= 0) {
				continue;
			}

			int nextMana = Math.max(0, MagicPlayerData.getMana(player) - manaDrain);
			MagicPlayerData.setMana(player, nextMana);
			if (nextMana == 0) {
				MagicPlayerData.setDepletedRecoveryMode(player, true);
			}
		}

		MANA_SICKNESS_DRAIN_BUFFER.entrySet().removeIf(entry -> !activePlayers.contains(entry.getKey()));
	}

	static void applyGildedBurden(ServerPlayerEntity player, int coinsSpent) {
		MagicConfig.TollkeepersClaimStageConfig stage = tollkeepersStage(coinsSpent);
		int refreshTicks = Math.max(1, MagicConfig.get().greed.tollkeepersClaim.burdenRefreshTicks);
		player.addStatusEffect(
			new StatusEffectInstance(
				ModStatusEffects.gildedBurdenEntry(),
				refreshTicks,
				MathHelper.clamp(coinsSpent - 1, 0, 4),
				true,
				false,
				true
			)
		);
		applyStageEffect(player, StatusEffects.SLOWNESS, refreshTicks, stage.slownessAmplifier);
	}

	static void applyCasterZoneBuff(ServerPlayerEntity caster, int coinsSpent) {
		MagicConfig.TollkeepersClaimStageConfig stage = tollkeepersStage(coinsSpent);
		if (stage.casterResistanceAmplifier < 0) {
			return;
		}

		caster.addStatusEffect(
			new StatusEffectInstance(
				StatusEffects.RESISTANCE,
				Math.max(1, MagicConfig.get().greed.tollkeepersClaim.burdenRefreshTicks),
				stage.casterResistanceAmplifier,
				true,
				false,
				true
			)
		);
	}

	static void applyRoot(ServerPlayerEntity target, TollkeepersClaimZoneState zone, int currentTick) {
		MagicConfig.TollkeepersClaimStageConfig stage = tollkeepersStage(zone.coinsSpent);
		int endTick = currentTick + Math.max(1, stage.rootDurationTicks);
		RootState existing = ROOTED_PLAYERS.get(target.getUuid());
		if (existing != null && existing.endTick > endTick) {
			endTick = existing.endTick;
		}

		ROOTED_PLAYERS.put(
			target.getUuid(),
			new RootState(target.getEntityWorld().getRegistryKey(), target.getX(), target.getY(), target.getZ(), endTick)
		);
		target.setSprinting(false);
		target.setVelocity(0.0, 0.0, 0.0);
		target.addStatusEffect(
			new StatusEffectInstance(
				StatusEffects.SLOWNESS,
				Math.max(1, endTick - currentTick),
				MagicConfig.get().greed.tollkeepersClaim.rootSlownessAmplifier,
				true,
				false,
				true
			)
		);
		target.addStatusEffect(
			new StatusEffectInstance(
				StatusEffects.MINING_FATIGUE,
				Math.max(1, endTick - currentTick),
				MagicConfig.get().greed.tollkeepersClaim.rootMiningFatigueAmplifier,
				true,
				false,
				true
			)
		);
		spawnChainParticles(target);
		playChainSound(target);
	}

	static void applyAttackSpeedLock(ServerPlayerEntity target, int endTick, double amount) {
		AttackSpeedLockState existing = ATTACK_SPEED_LOCKS.get(target.getUuid());
		int finalEndTick = existing == null ? endTick : Math.max(existing.endTick, endTick);
		double finalAmount = existing == null ? amount : Math.min(existing.amount, amount);
		ATTACK_SPEED_LOCKS.put(target.getUuid(), new AttackSpeedLockState(finalEndTick, finalAmount));
		applyAttackSpeedModifier(target, finalAmount);
	}
}

