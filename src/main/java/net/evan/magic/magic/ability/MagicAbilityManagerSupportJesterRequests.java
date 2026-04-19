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


abstract class MagicAbilityManagerSupportJesterRequests extends MagicAbilityManagerSupportServerTick {
	static void handleSpotlightRequest(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		if (SPOTLIGHT_PASSIVE_ENABLED.contains(playerId)) {
			deactivateSpotlight(player, true);
			player.sendMessage(Text.translatable("message.magic.ability.passive_disabled", MagicAbility.SPOTLIGHT.displayName()), false);
			return;
		}

		SPOTLIGHT_PASSIVE_ENABLED.add(playerId);
		SPOTLIGHT_STATES.computeIfAbsent(playerId, ignored -> new SpotlightState());
		player.sendMessage(Text.translatable("message.magic.ability.passive_enabled", MagicAbility.SPOTLIGHT.displayName()), false);
		recordOrionsGambitAbilityUse(player, MagicAbility.SPOTLIGHT);
	}

	static void handleCassiopeiaRequest(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		if (CASSIOPEIA_PASSIVE_ENABLED.contains(playerId)) {
			deactivateCassiopeia(player, true);
			player.sendMessage(Text.translatable("message.magic.ability.passive_disabled", MagicAbility.CASSIOPEIA.displayName()), false);
			return;
		}

		CASSIOPEIA_PASSIVE_ENABLED.add(playerId);
		CASSIOPEIA_LAST_OUTLINED_PLAYERS.remove(playerId);
		player.sendMessage(Text.translatable("message.magic.ability.passive_enabled", MagicAbility.CASSIOPEIA.displayName()), false);
		recordOrionsGambitAbilityUse(player, MagicAbility.CASSIOPEIA);
	}

	static void handleComedicRewriteRequest(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		if (COMEDIC_REWRITE_PASSIVE_ENABLED.contains(playerId)) {
			deactivateComedicRewrite(player);
			player.sendMessage(Text.translatable("message.magic.ability.passive_disabled", MagicAbility.COMEDIC_REWRITE.displayName()), false);
			return;
		}

		COMEDIC_REWRITE_PASSIVE_ENABLED.add(playerId);
		player.sendMessage(Text.translatable("message.magic.ability.passive_enabled", MagicAbility.COMEDIC_REWRITE.displayName()), false);
		recordOrionsGambitAbilityUse(player, MagicAbility.COMEDIC_REWRITE);
	}

	static void handleComedicAssistantRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		ComedicAssistantArmedState existingState = COMEDIC_ASSISTANT_ARMED_STATES.get(player.getUuid());
		if (existingState != null) {
			deactivateComedicAssistant(player, currentTick, COMEDIC_ASSISTANT_CANCEL_COOLDOWN_TICKS, true, false);
			return;
		}

		int remainingTicks = comedicAssistantCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.COMEDIC_ASSISTANT, remainingTicks, false);
			return;
		}

		if (!hasEnabledComedicAssistantOutcomes()) {
			player.sendMessage(Text.translatable("message.magic.jester.comedic_assistant.no_outcomes"), true);
			return;
		}

		int manaCost = (int) Math.ceil(manaFromPercentExact(COMEDIC_ASSISTANT_ACTIVATION_COST_PERCENT));
		if (!canSpendAbilityCost(player, manaCost)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		spendAbilityCost(player, manaCost);
		COMEDIC_ASSISTANT_ARMED_STATES.put(
			player.getUuid(),
			new ComedicAssistantArmedState(
				player.getEntityWorld().getRegistryKey(),
				currentTick + COMEDIC_ASSISTANT_ARMED_DURATION_TICKS,
				currentTick + COMEDIC_ASSISTANT_ARMED_INDICATOR_REFRESH_TICKS
			)
		);
		sendComedicAssistantArmedIndicator(player);
		spawnComedicAssistantArmedParticles(player);
		recordOrionsGambitAbilityUse(player, MagicAbility.COMEDIC_ASSISTANT);
	}

	static void handlePlusUltraRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		PlusUltraState activeState = PLUS_ULTRA_STATES.get(player.getUuid());
		if (activeState != null) {
			PlusUltraEndMode endMode = currentTick < activeState.halfwayTick() ? PlusUltraEndMode.MANUAL_EARLY : PlusUltraEndMode.FULL;
			endPlusUltra(player, currentTick, endMode, true);
			return;
		}

		int remainingTicks = plusUltraCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.PLUS_ULTRA, remainingTicks, false);
			return;
		}

		int manaCost = (int) Math.ceil(manaFromPercentExact(PLUS_ULTRA_ACTIVATION_COST_PERCENT));
		if (!canSpendAbilityCost(player, manaCost)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		spendAbilityCost(player, manaCost);
		activatePlusUltra(player, currentTick);
		recordOrionsGambitAbilityUse(player, MagicAbility.PLUS_ULTRA);
	}

	static void activatePlusUltra(ServerPlayerEntity player, int currentTick) {
		UUID playerId = player.getUuid();
		PlusUltraState previousState = PLUS_ULTRA_STATES.remove(playerId);
		if (previousState != null) {
			removePlusUltraOverheadText(player.getEntityWorld().getServer(), previousState);
		}

		boolean hadAllowFlying = player.getAbilities().allowFlying;
		boolean hadFlying = player.getAbilities().flying;
		float hadFlySpeed = player.getAbilities().getFlySpeed();
		int overheadTextEndTick = currentTick + Math.max(0, PLUS_ULTRA_OVERHEAD_TEXT_DURATION_TICKS);
		UUID overheadTextEntityId = PLUS_ULTRA_OVERHEAD_TEXT_ENABLED && PLUS_ULTRA_OVERHEAD_TEXT_DURATION_TICKS > 0 ? spawnPlusUltraOverheadText(player) : null;
		PLUS_ULTRA_STATES.put(
			playerId,
			new PlusUltraState(
				player.getEntityWorld().getRegistryKey(),
				currentTick,
				currentTick + Math.max(1, PLUS_ULTRA_DURATION_TICKS / 2),
				currentTick + PLUS_ULTRA_DURATION_TICKS,
				currentTick,
				currentTick,
				hadAllowFlying,
				hadFlying,
				hadFlySpeed,
				overheadTextEndTick,
				overheadTextEntityId
			)
		);
		MagicPlayerData.setPlusUltraActive(player, true);
		PLUS_ULTRA_LAST_OUTLINED_PLAYERS.remove(playerId);
		setActiveAbility(player, MagicAbility.PLUS_ULTRA);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		ensurePlusUltraFlightState(player);
		syncPlusUltraOutlines(player);
		player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.PLUS_ULTRA.displayName()), true);
	}

	static void applyPlusUltra(ServerPlayerEntity player, int currentTick) {
		PlusUltraState state = PLUS_ULTRA_STATES.get(player.getUuid());
		if (state == null) {
			clearPlusUltraRuntimeState(player, true);
			return;
		}

		if (!shouldKeepPlusUltraEnabled(player) || player.getEntityWorld().getRegistryKey() != state.dimension()) {
			endPlusUltra(player, currentTick, PlusUltraEndMode.FULL, false);
			return;
		}

		if (currentTick >= state.endTick()) {
			endPlusUltra(player, currentTick, PlusUltraEndMode.FULL, true);
			return;
		}

		ensurePlusUltraFlightState(player);
		if (currentTick >= state.nextOutlineTick()) {
			syncPlusUltraOutlines(player);
			state.nextOutlineTick(currentTick + PLUS_ULTRA_OUTLINE_REFRESH_TICKS);
		}
		if (state.overheadTextEntityId() != null && currentTick >= state.overheadTextEndTick()) {
			removePlusUltraOverheadText(player.getEntityWorld().getServer(), state);
			state.overheadTextEntityId(null);
		}
		if (currentTick < state.overheadTextEndTick() && currentTick >= state.nextTextTick()) {
			syncPlusUltraOverheadText(player, state, currentTick);
			state.nextTextTick(currentTick + PLUS_ULTRA_OVERHEAD_TEXT_REFRESH_TICKS);
		}
	}

	static void endPlusUltra(ServerPlayerEntity player, int currentTick, PlusUltraEndMode endMode, boolean sendFeedback) {
		clearPlusUltraRuntimeState(player, true);
		if (endMode != PlusUltraEndMode.ADMIN_CLEAR) {
			if (endMode == PlusUltraEndMode.MANUAL_EARLY) {
				applyPlusUltraPenalty(
					player,
					PLUS_ULTRA_EARLY_CANCEL_PENALTY_DURATION_TICKS,
					PLUS_ULTRA_EARLY_CANCEL_SLOWNESS_AMPLIFIER,
					PLUS_ULTRA_EARLY_CANCEL_WEAKNESS_AMPLIFIER
				);
				startPlusUltraCooldown(player.getUuid(), currentTick, PLUS_ULTRA_EARLY_CANCEL_COOLDOWN_TICKS);
			} else {
				applyPlusUltraPenalty(
					player,
					PLUS_ULTRA_FULL_END_PENALTY_DURATION_TICKS,
					PLUS_ULTRA_FULL_END_SLOWNESS_AMPLIFIER,
					PLUS_ULTRA_FULL_END_WEAKNESS_AMPLIFIER
				);
				startPlusUltraCooldown(player.getUuid(), currentTick, PLUS_ULTRA_FULL_END_COOLDOWN_TICKS);
			}
		}

		if (sendFeedback) {
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.PLUS_ULTRA.displayName()), true);
		}
	}

	static void applyPlusUltraPenalty(ServerPlayerEntity player, int durationTicks, int slownessAmplifier, int weaknessAmplifier) {
		if (!player.isAlive()) {
			return;
		}

		addConfiguredStatusEffect(player, StatusEffects.SLOWNESS, durationTicks, slownessAmplifier);
		addConfiguredStatusEffect(player, StatusEffects.WEAKNESS, durationTicks, weaknessAmplifier);
	}

	static void ensurePlusUltraFlightState(ServerPlayerEntity player) {
		if (!PLUS_ULTRA_FLIGHT_ENABLED || player.isSpectator()) {
			updatePlusUltraPose(player, false);
			return;
		}

		float targetFlySpeed = plusUltraCurrentFlySpeed(player);
		boolean changed = false;
		if (!player.getAbilities().allowFlying) {
			player.getAbilities().allowFlying = true;
			changed = true;
		}
		if (Math.abs(player.getAbilities().getFlySpeed() - targetFlySpeed) > 1.0E-4F) {
			player.getAbilities().setFlySpeed(targetFlySpeed);
			changed = true;
		}
		if (changed) {
			player.sendAbilitiesUpdate();
		}
		applyPlusUltraFlightMovement(player);
		updatePlusUltraPose(player, player.getAbilities().flying);
	}

	static void applyPlusUltraFlightMovement(ServerPlayerEntity player) {
		if (!player.getAbilities().flying) {
			return;
		}

		Vec3d look = player.getRotationVector();
		Vec3d normalizedLook = look.lengthSquared() > 1.0E-5 ? look.normalize() : Vec3d.ZERO;
		Vec3d horizontalLook = new Vec3d(look.x, 0.0, look.z);
		if (horizontalLook.lengthSquared() <= 1.0E-5) {
			float yawRadians = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
			horizontalLook = new Vec3d(-MathHelper.sin(yawRadians), 0.0, MathHelper.cos(yawRadians));
		}
		Vec3d normalizedHorizontalLook = horizontalLook.normalize();
		Vec3d sideways = new Vec3d(-normalizedHorizontalLook.z, 0.0, normalizedHorizontalLook.x);
		double forwardInput = MathHelper.clamp(player.forwardSpeed, -1.0F, 1.0F);
		double sidewaysInput = MathHelper.clamp(player.sidewaysSpeed, -1.0F, 1.0F);
		double sprintMultiplier = plusUltraSprintFlightMultiplier(player);
		Vec3d acceleration = Vec3d.ZERO;
		if (Math.abs(forwardInput) > 0.01) {
			acceleration = acceleration.add(normalizedLook.multiply(forwardInput * PLUS_ULTRA_FLIGHT_ACCELERATION * sprintMultiplier));
		}
		if (Math.abs(sidewaysInput) > 0.01) {
			acceleration = acceleration.add(sideways.multiply(sidewaysInput * PLUS_ULTRA_FLIGHT_ACCELERATION * 0.75 * sprintMultiplier));
		}
		if (player.isJumping()) {
			acceleration = acceleration.add(0.0, PLUS_ULTRA_FLIGHT_VERTICAL_ACCELERATION, 0.0);
		}
		if (player.isSneaking()) {
			acceleration = acceleration.add(0.0, -PLUS_ULTRA_FLIGHT_VERTICAL_ACCELERATION, 0.0);
		}

		Vec3d nextVelocity = player.getVelocity().multiply(PLUS_ULTRA_FLIGHT_DRAG).add(acceleration);
		player.setVelocity(clampPlusUltraFlightVelocity(nextVelocity, sprintMultiplier));
	}

	static float plusUltraCurrentFlySpeed(ServerPlayerEntity player) {
		return (float) (PLUS_ULTRA_FLIGHT_FLY_SPEED * plusUltraSprintFlightMultiplier(player));
	}

	static double plusUltraSprintFlightMultiplier(ServerPlayerEntity player) {
		return player.getAbilities().flying && !player.isOnGround() && player.isSprinting()
			? PLUS_ULTRA_FLIGHT_SPRINT_SPEED_MULTIPLIER
			: 1.0;
	}

	static Vec3d clampPlusUltraFlightVelocity(Vec3d velocity, double sprintMultiplier) {
		double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
		double clampedX = velocity.x;
		double clampedZ = velocity.z;
		double horizontalMaxSpeed = PLUS_ULTRA_FLIGHT_MAX_SPEED * sprintMultiplier;
		if (horizontalMaxSpeed > 0.0 && horizontalSpeed > horizontalMaxSpeed) {
			double scale = horizontalMaxSpeed / horizontalSpeed;
			clampedX *= scale;
			clampedZ *= scale;
		}
		double clampedY = MathHelper.clamp(velocity.y, -PLUS_ULTRA_FLIGHT_VERTICAL_MAX_SPEED, PLUS_ULTRA_FLIGHT_VERTICAL_MAX_SPEED);
		return new Vec3d(clampedX, clampedY, clampedZ);
	}

	static void updatePlusUltraPose(ServerPlayerEntity player, boolean shouldUseFlyingPose) {
		EntityPose desiredPose = shouldUseFlyingPose && PLUS_ULTRA_ELYTRA_POSE_WHILE_FLYING && !player.isSpectator()
			? EntityPose.GLIDING
			: defaultPlusUltraPose(player);
		if (player.getPose() != desiredPose) {
			player.setPose(desiredPose);
		}
	}

	static EntityPose defaultPlusUltraPose(ServerPlayerEntity player) {
		if (player.isSleeping()) {
			return EntityPose.SLEEPING;
		}
		if (player.isSwimming()) {
			return EntityPose.SWIMMING;
		}
		if (player.isGliding()) {
			return EntityPose.GLIDING;
		}
		if (player.isUsingRiptide()) {
			return EntityPose.SPIN_ATTACK;
		}
		return player.isSneaking() && !player.getAbilities().flying ? EntityPose.CROUCHING : EntityPose.STANDING;
	}

	static PlusUltraState clearPlusUltraRuntimeState(ServerPlayerEntity player, boolean clearOutlinePayload) {
		UUID playerId = player.getUuid();
		PlusUltraState state = PLUS_ULTRA_STATES.remove(playerId);
		MagicPlayerData.setPlusUltraActive(player, false);
		PLUS_ULTRA_LAST_OUTLINED_PLAYERS.remove(playerId);
		clearPlusUltraImpactStates(playerId);
		if (clearOutlinePayload) {
			sendCassiopeiaOutlinePayload(player, List.of());
		}

		if (state != null) {
			restorePlusUltraFlight(player, state.hadAllowFlying(), state.hadFlying(), state.hadFlySpeed());
			removePlusUltraOverheadText(player.getEntityWorld().getServer(), state);
		} else {
			restorePlusUltraFlightFallback(player);
		}

		if (activeAbility(player) == MagicAbility.PLUS_ULTRA || MagicPlayerData.getActiveAbilitySlot(player) == MagicAbility.PLUS_ULTRA.slot()) {
			setActiveAbility(player, MagicAbility.NONE);
		}
		return state;
	}

	static void clearPlusUltraImpactStates(UUID playerId) {
		PLUS_ULTRA_IMPACT_STATES.entrySet().removeIf(entry -> entry.getKey().equals(playerId) || playerId.equals(entry.getValue().casterId()));
	}

	static void restorePlusUltraFlight(ServerPlayerEntity player, boolean allowFlying, boolean flying, float flySpeed) {
		boolean nextAllowFlying = allowFlying || player.isCreative() || player.isSpectator();
		boolean nextFlying = nextAllowFlying && (flying || player.isCreative() || player.isSpectator());
		boolean changed = player.getAbilities().allowFlying != nextAllowFlying
			|| player.getAbilities().flying != nextFlying
			|| Math.abs(player.getAbilities().getFlySpeed() - flySpeed) > 1.0E-4F;
		player.getAbilities().allowFlying = nextAllowFlying;
		player.getAbilities().flying = nextFlying;
		player.getAbilities().setFlySpeed(flySpeed);
		if (changed) {
			player.sendAbilitiesUpdate();
		}
		updatePlusUltraPose(player, false);
	}

	static void restorePlusUltraFlightFallback(ServerPlayerEntity player) {
		if (player.isCreative() || player.isSpectator()) {
			return;
		}

		boolean changed = player.getAbilities().allowFlying || player.getAbilities().flying;
		player.getAbilities().allowFlying = false;
		player.getAbilities().flying = false;
		if (Math.abs(player.getAbilities().getFlySpeed() - DEFAULT_PLAYER_FLY_SPEED) > 1.0E-4F) {
			player.getAbilities().setFlySpeed(DEFAULT_PLAYER_FLY_SPEED);
			changed = true;
		}
		if (changed) {
			player.sendAbilitiesUpdate();
		}
		updatePlusUltraPose(player, false);
	}

	static boolean shouldKeepPlusUltraEnabled(ServerPlayerEntity player) {
		return player.isAlive()
			&& MagicPlayerData.hasMagic(player)
			&& MagicPlayerData.getSchool(player) == MagicSchool.JESTER
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.PLUS_ULTRA);
	}

	static boolean hasActivePlusUltra(ServerPlayerEntity player) {
		return player != null && activeAbility(player) == MagicAbility.PLUS_ULTRA && PLUS_ULTRA_STATES.containsKey(player.getUuid());
	}

	public static boolean shouldRenderPlusUltraFlightPose(PlayerEntity player) {
		return player != null
			&& PLUS_ULTRA_ELYTRA_POSE_WHILE_FLYING
			&& MagicPlayerData.isPlusUltraActive(player)
			&& player.getAbilities().flying
			&& !player.isOnGround()
			&& !player.isSpectator();
	}

	static void syncPlusUltraOutlines(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		Set<UUID> outlinedPlayers = new HashSet<>();
		if (PLUS_ULTRA_OUTLINE_RADIUS > 0.0) {
			Box area = player.getBoundingBox().expand(PLUS_ULTRA_OUTLINE_RADIUS);
			for (ServerPlayerEntity target : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
				if (target == player || !target.isAlive() || target.isSpectator() || target.getEntityWorld() != player.getEntityWorld()) {
					continue;
				}

				if (target.getBoundingBox().intersects(area)) {
					outlinedPlayers.add(target.getUuid());
				}
			}
		}

		Set<UUID> lastSent = PLUS_ULTRA_LAST_OUTLINED_PLAYERS.get(playerId);
		if (lastSent != null && lastSent.equals(outlinedPlayers)) {
			return;
		}

		PLUS_ULTRA_LAST_OUTLINED_PLAYERS.put(playerId, Set.copyOf(outlinedPlayers));
		sendCassiopeiaOutlinePayload(player, outlinedPlayers);
	}

	static void syncPlusUltraOverheadText(ServerPlayerEntity player, PlusUltraState state, int currentTick) {
		if (!PLUS_ULTRA_OVERHEAD_TEXT_ENABLED || currentTick >= state.overheadTextEndTick()) {
			if (state.overheadTextEntityId() != null) {
				removePlusUltraOverheadText(player.getEntityWorld().getServer(), state);
				state.overheadTextEntityId(null);
			}
			return;
		}

		ArmorStandEntity textEntity = null;
		if (state.overheadTextEntityId() != null) {
			Entity existing = player.getEntityWorld().getServer().getWorld(state.dimension()) == null
				? null
				: player.getEntityWorld().getServer().getWorld(state.dimension()).getEntity(state.overheadTextEntityId());
			if (existing instanceof ArmorStandEntity armorStand && armorStand.isAlive()) {
				textEntity = armorStand;
			}
		}

		if (textEntity == null) {
			state.overheadTextEntityId(spawnPlusUltraOverheadText(player));
			if (state.overheadTextEntityId() == null) {
				return;
			}
			Entity existing = player.getEntityWorld().getServer().getWorld(player.getEntityWorld().getRegistryKey()).getEntity(state.overheadTextEntityId());
			if (existing instanceof ArmorStandEntity armorStand && armorStand.isAlive()) {
				textEntity = armorStand;
			}
		}

		if (textEntity == null) {
			return;
		}

		textEntity.setCustomName(plusUltraOverheadText());
		textEntity.setCustomNameVisible(true);
		textEntity.refreshPositionAndAngles(
			player.getX(),
			player.getY() + PLUS_ULTRA_OVERHEAD_TEXT_VERTICAL_OFFSET,
			player.getZ(),
			0.0F,
			0.0F
		);
	}

	static UUID spawnPlusUltraOverheadText(ServerPlayerEntity player) {
		if (!PLUS_ULTRA_OVERHEAD_TEXT_ENABLED || PLUS_ULTRA_OVERHEAD_TEXT.isBlank() || PLUS_ULTRA_OVERHEAD_TEXT_DURATION_TICKS <= 0) {
			return null;
		}

		ServerWorld world = (ServerWorld) player.getEntityWorld();
		ArmorStandEntity textEntity = new ArmorStandEntity(world, player.getX(), player.getY() + PLUS_ULTRA_OVERHEAD_TEXT_VERTICAL_OFFSET, player.getZ());
		textEntity.setInvisible(true);
		textEntity.setInvulnerable(true);
		textEntity.setNoGravity(true);
		((ArmorStandEntityAccessorMixin) textEntity).magic$setMarker(true);
		textEntity.setSilent(true);
		textEntity.setCustomName(plusUltraOverheadText());
		textEntity.setCustomNameVisible(true);
		if (!world.spawnEntity(textEntity)) {
			return null;
		}
		return textEntity.getUuid();
	}

	static void removePlusUltraOverheadText(MinecraftServer server, PlusUltraState state) {
		if (server == null || state == null || state.overheadTextEntityId() == null) {
			return;
		}

		ServerWorld world = server.getWorld(state.dimension());
		if (world == null) {
			return;
		}

		Entity entity = world.getEntity(state.overheadTextEntityId());
		if (entity != null) {
			entity.discard();
		}
	}

	static Text plusUltraOverheadText() {
		return Text.literal(PLUS_ULTRA_OVERHEAD_TEXT).formatted(Formatting.RED, Formatting.BOLD);
	}

	static void handleHerculesBurdenRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		if (activeAbility(player) == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			deactivateHerculesBurden(player, true, true);
			return;
		}

		int remainingTicks = herculesBurdenCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.HERCULES_BURDEN_OF_THE_SKY, remainingTicks, false);
			return;
		}

		int manaCost = (int) Math.ceil(manaFromPercentExact(HERCULES_ACTIVATION_COST_PERCENT));
		if (!canSpendAbilityCost(player, manaCost)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		LivingEntity primaryTarget = findLivingTargetInLineOfSight(player, HERCULES_TARGET_RANGE);
		if (primaryTarget == null) {
			player.sendMessage(Text.translatable("message.magic.ability.no_target", MagicAbility.HERCULES_BURDEN_OF_THE_SKY.displayName()), true);
			return;
		}

		List<LivingEntity> targets = findLivingTargetsAround(primaryTarget, HERCULES_SPLASH_RADIUS);
		targets.removeIf(target -> target == player);
		if (targets.isEmpty()) {
			player.sendMessage(Text.translatable("message.magic.ability.no_target", MagicAbility.HERCULES_BURDEN_OF_THE_SKY.displayName()), true);
			return;
		}

		spendAbilityCost(player, manaCost);
		activateHerculesBurden(player, targets, currentTick);
		recordOrionsGambitAbilityUse(player, MagicAbility.HERCULES_BURDEN_OF_THE_SKY);
	}

	static void handleSagittariusAstralArrowRequest(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		int currentTick = player.getEntityWorld().getServer().getTicks();
		CelestialAlignmentSessionState session = SAGITTARIUS_STATES.get(playerId);
		CelestialGamaRayState gamaRayState = CELESTIAL_GAMA_RAY_STATES.get(playerId);
		if (gamaRayState != null) {
			if (gamaRayState.phase == CelestialGamaRayPhase.TRACING) {
				cancelCelestialGamaRayTrace(player, true);
				return;
			}
			player.sendMessage(Text.translatable("message.magic.constellation.still_charging", celestialGamaRayDisplayName()), true);
			return;
		}

		if (player.isSneaking()) {
			if (session != null && !session.constellations.isEmpty()) {
				if (!CELESTIAL_ALIGNMENT_CONFIG.shiftCancelEnabled) {
					return;
				}
				endCelestialAlignmentSession(player, CelestialAlignmentSessionEndReason.SHIFT_CANCEL, currentTick, true, true);
				return;
			}

			int remainingTicks = sagittariusAstralArrowCooldownRemaining(player, currentTick);
			if (remainingTicks > 0) {
				sendAbilityCooldownMessage(player, MagicAbility.SAGITTARIUS_ASTRAL_ARROW, remainingTicks, false);
				return;
			}

			if (!hasRequiredManaPercent(player, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.minimumStartManaPercent)) {
				player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
				return;
			}

			startCelestialGamaRayTracing(player, currentTick);
			recordOrionsGambitAbilityUse(player, MagicAbility.SAGITTARIUS_ASTRAL_ARROW);
			return;
		}

		int remainingTicks = sagittariusAstralArrowCooldownRemaining(player, currentTick);
		if ((session == null || session.constellations.isEmpty()) && remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.SAGITTARIUS_ASTRAL_ARROW, remainingTicks, false);
			return;
		}

		if (session != null && session.constellations.size() >= CELESTIAL_ALIGNMENT_CONFIG.maxActiveConstellations) {
			endCelestialAlignmentSession(player, CelestialAlignmentSessionEndReason.FOURTH_PRESS_CANCEL, currentTick, true, true);
			return;
		}

		int manaCost = (int) Math.ceil(manaFromPercentExact(CELESTIAL_ALIGNMENT_CONFIG.activationCostPercent));
		if (!canSpendAbilityCost(player, manaCost)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		BlockHitResult placement = findTargetedBlock(player, CELESTIAL_ALIGNMENT_CONFIG.targetPlacementRange);
		if (placement == null || placement.getType() != HitResult.Type.BLOCK) {
			player.sendMessage(Text.translatable("message.magic.constellation.celestial_alignment.no_ground"), true);
			return;
		}

		CelestialAlignmentConstellation selectedConstellation = rollCelestialAlignment(player);
		Vec3d blockCenter = placement.getBlockPos().toCenterPos();
		Vec3d center = new Vec3d(blockCenter.x, placement.getBlockPos().getY() + 1.05, blockCenter.z);
		spendAbilityCost(player, manaCost);
		setActiveAbility(player, MagicAbility.SAGITTARIUS_ASTRAL_ARROW);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		CelestialAlignmentSessionState activeSession = session;
		if (activeSession == null || activeSession.constellations.isEmpty()) {
			activeSession = new CelestialAlignmentSessionState(player.getUuid(), player.getEntityWorld().getRegistryKey(), currentTick);
			SAGITTARIUS_STATES.put(playerId, activeSession);
			player.sendMessage(Text.translatable("message.magic.ability.activated", MagicAbility.SAGITTARIUS_ASTRAL_ARROW.displayName()), true);
		}
		activeSession.constellations.add(
			new CelestialAlignmentState(
				player.getUuid(),
				player.getEntityWorld().getRegistryKey(),
				center,
				celestialAlignmentRadius(selectedConstellation),
				center.y,
				center.y + CELESTIAL_ALIGNMENT_CONFIG.columnHeight,
				selectedConstellation,
				currentTick
			)
		);
		sendCelestialAlignmentBanner(player, selectedConstellation);
		recordOrionsGambitAbilityUse(player, MagicAbility.SAGITTARIUS_ASTRAL_ARROW);
	}

	static void handleOrionsGambitRequest(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		int currentTick = player.getEntityWorld().getServer().getTicks();
		OrionGambitState existingState = ORIONS_GAMBIT_STATES.get(playerId);
		if (existingState != null) {
			if (existingState.stage == OrionGambitStage.WAITING) {
				endOrionsGambit(player, OrionGambitEndReason.WAITING_CANCEL, currentTick, true);
			} else {
				endOrionsGambit(player, OrionGambitEndReason.MANUAL_CANCEL, currentTick, true);
			}
			return;
		}

		int remainingTicks = orionsGambitCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.ORIONS_GAMBIT, remainingTicks, false);
			return;
		}

		setActiveAbility(player, MagicAbility.ORIONS_GAMBIT);
		MagicPlayerData.setDepletedRecoveryMode(player, false);
		ORIONS_GAMBIT_STATES.put(playerId, OrionGambitState.waiting());
		player.sendMessage(Text.translatable("message.magic.constellation.orion_waiting"), true);
		recordOrionsGambitAbilityUse(player, MagicAbility.ORIONS_GAMBIT);
	}

	static void handleAstralCataclysmRequest(ServerPlayerEntity player, int currentTick) {
		ConstellationDomainState state = ASTRAL_CATACLYSM_DOMAIN_STATES.get(player.getUuid());
		if (state == null) {
			deactivateDomainExpansion(player);
			setActiveAbility(player, MagicAbility.NONE);
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.ASTRAL_CATACLYSM.displayName()), true);
			return;
		}

		if (state.phase == ConstellationDomainPhase.CHARGING && currentTick < state.chargeCompleteTick) {
			player.sendMessage(Text.translatable("message.magic.constellation.still_charging", MagicAbility.ASTRAL_CATACLYSM.displayName()), true);
			return;
		}

		if (state.phase == ConstellationDomainPhase.CHARGING || state.phase == ConstellationDomainPhase.READY) {
			state.phase = ConstellationDomainPhase.ACQUIRING;
			state.acquireEndTick = currentTick + ASTRAL_CATACLYSM_ACQUIRE_WINDOW_TICKS;
			player.sendMessage(Text.translatable("message.magic.constellation.astral_cataclysm.hunting"), true);
			return;
		}

		deactivateDomainExpansion(player);
		setActiveAbility(player, MagicAbility.NONE);
		player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.ASTRAL_CATACLYSM.displayName()), true);
	}

	static void handleWittyOneLinerRequest(ServerPlayerEntity player) {
		int currentTick = player.getEntityWorld().getServer().getTicks();
		int remainingTicks = wittyOneLinerCooldownRemaining(player, currentTick);
		if (remainingTicks > 0) {
			sendAbilityCooldownMessage(player, MagicAbility.WITTY_ONE_LINER, remainingTicks, false);
			return;
		}

		int manaCost = (int) Math.ceil(manaFromPercentExact(WITTY_ONE_LINER_ACTIVATION_COST_PERCENT));
		if (!canSpendAbilityCost(player, manaCost)) {
			player.sendMessage(Text.translatable("message.magic.ability.no_mana"), true);
			return;
		}

		ServerPlayerEntity target = findPlayerTargetInLineOfSight(player, WITTY_ONE_LINER_RANGE);
		if (target == null) {
			player.sendMessage(Text.translatable("message.magic.ability.no_target", MagicAbility.WITTY_ONE_LINER.displayName()), true);
			return;
		}

		WittyOneLinerTierSettings selectedTier = selectWittyOneLinerTier(player);
		String joke = selectRandomJoke(player, selectedTier.jokes());
		if (joke == null || joke.isBlank()) {
			joke = MagicAbility.WITTY_ONE_LINER.displayName().getString();
		}

		spendAbilityCost(player, manaCost);
		if (!isCooldownDeferredByOrionsGambit(player.getUuid(), MagicAbility.WITTY_ONE_LINER) && selectedTier.cooldownTicks() > 0) {
			WITTY_ONE_LINER_COOLDOWN_END_TICK.put(
				player.getUuid(),
				currentTick + adjustedCooldownTicks(player.getUuid(), MagicAbility.WITTY_ONE_LINER, selectedTier.cooldownTicks(), currentTick)
			);
		}
		applyWittyOneLinerEffects(target, selectedTier);
		sendWittyOneLinerOverlay(player, target, joke, selectedTier.colorRgb());
		recordOrionsGambitAbilityUse(player, MagicAbility.WITTY_ONE_LINER, selectedTier.cooldownTicks());
	}
}

