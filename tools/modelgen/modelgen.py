"""Gerador de modelos de entidade por cubos: spec em Python -> código Java (LayerDefinition) + textura PNG pintada.

Cada caixa recebe uma região de UV no layout padrão do Minecraft:
  linha 1 (altura d):  [d vazio][topo w][baixo w]
  linha 2 (altura h):  [direita d][frente w][esquerda d][trás w]
A frente é a face -Z (para onde o mob olha).
"""
import math
import random
from PIL import Image


def clamp(v):
    return max(0, min(255, int(v)))


def shade(c, k):
    return tuple(clamp(x * k) for x in c[:3])


def hexc(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


class Box:
    def __init__(self, origin, size, color, faces=None, mirror=False, glow=False, pattern=None, noise=0.035, inflate=0.0):
        self.origin = origin
        self.size = size
        self.color = hexc(color) if isinstance(color, int) else color
        self.faces = faces or {}
        self.mirror = mirror
        self.glow = glow
        self.pattern = pattern
        self.noise = noise
        self.inflate = inflate
        self.uv = None

    @property
    def isize(self):
        """Tamanho inteiro usado na textura. O Minecraft mapeia a UV com o tamanho real da caixa, então caixa de tamanho
        quebrado (1,5; 0,01...) lê a textura fora do lugar: a caixa vira inteira na UV e encolhe com CubeDeformation."""
        return tuple(max(1, math.ceil(s - 1e-6)) for s in self.size)

    @property
    def grow(self):
        return tuple((s - n) / 2.0 for s, n in zip(self.size, self.isize))

    @property
    def uvsize(self):
        w, h, d = self.isize
        return (2 * (w + d), d + h)


class Part:
    def __init__(self, model, name, parent, pivot, rot):
        self.model = model
        self.name = name
        self.parent = parent
        self.pivot = pivot
        self.rot = rot
        self.boxes = []

    def box(self, origin, size, color, **kw):
        b = Box(origin, size, color, **kw)
        self.boxes.append(b)
        return self

    def child(self, name, pivot=(0, 0, 0), rot=(0, 0, 0)):
        return self.model.part(name, self, pivot, rot)


class Model:
    def __init__(self, name, tex_w=64, seed=1):
        self.name = name
        self.tex_w = tex_w
        self.parts = []
        self.seed = seed

    def part(self, name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0)):
        p = Part(self, name, parent, pivot, rot)
        self.parts.append(p)
        return p

    # ----- UV -----
    def pack(self):
        boxes = [b for p in self.parts for b in p.boxes]
        order = sorted(boxes, key=lambda b: (-b.uvsize[1], -b.uvsize[0]))
        x = y = shelf = 0
        for b in order:
            w, h = b.uvsize
            if x + w > self.tex_w:
                x = 0
                y += shelf
                shelf = 0
            b.uv = (x, y)
            x += w
            shelf = max(shelf, h)
        used = y + shelf
        th = 16
        while th < used:
            th *= 2
        self.tex_h = th

    # ----- textura -----
    def paint(self, path, glow_path=None):
        img = Image.new('RGBA', (self.tex_w, self.tex_h), (0, 0, 0, 0))
        glow = Image.new('RGBA', (self.tex_w, self.tex_h), (0, 0, 0, 0)) if glow_path else None
        rng = random.Random(self.seed)
        for p in self.parts:
            for b in p.boxes:
                self._paint_box(img, glow, b, rng)
        img.save(path)
        if glow_path:
            glow.save(glow_path)

    def _paint_box(self, img, glow, b, rng):
        w, h, d = b.isize
        u, v = b.uv
        regions = {
            'top': (u + d, v, w, d, 1.10),
            'bottom': (u + d + w, v, w, d, 0.72),
            'right': (u, v + d, d, h, 0.88),
            'front': (u + d, v + d, w, h, 1.0),
            'left': (u + d + w, v + d, d, h, 0.88),
            'back': (u + 2 * d + w, v + d, w, h, 0.82),
        }
        px = img.load()
        cols = [rng.randint(0, 3) for _ in range(64)]
        for face, (x0, y0, fw, fh, k) in regions.items():
            side = face not in ('top', 'bottom')
            for yy in range(fh):
                for xx in range(fw):
                    n = 1.0 + rng.uniform(-b.noise, b.noise)
                    # luz de cima: as laterais escurecem para baixo
                    if side and fh > 1:
                        n *= 1.06 - 0.16 * yy / (fh - 1)
                    # contorno suave nas bordas da face
                    if fw > 2 and fh > 2 and (xx == 0 or xx == fw - 1 or (side and yy == fh - 1)):
                        n *= 0.9
                    if b.pattern == 'fur' and (yy + cols[(xx + x0) % 64]) % 4 == 0:
                        n *= 0.84
                    if b.pattern == 'fur' and (yy + cols[(xx + x0) % 64]) % 4 == 2:
                        n *= 1.06
                    if b.pattern == 'scales':
                        # escamas em losango
                        if (xx + yy) % 4 == 0 or (xx - yy) % 4 == 0:
                            n *= 0.78
                        elif (xx + yy) % 4 == 2 and (xx - yy) % 4 == 2:
                            n *= 1.12
                    if b.pattern == 'stripes' and yy % 3 == 0:
                        n *= 0.82
                    px[x0 + xx, y0 + yy] = shade(b.color, k * n) + (255,)
            fn = b.faces.get(face)
            if fn:
                fn(Painter(img, glow, x0, y0, fw, fh, rng, b.color))
            if b.glow and glow is not None:
                g = glow.load()
                for yy in range(fh):
                    for xx in range(fw):
                        g[x0 + xx, y0 + yy] = px[x0 + xx, y0 + yy]

    # ----- Java -----
    def java(self):
        lines = []
        var = {}
        for p in self.parts:
            pv = 'root' if p.parent is None else var[p.parent.name]
            v = 'p_' + p.name
            var[p.name] = v
            cubes = 'CubeListBuilder.create()'
            for b in p.boxes:
                gx, gy, gz = b.grow
                ox, oy, oz = b.origin[0] + gx, b.origin[1] + gy, b.origin[2] + gz
                w, h, d = b.isize
                cubes += f'.texOffs({b.uv[0]}, {b.uv[1]})'
                if b.mirror:
                    cubes += '.mirror()'
                if b.inflate or gx or gy or gz:
                    i = b.inflate
                    cubes += (f'.addBox({f(ox)}, {f(oy)}, {f(oz)}, {f(w)}, {f(h)}, {f(d)}, '
                              f'new CubeDeformation({f(gx + i)}, {f(gy + i)}, {f(gz + i)}))')
                else:
                    cubes += f'.addBox({f(ox)}, {f(oy)}, {f(oz)}, {f(w)}, {f(h)}, {f(d)})'
                if b.mirror:
                    cubes += '.mirror(false)'
            px, py, pz = p.pivot
            rx, ry, rz = p.rot
            if (rx, ry, rz) == (0, 0, 0):
                pose = f'PartPose.offset({f(px)}, {f(py)}, {f(pz)})'
            else:
                pose = f'PartPose.offsetAndRotation({f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)})'
            lines.append(f'        PartDefinition {v} = {pv}.addOrReplaceChild("{p.name}", {cubes}, {pose});')
        body = '\n'.join(lines)
        return ('    public static LayerDefinition createBodyLayer() {\n'
                '        MeshDefinition mesh = new MeshDefinition();\n'
                '        PartDefinition root = mesh.getRoot();\n'
                f'{body}\n'
                f'        return LayerDefinition.create(mesh, {self.tex_w}, {self.tex_h});\n'
                '    }\n')


def f(x):
    return f'{float(x)}F'


class Painter:
    """Ajuda a desenhar detalhes numa face (coordenadas locais da face)."""

    def __init__(self, img, glow, x0, y0, w, h, rng, base):
        self.img = img
        self.glow = glow
        self.x0, self.y0, self.w, self.h = x0, y0, w, h
        self.rng = rng
        self.base = base

    def px(self, x, y, color, glow=False):
        if 0 <= x < self.w and 0 <= y < self.h:
            c = hexc(color) if isinstance(color, int) else color
            self.img.putpixel((self.x0 + x, self.y0 + y), c[:3] + (255,))
            if glow and self.glow is not None:
                self.glow.putpixel((self.x0 + x, self.y0 + y), c[:3] + (255,))

    def pxa(self, x, y, color, alpha, glow=False):
        """Pixel com transparência (alpha 0-255) — partes fantasmagóricas."""
        if 0 <= x < self.w and 0 <= y < self.h:
            c = hexc(color) if isinstance(color, int) else color
            self.img.putpixel((self.x0 + x, self.y0 + y), c[:3] + (alpha,))
            if glow and self.glow is not None:
                self.glow.putpixel((self.x0 + x, self.y0 + y), c[:3] + (alpha,))

    def clear(self, x, y):
        """Pixel transparente (recorte: bordas rasgadas, buracos)."""
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((self.x0 + x, self.y0 + y), (0, 0, 0, 0))

    def rect(self, x, y, w, h, color, glow=False):
        for yy in range(h):
            for xx in range(w):
                self.px(x + xx, y + yy, color, glow)

    def fill(self, color, noise=0.08):
        c = hexc(color) if isinstance(color, int) else color
        for yy in range(self.h):
            for xx in range(self.w):
                self.px(xx, yy, shade(c, 1 + self.rng.uniform(-noise, noise)))

    def row(self, y, color, glow=False):
        self.rect(0, y, self.w, 1, color, glow)
