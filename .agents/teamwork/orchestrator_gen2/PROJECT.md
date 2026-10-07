# Project: Stellar Odyssey — Space Exploration Features (R1–R4)

## Architecture
Stellar Odyssey is an Architectury-based multi-loader mod (Fabric + NeoForge, Minecraft 26.3, Java 25).
The space exploration systems operate across decoupled packages:
- `com.amaro.stellarodyssey.lifesupport`: Atmospheric detection, oxygen consumption, spacesuit coupling, and sealed room flood-fill.
- `com.amaro.stellarodyssey.world`: Planetary dimensions, celestial catalog (`CelestialBodyRegistry`), and adaptive entity gravity modifiers (`PlanetaryGravityManager`).
- `com.amaro.stellarodyssey.rocket`: Rocket tiers (`RocketTierRegistry`), component assemblies, launch pad interaction, and entity mechanics (`RocketEntity`).
- `com.amaro.stellarodyssey.client`: Java 3D models (`RocketModel`, `RocketTier2Model`, `RocketTier3Model`), entity renderers (`RocketEntityRenderer`), and full-bright emissive layers via `SubmitNodeCollector`.
- `com.amaro.stellarodyssey.satellites.starmap`: Navigation screen (`StarMapScreen`), celestial target selection, and launch initiation.
- `com.amaro.stellarodyssey.network`: Common network payloads (`OxygenSyncPayload`, `SelectDestinationPayload`, `FlightPhasePayload`).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Atmosphere Differentiation | Differentiate hard vacuum (<0.05 atm, Moon) vs toxic atmosphere (0.85 atm, Exotic Prime) in `AtmosphereHelper` via `CelestialBodyRegistry` | M1 | R1 Survey |
| 2 | Spacesuit O2 Coupling | Require `SPACESUIT_CHESTPLATE` (manifold) & `SPACESUIT_HELMET` for tank drain; scale drain by 2x for partial suits | M1 | R1 Survey |
| 3 | Oxygen Refiller Station | `ModBlocks.OXYGEN_REFILLER` + `ModItems.OXYGEN_REFILLER` with right-click tank refill logic & 26.3 item descriptor | M1 | R1 Survey |
| 4 | Oxygen Sealer System | `ModBlocks.OXYGEN_SEALER` + `ModBlockEntityTypes.OXYGEN_SEALER` with 3D BFS room flood-fill & sealed atmosphere caching | M1 | R1 Survey |
| 5 | Life Support Unit Tests | Unit tests verifying tank durability math, suit coupling rules, catalog parameters, and BFS seal logic | M1 | R1 Survey |
| 6 | Gravity Catalog Alignment | Set `CelestialBodyRegistry.PROXIMA_B` to 0.35g and verify `NEXUS_MOON` at 0.16g | M2 | R2 Survey |
| 7 | Dynamic Player Gravity | Dynamic `Attributes.GRAVITY`, `SAFE_FALL_DISTANCE`, and `FALL_DAMAGE_MULTIPLIER` modifiers in `PlanetaryGravityManager` | M2 | R2 Survey |
| 8 | Entity & Mob Gravity | Scale gravity of all `LivingEntity` instances entering levels via `EntityEvent.ADD` | M2 | R2 Survey |
| 9 | Adaptive Gravity Tests | Unit tests verifying formula calculations, dimension mappings, and attribute modifiers | M2 | R2 Survey |
| 10 | Rocket Tier 2 Java 3D Model | `RocketTier2Model` (Voyager) with dual side boosters, fuel conduit ribs, and UV atlas | M3 | R3 Survey |
| 11 | Rocket Tier 3 Java 3D Model | `RocketTier3Model` (Odyssey) with quad warp nacelles, crystalline wings, tri-engine bells, and UV atlas | M3 | R3 Survey |
| 12 | Rocket Textures & Emissives | Diffuse & bioluminescent emissive textures (`rocket_t2`, `rocket_t2_emissive`, `rocket_t3`, `rocket_t3_emissive`) | M3 | R3 Survey |
| 13 | Multi-Model Emissive Rendering | Multi-model baking in `RocketEntityRenderer`, scale modifiers, and `SubmitNodeCollector` emissive pass | M3 | R3 Survey |
| 14 | Launch Pad Mounting StarMap | Mounting rocket on `LaunchPadBlock` opens `StarMapScreen` for celestial destination selection | M4 | R4 Survey |
| 15 | StarMap Destination Selection | Interactive celestial body selection with tier requirement badges and launch initiation | M4 | R4 Survey |
| 16 | Strict Tier Validation & Network | `SelectDestinationPayload` C2S with server validation (T1: Moon; T2: Moon & Proxima; T3: Moon, Proxima, Exotic) | M4 | R4 Survey |
| 17 | StarMap & Rocket Tier Tests | Unit tests verifying tier destination constraints, launch pad detection, and network validation | M4 | R4 Survey |
| 18 | Master JUnit Test Suite | `./gradlew test` passes 100% of all unit tests (135/135 tests passing) | M5 | Acceptance |
| 19 | Cross-Loader Build Verification | `./gradlew :fabric:build` and `./gradlew :neoforge:build` compile cleanly without side leaks | M5 | Acceptance |
| 20 | Obsidian Vault Sync | Sync comprehensive architectural notes to `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\Agentes\` | M5 | Acceptance |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Oxygen & Planetary Atmosphere | Features 1–5 | none | DONE |
| M2 | Adaptive Planetary Gravity | Features 6–9 | none | DONE |
| M3 | Rocket Models & Emissive Rendering | Features 10–13 | none | DONE |
| M4 | StarMap GUI & Destination Selection | Features 14–17 | M3 | DONE |
| M5 | Final Verification & Obsidian Sync | Features 18–20 | M1, M2, M3, M4 | DONE |
