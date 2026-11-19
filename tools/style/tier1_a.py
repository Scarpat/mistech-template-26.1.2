"""Estilo A: Triturador Rúnico (ᛏ tiwaz), Extrator de Essência (ᛜ ingwaz), Compressor de Magia (ᚺ hagalaz).
Uso: python tier1_a.py <pasta>"""
import os, sys, json, math
from PIL import Image
from estilo_a_lib import *
from kit import COBRE, AMETISTA, LATAO, FERRO

OUT = sys.argv[1] if len(sys.argv) > 1 else 'out_tier1_a'
os.makedirs(OUT + '/models', exist_ok=True)
def save(name, im): im.save(f'{OUT}/{name}.png')
def model(name, d): json.dump(d, open(f'{OUT}/models/{name}.json', 'w'), indent=1)
M = lambda n: 'mistech:block/' + n

# ============ TRITURADOR RÚNICO: cubo de tijolos escuros, rolos na frente, funil de cobre no topo ============
def crusher():
    side = darken(V('stone_bricks')); strap(side, 8, 14)
    rp = rune_at('tiwaz', 7, 11); engrave(side, rp); save('runic_crusher_side', side)
    bottom = darken(V('stone_bricks')); save('runic_crusher_bottom', bottom)
    # frente: fenda com dois rolos dentados
    fr = darken(V('stone_bricks')); strap(fr, 8, 14)
    for x in range(3, 13):
        fr.putpixel((x, 1), COBRE[5]); fr.putpixel((x, 7), COBRE[1])
    for y in range(1, 8):
        fr.putpixel((3, y), COBRE[4]); fr.putpixel((12, y), COBRE[2])
    g = V('grindstone_side')
    for y in range(2, 7):
        for x in range(4, 12):
            if y == 4: fr.putpixel((x, y), VAZIO); continue
            p = g.getpixel((x, 4 + y)); k = 0.95 if y in (2, 5) else 0.7
            if (x + (y > 4)) % 2 == 0 and y in (3, 5): k *= 0.6        # dentes
            fr.putpixel((x, y), tuple(int(c * k) for c in p[:3]) + (255,))
    save('runic_crusher_front', fr)
    # topo: boca de funil de cobre (aro claro, parede inclinada mais escura, furo no meio)
    top = darken(V('stone_bricks'))
    for y in range(2, 14):
        for x in range(2, 14):
            d = min(x - 2, y - 2, 13 - x, 13 - y)
            if d == 0: t = 5 if (x == 2 or y == 2) else 2
            elif d == 1: t = 4 if (x == 3 or y == 3) else 2
            elif d == 2: t = 2
            else: top.putpixel((x, y), VAZIO); continue
            top.putpixel((x, y), COBRE[t])
    save('runic_crusher_top', top)
    # brilho (acesa)
    gap = [(x, 4) for x in range(4, 12)]
    save_anim(OUT, 'runic_crusher_glow_side', prisma_frames(rp))
    save_anim(OUT, 'runic_crusher_glow_front', prisma_frames([], n=8, extra=[(gap, lambda f, x, y: ROXOS[3 + int(1.5 + 1.5 * math.sin(f * 0.8 + x))])]), 3)
    hole = [(x, y) for y in range(16) for x in range(16) if top.getpixel((x, y)) == VAZIO]
    save_anim(OUT, 'runic_crusher_glow_top', prisma_frames([], n=8, extra=[(hole, lambda f, x, y: ROXOS[1 + int(1 + math.sin(f * 0.8)) + (2 if 6 <= x <= 9 and 6 <= y <= 9 else 0)])]))
    for on in (False, True):
        tex = {'particle': M('runic_crusher_side'), 'front': M('runic_crusher_front'), 'side': M('runic_crusher_side'),
               'top': M('runic_crusher_top'), 'bottom': M('runic_crusher_bottom')}
        els = [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {
            'north': {'texture': '#front', 'cullface': 'north'}, 'south': {'texture': '#side', 'cullface': 'south'},
            'east': {'texture': '#side', 'cullface': 'east'}, 'west': {'texture': '#side', 'cullface': 'west'},
            'up': {'texture': '#top', 'cullface': 'up'}, 'down': {'texture': '#bottom', 'cullface': 'down'}}}]
        if on:
            for k in ('front', 'side', 'top'): tex['glow_' + k] = M('runic_crusher_glow_' + k)
            els += glow_planes({'north': 'glow_front', 'south': 'glow_side', 'east': 'glow_side', 'west': 'glow_side', 'up': 'glow_top'})
        model('runic_crusher' + ('_on' if on else ''), {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': els})

# ============ EXTRATOR DE ESSÊNCIA: caldeirão vanilla com cinta de cobre e runa ingwaz ============
def extractor():
    side = V('cauldron_side'); strap(side, 5, 11, rivets=True)
    rp = rune_at('ingwaz', 7, 8); engrave(side, rp); save('essence_extractor_side', side)
    wall = darken(V('cauldron_side'), 0.85); save('essence_extractor_wall', wall)
    save('essence_extractor_top', V('cauldron_top')); save('essence_extractor_inner', V('cauldron_inner'))
    save('essence_extractor_bottom', V('cauldron_bottom'))
    save_anim(OUT, 'essence_extractor_glow_side', prisma_frames(rp))
    tpl = {0: 'cauldron', 1: 'template_cauldron_level1', 2: 'template_cauldron_level2', 3: 'template_cauldron_full'}
    for lvl, t in tpl.items():
        base = json.load(open(f'{VAN}models/block/{t}.json'))
        els = json.loads(json.dumps(base['elements']))
        for e in els:
            wall_el = e['from'][1] == 3 and e['to'][1] == 16 and e['from'] != [2, 3, 2]
            for fname, fd in e['faces'].items():
                fd.pop('tintindex', None)
                if wall_el and fd.get('texture') == '#side' and 'cullface' not in fd: fd['texture'] = '#wall'
        for on in (False, True):
            tex = {'particle': M('essence_extractor_side'), 'top': M('essence_extractor_top'), 'bottom': M('essence_extractor_bottom'),
                   'side': M('essence_extractor_side'), 'inside': M('essence_extractor_inner'), 'wall': M('essence_extractor_wall'),
                   'content': M('impure_essence_still')}
            e2 = list(els)
            if on:
                tex['glow'] = M('essence_extractor_glow_side')
                e2 = e2 + glow_planes({'north': 'glow', 'south': 'glow', 'east': 'glow', 'west': 'glow'}, (0, 3, 0), (16, 16, 16))
                for g in e2[-4:]:
                    for fd in g['faces'].values(): fd['uv'] = [0, 0, 16, 13]
            name = 'essence_extractor' + (f'_level{lvl}' if lvl else '') + ('_on' if on else '')
            model(name, {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': e2})

# ============ COMPRESSOR DE MAGIA: prensa (mesma forma/hitbox), coluna com cinta e runa hagalaz ============
def compressor():
    stone = darken(V('smooth_stone')); save('magic_compressor_stone', stone)
    col = darken(V('stone_bricks'))
    strap(col, 6, 12, x0=0, x1=15, rivets=False)
    rp = rune_at('hagalaz', 7.5, 9); engrave(col, rp); save('magic_compressor_column', col)
    band = darken(V('stone_bricks'))
    for x in range(16): band.putpixel((x, 3), COBRE[2]); band.putpixel((x, 2), COBRE[4])
    save('magic_compressor_head', band)
    head_top = darken(V('stone_bricks'))
    for i in range(5, 11):
        for (x, y) in ((i, 5), (5, i)): head_top.putpixel((x, y), COBRE[5])
        for (x, y) in ((i, 10), (10, i)): head_top.putpixel((x, y), COBRE[2])
    for y in range(6, 10):
        for x in range(6, 10): head_top.putpixel((x, y), VAZIO)
    save('magic_compressor_head_top', head_top)
    save_anim(OUT, 'magic_compressor_glow_column', prisma_frames(rp))
    core = [(x, y) for y in range(6, 10) for x in range(6, 10)]
    save_anim(OUT, 'magic_compressor_glow_core', prisma_frames([], n=8, extra=[(core, lambda f, x, y: ROXOS[3 + int(1 + math.sin(f * 0.8)) + (1 if 7 <= x <= 8 and 7 <= y <= 8 else 0)])]))
    for filled in (False, True):
        tex = {'particle': M('magic_compressor_stone'), 'stone': M('magic_compressor_stone'), 'col': M('magic_compressor_column'),
               'head': M('magic_compressor_head'), 'htop': M('magic_compressor_head_top'), 'pin': M('t1_pin_copper')}
        S = lambda t, uv=None: ({'texture': '#' + t, 'uv': uv} if uv else {'texture': '#' + t})
        els = [
            {'from': [2, 0, 2], 'to': [14, 2, 14], 'faces': {d: S('stone') for d in ('north', 'south', 'east', 'west', 'up')} | {'down': S('stone')}},
            {'from': [4, 2, 4], 'to': [12, 12, 12], 'faces': {d: S('col', [4, 3, 12, 13]) for d in ('north', 'south', 'east', 'west')}},
            {'from': [2, 12, 2], 'to': [14, 16, 14], 'faces': {d: S('head', [2, 0, 14, 4]) for d in ('north', 'south', 'east', 'west')}
             | {'down': S('stone')}},
        ]
        # tampo com furo no meio: 4 peças em volta + fundo do furo
        ring = [((2, 15.99, 2), (14, 16, 6)), ((2, 15.99, 10), (14, 16, 14)), ((2, 15.99, 6), (6, 16, 10)), ((10, 15.99, 6), (14, 16, 10))]
        for a, b in ring:
            els.append({'from': list(a), 'to': list(b), 'faces': {'up': S('htop', [a[0], a[2], b[0], b[2]])}})
        els.append({'from': [6, 13, 6], 'to': [10, 13, 10], 'faces': {'up': S('htop', [6, 6, 10, 10])}})
        for w in (((6, 13, 6), (10, 16, 6)), ((6, 13, 10), (10, 16, 10)), ((6, 13, 6), (6, 16, 10)), ((10, 13, 6), (10, 16, 10))):
            els.append({'from': list(w[0]), 'to': list(w[1]), 'faces': {('south' if w[0][2] == 6 and w[1][2] == 6 else 'north' if w[0][2] == 10 and w[1][2] == 10 else 'east' if w[0][0] == 6 and w[1][0] == 6 else 'west'): S('stone')}})
        if filled:
            tex['glow_col'] = M('magic_compressor_glow_column'); tex['glow_core'] = M('magic_compressor_glow_core')
            gp = glow_planes({'north': 'glow_col', 'south': 'glow_col', 'east': 'glow_col', 'west': 'glow_col'}, (4, 2, 4), (12, 12, 12))
            for g in gp:
                for fd in g['faces'].values(): fd['uv'] = [4, 3, 12, 13]
            els += gp
            els.append({'from': [6, 13.01, 6], 'to': [10, 13.01, 10], 'shade': False, 'light_emission': 15, 'faces': {'up': S('glow_core', [6, 6, 10, 10])}})
        model('magic_compressor' + ('_filled' if filled else ''), {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': els})
    pin = Image.new('RGBA', (16, 16), COBRE[4]); save('t1_pin_copper', pin)

crusher(); extractor(); compressor()
print('ok')
