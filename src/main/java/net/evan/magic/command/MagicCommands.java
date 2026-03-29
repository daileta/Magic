package net.evan.magic.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.evan.magic.config.MagicConfig;
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.MagicSchool;
import net.evan.magic.magic.ability.MagicAbility;
import net.evan.magic.magic.ability.MagicAbilityManager;
import net.evan.magic.magic.ability.GreedRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class MagicCommands {
	private MagicCommands() {
	}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(
				CommandManager.literal("magic")
					.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
					.then(
						CommandManager.literal("cooldown")
							.then(
								CommandManager.literal("reset")
									.then(
										CommandManager.literal("all")
											.executes(context -> resetAllForSelf(context.getSource()))
											.then(
												CommandManager.argument("targets", EntityArgumentType.players())
													.executes(context ->
														resetAll(context.getSource(), EntityArgumentType.getPlayers(context, "targets"))
													)
											)
									)
									.then(cooldownAbilityLiteral(MagicAbility.BELOW_FREEZING, "below_freezing"))
									.then(cooldownAbilityLiteral(MagicAbility.FROST_ASCENT, "frost_ascent"))
									.then(cooldownAbilityLiteral(MagicAbility.MARTYRS_FLAME, "martyrs_flame"))
									.then(cooldownAbilityLiteral(MagicAbility.ABSOLUTE_ZERO, "absolute_zero"))
									.then(cooldownAbilityLiteral(MagicAbility.PLANCK_HEAT, "planck_heat"))
									.then(cooldownAbilityLiteral(MagicAbility.FROST_DOMAIN_EXPANSION, "frost_domain_expansion"))
									.then(cooldownAbilityLiteral(MagicAbility.LOVE_DOMAIN_EXPANSION, "love_domain_expansion"))
									.then(cooldownAbilityLiteral(MagicAbility.TILL_DEATH_DO_US_PART, "till_death_do_us_part"))
									.then(cooldownAbilityLiteral(MagicAbility.MANIPULATION, "empty_embrace"))
									.then(cooldownAbilityLiteral(MagicAbility.SPOTLIGHT, "spotlight"))
									.then(cooldownAbilityLiteral(MagicAbility.WITTY_ONE_LINER, "witty_one_liner"))
									.then(cooldownAbilityLiteral(MagicAbility.COMEDIC_REWRITE, "comedic_rewrite"))
									.then(cooldownAbilityLiteral(MagicAbility.COMEDIC_ASSISTANT, "comedic_assistant"))
									.then(cooldownAbilityLiteral(MagicAbility.PLUS_ULTRA, "plus_ultra"))
									.then(cooldownAbilityLiteral(MagicAbility.HERCULES_BURDEN_OF_THE_SKY, "hercules_burden_of_the_sky"))
									.then(cooldownAbilityLiteral(MagicAbility.SAGITTARIUS_ASTRAL_ARROW, "sagittarius_astral_arrow"))
									.then(cooldownAbilityLiteral(MagicAbility.ORIONS_GAMBIT, "orions_gambit"))
									.then(cooldownAbilityLiteral(MagicAbility.ASTRAL_CATACLYSM, "astral_cataclysm"))
									.then(cooldownAbilityLiteral(MagicAbility.APPRAISERS_MARK, "appraisers_mark"))
									.then(cooldownAbilityLiteral(MagicAbility.TOLLKEEPERS_CLAIM, "tollkeepers_claim"))
									.then(cooldownAbilityLiteral(MagicAbility.KINGS_DUES, "kings_dues"))
									.then(cooldownAbilityLiteral(MagicAbility.BANKRUPTCY, "bankruptcy"))
							)
					)
					.then(
						CommandManager.literal("config")
							.then(
								CommandManager.literal("reload")
									.executes(context -> reloadConfig(context.getSource()))
							)
							.then(
								CommandManager.literal("astral_cataclysm_mob_targeting")
									.then(
										CommandManager.literal("on")
											.executes(context -> setAstralCataclysmMobTargeting(context.getSource(), true))
									)
									.then(
										CommandManager.literal("off")
											.executes(context -> setAstralCataclysmMobTargeting(context.getSource(), false))
									)
							)
					)
					.then(
						CommandManager.literal("remove")
							.then(
								CommandManager.argument("targets", EntityArgumentType.players())
									.executes(context -> removeMagic(context.getSource(), EntityArgumentType.getPlayers(context, "targets")))
							)
					)
					.then(
						CommandManager.literal("testing")
							.then(testingModeLiteral("enable", true))
							.then(testingModeLiteral("disable", false))
					)
					.then(
						CommandManager.literal("greed")
							.then(
								CommandManager.literal("coins")
									.then(
										CommandManager.literal("add")
											.then(
												CommandManager.argument("amount", DoubleArgumentType.doubleArg(0.01))
													.executes(context ->
														addGreedCoinsForSelf(
															context.getSource(),
															DoubleArgumentType.getDouble(context, "amount")
														)
													)
													.then(
														CommandManager.argument("targets", EntityArgumentType.players())
															.executes(context ->
																addGreedCoins(
																	context.getSource(),
																	EntityArgumentType.getPlayers(context, "targets"),
																	DoubleArgumentType.getDouble(context, "amount")
																)
															)
													)
											)
									)
							)
					)
					.then(
						CommandManager.literal("school")
							.then(schoolLiteral(MagicSchool.FROST, "frost"))
							.then(schoolLiteral(MagicSchool.LOVE, "love"))
							.then(schoolLiteral(MagicSchool.BURNING_PASSION, "burning_passion"))
							.then(schoolLiteral(MagicSchool.JESTER, "jester"))
							.then(schoolLiteral(MagicSchool.CONSTELLATION, "constellation"))
							.then(schoolLiteral(MagicSchool.GREED, "greed"))
					)
					.then(
						CommandManager.literal("ability")
							.then(
								CommandManager.literal("lock")
									.then(abilityAccessLiteral(MagicAbility.MARTYRS_FLAME, "martyrs_flame", true))
									.then(abilityAccessLiteral(MagicAbility.BELOW_FREEZING, "below_freezing", true))
									.then(abilityAccessLiteral(MagicAbility.FROST_ASCENT, "frost_ascent", true))
									.then(abilityAccessLiteral(MagicAbility.ABSOLUTE_ZERO, "absolute_zero", true))
									.then(abilityAccessLiteral(MagicAbility.PLANCK_HEAT, "planck_heat", true))
									.then(abilityAccessLiteral(MagicAbility.FROST_DOMAIN_EXPANSION, "frost_domain_expansion", true))
									.then(abilityAccessLiteral(MagicAbility.LOVE_AT_FIRST_SIGHT, "love_at_first_sight", true))
									.then(abilityAccessLiteral(MagicAbility.TILL_DEATH_DO_US_PART, "till_death_do_us_part", true))
									.then(abilityAccessLiteral(MagicAbility.MANIPULATION, "empty_embrace", true))
									.then(abilityAccessLiteral(MagicAbility.LOVE_DOMAIN_EXPANSION, "love_domain_expansion", true))
									.then(abilityAccessLiteral(MagicAbility.SPOTLIGHT, "spotlight", true))
									.then(abilityAccessLiteral(MagicAbility.WITTY_ONE_LINER, "witty_one_liner", true))
									.then(abilityAccessLiteral(MagicAbility.COMEDIC_REWRITE, "comedic_rewrite", true))
									.then(abilityAccessLiteral(MagicAbility.COMEDIC_ASSISTANT, "comedic_assistant", true))
									.then(abilityAccessLiteral(MagicAbility.PLUS_ULTRA, "plus_ultra", true))
									.then(abilityAccessLiteral(MagicAbility.CASSIOPEIA, "cassiopeia", true))
									.then(abilityAccessLiteral(MagicAbility.HERCULES_BURDEN_OF_THE_SKY, "hercules_burden_of_the_sky", true))
									.then(abilityAccessLiteral(MagicAbility.SAGITTARIUS_ASTRAL_ARROW, "sagittarius_astral_arrow", true))
									.then(abilityAccessLiteral(MagicAbility.ORIONS_GAMBIT, "orions_gambit", true))
									.then(abilityAccessLiteral(MagicAbility.ASTRAL_CATACLYSM, "astral_cataclysm", true))
									.then(abilityAccessLiteral(MagicAbility.APPRAISERS_MARK, "appraisers_mark", true))
									.then(abilityAccessLiteral(MagicAbility.TOLLKEEPERS_CLAIM, "tollkeepers_claim", true))
									.then(abilityAccessLiteral(MagicAbility.KINGS_DUES, "kings_dues", true))
									.then(abilityAccessLiteral(MagicAbility.BANKRUPTCY, "bankruptcy", true))
							)
							.then(
								CommandManager.literal("unlock")
									.then(abilityAccessLiteral(MagicAbility.MARTYRS_FLAME, "martyrs_flame", false))
									.then(abilityAccessLiteral(MagicAbility.BELOW_FREEZING, "below_freezing", false))
									.then(abilityAccessLiteral(MagicAbility.FROST_ASCENT, "frost_ascent", false))
									.then(abilityAccessLiteral(MagicAbility.ABSOLUTE_ZERO, "absolute_zero", false))
									.then(abilityAccessLiteral(MagicAbility.PLANCK_HEAT, "planck_heat", false))
									.then(abilityAccessLiteral(MagicAbility.FROST_DOMAIN_EXPANSION, "frost_domain_expansion", false))
									.then(abilityAccessLiteral(MagicAbility.LOVE_AT_FIRST_SIGHT, "love_at_first_sight", false))
									.then(abilityAccessLiteral(MagicAbility.TILL_DEATH_DO_US_PART, "till_death_do_us_part", false))
									.then(abilityAccessLiteral(MagicAbility.MANIPULATION, "empty_embrace", false))
									.then(abilityAccessLiteral(MagicAbility.LOVE_DOMAIN_EXPANSION, "love_domain_expansion", false))
									.then(abilityAccessLiteral(MagicAbility.SPOTLIGHT, "spotlight", false))
									.then(abilityAccessLiteral(MagicAbility.WITTY_ONE_LINER, "witty_one_liner", false))
									.then(abilityAccessLiteral(MagicAbility.COMEDIC_REWRITE, "comedic_rewrite", false))
									.then(abilityAccessLiteral(MagicAbility.COMEDIC_ASSISTANT, "comedic_assistant", false))
									.then(abilityAccessLiteral(MagicAbility.PLUS_ULTRA, "plus_ultra", false))
									.then(abilityAccessLiteral(MagicAbility.CASSIOPEIA, "cassiopeia", false))
									.then(abilityAccessLiteral(MagicAbility.HERCULES_BURDEN_OF_THE_SKY, "hercules_burden_of_the_sky", false))
									.then(abilityAccessLiteral(MagicAbility.SAGITTARIUS_ASTRAL_ARROW, "sagittarius_astral_arrow", false))
									.then(abilityAccessLiteral(MagicAbility.ORIONS_GAMBIT, "orions_gambit", false))
									.then(abilityAccessLiteral(MagicAbility.ASTRAL_CATACLYSM, "astral_cataclysm", false))
									.then(abilityAccessLiteral(MagicAbility.APPRAISERS_MARK, "appraisers_mark", false))
									.then(abilityAccessLiteral(MagicAbility.TOLLKEEPERS_CLAIM, "tollkeepers_claim", false))
									.then(abilityAccessLiteral(MagicAbility.KINGS_DUES, "kings_dues", false))
									.then(abilityAccessLiteral(MagicAbility.BANKRUPTCY, "bankruptcy", false))
							)
					)
			)
		);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> cooldownAbilityLiteral(MagicAbility ability, String literal) {
		return CommandManager.literal(literal)
			.executes(context -> resetAbilityForSelf(context.getSource(), ability))
			.then(
				CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context -> resetAbility(context.getSource(), EntityArgumentType.getPlayers(context, "targets"), ability))
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> abilityAccessLiteral(
		MagicAbility ability,
		String literal,
		boolean locked
	) {
		return CommandManager.literal(literal)
			.executes(context -> setAbilityAccessForSelf(context.getSource(), ability, locked))
			.then(
				CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context ->
						setAbilityAccess(
							context.getSource(),
							EntityArgumentType.getPlayers(context, "targets"),
							ability,
							locked
						)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> testingModeLiteral(String literal, boolean enabled) {
		return CommandManager.literal(literal)
			.executes(context -> setTestingModeForSelf(context.getSource(), enabled))
			.then(
				CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context ->
						setTestingMode(
							context.getSource(),
							EntityArgumentType.getPlayers(context, "targets"),
							enabled
						)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> schoolLiteral(MagicSchool school, String literal) {
		return CommandManager.literal(literal)
			.executes(context -> setSchoolForSelf(context.getSource(), school))
			.then(
				CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context ->
						setSchool(
							context.getSource(),
							EntityArgumentType.getPlayers(context, "targets"),
							school
						)
					)
			);
	}

	private static int resetAllForSelf(ServerCommandSource source) throws CommandSyntaxException {
		return resetAll(source, List.of(source.getPlayerOrThrow()));
	}

	private static int resetAbilityForSelf(ServerCommandSource source, MagicAbility ability) throws CommandSyntaxException {
		return resetAbility(source, List.of(source.getPlayerOrThrow()), ability);
	}

	private static int setAbilityAccessForSelf(
		ServerCommandSource source,
		MagicAbility ability,
		boolean locked
	) throws CommandSyntaxException {
		return setAbilityAccess(source, List.of(source.getPlayerOrThrow()), ability, locked);
	}

	private static int setTestingModeForSelf(ServerCommandSource source, boolean enabled) throws CommandSyntaxException {
		return setTestingMode(source, List.of(source.getPlayerOrThrow()), enabled);
	}

	private static int setSchoolForSelf(ServerCommandSource source, MagicSchool school) throws CommandSyntaxException {
		return setSchool(source, List.of(source.getPlayerOrThrow()), school);
	}

	private static int addGreedCoinsForSelf(ServerCommandSource source, double amount) throws CommandSyntaxException {
		return addGreedCoins(source, List.of(source.getPlayerOrThrow()), amount);
	}

	private static int resetAll(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		int resetCount = 0;
		for (ServerPlayerEntity target : targets) {
			resetCount += MagicAbilityManager.resetAllCooldowns(target);
		}

		sendAllResetFeedback(source, targets);
		return resetCount == 0 ? Command.SINGLE_SUCCESS : resetCount;
	}

	private static int resetAbility(ServerCommandSource source, Collection<ServerPlayerEntity> targets, MagicAbility ability) {
		int resetCount = 0;
		for (ServerPlayerEntity target : targets) {
			resetCount += MagicAbilityManager.resetCooldown(target, ability);
		}

		sendAbilityResetFeedback(source, targets, ability);
		return resetCount == 0 ? Command.SINGLE_SUCCESS : resetCount;
	}

	private static int removeMagic(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
		List<java.util.UUID> playerIds = new ArrayList<>(targets.size());

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

		sendRemoveMagicFeedback(source, targets);
		return targets.isEmpty() ? 0 : targets.size();
	}

	private static int setAbilityAccess(
		ServerCommandSource source,
		Collection<ServerPlayerEntity> targets,
		MagicAbility ability,
		boolean locked
	) {
		List<java.util.UUID> playerIds = new ArrayList<>(targets.size());
		for (ServerPlayerEntity target : targets) {
			playerIds.add(target.getUuid());
		}

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

		sendAbilityAccessFeedback(source, targets, ability, locked);
		return targets.size();
	}

	private static int setTestingMode(
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

		sendTestingModeFeedback(source, targets, enabled);
		return changedCount == 0 ? Command.SINGLE_SUCCESS : changedCount;
	}

	private static int setSchool(
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

		sendSchoolSetFeedback(source, targets, school);
		return changedCount == 0 ? Command.SINGLE_SUCCESS : changedCount;
	}

	private static int addGreedCoins(ServerCommandSource source, Collection<ServerPlayerEntity> targets, double amount) {
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

		sendGreedCoinAddFeedback(source, updatedTargets, amount);
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

	private static void sendAllResetFeedback(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
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

	private static void sendAbilityResetFeedback(ServerCommandSource source, Collection<ServerPlayerEntity> targets, MagicAbility ability) {
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

	private static void sendRemoveMagicFeedback(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
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

	private static void sendAbilityAccessFeedback(
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

	private static void sendTestingModeFeedback(
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

	private static void sendSchoolSetFeedback(
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

	private static void sendGreedCoinAddFeedback(
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

	private static int reloadConfig(ServerCommandSource source) {
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

	private static int setAstralCataclysmMobTargeting(ServerCommandSource source, boolean enabled) {
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
}
