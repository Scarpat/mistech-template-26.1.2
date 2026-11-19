"""4 estilos bem diferentes para a Fornalha Rúnica, partindo do pixel art vanilla como matéria-prima.
A = Vanilla+ rúnico | B = Engenharia (Create) | C = Arcano (Thaumcraft) | D = Místico ornamentado (Iron's Spells)
Uso: python estilos4.py <pasta_saida>"""
import os, sys, json
from PIL import Image
from kit import R, COBRE, AMETISTA, OURO, ARCANO, FOGO, LATAO
from modelkit import Model

VAN = os.path.expanduser('~/van/assets/minecraft/textures/block/')
def V(n):
    im = Image.open(VAN + n + '.png').convert('RGBA'); return im.crop((0, 0, 16, 16))
def lum(p): return 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]

def recolor(im, ramp, box=(0, 0, 16, 16), pred=None, lo=None, hi=None, shift=0):
    """Mapeia o brilho de cada pixel (dentro de box, e que passe em pred) para a rampa, mantendo o desenho vanilla."""
    x0, y0, x1, y1 = box
    pts = [(x, y) for y in range(y0, y1) for x in range(x0, x1) if im.getpixel((x, y))[3] > 0 and (pred is None or pred(im.getpixel((x, y)), x, y))]
    if not pts: return im
    ls = [lum(im.getpixel(p)) for p in pts]
    lo = min(ls) if lo is None else lo; hi = max(ls) if hi is None else hi
    for (x, y), l in zip(pts, ls):
        k = 0 if hi == lo else (l - lo) / (hi - lo)
        i = max(0, min(len(ramp) - 1, int(round(k * (len(ramp) - 1))) + shift))
        im.putpixel((x, y), ramp[i])
    return im

def paste(dst, src, sbox, pos, pred=None):
    x0, y0, x1, y1 = sbox
    for y in range(y0, y1):
        for x in range(x0, x1):
            p = src.getpixel((x, y))
            if p[3] and (pred is None or pred(p, x, y)): dst.putpixel((pos[0] + x - x0, pos[1] + y - y0), p)
    return dst

is_fire = lambda p, x, y: p[0] > 150 and p[0] > p[2] + 60
not_dark = lambda p, x, y: lum(p) > 45
OUT = {}

# ---------------- A: Vanilla+ rúnico ----------------
def a_front(on):
    im = V('furnace_front_on' if on else 'furnace_front')
    recolor(im, COBRE, (1, 9, 15, 15), pred=lambda p, x, y: not_dark(p, x, y) and not is_fire(p, x, y))
    recolor(im, AMETISTA, (3, 7, 13, 8), shift=1 if on else -1)
    if on:   # chama rúnica: pontas violeta
        for y in range(11, 16):
            for x in range(16):
                p = im.getpixel((x, y))
                if is_fire(p, x, y) and y <= 12: im.putpixel((x, y), AMETISTA[5])
                elif is_fire(p, x, y) and y == 13 and lum(p) > 200: im.putpixel((x, y), AMETISTA[6])
    return im

def a_side():
    im = V('furnace_side')
    recolor(im, COBRE, (1, 9, 15, 14))
    for (x, y, t) in ((7, 3, 4), (8, 4, 5), (7, 5, 4), (6, 4, 3), (9, 4, 3)): im.putpixel((x, y), AMETISTA[t])
    return im

def grate():
    g = recolor(V('copper_grate'), COBRE, pred=not_dark)
    for y in range(16):
        for x in range(16):
            if g.getpixel((x, y))[3] == 0: g.putpixel((x, y), (24, 16, 14, 255))
    return g

def a_top():
    im = V('furnace_top'); paste(im, grate(), (4, 4, 12, 12), (4, 4))
    return im

# ---------------- B: Engenharia (Create) ----------------
def casing(panel):
    im = V('polished_andesite')
    paste(im, panel, (2, 2, 14, 14), (2, 2))
    for i in range(2, 14):   # sombra do quadro sobre o painel
        for (x, y) in ((i, 2), (2, i)):
            p = im.getpixel((x, y)); im.putpixel((x, y), tuple(int(c * 0.7) for c in p[:3]) + (255,))
    return im

def b_side(): return casing(V('stripped_spruce_log'))
def b_top(): return casing(grate())
def b_front(on):
    im = casing(V('stripped_spruce_log'))
    g = V('blast_furnace_front_on' if on else 'blast_furnace_front')
    recolor(g, COBRE, (3, 6, 13, 13), pred=lambda p, x, y: not is_fire(p, x, y) and lum(p) > 70)
    paste(im, g, (3, 6, 13, 13), (3, 5))
    return im

# ---------------- C: Arcano (Thaumcraft) ----------------
ARCANA = R('120f1a', '1c1828', '272236', '332c45', '413858', '534870', '6a5c8a')
def c_side():
    im = recolor(V('polished_blackstone_bricks'), ARCANA)
    sig = ["....##....", "...#..#...", "..#.##.#..", ".#.#..#.#.", "#.#.@@.#.#", "#.#.@@.#.#", ".#.#..#.#.", "..#.##.#..", "...#..#...", "....##...."]
    for j, r in enumerate(sig):
        for i, c in enumerate(r):
            if c == '#': im.putpixel((3 + i, 3 + j), OURO[4] if j < 5 else OURO[3])
            elif c == '@': im.putpixel((3 + i, 3 + j), AMETISTA[5])
    for x in range(16): im.putpixel((x, 0), OURO[4]); im.putpixel((x, 15), OURO[2])
    return im

def c_front(on):
    im = V('furnace_front_on' if on else 'furnace_front')
    recolor(im, ARCANA, pred=lambda p, x, y: not_dark(p, x, y) and not is_fire(p, x, y))
    recolor(im, OURO, (1, 9, 15, 10))
    for x in range(16): im.putpixel((x, 0), OURO[4]); im.putpixel((x, 15), OURO[2])
    if on: recolor(im, AMETISTA, pred=is_fire, shift=1)
    return im

def c_top():
    im = recolor(V('lodestone_top'), ARCANA)
    for (x, y) in ((7, 4), (8, 4), (4, 7), (4, 8), (11, 7), (11, 8), (7, 11), (8, 11)): im.putpixel((x, y), OURO[4])
    im.putpixel((7, 7), AMETISTA[5]); im.putpixel((8, 8), AMETISTA[4])
    return im

# ---------------- D: Místico ornamentado (Iron's Spells) ----------------
def gold_frame(im):
    g = recolor(V('gold_block'), OURO, shift=-2)
    for i in range(16):
        for (x, y) in ((i, 0), (i, 1), (0, i), (1, i), (i, 14), (i, 15), (14, i), (15, i)):
            im.putpixel((x, y), g.getpixel((x, y)))
    cc = recolor(V('chiseled_copper'), OURO, shift=-1)
    for (cx, cy) in ((0, 0), (12, 0), (0, 12), (12, 12)):
        paste(im, cc, (cx, cy, cx + 4, cy + 4), (cx, cy))
    return im

def d_side():
    im = gold_frame(V('dark_oak_planks'))
    am = V('amethyst_block')
    for y in range(5, 11):
        for x in range(5, 11):
            if abs(x - 7.5) + abs(y - 7.5) <= 3: im.putpixel((x, y), am.getpixel((x, y)))
    return im

def d_front(on):
    im = gold_frame(V('dark_oak_planks'))
    sm = V('smoker_front_on' if on else 'smoker_front')
    if on: recolor(sm, ARCANO, pred=is_fire, shift=1)
    paste(im, sm, (3, 8, 13, 14), (3, 6))
    recolor(im, OURO, (3, 5, 13, 6))
    for (x, y) in ((7, 3), (8, 3), (7, 4), (8, 4)): im.putpixel((x, y), AMETISTA[5 if (x + y) % 2 else 4] if not on else AMETISTA[6])
    return im

def d_top():
    im = gold_frame(V('dark_oak_planks'))
    for (x, y) in ((6, 4), (7, 4), (8, 4), (9, 4), (5, 5), (10, 5), (4, 6), (11, 6), (4, 7), (11, 7), (4, 8), (11, 8), (4, 9), (11, 9),
                   (5, 10), (10, 10), (6, 11), (7, 11), (8, 11), (9, 11)): im.putpixel((x, y), OURO[4])
    im.putpixel((7, 7), AMETISTA[5]); im.putpixel((8, 8), AMETISTA[4]); im.putpixel((8, 7), AMETISTA[3]); im.putpixel((7, 8), AMETISTA[3])
    return im

def cube(prefix, on):
    t = lambda n: f'mistech:block/{prefix}_{n}'
    return {'parent': 'minecraft:block/orientable', 'textures': {'top': t('top'), 'front': t('front_on' if on else 'front'), 'side': t('side')}}

def cog_model(on):
    m = Model({'particle': 'mistech:block/sb_side', 'side': 'mistech:block/sb_side', 'front': 'mistech:block/sb_front_on' if on else 'mistech:block/sb_front',
               'top': 'mistech:block/sb_top', 'wood': 'minecraft:block/stripped_spruce_log', 'hub': 'minecraft:block/polished_andesite'})
    m.box((0, 0, 0), (16, 16, 16), {'north': 'front', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'side'})
    for r in (None, ((17, 8, 8), 'x', 45)):
        m.box((16, 4.5, 4.5), (18, 11.5, 11.5), 'wood', rot=r)
        for (y0, y1, z0, z1) in ((2.5, 4.5, 7, 9), (11.5, 13.5, 7, 9), (7, 9, 2.5, 4.5), (7, 9, 11.5, 13.5)):
            m.box((16.25, y0, z0), (17.75, y1, z1), 'wood', rot=r)
    m.box((17.5, 6.5, 6.5), (19, 9.5, 9.5), 'hub')
    return m.json()

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out + '/models', exist_ok=True)
    T = {'sa_front': a_front(False), 'sa_front_on': a_front(True), 'sa_side': a_side(), 'sa_top': a_top(),
         'sb_front': b_front(False), 'sb_front_on': b_front(True), 'sb_side': b_side(), 'sb_top': b_top(),
         'sc_front': c_front(False), 'sc_front_on': c_front(True), 'sc_side': c_side(), 'sc_top': c_top(),
         'sd_front': d_front(False), 'sd_front_on': d_front(True), 'sd_side': d_side(), 'sd_top': d_top()}
    for n, im in T.items(): im.save(f'{out}/{n}.png')
    for p in ('sa', 'sc', 'sd'):
        for on in (False, True): json.dump(cube(p, on), open(f'{out}/models/{p}_furnace{"_on" if on else ""}.json', 'w'))
    for on in (False, True): json.dump(cog_model(on), open(f'{out}/models/sb_furnace{"_on" if on else ""}.json', 'w'))
    print('ok')
