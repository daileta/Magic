package net.evan.magic.client;

import net.evan.magic.Magic;
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.MagicSchool;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public final class ManaHudOverlay {
	private static final Identifier WITHER_BAR_BACKGROUND = Identifier.ofVanilla("boss_bar/purple_background");
	private static final Identifier WITHER_BAR_PROGRESS = Identifier.ofVanilla("boss_bar/purple_progress");
	private static final Identifier MANA_HUD_LAYER = Identifier.of(Magic.MOD_ID, "mana_bar");
	private static final int BAR_WIDTH = 182;
	private static final int BAR_HEIGHT = 5;
	private static final int CLASH_BAR_BACKGROUND = 0xAA222222;
	private static final int CLASH_BAR_PROGRESS = 0xFF29CC72;
	private static final int CLASH_BAR_WRONG_FLASH = 0xFFFF2B2B;
	private static final int CLASH_BAR_WRONG_FLASH_TICKS = 5;
	private static final int PROMPT_TRANSITION_TICKS = 4;
	private static final float PROMPT_SCALE = 2.6F;
	private static final int PROMPT_COLOR = 0xFFFF2B2B;
	private static final float INSTRUCTION_SCALE = 1.15F;
	private static final int INSTRUCTION_LINE_SPACING = 12;
	private static final int INSTRUCTION_COLOR = 0xFF2EA8FF;
	private static final int FROST_COLOR = 0x66CCFF;
	private static final int LOVE_COLOR = 0xFF77CC;
	private static final int BURNING_PASSION_COLOR = 0xFF8A3D;
	private static final int JESTER_COLOR = 0xFFD54A;
	private static final int CONSTELLATION_COLOR = 0x39B7FF;
	private static final int GREED_COLOR = 0xD8A624;
	private static final float JESTER_JOKE_SCALE = 1.2F;
	private static final int JESTER_JOKE_Y = 28;
	private static int lastObservedPlayerAge = Integer.MIN_VALUE;
	private static int lastClashProgress = 0;
	private static int wrongFlashTicksRemaining = 0;
	private static int lastPromptKey = 0;
	private static int outgoingPromptKey = 0;
	private static int incomingPromptKey = 0;
	private static int promptTransitionTicksRemaining = 0;
	private static Text activeJesterJoke = Text.empty();
	private static int activeJesterJokeColor = 0xFFFFFFFF;
	private static int jesterJokeFadeInTicks = 0;
	private static int jesterJokeStayTicks = 0;
	private static int jesterJokeFadeOutTicks = 0;
	private static int jesterJokeStartAge = Integer.MIN_VALUE;
	private static Text activeConstellationWarning = Text.empty();
	private static int activeConstellationWarningColor = 0xFFFFFFFF;
	private static float constellationWarningScale = 1.35F;
	private static int constellationWarningFadeInTicks = 0;
	private static int constellationWarningStayTicks = 0;
	private static int constellationWarningFadeOutTicks = 0;
	private static int constellationWarningStartAge = Integer.MIN_VALUE;

	private ManaHudOverlay() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR, MANA_HUD_LAYER, ManaHudOverlay::render);
	}

	private static void render(DrawContext drawContext, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null || client.world == null) {
			clearJesterJoke();
			clearConstellationWarning();
			return;
		}

		renderConstellationWarningOverlay(drawContext, client);
		renderJesterJokeOverlay(drawContext, client);
		if (!MagicPlayerData.hasMagic(client.player)) {
			return;
		}

		MagicSchool school = MagicPlayerData.getSchool(client.player);
		int filledWidth = MathHelper.clamp(
			Math.round((float) MagicPlayerData.getMana(client.player) / MagicPlayerData.MAX_MANA * BAR_WIDTH),
			0,
			BAR_WIDTH
		);

		int x = drawContext.getScaledWindowWidth() / 2 - 91;
		int y = drawContext.getScaledWindowHeight() - 63;
		renderDomainClashHud(drawContext, client, x, y);
		drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, WITHER_BAR_BACKGROUND, x, y, BAR_WIDTH, BAR_HEIGHT);

		if (filledWidth > 0) {
			drawContext.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				WITHER_BAR_PROGRESS,
				BAR_WIDTH,
				BAR_HEIGHT,
				0,
				0,
				x,
				y,
				filledWidth,
				BAR_HEIGHT
			);
		}

		Text schoolName = school.displayName();
		int textWidth = client.textRenderer.getWidth(schoolName);
		int textX = drawContext.getScaledWindowWidth() - textWidth - 6;
		int textY = drawContext.getScaledWindowHeight() - 18;
		if (school == MagicSchool.GREED) {
			Text greedCoins = Text.literal("Coins: " + MagicPlayerData.formatGreedCoins(client.player));
			int coinsWidth = client.textRenderer.getWidth(greedCoins);
			drawContext.drawTextWithShadow(
				client.textRenderer,
				greedCoins,
				drawContext.getScaledWindowWidth() - coinsWidth - 6,
				drawContext.getScaledWindowHeight() - 30,
				GREED_COLOR
			);
		}
		drawContext.drawTextWithShadow(client.textRenderer, schoolName, textX, textY, colorForSchool(school));
	}

	public static void showJesterJoke(String message, int colorRgb, int fadeInTicks, int stayTicks, int fadeOutTicks) {
		MinecraftClient client = MinecraftClient.getInstance();
		activeJesterJoke = Text.literal(message == null ? "" : message);
		activeJesterJokeColor = 0xFF000000 | (colorRgb & 0x00FFFFFF);
		jesterJokeFadeInTicks = Math.max(0, fadeInTicks);
		jesterJokeStayTicks = Math.max(0, stayTicks);
		jesterJokeFadeOutTicks = Math.max(0, fadeOutTicks);
		jesterJokeStartAge = client.player == null ? Integer.MIN_VALUE : client.player.age;
	}

	public static void showConstellationWarning(
		String message,
		int colorRgb,
		float scale,
		int fadeInTicks,
		int stayTicks,
		int fadeOutTicks
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		activeConstellationWarning = Text.literal(message == null ? "" : message).formatted(Formatting.BOLD);
		activeConstellationWarningColor = 0xFF000000 | (colorRgb & 0x00FFFFFF);
		constellationWarningScale = Math.max(JESTER_JOKE_SCALE + 0.1F, scale);
		constellationWarningFadeInTicks = Math.max(0, fadeInTicks);
		constellationWarningStayTicks = Math.max(0, stayTicks);
		constellationWarningFadeOutTicks = Math.max(0, fadeOutTicks);
		constellationWarningStartAge = client.player == null ? Integer.MIN_VALUE : client.player.age;
	}

	private static void renderDomainClashHud(DrawContext drawContext, MinecraftClient client, int x, int manaBarY) {
		if (!MagicPlayerData.isDomainClashActive(client.player)) {
			resetDomainClashVisualState();
			return;
		}

		advanceVisualTimers(client.player.age);
		int progress = MagicPlayerData.getDomainClashProgress(client.player);
		if (progress < lastClashProgress) {
			wrongFlashTicksRemaining = CLASH_BAR_WRONG_FLASH_TICKS;
		}
		lastClashProgress = progress;

		int filledWidth = MathHelper.clamp(Math.round(progress / 100.0F * BAR_WIDTH), 0, BAR_WIDTH);
		int clashY = manaBarY - 8;
		drawContext.fill(x, clashY, x + BAR_WIDTH, clashY + BAR_HEIGHT, CLASH_BAR_BACKGROUND);
		if (filledWidth > 0) {
			int progressColor = wrongFlashTicksRemaining > 0 ? CLASH_BAR_WRONG_FLASH : CLASH_BAR_PROGRESS;
			drawContext.fill(x, clashY, x + filledWidth, clashY + BAR_HEIGHT, progressColor);
		}

		int promptKey = MagicPlayerData.getDomainClashPromptKey(client.player);
		int instructionVisibility = MagicPlayerData.getDomainClashInstructionVisibility(client.player);
		if (promptKey == 0 && instructionVisibility <= 0) {
			return;
		}

		int centerX = drawContext.getScaledWindowWidth() / 2;
		int centerY = drawContext.getScaledWindowHeight() / 2 - 24;

		if (promptKey != lastPromptKey) {
			if (lastPromptKey != 0 && promptKey != 0) {
				outgoingPromptKey = lastPromptKey;
				incomingPromptKey = promptKey;
				promptTransitionTicksRemaining = PROMPT_TRANSITION_TICKS;
			} else {
				outgoingPromptKey = 0;
				incomingPromptKey = promptKey;
				promptTransitionTicksRemaining = PROMPT_TRANSITION_TICKS;
			}
			lastPromptKey = promptKey;
		}

		if (promptKey != 0) {
			renderPromptLetter(drawContext, client, centerX, centerY, promptKey);
			return;
		}

		renderInstructions(drawContext, client, centerX, centerY, instructionVisibility / 100.0F);
	}

	private static void renderInstructions(DrawContext drawContext, MinecraftClient client, int centerX, int centerY, float alpha) {
		Text[] lines = {
			Text.translatable("overlay.magic.domain_clash.instructions.line1"),
			Text.translatable("overlay.magic.domain_clash.instructions.line2"),
			Text.translatable("overlay.magic.domain_clash.instructions.line3"),
			Text.translatable("overlay.magic.domain_clash.instructions.line4"),
			Text.translatable("overlay.magic.domain_clash.instructions.line5")
		};

		int totalHeight = (lines.length - 1) * INSTRUCTION_LINE_SPACING;
		int startY = centerY - totalHeight / 2;
		for (int i = 0; i < lines.length; i++) {
			drawScaledInstruction(drawContext, client, lines[i], centerX, startY + i * INSTRUCTION_LINE_SPACING, alpha);
		}
	}

	private static void renderPromptLetter(DrawContext drawContext, MinecraftClient client, int centerX, int centerY, int promptKey) {
		if (promptTransitionTicksRemaining > 0 && outgoingPromptKey != 0 && incomingPromptKey != 0) {
			int halfTicks = Math.max(1, PROMPT_TRANSITION_TICKS / 2);
			int elapsed = PROMPT_TRANSITION_TICKS - promptTransitionTicksRemaining;
			if (elapsed < halfTicks) {
				float alpha = 1.0F - (elapsed / (float) halfTicks);
				drawScaledPrompt(drawContext, client, outgoingPromptKey, centerX, centerY, alpha);
				return;
			}

			float alpha = (elapsed - halfTicks + 1) / (float) Math.max(1, PROMPT_TRANSITION_TICKS - halfTicks);
			drawScaledPrompt(drawContext, client, incomingPromptKey, centerX, centerY, alpha);
			return;
		}

		int stablePrompt = promptKey != 0 ? promptKey : incomingPromptKey;
		if (stablePrompt == 0) {
			return;
		}

		drawScaledPrompt(drawContext, client, stablePrompt, centerX, centerY, 1.0F);
	}

	private static void drawScaledPrompt(
		DrawContext drawContext,
		MinecraftClient client,
		int promptKey,
		int centerX,
		int centerY,
		float alpha
	) {
		String promptLabel = keyCodeToPrompt(promptKey);
		if (promptLabel.isEmpty()) {
			return;
		}

		Text prompt = Text.literal(promptLabel).formatted(Formatting.BOLD);
		int promptWidth = client.textRenderer.getWidth(prompt);
		int color = withAlpha(PROMPT_COLOR, alpha);
		var matrices = drawContext.getMatrices();
		matrices.pushMatrix();
		matrices.scale(PROMPT_SCALE, PROMPT_SCALE);
		int drawX = Math.round((centerX - (promptWidth * PROMPT_SCALE) / 2.0F) / PROMPT_SCALE);
		int drawY = Math.round(centerY / PROMPT_SCALE);
		drawContext.drawTextWithShadow(client.textRenderer, prompt, drawX, drawY, color);
		matrices.popMatrix();
	}

	private static void drawScaledInstruction(
		DrawContext drawContext,
		MinecraftClient client,
		Text line,
		int centerX,
		int centerY,
		float alpha
	) {
		int lineWidth = client.textRenderer.getWidth(line);
		int color = withAlpha(INSTRUCTION_COLOR, alpha);
		var matrices = drawContext.getMatrices();
		matrices.pushMatrix();
		matrices.scale(INSTRUCTION_SCALE, INSTRUCTION_SCALE);
		int drawX = Math.round((centerX - (lineWidth * INSTRUCTION_SCALE) / 2.0F) / INSTRUCTION_SCALE);
		int drawY = Math.round(centerY / INSTRUCTION_SCALE);
		drawContext.drawTextWithShadow(client.textRenderer, line, drawX, drawY, color);
		matrices.popMatrix();
	}

	private static void renderJesterJokeOverlay(DrawContext drawContext, MinecraftClient client) {
		if (activeJesterJoke.getString().isEmpty()) {
			return;
		}

		if (jesterJokeStartAge == Integer.MIN_VALUE) {
			jesterJokeStartAge = client.player.age;
		}

		int totalTicks = jesterJokeFadeInTicks + jesterJokeStayTicks + jesterJokeFadeOutTicks;
		int elapsed = Math.max(0, client.player.age - jesterJokeStartAge);
		if (totalTicks <= 0 || elapsed >= totalTicks) {
			clearJesterJoke();
			return;
		}

		float alpha = 1.0F;
		if (jesterJokeFadeInTicks > 0 && elapsed < jesterJokeFadeInTicks) {
			alpha = elapsed / (float) jesterJokeFadeInTicks;
		} else if (
			jesterJokeFadeOutTicks > 0
			&& elapsed >= jesterJokeFadeInTicks + jesterJokeStayTicks
		) {
			int fadeElapsed = elapsed - jesterJokeFadeInTicks - jesterJokeStayTicks;
			alpha = 1.0F - (fadeElapsed / (float) jesterJokeFadeOutTicks);
		}

		int jokeWidth = client.textRenderer.getWidth(activeJesterJoke);
		var matrices = drawContext.getMatrices();
		matrices.pushMatrix();
		matrices.scale(JESTER_JOKE_SCALE, JESTER_JOKE_SCALE);
		int drawX = Math.round((drawContext.getScaledWindowWidth() - (jokeWidth * JESTER_JOKE_SCALE)) / (2.0F * JESTER_JOKE_SCALE));
		int drawY = Math.round(JESTER_JOKE_Y / JESTER_JOKE_SCALE);
		drawContext.drawTextWithShadow(
			client.textRenderer,
			activeJesterJoke,
			drawX,
			drawY,
			withAlpha(activeJesterJokeColor, alpha)
		);
		matrices.popMatrix();
	}

	private static void renderConstellationWarningOverlay(DrawContext drawContext, MinecraftClient client) {
		if (activeConstellationWarning.getString().isEmpty()) {
			return;
		}

		if (constellationWarningStartAge == Integer.MIN_VALUE) {
			constellationWarningStartAge = client.player.age;
		}

		int totalTicks = constellationWarningFadeInTicks + constellationWarningStayTicks + constellationWarningFadeOutTicks;
		int elapsed = Math.max(0, client.player.age - constellationWarningStartAge);
		if (totalTicks <= 0 || elapsed >= totalTicks) {
			clearConstellationWarning();
			return;
		}

		float alpha = 1.0F;
		if (constellationWarningFadeInTicks > 0 && elapsed < constellationWarningFadeInTicks) {
			alpha = elapsed / (float) constellationWarningFadeInTicks;
		} else if (
			constellationWarningFadeOutTicks > 0
			&& elapsed >= constellationWarningFadeInTicks + constellationWarningStayTicks
		) {
			int fadeElapsed = elapsed - constellationWarningFadeInTicks - constellationWarningStayTicks;
			alpha = 1.0F - (fadeElapsed / (float) constellationWarningFadeOutTicks);
		}

		int warningWidth = client.textRenderer.getWidth(activeConstellationWarning);
		float targetScale = Math.max(0.5F, constellationWarningScale);
		float maxWidth = Math.max(1.0F, drawContext.getScaledWindowWidth() - 24.0F);
		float fittedScale = warningWidth <= 0 ? targetScale : Math.min(targetScale, maxWidth / warningWidth);
		fittedScale = Math.max(0.5F, fittedScale);
		var matrices = drawContext.getMatrices();
		matrices.pushMatrix();
		matrices.scale(fittedScale, fittedScale);
		int drawX = Math.round((drawContext.getScaledWindowWidth() - (warningWidth * fittedScale)) / (2.0F * fittedScale));
		int drawY = Math.round(
			(drawContext.getScaledWindowHeight() - (client.textRenderer.fontHeight * fittedScale)) / (2.0F * fittedScale)
		);
		drawContext.drawTextWithShadow(
			client.textRenderer,
			activeConstellationWarning,
			drawX,
			drawY,
			withAlpha(activeConstellationWarningColor, alpha)
		);
		matrices.popMatrix();
	}

	private static void advanceVisualTimers(int playerAge) {
		if (lastObservedPlayerAge == Integer.MIN_VALUE) {
			lastObservedPlayerAge = playerAge;
			return;
		}

		int delta = Math.max(0, playerAge - lastObservedPlayerAge);
		lastObservedPlayerAge = playerAge;
		if (delta <= 0) {
			return;
		}

		if (wrongFlashTicksRemaining > 0) {
			wrongFlashTicksRemaining = Math.max(0, wrongFlashTicksRemaining - delta);
		}
		if (promptTransitionTicksRemaining > 0) {
			promptTransitionTicksRemaining = Math.max(0, promptTransitionTicksRemaining - delta);
			if (promptTransitionTicksRemaining == 0) {
				outgoingPromptKey = 0;
			}
		}
	}

	private static int withAlpha(int rgbColor, float alpha) {
		int alphaChannel = MathHelper.clamp(Math.round(alpha * 255.0F), 0, 255);
		return (alphaChannel << 24) | (rgbColor & 0x00FFFFFF);
	}

	private static void resetDomainClashVisualState() {
		lastObservedPlayerAge = Integer.MIN_VALUE;
		lastClashProgress = 0;
		wrongFlashTicksRemaining = 0;
		lastPromptKey = 0;
		outgoingPromptKey = 0;
		incomingPromptKey = 0;
		promptTransitionTicksRemaining = 0;
	}

	private static void clearJesterJoke() {
		activeJesterJoke = Text.empty();
		activeJesterJokeColor = 0xFFFFFFFF;
		jesterJokeFadeInTicks = 0;
		jesterJokeStayTicks = 0;
		jesterJokeFadeOutTicks = 0;
		jesterJokeStartAge = Integer.MIN_VALUE;
	}

	private static void clearConstellationWarning() {
		activeConstellationWarning = Text.empty();
		activeConstellationWarningColor = 0xFFFFFFFF;
		constellationWarningScale = 1.35F;
		constellationWarningFadeInTicks = 0;
		constellationWarningStayTicks = 0;
		constellationWarningFadeOutTicks = 0;
		constellationWarningStartAge = Integer.MIN_VALUE;
	}

	private static String keyCodeToPrompt(int keyCode) {
		return switch (keyCode) {
			case GLFW.GLFW_KEY_W -> "W";
			case GLFW.GLFW_KEY_A -> "A";
			case GLFW.GLFW_KEY_S -> "S";
			case GLFW.GLFW_KEY_D -> "D";
			default -> "";
		};
	}

	private static int colorForSchool(MagicSchool school) {
		return switch (school) {
			case FROST -> FROST_COLOR;
			case LOVE -> LOVE_COLOR;
			case BURNING_PASSION -> BURNING_PASSION_COLOR;
			case JESTER -> JESTER_COLOR;
			case CONSTELLATION -> CONSTELLATION_COLOR;
			case GREED -> GREED_COLOR;
			default -> 0xFFFFFF;
		};
	}
}
