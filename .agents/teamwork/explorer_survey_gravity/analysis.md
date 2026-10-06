# Technical Survey & Architecture Analysis: R2 Adaptive Planetary Gravity

**Module**: Stellar Odyssey (Architectury Multi-loader, Minecraft 26.3, Java 25)  
**Author**: `explorer_survey_gravity`  
**Date**: 2026-10-06  
**Status**: COMPLETE  

---

## 1. Executive Summary

Requirement **R2 (Adaptive Planetary Gravity)** specifies:
1. Dynamic gravity modification driven by dimensions/celestial bodies via Architectury/Minecraft events in `ModDimensions` / `world`.
2. Reduced gravity adjustments on **Nexus Moon (0.16g)** and **Proxima B (0.35g)** affecting both **players and entities** (incorporating slow floating falls, jump impulses, and scaled fall damage).

### Current Codebase Assessment
- **Dimension Keys**: Properly registered in `ModDimensions.java` (`PROXIMA_B`, `NEXUS_MOON`, `EXOTIC_PRIME`, `GLIESE_DEEP`) with corresponding data pack JSON dimensions and `stellarodyssey:alien_planet` dimension type.
- **Celestial Catalog**: `CelestialBodyRegistry.java` implements `ICelestialCatalog` and defines celestial parameters, but **`PROXIMA_B` is currently set to `0.40g` instead of the required `0.35g`**.
- **Current Gravity Manager**: `PlanetaryGravityManager.java` currently uses a single hardcoded modifier (`-0.60`, corresponding to 0.40g) triggered only if `AtmosphereHelper.isVacuumEnvironment(player)` is true. It completely neglects per-dimension variations (treating Nexus Moon and Proxima B identically), only ticks `Player` instances (leaving mobs and other entities completely unaffected), and does not leverage the `CelestialBodyRegistry` service.

---

## 2. Dimension Registration & Celestial Catalog Survey

### 2.1 Dimension Key Definitions (`ModDimensions.java`)
Located at: `common/src/main/java/com/amaro/stellarodyssey/world/ModDimensions.java`

```java
public static final ResourceKey<DimensionType> ALIEN_PLANET_TYPE = ResourceKey.create(
        Registries.DIMENSION_TYPE, StellarOdyssey.id("alien_planet"));

public static final ResourceKey<Level> PROXIMA_B = ResourceKey.create(
        Registries.DIMENSION, StellarOdyssey.id("proxima_b"));

public static final ResourceKey<Level> NEXUS_MOON = ResourceKey.create(
        Registries.DIMENSION, StellarOdyssey.id("nexus_moon"));

public static final ResourceKey<Level> EXOTIC_PRIME = ResourceKey.create(
        Registries.DIMENSION, StellarOdyssey.id("exotic_prime"));

public static final ResourceKey<Level> GLIESE_DEEP = ResourceKey.create(
        Registries.DIMENSION, StellarOdyssey.id("gliese_deep"));
```

- Each dimension key is cleanly registered in the standard Minecraft `Registries.DIMENSION` registry.
- Dimensions map to data pack JSON definitions at `common/src/main/resources/data/stellarodyssey/dimension/*.json`.
- All alien planet dimensions share the dimension type `stellarodyssey:alien_planet`, which is tagged under `tags/dimension_type/vacuum.json`.

### 2.2 Celestial Catalog Interface & Registry (`ICelestialBody`, `CelestialBodyRegistry`)
Located at:
- Interface: `common/src/main/java/com/amaro/stellarodyssey/api/celestial/ICelestialBody.java`
- Catalog: `common/src/main/java/com/amaro/stellarodyssey/world/CelestialBodyRegistry.java`

#### `ICelestialBody` Contract
```java
public interface ICelestialBody {
    ResourceKey<Level> dimensionKey();
    double gravityMultiplier();        // 1.0 = Earth/Overworld standard
    float atmosphericPressure();       // 0.0 = hard vacuum, 1.0 = 1 atm
    boolean hasBreathableAtmosphere(); // True if breathable without life support
    float solarRadiation();            // 1.0 = standard flux, >1.5 = hazard
    String starSystemName();           // e.g. "Alpha Centauri"
    default String name() { ... }
    default boolean isVacuum() { return atmosphericPressure() < 0.05f; }
    default boolean isHazardous() { ... }
}
```

#### Current Celestial Body Definitions vs. R2 Requirements
| Body | Dimension Key | Current Registry Value | R2 Required Value | Discrepancy / Action |
|---|---|---|---|---|
| **Nexus Moon** | `stellarodyssey:nexus_moon` | `0.16` | **0.16g** | Matches requirement. |
| **Proxima B** | `stellarodyssey:proxima_b` | `0.40` | **0.35g** | **Mismatch!** Must be updated from `0.40` to `0.35`. |
| **Exotic Prime** | `stellarodyssey:exotic_prime` | `0.75` | 0.75g | Built-in super-Earth. |
| **Gliese Deep** | `stellarodyssey:gliese_deep` | `1.35` | 1.35g | Built-in high-gravity terrestrial world. |
| **Orbital Space** | Any dimension $Y \ge 320$ | N/A (hardcoded in vacuum check) | Microgravity (0.05g – 0.08g) | Needs formalization in gravity manager. |

---

## 3. Minecraft 26.3 Gravity & Entity Physics Mechanics

Through decompiler and bytecode inspection of the official Minecraft 26.3 classes (`net.minecraft.world.entity.LivingEntity`, `net.minecraft.world.entity.ai.attributes.Attributes`), the internal mechanics of gravity, jumping, and fall damage were identified:

### 3.1 Mojang Modern Entity Attributes
In Minecraft 26.3, gravity and locomotion are attribute-driven rather than hardcoded math formulas in movement loops:

1. **`Attributes.GRAVITY`**:
   - Class: `net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY`
   - Base Value: `0.08` blocks/tick² ($1.6\text{ m/s}^2$ per tick equivalent).
   - Range: `[-1.0, 1.0]`.
   - Synchronization: **`setSyncable(true)`**.
   - **Crucial Feature**: Because `GRAVITY` is registered with `syncable = true`, adding or modifying an `AttributeModifier` on the server automatically generates and dispatches `ClientboundUpdateAttributesPacket` to tracking clients. Client-side movement prediction and server-side position validation automatically align with zero desync!

2. **`Attributes.SAFE_FALL_DISTANCE`**:
   - Base Value: `3.0` blocks.
   - Range: `[-1024.0, 1024.0]`.
   - Synchronization: **`setSyncable(true)`**.
   - Controls how far an entity can fall before accumulating fall damage.

3. **`Attributes.FALL_DAMAGE_MULTIPLIER`**:
   - Base Value: `1.0`.
   - Range: `[0.0, 100.0]`.
   - Synchronization: **`setSyncable(true)`**.
   - Directly multiplies the final fall damage output.

4. **`Attributes.JUMP_STRENGTH`**:
   - Base Value: `0.41999998688697815` ($\approx 0.42$ blocks/tick upward impulse).
   - Range: `[0.0, 32.0]`.
   - Synchronization: **`setSyncable(true)`**.
   - Determines the initial vertical velocity imparted when executing a jump from the ground.

### 3.2 Air Travel Physics (`LivingEntity.travelInAir`)
Minecraft's discrete tick integration in `LivingEntity.travelInAir(Vec3)` proceeds as follows:
```java
// 1. Calculate effective downward gravity acceleration:
double effectiveGravity = this.getEffectiveGravity();

// 2. Subtract gravity from vertical velocity:
vy = vy - effectiveGravity;

// 3. Apply standard air friction drag (0.98 multiplier):
vy = vy * 0.98;
```

#### Interaction with `MobEffects.SLOW_FALLING`
Minecraft's `getEffectiveGravity()` method explicitly accounts for Slow Falling:
```java
protected double getEffectiveGravity() {
    boolean isFalling = this.getDeltaMovement().y <= 0.0;
    if (isFalling && this.hasEffect(MobEffects.SLOW_FALLING)) {
        return Math.min(this.getGravity(), 0.01);
    }
    return this.getGravity();
}
```
- On **Nexus Moon** ($0.16g$, $g = 0.0128$): Natural downward acceleration is already $0.0128\text{ blocks/tick}^2$. If the player drinks a Slow Falling potion, $\min(0.0128, 0.01) = 0.01$. The potion still functions and gently softens descent without ever causing gravity inversion or glitches.
- If gravity is lower than $0.01$ (e.g. $0.0064$ in orbital microgravity), $\min(0.0064, 0.01) = 0.0064$. Slow Falling never artificially increases downward acceleration!

### 3.3 Jump Impulse & Low-Gravity Trajectory
In normal gravity ($g = 0.08$):
- Initial upward velocity: $v_0 = 0.42$.
- Velocity decays to $0$ in $\approx 5$ ticks.
- Peak jump apex: $\approx 1.25$ blocks.

In reduced gravity (e.g. **Nexus Moon 0.16g**, $g = 0.0128$):
- Initial upward velocity: $v_0 = 0.42$.
- Downward deceleration per tick is reduced by $84\%$.
- Hang time increases to $>35$ ticks ($\approx 1.8$ seconds).
- Peak jump apex reaches $\approx 6.5$ to $7.0$ blocks!
- **Jump Impulse Adaptation**: Because low gravity alone multiplies vertical jump height by $>5\times$, entities automatically experience soaring, high-arc leaps without needing unnatural instantaneous velocity boosts. For extra responsiveness or propulsion, `Attributes.JUMP_STRENGTH` can optionally receive a subtle positive modifier (e.g. $+0.05$ to $+0.10$).

Furthermore, `LowGravityJumpGoal` (`satellites/ecology/ai/LowGravityJumpGoal.java`) already checks:
```java
AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
if (gravity != null && gravity.getValue() < 0.075) {
    return true; // Low gravity environment detected!
}
```
Thus, adjusting `Attributes.GRAVITY` on entities automatically enables alien fauna pathfinding leaps and chasm-clearing AI routines.

### 3.4 Fall Damage Mechanics (`LivingEntity.calculateFallDamage`)
Vanilla calculate fall damage as:
```java
private double calculateFallPower(double fallDistance) {
    return fallDistance + 1.0E-6 - this.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
}

protected int calculateFallDamage(double fallDistance, float damageMultiplier) {
    if (this.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) return 0;
    double fallPower = this.calculateFallPower(fallDistance);
    return Mth.floor(fallPower * damageMultiplier * this.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
}
```
**The Physics Challenge**: In low gravity, entities fall much slower (lower terminal velocity), but fall distance in blocks accumulated over time can still be large. If safe fall distance remained $3.0$ blocks, an astronaut dropping $15$ blocks on the Moon (at a gentle impact speed of only $0.6\text{ blocks/tick}$) would take $12$ HP of damage!

**The Solution**:
1. Scale **`Attributes.SAFE_FALL_DISTANCE`**:
   $$\Delta h_{safe} = \left(\frac{3.0}{\text{gravityMultiplier}}\right) - 3.0$$
   - Nexus Moon ($0.16g$): Safe fall threshold becomes $\approx 18.75$ blocks ($+15.75$ blocks).
   - Proxima B ($0.35g$): Safe fall threshold becomes $\approx 8.57$ blocks ($+5.57$ blocks).
2. Scale **`Attributes.FALL_DAMAGE_MULTIPLIER`**:
   $$\text{Modifier} = \text{gravityMultiplier} - 1.0$$
   - Nexus Moon ($0.16g$): Multiplier becomes $0.16$ ($-84\%$ damage).
   - Proxima B ($0.35g$): Multiplier becomes $0.35$ ($-65\%$ damage).

---

## 4. Architectury Event Architecture & Server-Client Synchronization

### 4.1 Applicable Events
Architectury API `22.0.3` provides the following hooks in `dev.architectury.event.events.common.*`:

1. **`TickEvent.PLAYER_POST`**:
   - Signature: `TickEvent.PLAYER_POST.register((Player player) -> { ... })`
   - Evaluated every tick on the server side (`if (player.level().isClientSide()) return;`).
   - Essential for handling **altitude-dependent orbital transitions** ($Y \ge 320$) and cross-dimension changes.
2. **`EntityEvent.ADD`**:
   - Signature: `EntityEvent.ADD.register((Entity entity, Level level) -> { ... return EventResult.pass(); })`
   - Fires whenever any entity is spawned, summoned, or loaded from chunk data.
   - Allows attaching adaptive gravity modifiers to **all living entities** (`if (entity instanceof LivingEntity living && !level.isClientSide())`).
3. **`PlayerEvent.CHANGE_DIMENSION`**:
   - Signature: `PlayerEvent.CHANGE_DIMENSION.register((ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to) -> { ... })`
   - Provides immediate trigger to recalculate modifiers upon interplanetary transit.
4. **`PlayerEvent.PLAYER_RESPAWN`**:
   - Signature: `PlayerEvent.PLAYER_RESPAWN.register((ServerPlayer player, boolean conqueredEnd, RemovalReason reason) -> { ... })`
   - Ensures attribute instances on freshly respawned players are refreshed immediately.

### 4.2 Side-Safety and Desync Prevention
- **Zero Client-Only Class Leakage**: All gravity logic must remain in `common`, referencing only standard Minecraft server-safe classes (`Player`, `LivingEntity`, `Level`, `Attributes`, `AttributeModifier`, `ResourceKey`, `Identifier`).
- **Transient Modifiers**:
  Using `attribute.addOrUpdateTransientModifier(modifier)` prevents modifiers from persisting into world save files. If the player travels back to the Overworld or uninstalls the mod, no lingering attributes remain.
- **Vanilla Sync Engine**:
  Because `Attributes.GRAVITY`, `Attributes.SAFE_FALL_DISTANCE`, and `Attributes.FALL_DAMAGE_MULTIPLIER` are vanilla syncable attributes, modifying them on the server causes Minecraft to transmit `ClientboundUpdateAttributesPacket`. The client immediately simulates identical gravity physics in its local client-side prediction loop, resulting in silky-smooth locomotion without rubber-banding.

---

## 5. Architectural Gap Analysis & Code Recommendations

### 5.1 Gaps in Current Implementation (`PlanetaryGravityManager.java`)

| Dimension / Aspect | Existing Implementation | Proposed Architecture (R2 Compliant) |
|---|---|---|
| **Dimension Lookup** | Binary check: `AtmosphereHelper.isVacuumEnvironment(player)` | Dynamic lookup via `CelestialBodyRegistry.getInstance().getBody(level.dimension())` |
| **Nexus Moon** | Receives fixed `-0.60` (0.40g) modifier | Receives $-0.84$ modifier $\rightarrow$ **$0.16g$** |
| **Proxima B** | Receives fixed `-0.60` (0.40g) modifier | Receives $-0.65$ modifier $\rightarrow$ **$0.35g$** |
| **Exotic Prime** | Receives fixed `-0.60` (0.40g) modifier | Receives $-0.25$ modifier $\rightarrow$ **$0.75g$** |
| **Gliese Deep** | Receives fixed `-0.60` (0.40g) modifier | Receives $+0.35$ modifier $\rightarrow$ **$1.35g$ (Heavy-G)** |
| **Overworld / Earth** | Removes modifier ($1.0g$) | Removes modifier ($1.0g$) |
| **Orbit ($Y \ge 320$)** | Treated as generic vacuum ($0.40g$) | Simulates orbital microgravity ($0.08g$, modifier $-0.92$) |
| **Entity Support** | Players only (`TickEvent.PLAYER_POST`) | Players **AND** Living Entities (`EntityEvent.ADD` + `TickEvent.PLAYER_POST`) |
| **Fall Damage** | Fixed $+12$ blocks safe fall | Proportional safe fall distance $+15.75\text{m}$ (Moon) / $+5.57\text{m}$ (Proxima) & scaled fall damage multiplier |

### 5.2 Required Code Changes Summary

1. **`CelestialBodyRegistry.java` (Line 51)**:
   ```java
   // Before:
   public static final ICelestialBody PROXIMA_B = new PlanetaryBody(
           ModDimensions.PROXIMA_B, 0.40, ...);
   // After:
   public static final ICelestialBody PROXIMA_B = new PlanetaryBody(
           ModDimensions.PROXIMA_B, 0.35, ...);
   ```

2. **`PlanetaryGravityManager.java`**:
   - Refactor to compute effective gravity multipliers dynamically from `CelestialBodyRegistry`.
   - Create attribute modifier helpers for:
     - `Attributes.GRAVITY` (`ADD_MULTIPLIED_TOTAL`, `multiplier - 1.0`).
     - `Attributes.SAFE_FALL_DISTANCE` (`ADD_VALUE`, $\Delta h_{safe}$).
     - `Attributes.FALL_DAMAGE_MULTIPLIER` (`ADD_MULTIPLIED_TOTAL`, `multiplier - 1.0`).
   - Listen to `TickEvent.PLAYER_POST` (for players and orbital transitions).
   - Listen to `EntityEvent.ADD` (for living entities entering levels).
   - Listen to `PlayerEvent.CHANGE_DIMENSION` and `PlayerEvent.PLAYER_RESPAWN`.

---

## 6. Proposed Testing & Verification Strategy

### 6.1 Unit Test Suite (`AdaptivePlanetaryGravityTest.java`)
A headless JUnit 5 test class under `common/src/test/java/com/amaro/stellarodyssey/` should verify:
1. **Catalog Integrity**:
   - `CelestialBodyRegistry.PROXIMA_B.gravityMultiplier() == 0.35`.
   - `CelestialBodyRegistry.NEXUS_MOON.gravityMultiplier() == 0.16`.
   - `CelestialBodyRegistry.EXOTIC_PRIME.gravityMultiplier() == 0.75`.
   - `CelestialBodyRegistry.GLIESE_DEEP.gravityMultiplier() == 1.35`.
2. **Mathematical Consistency**:
   - Gravity modifier values: $0.16 - 1.0 = -0.84$; $0.35 - 1.0 = -0.65$.
   - Effective acceleration: $0.08 \times 0.16 = 0.0128\text{ blocks/tick}^2$; $0.08 \times 0.35 = 0.0280\text{ blocks/tick}^2$.
   - Safe fall distance calculations: $3.0 / 0.16 \approx 18.75$; $3.0 / 0.35 \approx 8.57$.
   - Slow falling clamp: $\min(0.0128, 0.01) == 0.01$.
3. **Decoupling and Side-Safety**:
   - Assert `PlanetaryGravityManager` has zero imports from client packages (`net.minecraft.client.*`) and zero imports from platform loaders (`net.fabricmc.*`, `net.neoforged.*`).
   - Assert no static fields retain entity or level references.

### 6.2 Build & Integration Verification
- `./gradlew test --console=plain` (all 34+ unit tests pass).
- `./gradlew compileJava` across common, fabric, and neoforge subprojects.
- In-game verification checklist:
  - Jump on Nexus Moon: high floaty leap ($>6$ blocks height), slow descent.
  - Jump on Proxima B: medium low-gravity leap ($\approx 3.5$ blocks height).
  - Falling from 15 blocks on Nexus Moon: zero or negligible damage.
  - Return to Overworld: normal vanilla 1.0g gravity restored.
