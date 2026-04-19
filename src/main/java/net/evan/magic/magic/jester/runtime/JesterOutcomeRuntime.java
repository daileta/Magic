package net.evan.magic.magic.ability;
import net.evan.magic.magic.core.ability.MagicAbility;

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


abstract class JesterOutcomeRuntime extends ConstellationAdvancedRuntime {
	static void applyComedicRewriteLaunchOutcome(
		ServerPlayerEntity player,
		ServerWorld world,
		PendingComedicRewriteState state
	) {
		double horizontalVelocity = COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.baseHorizontalVelocity()
			+ state.severity() * COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.horizontalVelocityPerSeverity();
		double verticalVelocity = COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.baseVerticalVelocity()
			+ state.severity() * COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.verticalVelocityPerSeverity();
		Vec3d direction = normalizedHorizontalDirection(state.launchDirection(), player);
		player.setVelocity(direction.x * horizontalVelocity, verticalVelocity, direction.z * horizontalVelocity);
		player.fallDistance = 0.0F;
		applyComedicRewriteSlowness(
			player,
			COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.slownessDurationTicks(),
			COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.slownessAmplifier()
		);
		world.spawnParticles(
			ParticleTypes.CLOUD,
			player.getX(),
			player.getBodyY(0.5),
			player.getZ(),
			COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.smokeParticleCount(),
			0.45,
			0.25,
			0.45,
			0.03
		);
		world.spawnParticles(
			ParticleTypes.POOF,
			player.getX(),
			player.getBodyY(0.5),
			player.getZ(),
			COMEDIC_REWRITE_LAUNCHED_THROUGH_THE_SCENE.poofParticleCount(),
			0.35,
			0.25,
			0.35,
			0.02
		);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.9F, 1.5F);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.7F, 1.6F);
	}

	static void applyComedicRewriteRavagerOutcome(
		ServerPlayerEntity player,
		ServerWorld world,
		PendingComedicRewriteState state
	) {
		double horizontalVelocity = COMEDIC_REWRITE_RAVAGER_BIT.baseHorizontalVelocity()
			+ state.severity() * COMEDIC_REWRITE_RAVAGER_BIT.horizontalVelocityPerSeverity();
		double verticalVelocity = COMEDIC_REWRITE_RAVAGER_BIT.baseVerticalVelocity()
			+ state.severity() * COMEDIC_REWRITE_RAVAGER_BIT.verticalVelocityPerSeverity();
		Vec3d direction = normalizedHorizontalDirection(state.launchDirection(), player);
		player.setVelocity(direction.x * horizontalVelocity, verticalVelocity, direction.z * horizontalVelocity);
		player.fallDistance = 0.0F;
		applyComedicRewriteSlowness(
			player,
			COMEDIC_REWRITE_RAVAGER_BIT.slownessDurationTicks(),
			COMEDIC_REWRITE_RAVAGER_BIT.slownessAmplifier()
		);
		world.spawnParticles(
			new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DIRT.getDefaultState()),
			player.getX(),
			player.getY(),
			player.getZ(),
			COMEDIC_REWRITE_RAVAGER_BIT.dustParticleCount(),
			0.55,
			0.15,
			0.55,
			0.08
		);
		world.spawnParticles(
			ParticleTypes.CAMPFIRE_COSY_SMOKE,
			player.getX(),
			player.getBodyY(0.5),
			player.getZ(),
			COMEDIC_REWRITE_RAVAGER_BIT.smokeParticleCount(),
			0.4,
			0.25,
			0.4,
			0.01
		);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 1.1F, 0.85F);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.8F, 0.75F);
		spawnComedicRewriteRavagerVisual(player, world, direction, horizontalVelocity);
	}

	static void applyComedicRewriteParrotOutcome(
		ServerPlayerEntity player,
		ServerWorld world,
		PendingComedicRewriteState state
	) {
		Vec3d safePos = state.safePos();
		if (safePos == null) {
			applyComedicRewriteLaunchOutcome(player, world, state.withOutcome(ComedicRewriteOutcome.LAUNCHED_THROUGH_THE_SCENE));
			return;
		}

		double carryHeight = COMEDIC_REWRITE_PARROT_RESCUE.carryHeight()
			+ state.severity() * COMEDIC_REWRITE_PARROT_RESCUE.carryHeightPerSeverity();
		double liftedY = Math.min(
			safePos.y + carryHeight,
			world.getTopYInclusive() + 1.0 - player.getHeight()
		);
		teleportComedicRewritePlayer(player, world, new Vec3d(safePos.x, liftedY, safePos.z));
		double sideVelocity = COMEDIC_REWRITE_PARROT_RESCUE.sideVelocity()
			+ state.severity() * COMEDIC_REWRITE_PARROT_RESCUE.sideVelocityPerSeverity();
		Vec3d direction = normalizedHorizontalDirection(state.launchDirection(), player);
		player.setVelocity(direction.z * sideVelocity, 0.15, -direction.x * sideVelocity);
		player.fallDistance = 0.0F;
		if (COMEDIC_REWRITE_PARROT_RESCUE.levitationDurationTicks() > 0) {
			player.addStatusEffect(
				new StatusEffectInstance(
					StatusEffects.LEVITATION,
					COMEDIC_REWRITE_PARROT_RESCUE.levitationDurationTicks(),
					0,
					true,
					false,
					true
				)
			);
		}
		if (COMEDIC_REWRITE_PARROT_RESCUE.slowFallingDurationTicks() > 0) {
			player.addStatusEffect(
				new StatusEffectInstance(
					StatusEffects.SLOW_FALLING,
					COMEDIC_REWRITE_PARROT_RESCUE.slowFallingDurationTicks(),
					0,
					true,
					false,
					true
				)
			);
		}
		world.spawnParticles(
			new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.FEATHER)),
			player.getX(),
			player.getBodyY(0.7),
			player.getZ(),
			COMEDIC_REWRITE_PARROT_RESCUE.featherParticleCount(),
			0.45,
			0.35,
			0.45,
			0.03
		);
		world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getBodyY(0.4), player.getZ(), 8, 0.3, 0.25, 0.3, 0.01);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PARROT_AMBIENT, SoundCategory.PLAYERS, 1.0F, 1.2F);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PARROT_FLY, SoundCategory.PLAYERS, 0.9F, 1.0F);
		spawnComedicRewriteParrotVisual(player, world, direction);
	}

	static void applyComedicRewriteSlowness(ServerPlayerEntity player, int durationTicks, int amplifier) {
		if (durationTicks <= 0 || amplifier < 0) {
			return;
		}

		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, durationTicks, amplifier, true, false, true));
	}

	static void spawnComedicRewriteRavagerVisual(
		ServerPlayerEntity player,
		ServerWorld world,
		Vec3d direction,
		double playerHorizontalVelocity
	) {
		if (!COMEDIC_REWRITE_RAVAGER_BIT.showVisualCameo() || COMEDIC_REWRITE_RAVAGER_BIT.visualDurationTicks() <= 0) {
			return;
		}

		Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(world, playerPos);
		if (viewers.isEmpty()) {
			return;
		}

		Vec3d spawnDirection = normalizedHorizontalDirection(direction, player);
		Vec3d spawnPos = playerPos
			.add(0.0, COMEDIC_REWRITE_RAVAGER_BIT.visualVerticalOffset(), 0.0)
			.subtract(spawnDirection.multiply(COMEDIC_REWRITE_RAVAGER_BIT.visualSpawnDistance()));
		float yaw = horizontalYawFromDirection(spawnDirection);
		RavagerEntity ravager = new RavagerEntity(EntityType.RAVAGER, world);
		ravager.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, yaw, 0.0F);
		ravager.setBodyYaw(yaw);
		ravager.setHeadYaw(yaw);
		ravager.setOnGround(player.isOnGround());
		ravager.setAiDisabled(true);
		ravager.setSilent(true);
		double chargeVelocity = Math.max(
			COMEDIC_REWRITE_RAVAGER_BIT.visualChargeVelocity(),
			Math.max(0.0, playerHorizontalVelocity) + COMEDIC_REWRITE_RAVAGER_BIT.visualChargeVelocityBuffer()
		);
		ravager.setVelocity(spawnDirection.multiply(chargeVelocity));
		queueComedicRewriteVisualCameo(
			world.getServer(),
			COMEDIC_REWRITE_RAVAGER_BIT.visualDurationTicks(),
			viewers,
			COMEDIC_REWRITE_RAVAGER_BIT.visualChargeDurationTicks(),
			null,
			ravager
		);
	}

	static void spawnComedicRewriteParrotVisual(ServerPlayerEntity player, ServerWorld world, Vec3d direction) {
		if (!COMEDIC_REWRITE_PARROT_RESCUE.showVisualCameo()
			|| COMEDIC_REWRITE_PARROT_RESCUE.visualDurationTicks() <= 0
			|| COMEDIC_REWRITE_PARROT_RESCUE.visualCount() <= 0) {
			return;
		}

		Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(world, playerPos);
		if (viewers.isEmpty()) {
			return;
		}

		Vec3d carryDirection = normalizedHorizontalDirection(direction, player);
		Vec3d carryVelocity = player.getVelocity().add(0.0, COMEDIC_REWRITE_PARROT_RESCUE.visualLiftVelocity(), 0.0);
		float yaw = horizontalYawFromDirection(carryVelocity.lengthSquared() > 1.0E-5 ? carryVelocity : carryDirection);
		List<Entity> parrots = new ArrayList<>();
		Vec3d[] headOffsets = new Vec3d[COMEDIC_REWRITE_PARROT_RESCUE.visualCount()];
		for (int index = 0; index < COMEDIC_REWRITE_PARROT_RESCUE.visualCount(); index++) {
			double angle = (Math.PI * 2.0 * index) / COMEDIC_REWRITE_PARROT_RESCUE.visualCount();
			double offsetX = Math.cos(angle) * COMEDIC_REWRITE_PARROT_RESCUE.visualRadius();
			double offsetZ = Math.sin(angle) * COMEDIC_REWRITE_PARROT_RESCUE.visualRadius();
			Vec3d headOffset = new Vec3d(
				offsetX,
				COMEDIC_REWRITE_PARROT_RESCUE.visualVerticalOffset() + (index % 2 == 0 ? 0.1 : 0.0),
				offsetZ
			);
			headOffsets[index] = headOffset;
			Vec3d spawnPos = new Vec3d(player.getX(), player.getY() + player.getHeight(), player.getZ()).add(headOffset);
			ParrotEntity parrot = new ParrotEntity(EntityType.PARROT, world);
			parrot.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, yaw, 0.0F);
			parrot.setBodyYaw(yaw);
			parrot.setHeadYaw(yaw);
			parrot.setAiDisabled(true);
			parrot.setSilent(true);
			parrot.setNoGravity(true);
			parrot.setComponent(
				DataComponentTypes.PARROT_VARIANT,
				ParrotEntity.Variant.byIndex(player.getRandom().nextInt(ParrotEntity.Variant.values().length))
			);
			parrot.setVelocity(
				COMEDIC_REWRITE_PARROT_RESCUE.visualFollowPlayerHead()
					? Vec3d.ZERO
					: carryVelocity.add(offsetX * 0.08, 0.0, offsetZ * 0.08)
			);
			parrots.add(parrot);
		}
		queueComedicRewriteVisualCameo(
			world.getServer(),
			COMEDIC_REWRITE_PARROT_RESCUE.visualDurationTicks(),
			viewers,
			0,
			COMEDIC_REWRITE_PARROT_RESCUE.visualFollowPlayerHead()
				? new ComedicRewriteVisualFollowState(
					world.getRegistryKey(),
					player.getUuid(),
					headOffsets,
					ComedicRewriteVisualFollowMode.ENTITY_TOP,
					0.0
				)
				: null,
			parrots.toArray(Entity[]::new)
		);
	}

	static boolean hasEnabledComedicAssistantOutcomes() {
		return COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.enabled()
			|| COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.enabled()
			|| COMEDIC_ASSISTANT_PARROT_KIDNAPPING.enabled()
			|| COMEDIC_ASSISTANT_DIVINE_OVERREACTION.enabled()
			|| COMEDIC_ASSISTANT_ACME_DROP.enabled()
			|| COMEDIC_ASSISTANT_PIE_TO_THE_FACE.enabled()
			|| COMEDIC_ASSISTANT_GIANT_CANE_YANK.enabled();
	}

	static boolean isValidComedicAssistantTarget(LivingEntity target) {
		if (target == null || !target.isAlive()) {
			return false;
		}
		if (target instanceof ServerPlayerEntity playerTarget) {
			return COMEDIC_ASSISTANT_ALLOW_PLAYER_TARGETS && !playerTarget.isSpectator();
		}
		return COMEDIC_ASSISTANT_ALLOW_MOB_TARGETS && target instanceof MobEntity;
	}

	static void tryTriggerComedicAssistant(ServerPlayerEntity attacker, LivingEntity target, float damageTaken) {
		ComedicAssistantArmedState state = COMEDIC_ASSISTANT_ARMED_STATES.get(attacker.getUuid());
		if (state == null || !(target.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		int currentTick = world.getServer().getTicks();
		if (!shouldKeepComedicAssistantEnabled(attacker) || attacker.getEntityWorld().getRegistryKey() != state.dimension()) {
			clearComedicAssistantState(attacker.getUuid());
			return;
		}
		if (currentTick >= state.expiresTick()) {
			deactivateComedicAssistant(attacker, currentTick, COMEDIC_ASSISTANT_CANCEL_COOLDOWN_TICKS, false, true);
			return;
		}

		ComedicAssistantOutcome outcome = selectComedicAssistantOutcome(attacker);
		if (outcome == null) {
			clearComedicAssistantState(attacker.getUuid());
			attacker.sendMessage(Text.translatable("message.magic.jester.comedic_assistant.no_outcomes"), true);
			return;
		}

		clearComedicAssistantState(attacker.getUuid());
		startComedicAssistantCooldown(attacker.getUuid(), currentTick, COMEDIC_ASSISTANT_PROC_COOLDOWN_TICKS);
		attacker.sendMessage(Text.translatable("message.magic.jester.comedic_assistant.proc", outcome.displayName()), true);
		sendComedicAssistantProcOverlay(attacker, target, outcome);
		applyComedicAssistantOutcome(outcome, attacker, target, world, currentTick, damageTaken);
	}

	static void tryTriggerPlusUltraHit(ServerPlayerEntity attacker, LivingEntity target, float damageTaken) {
		if (!hasActivePlusUltra(attacker) || !(target.getEntityWorld() instanceof ServerWorld world) || !isValidPlusUltraTarget(target)) {
			return;
		}

		PlusUltraState state = PLUS_ULTRA_STATES.get(attacker.getUuid());
		if (state == null || attacker.getEntityWorld().getRegistryKey() != state.dimension() || !shouldKeepPlusUltraEnabled(attacker)) {
			return;
		}

		Vec3d hitPos = new Vec3d(target.getX(), target.getBodyY(0.45), target.getZ());
		world.spawnParticles(
			ParticleTypes.CLOUD,
			hitPos.x,
			hitPos.y,
			hitPos.z,
			PLUS_ULTRA_SMOKE_PARTICLE_COUNT,
			PLUS_ULTRA_SMOKE_PARTICLE_SPREAD,
			PLUS_ULTRA_SMOKE_PARTICLE_SPREAD * 0.6,
			PLUS_ULTRA_SMOKE_PARTICLE_SPREAD,
			PLUS_ULTRA_SMOKE_PARTICLE_SPEED
		);
		world.spawnParticles(
			ParticleTypes.DUST_PLUME,
			hitPos.x,
			hitPos.y,
			hitPos.z,
			PLUS_ULTRA_HIT_DUST_PARTICLE_COUNT,
			PLUS_ULTRA_HIT_DUST_PARTICLE_SPREAD,
			PLUS_ULTRA_HIT_DUST_PARTICLE_SPREAD * 0.5,
			PLUS_ULTRA_HIT_DUST_PARTICLE_SPREAD,
			PLUS_ULTRA_HIT_DUST_PARTICLE_SPEED
		);
		applyComedicAssistantBonusDamage(attacker, target, world, PLUS_ULTRA_MELEE_BONUS_DAMAGE);
		if (!target.isAlive()) {
			return;
		}

		launchTarget(target, resolvePlusUltraLaunchDirection(attacker, target), PLUS_ULTRA_FLING_HORIZONTAL_STRENGTH, PLUS_ULTRA_FLING_VERTICAL_STRENGTH);
		trackPlusUltraImpact(attacker, target, world);
	}

	static boolean isValidPlusUltraTarget(LivingEntity target) {
		if (target == null || !target.isAlive()) {
			return false;
		}
		if (target instanceof ServerPlayerEntity) {
			return PLUS_ULTRA_ALLOW_PLAYER_TARGETS;
		}
		return PLUS_ULTRA_ALLOW_MOB_TARGETS && target instanceof MobEntity;
	}

	static Vec3d resolvePlusUltraLaunchDirection(ServerPlayerEntity attacker, LivingEntity target) {
		Vec3d facing = attacker.getRotationVector();
		Vec3d horizontalFacing = new Vec3d(facing.x, 0.0, facing.z);
		if (horizontalFacing.lengthSquared() > 1.0E-5) {
			return horizontalFacing.normalize();
		}
		return directionFromAttackerToTarget(attacker, target);
	}

	static void trackPlusUltraImpact(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		MinecraftServer server = world.getServer();
		if (server == null || PLUS_ULTRA_IMPACT_TRACKING_TICKS <= 0 || PLUS_ULTRA_IMPACT_DAMAGE_MAX <= 0.0) {
			return;
		}

		int currentTick = server.getTicks();
		PLUS_ULTRA_IMPACT_STATES.put(
			target.getUuid(),
			new PlusUltraImpactState(
				world.getRegistryKey(),
				attacker.getUuid(),
				currentTick,
				currentTick + PLUS_ULTRA_IMPACT_TRACKING_TICKS,
				entityPosition(target),
				target.getVelocity(),
				target.getVelocity().length(),
				false
			)
		);
	}

	static void deactivateComedicAssistant(
		ServerPlayerEntity player,
		int currentTick,
		int cooldownTicks,
		boolean manualCancel,
		boolean expired
	) {
		if (COMEDIC_ASSISTANT_ARMED_STATES.remove(player.getUuid()) == null) {
			return;
		}

		startComedicAssistantCooldown(player.getUuid(), currentTick, cooldownTicks);
		if (manualCancel) {
			player.sendMessage(Text.translatable("message.magic.jester.comedic_assistant.cancelled"), true);
			return;
		}
		if (expired) {
			player.sendMessage(Text.translatable("message.magic.jester.comedic_assistant.expired"), true);
		}
	}

	static void clearComedicAssistantState(UUID playerId) {
		COMEDIC_ASSISTANT_ARMED_STATES.remove(playerId);
	}

	static boolean clearComedicAssistantCaneImpactState(UUID playerId) {
		boolean removed = COMEDIC_ASSISTANT_CANE_IMPACT_STATES.remove(playerId) != null;
		removed |= COMEDIC_ASSISTANT_CANE_IMPACT_STATES.entrySet().removeIf(entry -> entry.getValue().casterId().equals(playerId));
		return removed;
	}

	static ComedicAssistantOutcome selectComedicAssistantOutcome(ServerPlayerEntity player) {
		int slimeWeight = COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.enabled() ? Math.max(0, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.weight()) : 0;
		int pandaWeight = COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.enabled() ? Math.max(0, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.weight()) : 0;
		int parrotWeight = COMEDIC_ASSISTANT_PARROT_KIDNAPPING.enabled() ? Math.max(0, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.weight()) : 0;
		int divineWeight = COMEDIC_ASSISTANT_DIVINE_OVERREACTION.enabled() ? Math.max(0, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.weight()) : 0;
		int acmeWeight = COMEDIC_ASSISTANT_ACME_DROP.enabled() ? Math.max(0, COMEDIC_ASSISTANT_ACME_DROP.weight()) : 0;
		int pieWeight = COMEDIC_ASSISTANT_PIE_TO_THE_FACE.enabled() ? Math.max(0, COMEDIC_ASSISTANT_PIE_TO_THE_FACE.weight()) : 0;
		int caneWeight = COMEDIC_ASSISTANT_GIANT_CANE_YANK.enabled() ? Math.max(0, COMEDIC_ASSISTANT_GIANT_CANE_YANK.weight()) : 0;
		int totalWeight = slimeWeight + pandaWeight + parrotWeight + divineWeight + acmeWeight + pieWeight + caneWeight;
		if (totalWeight <= 0) {
			return null;
		}

		int roll = player.getRandom().nextInt(totalWeight);
		if (roll < slimeWeight) {
			return ComedicAssistantOutcome.GIANT_SLIME_SLAM;
		}
		roll -= slimeWeight;
		if (roll < pandaWeight) {
			return ComedicAssistantOutcome.PANDA_BOWLING_BALL;
		}
		roll -= pandaWeight;
		if (roll < parrotWeight) {
			return ComedicAssistantOutcome.PARROT_KIDNAPPING;
		}
		roll -= parrotWeight;
		if (roll < divineWeight) {
			return ComedicAssistantOutcome.DIVINE_OVERREACTION;
		}
		roll -= divineWeight;
		if (roll < acmeWeight) {
			return ComedicAssistantOutcome.ACME_DROP;
		}
		roll -= acmeWeight;
		if (roll < pieWeight) {
			return ComedicAssistantOutcome.PIE_TO_THE_FACE;
		}
		return ComedicAssistantOutcome.GIANT_CANE_YANK;
	}

	static void sendComedicAssistantProcOverlay(ServerPlayerEntity attacker, LivingEntity target, ComedicAssistantOutcome outcome) {
		JesterJokeOverlayPayload payload = new JesterJokeOverlayPayload(
			outcome.displayName().getString(),
			COMEDIC_ASSISTANT_OVERLAY_COLOR_RGB,
			COMEDIC_ASSISTANT_OVERLAY_FADE_IN_TICKS,
			COMEDIC_ASSISTANT_OVERLAY_STAY_TICKS,
			COMEDIC_ASSISTANT_OVERLAY_FADE_OUT_TICKS
		);
		ServerPlayNetworking.send(attacker, payload);
		if (target instanceof ServerPlayerEntity targetPlayer && targetPlayer != attacker) {
			ServerPlayNetworking.send(targetPlayer, payload);
		}
	}

	static void applyComedicAssistantOutcome(
		ComedicAssistantOutcome outcome,
		ServerPlayerEntity attacker,
		LivingEntity target,
		ServerWorld world,
		int currentTick,
		float damageTaken
	) {
		switch (outcome) {
			case GIANT_SLIME_SLAM -> applyComedicAssistantSlimeSlam(attacker, target, world);
			case PANDA_BOWLING_BALL -> applyComedicAssistantPandaBowlingBall(attacker, target, world);
			case PARROT_KIDNAPPING -> applyComedicAssistantParrotKidnapping(attacker, target, world, currentTick);
			case DIVINE_OVERREACTION -> applyComedicAssistantDivineOverreaction(attacker, target, world, damageTaken);
			case ACME_DROP -> applyComedicAssistantAcmeDrop(attacker, target, world);
			case PIE_TO_THE_FACE -> applyComedicAssistantPieToTheFace(attacker, target, world);
			case GIANT_CANE_YANK -> applyComedicAssistantGiantCaneYank(attacker, target, world);
		}
	}

	static void applyComedicAssistantSlimeSlam(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		Vec3d impactPos = new Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ());
		playConfiguredSound(world, impactPos.add(0.0, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualSpawnHeight(), 0.0), SoundEvents.ENTITY_SLIME_JUMP, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.spawnSoundVolume(), COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.spawnSoundPitch());
		playConfiguredSound(world, impactPos, SoundEvents.ENTITY_SLIME_SQUISH, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.impactSoundVolume(), COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.impactSoundPitch());
		spawnComedicAssistantBlockParticles(world, Blocks.SLIME_BLOCK.getDefaultState(), impactPos, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.particleCount(), 0.45, 0.1);
		spawnComedicAssistantSlimeVisual(target, world);
		if (!target.isAlive()) {
			return;
		}

		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.bonusDamage());
		addConfiguredStatusEffect(target, StatusEffects.SLOWNESS, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.slownessDurationTicks(), COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.slownessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.OOZING, COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.oozingDurationTicks(), COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.oozingAmplifier());
	}

	static void applyComedicAssistantPandaBowlingBall(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		Vec3d launchDirection = directionFromAttackerToTarget(attacker, target);
		Vec3d impactPos = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.45, target.getZ());
		playConfiguredSound(world, impactPos, SoundEvents.ENTITY_PANDA_BITE, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.rollSoundVolume(), COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.rollSoundPitch());
		playConfiguredSound(world, impactPos, SoundEvents.ENTITY_PANDA_HURT, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.impactSoundVolume(), COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.impactSoundPitch());
		spawnComedicAssistantItemParticles(world, new ItemStack(Items.BAMBOO), impactPos, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.particleCount(), 0.35, 0.14);
		spawnComedicAssistantPandaVisual(target, world, launchDirection);
		if (!target.isAlive()) {
			return;
		}

		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.bonusDamage());
		launchTarget(target, launchDirection, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.horizontalLaunch(), COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.verticalLaunch());
		addConfiguredStatusEffect(target, StatusEffects.SLOWNESS, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.slownessDurationTicks(), COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.slownessAmplifier());
	}

	static void applyComedicAssistantParrotKidnapping(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world, int currentTick) {
		Vec3d impactPos = new Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ());
		playConfiguredSound(world, impactPos, SoundEvents.ENTITY_PARROT_FLY, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.soundVolume(), COMEDIC_ASSISTANT_PARROT_KIDNAPPING.soundPitch());
		spawnComedicAssistantItemParticles(world, new ItemStack(Items.FEATHER), impactPos, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.particleCount(), 0.55, 0.12);
		spawnComedicAssistantParrotVisual(target, world);
		if (!target.isAlive()) {
			return;
		}

		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.bonusDamage());
		double targetY = resolveComedicAssistantCarryTargetY(target, world, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.liftHeight());
		if (targetY > target.getY() + 1.0 && COMEDIC_ASSISTANT_PARROT_KIDNAPPING.maxCarryTicks() > 0) {
			COMEDIC_ASSISTANT_PARROT_CARRY_STATES.put(
				target.getUuid(),
				new ComedicAssistantParrotCarryState(
					world.getRegistryKey(),
					targetY,
					currentTick + COMEDIC_ASSISTANT_PARROT_KIDNAPPING.maxCarryTicks(),
					currentTick,
					currentTick,
					COMEDIC_ASSISTANT_PARROT_KIDNAPPING
				)
			);
			target.fallDistance = 0.0F;
		} else {
			launchTarget(target, new Vec3d(0.0, 1.0, 0.0), 0.0, Math.max(0.6, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.upwardVelocity()));
		}
		if (COMEDIC_ASSISTANT_PARROT_KIDNAPPING.applyGlowing()) {
			addConfiguredStatusEffect(target, StatusEffects.GLOWING, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.glowingDurationTicks(), 0);
		}
	}

	static void applyComedicAssistantDivineOverreaction(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world, float damageTaken) {
		Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ());
		playConfiguredSound(world, targetPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.soundVolume(), COMEDIC_ASSISTANT_DIVINE_OVERREACTION.soundPitch());
		playConfiguredSound(world, targetPos, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.soundVolume() * 0.85F, Math.max(0.1F, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.soundPitch() + 0.15F));
		int strikeCount = Math.max(1, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.strikeCount());
		spawnComedicAssistantDivineLightningVisual(target, world, strikeCount);
		for (int index = 0; index < strikeCount; index++) {
			double angle = (Math.PI * 2.0 * index) / strikeCount;
			double radius = target.getRandom().nextDouble() * COMEDIC_ASSISTANT_DIVINE_OVERREACTION.strikeRadius();
			double x = targetPos.x + Math.cos(angle) * radius;
			double z = targetPos.z + Math.sin(angle) * radius;
			world.spawnParticles(
				ParticleTypes.ELECTRIC_SPARK,
				x,
				targetPos.y + target.getRandom().nextDouble() * Math.max(1.0, target.getHeight()),
				z,
				Math.max(1, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.particleCount() / Math.max(1, strikeCount / 2)),
				0.08,
				0.5,
				0.08,
				0.25
			);
			world.spawnParticles(ParticleTypes.GLOW, x, targetPos.y + 0.2, z, 2, 0.02, 0.2, 0.02, 0.01);
		}
		world.spawnParticles(
			ParticleTypes.POOF,
			targetPos.x,
			targetPos.y,
			targetPos.z,
			Math.max(6, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.particleCount() / 2),
			0.45,
			0.4,
			0.45,
			0.06
		);
		if (!target.isAlive()) {
			return;
		}

		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.bonusDamage());
		addConfiguredStatusEffect(target, StatusEffects.GLOWING, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.glowingDurationTicks(), 0);
		addConfiguredStatusEffect(target, StatusEffects.BLINDNESS, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.blindnessDurationTicks(), COMEDIC_ASSISTANT_DIVINE_OVERREACTION.blindnessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.NAUSEA, COMEDIC_ASSISTANT_DIVINE_OVERREACTION.nauseaDurationTicks(), COMEDIC_ASSISTANT_DIVINE_OVERREACTION.nauseaAmplifier());
	}

	static void applyComedicAssistantAcmeDrop(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ());
		playConfiguredSound(world, targetPos, SoundEvents.BLOCK_ANVIL_LAND, COMEDIC_ASSISTANT_ACME_DROP.soundVolume(), COMEDIC_ASSISTANT_ACME_DROP.soundPitch());
		spawnComedicAssistantBlockParticles(world, Blocks.ANVIL.getDefaultState(), targetPos, COMEDIC_ASSISTANT_ACME_DROP.particleCount(), 0.35, 0.12);
		world.spawnParticles(
			ParticleTypes.CAMPFIRE_COSY_SMOKE,
			targetPos.x,
			target.getY() + 0.05,
			targetPos.z,
			Math.max(4, COMEDIC_ASSISTANT_ACME_DROP.particleCount() / 2),
			0.35,
			0.05,
			0.35,
			0.02
		);
		spawnComedicAssistantAcmeVisual(target, world);
		if (!target.isAlive()) {
			return;
		}

		target.setVelocity(0.0, Math.min(0.0, target.getVelocity().y), 0.0);
		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_ACME_DROP.bonusDamage());
		addConfiguredStatusEffect(target, StatusEffects.SLOWNESS, COMEDIC_ASSISTANT_ACME_DROP.slownessDurationTicks(), COMEDIC_ASSISTANT_ACME_DROP.slownessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.WEAKNESS, COMEDIC_ASSISTANT_ACME_DROP.weaknessDurationTicks(), COMEDIC_ASSISTANT_ACME_DROP.weaknessAmplifier());
	}

	static void applyComedicAssistantPieToTheFace(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		Vec3d facePos = new Vec3d(target.getX(), target.getEyeY(), target.getZ());
		playConfiguredSound(world, facePos, SoundEvents.ENTITY_GENERIC_SPLASH, COMEDIC_ASSISTANT_PIE_TO_THE_FACE.soundVolume(), COMEDIC_ASSISTANT_PIE_TO_THE_FACE.soundPitch());
		spawnComedicAssistantItemParticles(world, new ItemStack(Items.PUMPKIN_PIE), facePos, COMEDIC_ASSISTANT_PIE_TO_THE_FACE.particleCount(), 0.16, 0.08);
		if (!target.isAlive()) {
			return;
		}

		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_PIE_TO_THE_FACE.bonusDamage());
		addConfiguredStatusEffect(target, StatusEffects.BLINDNESS, COMEDIC_ASSISTANT_PIE_TO_THE_FACE.blindnessDurationTicks(), COMEDIC_ASSISTANT_PIE_TO_THE_FACE.blindnessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.NAUSEA, COMEDIC_ASSISTANT_PIE_TO_THE_FACE.nauseaDurationTicks(), COMEDIC_ASSISTANT_PIE_TO_THE_FACE.nauseaAmplifier());
	}

	static void applyComedicAssistantGiantCaneYank(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		Vec3d yankDirection = sidewaysDirectionFromAttackerToTarget(attacker, target);
		Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.4, target.getZ());
		playConfiguredSound(world, targetPos, SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE, COMEDIC_ASSISTANT_GIANT_CANE_YANK.soundVolume(), COMEDIC_ASSISTANT_GIANT_CANE_YANK.soundPitch());
		spawnComedicAssistantItemParticles(world, new ItemStack(Items.STICK), targetPos, COMEDIC_ASSISTANT_GIANT_CANE_YANK.particleCount(), 0.28, 0.12);
		world.spawnParticles(
			ParticleTypes.CLOUD,
			targetPos.x,
			targetPos.y,
			targetPos.z,
			Math.max(2, COMEDIC_ASSISTANT_GIANT_CANE_YANK.particleCount() / 2),
			0.25,
			0.2,
			0.25,
			0.03
		);
		spawnComedicAssistantCaneVisual(target, world, yankDirection);
		if (!target.isAlive()) {
			return;
		}

		applyComedicAssistantBonusDamage(attacker, target, world, COMEDIC_ASSISTANT_GIANT_CANE_YANK.bonusDamage());
		launchTarget(target, yankDirection, COMEDIC_ASSISTANT_GIANT_CANE_YANK.horizontalLaunch(), COMEDIC_ASSISTANT_GIANT_CANE_YANK.verticalLaunch());
		trackComedicAssistantCaneImpact(attacker, target, world);
		addConfiguredStatusEffect(target, StatusEffects.SLOWNESS, COMEDIC_ASSISTANT_GIANT_CANE_YANK.slownessDurationTicks(), COMEDIC_ASSISTANT_GIANT_CANE_YANK.slownessAmplifier());
	}

	static void applyComedicAssistantBonusDamage(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world, float bonusDamage) {
		if (bonusDamage <= 0.0F || !target.isAlive()) {
			return;
		}
		dealTrackedMagicDamage(target, attacker.getUuid(), world.getDamageSources().genericKill(), bonusDamage);
	}

	static void launchTarget(LivingEntity target, Vec3d direction, double horizontalVelocity, double verticalVelocity) {
		Vec3d launchDirection = normalizedHorizontalDirection(direction, target instanceof ServerPlayerEntity player ? player : null);
		applyForcedVelocity(target, new Vec3d(launchDirection.x * horizontalVelocity, verticalVelocity, launchDirection.z * horizontalVelocity));
	}

	static void applyForcedVelocity(LivingEntity target, Vec3d velocity) {
		target.setVelocity(velocity);
		target.setOnGround(false);
		target.fallDistance = 0.0F;
		target.velocityDirty = true;
		if (target instanceof ServerPlayerEntity playerTarget) {
			playerTarget.networkHandler.requestTeleport(
				new EntityPosition(new Vec3d(playerTarget.getX(), playerTarget.getY(), playerTarget.getZ()), playerTarget.getVelocity(), playerTarget.getYaw(), playerTarget.getPitch()),
				Set.of()
			);
			playerTarget.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(playerTarget.getId(), playerTarget.getVelocity()));
		}
	}

	static void forceTargetPositionAndVelocity(ServerWorld world, LivingEntity target, Vec3d targetPos, Vec3d velocity) {
		if (target.hasVehicle()) {
			target.stopRiding();
		}

		if (target instanceof ServerPlayerEntity playerTarget) {
			playerTarget.teleport(world, targetPos.x, targetPos.y, targetPos.z, Set.<PositionFlag>of(), playerTarget.getYaw(), playerTarget.getPitch(), false);
		} else {
			target.requestTeleport(targetPos.x, targetPos.y, targetPos.z);
		}
		applyForcedVelocity(target, velocity);
	}

	static Vec3d entityPosition(Entity entity) {
		return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
	}

	static void trackComedicAssistantCaneImpact(ServerPlayerEntity attacker, LivingEntity target, ServerWorld world) {
		MinecraftServer server = world.getServer();
		if (server == null || COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageTrackingTicks() <= 0 || COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageMax() <= 0.0) {
			return;
		}

		int currentTick = server.getTicks();
		COMEDIC_ASSISTANT_CANE_IMPACT_STATES.put(
			target.getUuid(),
			new ComedicAssistantCaneImpactState(
				world.getRegistryKey(),
				attacker.getUuid(),
				currentTick,
				currentTick + COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageTrackingTicks(),
				currentTick + COMEDIC_ASSISTANT_GIANT_CANE_YANK.launchControlTicks(),
				target.getVelocity(),
				entityPosition(target),
				target.getVelocity(),
				target.getVelocity().length()
			)
		);
	}

	static Vec3d directionFromAttackerToTarget(ServerPlayerEntity attacker, LivingEntity target) {
		Vec3d direction = new Vec3d(target.getX() - attacker.getX(), 0.0, target.getZ() - attacker.getZ());
		if (direction.lengthSquared() > 1.0E-5) {
			return direction.normalize();
		}
		Vec3d facing = attacker.getRotationVector();
		return normalizedHorizontalDirection(new Vec3d(facing.x, 0.0, facing.z), attacker);
	}

	static Vec3d sidewaysDirectionFromAttackerToTarget(ServerPlayerEntity attacker, LivingEntity target) {
		Vec3d baseDirection = directionFromAttackerToTarget(attacker, target);
		Vec3d sideways = new Vec3d(-baseDirection.z, 0.0, baseDirection.x);
		if (target.getRandom().nextBoolean()) {
			sideways = sideways.multiply(-1.0);
		}
		return sideways.normalize();
	}

	static void spawnComedicAssistantBlockParticles(
		ServerWorld world,
		BlockState state,
		Vec3d center,
		int count,
		double spread,
		double speed
	) {
		if (count <= 0) {
			return;
		}
		world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), center.x, center.y, center.z, count, spread, spread, spread, speed);
	}

	static void spawnComedicAssistantItemParticles(
		ServerWorld world,
		ItemStack stack,
		Vec3d center,
		int count,
		double spread,
		double speed
	) {
		if (count <= 0) {
			return;
		}
		world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stack), center.x, center.y, center.z, count, spread, spread, spread, speed);
	}

	static void playConfiguredSound(ServerWorld world, Vec3d pos, SoundEvent sound, float volume, float pitch) {
		if (world == null || sound == null || volume <= 0.0F || pitch <= 0.0F) {
			return;
		}
		world.playSound(null, pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, volume, pitch);
	}
}


