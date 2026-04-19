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


abstract class MagicAbilityManagerSupportCooldowns extends MagicAbilityManagerSupportDomainBlocks {
	public static void onCooldownCheckRequested(ServerPlayerEntity player) {
		if (!MagicPlayerData.hasMagic(player)) {
			player.sendMessage(Text.translatable("message.magic.no_access"), false);
			return;
		}

		int currentTick = player.getEntityWorld().getServer().getTicks();
		boolean foundCooldown = false;
		for (MagicAbility ability : abilitiesForSchool(MagicPlayerData.getSchool(player))) {
			int remainingTicks = cooldownRemaining(player, ability, currentTick);
			if (remainingTicks <= 0) {
				continue;
			}

			sendAbilityCooldownMessage(player, ability, remainingTicks, false);
			foundCooldown = true;
		}

		if (!foundCooldown) {
			player.sendMessage(Text.translatable("message.magic.cooldown.none"), false);
		}
	}

	static List<MagicAbility> abilitiesForSchool(MagicSchool school) {
		return switch (school) {
			case FROST -> List.of(
				MagicAbility.BELOW_FREEZING,
				MagicAbility.FROST_ASCENT,
				MagicAbility.PLANCK_HEAT,
				MagicAbility.ABSOLUTE_ZERO,
				MagicAbility.FROST_DOMAIN_EXPANSION
			);
			case LOVE -> List.of(
				MagicAbility.LOVE_AT_FIRST_SIGHT,
				MagicAbility.TILL_DEATH_DO_US_PART,
				MagicAbility.MANIPULATION,
				MagicAbility.LOVE_DOMAIN_EXPANSION
			);
			case JESTER -> List.of(
				MagicAbility.SPOTLIGHT,
				MagicAbility.WITTY_ONE_LINER,
				MagicAbility.COMEDIC_REWRITE,
				MagicAbility.COMEDIC_ASSISTANT,
				MagicAbility.PLUS_ULTRA
			);
			case CONSTELLATION -> List.of(
				MagicAbility.CASSIOPEIA,
				MagicAbility.HERCULES_BURDEN_OF_THE_SKY,
				MagicAbility.SAGITTARIUS_ASTRAL_ARROW,
				MagicAbility.ORIONS_GAMBIT,
				MagicAbility.ASTRAL_CATACLYSM
			);
			case BURNING_PASSION -> List.of(
				MagicAbility.MARTYRS_FLAME,
				MagicAbility.IGNITION,
				MagicAbility.SEARING_DASH,
				MagicAbility.CINDER_MARK,
				MagicAbility.ENGINE_HEART,
				MagicAbility.OVERRIDE
			);
			case GREED -> List.of(
				MagicAbility.APPRAISERS_MARK,
				MagicAbility.TOLLKEEPERS_CLAIM,
				MagicAbility.KINGS_DUES,
				MagicAbility.BANKRUPTCY,
				MagicAbility.GREED_DOMAIN_EXPANSION
			);
			default -> List.of();
		};
	}

	static int cooldownRemaining(ServerPlayerEntity player, MagicAbility ability, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}

		return switch (ability) {
			case MARTYRS_FLAME -> martyrsFlameCooldownRemaining(player, currentTick);
			case IGNITION -> ignitionCooldownRemaining(player, currentTick);
			case SEARING_DASH -> searingDashCooldownRemaining(player, currentTick);
			case CINDER_MARK -> cinderMarkCooldownRemaining(player, currentTick);
			case ENGINE_HEART -> engineHeartCooldownRemaining(player, currentTick);
			case OVERRIDE -> overrideCooldownRemaining(player, currentTick);
			case BELOW_FREEZING -> belowFreezingCooldownRemaining(player, currentTick);
			case FROST_ASCENT -> frostAscentCooldownRemaining(player, currentTick);
			case WITTY_ONE_LINER -> wittyOneLinerCooldownRemaining(player, currentTick);
			case COMEDIC_REWRITE -> comedicRewriteCooldownRemaining(player, currentTick);
			case COMEDIC_ASSISTANT -> comedicAssistantCooldownRemaining(player, currentTick);
			case PLUS_ULTRA -> plusUltraCooldownRemaining(player, currentTick);
			case ABSOLUTE_ZERO -> absoluteZeroCooldownRemaining(player, currentTick);
			case PLANCK_HEAT -> planckHeatCooldownRemaining(player, currentTick);
			case TILL_DEATH_DO_US_PART -> tillDeathDoUsPartCooldownRemaining(player, currentTick);
			case MANIPULATION -> manipulationCooldownRemaining(player, currentTick);
			case HERCULES_BURDEN_OF_THE_SKY -> herculesBurdenCooldownRemaining(player, currentTick);
			case SAGITTARIUS_ASTRAL_ARROW -> sagittariusAstralArrowCooldownRemaining(player, currentTick);
			case ORIONS_GAMBIT -> orionsGambitCooldownRemaining(player, currentTick);
			case APPRAISERS_MARK, TOLLKEEPERS_CLAIM, KINGS_DUES, BANKRUPTCY -> GreedRuntime.cooldownRemaining(player, ability, currentTick);
			case FROST_DOMAIN_EXPANSION, LOVE_DOMAIN_EXPANSION, ASTRAL_CATACLYSM, GREED_DOMAIN_EXPANSION -> domainCooldownRemaining(player, ability, currentTick);
			default -> 0;
		};
	}

	static void sendAbilityCooldownMessage(
		ServerPlayerEntity player,
		MagicAbility ability,
		int remainingTicks,
		boolean actionBar
	) {
		int secondsRemaining = (int) Math.ceil(remainingTicks / 20.0);
		player.sendMessage(Text.translatable("message.magic.ability.cooldown", ability.displayName(), secondsRemaining), actionBar);
	}

	static boolean shouldSendTillDeathCooldownMessage(UUID playerId, int currentTick) {
		int nextAllowedTick = TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK.getOrDefault(playerId, 0);
		if (currentTick < nextAllowedTick) {
			return false;
		}

		TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK.put(playerId, currentTick + COOLDOWN_MESSAGE_DEBOUNCE_TICKS);
		return true;
	}

	static boolean ownsLoveDomain(UUID playerId) {
		DomainExpansionState state = DOMAIN_EXPANSIONS.get(playerId);
		return state != null && state.ability == MagicAbility.LOVE_DOMAIN_EXPANSION;
	}

	static boolean isLockedByForeignLoveDomain(PlayerEntity player) {
		return isEntityCapturedByLoveDomain(player) && !ownsLoveDomain(player.getUuid());
	}

	static boolean canUseWindChargeWhileLocked(PlayerEntity player, ItemStack stack) {
		if (!stack.isOf(Items.WIND_CHARGE)) {
			return false;
		}

		UUID playerId = player.getUuid();
		return ownsLoveDomain(playerId) || domainClashStateForParticipant(playerId) != null;
	}

	static void applyDomainClashStartEffects(ServerPlayerEntity player) {
		applyDomainClashCombatEffects(player);
	}

	static void applyDomainClashCombatEffects(ServerPlayerEntity player) {
		player.addStatusEffect(
			new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, DOMAIN_CLASH_INSTANT_HEALTH_AMPLIFIER, true, false, true)
		);
		refreshStatusEffect(
			player,
			StatusEffects.REGENERATION,
			DOMAIN_CLASH_REGENERATION_REFRESH_TICKS,
			DOMAIN_CLASH_REGENERATION_AMPLIFIER,
			true,
			false,
			true
		);
		refreshStatusEffect(
			player,
			StatusEffects.RESISTANCE,
			DOMAIN_CLASH_REGENERATION_REFRESH_TICKS,
			DOMAIN_CLASH_RESISTANCE_AMPLIFIER,
			true,
			false,
			true
		);
	}

	static void applyPendingDomainReturn(ServerPlayerEntity player, MinecraftServer server) {
		DomainPendingReturnState pendingReturn = DOMAIN_PENDING_RETURNS.remove(player.getUuid());
		if (pendingReturn == null) {
			return;
		}

		ServerWorld targetWorld = server.getWorld(pendingReturn.dimension);
		if (targetWorld == null) {
			DOMAIN_PENDING_RETURNS.put(player.getUuid(), pendingReturn);
			return;
		}

		runWithDomainTeleportBypass(() ->
			player.teleport(
				targetWorld,
				pendingReturn.x,
				pendingReturn.y,
				pendingReturn.z,
				Set.<PositionFlag>of(),
				pendingReturn.yaw,
				pendingReturn.pitch,
				false
			)
		);
		player.setVelocity(0.0, 0.0, 0.0);
		player.setOnGround(true);
		persistDomainRuntimeState(server);
	}

	static int absoluteZeroCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, ABSOLUTE_ZERO_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int belowFreezingCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player) || isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, BELOW_FREEZING_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int frostAscentCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player) || isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, FROST_ASCENT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int martyrsFlameCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, MARTYRS_FLAME_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int planckHeatCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, PLANCK_HEAT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int tillDeathDoUsPartCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int domainCooldownRemaining(ServerPlayerEntity player, MagicAbility ability, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}

		UUID playerId = player.getUuid();
		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			return Math.max(0, FROST_DOMAIN_COOLDOWN_END_TICK.getOrDefault(playerId, 0) - currentTick);
		}
		if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return Math.max(0, LOVE_DOMAIN_COOLDOWN_END_TICK.getOrDefault(playerId, 0) - currentTick);
		}
		if (ability == MagicAbility.ASTRAL_CATACLYSM) {
			return Math.max(0, ASTRAL_CATACLYSM_COOLDOWN_END_TICK.getOrDefault(playerId, 0) - currentTick);
		}
		if (ability == MagicAbility.GREED_DOMAIN_EXPANSION) {
			return Math.max(0, GREED_DOMAIN_COOLDOWN_END_TICK.getOrDefault(playerId, 0) - currentTick);
		}
		return 0;
	}

	static void startDomainCooldown(UUID playerId, MagicAbility ability, int currentTick, double multiplier) {
		if (isCooldownDeferredByOrionsGambit(playerId, ability)) {
			return;
		}

		int baseTicks = domainCooldownTicks(ability);
		if (baseTicks <= 0) {
			return;
		}

		double safeMultiplier = MathHelper.clamp(multiplier, 0.0, 10.0);
		int cooldownTicks = adjustedCooldownTicks(playerId, ability, (int) Math.ceil(baseTicks * safeMultiplier), currentTick);
		if (cooldownTicks <= 0) {
			return;
		}

		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			FROST_DOMAIN_COOLDOWN_END_TICK.put(playerId, currentTick + cooldownTicks);
			return;
		}

		if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
			LOVE_DOMAIN_COOLDOWN_END_TICK.put(playerId, currentTick + cooldownTicks);
			return;
		}

		if (ability == MagicAbility.ASTRAL_CATACLYSM) {
			ASTRAL_CATACLYSM_COOLDOWN_END_TICK.put(playerId, currentTick + cooldownTicks);
			return;
		}

		if (ability == MagicAbility.GREED_DOMAIN_EXPANSION) {
			GREED_DOMAIN_COOLDOWN_END_TICK.put(playerId, currentTick + cooldownTicks);
		}
	}

	static void applyDomainEndCooldowns(
		UUID playerId,
		MagicAbility domainAbility,
		int currentTick,
		double domainCooldownMultiplier
	) {
		for (MagicAbility ability : abilitiesForSchool(domainAbility.school())) {
			if (ability == domainAbility) {
				startDomainCooldown(playerId, ability, currentTick, domainCooldownMultiplier);
				continue;
			}
			startAbilityCooldownFromNow(playerId, ability, currentTick);
		}
	}

	static void applyDomainCasterShutdown(
		UUID playerId,
		MagicAbility domainAbility,
		MinecraftServer server,
		int currentTick,
		double domainCooldownMultiplier
	) {
		disableSchoolPassivesAfterDomain(playerId, domainAbility.school(), server.getPlayerManager().getPlayer(playerId));
		applyDomainEndCooldowns(playerId, domainAbility, currentTick, domainCooldownMultiplier);
	}

	static void disableSchoolPassivesAfterDomain(UUID playerId, MagicSchool school, ServerPlayerEntity player) {
		switch (school) {
			case BURNING_PASSION -> {
				MARTYRS_FLAME_PASSIVE_ENABLED.remove(playerId);
				MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
				if (player != null) {
					player.removeStatusEffect(StatusEffects.GLOWING);
				}
			}
			case LOVE -> {
				TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.remove(playerId);
				TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
			}
			case JESTER -> {
				if (player != null) {
					deactivateSpotlight(player, true);
				} else {
					SPOTLIGHT_PASSIVE_ENABLED.remove(playerId);
					SPOTLIGHT_STATES.remove(playerId);
				}
				COMEDIC_REWRITE_PASSIVE_ENABLED.remove(playerId);
				COMEDIC_REWRITE_PENDING_STATES.remove(playerId);
				COMEDIC_REWRITE_IMMUNITY_END_TICK.remove(playerId);
				COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.remove(playerId);
			}
			case CONSTELLATION -> {
				if (player != null) {
					deactivateCassiopeia(player, true);
				} else {
					CASSIOPEIA_PASSIVE_ENABLED.remove(playerId);
					CASSIOPEIA_LAST_OUTLINED_PLAYERS.remove(playerId);
				}
			}
			default -> {
			}
		}
	}

	static int domainCooldownTicks(MagicAbility ability) {
		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			return Math.max(0, FROST_DOMAIN_COOLDOWN_TICKS);
		}
		if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return Math.max(0, LOVE_DOMAIN_COOLDOWN_TICKS);
		}
		if (ability == MagicAbility.ASTRAL_CATACLYSM) {
			return Math.max(0, ASTRAL_CATACLYSM_COOLDOWN_TICKS);
		}
		if (ability == MagicAbility.GREED_DOMAIN_EXPANSION) {
			return Math.max(0, GreedDomainRuntime.domainCooldownTicks());
		}
		return 0;
	}

	static int domainExpiresTick(MagicAbility ability, int currentTick) {
		if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
			return Integer.MAX_VALUE;
		}
		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			int durationTicks = Math.max(0, FROST_CONFIG.domain.startupTicks) + Math.max(1, FROST_CONFIG.domain.totalDurationTicks);
			if (FROST_CONFIG.domain.finalExecutionEnabled) {
				durationTicks += Math.max(0, FROST_CONFIG.domain.finalExecutionDelayTicks);
			}
			return currentTick + Math.max(TICKS_PER_SECOND, durationTicks);
		}

		int durationTicks = ability == MagicAbility.ASTRAL_CATACLYSM
			? ASTRAL_CATACLYSM_DURATION_TICKS
			: ability == MagicAbility.GREED_DOMAIN_EXPANSION
				? GreedDomainRuntime.domainDurationTicks()
				: DOMAIN_EXPANSION_DURATION_TICKS;
		return currentTick + Math.max(TICKS_PER_SECOND, durationTicks);
	}

	static void startAbilityCooldownFromNow(UUID playerId, MagicAbility ability, int currentTick) {
		if (ability == MagicAbility.BELOW_FREEZING) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || BELOW_FREEZING_COOLDOWN_TICKS <= 0) {
				BELOW_FREEZING_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			BELOW_FREEZING_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, BELOW_FREEZING_COOLDOWN_TICKS, currentTick));
			return;
		}

		if (ability == MagicAbility.FROST_ASCENT) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || FROST_ASCENT_COOLDOWN_TICKS <= 0) {
				FROST_ASCENT_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			FROST_ASCENT_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, FROST_ASCENT_COOLDOWN_TICKS, currentTick));
			return;
		}

		if (ability == MagicAbility.MARTYRS_FLAME) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || MARTYRS_FLAME_COOLDOWN_TICKS <= 0) {
				MARTYRS_FLAME_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			MARTYRS_FLAME_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, MARTYRS_FLAME_COOLDOWN_TICKS, currentTick));
			return;
		}

		if (ability == MagicAbility.IGNITION) {
			int cooldownTicks = Math.max(0, BURNING_PASSION_CONFIG.ignition.cooldownTicks);
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || cooldownTicks <= 0) {
				IGNITION_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			IGNITION_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, cooldownTicks, currentTick));
			return;
		}

		if (ability == MagicAbility.SEARING_DASH) {
			int cooldownTicks = Math.max(0, BURNING_PASSION_CONFIG.searingDash.cooldownTicks);
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || cooldownTicks <= 0) {
				SEARING_DASH_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			SEARING_DASH_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, cooldownTicks, currentTick));
			return;
		}

		if (ability == MagicAbility.CINDER_MARK) {
			int cooldownTicks = Math.max(0, BURNING_PASSION_CONFIG.cinderMark.cooldownTicks);
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || cooldownTicks <= 0) {
				CINDER_MARK_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			CINDER_MARK_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, cooldownTicks, currentTick));
			return;
		}

		if (ability == MagicAbility.ENGINE_HEART) {
			ENGINE_HEART_COOLDOWN_END_TICK.remove(playerId);
			return;
		}

		if (ability == MagicAbility.OVERRIDE) {
			int cooldownTicks = Math.max(0, BURNING_PASSION_CONFIG.override.cooldownTicks);
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || cooldownTicks <= 0) {
				OVERRIDE_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			OVERRIDE_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, cooldownTicks, currentTick));
			return;
		}

		if (ability == MagicAbility.ABSOLUTE_ZERO) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || ABSOLUTE_ZERO_COOLDOWN_TICKS <= 0) {
				ABSOLUTE_ZERO_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			ABSOLUTE_ZERO_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, ABSOLUTE_ZERO_COOLDOWN_TICKS, currentTick));
			return;
		}

		if (ability == MagicAbility.PLANCK_HEAT) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || PLANCK_HEAT_COOLDOWN_TICKS <= 0) {
				PLANCK_HEAT_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			PLANCK_HEAT_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, PLANCK_HEAT_COOLDOWN_TICKS, currentTick));
			return;
		}

		if (ability == MagicAbility.WITTY_ONE_LINER) {
			WittyOneLinerTierSettings tier = WITTY_ONE_LINER_HIGH_TIER;
			int cooldownTicks = Math.max(
				WITTY_ONE_LINER_LOW_TIER.cooldownTicks(),
				Math.max(WITTY_ONE_LINER_MID_TIER.cooldownTicks(), tier.cooldownTicks())
			);
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || cooldownTicks <= 0) {
				WITTY_ONE_LINER_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			WITTY_ONE_LINER_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, cooldownTicks, currentTick));
			return;
		}

		if (ability == MagicAbility.COMEDIC_REWRITE) {
			startComedicRewriteCooldown(playerId, currentTick);
			return;
		}

		if (ability == MagicAbility.COMEDIC_ASSISTANT) {
			startComedicAssistantCooldown(playerId, currentTick, COMEDIC_ASSISTANT_PROC_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.PLUS_ULTRA) {
			startPlusUltraCooldown(playerId, currentTick, PLUS_ULTRA_FULL_END_COOLDOWN_TICKS);
			return;
		}

		if (ability == MagicAbility.TILL_DEATH_DO_US_PART) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || TILL_DEATH_DO_US_PART_COOLDOWN_TICKS <= 0) {
				TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, TILL_DEATH_DO_US_PART_COOLDOWN_TICKS, currentTick));
			return;
		}

		if (ability == MagicAbility.MANIPULATION) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || MANIPULATION_COOLDOWN_TICKS <= 0) {
				MANIPULATION_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			MANIPULATION_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, MANIPULATION_COOLDOWN_TICKS, currentTick));
			return;
		}

		if (ability == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || HERCULES_COOLDOWN_TICKS <= 0) {
				HERCULES_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			HERCULES_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, HERCULES_COOLDOWN_TICKS, currentTick));
			return;
		}

		if (ability == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability) || CELESTIAL_ALIGNMENT_CONFIG.normalCooldownTicks <= 0) {
				SAGITTARIUS_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			SAGITTARIUS_COOLDOWN_END_TICK.put(
				playerId,
				currentTick + adjustedCooldownTicks(playerId, ability, CELESTIAL_ALIGNMENT_CONFIG.normalCooldownTicks, currentTick)
			);
			return;
		}

		if (ability == MagicAbility.ORIONS_GAMBIT) {
			if (ORIONS_GAMBIT_COOLDOWN_TICKS <= 0) {
				ORIONS_GAMBIT_COOLDOWN_END_TICK.remove(playerId);
				return;
			}
			ORIONS_GAMBIT_COOLDOWN_END_TICK.put(playerId, currentTick + adjustedCooldownTicks(playerId, ability, ORIONS_GAMBIT_COOLDOWN_TICKS, currentTick));
			return;
		}

		if (
			ability == MagicAbility.APPRAISERS_MARK
			|| ability == MagicAbility.TOLLKEEPERS_CLAIM
			|| ability == MagicAbility.KINGS_DUES
			|| ability == MagicAbility.BANKRUPTCY
		) {
			GreedRuntime.startAbilityCooldownFromNow(playerId, ability, currentTick);
			return;
		}

		if (
			ability == MagicAbility.FROST_DOMAIN_EXPANSION
			|| ability == MagicAbility.LOVE_DOMAIN_EXPANSION
			|| ability == MagicAbility.ASTRAL_CATACLYSM
			|| ability == MagicAbility.GREED_DOMAIN_EXPANSION
		) {
			if (isCooldownDeferredByOrionsGambit(playerId, ability)) {
				if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
					FROST_DOMAIN_COOLDOWN_END_TICK.remove(playerId);
				} else if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
					LOVE_DOMAIN_COOLDOWN_END_TICK.remove(playerId);
				} else if (ability == MagicAbility.ASTRAL_CATACLYSM) {
					ASTRAL_CATACLYSM_COOLDOWN_END_TICK.remove(playerId);
				} else {
					GREED_DOMAIN_COOLDOWN_END_TICK.remove(playerId);
				}
				return;
			}
			startDomainCooldown(playerId, ability, currentTick, 1.0);
		}
	}

	static void forceAbilityCooldown(UUID playerId, MagicAbility ability, int currentTick) {
		startAbilityCooldownFromNow(playerId, ability, currentTick);
	}

	static boolean isCooldownDeferredByOrionsGambit(UUID playerId, MagicAbility ability) {
		UUID casterId = ORIONS_GAMBIT_CASTER_BY_TARGET.get(playerId);
		if (casterId == null) {
			return false;
		}

		OrionGambitState state = ORIONS_GAMBIT_STATES.get(casterId);
		if (state == null || state.stage != OrionGambitStage.LINKED || !playerId.equals(state.targetId)) {
			return false;
		}

		state.usedTargetAbilities.add(ability);
		return ORIONS_GAMBIT_SUPPRESS_TARGET_COOLDOWNS;
	}

	static int adjustedCooldownTicks(UUID playerId, MagicAbility ability, int baseTicks, int currentTick) {
		return Math.max(0, GreedDomainRuntime.adjustCooldownTicks(playerId, ability, baseTicks, currentTick));
	}

	static void applyDomainInstabilityPenalty(ServerPlayerEntity player) {
		MagicPlayerData.setMana(player, 0);
		player.sendMessage(Text.translatable("message.magic.domain.unstable"), true);
	}

	static boolean isManipulationRequestDebounced(ServerPlayerEntity player, int currentTick) {
		UUID playerId = player.getUuid();
		int nextAllowedTick = MANIPULATION_NEXT_REQUEST_TICK.getOrDefault(playerId, 0);
		if (currentTick < nextAllowedTick) {
			debugManipulation(
				"{} manipulation request debounced at tick {} (nextAllowedTick={})",
				debugName(player),
				currentTick,
				nextAllowedTick
			);
			return true;
		}

		MANIPULATION_NEXT_REQUEST_TICK.put(playerId, currentTick + MANIPULATION_REQUEST_DEBOUNCE_TICKS);
		debugManipulation(
			"{} manipulation request accepted at tick {} (nextAllowedTick set to {})",
			debugName(player),
			currentTick,
			currentTick + MANIPULATION_REQUEST_DEBOUNCE_TICKS
		);
		return false;
	}

	static boolean shouldLogManipulationClamp(ServerPlayerEntity player, int currentTick) {
		UUID playerId = player.getUuid();
		int nextAllowedTick = MANIPULATION_LAST_CLAMP_LOG_TICK.getOrDefault(playerId, 0);
		if (currentTick < nextAllowedTick) {
			return false;
		}

		MANIPULATION_LAST_CLAMP_LOG_TICK.put(playerId, currentTick + MANIPULATION_CLAMP_LOG_INTERVAL_TICKS);
		return true;
	}

	static int manipulationCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, MANIPULATION_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int herculesBurdenCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, HERCULES_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int sagittariusAstralArrowCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		if (isOrionsGambitCooldownSuppressed(player)) {
			return 0;
		}
		return Math.max(0, SAGITTARIUS_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static int orionsGambitCooldownRemaining(ServerPlayerEntity player, int currentTick) {
		if (isTestingMode(player)) {
			return 0;
		}
		return Math.max(0, ORIONS_GAMBIT_COOLDOWN_END_TICK.getOrDefault(player.getUuid(), 0) - currentTick);
	}

	static Vec3d clampVector(Vec3d vector, double maxLength) {
		double lengthSquared = vector.lengthSquared();
		double maxLengthSquared = maxLength * maxLength;
		if (lengthSquared <= maxLengthSquared) {
			return vector;
		}

		double length = Math.sqrt(lengthSquared);
		if (length <= 1.0E-7) {
			return Vec3d.ZERO;
		}

		double scale = maxLength / length;
		return vector.multiply(scale);
	}

	static Vec3d manipulationHorizontalDelta(Vec3d rawInput, float yaw, double maxLengthPerTick) {
		Vec3d clampedInput = clampVector(rawInput, 1.0);
		double yawRadians = Math.toRadians(yaw);
		double sin = Math.sin(yawRadians);
		double cos = Math.cos(yawRadians);
		double worldX = clampedInput.x * cos - clampedInput.z * sin;
		double worldZ = clampedInput.z * cos + clampedInput.x * sin;
		return clampVector(new Vec3d(worldX, 0.0, worldZ), maxLengthPerTick);
	}

	static boolean isDomainExpansion(MagicAbility ability) {
		return ability == MagicAbility.FROST_DOMAIN_EXPANSION
			|| ability == MagicAbility.LOVE_DOMAIN_EXPANSION
			|| ability == MagicAbility.ASTRAL_CATACLYSM
			|| ability == MagicAbility.GREED_DOMAIN_EXPANSION;
	}

	static boolean isMaximumAbility(MagicAbility ability) {
		return ability == MagicAbility.PLUS_ULTRA || ability == MagicAbility.ABSOLUTE_ZERO || ability == MagicAbility.OVERRIDE;
	}

	static boolean isBankruptcyProtectedAbility(
		MagicAbility ability,
		boolean preserveMaximumAbilities,
		boolean preserveDomainAbilities
	) {
		if (ability == MagicAbility.NONE) {
			return false;
		}
		if (preserveDomainAbilities && isDomainExpansion(ability)) {
			return true;
		}
		return preserveMaximumAbilities && isMaximumAbility(ability);
	}
}

