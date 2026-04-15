package net.evan.magic.command;

import java.util.Collection;
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.MagicSchool;
import net.evan.magic.magic.ability.MagicAbility;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

final class MagicCommandFeedback {
	private MagicCommandFeedback() {
	}

	static void sendAllResetFeedback(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.cooldown.reset.all.single", target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.cooldown.reset.all.multiple", targets.size()),
			true
		);
	}

	static void sendAbilityResetFeedback(ServerCommandSource source, Collection<ServerPlayerEntity> targets, MagicAbility ability) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.cooldown.reset.ability.single", ability.displayName(), target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.cooldown.reset.ability.multiple", ability.displayName(), targets.size()),
			true
		);
	}

	static void sendRemoveMagicFeedback(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.remove.single", target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.remove.multiple", targets.size()),
			true
		);
	}

	static void sendAbilityAccessFeedback(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		MagicAbility ability,
		boolean locked
	) {
		String keyPrefix = locked ? "command.magic.ability.lock" : "command.magic.ability.unlock";

		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable(keyPrefix + ".single", ability.displayName(), target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable(keyPrefix + ".multiple", ability.displayName(), targets.size()),
			true
		);
	}

	static void sendStageProgressionAccessFeedback(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		MagicSchool school,
		int stage,
		boolean locked
	) {
		String keyPrefix = locked ? "command.magic.stage_progression.lock" : "command.magic.stage_progression.unlock";

		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable(keyPrefix + ".single", school.displayName(), stage, target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable(keyPrefix + ".multiple", school.displayName(), stage, targets.size()),
			true
		);
	}

	static void sendTestingModeFeedback(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		boolean enabled
	) {
		String keyPrefix = enabled ? "command.magic.testing.enable" : "command.magic.testing.disable";

		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable(keyPrefix + ".single", target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable(keyPrefix + ".multiple", targets.size()),
			true
		);
	}

	static void sendSchoolSetFeedback(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		MagicSchool school
	) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.school.set.single", school.displayName(), target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.school.set.multiple", school.displayName(), targets.size()),
			true
		);
	}

	static void sendGreedCoinAddFeedback(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		double amount
	) {
		String formattedAmount = formatCoinAmount(amount);
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.greed.coins.add.single", formattedAmount, target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.greed.coins.add.multiple", formattedAmount, targets.size()),
			true
		);
	}

	static void sendFrostStageSetFeedback(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		int stage
	) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.frost.stage.set.single", stage, target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.frost.stage.set.multiple", stage, targets.size()),
			true
		);
	}

	static void sendFrostStageClearFeedback(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.frost.stage.clear.single", target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.frost.stage.clear.multiple", targets.size()),
			true
		);
	}

	static void sendFrostStageNextFeedback(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.frost.stage.next.single", target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.frost.stage.next.multiple", targets.size()),
			true
		);
	}

	static void sendFrostStageProgressFeedback(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		int seconds
	) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.frost.stage.progress.set.single", seconds, target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.frost.stage.progress.set.multiple", seconds, targets.size()),
			true
		);
	}

	static void sendFrostStageThreeProgressFeedback(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		int seconds
	) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.frost.stage.progress.maximum.set.single", seconds, target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.frost.stage.progress.maximum.set.multiple", seconds, targets.size()),
			true
		);
	}

	static void sendBurningPassionStageSetFeedback(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		int stage
	) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.burning_passion.ignition.stage.set.single", stage, target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.burning_passion.ignition.stage.set.multiple", stage, targets.size()),
			true
		);
	}

	static void sendBurningPassionStageNextFeedback(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.burning_passion.ignition.stage.next.single", target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.burning_passion.ignition.stage.next.multiple", targets.size()),
			true
		);
	}

	static void sendBurningPassionStageProgressFeedback(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		int seconds
	) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.burning_passion.ignition.stage.progress.set.single", seconds, target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.burning_passion.ignition.stage.progress.set.multiple", seconds, targets.size()),
			true
		);
	}

	static void sendBurningPassionOverrideForceFeedback(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		if (targets.size() == 1) {
			ServerPlayerEntity target = targets.iterator().next();
			source.sendFeedback(
				() -> Text.translatable("command.magic.burning_passion.override.force.single", target.getDisplayName()),
				true
			);
			return;
		}

		source.sendFeedback(
			() -> Text.translatable("command.magic.burning_passion.override.force.multiple", targets.size()),
			true
		);
	}

	private static String formatCoinAmount(double amount) {
		long coinUnits = Math.round(Math.max(0.0, amount) * MagicPlayerData.GREED_COIN_UNITS_PER_COIN);
		long wholeCoins = coinUnits / MagicPlayerData.GREED_COIN_UNITS_PER_COIN;
		long fractionalUnits = coinUnits % MagicPlayerData.GREED_COIN_UNITS_PER_COIN;
		if (fractionalUnits == 0L) {
			return Long.toString(wholeCoins);
		}

		String fractional = fractionalUnits < 10L ? "0" + fractionalUnits : Long.toString(fractionalUnits);
		while (fractional.endsWith("0")) {
			fractional = fractional.substring(0, fractional.length() - 1);
		}
		return wholeCoins + "." + fractional;
	}
}
