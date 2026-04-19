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

abstract class GreedRuntimeState {
	static final int TICKS_PER_SECOND = 20;
	static final int TOLLKEEPERS_CLAIM_VORTEX_ARMS = 8;
	static final int COIN_UNITS_PER_COIN = MagicPlayerData.GREED_COIN_UNITS_PER_COIN;
	static final UUID COMMAND_COIN_SOURCE_ID = new UUID(0L, 1L);
	static final Identifier KINGS_DUES_ATTACK_SPEED_MODIFIER_ID = Identifier.of(
		Magic.MOD_ID,
		"kings_dues_attack_speed_lock"
	);
	static final ParticleEffect GOLD_NUGGET_PARTICLE = new ItemStackParticleEffect(
		ParticleTypes.ITEM,
		new ItemStack(Items.GOLD_NUGGET)
	);
	static final Map<UUID, AppraisersMarkState> APPRAISERS_MARK_STATES = new HashMap<>();
	static final Map<UUID, Integer> APPRAISERS_MARK_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, GreedCoinStorage> GREED_COIN_STORAGES = new HashMap<>();
	static final Map<UUID, TollkeepersClaimZoneState> TOLLKEEPER_ZONES = new HashMap<>();
	static final Map<UUID, RootState> ROOTED_PLAYERS = new HashMap<>();
	static final Map<UUID, Integer> KINGS_DUES_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> BANKRUPTCY_COOLDOWN_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> SPRINT_LOCK_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> SHIELD_LOCK_END_TICK = new HashMap<>();
	static final Map<UUID, Integer> ABILITY_LOCK_END_TICK = new HashMap<>();
	static final Map<UUID, AttackSpeedLockState> ATTACK_SPEED_LOCKS = new HashMap<>();
	static final Map<UUID, Double> MANA_SICKNESS_DRAIN_BUFFER = new HashMap<>();
	static final Map<UUID, Boolean> LAST_ELYTRA_EQUIPPED_STATE = new HashMap<>();
	static final Map<UUID, Integer> LAST_SPRINT_TRIGGER_TICK = new HashMap<>();
	static final Map<UUID, Integer> LAST_JUMP_TRIGGER_TICK = new HashMap<>();

	static int currentTick(PlayerEntity player) {
		MinecraftServer server = player.getEntityWorld().getServer();
		return server == null ? Integer.MIN_VALUE : server.getTicks();
	}

	static String greedCoinTriggerIdForAbility(MagicAbility ability) {
		if (ability == null || ability == MagicAbility.NONE) {
			return null;
		}
		if (isMaximumOrDomainGreedCoinAbility(ability)) {
			return "magic_ability_maximum_or_domain";
		}
		return switch (ability.slot()) {
			case 1 -> "magic_ability_1";
			case 2 -> "magic_ability_2";
			case 3 -> "magic_ability_3";
			case 4 -> "magic_ability_4";
			default -> null;
		};
	}

	static boolean isMaximumOrDomainGreedCoinAbility(MagicAbility ability) {
		return ability == MagicAbility.ABSOLUTE_ZERO
			|| ability == MagicAbility.FROST_DOMAIN_EXPANSION
			|| ability == MagicAbility.LOVE_DOMAIN_EXPANSION
			|| ability == MagicAbility.OVERRIDE
			|| ability == MagicAbility.PLUS_ULTRA
			|| ability == MagicAbility.ASTRAL_CATACLYSM
			|| ability == MagicAbility.GREED_DOMAIN_EXPANSION;
	}

	static boolean isPassiveGreedCoinAbility(MagicAbility ability) {
		return ability == MagicAbility.MARTYRS_FLAME
			|| ability == MagicAbility.SPOTLIGHT
			|| ability == MagicAbility.CASSIOPEIA
			|| ability == MagicAbility.COMEDIC_REWRITE
			|| ability == MagicAbility.TILL_DEATH_DO_US_PART;
	}

	enum AppraisersMarkStage {
		WAITING,
		MARKED
	}

	static final class AppraisersMarkState {
		AppraisersMarkStage stage;
		UUID markedTargetId;
		double drainBuffer;
		int lastParticleTick;

		AppraisersMarkState(AppraisersMarkStage stage) {
			this.stage = stage;
			this.lastParticleTick = Integer.MIN_VALUE;
		}

		static AppraisersMarkState waiting() {
			return new AppraisersMarkState(AppraisersMarkStage.WAITING);
		}
	}

	static final class GreedCoinStorage {
		final LinkedHashMap<UUID, GreedCoinContribution> contributions = new LinkedHashMap<>();
		int temporaryOverflowCoinUnits;
		int lastGainTick = Integer.MIN_VALUE;
	}

	static final class GreedCoinContribution {
		int coinUnits;
		int expiresTick;

		GreedCoinContribution(int coinUnits, int expiresTick) {
			this.coinUnits = coinUnits;
			this.expiresTick = expiresTick;
		}
	}

	static final class TollkeepersClaimZoneState {
		final UUID casterId;
		final net.minecraft.registry.RegistryKey<World> dimension;
		final double centerX;
		final double centerY;
		final double centerZ;
		final double radius;
		final int coinsSpent;
		final int createdTick;
		final int expiresTick;
		int nextParticleTick;
		int nextSoundTick;
		final Set<UUID> insidePlayerIds = new HashSet<>();
		final Set<UUID> rootConsumedPlayerIds = new HashSet<>();

		TollkeepersClaimZoneState(
			UUID casterId,
			net.minecraft.registry.RegistryKey<World> dimension,
			double centerX,
			double centerY,
			double centerZ,
			double radius,
			int coinsSpent,
			int createdTick,
			int expiresTick
		) {
			this.casterId = casterId;
			this.dimension = dimension;
			this.centerX = centerX;
			this.centerY = centerY;
			this.centerZ = centerZ;
			this.radius = radius;
			this.coinsSpent = coinsSpent;
			this.createdTick = createdTick;
			this.expiresTick = expiresTick;
			this.nextParticleTick = Integer.MIN_VALUE;
			this.nextSoundTick = Integer.MIN_VALUE;
		}
	}

	static final class RootState {
		final net.minecraft.registry.RegistryKey<World> dimension;
		final double lockedX;
		final double lockedY;
		final double lockedZ;
		final int endTick;

		RootState(net.minecraft.registry.RegistryKey<World> dimension, double lockedX, double lockedY, double lockedZ, int endTick) {
			this.dimension = dimension;
			this.lockedX = lockedX;
			this.lockedY = lockedY;
			this.lockedZ = lockedZ;
			this.endTick = endTick;
		}
	}

	static final class AttackSpeedLockState {
		final int endTick;
		final double amount;

		AttackSpeedLockState(int endTick, double amount) {
			this.endTick = endTick;
			this.amount = amount;
		}
	}
}

