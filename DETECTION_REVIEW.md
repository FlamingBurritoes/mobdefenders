# Peashooter / Weed detection review

Reviewed 2026-11-17 against the **actual dependency JARs pinned by `../pvzspawner`**:

- Plants & Zombies (`plantz`) **1.5.1**, CurseForge file **9347311**.
- Pea Extension (`pea_extension`) **1.2.8**, CurseForge file **9379288**.
- Minecraft **26.1.2** vanilla targeting implementation.

This is a bytecode/decompiled-code review, not a live gameplay test of those two dependency mods. No dependency or detection change has been added to Mob Defenders as part of the review.

## Main finding

These plants use vanilla **nearest visible eligible target** acquisition. They do not have a special forward-facing vision cone, lane-only detection or a scan centered on a spawner. Detection distance is measured from the **plant itself**.

Enemy selection, attack range and pursuit limits are separate systems.

## Proactive acquisition

| Entity | Untamed/wild candidates | Tamed candidates | Default acquisition range |
| --- | --- | --- | --- |
| Base Peashooter, `plantz:peashooter` | `Zombie` subclasses | `Zombie` subclasses or any vanilla `Enemy` | 14 blocks |
| Extension GW Peashooter, `pea_extension:gw_peashooter` | `Zombie` subclasses | `Zombie` subclasses or any vanilla `Enemy` | 14 blocks |
| Weed, `pea_extension:weed` | `Zombie` subclasses | `Zombie` subclasses or any vanilla `Enemy` | Wild: 6 blocks; tamed: 16 blocks |

All three proactive selectors explicitly exclude `Plant` entities and Creepers. `Zombie` means the Java class family, not a name search or a zombie tag. Plants & Zombies' `PazZombie` extends vanilla `Zombie`, so its zombie subclasses qualify.

The shared predicate is effectively:

```java
notPlant && notCreeper && (target instanceof Zombie || (target instanceof Enemy && plant.isTame()))
```

Weed adds a six-block, plant-centered squared-distance check for wild acquisition. Tamed Weed skips that extra check and uses its full follow-range attribute.

Sources:

- `joshxviii.plantz.entity.plant.PeaShooter.registerGoals` and its selector lambda.
- `zombieman.pea_extension.entity.GWPlant.registerGoals`, `registerGoals$lambda$1`, `canAttack` and `wantsToAttack`.
- `zombieman.pea_extension.entity.Weed.getWildTargetRange`.
- `PeaExtensionEntities` and `Plant.Companion.PlantAttributes` for registered/default attributes: follow range defaults to **14**; Weed explicitly sets **16**.

## How visibility and selection work

Both the base Peashooter and shared GW plant code install:

```java
new NearestAttackableTargetGoal(mob, LivingEntity.class, 5, true, false, selector)
```

- **Nearest**, not weakest or first to enter the area.
- **360-degree candidate search**: there is no heading/FOV predicate.
- **Line of sight is required for acquisition**, using vanilla combat `TargetingConditions` and mob sensing.
- **No reachable-path requirement** (`mustReach = false`). A visible target behind a gap/fence can qualify even when the plant cannot walk to it.
- Follow range defines the nominal three-dimensional distance check; the broad-phase search inflates the mob's box in all three axes in this Minecraft version. Vanilla visibility effects can reduce effective acquisition range.
- The constructor's scan parameter is **5**, versus Mob Defenders' current **10**. This is a randomized retry parameter, not a guarantee of one scan every exactly five ticks: vanilla applies reduced tick delays and goal scheduling.
- Once a goal has a target, it normally retains it instead of switching every time another enemy moves slightly closer.
- Losing sight does **not** necessarily clear the target immediately. Inherited `TargetGoal` has unseen-target memory (`unseenMemoryTicks = 60`, passed through reduced tick timing), plus normal range/liveness/team/`canAttack` checks. Other attack/ability goals can clear it sooner.

Sources: vanilla `NearestAttackableTargetGoal`, `TargetGoal`, `TargetingConditions`, and the constructors above.

## Retaliation / helping the owner

`Plant.attackGoals` also registers higher-priority goals:

1. `HurtByTargetGoal`, ignoring attackers of class `Plant`, with alert-others enabled.
2. `OwnerHurtByTargetGoal`.
3. `OwnerHurtTargetGoal`.

Consequently, the proactive candidate table is **not an exhaustive statement of every possible reaction**. Damage, nearby alerts or the owner's combat can provide targets through different paths.

`GWPlant.canAttack`/`wantsToAttack` reject players and plants. Base Plant logic also rejects entities sharing the same root owner. The proactive Creeper exclusion is not repeated in `GWPlant.canAttack`; do not assume copying the selector alone gives Mob Defenders' strict all-path Creeper protection.

Source: `Plant.attackGoals`, `Plant.canAttack`, `Plant.wantsToAttack`, `GWPlant.canAttack`, `GWPlant.wantsToAttack`.

## Attacking and chasing

### Peashooter

- Ordinary projectile attacks use `ProjectileAttackGoal`; its default attack radius comes from **FOLLOW_RANGE** (14 by default for these two Peashooters).
- The ordinary projectile action checks target liveness/use predicate/range. It does **not** independently require line of sight on every shot. The acquisition/retention goal is what supplies the ordinary visibility policy.
- GW Peashooter's special Gatling targeting additionally checks liveness, `canAttack`, follow range and **current line of sight** on its own.
- GW plants have their own strafing, follow and anchored-wander movement, distinct from target acquisition.

Sources: `ProjectileAttackGoal` constructor/defaults and `canDoAction`, `GWPeaShooter.registerAttackGoal`, `GWPeaShooter.gatlingTarget`.

### Weed

- Nominal melee reach: **3 blocks**.
- Adds `ChaseTargetGoal(this, 0.75, 0.45, 16.0, 6.0)`.
- Starts moving toward the selected target when farther than **2.25 blocks** (`3 * 0.75`).
- Stops that chase when within **1.35 blocks** (`3 * 0.45`). Different start/stop thresholds help avoid movement jitter.
- Repaths toward the target every **5 ticks** while this chase goal runs.
- Pursuit leash: **16** if tamed; **6** if wild. Its anchor is the plant's position when it starts tracking that target, **not a spawner block**.
- If the wild target exceeds its shorter leash, the chase goal explicitly clears the target. Tamed leash behavior stops chasing but does not use that same wild-only clearing branch.

- Weed disables the GW combat-strafe goal in favor of chasing.

Sources: `Weed.registerGoals`, `Weed.getUsesCombatStrafe`, `ChaseTargetGoal.engaged`, `reach` and `tick`.

## Current Mob Defenders — updated after the review

Mob Defenders now deliberately differs from Plantz's visibility policy: its nearest-target goal uses `mustSee = false`, `mustReach = false`, and explicitly disables the acquisition condition's line-of-sight check. The main differences are:

- **Every defender already accepts all hostile `Enemy` mobs**, except Creepers and other defenders; there is no wild/tamed distinction.
- Detection now defaults to **10 blocks from the ally's current position**, in three dimensions. The new `detectionRadius` configuration is independent of source-centered patrol/recall. Both the target goal's acquisition/retention range and `canAttack` use it; the old six-block spawner-centered combat gate has been removed.
- It uses randomized scan parameter **10**, not 5.
- Walls do not prevent acquiring or retaining enemies. This changes detection only; physical movement, melee reach and bow/projectile obstruction are not bypassed.
- It has no plant-style owner-defense/alert-others goals.
- Its twelve-block emergency recall is source-centered. Copying a 14/16-block detector without a separate chase policy could cause repeated chase/recall cycles.
- A literal copy of the plant predicate is unsafe: an untamed plant would treat our `ZombieAlly` as a zombie enemy, and tamed plants could treat either defender as `Enemy`. Preserve explicit `Defender` exclusions if adapting this logic.

## Selected policy and remaining separate choices

The user selected **10-block, mob-centered acquisition without LOS**, now implemented for both species. Scan timing is unchanged. Keep these concepts independent:

1. **Patrol radius** around the spawner.
2. **Detection radius** around the defender.
3. **Pursuit leash** around the spawner, with deliberate target release before emergency recall.
4. **Emergency recall radius** for genuine displacement/escape.

Retain blanket exclusions for players, passive/friendly mobs, all defenders and Creepers on every targeting/projectile path. Decide explicitly whether owner defense/nearby defender alerts are wanted; neither should be added simply because plants inherit them.

The original research task changed guard-return movement only. Subsequent user-approved changes implemented `detectionRadius: 10`, removed LOS requirements and explicitly return idle Wander defenders to their patrol area after combat. The original Plantz findings above remain unchanged. See `MOD_FILES.md`; no new pursuit leash or owner/group-defense system has been added.
