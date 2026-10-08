"""Visual dos itens do Mago: Cajado do Mago em 3D (na mão) + ícones (cajado, Pingente do Arcano, ovo).
Uso (na pasta do projeto): python tools/tarot/magus/gen_magus_items.py [pasta_previa]
Depende de tools/artifacts/boxmodel.py."""
import sys, os, json, math
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', 'artifacts'))
from PIL import Image
from boxmodel import Model, hexc, solid, bands, per_face, write_model

A = 'src/main/resources/assets/mistech/'
T = A + 'textures/'
PREV = sys.argv[1] if len(sys.argv) > 1 else None

IVORY_D, IVORY, IVORY_L = hexc('BDB39C'), hexc('EDE6D2'), hexc('FFFBEE')
GOLD_D, GOLD, GOLD_L = hexc('8A5A0E'), hexc('E8B830'), hexc('FFF0A0')
RED_D, RED, RED_L = hexc('6E1418'), hexc('B8282E'), hexc('E86A6A')
EARTH, AIR, WATER, FIRE = hexc('6FB83A'), hexc('E8F8FF'), hexc('3AB8F0'), hexc('FF6A1A')
EARTH_D, AIR_D, WATER_D, FIRE_D = hexc('3E7A1E'), hexc('A8C8D8'), hexc('1A6E9A'), hexc('B8300A')
PRISM, PRISM_L = hexc('C8A8FF'), hexc('F4ECFF')
OUT = hexc('2A1A10')


def staff():
    m = Model('magician_staff_3d', 64, seed=11)
    # haste de marfim com faixas de ouro e empunhadura vermelha
    m.box((7.4, -8, 7.4), (8.6, 17, 8.6), bands([IVORY, IVORY_L, IVORY, IVORY, IVORY_D, GOLD]), name='haste')
    m.box((7.2, 0, 7.2), (8.8, 6, 8.8), bands([RED, RED_L, RED, RED_D]), name='empunhadura')
    m.box((7.0, -10, 7.0), (9.0, -8, 9.0), solid(GOLD, 6, top=GOLD_L, bottom=GOLD_D), name='ponteira')
    m.box((7.5, -11, 7.5), (8.5, -10, 8.5), solid(GOLD_L, 4), name='ponta')
    m.box((6.4, 17, 6.4), (9.6, 18.5, 9.6), solid(GOLD, 6, top=GOLD_L, bottom=GOLD_D), name='colar')
    # o ∞ de ouro no alto (dois anéis lado a lado) com um prisma no meio
    gold = solid(GOLD, 6, top=GOLD_L, bottom=GOLD_D)
    for side in (-1, 1):
        cx = 8 + side * 2.6
        m.box((cx - 2.2, 18.5, 7.5), (cx + 2.2, 19.3, 8.5), gold, name='inf_b%d' % side)
        m.box((cx - 2.2, 23.2, 7.5), (cx + 2.2, 24.0, 8.5), gold, name='inf_t%d' % side)
        x_out = cx + side * 2.2
        m.box((min(x_out, x_out - side * 0.8), 19.3, 7.5), (max(x_out, x_out - side * 0.8), 23.2, 8.5), gold, name='inf_o%d' % side)
    m.box((7.0, 19.8, 7.0), (9.0, 22.6, 9.0), per_face(solid(PRISM, 10), up=solid(PRISM_L, 6)), name='prisma')
    # quatro gemas dos elementos em volta do prisma
    for i, (c, cd) in enumerate(((EARTH, EARTH_D), (AIR, AIR_D), (WATER, WATER_D), (FIRE, FIRE_D))):
        a = i * math.pi / 2 + math.pi / 4
        x, z = 8 + math.cos(a) * 2.4, 8 + math.sin(a) * 2.4
        m.box((x - 0.6, 24.2, z - 0.6), (x + 0.6, 25.4, z + 0.6), solid(c, 6, bottom=cd), name='gema%d' % i)
    m.box((7.6, 24.0, 7.6), (8.4, 26.5, 8.4), solid(GOLD_L, 4), name='agulha')
    return m


def tool_display(grip, scale=0.85, fp_scale=0.68):
    t3 = round((8 - grip) * scale, 2)
    t1 = round(2.2 + (8 - grip) * fp_scale * 0.6, 2)
    return {
        'thirdperson_righthand': {'rotation': [0, 90, -10], 'translation': [0, t3, 0.5], 'scale': [scale] * 3},
        'thirdperson_lefthand': {'rotation': [0, -90, 10], 'translation': [0, t3, 0.5], 'scale': [scale] * 3},
        'firstperson_righthand': {'rotation': [0, -90, 15], 'translation': [1.13, t1, 1.13], 'scale': [fp_scale] * 3},
        'firstperson_lefthand': {'rotation': [0, 90, -15], 'translation': [1.13, t1, 1.13], 'scale': [fp_scale] * 3},
        'gui': {'rotation': [15, -30, -45], 'scale': [0.5] * 3},
        'ground': {'translation': [0, 2, 0], 'scale': [0.4] * 3},
        'fixed': {'rotation': [0, 0, -45], 'scale': [0.6] * 3},
    }


m = staff()
write_model(m, A, tool_display(3.0, 0.8, 0.6), 'magician_staff_model')
os.makedirs(A + 'items', exist_ok=True)
json.dump({'model': {'type': 'minecraft:select', 'property': 'minecraft:display_context',
                     'cases': [{'when': ['gui', 'ground', 'fixed', 'on_shelf'],
                                'model': {'type': 'minecraft:model', 'model': 'mistech:item/magician_staff'}}],
                     'fallback': {'type': 'minecraft:model', 'model': 'mistech:item/magician_staff_3d'}}},
          open(A + 'items/magician_staff.json', 'w'), indent=2)
if PREV:
    os.makedirs(PREV, exist_ok=True)
    m.preview(PREV + '/magician_staff_3d.png', yaws=(35, 125, 215), pitch=20, scale=6, fit=48)

# ================================================================== ícones 16x16
def icon(rows, pal):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                p[x, y] = pal[ch]
    return im

pal = {'o': OUT, 'g': GOLD, 'G': GOLD_D, 'l': GOLD_L, 'i': IVORY, 'I': IVORY_D, 'w': IVORY_L, 'r': RED, 'R': RED_D,
       'p': PRISM, 'P': PRISM_L, 'e': EARTH, 'a': AIR, 'u': WATER, 'f': FIRE}
icon([
    "..........e.a...",
    "........gggggg..",
    ".......g.gPpg.g.",
    ".......g.gppg.g.",
    "........ggggggf.",
    "........u.gl....",
    "..........i.....",
    ".........w......",
    "........i.......",
    ".......r........",
    "......R.........",
    ".....i..........",
    "....I...........",
    "...g............",
    "..G.............",
    "................"], pal).save(T + 'item/magician_staff.png')

icon([
    "...G........G...",
    "....g......g....",
    ".....g....g.....",
    "......g..g......",
    ".......gg.......",
    "......oGGo......",
    ".....oglllgo....",
    "....oge.l.agoo..",
    "....oglPlPlgo...",
    "....ogPlPlPgo...",
    "....ogu.l.fgo...",
    ".....oglllgo....",
    "......oGGo......",
    ".......oo.......",
    "................",
    "................"], pal).save(T + 'item/arcane_pendant.png')
print('ok')
