package net.amemah.mobdefenders.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZombieSpawnerConfigTest {
	@Test void defaultsAndInitialDelay() {
		var settings = ZombieSpawnerConfig.defaults();
		assertEquals(1800, settings.delayTicks());
		assertEquals(900, settings.initialDelayTicks());
		assertEquals(900, new ZombieSpawnerConfig.Settings(91, 3, 6).initialDelayTicks());
		assertEquals(12, settings.recallRadius());
	}
	@Test void validatesBounds() {
		assertThrows(IllegalArgumentException.class, () -> new ZombieSpawnerConfig.Settings(0, 3, 6));
		assertThrows(IllegalArgumentException.class, () -> new ZombieSpawnerConfig.Settings(86401, 3, 6));
		assertThrows(IllegalArgumentException.class, () -> new ZombieSpawnerConfig.Settings(180, 0, 6));
		assertThrows(IllegalArgumentException.class, () -> new ZombieSpawnerConfig.Settings(180, 33, 40));
		assertThrows(IllegalArgumentException.class, () -> new ZombieSpawnerConfig.Settings(180, 7, 6));
		assertThrows(IllegalArgumentException.class, () -> new ZombieSpawnerConfig.Settings(180, 3, 65));
		assertThrows(IllegalArgumentException.class, () -> new ZombieSpawnerConfig.Settings(90, 3, 6, 5));
		assertThrows(IllegalArgumentException.class, () -> new ZombieSpawnerConfig.Settings(90, 3, 6, 129));
	}
	@Test void gsonCannotBypassValidation() {
		assertThrows(RuntimeException.class, () -> new Gson().fromJson(
			"{\"respawnDelaySeconds\":180,\"spawnRadius\":3}", ZombieSpawnerConfig.Settings.class));
	}
	@Test void spawnCapIsNotASetting() {
		assertTrue(java.util.Arrays.stream(ZombieSpawnerConfig.Settings.class.getRecordComponents())
			.noneMatch(field -> field.getName().toLowerCase().contains("limit") || field.getName().toLowerCase().contains("max")));
	}
	@Test void olderConfigsGainRecallWithoutLosingCustomValues() {
		var settings = ZombieSpawnerConfig.parse("{\"respawnDelaySeconds\":123,\"spawnRadius\":4,\"roamRadius\":9}");
		assertEquals(123, settings.respawnDelaySeconds());
		assertEquals(4, settings.spawnRadius());
		assertEquals(9, settings.roamRadius());
		assertEquals(12, settings.recallRadius());
		assertEquals(24, ZombieSpawnerConfig.parse(
			"{\"respawnDelaySeconds\":90,\"spawnRadius\":4,\"roamRadius\":24}").recallRadius());
		assertEquals(16, ZombieSpawnerConfig.parse(
			"{\"respawnDelaySeconds\":90,\"spawnRadius\":4,\"roamRadius\":6,\"recallRadius\":16}").recallRadius());
	}
}
