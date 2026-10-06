# Architectural & Registries Compliance Review Report (M1)

**Reviewer**: `reviewer_arch_compliance_1` (Roles: `reviewer`, `critic`)  
**Scope**: Core Architecture & Mod Lifecycle (`com.amaro.stellarodyssey.core`), Registries Architecture (`com.amaro.stellarodyssey.registry`), and Architectural Rules Compliance.  
**Date**: 2026-10-06  
**Verdict**: **REQUEST_CHANGES**

---

## 1. Observation

Direct observations from codebase inspection, ripgrep searches, build executions, and unit test results:

### 1.1 Build and Test Executions
- **Compilation**: Command `.\gradlew.bat compileJava` executed cleanly with exit code `0`.
  Tasks: `:common:compileJava`, `:neoforge:compileJava`, `:fabric:compileJava` all passed without errors.
- **Unit Test Suite**: Command `.\gradlew.bat :common:test --rerun-tasks --info` executed cleanly with exit code `0`.
  All 5 test suites ran and passed 100%:
  - `CoreLifecycleAndConstantsTest` (7 tests)
  - `DecoupledSatellitesContractTest` (4 tests)
  - `ModRegistriesBindingTest` (6 tests)
  - `AlienMineralTierMatrixTest` (8 tests)
  - `AlienMineralTierExtensibilityTest` (4 tests)

### 1.2 Architectural Rules Compliance
- **Platform Separation**: Ripgrep search for `net.neoforged` and `net.fabricmc` across `common/src/` returned **0 occurrences**. Platform-specific code is strictly confined to `neoforge/` and `fabric/` subprojects.
- **Static World/Entity References**: Ripgrep search for static fields storing `Level`, `ServerLevel`, `Entity`, `LivingEntity`, `Player`, `ServerPlayer` or collections of them in `common/src/main/java` returned **0 occurrences**. All methods accept them as local parameters or manipulate transient attributes.
- **Client/Server Boundary Separation**: All usages of `net.minecraft.client.*` are strictly confined to `com.amaro.stellarodyssey.client.**` and `com.amaro.stellarodyssey.satellites.starmap.**`. In `StarMapSatellite.java`, client screen invocations are isolated inside `StarMapClientHandler` and guarded with `Platform.getEnvironment() != Env.CLIENT` and `EnvExecutor.runInEnv(Env.CLIENT, ...)`.

### 1.3 Registries Architecture (`com.amaro.stellarodyssey.registry`)
- `ModRegistries.registerAll()` cleanly triggers:
  - `ModCreativeTabs.register()` -> `DeferredRegister<CreativeModeTab> TABS` (`Registries.CREATIVE_MODE_TAB`)
  - `ModBlocks.register()` -> `DeferredRegister<Block> BLOCKS` (`Registries.BLOCK`)
  - `ModItems.register()` -> `DeferredRegister<Item> ITEMS` (`Registries.ITEM`)
  - `ModEntities.register()` -> `DeferredRegister<EntityType<?>> ENTITIES` (`Registries.ENTITY_TYPE`)
  - `ModSoundEvents.register()` -> `DeferredRegister<SoundEvent> SOUND_EVENTS` (`Registries.SOUND_EVENT`)
- All registered objects are typed stable references returning `RegistrySupplier<T>`.
- BlockItems in `ModItems` properly defer `ModBlocks.ALIEN_*.get()` within supplier factories.

### 1.4 Mod Lifecycle & Satellite Discovery Observations
- In `common/src/main/java/com/amaro/stellarodyssey/StellarOdyssey.java`:
  ```java
  24:     public static void init() {
  25:         ModRegistries.registerAll();
  26:         ModLifecycleManager.fireStage(ModLifecycleStage.REGISTRY);
  27: 
  28:         ModNetworking.init();
  29:         LifeSupportManager.init();
  30:         PlanetaryGravityManager.init();
  31: 
  32:         ModLifecycleManager.fireStage(ModLifecycleStage.COMMON_SETUP);
  33: 
  34:         LOGGER.info("Stellar Odyssey initialised - preparing for launch.");
  35:     }
  38:     public static void clientInit() {
  39:         ModLifecycleManager.fireStage(ModLifecycleStage.CLIENT_SETUP);
  40:     }
  43:     public static void serverStarting() {
  44:         ModLifecycleManager.fireStage(ModLifecycleStage.SERVER_STARTING);
  45:     }
  ```
- In `common/src/main/java/com/amaro/stellarodyssey/client/StellarOdysseyClient.java`:
  ```java
  19:     public static void init() {
  20:         ClientGuiEvent.RENDER_HUD.register(OxygenHudOverlay::render);
  21: 
  22:         EntityModelLayerRegistry.register(StarshipModel.LAYER_LOCATION, StarshipModel::createBodyLayer);
  23:         EntityRendererRegistry.register(ModEntities.STARSHIP, StarshipEntityRenderer::new);
  24:     }
  ```
  `StellarOdysseyClient.init()` **never calls `StellarOdyssey.clientInit()`**.
- Grep for `serverStarting` across the entire project reveals **zero callers of `StellarOdyssey.serverStarting()`**. There is no registration of Architectury's `LifecycleEvent.SERVER_STARTING`.
- Satellite classes (`WorldGenSatellite`, `EcologySatellite`, `StarMapSatellite`) rely on static initializers:
  ```java
  static {
      try {
          init();
      } catch (Throwable t) { ... }
  }
  ```
  However, search for references to `WorldGenSatellite` and `EcologySatellite` reveals they are **only referenced in their own source files and in `DecoupledSatellitesContractTest.java`**. Neither `StellarOdyssey.java`, nor loader entrypoints, nor `ModLifecycleManager` ever reference them.
- File system search for `META-INF/services/` or `ServiceLoader` returns **0 results**.
- In `common/src/main/java/com/amaro/stellarodyssey/core/StellarOdysseyCore.java`:
  Methods `fireRegistry()`, `fireCommonSetup()`, `fireClientSetup()`, and `fireServerStarting()` are declared, but grep reveals **zero callers of `StellarOdysseyCore`** across the entire codebase.
- In `common/src/main/java/com/amaro/stellarodyssey/block/AlienMineralBlock.java`:
  Lines 37-58 reflectively mutate private fields `unregisteredIntrusiveHolders` and `frozen` in `BuiltInRegistries.BLOCK`.

---

## 2. Logic Chain

1. **Premise 1 (JVM Classloading Specification)**: A Java class is only loaded and its static initialization block (`static { ... }`) executed when the class is first actively referenced (instantiated, static field accessed, static method invoked, or loaded via explicit `Class.forName` / `ServiceLoader`).
2. **Premise 2 (Zero Runtime References to Satellites)**: In the actual mod runtime execution path, `StellarOdyssey.init()` calls `ModRegistries.registerAll()` and `ModLifecycleManager.fireStage(...)`. Neither `StellarOdyssey`, `ModRegistries`, `ModLifecycleManager`, `StellarOdysseyNeoForge`, nor `StellarOdysseyFabric` contains any reference to `WorldGenSatellite`, `EcologySatellite`, or `StarMapSatellite`.
3. **Inference 1 (Satellites Dead at Runtime)**: When the game launches on NeoForge or Fabric, `WorldGenSatellite`, `EcologySatellite`, and `StarMapSatellite` classes are NEVER loaded. Consequently, their `init()` methods never run, and `ModLifecycleManager` iterates over an empty list (`MODULES.isEmpty() == true`). None of the satellite modules register their worldgen features, ecology behaviors, or star map components during actual gameplay.
4. **Premise 3 (Unit Test False Negative)**: In `DecoupledSatellitesContractTest.java`, the test explicitly references `WorldGenSatellite.INSTANCE`, `EcologySatellite.INSTANCE`, and `StarMapSatellite.INSTANCE`, artificially forcing classloading and registration. This masks the fact that the mod does not discover them during actual game startup.
5. **Premise 4 (Unwired Lifecycle Hooks)**:
   - `StellarOdysseyClient.init()` sets up renderers but fails to call `StellarOdyssey.clientInit()`. Thus, `ModLifecycleStage.CLIENT_SETUP` is never fired in game.
   - `StellarOdyssey.init()` does not register any listener on Architectury's `LifecycleEvent.SERVER_STARTING`. Thus, `ModLifecycleStage.SERVER_STARTING` is never fired in game.
6. **Inference 2 (Lifecycle Contract Incomplete)**: 2 out of the 4 lifecycle stages defined in `ModLifecycleStage` are completely dead in the game runtime.
7. **Conclusion**: While code compiles cleanly, tests pass, and registry definitions are pristine, the core mod lifecycle and satellite SPI contract are functionally broken at runtime due to missing discovery mechanisms and unwired lifecycle hooks. Therefore, changes must be requested.

---

## 3. Review Findings

### [Major] Finding 1: Satellite Modules Missing Discovery Bridge at Runtime
- **What**: Satellites rely on static initializers (`static { init(); }`) to register with `ModLifecycleManager`. Because nothing in the mod startup path references these classes, lazy classloading prevents them from ever being registered during game launch.
- **Where**: `com.amaro.stellarodyssey.core.lifecycle.ModLifecycleManager`, `com.amaro.stellarodyssey.satellites.*`, `com.amaro.stellarodyssey.StellarOdyssey:24-35`.
- **Why**: In game, `ModLifecycleManager.fireStage(REGISTRY)` and `fireStage(COMMON_SETUP)` execute with 0 registered modules. Procedural world generation, alien ecology, and star map navigation will fail to activate.
- **Suggestion**:
  - *Option A (Recommended SPI approach)*: Implement standard Java SPI via `ServiceLoader.load(SatelliteModule.class, ModLifecycleManager.class.getClassLoader())` inside `ModLifecycleManager.init()` and create `common/src/main/resources/META-INF/services/com.amaro.stellarodyssey.core.lifecycle.SatelliteModule` declaring the 3 satellite provider classes.
  - *Option B (Explicit Bootstrap Registry)*: In `ModLifecycleManager`, declare a default discovery or bootstrap method (e.g. `registerDefaultSatellites()` or table) that registers `WorldGenSatellite.INSTANCE`, `EcologySatellite.INSTANCE`, and `StarMapSatellite.INSTANCE`.

### [Major] Finding 2: `CLIENT_SETUP` and `SERVER_STARTING` Lifecycle Stages Never Dispatched
- **What**: `StellarOdyssey.clientInit()` and `StellarOdyssey.serverStarting()` are never called by any client entrypoint or server event listener.
- **Where**:
  - `com.amaro.stellarodyssey.client.StellarOdysseyClient:19-24`
  - `com.amaro.stellarodyssey.StellarOdyssey:24-45`
- **Why**:
  - `StellarOdysseyClient.init()` does not call `StellarOdyssey.clientInit()`, preventing satellites from running client-specific registrations (e.g. `StarMapSatellite.onClientSetup()`).
  - `StellarOdyssey.init()` does not register Architectury's `LifecycleEvent.SERVER_STARTING.register(server -> StellarOdyssey.serverStarting())`, preventing satellites from charting default planetary catalogs in `onServerStarting()`.
- **Suggestion**:
  1. Add `StellarOdyssey.clientInit();` inside `StellarOdysseyClient.init()`.
  2. Add `LifecycleEvent.SERVER_STARTING.register(server -> StellarOdyssey.serverStarting());` inside `StellarOdyssey.init()`.

### [Minor] Finding 3: Dead Facade Class `StellarOdysseyCore`
- **What**: `StellarOdysseyCore` exposes lifecycle helper methods (`fireRegistry`, `fireCommonSetup`, etc.) that are never invoked anywhere in the mod or tests.
- **Where**: `common/src/main/java/com/amaro/stellarodyssey/core/StellarOdysseyCore.java`.
- **Why**: Redundant facade causing maintenance confusion with `StellarOdyssey.java` and `ModLifecycleManager`.
- **Suggestion**: Either route lifecycle dispatch calls through `StellarOdysseyCore` or remove the dead class.

### [Minor] Finding 4: Unsafe Registry Reflection in `AlienMineralBlock` Constructor Helper
- **What**: `AlienMineralBlock.prepareProperties` reflectively mutates `BuiltInRegistries.BLOCK` to unlock `unregisteredIntrusiveHolders` and force `frozen = false`.
- **Where**: `common/src/main/java/com/amaro/stellarodyssey/block/AlienMineralBlock.java:37-58`.
- **Why**: This was added as a workaround for unit tests instantiating blocks outside registration. In a production environment, mutating global vanilla registry flags inside block constructors can cause race conditions or break registry immutability guarantees.
- **Suggestion**: Remove production reflection and handle test environment registration properly via a test harness or mock properties.

---

## 4. Adversarial Review (Stress Tests & Attack Surface)

**Overall Risk Assessment**: **MEDIUM** (High for runtime satellite functionality, Low for compilation and memory leaks).

### Challenge 1: Silent Failure of Satellite Module Registration in Production
- **Assumption Challenged**: Static initializers in satellite classes guarantee automatic registration with `ModLifecycleManager`.
- **Attack Scenario**: Package the mod jar, install on a NeoForge server, and boot the server without tests.
- **Blast Radius**: Zero satellites registered. `WorldGenSatellite` does not register configured features or surface rules, `EcologySatellite` does not attach AI goals, `StarMapSatellite` does not register client components. No crash occurs, but all satellite features silently fail to exist.
- **Mitigation**: Add a ServiceLoader SPI provider configuration or explicit bootstrap module registry, and add an automated test verifying `ModLifecycleManager.getRegisteredModules()` has >= 3 modules immediately after `StellarOdyssey.init()` without prior manual registration.

### Challenge 2: Client Setup & Server Starting Desynchronization
- **Assumption Challenged**: Calling `StellarOdyssey.init()` completes the entire lifecycle chain.
- **Attack Scenario**: Player joins dedicated server or opens StarMap client screen expecting `clientInitialized == true`.
- **Blast Radius**: `StarMapSatellite.isClientInitialized()` remains `false`. Catalogs prepared during server startup are never populated.
- **Mitigation**: Hook Architectury lifecycle events in `StellarOdyssey.init()` and `StellarOdysseyClient.init()`.

---

## 5. Verified Claims Matrix

| Claim / Requirement | Verification Method | Result | Notes |
|---|---|---|---|
| `./gradlew.bat compileJava` succeeds with code 0 | Propose and run command via shell | **PASS** | `:common`, `:neoforge`, `:fabric` all compile cleanly. |
| `./gradlew.bat test` succeeds with code 0 | Run `:common:test --rerun-tasks` | **PASS** | 29/29 tests pass across 5 test classes. |
| Zero `net.neoforged` / `net.fabricmc` imports in `common/` | Ripgrep query across `common/src` | **PASS** | 0 occurrences. Complete loader independence. |
| Zero static references to `Level`, `Entity`, `Player` | Ripgrep query across `common/src/main/java` | **PASS** | 0 occurrences. Zero memory leaks across dimension unloads. |
| Client/Server boundary separation | Ripgrep search for `net.minecraft.client.*` | **PASS** | Confined strictly to `client` and `starmap` packages with EnvExecutor guards. |
| `DeferredRegister` instances bound cleanly | Inspect `ModRegistries`, `ModBlocks`, `ModItems`, `ModCreativeTabs`, `ModEntities`, `ModSoundEvents` | **PASS** | All use Architectury `DeferredRegister` and return stable `RegistrySupplier<T>`. |
| Monotonic progression across Mineral Tiers 1-5 | `AlienMineralTierMatrixTest.testMonotonicProgressionInvariants()` | **PASS** | Durability, speed, damage, hardness, resistance strictly monotonic. |
| Extensibility of Mineral Tiers without engine changes | `AlienMineralTierExtensibilityTest.testDynamicRegistrationOfTier6()` | **PASS** | Custom Tier 6 registers dynamically and binds to `AlienMineralBlock`. |
| Runtime Satellite Module Discovery | Code inspection of entrypoints and classloader tracing | **FAIL** | Satellites never loaded during game startup (Finding 1). |
| Complete Lifecycle Stage Dispatch | Code inspection of `StellarOdysseyClient` and `StellarOdyssey` | **FAIL** | `CLIENT_SETUP` and `SERVER_STARTING` never fired (Finding 2). |

---

## 6. Caveats

- Tests were executed within the Gradle JVM unit test environment with Minecraft bootstrapped via `Bootstrap.bootStrap()`. Dedicated server runtime in a fully packaged JAR was not tested in this dry run, which is why classloader lazy loading defects were caught via static code analysis and structural inspection rather than test crashes.

---

## 7. Conclusion & Next Steps

**Verdict**: **REQUEST_CHANGES**

The core registries (`ModBlocks`, `ModItems`, `ModCreativeTabs`, `ModEntities`, `ModSoundEvents`), mineral tier matrix (`AlienMineralTier`, `AlienMineralTierRegistry`), and architectural isolation rules (zero loader imports, zero static entity references) are implemented to an exceptionally high standard of quality and correctness.

However, approval is withheld pending the resolution of two major lifecycle integration defects:
1. **Finding 1 (Major)**: Implement an SPI discovery bridge (`ServiceLoader` in `ModLifecycleManager` with `META-INF/services/com.amaro.stellarodyssey.core.lifecycle.SatelliteModule`, or explicit bootstrap table) so satellite modules are actually registered at game launch.
2. **Finding 2 (Major)**: Wire `StellarOdyssey.clientInit()` into `StellarOdysseyClient.init()`, and wire `StellarOdyssey.serverStarting()` to `LifecycleEvent.SERVER_STARTING` in `StellarOdyssey.init()`.

Once Worker 1 / core implements these two lifecycle wiring fixes, this architecture will be 100% production-ready.

---

## 8. Verification Method for Resolving Agent

To verify resolution:
1. Ensure `META-INF/services/com.amaro.stellarodyssey.core.lifecycle.SatelliteModule` exists or `ModLifecycleManager` loads default satellites.
2. In `CoreLifecycleAndConstantsTest.java` or `DecoupledSatellitesContractTest.java`, add a test that calls `StellarOdyssey.init()` in an isolated environment and asserts `ModLifecycleManager.getRegisteredModules().size() >= 3` without manually invoking `registerModule`.
3. Check `StellarOdysseyClient.java` contains `StellarOdyssey.clientInit();`.
4. Check `StellarOdyssey.java` registers `LifecycleEvent.SERVER_STARTING`.
5. Run `.\gradlew.bat compileJava` and `.\gradlew.bat test`.
