package com.lenefun.thecost.command;

import com.lenefun.thecost.sanity.SanityData;
import com.lenefun.thecost.sanity.SanityStage;
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
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(literal("thecost")
						.then(literal("sanity")
								.then(literal("get")
										.executes(context -> report(context.getSource())))
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
														IntegerArgumentType.getInteger(context, "delta"))))))));
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
}
