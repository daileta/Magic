package net.evan.magic.client;

import java.util.ArrayList;
import java.util.List;
import net.evan.magic.Magic;
import net.evan.magic.config.MagicConfig;
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.MagicSchool;
import net.evan.magic.magic.ability.MagicAbility;
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
	private static final int GREED_COINS_MARGIN = 6;
	private static final int GREED_COINS_BOX_PADDING_X = 4;
	private static final int GREED_COINS_BOX_PADDING_Y = 3;
	private static final int GREED_COINS_SCHOOL_GAP = 6;
	private static final int GREED_COINS_LINE_GAP = 2;
	private static final int GREED_COINS_BOX_BACKGROUND = 0x70000000;
	private static final int GREED_COINS_LABEL_COLOR = 0xFFF4DE95;
	private static final int GREED_COINS_VALUE_COLOR = 0xFFFFC94A;
	private static final int CLASH_BAR_BACKGROUND = 0xAA222222;
	private static final int CLASH_BAR_PROGRESS = 0xFF29CC72;
	private static final int CLASH_BAR_WRONG_FLASH = 0xFFFF2B2B;
	private static final int CLASH_BAR_WRONG_FLASH_TICKS = 5;
	private static final int PROMPT_TRANSITION_TICKS = 4;
	private static final float DOMAIN_TIMER_SCALE = 1.15F;
	private static final int DOMAIN_TIMER_Y = 10;
	private static final int DOMAIN_TIMER_LINE_SPACING = 12;
	private static final float DOMAIN_CLASH_TITLE_SCALE = 2.35F;
	private static final float PROMPT_SCALE = 3.35F;
	private static final int PROMPT_COLOR = 0xFFFF3A3A;
	private static final float INSTRUCTION_SCALE = 1.55F;
	private static final int INSTRUCTION_LINE_SPACING = 14;
	private static final int INSTRUCTION_COLOR = 0xFFFF3A3A;
	private static final int OUTLINE_COLOR = 0xFF000000;
	private static final int FROST_DOMAIN_TIMER_COLOR = 0xFF00FFFF;
	private static final int DOMAIN_TIMER_SECONDARY_COLOR = 0xFFF7D76E;
	private static final int FROST_COLOR = 0x66CCFF;
	private static final int FROST_HUD_TEXT_COLOR = 0xFFFFFF;
	private static final int FROST_OUTLINE_COLOR = 0xFF000000;
	private static final int FROST_STAGE_HUD_BACKGROUND = 0x70000000;
	private static final int BURNING_PASSION_HUD_BACKGROUND = 0x70000000;
	private static final int BURNING_PASSION_HEAT_BAR_BACKGROUND = 0x66000000;
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
	private static int constellationWarningXOffset = Integer.MIN_VALUE;
	private static int constellationWarningYOffset = Integer.MIN_VALUE;
	private static int constellationWarningStartAge = Integer.MIN_VALUE;
	private static List<Text> activeGreedDomainWarningLines = List.of();
	private static int activeGreedDomainWarningColor = 0xFFFFFFFF;
	private static int activeGreedDomainWarningOutlineColor = 0xFF000000;
	private static float greedDomainWarningScale = 0.95F;
	private static int greedDomainWarningDurationTicks = 0;
	private static int greedDomainWarningLineSpacing = 11;
	private static int greedDomainWarningStartAge = Integer.MIN_VALUE;

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
			clearGreedDomainWarning();
			return;
		}

		renderConstellationWarningOverlay(drawContext, client);
		renderJesterJokeOverlay(drawContext, client);
		renderGreedDomainWarningOverlay(drawContext, client);
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
		renderDomainTimerOverlay(drawContext, client);
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
			renderGreedCoins(drawContext, client, textY);
		} else if (school == MagicSchool.FROST) {
			renderFrostStageHud(drawContext, client, textY);
		} else if (school == MagicSchool.BURNING_PASSION) {
			renderBurningPassionHud(drawContext, client, x, y, textY);
		}
		drawContext.drawTextWithShadow(client.textRenderer, schoolName, textX, textY, colorForSchool(school));
	}

	private static void renderGreedCoins(DrawContext drawContext, MinecraftClient client, int schoolTextY) {
		Text label = Text.literal("Coins");
		Text value = Text.literal(MagicPlayerData.formatGreedCoins(client.player)).formatted(Formatting.BOLD);
		int labelWidth = client.textRenderer.getWidth(label);
		int valueWidth = client.textRenderer.getWidth(value);
		int contentWidth = Math.max(labelWidth, valueWidth);
		int contentHeight = client.textRenderer.fontHeight * 2 + GREED_COINS_LINE_GAP;
		int coinsX = drawContext.getScaledWindowWidth() - contentWidth - GREED_COINS_MARGIN - GREED_COINS_BOX_PADDING_X;
		int coinsY = schoolTextY - contentHeight - GREED_COINS_SCHOOL_GAP;
		drawContext.fill(
			coinsX - GREED_COINS_BOX_PADDING_X,
			coinsY - GREED_COINS_BOX_PADDING_Y,
			coinsX + contentWidth + GREED_COINS_BOX_PADDING_X,
			coinsY + contentHeight + GREED_COINS_BOX_PADDING_Y,
			GREED_COINS_BOX_BACKGROUND
		);
		drawContext.drawTextWithShadow(
			client.textRenderer,
			label,
			coinsX + (contentWidth - labelWidth) / 2,
			coinsY,
			GREED_COINS_LABEL_COLOR
		);
		drawContext.drawTextWithShadow(
			client.textRenderer,
			value,
			coinsX + (contentWidth - valueWidth) / 2,
			coinsY + client.textRenderer.fontHeight + GREED_COINS_LINE_GAP,
			GREED_COINS_VALUE_COLOR
		);
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
		int fadeOutTicks,
		int xOffset,
		int yOffset
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		activeConstellationWarning = Text.literal(message == null ? "" : message);
		activeConstellationWarningColor = 0xFF000000 | (colorRgb & 0x00FFFFFF);
		constellationWarningScale = Math.max(0.5F, scale);
		constellationWarningFadeInTicks = Math.max(0, fadeInTicks);
		constellationWarningStayTicks = Math.max(0, stayTicks);
		constellationWarningFadeOutTicks = Math.max(0, fadeOutTicks);
		constellationWarningXOffset = xOffset;
		constellationWarningYOffset = yOffset;
		constellationWarningStartAge = client.player == null ? Integer.MIN_VALUE : client.player.age;
	}

	public static void showGreedDomainWarning(
		String message,
		int colorRgb,
		int outlineColorRgb,
		float scale,
		int durationTicks,
		int lineSpacing
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		ArrayList<Text> lines = new ArrayList<>();
		String rawMessage = message == null ? "" : message;
		for (String line : rawMessage.split("\\R", -1)) {
			lines.add(Text.literal(line));
		}
		activeGreedDomainWarningLines = lines;
		activeGreedDomainWarningColor = 0xFF000000 | (colorRgb & 0x00FFFFFF);
		activeGreedDomainWarningOutlineColor = 0xFF000000 | (outlineColorRgb & 0x00FFFFFF);
		greedDomainWarningScale = Math.max(0.5F, scale);
		greedDomainWarningDurationTicks = Math.max(0, durationTicks);
		greedDomainWarningLineSpacing = Math.max(8, lineSpacing);
		greedDomainWarningStartAge = client.player == null ? Integer.MIN_VALUE : client.player.age;
	}

	private static void renderDomainTimerOverlay(DrawContext drawContext, MinecraftClient client) {
		String abilityId = MagicPlayerData.getDomainTimerAbilityId(client.player);
		if (abilityId.isBlank()) {
			return;
		}

		MagicAbility ability = MagicAbility.fromId(abilityId);
		if (ability == MagicAbility.NONE) {
			return;
		}

		int centerX = drawContext.getScaledWindowWidth() / 2;
		int remainingSeconds = MagicPlayerData.getDomainTimerSeconds(client.player);
		boolean paused = MagicPlayerData.isDomainClashActive(client.player);
		boolean frostDomainTimer = ability == MagicAbility.FROST_DOMAIN_EXPANSION;
		Text timerLine = paused
			? Text.translatable("overlay.magic.domain.timer.paused", ability.displayName(), remainingSeconds)
			: Text.translatable("overlay.magic.domain.timer", ability.displayName(), remainingSeconds);
		if (!frostDomainTimer) {
			timerLine = timerLine.copy().formatted(Formatting.BOLD);
		}
		if (frostDomainTimer) {
			drawCenteredScaledText(
				drawContext,
				client,
				timerLine,
				centerX,
				DOMAIN_TIMER_Y,
				DOMAIN_TIMER_SCALE,
				FROST_DOMAIN_TIMER_COLOR
			);
		} else {
			drawCenteredScaledOutlinedText(
				drawContext,
				client,
				timerLine,
				centerX,
				DOMAIN_TIMER_Y,
				DOMAIN_TIMER_SCALE,
				colorForSchool(ability.school()),
				OUTLINE_COLOR
			);
		}

		int secondarySeconds = MagicPlayerData.getDomainSecondaryTimerSeconds(client.player);
		if (secondarySeconds <= 0) {
			return;
		}

		Text secondaryLine = Text.translatable("overlay.magic.constellation.beam_power", secondarySeconds).formatted(Formatting.BOLD);
		drawCenteredScaledOutlinedText(
			drawContext,
			client,
			secondaryLine,
			centerX,
			DOMAIN_TIMER_Y + DOMAIN_TIMER_LINE_SPACING,
			DOMAIN_TIMER_SCALE,
			DOMAIN_TIMER_SECONDARY_COLOR,
			OUTLINE_COLOR
		);
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
		int centerX = drawContext.getScaledWindowWidth() / 2;
		int centerY = drawContext.getScaledWindowHeight() / 2 - 24;
		drawCenteredScaledOutlinedText(
			drawContext,
			client,
			Text.translatable("overlay.magic.domain_clash.title").formatted(Formatting.BOLD),
			centerX,
			centerY - 46,
			DOMAIN_CLASH_TITLE_SCALE,
			PROMPT_COLOR,
			OUTLINE_COLOR
		);

		if (promptKey == 0 && instructionVisibility <= 0) {
			return;
		}

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

	private static void renderFrostStageHud(DrawContext drawContext, MinecraftClient client, int schoolTextY) {
		if (client.player == null || MagicPlayerData.getSchool(client.player) != MagicSchool.FROST) {
			return;
		}
		MagicConfig.FrostHudConfig hud = MagicConfig.get().frost.hud;
		int currentStage = MagicPlayerData.getFrostStage(client.player);
		int highestUnlockedStage = MagicPlayerData.getFrostHighestUnlockedStage(client.player);
		int progressTicks = MagicPlayerData.getFrostStageProgressTicks(client.player);
		int requiredTicks = MagicPlayerData.getFrostStageRequiredTicks(client.player);
		if (
			!MagicPlayerData.isFrostStageActive(client.player)
			&& currentStage <= 0
			&& highestUnlockedStage <= 0
			&& progressTicks <= 0
			&& requiredTicks <= 0
		) {
			return;
		}
		Text line = frostStageHudText(hud, currentStage, highestUnlockedStage, progressTicks, requiredTicks);
		if (line == null) {
			return;
		}

		String displayLine = line.getString();
		if (displayLine.isBlank()) {
			return;
		}

		int lineWidth = client.textRenderer.getWidth(displayLine);
		int lineX = drawContext.getScaledWindowWidth() - lineWidth - GREED_COINS_MARGIN - GREED_COINS_BOX_PADDING_X;
		int lineY = schoolTextY - client.textRenderer.fontHeight - GREED_COINS_SCHOOL_GAP;
		int textColor = withOpaqueAlpha(
			ensureReadableHudColor(parseHexColor(hud.textColorHex, FROST_HUD_TEXT_COLOR), FROST_HUD_TEXT_COLOR)
		);
		int outlineColor = withOpaqueAlpha(parseHexColor(hud.outlineColorHex, FROST_OUTLINE_COLOR));
		drawContext.fill(
			lineX - GREED_COINS_BOX_PADDING_X,
			lineY - GREED_COINS_BOX_PADDING_Y,
			lineX + lineWidth + GREED_COINS_BOX_PADDING_X,
			lineY + client.textRenderer.fontHeight + GREED_COINS_BOX_PADDING_Y,
			FROST_STAGE_HUD_BACKGROUND
		);
		drawOutlinedText(drawContext, client, Text.literal(displayLine), lineX, lineY, textColor, outlineColor);
	}

	private static void renderBurningPassionHud(
		DrawContext drawContext,
		MinecraftClient client,
		int manaBarX,
		int manaBarY,
		int schoolTextY
	) {
		if (client.player == null || MagicPlayerData.getSchool(client.player) != MagicSchool.BURNING_PASSION) {
			return;
		}

		MagicConfig.BurningPassionHudConfig hud = MagicConfig.get().burningPassion.hud;
		if (hud.hideDuringDomainClash && MagicPlayerData.isDomainClashActive(client.player)) {
			return;
		}

		int currentStage = MagicPlayerData.getBurningPassionStage(client.player);
		int remainingTicks = MagicPlayerData.getBurningPassionStageRemainingTicks(client.player);
		double heatPercent = MagicPlayerData.getBurningPassionHeatPercent(client.player);
		String hudNotification = MagicPlayerData.getBurningPassionHudNotification(client.player);
		if (!MagicPlayerData.isBurningPassionStageActive(client.player) && currentStage <= 0 && remainingTicks <= 0 && heatPercent <= 0.0) {
			return;
		}

		Text stageLine = burningPassionStageHudText(hud, currentStage, remainingTicks);
		Text heatLine = burningPassionHeatHudText(hud, heatPercent);
		int textColor = withOpaqueAlpha(
			ensureReadableHudColor(parseHexColor(hud.textColorHex, FROST_HUD_TEXT_COLOR), FROST_HUD_TEXT_COLOR)
		);
		int heatColor = withOpaqueAlpha(
			ensureReadableHudColor(parseHexColor(hud.heatTextColorHex, 0xFFB56B), 0xFFB56B)
		);
		int outlineColor = withOpaqueAlpha(parseHexColor(hud.outlineColorHex, FROST_OUTLINE_COLOR));
		int heatBarStartColor = withOpaqueAlpha(parseHexColor(hud.heatBarStartColorHex, 0xFFB347));
		int heatBarEndColor = withOpaqueAlpha(parseHexColor(hud.heatBarEndColorHex, 0xFF3B1F));
		int centerX = manaBarX + BAR_WIDTH / 2;
		if (hud.heatBarEnabled) {
			int heatBarY = manaBarY - hud.heatBarYOffset;
			int heatBarHeight = hud.heatBarHeight;
			int heatFillWidth = MathHelper.clamp((int) Math.round((heatPercent / 100.0) * BAR_WIDTH), 0, BAR_WIDTH);
			drawContext.fill(manaBarX, heatBarY, manaBarX + BAR_WIDTH, heatBarY + heatBarHeight, BURNING_PASSION_HEAT_BAR_BACKGROUND);
			if (heatFillWidth > 0) {
				drawGradientRect(
					drawContext,
					manaBarX,
					heatBarY,
					manaBarX + heatFillWidth,
					heatBarY + heatBarHeight,
					heatBarStartColor,
					heatBarEndColor
				);
			}
		}

		if (hud.heatTextEnabled) {
			String heatText = hudNotification == null || hudNotification.isBlank() ? (heatLine == null ? "" : heatLine.getString()) : hudNotification;
			if (!heatText.isBlank()) {
				int renderedTextColor = hudNotification == null || hudNotification.isBlank() ? heatColor : textColor;
				drawCenteredHudBoxedText(
					drawContext,
					client,
					Text.literal(heatText),
					centerX,
					manaBarY - hud.heatTextYOffset,
					renderedTextColor,
					outlineColor
				);
			}
		}

		if (stageLine != null && hud.timerEnabled) {
			String stageText = stageLine.getString();
			if (!stageText.isBlank()) {
				if (hud.timerBottomRightAnchor) {
					int lineWidth = client.textRenderer.getWidth(stageText);
					int lineX = drawContext.getScaledWindowWidth() - lineWidth - GREED_COINS_MARGIN - GREED_COINS_BOX_PADDING_X;
					int lineY = schoolTextY - client.textRenderer.fontHeight - GREED_COINS_SCHOOL_GAP;
					drawContext.fill(
						lineX - GREED_COINS_BOX_PADDING_X,
						lineY - GREED_COINS_BOX_PADDING_Y,
						lineX + lineWidth + GREED_COINS_BOX_PADDING_X,
						lineY + client.textRenderer.fontHeight + GREED_COINS_BOX_PADDING_Y,
						BURNING_PASSION_HUD_BACKGROUND
					);
					drawOutlinedText(drawContext, client, Text.literal(stageText), lineX, lineY, textColor, outlineColor);
				} else {
					drawCenteredHudBoxedText(
						drawContext,
						client,
						Text.literal(stageText),
						centerX,
						manaBarY - hud.stageTextYOffset,
						textColor,
						outlineColor
					);
				}
			}
		}
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
		drawCenteredScaledOutlinedText(
			drawContext,
			client,
			prompt,
			centerX,
			centerY,
			PROMPT_SCALE,
			withAlpha(PROMPT_COLOR, alpha),
			withAlpha(OUTLINE_COLOR, alpha)
		);
	}

	private static void drawScaledInstruction(
		DrawContext drawContext,
		MinecraftClient client,
		Text line,
		int centerX,
		int centerY,
		float alpha
	) {
		drawCenteredScaledOutlinedText(
			drawContext,
			client,
			line,
			centerX,
			centerY,
			INSTRUCTION_SCALE,
			withAlpha(INSTRUCTION_COLOR, alpha),
			withAlpha(OUTLINE_COLOR, alpha)
		);
	}

	private static Text frostStageHudText(
		MagicConfig.FrostHudConfig hud,
		int currentStage,
		int highestUnlockedStage,
		int progressTicks,
		int requiredTicks
	) {
		if (currentStage >= 3) {
			if (requiredTicks > 0) {
				int clampedProgress = Math.min(progressTicks, requiredTicks);
				if (clampedProgress >= requiredTicks) {
					if (!hud.readyTextEnabled) {
						return null;
					}
					String fallback = Text.translatable("overlay.magic.frost.maximum.ready").getString();
					return Text.literal(formatFrostHudText(hud.maximumReadyLabelFormat, fallback, null));
				}
				if (!hud.timerEnabled) {
					return null;
				}
				String timeText = formatRemainingTime(Math.max(0, requiredTicks - clampedProgress));
				String fallback = Text.translatable("overlay.magic.frost.maximum.timer", timeText).getString();
				return Text.literal(formatFrostHudText(hud.maximumTimerLabelFormat, fallback, timeText));
			}
			if (!hud.showFinalStageText) {
				return null;
			}
			String fallback = Text.translatable("overlay.magic.frost.stage.final").getString();
			return Text.literal(hud.finalStageText.isBlank() ? fallback : hud.finalStageText);
		}

		int nextStage = Math.min(3, currentStage + 1);
		if (highestUnlockedStage > currentStage) {
			if (!hud.readyTextEnabled) {
				return null;
			}
			String fallback = Text.translatable("overlay.magic.frost.stage.ready", nextStage).getString();
			return Text.literal(formatHudLabel(hud.readyLabelFormat, nextStage, null, fallback));
		}

		if (!hud.timerEnabled || requiredTicks <= 0) {
			return null;
		}

		int remainingTicks = Math.max(0, requiredTicks - progressTicks);
		String timeText = formatRemainingTime(remainingTicks);
		String fallback = Text.translatable("overlay.magic.frost.stage.timer", nextStage, timeText).getString();
		return Text.literal(
			formatHudLabel(hud.timerLabelFormat, nextStage, timeText, fallback)
		);
	}

	private static Text burningPassionStageHudText(
		MagicConfig.BurningPassionHudConfig hud,
		int currentStage,
		int remainingTicks
	) {
		if (currentStage <= 0 || remainingTicks < 0) {
			return null;
		}
		String timeText = formatRemainingTime(remainingTicks);
		String fallback = switch (currentStage) {
			case 1 -> Text.translatable("overlay.magic.burning_passion.stage_one.timer", timeText).getString();
			case 2 -> Text.translatable("overlay.magic.burning_passion.stage_two.timer", timeText).getString();
			case 3 -> Text.translatable("overlay.magic.burning_passion.stage_three.timer", timeText).getString();
			default -> "";
		};
		String template = switch (currentStage) {
			case 1 -> hud.stageOneTimerLabelFormat;
			case 2 -> hud.stageTwoTimerLabelFormat;
			case 3 -> hud.stageThreeTimerLabelFormat;
			default -> "";
		};
		return fallback.isBlank() ? null : Text.literal(formatBurningPassionHudText(template, fallback, timeText, null));
	}

	private static Text burningPassionHeatHudText(MagicConfig.BurningPassionHudConfig hud, double heatPercent) {
		String fallback = Text.translatable("overlay.magic.burning_passion.heat", String.format(java.util.Locale.ROOT, "%.1f", heatPercent)).getString();
		return Text.literal(
			formatBurningPassionHudText(
				hud.heatLabelFormat,
				fallback,
				null,
				String.format(java.util.Locale.ROOT, "%.1f", heatPercent)
			)
		);
	}

	private static void drawCenteredHudBoxedText(
		DrawContext drawContext,
		MinecraftClient client,
		Text text,
		int centerX,
		int y,
		int textColor,
		int outlineColor
	) {
		int lineWidth = client.textRenderer.getWidth(text);
		int lineX = centerX - lineWidth / 2;
		drawContext.fill(
			lineX - GREED_COINS_BOX_PADDING_X,
			y - GREED_COINS_BOX_PADDING_Y,
			lineX + lineWidth + GREED_COINS_BOX_PADDING_X,
			y + client.textRenderer.fontHeight + GREED_COINS_BOX_PADDING_Y,
			BURNING_PASSION_HUD_BACKGROUND
		);
		drawOutlinedText(drawContext, client, text, lineX, y, textColor, outlineColor);
	}

	private static void drawGradientRect(
		DrawContext drawContext,
		int left,
		int top,
		int right,
		int bottom,
		int startColor,
		int endColor
	) {
		int width = Math.max(1, right - left);
		for (int offset = 0; offset < width; offset++) {
			float progress = width <= 1 ? 1.0F : offset / (float) (width - 1);
			int color = interpolateColor(startColor, endColor, progress);
			drawContext.fill(left + offset, top, left + offset + 1, bottom, color);
		}
	}

	private static void drawCenteredScaledOutlinedText(
		DrawContext drawContext,
		MinecraftClient client,
		Text text,
		int centerX,
		int y,
		float scale,
		int textColor,
		int outlineColor
	) {
		int lineWidth = client.textRenderer.getWidth(text);
		var matrices = drawContext.getMatrices();
		matrices.pushMatrix();
		matrices.scale(scale, scale);
		int drawX = Math.round((centerX - (lineWidth * scale) / 2.0F) / scale);
		int drawY = Math.round(y / scale);
		drawOutlinedText(drawContext, client, text, drawX, drawY, textColor, outlineColor);
		matrices.popMatrix();
	}

	private static void drawCenteredScaledText(
		DrawContext drawContext,
		MinecraftClient client,
		Text text,
		int centerX,
		int y,
		float scale,
		int textColor
	) {
		int lineWidth = client.textRenderer.getWidth(text);
		var matrices = drawContext.getMatrices();
		matrices.pushMatrix();
		matrices.scale(scale, scale);
		int drawX = Math.round((centerX - (lineWidth * scale) / 2.0F) / scale);
		int drawY = Math.round(y / scale);
		drawContext.drawText(client.textRenderer, text, drawX, drawY, textColor, false);
		matrices.popMatrix();
	}

	private static void drawOutlinedText(
		DrawContext drawContext,
		MinecraftClient client,
		Text text,
		int x,
		int y,
		int textColor,
		int outlineColor
	) {
		drawContext.drawText(client.textRenderer, text, x - 1, y, outlineColor, false);
		drawContext.drawText(client.textRenderer, text, x + 1, y, outlineColor, false);
		drawContext.drawText(client.textRenderer, text, x, y - 1, outlineColor, false);
		drawContext.drawText(client.textRenderer, text, x, y + 1, outlineColor, false);
		drawContext.drawText(client.textRenderer, text, x - 1, y - 1, outlineColor, false);
		drawContext.drawText(client.textRenderer, text, x - 1, y + 1, outlineColor, false);
		drawContext.drawText(client.textRenderer, text, x + 1, y - 1, outlineColor, false);
		drawContext.drawText(client.textRenderer, text, x + 1, y + 1, outlineColor, false);
		drawContext.drawText(client.textRenderer, text, x, y, textColor, false);
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
		int drawY = Math.round(JESTER_JOKE_Y / fittedScale);
		if (constellationWarningXOffset != Integer.MIN_VALUE) {
			drawX += Math.round(constellationWarningXOffset / fittedScale);
		}
		if (constellationWarningYOffset != Integer.MIN_VALUE) {
			drawY = Math.round((JESTER_JOKE_Y + constellationWarningYOffset) / fittedScale);
		}
		drawContext.drawTextWithShadow(
			client.textRenderer,
			activeConstellationWarning,
			drawX,
			drawY,
			withAlpha(activeConstellationWarningColor, alpha)
		);
		matrices.popMatrix();
	}

	private static void renderGreedDomainWarningOverlay(DrawContext drawContext, MinecraftClient client) {
		if (activeGreedDomainWarningLines.isEmpty()) {
			return;
		}

		if (greedDomainWarningStartAge == Integer.MIN_VALUE) {
			greedDomainWarningStartAge = client.player.age;
		}

		int elapsed = Math.max(0, client.player.age - greedDomainWarningStartAge);
		if (greedDomainWarningDurationTicks <= 0 || elapsed >= greedDomainWarningDurationTicks) {
			clearGreedDomainWarning();
			return;
		}

		int widestLineWidth = 0;
		for (Text line : activeGreedDomainWarningLines) {
			widestLineWidth = Math.max(widestLineWidth, client.textRenderer.getWidth(line));
		}
		int totalHeight = client.textRenderer.fontHeight;
		if (activeGreedDomainWarningLines.size() > 1) {
			totalHeight += (activeGreedDomainWarningLines.size() - 1) * greedDomainWarningLineSpacing;
		}
		float maxWidth = Math.max(1.0F, drawContext.getScaledWindowWidth() - 24.0F);
		float maxHeight = Math.max(1.0F, drawContext.getScaledWindowHeight() - 36.0F);
		float fittedScale = greedDomainWarningScale;
		if (widestLineWidth > 0) {
			fittedScale = Math.min(fittedScale, maxWidth / widestLineWidth);
		}
		if (totalHeight > 0) {
			fittedScale = Math.min(fittedScale, maxHeight / totalHeight);
		}
		fittedScale = Math.max(0.5F, fittedScale);
		int startY = Math.round((drawContext.getScaledWindowHeight() - totalHeight * fittedScale) / 2.0F);
		int centerX = drawContext.getScaledWindowWidth() / 2;
		for (int index = 0; index < activeGreedDomainWarningLines.size(); index++) {
			drawCenteredScaledOutlinedText(
				drawContext,
				client,
				activeGreedDomainWarningLines.get(index),
				centerX,
				startY + Math.round(index * greedDomainWarningLineSpacing * fittedScale),
				fittedScale,
				activeGreedDomainWarningColor,
				activeGreedDomainWarningOutlineColor
			);
		}
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

	private static String formatRemainingTime(int remainingTicks) {
		int totalSeconds = Math.max(0, (remainingTicks + 19) / 20);
		int minutes = totalSeconds / 60;
		int seconds = totalSeconds % 60;
		return minutes + ":" + (seconds < 10 ? "0" + seconds : Integer.toString(seconds));
	}

	private static String formatHudLabel(String template, int stage, String time, String fallback) {
		if (template == null || template.isBlank()) {
			return fallback;
		}
		String value = template.replace("%stage%", Integer.toString(stage)).replace("%time%", time == null ? "" : time);
		if (value.contains("%s")) {
			try {
				if (time == null) {
					value = value.formatted(stage);
				} else {
					value = value.formatted(stage, time);
				}
			} catch (RuntimeException exception) {
				return fallback;
			}
		}
		return value.isBlank() ? fallback : value;
	}

	private static String formatFrostHudText(String template, String fallback, String time) {
		if (template == null || template.isBlank()) {
			return fallback;
		}
		String value = template.replace("%time%", time == null ? "" : time);
		if (value.contains("%s")) {
			try {
				value = time == null ? value.formatted() : value.formatted(time);
			} catch (RuntimeException exception) {
				return fallback;
			}
		}
		return value.isBlank() ? fallback : value;
	}

	private static String formatBurningPassionHudText(String template, String fallback, String time, String heat) {
		if (template == null || template.isBlank()) {
			return fallback;
		}
		String value = template
			.replace("%time%", time == null ? "" : time)
			.replace("%heat%", heat == null ? "" : heat);
		if (value.contains("%s")) {
			try {
				if (time != null && heat != null) {
					value = value.formatted(time, heat);
				} else if (time != null) {
					value = value.formatted(time);
				} else if (heat != null) {
					value = value.formatted(heat);
				} else {
					value = value.formatted();
				}
			} catch (RuntimeException exception) {
				return fallback;
			}
		}
		return value.isBlank() ? fallback : value;
	}

	private static int parseHexColor(String rawColor, int fallbackColor) {
		if (rawColor == null || rawColor.isBlank()) {
			return fallbackColor;
		}
		String normalized = rawColor.trim();
		if (normalized.startsWith("#")) {
			normalized = normalized.substring(1);
		}
		if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
			normalized = normalized.substring(2);
		}
		if (normalized.length() != 6) {
			return fallbackColor;
		}
		try {
			return Integer.parseInt(normalized, 16);
		} catch (NumberFormatException exception) {
			return fallbackColor;
		}
	}

	private static int ensureReadableHudColor(int color, int fallbackColor) {
		int red = (color >> 16) & 0xFF;
		int green = (color >> 8) & 0xFF;
		int blue = color & 0xFF;
		if (red + green + blue < 96) {
			return fallbackColor;
		}
		return color;
	}

	private static int withOpaqueAlpha(int color) {
		return 0xFF000000 | (color & 0x00FFFFFF);
	}

	private static int interpolateColor(int startColor, int endColor, float progress) {
		float clamped = MathHelper.clamp(progress, 0.0F, 1.0F);
		int startAlpha = (startColor >> 24) & 0xFF;
		int startRed = (startColor >> 16) & 0xFF;
		int startGreen = (startColor >> 8) & 0xFF;
		int startBlue = startColor & 0xFF;
		int endAlpha = (endColor >> 24) & 0xFF;
		int endRed = (endColor >> 16) & 0xFF;
		int endGreen = (endColor >> 8) & 0xFF;
		int endBlue = endColor & 0xFF;
		int alpha = Math.round(MathHelper.lerp(clamped, startAlpha, endAlpha));
		int red = Math.round(MathHelper.lerp(clamped, startRed, endRed));
		int green = Math.round(MathHelper.lerp(clamped, startGreen, endGreen));
		int blue = Math.round(MathHelper.lerp(clamped, startBlue, endBlue));
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
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
		constellationWarningXOffset = Integer.MIN_VALUE;
		constellationWarningYOffset = Integer.MIN_VALUE;
		constellationWarningStartAge = Integer.MIN_VALUE;
	}

	private static void clearGreedDomainWarning() {
		activeGreedDomainWarningLines = List.of();
		activeGreedDomainWarningColor = 0xFFFFFFFF;
		activeGreedDomainWarningOutlineColor = 0xFF000000;
		greedDomainWarningScale = 0.95F;
		greedDomainWarningDurationTicks = 0;
		greedDomainWarningLineSpacing = 11;
		greedDomainWarningStartAge = Integer.MIN_VALUE;
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
		return withOpaqueAlpha(switch (school) {
			case FROST -> FROST_COLOR;
			case LOVE -> LOVE_COLOR;
			case BURNING_PASSION -> BURNING_PASSION_COLOR;
			case JESTER -> JESTER_COLOR;
			case CONSTELLATION -> CONSTELLATION_COLOR;
			case GREED -> GREED_COLOR;
			default -> 0xFFFFFF;
		});
	}
}
