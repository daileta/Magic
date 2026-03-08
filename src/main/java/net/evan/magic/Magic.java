package net.evan.magic;

import net.evan.magic.content.ModBootstrap;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Magic implements ModInitializer {
	public static final String MOD_ID = "magic";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBootstrap.initialize();
		LOGGER.info("Initialized mod {}", MOD_ID);
	}
}
