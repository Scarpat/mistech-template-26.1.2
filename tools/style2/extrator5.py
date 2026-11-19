"""Extrator v5: mesmo cadinho da v4, so com arte do jogo/mod.
Obsidiana vanilla; ligado, o bojo vira obsidiana chorosa vanilla; essencia = fluido impure_essence do mod;
Olho do Fim vanilla pairando acima (no jogo: o renderer gira o item). Sem runas pintadas."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import *
OUT = os.path.join(os.path.dirname(__file__), 'out/extrator5'); M = OUT + '/models'; os.makedirs(OUT + '/textures', exist_ok=True)
inner = V('obsidian'); shade(inner, (0, 0, 15, 15), 0.5); save(inner, OUT + '/textures/s5_inner.png')

def tier(fr, to, side, top='#obs', bottom='#obs', name=None):
    f = {}
    for d in ('north', 'south', 'east', 'west'):
        u0, u1 = (fr[0], to[0]) if d in ('north', 'south') else (fr[2], to[2])
        f[d] = {'texture': side, 'uv': [u0, 16 - to[1], u1, 16 - fr[1]]}
    if top: f['up'] = {'texture': top}
    if bottom: f['down'] = {'texture': bottom}
    return box(fr, to, f, name=name)

def ring(y0, y1, a, b, w, tex, top=None):
    return [tier((a, y0, a), (b, y1, a + w), tex, top), tier((a, y0, b - w), (b, y1, b), tex, top),
            tier((a, y0, a + w), (a + w, y1, b - w), tex, top), tier((b - w, y0, a + w), (b, y1, b - w), tex, top)]

def model(level, lit, eye_ang=None, bob=0, eye='A'):
    belly = '#crying' if lit else '#obs'
    els = [tier((3, 0, 3), (13, 1.5, 13), '#obs', name='base'), tier((2, 1.5, 2), (14, 3, 14), '#obs', name='pe'),
           tier((1, 3, 1), (15, 4.5, 15), '#obs', name='bojo_baixo'), tier((0.5, 4.5, 0.5), (15.5, 11, 15.5), belly, top='#obs', name='bojo'),
           tier((1, 11, 1), (15, 12, 15), '#copper', top=None, name='aro_cobre')]
    els += ring(12, 13.5, 1.5, 14.5, 2, '#obs', '#obs') + ring(13.5, 14.5, 2.5, 13.5, 1.5, '#obs', '#obs') + ring(14.5, 16, 1.5, 14.5, 2, '#obs', '#obs')
    els.append(tier((3, 11.9, 3), (13, 12, 13), '#inner', top='#inner', name='fundo_interno'))
    if level:
        els.append(box((3.5, 12, 3.5), (12.5, 12 + level * 1.2, 12.5), {'up': {'texture': '#essence'}}, name='essencia'))
    if eye_ang is not None and eye == 'A':   # item menor e baixo, saindo da boca
        y0 = 14.5 + bob
        for an in (45, -45):
            els.append({'from': [6, y0, 8], 'to': [10, y0 + 4, 8], 'shade': False,
                        'rotation': {'origin': [8, y0 + 2, 8], 'axis': 'y', 'angle': an},
                        'faces': {'north': {'texture': '#eye', 'uv': [0, 0, 16, 16]}, 'south': {'texture': '#eye', 'uv': [0, 0, 16, 16]}}})
    if eye_ang is not None and eye == 'B':   # cubo 3D 6x6x6, pixel na escala do bloco
        y0 = 13.5 + bob
        f = {d: {'texture': '#eye3d_back', 'uv': [5, 5, 11, 11]} for d in ('north', 'south', 'east', 'west')}
        f['north'] = {'texture': '#eye3d', 'uv': [5, 5, 11, 11]}
        f['up'] = {'texture': '#eye3d_top', 'uv': [5, 5, 11, 11]}; f['down'] = {'texture': '#eye3d_top', 'uv': [5, 5, 11, 11]}
        els.append(box((5, y0, 5), (11, y0 + 6, 11), f, rot={'origin': [8, y0 + 3, 8], 'axis': 'y', 'angle': eye_ang}, name='olho'))
    return els

tex = {'obs': 'minecraft:block/obsidian', 'crying': 'minecraft:block/crying_obsidian', 'copper': 'minecraft:block/cut_copper',
       'inner': 'mistech:block/s5_inner', 'essence': 'mistech:block/impure_essence_still', 'eye': 'minecraft:item/ender_eye', 'eye3d': 'mistech:block/s5_eye3d', 'eye3d_top': 'mistech:block/s5_eye3d_top', 'eye3d_back': 'mistech:block/s5_eye3d_back',
       'particle': 'minecraft:block/obsidian'}
os.makedirs(M, exist_ok=True)
for lv in range(4):
    for lit in (False, True):
        json.dump({'parent': 'minecraft:block/block', 'textures': tex, 'elements': model(lv, lit)}, open(f'{M}/s5_l{lv}{"_on" if lit else ""}.json', 'w'), indent=1)
for e in 'ABC':
    json.dump({'textures': tex, 'elements': model(3, True, 22.5 if e == 'B' else 0, 0, e)}, open(f'{M}/s5_eye{e}.json', 'w'), indent=1)
# olho 3D 6x6 (regiao 5..10): contorno escuro, iris em anel, fenda vertical, brilho no canto
E = {'o': R('0b2620')[0], 'd': R('17483a')[0], 'b': R('2f7a5a')[0], 'g': R('5fae4a')[0], 'l': R('a7e48e')[0],
     'p': R('050d0a')[0], 'q': R('123826')[0], 'w': R('eaffde')[0]}
SIDE = ["odbbdo", "dwllbd", "blppgb", "bgppgb", "dgllgd", "odbbdo"]
BACK = ["odbbdo", "dbggbd", "bggggb", "bgggbb", "dbbbbd", "oddddo"]
TOP = ["oddddo", "dbbbbd", "bbggbb", "bbggbb", "dbbbbd", "oddddo"]
for name, P in (('s5_eye3d', SIDE), ('s5_eye3d_top', TOP), ('s5_eye3d_back', BACK)):
    im = blank()
    for j, r in enumerate(P):
        for i, c in enumerate(r): im.putpixel((5 + i, 5 + j), E[c])
    save(im, OUT + '/textures/' + name + '.png')
for i in range(8):
    json.dump({'textures': tex, 'elements': model(3, True, -45 + i * 11.25 + 0, 0.4 * __import__('math').sin(i / 8 * 6.283), 'B')}, open(f'{M}/s5_eyeB_f{i}.json', 'w'), indent=1)
