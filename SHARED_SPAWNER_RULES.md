# Shared Ally Spawner requirements

Applies to **Zombie, Skeleton, Spider and Creeper Ally Spawners**. Species keep their own movement speed, navigation, combat and vanilla animations; the spawner/guard rules below are shared.

## Placement, block and crafting

- Directional placement uses the same horizontal facing rule: faces away from the placing player, with north/east/south/west model rotations.
- Full-cube visual, selection and collision; dirt-like mining, shovel efficiency and a self-drop.
- One spawner per shaped craft. Each species has its own supplied texture/model and recipe, not interchangeable ingredients.
- Copper and Iron Reinforced Planks are ordinary full-cube oak-like building/crafting blocks, axe-efficient and self-dropping. Their recipes use eight corresponding ingots around one **Oak Planks**, yielding one reinforced block.

## Spawning and ownership

- **Exactly one living owned ally per spawner**, hard-coded rather than a configurable cap.
- **45-second initial wait**, then **90-second replacement wait** after death or permanent discard. Initial wait is half the configured replacement delay, floored to whole seconds.
- Timers use world ticks. Attempts run once a second, so a ready spawn can be up to one second later than its deadline.
- Replacement is the same species. A Creeper Ally consuming itself in an explosion starts the same replacement wait.
- Source, species, guard mode, ally UUID and deadlines persist across saves/restarts. Unloading is not death and never creates another ally.
- Nearby/overlapping spawners own separate allies. Breaking/replacing a spawner retires only its own ally; unloaded allies are queued for cleanup on next load.
- Spawn only on safe, supported, dry, collision-free ground in available chunks; do not force-load chunks. Obstructed/occupied destinations retry rather than spawning into blocks/entities.
- Successful spawn uses the same smoke particles and spawn animation.

## Guard controls

- Same right-click menu: **Guard Type:** and one **Standing/Wander** toggle.
- **Standing** by default; mode persists after closing the menu/reloading. Switching modes applies to the same existing ally.
- **Standing:** spawn on top of the source, hold that post while idle, engage eligible enemies, then return to the exact post.
- **Wander:** spawn near the source and choose safe reachable destinations within the **six-block patrol area**.
- Every ally pauses a randomized **3–5 seconds** between patrol walks. Combat and guard return are not delayed by the idle cooldown.
- If combat takes a Wander ally outside patrol, it **walks back inside after it has no enemy**, then resumes wandering. It need not stand on the source.

## Targeting and movement safety

- Default detection is **ten blocks from the ally's current position**, not its spawner, with **no line-of-sight requirement** for acquisition/retention.
- Fight hostile monsters, excluding **Creepers and every Defender**. Do not deliberately attack players, defenders or non-enemy passive mobs, even in retaliation. Detection through walls does not bypass physical attack/projectile obstruction.
- All defenders are **sunlight-safe**, including helmet wear; this is not general fire/lava immunity.
- Use normal species combat/vanilla animations and supplied art. Friendly golems do not target defenders; defender projectiles and Creeper Ally blasts exclude players, defenders, Creepers and non-enemy passive entities, including iron golems.
- Creeper Ally uses the same monster filter as the other allies: ordinary spiders, piglins, zombified piglins and endermen are eligible, even though some are classed as neutral in vanilla gameplay.
- Return by **pathfinding/walking first**. No teleport merely for reaching a patrol edge or taking a long detour.
- Only **200 ticks (ten seconds) without at least 0.25 blocks of progress** allows the stuck-return teleport fallback; the destination must still be safe.
- Separate **twelve-block emergency recall** applies in both modes, preferably to the source top, otherwise a safe point inside patrol. Unsafe destinations retry.
- Source-bound allies cannot use portals/change dimensions. Allies persist instead of distance-despawning and do not prevent player sleep.

## Shared defaults

| Setting | Default |
| --- | --- |
| Initial wait | 45 seconds |
| Replacement delay | 90 seconds |
| Spawn radius | 3 blocks |
| Patrol radius | 6 blocks |
| Detection radius | 10 blocks from ally |
| Emergency recall radius | 12 blocks from source |
| Between-walk pause | Random 3–5 seconds |
| Owned-ally cap | 1 |

Spawn/patrol/recall settings are source-centred. Patrol/recall use a horizontal circle and vertical tolerance; detection uses three-dimensional ally-centred distance. These are the current shipped defaults. Edit each type's `defaults(SpawnerKind kind)` arm in `config/AllySpawnerConfig.java` to change its built-in settings. Resolution is field by field: **type-specific code defaults → explicit shared JSON fields → explicit per-type JSON fields**. Empty/omitted JSON declarations preserve code defaults. Explicit fields in existing configs still override Java changes. New files use empty declaration objects; old flat configs remain compatible. Initial wait stays half the effective replacement delay. Health, movement and combat remain species-specific.
