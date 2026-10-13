"""Narguilé Rúnico v2: modelo em partes (multipart) que mostra o que está dentro — líquido da base no vaso de vidro,
fumo no fornilho, brasa acesa e a pedra da runa (tingida com a cor da runa).
Rodar na raiz do projeto:  python tools/utility/hookah_v2.py"""
import json, random, sys
from PIL import Image
sys.path.insert(0, 'tools/pendencias2')
from res import A, wj

T = 'src/main/resources/assets/mistech/textures/block/'
rnd = random.Random(5)


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def img(fn):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    p = im.load()
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            if c:
                p[x, y] = c
    return im


def pick(cols, x, y, k=3):
    return cols[(x * 7 + y * 13 + rnd.randrange(k)) % len(cols)]


# ---------- texturas ----------
# vidro roxo translúcido com bordas mais fortes e runas douradas
def glass(x, y):
    edge = x in (0, 15) or y in (0, 15)
    if edge:
        return hexc('c9a2ff') + (190,)
    if (x, y) in ((4, 4), (5, 5), (4, 6), (11, 9), (10, 10), (11, 11), (7, 12), (8, 12)):
        return hexc('f0cf6a') + (220,)
    if x == 2 and 2 <= y <= 12:
        return hexc('f4eaff') + (150,)
    return hexc('7a44c0') + (70,)


img(glass).save(T + 'runic_hookah_glass.png')
BRASS = [hexc('9a6c22'), hexc('d8a640'), hexc('c08a30'), hexc('f8e090')]
img(lambda x, y: (BRASS[3] if y % 5 == 0 else BRASS[(x + y) % 3]) + (255,)).save(T + 'runic_hookah_brass.png')
CLAY = [hexc('6a3420'), hexc('8a4428'), hexc('a05a34')]
img(lambda x, y: (hexc('c07040') if y in (0, 1) else pick(CLAY, x, y)) + (255,)).save(T + 'runic_hookah_clay.png')
img(lambda x, y: (hexc('2a1810') if (x + y) % 3 else hexc('3a2418')) + (255,)).save(T + 'runic_hookah_bowl.png')
HOSE = [hexc('0e2a2a'), hexc('1c4c48'), hexc('2e7268')]
img(lambda x, y: (hexc('d8a640') if y % 6 == 0 else HOSE[(x + 2 * y) % 3]) + (255,)).save(T + 'runic_hookah_hose.png')
# fumos
img(lambda x, y: pick([hexc('5a3416'), hexc('7a4a20'), hexc('9a6430'), hexc('6a3e18')], x, y, 4) + (255,)).save(T + 'runic_hookah_tobacco.png')
img(lambda x, y: pick([hexc('3a6a24'), hexc('4e8a30'), hexc('2c5418'), hexc('6aa040')], x, y, 4) + (255,)).save(T + 'runic_hookah_hemp.png')
img(lambda x, y: (hexc('e8d040') if rnd.random() < 0.12 else pick([hexc('7a9a6a'), hexc('5e7e52'), hexc('90b080')], x, y)) + (255,)).save(
    T + 'runic_hookah_rue.png')
img(lambda x, y: ((hexc('ffe070') if rnd.random() > 0.8 else hexc('ff8a20') if rnd.random() > 0.4 else hexc('c03a10')) + (255,))).save(
    T + 'runic_hookah_ember_lit.png')
# água (tinta 1) em tons de cinza claro
img(lambda x, y: ((200, 200, 200, 200) if (x + y * 3) % 7 else (240, 240, 240, 210))).save(T + 'runic_hookah_water.png')


# pedra da runa (tinta 0): cinza com glifo claro
def rune(x, y):
    glyph = (x == 7 or x == 8) and 3 <= y <= 12 or (y == 5 and 5 <= x <= 10) or (y == 10 and x in (5, 6, 9, 10))
    return (255, 255, 255, 255) if glyph else ((150, 150, 150, 255) if (x + y) % 4 else (120, 120, 120, 255))


img(rune).save(T + 'runic_hookah_rune.png')

# ---------- modelos ----------
SIDES = ('north', 'south', 'east', 'west')
ALL = SIDES + ('up', 'down')


def el(frm, to, tex, faces=ALL, uv=None, tint=None):
    f = {}
    for d in faces:
        face = {'texture': tex}
        if uv:
            face['uv'] = uv
        if tint is not None:
            face['tintindex'] = tint
        f[d] = face
    return {'from': frm, 'to': to, 'faces': f}


TEX = {'particle': 'mistech:block/runic_hookah_brass', 'glass': 'mistech:block/runic_hookah_glass', 'brass': 'mistech:block/runic_hookah_brass',
       'clay': 'mistech:block/runic_hookah_clay', 'bowl': 'mistech:block/runic_hookah_bowl', 'hose': 'mistech:block/runic_hookah_hose'}
frame = [
    el([4.5, 0, 4.5], [11.5, 0.5, 11.5], '#brass', uv=[0, 0, 7, 7]),            # pé
    el([5, 0.5, 5], [11, 6.5, 11], '#glass', uv=[0, 2, 6, 8]),                  # vaso de vidro
    el([6, 6.5, 6], [10, 7.5, 10], '#brass', uv=[0, 0, 4, 1]),                  # gargalo
    el([7.5, 7.5, 7.5], [8.5, 11, 8.5], '#brass', uv=[2, 0, 3, 4]),             # haste
    el([4.5, 11, 4.5], [11.5, 11.5, 11.5], '#brass', uv=[0, 4, 7, 11]),         # bandeja
    el([6, 11.5, 6], [10, 14, 10], '#clay', faces=SIDES + ('down',), uv=[0, 0, 4, 2.5]),   # fornilho
    el([6.2, 13.2, 6.2], [9.8, 13.3, 9.8], '#bowl', faces=('up',), uv=[0, 0, 4, 4]),       # fundo do fornilho
    # mangueiras: três saindo do gargalo e caindo até o chão
    el([3, 5.5, 7.5], [6, 6.5, 8.5], '#hose', uv=[0, 0, 3, 1]),
    el([2, 0, 7.5], [3, 6.5, 8.5], '#hose', uv=[0, 0, 1, 6.5]),
    el([10, 5.5, 7.5], [13, 6.5, 8.5], '#hose', uv=[0, 0, 3, 1]),
    el([13, 0, 7.5], [14, 6.5, 8.5], '#hose', uv=[0, 0, 1, 6.5]),
    el([7.5, 5.5, 3], [8.5, 6.5, 6], '#hose', uv=[0, 0, 1, 3]),
    el([7.5, 0, 2], [8.5, 6.5, 3], '#hose', uv=[0, 0, 1, 6.5]),
]
wj(A + 'models/block/runic_hookah.json', {'parent': 'minecraft:block/block', 'textures': TEX, 'elements': frame})
LIQUID = {1: ('water', 'mistech:block/runic_hookah_water', 1), 2: ('impure', 'mistech:block/impure_essence_still', None),
          3: ('pure', 'mistech:block/purified_essence_still', None)}
for _, (name, tex, tint) in LIQUID.items():
    wj(A + 'models/block/runic_hookah_liquid_%s.json' % name, {'textures': {'particle': tex, 'liquid': tex},
        'elements': [el([5.5, 0.6, 5.5], [10.5, 4.5, 10.5], '#liquid', uv=[3, 3, 8, 7], tint=tint)]})
for name in ('tobacco', 'hemp', 'rue'):
    wj(A + 'models/block/runic_hookah_leaf_%s.json' % name, {'textures': {'particle': 'mistech:block/runic_hookah_' + name,
        'leaf': 'mistech:block/runic_hookah_' + name},
        'elements': [el([6.3, 13.3, 6.3], [9.7, 14.3, 9.7], '#leaf', uv=[4, 4, 8, 8]),
                     el([7, 14.3, 7], [8.5, 14.8, 8.8], '#leaf', uv=[2, 2, 4, 4])]})
wj(A + 'models/block/runic_hookah_ember.json', {'textures': {'particle': 'mistech:block/runic_hookah_ember_lit', 'ember': 'mistech:block/runic_hookah_ember_lit'},
    'elements': [el([6.6, 14.3, 6.6], [9.4, 15, 9.4], '#ember', uv=[0, 0, 3, 3])]})
wj(A + 'models/block/runic_hookah_rune.json', {'textures': {'particle': 'mistech:block/runic_hookah_rune', 'rune': 'mistech:block/runic_hookah_rune'},
    'elements': [el([6.75, 11.5, 4.7], [9.25, 14, 5.5], '#rune', uv=[4, 2, 12, 14], tint=0)]})

parts = [{'apply': {'model': 'mistech:block/runic_hookah'}}]
for b, (name, _, _) in LIQUID.items():
    parts.append({'when': {'base': str(b)}, 'apply': {'model': 'mistech:block/runic_hookah_liquid_' + name}})
for l, name in ((1, 'tobacco'), (2, 'hemp'), (3, 'rue')):
    parts.append({'when': {'leaf': str(l)}, 'apply': {'model': 'mistech:block/runic_hookah_leaf_' + name}})
parts.append({'when': {'lit': 'true'}, 'apply': {'model': 'mistech:block/runic_hookah_ember'}})
parts.append({'when': {'rune': '|'.join(str(i) for i in range(1, 21))}, 'apply': {'model': 'mistech:block/runic_hookah_rune'}})
wj(A + 'blockstates/runic_hookah.json', {'multipart': parts})
print('ok')
