"""Visual dos itens do Louco: Cajado do Louco em 3D (na mão) + ícones, e o chapéu de bobo da corte
(textura do chapéu vestido e o pedaço do chapéu na textura do chefe).
Uso (na pasta do projeto): python tools/tarot/gen_fool_designs.py [pasta_previa]
Depende de tools/artifacts/boxmodel.py."""
import sys, os, json
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'artifacts'))
from PIL import Image
from boxmodel import Model, hexc, mix, jitter, solid, grain, bands, per_face, write_model

A = 'src/main/resources/assets/mistech/'
T = A + 'textures/'
PREV = sys.argv[1] if len(sys.argv) > 1 else None

RED_D, RED, RED_L = hexc('7E1A22'), hexc('C8323C'), hexc('E86A6A')
YEL_D, YEL, YEL_L = hexc('B8862A'), hexc('F2C94C'), hexc('FFE79A')
GOLD_D, GOLD, GOLD_L = hexc('8A5A0E'), hexc('E8B830'), hexc('FFF0A0')
BIRCH_D, BIRCH, BIRCH_L = hexc('B9AE96'), hexc('DDD3BC'), hexc('F3EEDF')
SKY_D, SKY, SKY_L, WHITE = hexc('5FA8D8'), hexc('9FD8FF'), hexc('DDF3FF'), hexc('FFFFFF')
OUT = hexc('2A1A10')

# ================================================================== chapéu de bobo (texturas)
def paint_cap(img, u, v):
    """Pinta o chapéu no layout de JesterCap (touca, faixa e três pontas) a partir de (u, v)."""
    p = img.load()

    def put(x, y, c):
        p[u + x, v + y] = c

    def region(x0, y0, w, h, fn):
        for j in range(h):
            for i in range(w):
                put(x0 + i, y0 + j, fn(i, j, w, h))

    def split(right_first):  # metade amarela (lado direito do personagem) / metade vermelha
        def f(i, j, w, h):
            first = i < w / 2
            c = (YEL if first else RED) if right_first else (RED if first else YEL)
            shade = 1.0 - j * 0.03
            c = tuple(int(c[k] * shade) for k in range(3)) + (255,)
            if (i + j) % 5 == 0:
                c = mix(c, WHITE, 0.15)
            return c
        return f

    # touca 9x4x9: (0,0); topo (9,0) 9x9; baixo (18,0); lados linha 9: dir (0,9) 9x4, frente (9,9), esq (18,9), trás (27,9)
    w, h, d = 9, 4, 9
    region(d, 0, w, d, split(True))
    region(d + w, 0, w, d, lambda i, j, ww, hh: GOLD_D)
    region(0, d, d, h, lambda i, j, ww, hh: YEL if j < h - 1 else YEL_D)
    region(d, d, w, h, split(True))
    region(d + w, d, d, h, lambda i, j, ww, hh: RED if j < h - 1 else RED_D)
    region(2 * d + w, d, w, h, split(False))

    # faixa 10x1x10 em (0,13): ouro com losangos vermelhos e amarelos alternados
    w, h, d = 10, 1, 10
    def trim(i, j, ww, hh):
        k = i % 4
        return RED if k == 1 else YEL if k == 3 else GOLD
    region(d, 13, w, d, lambda i, j, ww, hh: GOLD)
    region(d + w, 13, w, d, lambda i, j, ww, hh: GOLD_D)
    region(0, 13 + d, 2 * d + 2 * w, h, trim)

    # pontas: linha de 7 de altura; segmentos 3x4x3 (0), 2x4x2 (12), 2x3x2 (20), 1x3x1 (28), guizo 2x2x2 (32)
    def cube(x0, y0, w, h, d, fn):
        region(x0 + d, y0, w, d, fn)
        region(x0 + d + w, y0, w, d, fn)
        region(x0, y0 + d, d, h, fn)
        region(x0 + d, y0 + d, w, h, fn)
        region(x0 + d + w, y0 + d, d, h, fn)
        region(x0 + 2 * d + w, y0 + d, w, h, fn)

    def cloth(c, cl, cd):
        def f(i, j, w, h):
            if j == 0 and h > 1:
                return cl
            return cd if (i + j) % 3 == 0 else c
        return f

    def bell(i, j, w, h):
        return GOLD_D if (j == h - 1 and i == w // 2) else (GOLD_L if (i + j) % 3 == 0 else GOLD)

    rows = {24: [(RED, RED_L, RED_D)] * 4,          # ponta esquerda: vermelha
            31: [(YEL, YEL_L, YEL_D)] * 4,          # direita: amarela
            38: [(RED, RED_L, RED_D), (YEL, YEL_L, YEL_D)] * 2}  # de trás: alternada
    segs = [(0, 3, 4, 3), (12, 2, 4, 2), (20, 2, 3, 2), (28, 1, 3, 1)]
    for row, colors in rows.items():
        for (x0, sw, sh, sd), col in zip(segs, colors):
            cube(x0, row, sw, sh, sd, cloth(*col))
        cube(32, row, 2, 2, 2, bell)


# chapéu vestido (textura própria 64x64)
cap = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
paint_cap(cap, 0, 0)
cap.save(T + 'entity/fool_cap.png')
# chapéu do chefe: dentro da textura dele, a partir de (64, 16) — limpa a área antes
boss = Image.open(T + 'entity/the_fool.png').convert('RGBA')
bp = boss.load()
for y in range(16, 64):
    for x in range(64, 128):
        bp[x, y] = (0, 0, 0, 0)
paint_cap(boss, 64, 16)
boss.save(T + 'entity/the_fool.png')

# ================================================================== Cajado do Louco 3D
def staff():
    m = Model('fool_staff_3d', 64, seed=7)
    m.box((7.4, -7.5, 7.4), (8.6, 18, 8.6), bands([BIRCH, BIRCH_L, BIRCH, BIRCH_D, GOLD]), name='haste')
    m.box((7.2, 1, 7.2), (8.8, 6, 8.8), bands([RED, RED, YEL, YEL]), name='empunhadura')
    m.box((7.2, -9, 7.2), (8.8, -7.5, 8.8), solid(GOLD, 6, top=GOLD_L, bottom=GOLD_D), name='ponteira')
    m.box((6.5, 18, 6.5), (9.5, 19.5, 9.5), solid(GOLD, 6, top=GOLD_L, bottom=GOLD_D), name='colar')
    # orbe de vento com aro
    m.box((6.25, 19.5, 6.25), (9.75, 23, 9.75), per_face(solid(SKY, 12, top=SKY_L, bottom=SKY_D), up=solid(SKY_L, 8)), name='orbe')
    hoop = solid(SKY_L, 8)
    m.box((4.5, 21, 4.5), (11.5, 21.7, 5.2), hoop, name='aro_n')
    m.box((4.5, 21, 10.8), (11.5, 21.7, 11.5), hoop, name='aro_s')
    m.box((4.5, 21, 5.2), (5.2, 21.7, 10.8), hoop, name='aro_o')
    m.box((10.8, 21, 5.2), (11.5, 21.7, 10.8), hoop, name='aro_l')
    # três pontas de bobo moles, caindo, com guizos: esquerda vermelha, direita amarela, de trás alternada
    red = solid(RED, 8, top=RED_L, bottom=RED_D)
    yel = solid(YEL, 8, top=YEL_L, bottom=YEL_D)
    bell = solid(GOLD, 8, top=GOLD_L, bottom=GOLD_D)
    left = [((5.0, 22.5, 7.2), (7.0, 24.5, 8.8)), ((3.4, 24.0, 7.35), (5.2, 26.0, 8.65)),
            ((2.0, 24.4, 7.5), (3.6, 25.8, 8.5)), ((1.4, 22.8, 7.6), (2.5, 24.5, 8.4))]
    for i, (a0, b0) in enumerate(left):
        m.box(a0, b0, red, name='esq%d' % i)
        m.box((16 - b0[0], a0[1], a0[2]), (16 - a0[0], b0[1], b0[2]), yel, name='dir%d' % i)
    m.box((1.0, 21.2, 7.2), (2.9, 23.0, 8.8), bell, name='guizo_esq')
    m.box((13.1, 21.2, 7.2), (15.0, 23.0, 8.8), bell, name='guizo_dir')
    m.box((7.0, 23, 7.0), (9.0, 25.5, 9.0), yel, name='tras0')
    m.box((7.25, 25.5, 7.6), (8.75, 27.5, 9.4), red, name='tras1')
    m.box((7.45, 26.3, 9.4), (8.55, 27.6, 11.0), yel, name='tras2')
    m.box((7.55, 24.8, 10.6), (8.45, 26.4, 11.6), red, name='tras3')
    m.box((7.1, 23.2, 10.2), (8.9, 25.0, 12.0), bell, name='guizo_tras')
    return m


def tool_display(grip, scale=0.85, fp_scale=0.68):
    t3 = round((8 - grip) * scale, 2)
    t1 = round(2.2 + (8 - grip) * fp_scale * 0.6, 2)
    return {
        'thirdperson_righthand': {'rotation': [0, 90, -10], 'translation': [0, t3, 0.5], 'scale': [scale] * 3},
        'thirdperson_lefthand': {'rotation': [0, -90, 10], 'translation': [0, t3, 0.5], 'scale': [scale] * 3},
        'firstperson_righthand': {'rotation': [0, -90, 15], 'translation': [1.13, t1, 1.13], 'scale': [fp_scale] * 3},
        'firstperson_lefthand': {'rotation': [0, 90, -15], 'translation': [1.13, t1, 1.13], 'scale': [fp_scale] * 3},
        'gui': {'rotation': [15, -30, -45], 'scale': [0.5] * 3},
        'ground': {'translation': [0, 2, 0], 'scale': [0.4] * 3},
        'fixed': {'rotation': [0, 0, -45], 'scale': [0.6] * 3},
    }


m = staff()
write_model(m, A, tool_display(4.5, 0.8, 0.6), 'fool_staff_model')
json.dump({'model': {'type': 'minecraft:select', 'property': 'minecraft:display_context',
                     'cases': [{'when': ['gui', 'ground', 'fixed', 'on_shelf'],
                                'model': {'type': 'minecraft:model', 'model': 'mistech:item/fool_staff'}}],
                     'fallback': {'type': 'minecraft:model', 'model': 'mistech:item/fool_staff_3d'}}},
          open(A + 'items/fool_staff.json', 'w'), indent=2)
if PREV:
    os.makedirs(PREV, exist_ok=True)
    m.preview(PREV + '/fool_staff_3d.png', yaws=(35, 125, 215), pitch=20, scale=6, fit=44)

# ================================================================== ícones 16x16
def icon(rows, pal):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                p[x, y] = pal[ch]
    return im

pal = {'o': OUT, 'r': RED, 'R': RED_D, 'l': RED_L, 'y': YEL, 'Y': YEL_D, 'w': YEL_L, 'g': GOLD, 'G': GOLD_D,
       'b': BIRCH, 'B': BIRCH_D, 's': SKY, 'S': SKY_L}
icon([
    ".......gg.......",
    ".......oo.......",
    "......oryo......",
    "..ooo.oryo.ooo..",
    ".orrrooryoyyyyo.",
    "orrrrrrryyyyyyyo",
    "orroorrryyyyooyo",
    "oro.orrryyyyo.yo",
    "ogo.orrryyyyo.go",
    ".g..orrryyyyo.g.",
    "...ogygygygygo..",
    "...oGGGGGGGGGo..",
    "....ooooooooo...",
    "................",
    "................",
    "................"], pal).save(T + 'item/fool_cap.png')

icon([
    "...........g....",
    ".........gro.y..",
    "........r.ss.yg.",
    "........gSsso...",
    "..........sgo...",
    ".........bg.....",
    "........b.......",
    ".......B........",
    "......b.........",
    ".....r..........",
    "....y...........",
    "...r............",
    "..b.............",
    ".g..............",
    "................",
    "................"], pal).save(T + 'item/fool_staff.png')
print('ok')
