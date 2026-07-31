package com.lenefun.thecost.sanity;

import com.lenefun.thecost.TheCost;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class SanityNetworking {
	public static final Identifier SYNC_CHANNEL = TheCost.id("sanity_sync");

	private SanityNetworking() {
	}

	public static void registerServerEvents() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));

		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
				SanityData.copy(oldPlayer, newPlayer));

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
				sync(newPlayer));

		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
				sync(player));
	}

	public static void sync(ServerPlayerEntity player) {
		if (player.networkHandler == null || !ServerPlayNetworking.canSend(player, SYNC_CHANNEL)) {
			return;
		}

		PacketByteBuf buffer = PacketByteBufs.create();
		buffer.writeInt(SanityData.get(player));
		ServerPlayNetworking.send(player, SYNC_CHANNEL, buffer);
	}
}
