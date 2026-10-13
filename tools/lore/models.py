"""Modelos do folclore novo (leva 1): Matinta Pereira (pássaro e bruxa) e Caipora montada no queixada.
Uso (raiz do projeto): python tools/lore/models.py
Saída: textures/entity/<nome>.png (+ _glow), build/lore/<nome>_layer.txt e build/lore/<nome>_view.png."""
import os, sys
sys.path.insert(0, 'tools/modelgen')
sys.path.insert(0, 'tools/boitata')
from modelgen import Model
import preview
import zview

T = 'src/main/resources/assets/mistech/textures/entity/'
OUT = 'build/lore/'
os.makedirs(OUT, exist_ok=True)

ALL = ('front', 'back', 'left', 'right', 'top', 'bottom')
SIDES = ('front', 'back', 'left', 'right')


def every(fn, faces=ALL):
    return {k: fn for k in faces}


# =============================================================== Matinta Pereira — pássaro
FEATHER, FEATHER_D, FEATHER_H = 0x2c2420, 0x1a1412, 0x4a3a30
DISC = 0x8a7a68


def feathers(p):
    # liso, só umas penas em "V" mais claras de vez em quando
    for (x, y) in ((1, 2), (p.w - 2, 4), (p.w // 2, p.h - 2)):
        if 0 < x < p.w - 1 and 0 < y < p.h:
            p.px(x, y, FEATHER_H)
            p.px(x - 1, y - 1, FEATHER_H)
            p.px(x + 1, y - 1, FEATHER_H)


def owl_chest(p):
    p.fill(0x5a4a3c, 0.03)
    for y in range(1, p.h, 2):
        for x in range(1 + (y // 2) % 2, p.w - 1, 2):
            p.px(x, y, 0x3a2e26)


def owl_face(p):
    # disco facial em coração, olhos grandes amarelos com pupila e brilho
    p.fill(0xb8a68a, 0.02)
    for x in range(p.w):
        p.px(x, 0, FEATHER)
    p.px(0, 1, FEATHER)
    p.px(p.w - 1, 1, FEATHER)
    p.px(p.w // 2, 1, 0x8a7a64)
    for x0 in (0, p.w - 2):
        p.rect(x0, 2, 2, 2, 0xffd040, True)
        p.px(x0 + (1 if x0 == 0 else 0), 3, 0x140c08, True)
        p.px(x0 + (0 if x0 == 0 else 1), 2, 0xfff8d0, True)


def wing(p):
    # penas de voo mais escuras na ponta, com uma faixa clara
    for x in range(p.w):
        p.px(x, p.h - 1, FEATHER_D)
        if p.h > 3:
            p.px(x, p.h - 3, FEATHER_H)


def bird():
    M = Model('matinta_bird', 64, seed=51)
    body = M.part('body', pivot=(0, 19, 0), rot=(0.35, 0, 0)).box((-2.5, -3, -3), (5, 6, 6), FEATHER, faces={**every(feathers), 'front': owl_chest})
    head = M.part('head', pivot=(0, 16, -1)).box((-2.5, -4.5, -2.5), (5, 4.5, 5), FEATHER, faces={**every(feathers), 'front': owl_face})
    head.child('beak', pivot=(0, -2, -2.5)).box((-0.5, 0, -1), (1, 1.5, 1), 0x3a3028)
    head.child('right_tuft', pivot=(-2, -4.5, -1), rot=(0, 0, -0.4)).box((-0.5, -1.5, -0.5), (1, 1.5, 1), FEATHER_D)
    head.child('left_tuft', pivot=(2, -4.5, -1), rot=(0, 0, 0.4)).box((-0.5, -1.5, -0.5), (1, 1.5, 1), FEATHER_D)
    M.part('right_wing', pivot=(-2.5, 17, -1.5), rot=(0.35, 0, 0)).box((-1, 0, 0), (1, 5, 6), FEATHER, faces=every(wing))
    M.part('left_wing', pivot=(2.5, 17, -1.5), rot=(0.35, 0, 0)).box((0, 0, 0), (1, 5, 6), FEATHER, faces=every(wing))
    M.part('tail', pivot=(0, 21, 2.5), rot=(0.6, 0, 0)).box((-1.5, 0, 0), (3, 1, 4), FEATHER_D, faces=every(wing))
    for side, x in (('right_leg', -1.2), ('left_leg', 1.2)):
        M.part(side, pivot=(x, 22, 0)).box((-0.5, 0, -0.5), (1, 2, 1), 0x6a5a40).box((-1, 1.5, -1.5), (2, 0.5, 2), 0x6a5a40)
    return M


# =============================================================== Matinta Pereira — bruxa velha
SKIN, SKIN_D, SKIN_H = 0x8a8a6a, 0x6a6a52, 0xa0a07e
SHAWL, SHAWL_D, SHAWL_H = 0x1e1a22, 0x121014, 0x3a3240
SKIRT, SKIRT_D = 0x3a2a24, 0x2a1c18
HAIR = 0xd8d4c8


def hag_face(p):
    # rosto de desenho: sobrancelhas grossas, olhos amarelos grandes, sorriso torto com um dente
    p.fill(SKIN, 0.02)
    for x0 in (1, p.w - 3):
        p.rect(x0, 2, 2, 1, 0xd8d4c8)                      # sobrancelha branca
        p.rect(x0, 3, 2, 2, 0xfff0a0, True)                 # olho
        p.px(x0 + (1 if x0 == 1 else 0), 4, 0x1a1008, True)  # pupila
    p.rect(2, p.h - 2, p.w - 4, 1, 0x3a1a14)               # boca
    p.px(2, p.h - 3, 0x3a1a14)
    p.px(p.w - 3, p.h - 1, 0xf0e8d0)                        # dente
    p.px(1, p.h - 3, SKIN_D)
    p.px(p.w - 2, p.h - 3, SKIN_D)


def shawl(p):
    # xale liso com dobras e franja
    for y in range(p.h):
        for x in range(p.w):
            if x % 4 == 1 and y > 1:
                p.px(x, y, SHAWL_D)
    for x in range(0, p.w, 2):
        p.px(x, p.h - 1, SHAWL_H)


def skirt(p):
    for y in range(p.h):
        for x in range(p.w):
            if x % 4 == 2 and y > 2:
                p.px(x, y, SKIRT_D)
    p.row(p.h - 1, 0x1a1210)
    p.row(p.h - 3, 0x8a3a24)  # barra vermelha
    p.row(p.h - 4, 0x8a3a24)


def hair(p):
    for y in range(p.h):
        for x in range(p.w):
            p.px(x, y, 0xb8b4a8 if x % 3 == 1 else HAIR)


def witch():
    M = Model('matinta_witch', 64, seed=52)
    M.part('skirt', pivot=(0, 12, 0)).box((-4.5, 0, -3), (9, 12, 6), SKIRT, faces=every(skirt, SIDES))
    body = M.part('body', pivot=(0, 12, 0.5), rot=(0.35, 0, 0)).box((-4, -9, -2.5), (8, 9, 5), SHAWL, faces=every(shawl))
    body.child('hump', pivot=(0, -7, 2.5)).box((-3.5, -2, -1), (7, 4, 2), SHAWL, faces=every(shawl))
    head = M.part('head', pivot=(0, 4.5, -2)).box((-4, -8, -4), (8, 8, 8), SKIN, faces={'front': hag_face})
    head.child('nose', pivot=(0, -4, -4), rot=(0.3, 0, 0)).box((-0.5, -0.5, -2.5), (1, 2.5, 2.5), SKIN_H).box((-0.5, 1.5, -2.5), (1, 0.5, 0.5), 0x5a4a3a)
    hood = head.child('hood', pivot=(0, 0, 0)).box((-4.5, -8.6, -4.5), (9, 3, 9), SHAWL, faces=every(shawl))
    hood.child('drape', pivot=(0, -6, 4)).box((-4.5, 0, 0), (9, 8, 1.5), SHAWL, faces=every(shawl))
    head.child('hair_r', pivot=(-4.1, -5, -1)).box((-0.6, 0, -2), (1, 8, 4), HAIR, faces=every(hair))
    head.child('hair_l', pivot=(4.1, -5, -1)).box((-0.4, 0, -2), (1, 8, 4), HAIR, faces=every(hair))
    ra = M.part('right_arm', pivot=(-4.5, 5, -1)).box((-1.5, 0, -1), (2, 9, 2), SHAWL, faces=every(shawl))
    ra.child('right_hand', pivot=(-0.5, 9, 0)).box((-1, 0, -1), (2, 2, 2), SKIN)
    la = M.part('left_arm', pivot=(4.5, 5, -1)).box((-0.5, 0, -1), (2, 9, 2), SHAWL, faces=every(shawl))
    la.child('left_hand', pivot=(0.5, 9, 0)).box((-1, 0, -1), (2, 2, 2), SKIN)
    la.child('cane', pivot=(0.5, 10, -0.5)).box((-0.5, -3, -0.5), (1, 15, 1), 0x5a3a20).box((-1, -4.5, -1), (2, 1.5, 2), 0x3a2410)
    return M


# =============================================================== Caipora no queixada
FUR, FUR_D, FUR_H = 0x3a3430, 0x24201e, 0x56504a
WHITE = 0xe8e2d4
CSKIN, CSKIN_D, CSKIN_H = 0x9a5a34, 0x7a4426, 0xb06e42
RED_HAIR, RED_HAIR_D, RED_HAIR_Y = 0xc8341a, 0x8a2010, 0xff7a2a
PAINT = 0xd8261a


def fur(p):
    # pelo curto: só alguns fios mais escuros
    for y in range(p.h):
        for x in range(p.w):
            if (x * 5 + y * 3) % 11 == 0:
                p.px(x, y, FUR_D)


def peccary_face(p):
    fur(p)
    p.px(1, 1, 0x101010)
    p.px(p.w - 2, 1, 0x101010)
    for x in range(p.w):  # queixo branco (queixada)
        p.px(x, p.h - 1, WHITE)
        if 0 < x < p.w - 1:
            p.px(x, p.h - 2, WHITE)


def snout(p):
    p.fill(0x4a3a36, 0.05)


def snout_front(p):
    p.fill(0x6a4a48, 0.05)
    p.px(1, 1, 0x201010)
    p.px(p.w - 2, 1, 0x201010)


def bristles(p):
    p.fill(0x1a1614, 0.03)


def cskin(p):
    p.fill(CSKIN, 0.05)


def chest(p):
    p.fill(CSKIN, 0.04)
    for x in range(p.w):  # pintura de urucum
        if x % 2 == 0:
            p.px(x, 1, PAINT)
    p.px(p.w // 2, 3, PAINT)
    p.px(p.w // 2 - 1, 4, PAINT)
    p.px(p.w // 2 + 1, 4, PAINT)


def caipora_face(p):
    # olhos grandes com brilho (como o Saci/Curupira), faixa de urucum e sorriso maroto
    p.fill(CSKIN, 0.02)
    for x in range(p.w):
        p.px(x, 2, PAINT)
    for x0 in (1, p.w - 3):
        p.rect(x0, 3, 2, 2, 0xf6f2e6)
        p.px(x0 + (1 if x0 == 1 else 0), 4, 0x2a1408)
        p.px(x0 + (1 if x0 == 1 else 0), 3, 0x4a2a14)
    p.px(p.w // 2, 5, CSKIN_D)
    p.rect(2, p.h - 2, p.w - 4, 1, 0x5a2a18)
    p.px(p.w - 3, p.h - 3, 0x5a2a18)
    p.px(1, p.h - 3, 0xc06a4a)
    p.px(p.w - 2, p.h - 3, 0xc06a4a)


def red_hair(p):
    # mechas: linhas verticais mais escuras e um reflexo claro em cima
    for y in range(p.h):
        for x in range(p.w):
            if x % 3 == 0:
                p.px(x, y, RED_HAIR_D)
    p.row(0, RED_HAIR_Y)


def loincloth(p):
    p.fill(0x7a5424, 0.02)
    p.row(0, 0xd8a640)
    for x in range(1, p.w, 3):
        p.px(x, 1, 0xd8261a)


def feather(p):
    for y in range(p.h):
        for x in range(p.w):
            p.px(x, y, 0xffd040 if y < p.h // 2 else 0x2a8a3a)


BONE, BONE_D = 0xe8dcc0, 0xb8a888
BARK, BARK_D, BARK_H = 0x4a3428, 0x2e2018, 0x6a4a34
EMBER = 0xff5a1a


def bark(p):
    # pele de casca de árvore: veios verticais escuros
    for y in range(p.h):
        for x in range(p.w):
            if x % 3 == 0 or (x * 7 + y * 3) % 13 == 0:
                p.px(x, y, BARK_D)
    if p.h > 2:
        p.px(p.w // 2, p.h // 2, BARK_H)


def skull_mask(p):
    # máscara de caveira de queixada: órbitas fundas com brasas, focinho rachado, dentes
    p.fill(BONE, 0.02)
    for x0 in (1, p.w - 3):
        p.rect(x0, 2, 2, 2, 0x140806)
        p.px(x0 + (1 if x0 == 1 else 0), 3, EMBER, True)
        p.px(x0 + (0 if x0 == 1 else 1), 2, 0xffb040, True)
    p.px(p.w // 2, 2, BONE_D)
    p.px(p.w // 2, 3, BONE_D)
    p.rect(p.w // 2 - 1, 4, 2, 1, 0x2a1a10)
    for x in range(1, p.w - 1):
        p.px(x, p.h - 2, 0x2a1a10 if x % 2 else BONE)
    p.row(p.h - 1, BONE_D)


def bone(p):
    p.fill(BONE, 0.03)
    p.row(p.h - 1, BONE_D)


def claws(p):
    p.fill(0x1a1410, 0.02)


def demon_pig_face(p):
    fur(p)
    for x0 in (0, p.w - 2):
        p.rect(x0, 1, 2, 1, EMBER, True)
        p.px(x0 + (1 if x0 == 0 else 0), 1, 0xffd040, True)
    for x in range(p.w):
        p.px(x, p.h - 1, WHITE)


def scar_fur(p):
    fur(p)
    for i in range(min(p.w, p.h)):  # cicatriz
        if (i * 2) < p.w and i < p.h and i % 2 == 0:
            p.px(i * 2 % p.w, i, 0x6a3a30)


def caipora():
    M = Model('caipora', 128, seed=53)
    # --- queixada demoníaco: maior, crista de cerdas, presas curvas, olhos em brasa ---
    body = M.part('pig', pivot=(0, 13, 1)).box((-5, -5, -8), (10, 9, 16), FUR, faces={'top': fur, 'left': scar_fur, 'right': fur, 'back': fur})
    for i, z in enumerate((-6, -3, 0, 3, 6)):
        h = (3, 4, 4, 3, 2)[i]
        body.child('spike_%d' % i, pivot=(0, -5, z), rot=(-0.35, 0, 0)).box((-0.5, -h, -1), (1, h, 2), 0x141010, faces=every(bristles))
    body.child('pig_tail', pivot=(0, -3, 8), rot=(0.5, 0, 0)).box((-0.5, 0, 0), (1, 3, 1), FUR_D)
    head = M.part('pig_head', pivot=(0, 12, -7)).box((-3.5, -3.5, -6), (7, 7, 6), FUR, faces={**every(fur), 'front': demon_pig_face})
    sn = head.child('snout', pivot=(0, 1.5, -6)).box((-2.5, -1.5, -3), (5, 3.5, 3), 0x4a3a36, faces={**every(snout), 'front': snout_front})
    for side, x, rz in (('right_tusk', -3, -0.35), ('left_tusk', 3, 0.35)):
        t = sn.child(side, pivot=(x, 1.5, -1.5), rot=(-0.3, 0, rz)).box((-0.5, -4, -0.5), (1, 4, 1), WHITE, faces=every(bone))
        t.child(side + '_tip', pivot=(0, -4, 0), rot=(-0.8, 0, 0)).box((-0.5, -2.5, -0.5), (1, 2.5, 1), WHITE, faces=every(bone))
    head.child('right_ear', pivot=(-3, -3.5, -1), rot=(0.3, 0, -0.6)).box((-1, -3, 0), (2, 3, 1), FUR_D)
    head.child('left_ear', pivot=(3, -3.5, -1), rot=(0.3, 0, 0.6)).box((-1, -3, 0), (2, 3, 1), FUR_D)
    for name, x, z in (('leg_fr', -3, -5.5), ('leg_fl', 3, -5.5), ('leg_br', -3, 6.5), ('leg_bl', 3, 6.5)):
        M.part(name, pivot=(x, 17, z)).box((-1.5, 0, -1.5), (3, 7, 3), FUR_D, faces={**every(fur), 'bottom': claws})
    # --- Caipora: espírito da mata, magro e alto, pele de casca, juba de fogo, máscara de caveira com galhos ---
    rider = M.part('rider', pivot=(0, 8, 2))
    rider.box((-3, -9, -1.5), (6, 9, 3), BARK, faces=every(bark))
    rider.child('ribs', pivot=(0, -6, -1.5)).box((-2.5, 0, -0.5), (5, 4, 0.5), BONE, faces=every(bone))
    rh = rider.child('rider_head', pivot=(0, -9, 0)).box((-4, -8, -4), (8, 8, 8), BARK, faces={**every(bark), 'front': skull_mask})
    jaw = rh.child('rider_jaw', pivot=(0, 0, -2), rot=(0.25, 0, 0)).box((-3, 0, -2.2), (6, 2, 4), BONE, faces={**every(bone), 'front': lambda p: [p.px(x, 0, 0xf8f0e0) for x in range(0, p.w, 2)]})
    mane = rh.child('hair', pivot=(0, -8, 0)).box((-4.5, -2, -4.5), (9, 3, 9), RED_HAIR, faces=every(red_hair), glow=True)
    mane.child('hair_back', pivot=(0, 1, 4)).box((-5, 0, 0), (10, 13, 2), RED_HAIR, faces=every(red_hair), glow=True)
    mane.child('hair_r', pivot=(-4.5, 1, 1), rot=(0, 0, 0.15)).box((-1.5, 0, 0), (2, 9, 4), RED_HAIR, faces=every(red_hair), glow=True)
    mane.child('hair_l', pivot=(4.5, 1, 1), rot=(0, 0, -0.15)).box((-0.5, 0, 0), (2, 9, 4), RED_HAIR, faces=every(red_hair), glow=True)
    # chifres de galho
    for side, x, rz in (('right_antler', -3, -0.5), ('left_antler', 3, 0.5)):
        an = rh.child(side, pivot=(x, -8, -1), rot=(-0.2, 0, rz)).box((-0.5, -6, -0.5), (1, 6, 1), BARK_D)
        an.child(side + '_fork', pivot=(0, -4, 0), rot=(0, 0, -rz * 1.6)).box((-0.5, -3, -0.5), (1, 3, 1), BARK_D)
        an.child(side + '_tip', pivot=(0, -6, 0), rot=(0.4, 0, rz * 0.6)).box((-0.5, -2, -0.5), (1, 2, 1), BARK_D)
    rider.child('rider_right_leg', pivot=(-2.5, 0, 0), rot=(-1.3, 0, 0.6)).box((-1, 0, -1), (2, 8, 2), BARK, faces=every(bark))
    rider.child('rider_left_leg', pivot=(2.5, 0, 0), rot=(-1.3, 0, -0.6)).box((-1, 0, -1), (2, 8, 2), BARK, faces=every(bark))
    ra = rider.child('rider_right_arm', pivot=(-3.5, -8.5, 0), rot=(-0.9, 0, 0.1)).box((-1.5, 0, -1), (2, 10, 2), BARK, faces=every(bark))
    ra.box((-1.8, 9.5, -1.3), (2.6, 2, 2.6), 0x1a1410)
    ra.child('spear', pivot=(-0.5, 10, 0), rot=(0.9, 0, 0)).box((-0.5, -18, -0.5), (1, 22, 1), BONE_D, faces=every(bone)).box((-1, -22, -1), (2, 4, 2), BONE, faces=every(bone))
    la = rider.child('rider_left_arm', pivot=(3.5, -8.5, 0), rot=(-0.4, 0, -0.2)).box((-0.5, 0, -1), (2, 10, 2), BARK, faces=every(bark))
    for i in range(3):  # garras
        la.child('claw_%d' % i, pivot=(0.5 + (i - 1) * 0.7, 10, -0.5), rot=(-0.5, 0, 0)).box((-0.25, 0, -0.25), (0.5, 2.5, 0.5), 0x1a1410)
    return M


def emit(M, name, yaws, scale):
    M.pack()
    glow = T + name + '_glow.png'
    M.paint(T + name + '.png', glow)
    open(OUT + name + '_layer.txt', 'w').write(M.java())
    preview.draw = zview.zdraw
    preview.render(M, T + name + '.png', OUT + name + '_view.png', scale=scale, yaws=yaws, pitch=-15)


# =============================================================== Iara
I_SKIN, I_SKIN_D = 0xb0784a, 0x8a5a36
SCALE_G, SCALE_D, SCALE_H = 0x2a8a7a, 0x1a5a52, 0x5ad0b0
I_HAIR, I_HAIR_H = 0x14100e, 0x3a3040


def iara_face(p):
    p.fill(I_SKIN, 0.02)
    for x0 in (1, p.w - 3):
        p.rect(x0, 3, 2, 2, 0xf6f2e6)
        p.px(x0 + (1 if x0 == 1 else 0), 4, 0x1a7a5a, True)
        p.px(x0 + (1 if x0 == 1 else 0), 3, 0x2ab08a, True)
        p.px(x0, 2, I_HAIR)
        p.px(x0 + 1, 2, I_HAIR)
    p.px(p.w // 2, 5, I_SKIN_D)
    p.rect(3, p.h - 2, p.w - 6, 1, 0xb0303a)
    p.px(1, p.h - 3, 0xc88a6a)
    p.px(p.w - 2, p.h - 3, 0xc88a6a)


def iara_hair(p):
    for y in range(p.h):
        for x in range(p.w):
            if x % 3 == 1:
                p.px(x, y, I_HAIR_H)


def tail_scales(p):
    # escamas grandes e suaves: arco claro a cada 3 px, desencontrado por fileira
    for y in range(p.h):
        for x in range(p.w):
            row = y // 3
            lx = (x + (row % 2) * 2) % 4
            if y % 3 == 0 and lx in (1, 2):
                p.px(x, y, SCALE_H)
            elif y % 3 == 2 and lx in (0, 3):
                p.px(x, y, SCALE_D)


def fin(p):
    for y in range(p.h):
        for x in range(p.w):
            p.px(x, y, 0x7ae0c8 if (x + y) % 3 else SCALE_H, True)


def shell(p):
    p.fill(0xf0c8b0, 0.02)
    p.row(0, 0xd89878)


def iara():
    M = Model('iara', 64, seed=61)
    body = M.part('body', pivot=(0, 8, 0)).box((-3.5, -7, -2), (7, 7, 4), I_SKIN)
    body.child('shells', pivot=(0, -5.5, -2)).box((-3, 0, -0.6), (6, 2, 0.6), 0xf0c8b0, faces=every(shell))
    head = M.part('head', pivot=(0, 1, 0)).box((-4, -8, -4), (8, 8, 8), I_SKIN, faces={'front': iara_face})
    hair = head.child('hair', pivot=(0, -8, 0)).box((-4.5, -1, -4.5), (9, 2.5, 9), I_HAIR, faces=every(iara_hair))
    hair.child('hair_back', pivot=(0, 1, 4)).box((-4.5, 0, 0), (9, 16, 1.5), I_HAIR, faces=every(iara_hair))
    hair.child('hair_r', pivot=(-4.5, 1, 1)).box((-0.6, 0, -2), (1, 10, 5), I_HAIR, faces=every(iara_hair))
    hair.child('hair_l', pivot=(4.5, 1, 1)).box((-0.4, 0, -2), (1, 10, 5), I_HAIR, faces=every(iara_hair))
    hair.child('flower', pivot=(3.5, 0, -2.5)).box((-1.5, -1.5, -1.5), (3, 2, 3), 0xf8e0f0).box((-0.5, -2, -0.5), (1, 1, 1), 0xffd040, glow=True)
    M.part('right_arm', pivot=(-3.5, 1.5, 0)).box((-2, 0, -1), (2, 7, 2), I_SKIN).box((-2.2, 5, -1.2), (2.4, 0.6, 2.4), 0x2ab08a)
    M.part('left_arm', pivot=(3.5, 1.5, 0)).box((0, 0, -1), (2, 7, 2), I_SKIN).box((-0.2, 5, -1.2), (2.4, 0.6, 2.4), 0x2ab08a)
    t0 = M.part('tail0', pivot=(0, 8, 0)).box((-3.5, 0, -2), (7, 6, 4), SCALE_G, faces=every(tail_scales))
    t1 = t0.child('tail1', pivot=(0, 6, 0)).box((-3, 0, -1.75), (6, 5, 3.5), SCALE_G, faces=every(tail_scales))
    t2 = t1.child('tail2', pivot=(0, 5, 0)).box((-2.5, 0, -1.5), (5, 4, 3), SCALE_G, faces=every(tail_scales))
    t2.child('tail_fin', pivot=(0, 4, 0)).box((-5, 0, -0.5), (10, 3, 1), 0x5ad0b0, faces=every(fin))
    return M


# =============================================================== Cuca
GATOR, GATOR_D, GATOR_H = 0x4a6a2a, 0x2e4618, 0x6a8a3a
ROBE, ROBE_D, ROBE_H = 0x3a1e4a, 0x24122e, 0x5a2e6a
C_HAIR = 0x8a8a7a


def gator(p):
    # couro de jacaré: placas em fileiras
    for y in range(p.h):
        for x in range(p.w):
            if y % 3 == 2 or (x + (y // 3) * 2) % 4 == 0:
                p.px(x, y, GATOR_D)
    for x in range(1, p.w, 4):
        p.px(x, 0, GATOR_H)


def gator_belly(p):
    p.fill(0xb8b07a, 0.02)
    for y in range(1, p.h, 2):
        p.row(y, 0x9a9262)


def teeth(p):
    p.fill(GATOR, 0.02)
    for x in range(0, p.w, 2):
        p.px(x, p.h - 1, 0xf4ecd8)


def teeth_up(p):
    p.fill(GATOR, 0.02)
    for x in range(1, p.w, 2):
        p.px(x, 0, 0xf4ecd8)


def mouth_in(p):
    p.fill(0x6a1a1a, 0.04)


def gator_eye(p):
    p.fill(0xffd030, 0.02)
    for y in range(p.h):
        p.px(p.w // 2, y, 0x140a04, True)
    for y in range(p.h):
        for x in range(p.w):
            if x != p.w // 2:
                p.px(x, y, 0xffd030, True)


def robe(p):
    for y in range(p.h):
        for x in range(p.w):
            if x % 4 == 2 and y > 1:
                p.px(x, y, ROBE_D)
    p.row(p.h - 1, ROBE_D)


def robe_trim(p):
    robe(p)
    p.row(p.h - 2, 0xb08a2a)
    for x in range(0, p.w, 3):
        p.px(x, p.h - 3, 0x5aff8a, True)  # runas verdes na barra


def stringy(p):
    for y in range(p.h):
        for x in range(p.w):
            if x % 2 == 0:
                p.px(x, y, 0x6a6a5a)


def cuca():
    M = Model('cuca', 128, seed=62)
    M.part('skirt', pivot=(0, 13, 0)).box((-6, 0, -4.5), (12, 11, 9), ROBE, faces=every(robe_trim, SIDES))
    body = M.part('body', pivot=(0, 13, 0.5), rot=(0.2, 0, 0)).box((-5, -12, -3), (10, 12, 6), ROBE, faces=every(robe))
    body.child('necklace', pivot=(0, -11, -3)).box((-3, 0, -0.5), (6, 1, 0.5), 0xd8d0b8).box((-1, 1, -0.6), (2, 2, 0.6), 0xd8d0b8)
    head = M.part('head', pivot=(0, 1.5, -1.5)).box((-4, -6, -5), (8, 6, 8), GATOR, faces={**every(gator), 'bottom': gator_belly})
    up = head.child('snout', pivot=(0, -2, -5)).box((-3, -2.5, -10), (6, 3, 10), GATOR, faces={**every(gator), 'bottom': teeth_up, 'right': teeth, 'left': teeth})
    up.child('nostrils', pivot=(0, -2.5, -9)).box((-1.5, -0.8, -0.5), (3, 0.8, 1.5), GATOR_D)
    head.child('jaw', pivot=(0, 0, -4), rot=(0.25, 0, 0)).box((-3, 0, -11), (6, 2, 11), GATOR, faces={'top': mouth_in, 'bottom': gator_belly, 'right': teeth_up,
                                                                                              'left': teeth_up, 'front': gator, 'back': gator})
    for side, x in (('right_eye', -3.2), ('left_eye', 3.2)):
        head.child(side, pivot=(x, -6, -3)).box((-1, -2, -1), (2, 2, 2), 0xffd030, faces={'front': gator_eye, 'top': gator, 'back': gator,
                                                                                         'left': gator_eye, 'right': gator_eye, 'bottom': gator})
    hair = head.child('hair', pivot=(0, -6, 1)).box((-4.5, -1, -2), (9, 2, 6), C_HAIR, faces=every(stringy))
    hair.child('hair_back', pivot=(0, 0, 4)).box((-4.5, 0, -0.5), (9, 14, 1.5), C_HAIR, faces=every(stringy))
    hair.child('bone_pin', pivot=(2.5, -1, 1), rot=(0, 0, 0.6)).box((-0.5, -4, -0.5), (1, 5, 1), 0xe8dcc0)
    ra = M.part('right_arm', pivot=(-5.5, 3, -0.5)).box((-2.5, 0, -1.5), (3, 11, 3), ROBE, faces=every(robe))
    ra.child('right_hand', pivot=(-1, 11, 0)).box((-1.5, 0, -1.5), (3, 2, 3), GATOR, faces=every(gator))
    ra.child('ladle', pivot=(-1, 12, -0.5), rot=(-0.4, 0, 0)).box((-0.5, -2, -0.5), (1, 10, 1), 0x5a3a20).box((-1.5, 7.5, -1.5), (3, 2, 3), 0x3a3a3a)
    la = M.part('left_arm', pivot=(5.5, 3, -0.5)).box((-0.5, 0, -1.5), (3, 11, 3), ROBE, faces=every(robe))
    la.child('left_hand', pivot=(1, 11, 0)).box((-1.5, 0, -1.5), (3, 2, 3), GATOR, faces=every(gator))
    for i in range(3):
        la.child('claw_%d' % i, pivot=(1 + (i - 1) * 0.9, 13, -1), rot=(-0.4, 0, 0)).box((-0.3, 0, -0.3), (0.6, 2, 0.6), 0xf4ecd8)
    t0 = M.part('tail0', pivot=(0, 21, 4), rot=(-0.25, 0, 0)).box((-2.5, -2, 0), (5, 4, 7), GATOR, faces=every(gator))
    t1 = t0.child('tail1', pivot=(0, 0, 7), rot=(0.15, 0, 0)).box((-2, -1.5, 0), (4, 3, 7), GATOR, faces=every(gator))
    t1.child('tail2', pivot=(0, 0, 7), rot=(0.1, 0, 0)).box((-1.5, -1, 0), (3, 2, 7), GATOR, faces=every(gator))
    return M


# =============================================================== Mapinguari
M_FUR, M_FUR_D, M_FUR_H = 0x6a3a22, 0x4a2414, 0x8a5030
M_SKIN = 0x3a2a24


def mfur(p):
    # pelo comprido: mechas verticais suaves e pontas mais escuras embaixo
    for y in range(p.h):
        for x in range(p.w):
            if x % 3 == 0 and (y + x) % 5 != 0:
                p.px(x, y, 0x5a301c)
    for x in range(p.w):
        if x % 2 == 0:
            p.px(x, p.h - 1, M_FUR_D)
    if p.w > 2:
        for x in range(1, p.w, 4):
            p.px(x, 0, M_FUR_H)


def belly_mouth(p):
    # a boca na barriga: lábios escuros, dentes em cima e embaixo, garganta em brasa
    mfur(p)
    w, h = p.w, p.h
    x0, x1, y0, y1 = 2, w - 3, h // 2 - 2, h // 2 + 3
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p.px(x, y, 0x1a0606)
    for y in range(y0 + 1, y1):
        for x in range(x0 + 2, x1 - 1):
            p.px(x, y, 0x8a1a10 if (x + y) % 2 else 0xb02a14, True)
    for x in range(x0, x1 + 1, 2):
        p.px(x, y0, 0xf0e6c8)
        p.px(x + 1 if x + 1 <= x1 else x, y1, 0xf0e6c8)
    for x in range(x0 - 1, x1 + 2):
        p.px(x, y0 - 1, M_SKIN)
        p.px(x, y1 + 1, M_SKIN)


def one_eye(p):
    mfur(p)
    cx = p.w // 2
    for y in range(1, 6):
        for x in range(cx - 2, cx + 2):
            p.px(x, y, 0xfff0b0 if (y in (1, 5) or x in (cx - 2, cx + 1)) else 0xffc030, True)
    p.px(cx - 1, 3, 0x200804, True)
    p.px(cx, 3, 0x200804, True)
    p.px(cx - 1, 2, 0x200804, True)
    for x in range(cx - 3, cx + 3):
        p.px(x, 0, M_FUR_D)


def claw_paint(p):
    p.fill(0x2a2420, 0.03)
    p.row(p.h - 1, 0xd8d0b8)


def mapinguari():
    M = Model('mapinguari', 128, seed=63)
    body = M.part('body', pivot=(0, 14, 0)).box((-8, -15, -6), (16, 16, 12), M_FUR, faces={**every(mfur), 'front': belly_mouth})
    body.child('belly_lip', pivot=(0, -6, -6)).box((-5, 0, -1), (10, 2, 1), M_SKIN)
    body.child('hump', pivot=(0, -15, 2)).box((-6, -3, -4), (12, 4, 8), M_FUR, faces=every(mfur))
    for i, (x, z) in enumerate(((-4, 3), (0, 4), (4, 3), (-2, -1), (2, -1))):
        body.child('shag_%d' % i, pivot=(x, -16, z), rot=(0.4, 0, (i - 2) * 0.15)).box((-1, -3, -1), (2, 4, 2), M_FUR_D, faces=every(mfur))
    head = M.part('head', pivot=(0, -1, -3)).box((-4.5, -8, -5), (9, 8, 9), M_FUR, faces={**every(mfur), 'front': one_eye})
    head.child('brow', pivot=(0, -6, -5)).box((-3.5, -1, -0.8), (7, 1.5, 1), M_FUR_D)
    for side, x, rz in (('right_ear', -4.5, -0.5), ('left_ear', 4.5, 0.5)):
        head.child(side, pivot=(x, -7, -1), rot=(0, 0, rz)).box((-1, -2, -0.5), (2, 2, 1), M_FUR_D)
    for side, x, sgn in (('right_arm', -9, -1), ('left_arm', 9, 1)):
        arm = M.part(side, pivot=(x, -0.5, -1)).box((-2.5 if sgn < 0 else -1.5, 0, -2.5), (4, 19, 5), M_FUR, faces=every(mfur))
        for i in range(3):
            arm.child('%s_claw_%d' % (side, i), pivot=((-0.5 if sgn < 0 else 0.5) + (i - 1) * 1.2, 19, -1.5), rot=(-0.5, 0, 0)).box(
                (-0.4, 0, -0.4), (0.8, 4, 0.8), 0x2a2420, faces=every(claw_paint))
    for side, x in (('right_leg', -4), ('left_leg', 4)):
        leg = M.part(side, pivot=(x, 15, 0)).box((-3, 0, -3), (6, 9, 6), M_FUR, faces=every(mfur))
        leg.child(side + '_foot', pivot=(0, 8, 0)).box((-3, 0, -1), (6, 1, 6), M_SKIN)  # pé virado para trás
    return M


if __name__ == '__main__':
    emit(bird(), 'matinta_bird', (-35, 35, 160), 14)
    emit(witch(), 'matinta_witch', (-30, 30, 160), 9)
    emit(caipora(), 'caipora', (-40, 40, 140), 7)
    emit(iara(), 'iara', (-30, 30, 160), 9)
    emit(cuca(), 'cuca', (-35, 35, 150), 6)
    emit(mapinguari(), 'mapinguari', (-35, 35, 150), 5)
    print('ok')
