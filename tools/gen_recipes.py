"""Generates crafting recipes for Stellar Odyssey rocket progression.

Run from the project root:  python tools/gen_recipes.py
"""
import json
from pathlib import Path

OUT = Path("common/src/main/resources/data/stellarodyssey/recipe")
OUT.mkdir(parents=True, exist_ok=True)

# Tier materials: (primary, secondary) - difficulty scales with tier
TIER_MATERIALS = {
    1: ("minecraft:iron_ingot", "minecraft:copper_ingot"),
    2: ("stellarodyssey:celidium_ingot", "minecraft:gold_ingot"),
    3: ("stellarodyssey:verdantite_ingot", "stellarodyssey:astralite_ingot"),
}

# Component type -> shaped pattern (X = primary, Y = secondary)
COMPONENT_PATTERNS = {
    "cone":        (["X X", "XXX", " X "], "X"),
    "fin":         (["X  ", "XX ", "XXX"], "X"),
    "tank":        (["X X", "XXX", "XXX"], "X"),
    "engine":      ([" X ", "XXX", "X X"], "X"),
    "plate":       (["XXX", "XYX", "XXX"], "X"),
    "thruster":    ([" X ", " X ", "XXX"], "X"),
    "guidance":    ([" X ", "XYX", " X "], "X"),
    "heat_shield": (["XXX", "X X", "XXX"], "X"),
}


def shaped(name, pattern, key, result, count=1):
    recipe = {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "key": key,
        "pattern": pattern,
        "result": {"id": result, "count": count},
    }
    path = OUT / f"{name}.json"
    path.write_text(json.dumps(recipe, indent=2), encoding="utf-8")
    print(f"wrote {path}")


# 1. Rocket components (8 types x 3 tiers)
for tier, (primary, secondary) in TIER_MATERIALS.items():
    for ctype, (pattern, _) in COMPONENT_PATTERNS.items():
        name = f"rocket_{ctype}_t{tier}"
        key = {"X": primary}
        if "Y" in "".join(pattern):
            key["Y"] = secondary
        shaped(name, pattern, key, f"stellarodyssey:{name}")

# 2. Assembly table: iron block + crafting table + redstone
shaped(
    "assembly_table",
    ["III", "C C", "III"],
    {"I": "minecraft:iron_ingot", "C": "minecraft:crafting_table"},
    "stellarodyssey:assembly_table",
)

# 3. Launch pad center: iron block + redstone + stone
shaped(
    "launch_pad",
    ["IRI", "RSR", "IRI"],
    {"I": "minecraft:iron_ingot", "R": "minecraft:redstone", "S": "minecraft:stone"},
    "stellarodyssey:launch_pad",
)

# 4. Launch pad base: stone + iron nugget
shaped(
    "launch_pad_base",
    ["SSS", "SIS", "SSS"],
    {"S": "minecraft:stone", "I": "minecraft:iron_nugget"},
    "stellarodyssey:launch_pad_base",
    8,
)

print("All recipes generated.")
