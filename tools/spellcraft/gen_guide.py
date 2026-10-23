#!/usr/bin/env python3
"""Páginas do Grimório (guide/book.json, categoria "spells") para Cajados, Carregador, Tomos de elemento/Varinhas/engastes,
tipos de lançamento/ressonância/invocações e estruturas únicas. Idempotente: troca as entradas pelo id.
Os textos ficam em gen_data.py (LANG_PT/LANG_EN)."""
import json, os
HERE = os.path.dirname(os.path.abspath(__file__))
BOOK = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'mistech', 'guide', 'book.json')

E = [
    {"id": "spell_casting", "icon": "mistech:rune_altar", "name": "guide.mistech.entry.spell_casting", "deps": ["apprentice_tome"],
     "task": {"type": "item", "items": ["mistech:apprentice_tome"]}, "x": 4.8, "y": 1.5,
     "pages": [{"type": "text", "text": "guide.mistech.spell_casting.1"}, {"type": "text", "text": "guide.mistech.spell_casting.2"},
               {"type": "text", "text": "guide.mistech.spell_casting.3"}]},
    {"id": "staves", "icon": "mistech:wooden_staff", "name": "guide.mistech.entry.staves", "deps": ["apprentice_tome"],
     "task": {"type": "item", "items": ["mistech:wooden_staff"]}, "x": 4.8, "y": 0.5,
     "pages": [{"type": "crafting", "grid": ["", "minecraft:amethyst_shard", "", "minecraft:stick", "", "", "minecraft:stick", "", ""],
                "result": "mistech:wooden_staff", "text": "guide.mistech.staves.recipe"},
               {"type": "text", "text": "guide.mistech.staves.1"}, {"type": "text", "text": "guide.mistech.staves.2"}]},
    {"id": "arcane_charger", "icon": "mistech:arcane_charger", "name": "guide.mistech.entry.arcane_charger", "deps": ["staves"],
     "task": {"type": "item", "items": ["mistech:arcane_charger"]}, "x": 6.4, "y": 0.5,
     "pages": [{"type": "crafting", "grid": ["minecraft:amethyst_shard", "mistech:arcane_rune", "minecraft:amethyst_shard",
                                              "minecraft:iron_ingot", "minecraft:copper_ingot", "minecraft:iron_ingot",
                                              "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot"],
                "result": "mistech:arcane_charger", "text": "guide.mistech.arcane_charger.recipe"},
               {"type": "text", "text": "guide.mistech.arcane_charger.1"}]},
    {"id": "element_tomes", "icon": "mistech:ice_tome", "name": "guide.mistech.entry.element_tomes", "deps": ["apprentice_tome"],
     "task": {"type": "item", "items": ["mistech:adept_tome"]}, "x": 4.8, "y": -0.5,
     "pages": [{"type": "crafting", "grid": ["mistech:adept_tome", "mistech:ice_rune", "mistech:ice_rune", "mistech:ice_rune", "", "", "", "", ""],
                "result": "mistech:ice_tome", "text": "guide.mistech.element_tomes.recipe", "shapeless": True},
               {"type": "text", "text": "guide.mistech.element_tomes.1"}, {"type": "text", "text": "guide.mistech.element_tomes.2"}]},
    {"id": "arcane_ruins", "icon": "mistech:archmage_staff", "name": "guide.mistech.entry.arcane_ruins", "deps": ["spell_casting"],
     "task": {"type": "item", "items": ["mistech:spell_scroll"]}, "x": 6.4, "y": 1.5,
     "pages": [{"type": "text", "text": "guide.mistech.arcane_ruins.1"}, {"type": "text", "text": "guide.mistech.arcane_ruins.2"},
               {"type": "text", "text": "guide.mistech.arcane_ruins.3"}]},
]

book = json.load(open(BOOK, encoding='utf-8'))
cat = next(c for c in book['categories'] if c['id'] == 'spells')
ids = {e['id'] for e in E}
cat['entries'] = [e for e in cat['entries'] if e['id'] not in ids] + E
json.dump(book, open(BOOK, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
print('ok', len(cat['entries']), 'entradas em spells')
