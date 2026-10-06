# Handoff Report: Survey Adaptive Planetary Gravity (R2)

**Agent**: `explorer_survey_gravity`  
**Task**: Survey and Technical Architecture for R2 (Adaptive Planetary Gravity)  
**Date**: 2026-10-06  
**Type**: Hard Handoff (Investigation Complete)  

---

## 1. Observation

1. **Authoritative Requirement R2**:
   - `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md` lines 79-82:
     ```markdown
     ### R2. Física de Gravedad Adaptativa por Dimensión
     - Modificador de gravedad mediante eventos Architectury/Minecraft en ModDimensions.
     - Ajuste de gravedad reducida en Nexus Moon (0.16g) y Proxima B (0.35g) afectando a jugadores y entidades (caída lenta, impulso de salto).
     ```

2. **Existing Celestial Catalog**:
   - In `common/src/main/java/com/amaro/stellarodyssey/world/CelestialBodyRegistry.java` lines 49-80:
     - `NEXUS_MOON`: defined with `gravityMultiplier = 0.16`.
     - `PROXIMA_B`: defined with `gravityMultiplier = 0.40`. **Verbatim mismatch**: requirement specifies `0.35g`, but catalog defines `0.40`.
     - `EXOTIC_PRIME`: `0.75`.
     - `GLIESE_DEEP`: `1.35`.

3. **Existing Gravity Implementation**:
   - In `common/src/main/java/com/amaro/stellarodyssey/world/PlanetaryGravityManager.java` lines 17-69:
     - Applies a single static `AttributeModifier` with value `-0.60` (`ADD_MULTIPLIED_TOTAL`, yielding 0.40g) across ALL dimensions if `AtmosphereHelper.isVacuumEnvironment(player)` evaluates true.
     - Only hooks `TickEvent.PLAYER_POST` (line 37).
     - Does NOT modify general living entities (`LivingEntity`), alien fauna, or mobs.
     - Does NOT query `CelestialBodyRegistry` or the dimension's `gravityMultiplier()`.

4. **Minecraft 26.3 Attribute Engine Inspection**:
   - In `net.minecraft.world.entity.ai.attributes.Attributes`:
     - `Attributes.GRAVITY`: Base `0.08`, ranged `[-1.0, 1.0]`, registered with `setSyncable(true)`.
     - `Attributes.SAFE_FALL_DISTANCE`: Base `3.0`, ranged `[-1024.0, 1024.0]`, registered with `setSyncable(true)`.
     - `Attributes.FALL_DAMAGE_MULTIPLIER`: Base `1.0`, ranged `[0.0, 100.0]`, registered with `setSyncable(true)`.
     - `Attributes.JUMP_STRENGTH`: Base `0.42`, ranged `[0.0, 32.0]`, registered with `setSyncable(true)`.
   - In `net.minecraft.world.entity.LivingEntity`:
     - Method `travelInAir(Vec3)` computes downward acceleration as: `vy = (vy - getEffectiveGravity()) * 0.98`.
     - Method `getEffectiveGravity()` clamps gravity to `Math.min(this.getGravity(), 0.01)` when falling with `MobEffects.SLOW_FALLING`.
     - Method `calculateFallDamage(double, float)` calculates damage from $(fallDistance - safeFallDistance) \times fallDamageMultiplier$.

5. **Architectury Event Hooks Available**:
   - `dev.architectury.event.events.common.TickEvent.PLAYER_POST` (fires per-player tick on server).
   - `dev.architectury.event.events.common.EntityEvent.ADD` (`EventResult add(Entity, Level)`, fires whenever any entity spawns or loads into a level).
   - `dev.architectury.event.events.common.PlayerEvent.CHANGE_DIMENSION` (`change(ServerPlayer, ResourceKey<Level>, ResourceKey<Level>)`).
   - `dev.architectury.event.events.common.PlayerEvent.PLAYER_RESPAWN` (`respawn(ServerPlayer, boolean, RemovalReason)`).

6. **Current Build & Test Status**:
   - Command `./gradlew test --console=plain` executed with exit code 0; all 34 existing tests pass cleanly in 7 seconds.

---

## 2. Logic Chain

1. **Discrepancy Identification** (from Observations 1 & 2):
   - The user request explicitly demands $0.16g$ for Nexus Moon and $0.35g$ for Proxima B.
   - `CelestialBodyRegistry.java:51` sets Proxima B to $0.40g$.
   - **Inference**: `CelestialBodyRegistry.PROXIMA_B` must be changed from `0.40` to `0.35`.

2. **Inadequacy of Existing `PlanetaryGravityManager`** (from Observations 1 & 3):
   - `PlanetaryGravityManager.java` currently applies a flat $-0.60$ modifier (resulting in $0.40g$) regardless of whether the player is on Nexus Moon ($0.16g$), Proxima B ($0.35g$), Exotic Prime ($0.75g$), or Gliese Deep ($1.35g$).
   - `PlanetaryGravityManager.java` only operates on `Player` instances in `TickEvent.PLAYER_POST`, leaving all other entities with standard Overworld gravity ($1.0g$).
   - **Inference**: `PlanetaryGravityManager` must be refactored to look up the dimension's gravity multiplier dynamically from `CelestialBodyRegistry.getInstance().getBody(dimensionKey)`, apply distinct modifiers per dimension, and listen to `EntityEvent.ADD` to apply gravity modifiers to all `LivingEntity` instances.

3. **Side-Safety & Synchronization Mechanism** (from Observations 4 & 5):
   - `Attributes.GRAVITY`, `Attributes.SAFE_FALL_DISTANCE`, and `Attributes.FALL_DAMAGE_MULTIPLIER` are marked `syncable = true` in Minecraft 26.3.
   - When modifiers are added or updated on the server using `addOrUpdateTransientModifier()`, vanilla automatically generates and sends `ClientboundUpdateAttributesPacket` to tracking clients.
   - **Inference**: No custom network packets or client-side event hooks are needed for gravity synchronization. Restricting all modification to the server (`if (!level.isClientSide())`) ensures 100% side-safety, eliminates desync/rubber-banding, and avoids any client-only class leakage in `common`.

4. **Physics Calibration for Fall Damage and Jumps** (from Observation 4):
   - In low gravity, terminal velocity is reduced (0.64 blocks/tick on Moon vs 3.92 blocks/tick on Earth), but a player falling 15 blocks still accumulates a 15-block distance. Under vanilla default safe fall distance (3.0 blocks), the player would take 12 HP damage from a low-velocity impact.
   - **Inference**: To make fall damage realistic in low gravity, `SAFE_FALL_DISTANCE` must be increased by $(3.0 / \text{gravityMultiplier}) - 3.0$ (i.e. $+15.75$ blocks on Nexus Moon, $+5.57$ blocks on Proxima B), and `FALL_DAMAGE_MULTIPLIER` should be scaled by $\text{gravityMultiplier} - 1.0$ (reducing damage by 84% on Moon, 65% on Proxima B).
   - In low gravity, jumps automatically float up to $>6$ blocks because downward deceleration is reduced from $0.08$ to $0.0128\text{ blocks/tick}^2$.

---

## 3. Caveats

1. **Non-Living Entities**:
   - Entities not inheriting from `LivingEntity` (e.g. `ItemEntity`, `FallingBlockEntity`, `Arrow`) do not have an `AttributeMap` and hardcode their falling velocity in their private tick loops. Standard modding practice in Fabric/NeoForge affects `LivingEntity` (players, mobs, animals); modifying non-living entities would require invasive Mixins into `Entity.tick()` or `ItemEntity.tick()`, which is outside the scope of R2.
2. **Dynamic WorldGen Gravity Variations**:
   - Planetary gravity is modeled per dimension / celestial body and orbital altitude ($Y \ge 320$), rather than per-chunk or per-biome local anomalies. This matches the design of `ICelestialBody` and `ModDimensions`.
3. **Orbital Microgravity**:
   - In orbit ($Y \ge 320$), gravity is simulated as microgravity ($0.08g$, modifier $-0.92$). When descending below $Y = 320$, it transitions to the planet's surface gravity.

---

## 4. Conclusion

The technical path forward for implementing R2 (Adaptive Planetary Gravity) is well-defined and requires no changes to core game mechanics or external libraries:

1. **Update `CelestialBodyRegistry.java`**:
   - Change `PROXIMA_B` gravity multiplier from `0.40` to `0.35`.
2. **Refactor `PlanetaryGravityManager.java`**:
   - Replace the static modifier with dynamic resolver:
     $$\text{modifierValue} = \text{gravityMultiplier} - 1.0$$
   - Hook `TickEvent.PLAYER_POST` for players (handling altitude $Y \ge 320$ and dimension updates).
   - Hook `EntityEvent.ADD` for `LivingEntity` instances entering levels.
   - Scale `Attributes.GRAVITY`, `Attributes.SAFE_FALL_DISTANCE`, and `Attributes.FALL_DAMAGE_MULTIPLIER`.
   - Remove modifiers cleanly when returning to standard gravity ($1.0g$).
3. **Implement JUnit Test Suite**:
   - Write `AdaptivePlanetaryGravityTest.java` verifying registry values, modifier formulas, and decoupling rules.

---

## 5. Verification Method

To independently verify the findings and any future implementation:

1. **Build & Test Suite**:
   ```bash
   ./gradlew test --console=plain
   ```
   *Expected outcome*: 100% of unit tests pass with exit code 0.

2. **Cross-Loader Build**:
   ```bash
   ./gradlew :fabric:build :neoforge:build -x test --console=plain
   ```
   *Expected outcome*: Both loaders compile cleanly without side-safety or classloading errors.

3. **Code Inspection**:
   - Verify `CelestialBodyRegistry.PROXIMA_B.gravityMultiplier() == 0.35`.
   - Verify `CelestialBodyRegistry.NEXUS_MOON.gravityMultiplier() == 0.16`.
   - Verify `PlanetaryGravityManager.java` contains no references to `net.minecraft.client.*`, `net.fabricmc.*`, or `net.neoforged.*`.

4. **Invalidation Conditions**:
   - If `CelestialBodyRegistry.PROXIMA_B` remains `0.40`, R2 acceptance criteria are violated.
   - If non-player living entities on Nexus Moon or Proxima B do not receive reduced gravity, R2 acceptance criteria are violated.
   - If client-only classes are imported in `PlanetaryGravityManager`, dedicated server builds will crash.
