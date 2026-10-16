"""Criaturas do Sonho (fase 2): Ovelha-de-Contar, Bicho-Papão e Sombra (modelgen) + texturas do Gnomo Mercador e do
Gnomo Corrompido (recolor do gnomo) + ovos.
Uso (raiz do projeto): python tools/dream/mobs.py  -> textures/entity/*.png, build/dream/<nome>_layer.txt e _view.png"""
import colorsys, os, random, sys
sys.path.insert(0, 'tools/modelgen')
from modelgen import Model
import preview
from PIL import Image

T = 'src/main/resources/assets/mistech/textures/'
OUT = 'build/dream/'
os.makedirs(OUT, exist_ok=True)
ALL = ('front', 'back', 'left', 'right', 'top', 'bottom')


def every(fn, faces=ALL):
    return {k: fn for k in faces}


def _shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in ((c >> 16) & 255, (c >> 8) & 255, c & 255))


def soft(base, amp=0.07, cell=3, seed=0, top=0.05, bottom=0.12):
    """Manchas grandes e suaves (value noise), topo um pouco mais claro e base mais escura — sem pontinhos soltos."""
    def paint(p):
        rnd = random.Random(seed * 7919 + p.x0 * 31 + p.y0)
        gw, gh = p.w // cell + 2, p.h // cell + 2
        grid = [[rnd.uniform(-1, 1) for _ in range(gw)] for _ in range(gh)]
        for y in range(p.h):
            for x in range(p.w):
                gx, gy = x / cell, y / cell
                ix, iy = int(gx), int(gy)
                fx, fy = gx - ix, gy - iy
                v = (grid[iy][ix] * (1 - fx) * (1 - fy) + grid[iy][ix + 1] * fx * (1 - fy) + grid[iy + 1][ix] * (1 - fx) * fy
                     + grid[iy + 1][ix + 1] * fx * fy)
                k = 1.0 + round(v * amp / 0.04) * 0.04
                if p.h > 2:
                    k *= 1.0 + top - (top + bottom) * y / (p.h - 1)
                p.px(x, y, _shade(base, k))
    return paint


# =============================================================== Ovelha-de-Contar (caricata: uma nuvem com pezinhos)
WOOL, WOOL_D = 0xf6f2ff, 0xd9d0f0
FACE = 0x8f84c0
SKIN = 0xe9c6dc


def cloud(seed):
    # lã de nuvem: manchas grandes e suaves, bem mais clara em cima e lilás embaixo
    return soft(WOOL, 0.06, 4, seed, 0.04, 0.16)


def sheep_face(p):
    p.fill(FACE, 0.02)
    # olhinhos pretos com brilho, bochechas rosadas e um "w" de boca
    p.px(1, 1, 0x1c1830)
    p.px(3, 1, 0x1c1830)
    p.px(0, 2, 0xe8a0c8)
    p.px(4, 2, 0xe8a0c8)
    p.px(2, 3, 0x5a4f88)


def sheep():
    M = Model('counting_sheep', 128, seed=71)
    # corpo tosquiado (só aparece sem lã)
    M.part('body', pivot=(0, 17, 0)).box((-4, -3, -5), (8, 6, 10), SKIN)
    # a nuvem: um bloco grande + "puffs" em volta (tudo filho de wool, some quando tosquia)
    wool = M.part('wool', pivot=(0, 15, 0))
    wool.box((-6.5, -6, -7.5), (13, 12, 15), WOOL, faces=every(cloud(1)), noise=0.0)
    # calombos irregulares em volta: a silhueta vira nuvem, não caixa
    PUFFS = [((-6, -9, -6), (6, 3, 6)), ((0, -10, -4), (6, 4, 7)), ((-4, -9, 2), (7, 3, 5)), ((-2, -11, -1), (4, 2, 4)),
             ((-8.5, -4, -5), (2, 6, 6)), ((-8, -3, 2), (2, 6, 5)), ((6.5, -5, -3), (2, 7, 7)), ((6, -2, 4), (2, 5, 4)),
             ((-4, -4, 7.5), (8, 7, 2)), ((-6, -6, -9), (5, 6, 2)), ((2, -5, -9), (5, 6, 2)), ((-5, 4, -6), (10, 2, 12))]
    for i, (o, sz) in enumerate(PUFFS):
        wool.box(o, sz, WOOL, faces=every(cloud(10 + i)), noise=0.0)
    wool.child('tail', pivot=(0, -2, 9.5)).box((-1.5, -1.5, 0), (3, 3, 2), WOOL, faces=every(cloud(8)), noise=0.0)
    # rostinho pequeno espiando da nuvem
    head = M.part('head', pivot=(0, 15, -9.5))
    head.box((-2.5, -2, -2), (5, 4, 2), FACE, faces={'front': sheep_face})
    head.box((-2, -3, -1.5), (4, 1, 2), WOOL, faces=every(cloud(9)), noise=0.0)
    head.child('right_ear', pivot=(-2.5, -1, -1), rot=(0, 0, 0.6)).box((-1.5, -0.5, -0.5), (1.5, 1, 1), FACE)
    head.child('left_ear', pivot=(2.5, -1, -1), rot=(0, 0, -0.6)).box((0, -0.5, -0.5), (1.5, 1, 1), FACE)
    # pezinhos curtos
    for name, x, z in (('leg_fr', -3, -4.5), ('leg_fl', 3, -4.5), ('leg_br', -3, 4.5), ('leg_bl', 3, 4.5)):
        M.part(name, pivot=(x, 21.5, z)).box((-1, 0, -1), (2, 2.5, 2), FACE)
    return M


# =============================================================== Bicho-Papão (fera corcunda com o cobertor da cama)
FUR, FUR_D, FUR_H = 0x2a1d24, 0x170f14, 0x3d2b33
EYE, EYE_D = 0xffd23a, 0xb07a10
QUILT, QUILT_D, QUILT_L = 0x3b3466, 0x241f42, 0x6a5aa0
TOOTH = 0xe8e0c0


def fur(seed):
    def paint(p):
        soft(FUR, 0.10, 3, seed, 0.06, 0.22)(p)
        # mechas: traços verticais compridos e contínuos, sem pontinhos
        for x in range(1, p.w, 3):
            y0 = (x * 5 + seed) % 4
            for y in range(y0, min(p.h, y0 + max(3, p.h // 2))):
                p.px(x, y, FUR_H if (x // 3) % 2 else FUR_D)
    return paint


def boogey_face(p):
    fur(11)(p)
    # olhos desiguais: um grande arregalado, outro pequeno e semicerrado
    p.rect(1, 1, 3, 2, EYE, True)
    p.px(2, 2, EYE_D, True)
    p.rect(p.w - 3, 2, 2, 1, EYE, True)
    # sorriso que vai de orelha a orelha (a parte de cima da boca; a mandíbula é outra peça)
    y = p.h - 2
    for x in range(0, p.w):
        p.px(x, y, 0x050305)
        p.px(x, y + 1, TOOTH if x % 2 == 0 else 0x050305, x % 2 == 0)
    p.px(0, y - 1, 0x050305)
    p.px(p.w - 1, y - 1, 0x050305)


def jaw_front(p):
    p.fill(FUR_D, 0.02)
    for x in range(0, p.w):
        p.px(x, 0, TOOTH if x % 2 == 1 else 0x050305, x % 2 == 1)


def quilt(p):
    # cobertor de retalhos: quadrados grandes alternados + costura, borda de baixo rasgada
    for y in range(p.h):
        for x in range(p.w):
            c = QUILT if ((x // 4) + (y // 4)) % 2 else QUILT_D
            if x % 4 == 0 or y % 4 == 0:
                c = QUILT_L if (x + y) % 2 else c
            p.px(x, y, _shade(c, 1.0 - 0.25 * y / max(1, p.h - 1)))
    for x in range(p.w):
        cut = (x * 7) % 5
        for y in range(p.h - cut // 2 - (1 if cut > 2 else 0), p.h):
            p.clear(x, y)


def claw(p):
    p.fill(0x120c10, 0.02)
    p.px(0, p.h - 1, TOOTH)
    if p.w > 1:
        p.px(p.w - 1, p.h - 1, TOOTH)


def bogeyman():
    M = Model('bogeyman', 128, seed=91)
    # pernas curtas e dobradas, pés largos
    for name, x in (('right_leg', -3), ('left_leg', 3)):
        M.part(name, pivot=(x, 11, 3)).box((-1.5, 0, -1.5), (3, 13, 3), FUR, faces=every(fur(3))).box((-2, 11, -4.5), (4, 2, 5), FUR_D,
                                                                                                    faces={'front': claw})
    # tronco inclinado para a frente, com corcunda
    torso = M.part('torso', pivot=(0, 12, 3), rot=(0.95, 0, 0))
    torso.box((-5, -17, -3), (10, 17, 6), FUR, faces=every(fur(4)))
    torso.box((-4, -17, 2), (8, 9, 3), FUR, faces=every(fur(5)))
    # o cobertor da cama jogado nas costas
    torso.child('quilt', pivot=(0, -17, 5), rot=(-0.12, 0, 0)).box((-6, 0, 0), (12, 16, 1), QUILT, faces=every(quilt), noise=0.0)
    # cabeça larga, chifre torto de um lado e quebrado do outro
    head = torso.child('head', pivot=(0, -17, -1), rot=(-0.95, 0, 0))
    head.box((-5, -7, -6), (10, 7, 8), FUR, faces={**every(fur(6)), 'front': boogey_face})
    head.box((-2, -9, -3), (4, 2, 4), FUR_H, faces=every(fur(7)))
    rh = head.child('right_horn', pivot=(-4, -6, -1), rot=(0, 0, -0.7))
    rh.box((-1, -4, -1), (2, 4, 2), FUR_D)
    rh.child('right_horn_tip', pivot=(0, -4, 0), rot=(0, 0, 0.9)).box((-0.5, -3, -0.5), (1, 3, 1), 0x0d080b)
    head.child('left_horn', pivot=(4, -6, -1), rot=(0, 0, 0.5)).box((-1, -2, -1), (2, 2, 2), FUR_D)
    head.child('jaw', pivot=(0, 0, 0), rot=(0.15, 0, 0)).box((-4.5, 0, -5.5), (9, 2, 7), FUR_D, faces={'front': jaw_front})
    # braços compridos que arrastam no chão, mãos com 3 garras
    for name, x in (('right_arm', -6), ('left_arm', 6)):
        arm = torso.child(name, pivot=(x, -15, 0), rot=(-0.95, 0, 0))
        arm.box((-1.5, -1, -1.5), (3, 22, 3), FUR, faces=every(fur(8)))
        hand = arm.child(name.replace('arm', 'hand'), pivot=(0, 21, 0))
        hand.box((-2, 0, -2), (4, 2, 4), FUR_D)
        for i, cx in enumerate((-1.5, 0, 1.5)):
            hand.child('%s_claw_%d' % (name, i), pivot=(cx, 2, -1.5), rot=(-0.35, 0, 0)).box((-0.5, 0, -0.5), (1, 4, 1), 0x120c10,
                                                                                              faces=every(claw))
    return M


# =============================================================== Sombra (alta, de capuz e máscara de porcelana, flutua)
SH, SH_D, SH_E = 0x1c2233, 0x0d1019, 0x2c3550
MASK, MASK_D = 0xe8e4dc, 0xa8a49c


def smoke(seed, ragged=False):
    def paint(p):
        soft(SH, 0.18, 4, seed, 0.2, 0.35)(p)
        if ragged and p.h > 3:
            # barra esfiapada: tiras de comprimentos diferentes
            for x in range(p.w):
                cut = (x * 3 + seed) % 4
                for y in range(p.h - cut, p.h):
                    p.clear(x, y)
    return paint


def hood_front(p):
    smoke(1)(p)
    # o buraco do capuz (a máscara fica dentro)
    p.rect(1, 2, p.w - 2, p.h - 2, 0x05060a)


def mask(p):
    p.fill(MASK, 0.02)
    # fendas verticais vazadas, sem boca; uma lágrima escura escorre de um olho e uma rachadura desce do outro lado
    for x in (1, p.w - 2):
        p.rect(x, 2, 1, 3, 0x05060a)
    p.px(1, 5, 0x3a3a40)
    p.px(1, 6, 0x5a5a60)
    p.px(p.w - 2, 0, MASK_D)
    p.px(p.w - 3, 1, MASK_D)
    p.px(p.w - 1, 5, MASK_D)
    # contorno brilha de leve no escuro
    for x in range(p.w):
        p.px(x, 0, 0xd8d4cc, True)


def finger(p):
    p.fill(0xb8b4ac, 0.02)


def shade():
    M = Model('shade', 64, seed=33)
    head = M.part('head', pivot=(0, -1, 0))
    head.box((-4, -9, -4), (8, 9, 8), SH, faces={**every(smoke(2)), 'front': hood_front}, noise=0.0)
    head.box((-2, -11, -1), (4, 2, 4), SH, faces=every(smoke(3)), noise=0.0)
    head.child('mask', pivot=(0, -4.5, -3.6)).box((-3, -3.5, -0.5), (6, 7, 1), MASK, faces={'front': mask})
    body = M.part('body', pivot=(0, -1, 0))
    body.box((-3.5, 0, -2), (7, 10, 4), SH, faces=every(smoke(4)), noise=0.0)
    skirt = body.child('skirt', pivot=(0, 10, 0))
    skirt.box((-4.5, 0, -3), (9, 9, 6), SH, faces=every(smoke(5, True)), noise=0.0)
    for i, (x, z) in enumerate(((-2.5, -1), (1.5, 0), (-0.5, 1.5))):
        skirt.child('wisp_%d' % i, pivot=(x, 9, z)).box((-1, 0, -1), (2, 4 - i % 2, 2), SH_E, faces=every(smoke(6 + i, True)), noise=0.0)
    # braços finíssimos e compridos, dedos pálidos
    for name, x in (('right_arm', -4.5), ('left_arm', 4.5)):
        arm = M.part(name, pivot=(x, 1, 0))
        arm.box((-1, -1, -1), (2, 13, 2), SH, faces=every(smoke(9)), noise=0.0)
        for i, fx in enumerate((-0.6, 0.0, 0.6)):
            arm.child('%s_finger_%d' % (name, i), pivot=(fx, 12, -0.3 + 0.3 * (i % 2))).box((-0.3, 0, -0.3), (0.6, 6 - (i % 2) * 2 + i, 0.6), 0xb8b4ac, faces=every(finger))
    return M



for M, glow in ((sheep(), False), (bogeyman(), True), (shade(), True)):
    M.pack()
    M.paint(T + 'entity/%s.png' % M.name, T + 'entity/%s_glow.png' % M.name if glow else None)
    open(OUT + '%s_layer.txt' % M.name, 'w', encoding='utf-8').write(M.java())
    try:
        preview.render(M, T + 'entity/%s.png' % M.name, OUT + '%s_view.png' % M.name)
    except Exception as e:
        print('sem preview', M.name, e)
    print('ok', M.name, M.tex_w, M.tex_h)


# =============================================================== gnomos (recolor)
def recolor_gnome(name, hat, skin=None, beard=None, eyes=None):
    src = Image.open(T + 'entity/gnome.png').convert('RGBA')
    out = src.copy()
    glow = Image.new('RGBA', src.size, (0, 0, 0, 0))
    ps, po, pg = src.load(), out.load(), glow.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = ps[x, y]
            if not a:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            new = None
            if s > 0.55 and (h < 0.05 or h > 0.95) and y < 32 and not (16 <= x < 28 and 6 <= y < 20):
                new = hat(h, s, v)
            elif skin and 0.03 < h < 0.12 and 0.2 < s < 0.6 and v > 0.5:
                new = skin(h, s, v)
            elif beard and s < 0.08 and v > 0.7 and v < 0.999:
                new = beard(h, s, v)
            elif eyes and v < 0.2 and 8 <= y <= 14 and x < 12:
                new = eyes
                pg[x, y] = eyes + (255,)
            if new:
                po[x, y] = new + (a,) if len(new) == 3 else new
    out.save(T + 'entity/%s.png' % name)
    if eyes:
        glow.save(T + 'entity/%s_glow.png' % name)


def hsv(h, s, v):
    r, g, b = colorsys.hsv_to_rgb(h % 1, max(0, min(1, s)), max(0, min(1, v)))
    return (int(r * 255), int(g * 255), int(b * 255))


recolor_gnome('gnome_merchant', lambda h, s, v: hsv(0.47, s * 0.8, v * 0.9))
recolor_gnome('corrupted_gnome', lambda h, s, v: hsv(0.0, s * 0.7, v * 0.35), skin=lambda h, s, v: hsv(0.28, 0.2, v * 0.7),
              beard=lambda h, s, v: hsv(0.7, 0.05, v * 0.4), eyes=(230, 40, 40))
print('ok gnomos')


# =============================================================== ovos
def egg(name, base, spots):
    src = Image.open(T + 'item/gnome_spawn_egg.png').convert('RGBA')
    out = Image.new('RGBA', src.size, (0, 0, 0, 0))
    sp, op = src.load(), out.load()
    rnd = random.Random(name)
    for y in range(src.height):
        for x in range(src.width):
            c = sp[x, y]
            if not c[3]:
                continue
            lum = (c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11) / 255
            col = ((base >> 16) & 255, (base >> 8) & 255, base & 255)
            pix = tuple(max(0, min(255, int(v * (0.55 + lum * 0.7)))) for v in col)
            if rnd.random() < 0.18:
                sc = rnd.choice(spots)
                pix = ((sc >> 16) & 255, (sc >> 8) & 255, sc & 255)
            op[x, y] = pix + (255,)
    out.save(T + 'item/%s.png' % name)


egg('counting_sheep_spawn_egg', 0xf4f0ff, [0x9a8fc8, 0xd9d0f0])
egg('gnome_merchant_spawn_egg', 0x2f8f86, [0xf0d0b0, 0xffffff])
egg('corrupted_gnome_spawn_egg', 0x2a2a2a, [0x5a1414, 0x8a9a7a])
egg('bogeyman_spawn_egg', 0x2a1d24, [0xffd23a, 0x3b3466])
egg('shade_spawn_egg', 0x1c2233, [0xe8e4dc, 0x0d1019])
print('ok ovos')


# =============================================================== peças extras das variantes do gnomo (área livre da textura)
def box_regions(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


def paint_box(img, u, v, size, fn, glow=None):
    w, h, d = size
    for face, (x0, y0, fw, fh) in box_regions(u, v, w, h, d).items():
        for y in range(fh):
            for x in range(fw):
                c = fn(face, x, y, fw, fh)
                if c is None:
                    continue
                g = False
                if isinstance(c, tuple) and len(c) == 2:
                    c, g = c
                rgb = _shade(c, 1.0 - (0.18 * y / max(1, fh - 1) if face not in ('top', 'bottom') else (0.0 if face == 'top' else 0.25)))
                img.putpixel((x0 + x, y0 + y), rgb + (255,))
                if g and glow is not None:
                    glow.putpixel((x0 + x, y0 + y), rgb + (255,))


def merchant_extras():
    p = T + 'entity/gnome_merchant.png'
    img = Image.open(p).convert('RGBA')
    LEATHER, LEATHER_D, STRAP = 0x8a5a34, 0x6a4226, 0x4a2e1a

    def pack(face, x, y, fw, fh):
        if face == 'back' and y == 1 and 1 <= x < fw - 1:
            return STRAP
        if face == 'back' and 2 <= y <= 3 and fw // 2 - 1 <= x <= fw // 2:
            return 0xd8b040 if y == 2 else LEATHER_D  # fivela
        if face in ('left', 'right') and y == fh // 2:
            return STRAP
        return LEATHER if (x + y // 2) % 5 else LEATHER_D
    paint_box(img, 36, 0, (5, 5, 3), pack)
    paint_box(img, 36, 10, (6, 2, 2), lambda f, x, y, fw, fh: 0x3f6f8a if (x % 3) else 0x2f4f6a)  # saco de dormir azul listrado
    paint_box(img, 54, 10, (2, 2, 2), lambda f, x, y, fw, fh: 0x6a6a72 if f != 'top' else 0x2a2a30)  # panelinha
    paint_box(img, 54, 16, (2, 1, 2), lambda f, x, y, fw, fh: 0x3a3a40)  # tampa do lampião
    # o "cristal" do cajado (uv 16,19, 3x3x3) vira lampião: vidro amarelo com moldura escura
    paint_box(img, 16, 19, (3, 3, 3), lambda f, x, y, fw, fh: 0x3a3a40 if f in ('top', 'bottom') or y == fh - 1
              else (0xffe08a if y == 0 else 0xffc850))
    img.save(p)


def corrupted_extras():
    p, gp = T + 'entity/corrupted_gnome.png', T + 'entity/corrupted_gnome_glow.png'
    img = Image.open(p).convert('RGBA')
    glow = Image.open(gp).convert('RGBA')
    for i in range(3):
        paint_box(img, 36 + i * 4, 20, (1, 3, 1), lambda f, x, y, fw, fh: 0x1a1418 if y else 0x3a2a30)  # espinhos
    for i, n in enumerate((4, 3, 5)):
        paint_box(img, 48 + i * 4, 20, (1, n, 1), lambda f, x, y, fw, fh: 0x4a5a3a if (y % 3) else 0x6a7a4a)  # hera-insone
    for i in range(3):
        paint_box(img, 36 + i * 4, 26, (1, 2, 1), lambda f, x, y, fw, fh: 0x2a2228 if y == 0 else 0xc8c0a8)  # garras
    # cristal rachado do cajado brilha vermelho
    paint_box(img, 16, 19, (3, 3, 3), lambda f, x, y, fw, fh: (0x3a0a0a, False) if x == y else (0xe03030 if y else 0xff6a5a, True), glow)
    img.save(p)
    glow.save(gp)


merchant_extras()
corrupted_extras()
print('ok extras dos gnomos')


# =============================================================== Gnomo Minerador
def miner_extras():
    recolor_gnome('gnome_miner', lambda h, s, v: hsv(h, s, v), beard=lambda h, s, v: hsv(0.08, 0.06, v * 0.78))
    p, gp = T + 'entity/gnome_miner.png', T + 'entity/gnome_miner_glow.png'
    img = Image.open(p).convert('RGBA')
    glow = Image.new('RGBA', img.size, (0, 0, 0, 0))
    HELM, HELM_D = 0xe0b030, 0xb88a20

    def helmet(f, x, y, fw, fh):
        if f in ('front', 'left', 'right', 'back') and y == fh - 1:
            return HELM_D
        return HELM if f != 'bottom' else 0x9a7418
    paint_box(img, 36, 0, (6, 3, 6), helmet)
    paint_box(img, 36, 10, (7, 1, 7), lambda f, x, y, fw, fh: HELM_D if f != 'bottom' else 0xa07a1c)
    # lanterna do capacete: vidro que brilha + aro escuro
    paint_box(img, 36, 18, (2, 2, 1), lambda f, x, y, fw, fh: (0xfff0a0, True) if f == 'front' else 0x4a4a50, glow)
    # pontinha do gorro vermelho escapando do capacete
    paint_box(img, 44, 18, (2, 3, 2), lambda f, x, y, fw, fh: 0xc8302a if y < fh - 1 else 0xa02420)
    # cabeça da picareta (ferro, pontas mais claras)
    paint_box(img, 52, 18, (1, 1, 5), lambda f, x, y, fw, fh: 0xd0d0d8 if (x in (0, fw - 1)) else 0x8a8a94)
    # saco de minério: lona marrom com amarração no topo
    paint_box(img, 36, 24, (4, 4, 3), lambda f, x, y, fw, fh: 0x6a5434 if f == 'top' else (0x5a4428 if y == 0 else 0x8a7048))
    paint_box(img, 50, 24, (1, 1, 1), lambda f, x, y, fw, fh: 0xd8a070)  # cobre
    paint_box(img, 54, 24, (1, 1, 1), lambda f, x, y, fw, fh: 0x7ad8e8)  # mithril
    # cabo da picareta (uv 22,25, 1x8x1 cabe na região do cajado): madeira
    paint_box(img, 22, 25, (1, 11, 1), lambda f, x, y, fw, fh: 0x7a5432 if y % 4 else 0x5e3f24)
    img.save(p)
    glow.save(gp)


miner_extras()
egg('gnome_miner_spawn_egg', 0xe0b030, [0x6a5434, 0x8a8a94])
print('ok minerador')
