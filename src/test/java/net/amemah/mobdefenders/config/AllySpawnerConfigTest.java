package net.amemah.mobdefenders.config;

import com.google.gson.Gson;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AllySpawnerConfigTest {
	@Test void absentSettingsAndAbsentKindsUseDefaults() {
		var config = AllySpawnerConfig.parseConfiguration("{}");
		for (var kind : SpawnerKind.values()) assertEquals(AllySpawnerConfig.defaults(kind), config.forKind(kind));
		assertEquals(AllySpawnerConfig.defaults(), config.forKind(null));
	}
	private static AllySpawnerConfig.Settings differentDefaults(SpawnerKind kind) {
		return switch (kind) {
			case ZOMBIE_ALLY -> new AllySpawnerConfig.Settings(90, 3, 6, 12, 10);
			case SKELETON_ALLY -> new AllySpawnerConfig.Settings(120, 3, 6, 12, 14);
			case SPIDER_ALLY -> new AllySpawnerConfig.Settings(60, 2, 8, 14, 10);
			case CREEPER_ALLY -> new AllySpawnerConfig.Settings(180, 3, 6, 12, 10);
		};
	}
	@Test void absentEntriesUseEachSpeciesCodeDefaultsNotOneGlobalRecord() {
		for (String json : new String[]{"{}", "{\"defaults\":{},\"spawners\":{}}",
				"{\"spawners\":{\"skeleton_ally_spawner\":{}}}"}) {
			var config = AllySpawnerConfig.parseConfiguration(json, AllySpawnerConfigTest::differentDefaults);
			for (var kind : SpawnerKind.values()) assertEquals(differentDefaults(kind), config.forKind(kind));
		}
	}
	@Test void precedenceIsSpeciesCodeThenDeclaredSharedThenDeclaredSpeciesJson() {
		var config = AllySpawnerConfig.parseConfiguration("""
			{"defaults":{"detectionRadius":18},
			 "spawners":{"skeleton_ally_spawner":{"respawnDelaySeconds":30}}}
			""", AllySpawnerConfigTest::differentDefaults);
		assertEquals(new AllySpawnerConfig.Settings(30, 3, 6, 12, 18), config.forKind(SpawnerKind.SKELETON_ALLY));
		assertEquals(new AllySpawnerConfig.Settings(60, 2, 8, 14, 18), config.forKind(SpawnerKind.SPIDER_ALLY));
		assertEquals(180, config.forKind(SpawnerKind.CREEPER_ALLY).respawnDelaySeconds());
	}
	@Test void explicitLegacyFieldsOverrideCodeButAbsentFieldsStaySpeciesSpecific() {
		var config = AllySpawnerConfig.parseConfiguration("""
			{"respawnDelaySeconds":100,"spawners":{"creeper_ally_spawner":{"detectionRadius":7}}}
			""", AllySpawnerConfigTest::differentDefaults);
		assertEquals(new AllySpawnerConfig.Settings(100, 2, 8, 14, 10), config.forKind(SpawnerKind.SPIDER_ALLY));
		assertEquals(new AllySpawnerConfig.Settings(100, 3, 6, 12, 14), config.forKind(SpawnerKind.SKELETON_ALLY));
		assertEquals(7, config.forKind(SpawnerKind.CREEPER_ALLY).detectionRadius());
	}
	@Test void validateOnlyAfterCombiningSharedAndSpeciesDeclarations() {
		var config = AllySpawnerConfig.parseConfiguration("""
			{"defaults":{"recallRadius":6},"spawners":{"spider_ally_spawner":{"roamRadius":4}}}
			""", AllySpawnerConfigTest::differentDefaults);
		assertEquals(4, config.forKind(SpawnerKind.SPIDER_ALLY).roamRadius());
		assertEquals(6, config.forKind(SpawnerKind.SPIDER_ALLY).recallRadius());
	}
	@Test void sparseOverridesInheritEachUndeclaredValue() {
		var config = AllySpawnerConfig.parseConfiguration("""
			{"defaults":{"respawnDelaySeconds":120,"detectionRadius":14},
			 "spawners":{"skeleton_ally_spawner":{"respawnDelaySeconds":40},
			             "creeper_ally_spawner":{"detectionRadius":8}}}
			""");
		assertEquals(40, config.forKind(SpawnerKind.SKELETON_ALLY).respawnDelaySeconds());
		assertEquals(14, config.forKind(SpawnerKind.SKELETON_ALLY).detectionRadius());
		assertEquals(120, config.forKind(SpawnerKind.CREEPER_ALLY).respawnDelaySeconds());
		assertEquals(8, config.forKind(SpawnerKind.CREEPER_ALLY).detectionRadius());
		assertEquals(config.defaults(), config.forKind(SpawnerKind.ZOMBIE_ALLY));
		assertEquals(config.defaults(), config.forKind(SpawnerKind.SPIDER_ALLY));
		assertEquals(3, config.forKind(SpawnerKind.CREEPER_ALLY).spawnRadius());
	}
	@Test void legacyFlatSettingsRemainSharedAndCanHaveOverrides() {
		var config = AllySpawnerConfig.parseConfiguration("""
			{"respawnDelaySeconds":123,"spawnRadius":4,"roamRadius":9,
			 "spawners":{"zombie_ally_spawner":{"respawnDelaySeconds":30}}}
			""");
		assertEquals(123, config.forKind(SpawnerKind.SPIDER_ALLY).respawnDelaySeconds());
		assertEquals(30, config.forKind(SpawnerKind.ZOMBIE_ALLY).respawnDelaySeconds());
		assertEquals(4, config.forKind(SpawnerKind.ZOMBIE_ALLY).spawnRadius());
		assertEquals(9, config.forKind(SpawnerKind.ZOMBIE_ALLY).roamRadius());
		assertEquals(12, config.forKind(SpawnerKind.ZOMBIE_ALLY).recallRadius());
	}
	@Test void allFiveSettingsCanBeOverriddenAndNamespacedKeysWork() {
		var config = AllySpawnerConfig.parseConfiguration("""
			{"spawners":{"mobdefenders:spider_ally_spawner":{
			 "respawnDelaySeconds":20,"spawnRadius":2,"roamRadius":4,"recallRadius":8,"detectionRadius":7}}}
			""");
		assertEquals(new AllySpawnerConfig.Settings(20, 2, 4, 8, 7), config.forKind(SpawnerKind.SPIDER_ALLY));
		assertEquals(200, config.forKind(SpawnerKind.SPIDER_ALLY).initialDelayTicks());
		assertEquals(400, config.forKind(SpawnerKind.SPIDER_ALLY).delayTicks());
		assertThrows(UnsupportedOperationException.class,
			() -> config.spawners().put(SpawnerKind.CREEPER_ALLY, AllySpawnerConfig.defaults()));
	}
	@Test void omittedRecallExpandsForLargerPatrolLikeLegacyConfigs() {
		var config = AllySpawnerConfig.parseConfiguration("""
			{"spawners":{"zombie_ally_spawner":{"roamRadius":24}}}
			""");
		assertEquals(24, config.forKind(SpawnerKind.ZOMBIE_ALLY).recallRadius());
		assertEquals(12, config.forKind(SpawnerKind.SKELETON_ALLY).recallRadius());
	}
	@Test void invalidOverridesDoNotSilentlyPassValidation() {
		for (String value : new String[]{"0", "86401", "1.5", "\"30\"", "null", "true"})
			assertThrows(RuntimeException.class, () -> AllySpawnerConfig.parseConfiguration(
				"{\"spawners\":{\"creeper_ally_spawner\":{\"respawnDelaySeconds\":" + value + "}}}"));
		assertThrows(RuntimeException.class, () -> AllySpawnerConfig.parseConfiguration(
			"{\"spawners\":{\"spider_ally_spawner\":{\"recallRadius\":2}}}"));
		assertThrows(RuntimeException.class, () -> AllySpawnerConfig.parseConfiguration(
			"{\"spawners\":{\"spider_ally_spawner\":{\"spawnRadius\":7}}}"));
		assertThrows(RuntimeException.class, () -> AllySpawnerConfig.parseConfiguration(
			"{\"spawners\":{\"unknown_spawner\":{}}}"));
		assertThrows(RuntimeException.class, () -> AllySpawnerConfig.parseConfiguration(
			"{\"spawners\":{\"spider_ally_spawner\":{},\"mobdefenders:spider_ally_spawner\":{}}}"));
	}
	@Test void overrideOnlyAndEmptyOverrideFilesNeedNoDefaultDeclarations() {
		var config = AllySpawnerConfig.parseConfiguration("""
			{"spawners":{"creeper_ally_spawner":{"respawnDelaySeconds":20},"spider_ally_spawner":{}}}
			""");
		assertEquals(20, config.forKind(SpawnerKind.CREEPER_ALLY).respawnDelaySeconds());
		assertEquals(AllySpawnerConfig.defaults(), config.forKind(SpawnerKind.SPIDER_ALLY));
		assertEquals(AllySpawnerConfig.defaults(), AllySpawnerConfig.parseConfiguration("{\"defaults\":{}}").defaults());
	}
	@Test void defaultsAndInitialDelay() {
		var settings = AllySpawnerConfig.defaults();
		assertEquals(1800, settings.delayTicks());
		assertEquals(900, settings.initialDelayTicks());
		assertEquals(900, new AllySpawnerConfig.Settings(91, 3, 6).initialDelayTicks());
		assertEquals(12, settings.recallRadius());
		assertEquals(10, settings.detectionRadius());
	}
	@Test void validatesBounds() {
		assertThrows(IllegalArgumentException.class, () -> new AllySpawnerConfig.Settings(0, 3, 6));
		assertThrows(IllegalArgumentException.class, () -> new AllySpawnerConfig.Settings(86401, 3, 6));
		assertThrows(IllegalArgumentException.class, () -> new AllySpawnerConfig.Settings(180, 0, 6));
		assertThrows(IllegalArgumentException.class, () -> new AllySpawnerConfig.Settings(180, 33, 40));
		assertThrows(IllegalArgumentException.class, () -> new AllySpawnerConfig.Settings(180, 7, 6));
		assertThrows(IllegalArgumentException.class, () -> new AllySpawnerConfig.Settings(180, 3, 65));
		assertThrows(IllegalArgumentException.class, () -> new AllySpawnerConfig.Settings(90, 3, 6, 5));
		assertThrows(IllegalArgumentException.class, () -> new AllySpawnerConfig.Settings(90, 3, 6, 129));
		assertThrows(IllegalArgumentException.class, () -> new AllySpawnerConfig.Settings(90, 3, 6, 12, 0));
		assertThrows(IllegalArgumentException.class, () -> new AllySpawnerConfig.Settings(90, 3, 6, 12, 65));
	}
	@Test void gsonCannotBypassValidation() {
		assertThrows(RuntimeException.class, () -> new Gson().fromJson(
			"{\"respawnDelaySeconds\":180,\"spawnRadius\":3}", AllySpawnerConfig.Settings.class));
	}
	@Test void spawnCapIsNotASetting() {
		assertTrue(java.util.Arrays.stream(AllySpawnerConfig.Settings.class.getRecordComponents())
			.noneMatch(field -> field.getName().toLowerCase().contains("limit") || field.getName().toLowerCase().contains("max")));
	}
	@Test void olderConfigsGainRecallWithoutLosingCustomValues() {
		var settings = AllySpawnerConfig.parse("{\"respawnDelaySeconds\":123,\"spawnRadius\":4,\"roamRadius\":9}");
		assertEquals(123, settings.respawnDelaySeconds());
		assertEquals(4, settings.spawnRadius());
		assertEquals(9, settings.roamRadius());
		assertEquals(12, settings.recallRadius());
		assertEquals(10, settings.detectionRadius());
		assertEquals(24, AllySpawnerConfig.parse(
			"{\"respawnDelaySeconds\":90,\"spawnRadius\":4,\"roamRadius\":24}").recallRadius());
		assertEquals(16, AllySpawnerConfig.parse(
			"{\"respawnDelaySeconds\":90,\"spawnRadius\":4,\"roamRadius\":6,\"recallRadius\":16}").recallRadius());
	}
	@Test void detectionSettingIsIndependentAndOlderFilesKeepTheirValues() {
		var settings = AllySpawnerConfig.parse(
			"{\"respawnDelaySeconds\":123,\"spawnRadius\":4,\"roamRadius\":9,\"recallRadius\":16,\"detectionRadius\":20}");
		assertEquals(20, settings.detectionRadius());
		assertEquals(9, settings.roamRadius());
		assertEquals(16, settings.recallRadius());
		assertEquals(123, settings.respawnDelaySeconds());
		assertEquals(10, AllySpawnerConfig.parse(
			"{\"respawnDelaySeconds\":90,\"spawnRadius\":3,\"roamRadius\":6,\"recallRadius\":12}").detectionRadius());
	}
}
