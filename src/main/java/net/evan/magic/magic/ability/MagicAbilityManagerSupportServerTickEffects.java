package net.evan.magic.magic.ability;

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
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.MagicSchool;
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


abstract class MagicAbilityManagerSupportServerTickEffects extends MagicAbilityManagerSupportPersistence {
	static void applyActiveAbilityEffects(
		ServerPlayerEntity player,
		int currentTick,
		Map<UUID, Set<UUID>> absoluteZeroAuraSeenThisTick
	) {
		MagicAbility activeAbility = activeAbility(player);
		if (activeAbility == MagicAbility.NONE) {
			return;
		}

		if (!player.isAlive()) {
			clearDeathAbilityState(player);
			return;
		}

		if (activeAbility.school() != MagicPlayerData.getSchool(player)) {
			if (activeAbility == MagicAbility.BELOW_FREEZING) {
				endFrostStagedMode(player, currentTick, FrostStageEndReason.INVALID, false, false);
			}
			if (activeAbility == MagicAbility.IGNITION) {
				endIgnition(player, currentTick, BurningPassionIgnitionEndReason.INVALID, false, false);
			}
			if (activeAbility == MagicAbility.ABSOLUTE_ZERO) {
				deactivateAbsoluteZero(player);
			}
			if (activeAbility == MagicAbility.PLANCK_HEAT) {
				deactivatePlanckHeat(player);
			}
			if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
				deactivateLoveAtFirstSight(player);
			}
			if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
				deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.SCHOOL_CHANGED, false);
			}
			if (activeAbility == MagicAbility.MANIPULATION) {
				deactivateManipulation(player, true, "school changed or invalid");
			}
			if (activeAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
				deactivateHerculesBurden(player, true, false);
			}
			if (activeAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
				clearSagittariusWindup(player);
			}
			if (activeAbility == MagicAbility.ORIONS_GAMBIT) {
				endOrionsGambit(player, OrionGambitEndReason.SCHOOL_CHANGED, currentTick, false);
			}
			if (activeAbility == MagicAbility.PLUS_ULTRA) {
				endPlusUltra(player, currentTick, PlusUltraEndMode.FULL, false);
			}

			setActiveAbility(player, MagicAbility.NONE);
			return;
		}

		if (activeAbility == MagicAbility.BELOW_FREEZING) {
			if (isMagicSuppressed(player)) {
				endFrostStagedMode(player, currentTick, FrostStageEndReason.LOCKED, true, false);
				return;
			}
			if (isLockedByForeignLoveDomain(player)) {
				endFrostStagedMode(player, currentTick, FrostStageEndReason.LOCKED, false, false);
				return;
			}
			applyBelowFreezing(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.IGNITION) {
			if (isMagicSuppressed(player)) {
				endIgnition(player, currentTick, BurningPassionIgnitionEndReason.LOCKED, true, false);
				return;
			}
			if (isLockedByForeignLoveDomain(player)) {
				endIgnition(player, currentTick, BurningPassionIgnitionEndReason.LOCKED, false, false);
				return;
			}
			applyIgnition(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.ABSOLUTE_ZERO) {
			applyAbsoluteZero(player, currentTick, absoluteZeroAuraSeenThisTick);
			return;
		}

		if (activeAbility == MagicAbility.PLANCK_HEAT) {
			applyPlanckHeat(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			applyLoveAtFirstSight(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			applyTillDeathDoUsPart(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.MANIPULATION) {
			applyManipulation(player);
			return;
		}

		if (activeAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			applyHerculesBurden(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			applySagittariusAstralArrow(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.ORIONS_GAMBIT) {
			applyOrionsGambit(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.PLUS_ULTRA) {
			applyPlusUltra(player, currentTick);
			return;
		}

		if (activeAbility == MagicAbility.ASTRAL_CATACLYSM) {
			applyAstralCataclysm(player, currentTick);
		}
	}

	static void applyBelowFreezing(ServerPlayerEntity player, int currentTick) {
		applyFrostStagedMode(player, currentTick);
	}

	static void applyAbsoluteZero(
		ServerPlayerEntity player,
		int currentTick,
		Map<UUID, Set<UUID>> absoluteZeroAuraSeenThisTick
	) {
		applyFrostMaximum(player, currentTick);
	}

	static void applyPlanckHeat(ServerPlayerEntity player, int currentTick) {
		setActiveAbility(player, MagicAbility.NONE);
	}

	static void applyPlanckHeatCasterBuffs(ServerPlayerEntity caster) {
		caster.addStatusEffect(
			new StatusEffectInstance(
				StatusEffects.ABSORPTION,
				PLANCK_HEAT_ABSORPTION_DURATION_TICKS,
				PLANCK_HEAT_ABSORPTION_AMPLIFIER,
				true,
				true,
				true
			)
		);
		caster.addStatusEffect(
			new StatusEffectInstance(
				StatusEffects.STRENGTH,
				PLANCK_HEAT_STRENGTH_DURATION_TICKS,
				PLANCK_HEAT_STRENGTH_AMPLIFIER,
				true,
				true,
				true
			)
		);
		caster.addStatusEffect(
			new StatusEffectInstance(
				StatusEffects.FIRE_RESISTANCE,
				PLANCK_HEAT_FIRE_RESISTANCE_DURATION_TICKS,
				PLANCK_HEAT_FIRE_RESISTANCE_AMPLIFIER,
				true,
				true,
				true
			)
		);
	}

	static void applyPlanckHeatFrostPhase(ServerPlayerEntity caster, int currentTick) {
		cleanseAbsoluteZeroNegatives(caster);
		refreshStatusEffect(caster, StatusEffects.RESISTANCE, EFFECT_REFRESH_TICKS, ABSOLUTE_ZERO_RESISTANCE_AMPLIFIER, true, false, true);
		spawnPlanckHeatFrostCasterParticles(caster);

		UUID casterId = caster.getUuid();
		Map<UUID, Integer> nextDamageTicksByTarget = PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.computeIfAbsent(casterId, ignored -> new HashMap<>());
		Set<UUID> seenTargets = new HashSet<>();
		Box area = caster.getBoundingBox().expand(PLANCK_HEAT_AURA_RADIUS);
		Predicate<Entity> filter = entity -> isMagicTargetableEntity(entity) && entity != caster;

		for (Entity entity : caster.getEntityWorld().getOtherEntities(caster, area, filter)) {
			if (entity instanceof LivingEntity target) {
				UUID targetId = target.getUuid();
				seenTargets.add(targetId);
				refreshStatusEffect(target, StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, PLANCK_HEAT_FROST_SLOWNESS_AMPLIFIER, true, true, true);
				spawnHitboxShardParticles(target, 2);

				int nextDamageTick = nextDamageTicksByTarget.getOrDefault(targetId, currentTick + PLANCK_HEAT_FROST_DAMAGE_INTERVAL_TICKS);
				if (currentTick >= nextDamageTick) {
					dealPlanckHeatFrostDamage(caster.getUuid(), target);
					nextDamageTick = currentTick + PLANCK_HEAT_FROST_DAMAGE_INTERVAL_TICKS;
				}

				nextDamageTicksByTarget.put(targetId, nextDamageTick);
			}
		}

		nextDamageTicksByTarget.entrySet().removeIf(entry -> !seenTargets.contains(entry.getKey()));
		if (nextDamageTicksByTarget.isEmpty()) {
			PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.remove(casterId);
		}
	}

	static void applyPlanckHeatFirePhase(ServerPlayerEntity caster, int currentTick) {
		spawnPlanckHeatFireCasterParticles(caster);

		Box area = caster.getBoundingBox().expand(PLANCK_HEAT_AURA_RADIUS);
		Predicate<Entity> filter = entity -> isMagicTargetableEntity(entity) && entity != caster;

		for (Entity entity : caster.getEntityWorld().getOtherEntities(caster, area, filter)) {
			if (entity instanceof LivingEntity target) {
				applyOrRefreshEnhancedFire(target, caster.getUuid(), currentTick, PLANCK_HEAT_AURA_ENHANCED_FIRE_DURATION_TICKS);
				refreshStatusEffect(target, StatusEffects.HUNGER, EFFECT_REFRESH_TICKS, PLANCK_HEAT_FIRE_PHASE_HUNGER_AMPLIFIER, true, true, true);
				refreshStatusEffect(target, StatusEffects.NAUSEA, EFFECT_REFRESH_TICKS, PLANCK_HEAT_FIRE_PHASE_NAUSEA_AMPLIFIER, true, true, true);
			}
		}
	}

	static void applyLoveAtFirstSight(ServerPlayerEntity caster, int currentTick) {
		LivingEntity target = findLivingTargetInLineOfSight(caster);
		if (target == null) {
			return;
		}

		LOVE_POWER_ACTIVE_THIS_SECOND.put(caster.getUuid(), true);
		LoveLockState existingState = LOVE_LOCKED_TARGETS.get(target.getUuid());
		if (existingState == null || !existingState.casterId.equals(caster.getUuid())) {
			LOVE_LOCKED_TARGETS.put(
				target.getUuid(),
				new LoveLockState(
					target.getEntityWorld().getRegistryKey(),
					caster.getUuid(),
					target.getX(),
					target.getY(),
					target.getZ(),
					currentTick
				)
			);
			return;
		}

		existingState.lastSeenTick = currentTick;
	}

	static void applyTillDeathDoUsPart(ServerPlayerEntity caster, int currentTick) {
		TillDeathDoUsPartState state = TILL_DEATH_DO_US_PART_STATES.get(caster.getUuid());
		if (state == null) {
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		ServerPlayerEntity linkedPlayer = caster.getEntityWorld().getServer().getPlayerManager().getPlayer(state.linkedPlayerId);
		if (linkedPlayer == null || linkedPlayer.isSpectator()) {
			deactivateTillDeathDoUsPart(caster, TillDeathDoUsPartEndReason.LINK_TARGET_INVALID, true);
			return;
		}
		if (!linkedPlayer.isAlive()) {
			dealTrackedMagicDamage(caster, state.linkedPlayerId, caster.getDamageSources().magic(), Float.MAX_VALUE);
			deactivateTillDeathDoUsPart(caster, TillDeathDoUsPartEndReason.LINK_TARGET_DIED, false);
			return;
		}

		if (currentTick > state.linkEndTick) {
			deactivateTillDeathDoUsPart(caster, TillDeathDoUsPartEndReason.LINK_EXPIRED, true);
			return;
		}

		syncTillDeathDoUsPartHealth(caster, linkedPlayer, state);
	}

	static boolean onAllowLivingEntityDamage(LivingEntity entity, DamageSource source, float amount) {
		if (amount <= 0.0F) {
			return true;
		}
		if (isFrostMaximumInvulnerable(entity, source)) {
			return false;
		}
		if (isFrostHelplessAttacker(source)) {
			return false;
		}

		int currentTick = entity.getEntityWorld().getServer().getTicks();
		ServerPlayerEntity directAttacker = attackingPlayerFrom(source);
		if (directAttacker != null && isDirectCasterDamageBlocked(directAttacker, entity, currentTick)) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(entity.getUuid());
			return false;
		}

		if (!CELESTIAL_DAMAGE_REDIRECT_GUARD.contains(entity.getUuid()) && tryApplyCelestialAlignmentDamageRedirect(entity, source, amount)) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(entity.getUuid());
			return false;
		}

		if (!CELESTIAL_DAMAGE_SCALE_GUARD.contains(entity.getUuid())) {
			float scaledAmount = celestialScaledIncomingDamage(entity, source, amount);
			if (Math.abs(scaledAmount - amount) > 1.0E-6F && entity.getEntityWorld() instanceof ServerWorld world) {
				CELESTIAL_DAMAGE_SCALE_GUARD.add(entity.getUuid());
				try {
					entity.damage(world, source, scaledAmount);
				} finally {
					CELESTIAL_DAMAGE_SCALE_GUARD.remove(entity.getUuid());
				}
				DOMAIN_CLASH_PENDING_DAMAGE.remove(entity.getUuid());
				return false;
			}
		}

		if (!(entity instanceof ServerPlayerEntity player)) {
			return true;
		}

		UUID playerId = player.getUuid();
		if (hasActiveComedicRewriteImmunity(playerId, currentTick)) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(playerId);
			return false;
		}

		if (source.isIn(DamageTypeTags.IS_FALL) && hasActiveComedicRewriteFallProtection(playerId, currentTick)) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(playerId);
			return false;
		}

		if (
			hasActivePlusUltra(player)
			&& !source.isOf(DamageTypes.OUT_OF_WORLD)
			&& !source.isOf(DamageTypes.GENERIC_KILL)
			&& !source.isOf(DamageTypes.OUTSIDE_BORDER)
		) {
			if (!PLUS_ULTRA_DAMAGE_SCALING_GUARD.add(playerId)) {
				return true;
			}

			try {
				float scaledAmount = (float) (amount * PLUS_ULTRA_INCOMING_DAMAGE_MULTIPLIER);
				if (Math.abs(scaledAmount - amount) <= 1.0E-6F) {
					return true;
				}

				if (!(player.getEntityWorld() instanceof ServerWorld world)) {
					return true;
				}

				player.damage(world, source, scaledAmount);
			} finally {
				PLUS_ULTRA_DAMAGE_SCALING_GUARD.remove(playerId);
			}

			return false;
		}

		if (!hasActiveOrionsGambitPenalty(player)) {
			return true;
		}

		if (!ORIONS_GAMBIT_DAMAGE_SCALING_GUARD.add(playerId)) {
			return true;
		}

		try {
			float scaledAmount = (float) (amount * ORIONS_GAMBIT_CASTER_INCOMING_DAMAGE_MULTIPLIER);
			if (Math.abs(scaledAmount - amount) <= 1.0E-6F) {
				return true;
			}

			if (!(player.getEntityWorld() instanceof ServerWorld world)) {
				return true;
			}

			player.damage(world, source, scaledAmount);
		} finally {
			ORIONS_GAMBIT_DAMAGE_SCALING_GUARD.remove(playerId);
		}

		return false;
	}

	public static boolean onPlayerDamaged(ServerPlayerEntity damagedPlayer, DamageSource source, float amount) {
		if (source == null || amount <= 0.0F || damagedPlayer.getEntityWorld().isClient()) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(damagedPlayer.getUuid());
			return false;
		}

		captureDomainClashDamage(damagedPlayer, source, amount);

		if (isMagicSuppressed(damagedPlayer)) {
			return false;
		}

		if (shouldTriggerMartyrsFlameRetaliation(damagedPlayer)) {
			ServerPlayerEntity martyrsFlameAttacker = attackingPlayerFrom(source);
			if (martyrsFlameAttacker != null && martyrsFlameAttacker != damagedPlayer) {
				retaliateWithMartyrsFlame(damagedPlayer, martyrsFlameAttacker, damagedPlayer.getEntityWorld().getServer().getTicks());
			}
		}

		UUID playerId = damagedPlayer.getUuid();
		if (!TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.contains(playerId) || TILL_DEATH_DO_US_PART_STATES.containsKey(playerId)) {
			return false;
		}

		if (MagicPlayerData.getSchool(damagedPlayer) != MagicSchool.LOVE) {
			return false;
		}

		if (!MagicConfig.get().abilityAccess.isAbilityUnlocked(playerId, MagicAbility.TILL_DEATH_DO_US_PART)) {
			return false;
		}

		DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.get(playerId);
		if (ownedDomain != null && ownedDomain.ability != MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return false;
		}

		Entity attacker = source.getAttacker();
		if (!(attacker instanceof ServerPlayerEntity attackerPlayer) || attackerPlayer == damagedPlayer || !attackerPlayer.isAlive() || attackerPlayer.isSpectator()) {
			return false;
		}

		int currentTick = damagedPlayer.getEntityWorld().getServer().getTicks();
		int remainingTicks = tillDeathDoUsPartCooldownRemaining(damagedPlayer, currentTick);
		if (remainingTicks > 0) {
			if (shouldSendTillDeathCooldownMessage(playerId, currentTick)) {
				sendAbilityCooldownMessage(damagedPlayer, MagicAbility.TILL_DEATH_DO_US_PART, remainingTicks, true);
			}
			return false;
		}

		if (!isOrionsGambitManaCostSuppressed(damagedPlayer) && MagicPlayerData.getMana(damagedPlayer) <= 0) {
			return false;
		}

		activateTillDeathDoUsPartLink(damagedPlayer, attackerPlayer, currentTick);
		return false;
	}

	public static boolean onPlayerPreApplyDamage(
		ServerPlayerEntity player,
		ServerWorld world,
		DamageSource source,
		float incomingAmount,
		float finalDamage
	) {
		if (player == null || world == null || source == null || world.isClient() || player.isDead() || finalDamage <= 0.0F) {
			return false;
		}

		UUID playerId = player.getUuid();
		if (
			COMEDIC_REWRITE_PENDING_STATES.containsKey(playerId) ||
			isMagicSuppressed(player) ||
			!COMEDIC_REWRITE_PASSIVE_ENABLED.contains(playerId)
		) {
			return false;
		}

		if (!shouldKeepComedicRewriteEnabled(player)) {
			deactivateComedicRewrite(player);
			return false;
		}

		int currentTick = world.getServer().getTicks();
		if (comedicRewriteCooldownRemaining(player, currentTick) > 0) {
			return false;
		}

		float currentHealth = player.getHealth();
		if (currentHealth <= 0.0F) {
			return false;
		}

		boolean lethal = finalDamage >= currentHealth;
		double healthFraction = finalDamage / Math.max(0.0001F, currentHealth);
		if (
			!lethal &&
			finalDamage < COMEDIC_REWRITE_DANGEROUS_DAMAGE_THRESHOLD &&
			healthFraction < COMEDIC_REWRITE_DANGEROUS_HEALTH_FRACTION_THRESHOLD
		) {
			return false;
		}

		int manaCost = (int) Math.ceil(manaFromPercentExact(COMEDIC_REWRITE_MANA_COST_PERCENT));
		if (!canSpendAbilityCost(player, manaCost)) {
			return false;
		}

		double severity = comedicRewriteSeverity(currentHealth, finalDamage, lethal);
		double procChancePercent = MathHelper.clamp(
			COMEDIC_REWRITE_BASE_PROC_CHANCE_PERCENT
				+ severity * COMEDIC_REWRITE_SEVERITY_PROC_BONUS_PERCENT
				+ (lethal ? COMEDIC_REWRITE_LETHAL_PROC_BONUS_PERCENT : 0.0),
			0.0,
			COMEDIC_REWRITE_MAX_PROC_CHANCE_PERCENT
		);
		if (player.getRandom().nextDouble() * 100.0 >= procChancePercent) {
			return false;
		}

		boolean unsafeScene = isComedicRewriteUnsafeScene(player, world, source);
		Vec3d safePos = findBestComedicRewriteSafePosition(player, world);
		ComedicRewriteOutcome outcome = selectComedicRewriteOutcome(player, unsafeScene, safePos);
		if (outcome == null) {
			return false;
		}

		spendAbilityCost(player, manaCost);
		startComedicRewriteCooldown(playerId, currentTick);
		if (COMEDIC_REWRITE_IMMUNITY_TICKS > 0) {
			COMEDIC_REWRITE_IMMUNITY_END_TICK.put(playerId, currentTick + COMEDIC_REWRITE_IMMUNITY_TICKS);
		}
		if (COMEDIC_REWRITE_FALL_PROTECTION_TICKS > 0) {
			COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.put(playerId, currentTick + COMEDIC_REWRITE_FALL_PROTECTION_TICKS);
		}

		player.setHealth(comedicRewriteSavedHealthPoints(severity));
		player.fallDistance = 0.0F;
		if (COMEDIC_REWRITE_EXTINGUISH_ON_REWRITE) {
			player.extinguish();
		}

		DOMAIN_CLASH_PENDING_DAMAGE.remove(playerId);
		COMEDIC_REWRITE_PENDING_STATES.put(
			playerId,
			new PendingComedicRewriteState(
				outcome,
				severity,
				resolveComedicRewriteLaunchDirection(player, source),
				safePos
			)
		);
		return true;
	}

	public static void onPlayerDamageResolved(ServerPlayerEntity player) {
		PendingComedicRewriteState state = COMEDIC_REWRITE_PENDING_STATES.remove(player.getUuid());
		if (state == null || player.isDead() || !(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		if (state.outcome() == ComedicRewriteOutcome.LAUNCHED_THROUGH_THE_SCENE) {
			applyComedicRewriteLaunchOutcome(player, world, state);
			return;
		}
		if (state.outcome() == ComedicRewriteOutcome.RAVAGER_BIT) {
			applyComedicRewriteRavagerOutcome(player, world, state);
			return;
		}
		applyComedicRewriteParrotOutcome(player, world, state);
	}
}

