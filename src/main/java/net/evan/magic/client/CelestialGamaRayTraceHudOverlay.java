package net.evan.magic.client;

import java.util.List;
import net.evan.magic.Magic;
import net.evan.magic.network.payload.CelestialGamaRayTraceProgressPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class CelestialGamaRayTraceHudOverlay {
	private static final Identifier HUD_LAYER = Identifier.of(Magic.MOD_ID, "celestial_gama_ray_trace");
	private static final float INPUT_SCALE = 7.5F;
	private static boolean active;
	private static String constellationId = "";
	private static float overlayScale = 1.0F;
	private static int overlayXOffset;
	private static int overlayYOffset;
	private static int pathColor = 0xFF86B6FF;
	private static int progressColor = 0xFFFFF1B0;
	private static int activeSegmentColor = 0xFFFFFFFF;
	private static int startNodeColor = 0xFF9CFFDE;
	private static int endNodeColor = 0xFFFFA0B4;
	private static int successColor = 0xFF9CFFDE;
	private static int failColor = 0xFFFF8080;
	private static float toleranceRadius = 18.0F;
	private static float segmentCompletionRadius = 20.0F;
	private static Text promptText = Text.empty();
	private static Text chargingText = Text.empty();
	private static float cursorX;
	private static float cursorY;
	private static float lastYaw = Float.NaN;
	private static float lastPitch = Float.NaN;
	private static int segmentIndex;
	private static int flashTicks;
	private static int flashColor;
	private static boolean completionSent;

	private CelestialGamaRayTraceHudOverlay() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, HUD_LAYER, CelestialGamaRayTraceHudOverlay::render);
		ClientTickEvents.END_CLIENT_TICK.register(CelestialGamaRayTraceHudOverlay::tick);
	}

	public static void show(
		boolean active,
		String constellationId,
		float scale,
		int xOffset,
		int yOffset,
		int pathColorRgb,
		int progressColorRgb,
		int activeSegmentColorRgb,
		int startNodeColorRgb,
		int endNodeColorRgb,
		int successColorRgb,
		int failColorRgb,
		float toleranceRadius,
		float segmentCompletionRadius,
		String promptText,
		String chargingText
	) {
		CelestialGamaRayTraceHudOverlay.active = active;
		CelestialGamaRayTraceHudOverlay.constellationId = constellationId == null ? "" : constellationId;
		CelestialGamaRayTraceHudOverlay.overlayScale = Math.max(0.25F, scale);
		CelestialGamaRayTraceHudOverlay.overlayXOffset = xOffset;
		CelestialGamaRayTraceHudOverlay.overlayYOffset = yOffset;
		CelestialGamaRayTraceHudOverlay.pathColor = 0xFF000000 | (pathColorRgb & 0x00FFFFFF);
		CelestialGamaRayTraceHudOverlay.progressColor = 0xFF000000 | (progressColorRgb & 0x00FFFFFF);
		CelestialGamaRayTraceHudOverlay.activeSegmentColor = 0xFF000000 | (activeSegmentColorRgb & 0x00FFFFFF);
		CelestialGamaRayTraceHudOverlay.startNodeColor = 0xFF000000 | (startNodeColorRgb & 0x00FFFFFF);
		CelestialGamaRayTraceHudOverlay.endNodeColor = 0xFF000000 | (endNodeColorRgb & 0x00FFFFFF);
		CelestialGamaRayTraceHudOverlay.successColor = 0xFF000000 | (successColorRgb & 0x00FFFFFF);
		CelestialGamaRayTraceHudOverlay.failColor = 0xFF000000 | (failColorRgb & 0x00FFFFFF);
		CelestialGamaRayTraceHudOverlay.toleranceRadius = Math.max(1.0F, toleranceRadius);
		CelestialGamaRayTraceHudOverlay.segmentCompletionRadius = Math.max(1.0F, segmentCompletionRadius);
		CelestialGamaRayTraceHudOverlay.promptText = Text.literal(promptText == null ? "" : promptText);
		CelestialGamaRayTraceHudOverlay.chargingText = Text.literal(chargingText == null ? "" : chargingText);
		List<TracePoint> path = tracePath(constellationId);
		TracePoint first = path.isEmpty() ? new TracePoint(0.0F, 0.0F) : path.getFirst();
		cursorX = first.x;
		cursorY = first.y;
		lastYaw = Float.NaN;
		lastPitch = Float.NaN;
		segmentIndex = 0;
		flashTicks = 0;
		flashColor = 0;
		completionSent = false;
	}

	public static void clear() {
		active = false;
		constellationId = "";
		promptText = Text.empty();
		chargingText = Text.empty();
		flashTicks = 0;
		completionSent = false;
	}

	private static void tick(MinecraftClient client) {
		if (!active || client.player == null) {
			lastYaw = Float.NaN;
			lastPitch = Float.NaN;
			return;
		}
		List<TracePoint> path = tracePath(constellationId);
		if (path.size() < 2) {
			return;
		}
		if (Float.isNaN(lastYaw)) {
			lastYaw = client.player.getYaw();
			lastPitch = client.player.getPitch();
			return;
		}

		float yawDelta = MathHelper.wrapDegrees(client.player.getYaw() - lastYaw);
		float pitchDelta = client.player.getPitch() - lastPitch;
		lastYaw = client.player.getYaw();
		lastPitch = client.player.getPitch();
		cursorX += yawDelta * INPUT_SCALE;
		cursorY += pitchDelta * INPUT_SCALE;

		TracePoint start = path.get(segmentIndex);
		TracePoint end = path.get(segmentIndex + 1);
		SegmentProjection projection = project(cursorX, cursorY, start, end);
		if (projection.distance > toleranceRadius) {
			resetTrace();
			return;
		}

		if (distance(cursorX, cursorY, end.x, end.y) <= segmentCompletionRadius) {
			segmentIndex++;
			flashTicks = 6;
			flashColor = successColor;
			if (segmentIndex >= path.size() - 1 && !completionSent) {
				completionSent = true;
				ClientPlayNetworking.send(new CelestialGamaRayTraceProgressPayload("success"));
			}
		}

		if (flashTicks > 0) {
			flashTicks--;
		}
	}

	private static void resetTrace() {
		List<TracePoint> path = tracePath(constellationId);
		TracePoint first = path.isEmpty() ? new TracePoint(0.0F, 0.0F) : path.getFirst();
		cursorX = first.x;
		cursorY = first.y;
		segmentIndex = 0;
		completionSent = false;
		flashTicks = 10;
		flashColor = failColor;
		ClientPlayNetworking.send(new CelestialGamaRayTraceProgressPayload("reset"));
	}

	private static void render(DrawContext context, RenderTickCounter tickCounter) {
		if (!active) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			clear();
			return;
		}
		List<TracePoint> path = tracePath(constellationId);
		if (path.size() < 2) {
			return;
		}

		int centerX = context.getScaledWindowWidth() / 2 + overlayXOffset;
		int centerY = context.getScaledWindowHeight() / 2 + overlayYOffset;
		float scale = overlayScale;
		for (int index = 0; index < path.size() - 1; index++) {
			TracePoint start = path.get(index);
			TracePoint end = path.get(index + 1);
			int color = index < segmentIndex ? progressColor : (index == segmentIndex ? activeSegmentColor : pathColor);
			drawLine(
				context,
				centerX + Math.round(start.x * scale),
				centerY + Math.round(start.y * scale),
				centerX + Math.round(end.x * scale),
				centerY + Math.round(end.y * scale),
				color
			);
		}

		for (int index = 0; index < path.size(); index++) {
			TracePoint point = path.get(index);
			int color = index == 0 ? startNodeColor : (index == path.size() - 1 ? endNodeColor : pathColor);
			drawNode(context, centerX + Math.round(point.x * scale), centerY + Math.round(point.y * scale), 2, color);
		}

		if (flashTicks > 0) {
			drawNode(context, centerX + Math.round(cursorX * scale), centerY + Math.round(cursorY * scale), 3, flashColor);
		} else {
			drawNode(context, centerX + Math.round(cursorX * scale), centerY + Math.round(cursorY * scale), 2, progressColor);
		}

		Text line = completionSent ? chargingText : promptText;
		if (!line.getString().isEmpty()) {
			int width = client.textRenderer.getWidth(line);
			context.drawTextWithShadow(client.textRenderer, line, centerX - width / 2, centerY + Math.round(82 * scale), 0xFFFFFFFF);
		}
	}

	private static void drawLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx - dy;
		int x = x0;
		int y = y0;
		while (true) {
			context.fill(x, y, x + 2, y + 2, color);
			if (x == x1 && y == y1) {
				break;
			}
			int e2 = err * 2;
			if (e2 > -dy) {
				err -= dy;
				x += sx;
			}
			if (e2 < dx) {
				err += dx;
				y += sy;
			}
		}
	}

	private static void drawNode(DrawContext context, int x, int y, int radius, int color) {
		context.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, color);
	}

	private static SegmentProjection project(float px, float py, TracePoint start, TracePoint end) {
		float dx = end.x - start.x;
		float dy = end.y - start.y;
		float lengthSq = dx * dx + dy * dy;
		if (lengthSq <= 1.0E-6F) {
			return new SegmentProjection(distance(px, py, start.x, start.y));
		}
		float t = MathHelper.clamp(((px - start.x) * dx + (py - start.y) * dy) / lengthSq, 0.0F, 1.0F);
		float closestX = start.x + dx * t;
		float closestY = start.y + dy * t;
		return new SegmentProjection(distance(px, py, closestX, closestY));
	}

	private static float distance(float ax, float ay, float bx, float by) {
		float dx = ax - bx;
		float dy = ay - by;
		return MathHelper.sqrt(dx * dx + dy * dy);
	}

	private static List<TracePoint> tracePath(String constellationId) {
		return switch (constellationId) {
			case "crater" -> List.of(
				new TracePoint(-58.0F, -18.0F),
				new TracePoint(-28.0F, -44.0F),
				new TracePoint(0.0F, -52.0F),
				new TracePoint(28.0F, -44.0F),
				new TracePoint(58.0F, -18.0F),
				new TracePoint(28.0F, -44.0F),
				new TracePoint(24.0F, 16.0F),
				new TracePoint(0.0F, 40.0F),
				new TracePoint(0.0F, 62.0F),
				new TracePoint(0.0F, 40.0F),
				new TracePoint(-24.0F, 16.0F),
				new TracePoint(-28.0F, -44.0F)
			);
			case "sagitta" -> List.of(
				new TracePoint(0.0F, -58.0F),
				new TracePoint(-18.0F, -32.0F),
				new TracePoint(0.0F, -8.0F),
				new TracePoint(18.0F, -32.0F),
				new TracePoint(0.0F, -58.0F),
				new TracePoint(0.0F, 44.0F),
				new TracePoint(-20.0F, 26.0F),
				new TracePoint(0.0F, 44.0F),
				new TracePoint(20.0F, 26.0F)
			);
			case "gemini" -> List.of(
				new TracePoint(-24.0F, -56.0F),
				new TracePoint(-22.0F, -14.0F),
				new TracePoint(-20.0F, 24.0F),
				new TracePoint(-16.0F, 58.0F),
				new TracePoint(-22.0F, -14.0F),
				new TracePoint(0.0F, -34.0F),
				new TracePoint(22.0F, -14.0F),
				new TracePoint(24.0F, -56.0F),
				new TracePoint(22.0F, -14.0F),
				new TracePoint(20.0F, 24.0F),
				new TracePoint(16.0F, 58.0F),
				new TracePoint(20.0F, 24.0F),
				new TracePoint(0.0F, 6.0F)
			);
			case "aquila" -> List.of(
				new TracePoint(0.0F, -42.0F),
				new TracePoint(-56.0F, -10.0F),
				new TracePoint(-28.0F, 8.0F),
				new TracePoint(0.0F, 44.0F),
				new TracePoint(28.0F, 8.0F),
				new TracePoint(56.0F, -10.0F),
				new TracePoint(0.0F, -42.0F)
			);
			case "scorpius" -> List.of(
				new TracePoint(-54.0F, -20.0F),
				new TracePoint(-28.0F, -34.0F),
				new TracePoint(0.0F, -12.0F),
				new TracePoint(24.0F, 10.0F),
				new TracePoint(12.0F, 36.0F),
				new TracePoint(34.0F, 58.0F)
			);
			case "libra" -> List.of(
				new TracePoint(0.0F, -58.0F),
				new TracePoint(0.0F, -14.0F),
				new TracePoint(-50.0F, 10.0F),
				new TracePoint(-28.0F, 38.0F),
				new TracePoint(0.0F, -14.0F),
				new TracePoint(50.0F, 10.0F),
				new TracePoint(28.0F, 38.0F)
			);
			default -> List.of(new TracePoint(0.0F, 0.0F), new TracePoint(0.0F, 0.0F));
		};
	}

	private record TracePoint(float x, float y) {
	}

	private record SegmentProjection(float distance) {
	}
}
