#!/usr/bin/env python3
"""Gera structure/structure_set JSON e as tabelas de loot das Torre do Arquimago e Santuários de Elemento.
Uso: python3 tools/spellcraft/gen_structures.py"""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(HERE, 'gen_data.py'), encoding='utf-8').read().split('def merge_lang')[0]
ns = {'__file__': os.path.join(HERE, 'gen_data.py')}
exec(src, ns)
SPELLS, COLORS = ns['SPELLS'], ns['COLORS']
DATA = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'data', 'mistech')


def write(rel, obj):
    path = os.path.join(DATA, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, 'w', encoding='utf-8'), indent=2)


def scroll(spell_id, weight=1):
    color = COLORS.get(SPELLS[spell_id][0], 0xFFFFFF)
    return {"type": "minecraft:item", "name": "mistech:spell_scroll", "weight": weight, "functions": [
        {"function": "minecraft:set_components", "components": {
            "mistech:spell_id": spell_id, "minecraft:custom_model_data": {"colors": [color]}}}]}


def item(name, lo, hi, weight=1):
    return {"type": "minecraft:item", "name": name, "weight": weight, "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}


# --- Torre do Arquimago
high = [sid for sid, v in SPELLS.items() if v[1] >= 4]
write('loot_table/chests/archmage_tower.json', {"type": "minecraft:chest", "pools": [
    {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "mistech:archmage_staff"}]},
    {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "mistech:aether_crown", "weight": 1}, {"type": "minecraft:empty", "weight": 3}]},
    {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "bonus_rolls": 0, "entries": [scroll(s) for s in high]},
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "bonus_rolls": 0, "entries": [
        item("mistech:arcane_rune", 1, 3, 4), item("minecraft:lapis_lazuli", 6, 16, 6), item("mistech:impure_magic_shard", 2, 6, 4),
        item("minecraft:amethyst_shard", 4, 12, 4), item("minecraft:diamond", 1, 3, 2)]}]})
write('worldgen/structure/archmage_tower.json', {"type": "mistech:arcane_ruin", "kind": "tower", "biomes": "#minecraft:is_overworld",
      "spawn_overrides": {}, "step": "surface_structures", "terrain_adaptation": "none"})
write('worldgen/structure_set/archmage_towers.json', {"placement": {"type": "minecraft:random_spread", "salt": 71830211, "spacing": 48, "separation": 20},
      "structures": [{"structure": "mistech:archmage_tower", "weight": 1}]})

# --- Santuário de Elemento (um por elemento, sorteado na geração)
for el in ['fire', 'ice', 'water', 'earth', 'air', 'energy', 'light', 'darkness', 'time', 'space']:
    spells = [sid for sid, v in SPELLS.items() if v[0] == el and v[1] >= 3]
    entries = [scroll(s, 4 if SPELLS[s][1] >= 4 else 2) for s in spells]
    pools = [
        {"rolls": 1, "bonus_rolls": 0, "entries": entries or [item("mistech:arcane_rune", 1, 2)]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "bonus_rolls": 0, "entries": [
            item(f"mistech:{el}_rune", 2, 5, 8), item("mistech:arcane_rune", 1, 2, 3), item("mistech:impure_magic_shard", 1, 4, 4),
            item("minecraft:lapis_lazuli", 4, 10, 5)]}]
    if el != 'fire':
        pools.append({"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"mistech:{el}_tome", "weight": 1},
                                                                  {"type": "minecraft:empty", "weight": 9}]})
    else:
        pools.append({"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "mistech:flame_tome", "weight": 1},
                                                                  {"type": "minecraft:empty", "weight": 9}]})
    write(f'loot_table/chests/shrine_{el}.json', {"type": "minecraft:chest", "pools": pools})
write('worldgen/structure/element_shrine.json', {"type": "mistech:arcane_ruin", "kind": "shrine", "biomes": "#minecraft:is_overworld",
      "spawn_overrides": {}, "step": "surface_structures", "terrain_adaptation": "none"})
write('worldgen/structure_set/element_shrines.json', {"placement": {"type": "minecraft:random_spread", "salt": 71830977, "spacing": 32, "separation": 12},
      "structures": [{"structure": "mistech:element_shrine", "weight": 1}]})

# --- Biblioteca Proibida
mid = [sid for sid, v in SPELLS.items() if v[1] >= 3]
write('loot_table/chests/forbidden_library.json', {"type": "minecraft:chest", "pools": [
    {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "mistech:forbidden_tome"}]},
    {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "mistech:void_wand", "weight": 1}, {"type": "minecraft:empty", "weight": 2}]},
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "bonus_rolls": 0, "entries": [scroll(s, 3 if SPELLS[s][1] < 5 else 2) for s in mid]},
    {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "bonus_rolls": 0, "entries": [
        item("mistech:arcane_rune", 1, 3, 4), item("mistech:impure_magic_shard", 3, 8, 5), item("minecraft:lapis_lazuli", 8, 20, 5)]}]})
write('loot_table/chests/forbidden_library_minor.json', {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "bonus_rolls": 0, "entries": [scroll(s) for s, v in SPELLS.items() if 2 <= v[1] <= 4]},
    {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "bonus_rolls": 0, "entries": [
        item("minecraft:paper", 4, 12, 6), item("minecraft:book", 2, 5, 5), item("minecraft:lapis_lazuli", 4, 12, 5), item("mistech:impure_magic_shard", 1, 3, 3)]}]})
write('worldgen/structure/forbidden_library.json', {"type": "mistech:arcane_ruin", "kind": "library", "biomes": "#minecraft:is_overworld",
      "spawn_overrides": {}, "step": "surface_structures", "terrain_adaptation": "none"})
write('worldgen/structure_set/forbidden_libraries.json', {"placement": {"type": "minecraft:random_spread", "salt": 71831405, "spacing": 40, "separation": 16},
      "structures": [{"structure": "mistech:forbidden_library", "weight": 1}]})

# --- Árvore-Mãe
life = [sid for sid, v in SPELLS.items() if v[0] == 'life']
write('loot_table/chests/mother_tree.json', {"type": "minecraft:chest", "pools": [
    {"rolls": 1, "bonus_rolls": 0, "entries": [scroll('vine')]},
    {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "bonus_rolls": 0, "entries": [scroll(s) for s in life]},
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 4}, "bonus_rolls": 0, "entries": [
        item("mistech:life_rune", 3, 6, 8), item("minecraft:bone_meal", 8, 20, 6), item("minecraft:golden_apple", 1, 2, 2),
        item("minecraft:glow_berries", 6, 14, 4), item("mistech:impure_magic_shard", 1, 4, 3)]},
    {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "mistech:life_tome", "weight": 1}, {"type": "minecraft:empty", "weight": 3}]}]})
write('worldgen/structure/mother_tree.json', {"type": "mistech:arcane_ruin", "kind": "mothertree", "biomes": "#minecraft:is_forest",
      "spawn_overrides": {}, "step": "surface_structures", "terrain_adaptation": "none"})
write('worldgen/structure_set/mother_trees.json', {"placement": {"type": "minecraft:random_spread", "salt": 71831733, "spacing": 36, "separation": 14},
      "structures": [{"structure": "mistech:mother_tree", "weight": 1}]})
write('worldgen/structure/rune_circle.json', {"type": "mistech:arcane_ruin", "kind": "circle", "biomes": "#minecraft:is_overworld",
      "spawn_overrides": {}, "step": "surface_structures", "terrain_adaptation": "none"})
write('worldgen/structure_set/rune_circles.json', {"placement": {"type": "minecraft:random_spread", "salt": 71832099, "spacing": 30, "separation": 10},
      "structures": [{"structure": "mistech:rune_circle", "weight": 1}]})
print('ok')
