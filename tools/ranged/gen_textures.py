"""Texturas da Adaga de Prata, arcos (Turvo, Mitril, Adamantina, Oricalco, Oricalco Puro), Besta Mágica e Virote Arcano.
Uso (raiz do projeto): python tools/ranged/gen_textures.py [pasta_texturas_vanilla]
A pasta vanilla é a 'textures' extraída do jar do Minecraft (padrão: vanilla/assets/minecraft/textures); usa item/bow*.png e
item/crossbow*.png. A madeira marrom é recolorida pela luminância numa rampa de cores por material."""
import colorsys, sys
from PIL import Image

VAN = (sys.argv[1] if len(sys.argv) > 1 else 'vanilla/assets/minecraft/textures').rstrip('/') + '/'
OUT = 'src/main/resources/assets/mistech/textures/item/'


def hx(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


RAMPS = {
    'murky': ['141216', '2a2730', '45414d', '6b6577', 'c9a640'],
    'mithril': ['2d4a66', '5c86a8', '9fd0ec', 'e6f7ff'],
    'adamantine': ['4a0f1c', '8a1f33', 'd14a5e', 'f2a0a8'],
    'orichalcum': ['5a2d0a', 'a85a14', 'e8962e', 'ffd27a'],
    'pure_orichalcum': ['8a7440', 'd6c27a', 'f5ebc0', 'ffffff'],
    'arcane': ['1f1430', '3d2560', '6b45a8', 'b48cf0'],
}
GOLD = ['6a4e14', 'b8902c', 'f2d070']


def ramp(colors, t):
    cs = [hx(c) for c in colors]
    t = max(0.0, min(1.0, t)) * (len(cs) - 1)
    i = min(int(t), len(cs) - 2)
    f = t - i
    return tuple(round(cs[i][k] + (cs[i + 1][k] - cs[i][k]) * f) for k in range(3)) + (255,)


def lum(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255


def is_wood(c):
    h, s, v = colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)
    return 0.02 < h < 0.16 and s > 0.3


def is_gray(c):
    return max(c[:3]) - min(c[:3]) < 12 and c[0] < 0xe0


def recolor(src, dst, wood_ramp, gray_ramp=None):
    im = Image.open(VAN + 'item/' + src + '.png').convert('RGBA')
    px = im.load()
    woods = [lum(px[x, y]) for y in range(16) for x in range(16) if px[x, y][3] and is_wood(px[x, y])]
    grays = [lum(px[x, y]) for y in range(16) for x in range(16) if px[x, y][3] and is_gray(px[x, y])]
    lo, hi = (min(woods), max(woods)) if woods else (0, 1)
    glo, ghi = (min(grays), max(grays)) if grays else (0, 1)
    for y in range(16):
        for x in range(16):
            c = px[x, y]
            if not c[3]:
                continue
            if is_wood(c):
                px[x, y] = ramp(wood_ramp, (lum(c) - lo) / max(1e-6, hi - lo))
            elif gray_ramp and is_gray(c):
                px[x, y] = ramp(gray_ramp, (lum(c) - glo) / max(1e-6, ghi - glo))
    im.save(OUT + dst + '.png')


for name, r in (('murky_bow', 'murky'), ('mithril_bow', 'mithril'), ('adamantine_bow', 'adamantine'),
                ('orichalcum_bow', 'orichalcum'), ('pure_orichalcum_bow', 'pure_orichalcum')):
    recolor('bow', name, RAMPS[r])
    for i in range(3):
        recolor('bow_pulling_%d' % i, '%s_pulling_%d' % (name, i), RAMPS[r])

recolor('crossbow_standby', 'arcane_crossbow_standby', RAMPS['arcane'], GOLD)
for i in range(3):
    recolor('crossbow_pulling_%d' % i, 'arcane_crossbow_pulling_%d' % i, RAMPS['arcane'], GOLD)
recolor('crossbow_arrow', 'arcane_crossbow_bolt', RAMPS['arcane'], GOLD)
# virote carregado: o "arco da flecha" vira um cristal claro no meio
im = Image.open(OUT + 'arcane_crossbow_bolt.png')
px = im.load()
for y in range(16):
    for x in range(16):
        c = px[x, y]
        if c[3] and c[:3] == (255, 255, 255):
            px[x, y] = (0xE8, 0xD8, 0xFF, 255)
im.save(OUT + 'arcane_crossbow_bolt.png')

# ---------- Virote Arcano (cinza; a cor da runa vem do tint) ----------
im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
px = im.load()
for y in range(16):
    for x in range(16):
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
        if d < 2.2:
            px[x, y] = (255, 255, 255, 255)
        elif d < 4.0:
            v = int(230 - (d - 2.2) * 40)
            px[x, y] = (v, v, v, 255)
        elif d < 5.5 and (x + y) % 2 == 0:
            px[x, y] = (150, 150, 150, 200)
for (x, y) in ((7, 1), (8, 14), (1, 8), (14, 7)):
    px[x, y] = (220, 220, 220, 255)
im.save(OUT + 'arcane_bolt.png')

# ---------- Adaga de Prata (desenhada) ----------
im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
px = im.load()
S = {'o': hx('1e2228'), 'b': hx('c8d0d8'), 'w': hx('f4f8fc'), 'm': hx('8a96a4'), 'g': hx('5a6a7c'),
     'h': hx('3a2a20'), 'l': hx('6a4a32'), 'f': hx('ece4d0'), 'F': hx('b8ac90')}
art = [
    '................',
    '................',
    '............oo..',
    '...........owbo.',
    '..........owbmo.',
    '.........owbmo..',
    '........owbmo...',
    '.......owbmo....',
    '...oo.owbmo.....',
    '...ogoobmo......',
    '....ogggo.......',
    '....olgoo.......',
    '...olho.go......',
    '..ofho...o......',
    '.oFfo...........',
    '..oo............',
]
for y, row in enumerate(art):
    for x, ch in enumerate(row):
        if ch in S:
            px[x, y] = S[ch] + (255,)
im.save(OUT + 'silver_dagger.png')
print('ok')
