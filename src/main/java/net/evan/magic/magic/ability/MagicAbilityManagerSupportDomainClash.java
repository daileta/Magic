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


abstract class MagicAbilityManagerSupportDomainClash extends MagicAbilityManagerSupportDomainExpansion {
	public static void onDomainClashInput(ServerPlayerEntity player, int keyCode) {
	}

	static void resolveDomainClash(MinecraftServer server, UUID ownerId, UUID winnerId, int currentTick) {
		DomainClashState clash = DOMAIN_CLASHES_BY_OWNER.remove(ownerId);
		if (clash == null) {
			return;
		}

		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(clash.ownerId);
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(clash.challengerId);

		DomainExpansionState domain = DOMAIN_EXPANSIONS.get(ownerId);
		ServerPlayerEntity owner = server.getPlayerManager().getPlayer(clash.ownerId);
		ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(clash.challengerId);

		if (owner != null) {
			MagicPlayerData.clearDomainClashUi(owner);
		}
		if (challenger != null) {
			MagicPlayerData.clearDomainClashUi(challenger);
		}

		if (domain == null) {
			return;
		}

		ServerWorld world = server.getWorld(domain.dimension);
		if (world == null) {
			return;
		}

		boolean challengerWon = winnerId.equals(clash.challengerId);

		if (challengerWon) {
			applyDomainVisualForAbility(world, domain, clash.challengerAbility);
			resetDomainTimingForAbility(domain, clash.challengerAbility, currentTick);
			domain.cooldownMultiplier = DOMAIN_CLASH_POST_CLASH_COOLDOWN_MULTIPLIER;
			DOMAIN_EXPANSIONS.remove(ownerId);
			DOMAIN_EXPANSIONS.put(clash.challengerId, domain);
			ASTRAL_CATACLYSM_DOMAIN_STATES.remove(ownerId);
			if (clash.challengerAbility == MagicAbility.ASTRAL_CATACLYSM) {
				ASTRAL_CATACLYSM_DOMAIN_STATES.put(
					clash.challengerId,
					new ConstellationDomainState(ConstellationDomainPhase.CHARGING, currentTick + ASTRAL_CATACLYSM_CHARGE_DURATION_TICKS)
				);
			} else {
				ASTRAL_CATACLYSM_DOMAIN_STATES.remove(clash.challengerId);
			}
			applyDomainCasterShutdown(
				clash.ownerId,
				clash.ownerAbility,
				server,
				currentTick,
				DOMAIN_CLASH_POST_CLASH_COOLDOWN_MULTIPLIER
			);
			if (owner != null) {
				setActiveAbility(owner, MagicAbility.NONE);
				applyDomainClashLoserManaPenalty(owner);
				owner.sendMessage(Text.translatable("message.magic.domain.clash.lost"), true);
			}
			if (challenger != null) {
				setActiveAbility(challenger, clash.challengerAbility);
				challenger.sendMessage(Text.translatable("message.magic.domain.clash.won"), true);
			}
		} else {
			applyDomainVisualForAbility(world, domain, clash.ownerAbility);
			domain.cooldownMultiplier = DOMAIN_CLASH_POST_CLASH_COOLDOWN_MULTIPLIER;
			applyDomainCasterShutdown(
				clash.challengerId,
				clash.challengerAbility,
				server,
				currentTick,
				DOMAIN_CLASH_POST_CLASH_COOLDOWN_MULTIPLIER
			);
			if (challenger != null) {
				setActiveAbility(challenger, MagicAbility.NONE);
				applyDomainClashLoserManaPenalty(challenger);
				challenger.sendMessage(Text.translatable("message.magic.domain.clash.lost"), true);
			}
			if (owner != null) {
				setActiveAbility(owner, clash.ownerAbility);
				owner.sendMessage(Text.translatable("message.magic.domain.clash.won"), true);
			}
		}

		persistDomainRuntimeState(server);
	}

	static void clearDomainClashUiFor(DomainClashState clash, MinecraftServer server) {
		ServerPlayerEntity owner = server.getPlayerManager().getPlayer(clash.ownerId);
		ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(clash.challengerId);
		if (owner != null) {
			MagicPlayerData.clearDomainClashUi(owner);
		}
		if (challenger != null) {
			MagicPlayerData.clearDomainClashUi(challenger);
		}
	}

	static void cancelDomainClash(UUID ownerId, MinecraftServer server) {
		DomainClashState clash = DOMAIN_CLASHES_BY_OWNER.remove(ownerId);
		if (clash == null) {
			return;
		}

		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(clash.ownerId);
		DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(clash.challengerId);
		clearDomainClashUiFor(clash, server);
	}

	static void cancelDomainClashParticipant(UUID playerId, MinecraftServer server) {
		UUID ownerId = DOMAIN_CLASH_OWNER_BY_PARTICIPANT.get(playerId);
		if (ownerId == null) {
			return;
		}

		DomainClashState clash = DOMAIN_CLASHES_BY_OWNER.get(ownerId);
		if (clash == null) {
			DOMAIN_CLASH_OWNER_BY_PARTICIPANT.remove(playerId);
			return;
		}

		UUID winnerId = playerId.equals(clash.ownerId) ? clash.challengerId : clash.ownerId;
		resolveDomainClash(server, ownerId, winnerId, server.getTicks());
	}

	static void applyDomainClashLoserManaPenalty(ServerPlayerEntity loser) {
		int manaDrain = (int) Math.ceil(MagicPlayerData.MAX_MANA * (DOMAIN_CLASH_LOSER_MANA_DRAIN_PERCENT / 100.0));
		MagicPlayerData.setMana(loser, Math.max(0, MagicPlayerData.getMana(loser) - manaDrain));
	}

	static int domainClashTitleDurationTicks() {
		return DOMAIN_CLASH_TITLE_FADE_IN_TICKS + DOMAIN_CLASH_TITLE_STAY_TICKS + DOMAIN_CLASH_TITLE_FADE_OUT_TICKS;
	}

	static int domainClashIntroLockTicks() {
		return domainClashTitleDurationTicks() + DOMAIN_CLASH_INSTRUCTIONS_DURATION_TICKS + DOMAIN_CLASH_INSTRUCTIONS_FADE_OUT_TICKS;
	}

	static boolean isDomainClashFrozen(DomainClashState clash, int currentTick) {
		return currentTick < clash.combatStartTick;
	}

	static boolean isDomainClashCombatActive(DomainClashState clash, int currentTick) {
		return currentTick >= clash.combatStartTick;
	}

	static int domainClashProgressPercent(double damageDealt) {
		return MathHelper.clamp((int) Math.round((damageDealt / DOMAIN_CLASH_DAMAGE_TO_WIN) * 100.0), 0, 100);
	}

	static int domainClashInstructionVisibilityPercent(DomainClashState clash, int currentTick) {
		if (currentTick < clash.titleEndTick || currentTick >= clash.combatStartTick) {
			return 0;
		}

		if (currentTick < clash.instructionsFadeStartTick || DOMAIN_CLASH_INSTRUCTIONS_FADE_OUT_TICKS <= 0) {
			return 100;
		}

		int remainingFadeTicks = Math.max(0, clash.combatStartTick - currentTick);
		return MathHelper.clamp(
			(int) Math.round(remainingFadeTicks * 100.0 / Math.max(1, DOMAIN_CLASH_INSTRUCTIONS_FADE_OUT_TICKS)),
			0,
			100
		);
	}

	static DomainClashState domainClashStateForParticipant(UUID playerId) {
		UUID ownerId = DOMAIN_CLASH_OWNER_BY_PARTICIPANT.get(playerId);
		if (ownerId == null) {
			return null;
		}

		return DOMAIN_CLASHES_BY_OWNER.get(ownerId);
	}

	static boolean addDomainClashDamage(DomainClashState clash, UUID attackerId, float amount, MinecraftServer server, int currentTick) {
		UUID winnerId = null;
		if (attackerId.equals(clash.ownerId)) {
			clash.ownerDamageDealt = Math.min(DOMAIN_CLASH_DAMAGE_TO_WIN, clash.ownerDamageDealt + amount);
			if (clash.ownerDamageDealt >= DOMAIN_CLASH_DAMAGE_TO_WIN) {
				winnerId = clash.ownerId;
			}
		} else if (attackerId.equals(clash.challengerId)) {
			clash.challengerDamageDealt = Math.min(DOMAIN_CLASH_DAMAGE_TO_WIN, clash.challengerDamageDealt + amount);
			if (clash.challengerDamageDealt >= DOMAIN_CLASH_DAMAGE_TO_WIN) {
				winnerId = clash.challengerId;
			}
		}

		if (winnerId != null) {
			resolveDomainClash(server, clash.ownerId, winnerId, currentTick);
			return true;
		}

		return false;
	}

	static void captureDomainClashDamage(ServerPlayerEntity damagedPlayer, DamageSource source, float amount) {
		DomainClashState clash = domainClashStateForParticipant(damagedPlayer.getUuid());
		if (clash == null) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(damagedPlayer.getUuid());
			return;
		}

		MinecraftServer server = damagedPlayer.getEntityWorld().getServer();
		if (server == null || !isDomainClashCombatActive(clash, server.getTicks())) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(damagedPlayer.getUuid());
			return;
		}

		UUID attackerId = resolveDomainClashAttackerId(clash, damagedPlayer.getUuid(), source, server);
		if (attackerId == null) {
			DOMAIN_CLASH_PENDING_DAMAGE.remove(damagedPlayer.getUuid());
			return;
		}

		DOMAIN_CLASH_PENDING_DAMAGE.put(
			damagedPlayer.getUuid(),
			new DomainClashPendingDamageState(attackerId, damagedPlayer.getHealth(), amount)
		);
	}

	static void onPlayerAfterDamage(ServerPlayerEntity damagedPlayer, DamageSource source, float damageTaken) {
		GreedRuntime.onPlayerAfterDamage(damagedPlayer, source, damageTaken);
		DomainClashPendingDamageState pendingDamage = DOMAIN_CLASH_PENDING_DAMAGE.remove(damagedPlayer.getUuid());
		if (pendingDamage == null) {
			return;
		}

		DomainClashState clash = domainClashStateForParticipant(damagedPlayer.getUuid());
		if (clash == null) {
			return;
		}

		MinecraftServer server = damagedPlayer.getEntityWorld().getServer();
		if (server == null || !isDomainClashCombatActive(clash, server.getTicks())) {
			return;
		}

		UUID attackerId = resolveDomainClashAttackerId(clash, damagedPlayer.getUuid(), source, server);
		if (attackerId == null || !attackerId.equals(pendingDamage.attackerId)) {
			return;
		}

		float clashDamage = Math.max(0.0F, pendingDamage.healthBefore - damagedPlayer.getHealth());
		if (clashDamage <= 0.0F) {
			clashDamage = Math.max(0.0F, damageTaken);
		}
		if (clashDamage <= 0.0F) {
			clashDamage = Math.max(0.0F, pendingDamage.incomingAmount);
		}
		if (clashDamage <= 0.0F) {
			return;
		}

		addDomainClashDamage(clash, attackerId, clashDamage, server, server.getTicks());
	}

	public static void onLivingEntityDeathProtectorTriggered(LivingEntity entity) {
		if (entity == null) {
			return;
		}
	}

	public static void onPlayerDeathProtectorTriggered(ServerPlayerEntity player) {
		onLivingEntityDeathProtectorTriggered(player);
	}

	static void onLivingEntityAfterDamage(LivingEntity entity, DamageSource source, float damageTaken) {
		if (entity == null || !isMagicTargetableEntity(entity) || source == null || damageTaken <= 0.0F || entity.getEntityWorld().isClient()) {
			return;
		}

		ServerPlayerEntity anyAttacker = attackingPlayerFrom(source);
		if (anyAttacker != null && anyAttacker != entity && anyAttacker.isAlive() && !anyAttacker.isSpectator()) {
			recordCelestialGeminiHit(anyAttacker, entity, damageTaken);
		}

		ServerPlayerEntity attacker = directMeleePlayerAttackerFrom(source);
		if (attacker == null || attacker == entity || !attacker.isAlive() || attacker.isSpectator()) {
			return;
		}

		tryApplyBurningPassionAttackEffects(attacker, entity);
		if (!isValidComedicAssistantTarget(entity)) {
			return;
		}

		tryTriggerComedicAssistant(attacker, entity, damageTaken);
		tryTriggerPlusUltraHit(attacker, entity, damageTaken);
	}

	static ServerPlayerEntity directMeleePlayerAttackerFrom(DamageSource source) {
		Entity attacker = source.getAttacker();
		Entity directSource = source.getSource();
		if (attacker instanceof ServerPlayerEntity attackerPlayer && directSource == attackerPlayer && attackerPlayer.isAlive() && !attackerPlayer.isSpectator()) {
			return attackerPlayer;
		}
		if (directSource instanceof ServerPlayerEntity sourcePlayer && (attacker == null || attacker == sourcePlayer) && sourcePlayer.isAlive() && !sourcePlayer.isSpectator()) {
			return sourcePlayer;
		}
		return null;
	}

	static UUID resolveDomainClashAttackerId(
		DomainClashState clash,
		UUID damagedId,
		DamageSource source,
		MinecraftServer server
	) {
		Entity attacker = source.getAttacker();
		UUID attackerId = null;
		if (attacker instanceof ServerPlayerEntity attackerPlayer && attackerPlayer.isAlive() && !attackerPlayer.isSpectator()) {
			attackerId = attackerPlayer.getUuid();
		}

		if (attackerId == null && source.getSource() instanceof ServerPlayerEntity sourcePlayer && sourcePlayer.isAlive() && !sourcePlayer.isSpectator()) {
			attackerId = sourcePlayer.getUuid();
		}

		if (attackerId == null) {
			UUID pendingAttackerId = MAGIC_DAMAGE_PENDING_ATTACKER.get(damagedId);
			if (pendingAttackerId != null) {
				ServerPlayerEntity pendingAttacker = server.getPlayerManager().getPlayer(pendingAttackerId);
				if (pendingAttacker != null && pendingAttacker.isAlive() && !pendingAttacker.isSpectator()) {
					attackerId = pendingAttackerId;
				}
			}
		}

		if (attackerId == null) {
			return null;
		}

		boolean ownerHitChallenger = attackerId.equals(clash.ownerId) && damagedId.equals(clash.challengerId);
		boolean challengerHitOwner = attackerId.equals(clash.challengerId) && damagedId.equals(clash.ownerId);
		return ownerHitChallenger || challengerHitOwner ? attackerId : null;
	}

	static void lockDomainClashParticipant(ServerPlayerEntity player, Vec3d lockedPos, Vec3d opponentEyePos) {
		float yaw = player.getYaw();
		float pitch = player.getPitch();
		if (DOMAIN_CLASH_FORCE_LOOK) {
			float[] facing = computeFacingAngles(player, opponentEyePos);
			yaw = facing[0];
			pitch = facing[1];
		}

		teleportDomainEntity(player, lockedPos.x, lockedPos.y, lockedPos.z, yaw, pitch);
	}

	static Vec3d resolveGreedDomainClashLockedPosition(
		ServerWorld world,
		DomainExpansionState state,
		LivingEntity participant,
		Vec3d preferredPos
	) {
		double horizontalDistanceSq = squaredHorizontalDistance(preferredPos.x, preferredPos.z, state.centerX, state.centerZ);
		if (
			isInsideDomainInterior(horizontalDistanceSq, preferredPos.y - state.baseY, state.innerRadius, state.innerHeight)
				&& isSafeDomainTeleportPosition(world, participant, preferredPos.x, preferredPos.y, preferredPos.z)
		) {
			return preferredPos;
		}

		Vec3d safePos = findNearestSafeDomainOccupantPosition(
			world,
			participant,
			state.centerX,
			state.centerZ,
			state.baseY,
			state.innerRadius,
			state.innerHeight,
			preferredPos.x,
			preferredPos.z
		);
		if (safePos != null) {
			return safePos;
		}

		safePos = findNearestSafeDomainOccupantPosition(
			world,
			participant,
			state.centerX,
			state.centerZ,
			state.baseY,
			state.innerRadius,
			state.innerHeight,
			state.centerX,
			state.centerZ
		);
		return safePos == null ? preferredPos : safePos;
	}

	static void spawnDomainClashParticles(ServerWorld world, DomainExpansionState state, int particlesPerTick) {
		if (particlesPerTick <= 0) {
			return;
		}

		double centerY = state.baseY + (state.height * 0.5);
		world.spawnParticles(
			ParticleTypes.END_ROD,
			state.centerX,
			centerY,
			state.centerZ,
			particlesPerTick,
			state.innerRadius * 0.65,
			state.height * 0.35,
			state.innerRadius * 0.65,
			0.02
		);
	}

	static void showDomainClashTitle(ServerPlayerEntity player) {
		player.networkHandler.sendPacket(
			new TitleFadeS2CPacket(DOMAIN_CLASH_TITLE_FADE_IN_TICKS, DOMAIN_CLASH_TITLE_STAY_TICKS, DOMAIN_CLASH_TITLE_FADE_OUT_TICKS)
		);
		player.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("title.magic.domain_clash").formatted(Formatting.BOLD, Formatting.RED)));
	}

	static void applySplitDomainInteriorVisuals(
		ServerWorld world,
		DomainExpansionState state,
		MagicAbility ownerAbility,
		MagicAbility challengerAbility
	) {
		int centerX = MathHelper.floor(state.centerX);
		int centerZ = MathHelper.floor(state.centerZ);
		int centerLineX = MathHelper.floor(state.centerX);
		int modulo = Math.max(2, DOMAIN_CLASH_SPLIT_PATTERN_MODULO);

		for (BlockPos pos : state.savedBlocks.keySet()) {
			int relativeY = pos.getY() - state.baseY;
			if (relativeY <= 0 || relativeY > state.height) {
				continue;
			}

			int horizontalDistanceSq = horizontalDistanceSq(pos.getX(), centerX, pos.getZ(), centerZ);
			if (!isInsideDomainDome(horizontalDistanceSq, relativeY, state.radius, state.height)) {
				continue;
			}

			boolean shell = relativeY == 0 || !isInsideDomainDome(horizontalDistanceSq, relativeY, state.innerRadius, state.innerHeight);
			if (shell) {
				continue;
			}

			MagicAbility targetAbility;
			if (pos.getX() < centerLineX) {
				targetAbility = ownerAbility;
			} else if (pos.getX() > centerLineX) {
				targetAbility = challengerAbility;
			} else {
				// Split the center seam deterministically when x is exactly on the divide.
				targetAbility = Math.floorMod(pos.getZ() + pos.getY(), modulo) == 0 ? challengerAbility : ownerAbility;
			}
			BlockState targetState = resolveDomainTargetState(
				targetAbility,
				false,
				centerX,
				centerZ,
				state.baseY,
				state.radius,
				state.height,
				state.innerRadius,
				state.innerHeight,
				pos
			);

			if (hasProtectedDecorationEntity(world, pos)) {
				continue;
			}
			setDomainBlockState(world, pos, targetState, DOMAIN_BLOCK_PLACE_FLAGS);
		}
	}

	static void applyDomainVisualForAbility(ServerWorld world, DomainExpansionState state, MagicAbility ability) {
		int centerX = MathHelper.floor(state.centerX);
		int centerZ = MathHelper.floor(state.centerZ);
		Map<BlockPos, BlockState> refreshedShell = new HashMap<>();
		for (Map.Entry<BlockPos, DomainSavedBlockState> savedEntry : state.savedBlocks.entrySet()) {
			BlockPos pos = savedEntry.getKey();
			int relativeY = pos.getY() - state.baseY;
			if (relativeY < 0 || relativeY > state.height) {
				continue;
			}

			int horizontalDistanceSq = horizontalDistanceSq(pos.getX(), centerX, pos.getZ(), centerZ);
			if (relativeY > 0 && !isInsideDomainDome(horizontalDistanceSq, relativeY, state.radius, state.height)) {
				continue;
			}

			if (shouldPreserveBeaconAnchor(savedEntry.getValue().blockState)) {
				refreshedShell.put(pos, savedEntry.getValue().blockState);
				setDomainBlockState(world, pos, savedEntry.getValue().blockState, DOMAIN_BLOCK_PLACE_FLAGS);
				continue;
			}

			boolean shell = relativeY == 0 || !isInsideDomainDome(horizontalDistanceSq, relativeY, state.innerRadius, state.innerHeight);
			BlockState targetState = resolveDomainTargetState(
				ability,
				shell,
				centerX,
				centerZ,
				state.baseY,
				state.radius,
				state.height,
				state.innerRadius,
				state.innerHeight,
				pos
			);
			if (hasProtectedDecorationEntity(world, pos)) {
				if (
					shell
						|| targetState.isOf(Blocks.LIGHT)
						|| ability == MagicAbility.GREED_DOMAIN_EXPANSION
							&& GreedDomainRuntime.isProtectedInteriorStructureBlock(centerX, centerZ, state.baseY, state.innerRadius, state.innerHeight, pos)
				) {
					refreshedShell.put(pos, savedEntry.getValue().blockState);
				}
				continue;
			}
			if (
				shell
					|| targetState.isOf(Blocks.LIGHT)
					|| ability == MagicAbility.GREED_DOMAIN_EXPANSION
						&& GreedDomainRuntime.isProtectedInteriorStructureBlock(centerX, centerZ, state.baseY, state.innerRadius, state.innerHeight, pos)
			) {
				refreshedShell.put(pos, targetState);
			}
			setDomainBlockState(world, pos, targetState, DOMAIN_BLOCK_PLACE_FLAGS);
		}

		state.protectedShellStates.clear();
		state.protectedShellStates.putAll(refreshedShell);
	}

	public static boolean isDomainClashParticipantInvincible(Entity entity) {
		return DOMAIN_CLASH_PARTICIPANTS_INVINCIBLE && domainClashStateForParticipant(entity.getUuid()) != null;
	}

	public static boolean isGreedDomainIntroFrozen(ServerPlayerEntity player) {
		if (player == null || player.getEntityWorld().isClient()) {
			return false;
		}
		MinecraftServer server = player.getEntityWorld().getServer();
		return server != null && GreedDomainRuntime.isPlayerFrozenDuringIntro(player, server.getTicks());
	}

	public static boolean isGreedDomainIntroInvincible(Entity entity) {
		if (!(entity instanceof ServerPlayerEntity player) || player.getEntityWorld().isClient()) {
			return false;
		}
		MinecraftServer server = player.getEntityWorld().getServer();
		return server != null && GreedDomainRuntime.isPlayerInvulnerableDuringIntro(player, server.getTicks());
	}

	static void maintainDomainShell(ServerWorld world, DomainExpansionState state) {
		for (Map.Entry<BlockPos, BlockState> entry : state.protectedShellStates.entrySet()) {
			BlockPos pos = entry.getKey();
			BlockState expectedState = entry.getValue();
			if (!world.isInBuildLimit(pos)) {
				continue;
			}

			if (!world.getBlockState(pos).equals(expectedState)) {
				setDomainBlockState(world, pos, expectedState, DOMAIN_BLOCK_PLACE_FLAGS);
			}
		}
	}

	static void enforceCapturedEntitiesInsideDomain(ServerWorld world, DomainExpansionState state) {
		for (DomainCapturedEntityState captured : state.capturedEntities.values()) {
			Entity entity = world.getEntity(captured.entityId);
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				continue;
			}

			double dx = living.getX() - state.centerX;
			double dz = living.getZ() - state.centerZ;
			double horizontalDistanceSq = dx * dx + dz * dz;
			double relativeY = living.getY() - state.baseY;

			if (isInsideDomainInterior(horizontalDistanceSq, relativeY, state.innerRadius, state.innerHeight)) {
				captured.lastSafePos = new Vec3d(living.getX(), living.getY(), living.getZ());
				captured.lastSafeYaw = living.getYaw();
				captured.lastSafePitch = living.getPitch();
				continue;
			}

			Vec3d safePos = captured.lastSafePos;
			if (
				safePos == null
				|| !isInsideDomainInterior(
					(safePos.x - state.centerX) * (safePos.x - state.centerX) + (safePos.z - state.centerZ) * (safePos.z - state.centerZ),
					safePos.y - state.baseY,
					state.innerRadius,
					state.innerHeight
				)
				|| !isSafeDomainTeleportPosition(world, living, safePos.x, safePos.y, safePos.z)
			) {
				safePos = findNearestSafeDomainOccupantPosition(
					world,
					living,
					state.centerX,
					state.centerZ,
					state.baseY,
					state.innerRadius,
					state.innerHeight,
					living.getX(),
					living.getZ()
				);
			}
			if (safePos == null) {
				safePos = new Vec3d(state.centerX, state.baseY + 1.0, state.centerZ);
			}
			teleportDomainEntity(living, safePos.x, safePos.y, safePos.z, captured.lastSafeYaw, captured.lastSafePitch);
			captured.lastSafePos = safePos;
		}
	}

	static boolean isInsideDomainInterior(double horizontalDistanceSq, double y, int innerRadius, int innerHeight) {
		if (innerRadius <= 0 || innerHeight <= 1) {
			return false;
		}
		if (y < 1.0 || y > innerHeight - 0.2) {
			return false;
		}

		double horizontalTerm = horizontalDistanceSq / (double) (innerRadius * innerRadius);
		double verticalTerm = (y * y) / (double) (innerHeight * innerHeight);
		return horizontalTerm + verticalTerm <= 1.0;
	}

	static Map<UUID, DomainCapturedEntityState> captureDomainEntities(
		ServerWorld world,
		double centerX,
		double centerZ,
		int baseY,
		int radius,
		int height
	) {
		Map<UUID, DomainCapturedEntityState> captured = new HashMap<>();
		Box captureBox = new Box(
			centerX - radius,
			baseY - 3,
			centerZ - radius,
			centerX + radius + 1.0,
			baseY + height + 4.0,
			centerZ + radius + 1.0
		);

		List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, captureBox, entity -> entity.isAlive() && isDomainCapturable(entity));
		for (LivingEntity entity : entities) {
			double dx = entity.getX() - centerX;
			double dz = entity.getZ() - centerZ;
			if (dx * dx + dz * dz > radius * radius) {
				continue;
			}

			captured.put(
				entity.getUuid(),
				new DomainCapturedEntityState(
					entity.getUuid(),
					entity instanceof PlayerEntity,
					new Vec3d(entity.getX(), entity.getY(), entity.getZ()),
					entity.getYaw(),
					entity.getPitch()
				)
			);
		}

		return captured;
	}

	static void moveCapturedEntitiesIntoDomain(
		ServerWorld world,
		Iterable<DomainCapturedEntityState> capturedEntities,
		double centerX,
		double centerZ,
		int baseY,
		int innerRadius,
		int innerHeight
	) {
		double maxRadius = Math.max(0.0, innerRadius - 1.5);

		for (DomainCapturedEntityState captured : capturedEntities) {
			Entity entity = world.getEntity(captured.entityId);
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				continue;
			}

			double targetX = captured.position.x;
			double targetZ = captured.position.z;
			double dx = targetX - centerX;
			double dz = targetZ - centerZ;
			double distance = Math.sqrt(dx * dx + dz * dz);
			if (maxRadius > 0.0 && distance > maxRadius) {
				double scale = maxRadius / Math.max(distance, 1.0E-7);
				targetX = centerX + dx * scale;
				targetZ = centerZ + dz * scale;
			}
			Vec3d safePos = findNearestSafeDomainOccupantPosition(world, living, centerX, centerZ, baseY, innerRadius, innerHeight, targetX, targetZ);
			if (safePos == null) {
				safePos = findNearestSafeDomainOccupantPosition(world, living, centerX, centerZ, baseY, innerRadius, innerHeight, centerX, centerZ);
			}
			if (safePos == null) {
				safePos = new Vec3d(centerX, baseY + 1.0, centerZ);
			}

			teleportDomainEntity(living, safePos.x, safePos.y, safePos.z, captured.yaw, captured.pitch);
			captured.lastSafePos = safePos;
			captured.lastSafeYaw = captured.yaw;
			captured.lastSafePitch = captured.pitch;
		}
	}

	static Vec3d findNearestSafeDomainOccupantPosition(
		ServerWorld world,
		LivingEntity entity,
		double centerX,
		double centerZ,
		int baseY,
		int innerRadius,
		int innerHeight,
		double preferredX,
		double preferredZ
	) {
		int preferredBlockX = MathHelper.floor(preferredX);
		int preferredBlockZ = MathHelper.floor(preferredZ);
		int searchRadius = Math.max(1, innerRadius * 2);
		for (int radius = 0; radius <= searchRadius; radius++) {
			Vec3d bestCandidate = null;
			double bestDistanceSq = Double.MAX_VALUE;
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue;
					}
					Vec3d candidate = safeDomainOccupantPositionAtColumn(
						world,
						entity,
						centerX,
						centerZ,
						baseY,
						innerRadius,
						innerHeight,
						preferredBlockX + dx,
						preferredBlockZ + dz
					);
					if (candidate == null) {
						continue;
					}
					double distanceSq = squaredHorizontalDistance(candidate.x, candidate.z, preferredX, preferredZ);
					if (distanceSq < bestDistanceSq) {
						bestDistanceSq = distanceSq;
						bestCandidate = candidate;
					}
				}
			}
			if (bestCandidate != null) {
				return bestCandidate;
			}
		}
		return null;
	}

	static Vec3d safeDomainOccupantPositionAtColumn(
		ServerWorld world,
		LivingEntity entity,
		double centerX,
		double centerZ,
		int baseY,
		int innerRadius,
		int innerHeight,
		int blockX,
		int blockZ
	) {
		double targetX = blockX + 0.5;
		double targetZ = blockZ + 0.5;
		double horizontalDistanceSq = squaredHorizontalDistance(targetX, targetZ, centerX, centerZ);
		for (int supportY : new int[] { baseY + 1, baseY }) {
			BlockPos supportPos = new BlockPos(blockX, supportY, blockZ);
			BlockState supportState = world.getBlockState(supportPos);
			VoxelShape supportShape = supportState.getCollisionShape(world, supportPos);
			if (supportShape.isEmpty()) {
				continue;
			}
			if (supportY > baseY && supportState.blocksMovement()) {
				continue;
			}
			double targetY = supportPos.getY() + supportShape.getMax(Direction.Axis.Y);
			if (!isInsideDomainInterior(horizontalDistanceSq, targetY - baseY, innerRadius, innerHeight)) {
				continue;
			}
			if (!isSafeDomainTeleportPosition(world, entity, targetX, targetY, targetZ)) {
				continue;
			}
			return new Vec3d(targetX, targetY, targetZ);
		}
		return null;
	}

	static boolean isSafeDomainTeleportPosition(ServerWorld world, LivingEntity entity, double x, double y, double z) {
		Box targetBox = entity.getBoundingBox().offset(x - entity.getX(), y - entity.getY(), z - entity.getZ());
		return world.isSpaceEmpty(entity, targetBox) && !world.containsFluid(targetBox);
	}

	static double squaredHorizontalDistance(double x1, double z1, double x2, double z2) {
		double dx = x1 - x2;
		double dz = z1 - z2;
		return dx * dx + dz * dz;
	}

	static void restoreCapturedEntities(MinecraftServer server, DomainExpansionState state) {
		ServerWorld world = server.getWorld(state.dimension);
		if (world == null) {
			return;
		}

		for (DomainCapturedEntityState captured : state.capturedEntities.values()) {
			Entity entity = world.getEntity(captured.entityId);
			if (entity == null) {
				if (captured.playerEntity) {
					DOMAIN_PENDING_RETURNS.put(
						captured.entityId,
						new DomainPendingReturnState(
							state.dimension,
							captured.position.x,
							captured.position.y,
							captured.position.z,
							captured.yaw,
							captured.pitch
						)
					);
				}
				continue;
			}

			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				continue;
			}

			teleportDomainEntity(
				living,
				captured.position.x,
				captured.position.y,
				captured.position.z,
				captured.yaw,
				captured.pitch
			);
		}
	}

	static void teleportDomainEntity(LivingEntity entity, double x, double y, double z, float yaw, float pitch) {
		if (entity.hasVehicle()) {
			entity.stopRiding();
		}

		runWithDomainTeleportBypass(() -> {
			boolean positionChanged = entity.squaredDistanceTo(x, y, z) > DOMAIN_TELEPORT_POSITION_EPSILON_SQUARED;
			boolean rotationChanged = angleDeltaDegrees(entity.getYaw(), yaw) > DOMAIN_TELEPORT_ROTATION_EPSILON_DEGREES
				|| angleDeltaDegrees(entity.getPitch(), pitch) > DOMAIN_TELEPORT_ROTATION_EPSILON_DEGREES;
			if (positionChanged) {
				entity.requestTeleport(x, y, z);
			}
			entity.setVelocity(0.0, 0.0, 0.0);
			entity.setOnGround(true);
			entity.setYaw(yaw);
			entity.setPitch(pitch);
			entity.setHeadYaw(yaw);
			entity.setBodyYaw(yaw);

			if (entity instanceof ServerPlayerEntity player && (positionChanged || rotationChanged)) {
				player.networkHandler.requestTeleport(x, y, z, yaw, pitch);
			}
		});
	}

	static float angleDeltaDegrees(float current, float target) {
		return Math.abs(MathHelper.wrapDegrees(target - current));
	}

	static int currentTick(ServerPlayerEntity player) {
		return player == null || player.getEntityWorld().getServer() == null ? Integer.MIN_VALUE : player.getEntityWorld().getServer().getTicks();
	}

	static void runWithDomainTeleportBypass(Runnable action) {
		int depth = DOMAIN_TELEPORT_BYPASS_DEPTH.get();
		DOMAIN_TELEPORT_BYPASS_DEPTH.set(depth + 1);
		try {
			action.run();
		} finally {
			if (depth <= 0) {
				DOMAIN_TELEPORT_BYPASS_DEPTH.remove();
			} else {
				DOMAIN_TELEPORT_BYPASS_DEPTH.set(depth);
			}
		}
	}

	static boolean isDomainTeleportBypassActive() {
		return DOMAIN_TELEPORT_BYPASS_DEPTH.get() > 0;
	}

	public static boolean shouldBlockPlayerTeleport(ServerPlayerEntity player, ServerWorld targetWorld, double x, double y, double z) {
		if (
			player == null
			|| targetWorld == null
			|| isDomainTeleportBypassActive()
			|| !DOMAIN_CONTROL_BLOCK_TELEPORT_ACROSS_BOUNDARIES
		) {
			return false;
		}
		if (isFrostTeleportBlocked(player)) {
			player.sendMessage(Text.translatable("message.magic.frost.teleport_blocked"), true);
			return true;
		}

		if (isEntityCapturedByCelestialGamaRayBeam(player)) {
			player.sendMessage(Text.translatable("message.magic.constellation.celestial_gama_ray.teleport_blocked"), true);
			return true;
		}

		if (isEntityCapturedByDomain(player)) {
			player.sendMessage(Text.translatable("message.magic.domain.teleport_blocked"), true);
			return true;
		}

		if (isPositionInsideAnyDomain(targetWorld.getRegistryKey(), x, y, z)) {
			player.sendMessage(Text.translatable("message.magic.domain.teleport_blocked"), true);
			return true;
		}

		return false;
	}

	static boolean shouldPreserveBeaconAnchor(BlockState state) {
		return ASTRAL_CATACLYSM_PRESERVE_BEACON_ANCHOR && isBeaconAnchorBlock(state);
	}

	static boolean isBeaconAnchorBlock(BlockState state) {
		return state != null && Registries.BLOCK.getId(state.getBlock()).equals(ASTRAL_CATACLYSM_BEACON_ANCHOR_BLOCK_ID);
	}

	static boolean isHoldingBeaconCore(PlayerEntity player) {
		return player != null && (
			Registries.ITEM.getId(player.getMainHandStack().getItem()).equals(ASTRAL_CATACLYSM_BEACON_CORE_ITEM_ID) ||
			Registries.ITEM.getId(player.getOffHandStack().getItem()).equals(ASTRAL_CATACLYSM_BEACON_CORE_ITEM_ID)
		);
	}

	static boolean isBeaconCoreProtected(ServerPlayerEntity player) {
		if (
			player == null
			|| !isHoldingBeaconCore(player)
			|| !(player.getEntityWorld() instanceof ServerWorld world)
			|| ASTRAL_CATACLYSM_BEACON_CORE_PROTECTION_RADIUS <= 0.0
		) {
			return false;
		}

		BeaconCoreAnchorState anchor = currentBeaconCoreAnchor(world.getServer());
		if (anchor == null || !anchor.dimension.equals(world.getRegistryKey())) {
			return false;
		}

		ServerWorld anchorWorld = world.getServer().getWorld(anchor.dimension);
		if (anchorWorld == null || !isBeaconAnchorBlock(anchorWorld.getBlockState(anchor.pos))) {
			return false;
		}

		double protectionRadiusSquared = ASTRAL_CATACLYSM_BEACON_CORE_PROTECTION_RADIUS * ASTRAL_CATACLYSM_BEACON_CORE_PROTECTION_RADIUS;
		return player.squaredDistanceTo(anchor.pos.toCenterPos()) <= protectionRadiusSquared;
	}

	static BeaconCoreAnchorState currentBeaconCoreAnchor(MinecraftServer server) {
		if (server == null) {
			return null;
		}

		int currentTick = server.getTicks();
		if (
			cachedBeaconCoreAnchorTick != Integer.MIN_VALUE &&
			currentTick - cachedBeaconCoreAnchorTick < BEACON_CORE_ANCHOR_CACHE_REFRESH_TICKS
		) {
			return cachedBeaconCoreAnchor;
		}

		ServerWorld overworld = server.getOverworld();
		if (overworld == null) {
			cachedBeaconCoreAnchor = null;
			cachedBeaconCoreAnchorTick = currentTick;
			return null;
		}

		BeaconCoreAnchorPersistentState state = overworld.getPersistentStateManager().getOrCreate(
			new PersistentStateType<>(
				ASTRAL_CATACLYSM_BEACON_CORE_ANCHOR_STATE_ID,
				BeaconCoreAnchorPersistentState::new,
				BeaconCoreAnchorPersistentState.CODEC,
				DataFixTypes.SAVED_DATA_COMMAND_STORAGE
			)
		);
		cachedBeaconCoreAnchor = state == null ? null : state.getAnchor();
		cachedBeaconCoreAnchorTick = currentTick;
		return cachedBeaconCoreAnchor;
	}

	static boolean isDomainCapturable(LivingEntity entity) {
		if (entity instanceof PlayerEntity player) {
			return !player.isSpectator();
		}

		return entity instanceof MobEntity;
	}
}

