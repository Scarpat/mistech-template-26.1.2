# Visual próprio dos Artefatos Rúnicos: modelos 3D na mão, ícones desenhados e armadura vestida pintada à mão.
# Uso (na pasta do projeto): python3 tools/artifacts/gen_designs.py [pasta_previa]
# Gera: models/item/<x>_3d.json + textures/item/<x>_model.png, items/<x>.json (ícone no inventário, 3D na mão),
#       ícones textures/item/<x>.png e textures/entity/equipment/humanoid/<x>.png.
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
from boxmodel import Model, hexc, mix, jitter, solid, grain, bands, speckle, per_face, write_model

A = 'src/main/resources/assets/mistech/'
PREV = sys.argv[1] if len(sys.argv) > 1 else None

# ------------------------------------------------------------------ transformações de exibição
def tool_display(grip, scale=0.85, fp_scale=0.68):
    """Ferramenta comprida em pé (eixo Y); grip = altura do modelo onde a mão segura."""
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


def trinket_display(scale=0.55, fp=0.6, rot3=(10, 30, 0), rot1=(0, -60, 15)):
    return {
        'thirdperson_righthand': {'rotation': list(rot3), 'translation': [0, 3, 1], 'scale': [scale] * 3},
        'thirdperson_lefthand': {'rotation': [rot3[0], -rot3[1], -rot3[2]], 'translation': [0, 3, 1], 'scale': [scale] * 3},
        'firstperson_righthand': {'rotation': list(rot1), 'translation': [1.13, 3.2, 1.13], 'scale': [fp] * 3},
        'firstperson_lefthand': {'rotation': [rot1[0], -rot1[1], -rot1[2]], 'translation': [1.13, 3.2, 1.13], 'scale': [fp] * 3},
        'gui': {'rotation': [25, -40, 0], 'scale': [0.8] * 3},
        'ground': {'translation': [0, 2, 0], 'scale': [0.4] * 3},
        'fixed': {'scale': [0.8] * 3},
    }


def item_def(name, model3d, icon_contexts=('gui', 'ground', 'fixed', 'on_shelf')):
    """Ícone 2D no inventário/chão/moldura; modelo 3D nas mãos (e no resto)."""
    j = {'model': {'type': 'minecraft:select', 'property': 'minecraft:display_context',
                   'cases': [{'when': list(icon_contexts), 'model': {'type': 'minecraft:model', 'model': 'mistech:item/' + name}}],
                   'fallback': {'type': 'minecraft:model', 'model': 'mistech:item/' + model3d}}}
    json.dump(j, open(A + 'items/' + name + '.json', 'w'), indent=2)


def emit(m, display, icon_name, icon_contexts=('gui', 'ground', 'fixed', 'on_shelf')):
    write_model(m, A, display, m.name + '_model' if not m.name.endswith('_3d') else m.name[:-3] + '_model')
    item_def(icon_name, m.name, icon_contexts)
    if PREV:
        os.makedirs(PREV, exist_ok=True)
        m.preview(PREV + '/' + m.name + '.png', yaws=(35, 125, 215), pitch=20, scale=6, fit=44)


# ------------------------------------------------------------------ cores
WOOD_D, WOOD_L = hexc('3B2414'), hexc('5E3B20')
BIRCH_D, BIRCH_L = hexc('C9C2B0'), hexc('EDE8DA')
STRAW_D, STRAW, STRAW_L = hexc('8A6A1E'), hexc('C99A3A'), hexc('EBCB74')
CLOUD_D, CLOUD, CLOUD_L = hexc('A9BFDC'), hexc('DDE8F7'), hexc('FFFFFF')
ORI_D, ORI, ORI_L = hexc('8A4E10'), hexc('E39A33'), hexc('FFD27A')
BASALT_D, BASALT, BASALT_L = hexc('1A1514'), hexc('2E2624'), hexc('463A36')
MAGMA, MAGMA_L, MAGMA_Y = hexc('C2401A'), hexc('FF7A1E'), hexc('FFC94A')
ADAM_D, ADAM, ADAM_L = hexc('5E1622'), hexc('A32B3C'), hexc('E8576A')
AQUA_D, AQUA, AQUA_L, FOAM = hexc('0F5E66'), hexc('22A3A3'), hexc('6FE3D6'), hexc('E2FFFA')
VOID_D, VOID, VOID_L, VOID_G = hexc('140A24'), hexc('3B1E66'), hexc('8A4FD8'), hexc('E2C8FF')
GLASS, GLASS_L, SAND = hexc('BFE8F0'), hexc('E8FBFF'), hexc('E8C77A')
SKY_D, SKY, SKY_L, BOLT = hexc('2C4E6E'), hexc('6FA9D8'), hexc('BFE2FF'), hexc('FFE14A')
GOLD_D, GOLD, GOLD_L, BEAM = hexc('8A5A0E'), hexc('E8B830'), hexc('FFF0A0'), hexc('7FF0FF')

# ------------------------------------------------------------------ vassouras
def bristles(d, m, l):
    """Palha: fios verticais com pontas mais claras embaixo."""
    def p(face, w, h, rnd):
        im = Image.new('RGBA', (w, h))
        for x in range(w):
            base = [d, m, l][(x * 7 + rnd.randint(0, 2)) % 3]
            for y in range(h):
                c = mix(base, l, 0.35) if (face not in ('up', 'down') and y >= h - 1) else base
                im.putpixel((x, y), jitter(c, rnd, 8))
        return im
    return p


def puff(face, w, h, rnd):
    im = Image.new('RGBA', (w, h))
    for y in range(h):
        for x in range(w):
            c = CLOUD_L if (face == 'up' or y == 0) else (CLOUD if rnd.random() < 0.7 else CLOUD_D)
            if face == 'down':
                c = CLOUD_D
            im.putpixel((x, y), jitter(c, rnd, 5))
    return im


def broom(name, cloud):
    m = Model(name + '_3d', 64, seed=11 if cloud else 7)
    wood = grain(BIRCH_D, BIRCH_L) if cloud else grain(WOOD_D, WOOD_L)
    band = solid(ORI, 6, top=ORI_L, bottom=ORI_D) if cloud else solid(hexc('6B4A2A'), 6, top=hexc('9A7048'))
    # cabo (y 1 → 24); a ponta de cima vai para a frente quando montada
    m.box((7.25, 1, 7.25), (8.75, 24, 8.75), wood, name='cabo')
    m.box((7, 22, 7), (9, 23, 9), band, name='anel_ponta')
    m.box((7, 12, 7), (9, 13, 9), band, name='anel_meio')
    if cloud:
        m.box((7.25, 24, 7.25), (8.75, 25.5, 8.75), solid(BEAM, 6, top=hexc('E8FFFF')), name='cristal')
    else:
        m.box((7, 24, 7), (9, 25, 9), solid(WOOD_D, 5), name='pomo')
    # amarração + palha (abre para trás)
    m.box((6.5, 0, 6.5), (9.5, 2.5, 9.5), band, name='amarra')
    if cloud:
        br = bristles(CLOUD_D, CLOUD, CLOUD_L)
    else:
        br = bristles(STRAW_D, STRAW, STRAW_L)
    m.box((6, -4, 6), (10, 0, 10), br, name='palha_1')
    m.box((4.5, -9, 5), (11.5, -4, 11), br, name='palha_2')
    m.box((3.5, -11, 5.5), (12.5, -9, 10.5), br, name='palha_3')
    if cloud:
        m.box((3.5, -8, 7), (5.5, -5, 10), puff, name='nuvem_1')
        m.box((10.5, -10, 6.5), (12.5, -7, 9.5), puff, name='nuvem_2')
        m.box((7, -12, 4.5), (10, -10, 6.5), puff, name='nuvem_3')
        m.box((6.5, -7, 10.5), (9.5, -4, 12), puff, name='nuvem_4')
    return m


for nm, cl in (('broom', False), ('cloud_broom', True)):
    emit(broom(nm, cl), tool_display(14, 0.8, 0.6), nm)

# ------------------------------------------------------------------ martelo vulcânico
def magma_rock(face, w, h, rnd):
    im = Image.new('RGBA', (w, h))
    for y in range(h):
        for x in range(w):
            im.putpixel((x, y), jitter(rnd.choice((BASALT_D, BASALT, BASALT, BASALT_L)), rnd, 5))
    # rachaduras de magma
    for _ in range(max(1, w * h // 14)):
        x, y = rnd.randrange(w), rnd.randrange(h)
        for _ in range(rnd.randint(2, 4)):
            if 0 <= x < w and 0 <= y < h:
                im.putpixel((x, y), jitter(rnd.choice((MAGMA, MAGMA_L, MAGMA_L, MAGMA_Y)), rnd, 6))
            x += rnd.choice((-1, 0, 1))
            y += rnd.choice((0, 1))
    return im


def hammer():
    m = Model('volcanic_hammer_3d', 64, seed=21)
    grip = bands([ADAM_D, ADAM, ADAM, ADAM_D])
    m.box((7.25, -4, 7.25), (8.75, 14, 8.75), grip, name='cabo')
    m.box((6.75, -5.5, 6.75), (9.25, -3.5, 9.25), solid(ADAM_D, 6, top=ADAM_L), name='pomo')
    m.box((7, 6, 7), (9, 7, 9), solid(ORI, 6, top=ORI_L), name='anel')
    m.box((6.5, 12, 6.5), (9.5, 14, 9.5), solid(ADAM_D, 6, top=ADAM), name='colar')
    m.box((3, 14, 5.5), (13, 19.5, 10.5), magma_rock, name='cabeca')
    m.box((1.5, 13.5, 5), (3, 20, 11), solid(ADAM, 8, top=ADAM_L, bottom=ADAM_D), name='face_esq')
    m.box((13, 13.5, 5), (14.5, 20, 11), solid(ADAM, 8, top=ADAM_L, bottom=ADAM_D), name='face_dir')
    m.box((6.5, 19.5, 7), (9.5, 20.5, 9), solid(MAGMA_L, 8, top=MAGMA_Y), name='nucleo')
    return m


emit(hammer(), tool_display(2, 0.85, 0.65), 'volcanic_hammer')

# ------------------------------------------------------------------ lança das marés
def spear():
    m = Model('tide_spear_3d', 64, seed=31)
    m.box((7.5, -8, 7.5), (8.5, 21, 8.5), bands([ORI, ORI_L, ORI, ORI_D]), name='haste')
    m.box((7.25, 2, 7.25), (8.75, 7, 8.75), bands([AQUA_D, AQUA]), name='empunhadura')
    m.box((7.25, -9, 7.25), (8.75, -7.5, 8.75), solid(ORI_D, 5, top=ORI), name='ponteira')
    m.box((4.5, 21, 7.25), (11.5, 22.5, 8.75), solid(ORI, 6, top=ORI_L, bottom=ORI_D), name='travessa')
    m.box((7.25, 21.25, 6.75), (8.75, 22.75, 7.25), solid(FOAM, 4), name='perola')
    prong = solid(AQUA, 8, top=FOAM, bottom=AQUA_D)
    m.box((7.4, 22.5, 7.4), (8.6, 30, 8.6), prong, name='dente_meio')
    m.box((4.5, 22.5, 7.5), (5.5, 27.5, 8.5), prong, name='dente_esq')
    m.box((10.5, 22.5, 7.5), (11.5, 27.5, 8.5), prong, name='dente_dir')
    m.box((3.75, 25, 7.6), (4.5, 26.5, 8.4), solid(AQUA_L, 5), name='farpa_esq')
    m.box((11.5, 25, 7.6), (12.25, 26.5, 8.4), solid(AQUA_L, 5), name='farpa_dir')
    return m


emit(spear(), tool_display(4, 0.8, 0.6), 'tide_spear', ('gui', 'ground', 'fixed', 'on_shelf'))

# ------------------------------------------------------------------ ímã do vazio
def magnet():
    m = Model('void_magnet_3d', 64, seed=41)
    body = speckle(VOID, [VOID_D, VOID_L], 0.2)
    m.box((3.5, 3, 7), (12.5, 5.5, 9), body, name='curva')
    m.box((3.5, 5.5, 7), (6, 11, 9), body, name='perna_esq')
    m.box((10, 5.5, 7), (12.5, 11, 9), body, name='perna_dir')
    tip = solid(VOID_G, 6, bottom=VOID_L)
    m.box((3.5, 11, 7), (6, 13, 9), tip, name='polo_esq')
    m.box((10, 11, 7), (12.5, 13, 9), tip, name='polo_dir')
    m.box((7.5, 13.5, 7.5), (8.5, 14.5, 8.5), solid(VOID_G, 4), name='faisca')
    return m


emit(magnet(), trinket_display(), 'void_magnet')

# ------------------------------------------------------------------ ampulheta de bolso
def bulb(lower):
    def p(face, w, h, rnd):
        im = Image.new('RGBA', (w, h))
        for y in range(h):
            for x in range(w):
                if face == 'up':
                    c = SAND if lower else GLASS
                elif face == 'down':
                    c = SAND if not lower or True else GLASS
                else:
                    filled = (y >= h // 2) if lower else (y >= h - 1)
                    c = SAND if filled else (GLASS_L if (x == 0 or y == 0) else GLASS)
                im.putpixel((x, y), jitter(c, rnd, 4))
        return im
    return p


def hourglass():
    m = Model('pocket_hourglass_3d', 64, seed=51)
    plate = solid(GOLD, 6, top=GOLD_L, bottom=GOLD_D)
    m.box((4.5, 2, 4.5), (11.5, 3, 11.5), plate, name='base')
    m.box((4.5, 13, 4.5), (11.5, 14, 11.5), plate, name='topo')
    for i, (x, z) in enumerate(((5, 5), (10, 5), (5, 10), (10, 10))):
        m.box((x, 3, z), (x + 1, 13, z + 1), solid(hexc('5A3A1E'), 5), name='coluna_%d' % i)
    m.box((6, 3, 6), (10, 7.5, 10), bulb(True), name='bulbo_baixo')
    m.box((7.25, 7.5, 7.25), (8.75, 8.5, 8.75), solid(SAND, 4), name='gargalo')
    m.box((6, 8.5, 6), (10, 13, 10), bulb(False), name='bulbo_cima')
    m.box((7.5, 14, 7.5), (8.5, 15, 8.5), solid(hexc('5FD6B0'), 4), name='gema')
    return m


emit(hourglass(), trinket_display(), 'pocket_hourglass')

# ------------------------------------------------------------------ anel espacial
def ring():
    m = Model('spatial_ring_3d', 64, seed=61)
    band = solid(ORI, 6, top=ORI_L, bottom=ORI_D)
    m.box((5, 4, 7.25), (11, 5, 8.75), band, name='aro_baixo')
    m.box((4, 5, 7.25), (5, 10, 8.75), band, name='aro_esq')
    m.box((11, 5, 7.25), (12, 10, 8.75), band, name='aro_dir')
    m.box((5, 10, 7.25), (11, 11, 8.75), band, name='aro_cima')
    m.box((6.5, 11, 6.5), (9.5, 12, 9.5), solid(ORI_D, 5, top=ORI), name='engaste')
    m.box((7, 12, 7), (9, 14.5, 9), solid(VOID_L, 6, top=VOID_G, bottom=VOID), name='gema')
    return m


emit(ring(), trinket_display(0.45, 0.5), 'spatial_ring')

# ------------------------------------------------------------------ manopla da tempestade
def gauntlet():
    m = Model('storm_gauntlet_3d', 64, seed=71)
    plate = solid(SKY, 8, top=SKY_L, bottom=SKY_D)
    m.box((5, 1, 5.5), (11, 7, 10.5), bands([SKY_D, SKY, SKY, SKY_L]), name='punho')
    m.box((4.5, 6, 5), (11.5, 7.5, 11), solid(GOLD, 6, top=GOLD_L), name='virola')
    m.box((5, 7.5, 5.5), (11, 12, 10.5), plate, name='mao')
    for i in range(4):
        x = 5 + i * 1.5
        m.box((x, 12, 5.5), (x + 1.25, 15 - (0 if i in (1, 2) else 0.75), 7.5), plate, name='dedo_%d' % i)
    m.box((11, 8, 6), (12.5, 11, 8), plate, name='polegar')
    m.box((7, 9, 10.5), (9, 11, 11.25), solid(BOLT, 6, top=hexc('FFFFFF')), name='gema')
    m.box((6, 3, 10.5), (7, 5, 11), solid(BOLT, 4), name='raio_1')
    m.box((7, 2, 10.5), (8, 4, 11), solid(BOLT, 4), name='raio_2')
    return m


emit(gauntlet(), trinket_display(0.55, 0.6), 'storm_gauntlet')

# ------------------------------------------------------------------ coroa do farol (na mão)
def crown():
    m = Model('beacon_crown_3d', 64, seed=81)
    band = solid(GOLD, 6, top=GOLD_L, bottom=GOLD_D)
    m.box((3, 3, 3), (13, 6, 4), band, name='frente')
    m.box((3, 3, 12), (13, 6, 13), band, name='tras')
    m.box((3, 3, 4), (4, 6, 12), band, name='esq')
    m.box((12, 3, 4), (13, 6, 12), band, name='dir')
    spike = solid(GOLD, 5, top=GOLD_L)
    for i, (x, z) in enumerate(((3, 3), (7.5, 3), (12, 3), (3, 12), (7.5, 12), (12, 12), (3, 7.5), (12, 7.5))):
        h = 9 if x == 7.5 or z == 7.5 else 8
        m.box((x, 6, z), (x + 1, h, z + 1), spike, name='ponta_%d' % i)
    m.box((7, 4, 2.5), (9, 6, 3), solid(BEAM, 5, top=hexc('E8FFFF')), name='gema')
    m.box((7.25, 3, 7.25), (8.75, 7, 8.75), solid(BEAM, 6, top=hexc('E8FFFF')), name='luz')
    return m


emit(crown(), trinket_display(0.5, 0.55), 'beacon_crown')
print('modelos ok')

# Ícones 2D: ver tools/artifacts/gen_icons.py

# ================================================================== armadura vestida (64×32)
EQ = A + 'textures/entity/equipment/humanoid/'


def cube(u, v, w, h, d):
    """Regiões da UV de um cubo: top, bottom, right(fora), front, left(dentro), back → (x0, y0, w, h)."""
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


HEAD, BODY, ARM, LEG = cube(0, 0, 8, 8, 8), cube(16, 16, 8, 12, 4), cube(40, 16, 4, 12, 4), cube(0, 16, 4, 12, 4)
import random as _r


def canvas():
    return Image.new('RGBA', (64, 32))


def fill(im, reg, fn):
    x0, y0, w, h = reg
    for y in range(h):
        for x in range(w):
            c = fn(x, y, w, h)
            if c:
                im.putpixel((x0 + x, y0 + y), c)


# --- Botas de Vento: botas brancas com asinhas e espirais
def wind_boots():
    im, rnd = canvas(), _r.Random(5)
    W, S, D, T, F = hexc('DFF6F8'), hexc('9FD3DD'), hexc('2E5566'), hexc('6FA9BE'), hexc('FFFFFF')
    top = 6  # as botas ocupam as 6 linhas de baixo da perna
    for side in ('right', 'front', 'left', 'back'):
        def f(x, y, w, h, side=side):
            if y < h - top:
                # asinha subindo pela lateral de fora
                if side == 'right' and h - top - 4 <= y < h - top and x >= 1 + (h - top - 1 - y) // 2 * 0 and (x + y) % 2 == 0:
                    return F if x > 0 else S
                return None
            yy = y - (h - top)
            if yy == 0:
                return D if x % 2 == 0 else T
            if yy == top - 1:
                return D
            if yy == top - 2:
                return T
            if side == 'front' and (x, yy) in ((1, 2), (2, 2), (2, 3)):
                return S
            if side == 'right' and yy in (1, 2) and x in (1, 2):
                return F
            return jitter(W, rnd, 6)
        fill(im, LEG[side], f)
    fill(im, LEG['bottom'], lambda x, y, w, h: D if (x in (0, w - 1) or y in (0, h - 1)) else T)
    im.save(EQ + 'wind_boots.png')


# --- Patins de Gelo: botas azul-escuro com pelo branco e lâmina prateada
def frost_skates():
    im, rnd = canvas(), _r.Random(6)
    B, b, D, F, f, C, S = hexc('3F7FD0'), hexc('2D63AE'), hexc('1D3F7A'), hexc('F4FBFF'), hexc('C9DCEA'), hexc('BFF0FF'), hexc('DDE8F0')
    top = 7
    for side in ('right', 'front', 'left', 'back'):
        def f2(x, y, w, h, side=side):
            if y < h - top:
                return None
            yy = y - (h - top)
            if yy == 0:
                return jitter(F if rnd.random() < 0.7 else f, rnd, 4)
            if yy == 1:
                return f if x % 2 else F
            if yy == top - 1:
                return S if side in ('right', 'left') else D
            if side in ('front', 'right') and (x + yy) % 4 == 0 and 2 <= yy <= 4:
                return C
            return jitter(B if (x + yy) % 3 else b, rnd, 5)
        fill(im, LEG[side], f2)
    fill(im, LEG['bottom'], lambda x, y, w, h: hexc('E6EEF4') if x in (1, 2) else hexc('8E9AA6'))
    im.save(EQ + 'frost_skates.png')


# --- Máscara das Marés: faixa nos olhos com furos, crista de barbatana e guelras
def tide_mask():
    im, rnd = canvas(), _r.Random(7)
    T, t, D, C, P, G = hexc('17808A'), hexc('4FD2C9'), hexc('0B3A42'), hexc('6FE3D6'), hexc('F4FFFC'), hexc('0F5E66')
    band = (2, 6)  # linhas da faixa (da testa até abaixo dos olhos)

    def front(x, y, w, h):
        if x in (3, 4) and y < band[0]:
            return C if y == 0 else t  # crista
        if band[0] <= y < band[1]:
            if y in (4, 5) and x in (1, 2, 5, 6) and not (y == 5 and x in (1, 6)):
                return None  # furo dos olhos
            if y == band[0]:
                return D
            if (x, y) in ((3, 3), (4, 3)):
                return P  # pérola
            return jitter(T if (x + y) % 3 else t, rnd, 5)
        if y == band[1] and x in (1, 2, 5, 6):
            return D
        return None

    def side(x, y, w, h):
        if band[0] <= y < band[1]:
            return D if y == band[0] else jitter(T, rnd, 5)
        if 6 <= y <= 7 and x in (5, 6) and (x + y) % 2:
            return G  # guelras
        return None

    def back(x, y, w, h):
        if x in (3, 4) and y < 5:
            return C if y == 0 else t
        if band[0] <= y < band[1]:
            return D if y == band[0] else jitter(T, rnd, 5)
        return None

    fill(im, HEAD['front'], front)
    fill(im, HEAD['right'], side)
    fill(im, HEAD['left'], lambda x, y, w, h: side(w - 1 - x, y, w, h))
    fill(im, HEAD['back'], back)
    fill(im, HEAD['top'], lambda x, y, w, h: (C if y < 2 else t) if x in (3, 4) else None)
    im.save(EQ + 'tide_mask.png')


# --- Coroa do Farol: aro de ouro com pontas e gema ciano na testa
def beacon_crown():
    im, rnd = canvas(), _r.Random(8)
    G, g, L, D, B = hexc('E8B830'), hexc('C9961E'), hexc('FFF0A0'), hexc('8A5A0E'), hexc('7FF0FF')

    def band(x, y, w, h, gem=False):
        if y == 0:
            return L if x % 3 == 1 else None  # pontas
        if y == 1:
            return G if x % 3 != 2 else g
        if y == 2:
            if gem and x in (3, 4):
                return B
            return jitter(G, rnd, 6)
        if y == 3:
            if gem and x in (3, 4):
                return hexc('E8FFFF')
            return D
        return None

    fill(im, HEAD['front'], lambda x, y, w, h: band(x, y, w, h, True))
    for s in ('right', 'left', 'back'):
        fill(im, HEAD[s], band)
    im.save(EQ + 'beacon_crown.png')


# --- Manto das Sombras: aberto na frente, fecho de gema, runa nas costas, mangas com punho lilás
def shadow_cloak():
    im, rnd = canvas(), _r.Random(9)
    C, c, K, L, V, G = hexc('2A1840'), hexc('3A2257'), hexc('140A24'), hexc('B28CE0'), hexc('8A4FD8'), hexc('E2C8FF')

    def cloth(x, y, w, h):
        return jitter(C if (x + y // 2) % 4 else c, rnd, 4)

    def front(x, y, w, h):
        if y < 2 and x in (3, 4):
            return G if y == 0 else V  # fecho
        if x in (3, 4) and y >= 2:
            return None  # aberto
        if x in (2, 5) and y >= 2:
            return L  # debrum da abertura
        if y == h - 1:
            return L if x % 2 == 0 else None
        return cloth(x, y, w, h)

    rune = {(3, 3), (4, 3), (2, 4), (5, 4), (2, 5), (5, 5), (3, 6), (4, 6), (3, 5), (4, 4), (3, 8), (4, 8), (3, 9), (4, 9)}

    def back(x, y, w, h):
        if y == h - 1:
            return L if x % 2 == 0 else None
        if (x, y) in rune:
            return L if (x, y) != (4, 4) else G
        cc = cloth(x, y, w, h)
        return mix(cc, K, min(0.6, y / h)) if y > 6 else cc

    def side(x, y, w, h):
        if y == h - 1:
            return L if x % 2 == 0 else None
        return cloth(x, y, w, h)

    fill(im, BODY['front'], front)
    fill(im, BODY['back'], back)
    fill(im, BODY['right'], side)
    fill(im, BODY['left'], side)
    fill(im, BODY['top'], lambda x, y, w, h: c)

    def sleeve(x, y, w, h):
        if y < 3:
            return K if y < 2 else V  # ombreira
        if y == 3:
            return L
        if y >= h - 2:
            return L if y == h - 2 else V  # punho
        return cloth(x, y, w, h)

    for s in ('right', 'front', 'left', 'back'):
        fill(im, ARM[s], sleeve)
    fill(im, ARM['top'], lambda x, y, w, h: K)
    im.save(EQ + 'shadow_cloak.png')


wind_boots()
frost_skates()
tide_mask()
beacon_crown()
shadow_cloak()
print('ícones e armaduras ok')
