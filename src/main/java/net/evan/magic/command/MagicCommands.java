package net.evan.magic.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.evan.magic.config.MagicConfig;
import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.ability.MagicAbility;
import net.evan.magic.magic.ability.MagicAbilityManager;
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
									.then(cooldownAbilityLiteral(MagicAbility.ABSOLUTE_ZERO, "absolute_zero"))
									.then(cooldownAbilityLiteral(MagicAbility.PLANCK_HEAT, "planck_heat"))
									.then(cooldownAbilityLiteral(MagicAbility.FROST_DOMAIN_EXPANSION, "frost_domain_expansion"))
									.then(cooldownAbilityLiteral(MagicAbility.LOVE_DOMAIN_EXPANSION, "love_domain_expansion"))
									.then(cooldownAbilityLiteral(MagicAbility.TILL_DEATH_DO_US_PART, "till_death_do_us_part"))
									.then(cooldownAbilityLiteral(MagicAbility.MANIPULATION, "empty_embrace"))
							)
					)
					.then(
						CommandManager.literal("config")
							.then(
								CommandManager.literal("reload")
									.executes(context -> reloadConfig(context.getSource()))
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
						CommandManager.literal("ability")
							.then(
								CommandManager.literal("lock")
									.then(abilityAccessLiteral(MagicAbility.BELOW_FREEZING, "below_freezing", true))
									.then(abilityAccessLiteral(MagicAbility.ABSOLUTE_ZERO, "absolute_zero", true))
									.then(abilityAccessLiteral(MagicAbility.PLANCK_HEAT, "planck_heat", true))
									.then(abilityAccessLiteral(MagicAbility.FROST_DOMAIN_EXPANSION, "frost_domain_expansion", true))
									.then(abilityAccessLiteral(MagicAbility.LOVE_AT_FIRST_SIGHT, "love_at_first_sight", true))
									.then(abilityAccessLiteral(MagicAbility.TILL_DEATH_DO_US_PART, "till_death_do_us_part", true))
									.then(abilityAccessLiteral(MagicAbility.MANIPULATION, "empty_embrace", true))
									.then(abilityAccessLiteral(MagicAbility.LOVE_DOMAIN_EXPANSION, "love_domain_expansion", true))
							)
							.then(
								CommandManager.literal("unlock")
									.then(abilityAccessLiteral(MagicAbility.BELOW_FREEZING, "below_freezing", false))
									.then(abilityAccessLiteral(MagicAbility.ABSOLUTE_ZERO, "absolute_zero", false))
									.then(abilityAccessLiteral(MagicAbility.PLANCK_HEAT, "planck_heat", false))
									.then(abilityAccessLiteral(MagicAbility.FROST_DOMAIN_EXPANSION, "frost_domain_expansion", false))
									.then(abilityAccessLiteral(MagicAbility.LOVE_AT_FIRST_SIGHT, "love_at_first_sight", false))
									.then(abilityAccessLiteral(MagicAbility.TILL_DEATH_DO_US_PART, "till_death_do_us_part", false))
									.then(abilityAccessLiteral(MagicAbility.MANIPULATION, "empty_embrace", false))
									.then(abilityAccessLiteral(MagicAbility.LOVE_DOMAIN_EXPANSION, "love_domain_expansion", false))
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

	private static int resetAllForSelf(ServerCommandSource source) throws CommandSyntaxException {
		return resetAll(source, List.of(source.getPlayerOrThrow()));
	}

	private static int resetAbilityForSelf(ServerCommandSource source, MagicAbility ability) throws CommandSyntaxException {
		return resetAbility(source, List.of(source.getPlayerOrThrow()), ability);
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
			target.getInventory().clear();
			target.getEnderChestInventory().clear();
			target.clearStatusEffects();
			target.extinguish();
			target.setExperienceLevel(0);
			target.setExperiencePoints(0);
			MagicAbilityManager.clearAllRuntimeState(target);
			MagicPlayerData.clear(target);
			target.playerScreenHandler.sendContentUpdates();
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

		sendAbilityAccessFeedback(source, targets, ability, locked);
		return targets.size();
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
}
