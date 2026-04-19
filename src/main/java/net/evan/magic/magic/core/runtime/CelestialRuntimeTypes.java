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


abstract class CelestialRuntimeTypes extends ElementalRuntimeTypes {
	enum CelestialAlignmentConstellation {
		CRATER("magic.constellation.celestial_alignment.crater"),
		SAGITTA("magic.constellation.celestial_alignment.sagitta"),
		GEMINI("magic.constellation.celestial_alignment.gemini"),
		AQUILA("magic.constellation.celestial_alignment.aquila"),
		SCORPIUS("magic.constellation.celestial_alignment.scorpius"),
		LIBRA("magic.constellation.celestial_alignment.libra");

		final String translationKey;

		CelestialAlignmentConstellation(String translationKey) {
			this.translationKey = translationKey;
		}
	}

	static final class CelestialPattern {
		final List<Vec3d> nodes;
		final List<CelestialPatternEdge> edges;

		CelestialPattern(List<Vec3d> nodes, List<CelestialPatternEdge> edges) {
			this.nodes = nodes;
			this.edges = edges;
		}
	}

	static final class CelestialPatternEdge {
		final int startIndex;
		final int endIndex;

		CelestialPatternEdge(int startIndex, int endIndex) {
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}
	}

	static final class CelestialAlignmentState {
		final UUID ownerId;
		final RegistryKey<World> dimension;
		final Vec3d center;
		final double radius;
		final double minY;
		final double maxY;
		final CelestialAlignmentConstellation constellation;
		final int startTick;
		int nextVisualTick;
		int nextEffectTick;
		int nextConstellationTick;
		final Set<UUID> trackedLivingIds = new HashSet<>();
		final Map<UUID, List<Float>> geminiRecordedDamage = new HashMap<>();
		final List<AquilaStarState> aquilaStars = new ArrayList<>();
		final List<CelestialSagittaStrikeState> sagittaStrikes = new ArrayList<>();

		CelestialAlignmentState(
			UUID ownerId,
			RegistryKey<World> dimension,
			Vec3d center,
			double radius,
			double minY,
			double maxY,
			CelestialAlignmentConstellation constellation,
			int startTick
		) {
			this.ownerId = ownerId;
			this.dimension = dimension;
			this.center = center;
			this.radius = radius;
			this.minY = minY;
			this.maxY = maxY;
			this.constellation = constellation;
			this.startTick = startTick;
			this.nextVisualTick = startTick;
			this.nextEffectTick = startTick;
			this.nextConstellationTick = startTick;
		}
	}

	static final class CelestialAlignmentSessionState {
		final UUID ownerId;
		final RegistryKey<World> dimension;
		final int startTick;
		final List<CelestialAlignmentState> constellations = new ArrayList<>();

		CelestialAlignmentSessionState(UUID ownerId, RegistryKey<World> dimension, int startTick) {
			this.ownerId = ownerId;
			this.dimension = dimension;
			this.startTick = startTick;
		}
	}

	static final class AquilaStarState {
		Vec3d position;
		Vec3d velocity;
		final UUID targetId;
		final int expireTick;

		AquilaStarState(Vec3d position, Vec3d velocity, UUID targetId, int expireTick) {
			this.position = position;
			this.velocity = velocity;
			this.targetId = targetId;
			this.expireTick = expireTick;
		}
	}

	static final class CelestialSagittaStrikeState {
		final Vec3d center;
		final int impactTick;

		CelestialSagittaStrikeState(Vec3d center, int impactTick) {
			this.center = center;
			this.impactTick = impactTick;
		}
	}

	static final class CelestialGeminiReplayState {
		final RegistryKey<World> dimension;
		final UUID casterId;
		final UUID targetId;
		final List<Float> recordedHits;
		int nextTick;
		int hitIndex;

		CelestialGeminiReplayState(RegistryKey<World> dimension, UUID casterId, UUID targetId, List<Float> recordedHits, int nextTick) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.targetId = targetId;
			this.recordedHits = recordedHits;
			this.nextTick = nextTick;
		}
	}

	enum CelestialAlignmentSessionEndReason {
		FOURTH_PRESS_CANCEL,
		SHIFT_CANCEL,
		MANA_DEPLETED
	}

	enum CelestialGamaRayPhase {
		TRACING,
		CHARGING,
		FIRING
	}

	static final class CelestialBeamPinnedTargetState {
		final RegistryKey<World> dimension;
		final double lockedX;
		final double lockedY;
		final double lockedZ;

		CelestialBeamPinnedTargetState(RegistryKey<World> dimension, double lockedX, double lockedY, double lockedZ) {
			this.dimension = dimension;
			this.lockedX = lockedX;
			this.lockedY = lockedY;
			this.lockedZ = lockedZ;
		}
	}

	static final class CelestialGamaRayState {
		final UUID casterId;
		final RegistryKey<World> dimension;
		final CelestialAlignmentConstellation constellation;
		CelestialGamaRayPhase phase;
		int traceExpireTick;
		int chargeCompleteTick;
		int endTick;
		int nextParticleTick;
		int nextDamageTick;
		int nextSoundTick;
		int nextVisualSyncTick;
		Vec3d beamOrigin = Vec3d.ZERO;
		Vec3d beamDirection = Vec3d.ZERO;
		double lockedX;
		double lockedY;
		double lockedZ;
		float lockedYaw;
		float lockedPitch;
		final Map<UUID, CelestialBeamPinnedTargetState> pinnedTargets = new HashMap<>();

		CelestialGamaRayState(UUID casterId, RegistryKey<World> dimension, CelestialAlignmentConstellation constellation, int traceExpireTick) {
			this.casterId = casterId;
			this.dimension = dimension;
			this.constellation = constellation;
			this.phase = CelestialGamaRayPhase.TRACING;
			this.traceExpireTick = traceExpireTick;
		}
	}

	interface DelayedCelestialAction {
		int executeTick();

		void apply(ServerPlayerEntity player);
	}

	record DelayedMoveAction(int executeTick, PlayerMoveC2SPacket packet) implements DelayedCelestialAction {
		@Override
		public void apply(ServerPlayerEntity player) {
			player.networkHandler.onPlayerMove(packet);
		}
	}

	record DelayedInputAction(int executeTick, PlayerInputC2SPacket packet) implements DelayedCelestialAction {
		@Override
		public void apply(ServerPlayerEntity player) {
			player.networkHandler.onPlayerInput(packet);
		}
	}

	record DelayedClientCommandAction(int executeTick, ClientCommandC2SPacket packet) implements DelayedCelestialAction {
		@Override
		public void apply(ServerPlayerEntity player) {
			player.networkHandler.onClientCommand(packet);
		}
	}

	record DelayedPlayerActionAction(int executeTick, PlayerActionC2SPacket packet) implements DelayedCelestialAction {
		@Override
		public void apply(ServerPlayerEntity player) {
			player.networkHandler.onPlayerAction(packet);
		}
	}

	record DelayedInteractBlockAction(int executeTick, PlayerInteractBlockC2SPacket packet) implements DelayedCelestialAction {
		@Override
		public void apply(ServerPlayerEntity player) {
			player.networkHandler.onPlayerInteractBlock(packet);
		}
	}

	record DelayedInteractItemAction(int executeTick, PlayerInteractItemC2SPacket packet) implements DelayedCelestialAction {
		@Override
		public void apply(ServerPlayerEntity player) {
			player.networkHandler.onPlayerInteractItem(packet);
		}
	}

	record DelayedInteractEntityAction(int executeTick, PlayerInteractEntityC2SPacket packet) implements DelayedCelestialAction {
		@Override
		public void apply(ServerPlayerEntity player) {
			player.networkHandler.onPlayerInteractEntity(packet);
		}
	}

	record DelayedAbilityRequestAction(int executeTick, int abilitySlot) implements DelayedCelestialAction {
		@Override
		public void apply(ServerPlayerEntity player) {
			onAbilityRequested(player, abilitySlot);
		}
	}

	enum OrionGambitStage {
		WAITING,
		LINKED
	}

	enum OrionGambitEndReason {
		WAITING_CANCEL,
		MANUAL_CANCEL,
		EXPIRED,
		TARGET_INVALID,
		MANA_DEPLETED,
		SCHOOL_CHANGED,
		CASTER_DIED
	}

	static final class OrionGambitState {
		OrionGambitStage stage;
		RegistryKey<World> targetDimension;
		UUID targetId;
		int benefitEndTick;
		final Set<MagicAbility> usedTargetAbilities = new HashSet<>();
		final Map<MagicAbility, Integer> usedTargetCooldownOverrides = new HashMap<>();

		OrionGambitState(OrionGambitStage stage) {
			this.stage = stage;
		}

		static OrionGambitState waiting() {
			return new OrionGambitState(OrionGambitStage.WAITING);
		}
	}

	static final class OrionPenaltyState {
		final int endTick;

		OrionPenaltyState(int endTick) {
			this.endTick = endTick;
		}
	}

	static final class OrionPendingEndState {
		final ServerPlayerEntity caster;
		final OrionGambitEndReason reason;
		final boolean sendFeedback;

		OrionPendingEndState(ServerPlayerEntity caster, OrionGambitEndReason reason, boolean sendFeedback) {
			this.caster = caster;
			this.reason = reason;
			this.sendFeedback = sendFeedback;
		}
	}

	enum PlusUltraEndMode {
		FULL,
		MANUAL_EARLY,
		ADMIN_CLEAR
	}

	static final class PlusUltraState {
		final RegistryKey<World> dimension;
		final int startTick;
		final int halfwayTick;
		final int endTick;
		int nextOutlineTick;
		int nextTextTick;
		final boolean hadAllowFlying;
		final boolean hadFlying;
		final float hadFlySpeed;
		final int overheadTextEndTick;
		UUID overheadTextEntityId;

		PlusUltraState(
			RegistryKey<World> dimension,
			int startTick,
			int halfwayTick,
			int endTick,
			int nextOutlineTick,
			int nextTextTick,
			boolean hadAllowFlying,
			boolean hadFlying,
			float hadFlySpeed,
			int overheadTextEndTick,
			UUID overheadTextEntityId
		) {
			this.dimension = dimension;
			this.startTick = startTick;
			this.halfwayTick = halfwayTick;
			this.endTick = endTick;
			this.nextOutlineTick = nextOutlineTick;
			this.nextTextTick = nextTextTick;
			this.hadAllowFlying = hadAllowFlying;
			this.hadFlying = hadFlying;
			this.hadFlySpeed = hadFlySpeed;
			this.overheadTextEndTick = overheadTextEndTick;
			this.overheadTextEntityId = overheadTextEntityId;
		}

		RegistryKey<World> dimension() {
			return dimension;
		}

		int startTick() {
			return startTick;
		}

		int halfwayTick() {
			return halfwayTick;
		}

		int endTick() {
			return endTick;
		}

		int nextOutlineTick() {
			return nextOutlineTick;
		}

		void nextOutlineTick(int nextOutlineTick) {
			this.nextOutlineTick = nextOutlineTick;
		}

		int nextTextTick() {
			return nextTextTick;
		}

		void nextTextTick(int nextTextTick) {
			this.nextTextTick = nextTextTick;
		}

		boolean hadAllowFlying() {
			return hadAllowFlying;
		}

		boolean hadFlying() {
			return hadFlying;
		}

		float hadFlySpeed() {
			return hadFlySpeed;
		}

		int overheadTextEndTick() {
			return overheadTextEndTick;
		}

		UUID overheadTextEntityId() {
			return overheadTextEntityId;
		}

		void overheadTextEntityId(UUID overheadTextEntityId) {
			this.overheadTextEntityId = overheadTextEntityId;
		}
	}

	static final class PlusUltraImpactState {
		final RegistryKey<World> dimension;
		final UUID casterId;
		final int startTick;
		final int endTick;
		Vec3d lastPosition;
		Vec3d lastVelocity;
		double lastSpeed;
		boolean impacted;

		PlusUltraImpactState(
			RegistryKey<World> dimension,
			UUID casterId,
			int startTick,
			int endTick,
			Vec3d lastPosition,
			Vec3d lastVelocity,
			double lastSpeed,
			boolean impacted
		) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.startTick = startTick;
			this.endTick = endTick;
			this.lastPosition = lastPosition;
			this.lastVelocity = lastVelocity;
			this.lastSpeed = Math.max(0.0, lastSpeed);
			this.impacted = impacted;
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

		boolean impacted() {
			return impacted;
		}

		void impacted(boolean impacted) {
			this.impacted = impacted;
		}
	}

	record PlusUltraImpactHit(Vec3d position, BlockState blockState, Direction side) {
	}

	enum ConstellationDomainPhase {
		CHARGING,
		READY,
		ACQUIRING
	}

	static final class ConstellationDomainState {
		ConstellationDomainPhase phase;
		int chargeCompleteTick;
		int acquireEndTick;
		final Set<Integer> announcedExpiryWarnings = new HashSet<>();

		ConstellationDomainState(ConstellationDomainPhase phase, int chargeCompleteTick) {
			this.phase = phase;
			this.chargeCompleteTick = chargeCompleteTick;
		}
	}

	static final class AstralExecutionBeamState {
		final RegistryKey<World> dimension;
		final UUID casterId;
		final double lockedX;
		final double baseY;
		final double lockedZ;
		final double restoreX;
		final double restoreY;
		final double restoreZ;
		final float restoreYaw;
		final float restorePitch;
		double currentY;
		int nextDamageTick;
		int nextSoundTick;

		AstralExecutionBeamState(
			RegistryKey<World> dimension,
			UUID casterId,
			double lockedX,
			double baseY,
			double lockedZ,
			double restoreX,
			double restoreY,
			double restoreZ,
			float restoreYaw,
			float restorePitch,
			int nextDamageTick,
			int nextSoundTick
		) {
			this.dimension = dimension;
			this.casterId = casterId;
			this.lockedX = lockedX;
			this.baseY = baseY;
			this.lockedZ = lockedZ;
			this.restoreX = restoreX;
			this.restoreY = restoreY;
			this.restoreZ = restoreZ;
			this.restoreYaw = restoreYaw;
			this.restorePitch = restorePitch;
			this.currentY = baseY;
			this.nextDamageTick = nextDamageTick;
			this.nextSoundTick = nextSoundTick;
		}
	}
}


