"""Amostra v2: Extrator de Essencia como cadinho (Thaumcraft). Pedra neutra escura + cintas de cobre + runa + essencia escura."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import *
from estilo_a_lib import strap
OUT = os.path.join(os.path.dirname(__file__), 'out/extrator2')
T = OUT + '/textures'; M = OUT + '/models'
SYM = {
    'water': ["..XX..", ".XXXX.", "XXXXXX", "XXXXXX", ".XXXX."],
    'light': ["X.XX.X", ".XXXX.", "XXXXXX", ".XXXX.", "X.XX.X"],
    'dark':  ["..XXX.", ".XX...", ".XX...", ".XX...", "..XXX."],
}
EYE = [".oooo.", "oiggio", "igppgi", "oiggio", ".oooo."]
EYEC = {'o': R('0f2e2a')[0], 'i': R('2f7a5a')[0], 'g': R('7fc25a')[0], 'p': R('0a1410')[0]}

pd = V('polished_deepslate'); shade(pd, (0, 0, 15, 15), 0.9)
# parede externa: cinta de cobre em cima (linhas 1-3) e embaixo (11-12), runa no meio
def wall(lit):
    s = pd.copy()
    strap(s, 1, 3); strap(s, 11, 12, rivets=False)
    for x in range(16): shade(s, (x, 4, x, 4), 0.6); shade(s, (x, 13, x, 13), 0.65)
    return s
for name, g in SYM.items():
    for lit in (False, True):
        s = wall(lit)
        pts = [(5 + i, 5 + j) for j, r in enumerate(g) for i, c in enumerate(r) if c == 'X']
        for (x, y) in pts:
            if lit: s.putpixel((x, y), ROXOS[5] if (x - 1, y) not in pts or (x, y - 1) not in pts else ROXOS[4])
            else: s.putpixel((x, y), ARDOSIA[1])
        save(s, f'{T}/s2_crucible_{name}{"_on" if lit else ""}.png')
for lit in (False, True):
    s = wall(lit)
    # moldura de obsidiana em volta do olho
    for (x, y) in [(x, 4) for x in range(5, 11)] + [(x, 10) for x in range(5, 11)] + [(4, y) for y in range(5, 10)] + [(11, y) for y in range(5, 10)]:
        s.putpixel((x, y), R('100c16')[0])   # engaste escuro do olho
    for j, r in enumerate(EYE):
        for i, c in enumerate(r):
            if c != '.':
                col = EYEC[c]
                if lit and c in 'ig': col = tuple(min(255, int(v * 1.35)) for v in col[:3]) + (255,)
                s.putpixel((5 + i, 5 + j), col)
    if lit: s.putpixel((6, 6), (200, 255, 190, 255))
    save(s, f'{T}/s2_crucible_eye{"_on" if lit else ""}.png')
obs = V('obsidian'); save(obs, f'{T}/s2_obsidian.png')
po = V('obsidian')
for x in range(16): shade(po, (x, 0, x, 0), 1.9)
for y in range(16): shade(po, (0, y, 0, y), 1.5)
save(po, f'{T}/s2_obsidian_edge.png')
inner = pd.copy(); shade(inner, (0, 0, 15, 15), 0.55); save(inner, f'{T}/s2_crucible_inner.png')
rim = blank()
for y in range(16):
    for x in range(16):
        rim.putpixel((x, y), COBRE[5] if y == 0 or x == 0 else COBRE[2] if y == 15 or x == 15 else COBRE[4] if (x * 5 + y * 3) % 11 else COBRE[3])
for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)): rim.putpixel((x, y), LATAO[5])
save(rim, f'{T}/s2_crucible_rim.png')

ESS = R('120a1c', '1c1030', '281646', '3a1f66')
ess = blank()
for y in range(16):
    for x in range(16):
        k = ((x * 7 + y * 13) % 17) // 5
        c = ESS[min(3, k)]
        if (x * 11 + y * 5) % 37 == 0: c = ROXOS[4]
        ess.putpixel((x, y), c)
save(ess, f'{T}/s2_essence_dark.png')

def model(level, lit):
    on = '_on' if lit else ''
    els = []
    for (x, z) in ((1, 1), (12, 1), (1, 12), (12, 12)):
        els.append(box((x, 0, z), (x + 3, 2, z + 3), all_faces('#obsidian'), name='pe'))
    els.append(box((1, 2, 1), (15, 3, 15), all_faces('#inner'), name='fundo'))
    # paredes (face externa com a textura da cinta, interna escura)
    walls = [((1, 2, 1), (15, 14, 3)), ((1, 2, 13), (15, 14, 15)), ((1, 2, 3), (3, 14, 13)), ((13, 2, 3), (15, 14, 13))]
    for fr, to in walls:
        f = {}
        for d in ('north', 'south', 'east', 'west'):
            outer = (d == 'north' and fr[2] == 1) or (d == 'south' and to[2] == 15) or (d == 'west' and fr[0] == 1) or (d == 'east' and to[0] == 15)
            side = {'north': '#eye', 'south': '#water', 'east': '#light', 'west': '#dark'}[d] + on
            f[d] = {'texture': side if outer else '#inner', 'uv': [fr[0] if d in ('north', 'south') else fr[2], 2, to[0] if d in ('north', 'south') else to[2], 14]}
        f['up'] = {'texture': '#inner'}
        els.append(box(fr, to, f, name='parede'))
    # aro de cobre por cima, um pouco mais largo
    # colunas de obsidiana nos cantos
    for (x, z) in ((0.5, 0.5), (13, 0.5), (0.5, 13), (13, 13)):
        els.append(box((x, 2, z), (x + 2.5, 14, z + 2.5), all_faces('#obsidian'), name='coluna'))
    # aro de obsidiana com quina polida, por cima de tudo
    for fr, to in [((0, 14, 0), (16, 15.5, 2.5)), ((0, 14, 13.5), (16, 15.5, 16)), ((0, 14, 2.5), (2.5, 15.5, 13.5)), ((13.5, 14, 2.5), (16, 15.5, 13.5))]:
        els.append(box(fr, to, {**all_faces('#obsidian_edge'), 'up': {'texture': '#obsidian'}}, name='aro'))
    if level:
        h = 3 + level * 3.5
        els.append(box((3, 3, 3), (13, h, 13), {'up': {'texture': '#essence'}}, name='essencia'))
    return els

tex = {**{k + s: f'mistech:block/s2_crucible_{k}{s}' for k in ('eye', 'water', 'light', 'dark') for s in ('', '_on')}, 'inner': 'mistech:block/s2_crucible_inner',
       'obsidian': 'mistech:block/s2_obsidian', 'obsidian_edge': 'mistech:block/s2_obsidian_edge', 'essence': 'mistech:block/s2_essence_dark',
       'particle': 'mistech:block/s2_obsidian'}
os.makedirs(M, exist_ok=True)
for lv in range(4):
    for lit in (False, True):
        json.dump({'parent': 'minecraft:block/block', 'textures': tex, 'elements': model(lv, lit)},
                  open(f'{M}/s2_crucible_l{lv}{"_on" if lit else ""}.json', 'w'), indent=1)
print('ok')
