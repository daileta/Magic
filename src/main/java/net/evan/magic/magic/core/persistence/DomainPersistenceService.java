package net.evan.magic.magic.ability;
import net.evan.magic.magic.core.ability.MagicAbility;
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


abstract class DomainPersistenceService extends DomainClashService {
	static void onServerStarted(MinecraftServer server) {
		domainRuntimePersistentState = null;
		cachedBeaconCoreAnchor = null;
		cachedBeaconCoreAnchorTick = Integer.MIN_VALUE;
		DOMAIN_EXPANSIONS.clear();
		DOMAIN_CLASHES_BY_OWNER.clear();
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.clear();
		DOMAIN_CLASH_PENDING_DAMAGE.clear();
		FROST_DOMAIN_COOLDOWN_END_TICK.clear();
		LOVE_DOMAIN_COOLDOWN_END_TICK.clear();
		GREED_DOMAIN_COOLDOWN_END_TICK.clear();
		MAGIC_DAMAGE_PENDING_ATTACKER.clear();
		DOMAIN_PENDING_RETURNS.clear();
		FROSTBITTEN_TARGETS.clear();
		FROST_DOMAIN_FROSTBITTEN_TARGETS.clear();
		FROST_DOMAIN_PULSE_STATUS_TARGETS.clear();
		FROST_STAGE_STATES.clear();
		FROST_MAXIMUM_STATES.clear();
		FROST_SPIKE_WAVES.clear();
		FROST_FROZEN_TARGETS.clear();
		FROST_DOMAIN_FROZEN_TARGETS.clear();
		FROST_MAXIMUM_FEAR_TARGETS.clear();
		FROST_HELPLESS_TARGETS.clear();
		FROST_PACKED_ICE_TARGETS.clear();
		BELOW_FREEZING_COOLDOWN_END_TICK.clear();
		FROST_ASCENT_COOLDOWN_END_TICK.clear();
		FROST_MANA_REGEN_BLOCKED_END_TICK.clear();
		MANA_REGEN_BUFFER.clear();
		MARTYRS_FLAME_PASSIVE_ENABLED.clear();
		MARTYRS_FLAME_COOLDOWN_END_TICK.clear();
		MARTYRS_FLAME_DRAIN_BUFFER.clear();
		MARTYRS_FLAME_BURNING_TARGETS.clear();
		BURNING_PASSION_IGNITION_STATES.clear();
		BURNING_PASSION_AURA_FIRE_TARGETS.clear();
		BURNING_PASSION_SELF_FIRE_TARGETS.clear();
		BURNING_PASSION_SEARING_DASH_STATES.clear();
		BURNING_PASSION_TRAIL_LINES.clear();
		BURNING_PASSION_CINDER_MARK_ARMED_STATES.clear();
		BURNING_PASSION_CINDER_MARKS.clear();
		BURNING_PASSION_ENGINE_HEART_STATES.clear();
		BURNING_PASSION_HUD_NOTIFICATION_STATES.clear();
		BURNING_PASSION_PENDING_MELEE_IMPACTS.clear();
		OVERRIDE_METEOR_STATES.clear();
		IGNITION_COOLDOWN_END_TICK.clear();
		SEARING_DASH_COOLDOWN_END_TICK.clear();
		CINDER_MARK_COOLDOWN_END_TICK.clear();
		ENGINE_HEART_COOLDOWN_END_TICK.clear();
		OVERRIDE_COOLDOWN_END_TICK.clear();
		TEST_MODE_PLAYERS.clear();
		TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.clear();
		TILL_DEATH_DO_US_PART_STATES.clear();
		TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.clear();
		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.clear();
		TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK.clear();
		MANIPULATION_DRAIN_BUFFER.clear();
		GreedDomainRuntime.onServerStarted(server);
		loadPersistedDomainRuntimeState(server);
	}

	static void onServerStopping(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			MagicPlayerData.clearDomainClashUi(player);
			MagicPlayerData.clearDomainTimer(player);
		}

		DOMAIN_CLASHES_BY_OWNER.clear();
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.clear();
		DOMAIN_CLASH_PENDING_DAMAGE.clear();
		MAGIC_DAMAGE_PENDING_ATTACKER.clear();
		persistDomainRuntimeState(server);
		domainRuntimePersistentState = null;
		cachedBeaconCoreAnchor = null;
		cachedBeaconCoreAnchorTick = Integer.MIN_VALUE;
		TEST_MODE_PLAYERS.clear();
		MARTYRS_FLAME_PASSIVE_ENABLED.clear();
		MARTYRS_FLAME_COOLDOWN_END_TICK.clear();
		MARTYRS_FLAME_DRAIN_BUFFER.clear();
		MARTYRS_FLAME_BURNING_TARGETS.clear();
		BURNING_PASSION_IGNITION_STATES.clear();
		BURNING_PASSION_AURA_FIRE_TARGETS.clear();
		BURNING_PASSION_SELF_FIRE_TARGETS.clear();
		BURNING_PASSION_SEARING_DASH_STATES.clear();
		BURNING_PASSION_TRAIL_LINES.clear();
		BURNING_PASSION_CINDER_MARK_ARMED_STATES.clear();
		BURNING_PASSION_CINDER_MARKS.clear();
		BURNING_PASSION_ENGINE_HEART_STATES.clear();
		BURNING_PASSION_HUD_NOTIFICATION_STATES.clear();
		BURNING_PASSION_PENDING_MELEE_IMPACTS.clear();
		OVERRIDE_METEOR_STATES.clear();
		IGNITION_COOLDOWN_END_TICK.clear();
		SEARING_DASH_COOLDOWN_END_TICK.clear();
		CINDER_MARK_COOLDOWN_END_TICK.clear();
		ENGINE_HEART_COOLDOWN_END_TICK.clear();
		OVERRIDE_COOLDOWN_END_TICK.clear();
		TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.clear();
		TILL_DEATH_DO_US_PART_STATES.clear();
		TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.clear();
		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.clear();
		TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK.clear();
		MANIPULATION_DRAIN_BUFFER.clear();
		GREED_DOMAIN_COOLDOWN_END_TICK.clear();
		GreedDomainRuntime.onServerStopping(server);
		DOMAIN_PENDING_RETURNS.clear();
	}

	static void loadPersistedDomainRuntimeState(MinecraftServer server) {
		DomainRuntimePersistentState persistentState = domainRuntimePersistentState(server);
		if (persistentState == null) {
			return;
		}

		deserializeDomainRuntimeState(server, persistentState.getDataCopy());
		cleanupPersistedDomainsAfterRestart(server);
	}

	static void persistDomainRuntimeState(MinecraftServer server) {
		DomainRuntimePersistentState persistentState = domainRuntimePersistentState(server);
		if (persistentState == null) {
			return;
		}

		persistentState.setData(serializeDomainRuntimeState(server));
	}

	static void cleanupPersistedDomainsAfterRestart(MinecraftServer server) {
		if (DOMAIN_EXPANSIONS.isEmpty()) {
			return;
		}

		for (DomainExpansionState state : new ArrayList<>(DOMAIN_EXPANSIONS.values())) {
			restoreDomainExpansion(server, state);
		}
		DOMAIN_EXPANSIONS.clear();
		DOMAIN_CLASHES_BY_OWNER.clear();
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.clear();
		persistDomainRuntimeState(server);
	}

	static DomainRuntimePersistentState domainRuntimePersistentState(MinecraftServer server) {
		if (domainRuntimePersistentState != null) {
			return domainRuntimePersistentState;
		}

		ServerWorld overworld = server.getOverworld();
		if (overworld == null) {
			return null;
		}

		domainRuntimePersistentState = overworld.getPersistentStateManager().getOrCreate(DomainRuntimePersistentState.TYPE);
		return domainRuntimePersistentState;
	}

	static NbtCompound serializeDomainRuntimeState(MinecraftServer server) {
		NbtCompound root = new NbtCompound();
		NbtList domains = new NbtList();
		int currentTick = server.getTicks();

		for (Map.Entry<UUID, DomainExpansionState> entry : DOMAIN_EXPANSIONS.entrySet()) {
			DomainExpansionState state = entry.getValue();
			NbtCompound domainTag = new NbtCompound();
			domainTag.putString("owner", entry.getKey().toString());
			domainTag.putString("dimension", state.dimension.getValue().toString());
			domainTag.putString("ability", state.ability.id());
			int remainingTicks = state.expiresTick == Integer.MAX_VALUE ? -1 : Math.max(0, state.expiresTick - currentTick);
			domainTag.putInt("expiresRemainingTicks", remainingTicks);
			int effectRemainingTicks = state.effectEndTick == Integer.MAX_VALUE ? -1 : Math.max(0, state.effectEndTick - currentTick);
			domainTag.putInt("effectEndRemainingTicks", effectRemainingTicks);
			int nextPulseRemainingTicks = state.nextFrostPulseTick == Integer.MAX_VALUE ? -1 : Math.max(0, state.nextFrostPulseTick - currentTick);
			domainTag.putInt("nextFrostPulseRemainingTicks", nextPulseRemainingTicks);
			domainTag.putInt("frostPulseCount", state.frostPulseCount);
			domainTag.putDouble("centerX", state.centerX);
			domainTag.putDouble("centerZ", state.centerZ);
			domainTag.putInt("baseY", state.baseY);
			domainTag.putInt("radius", state.radius);
			domainTag.putInt("height", state.height);
			domainTag.putInt("innerRadius", state.innerRadius);
			domainTag.putInt("innerHeight", state.innerHeight);
			domainTag.putBoolean("clashOccurred", state.clashOccurred);
			domainTag.putDouble("cooldownMultiplier", state.cooldownMultiplier);

			NbtList savedBlocks = new NbtList();
			for (Map.Entry<BlockPos, DomainSavedBlockState> savedEntry : state.savedBlocks.entrySet()) {
				NbtCompound blockTag = new NbtCompound();
				blockTag.putLong("pos", savedEntry.getKey().asLong());
				blockTag.put("state", NbtHelper.fromBlockState(savedEntry.getValue().blockState));
				if (savedEntry.getValue().blockEntityNbt != null) {
					blockTag.put("blockEntity", savedEntry.getValue().blockEntityNbt.copy());
				}
				savedBlocks.add(blockTag);
			}
			domainTag.put("savedBlocks", savedBlocks);

			NbtList protectedShell = new NbtList();
			for (Map.Entry<BlockPos, BlockState> shellEntry : state.protectedShellStates.entrySet()) {
				NbtCompound blockTag = new NbtCompound();
				blockTag.putLong("pos", shellEntry.getKey().asLong());
				blockTag.put("state", NbtHelper.fromBlockState(shellEntry.getValue()));
				protectedShell.add(blockTag);
			}
			domainTag.put("protectedShell", protectedShell);

			NbtList capturedEntities = new NbtList();
			for (DomainCapturedEntityState captured : state.capturedEntities.values()) {
				NbtCompound capturedTag = new NbtCompound();
				capturedTag.putString("entityId", captured.entityId.toString());
				capturedTag.putBoolean("playerEntity", captured.playerEntity);
				capturedTag.putDouble("x", captured.position.x);
				capturedTag.putDouble("y", captured.position.y);
				capturedTag.putDouble("z", captured.position.z);
				capturedTag.putFloat("yaw", captured.yaw);
				capturedTag.putFloat("pitch", captured.pitch);
				Vec3d lastSafePos = captured.lastSafePos == null ? captured.position : captured.lastSafePos;
				capturedTag.putDouble("lastSafeX", lastSafePos.x);
				capturedTag.putDouble("lastSafeY", lastSafePos.y);
				capturedTag.putDouble("lastSafeZ", lastSafePos.z);
				capturedTag.putFloat("lastSafeYaw", captured.lastSafeYaw);
				capturedTag.putFloat("lastSafePitch", captured.lastSafePitch);
				capturedEntities.add(capturedTag);
			}
			domainTag.put("capturedEntities", capturedEntities);
			domains.add(domainTag);
		}

		root.put(DOMAIN_PERSISTENCE_DOMAINS_KEY, domains);

		NbtList loveCooldowns = new NbtList();
		for (Map.Entry<UUID, Integer> entry : LOVE_DOMAIN_COOLDOWN_END_TICK.entrySet()) {
			int remainingTicks = Math.max(0, entry.getValue() - currentTick);
			if (remainingTicks <= 0) {
				continue;
			}

			NbtCompound cooldownTag = new NbtCompound();
			cooldownTag.putString("owner", entry.getKey().toString());
			cooldownTag.putInt("remainingTicks", remainingTicks);
			loveCooldowns.add(cooldownTag);
		}
		root.put(DOMAIN_PERSISTENCE_LOVE_COOLDOWNS_KEY, loveCooldowns);

		NbtList frostCooldowns = new NbtList();
		for (Map.Entry<UUID, Integer> entry : FROST_DOMAIN_COOLDOWN_END_TICK.entrySet()) {
			int remainingTicks = Math.max(0, entry.getValue() - currentTick);
			if (remainingTicks <= 0) {
				continue;
			}

			NbtCompound cooldownTag = new NbtCompound();
			cooldownTag.putString("owner", entry.getKey().toString());
			cooldownTag.putInt("remainingTicks", remainingTicks);
			frostCooldowns.add(cooldownTag);
		}
		root.put(DOMAIN_PERSISTENCE_FROST_COOLDOWNS_KEY, frostCooldowns);

		NbtList pendingReturns = new NbtList();
		for (Map.Entry<UUID, DomainPendingReturnState> entry : DOMAIN_PENDING_RETURNS.entrySet()) {
			DomainPendingReturnState state = entry.getValue();
			NbtCompound returnTag = new NbtCompound();
			returnTag.putString("playerId", entry.getKey().toString());
			returnTag.putString("dimension", state.dimension.getValue().toString());
			returnTag.putDouble("x", state.x);
			returnTag.putDouble("y", state.y);
			returnTag.putDouble("z", state.z);
			returnTag.putFloat("yaw", state.yaw);
			returnTag.putFloat("pitch", state.pitch);
			pendingReturns.add(returnTag);
		}
		root.put(DOMAIN_PERSISTENCE_PENDING_RETURNS_KEY, pendingReturns);

		return root;
	}

	static void deserializeDomainRuntimeState(MinecraftServer server, NbtCompound root) {
		DOMAIN_EXPANSIONS.clear();
		DOMAIN_CLASHES_BY_OWNER.clear();
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.clear();
		FROST_DOMAIN_COOLDOWN_END_TICK.clear();
		LOVE_DOMAIN_COOLDOWN_END_TICK.clear();
		DOMAIN_PENDING_RETURNS.clear();
		int currentTick = server.getTicks();

		NbtList domains = root.getListOrEmpty(DOMAIN_PERSISTENCE_DOMAINS_KEY);
		for (NbtElement element : domains) {
			if (!(element instanceof NbtCompound domainTag)) {
				continue;
			}

			UUID ownerId = readUuid(domainTag, "owner");
			if (ownerId == null) {
				continue;
			}

			Identifier dimensionId = Identifier.tryParse(domainTag.getString("dimension", ""));
			if (dimensionId == null) {
				continue;
			}

			RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
			if (server.getWorld(dimension) == null) {
				continue;
			}

			MagicAbility ability = MagicAbility.fromId(domainTag.getString("ability", ""));
			if (!isDomainExpansion(ability)) {
				continue;
			}

			int expiresRemainingTicks = domainTag.getInt("expiresRemainingTicks", 0);
			int expiresTick = expiresRemainingTicks < 0 ? Integer.MAX_VALUE : currentTick + Math.max(0, expiresRemainingTicks);
			int effectEndRemainingTicks = domainTag.getInt("effectEndRemainingTicks", expiresRemainingTicks);
			int effectEndTick = effectEndRemainingTicks < 0 ? Integer.MAX_VALUE : currentTick + Math.max(0, effectEndRemainingTicks);
			int nextFrostPulseRemainingTicks = domainTag.getInt("nextFrostPulseRemainingTicks", -1);
			int nextFrostPulseTick = nextFrostPulseRemainingTicks < 0 ? Integer.MAX_VALUE : currentTick + Math.max(0, nextFrostPulseRemainingTicks);
			int frostPulseCount = Math.max(0, domainTag.getInt("frostPulseCount", 0));

			double centerX = domainTag.getDouble("centerX", 0.0);
			double centerZ = domainTag.getDouble("centerZ", 0.0);
			int baseY = domainTag.getInt("baseY", 0);
			int radius = Math.max(1, domainTag.getInt("radius", Math.max(1, DOMAIN_EXPANSION_RADIUS)));
			int height = Math.max(1, domainTag.getInt("height", Math.max(1, DOMAIN_EXPANSION_HEIGHT)));
			int innerRadius = Math.max(0, domainTag.getInt("innerRadius", 0));
			int innerHeight = Math.max(1, domainTag.getInt("innerHeight", 1));
			boolean clashOccurred = domainTag.getBoolean("clashOccurred", false);
			double cooldownMultiplier = MathHelper.clamp(domainTag.getDouble("cooldownMultiplier", 1.0), 0.0, 10.0);

			Map<BlockPos, DomainSavedBlockState> savedBlocks = new HashMap<>();
			for (NbtElement savedElement : domainTag.getListOrEmpty("savedBlocks")) {
				if (!(savedElement instanceof NbtCompound blockTag)) {
					continue;
				}

				BlockPos pos = BlockPos.fromLong(blockTag.getLong("pos", 0L));
				BlockState blockState = readPersistedBlockState(blockTag.getCompoundOrEmpty("state"), Blocks.AIR.getDefaultState());
				NbtCompound blockEntityNbt = blockTag.getCompound("blockEntity").map(NbtCompound::copy).orElse(null);
				savedBlocks.put(pos, new DomainSavedBlockState(blockState, blockEntityNbt));
			}

			Map<BlockPos, BlockState> protectedShellStates = new HashMap<>();
			for (NbtElement shellElement : domainTag.getListOrEmpty("protectedShell")) {
				if (!(shellElement instanceof NbtCompound blockTag)) {
					continue;
				}

				BlockPos pos = BlockPos.fromLong(blockTag.getLong("pos", 0L));
				BlockState blockState = readPersistedBlockState(blockTag.getCompoundOrEmpty("state"), DOMAIN_SHELL_BLOCK_STATE);
				protectedShellStates.put(pos, blockState);
			}

			Map<UUID, DomainCapturedEntityState> capturedEntities = new HashMap<>();
			for (NbtElement capturedElement : domainTag.getListOrEmpty("capturedEntities")) {
				if (!(capturedElement instanceof NbtCompound capturedTag)) {
					continue;
				}

				UUID entityId = readUuid(capturedTag, "entityId");
				if (entityId == null) {
					continue;
				}

				boolean playerEntity = capturedTag.getBoolean("playerEntity", false);
				Vec3d position = new Vec3d(
					capturedTag.getDouble("x", 0.0),
					capturedTag.getDouble("y", 0.0),
					capturedTag.getDouble("z", 0.0)
				);
				float yaw = capturedTag.getFloat("yaw", 0.0F);
				float pitch = capturedTag.getFloat("pitch", 0.0F);
				DomainCapturedEntityState capturedState = new DomainCapturedEntityState(entityId, playerEntity, position, yaw, pitch);
				capturedState.lastSafePos = new Vec3d(
					capturedTag.getDouble("lastSafeX", position.x),
					capturedTag.getDouble("lastSafeY", position.y),
					capturedTag.getDouble("lastSafeZ", position.z)
				);
				capturedState.lastSafeYaw = capturedTag.getFloat("lastSafeYaw", yaw);
				capturedState.lastSafePitch = capturedTag.getFloat("lastSafePitch", pitch);
				capturedEntities.put(entityId, capturedState);
			}

			DOMAIN_EXPANSIONS.put(
				ownerId,
				new DomainExpansionState(
					dimension,
					ability,
					expiresTick,
					effectEndTick,
					centerX,
					centerZ,
					baseY,
					radius,
					height,
					innerRadius,
					innerHeight,
					currentTick - DOMAIN_CLASH_SIMULTANEOUS_WINDOW_TICKS - 1,
					clashOccurred,
					cooldownMultiplier,
					frostPulseCount,
					nextFrostPulseTick,
					savedBlocks,
					protectedShellStates,
					capturedEntities
				)
			);
		}

		NbtList loveCooldowns = root.getListOrEmpty(DOMAIN_PERSISTENCE_LOVE_COOLDOWNS_KEY);
		for (NbtElement element : loveCooldowns) {
			if (!(element instanceof NbtCompound cooldownTag)) {
				continue;
			}

			UUID ownerId = readUuid(cooldownTag, "owner");
			if (ownerId == null) {
				continue;
			}

			int remainingTicks = cooldownTag.getInt("remainingTicks", 0);
			if (remainingTicks <= 0) {
				continue;
			}

			LOVE_DOMAIN_COOLDOWN_END_TICK.put(ownerId, currentTick + remainingTicks);
		}

		NbtList frostCooldowns = root.getListOrEmpty(DOMAIN_PERSISTENCE_FROST_COOLDOWNS_KEY);
		for (NbtElement element : frostCooldowns) {
			if (!(element instanceof NbtCompound cooldownTag)) {
				continue;
			}

			UUID ownerId = readUuid(cooldownTag, "owner");
			if (ownerId == null) {
				continue;
			}

			int remainingTicks = cooldownTag.getInt("remainingTicks", 0);
			if (remainingTicks <= 0) {
				continue;
			}

			FROST_DOMAIN_COOLDOWN_END_TICK.put(ownerId, currentTick + remainingTicks);
		}

		NbtList pendingReturns = root.getListOrEmpty(DOMAIN_PERSISTENCE_PENDING_RETURNS_KEY);
		for (NbtElement element : pendingReturns) {
			if (!(element instanceof NbtCompound returnTag)) {
				continue;
			}

			UUID playerId = readUuid(returnTag, "playerId");
			if (playerId == null) {
				continue;
			}

			Identifier dimensionId = Identifier.tryParse(returnTag.getString("dimension", ""));
			if (dimensionId == null) {
				continue;
			}

			RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
			if (server.getWorld(dimension) == null) {
				continue;
			}

			DOMAIN_PENDING_RETURNS.put(
				playerId,
				new DomainPendingReturnState(
					dimension,
					returnTag.getDouble("x", 0.0),
					returnTag.getDouble("y", 0.0),
					returnTag.getDouble("z", 0.0),
					returnTag.getFloat("yaw", 0.0F),
					returnTag.getFloat("pitch", 0.0F)
				)
			);
		}
	}

	static BlockState readPersistedBlockState(NbtCompound stateTag, BlockState fallback) {
		if (stateTag.isEmpty()) {
			return fallback;
		}

		try {
			return NbtHelper.toBlockState(BLOCK_ENTRY_LOOKUP, stateTag);
		} catch (Exception exception) {
			return fallback;
		}
	}

	static UUID readUuid(NbtCompound compound, String key) {
		String raw = compound.getString(key, "");
		if (raw.isBlank()) {
			return null;
		}

		try {
			return UUID.fromString(raw);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}
}


