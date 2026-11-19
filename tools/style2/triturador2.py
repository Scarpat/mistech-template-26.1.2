"""Triturador v2 fiel a receita (ferro, pedregulho, rebolo), so texturas vanilla sem repintar.
Corpo de pedregulho rebaixado 0.5 px entre colunas de ferro embutidas; aro de ferro em cima; rodas = rebolo vanilla."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
OUT = os.path.join(os.path.dirname(__file__), 'out/triturador2'); M = OUT + '/models'; os.makedirs(M, exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)

def f(tex, fr, to, faces=('north', 'south', 'east', 'west', 'up', 'down')):
    out = {}
    for d in faces:
        if d in ('north', 'south'): uv = [fr[0], 16 - to[1], to[0], 16 - fr[1]]
        elif d in ('east', 'west'): uv = [fr[2], 16 - to[1], to[2], 16 - fr[1]]
        else: uv = [fr[0], fr[2], to[0], to[2]]
        out[d] = {'texture': tex, 'uv': uv}
    return out

def body():
    H = 10
    els = []
    # nucleo de pedregulho rebaixado
    els.append(box((0.5, 0, 0.5), (15.5, H - 1, 15.5), f('#cobble', (0.5, 0, 0.5), (15.5, H - 1, 15.5)), name='pedregulho'))
    # colunas de ferro nos cantos, rentes ao bloco
    for (x, z) in ((0, 0), (14, 0), (0, 14), (14, 14)):
        els.append(box((x, 0, z), (x + 2, H, z + 2), f('#iron', (x, 0, z), (x + 2, H, z + 2)), name='coluna'))
    # aro de ferro em cima (moldura da abertura) e base de ferro
    for fr, to in [((0, H - 1, 0), (16, H, 2)), ((0, H - 1, 14), (16, H, 16)), ((0, H - 1, 2), (2, H, 14)), ((14, H - 1, 2), (16, H, 14)),
                   ((2, H - 1, 2), (4, H, 14)), ((12, H - 1, 2), (14, H, 14))]:
        els.append(box(fr, to, f('#iron', fr, to), name='aro'))
    els.append(box((0, 0, 0), (16, 1, 16), f('#iron', (0, 0, 0), (16, 1, 16)), name='base'))
    # fundo escuro da abertura
    els.append(box((4, H - 1.5, 2), (12, H - 1.4, 14), {'up': {'texture': '#dark'}}, name='abertura'))
    # mancais e eixo
    for zc in (4.5, 11.5):
        els.append(box((3, 9.25, zc - 0.75), (13, 10.75, zc + 0.75), f('#iron_dark', (3, 9.25, zc - 0.75), (13, 10.75, zc + 0.75)), name='eixo'))
        for x0 in (0, 13):
            els.append(box((x0, 10, zc - 2), (x0 + 3, 12, zc + 2), f('#iron', (x0, 10, zc - 2), (x0 + 3, 12, zc + 2)), name='mancal'))
    return els

def wheel(zc, ang, tag):
    els = []
    for k, a in enumerate((0, 22.5, 45, -22.5)):
        e = 0.01 * k
        faces = {d: {'texture': '#round', 'uv': [0, 0, 8, 6]} for d in ('up', 'down', 'north', 'south')}
        faces.update({d: {'texture': '#gside', 'uv': [3, 3, 9, 9]} for d in ('east', 'west')})
        el = box((4 + e, 7, zc - 3), (12 - e, 13, zc + 3), faces, name=f'{tag}_{k}')
        el['rotation'] = {'origin': [8, 10, zc], 'axis': 'x', 'angle': a + ang}
        els.append(el)
    return els

tex = {'cobble': 'minecraft:block/cobblestone', 'iron': 'minecraft:block/iron_block', 'iron_dark': 'minecraft:block/heavy_core',
       'dark': 'minecraft:block/black_concrete', 'round': 'minecraft:block/grindstone_round', 'gside': 'minecraft:block/grindstone_side',
       'particle': 'minecraft:block/cobblestone'}
json.dump({'textures': tex, 'elements': body() + wheel(4.5, 0, 'a') + wheel(11.5, 0, 'b')}, open(f'{M}/t2_prev.json', 'w'), indent=1)
for i in range(6):
    a = i * 7.5
    json.dump({'textures': tex, 'elements': body() + wheel(4.5, a, 'a') + wheel(11.5, -a, 'b')}, open(f'{M}/t2_f{i}.json', 'w'), indent=1)
print('ok')
