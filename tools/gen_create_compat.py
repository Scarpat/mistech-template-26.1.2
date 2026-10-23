"""Gera as receitas de compatibilidade com o Create em data/mistech/recipe/compat/create.

Todas levam a condição neoforge:mod_loaded "create" (sem o Create, nada carrega).
Formato: o do Create 6 (results com id/count/chance), mas com ingredientes no formato do 26.1
(string "mod:item" ou "#tag") e fluido como SizedFluidIngredient do NeoForge 26.1
({"ingredient": "minecraft:water", "amount": 250}). Rodar de novo apaga e regera a pasta.
"""
import json, os, shutil

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "mistech", "recipe", "compat", "create")
COND = [{"type": "neoforge:mod_loaded", "modid": "create"}]


def out(id_, count=1, chance=None):
    r = {"id": id_}
    if count != 1:
        r["count"] = count
    if chance is not None:
        r["chance"] = chance
    return r


def water(amount=250):
    return {"ingredient": "minecraft:water", "amount": amount}


def write(kind, name, body):
    path = os.path.join(ROOT, kind, name + ".json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = {"neoforge:conditions": COND}
    data.update(body)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def simple(kind, name, ingredients, results, time=None, heat=None):
    body = {"type": "create:" + kind, "ingredients": ingredients, "results": results}
    if time:
        body["processing_time"] = time
    if heat:
        body["heat_requirement"] = heat
    write(kind, name, body)


def deploy_step(transitional, held):
    return {"type": "create:deploying", "ingredients": [transitional, held], "results": [out(transitional)]}


def assembly(name, start, transitional, held_items, result):
    """Linha de montagem com Implantadores: um passo por item segurado, 1 volta, sempre dá o resultado."""
    write("sequenced_assembly", name, {
        "type": "create:sequenced_assembly",
        "ingredient": start,
        "transitional_item": {"id": transitional},
        "loops": 1,
        "sequence": [deploy_step(transitional, h) for h in held_items],
        "results": [out(result)],
    })


if os.path.isdir(ROOT):
    shutil.rmtree(ROOT)

# ---------- Implantador (a "mão"): enrolar charuto e baseado ----------
# A folha seca vai na esteira/depot; o Implantador segura o papel.
simple("deploying", "cigar", ["mistech:dried_tobacco_leaf", "minecraft:paper"], [out("mistech:cigar")])
simple("deploying", "joint", ["mistech:dried_hemp_leaf", "minecraft:paper"], [out("mistech:joint")])

# ---------- Linha de montagem (vários Implantadores em sequência) ----------
# Charuto de Arruda: papel -> folha de arruda -> folha de arruda.
assembly("rue_cigar", "minecraft:paper", "minecraft:paper", ["mistech:rue_leaf", "mistech:rue_leaf"], "mistech:rue_cigar")
# Incensos: graveto -> graveto -> o que vai na ponta (mesmos itens da bancada).
incenses = {
    "incense": ["#minecraft:small_flowers"],
    "magic_incense": ["mistech:impure_magic_shard"],
    "rue_incense": ["mistech:rue_leaf"],
    "fairy_incense": ["mistech:fairy_dust"],
    "lily_incense": ["minecraft:lily_of_the_valley", "minecraft:lily_of_the_valley"],
    "torchflower_incense": ["minecraft:torchflower", "minecraft:torchflower"],
    "wither_rose_incense": ["minecraft:wither_rose", "minecraft:wither_rose"],
    "eyeblossom_incense": ["minecraft:resin_clump", "minecraft:open_eyeblossom"],
}
for name, tops in incenses.items():
    assembly(name, "minecraft:stick", "minecraft:stick", ["minecraft:stick"] + tops, "mistech:" + name)

# ---------- Mó (milling) e Rodas Trituradoras (crushing) ----------
simple("milling", "fairy_dust", ["mistech:fey_remains"], [out("mistech:fairy_dust"), out("mistech:fairy_dust", chance=0.3)], 100)
simple("milling", "coffee_powder", ["mistech:roasted_coffee_beans"], [out("mistech:coffee_powder")], 50)
simple("milling", "incense_ash", ["mistech:ash_block"], [out("mistech:incense_ash", 4)], 50)
simple("milling", "coffee_seeds", ["mistech:coffee_cherry"], [out("mistech:coffee_seeds", 3)], 50)

ores = {
    "mithril": [("mithril_ore", "minecraft:cobblestone"), ("deepslate_mithril_ore", "minecraft:cobbled_deepslate")],
    "adamantine": [("deepslate_adamantine_ore", "minecraft:cobbled_deepslate")],
    "orichalcum": [("deepslate_orichalcum_ore", "minecraft:cobbled_deepslate")],
}
for metal, blocks in ores.items():
    for block, stone in blocks:
        # Igual aos minérios de diamante/esmeralda do Create: 1 bruto + 75% de outro, pepita de XP e um pouco de pedra.
        simple("crushing", block, ["mistech:" + block],
               [out("mistech:raw_" + metal), out("mistech:raw_" + metal, chance=0.75),
                out("create:experience_nugget", chance=0.75), out(stone, chance=0.125)], 350)

# ---------- Serra Mecânica (cutting) ----------
simple("cutting", "murky_log", ["mistech:murky_log"], [out("mistech:stripped_murky_log")], 50)
simple("cutting", "murky_wood", ["mistech:murky_wood"], [out("mistech:stripped_murky_wood")], 50)
simple("cutting", "stripped_murky_log", ["mistech:stripped_murky_log"], [out("mistech:murky_planks", 6)], 50)
simple("cutting", "stripped_murky_wood", ["mistech:stripped_murky_wood"], [out("mistech:murky_planks", 6)], 50)
# Fractal não tem versão descascada: vai direto para tábuas.
simple("cutting", "fractal_log", ["mistech:fractal_log"], [out("mistech:fractal_planks", 6)], 50)
simple("cutting", "fractal_wood", ["mistech:fractal_wood"], [out("mistech:fractal_planks", 6)], 50)

# ---------- Misturador (mixing) ----------
simple("mixing", "garapa", ["minecraft:sugar_cane", "minecraft:sugar_cane", "minecraft:sugar_cane", "minecraft:glass_bottle"], [out("mistech:garapa")])
simple("mixing", "impure_chalk", ["minecraft:clay_ball", "mistech:magic_residue", water()], [out("mistech:impure_chalk")])
simple("mixing", "ash_chalk", ["minecraft:clay_ball", "mistech:incense_ash", water()], [out("mistech:ash_chalk")])
simple("mixing", "pale_chalk", ["mistech:ash_chalk", "minecraft:pale_moss_block", "minecraft:bone_meal"], [out("mistech:pale_chalk")])
simple("mixing", "honey_cachaca", ["mistech:cachaca", "minecraft:honey_bottle"], [out("mistech:honey_cachaca")])

count = sum(len(fs) for _, _, fs in os.walk(ROOT))
print("receitas do Create geradas:", count)
