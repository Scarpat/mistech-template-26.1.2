"""Confere o renderer do extrator com as mesmas formulas do Java: essencia continua e olho mirando o jogador."""
import sys, os, json, math
os.environ['STYLE_OVERRIDE'] = ''
sys.path.insert(0, 'tools/style'); sys.path.insert(0, 'tools/boitata')
import blockview2 as bv
from PIL import Image, ImageDraw
FLOOR, PER = 12.0, 1.2

def block_quads(lv, lit):
    n = 'essence_extractor' + (f'_level{lv}' if lv else '') + ('_on' if lit else '')
    return bv.quads(bv.load('mistech:block/' + n))

def essence_quad(h):
    m = {'textures': {'e': 'mistech:block/impure_essence_still'},
         'elements': [{'from': [3.5, h - 0.01, 3.5], 'to': [12.5, h, 12.5], 'faces': {'up': {'texture': '#e', 'uv': [3.5, 3.5, 12.5, 12.5]}}}]}
    return bv.quads(m)

EYE = {'textures': {'f': 'mistech:block/essence_extractor_eye_front', 'b': 'mistech:block/essence_extractor_eye_back', 't': 'mistech:block/essence_extractor_eye_top'},
       'elements': [{'from': [5, 13.5, 5], 'to': [11, 19.5, 11], 'faces': {
           'north': {'texture': '#f', 'uv': [5, 5, 11, 11]}, 'south': {'texture': '#b', 'uv': [5, 5, 11, 11]}, 'east': {'texture': '#b', 'uv': [5, 5, 11, 11]},
           'west': {'texture': '#b', 'uv': [5, 5, 11, 11]}, 'up': {'texture': '#t', 'uv': [5, 5, 11, 11]}, 'down': {'texture': '#t', 'uv': [5, 5, 11, 11]}}}]}

def eye_quads(yaw, pitch):
    out = []
    ty, tp = math.radians(yaw), math.radians(pitch)
    for pts, c in bv.quads(EYE):
        np_ = []
        for (x, y, z) in pts:
            X, Y, Z = x + 8 - 8, -y - 16.5, z + 8 - 8          # volta para coordenadas do bloco, centrado no olho
            Y, Z = Y * math.cos(tp) - Z * math.sin(tp), Y * math.sin(tp) + Z * math.cos(tp)   # rotateX
            X, Z = X * math.cos(ty) + Z * math.sin(ty), -X * math.sin(ty) + Z * math.cos(ty)  # rotateY
            np_.append((X, -(Y + 16.5), Z))
        out.append((np_, c))
    return out

def marker(px, py, pz):
    m = {'textures': {'r': 'minecraft:block/red_wool'}, 'elements': [{'from': [px - 1, py - 1, pz - 1], 'to': [px + 1, py + 1, pz + 1],
         'faces': {d: {'texture': '#r'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}]}
    return bv.quads(m)

def aim(cam):  # cam em pixels do bloco
    dx, dy, dz = cam[0] - 8, cam[1] - 16.5, cam[2] - 8
    yaw = math.degrees(math.atan2(-dx, -dz)); pitch = max(-60, min(70, math.degrees(math.atan2(dy, math.hypot(dx, dz)))))
    return yaw, pitch

out = sys.argv[1]
# 1) enchimento continuo
fr = []
for i in range(25):
    t = i / 24 * 3.0
    lv = min(3, int(t)); frac = t - lv if lv < 3 else 0
    h = FLOOR + (lv + frac) * PER
    qs = block_quads(lv, True) + (essence_quad(h) if t > 0 else []) + eye_quads(0, 0)
    v = bv.zview.zdraw(qs, -35, -40, 12)
    b = Image.new('RGBA', v.size, (70, 74, 82, 255)); b.alpha_composite(v)
    ImageDraw.Draw(b).text((6, 6), f'nivel {lv} + {frac:.2f}  (altura {h:.1f} px)', fill=(255, 255, 200))
    fr.append(b.convert('P', palette=Image.ADAPTIVE))
fr[0].save(out + '_enchendo.gif', save_all=True, append_images=fr[1:], duration=120, loop=0)
# 2) olho mirando o marcador (jogador) em varias posicoes; a camera fica fixa para dar para ver
cams = [(8, 18, -16), (30, 18, -10), (30, 18, 8), (24, 34, 24), (8, 40, -12), (-14, 6, -10), (-16, 18, 8), (8, 18, 30)]
views = []
for c in cams:
    yaw, pitch = aim(c)
    qs = block_quads(3, True) + essence_quad(15.6) + eye_quads(yaw, pitch) + marker(*c)
    v = bv.zview.zdraw(qs, -35, -25, 7)
    views.append((v, f'jogador em {c}  yaw {yaw:.0f} pitch {pitch:.0f}'))
cw = max(v.width for v, _ in views); ch = max(v.height for v, _ in views) + 16
S = Image.new('RGBA', (cw * 4, ch * 2), (70, 74, 82, 255)); d = ImageDraw.Draw(S)
for i, (v, l) in enumerate(views):
    x, y = (i % 4) * cw, (i // 4) * ch
    S.alpha_composite(v, (x + (cw - v.width) // 2, y)); d.text((x + 4, y + ch - 14), l, fill=(235, 235, 235))
S.save(out + '_olho_mira.png')
print('ok')
