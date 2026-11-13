package com.example.custommenu;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class CustomMenuClient implements ClientModInitializer {
	public static final String MOD_ID = "custommenu";

	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	private static KeyMapping openKey;

	@Override
	public void onInitializeClient() {
		Config.load();

		openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.custommenu.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.consumeClick()) {
				if (client.screen == null) client.setScreen(new MenuScreen(null));
			}
		});

		// Napisy na HUD podczas gry
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "lyrics"), (graphics, tickCounter) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.screen != null) return;
			if (Config.INSTANCE.topBar) Assets.drawTopBar(graphics, mc.font, graphics.guiWidth());
			boolean center = Config.INSTANCE.lyricsCenter;
			Assets.drawLyrics(graphics, mc.font, graphics.guiWidth(),
					center ? graphics.guiHeight() / 2 + 30 : graphics.guiHeight() - 70, !center);
		});

		// Napisy na tle pozostalych ekranow (menu glowne, pauza itd.)
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) ->
				ScreenEvents.afterRender(screen).register((scr, graphics, mouseX, mouseY, tickDelta) -> {
					if (scr instanceof MenuScreen) return;
					Assets.drawLyrics(graphics, client.font, scr.width, scr.height - 40, true);
				}));
	}
}
