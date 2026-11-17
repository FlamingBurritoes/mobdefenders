# Project maintenance

- Keep `MOD_FILES.md` current whenever working on this project. Record added/edited files, gameplay changes, assumptions, asset requirements and verification results. Append a dated change entry rather than erasing earlier history.
- Target Minecraft 26.1.2, Fabric and Java 25; check the installed APIs before copying older examples.
- Preserve user-supplied Blockbench models and textures. Placeholder visuals are documented in `MOD_FILES.md`; inspect the user's `.bbmodel` before choosing a custom entity model pipeline.
- This mod is independent of `pvzspawner` and its Plantz/Pea Extension dependencies. That project's Weed Spawner is a behavioral reference, not a runtime dependency.
- Every defender, including future entity types, must implement `entity.Defender` and remain immune to sunlight burning. `SunSafeDefenderMixin` skips vanilla daylight ignition/helmet damage for these mobs; do not make them generally fireproof. Disable any additional entity-specific sunlight behavior when necessary.
