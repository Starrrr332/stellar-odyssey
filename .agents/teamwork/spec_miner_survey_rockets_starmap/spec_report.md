# Specification Report: Rocket 3D Models, Emissive Rendering, StarMap GUI & Obsidian Sync (R3, R4)

**Agent:** `spec_miner_survey_rockets_starmap`  
**Date:** 2026-10-06  
**Target Project:** Stellar Odyssey (`stellarodyssey`)  
**Environment:** Minecraft 26.3, Java 25, Architectury Loom (Fabric + NeoForge)  
**Authoritative Sources:** `ORIGINAL_REQUEST.md` (section 2026-10-06T17:13:21Z), `PROJECT.md`, `RESUMEN_PARA_OTRA_IA.md`, codebase in `common/src/`  

---

## 1. Executive Summary

This specification report details the technical discovery, gaps, and precise engineering requirements for:
- **R3**: 3D Java Models, UV layout atlases, and full-bright emissive rendering pipelines for Tier 2 (Voyager) and Tier 3 (Odyssey) rockets.
- **R4**: StarMap navigation GUI interaction upon mounting the rocket on a complete 3×3 Launch Pad, strict rocket tier destination validation, and C2S network synchronization for launching.
- **Build & Verification**: Gradle test suite execution and side-safety compliance.
- **Obsidian Vault Synchronization**: Real-time markdown synchronization with `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\`.

---

## 2. Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | R3 - Rocket Entity | `RocketEntity` Data & State | Synched entity managing tier (1-3), launch countdown, and vertical liftoff | Entity creation with tier, `DATA_TIER`, `DATA_LAUNCHING` | Renders at position, handles passenger riding | Clamps tier between 1 and 3 | `com.amaro.stellarodyssey.entity.RocketEntity` |
| 2 | R3 - Rocket Registry | `RocketTier` & `RocketTiers` | Canonical tier definitions: T1 (Pioneer), T2 (Voyager), T3 (Odyssey) with destinations, capacity, countdown, scale | Tier params (name, destination, countdown, color, scale) | Registered in `RocketTierRegistry` | Duplicate tier level/name throws `IllegalArgumentException` | `com.amaro.stellarodyssey.registry.tiers.RocketTiers` |
| 3 | R3 - Rocket Model | `RocketModel` (T1) | 3D Java EntityModel defining vertical stack: fuselage, nose cone, engine bell, 4 fins, window band | `ModelPart` root baked from `LAYER_LOCATION` | Mesh submitted to renderer | Missing bone throws `NoSuchElementException` | `com.amaro.stellarodyssey.client.model.RocketModel` |
| 4 | R3 - Rocket Renderer | `RocketEntityRenderer` | EntityRenderer submitting model passes using MC 26.3 `SubmitNodeCollector` | `RocketRenderState`, `PoseStack`, `SubmitNodeCollector` | Calls `collector.submitModel(...)` | Texture defaults or crashes if null | `com.amaro.stellarodyssey.client.renderer.RocketEntityRenderer` |
| 5 | R3 - Emissive Pipeline | `EmissiveModelLayer` | Utility submitting full-bright (`0x00F000F0`) model overlays via `SubmitNodeCollector` without night attenuation | Model, State, PoseStack, Collector, Emissive Identifier | Overlay draw pass at `FULL_BRIGHT` | Null params safely early-return | `com.amaro.stellarodyssey.client.renderer.layer.EmissiveModelLayer` |
| 6 | R4 - Launch Pad | `LaunchPadBlock` 3×3 Check | Multi-block launch platform requiring center `LaunchPadBlock` surrounded by 8 `LaunchPadBaseBlock`s | `Level`, `BlockPos` | `isCompletePad(...)` boolean | Returns `false` if any neighbour block is missing/wrong | `com.amaro.stellarodyssey.block.LaunchPadBlock` |
| 7 | R4 - Rocket Deployment | `RocketItem.useOn` | Right-clicking Launch Pad deploys `RocketEntity` and consumes 1 item | `UseOnContext` on `LaunchPadBlock` | Spawns `RocketEntity` at `pos.above()` | Fails with chat message if pad incomplete | `com.amaro.stellarodyssey.item.RocketItem` |
| 8 | R4 - StarMap GUI | `StarMapScreen` | Fullscreen interactive celestial cartography screen with pan/zoom, grid, systems, and telemetry | `ICelestialCatalog` | Renderable interactive GUI | Empty catalog defaults to blank sector grid | `com.amaro.stellarodyssey.satellites.starmap.screen.StarMapScreen` |
| 9 | R4 - StarMap Satellite | `StarMapSatellite` SPI | Decoupled satellite module providing `openScreen(ICelestialCatalog)` with physical client guards | `openScreen(...)` invocation | Safely opens screen via `EnvExecutor.runInEnv` | No-ops on dedicated server | `com.amaro.stellarodyssey.satellites.starmap.StarMapSatellite` |
| 10 | R4 - Celestial Catalog | `CelestialBodyRegistry` | Central thread-safe catalog of charted planetary bodies (Proxima B, Exotic Prime, Nexus Moon, Gliese Deep) | Dimension ResourceKey | `ICelestialBody` record with gravity, atmosphere, system | Null returns `Optional.empty()` | `com.amaro.stellarodyssey.world.CelestialBodyRegistry` |
| 11 | Networking | `ModNetworking` & Architectury | NetworkManager registering C2S/S2C packet payloads | Custom packet codecs and payload types | Network serialization/deserialization | Unregistered payload drops or disconnects | `com.amaro.stellarodyssey.network.ModNetworking` |
| 12 | Build & Tests | Gradle Architectury Multi-loader | Root build orchestrating `:common`, `:fabric`, and `:neoforge` with JUnit 5 test runner | `./gradlew test`, `./gradlew build` | 39 unit tests passing 100% | Non-zero exit on assertion/compilation failure | `build.gradle`, `gradlew` |
| 13 | Vault Sync | Obsidian Vault Knowledge Base | Bi-directional note tracking with Antigravity and OpenCode in `Obsidian Vault/` | Markdown files in Obsidian Vault | Synchronized status, handoffs, and decisions | File lock conflict if edited concurrently | `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\` |

---

## 3. Edge Cases & Observed Behaviors

| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | Rocket Interaction | Player right-clicks empty rocket on Launch Pad | Player enters rocket as vehicle (`player.startRiding(this)`), but **no GUI opens** |
| 2 | Rocket Interaction | Player right-clicks rocket while already seated | Triggers launch sequence immediately to single hardcoded destination (`tier.destination()`) |
| 3 | Rocket Destination | Player in Tier 1 rocket wants to reach Proxima B | Currently impossible to select: rocket destination is immutable in `RocketTier` (fixed to `NEXUS_MOON`) |
| 4 | StarMap Navigation | Player clicks "PLOT COURSE" in `StarMapScreen` | Only updates client-side status text `navigationStatusMessage`; no packet sent, no tier check performed |
| 5 | Rocket Rendering | `state.tierLevel` is 2 or 3 | Renderer looks for `textures/entity/rocket_t2.png` or `rocket_t3.png`, which **do not exist** on disk; falls back to missing texture or base model |
| 6 | Rocket Emissives | Tier 2/3 rocket placed in complete dark (cave/night) | Rocket is rendered diffuse without any bioluminescent glow because `RocketEntityRenderer.submit` lacks `EmissiveModelLayer` calls |
| 7 | Dedicated Server GUI | `StarMapSatellite.openScreen(...)` called on server | Guarded by `Platform.getEnvironment() != Env.CLIENT` and `EnvExecutor`; safely logs warning and does not crash |
| 8 | Launch Pad Check | Rocket placed on single LaunchPadBlock without 8 base blocks | Placement fails; displays chat message `"The launch pad needs a complete 3x3 base!"` |
| 9 | Rocket Assembly | Mixing T1 and T2 components in Assembly Table | `AssemblyLogic.validate` returns `Optional.empty()`; result slot remains empty |

---

## 4. Deep Architectural Specification: R3 (Modelos 3D & Renderizado Cohetes Tier 2 & Tier 3)

### 4.1 Current Implementation & Missing Components
1. **Existing Code**:
   - `RocketModel.java`: Single Java model with `LAYER_LOCATION = new ModelLayerLocation(id("rocket"), "main")`.
   - `RocketEntityRenderer.java`: Only bakes `RocketModel.LAYER_LOCATION`. In `submit(...)`, it binds `textures/entity/rocket_t{tierLevel}.png` to `this.model`.
   - `RocketEntity.java`: Stores `DATA_TIER` (1..3).
2. **Missing Components**:
   - **Distinct 3D Geometry**: Tier 2 (Voyager) and Tier 3 (Odyssey) require visually distinct, escalating spaceframe geometries representing their advanced tech levels.
   - **Entity Textures for T2 and T3**: While `textures/item/rocket_t2.png` and `rocket_t3.png` exist for inventory icons, `textures/entity/rocket_t2.png` and `textures/entity/rocket_t3.png` **do not exist** in `assets/stellarodyssey/textures/entity/`.
   - **Emissive Overlay Textures**: Neither `textures/entity/rocket_t2_emissive.png` nor `textures/entity/rocket_t3_emissive.png` exists.
   - **Emissive Submission Pass**: `RocketEntityRenderer` does NOT invoke `EmissiveModelLayer.submitEmissive(...)`.

### 4.2 3D Model Hierarchy & UV Atlases Specifications

#### Tier 1: "Pioneer" (Earth / Overworld Progression)
- **Visual Design**: Sleek retro-futuristic single-stage booster with terrestrial materials (brushed steel, copper conduits, heat tiles).
- **Scale**: `modelScale = 0.8F`.
- **Dimensions**: Fuselage (12×24×12), nose cone (8×8×8), engine bell (4×2×4), 4 stabilizing fins (2×12×4), window observation strip (10×4×10).
- **UV Texture Layout**: 128×64 diffuse atlas (`textures/entity/rocket_t1.png`).
- **Emissive Pass**: None (terrestrial chemical propulsion, non-bioluminescent).

#### Tier 2: "Voyager" (Celidium Interplanetary Cruiser)
- **Visual Design**: Heavy interplanetary explorer reinforced with Celidium amber alloy. Features dual auxiliary side booster pods, an enlarged heat deflection nose cone, dual heavy engine nozzles, and exposed glowing Celidium energy conduits along the hull.
- **Scale**: `modelScale = 1.0F`.
- **Model Geometry Specifications**:
  - `fuselage`: Center column (14×26×14) at `texOffs(0, 0)`.
  - `nose_cone`: Stepped aerodynamic heat cone (10×10×10) at `texOffs(0, 40)`.
  - `booster_left`: Auxiliary fuel/warp pod (6×18×6) at `texOffs(56, 0)` positioned at `x = -10.0F`.
  - `booster_right`: Auxiliary fuel/warp pod (6×18×6) at `texOffs(56, 0)` positioned at `x = 10.0F`.
  - `dual_engines`: Twin vector bells (4×4×4 each) at `texOffs(80, 0)`.
  - `conduit_ribs`: 4 external Celidium power conduits running vertically at `texOffs(0, 60)`.
  - `fins`: 4 heavy swept aerodynamic stabilizers (2×14×6) at `texOffs(40, 40)`.
- **UV Texture Layout**: 128×64 (or 128×128) diffuse texture (`textures/entity/rocket_t2.png`) featuring amber/bronze plating (`#D97724`, `#FBBF77`, `#92400E`).
- **Emissive Layer Texture (`textures/entity/rocket_t2_emissive.png`)**:
  - Transparent background (`alpha = 0`).
  - Glowing pixels (`alpha = 255`) on:
    - Celidium energy conduits running along the fuselage ribs (`#FBBF77` amber-gold).
    - Twin engine nozzle ignition chambers (`#D97724` to `#FF8A2A` flame).
    - Navigation sensor array on the nose cone.

#### Tier 3: "Odyssey" (Astralite / Verdantite Interstellar Flagship)
- **Visual Design**: Interstellar hyperspace capital ship forged from Astralite (`#06B6D4`) and Verdantite (`#22C55E`). Quad-nacelle hyperdrive configuration, crystalline radiator wings, heavy tri-engine thruster cluster, and hermetic command bridge.
- **Scale**: `modelScale = 1.3F`.
- **Model Geometry Specifications**:
  - `command_fuselage`: Heavy reinforced hexagonal hull (16×28×16) at `texOffs(0, 0)`.
  - `hyperdrive_nose`: Crystalline focal dome (12×10×12) at `texOffs(0, 44)`.
  - `quad_nacelles`: 4 outboard warp pods (6×22×6 each) positioned radially at `(-12, 0, 0)`, `(12, 0, 0)`, `(0, 0, -12)`, `(0, 0, 12)` at `texOffs(64, 0)`.
  - `crystalline_radiators`: 4 Astralite crystalline radiator wings (1×16×8) at `texOffs(88, 0)`.
  - `tri_engine_cluster`: 3 heavy ion bells (5×4×5 each) in triangular formation at `texOffs(64, 28)`.
  - `bridge_cupola`: Panoramic pressurized command bridge (8×4×8) at `texOffs(48, 44)`.
- **UV Texture Layout**: 128×128 diffuse texture (`textures/entity/rocket_t3.png`) with cyan Astralite composite plating (`#06B6D4`) and emerald Verdantite trim (`#22C55E`).
- **Emissive Layer Texture (`textures/entity/rocket_t3_emissive.png`)**:
  - Transparent background (`alpha = 0`).
  - Glowing pixels (`alpha = 255`) on:
    - Astralite hyperdrive nacelle coils (`#06B6D4` / `#A5F3FC` celestial cyan).
    - Verdantite life-support power circuits (`#22C55E` / `#86EFAC` bio-emerald).
    - Tri-engine ion exhaust nozzles (`#00E5FF` high-energy plasma).
    - Bridge holographic HUD arrays.

### 4.3 Client Registration & `SubmitNodeCollector` Pipeline

1. **Model Layer Registrations**:
   In `StellarOdysseyClient.java`:
   ```java
   EntityModelLayerRegistry.register(RocketModel.LAYER_LOCATION_T1, RocketModel::createTier1Layer);
   EntityModelLayerRegistry.register(RocketModel.LAYER_LOCATION_T2, RocketModel::createTier2Layer);
   EntityModelLayerRegistry.register(RocketModel.LAYER_LOCATION_T3, RocketModel::createTier3Layer);
   ```
2. **Renderer Multi-Model Baking**:
   `RocketEntityRenderer` bakes all three tier models during context initialization:
   ```java
   this.modelT1 = new RocketModel(context.bakeLayer(RocketModel.LAYER_LOCATION_T1));
   this.modelT2 = new RocketTier2Model(context.bakeLayer(RocketModel.LAYER_LOCATION_T2));
   this.modelT3 = new RocketTier3Model(context.bakeLayer(RocketModel.LAYER_LOCATION_T3));
   ```
3. **Execution in `submit(...)`**:
   ```java
   // 1. Select model and scale based on tier
   RocketTier tier = RocketTierRegistry.getTier(state.tierLevel).orElse(RocketTiers.TIER_1.get());
   float scale = tier.modelScale();
   poseStack.scale(-scale, -scale, scale);

   // 2. Diffuse Base Pass
   Identifier diffuseTexture = StellarOdyssey.id("textures/entity/rocket_t" + state.tierLevel + ".png");
   collector.submitModel(activeModel, state, poseStack, diffuseTexture, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

   // 3. Full-Bright Emissive Overlay Pass
   if (state.tierLevel >= 2) {
       Identifier emissiveTexture = StellarOdyssey.id("textures/entity/rocket_t" + state.tierLevel + "_emissive.png");
       EmissiveModelLayer.submitEmissive(activeModel, state, poseStack, collector, emissiveTexture);
   }
   ```

---

## 5. Deep Architectural Specification: R4 (StarMap GUI & Selección de Destino)

### 5.1 Launch Pad Mount Interaction & Screen Trigger
1. **Current Flaw**: When `player.startRiding(rocket)` executes, no screen opens. Subsequent right-clicks launch the rocket directly to its default dimension.
2. **Architectural Solution**:
   - When a player mounts a `RocketEntity` resting on a valid `LaunchPadBlock`:
     - **Option A (Network S2C Packet)**: Server detects successful `startRiding`, checks `isCompletePad` underneath, and sends `OpenRocketStarMapPayload(rocketEntityId, rocketTierLevel)` to the player.
     - **Option B (Client Vehicle Mount Event / Probe)**: Client detects that `Minecraft.getInstance().player.getVehicle() instanceof RocketEntity rocket`, and if `!rocket.isLaunching()` and the screen is not already open, opens `StarMapScreen`.
   - **Recommendation (Option A - Server Controlled)**: Cleanest for multi-player synchronization and side-safety. Server confirms the pad is valid and dispatches `OpenRocketStarMapPayload`. The client receiver invokes `StarMapSatellite.openScreen(...)` passing the rocket context.

### 5.2 Strict Tier Validation Matrix

The core progression rule is that each rocket tier strictly defines which celestial bodies can be navigated:

| Celestial Body | Dimension Key | Star System | Min Rocket Tier Required | Unlocking Mineral / Technology |
|---|---|---|---|---|
| **Nexus Moon** | `stellarodyssey:nexus_moon` | Alpha Centauri | **Tier 1 (Pioneer)** | Overworld materials (Iron, Copper, Coal) |
| **Proxima B** | `stellarodyssey:proxima_b` | Alpha Centauri | **Tier 2 (Voyager)** | Celidium Ingot (mined on Nexus Moon) |
| **Exotic Prime** | `stellarodyssey:exotic_prime` | Kepler-452 | **Tier 3 (Odyssey)** | Verdantite & Astralite (mined on Proxima B) |
| **Gliese Deep** | `stellarodyssey:gliese_deep` | Gliese 667 | **Tier 3 (Odyssey)** | High-gravity super-alloy hull |
| **Overworld** | `minecraft:overworld` | Sol | **Tier 1 (Any)** | Return trip from any alien dimension |

### 5.3 StarMapScreen UI Modifications for Destination Selection
1. **Rocket Mode State**:
   `StarMapScreen` must receive the mounting `rocketTier` (int 1..3) and `rocketEntityId`.
2. **Visual Destination Status**:
   For each celestial body rendered in `StarMapSkyRenderer.renderCelestialNode(...)`:
   - If `rocketTier >= body.minTier()`:
     - Reticle: Neon cyan/green `[UNLOCKED]`.
     - Clicking enables the "ENGAGE LAUNCH SEQUENCE" button.
   - If `rocketTier < body.minTier()`:
     - Reticle: Red lock icon `[LOCKED - REQUIRES TIER X]`.
     - Tooltip displays warning: `§cRequires Tier X Rocket (${tierName})`.
     - "ENGAGE LAUNCH SEQUENCE" button is **disabled** (greyed out).
3. **Launch Execution Button**:
   - Replaces or augments "PLOT COURSE" with "ENGAGE LAUNCH SEQUENCE".
   - When clicked on a valid, unlocked destination:
     - Sends `SelectRocketDestinationPayload(rocketEntityId, selectedDimensionKey)` from client to server.
     - Closes `StarMapScreen`.

### 5.4 Network Protocol & Server-Side Security Validation
1. **Packet: `SelectRocketDestinationPayload` (C2S)**:
   - Fields: `int entityId`, `ResourceKey<Level> destinationDimension`.
   - Codec registered via Architectury `NetworkManager.registerC2S(...)`.
2. **Server-Side Validation Handler**:
   When the packet arrives on the server:
   - Verify sender is `ServerPlayer player`.
   - Retrieve entity by `entityId`. Verify entity is `RocketEntity rocket`.
   - Verify `player.getVehicle() == rocket` (prevent remote injection).
   - Verify `rocket.isLaunching() == false`.
   - Verify `LaunchPadBlock.isCompletePad(level, rocket.blockPosition().below())`.
   - Verify `rocket.getTierLevel() >= getRequiredTier(destinationDimension)`.
   - If checks pass:
     - Set target destination on rocket: `rocket.setTargetDestination(destinationDimension)`.
     - Trigger launch sequence: `rocket.setLaunching(true)`, `rocket.setLaunchTicks(0)`.
     - Play liftoff sound `ModSoundEvents.STARSHIP_THRUST.get()`.
   - If validation fails:
     - Send warning message to player chat and abort.

---

## 6. Build, Tests & Acceptance Criteria

### 6.1 Gradle Build & Verification Commands
- **Compile all loaders**:
  ```bash
  ./gradlew compileJava --console=plain
  ```
  Must compile `:common`, `:fabric`, and `:neoforge` with exit code 0.
- **Run all JUnit tests**:
  ```bash
  ./gradlew test --console=plain
  ```
  Must execute and pass 100% of tests. Currently 39 tests passing.
- **Full multi-loader build**:
  ```bash
  ./gradlew build --console=plain
  ```

### 6.2 Existing Tests Inventory (39 tests passing)
1. `AlienMineralTierExtensibilityTest`: 7 tests verifying runtime tier registration, duplicate prevention, and properties.
2. `AlienMineralTierMatrixTest`: 12 tests verifying canonical Tiers 1-5, tags, hardness, and durability.
3. `AssemblyLogicTest`: 4 tests verifying component assembly by tier.
4. `CoreLifecycleAndConstantsTest`: 8 tests verifying lifecycle stages and mod constants.
5. `DecoupledSatellitesContractTest`: 5 tests verifying SPI discovery, priority ordering (10 > 5 > 0), and DAG decoupling (zero cross-satellite imports).
6. `ModRegistriesBindingTest`: 3 tests verifying deferred registers.

### 6.3 Required New Test Suites for R3 & R4
1. **`RocketTierDestinationValidationTest`**:
   - Verify Tier 1 can target Nexus Moon, but rejects Proxima B and Exotic Prime.
   - Verify Tier 2 can target Nexus Moon and Proxima B, but rejects Exotic Prime.
   - Verify Tier 3 can target Nexus Moon, Proxima B, Exotic Prime, and Gliese Deep.
   - Verify invalid or unknown dimension keys are rejected.
2. **`RocketAssemblyProgressionTest`**:
   - Verify 4 components for T1, 6 components for T2, 8 components for T3.
3. **`RocketModelLayerRegistrationTest`**:
   - Verify model layer locations are non-null and distinct for T1, T2, and T3.

---

## 7. Obsidian Vault Synchronization Protocol

### 7.1 Target Obsidian Vault Topology
Path: `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\`

```
C:\Users\amaro\OneDrive\Documents\Obsidian Vault\
├── .obsidian/
├── Agentes/
│   ├── DECISIONES.md          <- Shared architectural decisions log
│   ├── HANDOFFS/
│   │   ├── antigravity.md     <- Deliverable reports from Antigravity to OpenCode
│   │   └── opencode.md        <- Deliverable reports from OpenCode to Antigravity
│   ├── INBOX.md               <- Inter-brain coordination messages
│   └── STATUS.md              <- Live task claims and brain statuses
├── Arquitectura & Subagentes.md <- Package structure, DAG SPI, and worker team
├── Progreso & Checkpoints.md    <- Texture status, test counts, build logs
└── Stellar Odyssey - Proyecto.md <- Project overview, Minecraft version, pillars
```

### 7.2 Synchronization Requirements for This Milestone
Whenever features R3 and R4 are implemented or updated:
1. **`Agentes/STATUS.md`**: Update Antigravity's current task to R3/R4 implementation and log unblocked status.
2. **`Agentes/HANDOFFS/antigravity.md`**: Record the completed survey/spec findings, files analyzed, and handoff to OpenCode.
3. **`Agentes/DECISIONES.md`**: Record decisions:
   - "Cohetes T2 y T3 incorporan modelos 3D y atlas UV independientes con capas emisivas SubmitNodeCollector vinculadas a Celidium (ámbar) y Astralite/Verdantite (cian/esmeralda)."
   - "Al montar el cohete sobre la Launch Pad 3x3, se despliega automáticamente el StarMapScreen validando el Tier del cohete (T1 -> Luna; T2 -> Proxima B + Luna; T3 -> Exotic Prime + Proxima B + Luna)."
4. **`Progreso & Checkpoints.md`**:
   - Document new 3D model layers and emissive texture integrations.
   - Document StarMap destination selection packet and test suite results.
5. **`Arquitectura & Subagentes.md`**:
   - Update worker mapping and package layout for `rocket` and `satellites.starmap`.

---

## 8. Summary of Actionable Implementation Tasks (For Next Workers)

1. **Task 1 (R3 - 3D Models & UV Atlases)**:
   - Create `RocketTier2Model.java` (Voyager) with dual boosters and Celidium conduits.
   - Create `RocketTier3Model.java` (Odyssey) with quad warp nacelles and Astralite radiator fins.
   - Register both model layer definitions in `StellarOdysseyClient.java`.
   - Implement `rocket_t2.png`, `rocket_t3.png`, `rocket_t2_emissive.png`, `rocket_t3_emissive.png` in `TextureGen.java` and generate PNG assets.
2. **Task 2 (R3 - Emissive Renderer Integration)**:
   - Update `RocketEntityRenderer.java` to bake and switch between `modelT1`, `modelT2`, `modelT3`.
   - Apply `poseStack.scale` from `tier.modelScale()`.
   - Call `EmissiveModelLayer.submitEmissive(...)` for Tier 2 and Tier 3.
3. **Task 3 (R4 - Networking & Mount Trigger)**:
   - Create `SelectRocketDestinationPayload` (C2S) and `OpenRocketStarMapPayload` (S2C).
   - Register payloads in `ModNetworking.java`.
   - Update `RocketEntity.java` to support dynamic destination (`DATA_DESTINATION` or entity field) and trigger `OpenRocketStarMapPayload` upon player mounting on a complete 3×3 pad.
4. **Task 4 (R4 - StarMap UI & Tier Validation)**:
   - Update `StarMapScreen.java` to accept rocket context (tier level and entity ID).
   - Display tier requirement badges and lock indicators on planetary nodes.
   - Replace/augment "PLOT COURSE" with "ENGAGE LAUNCH SEQUENCE", sending `SelectRocketDestinationPayload` when clicked on valid unlocked destinations.
5. **Task 5 (Verification & Tests)**:
   - Add `RocketDestinationValidationTest.java` to verify tier access rules.
   - Run `./gradlew test` and verify 100% pass rate.
   - Sync notes into `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\`.
