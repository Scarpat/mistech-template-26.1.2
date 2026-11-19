"""Texturas do Conjurador Rúnico e do Cristal de Feitiço (estilo vanilla, 16x16).
Uso (na pasta do projeto): python3 tools/conjurer/gen_textures.py"""
from PIL import Image
import os
OUT_I = 'src/main/resources/assets/mistech/textures/item/'
OUT_B = 'src/main/resources/assets/mistech/textures/block/'

def inside(poly, x, y):
    c = False
    n = len(poly)
    for i in range(n):
        x1, y1 = poly[i]; x2, y2 = poly[(i + 1) % n]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            c = not c
    return c

# cristal: bipirâmide hexagonal em pé, levemente inclinada
POLY = [(8.5, 0.6), (12.4, 4.2), (11.6, 11.6), (7.5, 15.4), (3.6, 11.8), (4.4, 4.4)]

def crystal_mask():
    m = [[0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            if inside(POLY, x + 0.5, y + 0.5):
                m[y][x] = 1
    return m

def shade(x, y):
    """0..1: face esquerda clara, aresta central brilhante, face direita escura, ponta de baixo mais escura."""
    # aresta central: linha de (8.5,0.6) a (7.5,15.4)
    cx = 8.5 - (y + 0.5) * (1.0 / 14.8)
    d = (x + 0.5) - cx
    if abs(d) < 0.7:
        v = 0.95
    elif d < 0:
        v = 0.72 + 0.04 * d  # esquerda
    else:
        v = 0.42 - 0.04 * d  # direita
    if y >= 11:
        v -= 0.10 * (y - 10)
    if y <= 3:
        v += 0.08
    return max(0.05, min(1.0, v))

def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))

def ramp(cols, t):
    t = max(0.0, min(1.0, t)); f = t * (len(cols) - 1); i = min(int(f), len(cols) - 2)
    return lerp(cols[i], cols[i + 1], f - i)

def gem(cols, outline, path, glyph=None, sparkle=True):
    m = crystal_mask()
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if m[y][x]:
                im.putpixel((x, y), ramp(cols, shade(x, y)) + (255,))
    for y in range(16):
        for x in range(16):
            if not m[y][x] and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and m[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                im.putpixel((x, y), outline + (255,))
    if sparkle:
        for p in ((6, 4), (6, 5), (7, 3)):
            im.putpixel(p, ramp(cols, 1.0) + (255,))
    im.save(path)
    return m

# Cristal vazio: vidro lilás acinzentado, apagado
gem([(58, 48, 74), (112, 98, 138), (170, 158, 196), (226, 220, 240)], (34, 26, 46), OUT_I + 'blank_spell_crystal.png')
# Cristal gravado: camada cinza (tingida pela cor do elemento) + camada de runa brilhante (sem tinta)
m = gem([(40, 40, 40), (110, 110, 110), (190, 190, 190), (255, 255, 255)], (30, 30, 30), OUT_I + 'spell_crystal.png')
shine = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
GLYPH = [(8, 5), (8, 6), (7, 7), (8, 7), (9, 7), (8, 8), (8, 9), (7, 10), (9, 10), (6, 4), (6, 5), (7, 3)]
for i, p in enumerate(GLYPH):
    if m[p[1]][p[0]]:
        shine.putpixel(p, (255, 255, 255, 235 if i < 9 else 180))
shine.save(OUT_I + 'spell_crystal_glyph.png')
print('ok itens')

# ---------------------------------------------------------------- bloco: runas na coluna (apagada / acesa animada)
VAN = 'vanilla/assets/minecraft/textures/block/'
base = Image.open(VAN + 'polished_blackstone.png').convert('RGBA')

def runes(frame_glow, path):
    im = base.copy()
    # sulcos das runas (3 glifos verticais) - mesmos pixels em todos os quadros
    # a coluna mostra o meio da textura (x 4..11, y 2..7): uma runa ᛉ e dois pontos
    pts = [(8, y) for y in range(3, 8)] + [(7, 4), (6, 3), (9, 4), (10, 3), (5, 6), (11, 6)]
    for (x, y) in pts:
        if frame_glow is None:
            im.putpixel((x, y), (28, 22, 34, 255))
        else:
            im.putpixel((x, y), frame_glow + (255,))
    # filete de mitril embaixo
    for x in range(16):
        im.putpixel((x, 14), (150, 176, 196, 255) if x % 4 else (196, 220, 236, 255))
        im.putpixel((x, 15), (70, 86, 100, 255))
    return im

runes(None, None).save(OUT_B + 'runic_conjurer_column.png')
frames = []
for g in (0.55, 0.75, 1.0, 0.75):
    frames.append(runes(lerp((92, 40, 150), (214, 160, 255), g), None))
strip = Image.new('RGBA', (16, 16 * len(frames)))
for i, f in enumerate(frames):
    strip.paste(f, (0, 16 * i))
strip.save(OUT_B + 'runic_conjurer_column_on.png')
open(OUT_B + 'runic_conjurer_column_on.png.mcmeta', 'w').write('{"animation":{"frametime":6,"interpolate":true}}\n')

# tampo: círculo rúnico (apagado / aceso)
def top(glow):
    im = Image.open(VAN + 'polished_blackstone_bricks.png').convert('RGBA')
    import math
    for a in range(0, 360, 8):
        r = 5.2
        x = round(7.5 + r * math.cos(math.radians(a))); y = round(7.5 + r * math.sin(math.radians(a)))
        im.putpixel((x, y), (glow + (255,)) if glow else (24, 20, 30, 255))
    for (x, y) in ((7, 5), (8, 5), (5, 7), (5, 8), (10, 7), (10, 8), (7, 10), (8, 10), (7, 7), (8, 8), (7, 8), (8, 7)):
        im.putpixel((x, y), (glow + (255,)) if glow else (40, 34, 48, 255))
    return im
top(None).save(OUT_B + 'runic_conjurer_top.png')
tf = [top(lerp((92, 40, 150), (214, 160, 255), g)) for g in (0.55, 0.8, 1.0, 0.8)]
strip = Image.new('RGBA', (16, 64))
for i, f in enumerate(tf):
    strip.paste(f, (0, 16 * i))
strip.save(OUT_B + 'runic_conjurer_top_on.png')
open(OUT_B + 'runic_conjurer_top_on.png.mcmeta', 'w').write('{"animation":{"frametime":6,"interpolate":true}}\n')
print('ok bloco')
