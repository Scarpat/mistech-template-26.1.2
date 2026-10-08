# Construtor simples de modelos de item 3D (elementos JSON) com textura pintada por código + prévia.
# Usado por tools/artifacts/gen_designs.py.
import json, math, os, random
from PIL import Image, ImageDraw

FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def hexc(h, a=255):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def mix(a, b, t):
    return tuple(int(a[k] + (b[k] - a[k]) * t) for k in range(3)) + (255,)


def jitter(c, rnd, amt=10):
    d = rnd.randint(-amt, amt)
    return tuple(max(0, min(255, c[k] + d)) for k in range(3)) + (c[3],)


class Box:
    def __init__(self, frm, to, paint, faces=FACES, rot=None, name=''):
        self.frm, self.to, self.paint, self.faces, self.rot, self.name = frm, to, paint, faces, rot, name

    def size(self, face):
        dx, dy, dz = (self.to[i] - self.frm[i] for i in range(3))
        return {'north': (dx, dy), 'south': (dx, dy), 'east': (dz, dy), 'west': (dz, dy), 'up': (dx, dz), 'down': (dx, dz)}[face]


class Model:
    def __init__(self, name, tex=64, seed=1):
        self.name, self.tex, self.boxes = name, tex, []
        self.rnd = random.Random(seed)

    def box(self, frm, to, paint, **kw):
        b = Box(frm, to, paint, **kw)
        self.boxes.append(b)
        return b

    # ------------------------------------------------------------ textura + uv
    def build(self):
        img = Image.new('RGBA', (self.tex, self.tex), (0, 0, 0, 0))
        x = y = row_h = 0
        self.uv = {}
        for bi, b in enumerate(self.boxes):
            for f in b.faces:
                w, h = b.size(f)
                pw, ph = max(1, math.ceil(w - 1e-6)), max(1, math.ceil(h - 1e-6))
                if x + pw > self.tex:
                    x, y, row_h = 0, y + row_h, 0
                if y + ph > self.tex:
                    raise SystemExit(self.name + ': textura pequena demais')
                face_img = b.paint(f, pw, ph, self.rnd)
                img.paste(face_img, (x, y))
                self.uv[(bi, f)] = (x, y, x + max(w, 0.01), y + max(h, 0.01))
                x += pw
                row_h = max(row_h, ph)
        self.img = img
        return img

    def json(self, texture, display):
        k = 16.0 / self.tex
        els = []
        for bi, b in enumerate(self.boxes):
            faces = {}
            for f in b.faces:
                u1, v1, u2, v2 = self.uv[(bi, f)]
                faces[f] = {'uv': [round(u1 * k, 4), round(v1 * k, 4), round(u2 * k, 4), round(v2 * k, 4)], 'texture': '#t'}
            e = {'from': list(b.frm), 'to': list(b.to), 'faces': faces}
            if b.name:
                e['name'] = b.name
            if b.rot:
                axis, angle, origin = b.rot
                e['rotation'] = {'angle': angle, 'axis': axis, 'origin': list(origin)}
            els.append(e)
        return {'textures': {'t': texture, 'particle': texture}, 'elements': els, 'display': display}

    # ------------------------------------------------------------ prévia (ortográfica, pintor)
    def preview(self, path, yaws=(30, 150), pitch=25, scale=10, fit=None):
        quads = []
        for bi, b in enumerate(self.boxes):
            x1, y1, z1 = b.frm
            x2, y2, z2 = b.to
            for f in b.faces:
                u1, v1, u2, v2 = self.uv[(bi, f)]
                w, h = b.size(f)
                for j in range(max(1, math.ceil(h - 1e-6))):
                    for i in range(max(1, math.ceil(w - 1e-6))):
                        c = self.img.getpixel((int(u1) + i, int(v1) + j))
                        if c[3] < 128:
                            continue
                        a0, a1 = i, min(i + 1, w)
                        c0, c1 = j, min(j + 1, h)
                        pts = [self._pt(f, (x1, y1, z1, x2, y2, z2), uu, vv) for uu, vv in ((a0, c0), (a1, c0), (a1, c1), (a0, c1))]
                        if b.rot:
                            pts = [rotate(p, b.rot) for p in pts]
                        shade = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6}[f]
                        quads.append((pts, tuple(int(c[k] * shade) for k in range(3))))
        views = []
        for yaw in yaws:
            views.append(render(quads, yaw, pitch, scale, fit))
        W = sum(v.width for v in views)
        H = max(v.height for v in views)
        out = Image.new('RGB', (W, H), (40, 44, 52))
        x = 0
        for v in views:
            out.paste(v, (x, 0))
            x += v.width
        out.save(path)

    @staticmethod
    def _pt(f, bb, u, v):
        x1, y1, z1, x2, y2, z2 = bb
        if f == 'north':
            return (x2 - u, y2 - v, z1)
        if f == 'south':
            return (x1 + u, y2 - v, z2)
        if f == 'west':
            return (x1, y2 - v, z1 + u)
        if f == 'east':
            return (x2, y2 - v, z2 - u)
        if f == 'up':
            return (x1 + u, y2, z1 + v)
        return (x1 + u, y1, z2 - v)


def rotate(p, rot):
    axis, angle, o = rot
    a = math.radians(angle)
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    c, s = math.cos(a), math.sin(a)
    if axis == 'x':
        y, z = y * c - z * s, y * s + z * c
    elif axis == 'y':
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + o[0], y + o[1], z + o[2])


def render(quads, yaw, pitch, scale, fit):
    ya, pa = math.radians(yaw), math.radians(pitch)

    def proj(p):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        x, z = x * math.cos(ya) - z * math.sin(ya), x * math.sin(ya) + z * math.cos(ya)
        y, z = y * math.cos(pa) - z * math.sin(pa), y * math.sin(pa) + z * math.cos(pa)
        return x, y, z

    pq = []
    for pts, col in quads:
        pp = [proj(p) for p in pts]
        pq.append((sum(p[2] for p in pp) / 4, pp, col))
    pq.sort(key=lambda q: -q[0])
    size = fit or 48
    img = Image.new('RGB', (int(size * scale), int(size * scale)), (40, 44, 52))
    d = ImageDraw.Draw(img)
    cx = cy = size * scale / 2
    for _, pp, col in pq:
        d.polygon([(cx + p[0] * scale, cy - p[1] * scale) for p in pp], fill=col)
    return img


# ------------------------------------------------------------------ pintores
def solid(base, amt=8, top=None, bottom=None):
    def p(face, w, h, rnd):
        im = Image.new('RGBA', (w, h))
        for y in range(h):
            for x in range(w):
                c = base
                if top and y == 0 and face not in ('up', 'down'):
                    c = top
                if bottom and y == h - 1 and face not in ('up', 'down'):
                    c = bottom
                im.putpixel((x, y), jitter(c, rnd, amt))
        return im
    return p


def grain(dark, light, amt=6):
    """Madeira: veios na direção do comprimento (v nas faces laterais)."""
    def p(face, w, h, rnd):
        im = Image.new('RGBA', (w, h))
        for x in range(w):
            col = light if (x + rnd.randint(0, 1)) % 2 else dark
            for y in range(h):
                c = col if rnd.random() > 0.15 else mix(dark, light, 0.5)
                im.putpixel((x, y), jitter(c, rnd, amt))
        return im
    return p


def bands(colors, amt=6):
    """Faixas horizontais (de cima para baixo) — cada cor é uma linha, repetindo."""
    def p(face, w, h, rnd):
        im = Image.new('RGBA', (w, h))
        for y in range(h):
            for x in range(w):
                im.putpixel((x, y), jitter(colors[y % len(colors)], rnd, amt))
        return im
    return p


def speckle(base, spots, chance=0.25, amt=8):
    def p(face, w, h, rnd):
        im = Image.new('RGBA', (w, h))
        for y in range(h):
            for x in range(w):
                c = rnd.choice(spots) if rnd.random() < chance else base
                im.putpixel((x, y), jitter(c, rnd, amt))
        return im
    return p


def per_face(default, **faces):
    def p(face, w, h, rnd):
        return faces.get(face, default)(face, w, h, rnd)
    return p


def write_model(m, root, display, item_tex_name):
    img = m.build()
    tex_path = root + 'textures/item/' + item_tex_name + '.png'
    os.makedirs(os.path.dirname(tex_path), exist_ok=True)
    img.save(tex_path)
    j = m.json('mistech:item/' + item_tex_name, display)
    json.dump(j, open(root + 'models/item/' + m.name + '.json', 'w'), indent=1)
