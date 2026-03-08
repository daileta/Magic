package net.evan.magic.content;

import net.evan.magic.command.MagicCommands;
import net.evan.magic.config.MagicConfig;
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.ability.MagicAbilityManager;
import net.evan.magic.network.MagicNetworking;
import net.evan.magic.registry.ModBlocks;
import net.evan.magic.registry.ModItems;

public final class ModBootstrap {
	private static boolean initialized;

	private ModBootstrap() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		MagicConfig.initialize();
		MagicPlayerData.initialize();
		MagicNetworking.initialize();
		MagicAbilityManager.initialize();
		MagicCommands.initialize();
		ModItems.register();
		ModBlocks.register();
		initialized = true;
	}
}
