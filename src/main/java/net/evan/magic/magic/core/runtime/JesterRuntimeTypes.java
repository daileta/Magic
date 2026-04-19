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


abstract class JesterRuntimeTypes extends MagicAbilityCoreSupport {
	static final class SpotlightState {
		final Map<UUID, Integer> viewerLastLookTicks = new HashMap<>();
		int currentStage;
		int downgradeDeadlineTick = -1;
		double appliedMaxHealthBonusPoints;

		Map<UUID, Integer> viewerLastLookTicks() {
			return viewerLastLookTicks;
		}

		int currentStage() {
			return currentStage;
		}

		void currentStage(int currentStage) {
			this.currentStage = Math.max(0, currentStage);
		}

		int downgradeDeadlineTick() {
			return downgradeDeadlineTick;
		}

		void downgradeDeadlineTick(int downgradeDeadlineTick) {
			this.downgradeDeadlineTick = downgradeDeadlineTick;
		}

		double appliedMaxHealthBonusPoints() {
			return appliedMaxHealthBonusPoints;
		}

		void appliedMaxHealthBonusPoints(double appliedMaxHealthBonusPoints) {
			this.appliedMaxHealthBonusPoints = Math.max(0.0, appliedMaxHealthBonusPoints);
		}
	}

	record SpotlightStageSettings(
		int viewersRequired,
		int activationWindowTicks,
		int downgradeGraceTicks,
		int fallbackWindowTicks,
		double attackDamageBonus,
		double movementSpeedMultiplier,
		double scale,
		int jumpBoostAmplifier,
		int resistanceAmplifier,
		double maxHealthBonusHearts
	) {
		static SpotlightStageSettings defaults(
			int viewersRequired,
			int activationWindowTicks,
			int downgradeGraceTicks,
			int fallbackWindowTicks,
			double attackDamageBonus,
			double movementSpeedMultiplier,
			double scale,
			int jumpBoostAmplifier,
			int resistanceAmplifier,
			double maxHealthBonusHearts
		) {
			return new SpotlightStageSettings(
				viewersRequired,
				activationWindowTicks,
				downgradeGraceTicks,
				fallbackWindowTicks,
				attackDamageBonus,
				movementSpeedMultiplier,
				scale,
				jumpBoostAmplifier,
				resistanceAmplifier,
				maxHealthBonusHearts
			);
		}

		int maxRelevantWindowTicks() {
			return Math.max(activationWindowTicks, fallbackWindowTicks);
		}

		double maxHealthBonusPoints() {
			return maxHealthBonusHearts * 2.0;
		}
	}

	record WittyOneLinerTierSettings(
		int selectionWeight,
		int cooldownTicks,
		int effectDurationTicks,
		int colorRgb,
		int slownessAmplifier,
		int weaknessAmplifier,
		int blindnessAmplifier,
		int nauseaAmplifier,
		int darknessAmplifier,
		int weavingAmplifier,
		boolean applyGlowing,
		List<String> jokes
	) {
		static WittyOneLinerTierSettings defaults(
			int selectionWeight,
			int cooldownTicks,
			int effectDurationTicks,
			int colorRgb,
			int slownessAmplifier,
			int weaknessAmplifier,
			int blindnessAmplifier,
			int nauseaAmplifier,
			int darknessAmplifier,
			int weavingAmplifier,
			boolean applyGlowing,
			List<String> jokes
		) {
			return new WittyOneLinerTierSettings(
				selectionWeight,
				cooldownTicks,
				effectDurationTicks,
				colorRgb,
				slownessAmplifier,
				weaknessAmplifier,
				blindnessAmplifier,
				nauseaAmplifier,
				darknessAmplifier,
				weavingAmplifier,
				applyGlowing,
				jokes
			);
		}
	}

	enum ComedicAssistantOutcome {
		GIANT_SLIME_SLAM("giant_slime_slam"),
		PANDA_BOWLING_BALL("panda_bowling_ball"),
		PARROT_KIDNAPPING("parrot_kidnapping"),
		DIVINE_OVERREACTION("divine_overreaction"),
		ACME_DROP("acme_drop"),
		PIE_TO_THE_FACE("pie_to_the_face"),
		GIANT_CANE_YANK("giant_cane_yank");

		final String id;

		ComedicAssistantOutcome(String id) {
			this.id = id;
		}

		Text displayName() {
			return Text.translatable("magic.jester_gag." + id);
		}
	}

	record ComedicAssistantArmedState(
		RegistryKey<World> dimension,
		int expiresTick,
		int nextIndicatorTick
	) {
	}

	static final class ComedicAssistantParrotCarryState {
		final RegistryKey<World> dimension;
		final double targetY;
		final int endTick;
		int nextParticleTick;
		int nextSoundTick;
		final ComedicAssistantParrotSettings settings;

		ComedicAssistantParrotCarryState(
			RegistryKey<World> dimension,
			double targetY,
			int endTick,
			int nextParticleTick,
			int nextSoundTick,
			ComedicAssistantParrotSettings settings
		) {
			this.dimension = dimension;
			this.targetY = targetY;
			this.endTick = endTick;
			this.nextParticleTick = nextParticleTick;
			this.nextSoundTick = nextSoundTick;
			this.settings = settings;
		}

		RegistryKey<World> dimension() {
			return dimension;
		}

		double targetY() {
			return targetY;
		}

		int endTick() {
			return endTick;
		}

		int nextParticleTick() {
			return nextParticleTick;
		}

		void nextParticleTick(int nextParticleTick) {
			this.nextParticleTick = nextParticleTick;
		}

		int nextSoundTick() {
			return nextSoundTick;
		}

		void nextSoundTick(int nextSoundTick) {
			this.nextSoundTick = nextSoundTick;
		}

		ComedicAssistantParrotSettings settings() {
			return settings;
		}
	}

	record ComedicAssistantAcmeVisualState(
		RegistryKey<World> dimension,
		UUID visualEntityId,
		UUID targetEntityId,
		int endTick,
		double dropSpeed
	) {
	}

	static final class ComedicAssistantCaneImpactState {
		final RegistryKey<World> dimension;
		final UUID casterId;
		final int startTick;
		final int endTick;
		final int forceEndTick;
		final Vec3d forcedVelocity;
		Vec3d lastPosition;
		Vec3d lastVelocity;
		double lastSpeed;

		ComedicAssistantCaneImpactState(
			RegistryKey<World> dimension,
			UUID casterId,
			int startTick,
			int endTick,
			int forceEndTick,
			Vec3d forcedVelocity,
			Vec3d lastPosition,
			Vec3d lastVelocity,
			double lastSpeed
		) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.startTick = startTick;
			this.endTick = endTick;
			this.forceEndTick = forceEndTick;
			this.forcedVelocity = forcedVelocity;
			this.lastPosition = lastPosition;
			this.lastVelocity = lastVelocity;
			this.lastSpeed = Math.max(0.0, lastSpeed);
		}

		RegistryKey<World> dimension() {
			return dimension;
		}

		UUID casterId() {
			return casterId;
		}

		int startTick() {
			return startTick;
		}

		int endTick() {
			return endTick;
		}

		int forceEndTick() {
			return forceEndTick;
		}

		Vec3d forcedVelocity() {
			return forcedVelocity;
		}

		Vec3d lastPosition() {
			return lastPosition;
		}

		void lastPosition(Vec3d lastPosition) {
			this.lastPosition = lastPosition;
		}

		Vec3d lastVelocity() {
			return lastVelocity;
		}

		void lastVelocity(Vec3d lastVelocity) {
			this.lastVelocity = lastVelocity;
		}

		double lastSpeed() {
			return lastSpeed;
		}

		void lastSpeed(double lastSpeed) {
			this.lastSpeed = Math.max(0.0, lastSpeed);
		}
	}

	record ComedicAssistantSlimeSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		int slownessDurationTicks,
		int slownessAmplifier,
		int oozingDurationTicks,
		int oozingAmplifier,
		int visualDurationTicks,
		double visualSpawnHeight,
		double visualFallSpeed,
		int visualSize,
		int particleCount,
		float spawnSoundVolume,
		float spawnSoundPitch,
		float impactSoundVolume,
		float impactSoundPitch
	) {
		static ComedicAssistantSlimeSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			int slownessDurationTicks,
			int slownessAmplifier,
			int oozingDurationTicks,
			int oozingAmplifier,
			int visualDurationTicks,
			double visualSpawnHeight,
			double visualFallSpeed,
			int visualSize,
			int particleCount,
			float spawnSoundVolume,
			float spawnSoundPitch,
			float impactSoundVolume,
			float impactSoundPitch
		) {
			return new ComedicAssistantSlimeSettings(
				enabled,
				weight,
				bonusDamage,
				slownessDurationTicks,
				slownessAmplifier,
				oozingDurationTicks,
				oozingAmplifier,
				visualDurationTicks,
				visualSpawnHeight,
				visualFallSpeed,
				visualSize,
				particleCount,
				spawnSoundVolume,
				spawnSoundPitch,
				impactSoundVolume,
				impactSoundPitch
			);
		}
	}

	record ComedicAssistantPandaSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		double horizontalLaunch,
		double verticalLaunch,
		int slownessDurationTicks,
		int slownessAmplifier,
		int visualDurationTicks,
		double visualSpawnDistance,
		double visualChargeVelocity,
		int particleCount,
		float rollSoundVolume,
		float rollSoundPitch,
		float impactSoundVolume,
		float impactSoundPitch
	) {
		static ComedicAssistantPandaSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			double horizontalLaunch,
			double verticalLaunch,
			int slownessDurationTicks,
			int slownessAmplifier,
			int visualDurationTicks,
			double visualSpawnDistance,
			double visualChargeVelocity,
			int particleCount,
			float rollSoundVolume,
			float rollSoundPitch,
			float impactSoundVolume,
			float impactSoundPitch
		) {
			return new ComedicAssistantPandaSettings(
				enabled,
				weight,
				bonusDamage,
				horizontalLaunch,
				verticalLaunch,
				slownessDurationTicks,
				slownessAmplifier,
				visualDurationTicks,
				visualSpawnDistance,
				visualChargeVelocity,
				particleCount,
				rollSoundVolume,
				rollSoundPitch,
				impactSoundVolume,
				impactSoundPitch
			);
		}
	}

	record ComedicAssistantParrotSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		double liftHeight,
		double upwardVelocity,
		int maxCarryTicks,
		double releaseDownwardVelocity,
		boolean applyGlowing,
		int glowingDurationTicks,
		int visualDurationTicks,
		int visualCount,
		double visualRadius,
		double visualVerticalOffset,
		int particleCount,
		int flapSoundIntervalTicks,
		float soundVolume,
		float soundPitch
	) {
		static ComedicAssistantParrotSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			double liftHeight,
			double upwardVelocity,
			int maxCarryTicks,
			double releaseDownwardVelocity,
			boolean applyGlowing,
			int glowingDurationTicks,
			int visualDurationTicks,
			int visualCount,
			double visualRadius,
			double visualVerticalOffset,
			int particleCount,
			int flapSoundIntervalTicks,
			float soundVolume,
			float soundPitch
		) {
			return new ComedicAssistantParrotSettings(
				enabled,
				weight,
				bonusDamage,
				liftHeight,
				upwardVelocity,
				maxCarryTicks,
				releaseDownwardVelocity,
				applyGlowing,
				glowingDurationTicks,
				visualDurationTicks,
				visualCount,
				visualRadius,
				visualVerticalOffset,
				particleCount,
				flapSoundIntervalTicks,
				soundVolume,
				soundPitch
			);
		}
	}

	record ComedicAssistantDivineSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		int strikeCount,
		double strikeRadius,
		int glowingDurationTicks,
		int blindnessDurationTicks,
		int blindnessAmplifier,
		int nauseaDurationTicks,
		int nauseaAmplifier,
		int particleCount,
		float soundVolume,
		float soundPitch
	) {
		static ComedicAssistantDivineSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			int strikeCount,
			double strikeRadius,
			int glowingDurationTicks,
			int blindnessDurationTicks,
			int blindnessAmplifier,
			int nauseaDurationTicks,
			int nauseaAmplifier,
			int particleCount,
			float soundVolume,
			float soundPitch
		) {
			return new ComedicAssistantDivineSettings(
				enabled,
				weight,
				bonusDamage,
				strikeCount,
				strikeRadius,
				glowingDurationTicks,
				blindnessDurationTicks,
				blindnessAmplifier,
				nauseaDurationTicks,
				nauseaAmplifier,
				particleCount,
				soundVolume,
				soundPitch
			);
		}
	}

	record ComedicAssistantAcmeSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		int slownessDurationTicks,
		int slownessAmplifier,
		int weaknessDurationTicks,
		int weaknessAmplifier,
		int visualDurationTicks,
		double visualDropHeight,
		int particleCount,
		float soundVolume,
		float soundPitch
	) {
		static ComedicAssistantAcmeSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			int slownessDurationTicks,
			int slownessAmplifier,
			int weaknessDurationTicks,
			int weaknessAmplifier,
			int visualDurationTicks,
			double visualDropHeight,
			int particleCount,
			float soundVolume,
			float soundPitch
		) {
			return new ComedicAssistantAcmeSettings(
				enabled,
				weight,
				bonusDamage,
				slownessDurationTicks,
				slownessAmplifier,
				weaknessDurationTicks,
				weaknessAmplifier,
				visualDurationTicks,
				visualDropHeight,
				particleCount,
				soundVolume,
				soundPitch
			);
		}
	}

	record ComedicAssistantPieSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		int blindnessDurationTicks,
		int blindnessAmplifier,
		int nauseaDurationTicks,
		int nauseaAmplifier,
		int particleCount,
		float soundVolume,
		float soundPitch
	) {
		static ComedicAssistantPieSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			int blindnessDurationTicks,
			int blindnessAmplifier,
			int nauseaDurationTicks,
			int nauseaAmplifier,
			int particleCount,
			float soundVolume,
			float soundPitch
		) {
			return new ComedicAssistantPieSettings(
				enabled,
				weight,
				bonusDamage,
				blindnessDurationTicks,
				blindnessAmplifier,
				nauseaDurationTicks,
				nauseaAmplifier,
				particleCount,
				soundVolume,
				soundPitch
			);
		}
	}

	record ComedicAssistantCaneSettings(
		boolean enabled,
		int weight,
		float bonusDamage,
		double horizontalLaunch,
		double verticalLaunch,
		int launchControlTicks,
		int velocityDamageTrackingTicks,
		double velocityDamageThreshold,
		double velocityDamageMultiplier,
		double velocityDamageMax,
		int slownessDurationTicks,
		int slownessAmplifier,
		int visualDurationTicks,
		double visualSpawnDistance,
		double visualChargeVelocity,
		int particleCount,
		float soundVolume,
		float soundPitch
	) {
		static ComedicAssistantCaneSettings defaults(
			boolean enabled,
			int weight,
			float bonusDamage,
			double horizontalLaunch,
			double verticalLaunch,
			int launchControlTicks,
			int velocityDamageTrackingTicks,
			double velocityDamageThreshold,
			double velocityDamageMultiplier,
			double velocityDamageMax,
			int slownessDurationTicks,
			int slownessAmplifier,
			int visualDurationTicks,
			double visualSpawnDistance,
			double visualChargeVelocity,
			int particleCount,
			float soundVolume,
			float soundPitch
		) {
			return new ComedicAssistantCaneSettings(
				enabled,
				weight,
				bonusDamage,
				horizontalLaunch,
				verticalLaunch,
				launchControlTicks,
				velocityDamageTrackingTicks,
				velocityDamageThreshold,
				velocityDamageMultiplier,
				velocityDamageMax,
				slownessDurationTicks,
				slownessAmplifier,
				visualDurationTicks,
				visualSpawnDistance,
				visualChargeVelocity,
				particleCount,
				soundVolume,
				soundPitch
			);
		}
	}

	enum ComedicRewriteOutcome {
		LAUNCHED_THROUGH_THE_SCENE,
		RAVAGER_BIT,
		PARROT_RESCUE
	}

	record PendingComedicRewriteState(
		ComedicRewriteOutcome outcome,
		double severity,
		Vec3d launchDirection,
		Vec3d safePos
	) {
		PendingComedicRewriteState withOutcome(ComedicRewriteOutcome replacementOutcome) {
			return new PendingComedicRewriteState(replacementOutcome, severity, launchDirection, safePos);
		}
	}

	static final class ComedicRewriteVisualCameo {
		final int endTick;
		final int chargeEndTick;
		final int[] entityIds;
		final List<UUID> viewerIds;
		final ComedicRewriteVisualFollowState followState;
		final ComedicRewriteVisualRotationState rotationState;
		final Entity[] entities;
		boolean chargeStopped;

		ComedicRewriteVisualCameo(
			int endTick,
			int chargeEndTick,
			int[] entityIds,
			List<UUID> viewerIds,
			ComedicRewriteVisualFollowState followState,
			ComedicRewriteVisualRotationState rotationState,
			Entity[] entities
		) {
			this.endTick = endTick;
			this.chargeEndTick = chargeEndTick;
			this.entityIds = entityIds;
			this.viewerIds = viewerIds;
			this.followState = followState;
			this.rotationState = rotationState;
			this.entities = entities;
		}

		int endTick() {
			return endTick;
		}

		int chargeEndTick() {
			return chargeEndTick;
		}

		int[] entityIds() {
			return entityIds;
		}

		List<UUID> viewerIds() {
			return viewerIds;
		}

		ComedicRewriteVisualFollowState followState() {
			return followState;
		}

		ComedicRewriteVisualRotationState rotationState() {
			return rotationState;
		}

		Entity[] entities() {
			return entities;
		}

		boolean chargeStopped() {
			return chargeStopped;
		}

		void markChargeStopped() {
			chargeStopped = true;
		}
	}

	record ComedicRewriteVisualFollowState(
		RegistryKey<World> dimension,
		UUID anchorEntityId,
		Vec3d[] offsets,
		ComedicRewriteVisualFollowMode mode,
		double verticalSpeed
	) {
	}

	enum ComedicRewriteVisualFollowMode {
		ENTITY_TOP,
		DROP_TO_ENTITY_TOP
	}

	record ComedicRewriteVisualRotationState(
		boolean alignYawToVelocity,
		float yawOffsetDegrees,
		float pitchPerTick
	) {
	}

	record ComedicRewriteLaunchSettings(
		int weight,
		double baseHorizontalVelocity,
		double horizontalVelocityPerSeverity,
		double baseVerticalVelocity,
		double verticalVelocityPerSeverity,
		int slownessDurationTicks,
		int slownessAmplifier,
		int smokeParticleCount,
		int poofParticleCount
	) {
		static ComedicRewriteLaunchSettings defaults(
			int weight,
			double baseHorizontalVelocity,
			double horizontalVelocityPerSeverity,
			double baseVerticalVelocity,
			double verticalVelocityPerSeverity,
			int slownessDurationTicks,
			int slownessAmplifier,
			int smokeParticleCount,
			int poofParticleCount
		) {
			return new ComedicRewriteLaunchSettings(
				weight,
				baseHorizontalVelocity,
				horizontalVelocityPerSeverity,
				baseVerticalVelocity,
				verticalVelocityPerSeverity,
				slownessDurationTicks,
				slownessAmplifier,
				smokeParticleCount,
				poofParticleCount
			);
		}
	}

	record ComedicRewriteRavagerSettings(
		int weight,
		double baseHorizontalVelocity,
		double horizontalVelocityPerSeverity,
		double baseVerticalVelocity,
		double verticalVelocityPerSeverity,
		int slownessDurationTicks,
		int slownessAmplifier,
		int smokeParticleCount,
		int dustParticleCount,
		boolean showVisualCameo,
		int visualDurationTicks,
		double visualSpawnDistance,
		double visualVerticalOffset,
		double visualChargeVelocity,
		double visualChargeVelocityBuffer,
		int visualChargeDurationTicks
	) {
		static ComedicRewriteRavagerSettings defaults(
			int weight,
			double baseHorizontalVelocity,
			double horizontalVelocityPerSeverity,
			double baseVerticalVelocity,
			double verticalVelocityPerSeverity,
			int slownessDurationTicks,
			int slownessAmplifier,
			int smokeParticleCount,
			int dustParticleCount,
			boolean showVisualCameo,
			int visualDurationTicks,
			double visualSpawnDistance,
			double visualVerticalOffset,
			double visualChargeVelocity,
			double visualChargeVelocityBuffer,
			int visualChargeDurationTicks
		) {
			return new ComedicRewriteRavagerSettings(
				weight,
				baseHorizontalVelocity,
				horizontalVelocityPerSeverity,
				baseVerticalVelocity,
				verticalVelocityPerSeverity,
				slownessDurationTicks,
				slownessAmplifier,
				smokeParticleCount,
				dustParticleCount,
				showVisualCameo,
				visualDurationTicks,
				visualSpawnDistance,
				visualVerticalOffset,
				visualChargeVelocity,
				visualChargeVelocityBuffer,
				visualChargeDurationTicks
			);
		}
	}

	record ComedicRewriteParrotSettings(
		int weight,
		double carryHeight,
		double carryHeightPerSeverity,
		double sideVelocity,
		double sideVelocityPerSeverity,
		int slowFallingDurationTicks,
		int levitationDurationTicks,
		int featherParticleCount,
		boolean showVisualCameo,
		int visualDurationTicks,
		int visualCount,
		double visualRadius,
		double visualVerticalOffset,
		boolean visualFollowPlayerHead,
		double visualLiftVelocity
	) {
		static ComedicRewriteParrotSettings defaults(
			int weight,
			double carryHeight,
			double carryHeightPerSeverity,
			double sideVelocity,
			double sideVelocityPerSeverity,
			int slowFallingDurationTicks,
			int levitationDurationTicks,
			int featherParticleCount,
			boolean showVisualCameo,
			int visualDurationTicks,
			int visualCount,
			double visualRadius,
			double visualVerticalOffset,
			boolean visualFollowPlayerHead,
			double visualLiftVelocity
		) {
			return new ComedicRewriteParrotSettings(
				weight,
				carryHeight,
				carryHeightPerSeverity,
				sideVelocity,
				sideVelocityPerSeverity,
				slowFallingDurationTicks,
				levitationDurationTicks,
				featherParticleCount,
				showVisualCameo,
				visualDurationTicks,
				visualCount,
				visualRadius,
				visualVerticalOffset,
				visualFollowPlayerHead,
				visualLiftVelocity
			);
		}
	}
}


