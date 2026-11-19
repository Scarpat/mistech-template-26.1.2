"""Tres versoes do Gerador de Blaze (receita: varas de blaze, tijolos de pedra-negra polida, Fornalha Runica, runas)."""
import sys, os, json, math
sys.path.insert(0, os.path.dirname(__file__))
from lib import box, save, blank
from triturador2 import f
from PIL import Image
OUT = os.path.join(os.path.dirname(__file__), 'out/blaze'); M = OUT + '/models'; T = OUT + '/textures'
os.makedirs(M, exist_ok=True); os.makedirs(T, exist_ok=True)
# vara de blaze: copia das faces da vara do Blaze vanilla (2x8 de lado, 2x2 de topo), sem repintar
bz = Image.open(os.path.expanduser('~/van/assets/minecraft/textures/entity/blaze/blaze.png')).convert('RGBA')
rod = blank()
for i, u in enumerate((0, 2, 4, 6)):      # 4 lados lado a lado: x 0..8, y 0..8
    for y in range(8):
        for x in range(2): rod.putpixel((i * 2 + x, y), bz.getpixel((u + x, 18 + y)))
for y in range(2):
    for x in range(2): rod.putpixel((8 + x, y), bz.getpixel((2 + x, 16 + y)))   # topo em 8..10, 0..2
save(rod, f'{T}/blaze_generator_rod.png')

def rodbox(x, y, z, h=8, name='vara', rot=None):
    fc = {'north': {'texture': '#rod', 'uv': [2, 0, 4, h]}, 'east': {'texture': '#rod', 'uv': [4, 0, 6, h]},
          'south': {'texture': '#rod', 'uv': [6, 0, 8, h]}, 'west': {'texture': '#rod', 'uv': [0, 0, 2, h]},
          'up': {'texture': '#rod', 'uv': [8, 0, 10, 2]}, 'down': {'texture': '#rod', 'uv': [8, 0, 10, 2]}}
    return box((x, y, z), (x + 2, y + h, z + 2), fc, rot=rot, name=name)

def A(lit):
    """Fogao do Nether: mesmo formato do Gerador de Cinzas (familia), varas de blaze no tampo."""
    m = json.load(open(os.path.join(os.path.dirname(__file__), '../../src/main/resources/assets/mistech/models/block/blaze_generator.json')))
    els = [e for e in m['elements'] if e.get('name') not in ('chimney', 'chimney_cap', 'ash_tray')]   # tira as pecas de ouro
    for (x, z) in ((2, 2), (12, 2), (2, 12), (12, 12)):
        els.append(rodbox(x, 12.5, z, 5))
    return els, m['textures']

def B(lit, ang=0.0, base='rf'):
    """Gaiola do blaze: Fornalha Runica embaixo, colunas de tijolo, nucleo de magma e varas girando em volta."""
    if base == 'rf':
        els = [box((0, 0, 0), (16, 7, 16), {**f('#rf_side', (0, 0, 0), (16, 7, 16)), 'north': {'texture': '#rf_front', 'uv': [0, 9, 16, 16]},
                                             'up': {'texture': '#bricks'}}, name='fornalha_runica')]
    else:
        els = [box((0, 0, 0), (16, 7, 16), {**f('#bricks', (0, 0, 0), (16, 7, 16)), 'up': {'texture': '#pb'}}, name='base')]
        if base == 'grade':
            # a grade que o gerador ja usa hoje, numa moldura de pedra-negra polida
            els.append(box((3, 1, -0.01), (13, 6, 0.5), {'north': {'texture': '#grate', 'uv': [3, 8, 13, 13]}}, name='grade'))
        else:
            # janela de magma vanilla com moldura
            els.append(box((4, 1.5, -0.5), (12, 5.5, 0), {'north': {'texture': '#magma', 'uv': [4, 10.5, 12, 14.5]},
                       'east': {'texture': '#pb'}, 'west': {'texture': '#pb'}, 'up': {'texture': '#pb'}, 'down': {'texture': '#pb'}}, name='janela'))
            for fr, to in [((3, 1, -1), (13, 1.5, 0)), ((3, 5.5, -1), (13, 6, 0)), ((3, 1.5, -1), (4, 5.5, 0)), ((12, 1.5, -1), (13, 5.5, 0))]:
                els.append(box(fr, to, f('#pb', fr, to), name='moldura'))
    for (x, z) in ((0, 0), (13, 0), (0, 13), (13, 13)):
        els.append(box((x, 7, z), (x + 3, 14, z + 3), f('#bricks', (x, 7, z), (x + 3, 14, z + 3)), name='coluna'))
    els.append(box((0, 14, 0), (16, 16, 16), {**f('#bricks', (0, 14, 0), (16, 16, 16)), 'down': {'texture': '#bricks'}}, name='teto') if False else None)
    for fr, to in [((0, 14, 0), (16, 16, 3)), ((0, 14, 13), (16, 16, 16)), ((0, 14, 3), (3, 16, 13)), ((13, 14, 3), (16, 16, 13))]:
        els.append(box(fr, to, f('#bricks', fr, to), name='aro'))
    els.append(box((6, 8.5, 6), (10, 12.5, 10), f('#magma', (6, 8.5, 6), (10, 12.5, 10)), name='nucleo'))
    for k in range(4):   # previa: na hora o renderer gira as varas em volta do nucleo
        a = math.radians(ang + k * 90)
        cx, cz = 8 + 4 * math.cos(a) - 1, 8 + 4 * math.sin(a) - 1
        els.append(rodbox(cx, 7.5 + (k % 2) * 1.5, cz, 6))
    return [e for e in els if e], None

def C(lit):
    """Pira: bloco da Fornalha Runica com varas de blaze cruzadas em cima e fogo."""
    els = [box((0, 0, 0), (16, 11, 16), {**f('#rf_side', (0, 0, 0), (16, 11, 16)), 'north': {'texture': '#rf_front', 'uv': [0, 5, 16, 16]},
                                          'up': {'texture': '#rf_top'}}, name='fornalha_runica')]
    def lying(fr, to, along_x):
        side = {'texture': '#rod', 'uv': [2, 0, 4, 8], 'rotation': 90}
        end = {'texture': '#rod', 'uv': [8, 0, 10, 2]}
        if along_x:
            fc = {'north': side, 'south': side, 'up': side, 'down': side, 'east': end, 'west': end}
        else:
            fc = {'east': side, 'west': side, 'up': dict(side, rotation=0), 'down': dict(side, rotation=0), 'north': end, 'south': end}
        return box(fr, to, fc, name='vara_deitada')
    els.append(lying((4, 11, 7), (12, 13, 9), True))
    els.append(lying((7, 13, 4), (9, 15, 12), False))
    if lit:
        for a in (45, -45):
            els.append({'from': [4, 13, 8], 'to': [12, 19, 8], 'shade': False, 'rotation': {'origin': [8, 13, 8], 'axis': 'y', 'angle': a},
                        'faces': {d: {'texture': '#fire', 'uv': [4, 10, 12, 16]} for d in ('north', 'south')}})
    return els, None

tex = {'rod': 'mistech:block/blaze_generator_rod', 'bricks': 'minecraft:block/polished_blackstone_bricks', 'magma': 'minecraft:block/magma',
       'rf_side': 'mistech:block/runic_furnace_side', 'rf_top': 'mistech:block/runic_furnace_top', 'fire': 'minecraft:block/fire_0',
       'particle': 'minecraft:block/polished_blackstone_bricks', 'pb': 'minecraft:block/polished_blackstone'}
for lit in (False, True):
    on = '_on' if lit else ''
    els, t0 = A(lit); t = dict(t0, rod=tex['rod']); t['front'] = 'mistech:block/blaze_generator_front' + on
    json.dump({'textures': t, 'elements': els}, open(f'{M}/bA{on}.json', 'w'), indent=1)
    for n, fn in (('B', B), ('C', C)):
        els, _ = fn(lit); t = dict(tex, rf_front='mistech:block/runic_furnace_front' + on)
        json.dump({'textures': t, 'elements': els}, open(f'{M}/b{n}{on}.json', 'w'), indent=1)
for i in range(8):
    els, _ = B(True, i * 11.25); json.dump({'textures': dict(tex, rf_front='mistech:block/runic_furnace_front_on'), 'elements': els}, open(f'{M}/bB_f{i}.json', 'w'), indent=1)
for b in ('grade', 'magma'):
    for lit in (False, True):
        on = '_on' if lit else ''
        els, _ = B(lit, 20, b)
        json.dump({'textures': dict(tex, grate='mistech:block/blaze_generator_front' + on), 'elements': els}, open(f'{M}/bB_{b}{on}.json', 'w'), indent=1)
print('ok')
