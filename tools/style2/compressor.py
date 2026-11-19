"""Amostra: Compressor de Magia como prensa (receita: pedra-negra polida, laje, ametista). Texturas vanilla.
Cabecote desce conforme o progresso (renderer); ponta de ametista."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
from triturador2 import f
OUT = os.path.join(os.path.dirname(__file__), 'out/compressor'); M = OUT + '/models'; os.makedirs(M, exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)

def body(filled):
    els = [box((1, 0, 1), (15, 2, 15), f('#pb', (1, 0, 1), (15, 2, 15)), name='base'),
           box((2, 2, 2), (14, 3, 14), f('#pb', (2, 2, 2), (14, 3, 14)), name='degrau')]
    # bacia: anel 4..12 com boca 5..11
    for fr, to in [((4, 3, 4), (12, 5, 5)), ((4, 3, 11), (12, 5, 12)), ((4, 3, 5), (5, 5, 11)), ((11, 3, 5), (12, 5, 11))]:
        els.append(box(fr, to, f('#pb', fr, to), name='bacia'))
    els.append(box((5, 3, 5), (11, 3.5, 11), {'up': {'texture': '#ess' if filled else '#dark'}} if not filled else {'up': {'texture': '#ess'}}, name='fundo'))
    if filled:
        els[-1] = box((5, 3, 5), (11, 4.5, 11), {'up': {'texture': '#ess'}}, name='essencia')
    # pilares e viga
    for x0 in (1, 12):
        els.append(box((x0, 2, 6), (x0 + 3, 13, 10), f('#bricks', (x0, 2, 6), (x0 + 3, 13, 10)), name='pilar'))
    els.append(box((0, 13, 5), (16, 16, 11), f('#bricks', (0, 13, 5), (16, 16, 11)), name='viga'))
    return els

TOP = 9.0  # posicao de cima do cabecote; a textura fica presa a ele (uv calculado sempre nessa altura)

def moved(fr, to, faces_from, dy):
    e = box(fr, to, faces_from)
    e['from'][1] += dy; e['to'][1] += dy
    return e

def head(y):
    """Cabecote de pedra-negra talhada com ponta de ametista; y = base da ponta. UV fixo: a textura desce junto."""
    dy = y - TOP
    els = [moved((4.5, TOP + 1.5, 6), (11.5, TOP + 4, 10), f('#chis', (4.5, TOP + 1.5, 6), (11.5, TOP + 4, 10)), dy),
           moved((5.5, TOP, 6.5), (10.5, TOP + 1.5, 9.5), f('#amethyst', (5.5, TOP, 6.5), (10.5, TOP + 1.5, 9.5)), dy)]
    # corrente: os elos descem junto com o cabecote (a ponta de baixo da textura fica presa nele)
    top_len = 13 - (TOP + 4)
    length = 13 - (y + 4)
    for a in (45, -45):
        els.append({'from': [6.5, y + 4, 8], 'to': [9.5, 13, 8], 'shade': False, 'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': a},
                    'faces': {d: {'texture': '#chain', 'uv': [0, 16 - length, 3, 16]} for d in ('north', 'south')}})
    return els

tex = {'pb': 'minecraft:block/polished_blackstone', 'amethyst': 'minecraft:block/amethyst_block', 'chain': 'minecraft:block/iron_chain', 'bricks': 'minecraft:block/polished_blackstone_bricks', 'chis': 'minecraft:block/chiseled_polished_blackstone',
       'dark': 'minecraft:block/black_concrete', 'ess': 'mistech:block/impure_essence_still', 'particle': 'minecraft:block/polished_blackstone'}
json.dump({'textures': tex, 'elements': body(False) + head(9)}, open(f'{M}/c_vazio.json', 'w'), indent=1)
json.dump({'textures': tex, 'elements': body(True) + head(6.5)}, open(f'{M}/c_meio.json', 'w'), indent=1)
json.dump({'textures': tex, 'elements': body(True) + head(4.5)}, open(f'{M}/c_fim.json', 'w'), indent=1)
for i in range(8):
    y = 9 - (9 - 4.5) * i / 7
    json.dump({'textures': tex, 'elements': body(True) + head(y)}, open(f'{M}/c_f{i}.json', 'w'), indent=1)
print('ok')
