package net.evan.magic.client;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.evan.magic.network.payload.ConstellationWarningOverlayPayload;
import net.evan.magic.network.payload.GreedDomainWarningOverlayPayload;
import net.evan.magic.network.payload.JesterJokeOverlayPayload;
import net.evan.magic.network.payload.ConstellationOutlinePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.Entity;

public final class MagicClientNetworking {
	private static boolean initialized;
	private static final Set<UUID> CONSTELLATION_OUTLINED_ENTITIES = new HashSet<>();

	private MagicClientNetworking() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		ClientPlayNetworking.registerGlobalReceiver(ConstellationOutlinePayload.ID, (payload, context) ->
			context.client().execute(() -> {
				CONSTELLATION_OUTLINED_ENTITIES.clear();
				CONSTELLATION_OUTLINED_ENTITIES.addAll(payload.entityUuids());
			})
		);
		ClientPlayNetworking.registerGlobalReceiver(ConstellationWarningOverlayPayload.ID, (payload, context) ->
			context.client().execute(() ->
				ManaHudOverlay.showConstellationWarning(
					payload.message(),
					payload.colorRgb(),
					payload.scale(),
					payload.fadeInTicks(),
					payload.stayTicks(),
					payload.fadeOutTicks()
				)
			)
		);
		ClientPlayNetworking.registerGlobalReceiver(GreedDomainWarningOverlayPayload.ID, (payload, context) ->
			context.client().execute(() ->
				ManaHudOverlay.showGreedDomainWarning(
					payload.message(),
					payload.colorRgb(),
					payload.outlineColorRgb(),
					payload.scale(),
					payload.durationTicks(),
					payload.lineSpacing()
				)
			)
		);
		ClientPlayNetworking.registerGlobalReceiver(JesterJokeOverlayPayload.ID, (payload, context) ->
			context.client().execute(() ->
				ManaHudOverlay.showJesterJoke(
					payload.message(),
					payload.colorRgb(),
					payload.fadeInTicks(),
					payload.stayTicks(),
					payload.fadeOutTicks()
				)
			)
		);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CONSTELLATION_OUTLINED_ENTITIES.clear());
		initialized = true;
	}

	public static boolean shouldRenderConstellationOutline(Entity entity) {
		return entity != null && CONSTELLATION_OUTLINED_ENTITIES.contains(entity.getUuid());
	}
}
