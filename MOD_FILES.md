# Mob Defenders — project and change guide

Keep this document updated for every project task: describe the behavior, added/edited files, assets still needed, tests actually run and remaining checks. Add dated entries to the change history.

## Project

- Namespace: `mobdefenders`; Java/Kotlin package: `net.amemah.mobdefenders`.
- Minecraft **26.1.2**, Fabric Loader **0.20.1+**, Fabric API, Fabric Language Kotlin and **Java 25+**.
- Independent mod; does **not** require PvZ Spawner, Plantz or Pea Extension.
- Existing Kotlin entrypoints remain; gameplay and client systems are Java.

## Zombie Ally and Zombie Spawner

### Ally

- Entity ID: `mobdefenders:zombie_ally`. No biome/natural-spawn registration or spawn egg. The normal gameplay source is a placed Zombie Spawner; administrative commands can still summon it.
- Adult baseline: **20 health, 3 attack damage, 2 armor, 0.23 movement speed**, with vanilla zombie follow range, melee AI, walking/attack animation, sounds and undead traits.
- Acquires living hostile `Enemy` mobs, excluding Creepers and other Zombie Allies. Never deliberately targets players, villagers or passive mobs, even in retaliation. Modded hostiles implementing vanilla's `Enemy` interface are eligible.
- Persists instead of distance-despawning; does not prevent player sleep. Vanilla golems are prevented from targeting this friendly zombie by a narrowly scoped mixin.
- **Sunlight cannot ignite or damage defenders or wear out their helmets.** Every current/future defender implements `entity.Defender`; the shared sunlight mixin skips vanilla `Mob.burnUndead`, even if a datapack adds the entity to `minecraft:burn_in_daylight`. The Zombie Ally also disables its zombie-specific sun-sensitivity flag. Ordinary fire/lava damage, undead vulnerabilities and zombie melee behavior remain. Water conversion is disabled so it cannot become a hostile Drowned. Spawning deliberately skips vanilla `finalizeSpawn`: no baby/jockey/leader rolls or randomized equipment. Reinforcement chance is zero.
- Uses the supplied **`zombie_ally.bbmodel` geometry and `zombie_ally.png` texture**, translated into a native model layer while retaining vanilla zombie animation, equipment and held-item rendering. No OptiFine or animation-library dependency is required.

### Spawner

- Block/item ID: `mobdefenders:zombie_spawner`; available in Functional Blocks.
- Hard-coded **one owned ally per source**, with no configurable cap. Overlapping sources have separate UUID ownership.
- Default full replacement delay: **90 seconds / 1,800 world ticks**, after confirmed death or permanent discard.
- Initial placement waits half the configured delay, floored to whole seconds: **45 seconds / 900 ticks** by default. These are half the original timers. Breaking/replacing starts a fresh initial wait. Spawn attempts run once per second, so an attempt may occur up to one second after its deadline.
- Unloading a chunk/server shutdown is not death. Sources, live UUIDs, mode and deadlines are persisted in dimension SavedData. A missing loaded entity never frees the slot, and nothing force-loads chunks.
- Right-click opens a server-synchronized menu: **Guard Type:** above one button showing **Standing** or **Wander**. Clicking toggles the current source mode. No inventory, item input or extra buttons. Closing/reopening or restarting retains the selection.
- **Standing** is the default: spawn centered on the block's top, stay there when idle, engage eligible nearby hostiles, then return to that top position after combat. Navigation is preferred; a collision-checked return teleport handles an unreachable platform or a path stalled for five seconds. Obstructed top prevents initial Standing spawning; idle return waits for a safe top.
- **Wander**: random safe spawn around the source, then actively pathfinds between randomly chosen reachable destinations around that block while idle. A source-centered movement goal replaces vanilla random strolling, retries at one-second intervals between paths and validates dry supported destinations/path nodes inside the roam range. Switching a living ally back to Standing stops roaming and returns it to its post.
- Default spawn radius **3**, roam/engagement radius **6**. Circular horizontal range plus vertical `+/- roamRadius`, measured from the source's block Y and horizontal center. Nearby spawn ground is searched within three blocks up/down. Spawn/return needs solid supported dry ground, available chunks, world-border clearance, no collision and no overlapping living entity.
- The roam range is an **AI patrol/engagement range, not an invisible physics wall**. Knockback/external movement can take the ally outside it. A separate **12-block recall radius** by default (same circular horizontal plus vertical +/- interpretation) teleports a stray back onto the source block in either mode. Between six and twelve blocks, Wander can walk back naturally instead of snapping at the patrol edge. Beyond the recall radius, the block's top is preferred; if obstructed/occupied, a safe supported position in the roam range is used instead. No safe position means retry later without loading chunks or teleporting into solids. Owned allies still cannot use portals/change dimensions.
- Destroying a source clears ownership first and kills its linked ally normally, retaining death animation/sounds. Unloaded allies enter a saved pending-death queue and die on next load. No other source's ally is affected. Source-break normal death uses Minecraft's ordinary death rules; no custom entity loot table is currently supplied.
- Placement faces away from the player, as in the current Weed Spawner; rotations retain ownership/mode/timer. Full-cube visual/selection/collision. Dirt-like mining/shovel efficiency and self-drop.
- Successful spawn emits vanilla smoke and spawn animation. Failed attempts do not.

### Copper Reinforced Planks

- Block/item ID: `mobdefenders:copper_reinforced_planks`; Building Blocks tab.
- Ordinary full-cube building/crafting block with oak-plank mining properties/axe efficiency and self-drop. No special effects.
- Uses the supplied Copper Reinforced Planks full-cube model/texture. Zombie Spawner likewise uses its supplied full-cube model/texture, replacing all initial placeholder visuals.

### Recipes

Each recipe outputs **one** block.

Zombie Spawner (`F` = Rotten Flesh; `C` = Copper Reinforced Planks):

```text
FFF
FCF
FFF
```

Copper Reinforced Planks (`C` = Copper Ingot; `W` = Oak Planks):

```text
CCC
CWC
CCC
```

### Server configuration

Created at `config/mobdefenders.json` on server/world startup:

```json
{
  "respawnDelaySeconds": 90,
  "spawnRadius": 3,
  "roamRadius": 6,
  "recallRadius": 12
}
```

Edit with the server stopped and restart. These are server-authoritative rules, not multiplayer client preferences. Delay: `1..86400` seconds; spawn radius: `1..32`; roam radius: `spawnRadius..64`; recall radius: `roamRadius..128`. Invalid/missing required fields or malformed JSON log an error and use defaults without overwriting the bad file. Older configs without `recallRadius` receive an in-memory default of `max(12, roamRadius)` without overwriting their custom settings. There is intentionally no spawn-limit setting. Existing saved deadlines are preserved; changed delays apply to future placements/deaths. Time is world ticks, not wall-clock time.

The project's existing `run/config/mobdefenders.json` and generated GameTest config were changed from 180 to 90 seconds and given `recallRadius: 12`. Already-installed external game/server configs are not silently overwritten: change their `respawnDelaySeconds` to 90 (or halve a custom value) to use the shorter timer.

## Supplied assets and future editing

Source art is in `../Useful Vanilla Textures/mobdefenders/` (outside this mod project). The three supplied `.bbmodel` files and original PNGs are preserved there unchanged:

| Editable source | Runtime implementation |
| --- | --- |
| `zombie_ally.bbmodel` | `client/ZombieAllyModel.java` and `textures/entity/zombie_ally.png` |
| `zombie_spawner.bbmodel` | `models/block/zombie_spawner.json` and `textures/block/zombie_spawner.png` |
| `copper_reinorced_plank.bbmodel` (original filename typo retained) | `models/block/copper_reinforced_planks.json` and `textures/block/copper_reinforced_plank.png` |

- The zombie is an **OptiFine CEM-format** Blockbench project, but its seven cubes and standard pivots need no OptiFine runtime. Native conversion uses `(-x, 24-y, z)` coordinates, a 64x64 texture, original box-UV offsets and mirrored left arm/leg regions. It preserves the exact supplied leg bounds around the +/-1.9 pivots, rather than silently substituting vanilla's slightly shifted bounds.
- `headwear` becomes a `hat` child of `head` for the native HumanoidModel hierarchy, retaining its 0.5-unit inflation and head animation. Body/limbs retain the vanilla part names. There are no separate animation tracks to import.
- Both block projects contain an unrotated 0..16 full cube with identical 0..16 UVs on all six faces. Runtime JSON retains those elements/face UVs and uses valid `mobdefenders:block/...` texture identifiers plus a particle texture. A standard `minecraft:block/block` parent supplies normal item display transforms. Existing spawner item GUI lighting is retained.
- The block projects' editor target is labeled 26.3, but the actual geometry uses ordinary full-cube JSON supported by this project's 26.1.2 target.
- All three external PNGs matched their corresponding embedded `.bbmodel` textures byte-for-byte. Runtime PNGs are copied unchanged: no resizing, repainting or UV relocation.
- Registry IDs remain `copper_reinforced_planks` and `zombie_spawner`; no recipe/save migration is needed. The singular supplied plank texture filename is intentional.
- No assets are missing for these three additions. Other art in the same folder (skeleton/enderman/spider/template) is outside this task and has not been imported.

To update textures, replace the corresponding PNG under `src/main/resources/assets/mobdefenders/textures/`, rebuild and install the new JAR. Geometry edits in `.bbmodel` require updating the native entity model layer or exported block JSON as appropriate; `.bbmodel` files are not runtime Minecraft models. Keep original UVs/dimensions unless deliberately changing the model. Client visual checks remain necessary.

## File inventory

Paths are relative to this project.

### Added gameplay/client files

Under `src/main/java/net/amemah/mobdefenders/`:

| File | Purpose |
| --- | --- |
| `ModContent.java` | Block/item/entity/menu registration, entity attributes and Creative entries. |
| `entity/ZombieAlly.java` | Friendly targeting, vanilla zombie melee/animation compatibility, guard return, wandering and movement/portal restrictions. |
| `entity/Defender.java` | Shared sunlight-safe marker required for all current/future friendly defenders. |
| `block/ZombieSpawnerBlock.java` | Placement/facing, source lifecycle and right-click menu. |
| `config/ZombieSpawnerConfig.java` | Validated server JSON configuration; no cap setting. |
| `spawner/ZombieSpawnerData.java` | One-slot UUID ownership, saved mode/deadlines and unloaded source-break cleanup. |
| `spawner/ZombieSpawnerManager.java` | Server lifecycle events, confirmed deaths, safe spawning, smoke and range recall. |
| `menu/ZombieSpawnerMenu.java` | Server-validated toggle and synchronized mode; no item slots. |
| `client/MobdefendersClient.java` | Client-only screen, supplied zombie model layer and custom-texture renderer registration. |
| `client/ZombieAllyModel.java` | Native supplied geometry, pivots, mirrored limb UVs and headwear hierarchy; compatible with vanilla ZombieModel animation. |
| `client/ZombieAllyRenderer.java` | Uses the supplied model layer/texture through the vanilla zombie renderer, preserving animation and equipment rendering. |
| `client/ZombieSpawnerScreen.java` | Compact Guard Type label and mode button. |
| `mixin/FriendlyGolemMixin.java` | Excludes Zombie Allies from vanilla golem attack/target acquisition without affecting other targets. |
| `mixin/SunSafeDefenderMixin.java` | Skips vanilla sunlight ignition and helmet wear for all marked defenders, without making them fireproof. |

### Added resources

Under `src/main/resources/`:

| Files | Purpose |
| --- | --- |
| `assets/mobdefenders/lang/en_us.json` | Block/entity names and Guard Type/Standing/Wander text. |
| `assets/mobdefenders/blockstates/{zombie_spawner,copper_reinforced_planks}.json` | Placed-block models and spawner facing variants. |
| `assets/mobdefenders/models/block/{zombie_spawner,copper_reinforced_planks}.json` | Supplied full-cube geometry and exact face UVs, with corrected namespaced texture references. |
| `assets/mobdefenders/textures/entity/zombie_ally.png` | Original supplied 64x64 zombie texture, copied unchanged. |
| `assets/mobdefenders/textures/block/{zombie_spawner,copper_reinforced_plank}.png` | Original supplied 16x16 block textures, copied unchanged. |
| `assets/mobdefenders/models/item/{zombie_spawner,copper_reinforced_planks}.json` | Inventory/held models; spawner front GUI lighting. |
| `assets/mobdefenders/items/{zombie_spawner,copper_reinforced_planks}.json` | Modern Minecraft item-model definitions. |
| `data/mobdefenders/recipe/{zombie_spawner,copper_reinforced_planks}.json` | Requested shaped recipes. |
| `data/mobdefenders/loot_table/blocks/{zombie_spawner,copper_reinforced_planks}.json` | Self-drops with explosion-survival conditions. |
| `data/minecraft/tags/block/mineable/{shovel,axe}.json` | Correct efficient tools for the spawner and planks, respectively. |

### Edited existing files

| File | Change |
| --- | --- |
| `src/main/kotlin/net/amemah/mobdefenders/Mobdefenders.kt` | Initializes registrations and spawner manager instead of template greeting. |
| `src/main/resources/fabric.mod.json` | Adds client entrypoint and actual mod description. |
| `src/main/resources/mobdefenders.mixins.json` | Registers friendly-golem and sunlight-safe defender hooks. Existing no-op template mixin is retained. |
| `build.gradle` | Adds JUnit and isolated Fabric server GameTests; retains Kotlin setup. |
| `README.md` | Project overview and link to this guide. |

### Added tests/documentation

| File | Purpose |
| --- | --- |
| `src/test/java/net/amemah/mobdefenders/config/ZombieSpawnerConfigTest.java` | Defaults, half-delay rounding, bounds, Gson validation, nonconfigurable cap and backward-compatible recall settings. |
| `src/test/java/net/amemah/mobdefenders/client/ZombieAllyModelTest.java` | Baked native hierarchy/leg bounds and supplied mirrored-limb UV region checks. |
| `src/gametest/java/net/amemah/mobdefenders/ZombieSpawnerGameTests.java` | Real server tests for lifecycle, targeting/return, mode/range/break cleanup, obstruction, persistence and recipes. |
| `src/gametest/resources/fabric.mod.json` | Development-only test entrypoint; not shipped in release JAR. |
| `src/gametest/resources/data/minecraft/tags/entity_type/burn_in_daylight.json` | Test-only tag forces Zombie Ally through the real vanilla daylight-burning entry point to verify the shared immunity hook; never shipped. |
| `AGENTS.md` | Requires future project work to maintain this guide and preserve supplied art. |
| `MOD_FILES.md` | This ongoing project/file/change guide. |

## Verification

- Initial production compilation/build passed on Java 25.
- Expanded build passed with **4 JUnit tests** and **7 server GameTests** (6 mod tests plus Fabric's test). Coverage includes the real 90-second initial/180-second replacement deadlines, single-entity cap, hostile-only targeting and guard return, actual recipe matching, mode persistence, hard movement/teleport recall, obstructed spawn retries, normal break death and serialized unloaded ownership/pending cleanup.
- Final repeat build also passed, including golem attack/contact-target exclusions. The release `build/libs/mobdefenders-1.0.0.jar` contains the gameplay/client classes and both recipes, and excludes test classes. Python's JSON parser validated all **18** source JSON files; generated server configuration was confirmed as `180 / 3 / 6` with no cap field. `git diff --check` found no whitespace errors (only Git's Windows line-ending notices).
- Windows PowerShell's legacy `ConvertFrom-Json` rejected the valid empty-string variant key in the plain block's blockstate. Validation was repeated with Python's standards-compliant parser; the resource was not incorrectly changed.
- Manual checks still needed: client menu layout/button text sync, supplied entity/block appearance and animation/smoke, full real chunk unload/reload and save/restart cycles, multiplayer interaction, terrain edge cases and modded-hostile compatibility.
- Loom warns that this OneDrive workspace can cause locking/performance problems. No files have been relocated.
- Supplied-art integration: full build passed with **6 JUnit tests** (including two model baking/UV tests) and **7 server GameTests**. All three runtime PNGs match both source PNGs and embedded textures exactly; both exported blocks match source cube bounds and every face UV. All 18 JSON resources parse. Release JAR includes both new client classes and the three exact PNGs, and excludes test classes. In-game visual inspection has not been performed.
- Latest behavior revision: full build passed with **7 JUnit tests and 10 server GameTests** (9 mod tests plus Fabric's). Covers real 45/90-second deadlines, active wandering and live mode switching, unclipped movement beyond patrol range, no recall inside twelve blocks, exact top recall outside it, obstructed recall/retry, sunlight helmet/health protection even with a daylight-burning test tag, and retained ordinary fire damage. All **19** source JSON files parse; release JAR contains the shared protection code and excludes test classes/the test-only tag. Development configuration confirmed as `90 / 3 / 6 / 12`; whitespace checks passed.

## Change history

### 2026-11-17 — Initial defender implementation

- Added Zombie Ally, one-slot Zombie Spawner, persistent Standing/Wander menu, configurable timers/radii, source-bound ownership/range rules and normal source-break death.
- Added ordinary Copper Reinforced Planks and both requested recipes.
- Added documented temporary vanilla visuals while waiting for the user's `.bbmodel` and PNGs.
- Added file inventory, art handoff instructions, future documentation maintenance instructions and isolated automated tests.
- Fixed an idle-guard goal timing issue found by the initial server tests. Guard return now requests per-tick updates and uses its own retry counter, avoiding every-other-tick AI scheduling parity. Tests use explicit padding to isolate terrain and wait for randomized target acquisition rather than assuming a fixed acquisition tick.
- Verification: production build and repeated full automated test builds passed, including follow-up golem protection assertions and stalled-path recovery. Client visuals and complete real unload/restart cycles remain manual follow-ups.

### 2026-11-17 — Supplied model and texture integration

- Inspected the three supplied Blockbench files and their external/embedded textures; preserved all source art unchanged.
- Added `client/ZombieAllyModel.java` and `client/ZombieAllyRenderer.java`; edited `client/MobdefendersClient.java` to register them. Keeps vanilla zombie animation without adding OptiFine/GeckoLib.
- Replaced both placeholder block-model JSONs with supplied cube geometry/UVs and valid runtime texture references. Added the three unchanged PNGs listed in the inventory above.
- Added `src/test/java/net/amemah/mobdefenders/client/ZombieAllyModelTest.java`; refreshed this guide and `README.md` to describe the actual art pipeline rather than pending placeholders.
- Used this Fabric version's `ModelLayerRegistry` API. Model tests verify native ZombieModel hierarchy, exact leg bounds, original UV regions and mirrored front-face U direction; vertex order itself is not a reliable mirror check because the baker reverses it.
- Verification: full build passed with **6 JUnit tests and 7 server GameTests**; source/embedded/runtime texture identity, block geometry/UV parity, JSON validity and release-JAR contents checked. Client appearance still requires an in-game check.

### 2026-11-17 — Sun immunity, active patrol, wider recall and faster spawning

- Added `entity/Defender.java` and `mixin/SunSafeDefenderMixin.java`; updated Zombie Ally, mixin registration and `AGENTS.md` so present/future defenders never burn from sunlight or lose helmet durability to it. Ordinary fire still hurts.
- Replaced Zombie Ally's vanilla random-stroll goal with reachable, source-centered patrol paths. Removed the old hard six-block movement clamp. `spawner/ZombieSpawnerManager.java` now recalls beyond a separate configurable radius (default 12), preferring the top of the spawner in both modes.
- Changed `config/ZombieSpawnerConfig.java` defaults to 90-second replacement / 45-second initial wait, added validated backward-compatible `recallRadius`, and updated the project's existing development/test configs without changing other values. External custom configs and recorded world deadlines remain intact.
- Updated config unit tests and server integration tests for shorter timers, actual walking/live mode switching, free movement beyond patrol range, twelve-block top recall, direct sunlight/helmet protection and ordinary fire damage. Added the test-only daylight tag listed above.
- The no-safe-return test blocks the entire candidate search volume, including natural ground beyond the test floor; removing only that small floor still leaves legitimate fallback destinations and is not a valid all-obstructed scenario.
- Verification: full build passed with **7 JUnit tests and 10 server GameTests**; release contents, test-only tag exclusion, all 19 JSON files, development config and whitespace checked. Client patrol/teleport presentation and unusual terrain still require manual checks.
