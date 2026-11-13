package com.example.custommenu;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/** Animowane, proceduralne niebo rysowane samymi prostokatami (w stylu Minecrafta). */
public final class Sky {
	private static final int STARS = 140;
	private static final float[] SX = new float[STARS];
	private static final float[] SY = new float[STARS];
	private static final float[] SP = new float[STARS];

	static {
		Random r = new Random(42);
		for (int i = 0; i < STARS; i++) {
			SX[i] = r.nextFloat();
			SY[i] = r.nextFloat() * 0.7f;
			SP[i] = r.nextFloat() * 6.28f;
		}
	}

	private Sky() {}

	public static void draw(GuiGraphics g, int w, int h, String kind) {
		float t = (System.currentTimeMillis() % 10_000_000L) / 1000f;
		switch (kind) {
			case "day" -> {
				bands(g, w, h, 0xFF3A8DDE, 0xFF7FC4F5, 0xFFBFE6FF);
				sun(g, w, h, 0.78f, 0.2f, 0xFFFFF2A0);
				clouds(g, w, h, t, 0xCCFFFFFF, 6);
			}
			case "sunset" -> {
				bands(g, w, h, 0xFF1B1B4B, 0xFF7B2E7E, 0xFFE8645A, 0xFFFFB347);
				sun(g, w, h, 0.3f, 0.64f, 0xFFFFD27A);
				clouds(g, w, h, t, 0x99FFB0A0, 5);
				ground(g, w, h, 0xFF140A1E);
			}
			case "night" -> {
				bands(g, w, h, 0xFF02030F, 0xFF0A1030, 0xFF18224A);
				stars(g, w, h, t);
				moon(g, w, h, 0.8f, 0.18f);
				ground(g, w, h, 0xFF05060C);
			}
			case "aurora" -> {
				bands(g, w, h, 0xFF01040C, 0xFF06142A, 0xFF0B2A3A);
				stars(g, w, h, t);
				aurora(g, w, h, t);
				ground(g, w, h, 0xFF03060A);
			}
			case "dawn" -> {
				bands(g, w, h, 0xFF3B3A7A, 0xFFD87FA8, 0xFFFFC9A0, 0xFFFFE8B8);
				sun(g, w, h, 0.5f, 0.74f, 0xFFFFF0C0);
				clouds(g, w, h, t, 0xAAFFE0E0, 5);
			}
			default -> g.fillGradient(0, 0, w, h, 0xFF0B0B12, 0xFF1C1C2E);
		}
	}

	private static void bands(GuiGraphics g, int w, int h, int... c) {
		int n = c.length - 1;
		for (int i = 0; i < n; i++) {
			g.fillGradient(0, h * i / n, w, h * (i + 1) / n + 1, c[i], c[i + 1]);
		}
	}

	private static void sun(GuiGraphics g, int w, int h, float fx, float fy, int color) {
		int cx = (int) (w * fx), cy = (int) (h * fy);
		int glow = (color & 0x00FFFFFF) | 0x22000000;
		for (int r : new int[]{30, 24, 18}) g.fill(cx - r, cy - r, cx + r, cy + r, glow);
		g.fill(cx - 13, cy - 13, cx + 13, cy + 13, color);
	}

	private static void moon(GuiGraphics g, int w, int h, float fx, float fy) {
		int cx = (int) (w * fx), cy = (int) (h * fy);
		int glow = 0x22AACCFF;
		for (int r : new int[]{22, 17}) g.fill(cx - r, cy - r, cx + r, cy + r, glow);
		g.fill(cx - 11, cy - 11, cx + 11, cy + 11, 0xFFE8EEF8);
		g.fill(cx - 5, cy - 6, cx - 1, cy - 2, 0xFFC5CEE0);
		g.fill(cx + 3, cy + 1, cx + 8, cy + 6, 0xFFC5CEE0);
	}

	private static void clouds(GuiGraphics g, int w, int h, float t, int color, int count) {
		for (int i = 0; i < count; i++) {
			float speed = 6 + i * 2;
			int x = (int) (((i * 173 + t * speed) % (w + 140)) - 70);
			int y = (int) (h * (0.08f + (i * 0.13f) % 0.35f));
			int cw = 50 + (i * 37) % 40;
			g.fill(x + 8, y, x + cw - 8, y + 5, color);
			g.fill(x, y + 5, x + cw, y + 12, color);
			g.fill(x + 12, y + 12, x + cw - 12, y + 16, color);
		}
	}

	private static void stars(GuiGraphics g, int w, int h, float t) {
		for (int i = 0; i < STARS; i++) {
			float tw = 0.5f + 0.5f * (float) Math.sin(t * 1.5f + SP[i]);
			int a = 110 + (int) (145 * tw);
			int size = i % 7 == 0 ? 2 : 1;
			int x = (int) (SX[i] * w), y = (int) (SY[i] * h);
			g.fill(x, y, x + size, y + size, (a << 24) | 0xFFFFFF);
		}
	}

	private static void ground(GuiGraphics g, int w, int h, int color) {
		for (int x = 0; x < w; x += 6) {
			int hh = 12 + (int) (8 * Math.sin(x * 0.05) + 6 * Math.sin(x * 0.13 + 1));
			g.fill(x, h - hh, x + 6, h, color);
		}
	}

	private static void aurora(GuiGraphics g, int w, int h, float t) {
		int[] rgb = {0x33FF99, 0x55AAFF};
		for (int layer = 0; layer < 2; layer++) {
			for (int x = 0; x < w; x += 3) {
				float v = 0.5f + 0.5f * (float) Math.sin(x * 0.012f + t * 0.5f + layer * 2f);
				int top = (int) (h * (0.12f + 0.15f * Math.sin(x * 0.007f + t * 0.3f + layer)));
				int len = (int) (h * (0.2f + 0.25f * v));
				g.fillGradient(x, top, x + 3, top + len, 0x66000000 | rgb[layer], rgb[layer]);
			}
		}
	}
}
