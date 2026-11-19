"""Texturas do Relógio Astral (Fase 6), 16x16.
Laterais = lápis-lazúli escurecido com moldura de ouro; frente = mostrador com anel de ouro e o ícone do modo.
Uso (na pasta do projeto): python3 tools/sky/gen_textures.py"""
import math
from PIL import Image

OUT = 'src/main/resources/assets/mistech/textures/block/'
VAN = 'vanilla/assets/minecraft/textures/block/'
GOLD = [(120, 84, 20), (200, 150, 50), (255, 226, 120)]


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def ramp(cols, t):
    t = max(0.0, min(1.0, t)); f = t * (len(cols) - 1); i = min(int(f), len(cols) - 2); r = f - i
    return tuple(round(cols[i][k] * (1 - r) + cols[i + 1][k] * r) for k in range(3))


def recolor(im, cols):
    im = im.convert('RGBA').crop((0, 0, 16, 16))
    ls = [lum(im.getpixel((x, y))) for y in range(16) for x in range(16)]
    a, b = min(ls), max(ls)
    out = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            out.putpixel((x, y), ramp(cols, (lum(im.getpixel((x, y))) - a) / ((b - a) or 1)) + (255,))
    return out


NAVY = [(8, 12, 40), (18, 28, 80), (34, 52, 130), (60, 90, 180)]
lapis = Image.open(VAN + 'lapis_block.png')


def framed(base):
    im = base.copy()
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            im.putpixel((x, y), GOLD[1] + (255,))
    for i in range(1, 15):
        for x, y in ((i, 1), (1, i)):
            im.putpixel((x, y), GOLD[2] + (255,))
        for x, y in ((i, 14), (14, i)):
            im.putpixel((x, y), GOLD[0] + (255,))
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        im.putpixel((x, y), (255, 246, 200, 255))
    return im


side = framed(recolor(lapis, NAVY))
side.save(OUT + 'astral_clock_side.png')

DIAL = [(4, 6, 22), (10, 14, 44), (18, 24, 64)]


def dial(icon):
    im = framed(recolor(lapis, DIAL))
    # anel de ouro
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 5.3 <= d < 6.2:
                im.putpixel((x, y), GOLD[1 if (x + y) % 3 else 2] + (255,))
    # marcas das horas
    for x, y in ((7, 2), (8, 2), (7, 13), (8, 13), (2, 7), (2, 8), (13, 7), (13, 8)):
        im.putpixel((x, y), GOLD[2] + (255,))
    for (x, y), c in icon.items():
        im.putpixel((x, y), c + (255,))
    return im


def shape(rows, palette):
    out = {}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                out[(x + 4, y + 4)] = palette[ch]
    return out


MOON = shape(['..##....', '.##.....', '##......', '##......', '##......', '##.....#', '.##...##', '..####..'],
             {'#': (230, 236, 255)})
STAR = shape(['...##...', '...##...', '..####..', '########', '########', '..####..', '...##...', '...##...'],
             {'#': (255, 230, 120)})
SUN = shape(['#..##..#', '..####..', '.######.', '########', '########', '.######.', '..####..', '#..##..#'],
            {'#': (255, 200, 60)})
DROP = shape(['...##...', '...##...', '..####..', '..####..', '.######.', '.##o###.', '.######.', '..####..'],
             {'#': (150, 70, 210), 'o': (220, 180, 255)})
for name, icon in (('moon', MOON), ('event', STAR), ('day', SUN), ('taint', DROP)):
    dial(icon).save(OUT + 'astral_clock_front_%s.png' % name)
print('ok')
