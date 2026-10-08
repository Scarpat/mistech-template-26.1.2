# Grimório e textos dos golems de trabalho. Idempotente.
# Uso: python3 tools/golem/guide_entries.py && python3 tools/guide/journey.py && python3 tools/guide/views.py
import json
BOOK = 'src/main/resources/assets/mistech/guide/book.json'
LANG = 'src/main/resources/assets/mistech/lang/'

RECIPES = {
    'sorter': ('minecraft:terracotta', 'minecraft:brick', 'minecraft:amethyst_shard'),
    'collector': ('minecraft:amethyst_block', 'minecraft:amethyst_shard', 'mistech:impure_magic_shard'),
    'messenger': ('mistech:mithril_block', 'mistech:mithril_ingot', 'minecraft:amethyst_shard'),
    'crafter': ('minecraft:polished_blackstone', 'minecraft:polished_blackstone_bricks', 'minecraft:crafting_table'),
    'keeper': ('mistech:orichalcum_block', 'mistech:orichalcum_ingot', 'minecraft:ender_eye'),
}


def craft(kind):
    b, c, a = RECIPES[kind]
    return {'type': 'crafting', 'grid': ['', b, '', c, a, c, c, '', c], 'result': 'mistech:' + kind + '_statue'}


E = [('golem_' + k, 'mistech:' + k + '_statue', [craft(k), {'type': 'text', 'text': 'guide.mistech.golem_' + k + '.1'}]) for k in RECIPES]
E.append(('golem_runes', 'mistech:time_rune', [{'type': 'text', 'text': 'guide.mistech.golem_runes.1'}]))

b = json.load(open(BOOK, encoding='utf-8'))
cat = next(c for c in b['categories'] if c['id'] == 'golems')
have = {e['id']: e for c in b['categories'] for e in c['entries']}
for eid, icon, pages in E:
    if eid in have:
        have[eid]['pages'] = pages
    else:
        cat['entries'].append({'id': eid, 'icon': icon, 'name': 'guide.mistech.entry.' + eid, 'deps': [], 'task': {'type': 'read'},
                               'x': 0, 'y': 0, 'pages': pages})
json.dump(b, open(BOOK, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

PT = {
    'entity.mistech.work_golem': 'Golem de Trabalho',
    'entity.mistech.golem.sorter': 'Golem de Terracota', 'entity.mistech.golem.collector': 'Golem de Ametista',
    'entity.mistech.golem.messenger': 'Golem de Mitril', 'entity.mistech.golem.crafter': 'Golem de Pedra-negra',
    'entity.mistech.golem.keeper': 'Golem de Oricalco',
    'block.mistech.sorter_statue': 'Estátua de Terracota', 'block.mistech.collector_statue': 'Estátua de Ametista',
    'block.mistech.messenger_statue': 'Estátua de Mitril', 'block.mistech.crafter_statue': 'Estátua de Pedra-negra',
    'block.mistech.keeper_statue': 'Estátua de Oricalco',
    'message.mistech.golem.describe': '%s · %s · %s · runas: %s',
    'message.mistech.golem.rune_full': 'Este golem já tem essa runa ou %s runas',
    'message.mistech.golem.rune_added': 'Runa aprendida: %s',
    'message.mistech.golem.no_recipe': 'Não achei receita de mesa de trabalho para %s',
    'message.mistech.golem.recipe': 'Receita aprendida: %s',
    'message.mistech.golem.rules_full': 'O golem já guarda %s regras',
    'message.mistech.golem.rule_added': 'Manter %s × %s', 'message.mistech.golem.rule_removed': 'Regra removida: %s',
    'guide.mistech.entry.golem_sorter': 'Golem de Terracota',
    'guide.mistech.golem_sorter.1': 'O §6Selecionador§r. Com a Vara: §aorigem§r = o baú de entrada, §6destino§r = o centro da área arrumada.\n\nCada item vai para o baú perto do destino (8 blocos) que §ljá tem aquele item§r; o que não tem lugar vai para o próprio destino. Carrega 32.',
    'guide.mistech.entry.golem_collector': 'Golem de Ametista',
    'guide.mistech.golem_collector.1': 'O §dColetor§r. Recolhe os itens do chão a até 8 blocos do §6destino§r (16 com Runa do Espaço) e guarda nele. Botão direito com item: só aquele item. Carrega 32.',
    'guide.mistech.entry.golem_messenger': 'Golem de Mitril',
    'guide.mistech.golem_messenger.1': 'O §bMensageiro§r. Igual ao Golem de Barro, mas carrega §l64§r e anda bem mais rápido.\n\nCom a §5Runa do Espaço§r ele se §lteleporta§r entre a origem e o destino.',
    'guide.mistech.entry.golem_crafter': 'Golem de Pedra-negra',
    'guide.mistech.golem_crafter.1': 'O §8Artesão§r. Mostre a ele o item pronto (botão direito com o item): ele aprende a receita da mesa de trabalho.\n\nPega os ingredientes da §aorigem§r, faz o item e guarda no §6destino§r. Sobras (balde vazio...) voltam para a origem.',
    'guide.mistech.entry.golem_keeper': 'Golem de Oricalco',
    'guide.mistech.golem_keeper.1': 'O §6Zelador§r do §5Arquivo Espacial§r. O §6destino§r precisa estar ao alcance de um Núcleo.\n\nBotão direito com uma pilha: "manter essa quantidade" no destino (64 tochas na mão = manter 64). Ele busca no Arquivo o que faltar. Até 9 regras; de novo com o mesmo item tira a regra.',
    'guide.mistech.entry.golem_runes': 'Runas nos Golems',
    'guide.mistech.golem_runes.1': 'Golems de trabalho aprendem até §l2 runas§r (agachar + botão direito com a runa; ela é gasta):\n\n§fAr§r — 50% mais rápido.\n§6Terra§r — carga em dobro.\n§5Espaço§r — mais alcance; o de Mitril teleporta.\n§bTempo§r — quase sem espera.\n§cFogo§r — derrete o que carrega antes de guardar.\n\nMão vazia mostra a configuração; agachado com mão vazia limpa filtro e regras.',
}
EN = {
    'entity.mistech.work_golem': 'Work Golem',
    'entity.mistech.golem.sorter': 'Terracotta Golem', 'entity.mistech.golem.collector': 'Amethyst Golem',
    'entity.mistech.golem.messenger': 'Mithril Golem', 'entity.mistech.golem.crafter': 'Blackstone Golem',
    'entity.mistech.golem.keeper': 'Orichalcum Golem',
    'block.mistech.sorter_statue': 'Terracotta Statue', 'block.mistech.collector_statue': 'Amethyst Statue',
    'block.mistech.messenger_statue': 'Mithril Statue', 'block.mistech.crafter_statue': 'Blackstone Statue',
    'block.mistech.keeper_statue': 'Orichalcum Statue',
    'message.mistech.golem.describe': '%s · %s · %s · runes: %s',
    'message.mistech.golem.rune_full': 'This golem already has that rune or %s runes',
    'message.mistech.golem.rune_added': 'Rune learned: %s',
    'message.mistech.golem.no_recipe': 'No crafting table recipe found for %s',
    'message.mistech.golem.recipe': 'Recipe learned: %s',
    'message.mistech.golem.rules_full': 'The golem already keeps %s rules',
    'message.mistech.golem.rule_added': 'Keep %s × %s', 'message.mistech.golem.rule_removed': 'Rule removed: %s',
    'guide.mistech.entry.golem_sorter': 'Terracotta Golem',
    'guide.mistech.golem_sorter.1': 'The §6Sorter§r. With the Wand: §asource§r = the input chest, §6target§r = the center of the sorted area.\n\nEach item goes to the chest near the target (8 blocks) that §lalready holds that item§r; anything without a place goes into the target itself. Carries 32.',
    'guide.mistech.entry.golem_collector': 'Amethyst Golem',
    'guide.mistech.golem_collector.1': 'The §dCollector§r. Picks up dropped items within 8 blocks of the §6target§r (16 with a Space Rune) and stores them there. Right-click with an item: only that item. Carries 32.',
    'guide.mistech.entry.golem_messenger': 'Mithril Golem',
    'guide.mistech.golem_messenger.1': 'The §bMessenger§r. Like the Clay Golem, but carries §l64§r and walks much faster.\n\nWith a §5Space Rune§r it §lteleports§r between source and target.',
    'guide.mistech.entry.golem_crafter': 'Blackstone Golem',
    'guide.mistech.golem_crafter.1': 'The §8Crafter§r. Show it the finished item (right-click with it): it learns the crafting table recipe.\n\nIt takes ingredients from the §asource§r, crafts and stores in the §6target§r. Leftovers (empty bucket...) go back to the source.',
    'guide.mistech.entry.golem_keeper': 'Orichalcum Golem',
    'guide.mistech.golem_keeper.1': 'The §6Keeper§r of the §5Spatial Archive§r. The §6target§r must be in reach of a Core.\n\nRight-click with a stack: "keep this amount" in the target (64 torches in hand = keep 64). It fetches what is missing from the Archive. Up to 9 rules; the same item again removes the rule.',
    'guide.mistech.entry.golem_runes': 'Runes on Golems',
    'guide.mistech.golem_runes.1': 'Work golems learn up to §l2 runes§r (sneak + right-click with the rune; it is used up):\n\n§fAir§r — 50% faster.\n§6Earth§r — double load.\n§5Space§r — more reach; the Mithril one teleports.\n§bTime§r — almost no waiting.\n§cFire§r — smelts what it carries before storing.\n\nEmpty hand shows the setup; sneaking with an empty hand clears filter and rules.',
}
for f, d in (('pt_br.json', PT), ('en_us.json', EN)):
    j = json.load(open(LANG + f, encoding='utf-8'))
    j.update(d)
    json.dump(j, open(LANG + f, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
assert set(PT) == set(EN)
print('ok')
