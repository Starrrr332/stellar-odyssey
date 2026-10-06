# Plan: Stellar Odyssey Space Exploration Features Implementation

## Objectives
Deliver full, genuine implementations of:
1. **R1: Oxygen & Planetary Atmosphere**:
   - O2 degradation in unbreathable dimensions (Nexus Moon, Proxima B, Exotic Prime).
   - OxygenTankItem coupled with full SpacesuitItem set (helmet, chestplate, leggings, boots).
   - Oxygen Sealer / Refiller block & tile/entity registered in ModBlocks & ModItems.
2. **R2: Adaptive Planetary Gravity**:
   - Dimension-based gravity modifiers via Architectury/Minecraft events in ModDimensions.
   - Reduced gravity on Nexus Moon (0.16g) and Proxima B (0.35g) affecting players and entities with slow fall and jump dynamics.
3. **R3: 3D Rocket Models & Emissive Rendering for T2 & T3**:
   - Java 3D models and UV atlas for Tier 2 (Voyager) and Tier 3 (Odyssey).
   - Bioluminescent emissive textures for Celidium (T2) and Astralite/Verdantite (T3) integrated into SubmitNodeCollector.
4. **R4: StarMap GUI & Destination Selection**:
   - Interactive celestial destination picker when mounted in rocket on Launch Pad.
   - Strict rocket Tier validation before dimension travel.
5. **Quality & Acceptance**:
   - 100% JUnit tests passing.
   - Clean `:fabric:build` and `:neoforge:build`.
   - Side-safety guaranteed (no client classes in common).
   - Zero circular dependencies.
   - Sync notes to Obsidian Vault (`C:\Users\amaro\OneDrive\Documents\Obsidian Vault\`).

## Execution Phases
- **Phase 0: Multi-perspective Survey (3 subagents)**:
  - Survey Explorer 1: Inspect `lifesupport/`, `item/`, `block/`, `registry/`, atmosphere logic.
  - Survey Explorer 2: Inspect `world/`, `dimension/`, gravity handlers, physics/event hooks.
  - Survey Spec Miner 3: Inspect `rocket/`, `client/`, `satellites/starmap/`, UI widgets, models, textures, build scripts, test suites, Obsidian vault path.
- **Phase 1: Synthesize Survey & Update PROJECT.md**:
  - Update Feature Inventory with all detailed sub-features.
  - Lock interface contracts and code ownership boundaries.
- **Phase 2: Milestone Iterations**:
  - M1 (Oxygen & Atmosphere): Explorer -> Worker -> Reviewers (2) -> Challengers (2) -> Auditor -> Gate.
  - M2 (Gravity Physics): Explorer -> Worker -> Reviewers (2) -> Challengers (2) -> Auditor -> Gate.
  - M3 (Rocket Models & Emissive): Explorer -> Worker -> Reviewers (2) -> Challengers (2) -> Auditor -> Gate.
  - M4 (StarMap GUI & Launch Pad Selection): Explorer -> Worker -> Reviewers (2) -> Challengers (2) -> Auditor -> Gate.
- **Phase 3: Final Verification & Obsidian Vault Sync**:
  - Full `./gradlew test`, multi-loader builds, documentation synchronization to Obsidian.
