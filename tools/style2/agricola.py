"""Colhedor + Plantador (familia). Receitas: madeira turva, cobre, enxadas de ferro + bau / sementes + dispenser, runas."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
from triturador2 import f
OUT = os.path.join(os.path.dirname(__file__), 'out/agricola'); M = OUT + '/models'; os.makedirs(M, exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)

def B(fr, to, tex, name, **over):
    d = f(tex, fr, to); d.update(over); return box(fr, to, d, name=name)

def cross(x0, x1, y0, y1, zc, tex, name='planta'):
    return [{'from': [x0, y0, zc], 'to': [x1, y1, zc], 'shade': False, 'rotation': {'origin': [(x0 + x1) / 2, y0, zc], 'axis': 'y', 'angle': a},
             'faces': {d: {'texture': tex, 'uv': [0, 16 - (y1 - y0), x1 - x0, 16]} for d in ('north', 'south')}, 'name': name} for a in (45, -45)]

def crate(h=10):
    els = [B((1, 0, 1), (15, h, 15), '#planks', 'caixa')]
    for (x, z) in ((0.5, 0.5), (13.5, 0.5), (0.5, 13.5), (13.5, 13.5)):
        els.append(B((x, 0, z), (x + 2, h, z + 2), '#copper', 'cantoneira'))
    return els

# ---------- A: maquina agricola ----------
def A_harv(ang=0):
    els = crate()
    els.append(B((1.5, 10, 1.5), (14.5, 12, 14.5), '#planks', 'tampa'))
    els.append(B((7, 9, 0.5), (9, 11, 1), '#copper', 'trinco'))
    for x0 in (1, 13):
        els.append(B((x0, 4, -4), (x0 + 2, 8, 1), '#copper', 'suporte'))
    els.append(B((3, 5.5, -2.5), (13, 6.5, -1.5), '#anvil', 'eixo'))
    for k, a in enumerate((0, 45, 90, 135)):
        e = B((3.01, 5.5, -6), (12.99, 6.5, 2), '#iron', 'lamina')
        e['from'][2], e['to'][2] = -2.5 - 3.5, -1.5 + 3.5 - 7   # pa radial de 3.5 px para cada lado
        e = box((3.01 + k * 0.001, 5.6, -5.5), (12.99 - k * 0.001, 6.4, 1.5), f('#iron', (3, 5.6, -5.5), (13, 6.4, 1.5)), name='lamina')
        e['rotation'] = {'origin': [8, 6, -2], 'axis': 'x', 'angle': (a + ang) if (a + ang) <= 45 else (a + ang) - 180}
        els.append(e)
    return els

def A_plant():
    els = crate()
    els.append(box((3, 2, 0.9), (13, 9, 1), {'north': {'texture': '#dispenser', 'uv': [3, 5, 13, 12]}}, name='boca_dispenser'))
    # funil de sementes (hopper vanilla)
    els.append(B((2, 10, 2), (14, 13, 14), '#hopper_out', 'funil', up={'texture': '#hopper_top'}))
    els.append(box((4, 12.9, 4), (12, 13, 12), {'up': {'texture': '#compost'}}, name='sementes'))
    els.append(B((6, 13, 6), (10, 14, 10), '#copper', 'tampa_funil') if False else None)
    return [e for e in els if e]

# ---------- B: canteiro vivo ----------
def B_base():
    els = [B((1, 0, 1), (15, 9, 15), '#planks', 'canteiro', up={'texture': '#farmland'}),
           B((0.5, 7.5, 0.5), (15.5, 9, 15.5), '#copper', 'friso', up={'texture': '#copper'}) if False else None]
    for fr, to in [((0.5, 7.5, 0.5), (15.5, 9.5, 1.5)), ((0.5, 7.5, 14.5), (15.5, 9.5, 15.5)), ((0.5, 7.5, 1.5), (1.5, 9.5, 14.5)), ((14.5, 7.5, 1.5), (15.5, 9.5, 14.5))]:
        els.append(B(fr, to, '#copper', 'borda'))
    return [e for e in els if e]

def B_harv():
    els = B_base() + cross(2, 14, 9, 16, 8, '#wheat7')
    els.append(B((1, 1, -1), (15, 2.5, 1), '#anvil', 'barra_corte'))
    for x0 in (1.5, 4, 6.5, 9, 11.5, 14):
        els.append(B((x0 - 0.5, 1.25, -3), (x0 + 0.5, 2.25, -1), '#iron', 'dente'))
    return els

def B_plant():
    els = B_base()
    for (x, z) in ((4, 4), (11, 11), (4, 11), (11, 4)):
        els += cross(x - 2, x + 2, 9, 12, z, '#wheat2')
    els.append(B((7, 9, 7), (9, 14, 9), '#copper', 'cano'))
    els.append(B((5, 14, 5), (11, 15.5, 11), '#copper', 'irrigador', down={'texture': '#copper_grate'}))
    return els

# ---------- C: jardineiro mecanico ----------
def C_body():
    els = []
    for (x, z) in ((3, 3), (11, 3), (3, 11), (11, 11)):
        els.append(B((x, 0, z), (x + 2, 4, z + 2), '#copper', 'perna'))
    els.append(B((2, 4, 2), (14, 13, 14), '#planks', 'corpo'))
    els.append(B((1.5, 12, 1.5), (14.5, 14, 14.5), '#copper', 'tampa'))
    els.append(box((5, 7, 1.9), (11, 10, 2), {'north': {'texture': '#lamp_on', 'uv': [5, 7, 11, 10]}}, name='visor') if False else None)
    return [e for e in els if e]

def C_harv():
    els = C_body()
    els.append(B((14, 8, 6), (16, 10, 8), '#copper', 'ombro'))
    els.append(B((14.5, 8.5, -3), (15.5, 9.5, 6), '#stick', 'cabo'))
    els.append(B((12.5, 6, -4), (17, 9.5, -3), '#iron', 'enxada'))
    return els

def C_plant():
    els = C_body()
    els.append(B((14, 8, 6), (16, 10, 8), '#copper', 'ombro'))
    els.append(B((14.5, 8.5, -1), (15.5, 9.5, 6), '#copper', 'bico'))
    els.append(B((13.5, 7.5, -3), (16.5, 10.5, -1), '#copper', 'regador', north={'texture': '#copper_grate', 'uv': [6, 6, 9, 9]}))
    els.append(B((6, 14, 6), (10, 16, 10), '#hopper_out', 'funil', up={'texture': '#compost'}))
    return els

tex = {'planks': 'mistech:block/murky_planks', 'copper': 'minecraft:block/cut_copper', 'iron': 'minecraft:block/iron_block', 'anvil': 'minecraft:block/anvil',
       'dispenser': 'minecraft:block/dispenser_front', 'hopper_out': 'minecraft:block/hopper_outside', 'hopper_top': 'minecraft:block/hopper_top',
       'compost': 'minecraft:block/composter_compost', 'farmland': 'minecraft:block/farmland_moist', 'wheat7': 'minecraft:block/wheat_stage7',
       'wheat2': 'minecraft:block/wheat_stage2', 'stick': 'minecraft:block/stripped_oak_log', 'copper_grate': 'minecraft:block/copper_grate',
       'particle': 'mistech:block/murky_planks'}
for n, fn in (('A_h', A_harv), ('A_p', A_plant), ('B_h', B_harv), ('B_p', B_plant), ('C_h', C_harv), ('C_p', C_plant)):
    json.dump({'textures': tex, 'elements': fn()}, open(f'{M}/ag{n}.json', 'w'), indent=1)
print('ok')
