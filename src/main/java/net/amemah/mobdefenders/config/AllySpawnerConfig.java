package net.amemah.mobdefenders.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AllySpawnerConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Settings current = defaults();

	public record Settings(int respawnDelaySeconds, int spawnRadius, int roamRadius, int recallRadius, int detectionRadius) {
		public Settings(int delay, int spawn, int roam) { this(delay, spawn, roam, Math.max(12, roam), 10); }
		public Settings(int delay, int spawn, int roam, int recall) { this(delay, spawn, roam, recall, 10); }
		public Settings {
			if (respawnDelaySeconds < 1 || respawnDelaySeconds > 86400
				|| spawnRadius < 1 || spawnRadius > 32 || roamRadius < spawnRadius || roamRadius > 64
				|| recallRadius < roamRadius || recallRadius > 128 || detectionRadius < 1 || detectionRadius > 64) {
				throw new IllegalArgumentException("Delay 1..86400; spawn radius 1..32; roam radius spawnRadius..64; recall radius roamRadius..128; detection radius 1..64");
			}
		}
		public long delayTicks() { return respawnDelaySeconds * 20L; }
		public long initialDelayTicks() { return (respawnDelaySeconds / 2) * 20L; }
	}

	public static Settings get() { return current; }
	public static Settings defaults() { return new Settings(90, 3, 6, 12, 10); }

	/** Older configs retain custom values; the new recall field is optional on disk. */
	static Settings parse(String json) {
		var object = JsonParser.parseString(json).getAsJsonObject();
		if (!object.has("recallRadius")) {
			int roam = object.has("roamRadius") ? object.get("roamRadius").getAsInt() : 6;
			object.addProperty("recallRadius", Math.max(12, roam));
		}
		if (!object.has("detectionRadius")) object.addProperty("detectionRadius", 10);
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
	private AllySpawnerConfig() { }
}
