package com.example.custommenu;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Menu moda otwierane klawiszem U. */
public class MenuScreen extends Screen {
	private final Screen parent;
	private Button bgBtn, fontBtn, musicBtn, playBtn, volBtn, lyricsBtn;

	public MenuScreen(Screen parent) {
		super(Component.literal("Fajne Niebo z Napisami"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int w = 150, h = 20, gap = 24;
		int left = width / 2 - 155, right = width / 2 + 5;
		int top = height / 2 - 40;

		bgBtn = addRenderableWidget(Button.builder(bgLabel(), b -> {
			List<Assets.Bg> all = Assets.backgrounds();
			int idx = 0;
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).name().equals(Config.INSTANCE.background)) idx = i;
			}
			Config.INSTANCE.background = all.get((idx + 1) % all.size()).name();
			b.setMessage(bgLabel());
			Config.save();
		}).bounds(left, top, w, h).build());

		fontBtn = addRenderableWidget(Button.builder(fontLabel(), b -> {
			int idx = 0;
			for (int i = 0; i < Assets.FONT_IDS.length; i++) {
				if (Assets.FONT_IDS[i].equals(Config.INSTANCE.font)) idx = i;
			}
			Config.INSTANCE.font = Assets.FONT_IDS[(idx + 1) % Assets.FONT_IDS.length];
			b.setMessage(fontLabel());
			Config.save();
		}).bounds(left, top + gap, w, h).build());

		lyricsBtn = addRenderableWidget(Button.builder(lyricsLabel(), b -> {
			Config.INSTANCE.lyrics = !Config.INSTANCE.lyrics;
			b.setMessage(lyricsLabel());
			Config.save();
		}).bounds(left, top + gap * 2, w, h).build());

		addRenderableWidget(Button.builder(Component.literal("Playlista YT"),
				b -> Util.getPlatform().openUri(Config.INSTANCE.playlistUrl))
				.bounds(left, top + gap * 3, w, h).build());

		musicBtn = addRenderableWidget(Button.builder(musicLabel(), b -> {
			List<String> opts = new ArrayList<>();
			opts.add("");
			for (Path p : MusicPlayer.tracks()) opts.add(p.getFileName().toString());
			int idx = Math.max(0, opts.indexOf(Config.INSTANCE.music));
			Config.INSTANCE.music = opts.get((idx + 1) % opts.size());
			b.setMessage(musicLabel());
			Config.save();
			if (MusicPlayer.isPlaying()) startSelected();
			playBtn.setMessage(playLabel());
		}).bounds(right, top, w, h).build());

		playBtn = addRenderableWidget(Button.builder(playLabel(), b -> {
			if (MusicPlayer.isPlaying()) MusicPlayer.stop();
			else startSelected();
			b.setMessage(playLabel());
		}).bounds(right, top + gap, w, h).build());

		volBtn = addRenderableWidget(Button.builder(volLabel(), b -> {
			int v = Config.INSTANCE.volume;
			Config.INSTANCE.volume = v >= 100 ? 0 : ((v / 25) + 1) * 25;
			b.setMessage(volLabel());
			Config.save();
		}).bounds(right, top + gap * 2, w, h).build());

		addRenderableWidget(Button.builder(Component.literal("Gotowe"), b -> onClose())
				.bounds(right, top + gap * 3, w, h).build());

		addRenderableWidget(Button.builder(topBarLabel(), b -> {
			Config.INSTANCE.topBar = !Config.INSTANCE.topBar;
			b.setMessage(topBarLabel());
			Config.save();
		}).bounds(left, top + gap * 4, w, h).build());

		addRenderableWidget(Button.builder(posLabel(), b -> {
			Config.INSTANCE.lyricsCenter = !Config.INSTANCE.lyricsCenter;
			b.setMessage(posLabel());
			Config.save();
		}).bounds(right, top + gap * 4, w, h).build());
	}

	private void startSelected() {
		String sel = Config.INSTANCE.music;
		if (sel.isEmpty()) {
			MusicPlayer.stop();
			return;
		}
		Path p = Config.MUSIC_DIR.resolve(sel);
		if (java.nio.file.Files.exists(p)) MusicPlayer.play(p);
	}

	private Component bgLabel() { return Component.literal("Tlo: " + shorten(Config.INSTANCE.background)); }
	private Component fontLabel() { return Component.literal("Czcionka: " + Assets.fontName()); }
	private Component lyricsLabel() { return Component.literal("Napisy: " + (Config.INSTANCE.lyrics ? "TAK" : "NIE")); }
	private Component topBarLabel() { return Component.literal("Pasek gorny: " + (Config.INSTANCE.topBar ? "TAK" : "NIE")); }
	private Component posLabel() { return Component.literal("Napisy: " + (Config.INSTANCE.lyricsCenter ? "srodek" : "dol")); }
	private Component volLabel() { return Component.literal("Glosnosc: " + Config.INSTANCE.volume + "%"); }
	private Component playLabel() { return Component.literal(MusicPlayer.isPlaying() ? "Stop" : "Odtwarzaj"); }
	private Component musicLabel() {
		String m = Config.INSTANCE.music;
		return Component.literal("Muzyka: " + (m.isEmpty() ? "brak" : shorten(m)));
	}

	private static String shorten(String s) {
		return s.length() > 16 ? s.substring(0, 15) + "~" : s;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Assets.drawBackground(g, width, height);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		g.drawCenteredString(font, Assets.styled("Fajne Niebo z Napisami"), width / 2, height / 2 - 70, 0xFFFFFFFF);
		g.drawCenteredString(font, Component.literal("Wlasne pliki: .minecraft/config/custommenu/ (music, backgrounds)"),
				width / 2, height / 2 + 84, 0xFFAAAAAA);
		Assets.drawLyrics(g, font, width, height - 40, true);
		playBtn.setMessage(playLabel());
	}

	@Override
	public void onClose() {
		Config.save();
		this.minecraft.setScreen(parent);
	}
}
