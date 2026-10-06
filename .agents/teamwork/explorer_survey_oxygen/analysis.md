# Technical Analysis: Oxygen & Planetary Atmosphere (R1)

## Executive Summary
This report provides a comprehensive architectural survey and implementation roadmap for **R1 (Oxygen & Planetary Atmosphere System)** in the Stellar Odyssey mod (Minecraft 26.3, Java 25, Architectury Loom multi-loader). 

Currently, a basic prototype of life support exists in `lifesupport/`, `item/OxygenTankItem.java`, `item/SpacesuitItem.java`, and `client/gui/OxygenHudOverlay.java`. However:
1. Planetary atmospheric state is currently hardcoded as a blanket vacuum for all `stellarodyssey:*` dimensions, bypassing the detailed physical parameters in `CelestialBodyRegistry` (e.g., distinguishing between hard vacuum decompression on Nexus Moon and toxic atmosphere suffocation on Exotic Prime).
2. OxygenTankItem consumption scans the raw inventory without requiring the Spacesuit Chestplate (the canonical "Oxygen Manifold"), and partial suit states treat any missing piece identically to being naked in hard vacuum.
3. Neither **Oxygen Sealer** nor **Oxygen Refill Station** exists in the codebase (`ModBlocks`, `ModItems`, `ModBlockEntityTypes`, or assets).
4. No unit tests exist for life support, oxygen tanks, or atmospheric conditions.

---

## 1. Planetary Atmosphere & O2 Degradation Mechanics

### 1.1 Current Architecture & Call Chain
Atmospheric safety is currently evaluated by two classes:
- `com.amaro.stellarodyssey.lifesupport.AtmosphereHelper`
- `com.amaro.stellarodyssey.lifesupport.LifeSupportManager` (ticked via Architectury `TickEvent.PLAYER_POST.register(...)`)

#### Current Logic in `AtmosphereHelper.java`:
```java
// AtmosphereHelper.java:34-49
public static boolean isVacuumEnvironment(Player player) {
    Level level = player.level();
    if (player.getY() >= VACUUM_ALTITUDE_THRESHOLD) { // Y >= 320
        return true;
    }
    if (level.dimensionTypeRegistration().is(VACUUM_DIMENSIONS)) { // #stellarodyssey:vacuum tag
        return true;
    }
    return level.dimension().identifier().getNamespace().equals(StellarOdyssey.MOD_ID); // Blanket check
}

public static boolean lacksOxygen(Player player) {
    return player.isEyeInFluid(FluidTags.WATER) || isVacuumEnvironment(player);
}
```

#### Current Degradation in `LifeSupportManager.java`:
Runs on server every 20 ticks (1 second):
- **If `inVacuum && !suitSealed`**: Player receives 3.0F drown damage, `airSupply` is set to `-20`, and `ModSoundEvents.DECOMPRESSION_ALARM` plays every 40 ticks.
- **Else if `!firstAvailableTank.isEmpty()`**: Drains 1 unit from the first tank found in player inventory; restores `airSupply` to maximum.
- **Else (empty tanks / unprotected)**: Reduces `airSupply` by 15. When `airSupply <= -20`, inflicts 2.0F drown damage and resets `airSupply` to 0.

### 1.2 Discrepancies and Planetary Differences
In `CelestialBodyRegistry.java`, each dimension has distinct physical and atmospheric properties:
| Body / Dimension | Pressure (`atm`) | Breathable | Radiation | Nature |
|---|---|---|---|---|
| **Nexus Moon** (`nexus_moon`) | `0.00f` | `false` | `3.20f` | True Hard Vacuum (< 0.05 atm). Explosive decompression hazard. |
| **Proxima B** (`proxima_b`) | `0.15f` | `false` | `2.40f` | Thin unbreathable atmosphere, low pressure, solar flares. |
| **Exotic Prime** (`exotic_prime`) | `0.85f` | `false` | `1.10f` | Dense atmosphere (~0.85 atm), xenomorphic flora, toxic/unbreathable. |
| **Gliese Deep** (`gliese_deep`) | `2.50f` | `false` | `0.80f` | Hyper-dense, high pressure, toxic. |

#### Architectural Gaps:
1. **Conflation of Vacuum vs. Unbreathable**:
   Because `AtmosphereHelper.isVacuumEnvironment` returns `true` for all mod dimensions, a player on Exotic Prime (pressure 0.85 atm) who removes their boots is treated as being in hard vacuum—triggering decompression alarms and explosive decompression damage. In reality, Exotic Prime has adequate pressure but toxic/non-breathable air: removing boots should not cause decompression, but removing the helmet should cause toxic inhalation/asphyxiation.
2. **Disconnected Catalog**:
   `AtmosphereHelper` does not consult `CelestialBodyRegistry.getInstance().getBody(level.dimension())` or `ICelestialBody.hasBreathableAtmosphere()`.
3. **No Sealed Room Awareness**:
   There is currently no hook in `AtmosphereHelper` to recognize when a player is standing in an artificial, pressurized, oxygenated habitat.

### 1.3 Recommended Atmospheric Decision Matrix
```
Player in Dimension
 │
 ├── 1. Inside Sealed Room (Oxygen Sealer active)? ──────────────> SAFE (Nominal O2, 0 drain)
 │
 ├── 2. Native Atmosphere Breathable? (ICelestialBody.hasBreathableAtmosphere) -> SAFE
 │
 ├── 3. Hard Vacuum? (Y >= 320 OR body.isVacuum() / pressure < 0.05 atm)
 │    ├── Full Sealed Spacesuit (4/4)? ─────────────────────────> SAFE (Consumes 1 O2/s)
 │    ├── Partial Suit (e.g. 3/4)? ─────────────────────────────> LEAK (Consumes 2-3 O2/s + breach alert)
 │    └── Missing Helmet / Unsealed? ───────────────────────────> DECOMPRESSION DAMAGE (3.0F/s + Klaxon)
 │
 └── 4. Unbreathable / Toxic Atmosphere (pressure >= 0.05 atm, not breathable)
      ├── Helmet + Chestplate (Manifold) equipped? ────────────> SAFE (Consumes 1 O2/s)
      └── Missing Helmet? ──────────────────────────────────────> ASPHYXIATION DAMAGE (Air drops by 15/s)
```

---

## 2. Spacesuit Equipment & OxygenTankItem Coupling

### 2.1 Spacesuit Equipment Structure
In `com.amaro.stellarodyssey.item.SpacesuitItem`:
- Registered as standard armor items with material `ModArmorMaterials.SPACESUIT`:
  - `spacesuit_helmet` (`EquipmentSlot.HEAD`)
  - `spacesuit_chestplate` (`EquipmentSlot.CHEST`)
  - `spacesuit_leggings` (`EquipmentSlot.LEGS`)
  - `spacesuit_boots` (`EquipmentSlot.FEET`)
- Checked via `AtmosphereHelper.isFullSuitEquipped(Player player)` using exact `ArmorType` matching across all 4 armor slots.

### 2.2 Lore & Design Intent from Localization
The item tooltips defined in `en_us.json` establish the exact subsystem role for each piece:
- **Helmet**: `"Pressurized Visor & Environmental Scanner"` — Seals head unit and provides HUD telemetry.
- **Chestplate**: `"Oxygen Manifold & Atmospheric Regulator"` — **"Distributes carried O2 tanks across the suit."**
- **Leggings**: `"Thermal & Radiation Plating"` — Thermal flux and cosmic ray protection.
- **Boots**: `"Gravitational Dampeners & Micro-Thrusters"` — Low-gravity impact absorption.
- **Full Set**: `"◆ Full Set: Hermetic Seal (Vacuum & Decompression Protection)"`.

### 2.3 OxygenTankItem Durability Storage
`com.amaro.stellarodyssey.item.OxygenTankItem` stores oxygen using Minecraft's native item damage system:
- `CAPACITY = 600` (600 seconds = 10 minutes of nominal respiration).
- Durability in `ModItems.java`: `.durability(OxygenTankItem.CAPACITY)`.
- Damage value `0` = 600 O2 units (full).
- Damage value `600` = 0 O2 units (empty).
- Durability bar automatically renders in inventory without custom packet sync.
- `drain(ItemStack stack, int amount)` safely increases damage without destroying the item (unlike `hurtAndBreak`).
- `fill(ItemStack stack, int amount)` reduces damage towards 0.

### 2.4 Coupling Mechanics ("Acoplado al equipamiento")
#### Current Implementation Flaw:
`LifeSupportManager` iterates through the whole inventory (`player.getInventory()`) and drains the tank regardless of whether the player is even wearing a chestplate or helmet. If a player is underwater naked, carrying a tank in their hotbar restores their air supply!

#### Recommended Coupling Architecture:
1. **Functional Coupling via the Oxygen Manifold (Chestplate)**:
   - To draw oxygen from carried `OxygenTankItem`s in the inventory, the player **must** wear the `SPACESUIT_CHESTPLATE` (the manifold).
   - To breathe the oxygen, the player **must** wear the `SPACESUIT_HELMET`.
   - If either is missing, carried oxygen tanks cannot be utilized.
2. **Full Suit vs. Partial Suit Degradation Matrix**:
   - **4/4 Pieces (Full Hermetic Seal)**:
     - 100% vacuum protection.
     - Nominal oxygen consumption: **1 unit / sec** (20 ticks).
     - Visor HUD shows cyan nominal indicator: `● SUIT SEALED [NOMINAL]`.
   - **3/4 Pieces (Minor Seal Breach, e.g. missing boots or leggings)**:
     - In vacuum: Pressurized air leaks into space through the unsealed junction.
     - Degradation rate: **2x consumption (2 units / sec)** while tanks last.
     - HUD warning: `⚠ SEAL BREACH [INCREASED O2 DRAIN]`.
     - Decompression damage delayed or softened while tank actively compensates.
   - **< 3 Pieces or Missing Helmet / Chestplate in Vacuum**:
     - Catastrophic decompression breach: Cannot maintain pressure.
     - Klaxon alarm sounds (`hazard.decompression_alarm`), decompression damage (3.0F/s).
   - **Exhausted Tanks (0 O2 remaining)**:
     - Once all tanks are depleted (`totalOxygen == 0`), suit runs out of air.
     - Player air supply drops by 15 per second until suffocation/drowning occurs.

---

## 3. Oxygen Sealer & Oxygen Refill Station (Blocks & Logic)

### 3.1 Current Status: None Existing
Searching for `Sealer` or `Refill` across `common/src/main/` returns 0 occurrences. Both blocks and their corresponding block entities, block items, recipes, blockstates, models, and descriptors must be designed from scratch.

### 3.2 Block 1: Oxygen Refill Station (`OxygenRefillerBlock`)
#### Purpose:
Recharges portable `OxygenTankItem`s and player suit systems.

#### Registration Contracts:
- `ModBlocks.OXYGEN_REFILLER`: Properties `strength(3.5F, 6.0F)`, `requiresCorrectToolForDrops()`, `sound(SoundType.METAL)`, `mapColor(MapColor.COLOR_CYAN)`.
- `ModItems.OXYGEN_REFILLER`: `BlockItem` registered with `ModCreativeTabs.MAIN`.

#### Gameplay Mechanics:
1. **Right-Click with OxygenTankItem**:
   - Checks if tank damage > 0 (`getOxygen(stack) < CAPACITY`).
   - Calls `OxygenTankItem.fill(held, OxygenTankItem.CAPACITY)`.
   - Plays pressurized sound (`SoundEvents.BREWING_STAND_BREW` / hiss) and emits gas particles (`ParticleTypes.CLOUD` / `WHITE_SMOKE`).
   - Displays actionbar confirmation: `"Oxygen Tank refilled to 100%"`.
2. **Right-Click with Empty Hand (Wearing Spacesuit)**:
   - Scans player inventory for any partially empty `OxygenTankItem`s and refills them.
3. **Optional Automation**:
   - Can optionally have `OxygenRefillerBlockEntity` with hopper support (input top/sides, output bottom).

### 3.3 Block 2: Oxygen Sealer (`OxygenSealerBlock` & `OxygenSealerBlockEntity`)
#### Purpose:
Pressurizes and oxygenates an enclosed habitat on airless moons (Nexus Moon) or toxic exoplanets (Proxima B, Exotic Prime). When active, players inside the sealed room can remove their spacesuits, breathe normally, and consume 0 tank oxygen.

#### Registration Contracts:
- `ModBlocks.OXYGEN_SEALER`: Solid metal machine block. Has boolean blockstate property `SEALED` (or `ACTIVE`).
- `ModItems.OXYGEN_SEALER`: `BlockItem` registered in `ModItems`.
- `ModBlockEntityTypes.OXYGEN_SEALER`: Block entity supplier bound to `ModBlocks.OXYGEN_SEALER`.

#### Room Sealing Algorithm (Breadth-First Search Flood-Fill):
1. **Execution Scope**:
   - Runs strictly on the **server side** (`!level.isClientSide()`).
   - Ticked periodically (e.g. every 40–80 game ticks, or triggered by neighbor block updates) to maintain high server TPS.
2. **Flood-Fill Parameters**:
   - `MAX_VOLUME`: 1,024 blocks (configurable up to 2,048).
   - `MAX_RADIUS`: 16 blocks horizontal, 10 blocks vertical.
3. **Sealing Barrier Check**:
   - A block position is a **barrier** if:
     - `state.isSolidRender()` is true, OR
     - `state.getBlock() instanceof DoorBlock` (closed), `TrapDoorBlock` (closed), or glass/tinted glass.
   - A block position is an **open leak** if:
     - BFS visits open sky (`level.canSeeSky(pos)` when at ceiling level), OR
     - Visited block count exceeds `MAX_VOLUME`, OR
     - Visited coordinate exceeds `MAX_RADIUS`.
4. **Outcome**:
   - **If leak detected**: `isSealed = false`. Blockstate property `SEALED = false`.
   - **If fully enclosed within volume limit**: `isSealed = true`. Blockstate property `SEALED = true`. Interior coordinates cached in a `Set<BlockPos>` or bounding box `AABB`.

### 3.4 Multi-Loader Side Safety & Memory Leak Prevention
To guarantee zero memory leaks and multi-loader compliance (Fabric + NeoForge):
1. **Never store static `Level` or `Player` references**:
   - Keep sealer tracking strictly within the `OxygenSealerBlockEntity` instance.
   - When the chunk unloads, `OxygenSealerBlockEntity.setRemoved()` is invoked: cleanly remove the sealer from any level manager or cache.
2. **Level-Scoped Manager via `SavedData` or Level Capability**:
   - Use a lightweight server-level tracker or iterate active sealers in loaded chunks for the player's position.
   - Fast check: Test if `player.blockPosition()` is within `sealer.getBoundingBox()` first, before checking the exact `Set<BlockPos>`.
3. **MC 26.3 Block Entity Serialization**:
   - Use `loadAdditional(ValueInput input)` and `saveAdditional(ValueOutput output)` (MC 26.3 API, not deprecated `CompoundTag`).
4. **Client-Side HUD Integration**:
   - Client does **not** execute flood-fills.
   - When server determines the player is within a sealed room, `LifeSupportManager` sends `OxygenSyncPayload(totalOxygen, maxCapacity, false /* inHazard = false */)`.
   - Client HUD receives `inHazard = false`, immediately showing `● ROOM SEALED [PRESSURIZED]` or safely hiding.

### 3.5 MC 26.3 Asset & Resource Requirements
As documented in `RESUMEN_PARA_OTRA_IA.md` (critical MC 26.3 changes):
1. **Item Descriptors (`assets/stellarodyssey/items/*.json`)**:
   - Must create `items/oxygen_sealer.json` and `items/oxygen_refiller.json`:
     ```json
     { "model": { "type": "minecraft:model", "model": "stellarodyssey:item/oxygen_sealer" } }
     ```
2. **Blockstates & Models**:
   - `blockstates/oxygen_sealer.json` (variants for `sealed=true/false`)
   - `blockstates/oxygen_refiller.json`
   - `models/block/oxygen_sealer.json`
   - `models/block/oxygen_refiller.json`
   - `models/item/oxygen_sealer.json`
   - `models/item/oxygen_refiller.json`
3. **Textures**:
   - 32x32 textures in `assets/stellarodyssey/textures/block/` (compatible with `tools/TextureGen.java`).
4. **Loot Tables**:
   - Standard block drop loot tables in `data/stellarodyssey/loot_table/blocks/`.
5. **Localization**:
   - Keys in both `en_us.json` and `es_es.json`.

---

## 4. Existing Tests and Test Strategy for R1

### 4.1 Current Test Baseline
- **Total Tests**: 34 unit tests in `common/src/test/java/com/amaro/stellarodyssey/`.
- **Existing Coverage**:
  - `AlienMineralTierMatrixTest.java` (Tiers 1-5, hardness, resistance, sound properties)
  - `AlienMineralTierExtensibilityTest.java` (Dynamic registration of alien mineral tiers)
  - `AssemblyLogicTest.java` (Rocket assembly matrix validation)
  - `CoreLifecycleAndConstantsTest.java` (Lifecycle manager, ModConstants)
  - `DecoupledSatellitesContractTest.java` (DAG decoupling and SPI priority)
  - `ModRegistriesBindingTest.java` (DeferredRegister binding and 1:1 Block/Item parity)
- **Current Life Support Coverage**: **0 tests**.

### 4.2 Comprehensive Test Plan for R1

```
common/src/test/java/com/amaro/stellarodyssey/lifesupport/
├── OxygenTankItemTest.java
├── AtmosphereHelperTest.java
├── CelestialAtmosphereCatalogTest.java
└── OxygenSealerAlgorithmTest.java
```

#### Test Suite 1: `OxygenTankItemTest`
- `testCapacityAndDurabilityParity()`: Verify `CAPACITY == 600`, undamaged tank returns 600 units, damage 600 returns 0.
- `testDrainExact()`: Draining 10 units reduces remaining oxygen to 590; draining 700 units from 600 caps at 600 damage and returns 600.
- `testFillExact()`: Filling 50 units into an empty tank reduces damage from 600 to 550; filling 1000 units into full tank returns 0 and does not overfill.
- `testDurabilityNeverBreaksItem()`: Verify draining to 0 oxygen leaves item at damage = 600 without item deletion.

#### Test Suite 2: `AtmosphereHelperTest`
- `testAltitudeThreshold()`: Player at Y=319 is below threshold; player at Y=320 is in vacuum.
- `testSpacesuitArmorPieceMatching()`: Verify `isSpacesuitPiece` correctly validates matching `ArmorType` (HELMET, CHESTPLATE, LEGGINGS, BOOTS) and rejects empty or mismatched stacks.
- `testFullSuitEquipped()`: Verify returns `true` if and only if all 4 armor slots contain valid spacesuit items.
- `testPartialSuitEquipmentCount()`: Helper to verify counting equipped pieces (0 to 4) for scaled leak degradation.

#### Test Suite 3: `CelestialAtmosphereCatalogTest`
- Verify catalog entries for charted celestial bodies:
  - `NEXUS_MOON`: `isVacuum() == true`, `atmosphericPressure() == 0.00f`, `hasBreathableAtmosphere() == false`.
  - `PROXIMA_B`: `isVacuum() == false`, `atmosphericPressure() == 0.15f`, `hasBreathableAtmosphere() == false`.
  - `EXOTIC_PRIME`: `isVacuum() == false`, `atmosphericPressure() == 0.85f`, `hasBreathableAtmosphere() == false`.
  - Verify that `isHazardous()` is `true` for all three.

#### Test Suite 4: `OxygenSealerAlgorithmTest`
Pure algorithmic unit test simulating room volumes:
- `testEnclosedRoomSeals()`: A 3x3x3 hollow cube of solid blocks with an air interior correctly resolves to `SEALED`.
- `testRoomWithHoleFails()`: A cube missing 1 block allows BFS to leak outside -> resolves to `LEAK_DETECTED`.
- `testRoomExceedingMaxVolumeFails()`: A 20x20x20 open room exceeds `MAX_VOLUME` (1024) -> resolves to `VOLUME_EXCEEDED / LEAK`.

#### Test Suite 5: Registry Parity Test Extension
- Update `ModRegistriesBindingTest.java`:
  - Assert `ModBlocks.OXYGEN_SEALER` and `ModBlocks.OXYGEN_REFILLER` exist.
  - Assert matching items `ModItems.OXYGEN_SEALER` and `ModItems.OXYGEN_REFILLER` exist.
  - Verify `ModBlockEntityTypes.OXYGEN_SEALER` is bound.

---

## 5. Architectural Implementation Roadmap

1. **Phase 1: Atmospheric Refinement (`AtmosphereHelper` & `CelestialBodyRegistry`)**:
   - Connect `AtmosphereHelper` to `CelestialBodyRegistry.getInstance()`.
   - Differentiate hard vacuum decompression (Nexus Moon, orbit) from unbreathable/toxic atmosphere (Proxima B, Exotic Prime).
   - Add hook for active sealed rooms.
2. **Phase 2: Spacesuit Subsystems & Oxygen Drain Scaling**:
   - Enforce Chestplate (Oxygen Manifold) requirement for drawing O2 from inventory tanks.
   - Enforce Helmet requirement for breathing O2.
   - Implement partial suit leak rate (e.g. 2x drain when 3/4 pieces equipped).
3. **Phase 3: Oxygen Refiller Station**:
   - Register `ModBlocks.OXYGEN_REFILLER` and `ModItems.OXYGEN_REFILLER`.
   - Implement right-click refill interaction.
   - Add assets (MC 26.3 item descriptor, blockstate, models, textures, lang).
4. **Phase 4: Oxygen Sealer Block & BlockEntity**:
   - Register `ModBlocks.OXYGEN_SEALER`, `ModItems.OXYGEN_SEALER`, `ModBlockEntityTypes.OXYGEN_SEALER`.
   - Implement BFS flood-fill algorithm in `OxygenSealerBlockEntity` with bounded radius and volume.
   - Connect sealed volume check to `AtmosphereHelper`.
   - Add assets (MC 26.3 item descriptor, blockstate, models, textures, lang).
5. **Phase 5: Unit Testing & Verification**:
   - Implement `OxygenTankItemTest`, `AtmosphereHelperTest`, `CelestialAtmosphereCatalogTest`, and `OxygenSealerAlgorithmTest`.
   - Verify `./gradlew test` passes 100% and `./gradlew build` succeeds on common, fabric, and neoforge.
