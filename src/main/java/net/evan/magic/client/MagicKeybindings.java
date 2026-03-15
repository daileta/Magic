package net.evan.magic.client;

import net.evan.magic.Magic;
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.network.payload.CooldownCheckPayload;
import net.evan.magic.network.payload.DomainClashInputPayload;
import net.evan.magic.network.payload.UseAbilityPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class MagicKeybindings {
	private static final KeyBinding.Category MAGIC_ABILITY_CATEGORY = KeyBinding.Category.create(Identifier.of(Magic.MOD_ID, "magic_abilities"));
	private static final KeyBinding ABILITY_1 = register("ability_1", GLFW.GLFW_KEY_Z);
	private static final KeyBinding ABILITY_2 = register("ability_2", GLFW.GLFW_KEY_X);
	private static final KeyBinding ABILITY_3 = register("ability_3", GLFW.GLFW_KEY_C);
	private static final KeyBinding ABILITY_4 = register("ability_4", GLFW.GLFW_KEY_V);
	private static final KeyBinding ABILITY_5 = register("ability_5", GLFW.GLFW_KEY_B);
	private static final KeyBinding COOLDOWN_CHECK = register("cooldown_check", GLFW.GLFW_KEY_N);
	private static final int[] CLASH_KEYS = { GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D };
	private static final boolean[] CLASH_KEY_PREVIOUS_DOWN = new boolean[CLASH_KEYS.length];

	private MagicKeybindings() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(MagicKeybindings::onEndClientTick);
	}

	private static void onEndClientTick(MinecraftClient client) {
		if (client.player == null || client.getNetworkHandler() == null || client.currentScreen != null) {
			return;
		}

		if (MagicPlayerData.isDomainClashActive(client.player) && MagicPlayerData.getDomainClashPromptKey(client.player) != 0) {
			sendDomainClashInput(client);
		}

		sendPressedAbilities();
	}

	private static void sendPressedAbilities() {
		sendWhilePressed(ABILITY_1, 1);
		sendWhilePressed(ABILITY_2, 2);
		sendWhilePressed(ABILITY_3, 3);
		sendWhilePressed(ABILITY_4, 4);
		sendWhilePressed(ABILITY_5, 5);
		sendCooldownCheck();
	}

	private static void sendWhilePressed(KeyBinding keyBinding, int abilitySlot) {
		while (keyBinding.wasPressed()) {
			ClientPlayNetworking.send(new UseAbilityPayload(abilitySlot));
		}
	}

	private static void sendCooldownCheck() {
		while (COOLDOWN_CHECK.wasPressed()) {
			ClientPlayNetworking.send(new CooldownCheckPayload(0));
		}
	}

	private static void sendDomainClashInput(MinecraftClient client) {
		for (int i = 0; i < CLASH_KEYS.length; i++) {
			int keyCode = CLASH_KEYS[i];
			boolean down = InputUtil.isKeyPressed(client.getWindow(), keyCode);
			if (down && !CLASH_KEY_PREVIOUS_DOWN[i]) {
				ClientPlayNetworking.send(new DomainClashInputPayload(keyCode));
			}
			CLASH_KEY_PREVIOUS_DOWN[i] = down;
		}
	}

	private static KeyBinding register(String abilityName, int defaultKey) {
		return KeyBindingHelper.registerKeyBinding(
			new KeyBinding(
				"key.magic." + abilityName,
				InputUtil.Type.KEYSYM,
				defaultKey,
				MAGIC_ABILITY_CATEGORY
			)
		);
	}
}
