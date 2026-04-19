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


abstract class MagicAbilityManagerSupportPackets extends MagicAbilityManagerSupportJesterRequests {
	static void debugManipulation(String message, Object... args) {
	}

	public static void debugManipulationPacket(String message, Object... args) {
	}

	static boolean isManipulationPacketServerThread(ServerPlayerEntity player) {
		MinecraftServer server = player.getEntityWorld().getServer();
		return server != null && server.isOnThread();
	}

	public static void onManipulationInputPacket(ServerPlayerEntity player, PlayerInput input) {
		if (!isManipulationPacketServerThread(player)) {
			return;
		}

		UUID playerId = player.getUuid();
		MANIPULATION_INPUT_BY_CASTER.put(playerId, input);
		if (!isManipulatingCaster(player) && !isManipulationControlledTarget(player)) {
			return;
		}

		debugManipulation(
			"{} player input packet: forward={}, backward={}, left={}, right={}, jump={}, sneak={}, sprint={}",
			debugName(player),
			input.forward(),
			input.backward(),
			input.left(),
			input.right(),
			input.jump(),
			input.sneak(),
			input.sprint()
		);
	}

	public static void onManipulationLookPacket(ServerPlayerEntity player, float yaw, float pitch) {
		if (!isManipulationPacketServerThread(player)) {
			return;
		}

		UUID playerId = player.getUuid();
		MANIPULATION_LOOK_BY_CASTER.put(playerId, new ManipulationLookState(yaw, pitch));
		if (!isManipulatingCaster(player) && !isManipulationControlledTarget(player)) {
			return;
		}

		debugManipulation(
			"{} look packet: yaw={}, pitch={}",
			debugName(player),
			round3(yaw),
			round3(pitch)
		);
	}

	public static boolean handleCelestialPlayerMovePacket(ServerPlayerEntity player, PlayerMoveC2SPacket packet) {
		if (!isManipulationPacketServerThread(player)) {
			return false;
		}

		if (!isCelestialDelayBypassActive()) {
			int delayTicks = celestialReticulumDelayTicks(player);
			if (delayTicks > 0) {
				queueCelestialAction(player.getUuid(), new DelayedMoveAction(currentTick(player) + delayTicks, packet));
				return true;
			}
		}

		if (isCelestialTransformBypassActive()) {
			return false;
		}

		PlayerMoveC2SPacket transformed = transformCelestialPyxisMovePacket(player, packet);
		if (transformed == null) {
			return false;
		}

		runWithCelestialDelayAndTransformBypass(() -> player.networkHandler.onPlayerMove(transformed));
		return true;
	}

	public static boolean handleCelestialPlayerInputPacket(ServerPlayerEntity player, PlayerInputC2SPacket packet) {
		if (!isManipulationPacketServerThread(player)) {
			return false;
		}

		if (!isCelestialDelayBypassActive()) {
			int delayTicks = celestialReticulumDelayTicks(player);
			if (delayTicks > 0) {
				queueCelestialAction(player.getUuid(), new DelayedInputAction(currentTick(player) + delayTicks, packet));
				return true;
			}
		}

		if (isCelestialTransformBypassActive()) {
			return false;
		}

		PlayerInput transformed = transformCelestialPyxisInput(player, packet.input());
		if (transformed.equals(packet.input())) {
			return false;
		}

		runWithCelestialDelayAndTransformBypass(() -> player.networkHandler.onPlayerInput(new PlayerInputC2SPacket(transformed)));
		return true;
	}

	public static boolean handleCelestialClientCommandPacket(ServerPlayerEntity player, ClientCommandC2SPacket packet) {
		if (!isManipulationPacketServerThread(player) || isCelestialDelayBypassActive()) {
			return false;
		}
		int delayTicks = celestialReticulumDelayTicks(player);
		if (delayTicks <= 0) {
			return false;
		}
		queueCelestialAction(player.getUuid(), new DelayedClientCommandAction(currentTick(player) + delayTicks, packet));
		return true;
	}

	public static boolean handleCelestialPlayerActionPacket(ServerPlayerEntity player, PlayerActionC2SPacket packet) {
		if (!isManipulationPacketServerThread(player) || isCelestialDelayBypassActive()) {
			return false;
		}
		int delayTicks = celestialReticulumDelayTicks(player);
		if (delayTicks <= 0) {
			return false;
		}
		queueCelestialAction(player.getUuid(), new DelayedPlayerActionAction(currentTick(player) + delayTicks, packet));
		return true;
	}

	public static boolean handleCelestialInteractBlockPacket(ServerPlayerEntity player, PlayerInteractBlockC2SPacket packet) {
		if (!isManipulationPacketServerThread(player) || isCelestialDelayBypassActive()) {
			return false;
		}
		int delayTicks = celestialReticulumDelayTicks(player);
		if (delayTicks <= 0) {
			return false;
		}
		queueCelestialAction(player.getUuid(), new DelayedInteractBlockAction(currentTick(player) + delayTicks, packet));
		return true;
	}

	public static boolean handleCelestialInteractItemPacket(ServerPlayerEntity player, PlayerInteractItemC2SPacket packet) {
		if (!isManipulationPacketServerThread(player) || isCelestialDelayBypassActive()) {
			return false;
		}
		int delayTicks = celestialReticulumDelayTicks(player);
		if (delayTicks <= 0) {
			return false;
		}
		queueCelestialAction(player.getUuid(), new DelayedInteractItemAction(currentTick(player) + delayTicks, packet));
		return true;
	}

	public static boolean handleCelestialInteractEntityPacket(ServerPlayerEntity player, PlayerInteractEntityC2SPacket packet) {
		if (!isManipulationPacketServerThread(player) || isCelestialDelayBypassActive()) {
			return false;
		}
		int delayTicks = celestialReticulumDelayTicks(player);
		if (delayTicks <= 0) {
			return false;
		}
		queueCelestialAction(player.getUuid(), new DelayedInteractEntityAction(currentTick(player) + delayTicks, packet));
		return true;
	}

	public static boolean queueCelestialAbilityRequest(ServerPlayerEntity player, int abilitySlot) {
		if (player == null || isCelestialDelayBypassActive()) {
			return false;
		}
		int delayTicks = celestialReticulumDelayTicks(player);
		if (delayTicks <= 0) {
			return false;
		}
		queueCelestialAction(player.getUuid(), new DelayedAbilityRequestAction(currentTick(player) + delayTicks, abilitySlot));
		return true;
	}

	static void updateCelestialDelayedActions(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, List<DelayedCelestialAction>>> iterator = CELESTIAL_DELAYED_ACTIONS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, List<DelayedCelestialAction>> entry = iterator.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			if (player == null || !player.isAlive()) {
				iterator.remove();
				continue;
			}
			if (celestialReticulumDelayTicks(player) <= 0) {
				entry.getValue().clear();
				iterator.remove();
				continue;
			}
			Iterator<DelayedCelestialAction> actionIterator = entry.getValue().iterator();
			while (actionIterator.hasNext()) {
				DelayedCelestialAction action = actionIterator.next();
				if (currentTick < action.executeTick()) {
					continue;
				}
				runWithCelestialDelayBypass(() -> action.apply(player));
				actionIterator.remove();
			}
			if (entry.getValue().isEmpty()) {
				iterator.remove();
			}
		}
	}

	static void queueCelestialAction(UUID playerId, DelayedCelestialAction action) {
		CELESTIAL_DELAYED_ACTIONS.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(action);
	}

	static void clearCelestialDelayQueue(UUID playerId) {
		CELESTIAL_DELAYED_ACTIONS.remove(playerId);
	}

	static void clearCelestialDelayQueueIfUnaffected(MinecraftServer server, UUID playerId, CelestialAlignmentState ignoredState) {
		ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
		if (!(player instanceof ServerPlayerEntity serverPlayer) || celestialReticulumDelayTicks(serverPlayer, ignoredState) > 0) {
			return;
		}
		clearCelestialDelayQueue(playerId);
	}

	static int celestialReticulumDelayTicks(ServerPlayerEntity player) {
		return celestialReticulumDelayTicks(player, null);
	}

	static int celestialReticulumDelayTicks(ServerPlayerEntity player, CelestialAlignmentState ignoredState) {
		return 0;
	}

	static PlayerInput transformCelestialPyxisInput(ServerPlayerEntity player, PlayerInput input) {
		return input;
	}

	static PlayerMoveC2SPacket transformCelestialPyxisMovePacket(ServerPlayerEntity player, PlayerMoveC2SPacket packet) {
		return null;
	}

	static void runWithCelestialDelayBypass(Runnable action) {
		int depth = CELESTIAL_DELAY_BYPASS_DEPTH.get();
		CELESTIAL_DELAY_BYPASS_DEPTH.set(depth + 1);
		try {
			action.run();
		} finally {
			if (depth <= 0) {
				CELESTIAL_DELAY_BYPASS_DEPTH.remove();
			} else {
				CELESTIAL_DELAY_BYPASS_DEPTH.set(depth);
			}
		}
	}

	static void runWithCelestialDelayAndTransformBypass(Runnable action) {
		int delayDepth = CELESTIAL_DELAY_BYPASS_DEPTH.get();
		int transformDepth = CELESTIAL_TRANSFORM_BYPASS_DEPTH.get();
		CELESTIAL_DELAY_BYPASS_DEPTH.set(delayDepth + 1);
		CELESTIAL_TRANSFORM_BYPASS_DEPTH.set(transformDepth + 1);
		try {
			action.run();
		} finally {
			if (delayDepth <= 0) {
				CELESTIAL_DELAY_BYPASS_DEPTH.remove();
			} else {
				CELESTIAL_DELAY_BYPASS_DEPTH.set(delayDepth);
			}
			if (transformDepth <= 0) {
				CELESTIAL_TRANSFORM_BYPASS_DEPTH.remove();
			} else {
				CELESTIAL_TRANSFORM_BYPASS_DEPTH.set(transformDepth);
			}
		}
	}

	static boolean isCelestialDelayBypassActive() {
		return CELESTIAL_DELAY_BYPASS_DEPTH.get() > 0;
	}

	static boolean isCelestialTransformBypassActive() {
		return CELESTIAL_TRANSFORM_BYPASS_DEPTH.get() > 0;
	}

	static boolean isLovePowerActiveThisSecond(ServerPlayerEntity player) {
		return LOVE_POWER_ACTIVE_THIS_SECOND.getOrDefault(player.getUuid(), false);
	}

	static boolean isControlLocked(PlayerEntity player) {
		return isLoveLocked(player)
			|| isDomainClashParticipantFrozen(player)
			|| isGreedDomainIntroFrozen(player)
			|| isAstralExecutionTarget(player)
			|| isFrostMaximumFearLocked(player)
			|| isFrostHelpless(player);
	}

	static boolean isMagicSuppressed(PlayerEntity player) {
		return MANIPULATION_CASTER_BY_TARGET.containsKey(player.getUuid());
	}

	static boolean isDomainClashParticipantFrozen(PlayerEntity player) {
		DomainClashState clash = domainClashStateForParticipant(player.getUuid());
		if (clash == null) {
			return false;
		}

		MinecraftServer server = player.getEntityWorld().getServer();
		return server != null && isDomainClashFrozen(clash, server.getTicks());
	}

	static boolean isLoveLocked(PlayerEntity player) {
		return LOVE_LOCKED_TARGETS.containsKey(player.getUuid());
	}

	static boolean isAstralExecutionTarget(PlayerEntity player) {
		return ASTRAL_EXECUTION_BEAMS.containsKey(player.getUuid());
	}

	static boolean isDirectCasterDamageBlocked(ServerPlayerEntity attacker, LivingEntity target, int currentTick) {
		return isLoveCasterDamageBlocked(attacker, target, currentTick) || isHerculesCasterDamageBlocked(attacker, target, currentTick);
	}

	static boolean isLoveCasterDamageBlocked(ServerPlayerEntity attacker, LivingEntity target, int currentTick) {
		if (!LOVE_AT_FIRST_SIGHT_PREVENT_CASTER_DIRECT_DAMAGE) {
			return false;
		}

		LoveLockState state = LOVE_LOCKED_TARGETS.get(target.getUuid());
		if (state == null || !attacker.getUuid().equals(state.casterId) || state.lastSeenTick != currentTick) {
			return false;
		}

		return activeAbility(attacker) == MagicAbility.LOVE_AT_FIRST_SIGHT;
	}

	static boolean isHerculesCasterDamageBlocked(ServerPlayerEntity attacker, LivingEntity target, int currentTick) {
		if (!HERCULES_PREVENT_CASTER_DIRECT_DAMAGE) {
			return false;
		}

		AstralBurdenTargetState state = HERCULES_TARGETS.get(target.getUuid());
		return state != null && attacker.getUuid().equals(state.casterId) && currentTick < state.endTick;
	}

	static boolean isLoveItemUseBlocked(PlayerEntity player) {
		return LOVE_AT_FIRST_SIGHT_BLOCK_ITEM_USE && isLoveLocked(player);
	}

	static boolean isItemUseBlocked(PlayerEntity player) {
		return isLoveItemUseBlocked(player)
			|| isDomainClashParticipantFrozen(player)
			|| isGreedDomainIntroFrozen(player)
			|| isAstralExecutionTarget(player)
			|| isFrostHelpless(player);
	}

	static boolean isAttackBlocked(PlayerEntity player) {
		return (LOVE_AT_FIRST_SIGHT_BLOCK_ATTACKS && isLoveLocked(player))
			|| isDomainClashParticipantFrozen(player)
			|| isGreedDomainIntroFrozen(player)
			|| isAstralExecutionTarget(player)
			|| isFrostHelpless(player);
	}

	static boolean isGreedDomainIntroFrozen(PlayerEntity player) {
		if (!(player instanceof ServerPlayerEntity serverPlayer) || player.getEntityWorld().isClient()) {
			return false;
		}
		MinecraftServer server = player.getEntityWorld().getServer();
		return server != null && GreedDomainRuntime.isPlayerFrozenDuringIntro(serverPlayer, server.getTicks());
	}

	public static boolean isPlayerControlLocked(ServerPlayerEntity player) {
		return isControlLocked(player);
	}

	public static boolean shouldBlockHealing(LivingEntity entity) {
		return entity != null && (isFrostHelpless(entity) || isCelestialHealingBlocked(entity));
	}

	public static boolean shouldBlockFrostTeleport(Entity entity) {
		return entity != null && isFrostTeleportBlocked(entity);
	}

	public static boolean isManipulatingCaster(ServerPlayerEntity player) {
		return MANIPULATION_STATES.containsKey(player.getUuid()) && activeAbility(player) == MagicAbility.MANIPULATION;
	}

	public static boolean isManipulationControlledTarget(ServerPlayerEntity player) {
		return MANIPULATION_CASTER_BY_TARGET.containsKey(player.getUuid());
	}

	public static void beginManipulationMovementProxy(ServerPlayerEntity caster) {
		if (isManipulatingCaster(caster)) {
			debugManipulation(
				"{} movement proxy begin (noop): casterPos=[{}, {}, {}], yaw={}, pitch={}",
				debugName(caster),
				round3(caster.getX()),
				round3(caster.getY()),
				round3(caster.getZ()),
				round3(caster.getYaw()),
				round3(caster.getPitch())
			);
		}
	}

	public static void endManipulationMovementProxy(ServerPlayerEntity caster) {
		if (isManipulatingCaster(caster)) {
			debugManipulation(
				"{} movement proxy end (noop): casterPos=[{}, {}, {}], velocity=[{}, {}, {}]",
				debugName(caster),
				round3(caster.getX()),
				round3(caster.getY()),
				round3(caster.getZ()),
				round3(caster.getVelocity().x),
				round3(caster.getVelocity().y),
				round3(caster.getVelocity().z)
			);
		}
	}

	public static void beginManipulationInteractionProxy(ServerPlayerEntity caster) {
		if (!isManipulationPacketServerThread(caster)) {
			return;
		}

		if (!isManipulatingCaster(caster)) {
			return;
		}

		UUID casterId = caster.getUuid();
		if (MANIPULATION_INTERACTION_PROXY.containsKey(casterId)) {
			debugManipulation("{} interaction proxy begin skipped: already active", debugName(caster));
			return;
		}

		ManipulationState state = MANIPULATION_STATES.get(casterId);
		if (state == null) {
			debugManipulation("{} interaction proxy begin skipped: state missing", debugName(caster));
			return;
		}

		ServerPlayerEntity target = resolveManipulationTarget(caster.getEntityWorld().getServer(), state);
		if (target == null) {
			deactivateManipulation(caster, true, "target missing during interaction proxy");
			setActiveAbility(caster, MagicAbility.NONE);
			return;
		}

		MANIPULATION_INTERACTION_PROXY.put(casterId, new ManipulationProxyState(new Vec3d(caster.getX(), caster.getY(), caster.getZ()), caster.getYaw(), caster.getPitch()));
		caster.setPosition(target.getX(), target.getY(), target.getZ());
		caster.setYaw(target.getYaw());
		caster.setPitch(target.getPitch());
		caster.setHeadYaw(target.getHeadYaw());
		caster.setBodyYaw(target.getBodyYaw());
		debugManipulation(
			"{} interaction proxy begin: moved caster to target {} at [{}, {}, {}] yaw={} pitch={}",
			debugName(caster),
			target.getUuid(),
			round3(target.getX()),
			round3(target.getY()),
			round3(target.getZ()),
			round3(target.getYaw()),
			round3(target.getPitch())
		);
	}

	public static void endManipulationInteractionProxy(ServerPlayerEntity caster) {
		if (!isManipulationPacketServerThread(caster)) {
			return;
		}

		UUID casterId = caster.getUuid();
		ManipulationProxyState proxyState = MANIPULATION_INTERACTION_PROXY.remove(casterId);
		if (proxyState == null) {
			debugManipulation("{} interaction proxy end skipped: no active proxy", debugName(caster));
			return;
		}

		ManipulationState manipulationState = MANIPULATION_STATES.get(casterId);
		Vec3d restorePos = manipulationState == null ? proxyState.originalPos : manipulationState.lockedCasterPos;
		caster.setPosition(restorePos.x, restorePos.y, restorePos.z);
		caster.setYaw(proxyState.yaw);
		caster.setPitch(proxyState.pitch);
		caster.setHeadYaw(proxyState.yaw);
		caster.setBodyYaw(proxyState.yaw);
		debugManipulation(
			"{} interaction proxy end: restoredPos=[{}, {}, {}], yaw={}, pitch={}",
			debugName(caster),
			round3(restorePos.x),
			round3(restorePos.y),
			round3(restorePos.z),
			round3(proxyState.yaw),
			round3(proxyState.pitch)
		);
	}

	public static boolean shouldCancelManipulationEntityAttack(ServerPlayerEntity caster, PlayerInteractEntityC2SPacket packet) {
		if (!isManipulatingCaster(caster) || !isAttackInteraction(packet)) {
			return false;
		}

		Entity entity = packet.getEntity(caster.getEntityWorld());
		if (entity == null || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity == caster) {
			debugManipulation(
				"{} manipulation entity attack canceled: invalid entity target (entity={}, self={})",
				debugName(caster),
				entity == null ? "null" : entity.getClass().getSimpleName(),
				entity == caster
			);
			return true;
		}

		if (entity instanceof PersistentProjectileEntity projectile && !projectile.isAttackable()) {
			debugManipulation("{} manipulation entity attack canceled: non-attackable projectile {}", debugName(caster), projectile.getUuid());
			return true;
		}

		ManipulationState state = MANIPULATION_STATES.get(caster.getUuid());
		if (state != null && entity.getUuid().equals(state.targetId)) {
			debugManipulation("{} manipulation entity attack allowed: controlled target {}", debugName(caster), state.targetId);
		} else {
			debugManipulation("{} manipulation entity attack allowed: targetEntity={}", debugName(caster), entity.getUuid());
		}
		return false;
	}

	public static void recordBurningPassionMeleeImpact(ServerPlayerEntity attacker, PlayerInteractEntityC2SPacket packet) {
		if (attacker == null || packet == null || !isAttackInteraction(packet)) {
			return;
		}

		Entity entity = packet.getEntity(attacker.getEntityWorld());
		if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target) || target == attacker) {
			return;
		}

		BurningPassionIgnitionState ignitionState = burningPassionIgnitionState(attacker);
		if (MagicPlayerData.getSchool(attacker) != MagicSchool.BURNING_PASSION || ignitionState == null || ignitionState.currentStage < 2) {
			BURNING_PASSION_PENDING_MELEE_IMPACTS.remove(attacker.getUuid());
			return;
		}

		MinecraftServer server = attacker.getEntityWorld().getServer();
		if (server == null) {
			return;
		}

		BURNING_PASSION_PENDING_MELEE_IMPACTS.put(
			attacker.getUuid(),
			new BurningPassionPendingMeleeImpactState(
				attacker.getEntityWorld().getRegistryKey(),
				target.getUuid(),
				resolveBurningPassionMeleeImpactPosition(attacker, target),
				server.getTicks() + 4
			)
		);
	}
}

