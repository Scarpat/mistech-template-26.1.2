"""Texturas da Fase 5 (Poço de Mana e Sangria Arcana), estilo vanilla 16x16.
Mana = água vanilla recolorida em azul-violeta (animada). Runa da Sangria = pedra-negra com um glifo entalhado (acende ciano).
Uso (na pasta do projeto): python3 tools/well/gen_textures.py"""
from PIL import Image

OUT = 'src/main/resources/assets/mistech/textures/block/'
VAN = 'vanilla/assets/minecraft/textures/block/'


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def ramp(cols, t):
    t = max(0.0, min(1.0, t)); f = t * (len(cols) - 1); i = min(int(f), len(cols) - 2); r = f - i
    return tuple(round(cols[i][k] * (1 - r) + cols[i + 1][k] * r) for k in range(3))


def recolor(im, cols, bright=0.0):
    out = Image.new('RGBA', (16, 16))
    ls = [lum(im.getpixel((x, y))) for y in range(16) for x in range(16)]
    a, b = min(ls), max(ls)
    for y in range(16):
        for x in range(16):
            out.putpixel((x, y), ramp(cols, (lum(im.getpixel((x, y))) - a) / ((b - a) or 1) + bright) + (255,))
    return out


def strip(path, frames, frametime):
    s = Image.new('RGBA', (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        s.paste(f, (0, 16 * i))
    s.save(path)
    open(path + '.mcmeta', 'w').write('{"animation":{"frametime":%d,"interpolate":true}}\n' % frametime)


water = Image.open(VAN + 'water_still.png').convert('RGBA')
n = water.height // 16
MANA = [(18, 24, 90), (40, 70, 190), (90, 150, 240), (170, 200, 255), (235, 225, 255)]
MANA_DIM = [(12, 14, 50), (26, 40, 110), (50, 84, 160), (90, 120, 200)]
recolor(water.crop((0, 0, 16, 16)), MANA_DIM).save(OUT + 'mana_well_mana.png')
strip(OUT + 'mana_well_mana_on.png', [recolor(water.crop((0, 16 * i, 16, 16 * i + 16)), MANA, 0.05) for i in range(0, n, 2)], 2)

# glifo da Sangria (gota sobre um raio)
GLYPH = ['................'] * 6 + [
         '.......##.......',
         '......#..#......',
         '.....#.##.#.....',
         '.....#.##.#.....',
         '......#..#......',
         '.......##.......'] + ['................'] * 4
stone = Image.open(VAN + 'polished_blackstone.png').convert('RGBA').crop((0, 0, 16, 16))


def rune(color):
    im = stone.copy()
    for y, row in enumerate(GLYPH):
        for x, c in enumerate(row):
            if c == '#':
                im.putpixel((x, y), color + (255,))
            elif y > 0 and GLYPH[y - 1][x] == '#' and c == '.':
                p = im.getpixel((x, y))
                im.putpixel((x, y), tuple(max(0, v - 18) for v in p[:3]) + (255,))
    return im


rune((44, 40, 52)).save(OUT + 'mana_siphon_rune.png')
strip(OUT + 'mana_siphon_rune_on.png', [rune(c) for c in ((60, 150, 220), (110, 210, 255), (200, 245, 255), (110, 210, 255))], 6)
print('ok')
