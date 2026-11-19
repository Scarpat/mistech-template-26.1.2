"""Extrator v3: o corpo inteiro e obsidiana polida (mesma estrutura da ardosia polida, paleta da obsidiana).
Variante A: cintas e aro de cobre (familia). Variante B: tudo obsidiana, cintas com veio roxo (obsidiana chorosa)."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import *
from estilo_a_lib import strap
OUT = os.path.join(os.path.dirname(__file__), 'out/extrator3')
T = OUT + '/textures'; M = OUT + '/models'
OBS = R('07050c', '0e0a17', '161022', '1e162e', '281d3c', '35264e', '4a3668')
SYM = {'water': ["..XX..", ".XXXX.", "XXXXXX", "XXXXXX", ".XXXX."],
       'light': ["X.XX.X", ".XXXX.", "XXXXXX", ".XXXX.", "X.XX.X"],
       'dark':  ["..XXX.", ".XX...", ".XX...", ".XX...", "..XXX."]}
EYE = [".oooo.", "oiggio", "igppgi", "oiggio", ".oooo."]
EYEC = {'o': R('0f2e2a')[0], 'i': R('2f7a5a')[0], 'g': R('7fc25a')[0], 'p': R('0a1410')[0]}

pol = remap(V('polished_deepslate'), OBS[1:6])
ob = V('obsidian')
for y in range(16):          # brilhos da obsidiana vanilla (so os pixels roxos mais claros) para manter a identidade
    for x in range(16):
        p = ob.getpixel((x, y))
        if lum(p) > 60: pol.putpixel((x, y), OBS[6])
save(pol, f'{T}/s3_obsidian_polished.png')
inner = pol.copy(); shade(inner, (0, 0, 15, 15), 0.6); save(inner, f'{T}/s3_inner.png')
dark = remap(V('polished_deepslate'), OBS[0:4]); save(dark, f'{T}/s3_obsidian_dark.png')

def band(s, y0, y1, var):
    if var == 'A':
        strap(s, y0, y1, rivets=(y1 - y0 > 1))
    else:
        for y in range(y0, y1 + 1):
            for x in range(16):
                s.putpixel((x, y), OBS[5] if y == y0 else OBS[1] if y == y1 else OBS[3])
        yv = (y0 + y1 + 1) // 2 if y1 > y0 else y0
        for x in range(16):
            if x % 5 != 4: s.putpixel((x, yv), ROXOS[1])
    for x in range(16): shade(s, (x, y1 + 1, x, y1 + 1), 0.6)

def wall(var, kind, lit):
    s = pol.copy(); band(s, 1, 3, var); band(s, 11, 12, var)
    if kind == 'eye':
        for (x, y) in [(x, 4) for x in range(5, 11)] + [(x, 10) for x in range(5, 11)] + [(4, y) for y in range(5, 10)] + [(11, y) for y in range(5, 10)]:
            s.putpixel((x, y), OBS[0])
        for j, r in enumerate(EYE):
            for i, c in enumerate(r):
                if c != '.':
                    col = EYEC[c]
                    if lit and c in 'ig': col = tuple(min(255, int(v * 1.35)) for v in col[:3]) + (255,)
                    s.putpixel((5 + i, 5 + j), col)
        if lit: s.putpixel((6, 6), (200, 255, 190, 255))
    else:
        g = SYM[kind]; pts = [(5 + i, 5 + j) for j, r in enumerate(g) for i, c in enumerate(r) if c == 'X']
        for (x, y) in pts:
            s.putpixel((x, y), (ROXOS[5] if (x - 1, y) not in pts or (x, y - 1) not in pts else ROXOS[4]) if lit else OBS[0])
    return s

rimA = blank()
for y in range(16):
    for x in range(16):
        rimA.putpixel((x, y), COBRE[5] if y == 0 or x == 0 else COBRE[2] if y == 15 or x == 15 else COBRE[4] if (x * 5 + y * 3) % 11 else COBRE[3])
save(rimA, f'{T}/s3_rim_A.png')
rimB = pol.copy()
for x in range(16): rimB.putpixel((x, 0), OBS[6]); rimB.putpixel((x, 15), OBS[1])
for y in range(16): rimB.putpixel((0, y), OBS[5])
save(rimB, f'{T}/s3_rim_B.png')
ESS = R('120a1c', '1c1030', '281646', '3a1f66')
ess = blank()
for y in range(16):
    for x in range(16):
        c = ESS[min(3, ((x * 7 + y * 13) % 17) // 5)]
        if (x * 11 + y * 5) % 37 == 0: c = ROXOS[4]
        ess.putpixel((x, y), c)
save(ess, f'{T}/s3_essence.png')
for var in 'AB':
    for kind in ('eye', 'water', 'light', 'dark'):
        for lit in (False, True):
            save(wall(var, kind, lit), f'{T}/s3_{var}_{kind}{"_on" if lit else ""}.png')

def model(var, level, lit):
    on = '_on' if lit else ''
    els = [box((x, 0, z), (x + 3, 2, z + 3), all_faces('#dark'), name='pe') for (x, z) in ((1, 1), (12, 1), (1, 12), (12, 12))]
    els.append(box((1, 2, 1), (15, 3, 15), all_faces('#inner'), name='fundo'))
    for fr, to in [((1, 2, 1), (15, 14, 3)), ((1, 2, 13), (15, 14, 15)), ((1, 2, 3), (3, 14, 13)), ((13, 2, 3), (15, 14, 13))]:
        f = {}
        for d in ('north', 'south', 'east', 'west'):
            outer = (d == 'north' and fr[2] == 1) or (d == 'south' and to[2] == 15) or (d == 'west' and fr[0] == 1) or (d == 'east' and to[0] == 15)
            side = {'north': '#eye', 'south': '#water', 'east': '#light', 'west': '#dark_rune'}[d] + on
            f[d] = {'texture': side if outer else '#inner', 'uv': [fr[0] if d in ('north', 'south') else fr[2], 2, to[0] if d in ('north', 'south') else to[2], 14]}
        f['up'] = {'texture': '#inner'}
        els.append(box(fr, to, f, name='parede'))
    for fr, to in [((0.5, 14, 0.5), (15.5, 15, 3)), ((0.5, 14, 13), (15.5, 15, 15.5)), ((0.5, 14, 3), (3, 15, 13)), ((13, 14, 3), (15.5, 15, 13))]:
        els.append(box(fr, to, all_faces('#rim'), name='aro'))
    if level:
        els.append(box((3, 3, 3), (13, 3 + level * 3.5, 13), {'up': {'texture': '#essence'}}, name='essencia'))
    return els

os.makedirs(M, exist_ok=True)
for var in 'AB':
    tex = {**{k + s: f'mistech:block/s3_{var}_{k}{s}' for k in ('eye', 'water', 'light') for s in ('', '_on')},
           'dark_rune': f'mistech:block/s3_{var}_dark', 'dark_rune_on': f'mistech:block/s3_{var}_dark_on',
           'inner': 'mistech:block/s3_inner', 'dark': 'mistech:block/s3_obsidian_dark', 'rim': f'mistech:block/s3_rim_{var}',
           'essence': 'mistech:block/s3_essence', 'particle': 'mistech:block/s3_obsidian_polished'}
    for lv in range(4):
        for lit in (False, True):
            json.dump({'parent': 'minecraft:block/block', 'textures': tex, 'elements': model(var, lv, lit)},
                      open(f'{M}/s3_{var}_l{lv}{"_on" if lit else ""}.json', 'w'), indent=1)
print('ok')
