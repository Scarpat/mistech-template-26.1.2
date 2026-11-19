"""Condensador (receita: fornalha + 4 runas de Gelo). Tres versoes, so texturas vanilla, sem colar face inteira de bloco."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
from triturador2 import f
OUT = os.path.join(os.path.dirname(__file__), 'out/condensador'); M = OUT + '/models'; os.makedirs(M, exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)

def B(fr, to, tex, name, **over):
    d = f(tex, fr, to); d.update(over); return box(fr, to, d, name=name)

def A(lit):
    els = [B((1, 0, 1), (15, 13, 15), '#stone', 'corpo', up={'texture': '#ftop'})]
    for (x, z) in ((0, 0), (13, 0), (0, 13), (13, 13)):
        els.append(B((x, 0, z), (x + 3, 14, z + 3), '#packed', 'canto_gelo'))
    els.append(B((0.5, 13, 0.5), (15.5, 14.5, 15.5), '#snow', 'neve'))
    els.append(B((3, 14.5, 4), (8, 15.5, 9), '#snow', 'neve_monte'))
    # boca: moldura de pedra e gelo azul dentro
    els.append(B((4, 2, 0.5), (12, 8, 1), '#smooth', 'moldura'))
    els.append(box((5, 3, 0.4), (11, 7, 0.5), {'north': {'texture': '#blue_ice' if lit else '#packed', 'uv': [5, 9, 11, 13]}}, name='boca_gelo'))
    for (x, h) in ((4.5, 3), (7.5, 2), (10.5, 2.5)):
        els.append(B((x, 13 - h, 0.5), (x + 1, 13, 1), '#ice', 'pingente'))
    return els

def Bv(lit):
    els = [B((0, 0, 0), (16, 2, 16), '#stone', 'base')]
    for (x, z) in ((0, 0), (13, 0), (0, 13), (13, 13)):
        els.append(B((x, 2, z), (x + 3, 14, z + 3), '#blue_ice', 'coluna'))
    for fr, to in [((3, 12, 0), (13, 14, 3)), ((3, 12, 13), (13, 14, 16)), ((0, 12, 3), (3, 14, 13)), ((13, 12, 3), (16, 14, 13))]:
        els.append(B(fr, to, '#blue_ice', 'aro'))
    for fr, to in [((3, 2, 13.5), (13, 12, 14.5)), ((1.5, 2, 3), (2.5, 12, 13)), ((13.5, 2, 3), (14.5, 12, 13))]:
        els.append(B(fr, to, '#packed', 'painel'))
    els.append(B((1, 14, 1), (15, 15, 15), '#snow', 'neve'))
    # janela da frente: so moldura, deixa ver o cristal
    els.append(B((3, 2, 1.5), (13, 4, 2.5), '#packed', 'painel'))
    els.append(B((5, 5, 5), (11, 11, 11), '#blue_ice' if lit else '#packed', 'cristal'))
    els.append(B((7, 4, 7), (9, 5, 9), '#ice', 'pe_cristal'))
    return els

def C(lit):
    els = [B((0, 0, 0), (16, 2, 16), '#snow', 'base_neve')]
    for (a, y0, y1) in ((1, 2, 7), (2, 7, 10), (3.5, 10, 12), (5.5, 12, 13.5)):
        els.append(B((a, y0, a), (16 - a, y1, 16 - a), '#snow_block' if a < 3 else '#packed', 'cupula'))
    # entrada em arco na frente
    els.append(B((5, 2, -1), (11, 8, 1), '#snow_block', 'tunel'))
    els.append(box((6.5, 2, -1.01), (9.5, 6, -1), {'north': {'texture': '#dark'}}, name='porta'))
    els.append(B((6, 6, -1.2), (10, 7, 1), '#packed', 'arco'))
    # chamine de pedra
    els.append(B((10, 9, 9), (13, 16, 12), '#stone', 'chamine', up={'texture': '#blue_ice' if lit else '#dark'}))
    return els

tex = {'stone': 'minecraft:block/furnace_side', 'ftop': 'minecraft:block/furnace_top', 'smooth': 'minecraft:block/smooth_stone',
       'packed': 'minecraft:block/packed_ice', 'blue_ice': 'minecraft:block/blue_ice', 'ice': 'minecraft:block/ice', 'snow': 'minecraft:block/snow',
       'snow_block': 'minecraft:block/snow', 'dark': 'minecraft:block/black_concrete', 'particle': 'minecraft:block/packed_ice'}
for n, fn in (('A', A), ('B', Bv), ('C', C)):
    for lit in (False, True):
        json.dump({'textures': tex, 'elements': fn(lit)}, open(f'{M}/cd{n}{"_on" if lit else ""}.json', 'w'), indent=1)
print('ok')
