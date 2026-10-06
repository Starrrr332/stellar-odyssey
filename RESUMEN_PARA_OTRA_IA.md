# Stellar Odyssey — Resumen técnico para continuar el desarrollo

> Documento de transferencia para otra IA. Describe el estado actual del mod, la arquitectura,
> todo lo implementado, los bugs corregidos y lo que queda pendiente.

---

## 1. Datos del proyecto

| Campo | Valor |
|---|---|
| **Ruta** | `C:\Users\amaro\Documents\antigravity\blissful-lavoisier` |
| **Mod** | Stellar Odyssey (id: `stellarodyssey`) |
| **Minecraft** | 26.3 (versión nueva, sistema de items/recetas/loot cambiado) |
| **Loaders** | Fabric + NeoForge (multi-loader vía **Architectury Loom**) |
| **Java** | 25 (requerido por MC 26.3) |
| **Dependencias** | fabric-api `>=0.161.0`, architectury `>=22.0.3`, fabricloader `>=0.19.5` |
| **Build** | `./gradlew build` (compila common + fabric + neoforge, corre 34 tests) |
| **Jar de salida** | `fabric/build/libs/stellarodyssey-fabric-0.1.0+mc26.3.jar` |

**Estructura**: 77 clases Java, 177 JSON de recursos, 66 texturas PNG.
Paquetes: `api/`, `block/`, `block/entity/`, `client/`, `core/`, `entity/`, `item/`,
`lifesupport/`, `network/`, `registry/`, `registry/tiers/`, `rocket/`, `satellites/`,
`world/`.

---

## 2. ⚠️ CRÍTICO: cambios de MC 26.3 que rompen mods antiguos

Estos son los cambios de formato que causaron los bugs y que **cualquier código nuevo debe respetar**:

### 2.1 Modelos de item — descriptor `items/*.json` (OBLIGATORIO)
En MC 26.3 el punto de entrada de un item **ya no es** `models/item/<item>.json`.
Es un descriptor nuevo en `assets/<mod>/items/<item>.json`:

```json
{ "model": { "type": "minecraft:model", "model": "stellarodyssey:item/rocket_cone_t1" } }
```

- Sin este descriptor, el item cae al modelo "missing" (morado/negro) aunque exista
  `models/item/`. Los bloques colocados en el mundo sí se ven; los items en inventario no.
- Vanilla 26.3 tiene 1658 descriptores `items/` y solo 1309 `models/item/` (ej. `acacia_button`
  solo tiene `items/acacia_button.json`).
- **El mod ya tiene los 48 descriptores** (`assets/stellarodyssey/items/*.json`).
- Los modelos con `elements` (3D) siguen soportados (1 en vanilla).

### 2.2 Recetas de fundición — `smelting`/`blasting` (NO `furnace`/`blast_furnace`)
Los tipos de receta `minecraft:furnace` y `minecraft:blast_furnace` **ya no existen**.
Usar `minecraft:smelting` y `minecraft:blasting`. Formato vanilla 26.3:

```json
{
  "type": "minecraft:smelting",
  "cookingtime": 200,
  "experience": 4.5,
  "ingredient": "stellarodyssey:raw_celidium",
  "result": { "id": "stellarodyssey:celidium_ingot" }
}
```

> ⚠️ **Un tipo de receta inválido rompe TODO el registro** (`Registry loading errors` →
> `Failed to load registries`), lo que **destruye la generación de mundo**. Este fue el bug
> principal de worldgen.

### 2.3 Loot tables — condición silk touch nueva
`minecraft:match_tool` con predicate de encantamiento ya no funciona. Usar
`minecraft:tool/can_silk_touch` como condición directa de la entrada:

```json
{ "type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": "stellarodyssey:alien_turf" }
```

### 2.4 Menús — Fabric exige `ExtendedMenuProvider`
Si el `MenuType` se registra con `MenuRegistry.ofExtended(...)`, en Fabric el bloque **debe**
abrirse con `MenuRegistry.openExtendedMenu(serverPlayer, provider)` y el provider debe
implementar `dev.architectury.registry.menu.ExtendedMenuProvider` (método `saveExtraData(FriendlyByteBuf)`).
Usar `SimpleMenuProvider` con un menú extended lanza:
`[Fabric] Extended menu ... must be opened with an ExtendedMenuProvider!`

### 2.5 Sonidos — `sounds.json` de assets ya no existe en vanilla
MC 26.3 ya no usa `assets/minecraft/sounds.json`. Los `.ogg` del mod están bien y se reproducen;
el aviso "Missing sound for event" es un WARN menor. No bloquea nada.

---

## 3. Contenido implementado

### 3.1 Progresión de cohetes por niveles (feature principal)
- **3 tiers de cohete** (`RocketTierRegistry`): Pioneer→Luna, Voyager→Proxima B, Odyssey→Exotic Prime.
- **24 componentes crafteables** (`RocketComponentRegistry`): `rocket_cone/fin/tank/engine/plate/thruster/guidance/heat_shield` × `t1/t2/t3`.
- **Lógica de ensamblaje pura** (`AssemblyLogic.validate`): valida el conjunto exacto de componentes por tier (4/6/8).
- **Mesa de ensamblaje** (`AssemblyTableBlock` + `AssemblyTableBlockEntity` + `AssemblyTableMenu`):
  8 slots de componentes (2×4) + slot de resultado. Menú extended (ver 2.4).
- **Plataforma de lanzamiento 3×3** (`launch_pad` + `launch_pad_base`).
- **Cohete entidad** (`RocketEntity`): secuencia de lanzamiento, teleport entre dimensiones.
- **Items de cohete** (`RocketItem`): `rocket_t1/t2/t3`.

### 3.2 Minerales alienígenas (3 tiers)
| Mineral | Color | Tier | Tag de picado |
|---|---|---|---|
| Celidium | Ámbar `#D97724` | 1 | `needs_iron_tool` |
| Verdantite | Esmeralda `#22C55E` | 2 | `needs_diamond_tool` |
| Astralite | Cian `#06B6D4` | 3 | `needs_netherite_tool` (tag `neoforge:needs_netherite_tool`) |

Cada uno: bloque de mena, item en bruto (`raw_*`), lingote (`*_ingot`), recetas de fundición
(smelting + blasting), texturas, modelos, blockstates, lang EN/ES.

### 3.3 Sistema de soporte vital
- Oxígeno, traje espacial modular (casco/pecho/piernas/botas), tanque de O₂.
- HUD sci-fi (`OxygenHudOverlay`), sincronización de red (`oxygen_sync`).
- Protección contra descompresión.

### 3.4 Dimensiones planetarias
- `proxima_b`, `exotic_prime`, `nexus_moon` (3 dimensiones).
- Biome `bioluminescent_wastes`, surface rules procedurales (`PlanetarySurfaceRules`).
- Gravedad modificada, flora alienígena, IA de baja gravedad.

### 3.5 Nave estelar
- Entidad `StarshipEntity`, controles de vuelo, modelo 3D Blockbench, transición orbital warp.

### 3.6 Texturas (66 PNG)
- Generadas proceduralmente con `tools/TextureGen.java` (Java, sin dependencias).
- Dirección de arte "bioluminiscente de espacio profundo".
- Capas `_emissive` alineadas píxel a píxel con la textura base.

---

## 4. Bugs corregidos (en orden)

| Commit | Bug | Causa raíz |
|---|---|---|
| `9062651` | Items morados/negros en inventario | Faltaban descriptores `items/*.json` (ver 2.1) |
| `affb9fb` | Minerales nuevos sin assets | Faltaban texturas, modelos, blockstates, recetas, tags, lang |
| `f1cd7bf` | **Worldgen roto** | Recetas con tipo `furnace`/`blast_furnace` inválido → registro roto (ver 2.2) |
| `f1cd7bf` | Loot table `alien_turf` inválida | Condición silk touch antigua (ver 2.3) |
| `f1cd7bf` | Item 3D de nave con UVs fuera de rango | UVs en píxeles (0-58) en vez de grid 0-16 → `translucency out of bounds` |
| `22012a2` | Mesa de ensamblaje no abría | Menú extended abierto con `SimpleMenuProvider` (ver 2.4) |
| `22012a2` | Dupe de items al cerrar mesa | `removed()` devolvía items sin vaciar el contenedor |

---

## 5. Instalación en Prism Launcher

⚠️ **La instancia tiene 4 carpetas de mods** y el juego lee `minecraft/mods`. Hay que
actualizar el jar en **todas**:

```
C:\Users\amaro\Downloads\Prism Launcher\instances\Stellar Odyssey\mods\
C:\Users\amaro\Downloads\Prism Launcher\instances\Stellar Odyssey\minecraft\mods\
C:\Users\amaro\Downloads\Prism Launcher\instances\Stellar Odyssey\.minecraft\mods\
C:\Users\amaro\AppData\Roaming\PrismLauncher\instances\Stellar Odyssey\mods\  (y minecraft/mods, .minecraft/mods)
```

- Prism Launcher es **portable** (está en `Downloads/Prism Launcher`, no en AppData).
- Cuenta offline (`Stargolden`). Se lanza con `./prismlauncher.exe --launch "Stellar Odyssey"`.
- El jar actualizado también se copia a `C:\Users\amaro\Downloads\stellarodyssey-fabric-0.1.0+mc26.3.jar`.

---

## 6. Verificación

- `./gradlew build` → **BUILD SUCCESSFUL** (common + fabric + neoforge), **34/34 tests**.
- Log de arranque limpio: 0 errores de registro, 0 de worldgen, 0 de menú, 0 de translucency.
- Las 3 dimensiones se registran y cargan (`Saving chunks for level .../stellarodyssey:proxima_b`).
- El worldgen genera terreno (15 region files en el overworld; las dimensiones alienígenas
  generan al entrar por primera vez).

---

## 7. Pendiente / no verificado

1. **Minerales nuevos en worldgen**: los 3 minerales (celidium/verdantite/astralite) están
   registrados y crafteables, pero **no se generan en el mundo** — `ModConfiguredFeatures.java`
   solo tiene features para `ALIEN_ORE`. Falta añadir features/placement para los 3 minerales
   nuevos (o reutilizar `OreDistributionConfig`).
2. **Verificación visual en juego**: no se ha confirmado a ojo que las texturas se vean bien
   (solo programáticamente). Falta que un humano entre al juego.
3. **Sonidos**: los `.ogg` están bien pero el aviso "Missing sound" persiste (ver 2.5). Si se
   oye silencio al acelerar la nave, investigar el formato de sonido de MC 26.3.
4. **Secuencia de lanzamiento / teleport**: compila y pasa tests, pero no probado en partida real.
5. **Logs de runtime**: `common/logs/*.log.gz` se modifican al ejecutar el juego (no commitearlos).

---

## 8. Comandos útiles

```bash
cd 'C:/Users/amaro/Documents/antigravity/blissful-lavoisier'
./gradlew build -x test --console=plain   # build rápido sin tests
./gradlew build --console=plain           # build completo con tests
java tools/TextureGen.java                # regenera las 66 texturas
```

**Regenerar texturas**: `tools/TextureGen.java` escribe directamente en
`common/src/main/resources/assets/stellarodyssey/textures/`. Añadir nuevos minerales/items
allí y re-ejecutar.

---

## 9. Notas de estilo

- Multi-loader: usar APIs de Architectury (`DeferredRegister`, `MenuRegistry`, `RegistrySupplier`).
- Los items se registran con un helper local `register("nombre", ...)` en `ModItems.java`.
- Los componentes/cohetes se registran en bucles (no con literales) — al auditar assets,
  expandir los bucles manualmente.
- Texturas 32×32 (2× vanilla) para bloques/items, 128×128 para la nave, 64×64 para armadura.
- Todo el texto visible al usuario en español (`es_es.json`) e inglés (`en_us.json`), claves idénticas.
