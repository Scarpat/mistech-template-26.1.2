"""Texturas do Tecido Mágico, Tear Mágico e Pêndulo Localizador.
Uso (raiz do projeto): python tools/mage/gen_textures.py [pasta_texturas_vanilla]
Usa do vanilla: entity/equipment/humanoid(_leggings)/leather.png, item/leather_*.png, block/loom_*.png."""
import colorsys, sys
from PIL import Image

VAN = (sys.argv[1] if len(sys.argv) > 1 else 'vanilla/assets/minecraft/textures').rstrip('/') + '/'
T = 'src/main/resources/assets/mistech/textures/'

TRIM = {'initiate': ((0xE8, 0xC0, 0x4A), None), 'adept': ((0xD8, 0xD8, 0xF0), (0xB0, 0x5C, 0xFF)),
        'archmage': ((0xFF, 0xB0, 0x4A), (0xFF, 0xF4, 0xC0))}


def edge_overlay(base, color, accent=None, seed=0):
    """Contorno das partes (pixels opacos com vizinho transparente) + pontinhos de destaque."""
    w, h = base.size
    bp = base.load()
    out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    op = out.load()
    for y in range(h):
        for x in range(w):
            if not bp[x, y][3]:
                continue
            edge = any(not (0 <= x + dx < w and 0 <= y + dy < h) or not bp[x + dx, y + dy][3]
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                op[x, y] = color + (255,)
            elif accent and (x * 7 + y * 13 + seed) % 23 == 0:
                op[x, y] = accent + (255,)
    return out


# (as Vestes de Mago — armadura, partes 3D e ícones — ficam em tools/mage/robe_design.py)

# ---------- Tecido Mágico (desenhado) ----------
im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
px = im.load()
for y in range(3, 13):
    for x in range(2, 14):
        shade = 0.85 + 0.15 * ((x + y) % 3 == 0) - 0.12 * (y > 10)
        r, g, b = colorsys.hsv_to_rgb(0.78, 0.45, 0.75 * shade + 0.1)
        px[x, y] = (int(r * 255), int(g * 255), int(b * 255), 255)
for x in range(2, 14):
    px[x, 3] = (0xE8, 0xC0, 0x4A, 255)  # costura de ouro
    if x % 2 == 0:
        px[x, 12] = (0xE8, 0xC0, 0x4A, 255)
for y in range(4, 12):
    px[13, y] = tuple(max(0, c - 40) for c in px[13, y][:3]) + (255,)
for (x, y) in ((5, 6), (9, 8), (7, 10)):
    px[x, y] = (0xF4, 0xE8, 0xFF, 255)
im.save(T + 'item/magic_cloth.png')

# ---------- Tear Mágico: madeira escura arroxeada, fios lilás ----------
def loom(name):
    im = Image.open(VAN + 'block/loom_%s.png' % name).convert('RGBA')
    px = im.load()
    for y in range(16):
        for x in range(16):
            c = px[x, y]
            h, s, v = colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)
            if s < 0.12 and v > 0.45:  # fios
                r, g, b = colorsys.hsv_to_rgb(0.80, 0.35, min(1.0, v * 1.1))
            elif 0.0 <= h < 0.15 or h > 0.9:  # madeira
                r, g, b = colorsys.hsv_to_rgb(0.80, min(1.0, s * 0.6 + 0.15), v * 0.65)
            else:
                r, g, b = c[0] / 255, c[1] / 255, c[2] / 255
            px[x, y] = (int(r * 255), int(g * 255), int(b * 255), c[3])
    im.save(T + 'block/magic_loom_%s.png' % name)


for n in ('front', 'side', 'top', 'bottom'):
    loom(n)

# ---------- Pêndulo Localizador: o pêndulo de ametista em lilás; "achou" brilha ----------
src = Image.open(T + 'item/dowsing_pendulum_pure.png').convert('RGBA')
for name, bright in (('seeking_pendulum', 0.0), ('seeking_pendulum_found', 0.25)):
    im = src.copy()
    px = im.load()
    for y in range(16):
        for x in range(16):
            c = px[x, y]
            if not c[3]:
                continue
            h, s, v = colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)
            if y >= 8:  # cristal
                r, g, b = colorsys.hsv_to_rgb(0.86, s * 0.7, min(1.0, v + bright))
            else:  # corrente: prata
                r, g, b = colorsys.hsv_to_rgb(0.65, 0.08, v)
            px[x, y] = (int(r * 255), int(g * 255), int(b * 255), c[3])
    im.save(T + 'item/%s.png' % name)
print('ok')
