"""Folclore novo v2 — modelos e texturas refeitos (Matinta, Caipora, Iara, Cuca, Mapinguari).
Uso (raiz do projeto): python tools/lore/models2.py [nome ...]
Saída: textures/entity/<nome>.png (+ _glow), build/lore/<nome>_layer.txt e build/lore/<nome>_view.png.
Estilo: cores lisas com luz por face (lpaint), rostos e detalhes desenhados em grade ASCII.
Os nomes das partes animadas nos *Model.java são mantidos."""
import os
import sys

sys.path.insert(0, 'tools/modelgen')
sys.path.insert(0, 'tools/boitata')
sys.path.insert(0, 'tools/lore')
import preview
import zview
from lpaint import LModel, ramp, soft, fur, feathers, scales, scutes, cloth, wood, solid, faces, mul

T = 'src/main/resources/assets/mistech/textures/entity/'
OUT = 'build/lore/'
os.makedirs(OUT, exist_ok=True)


def art(lines, pal, base=None, ox=0, oy=0, glow='', center=False):
    """Função de face: pinta a base (outra função de face) e desenha a arte por cima."""
    def f(p):
        if base:
            base(p)
        p.art(lines, pal, ox=ox, oy=oy, glowkeys=glow, center=center)
    return f


def then(*fns):
    def f(p):
        for fn in fns:
            fn(p)
    return f


# =================================================================== Matinta Pereira — pássaro
# rasga-mortalha escura: penas em escama marrom-carvão, disco facial claro, olhos de fogo
F = ramp(0x3a2e2a, dark=0.45, light=1.45)          # pena escura
FB = ramp(0x6a5444, dark=0.55, light=1.35)          # peito
DISC = ramp(0xc8b496, dark=0.6, light=1.15)
EYE, EYE_H, PUP = 0xffc21a, 0xfff2a0, 0x1a0804
SEED = 0xc8201a                                      # contas de tento (vermelhas)


def owl_face(p):
    soft(F)(p)
    p.art([
        "kdddddk",
        "dYYdYYd",
        "dwPdPwd",
        "kddbddk",
        ".kdbdk.",
    ], {'k': F[1], 'd': DISC[2], 'Y': EYE, 'P': PUP, 'w': EYE_H, 'b': 0x2a2420}, glowkeys='YwP')


def owl_chest(p):
    p.fill(FB[2])
    for y in range(2, p.h, 2):  # chevrons escuros espalhados
        for x in range((y // 2) % 2 * 2, p.w, 4):
            p.px(x, y, FB[1])
            p.px(x + 1, y + 1 if y + 1 < p.h else y, FB[1])
    p.art(["s.o.s."[:p.w]], {'s': SEED, 'o': 0x2a1a10}, ox=0, oy=0)  # colar de sementes


def wing_face(p):
    feathers(F, fw=4, fh=3)(p)
    if p.side:
        for x in range(p.w):  # rêmiges com faixas claras
            p.px(x, p.h - 1, F[0])
            if (x % 2 == 0) and p.h > 4:
                p.px(x, p.h - 3, FB[3])


def bird():
    M = LModel('matinta_bird', 64, seed=51)
    M.part('body', pivot=(0, 19, 0), rot=(0.35, 0, 0)).box((-3, -3.5, -3), (6, 7, 6), F[2],
                                                           faces=faces(feathers(F), front=owl_chest, top=soft(F)))
    head = M.part('head', pivot=(0, 16, -1)).box((-3.5, -5, -3), (7, 5, 6), F[2],
                                                  faces=faces(feathers(F, fw=3, fh=2), front=owl_face, top=soft(F)))
    head.child('beak_tip', pivot=(0, -1.5, -3)).box((-0.5, 0, -1), (1, 1, 1), 0x2a2420)
    head.child('right_tuft', pivot=(-2.5, -5, -1), rot=(-0.2, 0, -0.5)).box((-0.5, -2.5, -0.5), (1.5, 2.5, 1), F[1])
    head.child('left_tuft', pivot=(2.5, -5, -1), rot=(-0.2, 0, 0.5)).box((-1, -2.5, -0.5), (1.5, 2.5, 1), F[1])
    M.part('right_wing', pivot=(-3, 16.5, -2), rot=(0.35, 0, 0)).box((-1, 0, 0), (1, 6, 7), F[2], faces=faces(wing_face))
    M.part('left_wing', pivot=(3, 16.5, -2), rot=(0.35, 0, 0)).box((0, 0, 0), (1, 6, 7), F[2], faces=faces(wing_face))
    tail = M.part('tail', pivot=(0, 21.5, 2.5), rot=(0.6, 0, 0)).box((-2, 0, 0), (4, 1, 5), F[2], faces=faces(wing_face))
    tail.child('tail_ribbon', pivot=(1, 0.5, 4.5)).box((-0.5, 0, 0), (1, 3, 0.5), SEED)  # fitinha vermelha da bruxa
    for side, x in (('right_leg', -1.3), ('left_leg', 1.3)):
        M.part(side, pivot=(x, 22, 0)).box((-0.5, 0, -0.5), (1, 2, 1), 0x8a7048).box((-1, 1.5, -1.5), (2, 0.5, 2.5), 0x6a5434)
    return M


# =================================================================== Matinta Pereira — bruxa velha
SK = ramp(0x9c9172, dark=0.55, light=1.25)           # pele velha
SH = ramp(0x2a2030, dark=0.5, light=1.6)             # xale preto-arroxeado
SKT = ramp(0x4a3226, dark=0.5, light=1.4)            # saia
PATCH = ramp(0x6a5a2a)
HAIR = ramp(0xd8d4c8, dark=0.65, light=1.1)


def hag_face(p):
    soft(SK, edge=False)(p)
    p.art([
        "hhh..hhh",
        "hWW..WWh",
        "sYPssPYs",
        "cddccddc",
        "c......c",
        "c.tmmm.c",
        "..mmmt.v",
        "...ss...",
    ], {'h': HAIR[2], 'W': HAIR[3], 's': SK[1], 'Y': 0xffd23a, 'P': 0x1a0a04, 'c': SK[1], 'd': SK[0],
        't': 0xeee4c8, 'm': 0x2a1010, 'v': 0x5a4a2a}, glowkeys='YP')


def shawl(p):
    cloth(SH, every=4, fringe=True)(p)
    if p.face == 'back' and p.w >= 6:  # remendo e bordado de lua
        p.art([".u.", "u..", ".u."], {'u': 0x8a7ab0}, ox=p.w // 2 - 1, oy=2)


def skirt(p):
    cloth(SKT, every=4, hem=0x8a2a1a, hem2=0xc84a24)(p)
    if p.face in ('front', 'left') and p.w >= 5:
        p.art(["ppp", "pqp", "ppp"], {'p': PATCH[2], 'q': PATCH[1]}, ox=1, oy=3)


def hair(p):
    fur(HAIR, seed=3, lock=1, period=4)(p)


def witch():
    M = LModel('matinta_witch', 128, seed=52)
    M.part('skirt', pivot=(0, 12, 0)).box((-4.5, 0, -3), (9, 12, 6), SKT[2], faces=faces(skirt, top=soft(SKT)))
    body = M.part('body', pivot=(0, 12, 0.5), rot=(0.4, 0, 0)).box((-4, -9, -2.5), (8, 9, 5), SH[2], faces=faces(shawl))
    body.child('hump', pivot=(0, -7, 2.5)).box((-3.5, -2.5, -1), (7, 5, 2.5), SH[2], faces=faces(shawl))
    body.child('beads', pivot=(0, -8.5, -2.6), rot=(-0.1, 0, 0)).box((-2.5, 0, -0.4), (5, 3, 0.4), SEED,
                                                                      faces=faces(art(["s...s", ".s.s.", "..o.."], {'s': SEED, 'o': 0xe8dcc0}, base=lambda p: p.fill(SH[1]))))
    # penas de coruja no ombro
    for i, (x, rz) in enumerate(((-3.5, -0.5), (-2.5, -0.2))):
        body.child('shoulder_feather_%d' % i, pivot=(x, -9, 0), rot=(0, 0, rz)).box((-0.5, -3, -0.5), (1, 3, 1), F[2], faces=faces(soft(F)))
    head = M.part('head', pivot=(0, 4.5, -2.5)).box((-4, -8, -4), (8, 8, 8), SK[2], faces=faces(soft(SK), front=hag_face, top=hair))
    nose = head.child('nose', pivot=(0, -3.5, -4), rot=(0.35, 0, 0)).box((-1, -1, -2.5), (2, 2.5, 2.5), SK[3], faces=faces(soft(SK)))
    nose.child('nose_tip', pivot=(0, 1.2, -2.5), rot=(0.6, 0, 0)).box((-0.5, -0.5, -1.2), (1, 1.2, 1.2), SK[2], faces=faces(soft(SK)))
    hood = head.child('hood', pivot=(0, 0, 0)).box((-4.6, -8.7, -4.6), (9.2, 2, 9.2), SH[2], faces=faces(shawl, top=soft(SH)))
    hood.child('hood_r', pivot=(-4.6, -6.7, -4.6)).box((-0.5, 0, 0), (1, 6, 9.2), SH[2], faces=faces(shawl))
    hood.child('hood_l', pivot=(4.6, -6.7, -4.6)).box((-0.5, 0, 0), (1, 6, 9.2), SH[2], faces=faces(shawl))
    hood.child('drape', pivot=(0, -6.7, 4.1)).box((-4.6, 0, 0), (9.2, 9, 1.2), SH[2], faces=faces(shawl))
    head.child('hair_r', pivot=(-4.2, -3, -3.4), rot=(0, 0, 0.25)).box((-1, 0, 0), (1.5, 5, 2), HAIR[2], faces=faces(hair))
    head.child('hair_l', pivot=(4.2, -3, -3.4), rot=(0, 0, -0.25)).box((-0.5, 0, 0), (1.5, 5, 2), HAIR[2], faces=faces(hair))
    ra = M.part('right_arm', pivot=(-4.5, 5, -1)).box((-1.5, 0, -1), (2, 9, 2), SH[2], faces=faces(shawl))
    ra.child('right_hand', pivot=(-0.5, 9, 0)).box((-1, 0, -1), (2, 2, 2), SK[2], faces=faces(soft(SK)))
    for i in range(3):
        ra.child('right_nail_%d' % i, pivot=(-1 + i * 0.75, 11, -0.6), rot=(-0.3, 0, 0)).box((-0.2, 0, -0.2), (0.4, 1.5, 0.4), 0x3a3020)
    la = M.part('left_arm', pivot=(4.5, 5, -1)).box((-0.5, 0, -1), (2, 9, 2), SH[2], faces=faces(shawl))
    la.child('left_hand', pivot=(0.5, 9, 0)).box((-1, 0, -1), (2, 2, 2), SK[2], faces=faces(soft(SK)))
    W = ramp(0x5a3a22)
    cane = la.child('cane', pivot=(0.5, 10, -0.5)).box((-0.5, -3, -0.5), (1, 15, 1), W[2], faces=faces(wood(W)))
    cane.child('cane_knob', pivot=(0, -3, 0), rot=(0, 0.6, 0.4)).box((-1, -2, -1), (2, 2, 2), W[1], faces=faces(soft(W)))
    cane.child('cane_skull', pivot=(0, -4.5, 0)).box((-0.75, -1.5, -0.75), (1.5, 1.5, 1.5), 0xe8dcc0,
                                                     faces=faces(soft(ramp(0xe8dcc0)), front=art(["k.k", "..."], {'k': 0x2a1a10})))
    cane.child('cane_ribbon', pivot=(0.5, -2, 0)).box((0, 0, -0.25), (0.5, 4, 0.5), SEED)
    return M


# =================================================================== Caipora no queixada
# Caipora pequena e atarracada, pele morena-avermelhada com pintura de urucum e jenipapo, juba de fogo,
# olhos grandes e sorriso maroto, cachimbo (ela adora fumo) e borduna com penas, montada no queixada.
PF = ramp(0x34302e, dark=0.5, light=1.5)             # pelo do queixada
PW = ramp(0xe6dcc6, dark=0.7, light=1.08)            # branco do queixo
CS = ramp(0x9a5432, dark=0.55, light=1.3)            # pele da Caipora
HR = ramp(0xe0401a, dark=0.55, light=1.35, hue=-0.04)  # cabelo de fogo
URU, JEN = 0xd8261a, 0x1a1414                        # urucum, jenipapo
STRAW = ramp(0xc8a050, dark=0.55, light=1.2)
GREEN = 0x2a8a3a


def pig_side(p):
    fur(PF, seed=11, lock=2, period=7)(p)
    if p.side:
        for x in range(p.w):
            p.px(x, p.h - 1, PF[0])


def pig_face(p):
    fur(PF, seed=12, lock=1, period=4, tips=False)(p)
    p.art([
        ".......",
        ".rk.kr.",
        ".......",
        ".......",
        ".......",
        "wwwwwww",
        "wwwwwww",
    ], {'r': 0xff3a1a, 'k': 0x140808, 'w': PW[2]}, glowkeys='r')


def pig_jaw_side(p):
    pig_side(p)
    if p.side and p.h > 3:
        for y in (p.h - 2, p.h - 1):
            for x in range(p.w if p.face in ('right', 'left') else 0):
                if x < 4:
                    p.px(x, y, PW[2])


def snout_front(p):
    p.fill(0x6a4a46)
    p.art(["kkkkk", "k.k.k", "kkkkk"], {'k': 0x4a3230}, oy=0)
    p.art([".n.n."], {'n': 0x1a0a0a}, oy=1)


def blanket(p):
    p.fill(0x7a2a18)
    for y in range(p.h):  # grafismo marajoara em zigue-zague
        for x in range(p.w):
            z = (x + y) % 4
            if z == 0:
                p.px(x, y, 0x1a1414)
            elif (x - y) % 4 == 0:
                p.px(x, y, 0xe8c87a)
    if p.side:
        p.row(p.h - 1, 0xe8c87a)


def cskin(p):
    soft(CS)(p)


def caipora_face(p):
    soft(CS, edge=False)(p)
    p.art([
        "........",
        ".kk..kk.",
        "rWWrrWWr",
        "rWPrrPWr",
        "...nn...",
        "........",
        ".m....m.",
        "..mttm..",
    ], {'k': JEN, 'r': URU, 'W': 0xf8f4e8, 'P': 0x2a1408, 'n': CS[1], 'm': 0x4a1a10, 't': 0xf8f0e0})


def caipora_body(p):
    cskin(p)
    if p.face == 'front':
        p.art(["r.r.r.", ".r.r.r", "......", "k.k.k.", ".kkkk."], {'r': URU, 'k': JEN}, center=True, oy=1)
    elif p.face == 'back':
        p.art(["kk..kk", "..kk..", "kk..kk"], {'k': JEN}, center=True, oy=2)


def fire_hair(p):
    fur(HR, seed=21, lock=2, period=5)(p)
    if p.side:  # pontas amarelas que brilham
        for x in range(p.w):
            if (x * 3) % 5 < 2:
                p.px(x, p.h - 1, 0xffc030, glow=True)
                if p.h > 2:
                    p.px(x, p.h - 2, 0xff8a20, glow=True)


def fire_top(p):
    p.fill(HR[3])
    for y in range(p.h):
        for x in range(p.w):
            if (x * 7 + y * 3) % 11 == 0:
                p.px(x, y, 0xffc030, glow=True)
            elif (x * 5 + y * 2) % 9 == 0:
                p.px(x, y, HR[2])


def straw(p):
    for y in range(p.h):
        for x in range(p.w):
            p.px(x, y, STRAW[1] if x % 2 else STRAW[2 if y % 3 else 3])
    if p.side:
        for x in range(0, p.w, 2):
            p.px(x, p.h - 1, STRAW[0])


def caipora():
    M = LModel('caipora', 128, seed=53)
    # --- queixada ---
    pig = M.part('pig', pivot=(0, 13, 1)).box((-5, -5, -8), (10, 10, 16), PF[2], faces=faces(pig_side, top=fur(PF, seed=13)))
    for i, z in enumerate((-7, -4.5, -2, 0.5, 3, 5.5)):  # crista de cerdas
        h = (3, 4, 4.5, 4, 3, 2)[i]
        pig.child('bristle_%d' % i, pivot=(0, -5, z), rot=(-0.45, 0, 0)).box((-1, -h, -1), (2, h, 2), PF[0], faces=faces(fur(PF, seed=40 + i, lock=1)))
    pig.child('blanket', pivot=(0, -5, -1)).box((-5.5, 0, -3.5), (11, 4, 7), 0x7a2a18, faces=faces(blanket))
    pig.child('pig_tail', pivot=(0, -3, 8), rot=(0.5, 0, 0)).box((-0.5, 0, 0), (1, 3, 1), PF[1])
    head = M.part('pig_head', pivot=(0, 12, -7)).box((-4, -4, -6), (8, 8, 6), PF[2], faces=faces(pig_jaw_side, front=pig_face, top=fur(PF, seed=14)))
    sn = head.child('snout', pivot=(0, 1, -6)).box((-2.5, -1.5, -3), (5, 4, 3), 0x5a4240, faces=faces(soft(ramp(0x5a4240)), front=snout_front))
    sn.child('bridle', pivot=(0, -1.6, -1.5)).box((-2.7, 0, -0.6), (5.4, 0.8, 1.2), 0x8a6a3a)
    for side, x, rz in (('right_tusk', -2.6, -0.3), ('left_tusk', 2.6, 0.3)):
        t = sn.child(side, pivot=(x, 2, -2), rot=(-0.2, 0, rz)).box((-0.5, -3, -0.5), (1, 3, 1), PW[2], faces=faces(soft(PW)))
        t.child(side + '_tip', pivot=(0, -3, 0), rot=(-0.9, 0, 0)).box((-0.5, -2, -0.5), (1, 2, 1), PW[3], faces=faces(soft(PW)))
    for side, x, rz in (('pig_right_ear', -3.5, -0.6), ('pig_left_ear', 3.5, 0.6)):
        head.child(side, pivot=(x, -4, -1), rot=(0.3, 0, rz)).box((-1, -3, 0), (2, 3, 1), PF[1], faces=faces(soft(PF)))
    head.child('feather_charm', pivot=(-3.6, -3, -1), rot=(0, 0, 0.3)).box((-0.25, 0, -0.5), (0.5, 4, 1), URU,
                                                                           faces=faces(art(["y", "y", "r", "g"], {'y': 0xffd040, 'r': URU, 'g': GREEN})))
    for name, x, z in (('leg_fr', -3, -5.5), ('leg_fl', 3, -5.5), ('leg_br', -3, 6.5), ('leg_bl', 3, 6.5)):
        M.part(name, pivot=(x, 17, z)).box((-1.5, 0, -1.5), (3, 7, 3), PF[1],
                                           faces=faces(fur(PF, seed=hash(name) % 50, lock=1), bottom=solid(0x1a1410, False)))
    # --- Caipora ---
    rider = M.part('rider', pivot=(0, 8, 1.5))
    rider.box((-3, -7, -2), (6, 7, 4), CS[2], faces=faces(caipora_body))
    rider.child('skirt', pivot=(0, -1, 0)).box((-3.5, 0, -2.5), (7, 3, 5), STRAW[2], faces=faces(straw, top=solid(STRAW[2], False)))
    rider.child('necklace', pivot=(0, -6.5, -2.1)).box((-2.5, 0, -0.3), (5, 2, 0.3), 0xe8dcc0,
                                                      faces=faces(art(["r.r.r", ".t.t."], {'r': URU, 't': PW[3]}, base=lambda p: p.fill(CS[2]))))
    rh = rider.child('rider_head', pivot=(0, -7, 0)).box((-4, -8, -4), (8, 8, 8), CS[2], faces=faces(cskin, front=caipora_face, top=fire_top))
    rh.child('nose', pivot=(0, -3.5, -4)).box((-1, 0, -1), (2, 1.5, 1), CS[3], faces=faces(cskin))
    for side, x in (('right_ear', -4), ('left_ear', 4)):
        rh.child(side, pivot=(x, -4, 0)).box((-0.5, -1, -1), (1, 2, 2), CS[3], faces=faces(cskin))
    # cachimbo na boca (é a "mandíbula" animada: balança quando ela fala / fica brava)
    jaw = rh.child('rider_jaw', pivot=(1.5, -1.5, -4), rot=(0.25, 0, 0))
    W = ramp(0x6a4024)
    jaw.box((-0.5, -0.5, -3), (1, 1, 3), W[2], faces=faces(wood(W)))
    bowl = jaw.child('pipe_bowl', pivot=(0, 0, -3), rot=(-0.25, 0, 0))
    bowl.box((-1, -2.5, -1.5), (2, 3, 2), W[1], faces=faces(soft(W), top=lambda p: p.art(["ey", "ye"], {'e': 0xff5a10, 'y': 0xffc030}, glowkeys='ey')))
    # juba de fogo
    mane = rh.child('hair', pivot=(0, -8, 0)).box((-4.5, -3, -4.5), (9, 3, 9), HR[2], faces=faces(fire_hair, top=fire_top))
    mane.child('hair_back', pivot=(0, 0, 3.5)).box((-5, 0, 0), (10, 11, 2.5), HR[2], faces=faces(fire_hair))
    mane.child('hair_r', pivot=(-4.5, 0, -2)).box((-1.5, 0, 0), (2, 7, 6), HR[2], faces=faces(fire_hair))
    mane.child('hair_l', pivot=(4.5, 0, -2)).box((-0.5, 0, 0), (2, 7, 6), HR[2], faces=faces(fire_hair))
    for i, (x, z, rx, rz) in enumerate(((-2.5, 0, -0.3, -0.4), (0, -1, -0.15, 0), (2.5, 0, -0.3, 0.4), (-1, 2, 0.3, -0.2), (1.5, 2.5, 0.35, 0.25))):
        mane.child('flame_%d' % i, pivot=(x, -3, z), rot=(rx, 0, rz)).box((-1, -3.5, -1), (2, 3.5, 2), HR[3], faces=faces(fire_hair, top=fire_top))
    mane.child('head_feather', pivot=(3, -2, 2), rot=(0.4, 0, 0.5)).box((-0.5, -6, -0.25), (1, 6, 0.5), GREEN,
                                                                         faces=faces(art(["y", "y", "y", "r", "g", "g"], {'y': 0xffd040, 'r': URU, 'g': GREEN})))
    rider.child('rider_right_leg', pivot=(-2, 0, 0), rot=(-1.3, 0, 0.75)).box((-1, 0, -1), (2, 6, 2), CS[2], faces=faces(cskin))
    rider.child('rider_left_leg', pivot=(2, 0, 0), rot=(-1.3, 0, -0.75)).box((-1, 0, -1), (2, 6, 2), CS[2], faces=faces(cskin))
    ra = rider.child('rider_right_arm', pivot=(-3, -6.5, 0), rot=(-0.6, 0, 0.15)).box((-2, 0, -1), (2, 7, 2), CS[2],
                                                                                        faces=faces(then(cskin, lambda p: p.side and p.row(1, URU))))
    club = ra.child('spear', pivot=(-1, 6.5, 0), rot=(1.2, 0, 0))
    club.box((-0.5, -4, -0.5), (1, 12, 1), W[2], faces=faces(wood(W)))
    club.box((-1.5, -9, -1.5), (3, 5, 3), W[1], faces=faces(then(wood(W), lambda p: p.side and p.art(["k.k", ".k.", "k.k"], {'k': JEN}, oy=1))))
    for i, rz in enumerate((-0.5, 0.5)):
        club.child('club_feather_%d' % i, pivot=(0, -4, 0), rot=(0, 0, rz + 3.14)).box((-0.25, -3, -0.5), (0.5, 3, 1), URU,
                                                                                       faces=faces(art(["y", "r", "r"], {'y': 0xffd040, 'r': URU})))
    la = rider.child('rider_left_arm', pivot=(3, -6.5, 0), rot=(-0.9, 0, -0.15)).box((0, 0, -1), (2, 7, 2), CS[2],
                                                                                       faces=faces(then(cskin, lambda p: p.side and p.row(1, URU))))
    la.child('reins', pivot=(1, 7, 0), rot=(0.5, 0, 0)).box((-0.25, 0, -0.25), (0.5, 8, 0.5), 0x8a6a3a)
    return M


# =================================================================== Iara
IS = ramp(0xb07548, dark=0.55, light=1.28)           # pele
IH = ramp(0x1a1620, dark=0.5, light=2.2, hue=-0.08)  # cabelo preto com reflexo azul
TS = ramp(0x1f8a7c, dark=0.45, light=1.5, hue=0.05)  # escamas
TB = ramp(0x8ad8c0, dark=0.6, light=1.15)            # barriga
FIN = ramp(0x6ad0d8, dark=0.55, light=1.3)
GOLD = 0xe8b830
LILY, LILY_D, LILY_C = 0xf6d8e8, 0xd890b8, 0xffd040


def iara_face(p):
    soft(IS, edge=False)(p)
    p.art([
        "hhhhhhhh",
        "hh....hh",
        ".kk..kk.",
        ".WG..GW.",
        ".WE..EW.",
        "...nn...",
        "c..ll..c",
        "........",
    ], {'h': IH[2], 'k': IH[1], 'W': 0xf8f4ec, 'G': 0x2ac89a, 'E': 0x0a5a44, 'n': IS[1], 'c': 0xc8806a,
        'l': 0xb0303a, 'L': 0x8a2030}, glowkeys='GE')


def iara_hair(p):
    fur(IH, seed=31, lock=2, period=7)(p)
    if p.face == 'front' and p.h <= 3:
        return


def iara_body(p):
    soft(IS)(p)
    if p.face == 'front':
        p.art([
            ".g.g.g.",
            "..p.p..",
            "sss.sss",
            "SsS.SsS",
            ".s...s.",
        ], {'g': GOLD, 'p': 0xf8f0f0, 's': 0xf0b8a0, 'S': 0xd88a7a}, oy=0)


def tail_face(p):
    scales(TS, sw=3, sh=2, shine=0x9af0d0)(p)
    if p.face == 'front' and p.w >= 5:  # faixa clara da barriga
        for y in range(p.h):
            for x in range(p.w // 2 - 1, p.w // 2 + 2 - (p.w % 2 == 0)):
                p.px(x, y, TB[1] if y % 2 else TB[2])
    if p.face == 'back':
        p.col(p.w // 2, TS[0])
    # brilho dourado esparso que reluz no escuro
    for (x, y) in ((1, 1), (p.w - 2, p.h - 2)):
        if 0 < x < p.w and 0 < y < p.h and p.face in ('left', 'right'):
            p.px(x, y, 0xffe08a, glow=True)


def tail_top_skin(p):
    """primeiro segmento: a pele vira escama com uma faixa de escamas douradas."""
    tail_face(p)
    if p.side:
        p.row(0, IS[2])
        for x in range(p.w):
            p.px(x, 1, GOLD if x % 2 else TS[3])


def fin_face(p):
    for y in range(p.h):
        for x in range(p.w):
            c = FIN[2] if x % 2 else FIN[3]
            if y == p.h - 1:
                c = 0xe89ab8  # borda rosada
            p.px(x, y, c)
    for x in range(0, p.w, 3):
        p.px(x, p.h - 1, FIN[3], glow=True)


def lily(p):
    p.fill(LILY)
    if p.face == 'top':
        p.art([".d.", "dcd", ".d."], {'d': LILY_D, 'c': LILY_C}, center=True, oy=0, glowkeys='c')
    else:
        p.row(p.h - 1, 0x3a8a3a)


def iara():
    M = LModel('iara', 128, seed=61)
    body = M.part('body', pivot=(0, 8, 0)).box((-3.5, -7, -2), (7, 7, 4), IS[2], faces=faces(iara_body))
    head = M.part('head', pivot=(0, 1, 0)).box((-4, -8, -4), (8, 8, 8), IS[2], faces=faces(soft(IS), front=iara_face))
    hair = head.child('hair', pivot=(0, -8, 0)).box((-4.5, -1, -4.5), (9, 2.5, 9), IH[2], faces=faces(iara_hair, top=soft(IH)))
    hair.child('hair_back', pivot=(0, 1, 4)).box((-4.5, 0, -0.5), (9, 17, 1.5), IH[2], faces=faces(iara_hair))
    hair.child('hair_r', pivot=(-4.5, 1, 1)).box((-0.6, 0, -4.2), (1, 9, 7.5), IH[2], faces=faces(iara_hair))
    hair.child('hair_l', pivot=(4.5, 1, 1)).box((-0.4, 0, -4.2), (1, 9, 7.5), IH[2], faces=faces(iara_hair))
    # mechas na frente dos ombros
    hair.child('lock_r', pivot=(-3.6, 8, -4.3)).box((-1, 0, 0), (2, 8, 1), IH[2], faces=faces(iara_hair))
    hair.child('lock_l', pivot=(3.6, 8, -4.3)).box((-1, 0, 0), (2, 8, 1), IH[2], faces=faces(iara_hair))
    # coroa de flores-d'água (vitória-régia)
    for i, (x, z, s) in enumerate(((3, -2.5, 3), (-3.2, -1.5, 2), (0.5, -4, 2))):
        hair.child('flower' if i == 0 else 'flower_%d' % i, pivot=(x, -0.5, z)).box((-s / 2, -1.5, -s / 2), (s, 1.5, s), LILY, faces=faces(lily))
    hair.child('lily_pad', pivot=(3, -0.6, -1.5), rot=(0, 0.4, 0.15)).box((-2, -0.5, -2), (4, 0.5, 4), 0x2a7a3a,
                                                                           faces=faces(art(["....", ".g..", "...."], {'g': 0x4aa04a}, base=lambda p: p.fill(0x2a7a3a))))
    for side, x0 in (('right_arm', -3.5), ('left_arm', 3.5)):
        ox = -2 if x0 < 0 else 0
        M.part(side, pivot=(x0, 1.5, 0)).box((ox, 0, -1), (2, 7, 2), IS[2], faces=faces(soft(IS))) \
            .box((ox - 0.2, 4.5, -1.2), (2.4, 0.8, 2.4), GOLD, faces=faces(art(["g.g", ".t."], {'g': 0xfff0a0, 't': 0x2ab08a}, base=lambda p: p.fill(GOLD))))
    t0 = M.part('tail0', pivot=(0, 8, 0)).box((-3.5, 0, -2), (7, 6, 4), TS[2], faces=faces(tail_top_skin, top=soft(IS)))
    for side, x, rz in (('hip_fin_r', -3.5, 0.6), ('hip_fin_l', 3.5, -0.6)):
        t0.child(side, pivot=(x, 2, 0), rot=(0, 0, rz)).box((-0.25 if x > 0 else -0.25, 0, -1.5), (0.5, 4, 3), FIN[2], faces=faces(fin_face))
    t1 = t0.child('tail1', pivot=(0, 6, 0)).box((-3, 0, -1.75), (6, 6, 3.5), TS[2], faces=faces(tail_face))
    t2 = t1.child('tail2', pivot=(0, 6, 0)).box((-2.5, 0, -1.5), (5, 5, 3), TS[2], faces=faces(tail_face))
    t2.child('dorsal', pivot=(0, 0, 1.5), rot=(-0.3, 0, 0)).box((-0.25, 0, 0), (0.5, 5, 2), FIN[2], faces=faces(fin_face))
    t3 = t2.child('tail3', pivot=(0, 5, 0)).box((-1.5, 0, -1), (3, 3, 2), TS[2], faces=faces(tail_face))
    fin = t2.child('tail_fin', pivot=(0, 7.5, 0))
    for side, rz in (('fin_r', 0.65), ('fin_l', -0.65)):
        fin.child(side, pivot=(0, 0, 0), rot=(0, 0, rz)).box((-2, 0, -0.5), (4, 7, 1), FIN[2], faces=faces(fin_face))
    return M


# =================================================================== Cuca
GA = ramp(0x4e7030, dark=0.45, light=1.4, hue=0.04)   # couro de jacaré
GB = ramp(0xc8bc80, dark=0.6, light=1.12)             # barriga
RB = ramp(0x40205a, dark=0.45, light=1.55)            # manto roxo
CH = ramp(0x7a7a5e, dark=0.5, light=1.3)             # cabelo seco
TOOTH = 0xf4ecd8
EMB = 0xd8a830                                        # bordado dourado


def gbelly(p):
    for y in range(p.h):
        p.row(y, GB[2] if y % 2 else GB[1])
    for x in range(0, p.w, 3):
        p.col(x, GB[1])


def gator_skin(p):
    scutes(GA, sw=3, sh=3, belly=gbelly)(p)


def teeth_row(top):
    def f(p):
        gator_skin(p)
        if p.side:
            y = p.h - 1 if top else 0
            for x in range(p.w):
                if x % 2 == 0:
                    p.px(x, y, TOOTH)
    return f


def mouth_in(p):
    p.fill(0x7a2026)
    for y in range(1, p.h, 2):
        p.px(p.w // 2, y, 0x5a1218)
    if p.face == 'top':
        p.art(["t" + "." * (p.w - 2) + "t"] * 0 + [], {})


def snout_top(p):
    gator_skin(p)
    for y in range(0, p.h, 2):  # duas fileiras de calombos
        p.px(1, y, GA[3])
        p.px(p.w - 2, y, GA[3])


def gator_eye(p):
    p.fill(GA[2])
    if p.face in ('front', 'left', 'right'):
        p.art(["YY", "YP"] if p.face != 'left' else ["YY", "PY"], {'Y': 0xffd030, 'P': 0x140a04}, glowkeys='YP')


def robe(p):
    cloth(RB, every=4, hem=EMB, hem2=0xfff0a0)(p)
    if p.face == 'front' and p.w >= 9:
        p.art(["..y......", ".yy...*..", ".yy......", "..y..*...", "......*.."], {'y': EMB, '*': 0xfff0a0}, oy=2, center=True)
    if p.face == 'back' and p.w >= 9:
        p.art(["...yyy...", "..y...y..", "..y.o.y..", "..y...y..", "...yyy..."], {'y': EMB, 'o': 0x5aff8a}, oy=3, center=True, glowkeys='o')


def skirt_robe(p):
    cloth(RB, every=3, hem=EMB)(p)
    if p.side:
        for x in range(1, p.w, 3):  # runas verdes na barra
            p.px(x, p.h - 3, 0x5aff8a, glow=True)


def vial(color):
    def f(p):
        p.fill(color)
        if p.side:
            p.px(0, 0, 0xffffff, glow=True)
            p.row(p.h - 1, mul(color, 0.7))
        for y in range(p.h):
            for x in range(p.w):
                p.px(x, y, p.get(x, y), glow=True)
    return f


def stringy(p):
    fur(CH, seed=71, lock=1, period=5)(p)
    if p.side and p.h > 4:
        for x in range(p.w):  # pontas ralas, desiguais
            if (x * 5) % 3 == 0:
                p.px(x, p.h - 1, CH[0])
                p.px(x, p.h - 2, CH[1])


def cuca():
    M = LModel('cuca', 128, seed=62)
    M.part('skirt', pivot=(0, 13, 0)).box((-6, 0, -4.5), (12, 11, 9), RB[2], faces=faces(skirt_robe, top=soft(RB)))
    body = M.part('body', pivot=(0, 13, 0.5), rot=(0.25, 0, 0)).box((-5, -12, -3), (10, 12, 6), RB[2], faces=faces(robe))
    belt = body.child('belt', pivot=(0, -2, 0)).box((-5.4, 0, -3.4), (10.8, 1.5, 6.8), 0x3a2414, faces=faces(solid(0x3a2414)))
    for i, (x, c) in enumerate(((-3.5, 0x5aff6a), (-1.5, 0xc040ff), (3, 0xff4a2a))):
        v = belt.child('vial_%d' % i, pivot=(x, 1, -3.6)).box((-0.75, 0, -1), (1.5, 2.5, 1.5), c, faces=faces(vial(c)))
        v.box((-0.4, -0.6, -0.65), (0.8, 0.6, 0.8), 0x8a6a3a)
    neck = body.child('necklace', pivot=(0, -11.5, -3.1))
    neck.box((-3.5, 0, -0.4), (7, 1, 0.4), 0xd8d0b8, faces=faces(art([".t.t.t."], {'t': 0x8a7a5a}, base=lambda p: p.fill(0xd8d0b8))))
    neck.child('skull', pivot=(0, 1, 0)).box((-1, 0, -1), (2, 2, 1), 0xe8dcc0,
                                            faces=faces(soft(ramp(0xe8dcc0)), front=art(["kk", ".."], {'k': 0x1a1010})))
    # cabeça de jacaré
    head = M.part('head', pivot=(0, 1.5, -1.5)).box((-4, -6, -5), (8, 6, 8), GA[2], faces=faces(gator_skin))
    up = head.child('snout', pivot=(0, -2, -5)).box((-3, -2.5, -10), (6, 3, 10), GA[2],
                                                   faces=faces(teeth_row(True), top=snout_top, bottom=mouth_in))
    up.child('nostrils', pivot=(0, -2.5, -9)).box((-1.5, -1, -1), (3, 1, 2), GA[1],
                                                  faces=faces(soft(GA), top=art(["k.k", "..."], {'k': 0x141008})))
    for i, x in enumerate((-3.3, 3.3)):  # dentes de fora (caninos)
        up.child('fang_%d' % i, pivot=(x * 0.85, 0.5, -8.5)).box((-0.3, 0, -0.3), (0.6, 1.5, 0.6), TOOTH)
    head.child('jaw', pivot=(0, 0, -4), rot=(0.25, 0, 0)).box((-3, 0, -11), (6, 2, 11), GA[2],
                                                             faces=faces(teeth_row(False), top=mouth_in, bottom=gbelly))
    for side, x in (('right_eye', -3), ('left_eye', 3)):
        head.child(side, pivot=(x, -6, -3)).box((-1.25, -2, -1.25), (2.5, 2, 2.5), GA[2], faces=faces(gator_eye, top=gator_skin))
    hair = head.child('hair', pivot=(0, -6, 0.5)).box((-4.5, -1.5, -2), (9, 2.5, 6.5), CH[2], faces=faces(stringy))
    hair.child('hair_back', pivot=(0, 0.5, 4)).box((-4.5, 0, -0.5), (9, 11, 1.5), CH[2], faces=faces(stringy))
    hair.child('hair_r', pivot=(-4.5, 0.5, 0)).box((-1, 0, 0), (1.5, 7, 4), CH[2], faces=faces(stringy))
    hair.child('hair_l', pivot=(4.5, 0.5, 0)).box((-0.5, 0, 0), (1.5, 7, 4), CH[2], faces=faces(stringy))
    hair.child('bone_pin', pivot=(2.5, -1, 1), rot=(0, 0, 0.6)).box((-0.5, -5, -0.5), (1, 6, 1), 0xe8dcc0, faces=faces(soft(ramp(0xe8dcc0))))
    hair.child('crow_feather', pivot=(-2.5, -1, 2), rot=(-0.3, 0, -0.5)).box((-0.25, -6, -0.75), (0.5, 6, 1.5), 0x1a1a2a,
                                                                            faces=faces(art(["b", ".", "b", ".", ".", "."], {'b': 0x3a4a7a}, base=lambda p: p.fill(0x1a1a2a))))
    for side, x, sgn in (('right_arm', -5.5, -1), ('left_arm', 5.5, 1)):
        ox = -2.5 if sgn < 0 else -0.5
        arm = M.part(side, pivot=(x, 3, -0.5)).box((ox, 0, -1.5), (3, 9, 3), RB[2], faces=faces(cloth(RB, every=3)))
        arm.child(side + '_sleeve', pivot=(ox + 1.5, 7, 0)).box((-2.2, 0, -2.2), (4.4, 3, 4.4), RB[2], faces=faces(cloth(RB, every=3, hem=EMB)))
        hand = arm.child(side.replace('arm', 'hand'), pivot=(ox + 1.5, 10, 0)).box((-1.5, 0, -1.5), (3, 2, 3), GA[2], faces=faces(gator_skin))
        for i in range(3):
            hand.child('%s_claw_%d' % (side, i), pivot=((i - 1) * 0.9, 2, -1), rot=(-0.4, 0, 0)).box((-0.3, 0, -0.3), (0.6, 2, 0.6), TOOTH)
        if sgn < 0:
            W = ramp(0x5a3a20)
            ladle = hand.child('ladle', pivot=(0, 1, -0.5), rot=(-0.4, 0, 0)).box((-0.5, -3, -0.5), (1, 11, 1), W[2], faces=faces(wood(W)))
            ladle.box((-1.5, 7.5, -1.5), (3, 2, 3), 0x3a3a3a, faces=faces(solid(0x3a3a3a), top=lambda p: p.fill(0x5aff6a) or [p.px(x, y, 0x8aff8a, glow=True) for y in range(p.h) for x in range(p.w)]))
    t0 = M.part('tail0', pivot=(0, 21, 4), rot=(-0.25, 0, 0)).box((-2.5, -2, 0), (5, 4, 7), GA[2], faces=faces(gator_skin))
    t1 = t0.child('tail1', pivot=(0, 0, 7), rot=(0.15, 0, 0)).box((-2, -1.5, 0), (4, 3, 7), GA[2], faces=faces(gator_skin))
    t2 = t1.child('tail2', pivot=(0, 0, 7), rot=(0.1, 0, 0)).box((-1.5, -1, 0), (3, 2, 7), GA[2], faces=faces(gator_skin))
    for seg, (h0, n) in ((t0, (-2, 3)), (t1, (-1.5, 3)), (t2, (-1, 3))):
        for i in range(n):  # crista de escudos
            seg.child('%s_ridge_%d' % (seg.name, i), pivot=(0, h0, 1 + i * 2.2)).box((-0.5, -1, -0.5), (1, 1, 1.5), GA[1], faces=faces(soft(GA)))
    return M


# =================================================================== Mapinguari
MF = ramp(0x7a3a1e, dark=0.45, light=1.45, hue=0.03)   # pelo ruivo comprido
MS = ramp(0x3e2c26, dark=0.55, light=1.35)              # pele nua (rosto, barriga, pés)
AR = ramp(0x4a4a3a, dark=0.5, light=1.45)               # couraça
MOSS = (0x3a6a2a, 0x5a8a34)
BONE2 = ramp(0xd8ccae, dark=0.55, light=1.12)


def mfur(seed):
    def f(p):
        fur(MF, seed=seed, lock=3, period=9)(p)
        if p.side:
            for y in range(p.h - 1):  # risca entre as mechas
                for x in range(2, p.w, 3):
                    if (y * 3 + x * 7 + seed) % 7 > 1:
                        p.px(x, y, MF[1])
        rnd = __import__('random').Random(seed * 13 + p.w)
        if p.face in ('top', 'back', 'left', 'right') and p.w * p.h > 30:
            for _ in range(p.w * p.h // 140):  # musgo e folhinhas presos no pelo
                x, y = rnd.randrange(p.w), rnd.randrange(p.h)
                p.px(x, y, MOSS[0])
                p.px(x + 1, y, MOSS[1])
    return f


def belly_mouth(p):
    mfur(5)(p)
    w, h = p.w, p.h
    x0, x1, y0, y1 = 3, w - 4, 5, 11
    for y in range(y0 - 1, y1 + 1):
        for x in range(x0 - 1, x1 + 2):
            p.px(x, y, MS[1])
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p.px(x, y, 0x1a0606)
    for y in range(y0 + 2, y1 + 1):
        for x in range(x0 + 2, x1 - 1):
            p.px(x, y, 0x8a1a10 if (x + y) % 3 else 0xc8341a, glow=True)  # garganta em brasa
    for x in range(x0, x1 + 1):  # dentes de cima (desiguais)
        p.px(x, y0, BONE2[2] if x % 2 == 0 else 0x1a0606)
        if x % 4 == 0:
            p.px(x, y0 + 1, BONE2[1])
    # língua pendente
    for y in range(y1 - 2, y1 + 1):
        for x in range(w // 2 - 2, w // 2 + 2):
            p.px(x, y, 0xb04a5a)
    p.px(w // 2 - 1, y1 - 1, 0x8a2a3a)


def lip_face(p):
    mfur(6)(p)
    if p.face == 'front':
        p.row(0, MS[1])
        for x in range(1, p.w - 1, 2):
            p.px(x, 0, BONE2[2])
    if p.face == 'top':
        p.fill(0x1a0606)
        for x in range(0, p.w, 2):
            p.px(x, p.h - 1, BONE2[2])


def cyclops_face(p):
    mfur(7)(p)
    p.art([
        "..BBBBBBB..",
        ".BkkkkkkkB.",
        ".sWWYYYWWs.",
        ".sWYYPYYWs.",
        ".sWWYYYWWs.",
        "..sssssss..",
        "...snnns...",
        "...sssss...",
        "....sss....",
    ], {'B': MF[0], 'k': MS[0], 's': MS[2], 'W': 0xf0e0b0, 'Y': 0xffb020, 'P': 0x200804, 'n': MS[0]},
        center=True, glowkeys='YP')


def armor(p):
    scutes(AR, sw=4, sh=3)(p)


def claw_box(p):
    for y in range(p.h):
        for x in range(p.w):
            p.px(x, y, MS[0] if y < p.h - 2 else BONE2[1])


def mapinguari():
    M = LModel('mapinguari', 128, seed=63)
    body = M.part('body', pivot=(0, 14, 0), rot=(0.22, 0, 0)).box((-8, -15, -6), (16, 16, 12), MF[2],
                                                                    faces=faces(mfur(1), front=belly_mouth))
    body.child('belly_lip', pivot=(0, -4, -6)).box((-5, 0, -1), (10, 2, 1.5), MS[2], faces=faces(lip_face))
    # couraça de escudos nas costas
    body.child('back_armor', pivot=(0, -15, 1)).box((-6.5, -1.5, -4), (13, 3, 9), AR[2], faces=faces(armor))
    body.child('back_armor2', pivot=(0, -11, 6)).box((-6, 0, -0.5), (12, 8, 1.5), AR[2], faces=faces(armor))
    # pelo pendente (franja da barriga e dos lados)
    body.child('fringe', pivot=(0, 1, 0)).box((-8.5, 0, -6.5), (17, 3, 13), MF[1], faces=faces(mfur(2), top=solid(MF[1], False)))
    for i, (x, z) in enumerate(((-4.5, -2), (0, -3), (4.5, -2))):
        body.child('shag_%d' % i, pivot=(x, -16, z), rot=(-0.5, 0, (i - 1) * 0.3)).box((-1.5, -3.5, -1), (3, 4, 2), MF[1], faces=faces(mfur(20 + i)))
    head = M.part('head', pivot=(0, 1, -7)).box((-5.5, -9, -5), (11, 9, 9), MF[2], faces=faces(mfur(3), front=cyclops_face))
    head.child('brow', pivot=(0, -7.5, -5)).box((-4, -1, -1.2), (8, 1.5, 1.2), MF[0], faces=faces(mfur(9)))
    head.child('muzzle', pivot=(0, -2.5, -5)).box((-2, -1, -1.5), (4, 3, 1.5), MS[2],
                                                 faces=faces(soft(MS), front=art(["....", ".nn.", "...."], {'n': MS[0]})))
    head.child('mane', pivot=(0, -9, 3)).box((-6, -1, -2), (12, 10, 3), MF[1], faces=faces(mfur(4)))
    for side, x, rz in (('right_ear', -5.5, -0.7), ('left_ear', 5.5, 0.7)):
        head.child(side, pivot=(x, -8, -1), rot=(0, 0, rz)).box((-1, -2.5, -0.5), (2, 2.5, 1), MF[0])
    for side, x, sgn in (('right_arm', -9, -1), ('left_arm', 9, 1)):
        ox = -3 if sgn < 0 else -1
        arm = M.part(side, pivot=(x, 0, -2)).box((ox, 0, -2), (4, 13, 4), MF[2], faces=faces(mfur(10 + sgn)))
        arm.child(side + '_pauldron', pivot=(ox + 2, -0.5, 0)).box((-3, -1.5, -3), (6, 4, 6), AR[2], faces=faces(armor))
        fore = arm.child(side + '_fore', pivot=(ox + 2, 12, 0)).box((-3, 0, -3), (6, 8, 6), MF[2], faces=faces(mfur(12 + sgn)))
        fore.box((-2.5, 7.5, -2.5), (5, 2, 5), MS[2], faces=faces(soft(MS)))
        for i in range(3):
            c = fore.child('%s_claw_%d' % (side, i), pivot=((i - 1) * 1.6, 9.5, -1.5), rot=(-0.35, 0, 0)).box(
                (-0.5, 0, -0.5), (1, 4, 1), MS[0], faces=faces(claw_box))
            c.child('%s_claw_tip_%d' % (side, i), pivot=(0, 4, 0), rot=(-0.7, 0, 0)).box((-0.5, 0, -0.5), (1, 3, 1), BONE2[2],
                                                                                         faces=faces(soft(BONE2)))
    for side, x in (('right_leg', -4.5), ('left_leg', 4.5)):
        leg = M.part(side, pivot=(x, 15, 1)).box((-3, 0, -3), (6, 8, 6), MF[2], faces=faces(mfur(30 + int(x))))
        # pé virado para trás: o calcanhar vai na frente, os dedos apontam para trás
        foot = leg.child(side + '_foot', pivot=(0, 8, 0)).box((-3, 0, -2.5), (6, 1, 7), MS[2], faces=faces(soft(MS)))
        for i in range(3):
            foot.child('%s_toe_%d' % (side, i), pivot=((i - 1) * 2, 0, 4.5)).box((-0.5, 0, 0), (1, 1, 2), BONE2[1])
    return M


# ------------------------------------------------------------------- saída
def emit(M, name, yaws, scale, pitch=-15):
    M.pack()
    M.paint(T + name + '.png', T + name + '_glow.png')
    open(OUT + name + '_layer.txt', 'w').write(M.java())
    preview.draw = zview.zdraw
    preview.render(M, T + name + '.png', OUT + name + '_view.png', scale=scale, yaws=yaws, pitch=pitch)
    print(name, M.tex_w, M.tex_h)


JOBS = {
    'matinta_bird': lambda: emit(bird(), 'matinta_bird', (-35, 25, 160), 14),
    'matinta_witch': lambda: emit(witch(), 'matinta_witch', (-30, 20, 160), 9),
    'iara': lambda: emit(iara(), 'iara', (-30, 30, 160), 9),
    'cuca': lambda: emit(cuca(), 'cuca', (-35, 35, 150), 6),
    'mapinguari': lambda: emit(mapinguari(), 'mapinguari', (-35, 35, 150), 5),
    'caipora': lambda: emit(caipora(), 'caipora', (-40, 30, 140), 7),
}

if __name__ == '__main__':
    names = sys.argv[1:] or list(JOBS)
    for n in names:
        JOBS[n]()
