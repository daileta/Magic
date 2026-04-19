package net.evan.magic.magic.ability;
import net.evan.magic.magic.core.ability.MagicAbility;
import net.evan.magic.magic.ability.GreedAbilityRuntime;

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


abstract class CombatTargetingService extends CombatEffectService {
	static boolean activeAbilityPreventsManaRegen(MagicAbility activeAbility) {
		return switch (activeAbility) {
			case NONE -> false;
			case BELOW_FREEZING -> false;
			case IGNITION -> false;
			case LOVE_DOMAIN_EXPANSION -> false;
			case HERCULES_BURDEN_OF_THE_SKY -> HERCULES_DISABLE_MANA_REGEN_WHILE_ACTIVE;
			case SAGITTARIUS_ASTRAL_ARROW -> true;
			case ASTRAL_CATACLYSM -> ASTRAL_CATACLYSM_DISABLE_MANA_REGEN_WHILE_ACTIVE;
			default -> true;
		};
	}

	static void recordOrionsGambitAbilityUse(ServerPlayerEntity player, MagicAbility ability) {
		recordOrionsGambitAbilityUse(player, ability, -1);
	}

	static void recordOrionsGambitAbilityUse(ServerPlayerEntity player, MagicAbility ability, int trackedCooldownTicks) {
		if (ability != null && ability.school() != MagicSchool.GREED) {
			GreedAbilityRuntime.recordSuccessfulMagicAbilityUse(player, ability);
		}

		UUID casterId = ORIONS_GAMBIT_CASTER_BY_TARGET.get(player.getUuid());
		if (casterId == null) {
			return;
		}

		OrionGambitState state = ORIONS_GAMBIT_STATES.get(casterId);
		if (state == null || state.stage != OrionGambitStage.LINKED || !player.getUuid().equals(state.targetId)) {
			return;
		}

		state.usedTargetAbilities.add(ability);
		if (trackedCooldownTicks >= 0) {
			state.usedTargetCooldownOverrides.merge(ability, trackedCooldownTicks, Math::max);
		}
	}

	static void trackOrionsGambitCooldownOverride(UUID playerId, MagicAbility ability, int trackedCooldownTicks) {
		if (trackedCooldownTicks < 0) {
			return;
		}

		UUID casterId = ORIONS_GAMBIT_CASTER_BY_TARGET.get(playerId);
		if (casterId == null) {
			return;
		}

		OrionGambitState state = ORIONS_GAMBIT_STATES.get(casterId);
		if (state == null || state.stage != OrionGambitStage.LINKED || !playerId.equals(state.targetId)) {
			return;
		}

		state.usedTargetAbilities.add(ability);
		state.usedTargetCooldownOverrides.merge(ability, trackedCooldownTicks, Math::max);
	}

	static boolean shouldKeepCassiopeiaEnabled(ServerPlayerEntity player) {
		return MagicPlayerData.hasMagic(player)
			&& MagicPlayerData.getSchool(player) == MagicSchool.CONSTELLATION
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.CASSIOPEIA);
	}

	static void deactivateCassiopeia(ServerPlayerEntity player, boolean sendEmptyOutline) {
		UUID playerId = player.getUuid();
		CASSIOPEIA_PASSIVE_ENABLED.remove(playerId);
		CASSIOPEIA_LAST_OUTLINED_PLAYERS.remove(playerId);
		if (sendEmptyOutline) {
			sendCassiopeiaOutlinePayload(player, List.of());
		}
	}

	static void syncCassiopeiaOutlines(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		Set<UUID> outlinedPlayers = new HashSet<>();
		Box area = player.getBoundingBox().expand(CASSIOPEIA_DETECTION_RADIUS);
		for (ServerPlayerEntity target : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
			if (target == player || !target.isAlive() || target.isSpectator() || target.getEntityWorld() != player.getEntityWorld()) {
				continue;
			}

			if (target.getBoundingBox().intersects(area)) {
				outlinedPlayers.add(target.getUuid());
			}
		}

		Set<UUID> lastSent = CASSIOPEIA_LAST_OUTLINED_PLAYERS.get(playerId);
		if (lastSent != null && lastSent.equals(outlinedPlayers)) {
			return;
		}

		CASSIOPEIA_LAST_OUTLINED_PLAYERS.put(playerId, Set.copyOf(outlinedPlayers));
		sendCassiopeiaOutlinePayload(player, outlinedPlayers);
	}

	static void sendCassiopeiaOutlinePayload(ServerPlayerEntity player, Iterable<UUID> outlinedPlayers) {
		List<UUID> uuids = new ArrayList<>();
		for (UUID outlinedPlayer : outlinedPlayers) {
			uuids.add(outlinedPlayer);
		}
		ServerPlayNetworking.send(player, new ConstellationOutlinePayload(List.copyOf(uuids)));
	}

	static boolean isMagicTargetableEntity(Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive() || living instanceof ArmorStandEntity) {
			return false;
		}
		return !(living instanceof PlayerEntity player) || !player.isSpectator();
	}

	static List<LivingEntity> findLivingTargetsAround(LivingEntity center, double radius) {
		List<LivingEntity> targets = new ArrayList<>();
		if (!isMagicTargetableEntity(center)) {
			return targets;
		}

		targets.add(center);
		if (radius <= 0.0) {
			return targets;
		}

		Box area = center.getBoundingBox().expand(radius);
		Predicate<Entity> filter = entity -> isMagicTargetableEntity(entity) && entity != center;
		for (Entity entity : center.getEntityWorld().getOtherEntities(center, area, filter)) {
			if (entity instanceof LivingEntity livingTarget && center.squaredDistanceTo(livingTarget) <= radius * radius) {
				targets.add(livingTarget);
			}
		}
		return targets;
	}

	static boolean shouldKeepMartyrsFlameEnabled(ServerPlayerEntity player) {
		return player.isAlive()
			&& MagicPlayerData.hasMagic(player)
			&& MagicPlayerData.getSchool(player) == MagicSchool.BURNING_PASSION
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.MARTYRS_FLAME);
	}

	static LivingEntity findLivingTargetInLineOfSight(ServerPlayerEntity caster) {
		return findLivingTargetInLineOfSight(caster, LOVE_GAZE_RANGE);
	}

	static LivingEntity findLivingTargetInLineOfSight(ServerPlayerEntity caster, double range) {
		Vec3d eyePos = caster.getEyePos();
		Vec3d look = caster.getRotationVec(1.0F);
		double clampedRange = Math.max(1.0, range);
		Vec3d end = eyePos.add(look.multiply(clampedRange));
		Box area = caster.getBoundingBox().stretch(look.multiply(clampedRange)).expand(1.0);
		Predicate<Entity> filter = entity -> isMagicTargetableEntity(entity) && entity != caster;

		EntityHitResult hitResult = ProjectileUtil.raycast(caster, eyePos, end, area, filter, clampedRange * clampedRange);
		if (hitResult == null || !(hitResult.getEntity() instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
			return null;
		}

		if (!caster.canSee(target)) {
			return null;
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

	static BlockHitResult findTargetedBlock(ServerPlayerEntity caster, double range) {
		Vec3d eyePos = caster.getEyePos();
		Vec3d look = caster.getRotationVec(1.0F);
		Vec3d end = eyePos.add(look.multiply(Math.max(1.0, range)));
		BlockHitResult hitResult = caster.getEntityWorld().raycast(
			new RaycastContext(eyePos, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, caster)
		);
		return hitResult.getType() == HitResult.Type.BLOCK ? hitResult : null;
	}

	static List<LivingEntity> collectLivingEntitiesAlongBeam(
		ServerPlayerEntity caster,
		Vec3d start,
		Vec3d end,
		double radius
	) {
		double minX = Math.min(start.x, end.x);
		double minY = Math.min(start.y, end.y);
		double minZ = Math.min(start.z, end.z);
		double maxX = Math.max(start.x, end.x);
		double maxY = Math.max(start.y, end.y);
		double maxZ = Math.max(start.z, end.z);
		Box searchBox = new Box(minX, minY, minZ, maxX, maxY, maxZ).expand(radius);
		List<LivingEntity> targets = new ArrayList<>();
		Predicate<Entity> filter = entity -> isMagicTargetableEntity(entity) && entity != caster;
		for (Entity entity : caster.getEntityWorld().getOtherEntities(caster, searchBox, filter)) {
			if (entity instanceof LivingEntity livingTarget && livingTarget.getBoundingBox().expand(radius).raycast(start, end).isPresent()) {
				targets.add(livingTarget);
			}
		}
		return targets;
	}

	static ServerPlayerEntity findPlayerTargetInLineOfSight(ServerPlayerEntity caster) {
		return findPlayerTargetInLineOfSight(caster, LOVE_GAZE_RANGE);
	}

	static ServerPlayerEntity findPlayerTargetInLineOfSight(ServerPlayerEntity caster, double range) {
		LivingEntity target = findLivingTargetInLineOfSight(caster, range);
		if (target == null) {
			debugManipulation("{} manipulation target search: no living target found", debugName(caster));
			return null;
		}

		if (target instanceof ServerPlayerEntity targetPlayer && targetPlayer.isAlive() && !targetPlayer.isSpectator()) {
			debugManipulation(
				"{} manipulation target search: resolved player target {} ({})",
				debugName(caster),
				targetPlayer.getName().getString(),
				targetPlayer.getUuid()
			);
			return targetPlayer;
		}

		debugManipulation(
			"{} manipulation target search: living target {} ({}) was not a valid player target (type={}, alive={}, spectator={})",
			debugName(caster),
			target.getName().getString(),
			target.getUuid(),
			target.getClass().getSimpleName(),
			target.isAlive(),
			target instanceof PlayerEntity player && player.isSpectator()
		);
		return null;
	}

	static LivingEntity findAstralCataclysmTargetInLineOfSight(ServerPlayerEntity caster, double range) {
		LivingEntity target = findLivingTargetInLineOfSight(caster, range);
		if (target == null || target == caster) {
			return null;
		}

		if (target instanceof ServerPlayerEntity targetPlayer && targetPlayer.isAlive() && !targetPlayer.isSpectator()) {
			return targetPlayer;
		}

		if (ASTRAL_CATACLYSM_ALLOW_MOB_TARGETS && target instanceof MobEntity) {
			return target;
		}

		return null;
	}

	static void activateHerculesBurden(ServerPlayerEntity caster, List<LivingEntity> targets, int currentTick) {
		UUID casterId = caster.getUuid();
		int endTick = currentTick + HERCULES_EFFECT_DURATION_TICKS;
		HerculesBurdenState state = new HerculesBurdenState(endTick, new HashSet<>());
		HERCULES_STATES.put(casterId, state);
		setActiveAbility(caster, MagicAbility.HERCULES_BURDEN_OF_THE_SKY);
		MagicPlayerData.setDepletedRecoveryMode(caster, false);

		for (LivingEntity target : targets) {
			bindHerculesTarget(casterId, state, target, currentTick, endTick);
			applyHerculesInitialImpact(caster, target);
		}

		caster.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.HERCULES_BURDEN_OF_THE_SKY.displayName()), true);
	}

	static void bindHerculesTarget(UUID casterId, HerculesBurdenState state, LivingEntity target, int currentTick, int endTick) {
		AstralBurdenTargetState existing = HERCULES_TARGETS.get(target.getUuid());
		if (existing != null && !existing.casterId.equals(casterId)) {
			HerculesBurdenState previousState = HERCULES_STATES.get(existing.casterId);
			if (previousState != null) {
				previousState.targetIds.remove(target.getUuid());
			}
		}

		HERCULES_TARGETS.put(
			target.getUuid(),
			new AstralBurdenTargetState(
				target.getEntityWorld().getRegistryKey(),
				casterId,
				target.getX(),
				target.getY(),
				target.getZ(),
				endTick,
				currentTick
			)
		);
		state.targetIds.add(target.getUuid());
	}

	static void applyHerculesInitialImpact(ServerPlayerEntity caster, LivingEntity target) {
		target.setVelocity(0.0, 0.0, 0.0);
		target.setOnGround(true);
		if (HERCULES_INTERRUPT_TARGET_ITEM_USE) {
			target.stopUsingItem();
		}
		if (target instanceof MobEntity mob) {
			mob.getNavigation().stop();
		}
		target.addStatusEffect(
			new StatusEffectInstance(StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, HERCULES_SLOWNESS_AMPLIFIER, true, false, false)
		);
		refreshStatusEffect(target, StatusEffects.RESISTANCE, EFFECT_REFRESH_TICKS, HERCULES_RESISTANCE_AMPLIFIER, true, false, false);
		spawnHerculesImpactBurst(target);
		spawnHerculesBurdenParticles(target);
		if (HERCULES_TRUE_DAMAGE > 0.0F && target.getEntityWorld() instanceof ServerWorld world) {
			dealTrackedMagicDamage(target, caster.getUuid(), createTrueMagicDamageSource(world, caster), HERCULES_TRUE_DAMAGE);
		}
		if (target instanceof ServerPlayerEntity targetPlayer) {
			sendHerculesWarning(targetPlayer);
		}
	}

	static void sendHerculesWarning(ServerPlayerEntity target) {
		ConstellationWarningOverlayPayload payload = new ConstellationWarningOverlayPayload(
			Text.translatable("message.magic.constellation.hercules_warning").getString(),
			HERCULES_WARNING_COLOR_RGB,
			HERCULES_WARNING_SCALE,
			HERCULES_WARNING_FADE_IN_TICKS,
			HERCULES_WARNING_STAY_TICKS,
			HERCULES_WARNING_FADE_OUT_TICKS,
			Integer.MIN_VALUE,
			Integer.MIN_VALUE
		);
		ServerPlayNetworking.send(target, payload);
	}

	static void applyHerculesBurden(ServerPlayerEntity caster, int currentTick) {
		HerculesBurdenState state = HERCULES_STATES.get(caster.getUuid());
		if (state == null) {
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		if (currentTick >= state.endTick) {
			deactivateHerculesBurden(caster, true, true);
		}
	}

	static void updateHerculesBurdenStates(MinecraftServer server, int currentTick) {
		List<UUID> expiredCasters = new ArrayList<>();
		Iterator<Map.Entry<UUID, HerculesBurdenState>> iterator = HERCULES_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, HerculesBurdenState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			if (caster == null || !caster.isAlive() || activeAbility(caster) != MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
				releaseHerculesTargets(entry.getKey());
				iterator.remove();
				continue;
			}

			if (currentTick >= entry.getValue().endTick) {
				expiredCasters.add(entry.getKey());
			}
		}

		for (UUID casterId : expiredCasters) {
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(casterId);
			if (caster != null) {
				deactivateHerculesBurden(caster, true, true);
			}
		}
	}

	static void updateHerculesBurdenTargets(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, AstralBurdenTargetState>> iterator = HERCULES_TARGETS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, AstralBurdenTargetState> entry = iterator.next();
			AstralBurdenTargetState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension);
			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target) || currentTick >= state.endTick) {
				iterator.remove();
				continue;
			}

			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(state.casterId);
			if (caster == null || !caster.isAlive() || activeAbility(caster) != MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
				iterator.remove();
				continue;
			}

			teleportDomainEntity(target, state.lockedX, state.lockedY, state.lockedZ, target.getYaw(), target.getPitch());
			if (HERCULES_INTERRUPT_TARGET_ITEM_USE) {
				target.stopUsingItem();
			}
			refreshStatusEffect(target, StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, HERCULES_SLOWNESS_AMPLIFIER, true, false, false);
			refreshStatusEffect(target, StatusEffects.RESISTANCE, EFFECT_REFRESH_TICKS, HERCULES_RESISTANCE_AMPLIFIER, true, false, false);
			if (target instanceof MobEntity mob) {
				mob.getNavigation().stop();
			}
			if (currentTick - state.lastParticleTick >= HERCULES_PARTICLE_INTERVAL_TICKS) {
				spawnHerculesBurdenParticles(target);
				state.lastParticleTick = currentTick;
			}
		}
	}

	static void deactivateHerculesBurden(ServerPlayerEntity caster, boolean startCooldown, boolean sendFeedback) {
		UUID casterId = caster.getUuid();
		HerculesBurdenState state = HERCULES_STATES.remove(casterId);
		releaseHerculesTargets(casterId);
		if (startCooldown) {
			startAbilityCooldownFromNow(casterId, MagicAbility.HERCULES_BURDEN_OF_THE_SKY, caster.getEntityWorld().getServer().getTicks());
		}
		if (activeAbility(caster) == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			setActiveAbility(caster, MagicAbility.NONE);
		}
		if (sendFeedback && state != null) {
			caster.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.HERCULES_BURDEN_OF_THE_SKY.displayName()), true);
		}
	}

	static void releaseHerculesTargets(UUID casterId) {
		HERCULES_TARGETS.entrySet().removeIf(entry -> entry.getValue().casterId.equals(casterId));
	}

	static void clearSagittariusWindup(ServerPlayerEntity caster) {
		UUID casterId = caster.getUuid();
		CelestialAlignmentSessionState session = SAGITTARIUS_STATES.remove(casterId);
		if (session != null) {
			for (CelestialAlignmentState constellation : new ArrayList<>(session.constellations)) {
				releaseCelestialAlignmentState(caster.getEntityWorld().getServer(), constellation, false);
			}
		}
		SAGITTARIUS_DRAIN_BUFFER.remove(casterId);
		clearCelestialGamaRayState(caster, false, false);
		if (activeAbility(caster) == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			setActiveAbility(caster, MagicAbility.NONE);
		}
	}

	static void applySagittariusAstralArrow(ServerPlayerEntity caster, int currentTick) {
		CelestialAlignmentSessionState session = SAGITTARIUS_STATES.get(caster.getUuid());
		CelestialGamaRayState gamaRayState = CELESTIAL_GAMA_RAY_STATES.get(caster.getUuid());
		if ((session == null || session.constellations.isEmpty()) && gamaRayState == null) {
			clearSagittariusWindup(caster);
		}
	}

	static void updateSagittariusWindups(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, CelestialAlignmentSessionState>> iterator = SAGITTARIUS_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, CelestialAlignmentSessionState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			CelestialAlignmentSessionState session = entry.getValue();
			if (
				caster == null
				|| !caster.isAlive()
				|| activeAbility(caster) != MagicAbility.SAGITTARIUS_ASTRAL_ARROW
				|| caster.getEntityWorld().getRegistryKey() != session.dimension
			) {
				for (CelestialAlignmentState constellation : new ArrayList<>(session.constellations)) {
					releaseCelestialAlignmentState(server, constellation, false);
				}
				SAGITTARIUS_DRAIN_BUFFER.remove(entry.getKey());
				iterator.remove();
				continue;
			}
			if (session.constellations.isEmpty()) {
				SAGITTARIUS_DRAIN_BUFFER.remove(entry.getKey());
				if (!CELESTIAL_GAMA_RAY_STATES.containsKey(entry.getKey()) && activeAbility(caster) == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
					setActiveAbility(caster, MagicAbility.NONE);
				}
				iterator.remove();
				continue;
			}
			for (CelestialAlignmentState constellation : new ArrayList<>(session.constellations)) {
				updateCelestialAlignmentState(server, caster, constellation, currentTick);
			}
		}
		updateCelestialGeminiReplays(server, currentTick);
		updateCelestialGamaRayStates(server, currentTick);
	}
}


