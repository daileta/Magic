package net.evan.magic.network;

import net.evan.magic.magic.ability.MagicAbilityManager;
import net.evan.magic.network.payload.CooldownCheckPayload;
import net.evan.magic.network.payload.ConstellationOutlinePayload;
import net.evan.magic.network.payload.ConstellationWarningOverlayPayload;
import net.evan.magic.network.payload.DomainClashInputPayload;
import net.evan.magic.network.payload.GreedDomainWarningOverlayPayload;
import net.evan.magic.network.payload.JesterJokeOverlayPayload;
import net.evan.magic.network.payload.UseAbilityPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class MagicNetworking {
	private static boolean initialized;

	private MagicNetworking() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		PayloadTypeRegistry.playC2S().register(UseAbilityPayload.ID, UseAbilityPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DomainClashInputPayload.ID, DomainClashInputPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CooldownCheckPayload.ID, CooldownCheckPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ConstellationOutlinePayload.ID, ConstellationOutlinePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ConstellationWarningOverlayPayload.ID, ConstellationWarningOverlayPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(GreedDomainWarningOverlayPayload.ID, GreedDomainWarningOverlayPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(JesterJokeOverlayPayload.ID, JesterJokeOverlayPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(UseAbilityPayload.ID, (payload, context) ->
			MagicAbilityManager.onAbilityRequested(context.player(), payload.abilitySlot())
		);
		ServerPlayNetworking.registerGlobalReceiver(DomainClashInputPayload.ID, (payload, context) ->
			MagicAbilityManager.onDomainClashInput(context.player(), payload.keyCode())
		);
		ServerPlayNetworking.registerGlobalReceiver(CooldownCheckPayload.ID, (payload, context) ->
			MagicAbilityManager.onCooldownCheckRequested(context.player())
		);
		initialized = true;
	}
}
