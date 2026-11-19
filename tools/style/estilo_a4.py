"""Estilo A4: cinta de cobre LISA (sem o ruído da pedra por baixo), uma runa grande e centralizada por lateral.
Runa com haste de 2 px para ficar no centro exato do bloco (16 px)."""
import os, sys, json
from estilos4 import V, is_fire
from kit import COBRE, AMETISTA, LATAO

ALGIZ = ["X.XX.X",
         ".XXXX.",
         "..XX..",
         "..XX..",
         "..XX.."]
Y0, Y1 = 8, 14          # cinta
STRAP = {8: 5, 9: 4, 10: 4, 11: 3, 12: 3, 13: 3, 14: 1}
SHINE = {(3, 9), (4, 9), (12, 10), (2, 12), (13, 12)}   # poucos brilhos postos à mão

def strap(im, hole=lambda x, y: False):
    for y in range(Y0, Y1 + 1):
        for x in range(16):
            if hole(x, y): continue
            t = STRAP[y]
            if (x, y) in SHINE: t += 1
            im.putpixel((x, y), COBRE[t])
    for x in (1, 14):
        im.putpixel((x, 11), LATAO[5]); im.putpixel((x, 12), COBRE[1])

def rune(im, on, x0=5, y0=9):
    for j, r in enumerate(ALGIZ):
        for i, c in enumerate(r):
            if c == 'X':
                im.putpixel((x0 + i, y0 + j), AMETISTA[6 if (on and 2 <= i <= 3) else 5] if on else COBRE[1])
    if on:   # leve brilho no cobre em volta da runa
        for y in range(Y0 + 1, Y1):
            for x in range(x0 - 1, x0 + 7):
                p = im.getpixel((x, y))
                if p[:3] in [c[:3] for c in COBRE]:
                    im.putpixel((x, y), tuple(int(p[k] * 0.8 + AMETISTA[4][k] * 0.2) for k in range(3)) + (255,))

def side(on):
    im = V('furnace_side'); strap(im); rune(im, on); return im

def front(on):
    im = V('furnace_front_on' if on else 'furnace_front')
    ref = V('furnace_front')
    hole = lambda x, y: 3 <= x <= 12 and 11 <= y <= 14 and (ref.getpixel((x, y))[0] < 40)
    strap(im, hole)
    for x in range(3, 13):          # aro de cobre em volta da boca do fogo
        if not hole(x, 10): im.putpixel((x, 10), COBRE[2])
    return im

if __name__ == '__main__':
    from estilo_a import top
    out = sys.argv[1]; os.makedirs(out + '/models', exist_ok=True)
    top().save(f'{out}/a4_top.png')
    for on in (False, True):
        s = '_on' if on else ''
        front(on).save(f'{out}/a4_front{s}.png'); side(on).save(f'{out}/a4_side{s}.png')
        json.dump({'parent': 'minecraft:block/orientable', 'textures': {'top': 'mistech:block/a4_top',
                   'front': f'mistech:block/a4_front{s}', 'side': f'mistech:block/a4_side{s}'}}, open(f'{out}/models/a4_furnace{s}.json', 'w'))
    print('ok')
