#!/usr/bin/env python3
"""Dados das pendências pós-Fase 9: SilentGear Celeste, tags, Bomba do Boitatá, Poção de Mana, receitas, textos e Grimório.
Idempotente. Depois rode tools/guide/views.py.
Uso (na pasta do projeto): python3 tools/pendencias/gen_data.py"""
import json, os

R = 'src/main/resources/'
A = R + 'assets/mistech/'
D = R + 'data/'
LANG = A + 'lang/'


def dump(path, obj, newline=True):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        if newline:
            f.write('\n')


def load(path):
    return json.load(open(path, encoding='utf-8'))


def tag(path, values):
    p = D + path
    j = load(p) if os.path.exists(p) else {'replace': False, 'values': []}
    for v in values:
        if v not in j['values']:
            j['values'].append(v)
    dump(p, j)


# ---------------------------------------------------------------- SilentGear: material Celeste
tag('c/tags/item/ingots/celestial.json', ['mistech:celestial_ingot'])
tag('c/tags/item/ingots.json', ['#c:ingots/celestial'])
tag('c/tags/item/storage_blocks/celestial.json', ['mistech:celestial_block'])
tag('c/tags/item/storage_blocks.json', ['#c:storage_blocks/celestial'])
tag('c/tags/block/storage_blocks/celestial.json', ['mistech:celestial_block'])
tag('c/tags/block/storage_blocks.json', ['#c:storage_blocks/celestial'])
mat = load(D + 'mistech/silentgear_materials/orichalcum.json')
mat['crafting']['ingredient'] = '#c:ingots/celestial'
mat['display']['color'] = '#FF9FD8FF'
mat['display']['name'] = {'translate': 'material.mistech.celestial'}
main = mat['properties']['silentgear:main']
main.update({'armor': 30.0, 'armor/boots': 5, 'armor/chestplate': 11, 'armor/helmet': 5, 'armor/leggings': 9, 'armor_durability': 64.0,
             'armor_toughness': 26.0, 'attack_damage': 9.0, 'durability': 3600.0, 'enchantment_value': 30.0, 'harvest_speed': 16.0,
             'knockback_resistance': 0.2, 'magic_armor': 24.0, 'magic_damage': 14.0, 'projectile_accuracy': 1.3,
             'projectile_speed': 1.25, 'ranged_damage': 5.0, 'rarity': 140.0,
             'traits': [{'conditions': [], 'level': 4, 'trait': 'silentgear:stellar'},
                        {'conditions': [], 'level': 3, 'trait': 'silentgear:lustrous'},
                        {'conditions': [], 'level': 2, 'trait': 'silentgear:holy'}]})
mat['properties']['silentgear:rod']['rarity'] = 140.0
mat['properties']['silentgear:rod']['traits'] = [{'conditions': [], 'level': 2, 'trait': 'silentgear:stellar'},
                                                 {'conditions': [], 'level': 2, 'trait': 'silentgear:lustrous'}]
dump(D + 'mistech/silentgear_materials/celestial.json', mat, newline=False)

# ---------------------------------------------------------------- receitas, loot, tags
def shaped(name, pattern, key, count=1):
    dump(D + 'mistech/recipe/%s.json' % name, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
                                               'result': {'id': 'mistech:' + name, 'count': count}})
    return [key.get(ch, '') if ch != ' ' else '' for row in pattern for ch in row]


def loot(name):
    dump(D + 'mistech/loot_table/blocks/%s.json' % name, {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'mistech:' + name}],
                                              'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': 'mistech:blocks/' + name})


PUMP_GRID = shaped('boitata_pump', ['NBN', 'FTF', 'KKK'], {'N': 'minecraft:netherite_scrap', 'B': 'minecraft:bucket', 'F': 'mistech:fire_rune',
                                                           'T': 'mistech:runic_tank', 'K': 'minecraft:polished_blackstone_bricks'})
CONDUIT_GRID = shaped('runic_conduit', ['CRC', 'GGG', 'CRC'], {'C': 'minecraft:copper_ingot', 'R': 'mistech:arcane_rune', 'G': 'minecraft:glass_pane'}, 8)
# níveis: 8 condutos do nível de baixo em volta de 1 lingote = 8 do nível de cima
CONDUIT_TIERS = [('mithril_conduit', 'runic_conduit', 'mistech:mithril_ingot'),
                 ('adamantine_conduit', 'mithril_conduit', 'mistech:adamantine_ingot'),
                 ('orichalcum_conduit', 'adamantine_conduit', 'mistech:orichalcum_ingot')]
TIER_GRIDS = {}
for name, lower, ingot in CONDUIT_TIERS:
    TIER_GRIDS[name] = shaped(name, ['CCC', 'CIC', 'CCC'], {'C': 'mistech:' + lower, 'I': ingot}, 8)
CONDUITS = ['runic_conduit'] + [t[0] for t in CONDUIT_TIERS]
for n in ['boitata_pump'] + CONDUITS:
    loot(n)
tag('minecraft/tags/block/mineable/pickaxe.json', ['mistech:boitata_pump'] + ['mistech:' + n for n in CONDUITS])
dump(A + 'models/item/mana_potion.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mistech:item/mana_potion'}})
dump(A + 'items/mana_potion.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:item/mana_potion'}})
# limpar o Cristal de Feitiço também na mesa
dump(D + 'mistech/recipe/spell_crystal_clear.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                                     'ingredients': ['mistech:spell_crystal', 'minecraft:water_bucket'],
                                                     'result': {'id': 'mistech:blank_spell_crystal', 'count': 1}})

# ---------------------------------------------------------------- textos
PT = {
    'material.mistech.celestial': 'Celeste',
    'block.mistech.boitata_pump': 'Bomba do Boitatá', 'container.mistech.boitata_pump': 'Bomba do Boitatá',
    'gui.mistech.boitata_pump.status.pumping': 'Bombeando lava', 'gui.mistech.boitata_pump.status.river': 'Lava sem fim',
    'gui.mistech.boitata_pump.status.no_scale': 'Sem Escama do Boitatá', 'gui.mistech.boitata_pump.status.no_energy': 'Sem energia',
    'gui.mistech.boitata_pump.status.dry': 'Nada para puxar', 'gui.mistech.boitata_pump.status.full': 'Tanque cheio',
    'gui.mistech.boitata_pump.status.infinite': 'Mar de lava (sem fim)',
    'gui.mistech.iara_pump.status.infinite': 'Fonte infinita (sem fim)',
    'block.mistech.runic_conduit': 'Conduto Rúnico', 'block.mistech.mithril_conduit': 'Conduto de Mitril',
    'block.mistech.adamantine_conduit': 'Conduto de Adamantina', 'block.mistech.orichalcum_conduit': 'Conduto de Oricalco',
    'message.mistech.conduit.state': 'Leva: %s · %s itens a cada %s s · %s mB · %s FE/t',
    'conduit.mistech.carry.energy': 'energia', 'conduit.mistech.carry.items': 'itens', 'conduit.mistech.carry.fluids': 'fluidos',
    'conduit.mistech.carry.energy_fluids': 'energia e fluidos', 'conduit.mistech.carry.energy_items': 'energia e itens',
    'conduit.mistech.carry.fluids_items': 'fluidos e itens', 'conduit.mistech.carry.all': 'tudo',
    'guide.mistech.runic_conduit.3': '§lNíveis§r (8 do nível de baixo + 1 lingote = 8)\n§6Rúnico§r 8 itens/s, 500 mB/s, 2.000 FE/t\n§bMitril§r 16 itens a cada 0,5 s, 1.000 mB, 8.000 FE/t\n§cAdamantina§r 32 itens a cada 0,25 s, 4.000 mB, 32.000 FE/t\n§eOricalco§r 64 itens a cada 0,1 s, 16.000 mB, 128.000 FE/t\nNuma rede misturada vale o mais lento.',
    'guide.mistech.runic_conduit.4': '§lO que leva§r\nAgachado + clique no §lmiolo§r troca: energia → itens → fluidos → energia e fluidos → energia e itens → fluidos e itens → tudo. Só passa por um conduto o que ele leva.\n\nCor da runa do miolo: §eamarela§r energia, §averde§r itens, §9azul§r fluidos, §5roxa§r energia+fluidos, §6laranja§r energia+itens, §bciano§r fluidos+itens, §fbranca§r tudo. Clique sem agachar mostra o estado.',
    'message.mistech.runic_conduit.link': 'Ponta %s: entrega', 'message.mistech.runic_conduit.pull': 'Ponta %s: puxa',
    'message.mistech.runic_conduit.off': 'Ponta %s: desligada', 'message.mistech.runic_conduit.conduit': 'Ponta %s: ligada ao conduto',
    'message.mistech.runic_conduit.none': 'Ponta %s', 'direction.mistech.north': 'norte', 'direction.mistech.south': 'sul',
    'direction.mistech.east': 'leste', 'direction.mistech.west': 'oeste', 'direction.mistech.up': 'cima', 'direction.mistech.down': 'baixo',
    'item.mistech.mana_potion': 'Poção de Mana', 'item.mistech.mana_potion.tooltip': 'Devolve %s de mana (o que sobrar vai para o cajado)',
    'gui.mistech.mana_well.liquid': 'Mana líquida',
    'message.mistech.mana_well.no_liquid': 'Mana líquida: %s/%s mB',
    'message.mistech.mana_siphon.full': 'A Sangria está cheia',
    'tooltip.mistech.spell_crystal.clear': 'Agachado + botão direito: apaga a magia',
    'message.mistech.spell_crystal.cleared': 'O cristal esqueceu a magia',
    'message.mistech.pattern.crafting_n': 'Padrão: %s (receita %s de %s: %s)',
    'message.mistech.pattern.priority': 'Prioridade: %s',
    'item.mistech.pattern_scroll.priority': 'Prioridade: %s',
    'item.mistech.pattern_scroll.priority.high': 'alta', 'item.mistech.pattern_scroll.priority.normal': 'normal',
    'item.mistech.pattern_scroll.priority.low': 'baixa',
    # dicas dos espaços
    'hint.mistech.saci_windmill.tobacco': 'Folha de Tabaco Seca', 'hint.mistech.saci_windmill.tobacco.more': 'Deixa o Saci contente (+25%)',
    'hint.mistech.saci_windmill.saci': 'Saci Engarrafado', 'hint.mistech.saci_windmill.saci.more': 'Não é gasto',
    'hint.mistech.pump.bucket': 'Balde vazio', 'hint.mistech.pump.bucket.more': 'Sai cheio no espaço do meio',
    'hint.mistech.iara_pump.scale': 'Escama da Iara', 'hint.mistech.iara_pump.scale.more': 'Não é gasta',
    'hint.mistech.boitata_pump.scale': 'Escama do Boitatá', 'hint.mistech.boitata_pump.scale.more': 'Não é gasta',
    'hint.mistech.curupira_stump.offering': 'Cachaça', 'hint.mistech.curupira_stump.offering.more': 'Qualquer uma: paga 20 árvores',
    'hint.mistech.curupira_stump.sapling': 'Mudas', 'hint.mistech.curupira_stump.sapling.more': 'Plantadas na área à frente',
    'hint.mistech.potion_still.bottles': 'Frascos vazios', 'hint.mistech.potion_still.bottles.more': 'Até 8 por lote',
    'hint.mistech.potion_still.first': '1º ingrediente', 'hint.mistech.potion_still.first.more': 'Ex.: Fungo do Nether (ou 2–3 poções = Cuca)',
    'hint.mistech.potion_still.second': '2º ingrediente', 'hint.mistech.potion_still.second.more': 'Ex.: Açúcar (opcional)',
    'hint.mistech.potion_still.third': '3º ingrediente', 'hint.mistech.potion_still.third.more': 'Ex.: Redstone (opcional)',
    'hint.mistech.scroll_printer.template': 'Pergaminho Matriz', 'hint.mistech.scroll_printer.template.more': 'Não é gasto',
    'hint.mistech.scroll_printer.paper': 'Papel', 'hint.mistech.scroll_printer.paper.more': '1 por cópia',
    'hint.mistech.scroll_printer.lapis': 'Lápis-lazúli', 'hint.mistech.scroll_printer.lapis.more': '2 por cópia',
    'hint.mistech.star_forge.metal': 'Metal', 'hint.mistech.star_forge.metal.more': 'Lingote, bloco ou balde de mitril, adamantina ou oricalco',
    'hint.mistech.star_forge.fragment': 'Fragmento de Estrela', 'hint.mistech.star_forge.fragment.more': '1 por lingote',
    'hint.mistech.dream_reactor.fuel': 'Sonhos', 'hint.mistech.dream_reactor.fuel.more': 'Fragmento de Sonho, Pó de Sonho ou Estilhaço de Memória',
    'hint.mistech.conjurer.crystal_a': 'Cristal A', 'hint.mistech.conjurer.crystal_a.more': 'Cristal de Feitiço gravado',
    'hint.mistech.conjurer.crystal_b': 'Cristal B', 'hint.mistech.conjurer.crystal_b.more': 'Alterna com o A',
    'hint.mistech.conjurer.essence': 'Essência', 'hint.mistech.conjurer.essence.more': 'Frasco ou balde (Purificada ou Impura)',
    'hint.mistech.mana_well.essence': 'Essência Purificada', 'hint.mistech.mana_well.essence.more': 'Frasco ou balde',
    'hint.mistech.mana_well.rune': 'Runa de afinidade', 'hint.mistech.mana_well.rune.more': '+25% para quem tem nível 5+ no elemento',
    # Grimório
    'guide.mistech.entry.boitata_pump': 'Bomba do Boitatá', 'guide.mistech.entry.runic_conduit': 'Conduto Rúnico',
    'guide.mistech.boitata_pump.recipe': 'A §6Bomba do Boitatá§r é a Bomba da Iara para lava.',
    'guide.mistech.boitata_pump.1': 'Com uma §6Escama do Boitatá§r no espaço (não é gasta), puxa fontes de §6lava§r num raio de 16 e até 16 para baixo, como a Bomba da Iara.\n\nNo §cNether§r, perto de um mar de lava (16+ fontes a até 4 blocos, para baixo), dá §elava sem fim§r: 500 mB/s, sem secar nada.',
    'guide.mistech.runic_conduit.recipe': 'O §6Conduto Rúnico§r leva itens, fluidos e energia num cano só.',
    'guide.mistech.runic_conduit.1': 'Liga sozinho em outros condutos e em blocos com inventário, tanque ou energia. Cada ponta num bloco é de §aentrega§r (padrão).\n\n§7Agachado + clique com a mão vazia na ponta:§r entrega → §6puxa§r → desligada (tampão com X; clique nele ou no miolo daquele lado para religar). A ponta que §6puxa§r (aro de ouro) tira do bloco e manda para as de entrega da rede, em rodízio.',
    'guide.mistech.runic_conduit.2': 'Cada ponta que puxa move itens e fluidos de tempos em tempos e energia todo tick; quanto, depende do nível (próxima página).\n\nDesligar uma ponta entre dois condutos separa as redes. Máquinas do Mistech: puxar tira só dos espaços de saída.',
}
PT['guide.mistech.iara_pump.1'] = ('Com uma §3Escama da Iara§r no espaço (não é gasta), puxa blocos de fonte de qualquer fluido num raio de 16 e até 16 '
                                   'para baixo: 1 bloco a cada 0,5 s, 50 FE cada, para um tanque de 16.000 mB.\n\nNum §9rio ou oceano§r, dá água sem fim: '
                                   '1.000 mB/s. Perto de uma §9fonte infinita§r (poça 3×1 ou 2×2), 500 mB/s. Sem secar nada.')
PT['guide.mistech.mana_well.3'] = ('§lMana líquida§r\nCom essência e energia, o poço vai convertendo devagar (20 mB/s) em §bmana líquida§r '
                                   '(até 4.000 mB, 1 mB = 1 ponto). A aura e o clique usam ela primeiro.\n\n§bGarrafa de vidro§r (ou Pote de Vidro) na mão: tira 100 mB numa '
                                   '§bPoção de Mana§r. Beber devolve 100 de mana (o que sobrar vai para o cajado) e a garrafa.')
PT['guide.mistech.mana_siphon.3'] = '§lPoção de Mana§r\nClique na Sangria com uma Poção de Mana: os 100 de mana viram FE na hora (1.500 FE) e a garrafa volta.'
PT['guide.mistech.archive_autocraft.4'] = ('§lVárias receitas (v2)§r\nUm item pode ter vários padrões. O planejador tenta o de §eprioridade§r maior '
                                           'primeiro; se faltar ingrediente, tenta o próximo. Clique com o padrão pronto (mão secundária vazia) troca a '
                                           'prioridade. Com o mesmo item na outra mão, troca a receita da mesa.\n\nPadrões de §bmáquina§r entram na mesma '
                                           'árvore: lingote → Triturador → pó → ...')
PT['guide.mistech.runic_conjurer.6'] = '§lApagar o cristal§r\nAgachado + botão direito com o cristal gravado (ou cristal + balde de água na mesa): volta a Cristal vazio.'

EN = {
    'material.mistech.celestial': 'Celestial',
    'block.mistech.boitata_pump': 'Boitatá Pump', 'container.mistech.boitata_pump': 'Boitatá Pump',
    'gui.mistech.boitata_pump.status.pumping': 'Pumping lava', 'gui.mistech.boitata_pump.status.river': 'Endless lava',
    'gui.mistech.boitata_pump.status.no_scale': 'No Boitatá Scale', 'gui.mistech.boitata_pump.status.no_energy': 'No energy',
    'gui.mistech.boitata_pump.status.dry': 'Nothing to pull', 'gui.mistech.boitata_pump.status.full': 'Tank full',
    'gui.mistech.boitata_pump.status.infinite': 'Lava sea (endless)',
    'gui.mistech.iara_pump.status.infinite': 'Infinite source (endless)',
    'block.mistech.runic_conduit': 'Runic Conduit', 'block.mistech.mithril_conduit': 'Mithril Conduit',
    'block.mistech.adamantine_conduit': 'Adamantine Conduit', 'block.mistech.orichalcum_conduit': 'Orichalcum Conduit',
    'message.mistech.conduit.state': 'Carries: %s · %s items every %s s · %s mB · %s FE/t',
    'conduit.mistech.carry.energy': 'energy', 'conduit.mistech.carry.items': 'items', 'conduit.mistech.carry.fluids': 'fluids',
    'conduit.mistech.carry.energy_fluids': 'energy and fluids', 'conduit.mistech.carry.energy_items': 'energy and items',
    'conduit.mistech.carry.fluids_items': 'fluids and items', 'conduit.mistech.carry.all': 'everything',
    'guide.mistech.runic_conduit.3': '§lTiers§r (8 of the tier below + 1 ingot = 8)\n§6Runic§r 8 items/s, 500 mB/s, 2,000 FE/t\n§bMithril§r 16 items every 0.5 s, 1,000 mB, 8,000 FE/t\n§cAdamantine§r 32 items every 0.25 s, 4,000 mB, 32,000 FE/t\n§eOrichalcum§r 64 items every 0.1 s, 16,000 mB, 128,000 FE/t\nIn a mixed network the slowest one counts.',
    'guide.mistech.runic_conduit.4': '§lWhat it carries§r\nSneak + click the §lcore§r to cycle: energy → items → fluids → energy and fluids → energy and items → fluids and items → everything. Only what a conduit carries goes through it.\n\nCore rune color: §eyellow§r energy, §agreen§r items, §9blue§r fluids, §5purple§r energy+fluids, §6orange§r energy+items, §bcyan§r fluids+items, §fwhite§r everything. Click without sneaking to see its state.',
    'message.mistech.runic_conduit.link': '%s end: deliver', 'message.mistech.runic_conduit.pull': '%s end: pull',
    'message.mistech.runic_conduit.off': '%s end: off', 'message.mistech.runic_conduit.conduit': '%s end: linked to conduit',
    'message.mistech.runic_conduit.none': '%s end', 'direction.mistech.north': 'North', 'direction.mistech.south': 'South',
    'direction.mistech.east': 'East', 'direction.mistech.west': 'West', 'direction.mistech.up': 'Top', 'direction.mistech.down': 'Bottom',
    'item.mistech.mana_potion': 'Mana Potion', 'item.mistech.mana_potion.tooltip': 'Restores %s mana (the rest goes to your staff)',
    'gui.mistech.mana_well.liquid': 'Liquid mana',
    'message.mistech.mana_well.no_liquid': 'Liquid mana: %s/%s mB',
    'message.mistech.mana_siphon.full': 'The Siphon is full',
    'tooltip.mistech.spell_crystal.clear': 'Sneak + right-click: erase the spell',
    'message.mistech.spell_crystal.cleared': 'The crystal forgot its spell',
    'message.mistech.pattern.crafting_n': 'Pattern: %s (recipe %s of %s: %s)',
    'message.mistech.pattern.priority': 'Priority: %s',
    'item.mistech.pattern_scroll.priority': 'Priority: %s',
    'item.mistech.pattern_scroll.priority.high': 'high', 'item.mistech.pattern_scroll.priority.normal': 'normal',
    'item.mistech.pattern_scroll.priority.low': 'low',
    'hint.mistech.saci_windmill.tobacco': 'Dried Tobacco Leaf', 'hint.mistech.saci_windmill.tobacco.more': 'Keeps the Saci happy (+25%)',
    'hint.mistech.saci_windmill.saci': 'Bottled Saci', 'hint.mistech.saci_windmill.saci.more': 'Not used up',
    'hint.mistech.pump.bucket': 'Empty bucket', 'hint.mistech.pump.bucket.more': 'Comes out full in the middle slot',
    'hint.mistech.iara_pump.scale': 'Iara Scale', 'hint.mistech.iara_pump.scale.more': 'Not used up',
    'hint.mistech.boitata_pump.scale': 'Boitatá Scale', 'hint.mistech.boitata_pump.scale.more': 'Not used up',
    'hint.mistech.curupira_stump.offering': 'Cachaça', 'hint.mistech.curupira_stump.offering.more': 'Any kind: pays for 20 trees',
    'hint.mistech.curupira_stump.sapling': 'Saplings', 'hint.mistech.curupira_stump.sapling.more': 'Planted in the area in front',
    'hint.mistech.potion_still.bottles': 'Empty bottles', 'hint.mistech.potion_still.bottles.more': 'Up to 8 per batch',
    'hint.mistech.potion_still.first': '1st ingredient', 'hint.mistech.potion_still.first.more': 'E.g. Nether Wart (or 2–3 potions = Cuca)',
    'hint.mistech.potion_still.second': '2nd ingredient', 'hint.mistech.potion_still.second.more': 'E.g. Sugar (optional)',
    'hint.mistech.potion_still.third': '3rd ingredient', 'hint.mistech.potion_still.third.more': 'E.g. Redstone (optional)',
    'hint.mistech.scroll_printer.template': 'Template Scroll', 'hint.mistech.scroll_printer.template.more': 'Not used up',
    'hint.mistech.scroll_printer.paper': 'Paper', 'hint.mistech.scroll_printer.paper.more': '1 per copy',
    'hint.mistech.scroll_printer.lapis': 'Lapis Lazuli', 'hint.mistech.scroll_printer.lapis.more': '2 per copy',
    'hint.mistech.star_forge.metal': 'Metal', 'hint.mistech.star_forge.metal.more': 'Mithril, adamantine or orichalcum ingot, block or bucket',
    'hint.mistech.star_forge.fragment': 'Star Fragment', 'hint.mistech.star_forge.fragment.more': '1 per ingot',
    'hint.mistech.dream_reactor.fuel': 'Dreams', 'hint.mistech.dream_reactor.fuel.more': 'Dream Fragment, Dream Dust or Memory Shard',
    'hint.mistech.conjurer.crystal_a': 'Crystal A', 'hint.mistech.conjurer.crystal_a.more': 'Engraved Spell Crystal',
    'hint.mistech.conjurer.crystal_b': 'Crystal B', 'hint.mistech.conjurer.crystal_b.more': 'Takes turns with A',
    'hint.mistech.conjurer.essence': 'Essence', 'hint.mistech.conjurer.essence.more': 'Bottle or bucket (Purified or Impure)',
    'hint.mistech.mana_well.essence': 'Purified Essence', 'hint.mistech.mana_well.essence.more': 'Bottle or bucket',
    'hint.mistech.mana_well.rune': 'Affinity rune', 'hint.mistech.mana_well.rune.more': '+25% for level 5+ in that element',
    'guide.mistech.entry.boitata_pump': 'Boitatá Pump', 'guide.mistech.entry.runic_conduit': 'Runic Conduit',
    'guide.mistech.boitata_pump.recipe': 'The §6Boitatá Pump§r is the Iara Pump for lava.',
    'guide.mistech.boitata_pump.1': 'With a §6Boitatá Scale§r in its slot (not used up), it pulls §6lava§r sources within 16 blocks and down to 16 below, like the Iara Pump.\n\nIn the §cNether§r, next to a lava sea (16+ sources within 4 blocks, below), it gives §eendless lava§r: 500 mB/s, draining nothing.',
    'guide.mistech.runic_conduit.recipe': 'The §6Runic Conduit§r carries items, fluids and energy in a single pipe.',
    'guide.mistech.runic_conduit.1': 'It links by itself to other conduits and to blocks with an inventory, tank or energy. Every end on a block §adelivers§r (default).\n\n§7Sneak + empty-hand click on an end:§r deliver → §6pull§r → off (a plug with an X; click it or the core on that side to turn it back on). A §6pulling§r end (gold ring) takes from the block and sends to the network\'s delivering ends, in turn.',
    'guide.mistech.runic_conduit.2': 'Each pulling end moves items and fluids every so often and energy every tick; how much depends on the tier (next page).\n\nTurning off an end between two conduits splits the networks. On Mistech machines, pulling only takes from output slots.',
}
EN['guide.mistech.iara_pump.1'] = ('With an §3Iara Scale§r in its slot (not used up), it pulls source blocks of any fluid within 16 blocks and down to 16 '
                                   'below: 1 block every 0.5 s, 50 FE each, into a 16,000 mB tank.\n\nIn a §9river or ocean§r it gives endless water: '
                                   '1,000 mB/s. Next to an §9infinite source§r (3×1 or 2×2 pool), 500 mB/s. Draining nothing.')
EN['guide.mistech.mana_well.3'] = ('§lLiquid mana§r\nWith essence and energy, the well slowly turns them (20 mB/s) into §bliquid mana§r '
                                   '(up to 4,000 mB, 1 mB = 1 point). The aura and the click use it first.\n\nA §bGlass Bottle§r (or Glass Jar) in hand takes 100 mB as a '
                                   '§bMana Potion§r. Drinking it restores 100 mana (the rest goes to your staff) and gives the bottle back.')
EN['guide.mistech.mana_siphon.3'] = '§lMana Potion§r\nClick the Siphon with a Mana Potion: its 100 mana become FE at once (1,500 FE) and the bottle comes back.'
EN['guide.mistech.archive_autocraft.4'] = ('§lSeveral recipes (v2)§r\nAn item can have several patterns. The planner tries the highest §epriority§r '
                                           'first; if an ingredient is missing, it tries the next. Click a finished pattern (empty off hand) to change its '
                                           'priority. With the same item in the off hand, it cycles the crafting recipe.\n\n§bMachine§r patterns join the '
                                           'same tree: ingot → Crusher → dust → ...')
EN['guide.mistech.runic_conjurer.6'] = '§lErasing a crystal§r\nSneak + right-click with an engraved crystal (or crystal + water bucket in a crafting grid): it becomes a blank Crystal again.'
for name, d in (('pt_br', PT), ('en_us', EN)):
    p = LANG + name + '.json'
    data = load(p)
    data.update(d)
    dump(p, data)


# ---------------------------------------------------------------- Grimório
def text(key):
    return {'type': 'text', 'text': key}


BOOK = A + 'guide/book.json'
book = load(BOOK)


def entry(cid, eid):
    cat = next(c for c in book['categories'] if c['id'] == cid)
    return cat, next((e for e in cat['entries'] if e['id'] == eid), None)


def add_page(cid, eid, page):
    _, e = entry(cid, eid)
    if e and not any(p.get('text') == page.get('text') for p in e['pages']):
        e['pages'].append(page)


add_page('spells', 'mana_well', text('guide.mistech.mana_well.3'))
add_page('spells', 'mana_siphon', text('guide.mistech.mana_siphon.3'))
add_page('automation', 'archive_autocraft', text('guide.mistech.archive_autocraft.4'))
add_page('spells', 'runic_conjurer', text('guide.mistech.runic_conjurer.6'))
NEW = {
    'folklore': [{'id': 'boitata_pump', 'icon': 'mistech:boitata_pump', 'name': 'guide.mistech.entry.boitata_pump', 'deps': ['iara_pump', 'boitata'],
                  'task': {'type': 'item', 'items': ['mistech:boitata_pump']}, 'x': 8.0, 'y': 6.75,
                  'pages': [{'type': 'crafting', 'grid': PUMP_GRID, 'result': 'mistech:boitata_pump', 'text': 'guide.mistech.boitata_pump.recipe'},
                            text('guide.mistech.boitata_pump.1')]}],
    'automation': [{'id': 'runic_conduit', 'icon': 'mistech:runic_conduit', 'name': 'guide.mistech.entry.runic_conduit', 'deps': ['runic_tank'],
                    'task': {'type': 'item', 'items': ['mistech:runic_conduit']}, 'x': 4.8, 'y': 1.0,
                    'pages': [{'type': 'crafting', 'grid': CONDUIT_GRID, 'result': 'mistech:runic_conduit',
                               'text': 'guide.mistech.runic_conduit.recipe'}, text('guide.mistech.runic_conduit.1'),
                              text('guide.mistech.runic_conduit.2'), text('guide.mistech.runic_conduit.3'),
                              {'type': 'crafting', 'grid': TIER_GRIDS['mithril_conduit'], 'result': 'mistech:mithril_conduit'},
                              text('guide.mistech.runic_conduit.4')]}],
}
for cid, entries in NEW.items():
    cat = next(c for c in book['categories'] if c['id'] == cid)
    ids = {e['id'] for e in entries}
    cat['entries'] = [e for e in cat['entries'] if e['id'] not in ids] + entries
dump(BOOK, book, newline=False)
print('ok')
