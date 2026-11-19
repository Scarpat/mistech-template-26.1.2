"""Texturas da Mácula do lugar (Fase 4), estilo vanilla 16x16.
(A Grama Murcha já existe no Reino dos Gnomos e é reaproveitada.)
Filtro de Arruda = brilho verde do fundo da chaminé (animado quando ligado).
Uso (na pasta do projeto): python3 tools/area_taint/gen_textures.py"""
from PIL import Image

OUT = 'src/main/resources/assets/mistech/textures/block/'
VAN = 'vanilla/assets/minecraft/textures/block/'


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def ramp(cols, t):
    t = max(0.0, min(1.0, t)); f = t * (len(cols) - 1); i = min(int(f), len(cols) - 2); r = f - i
    return tuple(round(cols[i][k] * (1 - r) + cols[i + 1][k] * r) for k in range(3))


def recolor(im, cols, bright=0.0):
    im = im.convert('RGBA').crop((0, 0, 16, 16))
    ls = [lum(im.getpixel((x, y))) for y in range(16) for x in range(16) if im.getpixel((x, y))[3] > 0]
    a, b = min(ls), max(ls)
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            p = im.getpixel((x, y))
            if p[3] == 0:
                continue
            out.putpixel((x, y), ramp(cols, (lum(p) - a) / ((b - a) or 1) + bright) + (p[3],))
    return out


# fundo da chaminé do Filtro de Arruda
GLOW = [(20, 60, 24), (60, 150, 60), (140, 230, 120), (220, 255, 200)]
base = Image.open(VAN + 'moss_block.png')
recolor(base, [(14, 18, 14), (26, 34, 24), (40, 52, 36)]).save(OUT + 'rue_filter_inside.png')
frames = [recolor(base, GLOW, b) for b in (-0.15, 0.05, 0.25, 0.05)]
strip = Image.new('RGBA', (16, 64))
for i, f in enumerate(frames):
    strip.paste(f, (0, 16 * i))
strip.save(OUT + 'rue_filter_glow.png')
open(OUT + 'rue_filter_glow.png.mcmeta', 'w').write('{"animation":{"frametime":6,"interpolate":true}}\n')
print('ok')
