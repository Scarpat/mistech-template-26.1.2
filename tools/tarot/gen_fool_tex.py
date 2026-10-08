"""Texturas do Louco: chefe (128x64 + brilho), tornado, Chapéu do Bobo (vestido e ícone), Cajado e ovo.
Uso: python gen_fool_tex.py <assets/mistech/textures>"""
import sys, os, math
from PIL import Image

T = sys.argv[1]
def rgb(h, a=255): return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)
def mix(a, b, t): return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (a[3] if len(a) > 3 else 255,)
def shade(c, f): return tuple(max(0, min(255, int(c[i] * f))) for i in range(3)) + (c[3],)

SKIN = rgb(0xA9DCF2); SKIN_D = rgb(0x7FB9D8)
YEL = rgb(0xF2D16B); YEL_D = rgb(0xD1A93C)
WHITE = rgb(0xF6F3EA); WHITE_D = rgb(0xD8DDE6)
GOLD = rgb(0xF5C842); GOLD_D = rgb(0xB8862A)
WOOD = rgb(0x7A5232); WOOD_D = rgb(0x5A3A22)
NAVY = rgb(0x1E2E5A)
WIND1 = rgb(0xF4FBFF); WIND2 = rgb(0x9FD8FF)

class Tex:
    def __init__(s, w, h):
        s.im = Image.new('RGBA', (w, h), (0, 0, 0, 0)); s.p = s.im.load()
    def rect(s, x, y, w, h, fn):
        for j in range(h):
            for i in range(w):
                c = fn(i, j, w, h)
                if c: s.p[x + i, y + j] = c
    def box(s, u, v, w, h, d, side, top=None, bottom=None, front=None, back=None):
        """Pinta um cubo no layout padrão. side/top/... = função (i, j, w, h) -> cor."""
        top = top or side; bottom = bottom or side; front = front or side; back = back or side
        s.rect(u + d, v, w, d, top)
        s.rect(u + d + w, v, w, d, bottom)
        s.rect(u, v + d, d, h, side)
        s.rect(u + d, v + d, w, h, front)
        s.rect(u + d + w, v + d, d, h, side)
        s.rect(u + 2 * d + w, v + d, w, h, back)

def solid(c, edge=None):
    def f(i, j, w, h):
        if edge and (j == h - 1): return edge
        return c
    return f

def noisy(c, amt=0.06, seed=0):
    def f(i, j, w, h):
        n = ((i * 73 + j * 151 + seed * 37) % 17) / 16.0
        return shade(c, 1.0 - amt + n * amt * 2)
    return f

def harlequin(a, b, size=3):
    def f(i, j, w, h):
        k = ((i + j) // size + (i - j + 64) // size) % 2
        return a if k == 0 else b
    return f

def stripes(a, b, period=2):
    def f(i, j, w, h):
        return a if (j // period) % 2 == 0 else b
    return f

def wind(i, j, w, h):
    t = 0.5 + 0.5 * math.sin((i + j * 0.8) * 1.3)
    return mix(WIND1, WIND2, t * 0.7)

# ----------------------------------------------------------- chefe
boss = Tex(128, 64)
def face(i, j, w, h):
    c = noisy(SKIN, 0.04, 1)(i, j, w, h)
    if j in (2,) and i in (1, 2, 5, 6): c = NAVY                     # sobrancelhas marotas
    if j == 3 and i in (1, 2, 5, 6): c = WHITE
    if j == 4 and i in (1, 6): c = WHITE
    if j == 4 and i in (2, 5): c = NAVY                              # pupilas
    if j == 6 and i in (1, 6): c = NAVY                              # cantos do sorriso
    if j == 7 and 2 <= i <= 5: c = NAVY
    if j == 6 and 2 <= i <= 5: c = WHITE                             # dentes
    return c
def head_side(i, j, w, h):
    c = noisy(SKIN, 0.05, 2)(i, j, w, h)
    if j >= 6: c = shade(c, 0.9)
    return c
boss.box(0, 0, 8, 8, 8, head_side, top=noisy(SKIN, 0.05, 3), bottom=solid(SKIN_D), front=face)
# faixa do chapéu
boss.box(32, 0, 9, 2, 9, harlequin(GOLD, GOLD_D, 2), top=solid(YEL), bottom=solid(GOLD_D))
# pontas e guizos
boss.box(0, 16, 3, 5, 3, stripes(YEL, YEL_D, 2), top=solid(YEL))
boss.box(12, 16, 2, 4, 2, stripes(WHITE, WHITE_D, 2))
boss.box(20, 16, 2, 2, 2, solid(GOLD), bottom=solid(GOLD_D))
# tronco: túnica de arlequim
def torso_front(i, j, w, h):
    c = harlequin(YEL, WHITE, 3)(i, j, w, h)
    if i in (3, 4) and j % 3 == 1: c = GOLD                          # botões
    return c
boss.box(0, 26, 8, 12, 5, harlequin(YEL_D, WHITE_D, 3), top=solid(WHITE), front=torso_front, back=harlequin(YEL, WHITE, 3))
# faixa da cintura
def belt(i, j, w, h):
    return GOLD if 3 <= i <= 5 else rgb(0x6B4426)
boss.box(72, 0, 9, 2, 6, solid(rgb(0x6B4426)), front=belt)
# gola de babados
def ruff(i, j, w, h):
    return WHITE if (i + j) % 2 == 0 else WHITE_D
boss.box(72, 8, 10, 2, 6, ruff)
# braços: manga listrada em cima, pele embaixo
def arm(i, j, w, h):
    if j < 5: return stripes(YEL, WHITE, 1)(i, j, w, h)
    return noisy(SKIN, 0.04, 5)(i, j, w, h)
boss.box(28, 26, 3, 11, 3, arm, top=solid(YEL), bottom=solid(SKIN_D))
# cajado: madeira com fita dourada em espiral; estrela dourada
def staff(i, j, w, h):
    return GOLD if j % 5 == 0 else (WOOD if j % 2 else WOOD_D)
boss.box(44, 26, 1, 24, 1, staff)
boss.box(48, 26, 3, 3, 3, solid(GOLD), front=lambda i, j, w, h: WHITE if (i, j) == (1, 1) else GOLD)
boss.box(48, 32, 5, 1, 1, solid(GOLD))
boss.box(48, 34, 1, 5, 1, solid(GOLD))
# cauda de vento
boss.box(0, 44, 7, 6, 5, wind)
boss.box(24, 44, 5, 5, 4, wind)
boss.box(44, 44, 3, 5, 3, wind)
boss.box(56, 44, 1, 4, 1, wind)
os.makedirs(os.path.join(T, 'entity'), exist_ok=True)
boss.im.save(os.path.join(T, 'entity/the_fool.png'))

# brilho: olhos e guizos
glow = Tex(128, 64)
def eyes(i, j, w, h):
    if j == 3 and i in (1, 2, 5, 6): return rgb(0xC8F4FF)
    if j == 4 and i in (2, 5): return rgb(0x6FE0FF)
    return None
glow.rect(8, 8, 8, 8, eyes)
glow.box(20, 16, 2, 2, 2, solid(rgb(0xFFE07A)))
glow.box(48, 26, 3, 3, 3, solid(rgb(0xFFF2B0)))
glow.im.save(os.path.join(T, 'entity/the_fool_glow.png'))

# ----------------------------------------------------------- tornado (64x32, translúcido)
tor = Image.new('RGBA', (64, 32)); tp = tor.load()
for y in range(32):
    for x in range(64):
        t = 0.5 + 0.5 * math.sin((x * 0.55 + y * 0.9))
        c = mix(WIND1, WIND2, t * 0.5)
        tp[x, y] = c[:3] + (int(110 + 120 * t),)
tor.save(os.path.join(T, 'entity/fool_tornado.png'))

# ----------------------------------------------------------- Chapéu do Bobo vestido (64x64)
cap = Tex(64, 64)
cap.box(0, 0, 9, 3, 9, harlequin(GOLD, GOLD_D, 2), top=solid(YEL), bottom=solid(GOLD_D))
cap.box(0, 14, 3, 5, 3, stripes(YEL, YEL_D, 2), top=solid(YEL))
cap.box(12, 14, 2, 4, 2, stripes(WHITE, WHITE_D, 2))
cap.box(20, 14, 2, 2, 2, solid(GOLD), bottom=solid(GOLD_D))
cap.im.save(os.path.join(T, 'entity/fool_cap.png'))

# ----------------------------------------------------------- ícones 16x16
def icon(rows, palette):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.': p[x, y] = palette[ch]
    return im

OUT = rgb(0x3A2A12)
icon([
    "................",
    "..g..........g..",
    ".ggo........ogg.",
    "..oyo......owo..",
    "...oyo....owwo..",
    "....oyo..owwo...",
    ".....oyyowwo....",
    ".....oyywwwo....",
    "....oyyywwwwo...",
    "...oyyyywwwwwo..",
    "...oGGGGGGGGGo..",
    "...oGdGdGdGdGo..",
    "....oooooooooo..",
    "................",
    "................",
    "................"], {'o': OUT, 'y': YEL, 'w': WHITE, 'g': GOLD, 'G': GOLD, 'd': GOLD_D}).save(os.path.join(T, 'item/fool_cap.png'))

icon([
    "...........g....",
    "..........ggg...",
    "........gggWggg.",
    "..........ggg...",
    "..........bg....",
    ".........b......",
    "........b.......",
    ".......B........",
    "......b.........",
    ".....b..........",
    "....B...........",
    "...b............",
    "..b.............",
    ".b..............",
    "................",
    "................"], {'g': GOLD, 'W': WHITE, 'b': WOOD, 'B': GOLD_D}).save(os.path.join(T, 'item/fool_staff.png'))

# ovo: mesmo formato do ovo de gnomo, recolorido (azul-céu com manchas amarelas)
egg = Image.open(os.path.join(T, 'item/gnome_spawn_egg.png')).convert('RGBA'); ep = egg.load()
for y in range(16):
    for x in range(16):
        c = ep[x, y]
        if c[3] == 0: continue
        lum = (c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11) / 255.0
        if c[0] > c[1] + 40:   # manchas (vermelhas no original)
            ep[x, y] = shade(YEL, 0.75 + lum * 0.4)
        else:
            ep[x, y] = shade(SKIN, 0.55 + lum * 0.5)
egg.save(os.path.join(T, 'item/the_fool_spawn_egg.png'))
print('ok')
