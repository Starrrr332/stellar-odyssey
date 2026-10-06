# Project: Stellar Odyssey — Core Architecture & Modular Subsystems

## Architecture
Stellar Odyssey is an Architectury-based space exploration Minecraft mod (Minecraft 26.3, Java 25, NeoForge 26.3.0.51-beta).
The architecture is structured under `com.amaro.stellarodyssey` following a decoupled Directed Acyclic Graph (DAG):
- `core`: Constants, mod entrypoint, and deterministic lifecycle management via `SatelliteModule` SPI.
- `api`: Leaf dependency interfaces (`ICelestialBody`, `ICelestialCatalog`, `IAtmosphereCondition`, `SatelliteModule`).
- `registry`: Centralized `DeferredRegister` instances for `Blocks`, `Items`, `CreativeModeTabs`, `EntityTypes`, and `SoundEvents`.
- `registry.tiers`: Extensible Alien Mineral Tier Matrix (`IAlienMineralTier`, `AlienMineralTierRegistry`, built-in Tiers 1-5).
- `world`: Dimension ResourceKeys, celestial physics, and planetary catalogs.
- `client`: Model layers, renderers, and full-bright emissive overlays (`SubmitNodeCollector`).
- `satellites`: Decoupled expansion modules:
  - `satellites.worldgen`: Procedural planet generation, noise curves, surface rules.
  - `satellites.ecology`: Alien fauna/flora behavior goals (low gravity jumping, vacuum evasion, spore ticking).
  - `satellites.starmap`: Intergalactic navigation screen, coordinates widget, skybox renderer.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Core Package & Constants | `ModConstants`, `ModLifecycleManager`, `SatelliteModule` SPI | M1 | Survey / R1 / R3 |
| 2 | SoundEvents Registry | `ModSoundEvents` with thrust, alarms, alien ambience, resonance | M2 | Survey / R1 |
| 3 | Unified Registry Binder | `ModRegistries` registering blocks, items, tabs, entities, sounds | M2 | Survey / R1 |
| 4 | Creative Tab & Item Population | `ModCreativeTabs` with icon, item auto-population | M2 | Survey / R2 |
| 5 | Alien Mineral Tier Matrix | `IAlienMineralTier`, `AlienMineralTierRegistry`, Tiers 1–5 | M3 | Survey / R2 |
| 6 | Alien Ore & Material Blocks | Hardness, resistance, sound properties for alien mineral progression | M3 | Survey / R2 |
| 7 | Procedural WorldGen Satellite | `WorldGenSatellite`, noise settings, planetary surface rules | M4 | Survey / R3 |
| 8 | Fauna/Flora Alien AI Satellite | `EcologySatellite`, `LowGravityJumpGoal`, `VacuumFleeGoal` | M5 | Survey / R3 |
| 9 | Intergalactic Star Map Satellite | `StarMapSatellite`, `StarMapScreen`, `StarMapSkyRenderer` | M6 | Survey / R3 |
| 10 | Emissive Client Rendering | `EmissiveModelLayer` with `SubmitNodeCollector` full-bright `0x00F000F0` | M7 | Survey / R1 |
| 11 | Comprehensive Test Suite | Unit & integration tests for registries, tiers, lifecycle, and decoupling | M-Test | Acceptance Criteria |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Core & Decoupling SPI | `core`, `api`, `ModLifecycleManager`, `SatelliteModule` | none | IN_PROGRESS |
| M2 | Centralized Registries & Sounds | `registry.ModSoundEvents`, `registry.ModRegistries` | M1 | IN_PROGRESS |
| M3 | Extensible Alien Mineral Tiers | `registry.tiers` (Tiers 1-5, `IAlienMineralTier`, `AlienMineralTierRegistry`) | M1, M2 | IN_PROGRESS |
| M4 | Procedural WorldGen Satellite | `satellites.worldgen` (Noise, SurfaceRules, SatelliteModule) | M1 | IN_PROGRESS |
| M5 | Alien Ecology & AI Satellite | `satellites.ecology` (Jump goal, Vacuum goal, Spore ticker) | M1 | IN_PROGRESS |
| M6 | Space Navigation Star Map GUI | `satellites.starmap` (Screen, widgets, celestial projection) | M1 | IN_PROGRESS |
| M7 | Client Emissive Rendering | `client.renderer.layer.EmissiveModelLayer`, Starship emissive pass | M1 | IN_PROGRESS |
| M-Test | Test Suite & Verification | JUnit tests for tiers, registry keys, and satellite decoupling | all | IN_PROGRESS |

## Code Layout & File Ownership
Strict non-overlapping write boundaries for parallel agents:
- **Worker 1 (Core & Registries)**:
  - `common/src/main/java/com/amaro/stellarodyssey/core/**`
  - `common/src/main/java/com/amaro/stellarodyssey/api/**`
  - `common/src/main/java/com/amaro/stellarodyssey/registry/ModSoundEvents.java`
  - `common/src/main/java/com/amaro/stellarodyssey/registry/ModRegistries.java`
  - `common/src/main/java/com/amaro/stellarodyssey/StellarOdyssey.java` (linking `ModRegistries` & lifecycle)
- **Worker 2 (Alien Mineral Tiers & Materials)**:
  - `common/src/main/java/com/amaro/stellarodyssey/registry/tiers/**`
  - `common/src/main/java/com/amaro/stellarodyssey/block/AlienMineralBlock.java`
- **Worker 3 (Procedural WorldGen Satellite)**:
  - `common/src/main/java/com/amaro/stellarodyssey/satellites/worldgen/**`
  - `common/src/main/java/com/amaro/stellarodyssey/world/CelestialBodyRegistry.java`
- **Worker 4 (Ecology & Alien AI Satellite)**:
  - `common/src/main/java/com/amaro/stellarodyssey/satellites/ecology/**`
- **Worker 5 (Star Map GUI & Emissive Client)**:
  - `common/src/main/java/com/amaro/stellarodyssey/satellites/starmap/**`
  - `common/src/main/java/com/amaro/stellarodyssey/client/renderer/layer/EmissiveModelLayer.java`
  - `common/src/main/java/com/amaro/stellarodyssey/client/renderer/StarshipEntityRenderer.java`
- **Worker 6 (Test Writer / Verification Suite)**:
  - `common/src/test/java/com/amaro/stellarodyssey/**`

## Interface Contracts
- `SatelliteModule`: `getId()`, `getPriority()`, `onRegister()`, `onCommonSetup()`, `onClientSetup()`, `onServerStarting()`
- `IAlienMineralTier`: `getTierLevel()`, `getName()`, `getId()`, `getToolMaterial()`, `getArmorMaterial()`, `getIncorrectBlocksForDropsTag()`, `getRequiredMiningTierTag()`, `getBlockHardness()`, `getExplosionResistance()`, `getSoundType()`, `getLuminance()`
- `AlienMineralTierRegistry`: `registerTier(IAlienMineralTier)`, `getTier(int)`, `getTier(String)`, `getAllTiers()`
- `ICelestialBody`: `dimensionKey()`, `gravityMultiplier()`, `atmosphericPressure()`, `hasBreathableAtmosphere()`, `solarRadiation()`, `starSystemName()`
- `ICelestialCatalog`: `getBody(ResourceKey<Level>)`, `registerBody(ICelestialBody)`

## Acceptance Criteria
- Clean compilation: `./gradlew compileJava` builds with exit code 0.
- All registries bound properly via `ModRegistries.registerAll()`.
- Extensible Mineral Tier Matrix with Tiers 1-5 pre-registered and dynamic extension verified.
- Satellites register and execute deterministically via `ModLifecycleManager`.
- Test suite passing 100%.
