"""Escavador, rodada 2: corpo mantido (deepslate + cobre); frentes novas com personalidade de terra. Placa da runa da Terra nas laterais."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
from triturador2 import f
from escavador import casing
OUT = os.path.join(os.path.dirname(__file__), 'out/escavador2'); M = OUT + '/models'; os.makedirs(M, exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)

def plates(lit):
    t = '#plate_on' if lit else '#plate'
    return [box((-0.5, 4.5, 4), (0, 11.5, 12), {'west': {'texture': t, 'uv': [4, 7, 12, 14]}, 'up': {'texture': '#iron'}, 'down': {'texture': '#iron'},
                                                 'north': {'texture': '#iron'}, 'south': {'texture': '#iron'}}, name='placa_runa'),
            box((16, 4.5, 4), (16.5, 11.5, 12), {'east': {'texture': t, 'uv': [4, 7, 12, 14]}, 'up': {'texture': '#iron'}, 'down': {'texture': '#iron'},
                                                  'north': {'texture': '#iron'}, 'south': {'texture': '#iron'}}, name='placa_runa')]

def cross(x0, x1, y0, y1, zc, tex, uv, rot_axis='y', origin=None, angle=45):
    out = []
    for a in (angle, -angle):
        out.append({'from': [x0, y0, zc], 'to': [x1, y1, zc], 'shade': False,
                    'rotation': {'origin': origin or [(x0 + x1) / 2, (y0 + y1) / 2, zc], 'axis': rot_axis, 'angle': a},
                    'faces': {d: {'texture': tex, 'uv': uv} for d in ('north', 'south')}})
    return out

def P(lit):
    """Picareta: cabo de madeira saindo da frente e cabeca de ferro curva, como a picareta vanilla em 3D."""
    els = casing() + plates(lit)
    els.append(box((7, 7, -6), (9, 9, 0), f('#stick', (7, 7, -6), (9, 9, 0)), name='cabo'))
    els.append(box((6, 6, -0.5), (10, 10, 0), f('#iron', (6, 6, -0.5), (10, 10, 0)), name='bucha'))
    # cabeca: barra central + bracos curvando para tras, terminando em ponta
    head = [((5, 6.5, -7.5), (11, 9.5, -5.5)), ((2.5, 7, -6.5), (5, 9, -4.5)), ((11, 7, -6.5), (13.5, 9, -4.5)),
            ((1, 7.25, -4.5), (3, 8.75, -2.5)), ((13, 7.25, -4.5), (15, 8.75, -2.5)), ((0.5, 7.5, -2.5), (2, 8.5, -1)), ((14, 7.5, -2.5), (15.5, 8.5, -1))]
    for fr, to in head:
        els.append(box(fr, to, f('#iron', fr, to), name='cabeca'))
    return els

def D(lit):
    """Presas de pedra: anel de terra enraizada e musgo, tres pontas de dripstone em degraus apontando para frente."""
    els = casing() + plates(lit)
    els.append(box((2, 2, -1), (14, 14, 0), {**f('#rooted', (2, 2, -1), (14, 14, 0)), 'north': {'texture': '#rooted', 'uv': [2, 2, 14, 14]}}, name='terra'))
    for fr, to in [((2, 12, -1.5), (14, 14, -1)), ((2, 2, -1.5), (4, 12, -1)), ((10, 2, -1.5), (14, 5, -1))]:
        els.append(box(fr, to, f('#moss', fr, to), name='musgo'))
    for (cx, cy, s) in ((8, 8, 1.0), (4.5, 5, 0.7), (11.5, 10.5, 0.7)):
        z = -1
        for k, w in enumerate((4, 3, 2, 1)):
            w = w * s if w > 1 else 1
            L = 2.5 * s
            fr, to = (cx - w / 2, cy - w / 2, z - L), (cx + w / 2, cy + w / 2, z)
            els.append(box(fr, to, f('#dripstone', fr, to), name='presa'))
            z -= L
    return els

def E(lit):
    """Cacamba de escavadeira: pa de ferro com 4 dentes, presa por dobradicas de cobre, com terra dentro."""
    els = casing() + plates(lit)
    for x0 in (3, 11):
        els.append(box((x0, 10, -2), (x0 + 2, 13, 0), f('#copper', (x0, 10, -2), (x0 + 2, 13, 0)), name='dobradica'))
    els.append(box((2.5, 11, -3), (13.5, 12, -2), f('#copper', (2.5, 11, -3), (13.5, 12, -2)), name='eixo'))
    parts = [((2, 2, -9), (14, 3, -2)),            # fundo
             ((2, 3, -9), (3, 11, -2)), ((13, 3, -9), (14, 11, -2)),   # laterais
             ((2, 3, -2.5), (14, 12, -2))]         # costas
    for fr, to in parts:
        els.append(box(fr, to, f('#anvil', fr, to), name='cacamba'))
    els.append(box((3, 3, -8.5), (13, 5, -2.5), {'up': {'texture': '#rooted'}, 'north': {'texture': '#rooted'}}, name='terra'))
    for x0 in (2.5, 5.5, 8.5, 11.5):
        els.append(box((x0, 1.5, -10.5), (x0 + 2, 3, -9), f('#iron', (x0, 1.5, -10.5), (x0 + 2, 3, -9)), name='dente'))
    return els

def H(lit, ang=0):
    """Broca helicoidal: eixo de ferro com laminas em espiral (cada uma 22.5 graus a frente)."""
    els = casing() + plates(lit)
    els.append(box((5, 5, -1), (11, 11, 0), f('#anvil', (5, 5, -1), (11, 11, 0)), name='flange'))
    els.append(box((7, 7, -9), (9, 9, -1), f('#anvil', (7, 7, -9), (9, 9, -1)), name='eixo'))
    for i in range(7):
        z = -1.5 - i * 1.1
        r = 5.5 - i * 0.6
        e = box((8 - r, 7.5, z - 0.6), (8 + r, 8.5, z), f('#iron', (8 - r, 7.5, z - 0.6), (8 + r, 8.5, z)), name='lamina')
        e['rotation'] = {'origin': [8, 8, z], 'axis': 'z', 'angle': [0, 22.5, 45, -22.5][(i + ang) % 4]}
        els.append(e)
    els.append(box((7.5, 7.5, -10), (8.5, 8.5, -9), f('#iron', (7.5, 7.5, -10), (8.5, 8.5, -9)), name='ponta'))
    return els

tex = {'cdeep': 'minecraft:block/cobbled_deepslate', 'copper': 'minecraft:block/cut_copper', 'iron': 'minecraft:block/iron_block',
       'anvil': 'minecraft:block/anvil', 'redstone': 'minecraft:block/redstone_block', 'stick': 'minecraft:block/stripped_oak_log',
       'rooted': 'minecraft:block/rooted_dirt', 'moss': 'minecraft:block/moss_block', 'tip': 'minecraft:block/pointed_dripstone_down_tip',
       'frustum': 'minecraft:block/pointed_dripstone_down_frustum', 'plate': 'mistech:block/runic_crusher_rune_plate',
       'plate_on': 'mistech:block/runic_crusher_rune_plate_on', 'particle': 'minecraft:block/cobbled_deepslate', 'dripstone': 'minecraft:block/dripstone_block'}
for n, fn in (('P', P), ('D', D), ('E', E)):
    for lit in (False, True):
        json.dump({'textures': tex, 'elements': fn(lit)}, open(f'{M}/e2{n}{"_on" if lit else ""}.json', 'w'), indent=1)
for i in range(4):
    json.dump({'textures': tex, 'elements': H(True, i)}, open(f'{M}/e2H_f{i}.json', 'w'), indent=1)
print('ok')
