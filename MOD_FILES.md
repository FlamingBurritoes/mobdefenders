# Mob Defenders — project and change guide

Keep this document updated for every project task: describe the behavior, added/edited files, assets still needed, tests actually run and remaining checks. Add dated entries to the change history.

## Project

- Namespace: `mobdefenders`; Java/Kotlin package: `net.amemah.mobdefenders`.
- Minecraft **26.1.2**, Fabric Loader **0.20.1+**, Fabric API, Fabric Language Kotlin and **Java 25+**.
- Independent mod; does **not** require PvZ Spawner, Plantz or Pea Extension.
- Existing Kotlin entrypoints remain; gameplay and client systems are Java.

## Zombie Ally and shared ally-spawner rules

### Ally

- Entity ID: `mobdefenders:zombie_ally`. No biome/natural-spawn registration or spawn egg. The normal gameplay source is a placed Zombie Ally Spawner; administrative commands can still summon it.
- Adult baseline: **20 health, 3 attack damage, 2 armor, 0.23 movement speed**, with vanilla zombie follow range, melee AI, walking/attack animation, sounds and undead traits.
- Acquires living hostile `Enemy` mobs, excluding Creepers and **all marked defenders**, including Skeleton Allies. Never deliberately targets players, villagers or passive mobs, even in retaliation. Modded hostiles implementing vanilla's `Enemy` interface are eligible.
- Persists instead of distance-despawning; does not prevent player sleep. Vanilla golems are prevented from targeting this friendly zombie by a narrowly scoped mixin.
- **Sunlight cannot ignite or damage defenders or wear out their helmets.** Every current/future defender implements `entity.Defender`; the shared sunlight mixin skips vanilla `Mob.burnUndead`, even if a datapack adds the entity to `minecraft:burn_in_daylight`. The Zombie Ally also disables its zombie-specific sun-sensitivity flag. Ordinary fire/lava damage, undead vulnerabilities and zombie melee behavior remain. Water conversion is disabled so it cannot become a hostile Drowned. Spawning deliberately skips vanilla `finalizeSpawn`: no baby/jockey/leader rolls or randomized equipment. Reinforcement chance is zero.
- Uses the supplied **`zombie_ally.bbmodel` geometry and `zombie_ally.png` texture**, translated into a native model layer while retaining vanilla zombie animation, equipment and held-item rendering. No OptiFine or animation-library dependency is required.

### Spawner

- Block/item IDs: **`mobdefenders:zombie_ally_spawner`**, **`mobdefenders:skeleton_ally_spawner`**, **`mobdefenders:spider_ally_spawner`** and **`mobdefenders:creeper_ally_spawner`**, all in Functional Blocks. All rules below are shared by the four source kinds. See [SHARED_SPAWNER_RULES.md](SHARED_SPAWNER_RULES.md) for the compiled checklist.
- Registry rename compatibility: old `mobdefenders:zombie_spawner` block/item lookups are redirected to the canonical zombie ID when decoding saves/stacks. New registry keys, models, textures, recipes, loot and translations use `zombie_ally_spawner`. No duplicate old block/item is registered. The old dimension SavedData storage key `mobdefenders:zombie_spawners` is intentionally retained so existing source ownership/deadlines survive; entries now save their kind, defaulting to Zombie Ally for older records.
- Hard-coded **one owned ally per source**, with no configurable cap. Overlapping sources have separate UUID ownership.
- Default full replacement delay: **90 seconds / 1,800 world ticks**, after confirmed death or permanent discard.
- Initial placement waits half the configured delay, floored to whole seconds: **45 seconds / 900 ticks** by default. These are half the original timers. Breaking/replacing starts a fresh initial wait. Spawn attempts run once per second, so an attempt may occur up to one second after its deadline.
- Unloading a chunk/server shutdown is not death. Sources, live UUIDs, mode and deadlines are persisted in dimension SavedData. A missing loaded entity never frees the slot, and nothing force-loads chunks.
- Right-click opens a server-synchronized menu: **Guard Type:** above one button showing **Standing** or **Wander**. Clicking toggles the current source mode. No inventory, item input or extra buttons. Closing/reopening or restarting retains the selection.
- **Standing** is the default: spawn centered on the block's top, stay there when idle, engage eligible nearby hostiles, then **pathfind/walk/jump back** after combat. There is no close-range snap or fixed travel-time teleport. Navigation retries once per second, targets the exact top block (zero path tolerance), discards stale partial paths and allows a bounded detour search without changing detection attributes. Movement of at least 0.25 blocks resets the stuck counter. Only **ten continuous seconds without that progress** permits a collision-checked fallback onto a safe top. Long detours can continue without a time limit while making progress. Final centering uses the movement controller, not teleportation. Obstructed top prevents initial Standing spawning; idle return waits/retries until safe. The separate twelve-block emergency recall remains unchanged.
- **Wander**: random safe spawn around the source, then actively pathfinds between randomly chosen reachable destinations around that block while idle. If combat takes it outside the patrol area, a higher-priority return goal **walks it back inside as soon as it has no target**, then stops and resumes normal wandering. It does not need to stand on the block. This uses the shared walk-first retries/stuck fallback, with a patrol-area destination tolerance rather than Standing's exact-post tolerance. Switching to Standing still returns it to its post.
- Default spawn radius **3**, patrol/roam radius **6**, both measured around the source (circular horizontal plus vertical `+/- roamRadius`). Enemy detection is separately **10 blocks from the ally's current position**, using three-dimensional distance and nearest-eligible selection with **no line-of-sight requirement** for acquisition or retention. Walls do not hide enemies, but still block movement/projectiles; normal combat goals handle reaching or getting a clear shot. Vanilla non-LOS checks such as visibility scaling from invisibility are otherwise unchanged. The detector's scan/retention range is explicitly configured, not inherited from the species' follow attributes. Nearby spawn ground is searched within three blocks up/down. Spawn/return needs solid supported dry ground, available chunks, world-border clearance, no collision and no overlapping living entity.
- The roam range is an **AI patrol/engagement range, not an invisible physics wall**. Knockback/external movement can take the ally outside it. A separate **12-block recall radius** by default (same circular horizontal plus vertical +/- interpretation) teleports a stray back onto the source block in either mode. Between six and twelve blocks, Wander can walk back naturally instead of snapping at the patrol edge. Beyond the recall radius, the block's top is preferred; if obstructed/occupied, a safe supported position in the roam range is used instead. No safe position means retry later without loading chunks or teleporting into solids. Owned allies still cannot use portals/change dimensions.
- Destroying a source clears ownership first and kills its linked ally normally, retaining death animation/sounds. Unloaded allies enter a saved pending-death queue and die on next load. No other source's ally is affected. Source-break normal death uses Minecraft's ordinary death rules; no custom entity loot table is currently supplied.
- Placement faces away from the player, as in the current Weed Spawner; rotations retain ownership/mode/timer. Full-cube visual/selection/collision. Dirt-like mining/shovel efficiency and self-drop.
- Successful spawn emits vanilla smoke and spawn animation. Failed attempts do not.

### Skeleton Ally

- Entity ID: **`mobdefenders:skeleton_ally`**; source: **`mobdefenders:skeleton_ally_spawner`**. No natural spawning or spawn egg.
- Vanilla normal-mode baseline: **20 health, 0 armor, 0.25 movement speed**, normal skeleton follow range, equipped plain bow, native strafing/bow-draw/ranged AI and normal skeleton sounds/animation. Its bow uses the normal **40-tick** attack interval, **1.6** launch speed, **6** inaccuracy and vanilla normal-mode random base-arrow damage distribution, rather than silently becoming Hard-mode strength on a Hard world.
- Uses the same friendly targeting, sunlight/helmet protection, persistent ownership, Standing/Wander goals, safe recall, portal restrictions and sleep friendliness as the zombie. Both defenders exclude each other, and vanilla golems exclude both.
- Defender-owned arrows pass through players, defenders, passive/friendly mobs and Creepers. Ordinary arrows from players/hostile skeletons are unchanged. This protects allies from stray shots, not merely from deliberate target selection.
- Keeps vanilla wolf-avoidance and bow/melee weapon reassessment; no sun-flee/restrict-sun goals are installed because sunlight is harmless. Powder-snow conversion is disabled to prevent becoming a hostile Stray. No randomized spawn equipment or group-spawn hooks; the supplied bow has zero death-drop chance.
- Supplied OptiFine-format `skeleton_ally.bbmodel` is translated to a native 64x32 model layer with thin mirrored limbs, exact +/-2 leg pivots and inflated headwear. Uses vanilla skeleton bow/held-item/armor rendering without an OptiFine/GeckoLib dependency.
- All allies use the same idle patrol pause: **3–5 seconds (60–100 ticks)** after a walk ends. Failed destination searches still retry once per second; combat, Standing return and return into patrol are not delayed.

### Spider Ally

- Entity ID: **`mobdefenders:spider_ally`**; block/item: **`mobdefenders:spider_ally_spawner`**. No natural spawning or spawn egg. Spawner craftable using the recipe below, available in Functional Blocks and with `/give @s mobdefenders:spider_ally_spawner`.
- Vanilla spider baseline: **16 health, 2 attack damage, approximately 0.3 movement speed**, spider sounds, wall-climbing navigation, poison/cobweb resistance and leap/melee combat. Uses ordinary melee AI instead of the vanilla spider attack goal that randomly abandons enemies in daylight; no hostile skeleton jockeys or random Hard-mode potion buffs.
- Implements `Defender` and shares all player/friendly/Creeper exclusions, ten-block no-LOS detection, sunlight protection, Standing/Wander patrol/return, ownership, timers, cap, recall, cleanup, portal and sleep rules. Ordinary fire/lava can still hurt it.
- Uses original `spider_ally.bbmodel` boxes, non-mirrored 64x32 UVs and group pivots, with vanilla resting leg rotations so native spider head/eight-leg animation has the correct splayed stance. No vanilla red-eye overlay covers the supplied texture. Both PNGs are copied byte-for-byte; spawner uses the supplied full cube/all-face texture with north/east/south/west blockstate rotations.

### Creeper Ally

- Entity ID **`mobdefenders:creeper_ally`**, source **`mobdefenders:creeper_ally_spawner`**. No natural spawn or spawn egg. Uses unmodified `Creeper.createAttributes()` (**20 health, 0.25 movement speed**) and normal native 30-tick fuse/radius-3 blast, doubled radius when lightning-charged.
- Keeps vanilla swell/chase/cat-and-ocelot avoidance, falling/manual ignition and charged state. Uses shared persistent friendly targeting and all Standing/Wander rules; native melee goal approaches without adding melee damage.
- A narrowly scoped mixin replaces only the ally's private detonation hook. Real vanilla explosion damage/falloff/occlusion applies to eligible monsters, with **no block destruction, fire, TNT priming or friendly/player/passive/iron-golem damage/knockback**. Vanilla hostile creepers are untouched.
- Uses the same monster filter as every other ally: spiders, piglins, zombified piglins and endermen are eligible for targeting/blast damage. The user's passive-mob protection does not mean excluding vanilla neutral-monster families. Other defenders and hostile Creepers remain excluded.
- Ally detonation suppresses vanilla unfiltered lingering-potion clouds and secondary death-effect hooks so the blast cannot leave a harmful friendly-area effect. Self-detonation permanently discards the ally and starts the normal 90-second replacement delay; no instant respawn.
- Supplied `creeper_ally.bbmodel` has exactly vanilla's six boxes/64x32 UVs/group pivots. The native creeper model and renderer retain walking/head animation, swelling/flashing and charged overlay while using the original ally texture.

### Iron Reinforced Planks

- Block/item **`mobdefenders:iron_reinforced_planks`**, Building Blocks tab, ordinary oak-like building/crafting block with axe efficiency and self-drop, matching Copper Reinforced Planks usage.
- Original `iron_reinforced_plank.bbmodel` full cube/all-face 16x16 UVs and embedded PNG are preserved. Runtime singular texture filename matches the supplied name; registry/recipe use plural `planks`, consistent with Copper Reinforced Planks.

### Copper Reinforced Planks

- Block/item ID: `mobdefenders:copper_reinforced_planks`; Building Blocks tab.
- Ordinary full-cube building/crafting block with oak-plank mining properties/axe efficiency and self-drop. No special effects.
- Uses the supplied Copper Reinforced Planks full-cube model/texture. Both Ally Spawners use their supplied full-cube art; no placeholders remain.

### Recipes

Each recipe outputs **one** block.

Creeper Ally Spawner (`G` = Gunpowder; `I` = Iron Reinforced Planks):

```text
GGG
GIG
GGG
```

Iron Reinforced Planks (`I` = Iron Ingot; `P` = Oak Planks):

```text
III
IPI
III
```

Spider Ally Spawner (`S` = String; `E` = normal Spider Eye; `C` = Copper Reinforced Planks):

```text
SES
SCS
SSS
```

Uses seven String, one Spider Eye and one Copper Reinforced Planks. Fermented Spider Eye and ordinary planks are not substitutes.

Zombie Ally Spawner (`F` = Rotten Flesh; `C` = Copper Reinforced Planks):

```text
FFF
FCF
FFF
```

Skeleton Ally Spawner (`B` = Bone; `A` = **normal Arrow**, not spectral/tipped; `C` = Copper Reinforced Planks):

```text
BAB
ACA
BAB
```

Copper Reinforced Planks (`C` = Copper Ingot; `W` = Oak Planks):

```text
CCC
CWC
CCC
```

### Server configuration

Created at `config/mobdefenders.json` on server/world startup. Settings resolve **per spawner type** (not per individual placed block), field by field: **type-specific code defaults → explicitly declared shared JSON values → explicitly declared type-specific JSON values**. Omitted JSON fields retain that type's code defaults:

```json
{
  "defaults": {
    "respawnDelaySeconds": 90,
    "spawnRadius": 3,
    "roamRadius": 6,
    "recallRadius": 12,
    "detectionRadius": 10
  },
  "spawners": {
    "zombie_ally_spawner": {"respawnDelaySeconds": 60},
    "creeper_ally_spawner": {"respawnDelaySeconds": 120, "detectionRadius": 8}
  }
}
```

Valid override keys: `zombie_ally_spawner`, `skeleton_ally_spawner`, `spider_ally_spawner`, `creeper_ally_spawner` (optionally prefixed `mobdefenders:`). Any/all five settings can be overridden independently. In this example skeleton/spider retain the explicitly shared values, zombie replacement/initial waits are 60/30 seconds, and creeper waits are 120/60 seconds with eight-block detection. Empty shared/type objects do not erase species defaults.

**To change each species' built-in defaults in Java**, edit `defaults(SpawnerKind kind)` in `src/main/java/net/amemah/mobdefenders/config/AllySpawnerConfig.java`. Each switch arm has its own `new Settings(replacementSeconds, spawnRadius, patrolRadius, recallRadius, detectionRadius)`. All four currently retain `90, 3, 6, 12, 10`; changing one arm affects that species wherever JSON does not explicitly override the field. Rebuild/install the JAR and restart. Initial wait remains half the resulting replacement delay.

New config files contain `{"defaults": {}, "spawners": {}}`, so they follow editable per-species code defaults instead of freezing those values into JSON. **Existing explicit JSON values still win**, including fields in older flat files: remove the corresponding shared/type field if you want changes in Java to take effect. Existing configs are not rewritten automatically.

Edit with the server stopped and restart. These are server-authoritative rules, not multiplayer client preferences. Delay: `1..86400` integer seconds; spawn radius: `1..32`; roam radius: `spawnRadius..64`; recall radius: `roamRadius..128`; detection radius: `1..64`, independent of the other radii. Overrides are validated **after inheritance**. An omitted recall radius expands to at least the effective roam radius; other invalid radius combinations must be configured explicitly. Invalid values/types, duplicate/unknown spawner keys or malformed JSON log an error and fall back atomically to built-in defaults for all types, without overwriting the bad file. There is intentionally no spawn-limit setting. Existing saved deadlines are preserved; changed delays apply to future placements/deaths. Time is world ticks, not wall-clock time.

Existing flat configs (the five fields at the JSON root) still work as shared overrides and may also gain a `spawners` object. No migration or overwrite is required. Use either root-level shared fields or the new `defaults` object; when `defaults` exists it supplies shared declarations. The development instance's existing config is intentionally left unchanged. Summoned/unowned defenders resolve the same type-specific detection/patrol settings.

The project's existing `run/config/mobdefenders.json` and generated GameTest config were changed from 180 to 90 seconds and given `recallRadius: 12`. Already-installed external game/server configs are not silently overwritten: change their `respawnDelaySeconds` to 90 (or halve a custom value) to use the shorter timer.

## Supplied assets and future editing

Source art is in `../Useful Vanilla Textures/mobdefenders/` (outside this mod project). All supplied `.bbmodel` files and original PNGs are preserved there unchanged:

| Editable source | Runtime implementation |
| --- | --- |
| `zombie_ally.bbmodel` | `client/ZombieAllyModel.java` and `textures/entity/zombie_ally.png` |
| `zombie_spawner.bbmodel` (original source name retained) | `models/block/zombie_ally_spawner.json` and `textures/block/zombie_ally_spawner.png` |
| `skeleton_ally.bbmodel` | `client/SkeletonAllyModel.java` and `textures/entity/skeleton_ally.png` |
| `skeleton_ally_spawner.bbmodel` | `models/block/skeleton_ally_spawner.json` and `textures/block/skeleton_ally_spawner.png` |
| `spider_ally.bbmodel` | `client/SpiderAllyModel.java` and `textures/entity/spider_ally.png` |
| `spider_ally_spawner.bbmodel` | `models/block/spider_ally_spawner.json` and `textures/block/spider_ally_spawner.png` |
| `creeper ally/creeper_ally.bbmodel` | `client/CreeperAllyModel.java` and `textures/entity/creeper_ally.png` |
| `creeper ally/creeper_spawner_block.bbmodel` | `models/block/creeper_ally_spawner.json` and `textures/block/creeper_ally_spawner.png` |
| `iron_reinforced_plank.bbmodel` | `models/block/iron_reinforced_planks.json` and `textures/block/iron_reinforced_plank.png` |
| `copper_reinorced_plank.bbmodel` (original filename typo retained) | `models/block/copper_reinforced_planks.json` and `textures/block/copper_reinforced_plank.png` |

- The zombie is an **OptiFine CEM-format** Blockbench project, but its seven cubes and standard pivots need no OptiFine runtime. Native conversion uses `(-x, 24-y, z)` coordinates, a 64x64 texture, original box-UV offsets and mirrored left arm/leg regions. It preserves the exact supplied leg bounds around the +/-1.9 pivots, rather than silently substituting vanilla's slightly shifted bounds.
- `headwear` becomes a `hat` child of `head` for the native HumanoidModel hierarchy, retaining its 0.5-unit inflation and head animation. Body/limbs retain the vanilla part names. There are no separate animation tracks to import.
- All three block projects contain an unrotated 0..16 full cube with identical 0..16 UVs on all six faces. Runtime JSON retains those elements/face UVs and uses valid `mobdefenders:block/...` texture identifiers plus a particle texture. A standard `minecraft:block/block` parent supplies normal item display transforms. Existing spawner item GUI lighting is retained.
- The block projects' editor target is labeled 26.3, but the actual geometry uses ordinary full-cube JSON supported by this project's 26.1.2 target.
- All three external PNGs matched their corresponding embedded `.bbmodel` textures byte-for-byte. Runtime PNGs are copied unchanged: no resizing, repainting or UV relocation.
- Canonical block IDs are `copper_reinforced_planks`, `zombie_ally_spawner` and `skeleton_ally_spawner`. Legacy zombie block/item ID decoding is supported as described above. The singular supplied plank texture filename is intentional.
- Skeleton entity PNG and the spawner's referenced texture were extracted byte-for-byte from the supplied models. The spawner project names texture 0 `skeleton_spawner.png`; its runtime copy is renamed `skeleton_ally_spawner.png`. Its second `oak_planks_copper.png` texture is unused by every face and is not imported.
- Spider entity/spawner PNGs are copied byte-for-byte from the supplied external files and also match the embedded textures. Enderman/template art remains outside this task.

To update textures, replace the corresponding PNG under `src/main/resources/assets/mobdefenders/textures/`, rebuild and install the new JAR. Geometry edits in `.bbmodel` require updating the native entity model layer or exported block JSON as appropriate; `.bbmodel` files are not runtime Minecraft models. Keep original UVs/dimensions unless deliberately changing the model. Client visual checks remain necessary.

## File inventory

Paths are relative to this project.

### Added gameplay/client files

Under `src/main/java/net/amemah/mobdefenders/`:

| File | Purpose |
| --- | --- |
| `ModContent.java` | Block/item/entity/menu registration, entity attributes and Creative entries. |
| `entity/ZombieAlly.java` | Vanilla zombie melee, shared friendly targeting and portal/conversion restrictions. |
| `entity/SkeletonAlly.java` | Vanilla skeleton bow/strafe AI with normal-mode projectile stats, shared friendliness and no Stray conversion. |
| `entity/SpiderAlly.java` | Vanilla climbing/leap/melee movement with shared defender rules, no daylight target abandonment or hostile jockeys. |
| `entity/CreeperAlly.java` | Vanilla fuse/chase with terrain-safe blast using the shared monster filter; protects players, defenders, passive mobs and golems. |
| `entity/DefenderBehavior.java` | Shared enemy filtering, guard return, source-centered wandering and 3–5 second patrol pause for every ally. |
| `entity/Defender.java` | Shared sunlight-safe marker required for all current/future friendly defenders. |
| `block/AllySpawnerBlock.java` | Shared placement/facing, source lifecycle/kind and right-click menu; renamed from ZombieSpawnerBlock. |
| `config/AllySpawnerConfig.java` | Shared validated server JSON configuration; no cap setting; renamed from ZombieSpawnerConfig. |
| `spawner/AllySpawnerData.java` | One-slot UUID ownership, saved kind/mode/deadlines and unloaded cleanup; renamed from ZombieSpawnerData, preserving legacy storage key. |
| `spawner/AllySpawnerManager.java` | Shared server lifecycle, confirmed deaths, typed spawning, smoke and recall; renamed from ZombieSpawnerManager. |
| `spawner/SpawnerKind.java` | Canonical block/entity mapping for Zombie, Skeleton, Spider and Creeper Ally sources. |
| `menu/AllySpawnerMenu.java` | Shared server-validated toggle/mode; no item slots; renamed from ZombieSpawnerMenu. |
| `client/MobdefendersClient.java` | Client-only screen, supplied zombie model layer and custom-texture renderer registration. |
| `client/ZombieAllyModel.java` | Native supplied geometry, pivots, mirrored limb UVs and headwear hierarchy; compatible with vanilla ZombieModel animation. |
| `client/ZombieAllyRenderer.java` | Uses the supplied model layer/texture through the vanilla zombie renderer, preserving animation and equipment rendering. |
| `client/SkeletonAllyModel.java` | Native supplied 64x32 skeleton geometry/pivots/mirrored limb UVs/headwear. |
| `client/SkeletonAllyRenderer.java` | Supplied skeleton art with vanilla skeleton animation/bow/armor layers. |
| `client/SpiderAllyModel.java` | Native supplied spider boxes, 64x32 UVs and group pivots for vanilla SpiderModel animation. |
| `client/SpiderAllyRenderer.java` | Supplied spider layer/texture without vanilla hostile-eye overlay. |
| `client/CreeperAllyModel.java` | Native vanilla layer matching all supplied creeper boxes, UVs and pivots. |
| `client/CreeperAllyRenderer.java` | Original ally texture with native walking, fuse swelling/flashing and charged overlay. |
| `client/AllySpawnerScreen.java` | Shared Guard Type label/button; renamed from ZombieSpawnerScreen. |
| `mixin/FriendlyGolemMixin.java` | Excludes all marked defenders from vanilla golem attack/target acquisition. |
| `mixin/SunSafeDefenderMixin.java` | Skips vanilla sunlight ignition and helmet wear for all marked defenders, without making them fireproof. |
| `mixin/FriendlyProjectileMixin.java` | Defender projectiles ignore players, allies, passive mobs and Creepers. |
| `mixin/LegacySpawnerIdMixin.java` | Old zombie block/item ID lookup compatibility without retaining a duplicate old registry entry. |
| `mixin/FriendlyCreeperExplosionMixin.java` | Replaces only ally detonation; preserves ordinary hostile creepers. |

### Added resources

Under `src/main/resources/`:

| Files | Purpose |
| --- | --- |
| `assets/mobdefenders/lang/en_us.json` | Block/entity names and Guard Type/Standing/Wander text. |
| `assets/mobdefenders/blockstates/{zombie_ally_spawner,skeleton_ally_spawner,copper_reinforced_planks}.json` | Placed-block models and spawner facing variants. |
| `assets/mobdefenders/models/block/{zombie_ally_spawner,skeleton_ally_spawner,copper_reinforced_planks}.json` | Supplied full-cube geometry and exact face UVs, with namespaced texture references. |
| `assets/mobdefenders/textures/entity/zombie_ally.png` | Original supplied 64x64 zombie texture, copied unchanged. |
| `assets/mobdefenders/textures/entity/skeleton_ally.png` | Original supplied 64x32 skeleton texture, copied unchanged. |
| `assets/mobdefenders/textures/block/{zombie_ally_spawner,skeleton_ally_spawner,copper_reinforced_plank}.png` | Original supplied 16x16 textures, unchanged except spawner runtime filenames. |
| `assets/mobdefenders/models/item/{zombie_ally_spawner,skeleton_ally_spawner,copper_reinforced_planks}.json` | Inventory/held models; spawner front GUI lighting. |
| `assets/mobdefenders/items/{zombie_ally_spawner,skeleton_ally_spawner,copper_reinforced_planks}.json` | Modern Minecraft item-model definitions. |
| `data/mobdefenders/recipe/{zombie_ally_spawner,skeleton_ally_spawner,copper_reinforced_planks}.json` | Requested shaped recipes. |
| `data/mobdefenders/loot_table/blocks/{zombie_ally_spawner,skeleton_ally_spawner,copper_reinforced_planks}.json` | Self-drops with explosion-survival conditions. |
| `data/minecraft/tags/block/mineable/{shovel,axe}.json` | Correct efficient tools for the spawner and planks, respectively. |
| `assets/mobdefenders/{blockstates,models/block,items}/spider_ally_spawner.json` | Spider full-cube block/item model and placed-block mapping. |
| `assets/mobdefenders/textures/{entity/spider_ally,block/spider_ally_spawner}.png` | Unchanged supplied spider entity/spawner textures. |
| `data/mobdefenders/loot_table/blocks/spider_ally_spawner.json` | Spider spawner self-drop. |
| `data/mobdefenders/recipe/spider_ally_spawner.json` | Requested SES/SCS/SSS shaped recipe, producing one spawner. |
| `assets/mobdefenders/{blockstates,models/block,items}/{creeper_ally_spawner,iron_reinforced_planks}.json` | Creeper directional/full-cube models and iron building-block models/item definitions. |
| `assets/mobdefenders/textures/{entity/creeper_ally,block/creeper_ally_spawner,block/iron_reinforced_plank}.png` | Original supplied/embedded textures unchanged. |
| `data/mobdefenders/{recipe,loot_table/blocks}/{creeper_ally_spawner,iron_reinforced_planks}.json` | Exact requested recipes and explosion-survival self-drops. |

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
| `src/test/java/net/amemah/mobdefenders/config/AllySpawnerConfigTest.java` | Shared config tests; renamed from ZombieSpawnerConfigTest. |
| `src/test/java/net/amemah/mobdefenders/client/ZombieAllyModelTest.java` | Baked native hierarchy/leg bounds and supplied mirrored-limb UV region checks. |
| `src/test/java/net/amemah/mobdefenders/client/SpiderAllyModelTest.java` | Supplied leg dimensions/pivots/UVs and vanilla head/leg animation reset. |
| `src/test/java/net/amemah/mobdefenders/client/CreeperAllyModelTest.java` | Supplied creeper boxes/pivots/UVs and native animation parity. |
| `src/gametest/java/net/amemah/mobdefenders/CreeperAllySpawnerGameTests.java` | Fuse/self-discard lifecycle, safe normal/charged blasts, neutral/friendly protection, vanilla regression, patrol return/menu/recall/sun and recipes. |
| `SHARED_SPAWNER_RULES.md` | Compiled common spawner/guard requirements for every species. |
| `src/gametest/java/net/amemah/mobdefenders/config/AllySpawnerConfigGameTests.java` | Per-kind initial/replacement deadlines, preserved save data and per-species detection/fallback with temporary synchronous overrides. |
| `src/gametest/java/net/amemah/mobdefenders/SpiderAllySpawnerGameTests.java` | Spider timers/cap/save/cleanup, friendliness/daylight combat, patrol/recall/Standing and skeleton pause regression. |
| `src/test/java/net/amemah/mobdefenders/client/SkeletonAllyModelTest.java` | Native skeleton hierarchy, thin-limb bounds/pivots and source UV-region checks. |
| `src/gametest/java/net/amemah/mobdefenders/ZombieAllySpawnerGameTests.java` | Existing zombie regressions; renamed from ZombieSpawnerGameTests. |
| `src/gametest/java/net/amemah/mobdefenders/SkeletonAllySpawnerGameTests.java` | Skeleton lifecycle, mixed ownership, real bow combat, friendly projectiles, menu/patrol/recall, sunlight, recipes and legacy ID/save decoding. |
| `src/gametest/java/net/amemah/mobdefenders/GuardReturnGameTests.java` | Both allies' obstacle-detour walking, delayed unreachable-post fallback and walk-back/resumed wandering after combat outside patrol. |
| `src/gametest/java/net/amemah/mobdefenders/DefenderDetectionGameTests.java` | Both species' ten-block boundaries, ally-vs-source distance checks, real acquisition outside the old patrol circle and through-wall acquisition/retention without through-wall damage. |
| `src/gametest/resources/fabric.mod.json` | Development-only test entrypoint; not shipped in release JAR. |
| `src/gametest/resources/data/minecraft/tags/entity_type/burn_in_daylight.json` | Test-only tag forces both allies through the real vanilla sunlight entry point; never shipped. |
| `AGENTS.md` | Requires future project work to maintain this guide and preserve supplied art. |
| `MOD_FILES.md` | This ongoing project/file/change guide. |
| `DETECTION_REVIEW.md` | Version-checked Peashooter/Weed acquisition, visibility, wild/tamed filters, owner/retaliation paths, pursuit rules and comparison with current defenders. |

## Verification

- Initial production compilation/build passed on Java 25.
- Expanded build passed with **4 JUnit tests** and **7 server GameTests** (6 mod tests plus Fabric's test). Coverage includes the real 90-second initial/180-second replacement deadlines, single-entity cap, hostile-only targeting and guard return, actual recipe matching, mode persistence, hard movement/teleport recall, obstructed spawn retries, normal break death and serialized unloaded ownership/pending cleanup.
- Final repeat build also passed, including golem attack/contact-target exclusions. The release `build/libs/mobdefenders-1.0.0.jar` contains the gameplay/client classes and both recipes, and excludes test classes. Python's JSON parser validated all **18** source JSON files; generated server configuration was confirmed as `180 / 3 / 6` with no cap field. `git diff --check` found no whitespace errors (only Git's Windows line-ending notices).
- Windows PowerShell's legacy `ConvertFrom-Json` rejected the valid empty-string variant key in the plain block's blockstate. Validation was repeated with Python's standards-compliant parser; the resource was not incorrectly changed.
- Manual checks still needed: client menu layout/button text sync, supplied entity/block appearance and animation/smoke, full real chunk unload/reload and save/restart cycles, multiplayer interaction, terrain edge cases and modded-hostile compatibility.
- Loom warns that this OneDrive workspace can cause locking/performance problems. No files have been relocated.
- Supplied-art integration: full build passed with **6 JUnit tests** (including two model baking/UV tests) and **7 server GameTests**. All three runtime PNGs match both source PNGs and embedded textures exactly; both exported blocks match source cube bounds and every face UV. All 18 JSON resources parse. Release JAR includes both new client classes and the three exact PNGs, and excludes test classes. In-game visual inspection has not been performed.
- Latest behavior revision: full build passed with **7 JUnit tests and 10 server GameTests** (9 mod tests plus Fabric's). Covers real 45/90-second deadlines, active wandering and live mode switching, unclipped movement beyond patrol range, no recall inside twelve blocks, exact top recall outside it, obstructed recall/retry, sunlight helmet/health protection even with a daylight-burning test tag, and retained ordinary fire damage. All **19** source JSON files parse; release JAR contains the shared protection code and excludes test classes/the test-only tag. Development configuration confirmed as `90 / 3 / 6 / 12`; whitespace checks passed.
- Skeleton/rename revision: clean build and final repeat build passed with **9 JUnit tests and 17 server GameTests** (16 mod tests plus Fabric's). New checks cover actual ranged AI/arrows, friendly/creeper projectile filtering, 45/90-second skeleton lifecycle, mixed source ownership, kind/deadline serialization, live menu/patrol/standing/recall, sunlight/helmet immunity, pending cleanup, normal-arrow-only recipe, old blockstate/item decoding and pre-kind save compatibility. Replacing one spawner kind with the other kills only the old ally and starts a fresh initial wait.
- All **25** source JSON files parse. Supplied skeleton/zombie spawner textures remain byte-identical to the original/embedded art, and exported block bounds/face UVs match. Release JAR contains canonical ally-spawner assets/classes and both recipes, and excludes obsolete zombie-spawner resources/classes, tests and the test-only daylight tag. Client visuals and a full real-world upgrade/unload/restart cycle still need manual checking.

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

### 2026-11-17 — Explicit ally-spawner IDs and Skeleton Ally

- Renamed the canonical zombie block/item and every runtime resource reference to `zombie_ally_spawner`, including texture filename, recipes, loot, translations and Creative entries. Source `.bbmodel`/PNGs outside this project remain unchanged. Compatibility lookup redirects preserve old blockstates/inventory stacks; old ownership storage is retained.
- Generalized the six former ZombieSpawner block/config/data/manager/menu/screen classes to `AllySpawner*`, renamed their tests, added persisted `SpawnerKind`, and extracted `entity/DefenderBehavior.java`. Both allies now share one implementation of timers, cap, lifecycle, menu, patrol and recall rather than duplicated systems.
- Added `entity/SkeletonAlly.java`, `client/SkeletonAllyModel.java`, `client/SkeletonAllyRenderer.java`, supplied textures, six skeleton-spawner JSON resources and the requested `BAB / ACA / BAB` recipe.
- Extended friendly golem filtering to all defenders and added `mixin/FriendlyProjectileMixin.java` so arrows cannot accidentally hit friendly/passive entities or Creepers. Added `mixin/LegacySpawnerIdMixin.java`; updated initializer, client registration, mixin list, shovel tag, language and documentation.
- Added model tests and skeleton server integration tests, extending the test-only sunlight tag to both allies. Source replacement explicitly guards the source kind, so replacing a skeleton spawner with a zombie spawner cannot leave old ownership behind or remove the newly registered source.
- Verification: clean build plus final full build passed with **9 JUnit tests and 17 server GameTests**. All 25 JSON files, source art identity/UV parity, canonical release resources, test exclusion and whitespace checked. Existing saved ID compatibility is tested through the actual blockstate and ItemStack codecs; complete real-world upgrades/client appearance remain manual follow-ups.

### 2026-11-17 — Walk-first guard return and Plantz targeting review

- Edited `entity/DefenderBehavior.java` to remove the within-1.5-block teleport, per-tick exact-center snap and five-second total-travel timeout. Both allies now pathfind/retry and physically center themselves; a safe fallback requires ten seconds without meaningful movement. The separate twelve-block emergency recall is unchanged.
- Added `GuardReturnGameTests.java` and registered it in the isolated GameTest mod. Tests cover both species walking/jumping around a wall without large per-tick position jumps, and retrying unreachable posts before a delayed safe fallback.
- Reviewed the pinned Plants & Zombies 1.5.1 / Pea Extension 1.2.8 JARs and Minecraft 26.1.2 targeting classes. Added `DETECTION_REVIEW.md` with verified mob-centered ranges (Peashooters 14; Weed wild 6/tamed 16), visibility/nearest selection, owner/retaliation paths, chase thresholds/leashes and recommendations.
- No enemy-detection code, filters, scan rate or range values were changed; awaiting the user's preferred policy after this report. No dependency on the reviewed mods was added, and `pvzspawner` was not edited.
- Test harness fixes: register per-tick samplers before ticking to avoid mutating Minecraft's scheduled-task map during its iteration. Keep the path detour and the existing skeleton bow test's target within the empty structure's enclosed area; entities outside its barriers are not a valid reachable/visible fixture. Updated `SkeletonAllySpawnerGameTests.java` accordingly.
- Verification: full build passed with **9 JUnit tests and 21 server GameTests** (20 mod tests plus Fabric's). Both allies walked around the wall and climbed onto the post with per-tick/arrival checks rejecting teleports; both retried unreachable posts before fallback. Existing spawning, combat, menu, art-model, save-compatibility and emergency-recall regressions passed. Client movement appearance and complex terrain remain manual checks. Dependency detection findings are code-reviewed, not live-tested.

### 2026-11-17 — Ten-block ally-centered detection

- Edited `entity/DefenderBehavior.java` so range is measured from the ally, not the spawner. Both target acquisition/retention and `canAttack` use the same configured radius; default **10 blocks** in three dimensions. Nearest-visible selection, scan timing and friendly/Creeper filtering are unchanged.
- Added independent `detectionRadius` to `config/AllySpawnerConfig.java` (valid 1..64), with a backward-compatible default of 10 for older files. Updated the existing development/GameTest configs without changing timers, patrol or recall settings.
- Extended `AllySpawnerConfigTest.java`; added `DefenderDetectionGameTests.java` and registered it in the isolated test entrypoint. Refreshed this guide and `DETECTION_REVIEW.md` to distinguish the implemented detector from the reviewed old source-centered gate.
- Patrol remains six blocks around the source; emergency recall remains twelve. No new pursuit leash, owner-defense or group-alert behavior was added.
- Verification: full build passed with **10 JUnit tests and 25 server GameTests** (24 mod tests plus Fabric's), including both species' mob-centered boundary and real visibility/acquisition tests. Client movement/targeting presentation remains a manual check.

### 2026-11-17 — No-LOS detection and explicit post-combat patrol return

- Edited `entity/DefenderBehavior.java` to disable both target-goal sight retention and the separate acquisition LOS condition. Range remains ten blocks from the ally; hostile/friendly/Creeper filters and scan timing are unchanged. No projectile phasing or attack-through-wall behavior was added.
- Generalized the shared return goal: Standing returns to the top; Wander activates it only when outside patrol and without a combat target, and stops once back inside. It takes priority over random strolling and uses physical pathfinding, not a teleport at the patrol boundary. Existing stuck fallback and twelve-block emergency recall are preserved.
- Updated `DefenderDetectionGameTests.java` to prove both species acquire/retain enemies behind an opaque wall without damaging them through it. Added two `GuardReturnGameTests.java` scenarios for retaining combat outside patrol, then physically returning and staying in Wander mode after the enemy is removed.
- Updated this guide and `DETECTION_REVIEW.md`; no config, recipe or art changes.
- Verification: full build passed with **10 JUnit tests and 27 server GameTests** (26 mod tests plus Fabric's). Both species acquire and retain targets through opaque walls without through-wall damage in the fixtures, and both physically return from combat outside patrol before resuming Wander. Existing ten-block boundaries, Standing return, stuck fallback, twelve-block emergency recall and lifecycle regressions pass. All 25 source JSON files parse; release contains the new target goal but no test classes; whitespace checks passed. Client presentation remains a manual check.

### 2026-11-17 — Calmer skeleton patrol and Spider Ally

- Edited `entity/DefenderBehavior.java`: Skeleton Ally now takes a randomized 60–100 tick pause after each patrol path stops; other species retain their existing interval. The cooldown affects only wandering, not targeting/combat or higher-priority guard return.
- Added `entity/SpiderAlly.java`, registered entity attributes/block/item/Creative entry in `ModContent.java`, and extended `spawner/SpawnerKind.java` to three explicit kinds. Existing generic manager/menu/persistence implement the same lifecycle without a second spider-specific ownership system. No existing IDs or saved kinds were renamed.
- Added `client/SpiderAllyModel.java` and `SpiderAllyRenderer.java`; registered them in `MobdefendersClient.java`. Preserved supplied model geometry/UVs/group pivots and original PNG bytes; source art remains untouched.
- Added spider spawner blockstate, block model, item definition, loot table and both runtime textures. Edited English translations and shovel tag. Updated `README.md` and this guide. **No spider crafting recipe invented**; Creative/commands remain available pending user recipe.
- Added `SpiderAllyModelTest.java` and `SpiderAllySpawnerGameTests.java` (timers/cap/save/cleanup, stats/climbing navigation, friendly filters, daylight combat, patrol/Standing/recall, skeleton cooldown). Extended `DefenderDetectionGameTests.java` and the test-only daylight tag for spider, and registered the new suite.
- Extended `GuardReturnGameTests.java` with spider post-combat walk-back coverage.
- Verification: full build passed with **12 JUnit tests and 34 server GameTests** (33 mod tests plus Fabric's). Includes spider range/through-wall detection, physical return to patrol after combat, lifecycle, real melee, patrol/recall/Standing and skeleton pause bounds. All 29 source JSON files parse; runtime PNGs match source/embedded bytes; release contains spider classes/assets but excludes tests and the test-only daylight tag. Whitespace checks passed. Client appearance and real wall-climbing terrain remain manual checks.

### 2026-11-17 — Spider spawner recipe and Wander consistency check

- Added `src/main/resources/data/mobdefenders/recipe/spider_ally_spawner.json`: requested `SES / SCS / SSS` recipe, using seven String, one normal Spider Eye and one Copper Reinforced Planks, producing one spawner.
- Extended `SpiderAllySpawnerGameTests.java` with real recipe-manager matching/output checks and rejection of Fermented Spider Eye/ordinary planks. Updated `README.md` and this guide to remove the current missing-recipe restriction; earlier change entries remain historical.
- Checked `entity/DefenderBehavior.java`: all three defenders share the same Wander goal, six-block patrol, no-LOS detection, combat interruption/post-combat return and twelve-block recall rules. Skeleton alone has the requested 3–5 second between-walk pause; zombie/spider retain one second. Species movement speed/navigation/combat still differ. No guard AI changes made during this task.
- Removed an accidentally duplicated spider patrol-return test declaration from `GuardReturnGameTests.java` found by compilation; retained the original test and behavior.
- Verification: initial build caught the duplicate test declaration; after correction the full build passed with **12 JUnit tests and 35 server GameTests**. Actual recipe matching/output and invalid-substitution checks pass. All 30 source JSON files parse; release JAR contains the requested recipe; whitespace checks passed.

### 2026-11-17 — Spider facing/animation fixes and consistent patrol pauses

- Fixed `assets/mobdefenders/blockstates/spider_ally_spawner.json`: added the four horizontal facing variants with 0/90/180/270-degree model rotations, matching the existing spawners. The block already saved its facing; the previous catch-all visual ignored it.
- Fixed `client/SpiderAllyModel.java`: native SpiderModel adds walking motion to the baked resting pose, so flat zero-rotation legs did not animate like vanilla. Restored vanilla yaw/roll resting angles while keeping the supplied boxes, UVs, textures and pivots unchanged. Checked Minecraft 26.1.2 SpiderRenderer: the existing renderer already uses the same render-state extraction path, so no extra animation hook is needed.
- Edited `entity/DefenderBehavior.java` to use randomized 60–100 tick between-walk pauses for **all allies**, not just skeletons. Combat and guard-return goals remain independent of that idle cooldown.
- Extended `SpiderAllyModelTest.java` to compare head/every leg rotation with vanilla over 72 rest/walking frames and verify native resting rotations. Added `SpiderSpawnerResourcesTest.java` for all four model-facing variants. Extended the cooldown check in `SpiderAllySpawnerGameTests.java` to all three species.
- Updated `README.md` and this guide. No source art, recipes, spawn timers, patrol/detection radii or combat rules changed.
- Verification: full build retry with `--no-daemon` passed: **14 JUnit tests and 35 server GameTests**, including vanilla animation-rotation parity, four-way facing variants and all-species patrol cooldowns. All 30 source JSON files parse and whitespace checks pass. Runtime textures match the supplied PNGs now located in `../Useful Vanilla Textures/mobdefenders/spider ally/`. First build's Gradle daemon disappeared at server-test startup; retry completed successfully. Release JAR includes all four spider facing variants. In-game appearance remains a manual check.

### 2026-11-18 — Creeper Ally, Iron Reinforced Planks and shared requirements

- Added `entity/CreeperAlly.java` and `mixin/FriendlyCreeperExplosionMixin.java`; registered the mixin. Vanilla stats/fuse and charged radius retained; filtered real explosion protects terrain, players, defenders and neutral/passive mobs, including neutral monster families. Suppressed unfiltered lingering clouds/secondary death effects on ally detonation. Existing hostile creepers remain vanilla.
- Edited `ModContent.java`, `spawner/SpawnerKind.java` and `client/MobdefendersClient.java` for the fourth source/entity, iron block/items/attributes/Creative entries. All source ownership/menu/timers/guard logic stays shared; no duplicated lifecycle.
- Added `client/CreeperAllyModel.java` and `CreeperAllyRenderer.java`, using native geometry/animation matching inspected supplied art. Added three unchanged embedded PNGs, two blockstates (four facing variants for spawner), two full-cube block models, two modern item definitions, two self-drop loot tables and the exact two requested recipes. Edited language, shovel/axe tags and mixin registration. Source assets unchanged.
- Added `CreeperAllyModelTest.java` and registered `CreeperAllySpawnerGameTests.java` for stats/fuse/self-discard replacement, cap/save/cleanup, actual swell combat, normal/charged blast safety, vanilla-creeper regression, post-combat patrol return, shared menu/patrol/Standing/recall/sun safety and both recipes. Extended detection GameTests/daylight tag for creeper, the existing all-kind cooldown test covers the fourth kind automatically, and `SpiderSpawnerResourcesTest.java` now validates every spawner's facing variants.
- Added `SHARED_SPAWNER_RULES.md` as the user-requested compiled similarities checklist; updated `README.md` and this guide.
- Verification: expanded full build passed with **16 JUnit tests and 45 server GameTests** (44 mod tests plus Fabric's). Covers normal/charged damage/knockback protection for real survival players, all defenders, passive mobs and neutral monsters; block/TNT safety; no lingering poison cloud; real native swell/fuse; self-discard replacement timing; unchanged hostile creeper damage/terrain destruction; post-combat walk-back and shared menu/Standing/patrol/recall/sunlight/recipes. All 40 source JSON files parse; all three runtime PNGs match both supplied external and embedded bytes. Release classes/resources/recipes/mixin/four-way facing verified; tests and the test-only daylight tag excluded. Whitespace checks pass. Client appearance remains a manual check.

### 2026-11-18 — Clarified Creeper Ally monster targeting

- User clarified that “neutral” protection meant passive mobs and friendly mobs such as Iron Golems, not spiders/piglins/other neutral-monster families.
- Edited `entity/CreeperAlly.java` to remove the extra species exclusions and use `DefenderBehavior.validEnemy` for blast damage/knockback and the shared `canAttack` rule for targeting. Spiders, Piglins, Zombified Piglins and Endermen are eligible again; players, defenders, passive mobs, Iron Golems and Creepers stay protected. Block safety is unchanged.
- Updated both normal/charged blast fixtures in `CreeperAllySpawnerGameTests.java` to assert targeting and actual damage for those monster types while retaining player/ally/passive/golem/block safety checks.
- Updated `README.md`, `SHARED_SPAWNER_RULES.md` and the current Creeper section above; previous entries describe historical verification before this clarification.
- Verification: full build passed with **16 JUnit tests and 45 server GameTests**. Normal and charged blasts target/damage spiders, piglins, zombified piglins and endermen while preserving players, defenders, passive mobs, Iron Golems, Creepers and blocks. Whitespace checks passed.

### 2026-11-18 — Per-spawner-type configuration with default inheritance

- Edited `config/AllySpawnerConfig.java` to resolve shared defaults plus optional sparse overrides per canonical spawner key. Omitted shared fields use built-ins; omitted per-kind fields inherit shared defaults. Existing flat JSON files remain compatible and are never rewritten. Integer/radius validation applies to fully resolved settings; invalid files fall back atomically without changing the file.
- Edited `spawner/SpawnerKind.java` with stable config keys and entity-to-kind fallback for unowned/summoned allies. Edited `AllySpawnerData.java` to schedule initial/replacement deadlines from the source kind, and `AllySpawnerManager.java` to use source-specific spawn/patrol/recall values. Saved deadlines and ownership format remain unchanged.
- Edited `entity/DefenderBehavior.java` to resolve the owned source's settings (or entity species when unowned) for target acquisition/retention, patrol destinations and guard-return pathfinding. No guard modes, friendly filters, fixed one-ally cap or shared 3–5-second patrol pause changed.
- Extended `AllySpawnerConfigTest.java` for sparse/absent overrides, all five settings, namespaced keys, legacy compatibility, validation, immutable results and radius inheritance. Added/registered `config/AllySpawnerConfigGameTests.java` for per-kind timer scheduling/save preservation and real mob detection boundaries. Temporary test overrides are restored synchronously before any world tick.
- Updated `README.md`, `SHARED_SPAWNER_RULES.md` and the configuration guide above. Existing development/server configs are preserved.
- Verification: full build passed with **23 JUnit tests and 46 server GameTests**. Per-kind inheritance/legacy parsing, validation, actual timer scheduling/save preservation and species-specific mob detection pass alongside existing lifecycle/combat/guard regressions. All 40 source JSON files parse; release contains per-kind config classes and excludes test classes. Existing development config remained unchanged.

### 2026-11-18 — Editable per-species built-in code defaults

- Added the actual `defaults(SpawnerKind kind)` switch to `config/AllySpawnerConfig.java`. All four arms retain the current gameplay values pending deliberate edits. Startup/invalid-file fallback and omitted-kind resolution now use those individual code defaults.
- Wired JSON inheritance field by field: type-specific Java defaults, then explicit shared JSON fields, then explicit per-type JSON fields. Validate the final merged per-type result, not partially overridden intermediate settings. Kept flat-config compatibility and saved deadlines unchanged.
- New generated files use empty `defaults`/`spawners` objects so future Java edits are not masked by generated explicit values. Existing files remain untouched; their explicit values continue to override code.
- Extended `AllySpawnerConfigTest.java` with distinct code-default fixtures, missing/empty JSON, precedence, flat-file preservation and final-merge validation. Extended `config/AllySpawnerConfigGameTests.java` to verify actual timer scheduling/detection from deliberately different code defaults with no JSON declarations.
- Updated `README.md`, `SHARED_SPAWNER_RULES.md` and the configuration instructions above, including the exact Java method/argument order and JSON override caveat.
- Verification: full build passed with **27 JUnit tests and 47 server GameTests**. Distinct code-default fixtures prove field-level inheritance, override precedence and real timer/detection behavior without JSON declarations. Existing compatibility and gameplay regressions pass. Whitespace checks passed; release config class present and test classes excluded.

### 2026-11-19 — IntelliJ mixin resolution diagnostic

- Checked the reported unresolved `FriendlyCreeperExplosionMixin` entry: the Java file is in the configured `net.amemah.mobdefenders.mixin` package, the JAR contains the class, and all 47 server GameTests passed. No mixin or gameplay code changed. Reload the project from the Gradle tool window in IntelliJ; if the warning persists, check that `src/main/java` is a source root and the Gradle JVM uses Java 25.
- The full build separately reported four configuration unit-test failures: those assertions still expect the previous 12-block recall default, while the current per-kind Java settings use 15. Existing per-kind settings were left untouched.
