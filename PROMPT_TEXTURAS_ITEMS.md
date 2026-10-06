# PROMPT: Regenerar texturas de items de Stellar Odyssey (detalladas, bonitas y pulidas)

Eres un artista de texturas de Minecraft. Debes regenerar **todas** las texturas de items
del mod **Stellar Odyssey** (MC 26.3, Fabric/NeoForge) para que sean mucho más detalladas,
bonitas y pulidas que las actuales (que son procedurales y básicas).

## Contexto del mod

- **Dirección de arte**: "bioluminiscente de espacio profundo" — colores vivos, brillos
  suaves, materiales metálicos y cristalinos, estética sci-fi de exploración espacial.
- **Resolución**: 32×32 píxeles (2× vanilla) para todos los items, EXCEPTO `starship_3d`
  que es 64×64 (textura de un modelo 3D).
- **Formato**: PNG con canal alfa (RGBA). Fondo transparente.
- **Estilo**: consistente con el resto del mod. Cada item debe verse nítido, con volumen,
  iluminación y detalle, no como un sprite plano.

## RUTA de los archivos a reemplazar

```
C:\Users\amaro\Documents\antigravity\blissful-lavoisier\common\src\main\resources\assets\stellarodyssey\textures\item\
```

Reemplaza cada `<nombre>.png` existente. NO cambies los nombres de archivo.

## LISTA COMPLETA de items y su diseño

### Minerales y lingotes (3 tiers)
| Archivo | Diseño |
|---|---|
| `raw_celidium.png` | Trozo de mineral en bruto, cristales ámbar/dorados `#D97724` con brillo cálido, vetas oscuras |
| `raw_verdantite.png` | Trozo de mineral en bruto, cristales esmeralda `#22C55E` tóxicos, brillo verde venenoso |
| `raw_astralite.png` | Trozo de mineral en bruto, cristales cian `#06B6D4` helados, brillo glacial |
| `celidium_ingot.png` | Lingote metálico dorado/ámbar pulido, brillo metálico, bordes biselados |
| `verdantite_ingot.png` | Lingote metálico esmeralda pulido, brillo metálico, bordes biselados |
| `astralite_ingot.png` | Lingote metálico cian pulido, brillo metálico, bordes biselados |

### Componentes de cohete (8 tipos × 3 tiers)
Cada componente tiene 3 variantes (t1/t2/t3) que deben diferenciarse por **material y color**:
- **t1** = hierro/plateado (gris metálico)
- **t2** = cobre/bronce (naranja cobrizo)
- **t3** = titanio/negro con acentos cian (material premium)

| Archivo | Diseño |
|---|---|
| `rocket_cone_t1/t2/t3.png` | Cono de cohete aerodinámico, punta afilada, remaches, brillo metálico |
| `rocket_fin_t1/t2/t3.png` | Aleta de cohete curva, perfil aerodinámico, remaches |
| `rocket_tank_t1/t2/t3.png` | Tanque de combustible cilíndrico, bandas de refuerzo, válvula |
| `rocket_engine_t1/t2/t3.png` | Motor de cohete con tobera, aletas de refrigeración, detalle mecánico |
| `rocket_plate_t1/t2/t3.png` | Placa de blindaje con remaches y paneles |
| `rocket_thruster_t1/t2/t3.png` | Propulsor pequeño con tobera y detalle de combustión |
| `rocket_guidance_t1/t2/t3.png` | Módulo de guiado con antena y sensor, acentos luminosos |
| `rocket_heat_shield_t1/t2/t3.png` | Escudo térmico con patrón de baldosas cerámicas |

### Cohetes ensamblados (3 tiers)
| Archivo | Diseño |
|---|---|
| `rocket_t1.png` | Cohete completo pequeño, plateado, cono, aletas, motor |
| `rocket_t2.png` | Cohete completo mediano, cobre/bronce, más detalle |
| `rocket_t3.png` | Cohete completo grande, negro/titanio con acentos cian, premium |

### Equipamiento
| Archivo | Diseño |
|---|---|
| `oxygen_tank.png` | Tanque de oxígeno cilíndrico con banda de peligro amarilla, válvula, medidor |
| `spacesuit_helmet.png` | Casco de traje espacial con visor de cristal, luces, detalle |
| `spacesuit_chestplate.png` | Pechera de traje espacial con panel de control, tubos, detalle |
| `spacesuit_leggings.png` | Piernas de traje espacial con rodilleras, tubos |
| `spacesuit_boots.png` | Botas de traje espacial con suela gruesa, detalle |

### Nave
| Archivo | Diseño |
|---|---|
| `starship.png` | Nave estelar (icono 2D), silueta elegante, motores con brillo |
| `starship_3d.png` | **64×64** — textura UV del modelo 3D de la nave (casco, cabina de cristal, motores, alas) |

## CAPAS EMISIVAS (importante)

Algunos items tienen una capa `_emissive` que brilla en la oscuridad. Debes regenerar
**también** estas capas, alineadas píxel a píxel con la textura base:

- `oxygen_tank_emissive.png` (y su `.mcmeta`)
- `spacesuit_helmet_emissive.png`
- `spacesuit_chestplate_emissive.png`
- `spacesuit_leggings_emissive.png`
- `spacesuit_boots_emissive.png`
- `starship_emissive.png`

La capa emisiva solo debe tener píxeles blancos/brillantes en las zonas que emiten luz
(visores, luces, motores, cristales), y transparente en el resto.

## REQUISITOS DE CALIDAD

1. **Detalle**: cada item debe tener volumen, sombreado, brillos especulares y textura de
   material (metal, cristal, cerámica). Nada de sprites planos.
2. **Consistencia**: los 3 tiers de cada componente deben verse como la misma pieza en
   materiales distintos. Los cohetes t1/t2/t3 deben verse como la misma familia en escala.
3. **Nitidez**: bordes limpios, sin píxeles sueltos ni ruido. El item debe leerse bien a
   tamaño 16×16 (mitad del tamaño real).
4. **Paleta**: coherente con la dirección de arte bioluminiscente. Los acentos cian/verde/
   ámbar deben destacar.
5. **Fondo transparente** en todos.

## ENTREGA

Genera los archivos PNG directamente en la ruta indicada, reemplazando los existentes.
Al terminar, confirma la lista de archivos generados y verifica que todos los nombres
coinciden exactamente con los originales.

## VERIFICACIÓN

Después de generar, comprueba:
- Que cada `<nombre>.png` exista y tenga el tamaño correcto (32×32, o 64×64 para `starship_3d`).
- Que las capas `_emissive` estén alineadas con su textura base (mismas dimensiones).
- Que no haya archivos nuevos con nombres inventados (solo reemplazar los existentes).
