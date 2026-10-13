"""Infusor Arcano v2: três blocos bem diferentes.
  Infusor (I): púlpito de pedra roxa com um livro aberto e cachos de ametista.
  Maior (II): pedra escura com friso de ouro, quatro colunas de ouro e um cristal magenta flutuando.
  Astral (III): pedra azul-noite com estrelas, anel de prata em estrela e um cristal-estrela branco.
Rodar na raiz do projeto:  python tools/utility/infuser_v2.py"""
import math, random, sys
from PIL import Image
sys.path.insert(0, 'tools/pendencias2')
from res import A, wj

T = 'src/main/resources/assets/mistech/textures/block/'
rnd = random.Random(9)


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def grad(cols, t):
    t = max(0.0, min(1.0, t))
    f = t * (len(cols) - 1)
    i = min(int(f), len(cols) - 2)
    r = f - i
    return tuple(round(cols[i][k] * (1 - r) + cols[i + 1][k] * r) for k in range(3))


def save(name, fn):
    im = Image.new('RGBA', (16, 16))
    p = im.load()
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            p[x, y] = c + (255,) if len(c) == 3 else c
    im.save(T + name + '.png')


def bricks(cols, mortar, rune=None, stars=False):
    """Pedra em blocos (2 fileiras de 8 px) com runa entalhada opcional."""
    def fn(x, y):
        row = y // 4
        off = 4 if row % 2 else 0
        if y % 4 == 3 or (x + off) % 8 == 7:
            return mortar
        t = 0.55 + rnd.uniform(-0.12, 0.12) - 0.15 * ((y % 4) / 3)
        c = grad(cols, t)
        if stars and rnd.random() < 0.05:
            c = (240, 248, 255)
        if rune and (x, y) in rune[0]:
            c = rune[1]
        return c
    return fn


RUNE_PTS = {(7, 5), (8, 5), (7, 6), (7, 7), (8, 7), (9, 6), (6, 9), (7, 9), (8, 9), (9, 9), (7, 10), (8, 10)}

TIERS = {
    'arcane_infuser': dict(stone=[hexc('1e1430'), hexc('35254f'), hexc('4a3670'), hexc('5e4890')], mortar=hexc('120b1e'),
                           rune=hexc('c49cff'), trim=[hexc('7a7a8a'), hexc('b8b8c8'), hexc('e8e8f0')],
                           crystal=[hexc('5a2a9a'), hexc('9a5ae0'), hexc('d8b8ff')], stars=False),
    'greater_arcane_infuser': dict(stone=[hexc('1a0a14'), hexc('3a1426'), hexc('5a2038'), hexc('7a2c4c')], mortar=hexc('100408'),
                                   rune=hexc('ffd060'), trim=[hexc('8a5a10'), hexc('d8a030'), hexc('fff0a0')],
                                   crystal=[hexc('8a0a5a'), hexc('e040a0'), hexc('ffb0e0')], stars=False),
    'astral_arcane_infuser': dict(stone=[hexc('060a1e'), hexc('0e1838'), hexc('182a5a'), hexc('24407a')], mortar=hexc('03050e'),
                                  rune=hexc('9ae0ff'), trim=[hexc('6a7a9a'), hexc('c0d0f0'), hexc('ffffff')],
                                  crystal=[hexc('3a8ad0'), hexc('a0e8ff'), hexc('ffffff')], stars=True),
}

for name, c in TIERS.items():
    save(name + '_side', bricks(c['stone'], c['mortar'], (RUNE_PTS, c['rune']), c['stars']))
    save(name + '_bottom', bricks(c['stone'], c['mortar'], None, False))

    def top(x, y, c=c):
        d = math.hypot(x - 7.5, y - 7.5)
        base = grad(c['stone'], 0.5 + rnd.uniform(-0.1, 0.1))
        if c['stars'] and rnd.random() < 0.06:
            base = (240, 248, 255)
        if 5.6 <= d <= 6.6:
            return grad(c['trim'], 0.6 + 0.3 * math.sin(x + y))
        if 3.0 <= d <= 3.6:
            return c['rune']
        ang = math.atan2(y - 7.5, x - 7.5)
        if 3.6 < d < 5.6 and abs(math.sin(ang * 3)) < 0.18:
            return c['rune']
        if x in (0, 15) or y in (0, 15):
            return grad(c['trim'], 0.4)
        return base
    save(name + '_top', top)
    save(name + '_trim', lambda x, y, c=c: grad(c['trim'], 0.35 + 0.5 * ((x + y) % 4 == 0) + 0.3 * (y < 2)))
    save(name + '_crystal', lambda x, y, c=c: grad(c['crystal'], 0.3 + 0.6 * (1 - abs(x - 7.5) / 8) + 0.15 * ((x - y) % 5 == 0)))

# livro aberto (só o Infusor I)
save('arcane_infuser_page', lambda x, y: (hexc('3a2a20') if y in (0, 15) or x in (0, 15) else hexc('8a7a90') if y % 3 == 1 and 2 < x < 13
                                          else hexc('f0e6d0') if (x + y) % 7 else hexc('e2d6bc')))
save('arcane_infuser_cover', lambda x, y: hexc('5a1a2a') if (x + y) % 5 else hexc('7a2a3a'))

# ---------- modelos ----------
SIDES = ('north', 'south', 'east', 'west')
ALL = SIDES + ('up', 'down')


def el(frm, to, tex, top=None, rot=None, faces=ALL, uvfit=True):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    f = {}
    for d in faces:
        t = top if (d == 'up' and top) else tex
        if d in ('up', 'down'):
            uv = [x0, z0, x1, z1]
        elif d in ('north', 'south'):
            uv = [x0, 16 - y1, x1, 16 - y0]
        else:
            uv = [z0, 16 - y1, z1, 16 - y0]
        f[d] = {'texture': t, 'uv': uv}
        if d == 'down' and y0 == 0:
            f[d]['cullface'] = 'down'
    e = {'from': frm, 'to': to, 'faces': f}
    if rot:
        e['rotation'] = rot
    return e


def y45(cx, cy, cz, angle=45, axis='y'):
    return {'angle': angle, 'axis': axis, 'origin': [cx, cy, cz]}


def textures(name):
    return {'particle': 'mistech:block/%s_side' % name, 'side': 'mistech:block/%s_side' % name, 'top': 'mistech:block/%s_top' % name,
            'bottom': 'mistech:block/%s_bottom' % name, 'trim': 'mistech:block/%s_trim' % name, 'crystal': 'mistech:block/%s_crystal' % name}


def pedestal():
    return [el([1, 0, 1], [15, 3, 15], '#side'), el([3.5, 3, 3.5], [12.5, 9, 12.5], '#side'), el([1.5, 9, 1.5], [14.5, 11, 14.5], '#side', top='#top')]


# I: livro aberto + cachos de ametista
t1 = textures('arcane_infuser')
t1.update({'page': 'mistech:block/arcane_infuser_page', 'cover': 'mistech:block/arcane_infuser_cover'})
m1 = pedestal() + [
    el([4, 11, 5], [12, 11.4, 11], '#cover'),
    el([4.2, 11.4, 5.3], [8, 11.9, 10.7], '#page', rot=y45(8, 11.4, 8, 22.5, 'z')),
    el([8, 11.4, 5.3], [11.8, 11.9, 10.7], '#page', rot=y45(8, 11.4, 8, -22.5, 'z')),
]
for (x, z, h) in ((2.3, 2.3, 3), (12.2, 2.6, 2), (2.6, 12.2, 2), (12.3, 12.3, 3.5)):
    m1.append(el([x, 11, z], [x + 1.5, 11 + h, z + 1.5], '#crystal', rot=y45(x + 0.75, 11, z + 0.75)))
    m1.append(el([x + 0.5, 11, z - 0.2], [x + 1.3, 11 + h * 0.6, z + 0.6], '#crystal', rot=y45(x + 0.9, 11, z + 0.2, 22.5)))
wj(A + 'models/block/arcane_infuser.json', {'parent': 'minecraft:block/block', 'textures': t1, 'elements': m1})

# II: friso de ouro, quatro colunas e cristal magenta
t2 = textures('greater_arcane_infuser')
m2 = pedestal() + [
    el([0.8, 2.2, 0.8], [15.2, 3.2, 15.2], '#trim'),
    el([3, 8.2, 3], [13, 9, 13], '#trim'),
    el([5.5, 11, 5.5], [10.5, 11.6, 10.5], '#trim'),
    el([6.75, 12.5, 6.75], [9.25, 16, 9.25], '#crystal', rot=y45(8, 12.5, 8)),
    el([7.2, 11.6, 7.2], [8.8, 12.5, 8.8], '#crystal', rot=y45(8, 11.6, 8)),
]
for (x, z) in ((2, 2), (12.5, 2), (2, 12.5), (12.5, 12.5)):
    m2.append(el([x, 11, z], [x + 1.5, 15, z + 1.5], '#trim'))
    m2.append(el([x - 0.25, 15, z - 0.25], [x + 1.75, 15.6, z + 1.75], '#crystal'))
wj(A + 'models/block/greater_arcane_infuser.json', {'parent': 'minecraft:block/block', 'textures': t2, 'elements': m2})

# III: anel de prata em estrela (dois quadrados, um girado) e cristal-estrela
t3 = textures('astral_arcane_infuser')
m3 = pedestal() + [
    el([0.8, 2.2, 0.8], [15.2, 3.2, 15.2], '#trim'),
    el([7.25, 11, 7.25], [8.75, 13, 8.75], '#trim'),
    el([6.5, 13, 6.5], [9.5, 16, 9.5], '#crystal', rot=y45(8, 13, 8)),
    el([7, 13.8, 5.6], [9, 15.2, 10.4], '#crystal', rot=y45(8, 14.5, 8, 45, 'x')),
]
for rot in (None, y45(8, 14, 8)):
    for (frm, to) in (([3.5, 14, 3.5], [12.5, 14.6, 4.2]), ([3.5, 14, 11.8], [12.5, 14.6, 12.5]), ([3.5, 14, 4.2], [4.2, 14.6, 11.8]),
                      ([11.8, 14, 4.2], [12.5, 14.6, 11.8])):
        m3.append(el(frm, to, '#trim', rot=rot))
for (x, z) in ((3.6, 3.6), (11.4, 3.6), (3.6, 11.4), (11.4, 11.4)):
    m3.append(el([x, 11, z], [x + 1, 14, z + 1], '#trim'))
wj(A + 'models/block/astral_arcane_infuser.json', {'parent': 'minecraft:block/block', 'textures': t3, 'elements': m3})
print('ok')
