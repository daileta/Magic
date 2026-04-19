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


abstract class MagicAbilityManagerManipulationTypes {
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

	static MagicAbilityManagerLoveDomainTypes.DomainClashState domainClashStateForParticipant(UUID playerId) {
		return MagicAbilityManagerSupportDomainClash.domainClashStateForParticipant(playerId);
	}

	static boolean isPositionInsideAnyDomain(RegistryKey<World> dimension, double x, double y, double z) {
		return MagicAbilityManagerSupportDomainExpansion.isPositionInsideAnyDomain(dimension, x, y, z);
	}

	static boolean isTestingMode(ServerPlayerEntity player) {
		return MagicAbilityManagerSupportJesterEffectsD.isTestingMode(player);
	}

	static int ignitionCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return MagicAbilityManagerSupportJesterEffectsD.ignitionCooldownRemaining(player, currentTick);
	}

	static int searingDashCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return MagicAbilityManagerSupportJesterEffectsD.searingDashCooldownRemaining(player, currentTick);
	}

	static int cinderMarkCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return MagicAbilityManagerSupportJesterEffectsD.cinderMarkCooldownRemaining(player, currentTick);
	}

	static int engineHeartCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return MagicAbilityManagerSupportJesterEffectsD.engineHeartCooldownRemaining(player, currentTick);
	}

	static int overrideCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return MagicAbilityManagerSupportJesterEffectsD.overrideCooldownRemaining(player, currentTick);
	}

	static int wittyOneLinerCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return MagicAbilityManagerSupportJesterEffectsD.wittyOneLinerCooldownRemaining(player, currentTick);
	}

	static int comedicRewriteCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return MagicAbilityManagerSupportJesterEffectsD.comedicRewriteCooldownRemaining(player, currentTick);
	}

	static int comedicAssistantCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return MagicAbilityManagerSupportJesterEffectsD.comedicAssistantCooldownRemaining(player, currentTick);
	}

	static int plusUltraCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		return MagicAbilityManagerSupportJesterEffectsD.plusUltraCooldownRemaining(player, currentTick);
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
		MagicAbilityManagerSupportDamageParticles.refreshStatusEffect(
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
		MagicAbilityManagerSupportDomainClash.runWithDomainTeleportBypass(action);
	}

	static void persistDomainRuntimeState(MinecraftServer server) {
		MagicAbilityManagerSupportPersistence.persistDomainRuntimeState(server);
	}

	static boolean isOrionsGambitCooldownSuppressed(ServerPlayerEntity player) {
		return MagicAbilityManagerSupportOrionAstral.isOrionsGambitCooldownSuppressed(player);
	}

	static void deactivateSpotlight(ServerPlayerEntity player, boolean disablePassive) {
		MagicAbilityManagerSupportJesterEffectsD.deactivateSpotlight(player, disablePassive);
	}

	static void deactivateCassiopeia(ServerPlayerEntity player, boolean sendEmptyOutline) {
		MagicAbilityManagerSupportCombatTargeting.deactivateCassiopeia(player, sendEmptyOutline);
	}

	static void startComedicRewriteCooldown(UUID playerId, int currentTick) {
		MagicAbilityManagerSupportJesterEffectsD.startComedicRewriteCooldown(playerId, currentTick);
	}

	static void startComedicAssistantCooldown(UUID playerId, int currentTick, int cooldownTicks) {
		MagicAbilityManagerSupportJesterEffectsD.startComedicAssistantCooldown(playerId, currentTick, cooldownTicks);
	}

	static void startPlusUltraCooldown(UUID playerId, int currentTick, int cooldownTicks) {
		MagicAbilityManagerSupportJesterEffectsD.startPlusUltraCooldown(playerId, currentTick, cooldownTicks);
	}

	static void debugManipulation(String message, Object... args) {
		MagicAbilityManagerSupportPackets.debugManipulation(message, args);
	}

	static boolean isMagicTargetableEntity(Entity entity) {
		return MagicAbilityManagerSupportCombatTargeting.isMagicTargetableEntity(entity);
	}

	static MagicAbility activeAbility(PlayerEntity player) {
		return MagicAbilityManagerSupportResets.activeAbility(player);
	}

	static void teleportDomainEntity(LivingEntity entity, double x, double y, double z, float yaw, float pitch) {
		MagicAbilityManagerSupportDomainClash.teleportDomainEntity(entity, x, y, z, yaw, pitch);
	}

	static boolean isFrostTeleportItemBlocked(PlayerEntity player, ItemStack stack) {
		return MagicAbilityManagerSupportFrostA.isFrostTeleportItemBlocked(player, stack);
	}

	static boolean isLoveItemUseBlocked(PlayerEntity player) {
		return MagicAbilityManagerSupportPackets.isLoveItemUseBlocked(player);
	}

	static boolean isDomainClashParticipantFrozen(PlayerEntity player) {
		return MagicAbilityManagerSupportPackets.isDomainClashParticipantFrozen(player);
	}

	static boolean isConstellationTeleportItemBlocked(ServerPlayerEntity player, ItemStack stack) {
		return MagicAbilityManagerSupportConstellationCoreB.isConstellationTeleportItemBlocked(player, stack);
	}

	static boolean isTeleportEscapeItem(ItemStack stack) {
		return MagicAbilityManagerSupportConstellationCoreB.isTeleportEscapeItem(stack);
	}

	static boolean isItemUseBlocked(PlayerEntity player) {
		return MagicAbilityManagerSupportPackets.isItemUseBlocked(player);
	}

	static boolean isAttackBlocked(PlayerEntity player) {
		return MagicAbilityManagerSupportPackets.isAttackBlocked(player);
	}

	static boolean isDirectCasterDamageBlocked(ServerPlayerEntity attacker, LivingEntity target, int currentTick) {
		return MagicAbilityManagerSupportPackets.isDirectCasterDamageBlocked(attacker, target, currentTick);
	}

	static void applyFrostStageHitEffects(ServerPlayerEntity caster, LivingEntity target, int currentTick) {
		MagicAbilityManagerSupportFrostB.applyFrostStageHitEffects(caster, target, currentTick);
	}

	static boolean isBurningPassionFireExtinguished(LivingEntity target, boolean extinguishInWater, boolean extinguishInRain) {
		return MagicAbilityManagerSupportBurningA.isBurningPassionFireExtinguished(target, extinguishInWater, extinguishInRain);
	}

	static void dealBurningPassionFireDamage(
		LivingEntity target,
		UUID casterId,
		ServerWorld world,
		float damagePerTick,
		double fireResistantTargetDamageMultiplier,
		boolean fireDamageIgnoresFireResistance
	) {
		MagicAbilityManagerSupportBurningB.dealBurningPassionFireDamage(
			target,
			casterId,
			world,
			damagePerTick,
			fireResistantTargetDamageMultiplier,
			fireDamageIgnoresFireResistance
		);
	}

	static void setActiveAbility(ServerPlayerEntity player, MagicAbility ability) {
		MagicAbilityManagerSupportResets.setActiveAbility(player, ability);
	}

	static void spawnHerculesImpactBurst(LivingEntity target) {
		MagicAbilityManagerSupportOrionAstral.spawnHerculesImpactBurst(target);
	}

	static void spawnHerculesBurdenParticles(LivingEntity target) {
		MagicAbilityManagerSupportOrionAstral.spawnHerculesBurdenParticles(target);
	}

	static DamageSource createTrueMagicDamageSource(ServerWorld world, Entity attacker) {
		return MagicAbilityManagerSupportConstellationCoreA.createTrueMagicDamageSource(world, attacker);
	}

	static DamageSource createTrueMagicDamageSource(ServerWorld world) {
		return MagicAbilityManagerSupportConstellationCoreA.createTrueMagicDamageSource(world);
	}

	static DamageSource createTrueMagicDamageSource(MinecraftServer server, ServerWorld world, UUID casterId) {
		return MagicAbilityManagerSupportConstellationCoreA.createTrueMagicDamageSource(server, world, casterId);
	}

	static void releaseCelestialAlignmentState(
		MinecraftServer server,
		MagicAbilityManagerConstellationTypes.CelestialAlignmentState state,
		boolean expired
	) {
		MagicAbilityManagerSupportConstellationCoreA.releaseCelestialAlignmentState(server, state, expired);
	}

	static void clearCelestialGamaRayState(ServerPlayerEntity caster, boolean sendFeedback, boolean startCooldown) {
		MagicAbilityManagerSupportConstellationCoreA.clearCelestialGamaRayState(caster, sendFeedback, startCooldown);
	}

	static void clearCelestialGamaRayState(ServerPlayerEntity caster, boolean sendFeedback, boolean startCooldown, int cooldownTicks) {
		MagicAbilityManagerSupportConstellationCoreA.clearCelestialGamaRayState(caster, sendFeedback, startCooldown, cooldownTicks);
	}

	static void updateCelestialAlignmentState(
		MinecraftServer server,
		ServerPlayerEntity caster,
		MagicAbilityManagerConstellationTypes.CelestialAlignmentState state,
		int currentTick
	) {
		MagicAbilityManagerSupportConstellationCoreA.updateCelestialAlignmentState(server, caster, state, currentTick);
	}

	static void updateCelestialGeminiReplays(MinecraftServer server, int currentTick) {
		MagicAbilityManagerSupportConstellationCoreB.updateCelestialGeminiReplays(server, currentTick);
	}

	static void updateCelestialGamaRayStates(MinecraftServer server, int currentTick) {
		MagicAbilityManagerSupportConstellationCoreA.updateCelestialGamaRayStates(server, currentTick);
	}

	static int celestialAlignmentColor(MagicAbilityManagerConstellationTypes.CelestialAlignmentConstellation constellation) {
		return MagicAbilityManagerSupportConstellationCoreB.celestialAlignmentColor(constellation);
	}

	static void consumeFullManaBar(ServerPlayerEntity player) {
		MagicAbilityManagerSupportJesterEffectsD.consumeFullManaBar(player);
	}

	static double manaFromPercentExact(double percent) {
		return MagicAbilityManagerSupportJesterEffectsC.manaFromPercentExact(percent);
	}

	static void spendAbilityCost(ServerPlayerEntity player, int manaCost) {
		MagicAbilityManagerSupportJesterEffectsD.spendAbilityCost(player, manaCost);
	}

	static void refreshCelestialMovementSpeedModifier(LivingEntity entity) {
		MagicAbilityManagerSupportConstellationCoreB.refreshCelestialMovementSpeedModifier(entity);
	}

	static void updateSagittaConstellation(
		ServerWorld world,
		ServerPlayerEntity caster,
		MagicAbilityManagerConstellationTypes.CelestialAlignmentState state,
		int currentTick
	) {
		MagicAbilityManagerSupportConstellationCoreB.updateSagittaConstellation(world, caster, state, currentTick);
	}

	static void updateAquilaConstellation(
		ServerWorld world,
		ServerPlayerEntity caster,
		MagicAbilityManagerConstellationTypes.CelestialAlignmentState state,
		int currentTick
	) {
		MagicAbilityManagerSupportConstellationCoreB.updateAquilaConstellation(world, caster, state, currentTick);
	}

	static void spawnCraterSiphonEffects(ServerWorld world, MagicAbilityManagerConstellationTypes.CelestialAlignmentState state) {
		MagicAbilityManagerSupportConstellationCoreB.spawnCraterSiphonEffects(world, state);
	}

	static void releaseGeminiRecordedDamage(
		MinecraftServer server,
		ServerWorld world,
		MagicAbilityManagerConstellationTypes.CelestialAlignmentState state
	) {
		MagicAbilityManagerSupportConstellationCoreB.releaseGeminiRecordedDamage(server, world, state);
	}

	static void clearCelestialDelayQueueIfUnaffected(
		MinecraftServer server,
		UUID playerId,
		MagicAbilityManagerConstellationTypes.CelestialAlignmentState ignoredState
	) {
		MagicAbilityManagerSupportPackets.clearCelestialDelayQueueIfUnaffected(server, playerId, ignoredState);
	}

	static MagicAbilityManagerConstellationTypes.CelestialPattern celestialPattern(
		MagicAbilityManagerConstellationTypes.CelestialAlignmentConstellation constellation
	) {
		return MagicAbilityManagerSupportConstellationCoreB.celestialPattern(constellation);
	}

	static void spawnCelestialAmbientEffects(ServerWorld world, MagicAbilityManagerConstellationTypes.CelestialAlignmentState state) {
		MagicAbilityManagerSupportConstellationCoreB.spawnCelestialAmbientEffects(world, state);
	}

	static String celestialAlignmentColorHex(MagicAbilityManagerConstellationTypes.CelestialAlignmentConstellation constellation) {
		return MagicAbilityManagerSupportConstellationCoreB.celestialAlignmentColorHex(constellation);
	}

	static void removeAttributeModifier(EntityAttributeInstance attributeInstance, Identifier modifierId) {
		MagicAbilityManagerSupportJesterEffectsD.removeAttributeModifier(attributeInstance, modifierId);
	}

	static ServerPlayerEntity attackingPlayerFrom(DamageSource source) {
		return MagicAbilityManagerSupportJesterEffectsC.attackingPlayerFrom(source);
	}

	static int resetAllCooldowns(ServerPlayerEntity player) {
		return MagicAbilityManagerSupportResets.resetAllCooldowns(player);
	}

	static void setSignedAttributeModifier(
		EntityAttributeInstance attributeInstance,
		Identifier modifierId,
		double value,
		EntityAttributeModifier.Operation operation
	) {
		MagicAbilityManagerSupportJesterEffectsD.setSignedAttributeModifier(attributeInstance, modifierId, value, operation);
	}

	static boolean deactivateDomainExpansion(ServerPlayerEntity player) {
		return MagicAbilityManagerSupportDomainExpansion.deactivateDomainExpansion(player);
	}

	static boolean isBeaconCoreProtected(ServerPlayerEntity player) {
		return MagicAbilityManagerSupportDomainClash.isBeaconCoreProtected(player);
	}

	static void activateManipulation(ServerPlayerEntity caster, ServerPlayerEntity target, int currentTick) {
		MagicAbilityManagerSupportLoveManipulation.activateManipulation(caster, target, currentTick);
	}

	static void tryAcquireManipulationTarget(ServerPlayerEntity caster, int currentTick) {
		MagicAbilityManagerSupportLoveManipulation.tryAcquireManipulationTarget(caster, currentTick);
	}

	static void deactivateManipulation(ServerPlayerEntity caster, boolean startCooldown) {
		MagicAbilityManagerSupportResets.deactivateManipulation(caster, startCooldown);
	}

	static void deactivateManipulation(ServerPlayerEntity caster, boolean startCooldown, String reason) {
		MagicAbilityManagerSupportResets.deactivateManipulation(caster, startCooldown, reason);
	}

	static void releaseManipulationState(
		UUID casterId,
		ManipulationState state,
		MinecraftServer server,
		ServerPlayerEntity caster
	) {
		MagicAbilityManagerSupportResets.releaseManipulationState(casterId, state, server, caster);
	}

	static boolean isMagicSuppressed(PlayerEntity player) {
		return MagicAbilityManagerSupportPackets.isMagicSuppressed(player);
	}

	static Vec3d normalizedHorizontalDirection(Vec3d direction, ServerPlayerEntity player) {
		return MagicAbilityManagerSupportJesterEffectsC.normalizedHorizontalDirection(direction, player);
	}

	static float horizontalYawFromDirection(Vec3d direction) {
		return MagicAbilityManagerSupportJesterEffectsC.horizontalYawFromDirection(direction);
	}

	static List<ServerPlayerEntity> comedicRewriteVisualViewers(ServerWorld world, Vec3d origin) {
		return MagicAbilityManagerSupportJesterEffectsB.comedicRewriteVisualViewers(world, origin);
	}

	static List<ServerPlayerEntity> comedicRewriteVisualViewers(MinecraftServer server, List<UUID> viewerIds) {
		return MagicAbilityManagerSupportJesterEffectsB.comedicRewriteVisualViewers(server, viewerIds);
	}

	static void queueComedicRewriteVisualCameo(
		MinecraftServer server,
		int durationTicks,
		List<ServerPlayerEntity> viewers,
		int chargeDurationTicks,
		MagicAbilityManagerJesterTypes.ComedicRewriteVisualFollowState followState,
		Entity... entities
	) {
		MagicAbilityManagerSupportJesterEffectsB.queueComedicRewriteVisualCameo(
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
		MagicAbilityManagerJesterTypes.ComedicRewriteVisualFollowState followState,
		MagicAbilityManagerJesterTypes.ComedicRewriteVisualRotationState rotationState,
		Entity... entities
	) {
		MagicAbilityManagerSupportJesterEffectsB.queueComedicRewriteVisualCameo(
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
		return MagicAbilityManagerSupportJesterEffectsD.shouldKeepComedicAssistantEnabled(player);
	}

	static boolean hasActivePlusUltra(ServerPlayerEntity player) {
		return MagicAbilityManagerSupportJesterRequests.hasActivePlusUltra(player);
	}

	static boolean shouldKeepPlusUltraEnabled(ServerPlayerEntity player) {
		return MagicAbilityManagerSupportJesterRequests.shouldKeepPlusUltraEnabled(player);
	}

	static void addConfiguredStatusEffect(
		LivingEntity target,
		RegistryEntry<StatusEffect> effect,
		int durationTicks,
		int amplifier
	) {
		MagicAbilityManagerSupportJesterEffectsD.addConfiguredStatusEffect(target, effect, durationTicks, amplifier);
	}

	static void spawnComedicAssistantSlimeVisual(LivingEntity target, ServerWorld world) {
		MagicAbilityManagerSupportJesterEffectsB.spawnComedicAssistantSlimeVisual(target, world);
	}

	static void spawnComedicAssistantPandaVisual(LivingEntity target, ServerWorld world, Vec3d direction) {
		MagicAbilityManagerSupportJesterEffectsB.spawnComedicAssistantPandaVisual(target, world, direction);
	}

	static void spawnComedicAssistantParrotVisual(LivingEntity target, ServerWorld world) {
		MagicAbilityManagerSupportJesterEffectsB.spawnComedicAssistantParrotVisual(target, world);
	}

	static double resolveComedicAssistantCarryTargetY(LivingEntity target, ServerWorld world, double liftHeight) {
		return MagicAbilityManagerSupportJesterEffectsB.resolveComedicAssistantCarryTargetY(target, world, liftHeight);
	}

	static void spawnComedicAssistantDivineLightningVisual(LivingEntity target, ServerWorld world, int strikeCount) {
		MagicAbilityManagerSupportJesterEffectsB.spawnComedicAssistantDivineLightningVisual(target, world, strikeCount);
	}

	static void spawnComedicAssistantAcmeVisual(LivingEntity target, ServerWorld world) {
		MagicAbilityManagerSupportJesterEffectsB.spawnComedicAssistantAcmeVisual(target, world);
	}

	static void spawnComedicAssistantCaneVisual(LivingEntity target, ServerWorld world, Vec3d direction) {
		MagicAbilityManagerSupportJesterEffectsB.spawnComedicAssistantCaneVisual(target, world, direction);
	}

	static void teleportComedicRewritePlayer(ServerPlayerEntity player, ServerWorld world, Vec3d targetPos) {
		MagicAbilityManagerSupportJesterEffectsC.teleportComedicRewritePlayer(player, world, targetPos);
	}

	static void deactivateLoveAtFirstSight(ServerPlayerEntity player) {
		MagicAbilityManagerSupportResets.deactivateLoveAtFirstSight(player);
	}

	static void clearAllRuntimeState(ServerPlayerEntity player) {
		MagicAbilityManagerSupportResets.clearAllRuntimeState(player);
	}

	static MagicAbilityManagerBurningFrostTypes.BurningPassionIgnitionState ensureAdminBurningPassionIgnitionState(
		ServerPlayerEntity player,
		int currentTick
	) {
		return MagicAbilityManagerSupportFrostA.ensureAdminBurningPassionIgnitionState(player, currentTick);
	}

	static void syncBurningPassionHud(ServerPlayerEntity player) {
		MagicAbilityManagerSupportBurningC.syncBurningPassionHud(player);
	}

	static int burningPassionStageDurationTicks(int stage) {
		return MagicAbilityManagerSupportBurningC.burningPassionStageDurationTicks(stage);
	}

	static Vec3d findFrostGroundAnchor(ServerWorld world, Vec3d samplePoint, int searchUpBlocks, int searchDownBlocks) {
		return MagicAbilityManagerSupportFrostA.findFrostGroundAnchor(world, samplePoint, searchUpBlocks, searchDownBlocks);
	}

	static void applyIgnition(ServerPlayerEntity player, int currentTick) {
		MagicAbilityManagerSupportBurningC.applyIgnition(player, currentTick);
	}

	static boolean tryStartOverride(ServerPlayerEntity player, int currentTick, boolean bypassDomainRequirement) {
		return MagicAbilityManagerSupportBurningC.tryStartOverride(player, currentTick, bypassDomainRequirement);
	}

	static MagicAbilityManagerBurningFrostTypes.FrostStageState ensureAdminFrostStageState(ServerPlayerEntity player) {
		return MagicAbilityManagerSupportFrostA.ensureAdminFrostStageState(player);
	}

	static void syncFrostStageHud(ServerPlayerEntity player) {
		MagicAbilityManagerSupportFrostA.syncFrostStageHud(player);
	}

	static void endFrostStagedMode(
		ServerPlayerEntity player,
		int currentTick,
		MagicAbilityManagerBurningFrostTypes.FrostStageEndReason reason,
		boolean startCooldown,
		boolean sendDeactivatedMessage
	) {
		MagicAbilityManagerSupportFrostB.endFrostStagedMode(player, currentTick, reason, startCooldown, sendDeactivatedMessage);
	}

	static int frostProgressRequirementTicks(int currentStage) {
		return MagicAbilityManagerSupportFrostB.frostProgressRequirementTicks(currentStage);
	}

	static void discardBurningPassionCinderMarkDisplay(
		ServerWorld world,
		MagicAbilityManagerBurningFrostTypes.BurningPassionCinderMarkState state
	) {
		MagicAbilityManagerSupportBurningB.discardBurningPassionCinderMarkDisplay(world, state);
	}

	static MagicAbilityManagerBurningFrostTypes.BurningPassionIgnitionState burningPassionIgnitionState(ServerPlayerEntity player) {
		return MagicAbilityManagerSupportBurningC.burningPassionIgnitionState(player);
	}

	static void showBurningPassionHudNotification(ServerPlayerEntity player, String text, int currentTick, int durationTicks) {
		MagicAbilityManagerSupportBurningC.showBurningPassionHudNotification(player, text, currentTick, durationTicks);
	}

	static void endOwnedDomain(
		UUID ownerId,
		MagicAbilityManagerLoveDomainTypes.DomainExpansionState state,
		MinecraftServer server,
		int currentTick
	) {
		MagicAbilityManagerSupportDomainExpansion.endOwnedDomain(ownerId, state, server, currentTick);
	}

	static boolean isInsideDomainInterior(
		ServerPlayerEntity player,
		MagicAbilityManagerLoveDomainTypes.DomainExpansionState state
	) {
		return MagicAbilityManagerSupportDomainExpansion.isInsideDomainInterior(player, state);
	}

	static boolean isInsideDomainInterior(double horizontalDistanceSq, double y, int innerRadius, int innerHeight) {
		return MagicAbilityManagerSupportDomainClash.isInsideDomainInterior(horizontalDistanceSq, y, innerRadius, innerHeight);
	}

	static double clampBurningPassionHeat(double heatPercent) {
		return MagicAbilityManagerSupportBurningC.clampBurningPassionHeat(heatPercent);
	}

	static boolean canPayBurningPassionHealthDemand(
		ServerPlayerEntity player,
		MagicAbilityManagerBurningFrostTypes.BurningPassionIgnitionState ignitionState,
		MagicAbility ability
	) {
		return MagicAbilityManagerSupportBurningC.canPayBurningPassionHealthDemand(player, ignitionState, ability);
	}

	static boolean tryPayBurningPassionManaCost(
		ServerPlayerEntity player,
		MagicAbilityManagerBurningFrostTypes.BurningPassionIgnitionState ignitionState,
		MagicAbility ability,
		double manaCostPercent
	) {
		return MagicAbilityManagerSupportBurningC.tryPayBurningPassionManaCost(player, ignitionState, ability, manaCostPercent);
	}

	static void payBurningPassionHealthDemand(
		ServerPlayerEntity player,
		MagicAbilityManagerBurningFrostTypes.BurningPassionIgnitionState ignitionState,
		MagicAbility ability
	) {
		MagicAbilityManagerSupportBurningC.payBurningPassionHealthDemand(player, ignitionState, ability);
	}

	static void deactivateMartyrsFlame(ServerPlayerEntity player, boolean startCooldown) {
		MagicAbilityManagerSupportResets.deactivateMartyrsFlame(player, startCooldown);
	}

	static boolean isLockedBurningPassionStage(ServerPlayerEntity player, int stage) {
		return MagicAbilityManagerSupportResets.isLockedBurningPassionStage(player, stage);
	}

	static void removeStatusEffectIfMatching(
		LivingEntity entity,
		RegistryEntry<StatusEffect> effect,
		Set<Integer> allowedAmplifiers
	) {
		MagicAbilityManagerSupportFrostA.removeStatusEffectIfMatching(entity, effect, allowedAmplifiers);
	}

	static void clearFrostMaximumState(ServerPlayerEntity player, boolean clearCooldown) {
		MagicAbilityManagerSupportFrostB.clearFrostMaximumState(player, clearCooldown);
	}

	static void removeFrostStageCasterBuffs(ServerPlayerEntity player) {
		MagicAbilityManagerSupportFrostB.removeFrostStageCasterBuffs(player);
	}

	static boolean isLockedFrostStage(ServerPlayerEntity player, int stage) {
		return MagicAbilityManagerSupportResets.isLockedFrostStage(player, stage);
	}

	static void deactivateAbsoluteZero(ServerPlayerEntity player) {
		MagicAbilityManagerSupportResets.deactivateAbsoluteZero(player);
	}

	static void deactivatePlanckHeat(ServerPlayerEntity player) {
		MagicAbilityManagerSupportResets.deactivatePlanckHeat(player);
	}

	static void deactivateTillDeathDoUsPart(
		ServerPlayerEntity player,
		MagicAbilityManagerLoveDomainTypes.TillDeathDoUsPartEndReason reason,
		boolean sendFeedback
	) {
		MagicAbilityManagerSupportResets.deactivateTillDeathDoUsPart(player, reason, sendFeedback);
	}

	static void startTillDeathDoUsPartCooldown(UUID playerId, int currentTick) {
		MagicAbilityManagerSupportResets.startTillDeathDoUsPartCooldown(playerId, currentTick);
	}

	static void endPlusUltra(
		ServerPlayerEntity player,
		int currentTick,
		MagicAbilityManagerConstellationTypes.PlusUltraEndMode endMode,
		boolean sendFeedback
	) {
		MagicAbilityManagerSupportJesterRequests.endPlusUltra(player, currentTick, endMode, sendFeedback);
	}

	static void applyPlusUltra(ServerPlayerEntity player, int currentTick) {
		MagicAbilityManagerSupportJesterRequests.applyPlusUltra(player, currentTick);
	}

	static Map<UUID, MagicAbilityManagerLoveDomainTypes.DomainCapturedEntityState> captureDomainEntities(
		ServerWorld world,
		double centerX,
		double centerZ,
		int baseY,
		int radius,
		int height
	) {
		return MagicAbilityManagerSupportDomainClash.captureDomainEntities(world, centerX, centerZ, baseY, radius, height);
	}

	static boolean shouldPreserveBeaconAnchor(BlockState state) {
		return MagicAbilityManagerSupportDomainClash.shouldPreserveBeaconAnchor(state);
	}

	static void moveCapturedEntitiesIntoDomain(
		ServerWorld world,
		Iterable<MagicAbilityManagerLoveDomainTypes.DomainCapturedEntityState> capturedEntities,
		double centerX,
		double centerZ,
		int baseY,
		int innerRadius,
		int innerHeight
	) {
		MagicAbilityManagerSupportDomainClash.moveCapturedEntitiesIntoDomain(
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
		MagicAbilityManagerSupportDomainClash.cancelDomainClash(ownerId, server);
	}

	static void restoreCapturedEntities(MinecraftServer server, MagicAbilityManagerLoveDomainTypes.DomainExpansionState state) {
		MagicAbilityManagerSupportDomainClash.restoreCapturedEntities(server, state);
	}

	static void enforceCapturedEntitiesInsideDomain(ServerWorld world, MagicAbilityManagerLoveDomainTypes.DomainExpansionState state) {
		MagicAbilityManagerSupportDomainClash.enforceCapturedEntitiesInsideDomain(world, state);
	}

	static void maintainDomainShell(ServerWorld world, MagicAbilityManagerLoveDomainTypes.DomainExpansionState state) {
		MagicAbilityManagerSupportDomainClash.maintainDomainShell(world, state);
	}

	static int domainClashTitleDurationTicks() {
		return MagicAbilityManagerSupportDomainClash.domainClashTitleDurationTicks();
	}

	static int domainClashIntroLockTicks() {
		return MagicAbilityManagerSupportDomainClash.domainClashIntroLockTicks();
	}

	static void applySplitDomainInteriorVisuals(
		ServerWorld world,
		MagicAbilityManagerLoveDomainTypes.DomainExpansionState state,
		MagicAbility ownerAbility,
		MagicAbility challengerAbility
	) {
		MagicAbilityManagerSupportDomainClash.applySplitDomainInteriorVisuals(world, state, ownerAbility, challengerAbility);
	}

	static Vec3d resolveGreedDomainClashLockedPosition(
		ServerWorld world,
		MagicAbilityManagerLoveDomainTypes.DomainExpansionState state,
		LivingEntity participant,
		Vec3d preferredPos
	) {
		return MagicAbilityManagerSupportDomainClash.resolveGreedDomainClashLockedPosition(world, state, participant, preferredPos);
	}

	static void showDomainClashTitle(ServerPlayerEntity player) {
		MagicAbilityManagerSupportDomainClash.showDomainClashTitle(player);
	}

	static void lockDomainClashParticipant(ServerPlayerEntity player, Vec3d lockedPos, Vec3d opponentEyePos) {
		MagicAbilityManagerSupportDomainClash.lockDomainClashParticipant(player, lockedPos, opponentEyePos);
	}

	static void clearDomainClashUiFor(
		MagicAbilityManagerLoveDomainTypes.DomainClashState clash,
		MinecraftServer server
	) {
		MagicAbilityManagerSupportDomainClash.clearDomainClashUiFor(clash, server);
	}

	static boolean isDomainClashFrozen(MagicAbilityManagerLoveDomainTypes.DomainClashState clash, int currentTick) {
		return MagicAbilityManagerSupportDomainClash.isDomainClashFrozen(clash, currentTick);
	}

	static void spawnDomainClashParticles(
		ServerWorld world,
		MagicAbilityManagerLoveDomainTypes.DomainExpansionState state,
		int particlesPerTick
	) {
		MagicAbilityManagerSupportDomainClash.spawnDomainClashParticles(world, state, particlesPerTick);
	}

	static int domainClashProgressPercent(double damageDealt) {
		return MagicAbilityManagerSupportDomainClash.domainClashProgressPercent(damageDealt);
	}

	static int domainClashInstructionVisibilityPercent(
		MagicAbilityManagerLoveDomainTypes.DomainClashState clash,
		int currentTick
	) {
		return MagicAbilityManagerSupportDomainClash.domainClashInstructionVisibilityPercent(clash, currentTick);
	}

	static void resolveDomainClash(MinecraftServer server, UUID ownerId, UUID winnerId, int currentTick) {
		MagicAbilityManagerSupportDomainClash.resolveDomainClash(server, ownerId, winnerId, currentTick);
	}

	static void updateCelestialDelayedActions(MinecraftServer server, int currentTick) {
		MagicAbilityManagerSupportPackets.updateCelestialDelayedActions(server, currentTick);
	}

	static boolean clearCapturedDomainState(UUID playerId) {
		return MagicAbilityManagerSupportResets.clearCapturedDomainState(playerId);
	}

	static boolean isLovePowerActiveThisSecond(ServerPlayerEntity player) {
		return MagicAbilityManagerSupportPackets.isLovePowerActiveThisSecond(player);
	}

	static void clearDeathAbilityState(ServerPlayerEntity player) {
		MagicAbilityManagerSupportResets.clearDeathAbilityState(player);
	}

	static boolean isAttackInteraction(PlayerInteractEntityC2SPacket packet) {
		return MagicAbilityManagerSupportResets.isAttackInteraction(packet);
	}

}

