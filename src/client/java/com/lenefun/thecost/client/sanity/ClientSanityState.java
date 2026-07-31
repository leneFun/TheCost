package com.lenefun.thecost.client.sanity;

import com.lenefun.thecost.sanity.SanityData;
import com.lenefun.thecost.sanity.SanityStage;

public final class ClientSanityState {
	private static int sanity = SanityData.DEFAULT_VALUE;
	private static SanityStage stage = SanityStage.fromValue(sanity);
	private static boolean synced;

	private ClientSanityState() {
	}

	public static int getSanity() {
		return sanity;
	}

	public static SanityStage getStage() {
		return stage;
	}

	public static boolean isSynced() {
		return synced;
	}

	static void updateFromServer(int serverSanity) {
		sanity = SanityData.clamp(serverSanity);
		stage = SanityStage.fromValue(sanity);
		synced = true;
	}

	static void reset() {
		sanity = SanityData.DEFAULT_VALUE;
		stage = SanityStage.fromValue(sanity);
		synced = false;
	}
}
