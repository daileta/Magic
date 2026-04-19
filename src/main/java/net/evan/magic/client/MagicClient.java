package net.evan.magic.client;
import net.evan.magic.client.hud.CelestialGamaRayTraceHudOverlay;
import net.evan.magic.client.hud.ManaHudOverlay;
import net.evan.magic.client.input.MagicKeybindings;
import net.evan.magic.client.networking.MagicClientNetworking;
import net.evan.magic.client.render.BurningPassionAfterimageRenderer;
import net.evan.magic.client.render.CelestialGamaRayPresentationManager;
import net.evan.magic.client.render.ClientLibraryValidation;

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

