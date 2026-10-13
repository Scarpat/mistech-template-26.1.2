"""Texturas da Armadura do Boitatá (vestida, ícones, partes 3D) e do Ovo do Boitatá.
Uso (raiz do projeto): python tools/boitata/gen_textures.py [pasta_texturas_vanilla]
Usa do vanilla: entity/equipment/humanoid(_leggings)/netherite.png e item/netherite_{helmet,chestplate,leggings,boots}.png."""
import math, random, sys
from PIL import Image

VAN = (sys.argv[1] if len(sys.argv) > 1 else 'vanilla/assets/minecraft/textures').rstrip('/') + '/'
T = 'src/main/resources/assets/mistech/textures/'


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


SCALES = [hexc('2a0806'), hexc('5e140a'), hexc('9a2a10'), hexc('d0581c'), hexc('f09030'), hexc('ffd060')]
GOLD = hexc('ffd060')
IRIS = hexc('ffe14a')


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def grad(t):
    t = max(0.0, min(1.0, t))
    f = t * (len(SCALES) - 1)
    i = min(int(f), len(SCALES) - 2)
    r = f - i
    return tuple(round(SCALES[i][k] * (1 - r) + SCALES[i + 1][k] * r) for k in range(3))


def scaled(im, pattern=True, contrast=0.9):
    """Pinta pela luminância no degradê de escamas e desenha arcos de escama."""
    im = im.convert('RGBA')
    px = im.load()
    w, h = im.size
    ls = [lum(px[x, y]) for y in range(h) for x in range(w) if px[x, y][3]]
    a, b = min(ls), max(ls)
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            if not c[3]:
                continue
            t = 0.5 + ((lum(c) - a) / ((b - a) or 1) - 0.5) * contrast
            if pattern:
                # escamas: arcos em fileiras desencontradas (claro em cima, sombra embaixo)
                row = y // 2
                off = (row % 2) * 2
                local = (x + off) % 4
                if y % 2 == 0 and local in (1, 2):
                    t += 0.12
                elif y % 2 == 1 and local in (0, 3):
                    t -= 0.12
            px[x, y] = grad(t) + (c[3],)
    return im


# ---------- armadura vestida ----------
hum = scaled(Image.open(VAN + 'entity/equipment/humanoid/netherite.png'))
p = hum.load()
# moldura de ouro em volta do Olho (frente do peito: u 20..27, v 20..31; o olho fica em v 23..26)
for (x, y) in [(x, 22) for x in range(21, 27)] + [(x, 27) for x in range(21, 27)] + [(21, y) for y in range(22, 28)] + [(26, y) for y in range(22, 28)]:
    if p[x, y][3]:
        p[x, y] = GOLD + (255,)
for y in range(23, 27):
    for x in range(22, 26):
        p[x, y] = hexc('1a0402') + (255,)
hum.save(T + 'entity/equipment/humanoid/boitata.png')
scaled(Image.open(VAN + 'entity/equipment/humanoid_leggings/netherite.png')).save(T + 'entity/equipment/humanoid_leggings/boitata.png')

# ---------- ícones ----------
for piece in ('helmet', 'chestplate', 'leggings', 'boots'):
    icon = scaled(Image.open(VAN + 'item/netherite_%s.png' % piece), pattern=True, contrast=1.0)
    ip = icon.load()
    if piece == 'chestplate':
        # o Olho no centro do peito
        for (x, y) in ((7, 5), (8, 5), (6, 6), (9, 6), (6, 7), (9, 7), (7, 8), (8, 8)):
            ip[x, y] = GOLD + (255,)
        for (x, y) in ((7, 6), (8, 6), (7, 7), (8, 7)):
            ip[x, y] = IRIS + (255,)
        ip[7, 6] = ip[7, 7] = (20, 6, 4, 255)
    if piece == 'helmet':
        for (x, y) in ((7, 1), (8, 0), (8, 1), (9, 1)):
            ip[x, y] = hexc('ff7a20') + (255,)
    icon.save(T + 'item/boitata_%s.png' % piece)

# ---------- partes 3D (64x32) ----------
wear = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
wp = wear.load()


def fill_box(u, v, w, h, d, color_fn):
    """Preenche a área de UV de uma caixa (w, h, d) a partir de (u, v)."""
    for y in range(v, v + d + h):
        for x in range(u, u + 2 * (d + w)):
            if y < v + d and not (u + d <= x < u + d + 2 * w):
                continue
            wp[x, y] = color_fn(x - u, y - v, d + h) + (255,)


def flame(x, y, H):
    t = 1.0 - y / max(1, H - 1)  # topo mais claro
    t = 0.35 + 0.65 * t + (0.08 if (x + y) % 3 == 0 else 0)
    return grad(t)


fill_box(0, 0, 2, 5, 3, flame)
fill_box(10, 0, 2, 7, 3, flame)
fill_box(20, 0, 2, 5, 3, flame)
fill_box(30, 0, 1, 3, 2, lambda x, y, H: grad(0.25 + 0.5 * (1 - y / H)))
fill_box(0, 12, 4, 4, 1, lambda x, y, H: GOLD)
# olho: frente da caixa (u 1..4, v 13..16)
eye = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
ep = eye.load()
EYE = ["oyyo", "ykky", "ykky", "oyyo"]  # frente da caixa (u 1..4, v 13..16): íris amarela, pupila em fenda
COL = {'o': hexc('ff8a20'), 'y': IRIS, 'k': (20, 6, 4)}
for j, row in enumerate(EYE):
    for i, ch in enumerate(row):
        wp[1 + i, 13 + j] = COL[ch] + (255,)
        if ch != 'k':
            ep[1 + i, 13 + j] = COL[ch] + (255,)
wear.save(T + 'entity/boitata_wear.png')
eye.save(T + 'entity/boitata_wear_eye.png')

# ---------- Ovo do Boitatá ----------
rnd = random.Random(7)
shell = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
sp = shell.load()
for y in range(16):
    for x in range(16):
        base = 0.62 - 0.30 * (y / 15)  # mais claro em cima
        r = y // 3
        lx, ly = (x + (r % 2) * 2) % 4, y % 3
        if ly == 0 and lx in (1, 2):
            base += 0.20  # borda clara da escama
        elif ly == 2 and lx in (0, 3):
            base -= 0.16  # sombra embaixo da escama
        sp[x, y] = grad(base + rnd.uniform(-0.03, 0.03)) + (255,)
# rachaduras com fogo por dentro
for (x, y) in ((3, 9), (4, 10), (4, 11), (5, 12), (11, 4), (12, 5), (12, 6), (13, 7), (8, 13), (9, 14)):
    sp[x, y] = hexc('ffe070') + (255,)
shell.save(T + 'block/boitata_egg.png')
icon = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
ip = icon.load()
for y in range(16):
    for x in range(16):
        dx, dy = (x - 7.5) / 5.2, (y - 8.5) / 6.8
        if dy < 0:
            dx *= 1.15
        if dx * dx + dy * dy <= 1.0:
            light = -0.3 * (x - 7.5) / 5 - 0.25 * (y - 8.5) / 7
            ip[x, y] = grad(0.5 + light + (0.1 if (x + (y // 2) * 2) % 4 == 1 and y % 2 == 0 else 0)) + (255,)
for (x, y) in ((5, 6), (6, 7), (6, 8), (9, 10), (10, 11)):
    ip[x, y] = hexc('ffe070') + (255,)
for y in range(16):  # contorno
    for x in range(16):
        if ip[x, y][3] and any(not (0 <= x + a < 16 and 0 <= y + b < 16) or not ip[x + a, y + b][3]
                               for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            ip[x, y] = hexc('2a0806') + (255,)
icon.save(T + 'item/boitata_egg.png')
print('ok')

# ---------- armaduras de montaria e apito (v2, 12/10) ----------
def recolor(src, cols, accent=None):
    im = Image.open(VAN + src).convert('RGBA')
    p = im.load()
    ls = [lum(p[x, y]) for y in range(16) for x in range(16) if p[x, y][3]]
    a, b = min(ls), max(ls)
    for y in range(16):
        for x in range(16):
            c = p[x, y]
            if c[3]:
                t = (lum(c) - a) / ((b - a) or 1)
                f = t * (len(cols) - 1)
                i = min(int(f), len(cols) - 2)
                r = f - i
                p[x, y] = tuple(round(cols[i][k] * (1 - r) + cols[i + 1][k] * r) for k in range(3)) + (255,)
    return im


arm = recolor('item/netherite_horse_armor.png', [hexc('120a10'), hexc('2a1c26'), hexc('4a3240'), hexc('d8a640'), hexc('ffe090')])
ap = arm.load()
for (x, y) in ((7, 6), (8, 6), (7, 7), (8, 7)):
    if ap[x, y][3]:
        ap[x, y] = hexc('ff6a20') + (255,)
arm.save(T + 'item/boitata_mount_armor_scale.png')
ori = recolor('item/netherite_horse_armor.png', [hexc('5a3410'), hexc('a86a20'), hexc('e0a040'), hexc('ffd070'), hexc('fff4c0')])
op = ori.load()
for (x, y) in ((7, 6), (8, 6), (7, 7), (8, 7)):
    if op[x, y][3]:
        op[x, y] = hexc('40e0d0') + (255,)
ori.save(T + 'item/boitata_mount_armor_orichalcum.png')

wh = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
whp = wh.load()
# apito de osso dourado com uma escama na ponta e cordão
for i in range(9):
    x, y = 3 + i, 11 - i
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        whp[x + dx, y + dy] = (hexc('fff0c8') if (i + dx) % 3 else hexc('e0c890')) + (255,)
for (x, y) in ((4, 11), (5, 10)):
    whp[x, y] = hexc('2a1810') + (255,)  # bocal
for (x, y) in ((7, 7), (8, 6)):
    whp[x, y] = hexc('3a2010') + (255,)  # furo
for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
    whp[11 + dx, 1 + dy] = hexc('e2561a') + (255,)
whp[12, 2] = hexc('ffd060') + (255,)
for (x, y) in ((2, 13), (2, 14), (3, 14), (4, 15)):
    whp[x, y] = hexc('8a2a1a') + (255,)
wh.save(T + 'item/boitata_whistle.png')
print('ok v2')
