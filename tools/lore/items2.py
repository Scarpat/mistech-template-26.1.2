"""Itens do folclore novo v2 (16x16): pena da Matinta, escama e anel da Iara, garra e machado do Mapinguari, ovos.
Uso (raiz do projeto): python tools/lore/items2.py  -> textures/item/*.png e build/lore/items_view.png"""
import math
import colorsys
from PIL import Image

I = 'src/main/resources/assets/mistech/textures/item/'


def hx(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def mix(a, b, t):
    a, b = (hx(a) if isinstance(a, int) else a), (hx(b) if isinstance(b, int) else b)
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def mul(c, k):
    c = hx(c) if isinstance(c, int) else c
    return tuple(max(0, min(255, int(v * k))) for v in c)


class Canvas:
    def __init__(self):
        self.px = {}

    def set(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[(x, y)] = hx(c) if isinstance(c, int) else tuple(c)

    def has(self, x, y):
        return (x, y) in self.px

    def edge_shade(self, k=0.62, light_k=1.0):
        """Escurece os pixels de borda (lado de baixo/direita mais forte), como os itens vanilla."""
        out = dict(self.px)
        for (x, y), c in self.px.items():
            nb = [(x + 1, y), (x, y + 1), (x - 1, y), (x, y - 1)]
            miss = [not self.has(*n) for n in nb]
            if miss[0] or miss[1]:
                out[(x, y)] = mul(c, k)
            elif miss[2] or miss[3]:
                out[(x, y)] = mul(c, 0.8 * light_k)
        self.px = out

    def art(self, lines, pal, ox=0, oy=0):
        for j, l in enumerate(lines):
            for i, ch in enumerate(l):
                if ch not in '. ':
                    self.set(ox + i, oy + j, pal[ch])

    def save(self, name):
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for (x, y), c in self.px.items():
            im.putpixel((x, y), tuple(c) + (255,))
        im.save(I + name + '.png')
        return im


# ------------------------------------------------------------------ pena da Matinta
def feather():
    c = Canvas()
    ax, ay, bx, by = 2.5, 13.5, 13.5, 2.0
    L = math.hypot(bx - ax, by - ay)
    ux, uy = (bx - ax) / L, (by - ay) / L
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - ax, y + 0.5 - ay
            t = (dx * ux + dy * uy) / L
            d = -dx * uy + dy * ux
            if t < -0.02 or t > 1.0:
                continue
            if t < 0.28:
                w = 0.55
            else:
                s = (t - 0.28) / 0.72
                w = 0.8 + 3.2 * math.sin(min(1, s * 1.15) * math.pi) ** 0.8
                if s > 0.85:
                    w = min(w, (1 - s) * 9)
            # entalhe na pena (fio separado)
            if 0.55 < t < 0.6 and d > 0.8:
                continue
            if abs(d) > w:
                continue
            if abs(d) < 0.6 or t < 0.28:
                col = mix(0xd8c8a8, 0x8a7a68, max(0, t - 0.2))  # raque
            else:
                band = int(t * 9 + abs(d) * 0.35) % 2 == 0
                base = 0x3a2c2a if d > 0 else 0x4a3a34
                col = mix(base, 0xa88a68, 0.55) if band else hx(base)
                if abs(d) > w - 1:
                    col = mul(col, 0.8)
            c.set(x, y, col)
    c.edge_shade(0.6)
    # conta vermelha de tento amarrada no cálamo + linha
    c.art(["rR", "Rd"], {'r': 0xff5a40, 'R': 0xc8201a, 'd': 0x7a1010}, ox=3, oy=10)
    c.set(5, 11, 0x1a1414)
    c.set(13, 2, 0x5a4a42)
    return c.save('matinta_feather')


# ------------------------------------------------------------------ escama da Iara
def iara_scale():
    c = Canvas()
    cx, cy, r = 7.5, 8.2, 6.6
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            # leque: redondo embaixo, base reta em cima com encaixe
            dist = math.hypot(dx, dy * 1.05)
            if dist > r or y < 2:
                continue
            ang = math.atan2(dy, dx)
            hue = (0.47 + 0.07 * math.sin(ang * 2 + 0.6)) % 1  # iridescente: verde-água -> azul
            ring = dist / r
            v = 0.62 + 0.3 * (1 - ring) + (0.12 if int(dist * 1.1) % 2 == 0 else 0)
            col = tuple(int(255 * k) for k in colorsys.hsv_to_rgb(hue, 0.55 + 0.25 * ring, min(1, v)))
            if ring > 0.86:
                col = mix(col, 0xf0c860, 0.55)  # borda dourada
            c.set(x, y, col)
    c.edge_shade(0.6)
    # brilho
    c.art([".ww", "wW.", "w.."], {'w': 0xc8fff0, 'W': 0xffffff}, ox=4, oy=4)
    c.set(10, 11, 0xffffff)
    return c.save('iara_scale')


# ------------------------------------------------------------------ anel da Iara
def iara_ring():
    c = Canvas()
    c.art([
        "................",
        "................",
        "......tTTt......",
        ".....tTpPTt.....",
        ".....tpWPpt.....",
        "......tppt......",
        ".....gGtTGg.....",
        "....gG....Gg....",
        "...gG......Gg...",
        "...G........G...",
        "...G........g...",
        "...gG......Gg...",
        "....gG....Gd....",
        ".....dGGGGd.....",
        "................",
        "................",
    ], {'t': 0x1a8a7c, 'T': 0x4ad0b0, 'p': 0xe8e0e8, 'P': 0xd0c8d8, 'W': 0xffffff,
        'g': 0xb8861a, 'G': 0xf0c040, 'd': 0x7a5410})
    return c.save('iara_ring')


# ------------------------------------------------------------------ garra do Mapinguari
def claw_shape(c, ox=0, oy=0, scale=1.0, fur=True):
    # crescente: círculo externo menos interno
    for y in range(16):
        for x in range(16):
            X, Y = (x + 0.5 - ox) / scale, (y + 0.5 - oy) / scale
            o = math.hypot(X - 3, Y - 13)
            i = math.hypot(X - 0.5, Y - 15.5)
            if o <= 11.3 and i >= 9.6 and X >= 2 and Y <= 13.5:
                # posição ao longo da garra (0 base .. 1 ponta)
                t = min(1, max(0, (math.atan2(-(Y - 13), X - 3) - 0.15) / 1.3))
                t = 1 - t
                col = mix(0x2a201c, 0xd8ccae, max(0, t - 0.35) / 0.65)
                if o > 10.4:
                    col = mul(col, 1.25)  # luz na curva de fora
                c.set(x, y, col)
    if fur:
        c.art([
            ".fF.",
            "fFFf",
            "FfFF",
            "fFfd",
            ".dfd",
        ], {'f': 0x7a3a1e, 'F': 0xa85a2e, 'd': 0x4a2010}, ox=1 + ox, oy=11 + oy)


def shade_art(c, shape, cx, cy, ox=0, oy=0, base=0x3a2e28, tip=0xe0d4b6):
    """Desenha um gancho: tom pela letra e cor pelo ângulo (base escura -> ponta clara)."""
    tone = {'L': 1.3, 'M': 1.0, 'D': 0.72, 'd': 0.55, 'k': 0.35}
    for y, l in enumerate(shape):
        for x, ch in enumerate(l):
            if ch in tone:
                ang = math.atan2(-(y - cy), x - cx)
                t = min(1, max(0, (math.pi - ang) / math.pi)) if ang > -1.2 else 1.0
                c.set(x + ox, y + oy, mul(mix(base, tip, t ** 1.4), tone[ch]))


FURPAL = {'F': 0xa85a2e, 'f': 0x6a3018, 'd': 0x4a2010, 'k': 0x2a1a14}


def mapinguari_claw():
    c = Canvas()
    shape = [
        "................",
        "......kkkk......",
        "....kkLLLLkk....",
        "...kLLMMMMLLk...",
        "..kLMMDDDDMMDk..",
        "..kLMDkkkkDMDk..",
        ".kLMDk....kDMdk.",
        ".kLMDk.....kDdk.",
        ".kLMDk......kdk.",
        ".kLMDk.......k..",
        ".kLMDk..........",
        ".kLMDk..........",
    ]
    shade_art(c, shape, 7.5, 8)
    c.art([
        ".FfFfk..",
        "FfFfFfd.",
        "fFfFfd..",
        ".fdfd...",
    ], FURPAL, ox=0, oy=11)
    c.set(13, 9, 0xfff8ea)
    c.set(5, 2, 0xfff0dc)
    return c.save('mapinguari_claw')


def mapinguari_axe():
    c = Canvas()
    shape = [
        "................",
        ".....kkkkk......",
        "...kkLLLLLkk....",
        "..kLLMMMMMMk....",
        ".kLMMDDkkDMk....",
        ".kLMDk...kk.....",
        "kLMDk...........",
        "kLDk............",
        "kDk.............",
        ".k..............",
    ]
    shade_art(c, shape, 8, 6, base=0xe0d4b6, tip=0x3a2e28)
    c.art([
        ".........rRr....",
        "........rRkRr...",
        "........wrRr....",
        ".......wh.......",
        "......wh........",
        ".....wh.........",
        "....rh..........",
        "...wr...........",
        "..wh............",
        ".kh.............",
    ], {'r': 0x7a3a1e, 'R': 0xa85a2e, 'k': 0x3a2414, 'w': 0x8a6038, 'h': 0x5a3a20}, ox=1, oy=4)
    c.set(10, 3, 0x2a201c)
    c.set(11, 3, 0x2a201c)
    return c.save('mapinguari_axe')


# ------------------------------------------------------------------ ovos com rosto (como o do gnomo)
EGG = [
    "......xxxx......",
    ".....xxxxxx.....",
    "....xxxxxxxx....",
    "...xxxxxxxxxx...",
    "...xxxxxxxxxx...",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "...xxxxxxxxxx...",
    "...xxxxxxxxxx...",
    "....xxxxxxxx....",
    "......xxxx......",
    "................",
]


def egg(name, top, mid, bottom, split1=5, split2=11, face=None, pal=None, fy=4):
    c = Canvas()
    for y, l in enumerate(EGG):
        for x, ch in enumerate(l):
            if ch == 'x':
                c.set(x, y, top if y < split1 else (mid if y < split2 else bottom))
    if face:
        c.art(face, pal, ox=2, oy=fy)
    c.edge_shade(0.62)
    # brilho do ovo
    c.set(5, 3, mul(top, 1.5))
    c.set(4, 4, mul(top, 1.3))
    return c.save(name)


def eggs():
    egg('matinta_pereira_spawn_egg', 0x2a2220, 0xc8b496, 0x3a2e2a, split1=4, split2=9,
        face=["..t......t..", "..kkkkkkkk..", ".kYYkddkYYk.", ".kYPkbbkPYk.", "..kdddbddk..", "...rkrkrk..."],
        pal={'t': 0x1a1412, 'k': 0x2a2220, 'Y': 0xffc21a, 'P': 0x1a0804, 'd': 0xc8b496, 'b': 0x2a2420, 'r': 0xc8201a}, fy=2)
    egg('caipora_spawn_egg', 0xe0401a, 0x9a5432, 0x34302e, split1=5, split2=11,
        face=["yo.y..o..y..", "rrrrrrrrrrrr", "rWPrrrrrPWr.", "............", "...m....m...", "....mttm....", "wwwwwwwwwwww"],
        pal={'y': 0xffc030, 'o': 0xff8a20, 'r': 0xd8261a, 'W': 0xf8f4e8, 'P': 0x2a1408, 'm': 0x4a1a10, 't': 0xf8f0e0, 'w': 0x5a5452}, fy=3)
    egg('iara_spawn_egg', 0x1a1620, 0xb07548, 0x1f8a7c, split1=5, split2=10,
        face=["...p..pp....", "hh........hh", "h.WG....GW.h", "h..........h", "h....ll....h", "gggggggggggg", "t.t.t.t.t.t.", ".t.t.t.t.t.t"],
        pal={'p': 0xf6d8e8, 'h': 0x1a1620, 'W': 0xf8f4ec, 'G': 0x2ac89a, 'l': 0xb0303a, 'g': 0xe8b830, 't': 0x5ad0b0}, fy=2)
    egg('cuca_spawn_egg', 0x4e7030, 0x4e7030, 0x40205a, split1=6, split2=10,
        face=["..YY....YY..", "..YP....PY..", "............", "tTtTtTtTtTtT", "kkkkkkkkkkkk", "TtTtTtTtTtTt", "..y......y..", "...y....y..."],
        pal={'Y': 0xffd030, 'P': 0x140a04, 't': 0xf4ecd8, 'T': 0x2e4618, 'k': 0x7a2026, 'y': 0xd8a830}, fy=2)
    egg('mapinguari_spawn_egg', 0x7a3a1e, 0x7a3a1e, 0x5a2a14, split1=6, split2=12,
        face=["...WWWWW....", "...WYYPW....", "...WWWWW....", "............", "..kkkkkkk...", "..tktktkt...", "..kRRRRRk...", "..kkkkkkk..."],
        pal={'W': 0xf0e0b0, 'Y': 0xffb020, 'P': 0x200804, 'k': 0x1a0606, 't': 0xd8ccae, 'R': 0xc8341a}, fy=2)


if __name__ == '__main__':
    ims = [feather(), iara_scale(), iara_ring(), mapinguari_claw(), mapinguari_axe()]
    eggs()
    for n in ('matinta_pereira', 'caipora', 'iara', 'cuca', 'mapinguari'):
        ims.append(Image.open(I + n + '_spawn_egg.png'))
    sheet = Image.new('RGBA', (len(ims) * 140, 140), (60, 60, 70, 255))
    for i, im in enumerate(ims):
        big = im.resize((128, 128), Image.NEAREST)
        sheet.paste(big, (i * 140 + 6, 6), big)
    sheet.save('build/lore/items_view.png')
    print('ok')
