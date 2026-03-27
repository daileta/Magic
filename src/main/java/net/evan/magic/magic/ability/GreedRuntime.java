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
import net.evan.magic.config.MagicConfig.BankruptcyStageConfig;
import net.evan.magic.config.MagicConfig.KingsDuesStageConfig;
import net.evan.magic.config.MagicConfig.TollkeepersClaimStageConfig;
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.MagicSchool;
import net.evan.magic.registry.ModStatusEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
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

public final class GreedRuntime {
	private static final int TICKS_PER_SECOND = 20;
	private static final int COIN_UNITS_PER_COIN = MagicPlayerData.GREED_COIN_UNITS_PER_COIN;
	private static final UUID COMMAND_COIN_SOURCE_ID = new UUID(0L, 1L);
	private static final Identifier KINGS_DUES_ATTACK_SPEED_MODIFIER_ID = Identifier.of(
		Magic.MOD_ID,
		"kings_dues_attack_speed_lock"
	);
	private static final ParticleEffect GOLD_NUGGET_PARTICLE = new ItemStackParticleEffect(
		ParticleTypes.ITEM,
		new ItemStack(Items.GOLD_NUGGET)
	);
	private static final Map<UUID, AppraisersMarkState> APPRAISERS_MARK_STATES = new HashMap<>();
	private static final Map<UUID, Integer> APPRAISERS_MARK_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, GreedCoinStorage> GREED_COIN_STORAGES = new HashMap<>();
	private static final Map<UUID, TollkeepersClaimZoneState> TOLLKEEPER_ZONES = new HashMap<>();
	private static final Map<UUID, RootState> ROOTED_PLAYERS = new HashMap<>();
	private static final Map<UUID, Integer> KINGS_DUES_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> BANKRUPTCY_COOLDOWN_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> SPRINT_LOCK_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> SHIELD_LOCK_END_TICK = new HashMap<>();
	private static final Map<UUID, Integer> ABILITY_LOCK_END_TICK = new HashMap<>();
	private static final Map<UUID, AttackSpeedLockState> ATTACK_SPEED_LOCKS = new HashMap<>();
	private static final Map<UUID, Double> MANA_SICKNESS_DRAIN_BUFFER = new HashMap<>();

	private GreedRuntime() {
	}

	public static boolean handleAbilityRequest(ServerPlayerEntity player, MagicAbility ability) {
		if (ability.school() != MagicSchool.GREED) {
			return false;
		}

		int currentTick = currentTick(player);
		if (isAbilityUseLocked(player, currentTick)) {
			player.sendMessage(Text.translatable("message.magic.greed.ability_locked"), true);
			return true;
		}

		if (ability == MagicAbility.APPRAISERS_MARK) {
			handleAppraisersMarkRequest(player, currentTick);
			return true;
		}
		if (ability == MagicAbility.TOLLKEEPERS_CLAIM) {
			handleTollkeepersClaimRequest(player, currentTick);
			return true;
		}
		if (ability == MagicAbility.KINGS_DUES) {
			handleKingsDuesRequest(player, currentTick);
			return true;
		}
		if (ability == MagicAbility.BANKRUPTCY) {
			handleBankruptcyRequest(player, currentTick);
			return true;
		}

		return false;
	}

	public static void cancelActiveAbilities(ServerPlayerEntity player, int currentTick) {
		endAppraisersMark(player, currentTick, true);
		TOLLKEEPER_ZONES.remove(player.getUuid());
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
			recordActionFromPlayer(player, "ignite_tnt");
			return;
		}

		if (!(world instanceof ServerWorld serverWorld) || !state.isOf(Blocks.RESPAWN_ANCHOR)) {
			return;
		}

		int charges = state.getOrEmpty(RespawnAnchorBlock.CHARGES).orElse(0);
		if (charges > 0 && !RespawnAnchorBlock.isUsable(serverWorld, pos)) {
			recordActionFromPlayer(player, "explode_respawn_anchor");
		}
	}

	public static void onPlayerAfterDamage(ServerPlayerEntity damagedPlayer, DamageSource source, float damageTaken) {
		ServerPlayerEntity attacker = attackingPlayerFrom(source);
		if (attacker == null || attacker == damagedPlayer || !attacker.isAlive() || attacker.isSpectator() || damageTaken <= 0.0F) {
			return;
		}

		if (isFallingMaceAttack(source)) {
			recordActionFromPlayer(attacker, "falling_mace_attack");
			return;
		}
		if (isFireworkShot(source)) {
			recordActionFromPlayer(attacker, "firework_shot");
			return;
		}
		if (isTippedArrowShot(source)) {
			recordActionFromPlayer(attacker, "tipped_arrow_shot");
			return;
		}
		if (isSpearChargeAttack(attacker, source)) {
			recordActionFromPlayer(attacker, "spear_charge_attack");
			return;
		}
		if (isSpearLunge(source)) {
			recordActionFromPlayer(attacker, "spear_lunge");
			return;
		}
		if (isFullChargeBowShot(source)) {
			recordActionFromPlayer(attacker, "full_charge_bow_shot");
			return;
		}
	}

	public static void onShieldDisabled(ServerPlayerEntity attacker) {
		recordActionFromPlayer(attacker, "disable_shield");
	}

	public static void onEndCrystalDestroyed(DamageSource source) {
		ServerPlayerEntity attacker = attackingPlayerFrom(source);
		if (attacker != null) {
			recordActionFromPlayer(attacker, "destroy_end_crystal");
		}
	}

	public static void onTntMinecartPrimed(DamageSource source) {
		ServerPlayerEntity attacker = attackingPlayerFrom(source);
		if (attacker != null) {
			recordActionFromPlayer(attacker, "ignite_tnt_minecart");
		}
	}

	public static void recordExternalAction(ServerPlayerEntity actor, String triggerId) {
		recordActionFromPlayer(actor, triggerId);
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

	public static void onEndServerTick(MinecraftServer server, int currentTick) {
		updateAppraisersMarks(server, currentTick);
		updateCoinStorages(server, currentTick);
		updateTollkeepersClaimZones(server, currentTick);
		updateRoots(server, currentTick);
		updateSprintLocks(server, currentTick);
		updateShieldLocks(currentTick);
		updateAbilityLocks(currentTick);
		updateAttackSpeedLocks(server, currentTick);

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
		removeAttackSpeedModifier(player);
	}

	public static void clearAllRuntimeState(ServerPlayerEntity player) {
		APPRAISERS_MARK_STATES.remove(player.getUuid());
		APPRAISERS_MARK_COOLDOWN_END_TICK.remove(player.getUuid());
		GREED_COIN_STORAGES.remove(player.getUuid());
		MagicPlayerData.setGreedCoinUnits(player, 0);
		TOLLKEEPER_ZONES.remove(player.getUuid());
		ROOTED_PLAYERS.remove(player.getUuid());
		SPRINT_LOCK_END_TICK.remove(player.getUuid());
		SHIELD_LOCK_END_TICK.remove(player.getUuid());
		ABILITY_LOCK_END_TICK.remove(player.getUuid());
		ATTACK_SPEED_LOCKS.remove(player.getUuid());
		KINGS_DUES_COOLDOWN_END_TICK.remove(player.getUuid());
		BANKRUPTCY_COOLDOWN_END_TICK.remove(player.getUuid());
		MANA_SICKNESS_DRAIN_BUFFER.remove(player.getUuid());
		removeAttackSpeedModifier(player);
		player.removeStatusEffect(ModStatusEffects.gildedBurdenEntry());
		player.removeStatusEffect(ModStatusEffects.manaSicknessEntry());
	}

	public static int cooldownRemaining(ServerPlayerEntity player, MagicAbility ability, int currentTick) {
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
			return TOLLKEEPER_ZONES.remove(playerId) != null ? 1 : 0;
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

		TollkeepersClaimStageConfig stage = tollkeepersStage(MathHelper.clamp(burden.getAmplifier() + 1, 1, 5));
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

	private static void handleAppraisersMarkRequest(ServerPlayerEntity player, int currentTick) {
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
		player.sendMessage(Text.translatable("message.magic.greed.appraisers_mark.waiting"), true);
	}

	private static void handleTollkeepersClaimRequest(ServerPlayerEntity player, int currentTick) {
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
		TOLLKEEPER_ZONES.put(
			player.getUuid(),
			new TollkeepersClaimZoneState(
				player.getUuid(),
				player.getEntityWorld().getRegistryKey(),
				centerX,
				centerY,
				centerZ,
				config.zoneRadius,
				spend,
				currentTick + durationTicks
			)
		);
		removeCoins(player, spend);
		if (player.getEntityWorld() instanceof ServerWorld serverWorld) {
			spawnZoneParticles(serverWorld, TOLLKEEPER_ZONES.get(player.getUuid()), currentTick);
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
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.TOLLKEEPERS_CLAIM.displayName()), true);
	}

	private static void handleKingsDuesRequest(ServerPlayerEntity player, int currentTick) {
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

		KingsDuesStageConfig stage = kingsDuesStage(spend);
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
		KINGS_DUES_COOLDOWN_END_TICK.put(player.getUuid(), currentTick + config.cooldownTicks);
		applyKingsDuesVisuals(target, spend);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.KINGS_DUES.displayName()), true);
	}

	private static void handleBankruptcyRequest(ServerPlayerEntity player, int currentTick) {
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

		BankruptcyStageConfig stage = bankruptcyStage(spend);
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
			MagicAbilityManager.cancelOwnedNonDomainMagicOnTarget(target, currentTick);
		}
		BANKRUPTCY_COOLDOWN_END_TICK.put(player.getUuid(), currentTick + config.cooldownTicks);
		applyBankruptcyVisuals(target);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.BANKRUPTCY.displayName()), true);
	}

	private static void recordActionFromPlayer(ServerPlayerEntity actor, String triggerId) {
		if (actor == null || triggerId == null || triggerId.isBlank()) {
			return;
		}

		Double configuredCoins = MagicConfig.get().greed.appraisersMark.coinTriggers.get(triggerId.trim().toLowerCase());
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

			addCoinUnits(caster, actor.getUuid(), configuredCoinUnits, currentTick);
		}
	}

	private static void updateAppraisersMarks(MinecraftServer server, int currentTick) {
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
				startCooldown(caster.getUuid(), APPRAISERS_MARK_COOLDOWN_END_TICK, MagicConfig.get().greed.appraisersMark.cooldownTicks, currentTick);
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

	private static void updateCoinStorages(MinecraftServer server, int currentTick) {
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
				storage.contributions.isEmpty() ||
				(storage.lastGainTick != Integer.MIN_VALUE &&
					currentTick - storage.lastGainTick >= MagicConfig.get().greed.appraisersMark.inactivityWipeTicks)
			) {
				iterator.remove();
				MagicPlayerData.setGreedCoinUnits(caster, 0);
				continue;
			}

			MagicPlayerData.setGreedCoinUnits(caster, totalCoinUnits(storage));
		}
	}

	private static void updateTollkeepersClaimZones(MinecraftServer server, int currentTick) {
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
				zone.nextParticleTick = currentTick + MagicConfig.get().greed.tollkeepersClaim.ringParticleIntervalTicks;
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

			Set<UUID> newInsidePlayerIds = new HashSet<>();
			for (ServerPlayerEntity player : world.getPlayers()) {
				if (player == caster || !player.isAlive() || player.isSpectator()) {
					continue;
				}

				boolean insideZone = isInsideZone(zone, player);
				if (insideZone) {
					newInsidePlayerIds.add(player.getUuid());
					applyGildedBurden(player, zone.coinsSpent);
					if (player.isSprinting() && isSprintBlocked(player)) {
						player.setSprinting(false);
					}
					continue;
				}

				if (zone.insidePlayerIds.contains(player.getUuid())) {
					applyRoot(player, zone, currentTick);
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

			zone.insidePlayerIds.clear();
			zone.insidePlayerIds.addAll(newInsidePlayerIds);
		}
	}

	private static void updateRoots(MinecraftServer server, int currentTick) {
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

	private static void updateSprintLocks(MinecraftServer server, int currentTick) {
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

	private static void updateShieldLocks(int currentTick) {
		SHIELD_LOCK_END_TICK.entrySet().removeIf(entry -> currentTick >= entry.getValue());
	}

	private static void updateAbilityLocks(int currentTick) {
		ABILITY_LOCK_END_TICK.entrySet().removeIf(entry -> currentTick >= entry.getValue());
	}

	private static void updateAttackSpeedLocks(MinecraftServer server, int currentTick) {
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

	private static void updateAppraisersMarkMana(MinecraftServer server, int currentTick) {
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

	private static void updateManaSickness(MinecraftServer server, int currentTick, int intervalTicks) {
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

	private static void applyGildedBurden(ServerPlayerEntity player, int coinsSpent) {
		TollkeepersClaimStageConfig stage = tollkeepersStage(coinsSpent);
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

	private static void applyRoot(ServerPlayerEntity target, TollkeepersClaimZoneState zone, int currentTick) {
		TollkeepersClaimStageConfig stage = tollkeepersStage(zone.coinsSpent);
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

	private static void applyAttackSpeedLock(ServerPlayerEntity target, int endTick, double amount) {
		AttackSpeedLockState existing = ATTACK_SPEED_LOCKS.get(target.getUuid());
		int finalEndTick = existing == null ? endTick : Math.max(existing.endTick, endTick);
		double finalAmount = existing == null ? amount : Math.min(existing.amount, amount);
		ATTACK_SPEED_LOCKS.put(target.getUuid(), new AttackSpeedLockState(finalEndTick, finalAmount));
		applyAttackSpeedModifier(target, finalAmount);
	}

	private static void applyAttackSpeedModifier(PlayerEntity player, double amount) {
		EntityAttributeInstance attributeInstance = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
		if (attributeInstance == null) {
			return;
		}

		EntityAttributeModifier existing = attributeInstance.getModifier(KINGS_DUES_ATTACK_SPEED_MODIFIER_ID);
		if (existing != null && existing.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL && Math.abs(existing.value() - amount) <= 1.0E-6) {
			return;
		}
		if (existing != null) {
			attributeInstance.removeModifier(KINGS_DUES_ATTACK_SPEED_MODIFIER_ID);
		}
		if (Math.abs(amount) <= 1.0E-6) {
			return;
		}
		attributeInstance.addTemporaryModifier(
			new EntityAttributeModifier(KINGS_DUES_ATTACK_SPEED_MODIFIER_ID, amount, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
		);
	}

	private static void removeAttackSpeedModifier(PlayerEntity player) {
		EntityAttributeInstance attributeInstance = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
		if (attributeInstance != null) {
			attributeInstance.removeModifier(KINGS_DUES_ATTACK_SPEED_MODIFIER_ID);
		}
	}

	private static void applyKingsDuesVisuals(ServerPlayerEntity target, int spend) {
		ServerWorld world = (ServerWorld) target.getEntityWorld();
		world.spawnParticles(GOLD_NUGGET_PARTICLE, target.getX(), target.getBodyY(0.65), target.getZ(), 12, 0.25, 0.35, 0.25, 0.04);
		world.spawnParticles(ParticleTypes.GLOW, target.getX(), target.getBodyY(0.65), target.getZ(), 8, 0.3, 0.4, 0.3, 0.0);
		if (spend >= 4) {
			spawnChainParticles(target);
		}
		world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 0.7F, 1.25F);
	}

	private static void applyBankruptcyVisuals(ServerPlayerEntity target) {
		ServerWorld world = (ServerWorld) target.getEntityWorld();
		world.spawnParticles(ParticleTypes.ENCHANT, target.getX(), target.getBodyY(0.7), target.getZ(), 14, 0.35, 0.4, 0.35, 0.15);
		world.spawnParticles(ParticleTypes.GLOW, target.getX(), target.getBodyY(0.7), target.getZ(), 10, 0.3, 0.35, 0.3, 0.0);
		world.playSound(
			null,
			target.getX(),
			target.getY(),
			target.getZ(),
			SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
			SoundCategory.PLAYERS,
			0.8F,
			0.85F
		);
	}

	private static void applyStageEffect(ServerPlayerEntity player, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int durationTicks, int amplifier) {
		if (durationTicks <= 0 || amplifier < 0) {
			return;
		}

		player.addStatusEffect(new StatusEffectInstance(effect, durationTicks, amplifier, true, true, true));
	}

	private static void endAppraisersMark(ServerPlayerEntity player, int currentTick, boolean startCooldownIfMarked) {
		AppraisersMarkState state = APPRAISERS_MARK_STATES.remove(player.getUuid());
		if (state == null) {
			return;
		}

		if (startCooldownIfMarked && state.stage == AppraisersMarkStage.MARKED) {
			startCooldown(
				player.getUuid(),
				APPRAISERS_MARK_COOLDOWN_END_TICK,
				MagicConfig.get().greed.appraisersMark.cooldownTicks,
				currentTick
			);
		}
	}

	private static void clearCoins(ServerPlayerEntity player) {
		GREED_COIN_STORAGES.remove(player.getUuid());
		MagicPlayerData.setGreedCoinUnits(player, 0);
	}

	private static int spendAmount(ServerPlayerEntity player, int minimumCoins, int maximumCoins) {
		GreedCoinStorage storage = GREED_COIN_STORAGES.get(player.getUuid());
		if (storage == null) {
			return 0;
		}

		int available = availableWholeCoins(storage);
		if (available < minimumCoins) {
			return 0;
		}

		return Math.min(available, maximumCoins);
	}

	private static void addCoinUnits(ServerPlayerEntity caster, UUID sourcePlayerId, int coinUnits, int currentTick) {
		if (coinUnits <= 0) {
			return;
		}

		GreedCoinStorage storage = GREED_COIN_STORAGES.computeIfAbsent(caster.getUuid(), ignored -> new GreedCoinStorage());
		expireContributions(storage, currentTick);
		boolean commandContribution = COMMAND_COIN_SOURCE_ID.equals(sourcePlayerId);
		if (!commandContribution && !storage.contributions.containsKey(sourcePlayerId)) {
			while (trackedPlayerContributionCount(storage) >= MagicConfig.get().greed.appraisersMark.maxTrackedPlayers) {
				if (!removeOldestTrackedPlayerContribution(storage)) {
					break;
				}
			}
		}

		int availableRoom = Math.max(0, coinUnitsFromConfiguredCoins(MagicConfig.get().greed.appraisersMark.maxCoins) - totalCoinUnits(storage));
		if (availableRoom <= 0) {
			MagicPlayerData.setGreedCoinUnits(caster, totalCoinUnits(storage));
			return;
		}

		GreedCoinContribution contribution = storage.contributions.get(sourcePlayerId);
		if (contribution == null) {
			contribution = new GreedCoinContribution(0, currentTick + MagicConfig.get().greed.appraisersMark.contributionLifetimeTicks);
			storage.contributions.put(sourcePlayerId, contribution);
		}

		int allowed = Math.min(coinUnits, availableRoom);
		if (allowed <= 0) {
			MagicPlayerData.setGreedCoinUnits(caster, totalCoinUnits(storage));
			return;
		}

		contribution.coinUnits += allowed;
		contribution.expiresTick = currentTick + MagicConfig.get().greed.appraisersMark.contributionLifetimeTicks;
		storage.lastGainTick = currentTick;
		MagicPlayerData.setGreedCoinUnits(caster, totalCoinUnits(storage));
		if (MagicConfig.get().greed.appraisersMark.playCoinGainSound) {
			caster.playSound(
				SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
				MagicConfig.get().greed.appraisersMark.coinGainSoundVolume,
				MagicConfig.get().greed.appraisersMark.coinGainSoundPitch
			);
		}
	}

	private static void removeCoins(ServerPlayerEntity caster, int coinsToSpend) {
		int coinUnitsToSpend = coinUnitsFromWholeCoins(coinsToSpend);
		GreedCoinStorage storage = GREED_COIN_STORAGES.get(caster.getUuid());
		if (storage == null || coinUnitsToSpend <= 0) {
			return;
		}

		int remaining = coinUnitsToSpend;
		Iterator<Map.Entry<UUID, GreedCoinContribution>> iterator = storage.contributions.entrySet().iterator();
		while (iterator.hasNext() && remaining > 0) {
			GreedCoinContribution contribution = iterator.next().getValue();
			int spent = Math.min(contribution.coinUnits, remaining);
			contribution.coinUnits -= spent;
			remaining -= spent;
			if (contribution.coinUnits <= 0) {
				iterator.remove();
			}
		}

		if (storage.contributions.isEmpty()) {
			GREED_COIN_STORAGES.remove(caster.getUuid());
			MagicPlayerData.setGreedCoinUnits(caster, 0);
			return;
		}

		MagicPlayerData.setGreedCoinUnits(caster, totalCoinUnits(storage));
	}

	private static int totalCoinUnits(GreedCoinStorage storage) {
		int total = 0;
		for (GreedCoinContribution contribution : storage.contributions.values()) {
			total += contribution.coinUnits;
		}
		return total;
	}

	private static int availableWholeCoins(GreedCoinStorage storage) {
		return totalCoinUnits(storage) / COIN_UNITS_PER_COIN;
	}

	private static void expireContributions(GreedCoinStorage storage, int currentTick) {
		Iterator<Map.Entry<UUID, GreedCoinContribution>> iterator = storage.contributions.entrySet().iterator();
		while (iterator.hasNext()) {
			if (currentTick >= iterator.next().getValue().expiresTick) {
				iterator.remove();
			}
		}
	}

	private static int trackedPlayerContributionCount(GreedCoinStorage storage) {
		int count = 0;
		for (UUID contributorId : storage.contributions.keySet()) {
			if (!COMMAND_COIN_SOURCE_ID.equals(contributorId)) {
				count++;
			}
		}
		return count;
	}

	private static boolean removeOldestTrackedPlayerContribution(GreedCoinStorage storage) {
		Iterator<Map.Entry<UUID, GreedCoinContribution>> iterator = storage.contributions.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, GreedCoinContribution> entry = iterator.next();
			if (COMMAND_COIN_SOURCE_ID.equals(entry.getKey())) {
				continue;
			}
			iterator.remove();
			return true;
		}
		return false;
	}

	private static void spawnMarkedParticles(ServerWorld world, ServerPlayerEntity target) {
		MagicConfig.AppraisersMarkConfig config = MagicConfig.get().greed.appraisersMark;
		world.spawnParticles(
			ParticleTypes.WAX_ON,
			target.getX(),
			target.getBodyY(0.12),
			target.getZ(),
			config.markParticleCount,
			config.markParticleHorizontalSpread,
			config.markParticleVerticalSpread,
			config.markParticleHorizontalSpread,
			0.01
		);
	}

	private static void spawnZoneParticles(ServerWorld world, TollkeepersClaimZoneState zone, int currentTick) {
		MagicConfig.TollkeepersClaimConfig config = MagicConfig.get().greed.tollkeepersClaim;
		int verticalPoints = Math.max(2, config.ringVerticalPoints);
		double spinRadians = Math.toRadians(config.ringSpinDegreesPerTick * currentTick);
		double waveTravelRadians = Math.toRadians(config.ringWaveDegreesPerTick * currentTick);
		for (int verticalIndex = 0; verticalIndex < verticalPoints; verticalIndex++) {
			double heightProgress = verticalPoints == 1 ? 0.0 : verticalIndex / (double) (verticalPoints - 1);
			double height = config.ringColumnHeight * heightProgress;
			double verticalTwistRadians = Math.toRadians(config.ringTwistDegreesPerBlock * height);
			for (int i = 0; i < config.ringParticlePoints; i++) {
				double baseAngle = Math.PI * 2.0 * i / config.ringParticlePoints;
				double waveRadians = heightProgress * Math.PI * 2.0 * config.ringWaveCyclesPerColumn + baseAngle - waveTravelRadians;
				double radius =
					zone.radius +
					tollkeepersCurveOutwardOffset(config, height) +
					Math.sin(waveRadians) * config.ringWaveRadiusAmplitude;
				double angle = baseAngle + spinRadians + verticalTwistRadians + Math.cos(waveRadians) * 0.16;
				double ringY = zone.centerY + height + Math.cos(waveRadians) * config.ringWaveVerticalAmplitude;
				double ringX = zone.centerX + Math.cos(angle) * radius;
				double ringZ = zone.centerZ + Math.sin(angle) * radius;
				world.spawnParticles(ParticleTypes.WAX_ON, ringX, ringY, ringZ, 1, 0.0, 0.0, 0.0, 0.0);
				world.spawnParticles(ParticleTypes.GLOW, ringX, ringY, ringZ, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}
		if (config.risingParticleCount > 0) {
			world.spawnParticles(
				GOLD_NUGGET_PARTICLE,
				zone.centerX,
				zone.centerY + 0.2,
				zone.centerZ,
				config.risingParticleCount,
				zone.radius * 0.45,
				0.12,
				zone.radius * 0.45,
				0.02
			);
			world.spawnParticles(
				ParticleTypes.GLOW,
				zone.centerX,
				zone.centerY + 0.25,
				zone.centerZ,
				Math.max(1, config.risingParticleCount / 2),
				zone.radius * 0.4,
				0.18,
				zone.radius * 0.4,
				0.0
			);
		}
	}

	private static double tollkeepersCurveOutwardOffset(MagicConfig.TollkeepersClaimConfig config, double height) {
		if (height <= config.ringCurveStartHeight || config.ringColumnHeight <= config.ringCurveStartHeight + 1.0E-6) {
			return 0.0;
		}

		double progress = (height - config.ringCurveStartHeight) / (config.ringColumnHeight - config.ringCurveStartHeight);
		double curvedProgress = MathHelper.clamp(progress, 0.0, 1.0);
		return config.ringCurveOutwardRadius * curvedProgress * curvedProgress;
	}

	private static void spawnChainParticles(ServerPlayerEntity player) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		world.spawnParticles(GOLD_NUGGET_PARTICLE, player.getX(), player.getBodyY(0.65), player.getZ(), 8, 0.22, 0.3, 0.22, 0.03);
		world.spawnParticles(ParticleTypes.GLOW, player.getX(), player.getBodyY(0.65), player.getZ(), 6, 0.25, 0.32, 0.25, 0.0);
	}

	private static void playChainSound(ServerPlayerEntity player) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 0.5F, 1.15F);
	}

	private static double manaFromPercentExact(double percent) {
		return MagicPlayerData.MAX_MANA * MathHelper.clamp(percent, 0.0, 100.0) / 100.0;
	}

	private static double appraisersMarkDrainExact(int intervalTicks) {
		double percentPerSecond = MagicConfig.get().greed.appraisersMark.markedDrainPercentPerSecond;
		return manaFromPercentExact(percentPerSecond) * intervalTicks / TICKS_PER_SECOND;
	}

	private static int appraisersMarkManaDrain(ServerPlayerEntity player, int intervalTicks) {
		AppraisersMarkState state = APPRAISERS_MARK_STATES.get(player.getUuid());
		if (state == null || state.stage != AppraisersMarkStage.MARKED) {
			return 0;
		}

		state.drainBuffer += appraisersMarkDrainExact(intervalTicks);
		int drain = Math.max(0, (int) Math.floor(state.drainBuffer + 1.0E-7));
		state.drainBuffer = Math.max(0.0, state.drainBuffer - drain);
		return drain;
	}

	private static double manaSicknessDrainExact(PlayerEntity player, int intervalTicks) {
		StatusEffectInstance statusEffect = player.getStatusEffect(ModStatusEffects.manaSicknessEntry());
		if (statusEffect == null) {
			return 0.0;
		}

		MagicConfig.ManaSicknessConfig config = MagicConfig.get().greed.manaSickness;
		int level = statusEffect.getAmplifier() + 1;
		double percentPerSecond = switch (level) {
			case 1 -> config.levelOneDrainPercentPerSecond;
			case 2 -> config.levelTwoDrainPercentPerSecond;
			case 3 -> config.levelThreeDrainPercentPerSecond;
			case 4 -> config.levelFourDrainPercentPerSecond;
			case 5 -> config.levelFiveDrainPercentPerSecond;
			case 6 -> config.levelSixDrainPercentPerSecond;
			case 7 -> config.levelSevenDrainPercentPerSecond;
			case 8 -> config.levelEightDrainPercentPerSecond;
			default -> config.levelNinePlusDrainPercentPerSecond;
		};
		return manaFromPercentExact(percentPerSecond) * intervalTicks / TICKS_PER_SECOND;
	}

	private static void sendCooldownMessage(ServerPlayerEntity player, MagicAbility ability, int remainingTicks) {
		int secondsRemaining = (int) Math.ceil(remainingTicks / 20.0);
		player.sendMessage(Text.translatable("message.magic.ability.cooldown", ability.displayName(), secondsRemaining), true);
	}

	private static BlockHitResult findTargetedBlock(ServerPlayerEntity caster, double range) {
		Vec3d eyePos = caster.getEyePos();
		Vec3d look = caster.getRotationVec(1.0F);
		Vec3d end = eyePos.add(look.multiply(Math.max(1.0, range)));
		BlockHitResult hitResult = caster.getEntityWorld().raycast(
			new RaycastContext(eyePos, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, caster)
		);
		return hitResult.getType() == HitResult.Type.BLOCK ? hitResult : null;
	}

	private static ServerPlayerEntity findPlayerTargetInLineOfSight(ServerPlayerEntity caster, double range, boolean requireLineOfSight) {
		Vec3d eyePos = caster.getEyePos();
		Vec3d look = caster.getRotationVec(1.0F);
		double clampedRange = Math.max(1.0, range);
		Vec3d end = eyePos.add(look.multiply(clampedRange));
		Box area = caster.getBoundingBox().stretch(look.multiply(clampedRange)).expand(1.0);
		Predicate<Entity> filter = entity ->
			entity instanceof ServerPlayerEntity target &&
			target.isAlive() &&
			target != caster &&
			!target.isSpectator();

		EntityHitResult hitResult = ProjectileUtil.raycast(caster, eyePos, end, area, filter, clampedRange * clampedRange);
		if (hitResult == null || !(hitResult.getEntity() instanceof ServerPlayerEntity target)) {
			return null;
		}
		if (requireLineOfSight && !caster.canSee(target)) {
			return null;
		}
		if (!requireLineOfSight) {
			return target;
		}

		BlockHitResult blockHitResult = caster.getEntityWorld().raycast(
			new RaycastContext(
				eyePos,
				hitResult.getPos(),
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				caster
			)
		);
		if (blockHitResult.getType() == HitResult.Type.BLOCK) {
			double blockDistance = eyePos.squaredDistanceTo(blockHitResult.getPos());
			double targetDistance = eyePos.squaredDistanceTo(hitResult.getPos());
			if (blockDistance < targetDistance - 1.0E-6) {
				return null;
			}
		}
		return target;
	}

	private static boolean isInsideZone(TollkeepersClaimZoneState zone, ServerPlayerEntity player) {
		double dx = player.getX() - zone.centerX;
		double dz = player.getZ() - zone.centerZ;
		return dx * dx + dz * dz <= zone.radius * zone.radius && Math.abs(player.getY() - zone.centerY) <= 3.0;
	}

	private static boolean isMarkedTarget(ServerPlayerEntity caster, ServerPlayerEntity target) {
		AppraisersMarkState state = APPRAISERS_MARK_STATES.get(caster.getUuid());
		return state != null && state.stage == AppraisersMarkStage.MARKED && target.getUuid().equals(state.markedTargetId);
	}

	private static int coinUnitsFromWholeCoins(int coins) {
		return Math.max(0, coins) * COIN_UNITS_PER_COIN;
	}

	private static int coinUnitsFromConfiguredCoins(double coins) {
		return Math.max(0, (int) Math.round(Math.max(0.0, coins) * COIN_UNITS_PER_COIN));
	}

	private static boolean isFallingMaceAttack(DamageSource source) {
		return source.isOf(DamageTypes.MACE_SMASH) || source.isIn(DamageTypeTags.MACE_SMASH);
	}

	private static boolean isFireworkShot(DamageSource source) {
		return source.isOf(DamageTypes.FIREWORKS) && source.getSource() instanceof FireworkRocketEntity firework && firework.wasShotAtAngle();
	}

	private static boolean isTippedArrowShot(DamageSource source) {
		return source.isOf(DamageTypes.ARROW) && source.getSource() instanceof ArrowEntity arrow && arrow.getColor() != -1;
	}

	private static boolean isSpearChargeAttack(ServerPlayerEntity attacker, DamageSource source) {
		return source.isOf(DamageTypes.TRIDENT) && attacker.isUsingRiptide();
	}

	private static boolean isSpearLunge(DamageSource source) {
		return source.isOf(DamageTypes.TRIDENT);
	}

	private static boolean isFullChargeBowShot(DamageSource source) {
		if (!source.isOf(DamageTypes.ARROW) || !(source.getSource() instanceof PersistentProjectileEntity projectile)) {
			return false;
		}

		ItemStack weaponStack = projectile.getWeaponStack();
		return weaponStack != null && !weaponStack.isEmpty() && weaponStack.getItem() instanceof BowItem && projectile.isCritical();
	}

	private static ServerPlayerEntity attackingPlayerFrom(DamageSource source) {
		Entity attacker = source.getAttacker();
		if (attacker instanceof ServerPlayerEntity serverPlayer) {
			return serverPlayer;
		}
		if (attacker instanceof ProjectileEntity projectile && projectile.getOwner() instanceof ServerPlayerEntity owner) {
			return owner;
		}

		Entity directSource = source.getSource();
		if (directSource instanceof ServerPlayerEntity serverPlayer) {
			return serverPlayer;
		}
		if (directSource instanceof ProjectileEntity projectile && projectile.getOwner() instanceof ServerPlayerEntity owner) {
			return owner;
		}
		return null;
	}

	private static TollkeepersClaimStageConfig tollkeepersStage(int coinsSpent) {
		MagicConfig.TollkeepersClaimConfig config = MagicConfig.get().greed.tollkeepersClaim;
		return switch (MathHelper.clamp(coinsSpent, 1, 5)) {
			case 1 -> config.stageOne;
			case 2 -> config.stageTwo;
			case 3 -> config.stageThree;
			case 4 -> config.stageFour;
			default -> config.stageFive;
		};
	}

	private static KingsDuesStageConfig kingsDuesStage(int coinsSpent) {
		MagicConfig.KingsDuesConfig config = MagicConfig.get().greed.kingsDues;
		return switch (MathHelper.clamp(coinsSpent, 1, 6)) {
			case 1 -> config.stageOne;
			case 2 -> config.stageTwo;
			case 3 -> config.stageThree;
			case 4 -> config.stageFour;
			case 5 -> config.stageFive;
			default -> config.stageSix;
		};
	}

	private static BankruptcyStageConfig bankruptcyStage(int coinsSpent) {
		MagicConfig.BankruptcyConfig config = MagicConfig.get().greed.bankruptcy;
		return switch (MathHelper.clamp(coinsSpent, 2, 8)) {
			case 2 -> config.stageTwo;
			case 3 -> config.stageThree;
			case 4 -> config.stageFour;
			case 5 -> config.stageFive;
			case 6 -> config.stageSix;
			case 7 -> config.stageSeven;
			default -> config.stageEight;
		};
	}

	private static void startCooldown(UUID playerId, Map<UUID, Integer> cooldownMap, int cooldownTicks, int currentTick) {
		if (cooldownTicks > 0) {
			cooldownMap.put(playerId, currentTick + cooldownTicks);
		}
	}

	private static int currentTick(PlayerEntity player) {
		MinecraftServer server = player.getEntityWorld().getServer();
		return server == null ? Integer.MIN_VALUE : server.getTicks();
	}

	private enum AppraisersMarkStage {
		WAITING,
		MARKED
	}

	private static final class AppraisersMarkState {
		private AppraisersMarkStage stage;
		private UUID markedTargetId;
		private double drainBuffer;
		private int lastParticleTick;

		private AppraisersMarkState(AppraisersMarkStage stage) {
			this.stage = stage;
			this.lastParticleTick = Integer.MIN_VALUE;
		}

		private static AppraisersMarkState waiting() {
			return new AppraisersMarkState(AppraisersMarkStage.WAITING);
		}
	}

	private static final class GreedCoinStorage {
		private final LinkedHashMap<UUID, GreedCoinContribution> contributions = new LinkedHashMap<>();
		private int lastGainTick = Integer.MIN_VALUE;
	}

	private static final class GreedCoinContribution {
		private int coinUnits;
		private int expiresTick;

		private GreedCoinContribution(int coinUnits, int expiresTick) {
			this.coinUnits = coinUnits;
			this.expiresTick = expiresTick;
		}
	}

	private static final class TollkeepersClaimZoneState {
		private final UUID casterId;
		private final net.minecraft.registry.RegistryKey<World> dimension;
		private final double centerX;
		private final double centerY;
		private final double centerZ;
		private final double radius;
		private final int coinsSpent;
		private final int expiresTick;
		private int nextParticleTick;
		private int nextSoundTick;
		private final Set<UUID> insidePlayerIds = new HashSet<>();

		private TollkeepersClaimZoneState(
			UUID casterId,
			net.minecraft.registry.RegistryKey<World> dimension,
			double centerX,
			double centerY,
			double centerZ,
			double radius,
			int coinsSpent,
			int expiresTick
		) {
			this.casterId = casterId;
			this.dimension = dimension;
			this.centerX = centerX;
			this.centerY = centerY;
			this.centerZ = centerZ;
			this.radius = radius;
			this.coinsSpent = coinsSpent;
			this.expiresTick = expiresTick;
			this.nextParticleTick = Integer.MIN_VALUE;
			this.nextSoundTick = Integer.MIN_VALUE;
		}
	}

	private static final class RootState {
		private final net.minecraft.registry.RegistryKey<World> dimension;
		private final double lockedX;
		private final double lockedY;
		private final double lockedZ;
		private final int endTick;

		private RootState(net.minecraft.registry.RegistryKey<World> dimension, double lockedX, double lockedY, double lockedZ, int endTick) {
			this.dimension = dimension;
			this.lockedX = lockedX;
			this.lockedY = lockedY;
			this.lockedZ = lockedZ;
			this.endTick = endTick;
		}
	}

	private static final class AttackSpeedLockState {
		private final int endTick;
		private final double amount;

		private AttackSpeedLockState(int endTick, double amount) {
			this.endTick = endTick;
			this.amount = amount;
		}
	}
}
