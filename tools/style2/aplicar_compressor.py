"""Aplica o Compressor de Magia (prensa) no src. Cabecote, ponta de ametista e corrente: MagicCompressorRenderer."""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
import compressor as c
SRC = os.path.join(os.path.dirname(__file__), '../../src/main/resources/assets/mistech/models')
TEX = {'pb': 'minecraft:block/polished_blackstone', 'bricks': 'minecraft:block/polished_blackstone_bricks',
       'chis': 'minecraft:block/chiseled_polished_blackstone', 'amethyst': 'minecraft:block/amethyst_block',
       'chain': 'minecraft:block/iron_chain', 'dark': 'minecraft:block/polished_blackstone', 'ess': 'mistech:block/impure_essence_still',
       'particle': 'minecraft:block/polished_blackstone'}
json.dump({'parent': 'minecraft:block/block', 'textures': TEX, 'elements': c.body(False)}, open(f'{SRC}/block/magic_compressor.json', 'w'), indent=2)
json.dump({'parent': 'minecraft:block/block', 'textures': TEX, 'elements': c.body(True)}, open(f'{SRC}/block/magic_compressor_filled.json', 'w'), indent=2)
json.dump({'parent': 'minecraft:block/block', 'textures': TEX, 'elements': c.body(False) + c.head(c.TOP)}, open(f'{SRC}/item/magic_compressor.json', 'w'), indent=2)
json.dump({'model': {'type': 'minecraft:model', 'model': 'mistech:item/magic_compressor'}}, open(f'{SRC}/../items/magic_compressor.json', 'w'), indent=2)
print('ok')
