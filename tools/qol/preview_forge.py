"""Prévia da Forja Trituradora: as 8 peças soltas e o modelo montado (desligado/ligado).
Uso: python3 tools/qol/preview_forge.py saida.png   (na raiz do projeto; usa tools/blockview.py)"""
import sys

sys.path.insert(0, 'tools')
sys.path.insert(0, 'tools/boitata')
from PIL import Image  # noqa: E402

import blockview  # noqa: E402
import zview  # noqa: E402


def shifted(ref, dx, dy, dz):
    out = []
    for pts, c in blockview.quads(blockview.load(ref)):
        out.append(([(x + dx, y - dy, z + dz) for x, y, z in pts], c))
    return out


def loose():
    # frente = norte (z=0); direita de quem olha = oeste (-x); trás = +z; cima = +y (zview usa y para baixo)
    q = []
    q += shifted('mistech:block/crusher_forge', 0, 0, 0)
    q += shifted('mistech:block/arcane_furnace', -16, 0, 0)
    q += shifted('mistech:block/impure_magic_block', 0, 0, 16)
    q += shifted('mistech:block/impure_magic_block', -16, 0, 16)
    q += shifted('mistech:block/arcane_crusher', 0, 16, 0)
    q += shifted('mistech:block/impure_magic_block', -16, 16, 0)
    q += shifted('mistech:block/impure_magic_block', 0, 16, 16)
    q += shifted('mistech:block/impure_magic_block', -16, 16, 16)
    return q


# ---- mós (iguais às do CrusherForgeRenderer) ----
N, R_BODY, HALF = 16, 6.5, 11.0
TEETH, T_IN, T_OUT, T_W, T_HALF = 12, 5.8, 7.5, 1.0, 10.5
WHEELS = [(-7.5, 24.0, 16.0, -1), (7.5, 24.0, 16.0, 1)]
_tex = {}


def tex(name):
    if name not in _tex:
        ns, p = name.split(':')
        base = 'src/main/resources/assets/' if ns == 'mistech' else '/home/claude/w/van/assets/'
        im = Image.open(base + ns + '/textures/' + p + '.png').convert('RGBA')
        _tex[name] = im.crop((0, 0, im.width, im.width))
    return _tex[name]


def face(pts, uvs, name, shade, n=4):
    im = tex(name)
    out = []

    def lerp(a, b, t):
        return tuple(a[i] + (b[i] - a[i]) * t for i in range(len(a)))

    def at(s, t, arr):
        return lerp(lerp(arr[0], arr[1], s), lerp(arr[3], arr[2], s), t)
    for j in range(n):
        for i in range(n):
            u, v = at((i + 0.5) / n, (j + 0.5) / n, uvs)
            c = im.getpixel((min(im.width - 1, max(0, int(u * im.width))), min(im.width - 1, max(0, int(v * im.width)))))
            c = tuple(int(c[k] * shade) for k in range(3)) + (255,)
            q = [at(i / n, j / n, pts), at((i + 1) / n, j / n, pts), at((i + 1) / n, (j + 1) / n, pts), at(i / n, (j + 1) / n, pts)]
            out.append(([(x - 8, -y, z - 8) for x, y, z in q], c))
    return out


def wheel(cx, cy, cz, ang):
    import math
    out = []
    a = math.radians(ang)

    def P(x, y, z):
        return (cx + x * math.cos(a) - y * math.sin(a), cy + x * math.sin(a) + y * math.cos(a), cz + z)
    poly = [(R_BODY * math.cos(math.radians(k * 360 / N + 180 / N)), R_BODY * math.sin(math.radians(k * 360 / N + 180 / N))) for k in range(N)]
    fu = lambda x, y: ((x + 8) / 16, (8 - y) / 16)
    for k in range(N):
        (x0, y0), (x1, y1) = poly[k], poly[(k + 1) % N]
        out += face([P(x0, y0, -HALF), P(x1, y1, -HALF), P(x1, y1, HALF), P(x0, y0, HALF)],
                    [(0.3, 0), (0.45, 0), (0.45, 1), (0.3, 1)], 'mistech:block/crusher_wheel_rim', 0.75, 3)
    # face da frente: grade de texels dentro do círculo (no jogo é um leque de triângulos com a mesma textura)
    for ty in range(16):
        for tx in range(16):
            x0, y0 = tx - 8, 8 - ty - 1
            if math.hypot(x0 + 0.5, y0 + 0.5) <= R_BODY:
                out += face([P(x0, y0 + 1, -HALF), P(x0 + 1, y0 + 1, -HALF), P(x0 + 1, y0, -HALF), P(x0, y0, -HALF)],
                            [fu(x0 + 0.5, y0 + 0.5)] * 4, 'mistech:block/crusher_wheel_face', 0.9, 1)
    for t in range(TEETH):
        b = math.radians(t * 360 / TEETH)

        def T(x, y, z):
            return P(x * math.cos(b) - y * math.sin(b), x * math.sin(b) + y * math.cos(b), z)
        w, i0, o = T_W, T_IN, T_OUT
        out += face([T(-w, o, -T_HALF), T(w, o, -T_HALF), T(w, o, T_HALF), T(-w, o, T_HALF)], [(0, 0), (0.25, 0), (0.25, 1), (0, 1)],
                    'mistech:block/crusher_wheel_tooth', 0.95, 2)
        out += face([T(-w, i0, -T_HALF), T(w, i0, -T_HALF), T(w, o, -T_HALF), T(-w, o, -T_HALF)], [(0, 0), (0.25, 0), (0.25, 0.2), (0, 0.2)],
                    'mistech:block/crusher_wheel_tooth', 0.8, 1)
        for sx in (-w, w):
            out += face([T(sx, i0, -T_HALF), T(sx, o, -T_HALF), T(sx, o, T_HALF), T(sx, i0, T_HALF)], [(0, 0), (0.2, 0), (0.2, 1), (0, 1)],
                        'mistech:block/crusher_wheel_tooth', 0.7, 2)
    # cubo e eixo na frente
    for r, z0, z1, name in [(2.0, -HALF - 0.6, -HALF, 'minecraft:block/iron_block'), (1.0, -HALF - 1.6, -HALF - 0.6, 'minecraft:block/gold_block')]:
        out += face([P(-r, -r, z0), P(r, -r, z0), P(r, r, z0), P(-r, r, z0)], [(0, 1), (1, 1), (1, 0), (0, 0)], name, 0.95, 2)
    return out


def wheels(angle):
    q = []
    for cx, cy, cz, d in WHEELS:
        q += wheel(cx, cy, cz, d * angle + (15 if d > 0 else 0))
    return q


def main(target):
    rows = []
    for label, qs in [('soltas', loose()), ('montada', shifted('mistech:block/crusher_forge_formed', 0, 0, 0) + wheels(0)),
                      ('ligada', shifted('mistech:block/crusher_forge_formed_on', 0, 0, 0) + wheels(40))]:
        views = [zview.zdraw(qs, yaw, -25, 7) for yaw in (-25, 60, 160)]
        rows.append(views)
    W = max(sum(v.width for v in r) for r in rows)
    H = sum(max(v.height for v in r) for r in rows)
    sheet = Image.new('RGBA', (W, H), (70, 74, 82, 255))
    y = 0
    for r in rows:
        x = 0
        for v in r:
            sheet.alpha_composite(v, (x, y))
            x += v.width
        y += max(v.height for v in r)
    sheet.save(target)
    big = zview.zdraw(shifted('mistech:block/crusher_forge_formed_on', 0, 0, 0) + wheels(25), -20, -15, 14)
    big.save(target.replace('.png', '_frente.png'))


if __name__ == '__main__':
    main(sys.argv[1])
