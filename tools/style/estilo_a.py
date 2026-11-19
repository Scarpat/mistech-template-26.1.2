"""Estilo A refinado (Vanilla+ rúnico): a magia fica PRESA no cobre, nada solto.
A1 = gema de ametista engastada na cinta de cobre (acende com o fogo)
A2 = runas gravadas na cinta de cobre (escuras apagada, brilham violeta acesa)
Topo nos dois: pedra vanilla com uma única chaminé de cobre (anel + boca escura)."""
import os, sys, json
from estilos4 import V, recolor, not_dark, is_fire
from kit import COBRE, AMETISTA

def band(im, box):
    return recolor(im, COBRE, box, pred=lambda p, x, y: not_dark(p, x, y) and not is_fire(p, x, y))

def gem(im, x, y, on):
    for (dx, dy) in ((-1, 0), (2, 0), (-1, 1), (2, 1)): im.putpixel((x + dx, y + dy), COBRE[1])
    t = [6, 5, 5, 4] if on else [5, 4, 3, 2]
    for (dx, dy), k in zip(((0, 0), (1, 0), (0, 1), (1, 1)), t): im.putpixel((x + dx, y + dy), AMETISTA[k])

RUNES = {  # posições gravadas na cinta (coluna, linha)
    'front': [(4, 10), (5, 10), (10, 10), (11, 10)],
    'side': [(3, 11), (4, 10), (5, 11), (7, 10), (7, 11), (8, 11), (10, 10), (11, 11), (12, 10)],
}
def runes(im, key, on):
    for (x, y) in RUNES[key]: im.putpixel((x, y), AMETISTA[5] if on else COBRE[1])

def front(var, on):
    im = V('furnace_front_on' if on else 'furnace_front')
    band(im, (1, 9, 15, 15))
    if var == 1: gem(im, 7, 9, on)
    else: runes(im, 'front', on)
    return im

def side(var, on=False):
    im = V('furnace_side'); band(im, (1, 9, 15, 14))
    if var == 1: gem(im, 7, 10, on)
    else: runes(im, 'side', on)
    return im

def top():
    im = V('furnace_top')
    ring = [(6, 5), (7, 5), (8, 5), (9, 5), (5, 6), (10, 6), (5, 7), (10, 7), (5, 8), (10, 8), (5, 9), (10, 9), (6, 10), (7, 10), (8, 10), (9, 10)]
    for (x, y) in ring: im.putpixel((x, y), COBRE[5] if (y < 7 or x == 5) else COBRE[3])
    for y in range(6, 10):
        for x in range(6, 10): im.putpixel((x, y), (17, 17, 17, 255) if 7 <= x <= 8 and 7 <= y <= 8 else (33, 33, 33, 255))
    for (x, y) in ((6, 6), (7, 6), (8, 6), (6, 7), (6, 8)): im.putpixel((x, y), COBRE[1])
    return im

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out + '/models', exist_ok=True)
    top().save(f'{out}/a_top.png')
    for v in (1, 2):
        front(v, False).save(f'{out}/a{v}_front.png'); front(v, True).save(f'{out}/a{v}_front_on.png')
        side(v).save(f'{out}/a{v}_side.png'); side(v, True).save(f'{out}/a{v}_side_on.png')
        for on in (False, True):
            sfx = '_on' if on else ''
            json.dump({'parent': 'minecraft:block/orientable', 'textures': {'top': 'mistech:block/a_top',
                       'front': f'mistech:block/a{v}_front{sfx}', 'side': f'mistech:block/a{v}_side{sfx}'}},
                      open(f'{out}/models/a{v}_furnace{sfx}.json', 'w'))
    print('ok')
