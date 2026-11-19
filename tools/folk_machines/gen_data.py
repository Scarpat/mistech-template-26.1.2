#!/usr/bin/env python3
"""Dados da Fase 7 (máquinas de folclore): modelos, blockstates, loot, tags, receitas, textos e Grimório.
Idempotente. Depois rode tools/guide/views.py.
Uso (na pasta do projeto): python3 tools/folk_machines/gen_data.py"""
import json, os

R = 'src/main/resources/'
A = R + 'assets/mistech/'
D = R + 'data/'
MACHINES = ['saci_windmill', 'iara_pump', 'curupira_stump', 'potion_still', 'scroll_printer']
WOODEN = ['saci_windmill', 'curupira_stump', 'scroll_printer']


def dump(path, obj, newline=True):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        if newline:
            f.write('\n')


# ---------------------------------------------------------------- receitas
RECIPES = {
    'saci_windmill': (['WWW', 'RJR', 'PCP'], {'W': '#minecraft:planks', 'R': 'mistech:air_rune', 'J': 'mistech:glass_jar',
                                               'P': 'minecraft:copper_ingot', 'C': 'mistech:runic_battery'}),
    'iara_pump': (['PBP', 'WTW', 'CCC'], {'P': 'minecraft:prismarine_shard', 'B': 'minecraft:bucket', 'W': 'mistech:water_rune',
                                           'T': 'mistech:runic_tank', 'C': 'minecraft:copper_block'}),
    'curupira_stump': (['SAS', 'LHL', 'LLL'], {'S': '#minecraft:saplings', 'A': 'minecraft:iron_axe', 'L': '#minecraft:logs',
                                                'H': 'mistech:runic_harvester'}),
    'potion_still': (['CGC', 'CBC', 'IFI'], {'C': 'minecraft:copper_ingot', 'G': '#c:glass_blocks', 'B': 'minecraft:brewing_stand',
                                              'I': 'minecraft:iron_ingot', 'F': 'minecraft:furnace'}),
    'scroll_printer': (['PAP', 'LWL', 'OTO'], {'P': 'minecraft:paper', 'A': 'mistech:arcane_rune', 'L': 'minecraft:lapis_block',
                                                'W': 'mistech:writing_table', 'O': '#minecraft:planks', 'T': 'mistech:runic_tank'}),
}
GRIDS = {}
for name, (pattern, key) in RECIPES.items():
    dump(D + 'mistech/recipe/%s.json' % name, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
                                               'result': {'id': 'mistech:' + name, 'count': 1}})
    GRIDS[name] = [key[ch] for row in pattern for ch in row]
    dump(D + 'mistech/loot_table/blocks/%s.json' % name, {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'mistech:' + name}],
                                              'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': 'mistech:blocks/' + name})
for tag, names in (('mineable/axe', WOODEN), ('mineable/pickaxe', [m for m in MACHINES if m not in WOODEN])):
    p = D + 'minecraft/tags/block/%s.json' % tag
    j = json.load(open(p)) if os.path.exists(p) else {'replace': False, 'values': []}
    for n in names:
        if 'mistech:' + n not in j['values']:
            j['values'].append('mistech:' + n)
    dump(p, j)

# Brasa do Boitatá: 1–2 do Boitatá selvagem
p = D + 'mistech/loot_table/entities/boitata.json'
j = json.load(open(p))
if not any(e.get('name') == 'mistech:boitata_ember' for pool in j['pools'] for e in pool['entries']):
    j['pools'].append({'rolls': 1, 'bonus_rolls': 0.0, 'entries': [{'type': 'minecraft:item', 'name': 'mistech:boitata_ember',
                                                                   'functions': [{'function': 'minecraft:set_count',
                                                                                  'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}}]}]})
    dump(p, j, newline=False)
dump(A + 'items/boitata_ember.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:item/boitata_ember'}})
dump(A + 'models/item/boitata_ember.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mistech:item/boitata_ember'}})


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
        faces[d] = {'texture': tex[d] if isinstance(tex, dict) else tex, 'uv': uv[d]}
    e = {'from': f, 'to': t, 'faces': faces}
    if light:
        e['light_emission'] = light
    return e


def model(textures, els):
    return {'parent': 'minecraft:block/block', 'textures': textures, 'elements': els}


def windmill(on):
    return model({'particle': 'minecraft:block/spruce_planks', 'stone': 'minecraft:block/cobblestone', 'wood': 'minecraft:block/spruce_planks',
                  'roof': 'minecraft:block/dark_oak_planks', 'hub': 'minecraft:block/copper_block', 'door': 'minecraft:block/dark_oak_planks'}, [
        cube([1, 0, 1], [15, 6, 15], '#stone'),
        cube([2, 6, 2], [14, 14, 14], '#wood'),
        cube([1, 14, 1], [15, 15, 15], '#roof'),
        cube([3, 15, 3], [13, 16, 13], '#roof'),
        cube([7, 7, 0.5], [9, 11, 2], '#hub'),
        cube([6, 0, 0.5], [10, 5, 1], '#door', skip=('up',)),
    ])


def pump(on):
    surface = 'mistech:block/mana_well_mana_on' if on else 'mistech:block/mana_well_mana'
    return model({'particle': 'minecraft:block/prismarine', 'base': 'minecraft:block/prismarine', 'copper': 'minecraft:block/copper_block',
                  'water': surface, 'dark': 'minecraft:block/dark_prismarine'}, [
        cube([1, 0, 1], [15, 4, 15], '#base'),
        cube([5, 4, 5], [11, 11, 11], '#copper'),
        cube([2, 11, 2], [14, 12, 14], '#dark'),
        cube([2, 12, 2], [14, 16, 3], '#base'), cube([2, 12, 13], [14, 16, 14], '#base'),
        cube([2, 12, 3], [3, 16, 13], '#base'), cube([13, 12, 3], [14, 16, 13], '#base'),
        cube([3, 12, 3], [13, 14.5 if on else 13.5, 13], '#water', skip=('north', 'south', 'east', 'west', 'down'), light=8 if on else None),
        cube([11, 6, 7], [15.5, 8, 9], '#copper'),
    ])


def stump(on):
    return model({'particle': 'minecraft:block/oak_log', 'bark': 'minecraft:block/oak_log', 'top': 'minecraft:block/oak_log_top',
                  'moss': 'minecraft:block/moss_block', 'fire': 'minecraft:block/magma' if on else 'minecraft:block/oak_log_top'}, [
        cube([2, 0, 2], [14, 10, 14], {'north': '#bark', 'south': '#bark', 'east': '#bark', 'west': '#bark', 'up': '#top', 'down': '#top'}),
        cube([0, 0, 6], [2, 3, 10], '#bark'), cube([14, 0, 5], [16, 2, 9], '#bark'),
        cube([6, 0, 0], [10, 2, 2], '#bark'), cube([5, 0, 14], [9, 3, 16], '#bark'),
        cube([3, 10, 3], [7, 10.5, 8], '#moss'),
        cube([9, 10, 9], [11, 11, 11], '#fire', light=10 if on else None),
    ])


def still(on):
    front = 'minecraft:block/blast_furnace_front_on' if on else 'minecraft:block/blast_furnace_front'
    return model({'particle': 'minecraft:block/copper_block', 'brick': 'minecraft:block/bricks', 'front': front,
                  'copper': 'minecraft:block/copper_block', 'glass': 'minecraft:block/glass'}, [
        cube([2, 0, 2], [14, 5, 14], {'north': '#front', 'south': '#brick', 'east': '#brick', 'west': '#brick', 'up': '#brick', 'down': '#brick'},
             light=6 if on else None),
        cube([3, 5, 3], [11, 12, 11], '#copper'),
        cube([5, 12, 5], [9, 14, 9], '#copper'),
        cube([6.5, 14, 6.5], [7.5, 15, 13], '#copper'),
        cube([7.5, 9, 12], [8.5, 15, 13], '#copper'),
        cube([11, 5, 10], [15, 9, 14], '#glass'),
    ])


def printer(on):
    return model({'particle': 'minecraft:block/dark_oak_planks', 'wood': 'minecraft:block/dark_oak_planks', 'iron': 'minecraft:block/iron_block',
                  'paper': 'minecraft:block/white_wool', 'lapis': 'minecraft:block/lapis_block'}, [
        cube([1, 0, 1], [15, 7, 15], '#wood'),
        cube([2, 7, 3], [14, 7.5, 13], '#paper'),
        cube([1, 7, 1], [3, 15, 3], '#wood'), cube([13, 7, 1], [15, 15, 3], '#wood'),
        cube([1, 7, 13], [3, 15, 15], '#wood'), cube([13, 7, 13], [15, 15, 15], '#wood'),
        cube([1, 15, 1], [15, 16, 15], '#wood'),
        cube([3, 10 if on else 11, 3], [13, 12 if on else 13, 13], '#iron'),
        cube([7, 12 if on else 13, 7], [9, 15, 9], '#iron'),
        cube([12, 1, 0.5], [14, 3, 1], '#lapis', skip=('up',)),
    ])


for name, fn in (('saci_windmill', windmill), ('iara_pump', pump), ('curupira_stump', stump), ('potion_still', still),
                 ('scroll_printer', printer)):
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

# ---------------------------------------------------------------- textos
LANG = A + 'lang/'
PT = {
    'item.mistech.boitata_ember': 'Brasa do Boitatá',
    'message.mistech.boitata_mount.ember_fed': 'O Boitatá guarda o calor: uma Brasa vem em 10 minutos',
    'block.mistech.saci_windmill': 'Moinho do Saci', 'container.mistech.saci_windmill': 'Moinho do Saci',
    'block.mistech.iara_pump': 'Bomba da Iara', 'container.mistech.iara_pump': 'Bomba da Iara',
    'block.mistech.curupira_stump': 'Toco do Curupira', 'container.mistech.curupira_stump': 'Toco do Curupira',
    'block.mistech.potion_still': 'Alambique de Poções', 'container.mistech.potion_still': 'Alambique de Poções',
    'block.mistech.scroll_printer': 'Impressora de Pergaminhos', 'container.mistech.scroll_printer': 'Impressora de Pergaminhos',
    'gui.mistech.saci_windmill.status.happy': 'Saci contente (+25%)', 'gui.mistech.saci_windmill.status.grumpy': 'Saci sem fumo',
    'gui.mistech.saci_windmill.status.prank': 'O Saci está aprontando!', 'gui.mistech.saci_windmill.status.no_saci': 'Sem Saci',
    'gui.mistech.saci_windmill.status.blocked': 'Pás sem espaço', 'gui.mistech.saci_windmill.status.full': 'Energia cheia',
    'message.mistech.saci_windmill.prank': 'Uma risada no vento... o Saci travou o moinho!',
    'gui.mistech.iara_pump.status.pumping': 'Bombeando', 'gui.mistech.iara_pump.status.river': 'Água do rio (sem fim)',
    'gui.mistech.iara_pump.status.no_scale': 'Sem Escama da Iara', 'gui.mistech.iara_pump.status.no_energy': 'Sem energia',
    'gui.mistech.iara_pump.status.dry': 'Nada para puxar', 'gui.mistech.iara_pump.status.full': 'Tanque cheio',
    'gui.mistech.iara_pump.tank': 'Fluido',
    'gui.mistech.curupira_stump.status.working': 'Cuidando da mata', 'gui.mistech.curupira_stump.status.hungry': 'Quer cachaça',
    'gui.mistech.curupira_stump.status.no_energy': 'Sem energia', 'gui.mistech.curupira_stump.status.full': 'Saída cheia',
    'gui.mistech.curupira_stump.credit': 'Árvores pagas: %s',
    'gui.mistech.potion_still.status.idle': 'Parado', 'gui.mistech.potion_still.status.brewing': 'Destilando',
    'gui.mistech.potion_still.status.cuca': 'Mistura da Cuca', 'gui.mistech.potion_still.status.no_water': 'Sem água',
    'gui.mistech.potion_still.status.no_bottles': 'Sem frascos', 'gui.mistech.potion_still.status.no_energy': 'Sem energia',
    'gui.mistech.potion_still.status.full': 'Saída cheia', 'gui.mistech.potion_still.batch': 'Lote: %s poções',
    'gui.mistech.scroll_printer.status.idle': 'Sem matriz', 'gui.mistech.scroll_printer.status.printing': 'Imprimindo',
    'gui.mistech.scroll_printer.status.world_only': 'Magia que não se copia', 'gui.mistech.scroll_printer.status.no_paper': 'Sem papel',
    'gui.mistech.scroll_printer.status.no_lapis': 'Faltam 2 lápis-lazúli', 'gui.mistech.scroll_printer.status.no_essence': 'Sem essência',
    'gui.mistech.scroll_printer.status.no_energy': 'Sem energia', 'gui.mistech.scroll_printer.status.full': 'Saída cheia',
    'guide.mistech.entry.saci_windmill': 'Moinho do Saci', 'guide.mistech.entry.iara_pump': 'Bomba da Iara',
    'guide.mistech.entry.curupira_stump': 'Toco do Curupira', 'guide.mistech.entry.boitata_ember': 'Brasa do Boitatá',
    'guide.mistech.entry.potion_still': 'Alambique de Poções', 'guide.mistech.entry.scroll_printer': 'Impressora de Pergaminhos',
    'guide.mistech.saci_windmill.recipe': 'O §6Moinho do Saci§r é um gerador de vento movido a redemoinho.',
    'guide.mistech.saci_windmill.1': 'Ponha um §6Saci Engarrafado§r no espaço da direita (ele não é gasto) e deixe livres os 3×3 blocos na frente, onde giram as pás.\n\n§e20 FE/t§r até Y 64, subindo até §e150 FE/t§r em Y 200. Chuva +25%, tempestade +50%.',
    'guide.mistech.saci_windmill.2': '§lA vontade do Saci§r\nUma §6Folha de Tabaco Seca§r a cada 10 min o deixa contente: +25%.\n\nSem fumo, ele fica emburrado e, de vez em quando (2% por minuto), §capronta§r: trava o moinho por 30 s.',
    'guide.mistech.iara_pump.recipe': 'A §3Bomba da Iara§r puxa fluidos com a ajuda de uma Escama da Iara.',
    'guide.mistech.iara_pump.1': 'Com uma §3Escama da Iara§r no espaço (não é gasta), puxa blocos de fonte de qualquer fluido num raio de 16 e até 16 para baixo: 1 bloco a cada 0,5 s, 50 FE cada, para um tanque de 16.000 mB.\n\nNum §9rio ou oceano§r, dá água sem fim: 1.000 mB/s, sem secar nada.',
    'guide.mistech.iara_pump.2': 'Balde vazio no espaço da esquerda sai cheio no do meio; canos puxam do tanque.\n\n§lO canto§r\nÀ noite, quem estiver a até 8 blocos §csem Arruda§r no inventário ouve a Iara: Náusea leve. Só susto, sem dano.',
    'guide.mistech.curupira_stump.recipe': 'O §2Toco do Curupira§r cuida de uma mata inteira.',
    'guide.mistech.curupira_stump.1': 'Fazenda de árvores 9×9 à frente: planta as §2mudas§r do espaço de baixo, derruba a árvore inteira (tronco e folhas) quando cresce, guarda tudo e replanta. As mudas que caem voltam para o espaço. 30 FE/t.\n\nServe para qualquer árvore, até a Árvore Turva e as dos Sonhos.',
    'guide.mistech.curupira_stump.2': '§lA vontade do Curupira§r\nUma §6Cachaça§r (qualquer uma) paga 20 árvores. Sem oferenda, ele para e deixa §cpegadas viradas para trás§r em volta do toco.',
    'guide.mistech.boitata_ember.1': 'A §6Brasa do Boitatá§r cai do Boitatá selvagem (1 ou 2) e da sua montaria: alimente o Boitatá adulto e, 10 min depois, ele solta uma.\n\nNa §6Caldeira Ígnea§r é o melhor catalisador: §e×3 por 4 minutos§r, e enquanto queima a caldeira §enão gasta lava§r.',
    'guide.mistech.potion_still.recipe': 'O §5Alambique de Poções§r faz poções em lote, sem suporte de poções.',
    'guide.mistech.potion_still.1': 'Encha o tanque de água (balde na mão ou canos), ponha §5frascos vazios§r na esquerda e os ingredientes nos 3 espaços de cima, na ordem: ex. Fungo do Nether, Açúcar, Redstone.\n\nFaz lotes de até §e8 poções§r (333 mB de água cada), 20 s por passo a 30 FE/t, 1 ingrediente a cada 3 poções. Serve para as receitas de outros mods também.',
    'guide.mistech.potion_still.2': '§5Modo Cuca§r\nPonha 2 ou 3 poções prontas nos espaços de ingrediente: o alambique junta tudo numa §5Poção da Cuca§r, como o Caldeirão dela (durações somam até 8 min). Um Frasco de Essência Impura junto sobe 1 nível de tudo.',
    'guide.mistech.scroll_printer.recipe': 'A §9Impressora de Pergaminhos§r copia um pergaminho de magia.',
    'guide.mistech.scroll_printer.1': 'O pergaminho de cima é a §9Matriz§r: não é gasto. Cada cópia custa 1 papel, 2 lápis-lazúli, 100 mB de Essência Purificada e 5.000 FE, em 60 s.\n\nMagias §c"só no mundo"§r não podem ser copiadas: elas continuam raras.',
}
EN = {
    'item.mistech.boitata_ember': 'Boitatá Ember',
    'message.mistech.boitata_mount.ember_fed': 'The Boitatá keeps the heat: an Ember comes in 10 minutes',
    'block.mistech.saci_windmill': 'Saci Windmill', 'container.mistech.saci_windmill': 'Saci Windmill',
    'block.mistech.iara_pump': 'Iara Pump', 'container.mistech.iara_pump': 'Iara Pump',
    'block.mistech.curupira_stump': 'Curupira Stump', 'container.mistech.curupira_stump': 'Curupira Stump',
    'block.mistech.potion_still': 'Potion Still', 'container.mistech.potion_still': 'Potion Still',
    'block.mistech.scroll_printer': 'Scroll Printer', 'container.mistech.scroll_printer': 'Scroll Printer',
    'gui.mistech.saci_windmill.status.happy': 'Happy Saci (+25%)', 'gui.mistech.saci_windmill.status.grumpy': 'Saci wants a smoke',
    'gui.mistech.saci_windmill.status.prank': 'The Saci is pranking!', 'gui.mistech.saci_windmill.status.no_saci': 'No Saci',
    'gui.mistech.saci_windmill.status.blocked': 'Blades blocked', 'gui.mistech.saci_windmill.status.full': 'Energy full',
    'message.mistech.saci_windmill.prank': 'A laugh in the wind... the Saci jammed the windmill!',
    'gui.mistech.iara_pump.status.pumping': 'Pumping', 'gui.mistech.iara_pump.status.river': 'River water (endless)',
    'gui.mistech.iara_pump.status.no_scale': 'No Iara Scale', 'gui.mistech.iara_pump.status.no_energy': 'No energy',
    'gui.mistech.iara_pump.status.dry': 'Nothing to pull', 'gui.mistech.iara_pump.status.full': 'Tank full',
    'gui.mistech.iara_pump.tank': 'Fluid',
    'gui.mistech.curupira_stump.status.working': 'Tending the woods', 'gui.mistech.curupira_stump.status.hungry': 'Wants cachaça',
    'gui.mistech.curupira_stump.status.no_energy': 'No energy', 'gui.mistech.curupira_stump.status.full': 'Output full',
    'gui.mistech.curupira_stump.credit': 'Trees paid: %s',
    'gui.mistech.potion_still.status.idle': 'Idle', 'gui.mistech.potion_still.status.brewing': 'Distilling',
    'gui.mistech.potion_still.status.cuca': 'Cuca mix', 'gui.mistech.potion_still.status.no_water': 'No water',
    'gui.mistech.potion_still.status.no_bottles': 'No bottles', 'gui.mistech.potion_still.status.no_energy': 'No energy',
    'gui.mistech.potion_still.status.full': 'Output full', 'gui.mistech.potion_still.batch': 'Batch: %s potions',
    'gui.mistech.scroll_printer.status.idle': 'No template', 'gui.mistech.scroll_printer.status.printing': 'Printing',
    'gui.mistech.scroll_printer.status.world_only': 'Spell can\'t be copied', 'gui.mistech.scroll_printer.status.no_paper': 'No paper',
    'gui.mistech.scroll_printer.status.no_lapis': 'Needs 2 lapis lazuli', 'gui.mistech.scroll_printer.status.no_essence': 'No essence',
    'gui.mistech.scroll_printer.status.no_energy': 'No energy', 'gui.mistech.scroll_printer.status.full': 'Output full',
    'guide.mistech.entry.saci_windmill': 'Saci Windmill', 'guide.mistech.entry.iara_pump': 'Iara Pump',
    'guide.mistech.entry.curupira_stump': 'Curupira Stump', 'guide.mistech.entry.boitata_ember': 'Boitatá Ember',
    'guide.mistech.entry.potion_still': 'Potion Still', 'guide.mistech.entry.scroll_printer': 'Scroll Printer',
    'guide.mistech.saci_windmill.recipe': 'The §6Saci Windmill§r is a wind generator driven by a whirlwind.',
    'guide.mistech.saci_windmill.1': 'Put a §6Bottled Saci§r in the right slot (it is not used up) and keep the 3×3 blocks in front clear for the blades.\n\n§e20 FE/t§r up to Y 64, rising to §e150 FE/t§r at Y 200. Rain +25%, thunderstorm +50%.',
    'guide.mistech.saci_windmill.2': '§lThe Saci\'s will§r\nOne §6Dried Tobacco Leaf§r every 10 min keeps him happy: +25%.\n\nWithout a smoke he sulks and, now and then (2% per minute), §cplays a prank§r: jams the windmill for 30 s.',
    'guide.mistech.iara_pump.recipe': 'The §3Iara Pump§r pulls fluids with the help of an Iara Scale.',
    'guide.mistech.iara_pump.1': 'With an §3Iara Scale§r in its slot (not used up), it pulls source blocks of any fluid within 16 blocks and down to 16 below: 1 block every 0.5 s, 50 FE each, into a 16,000 mB tank.\n\nIn a §9river or ocean§r it gives endless water: 1,000 mB/s, draining nothing.',
    'guide.mistech.iara_pump.2': 'An empty bucket in the left slot comes out full in the middle one; pipes pull from the tank.\n\n§lThe song§r\nAt night, anyone within 8 blocks §cwithout Rue§r in their inventory hears the Iara: mild Nausea. Just a scare, no damage.',
    'guide.mistech.curupira_stump.recipe': 'The §2Curupira Stump§r tends a whole grove.',
    'guide.mistech.curupira_stump.1': '9×9 tree farm in front: plants the §2saplings§r from the lower slot, fells the whole tree (logs and leaves) once it grows, stores everything and replants. Dropped saplings go back to the slot. 30 FE/t.\n\nWorks with any tree, even the Murky Tree and the Dream trees.',
    'guide.mistech.curupira_stump.2': '§lThe Curupira\'s will§r\nOne §6Cachaça§r (any kind) pays for 20 trees. Without an offering he stops and leaves §cbackward footprints§r around the stump.',
    'guide.mistech.boitata_ember.1': 'The §6Boitatá Ember§r drops from the wild Boitatá (1 or 2) and from your mount: feed the adult Boitatá and, 10 min later, it drops one.\n\nIn the §6Igneous Boiler§r it is the best catalyst: §e×3 for 4 minutes§r, and while it burns the boiler §euses no lava§r.',
    'guide.mistech.potion_still.recipe': 'The §5Potion Still§r brews potions in batches, no brewing stand needed.',
    'guide.mistech.potion_still.1': 'Fill the tank with water (bucket in hand or pipes), put §5empty bottles§r on the left and the ingredients in the 3 top slots, in order: e.g. Nether Wart, Sugar, Redstone.\n\nMakes batches of up to §e8 potions§r (333 mB of water each), 20 s per step at 30 FE/t, 1 ingredient per 3 potions. Works with other mods\' recipes too.',
    'guide.mistech.potion_still.2': '§5Cuca mode§r\nPut 2 or 3 finished potions in the ingredient slots: the still merges them into a §5Cuca Potion§r, like her Cauldron (durations add up to 8 min). An Impure Essence Bottle alongside raises everything by 1 level.',
    'guide.mistech.scroll_printer.recipe': 'The §9Scroll Printer§r copies a spell scroll.',
    'guide.mistech.scroll_printer.1': 'The top scroll is the §9Template§r: it is not used up. Each copy costs 1 paper, 2 lapis lazuli, 100 mB of Purified Essence and 5,000 FE, in 60 s.\n\n§c"World only"§r spells cannot be copied: they stay rare.',
}
for name, d in (('pt_br', PT), ('en_us', EN)):
    p = LANG + name + '.json'
    data = json.load(open(p, encoding='utf-8'))
    data.update(d)
    dump(p, data)


# ---------------------------------------------------------------- Grimório
def craft(name):
    return {'type': 'crafting', 'grid': GRIDS[name], 'result': 'mistech:' + name, 'text': 'guide.mistech.%s.recipe' % name}


def text(key):
    return {'type': 'text', 'text': key}


FOLK = [
    {'id': 'saci_windmill', 'icon': 'mistech:saci_windmill', 'name': 'guide.mistech.entry.saci_windmill', 'deps': ['saci', 'ash_generator'],
     'task': {'type': 'item', 'items': ['mistech:saci_windmill']}, 'x': 4.8, 'y': -0.25,
     'pages': [craft('saci_windmill'), text('guide.mistech.saci_windmill.1'), text('guide.mistech.saci_windmill.2')]},
    {'id': 'curupira_stump', 'icon': 'mistech:curupira_stump', 'name': 'guide.mistech.entry.curupira_stump', 'deps': ['curupira', 'farming_machines'],
     'task': {'type': 'item', 'items': ['mistech:curupira_stump']}, 'x': 6.4, 'y': 2.0,
     'pages': [craft('curupira_stump'), text('guide.mistech.curupira_stump.1'), text('guide.mistech.curupira_stump.2')]},
    {'id': 'boitata_ember', 'icon': 'mistech:boitata_ember', 'name': 'guide.mistech.entry.boitata_ember', 'deps': ['boitata', 'igneous_boiler'],
     'task': {'type': 'item', 'items': ['mistech:boitata_ember']}, 'x': 8.0, 'y': 3.75,
     'pages': [{'type': 'spotlight', 'item': 'mistech:boitata_ember', 'text': 'guide.mistech.boitata_ember.1'}]},
    {'id': 'potion_still', 'icon': 'mistech:potion_still', 'name': 'guide.mistech.entry.potion_still', 'deps': ['cuca'],
     'task': {'type': 'item', 'items': ['mistech:potion_still']}, 'x': 8.0, 'y': 4.75,
     'pages': [craft('potion_still'), text('guide.mistech.potion_still.1'), text('guide.mistech.potion_still.2')]},
    {'id': 'iara_pump', 'icon': 'mistech:iara_pump', 'name': 'guide.mistech.entry.iara_pump', 'deps': ['iara'],
     'task': {'type': 'item', 'items': ['mistech:iara_pump']}, 'x': 6.4, 'y': 6.75,
     'pages': [craft('iara_pump'), text('guide.mistech.iara_pump.1'), text('guide.mistech.iara_pump.2')]},
]
SPELLS = [
    {'id': 'scroll_printer', 'icon': 'mistech:scroll_printer', 'name': 'guide.mistech.entry.scroll_printer', 'deps': ['spell_scrolls', 'pure_generator'],
     'task': {'type': 'item', 'items': ['mistech:scroll_printer']}, 'x': 3.2, 'y': 2.0,
     'pages': [craft('scroll_printer'), text('guide.mistech.scroll_printer.1')]},
]
BOOK = A + 'guide/book.json'
book = json.load(open(BOOK, encoding='utf-8'))
for cid, entries in (('folklore', FOLK), ('spells', SPELLS)):
    cat = next(c for c in book['categories'] if c['id'] == cid)
    ids = {e['id'] for e in entries}
    cat['entries'] = [e for e in cat['entries'] if e['id'] not in ids] + entries
dump(BOOK, book, newline=False)
print('ok')
