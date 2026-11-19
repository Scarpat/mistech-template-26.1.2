"""Estilo A3: cinta de cobre que DÁ A VOLTA no bloco (passa pelas quinas) + runas de verdade gravadas na cinta.
Apagada: runas = sulco escuro no cobre com borda de luz embaixo. Acesa: o sulco brilha violeta (ametista).
Frente sem magia: a frente é a boca do fogo."""
import os, sys, json
from estilos4 import V, recolor, is_fire
from kit import COBRE, AMETISTA, LATAO

GLYPHS = {   # runas 3x4 (estilo futhark)
    'raido':  ["XX.", "X.X", "XX.", "X.X"],
    'algiz':  ["X.X", ".X.", ".X.", ".X."],
    'othala': [".X.", "X.X", ".X.", "X.X"],
    'tiwaz':  [".X.", "XXX", ".X.", ".X."],
    'kenaz':  ["..X", ".X.", "X..", ".X."][::1],
}

def strap(im, y0, y1, hole=None):
    """Cinta de cobre de y0 a y1 (inclusive) ocupando a largura TODA (0-15)."""
    recolor(im, COBRE, (0, y0, 16, y1 + 1), pred=lambda p, x, y: not (hole and hole(x, y)) and not is_fire(p, x, y), lo=40, hi=200)
    for x in range(16):
        if not (hole and hole(x, y0)): im.putpixel((x, y0), COBRE[5])
        if not (hole and hole(x, y1)): im.putpixel((x, y1), COBRE[1])
    for y in range(y0 + 1, y1):
        for x in (0, 15):
            if not (hole and hole(x, y)): im.putpixel((x, y), COBRE[3])
    for (x, y) in ((1, (y0 + y1) // 2), (14, (y0 + y1) // 2)):
        im.putpixel((x, y), LATAO[5]); im.putpixel((x, y + 1), COBRE[1])

def engrave(im, names, xs, y, on):
    for name, x0 in zip(names, xs):
        g = GLYPHS[name]
        for j, r in enumerate(g):
            for i, c in enumerate(r):
                if c != 'X': continue
                im.putpixel((x0 + i, y + j), AMETISTA[6 if on and j == 1 else 5] if on else COBRE[0])
                below = (j + 1 >= len(g)) or g[j + 1][i] != 'X'
                if below and not on: im.putpixel((x0 + i, y + j + 1), COBRE[5])

def side(on, names=('raido', 'algiz', 'othala')):
    im = V('furnace_side')
    strap(im, 9, 14)
    engrave(im, names, (3, 7, 11), 10, on)
    return im

def front(on):
    im = V('furnace_front_on' if on else 'furnace_front')
    box = lambda x, y: 3 <= x <= 12 and 11 <= y <= 14 and (im.getpixel((x, y))[0] < 40 or is_fire(im.getpixel((x, y)), x, y))
    hole_px = {(x, y) for y in range(11, 15) for x in range(3, 13) if box(x, y)}
    strap(im, 9, 14, hole=lambda x, y: (x, y) in hole_px)
    return im

def top():
    from estilo_a import top as t
    return t()

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out + '/models', exist_ok=True)
    top().save(f'{out}/a3_top.png')
    for on in (False, True):
        s = '_on' if on else ''
        front(on).save(f'{out}/a3_front{s}.png'); side(on).save(f'{out}/a3_side{s}.png')
        json.dump({'parent': 'minecraft:block/orientable', 'textures': {'top': 'mistech:block/a3_top',
                   'front': f'mistech:block/a3_front{s}', 'side': f'mistech:block/a3_side{s}'}}, open(f'{out}/models/a3_furnace{s}.json', 'w'))
    print('ok')
