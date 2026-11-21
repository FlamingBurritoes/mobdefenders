# Mob Defenders

Player-friendly mob guards for Fabric Minecraft 26.1.2, including Wither Skeleton Allies that inflict Wither on enemies. Craft and place an Ally Spawner, then right-click to select **Standing** or **Wander**. Each source has configurable spawn timers and safe emergency recall. Slime and Magma Cube Spawners track up to eight and four split family members, respectively; other spawners track one ally. Drowned Spawners work underwater, and Blaze and Ghast Allies can patrol up to seven blocks above their spawner.

See [MOD_FILES.md](MOD_FILES.md) for gameplay rules, configuration, recipes, file inventory, supplied-model editing instructions and the ongoing change log. Uses the supplied art with vanilla entity animation. Defenders are sunlight-safe and fight monsters other than Creepers; skeleton arrows avoid friendly fire. All allies pause 3–5 seconds between patrol walks.

## Setup

See [SHARED_SPAWNER_RULES.md](SHARED_SPAWNER_RULES.md) for the compiled common spawner/guard requirements. Creeper Ally explosions damage eligible monsters (including spiders and piglins), without breaking blocks or harming/knocking back players, defenders, passive mobs or iron golems.

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

Requires Java 25+, Fabric Loader, Fabric API and Fabric Language Kotlin. No PvZ/Plantz dependencies.

Edit `defaults(SpawnerKind kind)` in `src/main/java/net/amemah/mobdefenders/config/AllySpawnerConfig.java` for each species' built-in values. `config/mobdefenders.json` optionally overrides them through shared `defaults` and per-type `spawners` fields. Explicit JSON values take precedence; omitted values follow the species' code defaults. Existing flat config files remain compatible. See `MOD_FILES.md` for examples.

Build: `.\gradlew.bat build` (includes unit tests and dedicated-server GameTests).

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
