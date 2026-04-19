package net.evan.magic.client.render;

import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.ModifierLayer;
import com.zigythebird.playeranimcore.animation.layered.PlayerAnimationFrame;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.evan.magic.Magic;
import net.evan.magic.config.MagicConfig;
import net.evan.magic.mixin.WorldRenderStateAccessorMixin;
import net.evan.magic.network.payload.CelestialGamaRayVisualPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class CelestialGamaRayPresentationManager {
	private static final Identifier HUD_LAYER = Identifier.of(Magic.MOD_ID, "celestial_gama_ray_presentation");
	private static final Identifier PAL_LAYER_ID = Identifier.of(Magic.MOD_ID, "celestial_gama_ray_pose");
	private static final Identifier BEAM_TEXTURE = Identifier.of("minecraft", "textures/particle/glow.png");
	private static final Map<UUID, VisualState> STATES = new HashMap<>();
	private static final RawAnimation CHARGE_ANIMATION = RawAnimation.begin().thenLoop("animation.celestial_gama_ray_charge_construct.charge");
	private static final RawAnimation FIRING_ANIMATION = RawAnimation.begin().thenLoop("animation.celestial_gama_ray_charge_construct.fire");
	private static final ChargeConstructRenderer CHARGE_CONSTRUCT_RENDERER = new ChargeConstructRenderer();
	private static boolean initialized;
	private static boolean chargeConstructRenderingAvailable = true;
	private static int localFlashTicks;
	private static int localFlashDuration;

	private CelestialGamaRayPresentationManager() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
			PAL_LAYER_ID,
			150,
			player -> new ModifierLayer<>(new CelestialGamaRayPoseAnimation(player instanceof PlayerEntity playerEntity ? playerEntity : null))
		);
		WorldRenderEvents.END_MAIN.register(CelestialGamaRayPresentationManager::renderWorld);
		ClientTickEvents.END_CLIENT_TICK.register(CelestialGamaRayPresentationManager::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, HUD_LAYER, CelestialGamaRayPresentationManager::renderHud);
		initialized = true;
	}

	public static void applyVisualState(CelestialGamaRayVisualPayload payload) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			return;
		}

		if (!payload.active()) {
			STATES.remove(payload.casterUuid());
			return;
		}

		VisualPhase phase = VisualPhase.fromId(payload.phase());
		VisualState state = STATES.computeIfAbsent(payload.casterUuid(), VisualState::new);
		VisualPhase previousPhase = state.phase;
		state.phase = phase;
		state.phaseStartTick = payload.phaseStartTick();
		state.phaseEndTick = payload.phaseEndTick();
		state.beamOrigin = new Vec3d(payload.originX(), payload.originY(), payload.originZ());
		state.beamDirection = new Vec3d(payload.directionX(), payload.directionY(), payload.directionZ());
		state.lockedYaw = payload.lockedYaw();
		state.lockedPitch = payload.lockedPitch();
		state.lastSeenWorldTick = (int) client.world.getTime();
		int phaseDuration = Math.max(0, payload.phaseEndTick() - payload.phaseStartTick());
		state.localPhaseExpireTick = state.lastSeenWorldTick + phaseDuration;

		if (
			client.player != null
			&& client.player.getUuid().equals(payload.casterUuid())
			&& phase == VisualPhase.FIRING
			&& previousPhase != VisualPhase.FIRING
		) {
			localFlashTicks = 8;
			localFlashDuration = 8;
		}
	}

	public static void clear() {
		STATES.clear();
		localFlashTicks = 0;
		localFlashDuration = 0;
	}

	public static float getLocalFovMultiplier(float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null) {
			return 1.0F;
		}

		MagicConfig.ConstellationCelestialAlignmentConfig config = config();
		if (!config.visuals.allowFovPulse || !config.gamaRay.presentation.allowFovPulse) {
			return 1.0F;
		}

		VisualState state = STATES.get(client.player.getUuid());
		if (state == null || state.phase != VisualPhase.FIRING) {
			return 1.0F;
		}

		float age = client.world.getTime() + tickDelta - state.phaseStartTick;
		float burst = MathHelper.clamp(1.0F - age / 10.0F, 0.0F, 1.0F);
		float sustain = 0.35F + 0.65F * (0.5F + 0.5F * MathHelper.sin((client.world.getTime() + tickDelta) * 0.18F));
		return 1.0F + config.gamaRay.presentation.fovPulseAmount * Math.max(burst, sustain * 0.4F);
	}

	public static float getLocalCameraShake(float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null) {
			return 0.0F;
		}

		MagicConfig.ConstellationCelestialAlignmentConfig config = config();
		if (!config.visuals.allowCameraShake || !config.gamaRay.presentation.allowCameraShake) {
			return 0.0F;
		}

		VisualState state = STATES.get(client.player.getUuid());
		if (state == null || state.phase != VisualPhase.FIRING) {
			return 0.0F;
		}

		float age = client.world.getTime() + tickDelta - state.phaseStartTick;
		float burst = MathHelper.clamp(1.0F - age / 8.0F, 0.0F, 1.0F) * config.gamaRay.presentation.cameraShakeStartStrength;
		float sustain = config.gamaRay.presentation.cameraShakeSustainStrength * (0.55F + 0.45F * MathHelper.sin((client.world.getTime() + tickDelta) * 0.7F));
		return burst + sustain;
	}

	private static void tick(MinecraftClient client) {
		if (client.world == null) {
			clear();
			return;
		}

		if (localFlashTicks > 0) {
			localFlashTicks--;
		}

		int worldTick = (int) client.world.getTime();
		Iterator<Map.Entry<UUID, VisualState>> iterator = STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, VisualState> entry = iterator.next();
			VisualState state = entry.getValue();
			boolean pastPhaseEnd = state.phase != VisualPhase.INACTIVE
				&& state.localPhaseExpireTick > state.lastSeenWorldTick
				&& worldTick > state.localPhaseExpireTick + 5;
			if (pastPhaseEnd || (state.phase == VisualPhase.INACTIVE && worldTick - state.lastSeenWorldTick > 40)) {
				iterator.remove();
				continue;
			}
			PlayerEntity player = client.world.getPlayerByUuid(entry.getKey());
			if (player == null || !player.isAlive()) {
				iterator.remove();
				continue;
			}
			if (state.phase == VisualPhase.FIRING && player instanceof AbstractClientPlayerEntity caster) {
				spawnBeamCylinderParticles(client, caster, state, worldTick);
			}
		}
	}

	private static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		if (localFlashTicks <= 0 || localFlashDuration <= 0) {
			return;
		}

		MagicConfig.ConstellationCelestialAlignmentConfig config = config();
		if (!config.gamaRay.presentation.allowScreenFlash) {
			return;
		}

		float progress = localFlashTicks / (float) localFlashDuration;
		float strength = config.gamaRay.presentation.screenFlashStrength * progress;
		int alpha = Math.max(0, Math.min(255, (int) (strength * 255.0F)));
		int color = (alpha << 24) | 0xD8F3FF;
		context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), color);
	}

	private static void renderWorld(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = client.world;
		if (world == null || STATES.isEmpty()) {
			return;
		}

		CameraRenderState cameraState = ((WorldRenderStateAccessorMixin) context.worldState()).magic$getCameraRenderState();
		float tickDelta = client.getRenderTickCounter().getTickProgress(false);
		Vec3d cameraPos = client.player == null ? Vec3d.ZERO : client.player.getCameraPosVec(tickDelta);
		MagicConfig.ConstellationCelestialAlignmentConfig config = config();
		double maxDistanceSq = config.gamaRay.beamVisual.beamMaxVisibleDistance * config.gamaRay.beamVisual.beamMaxVisibleDistance;

		for (VisualState state : STATES.values()) {
			PlayerEntity player = world.getPlayerByUuid(state.casterId);
			if (!(player instanceof AbstractClientPlayerEntity caster) || !caster.isAlive()) {
				continue;
			}
			if (caster.squaredDistanceTo(cameraPos) > maxDistanceSq && player != client.player) {
				continue;
			}
			boolean localCaster = caster == client.player;

			if (state.phase == VisualPhase.CHARGING) {
				if (localCaster) {
					renderWindupSigils(context, cameraPos, caster, state, tickDelta);
					renderChargeConstruct(context, cameraState, cameraPos, caster, state, tickDelta, false);
				}
			} else if (state.phase == VisualPhase.FIRING) {
				if (localCaster && config.gamaRay.chargeConstruct.constructPersistDuringBeam) {
					renderChargeConstruct(context, cameraState, cameraPos, caster, state, tickDelta, true);
				}
				renderBeam(context, cameraPos, world, caster, state, tickDelta);
			}
		}
	}

	private static void renderChargeConstruct(
		WorldRenderContext context,
		CameraRenderState cameraState,
		Vec3d cameraPos,
		AbstractClientPlayerEntity caster,
		VisualState state,
		float tickDelta,
		boolean firing
	) {
		MagicConfig.ConstellationCelestialAlignmentConfig config = config();
		MagicConfig.ChargeConstructConfig constructConfig = config.gamaRay.chargeConstruct;
		if (!constructEnabled(config, caster)) {
			return;
		}

		Vec3d direction = state.phase == VisualPhase.FIRING && state.beamDirection.lengthSquared() > 1.0E-6
			? state.beamDirection.normalize()
			: caster.getRotationVec(tickDelta).normalize();
		Vec3d right = horizontalRight(direction);
		Vec3d origin = caster.getEyePos()
			.add(direction.multiply(constructConfig.constructOffsetForward))
			.add(0.0, constructConfig.constructOffsetUp, 0.0)
			.add(right.multiply(0.36));
		MatrixStack matrices = context.matrices();
		matrices.push();
		matrices.translate(origin.x - cameraPos.x, origin.y - cameraPos.y, origin.z - cameraPos.z);
		state.construct.firing = firing;
		if (chargeConstructRenderingAvailable) {
			try {
				CHARGE_CONSTRUCT_RENDERER.withScale((float) constructConfig.constructScale);
				CHARGE_CONSTRUCT_RENDERER.performRenderPass(
					state.construct,
					null,
					matrices,
					context.commandQueue(),
					cameraState,
					LightmapTextureManager.MAX_LIGHT_COORDINATE,
					Math.round(tickDelta * 20.0F)
				);
			} catch (RuntimeException exception) {
				chargeConstructRenderingAvailable = false;
				Magic.LOGGER.warn("Disabling Celestial Gama Ray GeckoLib charge construct rendering and falling back to particle visuals", exception);
			}
		}
		matrices.pop();
		renderConstructOrbitals(context, cameraPos, origin, direction, constructConfig, tickDelta);
	}

	private static void renderConstructOrbitals(
		WorldRenderContext context,
		Vec3d cameraPos,
		Vec3d origin,
		Vec3d direction,
		MagicConfig.ChargeConstructConfig constructConfig,
		float tickDelta
	) {
		VertexConsumer consumer = context.consumers().getBuffer(RenderLayers.entityTranslucent(BEAM_TEXTURE));
		Matrix4f matrix = context.matrices().peek().getPositionMatrix();
		Vec3d right = horizontalRight(direction);
		Vec3d up = direction.crossProduct(right).normalize();
		float alpha = MathHelper.clamp(constructConfig.constructOpacity, 0.0F, 1.0F);
		double time = MinecraftClient.getInstance().world == null ? 0.0 : MinecraftClient.getInstance().world.getTime() + tickDelta;

		for (int ring = 0; ring < constructConfig.constructRingCount; ring++) {
			double radius = 0.45 + ring * 0.22;
			double spin = time * constructConfig.constructSpinSpeed * 0.03 + ring * 0.55;
			renderRing(consumer, matrix, cameraPos, origin, right, up, radius, 18, alpha * (0.7F - ring * 0.12F), 0xD8F6FF, spin);
		}

		for (int shard = 0; shard < constructConfig.constructShardCount; shard++) {
			double angle = time * constructConfig.constructSpinSpeed * 0.05 + shard * (Math.PI * 2.0 / Math.max(1, constructConfig.constructShardCount));
			double radius = 0.7 + 0.12 * Math.sin(time * constructConfig.constructPulseSpeed + shard);
			Vec3d point = origin
				.add(right.multiply(Math.cos(angle) * radius))
				.add(up.multiply(Math.sin(angle) * radius))
				.add(direction.multiply(Math.sin(time * 0.09 + shard) * 0.18));
			renderGlowQuad(consumer, matrix, point.subtract(cameraPos), right, up, 0.12, alpha * 0.95F, 0xFFF1C7);
		}
	}

	private static void renderWindupSigils(WorldRenderContext context, Vec3d cameraPos, AbstractClientPlayerEntity caster, VisualState state, float tickDelta) {
		MagicConfig.ConstellationCelestialAlignmentConfig config = config();
		MagicConfig.PresentationConfig presentation = config.gamaRay.presentation;
		if (!presentation.windupSigilsEnabled) {
			return;
		}

		VertexConsumer consumer = context.consumers().getBuffer(RenderLayers.entityTranslucent(BEAM_TEXTURE));
		Matrix4f matrix = context.matrices().peek().getPositionMatrix();
		Vec3d center = new Vec3d(caster.getX(), caster.getY(), caster.getZ()).add(0.0, 1.2, 0.0);
		Vec3d forward = caster.getRotationVec(tickDelta).normalize();
		Vec3d right = horizontalRight(forward);
		Vec3d up = forward.crossProduct(right).normalize();
		double time = caster.getEntityWorld().getTime() + tickDelta;
		for (int index = 0; index < presentation.windupSigilCount; index++) {
			double radius = presentation.windupSigilRadius + index * 0.28;
			double y = 0.15 + index * 0.22;
			Vec3d ringCenter = center.add(0.0, y, 0.0);
			renderRing(
				consumer,
				matrix,
				cameraPos,
				ringCenter,
				right,
				up,
				radius,
				22,
				0.14F,
				index % 2 == 0 ? 0x7FD6FF : 0xFFF1C7,
				time * 0.05 + index * 0.45
			);
		}
	}

	private static void renderBeam(
		WorldRenderContext context,
		Vec3d cameraPos,
		ClientWorld world,
		AbstractClientPlayerEntity caster,
		VisualState state,
		float tickDelta
	) {
		if (state.beamDirection.lengthSquared() <= 1.0E-6) {
			return;
		}

		MagicConfig.ConstellationCelestialAlignmentConfig config = config();
		MagicConfig.BeamVisualConfig beamVisual = config.gamaRay.beamVisual;
		int coreColor = parseHexColor(config.gamaRay.beam.coreColorHex, 0xFFE08A);
		int outerColor = parseHexColor(config.gamaRay.beam.outerColorHex, 0x58C8FF);
		int accentColor = parseHexColor(config.gamaRay.beam.accentColorHex, 0x7BA7FF);
		VertexConsumer consumer = context.consumers().getBuffer(RenderLayers.entityTranslucent(BEAM_TEXTURE));
		VertexConsumer accentConsumer = context.consumers().getBuffer(RenderLayers.entityTranslucent(BEAM_TEXTURE));
		Matrix4f matrix = context.matrices().peek().getPositionMatrix();
		Vec3d direction = state.beamDirection.normalize();
		Vec3d visualOrigin = visualBeamOrigin(state, direction);
		double visualRange = config.gamaRay.beam.range + beamVisual.beamStartBackOffsetBlocks;
		Vec3d start = clipBeamStartForCamera(
			visualOrigin,
			cameraPos,
			direction,
			visualRange,
			config.gamaRay.beam.radius,
			beamVisual,
			caster
		);
		Vec3d end = state.beamOrigin.add(direction.multiply(config.gamaRay.beam.range));
		double visibleLength = start.distanceTo(end);
		if (visibleLength <= 1.0E-6) {
			return;
		}
		int density = effectiveDensity(config, caster);
		int shellPlanes = Math.max(34, density + 24);
		int accentPlanes = Math.max(28, density + 18);
		int corePlanes = Math.max(18, density / 2 + 12);

		if (beamVisual.beamOuterShellEnabled) {
			renderBeamTube(
				consumer,
				matrix,
				cameraPos,
				start,
				end,
				direction,
				beamVisual.beamOuterShellThickness * 1.34,
				Math.min(1.0F, beamVisual.beamOuterShellOpacity * 1.42F),
				outerColor,
				shellPlanes,
				Math.max(0.25, beamVisual.beamSliceStep * 0.28)
			);
			renderBeamTube(
				consumer,
				matrix,
				cameraPos,
				start,
				end,
				direction,
				beamVisual.beamOuterShellThickness * 1.02,
				Math.min(1.0F, beamVisual.beamOuterShellOpacity * 1.05F),
				accentColor,
				accentPlanes,
				Math.max(0.25, beamVisual.beamSliceStep * 0.24)
			);
		}
		if (beamVisual.beamCoreEnabled) {
			float coreAlpha = (float) MathHelper.clamp(0.52 + beamVisual.beamCoreBrightness * 0.24, 0.0, 1.0);
			renderBeamTube(
				consumer,
				matrix,
				cameraPos,
				start,
				end,
				direction,
				beamVisual.beamCoreThickness * 1.08,
				coreAlpha,
				coreColor,
				corePlanes,
				Math.max(0.25, beamVisual.beamSliceStep * 0.2)
			);
			renderBeamTube(
				consumer,
				matrix,
				cameraPos,
				start,
				end,
				direction,
				beamVisual.beamCoreThickness * 0.82,
				Math.min(1.0F, coreAlpha + 0.08F),
				coreColor,
				corePlanes,
				Math.max(0.25, beamVisual.beamSliceStep * 0.18)
			);
		}
		if (beamVisual.beamRibbonEnabled && beamVisual.beamRibbonCount > 0) {
			renderBeamRibbons(consumer, matrix, cameraPos, start, direction, visibleLength, beamVisual, density, tickDelta, accentColor);
		}
		if (beamVisual.beamFlowEnabled && beamVisual.beamFlowDensity > 0) {
			renderBeamFlow(accentConsumer, matrix, cameraPos, start, direction, visibleLength, beamVisual, density, tickDelta, outerColor, accentColor);
		}
		if (beamVisual.beamInteriorParticlesEnabled && beamVisual.beamInteriorParticleCount > 0) {
			renderBeamInteriorParticles(
				accentConsumer,
				matrix,
				cameraPos,
				start,
				direction,
				visibleLength,
				beamVisual,
				density,
				tickDelta,
				blendColor(outerColor, 0xE4F8FF, 0.52F)
			);
		}
		if (beamVisual.beamOriginBurstEnabled) {
			renderOriginBurst(accentConsumer, matrix, cameraPos, state, direction, beamVisual, tickDelta, coreColor, outerColor);
		}
		if (beamVisual.beamImpactPressureEffectsEnabled || beamVisual.beamTargetBindVisualsEnabled) {
			renderTargetPressureEffects(accentConsumer, matrix, cameraPos, world, caster, state, beamVisual, tickDelta, coreColor, accentColor);
		}
	}

	private static boolean isLocalFirstPerson(PlayerEntity player) {
		MinecraftClient client = MinecraftClient.getInstance();
		return player != null
			&& client.player != null
			&& player.getUuid().equals(client.player.getUuid())
			&& client.options.getPerspective() == Perspective.FIRST_PERSON;
	}

	private static void renderBeamTube(
		VertexConsumer consumer,
		Matrix4f matrix,
		Vec3d cameraPos,
		Vec3d start,
		Vec3d end,
		Vec3d direction,
		double thickness,
		float alpha,
		int color,
		int planeCount,
		double sliceStep
	) {
		double length = start.distanceTo(end);
		int slices = Math.max(1, (int) Math.ceil(length / Math.max(0.25, sliceStep)));
		int bandCount = Math.max(1, Math.min(6, (int) Math.round(Math.sqrt(Math.max(1, planeCount)) * 0.45)));
		for (int band = 0; band < bandCount; band++) {
			double roll = bandCount <= 1 ? 0.0 : (Math.PI * band / bandCount);
			Vec3d lastCenter = start;
			for (int slice = 1; slice <= slices; slice++) {
				Vec3d center = start.add(direction.multiply(length * slice / slices));
				Vec3d axis = beamFacingAxis(lastCenter, center, cameraPos, direction, roll);
				renderRibbonSegment(
					consumer,
					matrix,
					lastCenter.subtract(cameraPos),
					center.subtract(cameraPos),
					axis,
					thickness * 0.5,
					alpha,
					color
				);
				lastCenter = center;
			}
		}
	}

	private static Vec3d clipBeamStartForCamera(
		Vec3d origin,
		Vec3d cameraPos,
		Vec3d direction,
		double range,
		double radius,
		MagicConfig.BeamVisualConfig beamVisual,
		PlayerEntity caster
	) {
		double visualRadius = beamEnvelopeRadius(radius, beamVisual);
		boolean localFirstPerson = isLocalFirstPerson(caster);
		double startAlong = localFirstPerson ? Math.max(2.6, visualRadius + 1.5) : 0.0;
		Vec3d toCamera = cameraPos.subtract(origin);
		double cameraAlong = toCamera.dotProduct(direction);
		if (cameraAlong > -visualRadius && cameraAlong < range) {
			Vec3d radialOffset = toCamera.subtract(direction.multiply(cameraAlong));
			double paddedRadius = visualRadius + 0.75;
			if (localFirstPerson || radialOffset.lengthSquared() <= paddedRadius * paddedRadius) {
				startAlong = Math.max(startAlong, cameraAlong + paddedRadius);
			}
		}
		return origin.add(direction.multiply(MathHelper.clamp(startAlong, 0.0, range)));
	}

	private static void spawnBeamCylinderParticles(MinecraftClient client, AbstractClientPlayerEntity caster, VisualState state, int worldTick) {
		MagicConfig.ConstellationCelestialAlignmentConfig config = config();
		MagicConfig.BeamVisualConfig beamVisual = config.gamaRay.beamVisual;
		if (!beamVisual.beamInteriorVanillaParticlesEnabled) {
			return;
		}
		int interval = Math.max(1, beamVisual.beamInteriorVanillaParticleIntervalTicks);
		if (worldTick < state.nextInteriorParticleTick) {
			return;
		}
		state.nextInteriorParticleTick = worldTick + interval;
		if (client.world == null || state.beamDirection.lengthSquared() <= 1.0E-6) {
			return;
		}

		Vec3d direction = state.beamDirection.normalize();
		Vec3d right = horizontalRight(direction);
		Vec3d up = direction.crossProduct(right).normalize();
		Vec3d origin = visualBeamOrigin(state, direction);
		double range = config.gamaRay.beam.range + beamVisual.beamStartBackOffsetBlocks;
		double radius = Math.max(0.2, config.gamaRay.beam.radius * beamVisual.beamInteriorParticleRadiusFactor);
		int density = effectiveDensity(config, caster);
		int count = Math.max(0, Math.min(beamVisual.beamInteriorVanillaParticleCount, density * 2));
		double speed = Math.max(0.0, beamVisual.beamInteriorVanillaParticleSpeed);
		for (int index = 0; index < count; index++) {
			double seed = worldTick * 0.173 + index * 1.371;
			double along = ((seed * 0.19) - Math.floor(seed * 0.19)) * range;
			double angle = seed * 2.7;
			double radialFactor = 0.18 + 0.72 * ((((seed * 0.43) - Math.floor(seed * 0.43))));
			Vec3d offset = right.multiply(Math.cos(angle) * radius * radialFactor)
				.add(up.multiply(Math.sin(angle) * radius * radialFactor));
			Vec3d position = origin.add(direction.multiply(along)).add(offset);
			Vec3d velocity = direction.multiply(speed * (0.65 + (index % 4) * 0.12))
				.add(offset.normalize().multiply(speed * 0.12));
			if ((index & 1) == 0) {
				client.particleManager.addParticle(
					new DustParticleEffect(blendColor(0x6FCFFF, 0xFFFFFF, 0.35F), (float) Math.max(0.2, beamVisual.beamInteriorVanillaParticleScale)),
					position.x,
					position.y,
					position.z,
					velocity.x,
					velocity.y,
					velocity.z
				);
			} else {
				client.particleManager.addParticle(
					ParticleTypes.END_ROD,
					position.x,
					position.y,
					position.z,
					velocity.x * 0.35,
					velocity.y * 0.35,
					velocity.z * 0.35
				);
			}
		}
	}

	private static Vec3d visualBeamOrigin(VisualState state, Vec3d direction) {
		return state.beamOrigin.subtract(direction.multiply(Math.max(0.0, config().gamaRay.beamVisual.beamStartBackOffsetBlocks)));
	}

	private static void renderBeamRibbons(
		VertexConsumer consumer,
		Matrix4f matrix,
		Vec3d cameraPos,
		Vec3d start,
		Vec3d direction,
		double range,
		MagicConfig.BeamVisualConfig beamVisual,
		int density,
		float tickDelta,
		int ribbonColor
	) {
		Vec3d right = horizontalRight(direction);
		Vec3d up = direction.crossProduct(right).normalize();
		double time = MinecraftClient.getInstance().world == null ? 0.0 : MinecraftClient.getInstance().world.getTime() + tickDelta;
		int ribbons = Math.min(beamVisual.beamRibbonCount, Math.max(1, density));
		int steps = Math.max(6, density * 4);
		for (int ribbon = 0; ribbon < ribbons; ribbon++) {
			Vec3d previous = null;
			for (int step = 0; step <= steps; step++) {
				double progress = step / (double) steps;
				double along = progress * range;
				double angle = time * beamVisual.beamRibbonAngularSpeed + ribbon * (Math.PI * 2.0 / ribbons) + progress * 8.0;
				Vec3d offset = right.multiply(Math.cos(angle) * beamVisual.beamRibbonOrbitRadius)
					.add(up.multiply(Math.sin(angle) * beamVisual.beamRibbonOrbitRadius));
				Vec3d point = start.add(direction.multiply(along)).add(offset);
				if (previous != null) {
					renderRibbonSegment(
						consumer,
						matrix,
						previous.subtract(cameraPos),
						point.subtract(cameraPos),
						offset.normalize(),
						beamVisual.beamRibbonThickness,
						0.44F,
						ribbonColor
					);
				}
				previous = point;
			}
		}
	}

	private static void renderBeamFlow(
		VertexConsumer consumer,
		Matrix4f matrix,
		Vec3d cameraPos,
		Vec3d start,
		Vec3d direction,
		double range,
		MagicConfig.BeamVisualConfig beamVisual,
		int density,
		float tickDelta,
		int primaryColor,
		int secondaryColor
	) {
		Vec3d right = horizontalRight(direction);
		Vec3d up = direction.crossProduct(right).normalize();
		double time = MinecraftClient.getInstance().world == null ? 0.0 : MinecraftClient.getInstance().world.getTime() + tickDelta;
		int count = Math.max(0, Math.min(beamVisual.beamFlowDensity, density * 2));
		for (int index = 0; index < count; index++) {
			double phase = (time * beamVisual.beamFlowSpeed * 0.04 + index / (double) Math.max(1, count)) % 1.0;
			double along = phase * range;
			double sideAngle = index * 0.85 + time * 0.03;
			Vec3d center = start.add(direction.multiply(along))
				.add(right.multiply(Math.cos(sideAngle) * 0.8))
				.add(up.multiply(Math.sin(sideAngle) * 0.8));
			renderGlowQuad(
				consumer,
				matrix,
				center.subtract(cameraPos),
				right,
				up,
				0.24,
				0.18F,
				index % 2 == 0 ? primaryColor : secondaryColor
			);
		}
	}

	private static void renderBeamInteriorParticles(
		VertexConsumer consumer,
		Matrix4f matrix,
		Vec3d cameraPos,
		Vec3d start,
		Vec3d direction,
		double range,
		MagicConfig.BeamVisualConfig beamVisual,
		int density,
		float tickDelta,
		int particleColor
	) {
		Vec3d right = horizontalRight(direction);
		Vec3d up = direction.crossProduct(right).normalize();
		double time = MinecraftClient.getInstance().world == null ? 0.0 : MinecraftClient.getInstance().world.getTime() + tickDelta;
		double radius = Math.max(0.2, config().gamaRay.beam.radius * beamVisual.beamInteriorParticleRadiusFactor);
		int count = Math.max(0, Math.min(beamVisual.beamInteriorParticleCount, density * 2));
		for (int index = 0; index < count; index++) {
			double progress = (time * beamVisual.beamInteriorParticleScrollSpeed * 0.018 + index / (double) Math.max(1, count)) % 1.0;
			double angle = time * 0.11 + index * 1.73;
			double radialFactor = 0.25 + 0.55 * (0.5 + 0.5 * Math.sin(index * 2.41 + time * 0.07));
			Vec3d offset = right.multiply(Math.cos(angle) * radius * radialFactor)
				.add(up.multiply(Math.sin(angle) * radius * radialFactor));
			Vec3d center = start.add(direction.multiply(progress * range)).add(offset);
			float alpha = beamVisual.beamInteriorParticleOpacity * (0.72F + 0.28F * (float) Math.sin(time * 0.23 + index * 0.9));
			renderGlowQuad(
				consumer,
				matrix,
				center.subtract(cameraPos),
				right,
				up,
				beamVisual.beamInteriorParticleSize,
				Math.max(0.04F, alpha),
				particleColor
			);
		}
	}

	private static void renderOriginBurst(
		VertexConsumer consumer,
		Matrix4f matrix,
		Vec3d cameraPos,
		VisualState state,
		Vec3d direction,
		MagicConfig.BeamVisualConfig beamVisual,
		float tickDelta,
		int coreColor,
		int outerColor
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			return;
		}
		float age = client.world.getTime() + tickDelta - state.phaseStartTick;
		if (age > 12.0F) {
			return;
		}
		Vec3d right = horizontalRight(direction);
		Vec3d up = direction.crossProduct(right).normalize();
		float burstAlpha = MathHelper.clamp(1.0F - age / 12.0F, 0.0F, 1.0F) * 0.52F;
		renderRing(
			consumer,
			matrix,
			cameraPos,
			state.beamOrigin.add(direction.multiply(0.3)),
			right,
			up,
			beamVisual.beamOriginBurstRadius * (1.0 + age * 0.08),
			28,
			burstAlpha,
			coreColor,
			age * 0.08
		);
		if (beamVisual.beamOriginBurstShockRingEnabled) {
			renderRing(
				consumer,
				matrix,
				cameraPos,
				state.beamOrigin.add(direction.multiply(0.1)),
				right,
				up,
				beamVisual.beamOriginBurstRadius * (0.45 + age * 0.14),
				24,
				burstAlpha * 0.7F,
				outerColor,
				0.0
			);
		}
	}

	private static void renderTargetPressureEffects(
		VertexConsumer consumer,
		Matrix4f matrix,
		Vec3d cameraPos,
		ClientWorld world,
		AbstractClientPlayerEntity caster,
		VisualState state,
		MagicConfig.BeamVisualConfig beamVisual,
		float tickDelta,
		int coreColor,
		int accentColor
	) {
		Vec3d direction = state.beamDirection.normalize();
		double radius = config().gamaRay.beam.radius;
		double range = config().gamaRay.beam.range;
		for (Entity entity : world.getOtherEntities(caster, caster.getBoundingBox().expand(range + radius + 4.0))) {
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				continue;
			}
			Vec3d point = new Vec3d(living.getX(), living.getBodyY(0.5), living.getZ());
			Vec3d relative = point.subtract(state.beamOrigin);
			double projection = relative.dotProduct(direction);
			if (projection < 0.0 || projection > range) {
				continue;
			}
			Vec3d nearest = state.beamOrigin.add(direction.multiply(projection));
			if (point.squaredDistanceTo(nearest) > radius * radius) {
				continue;
			}
			Vec3d center = new Vec3d(living.getX(), living.getY(), living.getZ()).add(0.0, living.getHeight() * 0.5, 0.0);
			Vec3d right = horizontalRight(direction);
			Vec3d up = new Vec3d(0.0, 1.0, 0.0);
			if (beamVisual.beamTargetBindVisualsEnabled) {
				renderRing(consumer, matrix, cameraPos, center, right, up, living.getWidth() * 0.9 + 0.35, 16, 0.16F, accentColor, world.getTime() * 0.08 + tickDelta);
				renderRing(consumer, matrix, cameraPos, center.add(0.0, 0.4, 0.0), right, up, living.getWidth() * 0.75 + 0.2, 16, 0.12F, coreColor, -world.getTime() * 0.06 - tickDelta);
			}
			if (beamVisual.beamImpactPressureEffectsEnabled) {
				renderGlowQuad(consumer, matrix, center.subtract(cameraPos), right, up, living.getWidth() * 0.45 + 0.2, 0.18F, coreColor);
			}
		}
	}

	private static int parseHexColor(String rawColor, int fallbackColor) {
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

	private static int blendColor(int firstColor, int secondColor, float amount) {
		float clampedAmount = MathHelper.clamp(amount, 0.0F, 1.0F);
		int red = Math.round(((firstColor >> 16) & 0xFF) * (1.0F - clampedAmount) + ((secondColor >> 16) & 0xFF) * clampedAmount);
		int green = Math.round(((firstColor >> 8) & 0xFF) * (1.0F - clampedAmount) + ((secondColor >> 8) & 0xFF) * clampedAmount);
		int blue = Math.round((firstColor & 0xFF) * (1.0F - clampedAmount) + (secondColor & 0xFF) * clampedAmount);
		return (red << 16) | (green << 8) | blue;
	}

	private static void renderRibbonSegment(VertexConsumer consumer, Matrix4f matrix, Vec3d start, Vec3d end, Vec3d normal, double halfWidth, float alpha, int color) {
		Vec3d axis = normal.lengthSquared() <= 1.0E-6 ? new Vec3d(0.0, 1.0, 0.0) : normal.normalize();
		Vec3d offset = axis.multiply(Math.max(0.01, halfWidth));
		vertex(consumer, matrix, start.add(offset), color, alpha, 0.0F, 0.0F);
		vertex(consumer, matrix, start.subtract(offset), color, alpha, 0.0F, 1.0F);
		vertex(consumer, matrix, end.subtract(offset), color, alpha, 1.0F, 1.0F);
		vertex(consumer, matrix, end.add(offset), color, alpha, 1.0F, 0.0F);
	}

	private static void renderGlowQuad(VertexConsumer consumer, Matrix4f matrix, Vec3d center, Vec3d right, Vec3d up, double size, float alpha, int color) {
		Vec3d x = right.normalize().multiply(size);
		Vec3d y = up.normalize().multiply(size);
		vertex(consumer, matrix, center.add(x).add(y), color, alpha, 1.0F, 0.0F);
		vertex(consumer, matrix, center.add(x).subtract(y), color, alpha, 1.0F, 1.0F);
		vertex(consumer, matrix, center.subtract(x).subtract(y), color, alpha, 0.0F, 1.0F);
		vertex(consumer, matrix, center.subtract(x).add(y), color, alpha, 0.0F, 0.0F);
	}

	private static void renderRing(
		VertexConsumer consumer,
		Matrix4f matrix,
		Vec3d cameraPos,
		Vec3d center,
		Vec3d right,
		Vec3d up,
		double radius,
		int segments,
		float alpha,
		int color,
		double spin
	) {
		Vec3d previous = null;
		for (int index = 0; index <= segments; index++) {
			double angle = spin + index * (Math.PI * 2.0 / Math.max(1, segments));
			Vec3d point = center
				.add(right.multiply(Math.cos(angle) * radius))
				.add(up.multiply(Math.sin(angle) * radius))
				.subtract(cameraPos);
			if (previous != null) {
				renderRibbonSegment(consumer, matrix, previous, point, point.subtract(previous).crossProduct(up), 0.05, alpha, color);
			}
			previous = point;
		}
	}

	private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3d position, int color, float alpha, float u, float v) {
		int red = (color >> 16) & 0xFF;
		int green = (color >> 8) & 0xFF;
		int blue = color & 0xFF;
		int alphaInt = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
		consumer
			.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
			.color(red, green, blue, alphaInt)
			.texture(u, v)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
			.normal(0.0F, 1.0F, 0.0F);
	}

	private static Vec3d beamFacingAxis(Vec3d start, Vec3d end, Vec3d cameraPos, Vec3d fallbackDirection, double roll) {
		Vec3d segment = end.subtract(start);
		Vec3d segmentDirection = segment.lengthSquared() <= 1.0E-6 ? fallbackDirection : segment.normalize();
		Vec3d midpoint = start.add(end).multiply(0.5);
		Vec3d axis = segmentDirection.crossProduct(midpoint.subtract(cameraPos));
		if (axis.lengthSquared() <= 1.0E-6) {
			axis = horizontalRight(segmentDirection);
		} else {
			axis = axis.normalize();
		}
		if (Math.abs(roll) > 1.0E-6) {
			axis = rotateAroundAxis(axis, segmentDirection, roll);
		}
		if (axis.lengthSquared() <= 1.0E-6) {
			return horizontalRight(segmentDirection);
		}
		return axis.normalize();
	}

	private static Vec3d rotateAroundAxis(Vec3d vector, Vec3d axis, double angle) {
		Vec3d normalizedAxis = axis.lengthSquared() <= 1.0E-6 ? new Vec3d(0.0, 1.0, 0.0) : axis.normalize();
		double cosine = Math.cos(angle);
		double sine = Math.sin(angle);
		return vector.multiply(cosine)
			.add(normalizedAxis.crossProduct(vector).multiply(sine))
			.add(normalizedAxis.multiply(normalizedAxis.dotProduct(vector) * (1.0 - cosine)));
	}

	private static double beamEnvelopeRadius(double beamRadius, MagicConfig.BeamVisualConfig beamVisual) {
		double radius = Math.max(beamRadius, 0.0);
		if (beamVisual.beamCoreEnabled) {
			radius = Math.max(radius, beamVisual.beamCoreThickness * 1.08 * 0.5);
		}
		if (beamVisual.beamOuterShellEnabled) {
			radius = Math.max(radius, beamVisual.beamOuterShellThickness * 1.34 * 0.5);
		}
		if (beamVisual.beamRibbonEnabled && beamVisual.beamRibbonCount > 0) {
			radius = Math.max(radius, beamVisual.beamRibbonOrbitRadius + beamVisual.beamRibbonThickness * 1.5);
		}
		if (beamVisual.beamInteriorParticlesEnabled) {
			radius = Math.max(radius, beamRadius * beamVisual.beamInteriorParticleRadiusFactor + beamVisual.beamInteriorParticleSize);
		}
		return radius;
	}


	private static Vec3d horizontalRight(Vec3d direction) {
		Vec3d axis = Math.abs(direction.y) > 0.9 ? new Vec3d(1.0, 0.0, 0.0) : new Vec3d(0.0, 1.0, 0.0);
		Vec3d right = direction.crossProduct(axis);
		if (right.lengthSquared() <= 1.0E-6) {
			right = new Vec3d(1.0, 0.0, 0.0);
		}
		return right.normalize();
	}

	private static int effectiveDensity(MagicConfig.ConstellationCelestialAlignmentConfig config, PlayerEntity caster) {
		int density = config.gamaRay.beamVisual.beamDecorativeDensity;
		if (config.visuals.lowFxFallbackMode || config.visuals.reducedBeamDensity) {
			density = Math.min(density, config.gamaRay.beamVisual.beamReducedFxDensity);
		}
		if (isLocalFirstPerson(caster) && config.visuals.reducedFirstPersonEffects) {
			density = Math.min(density, config.gamaRay.beamVisual.beamReducedFirstPersonDensity);
		}
		return Math.max(1, density);
	}

	private static boolean constructEnabled(MagicConfig.ConstellationCelestialAlignmentConfig config, PlayerEntity caster) {
		return chargeConstructRenderingAvailable
			&& !config.visuals.lowFxFallbackMode
			&& config.gamaRay.chargeConstruct.useGeckoLibChargeConstruct
			&& (!config.visuals.reducedFirstPersonEffects || !isLocalFirstPerson(caster))
			&& (
				config.visuals.useGeckoLibGamaRayChargeOrb
				|| config.gamaRay.charge.useGeckoLibChargeOrb
				|| config.gamaRay.chargeConstruct.useGeckoLibChargeConstruct
			);
	}

	private static MagicConfig.ConstellationCelestialAlignmentConfig config() {
		return MagicConfig.get().constellationCelestialAlignment;
	}

	private enum VisualPhase {
		INACTIVE,
		TRACING,
		CHARGING,
		FIRING;

		private static VisualPhase fromId(String raw) {
			if (raw == null || raw.isBlank()) {
				return INACTIVE;
			}
			return switch (raw.trim().toLowerCase(Locale.ROOT)) {
				case "tracing" -> TRACING;
				case "charging" -> CHARGING;
				case "firing" -> FIRING;
				default -> INACTIVE;
			};
		}
	}

	private static final class VisualState {
		private final UUID casterId;
		private final ChargeConstructAnimatable construct;
		private VisualPhase phase = VisualPhase.INACTIVE;
		private int phaseStartTick;
		private int phaseEndTick;
		private int localPhaseExpireTick;
		private int nextInteriorParticleTick;
		private Vec3d beamOrigin = Vec3d.ZERO;
		private Vec3d beamDirection = Vec3d.ZERO;
		private float lockedYaw;
		private float lockedPitch;
		private int lastSeenWorldTick;

		private VisualState(UUID casterId) {
			this.casterId = casterId;
			this.construct = new ChargeConstructAnimatable(casterId);
		}
	}

	private static final class CelestialGamaRayPoseAnimation extends PlayerAnimationFrame {
		private final PlayerEntity player;
		private float blend;

		private CelestialGamaRayPoseAnimation(PlayerEntity player) {
			this.player = player;
		}

		@Override
		public void tick(AnimationData state) {
			super.tick(state);
			resetPose();
			if (player == null) {
				isActive = false;
				return;
			}
			VisualState visualState = STATES.get(player.getUuid());
			MagicConfig.ConstellationCelestialAlignmentConfig config = config();
			boolean enabled = config.visuals.usePlayerAnimations && config.gamaRay.playerAnimation.enablePlayerAnimationPose;
			boolean posePhase = visualState != null && visualState.phase == VisualPhase.FIRING;
			float targetBlend = enabled && posePhase ? 1.0F : 0.0F;
			float step = targetBlend > blend ? 0.2F : Math.max(0.08F, 1.0F / Math.max(1, config.gamaRay.playerAnimation.recoveryDurationTicks));
			blend = MathHelper.lerp(step, blend, targetBlend);
			if (blend <= 0.01F || visualState == null) {
				isActive = false;
				return;
			}

			float firstPersonScale = state.isFirstPersonPass() && config.gamaRay.playerAnimation.reduceFirstPersonPoseIntensity ? 0.45F : 1.0F;
			float intensity = blend * firstPersonScale;
			double turnRadians = Math.toRadians(config.gamaRay.playerAnimation.sidewaysTurnDegrees) * intensity;
			double leanRadians = Math.toRadians(config.gamaRay.playerAnimation.torsoLeanDegrees) * intensity;
			double armExtension = config.gamaRay.playerAnimation.firingArmExtensionAmount * intensity;
			double worldTime = player.getEntityWorld().getTime() + state.getPartialTick();
			double recoil = visualState.phase == VisualPhase.FIRING && config.gamaRay.playerAnimation.recoilPulseEnabled
				? Math.sin(worldTime * config.gamaRay.playerAnimation.recoilPulseSpeed) * Math.toRadians(config.gamaRay.playerAnimation.recoilPulseAmplitude) * intensity * 0.15
				: 0.0;

			enableAll();
			body.rotY = (float) turnRadians;
			body.rotZ = (float) (-leanRadians * 0.55);
			head.rotY = (float) (-turnRadians * 0.32);
			head.rotX = (float) (-leanRadians * 0.1);
			rightArm.offsetPosZ = (float) (-armExtension);
			rightLeg.rotY = (float) (turnRadians * 0.3);
			leftLeg.rotY = (float) (-turnRadians * 0.2);

			if (visualState.phase == VisualPhase.FIRING && config.gamaRay.playerAnimation.firingPoseEnabled) {
				body.rotY = (float) (-turnRadians * 1.15);
				body.rotZ = (float) (-leanRadians);
				head.rotY = (float) (turnRadians * 1.12);
				head.rotX = (float) (-leanRadians * 0.06);
				leftArm.rotX = (float) (-1.6F * intensity + recoil);
				leftArm.rotY = 0.0F;
				leftArm.rotZ = 0.0F;
				leftArm.offsetPosX = (float) (0.16F * intensity);
				leftArm.offsetPosY = (float) (-0.42F * intensity);
				leftArm.offsetPosZ = (float) (-0.98F * intensity - armExtension * 0.42);
				rightArm.rotX = (float) (0.08F * intensity);
				rightArm.rotY = (float) (0.32F * intensity);
				rightArm.rotZ = (float) (0.22F * intensity);
				rightArm.offsetPosY = (float) (0.06F * intensity);
			}

			isActive = visualState.phase == VisualPhase.FIRING;
		}
	}

	private static final class ChargeConstructAnimatable implements GeoAnimatable {
		private final UUID ownerId;
		private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
		private boolean firing;

		private ChargeConstructAnimatable(UUID ownerId) {
			this.ownerId = ownerId;
		}

		@Override
		public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
			controllers.add(new AnimationController<>("celestial_gama_ray_charge_construct", animationState -> {
				animationState.setAnimation(this.firing ? FIRING_ANIMATION : CHARGE_ANIMATION);
				return PlayState.CONTINUE;
			}));
		}

		@Override
		public AnimatableInstanceCache getAnimatableInstanceCache() {
			return this.cache;
		}
	}

	private static final class ChargeConstructModel extends GeoModel<ChargeConstructAnimatable> {
		private static final Identifier MODEL = Identifier.of(Magic.MOD_ID, "geo/celestial_gama_ray_charge_construct.geo.json");
		private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/particle/glow.png");
		private static final Identifier ANIMATION = Identifier.of(Magic.MOD_ID, "animations/celestial_gama_ray_charge_construct.animation.json");

		@Override
		public Identifier getModelResource(GeoRenderState renderState) {
			return MODEL;
		}

		@Override
		public Identifier getTextureResource(GeoRenderState renderState) {
			return TEXTURE;
		}

		@Override
		public Identifier getAnimationResource(ChargeConstructAnimatable animatable) {
			return ANIMATION;
		}
	}

	private static final class ChargeConstructRenderer extends GeoObjectRenderer<ChargeConstructAnimatable, Void, GeoRenderState> {
		private ChargeConstructRenderer() {
			super(new ChargeConstructModel());
		}
	}
}

