# Shared Ally Spawner requirements

Applies to **Zombie, Skeleton, Spider, Creeper, Enderman, Slime, Drowned, Brute, Magma Cube, Blaze, Ghast and Wither Skeleton Ally Spawners**. Species keep their own movement speed, navigation, combat and animations; the spawner/guard rules below are shared.

## Placement, block and crafting

- Directional placement uses the same horizontal facing rule: faces away from the placing player, with north/east/south/west model rotations.
- Full-cube visual, selection and collision; dirt-like mining, shovel efficiency and a self-drop.
- Placement and breaking sound like Oak Planks for all spawners and both reinforced plank blocks, except the Brute Ally Spawner and Gold Reinforced Nether Brick, which sound like Nether Bricks.
- One spawner per shaped craft. Each species has its own supplied texture/model and recipe, not interchangeable ingredients.
- Copper and Iron Reinforced Planks are ordinary full-cube oak-like building/crafting blocks, axe-efficient and self-dropping. Their recipes use eight corresponding ingots around one **Oak Planks**, yielding one reinforced block.
- Gold Reinforced Nether Brick is a full-cube, pickaxe-mined building/crafting block that drops itself when mined with a pickaxe. Craft it with eight Gold Ingots surrounding one Nether Brick item.

## Spawning and ownership

- **One living owned ally per spawner** except Slime and Magma Cube: each starts large and can split into up to **eight Slimes** or **four Magma Cubes**. Caps are not configurable. Replacement starts only after the last member is gone. Two or more survivors can merge into one large ally after 20 seconds without detecting an enemy, weighted by their sizes and remaining health.
- **45-second initial wait** and **90-second replacement wait** by default (Creeper: 30/60 seconds). Initial wait is half the configured replacement delay, floored to whole seconds.
- Timers use world ticks. Attempts run once a second, so a ready spawn can be up to one second later than its deadline.
- Replacement is the same species. A Creeper Ally consuming itself in an explosion starts the same replacement wait.
- Source, species, guard mode, ally UUID and deadlines persist across saves/restarts. Unloading is not death and never creates another ally.
- Nearby/overlapping spawners own separate allies. Breaking/replacing a spawner retires only its own ally; unloaded allies are queued for cleanup on next load.
- Spawn only at safe, collision-free locations in available chunks; do not force-load chunks. Most allies require dry, supported ground; the Drowned Spawner also supports water. Obstructed/occupied destinations retry rather than spawning into blocks/entities.
- Successful spawn uses the same smoke particles and spawn animation.

## Guard controls

- Same right-click menu: **Guard Type:** and one **Standing/Wander** toggle.
- **Standing** by default; mode persists after closing the menu/reloading. Switching modes applies to the same existing ally.
- **Standing:** spawn on top of the source, hold that post while idle, engage eligible enemies, then return to the exact post.
- **Spider, Slime and Magma Cube Standing exception:** the spawner column and all eight adjacent columns form a **3×3 square**, including diagonals. Hold any dry, supported spot in that area within three blocks vertically of the source. Spawning, emergency recall and family merges prefer the top, but can use another safe spot in the square. Return paths target the area, not the center.
- **Wander:** spawn near the source and choose safe reachable destinations within the **six-block patrol area**.
- A Drowned Ally breathes underwater indefinitely. With an underwater post it swims within the same horizontal radius and at most seven blocks above or below the source; a dry-post Drowned does not seek water.
- Blaze and Ghast Allies hold the usual exact Standing post. In Wander they can fly from the source's height to seven blocks above it, within the same horizontal patrol radius.
- Every ally pauses a randomized **3–5 seconds** between patrol walks. Combat and guard return are not delayed by the idle cooldown.
- If combat takes a Wander ally outside patrol, it **walks back inside after it has no enemy**, then resumes wandering. It need not stand on the source.

## Targeting and movement safety

- Default detection is **ten blocks from the ally's current position**, not its spawner, with **no line-of-sight requirement** for acquisition/retention.
- Fight hostile monsters, excluding **Creepers and every Defender**. Do not deliberately attack players, defenders or non-enemy passive mobs, even in retaliation. Detection through walls does not bypass physical attack/projectile obstruction.
- All defenders are **sunlight-safe**, including helmet wear. The Piglin Brute and Magma Cube are also immune to fire/lava; the Piglin Brute cannot zombify. Sunlight safety alone does not grant fire immunity.
- Blaze Allies ignore rain, water, splash water bottles and snowballs and retain vanilla fire/lava immunity. Their fireballs cannot start fires on blocks.
- Ghast Ally fireballs only hit and blast eligible hostiles, never damage or knock back allies or dropped items, and never break or ignite blocks.
- Wither Skeleton Allies inflict Wither I for ten seconds when they land a sword hit on an eligible enemy.
- Zombie Allies are always adults and never pick up dropped items, even if older save data requests otherwise. Spider Allies do not flee armadillos.
- Use normal species combat/vanilla animations and supplied art. Friendly golems do not target defenders; defender projectiles and Creeper Ally blasts exclude players, defenders, Creepers and non-enemy passive entities, including iron golems.
- Creeper Ally uses the same monster filter as the other allies: ordinary spiders, piglins, zombified piglins and endermen are eligible, even though some are classed as neutral in vanilla gameplay.
- Return by **pathfinding/walking first**. No teleport merely for reaching a patrol edge or taking a long detour.
- Only **200 ticks (ten seconds) without at least 0.25 blocks of progress** allows the stuck-return teleport fallback; the destination must still be safe.
- Separate **emergency recall** (15 blocks by current code defaults) applies in both modes, preferably to the source top, otherwise a safe point inside patrol (inside the 3×3 guard area for Standing Spider, Slime and Magma Cube). Unsafe destinations retry.
- Source-bound allies cannot use portals/change dimensions. Allies persist instead of distance-despawning and do not prevent player sleep.

## Shared defaults

| Setting | Default |
| --- | --- |
| Initial wait | 45 seconds (Creeper: 30 seconds) |
| Replacement delay | 90 seconds (Creeper: 60 seconds) |
| Spawn radius | 3 blocks |
| Patrol radius | 6 blocks |
| Detection radius | 10 blocks from ally |
| Emergency recall radius | 15 blocks from source |
| Between-walk pause | Random 3–5 seconds |
| Owned-ally cap | 1 (Slime family: 8; Magma Cube family: 4) |

Spawn/patrol/recall settings are source-centred. Patrol/recall use a horizontal circle and vertical tolerance; detection uses three-dimensional ally-centred distance. These are the current shipped defaults. Edit each type's `defaults(SpawnerKind kind)` arm in `config/AllySpawnerConfig.java` to change its built-in settings. Resolution is field by field: **type-specific code defaults → explicit shared JSON fields → explicit per-type JSON fields**. Empty/omitted JSON declarations preserve code defaults. Explicit fields in existing configs still override Java changes. New files use empty declaration objects; old flat configs remain compatible. Initial wait stays half the effective replacement delay. Health, movement and combat remain species-specific.
