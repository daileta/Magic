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


abstract class MagicAbilityCoreSupport {
	enum ManipulationStage {
		WAITING,
		LINKED
	}

	static final class ManipulationState {
		ManipulationStage stage;
		RegistryKey<World> targetDimension;
		UUID targetId;
		Vec3d lockedCasterPos;
		Vec3d controlledPos;
		final int startedTick;

		ManipulationState(
			ManipulationStage stage,
			RegistryKey<World> targetDimension,
			UUID targetId,
			Vec3d lockedCasterPos,
			Vec3d controlledPos,
			int startedTick
		) {
			this.stage = stage;
			this.targetDimension = targetDimension;
			this.targetId = targetId;
			this.lockedCasterPos = lockedCasterPos;
			this.controlledPos = controlledPos;
			this.startedTick = startedTick;
		}

		static ManipulationState waiting(int startedTick, Vec3d casterPos) {
			return new ManipulationState(
				ManipulationStage.WAITING,
				null,
				null,
				casterPos,
				Vec3d.ZERO,
				startedTick
			);
		}
	}

	static final class ManipulationProxyState {
		final Vec3d originalPos;
		final float yaw;
		final float pitch;

		ManipulationProxyState(Vec3d originalPos, float yaw, float pitch) {
			this.originalPos = originalPos;
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}

	static final class ManipulationLookState {
		final float yaw;
		final float pitch;

		ManipulationLookState(float yaw, float pitch) {
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}

	static void onAbilityRequested(ServerPlayerEntity player, int abilitySlot) {
		MagicAbilityManager.onAbilityRequested(player, abilitySlot);
	}

	static LoveAndDomainRuntimeTypes.DomainClashState domainClashStateForParticipant(UUID playerId) {
		return DomainClashService.domainClashStateForParticipant(playerId);
	}

	static boolean isPositionInsideAnyDomain(RegistryKey<World> dimension, double x, double y, double z) {
		return DomainExpansionService.isPositionInsideAnyDomain(dimension, x, y, z);
	}

	static boolean isTestingMode(ServerPlayerEntity player) {
		return AbilityAdminAndPassiveSupport.isTestingMode(player);
	}

	static int ignitionCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return AbilityAdminAndPassiveSupport.ignitionCooldownRemaining(player, currentTick);
	}

	static int searingDashCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return AbilityAdminAndPassiveSupport.searingDashCooldownRemaining(player, currentTick);
	}

	static int cinderMarkCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return AbilityAdminAndPassiveSupport.cinderMarkCooldownRemaining(player, currentTick);
	}

	static int engineHeartCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return AbilityAdminAndPassiveSupport.engineHeartCooldownRemaining(player, currentTick);
	}

	static int overrideCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return AbilityAdminAndPassiveSupport.overrideCooldownRemaining(player, currentTick);
	}

	static int wittyOneLinerCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return AbilityAdminAndPassiveSupport.wittyOneLinerCooldownRemaining(player, currentTick);
	}

	static int comedicRewriteCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return AbilityAdminAndPassiveSupport.comedicRewriteCooldownRemaining(player, currentTick);
	}

	static int comedicAssistantCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return AbilityAdminAndPassiveSupport.comedicAssistantCooldownRemaining(player, currentTick);
	}

	static int plusUltraCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return AbilityAdminAndPassiveSupport.plusUltraCooldownRemaining(player, currentTick);
	}

	static void refreshStatusEffect(
		LivingEntity entity,
		RegistryEntry<StatusEffect> effect,
		int durationTicks,
		int amplifier,
		boolean ambient,
		boolean showParticles,
		boolean showIcon
	) {
		CombatEffectService.refreshStatusEffect(
			entity,
			effect,
			durationTicks,
			amplifier,
			ambient,
			showParticles,
			showIcon
		);
	}

	static void runWithDomainTeleportBypass(Runnable action) {
		DomainClashService.runWithDomainTeleportBypass(action);
	}

	static void persistDomainRuntimeState(MinecraftServer server) {
		DomainPersistenceService.persistDomainRuntimeState(server);
	}

	static boolean isOrionsGambitCooldownSuppressed(ServerPlayerEntity player) {
		return ConstellationAdvancedRuntime.isOrionsGambitCooldownSuppressed(player);
	}

	static void deactivateSpotlight(ServerPlayerEntity player, boolean disablePassive) {
		AbilityAdminAndPassiveSupport.deactivateSpotlight(player, disablePassive);
	}

	static void deactivateCassiopeia(ServerPlayerEntity player, boolean sendEmptyOutline) {
		CombatTargetingService.deactivateCassiopeia(player, sendEmptyOutline);
	}

	static void startComedicRewriteCooldown(UUID playerId, int currentTick) {
		AbilityAdminAndPassiveSupport.startComedicRewriteCooldown(playerId, currentTick);
	}

	static void startComedicAssistantCooldown(UUID playerId, int currentTick, int cooldownTicks) {
		AbilityAdminAndPassiveSupport.startComedicAssistantCooldown(playerId, currentTick, cooldownTicks);
	}

	static void startPlusUltraCooldown(UUID playerId, int currentTick, int cooldownTicks) {
		AbilityAdminAndPassiveSupport.startPlusUltraCooldown(playerId, currentTick, cooldownTicks);
	}

	static void debugManipulation(String message, Object... args) {
		MagicPacketService.debugManipulation(message, args);
	}

	static boolean isMagicTargetableEntity(Entity entity) {
		return CombatTargetingService.isMagicTargetableEntity(entity);
	}

	static MagicAbility activeAbility(PlayerEntity player) {
		return RuntimeResetService.activeAbility(player);
	}

	static void teleportDomainEntity(LivingEntity entity, double x, double y, double z, float yaw, float pitch) {
		DomainClashService.teleportDomainEntity(entity, x, y, z, yaw, pitch);
	}

	static boolean isFrostTeleportItemBlocked(PlayerEntity player, ItemStack stack) {
		return FrostEffectRuntime.isFrostTeleportItemBlocked(player, stack);
	}

	static boolean isLoveItemUseBlocked(PlayerEntity player) {
		return MagicPacketService.isLoveItemUseBlocked(player);
	}

	static boolean isDomainClashParticipantFrozen(PlayerEntity player) {
		return MagicPacketService.isDomainClashParticipantFrozen(player);
	}

	static boolean isConstellationTeleportItemBlocked(ServerPlayerEntity player, ItemStack stack) {
		return ConstellationEffectRuntime.isConstellationTeleportItemBlocked(player, stack);
	}

	static boolean isTeleportEscapeItem(ItemStack stack) {
		return ConstellationEffectRuntime.isTeleportEscapeItem(stack);
	}

	static boolean isItemUseBlocked(PlayerEntity player) {
		return MagicPacketService.isItemUseBlocked(player);
	}

	static boolean isAttackBlocked(PlayerEntity player) {
		return MagicPacketService.isAttackBlocked(player);
	}

	static boolean isDirectCasterDamageBlocked(ServerPlayerEntity attacker, LivingEntity target, int currentTick) {
		return MagicPacketService.isDirectCasterDamageBlocked(attacker, target, currentTick);
	}

	static void applyFrostStageHitEffects(ServerPlayerEntity caster, LivingEntity target, int currentTick) {
		FrostAbilityRequests.applyFrostStageHitEffects(caster, target, currentTick);
	}

	static boolean isBurningPassionFireExtinguished(LivingEntity target, boolean extinguishInWater, boolean extinguishInRain) {
		return BurningPassionTrailRuntime.isBurningPassionFireExtinguished(target, extinguishInWater, extinguishInRain);
	}

	static void dealBurningPassionFireDamage(
		LivingEntity target,
		UUID casterId,
		ServerWorld world,
		float damagePerTick,
		double fireResistantTargetDamageMultiplier,
		boolean fireDamageIgnoresFireResistance
	) {
		BurningPassionEffectRuntime.dealBurningPassionFireDamage(
			target,
			casterId,
			world,
			damagePerTick,
			fireResistantTargetDamageMultiplier,
			fireDamageIgnoresFireResistance
		);
	}

	static void setActiveAbility(ServerPlayerEntity player, MagicAbility ability) {
		RuntimeResetService.setActiveAbility(player, ability);
	}

	static void spawnHerculesImpactBurst(LivingEntity target) {
		ConstellationAdvancedRuntime.spawnHerculesImpactBurst(target);
	}

	static void spawnHerculesBurdenParticles(LivingEntity target) {
		ConstellationAdvancedRuntime.spawnHerculesBurdenParticles(target);
	}

	static DamageSource createTrueMagicDamageSource(ServerWorld world, Entity attacker) {
		return ConstellationAlignmentRuntime.createTrueMagicDamageSource(world, attacker);
	}

	static DamageSource createTrueMagicDamageSource(ServerWorld world) {
		return ConstellationAlignmentRuntime.createTrueMagicDamageSource(world);
	}

	static DamageSource createTrueMagicDamageSource(MinecraftServer server, ServerWorld world, UUID casterId) {
		return ConstellationAlignmentRuntime.createTrueMagicDamageSource(server, world, casterId);
	}

	static void releaseCelestialAlignmentState(
		MinecraftServer server,
		CelestialRuntimeTypes.CelestialAlignmentState state,
		boolean expired
	) {
		ConstellationAlignmentRuntime.releaseCelestialAlignmentState(server, state, expired);
	}

	static void clearCelestialGamaRayState(ServerPlayerEntity caster, boolean sendFeedback, boolean startCooldown) {
		ConstellationAlignmentRuntime.clearCelestialGamaRayState(caster, sendFeedback, startCooldown);
	}

	static void clearCelestialGamaRayState(ServerPlayerEntity caster, boolean sendFeedback, boolean startCooldown, int cooldownTicks) {
		ConstellationAlignmentRuntime.clearCelestialGamaRayState(caster, sendFeedback, startCooldown, cooldownTicks);
	}

	static void updateCelestialAlignmentState(
		MinecraftServer server,
		ServerPlayerEntity caster,
		CelestialRuntimeTypes.CelestialAlignmentState state,
		int currentTick
	) {
		ConstellationAlignmentRuntime.updateCelestialAlignmentState(server, caster, state, currentTick);
	}

	static void updateCelestialGeminiReplays(MinecraftServer server, int currentTick) {
		ConstellationEffectRuntime.updateCelestialGeminiReplays(server, currentTick);
	}

	static void updateCelestialGamaRayStates(MinecraftServer server, int currentTick) {
		ConstellationAlignmentRuntime.updateCelestialGamaRayStates(server, currentTick);
	}

	static int celestialAlignmentColor(CelestialRuntimeTypes.CelestialAlignmentConstellation constellation) {
		return ConstellationEffectRuntime.celestialAlignmentColor(constellation);
	}

	static void consumeFullManaBar(ServerPlayerEntity player) {
		AbilityAdminAndPassiveSupport.consumeFullManaBar(player);
	}

	static double manaFromPercentExact(double percent) {
		return AbilityMathAndConfigSupport.manaFromPercentExact(percent);
	}

	static void spendAbilityCost(ServerPlayerEntity player, int manaCost) {
		AbilityAdminAndPassiveSupport.spendAbilityCost(player, manaCost);
	}

	static void refreshCelestialMovementSpeedModifier(LivingEntity entity) {
		ConstellationEffectRuntime.refreshCelestialMovementSpeedModifier(entity);
	}

	static void updateSagittaConstellation(
		ServerWorld world,
		ServerPlayerEntity caster,
		CelestialRuntimeTypes.CelestialAlignmentState state,
		int currentTick
	) {
		ConstellationEffectRuntime.updateSagittaConstellation(world, caster, state, currentTick);
	}

	static void updateAquilaConstellation(
		ServerWorld world,
		ServerPlayerEntity caster,
		CelestialRuntimeTypes.CelestialAlignmentState state,
		int currentTick
	) {
		ConstellationEffectRuntime.updateAquilaConstellation(world, caster, state, currentTick);
	}

	static void spawnCraterSiphonEffects(ServerWorld world, CelestialRuntimeTypes.CelestialAlignmentState state) {
		ConstellationEffectRuntime.spawnCraterSiphonEffects(world, state);
	}

	static void releaseGeminiRecordedDamage(
		MinecraftServer server,
		ServerWorld world,
		CelestialRuntimeTypes.CelestialAlignmentState state
	) {
		ConstellationEffectRuntime.releaseGeminiRecordedDamage(server, world, state);
	}

	static void clearCelestialDelayQueueIfUnaffected(
		MinecraftServer server,
		UUID playerId,
		CelestialRuntimeTypes.CelestialAlignmentState ignoredState
	) {
		MagicPacketService.clearCelestialDelayQueueIfUnaffected(server, playerId, ignoredState);
	}

	static CelestialRuntimeTypes.CelestialPattern celestialPattern(
		CelestialRuntimeTypes.CelestialAlignmentConstellation constellation
	) {
		return ConstellationEffectRuntime.celestialPattern(constellation);
	}

	static void spawnCelestialAmbientEffects(ServerWorld world, CelestialRuntimeTypes.CelestialAlignmentState state) {
		ConstellationEffectRuntime.spawnCelestialAmbientEffects(world, state);
	}

	static String celestialAlignmentColorHex(CelestialRuntimeTypes.CelestialAlignmentConstellation constellation) {
		return ConstellationEffectRuntime.celestialAlignmentColorHex(constellation);
	}

	static void removeAttributeModifier(EntityAttributeInstance attributeInstance, Identifier modifierId) {
		AbilityAdminAndPassiveSupport.removeAttributeModifier(attributeInstance, modifierId);
	}

	static ServerPlayerEntity attackingPlayerFrom(DamageSource source) {
		return AbilityMathAndConfigSupport.attackingPlayerFrom(source);
	}

	static int resetAllCooldowns(ServerPlayerEntity player) {
		return RuntimeResetService.resetAllCooldowns(player);
	}

	static void setSignedAttributeModifier(
		EntityAttributeInstance attributeInstance,
		Identifier modifierId,
		double value,
		EntityAttributeModifier.Operation operation
	) {
		AbilityAdminAndPassiveSupport.setSignedAttributeModifier(attributeInstance, modifierId, value, operation);
	}

	static boolean deactivateDomainExpansion(ServerPlayerEntity player) {
		return DomainExpansionService.deactivateDomainExpansion(player);
	}

	static boolean isBeaconCoreProtected(ServerPlayerEntity player) {
		return DomainClashService.isBeaconCoreProtected(player);
	}

	static void activateManipulation(ServerPlayerEntity caster, ServerPlayerEntity target, int currentTick) {
		LoveAbilityRequests.activateManipulation(caster, target, currentTick);
	}

	static void tryAcquireManipulationTarget(ServerPlayerEntity caster, int currentTick) {
		LoveAbilityRequests.tryAcquireManipulationTarget(caster, currentTick);
	}

	static void deactivateManipulation(ServerPlayerEntity caster, boolean startCooldown) {
		RuntimeResetService.deactivateManipulation(caster, startCooldown);
	}

	static void deactivateManipulation(ServerPlayerEntity caster, boolean startCooldown, String reason) {
		RuntimeResetService.deactivateManipulation(caster, startCooldown, reason);
	}

	static void releaseManipulationState(
		UUID casterId,
		ManipulationState state,
		MinecraftServer server,
		ServerPlayerEntity caster
	) {
		RuntimeResetService.releaseManipulationState(casterId, state, server, caster);
	}

	static boolean isMagicSuppressed(PlayerEntity player) {
		return MagicPacketService.isMagicSuppressed(player);
	}

	static Vec3d normalizedHorizontalDirection(Vec3d direction, ServerPlayerEntity player) {
		return AbilityMathAndConfigSupport.normalizedHorizontalDirection(direction, player);
	}

	static float horizontalYawFromDirection(Vec3d direction) {
		return AbilityMathAndConfigSupport.horizontalYawFromDirection(direction);
	}

	static List<ServerPlayerEntity> comedicRewriteVisualViewers(ServerWorld world, Vec3d origin) {
		return JesterVisualRuntime.comedicRewriteVisualViewers(world, origin);
	}

	static List<ServerPlayerEntity> comedicRewriteVisualViewers(MinecraftServer server, List<UUID> viewerIds) {
		return JesterVisualRuntime.comedicRewriteVisualViewers(server, viewerIds);
	}

	static void queueComedicRewriteVisualCameo(
		MinecraftServer server,
		int durationTicks,
		List<ServerPlayerEntity> viewers,
		int chargeDurationTicks,
		JesterRuntimeTypes.ComedicRewriteVisualFollowState followState,
		Entity... entities
	) {
		JesterVisualRuntime.queueComedicRewriteVisualCameo(
			server,
			durationTicks,
			viewers,
			chargeDurationTicks,
			followState,
			entities
		);
	}

	static void queueComedicRewriteVisualCameo(
		MinecraftServer server,
		int durationTicks,
		List<ServerPlayerEntity> viewers,
		int chargeDurationTicks,
		JesterRuntimeTypes.ComedicRewriteVisualFollowState followState,
		JesterRuntimeTypes.ComedicRewriteVisualRotationState rotationState,
		Entity... entities
	) {
		JesterVisualRuntime.queueComedicRewriteVisualCameo(
			server,
			durationTicks,
			viewers,
			chargeDurationTicks,
			followState,
			rotationState,
			entities
		);
	}

	static boolean shouldKeepComedicAssistantEnabled(ServerPlayerEntity player) {
		return AbilityAdminAndPassiveSupport.shouldKeepComedicAssistantEnabled(player);
	}

	static boolean hasActivePlusUltra(ServerPlayerEntity player) {
		return SchoolAbilityRequestRouter.hasActivePlusUltra(player);
	}

	static boolean shouldKeepPlusUltraEnabled(ServerPlayerEntity player) {
		return SchoolAbilityRequestRouter.shouldKeepPlusUltraEnabled(player);
	}

	static void addConfiguredStatusEffect(
		LivingEntity target,
		RegistryEntry<StatusEffect> effect,
		int durationTicks,
		int amplifier
	) {
		AbilityAdminAndPassiveSupport.addConfiguredStatusEffect(target, effect, durationTicks, amplifier);
	}

	static void spawnComedicAssistantSlimeVisual(LivingEntity target, ServerWorld world) {
		JesterVisualRuntime.spawnComedicAssistantSlimeVisual(target, world);
	}

	static void spawnComedicAssistantPandaVisual(LivingEntity target, ServerWorld world, Vec3d direction) {
		JesterVisualRuntime.spawnComedicAssistantPandaVisual(target, world, direction);
	}

	static void spawnComedicAssistantParrotVisual(LivingEntity target, ServerWorld world) {
		JesterVisualRuntime.spawnComedicAssistantParrotVisual(target, world);
	}

	static double resolveComedicAssistantCarryTargetY(LivingEntity target, ServerWorld world, double liftHeight) {
		return JesterVisualRuntime.resolveComedicAssistantCarryTargetY(target, world, liftHeight);
	}

	static void spawnComedicAssistantDivineLightningVisual(LivingEntity target, ServerWorld world, int strikeCount) {
		JesterVisualRuntime.spawnComedicAssistantDivineLightningVisual(target, world, strikeCount);
	}

	static void spawnComedicAssistantAcmeVisual(LivingEntity target, ServerWorld world) {
		JesterVisualRuntime.spawnComedicAssistantAcmeVisual(target, world);
	}

	static void spawnComedicAssistantCaneVisual(LivingEntity target, ServerWorld world, Vec3d direction) {
		JesterVisualRuntime.spawnComedicAssistantCaneVisual(target, world, direction);
	}

	static void teleportComedicRewritePlayer(ServerPlayerEntity player, ServerWorld world, Vec3d targetPos) {
		AbilityMathAndConfigSupport.teleportComedicRewritePlayer(player, world, targetPos);
	}

	static void deactivateLoveAtFirstSight(ServerPlayerEntity player) {
		RuntimeResetService.deactivateLoveAtFirstSight(player);
	}

	static void clearAllRuntimeState(ServerPlayerEntity player) {
		RuntimeResetService.clearAllRuntimeState(player);
	}

	static ElementalRuntimeTypes.BurningPassionIgnitionState ensureAdminBurningPassionIgnitionState(
		ServerPlayerEntity player,
		int currentTick
	) {
		return FrostEffectRuntime.ensureAdminBurningPassionIgnitionState(player, currentTick);
	}

	static void syncBurningPassionHud(ServerPlayerEntity player) {
		BurningPassionAbilityRequests.syncBurningPassionHud(player);
	}

	static int burningPassionStageDurationTicks(int stage) {
		return BurningPassionAbilityRequests.burningPassionStageDurationTicks(stage);
	}

	static Vec3d findFrostGroundAnchor(ServerWorld world, Vec3d samplePoint, int searchUpBlocks, int searchDownBlocks) {
		return FrostEffectRuntime.findFrostGroundAnchor(world, samplePoint, searchUpBlocks, searchDownBlocks);
	}

	static void applyIgnition(ServerPlayerEntity player, int currentTick) {
		BurningPassionAbilityRequests.applyIgnition(player, currentTick);
	}

	static boolean tryStartOverride(ServerPlayerEntity player, int currentTick, boolean bypassDomainRequirement) {
		return BurningPassionAbilityRequests.tryStartOverride(player, currentTick, bypassDomainRequirement);
	}

	static ElementalRuntimeTypes.FrostStageState ensureAdminFrostStageState(ServerPlayerEntity player) {
		return FrostEffectRuntime.ensureAdminFrostStageState(player);
	}

	static void syncFrostStageHud(ServerPlayerEntity player) {
		FrostEffectRuntime.syncFrostStageHud(player);
	}

	static void endFrostStagedMode(
		ServerPlayerEntity player,
		int currentTick,
		ElementalRuntimeTypes.FrostStageEndReason reason,
		boolean startCooldown,
		boolean sendDeactivatedMessage
	) {
		FrostAbilityRequests.endFrostStagedMode(player, currentTick, reason, startCooldown, sendDeactivatedMessage);
	}

	static int frostProgressRequirementTicks(int currentStage) {
		return FrostAbilityRequests.frostProgressRequirementTicks(currentStage);
	}

	static void discardBurningPassionCinderMarkDisplay(
		ServerWorld world,
		ElementalRuntimeTypes.BurningPassionCinderMarkState state
	) {
		BurningPassionEffectRuntime.discardBurningPassionCinderMarkDisplay(world, state);
	}

	static ElementalRuntimeTypes.BurningPassionIgnitionState burningPassionIgnitionState(ServerPlayerEntity player) {
		return BurningPassionAbilityRequests.burningPassionIgnitionState(player);
	}

	static void showBurningPassionHudNotification(ServerPlayerEntity player, String text, int currentTick, int durationTicks) {
		BurningPassionAbilityRequests.showBurningPassionHudNotification(player, text, currentTick, durationTicks);
	}

	static void endOwnedDomain(
		UUID ownerId,
		LoveAndDomainRuntimeTypes.DomainExpansionState state,
		MinecraftServer server,
		int currentTick
	) {
		DomainExpansionService.endOwnedDomain(ownerId, state, server, currentTick);
	}

	static boolean isInsideDomainInterior(
		ServerPlayerEntity player,
		LoveAndDomainRuntimeTypes.DomainExpansionState state
	) {
		return DomainExpansionService.isInsideDomainInterior(player, state);
	}

	static boolean isInsideDomainInterior(double horizontalDistanceSq, double y, int innerRadius, int innerHeight) {
		return DomainClashService.isInsideDomainInterior(horizontalDistanceSq, y, innerRadius, innerHeight);
	}

	static double clampBurningPassionHeat(double heatPercent) {
		return BurningPassionAbilityRequests.clampBurningPassionHeat(heatPercent);
	}

	static boolean canPayBurningPassionHealthDemand(
		ServerPlayerEntity player,
		ElementalRuntimeTypes.BurningPassionIgnitionState ignitionState,
		MagicAbility ability
	) {
		return BurningPassionAbilityRequests.canPayBurningPassionHealthDemand(player, ignitionState, ability);
	}

	static boolean tryPayBurningPassionManaCost(
		ServerPlayerEntity player,
		ElementalRuntimeTypes.BurningPassionIgnitionState ignitionState,
		MagicAbility ability,
		double manaCostPercent
	) {
		return BurningPassionAbilityRequests.tryPayBurningPassionManaCost(player, ignitionState, ability, manaCostPercent);
	}

	static void payBurningPassionHealthDemand(
		ServerPlayerEntity player,
		ElementalRuntimeTypes.BurningPassionIgnitionState ignitionState,
		MagicAbility ability
	) {
		BurningPassionAbilityRequests.payBurningPassionHealthDemand(player, ignitionState, ability);
	}

	static void deactivateMartyrsFlame(ServerPlayerEntity player, boolean startCooldown) {
		RuntimeResetService.deactivateMartyrsFlame(player, startCooldown);
	}

	static boolean isLockedBurningPassionStage(ServerPlayerEntity player, int stage) {
		return RuntimeResetService.isLockedBurningPassionStage(player, stage);
	}

	static void removeStatusEffectIfMatching(
		LivingEntity entity,
		RegistryEntry<StatusEffect> effect,
		Set<Integer> allowedAmplifiers
	) {
		FrostEffectRuntime.removeStatusEffectIfMatching(entity, effect, allowedAmplifiers);
	}

	static void clearFrostMaximumState(ServerPlayerEntity player, boolean clearCooldown) {
		FrostAbilityRequests.clearFrostMaximumState(player, clearCooldown);
	}

	static void removeFrostStageCasterBuffs(ServerPlayerEntity player) {
		FrostAbilityRequests.removeFrostStageCasterBuffs(player);
	}

	static boolean isLockedFrostStage(ServerPlayerEntity player, int stage) {
		return RuntimeResetService.isLockedFrostStage(player, stage);
	}

	static void deactivateAbsoluteZero(ServerPlayerEntity player) {
		RuntimeResetService.deactivateAbsoluteZero(player);
	}

	static void deactivatePlanckHeat(ServerPlayerEntity player) {
		RuntimeResetService.deactivatePlanckHeat(player);
	}

	static void deactivateTillDeathDoUsPart(
		ServerPlayerEntity player,
		LoveAndDomainRuntimeTypes.TillDeathDoUsPartEndReason reason,
		boolean sendFeedback
	) {
		RuntimeResetService.deactivateTillDeathDoUsPart(player, reason, sendFeedback);
	}

	static void startTillDeathDoUsPartCooldown(UUID playerId, int currentTick) {
		RuntimeResetService.startTillDeathDoUsPartCooldown(playerId, currentTick);
	}

	static void endPlusUltra(
		ServerPlayerEntity player,
		int currentTick,
		CelestialRuntimeTypes.PlusUltraEndMode endMode,
		boolean sendFeedback
	) {
		SchoolAbilityRequestRouter.endPlusUltra(player, currentTick, endMode, sendFeedback);
	}

	static void applyPlusUltra(ServerPlayerEntity player, int currentTick) {
		SchoolAbilityRequestRouter.applyPlusUltra(player, currentTick);
	}

	static Map<UUID, LoveAndDomainRuntimeTypes.DomainCapturedEntityState> captureDomainEntities(
		ServerWorld world,
		double centerX,
		double centerZ,
		int baseY,
		int radius,
		int height
	) {
		return DomainClashService.captureDomainEntities(world, centerX, centerZ, baseY, radius, height);
	}

	static boolean shouldPreserveBeaconAnchor(BlockState state) {
		return DomainClashService.shouldPreserveBeaconAnchor(state);
	}

	static void moveCapturedEntitiesIntoDomain(
		ServerWorld world,
		Iterable<LoveAndDomainRuntimeTypes.DomainCapturedEntityState> capturedEntities,
		double centerX,
		double centerZ,
		int baseY,
		int innerRadius,
		int innerHeight
	) {
		DomainClashService.moveCapturedEntitiesIntoDomain(
			world,
			capturedEntities,
			centerX,
			centerZ,
			baseY,
			innerRadius,
			innerHeight
		);
	}

	static void cancelDomainClash(UUID ownerId, MinecraftServer server) {
		DomainClashService.cancelDomainClash(ownerId, server);
	}

	static void restoreCapturedEntities(MinecraftServer server, LoveAndDomainRuntimeTypes.DomainExpansionState state) {
		DomainClashService.restoreCapturedEntities(server, state);
	}

	static void enforceCapturedEntitiesInsideDomain(ServerWorld world, LoveAndDomainRuntimeTypes.DomainExpansionState state) {
		DomainClashService.enforceCapturedEntitiesInsideDomain(world, state);
	}

	static void maintainDomainShell(ServerWorld world, LoveAndDomainRuntimeTypes.DomainExpansionState state) {
		DomainClashService.maintainDomainShell(world, state);
	}

	static int domainClashTitleDurationTicks() {
		return DomainClashService.domainClashTitleDurationTicks();
	}

	static int domainClashIntroLockTicks() {
		return DomainClashService.domainClashIntroLockTicks();
	}

	static void applySplitDomainInteriorVisuals(
		ServerWorld world,
		LoveAndDomainRuntimeTypes.DomainExpansionState state,
		MagicAbility ownerAbility,
		MagicAbility challengerAbility
	) {
		DomainClashService.applySplitDomainInteriorVisuals(world, state, ownerAbility, challengerAbility);
	}

	static Vec3d resolveGreedDomainClashLockedPosition(
		ServerWorld world,
		LoveAndDomainRuntimeTypes.DomainExpansionState state,
		LivingEntity participant,
		Vec3d preferredPos
	) {
		return DomainClashService.resolveGreedDomainClashLockedPosition(world, state, participant, preferredPos);
	}

	static void showDomainClashTitle(ServerPlayerEntity player) {
		DomainClashService.showDomainClashTitle(player);
	}

	static void lockDomainClashParticipant(ServerPlayerEntity player, Vec3d lockedPos, Vec3d opponentEyePos) {
		DomainClashService.lockDomainClashParticipant(player, lockedPos, opponentEyePos);
	}

	static void clearDomainClashUiFor(
		LoveAndDomainRuntimeTypes.DomainClashState clash,
		MinecraftServer server
	) {
		DomainClashService.clearDomainClashUiFor(clash, server);
	}

	static boolean isDomainClashFrozen(LoveAndDomainRuntimeTypes.DomainClashState clash, int currentTick) {
		return DomainClashService.isDomainClashFrozen(clash, currentTick);
	}

	static void spawnDomainClashParticles(
		ServerWorld world,
		LoveAndDomainRuntimeTypes.DomainExpansionState state,
		int particlesPerTick
	) {
		DomainClashService.spawnDomainClashParticles(world, state, particlesPerTick);
	}

	static int domainClashProgressPercent(double damageDealt) {
		return DomainClashService.domainClashProgressPercent(damageDealt);
	}

	static int domainClashInstructionVisibilityPercent(
		LoveAndDomainRuntimeTypes.DomainClashState clash,
		int currentTick
	) {
		return DomainClashService.domainClashInstructionVisibilityPercent(clash, currentTick);
	}

	static void resolveDomainClash(MinecraftServer server, UUID ownerId, UUID winnerId, int currentTick) {
		DomainClashService.resolveDomainClash(server, ownerId, winnerId, currentTick);
	}

	static void updateCelestialDelayedActions(MinecraftServer server, int currentTick) {
		MagicPacketService.updateCelestialDelayedActions(server, currentTick);
	}

	static boolean clearCapturedDomainState(UUID playerId) {
		return RuntimeResetService.clearCapturedDomainState(playerId);
	}

	static boolean isLovePowerActiveThisSecond(ServerPlayerEntity player) {
		return MagicPacketService.isLovePowerActiveThisSecond(player);
	}

	static void clearDeathAbilityState(ServerPlayerEntity player) {
		RuntimeResetService.clearDeathAbilityState(player);
	}

	static boolean isAttackInteraction(PlayerInteractEntityC2SPacket packet) {
		return RuntimeResetService.isAttackInteraction(packet);
	}

}


