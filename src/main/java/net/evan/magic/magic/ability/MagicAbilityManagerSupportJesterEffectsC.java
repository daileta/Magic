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


abstract class MagicAbilityManagerSupportJesterEffectsC extends MagicAbilityManagerSupportJesterEffectsB {
	static float horizontalYawFromDirection(Vec3d direction) {
		return MathHelper.wrapDegrees((float) (Math.toDegrees(MathHelper.atan2(direction.z, direction.x)) - 90.0));
	}

	static ComedicRewriteOutcome selectComedicRewriteOutcome(
		ServerPlayerEntity player,
		boolean unsafeScene,
		Vec3d safePos
	) {
		if (unsafeScene && safePos == null) {
			return null;
		}
		if (unsafeScene) {
			return ComedicRewriteOutcome.PARROT_RESCUE;
		}

		int launchWeight = Math.max(0, COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.weight());
		int ravagerWeight = Math.max(0, COMEDIC_REWRITE_RAVAGER_BIT.weight());
		int parrotWeight = safePos != null ? Math.max(0, COMEDIC_REWRITE_PARROT_RESCUE.weight()) : 0;
		int totalWeight = launchWeight + ravagerWeight + parrotWeight;
		if (totalWeight <= 0) {
			return safePos != null ? ComedicRewriteOutcome.PARROT_RESCUE : ComedicRewriteOutcome.LAUNCHED_THROUGH_THE_SCENE;
		}

		int roll = player.getRandom().nextInt(totalWeight);
		if (roll < launchWeight) {
			return ComedicRewriteOutcome.LAUNCHED_THROUGH_THE_SCENE;
		}
		roll -= launchWeight;
		if (roll < ravagerWeight) {
			return ComedicRewriteOutcome.RAVAGER_BIT;
		}
		return ComedicRewriteOutcome.PARROT_RESCUE;
	}

	static double comedicRewriteSeverity(float currentHealth, float finalDamage, boolean lethal) {
		double damageSeverity = COMEDIC_REWRITE_SEVERITY_DAMAGE_CAP <= COMEDIC_REWRITE_DANGEROUS_DAMAGE_THRESHOLD
			? 1.0
			: (finalDamage - COMEDIC_REWRITE_DANGEROUS_DAMAGE_THRESHOLD)
				/ (COMEDIC_REWRITE_SEVERITY_DAMAGE_CAP - COMEDIC_REWRITE_DANGEROUS_DAMAGE_THRESHOLD);
		double fractionSeverity = COMEDIC_REWRITE_DANGEROUS_HEALTH_FRACTION_THRESHOLD >= 1.0
			? 1.0
			: (finalDamage / Math.max(0.0001F, currentHealth) - COMEDIC_REWRITE_DANGEROUS_HEALTH_FRACTION_THRESHOLD)
				/ Math.max(0.0001, 1.0 - COMEDIC_REWRITE_DANGEROUS_HEALTH_FRACTION_THRESHOLD);
		double severity = Math.max(MathHelper.clamp(damageSeverity, 0.0, 1.0), MathHelper.clamp(fractionSeverity, 0.0, 1.0));
		if (lethal) {
			double lethalSeverity = (finalDamage - currentHealth) / Math.max(1.0F, currentHealth);
			severity = Math.max(severity, MathHelper.clamp(0.65 + lethalSeverity, 0.65, 1.0));
		}
		return MathHelper.clamp(severity, 0.0, 1.0);
	}

	static float comedicRewriteSavedHealthPoints(double severity) {
		double hearts = MathHelper.lerp(severity, COMEDIC_REWRITE_MAX_SAVED_HEALTH_HEARTS, COMEDIC_REWRITE_MIN_SAVED_HEALTH_HEARTS);
		return Math.max(1.0F, (float) (MathHelper.clamp(hearts, COMEDIC_REWRITE_MIN_SAVED_HEALTH_HEARTS, COMEDIC_REWRITE_MAX_SAVED_HEALTH_HEARTS) * 2.0));
	}

	static boolean isComedicRewriteUnsafeScene(ServerPlayerEntity player, ServerWorld world, DamageSource source) {
		return source.isOf(DamageTypes.OUT_OF_WORLD)
			|| source.isOf(DamageTypes.IN_WALL)
			|| source.isOf(DamageTypes.LAVA)
			|| source.isOf(DamageTypes.DROWN)
			|| source.isOf(DamageTypes.OUTSIDE_BORDER)
			|| player.isInsideWall()
			|| player.isInLava()
			|| player.getY() <= world.getBottomY() + COMEDIC_REWRITE_UNSAFE_Y_BUFFER_BLOCKS
			|| !world.getWorldBorder().contains(player.getBlockPos());
	}

	static Vec3d findBestComedicRewriteSafePosition(ServerPlayerEntity player, ServerWorld world) {
		Vec3d safePos = findComedicRewriteSafePosition(
			player,
			world,
			COMEDIC_REWRITE_SAFE_SEARCH_RADIUS,
			COMEDIC_REWRITE_SAFE_SEARCH_VERTICAL_RANGE
		);
		if (safePos != null) {
			return safePos;
		}

		safePos = findComedicRewriteSafePosition(
			player,
			world,
			COMEDIC_REWRITE_SAFE_SEARCH_RADIUS * 2,
			COMEDIC_REWRITE_SAFE_SEARCH_VERTICAL_RANGE * 2
		);
		if (safePos != null) {
			return safePos;
		}

		return findComedicRewriteSafePosition(
			player,
			world,
			0,
			Math.max(COMEDIC_REWRITE_SAFE_SEARCH_VERTICAL_RANGE * 4, COMEDIC_REWRITE_UNSAFE_Y_BUFFER_BLOCKS + 16)
		);
	}

	static Vec3d findComedicRewriteSafePosition(
		ServerPlayerEntity player,
		ServerWorld world,
		int radius,
		int verticalRange
	) {
		BlockPos origin = player.getBlockPos();
		int originY = MathHelper.clamp(origin.getY(), world.getBottomY() + 1, world.getTopYInclusive() - 1);

		for (int distance = 0; distance <= radius; distance++) {
			Vec3d sameColumn = findComedicRewriteSafePositionInColumn(player, world, origin.getX(), origin.getZ(), originY, verticalRange);
			if (distance == 0 && sameColumn != null) {
				return sameColumn;
			}
			if (distance == 0) {
				continue;
			}

			for (int dx = -distance; dx <= distance; dx++) {
				Vec3d north = findComedicRewriteSafePositionInColumn(
					player,
					world,
					origin.getX() + dx,
					origin.getZ() - distance,
					originY,
					verticalRange
				);
				if (north != null) {
					return north;
				}

				Vec3d south = findComedicRewriteSafePositionInColumn(
					player,
					world,
					origin.getX() + dx,
					origin.getZ() + distance,
					originY,
					verticalRange
				);
				if (south != null) {
					return south;
				}
			}

			for (int dz = -distance + 1; dz <= distance - 1; dz++) {
				Vec3d west = findComedicRewriteSafePositionInColumn(
					player,
					world,
					origin.getX() - distance,
					origin.getZ() + dz,
					originY,
					verticalRange
				);
				if (west != null) {
					return west;
				}

				Vec3d east = findComedicRewriteSafePositionInColumn(
					player,
					world,
					origin.getX() + distance,
					origin.getZ() + dz,
					originY,
					verticalRange
				);
				if (east != null) {
					return east;
				}
			}
		}

		return null;
	}

	static Vec3d findComedicRewriteSafePositionInColumn(
		ServerPlayerEntity player,
		ServerWorld world,
		int x,
		int z,
		int originY,
		int verticalRange
	) {
		BlockPos chunkCheck = new BlockPos(x, originY, z);
		if (!world.isChunkLoaded(chunkCheck) || !world.getWorldBorder().contains(chunkCheck)) {
			return null;
		}

		int maxY = Math.min(world.getTopYInclusive() - 1, originY + verticalRange);
		int minY = Math.max(world.getBottomY() + 1, originY - verticalRange);
		for (int y = maxY; y >= minY; y--) {
			BlockPos feetPos = new BlockPos(x, y, z);
			if (isComedicRewriteSafePosition(player, world, feetPos)) {
				return new Vec3d(x + 0.5, y, z + 0.5);
			}
		}

		return null;
	}

	static boolean isComedicRewriteSafePosition(ServerPlayerEntity player, ServerWorld world, BlockPos feetPos) {
		BlockPos belowPos = feetPos.down();
		BlockPos headPos = feetPos.up();
		BlockState belowState = world.getBlockState(belowPos);
		BlockState feetState = world.getBlockState(feetPos);
		BlockState headState = world.getBlockState(headPos);
		if (!belowState.blocksMovement()) {
			return false;
		}
		if (
			isComedicRewriteUnsafeBlock(belowState) ||
			isComedicRewriteUnsafeBlock(feetState) ||
			isComedicRewriteUnsafeBlock(headState)
		) {
			return false;
		}
		if (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()) {
			return false;
		}
		if (feetState.blocksMovement() || headState.blocksMovement()) {
			return false;
		}

		Vec3d targetPos = new Vec3d(feetPos.getX() + 0.5, feetPos.getY(), feetPos.getZ() + 0.5);
		Box targetBox = player.getBoundingBox().offset(targetPos.x - player.getX(), targetPos.y - player.getY(), targetPos.z - player.getZ());
		return world.isSpaceEmpty(player, targetBox) && !world.containsFluid(targetBox);
	}

	static boolean isComedicRewriteUnsafeBlock(BlockState state) {
		return state.isOf(Blocks.LAVA)
			|| state.isOf(Blocks.FIRE)
			|| state.isOf(Blocks.SOUL_FIRE)
			|| state.isOf(Blocks.CAMPFIRE)
			|| state.isOf(Blocks.SOUL_CAMPFIRE)
			|| state.isOf(Blocks.CACTUS)
			|| state.isOf(Blocks.SWEET_BERRY_BUSH)
			|| state.isOf(Blocks.MAGMA_BLOCK);
	}

	static void teleportComedicRewritePlayer(ServerPlayerEntity player, ServerWorld world, Vec3d targetPos) {
		if (player.hasVehicle()) {
			player.stopRiding();
		}

		player.teleport(world, targetPos.x, targetPos.y, targetPos.z, Set.<PositionFlag>of(), player.getYaw(), player.getPitch(), false);
		player.setOnGround(false);
		player.fallDistance = 0.0F;
	}

	static Vec3d resolveComedicRewriteLaunchDirection(ServerPlayerEntity player, DamageSource source) {
		Vec3d sourcePos = source.getPosition();
		if (sourcePos != null) {
			Vec3d direction = new Vec3d(player.getX() - sourcePos.x, 0.0, player.getZ() - sourcePos.z);
			if (direction.lengthSquared() > 1.0E-5) {
				return direction.normalize();
			}
		}

		Vec3d backwards = player.getRotationVector().multiply(-1.0, 0.0, -1.0);
		if (backwards.lengthSquared() > 1.0E-5) {
			return backwards.normalize();
		}
		return randomHorizontalDirection(player);
	}

	static Vec3d normalizedHorizontalDirection(Vec3d direction, ServerPlayerEntity player) {
		if (direction != null) {
			Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z);
			if (horizontal.lengthSquared() > 1.0E-5) {
				return horizontal.normalize();
			}
		}
		if (player != null) {
			return randomHorizontalDirection(player);
		}
		return new Vec3d(1.0, 0.0, 0.0);
	}

	static Vec3d randomHorizontalDirection(ServerPlayerEntity player) {
		double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
		return new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
	}

	static boolean shouldTriggerMartyrsFlameRetaliation(ServerPlayerEntity damagedPlayer) {
		UUID playerId = damagedPlayer.getUuid();
		return MARTYRS_FLAME_PASSIVE_ENABLED.contains(playerId)
			&& MagicPlayerData.getSchool(damagedPlayer) == MagicSchool.BURNING_PASSION
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(playerId, MagicAbility.MARTYRS_FLAME)
			&& (MagicPlayerData.getMana(damagedPlayer) > 0 || isOrionsGambitManaCostSuppressed(damagedPlayer));
	}

	static ServerPlayerEntity attackingPlayerFrom(DamageSource source) {
		Entity attacker = source.getAttacker();
		if (attacker instanceof ServerPlayerEntity attackerPlayer && attackerPlayer.isAlive() && !attackerPlayer.isSpectator()) {
			return attackerPlayer;
		}

		if (source.getSource() instanceof ServerPlayerEntity sourcePlayer && sourcePlayer.isAlive() && !sourcePlayer.isSpectator()) {
			return sourcePlayer;
		}

		return null;
	}

	static void retaliateWithMartyrsFlame(ServerPlayerEntity defender, ServerPlayerEntity attacker, int currentTick) {
		if (MARTYRS_FLAME_RETALIATION_DAMAGE > 0.0F && attacker.getEntityWorld() instanceof ServerWorld world) {
			dealTrackedMagicDamage(attacker, defender.getUuid(), world.getDamageSources().genericKill(), MARTYRS_FLAME_RETALIATION_DAMAGE);
		}

		applyMartyrsFlameFire(attacker, defender.getUuid(), currentTick);
	}

	static void activateTillDeathDoUsPartLink(ServerPlayerEntity caster, ServerPlayerEntity linkedPlayer, int currentTick) {
		UUID casterId = caster.getUuid();
		if (!TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.contains(casterId) || TILL_DEATH_DO_US_PART_STATES.containsKey(casterId)) {
			return;
		}

		MagicAbility currentAbility = activeAbility(caster);
		DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.get(casterId);
		if (ownedDomain != null && ownedDomain.ability != MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return;
		}

		float averagedHealth = (float) Math.ceil((caster.getHealth() + linkedPlayer.getHealth()) / 2.0F);
		float maxSharedHealth = Math.min(caster.getMaxHealth(), linkedPlayer.getMaxHealth());
		float sharedHealth = MathHelper.clamp(averagedHealth, 0.0F, maxSharedHealth);

		if (currentAbility == MagicAbility.MANIPULATION) {
			deactivateManipulation(caster, true, "interrupted by Till Death Do Us Part");
		}

		if (currentAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			deactivateLoveAtFirstSight(caster);
		}

		TILL_DEATH_DO_US_PART_STATES.put(
			casterId,
			new TillDeathDoUsPartState(currentTick + TILL_DEATH_DO_US_PART_LINK_DURATION_TICKS, linkedPlayer.getUuid(), sharedHealth)
		);
		setActiveAbility(caster, MagicAbility.TILL_DEATH_DO_US_PART);
		MagicPlayerData.setDepletedRecoveryMode(caster, false);
		caster.setHealth(sharedHealth);
		linkedPlayer.setHealth(sharedHealth);
		caster.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.TILL_DEATH_DO_US_PART.displayName()), true);
		recordOrionsGambitAbilityUse(caster, MagicAbility.TILL_DEATH_DO_US_PART);
	}

	static void syncTillDeathDoUsPartHealth(
		ServerPlayerEntity caster,
		ServerPlayerEntity linkedPlayer,
		TillDeathDoUsPartState state
	) {
		float maxSharedHealth = Math.min(caster.getMaxHealth(), linkedPlayer.getMaxHealth());
		float sharedHealth = MathHelper.clamp(state.sharedHealth, 0.0F, maxSharedHealth);
		float casterHealth = caster.getHealth();
		float linkedHealth = linkedPlayer.getHealth();
		float epsilon = 0.001F;

		if (casterHealth < sharedHealth - epsilon || linkedHealth < sharedHealth - epsilon) {
			sharedHealth = Math.min(casterHealth, linkedHealth);
		} else if (casterHealth > sharedHealth + epsilon || linkedHealth > sharedHealth + epsilon) {
			sharedHealth = Math.max(casterHealth, linkedHealth);
		}

		sharedHealth = MathHelper.clamp(sharedHealth, 0.0F, maxSharedHealth);
		state.sharedHealth = sharedHealth;

		if (Math.abs(casterHealth - sharedHealth) > epsilon) {
			caster.setHealth(sharedHealth);
		}
		if (Math.abs(linkedHealth - sharedHealth) > epsilon) {
			linkedPlayer.setHealth(sharedHealth);
		}
	}

	static double manaFromPercentExact(double percent) {
		double normalizedPercent = MathHelper.clamp(percent, 0.0, 100.0);
		return MagicPlayerData.MAX_MANA * (normalizedPercent / 100.0);
	}

	static SpotlightStageSettings spotlightStageSettings(MagicConfig.JesterSpotlightStageConfig config) {
		return SpotlightStageSettings.defaults(
			Math.max(1, config.viewersRequired),
			Math.max(1, config.activationWindowTicks),
			Math.max(0, config.downgradeGraceTicks),
			Math.max(1, config.fallbackWindowTicks),
			Math.max(0.0, config.attackDamageBonus),
			Math.max(0.0, config.movementSpeedMultiplier),
			Math.max(1.0, config.scale),
			Math.max(-1, config.jumpBoostAmplifier),
			Math.max(-1, config.resistanceAmplifier),
			Math.max(0.0, config.maxHealthBonusHearts)
		);
	}

	static WittyOneLinerTierSettings wittyOneLinerTierSettings(
		MagicConfig.JesterWittyOneLinerTierConfig config,
		int fallbackColor
	) {
		List<String> jokes = new ArrayList<>(config.jokes);
		jokes.removeIf(joke -> joke == null || joke.isBlank());
		if (jokes.isEmpty()) {
			jokes = List.of(MagicAbility.WITTY_ONE_LINER.displayName().getString());
		}

		return WittyOneLinerTierSettings.defaults(
			Math.max(0, config.selectionWeight),
			Math.max(0, config.cooldownTicks),
			Math.max(0, config.effectDurationTicks),
			parseColorRgb(config.textColorHex, fallbackColor),
			Math.max(-1, config.slownessAmplifier),
			Math.max(-1, config.weaknessAmplifier),
			Math.max(-1, config.blindnessAmplifier),
			Math.max(-1, config.nauseaAmplifier),
			Math.max(-1, config.darknessAmplifier),
			Math.max(-1, config.weavingAmplifier),
			config.applyGlowing,
			List.copyOf(jokes)
		);
	}

	static ComedicRewriteLaunchSettings comedicRewriteLaunchSettings(MagicConfig.JesterComedicRewriteLaunchConfig config) {
		return ComedicRewriteLaunchSettings.defaults(
			Math.max(0, config.weight),
			Math.max(0.0, config.baseHorizontalVelocity),
			Math.max(0.0, config.horizontalVelocityPerSeverity),
			Math.max(0.0, config.baseVerticalVelocity),
			Math.max(0.0, config.verticalVelocityPerSeverity),
			Math.max(0, config.slownessDurationTicks),
			Math.max(-1, config.slownessAmplifier),
			Math.max(0, config.smokeParticleCount),
			Math.max(0, config.poofParticleCount)
		);
	}

	static ComedicRewriteRavagerSettings comedicRewriteRavagerSettings(MagicConfig.JesterComedicRewriteRavagerConfig config) {
		return ComedicRewriteRavagerSettings.defaults(
			Math.max(0, config.weight),
			Math.max(0.0, config.baseHorizontalVelocity),
			Math.max(0.0, config.horizontalVelocityPerSeverity),
			Math.max(0.0, config.baseVerticalVelocity),
			Math.max(0.0, config.verticalVelocityPerSeverity),
			Math.max(0, config.slownessDurationTicks),
			Math.max(-1, config.slownessAmplifier),
			Math.max(0, config.smokeParticleCount),
			Math.max(0, config.dustParticleCount),
			config.showVisualCameo,
			Math.max(0, config.visualDurationTicks),
			Math.max(0.0, config.visualSpawnDistance),
			config.visualVerticalOffset,
			Math.max(0.0, config.visualChargeVelocity),
			Math.max(0.0, config.visualChargeVelocityBuffer),
			Math.max(0, config.visualChargeDurationTicks)
		);
	}

	static ComedicRewriteParrotSettings comedicRewriteParrotSettings(MagicConfig.JesterComedicRewriteParrotConfig config) {
		return ComedicRewriteParrotSettings.defaults(
			Math.max(0, config.weight),
			Math.max(0.0, config.carryHeight),
			Math.max(0.0, config.carryHeightPerSeverity),
			Math.max(0.0, config.sideVelocity),
			Math.max(0.0, config.sideVelocityPerSeverity),
			Math.max(0, config.slowFallingDurationTicks),
			Math.max(0, config.levitationDurationTicks),
			Math.max(0, config.featherParticleCount),
			config.showVisualCameo,
			Math.max(0, config.visualDurationTicks),
			Math.max(0, config.visualCount),
			Math.max(0.0, config.visualRadius),
			config.visualVerticalOffset,
			config.visualFollowPlayerHead,
			Math.max(0.0, config.visualLiftVelocity)
		);
	}

	static ComedicAssistantSlimeSettings comedicAssistantSlimeSettings(MagicConfig.JesterComedicAssistantSlimeConfig config) {
		return ComedicAssistantSlimeSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0, config.slownessDurationTicks),
			Math.max(-1, config.slownessAmplifier),
			Math.max(0, config.oozingDurationTicks),
			Math.max(-1, config.oozingAmplifier),
			Math.max(0, config.visualDurationTicks),
			Math.max(0.0, config.visualSpawnHeight),
			Math.max(0.0, config.visualFallSpeed),
			MathHelper.clamp(config.visualSize, 1, 127),
			Math.max(0, config.particleCount),
			Math.max(0.0F, config.spawnSoundVolume),
			Math.max(0.0F, config.spawnSoundPitch),
			Math.max(0.0F, config.impactSoundVolume),
			Math.max(0.0F, config.impactSoundPitch)
		);
	}

	static ComedicAssistantPandaSettings comedicAssistantPandaSettings(MagicConfig.JesterComedicAssistantPandaConfig config) {
		return ComedicAssistantPandaSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0.0, config.horizontalLaunch),
			Math.max(0.0, config.verticalLaunch),
			Math.max(0, config.slownessDurationTicks),
			Math.max(-1, config.slownessAmplifier),
			Math.max(0, config.visualDurationTicks),
			Math.max(0.0, config.visualSpawnDistance),
			Math.max(0.0, config.visualChargeVelocity),
			Math.max(0, config.particleCount),
			Math.max(0.0F, config.rollSoundVolume),
			Math.max(0.0F, config.rollSoundPitch),
			Math.max(0.0F, config.impactSoundVolume),
			Math.max(0.0F, config.impactSoundPitch)
		);
	}

	static ComedicAssistantParrotSettings comedicAssistantParrotSettings(MagicConfig.JesterComedicAssistantParrotConfig config) {
		return ComedicAssistantParrotSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0.0, config.liftHeight),
			Math.max(0.0, config.upwardVelocity),
			Math.max(0, config.maxCarryTicks),
			Math.max(0.0, config.releaseDownwardVelocity),
			config.applyGlowing,
			Math.max(0, config.glowingDurationTicks),
			Math.max(0, config.visualDurationTicks),
			Math.max(0, config.visualCount),
			Math.max(0.0, config.visualRadius),
			config.visualVerticalOffset,
			Math.max(0, config.particleCount),
			Math.max(1, config.flapSoundIntervalTicks),
			Math.max(0.0F, config.soundVolume),
			Math.max(0.0F, config.soundPitch)
		);
	}

	static ComedicAssistantDivineSettings comedicAssistantDivineSettings(MagicConfig.JesterComedicAssistantDivineConfig config) {
		return ComedicAssistantDivineSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0, config.strikeCount),
			Math.max(0.0, config.strikeRadius),
			Math.max(0, config.glowingDurationTicks),
			Math.max(0, config.blindnessDurationTicks),
			Math.max(-1, config.blindnessAmplifier),
			Math.max(0, config.nauseaDurationTicks),
			Math.max(-1, config.nauseaAmplifier),
			Math.max(0, config.particleCount),
			Math.max(0.0F, config.soundVolume),
			Math.max(0.0F, config.soundPitch)
		);
	}

	static ComedicAssistantAcmeSettings comedicAssistantAcmeSettings(MagicConfig.JesterComedicAssistantAcmeConfig config) {
		return ComedicAssistantAcmeSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0, config.slownessDurationTicks),
			Math.max(-1, config.slownessAmplifier),
			Math.max(0, config.weaknessDurationTicks),
			Math.max(-1, config.weaknessAmplifier),
			Math.max(0, config.visualDurationTicks),
			Math.max(0.0, config.visualDropHeight),
			Math.max(0, config.particleCount),
			Math.max(0.0F, config.soundVolume),
			Math.max(0.0F, config.soundPitch)
		);
	}

	static ComedicAssistantPieSettings comedicAssistantPieSettings(MagicConfig.JesterComedicAssistantPieConfig config) {
		return ComedicAssistantPieSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0, config.blindnessDurationTicks),
			Math.max(-1, config.blindnessAmplifier),
			Math.max(0, config.nauseaDurationTicks),
			Math.max(-1, config.nauseaAmplifier),
			Math.max(0, config.particleCount),
			Math.max(0.0F, config.soundVolume),
			Math.max(0.0F, config.soundPitch)
		);
	}

	static ComedicAssistantCaneSettings comedicAssistantCaneSettings(MagicConfig.JesterComedicAssistantCaneConfig config) {
		return ComedicAssistantCaneSettings.defaults(
			config.enabled,
			Math.max(0, config.weight),
			Math.max(0.0F, config.bonusDamage),
			Math.max(0.0, config.horizontalLaunch),
			Math.max(0.0, config.verticalLaunch),
			Math.max(0, config.launchControlTicks),
			Math.max(0, config.velocityDamageTrackingTicks),
			Math.max(0.0, config.velocityDamageThreshold),
			Math.max(0.0, config.velocityDamageMultiplier),
			Math.max(0.0, config.velocityDamageMax),
			Math.max(0, config.slownessDurationTicks),
			Math.max(-1, config.slownessAmplifier),
			Math.max(0, config.visualDurationTicks),
			Math.max(0.0, config.visualSpawnDistance),
			Math.max(0.0, config.visualChargeVelocity),
			Math.max(0, config.particleCount),
			Math.max(0.0F, config.soundVolume),
			Math.max(0.0F, config.soundPitch)
		);
	}

	static int parseColorRgb(String rawColor, int fallbackColor) {
		return parseColorRgb(rawColor, fallbackColor, "configured");
	}

	static int parseColorRgb(String rawColor, int fallbackColor, String colorContext) {
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

		try {
			return Integer.parseInt(normalized, 16) & 0x00FFFFFF;
		} catch (NumberFormatException exception) {
			Magic.LOGGER.warn("Invalid {} color '{}'; using fallback {}.", colorContext, rawColor, fallbackColor);
			return fallbackColor;
		}
	}

	static List<Integer> parseColorRgbList(List<String> rawColors, List<Integer> fallbackColors, String colorContext) {
		if (rawColors == null || rawColors.isEmpty()) {
			return List.copyOf(fallbackColors);
		}

		List<Integer> parsedColors = new ArrayList<>();
		for (String rawColor : rawColors) {
			if (rawColor == null || rawColor.isBlank()) {
				continue;
			}
			parsedColors.add(parseColorRgb(rawColor, fallbackColors.get(parsedColors.size() % fallbackColors.size()), colorContext));
		}

		if (parsedColors.isEmpty()) {
			return List.copyOf(fallbackColors);
		}
		return List.copyOf(parsedColors);
	}

	static Identifier parseIdentifierOrFallback(String rawId, Identifier fallbackId) {
		if (rawId == null || rawId.isBlank()) {
			return fallbackId;
		}

		Identifier parsed = Identifier.tryParse(rawId.trim());
		if (parsed == null) {
			Magic.LOGGER.warn("Invalid identifier '{}'; using fallback {}.", rawId, fallbackId);
			return fallbackId;
		}

		return parsed;
	}
}

