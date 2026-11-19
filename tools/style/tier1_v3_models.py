"""Modelos v3 do Tier I: silhueta conta a função. Frente = norte (z=0).
Uso: python tier1_v3_models.py <pasta_saida>  (gera texturas em <pasta> e modelos em <pasta>/models)"""
import os, sys
from modelkit import Model
import tier1_v3_tex as TX

T = lambda n: 'mistech:block/' + n


def furnace(lit):
    m = Model({'particle': T('t1s_bricks'), 'bricks': T('t1s_bricks'), 'base': T('t1s_stone_dark'), 'stone': T('t1s_stone'),
               'copper': T('t1s_copper'), 'mouth': T('t1s_fire' if lit else 't1s_dark'),
               'gem': T('t1s_amethyst_on' if lit else 't1s_amethyst')})
    m.box((0, 0, 0), (16, 2, 16), 'base')                                   # soco
    m.box((1, 2, 3), (15, 13, 15), {'north': 'mouth', 'south': 'bricks', 'east': 'bricks', 'west': 'bricks', 'up': None, 'down': None})
    m.box((1, 2, 1), (4, 13, 3), 'bricks', skip=('down',))                  # pilares da boca
    m.box((12, 2, 1), (15, 13, 3), 'bricks', skip=('down',))
    m.box((4, 8, 1), (12, 13, 3), 'bricks', skip=('up',))                   # verga
    m.box((3.5, 7.5, 0.5), (12.5, 8.5, 1), 'copper')                         # friso de cobre da boca
    m.box((6.5, 9, 0.5), (9.5, 11, 1), 'gem')                               # runa (gema)
    m.box((0.5, 11, 0.5), (15.5, 12, 15.5), 'copper', skip=('up', 'down')) # cinta
    m.box((0, 13, 0), (16, 15, 16), 'stone')                                # tampo
    m.box((10, 15, 10), (14, 20, 14), 'copper', skip=('down',))             # chaminé
    m.box((9.5, 20, 9.5), (14.5, 21, 14.5), 'copper')
    m.box((10.5, 20.9, 10.5), (13.5, 21, 13.5), {'up': 'mouth'}, faces=('up',))
    return m


def crusher(lit):
    m = Model({'particle': T('t1s_stone'), 'base': T('t1s_stone_dark'), 'stone': T('t1s_stone'), 'copper': T('t1s_copper'),
               'dark': T('t1s_dark'), 'roller': T('t1s_roller'), 'iron': T('t1s_iron'),
               'plate': T('t1s_plaque_on' if lit else 't1s_plaque')})
    m.box((0, 0, 0), (16, 3, 16), 'base')
    m.box((2, 3, 4), (14, 11, 14), {'north': 'dark', 'south': 'stone', 'east': 'stone', 'west': 'stone', 'up': None, 'down': None})
    m.box((2, 3, 2), (4, 11, 4), 'stone', skip=('down',))
    m.box((12, 3, 2), (14, 11, 4), 'stone', skip=('down',))
    m.box((4, 9, 2), (12, 11, 4), 'stone')
    m.box((4, 3, 2), (12, 4, 4), 'stone', skip=('down',))
    for y in (7.5, 5.0):                                                    # dois rolos dentados
        m.box((4, y - 1, 2.5), (12, y + 1, 4.5), 'roller', rot=((8, y, 3.5), 'x', 45))
    m.box((1.5, 9, 1.5), (14.5, 10, 14.5), 'copper', skip=('up', 'down'))   # cinta
    m.box((4, 11, 4), (12, 13, 12), 'copper', skip=('down',))               # pescoço
    for (a, b) in (((2, 13, 2), (14, 16, 3)), ((2, 13, 13), (14, 16, 14)), ((2, 13, 3), (3, 16, 13)), ((13, 13, 3), (14, 16, 13))):
        m.box(a, b, 'copper')                                               # funil
    m.box((3, 13, 3), (13, 13.5, 13), {'up': 'dark'}, faces=('up',))
    m.box((14, 4, 5), (15, 10, 11), 'iron')                                 # roda lateral (octógono)
    m.box((14, 4, 5), (15, 10, 11), 'iron', rot=((14.5, 7, 8), 'x', 45))
    m.box((15, 6, 7), (16, 8, 9), 'copper')
    m.box((6, 0.5, -0.5), (10, 2.5, 0), {'north': 'plate'}, faces=('north',), uv={'north': [4, 5, 12, 10]})
    return m


def extractor(level, lit):
    m = Model({'particle': T('t1s_stone'), 'stone': T('t1s_stone'), 'base': T('t1s_stone_dark'), 'copper': T('t1s_copper'),
               'floor': T('t1s_rune_floor_on' if lit else 't1s_rune_floor'),
               'gem': T('t1s_amethyst_on' if lit else 't1s_amethyst'), 'content': T('impure_essence_still')})
    for (x, z) in ((1, 1), (12, 1), (1, 12), (12, 12)):
        m.box((x, 0, z), (x + 3, 4, z + 3), 'copper')                       # pés
    walls = [((1, 4, 1), (15, 14, 3)), ((1, 4, 13), (15, 14, 15)), ((1, 4, 3), (3, 14, 13)), ((13, 4, 3), (15, 14, 13))]
    for a, b in walls:
        m.box(a, b, {'north': 'stone', 'south': 'stone', 'east': 'stone', 'west': 'stone', 'up': 'copper', 'down': 'base'})
    m.box((3, 4, 3), (13, 5, 13), {'up': 'floor', 'down': 'base'}, faces=('up', 'down'))
    m.box((0.5, 11, 0.5), (15.5, 12, 15.5), 'copper', skip=('up', 'down'))  # cinta
    m.box((6.5, 6, 0.5), (9.5, 9, 1), 'gem')
    # arco de cobre com o cristal coletor
    m.box((7, 14, 1.5), (9, 20, 2.5), 'copper'); m.box((7, 14, 13.5), (9, 20, 14.5), 'copper')
    m.box((7, 20, 1.5), (9, 21, 14.5), 'copper')
    m.box((7, 15, 7), (9, 19, 9), 'gem', rot=((8, 17, 8), 'y', 45))
    if level:
        h = {1: 7, 2: 10, 3: 13}[level]
        m.box((3, h - 0.1, 3), (13, h, 13), {'up': 'content'}, faces=('up',))
    return m


def condenser(lit):
    m = Model({'particle': T('t1s_stone'), 'base': T('t1s_stone_dark'), 'stone': T('t1s_stone'), 'copper': T('t1s_copper'),
               'glass': T('t1s_glass'), 'water': T('t1s_water_on' if lit else 't1s_water'),
               'ice': T('t1s_ice_on' if lit else 't1s_ice')}, render_type='minecraft:cutout')
    m.box((0, 0, 0), (16, 3, 16), 'base')
    m.box((3, 3, 3), (13, 13, 13), 'glass', skip=('up', 'down'))
    m.box((3.5, 3, 3.5), (12.5, 8 if lit else 6, 12.5), {'up': 'water', 'north': 'water', 'south': 'water', 'east': 'water', 'west': 'water'},
          faces=('up', 'north', 'south', 'east', 'west'))
    m.box((7, 3, 7), (9, 13, 9), 'copper')                                  # serpentina: tubo + discos
    for y in (5, 7.5, 10):
        m.box((5, y, 5), (11, y + 0.75, 11), 'copper')
    for (x, z) in ((2.5, 2.5), (12.5, 2.5), (2.5, 12.5), (12.5, 12.5)):
        m.box((x, 3, z), (x + 1, 13, z + 1), 'copper')
    m.box((2, 13, 2), (14, 15, 14), 'stone')
    m.box((6, 15, 6), (10, 16, 10), 'copper')
    m.box((7, 16, 7), (9, 19, 9), 'ice', rot=((8, 17.5, 8), 'y', 45))
    m.box((7, 4, 1), (9, 6, 3), 'copper')                                   # torneira
    m.box((7.5, 3, 1), (8.5, 4, 2), 'copper')
    return m


MODELS = {
    'runic_furnace': furnace(False), 'runic_furnace_on': furnace(True),
    'runic_crusher': crusher(False), 'runic_crusher_on': crusher(True),
    'condenser': condenser(False), 'condenser_on': condenser(True),
    'essence_extractor': extractor(0, False), 'essence_extractor_on': extractor(0, True),
}
for lv in (1, 2, 3):
    MODELS[f'essence_extractor_level{lv}'] = extractor(lv, False)
    MODELS[f'essence_extractor_level{lv}_on'] = extractor(lv, True)

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out + '/models', exist_ok=True)
    for n, f in TX.ALL.items(): f().image().save(f'{out}/{n}.png')
    for n, m in MODELS.items(): m.save(f'{out}/models/{n}.json')
    print('ok', len(TX.ALL), len(MODELS))
