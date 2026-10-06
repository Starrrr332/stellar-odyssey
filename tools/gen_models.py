"""Generates block models, blockstates and item models for Stellar Odyssey.

Run from the project root:  python tools/gen_models.py
"""
import json
from pathlib import Path

ASSETS = Path("common/src/main/resources/assets/stellarodyssey")
BLOCK_MODELS = ASSETS / "models/block"
ITEM_MODELS = ASSETS / "models/item"
BLOCKSTATES = ASSETS / "blockstates"
BLOCK_MODELS.mkdir(parents=True, exist_ok=True)
ITEM_MODELS.mkdir(parents=True, exist_ok=True)
BLOCKSTATES.mkdir(parents=True, exist_ok=True)


def write(path, data):
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")
    print(f"wrote {path}")


# 1. Block models (cube_all)
for block in ["assembly_table", "launch_pad", "launch_pad_base"]:
    write(BLOCK_MODELS / f"{block}.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"stellarodyssey:block/{block}"},
    })

# 2. Blockstates
for block in ["assembly_table", "launch_pad", "launch_pad_base"]:
    write(BLOCKSTATES / f"{block}.json", {
        "variants": {"": {"model": f"stellarodyssey:block/{block}"}},
    })

# 3. Item models: rocket components + assembled rockets (generated sprites)
for tier in [1, 2, 3]:
    for ctype in ["cone", "fin", "tank", "engine", "plate", "thruster", "guidance", "heat_shield"]:
        name = f"rocket_{ctype}_t{tier}"
        write(ITEM_MODELS / f"{name}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"stellarodyssey:item/{name}"},
        })
    name = f"rocket_t{tier}"
    write(ITEM_MODELS / f"{name}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"stellarodyssey:item/{name}"},
    })

# 4. Block item models
for block in ["assembly_table", "launch_pad", "launch_pad_base"]:
    write(ITEM_MODELS / f"{block}.json", {
        "parent": f"stellarodyssey:block/{block}",
    })

print("All models generated.")
