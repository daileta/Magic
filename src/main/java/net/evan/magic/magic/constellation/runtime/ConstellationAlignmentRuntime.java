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


abstract class ConstellationAlignmentRuntime extends CombatTargetingService {
	static DamageSource createTrueMagicDamageSource(ServerWorld world, Entity attacker) {
		return createConfiguredMagicDamageSource(world, attacker, TRUE_MAGIC_DAMAGE_TYPE);
	}

	static DamageSource createTrueMagicDamageSource(ServerWorld world) {
		return createConfiguredMagicDamageSource(world, null, TRUE_MAGIC_DAMAGE_TYPE);
	}

	static DamageSource createTrueMagicDamageSource(MinecraftServer server, ServerWorld world, UUID casterId) {
		ServerPlayerEntity caster = server.getPlayerManager().getPlayer(casterId);
		return caster == null ? createTrueMagicDamageSource(world) : createTrueMagicDamageSource(world, caster);
	}

	static DamageSource createConfiguredMagicDamageSource(ServerWorld world, Entity attacker, RegistryKey<DamageType> damageTypeKey) {
		var damageTypeRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.DAMAGE_TYPE);
		return new DamageSource(damageTypeRegistry.getEntry(damageTypeRegistry.getValueOrThrow(damageTypeKey)), attacker);
	}

	static void spawnParticleBeam(ServerWorld world, Vec3d start, Vec3d end, ParticleEffect particle, double spacing) {
		Vec3d delta = end.subtract(start);
		double length = delta.length();
		if (length <= 1.0E-6) {
			world.spawnParticles(particle, start.x, start.y, start.z, 1, 0.0, 0.0, 0.0, 0.0);
			return;
		}

		Vec3d step = delta.normalize().multiply(Math.max(0.1, spacing));
		int steps = Math.max(1, (int) Math.ceil(length / Math.max(0.1, spacing)));
		Vec3d current = start;
		for (int i = 0; i <= steps; i++) {
			world.spawnParticles(particle, current.x, current.y, current.z, 2, 0.08, 0.08, 0.08, 0.0);
			current = current.add(step);
		}
	}

	static void sendCelestialAlignmentBanner(ServerPlayerEntity caster, CelestialAlignmentConstellation constellation) {
		if (!CELESTIAL_ALIGNMENT_CONFIG.banner.enabled) {
			return;
		}

		MagicConfig.BannerConfig banner = CELESTIAL_ALIGNMENT_CONFIG.banner;
		ServerPlayNetworking.send(
			caster,
			new ConstellationWarningOverlayPayload(
				celestialAlignmentDisplayName(constellation),
				celestialAlignmentColor(constellation),
				banner.scale,
				banner.fadeInTicks,
				banner.stayTicks,
				banner.fadeOutTicks,
				banner.screenXOffset,
				banner.screenYOffset
			)
		);
	}

	static void clearCelestialAlignmentBanner(ServerPlayerEntity caster) {
		if (caster == null) {
			return;
		}

		ServerPlayNetworking.send(
			caster,
			new ConstellationWarningOverlayPayload(
				"",
				0xFFFFFF,
				CELESTIAL_ALIGNMENT_CONFIG.banner.scale,
				0,
				0,
				0,
				CELESTIAL_ALIGNMENT_CONFIG.banner.screenXOffset,
				CELESTIAL_ALIGNMENT_CONFIG.banner.screenYOffset
			)
		);
	}

	static void sendCelestialGamaRayTraceBanner(ServerPlayerEntity caster) {
		if (caster == null || !CELESTIAL_ALIGNMENT_CONFIG.banner.enabled) {
			return;
		}

		MagicConfig.BannerConfig banner = CELESTIAL_ALIGNMENT_CONFIG.banner;
		ServerPlayNetworking.send(
			caster,
			new ConstellationWarningOverlayPayload(
				Text.translatable("message.magic.constellation.celestial_gama_ray.trace_prompt_short").getString(),
				parseHexColor(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.promptColorHex, 0x7FD4FF),
				banner.scale,
				banner.fadeInTicks,
				20 * 60 * 20,
				banner.fadeOutTicks,
				banner.screenXOffset,
				banner.screenYOffset
			)
		);
	}

	static CelestialAlignmentConstellation rollCelestialAlignment(ServerPlayerEntity caster) {
		List<CelestialAlignmentConstellation> constellations = List.of(
			CelestialAlignmentConstellation.CRATER,
			CelestialAlignmentConstellation.SAGITTA,
			CelestialAlignmentConstellation.GEMINI,
			CelestialAlignmentConstellation.AQUILA,
			CelestialAlignmentConstellation.SCORPIUS,
			CelestialAlignmentConstellation.LIBRA
		);
		return constellations.get(caster.getRandom().nextInt(constellations.size()));
	}

	static String celestialAlignmentDisplayName(CelestialAlignmentConstellation constellation) {
		return Text.translatable(constellation.translationKey).getString();
	}

	static Text celestialGamaRayDisplayName() {
		return Text.translatable("magic.constellation.celestial_gama_ray");
	}

	static void endCelestialAlignmentSession(
		ServerPlayerEntity caster,
		CelestialAlignmentSessionEndReason reason,
		int currentTick,
		boolean sendFeedback,
		boolean startCooldown
	) {
		if (caster == null) {
			return;
		}
		CelestialAlignmentSessionState session = SAGITTARIUS_STATES.remove(caster.getUuid());
		SAGITTARIUS_DRAIN_BUFFER.remove(caster.getUuid());
		MinecraftServer server = caster.getEntityWorld().getServer();
		if (session != null && server != null) {
			for (CelestialAlignmentState state : new ArrayList<>(session.constellations)) {
				releaseCelestialAlignmentState(server, state, true);
			}
		}
		if (startCooldown && CELESTIAL_ALIGNMENT_CONFIG.normalCooldownTicks > 0) {
			SAGITTARIUS_COOLDOWN_END_TICK.put(
				caster.getUuid(),
				currentTick + adjustedCooldownTicks(caster.getUuid(), MagicAbility.SAGITTARIUS_ASTRAL_ARROW, CELESTIAL_ALIGNMENT_CONFIG.normalCooldownTicks, currentTick)
			);
		}
		if (!CELESTIAL_GAMA_RAY_STATES.containsKey(caster.getUuid()) && activeAbility(caster) == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			setActiveAbility(caster, MagicAbility.NONE);
		}
		if (sendFeedback) {
			caster.sendMessage(Text.translatable("message.magic.ability.deactivated", MagicAbility.SAGITTARIUS_ASTRAL_ARROW.displayName()), true);
		}
	}

	static void startCelestialGamaRayTracing(ServerPlayerEntity caster, int currentTick) {
		CelestialAlignmentConstellation constellation = rollCelestialAlignment(caster);
		CelestialGamaRayState state = new CelestialGamaRayState(
			caster.getUuid(),
			caster.getEntityWorld().getRegistryKey(),
			constellation,
			currentTick + Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.resetTimeoutTicks)
		);
		CELESTIAL_GAMA_RAY_STATES.put(caster.getUuid(), state);
		setActiveAbility(caster, MagicAbility.SAGITTARIUS_ASTRAL_ARROW);
		MagicPlayerData.setDepletedRecoveryMode(caster, false);
		sendCelestialGamaRayTraceOverlay(caster, true, constellation);
		sendCelestialGamaRayTraceBanner(caster);
		broadcastCelestialGamaRayVisualState(caster, state, currentTick, state.traceExpireTick);
	}

	static void clearCelestialGamaRayState(ServerPlayerEntity caster, boolean sendFeedback, boolean startCooldown) {
		clearCelestialGamaRayState(caster, sendFeedback, startCooldown, CELESTIAL_ALIGNMENT_CONFIG.gamaRayCooldownTicks);
	}

	static void clearCelestialGamaRayState(ServerPlayerEntity caster, boolean sendFeedback, boolean startCooldown, int cooldownTicks) {
		if (caster == null) {
			return;
		}
		CelestialGamaRayState state = CELESTIAL_GAMA_RAY_STATES.remove(caster.getUuid());
		MinecraftServer server = caster.getEntityWorld().getServer();
		if (state != null && startCooldown && server != null && cooldownTicks > 0) {
			startCelestialGamaRayCooldown(caster, server.getTicks(), cooldownTicks);
		}
		if (!SAGITTARIUS_STATES.containsKey(caster.getUuid()) && activeAbility(caster) == MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
			setActiveAbility(caster, MagicAbility.NONE);
		}
		sendCelestialGamaRayTraceOverlay(caster, false, state == null ? null : state.constellation);
		clearCelestialGamaRayVisualState(caster);
		clearCelestialAlignmentBanner(caster);
		if (sendFeedback) {
			caster.sendMessage(Text.translatable("message.magic.ability.deactivated", celestialGamaRayDisplayName()), true);
		}
	}

	static void startCelestialGamaRayCooldown(ServerPlayerEntity caster, int currentTick, int cooldownTicks) {
		if (caster == null || cooldownTicks <= 0) {
			return;
		}
		SAGITTARIUS_COOLDOWN_END_TICK.put(
			caster.getUuid(),
			currentTick + adjustedCooldownTicks(caster.getUuid(), MagicAbility.SAGITTARIUS_ASTRAL_ARROW, cooldownTicks, currentTick)
		);
	}

	static void cancelCelestialGamaRayTrace(ServerPlayerEntity caster, boolean sendFeedback) {
		clearCelestialGamaRayState(caster, false, true, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.cancelCooldownTicks);
		if (caster != null && sendFeedback) {
			caster.sendMessage(Text.translatable("message.magic.constellation.celestial_gama_ray.trace_cancelled"), true);
		}
	}

	public static void onCelestialGamaRayTraceProgress(ServerPlayerEntity player, String action) {
		if (player == null || action == null) {
			return;
		}
		CelestialGamaRayState state = CELESTIAL_GAMA_RAY_STATES.get(player.getUuid());
		if (state == null || state.phase != CelestialGamaRayPhase.TRACING) {
			return;
		}
		int currentTick = player.getEntityWorld().getServer() == null ? 0 : player.getEntityWorld().getServer().getTicks();
		String normalized = action.trim().toLowerCase();
		if ("success".equals(normalized)) {
			state.phase = CelestialGamaRayPhase.CHARGING;
			state.chargeCompleteTick = currentTick + Math.max(0, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.charge.readyDelayTicks);
			sendCelestialGamaRayTraceOverlay(player, false, state.constellation);
			clearCelestialAlignmentBanner(player);
			broadcastCelestialGamaRayVisualState(player, state, currentTick, state.chargeCompleteTick);
			if (CELESTIAL_ALIGNMENT_CONFIG.gamaRay.presentation.chargeSoundLayeringEnabled && player.getEntityWorld() instanceof ServerWorld world) {
				world.playSound(null, player.getX(), player.getBodyY(0.6), player.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.0F, 0.9F);
			}
			player.sendMessage(Text.translatable("message.magic.constellation.celestial_gama_ray.charging"), true);
			return;
		}
		if ("reset".equals(normalized)) {
			state.traceExpireTick = currentTick + Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.resetTimeoutTicks);
			player.sendMessage(Text.translatable("message.magic.constellation.celestial_gama_ray.trace_reset"), true);
		}
	}

	static void sendCelestialGamaRayTraceOverlay(ServerPlayerEntity player, boolean active, CelestialAlignmentConstellation constellation) {
		if (player == null) {
			return;
		}
		ServerPlayNetworking.send(
			player,
			new CelestialGamaRayTraceOverlayPayload(
				active,
				constellation == null ? "" : constellation.name().toLowerCase(),
				CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.overlayScale,
				CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.overlayXOffset,
				CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.overlayYOffset,
				(float) CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.inputScale,
				CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.lineThickness,
				CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.nodeRadius,
				CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.cursorRadius,
				CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.lockCameraWhileTracing,
				parseHexColor(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.pathColorHex, 0x86B6FF),
				parseHexColor(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.progressColorHex, 0xFFF1B0),
				parseHexColor(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.activeSegmentColorHex, 0xFFFFFF),
				parseHexColor(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.startNodeColorHex, 0x9CFFDE),
				parseHexColor(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.endNodeColorHex, 0xFFA0B4),
				parseHexColor(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.successColorHex, 0x9CFFDE),
				parseHexColor(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.failColorHex, 0xFF8080),
				(float) CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.toleranceRadius,
				(float) CELESTIAL_ALIGNMENT_CONFIG.gamaRay.tracing.segmentCompletionRadius,
				"",
				Text.translatable("message.magic.constellation.celestial_gama_ray.charging").getString()
			)
		);
	}

	static void broadcastCelestialGamaRayVisualState(ServerPlayerEntity caster, CelestialGamaRayState state, int phaseStartTick, int phaseEndTick) {
		if (caster == null || !(caster.getEntityWorld() instanceof ServerWorld world) || state == null) {
			return;
		}

		CelestialGamaRayVisualPayload payload = new CelestialGamaRayVisualPayload(
			caster.getUuid(),
			true,
			switch (state.phase) {
				case TRACING -> "tracing";
				case CHARGING -> "charging";
				case FIRING -> "firing";
			},
			phaseStartTick,
			phaseEndTick,
			state.beamOrigin.x,
			state.beamOrigin.y,
			state.beamOrigin.z,
			state.beamDirection.x,
			state.beamDirection.y,
			state.beamDirection.z,
			state.lockedYaw,
			state.lockedPitch
		);
		for (ServerPlayerEntity watcher : world.getPlayers()) {
			ServerPlayNetworking.send(watcher, payload);
		}
	}

	static void clearCelestialGamaRayVisualState(ServerPlayerEntity caster) {
		if (caster == null || !(caster.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		CelestialGamaRayVisualPayload payload = new CelestialGamaRayVisualPayload(
			caster.getUuid(),
			false,
			"inactive",
			0,
			0,
			0.0,
			0.0,
			0.0,
			0.0,
			0.0,
			0.0,
			0.0F,
			0.0F
		);
		for (ServerPlayerEntity watcher : world.getPlayers()) {
			ServerPlayNetworking.send(watcher, payload);
		}
	}

	static void updateCelestialGamaRayStates(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, CelestialGamaRayState>> iterator = CELESTIAL_GAMA_RAY_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, CelestialGamaRayState> entry = iterator.next();
			ServerPlayerEntity caster = server.getPlayerManager().getPlayer(entry.getKey());
			CelestialGamaRayState state = entry.getValue();
			if (caster == null || !caster.isAlive() || activeAbility(caster) != MagicAbility.SAGITTARIUS_ASTRAL_ARROW) {
				if (caster != null) {
					clearCelestialGamaRayVisualState(caster);
				}
				iterator.remove();
				continue;
			}
			if (state.phase == CelestialGamaRayPhase.TRACING && currentTick >= state.traceExpireTick) {
				iterator.remove();
				sendCelestialGamaRayTraceOverlay(caster, false, state.constellation);
				clearCelestialGamaRayVisualState(caster);
				clearCelestialAlignmentBanner(caster);
				caster.sendMessage(Text.translatable("message.magic.constellation.celestial_gama_ray.trace_failed"), true);
				if (!SAGITTARIUS_STATES.containsKey(caster.getUuid())) {
					setActiveAbility(caster, MagicAbility.NONE);
				}
				continue;
			}
			if (state.phase == CelestialGamaRayPhase.CHARGING && currentTick >= state.chargeCompleteTick) {
				beginCelestialGamaRayBeam(caster, state, currentTick);
			}
			if (state.phase == CelestialGamaRayPhase.FIRING) {
				updateCelestialGamaRayBeam(caster, state, currentTick);
				if (currentTick >= state.endTick) {
					startCelestialGamaRayCooldown(caster, currentTick, CELESTIAL_ALIGNMENT_CONFIG.gamaRayCooldownTicks);
					iterator.remove();
					clearCelestialGamaRayState(caster, true, false);
				}
			}
		}
	}

	static int parseHexColor(String rawColor, int fallbackColor) {
		if (rawColor == null || rawColor.isBlank()) {
			return fallbackColor;
		}

		String normalized = rawColor.trim();
		if (normalized.startsWith("#")) {
			normalized = normalized.substring(1);
		}
		try {
			return Integer.parseInt(normalized, 16);
		} catch (NumberFormatException exception) {
			return fallbackColor;
		}
	}

	static DustParticleEffect celestialDust(String colorHex, float scale) {
		return new DustParticleEffect(parseHexColor(colorHex, 0xFFFFFF), Math.max(0.1F, scale));
	}

	static void beginCelestialGamaRayBeam(ServerPlayerEntity caster, CelestialGamaRayState state, int currentTick) {
		Vec3d direction = caster.getRotationVector();
		if (direction.lengthSquared() <= 1.0E-6) {
			direction = new Vec3d(0.0, 0.0, 1.0);
		}
		if (CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beamFireManaCostPercent >= 100.0) {
			consumeFullManaBar(caster);
		} else {
			int manaCost = (int) Math.ceil(manaFromPercentExact(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beamFireManaCostPercent));
			spendAbilityCost(caster, manaCost);
		}
		state.phase = CelestialGamaRayPhase.FIRING;
		state.beamOrigin = caster.getEyePos();
		state.beamDirection = direction.normalize();
		state.endTick = currentTick + Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.durationTicks);
		state.nextParticleTick = currentTick;
		state.nextDamageTick = currentTick;
		state.nextSoundTick = currentTick;
		state.nextVisualSyncTick = currentTick;
		state.lockedX = caster.getX();
		state.lockedY = caster.getY();
		state.lockedZ = caster.getZ();
		state.lockedYaw = caster.getYaw();
		state.lockedPitch = caster.getPitch();
		broadcastCelestialGamaRayVisualState(caster, state, currentTick, state.endTick);
		if (CELESTIAL_ALIGNMENT_CONFIG.gamaRay.presentation.firingSoundLayeringEnabled && caster.getEntityWorld() instanceof ServerWorld world) {
			world.playSound(null, state.beamOrigin.x, state.beamOrigin.y, state.beamOrigin.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.15F, 0.8F);
			world.playSound(null, state.beamOrigin.x, state.beamOrigin.y, state.beamOrigin.z, SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 0.9F, 0.65F);
		}
		caster.sendMessage(Text.translatable("message.magic.constellation.celestial_gama_ray.firing"), true);
	}

	static void updateCelestialGamaRayBeam(ServerPlayerEntity caster, CelestialGamaRayState state, int currentTick) {
		if (!(caster.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		if (currentTick >= state.nextVisualSyncTick) {
			broadcastCelestialGamaRayVisualState(caster, state, currentTick, state.endTick);
			state.nextVisualSyncTick = currentTick + 10;
		}

		if (CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.immobilizeCaster) {
			float yaw = CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.aimLockDuringBeam ? state.lockedYaw : caster.getYaw();
			float pitch = CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.aimLockDuringBeam ? state.lockedPitch : caster.getPitch();
			teleportDomainEntity(caster, state.lockedX, state.lockedY, state.lockedZ, yaw, pitch);
			caster.setVelocity(0.0, 0.0, 0.0);
			caster.setOnGround(true);
		}

		if (currentTick >= state.nextParticleTick) {
			spawnCelestialGamaRayBeamParticles(world, state, currentTick);
			state.nextParticleTick = currentTick + Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.particleIntervalTicks);
		}
		if (currentTick >= state.nextSoundTick) {
			world.playSound(null, state.beamOrigin.x, state.beamOrigin.y, state.beamOrigin.z, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.soundVolume, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.soundPitch);
			state.nextSoundTick = currentTick + Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.soundIntervalTicks);
		}

		List<LivingEntity> affected = collectCelestialGamaRayTargets(world, caster, state);
		HashSet<UUID> affectedIds = new HashSet<>();
		for (LivingEntity target : affected) {
			affectedIds.add(target.getUuid());
			if (CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.immobilizeTargets) {
				CelestialBeamPinnedTargetState pinned = state.pinnedTargets.computeIfAbsent(
					target.getUuid(),
					ignored -> new CelestialBeamPinnedTargetState(target.getEntityWorld().getRegistryKey(), target.getX(), target.getY(), target.getZ())
				);
				teleportDomainEntity(target, pinned.lockedX, pinned.lockedY, pinned.lockedZ, target.getYaw(), target.getPitch());
				target.setVelocity(0.0, 0.0, 0.0);
				target.setOnGround(true);
				if (target instanceof MobEntity mob) {
					mob.getNavigation().stop();
				}
			}
		}
		state.pinnedTargets.keySet().removeIf(id -> !affectedIds.contains(id));

		if (currentTick >= state.nextDamageTick) {
			for (LivingEntity target : affected) {
				dealTrackedMagicDamage(
					target,
					caster.getUuid(),
					createTrueMagicDamageSource(world, caster),
					CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.damagePerInterval
				);
			}
			state.nextDamageTick = currentTick + Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.damageIntervalTicks);
		}
	}

	static List<LivingEntity> collectCelestialGamaRayTargets(ServerWorld world, ServerPlayerEntity caster, CelestialGamaRayState state) {
		Vec3d end = state.beamOrigin.add(state.beamDirection.multiply(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.range));
		Box box = new Box(state.beamOrigin, end).expand(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.radius + 1.0);
		ArrayList<LivingEntity> targets = new ArrayList<>();
		for (Entity entity : world.getOtherEntities(caster, box, candidate -> candidate instanceof LivingEntity living && isMagicTargetableEntity(living))) {
			if (!(entity instanceof LivingEntity living)) {
				continue;
			}
			Vec3d feet = new Vec3d(living.getX(), living.getBodyY(0.5), living.getZ());
			Vec3d relative = feet.subtract(state.beamOrigin);
			double projection = relative.dotProduct(state.beamDirection);
			if (projection < 0.0 || projection > CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.range) {
				continue;
			}
			Vec3d nearest = state.beamOrigin.add(state.beamDirection.multiply(projection));
			if (feet.squaredDistanceTo(nearest) <= CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.radius * CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.radius) {
				targets.add(living);
			}
		}
		return targets;
	}

	static void spawnCelestialGamaRayBeamParticles(ServerWorld world, CelestialGamaRayState state, int currentTick) {
		if (!CELESTIAL_ALIGNMENT_CONFIG.visuals.lowFxFallbackMode) {
			return;
		}
		Vec3d start = celestialGamaRayVisualStart(state);
		double range = CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.range + celestialGamaRayVisualBackOffset();
		double spacing = Math.max(0.25, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.sliceSpacing);
		AstralCataclysmBeamParticleEffect coreEffect = new AstralCataclysmBeamParticleEffect(
			parseHexColor(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.coreColorHex, 0xFFF1C7),
			0.95F,
			Math.max(0.2F, (float) (CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.radius * 0.22)),
			8
		);
		AstralCataclysmBeamParticleEffect outerEffect = new AstralCataclysmBeamParticleEffect(
			parseHexColor(CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.outerColorHex, 0x7FD6FF),
			0.75F,
			Math.max(0.2F, (float) (CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.radius * 0.18)),
			10
		);
		for (double travelled = 0.0; travelled <= range + 1.0E-6; travelled += spacing) {
			Vec3d center = start.add(state.beamDirection.multiply(travelled));
			world.spawnParticles(coreEffect, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
			for (int index = 0; index < Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.outerParticleCount); index++) {
				double angle = currentTick * 0.15 + (Math.PI * 2.0 * index / Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.outerParticleCount));
				Vec3d side = perpendicularBeamOffset(state.beamDirection, angle, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beam.radius * 0.85);
				Vec3d point = center.add(side);
				world.spawnParticles(outerEffect, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}
	}

	static double celestialGamaRayVisualBackOffset() {
		return Math.max(0.0, CELESTIAL_ALIGNMENT_CONFIG.gamaRay.beamVisual.beamStartBackOffsetBlocks);
	}

	static Vec3d celestialGamaRayVisualStart(CelestialGamaRayState state) {
		return state.beamOrigin.subtract(state.beamDirection.multiply(celestialGamaRayVisualBackOffset()));
	}

	static Vec3d perpendicularBeamOffset(Vec3d direction, double angle, double radius) {
		Vec3d axisA = Math.abs(direction.y) > 0.9 ? new Vec3d(1.0, 0.0, 0.0) : new Vec3d(0.0, 1.0, 0.0);
		Vec3d right = direction.crossProduct(axisA).normalize();
		Vec3d up = direction.crossProduct(right).normalize();
		return right.multiply(Math.cos(angle) * radius).add(up.multiply(Math.sin(angle) * radius));
	}

	static void updateCelestialAlignmentState(
		MinecraftServer server,
		ServerPlayerEntity caster,
		CelestialAlignmentState state,
		int currentTick
	) {
		ServerWorld world = server.getWorld(state.dimension);
		if (world == null) {
			return;
		}

		if (currentTick >= state.nextVisualTick) {
			spawnCelestialAlignmentVisuals(world, state);
			int interval = CELESTIAL_ALIGNMENT_CONFIG.visuals.reducedConstellationAmbientParticles ? Math.max(2, CELESTIAL_ALIGNMENT_CONFIG.visuals.ambientParticleIntervalTicks) : 1;
			state.nextVisualTick = currentTick + interval;
		}

		List<LivingEntity> livingInside = collectLivingEntitiesInCelestialAlignment(world, state, caster);
		HashSet<UUID> currentLivingIds = new HashSet<>();
		for (LivingEntity living : livingInside) {
			currentLivingIds.add(living.getUuid());
		}

		for (UUID previousId : new HashSet<>(state.trackedLivingIds)) {
			if (!currentLivingIds.contains(previousId)) {
				onCelestialAlignmentEntityLeft(server, state, previousId);
			}
		}
		state.trackedLivingIds.clear();
		state.trackedLivingIds.addAll(currentLivingIds);

		for (LivingEntity living : livingInside) {
			refreshCelestialMovementSpeedModifier(living);
		}

		if (state.constellation == CelestialAlignmentConstellation.SAGITTA) {
			updateSagittaConstellation(world, caster, state, currentTick);
		}
		if (state.constellation == CelestialAlignmentConstellation.AQUILA) {
			updateAquilaConstellation(world, caster, state, currentTick);
		}
		if (state.constellation == CelestialAlignmentConstellation.CRATER && currentTick >= state.nextConstellationTick) {
			state.nextConstellationTick = currentTick + Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.crater.siphonRippleIntervalTicks);
			spawnCraterSiphonEffects(world, state);
		}
	}

	static void releaseCelestialAlignmentState(MinecraftServer server, CelestialAlignmentState state, boolean expired) {
		if (server == null || state == null) {
			return;
		}

		ServerWorld world = server.getWorld(state.dimension);
		if (world != null && state.constellation == CelestialAlignmentConstellation.GEMINI) {
			releaseGeminiRecordedDamage(server, world, state);
		}

		for (UUID trackedId : new HashSet<>(state.trackedLivingIds)) {
			onCelestialAlignmentEntityLeft(server, state, trackedId);
		}
		state.trackedLivingIds.clear();
		state.geminiRecordedDamage.clear();
		state.aquilaStars.clear();
		state.sagittaStrikes.clear();

		ServerPlayerEntity caster = server.getPlayerManager().getPlayer(state.ownerId);
		if (caster != null) {
			clearCelestialAlignmentBanner(caster);
		}
	}

	static void onCelestialAlignmentEntityLeft(MinecraftServer server, CelestialAlignmentState state, UUID entityId) {
		state.geminiRecordedDamage.remove(entityId);
		clearCelestialDelayQueueIfUnaffected(server, entityId, state);

		ServerWorld world = server.getWorld(state.dimension);
		if (world == null) {
			return;
		}
		Entity entity = world.getEntity(entityId);
		if (entity instanceof LivingEntity living) {
			refreshCelestialMovementSpeedModifier(living);
		}
	}

	static List<LivingEntity> collectLivingEntitiesInCelestialAlignment(
		ServerWorld world,
		CelestialAlignmentState state,
		ServerPlayerEntity caster
	) {
		Box searchBox = new Box(
			state.center.x - state.radius,
			state.minY,
			state.center.z - state.radius,
			state.center.x + state.radius,
			state.maxY,
			state.center.z + state.radius
		);
		ArrayList<LivingEntity> livingEntities = new ArrayList<>();
		if (isInsideCelestialAlignmentArea(state, caster)) {
			livingEntities.add(caster);
		}
		for (Entity entity : world.getOtherEntities(caster, searchBox, candidate -> candidate instanceof LivingEntity living && isMagicTargetableEntity(living))) {
			if (entity instanceof LivingEntity living && isInsideCelestialAlignmentArea(state, living)) {
				livingEntities.add(living);
			}
		}
		return livingEntities;
	}

	static boolean isInsideCelestialAlignmentArea(CelestialAlignmentState state, Entity entity) {
		return entity != null
			&& entity.getEntityWorld().getRegistryKey() == state.dimension
			&& isInsideCelestialAlignmentArea(state, entity.getX(), entity.getBodyY(0.5), entity.getZ());
	}

	static boolean isInsideCelestialAlignmentArea(CelestialAlignmentState state, double x, double y, double z) {
		if (y < state.minY - 1.0E-6 || y > state.maxY + 1.0E-6) {
			return false;
		}
		double dx = x - state.center.x;
		double dz = z - state.center.z;
		return dx * dx + dz * dz <= state.radius * state.radius + 1.0E-6;
	}

	static void spawnCelestialAlignmentVisuals(ServerWorld world, CelestialAlignmentState state) {
		CelestialPattern pattern = celestialPattern(state.constellation);
		double buildProgress = MathHelper.clamp(
			(currentTickForConstellation(world) - state.startTick + 1.0) / Math.max(1.0, CELESTIAL_ALIGNMENT_CONFIG.visuals.placementBuildInTicks),
			0.0,
			1.0
		);
		if (buildProgress < 0.28) {
			world.spawnParticles(ParticleTypes.GLOW, state.center.x, state.minY + 0.12, state.center.z, 2, 0.18, 0.04, 0.18, 0.0);
		}

		double nodeProgress = MathHelper.clamp((buildProgress - 0.12) / 0.28, 0.0, 1.0);
		double lineProgress = MathHelper.clamp((buildProgress - 0.25) / 0.3, 0.0, 1.0);
		double ringProgress = MathHelper.clamp((buildProgress - 0.4) / 0.22, 0.0, 1.0);
		double topProgress = MathHelper.clamp((buildProgress - 0.58) / 0.18, 0.0, 1.0);
		double verticalProgress = MathHelper.clamp((buildProgress - 0.72) / 0.2, 0.0, 1.0);

		spawnCelestialAlignmentPattern(world, state, pattern, state.minY, nodeProgress, lineProgress);
		if (topProgress > 0.0) {
			spawnCelestialAlignmentPattern(world, state, pattern, state.maxY, Math.min(nodeProgress, topProgress), Math.min(lineProgress, topProgress));
		}
		if (ringProgress > 0.0) {
			spawnCelestialAlignmentRing(world, state, state.minY, false, ringProgress);
		}
		if (topProgress > 0.0) {
			spawnCelestialAlignmentRing(world, state, state.maxY, true, topProgress);
		}
		if (verticalProgress > 0.0) {
			spawnCelestialAlignmentVerticalLinks(world, state, pattern, verticalProgress);
		}
		spawnCelestialAmbientEffects(world, state);
	}

	static int currentTickForConstellation(ServerWorld world) {
		return world.getServer() == null ? 0 : world.getServer().getTicks();
	}

	static void spawnCelestialAlignmentRing(ServerWorld world, CelestialAlignmentState state, double y, boolean top, double progress) {
		int count = top ? CELESTIAL_ALIGNMENT_CONFIG.visuals.topRingParticleCount : CELESTIAL_ALIGNMENT_CONFIG.visuals.groundRingParticleCount;
		if (count <= 0) {
			return;
		}

		DustParticleEffect primary = celestialDust(
			top ? CELESTIAL_ALIGNMENT_CONFIG.visuals.topRingPrimaryColorHex : CELESTIAL_ALIGNMENT_CONFIG.visuals.groundRingPrimaryColorHex,
			CELESTIAL_ALIGNMENT_CONFIG.visuals.ringParticleScale
		);
		DustParticleEffect secondary = celestialDust(
			top ? CELESTIAL_ALIGNMENT_CONFIG.visuals.topRingSecondaryColorHex : CELESTIAL_ALIGNMENT_CONFIG.visuals.groundRingSecondaryColorHex,
			CELESTIAL_ALIGNMENT_CONFIG.visuals.ringParticleScale
		);
		int drawCount = Math.max(1, MathHelper.ceil(count * MathHelper.clamp(progress, 0.0, 1.0)));
		for (int index = 0; index < drawCount; index++) {
			double angle = (Math.PI * 2.0 * index) / drawCount;
			double x = state.center.x + Math.cos(angle) * state.radius;
			double z = state.center.z + Math.sin(angle) * state.radius;
			world.spawnParticles(index % 2 == 0 ? primary : secondary, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
		}
		if (CELESTIAL_ALIGNMENT_CONFIG.visuals.ringShimmerEnabled && currentTickForConstellation(world) % Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.visuals.ringShimmerIntervalTicks) == 0) {
			DustParticleEffect shimmer = celestialDust(CELESTIAL_ALIGNMENT_CONFIG.visuals.shimmerColorHex, CELESTIAL_ALIGNMENT_CONFIG.visuals.ringParticleScale * 0.8F);
			double angle = world.random.nextDouble() * Math.PI * 2.0;
			double x = state.center.x + Math.cos(angle) * state.radius;
			double z = state.center.z + Math.sin(angle) * state.radius;
			world.spawnParticles(shimmer, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
		}
	}

	static void spawnCelestialAlignmentPattern(
		ServerWorld world,
		CelestialAlignmentState state,
		CelestialPattern pattern,
		double y,
		double nodeProgress,
		double lineProgress
	) {
		DustParticleEffect nodeParticle = celestialDust(celestialAlignmentColorHex(state.constellation), CELESTIAL_ALIGNMENT_CONFIG.visuals.starNodeParticleScale);
		DustParticleEffect lineParticle = celestialDust(celestialAlignmentColorHex(state.constellation), CELESTIAL_ALIGNMENT_CONFIG.visuals.connectionParticleScale);
		int nodeCount = MathHelper.clamp(MathHelper.ceil(pattern.nodes.size() * MathHelper.clamp(nodeProgress, 0.0, 1.0)), 0, pattern.nodes.size());
		for (int index = 0; index < nodeCount; index++) {
			Vec3d node = pattern.nodes.get(index);
			Vec3d point = celestialPatternPoint(state, node, y);
			world.spawnParticles(nodeParticle, point.x, point.y, point.z, Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.visuals.starNodeParticleCount), 0.03, 0.03, 0.03, 0.0);
		}
		int edgeCount = MathHelper.clamp(MathHelper.ceil(pattern.edges.size() * MathHelper.clamp(lineProgress, 0.0, 1.0)), 0, pattern.edges.size());
		for (int index = 0; index < edgeCount; index++) {
			CelestialPatternEdge edge = pattern.edges.get(index);
			Vec3d start = celestialPatternPoint(state, pattern.nodes.get(edge.startIndex), y);
			Vec3d end = celestialPatternPoint(state, pattern.nodes.get(edge.endIndex), y);
			if ("travel".equals(CELESTIAL_ALIGNMENT_CONFIG.visuals.lineDrawMode)) {
				double segmentProgress = MathHelper.clamp(lineProgress * pattern.edges.size() - index, 0.0, 1.0);
				spawnCelestialAlignmentLine(world, start, end, lineParticle, CELESTIAL_ALIGNMENT_CONFIG.visuals.connectionLineParticleCount, 0.0, segmentProgress);
			} else {
				spawnCelestialAlignmentLine(world, start, end, lineParticle, CELESTIAL_ALIGNMENT_CONFIG.visuals.connectionLineParticleCount, 0.0, 1.0);
			}
		}
	}

	static void spawnCelestialAlignmentVerticalLinks(ServerWorld world, CelestialAlignmentState state, CelestialPattern pattern, double progress) {
		DustParticleEffect particle = celestialDust(CELESTIAL_ALIGNMENT_CONFIG.visuals.verticalThreadColorHex, CELESTIAL_ALIGNMENT_CONFIG.visuals.verticalThreadParticleScale);
		int nodeCount = MathHelper.clamp(MathHelper.ceil(pattern.nodes.size() * MathHelper.clamp(progress, 0.0, 1.0)), 0, pattern.nodes.size());
		for (int index = 0; index < nodeCount; index++) {
			Vec3d node = pattern.nodes.get(index);
			spawnCelestialAlignmentLine(
				world,
				celestialPatternPoint(state, node, state.minY),
				celestialPatternPoint(state, node, state.maxY),
				particle,
				Math.max(1, CELESTIAL_ALIGNMENT_CONFIG.visuals.verticalThreadCount),
				0.0,
				MathHelper.clamp(progress, 0.0, 1.0)
			);
		}
	}

	static void spawnCelestialAlignmentLine(
		ServerWorld world,
		Vec3d start,
		Vec3d end,
		ParticleEffect particle,
		int count,
		double startProgress,
		double endProgress
	) {
		if (count <= 0) {
			return;
		}

		Vec3d delta = end.subtract(start);
		double clampedStart = MathHelper.clamp(startProgress, 0.0, 1.0);
		double clampedEnd = MathHelper.clamp(endProgress, clampedStart, 1.0);
		for (int index = 0; index <= count; index++) {
			double progress = MathHelper.lerp(index / (double) Math.max(1, count), clampedStart, clampedEnd);
			Vec3d point = start.add(delta.multiply(progress));
			world.spawnParticles(particle, point.x, point.y, point.z, 1, 0.01, 0.01, 0.01, 0.0);
		}
	}

	static Vec3d celestialPatternPoint(CelestialAlignmentState state, Vec3d normalizedPoint, double y) {
		return new Vec3d(state.center.x + normalizedPoint.x * state.radius, y, state.center.z + normalizedPoint.z * state.radius);
	}
}


