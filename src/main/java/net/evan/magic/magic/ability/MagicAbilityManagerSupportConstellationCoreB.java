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


abstract class MagicAbilityManagerSupportConstellationCoreB extends MagicAbilityManagerSupportConstellationCoreA {
	static void updateSagittaConstellation(ServerWorld world, ServerPlayerEntity caster, CelestialAlignmentState state, int currentTick) {
		if (currentTick >= state.startTick + CELESTIAL_ALIGNMENT_CONFIG.sagitta.beamStartDelayTicks && currentTick >= state.nextEffectTick) {
			state.nextEffectTick = currentTick + Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.sagitta.beamIntervalTicks);
			for (int index = 0; index < CELESTIAL_ALIGNMENT_CONFIG.sagitta.beamsPerInterval; index++) {
				double angle = caster.getRandom().nextDouble() * Math.PI * 2.0;
				double distance = Math.sqrt(caster.getRandom().nextDouble()) * state.radius;
				double x = state.center.x + Math.cos(angle) * distance;
				double z = state.center.z + Math.sin(angle) * distance;
				state.sagittaStrikes.add(new CelestialSagittaStrikeState(new Vec3d(x, state.minY, z), currentTick + Math.max(0, CELESTIAL_ALIGNMENT_CONFIG.sagitta.telegraphTicks)));
			}
		}

		Iterator<CelestialSagittaStrikeState> iterator = state.sagittaStrikes.iterator();
		while (iterator.hasNext()) {
			CelestialSagittaStrikeState strike = iterator.next();
			if (currentTick < strike.impactTick) {
				spawnSagittaTelegraph(world, state, strike);
				continue;
			}
			spawnSagittaImpact(world, state, caster, strike);
			iterator.remove();
		}
	}

	static void updateAquilaConstellation(ServerWorld world, ServerPlayerEntity caster, CelestialAlignmentState state, int currentTick) {
		if (currentTick >= state.nextEffectTick) {
			state.nextEffectTick = currentTick + Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.aquila.projectileSpawnIntervalTicks);
			List<LivingEntity> targets = collectLivingEntitiesInCelestialAlignment(world, state, caster).stream().filter(living -> living != caster).toList();
			for (int index = 0; index < CELESTIAL_ALIGNMENT_CONFIG.aquila.projectilesPerInterval && !targets.isEmpty(); index++) {
				LivingEntity target = targets.get(caster.getRandom().nextInt(targets.size()));
				double angle = caster.getRandom().nextDouble() * Math.PI * 2.0;
				double distance = Math.sqrt(caster.getRandom().nextDouble()) * state.radius;
				Vec3d position = new Vec3d(
					state.center.x + Math.cos(angle) * distance,
					state.maxY + 2.0,
					state.center.z + Math.sin(angle) * distance
				);
				state.aquilaStars.add(new AquilaStarState(position, Vec3d.ZERO, target.getUuid(), currentTick + CELESTIAL_ALIGNMENT_CONFIG.aquila.maxLifetimeTicks));
			}
		}

		Iterator<AquilaStarState> starIterator = state.aquilaStars.iterator();
		while (starIterator.hasNext()) {
			AquilaStarState star = starIterator.next();
			if (currentTick >= star.expireTick) {
				starIterator.remove();
				continue;
			}
			Entity entity = world.getEntity(star.targetId);
			if (!(entity instanceof LivingEntity target) || !isInsideCelestialAlignmentArea(state, target) || target == caster) {
				starIterator.remove();
				continue;
			}
			Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ());
			Vec3d desired = targetPos.subtract(star.position);
			if (desired.lengthSquared() <= 1.0E-6) {
				starIterator.remove();
				continue;
			}
			Vec3d desiredVelocity = desired.normalize().multiply(CELESTIAL_ALIGNMENT_CONFIG.aquila.projectileSpeed);
			Vec3d nextVelocity = star.velocity.lengthSquared() <= 1.0E-6
				? desiredVelocity
				: star.velocity.lerp(desiredVelocity, MathHelper.clamp(CELESTIAL_ALIGNMENT_CONFIG.aquila.homingTurnRate, 0.0, 1.0));
			if (nextVelocity.lengthSquared() > 1.0E-6) {
				nextVelocity = nextVelocity.normalize().multiply(CELESTIAL_ALIGNMENT_CONFIG.aquila.projectileSpeed);
			}
			star.velocity = nextVelocity;
			star.position = star.position.add(star.velocity);
			spawnAquilaProjectileEffects(world, state, star, currentTick);
			if (star.position.squaredDistanceTo(targetPos) <= CELESTIAL_ALIGNMENT_CONFIG.aquila.hitRadius * CELESTIAL_ALIGNMENT_CONFIG.aquila.hitRadius) {
				dealTrackedMagicDamage(target, caster.getUuid(), world.getDamageSources().magic(), CELESTIAL_ALIGNMENT_CONFIG.aquila.projectileDamage);
				spawnAquilaImpactEffects(world, target);
				starIterator.remove();
			}
		}
	}

	static void spawnSagittaTelegraph(ServerWorld world, CelestialAlignmentState state, CelestialSagittaStrikeState strike) {
		DustParticleEffect telegraph = celestialDust(celestialAlignmentColorHex(state.constellation), 0.9F);
		int previewPoints = Math.max(8, MathHelper.ceil(CELESTIAL_ALIGNMENT_CONFIG.sagitta.telegraphRingSize * 12.0));
		for (int index = 0; index < previewPoints; index++) {
			double angle = (Math.PI * 2.0 * index) / previewPoints;
			double x = strike.center.x + Math.cos(angle) * CELESTIAL_ALIGNMENT_CONFIG.sagitta.telegraphRingSize;
			double z = strike.center.z + Math.sin(angle) * CELESTIAL_ALIGNMENT_CONFIG.sagitta.telegraphRingSize;
			world.spawnParticles(telegraph, x, strike.center.y + 0.05, z, 1, 0.01, 0.01, 0.01, 0.0);
		}
		spawnParticleBeam(world, new Vec3d(strike.center.x, state.maxY, strike.center.z), new Vec3d(strike.center.x, strike.center.y, strike.center.z), ParticleTypes.GLOW, 1.2);
	}

	static void spawnSagittaImpact(ServerWorld world, CelestialAlignmentState state, ServerPlayerEntity caster, CelestialSagittaStrikeState strike) {
		spawnParticleBeam(world, new Vec3d(strike.center.x, state.maxY, strike.center.z), new Vec3d(strike.center.x, strike.center.y, strike.center.z), ParticleTypes.END_ROD, 0.45);
		world.spawnParticles(ParticleTypes.GLOW, strike.center.x, strike.center.y + 0.1, strike.center.z, Math.max(0, CELESTIAL_ALIGNMENT_CONFIG.sagitta.impactBurstDensity), 0.45, 0.18, 0.45, 0.0);
		world.playSound(null, strike.center.x, strike.center.y, strike.center.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, CELESTIAL_ALIGNMENT_CONFIG.sagitta.strikeSoundVolume, CELESTIAL_ALIGNMENT_CONFIG.sagitta.strikeSoundPitch);
		if (CELESTIAL_ALIGNMENT_CONFIG.sagitta.layeredStrikeSound) {
			world.playSound(null, strike.center.x, strike.center.y, strike.center.z, SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, CELESTIAL_ALIGNMENT_CONFIG.sagitta.strikeSoundVolume * 0.75F, CELESTIAL_ALIGNMENT_CONFIG.sagitta.strikeSoundPitch + 0.18F);
		}

		double hitRadiusSq = CELESTIAL_ALIGNMENT_CONFIG.sagitta.strikeHitRadius * CELESTIAL_ALIGNMENT_CONFIG.sagitta.strikeHitRadius;
		for (LivingEntity living : collectLivingEntitiesInCelestialAlignment(world, state, caster)) {
			if (living == caster) {
				continue;
			}
			double dx = living.getX() - strike.center.x;
			double dz = living.getZ() - strike.center.z;
			if (dx * dx + dz * dz <= hitRadiusSq) {
				dealTrackedMagicDamage(living, caster.getUuid(), world.getDamageSources().magic(), CELESTIAL_ALIGNMENT_CONFIG.sagitta.strikeDamage);
			}
		}
	}

	static void spawnAquilaProjectileEffects(ServerWorld world, CelestialAlignmentState state, AquilaStarState star, int currentTick) {
		int count = Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.aquila.trailParticleCount);
		DustParticleEffect trail = celestialDust(celestialAlignmentColorHex(state.constellation), (float) CELESTIAL_ALIGNMENT_CONFIG.aquila.projectileScale);
		world.spawnParticles(trail, star.position.x, star.position.y, star.position.z, count, 0.06, 0.06, 0.06, 0.0);
		world.spawnParticles(ParticleTypes.END_ROD, star.position.x, star.position.y, star.position.z, 1, 0.03, 0.03, 0.03, 0.0);
	}

	static void spawnAquilaImpactEffects(ServerWorld world, LivingEntity target) {
		if (CELESTIAL_ALIGNMENT_CONFIG.aquila.impactSparkCount <= 0) {
			return;
		}
		world.spawnParticles(ParticleTypes.GLOW, target.getX(), target.getBodyY(0.5), target.getZ(), CELESTIAL_ALIGNMENT_CONFIG.aquila.impactSparkCount, 0.25, 0.25, 0.25, 0.0);
	}

	static void releaseGeminiRecordedDamage(MinecraftServer server, ServerWorld world, CelestialAlignmentState state) {
		ServerPlayerEntity caster = server.getPlayerManager().getPlayer(state.ownerId);
		if (caster == null) {
			return;
		}
		for (Map.Entry<UUID, List<Float>> entry : state.geminiRecordedDamage.entrySet()) {
			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity living) || !isMagicTargetableEntity(living)) {
				continue;
			}
			List<Float> recorded = List.copyOf(entry.getValue());
			if (!recorded.isEmpty()) {
				CELESTIAL_GEMINI_REPLAYS.add(
					new CelestialGeminiReplayState(
						world.getRegistryKey(),
						caster.getUuid(),
						living.getUuid(),
						recorded,
						server.getTicks()
					)
				);
			}
		}
	}

	static void spawnCelestialAmbientEffects(ServerWorld world, CelestialAlignmentState state) {
		int currentTick = currentTickForConstellation(world);
		if (currentTick % Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.visuals.nodePulseIntervalTicks) == 0) {
			CelestialPattern pattern = celestialPattern(state.constellation);
			DustParticleEffect pulse = celestialDust(celestialAlignmentColorHex(state.constellation), CELESTIAL_ALIGNMENT_CONFIG.visuals.starNodeParticleScale * CELESTIAL_ALIGNMENT_CONFIG.visuals.nodePulseScale);
			for (Vec3d node : pattern.nodes) {
				Vec3d point = celestialPatternPoint(state, node, state.minY + 0.08);
				world.spawnParticles(pulse, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
			}
		}

		if (currentTick % Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.visuals.ambientParticleIntervalTicks) != 0) {
			return;
		}
		int ambientCount = CELESTIAL_ALIGNMENT_CONFIG.visuals.reducedConstellationAmbientParticles
			? Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.visuals.ambientParticleCount / 2)
			: CELESTIAL_ALIGNMENT_CONFIG.visuals.ambientParticleCount;
		DustParticleEffect ambient = celestialDust(celestialAlignmentColorHex(state.constellation), 0.55F);
		for (int index = 0; index < ambientCount; index++) {
			double angle = world.random.nextDouble() * Math.PI * 2.0;
			double radius = Math.sqrt(world.random.nextDouble()) * state.radius * 0.9;
			double x = state.center.x + Math.cos(angle) * radius;
			double y = MathHelper.lerp(world.random.nextDouble(), state.minY + 0.2, state.maxY - 0.2);
			double z = state.center.z + Math.sin(angle) * radius;
			world.spawnParticles(ambient, x, y, z, 1, 0.02, 0.03, 0.02, 0.0);
		}
	}

	static void spawnCraterSiphonEffects(ServerWorld world, CelestialAlignmentState state) {
		if (CELESTIAL_ALIGNMENT_CONFIG.crater.siphonParticleCount <= 0) {
			return;
		}
		DustParticleEffect dust = celestialDust(CELESTIAL_ALIGNMENT_CONFIG.crater.colorHex, 0.72F);
		for (int index = 0; index < CELESTIAL_ALIGNMENT_CONFIG.crater.siphonParticleCount; index++) {
			double angle = (Math.PI * 2.0 * index) / Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.crater.siphonParticleCount);
			double radius = state.radius * 0.9;
			double x = state.center.x + Math.cos(angle) * radius;
			double z = state.center.z + Math.sin(angle) * radius;
			double velocityX = (state.center.x - x) * 0.06;
			double velocityZ = (state.center.z - z) * 0.06;
			world.spawnParticles(dust, x, state.minY + 0.08, z, 0, velocityX, 0.0, velocityZ, 1.0);
		}
	}

	static void updateCelestialGeminiReplays(MinecraftServer server, int currentTick) {
		Iterator<CelestialGeminiReplayState> iterator = CELESTIAL_GEMINI_REPLAYS.iterator();
		while (iterator.hasNext()) {
			CelestialGeminiReplayState state = iterator.next();
			if (currentTick < state.nextTick || state.hitIndex >= state.recordedHits.size()) {
				if (state.hitIndex >= state.recordedHits.size()) {
					iterator.remove();
				}
				continue;
			}

			ServerWorld world = server.getWorld(state.dimension);
			if (world == null) {
				iterator.remove();
				continue;
			}
			Entity entity = world.getEntity(state.targetId);
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
				iterator.remove();
				continue;
			}
			dealTrackedMagicDamage(target, state.casterId, world.getDamageSources().magic(), state.recordedHits.get(state.hitIndex));
			spawnGeminiEchoMarker(target, null);
			state.hitIndex++;
			state.nextTick = currentTick + Math.max(0, CELESTIAL_ALIGNMENT_CONFIG.gemini.replayBurstSpacingTicks);
			if (state.hitIndex >= state.recordedHits.size()) {
				iterator.remove();
			}
		}
	}

	static void refreshCelestialMovementSpeedModifier(LivingEntity entity) {
		EntityAttributeInstance movementSpeed = entity.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
		if (movementSpeed == null) {
			return;
		}

		removeAttributeModifier(movementSpeed, CELESTIAL_ALIGNMENT_BOOTES_SPEED_MODIFIER_ID);
	}

	static int adjustCelestialAlignmentManaCost(ServerPlayerEntity player, int manaCost) {
		return Math.max(0, manaCost);
	}

	public static int adjustCelestialAlignmentGreedCoinCost(ServerPlayerEntity player, int coinCost) {
		return Math.max(0, coinCost);
	}

	static boolean isCelestialHealingBlocked(LivingEntity entity) {
		for (CelestialAlignmentSessionState session : SAGITTARIUS_STATES.values()) {
			for (CelestialAlignmentState state : session.constellations) {
				if (
					state.constellation == CelestialAlignmentConstellation.CRATER
					&& entity.getUuid() != state.ownerId
					&& isInsideCelestialAlignmentArea(state, entity)
				) {
					return CELESTIAL_ALIGNMENT_CONFIG.crater.blockNaturalHealing
						|| CELESTIAL_ALIGNMENT_CONFIG.crater.blockPotionHealing
						|| CELESTIAL_ALIGNMENT_CONFIG.crater.blockEffectHealing
						|| CELESTIAL_ALIGNMENT_CONFIG.crater.blockMagicHealing;
				}
			}
		}
		return false;
	}

	static boolean isConstellationTeleportItemBlocked(ServerPlayerEntity player, ItemStack stack) {
		if (player == null || stack == null || (!stack.isOf(Items.ENDER_PEARL) && !stack.isOf(Items.CHORUS_FRUIT))) {
			return false;
		}
		for (CelestialAlignmentSessionState session : SAGITTARIUS_STATES.values()) {
			for (CelestialAlignmentState state : session.constellations) {
				if (player.getUuid() != state.ownerId && isInsideCelestialAlignmentArea(state, player)) {
					return true;
				}
			}
		}
		return false;
	}

	static boolean isTeleportEscapeItem(ItemStack stack) {
		return stack != null && (stack.isOf(Items.ENDER_PEARL) || stack.isOf(Items.CHORUS_FRUIT));
	}

	static float celestialScaledIncomingDamage(LivingEntity entity, DamageSource source, float amount) {
		float scaledAmount = amount;
		for (CelestialAlignmentSessionState session : SAGITTARIUS_STATES.values()) {
			for (CelestialAlignmentState state : session.constellations) {
				if (entity.getUuid() == state.ownerId || !isInsideCelestialAlignmentArea(state, entity)) {
					continue;
				}
				if (state.constellation == CelestialAlignmentConstellation.SCORPIUS) {
					scaledAmount *= (float) CELESTIAL_ALIGNMENT_CONFIG.scorpius.incomingDamageMultiplier;
				}
			}
		}
		if (entity instanceof ServerPlayerEntity player) {
			for (CelestialAlignmentSessionState session : SAGITTARIUS_STATES.values()) {
				for (CelestialAlignmentState state : session.constellations) {
					if (
						state.constellation == CelestialAlignmentConstellation.LIBRA
						&& player.getUuid() == state.ownerId
						&& isInsideCelestialAlignmentArea(state, player)
					) {
						scaledAmount *= (float) CELESTIAL_ALIGNMENT_CONFIG.libra.casterIncomingDamageMultiplier;
					}
				}
			}
		}
		return scaledAmount;
	}

	static boolean tryApplyCelestialAlignmentDamageRedirect(LivingEntity entity, DamageSource source, float amount) {
		ServerPlayerEntity attacker = attackingPlayerFrom(source);
		if (attacker == null) {
			return false;
		}

		for (CelestialAlignmentSessionState session : SAGITTARIUS_STATES.values()) {
			for (CelestialAlignmentState state : session.constellations) {
				if (
					state.constellation != CelestialAlignmentConstellation.LIBRA
					|| !attacker.getUuid().equals(state.ownerId)
					|| !isInsideCelestialAlignmentArea(state, attacker)
				) {
					continue;
				}

				if (!(attacker.getEntityWorld() instanceof ServerWorld world)) {
					continue;
				}
				List<LivingEntity> eligibleTargets = collectLivingEntitiesInCelestialAlignment(world, state, attacker)
					.stream()
					.filter(living -> living != attacker && (CELESTIAL_ALIGNMENT_CONFIG.libra.includeOriginalTarget || !living.getUuid().equals(entity.getUuid())))
					.toList();
				if (eligibleTargets.size() < CELESTIAL_ALIGNMENT_CONFIG.libra.minimumEligibleTargets) {
					continue;
				}

				spawnLibraRedistributionEffects(world, state, entity, eligibleTargets);
				float sharedDamage = amount / Math.max(1, eligibleTargets.size());
				for (LivingEntity living : eligibleTargets) {
					CELESTIAL_DAMAGE_REDIRECT_GUARD.add(living.getUuid());
					try {
						living.damage(world, source, sharedDamage);
					} finally {
						CELESTIAL_DAMAGE_REDIRECT_GUARD.remove(living.getUuid());
					}
				}
				return true;
			}
		}

		return false;
	}

	static void recordCelestialGeminiHit(ServerPlayerEntity attacker, LivingEntity target, float damageTaken) {
		for (CelestialAlignmentSessionState session : SAGITTARIUS_STATES.values()) {
			for (CelestialAlignmentState state : session.constellations) {
				if (
					state.constellation != CelestialAlignmentConstellation.GEMINI
					|| !attacker.getUuid().equals(state.ownerId)
					|| !isInsideCelestialAlignmentArea(state, target)
				) {
					continue;
				}
				List<Float> recordedDamage = state.geminiRecordedDamage.computeIfAbsent(target.getUuid(), ignored -> new ArrayList<>());
				recordedDamage.add(damageTaken);
				spawnGeminiEchoMarker(target, state);
			}
		}
	}

	static void spawnGeminiEchoMarker(LivingEntity target, CelestialAlignmentState state) {
		if (!(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		String color = state == null ? CELESTIAL_ALIGNMENT_CONFIG.gemini.colorHex : celestialAlignmentColorHex(state.constellation);
		DustParticleEffect echo = celestialDust(color, Math.max(0.4F, CELESTIAL_ALIGNMENT_CONFIG.gemini.replayFlashIntensity));
		world.spawnParticles(echo, target.getX(), target.getBodyY(0.5), target.getZ(), Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.gemini.maxVisibleMarkers), 0.25, 0.35, 0.25, 0.0);
	}

	static void spawnLibraRedistributionEffects(ServerWorld world, CelestialAlignmentState state, LivingEntity originalTarget, List<LivingEntity> eligibleTargets) {
		DustParticleEffect tether = celestialDust(CELESTIAL_ALIGNMENT_CONFIG.libra.colorHex, 0.8F);
		for (LivingEntity living : eligibleTargets) {
			Vec3d start = new Vec3d(originalTarget.getX(), originalTarget.getBodyY(0.5), originalTarget.getZ());
			Vec3d end = new Vec3d(living.getX(), living.getBodyY(0.5), living.getZ());
			spawnCelestialAlignmentLine(world, start, end, tether, Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.libra.tetherParticleCount), 0.0, 1.0);
		}
		world.spawnParticles(tether, state.center.x, state.minY + 0.2, state.center.z, Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.libra.centerPulseParticleCount), 0.2, 0.1, 0.2, 0.0);
	}

	static double celestialAlignmentRadius(CelestialAlignmentConstellation constellation) {
		return switch (constellation) {
			case CRATER -> CELESTIAL_ALIGNMENT_CONFIG.crater.radius;
			case SAGITTA -> CELESTIAL_ALIGNMENT_CONFIG.sagitta.radius;
			case GEMINI -> CELESTIAL_ALIGNMENT_CONFIG.gemini.radius;
			case AQUILA -> CELESTIAL_ALIGNMENT_CONFIG.aquila.radius;
			case SCORPIUS -> CELESTIAL_ALIGNMENT_CONFIG.scorpius.radius;
			case LIBRA -> CELESTIAL_ALIGNMENT_CONFIG.libra.radius;
		};
	}

	static String celestialAlignmentColorHex(CelestialAlignmentConstellation constellation) {
		return switch (constellation) {
			case CRATER -> CELESTIAL_ALIGNMENT_CONFIG.crater.colorHex;
			case SAGITTA -> CELESTIAL_ALIGNMENT_CONFIG.sagitta.colorHex;
			case GEMINI -> CELESTIAL_ALIGNMENT_CONFIG.gemini.colorHex;
			case AQUILA -> CELESTIAL_ALIGNMENT_CONFIG.aquila.colorHex;
			case SCORPIUS -> CELESTIAL_ALIGNMENT_CONFIG.scorpius.colorHex;
			case LIBRA -> CELESTIAL_ALIGNMENT_CONFIG.libra.colorHex;
		};
	}

	static int celestialAlignmentColor(CelestialAlignmentConstellation constellation) {
		return parseHexColor(celestialAlignmentColorHex(constellation), 0xFFFFFF);
	}

	static CelestialPattern celestialPattern(CelestialAlignmentConstellation constellation) {
		return switch (constellation) {
			case CRATER -> new CelestialPattern(
				List.of(
					new Vec3d(-0.7, 0.0, -0.2),
					new Vec3d(-0.35, 0.0, -0.52),
					new Vec3d(0.0, 0.0, -0.62),
					new Vec3d(0.35, 0.0, -0.52),
					new Vec3d(0.7, 0.0, -0.2),
					new Vec3d(-0.28, 0.0, 0.18),
					new Vec3d(0.28, 0.0, 0.18),
					new Vec3d(0.0, 0.0, 0.46),
					new Vec3d(0.0, 0.0, 0.72)
				),
				List.of(new CelestialPatternEdge(0, 1), new CelestialPatternEdge(1, 2), new CelestialPatternEdge(2, 3), new CelestialPatternEdge(3, 4), new CelestialPatternEdge(1, 5), new CelestialPatternEdge(3, 6), new CelestialPatternEdge(5, 6), new CelestialPatternEdge(5, 7), new CelestialPatternEdge(6, 7), new CelestialPatternEdge(7, 8))
			);
			case SAGITTA -> new CelestialPattern(
				List.of(
					new Vec3d(0.0, 0.0, -0.7),
					new Vec3d(0.0, 0.0, -0.1),
					new Vec3d(-0.24, 0.0, -0.38),
					new Vec3d(0.24, 0.0, -0.38),
					new Vec3d(0.0, 0.0, 0.58),
					new Vec3d(-0.26, 0.0, 0.34),
					new Vec3d(0.26, 0.0, 0.34)
				),
				List.of(new CelestialPatternEdge(0, 2), new CelestialPatternEdge(0, 3), new CelestialPatternEdge(2, 1), new CelestialPatternEdge(3, 1), new CelestialPatternEdge(1, 4), new CelestialPatternEdge(4, 5), new CelestialPatternEdge(4, 6))
			);
			case GEMINI -> new CelestialPattern(
				List.of(
					new Vec3d(-0.32, 0.0, -0.68),
					new Vec3d(-0.28, 0.0, -0.18),
					new Vec3d(-0.26, 0.0, 0.28),
					new Vec3d(-0.22, 0.0, 0.7),
					new Vec3d(0.32, 0.0, -0.68),
					new Vec3d(0.28, 0.0, -0.18),
					new Vec3d(0.26, 0.0, 0.28),
					new Vec3d(0.22, 0.0, 0.7),
					new Vec3d(0.0, 0.0, -0.4),
					new Vec3d(0.0, 0.0, 0.08)
				),
				List.of(new CelestialPatternEdge(0, 1), new CelestialPatternEdge(1, 2), new CelestialPatternEdge(2, 3), new CelestialPatternEdge(4, 5), new CelestialPatternEdge(5, 6), new CelestialPatternEdge(6, 7), new CelestialPatternEdge(1, 8), new CelestialPatternEdge(5, 8), new CelestialPatternEdge(2, 9), new CelestialPatternEdge(6, 9))
			);
			case AQUILA -> new CelestialPattern(
				List.of(
					new Vec3d(0.0, 0.0, -0.5),
					new Vec3d(0.0, 0.0, 0.56),
					new Vec3d(-0.74, 0.0, -0.12),
					new Vec3d(-0.38, 0.0, 0.08),
					new Vec3d(0.74, 0.0, -0.12),
					new Vec3d(0.38, 0.0, 0.08)
				),
				List.of(new CelestialPatternEdge(0, 2), new CelestialPatternEdge(2, 3), new CelestialPatternEdge(3, 1), new CelestialPatternEdge(0, 4), new CelestialPatternEdge(4, 5), new CelestialPatternEdge(5, 1))
			);
			case SCORPIUS -> new CelestialPattern(
				List.of(
					new Vec3d(-0.68, 0.0, -0.26),
					new Vec3d(-0.38, 0.0, -0.42),
					new Vec3d(0.0, 0.0, -0.2),
					new Vec3d(0.28, 0.0, 0.08),
					new Vec3d(0.14, 0.0, 0.42),
					new Vec3d(0.46, 0.0, 0.72)
				),
				List.of(new CelestialPatternEdge(0, 1), new CelestialPatternEdge(1, 2), new CelestialPatternEdge(2, 3), new CelestialPatternEdge(3, 4), new CelestialPatternEdge(4, 5))
			);
			case LIBRA -> new CelestialPattern(
				List.of(
					new Vec3d(0.0, 0.0, -0.72),
					new Vec3d(0.0, 0.0, -0.2),
					new Vec3d(-0.62, 0.0, 0.08),
					new Vec3d(0.62, 0.0, 0.08),
					new Vec3d(-0.36, 0.0, 0.42),
					new Vec3d(0.36, 0.0, 0.42)
				),
				List.of(new CelestialPatternEdge(0, 1), new CelestialPatternEdge(1, 2), new CelestialPatternEdge(1, 3), new CelestialPatternEdge(2, 4), new CelestialPatternEdge(3, 5))
			);
		};
	}
}

