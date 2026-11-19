"""Aplica o Triturador Runico v2 (receita: ferro, pedregulho, rebolo) no src. Rodas: renderer (RunicCrusherRenderer)."""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from lib import box, V, save, ROXOS
from triturador2 import body, wheel, f
SRC = os.path.join(os.path.dirname(__file__), '../../src/main/resources/assets/mistech')
MONTANHA = ["..XX..", ".X..X.", "X....X", "......", "XXXXXX"]
for lit in (False, True):
    im = V('iron_block')
    for j, r in enumerate(MONTANHA):
        for i, c in enumerate(r):
            if c == 'X':
                im.putpixel((5 + i, 8 + j), (ROXOS[5] if j < 2 else ROXOS[4]) if lit else (74, 74, 80, 255))
    save(im, f'{SRC}/textures/block/runic_crusher_rune_plate{"_on" if lit else ""}.png')
# placa de ferro na frente (norte), rente as colunas
plate = box((4, 2, 0), (12, 9, 0.5), {**f('#plate', (4, 2, 0), (12, 9, 0.5), ('north', 'east', 'west', 'up', 'down'))}, name='placa_runa')
plate['faces']['north']['uv'] = [4, 7, 12, 14]
TEX = {'cobble': 'minecraft:block/cobblestone', 'iron': 'minecraft:block/iron_block', 'iron_dark': 'minecraft:block/heavy_core',
       'dark': 'minecraft:block/black_concrete', 'round': 'minecraft:block/grindstone_round', 'gside': 'minecraft:block/grindstone_side',
       'particle': 'minecraft:block/cobblestone'}
for lit in (False, True):
    t = dict(TEX, plate='mistech:block/runic_crusher_rune_plate' + ('_on' if lit else ''))
    json.dump({'parent': 'minecraft:block/block', 'textures': t, 'elements': body() + [plate]},
              open(f'{SRC}/models/block/runic_crusher{"_on" if lit else ""}.json', 'w'), indent=2)
t = dict(TEX, plate='mistech:block/runic_crusher_rune_plate')
json.dump({'parent': 'minecraft:block/block', 'textures': t, 'elements': body() + [plate] + wheel(4.5, 0, 'roda_a') + wheel(11.5, 0, 'roda_b')},
          open(f'{SRC}/models/item/runic_crusher.json', 'w'), indent=2)
json.dump({'model': {'type': 'minecraft:model', 'model': 'mistech:item/runic_crusher'}}, open(f'{SRC}/items/runic_crusher.json', 'w'), indent=2)
print('ok')
