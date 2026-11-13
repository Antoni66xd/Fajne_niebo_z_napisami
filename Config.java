package com.example.custommenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia zapisywane w .minecraft/config/custommenu/config.json */
public class Config {
	public String background = "Niebo: Noc";
	public String font = "minecraft:default";
	public String music = "";
	public int volume = 70;
	public boolean lyrics = true;
	public boolean lyricsCenter = true;
	public boolean topBar = true;
	public String playlistUrl = "https://www.youtube.com/playlist?list=PLLf1AHkYrDMA";

	public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("custommenu");
	public static final Path MUSIC_DIR = DIR.resolve("music");
	public static final Path BG_DIR = DIR.resolve("backgrounds");
	private static final Path FILE = DIR.resolve("config.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static Config INSTANCE = new Config();

	public static void load() {
		try {
			Files.createDirectories(MUSIC_DIR);
			Files.createDirectories(BG_DIR);
			if (Files.exists(FILE)) {
				Config c = GSON.fromJson(Files.readString(FILE), Config.class);
				if (c != null) INSTANCE = c;
			} else {
				save();
			}
		} catch (Exception e) {
			System.err.println("[custommenu] Nie udalo sie wczytac configu: " + e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(DIR);
			Files.writeString(FILE, GSON.toJson(INSTANCE));
		} catch (IOException e) {
			System.err.println("[custommenu] Nie udalo sie zapisac configu: " + e);
		}
	}
}
