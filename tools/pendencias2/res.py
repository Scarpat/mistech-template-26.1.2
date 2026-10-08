"""Ajudantes para gerar recursos (json, tags, lang) do Mistech."""
import json, os
R = 'src/main/resources/'
A = R + 'assets/mistech/'
D = R + 'data/'
def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(json.dumps(obj, indent=2, ensure_ascii=False) + '\n')
def tag(path, *values):
    p = D + path + '.json'
    d = json.load(open(p, encoding='utf-8')) if os.path.exists(p) else {'replace': False, 'values': []}
    for v in values:
        if v not in d['values']:
            d['values'].append(v)
    wj(p, d)
def lang(en, pt):
    for code, entries in (('en_us', en), ('pt_br', pt)):
        p = A + 'lang/%s.json' % code
        d = json.load(open(p, encoding='utf-8'))
        d.update(entries)
        wj(p, d)
def simple_block(name, texture=None):
    wj(A + 'blockstates/%s.json' % name, {'variants': {'': {'model': 'mistech:block/' + name}}})
    wj(A + 'models/block/%s.json' % name, {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'mistech:block/' + (texture or name)}})
    wj(A + 'items/%s.json' % name, {'model': {'type': 'minecraft:model', 'model': 'mistech:block/' + name}})
    self_loot(name)
def self_loot(name):
    wj(D + 'mistech/loot_table/blocks/%s.json' % name, {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0,
        'entries': [{'type': 'minecraft:item', 'name': 'mistech:' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': 'mistech:blocks/' + name})
def flat_item(name, parent='minecraft:item/generated', texture=None):
    wj(A + 'models/item/%s.json' % name, {'parent': parent, 'textures': {'layer0': 'mistech:item/' + (texture or name)}})
    wj(A + 'items/%s.json' % name, {'model': {'type': 'minecraft:model', 'model': 'mistech:item/' + name}})
def recipe(name, obj):
    wj(D + 'mistech/recipe/%s.json' % name, obj)
