package net.evan.magic.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.evan.magic.config.MagicConfig;
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.MagicSchool;
import net.evan.magic.magic.ability.GreedRuntime;
import net.evan.magic.magic.ability.MagicAbility;
import net.evan.magic.magic.ability.MagicAbilityManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

final class MagicCommandActions {
	private MagicCommandActions() {
	}

	static int resetAllForSelf(ServerCommandSource source) throws CommandSyntaxException {
		return resetAll(source, List.of(source.getPlayerOrThrow()));
	}

	static int resetAbilityForSelf(ServerCommandSource source, MagicAbility ability) throws CommandSyntaxException {
		return resetAbility(source, List.of(source.getPlayerOrThrow()), ability);
	}

	static int setAbilityAccessForSelf(
		ServerCommandSource source,
		MagicAbility ability,
		boolean locked
	) throws CommandSyntaxException {
		return setAbilityAccess(source, List.of(source.getPlayerOrThrow()), ability, locked);
	}

	static int setStageProgressionAccessForSelf(
		ServerCommandSource source,
		MagicSchool school,
		int stage,
		boolean locked
	) throws CommandSyntaxException {
		return setStageProgressionAccess(source, List.of(source.getPlayerOrThrow()), school, stage, locked);
	}

	static int setTestingModeForSelf(ServerCommandSource source, boolean enabled) throws CommandSyntaxException {
		return setTestingMode(source, List.of(source.getPlayerOrThrow()), enabled);
	}

	static int setSchoolForSelf(ServerCommandSource source, MagicSchool school) throws CommandSyntaxException {
		return setSchool(source, List.of(source.getPlayerOrThrow()), school);
	}

	static int addGreedCoinsForSelf(ServerCommandSource source, double amount) throws CommandSyntaxException {
		return addGreedCoins(source, List.of(source.getPlayerOrThrow()), amount);
	}

	static int setFrostStageForSelf(ServerCommandSource source, int stage) throws CommandSyntaxException {
		return setFrostStage(source, List.of(source.getPlayerOrThrow()), stage);
	}

	static int clearFrostStageForSelf(ServerCommandSource source) throws CommandSyntaxException {
		return clearFrostStage(source, List.of(source.getPlayerOrThrow()));
	}

	static int advanceFrostStageForSelf(ServerCommandSource source) throws CommandSyntaxException {
		return advanceFrostStage(source, List.of(source.getPlayerOrThrow()));
	}

	static int setFrostStageProgressForSelf(ServerCommandSource source, int seconds) throws CommandSyntaxException {
		return setFrostStageProgress(source, List.of(source.getPlayerOrThrow()), seconds);
	}

	static int setFrostStageThreeProgressForSelf(ServerCommandSource source, int seconds) throws CommandSyntaxException {
		return setFrostStageThreeProgress(source, List.of(source.getPlayerOrThrow()), seconds);
	}

	static int setBurningPassionStageForSelf(ServerCommandSource source, int stage) throws CommandSyntaxException {
		return setBurningPassionStage(source, List.of(source.getPlayerOrThrow()), stage);
	}

	static int advanceBurningPassionStageForSelf(ServerCommandSource source) throws CommandSyntaxException {
		return advanceBurningPassionStage(source, List.of(source.getPlayerOrThrow()));
	}

	static int setBurningPassionStageProgressForSelf(ServerCommandSource source, int seconds) throws CommandSyntaxException {
		return setBurningPassionStageProgress(source, List.of(source.getPlayerOrThrow()), seconds);
	}

	static int forceBurningPassionOverrideForSelf(ServerCommandSource source) throws CommandSyntaxException {
		return forceBurningPassionOverride(source, List.of(source.getPlayerOrThrow()));
	}

	static int resetAll(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		int resetCount = 0;
		for (ServerPlayerEntity target : targets) {
			resetCount += MagicAbilityManager.resetAllCooldowns(target);
		}

		MagicCommandFeedback.sendAllResetFeedback(source, targets);
		return resetCount == 0 ? Command.SINGLE_SUCCESS : resetCount;
	}

	static int resetAbility(ServerCommandSource source, Collection<ServerPlayerEntity> targets, MagicAbility ability) {
		int resetCount = 0;
		for (ServerPlayerEntity target : targets) {
			resetCount += MagicAbilityManager.resetCooldown(target, ability);
		}

		MagicCommandFeedback.sendAbilityResetFeedback(source, targets, ability);
		return resetCount == 0 ? Command.SINGLE_SUCCESS : resetCount;
	}

	static int removeMagic(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		List<UUID> playerIds = new ArrayList<>(targets.size());

		for (ServerPlayerEntity target : targets) {
			playerIds.add(target.getUuid());
			MagicAbilityManager.setTestingMode(target, false);
			MagicAbilityManager.clearAllRuntimeState(target);
			MagicPlayerData.clear(target);
		}

		boolean clearedOverrides = MagicConfig.clearPlayerAbilityOverrides(playerIds);
		if (!clearedOverrides && !playerIds.isEmpty()) {
			source.sendError(Text.translatable("command.magic.remove.override_cleanup.failure", MagicConfig.path().toString()));
		}

		MagicCommandFeedback.sendRemoveMagicFeedback(source, targets);
		return targets.isEmpty() ? 0 : targets.size();
	}

	static int setAbilityAccess(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		MagicAbility ability,
		boolean locked
	) {
		List<UUID> playerIds = playerIds(targets);
		boolean persisted = MagicConfig.setPlayerAbilityLocked(playerIds, ability, locked);
		if (!persisted) {
			source.sendError(Text.translatable("command.magic.ability.access.failure", MagicConfig.path().toString()));
			return 0;
		}

		if (locked) {
			for (ServerPlayerEntity target : targets) {
				MagicAbilityManager.clearLockedAbilityState(target, ability);
			}
		}

		MagicCommandFeedback.sendAbilityAccessFeedback(source, targets, ability, locked);
		return targets.size();
	}

	static int setStageProgressionAccess(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		MagicSchool school,
		int stage,
		boolean locked
	) {
		List<UUID> playerIds = playerIds(targets);
		boolean persisted = MagicConfig.setPlayerStageProgressionLocked(playerIds, school, stage, locked);
		if (!persisted) {
			source.sendError(Text.translatable("command.magic.stage_progression.access.failure", MagicConfig.path().toString()));
			return 0;
		}

		if (locked) {
			for (ServerPlayerEntity target : targets) {
				MagicAbilityManager.clearLockedStageProgressionState(target, school, stage);
			}
		}

		MagicCommandFeedback.sendStageProgressionAccessFeedback(source, targets, school, stage, locked);
		return targets.size();
	}

	static int setTestingMode(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		boolean enabled
	) {
		int changedCount = 0;
		for (ServerPlayerEntity target : targets) {
			if (MagicAbilityManager.setTestingMode(target, enabled)) {
				changedCount++;
			}
		}

		MagicCommandFeedback.sendTestingModeFeedback(source, targets, enabled);
		return changedCount == 0 ? Command.SINGLE_SUCCESS : changedCount;
	}

	static int setSchool(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		MagicSchool school
	) {
		int changedCount = 0;
		for (ServerPlayerEntity target : targets) {
			if (MagicAbilityManager.setMagicSchool(target, school)) {
				changedCount++;
			}
		}

		MagicCommandFeedback.sendSchoolSetFeedback(source, targets, school);
		return changedCount == 0 ? Command.SINGLE_SUCCESS : changedCount;
	}

	static int addGreedCoins(ServerCommandSource source, Collection<ServerPlayerEntity> targets, double amount) {
		List<ServerPlayerEntity> updatedTargets = new ArrayList<>();
		int skippedNonGreed = 0;

		for (ServerPlayerEntity target : targets) {
			if (MagicPlayerData.getSchool(target) != MagicSchool.GREED) {
				skippedNonGreed++;
				continue;
			}
			if (GreedRuntime.addCoins(target, amount) > 0) {
				updatedTargets.add(target);
			}
		}

		if (updatedTargets.isEmpty()) {
			source.sendError(Text.translatable("command.magic.greed.coins.add.failure.no_greed_targets"));
			return 0;
		}

		MagicCommandFeedback.sendGreedCoinAddFeedback(source, updatedTargets, amount);
		if (skippedNonGreed > 0) {
			source.sendError(
				Text.translatable(
					skippedNonGreed == 1
						? "command.magic.greed.coins.add.skipped_non_greed.single"
						: "command.magic.greed.coins.add.skipped_non_greed.multiple",
					skippedNonGreed
				)
			);
		}
		return updatedTargets.size();
	}

	static int setFrostStage(ServerCommandSource source, Collection<ServerPlayerEntity> targets, int stage) {
		List<ServerPlayerEntity> updatedTargets = new ArrayList<>();
		for (ServerPlayerEntity target : targets) {
			if (MagicAbilityManager.setFrostStage(target, stage) > 0) {
				updatedTargets.add(target);
			}
		}

		if (updatedTargets.isEmpty()) {
			source.sendError(Text.translatable("command.magic.frost.stage.failure.no_updates"));
			return 0;
		}

		MagicCommandFeedback.sendFrostStageSetFeedback(source, updatedTargets, stage);
		return updatedTargets.size();
	}

	static int clearFrostStage(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		List<ServerPlayerEntity> updatedTargets = new ArrayList<>();
		for (ServerPlayerEntity target : targets) {
			if (MagicAbilityManager.clearFrostStage(target) > 0) {
				updatedTargets.add(target);
			}
		}

		if (updatedTargets.isEmpty()) {
			source.sendError(Text.translatable("command.magic.frost.stage.failure.no_updates"));
			return 0;
		}

		MagicCommandFeedback.sendFrostStageClearFeedback(source, updatedTargets);
		return updatedTargets.size();
	}

	static int advanceFrostStage(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		List<ServerPlayerEntity> updatedTargets = new ArrayList<>();
		for (ServerPlayerEntity target : targets) {
			if (MagicAbilityManager.advanceFrostStageForTesting(target) > 0) {
				updatedTargets.add(target);
			}
		}

		if (updatedTargets.isEmpty()) {
			source.sendError(Text.translatable("command.magic.frost.stage.failure.no_updates"));
			return 0;
		}

		MagicCommandFeedback.sendFrostStageNextFeedback(source, updatedTargets);
		return updatedTargets.size();
	}

	static int setFrostStageProgress(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		int seconds
	) {
		List<ServerPlayerEntity> updatedTargets = new ArrayList<>();
		for (ServerPlayerEntity target : targets) {
			if (MagicAbilityManager.setFrostStageProgressSeconds(target, seconds) > 0) {
				updatedTargets.add(target);
			}
		}

		if (updatedTargets.isEmpty()) {
			source.sendError(Text.translatable("command.magic.frost.stage.failure.no_updates"));
			return 0;
		}

		MagicCommandFeedback.sendFrostStageProgressFeedback(source, updatedTargets, seconds);
		return updatedTargets.size();
	}

	static int setFrostStageThreeProgress(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		int seconds
	) {
		List<ServerPlayerEntity> updatedTargets = new ArrayList<>();
		for (ServerPlayerEntity target : targets) {
			if (MagicAbilityManager.setFrostStageThreeProgressSeconds(target, seconds) > 0) {
				updatedTargets.add(target);
			}
		}

		if (updatedTargets.isEmpty()) {
			source.sendError(Text.translatable("command.magic.frost.stage.failure.no_updates"));
			return 0;
		}

		MagicCommandFeedback.sendFrostStageThreeProgressFeedback(source, updatedTargets, seconds);
		return updatedTargets.size();
	}

	static int setBurningPassionStage(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		int stage
	) {
		List<ServerPlayerEntity> updatedTargets = new ArrayList<>();
		for (ServerPlayerEntity target : targets) {
			if (MagicAbilityManager.setBurningPassionStage(target, stage) > 0) {
				updatedTargets.add(target);
			}
		}

		if (updatedTargets.isEmpty()) {
			source.sendError(Text.translatable("command.magic.burning_passion.ignition.stage.failure.no_updates"));
			return 0;
		}

		MagicCommandFeedback.sendBurningPassionStageSetFeedback(source, updatedTargets, stage);
		return updatedTargets.size();
	}

	static int advanceBurningPassionStage(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		List<ServerPlayerEntity> updatedTargets = new ArrayList<>();
		for (ServerPlayerEntity target : targets) {
			if (MagicAbilityManager.advanceBurningPassionStageForTesting(target) > 0) {
				updatedTargets.add(target);
			}
		}

		if (updatedTargets.isEmpty()) {
			source.sendError(Text.translatable("command.magic.burning_passion.ignition.stage.failure.no_updates"));
			return 0;
		}

		MagicCommandFeedback.sendBurningPassionStageNextFeedback(source, updatedTargets);
		return updatedTargets.size();
	}

	static int setBurningPassionStageProgress(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		int seconds
	) {
		List<ServerPlayerEntity> updatedTargets = new ArrayList<>();
		for (ServerPlayerEntity target : targets) {
			if (MagicAbilityManager.setBurningPassionStageProgressSeconds(target, seconds) > 0) {
				updatedTargets.add(target);
			}
		}

		if (updatedTargets.isEmpty()) {
			source.sendError(Text.translatable("command.magic.burning_passion.ignition.stage.failure.no_updates"));
			return 0;
		}

		MagicCommandFeedback.sendBurningPassionStageProgressFeedback(source, updatedTargets, seconds);
		return updatedTargets.size();
	}

	static int forceBurningPassionOverride(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		List<ServerPlayerEntity> updatedTargets = new ArrayList<>();
		for (ServerPlayerEntity target : targets) {
			if (MagicAbilityManager.forceBurningPassionOverride(target) > 0) {
				updatedTargets.add(target);
			}
		}

		if (updatedTargets.isEmpty()) {
			source.sendError(Text.translatable("command.magic.burning_passion.override.force.failure.no_updates"));
			return 0;
		}

		MagicCommandFeedback.sendBurningPassionOverrideForceFeedback(source, updatedTargets);
		return updatedTargets.size();
	}

	static int reloadConfig(ServerCommandSource source) {
		boolean reloaded = MagicConfig.reload();
		if (!reloaded) {
			source.sendError(Text.translatable("command.magic.config.reload.failure", MagicConfig.path().toString()));
			return 0;
		}

		MagicAbilityManager.reloadConfigValues();
		source.sendFeedback(
			() -> Text.translatable("command.magic.config.reload.success", MagicConfig.path().toString()),
			true
		);
		return Command.SINGLE_SUCCESS;
	}

	static int setAstralCataclysmMobTargeting(ServerCommandSource source, boolean enabled) {
		boolean updated = MagicConfig.setAstralCataclysmAllowMobTargets(enabled);
		if (!updated) {
			source.sendError(Text.translatable("command.magic.config.astral_cataclysm_mob_targeting.failure", MagicConfig.path().toString()));
			return 0;
		}

		MagicAbilityManager.reloadConfigValues();
		source.sendFeedback(
			() -> Text.translatable(
				enabled
					? "command.magic.config.astral_cataclysm_mob_targeting.enabled"
					: "command.magic.config.astral_cataclysm_mob_targeting.disabled"
			),
			true
		);
		return Command.SINGLE_SUCCESS;
	}

	private static List<UUID> playerIds(Collection<ServerPlayerEntity> targets) {
		List<UUID> playerIds = new ArrayList<>(targets.size());
		for (ServerPlayerEntity target : targets) {
			playerIds.add(target.getUuid());
		}
		return playerIds;
	}
}
