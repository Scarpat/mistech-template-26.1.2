#!/usr/bin/env python3
"""Pendências pós-Fase 9: modelos novos (Sangria Arcana "dark", Relógio Astral em torre, Alambique, Bomba do Boitatá)
e as texturas que eles usam. Idempotente.
Uso (na pasta do projeto): python3 tools/pendencias/gen_models.py"""
import json, math, os
from PIL import Image

A = 'src/main/resources/assets/mistech/'
T = A + 'textures/block/'
V = 'vanilla/assets/minecraft/textures/block/'


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write('\n')


def hx(h, a=255):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def mix(a, b, t):
    return tuple(round(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (255,)


def lum(p):
    return (p[0] * 0.3 + p[1] * 0.59 + p[2] * 0.11) / 255


def recolor(src, dark, light, out, frames=None):
    im = Image.open(src).convert('RGBA')
    w, h = im.size
    o = im.copy()
    for y in range(h):
        for x in range(w):
            p = im.getpixel((x, y))
            if p[3]:
                c = mix(hx(dark), hx(light), lum(p))
                o.putpixel((x, y), c[:3] + (p[3],))
    o.save(T + out + '.png')
    meta = src + '.mcmeta'
    if os.path.exists(meta):
        open(T + out + '.png.mcmeta', 'w').write(open(meta).read())


# ---------------------------------------------------------------- texturas
# Sangria: líquido carmesim escuro e glifo vermelho
recolor(T + 'mana_well_mana.png', '1a0208', '6a0a1e', 'mana_siphon_blood')
recolor(T + 'mana_well_mana_on.png', '2a0410', 'e0284a', 'mana_siphon_blood_on')
recolor(T + 'mana_siphon_rune.png', '0a0608', '4a1018', 'mana_siphon_rune_dark')
recolor(T + 'mana_siphon_rune_on.png', '120408', 'ff3050', 'mana_siphon_rune_dark_on')
# Poço: mana líquida mais visível (já existe mana_well_mana)
# Bomba do Boitatá: lava
# (usa texturas vanilla)


# ---------------------------------------------------------------- modelos
def auto(f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
            'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
            'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}


def clampuv(uv):
    return [max(0, min(16, v)) for v in uv]


def cube(f, t, tex, skip=(), light=None, rot=None):
    uv = auto(f, t)
    faces = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        if d in skip:
            continue
        tx = tex[d] if isinstance(tex, dict) and d in tex else (tex.get('*') if isinstance(tex, dict) else tex)
        faces[d] = {'texture': tx, 'uv': clampuv(uv[d])}
    e = {'from': f, 'to': t, 'faces': faces}
    if light:
        e['light_emission'] = light
    if rot:
        e['rotation'] = rot
    return e


def model(textures, els, render=None):
    m = {'parent': 'minecraft:block/block', 'textures': textures, 'elements': els}
    if render:
        m['render_type'] = render
    return m


def siphon(on):
    """Altar de pedra-negra com espinhos nos cantos, coluna de obsidiana chorona com glifo vermelho e taça de netherite com sangue."""
    rune = 'mistech:block/mana_siphon_rune_dark' + ('_on' if on else '')
    blood = 'mistech:block/mana_siphon_blood' + ('_on' if on else '')
    els = [
        cube([1, 0, 1], [15, 3, 15], {'*': '#bricks', 'up': '#chiseled', 'down': '#stone'}),
        cube([4, 3, 4], [12, 4, 12], '#stone'),
        cube([5, 4, 5], [11, 10, 11], {'*': '#rune', 'up': '#crying', 'down': '#crying'}, light=9 if on else None),
        cube([4.5, 9, 4.5], [11.5, 10, 11.5], '#netherite'),
        cube([6, 10, 6], [10, 11, 10], '#netherite'),
        # taça
        cube([3, 11, 3], [13, 12, 13], '#netherite'),
        cube([2, 12, 2], [14, 15, 3], '#netherite'), cube([2, 12, 13], [14, 15, 14], '#netherite'),
        cube([2, 12, 3], [3, 15, 13], '#netherite'), cube([13, 12, 3], [14, 15, 13], '#netherite'),
        cube([3, 12, 3], [13, 14 if on else 13, 13], '#blood', skip=('north', 'south', 'east', 'west', 'down'), light=12 if on else None),
    ]
    # espinhos nos cantos (dois degraus, o de cima girado)
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        els.append(cube([x, 3, z], [x + 2, 7, z + 2], '#spike'))
        els.append(cube([x + 0.5, 7, z + 0.5], [x + 1.5, 10, z + 1.5], '#spike'))
    return model({'particle': 'minecraft:block/polished_blackstone', 'bricks': 'minecraft:block/polished_blackstone_bricks',
                  'chiseled': 'minecraft:block/chiseled_polished_blackstone', 'stone': 'minecraft:block/polished_blackstone',
                  'crying': 'minecraft:block/crying_obsidian', 'netherite': 'minecraft:block/netherite_block',
                  'spike': 'minecraft:block/blackstone', 'rune': rune, 'blood': blood}, els)


def clock(mode):
    """Torrezinha astral: base de lápis, corpo com o mostrador do modo na frente, pilares e telhado de ouro, orbe no topo."""
    els = [
        cube([1, 0, 1], [15, 2, 15], {'*': '#lapis', 'up': '#side'}),
        cube([3, 2, 3], [13, 12, 13], {'*': '#side', 'north': '#front'}, light=6),
        cube([2, 12, 2], [14, 13, 14], '#gold'),
        cube([4, 13, 4], [12, 14, 12], '#lapis'),
        cube([7, 14, 7], [9, 15, 9], '#gold'),
        cube([6.5, 15, 6.5], [9.5, 17, 9.5], '#orb', light=15),
        # aro do mostrador
        cube([3.5, 11, 2.5], [12.5, 11.5, 3], '#gold'), cube([3.5, 2.5, 2.5], [12.5, 3, 3], '#gold'),
    ]
    for x, z in ((2, 2), (12, 2), (2, 12), (12, 12)):
        els.append(cube([x, 2, z], [x + 2, 12, z + 2], '#gold'))
    orb = {'moon': 'minecraft:block/sea_lantern', 'event': 'minecraft:block/amethyst_block', 'day': 'minecraft:block/glowstone',
           'taint': 'minecraft:block/crying_obsidian'}[mode]
    return model({'particle': 'mistech:block/astral_clock_side', 'side': 'mistech:block/astral_clock_side',
                  'front': 'mistech:block/astral_clock_front_' + mode, 'lapis': 'minecraft:block/lapis_block',
                  'gold': 'minecraft:block/gold_block', 'orb': orb}, els)


def still(on):
    """Alambique: fornalha de tijolo com fogo, panela de cobre bojuda, pescoço de cisne até um frasco de vidro com a poção."""
    front = 'minecraft:block/blast_furnace_front_on' if on else 'minecraft:block/blast_furnace_front'
    els = [
        cube([1, 0, 1], [15, 5, 15], {'*': '#brick', 'north': '#front', 'up': '#stone'}, light=8 if on else None),
        # panela
        cube([3, 5, 2], [11, 11, 10], '#copper'),
        cube([2.5, 6, 1.5], [11.5, 10, 10.5], '#copper'),
        cube([4, 11, 3], [10, 12, 9], '#cut'),
        cube([5.5, 12, 4.5], [8.5, 14, 7.5], '#copper'),
        cube([5, 14, 4], [9, 15, 8], '#cut'),
        # pescoço de cisne até o frasco
        cube([8.5, 13, 5.5], [13.5, 14, 6.5], '#copper'),
        cube([12.5, 13, 6.5], [13.5, 14, 11.5], '#copper'),
        cube([12.5, 9, 10.5], [13.5, 13, 11.5], '#copper'),
        # frasco de vidro com a poção
        cube([11, 5, 9], [15, 9, 13], '#glass'),
        cube([11.5, 5.01, 9.5], [14.5, 7.5 if on else 6, 12.5], '#potion', skip=('down',)),
        cube([12, 9, 10], [14, 10, 12], '#glass'),
        # rebites
        cube([2.4, 9, 4], [2.6, 9.5, 4.5], '#cut'), cube([2.4, 7, 7], [2.6, 7.5, 7.5], '#cut'),
    ]
    return model({'particle': 'minecraft:block/copper_block', 'brick': 'minecraft:block/bricks', 'front': front,
                  'stone': 'minecraft:block/smooth_stone', 'copper': 'minecraft:block/copper_block', 'cut': 'minecraft:block/cut_copper',
                  'glass': 'minecraft:block/glass', 'potion': 'minecraft:block/purple_stained_glass' if on else 'minecraft:block/pink_stained_glass'},
                 els, 'minecraft:translucent')


def lava_pump(on):
    """Bomba do Boitatá: base de pedra-negra, corpo de basalto com faixas de ouro, bacia com lava."""
    surface = 'minecraft:block/lava_still' if on else 'minecraft:block/magma'
    return model({'particle': 'minecraft:block/polished_blackstone', 'base': 'minecraft:block/polished_blackstone_bricks',
                  'body': 'minecraft:block/polished_basalt_side', 'top': 'minecraft:block/polished_basalt_top',
                  'gold': 'minecraft:block/gold_block', 'lava': surface, 'dark': 'minecraft:block/nether_bricks'}, [
        cube([1, 0, 1], [15, 4, 15], '#base'),
        cube([5, 4, 5], [11, 11, 11], {'*': '#body', 'up': '#top', 'down': '#top'}),
        cube([4.5, 6, 4.5], [11.5, 7, 11.5], '#gold'),
        cube([4.5, 9, 4.5], [11.5, 10, 11.5], '#gold'),
        cube([2, 11, 2], [14, 12, 14], '#dark'),
        cube([2, 12, 2], [14, 16, 3], '#dark'), cube([2, 12, 13], [14, 16, 14], '#dark'),
        cube([2, 12, 3], [3, 16, 13], '#dark'), cube([13, 12, 3], [14, 16, 13], '#dark'),
        cube([3, 12, 3], [13, 14.5 if on else 13.5, 13], '#lava', skip=('north', 'south', 'east', 'west', 'down'), light=15 if on else 6),
        cube([11, 6, 7], [15.5, 8, 9], '#gold'),
    ])


for on in (False, True):
    suffix = '_on' if on else ''
    dump(A + 'models/block/mana_siphon%s.json' % suffix, siphon(on))
    dump(A + 'models/block/potion_still%s.json' % suffix, still(on))
    dump(A + 'models/block/boitata_pump%s.json' % suffix, lava_pump(on))
for mode in ('moon', 'event', 'day', 'taint'):
    dump(A + 'models/block/astral_clock_%s.json' % mode, clock(mode))
bs = {'variants': {}}
for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
    for lit in ('false', 'true'):
        v = {'model': 'mistech:block/boitata_pump%s' % ('_on' if lit == 'true' else '')}
        if r:
            v['y'] = r
        bs['variants']['facing=%s,lit=%s' % (f, lit)] = v
dump(A + 'blockstates/boitata_pump.json', bs)
dump(A + 'items/boitata_pump.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:block/boitata_pump'}})
print('ok')


# ---------------------------------------------------------------- Conduto Rúnico (multipart)
def van(n):
    return Image.open(V + n + '.png').convert('RGBA').crop((0, 0, 16, 16))


def tint(im, dark, light):
    out = im.copy()
    ls = [lum(im.getpixel((x, y))) for y in range(16) for x in range(16)]
    a, b = min(ls), max(ls)
    for y in range(16):
        for x in range(16):
            t = (lum(im.getpixel((x, y))) - a) / ((b - a) or 1)
            out.putpixel((x, y), mix(hx(dark), hx(light), t))
    return out


# Níveis: cano (escuro, claro, brilho, frisos) e a faixa/anel de cada metal
TIERS = {
    'runic_conduit': ('4a2410', 'c8784a', 'e8a070', 'f0c070', 'minecraft:block/cut_copper'),
    'mithril_conduit': ('1e3a58', '7fc0e8', 'c8f0ff', 'ffffff', 'mistech:block/mithril_block'),
    'adamantine_conduit': ('3a0610', 'c03440', 'ff8a8a', 'ffd0d0', 'mistech:block/adamantine_block'),
    'orichalcum_conduit': ('5a3606', 'e8a830', 'fff0b0', 'ffffff', 'mistech:block/orichalcum_block'),
}
ROT = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}

# runa do miolo: branca/cinza, pintada no jogo com a cor do que o conduto leva (tintindex 0)
# (textura sólida: a runa é feita de geometria, 3 barras em cruz saindo 0,01 do miolo; sem transparência)
glow = Image.new('RGBA', (16, 16), hx('ffffff'))
glow.save(T + 'runic_conduit_glow.png')

# tampão desligado (igual para todos)
off = tint(van('polished_deepslate'), '141014', '3a2a2a')
for k in range(4, 12):
    off.putpixel((k, k), hx('c83a2a'))
    off.putpixel((15 - k, k), hx('c83a2a'))
off.save(T + 'runic_conduit_off.png')


def arm_faces(el):
    """Braço ao longo de z: as 4 faces compridas usam as linhas 6..9 da textura, com o canal correndo ao longo do cano."""
    z0, z1 = el['from'][2], el['to'][2]
    for d in ('east', 'west', 'up', 'down'):
        if d in el['faces']:
            el['faces'][d]['texture'] = '#pipe'
            el['faces'][d]['uv'] = [z0, 6, z1, 10]
            if d in ('up', 'down'):
                el['faces'][d]['rotation'] = 90
    for d in ('north', 'south'):
        if d in el['faces']:
            el['faces'][d]['texture'] = '#core'
    return el


for name, (dark, light, shine, mark, band) in TIERS.items():
    # cano: metal com um canal no meio por onde correm pulsos de runa (animado; o cano usa as linhas 6..9)
    base = tint(van('cut_copper'), dark, light)
    frames = []
    for f in range(8):
        im = base.copy()
        for x in range(16):
            im.putpixel((x, 6), hx(shine) if x % 8 else hx(mark))
            im.putpixel((x, 9), mix(hx(dark), hx(light), 0.3) if x % 8 else hx(mark))
            for y in (7, 8):
                im.putpixel((x, y), hx('123a48'))
            k = (x - f * 2) % 8
            if k in (0, 1):
                glow = hx('bff6ff') if k == 0 else hx('5fd8f0')
                im.putpixel((x, 7), glow)
                im.putpixel((x, 8), glow)
        frames.append(im)
    strip = Image.new('RGBA', (16, 16 * len(frames)))
    for k, im in enumerate(frames):
        strip.paste(im, (0, 16 * k))
    strip.save(T + name + '.png')
    open(T + name + '.png.mcmeta', 'w').write('{\n  "animation": {\n    "frametime": 2\n  }\n}\n')
    # miolo (região 5..10): moldura do metal, pedra funda e a runa acesa
    core = tint(van('polished_deepslate'), '0e0e14', '2a2a34')
    for k in range(5, 11):
        for (x, y) in ((k, 5), (k, 10), (5, k), (10, k)):
            core.putpixel((x, y), hx(light))
    for (x, y) in ((5, 5), (10, 5), (5, 10), (10, 10)):
        core.putpixel((x, y), hx(mark))
    core.save(T + name + '_core.png')

    TX = {'particle': 'mistech:block/%s_core' % name, 'pipe': 'mistech:block/' + name, 'core': 'mistech:block/%s_core' % name,
          'band': band, 'gold': 'minecraft:block/gold_block', 'lapis': 'minecraft:block/lapis_block',
          'off': 'mistech:block/runic_conduit_off', 'dark': 'minecraft:block/polished_deepslate', 'glow': 'mistech:block/runic_conduit_glow'}
    glows = [cube([7, 7, 4.99], [9, 9, 11.01], '#glow', light=12, skip=('east', 'west', 'up', 'down')),
             cube([4.99, 7, 7], [11.01, 9, 9], '#glow', light=12, skip=('north', 'south', 'up', 'down')),
             cube([7, 4.99, 7], [9, 11.01, 9], '#glow', light=12, skip=('north', 'south', 'east', 'west'))]
    for el in glows:
        for face in el['faces'].values():
            face['tintindex'] = 0
    dump(A + 'models/block/%s_core.json' % name, model(TX, [cube([5, 5, 5], [11, 11, 11], '#core', light=4)] + glows))
    dump(A + 'models/block/%s_arm.json' % name, model(TX, [
        arm_faces(cube([6, 6, 0], [10, 10, 5], '#pipe', skip=('south',))),
        cube([5.5, 5.5, 4], [10.5, 10.5, 5], '#band', skip=('south',))]))
    dump(A + 'models/block/%s_link.json' % name, model(TX, [
        arm_faces(cube([6, 6, 1.5], [10, 10, 5], '#pipe', skip=('south',))),
        cube([5.5, 5.5, 4], [10.5, 10.5, 5], '#band', skip=('south',)),
        cube([4.5, 4.5, 0], [11.5, 11.5, 1.5], {'*': '#band', 'north': '#dark'})]))
    dump(A + 'models/block/%s_pull.json' % name, model(TX, [
        arm_faces(cube([6, 6, 2.5], [10, 10, 5], '#pipe', skip=('south',))),
        cube([5.5, 5.5, 4], [10.5, 10.5, 5], '#gold', skip=('south',)),
        cube([4, 4, 0], [12, 12, 2.5], {'*': '#gold', 'north': '#lapis'}),
        cube([7, 3, 0.5], [9, 4, 2], '#gold'), cube([7, 12, 0.5], [9, 13, 2], '#gold'),
        cube([3, 7, 0.5], [4, 9, 2], '#gold'), cube([12, 7, 0.5], [13, 9, 2], '#gold')]))
    dump(A + 'models/block/%s_off.json' % name, model(TX, [cube([6, 6, 3.5], [10, 10, 5], {'*': '#dark', 'north': '#off'}, skip=('south',))]))
    parts = [{'apply': {'model': 'mistech:block/%s_core' % name}}]
    for d, r in ROT.items():
        for side, m in (('conduit', 'arm'), ('link', 'link'), ('pull', 'pull'), ('off', 'off')):
            apply = {'model': 'mistech:block/%s_%s' % (name, m)}
            apply.update(r)
            parts.append({'when': {d: side}, 'apply': apply})
    dump(A + 'blockstates/%s.json' % name, {'multipart': parts})
    dump(A + 'models/item/%s.json' % name, {'parent': 'minecraft:block/block', 'textures': TX, 'elements': [
        cube([5, 5, 5], [11, 11, 11], '#core', light=6), cube([7, 7, 4.99], [9, 9, 11.01], '#glow'), cube([4.99, 7, 7], [11.01, 9, 9], '#glow'),
        cube([7, 4.99, 7], [9, 11.01, 9], '#glow'),
        arm_faces(cube([6, 6, 0], [10, 10, 5], '#pipe')), arm_faces(cube([6, 6, 11], [10, 10, 16], '#pipe')),
        cube([4.5, 4.5, 0], [11.5, 11.5, 1.5], {'*': '#band', 'north': '#dark'}),
        cube([4.5, 4.5, 14.5], [11.5, 11.5, 16], {'*': '#band', 'south': '#dark'})]})
    dump(A + 'items/%s.json' % name, {'model': {'type': 'minecraft:model', 'model': 'mistech:item/' + name}})
print('conduto ok')


# ---------------------------------------------------------------- Poção de Mana (garrafa vanilla com o líquido azul da mana)
bottle = Image.open('vanilla/assets/minecraft/textures/item/potion.png').convert('RGBA')
over = Image.open('vanilla/assets/minecraft/textures/item/potion_overlay.png').convert('RGBA')
out = bottle.copy()
for y in range(16):
    for x in range(16):
        p = over.getpixel((x, y))
        if p[3]:
            c = mix(hx('1a4ad8'), hx('9fe8ff'), lum(p))
            out.putpixel((x, y), c)
out.save(A + 'textures/item/mana_potion.png')
print('poção ok')
