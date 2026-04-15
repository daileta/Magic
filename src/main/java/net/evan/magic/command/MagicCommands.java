package net.evan.magic.command;

public final class MagicCommands {
	private MagicCommands() {
	}

	public static void initialize() {
		MagicCommandRegistrar.initialize();
	}
}
