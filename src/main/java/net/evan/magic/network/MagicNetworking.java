package net.evan.magic.network;

import net.evan.magic.magic.ability.MagicAbilityManager;
import net.evan.magic.network.payload.DomainClashInputPayload;
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
		ServerPlayNetworking.registerGlobalReceiver(UseAbilityPayload.ID, (payload, context) ->
			MagicAbilityManager.onAbilityRequested(context.player(), payload.abilitySlot())
		);
		ServerPlayNetworking.registerGlobalReceiver(DomainClashInputPayload.ID, (payload, context) ->
			MagicAbilityManager.onDomainClashInput(context.player(), payload.keyCode())
		);
		initialized = true;
	}
}
