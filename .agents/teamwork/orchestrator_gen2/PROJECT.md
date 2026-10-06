# Project: Stellar Odyssey — Space Exploration Features (R1–R4)

## Architecture
Stellar Odyssey is an Architectury-based multi-loader mod (Fabric + NeoForge, Minecraft 26.3, Java 25).
The space exploration systems operate across decoupled packages:
- `com.amaro.stellarodyssey.lifesupport`: Atmospheric detection, oxygen consumption, spacesuit coupling, and sealed room flood-fill.
- `com.amaro.stellarodyssey.world`: Planetary dimensions, celestial catalog (`CelestialBodyRegistry`), and adaptive entity gravity modifiers (`PlanetaryGravityManager`).
- `com.amaro.stellarodyssey.rocket`: Rocket tiers (`RocketTierRegistry`), component assemblies, launch pad interaction, and entity mechanics (`RocketEntity`).
- `com.amaro.stellarodyssey.client`: Java 3D models (`RocketModel`, `RocketTier2Model`, `RocketTier3Model`), entity renderers (`RocketEntityRenderer`), and full-bright emissive layers via `SubmitNodeCollector`.
- `com.amaro.stellarodyssey.satellites.starmap`: Navigation screen (`StarMapScreen`), celestial target selection, and launch initiation.
- `com.amaro.stellarodyssey.network`: Common network payloads (`OxygenSyncPayload`, `SelectRocketDestinationPayload`, `OpenRocketStarMapPayload`).

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
| 16 | Strict Tier Validation & Network | `SelectRocketDestinationPayload` C2S with server validation (T1: Moon; T2: Moon & Proxima; T3: Moon, Proxima, Exotic) | M4 | R4 Survey |
| 17 | StarMap & Rocket Tier Tests | Unit tests verifying tier destination constraints, launch pad detection, and network validation | M4 | R4 Survey |
| 18 | Master JUnit Test Suite | `./gradlew test` passes 100% of all unit tests | M5 | Acceptance |
| 19 | Cross-Loader Build Verification | `./gradlew :fabric:build` and `./gradlew :neoforge:build` compile cleanly without side leaks | M5 | Acceptance |
| 20 | Obsidian Vault Sync | Sync comprehensive architectural notes to `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\Agentes\HANDOFFS\antigravity.md` | M5 | Acceptance |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Oxygen & Planetary Atmosphere | Features 1–5 | none | IN_PROGRESS |
| M2 | Adaptive Planetary Gravity | Features 6–9 | none | IN_PROGRESS |
| M3 | Rocket Models & Emissive Rendering | Features 10–13 | none | PLANNED |
| M4 | StarMap GUI & Destination Selection | Features 14–17 | M3 | PLANNED |
| M5 | Final Verification & Obsidian Sync | Features 18–20 | M1, M2, M3, M4 | PLANNED |

## Code Layout & File Ownership
Strict non-overlapping write boundaries for parallel workers:
- **Worker M1 (Oxygen System & Atmosphere)**:
  - `common/src/main/java/com/amaro/stellarodyssey/lifesupport/**`
  - `common/src/main/java/com/amaro/stellarodyssey/block/OxygenSealerBlock.java`
  - `common/src/main/java/com/amaro/stellarodyssey/block/OxygenRefillerBlock.java`
  - `common/src/main/java/com/amaro/stellarodyssey/block/entity/OxygenSealerBlockEntity.java`
  - `common/src/main/java/com/amaro/stellarodyssey/registry/ModBlocks.java` (Oxygen sealer/refiller entries)
  - `common/src/main/java/com/amaro/stellarodyssey/registry/ModItems.java` (Oxygen sealer/refiller entries)
  - `common/src/main/java/com/amaro/stellarodyssey/registry/ModBlockEntityTypes.java`
  - `common/src/main/resources/assets/stellarodyssey/models/**/oxygen_*`
  - `common/src/main/resources/assets/stellarodyssey/items/oxygen_*.json`
  - `common/src/main/resources/assets/stellarodyssey/blockstates/oxygen_*.json`
  - `common/src/test/java/com/amaro/stellarodyssey/lifesupport/**`
- **Worker M2 (Adaptive Planetary Gravity)**:
  - `common/src/main/java/com/amaro/stellarodyssey/world/PlanetaryGravityManager.java`
  - `common/src/main/java/com/amaro/stellarodyssey/world/CelestialBodyRegistry.java` (Proxima B 0.35g adjustment)
  - `common/src/test/java/com/amaro/stellarodyssey/world/AdaptivePlanetaryGravityTest.java`
- **Worker M3 (Rocket 3D Models & Emissive Rendering)**:
  - `common/src/main/java/com/amaro/stellarodyssey/client/model/RocketTier2Model.java`
  - `common/src/main/java/com/amaro/stellarodyssey/client/model/RocketTier3Model.java`
  - `common/src/main/java/com/amaro/stellarodyssey/client/renderer/RocketEntityRenderer.java`
  - `tools/TextureGen.java` (Adding rocket_t2, rocket_t2_emissive, rocket_t3, rocket_t3_emissive)
  - `common/src/main/resources/assets/stellarodyssey/textures/entity/rocket_t*`
  - `common/src/main/java/com/amaro/stellarodyssey/rocket/RocketTier.java`
- **Worker M4 (StarMap GUI & Launch Destination Selection)**:
  - `common/src/main/java/com/amaro/stellarodyssey/rocket/RocketEntity.java` (Destination variable & pad launch hook)
  - `common/src/main/java/com/amaro/stellarodyssey/satellites/starmap/StarMapScreen.java`
  - `common/src/main/java/com/amaro/stellarodyssey/network/ModNetworking.java`
  - `common/src/main/java/com/amaro/stellarodyssey/network/SelectRocketDestinationPayload.java`
  - `common/src/test/java/com/amaro/stellarodyssey/rocket/RocketTierDestinationTest.java`
- **Worker M5 (Build Verification, Master Tests & Obsidian Vault Sync)**:
  - `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\Agentes\HANDOFFS\antigravity.md`
  - `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\Agentes\STATUS.md`
  - `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\Agentes\DECISIONES.md`

## Interface Contracts
- `AtmosphereHelper.isVacuumEnvironment(Player)` -> queries `CelestialBodyRegistry.getBody(dimensionKey).isVacuum()` and check sealed rooms.
- `AtmosphereHelper.isRoomSealed(Level, BlockPos)` -> boolean checked by BFS bounds cache of active `OxygenSealerBlockEntity`.
- `PlanetaryGravityManager.getGravityMultiplier(Level, BlockPos/Entity)` -> returns dimension multiplier or orbital 0.08g.
- `RocketTier.minTierForDestination(ResourceKey<Level>)` -> returns 1 for Moon, 2 for Proxima B, 3 for Exotic Prime.
- `SelectRocketDestinationPayload`: `(int rocketEntityId, ResourceKey<Level> destinationDimension)`.
