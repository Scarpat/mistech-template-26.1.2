"""Aplica o Gerador de Blaze (gaiola B1) no src. As varas giram no BlazeGeneratorRenderer."""
import json, os, sys, shutil
sys.path.insert(0, os.path.dirname(__file__))
import blaze as b
SRC = os.path.join(os.path.dirname(__file__), '../../src/main/resources/assets/mistech')
shutil.copy(os.path.join(b.T, 'blaze_generator_rod.png'), f'{SRC}/textures/block/blaze_generator_rod.png')
TEX = {'rod': 'mistech:block/blaze_generator_rod', 'bricks': 'minecraft:block/polished_blackstone_bricks', 'pb': 'minecraft:block/polished_blackstone',
       'magma': 'minecraft:block/magma', 'particle': 'minecraft:block/polished_blackstone_bricks'}
for lit in (False, True):
    on = '_on' if lit else ''
    els, _ = b.B(lit, 0, 'grade')
    els = [e for e in els if e.get('name') not in ('vara', 'nucleo')]
    json.dump({'parent': 'minecraft:block/block', 'textures': dict(TEX, grate='mistech:block/blaze_generator_front' + on), 'elements': els},
              open(f'{SRC}/models/block/blaze_generator{on}.json', 'w'), indent=2)
els, _ = b.B(False, 20, 'grade')
json.dump({'parent': 'minecraft:block/block', 'textures': dict(TEX, grate='mistech:block/blaze_generator_front'), 'elements': els},
          open(f'{SRC}/models/item/blaze_generator.json', 'w'), indent=2)
json.dump({'model': {'type': 'minecraft:model', 'model': 'mistech:item/blaze_generator'}}, open(f'{SRC}/items/blaze_generator.json', 'w'), indent=2)
print('ok')
