"""Amostra: Gerador de Cinzas (receita: cobre, fornalha, bloco de cinzas, runas de fogo e energia). Texturas vanilla/mod."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
from triturador2 import f
OUT = os.path.join(os.path.dirname(__file__), 'out/gerador'); M = OUT + '/models'; os.makedirs(M, exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)

def model(lit):
    els = []
    # base de cinzas
    els.append(box((0, 0, 0), (16, 2, 16), f('#ash', (0, 0, 0), (16, 2, 16)), name='base_cinzas'))
    # fornalha (frente = norte): boca da fornalha vanilla
    fb = f('#fside', (1, 2, 1), (15, 10, 15))
    fb['north'] = {'texture': '#front', 'uv': [1, 6, 15, 14]}
    fb['up'] = {'texture': '#ftop', 'uv': [1, 1, 15, 15]}
    els.append(box((1, 2, 1), (15, 10, 15), fb, name='fornalha'))
    # suportes de cobre da caldeira
    for x0 in (2, 12):
        els.append(box((x0, 10, 5), (x0 + 2, 11.5, 11), f('#copper', (x0, 10, 5), (x0 + 2, 11.5, 11)), name='berco'))
    # caldeira: tambor de cobre deitado ao longo de x, redondo em degraus (secao 6x6 com cantos cortados)
    def drum(fr, to, n):
        fc = f('#copper', fr, to)
        fc['east'] = {'texture': '#grate', 'uv': [fr[2], 16 - to[1], to[2], 16 - fr[1]]}
        fc['west'] = dict(fc['east'])
        return box(fr, to, fc, name=n)
    els.append(drum((2, 11, 5), (14, 15, 11), 'caldeira'))
    els.append(drum((2.01, 10.5, 5.5), (13.99, 15.5, 10.5), 'caldeira'))
    els.append(drum((2.02, 10, 6), (13.98, 16, 10), 'caldeira'))
    # cintas de ferro no tambor
    for x0 in (4, 11):
        els.append(box((x0, 9.9, 5.9), (x0 + 1, 16.1, 10.1), f('#iron', (x0, 9.9, 5.9), (x0 + 1, 16.1, 10.1)), name='cinta'))
    # chamine de cobre atras, a direita
    els.append(box((10, 10, 11), (13, 16, 14), {**f('#copper', (10, 10, 11), (13, 16, 14)), 'up': {'texture': '#dark'}}, name='chamine'))
    # lampada de cobre (energia) no canto da frente
    bulb = '#bulb_on' if lit else '#bulb'
    els.append(box((2, 10, 1.5), (6, 14, 5.5), {d: {'texture': bulb, 'uv': [6, 6, 10, 10]} for d in ('north', 'south', 'east', 'west', 'up')}, name='lampada'))
    els = [e for e in els if e]
    return els

tex = {'ash': 'mistech:block/ash_block', 'fside': 'minecraft:block/furnace_side', 'ftop': 'minecraft:block/furnace_top',
       'copper': 'minecraft:block/cut_copper', 'grate': 'minecraft:block/copper_grate', 'rod': 'minecraft:block/lightning_rod',
       'bulb': 'minecraft:block/copper_bulb', 'bulb_on': 'minecraft:block/copper_bulb_lit', 'iron': 'minecraft:block/anvil', 'dark': 'minecraft:block/black_concrete', 'particle': 'minecraft:block/cut_copper'}
for lit in (False, True):
    t = dict(tex, front='minecraft:block/furnace_front' + ('_on' if lit else ''))
    json.dump({'textures': t, 'elements': model(lit)}, open(f'{M}/g{"_on" if lit else ""}.json', 'w'), indent=1)
print('ok')
