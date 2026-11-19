"""Conceitos com personalidade (cada um apoiado num objeto vanilla marcante). Prévia: out_conceitos/."""
import os, sys, json, math
from PIL import Image
from estilo_a_lib import *
from kit import COBRE, AMETISTA, LATAO, FERRO

OUT = 'out_conceitos'; os.makedirs(OUT + '/models', exist_ok=True)
def save(n, im): im.save(f'{OUT}/{n}.png')
def model(n, d): json.dump(d, open(f'{OUT}/models/{n}.json', 'w'), indent=1)
M = lambda n: 'mistech:block/' + n
def box(f, t, faces, **kw):
    e = {'from': list(f), 'to': list(t), 'faces': faces}; e.update(kw); return e
def F(tex, uv=None, cull=None):
    d = {'texture': '#' + tex}
    if uv: d['uv'] = uv
    if cull: d['cullface'] = cull
    return d
def all6(tex, skip=(), uvs=None):
    return {f: F(tex, (uvs or {}).get(f)) for f in ('north', 'south', 'east', 'west', 'up', 'down') if f not in skip}
def emit(els):
    for e in els: e['shade'] = False; e['light_emission'] = 15
    return els
def cog(x, cy, cz, tex, r=3.5, w=1.5):
    """Engrenagem de cobre na face leste/oeste (eixo X)."""
    els = []
    for rot in (None, {'origin': [x + w / 2, cy, cz], 'axis': 'x', 'angle': 45}):
        for (a, b) in (((x, cy - r, cz - 1), (x + w, cy + r, cz + 1)), ((x, cy - 1, cz - r), (x + w, cy + 1, cz + r)),
                       ((x, cy - r + 1, cz - r + 1), (x + w, cy + r - 1, cz + r - 1))):
            e = box(a, b, all6(tex))
            if rot: e['rotation'] = rot
            els.append(e)
    return els
DUST = lambda f, x, y: ROXOS[3 + int(1.5 + 1.5 * math.sin(f * 0.8 + x))]
def side_planes(tex, lo, hi, uv):
    gp = glow_planes({'north': tex, 'south': tex, 'east': tex, 'west': tex}, lo, hi)
    for e in gp:
        for fd in e['faces'].values(): fd['uv'] = uv
    return gp

# ---------------- TRITURADOR ----------------
def c1():   # rebolo vanilla montado num soco de pedra com cinta
    side = darken(V('furnace_side')); strap_v(side, 9, 15); rp = rune_at('tiwaz', 7, 12); engrave(side, rp); save('c1_side', side)
    top = darken(V('furnace_top'))
    for y in range(2, 14):
        for x in range(4, 12): top.putpixel((x, y), VAZIO if 5 <= x <= 10 else COBRE[2])
    save('c1_top', top); save_anim(OUT, 'c1_glow', prisma_frames(rp))
    g, gt = vmodel('grindstone', 'g_')
    for on in (0, 1):
        tex = {'particle': M('c1_side'), 'side': M('c1_side'), 'top': M('c1_top')} | gt
        els = [box((0, 0, 0), (16, 8, 16), {f: F('side', None, f) for f in ('north', 'south', 'east', 'west', 'down')} | {'up': F('top')})] + json.loads(json.dumps(g))
        if on:
            tex['glow'] = M('c1_glow')
            els += emit(side_planes('glow', (0, 0, 0), (16, 8, 16), [0, 8, 16, 16]))
        model('c1' + ('_on' if on else ''), {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': els})

def c2():   # britador: alto-forno vanilla com mandíbulas dentadas na frente
    side = darken(V('blast_furnace_side')); strap_v(side, 8, 14); rp = rune_at('tiwaz', 7, 11); engrave(side, rp); save('c2_side', side)
    fr = darken(V('blast_furnace_front'))
    for y in range(6, 13):
        for x in range(3, 13):
            p = fr.getpixel((x, y)); l = 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]
            if l > 75 and (y in (6, 12) or x in (3, 12)): fr.putpixel((x, y), COBRE[4 if y == 6 or x == 3 else 2])
    jaws = []
    for y in range(7, 12):
        for x in range(4, 12):
            mid = 7.5 + (0.6 if (y % 2) else -0.6)
            if abs(x - mid) < 0.9: fr.putpixel((x, y), VAZIO); jaws.append((x, y))
            else: fr.putpixel((x, y), FERRO[5] if (x < mid and x == int(mid - 1)) or (x > mid and x == int(mid + 1)) else FERRO[3])
    save('c2_front', fr); save('c2_top', darken(V('blast_furnace_top')))
    save_anim(OUT, 'c2_glow_side', prisma_frames(rp)); save_anim(OUT, 'c2_glow_front', prisma_frames([], 8, extra=[(jaws, DUST)]), 3)
    for on in (0, 1):
        tex = {'particle': M('c2_side'), 'front': M('c2_front'), 'side': M('c2_side'), 'top': M('c2_top')}
        els = [box((0, 0, 0), (16, 16, 16), {'north': F('front', None, 'north'), 'south': F('side', None, 'south'), 'east': F('side', None, 'east'),
                                              'west': F('side', None, 'west'), 'up': F('top', None, 'up'), 'down': F('top', None, 'down')})]
        if on:
            tex |= {'gs': M('c2_glow_side'), 'gf': M('c2_glow_front')}
            els += emit(glow_planes({'north': 'gf', 'south': 'gs', 'east': 'gs', 'west': 'gs'}))
        model('c2' + ('_on' if on else ''), {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': els})

def c3():   # moinho: corpo de pedra com cinta, funil vanilla em cobre por cima, engrenagens nas laterais
    side = darken(V('stone_bricks')); strap_v(side, 8, 14); rp = rune_at('tiwaz', 7, 11); engrave(side, rp); save('c3_side', side)
    for n in ('hopper_outside', 'hopper_inside', 'hopper_top'):
        im = V(n); px = [(x, y) for y in range(16) for x in range(16) if im.getpixel((x, y))[3]]
        ls = [sum(im.getpixel(p)[:3]) for p in px]; lo, hi = min(ls), max(ls)
        for p, l in zip(px, ls): im.putpixel(p, COBRE[1 + int(4 * (l - lo) / max(1, hi - lo))])
        save('c3_' + n, im)
    save('c3_cog', V('cut_copper')); save_anim(OUT, 'c3_glow', prisma_frames(rp))
    h, ht = vmodel('hopper', 'h_'); h = [e for e in h if e['from'][1] >= 10]
    ht = {k: M('c3_' + v.split('/')[-1]) for k, v in ht.items()}
    for on in (0, 1):
        tex = {'particle': M('c3_side'), 'side': M('c3_side'), 'cog': M('c3_cog')} | ht
        els = [box((0, 0, 0), (16, 10, 16), {f: F('side', [0, 6, 16, 16], f) for f in ('north', 'south', 'east', 'west')} | {'down': F('side', None, 'down')})]
        els += json.loads(json.dumps(h)) + cog(16, 5, 8, 'cog') + cog(-1.5, 5, 8, 'cog')
        if on:
            tex['glow'] = M('c3_glow')
            els += emit(side_planes('glow', (0, 0, 0), (16, 10, 16), [0, 6, 16, 16]))
        model('c3' + ('_on' if on else ''), {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': els})

# ---------------- EXTRATOR ----------------
def cauldron_base(prefix, on=False, extra_tex=None, extra_els=None, glow=None):
    base = json.load(open(f'{VAN}models/block/template_cauldron_level2.json'))
    els = json.loads(json.dumps(base['elements']))
    for e in els:
        for fd in e['faces'].values(): fd.pop('tintindex', None)
    tex = {'particle': M(prefix + '_side'), 'top': 'minecraft:block/cauldron_top', 'bottom': 'minecraft:block/cauldron_bottom',
           'side': M(prefix + '_side'), 'inside': 'minecraft:block/cauldron_inner', 'content': M('impure_essence_still')}
    if extra_tex: tex |= extra_tex
    if extra_els: els += extra_els
    if on and glow:
        tex['glow'] = M(glow)
        els += emit(side_planes('glow', (0, 3, 0), (16, 16, 16), [0, 0, 16, 13]))
    return {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': els}

def e1():   # alambique: tubo de cobre saindo do caldeirão e um balão de vidro com essência pendurado
    side = V('cauldron_side'); strap_v(side, 5, 11); rp = rune_at('ingwaz', 7, 8); engrave(side, rp); save('e1_side', side)
    save_anim(OUT, 'e1_glow', prisma_frames(rp))
    cu = {'cu': 'minecraft:block/cut_copper', 'glass': 'minecraft:block/glass', 'ess': M('impure_essence_still')}
    pipe = [box((7, 12, 14), (9, 22, 16), all6('cu')), box((7, 20, 7), (9, 22, 14), all6('cu')), box((7.5, 18.5, 7.5), (8.5, 20, 8.5), all6('cu')),
            box((5.5, 14, 5.5), (10.5, 18.5, 10.5), all6('glass')), box((6.2, 14.2, 6.2), (9.8, 16.6, 9.8), all6('ess'))]
    for on in (0, 1): model('e1' + ('_on' if on else ''), cauldron_base('e1', on=on, extra_tex=cu, extra_els=json.loads(json.dumps(pipe)), glow='e1_glow'))

def e2():   # olho: caldeirão de obsidiana chorosa com um olho do fim na cinta
    co = V('crying_obsidian'); side = V('cauldron_side')
    for y in range(16):
        for x in range(16):
            if side.getpixel((x, y))[3]: side.putpixel((x, y), co.getpixel((x, y)))
    strap_v(side, 5, 11)
    eye = Image.open(VAN + 'textures/item/ender_eye.png').convert('RGBA')
    pts = []
    for y in range(4, 12):
        for x in range(4, 12):
            p = eye.getpixel((x, y))
            if p[3]:
                tx, ty = 5 + (x - 4) * 6 // 8, 5 + (y - 4) * 6 // 8
                if 6 <= ty <= 10: side.putpixel((tx, ty), p); pts.append((tx, ty))
    save('e2_side', side)
    fr = []
    for f in range(12):
        im = Image.new('RGBA', (16, 16), CLEAR)
        for (x, y) in pts:
            p = side.getpixel((x, y)); k = 1.0 + 0.35 * math.sin(f / 12 * 2 * math.pi)
            im.putpixel((x, y), tuple(min(255, int(c * k)) for c in p[:3]) + (255,))
        fr.append(im)
    save_anim(OUT, 'e2_glow', fr)
    for on in (0, 1): model('e2' + ('_on' if on else ''), cauldron_base('e2', on=on, extra_tex={'top': 'minecraft:block/crying_obsidian'}, glow='e2_glow'))

def e3():   # cristais: caldeirão com cachos de ametista vanilla nos 4 cantos da borda
    side = V('cauldron_side'); strap_v(side, 5, 11); rp = rune_at('ingwaz', 7, 8); engrave(side, rp); save('e3_side', side)
    save_anim(OUT, 'e3_glow', prisma_frames(rp))
    for on in (0, 1):
        cl = []
        for (cx, cz) in ((1.5, 1.5), (14.5, 1.5), (1.5, 14.5), (14.5, 14.5)):
            for ang in (45, -45):
                e = box((cx - 2.5, 16, cz), (cx + 2.5, 21, cz), {'north': F('cl', [0, 0, 16, 16]), 'south': F('cl', [0, 0, 16, 16])},
                        rotation={'origin': [cx, 16, cz], 'axis': 'y', 'angle': ang})
                if on: e['shade'] = False; e['light_emission'] = 12
                cl.append(e)
        model('e3' + ('_on' if on else ''), cauldron_base('e3', on=on, extra_tex={'cl': 'minecraft:block/amethyst_cluster'}, extra_els=cl, glow='e3_glow'))

# ---------------- COMPRESSOR ----------------
def p1():   # bigorna vanilla com a runa embutida no tampo e cinta no pescoço
    a, at = vmodel('template_anvil', 'a_')
    top = darken(V('anvil_top'), 0.9, only_grey=False); rp = rune_at('hagalaz', 7.5, 7.5)
    for (x, y) in rp: top.putpixel((x, y), COBRE[1])
    save('p1_top', top); neck = darken(V('anvil'), 0.9, only_grey=False); strap_v(neck, 4, 10, rivets=False); save('p1_neck', neck)
    save_anim(OUT, 'p1_glow', prisma_frames(rp))
    for on in (0, 1):
        els = json.loads(json.dumps(a)); tex = {'particle': 'minecraft:block/anvil', 'a_body': 'minecraft:block/anvil', 'top': M('p1_top'), 'neck': M('p1_neck')}
        els[3]['faces']['up'] = F('top', [3, 0, 13, 16])
        for f in ('north', 'south', 'east', 'west'):
            if f in els[2]['faces']: els[2]['faces'][f] = F('neck', [6, 4, 10, 9])
        if on:
            tex['glow'] = M('p1_glow')
            els += emit([box((3, 16.01, 0), (13, 16.01, 16), {'up': F('glow', [3, 0, 13, 16])})])
        model('p1' + ('_filled' if on else ''), {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': els})

def p2():   # prensa de parafuso: matriz de pedra com cinta/runa, colunas e travessa de cobre, fuso de ferro e volante
    die = darken(V('stone_bricks')); strap_v(die, 9, 15, rivets=False); rp = rune_at('hagalaz', 7.5, 12); engrave(die, rp); save('p2_die', die)
    save_anim(OUT, 'p2_glow', prisma_frames(rp))
    for on in (0, 1):
        tex = {'particle': M('p2_die'), 'die': M('p2_die'), 'stone': 'minecraft:block/smooth_stone', 'cu': 'minecraft:block/cut_copper',
               'iron': 'minecraft:block/iron_block', 'dark': 'minecraft:block/polished_blackstone'}
        els = [box((1, 0, 1), (15, 2, 15), all6('stone')),
               box((3, 2, 3), (13, 9, 13), {f: F('die', [3, 7, 13, 14]) for f in ('north', 'south', 'east', 'west')} | {'up': F('dark')}),
               box((1.5, 2, 6.5), (3, 15, 9.5), all6('cu')), box((13, 2, 6.5), (14.5, 15, 9.5), all6('cu')),
               box((1.5, 13, 6), (14.5, 15, 10), all6('cu')), box((7, 9, 7), (9, 18, 9), all6('iron')),
               box((6, 9, 6), (10, 10.5, 10), all6('iron'))]
        for rot in (None, {'origin': [8, 18.5, 8], 'axis': 'y', 'angle': 45}):
            for (a, b) in (((4, 18, 7.5), (12, 19, 8.5)), ((7.5, 18, 4), (8.5, 19, 12))):
                e = box(a, b, all6('cu'))
                if rot: e['rotation'] = rot
                els.append(e)
        if on:
            tex['glow'] = M('p2_glow')
            els += emit(side_planes('glow', (3, 2, 3), (13, 9, 13), [3, 7, 13, 14]))
        model('p2' + ('_filled' if on else ''), {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': els})

for f in (c1, c2, c3, e1, e2, e3, p1, p2): f()
print('ok')
