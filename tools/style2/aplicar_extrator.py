"""Gera os modelos do Extrator de Essencia (cadinho de obsidiana) direto no src. A essencia e o olho sao do renderer."""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
SRC = os.path.join(os.path.dirname(__file__), '../../src/main/resources/assets/mistech/models')

def tier(fr, to, side, top='#obs', bottom='#obs', name=None):
    f = {}
    for d in ('north', 'south', 'east', 'west'):
        u0, u1 = (fr[0], to[0]) if d in ('north', 'south') else (fr[2], to[2])
        f[d] = {'texture': side, 'uv': [u0, 16 - to[1], u1, 16 - fr[1]]}
    if top: f['up'] = {'texture': top}
    if bottom: f['down'] = {'texture': bottom, 'cullface': 'down'} if fr[1] == 0 else {'texture': bottom}
    return box(fr, to, f, name=name)

def ring(y0, y1, a, b, w, tex, top=None, name=None):
    return [tier((a, y0, a), (b, y1, a + w), tex, top, name=name), tier((a, y0, b - w), (b, y1, b), tex, top, name=name),
            tier((a, y0, a + w), (a + w, y1, b - w), tex, top, name=name), tier((b - w, y0, a + w), (b, y1, b - w), tex, top, name=name)]

def elements(lit):
    els = [tier((3, 0, 3), (13, 1.5, 13), '#obs', name='base'), tier((2, 1.5, 2), (14, 3, 14), '#obs', name='pe'),
           tier((1, 3, 1), (15, 4.5, 15), '#obs', name='bojo_baixo'),
           tier((0.5, 4.5, 0.5), (15.5, 11, 15.5), '#crying' if lit else '#obs', name='bojo'),
           tier((1, 11, 1), (15, 12, 15), '#copper', top=None, bottom=None, name='aro_cobre')]
    els += ring(12, 13.5, 1.5, 14.5, 2, '#obs', '#obs', 'ombro') + ring(13.5, 14.5, 2.5, 13.5, 1.5, '#obs', '#obs', 'gargalo') \
        + ring(14.5, 16, 1.5, 14.5, 2, '#obs', '#obs', 'borda')
    els.append(tier((3, 11.9, 3), (13, 12, 13), '#inner', top='#inner', bottom=None, name='fundo_interno'))
    return els

TEX = {'obs': 'minecraft:block/obsidian', 'crying': 'minecraft:block/crying_obsidian', 'copper': 'minecraft:block/cut_copper',
       'inner': 'mistech:block/essence_extractor_cauldron_inner', 'particle': 'minecraft:block/obsidian'}
for lv in range(4):
    for lit in (False, True):
        name = 'essence_extractor' + (f'_level{lv}' if lv else '') + ('_on' if lit else '')
        json.dump({'parent': 'minecraft:block/block', 'textures': TEX, 'elements': elements(lit)}, open(f'{SRC}/block/{name}.json', 'w'), indent=2)
# item: cadinho + olho parado de frente (no bloco o olho e desenhado pelo renderer)
eye = box((5, 13.5, 5), (11, 19.5, 11), {**{d: {'texture': '#eye_back', 'uv': [5, 5, 11, 11]} for d in ('south', 'east', 'west')},
          'north': {'texture': '#eye_front', 'uv': [5, 5, 11, 11]}, 'up': {'texture': '#eye_top', 'uv': [5, 5, 11, 11]},
          'down': {'texture': '#eye_top', 'uv': [5, 5, 11, 11]}}, name='olho')
it = {'parent': 'minecraft:block/block', 'textures': {**TEX, 'eye_front': 'mistech:block/essence_extractor_eye_front',
      'eye_back': 'mistech:block/essence_extractor_eye_back', 'eye_top': 'mistech:block/essence_extractor_eye_top'},
      'elements': elements(False) + [eye],
      'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, -1.5, 0], 'scale': [0.55, 0.55, 0.55]}}}
json.dump(it, open(f'{SRC}/item/essence_extractor.json', 'w'), indent=2)
print('modelos gerados')
