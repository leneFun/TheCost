package com.lenefun.thecost.client;

import com.lenefun.thecost.altar.ModScreenHandlers;
import com.lenefun.thecost.client.altar.GraftingAltarScreen;
import com.lenefun.thecost.client.hud.SanityHudRenderer;
import com.lenefun.thecost.client.perception.FracturedOrePerceptionRenderer;
import com.lenefun.thecost.client.sanity.ClientSanityNetworking;
import com.lenefun.thecost.client.visual.AscendedVisualRenderer;
import com.lenefun.thecost.client.entity.WitnessRenderer;
import com.lenefun.thecost.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class TheCostClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HandledScreens.register(ModScreenHandlers.GRAFTING_ALTAR, GraftingAltarScreen::new);
		EntityRendererRegistry.register(ModEntities.WITNESS, WitnessRenderer::new);
		ClientSanityNetworking.registerReceiver();
		FracturedOrePerceptionRenderer.register();
		AscendedVisualRenderer.register();
		SanityHudRenderer.register();
	}
}
