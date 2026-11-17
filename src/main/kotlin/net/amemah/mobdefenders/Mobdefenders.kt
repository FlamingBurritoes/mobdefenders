package net.amemah.mobdefenders

import net.fabricmc.api.ModInitializer
import net.amemah.mobdefenders.spawner.ZombieSpawnerManager
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object Mobdefenders : ModInitializer {
	const val MOD_ID: String = "mobdefenders"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		ModContent.initialize()
		ZombieSpawnerManager.initialize()
		LOGGER.info("Mob Defenders initialized")
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
