# Mob Defenders

Player-friendly zombie, skeleton and spider guards for Fabric Minecraft 26.1.2. Craft and place an Ally Spawner, then right-click to select **Standing** or **Wander**. Each source maintains one ally, with a 45-second initial wait, 90-second replacement timer and safe 12-block recall.

See [MOD_FILES.md](MOD_FILES.md) for gameplay rules, configuration, recipes, file inventory, supplied-model editing instructions and the ongoing change log. Uses the supplied art with vanilla entity animation. Defenders are sunlight-safe and fight monsters other than Creepers; skeleton arrows avoid friendly fire. All allies pause 3–5 seconds between patrol walks.

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

Requires Java 25+, Fabric Loader, Fabric API and Fabric Language Kotlin. No PvZ/Plantz dependencies.

Build: `.\gradlew.bat build` (includes unit tests and dedicated-server GameTests).

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
