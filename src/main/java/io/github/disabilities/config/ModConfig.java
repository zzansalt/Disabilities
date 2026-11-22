package io.github.disabilities.config;

import io.github.disabilities.Disabilities;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class ModConfig {
	public enum Language {
		EN("en"),
		KO("ko"),
		AUTO("auto");

		public final String key;

		Language(String key) {
			this.key = key;
		}

		public static Language parse(String value) {
			if (value == null) {
				return AUTO;
			}
			String v = value.trim().toLowerCase(Locale.ROOT);
			return switch (v) {
				case "en", "en_us", "english" -> EN;
				case "ko", "kr", "ko_kr", "korean" -> KO;
				default -> AUTO;
			};
		}
	}

	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("disabilities.properties");
	private static volatile Language language = Language.AUTO;
	private static volatile Supplier<String> clientLanguageSupplier = () -> "en_us";
	private static volatile FileTime lastModified = null;
	private static volatile long lastCheckMs = 0L;

	private ModConfig() {
	}

	public static void setClientLanguageSupplier(Supplier<String> supplier) {
		if (supplier != null) {
			clientLanguageSupplier = supplier;
		}
	}

	public static Language getLanguage() {
		checkHotReload();
		return language;
	}

	public static void setLanguage(Language newLanguage) {
		if (newLanguage == null) {
			return;
		}
		language = newLanguage;
		save();
	}

	public static boolean isKorean() {
		Language lang = getLanguage();
		if (lang == Language.KO) {
			return true;
		}
		if (lang == Language.EN) {
			return false;
		}
		String mcLang = clientLanguageSupplier.get();
		return mcLang != null && mcLang.toLowerCase(Locale.ROOT).startsWith("ko");
	}

	public static Path getConfigPath() {
		return CONFIG_PATH;
	}

	public static synchronized void load() {
		if (!Files.exists(CONFIG_PATH)) {
			save();
			return;
		}
		try {
			List<String> lines = Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8);
			for (String raw : lines) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				int eq = line.indexOf('=');
				if (eq < 0) {
					continue;
				}
				String key = line.substring(0, eq).trim().toLowerCase(Locale.ROOT);
				String val = line.substring(eq + 1).trim();
				if (key.equals("language")) {
					language = Language.parse(val);
				}
			}
			lastModified = Files.getLastModifiedTime(CONFIG_PATH);
			lastCheckMs = System.currentTimeMillis();
		} catch (IOException e) {
			Disabilities.LOGGER.error("Failed to load config file {}", CONFIG_PATH, e);
		}
	}

	public static synchronized void save() {
		String content = """
			# Disabilities Mod Configuration
			# Display language for disability names, descriptions, and HUD messages
			# Options: en (English), ko (Korean), auto (follow Minecraft language)
			language=%s
			""".formatted(language.key);
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, content, StandardCharsets.UTF_8);
			lastModified = Files.getLastModifiedTime(CONFIG_PATH);
			lastCheckMs = System.currentTimeMillis();
		} catch (IOException e) {
			Disabilities.LOGGER.error("Failed to save config file {}", CONFIG_PATH, e);
		}
	}

	private static void checkHotReload() {
		long now = System.currentTimeMillis();
		if (now - lastCheckMs < 2000L) {
			return;
		}
		lastCheckMs = now;
		try {
			if (Files.exists(CONFIG_PATH)) {
				FileTime current = Files.getLastModifiedTime(CONFIG_PATH);
				if (lastModified == null || !current.equals(lastModified)) {
					load();
				}
			}
		} catch (IOException ignored) {
		}
	}
}
