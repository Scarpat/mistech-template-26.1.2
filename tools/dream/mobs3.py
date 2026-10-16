"""Criaturas da fase 3 (Sonho Profundo e Sono REM + chefe): modelos (modelgen), texturas e ovos.
Uso (raiz do projeto): python tools/dream/mobs3.py -> textures/entity/*.png, build/dream/<nome>_layer.txt e _view.png"""
import colorsys, math, os, random, sys, zlib
sys.path.insert(0, 'tools/modelgen')
sys.path.insert(0, 'tools/dream')
from modelgen import Model
import preview
from PIL import Image

T = 'src/main/resources/assets/mistech/textures/'
OUT = 'build/dream/'
os.makedirs(OUT, exist_ok=True)
ALL = ('front', 'back', 'left', 'right', 'top', 'bottom')


def every(fn, faces=ALL):
    return {k: fn for k in faces}


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def mix(a, b, t):
    a, b = rgb(a), rgb(b)
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, k):
    c = rgb(c) if isinstance(c, int) else c
    return tuple(max(0, min(255, int(v * k))) for v in c)


def noise_grid(seed, w, h, cell):
    rnd = random.Random(seed)
    gw, gh = w // cell + 2, h // cell + 2
    grid = [[rnd.uniform(-1, 1) for _ in range(gw)] for _ in range(gh)]

    def at(x, y):
        gx, gy = x / cell, y / cell
        ix, iy = int(gx), int(gy)
        fx, fy = gx - ix, gy - iy
        return (grid[iy][ix] * (1 - fx) * (1 - fy) + grid[iy][ix + 1] * fx * (1 - fy) + grid[iy + 1][ix] * (1 - fx) * fy
                + grid[iy + 1][ix + 1] * fx * fy)
    return at


def soft(base, amp=0.07, cell=3, seed=0, top=0.05, bottom=0.12):
    """Manchas grandes e suaves, topo mais claro, base mais escura — sem pontinhos soltos."""
    def paint(p):
        n = noise_grid(seed * 7919 + p.x0 * 31 + p.y0, p.w, p.h, cell)
        for y in range(p.h):
            for x in range(p.w):
                k = 1.0 + round(n(x, y) * amp / 0.04) * 0.04
                if p.h > 2:
                    k *= 1.0 + top - (top + bottom) * y / (p.h - 1)
                p.px(x, y, shade(base, k))
    return paint


def gradient(c0, c1, seed=0, amp=0.05, vertical=True):
    def paint(p):
        n = noise_grid(seed * 131 + p.x0, p.w, p.h, 4)
        for y in range(p.h):
            for x in range(p.w):
                t = (y / max(1, p.h - 1)) if vertical else (x / max(1, p.w - 1))
                k = 1.0 + round(n(x, y) * amp / 0.04) * 0.04
                p.px(x, y, shade(mix(c0, c1, t), k))
    return paint


def build(M, glow=False, yaws=None, overrides=None):
    M.pack()
    tex = T + 'entity/%s.png' % M.name
    M.paint(tex, T + 'entity/%s_glow.png' % M.name if glow else None)
    open(OUT + '%s_layer.txt' % M.name, 'w', encoding='utf-8').write(M.java())
    kw = {}
    if yaws:
        kw['yaws'] = yaws
    try:
        preview.render(M, tex, OUT + '%s_view.png' % M.name, overrides=overrides, **kw)
    except Exception as e:
        print('sem preview', M.name, e)
    print('ok', M.name, M.tex_w, M.tex_h)
    return M


# =============================================================== Baleia-Nuvem (modelo em meia escala; o renderer aumenta 2,2x)
WH, WH_D, WH_B = 0xb9b4e8, 0x8f88c8, 0xeef0ff
CLOUD = 0xfbfbff


def whale_side(p):
    gradient(WH, WH_D, 1)(p)
    # barriga clara com sulcos compridos (linhas contínuas, não pontos)
    belly = p.h * 2 // 3
    for y in range(belly, p.h):
        for x in range(p.w):
            p.px(x, y, shade(WH_B, 1.0 - 0.1 * (y - belly) / max(1, p.h - belly)))
    for y in range(belly + 1, p.h, 2):
        for x in range(2, p.w - 2):
            p.px(x, y, shade(WH_B, 0.88))


def whale_head_front(p):
    whale_side(p)


def whale_head_side(p):
    whale_side(p)
    # olhinho com brilho, perto da boca
    ex, ey = 3, p.h // 2
    p.rect(ex, ey, 2, 2, 0x20183a)
    p.px(ex, ey, 0xffffff, True)
    # boca: linha longa curvando para cima
    for x in range(0, p.w - 1):
        p.px(x, p.h * 2 // 3 - (1 if x > p.w - 4 else 0), 0x4a3f7a)


def cloud_puff(seed):
    return soft(CLOUD, 0.04, 3, seed, 0.03, 0.18)


def whale():
    M = Model('cloud_whale', 128, seed=12)
    body = M.part('body', pivot=(0, 10, 0))
    body.box((-8, -7, -10), (16, 14, 22), WH, faces={**every(whale_side), 'top': soft(WH, 0.05, 4, 2)}, noise=0.0)
    body.box((-6.5, -8, -9), (13, 1, 20), WH, faces=every(soft(WH, 0.05, 4, 21)), noise=0.0)
    body.box((-6.5, 7, -9), (13, 1, 20), WH_B, faces=every(soft(WH_B, 0.03, 4, 22)), noise=0.0)
    body.box((-9, -5, -9), (1, 10, 20), WH, faces=every(whale_side), noise=0.0)
    body.box((8, -5, -9), (1, 10, 20), WH, faces=every(whale_side), noise=0.0)
    head = body.child('head', pivot=(0, 0, -10))
    head.box((-7, -6, -12), (14, 12, 12), WH, faces={'front': whale_head_front, 'left': whale_head_side, 'right': whale_head_side,
                                                   'top': soft(WH, 0.05, 4, 3), 'bottom': soft(WH_B, 0.03, 4, 4), 'back': whale_side}, noise=0.0)
    head.box((-5.5, -4.5, -14), (11, 9, 2), WH, faces={**every(whale_side), 'front': whale_head_front}, noise=0.0)
    head.box((-6, -7, -11), (12, 1, 10), WH, faces=every(soft(WH, 0.05, 4, 23)), noise=0.0)
    # nuvens nas costas (filhas do corpo)
    for i, (o, s) in enumerate((((-6, -10, -6), (8, 3, 8)), ((-1, -11, -2), (7, 4, 9)), ((-4, -9, 4), (9, 2, 6)), ((-3, -8, -12), (6, 2, 6)))):
        body.box(o, s, CLOUD, faces=every(cloud_puff(10 + i)), noise=0.0)
    tail = body.child('tail', pivot=(0, -1, 12))
    tail.box((-5, -4, 0), (10, 9, 10), WH, faces={**every(whale_side), 'top': soft(WH, 0.05, 4, 6)}, noise=0.0)
    tail2 = tail.child('tail_end', pivot=(0, 0, 10))
    tail2.box((-3, -2.5, 0), (6, 6, 8), WH, faces=every(whale_side), noise=0.0)
    flukes = tail2.child('flukes', pivot=(0, 0.5, 7))
    flukes.box((-11, -0.5, 0), (22, 1, 7), WH_D, faces=every(soft(WH_D, 0.05, 4, 7)), noise=0.0)
    for name, x, rz in (('right_fin', -8, 0.3), ('left_fin', 8, -0.3)):
        fin = body.child(name, pivot=(x, 4, -5), rot=(0, 0, rz))
        fin.box((-10, -0.5, -2) if x < 0 else (0, -0.5, -2), (10, 1, 7), WH_D, faces=every(soft(WH_D, 0.05, 4, 8)), noise=0.0)
    return M


# =============================================================== Peixe-do-Ar (3 cores)
FISH = [(0xff9ad5, 0xffe08a, 0xffffff), (0x8ae8ff, 0x9a8aff, 0xffffff), (0xb8ff8a, 0x5ad8c8, 0xfff0a0)]


def sky_fish(v):
    a, b, c = FISH[v]
    M = Model('sky_fish_%d' % v, 32, seed=40)
    body = M.part('body', pivot=(0, 21, 0))

    def side(p):
        gradient(a, b, v, 0.03, vertical=False)(p)
        # listra central contínua
        for x in range(p.w):
            p.px(x, p.h // 2, shade(c, 0.95))

    def face(p):
        p.fill(a, 0.0)
        p.px(0, 0, 0x1c1830)
        p.px(p.w - 1, 0, 0x1c1830)
    body.box((-1, -1.5, -3), (2, 3, 6), a, faces={**every(side), 'front': face}, noise=0.0)
    tail = body.child('tail', pivot=(0, 0, 3))
    tail.box((-0.5, -2, 0), (1, 4, 3), b, faces=every(gradient(b, c, v + 3, 0.0, vertical=False)), noise=0.0)
    body.box((-0.5, -3, -1), (1, 2, 3), b, faces=every(gradient(b, a, v + 5, 0.0)), noise=0.0)
    for name, x in (('right_fin', -1), ('left_fin', 1)):
        body.child(name, pivot=(x, 0.5, -1), rot=(0, 0, 0.6 if x < 0 else -0.6)).box((-2 if x < 0 else 0, 0, 0), (2, 1, 2), c,
                                                                                       faces=every(gradient(c, b, v + 7, 0.0)), noise=0.0)
    return M


# =============================================================== Mariposa-Vitral (id prism_moth)
LEAD = 0x14101a
PANES = [0xd8304a, 0x3a5ad8, 0x2aa86a, 0xf0b030, 0x9a3ad0, 0x30b8d0]


def stained(seed, shape):
    """Vitral: células de Voronoi com chumbo preto entre elas; vidros coloridos que brilham. Fora da forma da asa = transparente."""
    def paint(p):
        rnd = random.Random(seed * 101 + p.x0)
        pts = [(rnd.uniform(0, p.w), rnd.uniform(0, p.h), rnd.choice(PANES)) for _ in range(max(4, p.w * p.h // 22))]
        for y in range(p.h):
            for x in range(p.w):
                u, v = (x + 0.5) / p.w, (y + 0.5) / p.h
                if not shape(u, v):
                    p.clear(x, y)
                    continue
                if not shape(u + 1.2 / p.w, v) or not shape(u - 1.2 / p.w, v) or not shape(u, v + 1.2 / p.h) or not shape(u, v - 1.2 / p.h):
                    p.px(x, y, LEAD)
                    continue
                ds = sorted(((x + 0.5 - px) ** 2 + (y + 0.5 - py) ** 2, c) for px, py, c in pts)
                if math.sqrt(ds[1][0]) - math.sqrt(ds[0][0]) < 0.55:
                    p.px(x, y, LEAD)
                else:
                    k = 1.15 - 0.3 * v
                    p.px(x, y, shade(ds[0][1], k), True)
    return paint


def fore_shape_r(u, v):
    # asa da frente (lado direito, raiz em u=1): ponta arredondada para fora, borda de trás curva
    return (u - 1.0) ** 2 / 1.0 + (v - 0.45) ** 2 / 0.32 < 1.0 and v > 0.05 + 0.25 * (1 - u) ** 2


def hind_shape_r(u, v):
    return (u - 1.0) ** 2 / 0.9 + (v - 0.35) ** 2 / 0.45 < 1.0


def mirror(shape):
    return lambda u, v: shape(1 - u, v)


def moth():
    M = Model('prism_moth', 64, seed=55)
    body = M.part('body', pivot=(0, 18, 0))
    body.box((-1.5, -1.5, -2), (3, 3, 4), 0x2a2030, faces=every(soft(0x2a2030, 0.06, 2, 1)), noise=0.0)
    head = body.child('head', pivot=(0, 0, -2))
    head.box((-1.5, -1.5, -2), (3, 3, 2), 0x2a2030, faces={**every(soft(0x2a2030, 0.06, 2, 2)),
                                                          'front': lambda p: (p.fill(0x2a2030, 0), p.px(0, 1, 0xfff0c0, True), p.px(p.w - 1, 1, 0xfff0c0, True))},
             noise=0.0)

    def feather(p):
        for y in range(p.h):
            for x in range(p.w):
                if abs(x - (p.w - 1) / 2) <= (y + 1) * p.w / (2 * p.h) + 0.2:
                    p.px(x, y, 0xe8d8b0 if x == p.w // 2 else 0xb8a888)
                else:
                    p.clear(x, y)
    for name, x, rz in (('right_antenna', -1, -0.5), ('left_antenna', 1, 0.5)):
        head.child(name, pivot=(x, -1.5, -1.5), rot=(-0.5, 0, rz)).box((-1.5, -6, 0), (3, 6, 0.01), 0xe8d8b0,
                                                                        faces={'front': feather, 'back': feather}, noise=0.0)
    # abdômen = filamento de lâmpada aceso, anéis escuros
    def filament(p):
        for y in range(p.h):
            for x in range(p.w):
                ring = y % 3 == 0
                p.px(x, y, 0x3a2a20 if ring else shade(0xffc860, 1.1 - 0.4 * y / max(1, p.h - 1)), not ring)
    body.child('abdomen', pivot=(0, 0, 2), rot=(-0.15, 0, 0)).box((-1, -1, 0), (2, 2, 7), 0xffc860, faces=every(filament), noise=0.0)
    for name, x in (('right_wing', -1.5), ('left_wing', 1.5)):
        right = x < 0
        w = body.child(name, pivot=(x, -1.5, -1))
        w.box((-12 if right else 0, 0, -4), (12, 0.01, 9), LEAD,
              faces={'top': stained(1 if right else 2, fore_shape_r if right else mirror(fore_shape_r)),
                     'bottom': stained(1 if right else 2, fore_shape_r if right else mirror(fore_shape_r))}, noise=0.0)
        hw = body.child(name.replace('wing', 'hind_wing'), pivot=(x, -1, 2))
        hw.box((-8 if right else 0, 0, 0), (8, 0.01, 8), LEAD,
               faces={'top': stained(3 if right else 4, hind_shape_r if right else mirror(hind_shape_r)),
                      'bottom': stained(3 if right else 4, hind_shape_r if right else mirror(hind_shape_r))}, noise=0.0)
    return M


# =============================================================== Eco (negativo de foto, três cabeças em leque)
NEG, NEG_D, NEG_L = 0xb8e8f0, 0x6aa8b8, 0xe8fcff


def negative(seed, alpha=255):
    def paint(p):
        n = noise_grid(seed * 31 + p.x0, p.w, p.h, 4)
        for y in range(p.h):
            # faixas de "glitch": algumas linhas deslocadas e mais claras
            glitch = (y * 7 + seed) % 11 == 0
            for x in range(p.w):
                k = 1.0 + round(n(x, y) * 0.05 / 0.04) * 0.04
                c = shade(NEG_L if glitch else mix(NEG, NEG_D, y / max(1, p.h - 1)), k)
                if x == 0 or x == p.w - 1:
                    c = shade(NEG_D, 0.8)
                p.pxa(x, y, c, alpha)
    return paint


def echo_face(alpha):
    def paint(p):
        negative(9, alpha)(p)
        # no negativo os olhos são buracos pretos que brilham por dentro, e a boca é um risco vertical comprido
        for x0 in (1, p.w - 3):
            p.pxa(x0, 3, 0x05080a, alpha)
            p.pxa(x0 + 1, 3, 0x05080a, alpha)
            p.pxa(x0, 4, 0x05080a, alpha)
            p.pxa(x0 + 1, 4, 0x05080a, alpha)
            p.pxa(x0, 3, 0x203040, alpha, True)
        for y in range(5, p.h - 1):
            p.pxa(p.w // 2, y, 0x05080a, alpha)
    return paint


def echo():
    M = Model('echo', 64, seed=60)
    head = M.part('head', pivot=(0, -1, 0))
    head.box((-3, -7, -3), (6, 7, 6), NEG, faces={**every(negative(1)), 'front': echo_face(255)}, noise=0.0)
    for name, sx, rz, al in (('ghost_head_r', -4.5, -0.3, 110), ('ghost_head_l', 4.5, 0.3, 110), ('ghost_head_r2', -8.0, -0.55, 45),
                             ('ghost_head_l2', 8.0, 0.55, 45)):
        head.child(name, pivot=(sx, -0.5 - abs(sx) * 0.1, 0.5), rot=(0, 0, rz)).box((-3, -7, -3), (6, 7, 6), NEG,
                                                                                faces={**every(negative(2, al)), 'front': echo_face(al)}, noise=0.0)
    M.part('body', pivot=(0, -1, 0)).box((-3, 0, -1.5), (6, 13, 3), NEG, faces=every(negative(3)), noise=0.0)
    for name, x in (('right_arm', -4), ('left_arm', 4)):
        M.part(name, pivot=(x, 0, 0)).box((-1, -1, -1), (2, 17, 2), NEG, faces=every(negative(4)), noise=0.0)
    for name, x in (('right_leg', -1.5), ('left_leg', 1.5)):
        M.part(name, pivot=(x, 12, 0)).box((-1.25, 0, -1.25), (2.5, 12, 2.5), NEG, faces=every(negative(5)), noise=0.0)
    return M


# =============================================================== Vigia REM (lustre de olhos)
BRONZE, BRONZE_D = 0x6a5a3a, 0x3a3020
THREAD = 0x5a1a24


def metal(seed):
    return soft(BRONZE, 0.1, 2, seed, 0.15, 0.25)


def hanging_eye(p):
    soft(0xece4dc, 0.03, 3, 5)(p)
    cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
    for y in range(p.h):
        for x in range(p.w):
            d = math.hypot(x - cx, y - cy)
            if d < 0.8:
                p.px(x, y, 0x0a0208, True)
            elif d < 1.6:
                p.px(x, y, 0x8a3ad0, True)


def eye_side(p):
    soft(0xece4dc, 0.03, 3, 6, 0.02, 0.2)(p)
    p.px(0, p.h - 1, 0xc86a6a)


def watcher():
    M = Model('rem_watcher', 64, seed=70)
    crown = M.part('crown', pivot=(0, 6, 0))
    for i, (o, s) in enumerate((((-6, 0, -6), (12, 1, 1)), ((-6, 0, 5), (12, 1, 1)), ((-6, 0, -5), (1, 1, 10)), ((5, 0, -5), (1, 1, 10)),
                                ((-6, 0, -0.5), (12, 1, 1)), ((-0.5, 0, -6), (1, 1, 12)))):
        crown.box(o, s, BRONZE, faces=every(metal(i)), noise=0.0)
    # velas apagadas nos cantos, com pingos
    for i, (x, z) in enumerate(((-5.5, -5.5), (4.5, -5.5), (-5.5, 4.5), (4.5, 4.5))):
        crown.box((x, -3, z), (1, 3, 1), 0xe8e0d0, faces=every(gradient(0xf0e8d8, 0xc8c0b0, i)), noise=0.0)
    # corrente que sobe e some no nada
    chain = crown.child('chain', pivot=(0, 0, 0))
    for k in range(4):
        a = 255 - k * 60

        def link(p, a=a):
            for y in range(p.h):
                for x in range(p.w):
                    p.pxa(x, y, shade(BRONZE_D, 1.0 + 0.1 * ((y + x) % 2)), a)
        chain.box((-0.5, -4 - k * 4, -0.5) if k % 2 == 0 else (-0.5, -4 - k * 4, -0.5), (1, 4, 1), BRONZE_D, faces=every(link), noise=0.0)
    # seis olhos pendurados em fios de comprimentos diferentes
    spots = ((-6, -6, 3), (5, -6, 5), (-6, 5, 4), (5, 5, 2), (0, -6, 6), (0, 5, 4))
    for i, (x, z, n) in enumerate(spots):
        t = crown.child('thread_%d' % i, pivot=(x + 0.5, 1, z + 0.5))
        t.box((-0.5, 0, -0.5), (1, n, 1), THREAD, faces=every(gradient(THREAD, 0x2a0a10, i)), noise=0.0)
        e = t.child('eye_%d' % i, pivot=(0, n, 0))
        e.box((-1.5, 0, -1.5), (3, 3, 3), 0xece4dc, faces={**every(eye_side), 'front': hanging_eye}, noise=0.0)
    return M


# =============================================================== Memória (vestido sépia, cabeça de gaiola com uma luz)
SEPIA, SEPIA_D, SEPIA_L = 0xb8a07c, 0x6a5a44, 0xe0d0b0


def sepia(seed, fade=False):
    def paint(p):
        n = noise_grid(seed * 17 + p.x0, p.w, p.h, 4)
        for y in range(p.h):
            for x in range(p.w):
                k = 1.0 + round(n(x, y) * 0.06 / 0.04) * 0.04
                c = shade(mix(SEPIA_L, SEPIA_D, y / max(1, p.h - 1)), k)
                if fade:
                    # a barra se desfaz: some aos poucos embaixo
                    t = y / max(1, p.h - 1)
                    a = 255 if t < 0.45 else int(255 * max(0.0, 1.0 - (t - 0.45) / 0.55) ** 1.5)
                    if (x * 3 + y) % 4 == 0 and t > 0.6:
                        a = 0
                    p.pxa(x, y, c, a)
                else:
                    p.px(x, y, c)
    return paint


def cage_metal(seed):
    return soft(0x5a4a30, 0.08, 2, seed, 0.2, 0.2)


def memory():
    M = Model('memory', 64, seed=80)
    body = M.part('body', pivot=(0, 9, 0))
    body.box((-2.5, 0, -1.5), (5, 6, 3), SEPIA, faces=every(sepia(1)), noise=0.0)
    skirt = body.child('skirt', pivot=(0, 6, 0))
    skirt.box((-3.5, 0, -2.5), (7, 9, 5), SEPIA, faces=every(sepia(2, True)), noise=0.0)
    for name, x in (('right_arm', -3), ('left_arm', 3)):
        a = body.child(name, pivot=(x, 0.5, 0), rot=(0, 0, 0.12 if x < 0 else -0.12))
        a.box((-0.75, 0, -0.75), (1.5, 7, 1.5), SEPIA, faces=every(sepia(3)), noise=0.0)
    # gaiola no lugar da cabeça
    cage = body.child('cage', pivot=(0, 0, 0))
    cage.box((-3, -1, -3), (6, 1, 6), 0x5a4a30, faces=every(cage_metal(1)), noise=0.0)
    for i, (x, z) in enumerate(((-3, -3), (2, -3), (-3, 2), (2, 2), (-3, -0.5), (2, -0.5))):
        cage.box((x, -7, z), (1, 6, 1), 0x5a4a30, faces=every(cage_metal(2 + i)), noise=0.0)
    cage.box((-2.5, -8, -2.5), (5, 1, 5), 0x5a4a30, faces=every(cage_metal(11)), noise=0.0)
    cage.box((-1, -9, -1), (2, 1, 2), 0x5a4a30, faces=every(cage_metal(12)), noise=0.0)
    cage.box((-0.5, -11, -0.5), (1, 2, 1), 0x5a4a30, faces=every(cage_metal(13)), noise=0.0)
    light = cage.child('light', pivot=(0, -4, 0))
    light.box((-1.5, -2, -1.5), (3, 4, 3), 0xffe8a0, faces=every(gradient(0xfff6d0, 0xffb050, 6, 0.0)), glow=True, noise=0.0)
    return M


# =============================================================== O Sonhador (chefe; o renderer aumenta 1,6x)
PORC, PORC_D = 0xf0eaf4, 0xc8c0d8
GOLD = 0xe8b840
VOID, VOID_L = 0x0a0818, 0x241a48


def starry(seed, ragged=False):
    def paint(p):
        rnd = random.Random(seed * 53 + p.x0)
        n = noise_grid(seed + p.y0, p.w, p.h, 5)
        for y in range(p.h):
            for x in range(p.w):
                t = n(x, y)
                c = mix(VOID, VOID_L, max(0.0, min(1.0, 0.4 + t * 0.6)))
                p.px(x, y, c)
        # estrelas grandes e esparsas (em cruz), não pontilhado
        for _ in range(max(1, p.w * p.h // 60)):
            x, y = rnd.randrange(p.w), rnd.randrange(p.h)
            p.px(x, y, 0xffffff, True)
            if rnd.random() < 0.5:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    p.px(x + dx, y + dy, 0x9a8ad8, True)
        if ragged:
            for x in range(p.w):
                cut = (x * 7 + seed) % 5
                for y in range(p.h - cut, p.h):
                    p.clear(x, y)
    return paint


def mask_face(awake):
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.px(x, y, shade(mix(PORC, PORC_D, (abs(x - p.w / 2) / (p.w / 2)) ** 2 * 0.8 + y / p.h * 0.2), 1.0))
        ey = p.h * 2 // 5
        for side in (-1, 1):
            cx = p.w // 2 + side * 4
            if awake:
                # olhos abertos: buracos para o vazio estrelado
                for y in range(ey - 2, ey + 2):
                    for x in range(cx - 2, cx + 2):
                        p.px(x, y, VOID, True)
                p.px(cx - 1, ey - 1, 0xffffff, True)
                p.px(cx, ey, 0x9a8ad8, True)
            else:
                # pálpebras fechadas: arco + cílios
                for x in range(cx - 2, cx + 2):
                    p.px(x, ey, 0x3a2a5a)
                p.px(cx - 3, ey - 1, 0x3a2a5a)
                p.px(cx + 2, ey - 1, 0x3a2a5a)
            # tinta escorrendo dos olhos até o queixo
            for y in range(ey + 1, p.h - 2 - (side + 1)):
                p.px(cx + (0 if side < 0 else -1), y, 0x1a1030, awake)
        # boca pequena fechada
        p.rect(p.w // 2 - 1, p.h - 5, 2, 1, 0x8a6a9a)
        # rachaduras douradas (kintsugi), linhas contínuas
        x, y = p.w // 2 + 2, 0
        while y < p.h * 2 // 5 - 3:
            p.px(x, y, GOLD, True)
            y += 1
            x += 1 if y % 3 == 0 else 0
        x, y = 1, p.h // 2 + 2
        while x < p.w // 2 - 4:
            p.px(x, y, GOLD, True)
            x += 1
            y += 1 if x % 4 == 0 else 0
    return paint


def porcelain(seed):
    return gradient(PORC, PORC_D, seed, 0.03)


def dreamer(awake):
    M = Model('the_dreamer' + ('_awake' if awake else ''), 128, seed=90)
    head = M.part('head', pivot=(0, -14, 0))
    head.box((-9, -20, -2), (18, 22, 2), PORC, faces={**every(porcelain(1)), 'front': mask_face(awake)}, noise=0.0)
    head.box((-10, -21, 0), (20, 23, 9), VOID, faces=every(starry(2)), noise=0.0)
    veil = head.child('veil', pivot=(0, 2, 4))
    veil.box((-9, 0, -4), (18, 10, 8), VOID, faces=every(starry(3)), noise=0.0)
    v2 = veil.child('veil_2', pivot=(0, 10, 0))
    v2.box((-7, 0, -3.5), (14, 10, 7), VOID, faces=every(starry(4)), noise=0.0)
    v3 = v2.child('veil_3', pivot=(0, 10, 0))
    v3.box((-5, 0, -3), (10, 9, 6), VOID, faces=every(starry(5)), noise=0.0)
    v4 = v3.child('veil_4', pivot=(0, 9, 0))
    v4.box((-3, 0, -2.5), (6, 9, 5), VOID, faces=every(starry(6, True)), noise=0.0)
    # mãos enormes, soltas no ar
    for name, x in (('right_hand', -20), ('left_hand', 20)):
        hnd = M.part(name, pivot=(x, -8, -6))
        hnd.box((-4, -4, -1.5), (8, 8, 3), PORC, faces=every(porcelain(7)), noise=0.0)
        for i in range(4):
            fx = -3.5 + i * 2.2
            f = hnd.child('%s_finger_%d' % (name, i), pivot=(fx + 0.75, -4, 0), rot=(0.2 * (i % 2), 0, (fx) * 0.04))
            f.box((-0.75, -9 + abs(i - 1.5) * 1.5, -0.75), (1.5, 9 - abs(i - 1.5) * 1.5, 1.5), PORC, faces=every(porcelain(8 + i)), noise=0.0)
        th = hnd.child('%s_thumb' % name, pivot=((4 if x < 0 else -4), 0, 0), rot=(0, 0, (-0.9 if x < 0 else 0.9)))
        th.box((-0.75, -6, -0.75), (1.5, 6, 1.5), PORC, faces=every(porcelain(12)), noise=0.0)
    # órbita: relógio, porta e vela girando em volta da cabeça
    halo = M.part('halo', pivot=(0, -24, 2))

    def clock(p):
        p.fill(0xe8d8a0, 0)
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                if math.hypot(x - cx, y - cy) > p.w / 2:
                    p.clear(x, y)
                elif math.hypot(x - cx, y - cy) > p.w / 2 - 1:
                    p.px(x, y, GOLD)
        for k in range(3):
            p.px(int(cx), int(cy) - k, 0x2a2030)
            p.px(int(cx) + k, int(cy), 0x2a2030)
    halo.child('clock', pivot=(17, 0, 0), rot=(0, 0.6, 0.3)).box((-3.5, -3.5, -0.5), (7, 7, 1), 0xe8d8a0, faces={'front': clock, 'back': clock}, noise=0.0)

    def door(p):
        for y in range(p.h):
            for x in range(p.w):
                edge = x in (0, p.w - 1) or y == 0
                p.px(x, y, 0x3a2418 if edge else shade(0x7a4a2a, 1.0 - 0.15 * (y % 4 == 0)))
        p.px(p.w - 2, p.h // 2, GOLD, True)
    halo.child('door', pivot=(-17, 0, 0), rot=(0, -0.5, -0.2)).box((-2.5, -5, -0.5), (5, 10, 1), 0x7a4a2a, faces=every(door), noise=0.0)
    cnd = halo.child('candle', pivot=(0, 0, 17), rot=(0.3, 0, 0))
    cnd.box((-1, -3, -1), (2, 6, 2), 0xf0e8d8, faces=every(gradient(0xf8f0e0, 0xd8d0c0, 9)), noise=0.0)
    cnd.box((-0.5, -5, -0.5), (1, 2, 1), 0xffc040, faces=every(gradient(0xffffa0, 0xff8020, 10, 0.0)), glow=True, noise=0.0)
    return M


# =============================================================== ovos
def egg(name, base, spots):
    src = Image.open(T + 'item/gnome_spawn_egg.png').convert('RGBA')
    out = Image.new('RGBA', src.size, (0, 0, 0, 0))
    sp, op = src.load(), out.load()
    n = noise_grid(zlib.crc32(name.encode()) & 0xffff, src.width, src.height, 4)
    for y in range(src.height):
        for x in range(src.width):
            c = sp[x, y]
            if not c[3]:
                continue
            lum = (c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11) / 255
            v = n(x, y)
            col = spots[0] if v > 0.35 else (spots[1] if v < -0.45 else base)
            pix = shade(col, 0.55 + lum * 0.7)
            op[x, y] = pix + (255,)
    out.save(T + 'item/%s.png' % name)


if __name__ == '__main__':
    build(whale(), glow=True)
    for v in range(3):
        build(sky_fish(v))
    build(moth(), glow=True)
    build(echo(), glow=True)
    build(watcher(), glow=True)
    build(memory(), glow=True)
    build(dreamer(False), glow=True)
    build(dreamer(True), glow=True)
    egg('cloud_whale_spawn_egg', 0xb9b4e8, [0xfbfbff, 0x8f88c8])
    egg('sky_fish_spawn_egg', 0x8ae8ff, [0xff9ad5, 0xffe08a])
    egg('prism_moth_spawn_egg', 0x2a2030, [0xd8304a, 0x3a5ad8])
    egg('echo_spawn_egg', 0xb8e8f0, [0x05080a, 0x6aa8b8])
    egg('rem_watcher_spawn_egg', 0x6a5a3a, [0xece4dc, 0x8a3ad0])
    egg('memory_spawn_egg', 0xb8a07c, [0xffe8a0, 0x5a4a30])
    egg('the_dreamer_spawn_egg', 0x0a0818, [0xf0eaf4, 0xe8b840])
    print('ok ovos')
