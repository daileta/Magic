package net.evan.magic.magic;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.evan.magic.Magic;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class MagicPlayerData {
	public static final int MAX_MANA = 100;
	public static final int GREED_COIN_UNITS_PER_COIN = 100;

	private static final AttachmentType<String> MAGIC_SCHOOL = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "magic_school"),
		builder -> builder
			.initializer(MagicSchool.NONE::id)
			.persistent(Codec.STRING)
			.copyOnDeath()
			.syncWith(PacketCodecs.STRING, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> CURRENT_MANA = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "current_mana"),
		builder -> builder
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.copyOnDeath()
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> ACTIVE_ABILITY_SLOT = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "active_ability_slot"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Boolean> DEPLETED_RECOVERY_MODE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "depleted_recovery_mode"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(PacketCodecs.BOOLEAN, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Boolean> PLUS_ULTRA_ACTIVE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "plus_ultra_active"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(PacketCodecs.BOOLEAN, AttachmentSyncPredicate.all())
	);

	private static final AttachmentType<Boolean> DOMAIN_CLASH_ACTIVE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_clash_active"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(PacketCodecs.BOOLEAN, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> DOMAIN_CLASH_PROGRESS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_clash_progress"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> DOMAIN_CLASH_PROMPT_KEY = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_clash_prompt_key"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> DOMAIN_CLASH_INSTRUCTION_VISIBILITY = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_clash_instruction_visibility"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<String> DOMAIN_TIMER_ABILITY_ID = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_timer_ability_id"),
		builder -> builder
			.initializer(() -> "")
			.syncWith(PacketCodecs.STRING, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> DOMAIN_TIMER_SECONDS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_timer_seconds"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> DOMAIN_SECONDARY_TIMER_SECONDS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_secondary_timer_seconds"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> GREED_COINS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "greed_coins"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Boolean> FROST_STAGE_ACTIVE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "frost_stage_active"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(PacketCodecs.BOOLEAN, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> FROST_STAGE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "frost_stage"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> FROST_HIGHEST_UNLOCKED_STAGE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "frost_highest_unlocked_stage"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> FROST_STAGE_PROGRESS_TICKS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "frost_stage_progress_ticks"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> FROST_STAGE_REQUIRED_TICKS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "frost_stage_required_ticks"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Boolean> BURNING_PASSION_STAGE_ACTIVE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "burning_passion_stage_active"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(PacketCodecs.BOOLEAN, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> BURNING_PASSION_STAGE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "burning_passion_stage"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> BURNING_PASSION_STAGE_REMAINING_TICKS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "burning_passion_stage_remaining_ticks"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> BURNING_PASSION_HEAT_TENTHS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "burning_passion_heat_tenths"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	private MagicPlayerData() {
	}

	public static void initialize() {
		// Intentionally empty. The static fields above are registration side effects.
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
	}

	private static AttachmentTarget target(PlayerEntity player) {
		if (player instanceof AttachmentTarget target) {
			return target;
		}

		throw new IllegalStateException("Player does not implement AttachmentTarget");
	}

	private static <T> void setAttachedIfChanged(AttachmentTarget target, AttachmentType<T> attachment, T value, T fallbackValue) {
		T currentValue = target.getAttachedOrElse(attachment, fallbackValue);
		if (Objects.equals(currentValue, value)) {
			return;
		}

		target.setAttached(attachment, value);
	}
}
