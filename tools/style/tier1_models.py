"""Troca as texturas vanilla dos modelos das máquinas do Tier I pelos materiais t1_* e faces novas.
Uso: python tier1_models.py <pasta_modelos_saida>  (para aplicar de verdade: a pasta de modelos do src)"""
import json, os, re, sys
SRC = os.path.join(os.path.dirname(__file__), '../../src/main/resources/assets/mistech')
MACHINES = 'runic_crusher essence_extractor magic_compressor ash_generator condenser runic_harvester runic_planter miner tunneler runic_battery advanced_runic_battery rune_roller cane_still magic_loom blaze_generator'.split()
GLOBAL = {
    'minecraft:block/cut_copper': 'mistech:block/t1_copper', 'minecraft:block/copper_block': 'mistech:block/t1_copper',
    'minecraft:block/polished_deepslate': 'mistech:block/t1_base', 'minecraft:block/deepslate_tiles': 'mistech:block/t1_stone',
    'minecraft:block/amethyst_block': 'mistech:block/t1_crystal', 'minecraft:block/iron_block': 'mistech:block/t1_iron',
    'minecraft:block/composter_side': 'mistech:block/t1_crate', 'minecraft:block/composter_bottom': 'mistech:block/t1_crate_top',
    'minecraft:block/stripped_dark_oak_log': 'mistech:block/t1_wood', 'minecraft:block/stripped_spruce_log': 'mistech:block/t1_wood',
    'minecraft:block/dark_oak_planks': 'mistech:block/t1_wood',
    'minecraft:block/copper_bulb': 'mistech:block/t1_lamp', 'minecraft:block/copper_bulb_lit': 'mistech:block/t1_lamp_on',
    'minecraft:block/redstone_lamp': 'mistech:block/t1_lamp', 'minecraft:block/redstone_lamp_on': 'mistech:block/t1_lamp_on',
    'minecraft:block/cobblestone': 'mistech:block/t1_stone', 'minecraft:block/crying_obsidian': 'mistech:block/t1_void',
    'minecraft:block/blast_furnace_front': 'mistech:block/ash_generator_front',
    'minecraft:block/blast_furnace_front_on': 'mistech:block/ash_generator_front_on',
    'minecraft:block/blast_furnace_side': 'mistech:block/t1_generator_side', 'minecraft:block/blast_furnace_top': 'mistech:block/t1_top',
    'minecraft:block/polished_blackstone_bricks': 'mistech:block/t1_generator_side',
    'minecraft:block/gilded_blackstone': 'mistech:block/t1_iron', 'minecraft:block/gold_block': 'mistech:block/t1_copper',
}
PER_KEY = {  # (modelo-prefixo, chave) -> textura
    ('magic_compressor', 'body'): 'mistech:block/magic_compressor_body',
    ('magic_compressor', 'particle'): 'mistech:block/magic_compressor_body',
    ('magic_compressor', 'top'): 'mistech:block/magic_compressor_top',
    ('blaze_generator', 'top'): 'mistech:block/t1_top', ('blaze_generator', 'deep'): 'mistech:block/t1_base',
}


def models():
    out = []
    for b in MACHINES:
        t = open(f'{SRC}/blockstates/{b}.json').read()
        out += sorted(set(re.findall(r'"mistech:block/([a-z0-9_]+)"', t)))
    return out


def remap(name, m):
    tex = m.get('textures', {})
    for k, v in list(tex.items()):
        base = [p for (p, kk) in PER_KEY if name.startswith(p) and kk == k]
        if base: tex[k] = PER_KEY[(base[0], k)]
        elif v in GLOBAL: tex[k] = GLOBAL[v]
    return m


if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    n = 0
    for name in models():
        m = json.load(open(f'{SRC}/models/block/{name}.json'))
        m2 = remap(name, json.loads(json.dumps(m)))
        if m2 != m:
            json.dump(m2, open(os.path.join(out, name + '.json'), 'w'), indent=2); n += 1
    print('modelos alterados:', n)
