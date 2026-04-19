package net.evan.magic.magic.core;

import static net.evan.magic.magic.core.MagicPlayerAttachments.ACTIVE_ABILITY_SLOT;
import static net.evan.magic.magic.core.MagicPlayerAttachments.BURNING_PASSION_ENGINE_HEART_AFTERIMAGE_ACTIVE;
import static net.evan.magic.magic.core.MagicPlayerAttachments.BURNING_PASSION_ENGINE_HEART_AFTERIMAGE_TIER;
import static net.evan.magic.magic.core.MagicPlayerAttachments.BURNING_PASSION_HEAT_TENTHS;
import static net.evan.magic.magic.core.MagicPlayerAttachments.BURNING_PASSION_HUD_NOTIFICATION;
import static net.evan.magic.magic.core.MagicPlayerAttachments.BURNING_PASSION_STAGE;
import static net.evan.magic.magic.core.MagicPlayerAttachments.BURNING_PASSION_STAGE_ACTIVE;
import static net.evan.magic.magic.core.MagicPlayerAttachments.BURNING_PASSION_STAGE_REMAINING_TICKS;
import static net.evan.magic.magic.core.MagicPlayerAttachments.CURRENT_MANA;
import static net.evan.magic.magic.core.MagicPlayerAttachments.DEPLETED_RECOVERY_MODE;
import static net.evan.magic.magic.core.MagicPlayerAttachments.DOMAIN_CLASH_ACTIVE;
import static net.evan.magic.magic.core.MagicPlayerAttachments.DOMAIN_CLASH_INSTRUCTION_VISIBILITY;
import static net.evan.magic.magic.core.MagicPlayerAttachments.DOMAIN_CLASH_PROGRESS;
import static net.evan.magic.magic.core.MagicPlayerAttachments.DOMAIN_CLASH_PROMPT_KEY;
import static net.evan.magic.magic.core.MagicPlayerAttachments.DOMAIN_SECONDARY_TIMER_SECONDS;
import static net.evan.magic.magic.core.MagicPlayerAttachments.DOMAIN_TIMER_ABILITY_ID;
import static net.evan.magic.magic.core.MagicPlayerAttachments.DOMAIN_TIMER_SECONDS;
import static net.evan.magic.magic.core.MagicPlayerAttachments.FROST_HIGHEST_UNLOCKED_STAGE;
import static net.evan.magic.magic.core.MagicPlayerAttachments.FROST_STAGE;
import static net.evan.magic.magic.core.MagicPlayerAttachments.FROST_STAGE_ACTIVE;
import static net.evan.magic.magic.core.MagicPlayerAttachments.FROST_STAGE_PROGRESS_TICKS;
import static net.evan.magic.magic.core.MagicPlayerAttachments.FROST_STAGE_REQUIRED_TICKS;
import static net.evan.magic.magic.core.MagicPlayerAttachments.GREED_COINS;
import static net.evan.magic.magic.core.MagicPlayerAttachments.MAGIC_SCHOOL;
import static net.evan.magic.magic.core.MagicPlayerAttachments.PLUS_ULTRA_ACTIVE;
import static net.evan.magic.magic.core.MagicPlayerAttachments.setAttachedIfChanged;
import static net.evan.magic.magic.core.MagicPlayerAttachments.target;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public final class MagicPlayerData {
	public static final int MAX_MANA = 100;
	public static final int GREED_COIN_UNITS_PER_COIN = 100;

	private MagicPlayerData() {
	}

	public static void initialize() {
	}

	public static MagicSchool getSchool(PlayerEntity player) {
		String schoolId = target(player).getAttachedOrElse(MAGIC_SCHOOL, MagicSchool.NONE.id());
		return MagicSchool.fromId(schoolId);
	}

	public static boolean hasMagic(PlayerEntity player) {
		return getSchool(player).isMagic();
	}

	public static boolean canAttuneTo(PlayerEntity player, MagicSchool school) {
		MagicSchool currentSchool = getSchool(player);
		return currentSchool == MagicSchool.NONE || currentSchool == school;
	}

	public static void unlock(PlayerEntity player, MagicSchool school) {
		if (!school.isMagic()) {
			throw new IllegalArgumentException("Cannot unlock non-magical school");
		}

		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, MAGIC_SCHOOL, school.id(), MagicSchool.NONE.id());
		setAttachedIfChanged(target, CURRENT_MANA, MAX_MANA, 0);
		setAttachedIfChanged(target, ACTIVE_ABILITY_SLOT, 0, 0);
		setAttachedIfChanged(target, DEPLETED_RECOVERY_MODE, false, false);
		setAttachedIfChanged(target, PLUS_ULTRA_ACTIVE, false, false);
		setAttachedIfChanged(target, DOMAIN_CLASH_ACTIVE, false, false);
		setAttachedIfChanged(target, DOMAIN_CLASH_PROGRESS, 0, 0);
		setAttachedIfChanged(target, DOMAIN_CLASH_PROMPT_KEY, 0, 0);
		setAttachedIfChanged(target, DOMAIN_CLASH_INSTRUCTION_VISIBILITY, 0, 0);
		setAttachedIfChanged(target, DOMAIN_TIMER_ABILITY_ID, "", "");
		setAttachedIfChanged(target, DOMAIN_TIMER_SECONDS, 0, 0);
		setAttachedIfChanged(target, DOMAIN_SECONDARY_TIMER_SECONDS, 0, 0);
		setAttachedIfChanged(target, GREED_COINS, 0, 0);
		clearFrostStageHud(player);
		clearBurningPassionHud(player);
	}

	public static void clear(PlayerEntity player) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, MAGIC_SCHOOL, MagicSchool.NONE.id(), MagicSchool.NONE.id());
		setAttachedIfChanged(target, CURRENT_MANA, 0, 0);
		setAttachedIfChanged(target, ACTIVE_ABILITY_SLOT, 0, 0);
		setAttachedIfChanged(target, DEPLETED_RECOVERY_MODE, false, false);
		setAttachedIfChanged(target, PLUS_ULTRA_ACTIVE, false, false);
		setAttachedIfChanged(target, DOMAIN_CLASH_ACTIVE, false, false);
		setAttachedIfChanged(target, DOMAIN_CLASH_PROGRESS, 0, 0);
		setAttachedIfChanged(target, DOMAIN_CLASH_PROMPT_KEY, 0, 0);
		setAttachedIfChanged(target, DOMAIN_CLASH_INSTRUCTION_VISIBILITY, 0, 0);
		setAttachedIfChanged(target, DOMAIN_TIMER_ABILITY_ID, "", "");
		setAttachedIfChanged(target, DOMAIN_TIMER_SECONDS, 0, 0);
		setAttachedIfChanged(target, DOMAIN_SECONDARY_TIMER_SECONDS, 0, 0);
		setAttachedIfChanged(target, GREED_COINS, 0, 0);
		clearFrostStageHud(player);
		clearBurningPassionHud(player);
	}

	public static int getMana(PlayerEntity player) {
		int mana = target(player).getAttachedOrElse(CURRENT_MANA, 0);
		return MathHelper.clamp(mana, 0, MAX_MANA);
	}

	public static void setMana(PlayerEntity player, int mana) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, CURRENT_MANA, MathHelper.clamp(mana, 0, MAX_MANA), 0);
	}

	public static void refillMana(PlayerEntity player) {
		setMana(player, MAX_MANA);
	}

	public static int getActiveAbilitySlot(PlayerEntity player) {
		return target(player).getAttachedOrElse(ACTIVE_ABILITY_SLOT, 0);
	}

	public static void setActiveAbilitySlot(PlayerEntity player, int slot) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, ACTIVE_ABILITY_SLOT, Math.max(0, slot), 0);
	}

	public static boolean isInDepletedRecoveryMode(PlayerEntity player) {
		return target(player).getAttachedOrElse(DEPLETED_RECOVERY_MODE, false);
	}

	public static void setDepletedRecoveryMode(PlayerEntity player, boolean enabled) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, DEPLETED_RECOVERY_MODE, enabled, false);
	}

	public static boolean isPlusUltraActive(PlayerEntity player) {
		return target(player).getAttachedOrElse(PLUS_ULTRA_ACTIVE, false);
	}

	public static void setPlusUltraActive(PlayerEntity player, boolean active) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, PLUS_ULTRA_ACTIVE, active, false);
	}

	public static boolean isDomainClashActive(PlayerEntity player) {
		return target(player).getAttachedOrElse(DOMAIN_CLASH_ACTIVE, false);
	}

	public static void setDomainClashActive(PlayerEntity player, boolean active) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, DOMAIN_CLASH_ACTIVE, active, false);
	}

	public static int getDomainClashProgress(PlayerEntity player) {
		return MathHelper.clamp(target(player).getAttachedOrElse(DOMAIN_CLASH_PROGRESS, 0), 0, 100);
	}

	public static void setDomainClashProgress(PlayerEntity player, int progress) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, DOMAIN_CLASH_PROGRESS, MathHelper.clamp(progress, 0, 100), 0);
	}

	public static int getDomainClashPromptKey(PlayerEntity player) {
		return target(player).getAttachedOrElse(DOMAIN_CLASH_PROMPT_KEY, 0);
	}

	public static void setDomainClashPromptKey(PlayerEntity player, int promptKey) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, DOMAIN_CLASH_PROMPT_KEY, Math.max(0, promptKey), 0);
	}

	public static int getDomainClashInstructionVisibility(PlayerEntity player) {
		return MathHelper.clamp(target(player).getAttachedOrElse(DOMAIN_CLASH_INSTRUCTION_VISIBILITY, 0), 0, 100);
	}

	public static void setDomainClashInstructionVisibility(PlayerEntity player, int visibility) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, DOMAIN_CLASH_INSTRUCTION_VISIBILITY, MathHelper.clamp(visibility, 0, 100), 0);
	}

	public static void clearDomainClashUi(PlayerEntity player) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, DOMAIN_CLASH_ACTIVE, false, false);
		setAttachedIfChanged(target, DOMAIN_CLASH_PROGRESS, 0, 0);
		setAttachedIfChanged(target, DOMAIN_CLASH_PROMPT_KEY, 0, 0);
		setAttachedIfChanged(target, DOMAIN_CLASH_INSTRUCTION_VISIBILITY, 0, 0);
	}

	public static String getDomainTimerAbilityId(PlayerEntity player) {
		return target(player).getAttachedOrElse(DOMAIN_TIMER_ABILITY_ID, "");
	}

	public static int getDomainTimerSeconds(PlayerEntity player) {
		return Math.max(0, target(player).getAttachedOrElse(DOMAIN_TIMER_SECONDS, 0));
	}

	public static int getDomainSecondaryTimerSeconds(PlayerEntity player) {
		return Math.max(0, target(player).getAttachedOrElse(DOMAIN_SECONDARY_TIMER_SECONDS, 0));
	}

	public static void setDomainTimer(PlayerEntity player, String abilityId, int primarySeconds, int secondarySeconds) {
		AttachmentTarget target = target(player);
		String resolvedAbilityId = abilityId == null ? "" : abilityId;
		setAttachedIfChanged(target, DOMAIN_TIMER_ABILITY_ID, resolvedAbilityId, "");
		setAttachedIfChanged(target, DOMAIN_TIMER_SECONDS, Math.max(0, primarySeconds), 0);
		setAttachedIfChanged(target, DOMAIN_SECONDARY_TIMER_SECONDS, Math.max(0, secondarySeconds), 0);
	}

	public static void clearDomainTimer(PlayerEntity player) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, DOMAIN_TIMER_ABILITY_ID, "", "");
		setAttachedIfChanged(target, DOMAIN_TIMER_SECONDS, 0, 0);
		setAttachedIfChanged(target, DOMAIN_SECONDARY_TIMER_SECONDS, 0, 0);
	}

	public static int getGreedCoinUnits(PlayerEntity player) {
		return Math.max(0, target(player).getAttachedOrElse(GREED_COINS, 0));
	}

	public static double getGreedCoins(PlayerEntity player) {
		return getGreedCoinUnits(player) / (double) GREED_COIN_UNITS_PER_COIN;
	}

	public static String formatGreedCoins(PlayerEntity player) {
		int coinUnits = getGreedCoinUnits(player);
		if (coinUnits % GREED_COIN_UNITS_PER_COIN == 0) {
			return Integer.toString(coinUnits / GREED_COIN_UNITS_PER_COIN);
		}

		int wholeCoins = coinUnits / GREED_COIN_UNITS_PER_COIN;
		int fractionalUnits = coinUnits % GREED_COIN_UNITS_PER_COIN;
		String fractional = fractionalUnits < 10 ? "0" + fractionalUnits : Integer.toString(fractionalUnits);
		while (fractional.endsWith("0")) {
			fractional = fractional.substring(0, fractional.length() - 1);
		}
		return wholeCoins + "." + fractional;
	}

	public static void setGreedCoinUnits(PlayerEntity player, int coinUnits) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, GREED_COINS, Math.max(0, coinUnits), 0);
	}

	public static boolean isFrostStageActive(PlayerEntity player) {
		return target(player).getAttachedOrElse(FROST_STAGE_ACTIVE, false);
	}

	public static int getFrostStage(PlayerEntity player) {
		return MathHelper.clamp(target(player).getAttachedOrElse(FROST_STAGE, 0), 0, 3);
	}

	public static int getFrostHighestUnlockedStage(PlayerEntity player) {
		return MathHelper.clamp(target(player).getAttachedOrElse(FROST_HIGHEST_UNLOCKED_STAGE, 0), 0, 3);
	}

	public static int getFrostStageProgressTicks(PlayerEntity player) {
		return Math.max(0, target(player).getAttachedOrElse(FROST_STAGE_PROGRESS_TICKS, 0));
	}

	public static int getFrostStageRequiredTicks(PlayerEntity player) {
		return Math.max(0, target(player).getAttachedOrElse(FROST_STAGE_REQUIRED_TICKS, 0));
	}

	public static void setFrostStageHud(
		PlayerEntity player,
		boolean active,
		int currentStage,
		int highestUnlockedStage,
		int progressTicks,
		int requiredTicks
	) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, FROST_STAGE_ACTIVE, active, false);
		setAttachedIfChanged(target, FROST_STAGE, MathHelper.clamp(currentStage, 0, 3), 0);
		setAttachedIfChanged(target, FROST_HIGHEST_UNLOCKED_STAGE, MathHelper.clamp(highestUnlockedStage, 0, 3), 0);
		setAttachedIfChanged(target, FROST_STAGE_PROGRESS_TICKS, Math.max(0, progressTicks), 0);
		setAttachedIfChanged(target, FROST_STAGE_REQUIRED_TICKS, Math.max(0, requiredTicks), 0);
	}

	public static void clearFrostStageHud(PlayerEntity player) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, FROST_STAGE_ACTIVE, false, false);
		setAttachedIfChanged(target, FROST_STAGE, 0, 0);
		setAttachedIfChanged(target, FROST_HIGHEST_UNLOCKED_STAGE, 0, 0);
		setAttachedIfChanged(target, FROST_STAGE_PROGRESS_TICKS, 0, 0);
		setAttachedIfChanged(target, FROST_STAGE_REQUIRED_TICKS, 0, 0);
	}

	public static boolean isBurningPassionStageActive(PlayerEntity player) {
		return target(player).getAttachedOrElse(BURNING_PASSION_STAGE_ACTIVE, false);
	}

	public static int getBurningPassionStage(PlayerEntity player) {
		return MathHelper.clamp(target(player).getAttachedOrElse(BURNING_PASSION_STAGE, 0), 0, 3);
	}

	public static int getBurningPassionStageRemainingTicks(PlayerEntity player) {
		return Math.max(0, target(player).getAttachedOrElse(BURNING_PASSION_STAGE_REMAINING_TICKS, 0));
	}

	public static double getBurningPassionHeatPercent(PlayerEntity player) {
		return MathHelper.clamp(target(player).getAttachedOrElse(BURNING_PASSION_HEAT_TENTHS, 0) / 10.0, 0.0, 100.0);
	}

	public static void setBurningPassionHud(PlayerEntity player, boolean active, int currentStage, int remainingTicks, double heatPercent) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, BURNING_PASSION_STAGE_ACTIVE, active, false);
		setAttachedIfChanged(target, BURNING_PASSION_STAGE, MathHelper.clamp(currentStage, 0, 3), 0);
		setAttachedIfChanged(target, BURNING_PASSION_STAGE_REMAINING_TICKS, Math.max(0, remainingTicks), 0);
		int heatTenths = MathHelper.clamp((int) Math.round(MathHelper.clamp(heatPercent, 0.0, 100.0) * 10.0), 0, 1000);
		setAttachedIfChanged(target, BURNING_PASSION_HEAT_TENTHS, heatTenths, 0);
	}

	public static void clearBurningPassionHud(PlayerEntity player) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, BURNING_PASSION_STAGE_ACTIVE, false, false);
		setAttachedIfChanged(target, BURNING_PASSION_STAGE, 0, 0);
		setAttachedIfChanged(target, BURNING_PASSION_STAGE_REMAINING_TICKS, 0, 0);
		setAttachedIfChanged(target, BURNING_PASSION_HEAT_TENTHS, 0, 0);
		setAttachedIfChanged(target, BURNING_PASSION_HUD_NOTIFICATION, "", "");
		setAttachedIfChanged(target, BURNING_PASSION_ENGINE_HEART_AFTERIMAGE_ACTIVE, false, false);
		setAttachedIfChanged(target, BURNING_PASSION_ENGINE_HEART_AFTERIMAGE_TIER, 0, 0);
	}

	public static String getBurningPassionHudNotification(PlayerEntity player) {
		return target(player).getAttachedOrElse(BURNING_PASSION_HUD_NOTIFICATION, "");
	}

	public static boolean isBurningPassionEngineHeartAfterimageActive(PlayerEntity player) {
		return target(player).getAttachedOrElse(BURNING_PASSION_ENGINE_HEART_AFTERIMAGE_ACTIVE, false);
	}

	public static int getBurningPassionEngineHeartAfterimageTier(PlayerEntity player) {
		return MathHelper.clamp(target(player).getAttachedOrElse(BURNING_PASSION_ENGINE_HEART_AFTERIMAGE_TIER, 0), 0, 3);
	}

	public static void setBurningPassionHudNotification(PlayerEntity player, String text) {
		AttachmentTarget target = target(player);
		String resolvedText = text == null ? "" : text;
		setAttachedIfChanged(target, BURNING_PASSION_HUD_NOTIFICATION, resolvedText, "");
	}

	public static void setBurningPassionEngineHeartAfterimageState(PlayerEntity player, boolean active, int tier) {
		AttachmentTarget target = target(player);
		setAttachedIfChanged(target, BURNING_PASSION_ENGINE_HEART_AFTERIMAGE_ACTIVE, active, false);
		setAttachedIfChanged(target, BURNING_PASSION_ENGINE_HEART_AFTERIMAGE_TIER, MathHelper.clamp(tier, 0, 3), 0);
	}
}

