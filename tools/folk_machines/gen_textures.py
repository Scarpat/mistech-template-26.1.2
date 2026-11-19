"""Texturas da Fase 7: Brasa do Boitatá (bola de fogo vanilla recolorida em laranja com miolo azul).
Uso (na pasta do projeto): python3 tools/folk_machines/gen_textures.py"""
from PIL import Image

OUT = 'src/main/resources/assets/mistech/textures/item/'
VAN = 'vanilla/assets/minecraft/textures/item/'


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def ramp(cols, t):
    t = max(0.0, min(1.0, t)); f = t * (len(cols) - 1); i = min(int(f), len(cols) - 2); r = f - i
    return tuple(round(cols[i][k] * (1 - r) + cols[i + 1][k] * r) for k in range(3))


im = Image.open(VAN + 'fire_charge.png').convert('RGBA')
ls = [lum(im.getpixel((x, y))) for y in range(16) for x in range(16) if im.getpixel((x, y))[3] > 0]
a, b = min(ls), max(ls)
FIRE = [(60, 10, 4), (160, 40, 10), (240, 120, 30), (255, 210, 90)]
CORE = [(20, 40, 120), (60, 140, 255), (190, 240, 255)]
out = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        p = im.getpixel((x, y))
        if p[3] == 0:
            continue
        t = (lum(p) - a) / ((b - a) or 1)
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
        out.putpixel((x, y), (ramp(CORE, t) if d < 2.6 else ramp(FIRE, t)) + (p[3],))
out.save(OUT + 'boitata_ember.png')
print('ok')
