#!/usr/bin/env python3
"""Textos (pt_br/en_us) e página do Grimório do Conjurador Rúnico e do Cristal de Feitiço.
Idempotente: troca as chaves e a entrada pelo id. Depois rode tools/guide/views.py.
Uso (na pasta do projeto): python3 tools/conjurer/gen_data.py"""
import json

LANG = 'src/main/resources/assets/mistech/lang/'
BOOK = 'src/main/resources/assets/mistech/guide/book.json'

PT = {
    'block.mistech.runic_conjurer': 'Conjurador Rúnico',
    'container.mistech.runic_conjurer': 'Conjurador Rúnico',
    'item.mistech.blank_spell_crystal': 'Cristal de Feitiço Vazio',
    'item.mistech.spell_crystal': 'Cristal de Feitiço',
    'item.mistech.spell_crystal.named': 'Cristal de Feitiço: %s',
    'tooltip.mistech.spell_crystal.blank': 'Grave uma magia na Mesa de Inscrição (modo Cristal)',
    'tooltip.mistech.spell_crystal.power': 'Poder gravado: %s%%',
    'tooltip.mistech.spell_crystal.owner': 'Gravado por %s',
    'tooltip.mistech.spell_crystal.hint': 'Coloque num Conjurador Rúnico',
    'tooltip.mistech.spell_crystal.use.turret': 'No Conjurador: torreta. Atira na criatura hostil mais próxima que o cristal enxerga.',
    'tooltip.mistech.spell_crystal.use.area': 'No Conjurador: defesa em área. Atinge as criaturas hostis em volta do bloco.',
    'tooltip.mistech.spell_crystal.use.farm': 'No Conjurador: fazenda. Faz as plantas crescerem numa área à frente do bloco.',
    'tooltip.mistech.spell_crystal.use.support': 'No Conjurador: suporte. Ajuda os jogadores perto do bloco quando precisam.',
    'tooltip.mistech.spell_crystal.use.logistics': 'No Conjurador: logística. Puxa itens para o baú atrás do bloco, ou criaturas até ele.',
    'tooltip.mistech.spell_crystal.use.utility': 'No Conjurador: utilidade. Veja o Grimório para o que cada uma faz.',
    'gui.mistech.runic_conjurer.status.no_crystal': 'Sem cristal',
    'gui.mistech.runic_conjurer.status.incompatible': 'Magia não serve',
    'gui.mistech.runic_conjurer.status.recharging': 'Recarregando',
    'gui.mistech.runic_conjurer.status.ready': 'Pronto',
    'gui.mistech.runic_conjurer.status.no_target': 'Sem alvo',
    'gui.mistech.runic_conjurer.status.no_energy': 'Sem energia',
    'gui.mistech.runic_conjurer.status.no_essence': 'Sem essência',
    'gui.mistech.runic_conjurer.status.wait_pulse': 'Esperando pulso',
    'gui.mistech.runic_conjurer.status.cast': 'Lançou!',
    'gui.mistech.runic_conjurer.mode.auto': 'Modo: automático',
    'gui.mistech.runic_conjurer.mode.auto.help': 'Lança sempre que a recarga acaba e há o que fazer.',
    'gui.mistech.runic_conjurer.mode.pulse': 'Modo: pulso de redstone',
    'gui.mistech.runic_conjurer.mode.pulse.help': 'Lança uma vez a cada pulso de redstone.',
    'gui.mistech.runic_conjurer.mode.click': 'Clique para trocar',
    'gui.mistech.runic_conjurer.recharge': 'Recarga: %s%%',
    'info.mistech.runic_conjurer.spell': 'Magia: %s',
    'screen.mistech.inscription.mode.scroll': 'Modo: Pergaminho',
    'screen.mistech.inscription.mode.crystal': 'Modo: Cristal',
    'screen.mistech.inscription.engrave': 'Gravar',
    'screen.mistech.inscription.crystal_incompatible': 'Esta magia não funciona no Conjurador Rúnico.',
    'screen.mistech.inscription.crystal_mana': 'Mana: %s',
    'screen.mistech.inscription.crystal_power': 'Poder gravado: %s%%',
    'message.mistech.spell.engraved': 'Cristal gravado: %s',
    'guide.mistech.entry.runic_conjurer': 'Conjurador Rúnico',
    'guide.mistech.runic_conjurer.recipe': 'O §5Conjurador Rúnico§r lança sozinho a magia de um §5Cristal de Feitiço§r, pagando com FE.',
    'guide.mistech.runic_conjurer.crystal_recipe': 'O §5Cristal de Feitiço Vazio§r. A receita faz dois.',
    'guide.mistech.runic_conjurer.1': '§lGravar o cristal§r\nNa §5Mesa de Inscrição§r, troque para o §5Modo: Cristal§r. Escolha uma magia que você conhece e consegue lançar. Gravar gasta 1 cristal vazio e 5× a mana da magia, de uma vez.\n\nO cristal guarda o seu poder §ono momento da gravação§r (75% da sua afinidade). Ele não se gasta. Se a sua afinidade subir, grave outro.',
    'guide.mistech.runic_conjurer.2': '§lO que ele gasta§r\nCada lançamento custa §e40 FE por ponto de mana§r da magia. Magias de tier 3 ou mais também gastam §d25 mB de Essência por tier§r: a Purificada primeiro, a Impura se faltar. Encha os tanques com frascos e baldes, pela tela, pela mão ou por canos.\n\nA recarga é 1,5× a da magia. Melhorias de Velocidade aceleram, de Eficiência cortam o FE e de Alcance somam 2 blocos.',
    'guide.mistech.runic_conjurer.3': '§lModos§r\nNo botão da tela: §eautomático§r (lança sempre que pode e há o que fazer) ou §epulso de redstone§r (um lançamento por pulso). Sem alvo ou sem nada para fazer, ele não gasta nada.\n\nEle nunca atinge jogadores, animais com dono, golens de trabalho nem invocações. O dano sai em nome de quem gravou o cristal: conta loot e XP.',
    'guide.mistech.runic_conjurer.4': '§lMagias que funcionam§r\n§cTorreta:§r Fagulha, Estilhaço de Gelo, Pedregulho, Jato d\'Água, Dreno Sombrio, Toque do Vazio, Descarga, Arco Elétrico.\n§6Área:§r Anel de Brasas, Nova de Gelo, Terremoto, Nevasca, Colheita de Almas.\n§aFazenda:§r Floração, Broto (área à frente do bloco).',
    'guide.mistech.runic_conjurer.5': '§bSuporte:§r Maré Curativa, Pele de Pedra, Luz Sagrada, Purificar.\n§dLogística:§r Telecinese guarda itens soltos no baú atrás do bloco; Puxão traz criaturas até ele.\n§5Utilidade:§r Recarregar (FE do Conjurador para os itens de quem está perto), Impulso Arcano (máquinas num raio de 3), Orbe de Luz (acende cantos escuros), Corrente Ascendente (elevador: fique em cima), Olhar Vigilante (alarme no comparador).',
}

EN = {
    'block.mistech.runic_conjurer': 'Runic Conjurer',
    'container.mistech.runic_conjurer': 'Runic Conjurer',
    'item.mistech.blank_spell_crystal': 'Blank Spell Crystal',
    'item.mistech.spell_crystal': 'Spell Crystal',
    'item.mistech.spell_crystal.named': 'Spell Crystal: %s',
    'tooltip.mistech.spell_crystal.blank': 'Engrave a spell at the Inscription Table (Crystal mode)',
    'tooltip.mistech.spell_crystal.power': 'Engraved power: %s%%',
    'tooltip.mistech.spell_crystal.owner': 'Engraved by %s',
    'tooltip.mistech.spell_crystal.hint': 'Place it in a Runic Conjurer',
    'tooltip.mistech.spell_crystal.use.turret': 'In the Conjurer: turret. Shoots the nearest hostile creature the crystal can see.',
    'tooltip.mistech.spell_crystal.use.area': 'In the Conjurer: area defense. Hits hostile creatures around the block.',
    'tooltip.mistech.spell_crystal.use.farm': 'In the Conjurer: farm. Grows plants in an area in front of the block.',
    'tooltip.mistech.spell_crystal.use.support': 'In the Conjurer: support. Helps players near the block when they need it.',
    'tooltip.mistech.spell_crystal.use.logistics': 'In the Conjurer: logistics. Pulls items into the chest behind it, or creatures towards it.',
    'tooltip.mistech.spell_crystal.use.utility': 'In the Conjurer: utility. See the Grimoire for what each one does.',
    'gui.mistech.runic_conjurer.status.no_crystal': 'No crystal',
    'gui.mistech.runic_conjurer.status.incompatible': 'Spell not usable',
    'gui.mistech.runic_conjurer.status.recharging': 'Recharging',
    'gui.mistech.runic_conjurer.status.ready': 'Ready',
    'gui.mistech.runic_conjurer.status.no_target': 'No target',
    'gui.mistech.runic_conjurer.status.no_energy': 'No energy',
    'gui.mistech.runic_conjurer.status.no_essence': 'No essence',
    'gui.mistech.runic_conjurer.status.wait_pulse': 'Waiting for pulse',
    'gui.mistech.runic_conjurer.status.cast': 'Cast!',
    'gui.mistech.runic_conjurer.mode.auto': 'Mode: automatic',
    'gui.mistech.runic_conjurer.mode.auto.help': 'Casts whenever the cooldown ends and there is something to do.',
    'gui.mistech.runic_conjurer.mode.pulse': 'Mode: redstone pulse',
    'gui.mistech.runic_conjurer.mode.pulse.help': 'Casts once per redstone pulse.',
    'gui.mistech.runic_conjurer.mode.click': 'Click to switch',
    'gui.mistech.runic_conjurer.recharge': 'Cooldown: %s%%',
    'info.mistech.runic_conjurer.spell': 'Spell: %s',
    'screen.mistech.inscription.mode.scroll': 'Mode: Scroll',
    'screen.mistech.inscription.mode.crystal': 'Mode: Crystal',
    'screen.mistech.inscription.engrave': 'Engrave',
    'screen.mistech.inscription.crystal_incompatible': 'This spell does not work in the Runic Conjurer.',
    'screen.mistech.inscription.crystal_mana': 'Mana: %s',
    'screen.mistech.inscription.crystal_power': 'Engraved power: %s%%',
    'message.mistech.spell.engraved': 'Crystal engraved: %s',
    'guide.mistech.entry.runic_conjurer': 'Runic Conjurer',
    'guide.mistech.runic_conjurer.recipe': 'The §5Runic Conjurer§r casts the spell of a §5Spell Crystal§r on its own, paid with FE.',
    'guide.mistech.runic_conjurer.crystal_recipe': 'The §5Blank Spell Crystal§r. The recipe makes two.',
    'guide.mistech.runic_conjurer.1': '§lEngraving the crystal§r\nAt the §5Inscription Table§r, switch to §5Mode: Crystal§r. Pick a spell you know and can cast. Engraving takes 1 blank crystal and 5× the spell\'s mana at once.\n\nThe crystal keeps your power §oat the moment of engraving§r (75% of your affinity). It is never used up. If your affinity grows, engrave a new one.',
    'guide.mistech.runic_conjurer.2': '§lWhat it costs§r\nEach cast costs §e40 FE per point of mana§r of the spell. Tier 3+ spells also take §d25 mB of Essence per tier§r: Purified first, Impure if needed. Fill the tanks with bottles and buckets, through the screen, by hand or with pipes.\n\nThe cooldown is 1.5× the spell\'s. Speed Upgrades make it faster, Efficiency cuts FE and Range adds 2 blocks.',
    'guide.mistech.runic_conjurer.3': '§lModes§r\nUse the button on the screen: §eautomatic§r (casts whenever it can and has something to do) or §eredstone pulse§r (one cast per pulse). With no target or nothing to do, it spends nothing.\n\nIt never hits players, owned animals, work golems or summons. Damage is dealt in the name of whoever engraved the crystal: it counts for loot and XP.',
    'guide.mistech.runic_conjurer.4': '§lSpells that work§r\n§cTurret:§r Spark, Ice Shard, Boulder, Water Bolt, Dark Drain, Void Touch, Discharge, Spark Arc.\n§6Area:§r Ember Ring, Frost Nova, Earthquake, Blizzard, Soul Harvest.\n§aFarm:§r Bloom, Sprout (area in front of the block).',
    'guide.mistech.runic_conjurer.5': '§bSupport:§r Healing Tide, Stone Skin, Holy Light, Purify.\n§dLogistics:§r Telekinesis stores loose items in the chest behind the block; Pull drags creatures to it.\n§5Utility:§r Recharge (the Conjurer\'s FE into nearby players\' items), Arcane Impulse (machines within 3 blocks), Light Orb (lights dark corners), Updraft (elevator: stand on top), Vigilant Gaze (comparator alarm).',
}

for name, d in (('pt_br', PT), ('en_us', EN)):
    path = LANG + name + '.json'
    data = json.load(open(path, encoding='utf-8'))
    data.update(d)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write('\n')

ENTRY = {
    'id': 'runic_conjurer', 'icon': 'mistech:runic_conjurer', 'name': 'guide.mistech.entry.runic_conjurer',
    'deps': ['arcane_charger', 'inscription_table', 'impure_magic_block', 'realm_ores'],
    'task': {'type': 'item', 'items': ['mistech:runic_conjurer']}, 'x': 8.0, 'y': 0.5,
    'pages': [
        {'type': 'crafting', 'grid': ['mistech:mithril_ingot', 'minecraft:amethyst_cluster', 'mistech:mithril_ingot',
                                      'mistech:energy_rune', 'mistech:arcane_charger', 'mistech:energy_rune',
                                      'mistech:mithril_ingot', 'mistech:impure_magic_block', 'mistech:mithril_ingot'],
         'result': 'mistech:runic_conjurer', 'text': 'guide.mistech.runic_conjurer.recipe'},
        {'type': 'crafting', 'grid': ['minecraft:amethyst_shard', 'minecraft:amethyst_shard', 'minecraft:amethyst_shard',
                                      'minecraft:amethyst_shard', 'mistech:impure_magic_shard', 'minecraft:quartz', '', '', ''],
         'result': 'mistech:blank_spell_crystal', 'count': 2, 'shapeless': True, 'text': 'guide.mistech.runic_conjurer.crystal_recipe'},
        {'type': 'text', 'text': 'guide.mistech.runic_conjurer.1'},
        {'type': 'text', 'text': 'guide.mistech.runic_conjurer.2'},
        {'type': 'text', 'text': 'guide.mistech.runic_conjurer.3'},
        {'type': 'text', 'text': 'guide.mistech.runic_conjurer.4'},
        {'type': 'text', 'text': 'guide.mistech.runic_conjurer.5'},
    ],
}

book = json.load(open(BOOK, encoding='utf-8'))
cat = next(c for c in book['categories'] if c['id'] == 'spells')
cat['entries'] = [e for e in cat['entries'] if e['id'] != ENTRY['id']] + [ENTRY]
with open(BOOK, 'w', encoding='utf-8') as f:
    json.dump(book, f, ensure_ascii=False, indent=2)
print('ok: %d chaves, entrada %s' % (len(PT), ENTRY['id']))
