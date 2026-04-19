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


abstract class AbilityAdminAndPassiveSupport extends AbilityMathAndConfigSupport {
	static boolean shouldKeepSpotlightEnabled(ServerPlayerEntity player) {
		return MagicPlayerData.hasMagic(player)
			&& MagicPlayerData.getSchool(player) == MagicSchool.JESTER
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.SPOTLIGHT);
	}

	static boolean shouldKeepComedicRewriteEnabled(ServerPlayerEntity player) {
		return MagicPlayerData.hasMagic(player)
			&& MagicPlayerData.getSchool(player) == MagicSchool.JESTER
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.COMEDIC_REWRITE);
	}

	static boolean shouldKeepComedicAssistantEnabled(ServerPlayerEntity player) {
		return MagicPlayerData.hasMagic(player)
			&& MagicPlayerData.getSchool(player) == MagicSchool.JESTER
			&& MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.COMEDIC_ASSISTANT);
	}

	static void updateComedicAssistantState(ServerPlayerEntity player, int currentTick) {
		ComedicAssistantArmedState state = COMEDIC_ASSISTANT_ARMED_STATES.get(player.getUuid());
		if (state == null) {
			if (COMEDIC_ASSISTANT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), Integer.MIN_VALUE) <= currentTick) {
				COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(player.getUuid());
			}
			return;
		}

		if (!player.isAlive() || !shouldKeepComedicAssistantEnabled(player) || player.getEntityWorld().getRegistryKey() != state.dimension()) {
			clearComedicAssistantState(player.getUuid());
			return;
		}

		if (currentTick >= state.expiresTick()) {
			deactivateComedicAssistant(player, currentTick, COMEDIC_ASSISTANT_CANCEL_COOLDOWN_TICKS, false, true);
			return;
		}

		if (currentTick >= state.nextIndicatorTick()) {
			sendComedicAssistantArmedIndicator(player);
			spawnComedicAssistantArmedParticles(player);
			COMEDIC_ASSISTANT_ARMED_STATES.put(
				player.getUuid(),
				new ComedicAssistantArmedState(state.dimension(), state.expiresTick(), currentTick + COMEDIC_ASSISTANT_ARMED_INDICATOR_REFRESH_TICKS)
			);
		}

		if (COMEDIC_ASSISTANT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), Integer.MIN_VALUE) <= currentTick) {
			COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(player.getUuid());
		}
	}

	static void sendComedicAssistantArmedIndicator(ServerPlayerEntity player) {
		player.sendMessage(Text.translatable("message.magic.jester.comedic_assistant.ready"), true);
	}

	static void spawnComedicAssistantArmedParticles(ServerPlayerEntity player) {
		if (!(player.getEntityWorld() instanceof ServerWorld world) || COMEDIC_ASSISTANT_ARMED_PARTICLE_COUNT <= 0) {
			return;
		}

		world.spawnParticles(
			ParticleTypes.GLOW,
			player.getX(),
			player.getBodyY(0.75),
			player.getZ(),
			COMEDIC_ASSISTANT_ARMED_PARTICLE_COUNT,
			0.3,
			0.25,
			0.3,
			0.02
		);
	}

	static void applySpotlight(ServerPlayerEntity player, int currentTick) {
		UUID playerId = player.getUuid();
		SpotlightState state = SPOTLIGHT_STATES.computeIfAbsent(playerId, ignored -> new SpotlightState());
		if (!player.isAlive()) {
			resetSpotlightTracking(player);
			return;
		}

		if (SPOTLIGHT_MAX_HISTORY_TICKS > 0) {
			state.viewerLastLookTicks().entrySet().removeIf(entry -> currentTick - entry.getValue() > SPOTLIGHT_MAX_HISTORY_TICKS);
		}

		int currentViewers = 0;
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server != null) {
			for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
				if (!isSpotlightViewerLookingAtTarget(viewer, player)) {
					continue;
				}

				state.viewerLastLookTicks().put(viewer.getUuid(), currentTick);
				currentViewers++;
			}
		}

		int activationStage = highestSpotlightActivationStage(state, currentTick);
		if (activationStage > state.currentStage()) {
			state.currentStage(activationStage);
			state.downgradeDeadlineTick(-1);
		}

		if (state.currentStage() > 0) {
			SpotlightStageSettings currentSettings = spotlightStageSettingsFor(state.currentStage());
			if (currentViewers >= currentSettings.viewersRequired()) {
				state.downgradeDeadlineTick(-1);
			} else if (state.downgradeDeadlineTick() < 0) {
				state.downgradeDeadlineTick(currentTick + currentSettings.downgradeGraceTicks());
			} else if (currentTick >= state.downgradeDeadlineTick()) {
				state.currentStage(resolveSpotlightFallbackStage(state, currentTick, state.currentStage() - 1));
				state.downgradeDeadlineTick(-1);
			}
		}

		applySpotlightBuffs(player, state);
	}

	static boolean isSpotlightViewerLookingAtTarget(ServerPlayerEntity viewer, ServerPlayerEntity target) {
		return viewer != target
			&& viewer.isAlive()
			&& !viewer.isSpectator()
			&& viewer.getEntityWorld() == target.getEntityWorld()
			&& findLivingTargetInLineOfSight(viewer, SPOTLIGHT_DETECTION_RANGE) == target;
	}

	static int highestSpotlightActivationStage(SpotlightState state, int currentTick) {
		if (countRecentSpotlightViewers(state, currentTick, SPOTLIGHT_STAGE_THREE.activationWindowTicks()) >= SPOTLIGHT_STAGE_THREE.viewersRequired()) {
			return 3;
		}
		if (countRecentSpotlightViewers(state, currentTick, SPOTLIGHT_STAGE_TWO.activationWindowTicks()) >= SPOTLIGHT_STAGE_TWO.viewersRequired()) {
			return 2;
		}
		if (countRecentSpotlightViewers(state, currentTick, SPOTLIGHT_STAGE_ONE.activationWindowTicks()) >= SPOTLIGHT_STAGE_ONE.viewersRequired()) {
			return 1;
		}
		return 0;
	}

	static int resolveSpotlightFallbackStage(SpotlightState state, int currentTick, int maxStage) {
		for (int stage = Math.max(0, maxStage); stage >= 1; stage--) {
			SpotlightStageSettings settings = spotlightStageSettingsFor(stage);
			if (countRecentSpotlightViewers(state, currentTick, settings.fallbackWindowTicks()) >= settings.viewersRequired()) {
				return stage;
			}
		}
		return 0;
	}

	static int countRecentSpotlightViewers(SpotlightState state, int currentTick, int windowTicks) {
		int earliestTick = currentTick - Math.max(0, windowTicks) + 1;
		int viewers = 0;
		for (int lastLookTick : state.viewerLastLookTicks().values()) {
			if (lastLookTick >= earliestTick) {
				viewers++;
			}
		}
		return viewers;
	}

	static void applySpotlightBuffs(ServerPlayerEntity player, SpotlightState state) {
		SpotlightStageSettings stageSettings = spotlightStageSettingsFor(state.currentStage());
		if (stageSettings == null) {
			removeSpotlightBuffs(player, state);
			return;
		}

		overwriteAttributeModifier(
			player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE),
			SPOTLIGHT_ATTACK_DAMAGE_MODIFIER_ID,
			stageSettings.attackDamageBonus(),
			EntityAttributeModifier.Operation.ADD_VALUE
		);
		overwriteAttributeModifier(
			player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED),
			SPOTLIGHT_MOVEMENT_SPEED_MODIFIER_ID,
			stageSettings.movementSpeedMultiplier(),
			EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);
		overwriteAttributeModifier(
			player.getAttributeInstance(EntityAttributes.MAX_HEALTH),
			SPOTLIGHT_MAX_HEALTH_MODIFIER_ID,
			stageSettings.maxHealthBonusPoints(),
			EntityAttributeModifier.Operation.ADD_VALUE
		);
		overwriteAttributeModifier(
			player.getAttributeInstance(EntityAttributes.SCALE),
			SPOTLIGHT_SCALE_MODIFIER_ID,
			stageSettings.scale() - 1.0,
			EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);

		if (stageSettings.jumpBoostAmplifier() >= 0) {
			refreshStatusEffect(player, StatusEffects.JUMP_BOOST, SPOTLIGHT_EFFECT_REFRESH_TICKS, stageSettings.jumpBoostAmplifier(), true, false, true);
		}
		if (stageSettings.resistanceAmplifier() >= 0) {
			refreshStatusEffect(player, StatusEffects.RESISTANCE, SPOTLIGHT_EFFECT_REFRESH_TICKS, stageSettings.resistanceAmplifier(), true, false, true);
		}

		double newBonusPoints = stageSettings.maxHealthBonusPoints();
		double previousBonusPoints = state.appliedMaxHealthBonusPoints();
		if (newBonusPoints > previousBonusPoints) {
			player.setHealth((float) Math.min(player.getMaxHealth(), player.getHealth() + (newBonusPoints - previousBonusPoints)));
		} else if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		state.appliedMaxHealthBonusPoints(newBonusPoints);
	}

	static void overwriteAttributeModifier(
		EntityAttributeInstance attributeInstance,
		Identifier modifierId,
		double value,
		EntityAttributeModifier.Operation operation
	) {
		if (attributeInstance == null) {
			return;
		}

		EntityAttributeModifier existing = attributeInstance.getModifier(modifierId);
		if (value <= 0.0) {
			if (existing != null) {
				attributeInstance.removeModifier(modifierId);
			}
			return;
		}

		if (existing != null && existing.operation() == operation && Math.abs(existing.value() - value) <= 1.0E-6) {
			return;
		}

		if (existing != null) {
			attributeInstance.removeModifier(modifierId);
		}

		attributeInstance.addTemporaryModifier(new EntityAttributeModifier(modifierId, value, operation));
	}

	static void setSignedAttributeModifier(
		EntityAttributeInstance attributeInstance,
		Identifier modifierId,
		double value,
		EntityAttributeModifier.Operation operation
	) {
		if (attributeInstance == null) {
			return;
		}

		EntityAttributeModifier existing = attributeInstance.getModifier(modifierId);
		if (Math.abs(value) <= 1.0E-6) {
			if (existing != null) {
				attributeInstance.removeModifier(modifierId);
			}
			return;
		}

		if (existing != null && existing.operation() == operation && Math.abs(existing.value() - value) <= 1.0E-6) {
			return;
		}

		if (existing != null) {
			attributeInstance.removeModifier(modifierId);
		}

		attributeInstance.addTemporaryModifier(new EntityAttributeModifier(modifierId, value, operation));
	}

	static void removeSpotlightBuffs(ServerPlayerEntity player, SpotlightState state) {
		removeAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), SPOTLIGHT_ATTACK_DAMAGE_MODIFIER_ID);
		removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), SPOTLIGHT_MOVEMENT_SPEED_MODIFIER_ID);
		removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), SPOTLIGHT_MAX_HEALTH_MODIFIER_ID);
		removeAttributeModifier(player.getAttributeInstance(EntityAttributes.SCALE), SPOTLIGHT_SCALE_MODIFIER_ID);
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		state.appliedMaxHealthBonusPoints(0.0);
	}

	static void removeAttributeModifier(EntityAttributeInstance attributeInstance, Identifier modifierId) {
		if (attributeInstance != null) {
			attributeInstance.removeModifier(modifierId);
		}
	}

	static void resetSpotlightTracking(ServerPlayerEntity player) {
		SpotlightState state = SPOTLIGHT_STATES.get(player.getUuid());
		if (state == null) {
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), SPOTLIGHT_ATTACK_DAMAGE_MODIFIER_ID);
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), SPOTLIGHT_MOVEMENT_SPEED_MODIFIER_ID);
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), SPOTLIGHT_MAX_HEALTH_MODIFIER_ID);
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.SCALE), SPOTLIGHT_SCALE_MODIFIER_ID);
			if (player.getHealth() > player.getMaxHealth()) {
				player.setHealth(player.getMaxHealth());
			}
			return;
		}

		state.viewerLastLookTicks().clear();
		state.currentStage(0);
		state.downgradeDeadlineTick(-1);
		removeSpotlightBuffs(player, state);
	}

	static void deactivateSpotlight(ServerPlayerEntity player, boolean disablePassive) {
		if (disablePassive) {
			SPOTLIGHT_PASSIVE_ENABLED.remove(player.getUuid());
		}

		SpotlightState state = SPOTLIGHT_STATES.get(player.getUuid());
		if (state == null) {
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), SPOTLIGHT_ATTACK_DAMAGE_MODIFIER_ID);
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), SPOTLIGHT_MOVEMENT_SPEED_MODIFIER_ID);
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), SPOTLIGHT_MAX_HEALTH_MODIFIER_ID);
			removeAttributeModifier(player.getAttributeInstance(EntityAttributes.SCALE), SPOTLIGHT_SCALE_MODIFIER_ID);
			if (player.getHealth() > player.getMaxHealth()) {
				player.setHealth(player.getMaxHealth());
			}
			return;
		}

		state.viewerLastLookTicks().clear();
		state.currentStage(0);
		state.downgradeDeadlineTick(-1);
		removeSpotlightBuffs(player, state);
		if (disablePassive) {
			SPOTLIGHT_STATES.remove(player.getUuid());
		}
	}

	static void deactivateComedicRewrite(ServerPlayerEntity player) {
		COMEDIC_REWRITE_PASSIVE_ENABLED.remove(player.getUuid());
	}

	static SpotlightStageSettings spotlightStageSettingsFor(int stage) {
		return switch (stage) {
			case 1 -> SPOTLIGHT_STAGE_ONE;
			case 2 -> SPOTLIGHT_STAGE_TWO;
			case 3 -> SPOTLIGHT_STAGE_THREE;
			default -> null;
		};
	}

	static int wittyOneLinerCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, WITTY_ONE_LINER_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int comedicRewriteCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, COMEDIC_REWRITE_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int comedicAssistantCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, COMEDIC_ASSISTANT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int plusUltraCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, PLUS_ULTRA_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int ignitionCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player) || isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, IGNITION_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int searingDashCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player) || isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, SEARING_DASH_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int cinderMarkCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player) || isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, CINDER_MARK_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int engineHeartCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player) || isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, ENGINE_HEART_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int overrideCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player) || isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, OVERRIDE_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static void startComedicRewriteCooldown(UUID playerId, int currentTick) {
		if (isCooldownDeferredByOrionsGambit(playerId, MagicAbility.COMEDIC_REWRITE) || COMEDIC_REWRITE_COOLDOWN_TICKS <= 0) {
			COMEDIC_REWRITE_COOLDOWN_END_TICK.remove(playerId);
			return;
		}
		COMEDIC_REWRITE_COOLDOWN_END_TICK.put(
			playerId,
			currentTick + adjustedCooldownTicks(playerId, MagicAbility.COMEDIC_REWRITE, COMEDIC_REWRITE_COOLDOWN_TICKS, currentTick)
		);
	}

	static void startComedicAssistantCooldown(UUID playerId, int currentTick, int cooldownTicks) {
		int safeCooldownTicks = Math.max(0, cooldownTicks);
		if (isCooldownDeferredByOrionsGambit(playerId, MagicAbility.COMEDIC_ASSISTANT)) {
			trackOrionsGambitCooldownOverride(playerId, MagicAbility.COMEDIC_ASSISTANT, safeCooldownTicks);
			COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(playerId);
			return;
		}
		if (safeCooldownTicks <= 0) {
			COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(playerId);
			return;
		}
		COMEDIC_ASSISTANT_COOLDOWN_END_TICK.put(
			playerId,
			currentTick + adjustedCooldownTicks(playerId, MagicAbility.COMEDIC_ASSISTANT, safeCooldownTicks, currentTick)
		);
	}

	static void startPlusUltraCooldown(UUID playerId, int currentTick, int cooldownTicks) {
		int safeCooldownTicks = Math.max(0, cooldownTicks);
		if (isCooldownDeferredByOrionsGambit(playerId, MagicAbility.PLUS_ULTRA)) {
			trackOrionsGambitCooldownOverride(playerId, MagicAbility.PLUS_ULTRA, safeCooldownTicks);
			PLUS_ULTRA_COOLDOWN_END_TICK.remove(playerId);
			return;
		}
		if (safeCooldownTicks <= 0) {
			PLUS_ULTRA_COOLDOWN_END_TICK.remove(playerId);
			return;
		}
		PLUS_ULTRA_COOLDOWN_END_TICK.put(
			playerId,
			currentTick + adjustedCooldownTicks(playerId, MagicAbility.PLUS_ULTRA, safeCooldownTicks, currentTick)
		);
	}

	static void startEngineHeartCooldown(UUID playerId, int currentTick, int cooldownTicks) {
		int safeCooldownTicks = Math.max(0, cooldownTicks);
		if (isCooldownDeferredByOrionsGambit(playerId, MagicAbility.ENGINE_HEART)) {
			trackOrionsGambitCooldownOverride(playerId, MagicAbility.ENGINE_HEART, safeCooldownTicks);
			ENGINE_HEART_COOLDOWN_END_TICK.remove(playerId);
			return;
		}
		if (safeCooldownTicks <= 0) {
			ENGINE_HEART_COOLDOWN_END_TICK.remove(playerId);
			return;
		}
		ENGINE_HEART_COOLDOWN_END_TICK.put(
			playerId,
			currentTick + adjustedCooldownTicks(playerId, MagicAbility.ENGINE_HEART, safeCooldownTicks, currentTick)
		);
	}

	static boolean hasActiveComedicRewriteImmunity(UUID playerId, int currentTick) {
		int endTick = COMEDIC_REWRITE_IMMUNITY_END_TICK.getOrDefault(playerId, 0);
		if (endTick <= currentTick) {
			COMEDIC_REWRITE_IMMUNITY_END_TICK.remove(playerId);
			return false;
		}
		return true;
	}

	static boolean hasActiveComedicRewriteFallProtection(UUID playerId, int currentTick) {
		int endTick = COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.getOrDefault(playerId, 0);
		if (endTick <= currentTick) {
			COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.remove(playerId);
			return false;
		}
		return true;
	}

	static WittyOneLinerTierSettings selectWittyOneLinerTier(ServerPlayerEntity player) {
		int lowWeight = Math.max(0, WITTY_ONE_LINER_LOW_TIER.selectionWeight());
		int midWeight = Math.max(0, WITTY_ONE_LINER_MID_TIER.selectionWeight());
		int highWeight = Math.max(0, WITTY_ONE_LINER_HIGH_TIER.selectionWeight());
		int totalWeight = lowWeight + midWeight + highWeight;
		if (totalWeight <= 0) {
			return WITTY_ONE_LINER_LOW_TIER;
		}

		int roll = player.getRandom().nextInt(totalWeight);
		if (roll < lowWeight) {
			return WITTY_ONE_LINER_LOW_TIER;
		}
		roll -= lowWeight;
		if (roll < midWeight) {
			return WITTY_ONE_LINER_MID_TIER;
		}
		return WITTY_ONE_LINER_HIGH_TIER;
	}

	static String selectRandomJoke(ServerPlayerEntity player, List<String> jokes) {
		if (jokes == null || jokes.isEmpty()) {
			return null;
		}

		return jokes.get(player.getRandom().nextInt(jokes.size()));
	}

	static void applyWittyOneLinerEffects(ServerPlayerEntity target, WittyOneLinerTierSettings settings) {
		addConfiguredStatusEffect(target, StatusEffects.SLOWNESS, settings.effectDurationTicks(), settings.slownessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.WEAKNESS, settings.effectDurationTicks(), settings.weaknessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.BLINDNESS, settings.effectDurationTicks(), settings.blindnessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.NAUSEA, settings.effectDurationTicks(), settings.nauseaAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.DARKNESS, settings.effectDurationTicks(), settings.darknessAmplifier());
		addConfiguredStatusEffect(target, StatusEffects.WEAVING, settings.effectDurationTicks(), settings.weavingAmplifier());
		if (settings.applyGlowing()) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, settings.effectDurationTicks(), 0, true, true, true));
		}
	}

	static void addConfiguredStatusEffect(
		LivingEntity target,
		net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect,
		int durationTicks,
		int amplifier
	) {
		if (durationTicks <= 0 || amplifier < 0) {
			return;
		}

		target.addStatusEffect(new StatusEffectInstance(effect, durationTicks, amplifier, true, true, true));
	}

	static void sendWittyOneLinerOverlay(ServerPlayerEntity caster, ServerPlayerEntity target, String joke, int colorRgb) {
		JesterJokeOverlayPayload payload = new JesterJokeOverlayPayload(
			joke,
			colorRgb,
			WITTY_ONE_LINER_OVERLAY_FADE_IN_TICKS,
			WITTY_ONE_LINER_OVERLAY_STAY_TICKS,
			WITTY_ONE_LINER_OVERLAY_FADE_OUT_TICKS
		);
		ServerPlayNetworking.send(caster, payload);
		if (target != caster) {
			ServerPlayNetworking.send(target, payload);
		}
	}

	static boolean canSpendAbilityCost(ServerPlayerEntity player, int manaCost) {
		int adjustedManaCost = adjustCelestialAlignmentManaCost(player, GreedDomainRuntime.adjustManaCost(player, manaCost));
		return isTestingMode(player) || isOrionsGambitManaCostSuppressed(player) || MagicPlayerData.getMana(player) >= Math.max(0, adjustedManaCost);
	}

	static boolean hasFullManaBar(ServerPlayerEntity player) {
		return isTestingMode(player)
			|| isOrionsGambitManaCostSuppressed(player)
			|| MagicPlayerData.getMana(player) >= MagicPlayerData.MAX_MANA;
	}

	static boolean hasRequiredManaPercent(ServerPlayerEntity player, double percent) {
		if (isTestingMode(player) || isOrionsGambitManaCostSuppressed(player)) {
			return true;
		}
		double threshold = manaFromPercentExact(MathHelper.clamp(percent, 0.0, 100.0));
		return MagicPlayerData.getMana(player) >= Math.ceil(threshold);
	}

	static void spendAbilityCost(ServerPlayerEntity player, int manaCost) {
		int adjustedManaCost = adjustCelestialAlignmentManaCost(player, GreedDomainRuntime.adjustManaCost(player, manaCost));
		if (isTestingMode(player)) {
			MagicPlayerData.setMana(player, MagicPlayerData.MAX_MANA);
			MagicPlayerData.setDepletedRecoveryMode(player, false);
			return;
		}

		if (!isOrionsGambitManaCostSuppressed(player) && adjustedManaCost > 0) {
			MagicPlayerData.setMana(player, Math.max(0, MagicPlayerData.getMana(player) - adjustedManaCost));
		}
		MagicPlayerData.setDepletedRecoveryMode(player, false);
	}

	static void consumeFullManaBar(ServerPlayerEntity player) {
		if (isTestingMode(player)) {
			MagicPlayerData.setMana(player, MagicPlayerData.MAX_MANA);
			MagicPlayerData.setDepletedRecoveryMode(player, false);
			return;
		}
		if (!isOrionsGambitManaCostSuppressed(player)) {
			MagicPlayerData.setMana(player, 0);
		}
		MagicPlayerData.setDepletedRecoveryMode(player, false);
	}

	static boolean isTestingMode(ServerPlayerEntity player) {
		return player != null && TEST_MODE_PLAYERS.contains(player.getUuid());
	}

	public static boolean setTestingMode(ServerPlayerEntity player, boolean enabled) {
		if (player == null) {
			return false;
		}

		boolean changed = enabled ? TEST_MODE_PLAYERS.add(player.getUuid()) : TEST_MODE_PLAYERS.remove(player.getUuid());
		if (enabled) {
			MagicPlayerData.setMana(player, MagicPlayerData.MAX_MANA);
			MagicPlayerData.setDepletedRecoveryMode(player, false);
		}
		resetAllCooldowns(player);
		return changed;
	}

	public static boolean setMagicSchool(ServerPlayerEntity player, MagicSchool school) {
		if (player == null || school == null || !school.isMagic()) {
			return false;
		}

		clearAllRuntimeState(player);
		MagicPlayerData.clear(player);
		MagicPlayerData.unlock(player, school);
		MagicPlayerData.refillMana(player);
		return true;
	}

	public static int setBurningPassionStage(ServerPlayerEntity player, int stage) {
		if (player == null || player.getEntityWorld().getServer() == null) {
			return 0;
		}

		int currentTick = player.getEntityWorld().getServer().getTicks();
		BurningPassionIgnitionState state = ensureAdminBurningPassionIgnitionState(player, currentTick);
		if (state == null) {
			return 0;
		}

		state.currentStage = MathHelper.clamp(stage, 1, 3);
		state.stageStartTick = currentTick;
		state.auraPlayersInside.clear();
		state.boundaryCooldownEndTickByTarget.clear();
		syncBurningPassionHud(player);
		return 1;
	}

	public static int advanceBurningPassionStageForTesting(ServerPlayerEntity player) {
		if (player == null || player.getEntityWorld().getServer() == null) {
			return 0;
		}

		int currentTick = player.getEntityWorld().getServer().getTicks();
		BurningPassionIgnitionState state = ensureAdminBurningPassionIgnitionState(player, currentTick);
		if (state == null || state.currentStage >= 3) {
			return 0;
		}

		state.currentStage = Math.min(3, state.currentStage + 1);
		state.stageStartTick = currentTick;
		state.auraPlayersInside.clear();
		state.boundaryCooldownEndTickByTarget.clear();
		syncBurningPassionHud(player);
		return 1;
	}

	public static int setBurningPassionStageProgressSeconds(ServerPlayerEntity player, int seconds) {
		if (player == null || player.getEntityWorld().getServer() == null) {
			return 0;
		}

		int currentTick = player.getEntityWorld().getServer().getTicks();
		BurningPassionIgnitionState state = ensureAdminBurningPassionIgnitionState(player, currentTick);
		if (state == null) {
			return 0;
		}

		int stageDuration = burningPassionStageDurationTicks(state.currentStage);
		int progressTicks = MathHelper.clamp(Math.max(0, seconds) * TICKS_PER_SECOND, 0, stageDuration);
		state.stageStartTick = currentTick - progressTicks;
		state.auraPlayersInside.clear();
		state.boundaryCooldownEndTickByTarget.clear();
		applyIgnition(player, currentTick);
		return 1;
	}

	public static int forceBurningPassionOverride(ServerPlayerEntity player) {
		if (player == null) {
			return 0;
		}
		if (!MagicPlayerData.hasMagic(player)) {
			player.sendMessage(Text.translatable("message.magic.no_access"), true);
			return 0;
		}
		if (MagicPlayerData.getSchool(player) != MagicSchool.BURNING_PASSION) {
			player.sendMessage(Text.translatable("message.magic.ability.wrong_school", MagicSchool.BURNING_PASSION.displayName()), true);
			return 0;
		}
		if (isMagicSuppressed(player)) {
			player.sendMessage(Text.translatable("message.magic.empty_embrace.magic_blocked"), true);
			return 0;
		}
		if (isLockedByForeignLoveDomain(player)) {
			player.sendMessage(Text.translatable("message.magic.love_domain.abilities_locked"), true);
			return 0;
		}
		if (!MagicConfig.get().abilityAccess.isAbilityUnlocked(player.getUuid(), MagicAbility.OVERRIDE)) {
			player.sendMessage(Text.translatable("message.magic.ability.locked", MagicAbility.OVERRIDE.displayName()), true);
			return 0;
		}
		if (player.getEntityWorld().getServer() == null) {
			return 0;
		}

		int currentTick = player.getEntityWorld().getServer().getTicks();
		MagicAbility currentActiveAbility = activeAbility(player);
		if (currentActiveAbility == MagicAbility.ABSOLUTE_ZERO) {
			player.sendMessage(Text.translatable("message.magic.frost.maximum_locked"), true);
			return 0;
		}
		if (currentActiveAbility == MagicAbility.ORIONS_GAMBIT) {
			player.sendMessage(Text.translatable("message.magic.ability.orions_gambit_locked"), true);
			return 0;
		}
		if (currentActiveAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			player.sendMessage(Text.translatable("message.magic.constellation.celestial_alignment.active"), true);
			return 0;
		}
		if (currentActiveAbility == MagicAbility.PLUS_ULTRA) {
			player.sendMessage(Text.translatable("message.magic.ability.plus_ultra_locked"), true);
			return 0;
		}
		if (GreedDomainRuntime.isMagicSealed(player, currentTick)) {
			player.sendMessage(Text.translatable("message.magic.greed.magic_sealed"), true);
			return 0;
		}
		if (currentActiveAbility != MagicAbility.OVERRIDE && GreedDomainRuntime.beforeAbilityUse(player, MagicAbility.OVERRIDE, currentTick)) {
			return 0;
		}
		if (GreedAbilityRuntime.isAbilityUseLocked(player, currentTick)) {
			player.sendMessage(Text.translatable("message.magic.greed.ability_locked"), true);
			return 0;
		}
		return tryStartOverride(player, currentTick, true) ? 1 : 0;
	}

	public static int setFrostStage(ServerPlayerEntity player, int stage) {
		FrostStageState state = ensureAdminFrostStageState(player);
		if (state == null) {
			return 0;
		}

		int clampedStage = MathHelper.clamp(stage, 1, 3);
		state.currentStage = clampedStage;
		state.highestUnlockedStage = clampedStage;
		state.progressTicks = 0;
		state.stageThreeHoldTicks = 0;
		syncFrostStageHud(player);
		return 1;
	}

	public static int clearFrostStage(ServerPlayerEntity player) {
		if (player == null || MagicPlayerData.getSchool(player) != MagicSchool.FROST || player.getEntityWorld().getServer() == null) {
			return 0;
		}

		endFrostStagedMode(player, player.getEntityWorld().getServer().getTicks(), FrostStageEndReason.CLEAR_ALL, false, false);
		return 1;
	}

	public static int advanceFrostStageForTesting(ServerPlayerEntity player) {
		FrostStageState state = ensureAdminFrostStageState(player);
		if (state == null || state.currentStage >= 3) {
			return 0;
		}

		state.currentStage = Math.min(3, state.currentStage + 1);
		state.highestUnlockedStage = Math.max(state.highestUnlockedStage, state.currentStage);
		state.progressTicks = 0;
		state.stageThreeHoldTicks = 0;
		syncFrostStageHud(player);
		return 1;
	}

	public static int setFrostStageProgressSeconds(ServerPlayerEntity player, int seconds) {
		FrostStageState state = ensureAdminFrostStageState(player);
		if (state == null) {
			return 0;
		}

		int requirement = frostProgressRequirementTicks(state.currentStage);
		if (requirement <= 0) {
			state.progressTicks = 0;
			state.highestUnlockedStage = Math.max(state.highestUnlockedStage, state.currentStage);
			syncFrostStageHud(player);
			return 1;
		}

		int progressTicks = MathHelper.clamp(Math.max(0, seconds) * TICKS_PER_SECOND, 0, requirement);
		state.progressTicks = progressTicks;
		if (progressTicks >= requirement) {
			state.highestUnlockedStage = Math.min(3, state.currentStage + 1);
		} else {
			state.highestUnlockedStage = state.currentStage;
		}
		syncFrostStageHud(player);
		return 1;
	}

	public static int setFrostStageThreeProgressSeconds(ServerPlayerEntity player, int seconds) {
		FrostStageState state = ensureAdminFrostStageState(player);
		if (state == null) {
			return 0;
		}

		int requirement = Math.max(0, FROST_CONFIG.progression.maximumUnlockTicks);
		state.currentStage = 3;
		state.highestUnlockedStage = 3;
		state.progressTicks = 0;
		state.stageThreeHoldTicks = MathHelper.clamp(Math.max(0, seconds) * TICKS_PER_SECOND, 0, requirement);
		syncFrostStageHud(player);
		return 1;
	}
}


