"""Extrator v4: cadinho com bojo (degraus), obsidiana com degrade e reflexo, aro de cobre fino, olho e runas entalhados pequenos."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import *
VAR = sys.argv[1] if len(sys.argv) > 1 else ''
OUT = os.path.join(os.path.dirname(__file__), 'out/extrator4' + VAR)
T = OUT + '/textures'; M = OUT + '/models'
OBS = R('07050c', '0e0a17', '161022', '1e162e', '281d3c', '35264e', '4a3668', '6a5290')
ob = V('obsidian')

def obsidian(glyph=None, lit=False, eye=False):
    """Obsidiana: veio vanilla suavizado + degrade vertical (luz de cima) + reflexo vertical em x=3..4."""
    s = blank()
    if VAR == 'v':
        s = ob.copy()
    for y in (range(16) if VAR != 'v' else ()):
        for x in range(16):
            n = lum(ob.getpixel((x, y)))
            base = 3 - (y * 3) // 16                    # 3 em cima .. 1 embaixo
            t = base + (1 if n > 42 else 0) + (1 if n > 75 else 0) - (1 if n < 10 and y > 4 else 0)
            if x in (2, 3) and 3 < y < 12: t += 1 + (x == 2)   # reflexo vitreo
            s.putpixel((x, y), OBS[max(0, min(7, t))])
    if glyph:
        pts = [(5 + i, 6 + j) for j, r in enumerate(glyph) for i, c in enumerate(r) if c == 'X']
        for (x, y) in pts:
            s.putpixel((x, y), ROXOS[4] if lit else OBS[0])
            if not lit and (x, y + 1) not in pts: s.putpixel((x, y + 1), OBS[4] if VAR == 'v' else OBS[5])   # luz na borda de baixo do entalhe
    if eye:
        G = R('17483a', '2f7a5a', '5fae4a', '9fe07a', '0a1410', 'e0ffd0')
        BALL = [".aaaa.", "abbcba", "abppba", "abppba", "aabbaa", ".aaaa."]
        for x in range(4, 12):
            for y in range(4, 12):
                if not ((x in (4, 11)) and (y in (4, 11))): s.putpixel((x, y), OBS[0])
        for x in range(5, 11): s.putpixel((x, 12) if False else (x, 11), OBS[0])
        cmap = {'a': G[0], 'b': G[1], 'c': G[2], 'p': G[4]}
        if lit: cmap = {'a': G[1], 'b': G[2], 'c': G[3], 'p': G[4]}
        for j, r in enumerate(BALL):
            for i, c in enumerate(r):
                if c != '.': s.putpixel((5 + i, 5 + j), cmap[c])
        if lit: s.putpixel((6, 6), G[5])
    return s

GL = {'water': ["..XX..", ".X..X.", "X....X", "X....X", ".XXXX."],
      'light': ["X....X", "..XX..", ".X..X.", ".X..X.", "..XX..", "X....X"],
      'dark':  ["...XX.", "..X...", "..X...", "..X...", "...XX."]}
save(obsidian(), f'{T}/s4{VAR}_obs.png')
for lit in (False, True):
    on = '_on' if lit else ''
    save(obsidian(eye=True, lit=lit), f'{T}/s4{VAR}_eye{on}.png')
    for k, g in GL.items(): save(obsidian(g, lit), f'{T}/s4{VAR}_{k}{on}.png')
inner = obsidian(); shade(inner, (0, 0, 15, 15), 0.45); save(inner, f'{T}/s4{VAR}_inner.png')
top = blank()
for y in range(16):
    for x in range(16): top.putpixel((x, y), OBS[4] if (x + y) % 9 else OBS[5])
if VAR == 'v': top = ob.copy()
save(top, f'{T}/s4{VAR}_lip_top.png')
cu = blank()
for y in range(16):
    for x in range(16): cu.putpixel((x, y), COBRE[5] if y % 2 == 0 else COBRE[3])
for x in range(0, 16, 4): cu.putpixel((x, 0), LATAO[5])
if VAR == 'v': cu = V('cut_copper')
save(cu, f'{T}/s4{VAR}_copper.png')
ESS = R('120a1c', '1c1030', '281646', '3a1f66')
ess = blank()
for y in range(16):
    for x in range(16):
        c = ESS[min(3, ((x * 7 + y * 13) % 17) // 5)]
        if (x * 11 + y * 5) % 37 == 0: c = ROXOS[4]
        ess.putpixel((x, y), c)
save(ess, f'{T}/s4{VAR}_essence.png')

def tier(fr, to, side, top='#obs', bottom='#obs', name=None):
    f = {}
    for d in ('north', 'south', 'east', 'west'):
        f[d] = {'texture': side.get(d, '#obs') if isinstance(side, dict) else side}
        # uv: mantem o degrade continuo pelo bloco inteiro (v = 16 - y)
        u0, u1 = (fr[0], to[0]) if d in ('north', 'south') else (fr[2], to[2])
        f[d]['uv'] = [u0, 16 - to[1], u1, 16 - fr[1]]
    if top: f['up'] = {'texture': top}
    if bottom: f['down'] = {'texture': bottom}
    return box(fr, to, f, name=name)

def ring(y0, y1, a, b, w, tex, top=None):
    """Anel quadrado de a..b com parede w."""
    return [tier((a, y0, a), (b, y1, a + w), tex, top), tier((a, y0, b - w), (b, y1, b), tex, top),
            tier((a, y0, a + w), (a + w, y1, b - w), tex, top), tier((b - w, y0, a + w), (b, y1, b - w), tex, top)]

def model(level, lit):
    on = '_on' if lit else ''
    belly = {'north': '#eye' + on, 'south': '#water' + on, 'east': '#light' + on, 'west': '#dark' + on}
    els = [
        tier((3, 0, 3), (13, 1.5, 13), '#obs', name='base'),
        tier((2, 1.5, 2), (14, 3, 14), '#obs', name='pe'),
        tier((1, 3, 1), (15, 4.5, 15), '#obs', name='bojo_baixo'),
        tier((0.5, 4.5, 0.5), (15.5, 11, 15.5), belly, name='bojo'),
        tier((1, 11, 1), (15, 12, 15), '#copper', top=None, name='aro_cobre'),
    ]
    els += ring(12, 13.5, 1.5, 14.5, 2, '#obs', '#obs')          # ombro
    els += ring(13.5, 14.5, 2.5, 13.5, 1.5, '#obs', '#obs')      # gargalo
    els += ring(14.5, 16, 1.5, 14.5, 2, '#obs', '#lip')          # borda virada
    els.append(tier((3, 11.9, 3), (13, 12, 13), '#inner', top='#inner', name='fundo_interno'))
    if level:
        els.append(box((3.5, 12, 3.5), (12.5, 12 + level * 1.2, 12.5), {'up': {'texture': '#essence'}}, name='essencia'))
    return els

tex = {**{k + s: f'mistech:block/s4{VAR}_{k}{s}' for k in ('eye', 'water', 'light', 'dark') for s in ('', '_on')},
       'obs': f'mistech:block/s4{VAR}_obs', 'inner': f'mistech:block/s4{VAR}_inner', 'lip': f'mistech:block/s4{VAR}_lip_top',
       'copper': f'mistech:block/s4{VAR}_copper', 'essence': f'mistech:block/s4{VAR}_essence', 'particle': f'mistech:block/s4{VAR}_obs'}
os.makedirs(M, exist_ok=True)
for lv in range(4):
    for lit in (False, True):
        json.dump({'parent': 'minecraft:block/block', 'textures': tex, 'elements': model(lv, lit)}, open(f'{M}/s4{VAR}_l{lv}{"_on" if lit else ""}.json', 'w'), indent=1)
print('ok')
