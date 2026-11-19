#!/usr/bin/env python3
"""Dados da Fase 5: Poço de Mana e Sangria Arcana (modelos, blockstates, loot, tags, receitas, textos e Grimório).
Idempotente. Depois rode tools/guide/views.py.
Uso (na pasta do projeto): python3 tools/well/gen_data.py"""
import json, os

R = 'src/main/resources/'
A = R + 'assets/mistech/'
D = R + 'data/'


def dump(path, obj, newline=True):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        if newline:
            f.write('\n')


# ---------------------------------------------------------------- receitas, loot e tags
WELL = ['mistech:pure_magic_shard', 'mistech:arcane_rune', 'mistech:pure_magic_shard',
        'mistech:orichalcum_ingot', 'minecraft:chiseled_quartz_block', 'mistech:orichalcum_ingot',
        'mistech:pure_magic_shard', 'mistech:arcane_charger', 'mistech:pure_magic_shard']
SIPHON = ['minecraft:copper_ingot', 'mistech:energy_rune', 'minecraft:copper_ingot',
          'mistech:arcane_rune', 'mistech:glass_jar', 'mistech:arcane_rune',
          'minecraft:polished_blackstone', 'minecraft:polished_blackstone', 'minecraft:polished_blackstone']


def shaped(grid, pattern):
    key, out = {}, []
    for row in pattern:
        out.append(row)
    for row_i, row in enumerate(pattern):
        for col_i, ch in enumerate(row):
            key[ch] = grid[row_i * 3 + col_i]
    return out, key


for name, grid, pattern in (('mana_well', WELL, ['PEP', 'OFO', 'PCP']), ('mana_siphon', SIPHON, ['CEC', 'RTR', 'BBB'])):
    pat, key = shaped(grid, pattern)
    dump(D + 'mistech/recipe/%s.json' % name, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pat, 'key': key,
                                               'result': {'id': 'mistech:' + name, 'count': 1}})
    dump(D + 'mistech/loot_table/blocks/%s.json' % name, {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'mistech:' + name}],
                                              'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': 'mistech:blocks/' + name})
p = D + 'minecraft/tags/block/mineable/pickaxe.json'
j = json.load(open(p))
for b in ('mana_well', 'mana_siphon'):
    if 'mistech:' + b not in j['values']:
        j['values'].append('mistech:' + b)
dump(p, j)


# ---------------------------------------------------------------- modelos
def faces(tex, uvs=None, skip=()):
    out = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        if d in skip:
            continue
        out[d] = {'texture': tex[d] if isinstance(tex, dict) else tex}
        if uvs and d in uvs:
            out[d]['uv'] = uvs[d]
    return out


def box(f, t, tex, uvs=None, skip=(), light=None):
    e = {'from': f, 'to': t, 'faces': faces(tex, uvs, skip)}
    if light:
        e['light_emission'] = light
    return e


def auto(f, t):
    """UVs que seguem a posição (as faces não esticam a textura)."""
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
            'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
            'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}


def cube(f, t, tex, skip=(), light=None):
    return box(f, t, tex, auto(f, t), skip, light)


def bowl(x0, y0, x1, y1, wall, tex, skip_bottom=False):
    """Bacia: fundo + 4 paredes (sem faces encobertas)."""
    z0, z1 = x0, x1
    els = []
    if not skip_bottom:
        els.append(cube([x0, y0, z0], [x1, y0 + 1, z1], tex))
    els += [
        cube([x0, y0, z0], [x1, y1, z0 + wall], tex),
        cube([x0, y0, z1 - wall], [x1, y1, z1], tex),
        cube([x0, y0, z0 + wall], [x0 + wall, y1, z1 - wall], tex),
        cube([x1 - wall, y0, z0 + wall], [x1, y1, z1 - wall], tex),
    ]
    return els


def well(on):
    els = [
        cube([2, 0, 2], [14, 2, 14], '#base'),
        cube([3, 2, 3], [13, 3, 13], '#base'),
        cube([5, 3, 5], [11, 9, 11], '#pillar'),
    ] + bowl(1, 9, 15, 14, 1.5, '#base') + [
        cube([0.5, 13, 0.5], [15.5, 14, 15.5], '#gold', skip=('up', 'down')),
        box([2.5, 10, 2.5], [13.5, 12.5 if on else 11.5, 13.5], '#mana', {'up': [2.5, 2.5, 13.5, 13.5]},
            skip=('north', 'south', 'east', 'west', 'down'), light=12 if on else 5),
    ]
    t = {'particle': 'minecraft:block/quartz_block_side', 'base': 'minecraft:block/quartz_block_side',
         'pillar': 'minecraft:block/chiseled_quartz_block', 'gold': 'minecraft:block/gold_block',
         'mana': 'mistech:block/mana_well_mana' + ('_on' if on else '')}
    return {'parent': 'minecraft:block/block', 'textures': t, 'elements': els}


def siphon(on):
    rune = '#rune'
    els = [
        cube([2, 0, 2], [14, 3, 14], '#stone'),
        cube([3, 3, 3], [13, 4, 13], '#stone'),
        cube([4, 4, 4], [12, 10, 12], {'north': rune, 'south': rune, 'east': rune, 'west': rune, 'up': '#stone', 'down': '#stone'}, light=8 if on else None),
        cube([5, 10, 5], [11, 11, 11], '#copper'),
    ] + bowl(3, 11, 13, 15, 1, '#copper') + [
        box([4, 12, 4], [12, 13.5 if on else 12.5, 12], '#mana', {'up': [4, 4, 12, 12]}, skip=('north', 'south', 'east', 'west', 'down'),
            light=12 if on else None),
    ]
    t = {'particle': 'minecraft:block/polished_blackstone', 'stone': 'minecraft:block/polished_blackstone', 'copper': 'minecraft:block/cut_copper',
         'rune': 'mistech:block/mana_siphon_rune' + ('_on' if on else ''), 'mana': 'mistech:block/mana_well_mana' + ('_on' if on else '')}
    return {'parent': 'minecraft:block/block', 'textures': t, 'elements': els}


for name, fn in (('mana_well', well), ('mana_siphon', siphon)):
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
print('ok dados')

# ---------------------------------------------------------------- textos
LANG = A + 'lang/'
PT = {
    'block.mistech.mana_well': 'Poço de Mana', 'container.mistech.mana_well': 'Poço de Mana',
    'block.mistech.mana_siphon': 'Sangria Arcana', 'container.mistech.mana_siphon': 'Sangria Arcana',
    'gui.mistech.mana_well.status.idle': 'Ninguém precisa', 'gui.mistech.mana_well.status.working': 'Dando mana',
    'gui.mistech.mana_well.status.no_energy': 'Sem energia', 'gui.mistech.mana_well.status.no_essence': 'Sem essência',
    'gui.mistech.mana_well.giving': 'Mana: %s/s',
    'message.mistech.mana_well.filled': 'O poço te enche de mana (+%s)',
    'message.mistech.mana_well.full': 'Sua mana já está cheia',
    'message.mistech.mana_well.cooldown': 'O poço ainda está se recompondo (%s s)',
    'message.mistech.mana_well.no_essence': 'O poço está sem Essência Purificada',
    'message.mistech.mana_well.no_energy': 'O poço está sem energia',
    'gui.mistech.mana_siphon.status.idle': 'Parada', 'gui.mistech.mana_siphon.status.draining': 'Sangrando mana',
    'gui.mistech.mana_siphon.status.low_mana': 'Mana no limite', 'gui.mistech.mana_siphon.status.full': 'Energia cheia',
    'gui.mistech.mana_siphon.status.tainted': 'Sangue ruim (2×)',
    'gui.mistech.mana_siphon.hint': 'Segure o clique no bloco',
    'message.mistech.mana_siphon.low': 'A Sangria para: você precisa de mana para se defender',
    'guide.mistech.entry.mana_well': 'Poço de Mana',
    'guide.mistech.entry.mana_siphon': 'Sangria Arcana',
    'guide.mistech.mana_well.recipe': 'O §9Poço de Mana§r transforma energia e Essência Purificada em mana para você.',
    'guide.mistech.mana_well.1': '§lAura§r\nQuem estiver a até 6 blocos ganha até §b+2 de mana por segundo§r, por cima da regeneração, e a reserva do cajado na mão enche junto.\n\nCada ponto custa §e20 FE§r e §e1 mB de Essência Purificada§r. Encha o tanque com frascos e baldes (na mão, no espaço de baixo ou por canos).',
    'guide.mistech.mana_well.2': '§lEncher de uma vez§r\nClique no poço com a mão vazia: a sua mana enche até o máximo, pagando o custo cheio. Depois espera 30 s. §7Agachado + clique abre a tela.§r\n\n§lAfinidade§r\nUma runa no espaço de runa dá §b+25% de mana§r pelo mesmo custo para quem tem nível 5 ou mais nesse elemento.',
    'guide.mistech.mana_siphon.recipe': 'A §5Sangria Arcana§r faz o caminho de volta: a sua mana vira energia.',
    'guide.mistech.mana_siphon.1': 'Segure o clique na taça: ela drena §b10 de mana por segundo§r e gera §e15 FE§r por ponto (150 FE/s), mandando até 500 FE/t para os vizinhos. Para sozinha em §b20%§r da sua mana máxima.\n\nO ciclo mana → FE → mana perde 25% de propósito: não vira energia infinita.',
    'guide.mistech.mana_siphon.2': '§5Sangue ruim§r\nCom a sua Mácula §5Corrompida§r ou pior, a Sangria rende §e2×§r, mas cada 100 de mana drenada dá §5+1 de Mácula§r.\n\n§7Agachado + clique abre a tela.§r',
}
EN = {
    'block.mistech.mana_well': 'Mana Well', 'container.mistech.mana_well': 'Mana Well',
    'block.mistech.mana_siphon': 'Arcane Siphon', 'container.mistech.mana_siphon': 'Arcane Siphon',
    'gui.mistech.mana_well.status.idle': 'Nobody needs it', 'gui.mistech.mana_well.status.working': 'Giving mana',
    'gui.mistech.mana_well.status.no_energy': 'No energy', 'gui.mistech.mana_well.status.no_essence': 'No essence',
    'gui.mistech.mana_well.giving': 'Mana: %s/s',
    'message.mistech.mana_well.filled': 'The well fills you with mana (+%s)',
    'message.mistech.mana_well.full': 'Your mana is already full',
    'message.mistech.mana_well.cooldown': 'The well is still settling (%s s)',
    'message.mistech.mana_well.no_essence': 'The well has no Purified Essence',
    'message.mistech.mana_well.no_energy': 'The well has no energy',
    'gui.mistech.mana_siphon.status.idle': 'Stopped', 'gui.mistech.mana_siphon.status.draining': 'Siphoning mana',
    'gui.mistech.mana_siphon.status.low_mana': 'Mana at the limit', 'gui.mistech.mana_siphon.status.full': 'Energy full',
    'gui.mistech.mana_siphon.status.tainted': 'Bad blood (2×)',
    'gui.mistech.mana_siphon.hint': 'Hold use on the block',
    'message.mistech.mana_siphon.low': 'The Siphon stops: you need mana to defend yourself',
    'guide.mistech.entry.mana_well': 'Mana Well',
    'guide.mistech.entry.mana_siphon': 'Arcane Siphon',
    'guide.mistech.mana_well.recipe': 'The §9Mana Well§r turns energy and Purified Essence into mana for you.',
    'guide.mistech.mana_well.1': '§lAura§r\nAnyone within 6 blocks gets up to §b+2 mana per second§r on top of regeneration, and the reserve of the staff in hand fills too.\n\nEach point costs §e20 FE§r and §e1 mB of Purified Essence§r. Fill the tank with bottles and buckets (by hand, in the lower slot or with pipes).',
    'guide.mistech.mana_well.2': '§lFill at once§r\nClick the well with an empty hand: your mana fills to the maximum, paying the full cost. Then it waits 30 s. §7Sneak + click opens the screen.§r\n\n§lAffinity§r\nA rune in the rune slot gives §b+25% mana§r for the same cost to anyone with level 5 or more in that element.',
    'guide.mistech.mana_siphon.recipe': 'The §5Arcane Siphon§r goes the other way: your mana becomes energy.',
    'guide.mistech.mana_siphon.1': 'Hold use on the chalice: it drains §b10 mana per second§r and makes §e15 FE§r per point (150 FE/s), sending up to 500 FE/t to its neighbours. It stops on its own at §b20%§r of your maximum mana.\n\nThe mana → FE → mana loop loses 25% on purpose: no infinite energy.',
    'guide.mistech.mana_siphon.2': '§5Bad blood§r\nWith your Taint §5Corrupted§r or worse, the Siphon yields §e2×§r, but every 100 mana drained gives §5+1 Taint§r.\n\n§7Sneak + click opens the screen.§r',
}
for name, d in (('pt_br', PT), ('en_us', EN)):
    p = LANG + name + '.json'
    data = json.load(open(p, encoding='utf-8'))
    data.update(d)
    dump(p, data)

# ---------------------------------------------------------------- Grimório (Magias)
BOOK = A + 'guide/book.json'
E = [
    {'id': 'mana_well', 'icon': 'mistech:mana_well', 'name': 'guide.mistech.entry.mana_well',
     'deps': ['arcane_charger', 'pure_generator'], 'task': {'type': 'item', 'items': ['mistech:mana_well']}, 'x': 8.0, 'y': 1.5,
     'pages': [{'type': 'crafting', 'grid': WELL, 'result': 'mistech:mana_well', 'text': 'guide.mistech.mana_well.recipe'},
               {'type': 'text', 'text': 'guide.mistech.mana_well.1'}, {'type': 'text', 'text': 'guide.mistech.mana_well.2'}]},
    {'id': 'mana_siphon', 'icon': 'mistech:mana_siphon', 'name': 'guide.mistech.entry.mana_siphon',
     'deps': ['mana_well'], 'task': {'type': 'item', 'items': ['mistech:mana_siphon']}, 'x': 9.6, 'y': 1.0,
     'pages': [{'type': 'crafting', 'grid': SIPHON, 'result': 'mistech:mana_siphon', 'text': 'guide.mistech.mana_siphon.recipe'},
               {'type': 'text', 'text': 'guide.mistech.mana_siphon.1'}, {'type': 'text', 'text': 'guide.mistech.mana_siphon.2'}]},
]
book = json.load(open(BOOK, encoding='utf-8'))
cat = next(c for c in book['categories'] if c['id'] == 'spells')
ids = {e['id'] for e in E}
cat['entries'] = [e for e in cat['entries'] if e['id'] not in ids] + E
dump(BOOK, book, newline=False)
print('ok textos e Grimório')
