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

final class GreedDomainRuntime {
	private static final int COIN_UNITS_PER_COIN = MagicPlayerData.GREED_COIN_UNITS_PER_COIN;
	private static final int DISPLAY_REFRESH_TICKS = 40;
	private static final double DISPLAY_VERTICAL_OFFSET = 2.85;
	private static final double INTRO_FREEZE_POSITION_EPSILON_SQUARED = 1.0E-4;
	private static final float INTRO_FREEZE_ROTATION_EPSILON_DEGREES = 0.1F;
	private static final int HOTBAR_SLOT_COUNT = 9;
	private static final int MAIN_INVENTORY_SLOT_END = 36;
	private static final int ARMOR_SLOT_START = 36;
	private static final int ARMOR_SLOT_END = 40;
	private static final int OFFHAND_SLOT = 40;
	private static final Identifier GREED_ATTACK_DAMAGE_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_attack_damage_penalty");
	private static final Identifier GREED_ARMOR_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_armor_penalty");
	private static final Identifier GREED_ARMOR_TOUGHNESS_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_armor_toughness_penalty");
	private static final Identifier GREED_MOVEMENT_SPEED_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_movement_speed_penalty");
	private static final Identifier GREED_ATTACK_SPEED_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_attack_speed_penalty");
	private static final Identifier GREED_MAX_HEALTH_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_max_health_penalty");
	private static final Identifier GREED_KNOCKBACK_RESISTANCE_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_knockback_resistance_penalty");
	private static final Identifier GREED_SCALE_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_scale_penalty");
	private static int DOMAIN_RADIUS = 25;
	private static int DOMAIN_HEIGHT = 25;
	private static int DOMAIN_SHELL_THICKNESS = 1;
	private static int DOMAIN_DURATION_TICKS = 90 * 20;
	private static int DOMAIN_COOLDOWN_TICKS = 36000;
	private static BlockState CARPET_BLOCK_STATE = Blocks.RED_CARPET.getDefaultState();
	private static List<WeightedBlockState> SHELL_PALETTE = List.of(
		new WeightedBlockState(Blocks.GILDED_BLACKSTONE.getDefaultState(), 6),
		new WeightedBlockState(Blocks.BLACKSTONE.getDefaultState(), 5),
		new WeightedBlockState(Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 5),
		new WeightedBlockState(Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState(), 2),
		new WeightedBlockState(Blocks.GOLD_BLOCK.getDefaultState(), 2)
	);
	private static List<BlockState> THRONE_BLOCK_STATES = List.of(Blocks.GOLD_BLOCK.getDefaultState());
	private static List<BlockState> PILLAR_BLOCK_STATES = List.of(Blocks.GOLD_BLOCK.getDefaultState());
	private static int PILLAR_COUNT = 8;
	private static double PILLAR_RADIUS = 9.0;
	private static double PILLAR_SPACING_DEGREES = 45.0;
	private static int PILLAR_HEIGHT = 23;
	private static int PILLAR_WIDTH = 3;
	private static int CARPET_WIDTH = 3;
	private static int CARPET_EXTENSION_PAST_CENTER = 10;
	private static String THRONE_SIDE = "south";
	private static int THRONE_OFFSET = 12;
	private static int THRONE_WIDTH = 7;
	private static int THRONE_DEPTH = 5;
	private static int THRONE_HEIGHT = 7;
	private static boolean PROTECT_INTERIOR_STRUCTURES = true;
	private static final Map<UUID, ActiveDomainState> ACTIVE_DOMAINS = new HashMap<>();
	private static final Map<OwnerTargetKey, FrozenTributeState> FROZEN_TRIBUTES = new HashMap<>();
	private static final Map<UUID, ArrayList<DebtPenaltyState>> DEBT_PENALTIES = new HashMap<>();
	private static final Map<UUID, Map<UUID, PersistentMarkState>> PERSISTENT_MARKS_BY_OWNER = new HashMap<>();
	private static boolean initialized;

	private GreedDomainRuntime() {
	}

	static void reloadConfigValues() {
		MagicConfig.GreedDomainConfig config = MagicConfig.get().greed.domain;
		DOMAIN_RADIUS = config.radius;
		DOMAIN_HEIGHT = config.height;
		DOMAIN_SHELL_THICKNESS = config.shellThickness;
		DOMAIN_DURATION_TICKS = config.durationTicks;
		DOMAIN_COOLDOWN_TICKS = config.cooldownTicks;
		CARPET_BLOCK_STATE = parseBlockState(config.structure.carpetBlockId, Blocks.RED_CARPET.getDefaultState());
		SHELL_PALETTE = parseWeightedPalette(config.structure.shellPalette);
		THRONE_BLOCK_STATES = parseBlockStateList(config.structure.throneBlockIds, List.of(Blocks.GOLD_BLOCK.getDefaultState()));
		PILLAR_BLOCK_STATES = parseBlockStateList(config.structure.pillarBlockIds, List.of(Blocks.GOLD_BLOCK.getDefaultState()));
		PILLAR_COUNT = config.structure.pillarCount;
		PILLAR_RADIUS = config.structure.pillarPlacementRadius;
		PILLAR_SPACING_DEGREES = config.structure.pillarSpacingDegrees;
		PILLAR_HEIGHT = config.structure.pillarHeight;
		PILLAR_WIDTH = config.structure.pillarWidth;
		CARPET_WIDTH = config.structure.carpetWidth;
		CARPET_EXTENSION_PAST_CENTER = config.structure.carpetExtensionPastCenter;
		THRONE_SIDE = config.structure.throneSide;
		THRONE_OFFSET = config.structure.throneOffset;
		THRONE_WIDTH = config.structure.throneWidth;
		THRONE_DEPTH = config.structure.throneDepth;
		THRONE_HEIGHT = config.structure.throneHeight;
		PROTECT_INTERIOR_STRUCTURES = config.structure.protectInteriorStructures;
		GreedItemCategoryHelper.reloadConfigValues();
		initialized = true;
	}

	static int domainRadius() {
		ensureInitialized();
		return DOMAIN_RADIUS;
	}

	static int domainHeight() {
		ensureInitialized();
		return DOMAIN_HEIGHT;
	}

	static int domainShellThickness() {
		ensureInitialized();
		return DOMAIN_SHELL_THICKNESS;
	}

	static int domainDurationTicks() {
		ensureInitialized();
		return DOMAIN_DURATION_TICKS;
	}

	static int domainCooldownTicks() {
		ensureInitialized();
		return DOMAIN_COOLDOWN_TICKS;
	}

	static BlockState resolveDomainState(
		boolean shell,
		int centerX,
		int centerZ,
		int baseY,
		int radius,
		int height,
		int innerRadius,
		int innerHeight,
		BlockPos pos
	) {
		ensureInitialized();
		int relativeY = pos.getY() - baseY;
		StructureBlock structureBlock = structureBlockAt(pos, centerX, centerZ, baseY, innerRadius, innerHeight);
		if (structureBlock != StructureBlock.NONE) {
			return switch (structureBlock) {
				case CARPET -> CARPET_BLOCK_STATE;
				case THRONE -> patternedPaletteState(pos, THRONE_BLOCK_STATES);
				case PILLAR -> patternedPaletteState(pos, PILLAR_BLOCK_STATES);
				default -> Blocks.AIR.getDefaultState();
			};
		}
		if (shell) {
			return weightedShellState(pos).state();
		}
		if (relativeY <= 0) {
			return weightedShellState(pos).state();
		}
		BlockState sharedLightState = MagicAbilityManager.resolveSharedDomainInteriorLightState(pos, baseY, centerX, centerZ, innerRadius, innerHeight);
		if (sharedLightState != null) {
			return sharedLightState;
		}
		return Blocks.AIR.getDefaultState();
	}

	static boolean isProtectedInteriorStructureBlock(
		int centerX,
		int centerZ,
		int baseY,
		int innerRadius,
		int innerHeight,
		BlockPos pos
	) {
		ensureInitialized();
		return PROTECT_INTERIOR_STRUCTURES
			&& structureBlockAt(pos, centerX, centerZ, baseY, innerRadius, innerHeight) != StructureBlock.NONE;
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
		ActiveDomainState state = activeIntroStateForTarget(player.getUuid(), player.getEntityWorld().getRegistryKey(), currentTick);
		return state != null && state.freezePlayersDuringIntro && state.introFreezeByTarget.containsKey(player.getUuid());
	}

	static boolean isPlayerInvulnerableDuringIntro(ServerPlayerEntity player, int currentTick) {
		if (player == null) {
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
	}

	static void onServerStopping(MinecraftServer server) {
		ACTIVE_DOMAINS.clear();
		FROZEN_TRIBUTES.clear();
		DEBT_PENALTIES.clear();
		PERSISTENT_MARKS_BY_OWNER.clear();
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

	static boolean isArtifactItem(ItemStack stack) {
		return GreedItemCategoryHelper.isArtifact(stack);
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

	private static void updateFrozenTributeDisplays(MinecraftServer server, int currentTick) {
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

	private static void cleanupExpiredDebtPenalties(int currentTick) {
		Iterator<Map.Entry<UUID, ArrayList<DebtPenaltyState>>> iterator = DEBT_PENALTIES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ArrayList<DebtPenaltyState>> entry = iterator.next();
			entry.getValue().removeIf(state -> state.isExpired(currentTick));
			if (entry.getValue().isEmpty()) {
				iterator.remove();
			}
		}
	}

	private static void cleanupPersistentMarksForLowMana(MinecraftServer server) {
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

	private static void syncPenaltyAttributeModifiers(MinecraftServer server, int currentTick) {
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

	private static AggregatedPenaltyState aggregatePenaltyState(UUID targetId, int currentTick) {
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

	private static void addDebtPenalty(UUID targetId, DebtPenaltyState penaltyState) {
		if (penaltyState == null || !penaltyState.hasAnyTimedEffect()) {
			return;
		}
		DEBT_PENALTIES.computeIfAbsent(targetId, ignored -> new ArrayList<>()).add(penaltyState);
	}

	private static PersistentMarkState persistentMarkState(UUID ownerId, UUID targetId) {
		Map<UUID, PersistentMarkState> marks = PERSISTENT_MARKS_BY_OWNER.get(ownerId);
		return marks == null ? null : marks.get(targetId);
	}

	private static void removePersistentMarksOwnedBy(UUID ownerId) {
		PERSISTENT_MARKS_BY_OWNER.remove(ownerId);
	}

	private static void applyDebtCollection(MinecraftServer server, UUID casterId, UUID targetId, int frozenTributeUnits, int currentTick) {
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

	private static void applyLightDebt(ServerPlayerEntity target, MagicConfig.GreedDomainLightDebtConfig config, int currentTick) {
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

	private static boolean isIntroActive(ActiveDomainState state, int currentTick) {
		return state != null && currentTick < state.introEndTick;
	}

	private static ActiveDomainState activeIntroStateForTarget(
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

	private static void captureIntroFreezeTarget(ActiveDomainState state, ServerPlayerEntity player) {
		state.introFreezeByTarget.putIfAbsent(player.getUuid(), IntroFreezeState.capture(player));
	}

	private static void enforceIntroFreeze(ServerWorld world, ActiveDomainState state) {
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

	private static void enforceIntroFreeze(ServerPlayerEntity player, IntroFreezeState freezeState) {
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

	private static float angleDeltaDegrees(float current, float target) {
		return Math.abs(MathHelper.wrapDegrees(target - current));
	}

	private static void applyStandardDebt(ServerPlayerEntity target, MagicConfig.GreedDomainStandardDebtConfig config, int currentTick) {
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

	private static void applyHeavyDebt(ServerPlayerEntity caster, ServerPlayerEntity target, MagicConfig.GreedDomainHeavyDebtConfig config, int currentTick) {
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
		int convertedCoinUnits = Math.max(0, (int) Math.round(seizedItems.size() * GreedRuntime.coinUnitsFromConfiguredCoinsValue(config.coinConversion.coinsPerSeizedStack)));
		if (convertedCoinUnits > 0) {
			GreedRuntime.grantAnonymousCoinUnits(caster, convertedCoinUnits, currentTick, config.coinConversion.allowOverflowBeyondPouchCap);
		}
	}

	private static void applySevereCollection(ServerPlayerEntity caster, ServerPlayerEntity target, MagicConfig.GreedDomainSevereCollectionConfig config, int currentTick) {
		applyManaSicknessLevel(target, config.manaSicknessAmplifier, config.manaSicknessDurationTicks);
		DebtPenaltyState penaltyState = new DebtPenaltyState();
		penaltyState.castFailureChance = config.castFailureChance;
		penaltyState.castFailureEndTick = currentTick + config.castFailureDurationTicks;
		penaltyState.backfire = BackfireState.fromConfig(config.backfire);
		addDebtPenalty(target.getUuid(), penaltyState);
		transferItemsToCaster(caster, List.of(seizeSingleMajorItem(target, config.seizureItemCategorySelectionRules)));
	}

	private static void applyBankruptcyTier(ServerPlayerEntity caster, ServerPlayerEntity target, MagicConfig.GreedDomainBankruptcyTierConfig config, int currentTick) {
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

	private static void applyFinalCollection(ServerPlayerEntity caster, ServerPlayerEntity target, MagicConfig.GreedDomainFinalCollectionConfig config, int currentTick) {
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

	private static void applyTotalCollection(ServerPlayerEntity caster, ServerPlayerEntity target, MagicConfig.GreedDomainTotalCollectionConfig config, int currentTick) {
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
		int equipmentCoinUnits = Math.max(0, seizedEquipmentCount * GreedRuntime.coinUnitsFromConfiguredCoinsValue(config.equipmentCoinRewardPerSeizedItem));
		if (equipmentCoinUnits > 0) {
			GreedRuntime.grantAnonymousCoinUnits(caster, equipmentCoinUnits, currentTick, config.equipmentCoinRewardUsesTemporaryOverflowStack);
		}
		applyIndebtedMark(caster.getUuid(), target.getUuid(), config.indebted);
		applyCollectedMark(caster.getUuid(), target.getUuid(), config.collected);
	}

	private static void applyIndebtedMark(UUID ownerId, UUID targetId, MagicConfig.GreedDomainIndebtedConfig config) {
		if (!config.enabled) {
			return;
		}
		PersistentMarkState state = PERSISTENT_MARKS_BY_OWNER.computeIfAbsent(ownerId, ignored -> new HashMap<>()).computeIfAbsent(targetId, ignored -> new PersistentMarkState());
		state.indebted = true;
		state.lowManaCleanupThresholdPercent = config.casterLowManaCleanupThresholdPercent;
		state.uniqueTargetCapBypassEnabled = config.uniqueTargetCapBypassEnabled;
	}

	private static void applyCollectedMark(UUID ownerId, UUID targetId, MagicConfig.GreedDomainCollectedConfig config) {
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
			state.residualExtractionTriggerCoinUnits.put(entry.getKey(), GreedRuntime.coinUnitsFromConfiguredCoinsValue(entry.getValue()));
		}
	}

	private static int startingTributeUnits(UUID ownerId, UUID targetId) {
		int defaultUnits = MagicConfig.get().greed.domain.tribute.initialTribute * COIN_UNITS_PER_COIN;
		PersistentMarkState markState = persistentMarkState(ownerId, targetId);
		if (markState == null || !markState.collected || markState.futureReducedTributeUnits <= 0) {
			return defaultUnits;
		}
		return Math.min(defaultUnits, markState.futureReducedTributeUnits);
	}

	private static int adjustTributeLossUnits(UUID actorId, UUID ownerId, int configuredCoinUnits, int currentTick) {
		PersistentMarkState markState = persistentMarkState(ownerId, actorId);
		if (markState == null || !markState.collected) {
			return configuredCoinUnits;
		}
		return Math.max(0, (int) Math.round(configuredCoinUnits * markState.tributeLossMultiplier));
	}

	private static void applyManaSicknessRange(ServerPlayerEntity target, MagicConfig.GreedDomainIntRangeConfig levelRange, int durationTicks) {
		if (durationTicks <= 0) {
			return;
		}
		applyManaSicknessLevel(target, randomInt(levelRange.min, levelRange.max, target.getRandom()), durationTicks);
	}

	private static void applyManaSicknessLevel(ServerPlayerEntity target, int level, int durationTicks) {
		if (durationTicks <= 0 || level <= 0) {
			return;
		}
		target.addStatusEffect(new StatusEffectInstance(ModStatusEffects.manaSicknessEntry(), durationTicks, Math.max(0, level - 1), true, true, true));
	}

	private static void applyActiveDomainEffects(LivingEntity target) {
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

	private static int countArtifacts(ServerPlayerEntity target) {
		int count = 0;
		for (int slot = 0; slot < target.getInventory().size(); slot++) {
			ItemStack stack = target.getInventory().getStack(slot);
			if (!stack.isEmpty() && GreedItemCategoryHelper.isArtifact(stack)) {
				count++;
			}
		}
		return count;
	}

	private static int artifactBurdenAmplifier(MagicConfig.GreedDomainArtifactBurdenConfig config, int artifactCount) {
		int amplifier = -1;
		for (Map.Entry<String, Integer> entry : config.slownessByArtifactCount.entrySet()) {
			int requiredCount = Integer.parseInt(entry.getKey());
			if (artifactCount >= requiredCount) {
				amplifier = Math.max(amplifier, entry.getValue());
			}
		}
		return amplifier;
	}

	private static boolean matchesDurabilityTarget(MagicConfig.GreedDomainDurabilityTargetConfig config, ItemStack stack) {
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

	private static void applyDurabilityLoss(ServerPlayerEntity target, StackSelector selector, double lossFraction) {
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

	private static void applyRandomDurabilityLoss(ServerPlayerEntity target, StackSelector selector, MagicConfig.GreedDomainDoubleRangeConfig range) {
		forEachInventoryStack(target, slotRef -> {
			ItemStack stack = slotRef.stack(target);
			if (!stack.isEmpty() && selector.matches(stack)) {
				damageStack(stack, randomDouble(range.min, range.max, target.getRandom()));
			}
		});
		target.currentScreenHandler.sendContentUpdates();
	}

	private static void damageStack(ItemStack stack, double lossFraction) {
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

	private static ArrayList<ItemStack> seizeRandomInventoryItems(ServerPlayerEntity target, MagicConfig.GreedDomainSeizureFilterConfig filters, int itemCount) {
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

	private static ItemStack seizeSingleMajorItem(ServerPlayerEntity target, MagicConfig.GreedDomainMajorSeizureConfig config) {
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

	private static ArrayList<ItemStack> seizeArmorForFinalCollection(ServerPlayerEntity target, MagicConfig.GreedDomainFinalCollectionConfig config) {
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

	private static ItemStack seizePreferredWeapon(ServerPlayerEntity target, boolean preferMainHandWeapon, boolean allowInventoryWeaponFallback) {
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

	private static ArrayList<ItemStack> seizeHalfInventory(
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

	private static ArrayList<ItemStack> seizeArtifactsForTotalCollection(ServerPlayerEntity target, MagicConfig.GreedDomainTotalCollectionConfig config) {
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

	private static ItemStack seizeSingleArtifact(ServerPlayerEntity target) {
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

	private static ArrayList<ItemStack> seizeAllMatchingItems(ServerPlayerEntity target, StackSelector selector, boolean armorSlotsOnly) {
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

	private static ArrayList<InventorySlotRef> inventoryCandidates(ServerPlayerEntity target, MagicConfig.GreedDomainSeizureFilterConfig filters) {
		ArrayList<InventorySlotRef> candidates = new ArrayList<>();
		forEachInventoryStack(target, slotRef -> {
			if (matchesSeizureFilters(slotRef, target, filters)) {
				candidates.add(slotRef);
			}
		});
		return candidates;
	}

	private static boolean matchesSeizureFilters(InventorySlotRef slotRef, ServerPlayerEntity target, MagicConfig.GreedDomainSeizureFilterConfig filters) {
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

	private static ArrayList<InventorySlotRef> majorWeaponCandidates(ServerPlayerEntity target, boolean preferEquipped, boolean allowInventoryFallback) {
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

	private static ArrayList<InventorySlotRef> majorArmorCandidates(ServerPlayerEntity target, boolean preferEquipped, boolean allowInventoryFallback) {
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

	private static void transferItemsToCaster(ServerPlayerEntity caster, List<ItemStack> items) {
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

	private static void transferItemsBackToTarget(ServerPlayerEntity target, List<ItemStack> items) {
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

	private static void applyBackfire(ServerPlayerEntity player, BackfireState state) {
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

	private static void removeDisplaysForTarget(MinecraftServer server, UUID targetId) {
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

	private static void removeDisplaysOwnedBy(MinecraftServer server, UUID ownerId) {
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

	private static void clearDeathEndedDebtPenalties(UUID targetId) {
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

	private static void removePenaltyAttributeModifiers(ServerPlayerEntity player) {
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), GREED_ATTACK_DAMAGE_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.ARMOR), GREED_ARMOR_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.ARMOR_TOUGHNESS), GREED_ARMOR_TOUGHNESS_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), GREED_MOVEMENT_SPEED_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_SPEED), GREED_ATTACK_SPEED_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), GREED_MAX_HEALTH_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE), GREED_KNOCKBACK_RESISTANCE_MODIFIER_ID);
		removePenaltyAttributeModifier(player.getAttributeInstance(EntityAttributes.SCALE), GREED_SCALE_MODIFIER_ID);
	}

	private static void applyPenaltyAttributeModifier(EntityAttributeInstance attributeInstance, Identifier modifierId, double value) {
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

	private static void removePenaltyAttributeModifier(EntityAttributeInstance attributeInstance, Identifier modifierId) {
		if (attributeInstance != null) {
			attributeInstance.removeModifier(modifierId);
		}
	}

	private static int intervalCoinUnits(double coinsPerSecond, int intervalTicks) {
		return GreedRuntime.coinUnitsFromConfiguredCoinsValue(Math.max(0.0, coinsPerSecond) * intervalTicks / 20.0);
	}

	private static int countPassiveIncomeTargets(ServerWorld world, UUID casterId, double centerX, double centerZ, int baseY, int innerRadius, int innerHeight, boolean countMobs) {
		int count = 0;
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.getUuid().equals(casterId) || !player.isAlive() || player.isSpectator()) {
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

	private static List<LivingEntity> collectTributeTargetsInsideDomain(ServerWorld world, UUID casterId, double centerX, double centerZ, int baseY, int innerRadius, int innerHeight) {
		ArrayList<LivingEntity> targets = new ArrayList<>();
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.getUuid().equals(casterId) || !player.isAlive() || player.isSpectator()) {
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

	private static boolean isInsideDomainInterior(LivingEntity entity, double centerX, double centerZ, int baseY, int innerRadius, int innerHeight) {
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

	private static boolean isValidTributeTarget(LivingEntity living) {
		return !MagicConfig.get().greed.domain.tribute.applyOnlyToPlayers || living instanceof ServerPlayerEntity;
	}

	private static UUID syncTributeDisplay(ServerWorld world, LivingEntity target, UUID displayEntityId, int tributeUnits, String prefix) {
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
		displayEntity.setCustomName(tributeText(prefix, tributeUnits));
		displayEntity.setCustomNameVisible(true);
		displayEntity.refreshPositionAndAngles(target.getX(), target.getY() + DISPLAY_VERTICAL_OFFSET, target.getZ(), 0.0F, 0.0F);
		return displayEntity.getUuid();
	}

	private static UUID spawnTributeDisplay(ServerWorld world, LivingEntity target, int tributeUnits, String prefix) {
		ArmorStandEntity displayEntity = new ArmorStandEntity(world, target.getX(), target.getY() + DISPLAY_VERTICAL_OFFSET, target.getZ());
		displayEntity.setInvisible(true);
		displayEntity.setInvulnerable(true);
		displayEntity.setNoGravity(true);
		((ArmorStandEntityAccessorMixin) displayEntity).magic$setMarker(true);
		displayEntity.setSilent(true);
		displayEntity.setCustomName(tributeText(prefix, tributeUnits));
		displayEntity.setCustomNameVisible(true);
		if (!world.spawnEntity(displayEntity)) {
			return null;
		}
		return displayEntity.getUuid();
	}

	private static Text tributeText(String prefix, int tributeUnits) {
		return Text.literal(prefix + ": " + formatCoinUnits(tributeUnits)).formatted(Formatting.GOLD, Formatting.BOLD);
	}

	private static void removeDisplayEntity(MinecraftServer server, net.minecraft.registry.RegistryKey<World> dimension, UUID entityId) {
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

	private static void forEachInventoryStack(ServerPlayerEntity target, SlotConsumer consumer) {
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

	private static int currentTick(ServerPlayerEntity player) {
		MinecraftServer server = player.getEntityWorld().getServer();
		return server == null ? Integer.MIN_VALUE : server.getTicks();
	}

	private static int randomInt(int min, int max, net.minecraft.util.math.random.Random random) {
		return max <= min ? min : min + random.nextInt(max - min + 1);
	}

	private static double randomDouble(double min, double max, net.minecraft.util.math.random.Random random) {
		return max <= min ? min : min + (max - min) * random.nextDouble();
	}

	private static ParticleEffect resolveParticleEffect(String particleId, ParticleEffect fallback) {
		Identifier identifier = Identifier.tryParse(particleId);
		if (identifier == null || !Registries.PARTICLE_TYPE.containsId(identifier)) {
			return fallback;
		}
		Object particleType = Registries.PARTICLE_TYPE.get(identifier);
		return particleType instanceof ParticleEffect particleEffect ? particleEffect : fallback;
	}

	private static SoundEvent resolveSoundEvent(String soundId, SoundEvent fallback) {
		Identifier identifier = Identifier.tryParse(soundId);
		if (identifier == null || !Registries.SOUND_EVENT.containsId(identifier)) {
			return fallback;
		}
		return Registries.SOUND_EVENT.get(identifier);
	}

	private static String formatCoinUnits(int coinUnits) {
		int safeCoinUnits = Math.max(0, coinUnits);
		if (safeCoinUnits % COIN_UNITS_PER_COIN == 0) {
			return Integer.toString(safeCoinUnits / COIN_UNITS_PER_COIN);
		}
		int wholeCoins = safeCoinUnits / COIN_UNITS_PER_COIN;
		int fractionalUnits = safeCoinUnits % COIN_UNITS_PER_COIN;
		String fractional = fractionalUnits < 10 ? "0" + fractionalUnits : Integer.toString(fractionalUnits);
		while (fractional.endsWith("0")) {
			fractional = fractional.substring(0, fractional.length() - 1);
		}
		return wholeCoins + "." + fractional;
	}

	private static void ensureInitialized() {
		if (!initialized) {
			reloadConfigValues();
		}
	}

	private static WeightedBlockState weightedShellState(BlockPos pos) {
		int totalWeight = 0;
		for (WeightedBlockState entry : SHELL_PALETTE) {
			totalWeight += entry.weight;
		}
		if (totalWeight <= 0) {
			return new WeightedBlockState(Blocks.GILDED_BLACKSTONE.getDefaultState(), 1);
		}
		int selection = Math.floorMod(decorHash(pos), totalWeight);
		for (WeightedBlockState entry : SHELL_PALETTE) {
			selection -= entry.weight;
			if (selection < 0) {
				return entry;
			}
		}
		return SHELL_PALETTE.getFirst();
	}

	private static BlockState patternedPaletteState(BlockPos pos, List<BlockState> palette) {
		if (palette.isEmpty()) {
			return Blocks.GOLD_BLOCK.getDefaultState();
		}
		return palette.get(Math.floorMod(decorHash(pos), palette.size()));
	}

	private static StructureBlock structureBlockAt(
		BlockPos pos,
		int centerX,
		int centerZ,
		int baseY,
		int innerRadius,
		int innerHeight
	) {
		int relativeY = pos.getY() - baseY;
		if (relativeY < 0 || relativeY >= innerHeight) {
			return StructureBlock.NONE;
		}
		if (isCarpetBlock(pos, centerX, centerZ, baseY, innerRadius)) {
			return StructureBlock.CARPET;
		}
		if (isThroneBlock(pos, centerX, centerZ, baseY, innerRadius, innerHeight)) {
			return StructureBlock.THRONE;
		}
		if (isPillarBlock(pos, centerX, centerZ, baseY, innerRadius, innerHeight)) {
			return StructureBlock.PILLAR;
		}
		return StructureBlock.NONE;
	}

	private static boolean isCarpetBlock(BlockPos pos, int centerX, int centerZ, int baseY, int innerRadius) {
		int relativeY = pos.getY() - baseY;
		if (relativeY != 1) {
			return false;
		}
		int halfWidth = Math.max(0, (CARPET_WIDTH - 1) / 2);
		int forward = forwardAxisValue(pos, THRONE_SIDE);
		int centerForward = forwardAxisValue(centerX, centerZ, THRONE_SIDE);
		int lateral = lateralAxisValue(pos, THRONE_SIDE);
		int centerLateral = lateralAxisValue(centerX, centerZ, THRONE_SIDE);
		int direction = throneForwardDirection();
		int carpetStart = centerForward - direction * CARPET_EXTENSION_PAST_CENTER;
		int carpetEnd = throneFrontAxis(centerForward) - direction * 2;
		int pathStart = Math.min(carpetStart, carpetEnd);
		int pathEnd = Math.max(carpetStart, carpetEnd);
		return Math.abs(lateral - centerLateral) <= halfWidth && forward >= pathStart && forward <= pathEnd;
	}

	private static boolean isThroneBlock(BlockPos pos, int centerX, int centerZ, int baseY, int innerRadius, int innerHeight) {
		int relativeY = pos.getY() - baseY;
		if (relativeY <= 0 || relativeY > THRONE_HEIGHT || relativeY >= innerHeight) {
			return false;
		}
		int forward = forwardAxisValue(pos, THRONE_SIDE);
		int centerForward = forwardAxisValue(centerX, centerZ, THRONE_SIDE);
		int lateral = lateralAxisValue(pos, THRONE_SIDE);
		int centerLateral = lateralAxisValue(centerX, centerZ, THRONE_SIDE);
		int direction = throneForwardDirection();
		int seatFront = throneFrontAxis(centerForward);
		int seatBack = seatFront + direction * (THRONE_DEPTH - 1);
		int halfWidth = Math.max(1, THRONE_WIDTH / 2);
		int absLateral = Math.abs(lateral - centerLateral);
		int outerHalfWidth = halfWidth + 1;
		int baseFront = seatFront - direction * 2;
		int baseBack = seatBack + direction;
		int seatBodyBack = seatFront + direction * Math.max(1, THRONE_DEPTH - 3);
		int backEdge = direction > 0 ? Math.max(seatFront, seatBack) : Math.min(seatFront, seatBack);
		int innerBackEdge = backEdge - direction;
		int backPanelFront = backEdge - direction * Math.max(1, Math.min(2, THRONE_DEPTH - 1));
		int seatTopEnd = Math.max(3, Math.min(4, THRONE_HEIGHT - 3));
		int armrestTop = Math.max(3, Math.min(5, THRONE_HEIGHT - 2));
		if (relativeY == 1) {
			return withinThroneRect(forward, lateral, centerLateral, baseFront, baseBack, outerHalfWidth + 1);
		}
		if (relativeY == 2) {
			return withinThroneRect(forward, lateral, centerLateral, seatFront - direction, seatBack, outerHalfWidth)
				|| withinThroneRect(forward, lateral, centerLateral, baseFront, seatFront - direction, Math.max(1, halfWidth - 1));
		}
		boolean seatBody = relativeY <= seatTopEnd
			&& withinThroneRect(forward, lateral, centerLateral, seatFront, seatBodyBack, Math.max(1, halfWidth - 1));
		boolean seatFrontTrim = relativeY == 3
			&& withinThroneRect(forward, lateral, centerLateral, seatFront - direction, seatFront - direction, Math.max(1, halfWidth - 2));
		boolean armrests = relativeY <= armrestTop
			&& withinThroneRect(forward, lateral, centerLateral, seatFront - direction, seatBack, outerHalfWidth)
			&& absLateral >= halfWidth;
		boolean backrest = relativeY >= 4
			&& relativeY <= Math.max(4, THRONE_HEIGHT - 1)
			&& withinThroneRect(forward, lateral, centerLateral, backPanelFront, backEdge, Math.max(1, halfWidth - 1));
		boolean sideStandards = relativeY >= 4
			&& withinThroneRect(forward, lateral, centerLateral, innerBackEdge, backEdge, outerHalfWidth)
			&& absLateral >= halfWidth;
		boolean crownShelf = relativeY == Math.max(4, THRONE_HEIGHT - 1)
			&& withinThroneRect(forward, lateral, centerLateral, backPanelFront, backEdge, outerHalfWidth);
		boolean crownTop = relativeY == THRONE_HEIGHT
			&& (
				(withinForwardSpan(forward, backEdge, backEdge) && (absLateral == 0 || absLateral == halfWidth || absLateral == outerHalfWidth))
					|| (withinForwardSpan(forward, innerBackEdge, innerBackEdge) && absLateral <= Math.max(1, halfWidth - 2))
			);
		return seatBody || seatFrontTrim || armrests || backrest || sideStandards || crownShelf || crownTop;
	}

	private static boolean isPillarBlock(BlockPos pos, int centerX, int centerZ, int baseY, int innerRadius, int innerHeight) {
		int relativeY = pos.getY() - baseY;
		if (relativeY <= 0 || relativeY > PILLAR_HEIGHT || relativeY >= innerHeight) {
			return false;
		}
		if (PILLAR_COUNT <= 0) {
			return false;
		}
		for (BlockPos pillarBase : pillarBasePositions(centerX, centerZ, innerRadius)) {
			if (isWithinPillarFootprint(pos, pillarBase, baseY)) {
				return true;
			}
		}
		return false;
	}

	private static List<BlockPos> pillarBasePositions(int centerX, int centerZ, int innerRadius) {
		ArrayList<BlockPos> positions = new ArrayList<>();
		HashSet<Long> seen = new HashSet<>();
		double maxRadius = Math.max(1.0, innerRadius - 2.0);
		double placementRadius = Math.min(PILLAR_RADIUS, maxRadius);
		double spacing = PILLAR_COUNT <= 0 ? 360.0 : Math.min(360.0, PILLAR_SPACING_DEGREES);
		double baseAngle = spacing * 0.5;
		for (int index = 0; index < PILLAR_COUNT; index++) {
			double angleRadians = Math.toRadians(baseAngle + spacing * index);
			int x = MathHelper.floor(centerX + Math.cos(angleRadians) * placementRadius + 0.5);
			int z = MathHelper.floor(centerZ + Math.sin(angleRadians) * placementRadius + 0.5);
			BlockPos pos = new BlockPos(x, 0, z);
			if (!seen.add(pos.asLong())) {
				continue;
			}
			if (pillarFootprintIntersectsReservedSpace(pos, centerX, centerZ, innerRadius, DOMAIN_HEIGHT - DOMAIN_SHELL_THICKNESS)) {
				continue;
			}
			positions.add(pos);
		}
		return positions;
	}

	private static boolean isWithinPillarFootprint(BlockPos pos, BlockPos pillarBase, int baseY) {
		int relativeY = pos.getY() - baseY;
		if (relativeY <= 0 || relativeY > PILLAR_HEIGHT) {
			return false;
		}
		int shaftHalfWidth = Math.max(0, PILLAR_WIDTH / 2);
		int outerHalfWidth = shaftHalfWidth + 1;
		int dx = Math.abs(pos.getX() - pillarBase.getX());
		int dz = Math.abs(pos.getZ() - pillarBase.getZ());
		boolean shaft = dx <= shaftHalfWidth && dz <= shaftHalfWidth;
		boolean band = (dx <= outerHalfWidth && dz <= shaftHalfWidth) || (dx <= shaftHalfWidth && dz <= outerHalfWidth);
		if (relativeY == 1 || relativeY >= Math.max(2, PILLAR_HEIGHT - 1)) {
			return dx <= outerHalfWidth && dz <= outerHalfWidth;
		}
		if (relativeY == 2 || relativeY == Math.max(2, PILLAR_HEIGHT - 2)) {
			return band;
		}
		return shaft;
	}

	private static boolean pillarFootprintIntersectsReservedSpace(BlockPos pillarBase, int centerX, int centerZ, int innerRadius, int innerHeight) {
		int outerHalfWidth = Math.max(1, PILLAR_WIDTH / 2 + 1);
		for (int x = pillarBase.getX() - outerHalfWidth; x <= pillarBase.getX() + outerHalfWidth; x++) {
			for (int z = pillarBase.getZ() - outerHalfWidth; z <= pillarBase.getZ() + outerHalfWidth; z++) {
				if (isCarpetBlock(new BlockPos(x, 1, z), centerX, centerZ, 0, innerRadius)) {
					return true;
				}
				for (int y = 1; y <= THRONE_HEIGHT; y++) {
					if (isThroneBlock(new BlockPos(x, y, z), centerX, centerZ, 0, innerRadius, innerHeight)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static int throneFrontAxis(int centerForward) {
		return centerForward + throneForwardDirection() * THRONE_OFFSET;
	}

	private static int throneForwardDirection() {
		return switch (THRONE_SIDE) {
			case "south", "east" -> 1;
			case "north", "west" -> -1;
			default -> 1;
		};
	}

	private static int forwardAxisValue(BlockPos pos, String side) {
		return side.equals("east") || side.equals("west") ? pos.getX() : pos.getZ();
	}

	private static int forwardAxisValue(int x, int z, String side) {
		return side.equals("east") || side.equals("west") ? x : z;
	}

	private static int lateralAxisValue(BlockPos pos, String side) {
		return side.equals("east") || side.equals("west") ? pos.getZ() : pos.getX();
	}

	private static int lateralAxisValue(int x, int z, String side) {
		return side.equals("east") || side.equals("west") ? z : x;
	}

	private static List<WeightedBlockState> parseWeightedPalette(List<MagicConfig.GreedWeightedBlockEntry> entries) {
		ArrayList<WeightedBlockState> parsed = new ArrayList<>();
		if (entries != null) {
			for (MagicConfig.GreedWeightedBlockEntry entry : entries) {
				if (entry == null || entry.weight <= 0) {
					continue;
				}
				parsed.add(new WeightedBlockState(parseBlockState(entry.blockId, Blocks.GILDED_BLACKSTONE.getDefaultState()), entry.weight));
			}
		}
		if (parsed.isEmpty()) {
			parsed.add(new WeightedBlockState(Blocks.GILDED_BLACKSTONE.getDefaultState(), 6));
			parsed.add(new WeightedBlockState(Blocks.BLACKSTONE.getDefaultState(), 5));
			parsed.add(new WeightedBlockState(Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 5));
			parsed.add(new WeightedBlockState(Blocks.GOLD_BLOCK.getDefaultState(), 2));
		}
		return parsed;
	}

	private static List<BlockState> parseBlockStateList(List<String> blockIds, List<BlockState> fallback) {
		ArrayList<BlockState> parsed = new ArrayList<>();
		if (blockIds != null) {
			for (String blockId : blockIds) {
				parsed.add(parseBlockState(blockId, null));
			}
			parsed.removeIf(state -> state == null);
		}
		if (!parsed.isEmpty()) {
			return parsed;
		}
		return new ArrayList<>(fallback);
	}

	private static boolean withinThroneRect(int forward, int lateral, int centerLateral, int fromForward, int toForward, int halfWidth) {
		return withinForwardSpan(forward, fromForward, toForward) && Math.abs(lateral - centerLateral) <= Math.max(0, halfWidth);
	}

	private static boolean withinForwardSpan(int forward, int fromForward, int toForward) {
		int minForward = Math.min(fromForward, toForward);
		int maxForward = Math.max(fromForward, toForward);
		return forward >= minForward && forward <= maxForward;
	}

	private static BlockState parseBlockState(String blockId, BlockState fallback) {
		Identifier identifier = Identifier.tryParse(blockId);
		if (identifier == null) {
			return fallback;
		}
		if (!Registries.BLOCK.containsId(identifier)) {
			Magic.LOGGER.warn("Greed domain config block id '{}' was not found. Using fallback.", blockId);
			return fallback;
		}
		return Registries.BLOCK.get(identifier).getDefaultState();
	}

	private static int parseHexColor(String rawColor, int fallbackColor) {
		if (rawColor == null || rawColor.isBlank()) {
			return fallbackColor;
		}
		String normalized = rawColor.trim();
		if (normalized.startsWith("#")) {
			normalized = normalized.substring(1);
		}
		if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
			normalized = normalized.substring(2);
		}
		if (normalized.length() != 6) {
			return fallbackColor;
		}
		try {
			return Integer.parseInt(normalized, 16);
		} catch (NumberFormatException exception) {
			return fallbackColor;
		}
	}

	private static int decorHash(BlockPos pos) {
		return pos.getX() * 73428767 ^ pos.getY() * 912367 ^ pos.getZ() * 42317861;
	}

	private enum InventorySlotType {
		HOTBAR,
		MAIN,
		ARMOR,
		OFFHAND
	}

	private interface SlotConsumer {
		void accept(InventorySlotRef slotRef);
	}

	private interface StackSelector {
		boolean matches(ItemStack stack);
	}

	private static final class ActiveDomainState {
		private net.minecraft.registry.RegistryKey<World> dimension;
		private double centerX;
		private double centerZ;
		private int baseY;
		private int innerRadius;
		private int innerHeight;
		private int nextPassiveIncomeTick;
		private int introEndTick;
		private boolean pauseDomainTimerDuringIntro;
		private boolean freezePlayersDuringIntro;
		private boolean preventDamageDuringIntro;
		private final Map<UUID, TributeState> tributeByTarget = new HashMap<>();
		private final Map<UUID, IntroFreezeState> introFreezeByTarget = new HashMap<>();

		private ActiveDomainState(net.minecraft.registry.RegistryKey<World> dimension, double centerX, double centerZ, int baseY, int innerRadius, int innerHeight) {
			this.dimension = dimension;
			this.centerX = centerX;
			this.centerZ = centerZ;
			this.baseY = baseY;
			this.innerRadius = innerRadius;
			this.innerHeight = innerHeight;
		}
	}

	private static final class IntroFreezeState {
		private final double x;
		private final double y;
		private final double z;
		private final float yaw;
		private final float pitch;

		private IntroFreezeState(double x, double y, double z, float yaw, float pitch) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.pitch = pitch;
		}

		private static IntroFreezeState capture(ServerPlayerEntity player) {
			return new IntroFreezeState(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
		}
	}

	private static final class TributeState {
		private int tributeUnits;
		private UUID displayEntityId;

		private TributeState(int tributeUnits, UUID displayEntityId) {
			this.tributeUnits = tributeUnits;
			this.displayEntityId = displayEntityId;
		}
	}

	private static final class FrozenTributeState {
		private final net.minecraft.registry.RegistryKey<World> dimension;
		private final UUID targetId;
		private final int tributeUnits;
		private UUID displayEntityId;
		private final int expiresTick;
		private final String displayPrefix;

		private FrozenTributeState(
			net.minecraft.registry.RegistryKey<World> dimension,
			UUID targetId,
			int tributeUnits,
			UUID displayEntityId,
			int expiresTick,
			String displayPrefix
		) {
			this.dimension = dimension;
			this.targetId = targetId;
			this.tributeUnits = tributeUnits;
			this.displayEntityId = displayEntityId;
			this.expiresTick = expiresTick;
			this.displayPrefix = displayPrefix;
		}
	}

	private static final class DebtPenaltyState {
		private double manaRegenMultiplier = 1.0;
		private int manaRegenEndTick;
		private double manaCostMultiplier = 1.0;
		private int manaCostEndTick;
		private double castFailureChance;
		private int castFailureEndTick;
		private BackfireState backfire;
		private double cooldownMultiplier = 1.0;
		private int cooldownEndTick;
		private int magicSealEndTick;
		private double outgoingDamageMultiplier = 1.0;
		private double armorEffectivenessMultiplier = 1.0;
		private double movementSpeedMultiplier = 1.0;
		private double attackSpeedMultiplier = 1.0;
		private double maxHealthMultiplier = 1.0;
		private double knockbackResistanceMultiplier = 1.0;
		private double scaleMultiplier = 1.0;
		private int statPenaltyEndTick;

		private void clearStatPenalty() {
			outgoingDamageMultiplier = 1.0;
			armorEffectivenessMultiplier = 1.0;
			movementSpeedMultiplier = 1.0;
			attackSpeedMultiplier = 1.0;
			maxHealthMultiplier = 1.0;
			knockbackResistanceMultiplier = 1.0;
			scaleMultiplier = 1.0;
			statPenaltyEndTick = 0;
		}

		private boolean hasAnyTimedEffect() {
			return manaRegenEndTick > 0
				|| manaCostEndTick > 0
				|| castFailureEndTick > 0
				|| cooldownEndTick > 0
				|| magicSealEndTick > 0
				|| statPenaltyEndTick > 0;
		}

		private boolean isExpired(int currentTick) {
			return currentTick >= manaRegenEndTick
				&& currentTick >= manaCostEndTick
				&& currentTick >= castFailureEndTick
				&& currentTick >= cooldownEndTick
				&& currentTick >= magicSealEndTick
				&& currentTick >= statPenaltyEndTick;
		}
	}

	private static final class BackfireState {
		private final double chance;
		private final float damage;
		private final double horizontalLaunch;
		private final double verticalLaunch;
		private final String primaryParticleId;
		private final String secondaryParticleId;
		private final int primaryParticleCount;
		private final int secondaryParticleCount;
		private final String soundId;
		private final float soundVolume;
		private final float soundPitch;

		private BackfireState(
			double chance,
			float damage,
			double horizontalLaunch,
			double verticalLaunch,
			String primaryParticleId,
			String secondaryParticleId,
			int primaryParticleCount,
			int secondaryParticleCount,
			String soundId,
			float soundVolume,
			float soundPitch
		) {
			this.chance = chance;
			this.damage = damage;
			this.horizontalLaunch = horizontalLaunch;
			this.verticalLaunch = verticalLaunch;
			this.primaryParticleId = primaryParticleId;
			this.secondaryParticleId = secondaryParticleId;
			this.primaryParticleCount = primaryParticleCount;
			this.secondaryParticleCount = secondaryParticleCount;
			this.soundId = soundId;
			this.soundVolume = soundVolume;
			this.soundPitch = soundPitch;
		}

		private static BackfireState fromConfig(MagicConfig.GreedDomainBackfireConfig config) {
			return new BackfireState(
				config.chance,
				config.damage,
				config.horizontalLaunch,
				config.verticalLaunch,
				config.primaryParticleId,
				config.secondaryParticleId,
				config.primaryParticleCount,
				config.secondaryParticleCount,
				config.soundId,
				config.soundVolume,
				config.soundPitch
			);
		}
	}

	private static final class PersistentMarkState {
		private boolean indebted;
		private boolean collected;
		private double lowManaCleanupThresholdPercent = 20.0;
		private boolean uniqueTargetCapBypassEnabled = true;
		private int futureReducedTributeUnits = 12 * COIN_UNITS_PER_COIN;
		private double coinExtractionMultiplier = 1.25;
		private double tributeLossMultiplier = 1.5;
		private boolean residualExtractionEnabled = true;
		private Map<String, Integer> residualExtractionTriggerCoinUnits = Map.of();
	}

	private record AggregatedPenaltyState(
		double manaRegenMultiplier,
		double manaCostMultiplier,
		double cooldownMultiplier,
		double outgoingDamageMultiplier,
		double armorEffectivenessMultiplier,
		double movementSpeedMultiplier,
		double attackSpeedMultiplier,
		double maxHealthMultiplier,
		double knockbackResistanceMultiplier,
		double scaleMultiplier,
		double castFailureChance,
		boolean sealed,
		BackfireState backfire
	) {
		private static final AggregatedPenaltyState NONE = new AggregatedPenaltyState(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, false, null);
	}

	private record OwnerTargetKey(UUID ownerId, UUID targetId) {
	}

	private static final class InventorySlotRef {
		private final int slot;
		private final InventorySlotType type;

		private InventorySlotRef(int slot, InventorySlotType type) {
			this.slot = slot;
			this.type = type;
		}

		private ItemStack stack(ServerPlayerEntity player) {
			return player.getInventory().getStack(slot);
		}

		private ItemStack remove(ServerPlayerEntity player) {
			ItemStack existing = player.getInventory().getStack(slot);
			if (existing.isEmpty()) {
				return ItemStack.EMPTY;
			}
			ItemStack removed = existing.copy();
			player.getInventory().setStack(slot, ItemStack.EMPTY);
			return removed;
		}
	}

	private enum StructureBlock {
		NONE,
		CARPET,
		THRONE,
		PILLAR
	}

	private record WeightedBlockState(BlockState state, int weight) {
	}
}
