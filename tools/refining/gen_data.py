#!/usr/bin/env python3
"""Dados da cadeia de minério (Fase 3): receitas (refino, trituração, craft, ritual), modelos de item/bloco, blockstates,
loot, tags, textos e páginas do Grimório. Idempotente. Depois rode tools/guide/views.py.
Uso (na pasta do projeto): python3 tools/refining/gen_data.py"""
import json, os
from PIL import Image

R = 'src/main/resources/'
A = R + 'assets/mistech/'
D = R + 'data/'
METALS = ['iron', 'gold', 'copper', 'mithril', 'adamantine', 'orichalcum']
CROSS = {'iron': 'mithril', 'gold': 'orichalcum', 'copper': 'adamantine'}
NAMES_PT = {'iron': 'Ferro', 'gold': 'Ouro', 'copper': 'Cobre', 'mithril': 'Mitril', 'adamantine': 'Adamantina', 'orichalcum': 'Oricalco'}
NAMES_EN = {'iron': 'Iron', 'gold': 'Gold', 'copper': 'Copper', 'mithril': 'Mithril', 'adamantine': 'Adamantine', 'orichalcum': 'Orichalcum'}


def dump(path, obj, newline=True):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        if newline:
            f.write('\n')


# ---------------------------------------------------------------- receitas de refino e trituração
for m in METALS:
    ore = '#c:ores/' + m
    bp = lambda item, chance: {'item': {'id': item}, 'chance': chance}
    cross = [bp('mistech:%s_dust' % CROSS[m], 0.05)] if m in CROSS else []
    dump(D + 'mistech/recipe/refining/%s_washing.json' % m, {
        'type': 'mistech:ore_refining', 'stage': 'washing', 'ingredient': ore,
        'result': {'id': 'mistech:%s_clump' % m, 'count': 3}, 'byproducts': [bp('mistech:tainted_residue', 0.10)]})
    dump(D + 'mistech/recipe/refining/%s_crystallizing.json' % m, {
        'type': 'mistech:ore_refining', 'stage': 'crystallizing', 'ingredient': ore,
        'result': {'id': 'mistech:%s_ore_crystal' % m, 'count': 4}, 'byproducts': [bp('mistech:magic_residue', 0.15)] + cross})
    dump(D + 'mistech/recipe/refining/%s_transmutation.json' % m, {
        'type': 'mistech:ore_refining', 'stage': 'transmutation', 'ingredient': ore,
        'result': {'id': 'mistech:%s_dust' % m, 'count': 5}, 'byproducts': cross})
    for kind in ('clump', 'ore_crystal'):
        dump(D + 'mistech/recipe/crushing/%s_%s.json' % (m, kind), {
            'type': 'mistech:crushing', 'ingredient': 'mistech:%s_%s' % (m, kind),
            'result': {'id': 'mistech:%s_dust' % m, 'count': 1}})

# ---------------------------------------------------------------- craft e ritual
dump(D + 'mistech/recipe/essence_washer.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['IGI', 'RTR', 'IBI'],
    'key': {'I': 'minecraft:iron_ingot', 'G': '#c:glass_blocks', 'R': 'mistech:water_rune', 'T': 'mistech:runic_tank', 'B': 'mistech:runic_crusher'},
    'result': {'id': 'mistech:essence_washer', 'count': 1}})
dump(D + 'mistech/recipe/runic_crystallizer.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['GLG', 'PCP', 'OMO'],
    'key': {'G': '#c:glass_blocks', 'L': 'mistech:light_rune', 'P': 'mistech:pure_magic_shard', 'C': 'mistech:crystallizer',
            'O': 'mistech:orichalcum_ingot', 'M': 'mistech:mithril_ingot'},
    'result': {'id': 'mistech:runic_crystallizer', 'count': 1}})
RITUAL = {'type': 'mistech:ritual', 'catalyst': 'mistech:runic_crystallizer',
          'ingredients': ['mistech:star_fragment', 'mistech:star_fragment', 'mistech:space_rune', 'mistech:light_rune',
                          'mistech:orichalcum_ingot', 'mistech:orichalcum_ingot'],
          'essence': 6, 'duration': 600, 'condition': 'clear_night', 'min_tier': 2,
          'result': {'id': 'mistech:astral_prism'}, 'effect': 'stars'}
dump(D + 'mistech/recipe/ritual/astral_prism.json', RITUAL, newline=False)

# ---------------------------------------------------------------- loot e tags
for b in ('essence_washer', 'runic_crystallizer', 'astral_prism'):
    dump(D + 'mistech/loot_table/blocks/%s.json' % b, {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'mistech:' + b}],
                                              'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': 'mistech:blocks/' + b})
for tag in ('mineable/pickaxe', 'needs_iron_tool'):
    p = D + 'minecraft/tags/block/%s.json' % tag
    j = json.load(open(p))
    for b in ('essence_washer', 'runic_crystallizer', 'astral_prism'):
        if 'mistech:' + b not in j['values']:
            j['values'].append('mistech:' + b)
    dump(p, j)

# ---------------------------------------------------------------- modelos de item
for m in METALS:
    for kind in ('clump', 'ore_crystal'):
        n = '%s_%s' % (m, kind)
        dump(A + 'items/%s.json' % n, {'model': {'type': 'minecraft:model', 'model': 'mistech:item/' + n}})
        dump(A + 'models/item/%s.json' % n, {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mistech:item/' + n}})

# ---------------------------------------------------------------- modelos de bloco
def faces(tex, uvs=None, skip=()):
    out = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        if d in skip:
            continue
        t = tex[d] if isinstance(tex, dict) else tex
        out[d] = {'texture': t}
        if uvs and d in uvs:
            out[d]['uv'] = uvs[d]
    return out


def box(f, t, tex, uvs=None, skip=(), rot=None, light=None):
    e = {'from': f, 'to': t, 'faces': faces(tex, uvs, skip)}
    if rot:
        e['rotation'] = rot
    if light:
        e['light_emission'] = light
    return e


def side_uv(x0, y0, x1, y1):
    return {d: [x0, 16 - y1, x1, 16 - y0] for d in ('north', 'south', 'east', 'west')}


def washer(on):
    els = [
        box([1, 0, 1], [15, 3, 15], {'north': '#copper', 'south': '#copper', 'east': '#copper', 'west': '#copper', 'up': '#copper', 'down': '#copper'},
            side_uv(1, 0, 15, 3)),
        box([1, 3, 1], [15, 12, 2], '#wall', side_uv(1, 3, 15, 12)),
        box([1, 3, 14], [15, 12, 15], '#wall', side_uv(1, 3, 15, 12)),
        box([1, 3, 2], [2, 12, 14], '#wall', side_uv(2, 3, 14, 12)),
        box([14, 3, 2], [15, 12, 14], '#wall', side_uv(2, 3, 14, 12)),
        box([0.5, 6, 0.5], [15.5, 7, 15.5], '#copper', side_uv(0, 6, 16, 7), skip=('up', 'down')),
        box([0.5, 11, 0.5], [15.5, 12, 15.5], '#copper', side_uv(0, 11, 16, 12), skip=('up', 'down')),
        box([2, 3, 2], [14, 10 if on else 8, 14], '#essence', {'up': [2, 2, 14, 14]}, skip=('north', 'south', 'east', 'west', 'down'),
            light=6 if on else None),
        box([7.5, 8, 7.5], [8.5, 16, 8.5], '#iron', {d: [7, 0, 8, 8] for d in ('north', 'south', 'east', 'west', 'up', 'down')}),
        box([4, 14, 7.5], [12, 15, 8.5], '#copper', {d: [4, 1, 12, 2] for d in ('north', 'south', 'east', 'west', 'up', 'down')},
            rot={'origin': [8, 14.5, 8], 'axis': 'y', 'angle': 45 if on else 0}),
        box([4, 9, 7.5], [12, 10, 8.5], '#copper', {d: [4, 1, 12, 2] for d in ('north', 'south', 'east', 'west', 'up', 'down')},
            rot={'origin': [8, 9.5, 8], 'axis': 'y', 'angle': -45 if on else 0}),
    ]
    t = {'particle': 'minecraft:block/cut_copper', 'copper': 'minecraft:block/cut_copper', 'wall': 'minecraft:block/polished_deepslate',
         'essence': 'mistech:block/impure_essence_still', 'iron': 'minecraft:block/iron_block'}
    return {'parent': 'minecraft:block/block', 'textures': t, 'elements': els}


def crystallizer(on):
    els = [
        box([0, 0, 0], [16, 4, 16], {'north': '#quartz', 'south': '#quartz', 'east': '#quartz', 'west': '#quartz', 'up': '#quartz_top', 'down': '#quartz_top'},
            side_uv(0, 0, 16, 4)),
        box([1, 4, 1], [15, 5, 15], '#gold', {**side_uv(1, 4, 15, 5), 'up': [1, 1, 15, 15]}),
        box([2, 5, 2], [14, 14, 14], '#glass', side_uv(2, 5, 14, 14), skip=('down',)),
        box([7, 5, 7], [9, 13, 9], '#crystal', {d: [7, 3, 9, 11] for d in ('north', 'south', 'east', 'west')}, skip=('down',),
            rot={'origin': [8, 5, 8], 'axis': 'y', 'angle': 45}, light=12 if on else None),
        box([4.5, 5, 7.5], [6.5, 10, 9.5], '#crystal', {d: [4, 6, 6, 11] for d in ('north', 'south', 'east', 'west')}, skip=('down',),
            rot={'origin': [5.5, 5, 8.5], 'axis': 'z', 'angle': 22.5}, light=12 if on else None),
        box([9.5, 5, 6], [11.5, 9, 8], '#crystal', {d: [10, 7, 12, 11] for d in ('north', 'south', 'east', 'west')}, skip=('down',),
            rot={'origin': [10.5, 5, 7], 'axis': 'z', 'angle': -22.5}, light=12 if on else None),
        box([5, 14, 5], [11, 15, 11], '#gold', {**side_uv(5, 14, 11, 15), 'up': [5, 5, 11, 11], 'down': [5, 5, 11, 11]}),
        box([7, 15, 7], [9, 16, 9], '#rune', {d: [7, 7, 9, 9] for d in ('north', 'south', 'east', 'west', 'up')}, light=10 if on else None),
    ]
    t = {'particle': 'minecraft:block/quartz_block_side', 'quartz': 'minecraft:block/quartz_block_side', 'quartz_top': 'minecraft:block/quartz_block_top',
         'gold': 'minecraft:block/gold_block', 'glass': 'minecraft:block/glass',
         'crystal': 'mistech:block/runic_crystallizer_crystal' + ('_on' if on else ''), 'rune': 'minecraft:block/glowstone' if on else 'minecraft:block/gold_block'}
    return {'parent': 'minecraft:block/block', 'textures': t, 'elements': els}


def prism(on):
    els = [
        box([0, 0, 0], [16, 3, 16], {'north': '#lapis', 'south': '#lapis', 'east': '#lapis', 'west': '#lapis', 'up': '#lapis', 'down': '#lapis'},
            side_uv(0, 0, 16, 3)),
        box([0, 3, 0], [16, 4, 16], '#gold', {**side_uv(0, 3, 16, 4), 'up': [0, 0, 16, 16]}),
    ]
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        els.append(box([x, 4, z], [x + 2, 14, z + 2], '#gold', {d: [x, 2, x + 2, 12] for d in ('north', 'south', 'east', 'west', 'up', 'down')}))
    els += [
        box([1, 14, 1], [15, 15, 3], '#gold', {d: [1, 1, 15, 2] for d in ('north', 'south', 'east', 'west', 'up', 'down')}),
        box([1, 14, 13], [15, 15, 15], '#gold', {d: [1, 1, 15, 2] for d in ('north', 'south', 'east', 'west', 'up', 'down')}),
        box([1, 14, 3], [3, 15, 13], '#gold', {d: [1, 1, 15, 2] for d in ('north', 'south', 'east', 'west', 'up', 'down')}),
        box([13, 14, 3], [15, 15, 13], '#gold', {d: [1, 1, 15, 2] for d in ('north', 'south', 'east', 'west', 'up', 'down')}),
        box([5, 5, 5], [11, 13, 11], '#crystal', {d: [5, 3, 11, 11] for d in ('north', 'south', 'east', 'west')} | {'up': [5, 5, 11, 11], 'down': [5, 5, 11, 11]},
            rot={'origin': [8, 9, 8], 'axis': 'y', 'angle': 45}, light=15 if on else 4),
        box([6.5, 4, 6.5], [9.5, 5, 9.5], '#gold', {d: [6, 6, 9, 9] for d in ('north', 'south', 'east', 'west', 'up', 'down')}),
    ]
    t = {'particle': 'minecraft:block/lapis_block', 'lapis': 'minecraft:block/lapis_block', 'gold': 'minecraft:block/gold_block',
         'crystal': 'mistech:block/astral_prism_crystal' + ('_on' if on else '')}
    return {'parent': 'minecraft:block/block', 'textures': t, 'elements': els}


for name, fn in (('essence_washer', washer), ('runic_crystallizer', crystallizer), ('astral_prism', prism)):
    dump(A + 'models/block/%s.json' % name, fn(False))
    dump(A + 'models/block/%s_on.json' % name, fn(True))
    bs = {'variants': {}}
    for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for lit in ('false', 'true'):
            v = {'model': 'mistech:block/' + name + ('_on' if lit == 'true' else '')}
            if r:
                v['y'] = r
            bs['variants']['facing=%s,lit=%s' % (f, lit)] = v
    dump(A + 'blockstates/%s.json' % name, bs)
    dump(A + 'items/%s.json' % name, {'model': {'type': 'minecraft:model', 'model': 'mistech:block/' + name}})

# ---------------------------------------------------------------- texturas dos cristais dos blocos
VAN = 'vanilla/assets/minecraft/textures/block/'


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def recolor(src, cols, bright=0.0):
    im = Image.open(VAN + src).convert('RGBA').crop((0, 0, 16, 16))
    ls = [lum(im.getpixel((x, y))) for y in range(16) for x in range(16)]
    a, b = min(ls), max(ls)
    out = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            t = min(1.0, (lum(im.getpixel((x, y))) - a) / ((b - a) or 1) + bright)
            f = t * (len(cols) - 1); i = min(int(f), len(cols) - 2); r = f - i
            out.putpixel((x, y), tuple(round(cols[i][k] * (1 - r) + cols[i + 1][k] * r) for k in range(3)) + (255,))
    return out


def animated(path, frames):
    strip = Image.new('RGBA', (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        strip.paste(f, (0, 16 * i))
    strip.save(path)
    open(path + '.mcmeta', 'w').write('{"animation":{"frametime":5,"interpolate":true}}\n')


QUARTZ = [(120, 104, 80), (200, 186, 150), (246, 236, 200), (255, 252, 236)]
recolor('amethyst_block.png', QUARTZ).save(A + 'textures/block/runic_crystallizer_crystal.png')
animated(A + 'textures/block/runic_crystallizer_crystal_on.png', [recolor('amethyst_block.png', QUARTZ, b) for b in (0.0, 0.15, 0.3, 0.15)])
STAR = [(14, 22, 70), (40, 70, 170), (130, 170, 255), (255, 255, 255)]
recolor('amethyst_block.png', STAR).save(A + 'textures/block/astral_prism_crystal.png')
animated(A + 'textures/block/astral_prism_crystal_on.png', [recolor('amethyst_block.png', STAR, b) for b in (0.0, 0.2, 0.4, 0.2)])
print('ok dados')

# ---------------------------------------------------------------- textos
LANG = A + 'lang/'
PT = {
    'block.mistech.essence_washer': 'Lavadora de Essência', 'container.mistech.essence_washer': 'Lavadora de Essência',
    'block.mistech.runic_crystallizer': 'Cristalizador Rúnico', 'container.mistech.runic_crystallizer': 'Cristalizador Rúnico',
    'block.mistech.astral_prism': 'Prisma Astral', 'container.mistech.astral_prism': 'Prisma Astral',
    'gui.mistech.runic_crystallizer.light': 'Luz: %s%%', 'gui.mistech.runic_crystallizer.no_light': 'Sem luz',
    'gui.mistech.astral_prism.speed': 'Céu: %s%%',
    'gui.mistech.astral_prism.status.working': 'Transmutando', 'gui.mistech.astral_prism.status.structure': 'Estrutura incompleta',
    'gui.mistech.astral_prism.status.day': 'Só à noite', 'gui.mistech.astral_prism.status.no_sky': 'Sem céu aberto',
    'gui.mistech.astral_prism.status.storm': 'Tempestade', 'gui.mistech.astral_prism.status.no_essence': 'Sem essência',
    'gui.mistech.astral_prism.status.no_fragment': 'Sem Fragmento Estelar', 'gui.mistech.astral_prism.status.no_energy': 'Sem energia',
    'gui.mistech.astral_prism.status.idle': 'Sem minério', 'gui.mistech.astral_prism.status.full': 'Saída cheia',
    'info.mistech.astral_prism.batch': 'Lote: %s minérios',
    'socket.mistech.light': 'Geradores celestes (Astral, Purificado, Observatório) +15%. No Cristalizador Rúnico, trabalha a pelo menos 75% sem sol.',
    'guide.mistech.entry.essence_washer': 'Lavadora de Essência',
    'guide.mistech.entry.runic_crystallizer': 'Cristalizador Rúnico',
    'guide.mistech.entry.astral_prism': 'Prisma Astral',
    'guide.mistech.essence_washer.recipe': 'A §5Lavadora de Essência§r é o segundo degrau do minério: 3 pós por minério.',
    'guide.mistech.essence_washer.1': '§lCadeia de minério§r\n§7Triturador:§r 1 minério → 2 pós.\n§5Lavadora:§r 1 minério → 3 Torrões Impregnados.\n§6Cristalizador Rúnico:§r 1 minério → 4 Cristais de Minério.\n§9Prisma Astral:§r 1 minério → 5 pós.\nTorrões e cristais viram 1 pó cada no triturador (ou direto lingote na Forja Trituradora). Vale para ferro, ouro, cobre, mitril, adamantina e oricalco, só com o §ominério em bloco§r.',
    'guide.mistech.essence_washer.2': '§lA Lavadora§r\nCada minério gasta §d100 mB de Essência Impura§r e 10 s a 40 FE/t. 10% de chance de sair uma Borra Impura. Encha o tanque com frascos e baldes (no espaço de baixo, na mão ou por canos).\n\nEngastes: 2. Terra, Eco e Sorte dão torrões extras.',
    'guide.mistech.runic_crystallizer.recipe': 'O §6Cristalizador Rúnico§r cresce cristais de minério com Essência Purificada e luz: 4 pós por minério.',
    'guide.mistech.runic_crystallizer.1': '§lLuz§r\nComo o Cristalizador da pureza: §esol 100%§r (Solstício 200%), lua cheia 50%, escuro, chuva ou telhado em cima = parado. Com a §eRuna da Luz§r num engaste, trabalha a pelo menos 75% sem sol.\n\nCada minério gasta §e250 mB de Essência Purificada§r, 20 s com sol e 60 FE/t. 15% de Resíduo Mágico e 5% do metal irmão: ferro → mitril, ouro → oricalco, cobre → adamantina.',
    'guide.mistech.astral_prism.ritual': 'O Núcleo do Prisma nasce de um ritual Tier II numa noite limpa.',
    'guide.mistech.astral_prism.legend': 'Base 5×5 de lápis-lazúli sob o Núcleo (N), Pilares Astrais (P) nos cantos e vidro sobre o Núcleo, a céu aberto. A Pedra Guia mostra o holograma.',
    'guide.mistech.astral_prism.1': '§lO Prisma Astral§r\nSó §9à noite e a céu aberto§r. Transmuta lotes de até 8 minérios em §e5 pós cada§r. Por lote: até 500 mB de Essência Purificada e 30 s a 200 FE/t. Um §bFragmento Estelar§r a cada 16 minérios (5% de chance de não gastar).\n\nA lua manda: §elua nova 150%§r, cheia 75%. Chuva corta pela metade; tempestade para. 5% do metal irmão, como no Cristalizador.',
}
EN = {
    'block.mistech.essence_washer': 'Essence Washer', 'container.mistech.essence_washer': 'Essence Washer',
    'block.mistech.runic_crystallizer': 'Runic Crystallizer', 'container.mistech.runic_crystallizer': 'Runic Crystallizer',
    'block.mistech.astral_prism': 'Astral Prism', 'container.mistech.astral_prism': 'Astral Prism',
    'gui.mistech.runic_crystallizer.light': 'Light: %s%%', 'gui.mistech.runic_crystallizer.no_light': 'No light',
    'gui.mistech.astral_prism.speed': 'Sky: %s%%',
    'gui.mistech.astral_prism.status.working': 'Transmuting', 'gui.mistech.astral_prism.status.structure': 'Incomplete structure',
    'gui.mistech.astral_prism.status.day': 'Night only', 'gui.mistech.astral_prism.status.no_sky': 'No open sky',
    'gui.mistech.astral_prism.status.storm': 'Thunderstorm', 'gui.mistech.astral_prism.status.no_essence': 'No essence',
    'gui.mistech.astral_prism.status.no_fragment': 'No Star Fragment', 'gui.mistech.astral_prism.status.no_energy': 'No energy',
    'gui.mistech.astral_prism.status.idle': 'No ore', 'gui.mistech.astral_prism.status.full': 'Output full',
    'info.mistech.astral_prism.batch': 'Batch: %s ores',
    'socket.mistech.light': 'Celestial generators (Astral, Purified, Observatory) +15%. In the Runic Crystallizer, works at 75% or more without sun.',
    'guide.mistech.entry.essence_washer': 'Essence Washer',
    'guide.mistech.entry.runic_crystallizer': 'Runic Crystallizer',
    'guide.mistech.entry.astral_prism': 'Astral Prism',
    'guide.mistech.essence_washer.recipe': 'The §5Essence Washer§r is the second step of ore processing: 3 dusts per ore.',
    'guide.mistech.essence_washer.1': '§lOre processing§r\n§7Crusher:§r 1 ore → 2 dusts.\n§5Washer:§r 1 ore → 3 Impregnated Clumps.\n§6Runic Crystallizer:§r 1 ore → 4 Ore Crystals.\n§9Astral Prism:§r 1 ore → 5 dusts.\nClumps and crystals become 1 dust each in a crusher (or an ingot straight away in the Crusher Forge). Works for iron, gold, copper, mithril, adamantine and orichalcum, with §oore blocks§r only.',
    'guide.mistech.essence_washer.2': '§lThe Washer§r\nEach ore takes §d100 mB of Impure Essence§r and 10 s at 40 FE/t. 10% chance of a Tainted Residue. Fill the tank with bottles and buckets (lower slot, by hand or with pipes).\n\nSockets: 2. Earth, Echo and Luck give extra clumps.',
    'guide.mistech.runic_crystallizer.recipe': 'The §6Runic Crystallizer§r grows ore crystals with Purified Essence and light: 4 dusts per ore.',
    'guide.mistech.runic_crystallizer.1': '§lLight§r\nLike the purity Crystallizer: §esun 100%§r (Solstice 200%), full moon 50%, darkness, rain or a roof = stopped. With a §eLight Rune§r socketed it works at 75% or more without sun.\n\nEach ore takes §e250 mB of Purified Essence§r, 20 s in sunlight and 60 FE/t. 15% Magic Residue and 5% of the sibling metal: iron → mithril, gold → orichalcum, copper → adamantine.',
    'guide.mistech.astral_prism.ritual': 'The Prism Core is born from a Tier II ritual on a clear night.',
    'guide.mistech.astral_prism.legend': '5×5 lapis base under the Core (N), Astral Pillars (P) on its corners and glass above the Core, under open sky. The Guide Stone shows the hologram.',
    'guide.mistech.astral_prism.1': '§lThe Astral Prism§r\n§9Night and open sky§r only. Transmutes batches of up to 8 ores into §e5 dusts each§r. Per batch: up to 500 mB of Purified Essence and 30 s at 200 FE/t. One §bStar Fragment§r every 16 ores (5% chance to keep it).\n\nThe moon rules: §enew moon 150%§r, full 75%. Rain halves it; thunderstorms stop it. 5% sibling metal, like the Crystallizer.',
}
for m in METALS:
    PT['item.mistech.%s_clump' % m] = 'Torrão Impregnado de %s' % NAMES_PT[m]
    PT['item.mistech.%s_ore_crystal' % m] = 'Cristal de %s' % NAMES_PT[m]
    EN['item.mistech.%s_clump' % m] = 'Impregnated %s Clump' % NAMES_EN[m]
    EN['item.mistech.%s_ore_crystal' % m] = '%s Ore Crystal' % NAMES_EN[m]
for name, d in (('pt_br', PT), ('en_us', EN)):
    p = LANG + name + '.json'
    data = json.load(open(p, encoding='utf-8'))
    data.update(d)
    dump(p, data)

# ---------------------------------------------------------------- Grimório (Máquinas II)
BOOK = A + 'guide/book.json'
E = [
    {'id': 'essence_washer', 'icon': 'mistech:essence_washer', 'name': 'guide.mistech.entry.essence_washer',
     'deps': ['arcane_crusher', 'impure_essence'], 'task': {'type': 'item', 'items': ['mistech:essence_washer']}, 'x': 6.4, 'y': 5.5,
     'pages': [{'type': 'crafting', 'grid': ['minecraft:iron_ingot', '#c:glass_blocks', 'minecraft:iron_ingot', 'mistech:water_rune', 'mistech:runic_tank',
                                              'mistech:water_rune', 'minecraft:iron_ingot', 'mistech:runic_crusher', 'minecraft:iron_ingot'],
                'result': 'mistech:essence_washer', 'text': 'guide.mistech.essence_washer.recipe'},
               {'type': 'text', 'text': 'guide.mistech.essence_washer.1'}, {'type': 'text', 'text': 'guide.mistech.essence_washer.2'}]},
    {'id': 'runic_crystallizer', 'icon': 'mistech:runic_crystallizer', 'name': 'guide.mistech.entry.runic_crystallizer',
     'deps': ['essence_washer', 'crystallizer'], 'task': {'type': 'item', 'items': ['mistech:runic_crystallizer']}, 'x': 8.0, 'y': 5.5,
     'pages': [{'type': 'crafting', 'grid': ['#c:glass_blocks', 'mistech:light_rune', '#c:glass_blocks', 'mistech:pure_magic_shard', 'mistech:crystallizer',
                                              'mistech:pure_magic_shard', 'mistech:orichalcum_ingot', 'mistech:mithril_ingot', 'mistech:orichalcum_ingot'],
                'result': 'mistech:runic_crystallizer', 'text': 'guide.mistech.runic_crystallizer.recipe'},
               {'type': 'text', 'text': 'guide.mistech.runic_crystallizer.1'}]},
    {'id': 'astral_prism', 'icon': 'mistech:astral_prism', 'name': 'guide.mistech.entry.astral_prism',
     'deps': ['runic_crystallizer', 'astral_pillar', 'altar_tier2'], 'task': {'type': 'item', 'items': ['mistech:astral_prism']}, 'x': 9.6, 'y': 5.5,
     'pages': [{'type': 'ritual', 'catalyst': RITUAL['catalyst'], 'ingredients': RITUAL['ingredients'], 'essence': RITUAL['essence'],
                'result': 'mistech:astral_prism', 'title': 'guide.mistech.entry.astral_prism', 'condition': 'clear_night', 'min_tier': 2,
                'text': 'guide.mistech.astral_prism.ritual'},
               {'type': 'multiblock', 'pattern': ['P000P', '00000', '00N00', '00000', 'P000P'],
                'key': {'P': 'mistech:astral_pillar', 'N': 'mistech:astral_prism'}, 'text': 'guide.mistech.astral_prism.legend'},
               {'type': 'text', 'text': 'guide.mistech.astral_prism.1'}]},
]
book = json.load(open(BOOK, encoding='utf-8'))
cat = next(c for c in book['categories'] if c['id'] == 'machines2')
ids = {e['id'] for e in E}
cat['entries'] = [e for e in cat['entries'] if e['id'] not in ids] + E
dump(BOOK, book, newline=False)
print('ok textos e Grimório')
