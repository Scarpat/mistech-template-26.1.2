"""Texturas da cadeia de minério (Fase 3), estilo vanilla 16x16.
Torrão Impregnado = forma do minério bruto vanilla na cor do metal + veios roxos de Essência Impura.
Cristal de Minério = forma do fragmento de ametista na cor do metal + brilhos claros.
Uso (na pasta do projeto): python3 tools/refining/gen_textures.py"""
from PIL import Image
OUT = 'src/main/resources/assets/mistech/textures/item/'
VAN = 'vanilla/assets/minecraft/textures/item/'
# (escuro, médio, claro) de cada metal, tirados dos pós do mod
METALS = {
    'iron': ((70, 64, 60), (140, 132, 126), (214, 206, 198)),
    'gold': ((110, 72, 18), (196, 150, 50), (250, 222, 110)),
    'copper': ((88, 44, 26), (168, 96, 62), (232, 156, 112)),
    'mithril': ((42, 66, 88), (96, 140, 170), (176, 216, 240)),
    'adamantine': ((78, 10, 18), (160, 32, 44), (236, 92, 100)),
    'orichalcum': ((92, 52, 14), (172, 108, 40), (240, 176, 92)),
}

def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]

def ramp(cols, t):
    t = max(0.0, min(1.0, t)); f = t * (len(cols) - 1); i = min(int(f), len(cols) - 2); r = f - i
    return tuple(round(cols[i][k] * (1 - r) + cols[i + 1][k] * r) for k in range(3))

def recolor(src, cols, contrast=1.0):
    im = Image.open(VAN + src).convert('RGBA')
    px = [im.getpixel((x, y)) for y in range(16) for x in range(16)]
    ls = [lum(p) for p in px if p[3] > 0]
    a, b = min(ls), max(ls)
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            p = im.getpixel((x, y))
            if p[3] == 0:
                continue
            t = (lum(p) - a) / ((b - a) or 1)
            out.putpixel((x, y), ramp(cols, 0.5 + (t - 0.5) * contrast) + (p[3],))
    return out

VEIN = [(92, 40, 150), (150, 90, 210)]
for metal, (d, m, l) in METALS.items():
    clump = recolor('raw_iron.png', [tuple(max(0, c - 30) for c in d), d, m, l])
    # veios de essência: pixels médios na diagonal viram roxos
    for i, (x, y) in enumerate(((5, 5), (6, 6), (7, 6), (8, 7), (10, 9), (11, 10), (4, 10), (5, 11))):
        p = clump.getpixel((x, y))
        if p[3] > 0:
            clump.putpixel((x, y), VEIN[i % 2] + (255,))
    clump.save(OUT + metal + '_clump.png')
    crystal = recolor('amethyst_shard.png', [tuple(max(0, c - 25) for c in d), d, m, l, (255, 255, 255)], 1.1)
    crystal.save(OUT + metal + '_ore_crystal.png')
print('ok')
