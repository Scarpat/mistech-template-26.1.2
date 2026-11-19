"""Tres versoes do Gerador de Cinzas (receita: cobre, fornalha, bloco de cinzas). So texturas vanilla/mod."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
from triturador2 import f
OUT = os.path.join(os.path.dirname(__file__), 'out/gerador_v'); M = OUT + '/models'; os.makedirs(M, exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)

def faces(tex, fr, to, **over):
    d = f(tex, fr, to)
    for k, v in over.items(): d[k] = v
    return d

def bulb(fr, to, lit):
    return box(fr, to, {d: {'texture': '#bulb_on' if lit else '#bulb', 'uv': [6, 6, 10, 10]} for d in ('north', 'south', 'east', 'west', 'up')}, name='lampada')

def fire_planes(x0, x1, y0, y1, zc=8):
    out = []
    for a in (45, -45):
        out.append({'from': [x0, y0, zc], 'to': [x1, y1, zc], 'shade': False, 'rotation': {'origin': [8, y0, 8], 'axis': 'y', 'angle': a},
                    'faces': {d: {'texture': '#fire', 'uv': [x0, 16 - (y1 - y0), x1, 16]} for d in ('north', 'south')}})
    return out

def A(lit):
    """Fogao de ferro: pes, fornalha vanilla inteira, gaveta de cinzas, tampa de cobre, chamine."""
    els = [box((x, 0, z), (x + 3, 2, z + 3), f('#iron', (x, 0, z), (x + 3, 2, z + 3)), name='pe') for (x, z) in ((1, 1), (12, 1), (1, 12), (12, 12))]
    els.append(box((1, 2, 1), (15, 4, 15), faces('#ash', (1, 2, 1), (15, 4, 15)), name='gaveta_cinzas'))
    els.append(box((1.5, 4, 1.5), (14.5, 11, 14.5), faces('#fside', (1.5, 4, 1.5), (14.5, 11, 14.5),
               north={'texture': '#front', 'uv': [1.5, 7, 14.5, 14]}, up={'texture': '#ftop'}), name='fornalha'))
    els.append(box((1, 10, 1), (15, 11, 15), faces('#copper', (1, 10, 1), (15, 11, 15)), name='cinta'))
    els.append(box((0.5, 11, 0.5), (15.5, 12.5, 15.5), faces('#copper', (0.5, 11, 0.5), (15.5, 12.5, 15.5)), name='tampa'))
    els.append(box((9, 12.5, 9), (13, 16, 13), faces('#copper', (9, 12.5, 9), (13, 16, 13), up={'texture': '#dark'}), name='chamine'))
    els.append(bulb((3, 12.5, 3), (6.5, 15, 6.5), lit))
    return els

def B(lit):
    """Caldeira vertical: fornalha baixa embaixo, tanque de cobre alto e redondo em cima."""
    els = [box((1, 0, 1), (15, 6, 15), faces('#fside', (1, 0, 1), (15, 6, 15), north={'texture': '#front', 'uv': [1, 8, 15, 14]},
               up={'texture': '#ftop'}), name='fornalha')]
    els.append(box((0.5, 5, 0.5), (15.5, 6, 15.5), faces('#ash', (0.5, 5, 0.5), (15.5, 6, 15.5)), name='friso_cinzas'))
    for (a, b) in ((3, 13), (2.5, 13.5), (2, 14)):
        pass
    # tanque redondo em degraus (cruz de 3 caixas)
    els.append(box((3, 6, 2), (13, 15, 14), faces('#copper', (3, 6, 2), (13, 15, 14)), name='tanque'))
    els.append(box((2, 6, 3), (14, 15, 13), faces('#copper', (2, 6, 3), (14, 15, 13)), name='tanque'))
    els.append(box((2.5, 6, 2.5), (13.5, 15, 13.5), faces('#copper', (2.5, 6, 2.5), (13.5, 15, 13.5)), name='tanque'))
    for y0 in (8, 12.5):
        els.append(box((1.9, y0, 1.9), (14.1, y0 + 1, 14.1), faces('#iron', (1.9, y0, 1.9), (14.1, y0 + 1, 14.1)), name='cinta'))
    els.append(box((4, 15, 4), (12, 16, 12), faces('#copper', (4, 15, 4), (12, 16, 12)), name='domo'))
    els.append(box((6.5, 16, 6.5), (9.5, 18, 9.5), faces('#copper', (6.5, 16, 6.5), (9.5, 18, 9.5), up={'texture': '#dark'}), name='chamine'))
    els.append(bulb((6, 9.5, 1), (10, 12, 2), lit))   # manometro na frente do tanque
    return els

def C(lit):
    """Braseiro: pes de ferro, fornalha baixa, bacia de cobre aberta com cinzas e fogo por cima."""
    els = [box((x, 0, z), (x + 2, 4, z + 2), f('#iron', (x, 0, z), (x + 2, 4, z + 2)), name='pe') for (x, z) in ((2, 2), (12, 2), (2, 12), (12, 12))]
    els.append(box((3, 2, 3), (13, 5, 13), faces('#fside', (3, 2, 3), (13, 5, 13), north={'texture': '#front', 'uv': [3, 10, 13, 13]}), name='fornalha'))
    for fr, to in [((1, 5, 1), (15, 10, 2.5)), ((1, 5, 13.5), (15, 10, 15)), ((1, 5, 2.5), (2.5, 10, 13.5)), ((13.5, 5, 2.5), (15, 10, 13.5))]:
        els.append(box(fr, to, faces('#copper', fr, to), name='bacia'))
    els.append(box((2.5, 5, 2.5), (13.5, 8.5, 13.5), {'up': {'texture': '#ash'}}, name='cinzas'))
    els.append(box((0.5, 9.5, 0.5), (15.5, 10.5, 15.5), {**faces('#copper', (0.5, 9.5, 0.5), (15.5, 10.5, 15.5)), 'up': {'texture': '#copper'}}, name='borda') if False else box((0.5, 9.5, 0.5), (15.5, 10.5, 2), faces('#copper', (0.5, 9.5, 0.5), (15.5, 10.5, 2)), name='borda'))
    if lit:
        els += fire_planes(4.5, 11.5, 8.5, 15)
    els.append(bulb((12, 10, 12), (15, 13, 15), lit))
    return els

tex = {'ash': 'mistech:block/ash_block', 'fside': 'minecraft:block/furnace_side', 'ftop': 'minecraft:block/furnace_top',
       'copper': 'minecraft:block/cut_copper', 'iron': 'minecraft:block/anvil', 'dark': 'minecraft:block/black_concrete',
       'bulb': 'minecraft:block/copper_bulb', 'bulb_on': 'minecraft:block/copper_bulb_lit', 'fire': 'minecraft:block/campfire_fire',
       'particle': 'minecraft:block/cut_copper'}
for name, fn in (('A', A), ('B', B), ('C', C)):
    for lit in (False, True):
        t = dict(tex, front='minecraft:block/furnace_front' + ('_on' if lit else ''))
        json.dump({'textures': t, 'elements': fn(lit)}, open(f'{M}/gv{name}{"_on" if lit else ""}.json', 'w'), indent=1)
print('ok')
