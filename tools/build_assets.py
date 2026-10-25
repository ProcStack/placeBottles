"""Generates the placebottles item definitions (colors), starter 3D models and textures.

Usage (from the project root):  python tools/build_assets.py [--models] [--textures]
  - always rewrites assets/placebottles/items/*.json from tools/colors.json
  - --models    also rewrites the starter bottle/flask models (overwrites hand-made edits!)
  - --textures  also rewrites the starter glass/liquid textures (needs Pillow)
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "placebottles"


def signed_argb(hex_color: str) -> int:
    value = 0xFF000000 | int(hex_color.lstrip("#"), 16)
    return value - (1 << 32) if value >= 1 << 31 else value


def item_definition(model: str, tint: dict | None) -> dict:
    node = {"type": "minecraft:model", "model": f"placebottles:item/{model}"}
    if tint:
        node["tints"] = [tint]
    return {"model": node}


def potion_tint(default_hex: str) -> dict:
    return {"type": "minecraft:potion", "default": signed_argb(default_hex)}


def constant_tint(hex_color: str) -> dict:
    return {"type": "minecraft:constant", "value": signed_argb(hex_color)}


def write_items() -> None:
    c = json.loads((ROOT / "tools" / "colors.json").read_text())
    defs = {
        "glass_bottle": item_definition("bottle_empty", None),
        "potion": item_definition("bottle_full", potion_tint(c["potion_default"])),
        "splash_potion": item_definition("flask_full", potion_tint(c["splash_potion_default"])),
        "lingering_potion": item_definition("flask_full", potion_tint(c["lingering_potion_default"])),
        "ominous_bottle": item_definition("bottle_full", constant_tint(c["ominous_bottle"])),
        "honey_bottle": item_definition("bottle_full", constant_tint(c["honey_bottle"])),
        "experience_bottle": item_definition("flask_full", constant_tint(c["experience_bottle"])),
        "dragon_breath": item_definition("bottle_full", constant_tint(c["dragon_breath"])),
    }
    out = ASSETS / "items"
    out.mkdir(parents=True, exist_ok=True)
    for name, data in defs.items():
        (out / f"{name}.json").write_text(json.dumps(data, indent=2) + "\n")


def box(name, frm, to, texture, tint=None):
    face = {"uv": [0, 0, 16, 16], "texture": texture}
    if tint is not None:
        face["tintindex"] = tint
    return {"name": name, "from": frm, "to": to,
            "faces": {side: dict(face) for side in ("north", "east", "south", "west", "up", "down")}}


def write_models() -> None:
    glass, liquid = "#glass", "#liquid"
    shapes = {
        "bottle_empty": [
            box("body", [5, 0, 5], [11, 6, 11], glass),
            box("neck", [6.5, 6, 6.5], [9.5, 8, 9.5], glass),
            box("rim", [6, 8, 6], [10, 9, 10], glass),
        ],
        "bottle_full": [
            box("body", [5, 0, 5], [11, 6, 11], glass),
            box("neck", [6.5, 6, 6.5], [9.5, 8, 9.5], glass),
            box("rim", [6, 8, 6], [10, 9, 10], glass),
            box("liquid", [5.5, 0.5, 5.5], [10.5, 5, 10.5], liquid, 0),
        ],
        "flask_full": [
            box("body", [4, 0, 4], [12, 7, 12], glass),
            box("neck", [6.5, 7, 6.5], [9.5, 10, 9.5], glass),
            box("rim", [6, 10, 6], [10, 11, 10], glass),
            box("liquid", [4.5, 0.5, 4.5], [11.5, 6, 11.5], liquid, 0),
        ],
    }
    out = ASSETS / "models" / "item"
    out.mkdir(parents=True, exist_ok=True)
    for name, elements in shapes.items():
        model = {
            "textures": {"glass": "placebottles:item/glass", "liquid": "placebottles:item/liquid",
                         "particle": "placebottles:item/glass"},
            "elements": elements,
            "display": {"gui": {"rotation": [30, 225, 0], "scale": [1.2, 1.2, 1.2]}},
        }
        (out / f"{name}.json").write_text(json.dumps(model, indent=2) + "\n")


def write_textures() -> None:
    from PIL import Image

    out = ASSETS / "textures" / "item"
    out.mkdir(parents=True, exist_ok=True)
    glass = Image.new("RGBA", (16, 16))
    liquid = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            edge = x in (0, 15) or y in (0, 15)
            glass.putpixel((x, y), (215, 240, 250, 255) if edge else (200, 230, 245, 120))
            shade = 255 - (y * 3)
            liquid.putpixel((x, y), (shade, shade, shade, 255))
    glass.save(out / "glass.png")
    liquid.save(out / "liquid.png")


if __name__ == "__main__":
    write_items()
    if "--models" in sys.argv:
        write_models()
    if "--textures" in sys.argv:
        write_textures()
    print("done")
