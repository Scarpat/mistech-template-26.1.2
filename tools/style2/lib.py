"""Kit de amostra do redesign (rodada nov/2026, parte: maquinas Tier I).
Lado tech (Create): carcaca de madeira + cantoneiras de cobre + painel de pedra escura.
Lado magico (Thaumcraft): pedra arcana + cintas de latao + vidro + brilho roxo.
Elos comuns: cobre/latao com rebites, runa roxa pulsante (mesma da Fornalha Runica)."""
import os, sys, json, math
from PIL import Image
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../style'))
from kit import COBRE, LATAO, FERRO, PEDRA, ARDOSIA, AMETISTA, R
VAN = os.path.expanduser('~/van/assets/minecraft/textures/')
CLEAR = (0, 0, 0, 0)
ROXOS = R('3b1670', '5a24a8', '7a3fd0', '9a5cf0', 'b884ff', 'd4aaff', 'efdcff')
MADEIRA = R('2a1a0e', '3e2814', '553820', '6b4a2b', '826038', '9c7748', 'b8925e')
# pedra arcana: ardosia fria puxada ao roxo
ARCANA = R('121019', '1c1826', '272236', '332c45', '403858', '52486e', '6a5f8a')

def V(name, kind='block'):
    im = Image.open(f'{VAN}{kind}/{name}.png').convert('RGBA')
    return im.crop((0, 0, 16, 16))

def blank(): return Image.new('RGBA', (16, 16), CLEAR)

def lum(p): return (p[0] * 3 + p[1] * 6 + p[2]) / 10

def remap(src, ramp, lo=None, hi=None):
    """Recolore uma textura vanilla para uma rampa, preservando o desenho (valores de luz)."""
    px = [lum(src.getpixel((x, y))) for y in range(16) for x in range(16) if src.getpixel((x, y))[3]]
    lo = min(px) if lo is None else lo; hi = max(px) if hi is None else hi
    out = src.copy()
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if not p[3]: continue
            t = (lum(p) - lo) / max(1, hi - lo)
            out.putpixel((x, y), ramp[max(0, min(len(ramp) - 1, int(round(t * (len(ramp) - 1)))))])
    return out

def shade(im, box, k):
    x0, y0, x1, y1 = box
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p = im.getpixel((x, y))
            if p[3]: im.putpixel((x, y), tuple(max(0, min(255, int(c * k))) for c in p[:3]) + (p[3],))

def rect(im, box, c):
    x0, y0, x1, y1 = box
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1): im.putpixel((x, y), c)

def paste_region(dst, src, box, at=None):
    x0, y0, x1, y1 = box; ax, ay = at or (x0, y0)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1): dst.putpixel((ax + x - x0, ay + y - y0), src.getpixel((x, y)))

def bracket(im, cx, cy, sx, sy, metal=COBRE):
    """Cantoneira em L de 3x3 no canto (cx,cy); sx/sy = direcao para dentro (+1/-1)."""
    for dy in range(2):
        for dx in range(2):
            x, y = cx + dx * sx, cy + dy * sy
            im.putpixel((x, y), metal[4] if (dx + dy) == 0 else metal[3])
    im.putpixel((cx + (sx > 0 and 0 or 0) + (1 if sx > 0 else -1) * 0, cy), metal[5])
    im.putpixel((cx + sx, cy + sy), LATAO[5])      # rebite

def wood_frame(im, x0, y0, x1, y1, w=2):
    """Moldura de madeira (veio segue a barra), luz de cima-esquerda."""
    log = V('stripped_spruce_log')
    logh = log.rotate(90)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            top, bot = y < y0 + w, y > y1 - w
            left, right = x < x0 + w, x > x1 - w
            if not (top or bot or left or right): continue
            c = (logh if (top or bot) else log).getpixel((x, y))
            im.putpixel((x, y), c)
    # chanfro: borda externa clara em cima/esq, escura embaixo/dir
    for x in range(x0, x1 + 1):
        shade(im, (x, y0, x, y0), 1.18); shade(im, (x, y1, x, y1), 0.62)
    for y in range(y0 + 1, y1):
        shade(im, (x0, y, x0, y), 1.12); shade(im, (x1, y, x1, y), 0.7)
    # quina interna da moldura: luz embaixo/dir (borda que recebe luz), sombra em cima/esq
    for x in range(x0 + w, x1 - w + 1):
        shade(im, (x, y1 - w + 1, x, y1 - w + 1), 1.12)
    for y in range(y0 + w, y1 - w + 1):
        shade(im, (x1 - w + 1, y, x1 - w + 1, y), 1.1)
    # borda interna: sombra projetada no painel embaixo da barra de cima / esquerda
    return im

def casing(y0=0, panel='deepslate_tiles', corners=True):
    """Face da carcaca tech (ocupa as linhas y0..15)."""
    im = blank()
    pan = V(panel); shade(pan, (0, 0, 15, 15), 0.92)
    paste_region(im, pan, (0, y0, 15, 15))
    wood_frame(im, 0, y0, 15, 15)
    # painel rebaixado: sombra da moldura em cima/esq, luz fraca embaixo/dir
    for x in range(2, 14): shade(im, (x, y0 + 2, x, y0 + 2), 0.55)
    for y in range(y0 + 3, 14): shade(im, (2, y, 2, y), 0.62)
    for x in range(3, 14): shade(im, (x, 13, x, 13), 1.25)
    for y in range(y0 + 3, 14): shade(im, (13, y, 13, y), 1.18)
    if corners:
        bracket(im, 0, y0, 1, 1); bracket(im, 15, y0, -1, 1)
        bracket(im, 0, 15, 1, -1); bracket(im, 15, 15, -1, -1)
    return im

def runeplate(im, x0, y0, x1, y1, rune, lit=False, metal=COBRE):
    rect(im, (x0, y0, x1, y1), metal[3])
    for x in range(x0, x1 + 1): im.putpixel((x, y0), metal[5]); im.putpixel((x, y1), metal[1])
    for y in range(y0, y1 + 1): im.putpixel((x0, y), metal[4]); im.putpixel((x1, y), metal[1])
    for (x, y) in rune: im.putpixel((x, y), ROXOS[4] if lit else metal[1])
    return im

def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True); im.save(path)

def box(fr, to, faces, rot=None, name=None):
    e = {'from': list(fr), 'to': list(to), 'faces': faces}
    if rot: e['rotation'] = rot
    if name: e['name'] = name
    return e

def all_faces(tex, uv=None, skip=()):
    return {d: ({'texture': tex, 'uv': uv[d]} if uv and d in uv else {'texture': tex})
            for d in ('north', 'south', 'east', 'west', 'up', 'down') if d not in skip}
