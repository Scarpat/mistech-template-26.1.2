"""Texturas do Elevador Rúnico, Narguilé Rúnico, cachaça (garapa, barril, alambique) e Infusor Arcano.
Uso (raiz do projeto): python tools/utility/gen_textures.py [pasta_texturas_vanilla]
Usa do vanilla: block/polished_blackstone, barrel_*, copper_block, cut_copper, enchanting_table_*, obsidian, crying_obsidian."""
import colorsys, math, random, sys
from PIL import Image

VAN = (sys.argv[1] if len(sys.argv) > 1 else 'vanilla/assets/minecraft/textures').rstrip('/') + '/'
T = 'src/main/resources/assets/mistech/textures/'


def van(name):
    return Image.open(VAN + name + '.png').convert('RGBA').crop((0, 0, 16, 16))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def remap(im, cols, contrast=1.0):
    """Mapeia a luminância da imagem num degradê de cores."""
    im = im.copy()
    px = im.load()
    ls = [lum(px[x, y]) for y in range(im.size[1]) for x in range(im.size[0]) if px[x, y][3]]
    a, b = min(ls), max(ls)
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            c = px[x, y]
            if not c[3]:
                continue
            t = 0.5 + ((lum(c) - a) / ((b - a) or 1) - 0.5) * contrast
            t = max(0.0, min(1.0, t))
            f = t * (len(cols) - 1)
            i = min(int(f), len(cols) - 2)
            r = f - i
            px[x, y] = tuple(round(cols[i][k] * (1 - r) + cols[i + 1][k] * r) for k in range(3)) + (c[3],)
    return im


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c[:3]) + ((c[3],) if len(c) > 3 else (255,))


def new(w=16, h=16):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


# =================================================================== Elevador Rúnico
base = remap(van('block/polished_blackstone'), [hexc('15131a'), hexc('2a2633'), hexc('3d3848'), hexc('524b60')], 0.8)
px = base.load()
for i in range(16):  # moldura entalhada
    for (x, y) in ((i, 0), (0, i)):
        px[x, y] = shade(px[x, y], 1.35)
    for (x, y) in ((i, 15), (15, i)):
        px[x, y] = shade(px[x, y], 0.55)
for i in range(2, 14):  # sulco interno
    for (x, y) in ((i, 2), (2, i), (i, 13), (13, i)):
        px[x, y] = shade(px[x, y], 0.6)
base.save(T + 'block/runic_elevator.png')

# glifo (branco/cinza, tingido pela cor): setas para cima e para baixo dentro de um anel
glyph = new()
gp = glyph.load()
for y in range(16):
    for x in range(16):
        d = math.hypot(x - 7.5, y - 7.5)
        if 4.6 <= d <= 5.6:
            gp[x, y] = (230, 230, 230, 255)
UP = [(7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (7, 6), (8, 6)]
for (x, y) in UP:
    gp[x, y] = (255, 255, 255, 255)
    gp[x, 15 - y] = (255, 255, 255, 255)
for y in (7, 8):
    gp[7, y] = gp[8, y] = (200, 200, 200, 255)
for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
    gp[x, y] = (180, 180, 180, 255)
glyph.save(T + 'block/runic_elevator_glyph.png')
side = new()
sp = side.load()
for x in range(4, 12):  # faixa rúnica na lateral
    sp[x, 7] = (200, 200, 200, 255)
for x in (4, 7, 8, 11):
    sp[x, 6] = sp[x, 8] = (240, 240, 240, 255)
side.save(T + 'block/runic_elevator_glyph_side.png')

# =================================================================== Narguilé Rúnico
GLASS = [hexc('2a1240'), hexc('4c2380'), hexc('7a44c0'), hexc('b48cf0')]
BRASS = [hexc('5a3a12'), hexc('9a6c22'), hexc('d8a640'), hexc('f8e090')]
HOSE = [hexc('0e2a2a'), hexc('1c4c48'), hexc('2e7268')]
CLAY = [hexc('3a1e14'), hexc('6a3420'), hexc('a05a34')]
atlas = new()
ap = atlas.load()
rnd = random.Random(11)
for y in range(8):
    for x in range(8):  # vidro roxo com reflexo e runas
        t = 0.35 + 0.4 * (1 - abs(x - 3.5) / 4) + rnd.uniform(-0.06, 0.06)
        c = GLASS[min(3, int(t * 3.2))]
        ap[x, y] = c + (235,)
    for x in range(8, 16):  # latão com faixas
        c = BRASS[(1 + (x % 3 == 0) + (y % 4 == 0)) % 4]
        ap[x, y] = c + (255,)
for y in range(1, 7):
    ap[1, y] = hexc('e0ccff') + (255,)
for (x, y) in ((4, 2), (5, 3), (4, 4), (3, 5), (5, 5)):
    ap[x, y] = hexc('f0d070') + (255,)
for y in range(8, 16):
    for x in range(8):  # barro do fornilho
        ap[x, y] = CLAY[(x + y) % 3] + (255,)
    for x in range(8, 16):  # mangueira listrada
        ap[x, y] = HOSE[(x + 2 * y) % 3] + (255,)
atlas.save(T + 'block/runic_hookah.png')
for lit in (False, True):  # brasa em cima do fornilho
    im = new()
    p = im.load()
    for y in range(16):
        for x in range(16):
            if lit:
                v = rnd.random()
                p[x, y] = (hexc('ffe070') if v > 0.8 else hexc('ff8a20') if v > 0.45 else hexc('b02a10')) + (255,)
            else:
                v = rnd.random()
                p[x, y] = (hexc('5a5450') if v > 0.6 else hexc('3a3634') if v > 0.25 else hexc('222020')) + (255,)
    im.save(T + 'block/runic_hookah_ember%s.png' % ('_lit' if lit else ''))
# ícone
icon = new()
ip = icon.load()


def put(x, y, c, a=255):
    if 0 <= x < 16 and 0 <= y < 16:
        ip[x, y] = c + (a,)


for y in range(9, 15):  # vaso
    half = [2, 3, 3, 3, 3, 2][y - 9]
    for x in range(8 - half, 8 + half):
        t = 1 - abs(x - 7.5) / (half + 0.5)
        put(x, y, GLASS[1 + int(t * 2.4)])
put(6, 10, hexc('e0ccff'))
put(6, 11, hexc('e0ccff'))
put(8, 12, hexc('f0d070'))
for x in range(5, 11):
    put(x, 15, GLASS[0])
for y in range(4, 9):  # haste de latão
    put(7, y, BRASS[2])
    put(8, y, BRASS[1])
for x in range(5, 11):  # bandeja
    put(x, 6, BRASS[3] if x in (5, 10) else BRASS[2])
for y in range(2, 5):  # fornilho
    for x in range(6, 10):
        put(x, y, CLAY[1 + (x == 6)])
for x in range(6, 10):
    put(x, 1, hexc('ff8a20') if x % 2 else hexc('ffe070'))
for (x, y) in ((4, 9), (3, 10), (2, 11), (2, 12), (3, 13), (11, 9), (12, 10), (13, 11), (13, 12), (12, 13)):
    put(x, y, HOSE[2] if y % 2 else HOSE[1])
icon.save(T + 'item/runic_hookah.png')

# =================================================================== garapa e cachaças
BOTTLE_GLASS = (200, 225, 235)


def potion_style(liquid, name, bubbles=False, foam=None):
    """Garrafa de vidro (formato da poção) com líquido da cor dada."""
    bottle = Image.open(VAN + 'item/potion.png').convert('RGBA')
    over = Image.open(VAN + 'item/potion_overlay.png').convert('RGBA')
    out = bottle.copy()
    op, vp = out.load(), over.load()
    for y in range(16):
        for x in range(16):
            if vp[x, y][3]:
                g = vp[x, y][0] / 255
                op[x, y] = tuple(round(liquid[k] * (0.55 + 0.6 * g)) for k in range(3)) + (255,)
                op[x, y] = tuple(min(255, v) for v in op[x, y][:3]) + (255,)
    if bubbles:
        for (x, y) in ((6, 10), (9, 12), (7, 13), (10, 9)):
            if vp[x, y][3]:
                op[x, y] = (255, 250, 220, 255)
    if foam:
        for x in range(16):
            for y in range(16):
                if vp[x, y][3]:
                    op[x, y] = foam + (255,)
                    break
    out.save(T + 'item/%s.png' % name)


potion_style(hexc('b8d04a'), 'garapa', foam=hexc('e8f0a0'))
potion_style(hexc('d8c060'), 'fermented_garapa', bubbles=True, foam=hexc('f4ecc0'))


def cachaca(name, liquid, label, extra=None):
    """Garrafa alta de cachaça com rolha e rótulo."""
    im = new()
    p = im.load()
    outline = (40, 52, 60)
    for y in range(1, 3):  # rolha
        for x in range(7, 9):
            p[x, y] = hexc('a0703c') + (255,) if x == 7 else hexc('7a5028') + (255,)
    for y in range(3, 6):  # gargalo
        p[6, y] = outline + (255,)
        p[9, y] = outline + (255,)
        p[7, y] = shade(BOTTLE_GLASS + (255,), 0.95)
        p[8, y] = liquid + (255,) if y >= 5 else shade(BOTTLE_GLASS + (255,), 0.8)
    for y in range(6, 15):  # corpo
        w = 2 if y == 6 else 3
        for x in range(8 - w - 1, 8 + w + 1):
            if x in (8 - w - 1, 8 + w):
                p[x, y] = outline + (255,)
            else:
                t = (x - (8 - w)) / (2 * w)
                p[x, y] = shade(liquid + (255,), 1.15 - 0.35 * t)
    for x in range(4, 12):
        p[x, 15] = outline + (255,)
    for y in range(7, 13):  # brilho do vidro
        p[5, y] = (235, 248, 255, 255)
    for y in range(9, 12):  # rótulo
        for x in range(6, 11):
            p[x, y] = label + (255,)
    for x in range(6, 11):
        p[x, 9] = shade(label + (255,), 0.8)
    p[8, 10] = (60, 30, 20, 255)
    if extra:
        extra(p)
    im.save(T + 'item/%s.png' % name)


def rue_leaf(p):
    for (x, y) in ((7, 13), (8, 12), (9, 13), (8, 13)):
        p[x, y] = hexc('4a9a3a') + (255,)


def honey(p):
    for x in range(5, 11):
        p[x, 14] = hexc('f0a020') + (255,)
        p[x, 13] = hexc('f8c040') + (255,)


cachaca('cachaca', hexc('dceef0'), hexc('f0e6c8'))
cachaca('aged_cachaca', hexc('d88a2a'), hexc('e8d29a'))
cachaca('rue_cachaca', hexc('cfe6c4'), hexc('d8ecc8'), rue_leaf)
cachaca('honey_cachaca', hexc('f0c060'), hexc('fff0c0'), honey)

# =================================================================== Barril de Fermentação (madeira turva)
MURKY = [hexc('121116'), hexc('29272f'), hexc('3e3b47'), hexc('5a5364'), hexc('7a6a58')]
for part in ('top', 'side', 'bottom'):
    im = remap(van('block/barrel_' + part), MURKY, 0.95)
    if part == 'side':  # cintas de cobre envelhecido
        p = im.load()
        for x in range(16):
            for y in (2, 13):
                p[x, y] = (hexc('8a5a3a') if x % 4 else hexc('b88050')) + (255,)
    if part == 'top':  # torneirinha e marca rúnica
        p = im.load()
        for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
            p[x, y] = hexc('b88050') + (255,)
        for (x, y) in ((4, 4), (11, 4), (4, 11), (11, 11)):
            p[x, y] = hexc('9a7ac8') + (255,)
    im.save(T + 'block/fermentation_barrel_%s.png' % part)

# =================================================================== Alambique (cobre)
COPPER = [hexc('5a2a18'), hexc('9a4a2a'), hexc('c86a3a'), hexc('e89a62'), hexc('f8c49a')]
side = remap(van('block/cut_copper'), COPPER, 0.9)
side.save(T + 'block/cane_still_side.png')
top = remap(van('block/copper_block'), COPPER, 0.9)
p = top.load()
for y in range(16):  # boca do capelo
    for x in range(16):
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 2.6:
            p[x, y] = hexc('1a0c08') + (255,)
        elif d < 3.6:
            p[x, y] = hexc('f8c49a') + (255,)
top.save(T + 'block/cane_still_top.png')
remap(van('block/copper_block'), COPPER, 0.6).save(T + 'block/cane_still_bottom.png')
for lit in (False, True):
    im = remap(van('block/copper_block'), COPPER, 0.9)
    p = im.load()
    for y in range(16):  # visor redondo
        for x in range(16):
            d = math.hypot(x - 7.5, y - 6.5)
            if d < 3.2:
                if lit:
                    v = 1 - d / 3.2
                    p[x, y] = (255, round(160 + 80 * v), round(60 + 100 * v), 255)
                else:
                    p[x, y] = (hexc('2a1810') if (x + y) % 3 else hexc('3a2418')) + (255,)
            elif d < 4.2:
                p[x, y] = hexc('5a2a18') + (255,)
    for y in range(11, 14):  # bica
        p[7, y] = hexc('f8c49a') + (255,)
        p[8, y] = hexc('9a4a2a') + (255,)
    p[7, 14] = (hexc('fff0c0') if lit else hexc('9a4a2a')) + (255,)
    for x in range(2, 14):  # rebites
        if x % 3 == 2:
            p[x, 1] = p[x, 14] = hexc('f8c49a') + (255,)
    im.save(T + 'block/cane_still_front%s.png' % ('_on' if lit else ''))

# =================================================================== Infusor Arcano (3 níveis)
TIERS = {
    'arcane_infuser': ([hexc('0c0814'), hexc('241838'), hexc('3e2a5e'), hexc('5c4290')], hexc('b78cf0'), hexc('8a5cc8')),
    'greater_arcane_infuser': ([hexc('120614'), hexc('2e0e3a'), hexc('561a6a'), hexc('8a3aa0')], hexc('f0c860'), hexc('d070f0')),
    'astral_arcane_infuser': ([hexc('060a18'), hexc('101c3c'), hexc('1c3268'), hexc('3a5aa0')], hexc('e8f4ff'), hexc('70c8ff')),
}
for name, (cols, trim, gem) in TIERS.items():
    for part in ('top', 'side', 'bottom'):
        im = remap(van('block/enchanting_table_' + part), cols, 0.9)
        p = im.load()
        if part == 'top':
            for y in range(16):  # círculo de infusão
                for x in range(16):
                    d = math.hypot(x - 7.5, y - 7.5)
                    if 5.2 <= d <= 6.2:
                        p[x, y] = trim + (255,)
                    elif d < 1.6:
                        p[x, y] = gem + (255,)
            for a in range(6):
                ang = a * math.pi / 3
                p[round(7.5 + math.cos(ang) * 3.6), round(7.5 + math.sin(ang) * 3.6)] = gem + (255,)
            if name == 'astral_arcane_infuser':
                for (x, y) in ((2, 2), (13, 3), (3, 13), (12, 12), (8, 1)):
                    p[x, y] = (255, 255, 255, 255)
        elif part == 'side':
            for x in range(16):
                p[x, 4] = trim + (255,) if x % 2 == 0 else shade(trim + (255,), 0.7)
            for x in (3, 7, 8, 12):
                p[x, 8] = gem + (255,)
                p[x, 9] = shade(gem + (255,), 0.7)
        im.save(T + 'block/%s_%s.png' % (name, part))
    crystal = new()
    cp = crystal.load()
    for y in range(16):
        for x in range(16):
            t = 0.6 + 0.4 * ((x + y) % 5 == 0) - 0.25 * (x > 8)
            cp[x, y] = shade(gem + (255,), t)
    crystal.save(T + 'block/%s_crystal.png' % name)

# =================================================================== telas
SRC = T + 'gui/container/'
anvil = Image.open(SRC + 'rune_anvil.png').convert('RGBA')
crusher = Image.open(SRC + 'runic_crusher.png').convert('RGBA')
W, H = 176, 206
gui = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
g = gui.load()
s = anvil.load()
for y in range(H - 88):
    for x in range(W):
        if y < 7 or x < 7 or x >= W - 7:
            sy = y if y < 7 else 7 + (y - 7) % 60  # bordas: repete a faixa da borda
            g[x, y] = s[x, sy]
        else:
            band = (y - 7) // 11
            sx = 7 + (x - 7 + 37 * band) % 162
            g[x, y] = s[sx, 7 + (y - 7) % 11]
bottom = anvil.crop((0, 78, 176, 166))
gui.paste(bottom, (0, H - 88))
slot = crusher.crop((39, 24, 57, 42))
for (x, y) in ((17, 22), (8, 48), (26, 48), (8, 70), (26, 70)):
    gui.paste(slot, (x - 1, y - 1))
for i in range(-2, 20):  # contorno lilás no espaço do item
    for (x, y) in ((16 + i, 19), (16 + i, 40), (14, 21 + i), (35, 21 + i)):
        if 19 <= y <= 40 and 14 <= x <= 35:
            g[x, y] = (183, 140, 240, 255)


def well(x0, y0, x1, y1):
    for y in range(y0, y1):
        for x in range(x0, x1):
            edge_d = y == y0 or x == x0
            edge_l = y == y1 - 1 or x == x1 - 1
            g[x, y] = (6, 6, 8, 255) if edge_d else (70, 64, 82, 255) if edge_l else (16, 14, 20, 255)


well(47, 17, 55, 87)    # energia
well(59, 17, 171, 79)   # lista de encantos
gui.save(SRC + 'arcane_infuser.png')
crusher.save(SRC + 'cane_still.png')
print('ok')
