"""Baterias Runicas (normal + avancada). Receitas: cobre, residuo magico, Raio Engarrafado / 4 baterias, fragmento impuro, runa do Vazio."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
from lib import box
from triturador2 import f
OUT = os.path.join(os.path.dirname(__file__), 'out/bateria'); M = OUT + '/models'; os.makedirs(M, exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)

def B(fr, to, tex, name, **over):
    d = f(tex, fr, to); d.update(over); return box(fr, to, d, name=name)

def glass_cyl(x0, x1, y0, y1, z0, z1, name='vidro'):
    """Vidro: so as faces de lado (o miolo aparece)."""
    return box((x0, y0, z0), (x1, y1, z1), {d: {'texture': '#glass'} for d in ('north', 'south', 'east', 'west')}, name=name)

def core(cx, cy, cz, s, lit):
    els = [B((cx - s / 2, cy - s / 2, cz - s / 2), (cx + s / 2, cy + s / 2, cz + s / 2), '#core' if lit else '#core_off', 'nucleo')]
    if lit:
        for a in (45, -45):
            els.append({'from': [cx - s, cy - s, cz], 'to': [cx + s, cy + s, cz], 'shade': False, 'rotation': {'origin': [cx, cy, cz], 'axis': 'y', 'angle': a},
                        'faces': {d: {'texture': '#arcs', 'uv': [8 - s, 8 - s, 8 + s, 8 + s]} for d in ('north', 'south')}, 'name': 'arcos'})
    return els

def terminal(x, z, y=14.5):
    return [B((x, y, z), (x + 2, y + 1.5, z + 2), '#copper', 'terminal'), B((x + 0.5, y + 1.5, z + 0.5), (x + 1.5, y + 2.5, z + 1.5), '#rod', 'terminal_ponta')]

# ---------- A: gaiola de raio ----------
def A(lit, mat='#copper', cap='#copper'):
    els = [B((1.5, 0, 1.5), (14.5, 3, 14.5), mat, 'base'), B((3, 3, 3), (13, 3.5, 13), '#copper_block', 'piso')]
    for (x, z) in ((1.5, 1.5), (12, 1.5), (1.5, 12), (12, 12)):
        els.append(B((x, 3, z), (x + 2.5, 13, z + 2.5), '#copper', 'coluna'))
    els += core(8, 8.5, 8, 6, lit)
    els.append(B((1.5, 13, 1.5), (14.5, 14.5, 14.5), cap, 'tampa'))
    els += terminal(4, 7) + terminal(10, 7)
    return els

def A_adv(lit):
    els = A(lit, '#impure', '#impure')
    for (x, z) in ((2, 2), (12.5, 2), (2, 12.5), (12.5, 12.5)):
        els.append(B((x, 14.5, z), (x + 1.5, 16, z + 1.5), '#impure', 'cristal'))
    return els

# ---------- B: celula de cobre ----------
def B_(lit, body='#copper'):
    els = [B((1, 0, 1), (15, 2, 15), '#copper_dark', 'base')]
    for fr, to in [((1.5, 2, 1.5), (15, 14, 4)), ((1.5, 2, 12), (14.5, 14, 14.5)), ((1.5, 2, 4), (4, 14, 12)), ((12, 2, 4), (14.5, 14, 12))]:
        pass
    # corpo: 4 colunas de canto + faixas em cima/embaixo, janelas de vidro no meio de cada lado
    for (x, z) in ((1.5, 1.5), (11.5, 1.5), (1.5, 11.5), (11.5, 11.5)):
        els.append(B((x, 2, z), (x + 3, 14, z + 3), body, 'coluna'))
    for fr, to in [((4.5, 2, 1.5), (11.5, 4, 14.5)), ((4.5, 12, 1.5), (11.5, 14, 14.5)), ((1.5, 2, 4.5), (4.5, 4, 11.5)), ((11.5, 2, 4.5), (14.5, 4, 11.5)),
                   ((1.5, 12, 4.5), (4.5, 14, 11.5)), ((11.5, 12, 4.5), (14.5, 14, 11.5))]:
        els.append(B(fr, to, body, 'faixa'))
    els.append(glass_cyl(1.6, 14.4, 4, 12, 1.6, 14.4, 'janelas'))
    els += core(8, 8, 8, 5, lit)
    els.append(B((1, 14, 1), (15, 15, 15), '#copper', 'tampa'))
    els += terminal(4, 7, 15) + terminal(10, 7, 15)
    return els

def B_adv(lit):
    els = B_(lit, '#impure')
    els = [e for e in els if e.get('name') != 'terminal_ponta' and e.get('name') != 'terminal']
    els.append(B((6, 15, 6), (10, 16.5, 10), '#impure', 'selo_vazio'))
    return els

# ---------- C: bobina de Tesla ----------
def C(lit, orb=None):
    els = [B((2, 0, 2), (14, 2, 14), '#copper_dark', 'base'), B((3, 2, 3), (13, 3, 13), '#copper', 'degrau')]
    els.append(B((6.5, 3, 6.5), (9.5, 11, 9.5), '#copper', 'coluna'))
    for y in (3.5, 5.5, 7.5, 9.5):
        els.append(B((4.5, y, 4.5), (11.5, y + 1, 11.5), '#copper_block', 'espira'))
    els.append(B((5.5, 10.5, 5.5), (10.5, 11.5, 10.5), '#copper', 'colar'))
    if orb:
        els.append(B((5, 11.5, 5), (11, 16, 11), orb, 'orbe'))
    else:
        els += core(8, 13.75, 8, 4.5, lit)
    return els

def C_adv(lit):
    els = C(lit, '#impure')
    for (x, z) in ((2.5, 2.5), (11.5, 2.5), (2.5, 11.5), (11.5, 11.5)):
        els.append(B((x, 2, z), (x + 2, 6, z + 2), '#impure', 'cristal'))
    return els

tex = {'copper': 'minecraft:block/cut_copper', 'copper_dark': 'minecraft:block/exposed_cut_copper', 'copper_block': 'minecraft:block/copper_block',
       'rod': 'minecraft:block/lightning_rod', 'glass': 'mistech:block/glass_jar', 'core': 'mistech:block/bottled_lightning_core',
       'core_off': 'minecraft:block/light_gray_concrete', 'arcs': 'mistech:block/bottled_lightning_arcs_a', 'impure': 'mistech:block/impure_magic_block',
       'residue': 'minecraft:block/gravel', 'particle': 'minecraft:block/cut_copper'}
for n, fn in (('A', A), ('Aadv', A_adv), ('B', B_), ('Badv', B_adv), ('C', C), ('Cadv', C_adv)):
    for lit in (False, True):
        json.dump({'textures': tex, 'elements': fn(lit)}, open(f'{M}/bt{n}{"_on" if lit else ""}.json', 'w'), indent=1)
print('ok')
