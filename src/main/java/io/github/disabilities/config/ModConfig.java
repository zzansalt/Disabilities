package io.github.disabilities.config;

import io.github.disabilities.Disabilities;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
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

	public static final int DEFAULT_ACROPHOBIA_Y = 65;

	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("disabilities.properties");
	private static volatile Language language = Language.AUTO;
	private static volatile int acrophobiaY = DEFAULT_ACROPHOBIA_Y;
	private static volatile List<String> touretteMessages = List.of();
	private static volatile Supplier<String> clientLanguageSupplier = () -> "en_us";
	private static volatile java.util.function.IntSupplier effectiveAcrophobiaYSupplier = null;
	private static volatile FileTime lastModified = null;
	private static volatile long lastCheckMs = 0L;
	private static final AtomicInteger VERSION = new AtomicInteger(0);

	private ModConfig() {
	}

	public static void setClientLanguageSupplier(Supplier<String> supplier) {
		if (supplier != null) {
			clientLanguageSupplier = supplier;
		}
	}

	public static void setEffectiveAcrophobiaYSupplier(java.util.function.IntSupplier supplier) {
		effectiveAcrophobiaYSupplier = supplier;
	}

	public static int getEffectiveAcrophobiaY() {
		if (effectiveAcrophobiaYSupplier != null) {
			return effectiveAcrophobiaYSupplier.getAsInt();
		}
		return getAcrophobiaY();
	}

	public static int getVersion() {
		checkHotReload();
		return VERSION.get();
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
		VERSION.incrementAndGet();
		save();
	}

	public static int getAcrophobiaY() {
		checkHotReload();
		return acrophobiaY;
	}

	public static void setAcrophobiaY(int y) {
		acrophobiaY = Math.clamp(y, -64, 320);
		VERSION.incrementAndGet();
		save();
	}

	public static List<String> getTouretteMessages() {
		checkHotReload();
		return touretteMessages;
	}

	public static void setTouretteMessages(List<String> messages) {
		List<String> cleaned = new ArrayList<>();
		if (messages != null) {
			for (String m : messages) {
				if (m != null) {
					String trimmed = m.trim();
					if (!trimmed.isEmpty()) {
						cleaned.add(trimmed);
					}
				}
			}
		}
		touretteMessages = Collections.unmodifiableList(cleaned);
		VERSION.incrementAndGet();
		save();
	}

	public static boolean addTouretteMessage(String message) {
		if (message == null) {
			return false;
		}
		String trimmed = message.trim();
		if (trimmed.isEmpty()) {
			return false;
		}
		List<String> next = new ArrayList<>(getTouretteMessages());
		next.add(trimmed);
		setTouretteMessages(next);
		return true;
	}

	public static String removeTouretteMessage(String indexOrText) {
		if (indexOrText == null || indexOrText.isBlank()) {
			return null;
		}
		String trimmed = indexOrText.trim();
		List<String> next = new ArrayList<>(getTouretteMessages());
		// Try 1-based index first
		try {
			int idx = Integer.parseInt(trimmed) - 1;
			if (idx >= 0 && idx < next.size()) {
				String removed = next.remove(idx);
				setTouretteMessages(next);
				return removed;
			}
		} catch (NumberFormatException ignored) {
		}
		// Otherwise match exact text (case-insensitive)
		for (int i = 0; i < next.size(); i++) {
			if (next.get(i).equalsIgnoreCase(trimmed)) {
				String removed = next.remove(i);
				setTouretteMessages(next);
				return removed;
			}
		}
		return null;
	}

	public static void clearTouretteMessages() {
		setTouretteMessages(List.of());
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
			boolean hasAcro = false;
			boolean hasTourette = false;
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
				switch (key) {
					case "language" -> language = Language.parse(val);
					case "acrophobia_y" -> {
						hasAcro = true;
						try {
							acrophobiaY = Math.clamp(Integer.parseInt(val), -64, 320);
						} catch (NumberFormatException ignored) {
							acrophobiaY = DEFAULT_ACROPHOBIA_Y;
						}
					}
					case "tourette_messages" -> {
						hasTourette = true;
						List<String> parsed = new ArrayList<>();
						if (!val.isBlank()) {
							for (String token : val.split(",")) {
								String t = token.trim();
								if (!t.isEmpty()) {
									parsed.add(t);
								}
							}
						}
						touretteMessages = Collections.unmodifiableList(parsed);
					}
					default -> {
					}
				}
			}
			VERSION.incrementAndGet();
			if (!hasAcro || !hasTourette) {
				// Upgrade older config files with newly added keys
				save();
			} else {
				lastModified = Files.getLastModifiedTime(CONFIG_PATH);
				lastCheckMs = System.currentTimeMillis();
			}
		} catch (IOException e) {
			Disabilities.LOGGER.error("Failed to load config file {}", CONFIG_PATH, e);
		}
	}

	public static synchronized void save() {
		String joinedMessages = String.join(", ", touretteMessages);
		String content = """
			# Disabilities Mod Configuration
			# Display language for disability names, descriptions, and HUD messages
			# Options: auto (follow Minecraft language), en (English), ko (Korean)
			language=%s

			# Y-level threshold for Acrophobia (server setting overrides clients in multiplayer)
			acrophobia_y=%d

			# Custom chat messages for Tourette's (comma-separated).
			# Leave empty to use random Minecraft item/block/mob/biome names.
			# Example: tourette_messages=Oops!, Watch out!, Creeper!
			tourette_messages=%s
			""".formatted(language.key, acrophobiaY, joinedMessages);
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
