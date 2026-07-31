package com.lenefun.thecost.sanity;

import net.minecraft.server.network.ServerPlayerEntity;

public final class SanityData {
	public static final int MIN_VALUE = 0;
	public static final int MAX_VALUE = 100;
	public static final int DEFAULT_VALUE = MAX_VALUE;
	public static final String NBT_KEY = "thecost_sanity";

	private SanityData() {
	}

	public static int clamp(int sanity) {
		return clamp((long) sanity);
	}

	public static int clamp(long sanity) {
		return (int) Math.max(MIN_VALUE, Math.min(MAX_VALUE, sanity));
	}

	public static int get(ServerPlayerEntity player) {
		return holder(player).thecost$getSanity();
	}

	public static SanityStage getStage(ServerPlayerEntity player) {
		return holder(player).thecost$getSanityStage();
	}

	public static void set(ServerPlayerEntity player, int sanity) {
		holder(player).thecost$setSanity(sanity);
	}

	public static void add(ServerPlayerEntity player, int delta) {
		set(player, clamp((long) get(player) + delta));
	}

	public static void copy(ServerPlayerEntity source, ServerPlayerEntity destination) {
		holder(destination).thecost$setSanitySilently(get(source));
	}

	private static SanityHolder holder(ServerPlayerEntity player) {
		return (SanityHolder) player;
	}
}
