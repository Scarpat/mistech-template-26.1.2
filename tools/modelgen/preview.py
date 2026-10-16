"""Pré-visualização isométrica de um Model do modelgen (com textura), para conferir as formas sem abrir o jogo."""
import math
from PIL import Image, ImageDraw


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def mat_vec(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def rot_zyx(rx, ry, rz):
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return mat_mul(mat_mul(Rz, Ry), Rx)


def world_transform(part, overrides):
    chain = []
    p = part
    while p is not None:
        chain.append(p)
        p = p.parent
    chain.reverse()
    M = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    T = [0, 0, 0]
    for q in chain:
        pv = q.pivot
        r = overrides.get(q.name, q.rot)
        T = [T[i] + mat_vec(M, pv)[i] for i in range(3)]
        M = mat_mul(M, rot_zyx(*r))
    return M, T


def render(model, tex_path, out, yaws=(-35, 35, 180 - 35), pitch=20, scale=10, overrides=None, hide=()):
    overrides = overrides or {}
    tex = Image.open(tex_path).convert('RGBA')
    views = []
    for yaw in yaws:
        quads = []
        for part in model.parts:
            if part.name in hide:
                continue
            M, T = world_transform(part, overrides)
            for b in part.boxes:
                quads += box_quads(b, M, T, tex)
        views.append(draw(quads, yaw, pitch, scale))
    W = sum(v.width for v in views)
    H = max(v.height for v in views)
    sheet = Image.new('RGBA', (W, H), (70, 74, 82, 255))
    x = 0
    for v in views:
        sheet.alpha_composite(v, (x, 0))
        x += v.width
    sheet.save(out)


def box_quads(b, M, T, tex):
    x0, y0, z0 = b.origin
    w, h, d = b.size
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    iw, ih, idd = b.isize
    u, v = b.uv
    faces = []
    # (região uv, função (s,t) frações -> ponto 3D)
    faces.append(((u + idd, v + idd, iw, ih), lambda s, t: (x0 + s * w, y0 + t * h, z0)))  # frente (-z)
    faces.append(((u + 2 * idd + iw, v + idd, iw, ih), lambda s, t: (x1 - s * w, y0 + t * h, z1)))  # trás
    faces.append(((u, v + idd, idd, ih), lambda s, t: (x0, y0 + t * h, z1 - s * d)))  # direita (x0)
    faces.append(((u + idd + iw, v + idd, idd, ih), lambda s, t: (x1, y0 + t * h, z0 + s * d)))  # esquerda (x1)
    faces.append(((u + idd, v, iw, idd), lambda s, t: (x0 + s * w, y0, z1 - t * d)))  # topo
    faces.append(((u + idd + iw, v, iw, idd), lambda s, t: (x0 + s * w, y1, z1 - t * d)))  # baixo
    out = []
    for (fu, fv, fw, fh), fn in faces:
        if fw <= 0 or fh <= 0:
            continue
        for j in range(fh):
            for i in range(fw):
                tx = fu + (fw - 1 - i if b.mirror else i)
                c = tex.getpixel((tx, fv + j))
                if c[3] == 0:
                    continue
                pts = [fn(i / fw, j / fh), fn((i + 1) / fw, j / fh), fn((i + 1) / fw, (j + 1) / fh), fn(i / fw, (j + 1) / fh)]
                pts = [[T[k] + mat_vec(M, p)[k] for k in range(3)] for p in pts]
                out.append((pts, c))
    return out


def draw(quads, yaw, pitch, scale):
    ya, pa = math.radians(yaw), math.radians(pitch)
    proj = []
    for pts, c in quads:
        sp = []
        depth = 0
        for x, y, z in pts:
            # modelo: y para baixo, frente -z. Mundo: vira y.
            X, Y, Z = -x, -y, z
            # gira em y
            xr = X * math.cos(ya) - Z * math.sin(ya)
            zr = X * math.sin(ya) + Z * math.cos(ya)
            yr = Y * math.cos(pa) - zr * math.sin(pa)
            zr2 = Y * math.sin(pa) + zr * math.cos(pa)
            sp.append((xr, yr))
            depth += zr2
        proj.append((depth / 4, sp, c))
    proj.sort(key=lambda q: -q[0])
    xs = [p[0] for q in proj for p in q[1]]
    ys = [p[1] for q in proj for p in q[1]]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    W = int((maxx - minx) * scale) + 20
    H = int((maxy - miny) * scale) + 20
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img, 'RGBA')
    for _, sp, c in proj:
        poly = [((x - minx) * scale + 10, (maxy - y) * scale + 10) for x, y in sp]
        dr.polygon(poly, fill=tuple(c[:4]), outline=tuple(c[:4]) if c[3] == 255 else None)
    return img
