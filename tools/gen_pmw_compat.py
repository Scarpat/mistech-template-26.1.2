"""Integração com Productive Metalworks + assets dos metais derretidos. Uso: python3 tools/gen_pmw_compat.py"""
import json, os
RES = "src/main/resources"
METALS = {"mithril": ("Mithril", "Mithril"), "adamantine": ("Adamantine", "Adamantina"), "orichalcum": ("Orichalcum", "Oricalco")}
# temperatura mínima da fundição: lava = 1500, bobina elétrica = 1500, bobina de alta potência = 3000
TEMPS = {"mithril": 1000, "adamantine": 1500, "orichalcum": 2500}
COND = [{"type": "neoforge:mod_loaded", "modid": "productivemetalworks"}]
def w(path, data):
    full = os.path.join(RES, path); os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False); f.write("\n")
for m in METALS:
    n = f"molten_{m}"
    # assets
    w(f"assets/mistech/blockstates/{n}.json", {"variants": {"": {"model": f"mistech:block/{n}"}}})
    w(f"assets/mistech/models/block/{n}.json", {"textures": {"particle": f"mistech:block/{n}_still"}})
    w(f"assets/mistech/items/{n}_bucket.json", {"model": {"type": "minecraft:model", "model": f"mistech:item/{n}_bucket"}})
    w(f"assets/mistech/models/item/{n}_bucket.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mistech:item/{n}_bucket"}})
    # tag de fluido comum
    w(f"data/c/tags/fluid/{n}.json", {"replace": False, "values": [f"mistech:{n}", f"mistech:flowing_{n}"]})
    # fundir
    def melt(kind, tag, amount):
        w(f"data/mistech/recipe/compat/productivemetalworks/melting/{m}_{kind}.json", {
            "neoforge:conditions": COND, "type": "productivemetalworks:item_melting", "ingredient": f"#{tag}",
            "maximum_temperature": 0, "minimum_temperature": TEMPS[m], "result": [{"amount": amount, "id": f"mistech:{n}"}]})
    melt("ingot", f"c:ingots/{m}", 90)
    melt("raw", f"c:raw_materials/{m}", 180)
    melt("ore", f"c:ores/{m}", 270)
    melt("block", f"c:storage_blocks/{m}", 810)
    # moldar
    w(f"data/mistech/recipe/compat/productivemetalworks/casting/{m}_ingot.json", {
        "neoforge:conditions": COND, "type": "productivemetalworks:item_casting", "cast": "productivemetalworks:ingot_cast",
        "consume_cast": False, "fluid": {"amount": 90, "ingredient": f"#c:{n}"}, "result": {"id": f"mistech:{m}_ingot"}})
    w(f"data/mistech/recipe/compat/productivemetalworks/casting/{m}_block.json", {
        "neoforge:conditions": COND, "type": "productivemetalworks:block_casting",
        "consume_cast": False, "fluid": {"amount": 810, "ingredient": f"#c:{n}"}, "result": {"id": f"mistech:{m}_block"}})
# unidades (lingote/bloco) e combustíveis — data maps do PMW (só lidos com ele instalado)
w("data/productivemetalworks/data_maps/fluid/unit_map.json", {"values": {
    f"mistech:molten_{m}": {"units": [{"amount": 90, "unit": "ingot"}, {"amount": 810, "unit": "block"}]} for m in METALS}})
w("data/productivemetalworks/data_maps/fluid/fuel_map.json", {"values": {
    "mistech:impure_essence": {"temperature": 2500, "consumption": 0.15, "speed": 0.75},
    "mistech:purified_essence": {"temperature": 3000, "consumption": 0.1, "speed": 1.0}}})
# traduções
def add_lang(file, entries):
    full = os.path.join(RES, "assets/mistech/lang", file)
    text = open(full, encoding="utf-8").read(); json.loads(text)
    entries = {k: v for k, v in entries.items() if json.dumps(k) + ":" not in text.replace('": ', '":')}
    if not entries: print("lang", file, "nada novo"); return
    body = text.rstrip(); assert body.endswith("}"); body = body[:-1].rstrip()
    text = body + ",\n" + ",\n".join(f"  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}" for k, v in entries.items()) + "\n}\n"
    json.loads(text); open(full, "w", encoding="utf-8", newline="").write(text); print("lang", file, len(entries))
en, pt = {}, {}
for m, (e, p) in METALS.items():
    en[f"block.mistech.molten_{m}"] = f"Molten {e}"; pt[f"block.mistech.molten_{m}"] = f"{p} Derretido" if m != "adamantine" else f"{p} Derretida"
    en[f"item.mistech.molten_{m}_bucket"] = f"Molten {e} Bucket"; pt[f"item.mistech.molten_{m}_bucket"] = f"Balde de {pt[f'block.mistech.molten_{m}']}"
add_lang("en_us.json", en); add_lang("pt_br.json", pt)
print("ok")
