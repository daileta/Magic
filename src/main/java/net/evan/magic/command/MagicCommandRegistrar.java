package net.evan.magic.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import net.evan.magic.magic.core.MagicSchool;
import net.evan.magic.magic.core.ability.MagicAbility;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

final class MagicCommandRegistrar {
	private static final List<AbilityLiteral> COOLDOWN_ABILITIES = List.of(
		new AbilityLiteral(MagicAbility.BELOW_FREEZING, "below_freezing"),
		new AbilityLiteral(MagicAbility.FROST_ASCENT, "frost_ascent"),
		new AbilityLiteral(MagicAbility.MARTYRS_FLAME, "martyrs_flame"),
		new AbilityLiteral(MagicAbility.IGNITION, "ignition"),
		new AbilityLiteral(MagicAbility.SEARING_DASH, "searing_dash"),
		new AbilityLiteral(MagicAbility.CINDER_MARK, "cinder_mark"),
		new AbilityLiteral(MagicAbility.ENGINE_HEART, "engine_heart"),
		new AbilityLiteral(MagicAbility.OVERRIDE, "override"),
		new AbilityLiteral(MagicAbility.ABSOLUTE_ZERO, "absolute_zero"),
		new AbilityLiteral(MagicAbility.PLANCK_HEAT, "planck_heat"),
		new AbilityLiteral(MagicAbility.FROST_DOMAIN_EXPANSION, "frost_domain_expansion"),
		new AbilityLiteral(MagicAbility.LOVE_DOMAIN_EXPANSION, "love_domain_expansion"),
		new AbilityLiteral(MagicAbility.TILL_DEATH_DO_US_PART, "till_death_do_us_part"),
		new AbilityLiteral(MagicAbility.MANIPULATION, "empty_embrace"),
		new AbilityLiteral(MagicAbility.SPOTLIGHT, "spotlight"),
		new AbilityLiteral(MagicAbility.WITTY_ONE_LINER, "witty_one_liner"),
		new AbilityLiteral(MagicAbility.COMEDIC_REWRITE, "comedic_rewrite"),
		new AbilityLiteral(MagicAbility.COMEDIC_ASSISTANT, "comedic_assistant"),
		new AbilityLiteral(MagicAbility.PLUS_ULTRA, "plus_ultra"),
		new AbilityLiteral(MagicAbility.HERCULES_BURDEN_OF_THE_SKY, "hercules_burden_of_the_sky"),
		new AbilityLiteral(MagicAbility.SAGITTARIUS_ASTRAL_ARROW, "celestial_alignment"),
		new AbilityLiteral(MagicAbility.ORIONS_GAMBIT, "orions_gambit"),
		new AbilityLiteral(MagicAbility.ASTRAL_CATACLYSM, "astral_cataclysm"),
		new AbilityLiteral(MagicAbility.APPRAISERS_MARK, "appraisers_mark"),
		new AbilityLiteral(MagicAbility.TOLLKEEPERS_CLAIM, "tollkeepers_claim"),
		new AbilityLiteral(MagicAbility.KINGS_DUES, "kings_dues"),
		new AbilityLiteral(MagicAbility.BANKRUPTCY, "bankruptcy"),
		new AbilityLiteral(MagicAbility.GREED_DOMAIN_EXPANSION, "greed_domain_expansion")
	);

	private static final List<AbilityLiteral> ACCESS_ABILITIES = List.of(
		new AbilityLiteral(MagicAbility.MARTYRS_FLAME, "martyrs_flame"),
		new AbilityLiteral(MagicAbility.IGNITION, "ignition"),
		new AbilityLiteral(MagicAbility.SEARING_DASH, "searing_dash"),
		new AbilityLiteral(MagicAbility.CINDER_MARK, "cinder_mark"),
		new AbilityLiteral(MagicAbility.ENGINE_HEART, "engine_heart"),
		new AbilityLiteral(MagicAbility.OVERRIDE, "override"),
		new AbilityLiteral(MagicAbility.BELOW_FREEZING, "below_freezing"),
		new AbilityLiteral(MagicAbility.FROST_ASCENT, "frost_ascent"),
		new AbilityLiteral(MagicAbility.ABSOLUTE_ZERO, "absolute_zero"),
		new AbilityLiteral(MagicAbility.PLANCK_HEAT, "planck_heat"),
		new AbilityLiteral(MagicAbility.FROST_DOMAIN_EXPANSION, "frost_domain_expansion"),
		new AbilityLiteral(MagicAbility.LOVE_AT_FIRST_SIGHT, "love_at_first_sight"),
		new AbilityLiteral(MagicAbility.TILL_DEATH_DO_US_PART, "till_death_do_us_part"),
		new AbilityLiteral(MagicAbility.MANIPULATION, "empty_embrace"),
		new AbilityLiteral(MagicAbility.LOVE_DOMAIN_EXPANSION, "love_domain_expansion"),
		new AbilityLiteral(MagicAbility.SPOTLIGHT, "spotlight"),
		new AbilityLiteral(MagicAbility.WITTY_ONE_LINER, "witty_one_liner"),
		new AbilityLiteral(MagicAbility.COMEDIC_REWRITE, "comedic_rewrite"),
		new AbilityLiteral(MagicAbility.COMEDIC_ASSISTANT, "comedic_assistant"),
		new AbilityLiteral(MagicAbility.PLUS_ULTRA, "plus_ultra"),
		new AbilityLiteral(MagicAbility.CASSIOPEIA, "cassiopeia"),
		new AbilityLiteral(MagicAbility.HERCULES_BURDEN_OF_THE_SKY, "hercules_burden_of_the_sky"),
		new AbilityLiteral(MagicAbility.SAGITTARIUS_ASTRAL_ARROW, "celestial_alignment"),
		new AbilityLiteral(MagicAbility.ORIONS_GAMBIT, "orions_gambit"),
		new AbilityLiteral(MagicAbility.ASTRAL_CATACLYSM, "astral_cataclysm"),
		new AbilityLiteral(MagicAbility.APPRAISERS_MARK, "appraisers_mark"),
		new AbilityLiteral(MagicAbility.TOLLKEEPERS_CLAIM, "tollkeepers_claim"),
		new AbilityLiteral(MagicAbility.KINGS_DUES, "kings_dues"),
		new AbilityLiteral(MagicAbility.BANKRUPTCY, "bankruptcy"),
		new AbilityLiteral(MagicAbility.GREED_DOMAIN_EXPANSION, "greed_domain_expansion")
	);

	private static final List<SchoolLiteral> SCHOOL_LITERALS = List.of(
		new SchoolLiteral(MagicSchool.FROST, "frost"),
		new SchoolLiteral(MagicSchool.LOVE, "love"),
		new SchoolLiteral(MagicSchool.BURNING_PASSION, "burning_passion"),
		new SchoolLiteral(MagicSchool.JESTER, "jester"),
		new SchoolLiteral(MagicSchool.CONSTELLATION, "constellation"),
		new SchoolLiteral(MagicSchool.GREED, "greed")
	);

	private MagicCommandRegistrar() {
	}

	static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(rootLiteral())
		);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> rootLiteral() {
		return CommandManager.literal("magic")
			.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
			.then(cooldownLiteral())
			.then(configLiteral())
			.then(removeLiteral())
			.then(testingLiteral())
			.then(frostLiteral())
			.then(burningPassionLiteral())
			.then(greedLiteral())
			.then(schoolLiteral())
			.then(abilityLiteral());
	}

	private static LiteralArgumentBuilder<ServerCommandSource> cooldownLiteral() {
		LiteralArgumentBuilder<ServerCommandSource> reset = CommandManager.literal("reset")
			.then(
				CommandManager.literal("all")
					.executes(context -> MagicCommandActions.resetAllForSelf(context.getSource()))
					.then(
						CommandManager.argument("targets", EntityArgumentType.players())
							.executes(context ->
								MagicCommandActions.resetAll(context.getSource(), EntityArgumentType.getPlayers(context, "targets"))
							)
					)
			);

		for (AbilityLiteral ability : COOLDOWN_ABILITIES) {
			reset.then(cooldownAbilityLiteral(ability));
		}

		return CommandManager.literal("cooldown").then(reset);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> configLiteral() {
		return CommandManager.literal("config")
			.then(
				CommandManager.literal("reload")
					.executes(context -> MagicCommandActions.reloadConfig(context.getSource()))
			)
			.then(
				CommandManager.literal("astral_cataclysm_mob_targeting")
					.then(
						CommandManager.literal("on")
							.executes(context -> MagicCommandActions.setAstralCataclysmMobTargeting(context.getSource(), true))
					)
					.then(
						CommandManager.literal("off")
							.executes(context -> MagicCommandActions.setAstralCataclysmMobTargeting(context.getSource(), false))
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> removeLiteral() {
		return CommandManager.literal("remove")
			.then(
				CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context -> MagicCommandActions.removeMagic(context.getSource(), EntityArgumentType.getPlayers(context, "targets")))
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> testingLiteral() {
		return CommandManager.literal("testing")
			.then(testingModeLiteral("enable", true))
			.then(testingModeLiteral("disable", false));
	}

	private static LiteralArgumentBuilder<ServerCommandSource> frostLiteral() {
		return CommandManager.literal("frost")
			.then(
				CommandManager.literal("stage")
					.then(frostStageSetLiteral())
					.then(frostStageClearLiteral())
					.then(frostStageNextLiteral())
					.then(stageProgressionLiteral("lock", MagicSchool.FROST, true))
					.then(stageProgressionLiteral("unlock", MagicSchool.FROST, false))
					.then(frostProgressLiteral())
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> frostStageSetLiteral() {
		return CommandManager.literal("set")
			.then(
				CommandManager.argument("stage", IntegerArgumentType.integer(1, 3))
					.executes(context ->
						MagicCommandActions.setFrostStageForSelf(
							context.getSource(),
							IntegerArgumentType.getInteger(context, "stage")
						)
					)
					.then(
						CommandManager.argument("targets", EntityArgumentType.players())
							.executes(context ->
								MagicCommandActions.setFrostStage(
									context.getSource(),
									EntityArgumentType.getPlayers(context, "targets"),
									IntegerArgumentType.getInteger(context, "stage")
								)
							)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> frostStageClearLiteral() {
		return CommandManager.literal("clear")
			.executes(context -> MagicCommandActions.clearFrostStageForSelf(context.getSource()))
			.then(
				CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context ->
						MagicCommandActions.clearFrostStage(
							context.getSource(),
							EntityArgumentType.getPlayers(context, "targets")
						)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> frostStageNextLiteral() {
		return CommandManager.literal("next")
			.executes(context -> MagicCommandActions.advanceFrostStageForSelf(context.getSource()))
			.then(
				CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context ->
						MagicCommandActions.advanceFrostStage(
							context.getSource(),
							EntityArgumentType.getPlayers(context, "targets")
						)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> frostProgressLiteral() {
		return CommandManager.literal("progress")
			.then(
				CommandManager.literal("set")
					.then(
						CommandManager.argument("seconds", IntegerArgumentType.integer(0))
							.executes(context ->
								MagicCommandActions.setFrostStageProgressForSelf(
									context.getSource(),
									IntegerArgumentType.getInteger(context, "seconds")
								)
							)
							.then(
								CommandManager.argument("targets", EntityArgumentType.players())
									.executes(context ->
										MagicCommandActions.setFrostStageProgress(
											context.getSource(),
											EntityArgumentType.getPlayers(context, "targets"),
											IntegerArgumentType.getInteger(context, "seconds")
										)
									)
							)
					)
			)
			.then(
				CommandManager.literal("maximum")
					.then(
						CommandManager.literal("set")
							.then(
								CommandManager.argument("seconds", IntegerArgumentType.integer(0))
									.executes(context ->
										MagicCommandActions.setFrostStageThreeProgressForSelf(
											context.getSource(),
											IntegerArgumentType.getInteger(context, "seconds")
										)
									)
									.then(
										CommandManager.argument("targets", EntityArgumentType.players())
											.executes(context ->
												MagicCommandActions.setFrostStageThreeProgress(
													context.getSource(),
													EntityArgumentType.getPlayers(context, "targets"),
													IntegerArgumentType.getInteger(context, "seconds")
												)
											)
									)
							)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> burningPassionLiteral() {
		return CommandManager.literal("burning_passion")
			.then(burningPassionIgnitionLiteral())
			.then(
				CommandManager.literal("override")
					.then(
						CommandManager.literal("force")
							.executes(context -> MagicCommandActions.forceBurningPassionOverrideForSelf(context.getSource()))
							.then(
								CommandManager.argument("targets", EntityArgumentType.players())
									.executes(context ->
										MagicCommandActions.forceBurningPassionOverride(
											context.getSource(),
											EntityArgumentType.getPlayers(context, "targets")
										)
									)
							)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> burningPassionIgnitionLiteral() {
		return CommandManager.literal("ignition")
			.then(
				CommandManager.literal("stage")
					.then(burningPassionStageSetLiteral())
					.then(burningPassionStageNextLiteral())
					.then(stageProgressionLiteral("lock", MagicSchool.BURNING_PASSION, true))
					.then(stageProgressionLiteral("unlock", MagicSchool.BURNING_PASSION, false))
					.then(burningPassionStageProgressLiteral())
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> burningPassionStageSetLiteral() {
		return CommandManager.literal("set")
			.then(
				CommandManager.argument("stage", IntegerArgumentType.integer(1, 3))
					.executes(context ->
						MagicCommandActions.setBurningPassionStageForSelf(
							context.getSource(),
							IntegerArgumentType.getInteger(context, "stage")
						)
					)
					.then(
						CommandManager.argument("targets", EntityArgumentType.players())
							.executes(context ->
								MagicCommandActions.setBurningPassionStage(
									context.getSource(),
									EntityArgumentType.getPlayers(context, "targets"),
									IntegerArgumentType.getInteger(context, "stage")
								)
							)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> burningPassionStageNextLiteral() {
		return CommandManager.literal("next")
			.executes(context -> MagicCommandActions.advanceBurningPassionStageForSelf(context.getSource()))
			.then(
				CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context ->
						MagicCommandActions.advanceBurningPassionStage(
							context.getSource(),
							EntityArgumentType.getPlayers(context, "targets")
						)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> burningPassionStageProgressLiteral() {
		return CommandManager.literal("progress")
			.then(
				CommandManager.literal("set")
					.then(
						CommandManager.argument("seconds", IntegerArgumentType.integer(0))
							.executes(context ->
								MagicCommandActions.setBurningPassionStageProgressForSelf(
									context.getSource(),
									IntegerArgumentType.getInteger(context, "seconds")
								)
							)
							.then(
								CommandManager.argument("targets", EntityArgumentType.players())
									.executes(context ->
										MagicCommandActions.setBurningPassionStageProgress(
											context.getSource(),
											EntityArgumentType.getPlayers(context, "targets"),
											IntegerArgumentType.getInteger(context, "seconds")
										)
									)
							)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> greedLiteral() {
		return CommandManager.literal("greed")
			.then(
				CommandManager.literal("coins")
					.then(
						CommandManager.literal("add")
							.then(
								CommandManager.argument("amount", DoubleArgumentType.doubleArg(0.01))
									.executes(context ->
										MagicCommandActions.addGreedCoinsForSelf(
											context.getSource(),
											DoubleArgumentType.getDouble(context, "amount")
										)
									)
									.then(
										CommandManager.argument("targets", EntityArgumentType.players())
											.executes(context ->
												MagicCommandActions.addGreedCoins(
													context.getSource(),
													EntityArgumentType.getPlayers(context, "targets"),
													DoubleArgumentType.getDouble(context, "amount")
												)
											)
									)
							)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> schoolLiteral() {
		LiteralArgumentBuilder<ServerCommandSource> school = CommandManager.literal("school");
		for (SchoolLiteral entry : SCHOOL_LITERALS) {
			school.then(schoolLiteral(entry));
		}
		return school;
	}

	private static LiteralArgumentBuilder<ServerCommandSource> abilityLiteral() {
		return CommandManager.literal("ability")
			.then(abilityAccessGroupLiteral("lock", true))
			.then(abilityAccessGroupLiteral("unlock", false));
	}

	private static LiteralArgumentBuilder<ServerCommandSource> abilityAccessGroupLiteral(String literal, boolean locked) {
		LiteralArgumentBuilder<ServerCommandSource> access = CommandManager.literal(literal);
		for (AbilityLiteral ability : ACCESS_ABILITIES) {
			access.then(abilityAccessLiteral(ability, locked));
		}
		return access;
	}

	private static LiteralArgumentBuilder<ServerCommandSource> cooldownAbilityLiteral(AbilityLiteral ability) {
		return CommandManager.literal(ability.literal())
			.executes(context -> MagicCommandActions.resetAbilityForSelf(context.getSource(), ability.ability()))
			.then(
				CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context ->
						MagicCommandActions.resetAbility(
							context.getSource(),
							EntityArgumentType.getPlayers(context, "targets"),
							ability.ability()
						)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> abilityAccessLiteral(AbilityLiteral ability, boolean locked) {
		return CommandManager.literal(ability.literal())
			.executes(context -> MagicCommandActions.setAbilityAccessForSelf(context.getSource(), ability.ability(), locked))
			.then(
				CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context ->
						MagicCommandActions.setAbilityAccess(
							context.getSource(),
							EntityArgumentType.getPlayers(context, "targets"),
							ability.ability(),
							locked
						)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> testingModeLiteral(String literal, boolean enabled) {
		return CommandManager.literal(literal)
			.executes(context -> MagicCommandActions.setTestingModeForSelf(context.getSource(), enabled))
			.then(
				CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context ->
						MagicCommandActions.setTestingMode(
							context.getSource(),
							EntityArgumentType.getPlayers(context, "targets"),
							enabled
						)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> schoolLiteral(SchoolLiteral school) {
		return CommandManager.literal(school.literal())
			.executes(context -> MagicCommandActions.setSchoolForSelf(context.getSource(), school.school()))
			.then(
				CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context ->
						MagicCommandActions.setSchool(
							context.getSource(),
							EntityArgumentType.getPlayers(context, "targets"),
							school.school()
						)
					)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> stageProgressionLiteral(String literal, MagicSchool school, boolean locked) {
		return CommandManager.literal(literal)
			.then(
				CommandManager.argument("stage", IntegerArgumentType.integer(2, 3))
					.executes(context ->
						MagicCommandActions.setStageProgressionAccessForSelf(
							context.getSource(),
							school,
							IntegerArgumentType.getInteger(context, "stage"),
							locked
						)
					)
					.then(
						CommandManager.argument("targets", EntityArgumentType.players())
							.executes(context ->
								MagicCommandActions.setStageProgressionAccess(
									context.getSource(),
									EntityArgumentType.getPlayers(context, "targets"),
									school,
									IntegerArgumentType.getInteger(context, "stage"),
									locked
								)
							)
					)
			);
	}

	private record AbilityLiteral(MagicAbility ability, String literal) {
	}

	private record SchoolLiteral(MagicSchool school, String literal) {
	}
}

