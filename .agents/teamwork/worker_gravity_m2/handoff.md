# Handoff Report: Milestone M2 — Adaptive Planetary Gravity (R2) & AtmosphereHelper Resolution

## 1. Observation

1. **Celestial Catalog Parameters (`CelestialBodyRegistry.java`)**:
   - `CelestialBodyRegistry.PROXIMA_B.gravityMultiplier()` verified at `0.35` (matching R2 requirement).
   - `CelestialBodyRegistry.NEXUS_MOON.gravityMultiplier()` verified at `0.16` (matching R2 requirement).
   - `EXOTIC_PRIME` (0.75g, 0.85 atm) and `GLIESE_DEEP` (1.35g, 2.50 atm) verified intact.

2. **Atmosphere Differentiation Resolution (`AtmosphereHelper.java`)**:
   - In `AtmosphereHelper.isVacuumEnvironment(Player player)` (lines 115–144), `CelestialBodyRegistry.getInstance().getBody(level.dimension())` is now queried **before** checking the fallback `VACUUM_DIMENSIONS` tag.
   - For charted exoplanets like `EXOTIC_PRIME` (0.85 atm) and `PROXIMA_B` (0.15 atm), `body.isVacuum()` returns `false`, preventing false positive vacuum categorization even though their dimension type shares `stellarodyssey:alien_planet`.
   - `NEXUS_MOON` (0.00 atm) correctly resolves to `true` (vacuum).
   - `isUnbreathableAtmosphere(Player player)` now checks `!body.hasBreathableAtmosphere() && !body.isVacuum()`, ensuring hard vacuum bodies are not conflated with toxic atmospheres.
   - Added `LifecycleEvent.SERVER_STOPPING.register(server -> clearSealers())` to prevent static map memory retention across world unload events.

3. **Adaptive Planetary Gravity Physics (`PlanetaryGravityManager.java`)**:
   - Replaced static modifiers with dynamic calculations:
     - `gravityModifierAmount(multiplier)` = `multiplier - 1.0` (Nexus Moon: `-0.84`, Proxima B: `-0.65`, Orbit: `-0.92`, Overworld: `0.0`).
     - `safeFallDistanceBonus(multiplier)` = `(3.0 / multiplier) - 3.0` (+15.75 blocks on Nexus Moon, +5.57 blocks on Proxima B).
     - `fallDamageMultiplierAmount(multiplier)` = `multiplier - 1.0` (-84% damage on Moon, -65% damage on Proxima B).
   - Added `applyOrUpdateModifier` helper that verifies current modifier values before modifying attribute instances, preventing redundant attribute recalculation and client packet spam every tick.
   - Added `resolveBodyGravityMultiplier(ResourceKey<Level>)` overload for decoupled lookups.
   - Added `getGravityMultiplier(Level, double y)`, `getGravityMultiplier(Level, BlockPos pos)`, and `getGravityMultiplier(Entity entity)` fulfilling the `PROJECT.md` interface contracts.
   - Handled `EntityEvent.ADD` for all `LivingEntity` instances, `TickEvent.PLAYER_POST` for player locomotion and orbital transitions ($Y \ge 320$), and `PlayerEvent.CHANGE_DIMENSION` for interplanetary transit.
   - Implemented `clearAdaptiveGravity(LivingEntity)` ensuring clean modifier removal in neutral (1.0g) dimensions.

4. **Unit Test Suite (`AdaptivePlanetaryGravityTest.java`)**:
   - Created `common/src/test/java/com/amaro/stellarodyssey/world/AdaptivePlanetaryGravityTest.java` containing 8 tests:
     - `testRequiredGravityMultipliers`: PROXIMA_B 0.35g, NEXUS_MOON 0.16g.
     - `testAllCelestialBodiesGravity`: complete catalog distribution.
     - `testModifierFormula`: exact amounts for moon, proxima, exotic, gliese, overworld, and orbit.
     - `testEffectiveAcceleration`: verifies 0.08 base acceleration scaling (0.0128 on moon, 0.0280 on proxima).
     - `testSafeFallDistanceScaling`: inverse scaling math.
     - `testFallDamageMultiplierScaling`: damage reduction scaling.
     - `testDimensionKeyResolution` & `testOrbitalAltitudeTransitions`: dimension lookups and orbital microgravity decay at $Y \ge 320$.
     - `testAtmosphereHelperCategorization`: vacuum vs toxic differentiation across all dimensions.
     - `testModifierIds` & `testNoStaticEntityLeaks`: decoupling and memory leak prevention.

5. **Build and Test Verification**:
   - Executed `./gradlew test --rerun-tasks --console=plain`:
     Result: **BUILD SUCCESSFUL in 18s** (100% of all unit tests pass).
   - Executed `./gradlew :common:test --tests "com.amaro.stellarodyssey.world.AdaptivePlanetaryGravityTest" --console=plain`:
     Result: **BUILD SUCCESSFUL in 11s** (all 8 tests pass).
   - Executed `./gradlew :fabric:build :neoforge:build -x test --console=plain`:
     Result: **BUILD SUCCESSFUL in 7s** (clean multi-loader compilation).

---

## 2. Logic Chain

1. **Atmospheric Specificity Resolution**:
   - Observation: Tag `stellarodyssey:vacuum` included `stellarodyssey:alien_planet`, which is shared by all planetary dimensions.
   - Observation: In `AtmosphereHelper.isVacuumEnvironment`, checking `dimensionTypeRegistration().is(VACUUM_DIMENSIONS)` preceded `CelestialBodyRegistry`.
   - Consequence: Charted worlds with non-vacuum atmospheres (`EXOTIC_PRIME` 0.85 atm, `PROXIMA_B` 0.15 atm) were treated as hard vacuum, triggering decompression alarms instead of atmospheric toxicity.
   - Resolution: Prioritizing `CelestialBodyRegistry.getBody(dimension)` allows explicit catalog pressure parameters to govern charted worlds. Only uncharted dimensions fall back to tag/namespace heuristics.

2. **Gravity Physics Synchronization**:
   - Observation: Minecraft 26.3 registers `Attributes.GRAVITY`, `Attributes.SAFE_FALL_DISTANCE`, and `Attributes.FALL_DAMAGE_MULTIPLIER` as syncable attributes (`setSyncable(true)`).
   - Consequence: Modifying these attributes on the server propagates changes to clients automatically.
   - Refinement: Calling `addOrUpdateTransientModifier` every player tick was generating unnecessary packet updates. Adding `applyOrUpdateModifier` checks the current modifier value against the target, updating only when values change (such as cross-dimension travel or crossing $Y \ge 320$ into orbit).

3. **Entity Locomotion Parity**:
   - Observation: R2 mandates that reduced gravity affects both players and entities (mobs, alien fauna).
   - Resolution: Hooking `EntityEvent.ADD` applies adaptive modifiers to any `LivingEntity` entering a level. In low gravity, alien fauna AI like `LowGravityJumpGoal` detects `Attributes.GRAVITY < 0.075` and performs long leaping strides.

---

## 3. Caveats

- **Client Visual Jump Rendering**: Unit tests fully verify attribute modifier values, formulas, and math; in-game visual inspection requires launching a Minecraft client environment.
- **Data Pack Custom Dimensions**: Uncharted custom data pack dimensions without catalog entries default to 1.0g unless charted via `CelestialBodyRegistry.registerBody()`.

---

## 4. Conclusion

Milestone M2 (Adaptive Planetary Gravity & AtmosphereHelper Resolution) is **COMPLETE**:
- `CelestialBodyRegistry` reflects required 0.35g for `PROXIMA_B` and 0.16g for `NEXUS_MOON`.
- `AtmosphereHelper.isVacuumEnvironment` properly differentiates hard vacuum (<0.05 atm) from toxic/unbreathable exoplanetary atmospheres.
- `PlanetaryGravityManager` dynamically applies server-side attribute modifiers for gravity, safe fall distance, and fall damage to both players and living entities with orbital microgravity support.
- Multi-loader build and 100% of unit tests pass cleanly.

---

## 5. Verification Method

To independently verify this milestone:

1. **Run full unit test suite**:
   ```bash
   ./gradlew test --rerun-tasks --console=plain
   ```
   Expected: `BUILD SUCCESSFUL` with 100% of tests passing.

2. **Run dedicated adaptive planetary gravity unit tests**:
   ```bash
   ./gradlew :common:test --tests "com.amaro.stellarodyssey.world.AdaptivePlanetaryGravityTest" --console=plain
   ```
   Expected: `BUILD SUCCESSFUL` with all 8 tests passing.

3. **Verify multi-loader compilation**:
   ```bash
   ./gradlew :fabric:build :neoforge:build -x test --console=plain
   ```
   Expected: `BUILD SUCCESSFUL` for both Fabric and NeoForge jars.
