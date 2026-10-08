"""Gera texturas dos blocos de metal e dos isqueiros recolorindo as vanilla pela luminância."""
from PIL import Image
import sys
V = 'vanilla/assets/minecraft/textures/'
OUT = 'src/main/resources/assets/mistech/textures/'
PAL = {
    'mithril': ['1c2c44', '34567a', '4c79a0', '6090b6', '92c1e0', 'ceebf8', 'f4fdff'],
    'adamantine': ['2a0610', '510b1a', '721123', '871629', 'b72a3c', 'e66869', 'ffb4a4'],
    'orichalcum': ['3a140a', '9b4118', 'de7829', 'ec943c', 'ffc05f', 'ffd688', 'fff0b8'],
}
def rgb(h): return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))
def lum(p): return 0.299*p[0] + 0.587*p[1] + 0.114*p[2]
def grad(pal, t, lo=0.0, hi=1.0):
    t = lo + (hi-lo)*max(0.0, min(1.0, t))
    cols = [rgb(c) for c in pal]
    f = t*(len(cols)-1); i = min(int(f), len(cols)-2); r = f-i
    return tuple(round(cols[i][k]*(1-r)+cols[i+1][k]*r) for k in range(3))
def recolor(src, pal, mask=lambda x, y: True, lo=0.0, hi=1.0):
    im = Image.open(src).convert('RGBA'); out = im.copy()
    px = [(x, y) for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3] and mask(x, y)]
    ls = [lum(im.getpixel(p)) for p in px]; a, b = min(ls), max(ls)
    for p, l in zip(px, ls):
        c = grad(pal, (l-a)/(b-a or 1), lo, hi)
        out.putpixel(p, c + (im.getpixel(p)[3],))
    return out
for n, pal in PAL.items():
    recolor(V+'block/iron_block.png', pal, lo=0.25, hi=0.82).save(OUT+'block/%s_block.png' % n)
    recolor(V+'item/flint_and_steel.png', pal, mask=lambda x, y: x <= 7 and y <= 9, lo=0.05, hi=1.0).save(OUT+'item/%s_flint_and_steel.png' % n)
print('ok')
