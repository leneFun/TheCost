package com.lenefun.thecost.client.hud;

import com.lenefun.thecost.TheCost;
import com.lenefun.thecost.client.sanity.ClientSanityState;
import com.lenefun.thecost.sanity.SanityStage;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class SanityHudRenderer {
	private static final int MARGIN = 8;
	private static final int ICON_SIZE = 16;
	private static final int TEXT_OFFSET_X = 20;
	private static final int TEXT_OFFSET_Y = 4;

	private static final Identifier FULL_MOON_TEXTURE = TheCost.id("textures/gui/sanity/full_moon.png");
	private static final Identifier HALF_MOON_TEXTURE = TheCost.id("textures/gui/sanity/half_moon.png");
	private static final Identifier ECLIPSE_TEXTURE = TheCost.id("textures/gui/sanity/eclipse.png");

	private SanityHudRenderer() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register(SanityHudRenderer::render);
	}

	private static void render(DrawContext drawContext, float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null
				|| client.world == null
				|| client.options.hudHidden
				|| !ClientSanityState.isSynced()) {
			return;
		}

		SanityStage stage = ClientSanityState.getStage();
		Identifier texture = getTexture(stage);
		int textColor = getTextColor(stage);

		drawContext.drawTexture(
				texture,
				MARGIN,
				MARGIN,
				0.0F,
				0.0F,
				ICON_SIZE,
				ICON_SIZE,
				ICON_SIZE,
				ICON_SIZE);
		drawContext.drawTextWithShadow(
				client.textRenderer,
				Integer.toString(ClientSanityState.getSanity()),
				MARGIN + TEXT_OFFSET_X,
				MARGIN + TEXT_OFFSET_Y,
				textColor);
	}

	private static Identifier getTexture(SanityStage stage) {
		return switch (stage) {
			case LUCID -> FULL_MOON_TEXTURE;
			case FRACTURED -> HALF_MOON_TEXTURE;
			case ASCENDED -> ECLIPSE_TEXTURE;
		};
	}

	private static int getTextColor(SanityStage stage) {
		return switch (stage) {
			case LUCID -> 0xfff2f4e8;
			case FRACTURED -> 0xffa9c7e8;
			case ASCENDED -> 0xffc7a85b;
		};
	}
}
