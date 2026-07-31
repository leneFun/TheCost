package com.lenefun.thecost.client.sanity;

import com.lenefun.thecost.sanity.SanityNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientSanityNetworking {
	private ClientSanityNetworking() {
	}

	public static void registerReceiver() {
		ClientPlayConnectionEvents.INIT.register((handler, client) -> ClientSanityState.reset());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientSanityState.reset());

		ClientPlayNetworking.registerGlobalReceiver(SanityNetworking.SYNC_CHANNEL,
				(client, handler, buffer, responseSender) -> {
					int sanity = buffer.readInt();
					client.execute(() -> ClientSanityState.updateFromServer(sanity));
				});
	}
}
