"""Pintura v2 do folclore: luz por face sem ruído, materiais com rampa de cor e arte em grade (ASCII).

LModel = Model do modelgen, mas cada função de face recebe um LPainter que já aplica a luz da face
(topo claro, laterais escurecendo para baixo, trás mais escura). As cores passadas são "sem luz".
"""
import random
import colorsys
from modelgen import Model, hexc, clamp

LIGHT = {'top': 1.08, 'bottom': 0.70, 'right': 0.90, 'front': 1.0, 'left': 0.90, 'back': 0.84}


def rgb(c):
    return hexc(c) if isinstance(c, int) else tuple(c[:3])


def mul(c, k):
    c = rgb(c)
    return tuple(clamp(v * k) for v in c)


def ramp(base, n=5, dark=0.5, light=1.35, hue=0.03):
    """Rampa de 5 tons com deslocamento de matiz: sombras mais frias/saturadas, luzes mais quentes."""
    r, g, b = [v / 255 for v in rgb(base)]
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    out = []
    for i in range(n):
        t = i / (n - 1) * 2 - 1  # -1..1
        k = 1 + (t * (light - 1) if t > 0 else t * (1 - dark))
        hh = (h - hue * t) % 1.0 if t < 0 else (h - hue * t * 0.6) % 1.0
        ss = min(1, max(0, s * (1 - 0.25 * t)))
        vv = min(1, v * k)
        out.append(tuple(int(x * 255) for x in colorsys.hsv_to_rgb(hh, ss, vv)))
    return out


class LPainter:
    def __init__(self, img, glow, x0, y0, w, h, rng, base, face):
        self.img, self.glow = img, glow
        self.x0, self.y0, self.w, self.h = x0, y0, w, h
        self.rng = rng
        self.base = base
        self.face = face
        self.k = LIGHT[face]
        self.side = face not in ('top', 'bottom')

    def light(self, y):
        if self.side and self.h > 1:
            return self.k * (1.05 - 0.15 * y / (self.h - 1))
        return self.k

    def px(self, x, y, color, glow=False, lit=True):
        if 0 <= x < self.w and 0 <= y < self.h:
            c = rgb(color)
            if lit and not glow:
                c = mul(c, self.light(y))
            self.img.putpixel((self.x0 + x, self.y0 + y), c + (255,))
            if glow and self.glow is not None:
                self.glow.putpixel((self.x0 + x, self.y0 + y), c + (255,))

    def get(self, x, y):
        return self.img.getpixel((self.x0 + x, self.y0 + y))[:3]

    def rect(self, x, y, w, h, color, glow=False):
        for yy in range(h):
            for xx in range(w):
                self.px(x + xx, y + yy, color, glow)

    def fill(self, color, noise=0.0):
        for yy in range(self.h):
            for xx in range(self.w):
                k = 1 + (self.rng.uniform(-noise, noise) if noise else 0)
                self.px(xx, yy, mul(color, k))

    def row(self, y, color, glow=False):
        self.rect(0, y, self.w, 1, color, glow)

    def col(self, x, color, glow=False):
        self.rect(x, 0, 1, self.h, color, glow)

    def art(self, lines, pal, ox=0, oy=0, center=False, glowkeys=''):
        """Desenha uma grade ASCII. '.' ou ' ' = mantém. pal: char -> cor."""
        if center:
            ox = (self.w - max(len(l) for l in lines)) // 2 + ox
        for j, line in enumerate(lines):
            for i, ch in enumerate(line):
                if ch in '. ':
                    continue
                self.px(ox + i, oy + j, pal[ch], glow=ch in glowkeys)


class LModel(Model):
    def _paint_box(self, img, glow, b, rng):
        w, h, d = [int(round(s)) for s in b.size]
        u, v = b.uv
        regions = {
            'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
            'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h),
        }
        for face, (x0, y0, fw, fh) in regions.items():
            if fw <= 0 or fh <= 0:
                continue
            p = LPainter(img, glow, x0, y0, fw, fh, rng, b.color, face)
            fn = b.faces.get(face, b.faces.get('*'))
            if fn is None:
                fn = solid(b.color)
            fn(p)
            if b.glow and glow is not None:
                g = glow.load()
                pi = img.load()
                for yy in range(fh):
                    for xx in range(fw):
                        g[x0 + xx, y0 + yy] = pi[x0 + xx, y0 + yy]


# ------------------------------------------------------------------ materiais
def solid(c, edge=True):
    def f(p):
        p.fill(c)
        if edge and p.side and p.h > 2:
            for x in range(p.w):
                p.px(x, p.h - 1, mul(c, 0.86))
    return f


def soft(R, edge=True):
    """Cor lisa com luz no alto (R = rampa de 5)."""
    def f(p):
        for y in range(p.h):
            for x in range(p.w):
                c = R[2]
                if p.face == 'top':
                    c = R[3]
                p.px(x, y, c)
        if p.side and p.h > 3:
            p.row(0, R[3] if p.face != 'back' else R[2])
            if edge:
                p.row(p.h - 1, R[1])
    return f


def fur(R, seed=0, lock=2, period=6, tips=True):
    """Pelo em mechas: colunas agrupadas em mechas com quebras escuras e brilho."""
    def f(p):
        rnd = random.Random(seed * 997 + p.w * 31 + p.h * 7 + hash(p.face) % 100)
        phase = []
        for x in range(0, p.w + lock, lock):
            phase += [rnd.randint(0, period - 1)] * lock
        for y in range(p.h):
            for x in range(p.w):
                t = (y + phase[x]) % period
                c = R[2]
                if t == 0:
                    c = R[1]
                elif t == period // 2 and x % lock == 0:
                    c = R[3]
                if p.face == 'top':
                    c = R[3] if (x + 2 * y + phase[x]) % 7 == 0 else R[2]
                p.px(x, y, c)
        if tips and p.side and p.h > 2:
            for x in range(p.w):
                p.px(x, p.h - 1, R[1] if (x + phase[x]) % 3 else R[0])
    return f


def feathers(R, fw=4, fh=3, outline=None):
    """Penas em escama, discretas: só a ponta de cada pena escurece; top/bottom lisos."""
    def f(p):
        if p.face in ('top', 'bottom'):
            p.fill(R[2] if p.face == 'top' else R[1])
            return
        for y in range(p.h):
            r = y // fh
            ly = y % fh
            for x in range(p.w):
                lx = (x + (r % 2) * (fw // 2)) % fw
                c = R[2]
                if ly == fh - 1 and 0 < lx < fw - 1:
                    c = outline or R[1]
                elif ly == 0 and lx == 1 and (r + x // fw) % 2 == 0:
                    c = R[3]
                p.px(x, y, c)
    return f


def scales(R, sw=3, sh=2, shine=None):
    """Escamas de peixe arredondadas, com brilho no alto de cada uma."""
    def f(p):
        for y in range(p.h):
            r = y // sh
            ly = y % sh
            for x in range(p.w):
                lx = (x + (r % 2) * (sw // 2 + 1)) % sw
                c = R[2]
                if ly == sh - 1 and lx != 1:
                    c = R[1]
                if ly == sh - 1 and lx == 1:
                    c = R[2]
                if ly == 0 and lx == 1:
                    c = shine or R[3]
                if lx == 0 and ly == 0:
                    c = R[1]
                p.px(x, y, c)
    return f


def scutes(R, sw=3, sh=3, belly=None):
    """Placas de couro de jacaré: quadradinhos com junta escura e brilho no canto."""
    def f(p):
        if p.face == 'bottom' and belly:
            belly(p)
            return
        for y in range(p.h):
            r = y // sh
            for x in range(p.w):
                lx = (x + (r % 2) * (sw // 2)) % sw
                ly = y % sh
                c = R[2]
                if lx == sw - 1 or ly == sh - 1:
                    c = R[1]
                elif lx == 0 and ly == 0:
                    c = R[3]
                p.px(x, y, c)
    return f


def cloth(R, every=4, hem=None, hem2=None, fringe=False):
    """Pano com dobras verticais suaves (vale escuro + luz ao lado) e barra."""
    def f(p):
        for y in range(p.h):
            for x in range(p.w):
                m = x % every
                c = R[2]
                if m == 0:
                    c = R[1]
                elif m == 1:
                    c = R[3] if y < p.h * 0.6 else R[2]
                if p.face == 'top':
                    c = R[3] if m == 1 else R[2]
                if p.face == 'bottom':
                    c = R[1]
                p.px(x, y, c)
        if p.side:
            if hem is not None and p.h > 3:
                p.row(p.h - 2, hem)
                if hem2 is not None:
                    for x in range(0, p.w, 2):
                        p.px(x, p.h - 2, hem2)
            if fringe:
                for x in range(p.w):
                    p.px(x, p.h - 1, R[0] if x % 2 else R[1])
            elif p.h > 2:
                p.row(p.h - 1, R[0])
    return f


def wood(R):
    def f(p):
        for y in range(p.h):
            for x in range(p.w):
                c = R[2] if (x + (y // 4)) % 3 else R[1]
                if p.face in ('top', 'bottom'):
                    c = R[3] if (x + y) % 3 == 0 else R[2]
                p.px(x, y, c)
    return f


def faces(base, **over):
    d = {'*': base}
    d.update(over)
    return d
