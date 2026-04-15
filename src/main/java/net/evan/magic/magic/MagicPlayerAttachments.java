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

final class MagicPlayerAttachments {
	static final AttachmentType<String> MAGIC_SCHOOL = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "magic_school"),
		builder -> builder
			.initializer(MagicSchool.NONE::id)
			.persistent(Codec.STRING)
			.copyOnDeath()
			.syncWith(PacketCodecs.STRING, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> CURRENT_MANA = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "current_mana"),
		builder -> builder
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.copyOnDeath()
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> ACTIVE_ABILITY_SLOT = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "active_ability_slot"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Boolean> DEPLETED_RECOVERY_MODE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "depleted_recovery_mode"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(PacketCodecs.BOOLEAN, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Boolean> PLUS_ULTRA_ACTIVE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "plus_ultra_active"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(PacketCodecs.BOOLEAN, AttachmentSyncPredicate.all())
	);

	static final AttachmentType<Boolean> DOMAIN_CLASH_ACTIVE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_clash_active"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(PacketCodecs.BOOLEAN, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> DOMAIN_CLASH_PROGRESS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_clash_progress"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> DOMAIN_CLASH_PROMPT_KEY = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_clash_prompt_key"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> DOMAIN_CLASH_INSTRUCTION_VISIBILITY = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_clash_instruction_visibility"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<String> DOMAIN_TIMER_ABILITY_ID = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_timer_ability_id"),
		builder -> builder
			.initializer(() -> "")
			.syncWith(PacketCodecs.STRING, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> DOMAIN_TIMER_SECONDS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_timer_seconds"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> DOMAIN_SECONDARY_TIMER_SECONDS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "domain_secondary_timer_seconds"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> GREED_COINS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "greed_coins"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Boolean> FROST_STAGE_ACTIVE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "frost_stage_active"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(PacketCodecs.BOOLEAN, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> FROST_STAGE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "frost_stage"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> FROST_HIGHEST_UNLOCKED_STAGE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "frost_highest_unlocked_stage"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> FROST_STAGE_PROGRESS_TICKS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "frost_stage_progress_ticks"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> FROST_STAGE_REQUIRED_TICKS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "frost_stage_required_ticks"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Boolean> BURNING_PASSION_STAGE_ACTIVE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "burning_passion_stage_active"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(PacketCodecs.BOOLEAN, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> BURNING_PASSION_STAGE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "burning_passion_stage"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> BURNING_PASSION_STAGE_REMAINING_TICKS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "burning_passion_stage_remaining_ticks"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Integer> BURNING_PASSION_HEAT_TENTHS = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "burning_passion_heat_tenths"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<String> BURNING_PASSION_HUD_NOTIFICATION = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "burning_passion_hud_notification"),
		builder -> builder
			.initializer(() -> "")
			.syncWith(PacketCodecs.STRING, AttachmentSyncPredicate.targetOnly())
	);

	static final AttachmentType<Boolean> BURNING_PASSION_ENGINE_HEART_AFTERIMAGE_ACTIVE = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "burning_passion_engine_heart_tier_three_afterimage_active"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(PacketCodecs.BOOLEAN, AttachmentSyncPredicate.all())
	);

	static final AttachmentType<Integer> BURNING_PASSION_ENGINE_HEART_AFTERIMAGE_TIER = AttachmentRegistry.create(
		Identifier.of(Magic.MOD_ID, "burning_passion_engine_heart_afterimage_tier"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(PacketCodecs.INTEGER, AttachmentSyncPredicate.all())
	);

	private MagicPlayerAttachments() {
	}

	static AttachmentTarget target(PlayerEntity player) {
		if (player instanceof AttachmentTarget target) {
			return target;
		}

		throw new IllegalStateException("Player does not implement AttachmentTarget");
	}

	static <T> void setAttachedIfChanged(AttachmentTarget target, AttachmentType<T> attachment, T value, T fallbackValue) {
		T currentValue = target.getAttachedOrElse(attachment, fallbackValue);
		if (Objects.equals(currentValue, value)) {
			return;
		}

		target.setAttached(attachment, value);
	}
}
