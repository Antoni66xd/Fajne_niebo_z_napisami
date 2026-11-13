package com.example.custommenu;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/** Tla, czcionki i rysowanie napisow. */
public final class Assets {
	public record Bg(String name, int top, int bottom, Path file, String sky) {}
	private record Tex(Identifier id, int w, int h) {}

	public static final String[] FONT_IDS = {"minecraft:default", "minecraft:uniform", "minecraft:alt", "minecraft:illageralt"};
	public static final String[] FONT_NAMES = {"Domyslna", "Unicode", "Enchanting", "Illager"};

	private static final List<Bg> BUILTIN = List.of(
			new Bg("Niebo: Noc", 0, 0, null, "night"),
			new Bg("Niebo: Zorza", 0, 0, null, "aurora"),
			new Bg("Niebo: Zachod", 0, 0, null, "sunset"),
			new Bg("Niebo: Swit", 0, 0, null, "dawn"),
			new Bg("Niebo: Dzien", 0, 0, null, "day"),
			new Bg("Ciemny", 0xFF0B0B12, 0xFF1C1C2E, null, null),
			new Bg("Niebieski", 0xFF0A1F44, 0xFF1E5AA8, null, null),
			new Bg("Zielony", 0xFF0B2E13, 0xFF2E7D32, null, null),
			new Bg("Czerwony", 0xFF3B0A0A, 0xFFB23A3A, null, null),
			new Bg("Fioletowy", 0xFF1E0B3B, 0xFF7E3AB2, null, null));

	private static final Map<Path, Tex> TEXTURES = new HashMap<>();
	private static int counter = 0;

	private Assets() {}

	/** Wbudowane tla + obrazki .png/.jpg z config/custommenu/backgrounds. */
	public static List<Bg> backgrounds() {
		List<Bg> all = new ArrayList<>(BUILTIN);
		try (Stream<Path> s = Files.list(Config.BG_DIR)) {
			s.filter(p -> {
				String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
				return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg");
			}).sorted().forEach(p -> all.add(new Bg(p.getFileName().toString(), 0, 0, p, null)));
		} catch (Exception ignored) {}
		return all;
	}

	public static Bg currentBackground() {
		for (Bg b : backgrounds()) {
			if (b.name().equals(Config.INSTANCE.background)) return b;
		}
		return BUILTIN.get(0);
	}

	private static Tex texture(Path file) {
		Tex cached = TEXTURES.get(file);
		if (cached != null) return cached;
		try (InputStream in = Files.newInputStream(file)) {
			NativeImage img = NativeImage.read(in);
			int w = img.getWidth();
			int h = img.getHeight();
			Identifier id = Identifier.fromNamespaceAndPath("custommenu", "bg/img" + (counter++));
			DynamicTexture tex = new DynamicTexture(() -> "custommenu background", img);
			Minecraft.getInstance().getTextureManager().register(id, tex);
			Tex t = new Tex(id, w, h);
			TEXTURES.put(file, t);
			return t;
		} catch (Exception e) {
			System.err.println("[custommenu] Nie mozna wczytac tla " + file + ": " + e);
			return null;
		}
	}

	public static void drawBackground(GuiGraphics g, int w, int h) {
		Bg bg = currentBackground();
		if (bg.sky() != null) {
			Sky.draw(g, w, h, bg.sky());
			return;
		}
		if (bg.file() != null) {
			Tex t = texture(bg.file());
			if (t != null) {
				g.blit(RenderPipelines.GUI_TEXTURED, t.id(), 0, 0, 0, 0, w, h, t.w(), t.h(), t.w(), t.h());
				g.fill(0, 0, w, h, 0x55000000);
				return;
			}
		}
		g.fillGradient(0, 0, w, h, bg.top(), bg.bottom());
	}

	/** Tekst w wybranej przez gracza czcionce. */
	public static Component styled(String text) {
		Identifier font = Identifier.parse(Config.INSTANCE.font);
		return Component.literal(text).withStyle(s -> s.withFont(new FontDescription.Resource(font)));
	}

	public static String fontName() {
		for (int i = 0; i < FONT_IDS.length; i++) {
			if (FONT_IDS[i].equals(Config.INSTANCE.font)) return FONT_NAMES[i];
		}
		return Config.INSTANCE.font;
	}

	/** Napisy z muzyki rysowane na tle ekranu. */
	public static void drawLyrics(GuiGraphics g, Font font, int screenW, int y, boolean box) {
		if (!Config.INSTANCE.lyrics) return;
		String text = MusicPlayer.currentText();
		if (text.isEmpty()) return;
		Component c = styled(text);
		int tw = font.width(c);
		int cx = screenW / 2;
		if (box) g.fill(cx - tw / 2 - 6, y - 4, cx + tw / 2 + 6, y + 9 + 4, 0x88000000);
		g.drawCenteredString(font, c, cx, y, 0xFFFFFFFF);
	}

	private static long fpsStamp = System.currentTimeMillis();
	private static int fpsFrames, fpsValue;

	private static int pill(GuiGraphics g, Font font, int x, int y, int w, Component text) {
		int hh = 15;
		g.fill(x, y, x + w, y + hh, 0xAA101018);
		g.fill(x, y + hh - 1, x + w, y + hh, 0xFF8A5CF5);
		g.drawString(font, text, x + 6, y + 3, 0xFFFFFFFF);
		return x + w + 4;
	}

	/** Gorny pasek: FPS, czas + aktualny napis, ping. */
	public static void drawTopBar(GuiGraphics g, Font font, int screenW) {
		long now = System.currentTimeMillis();
		fpsFrames++;
		if (now - fpsStamp >= 1000) {
			fpsValue = fpsFrames;
			fpsFrames = 0;
			fpsStamp = now;
		}
		Minecraft mc = Minecraft.getInstance();
		int ping = 0;
		if (mc.getConnection() != null && mc.player != null) {
			var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			if (info != null) ping = info.getLatency();
		}
		Component fps = Component.literal(fpsValue + " FPS");
		Component pg = Component.literal(ping + " ms");
		String mid = MusicPlayer.isPlaying()
				? MusicPlayer.timeText() + "  " + MusicPlayer.currentText()
				: "brak muzyki";
		Component m = Component.literal(font.plainSubstrByWidth(mid, 190));
		int fw = font.width(fps) + 12, pw = font.width(pg) + 12, mw = 202;
		int total = fw + mw + pw + 8;
		int x = screenW / 2 - total / 2;
		x = pill(g, font, x, 4, fw, fps);
		x = pill(g, font, x, 4, mw, m);
		pill(g, font, x, 4, pw, pg);
	}
}
