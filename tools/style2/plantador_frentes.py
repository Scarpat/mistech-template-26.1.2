"""Plantador: tres frentes no lugar da boca de dispenser (corpo aprovado da versao A)."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
from triturador2 import f
import agricola as a
M = a.M
B = a.B

def base():
    return [e for e in a.A_plant() if e.get('name') != 'boca_dispenser']

def P1():
    els = base()
    els.append(B((5, 3, 0.5), (11, 9, 1), '#copper', 'placa'))
    els.append(B((6.5, 4.5, -2), (9.5, 7.5, 0.5), '#copper', 'bico', north={'texture': '#dark', 'uv': [7, 5, 9, 7]}))
    els.append(B((7, 5, -3), (9, 7, -2), '#copper', 'ponta', north={'texture': '#dark'}))
    return els

def P2():
    els = base()
    # calha inclinada: do funil (y 10) ate a frente embaixo
    for i in range(5):
        y = 9 - i * 1.6; z = 1 - i * 0.9
        els.append(B((5, y - 1, z - 1), (11, y, z + 0.5), '#copper', 'calha', up={'texture': '#compost', 'uv': [5, 0, 11, 2]}))
        els.append(B((4.5, y - 1, z - 1), (5, y + 0.8, z + 0.5), '#copper', 'borda_calha'))
        els.append(B((11, y - 1, z - 1), (11.5, y + 0.8, z + 0.5), '#copper', 'borda_calha'))
    return els

def P3():
    els = base()
    els.append(box((3, 2, 0.95), (13, 9, 1), {'north': {'texture': '#compost', 'uv': [3, 5, 13, 12]}}, name='sementes'))
    els.append(B((3, 2, 0.5), (13, 9, 0.9), '#grate', 'grade', north={'texture': '#grate', 'uv': [3, 5, 13, 12]}))
    return els

tex = dict(a.tex, dark='minecraft:block/black_concrete', grate='minecraft:block/copper_grate')
for n, fn in (('1', P1), ('2', P2), ('3', P3)):
    json.dump({'textures': tex, 'elements': fn()}, open(f'{M}/pf{n}.json', 'w'), indent=1)
print('ok')
