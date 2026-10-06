# Handoff Report: Survey Oxygen & Planetary Atmosphere (R1)

## 1. Observation

1. **Atmosphere check in `AtmosphereHelper.java:34-49`**:
   ```java
   public static boolean isVacuumEnvironment(Player player) {
       Level level = player.level();
       if (player.getY() >= VACUUM_ALTITUDE_THRESHOLD) {
           return true;
       }
       if (level.dimensionTypeRegistration().is(VACUUM_DIMENSIONS)) {
           return true;
       }
       return level.dimension().identifier().getNamespace().equals(StellarOdyssey.MOD_ID);
   }
   ```
   Every dimension with namespace `stellarodyssey` is unconditionally classified as a vacuum environment regardless of physical parameters.

2. **Planetary properties in `CelestialBodyRegistry.java:49-80`**:
   - `PROXIMA_B`: `atmosphericPressure = 0.15f`, `hasBreathableAtmosphere = false`.
   - `EXOTIC_PRIME`: `atmosphericPressure = 0.85f`, `hasBreathableAtmosphere = false`.
   - `NEXUS_MOON`: `atmosphericPressure = 0.00f`, `hasBreathableAtmosphere = false`.
   - `GLIESE_DEEP`: `atmosphericPressure = 2.50f`, `hasBreathableAtmosphere = false`.
   `ICelestialBody.isVacuum()` defines vacuum as `atmosphericPressure() < 0.05f`. `AtmosphereHelper` does not query `CelestialBodyRegistry`.

3. **Life support tick in `LifeSupportManager.java:39-85`**:
   ```java
   for (int i = 0; i < inv.getContainerSize(); i++) {
       ItemStack stack = inv.getItem(i);
       if (stack.is(ModItems.OXYGEN_TANK.get())) {
           int o2 = OxygenTankItem.getOxygen(stack);
           totalOxygen += o2;
           maxCapacity += OxygenTankItem.CAPACITY;
           if (firstAvailableTank.isEmpty() && o2 > 0) {
               firstAvailableTank = stack;
           }
       }
   }
   ```
   Tanks anywhere in player inventory provide breathable oxygen even if the player is not wearing spacesuit armor (e.g. underwater). Only in hard vacuum does `!suitSealed` trigger decompression damage (3.0F drown damage + klaxon sound).

4. **Spacesuit item definitions & lore in `en_us.json:17-25` and `SpacesuitItem.java`**:
   - Helmet: `"Pressurized Visor & Environmental Scanner"`
   - Chestplate: `"Oxygen Manifold & Atmospheric Regulator"` / `"Distributes carried O2 tanks across the suit."`
   - Set Bonus: `"◆ Full Set: Hermetic Seal (Vacuum & Decompression Protection)"`
   `AtmosphereHelper.isFullSuitEquipped` strictly checks `ArmorType` across all 4 slots. There is no partial suit scaling or manifold check.

5. **Oxygen Sealer & Refiller Machine Presence**:
   - `find_by_name` for `*Sealer*` and `*Refill*` returned 0 results.
   - `grep_search` across `common/` for `sealer` and `refill` returned 0 occurrences.
   - Neither block, block entity, block item, recipe, nor model exists.

6. **Current Test Suite**:
   - `./gradlew test` ran 34 tests in 1s with exit code 0.
   - Zero tests exist for `AtmosphereHelper`, `OxygenTankItem`, `LifeSupportManager`, or planetary atmosphere properties.

---

## 2. Logic Chain

1. **Step 1 (Atmosphere Disconnect)**: Observation 1 shows that `AtmosphereHelper` marks any `stellarodyssey:*` dimension as vacuum. Observation 2 shows that `CelestialBodyRegistry` gives Exotic Prime 0.85 atm of pressure. Therefore, players on Exotic Prime currently suffer vacuum decompression damage rather than toxic atmospheric asphyxiation when unsealed.
2. **Step 2 (Spacesuit Coupling Gap)**: Observation 3 shows that any inventory `OxygenTankItem` is drained without checking equipped armor. Observation 4 shows that lore defines the Chestplate as the "Oxygen Manifold" responsible for distributing carried O2 tanks. Therefore, the life support tick violates its intended design by allowing players without a chestplate or helmet to consume oxygen reserves.
3. **Step 3 (Missing Content)**: Observation 5 establishes that the Oxygen Sealer and Oxygen Refill Station required by R1 are completely absent from registrations (`ModBlocks`, `ModItems`, `ModBlockEntityTypes`) and assets.
4. **Step 4 (Side Safety & Multi-Loader Requirements)**: For the Oxygen Sealer to work cleanly across Fabric and NeoForge without client-side leaks or memory leaks, room flood-fill algorithms (BFS) must run exclusively on the server side, cache sealed bounds per BlockEntity without global static `Level` references, and communicate safety status to the client via `OxygenSyncPayload(inHazard = false)`.
5. **Step 5 (Verification Requirement)**: Observation 6 shows a clean test baseline (34/34 passing) but zero coverage for life support. Implementing R1 requires new unit tests covering tank durability math, suit piece validation, catalog atmosphere contracts, and sealed room BFS algorithms.

---

## 3. Caveats

1. **Client Visor Rendering**: The visor HUD overlay (`OxygenHudOverlay.java`) was inspected statically and verified to consume `ClientOxygenData`. Visual render validation in an active Minecraft client session requires human playtesting or GUI test harnesses.
2. **Flood-Fill Volume Bounds**: A maximum volume of 1,024 blocks and maximum horizontal radius of 16 blocks are recommended for the Oxygen Sealer BFS algorithm based on standard mod performance benchmarks; this balance should be tuned during gameplay playtests.
3. **No Code Modified**: In strict compliance with the explorer archetype and dispatch instructions, zero Java, JSON, or game files were modified.

---

## 4. Conclusion

The codebase possesses clean architectural foundations (durability-based `OxygenTankItem`, `SpacesuitItem` modular armor, and `OxygenSyncPayload` networking), but R1 requires five concrete implementation phases:
1. **Refactor Atmosphere Evaluation**: Update `AtmosphereHelper` to query `CelestialBodyRegistry`, distinguishing hard vacuum (< 0.05 atm, Nexus Moon) from unbreathable/toxic atmosphere (0.85 atm, Exotic Prime).
2. **Couple Oxygen Consumption to Spacesuit**: Require `SPACESUIT_CHESTPLATE` (Manifold) and `SPACESUIT_HELMET` to consume carried tanks; add partial suit leak scaling (2x drain for 3/4 pieces in vacuum).
3. **Implement Oxygen Refill Station**: Register `ModBlocks.OXYGEN_REFILLER` and `ModItems.OXYGEN_REFILLER` with right-click tank refill interaction and MC 26.3 item descriptor.
4. **Implement Oxygen Sealer Block & BlockEntity**: Register `ModBlocks.OXYGEN_SEALER`, `ModItems.OXYGEN_SEALER`, and `ModBlockEntityTypes.OXYGEN_SEALER` with server-side 3D BFS room sealing, caching, and `AtmosphereHelper` integration.
5. **Add Comprehensive Unit Tests**: Create unit test classes covering tank durability math, suit piece matching, catalog atmosphere parameters, and BFS room seal logic.

---

## 5. Verification Method

To independently verify these findings:
1. **Inspect Code Files**:
   - `common/src/main/java/com/amaro/stellarodyssey/lifesupport/AtmosphereHelper.java:34-49` (Blanket vacuum check)
   - `common/src/main/java/com/amaro/stellarodyssey/lifesupport/LifeSupportManager.java:39-85` (Inventory tank drain loop)
   - `common/src/main/java/com/amaro/stellarodyssey/world/CelestialBodyRegistry.java:49-80` (Planetary atmosphere parameters)
   - `common/src/main/java/com/amaro/stellarodyssey/registry/ModBlocks.java` (Absence of sealer/refiller)
2. **Execute Test Baseline**:
   - Run `./gradlew test` in project root: confirms 34 passing tests and zero existing life support tests.
3. **Invalidation Conditions**:
   - If `ModBlocks.OXYGEN_SEALER` or `ModBlocks.OXYGEN_REFILLER` is found already registered in any source file, finding 3 is invalidated.
   - If `AtmosphereHelper.java` is found already calling `CelestialBodyRegistry.getBody(...)`, finding 1 is invalidated.
