package net.evan.magic.client;

import net.evan.magic.client.particle.MagicClientParticles;
import net.fabricmc.api.ClientModInitializer;

public final class MagicClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientLibraryValidation.initialize();
		BurningPassionAfterimageRenderer.initialize();
		CelestialGamaRayPresentationManager.initialize();
		MagicClientParticles.initialize();
		MagicKeybindings.initialize();
		MagicClientNetworking.initialize();
		CelestialGamaRayTraceHudOverlay.register();
		ManaHudOverlay.register();
	}
}
