#!/usr/bin/env python3
"""Dados da Fase 9 (endgame): modelos, blockstates, itens, loot, tags, receitas, textos e Grimório.
Idempotente. Depois rode tools/guide/views.py.
Uso (na pasta do projeto): python3 tools/endgame/gen_data.py"""
import json, os

R = 'src/main/resources/'
A = R + 'assets/mistech/'
D = R + 'data/'
LANG = A + 'lang/'
MACHINES = ['star_forge', 'dream_reactor', 'celestial_battery', 'celestial_conjurer']
GEAR = ['helmet', 'chestplate', 'leggings', 'boots', 'sword', 'spear', 'pickaxe', 'axe', 'shovel', 'hoe']


def dump(path, obj, newline=True):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        if newline:
            f.write('\n')


def load(path):
    return json.load(open(path, encoding='utf-8'))


# ---------------------------------------------------------------- modelos
def auto(f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
            'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
            'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}


def cube(f, t, tex, skip=(), light=None):
    uv = auto(f, t)
    faces = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        if d in skip:
            continue
        faces[d] = {'texture': tex[d] if isinstance(tex, dict) and d in tex else (tex.get('*') if isinstance(tex, dict) else tex),
                    'uv': uv[d]}
    e = {'from': f, 'to': t, 'faces': faces}
    if light:
        e['light_emission'] = light
    return e


def model(textures, els):
    return {'parent': 'minecraft:block/block', 'textures': textures, 'elements': els}


def forge(on):
    return model({'particle': 'mistech:block/star_forge_side', 'side': 'mistech:block/star_forge_side',
                  'front': 'mistech:block/star_forge_front_on' if on else 'mistech:block/star_forge_front',
                  'top': 'mistech:block/star_forge_top', 'gold': 'minecraft:block/gold_block', 'core': 'mistech:block/celestial_block'}, [
        cube([0, 0, 0], [16, 4, 16], {'*': '#side', 'up': '#top', 'down': '#side'}),
        cube([2, 4, 2], [14, 11, 14], {'*': '#side', 'north': '#front'}, light=12 if on else None),
        cube([1, 11, 1], [15, 13, 15], {'*': '#gold', 'up': '#top'}),
        cube([6, 13, 6], [10, 14, 10], '#gold'),
        cube([6.5, 14, 6.5], [9.5, 16, 9.5], '#core', light=15 if on else 6),
    ])


def reactor(on):
    eye = 'mistech:block/dream_reactor_eye_on' if on else 'mistech:block/dream_reactor_eye'
    return model({'particle': 'mistech:block/dream_reactor_shell', 'shell': 'mistech:block/dream_reactor_shell', 'eye': eye,
                  'star': 'mistech:block/star_stone' if os.path.exists(A + 'textures/block/star_stone.png') else 'minecraft:block/end_stone'}, [
        cube([0, 0, 0], [16, 16, 16], {'*': '#shell', 'north': '#eye'}, light=10 if on else None),
        cube([-0.01, 15, -0.01], [16.01, 16.01, 16.01], '#star', skip=('down',)),
    ])


def retexture(src, dst, swap):
    m = load(A + 'models/block/%s.json' % src)
    m['textures'] = {k: swap.get(k, v) for k, v in m['textures'].items()}
    dump(A + 'models/block/%s.json' % dst, m)


for name, fn in (('star_forge', forge), ('dream_reactor', reactor)):
    dump(A + 'models/block/%s.json' % name, fn(False))
    dump(A + 'models/block/%s_on.json' % name, fn(True))
BATTERY = {'deep': 'minecraft:block/lapis_block', 'body': 'mistech:block/celestial_block', 'cut': 'minecraft:block/gold_block',
           'rune': 'minecraft:block/light_blue_stained_glass', 'void': 'mistech:block/star_forge_top', 'particle': 'mistech:block/celestial_block'}
retexture('advanced_runic_battery', 'celestial_battery', dict(BATTERY, glow='minecraft:block/light_blue_stained_glass'))
retexture('advanced_runic_battery_on', 'celestial_battery_on', dict(BATTERY, glow='minecraft:block/sea_lantern'))
CONJ = {'particle': 'mistech:block/celestial_block', 'bricks': 'mistech:block/star_forge_side', 'stone': 'minecraft:block/lapis_block',
        'mithril': 'mistech:block/celestial_block', 'crystal': 'minecraft:block/light_blue_stained_glass'}
retexture('runic_conjurer', 'celestial_conjurer', CONJ)
retexture('runic_conjurer_on', 'celestial_conjurer_on', CONJ)
dump(A + 'models/block/celestial_block.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'mistech:block/celestial_block'}})
dump(A + 'blockstates/celestial_block.json', {'variants': {'': {'model': 'mistech:block/celestial_block'}}})
dump(A + 'items/celestial_block.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:block/celestial_block'}})

for name in MACHINES:
    bs = {'variants': {}}
    for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for lit in ('false', 'true'):
            v = {'model': 'mistech:block/%s%s' % (name, '_on' if lit == 'true' else '')}
            if r:
                v['y'] = r
            bs['variants']['facing=%s,lit=%s' % (f, lit)] = v
    dump(A + 'blockstates/%s.json' % name, bs)
    dump(A + 'items/%s.json' % name, {'model': {'type': 'minecraft:model', 'model': 'mistech:block/' + name}})

for item in ['celestial_ingot', 'celestial_core'] + ['celestial_' + g for g in GEAR]:
    dump(A + 'models/item/%s.json' % item, {'parent': 'minecraft:item/handheld' if any(item.endswith(t) for t in
                                                                                   ('sword', 'pickaxe', 'axe', 'shovel', 'hoe')) else
                                         'minecraft:item/generated', 'textures': {'layer0': 'mistech:item/' + item}})
    if item != 'celestial_spear':
        dump(A + 'items/%s.json' % item, {'model': {'type': 'minecraft:model', 'model': 'mistech:item/' + item}})
# lança: mesmo formato da do Oricalco Puro
spear = load(A + 'items/pure_orichalcum_spear.json')
dump(A + 'items/celestial_spear.json', json.loads(json.dumps(spear).replace('pure_orichalcum_spear', 'celestial_spear')))
src = load(A + 'models/item/pure_orichalcum_spear.json')
dump(A + 'models/item/celestial_spear.json', json.loads(json.dumps(src).replace('pure_orichalcum_spear', 'celestial_spear')))
src = load(A + 'models/item/pure_orichalcum_spear_in_hand.json')
dump(A + 'models/item/celestial_spear_in_hand.json', json.loads(json.dumps(src).replace('pure_orichalcum_spear', 'celestial_spear')))
# armadura vestida
eq = load(A + 'equipment/pure_orichalcum.json')
dump(A + 'equipment/celestial.json', json.loads(json.dumps(eq).replace('mistech:pure_orichalcum', 'mistech:celestial')))

# ---------------------------------------------------------------- loot e tags
for name in MACHINES + ['celestial_block']:
    entry = {'type': 'minecraft:item', 'name': 'mistech:' + name}
    if name == 'celestial_battery':
        entry['functions'] = [{'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': ['mistech:battery_energy']}]
    dump(D + 'mistech/loot_table/blocks/%s.json' % name, {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [entry],
                                              'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': 'mistech:blocks/' + name})


def tag(path, values):
    p = D + path
    j = load(p) if os.path.exists(p) else {'replace': False, 'values': []}
    for v in values:
        if v not in j['values']:
            j['values'].append(v)
    dump(p, j)


tag('minecraft/tags/block/mineable/pickaxe.json', ['mistech:' + n for n in MACHINES + ['celestial_block']])
tag('minecraft/tags/block/needs_diamond_tool.json', ['mistech:celestial_block'])
tag('minecraft/tags/block/beacon_base_blocks.json', ['mistech:celestial_block'])
tag('minecraft/tags/item/beacon_payment_items.json', ['mistech:celestial_ingot'])
# o equipamento entra nas mesmas tags que o do Oricalco Puro
for root in ('minecraft/tags/item', 'mistech/tags/item'):
    for dirpath, _, files in os.walk(D + root):
        for fn in files:
            p = os.path.join(dirpath, fn)
            j = load(p)
            add = [v.replace('pure_orichalcum', 'celestial') for v in j.get('values', []) if isinstance(v, str)
                   and v.startswith('mistech:pure_orichalcum_') and not v.endswith('_bow')]
            if add:
                tag(os.path.relpath(p, D), add)

# ---------------------------------------------------------------- receitas
def ritual(name, catalyst, ingredients, essence, result, condition='night', tier=3, extra=None):
    r = {'type': 'mistech:ritual', 'catalyst': catalyst, 'ingredients': ingredients, 'essence': essence, 'duration': 600,
         'condition': condition, 'min_tier': tier, 'result': {'id': result}, 'effect': 'astral'}
    if extra:
        r.update(extra)
    dump(D + 'mistech/recipe/ritual/%s.json' % name, r)
    return r


RITUALS = {
    'star_forge': ritual('star_forge', 'mistech:crusher_forge', ['mistech:astral_prism', 'mistech:star_fragment', 'mistech:star_fragment',
                                                                 'mistech:star_fragment', 'mistech:star_fragment', 'mistech:space_rune',
                                                                 'mistech:time_rune'], 8, 'mistech:star_forge', 'clear_night'),
    'dream_reactor': ritual('dream_reactor', 'mistech:impure_reactor', ['mistech:memory_shard', 'mistech:memory_shard', 'mistech:memory_shard',
                                                                        'mistech:memory_shard', 'mistech:purified_dreamcatcher', 'mistech:time_rune'],
                            8, 'mistech:dream_reactor'),
    'celestial_core': ritual('celestial_core', 'mistech:celestial_block', ['mistech:time_rune', 'mistech:space_rune', 'mistech:echo_rune',
                                                                           'mistech:star_fragment'], 8, 'mistech:celestial_core'),
    'celestial_conjurer': ritual('celestial_conjurer', 'mistech:runic_conjurer', ['mistech:celestial_ingot', 'mistech:celestial_ingot',
                                                                                  'mistech:blank_spell_crystal', 'mistech:arcane_rune',
                                                                                  'mistech:star_fragment', 'mistech:time_rune'],
                                 8, 'mistech:celestial_conjurer', extra={'copy_components': True}),
}
for g in GEAR:
    RITUALS['celestial_' + g] = ritual('celestial_' + g, 'mistech:pure_orichalcum_' + g,
                                       ['mistech:celestial_ingot', 'mistech:celestial_ingot', 'mistech:star_fragment', 'mistech:space_rune'],
                                       6, 'mistech:celestial_' + g, extra={'copy_components': True})
dump(D + 'mistech/recipe/celestial_block.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['III', 'III', 'III'],
                                                'key': {'I': 'mistech:celestial_ingot'}, 'result': {'id': 'mistech:celestial_block', 'count': 1}})
dump(D + 'mistech/recipe/celestial_ingot_from_block.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                                           'ingredients': ['mistech:celestial_block'],
                                                           'result': {'id': 'mistech:celestial_ingot', 'count': 9}})
BATTERY_RECIPE = (['ISI', 'VAV', 'ISI'], {'I': 'mistech:celestial_ingot', 'S': 'mistech:star_fragment', 'V': 'mistech:void_rune',
                                           'A': 'mistech:advanced_runic_battery'})
dump(D + 'mistech/recipe/celestial_battery.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': BATTERY_RECIPE[0],
                                                  'key': BATTERY_RECIPE[1], 'result': {'id': 'mistech:celestial_battery', 'count': 1}})

# ---------------------------------------------------------------- textos
PT = {
    'container.mistech.star_forge': 'Forja Estelar', 'container.mistech.dream_reactor': 'Reator Onírico',
    'container.mistech.celestial_battery': 'Bateria Celeste', 'container.mistech.celestial_conjurer': 'Conjurador Celeste',
    'item.mistech.celestial_ingot': 'Lingote Celeste', 'block.mistech.celestial_block': 'Bloco Celeste',
    'item.mistech.celestial_core': 'Núcleo Celeste',
    'item.mistech.celestial_core.tooltip': 'Botão direito numa máquina com engastes: as runas param de se gastar.',
    'block.mistech.star_forge': 'Forja Estelar', 'block.mistech.dream_reactor': 'Reator Onírico',
    'block.mistech.celestial_battery': 'Bateria Celeste', 'block.mistech.celestial_conjurer': 'Conjurador Celeste',
    'item.mistech.celestial_helmet': 'Elmo Celeste', 'item.mistech.celestial_chestplate': 'Peitoral Celeste',
    'item.mistech.celestial_leggings': 'Calças Celestes', 'item.mistech.celestial_boots': 'Botas Celestes',
    'item.mistech.celestial_sword': 'Espada Celeste', 'item.mistech.celestial_spear': 'Lança Celeste',
    'item.mistech.celestial_pickaxe': 'Picareta Celeste', 'item.mistech.celestial_axe': 'Machado Celeste',
    'item.mistech.celestial_shovel': 'Pá Celeste', 'item.mistech.celestial_hoe': 'Enxada Celeste',
    'item.mistech.celestial_gear.hint': '10 engastes (runas ×1,75). Constelação (35%%). Cada peça vestida: +30%% Sol, +20%% Lua e Ascendente.',
    'item.mistech.celestial_gear.set': 'Conjunto (Celeste): +12 de vida; tudo do Solar; Resistência I à noite.',
    'item.mistech.celestial_gear.ability': '5×5 e árvores; ímã; golpe carregado solta onda e raio de luz.',
    'info.mistech.celestial_core': '§bNúcleo Celeste§r: engastes não se gastam',
    'info.mistech.structure_missing': 'Faltam %s peças (Pedra Guia mostra)',
    'message.mistech.celestial_core.installed': 'Núcleo Celeste instalado: as runas engastadas não se gastam mais',
    'message.mistech.celestial_core.already': 'Esta máquina já tem um Núcleo Celeste',
    'message.mistech.celestial_core.no_sockets': 'O Núcleo Celeste só serve em máquinas com engastes',
    'multiblock.mistech.piece.forge_floor': 'Bloco de Oricalco ou Adamantina',
    'gui.mistech.star_forge.status.forging': 'Forjando', 'gui.mistech.star_forge.status.structure': 'Estrutura incompleta (faltam %s)',
    'gui.mistech.star_forge.status.day': 'Só forja à noite', 'gui.mistech.star_forge.status.sky': 'Precisa de céu aberto',
    'gui.mistech.star_forge.status.taint': 'Mácula demais no lugar', 'gui.mistech.star_forge.status.no_metal': 'Falta metal',
    'gui.mistech.star_forge.status.no_essence': 'Falta Essência Purificada', 'gui.mistech.star_forge.status.no_fragment': 'Falta Fragmento de Estrela',
    'gui.mistech.star_forge.status.no_energy': 'Sem energia', 'gui.mistech.star_forge.status.full': 'Saída cheia',
    'gui.mistech.dream_reactor.status.ok': 'Sonhando', 'gui.mistech.dream_reactor.status.structure': 'Estrutura incompleta',
    'gui.mistech.dream_reactor.status.no_fuel': 'Sem sonhos para queimar', 'gui.mistech.dream_reactor.status.full': 'Cheio',
    'gui.mistech.dream_reactor.bonus': 'Bônus de sonho: %s%%',
}
EN = {
    'container.mistech.star_forge': 'Star Forge', 'container.mistech.dream_reactor': 'Dream Reactor',
    'container.mistech.celestial_battery': 'Celestial Battery', 'container.mistech.celestial_conjurer': 'Celestial Conjurer',
    'item.mistech.celestial_ingot': 'Celestial Ingot', 'block.mistech.celestial_block': 'Block of Celestial Metal',
    'item.mistech.celestial_core': 'Celestial Core',
    'item.mistech.celestial_core.tooltip': 'Right-click a machine with sockets: its runes stop wearing out.',
    'block.mistech.star_forge': 'Star Forge', 'block.mistech.dream_reactor': 'Dream Reactor',
    'block.mistech.celestial_battery': 'Celestial Battery', 'block.mistech.celestial_conjurer': 'Celestial Conjurer',
    'item.mistech.celestial_helmet': 'Celestial Helmet', 'item.mistech.celestial_chestplate': 'Celestial Chestplate',
    'item.mistech.celestial_leggings': 'Celestial Leggings', 'item.mistech.celestial_boots': 'Celestial Boots',
    'item.mistech.celestial_sword': 'Celestial Sword', 'item.mistech.celestial_spear': 'Celestial Spear',
    'item.mistech.celestial_pickaxe': 'Celestial Pickaxe', 'item.mistech.celestial_axe': 'Celestial Axe',
    'item.mistech.celestial_shovel': 'Celestial Shovel', 'item.mistech.celestial_hoe': 'Celestial Hoe',
    'item.mistech.celestial_gear.hint': '10 sockets (runes ×1.75). Constellation (35%%). Each worn piece: +30%% Sun, +20%% Moon and Rising.',
    'item.mistech.celestial_gear.set': 'Set (Celestial): +12 health; everything from Solar; Resistance I at night.',
    'item.mistech.celestial_gear.ability': '5×5 and trees; magnet; charged hits release a wave and a ray of light.',
    'info.mistech.celestial_core': '§bCelestial Core§r: sockets never wear out',
    'info.mistech.structure_missing': '%s pieces missing (Guide Stone shows them)',
    'message.mistech.celestial_core.installed': 'Celestial Core installed: socketed runes no longer wear out',
    'message.mistech.celestial_core.already': 'This machine already has a Celestial Core',
    'message.mistech.celestial_core.no_sockets': 'The Celestial Core only works in machines with sockets',
    'multiblock.mistech.piece.forge_floor': 'Orichalcum or Adamantine Block',
    'gui.mistech.star_forge.status.forging': 'Forging', 'gui.mistech.star_forge.status.structure': 'Structure incomplete (%s missing)',
    'gui.mistech.star_forge.status.day': 'Forges only at night', 'gui.mistech.star_forge.status.sky': 'Needs open sky',
    'gui.mistech.star_forge.status.taint': 'Too much taint here', 'gui.mistech.star_forge.status.no_metal': 'Missing metal',
    'gui.mistech.star_forge.status.no_essence': 'Missing Purified Essence', 'gui.mistech.star_forge.status.no_fragment': 'Missing Star Fragment',
    'gui.mistech.star_forge.status.no_energy': 'No energy', 'gui.mistech.star_forge.status.full': 'Output full',
    'gui.mistech.dream_reactor.status.ok': 'Dreaming', 'gui.mistech.dream_reactor.status.structure': 'Structure incomplete',
    'gui.mistech.dream_reactor.status.no_fuel': 'No dreams to burn', 'gui.mistech.dream_reactor.status.full': 'Full',
    'gui.mistech.dream_reactor.bonus': 'Dream bonus: %s%%',
}
GUIDE_PT = {
    'guide.mistech.entry.star_forge': 'Forja Estelar', 'guide.mistech.entry.celestial_core': 'Núcleo Celeste',
    'guide.mistech.entry.celestial_battery': 'Bateria Celeste', 'guide.mistech.entry.dream_reactor': 'Reator Onírico',
    'guide.mistech.entry.celestial_gear': 'Equipamento Celeste', 'guide.mistech.entry.celestial_conjurer': 'Conjurador Celeste',
    'guide.mistech.ritual.star_forge': 'Acender a Forja Estelar', 'guide.mistech.ritual.dream_reactor': 'Reator que Sonha',
    'guide.mistech.ritual.celestial_core': 'Núcleo Celeste', 'guide.mistech.ritual.celestial_conjurer': 'Conjurador Celeste',
    'guide.mistech.ritual.celestial_gear': 'Revestir de Céu',
    'guide.mistech.star_forge.1': 'A §bForja Estelar§r é o único jeito de fazer §bLingote Celeste§r: funde os três metais com essência e a luz de uma estrela caída.\n\nCada lingote: §e144 mB§r de Mitril, Adamantina e Oricalco, §e250 mB§r de Essência Purificada, 1 §bFragmento de Estrela§r e §e500.000 FE§r, em 2 min.',
    'guide.mistech.star_forge.2': 'Metais: lingote (144 mB), bloco (1.296 mB) ou balde de metal fundido no espaço de cima — ou por canos. Essência: frasco/balde na mão ou canos.\n\nSó forja §9à noite§r (o Eclipse vale), com §9céu aberto§r acima do vidro e a §5Mácula do lugar§r abaixo de 20. Na §eChuva de Estrelas§r vai §eduas vezes mais rápido§r.',
    'guide.mistech.star_forge.legend': 'Chão 5×5 (y −1) de blocos de §6Oricalco§r ou §cAdamantina§r. §bObeliscos de Mitril§r nos cantos, §6Braseiros de Oricalco§r no meio das bordas, §fvidro§r em cima do coração (F). A Pedra Guia mostra o holograma.',
    'guide.mistech.celestial_core.1': 'O §bNúcleo Celeste§r se instala com botão direito numa máquina com engastes: as runas engastadas §enunca mais se gastam§r. No §9Prisma Astral§r, cada minério dá §e6 pós§r em vez de 5.\n\nSai junto quando a máquina é quebrada.',
    'guide.mistech.celestial_battery.1': 'A §bBateria Celeste§r guarda §e10.000.000 FE§r. Sem fio, como a Aprimorada, só que maior: suga geradores a até §e5 blocos§r e energiza máquinas a até §e8§r, §e10.000 FE/t§r cada. Pelas faces, até §e100.000 FE/t§r.\n\nQuebrada, guarda a energia.',
    'guide.mistech.dream_reactor.1': 'O §dReator Onírico§r queima sonhos: §e2.000 FE/t§r. Fragmento de Sonho e Pó de Sonho valem §e100.000 FE§r, o Amplificado §e250.000§r e o §dEstilhaço de Memória§r §e1.000.000§r.\n\n§dDormindo§r a até 32 blocos (ou no Reino dos Sonhos), o dono rende §e×2§r. Montado no próprio Reino dos Sonhos: §e×1,5§r.',
    'guide.mistech.dream_reactor.2': '§cPesadelos§r: de 10 em 10 min ligado, 15% de chance de um §cBicho-Papão§r ou uma §cSombra§r escapar perto do reator. Um §fApanha Sonho Purificado§r a até 8 blocos segura os pesadelos.\n\nBuffer de 10.000.000 FE, entrega até 10.000 FE/t por face.',
    'guide.mistech.dream_reactor.legend': 'Cubo 3×3×3 em volta do núcleo: §ePedra Estelar§r nos 8 cantos, §fLã de Nuvem§r no resto e a frente (para onde o núcleo olha) §7aberta§r no meio. Corte do meio:',
    'guide.mistech.celestial_gear.1': 'Qualquer peça de §eOricalco Puro§r vira §bCeleste§r no altar Tier III, à noite: §e5.000.000 FE§r, §e10 engastes§r e §ecada runa vale ×1,75§r.\n\nTudo do Oricalco Puro continua valendo. Ferramentas: velocidade 24, +14 de dano. Armadura: 6/12/9/6, firmeza 12.',
    'guide.mistech.celestial_gear.2': '§bConjunto Celeste§r: §e+12§r de vida no total, tudo do conjunto Solar e §eResistência I§r à noite.\n\nCada peça vestida reforça o §eSol em 30%§r e a Lua e o Ascendente em 20%.',
    'guide.mistech.celestial_conjurer.1': 'O §bConjurador Celeste§r tem §edois Cristais de Feitiço§r que se alternam a cada lançamento. As magias saem com §e×1,25 de poder§r e a recarga é §e25% menor§r.\n\nEngastes: 3. O resto é igual ao Conjurador Rúnico.',
}
GUIDE_EN = {
    'guide.mistech.entry.star_forge': 'Star Forge', 'guide.mistech.entry.celestial_core': 'Celestial Core',
    'guide.mistech.entry.celestial_battery': 'Celestial Battery', 'guide.mistech.entry.dream_reactor': 'Dream Reactor',
    'guide.mistech.entry.celestial_gear': 'Celestial Gear', 'guide.mistech.entry.celestial_conjurer': 'Celestial Conjurer',
    'guide.mistech.ritual.star_forge': 'Kindling the Star Forge', 'guide.mistech.ritual.dream_reactor': 'A Reactor That Dreams',
    'guide.mistech.ritual.celestial_core': 'Celestial Core', 'guide.mistech.ritual.celestial_conjurer': 'Celestial Conjurer',
    'guide.mistech.ritual.celestial_gear': 'Clad in Sky',
    'guide.mistech.star_forge.1': 'The §bStar Forge§r is the only way to make §bCelestial Ingots§r: it fuses the three metals with essence and the light of a fallen star.\n\nEach ingot: §e144 mB§r of Mithril, Adamantine and Orichalcum, §e250 mB§r of Purified Essence, 1 §bStar Fragment§r and §e500,000 FE§r, over 2 min.',
    'guide.mistech.star_forge.2': 'Metals: ingot (144 mB), block (1,296 mB) or molten metal bucket in the top slot — or pipes. Essence: bottle/bucket in hand or pipes.\n\nIt only forges §9at night§r (Eclipse counts), with §9open sky§r above the glass and the §5place taint§r below 20. During a §eStarfall§r it works §etwice as fast§r.',
    'guide.mistech.star_forge.legend': '5×5 floor (y −1) of §6Orichalcum§r or §cAdamantine§r blocks. §bMithril Obelisks§r at the corners, §6Orichalcum Braziers§r at the middle of the edges, §fglass§r above the heart (F). The Guide Stone shows the hologram.',
    'guide.mistech.celestial_core.1': 'Right-click a machine with sockets to install the §bCelestial Core§r: its socketed runes §enever wear out again§r. In the §9Astral Prism§r, each ore gives §e6 dusts§r instead of 5.\n\nIt drops when the machine is broken.',
    'guide.mistech.celestial_battery.1': 'The §bCelestial Battery§r stores §e10,000,000 FE§r. Wireless like the Advanced one, only bigger: pulls from generators up to §e5 blocks§r away and powers machines up to §e8§r, §e10,000 FE/t§r each. Through its faces, up to §e100,000 FE/t§r.\n\nKeeps its energy when broken.',
    'guide.mistech.dream_reactor.1': 'The §dDream Reactor§r burns dreams: §e2,000 FE/t§r. Dream Fragments and Dream Dust are worth §e100,000 FE§r, the Amplified one §e250,000§r and a §dMemory Shard§r §e1,000,000§r.\n\nWhile its owner §dsleeps§r within 32 blocks (or is in the Dream Realm) it yields §e×2§r. Built inside the Dream Realm: §e×1.5§r.',
    'guide.mistech.dream_reactor.2': '§cNightmares§r: every 10 min it runs there is a 15% chance a §cBogeyman§r or a §cShade§r escapes near the reactor. A §fPurified Dreamcatcher§r within 8 blocks holds the nightmares back.\n\n10,000,000 FE buffer, outputs up to 10,000 FE/t per face.',
    'guide.mistech.dream_reactor.legend': '3×3×3 cube around the core: §eStar Stone§r at the 8 corners, §fCloud Wool§r everywhere else, and the front (where the core faces) §7open§r in the middle. Middle slice:',
    'guide.mistech.celestial_gear.1': 'Any §ePure Orichalcum§r piece becomes §bCelestial§r on a Tier III altar, at night: §e5,000,000 FE§r, §e10 sockets§r and §eeach rune counts ×1.75§r.\n\nEverything from Pure Orichalcum still applies. Tools: speed 24, +14 damage. Armor: 6/12/9/6, toughness 12.',
    'guide.mistech.celestial_gear.2': '§bCelestial set§r: §e+12§r health in total, everything from the Solar set and §eResistance I§r at night.\n\nEach worn piece makes your §eSun 30% stronger§r and Moon and Rising 20%.',
    'guide.mistech.celestial_conjurer.1': 'The §bCelestial Conjurer§r holds §etwo Spell Crystals§r that take turns on every cast. Spells come out with §e×1.25 power§r and §e25% less cooldown§r.\n\nSockets: 3. Everything else works like the Runic Conjurer.',
}
for name, d in (('pt_br', dict(PT, **GUIDE_PT)), ('en_us', dict(EN, **GUIDE_EN))):
    p = LANG + name + '.json'
    data = load(p)
    data.update(d)
    dump(p, data)


# ---------------------------------------------------------------- Grimório
def rpage(name, title, text=None):
    r = RITUALS[name]
    p = {'type': 'ritual', 'catalyst': r['catalyst'], 'ingredients': r['ingredients'], 'essence': r['essence'],
         'result': r['result']['id'], 'min_tier': r['min_tier'], 'condition': r['condition'], 'title': title}
    if text:
        p['text'] = text
    return p


def text(key):
    return {'type': 'text', 'text': key}


def item(*ids):
    return {'type': 'item', 'items': ['mistech:' + i for i in ids]}


MACH = [
    {'id': 'star_forge', 'icon': 'mistech:star_forge', 'name': 'guide.mistech.entry.star_forge',
     'deps': ['astral_prism', 'crusher_forge', 'tier3_rituals'], 'task': item('celestial_ingot'), 'x': 11.2, 'y': 5.5,
     'pages': [rpage('star_forge', 'guide.mistech.ritual.star_forge'), text('guide.mistech.star_forge.1'), text('guide.mistech.star_forge.2'),
               {'type': 'multiblock', 'pattern': ['O0B0O', '00000', 'B0F0B', '00000', 'O0B0O'],
                'key': {'F': 'mistech:star_forge', 'O': 'mistech:mithril_obelisk', 'B': 'mistech:orichalcum_brazier'},
                'text': 'guide.mistech.star_forge.legend'}]},
    {'id': 'celestial_core', 'icon': 'mistech:celestial_core', 'name': 'guide.mistech.entry.celestial_core',
     'deps': ['star_forge', 'machine_sockets'], 'task': item('celestial_core'), 'x': 12.8, 'y': 4.5,
     'pages': [rpage('celestial_core', 'guide.mistech.ritual.celestial_core', 'guide.mistech.celestial_core.1')]},
    {'id': 'celestial_battery', 'icon': 'mistech:celestial_battery', 'name': 'guide.mistech.entry.celestial_battery',
     'deps': ['star_forge', 'advanced_runic_battery'], 'task': item('celestial_battery'), 'x': 12.8, 'y': 6.5,
     'pages': [{'type': 'crafting', 'grid': [BATTERY_RECIPE[1][c] for row in BATTERY_RECIPE[0] for c in row],
                'result': 'mistech:celestial_battery', 'text': 'guide.mistech.celestial_battery.1'}]},
    {'id': 'dream_reactor', 'icon': 'mistech:dream_reactor', 'name': 'guide.mistech.entry.dream_reactor',
     'deps': ['impure_reactor', 'dream_fragments', 'tier3_rituals'], 'task': item('dream_reactor'), 'x': 11.2, 'y': 2.09,
     'pages': [rpage('dream_reactor', 'guide.mistech.ritual.dream_reactor'), text('guide.mistech.dream_reactor.1'),
               text('guide.mistech.dream_reactor.2'),
               {'type': 'multiblock', 'pattern': ['WWW', 'WR0', 'WWW'],
                'key': {'R': 'mistech:dream_reactor', 'W': 'mistech:cloud_wool'}, 'text': 'guide.mistech.dream_reactor.legend'}]},
]
GEAR_ENTRY = [
    {'id': 'celestial_gear', 'icon': 'mistech:celestial_chestplate', 'name': 'guide.mistech.entry.celestial_gear',
     'deps': ['pure_orichalcum_gear', 'star_forge'], 'task': item(*['celestial_' + g for g in GEAR]), 'x': 16.0, 'y': 3,
     'pages': [rpage('celestial_chestplate', 'guide.mistech.ritual.celestial_gear', 'guide.mistech.celestial_gear.1'),
               text('guide.mistech.celestial_gear.2')]},
]
SPELLS = [
    {'id': 'celestial_conjurer', 'icon': 'mistech:celestial_conjurer', 'name': 'guide.mistech.entry.celestial_conjurer',
     'deps': ['runic_conjurer', 'star_forge'], 'task': item('celestial_conjurer'), 'x': 9.6, 'y': 0.5,
     'pages': [rpage('celestial_conjurer', 'guide.mistech.ritual.celestial_conjurer', 'guide.mistech.celestial_conjurer.1')]},
]
BOOK = A + 'guide/book.json'
book = load(BOOK)
for cid, entries in (('machines2', MACH), ('rituals', GEAR_ENTRY), ('spells', SPELLS)):
    cat = next(c for c in book['categories'] if c['id'] == cid)
    ids = {e['id'] for e in entries}
    cat['entries'] = [e for e in cat['entries'] if e['id'] not in ids] + entries
dump(BOOK, book, newline=False)
print('ok')
