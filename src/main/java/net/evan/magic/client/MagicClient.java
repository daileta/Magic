package net.evan.magic.client;

import net.evan.magic.client.particle.MagicClientParticles;
import net.fabricmc.api.ClientModInitializer;

public final class MagicClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MagicClientParticles.initialize();
		MagicKeybindings.initialize();
		MagicClientNetworking.initialize();
		ManaHudOverlay.register();
	}
}
