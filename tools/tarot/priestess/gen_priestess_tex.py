"""Texturas da Sacerdotisa: chefe (128x128 + brilho) e ícones do Portal, do Véu e do ovo.
Uso: python gen_priestess_tex.py <assets/mistech/textures>"""
import sys, os, math
from PIL import Image

T = sys.argv[1]
def rgb(h, a=255): return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)
def mix(a, b, t): return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (a[3] if len(a) > 3 else 255,)
def shade(c, f): return tuple(max(0, min(255, int(c[i] * f))) for i in range(3)) + (c[3],)

ROBE = rgb(0x2B3F94); ROBE_D = rgb(0x1B2862); ROBE_L = rgb(0x4F6FD1)
WHITE = rgb(0xEEF2FA); WHITE_D = rgb(0xC6CFE3)
VEIL = rgb(0xDCE6F5); VEIL_D = rgb(0xA9BAD8)
SKIN = rgb(0xE9D8CB); SKIN_D = rgb(0xC9B2A2)
HAIR = rgb(0x2A2433)
GOLD = rgb(0xE8C05A); GOLD_D = rgb(0xA77F2A)
MOON = rgb(0xF6F2DE); MOON_D = rgb(0xD8D2B4)
SCROLL = rgb(0xE8DDB5); SCROLL_D = rgb(0xA8946A)
EYE = rgb(0x9FE8FF); NAVY = rgb(0x1A2140)
RED = rgb(0xB3122E)
WAVE = rgb(0x7FA6F0)
CLEAR = None

class Tex:
    def __init__(s, w, h):
        s.im = Image.new('RGBA', (w, h), (0, 0, 0, 0)); s.p = s.im.load()
    def rect(s, x, y, w, h, fn):
        for j in range(h):
            for i in range(w):
                c = fn(i, j, w, h)
                if c: s.p[x + i, y + j] = c
    def box(s, u, v, w, h, d, side, top=None, bottom=None, front=None, back=None, right=None):
        top = top or side; bottom = bottom or side; front = front or side; back = back or side; right = right or side
        s.rect(u + d, v, w, d, top)
        s.rect(u + d + w, v, w, d, bottom)
        s.rect(u, v + d, d, h, side)          # lado (+x... o "direito" da figura vista de frente)
        s.rect(u + d, v + d, w, h, front)
        s.rect(u + d + w, v + d, d, h, right)
        s.rect(u + 2 * d + w, v + d, w, h, back)

def noise(i, j, seed): return ((i * 73 + j * 151 + seed * 37) % 17) / 16.0
def solid(c): return lambda i, j, w, h: c
def noisy(c, amt=0.06, seed=0): return lambda i, j, w, h: shade(c, 1.0 - amt + noise(i, j, seed) * amt * 2)
def folds(c, dark, period=3, seed=0):
    def f(i, j, w, h):
        t = 0.5 + 0.5 * math.sin((i + 0.3 * j) * 2 * math.pi / period + seed)
        return mix(dark, c, 0.45 + 0.55 * t)
    return f

# ============================================================== chefe 128x128
boss = Tex(128, 128); glow = Tex(128, 128)

# --- cabeça (0,0) 8x8x8
def face(i, j, w, h):
    c = noisy(SKIN, 0.03, 1)(i, j, w, h)
    if j <= 1: c = HAIR                                  # franja escura sob o véu
    if j == 1 and i in (0, 7): c = HAIR
    if j in (2, 3, 4, 5) and i in (0, 7): c = HAIR       # cabelo dos lados do rosto
    if j == 3 and i in (2, 5): c = shade(SKIN_D, 0.85)   # sobrancelhas suaves
    if j == 4 and i in (2, 5): c = EYE                   # olhos prateados
    if j == 4 and i in (1, 6) and False: c = WHITE
    if j == 6 and i in (3, 4): c = rgb(0xB0707A)          # boca serena
    if j == 7 and i in (2, 5): c = shade(SKIN_D, 0.95)
    return c
def head_side(i, j, w, h):
    return HAIR if j < 6 else shade(SKIN_D, 0.9)
boss.box(0, 0, 8, 8, 8, head_side, top=solid(HAIR), bottom=solid(SKIN_D), front=face, back=solid(HAIR))
def face_glow(i, j, w, h):
    return EYE if (j == 4 and i in (2, 5)) else None
glow.rect(8, 8, 8, 8, face_glow)

# --- capuz/véu (32,0) 9x9x9: frente aberta (só a moldura), baixo aberto
def hood(i, j, w, h):
    c = folds(VEIL, VEIL_D, 4, 0.5)(i, j, w, h)
    return shade(c, 0.95 + 0.05 * noise(i, j, 3))
def hood_front(i, j, w, h):
    if j == 0 or i == 0 or i == w - 1:
        return shade(VEIL, 0.92)
    if j == 1 and (i == 1 or i == w - 2):
        return shade(VEIL, 0.88)
    return CLEAR
def hood_top(i, j, w, h): return folds(VEIL, VEIL_D, 5, 1.0)(i, j, w, h)
boss.box(32, 0, 9, 9, 9, hood, top=hood_top, bottom=lambda *a: CLEAR, front=hood_front, back=hood)

# --- tronco (0,18) 8x12x5: túnica branca no meio, manto azul nas bordas, cruz no peito
def torso_front(i, j, w, h):
    c = noisy(WHITE, 0.03, 2)(i, j, w, h)
    if i <= 1 or i >= w - 2: c = folds(ROBE, ROBE_D, 3, 0.2)(i, j, w, h)
    if i == 2 or i == w - 3: c = GOLD_D if j < 3 else shade(WHITE_D, 0.95)  # bordas do manto
    # cruz de braços iguais
    if 3 <= i <= 4 and 2 <= j <= 7: c = GOLD if False else rgb(0xF8F8FF)
    if 2 <= i <= 5 and 4 <= j <= 5: c = rgb(0xF8F8FF)
    if (3 <= i <= 4 and 2 <= j <= 7) or (2 <= i <= 5 and 4 <= j <= 5):
        c = rgb(0xFFFFFF)
    if j == 11: c = GOLD_D                                # cinto
    return c
def torso_side(i, j, w, h):
    c = folds(ROBE, ROBE_D, 3, 0.7)(i, j, w, h)
    return GOLD_D if j == 11 else c
boss.box(0, 18, 8, 12, 5, torso_side, top=solid(ROBE_L), bottom=solid(ROBE_D), front=torso_front, back=torso_side)
def cross_glow(i, j, w, h):
    return rgb(0xDDEEFF) if (3 <= i <= 4 and 2 <= j <= 7) or (2 <= i <= 5 and 4 <= j <= 5) else None
glow.rect(5, 23, 8, 12, cross_glow)

# --- braços (40,20) e (40,50) 4x11x4: manga azul larga, punho branco, mão
def sleeve(i, j, w, h):
    if j >= 9: return noisy(SKIN, 0.04, 4)(i, j, w, h)
    if j == 8: return WHITE
    return folds(ROBE, ROBE_D, 3, i * 0.4)(i, j, w, h)
for (u, v) in ((40, 20), (40, 50)):
    boss.box(u, v, 4, 11, 4, sleeve, top=solid(ROBE_L), bottom=solid(SKIN_D))

# --- rolo da Torá (40,36) 2x2x8
def scroll(i, j, w, h):
    c = noisy(SCROLL, 0.05, 5)(i, j, w, h)
    if i in (0, w - 1) and w > 2: c = SCROLL_D
    if w > 2 and j == 0 and i % 2 == 1 and 1 < i < w - 2: c = rgb(0x3A2A1A)   # letras
    return c
boss.box(40, 36, 2, 2, 8, scroll, top=scroll, bottom=scroll, front=solid(SCROLL_D), back=solid(SCROLL_D))

# --- manto das costas (68,0) 10x22x1
def cape_back(i, j, w, h):
    c = folds(ROBE, ROBE_D, 3, 0.3)(i, j, w, h)
    if (i * 7 + j * 3) % 23 == 0: c = mix(c, ROBE_L, 0.7)  # brilhos d'água
    if j >= h - 2: c = mix(c, WAVE, 0.5 + 0.5 * math.sin(i * 1.3))
    return c
boss.box(68, 0, 10, 22, 1, solid(ROBE_D), top=solid(ROBE_D), bottom=solid(ROBE_D), front=solid(ROBE_D), back=cape_back)

# --- véu das costas (68,24) 9x12x1
boss.box(68, 24, 9, 12, 1, solid(VEIL_D), top=solid(VEIL), bottom=solid(VEIL_D),
         front=folds(VEIL, VEIL_D, 4, 0.0), back=folds(VEIL, VEIL_D, 4, 1.2))

# --- coroa: aro (90,0) 7x1x7, chifres (90,10) (96,10), pontas (102,10) (106,10), lua (90,18) 5x5x1
boss.box(90, 0, 7, 1, 7, noisy(GOLD, 0.08, 6), top=noisy(GOLD, 0.1, 7), bottom=solid(GOLD_D))
horn = lambda i, j, w, h: mix(MOON_D, GOLD, j / max(1, h - 1) * 0.6)
for u in (90, 96):
    boss.box(u, 10, 1, 5, 1, horn)
for u in (102, 106):
    boss.box(u, 10, 1, 2, 1, solid(MOON_D))
def moon(i, j, w, h):
    dx, dy = i - (w - 1) / 2, j - (h - 1) / 2
    if dx * dx + dy * dy > 2.3 * 2.3 and w >= 5: return CLEAR
    c = MOON
    if (i, j) in ((1, 1), (3, 2), (2, 3)): c = MOON_D
    return c
boss.box(90, 18, 5, 5, 1, solid(MOON_D), front=moon, back=moon)
glow.rect(91, 19, 5, 5, lambda i, j, w, h: rgb(0xFFFBE8) if moon(i, j, w, h) else None)
glow.rect(97, 19, 5, 5, lambda i, j, w, h: rgb(0xFFFBE8) if moon(i, j, w, h) else None)

# --- lua crescente aos pés (90,26) região 18x5
boss.rect(90, 26, 18, 5, lambda i, j, w, h: mix(rgb(0xF2E7B0), MOON, noise(i, j, 8)))
glow.rect(90, 26, 18, 5, lambda i, j, w, h: rgb(0xF6EEC0))

# --- saia: coxas (0,36) 9x7x6 e canelas (0,50) 11x7x8
def skirt_front(i, j, w, h):
    c = folds(ROBE, ROBE_D, 3, 0.1)(i, j, w, h)
    if w // 2 - 2 <= i <= w // 2 + 2: c = noisy(WHITE, 0.04, 9)(i, j, w, h)   # túnica branca por baixo
    if i == w // 2 - 3 or i == w // 2 + 3: c = GOLD_D
    return c
boss.box(0, 36, 9, 7, 6, folds(ROBE, ROBE_D, 3, 0.9), top=solid(ROBE_D), bottom=solid(ROBE_D), front=skirt_front)
def hem(i, j, w, h, base):
    c = base(i, j, w, h)
    if j >= h - 2:   # barra em ondas (o manto vira água, como na carta)
        t = 0.5 + 0.5 * math.sin(i * 1.4 + j)
        c = mix(WAVE, WHITE, t * 0.6)
    return c
boss.box(0, 50, 11, 7, 8, lambda i, j, w, h: hem(i, j, w, h, folds(ROBE, ROBE_D, 3, 1.4)),
         top=solid(ROBE_D), bottom=solid(ROBE_D), front=lambda i, j, w, h: hem(i, j, w, h, skirt_front))

os.makedirs(os.path.join(T, 'entity'), exist_ok=True)
boss.im.save(os.path.join(T, 'entity/the_high_priestess.png'))
glow.im.save(os.path.join(T, 'entity/the_high_priestess_glow.png'))

# ============================================================== ícones 16x16
def icon(rows, palette):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.' and ch in palette: p[x, y] = palette[ch]
    return im

# O Portal: arco entre Boaz (preto) e Jachin (branco), véu azul e o redemoinho no meio
portal = icon([
    "................",
    "..GGGGGGGGGGGG..",
    "..gRGGGGGGGGRg..",
    "..BBvvvvvvvvWW..",
    "..BbvVVVVVVvWw..",
    "..BbVVpPPpVVWw..",
    "..BbVpPLLPpVWw..",
    "..BbVPLooLPVWw..",
    "..BbVPLooLPVWw..",
    "..BbVpPLLPpVWw..",
    "..BbVVpPPpVVWw..",
    "..BbvVVrVVVvWw..",
    "..BbvvvvvvrvWw..",
    ".kBBkkkkkkkkWWk.",
    ".kkkkkkkkkkkkkk.",
    "................"], {
    'G': GOLD, 'g': GOLD_D, 'R': RED, 'r': RED, 'B': rgb(0x1E1A24), 'b': rgb(0x3A3446), 'W': rgb(0xF2F2F6), 'w': rgb(0xC8CCD8),
    'v': rgb(0x1E2E78), 'V': ROBE, 'p': rgb(0x6A4FD1), 'P': rgb(0x9C82FF), 'L': rgb(0xC9F0FF), 'o': rgb(0xFFFFFF),
    'k': rgb(0x2A2F44)})
os.makedirs(os.path.join(T, 'item'), exist_ok=True)
portal.save(os.path.join(T, 'item/priestess_portal.png'))

# O Véu: corrente de ouro, lua crescente e o tecido translúcido caindo
veil = icon([
    ".g............g.",
    "..g..........g..",
    "...g........g...",
    "....g......g....",
    ".....g....g.....",
    "......gGGg......",
    ".....mMMMm......",
    "....MMm..mm.....",
    "....MM..........",
    "....MMm..mm.....",
    ".....mMMMm......",
    "......aAAa......",
    ".....aAaaAa.....",
    "....aAa..aAa....",
    "...aAa....aAa...",
    "..a.a......a.a.."], {
    'g': GOLD_D, 'G': GOLD, 'M': MOON, 'm': MOON_D,
    'a': (169, 186, 216, 150), 'A': (220, 230, 245, 200)})
veil.save(os.path.join(T, 'item/priestess_veil.png'))

# ovo gerador
egg = icon([
    "................",
    "......kkkk......",
    ".....kBBBBk.....",
    "....kBBWBBBk....",
    "....kBBBBBBk....",
    "...kBBBBBWBBk...",
    "...kBWBBBBBBk...",
    "...kBBBBBBBBk...",
    "..kBBBBGBBBBBk..",
    "..kBBBGGGBBWBk..",
    "..kBBBBGBBBBBk..",
    "..kBWBBBBBBBBk..",
    "...kBBBBBBWBk...",
    "....kBBBBBBk....",
    ".....kkkkkk.....",
    "................"], {'k': rgb(0x111A44), 'B': ROBE, 'W': WHITE, 'G': GOLD})
egg.save(os.path.join(T, 'item/the_high_priestess_spawn_egg.png'))
print('ok')
