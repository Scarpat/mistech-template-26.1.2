"""Prévia de modelos de bloco JSON do Mistech (com texturas), para conferir sem abrir o jogo.
Uso: python tools/blockview.py saida.png modelo1[+modelo2...] [modelo...]   (modelo = 'mistech:block/nome')
Vários modelos separados por '+' são desenhados juntos (multipart). Tintas: tintindex 0 = laranja, 1 = azul."""
import json, math, sys
from PIL import Image
sys.path.insert(0, 'tools/boitata')
import zview

R = 'src/main/resources/assets/'
VAN = 'vanilla/assets/'
TINTS = {0: (255, 140, 60), 1: (63, 118, 228)}


def path(ref, kind):
    ns, p = ref.split(':') if ':' in ref else ('minecraft', ref)
    base = R if ns == 'mistech' else '/home/claude/w/van/assets/'
    return base + ns + '/' + kind + '/' + p + ('.json' if kind == 'models' else '.png')


def load(ref):
    m = json.load(open(path(ref, 'models')))
    tex = {}
    elements = m.get('elements')
    if 'parent' in m and not m['parent'].startswith('minecraft:block/block'):
        try:
            p = load(m['parent'])
            tex.update(p['textures'])
            elements = elements or p['elements']
        except FileNotFoundError:
            pass
    tex.update(m.get('textures', {}))
    return {'textures': tex, 'elements': elements or []}


def resolve(tex, key):
    seen = 0
    while key.startswith('#') and seen < 10:
        key = tex.get(key[1:], key)
        seen += 1
    return key


def quads(model):
    out = []
    cache = {}
    for e in model['elements']:
        (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
        rot = e.get('rotation')
        for d, f in e['faces'].items():
            ref = resolve(model['textures'], f['texture'])
            if ref not in cache:
                im = Image.open(path(ref, 'textures')).convert('RGBA')
                cache[ref] = im.crop((0, 0, im.width, im.width))
            im = cache[ref]
            uv = f.get('uv')
            if uv is None:
                uv = {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
                      'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0], 'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}[d]
            # pontos da face: (s,t) -> 3D, s = u, t = v
            def pt(s, t, d=d):
                if d == 'north':
                    return (x1 - s * (x1 - x0), y1 - t * (y1 - y0), z0)
                if d == 'south':
                    return (x0 + s * (x1 - x0), y1 - t * (y1 - y0), z1)
                if d == 'west':
                    return (x0, y1 - t * (y1 - y0), z0 + s * (z1 - z0))
                if d == 'east':
                    return (x1, y1 - t * (y1 - y0), z1 - s * (z1 - z0))
                if d == 'up':
                    return (x0 + s * (x1 - x0), y1, z0 + t * (z1 - z0))
                return (x0 + s * (x1 - x0), y0, z1 - t * (z1 - z0))
            u0, v0, u1, v1 = uv
            n = 6
            sc = im.width / 16.0
            for j in range(n):
                for i in range(n):
                    tu = (u0 + (u1 - u0) * (i + 0.5) / n) * sc
                    tv = (v0 + (v1 - v0) * (j + 0.5) / n) * sc
                    c = im.getpixel((min(im.width - 1, max(0, int(tu))), min(im.width - 1, max(0, int(tv)))))
                    if c[3] < 100:
                        continue
                    if 'tintindex' in f:
                        t = TINTS.get(f['tintindex'], (255, 255, 255))
                        c = tuple(c[k] * t[k] // 255 for k in range(3)) + (255,)
                    k = {'up': 1.0, 'down': 0.55, 'north': 0.85, 'south': 0.85, 'east': 0.7, 'west': 0.7}[d]
                    c = tuple(int(c[q] * k) for q in range(3)) + (255,)
                    pts = [pt(i / n, j / n), pt((i + 1) / n, j / n), pt((i + 1) / n, (j + 1) / n), pt(i / n, (j + 1) / n)]
                    if rot:
                        ox, oy, oz = rot['origin']
                        a = math.radians(rot['angle'])
                        ax = rot['axis']

                        def r(p, ax=ax, a=a, ox=ox, oy=oy, oz=oz):
                            x, y, z = p[0] - ox, p[1] - oy, p[2] - oz
                            if ax == 'y':
                                x, z = x * math.cos(a) + z * math.sin(a), -x * math.sin(a) + z * math.cos(a)
                            elif ax == 'x':
                                y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
                            else:
                                x, y = x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a)
                            return (x + ox, y + oy, z + oz)
                        pts = [r(p) for p in pts]
                    # zview espera y para baixo e frente -z
                    out.append(([(p[0] - 8, -p[1], p[2] - 8) for p in pts], c))
    return out


if __name__ == '__main__':
    target = sys.argv[1]
    views = []
    for arg in sys.argv[2:]:
        qs = []
        for ref in arg.split('+'):
            qs += quads(load(ref))
        for yaw in (-35, 145):
            views.append(zview.zdraw(qs, yaw, -25, 14))
    W = sum(v.width for v in views)
    H = max(v.height for v in views)
    sheet = Image.new('RGBA', (W, H), (70, 74, 82, 255))
    x = 0
    for v in views:
        sheet.alpha_composite(v, (x, 0))
        x += v.width
    sheet.save(target)
