package net.evan.magic.magic.ability;
import net.evan.magic.magic.core.ability.MagicAbility;
import net.evan.magic.magic.ability.GreedAbilityRuntime;
import net.evan.magic.magic.ability.GreedDomainRuntime;

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
import net.evan.magic.magic.core.MagicPlayerData;
import net.evan.magic.magic.core.MagicSchool;
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


abstract class DomainExpansionService extends LoveAbilityRequests {
	static void handleDomainExpansionRequest(ServerPlayerEntity player, MagicAbility requestedAbility, int currentTick) {
		MagicAbility activeAbility = activeAbility(player);
		if (isDomainExpansion(activeAbility)) {
			deactivateDomainExpansion(player);
			setActiveAbility(player, MagicAbility.NONE);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", activeAbility.displayName()), true);
			return;
		}

		int remainingTicks = domainCooldownRemaining(player, requestedAbility, currentTick);
		if (remainingTicks > 0) {
			int secondsRemaining = (int) Math.ceil(remainingTicks / 20.0);
			player.sendMessage(
				Text.translatable("message.magic.ability.cooldown", requestedAbility.displayName(), secondsRemaining),
				true
			);
			return;
		}

		int mana = MagicPlayerData.getMana(player);
		if (!canSpendAbilityCost(player, DOMAIN_EXPANSION_ACTIVATION_MANA_COST)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		DomainOwnerState containingDomain = findContainingForeignDomain(player);
		if (containingDomain != null) {
			if (DOMAIN_CLASHES_BY_OWNER.containsKey(containingDomain.ownerId)) {
				player.sendMessage(Text.translatable("message.magic.domain.clash.active"), true);
				return;
			}

			if (containingDomain.state.clashOccurred) {
				player.sendMessage(Text.translatable("message.magic.domain.clash.used"), true);
				return;
			}

			if (!DOMAIN_CLASH_ENABLED) {
				player.sendMessage(Text.translatable("message.magic.domain.too_close", DOMAIN_CLASH_MIN_EXTERIOR_DISTANCE), true);
				return;
			}

			int ticksSinceActivation = Math.max(0, currentTick - containingDomain.state.activationTick);
			if (ticksSinceActivation <= DOMAIN_CLASH_SIMULTANEOUS_WINDOW_TICKS) {
				resolveSimultaneousDomainCast(player, requestedAbility, currentTick, containingDomain.ownerId, containingDomain.state);
				return;
			}

			boolean preserveFrostStageDuringClash = activeAbility == MagicAbility.BELOW_FREEZING && FROST_STAGE_STATES.containsKey(player.getUuid());
			if (!preserveFrostStageDuringClash) {
				prepareForDomainActivation(player, activeAbility);
			}
			if (startDomainClash(player, requestedAbility, containingDomain.ownerId, containingDomain.state, currentTick)) {
				if (preserveFrostStageDuringClash) {
					suspendFrostStagedModeForDomainClash(player);
				}
				spendAbilityCost(player, DOMAIN_EXPANSION_ACTIVATION_MANA_COST);
				setActiveAbility(player, requestedAbility);
				if (requestedAbility == MagicAbility.GREED_DOMAIN_EXPANSION) {
					GreedAbilityRuntime.recordSuccessfulMagicAbilityUse(player, requestedAbility);
				}
				recordOrionsGambitAbilityUse(player, requestedAbility);
				return;
			}

			player.sendMessage(Text.translatable("message.magic.domain.clash.failed"), true);
			return;
		}

		if (isTooCloseToForeignDomainExterior(player, DOMAIN_CLASH_MIN_EXTERIOR_DISTANCE)) {
			player.sendMessage(Text.translatable("message.magic.domain.too_close", DOMAIN_CLASH_MIN_EXTERIOR_DISTANCE), true);
			return;
		}

		prepareForDomainActivation(player, activeAbility);
		activateDomainExpansion(player, requestedAbility, currentTick);
		spendAbilityCost(player, DOMAIN_EXPANSION_ACTIVATION_MANA_COST);
		setActiveAbility(player, requestedAbility);
		if (requestedAbility == MagicAbility.GREED_DOMAIN_EXPANSION) {
			GreedAbilityRuntime.recordSuccessfulMagicAbilityUse(player, requestedAbility);
		}
		recordOrionsGambitAbilityUse(player, requestedAbility);
		if (requestedAbility == MagicAbility.ASTRAL_CATACLYSM) {
			ASTRAL_CATACLYSM_DOMAIN_STATES.put(player.getUuid(), new ConstellationDomainState(ConstellationDomainPhase.CHARGING, currentTick + ASTRAL_CATACLYSM_CHARGE_DURATION_TICKS));
			player.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.charging"), true);
			return;
		}
		player.sendMessage(Text.translatable("message.magic.ability.activated", requestedAbility.displayName()), true);
	}

	static void prepareForDomainActivation(ServerPlayerEntity player, MagicAbility activeAbility) {
		if (activeAbility == MagicAbility.BELOW_FREEZING) {
			endFrostStagedMode(player, player.getEntityWorld().getServer().getTicks(), FrostStageEndReason.MANUAL, false, false);
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
			deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.MANUAL_CANCEL, false);
		}
		if (activeAbility == MagicAbility.MANIPULATION) {
			deactivateManipulation(player, true, "switched to Domain Expansion");
		}
		if (activeAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			deactivateHerculesBurden(player, true, false);
		}
		if (activeAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			clearSagittariusWindup(player);
		}
		if (activeAbility == MagicAbility.ORIONS_GAMBIT) {
			endOrionsGambit(player, OrionGambitEndReason.MANUAL_CANCEL, player.getEntityWorld().getServer().getTicks(), false);
		}
	}

	static void suspendFrostStagedModeForDomainClash(ServerPlayerEntity player) {
		if (!FROST_STAGE_STATES.containsKey(player.getUuid())) {
			return;
		}
		clearFrostEffectsByCaster(player.getUuid(), false, player.getEntityWorld().getServer());
		removeFrostStageCasterBuffs(player);
		MagicPlayerData.clearFrostStageHud(player);
	}

	static DomainOwnerState findContainingForeignDomain(ServerPlayerEntity player) {
		RegistryKey<World> dimension = player.getEntityWorld().getRegistryKey();
		UUID playerId = player.getUuid();
		for (Map.Entry<UUID, DomainExpansionState> entry : DOMAIN_EXPANSIONS.entrySet()) {
			if (entry.getKey().equals(playerId)) {
				continue;
			}

			DomainExpansionState state = entry.getValue();
			if (!state.dimension.equals(dimension)) {
				continue;
			}

			if (isInsideDomainInterior(player, state)) {
				return new DomainOwnerState(entry.getKey(), state);
			}
		}

		return null;
	}

	static boolean isTooCloseToForeignDomainExterior(ServerPlayerEntity player, int minimumDistance) {
		if (minimumDistance <= 0) {
			return false;
		}

		RegistryKey<World> dimension = player.getEntityWorld().getRegistryKey();
		UUID playerId = player.getUuid();
		for (Map.Entry<UUID, DomainExpansionState> entry : DOMAIN_EXPANSIONS.entrySet()) {
			if (entry.getKey().equals(playerId)) {
				continue;
			}

			DomainExpansionState state = entry.getValue();
			if (!state.dimension.equals(dimension) || isInsideDomainInterior(player, state)) {
				continue;
			}

			Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
			if (distanceToDomainExterior(playerPos, state) <= minimumDistance) {
				return true;
			}
		}

		return false;
	}

	static boolean isInsideDomainInterior(ServerPlayerEntity player, DomainExpansionState state) {
		double dx = player.getX() - state.centerX;
		double dz = player.getZ() - state.centerZ;
		double horizontalDistanceSq = dx * dx + dz * dz;
		double relativeY = player.getY() - state.baseY;
		return isInsideDomainInterior(horizontalDistanceSq, relativeY, state.innerRadius, state.innerHeight);
	}

	static boolean isPositionInsideAnyDomain(RegistryKey<World> dimension, double x, double y, double z) {
		for (DomainExpansionState state : DOMAIN_EXPANSIONS.values()) {
			if (!state.dimension.equals(dimension)) {
				continue;
			}

			double dx = x - state.centerX;
			double dz = z - state.centerZ;
			double horizontalDistanceSq = dx * dx + dz * dz;
			double relativeY = y - state.baseY;
			if (isInsideDomainInterior(horizontalDistanceSq, relativeY, state.innerRadius, state.innerHeight)) {
				return true;
			}
		}

		return false;
	}

	static double distanceToDomainExterior(Vec3d position, DomainExpansionState state) {
		double dx = position.x - state.centerX;
		double dz = position.z - state.centerZ;
		double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
		return Math.abs(horizontalDistance - state.radius);
	}

	static void activateDomainExpansion(ServerPlayerEntity player, MagicAbility ability, int currentTick) {
		deactivateDomainExpansion(player);
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		int radius = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? Math.max(1, FROST_CONFIG.domain.radius)
			: ability == MagicAbility.GREED_DOMAIN_EXPANSION
				? Math.max(1, GreedDomainRuntime.domainRadius())
				: Math.max(1, DOMAIN_EXPANSION_RADIUS);
		int height = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? Math.max(1, FROST_CONFIG.domain.height)
			: ability == MagicAbility.GREED_DOMAIN_EXPANSION
				? Math.max(1, GreedDomainRuntime.domainHeight())
				: Math.max(1, DOMAIN_EXPANSION_HEIGHT);
		int shellThickness = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? Math.max(1, FROST_CONFIG.domain.shellThickness)
			: ability == MagicAbility.GREED_DOMAIN_EXPANSION
				? Math.max(1, GreedDomainRuntime.domainShellThickness())
				: Math.max(1, DOMAIN_EXPANSION_SHELL_THICKNESS);
		int innerRadius = Math.max(0, radius - shellThickness);
		int innerHeight = Math.max(1, height - shellThickness);

		int centerX = MathHelper.floor(player.getX());
		int centerZ = MathHelper.floor(player.getZ());
		int baseY = MathHelper.floor(player.getY()) - 1;
		double centerDx = centerX + 0.5;
		double centerDz = centerZ + 0.5;

		Map<BlockPos, DomainSavedBlockState> savedBlocks = new HashMap<>();
		Map<BlockPos, BlockState> protectedShellStates = new HashMap<>();
		Map<UUID, DomainCapturedEntityState> capturedEntities = captureDomainEntities(world, centerDx, centerDz, baseY, radius, height);
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				int horizontalDistanceSq = dx * dx + dz * dz;
				if (horizontalDistanceSq > radius * radius) {
					continue;
				}

				for (int y = 0; y <= height; y++) {
					if (y > 0 && !isInsideDomainDome(horizontalDistanceSq, y, radius, height)) {
						continue;
					}

						boolean shell = y == 0 || !isInsideDomainDome(horizontalDistanceSq, y, innerRadius, innerHeight);

						mutablePos.set(centerX + dx, baseY + y, centerZ + dz);
						if (!world.isInBuildLimit(mutablePos)) {
							continue;
						}

						BlockPos immutablePos = mutablePos.toImmutable();
						BlockState targetState = resolveDomainTargetState(
							ability,
							shell,
							centerX,
							centerZ,
							baseY,
							radius,
							height,
							innerRadius,
							innerHeight,
							immutablePos
						);
						boolean protectGreedInterior = ability == MagicAbility.GREED_DOMAIN_EXPANSION
							&& GreedDomainRuntime.isProtectedInteriorStructureBlock(centerX, centerZ, baseY, innerRadius, innerHeight, immutablePos);
						if (shell || targetState.isOf(Blocks.LIGHT) || protectGreedInterior) {
							protectedShellStates.put(immutablePos, targetState);
						}

						BlockState currentState = world.getBlockState(mutablePos);
						BlockEntity blockEntity = world.getBlockEntity(mutablePos);
						NbtCompound blockEntityNbt = blockEntity == null ? null : blockEntity.createNbtWithIdentifyingData(world.getRegistryManager());
						savedBlocks.put(immutablePos, new DomainSavedBlockState(currentState, blockEntityNbt));

						if (shouldPreserveBeaconAnchor(currentState)) {
							protectedShellStates.put(immutablePos, currentState);
							continue;
						}

						if (hasProtectedDecorationEntity(world, immutablePos)) {
							if (shell || targetState.isOf(Blocks.LIGHT) || protectGreedInterior) {
								protectedShellStates.put(immutablePos, currentState);
							}
							continue;
						}

						if (currentState.equals(targetState)) {
							continue;
						}

						if (!setDomainBlockState(world, immutablePos, targetState, DOMAIN_BLOCK_PLACE_FLAGS)) {
							continue;
						}
					}
				}
			}

		moveCapturedEntitiesIntoDomain(
			world,
			capturedEntities.values(),
			centerDx,
			centerDz,
			baseY,
			innerRadius,
			innerHeight
		);
		playDomainActivationSounds(world, player);

		int expiresTick = domainExpiresTick(ability, currentTick);
		int effectEndTick = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? currentTick + Math.max(0, FROST_CONFIG.domain.startupTicks) + Math.max(1, FROST_CONFIG.domain.totalDurationTicks)
			: expiresTick;
		int nextFrostPulseTick = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? currentTick + Math.max(0, FROST_CONFIG.domain.startupTicks) + Math.max(1, FROST_CONFIG.domain.pulseIntervalTicks)
			: Integer.MAX_VALUE;

		DomainExpansionState state = new DomainExpansionState(
			world.getRegistryKey(),
			ability,
			expiresTick,
			effectEndTick,
			centerDx,
			centerDz,
			baseY,
			radius,
			height,
			innerRadius,
			innerHeight,
			currentTick,
			false,
			1.0,
			0,
			nextFrostPulseTick,
			savedBlocks,
			protectedShellStates,
			capturedEntities
		);
		DOMAIN_EXPANSIONS.put(player.getUuid(), state);
		if (ability == MagicAbility.GREED_DOMAIN_EXPANSION) {
			GreedDomainRuntime.onDomainActivated(
				player,
				world,
				centerDx,
				centerDz,
				baseY,
				innerRadius,
				innerHeight,
				capturedEntities.keySet(),
				currentTick
			);
		}
		persistDomainRuntimeState(world.getServer());
	}

	static boolean deactivateDomainExpansion(ServerPlayerEntity player) {
		DomainExpansionState state = DOMAIN_EXPANSIONS.remove(player.getUuid());
		if (state == null) {
			return false;
		}

		MinecraftServer server = player.getEntityWorld().getServer();
		ASTRAL_CATACLYSM_DOMAIN_STATES.remove(player.getUuid());
		endOwnedDomain(player.getUuid(), state, server, server.getTicks());
		persistDomainRuntimeState(server);
		applyDomainInstabilityPenalty(player);
		return true;
	}

	static void endOwnedDomain(
		UUID ownerId,
		DomainExpansionState state,
		MinecraftServer server,
		int currentTick
	) {
		cancelDomainClash(ownerId, server);
		ServerWorld world = server.getWorld(state.dimension);
		if (world != null && state.ability == MagicAbility.GREED_DOMAIN_EXPANSION) {
			GreedDomainRuntime.onDomainEnded(
				server,
				ownerId,
				world,
				state.centerX,
				state.centerZ,
				state.baseY,
				state.innerRadius,
				state.innerHeight,
				currentTick
			);
		}
		restoreDomainExpansion(server, state);
		applyDomainCasterShutdown(ownerId, state.ability, server, currentTick, state.cooldownMultiplier);
	}

	static void restoreDomainExpansion(MinecraftServer server, DomainExpansionState state) {
		ServerWorld world = server.getWorld(state.dimension);
		if (world == null) {
			return;
		}

		for (Map.Entry<BlockPos, DomainSavedBlockState> entry : state.savedBlocks.entrySet()) {
			BlockPos pos = entry.getKey();
			DomainSavedBlockState saved = entry.getValue();
			if (!world.isInBuildLimit(pos)) {
				continue;
			}

			if (!setDomainBlockState(world, pos, saved.blockState, DOMAIN_BLOCK_RESTORE_FLAGS)) {
				continue;
			}

			world.removeBlockEntity(pos);
			if (saved.blockEntityNbt == null || !saved.blockState.hasBlockEntity()) {
				continue;
			}

			BlockEntity restored = BlockEntity.createFromNbt(pos, saved.blockState, saved.blockEntityNbt.copy(), world.getRegistryManager());
			if (restored == null) {
				continue;
			}

			world.getWorldChunk(pos).setBlockEntity(restored);
			restored.markDirty();
		}

		restoreCapturedEntities(server, state);
	}

	static void updateDomainExpansions(MinecraftServer server, int currentTick) {
		boolean changed = false;
		Iterator<Map.Entry<UUID, DomainExpansionState>> iterator = DOMAIN_EXPANSIONS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, DomainExpansionState> entry = iterator.next();
			DomainExpansionState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null) {
				continue;
			}

			UUID ownerId = entry.getKey();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(ownerId);
			boolean clashActive = DOMAIN_CLASHES_BY_OWNER.containsKey(ownerId);
			if (!clashActive && state.ability == MagicAbility.GREED_DOMAIN_EXPANSION && GreedDomainRuntime.shouldPauseDomainTimer(ownerId, currentTick)) {
				state.expiresTick++;
				if (state.effectEndTick != Integer.MAX_VALUE) {
					state.effectEndTick++;
				}
			}
			boolean expired = !clashActive && currentTick >= state.expiresTick;

			if (!expired) {
				if (state.ability == MagicAbility.FROST_DOMAIN_EXPANSION && caster != null && caster.isAlive()) {
					refreshFrostDomainCasterEffects(caster);
				}
				if (state.ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
					clearLoveDomainWitherEffects(world, state, ownerId);
				}
				if (!(clashActive && DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS) && state.ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
					changed |= updateFrostDomain(world, ownerId, state, currentTick);
				}
				if (!(clashActive && DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS) && state.ability == MagicAbility.GREED_DOMAIN_EXPANSION) {
					GreedDomainRuntime.updateActiveDomain(
						server,
						world,
						ownerId,
						state.centerX,
						state.centerZ,
						state.baseY,
						state.innerRadius,
						state.innerHeight,
						currentTick
					);
				}
				if (!(clashActive && DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS)) {
					enforceCapturedEntitiesInsideDomain(world, state);
				}
				maintainDomainShell(world, state);
				continue;
			}

			if (state.ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
				executeFrostDomainFinale(world, ownerId, state);
			}
			if (state.ability == MagicAbility.GREED_DOMAIN_EXPANSION) {
				GreedDomainRuntime.onDomainEnded(
					server,
					ownerId,
					world,
					state.centerX,
					state.centerZ,
					state.baseY,
					state.innerRadius,
					state.innerHeight,
					currentTick
				);
			}
			cancelDomainClash(ownerId, server);
			restoreDomainExpansion(server, state);
			applyDomainCasterShutdown(ownerId, state.ability, server, currentTick, state.cooldownMultiplier);
			ASTRAL_CATACLYSM_DOMAIN_STATES.remove(ownerId);
			iterator.remove();
			changed = true;

			if (caster != null && caster.isAlive() && activeAbility(caster) == state.ability) {
				MagicPlayerData.setActiveAbilitySlot(caster, MagicAbility.NONE.slot());
				caster.sendMessage(Text.translatable("message.magic.ability.deactivated", state.ability.displayName()), true);
			}

			if (caster != null && caster.isAlive()) {
				applyDomainInstabilityPenalty(caster);
			}
		}

		if (changed) {
			persistDomainRuntimeState(server);
		}
	}

	static void clearLoveDomainWitherEffects(ServerWorld world, DomainExpansionState state, UUID ownerId) {
		for (LivingEntity target : collectLivingEntitiesInsideDomain(world, state, ownerId)) {
			target.removeStatusEffect(StatusEffects.WITHER);
		}
		clearLoveDomainClashParticipantWither(world, state, ownerId);
		DomainClashState clash = domainClashStateForParticipant(ownerId);
		if (clash != null) {
			UUID otherId = clash.ownerId.equals(ownerId) ? clash.challengerId : clash.ownerId;
			clearLoveDomainClashParticipantWither(world, state, otherId);
		}
	}

	static void clearLoveDomainClashParticipantWither(ServerWorld world, DomainExpansionState state, UUID playerId) {
		if (playerId == null || world == null || state == null) {
			return;
		}

		Entity entity = world.getEntity(playerId);
		if (!(entity instanceof ServerPlayerEntity player) || !player.isAlive()) {
			return;
		}
		if (domainClashStateForParticipant(playerId) == null || MagicPlayerData.getSchool(player) != MagicSchool.LOVE) {
			return;
		}
		if (!isLivingEntityInsideDomainInterior(player, state)) {
			return;
		}

		player.removeStatusEffect(StatusEffects.WITHER);
	}

	static boolean updateFrostDomain(ServerWorld world, UUID ownerId, DomainExpansionState state, int currentTick) {
		if (currentTick > state.effectEndTick || state.nextFrostPulseTick == Integer.MAX_VALUE) {
			return false;
		}

		boolean changed = false;
		while (currentTick >= state.nextFrostPulseTick && state.nextFrostPulseTick <= state.effectEndTick) {
			state.frostPulseCount++;
			applyFrostDomainPulse(world, ownerId, state, currentTick);
			state.nextFrostPulseTick += Math.max(1, FROST_CONFIG.domain.pulseIntervalTicks);
			changed = true;
		}
		return changed;
	}

	static void applyFrostDomainPulse(ServerWorld world, UUID ownerId, DomainExpansionState state, int currentTick) {
		int pulseDurationTicks = Math.max(0, FROST_CONFIG.domain.pulseDurationTicks);
		int blindnessDurationTicks = Math.max(0, FROST_CONFIG.domain.blindnessPulseDurationTicks);
		int freezeDurationTicks = Math.max(0, FROST_CONFIG.domain.freezePulseDurationTicks);
		int pulseAmplifier = MathHelper.clamp(
			FROST_CONFIG.domain.slownessBaseAmplifier + Math.max(0, state.frostPulseCount - 1) * FROST_CONFIG.domain.slownessAmplifierPerPulse,
			-1,
			255
		);

		for (LivingEntity target : collectLivingEntitiesInsideDomain(world, state, ownerId)) {
			if (FROST_CONFIG.domain.applySlowness && pulseDurationTicks > 0 && pulseAmplifier >= 0) {
				refreshTrackedFrostDomainSlowness(target, ownerId, currentTick, pulseDurationTicks, pulseAmplifier);
			}
			if (FROST_CONFIG.domain.applyFrost && pulseDurationTicks > 0 && FROST_CONFIG.domain.frostDamagePerTick > 0.0F) {
				applyOrRefreshDomainFrostbite(
					target,
					ownerId,
					currentTick,
					pulseDurationTicks,
					FROST_CONFIG.domain.frostDamagePerTick,
					FROST_CONFIG.debuff.refreshDurationOnReapply,
					FROST_CONFIG.debuff.stackDamageOnReapply
				);
			}
			if (FROST_CONFIG.domain.applyBlindness && blindnessDurationTicks > 0) {
				refreshTrackedFrostDomainBlindness(target, ownerId, currentTick, blindnessDurationTicks);
			}
			if (FROST_CONFIG.domain.applyFreeze && freezeDurationTicks > 0) {
				applyDomainFrostFreeze(target, ownerId, currentTick + freezeDurationTicks);
			}
			world.spawnParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.3, 0.6, 0.3, 0.03);
			world.spawnParticles(ParticleTypes.WHITE_ASH, target.getX(), target.getBodyY(0.5), target.getZ(), 6, 0.25, 0.5, 0.25, 0.02);
		}
	}

	static void refreshFrostDomainCasterEffects(ServerPlayerEntity caster) {
		if (caster == null || !caster.isAlive()) {
			return;
		}

		if (FROST_CONFIG.domain.casterResistanceAmplifier >= 0) {
			refreshStatusEffect(caster, StatusEffects.RESISTANCE, EFFECT_REFRESH_TICKS, FROST_CONFIG.domain.casterResistanceAmplifier, true, false, true);
		}
		if (FROST_CONFIG.domain.casterSlownessAmplifier >= 0) {
			refreshStatusEffect(caster, StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, FROST_CONFIG.domain.casterSlownessAmplifier, true, false, true);
		}
	}

	static void executeFrostDomainFinale(ServerWorld world, UUID ownerId, DomainExpansionState state) {
		if (!FROST_CONFIG.domain.finalExecutionEnabled) {
			return;
		}

		for (LivingEntity target : collectLivingEntitiesInsideDomain(world, state, ownerId)) {
			world.spawnParticles(
				new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
				target.getX(),
				target.getBodyY(0.5),
				target.getZ(),
				FROST_CONFIG.domain.executionParticleCount,
				0.45,
				0.9,
				0.45,
				0.06
			);
			world.spawnParticles(ParticleTypes.WHITE_ASH, target.getX(), target.getBodyY(0.5), target.getZ(), 24, 0.35, 0.8, 0.35, 0.03);
			world.playSound(
				null,
				target.getX(),
				target.getY(),
				target.getZ(),
				SoundEvents.BLOCK_GLASS_BREAK,
				SoundCategory.PLAYERS,
				FROST_CONFIG.domain.executionSoundVolume,
				FROST_CONFIG.domain.executionSoundPitch
			);
			if (FROST_CONFIG.domain.finalExecutionBypassTotems) {
				target.kill(world);
			} else {
				dealTrackedMagicDamage(target, ownerId, world.getDamageSources().freeze(), Float.MAX_VALUE);
			}
		}
	}

	static List<LivingEntity> collectLivingEntitiesInsideDomain(ServerWorld world, DomainExpansionState state, UUID ownerId) {
		Box searchBox = new Box(
			state.centerX - state.innerRadius,
			state.baseY + 1.0,
			state.centerZ - state.innerRadius,
			state.centerX + state.innerRadius + 1.0,
			state.baseY + state.innerHeight + 1.0,
			state.centerZ + state.innerRadius + 1.0
		);
		List<LivingEntity> targets = new ArrayList<>();
		for (LivingEntity entity : world.getEntitiesByClass(
			LivingEntity.class,
			searchBox,
			living -> living.isAlive() && (!(living instanceof PlayerEntity player) || !player.isSpectator())
		)) {
			if (ownerId.equals(entity.getUuid()) || !isLivingEntityInsideDomainInterior(entity, state)) {
				continue;
			}
			targets.add(entity);
		}
		return targets;
	}

	static boolean isLivingEntityInsideDomainInterior(LivingEntity entity, DomainExpansionState state) {
		double dx = entity.getX() - state.centerX;
		double dz = entity.getZ() - state.centerZ;
		double horizontalDistanceSq = dx * dx + dz * dz;
		double relativeY = entity.getY() - state.baseY;
		return isInsideDomainInterior(horizontalDistanceSq, relativeY, state.innerRadius, state.innerHeight);
	}

	static void resetDomainTimingForAbility(DomainExpansionState state, MagicAbility ability, int currentTick) {
		state.ability = ability;
		state.expiresTick = domainExpiresTick(ability, currentTick);
		state.effectEndTick = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? currentTick + Math.max(0, FROST_CONFIG.domain.startupTicks) + Math.max(1, FROST_CONFIG.domain.totalDurationTicks)
			: state.expiresTick;
		state.frostPulseCount = 0;
		state.nextFrostPulseTick = ability == MagicAbility.FROST_DOMAIN_EXPANSION
			? currentTick + Math.max(0, FROST_CONFIG.domain.startupTicks) + Math.max(1, FROST_CONFIG.domain.pulseIntervalTicks)
			: Integer.MAX_VALUE;
		state.activationTick = currentTick;
	}

	static void syncDomainTimerOverlays(MinecraftServer server, int currentTick) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			DomainExpansionState domain = DOMAIN_EXPANSIONS.get(player.getUuid());
			if (domain == null || domain.ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
				MagicPlayerData.clearDomainTimer(player);
				continue;
			}

			int remainingTicks = Math.max(0, domain.expiresTick - currentTick);
			int remainingSeconds = Math.max(0, MathHelper.ceil(remainingTicks / (float) TICKS_PER_SECOND));
			int secondarySeconds = 0;
			if (domain.ability == MagicAbility.ASTRAL_CATACLYSM) {
				ConstellationDomainState state = ASTRAL_CATACLYSM_DOMAIN_STATES.get(player.getUuid());
				if (state != null && state.phase == ConstellationDomainPhase.CHARGING) {
					int chargeTicksRemaining = Math.max(0, state.chargeCompleteTick - currentTick);
					secondarySeconds = Math.max(0, MathHelper.ceil(chargeTicksRemaining / (float) TICKS_PER_SECOND));
				}
			}

			if (remainingSeconds <= 0 && secondarySeconds <= 0) {
				MagicPlayerData.clearDomainTimer(player);
				continue;
			}

			MagicPlayerData.setDomainTimer(player, domain.ability.id(), remainingSeconds, secondarySeconds);
		}
	}

	static void resolveSimultaneousDomainCast(
		ServerPlayerEntity challenger,
		MagicAbility challengerAbility,
		int currentTick,
		UUID ownerId,
		DomainExpansionState ownerState
	) {
		MinecraftServer server = challenger.getEntityWorld().getServer();
		if (server == null) {
			return;
		}

		spendAbilityCost(challenger, DOMAIN_EXPANSION_ACTIVATION_MANA_COST);
		setActiveAbility(challenger, MagicAbility.NONE);
		startDomainCooldown(challenger.getUuid(), challengerAbility, currentTick, 1.0);
		if (challengerAbility == MagicAbility.GREED_DOMAIN_EXPANSION) {
			GreedAbilityRuntime.recordSuccessfulMagicAbilityUse(challenger, challengerAbility);
		}
		recordOrionsGambitAbilityUse(challenger, challengerAbility);

		DOMAIN_EXPANSIONS.remove(ownerId);
		cancelDomainClash(ownerId, server);
		restoreDomainExpansion(server, ownerState);
		applyDomainCasterShutdown(ownerId, ownerState.ability, server, currentTick, 1.0);

		ServerPlayerEntity ownerPlayer = server.getPlayerManager().getPlayer(ownerId);
		if (ownerPlayer != null && activeAbility(ownerPlayer) == ownerState.ability) {
			setActiveAbility(ownerPlayer, MagicAbility.NONE);
		}

		if (ownerPlayer != null) {
			ownerPlayer.sendMessage(Text.translatable("message.magic.domain.clash.simultaneous"), true);
		}
		challenger.sendMessage(Text.translatable("message.magic.domain.clash.simultaneous"), true);
		persistDomainRuntimeState(server);
	}

	static boolean startDomainClash(
		ServerPlayerEntity challenger,
		MagicAbility challengerAbility,
		UUID ownerId,
		DomainExpansionState state,
		int currentTick
	) {
		if (!(challenger.getEntityWorld() instanceof ServerWorld world)) {
			return false;
		}

		MinecraftServer server = world.getServer();
		if (server == null || DOMAIN_CLASHES_BY_OWNER.containsKey(ownerId)) {
			return false;
		}

		ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerId);
		if (owner == null || !owner.isAlive() || owner.isSpectator()) {
			return false;
		}

		state.clashOccurred = true;
		int titleEndTick = currentTick + domainClashTitleDurationTicks();
		int instructionsFadeStartTick = titleEndTick + DOMAIN_CLASH_INSTRUCTIONS_DURATION_TICKS;
		int combatStartTick = instructionsFadeStartTick + DOMAIN_CLASH_INSTRUCTIONS_FADE_OUT_TICKS;
		int clashSafetyTick = currentTick + domainClashIntroLockTicks() + TICKS_PER_SECOND;
		state.expiresTick = Math.max(state.expiresTick, clashSafetyTick);
		state.effectEndTick = Math.max(state.effectEndTick, clashSafetyTick);
		applySplitDomainInteriorVisuals(world, state, state.ability, challengerAbility);
		Vec3d ownerLockedPos = new Vec3d(owner.getX(), owner.getY(), owner.getZ());
		Vec3d challengerLockedPos = new Vec3d(challenger.getX(), challenger.getY(), challenger.getZ());
		if (state.ability == MagicAbility.GREED_DOMAIN_EXPANSION || challengerAbility == MagicAbility.GREED_DOMAIN_EXPANSION) {
			ownerLockedPos = resolveGreedDomainClashLockedPosition(world, state, owner, ownerLockedPos);
			challengerLockedPos = resolveGreedDomainClashLockedPosition(world, state, challenger, challengerLockedPos);
		}

		DomainClashState clashState = new DomainClashState(
			ownerId,
			state.ability,
			challenger.getUuid(),
			challengerAbility,
			ownerLockedPos,
			challengerLockedPos,
			currentTick,
			titleEndTick,
			instructionsFadeStartTick,
			combatStartTick
		);

		DOMAIN_CLASHES_BY_OWNER.put(ownerId, clashState);
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.put(ownerId, ownerId);
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.put(challenger.getUuid(), ownerId);

		MagicPlayerData.setDomainClashActive(owner, true);
		MagicPlayerData.setDomainClashProgress(owner, 0);
		MagicPlayerData.setDomainClashPromptKey(owner, 0);
		MagicPlayerData.setDomainClashInstructionVisibility(owner, 0);
		MagicPlayerData.setDomainClashActive(challenger, true);
		MagicPlayerData.setDomainClashProgress(challenger, 0);
		MagicPlayerData.setDomainClashPromptKey(challenger, 0);
		MagicPlayerData.setDomainClashInstructionVisibility(challenger, 0);

		showDomainClashTitle(owner);
		showDomainClashTitle(challenger);
		if (state.ability == MagicAbility.GREED_DOMAIN_EXPANSION || challengerAbility == MagicAbility.GREED_DOMAIN_EXPANSION) {
			lockDomainClashParticipant(owner, clashState.ownerLockedPos, challenger.getEyePos());
			lockDomainClashParticipant(challenger, clashState.challengerLockedPos, owner.getEyePos());
		}
		applyDomainClashStartEffects(owner);
		applyDomainClashStartEffects(challenger);
		persistDomainRuntimeState(server);
		return true;
	}

	static void updateDomainClashes(MinecraftServer server, int currentTick) {
		List<DomainClashResolution> pendingResolutions = new ArrayList<>();
		Iterator<Map.Entry<UUID, DomainClashState>> iterator = DOMAIN_CLASHES_BY_OWNER.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, DomainClashState> entry = iterator.next();
			UUID ownerId = entry.getKey();
			DomainClashState clash = entry.getValue();
			DomainExpansionState domain = DOMAIN_EXPANSIONS.get(ownerId);
			if (domain == null) {
				clearDomainClashUiFor(clash, server);
				DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(clash.ownerId);
				DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(clash.challengerId);
				iterator.remove();
				continue;
			}

			ServerWorld world = server.getWorld(domain.dimension);
			if (world == null) {
				continue;
			}

			ServerPlayerEntity owner = server.getPlayerManager().getPlayer(clash.ownerId);
			ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(clash.challengerId);
			if (owner == null || !owner.isAlive() || owner.isSpectator()) {
				pendingResolutions.add(new DomainClashResolution(ownerId, clash.challengerId));
				continue;
			}
			if (challenger == null || !challenger.isAlive() || challenger.isSpectator()) {
				pendingResolutions.add(new DomainClashResolution(ownerId, clash.ownerId));
				continue;
			}
			if (DOMAIN_CLASH_PAUSE_TIMED_DOMAIN_COLLAPSE_TIMERS && domain.expiresTick != Integer.MAX_VALUE) {
				domain.expiresTick++;
				domain.effectEndTick++;
				if (DOMAIN_CLASH_DISABLE_DOMAIN_EFFECTS && domain.ability == MagicAbility.FROST_DOMAIN_EXPANSION && domain.nextFrostPulseTick != Integer.MAX_VALUE) {
					domain.nextFrostPulseTick++;
				}
			}
			if (DOMAIN_CLASH_PAUSE_ASTRAL_CATACLYSM_PHASE_TIMERS && domain.ability == MagicAbility.ASTRAL_CATACLYSM) {
				pauseAstralCataclysmDomainStateForClash(ownerId);
			}

			if (isDomainClashFrozen(clash, currentTick)) {
				lockDomainClashParticipant(owner, clash.ownerLockedPos, challenger.getEyePos());
				lockDomainClashParticipant(challenger, clash.challengerLockedPos, owner.getEyePos());
			}
			if (clash.ownerAbility == MagicAbility.FROST_DOMAIN_EXPANSION) {
				refreshFrostDomainCasterEffects(owner);
			}
			if (clash.challengerAbility == MagicAbility.FROST_DOMAIN_EXPANSION) {
				refreshFrostDomainCasterEffects(challenger);
			}
			progressSuspendedFrostStageDuringDomainClash(owner);
			progressSuspendedFrostStageDuringDomainClash(challenger);
			applyDomainClashCombatEffects(owner);
			applyDomainClashCombatEffects(challenger);
			spawnDomainClashParticles(world, domain, DOMAIN_CLASH_PARTICLES_PER_TICK);
			int ownerProgressPercent = domainClashProgressPercent(clash.ownerDamageDealt);
			int challengerProgressPercent = domainClashProgressPercent(clash.challengerDamageDealt);
			int instructionVisibilityPercent = domainClashInstructionVisibilityPercent(clash, currentTick);

			MagicPlayerData.setDomainClashActive(owner, true);
			MagicPlayerData.setDomainClashProgress(owner, ownerProgressPercent);
			MagicPlayerData.setDomainClashPromptKey(owner, 0);
			MagicPlayerData.setDomainClashInstructionVisibility(owner, instructionVisibilityPercent);
			MagicPlayerData.setDomainClashActive(challenger, true);
			MagicPlayerData.setDomainClashProgress(challenger, challengerProgressPercent);
			MagicPlayerData.setDomainClashPromptKey(challenger, 0);
			MagicPlayerData.setDomainClashInstructionVisibility(challenger, instructionVisibilityPercent);
		}

		for (DomainClashResolution resolution : pendingResolutions) {
			resolveDomainClash(server, resolution.ownerId, resolution.winnerId, currentTick);
		}
	}

	static void pauseAstralCataclysmDomainStateForClash(UUID ownerId) {
		ConstellationDomainState state = ASTRAL_CATACLYSM_DOMAIN_STATES.get(ownerId);
		if (state == null) {
			return;
		}

		if (state.phase == ConstellationDomainPhase.CHARGING) {
			state.chargeCompleteTick++;
			return;
		}

		if (state.phase == ConstellationDomainPhase.ACQUIRING) {
			state.acquireEndTick++;
		}
	}
}


