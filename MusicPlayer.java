package com.example.custommenu;

import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Odtwarza pliki .ogg i .wav z folderu config/custommenu/music w osobnym watku. */
public final class MusicPlayer {
	private record Decoded(byte[] pcm, int rate, int channels) {}

	private static volatile Thread thread;
	private static volatile boolean stopFlag;
	private static volatile SourceDataLine line;
	private static volatile String currentName = "";
	private static volatile List<Lrc.Line> lyrics = List.of();

	private MusicPlayer() {}

	public static List<Path> tracks() {
		try (Stream<Path> s = Files.list(Config.MUSIC_DIR)) {
			return s.filter(p -> {
				String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
				return n.endsWith(".ogg") || n.endsWith(".wav");
			}).sorted().toList();
		} catch (IOException e) {
			return List.of();
		}
	}

	public static boolean isPlaying() {
		Thread t = thread;
		return t != null && t.isAlive();
	}

	public static void play(Path first) {
		stop();
		stopFlag = false;
		Thread t = new Thread(() -> loop(first), "custommenu-music");
		t.setDaemon(true);
		thread = t;
		t.start();
	}

	public static void stop() {
		stopFlag = true;
		SourceDataLine l = line;
		if (l != null) {
			try { l.stop(); l.flush(); } catch (Exception ignored) {}
		}
		Thread t = thread;
		if (t != null && t != Thread.currentThread()) {
			try { t.join(500); } catch (InterruptedException ignored) {}
		}
		thread = null;
		currentName = "";
		lyrics = List.of();
	}

	/** Czas odtwarzania w formacie m:ss. */
	public static String timeText() {
		SourceDataLine sl = line;
		long s = sl == null ? 0 : sl.getMicrosecondPosition() / 1_000_000L;
		return (s / 60) + ":" + String.format("%02d", s % 60);
	}

	/** Tekst do wyswietlenia na ekranie (aktualna linijka z .lrc albo nazwa utworu). */
	public static String currentText() {
		if (!isPlaying()) return "";
		List<Lrc.Line> l = lyrics;
		if (l.isEmpty()) return "♪ " + currentName;
		SourceDataLine sl = line;
		long ms = sl == null ? 0 : sl.getMicrosecondPosition() / 1000;
		String t = Lrc.at(l, ms);
		return t.isEmpty() ? "" : "♪ " + t;
	}

	private static void loop(Path first) {
		Path next = first;
		while (!stopFlag && next != null) {
			playOne(next);
			if (stopFlag) break;
			List<Path> all = tracks();
			if (all.isEmpty()) break;
			int i = all.indexOf(next);
			next = all.get((i + 1) % all.size());
			Config.INSTANCE.music = next.getFileName().toString();
		}
	}

	private static void playOne(Path file) {
		String name = file.getFileName().toString();
		String base = name.substring(0, name.lastIndexOf('.'));
		currentName = base;
		lyrics = Lrc.parse(file.resolveSibling(base + ".lrc"));
		SourceDataLine sdl = null;
		try {
			Decoded d = decode(file);
			AudioFormat fmt = new AudioFormat(d.rate(), 16, d.channels(), true, false);
			sdl = AudioSystem.getSourceDataLine(fmt);
			sdl.open(fmt);
			sdl.start();
			line = sdl;
			int chunk = 4096;
			for (int off = 0; off < d.pcm().length && !stopFlag; off += chunk) {
				int len = Math.min(chunk, d.pcm().length - off);
				byte[] buf = new byte[len];
				float vol = Math.max(0, Math.min(100, Config.INSTANCE.volume)) / 100f;
				for (int i = 0; i + 1 < len; i += 2) {
					short s = (short) ((d.pcm()[off + i] & 0xFF) | (d.pcm()[off + i + 1] << 8));
					s = (short) (s * vol);
					buf[i] = (byte) (s & 0xFF);
					buf[i + 1] = (byte) ((s >> 8) & 0xFF);
				}
				sdl.write(buf, 0, len);
			}
			if (!stopFlag) sdl.drain();
		} catch (Exception e) {
			System.err.println("[custommenu] Blad odtwarzania " + name + ": " + e);
			stopFlag = true;
		} finally {
			if (sdl != null) {
				try { sdl.stop(); sdl.close(); } catch (Exception ignored) {}
			}
			line = null;
		}
	}

	private static Decoded decode(Path file) throws Exception {
		String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
		return n.endsWith(".ogg") ? decodeOgg(file) : decodeWav(file);
	}

	private static Decoded decodeWav(Path file) throws Exception {
		try (AudioInputStream in = AudioSystem.getAudioInputStream(file.toFile())) {
			AudioFormat b = in.getFormat();
			AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, b.getSampleRate(), 16,
					b.getChannels(), b.getChannels() * 2, b.getSampleRate(), false);
			try (AudioInputStream pcm = AudioSystem.getAudioInputStream(target, in)) {
				return new Decoded(pcm.readAllBytes(), (int) b.getSampleRate(), b.getChannels());
			}
		}
	}

	private static Decoded decodeOgg(Path file) throws Exception {
		byte[] raw = Files.readAllBytes(file);
		ByteBuffer data = MemoryUtil.memAlloc(raw.length);
		try (MemoryStack st = MemoryStack.stackPush()) {
			data.put(raw).flip();
			IntBuffer ch = st.mallocInt(1);
			IntBuffer sr = st.mallocInt(1);
			ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(data, ch, sr);
			if (pcm == null) throw new IOException("Nie mozna zdekodowac OGG");
			try {
				byte[] bytes = new byte[pcm.remaining() * 2];
				ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm);
				return new Decoded(bytes, sr.get(0), ch.get(0));
			} finally {
				LibCStdlib.free(pcm);
			}
		} finally {
			MemoryUtil.memFree(data);
		}
	}
}
