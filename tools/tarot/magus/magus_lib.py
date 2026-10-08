"""Biblioteca do gerador do Mago: partes/caixas com UV de caixa do Minecraft, pintura procedural em densidade 2x
(numpy), emissão de Java (LayerDefinition + mapa de partes) e prévia isométrica."""
import math, random
import numpy as np
from PIL import Image, ImageDraw

S = 2  # texels por pixel de modelo (textura em "alta", como a referência)

# ------------------------------------------------------------------ cores e ruído
def C(h, a=255):
    return np.array([(h >> 16) & 255, (h >> 8) & 255, h & 255, a], dtype=np.float32)

def vnoise(W, H, cell, rng):
    gw, gh = int(W / cell) + 3, int(H / cell) + 3
    g = rng.random((gh, gw)).astype(np.float32)
    ys = np.arange(H) / cell
    xs = np.arange(W) / cell
    y0 = ys.astype(int); x0 = xs.astype(int)
    fy = (ys - y0)[:, None]; fx = (xs - x0)[None, :]
    fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
    a = g[y0][:, x0]; b = g[y0][:, x0 + 1]; c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
    return a * (1 - fx) * (1 - fy) + b * fx * (1 - fy) + c * (1 - fx) * fy + d * fx * fy

def fbm(W, H, rng, cell=8.0, oct=4):
    out = np.zeros((H, W), np.float32); amp = 1.0; tot = 0
    for _ in range(oct):
        out += vnoise(W, H, max(1.0, cell), rng) * amp
        tot += amp; amp *= 0.5; cell /= 2
    return out / tot

def lerp(a, b, t):
    t = t[..., None] if isinstance(t, np.ndarray) else t
    return a + (b - a) * t

def grad_v(H, W, top=1.08, bot=0.82):
    return np.repeat(np.linspace(top, bot, H, dtype=np.float32)[:, None], W, axis=1)

def edge_dark(H, W, amt=0.18, width=2):
    m = np.ones((H, W), np.float32)
    for i in range(width):
        k = 1 - amt * (width - i) / width
        m[i, :] = np.minimum(m[i, :], k); m[H - 1 - i, :] = np.minimum(m[H - 1 - i, :], k)
        m[:, i] = np.minimum(m[:, i], k); m[:, W - 1 - i] = np.minimum(m[:, W - 1 - i], k)
    return m

def shade(img, m):
    img[..., :3] *= m[..., None]
    return img

def solid(H, W, col):
    return np.tile(col, (H, W, 1)).astype(np.float32)

def cracks(H, W, rng, n=3, length=None, wiggle=0.6):
    """Máscara de rachaduras (passeios aleatórios)."""
    m = np.zeros((H, W), np.float32)
    length = length or int((H + W) * 0.6)
    for _ in range(n):
        x, y = rng.random() * W, rng.random() * H
        ang = rng.random() * math.tau
        for _ in range(length):
            xi, yi = int(x), int(y)
            if 0 <= xi < W and 0 <= yi < H:
                m[yi, xi] = 1
            ang += (rng.random() - 0.5) * wiggle
            x += math.cos(ang); y += math.sin(ang)
            if rng.random() < 0.02:
                ang += (rng.random() - 0.5) * 2.5
    return m

def dilate(m, r=1):
    out = m.copy()
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            if dx * dx + dy * dy <= r * r:
                out = np.maximum(out, np.roll(np.roll(m, dy, 0), dx, 1))
    return out

# ------------------------------------------------------------------ desenho de detalhes
def put(img, x, y, col, glow=None, gcol=None):
    H, W = img.shape[:2]
    x, y = int(round(x)), int(round(y))
    if 0 <= x < W and 0 <= y < H:
        img[y, x] = col
        if glow is not None:
            glow[y, x] = gcol if gcol is not None else col

def disc(img, cx, cy, r, col, glow=None, gcol=None, soft=False):
    H, W = img.shape[:2]
    for y in range(max(0, int(cy - r - 1)), min(H, int(cy + r + 2))):
        for x in range(max(0, int(cx - r - 1)), min(W, int(cx + r + 2))):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r:
                if soft:
                    t = 1 - d / (r + 1e-6)
                    img[y, x, :3] = img[y, x, :3] * (1 - t) + col[:3] * t
                else:
                    img[y, x] = col
                if glow is not None:
                    glow[y, x] = gcol if gcol is not None else col

def line(img, x0, y0, x1, y1, col, glow=None, gcol=None, w=1):
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(n + 1):
        t = i / n
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        if w <= 1:
            put(img, x, y, col, glow, gcol)
        else:
            disc(img, x, y, w / 2, col, glow, gcol)

def infinity(img, cx, cy, size, col, glow=None, gcol=None, thick=1):
    """∞ (lemniscata de Bernoulli) centrado em (cx, cy), largura total = size."""
    a = size / 2.0
    pts = []
    for i in range(240):
        t = i / 240 * math.tau
        den = 1 + math.sin(t) ** 2
        pts.append((cx + a * math.cos(t) / den, cy + a * math.sin(t) * math.cos(t) / den * 1.25))
    for (x, y) in pts:
        if thick <= 1:
            put(img, x - 0.5, y - 0.5, col, glow, gcol)
        else:
            disc(img, x, y, thick / 2, col, glow, gcol)

def pentagram(img, cx, cy, r, col, glow=None, gcol=None, ring=True):
    pts = [(cx + r * math.sin(i * math.tau / 5), cy - r * math.cos(i * math.tau / 5)) for i in range(5)]
    for i in range(5):
        a, b = pts[i], pts[(i + 2) % 5]
        line(img, a[0], a[1], b[0], b[1], col, glow, gcol)
    if ring:
        for i in range(160):
            t = i / 160 * math.tau
            put(img, cx + (r + 1.2) * math.cos(t) - 0.5, cy + (r + 1.2) * math.sin(t) - 0.5, col, glow, gcol)

def eye(img, cx, cy, r, socket, iris, glow=None):
    disc(img, cx, cy, r + 0.8, socket)
    disc(img, cx, cy, r * 0.55, iris, glow, iris)

# ------------------------------------------------------------------ modelo
FACES = ('top', 'bottom', 'right', 'front', 'left', 'back')

class Box:
    def __init__(self, origin, size, mat, faces=None, inflate=0.0, glow=False):
        self.origin, self.size, self.mat = origin, size, mat
        self.faces = faces or {}
        self.inflate = inflate
        self.glow = glow
        self.uv = None

    @property
    def isize(self):
        return [max(1, int(math.ceil(s - 1e-6))) for s in self.size]

    @property
    def uvsize(self):
        w, h, d = self.isize
        return (2 * (w + d), d + h)

class Part:
    def __init__(self, model, name, parent, pivot, rot):
        self.model, self.name, self.parent, self.pivot, self.rot = model, name, parent, pivot, rot
        self.boxes = []
        self.children = []

    def box(self, origin, size, mat, **kw):
        self.boxes.append(Box(origin, size, mat, **kw))
        return self

    def child(self, name, pivot=(0, 0, 0), rot=(0, 0, 0)):
        return self.model.part(name, self, pivot, rot)

class Model:
    def __init__(self, tex_w, tex_h, seed=7):
        self.tex_w, self.tex_h = tex_w, tex_h
        self.parts = []
        self.seed = seed

    def part(self, name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0)):
        assert all(p.name != name for p in self.parts), name
        p = Part(self, name, parent, pivot, rot)
        if parent:
            parent.children.append(p)
        self.parts.append(p)
        return p

    def pack(self):
        boxes = [b for p in self.parts for b in p.boxes]
        order = sorted(boxes, key=lambda b: (-b.uvsize[1], -b.uvsize[0]))
        x = y = shelf = 0
        for b in order:
            w, h = b.uvsize
            if x + w > self.tex_w:
                x = 0; y += shelf; shelf = 0
            b.uv = (x, y)
            x += w
            shelf = max(shelf, h)
        used = y + shelf
        if used > self.tex_h:
            raise SystemExit(f'textura pequena: precisa de {used} linhas')
        return used

    def paint(self):
        W, H = self.tex_w * S, self.tex_h * S
        img = np.zeros((H, W, 4), np.float32)
        glow = np.zeros((H, W, 4), np.float32)
        rng = np.random.default_rng(self.seed)
        for p in self.parts:
            for bi, b in enumerate(p.boxes):
                w, h, d = b.isize
                u, v = b.uv
                regions = {
                    'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
                    'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
                    'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h),
                }
                for face, (x0, y0, fw, fh) in regions.items():
                    FW, FH = fw * S, fh * S
                    ctx = Ctx(face, FW, FH, rng, b, p)
                    fn = b.faces.get(face, b.faces.get('sides' if face in ('right', 'front', 'left', 'back') else '_', None))
                    face_img = b.mat(ctx)
                    gimg = ctx.glow
                    if fn:
                        fn(ctx, face_img)
                    face_img = np.clip(face_img, 0, 255)
                    img[y0 * S:y0 * S + FH, x0 * S:x0 * S + FW] = face_img
                    if b.glow:
                        glow[y0 * S:y0 * S + FH, x0 * S:x0 * S + FW] = face_img
                    elif gimg is not None:
                        glow[y0 * S:y0 * S + FH, x0 * S:x0 * S + FW] = np.clip(gimg, 0, 255)
        self.img = Image.fromarray(img.astype(np.uint8), 'RGBA')
        self.glow = Image.fromarray(glow.astype(np.uint8), 'RGBA')
        return self.img, self.glow

    # ----- Java -----
    def java_layer(self):
        lines = []
        var = {}
        for p in self.parts:
            pv = 'root' if p.parent is None else var[p.parent.name]
            v = 'p_' + p.name
            var[p.name] = v
            cubes = 'CubeListBuilder.create()'
            for b in p.boxes:
                ox, oy, oz = b.origin
                w, h, d = b.size
                cubes += f'\n                .texOffs({b.uv[0]}, {b.uv[1]})'
                if b.inflate:
                    cubes += f'.addBox({jf(ox)}, {jf(oy)}, {jf(oz)}, {jf(w)}, {jf(h)}, {jf(d)}, new CubeDeformation({jf(b.inflate)}))'
                else:
                    cubes += f'.addBox({jf(ox)}, {jf(oy)}, {jf(oz)}, {jf(w)}, {jf(h)}, {jf(d)})'
            px, py, pz = p.pivot
            rx, ry, rz = p.rot
            if (rx, ry, rz) == (0, 0, 0):
                pose = f'PartPose.offset({jf(px)}, {jf(py)}, {jf(pz)})'
            else:
                pose = f'PartPose.offsetAndRotation({jf(px)}, {jf(py)}, {jf(pz)}, {jf(rx)}, {jf(ry)}, {jf(rz)})'
            decl = f'PartDefinition {v} = ' if p.children else ''
            lines.append(f'        {decl}{pv}.addOrReplaceChild("{p.name}", {cubes}, {pose});')
        return '\n'.join(lines)

    def java_bind(self):
        lines = []
        for p in self.parts:
            chain = []
            q = p
            while q is not None:
                chain.append(q.name)
                q = q.parent
            expr = 'root' + ''.join(f'.getChild("{n}")' for n in reversed(chain))
            lines.append(f'        map.put("{p.name}", {expr});')
        return '\n'.join(lines)

def jf(x):
    s = f'{float(x):.4f}'.rstrip('0')
    if s.endswith('.'):
        s += '0'
    return s + 'F'

class Ctx:
    """O que a função de material recebe: face, tamanho em texels, rng, a caixa e a parte."""
    def __init__(self, face, W, H, rng, box, part):
        self.face, self.W, self.H, self.rng, self.box, self.part = face, W, H, rng, box, part
        self.glow = None

    def g(self):
        if self.glow is None:
            self.glow = np.zeros((self.H, self.W, 4), np.float32)
        return self.glow

    @property
    def side(self):
        return self.face not in ('top', 'bottom')

# ------------------------------------------------------------------ prévia
def rot_m(rx, ry, rz):
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx

def transforms(model, overrides):
    out = {}
    for p in model.parts:
        r = overrides.get(p.name, p.rot)
        R = rot_m(*r)
        if p.parent is None:
            M, T = R, np.array(p.pivot, float)
        else:
            PM, PT = out[p.parent.name]
            M, T = PM @ R, PT + PM @ np.array(p.pivot, float)
        out[p.name] = (M, T)
    return out

def render(model, roots, out, yaws=(-35, 35, 160), pitch=18, scale=6, overrides=None, step=1):
    overrides = overrides or {}
    tex = np.array(model.img).astype(np.int32)
    tr = transforms(model, overrides)
    names = set()
    for p in model.parts:
        q = p
        while q.parent is not None:
            q = q.parent
        if q.name in roots:
            names.add(p.name)
    quads = []
    for p in model.parts:
        if p.name not in names:
            continue
        M, T = tr[p.name]
        for b in p.boxes:
            quads += box_quads(b, M, T, tex, step)
    views = [draw(quads, y, pitch, scale) for y in yaws]
    W = sum(v.width for v in views); H = max(v.height for v in views)
    sheet = Image.new('RGBA', (W, H), (58, 60, 68, 255))
    x = 0
    for v in views:
        sheet.alpha_composite(v, (x, H - v.height)); x += v.width
    sheet.save(out)

def box_quads(b, M, T, tex, step):
    x0, y0, z0 = [o - b.inflate for o in b.origin]
    w, h, d = [s + 2 * b.inflate for s in b.size]
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    iw, ih, idd = b.isize
    u, v = b.uv
    F = [((u + idd, v + idd, iw, ih), lambda s, t: (x0 + s * w, y0 + t * h, z0)),
         ((u + 2 * idd + iw, v + idd, iw, ih), lambda s, t: (x1 - s * w, y0 + t * h, z1)),
         ((u, v + idd, idd, ih), lambda s, t: (x0, y0 + t * h, z1 - s * d)),
         ((u + idd + iw, v + idd, idd, ih), lambda s, t: (x1, y0 + t * h, z0 + s * d)),
         ((u + idd, v, iw, idd), lambda s, t: (x0 + s * w, y0, z1 - t * d)),
         ((u + idd + iw, v, iw, idd), lambda s, t: (x0 + s * w, y1, z1 - t * d))]
    out = []
    for (fu, fv, fw, fh), fn in F:
        FW, FH = fw * S, fh * S
        for j in range(0, FH, step):
            for i in range(0, FW, step):
                c = tex[fv * S + j, fu * S + i]
                if c[3] == 0:
                    continue
                pts = [fn(i / FW, j / FH), fn((i + step) / FW, j / FH), fn((i + step) / FW, (j + step) / FH), fn(i / FW, (j + step) / FH)]
                pts = [T + M @ np.array(p) for p in pts]
                out.append((pts, tuple(int(k) for k in c)))
    return out

def draw(quads, yaw, pitch, scale):
    ya, pa = math.radians(yaw), math.radians(pitch)
    cy_, sy_, cp, sp_ = math.cos(ya), math.sin(ya), math.cos(pa), math.sin(pa)
    proj = []
    for pts, c in quads:
        sp = []; depth = 0
        for x, y, z in pts:
            X, Y, Z = -x, -y, z
            xr = X * cy_ - Z * sy_
            zr = X * sy_ + Z * cy_
            yr = Y * cp - zr * sp_
            zr2 = Y * sp_ + zr * cp
            sp.append((xr, yr)); depth += zr2
        proj.append((depth / 4, sp, c))
    proj.sort(key=lambda q: -q[0])
    xs = [p[0] for q in proj for p in q[1]]; ys = [p[1] for q in proj for p in q[1]]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    W = int((maxx - minx) * scale) + 20; H = int((maxy - miny) * scale) + 20
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    for _, sp, c in proj:
        poly = [((x - minx) * scale + 10, (maxy - y) * scale + 10) for x, y in sp]
        dr.polygon(poly, fill=c[:3] + (255,))
    return img
