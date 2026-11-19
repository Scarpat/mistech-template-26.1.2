"""Amostra: Extrator de Essencia, lado Thaumcraft. Pedra arcana + latao + jarro de vidro + cristal flutuante."""
import sys, os, json, math
sys.path.insert(0, os.path.dirname(__file__))
from lib import *
from estilo_a_lib import rune_at
OUT = os.path.join(os.path.dirname(__file__), 'out/extrator')
T = OUT + '/textures'; M = OUT + '/models'

bricks = remap(V('deepslate_bricks'), ARCANA); save(bricks, f'{T}/s2_arcane_bricks.png')
pol = remap(V('polished_deepslate'), ARCANA[1:7]); save(pol, f'{T}/s2_arcane_polished.png')
# coluna: pedra polida com cinta de latao em cima e embaixo + glifo
for lit in (False, True):
    c = pol.copy()
    for y, ramp_t in ((0, (6, 5, 3)), (14, (5, 4, 2))):
        for x in range(16):
            for k in range(2):
                c.putpixel((x, y + k), LATAO[ramp_t[k]])
    for x in (2, 7, 12): c.putpixel((x, 1), LATAO[6]); c.putpixel((x, 15), LATAO[3])
    for (x, y) in rune_at('ingwaz', 7.5, 7.5):
        c.putpixel((x, y), ROXOS[5] if lit else ARCANA[0])
        if not lit and y + 1 < 14: pass
    save(c, f'{T}/s2_extractor_column{"_on" if lit else ""}.png')
# tampo de latao do pedestal
cap = blank()
for y in range(16):
    for x in range(16):
        e = min(x, y, 15 - x, 15 - y)
        cap.putpixel((x, y), LATAO[4] if e == 0 and (x == 0 or y == 0) else LATAO[1] if e == 0 else LATAO[3] if (x * 3 + y) % 9 else LATAO[2])
save(cap, f'{T}/s2_brass.png')
# vidro levemente roxo
g = blank()
for y in range(16):
    for x in range(16):
        edge = x in (0, 15) or y in (0, 15)
        streak = (x - y) == 6 and 7 < x < 13
        g.putpixel((x, y), (215, 205, 245, 210) if edge else (235, 230, 255, 150) if streak else (190, 170, 230, 40))
save(g, f'{T}/s2_jar_glass.png')
flu = blank()
for y in range(16):
    for x in range(16):
        k = 4 - (y * 4) // 16
        if (x * 5 + y * 3) % 13 == 0: k = min(6, k + 1)
        flu.putpixel((x, y), ROXOS[k])
save(flu, f'{T}/s2_essence.png')
fl_top = blank()
for y in range(16):
    for x in range(16): fl_top.putpixel((x, y), ROXOS[3] if (x + 2 * y) % 7 else ROXOS[5])
save(fl_top, f'{T}/s2_essence_top.png')
cry = blank()
for y in range(16):
    for x in range(16):
        cry.putpixel((x, y), (255, 250, 255, 255) if x % 8 == 0 and y % 5 == 1 else ROXOS[6] if x % 8 < 2 else ROXOS[5] if x % 8 < 5 else ROXOS[4])
save(cry, f'{T}/s2_crystal.png')

def ef(tex, **uv):
    return {d: ({'texture': tex, 'uv': uv[d]} if d in uv else {'texture': tex}) for d in ('north', 'south', 'east', 'west', 'up', 'down')}

def body(lit, crystal_ang=None, bob=0.0):
    col = '#col_on' if lit else '#col'
    els = [
        box((0, 0, 0), (16, 3, 16), ef('#bricks', north=[0, 13, 16, 16], south=[0, 13, 16, 16], east=[0, 13, 16, 16], west=[0, 13, 16, 16]), name='plinto'),
        box((1, 3, 1), (15, 3.5, 15), ef('#brass'), name='friso'),
        box((2, 3.5, 2), (14, 4, 14), ef('#polished'), name='tampo'),
        box((3, 4, 3), (13, 9, 13), {**ef(col, north=[3, 0, 13, 16], south=[3, 0, 13, 16], east=[3, 0, 13, 16], west=[3, 0, 13, 16]), 'up': {'texture': '#brass'}}, name='coluna'),
        box((4, 9, 4), (12, 15, 12), {d: {'texture': '#glass'} for d in ('north', 'south', 'east', 'west')}, name='jarro'),
        box((4.5, 9, 4.5), (11.5, 11, 11.5), {**ef('#essence'), 'up': {'texture': '#essence_top'}}, name='essencia'),
        box((3.5, 15, 3.5), (12.5, 15.5, 12.5), ef('#brass'), name='aro'),
        box((5, 15.5, 5), (11, 16, 11), ef('#polished'), name='tampa'),
        box((7, 16, 7), (9, 16.5, 9), ef('#brass'), name='pegador'),
    ]
    # pilares de latao nos cantos do jarro (estilo alambique)
    for (x, z) in ((3.5, 3.5), (11.5, 3.5), (3.5, 11.5), (11.5, 11.5)):
        els.append(box((x, 9, z), (x + 1, 15, z + 1), ef('#brass'), name='pilar'))
    if crystal_ang is not None:
        y0 = 11.5 + bob
        els.append(box((6.75, y0, 6.75), (9.25, y0 + 3.5, 9.25), ef('#crystal'), rot={'origin': [8, 12, 8], 'axis': 'y', 'angle': crystal_ang}, name='cristal'))
    return els

tex = {'bricks': 'mistech:block/s2_arcane_bricks', 'brass': 'mistech:block/s2_brass', 'col': 'mistech:block/s2_extractor_column',
       'col_on': 'mistech:block/s2_extractor_column_on', 'glass': 'mistech:block/s2_jar_glass', 'essence': 'mistech:block/s2_essence',
       'crystal': 'mistech:block/s2_crystal', 'essence_top': 'mistech:block/s2_essence_top', 'polished': 'mistech:block/s2_arcane_polished', 'particle': 'mistech:block/s2_arcane_bricks'}
os.makedirs(M, exist_ok=True)
json.dump({'parent': 'minecraft:block/block', 'textures': tex, 'elements': body(False)}, open(f'{M}/s2_extractor.json', 'w'), indent=1)
json.dump({'parent': 'minecraft:block/block', 'textures': tex, 'elements': body(True)}, open(f'{M}/s2_extractor_on.json', 'w'), indent=1)
json.dump({'textures': tex, 'elements': body(False, 0)}, open(f'{M}/s2_extractor_prev_off.json', 'w'), indent=1)
for i in range(8):
    a = -45 + i * 11.25
    json.dump({'textures': tex, 'elements': body(True, a, 0.5 * math.sin(i / 8 * 2 * math.pi))}, open(f'{M}/s2_extractor_prev{i}.json', 'w'), indent=1)
print('ok')
