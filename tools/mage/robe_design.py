"""Vestes de Mago v2: textura da armadura (64x32) + partes 3D (chapéu de mago, gola, saia longa, punhos largos, bico dos sapatos).

Gera, para cada nível (hemp/initiate/adept/archmage):
  - textures/entity/equipment/humanoid/mage_robe_base.png        (tingível, cinza: corpo, braços, sapatos)
  - textures/entity/equipment/humanoid/mage_<nível>_trim.png     (enfeites sem tinta)
  - textures/entity/equipment/humanoid_leggings/mage_pants_base.png + mage_<nível>_trim.png
  - textures/entity/mage_wear_base.png                            (partes 3D, tingível)
  - textures/entity/mage_wear_<nível>_trim.png                    (enfeites das partes 3D)
  - o código Java das partes 3D (impresso / gravado em build/mage_wear_layer.txt)
  - preview: build/mage_robes_preview.png (com a pele do Steve, se passar a pasta vanilla)

Uso (raiz do projeto): python tools/mage/robe_design.py [pasta_texturas_vanilla]
"""
import math, os, random, sys
from PIL import Image, ImageDraw

sys.path.insert(0, 'tools/modelgen')
import preview as pv

VAN = (sys.argv[1] if len(sys.argv) > 1 else 'vanilla/assets/minecraft/textures').rstrip('/') + '/'
T = 'src/main/resources/assets/mistech/textures/'
OUT_BUILD = 'build/'
os.makedirs(OUT_BUILD, exist_ok=True)

TIERS = {
    #            cor padrão  enfeite principal   enfeite 2 (gema/estrela)  escuro do enfeite
    'hemp':     (0x9C8A5A, (0xC8A46A), (0x8A6A3A), (0x5A4426)),
    'initiate': (0x2E4F9C, (0xF0C850), (0xFFF0A0), (0x9A7420)),
    'adept':    (0x6A2E8C, (0xD8DCEC), (0xC07CFF), (0x7A7E96)),
    'archmage': (0x2A1C48, (0xFFB84A), (0xFFF4C8), (0xA8642A)),
}


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def shade(c, k):
    return tuple(max(0, min(255, int(x * k))) for x in c)


# ------------------------------------------------------------------ faces de uma caixa no layout do Minecraft

def regions(u, v, w, h, d):
    return {
        'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
        'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h),
    }


class Face:
    """Desenho numa face: coordenadas locais (0..w-1, 0..h-1)."""

    def __init__(self, base, trim, rect, name, rng):
        self.base, self.trim = base, trim
        self.x0, self.y0, self.w, self.h = rect
        self.name = name
        self.rng = rng

    def b(self, x, y, g):
        if 0 <= x < self.w and 0 <= y < self.h:
            g = max(0, min(255, int(g)))
            self.base.putpixel((self.x0 + x, self.y0 + y), (g, g, g, 255))

    def t(self, x, y, c, a=255):
        if 0 <= x < self.w and 0 <= y < self.h and self.trim is not None:
            self.trim.putpixel((self.x0 + x, self.y0 + y), tuple(c) + (a,))

    def clear(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.base.putpixel((self.x0 + x, self.y0 + y), (0, 0, 0, 0))

    def cloth(self, top=232, bottom=196, folds=0):
        """Tecido: luz de cima, borda escura, trama leve e dobras verticais."""
        k = {'top': 1.06, 'bottom': 0.7, 'front': 1.0, 'back': 0.9, 'left': 0.93, 'right': 0.93}[self.name]
        for y in range(self.h):
            for x in range(self.w):
                g = top + (bottom - top) * (y / max(1, self.h - 1))
                if (x + y) % 2 == 0:
                    g *= 1.025
                if folds and x % folds == folds - 1 and self.name in ('front', 'back', 'left', 'right'):
                    g *= 0.88
                if self.w > 2 and (x == 0 or x == self.w - 1):
                    g *= 0.92
                self.b(x, y, g * k)


def blank(w, h):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


# ================================================================== 1) armadura (64x32)

def armor_textures(tier):
    main, gem, dark = [rgb(c) for c in TIERS[tier][1:]]
    rng = random.Random(3)
    base, trim = blank(64, 32), blank(64, 32)
    # --- tronco (16,16; 8x12x4) ---
    for name, rect in regions(16, 16, 8, 12, 4).items():
        f = Face(base, trim, rect, name, rng)
        f.cloth(folds=0)
        if name == 'front':
            # gola em V (forro escuro) e abertura central com botões
            for y in range(3):
                for x in range(3 - y, 5 + y):
                    f.b(x, y, 120)
            for y in range(3):
                f.t(2 - y + 0, y, main)
                f.t(5 + y, y, main)
            for y in range(3, 12):
                f.t(3, y, dark) if tier == 'hemp' else f.t(3, y, main)
                f.t(4, y, dark) if tier == 'hemp' else f.t(4, y, main)
            # faixa na cintura
            for x in range(8):
                f.t(x, 8, dark)
                f.t(x, 9, main)
            f.t(3, 9, gem)
            f.t(4, 9, gem)

        elif name == 'back':
            for x in range(8):
                f.t(x, 8, dark)
                f.t(x, 9, main)
            if tier == 'archmage':
                # estrela grande nas costas
                for (x, y) in ((3, 2), (4, 2), (2, 3), (3, 3), (4, 3), (5, 3), (3, 4), (4, 4), (3, 1), (4, 5)):
                    f.t(x, y, gem)
            elif tier == 'adept':
                for (x, y) in ((3, 2), (4, 3), (3, 4), (4, 5), (3, 6)):  # runa
                    f.t(x, y, gem)
        elif name in ('left', 'right'):
            for x in range(4):
                f.t(x, 8, dark)
                f.t(x, 9, main)
        elif name == 'top':
            for x in range(8):
                f.t(x, 0, main)
                f.t(x, 3, main)
    # --- braços (40,16; 4x12x4) ---
    for name, rect in regions(40, 16, 4, 12, 4).items():
        f = Face(base, trim, rect, name, rng)
        f.cloth()
        if name in ('front', 'back', 'left', 'right'):
            for x in range(4):
                f.t(x, 0, dark)        # costura do ombro
                f.t(x, 9, main)        # punho
                f.t(x, 11, main)

    # --- sapatos (perna 0,16; 4x12x4): só a parte de baixo ---
    for name, rect in regions(0, 16, 4, 12, 4).items():
        f = Face(base, trim, rect, name, rng)
        if name in ('front', 'back', 'left', 'right'):
            for y in range(12):
                for x in range(4):
                    if y < 8:
                        f.clear(x, y)
                    else:
                        g = 150 - (y - 8) * 10
                        f.b(x, y, g * (1.0 if name == 'front' else 0.9))
            for x in range(4):
                f.t(x, 8, main)
            if name == 'front':
                f.t(1, 10, gem); f.t(2, 10, gem)
        elif name == 'bottom':
            f.cloth(120, 110)
    # cabeça: o capuz é o chapéu 3D (nada na armadura)
    base.save(T + 'entity/equipment/humanoid/mage_robe_base.png')
    trim.save(T + 'entity/equipment/humanoid/mage_%s_trim.png' % tier)

    # --- calças (humanoid_leggings): pernas + cós ---
    lb, lt = blank(64, 32), blank(64, 32)
    for name, rect in regions(0, 16, 4, 12, 4).items():
        f = Face(lb, lt, rect, name, rng)
        f.cloth(205, 170)
        if name in ('front', 'back', 'left', 'right'):
            for x in range(4):
                f.t(x, 11, main)
    for name, rect in regions(16, 16, 8, 12, 4).items():
        f = Face(lb, lt, rect, name, rng)
        if name in ('front', 'back', 'left', 'right'):
            for y in range(12):
                for x in range(f.w):
                    if y < 8:
                        f.clear(x, y)
                    else:
                        f.b(x, y, 190 * (1.0 if name == 'front' else 0.9))
            for x in range(f.w):
                f.t(x, 8, dark)
    lb.save(T + 'entity/equipment/humanoid_leggings/mage_pants_base.png')
    lt.save(T + 'entity/equipment/humanoid_leggings/mage_%s_trim.png' % tier)
    return base, trim, lb, lt


# ================================================================== 2) partes 3D

class Box:
    def __init__(self, name, origin, size, inflate=0.0):
        self.name = name
        self.origin = origin
        self.size = size
        self.inflate = inflate
        self.uv = None
        self.mirror = False


class Part:
    def __init__(self, name, parent, pivot, rot, attach):
        self.name, self.parent, self.pivot, self.rot, self.attach = name, parent, pivot, rot, attach
        self.boxes = []


PARTS = []


def part(name, attach, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0)):
    p = Part(name, parent, pivot, rot, attach)
    PARTS.append(p)
    return p


def box(p, name, origin, size, inflate=0.0):
    b = Box(name, origin, size, inflate)
    p.boxes.append(b)
    return b


# --- chapéu de mago (preso na cabeça) ---
hat = part('hat', 'head')
box(hat, 'brim', (-6.5, -9.0, -6.5), (13, 1, 13))
box(hat, 'band', (-4.5, -11.5, -4.5), (9, 3, 9))
c1 = part('hat_c1', 'head', hat, (0, -11.5, 0.3), (-0.10, 0, 0))
box(c1, 'c1', (-3.5, -3, -3.5), (7, 3, 7))
c2 = part('hat_c2', 'head', c1, (0, -3, 0.3), (-0.16, 0, 0))
box(c2, 'c2', (-2.5, -3, -2.5), (5, 3, 5))
c3 = part('hat_c3', 'head', c2, (0, -3, 0.3), (-0.28, 0, 0))
box(c3, 'c3', (-2, -3, -2), (4, 3, 4))
c4 = part('hat_c4', 'head', c3, (0, -3, 0.3), (-0.45, 0, 0))
box(c4, 'c4', (-1.5, -3, -1.5), (3, 3, 3))
c5 = part('hat_c5', 'head', c4, (0, -3, 0.2), (-0.6, 0, 0))
box(c5, 'c5', (-1, -2, -1), (2, 2, 2))
c6 = part('hat_c6', 'head', c5, (0, -2, 0), (-0.5, 0, 0))
box(c6, 'tip', (-0.5, -2, -0.5), (1, 2, 1))

# --- gola e saia (presas no tronco) ---
robe = part('robe', 'body')
box(robe, 'collar', (-5.5, -2.0, -3.5), (11, 2, 7), 0.0)
box(robe, 'sash_knot', (-1, 8.5, -4.0), (2, 3, 1))
box(robe, 'skirt_back', (-5, 12, 2.4), (10, 5, 1), 0.3)
box(robe, 'skirt_front', (-5, 12, -3.4), (10, 5, 1), 0.3)
box(robe, 'skirt_side_r', (-5.6, 12, -3.0), (1, 5, 6), 0.3)
box(robe, 'skirt_side_l', (4.6, 12, -3.0), (1, 5, 6), 0.3)
box(robe, 'hem_back', (-5.5, 17, 2.9), (11, 6, 1), 0.3)
box(robe, 'hem_front', (-5.5, 17, -3.9), (11, 6, 1), 0.3)
box(robe, 'hem_side_r', (-6.1, 17, -3.5), (1, 6, 7), 0.3)
box(robe, 'hem_side_l', (5.1, 17, -3.5), (1, 6, 7), 0.3)

# --- punhos largos (presos nos braços; por fora da manga da armadura) ---
cuff_r = part('cuff_r', 'right_arm')
box(cuff_r, 'cuff_r', (-4.5, 7, -3.5), (7, 3, 7), 0.0)
cuff_l = part('cuff_l', 'left_arm')
box(cuff_l, 'cuff_l', (-2.5, 7, -3.5), (7, 3, 7), 0.0)

# --- bico dos sapatos (preso nas pernas) ---
shoe_r = part('shoe_r', 'right_leg')
box(shoe_r, 'toe_r', (-1.5, 10.5, -5.0), (3, 2, 2))
box(shoe_r, 'curl_r', (-0.5, 9.0, -6.0), (1, 2, 1))
shoe_l = part('shoe_l', 'left_leg')
box(shoe_l, 'toe_l', (-1.5, 10.5, -5.0), (3, 2, 2))
box(shoe_l, 'curl_l', (-0.5, 9.0, -6.0), (1, 2, 1))


def pack(tex_w=64):
    boxes = [b for p in PARTS for b in p.boxes]
    order = sorted(boxes, key=lambda b: (-(b.size[1] + b.size[2]), -(2 * (b.size[0] + b.size[2]))))
    x = y = shelf = 0
    for b in order:
        w, h, d = [int(round(s)) for s in b.size]
        uw, uh = 2 * (w + d), d + h
        if x + uw > tex_w:
            x, y, shelf = 0, y + shelf, 0
        b.uv = (x, y)
        x += uw
        shelf = max(shelf, uh)
    th = 16
    while th < y + shelf:
        th *= 2
    return tex_w, th


TEX_W, TEX_H = pack()


def wear_textures(tier):
    main, gem, dark = [rgb(c) for c in TIERS[tier][1:]]
    rng = random.Random(5)
    base, trim = blank(TEX_W, TEX_H), blank(TEX_W, TEX_H)
    for p in PARTS:
        for b in p.boxes:
            w, h, d = [int(round(s)) for s in b.size]
            for name, rect in regions(b.uv[0], b.uv[1], w, h, d).items():
                f = Face(base, trim, rect, name, rng)
                n = b.name
                if n.startswith(('skirt', 'hem')):
                    f.cloth(222, 190, folds=3 if name == 'back' else 0)
                elif n in ('brim',):
                    f.cloth(200, 200)
                elif n.startswith(('toe', 'curl')):
                    f.cloth(150, 130)
                else:
                    f.cloth()
                side = name in ('front', 'back', 'left', 'right')
                # ---- enfeites ----
                if n == 'band' and side:
                    for x in range(f.w):
                        f.t(x, 0, dark); f.t(x, 1, main); f.t(x, 2, dark)
                    if name == 'front':
                        mid = f.w // 2
                        if tier == 'hemp':
                            f.t(mid, 1, gem)
                        elif tier == 'initiate':
                            for (x, y) in ((0, 0), (-1, 1), (0, 1), (1, 1), (0, 2)):
                                f.t(mid + x, y, gem)  # estrelinha
                        elif tier == 'adept':
                            for (x, y) in ((0, 0), (-1, 1), (-1, 2), (0, 2)):
                                f.t(mid + x, y, gem)  # lua crescente
                        else:
                            for (x, y) in ((0, 0), (-1, 1), (0, 1), (1, 1), (0, 2), (-2, 1), (2, 1)):
                                f.t(mid + x, y, gem)
                elif n == 'brim':
                    if side:
                        for x in range(f.w):
                            f.t(x, 0, main if tier != 'hemp' else dark)
                    elif name == 'top':
                        for x in range(f.w):
                            f.t(x, 0, main); f.t(x, f.h - 1, main)
                        for y in range(f.h):
                            f.t(0, y, main); f.t(f.w - 1, y, main)
                elif n in ('c1', 'c2', 'c3', 'c4', 'c5', 'tip') and side:
                    if tier == 'archmage' and n in ('c1', 'c3') and name in ('front', 'left'):
                        f.t(f.w // 2, 1, gem)
                    if tier == 'adept' and n == 'c2' and name == 'front':
                        f.t(f.w // 2, 1, gem)
                    if n == 'tip' and tier != 'hemp':
                        for y in range(f.h):
                            f.t(0, y, main)
                elif n == 'collar':
                    if side:
                        for x in range(f.w):
                            f.t(x, 0, main)
                    elif name == 'top':
                        for x in range(f.w):
                            f.t(x, 0, main); f.t(x, f.h - 1, main)
                elif n == 'sash_knot' and side:
                    for y in range(f.h):
                        for x in range(f.w):
                            f.t(x, y, main if (y + x) % 3 else dark)
                    if name == 'front':
                        f.t(0, 0, gem)
                elif n.startswith('hem') and side:
                    for x in range(f.w):
                        f.t(x, f.h - 1, main)
                        f.t(x, f.h - 2, dark)
                    if name == 'front' and n == 'hem_front' and tier != 'hemp':
                        mid = f.w // 2
                        for y in range(f.h - 2):
                            f.t(mid - 1, y, main); f.t(mid, y, main)
                    if tier == 'adept' and name in ('front', 'back') and f.w > 3:
                        for x in (1, f.w - 2):
                            f.t(x, f.h - 4, gem)
                    if tier == 'archmage' and name in ('front', 'back') and f.w > 3:
                        for x in range(1, f.w, 3):
                            if name == 'back' or abs(x - f.w // 2) > 1:
                                f.t(x, f.h - 4, gem)
                elif n.startswith('skirt') and side:
                    if name == 'front' and n == 'skirt_front' and tier != 'hemp':
                        mid = f.w // 2
                        for y in range(f.h):
                            f.t(mid - 1, y, main); f.t(mid, y, main)
                    if name == 'front' and n == 'skirt_front' and tier == 'hemp':
                        for y in range(0, f.h, 2):
                            f.t(f.w // 2, y, dark)
                elif n.startswith('cuff') and side:
                    for x in range(f.w):
                        f.t(x, f.h - 1, main)
                        f.t(x, f.h - 2, dark)
                    if tier in ('adept', 'archmage') and name == 'front':
                        f.t(f.w // 2, 0, gem)
                elif n.startswith('curl') and tier != 'hemp':
                    for y in range(f.h):
                        for x in range(f.w):
                            f.t(x, y, main)
    base.save(T + 'entity/mage_wear_base.png')
    trim.save(T + 'entity/mage_wear_%s_trim.png' % tier)
    return base, trim


# ================================================================== 3) Java

def f_(x):
    return '%sF' % float(x)


def java():
    var = {}
    lines = []
    for p in PARTS:
        parent = 'root' if p.parent is None else var[p.parent.name]
        v = 'p_' + p.name
        var[p.name] = v
        cubes = 'CubeListBuilder.create()'
        for b in p.boxes:
            ox, oy, oz = b.origin
            w, h, d = b.size
            cubes += '\n                .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s, new CubeDeformation(%s))' % (
                b.uv[0], b.uv[1], f_(ox), f_(oy), f_(oz), f_(w), f_(h), f_(d), f_(b.inflate))
        px, py, pz = p.pivot
        rx, ry, rz = p.rot
        pose = 'PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)' % tuple(f_(c) for c in (px, py, pz, rx, ry, rz))
        lines.append('        PartDefinition %s = %s.addOrReplaceChild("%s", %s, %s);' % (v, parent, p.name, cubes, pose))
    return ('    public static LayerDefinition createLayer() {\n'
            '        MeshDefinition mesh = new MeshDefinition();\n'
            '        PartDefinition root = mesh.getRoot();\n' + '\n'.join(lines) + '\n'
            '        return LayerDefinition.create(mesh, %d, %d);\n    }\n' % (TEX_W, TEX_H))


# ================================================================== 4) preview

def tint(img, color):
    c = rgb(color)
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            p = px[x, y]
            if p[3]:
                px[x, y] = (p[0] * c[0] // 255, p[1] * c[1] // 255, p[2] * c[2] // 255, p[3])
    return out


class PBox:
    def __init__(self, origin, size, uv, tex, inflate=0.0):
        g = inflate
        self.origin = (origin[0] - g, origin[1] - g, origin[2] - g)
        self.size = (size[0] + 2 * g, size[1] + 2 * g, size[2] + 2 * g)
        self.uv = uv
        self.mirror = False
        self.tex = tex
        self.isize = size


def pbox_quads(b, M, Tr):
    # usa o tamanho em pixels da textura (sem o inflate) para a UV
    fake = type('B', (), {})()
    fake.origin, fake.size, fake.uv, fake.mirror = b.origin, b.size, b.uv, False
    w, h, d = b.isize
    x0, y0, z0 = b.origin
    W, H, D = b.size
    x1, y1, z1 = x0 + W, y0 + H, z0 + D
    u, v = b.uv
    faces = [((u + d, v + d, w, h), lambda s, t: (x0 + s * W, y0 + t * H, z0)),
             ((u + 2 * d + w, v + d, w, h), lambda s, t: (x1 - s * W, y0 + t * H, z1)),
             ((u, v + d, d, h), lambda s, t: (x0, y0 + t * H, z1 - s * D)),
             ((u + d + w, v + d, d, h), lambda s, t: (x1, y0 + t * H, z0 + s * D)),
             ((u + d, v, w, d), lambda s, t: (x0 + s * W, y0, z1 - t * D)),
             ((u + d + w, v, w, d), lambda s, t: (x0 + s * W, y1, z1 - t * D))]
    out = []
    for (fu, fv, fw, fh), fn in faces:
        for j in range(int(fh)):
            for i in range(int(fw)):
                c = b.tex.getpixel((int(fu + i), int(fv + j)))
                if c[3] == 0:
                    continue
                pts = [fn(i / fw, j / fh), fn((i + 1) / fw, j / fh), fn((i + 1) / fw, (j + 1) / fh), fn(i / fw, (j + 1) / fh)]
                pts = [[Tr[k] + pv.mat_vec(M, p)[k] for k in range(3)] for p in pts]
                out.append((pts, c))
    return out


def preview(tier, armor_tex, legs_tex, wear_tex):
    """Steve parado com a roupa toda (armadura inflada + partes 3D)."""
    skin_path = VAN + 'entity/player/wide/steve.png'
    skin = Image.open(skin_path).convert('RGBA') if os.path.exists(skin_path) else Image.new('RGBA', (64, 64), (200, 160, 130, 255))
    I = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    quads = []
    # corpo do jogador (pivôs do PlayerModel)
    limbs = {'head': ((0, 0, 0), (-4, -8, -4), (8, 8, 8), (0, 0)),
             'body': ((0, 0, 0), (-4, 0, -2), (8, 12, 4), (16, 16)),
             'right_arm': ((-5, 2, 0), (-3, -2, -2), (4, 12, 4), (40, 16)),
             'left_arm': ((5, 2, 0), (-1, -2, -2), (4, 12, 4), (32, 48)),
             'right_leg': ((-1.9, 12, 0), (-2, 0, -2), (4, 12, 4), (0, 16)),
             'left_leg': ((1.9, 12, 0), (-2, 0, -2), (4, 12, 4), (16, 48))}
    armor_uv = {'head': (0, 0), 'body': (16, 16), 'right_arm': (40, 16), 'left_arm': (40, 16), 'right_leg': (0, 16), 'left_leg': (0, 16)}
    for name, (pivot, origin, size, uv) in limbs.items():
        quads += pbox_quads(PBox(origin, size, uv, skin), I, pivot)
        if name in ('right_leg', 'left_leg'):
            quads += pbox_quads(PBox(origin, size, armor_uv[name], legs_tex, 0.5), I, pivot)
        if name != 'head':
            quads += pbox_quads(PBox(origin, size, armor_uv[name], armor_tex, 1.0), I, pivot)
        if name == 'body':
            quads += pbox_quads(PBox(origin, size, (16, 16), legs_tex, 0.5), I, pivot)
    attach_pivot = {k: v[0] for k, v in limbs.items()}
    for p in PARTS:
        M, Tr = pv.world_transform(p, {})
        base = attach_pivot[p.attach]
        Tr = [Tr[i] + base[i] for i in range(3)]
        for b in p.boxes:
            quads += pbox_quads(PBox(b.origin, b.size, b.uv, wear_tex, b.inflate), M, Tr)
    views = [pv.draw(quads, yaw, 15, 9) for yaw in (-30, 30, 160)]
    W = sum(v.width for v in views)
    H = max(v.height for v in views)
    sheet = Image.new('RGBA', (W, H), (70, 74, 82, 255))
    x = 0
    for v in views:
        sheet.alpha_composite(v, (x, H - v.height))
        x += v.width
    return sheet


if __name__ == '__main__':
    sheets = []
    for tier, (color, *_rest) in TIERS.items():
        base, trim, lb, lt = armor_textures(tier)
        wb, wt = wear_textures(tier)
        armor = tint(base, color); armor.alpha_composite(trim)
        legs = tint(lb, color); legs.alpha_composite(lt)
        wear = tint(wb, color); wear.alpha_composite(wt)
        sheets.append(preview(tier, armor, legs, wear))
    W = max(s.width for s in sheets)
    H = sum(s.height for s in sheets)
    out = Image.new('RGBA', (W, H), (70, 74, 82, 255))
    y = 0
    for s in sheets:
        out.alpha_composite(s, (0, y))
        y += s.height
    out.save(OUT_BUILD + 'mage_robes_preview.png')
    open(OUT_BUILD + 'mage_wear_layer.txt', 'w').write(java())
    print('ok', TEX_W, TEX_H)


# ------------------------------------------------------------------ preview com z-buffer (mais fiel que o do modelgen)

def zdraw(quads, yaw, pitch, scale):
    import numpy as np
    ya, pa = math.radians(yaw), math.radians(pitch)
    proj = []
    for pts, c in quads:
        sp = []
        for x, y, z in pts:
            X, Y, Z = -x, -y, z
            xr = X * math.cos(ya) - Z * math.sin(ya)
            zr = X * math.sin(ya) + Z * math.cos(ya)
            yr = Y * math.cos(pa) - zr * math.sin(pa)
            zr2 = Y * math.sin(pa) + zr * math.cos(pa)
            sp.append((xr, yr, zr2))
        proj.append((sp, c))
    xs = [p[0] for q in proj for p in q[0]]
    ys = [p[1] for q in proj for p in q[0]]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    W = int((maxx - minx) * scale) + 20
    H = int((maxy - miny) * scale) + 20
    zbuf = np.full((H, W), np.inf)
    col = np.zeros((H, W, 4), dtype=np.uint8)
    col[..., :3] = (70, 74, 82)
    col[..., 3] = 255
    for sp, c in proj:
        P = [((x - minx) * scale + 10, (maxy - y) * scale + 10, z) for x, y, z in sp]
        for tri in ((P[0], P[1], P[2]), (P[0], P[2], P[3])):
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = tri
            bx0, bx1 = int(max(0, min(x0, x1, x2))), int(min(W - 1, max(x0, x1, x2)) + 1)
            by0, by1 = int(max(0, min(y0, y1, y2))), int(min(H - 1, max(y0, y1, y2)) + 1)
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-9:
                continue
            gx, gy = np.meshgrid(np.arange(bx0, bx1) + 0.5, np.arange(by0, by1) + 0.5)
            a = ((y1 - y2) * (gx - x2) + (x2 - x1) * (gy - y2)) / den
            b = ((y2 - y0) * (gx - x2) + (x0 - x2) * (gy - y2)) / den
            g = 1 - a - b
            inside = (a >= -1e-6) & (b >= -1e-6) & (g >= -1e-6)
            z = a * z0 + b * z1 + g * z2
            sub = zbuf[by0:by1, bx0:bx1]
            m = inside & (z < sub)
            sub[m] = z[m]
            col[by0:by1, bx0:bx1][m] = (c[0], c[1], c[2], 255)
    return Image.fromarray(col, 'RGBA')


def preview_z(tier, armor_tex, legs_tex, wear_tex, yaws=(-30, 30, 160)):
    global_draw = pv.draw
    pv.draw = zdraw
    try:
        return preview(tier, armor_tex, legs_tex, wear_tex)
    finally:
        pv.draw = global_draw


if __name__ == '__main__':
    sheets = []
    for tier, (color, *_rest) in TIERS.items():
        base, trim, lb, lt = armor_textures(tier)
        wb, wt = wear_textures(tier)
        armor = tint(base, color); armor.alpha_composite(trim)
        legs = tint(lb, color); legs.alpha_composite(lt)
        wear = tint(wb, color); wear.alpha_composite(wt)
        sheets.append(preview_z(tier, armor, legs, wear))
    W = sum(s.width for s in sheets)
    H = max(s.height for s in sheets)
    out = Image.new('RGBA', (W, H), (70, 74, 82, 255))
    x = 0
    for s in sheets:
        out.alpha_composite(s, (x, 0))
        x += s.width
    out.save(OUT_BUILD + 'mage_robes_preview.png')


# ================================================================== 5) ícones (16x16): base cinza (tingida) + enfeite do nível

ICONS = {
    'hood': [
        '................',
        '..........HH....',
        '.........HHH....',
        '........HHH.....',
        '.......HHHH.....',
        '.......HHHH.....',
        '......HHHHH.....',
        '......HHHHHH....',
        '.....HHHHHHH....',
        '.....HHHHHHH....',
        '....TTTTSTTTT...',
        '..BBBBBBBBBBBB..',
        '.BBBBBBBBBBBBBB.',
        '..DDDDDDDDDDDD..',
        '................',
        '................'],
    'robe': [
        '................',
        '....CCC..CCC....',
        '...CCCCVVCCCC...',
        '..AACCCVVCCCAA..',
        '..AACCCLLCCCAA..',
        '..AACCCLLCCCAA..',
        '..AACCCLLCCCAA..',
        '..TT.TTTSTTT.TT.',
        '.....CCLLCC.....',
        '....CCCLLCCC....',
        '....CCCLLCCC....',
        '...CCCCLLCCCC...',
        '...CCCCLLCCCC...',
        '..CCCCCLLCCCCC..',
        '..TTTTTTTTTTTT..',
        '................'],
    'pants': [
        '................',
        '....TTTTTTTT....',
        '....CCCCCCCC....',
        '....CCCCCCCC....',
        '....CCC..CCC....',
        '....CCC..CCC....',
        '....CCC..CCC....',
        '....CCC..CCC....',
        '....CCC..CCC....',
        '...CCCC..CCCC...',
        '...CCCC..CCCC...',
        '...CCCC..CCCC...',
        '...TTTT..TTTT...',
        '................',
        '................',
        '................'],
    'shoes': [
        '................',
        '................',
        '................',
        '................',
        '.............S..',
        '..CCC.......CS..',
        '..CCC.......C...',
        '..CCC......CC...',
        '.CCCCC....CCC...',
        '.CCCCCCCCCCC....',
        '.CCCCCCCCCC.....',
        '.TTTTTTTTT......',
        '................',
        '................',
        '................',
        '................'],
}
GRAY = {'H': 215, 'C': 210, 'A': 190, 'B': 175, 'D': 120, 'V': 110}


def icons():
    for piece, art in ICONS.items():
        base = blank(16, 16)
        for y, row in enumerate(art):
            for x, ch in enumerate(row):
                if ch in GRAY or ch in 'TSL':
                    g = GRAY.get(ch, 200)
                    g = int(g * (1.06 - 0.2 * y / 15) * (0.88 if x > 0 and row[x - 1] == '.' else 1.0))
                    base.putpixel((x, y), (g, g, g, 255))
        # contorno escuro
        out = base.copy()
        for y in range(16):
            for x in range(16):
                if base.getpixel((x, y))[3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and base.getpixel((x + dx, y + dy))[3]
                                                         for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    out.putpixel((x, y), (40, 40, 40, 255))
        out.save(T + 'item/mage_%s_base.png' % piece)
        for tier in TIERS:
            main, gem, dark = [rgb(c) for c in TIERS[tier][1:]]
            trim = blank(16, 16)
            for y, row in enumerate(art):
                for x, ch in enumerate(row):
                    if ch == 'T':
                        trim.putpixel((x, y), main + (255,))
                    elif ch == 'S':
                        trim.putpixel((x, y), (gem if tier != 'hemp' else dark) + (255,))
                    elif ch == 'L' and tier != 'hemp':
                        trim.putpixel((x, y), main + (255,))
            if tier == 'archmage' and piece in ('hood', 'robe'):
                for (x, y) in ((8, 5), (10, 8)) if piece == 'hood' else ((4, 10), (11, 12), (5, 3)):
                    trim.putpixel((x, y), gem + (255,))
            trim.save(T + 'item/mage_%s_%s_trim.png' % (tier, piece))


if __name__ == '__main__':
    icons()
    sheet = Image.new('RGBA', (16 * 4 * 8 + 40, 16 * 8 * 4 + 40), (70, 74, 82, 255))
    for i, tier in enumerate(TIERS):
        for j, piece in enumerate(ICONS):
            b = tint(Image.open(T + 'item/mage_%s_base.png' % piece).convert('RGBA'), TIERS[tier][0])
            b.alpha_composite(Image.open(T + 'item/mage_%s_%s_trim.png' % (tier, piece)).convert('RGBA'))
            sheet.alpha_composite(b.resize((128, 128), Image.NEAREST), (j * 138, i * 138))
    sheet.save(OUT_BUILD + 'mage_icons_preview.png')
