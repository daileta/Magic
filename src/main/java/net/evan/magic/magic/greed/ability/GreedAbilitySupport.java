package net.evan.magic.magic.ability;
import net.evan.magic.magic.core.ability.MagicAbility;

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
import net.evan.magic.magic.core.MagicPlayerData;
import net.evan.magic.magic.core.MagicSchool;
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

abstract class GreedAbilitySupport extends GreedAbilityState {
	static void applyAttackSpeedModifier(PlayerEntity player, double amount) {
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

	static void removeAttackSpeedModifier(PlayerEntity player) {
		EntityAttributeInstance attributeInstance = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
		if (attributeInstance != null) {
			attributeInstance.removeModifier(KINGS_DUES_ATTACK_SPEED_MODIFIER_ID);
		}
	}

	static void applyKingsDuesVisuals(ServerPlayerEntity target, int spend) {
		ServerWorld world = (ServerWorld) target.getEntityWorld();
		world.spawnParticles(GOLD_NUGGET_PARTICLE, target.getX(), target.getBodyY(0.65), target.getZ(), 12, 0.25, 0.35, 0.25, 0.04);
		world.spawnParticles(ParticleTypes.GLOW, target.getX(), target.getBodyY(0.65), target.getZ(), 8, 0.3, 0.4, 0.3, 0.0);
		if (spend >= 4) {
			spawnChainParticles(target);
		}
		world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 0.7F, 1.25F);
	}

	static void applyBankruptcyVisuals(ServerPlayerEntity target) {
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

	static void applyStageEffect(ServerPlayerEntity player, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int durationTicks, int amplifier) {
		if (durationTicks <= 0 || amplifier < 0) {
			return;
		}

		player.addStatusEffect(new StatusEffectInstance(effect, durationTicks, amplifier, true, true, true));
	}

	static void endAppraisersMark(ServerPlayerEntity player, int currentTick, boolean startCooldownIfMarked) {
		AppraisersMarkState state = APPRAISERS_MARK_STATES.remove(player.getUuid());
		if (state == null) {
			return;
		}

		if (startCooldownIfMarked && state.stage == AppraisersMarkStage.MARKED) {
			startAbilityCooldownFromNow(player.getUuid(), MagicAbility.APPRAISERS_MARK, currentTick);
		}
	}

	static void clearCoins(ServerPlayerEntity player) {
		GREED_COIN_STORAGES.remove(player.getUuid());
		MagicPlayerData.setGreedCoinUnits(player, 0);
	}

	static int spendAmount(ServerPlayerEntity player, int minimumCoins, int maximumCoins) {
		GreedCoinStorage storage = GREED_COIN_STORAGES.get(player.getUuid());
		if (storage == null) {
			return 0;
		}

		int adjustedMinimumCoins = MagicAbilityManager.adjustCelestialAlignmentGreedCoinCost(player, minimumCoins);
		int adjustedMaximumCoins = Math.max(adjustedMinimumCoins, MagicAbilityManager.adjustCelestialAlignmentGreedCoinCost(player, maximumCoins));
		int available = availableWholeCoins(storage);
		if (available < adjustedMinimumCoins) {
			return 0;
		}

		return Math.min(available, adjustedMaximumCoins);
	}

	static void addCoinUnits(ServerPlayerEntity caster, UUID sourcePlayerId, int coinUnits, int currentTick) {
		addCoinUnits(caster, sourcePlayerId, coinUnits, currentTick, false);
	}

	static void addCoinUnits(ServerPlayerEntity caster, UUID sourcePlayerId, int coinUnits, int currentTick, boolean allowOverflowBeyondCap) {
		if (coinUnits <= 0) {
			return;
		}

		GreedCoinStorage storage = GREED_COIN_STORAGES.computeIfAbsent(caster.getUuid(), ignored -> new GreedCoinStorage());
		expireContributions(storage, currentTick);
		if (allowOverflowBeyondCap) {
			storage.temporaryOverflowCoinUnits += coinUnits;
			MagicPlayerData.setGreedCoinUnits(caster, totalCoinUnits(storage));
			if (MagicConfig.get().greed.appraisersMark.playCoinGainSound) {
				caster.playSound(
					SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
					MagicConfig.get().greed.appraisersMark.coinGainSoundVolume,
					MagicConfig.get().greed.appraisersMark.coinGainSoundPitch
				);
			}
			return;
		}
		boolean commandContribution = COMMAND_COIN_SOURCE_ID.equals(sourcePlayerId);
		if (!commandContribution && !storage.contributions.containsKey(sourcePlayerId)) {
			while (trackedPlayerContributionCount(caster.getUuid(), storage) >= MagicConfig.get().greed.appraisersMark.maxTrackedPlayers) {
				if (!removeOldestTrackedPlayerContribution(caster.getUuid(), storage)) {
					break;
				}
			}
		}

		int availableRoom = Math.max(0, coinUnitsFromConfiguredCoins(MagicConfig.get().greed.appraisersMark.maxCoins) - pouchCoinUnits(storage));
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

	static void removeCoins(ServerPlayerEntity caster, int coinsToSpend) {
		int coinUnitsToSpend = coinUnitsFromWholeCoins(coinsToSpend);
		GreedCoinStorage storage = GREED_COIN_STORAGES.get(caster.getUuid());
		if (storage == null || coinUnitsToSpend <= 0) {
			return;
		}

		int remaining = coinUnitsToSpend;
		if (storage.temporaryOverflowCoinUnits > 0) {
			int overflowSpent = Math.min(storage.temporaryOverflowCoinUnits, remaining);
			storage.temporaryOverflowCoinUnits -= overflowSpent;
			remaining -= overflowSpent;
		}
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

		if (pouchCoinUnits(storage) <= 0 && storage.temporaryOverflowCoinUnits <= 0) {
			GREED_COIN_STORAGES.remove(caster.getUuid());
			MagicPlayerData.setGreedCoinUnits(caster, 0);
			return;
		}

		MagicPlayerData.setGreedCoinUnits(caster, totalCoinUnits(storage));
	}

	static int totalCoinUnits(GreedCoinStorage storage) {
		int total = storage.temporaryOverflowCoinUnits;
		for (GreedCoinContribution contribution : storage.contributions.values()) {
			total += contribution.coinUnits;
		}
		return total;
	}

	static int pouchCoinUnits(GreedCoinStorage storage) {
		int total = 0;
		for (GreedCoinContribution contribution : storage.contributions.values()) {
			total += contribution.coinUnits;
		}
		return total;
	}

	static int availableWholeCoins(GreedCoinStorage storage) {
		return totalCoinUnits(storage) / COIN_UNITS_PER_COIN;
	}

	static void expireContributions(GreedCoinStorage storage, int currentTick) {
		Iterator<Map.Entry<UUID, GreedCoinContribution>> iterator = storage.contributions.entrySet().iterator();
		while (iterator.hasNext()) {
			if (currentTick >= iterator.next().getValue().expiresTick) {
				iterator.remove();
			}
		}
	}

	static int trackedPlayerContributionCount(UUID casterId, GreedCoinStorage storage) {
		int count = 0;
		for (UUID contributorId : storage.contributions.keySet()) {
			if (!COMMAND_COIN_SOURCE_ID.equals(contributorId) && GreedDomainRuntime.countsTowardUniqueTargetCap(casterId, contributorId)) {
				count++;
			}
		}
		return count;
	}

	static boolean removeOldestTrackedPlayerContribution(UUID casterId, GreedCoinStorage storage) {
		Iterator<Map.Entry<UUID, GreedCoinContribution>> iterator = storage.contributions.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, GreedCoinContribution> entry = iterator.next();
			if (COMMAND_COIN_SOURCE_ID.equals(entry.getKey()) || !GreedDomainRuntime.countsTowardUniqueTargetCap(casterId, entry.getKey())) {
				continue;
			}
			iterator.remove();
			return true;
		}
		return false;
	}

	static void spawnMarkedParticles(ServerWorld world, ServerPlayerEntity target) {
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

	static void spawnZoneParticles(ServerWorld world, TollkeepersClaimZoneState zone, int currentTick) {
		MagicConfig.TollkeepersClaimConfig config = MagicConfig.get().greed.tollkeepersClaim;
		double spinSpeed = Math.toRadians(config.vortexSpinDegreesPerTick);
		double twistPerBlock = Math.toRadians(config.vortexTwistDegreesPerBlock);
		double baseAngle = spinSpeed * currentTick;
		for (int armIndex = 0; armIndex < TOLLKEEPERS_CLAIM_VORTEX_ARMS; armIndex++) {
			double angle = baseAngle + Math.PI * 2.0 * armIndex / TOLLKEEPERS_CLAIM_VORTEX_ARMS;
			double particleX = zone.centerX + Math.cos(angle) * zone.radius;
			double particleZ = zone.centerZ + Math.sin(angle) * zone.radius;
			world.spawnParticles(
				new TollkeepersClaimVortexParticleEffect(
					zone.centerX,
					zone.centerY,
					zone.centerZ,
					0.0,
					angle,
					spinSpeed,
					twistPerBlock,
					zone.radius,
					config.vortexStraightHeight,
					config.vortexTotalHeight,
					config.vortexOutwardCurve,
					config.vortexParticleLifetimeTicks,
					config.vortexParticleScale
				),
				particleX,
				zone.centerY,
				particleZ,
				1,
				0.0,
				0.0,
				0.0,
				0.0
			);
		}
	}

	static void spawnChainParticles(ServerPlayerEntity player) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		world.spawnParticles(GOLD_NUGGET_PARTICLE, player.getX(), player.getBodyY(0.65), player.getZ(), 8, 0.22, 0.3, 0.22, 0.03);
		world.spawnParticles(ParticleTypes.GLOW, player.getX(), player.getBodyY(0.65), player.getZ(), 6, 0.25, 0.32, 0.25, 0.0);
	}

	static void playChainSound(ServerPlayerEntity player) {
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 0.5F, 1.15F);
	}

	static double manaFromPercentExact(double percent) {
		return MagicPlayerData.MAX_MANA * MathHelper.clamp(percent, 0.0, 100.0) / 100.0;
	}

	static double appraisersMarkDrainExact(int intervalTicks) {
		double percentPerSecond = MagicConfig.get().greed.appraisersMark.markedDrainPercentPerSecond;
		return manaFromPercentExact(percentPerSecond) * intervalTicks / TICKS_PER_SECOND;
	}

	static int appraisersMarkManaDrain(ServerPlayerEntity player, int intervalTicks) {
		AppraisersMarkState state = APPRAISERS_MARK_STATES.get(player.getUuid());
		if (state == null || state.stage != AppraisersMarkStage.MARKED) {
			return 0;
		}

		state.drainBuffer += appraisersMarkDrainExact(intervalTicks);
		int drain = Math.max(0, (int) Math.floor(state.drainBuffer + 1.0E-7));
		state.drainBuffer = Math.max(0.0, state.drainBuffer - drain);
		return drain;
	}

	static double manaSicknessDrainExact(PlayerEntity player, int intervalTicks) {
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

	static void sendCooldownMessage(ServerPlayerEntity player, MagicAbility ability, int remainingTicks) {
		int secondsRemaining = (int) Math.ceil(remainingTicks / 20.0);
		player.sendMessage(Text.translatable("message.magic.ability.cooldown", ability.displayName(), secondsRemaining), true);
	}

	static BlockHitResult findTargetedBlock(ServerPlayerEntity caster, double range) {
		Vec3d eyePos = caster.getEyePos();
		Vec3d look = caster.getRotationVec(1.0F);
		Vec3d end = eyePos.add(look.multiply(Math.max(1.0, range)));
		BlockHitResult hitResult = caster.getEntityWorld().raycast(
			new RaycastContext(eyePos, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, caster)
		);
		return hitResult.getType() == HitResult.Type.BLOCK ? hitResult : null;
	}

	static ServerPlayerEntity findPlayerTargetInLineOfSight(ServerPlayerEntity caster, double range, boolean requireLineOfSight) {
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

	static boolean isInsideZone(TollkeepersClaimZoneState zone, ServerPlayerEntity player) {
		double dx = player.getX() - zone.centerX;
		double dz = player.getZ() - zone.centerZ;
		return dx * dx + dz * dz <= zone.radius * zone.radius && Math.abs(player.getY() - zone.centerY) <= 3.0;
	}

	static double tollkeepersZoneRadius(int coinsSpent) {
		MagicConfig.TollkeepersClaimConfig config = MagicConfig.get().greed.tollkeepersClaim;
		MagicConfig.TollkeepersClaimStageConfig stage = tollkeepersStage(coinsSpent);
		return Math.max(0.5, config.zoneRadius + Math.max(0.0, stage.radiusBonusBlocks));
	}

	static boolean isMarkedTarget(ServerPlayerEntity caster, ServerPlayerEntity target) {
		AppraisersMarkState state = APPRAISERS_MARK_STATES.get(caster.getUuid());
		return state != null && state.stage == AppraisersMarkStage.MARKED && target.getUuid().equals(state.markedTargetId);
	}

	static int coinUnitsFromWholeCoins(int coins) {
		return Math.max(0, coins) * COIN_UNITS_PER_COIN;
	}

	static int coinUnitsFromConfiguredCoins(double coins) {
		return Math.max(0, (int) Math.round(Math.max(0.0, coins) * COIN_UNITS_PER_COIN));
	}

	static boolean isFallingMaceAttack(DamageSource source) {
		return source.isOf(DamageTypes.MACE_SMASH) || source.isIn(DamageTypeTags.MACE_SMASH);
	}

	static boolean isFireworkShot(DamageSource source) {
		return source.isOf(DamageTypes.FIREWORKS) && source.getSource() instanceof FireworkRocketEntity firework && firework.wasShotAtAngle();
	}

	static boolean isTippedArrowShot(DamageSource source) {
		return source.isOf(DamageTypes.ARROW) && source.getSource() instanceof ArrowEntity arrow && arrow.getColor() != -1;
	}

	static boolean isSpearChargeAttack(ServerPlayerEntity attacker, DamageSource source) {
		return source.isOf(DamageTypes.TRIDENT) && attacker.isUsingRiptide();
	}

	static boolean isSpearLunge(DamageSource source) {
		return source.isOf(DamageTypes.TRIDENT);
	}

	static boolean isFullChargeBowShot(DamageSource source) {
		if (!source.isOf(DamageTypes.ARROW) || !(source.getSource() instanceof PersistentProjectileEntity projectile)) {
			return false;
		}

		ItemStack weaponStack = projectile.getWeaponStack();
		return weaponStack != null && !weaponStack.isEmpty() && weaponStack.getItem() instanceof BowItem && projectile.isCritical();
	}

	static ServerPlayerEntity attackingPlayerFrom(DamageSource source) {
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

	static MagicConfig.TollkeepersClaimStageConfig tollkeepersStage(int coinsSpent) {
		MagicConfig.TollkeepersClaimConfig config = MagicConfig.get().greed.tollkeepersClaim;
		return switch (MathHelper.clamp(coinsSpent, 1, 5)) {
			case 1 -> config.stageOne;
			case 2 -> config.stageTwo;
			case 3 -> config.stageThree;
			case 4 -> config.stageFour;
			default -> config.stageFive;
		};
	}

	static MagicConfig.KingsDuesStageConfig kingsDuesStage(int coinsSpent) {
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

	static MagicConfig.BankruptcyStageConfig bankruptcyStage(int coinsSpent) {
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

	static void startCooldown(UUID playerId, Map<UUID, Integer> cooldownMap, int cooldownTicks, int currentTick) {
		if (cooldownTicks > 0) {
			cooldownMap.put(playerId, currentTick + cooldownTicks);
		}
	}

	static void startAbilityCooldownFromNow(UUID playerId, MagicAbility ability, int currentTick) {
		if (ability == MagicAbility.APPRAISERS_MARK) {
			startCooldown(
				playerId,
				APPRAISERS_MARK_COOLDOWN_END_TICK,
				GreedDomainRuntime.adjustCooldownTicks(playerId, ability, MagicConfig.get().greed.appraisersMark.cooldownTicks, currentTick),
				currentTick
			);
			return;
		}
		if (ability == MagicAbility.KINGS_DUES) {
			startCooldown(
				playerId,
				KINGS_DUES_COOLDOWN_END_TICK,
				GreedDomainRuntime.adjustCooldownTicks(playerId, ability, MagicConfig.get().greed.kingsDues.cooldownTicks, currentTick),
				currentTick
			);
			return;
		}
		if (ability == MagicAbility.BANKRUPTCY) {
			startCooldown(
				playerId,
				BANKRUPTCY_COOLDOWN_END_TICK,
				GreedDomainRuntime.adjustCooldownTicks(playerId, ability, MagicConfig.get().greed.bankruptcy.cooldownTicks, currentTick),
				currentTick
			);
		}
	}

	static int removeTollkeeperZones(UUID casterId) {
		int removed = 0;
		Iterator<Map.Entry<UUID, TollkeepersClaimZoneState>> iterator = TOLLKEEPER_ZONES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, TollkeepersClaimZoneState> entry = iterator.next();
			if (!casterId.equals(entry.getValue().casterId)) {
				continue;
			}

			iterator.remove();
			removed++;
		}
		return removed;
	}

	static void trimOldestTollkeeperZones(UUID casterId, int maxRemainingZones) {
		int allowedZones = Math.max(0, maxRemainingZones);
		while (countTollkeeperZones(casterId) > allowedZones) {
			UUID oldestZoneId = null;
			int oldestCreatedTick = Integer.MAX_VALUE;
			for (Map.Entry<UUID, TollkeepersClaimZoneState> entry : TOLLKEEPER_ZONES.entrySet()) {
				TollkeepersClaimZoneState zone = entry.getValue();
				if (!casterId.equals(zone.casterId) || zone.createdTick >= oldestCreatedTick) {
					continue;
				}

				oldestZoneId = entry.getKey();
				oldestCreatedTick = zone.createdTick;
			}

			if (oldestZoneId == null) {
				return;
			}
			TOLLKEEPER_ZONES.remove(oldestZoneId);
		}
	}

	static int countTollkeeperZones(UUID casterId) {
		int count = 0;
		for (TollkeepersClaimZoneState zone : TOLLKEEPER_ZONES.values()) {
			if (casterId.equals(zone.casterId)) {
				count++;
			}
		}
		return count;
	}
}



