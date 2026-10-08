# Gera as texturas da Mula sem Cabeça (headless_mule.png e headless_mule_glow.png) a partir da mula do jogo.
# Uso: python3 gen_headless_mule_tex.py <mule.png vanilla> <pasta de saída>
# Layout (64x64) = mula vanilla + o que o HeadlessMuleModel usa:
#   pescoço cortado  texOffs(0,35) 4x10x7  (topo = o corte em brasa)
#   chamas do corte  texOffs(0,0)  plano 8x12 (frente 0..8, verso 8..16)
#   crina de fogo    texOffs(16,0) plano 0x10x4 (lados em (16,4) e (20,4))
import random, sys
from PIL import Image

src = Image.open(sys.argv[1]).convert('RGBA')
out_dir = sys.argv[2] if len(sys.argv) > 2 else '.'
W = H = 64
base = Image.new('RGBA', (W, H), (0, 0, 0, 0))
glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
B, G, S = base.load(), glow.load(), src.load()
rnd = random.Random(1307)  # fixo: a textura sai sempre igual


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


CHAR_DARK = (14, 9, 10, 255)
CHAR_LIGHT = (70, 52, 46, 255)
EMBER_DEEP = (110, 18, 8, 255)
EMBER = (214, 74, 14, 255)
EMBER_HOT = (255, 150, 40, 255)
CORE = (255, 236, 150, 255)


def lum(p):
    return (0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]) / 255.0


# ------------------------------------------------------------------ 1) pele carbonizada a partir da mula
CHEST = (26, 21, 48, 32)
for y in range(H):
    for x in range(W):
        p = S[x, y]
        if p[3] == 0:
            continue
        if CHEST[0] <= x < CHEST[2] and CHEST[1] <= y < CHEST[3]:
            B[x, y] = (int(p[0] * 0.7), int(p[1] * 0.62), int(p[2] * 0.6), 255)  # baú: só queimado
            continue
        L = lum(p)
        c = lerp(CHAR_DARK, CHAR_LIGHT, (L - 0.08) / 0.55)
        # leve variação para não ficar chapado
        k = rnd.randint(-5, 5)
        B[x, y] = (max(0, c[0] + k), max(0, c[1] + k), max(0, c[2] + k), 255)

# a cabeça, a boca, as orelhas e a crina antigas não existem mais: limpa (0..26, 12..35) e a crina (56..64, 36..54)
for y in range(12, 35):
    for x in range(0, 26):
        B[x, y] = (0, 0, 0, 0)
for y in range(36, 54):
    for x in range(56, 64):
        B[x, y] = (0, 0, 0, 0)


def ember(x, y, strength):
    """Pinta uma rachadura de brasa (strength 0..1) na base e na textura brilhante."""
    if not (0 <= x < W and 0 <= y < H) or B[x, y][3] == 0:
        return
    if CHEST[0] <= x < CHEST[2] and CHEST[1] <= y < CHEST[3]:
        return
    B[x, y] = lerp(EMBER_DEEP, EMBER_HOT, strength)
    G[x, y] = lerp((150, 30, 5, 255), (255, 190, 80, 255), strength)


def crack(x, y, length, rect):
    """Rachadura em zigue-zague dentro de rect (x0, y0, x1, y1)."""
    dx, dy = rnd.choice([(1, 0), (0, 1), (1, 1), (-1, 1)])
    for i in range(length):
        if not (rect[0] <= x < rect[2] and rect[1] <= y < rect[3]):
            return
        ember(x, y, 0.75 if i % 3 else 0.35)
        if rnd.random() < 0.45:
            dx, dy = rnd.choice([(1, 0), (0, 1), (1, 1), (-1, 1), (1, -1)])
        x += dx
        y += dy


# rachaduras: corpo (topo e laterais), pernas, cauda
BODY_TOP = (22, 32, 62, 54)
BODY_SIDES = (0, 54, 64, 64)
LEGS = (48, 25, 64, 33)
TAIL = (42, 40, 56, 50)
for rect, n, (lmin, lmax) in [(BODY_TOP, 9, (4, 9)), (BODY_SIDES, 7, (3, 7)), (LEGS, 4, (2, 5)), (TAIL, 3, (3, 6))]:
    for _ in range(n):
        crack(rnd.randrange(rect[0], rect[2]), rnd.randrange(rect[1], rect[3]), rnd.randint(lmin, lmax), rect)

# ------------------------------------------------------------------ 2) cascos de magma
for x in range(48, 64):
    for y in range(32, 36):
        if B[x, y][3]:
            t = (y - 32) / 3.0
            c = lerp(EMBER, CORE, t * 0.8) if (x + y) % 3 else lerp(EMBER_DEEP, EMBER_HOT, t)
            B[x, y] = c
            G[x, y] = lerp((180, 50, 10, 255), (255, 210, 110, 255), t)
for x in range(56, 60):  # sola
    for y in range(21, 25):
        B[x, y] = CORE if (x + y) % 2 else EMBER_HOT
        G[x, y] = (255, 200, 90, 255)

# ------------------------------------------------------------------ 3) ponta da cauda em brasa
for x in range(42, 56):
    for y in range(49, 54):
        if B[x, y][3]:
            t = (y - 49) / 4.0
            B[x, y] = lerp(EMBER_DEEP, EMBER_HOT, t)
            G[x, y] = lerp((90, 15, 5, 255), (255, 170, 60, 255), t)

# ------------------------------------------------------------------ 4) pescoço cortado (0,35) 4x10x7
NU, NV, NW, NH, ND = 0, 35, 4, 10, 7
side_rects = [(NU, NV + ND, ND), (NU + ND, NV + ND, NW), (NU + ND + NW, NV + ND, ND), (NU + 2 * ND + NW, NV + ND, NW)]
for (sx, sy, w) in side_rects:
    for x in range(sx, sx + w):
        for y in range(sy, sy + NH):
            row = y - sy  # 0 = perto do corte
            src_p = S[min(63, x), min(63, y + 2)]
            L = lum(src_p) if src_p[3] else 0.3
            c = lerp(CHAR_DARK, CHAR_LIGHT, (L - 0.08) / 0.55)
            if row == 0:
                c = EMBER_HOT if (x % 2) else EMBER
                G[x, y] = (255, 170, 60, 255)
            elif row == 1:
                c = EMBER if (x + y) % 2 else EMBER_DEEP
                G[x, y] = (210, 80, 20, 255)
            elif row == 2 and rnd.random() < 0.6:
                c = EMBER_DEEP
                G[x, y] = (120, 25, 5, 255)
            B[x, y] = c
    # veias descendo do corte
    for _ in range(2):
        x = rnd.randrange(sx, sx + w)
        for row in range(2, rnd.randint(5, 9)):
            ember(x, sy + row, 0.6 - row * 0.05)
            if rnd.random() < 0.4:
                x = max(sx, min(sx + w - 1, x + rnd.choice([-1, 1])))
# o corte (faces de cima e de baixo): núcleo derretido
for fx in (NU + ND, NU + ND + NW):
    for x in range(fx, fx + NW):
        for y in range(NV, NV + ND):
            cx, cy = fx + (NW - 1) / 2, NV + (ND - 1) / 2
            d = max(abs(x - cx) / (NW / 2), abs(y - cy) / (ND / 2))
            c = CORE if d < 0.45 else EMBER_HOT if d < 0.75 else EMBER if d < 0.95 else EMBER_DEEP
            B[x, y] = c
            G[x, y] = lerp((255, 240, 170, 255), (160, 30, 5, 255), d)

# ------------------------------------------------------------------ 5) chamas (0,0) 8x12 frente e verso
FLAME_A = [
    "....#...",
    "...##..#",
    ".#.##..#",
    ".#.###.#",
    ".##.####",
    ".######.",
    "########",
    "########",
    "########",
    ".######.",
    ".######.",
    "..####..",
]
FLAME_B = [
    "...#....",
    "#..##...",
    "#..##.#.",
    "#.###.#.",
    "####.##.",
    ".######.",
    "########",
    "########",
    "########",
    ".######.",
    ".######.",
    "..####..",
]


def flame_color(x, y, shape):
    # mais quente no centro de baixo, vermelho nas bordas e pontas
    edge = any(not (0 <= x + dx < 8 and 0 <= y + dy < 12) or shape[y + dy][x + dx] != '#'
               for dx, dy in ((1, 0), (-1, 0), (0, -1)))
    heat = (y / 11.0) * (1.0 - abs(x - 3.5) / 5.0)
    if edge:
        return (200, 40, 10, 255), (220, 60, 15, 255)
    if heat > 0.55:
        return CORE, (255, 245, 190, 255)
    if heat > 0.3:
        return EMBER_HOT, (255, 180, 70, 255)
    return EMBER, (240, 100, 25, 255)


for ox, shape in ((0, FLAME_A), (8, FLAME_B)):
    for y in range(12):
        for x in range(8):
            if shape[y][x] == '#':
                b, g = flame_color(x, y, shape)
                B[ox + x, y] = b
                G[ox + x, y] = g

# ------------------------------------------------------------------ 6) crina de fogo (16,4) e (20,4), 4x10 cada
# simétrica (miolo quente, bordas vermelhas com falhas), para ficar certa dos dois lados do plano
for ox in (16, 20):
    for y in range(10):
        for x in range(4):
            if x in (1, 2):
                b, g = (CORE, (255, 240, 170, 255)) if (y + x) % 3 == 0 else (EMBER_HOT, (255, 180, 70, 255))
            elif (y * 2 + x) % 5 != 0:
                b, g = ((200, 40, 10, 255), (220, 60, 15, 255)) if y % 2 else (EMBER, (240, 100, 25, 255))
            else:
                continue
            B[ox + x, 4 + y] = b
            G[ox + x, 4 + y] = g

base.save(f'{out_dir}/headless_mule.png')
glow.save(f'{out_dir}/headless_mule_glow.png')
print('ok')
