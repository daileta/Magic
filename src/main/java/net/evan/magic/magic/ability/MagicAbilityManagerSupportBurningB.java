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


abstract class MagicAbilityManagerSupportBurningB extends MagicAbilityManagerSupportBurningA {
	static double burningPassionHealthCostHearts(MagicAbility ability) {
		return switch (ability) {
			case IGNITION -> BURNING_PASSION_CONFIG.heat.ignitionHealthCostHearts;
			case SEARING_DASH -> BURNING_PASSION_CONFIG.heat.searingDashHealthCostHearts;
			case CINDER_MARK -> BURNING_PASSION_CONFIG.heat.cinderMarkHealthCostHearts;
			case ENGINE_HEART -> BURNING_PASSION_CONFIG.heat.engineHeartHealthCostHearts;
			case OVERRIDE -> BURNING_PASSION_CONFIG.heat.overrideHealthCostHearts;
			default -> 0.0;
		};
	}

	static void applyOrRefreshBurningPassionAuraFire(
		LivingEntity target,
		UUID casterId,
		int currentTick,
		float damagePerTick,
		double fireResistantTargetDamageMultiplier,
		boolean fireDamageIgnoresFireResistance,
		int damageIntervalTicks,
		int refreshTicks,
		boolean persistent,
		boolean extinguishInWater,
		boolean extinguishInRain
	) {
		if (damagePerTick <= 0.0F || refreshTicks <= 0) {
			return;
		}
		if (isBurningPassionFireExtinguished(target, extinguishInWater, extinguishInRain)) {
			BURNING_PASSION_AURA_FIRE_TARGETS.remove(target.getUuid());
			target.extinguish();
			return;
		}

		RegistryKey<World> dimension = target.getEntityWorld().getRegistryKey();
		BurningPassionAuraFireState existingState = BURNING_PASSION_AURA_FIRE_TARGETS.get(target.getUuid());
		int expiresTick = currentTick + refreshTicks;
		if (existingState == null || existingState.dimension != dimension) {
			BURNING_PASSION_AURA_FIRE_TARGETS.put(
				target.getUuid(),
				new BurningPassionAuraFireState(
					dimension,
					casterId,
					expiresTick,
					currentTick + Math.max(1, damageIntervalTicks),
					Math.max(0.0F, damagePerTick),
					Math.max(0.0, fireResistantTargetDamageMultiplier),
					fireDamageIgnoresFireResistance,
					Math.max(1, damageIntervalTicks),
					refreshTicks,
					persistent,
					extinguishInWater,
					extinguishInRain
				)
			);
		} else {
			existingState.casterId = casterId;
			existingState.expiresTick = Math.max(existingState.expiresTick, expiresTick);
			existingState.damagePerTick = Math.max(existingState.damagePerTick, Math.max(0.0F, damagePerTick));
			existingState.fireResistantTargetDamageMultiplier = Math.max(
				existingState.fireResistantTargetDamageMultiplier,
				Math.max(0.0, fireResistantTargetDamageMultiplier)
			);
			existingState.fireDamageIgnoresFireResistance = existingState.fireDamageIgnoresFireResistance || fireDamageIgnoresFireResistance;
			existingState.damageIntervalTicks = Math.max(1, damageIntervalTicks);
			existingState.refreshTicks = Math.max(1, refreshTicks);
			existingState.persistent = existingState.persistent || persistent;
			existingState.extinguishInWater = extinguishInWater;
			existingState.extinguishInRain = extinguishInRain;
		}
		target.setOnFireForTicks(refreshTicks);
	}

	static void applyOrRefreshBurningPassionSelfFire(ServerPlayerEntity player, int stage, int currentTick) {
		int durationTicks = Math.max(1, BURNING_PASSION_CONFIG.heat.selfFireDurationTicks);
		int intervalTicks = Math.max(1, BURNING_PASSION_CONFIG.heat.selfFireDamageIntervalTicks);
		float damagePerTick = Math.max(0.0F, BURNING_PASSION_CONFIG.heat.selfFireDamagePerTick);
		boolean extinguishWhenWet = BURNING_PASSION_CONFIG.heat.selfFireExtinguishesWhenWetBeforeStageThree && stage < 3;
		if (isBurningPassionFireExtinguished(player, extinguishWhenWet, extinguishWhenWet)) {
			BURNING_PASSION_SELF_FIRE_TARGETS.remove(player.getUuid());
			player.extinguish();
			return;
		}
		RegistryKey<World> dimension = player.getEntityWorld().getRegistryKey();
		BurningPassionAuraFireState existingState = BURNING_PASSION_SELF_FIRE_TARGETS.get(player.getUuid());
		int expiresTick = currentTick + durationTicks;
		if (existingState == null || existingState.dimension != dimension) {
			BURNING_PASSION_SELF_FIRE_TARGETS.put(
				player.getUuid(),
				new BurningPassionAuraFireState(
					dimension,
					player.getUuid(),
					expiresTick,
					currentTick + intervalTicks,
					damagePerTick,
					Math.max(0.0, BURNING_PASSION_CONFIG.heat.selfFireResistantDamageMultiplier),
					BURNING_PASSION_CONFIG.heat.selfFireIgnoresFireResistance,
					intervalTicks,
					durationTicks,
					false,
					extinguishWhenWet,
					extinguishWhenWet
				)
			);
		} else {
			existingState.expiresTick = Math.max(existingState.expiresTick, expiresTick);
			existingState.damagePerTick = Math.max(existingState.damagePerTick, damagePerTick);
			existingState.fireResistantTargetDamageMultiplier = Math.max(
				existingState.fireResistantTargetDamageMultiplier,
				Math.max(0.0, BURNING_PASSION_CONFIG.heat.selfFireResistantDamageMultiplier)
			);
			existingState.fireDamageIgnoresFireResistance = existingState.fireDamageIgnoresFireResistance || BURNING_PASSION_CONFIG.heat.selfFireIgnoresFireResistance;
			existingState.damageIntervalTicks = intervalTicks;
			existingState.refreshTicks = durationTicks;
			existingState.extinguishInWater = extinguishWhenWet;
			existingState.extinguishInRain = extinguishWhenWet;
		}
		player.setOnFireForTicks(durationTicks);
	}

	static void updateBurningPassionAuraFireTargets(MinecraftServer server, int currentTick) {
		updateBurningPassionFireMap(server, currentTick, BURNING_PASSION_AURA_FIRE_TARGETS);
		updateBurningPassionFireMap(server, currentTick, BURNING_PASSION_SELF_FIRE_TARGETS);
	}

	static void updateBurningPassionFireMap(
		MinecraftServer server,
		int currentTick,
		Map<UUID, BurningPassionAuraFireState> fireStates
	) {
		Iterator<Map.Entry<UUID, BurningPassionAuraFireState>> iterator = fireStates.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, BurningPassionAuraFireState> entry = iterator.next();
			BurningPassionAuraFireState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
				iterator.remove();
				continue;
			}

			if (isBurningPassionFireExtinguished(target, state.extinguishInWater, state.extinguishInRain)) {
				target.extinguish();
				iterator.remove();
				continue;
			}
			if (currentTick > state.expiresTick) {
				iterator.remove();
				continue;
			}

			int fireTicks = Math.max(target.getFireTicks(), Math.max(1, state.expiresTick - currentTick + 1));
			target.setFireTicks(fireTicks);
			if (state.damagePerTick > 0.0F && currentTick >= state.nextDamageTick) {
				dealBurningPassionFireDamage(
					target,
					state.casterId,
					world,
					state.damagePerTick,
					state.fireResistantTargetDamageMultiplier,
					state.fireDamageIgnoresFireResistance
				);
				state.nextDamageTick = currentTick + state.damageIntervalTicks;
			}
		}
	}

	static void clearBurningPassionAuraFireByCaster(UUID casterId) {
		if (casterId == null) {
			return;
		}
		BURNING_PASSION_AURA_FIRE_TARGETS.entrySet().removeIf(entry -> casterId.equals(entry.getValue().casterId));
	}

	static void dealBurningPassionFireDamage(
		LivingEntity target,
		UUID casterId,
		ServerWorld world,
		float damagePerTick,
		double fireResistantTargetDamageMultiplier,
		boolean fireDamageIgnoresFireResistance
	) {
		if (damagePerTick <= 0.0F) {
			return;
		}
		boolean fireResistant = target.hasStatusEffect(StatusEffects.FIRE_RESISTANCE);
		float resolvedDamage = damagePerTick;
		DamageSource source = world.getDamageSources().onFire();
		if (fireResistant) {
			resolvedDamage = (float) (resolvedDamage * Math.max(0.0, fireResistantTargetDamageMultiplier));
			if (resolvedDamage <= 0.0F) {
				return;
			}
			if (fireDamageIgnoresFireResistance || Math.abs(fireResistantTargetDamageMultiplier - 1.0) > 1.0E-6) {
				source = world.getDamageSources().magic();
			}
		}
		dealTrackedMagicDamage(target, casterId, source, resolvedDamage);
	}

	static void applyBurningPassionEngineHeartState(
		ServerPlayerEntity player,
		BurningPassionIgnitionState ignitionState,
		int currentTick
	) {
		UUID playerId = player.getUuid();
		if (
			ignitionState.currentStage != 3
			|| !MagicConfig.get().abilityAccess.isAbilityUnlocked(playerId, MagicAbility.ENGINE_HEART)
			|| engineHeartCooldownRemaining(player, currentTick) > 0
		) {
			clearBurningPassionEngineHeartState(player);
			return;
		}

		BurningPassionEngineHeartState state = BURNING_PASSION_ENGINE_HEART_STATES.get(playerId);
		if (state == null || state.dimension != player.getEntityWorld().getRegistryKey()) {
			state = new BurningPassionEngineHeartState(player.getEntityWorld().getRegistryKey(), entityPosition(player));
			BURNING_PASSION_ENGINE_HEART_STATES.put(playerId, state);
		}

		boolean momentumActive = isBurningPassionEngineHeartMomentumActive(player, state);
		if (momentumActive) {
			state.slowdownTicks = 0;
			state.tierBeforeSlowdown = state.currentTier;
			state.sustainedTicks++;
			int unlockedTier = resolveBurningPassionEngineHeartTier(state.sustainedTicks);
			if (unlockedTier > state.currentTier) {
				for (int tier = state.currentTier + 1; tier <= unlockedTier; tier++) {
					if (tier >= 3) {
						spawnBurningPassionEngineHeartTierThreeBreakthroughBurst(player, state);
					}
					notifyBurningPassionEngineHeartTierUnlocked(player, tier, currentTick);
				}
				state.currentTier = unlockedTier;
				if (state.currentTier >= 3) {
					state.specialAttackReady = true;
				}
			}
		} else {
			if (state.slowdownTicks == 0) {
				state.tierBeforeSlowdown = state.currentTier;
			}
			state.slowdownTicks++;
			int downgradedTier = resolveBurningPassionEngineHeartTierAfterSlowdown(state.tierBeforeSlowdown, state.slowdownTicks);
			if (downgradedTier != state.currentTier) {
				state.currentTier = downgradedTier;
				state.sustainedTicks = burningPassionEngineHeartThresholdTicksForTier(downgradedTier);
				if (downgradedTier < 3) {
					state.specialAttackReady = false;
				}
			}
			if (state.slowdownTicks > BURNING_PASSION_CONFIG.engineHeart.fullResetTicks) {
				state.sustainedTicks = 0;
			}
		}

		if (state.currentTier >= 1) {
			ignitionState.heatPercent = clampBurningPassionHeat(
				ignitionState.heatPercent - BURNING_PASSION_CONFIG.engineHeart.tierOneHeatReductionPerSecond / TICKS_PER_SECOND
			);
		}
		if (state.currentTier >= 2) {
			overwriteAttributeModifier(
				player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE),
				BURNING_PASSION_ENGINE_HEART_ATTACK_DAMAGE_MODIFIER_ID,
				BURNING_PASSION_CONFIG.engineHeart.tierTwoAttackBonus,
				EntityAttributeModifier.Operation.ADD_VALUE
			);
		} else {
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), BURNING_PASSION_ENGINE_HEART_ATTACK_DAMAGE_MODIFIER_ID);
		}
		MagicPlayerData.setBurningPassionEngineHeartAfterimageState(
			player,
			momentumActive && state.currentTier >= 1,
			momentumActive ? state.currentTier : 0
		);
		spawnBurningPassionEngineHeartParticles(player, state, currentTick);
	}

	static void tryApplyBurningPassionAttackEffects(ServerPlayerEntity attacker, LivingEntity target) {
		if (
			target == attacker
			|| !isMagicTargetableEntity(target)
			|| MagicPlayerData.getSchool(attacker) != MagicSchool.BURNING_PASSION
			|| !(target.getEntityWorld() instanceof ServerWorld world)
		) {
			return;
		}
		BurningPassionIgnitionState ignitionState = burningPassionIgnitionState(attacker);
		if (ignitionState == null) {
			return;
		}

		int currentTick = world.getServer().getTicks();
		UUID attackerId = attacker.getUuid();
		boolean cinderMarkUnlocked = MagicConfig.get().abilityAccess.isAbilityUnlocked(attackerId, MagicAbility.CINDER_MARK);
		boolean engineHeartUnlocked = MagicConfig.get().abilityAccess.isAbilityUnlocked(attackerId, MagicAbility.ENGINE_HEART);
		BurningPassionCinderMarkArmedState armedState = BURNING_PASSION_CINDER_MARK_ARMED_STATES.get(attackerId);
		if (
			armedState != null
			&& (armedState.dimension != attacker.getEntityWorld().getRegistryKey() || currentTick > armedState.expiresTick)
		) {
			BURNING_PASSION_CINDER_MARK_ARMED_STATES.remove(attackerId);
			armedState = null;
		}

		BurningPassionEngineHeartState engineHeartState = BURNING_PASSION_ENGINE_HEART_STATES.get(attackerId);
		boolean specialAttack =
			engineHeartUnlocked
			&& ignitionState.currentStage == 3
			&& engineHeartState != null
			&& engineHeartState.currentTier >= 3
			&& engineHeartState.specialAttackReady
			&& engineHeartCooldownRemaining(attacker, currentTick) <= 0;
		int markCount = 0;
		if (engineHeartUnlocked && ignitionState.currentStage == 3 && engineHeartState != null && engineHeartState.currentTier >= 2) {
			markCount += Math.max(0, BURNING_PASSION_CONFIG.cinderMark.engineHeartFreeMarkCount);
		}
		if (
			cinderMarkUnlocked
			&& ignitionState.currentStage >= 2
			&& armedState != null
			&& canApplyCinderMarkToTarget(target)
			&& tryResolveBurningPassionCinderMarkHit(attacker, ignitionState, currentTick)
		) {
			markCount += Math.max(0, BURNING_PASSION_CONFIG.cinderMark.armedHitMarkCount);
		}

		if (markCount > 0) {
			applyBurningPassionCinderMarks(attacker, target, currentTick, markCount);
		}
		if (specialAttack) {
			applyBurningPassionEngineHeartSpecialHit(attacker, target, ignitionState, engineHeartState);
		} else if (engineHeartUnlocked && engineHeartState != null && engineHeartState.currentTier >= 2) {
			spawnBurningPassionFreeMarkBurst(world, target);
		}
	}

	static boolean tryResolveBurningPassionCinderMarkHit(
		ServerPlayerEntity attacker,
		BurningPassionIgnitionState ignitionState,
		int currentTick
	) {
		if (!canPayBurningPassionHealthDemand(attacker, ignitionState, MagicAbility.CINDER_MARK)) {
			return false;
		}
		if (!tryPayBurningPassionManaCost(attacker, ignitionState, MagicAbility.CINDER_MARK, BURNING_PASSION_CONFIG.cinderMark.manaCostPercent)) {
			return false;
		}

		payBurningPassionHealthDemand(attacker, ignitionState, MagicAbility.CINDER_MARK);
		double selfHealthCostHearts = Math.max(0.0, BURNING_PASSION_CONFIG.cinderMark.selfHealthCostHearts);
		if (selfHealthCostHearts > 0.0) {
			attacker.setHealth((float) Math.max(1.0, attacker.getHealth() - selfHealthCostHearts * 2.0));
		}
		ignitionState.heatPercent = clampBurningPassionHeat(ignitionState.heatPercent - BURNING_PASSION_CONFIG.cinderMark.heatReductionPercent);
		BURNING_PASSION_CINDER_MARK_ARMED_STATES.remove(attacker.getUuid());
		if (BURNING_PASSION_CONFIG.cinderMark.cooldownTicks > 0) {
			startAbilityCooldownFromNow(attacker.getUuid(), MagicAbility.CINDER_MARK, currentTick);
		}
		syncBurningPassionHud(attacker);
		return true;
	}

	static void applyBurningPassionEngineHeartSpecialHit(
		ServerPlayerEntity attacker,
		LivingEntity target,
		BurningPassionIgnitionState ignitionState,
		BurningPassionEngineHeartState engineHeartState
	) {
		if (!(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		Vec3d impactPos = new Vec3d(target.getX(), target.getBodyY(0.45), target.getZ());
		dealTrackedMagicDamage(
			target,
			attacker.getUuid(),
			world.getDamageSources().explosion(attacker, attacker),
			BURNING_PASSION_CONFIG.engineHeart.tierThreeSpecialAttackDamage
		);
		ignitionState.heatPercent = clampBurningPassionHeat(
			Math.max(ignitionState.heatPercent, BURNING_PASSION_CONFIG.engineHeart.tierThreeSpecialHeatGainPercent)
		);
		world.spawnParticles(
			ParticleTypes.FLAME,
			impactPos.x,
			impactPos.y,
			impactPos.z,
			Math.max(0, BURNING_PASSION_CONFIG.engineHeart.tierThreeBurstParticleCount),
			0.3,
			0.3,
			0.3,
			0.08
		);
		world.spawnParticles(
			ParticleTypes.EXPLOSION,
			impactPos.x,
			impactPos.y,
			impactPos.z,
			Math.max(1, BURNING_PASSION_CONFIG.engineHeart.tierThreeExplosionParticleCount),
			0.18,
			0.18,
			0.18,
			0.0
		);
		playConfiguredSound(world, impactPos, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), BURNING_PASSION_CONFIG.engineHeart.specialSoundVolume, BURNING_PASSION_CONFIG.engineHeart.specialSoundPitch);
		Vec3d launchDirection = normalizedHorizontalDirection(
			new Vec3d(target.getX() - attacker.getX(), 0.0, target.getZ() - attacker.getZ()),
			attacker
		);
		applyForcedVelocity(
			target,
			new Vec3d(
				launchDirection.x * BURNING_PASSION_CONFIG.engineHeart.specialKnockbackHorizontalVelocity,
				BURNING_PASSION_CONFIG.engineHeart.specialKnockbackVerticalVelocity,
				launchDirection.z * BURNING_PASSION_CONFIG.engineHeart.specialKnockbackHorizontalVelocity
			)
		);
		applyForcedVelocity(
			attacker,
			new Vec3d(
				-launchDirection.x * BURNING_PASSION_CONFIG.engineHeart.specialKnockbackHorizontalVelocity,
				BURNING_PASSION_CONFIG.engineHeart.specialKnockbackVerticalVelocity,
				-launchDirection.z * BURNING_PASSION_CONFIG.engineHeart.specialKnockbackHorizontalVelocity
			)
		);
		engineHeartState.sustainedTicks = 0;
		engineHeartState.currentTier = 0;
		engineHeartState.slowdownTicks = 0;
		engineHeartState.tierBeforeSlowdown = 0;
		engineHeartState.specialAttackReady = false;
		detonateBurningPassionCinderMarksOnTarget(attacker, target, BURNING_PASSION_CONFIG.cinderMark.manualDetonationTrueDamage);
		removeAttributeModifier(attacker.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), BURNING_PASSION_ENGINE_HEART_ATTACK_DAMAGE_MODIFIER_ID);
		syncBurningPassionHud(attacker);
	}

	static void spawnIgnitionActivationEffects(ServerPlayerEntity player) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		world.spawnParticles(
			ParticleTypes.FLAME,
			player.getX(),
			player.getBodyY(0.45),
			player.getZ(),
			BURNING_PASSION_CONFIG.ignition.activationFlameParticleCount,
			0.45,
			0.4,
			0.45,
			0.04
		);
		world.spawnParticles(
			ParticleTypes.CAMPFIRE_COSY_SMOKE,
			player.getX(),
			player.getBodyY(0.4),
			player.getZ(),
			BURNING_PASSION_CONFIG.ignition.activationSmokeParticleCount,
			0.35,
			0.25,
			0.35,
			0.01
		);
		world.spawnParticles(
			ParticleTypes.LAVA,
			player.getX(),
			player.getBodyY(0.45),
			player.getZ(),
			BURNING_PASSION_CONFIG.ignition.activationBurstParticleCount,
			0.28,
			0.18,
			0.28,
			0.02
		);
		playConfiguredSound(
			world,
			new Vec3d(player.getX(), player.getBodyY(0.45), player.getZ()),
			SoundEvents.ITEM_FIRECHARGE_USE,
			BURNING_PASSION_CONFIG.ignition.activationSoundVolume,
			BURNING_PASSION_CONFIG.ignition.activationSoundPitch
		);
	}

	static void spawnBurningPassionStageParticles(
		ServerPlayerEntity player,
		MagicConfig.BurningPassionStageConfig stageConfig,
		int stage
	) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		double spread = Math.max(0.25, stageConfig.auraRadius > 0.0 ? Math.min(0.8, stageConfig.auraRadius * 0.06) : 0.45 + stage * 0.08);
		world.spawnParticles(
			ParticleTypes.FLAME,
			player.getX(),
			player.getBodyY(0.45),
			player.getZ(),
			Math.max(4, stageConfig.auraFlameParticleCount),
			spread,
			0.38,
			spread,
			0.02
		);
		world.spawnParticles(
			ParticleTypes.CAMPFIRE_COSY_SMOKE,
			player.getX(),
			player.getBodyY(0.4),
			player.getZ(),
			stageConfig.auraSmokeParticleCount,
			spread,
			0.25,
			spread,
			0.01
		);
	}

	static void applyBurningPassionCinderMarks(
		ServerPlayerEntity attacker,
		LivingEntity target,
		int currentTick,
		int markCount
	) {
		if (!(target.getEntityWorld() instanceof ServerWorld world) || markCount <= 0) {
			return;
		}

		Vec3d impactPos = resolveBurningPassionMeleeImpactPosition(attacker, target);
		for (int index = 0; index < markCount; index++) {
			BurningPassionCinderMarkState state = createBurningPassionCinderMarkState(attacker, target, world, impactPos, currentTick);
			if (state != null) {
				BURNING_PASSION_CINDER_MARKS.add(state);
			}
		}
	}

	static BurningPassionCinderMarkState createBurningPassionCinderMarkState(
		ServerPlayerEntity attacker,
		LivingEntity target,
		ServerWorld world,
		Vec3d impactPos,
		int currentTick
	) {
		Vec3d anchorOffset = impactPos.subtract(new Vec3d(target.getX(), target.getY(), target.getZ()));
		UUID displayId = spawnBurningPassionCinderMarkDisplay(world, impactPos);
		spawnBurningPassionCinderMarkApplyEffects(world, impactPos);
		return new BurningPassionCinderMarkState(
			attacker.getUuid(),
			target.getUuid(),
			world.getRegistryKey(),
			currentTick + Math.max(1, BURNING_PASSION_CONFIG.cinderMark.autoExplodeTicks),
			anchorOffset,
			displayId
		);
	}

	static UUID spawnBurningPassionCinderMarkDisplay(ServerWorld world, Vec3d position) {
		DisplayEntity.BlockDisplayEntity display = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
		display.refreshPositionAndAngles(position.x, position.y, position.z, 0.0F, 0.0F);
		((BlockDisplayEntityAccessorMixin) display).magic$setBlockState(Blocks.MAGMA_BLOCK.getDefaultState());
		((DisplayEntityAccessorMixin) display).magic$setTeleportDuration(1);
		((DisplayEntityAccessorMixin) display).magic$setInterpolationDuration(1);
		float scale = (float) BURNING_PASSION_CONFIG.cinderMark.latchScale;
		((DisplayEntityAccessorMixin) display).magic$setTransformation(
			new AffineTransformation(
				new Vector3f(-0.5F * scale, -0.5F * scale, -0.5F * scale),
				new Quaternionf(),
				new Vector3f(scale, scale, scale),
				new Quaternionf()
			)
		);
		((DisplayEntityAccessorMixin) display).magic$setDisplayWidth(scale);
		((DisplayEntityAccessorMixin) display).magic$setDisplayHeight(scale);
		((DisplayEntityAccessorMixin) display).magic$setViewRange(2.0F);
		display.setNoGravity(true);
		display.setSilent(true);
		display.setInvulnerable(true);
		return world.spawnEntity(display) ? display.getUuid() : null;
	}

	static void spawnBurningPassionCinderMarkApplyEffects(ServerWorld world, Vec3d position) {
		world.spawnParticles(
			ParticleTypes.FLAME,
			position.x,
			position.y,
			position.z,
			Math.max(0, BURNING_PASSION_CONFIG.cinderMark.latchFlameParticleCount),
			0.08,
			0.08,
			0.08,
			0.01
		);
		world.spawnParticles(
			ParticleTypes.WHITE_ASH,
			position.x,
			position.y,
			position.z,
			Math.max(0, BURNING_PASSION_CONFIG.cinderMark.latchSmokeParticleCount),
			0.08,
			0.08,
			0.08,
			0.006
		);
		playConfiguredSound(world, position, SoundEvents.ITEM_FIRECHARGE_USE, BURNING_PASSION_CONFIG.cinderMark.soundVolume, BURNING_PASSION_CONFIG.cinderMark.soundPitch);
	}

	static void updateBurningPassionCinderMarks(MinecraftServer server, int currentTick) {
		Iterator<BurningPassionCinderMarkState> iterator = BURNING_PASSION_CINDER_MARKS.iterator();
		while (iterator.hasNext()) {
			BurningPassionCinderMarkState state = iterator.next();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity entity = world.getEntity(state.targetId);
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
				discardBurningPassionCinderMarkDisplay(world, state);
				iterator.remove();
				continue;
			}
			if (currentTick > state.expiresTick) {
				detonateBurningPassionCinderMark(server, state, BURNING_PASSION_CONFIG.cinderMark.autoExplodeTrueDamage, false);
				iterator.remove();
				continue;
			}

			syncBurningPassionCinderMarkDisplay(world, target, state);
		}
	}

	static void syncBurningPassionCinderMarkDisplay(
		ServerWorld world,
		LivingEntity target,
		BurningPassionCinderMarkState state
	) {
		Vec3d anchor = new Vec3d(target.getX(), target.getY(), target.getZ()).add(state.anchorOffset);
		if (state.displayEntityId != null) {
			Entity display = world.getEntity(state.displayEntityId);
			if (display != null) {
				display.refreshPositionAndAngles(anchor.x, anchor.y, anchor.z, display.getYaw(), display.getPitch());
			}
		}
		world.spawnParticles(
			ParticleTypes.FLAME,
			anchor.x,
			anchor.y,
			anchor.z,
			Math.max(0, BURNING_PASSION_CONFIG.cinderMark.latchFlameParticleCount),
			0.04,
			0.04,
			0.04,
			0.002
		);
	}

	static void detonateAllCinderMarks(ServerPlayerEntity player, int currentTick, float trueDamage) {
		MinecraftServer server = player.getEntityWorld().getServer();
		boolean detonatedAny = false;
		Iterator<BurningPassionCinderMarkState> iterator = BURNING_PASSION_CINDER_MARKS.iterator();
		while (iterator.hasNext()) {
			BurningPassionCinderMarkState state = iterator.next();
			if (!player.getUuid().equals(state.casterId)) {
				continue;
			}
			detonatedAny |= detonateBurningPassionCinderMark(server, state, trueDamage, true);
			iterator.remove();
		}
		if (detonatedAny) {
			recordOrionsGambitAbilityUse(player, MagicAbility.CINDER_MARK);
		}
	}

	static int countBurningPassionCinderMarks(UUID casterId) {
		int count = 0;
		for (BurningPassionCinderMarkState state : BURNING_PASSION_CINDER_MARKS) {
			if (casterId.equals(state.casterId)) {
				count++;
			}
		}
		return count;
	}

	static void detonateBurningPassionCinderMarksOnTarget(
		ServerPlayerEntity player,
		LivingEntity target,
		float trueDamage
	) {
		MinecraftServer server = player.getEntityWorld().getServer();
		Iterator<BurningPassionCinderMarkState> iterator = BURNING_PASSION_CINDER_MARKS.iterator();
		while (iterator.hasNext()) {
			BurningPassionCinderMarkState state = iterator.next();
			if (!player.getUuid().equals(state.casterId) || !target.getUuid().equals(state.targetId)) {
				continue;
			}
			detonateBurningPassionCinderMark(server, state, trueDamage, true);
			iterator.remove();
		}
	}

	static boolean detonateBurningPassionCinderMark(
		MinecraftServer server,
		BurningPassionCinderMarkState state,
		float trueDamage,
		boolean manualDetonation
	) {
		if (server == null) {
			return false;
		}

		ServerWorld world = server.getWorld(state.dimension);
		if (world == null) {
			return false;
		}

		Vec3d explosionPos;
		Entity entity = world.getEntity(state.targetId);
		if (entity instanceof LivingEntity target && target.isAlive()) {
			explosionPos = new Vec3d(target.getX(), target.getY(), target.getZ()).add(state.anchorOffset);
			dealTrackedMagicDamage(
				target,
				state.casterId,
				createTrueMagicDamageSource(server, world, state.casterId),
				Math.max(0.0F, trueDamage)
			);
		} else {
			explosionPos = entity == null ? Vec3d.ZERO : new Vec3d(entity.getX(), entity.getY(), entity.getZ());
		}

		discardBurningPassionCinderMarkDisplay(world, state);
		int particleCount = manualDetonation
			? BURNING_PASSION_CONFIG.cinderMark.manualDetonationParticleCount
			: BURNING_PASSION_CONFIG.cinderMark.autoExplodeParticleCount;
		world.spawnParticles(
			ParticleTypes.FLAME,
			explosionPos.x,
			explosionPos.y,
			explosionPos.z,
			Math.max(0, particleCount),
			0.18,
			0.18,
			0.18,
			0.04
		);
		world.spawnParticles(ParticleTypes.EXPLOSION, explosionPos.x, explosionPos.y, explosionPos.z, 1, 0.0, 0.0, 0.0, 0.0);
		playConfiguredSound(world, explosionPos, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), BURNING_PASSION_CONFIG.cinderMark.soundVolume, Math.max(0.1F, BURNING_PASSION_CONFIG.cinderMark.soundPitch * 0.9F));
		return true;
	}

	static void discardBurningPassionCinderMarkDisplay(ServerWorld world, BurningPassionCinderMarkState state) {
		if (world == null || state.displayEntityId == null) {
			return;
		}
		Entity display = world.getEntity(state.displayEntityId);
		if (display != null) {
			display.discard();
		}
	}
}

