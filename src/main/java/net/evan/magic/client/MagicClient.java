package net.evan.magic.client;

import net.fabricmc.api.ClientModInitializer;

public final class MagicClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MagicKeybindings.initialize();
		MagicClientNetworking.initialize();
		ManaHudOverlay.register();
	}
}
