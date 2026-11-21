package net.amemah.mobdefenders.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public final class AllySpawnerConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Configuration current = builtInConfiguration();

	public record Configuration(Settings defaults, Map<SpawnerKind, Settings> spawners) {
		public Configuration {
			Objects.requireNonNull(defaults);
			spawners = Map.copyOf(spawners);
		}
		public Settings forKind(SpawnerKind kind) {
			if (kind == null) return defaults;
			Settings resolved = spawners.get(kind);
			return resolved != null ? resolved : AllySpawnerConfig.defaults(kind);
		}
	}

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

	public static Settings get() { return current.defaults(); }
	public static Settings get(SpawnerKind kind) { return current.forKind(kind); }
	public static Settings defaults() { return new Settings(90, 3, 6, 12, 10); }

	/**
	 * Edit each spawner's built-in defaults here.
	 * Order: replacement seconds, spawn radius, patrol radius, recall radius, detection radius.
	 * Explicit JSON values override these; absent JSON fields use the corresponding values here.
	 */
	public static Settings defaults(SpawnerKind kind) {
		if (kind == null) return defaults();
		return switch (kind) {
			case ZOMBIE_ALLY -> new Settings(90, 3, 6, 15, 10);
			case SKELETON_ALLY -> new Settings(90, 3, 6, 15, 10);
			case SPIDER_ALLY -> new Settings(90, 3, 6, 15, 10);
			case CREEPER_ALLY -> new Settings(60, 3, 6, 15, 10);
			case ENDERMAN_ALLY -> new Settings(90, 3, 6, 15, 10);
			case SLIME_ALLY -> new Settings(90, 3, 6, 15, 10);
			case DROWNED_ALLY -> new Settings(90, 3, 6, 15, 10);
			case BRUTE_ALLY -> new Settings(90, 3, 6, 15, 10);
			case MAGMA_CUBE_ALLY -> new Settings(90, 3, 6, 15, 10);
			case BLAZE_ALLY -> new Settings(90, 3, 6, 15, 10);
			case GHAST_ALLY -> new Settings(90, 3, 6, 15, 10);
			case WITHER_SKELETON_ALLY -> new Settings(90, 3, 6, 15, 10);
		};
	}

	private static Configuration builtInConfiguration() {
		Map<SpawnerKind, Settings> resolved = new EnumMap<>(SpawnerKind.class);
		for (var kind : SpawnerKind.values()) resolved.put(kind, defaults(kind));
		return new Configuration(defaults(), resolved);
	}

	/** Retained for legacy callers/tests: returns the resolved shared defaults. */
	static Settings parse(String json) {
		return parseConfiguration(json).defaults();
	}

	static Configuration parseConfiguration(String json) {
		return parseConfiguration(json, AllySpawnerConfig::defaults);
	}

	/** Separate provider lets tests prove inheritance using deliberately different species defaults. */
	static Configuration parseConfiguration(String json, Function<SpawnerKind, Settings> codeDefaults) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		// Flat pre-override files remain the shared defaults, without changing them on disk.
		JsonObject sharedFields = root.has("defaults") ? root.getAsJsonObject("defaults") : root;
		Settings shared = merge(sharedFields, defaults());
		Map<SpawnerKind, JsonObject> overrides = new EnumMap<>(SpawnerKind.class);
		if (root.has("spawners")) {
			for (var entry : root.getAsJsonObject("spawners").entrySet()) {
				SpawnerKind kind = Arrays.stream(SpawnerKind.values())
					.filter(value -> entry.getKey().equals(value.configKey())
						|| entry.getKey().equals("mobdefenders:" + value.configKey()))
					.findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown spawner: " + entry.getKey()));
				if (overrides.containsKey(kind)) throw new IllegalArgumentException("Duplicate spawner override: " + entry.getKey());
				overrides.put(kind, entry.getValue().getAsJsonObject());
			}
		}
		Map<SpawnerKind, Settings> resolved = new EnumMap<>(SpawnerKind.class);
		for (var kind : SpawnerKind.values()) {
			JsonObject fields = sharedFields.deepCopy();
			if (overrides.containsKey(kind))
				for (var entry : overrides.get(kind).entrySet()) fields.add(entry.getKey(), entry.getValue());
			resolved.put(kind, merge(fields, codeDefaults.apply(kind)));
		}
		return new Configuration(shared, resolved);
	}

	private static Settings merge(JsonObject object, Settings fallback) {
		int roam = integer(object, "roamRadius", fallback.roamRadius());
		return new Settings(integer(object, "respawnDelaySeconds", fallback.respawnDelaySeconds()),
			integer(object, "spawnRadius", fallback.spawnRadius()), roam,
			integer(object, "recallRadius", Math.max(fallback.recallRadius(), roam)),
			integer(object, "detectionRadius", fallback.detectionRadius()));
	}

	private static int integer(JsonObject object, String name, int fallback) {
		if (!object.has(name)) return fallback;
		var value = object.get(name);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
			throw new IllegalArgumentException(name + " must be an integer");
		return value.getAsBigDecimal().intValueExact();
	}

	public static void load() {
		current = builtInConfiguration();
		Path file = FabricLoader.getInstance().getConfigDir().resolve("mobdefenders.json");
		try {
			if (!Files.exists(file)) {
				Files.createDirectories(file.getParent());
				JsonObject root = new JsonObject();
				// Empty declarations allow future code-default edits to take effect after rebuilding.
				root.add("defaults", new JsonObject());
				root.add("spawners", new JsonObject());
				Files.writeString(file, GSON.toJson(root));
			} else {
				current = parseConfiguration(Files.readString(file));
			}
		} catch (Exception exception) {
			LoggerFactory.getLogger("mobdefenders").error("Cannot read {}; keeping the file intact and using defaults", file, exception);
		}
	}
	private AllySpawnerConfig() { }
}
