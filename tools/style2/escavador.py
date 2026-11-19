"""Tres versoes do Escavador solto (receita: deepslate em pedregulho, cobre, runa da Terra, picareta de ferro, redstone)."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
from triturador2 import f
OUT = os.path.join(os.path.dirname(__file__), 'out/escavador'); M = OUT + '/models'; os.makedirs(M, exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)

def casing(panel='#cdeep'):
    """Corpo: painel de deepslate rebaixado 0.5 entre colunas e aros de cobre embutidos."""
    els = [box((0.5, 0.5, 0.5), (15.5, 15.5, 15.5), f(panel, (0.5, 0.5, 0.5), (15.5, 15.5, 15.5)), name='painel')]
    for (x, z) in ((0, 0), (14, 0), (0, 14), (14, 14)):
        els.append(box((x, 0, z), (x + 2, 16, z + 2), f('#copper', (x, 0, z), (x + 2, 16, z + 2)), name='coluna'))
    for y0 in (0, 14):
        for fr, to in [((2, y0, 0), (14, y0 + 2, 2)), ((2, y0, 14), (14, y0 + 2, 16)), ((0, y0, 2), (2, y0 + 2, 14)), ((14, y0, 2), (16, y0 + 2, 14))]:
            els.append(box(fr, to, f('#copper', fr, to), name='aro'))
    # redstone atras (ligacao)
    els.append(box((6, 6, 15.5), (10, 10, 16.01), {'south': {'texture': '#redstone', 'uv': [6, 6, 10, 10]}}, name='redstone'))
    return els

def A():
    els = casing()
    for i, (a, b) in enumerate(((3, 13), (4, 12), (5, 11), (6, 10), (7, 9))):
        z0 = -i - 1
        els.append(box((a, a, z0), (b, b, z0 + 1), f('#anvil' if i % 2 == 0 and i < 4 else '#iron', (a, a, z0), (b, b, z0 + 1)), name='broca'))
    return els

def B():
    els = casing()
    # disco de ferro em degraus (redondo) na frente
    for (a, b) in ((3, 13), (2, 14)):
        for (c, d) in ((2, 14), (3, 13)) if a == 3 else ((3, 13),):
            pass
    els.append(box((3, 2, -1), (13, 14, 0), f('#anvil', (3, 2, -1), (13, 14, 0)), name='disco'))
    els.append(box((2, 3, -1), (14, 13, 0), f('#anvil', (2, 3, -1), (14, 13, 0)), name='disco'))
    # dentes de ferro claro
    for (x, y) in ((3.5, 7), (10.5, 7), (7, 3.5), (7, 10.5), (4.5, 4.5), (9.5, 9.5), (4.5, 9.5), (9.5, 4.5)):
        els.append(box((x, y, -2.5), (x + 2, y + 2, -1), f('#iron', (x, y, -2.5), (x + 2, y + 2, -1)), name='dente'))
    els.append(box((6.5, 6.5, -3), (9.5, 9.5, -1), f('#copper', (6.5, 6.5, -3), (9.5, 9.5, -1)), name='cubo'))
    return els

def C():
    els = casing()
    m = json.load(open(os.path.join(os.path.dirname(__file__), '../../src/main/resources/assets/mistech/models/block/tunneler.json')))
    els += [e for e in m['elements'] if e.get('name') == 'drill']
    return els

tex = {'cdeep': 'minecraft:block/cobbled_deepslate', 'copper': 'minecraft:block/cut_copper', 'iron': 'minecraft:block/iron_block',
       'anvil': 'minecraft:block/anvil', 'redstone': 'minecraft:block/redstone_block', 'drill': 'mistech:block/tunneler_drill',
       'particle': 'minecraft:block/cobbled_deepslate'}
for n, fn in (('A', A), ('B', B), ('C', C)):
    json.dump({'textures': tex, 'elements': fn()}, open(f'{M}/e{n}.json', 'w'), indent=1)
print('ok')
