package net.evan.magic.magic.ability;
import net.evan.magic.magic.core.ability.MagicAbility;

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

abstract class GreedDomainState {
	static final int COIN_UNITS_PER_COIN = MagicPlayerData.GREED_COIN_UNITS_PER_COIN;
	static final int DISPLAY_REFRESH_TICKS = 40;
	static final double DISPLAY_VERTICAL_OFFSET = 2.85;
	static final double INTRO_FREEZE_POSITION_EPSILON_SQUARED = 1.0E-4;
	static final float INTRO_FREEZE_ROTATION_EPSILON_DEGREES = 0.1F;
	static final int HOTBAR_SLOT_COUNT = 9;
	static final int MAIN_INVENTORY_SLOT_END = 36;
	static final int ARMOR_SLOT_START = 36;
	static final int ARMOR_SLOT_END = 40;
	static final int OFFHAND_SLOT = 40;
	static final Identifier GREED_ATTACK_DAMAGE_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_attack_damage_penalty");
	static final Identifier GREED_ARMOR_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_armor_penalty");
	static final Identifier GREED_ARMOR_TOUGHNESS_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_armor_toughness_penalty");
	static final Identifier GREED_MOVEMENT_SPEED_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_movement_speed_penalty");
	static final Identifier GREED_ATTACK_SPEED_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_attack_speed_penalty");
	static final Identifier GREED_MAX_HEALTH_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_max_health_penalty");
	static final Identifier GREED_KNOCKBACK_RESISTANCE_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_knockback_resistance_penalty");
	static final Identifier GREED_SCALE_MODIFIER_ID = Identifier.of(Magic.MOD_ID, "greed_domain_scale_penalty");
	static final String TRIBUTE_DISPLAY_TAG = "magic_greed_tribute_display";
	static int DOMAIN_RADIUS = 25;
	static int DOMAIN_HEIGHT = 25;
	static int DOMAIN_SHELL_THICKNESS = 1;
	static int DOMAIN_DURATION_TICKS = 90 * 20;
	static int DOMAIN_COOLDOWN_TICKS = 36000;
	static BlockState CARPET_BLOCK_STATE = Blocks.RED_CARPET.getDefaultState();
	static List<WeightedBlockState> SHELL_PALETTE = List.of(
		new WeightedBlockState(Blocks.GILDED_BLACKSTONE.getDefaultState(), 6),
		new WeightedBlockState(Blocks.BLACKSTONE.getDefaultState(), 5),
		new WeightedBlockState(Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 5),
		new WeightedBlockState(Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState(), 2),
		new WeightedBlockState(Blocks.GOLD_BLOCK.getDefaultState(), 2)
	);
	static List<BlockState> THRONE_BLOCK_STATES = List.of(Blocks.GOLD_BLOCK.getDefaultState());
	static List<BlockState> PILLAR_BLOCK_STATES = List.of(Blocks.GOLD_BLOCK.getDefaultState());
	static int PILLAR_COUNT = 8;
	static double PILLAR_RADIUS = 9.0;
	static double PILLAR_SPACING_DEGREES = 45.0;
	static int PILLAR_HEIGHT = 23;
	static int PILLAR_WIDTH = 3;
	static int CARPET_WIDTH = 3;
	static int CARPET_EXTENSION_PAST_CENTER = 10;
	static String THRONE_SIDE = "south";
	static int THRONE_OFFSET = 12;
	static int THRONE_WIDTH = 7;
	static int THRONE_DEPTH = 5;
	static int THRONE_HEIGHT = 7;
	static boolean PROTECT_INTERIOR_STRUCTURES = true;
	static final Map<UUID, ActiveDomainState> ACTIVE_DOMAINS = new HashMap<>();
	static final Map<OwnerTargetKey, FrozenTributeState> FROZEN_TRIBUTES = new HashMap<>();
	static final Map<UUID, ArrayList<DebtPenaltyState>> DEBT_PENALTIES = new HashMap<>();
	static final Map<UUID, Map<UUID, PersistentMarkState>> PERSISTENT_MARKS_BY_OWNER = new HashMap<>();
	static boolean initialized;

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

	static int currentTick(ServerPlayerEntity player) {
		MinecraftServer server = player.getEntityWorld().getServer();
		return server == null ? Integer.MIN_VALUE : server.getTicks();
	}

	static int randomInt(int min, int max, net.minecraft.util.math.random.Random random) {
		return max <= min ? min : min + random.nextInt(max - min + 1);
	}

	static double randomDouble(double min, double max, net.minecraft.util.math.random.Random random) {
		return max <= min ? min : min + (max - min) * random.nextDouble();
	}

	static ParticleEffect resolveParticleEffect(String particleId, ParticleEffect fallback) {
		Identifier identifier = Identifier.tryParse(particleId);
		if (identifier == null || !Registries.PARTICLE_TYPE.containsId(identifier)) {
			return fallback;
		}
		Object particleType = Registries.PARTICLE_TYPE.get(identifier);
		return particleType instanceof ParticleEffect particleEffect ? particleEffect : fallback;
	}

	static SoundEvent resolveSoundEvent(String soundId, SoundEvent fallback) {
		Identifier identifier = Identifier.tryParse(soundId);
		if (identifier == null || !Registries.SOUND_EVENT.containsId(identifier)) {
			return fallback;
		}
		return Registries.SOUND_EVENT.get(identifier);
	}

	static String formatCoinUnits(int coinUnits) {
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

	static void ensureInitialized() {
		if (!initialized) {
			reloadConfigValues();
		}
	}

	static WeightedBlockState weightedShellState(BlockPos pos) {
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

	static BlockState patternedPaletteState(BlockPos pos, List<BlockState> palette) {
		if (palette.isEmpty()) {
			return Blocks.GOLD_BLOCK.getDefaultState();
		}
		return palette.get(Math.floorMod(decorHash(pos), palette.size()));
	}

	static StructureBlock structureBlockAt(
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

	static boolean isCarpetBlock(BlockPos pos, int centerX, int centerZ, int baseY, int innerRadius) {
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

	static boolean isThroneBlock(BlockPos pos, int centerX, int centerZ, int baseY, int innerRadius, int innerHeight) {
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

	static boolean isPillarBlock(BlockPos pos, int centerX, int centerZ, int baseY, int innerRadius, int innerHeight) {
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

	static List<BlockPos> pillarBasePositions(int centerX, int centerZ, int innerRadius) {
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

	static boolean isWithinPillarFootprint(BlockPos pos, BlockPos pillarBase, int baseY) {
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

	static boolean pillarFootprintIntersectsReservedSpace(BlockPos pillarBase, int centerX, int centerZ, int innerRadius, int innerHeight) {
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

	static int throneFrontAxis(int centerForward) {
		return centerForward + throneForwardDirection() * THRONE_OFFSET;
	}

	static int throneForwardDirection() {
		return switch (THRONE_SIDE) {
			case "south", "east" -> 1;
			case "north", "west" -> -1;
			default -> 1;
		};
	}

	static int forwardAxisValue(BlockPos pos, String side) {
		return side.equals("east") || side.equals("west") ? pos.getX() : pos.getZ();
	}

	static int forwardAxisValue(int x, int z, String side) {
		return side.equals("east") || side.equals("west") ? x : z;
	}

	static int lateralAxisValue(BlockPos pos, String side) {
		return side.equals("east") || side.equals("west") ? pos.getZ() : pos.getX();
	}

	static int lateralAxisValue(int x, int z, String side) {
		return side.equals("east") || side.equals("west") ? z : x;
	}

	static List<WeightedBlockState> parseWeightedPalette(List<MagicConfig.GreedWeightedBlockEntry> entries) {
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

	static List<BlockState> parseBlockStateList(List<String> blockIds, List<BlockState> fallback) {
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

	static boolean withinThroneRect(int forward, int lateral, int centerLateral, int fromForward, int toForward, int halfWidth) {
		return withinForwardSpan(forward, fromForward, toForward) && Math.abs(lateral - centerLateral) <= Math.max(0, halfWidth);
	}

	static boolean withinForwardSpan(int forward, int fromForward, int toForward) {
		int minForward = Math.min(fromForward, toForward);
		int maxForward = Math.max(fromForward, toForward);
		return forward >= minForward && forward <= maxForward;
	}

	static BlockState parseBlockState(String blockId, BlockState fallback) {
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

	static int parseHexColor(String rawColor, int fallbackColor) {
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

	static int decorHash(BlockPos pos) {
		return pos.getX() * 73428767 ^ pos.getY() * 912367 ^ pos.getZ() * 42317861;
	}

	enum InventorySlotType {
		HOTBAR,
		MAIN,
		ARMOR,
		OFFHAND
	}

	interface SlotConsumer {
		void accept(InventorySlotRef slotRef);
	}

	interface StackSelector {
		boolean matches(ItemStack stack);
	}

	static final class ActiveDomainState {
		net.minecraft.registry.RegistryKey<World> dimension;
		double centerX;
		double centerZ;
		int baseY;
		int innerRadius;
		int innerHeight;
		int nextPassiveIncomeTick;
		int introEndTick;
		boolean pauseDomainTimerDuringIntro;
		boolean freezePlayersDuringIntro;
		boolean preventDamageDuringIntro;
		final Map<UUID, TributeState> tributeByTarget = new HashMap<>();
		final Map<UUID, IntroFreezeState> introFreezeByTarget = new HashMap<>();

		ActiveDomainState(net.minecraft.registry.RegistryKey<World> dimension, double centerX, double centerZ, int baseY, int innerRadius, int innerHeight) {
			this.dimension = dimension;
			this.centerX = centerX;
			this.centerZ = centerZ;
			this.baseY = baseY;
			this.innerRadius = innerRadius;
			this.innerHeight = innerHeight;
		}
	}

	static final class IntroFreezeState {
		final double x;
		final double y;
		final double z;
		final float yaw;
		final float pitch;

		IntroFreezeState(double x, double y, double z, float yaw, float pitch) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.pitch = pitch;
		}

		static IntroFreezeState capture(ServerPlayerEntity player) {
			return new IntroFreezeState(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
		}
	}

	static final class TributeState {
		int tributeUnits;
		UUID displayEntityId;

		TributeState(int tributeUnits, UUID displayEntityId) {
			this.tributeUnits = tributeUnits;
			this.displayEntityId = displayEntityId;
		}
	}

	static final class FrozenTributeState {
		final net.minecraft.registry.RegistryKey<World> dimension;
		final UUID targetId;
		final int tributeUnits;
		UUID displayEntityId;
		final int expiresTick;
		final String displayPrefix;

		FrozenTributeState(
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

	static final class DebtPenaltyState {
		double manaRegenMultiplier = 1.0;
		int manaRegenEndTick;
		double manaCostMultiplier = 1.0;
		int manaCostEndTick;
		double castFailureChance;
		int castFailureEndTick;
		BackfireState backfire;
		double cooldownMultiplier = 1.0;
		int cooldownEndTick;
		int magicSealEndTick;
		double outgoingDamageMultiplier = 1.0;
		double armorEffectivenessMultiplier = 1.0;
		double movementSpeedMultiplier = 1.0;
		double attackSpeedMultiplier = 1.0;
		double maxHealthMultiplier = 1.0;
		double knockbackResistanceMultiplier = 1.0;
		double scaleMultiplier = 1.0;
		int statPenaltyEndTick;

		void clearStatPenalty() {
			outgoingDamageMultiplier = 1.0;
			armorEffectivenessMultiplier = 1.0;
			movementSpeedMultiplier = 1.0;
			attackSpeedMultiplier = 1.0;
			maxHealthMultiplier = 1.0;
			knockbackResistanceMultiplier = 1.0;
			scaleMultiplier = 1.0;
			statPenaltyEndTick = 0;
		}

		boolean hasAnyTimedEffect() {
			return manaRegenEndTick > 0
				|| manaCostEndTick > 0
				|| castFailureEndTick > 0
				|| cooldownEndTick > 0
				|| magicSealEndTick > 0
				|| statPenaltyEndTick > 0;
		}

		boolean isExpired(int currentTick) {
			return currentTick >= manaRegenEndTick
				&& currentTick >= manaCostEndTick
				&& currentTick >= castFailureEndTick
				&& currentTick >= cooldownEndTick
				&& currentTick >= magicSealEndTick
				&& currentTick >= statPenaltyEndTick;
		}
	}

	static final class BackfireState {
		final double chance;
		final float damage;
		final double horizontalLaunch;
		final double verticalLaunch;
		final String primaryParticleId;
		final String secondaryParticleId;
		final int primaryParticleCount;
		final int secondaryParticleCount;
		final String soundId;
		final float soundVolume;
		final float soundPitch;

		BackfireState(
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

		static BackfireState fromConfig(MagicConfig.GreedDomainBackfireConfig config) {
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

	static final class PersistentMarkState {
		boolean indebted;
		boolean collected;
		double lowManaCleanupThresholdPercent = 20.0;
		boolean uniqueTargetCapBypassEnabled = true;
		int futureReducedTributeUnits = 12 * COIN_UNITS_PER_COIN;
		double coinExtractionMultiplier = 1.25;
		double tributeLossMultiplier = 1.5;
		boolean residualExtractionEnabled = true;
		Map<String, Integer> residualExtractionTriggerCoinUnits = Map.of();
	}

	static final class AggregatedPenaltyState {
		static final AggregatedPenaltyState NONE = new AggregatedPenaltyState(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, false, null);

		final double manaRegenMultiplier;
		final double manaCostMultiplier;
		final double cooldownMultiplier;
		final double outgoingDamageMultiplier;
		final double armorEffectivenessMultiplier;
		final double movementSpeedMultiplier;
		final double attackSpeedMultiplier;
		final double maxHealthMultiplier;
		final double knockbackResistanceMultiplier;
		final double scaleMultiplier;
		final double castFailureChance;
		final boolean sealed;
		final BackfireState backfire;

		AggregatedPenaltyState(
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
			this.manaRegenMultiplier = manaRegenMultiplier;
			this.manaCostMultiplier = manaCostMultiplier;
			this.cooldownMultiplier = cooldownMultiplier;
			this.outgoingDamageMultiplier = outgoingDamageMultiplier;
			this.armorEffectivenessMultiplier = armorEffectivenessMultiplier;
			this.movementSpeedMultiplier = movementSpeedMultiplier;
			this.attackSpeedMultiplier = attackSpeedMultiplier;
			this.maxHealthMultiplier = maxHealthMultiplier;
			this.knockbackResistanceMultiplier = knockbackResistanceMultiplier;
			this.scaleMultiplier = scaleMultiplier;
			this.castFailureChance = castFailureChance;
			this.sealed = sealed;
			this.backfire = backfire;
		}
	}

	static final class OwnerTargetKey {
		final UUID ownerId;
		final UUID targetId;

		OwnerTargetKey(UUID ownerId, UUID targetId) {
			this.ownerId = ownerId;
			this.targetId = targetId;
		}
	}

	static final class InventorySlotRef {
		final int slot;
		final InventorySlotType type;

		InventorySlotRef(int slot, InventorySlotType type) {
			this.slot = slot;
			this.type = type;
		}

		ItemStack stack(ServerPlayerEntity player) {
			return player.getInventory().getStack(slot);
		}

		ItemStack remove(ServerPlayerEntity player) {
			ItemStack existing = player.getInventory().getStack(slot);
			if (existing.isEmpty()) {
				return ItemStack.EMPTY;
			}
			ItemStack removed = existing.copy();
			player.getInventory().setStack(slot, ItemStack.EMPTY);
			return removed;
		}
	}

	enum StructureBlock {
		NONE,
		CARPET,
		THRONE,
		PILLAR
	}

	record WeightedBlockState(BlockState state, int weight) {
	}
}



