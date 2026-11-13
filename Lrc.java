package com.example.custommenu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Prosty parser plikow .lrc ([mm:ss.xx]tekst). */
public final class Lrc {
	public record Line(long ms, String text) {}

	private static final Pattern TIME = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?]");

	private Lrc() {}

	public static List<Line> parse(Path file) {
		List<Line> out = new ArrayList<>();
		if (file == null || !Files.exists(file)) return out;
		try {
			for (String raw : Files.readAllLines(file)) {
				Matcher m = TIME.matcher(raw);
				List<Long> times = new ArrayList<>();
				int end = 0;
				while (m.find()) {
					long min = Long.parseLong(m.group(1));
					long sec = Long.parseLong(m.group(2));
					long frac = 0;
					String f = m.group(3);
					if (f != null) {
						frac = Long.parseLong(f);
						if (f.length() == 1) frac *= 100;
						else if (f.length() == 2) frac *= 10;
					}
					times.add(min * 60_000 + sec * 1000 + frac);
					end = m.end();
				}
				if (times.isEmpty()) continue;
				String text = raw.substring(end).trim();
				for (long t : times) out.add(new Line(t, text));
			}
		} catch (Exception e) {
			System.err.println("[custommenu] Blad wczytywania .lrc: " + e);
		}
		out.sort(Comparator.comparingLong(Line::ms));
		return out;
	}

	public static String at(List<Line> lines, long ms) {
		String cur = "";
		for (Line l : lines) {
			if (l.ms() <= ms) cur = l.text();
			else break;
		}
		return cur;
	}
}
