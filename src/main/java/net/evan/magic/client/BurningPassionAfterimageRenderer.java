package net.evan.magic.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.evan.magic.config.MagicConfig;
import net.evan.magic.magic.MagicPlayerData;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class BurningPassionAfterimageRenderer {
	private static final Map<Integer, TrailState> TRAILS = new HashMap<>();
	private static final ThreadLocal<AfterimageRenderContext> ACTIVE_RENDER_CONTEXT = new ThreadLocal<>();
	private static boolean initialized;

	private BurningPassionAfterimageRenderer() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		ClientTickEvents.END_CLIENT_TICK.register(BurningPassionAfterimageRenderer::onEndClientTick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
		initialized = true;
	}

	public static void capture(PlayerEntity player, PlayerEntityRenderState renderState) {
		if (player == null || renderState == null || !(player.getEntityWorld() instanceof ClientWorld world)) {
			return;
		}

		MagicConfig.EngineHeartAfterimageConfig config = config();
		if (!config.enabled) {
			TRAILS.remove(player.getId());
			return;
		}

		TrailState trail = trailState(player, world);
		long worldTime = world.getTime();
		double speed = horizontalSpeed(player.getVelocity());
		int activeTier = MagicPlayerData.getBurningPassionEngineHeartAfterimageTier(player);
		boolean active = MagicPlayerData.isBurningPassionEngineHeartAfterimageActive(player)
			&& player.isAlive()
			&& !player.isRemoved()
			&& activeTier >= 1
			&& speed >= config.minimumSpeedThreshold;
		boolean localFirstPerson = isLocalFirstPerson(player);
		trail.lastSeenTick = worldTime;
		trail.lastMeasuredSpeed = speed;
		trail.activeTier = activeTier;

		pruneSnapshots(trail, worldTime, config.snapshotLifetimeTicks, activeTier);
		if (!active) {
			return;
		}

		if (localFirstPerson && config.firstPersonReducedMode) {
			trail.snapshots.clear();
			if (!config.thirdPersonOnly) {
				spawnAccentParticles(world, player, trail, config, speed, true, worldTime);
			}
			return;
		}

		int captureInterval = effectiveSnapshotInterval(config, speed);
		if (worldTime - trail.lastSnapshotTick >= captureInterval) {
			trail.lastSnapshotTick = worldTime;
			trail.snapshots.addLast(new AfterimageSnapshot(copyRenderState(renderState), worldTime, speed, motionDirection(player, renderState), activeTier));
			while (trail.snapshots.size() > maxSnapshotHistory(config)) {
				trail.snapshots.removeFirst();
			}
		}

		spawnAccentParticles(world, player, trail, config, speed, false, worldTime);
	}

	public static void renderAfterimages(
		LivingEntityRenderer<?, PlayerEntityRenderState, ?> renderer,
		PlayerEntityRenderState currentState,
		MatrixStack matrices,
		OrderedRenderCommandQueue queue,
		CameraRenderState camera
	) {
		if (currentState == null || ACTIVE_RENDER_CONTEXT.get() != null) {
			return;
		}

		MagicConfig.EngineHeartAfterimageConfig config = config();
		if (!config.enabled || currentState.squaredDistanceToCamera > config.renderDistanceLimit * config.renderDistanceLimit) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			return;
		}

		TrailState trail = TRAILS.get(currentState.id);
		if (trail == null) {
			return;
		}

		long worldTime = client.world.getTime();
		pruneSnapshots(trail, worldTime, config.snapshotLifetimeTicks, trail.activeTier);
		if (trail.snapshots.isEmpty()) {
			return;
		}

		boolean localFirstPerson = client.player != null
			&& client.player.getId() == currentState.id
			&& client.options.getPerspective().isFirstPerson();
		if (localFirstPerson && (config.firstPersonReducedMode || config.thirdPersonOnly)) {
			return;
		}

		List<AfterimageSnapshot> snapshots = new ArrayList<>(trail.snapshots);
		int renderCount = Math.min(snapshots.size(), effectiveSnapshotCount(config, trail.lastMeasuredSpeed, trail.activeTier));
		int startIndex = Math.max(0, snapshots.size() - renderCount);
		Vec3d currentAnchor = anchoredPosition(currentState);
		float renderTime = worldTime + client.getRenderTickCounter().getTickProgress(false);
		for (int index = startIndex; index < snapshots.size(); index++) {
			AfterimageSnapshot snapshot = snapshots.get(index);
			float age = renderTime - snapshot.createdTick();
			float lifetime = Math.max(1.0F, config.snapshotLifetimeTicks);
			float progress = MathHelper.clamp(age / lifetime, 0.0F, 1.0F);
			float intensity = speedIntensity(config, snapshot.speed());
			float tierAlphaMultiplier = tierOpacityMultiplier(config, snapshot.tier());
			float alpha = (float) MathHelper.clamp(
				MathHelper.lerp(progress, config.opacityStart, config.opacityEnd) * MathHelper.lerp(intensity, 0.85F, 1.1F) * tierAlphaMultiplier,
				0.0,
				1.0
			);
			if (alpha <= 0.01F) {
				continue;
			}

			Vec3d snapshotAnchor = anchoredPosition(snapshot.renderState());
			float scale = (float) MathHelper.clamp(config.scaleMultiplier + intensity * 0.02F, 0.5, 2.0);
			int baseColor = withAlpha(sampleTint(config.tintColorHexes, progress), alpha);
			renderPass(renderer, currentState, snapshot.renderState(), matrices, queue, camera, currentAnchor, snapshotAnchor, scale, baseColor);

			if (config.distortionLayerEnabled && config.distortionIntensity > 0.0) {
				Vec3d left = leftDirection(snapshot.motionDirection());
				double phase = snapshot.createdTick() * 0.3 + index * 0.55;
				double sway = Math.sin(renderTime * 0.6 + phase) * config.distortionIntensity * 0.08;
				double depth = Math.cos(renderTime * 0.45 + phase) * config.distortionIntensity * 0.05;
				float offsetAlpha = alpha * 0.45F;
				int offsetColor = withAlpha(sampleTint(config.tintColorHexes, MathHelper.clamp(progress + 0.18F, 0.0F, 1.0F)), offsetAlpha);
				renderPass(
					renderer,
					currentState,
					snapshot.renderState(),
					matrices,
					queue,
					camera,
					currentAnchor,
					snapshotAnchor.add(left.multiply(sway)).add(snapshot.motionDirection().multiply(-depth)),
					scale * 1.02F,
					offsetColor
				);
				renderPass(
					renderer,
					currentState,
					snapshot.renderState(),
					matrices,
					queue,
					camera,
					currentAnchor,
					snapshotAnchor.add(left.multiply(-sway * 0.75)).add(0.0, config.distortionIntensity * 0.025, 0.0),
					scale * 0.995F,
					withAlpha(sampleTint(config.tintColorHexes, MathHelper.clamp(progress + 0.32F, 0.0F, 1.0F)), offsetAlpha * 0.85F)
				);
			}
		}
	}

	public static boolean shouldSuppressFeatures(PlayerEntityRenderState renderState) {
		AfterimageRenderContext context = ACTIVE_RENDER_CONTEXT.get();
		return context != null && context.renderState() == renderState;
	}

	public static boolean shouldSuppressLabels(PlayerEntityRenderState renderState) {
		return shouldSuppressFeatures(renderState);
	}

	public static Integer mixColorOverride(PlayerEntityRenderState renderState) {
		AfterimageRenderContext context = ACTIVE_RENDER_CONTEXT.get();
		return context != null && context.renderState() == renderState ? context.mixColor() : null;
	}

	public static boolean isAfterimageRenderState(PlayerEntityRenderState renderState) {
		AfterimageRenderContext context = ACTIVE_RENDER_CONTEXT.get();
		return context != null && context.renderState() == renderState;
	}

	private static void onEndClientTick(MinecraftClient client) {
		if (client.world == null) {
			clear();
			return;
		}

		MagicConfig.EngineHeartAfterimageConfig config = config();
		if (config.enabled && client.player != null && isLocalFirstPerson(client.player) && config.firstPersonReducedMode && !config.thirdPersonOnly) {
			TrailState trail = trailState(client.player, client.world);
			long worldTime = client.world.getTime();
			double speed = horizontalSpeed(client.player.getVelocity());
			int activeTier = MagicPlayerData.getBurningPassionEngineHeartAfterimageTier(client.player);
			trail.lastSeenTick = worldTime;
			trail.lastMeasuredSpeed = speed;
			trail.activeTier = activeTier;
			pruneSnapshots(trail, worldTime, config.snapshotLifetimeTicks, activeTier);
			if (
				MagicPlayerData.isBurningPassionEngineHeartAfterimageActive(client.player)
				&& client.player.isAlive()
				&& !client.player.isRemoved()
				&& activeTier >= 1
				&& speed >= config.minimumSpeedThreshold
			) {
				spawnAccentParticles(client.world, client.player, trail, config, speed, true, worldTime);
			}
		}

		long worldTime = client.world.getTime();
		Iterator<Map.Entry<Integer, TrailState>> iterator = TRAILS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, TrailState> entry = iterator.next();
			TrailState trail = entry.getValue();
			pruneSnapshots(trail, worldTime, config.snapshotLifetimeTicks, trail.activeTier);
			if (trail.dimension != client.world.getRegistryKey() || worldTime - trail.lastSeenTick > Math.max(5, config.snapshotLifetimeTicks + 5L)) {
				iterator.remove();
				continue;
			}
			if (!(client.world.getEntityById(entry.getKey()) instanceof PlayerEntity player) || !player.isAlive()) {
				if (trail.snapshots.isEmpty()) {
					iterator.remove();
				}
			}
		}
	}

	private static void renderPass(
		LivingEntityRenderer<?, PlayerEntityRenderState, ?> renderer,
		PlayerEntityRenderState currentState,
		PlayerEntityRenderState snapshotState,
		MatrixStack matrices,
		OrderedRenderCommandQueue queue,
		CameraRenderState camera,
		Vec3d currentAnchor,
		Vec3d snapshotAnchor,
		float scale,
		int mixColor
	) {
		matrices.push();
		matrices.translate(snapshotAnchor.x - currentAnchor.x, snapshotAnchor.y - currentAnchor.y, snapshotAnchor.z - currentAnchor.z);
		if (Math.abs(scale - 1.0F) > 1.0E-4F) {
			matrices.scale(scale, scale, scale);
		}
		ACTIVE_RENDER_CONTEXT.set(new AfterimageRenderContext(snapshotState, mixColor));
		try {
			renderer.render(snapshotState, matrices, queue, camera);
		} finally {
			ACTIVE_RENDER_CONTEXT.remove();
			matrices.pop();
		}
	}

	private static void spawnAccentParticles(
		ClientWorld world,
		PlayerEntity player,
		TrailState trail,
		MagicConfig.EngineHeartAfterimageConfig config,
		double speed,
		boolean reduced,
		long worldTime
	) {
		if (!config.bakedAccentEnabled && config.emberSupportParticleCount <= 0) {
			return;
		}

		int interval = Math.max(1, config.bakedAccentEnabled ? config.accentIntervalTicks : Integer.MAX_VALUE);
		if (worldTime - trail.lastAccentTick < interval) {
			return;
		}

		trail.lastAccentTick = worldTime;
		float intensity = speedIntensity(config, speed);
		Vec3d direction = motionDirection(player, null);
		Vec3d reverse = direction.multiply(-1.0);
		Vec3d left = leftDirection(direction);
		Vec3d center = new Vec3d(player.getX(), player.getBodyY(0.42), player.getZ())
			.add(reverse.multiply(0.55 + intensity * 0.35))
			.add(0.0, reduced ? -0.05 : 0.02, 0.0);

		if (config.bakedAccentEnabled) {
			int accentPoints = reduced ? 4 : 6;
			double radius = 0.28 + intensity * 0.34;
			for (int index = 0; index < accentPoints; index++) {
				double progress = accentPoints <= 1 ? 0.0 : (double) index / (accentPoints - 1) - 0.5;
				Vec3d point = center.add(left.multiply(progress * radius * 2.0));
				world.addParticleClient(
					ParticleTypes.WHITE_ASH,
					point.x,
					point.y + Math.sin(worldTime * 0.2 + index) * 0.03,
					point.z,
					reverse.x * 0.015,
					0.0,
					reverse.z * 0.015
				);
				if (!reduced && (index & 1) == 0) {
					world.addParticleClient(
						ParticleTypes.CLOUD,
						point.x,
						point.y,
						point.z,
						reverse.x * 0.01,
						0.0,
						reverse.z * 0.01
					);
				}
			}

			if (!reduced) {
				int ringPoints = 6;
				double ringRadius = 0.22 + intensity * 0.28;
				for (int index = 0; index < ringPoints; index++) {
					double angle = (Math.PI * 2.0 * index / ringPoints) + worldTime * 0.18;
					Vec3d point = center.add(left.multiply(Math.cos(angle) * ringRadius)).add(reverse.multiply(Math.sin(angle) * ringRadius * 0.35));
					world.addParticleClient(ParticleTypes.CLOUD, point.x, point.y, point.z, reverse.x * 0.012, 0.0, reverse.z * 0.012);
				}
			}
		}

		int emberCount = Math.max(0, reduced ? Math.max(1, config.emberSupportParticleCount / 2) : config.emberSupportParticleCount);
		for (int index = 0; index < emberCount; index++) {
			double lateral = emberCount <= 1 ? 0.0 : ((double) index / (emberCount - 1) - 0.5) * 0.35;
			Vec3d point = center.add(left.multiply(lateral)).add(0.0, index * 0.015, 0.0);
			world.addParticleClient(
				ParticleTypes.FLAME,
				point.x,
				point.y,
				point.z,
				reverse.x * config.emberSpeed,
				0.01 + intensity * 0.01,
				reverse.z * config.emberSpeed
			);
		}
	}

	private static TrailState trailState(PlayerEntity player, ClientWorld world) {
		TrailState existing = TRAILS.get(player.getId());
		if (existing != null && existing.uuid.equals(player.getUuid()) && existing.dimension == world.getRegistryKey()) {
			return existing;
		}

		TrailState created = new TrailState(player.getUuid(), world.getRegistryKey());
		TRAILS.put(player.getId(), created);
		return created;
	}

	private static PlayerEntityRenderState copyRenderState(PlayerEntityRenderState source) {
		PlayerEntityRenderState copy = new PlayerEntityRenderState();
		copy.entityType = source.entityType;
		copy.x = source.x;
		copy.y = source.y;
		copy.z = source.z;
		copy.age = source.age;
		copy.width = source.width;
		copy.height = source.height;
		copy.standingEyeHeight = source.standingEyeHeight;
		copy.squaredDistanceToCamera = source.squaredDistanceToCamera;
		copy.invisible = false;
		copy.sneaking = source.sneaking;
		copy.onFire = false;
		copy.light = source.light;
		copy.outlineColor = source.outlineColor;
		copy.positionOffset = source.positionOffset == null ? Vec3d.ZERO : source.positionOffset;
		copy.displayName = Text.empty();
		copy.nameLabelPos = source.nameLabelPos == null ? Vec3d.ZERO : source.nameLabelPos;
		copy.shadowRadius = 0.0F;
		copy.shadowPieces.clear();
		copy.leashDatas = List.of();
		copy.bodyYaw = source.bodyYaw;
		copy.relativeHeadYaw = source.relativeHeadYaw;
		copy.pitch = source.pitch;
		copy.deathTime = source.deathTime;
		copy.limbSwingAnimationProgress = source.limbSwingAnimationProgress;
		copy.limbSwingAmplitude = source.limbSwingAmplitude;
		copy.baseScale = source.baseScale;
		copy.ageScale = source.ageScale;
		copy.timeSinceLastKineticAttack = source.timeSinceLastKineticAttack;
		copy.flipUpsideDown = source.flipUpsideDown;
		copy.shaking = source.shaking;
		copy.baby = source.baby;
		copy.touchingWater = source.touchingWater;
		copy.usingRiptide = source.usingRiptide;
		copy.hurt = source.hurt;
		copy.invisibleToPlayer = false;
		copy.sleepingDirection = source.sleepingDirection;
		copy.pose = source.pose;
		copy.headItemAnimationProgress = source.headItemAnimationProgress;
		copy.wearingSkullType = source.wearingSkullType;
		copy.wearingSkullProfile = source.wearingSkullProfile;
		clearItemRenderState(copy.headItemRenderState);
		copy.mainArm = source.mainArm;
		copy.rightArmPose = source.rightArmPose;
		copy.rightHandItem = source.rightHandItem;
		copy.leftArmPose = source.leftArmPose;
		copy.leftHandItem = source.leftHandItem;
		copy.swingAnimationType = source.swingAnimationType;
		copy.handSwingProgress = source.handSwingProgress;
		clearItemRenderState(copy.rightHandItemState);
		clearItemRenderState(copy.leftHandItemState);
		copy.leaningPitch = source.leaningPitch;
		copy.limbAmplitudeInverse = source.limbAmplitudeInverse;
		copy.crossbowPullTime = source.crossbowPullTime;
		copy.itemUseTime = source.itemUseTime;
		copy.preferredArm = source.preferredArm;
		copy.activeHand = source.activeHand == null ? Hand.MAIN_HAND : source.activeHand;
		copy.isInSneakingPose = source.isInSneakingPose;
		copy.isGliding = source.isGliding;
		copy.isSwimming = source.isSwimming;
		copy.hasVehicle = source.hasVehicle;
		copy.isUsingItem = source.isUsingItem;
		copy.leftWingPitch = source.leftWingPitch;
		copy.leftWingYaw = source.leftWingYaw;
		copy.leftWingRoll = source.leftWingRoll;
		copy.equippedHeadStack = source.equippedHeadStack;
		copy.equippedChestStack = source.equippedChestStack;
		copy.equippedLegsStack = source.equippedLegsStack;
		copy.equippedFeetStack = source.equippedFeetStack;
		copy.skinTextures = source.skinTextures;
		copy.field_53536 = source.field_53536;
		copy.field_53537 = source.field_53537;
		copy.field_53538 = source.field_53538;
		copy.stuckArrowCount = 0;
		copy.stingerCount = 0;
		copy.spectator = false;
		copy.hatVisible = source.hatVisible;
		copy.jacketVisible = source.jacketVisible;
		copy.leftPantsLegVisible = source.leftPantsLegVisible;
		copy.rightPantsLegVisible = source.rightPantsLegVisible;
		copy.leftSleeveVisible = source.leftSleeveVisible;
		copy.rightSleeveVisible = source.rightSleeveVisible;
		copy.capeVisible = false;
		copy.glidingTicks = source.glidingTicks;
		copy.applyFlyingRotation = source.applyFlyingRotation;
		copy.flyingRotation = source.flyingRotation;
		copy.playerName = Text.empty();
		copy.leftShoulderParrotVariant = null;
		copy.rightShoulderParrotVariant = null;
		copy.id = source.id;
		copy.extraEars = source.extraEars;
		clearItemRenderState(copy.spyglassState);
		return copy;
	}

	private static void clearItemRenderState(ItemRenderState renderState) {
		renderState.clear();
	}

	private static void pruneSnapshots(TrailState trail, long worldTime, int lifetimeTicks, int activeTier) {
		while (!trail.snapshots.isEmpty()) {
			AfterimageSnapshot snapshot = trail.snapshots.peekFirst();
			if (snapshot == null || worldTime - snapshot.createdTick() <= lifetimeTicks) {
				break;
			}
			trail.snapshots.removeFirst();
		}
		if (activeTier <= 0 && trail.snapshots.isEmpty()) {
			trail.activeTier = 0;
		}
	}

	private static Vec3d anchoredPosition(PlayerEntityRenderState state) {
		Vec3d offset = state.positionOffset == null ? Vec3d.ZERO : state.positionOffset;
		return new Vec3d(state.x + offset.x, state.y + offset.y, state.z + offset.z);
	}

	private static Vec3d motionDirection(PlayerEntity player, PlayerEntityRenderState renderState) {
		Vec3d velocity = player.getVelocity();
		Vec3d horizontal = new Vec3d(velocity.x, 0.0, velocity.z);
		if (horizontal.lengthSquared() > 1.0E-5) {
			return horizontal.normalize();
		}

		float yaw = renderState == null ? player.getYaw() : renderState.bodyYaw;
		float yawRadians = yaw * MathHelper.RADIANS_PER_DEGREE;
		Vec3d facing = new Vec3d(-MathHelper.sin(yawRadians), 0.0, MathHelper.cos(yawRadians));
		return facing.lengthSquared() > 1.0E-5 ? facing.normalize() : new Vec3d(0.0, 0.0, 1.0);
	}

	private static Vec3d leftDirection(Vec3d direction) {
		Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z);
		if (horizontal.lengthSquared() <= 1.0E-5) {
			return new Vec3d(1.0, 0.0, 0.0);
		}

		Vec3d normalized = horizontal.normalize();
		return new Vec3d(-normalized.z, 0.0, normalized.x);
	}

	private static double horizontalSpeed(Vec3d velocity) {
		return velocity == null ? 0.0 : Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
	}

	private static int effectiveSnapshotInterval(MagicConfig.EngineHeartAfterimageConfig config, double speed) {
		float intensity = speedIntensity(config, speed);
		int base = Math.max(1, config.snapshotIntervalTicks);
		return Math.max(1, Math.round(MathHelper.lerp(intensity, base, 1.0F)));
	}

	private static int effectiveSnapshotCount(MagicConfig.EngineHeartAfterimageConfig config, double speed, int tier) {
		int configuredCap = tierSnapshotCap(config, tier);
		if (configuredCap <= 2) {
			return configuredCap;
		}

		float intensity = speedIntensity(config, speed);
		return MathHelper.clamp(2 + Math.round(intensity * (configuredCap - 2)), 2, configuredCap);
	}

	private static float speedIntensity(MagicConfig.EngineHeartAfterimageConfig config, double speed) {
		double range = Math.max(0.05, config.minimumSpeedThreshold);
		return (float) MathHelper.clamp((speed - config.minimumSpeedThreshold) / range, 0.0, 1.0);
	}

	private static int maxSnapshotHistory(MagicConfig.EngineHeartAfterimageConfig config) {
		return Math.max(config.tierOneMaxSnapshots, Math.max(config.tierTwoMaxSnapshots, config.tierThreeMaxSnapshots));
	}

	private static int tierSnapshotCap(MagicConfig.EngineHeartAfterimageConfig config, int tier) {
		return switch (MathHelper.clamp(tier, 1, 3)) {
			case 1 -> config.tierOneMaxSnapshots;
			case 2 -> config.tierTwoMaxSnapshots;
			default -> config.tierThreeMaxSnapshots;
		};
	}

	private static float tierOpacityMultiplier(MagicConfig.EngineHeartAfterimageConfig config, int tier) {
		return (float) switch (MathHelper.clamp(tier, 1, 3)) {
			case 1 -> config.tierOneOpacityMultiplier;
			case 2 -> config.tierTwoOpacityMultiplier;
			default -> config.tierThreeOpacityMultiplier;
		};
	}

	private static boolean isLocalFirstPerson(PlayerEntity player) {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.player != null
			&& client.player.getId() == player.getId()
			&& client.options.getPerspective() == Perspective.FIRST_PERSON;
	}

	private static int sampleTint(List<String> rawColors, float progress) {
		if (rawColors == null || rawColors.isEmpty()) {
			return 0xFFF2D1;
		}

		if (rawColors.size() == 1) {
			return parseHexColor(rawColors.get(0), 0xFFF2D1);
		}

		float clamped = MathHelper.clamp(progress, 0.0F, 1.0F);
		float scaled = clamped * (rawColors.size() - 1);
		int lowerIndex = MathHelper.clamp(MathHelper.floor(scaled), 0, rawColors.size() - 1);
		int upperIndex = MathHelper.clamp(lowerIndex + 1, 0, rawColors.size() - 1);
		float localProgress = scaled - lowerIndex;
		int startColor = parseHexColor(rawColors.get(lowerIndex), 0xFFF2D1);
		int endColor = parseHexColor(rawColors.get(upperIndex), startColor);
		return interpolateColor(startColor, endColor, localProgress);
	}

	private static int interpolateColor(int startColor, int endColor, float progress) {
		float clamped = MathHelper.clamp(progress, 0.0F, 1.0F);
		int startRed = (startColor >> 16) & 0xFF;
		int startGreen = (startColor >> 8) & 0xFF;
		int startBlue = startColor & 0xFF;
		int endRed = (endColor >> 16) & 0xFF;
		int endGreen = (endColor >> 8) & 0xFF;
		int endBlue = endColor & 0xFF;
		int red = Math.round(MathHelper.lerp(clamped, startRed, endRed));
		int green = Math.round(MathHelper.lerp(clamped, startGreen, endGreen));
		int blue = Math.round(MathHelper.lerp(clamped, startBlue, endBlue));
		return (red << 16) | (green << 8) | blue;
	}

	private static int withAlpha(int colorRgb, float alpha) {
		int alphaChannel = MathHelper.clamp(Math.round(alpha * 255.0F), 0, 255);
		return (alphaChannel << 24) | (colorRgb & 0x00FFFFFF);
	}

	private static int parseHexColor(String rawColor, int fallbackColor) {
		if (rawColor == null || rawColor.isBlank()) {
			return fallbackColor;
		}

		String normalized = rawColor.trim();
		if (normalized.startsWith("#")) {
			normalized = normalized.substring(1);
		}
		if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
			normalized = normalized.substring(2);
		}
		if (normalized.length() != 6) {
			return fallbackColor;
		}

		try {
			return Integer.parseInt(normalized, 16) & 0x00FFFFFF;
		} catch (NumberFormatException exception) {
			return fallbackColor;
		}
	}

	private static MagicConfig.EngineHeartAfterimageConfig config() {
		return MagicConfig.get().burningPassion.engineHeart.afterimageTrail;
	}

	private static void clear() {
		TRAILS.clear();
		ACTIVE_RENDER_CONTEXT.remove();
	}

	private record AfterimageRenderContext(PlayerEntityRenderState renderState, int mixColor) {
	}

	private record AfterimageSnapshot(PlayerEntityRenderState renderState, long createdTick, double speed, Vec3d motionDirection, int tier) {
	}

	private static final class TrailState {
		private final UUID uuid;
		private final RegistryKey<World> dimension;
		private final Deque<AfterimageSnapshot> snapshots = new ArrayDeque<>();
		private long lastSnapshotTick = Long.MIN_VALUE / 4;
		private long lastAccentTick = Long.MIN_VALUE / 4;
		private long lastSeenTick = Long.MIN_VALUE / 4;
		private double lastMeasuredSpeed;
		private int activeTier;

		private TrailState(UUID uuid, RegistryKey<World> dimension) {
			this.uuid = uuid;
			this.dimension = dimension;
		}
	}
}
