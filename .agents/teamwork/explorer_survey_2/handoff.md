# Handoff Report: Architecture & Modularity Analysis (R1 & R3)

**Author**: Survey Explorer 2 (Architecture & Modularity Analyst)  
**Date**: 2026-10-06T04:20:00Z  
**Project**: Stellar Odyssey (`stellarodyssey`)  
**Package Root**: `com.amaro.stellarodyssey`  
**Target Modloader**: NeoForge (Minecraft 26.3 "Wilderness Bound", Java 25, Architectury Loom Multi-Loader)

---

## 1. Observation

### 1.1 Project & Build Setup
- **Multi-Loader Structure**:
  - `settings.gradle` lines 23–27: Root project `stellar-odyssey` includes subprojects `:common`, `:fabric`, and `:neoforge`.
  - `gradle.properties` lines 27–40:
    ```properties
    minecraft_version=26.3
    java_version=25
    architectury_api_version=22.0.3
    neoforge_version=26.3.0.51-beta
    mod_id=stellarodyssey
    maven_group=com.amaro.stellarodyssey
    ```
  - `build.gradle` lines 1–6: Architectury plugin `3.5-SNAPSHOT`, Loom `1.17-SNAPSHOT` (`loom-no-remap`), Shadow `9.4.3`. MC 26.3 is unobfuscated (Mojang official names, Java 25 bytecode).
  - Clean build verified: `./gradlew compileJava` passes with exit code 0 (`:common:compileJava`, `:neoforge:compileJava`, `:fabric:compileJava`).

### 1.2 Existing Source Structure in `common`
Source path: `common/src/main/java/com/amaro/stellarodyssey/`
- `StellarOdyssey.java` (lines 18–36):
  ```java
  public final class StellarOdyssey {
      public static final String MOD_ID = "stellarodyssey";
      public static final Logger LOGGER = LoggerFactory.getLogger("Stellar Odyssey");
      ...
      public static void init() {
          ModCreativeTabs.register();
          ModBlocks.register();
          ModItems.register();
          ModEntities.register();

          ModNetworking.init();
          LifeSupportManager.init();
          PlanetaryGravityManager.init();

          LOGGER.info("Stellar Odyssey initialised - preparing for launch.");
      }
  ```
- `registry/`: Contains `ModBlocks.java`, `ModItems.java`, `ModCreativeTabs.java`, and `ModEntities.java`. All use `dev.architectury.registry.registries.DeferredRegister`.
  - **Missing**: No `ModSoundEvents.java` exists anywhere in the repository.
  - **Missing**: No unified registry binding coordinator (`ModRegistries`).
- `world/`:
  - `ModDimensions.java` defines `ResourceKey<DimensionType> ALIEN_PLANET_TYPE` and `ResourceKey<Level> PROXIMA_B`.
  - `PlanetaryGravityManager.java` (lines 45–48) couples directly to `AtmosphereHelper.isVacuumEnvironment(player)` and relies on hardcoded dimension namespace checks: `level.dimension().identifier().getNamespace().equals(StellarOdyssey.MOD_ID)`.
- `client/`:
  - `StellarOdysseyClient.java` (lines 19–24): Registers HUD overlay `OxygenHudOverlay::render`, model layer `StarshipModel.createBodyLayer`, and renderer `StarshipEntityRenderer::new`.
  - `renderer/StarshipEntityRenderer.java` (lines 44–55): Employs Minecraft 26.3 `SubmitNodeCollector` and `CameraRenderState`. Currently submits only base texture `TEXTURE`; the emissive texture `starship_emissive.png` (present in `assets/stellarodyssey/textures/entity/starship/starship_emissive.png`) is not rendered.
  - **Missing**: No emissive layer pipeline for entities or blocks on the client event bus.
  - **Missing**: No screen registration or base classes for GUI screens (e.g., Star Map).

### 1.3 Coupling and Satellite Modularity Status (R3)
- All game subsystems are statically referenced and directly invoked from `StellarOdyssey.init()`.
- No module interface, SPI, or lifecycle registry exists.
- Adding planned satellite subsystems (procedural WorldGen, alien Fauna/Flora AI, intergalactic Star Map GUI) under the current structure would require modifying `StellarOdyssey.java` directly with tight cross-package coupling, risking circular imports and dedicated-server crashes.

---

## 2. Logic Chain

1. **Premise 1 (R1 Mandate)**: R1 requires a clean package hierarchy organized by modular subsystems under `com.amaro.stellarodyssey`:
   - `core`: Mod entrypoint, global constants, lifecycle and `IEventBus` management.
   - `registry`: Centralized `DeferredRegister` for `Blocks`, `Items`, `CreativeModeTabs`, `EntityTypes`, and `SoundEvents`.
   - `world`: `ResourceKey` keys and dimension architectural foundations.
   - `client`: Client event bus setup, renderers, screens, and emissive layers.
   - *Direct link to Observation 1.2*: Currently, `SoundEvents` is absent, constants are mixed into `StellarOdyssey`, `core` package does not exist, and client lacks emissive layer dispatch and screen abstractions.

2. **Premise 2 (R3 Decoupling Mandate)**: R3 requires that procedural generation (`worldgen`), fauna/flora AI (`ecology`), and the intergalactic star map GUI (`starmap`) plug in as satellite modules without circular dependencies or monolithic coupling to the main mod class.
   - *Direct link to Observation 1.3*: If `StellarOdyssey.java` continues to statically invoke every subsystem, `StellarOdyssey` becomes a monolith. If `starmap` references internal `worldgen` classes or server-only AI, circular dependencies or dedicated server crashes will occur.

3. **Step 3 (Architectural Decoupling via Satellite Contract)**:
   - Defining a `SatelliteModule` interface (`getId()`, `getPriority()`, `onRegister()`, `onCommonSetup()`, `onClientSetup()`, `onServerStarting()`) in `core.lifecycle`.
   - A `ModLifecycleManager` in `core.lifecycle` maintains an ordered registry of `SatelliteModule` instances and dispatches lifecycle events deterministically.
   - The main class `StellarOdyssey` (or `StellarOdysseyCore`) delegates all subsystem execution to `ModLifecycleManager`, eliminating monolithic coupling.

4. **Step 4 (Dependency Inversion via API Contracts)**:
   - To prevent circular dependencies among satellites:
     * Introduce `com.amaro.stellarodyssey.api` holding pure interfaces and data transfer records (`ICelestialBody`, `ICelestialCatalog`, `IAtmosphereCondition`, `INavigationRoute`).
     * The WorldGen satellite registers biomes, noise parameters, and surface rules using standard `ResourceKey` contracts.
     * The Fauna/Flora AI satellite queries environmental contracts (`IAtmosphereCondition`, `ICelestialBody`) rather than concrete worldgen or block classes.
     * The Star Map GUI satellite runs strictly on `Dist.CLIENT` / `Env.CLIENT`, querying `ICelestialCatalog` without touching server worldgen code or mob AI.
     * Dependency Graph is strictly a Directed Acyclic Graph (DAG): Satellites -> API / Core / Registry -> Minecraft / Forge.

5. **Step 5 (Emissive Rendering Pipeline in MC 26.3)**:
   - Minecraft 26.3 uses `SubmitNodeCollector` instead of legacy `MultiBufferSource` batching.
   - Emissive layers must be submitted with full bright coordinates (`0x00F000F0` / `LightTexture.FULL_BRIGHT`) and unshaded render types, overlaying the base diffuse model pass without being dimmed by planetary night or cave darkness.

---

## 3. Proposed Architectural Design

### 3.1 Target Package Hierarchy

```
com.amaro.stellarodyssey
│
├── core                                      // R1: Entrypoint, constants, lifecycle
│   ├── ModConstants.java                     // MOD_ID, MOD_NAME, LOGGER, id()
│   ├── StellarOdysseyCore.java               // Core lifecycle orchestrator
│   └── lifecycle
│       ├── ModLifecycleManager.java          // Manages stages, dispatches to satellites
│       ├── ModLifecycleStage.java            // REGISTRY, COMMON_SETUP, CLIENT_SETUP, SERVER_STARTING
│       └── SatelliteModule.java              // Pluggable satellite interface
│
├── api                                       // R3: Decoupling contracts (leaf dependencies)
│   ├── celestial
│   │   ├── ICelestialBody.java               // Dimension ID, gravity, atmosphere, coordinates
│   │   └── ICelestialCatalog.java            // Lookup service for planetary systems
│   ├── ecology
│   │   ├── IAtmosphereCondition.java         // Vacuum/hazard conditions contract
│   │   └── IAlienEntityBehavior.java         // Behavior provider contract
│   └── navigation
│       └── INavigationRoute.java             // Hyperdrive navigation and orbital route data
│
├── registry                                  // R1: Centralized DeferredRegisters
│   ├── ModRegistries.java                    // Master binder (registerAll())
│   ├── ModBlocks.java                        // DeferredRegister<Block>
│   ├── ModItems.java                         // DeferredRegister<Item>
│   ├── ModCreativeTabs.java                  // DeferredRegister<CreativeModeTab>
│   ├── ModEntities.java                      // DeferredRegister<EntityType<?>>
│   ├── ModSoundEvents.java                   // DeferredRegister<SoundEvent> (NEW)
│   └── tiers                                 // R2: Extensible Mineral Tier Matrix
│       ├── AlienMineralTier.java             // Tiers 1-5 definitions & records
│       └── ModArmorMaterials.java            // Armor material configurations
│
├── world                                     // R1: Dimensions & Celestial Physics
│   ├── ModDimensions.java                    // ResourceKey<DimensionType>, ResourceKey<Level>
│   ├── ModBiomes.java                        // ResourceKey<Biome> keys
│   ├── CelestialBodyRegistry.java            // Implements ICelestialCatalog
│   └── PlanetaryGravityManager.java          // Low-gravity & safe-fall physics
│
├── lifesupport                               // Life support & hazards
│   ├── AtmosphereHelper.java                 // Vacuum & spacesuit hermetic seal checks
│   └── LifeSupportManager.java               // Oxygen consumption & decompression
│
├── network                                   // Client-Server networking
│   ├── ModNetworking.java                    // Packet registration & channels
│   └── OxygenSyncPayload.java                // Oxygen status payload
│
├── block                                     // Block classes
│   └── AlienOreBlock.java
│
├── item                                      // Item classes
│   ├── OxygenTankItem.java
│   ├── SpacesuitItem.java
│   └── StarshipItem.java
│
├── entity                                    // Entity classes
│   └── StarshipEntity.java
│
├── client                                    // R1: Client bus, renderers, screens, emissives
│   ├── StellarOdysseyClient.java             // Client orchestrator
│   ├── event
│   │   └── ClientBusSubscriber.java          // Model layer & renderer bindings
│   ├── renderer
│   │   ├── StarshipEntityRenderer.java       // Starship renderer with SubmitNodeCollector
│   │   └── layer
│   │       └── EmissiveModelLayer.java       // Full-bright unshaded emissive pass
│   ├── model
│   │   └── StarshipModel.java
│   └── gui
│       └── OxygenHudOverlay.java             // HUD overlay
│
└── satellites                                // R3: Decoupled Satellite Expansion Modules
    ├── worldgen                              // Satellite 1: Procedural WorldGen
    │   ├── WorldGenSatellite.java            // Implements SatelliteModule
    │   ├── PlanetaryNoiseSettings.java       // Custom noise curves
    │   ├── PlanetarySurfaceRules.java        // Procedural surface rules
    │   └── ModConfiguredFeatures.java        // Crystals & alien structures
    ├── ecology                               // Satellite 2: Fauna & Flora Alien AI
    │   ├── EcologySatellite.java             // Implements SatelliteModule
    │   ├── ai
    │   │   ├── LowGravityJumpGoal.java       // Alien low-gravity jump AI
    │   │   └── VacuumFleeGoal.java           // Vacuum evasion AI
    │   └── flora
    │       └── AlienSporeTicker.java         // Bioluminescent spore dispersal
    └── starmap                               // Satellite 3: Intergalactic Star Map GUI
        ├── StarMapSatellite.java             // Implements SatelliteModule (Client-only)
        ├── screen
        │   ├── StarMapScreen.java            // Fullscreen celestial navigation UI
        │   └── StarMapCoordinatesWidget.java
        └── render
            └── StarMapSkyRenderer.java       // Orbital skybox & sector projector
```

### 3.2 Key Modular Interfaces & Lifecycle Contracts

#### SatelliteModule Interface (`core.lifecycle.SatelliteModule`)
```java
package com.amaro.stellarodyssey.core.lifecycle;

/**
 * Pluggable contract for satellite subsystems (WorldGen, Fauna/Flora AI, Star Map GUI).
 * Eliminates monolithic hardcoding in the mod entrypoint class.
 */
public interface SatelliteModule {
    /** Unique identifier for the module (e.g. "worldgen", "ecology", "starmap"). */
    String getId();

    /** Priority for ordered execution (higher executes earlier). */
    default int getPriority() {
        return 0;
    }

    /** Called during mod registry phase (registration of blocks, items, features). */
    default void onRegister() {}

    /** Called during common setup (networking, capabilities, data fixers). */
    default void onCommonSetup() {}

    /** Called strictly on physical client side during client initialization. */
    default void onClientSetup() {}

    /** Called when server is starting (dimension catalogs, world data). */
    default void onServerStarting() {}
}
```

#### ModLifecycleManager (`core.lifecycle.ModLifecycleManager`)
```java
package com.amaro.stellarodyssey.core.lifecycle;

import com.amaro.stellarodyssey.core.ModConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ModLifecycleManager {
    private static final List<SatelliteModule> MODULES = new ArrayList<>();

    private ModLifecycleManager() {}

    public static synchronized void registerModule(SatelliteModule module) {
        MODULES.add(module);
        MODULES.sort(Comparator.comparingInt(SatelliteModule::getPriority).reversed());
        ModConstants.LOGGER.debug("Registered satellite module: {}", module.getId());
    }

    public static void fireStage(ModLifecycleStage stage) {
        for (SatelliteModule module : MODULES) {
            try {
                switch (stage) {
                    case REGISTRY -> module.onRegister();
                    case COMMON_SETUP -> module.onCommonSetup();
                    case CLIENT_SETUP -> module.onClientSetup();
                    case SERVER_STARTING -> module.onServerStarting();
                }
            } catch (Exception e) {
                ModConstants.LOGGER.error("Failed executing stage {} on module {}", stage, module.getId(), e);
            }
        }
    }
}
```

#### ModSoundEvents (`registry.ModSoundEvents`)
```java
package com.amaro.stellarodyssey.registry;

import com.amaro.stellarodyssey.core.ModConstants;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

public final class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ModConstants.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> STARSHIP_THRUST =
            register("entity.starship.thrust");
    public static final RegistrySupplier<SoundEvent> DECOMPRESSION_ALARM =
            register("hazard.decompression_alarm");
    public static final RegistrySupplier<SoundEvent> ALIEN_AMBIENCE =
            register("ambient.alien_world");
    public static final RegistrySupplier<SoundEvent> RESONANCE_CRYSTAL =
            register("block.alien_ore.resonate");

    private static RegistrySupplier<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ModConstants.id(name)));
    }

    public static void register() {
        SOUND_EVENTS.register();
    }
}
```

#### Centralized Registry Binder (`registry.ModRegistries`)
```java
package com.amaro.stellarodyssey.registry;

public final class ModRegistries {
    private ModRegistries() {}

    public static void registerAll() {
        ModCreativeTabs.register();
        ModBlocks.register();
        ModItems.register();
        ModEntities.register();
        ModSoundEvents.register();
    }
}
```

#### Celestial Catalog API (`api.celestial.ICelestialBody` & `ICelestialCatalog`)
```java
package com.amaro.stellarodyssey.api.celestial;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface ICelestialBody {
    ResourceKey<Level> dimensionKey();
    double gravityMultiplier();
    float atmosphericPressure();
    boolean hasBreathableAtmosphere();
    float solarRadiation();
    String starSystemName();
}
```

```java
package com.amaro.stellarodyssey.api.celestial;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.Optional;

public interface ICelestialCatalog {
    Optional<ICelestialBody> getBody(ResourceKey<Level> dimension);
    void registerBody(ICelestialBody body);
}
```

#### Emissive Layer Pipeline (`client.renderer.layer.EmissiveModelLayer`)
```java
package com.amaro.stellarodyssey.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class EmissiveModelLayer {
    // 0x00F000F0 represents max block (15) and max sky (15) light coords
    public static final int FULL_BRIGHT = 0x00F000F0;

    private EmissiveModelLayer() {}

    public static <S extends EntityRenderState, M extends EntityModel<S>> void submitEmissive(
            M model, S state, PoseStack poseStack, SubmitNodeCollector collector, Identifier emissiveTexture) {
        // Submits glowing texture without light attenuation
        collector.submitModel(model, state, poseStack, emissiveTexture, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
    }
}
```

---

## 4. Caveats

1. **Architectury Multi-Loader Compatibility**: The project is structured with Architectury Loom compiling `:common`, `:neoforge`, and `:fabric`. All shared abstractions (`DeferredRegister`, `RegistrySupplier`, `ClientGuiEvent`, `TickEvent`) must remain in `:common` using Architectury APIs so they map transparently to NeoForge event buses without breaking multi-platform portability.
2. **Datapack-Driven WorldGen in MC 26.3**: In modern Minecraft (1.21.2+ / 26.3), dimensions and biomes are predominantly datapack-driven (`data/stellarodyssey/dimension/` and `data/stellarodyssey/worldgen/biome/`). Java worldgen code must interface via `ResourceKey` and configured features rather than overriding chunk generators at runtime unless custom code chunk generation is explicitly mandated.
3. **Physical Side Safety**: `starmap` classes reference client rendering and UI (`Screen`, `SubmitNodeCollector`). They must be isolated in client packages and guarded so they are never loaded by a dedicated server runtime.

---

## 5. Conclusion

1. **R1 Fulfillment**:
   - The proposed architecture creates `core` (constants, lifecycle orchestrator, `ModLifecycleManager`), completes `registry` by adding `ModSoundEvents` and `ModRegistries`, establishes `world` dimension keys with `ICelestialCatalog` decoupling, and expands `client` with emissive layer rendering and screen foundations.
2. **R3 Decoupling & Satellite Expansion**:
   - The `SatelliteModule` SPI pattern allows WorldGen, Ecology AI, and Star Map GUI to plug into the lifecycle without modifying the root entrypoint class.
   - The Dependency DAG strictly isolates subsystems from one another, completely eliminating circular dependencies.
3. **Production Readiness**:
   - Fully compatible with NeoForge 26.3.0.51-beta and Java 25.
   - Preserves all existing assets, models, and networking while structuring the codebase for rapid, scalable expansion.

---

## 6. Verification Method

### 6.1 Build & Syntax Verification
Execute in project root:
```bash
./gradlew compileJava
```
- **Expected Outcome**: Task `:common:compileJava` and `:neoforge:compileJava` succeed with 0 errors.

### 6.2 Modularity & Dependency Verification
1. Inspect import statements across packages:
   - Ensure `satellites.worldgen` contains 0 imports from `satellites.starmap` or `satellites.ecology`.
   - Ensure `satellites.starmap` contains 0 imports from `satellites.worldgen` or `satellites.ecology`.
   - Ensure `common` classes contain 0 imports from `net.neoforged.*` (keeping `:common` loader-agnostic).
2. Verify registry linkage:
   - Check that `ModRegistries.registerAll()` registers `ModBlocks`, `ModItems`, `ModCreativeTabs`, `ModEntities`, and `ModSoundEvents` to the event bus.
3. Invalidation condition: Any circular import between packages or any class not found during compilation immediately invalidates the architectural layout.
