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


abstract class RuntimeResetService extends MagicPacketService {
	public static int resetCooldown(ServerPlayerEntity player, MagicAbility ability) {
		UUID playerId = player.getUuid();

		if (
			ability == MagicAbility.APPRAISERS_MARK ||
			ability == MagicAbility.TOLLKEEPERS_CLAIM ||
			ability == MagicAbility.KINGS_DUES ||
			ability == MagicAbility.BANKRUPTCY
		) {
			return GreedAbilityRuntime.resetCooldown(player, ability);
		}

		if (ability == MagicAbility.MARTYRS_FLAME) {
			MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
			return MARTYRS_FLAME_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.IGNITION) {
			boolean removed = IGNITION_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= BURNING_PASSION_IGNITION_STATES.containsKey(playerId);
			endIgnition(player, player.getEntityWorld().getServer().getTicks(), BurningPassionIgnitionEndReason.CLEAR_ALL, false, false);
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.SEARING_DASH) {
			boolean removed = SEARING_DASH_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= clearBurningPassionSearingDashState(playerId) != null;
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.CINDER_MARK) {
			boolean removed = CINDER_MARK_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= BURNING_PASSION_CINDER_MARK_ARMED_STATES.remove(playerId) != null;
			removed |= clearBurningPassionCinderMarksByCaster(playerId, player.getEntityWorld().getServer());
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.ENGINE_HEART) {
			boolean removed = ENGINE_HEART_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= BURNING_PASSION_ENGINE_HEART_STATES.remove(playerId) != null;
			clearBurningPassionEngineHeartState(player);
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.OVERRIDE) {
			boolean removed = OVERRIDE_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= clearOverrideMeteorState(playerId, player.getEntityWorld().getServer());
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.BELOW_FREEZING) {
			boolean removed = BELOW_FREEZING_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= FROST_STAGE_STATES.remove(playerId) != null;
			clearFrostEffectsByCaster(playerId, false, player.getEntityWorld().getServer());
			removeFrostStageCasterBuffs(player);
			if (activeAbility(player) == MagicAbility.BELOW_FREEZING) {
				setActiveAbility(player, MagicAbility.NONE);
				removed = true;
			}
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.FROST_ASCENT) {
			return FROST_ASCENT_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.SPOTLIGHT) {
			SpotlightState state = SPOTLIGHT_STATES.get(playerId);
			boolean changed = state != null && (state.currentStage() > 0 || !state.viewerLastLookTicks().isEmpty());
			resetSpotlightTracking(player);
			return changed ? 1 : 0;
		}

		if (ability == MagicAbility.WITTY_ONE_LINER) {
			return WITTY_ONE_LINER_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.COMEDIC_REWRITE) {
			boolean removed = COMEDIC_REWRITE_COOLDOWN_END_TICK.remove(playerId) != null;
			COMEDIC_REWRITE_IMMUNITY_END_TICK.remove(playerId);
			COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.remove(playerId);
			COMEDIC_REWRITE_PENDING_STATES.remove(playerId);
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.COMEDIC_ASSISTANT) {
			boolean removed = COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= COMEDIC_ASSISTANT_ARMED_STATES.remove(playerId) != null;
			COMEDIC_ASSISTANT_PARROT_CARRY_STATES.remove(playerId);
			removed |= clearComedicAssistantCaneImpactState(playerId);
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.PLUS_ULTRA) {
			boolean removed = PLUS_ULTRA_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= PLUS_ULTRA_STATES.containsKey(playerId) || activeAbility(player) == MagicAbility.PLUS_ULTRA;
			clearPlusUltraRuntimeState(player, true);
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			return HERCULES_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			return SAGITTARIUS_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.ORIONS_GAMBIT) {
			return ORIONS_GAMBIT_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.ABSOLUTE_ZERO) {
			boolean removed = ABSOLUTE_ZERO_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= FROST_MAXIMUM_STATES.remove(playerId) != null;
			clearFrostMaximumState(player, false);
			FROST_MANA_REGEN_BLOCKED_END_TICK.remove(playerId);
			MANA_REGEN_BUFFER.remove(playerId);
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.PLANCK_HEAT) {
			boolean removed = PLANCK_HEAT_COOLDOWN_END_TICK.remove(playerId) != null;
			removed |= FROST_SPIKE_WAVES.removeIf(state -> playerId.equals(state.casterId));
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.TILL_DEATH_DO_US_PART) {
			return TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		if (ability == MagicAbility.MANIPULATION) {
			boolean removed = MANIPULATION_COOLDOWN_END_TICK.remove(playerId) != null;
			MANIPULATION_DRAIN_BUFFER.remove(playerId);
			MANIPULATION_NEXT_REQUEST_TICK.remove(playerId);
			MANIPULATION_LAST_CLAMP_LOG_TICK.remove(playerId);
			MANIPULATION_INTERACTION_PROXY.remove(playerId);
			MANIPULATION_INPUT_BY_CASTER.remove(playerId);
			MANIPULATION_LOOK_BY_CASTER.remove(playerId);
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			boolean removed = FROST_DOMAIN_COOLDOWN_END_TICK.remove(playerId) != null;
			if (removed) {
				persistDomainRuntimeState(player.getEntityWorld().getServer());
			}
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.LOVE_DOMAIN_EXPANSION) {
			boolean removed = LOVE_DOMAIN_COOLDOWN_END_TICK.remove(playerId) != null;
			if (removed) {
				persistDomainRuntimeState(player.getEntityWorld().getServer());
			}
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.ASTRAL_CATACLYSM) {
			boolean removed = ASTRAL_CATACLYSM_COOLDOWN_END_TICK.remove(playerId) != null;
			if (removed) {
				persistDomainRuntimeState(player.getEntityWorld().getServer());
			}
			return removed ? 1 : 0;
		}

		if (ability == MagicAbility.GREED_DOMAIN_EXPANSION) {
			return GREED_DOMAIN_COOLDOWN_END_TICK.remove(playerId) != null ? 1 : 0;
		}

		return 0;
	}

	public static int resetAllCooldowns(ServerPlayerEntity player) {
		return resetCooldown(player, MagicAbility.BELOW_FREEZING)
			+ resetCooldown(player, MagicAbility.FROST_ASCENT)
			+ resetCooldown(player, MagicAbility.MARTYRS_FLAME)
			+ resetCooldown(player, MagicAbility.IGNITION)
			+ resetCooldown(player, MagicAbility.SEARING_DASH)
			+ resetCooldown(player, MagicAbility.CINDER_MARK)
			+ resetCooldown(player, MagicAbility.ENGINE_HEART)
			+ resetCooldown(player, MagicAbility.OVERRIDE)
			+ resetCooldown(player, MagicAbility.APPRAISERS_MARK)
			+ resetCooldown(player, MagicAbility.TOLLKEEPERS_CLAIM)
			+ resetCooldown(player, MagicAbility.KINGS_DUES)
			+ resetCooldown(player, MagicAbility.BANKRUPTCY)
			+ resetCooldown(player, MagicAbility.SPOTLIGHT)
			+ resetCooldown(player, MagicAbility.WITTY_ONE_LINER)
			+ resetCooldown(player, MagicAbility.COMEDIC_REWRITE)
			+ resetCooldown(player, MagicAbility.COMEDIC_ASSISTANT)
			+ resetCooldown(player, MagicAbility.PLUS_ULTRA)
			+ resetCooldown(player, MagicAbility.HERCULES_BURDEN_OF_THE_SKY)
			+ resetCooldown(player, MagicAbility.SAGITTARIUS_ASTRAL_ARROW)
			+ resetCooldown(player, MagicAbility.ORIONS_GAMBIT)
			+ resetCooldown(player, MagicAbility.ABSOLUTE_ZERO)
			+ resetCooldown(player, MagicAbility.PLANCK_HEAT)
			+ resetCooldown(player, MagicAbility.TILL_DEATH_DO_US_PART)
			+ resetCooldown(player, MagicAbility.MANIPULATION)
			+ resetCooldown(player, MagicAbility.FROST_DOMAIN_EXPANSION)
			+ resetCooldown(player, MagicAbility.LOVE_DOMAIN_EXPANSION)
			+ resetCooldown(player, MagicAbility.ASTRAL_CATACLYSM)
			+ resetCooldown(player, MagicAbility.GREED_DOMAIN_EXPANSION);
	}

	public static void clearLockedAbilityState(ServerPlayerEntity player, MagicAbility ability) {
		if (player == null) {
			return;
		}

		if (
			ability == MagicAbility.IGNITION
			|| ability == MagicAbility.SEARING_DASH
			|| ability == MagicAbility.CINDER_MARK
			|| ability == MagicAbility.ENGINE_HEART
		) {
			endIgnition(player, player.getEntityWorld().getServer().getTicks(), BurningPassionIgnitionEndReason.LOCKED, false, false);
			IGNITION_COOLDOWN_END_TICK.remove(player.getUuid());
			SEARING_DASH_COOLDOWN_END_TICK.remove(player.getUuid());
			CINDER_MARK_COOLDOWN_END_TICK.remove(player.getUuid());
			ENGINE_HEART_COOLDOWN_END_TICK.remove(player.getUuid());
			return;
		}

		if (ability == MagicAbility.OVERRIDE) {
			clearOverrideMeteorState(player.getUuid(), player.getEntityWorld().getServer());
			OVERRIDE_COOLDOWN_END_TICK.remove(player.getUuid());
			return;
		}

		if (ability == MagicAbility.BELOW_FREEZING || ability == MagicAbility.FROST_ASCENT || ability == MagicAbility.PLANCK_HEAT) {
			endFrostStagedMode(player, player.getEntityWorld().getServer().getTicks(), FrostStageEndReason.LOCKED, false, false);
			PLANCK_HEAT_COOLDOWN_END_TICK.remove(player.getUuid());
			FROST_ASCENT_COOLDOWN_END_TICK.remove(player.getUuid());
			BELOW_FREEZING_COOLDOWN_END_TICK.remove(player.getUuid());
			return;
		}

		if (ability == MagicAbility.ABSOLUTE_ZERO) {
			clearFrostMaximumState(player, false);
			ABSOLUTE_ZERO_COOLDOWN_END_TICK.remove(player.getUuid());
			FROST_MANA_REGEN_BLOCKED_END_TICK.remove(player.getUuid());
			return;
		}

		if (ability == MagicAbility.FROST_DOMAIN_EXPANSION) {
			if (DOMAIN_EXPANSIONS.containsKey(player.getUuid()) && activeAbility(player) == MagicAbility.FROST_DOMAIN_EXPANSION) {
				deactivateDomainExpansion(player);
				setActiveAbility(player, MagicAbility.NONE);
			}
			return;
		}

		if (ability == MagicAbility.GREED_DOMAIN_EXPANSION) {
			if (DOMAIN_EXPANSIONS.containsKey(player.getUuid()) && activeAbility(player) == MagicAbility.GREED_DOMAIN_EXPANSION) {
				deactivateDomainExpansion(player);
				setActiveAbility(player, MagicAbility.NONE);
			}
			GREED_DOMAIN_COOLDOWN_END_TICK.remove(player.getUuid());
			return;
		}

		if (ability == MagicAbility.COMEDIC_ASSISTANT) {
			clearComedicAssistantState(player.getUuid());
			COMEDIC_ASSISTANT_PARROT_CARRY_STATES.remove(player.getUuid());
			clearComedicAssistantCaneImpactState(player.getUuid());
			return;
		}

		if (ability == MagicAbility.PLUS_ULTRA) {
			clearPlusUltraRuntimeState(player, true);
		}
	}

	public static void clearLockedStageProgressionState(ServerPlayerEntity player, MagicSchool school, int stage) {
		if (player == null || school == null || player.getEntityWorld().getServer() == null) {
			return;
		}
		int currentTick = player.getEntityWorld().getServer().getTicks();

		if (school == MagicSchool.FROST) {
			FrostStageState state = FROST_STAGE_STATES.get(player.getUuid());
			if (state == null) {
				return;
			}
			if (state.currentStage >= stage) {
				endFrostStagedMode(player, currentTick, FrostStageEndReason.LOCKED, false, false);
				return;
			}

			state.highestUnlockedStage = Math.min(state.highestUnlockedStage, Math.max(state.currentStage, stage - 1));
			syncFrostStageHud(player);
			return;
		}

		if (school == MagicSchool.BURNING_PASSION) {
			BurningPassionIgnitionState state = BURNING_PASSION_IGNITION_STATES.get(player.getUuid());
			if (state == null) {
				return;
			}
			if (state.currentStage >= stage) {
				endIgnition(player, currentTick, BurningPassionIgnitionEndReason.LOCKED, false, false);
				return;
			}

			int stageDuration = burningPassionStageDurationTicks(state.currentStage);
			state.stageStartTick = Math.max(state.stageStartTick, currentTick - stageDuration);
			syncBurningPassionHud(player);
		}
	}

	static boolean isLockedFrostStage(ServerPlayerEntity player, int stage) {
		return stage >= 2 && !MagicConfig.get().abilityAccess.isStageProgressionUnlocked(player.getUuid(), MagicSchool.FROST, stage);
	}

	static boolean isLockedBurningPassionStage(ServerPlayerEntity player, int stage) {
		return stage >= 2 && !MagicConfig.get().abilityAccess.isStageProgressionUnlocked(player.getUuid(), MagicSchool.BURNING_PASSION, stage);
	}

	static void clearDeathAbilityState(ServerPlayerEntity player) {
		MinecraftServer server = player.getEntityWorld().getServer();
		clearFrostControlledTargetState(player.getUuid(), server);
		MagicAbility activeAbility = activeAbility(player);
		boolean martyrsFlameActive = MARTYRS_FLAME_PASSIVE_ENABLED.contains(player.getUuid());
		if (activeAbility == MagicAbility.NONE && !martyrsFlameActive) {
			return;
		}

		if (martyrsFlameActive) {
			deactivateMartyrsFlame(player, true);
		}
		if (activeAbility == MagicAbility.IGNITION) {
			endIgnition(player, server.getTicks(), BurningPassionIgnitionEndReason.CASTER_DIED, true, false);
		}
		if (activeAbility == MagicAbility.BELOW_FREEZING) {
			endFrostStagedMode(player, server.getTicks(), FrostStageEndReason.CASTER_DIED, true, false);
		}
		if (activeAbility == MagicAbility.ABSOLUTE_ZERO) {
			clearFrostMaximumState(player, true);
		}
		if (activeAbility == MagicAbility.PLANCK_HEAT) {
			deactivatePlanckHeat(player);
		}
		if (activeAbility == MagicAbility.LOVE_AT_FIRST_SIGHT) {
			deactivateLoveAtFirstSight(player);
		}
		if (activeAbility == MagicAbility.TILL_DEATH_DO_US_PART) {
			deactivateTillDeathDoUsPart(player, TillDeathDoUsPartEndReason.CASTER_DIED, false);
		}
		if (activeAbility == MagicAbility.MANIPULATION) {
			deactivateManipulation(player, true, "caster died");
		}
		if (activeAbility == MagicAbility.HERCULES_BURDEN_OF_THE_SKY) {
			deactivateHerculesBurden(player, true, false);
		}
		if (activeAbility == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			clearSagittariusWindup(player);
		}
		if (activeAbility == MagicAbility.ORIONS_GAMBIT) {
			endOrionsGambit(player, OrionGambitEndReason.CASTER_DIED, server.getTicks(), false);
		}
		if (activeAbility == MagicAbility.PLUS_ULTRA) {
			endPlusUltra(player, server.getTicks(), PlusUltraEndMode.FULL, false);
		}
		clearOverrideMeteorState(player.getUuid(), server);
		setActiveAbility(player, MagicAbility.NONE);
	}

	public static void clearAllRuntimeState(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server == null) {
			return;
		}

		clearPlusUltraRuntimeState(player, true);
		clearFrostMaximumState(player, false);
		endFrostStagedMode(player, server.getTicks(), FrostStageEndReason.CLEAR_ALL, false, false);
		setActiveAbility(player, MagicAbility.NONE);
		player.setCameraEntity(player);
		MagicPlayerData.clearDomainClashUi(player);
		clearManipulationSuppressionTags(player);
		deactivateCassiopeia(player, true);
		deactivateSpotlight(player, true);
		deactivateHerculesBurden(player, false, false);
		clearSagittariusWindup(player);
		endOrionsGambit(player, OrionGambitEndReason.MANUAL_CANCEL, server.getTicks(), false);
		MARTYRS_FLAME_PASSIVE_ENABLED.remove(playerId);
		MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
		if (MARTYRS_FLAME_BURNING_TARGETS.remove(playerId) != null) {
			player.extinguish();
		}
		endIgnition(player, server.getTicks(), BurningPassionIgnitionEndReason.CLEAR_ALL, false, false);
		clearBurningPassionSearingDashState(playerId);
		clearBurningPassionCinderMarksForPlayer(playerId, server);
		clearBurningPassionEngineHeartState(player);
		clearOverrideMeteorState(playerId, server);

		boolean domainStateChanged = false;

		DomainExpansionState ownedDomain = DOMAIN_EXPANSIONS.remove(playerId);
		if (ownedDomain != null) {
			cancelDomainClash(playerId, server);
			restoreDomainExpansion(server, ownedDomain);
			domainStateChanged = true;
		}

		MARTYRS_FLAME_COOLDOWN_END_TICK.remove(playerId);
		IGNITION_COOLDOWN_END_TICK.remove(playerId);
		SEARING_DASH_COOLDOWN_END_TICK.remove(playerId);
		CINDER_MARK_COOLDOWN_END_TICK.remove(playerId);
		ENGINE_HEART_COOLDOWN_END_TICK.remove(playerId);
		OVERRIDE_COOLDOWN_END_TICK.remove(playerId);
		BELOW_FREEZING_COOLDOWN_END_TICK.remove(playerId);
		FROST_ASCENT_COOLDOWN_END_TICK.remove(playerId);
		WITTY_ONE_LINER_COOLDOWN_END_TICK.remove(playerId);
		COMEDIC_REWRITE_COOLDOWN_END_TICK.remove(playerId);
		COMEDIC_REWRITE_IMMUNITY_END_TICK.remove(playerId);
		COMEDIC_REWRITE_FALL_PROTECTION_END_TICK.remove(playerId);
		COMEDIC_REWRITE_PENDING_STATES.remove(playerId);
		COMEDIC_ASSISTANT_COOLDOWN_END_TICK.remove(playerId);
		COMEDIC_ASSISTANT_ARMED_STATES.remove(playerId);
		COMEDIC_ASSISTANT_PARROT_CARRY_STATES.remove(playerId);
		clearComedicAssistantCaneImpactState(playerId);
		PLUS_ULTRA_COOLDOWN_END_TICK.remove(playerId);
		PLUS_ULTRA_LAST_OUTLINED_PLAYERS.remove(playerId);
		PLUS_ULTRA_DAMAGE_SCALING_GUARD.remove(playerId);
		HERCULES_COOLDOWN_END_TICK.remove(playerId);
		SAGITTARIUS_COOLDOWN_END_TICK.remove(playerId);
		ORIONS_GAMBIT_COOLDOWN_END_TICK.remove(playerId);
		ABSOLUTE_ZERO_COOLDOWN_END_TICK.remove(playerId);
		FROST_MANA_REGEN_BLOCKED_END_TICK.remove(playerId);
		MANA_REGEN_BUFFER.remove(playerId);
		PLANCK_HEAT_COOLDOWN_END_TICK.remove(playerId);
		TILL_DEATH_DO_US_PART_COOLDOWN_END_TICK.remove(playerId);
		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
		TILL_DEATH_DO_US_PART_LAST_COOLDOWN_MESSAGE_TICK.remove(playerId);
		MANIPULATION_DRAIN_BUFFER.remove(playerId);
		MANIPULATION_COOLDOWN_END_TICK.remove(playerId);
		MANIPULATION_NEXT_REQUEST_TICK.remove(playerId);
		MANIPULATION_LAST_CLAMP_LOG_TICK.remove(playerId);
		MANIPULATION_INTERACTION_PROXY.remove(playerId);
		MANIPULATION_INPUT_BY_CASTER.remove(playerId);
		MANIPULATION_LOOK_BY_CASTER.remove(playerId);
		FROST_STAGE_STATES.remove(playerId);
		FROST_MAXIMUM_STATES.remove(playerId);
		FROST_SPIKE_WAVES.removeIf(state -> playerId.equals(state.casterId));
		clearFrostEffectsByCaster(playerId, true, server);
		clearFrostControlledTargetState(playerId, server);
		DOMAIN_CLASH_PENDING_DAMAGE.remove(playerId);
		MAGIC_DAMAGE_PENDING_ATTACKER.remove(playerId);
		MAGIC_DAMAGE_PENDING_ATTACKER.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
		ABSOLUTE_ZERO_NEXT_DAMAGE_TICK.remove(playerId);
		PLANCK_HEAT_STATES.remove(playerId);
		PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.remove(playerId);
		BURNING_PASSION_IGNITION_STATES.remove(playerId);
		clearBurningPassionAuraFireByCaster(playerId);
		BURNING_PASSION_AURA_FIRE_TARGETS.remove(playerId);
		BURNING_PASSION_SELF_FIRE_TARGETS.remove(playerId);
		BURNING_PASSION_HUD_NOTIFICATION_STATES.remove(playerId);
		BURNING_PASSION_PENDING_MELEE_IMPACTS.remove(playerId);
		LOVE_POWER_ACTIVE_THIS_SECOND.remove(playerId);
		TILL_DEATH_DO_US_PART_PASSIVE_ENABLED.remove(playerId);
		CASSIOPEIA_PASSIVE_ENABLED.remove(playerId);
		CASSIOPEIA_LAST_OUTLINED_PLAYERS.remove(playerId);
		HERCULES_STATES.remove(playerId);
		HERCULES_TARGETS.remove(playerId);
		SAGITTARIUS_STATES.remove(playerId);
		CELESTIAL_GAMA_RAY_STATES.remove(playerId);
		SAGITTARIUS_DRAIN_BUFFER.remove(playerId);
		ORIONS_GAMBIT_STATES.remove(playerId);
		ORIONS_GAMBIT_PENALTIES.remove(playerId);
		ORIONS_GAMBIT_CASTER_BY_TARGET.remove(playerId);
		ORIONS_GAMBIT_DRAIN_BUFFER.remove(playerId);
		ORIONS_GAMBIT_DAMAGE_SCALING_GUARD.remove(playerId);
		ASTRAL_CATACLYSM_DOMAIN_STATES.remove(playerId);
		ASTRAL_EXECUTION_BEAMS.remove(playerId);
		SPOTLIGHT_STATES.remove(playerId);
		SPOTLIGHT_PASSIVE_ENABLED.remove(playerId);
		COMEDIC_REWRITE_PASSIVE_ENABLED.remove(playerId);
		FROSTBITTEN_TARGETS.remove(playerId);
		FROST_DOMAIN_FROSTBITTEN_TARGETS.remove(playerId);
		FROST_DOMAIN_PULSE_STATUS_TARGETS.remove(playerId);
		FROST_DOMAIN_FROZEN_TARGETS.remove(playerId);
		ENHANCED_FIRE_TARGETS.remove(playerId);
		LOVE_LOCKED_TARGETS.remove(playerId);
		TILL_DEATH_DO_US_PART_STATES.remove(playerId);
		cancelDomainClashParticipant(playerId, server);
		if (FROST_DOMAIN_COOLDOWN_END_TICK.remove(playerId) != null) {
			domainStateChanged = true;
		}
		if (LOVE_DOMAIN_COOLDOWN_END_TICK.remove(playerId) != null) {
			domainStateChanged = true;
		}
		if (ASTRAL_CATACLYSM_COOLDOWN_END_TICK.remove(playerId) != null) {
			domainStateChanged = true;
		}
		GREED_DOMAIN_COOLDOWN_END_TICK.remove(playerId);
		if (clearCapturedDomainState(playerId)) {
			domainStateChanged = true;
		}

		GreedDomainRuntime.clearAllRuntimeState(player);

		LOVE_LOCKED_TARGETS.entrySet().removeIf(entry -> entry.getValue().casterId.equals(playerId));
		List<UUID> linkedTillDeathCasters = new ArrayList<>();
		for (Map.Entry<UUID, TillDeathDoUsPartState> entry : TILL_DEATH_DO_US_PART_STATES.entrySet()) {
			if (playerId.equals(entry.getValue().linkedPlayerId)) {
				linkedTillDeathCasters.add(entry.getKey());
			}
		}
		for (UUID casterId : linkedTillDeathCasters) {
			ServerPlayerEntity linkedCaster = server.getPlayerManager().getPlayer(casterId);
			if (linkedCaster != null) {
				deactivateTillDeathDoUsPart(linkedCaster, TillDeathDoUsPartEndReason.LINK_TARGET_INVALID, false);
				continue;
			}

			TillDeathDoUsPartState state = TILL_DEATH_DO_US_PART_STATES.remove(casterId);
			if (state == null) {
				continue;
			}

			startTillDeathDoUsPartCooldown(casterId, server.getTicks());
		}

		ManipulationState ownManipulation = MANIPULATION_STATES.remove(playerId);
		if (ownManipulation != null) {
			releaseManipulationState(playerId, ownManipulation, server, player);
		}
		MANIPULATION_CASTER_BY_TARGET.entrySet().removeIf(entry -> entry.getValue().equals(playerId));

		UUID manipulatorId = MANIPULATION_CASTER_BY_TARGET.remove(playerId);
		if (manipulatorId != null) {
			ManipulationState manipulatorState = MANIPULATION_STATES.remove(manipulatorId);
			ServerPlayerEntity manipulator = server.getPlayerManager().getPlayer(manipulatorId);
			if (manipulatorState != null) {
				releaseManipulationState(manipulatorId, manipulatorState, server, manipulator);
			}
			MANIPULATION_LAST_CLAMP_LOG_TICK.remove(manipulatorId);
			MANIPULATION_INTERACTION_PROXY.remove(manipulatorId);
			MANIPULATION_INPUT_BY_CASTER.remove(manipulatorId);
			MANIPULATION_LOOK_BY_CASTER.remove(manipulatorId);
			if (manipulator != null && activeAbility(manipulator) == MagicAbility.MANIPULATION) {
				setActiveAbility(manipulator, MagicAbility.NONE);
				manipulator.setCameraEntity(manipulator);
			}
		}

		ABSOLUTE_ZERO_NEXT_DAMAGE_TICK.values().forEach(targets -> targets.remove(playerId));
		ABSOLUTE_ZERO_NEXT_DAMAGE_TICK.entrySet().removeIf(entry -> entry.getValue().isEmpty());
		PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.values().forEach(targets -> targets.remove(playerId));
		PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.entrySet().removeIf(entry -> entry.getValue().isEmpty());
		HERCULES_TARGETS.entrySet().removeIf(entry -> entry.getValue().casterId.equals(playerId));
		ORIONS_GAMBIT_CASTER_BY_TARGET.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
		ASTRAL_EXECUTION_BEAMS.entrySet().removeIf(entry -> entry.getValue().casterId.equals(playerId));

		if (domainStateChanged) {
			persistDomainRuntimeState(server);
		}
	}

	static void onPlayerDisconnect(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}

		MinecraftServer server = player.getEntityWorld().getServer();
		if (server == null) {
			return;
		}

		clearFrostControlledTargetState(player.getUuid(), server);

		if (PLUS_ULTRA_STATES.containsKey(player.getUuid()) || activeAbility(player) == MagicAbility.PLUS_ULTRA) {
			endPlusUltra(player, server.getTicks(), PlusUltraEndMode.FULL, false);
			return;
		}

		endIgnition(player, server.getTicks(), BurningPassionIgnitionEndReason.INVALID, false, false);
		clearOverrideMeteorState(player.getUuid(), server);
		endFrostStagedMode(player, server.getTicks(), FrostStageEndReason.INVALID, false, false);
		clearFrostMaximumState(player, false);
		clearPlusUltraRuntimeState(player, false);
	}

	static boolean clearCapturedDomainState(UUID playerId) {
		boolean changed = DOMAIN_PENDING_RETURNS.remove(playerId) != null;
		for (DomainExpansionState state : DOMAIN_EXPANSIONS.values()) {
			if (state.capturedEntities.remove(playerId) != null) {
				changed = true;
			}
		}
		return changed;
	}

	static void deactivateMartyrsFlame(ServerPlayerEntity player, boolean startCooldown) {
		UUID playerId = player.getUuid();
		MARTYRS_FLAME_PASSIVE_ENABLED.remove(playerId);
		MARTYRS_FLAME_DRAIN_BUFFER.remove(playerId);
		player.removeStatusEffect(StatusEffects.GLOWING);
		if (!startCooldown || MARTYRS_FLAME_COOLDOWN_TICKS <= 0) {
			MARTYRS_FLAME_COOLDOWN_END_TICK.remove(playerId);
			return;
		}

		startAbilityCooldownFromNow(playerId, MagicAbility.MARTYRS_FLAME, player.getEntityWorld().getServer().getTicks());
	}

	static void deactivateAbsoluteZero(ServerPlayerEntity player) {
		clearFrostMaximumState(player, true);
		ABSOLUTE_ZERO_NEXT_DAMAGE_TICK.remove(player.getUuid());
	}

	static void deactivatePlanckHeat(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		PLANCK_HEAT_STATES.remove(playerId);
		PLANCK_HEAT_FROST_NEXT_DAMAGE_TICK.remove(playerId);
	}

	static void deactivateLoveAtFirstSight(ServerPlayerEntity player) {
		UUID casterId = player.getUuid();
		LOVE_POWER_ACTIVE_THIS_SECOND.put(casterId, false);
		LOVE_LOCKED_TARGETS.entrySet().removeIf(entry -> entry.getValue().casterId.equals(casterId));
	}

	static void deactivateTillDeathDoUsPart(
		ServerPlayerEntity player,
		TillDeathDoUsPartEndReason reason,
		boolean sendFeedback
	) {
		UUID casterId = player.getUuid();
		TillDeathDoUsPartState state = TILL_DEATH_DO_US_PART_STATES.remove(casterId);
		if (state == null) {
			if (activeAbility(player) == MagicAbility.TILL_DEATH_DO_US_PART) {
				setActiveAbility(player, MagicAbility.NONE);
			}
			return;
		}

		if (reason == TillDeathDoUsPartEndReason.CASTER_DIED) {
			ServerPlayerEntity linkedPlayer = player.getEntityWorld().getServer().getPlayerManager().getPlayer(state.linkedPlayerId);
			if (linkedPlayer != null && linkedPlayer.isAlive()) {
				dealTrackedMagicDamage(linkedPlayer, casterId, linkedPlayer.getDamageSources().magic(), Float.MAX_VALUE);
			}
		}

		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(casterId);
		startTillDeathDoUsPartCooldown(casterId, player.getEntityWorld().getServer().getTicks());
		setActiveAbility(player, MagicAbility.NONE);

		if (sendFeedback) {
			player.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.TILL_DEATH_DO_US_PART.displayName()), true);
		}
	}

	static void startTillDeathDoUsPartCooldown(UUID playerId, int currentTick) {
		TILL_DEATH_DO_US_PART_DRAIN_BUFFER.remove(playerId);
		startAbilityCooldownFromNow(playerId, MagicAbility.TILL_DEATH_DO_US_PART, currentTick);
	}

	static void deactivateManipulation(ServerPlayerEntity caster, boolean startCooldown) {
		deactivateManipulation(caster, startCooldown, "unspecified");
	}

	static void deactivateManipulation(ServerPlayerEntity caster, boolean startCooldown, String reason) {
		UUID casterId = caster.getUuid();
		ManipulationState state = MANIPULATION_STATES.remove(casterId);
		MANIPULATION_DRAIN_BUFFER.remove(casterId);
		MANIPULATION_LAST_CLAMP_LOG_TICK.remove(casterId);
		MANIPULATION_INTERACTION_PROXY.remove(casterId);
		MANIPULATION_INPUT_BY_CASTER.remove(casterId);
		MANIPULATION_LOOK_BY_CASTER.remove(casterId);
		if (state == null) {
			debugManipulation(
				"{} manipulation deactivate skipped (no state). reason={}, startCooldown={}",
				debugName(caster),
				reason,
				startCooldown
			);
			if (startCooldown) {
				startAbilityCooldownFromNow(casterId, MagicAbility.MANIPULATION, caster.getEntityWorld().getServer().getTicks());
				debugManipulation(
					"{} manipulation deactivate(no-state) cooldown applied until tick {}",
					debugName(caster),
					caster.getEntityWorld().getServer().getTicks() + MANIPULATION_COOLDOWN_TICKS
				);
			}
			return;
		}

		debugManipulation(
			"{} manipulation deactivated. reason={}, startCooldown={}, targetId={}",
			debugName(caster),
			reason,
			startCooldown,
			state.targetId
		);
		releaseManipulationState(casterId, state, caster.getEntityWorld().getServer(), caster);
		if (startCooldown) {
			startAbilityCooldownFromNow(casterId, MagicAbility.MANIPULATION, caster.getEntityWorld().getServer().getTicks());
			debugManipulation(
				"{} manipulation cooldown applied until tick {}",
				debugName(caster),
				caster.getEntityWorld().getServer().getTicks() + MANIPULATION_COOLDOWN_TICKS
			);
		}
		debugManipulation(
			"{} manipulation deactivation finalized: remainingStates={}, remainingTargets={}",
			debugName(caster),
			MANIPULATION_STATES.size(),
			MANIPULATION_CASTER_BY_TARGET.size()
		);
	}

	static void releaseManipulationState(
		UUID casterId,
		ManipulationState state,
		MinecraftServer server,
		ServerPlayerEntity caster
	) {
		boolean removedMapping = state.targetId != null && MANIPULATION_CASTER_BY_TARGET.remove(state.targetId, casterId);
		MANIPULATION_INTERACTION_PROXY.remove(casterId);
		debugManipulation(
			"{} manipulation state release: targetId={}, mappingRemoved={}, serverTick={}",
			caster == null ? casterId : debugName(caster),
			state.targetId,
			removedMapping,
			server.getTicks()
		);

		if (state.targetId != null) {
			ServerPlayerEntity target = server.getPlayerManager().getPlayer(state.targetId);
			if (target != null) {
				clearManipulationSuppressionTags(target);
			}
		}

		if (caster != null) {
			debugManipulation("{} empty embrace state release complete", debugName(caster));
		}
	}

	static MagicAbility activeAbility(PlayerEntity player) {
		return MagicAbility.fromSlotForSchool(
			MagicPlayerData.getActiveAbilitySlot(player),
			MagicPlayerData.getSchool(player)
		);
	}

	static void setActiveAbility(ServerPlayerEntity player, MagicAbility ability) {
		MagicPlayerData.setActiveAbilitySlot(player, ability.slot());
	}

	static boolean isAttackInteraction(PlayerInteractEntityC2SPacket packet) {
		final boolean[] attack = { false };
		packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
			@Override
			public void interact(Hand hand) {
			}

			@Override
			public void interactAt(Hand hand, Vec3d pos) {
			}

			@Override
			public void attack() {
				attack[0] = true;
			}
		});
		return attack[0];
	}
}


