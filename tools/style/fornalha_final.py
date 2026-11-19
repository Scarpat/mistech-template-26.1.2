"""Fornalha Rúnica — versão final (estilo A, runa dagaz).
Base (sempre): pedra vanilla escurecida + cinta de cobre lisa que dá a volta + runa dagaz gravada.
Acesa: modelo ganha uma 2ª camada EMISSIVA (light_emission 15) só com o fogo roxo, a runa pulsando em roxos
(como prismarinho) e o brilho na boca da chaminé.
Uso: python fornalha_final.py <pasta>  -> texturas em <pasta>, modelos em <pasta>/models"""
import os, sys, json, math
from PIL import Image
from estilos4 import V, is_fire, lum
from kit import COBRE, AMETISTA, LATAO, R
import estilo_a4 as A
from estilo_a import top as top_flue

DARK = 0.78                      # pedregulho mais escuro
ROXOS = R('3b1670', '5a24a8', '7a3fd0', '9a5cf0', 'b884ff', 'd4aaff', 'efdcff')
PRISMA = ['5a24a8', '7a3fd0', '9a5cf0', 'b884ff', 'd4aaff', 'b884ff', '9a5cf0', 'c070f0', 'e08cff', 'c070f0', '9a5cf0', '7a3fd0']
PRISMA = [tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,) for c in PRISMA]
DAGAZ = (5, 9, ["X...X", "XX.XX", "X.X.X", "XX.XX", "X...X"])
CLEAR = (0, 0, 0, 0)

def darken(im, keep):
    for y in range(16):
        for x in range(16):
            if (x, y) in keep: continue
            p = im.getpixel((x, y))
            if p[3] and abs(p[0] - p[1]) < 12 and abs(p[1] - p[2]) < 12:     # só cinza (pedra)
                im.putpixel((x, y), (int(p[0] * DARK), int(p[1] * DARK), int(p[2] * DARK * 1.04), 255))
    return im

def rune_pixels():
    x0, y0, g = DAGAZ
    return [(x0 + i, y0 + j) for j, r in enumerate(g) for i, c in enumerate(r) if c == 'X']

def side():
    im = V('furnace_side'); darken(im, set()); A.strap(im)
    for (x, y) in rune_pixels(): im.putpixel((x, y), COBRE[1])
    return im

def front():
    im = V('furnace_front'); darken(im, set())
    ref = V('furnace_front')
    hole = lambda x, y: 3 <= x <= 12 and 11 <= y <= 14 and ref.getpixel((x, y))[0] < 40
    A.strap(im, hole)
    for x in range(3, 13):
        if not hole(x, 10): im.putpixel((x, 10), COBRE[2])
    return im

def top():
    im = top_flue(); keep = {(x, y) for y in range(5, 11) for x in range(5, 11)}
    return darken(im, keep)

# ---------------- camadas emissivas (animadas) ----------------
def glow_side(n=12):
    frames = []
    rp = rune_pixels(); rs = set(rp)
    halo = {(x + dx, y + dy) for (x, y) in rp for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))} - rs
    halo = {(x, y) for (x, y) in halo if 9 <= y <= 13}
    for f in range(n):
        im = Image.new('RGBA', (16, 16), CLEAR)
        for (x, y) in rp:
            k = int(f + x * 0.6 + y * 0.4) % len(PRISMA)
            im.putpixel((x, y), PRISMA[k])
        a = 0.35 + 0.15 * math.sin(2 * math.pi * f / n)
        for (x, y) in ():     # (sem halo: a runa sozinha brilha)
            c = COBRE[3]; p = AMETISTA[4]
            im.putpixel((x, y), tuple(int(c[i] * (1 - a) + p[i] * a) for i in range(3)) + (255,))
        frames.append(im)
    return frames

def glow_front(n=8):
    base = V('furnace_front_on')
    fire = [(x, y) for y in range(16) for x in range(16) if is_fire(base.getpixel((x, y)), x, y)]
    ls = [lum(base.getpixel(p)) for p in fire]; lo, hi = min(ls), max(ls)
    frames = []
    for f in range(n):
        im = Image.new('RGBA', (16, 16), CLEAR)
        for (x, y), l in zip(fire, ls):
            k = (l - lo) / (hi - lo) * 6
            k = k + 0.9 * math.sin(2 * math.pi * f / n + x * 0.9 + y * 0.5)
            im.putpixel((x, y), ROXOS[max(0, min(6, int(round(k))))])
        for x in ():
            if (x, 10) not in fire:
                c = COBRE[3]; p = AMETISTA[4]; a = 0.45
                im.putpixel((x, 10), tuple(int(c[i] * (1 - a) + p[i] * a) for i in range(3)) + (255,))
        frames.append(im)
    return frames

def glow_top(n=8):
    frames = []
    for f in range(n):
        im = Image.new('RGBA', (16, 16), CLEAR)
        a = 0.5 + 0.5 * math.sin(2 * math.pi * f / n)
        for y in range(6, 10):
            for x in range(6, 10):
                center = 7 <= x <= 8 and 7 <= y <= 8
                im.putpixel((x, y), ROXOS[(4 if center else 2) + (1 if a > 0.5 else 0)])
        frames.append(im)
    return frames

def strip(frames):
    out = Image.new('RGBA', (16, 16 * len(frames)), CLEAR)
    for i, f in enumerate(frames): out.paste(f, (0, 16 * i))
    return out

def model(on):
    T = lambda n: 'mistech:block/runic_furnace_' + n
    tex = {'particle': T('front'), 'front': T('front'), 'side': T('side'), 'top': T('top')}
    els = [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {
        'north': {'texture': '#front', 'cullface': 'north'}, 'south': {'texture': '#side', 'cullface': 'south'},
        'east': {'texture': '#side', 'cullface': 'east'}, 'west': {'texture': '#side', 'cullface': 'west'},
        'up': {'texture': '#top', 'cullface': 'up'}, 'down': {'texture': '#top', 'cullface': 'down'}}}]
    if on:
        tex.update({'glow_front': T('glow_front'), 'glow_side': T('glow_side'), 'glow_top': T('glow_top')})
        els.append({'from': [-0.01, -0.01, -0.01], 'to': [16.01, 16.01, 16.01], 'shade': False, 'light_emission': 15, 'faces': {
            'north': {'texture': '#glow_front', 'cullface': 'north'}, 'south': {'texture': '#glow_side', 'cullface': 'south'},
            'east': {'texture': '#glow_side', 'cullface': 'east'}, 'west': {'texture': '#glow_side', 'cullface': 'west'},
            'up': {'texture': '#glow_top', 'cullface': 'up'}}})
    return {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': els}

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out + '/models', exist_ok=True)
    front().save(f'{out}/runic_furnace_front.png'); side().save(f'{out}/runic_furnace_side.png'); top().save(f'{out}/runic_furnace_top.png')
    for name, fr, ft in (('glow_side', glow_side(), 4), ('glow_front', glow_front(), 3), ('glow_top', glow_top(), 4)):
        strip(fr).save(f'{out}/runic_furnace_{name}.png')
        json.dump({'animation': {'frametime': ft, 'interpolate': True}}, open(f'{out}/runic_furnace_{name}.png.mcmeta', 'w'))
    json.dump(model(False), open(f'{out}/models/runic_furnace.json', 'w'), indent=1)
    json.dump(model(True), open(f'{out}/models/runic_furnace_on.json', 'w'), indent=1)
    print('ok')
