# 🔄 REANUDACIÓN — Contenido + verificación final (Stellar Odyssey)

> **Propósito:** cualquier sesión nueva puede continuar leyendo SOLO este documento.
> **Fecha:** 2026-10-06 · **Proyecto:** `C:\Users\amaro\Documents\antigravity\blissful-lavoisier`
> **Regla actual:** NO ejecutar gradle en la sesión anterior (sin bash). Aquí queda todo lo pendiente de verificar.

---

## 0. BLOQUEADOR de entorno (sigue vigente)

No hay Git Bash → no se puede ejecutar `./gradlew`. Solución, una sola vez desde una terminal propia:

```powershell
Test-Path "C:\Program Files\Git\bin\bash.exe"     # ¿ya existe?
setx CODEBUFF_GIT_BASH_PATH "C:\Program Files\Git\bin\bash.exe"
# o, si no existe:
winget install --id Git.Git --exact --source winget
```

`setx` solo afecta a procesos nuevos → cerrar y reabrir el agente.

---

## 1. Lo que FALTA por verificar (orden recomendado)

```bash
./gradlew test --console=plain
./gradlew :fabric:build :neoforge:build --console=plain
```

Y luego, si todo está verde:

1. **Desplegar JAR a Prism Launcher** (copiar a TODAS las carpetas de mods):
   - `C:\Users\amaro\Downloads\Prism Launcher\instances\Stellar Odyssey\mods\`
   - `...\Stellar Odyssey\minecraft\mods\`, `...\Stellar Odyssey\.minecraft\mods\`
   - `C:\Users\amaro\AppData\Roaming\PrismLauncher\instances\Stellar Odyssey\{mods, minecraft\mods, .minecraft\mods}\`
   - Origen: `fabric/build/libs/stellarodyssey-fabric-0.1.0+mc26.3.jar`
2. **Obsidian:** `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\Agentes\` → `STATUS.md`, `HANDOFFS/antigravity.md`, `INBOX.md`.

### Comprobación en partida (datapack) — importante
Los siguientes cambios son JSON de datos y **deben cargar sin errores** al crear mundo:
- ¿Aparece Gliese Deep en el mapa estelar y el viaje funciona? (antes era un destino muerto)
- ¿Se desbloquean los avances? (revisar que no haya error de formato en el log)
- ¿Los minerales se generan en las 4 dimensiones?

---

## 2. Bugs de contenido corregidos en esta sesión

### 2.1 CRÍTICO — 5 bloques no dropeaban nunca
`assembly_table`, `launch_pad`, `launch_pad_base`, `oxygen_refiller`, `oxygen_sealer` llaman a
`requiresCorrectToolForDrops()` pero **no estaban en `minecraft:mineable/pickaxe`**. Como
`TieredItem.isCorrectToolForDrops` comprueba esa etiqueta, ningún pico era "la herramienta
correcta" → los bloques desaparecían al romperlos.
**Fix:** añadidos a `data/minecraft/tags/block/mineable/pickaxe.json`.

### 2.2 CRÍTICO — 2 loot tables ausentes
`oxygen_refiller` y `oxygen_sealer` no tenían `loot_table/blocks/*.json` → sin drop aunque se
arreglase el tag. **Fix:** creadas ambas (el sealer preserva `custom_name` de su block entity).

### 2.3 Gliese Deep era un destino muerto
`CelestialBodyRegistry.GLIESE_DEEP` y `RocketTiers` (Tier 3) lo anuncian, y salía en el mapa
estelar, pero **no existía `dimension/gliese_deep.json`** → el warp abortaba en silencio.
**Fix:** creada la dimensión (tipo `stellarodyssey:alien_planet`) + bioma propio
`worldgen/biome/metamorphic_canyons.json` (clon del bioma válido, recoloreado, con las 3 menas).

### 2.4 8 recetas ausentes (items inobtenibles en supervivencia)
Creadas: `oxygen_tank`, `spacesuit_helmet`, `spacesuit_chestplate`, `spacesuit_leggings`,
`spacesuit_boots`, `oxygen_refiller`, `oxygen_sealer`, `starship`.
Todas usan solo materiales de Overworld (la progresión alienígena se reserva a los cohetes).

### 2.5 `raw_alien` era un item muerto
Cae de `alien_ore` pero no tenía ningún uso. **Fix:** recetas de fundición
`raw_alien_from_smelting.json` y `raw_alien_from_blasting.json` → `minecraft:iron_ingot`
("hierro meteórico", coherente con la descripción de Nexus Moon). *Decisión de diseño: revisable.*

### 2.6 Economía T3 incoherente
`rocket_cone_t3`, `rocket_fin_t3`, `rocket_tank_t3` solo usaban Verdantite, contradiciendo el BOM
de `AssemblyLogic.getRequiredIngotIds(3)` y la lore. **Fix:** ahora los 8 componentes T3 usan
**Verdantite + Astralite**.

### 2.7 Sonidos registrados pero nunca reproducidos
`OXYGEN_SEALER_PRESSURIZE` y `AIRLOCK_CYCLE` existían en `ModSoundEvents` sin usarse.
**Fix:** `OxygenSealerBlockEntity` los reproduce al sellarse / al romperse el sellado
(comportamiento inspirado en el Oxygen Sealer de Galacticraft).

---

## 3. Contenido NUEVO añadido

### 3.1 Árbol de avances (12 avances, antes no existía ninguno)
`data/stellarodyssey/advancement/`: `root`, `rocket_pioneer`, `liftoff`, `celidium`, `voyager`,
`proxima_b`, `astralite`, `odyssey`, `exotic_prime`, `gliese_deep`, `breath_of_life`, `starship`.
Títulos/descripciones ya traducidos en `en_us.json` y `es_es.json`.

> ⚠️ **Formato a validar:** se usó `"icon": { "id": "..." }` (codec de ItemStack moderno, el mismo
> que usan las recetas de este repo con `"result": { "id": ... }`). Si MC 26.3 exigiera `"item"`,
> hay que cambiar las 12 líneas `icon`.

### 3.2 Tags de compatibilidad entre mods (`c:` namespace)
- Bloques: `c:ores` (completado con las 3 menas) + `c:ores/{celidium,verdantite,astralite}`.
- Items: `c:ores(+subtags)`, `c:raw_materials(+subtags)`, `c:ingots(+subtags)`.

### 3.3 Test de regresión de economía
`common/src/test/java/com/amaro/stellarodyssey/assembly/RecipeMaterialConsistencyTest.java`
ata las recetas JSON al BOM único declarado en `AssemblyLogic`.

---

## 4. Riesgos conocidos a compilar/verificar (por orden de probabilidad)

1. **`RocketPassengerTeleportTest`** — dobles con `super(...)` de `ServerPlayer` (4 args),
   `ServerLevel` (10 args), `MinecraftServer` (11 args) y varios `@Override` específicos de MC 26.3.
   Es el primer fichero que puede fallar. Ruta segura: sustituir por `Unsafe.allocateInstance`
   sin constructores (patrón que ya usan los tests antiguos) y quitar los `@Override` no esenciales.
2. **Formato de los avances** (ver 3.1).
3. **`metamorphic_canyons.json` + `gliese_deep.json`** — clonados de ficheros válidos, pero si MC 26.3
   rechaza algo, el registro de dimensiones/biomas falla y el mundo no carga. Si pasa, la vía rápida
   es apuntar `gliese_deep` al bioma ya probado `stellarodyssey:bioluminescent_wastes`.
4. **`RecipeMaterialConsistencyTest`** — carga recursos por classpath
   (`/data/stellarodyssey/recipe/...`). Si el classpath de test no incluyera `src/main/resources`,
   cambiar a lectura por fichero.

---

## 5. Reglas del proyecto que NO se deben romper

- `common` no importa `net.neoforged` / `net.fabricmc` ni clases client-only fuera de `client`.
- Registros solo vía `DeferredRegister`; sin referencias estáticas a `Level`/`Entity`/`Player`.
- Sin dependencias circulares entre `worldgen`, `ecology` y `starmap`.
- ES/EN con claves idénticas en `lang/`.
- Las recetas de fundición usan `minecraft:smelting` / `minecraft:blasting` (NUNCA `furnace`/`blast_furnace`).

---

## 6. 🔴 HALLAZGO CRÍTICO — el viaje interplanetario está roto (NO parcheado, requiere compilar + decidir diseño)

**Fichero:** `common/src/main/java/com/amaro/stellarodyssey/entity/RocketEntity.java` → `warpToDestination()` (~línea 464).

### Qué hace hoy
```java
for (Entity passenger : this.getPassengers()) {
    if (passenger instanceof ServerPlayer player) {
        Vec3 pos = new Vec3(this.getX(), WARP_ARRIVAL_ALTITUDE, this.getZ());
        player.teleport(new TeleportTransition(target, pos, Vec3.ZERO, ...));
        player.sendSystemMessage(...);
    }
}
```
Solo se teletransporta al **jugador**. El cohete se queda en la dimensión de origen y allí ejecuta
ARRIVAL → LANDING → `discard()`.

### Consecuencias reales (game-breaking)
1. El jugador llega a **Y=320 sin vehículo** y cae al vacío.
2. Daño de caída ≈ `(250 - safeFall) * fallDamageMultiplier`: **~37 de daño en Nexus Moon**
   (0.16g, mult 0.16) y **~84 en Proxima B** (0.35g) → **muerte casi garantizada** (20 HP).
3. Las fases ARRIVAL/LANDING, sus sonidos (`ATMOSPHERIC_REENTRY`) y partículas ocurren en la
   dimensión de ORIGEN: el jugador nunca los ve.
4. El `LaunchCinematicOverlay` de llegada/aterrizaje **nunca se muestra**, porque exige
   `mc.player.getVehicle() instanceof RocketEntity` y el jugador ya no monta nada.
5. El cohete se descarta en el aire (Y≈320) de la dimensión de origen.

### Arreglo recomendado (elegir una opción y compilar)
**Opción A (mínima, mantiene el teletransporte a Y=320):** tras teletransportar a cada
`ServerPlayer`, crear un cohete de llegada en `target` y montarlo:
```java
ServerLevel target = serverLevel.getServer().getLevel(destinationKey);
// ... por cada ServerPlayer:
player.teleport(new TeleportTransition(target, pos, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
RocketEntity arrival = new RocketEntity(target, pos.x, pos.y, pos.z, this.getTierLevel());
arrival.setTargetDestination(destinationKey);
arrival.setPhase(RocketFlightPhase.ARRIVAL);
arrival.setPhaseTicks(0);
target.addFreshEntity(arrival);
player.startRiding(arrival, true);
```
y que `warpToDestination()` devuelva `boolean` para que el `case WARP` NO llame a `advancePhase()`
ni descarte por su cuenta cuando ya se hizo el traspaso.

> ⚠️ **Pero la Opción A por sí sola NO evita la muerte:** ARRIVAL dura 60 ticks y `applyDescent()`
> limita a −0.4 bloques/tick → solo desciende ~24 bloques desde Y=320. La LANDING congela el
> cohete en Y≈296 y luego lo descarta, eyección a 296 bloques del suelo.

**Opción B (recomendada):** que la llegada NO empiece en órbita. Teletransportar al jugador a la
**superficie** (`target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)`) y hacer que el
cohete de llegada ejecute solo LANDING (cinemática de toma de contacto). Elimina el daño de caída
y conserva la cinemática.

### ⚠️ Impacto en los tests (obligatorio al elegir opción)
`RocketPassengerTeleportTest.testWarpTeleportationExecution` afirma literalmente:
```java
assertEquals(RocketEntity.WARP_ARRIVAL_ALTITUDE, transition.position().y, 0.001);
```
→ **la Opción B rompe esa aserción** y hay que actualizarla. La Opción A la mantiene, pero
añade un `ServerLevel` real con chunk source: el doble `TestServerLevel` (creado con
`Unsafe.allocateInstance`) hará NPE al crear/registrar la entidad, así que hay que ajustar el test
(p. ej. guardar el spawn tras `target.getChunkSource() != null` o capturar la excepción).

**No se implementó a propósito:** cambia el contrato de un test existente y exige decisión de
diseño (¿aterrizaje o caída libre?), y sin compilador el riesgo de empeorarlo es alto.

---

## 7. Ronda 2 de depuración — bugs corregidos (todos ya en disco)

### 7.1 🔴 DUPE INFINITO DE COHETES (corregido)
`AssemblyLogic.validate()` contaba **slots** en vez de **cantidades**: una pila de 2+ componentes
satisfacía "exactamente uno de cada" y `consumeComponents()` solo restaba 1 unidad por slot, así que
el mismo contenido fabricaba cohetes sin límite.
**Fix (doble barrera):**
- `rocket/AssemblyLogic.java` → `counts.merge(type, stack.getCount(), Integer::sum)`.
- `block/entity/AssemblyTableMenu.java` → slots de componentes con `getMaxStackSize() == 1`.
- **Nuevo test:** `common/src/test/java/com/amaro/stellarodyssey/assembly/AssemblyStackSizeGuardTest.java`.

### 7.2 24 claves de idioma ausentes (corregidas)
El código referenciaba claves que no existían y se mostraban como texto crudo:
- `block.stellarodyssey.oxygen_refiller` / `oxygen_sealer`
- `dimension.stellarodyssey.gliese_deep` (crítico tras habilitar Gliese Deep)
- `tier.stellarodyssey.{celidium,verdantite,astralite,voidstalker,chronostone}`
- `tooltip.stellarodyssey.rocket_component.tier` / `.type`
- `rocket_component.stellarodyssey.{cone,fin,tank,engine,plate,thruster,guidance,heat_shield}`
- `message.stellarodyssey.{sealer_sealed,sealer_unsealed,oxygen_refilled,oxygen_already_full,oxygen_all_refilled,oxygen_no_tanks}`
Añadidas en `en_us.json` y `es_es.json` (claves idénticas).

### 7.3 Ambience alienígena: sonaba una vez y solo en Proxima B (corregido)
`client/AlienAmbienceHandler.java` usaba un flag booleano que impedía volver a reproducir el sonido
para siempre, y solo miraba `ModDimensions.PROXIMA_B`.
**Fix:** se re-dispara cada `AMBIENCE_INTERVAL_TICKS` (300 ticks = 15 s) y se activa en cualquier
cuerpo celeste con atmósfera no respirable y no-vacío (Proxima B, Exotic Prime, Gliese Deep);
Nexus Moon sigue en silencio.

### 7.4 Side-safety en el Recargador de Oxígeno (corregido)
`block/OxygenRefillerBlock.java` → `useWithoutItem()` mutaba los `ItemStack` del inventario en el
**cliente y el servidor** a la vez (solo el sonido estaba guardado). Ahora el rellenado es
exclusivamente server-authoritative.

### 7.5 Texturas de máquinas reutilizadas (pendiente, ver §8)
`models/block/oxygen_sealer.json` usa la textura de `assembly_table`, `oxygen_sealer_active` la de
`launch_pad` y `oxygen_refiller` la de `launch_pad_base`, porque `tools/TextureGen.java`
→ `machineBlocks()` no genera texturas para esos tres bloques.

---

## 8. Pendiente de contenido: texturas dedicadas de máquina

Los tres bloques usan texturas de otros bloques como placeholder. No rompe nada, pero se ven
genéricos. Para darles arte propio hay que hacer **los tres pasos en este orden**, porque si se
cambian los modelos sin generar antes las texturas, los bloques salen **morados/negros**:

1. Extender `tools/TextureGen.java` → `machineBlocks()` para escribir
   `textures/block/oxygen_sealer.png`, `textures/block/oxygen_sealer_active.png` y
   `textures/block/oxygen_refiller.png` (usar los helpers existentes: `S = 32`, `valueNoise`, `write`, `outline`).
2. Ejecutar: `java tools/TextureGen.java` (desde la raíz del proyecto).
3. Actualizar los tres modelos en `assets/stellarodyssey/models/block/` para que apunten a
   `stellarodyssey:block/oxygen_sealer`, `...:block/oxygen_sealer_active` y `...:block/oxygen_refiller`.

> ⚠️ Si se hace el paso 3 sin el 2, los bloques se ven morados/negros.

### Otros hallazgos menores ya verificados (sin acción necesaria)
- Todas las texturas referenciadas por los modelos de items y el resto de bloques **existen**.
- Los 51 descriptores `assets/stellarodyssey/items/*.json` coinciden 1:1 con los 51 items registrados.
- Los 4 `.ogg` referenciados por `sounds.json` existen.
- `PlanetaryGravityManager` es genérico vía `CelestialBodyRegistry`, así que Gliese Deep (1.35g)
  ya funciona sin tocar código.
- El `dimension_type` `alien_planet` es compartido por los 4 planetas y está en el tag
  `stellarodyssey:vacuum`; es semánticamente impreciso para Exotic Prime (0.85 atm) y Gliese Deep
  (2.5 atm), pero el código decide por `CelestialBodyRegistry` (no por el tag) para dimensiones
  registradas, así que **no afecta al juego**. Solo importa si otro mod lee ese tag.
