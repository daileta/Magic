package net.evan.magic.magic;

import com.mojang.serialization.Codec;
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
	private static final int GREED_COIN_UNITS_PER_COIN = 2;

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

	private static final AttachmentType<Integer> GREED_COINS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "greed_coins"),
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
		target.setAttached(MAGIC_SCHOOL, school.id());
		target.setAttached(CURRENT_MANA, MAX_MANA);
		target.setAttached(ACTIVE_ABILITY_SLOT, 0);
		target.setAttached(DEPLETED_RECOVERY_MODE, false);
		target.setAttached(DOMAIN_CLASH_ACTIVE, false);
		target.setAttached(DOMAIN_CLASH_PROGRESS, 0);
		target.setAttached(DOMAIN_CLASH_PROMPT_KEY, 0);
		target.setAttached(DOMAIN_CLASH_INSTRUCTION_VISIBILITY, 0);
		target.setAttached(GREED_COINS, 0);
	}

	public static void clear(PlayerEntity player) {
		AttachmentTarget target = target(player);
		target.setAttached(MAGIC_SCHOOL, MagicSchool.NONE.id());
		target.setAttached(CURRENT_MANA, 0);
		target.setAttached(ACTIVE_ABILITY_SLOT, 0);
		target.setAttached(DEPLETED_RECOVERY_MODE, false);
		target.setAttached(DOMAIN_CLASH_ACTIVE, false);
		target.setAttached(DOMAIN_CLASH_PROGRESS, 0);
		target.setAttached(DOMAIN_CLASH_PROMPT_KEY, 0);
		target.setAttached(DOMAIN_CLASH_INSTRUCTION_VISIBILITY, 0);
		target.setAttached(GREED_COINS, 0);
	}

	public static int getMana(PlayerEntity player) {
		int mana = target(player).getAttachedOrElse(CURRENT_MANA, 0);
		return MathHelper.clamp(mana, 0, MAX_MANA);
	}

	public static void setMana(PlayerEntity player, int mana) {
		target(player).setAttached(CURRENT_MANA, MathHelper.clamp(mana, 0, MAX_MANA));
	}

	public static void refillMana(PlayerEntity player) {
		setMana(player, MAX_MANA);
	}

	public static int getActiveAbilitySlot(PlayerEntity player) {
		return target(player).getAttachedOrElse(ACTIVE_ABILITY_SLOT, 0);
	}

	public static void setActiveAbilitySlot(PlayerEntity player, int slot) {
		target(player).setAttached(ACTIVE_ABILITY_SLOT, Math.max(0, slot));
	}

	public static boolean isInDepletedRecoveryMode(PlayerEntity player) {
		return target(player).getAttachedOrElse(DEPLETED_RECOVERY_MODE, false);
	}

	public static void setDepletedRecoveryMode(PlayerEntity player, boolean enabled) {
		target(player).setAttached(DEPLETED_RECOVERY_MODE, enabled);
	}

	public static boolean isDomainClashActive(PlayerEntity player) {
		return target(player).getAttachedOrElse(DOMAIN_CLASH_ACTIVE, false);
	}

	public static void setDomainClashActive(PlayerEntity player, boolean active) {
		target(player).setAttached(DOMAIN_CLASH_ACTIVE, active);
	}

	public static int getDomainClashProgress(PlayerEntity player) {
		return MathHelper.clamp(target(player).getAttachedOrElse(DOMAIN_CLASH_PROGRESS, 0), 0, 100);
	}

	public static void setDomainClashProgress(PlayerEntity player, int progress) {
		target(player).setAttached(DOMAIN_CLASH_PROGRESS, MathHelper.clamp(progress, 0, 100));
	}

	public static int getDomainClashPromptKey(PlayerEntity player) {
		return target(player).getAttachedOrElse(DOMAIN_CLASH_PROMPT_KEY, 0);
	}

	public static void setDomainClashPromptKey(PlayerEntity player, int promptKey) {
		target(player).setAttached(DOMAIN_CLASH_PROMPT_KEY, Math.max(0, promptKey));
	}

	public static int getDomainClashInstructionVisibility(PlayerEntity player) {
		return MathHelper.clamp(target(player).getAttachedOrElse(DOMAIN_CLASH_INSTRUCTION_VISIBILITY, 0), 0, 100);
	}

	public static void setDomainClashInstructionVisibility(PlayerEntity player, int visibility) {
		target(player).setAttached(DOMAIN_CLASH_INSTRUCTION_VISIBILITY, MathHelper.clamp(visibility, 0, 100));
	}

	public static void clearDomainClashUi(PlayerEntity player) {
		AttachmentTarget target = target(player);
		target.setAttached(DOMAIN_CLASH_ACTIVE, false);
		target.setAttached(DOMAIN_CLASH_PROGRESS, 0);
		target.setAttached(DOMAIN_CLASH_PROMPT_KEY, 0);
		target.setAttached(DOMAIN_CLASH_INSTRUCTION_VISIBILITY, 0);
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
		return coinUnits / GREED_COIN_UNITS_PER_COIN + ".5";
	}

	public static void setGreedCoinUnits(PlayerEntity player, int coinUnits) {
		target(player).setAttached(GREED_COINS, Math.max(0, coinUnits));
	}

	private static AttachmentTarget target(PlayerEntity player) {
		if (player instanceof AttachmentTarget target) {
			return target;
		}

		throw new IllegalStateException("Player does not implement AttachmentTarget");
	}
}
