"""Escavador, rodada 3: tres blocos inteiros novos (receita: deepslate em pedregulho, cobre, runa da Terra, picareta de ferro, redstone)."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
from triturador2 import f
OUT = os.path.join(os.path.dirname(__file__), 'out/escavador3'); M = OUT + '/models'; os.makedirs(M, exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)

def B(fr, to, tex, name, **over):
    d = f(tex, fr, to)
    d.update(over)
    return box(fr, to, d, name=name)

def toupeira(lit):
    els = []
    # esteiras
    for x0 in (0, 13):
        els.append(B((x0, 0, 1), (x0 + 3, 4, 15), '#anvil', 'esteira'))
        for z0 in (2, 6.5, 11):
            side_x = x0 - 0.01 if x0 == 0 else x0 + 3
            els.append(box((side_x if x0 == 0 else x0 + 3, 0.5, z0), (x0 if x0 == 0 else x0 + 3.01, 3.5, z0 + 3),
                           {('west' if x0 == 0 else 'east'): {'texture': '#copper', 'uv': [0, 0, 3, 3]}}, name='roda'))
    # corpo abaulado em degraus
    els.append(B((2, 3, 2), (14, 10, 15), '#cdeep', 'corpo'))
    els.append(B((3, 10, 3), (13, 12, 14), '#cdeep', 'dorso'))
    els.append(B((5, 12, 5), (11, 13, 13), '#cdeep', 'dorso'))
    els.append(B((7, 12.5, 4), (9, 13.5, 14), '#copper', 'crista'))
    # focinho de cobre + broca
    els.append(B((4, 4, 1), (12, 10, 2), '#copper', 'focinho'))
    for i, (a, b, t) in enumerate(((4.5, 9.5, '#anvil'), (5, 9, '#iron'), (5.5, 8.5, '#anvil'), (6, 8, '#iron'), (6.5, 7.5, '#iron'))):
        z1 = 1 - i * 1.2
        els.append(B((8 - (b - a) / 2 - 0, 7 - (b - a) / 2, z1 - 1.2), (8 + (b - a) / 2, 7 + (b - a) / 2, z1), t, 'broca'))
    # escapamento e luz de redstone
    els.append(B((10, 10, 12), (12, 15, 14), '#copper', 'escapamento', up={'texture': '#dark'}))
    els.append(box((4, 12, 10), (6, 13.5, 12), {d: {'texture': '#lamp_on' if lit else '#lamp', 'uv': [6, 6, 8, 8]} for d in ('north', 'south', 'east', 'west', 'up')}, name='luz'))
    return els

def golem(lit):
    els = [B((1, 0, 1), (15, 13.5, 15), '#bricks', 'cabeca', up={'texture': '#pdeep'})]
    for fr, to in [((0.5, 12, 0.5), (15.5, 14, 2)), ((0.5, 12, 14), (15.5, 14, 15.5)), ((0.5, 12, 2), (2, 14, 14)), ((14, 12, 2), (15.5, 14, 14))]:
        els.append(B(fr, to, '#copper', 'coroa'))
    for (x, z) in ((0.5, 0.5), (13.5, 0.5), (0.5, 13.5), (13.5, 13.5), (7, 0.5)):
        els.append(B((x, 14, z), (x + 2, 16, z + 2), '#copper', 'ponta_coroa'))
    # testa com a runa da Terra (placa do triturador)
    els.append(box((5, 8.5, 0.5), (11, 11.5, 1), {'north': {'texture': '#plate_on' if lit else '#plate', 'uv': [5, 7, 11, 10]},
                                                   'up': {'texture': '#iron'}, 'down': {'texture': '#iron'}, 'east': {'texture': '#iron'}, 'west': {'texture': '#iron'}}, name='runa'))
    # sobrancelha
    els.append(B((2, 7, 0), (14, 8, 1), '#pdeep', 'sobrancelha'))
    # olhos de lampada de redstone
    for x0 in (3, 10):
        els.append(box((x0, 5, 0.5), (x0 + 3, 7, 1), {'north': {'texture': '#lamp_on' if lit else '#lamp', 'uv': [6, 7, 9, 9]}}, name='olho'))
    # boca aberta com dentes de picareta (ferro)
    els.append(box((3, 1, 0.9), (13, 4.5, 1), {'north': {'texture': '#dark'}}, name='boca'))
    for x0 in (3.5, 6.5, 9.5):
        els.append(B((x0, 3.5, 0), (x0 + 1.5, 4.5, 1), '#iron', 'dente'))
        els.append(B((x0 + 1.5, 1, 0), (x0 + 3, 2, 1), '#iron', 'dente'))
    # orelhas/parafusos
    for x0 in (0, 15):
        els.append(B((x0, 5, 6), (x0 + 1, 8, 10), '#copper', 'parafuso'))
    return els

def pilao(lit):
    els = [B((0, 0, 0), (16, 4, 16), '#cdeep', 'base'), B((1, 4, 1), (15, 5, 15), '#copper', 'friso')]
    for x0 in (1, 12):
        els.append(B((x0, 5, 11), (x0 + 3, 15, 14), '#copper', 'poste'))
    els.append(B((0.5, 12, 11.5), (15.5, 14, 13.5), '#anvil', 'eixo'))
    # braco de madeira do eixo ate a frente
    els.append(B((7, 11, 2), (9, 13, 12), '#stick', 'braco'))
    # cabeca de picareta grande na ponta, apontando para frente/baixo
    for fr, to in [((3, 10, 0), (13, 14, 2)), ((1, 7, 0.5), (3, 11, 1.5)), ((13, 7, 0.5), (15, 11, 1.5)), ((0.5, 5, 0.75), (1.5, 7, 1.25)), ((14.5, 5, 0.75), (15.5, 7, 1.25))]:
        els.append(B(fr, to, '#iron', 'picareta'))
    # redstone na base
    els.append(box((6, 4, 6), (10, 5.01, 10), {'up': {'texture': '#redstone', 'uv': [6, 6, 10, 10]}}, name='redstone'))
    return els

tex = {'cdeep': 'minecraft:block/cobbled_deepslate', 'pdeep': 'minecraft:block/polished_deepslate', 'bricks': 'minecraft:block/deepslate_bricks',
       'copper': 'minecraft:block/cut_copper', 'iron': 'minecraft:block/iron_block', 'anvil': 'minecraft:block/anvil', 'dark': 'minecraft:block/black_concrete',
       'lamp': 'minecraft:block/redstone_lamp', 'lamp_on': 'minecraft:block/redstone_lamp_on', 'redstone': 'minecraft:block/redstone_block',
       'stick': 'minecraft:block/stripped_spruce_log', 'plate': 'mistech:block/runic_crusher_rune_plate', 'plate_on': 'mistech:block/runic_crusher_rune_plate_on',
       'particle': 'minecraft:block/cobbled_deepslate'}
for n, fn in (('1', toupeira), ('2', golem), ('3', pilao)):
    for lit in (False, True):
        json.dump({'textures': tex, 'elements': fn(lit)}, open(f'{M}/e3_{n}{"_on" if lit else ""}.json', 'w'), indent=1)
print('ok')
