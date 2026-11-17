package net.amemah.mobdefenders.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ZombieSpawnerConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Settings current = defaults();

	public record Settings(int respawnDelaySeconds, int spawnRadius, int roamRadius, int recallRadius) {
		public Settings(int delay, int spawn, int roam) { this(delay, spawn, roam, Math.max(12, roam)); }
		public Settings {
			if (respawnDelaySeconds < 1 || respawnDelaySeconds > 86400
				|| spawnRadius < 1 || spawnRadius > 32 || roamRadius < spawnRadius || roamRadius > 64
				|| recallRadius < roamRadius || recallRadius > 128) {
				throw new IllegalArgumentException("Delay 1..86400; spawn radius 1..32; roam radius spawnRadius..64; recall radius roamRadius..128");
			}
		}
		public long delayTicks() { return respawnDelaySeconds * 20L; }
		public long initialDelayTicks() { return (respawnDelaySeconds / 2) * 20L; }
	}

	public static Settings get() { return current; }
	public static Settings defaults() { return new Settings(90, 3, 6, 12); }

	/** Older configs retain custom values; the new recall field is optional on disk. */
	static Settings parse(String json) {
		var object = JsonParser.parseString(json).getAsJsonObject();
		if (!object.has("recallRadius")) {
			int roam = object.has("roamRadius") ? object.get("roamRadius").getAsInt() : 6;
			object.addProperty("recallRadius", Math.max(12, roam));
		}
		return GSON.fromJson(object, Settings.class);
	}

	public static void load() {
		current = defaults();
		Path file = FabricLoader.getInstance().getConfigDir().resolve("mobdefenders.json");
		try {
			if (!Files.exists(file)) {
				Files.createDirectories(file.getParent());
				Files.writeString(file, GSON.toJson(current));
			} else {
				Settings parsed = parse(Files.readString(file));
				if (parsed == null) throw new IllegalArgumentException("Empty configuration");
				current = parsed;
			}
		} catch (Exception exception) {
			LoggerFactory.getLogger("mobdefenders").error("Cannot read {}; keeping the file intact and using defaults", file, exception);
		}
	}
	private ZombieSpawnerConfig() { }
}
