"""Aplica Colhedor + Plantador (versao A, maquina agricola). Laminas do molinete: RunicHarvesterRenderer."""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
from triturador2 import f
import agricola as a
SRC = os.path.join(os.path.dirname(__file__), '../../src/main/resources/assets/mistech')
TEX = dict(a.tex)

def blades():
    out = []
    for ang in (0, -45):   # duas pas cruzadas de lado a lado = 4 laminas (no item)
        for extra in (0, 90):
            pass
    for ang in (0, 45, -45, 90):
        pass
    for k, ang in enumerate((0, 90)):
        e = box((3 + 0.01 * k, 5.6, -5), (13 - 0.01 * k, 6.4, 1), f('#iron', (3, 5.6, -5), (13, 6.4, 1)), name='lamina')
        if ang:
            e = box((3 + 0.01 * k, 3, -2.4), (13 - 0.01 * k, 9, -1.6), f('#iron', (3, 3, -2.4), (13, 9, -1.6)), name='lamina')
        out.append(e)
    return out

harv = [e for e in a.A_harv() if e.get('name') != 'lamina']
plant = a.A_plant()
for name, els in (('runic_harvester', harv), ('runic_planter', plant)):
    for on in ('', '_on'):
        json.dump({'parent': 'minecraft:block/block', 'textures': TEX, 'elements': els}, open(f'{SRC}/models/block/{name}{on}.json', 'w'), indent=2)
json.dump({'parent': 'minecraft:block/block', 'textures': TEX, 'elements': harv + blades()}, open(f'{SRC}/models/item/runic_harvester.json', 'w'), indent=2)
json.dump({'model': {'type': 'minecraft:model', 'model': 'mistech:item/runic_harvester'}}, open(f'{SRC}/items/runic_harvester.json', 'w'), indent=2)
print('ok')
