"""Amostra: Triturador Runico, lado Create. Gera texturas e modelos em out/triturador/."""
import sys, os, json, math
sys.path.insert(0, os.path.dirname(__file__))
from lib import *
from estilo_a_lib import rune_at
from kit import VAZIO
OUT = os.path.join(os.path.dirname(__file__), 'out/triturador')
T = OUT + '/textures'; M = OUT + '/models'

# --- texturas ---
side = casing(6); save(side, f'{T}/s2_crusher_side.png')
for lit in (False, True):
    f = casing(6)
    # runa algiz simetrica: haste de 2 px centrada no eixo 7.5 do bloco
    ALGIZ = ["X....X", ".X..X.", "..XX..", "..XX..", "..XX.."]
    pts = [(5 + i, 8 + j) for j, r in enumerate(ALGIZ) for i, c in enumerate(r) if c == 'X']
    runeplate(f, 4, 7, 11, 13, [], lit)
    for (x, y) in pts:
        f.putpixel((x, y), (ROXOS[5] if y < 10 else ROXOS[4]) if lit else COBRE[1])
    save(f, f'{T}/s2_crusher_front{"_on" if lit else ""}.png')

top = blank()
pl = remap(V('spruce_planks'), MADEIRA); paste_region(top, pl, (0, 0, 15, 15))
wood_frame(top, 0, 0, 15, 15)
rect(top, (4, 2, 11, 13), VAZIO[1])
for x in range(4, 12): top.putpixel((x, 2), VAZIO[0]); top.putpixel((x, 13), VAZIO[3])
for y in range(2, 14): top.putpixel((4, y), VAZIO[0])
# trilhos de cobre ladeando a abertura
for y in range(1, 15): top.putpixel((3, y), COBRE[4]); top.putpixel((12, y), COBRE[2])
bracket(top, 0, 0, 1, 1); bracket(top, 15, 0, -1, 1); bracket(top, 0, 15, 1, -1); bracket(top, 15, 15, -1, -1)
save(top, f'{T}/s2_crusher_top.png')

bot = remap(V('spruce_planks'), MADEIRA[0:5]); save(bot, f'{T}/s2_crusher_bottom.png')

# roda: face (disco) na regiao 5..10, banda (piso) linhas 5..10 colunas 2..13
face = blank()
st = remap(V('andesite'), PEDRA[1:6])
for y in range(5, 11):
    for x in range(5, 11):
        ring = max(abs(x - 7.5), abs(y - 7.5))
        c = st.getpixel((x, y))
        if ring > 2: c = PEDRA[1] if (x + y) % 2 else PEDRA[2]          # dentes
        elif ring > 1: c = COBRE[5] if (x < 8 and y < 8) else COBRE[3] if (x < 8 or y < 8) else COBRE[2]
        else: c = FERRO[2]
        face.putpixel((x, y), c)
face.putpixel((7, 7), LATAO[6]); face.putpixel((8, 8), FERRO[1])
save(face, f'{T}/s2_wheel_face.png')

tread = blank()
for y in range(5, 11):
    for x in range(4, 12):
        g = (y - 5) % 3
        c = [PEDRA[4], PEDRA[2], PEDRA[0]][g]
        if (x * 7 + y * 3) % 5 == 0 and g == 1: c = PEDRA[3]
        if x in (4, 11): c = [COBRE[5], COBRE[3], COBRE[1]][g]
        tread.putpixel((x, y), c)
save(tread, f'{T}/s2_wheel_tread.png')

sh = blank()
for y in range(16):
    for x in range(16): sh.putpixel((x, y), [FERRO[5], FERRO[3]][y % 2] if x % 4 else FERRO[2])
save(sh, f'{T}/s2_shaft.png')
be = blank()
for y in range(0, 4):
    for x in range(0, 3): be.putpixel((x, y), COBRE[5] if y == 0 or x == 0 else COBRE[4] if y < 3 else COBRE[2])
be.putpixel((1, 1), LATAO[4])
for y in range(4, 6):
    for x in range(0, 7): be.putpixel((x, y), COBRE[4] if y == 4 else COBRE[2])
be.putpixel((5, 4), FERRO[1]); be.putpixel((4, 4), FERRO[1])
save(be, f'{T}/s2_bearing.png')
# --- modelos ---
def wheel(zc, ang, tag):
    els = []
    for k, a in enumerate((0, 22.5, 45, -22.5)):
        e = 0.01 * k
        fu = {'up': [4, 5, 12, 11], 'down': [4, 5, 12, 11], 'north': [4, 5, 12, 11], 'south': [4, 5, 12, 11],
              'east': [5, 5, 11, 11], 'west': [5, 5, 11, 11]}
        faces = {d: {'texture': '#tread' if d in ('up', 'down', 'north', 'south') else '#face', 'uv': fu[d]} for d in fu}
        el = box((4 + e, 7, zc - 3), (12 - e, 13, zc + 3), faces, name=f'{tag}_{k}')
        aa = a + ang
        el['rotation'] = {'origin': [8, 10, zc], 'axis': 'x', 'angle': aa}
        els.append(el)
    return els

def mancais(zc):
    sh = box((1, 9.25, zc - 0.75), (15, 10.75, zc + 0.75), all_faces('#shaft'), name='eixo')
    bs = []
    for x0 in (0, 13):
        bs.append(box((x0, 10, zc - 2), (x0 + 3, 12, zc + 2), {
            'up': {'texture': '#bearing', 'uv': [0, 0, 3, 4]}, 'north': {'texture': '#bearing', 'uv': [0, 4, 3, 6]},
            'south': {'texture': '#bearing', 'uv': [0, 4, 3, 6]}, 'east': {'texture': '#bearing', 'uv': [3, 4, 7, 6]},
            'west': {'texture': '#bearing', 'uv': [3, 4, 7, 6]}}, name='mancal'))
    return [sh] + bs

def body(lit):
    return mancais(4.5) + mancais(11.5) + [box((0, 0, 0), (16, 10, 16), {
        'north': {'texture': '#front', 'uv': [0, 6, 16, 16]}, 'south': {'texture': '#side', 'uv': [0, 6, 16, 16]},
        'east': {'texture': '#side', 'uv': [0, 6, 16, 16]}, 'west': {'texture': '#side', 'uv': [0, 6, 16, 16]},
        'up': {'texture': '#top'}, 'down': {'texture': '#bottom'}}, name='carcaca')]

tex = {'side': 'mistech:block/s2_crusher_side', 'top': 'mistech:block/s2_crusher_top', 'bottom': 'mistech:block/s2_crusher_bottom',
       'tread': 'mistech:block/s2_wheel_tread', 'face': 'mistech:block/s2_wheel_face', 'shaft': 'mistech:block/s2_shaft', 'bearing': 'mistech:block/s2_bearing', 'particle': 'mistech:block/s2_crusher_side'}
os.makedirs(M, exist_ok=True)
for lit in (False, True):
    t = dict(tex, front='mistech:block/s2_crusher_front' + ('_on' if lit else ''))
    json.dump({'parent': 'minecraft:block/block', 'textures': t, 'elements': body(lit)},
              open(f'{M}/s2_crusher{"_on" if lit else ""}.json', 'w'), indent=1)
# roda separada (o renderer gira) + modelos de previa por quadro
json.dump({'textures': tex, 'elements': wheel(8, 0, 'roda')}, open(f'{M}/s2_crusher_wheel.json', 'w'), indent=1)
for i in range(6):
    a = i * 7.5   # 16 dentes -> periodo de 22.5 graus
    t = dict(tex, front='mistech:block/s2_crusher_front_on')
    els = body(True) + wheel(4.5, a, 'roda_a') + wheel(11.5, -a, 'roda_b')
    json.dump({'textures': t, 'elements': els}, open(f'{M}/s2_crusher_prev{i}.json', 'w'), indent=1)
t = dict(tex, front='mistech:block/s2_crusher_front')
json.dump({'textures': t, 'elements': body(False) + wheel(4.5, 0, 'a') + wheel(11.5, 0, 'b')}, open(f'{M}/s2_crusher_prev_off.json', 'w'), indent=1)
print('ok')
