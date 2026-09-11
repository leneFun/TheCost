package com.lenefun.thecost.command;

import com.lenefun.thecost.entity.witness.ManagedWitnessManager;
import com.lenefun.thecost.entity.witness.WitnessDebugSnapshot;
import com.lenefun.thecost.sanity.SanityData;
import com.lenefun.thecost.sanity.SanityStage;
import com.lenefun.thecost.sanity.environment.SanityEnvironmentManager;
import com.lenefun.thecost.sanity.environment.SanityEnvironmentSnapshot;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class SanityCommand {
	private static final int MODIFY_PERMISSION_LEVEL = 2;

	private SanityCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			var root = literal("thecost")
					.then(literal("sanity")
								.then(literal("get")
										.executes(context -> report(context.getSource())))
								.then(literal("environment")
										.requires(source -> source.hasPermissionLevel(MODIFY_PERMISSION_LEVEL))
										.executes(context -> reportEnvironment(context.getSource())))
								.then(literal("witness")
										.requires(source -> source.hasPermissionLevel(MODIFY_PERMISSION_LEVEL))
										.executes(context -> reportWitness(context.getSource())))
								.then(literal("set")
										.requires(source -> source.hasPermissionLevel(MODIFY_PERMISSION_LEVEL))
										.then(argument("value", IntegerArgumentType.integer())
												.executes(context -> set(
														context.getSource(),
														IntegerArgumentType.getInteger(context, "value")))))
								.then(literal("add")
										.requires(source -> source.hasPermissionLevel(MODIFY_PERMISSION_LEVEL))
										.then(argument("delta", IntegerArgumentType.integer())
												.executes(context -> add(
														context.getSource(),
												IntegerArgumentType.getInteger(context, "delta"))))));
			root.then(LimbCommand.create());
			root.then(SeasonCommand.create());
			root.then(TemperatureCommand.create());
			root.then(DeerclopsCommand.create());
			dispatcher.register(root);
		});
	}

	private static int report(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayerOrThrow();
		int sanity = SanityData.get(player);
		SanityStage stage = SanityData.getStage(player);
		source.sendFeedback(() -> Text.literal("Sanity: " + sanity + " (" + stage.getDisplayName() + ")"), false);
		return sanity;
	}

	private static int set(ServerCommandSource source, int sanity) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		SanityData.set(source.getPlayerOrThrow(), sanity);
		return report(source);
	}

	private static int add(ServerCommandSource source, int delta) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		SanityData.add(source.getPlayerOrThrow(), delta);
		return report(source);
	}

	private static int reportEnvironment(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayerOrThrow();
		SanityEnvironmentSnapshot snapshot = SanityEnvironmentManager.inspect(player);
		int remainingTicks = SanityEnvironmentManager.getTicksUntilNextChange(player, snapshot);
		String nextChange = remainingTicks < 0
				? "none"
				: String.format(java.util.Locale.ROOT, "%.1fs", remainingTicks / 20.0D);
		source.sendFeedback(() -> Text.literal(
				"Sanity environment: light=" + snapshot.lightLevel()
						+ ", skyVisible=" + snapshot.skyVisible()
						+ ", darkPhase=" + snapshot.darkPhase()
						+ ", state=" + snapshot.state().getDisplayName()
						+ ", rate=" + snapshot.state().getSanityPerMinute() + "/min"
						+ ", eligible=" + SanityEnvironmentManager.isEligible(player)
						+ ", nextChange=" + nextChange), false);
		return snapshot.lightLevel();
	}

	private static int reportWitness(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayerOrThrow();
		WitnessDebugSnapshot snapshot = ManagedWitnessManager.inspect(player);
		String entity = snapshot.entityUuid() == null ? "none" : snapshot.entityUuid().toString();
		String distance = snapshot.distance() < 0.0D
				? "none"
				: String.format(java.util.Locale.ROOT, "%.1f", snapshot.distance());
		String cooldown = snapshot.cooldownTicks() < 0L
				? "inactive"
				: String.format(java.util.Locale.ROOT, "%.1fs", snapshot.cooldownTicks() / 20.0D);
		source.sendFeedback(() -> Text.literal(
				"Managed Witness: desired=" + snapshot.desiredMode()
						+ ", active=" + snapshot.activeMode()
						+ ", entity=" + entity
						+ ", distance=" + distance
						+ ", cooldown=" + cooldown
						+ ", eligible=" + snapshot.eligible()), false);
		return snapshot.activeMode().getTrackedId();
	}
}
