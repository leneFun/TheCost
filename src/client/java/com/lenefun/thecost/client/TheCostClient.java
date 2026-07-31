package com.lenefun.thecost.client;

import com.lenefun.thecost.client.hud.SanityHudRenderer;
import com.lenefun.thecost.client.perception.FracturedOrePerceptionRenderer;
import com.lenefun.thecost.client.sanity.ClientSanityNetworking;
import com.lenefun.thecost.client.visual.AscendedVisualRenderer;
import net.fabricmc.api.ClientModInitializer;

public class TheCostClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientSanityNetworking.registerReceiver();
		FracturedOrePerceptionRenderer.register();
		AscendedVisualRenderer.register();
		SanityHudRenderer.register();
	}
}
