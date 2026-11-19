"""Biblioteca do Estilo A (Vanilla+ rúnico), aprovado na Fornalha Rúnica.
Base vanilla escurecida + cinta de cobre lisa que dá a volta + 1 runa centralizada por lateral + camada emissiva roxa."""
import os, json, math
from PIL import Image
from kit import COBRE, AMETISTA, LATAO, R

VAN = os.path.expanduser('~/van/assets/minecraft/')
CLEAR = (0, 0, 0, 0)
ROXOS = R('3b1670', '5a24a8', '7a3fd0', '9a5cf0', 'b884ff', 'd4aaff', 'efdcff')
PRISMA = [tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,) for c in
          ['5a24a8', '7a3fd0', '9a5cf0', 'b884ff', 'd4aaff', 'b884ff', '9a5cf0', 'c070f0', 'e08cff', 'c070f0', '9a5cf0', '7a3fd0']]
VAZIO = (17, 17, 17, 255)

RUNES = {
    'dagaz':   ["X...X", "XX.XX", "X.X.X", "XX.XX", "X...X"],
    'tiwaz':   ["..X..", ".XXX.", "X.X.X", "..X..", "..X.."],
    'ingwaz':  ["..X..", ".X.X.", "X...X", ".X.X.", "..X.."],
    'hagalaz': ["X..X", "XX.X", "X.XX", "X..X", "X..X"],
    'algiz':   ["X.X.X", ".XXX.", "..X..", "..X..", "..X.."],
    'sowilo':  ["...X", "..X.", ".XX.", ".X..", "X..."],
    'othala':  [".XX.", "X..X", ".XX.", "X..X", "X..X"],
}

def V(name, kind='block'):
    im = Image.open(f'{VAN}textures/{kind}/{name}.png').convert('RGBA')
    return im.crop((0, 0, im.width, im.width))

def darken(im, k=0.78, box=(0, 0, 16, 16), only_grey=True):
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            p = im.getpixel((x, y))
            if not p[3]: continue
            if only_grey and not (abs(p[0] - p[1]) < 14 and abs(p[1] - p[2]) < 14): continue
            im.putpixel((x, y), (int(p[0] * k), int(p[1] * k), int(min(255, p[2] * k * 1.04)), p[3]))
    return im

def strap(im, y0, y1, x0=0, x1=15, rivets=True, hole=lambda x, y: False):
    """Cinta de cobre lisa (topo claro, base escura). Ocupa x0..x1 inteiro para dar a volta nas quinas."""
    n = y1 - y0
    for y in range(y0, y1 + 1):
        t = 5 if y == y0 else 1 if y == y1 else (4 if y - y0 <= n // 2 else 3)
        for x in range(x0, x1 + 1):
            if not hole(x, y): im.putpixel((x, y), COBRE[t])
    for (x, y) in ((x0 + 2, y0 + 1), (x1 - 3, y0 + 2), (x0 + 1, y1 - 2), (x1 - 2, y1 - 1)):
        if not hole(x, y) and y0 < y < y1: im.putpixel((x, y), COBRE[min(6, 5)])
    if rivets:
        ym = (y0 + y1) // 2
        for x in (x0 + 1, x1 - 1):
            if not hole(x, ym): im.putpixel((x, ym), LATAO[5]); im.putpixel((x, ym + 1), COBRE[1])

def rune_at(name, cx, cy):
    """Pixels da runa centrada em (cx, cy) (centro do bloco = 7.5)."""
    g = RUNES[name]; w, h = len(g[0]), len(g)
    x0 = int(round(cx - (w - 1) / 2)); y0 = int(round(cy - (h - 1) / 2))
    return [(x0 + i, y0 + j) for j, r in enumerate(g) for i, c in enumerate(r) if c == 'X']

def engrave(im, pts):
    for (x, y) in pts: im.putpixel((x, y), COBRE[1])

def prisma_frames(pts, n=12, size=16, extra=None):
    """Runa pulsando em roxos (como prismarinho). extra: lista de (pixels, cor_fn(f)) adicionais."""
    frames = []
    for f in range(n):
        im = Image.new('RGBA', (size, size), CLEAR)
        for (x, y) in pts: im.putpixel((x, y), PRISMA[int(f + x * 0.6 + y * 0.4) % len(PRISMA)])
        if extra:
            for px, fn in extra:
                for (x, y) in px: im.putpixel((x, y), fn(f, x, y))
        frames.append(im)
    return frames

def strip(frames):
    out = Image.new('RGBA', (frames[0].width, frames[0].height * len(frames)), CLEAR)
    for i, f in enumerate(frames): out.paste(f, (0, f.height * i))
    return out

def save_anim(out, name, frames, ft=4):
    strip(frames).save(f'{out}/{name}.png')
    json.dump({'animation': {'frametime': ft, 'interpolate': True}}, open(f'{out}/{name}.png.mcmeta', 'w'))

def glow_planes(textures_by_face, lo=(0, 0, 0), hi=(16, 16, 16)):
    """Elementos-plano emissivos (light_emission 15) colados nas faces externas indicadas."""
    els = []
    (x0, y0, z0), (x1, y1, z1) = lo, hi
    e = 0.01   # afasta 0,01 da face para não brigar (z-fighting) com a base
    planes = {'north': ([x0, y0, z0 - e], [x1, y1, z0 - e]), 'south': ([x0, y0, z1 + e], [x1, y1, z1 + e]),
              'west': ([x0 - e, y0, z0], [x0 - e, y1, z1]), 'east': ([x1 + e, y0, z0], [x1 + e, y1, z1]),
              'up': ([x0, y1 + e, z0], [x1, y1 + e, z1])}
    for face, tex in textures_by_face.items():
        f, t = planes[face]
        els.append({'from': f, 'to': t, 'shade': False, 'light_emission': 15, 'faces': {face: {'texture': '#' + tex}}})
    return els


# ---------------- v2 da lib: modelos vanilla como peças + cinta com textura de cobre vanilla ----------------
def vmodel(name, prefix):
    """Carrega um modelo vanilla (resolvendo parents), devolve (elementos, texturas) com chaves prefixadas."""
    chain = []; n = name
    while n:
        d = json.load(open(f'{VAN}models/block/{n.split("/")[-1]}.json')); chain.append(d)
        p = d.get('parent'); n = p.split('/')[-1] if p and 'block/block' not in p and 'cube' not in p else None
    tex = {}; els = None
    for d in reversed(chain):
        tex.update(d.get('textures', {}))
        if d.get('elements'): els = d['elements']
    def res(v):
        seen = 0
        while v.startswith('#') and seen < 8: v = tex.get(v[1:], v); seen += 1
        return v if ':' in v else 'minecraft:' + v
    out_t = {prefix + k: res(v) for k, v in tex.items() if not v.startswith('#') or res(v)}
    els = json.loads(json.dumps(els or []))
    for e in els:
        for fd in e['faces'].values():
            fd['texture'] = '#' + prefix + fd['texture'][1:]
            fd.pop('tintindex', None)
    return els, out_t

def move(els, dx=0, dy=0, dz=0):
    for e in els:
        e['from'] = [e['from'][0] + dx, e['from'][1] + dy, e['from'][2] + dz]
        e['to'] = [e['to'][0] + dx, e['to'][1] + dy, e['to'][2] + dz]
        if 'rotation' in e: o = e['rotation']['origin']; e['rotation']['origin'] = [o[0] + dx, o[1] + dy, o[2] + dz]
    return els

def strap_v(im, y0, y1, x0=0, x1=15, rivets=True, hole=lambda x, y: False):
    """Cinta usando o pixel art do bloco de cobre vanilla (com textura), bordas claras/escuras."""
    cu = V('cut_copper')
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if hole(x, y): continue
            if y == y0: im.putpixel((x, y), COBRE[5])
            elif y == y1: im.putpixel((x, y), COBRE[1])
            else: im.putpixel((x, y), cu.getpixel((x % 16, 1 + (y - y0) % 6)))
    if rivets:
        ym = (y0 + y1) // 2
        for x in (x0 + 1, x1 - 1):
            if not hole(x, ym): im.putpixel((x, ym), LATAO[5]); im.putpixel((x, ym + 1), COBRE[1])
