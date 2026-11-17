package net.amemah.mobdefenders.client;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class SpiderSpawnerResourcesTest {
	@Test void facingVariantsRotateTheSpiderSpawnerModel() throws Exception {
		try (var stream = getClass().getResourceAsStream("/assets/mobdefenders/blockstates/spider_ally_spawner.json")) {
			assertNotNull(stream);
			var variants = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
				.getAsJsonObject().getAsJsonObject("variants");
			assertEquals(4, variants.size());
			String[] directions = {"north", "east", "south", "west"};
			for (int i = 0; i < directions.length; i++) {
				var variant = variants.getAsJsonObject("facing=" + directions[i]);
				assertNotNull(variant);
				assertEquals("mobdefenders:block/spider_ally_spawner", variant.get("model").getAsString());
				assertEquals(i * 90, variant.has("y") ? variant.get("y").getAsInt() : 0);
			}
		}
	}
}
