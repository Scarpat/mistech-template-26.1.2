#!/usr/bin/env python3
"""Dados da Fase 8 (autocrafting no Arquivo): Pergaminho de Padrão e Estante de Padrões, textos e Grimório.
Idempotente. Depois rode tools/guide/views.py.
Uso (na pasta do projeto): python3 tools/autocraft/gen_data.py"""
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


dump(D + 'mistech/recipe/pattern_scroll.json', {
    'type': 'minecraft:crafting_shapeless', 'category': 'misc',
    'ingredients': ['minecraft:paper', 'minecraft:paper', 'minecraft:lapis_lazuli', 'minecraft:redstone'],
    'result': {'id': 'mistech:pattern_scroll', 'count': 2}})
SHELF = ['#minecraft:planks', '#minecraft:planks', '#minecraft:planks',
         'minecraft:lapis_lazuli', 'mistech:archive_relay', 'minecraft:lapis_lazuli',
         '#minecraft:planks', '#minecraft:planks', '#minecraft:planks']
dump(D + 'mistech/recipe/pattern_shelf.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['PPP', 'LRL', 'PPP'],
    'key': {'P': '#minecraft:planks', 'L': 'minecraft:lapis_lazuli', 'R': 'mistech:archive_relay'},
    'result': {'id': 'mistech:pattern_shelf', 'count': 1}})
dump(D + 'mistech/loot_table/blocks/pattern_shelf.json', {
    'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'mistech:pattern_shelf'}],
                                          'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
    'random_sequence': 'mistech:blocks/pattern_shelf'})
p = D + 'minecraft/tags/block/mineable/axe.json'
j = json.load(open(p))
if 'mistech:pattern_shelf' not in j['values']:
    j['values'].append('mistech:pattern_shelf')
dump(p, j)

dump(A + 'models/block/pattern_shelf.json', {'parent': 'minecraft:block/orientable', 'textures': {
    'front': 'mistech:block/pattern_shelf_front', 'side': 'minecraft:block/oak_planks', 'top': 'minecraft:block/oak_planks'}})
bs = {'variants': {}}
for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
    v = {'model': 'mistech:block/pattern_shelf'}
    if r:
        v['y'] = r
    bs['variants']['facing=%s' % f] = v
dump(A + 'blockstates/pattern_shelf.json', bs)
dump(A + 'items/pattern_shelf.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:block/pattern_shelf'}})
dump(A + 'items/pattern_scroll.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:item/pattern_scroll'}})
dump(A + 'models/item/pattern_scroll.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mistech:item/pattern_scroll'}})

LANG = A + 'lang/'
PT = {
    'item.mistech.pattern_scroll': 'Pergaminho de Padrão', 'block.mistech.pattern_shelf': 'Estante de Padrões',
    'item.mistech.pattern_scroll.blank': 'Em branco. Com um item na outra mão, clique no ar.',
    'item.mistech.pattern_scroll.crafting': 'Mesa: faz %s× %s',
    'item.mistech.pattern_scroll.processing': 'Processamento',
    'item.mistech.pattern_scroll.in': 'Entra: %s× %s', 'item.mistech.pattern_scroll.out': 'Sai: %s× %s',
    'item.mistech.pattern_scroll.no_output': 'Falta a saída: agachado + clique no ar com ela na outra mão',
    'item.mistech.pattern_scroll.machine': 'Máquina em %s, %s, %s',
    'message.mistech.pattern.help': 'Segure o item que quer fabricar na outra mão',
    'message.mistech.pattern.crafting': 'Padrão de mesa: %s',
    'message.mistech.pattern.cleared': 'Padrão apagado',
    'message.mistech.pattern.need_machine': 'Primeiro, agachado, clique na máquina com a entrada na outra mão',
    'message.mistech.pattern.not_machine': 'Esse bloco não tem inventário',
    'message.mistech.pattern.need_input': 'Segure a entrada (com a quantidade) na outra mão',
    'message.mistech.pattern.processing_input': 'Entra %s× %s em %s. Agora a saída: agachado + clique no ar com ela na outra mão',
    'message.mistech.pattern.processing_done': 'Padrão: %s× %s → %s× %s',
    'message.mistech.archive.craft_queued': 'Pedido: %s× %s (%s trabalhos na fila)',
    'message.mistech.archive.craft_missing': 'Não dá para fazer %s× %s. Falta:',
    'message.mistech.archive.craft_cancelled': '%s trabalhos de %s cancelados',
    'screen.mistech.archive.craft_tab': 'Fabricar (padrões das Estantes)',
    'screen.mistech.archive.jobs': 'Fila: %s',
    'screen.mistech.archive.no_patterns': 'Nenhum padrão nas Estantes',
    'screen.mistech.archive.queued': 'Na fila: %s',
    'screen.mistech.archive.craft_help': 'Clique: 1 · Direito: 16 · Shift: 64 · Shift + direito: cancelar',
    'guide.mistech.entry.archive_autocraft': 'Fabricação no Arquivo',
    'guide.mistech.archive_autocraft.scroll': 'O §9Pergaminho de Padrão§r guarda uma receita para o Arquivo fabricar.',
    'guide.mistech.archive_autocraft.shelf': 'A §9Estante de Padrões§r guarda 18 padrões e liga no Arquivo como um Relé (até 16 blocos do Núcleo ou de um Relé).',
    'guide.mistech.archive_autocraft.1': '§lFazer um padrão§r\n§eMesa:§r com o pergaminho na mão e o item na outra mão, clique no ar.\n§eMáquina:§r agachado, clique na máquina com a entrada (e a quantidade) na outra mão; depois, agachado, clique no ar com a saída na outra mão.\nAgachado com a outra mão vazia apaga.',
    'guide.mistech.archive_autocraft.2': '§lPedir§r\nNa Mesa do Arquivo, o botão §e⚒§r troca a grade para os fabricáveis. Clique pede 1 (direito 16, Shift 64; Shift + direito cancela). O Arquivo monta as etapas, usa o que já tem e, se faltar algo, diz o quê no chat.',
    'guide.mistech.archive_autocraft.3': '§lQuem trabalha§r\nLigue golens a uma Estante com a Vara de Comando (a Estante como origem):\n§8Artesão§r faz os trabalhos de mesa ali mesmo.\n§bMensageiro§r leva a entrada até a máquina do padrão, espera e traz a saída.\n§6Guardião§r sem o item pede para fabricar.\nCada passo gasta 200 FE do Núcleo.',
}
EN = {
    'item.mistech.pattern_scroll': 'Pattern Scroll', 'block.mistech.pattern_shelf': 'Pattern Shelf',
    'item.mistech.pattern_scroll.blank': 'Blank. Hold an item in the other hand and click the air.',
    'item.mistech.pattern_scroll.crafting': 'Crafting: makes %s× %s',
    'item.mistech.pattern_scroll.processing': 'Processing',
    'item.mistech.pattern_scroll.in': 'In: %s× %s', 'item.mistech.pattern_scroll.out': 'Out: %s× %s',
    'item.mistech.pattern_scroll.no_output': 'Missing output: sneak + click the air holding it in the other hand',
    'item.mistech.pattern_scroll.machine': 'Machine at %s, %s, %s',
    'message.mistech.pattern.help': 'Hold the item to craft in your other hand',
    'message.mistech.pattern.crafting': 'Crafting pattern: %s',
    'message.mistech.pattern.cleared': 'Pattern cleared',
    'message.mistech.pattern.need_machine': 'First, sneaking, click the machine holding the input in your other hand',
    'message.mistech.pattern.not_machine': 'That block has no inventory',
    'message.mistech.pattern.need_input': 'Hold the input (with the amount) in your other hand',
    'message.mistech.pattern.processing_input': '%s× %s goes into %s. Now the output: sneak + click the air holding it in your other hand',
    'message.mistech.pattern.processing_done': 'Pattern: %s× %s → %s× %s',
    'message.mistech.archive.craft_queued': 'Requested: %s× %s (%s jobs queued)',
    'message.mistech.archive.craft_missing': 'Cannot make %s× %s. Missing:',
    'message.mistech.archive.craft_cancelled': '%s jobs of %s cancelled',
    'screen.mistech.archive.craft_tab': 'Craft (Pattern Shelf patterns)',
    'screen.mistech.archive.jobs': 'Queue: %s',
    'screen.mistech.archive.no_patterns': 'No patterns on the Shelves',
    'screen.mistech.archive.queued': 'Queued: %s',
    'screen.mistech.archive.craft_help': 'Click: 1 · Right: 16 · Shift: 64 · Shift + right: cancel',
    'guide.mistech.entry.archive_autocraft': 'Archive Crafting',
    'guide.mistech.archive_autocraft.scroll': 'The §9Pattern Scroll§r stores a recipe for the Archive to craft.',
    'guide.mistech.archive_autocraft.shelf': 'The §9Pattern Shelf§r holds 18 patterns and joins the Archive like a Relay (within 16 blocks of the Core or a Relay).',
    'guide.mistech.archive_autocraft.1': '§lMaking a pattern§r\n§eCrafting:§r with the scroll in hand and the item in the other hand, click the air.\n§eMachine:§r sneaking, click the machine with the input (and amount) in the other hand; then, sneaking, click the air with the output in the other hand.\nSneaking with an empty other hand clears it.',
    'guide.mistech.archive_autocraft.2': '§lRequesting§r\nAt the Archive Terminal, the §e⚒§r button switches the grid to craftables. Click requests 1 (right 16, Shift 64; Shift + right cancels). The Archive plans the steps, uses what it has and, if something is missing, says what in chat.',
    'guide.mistech.archive_autocraft.3': '§lWho works§r\nBind golems to a Shelf with the Command Rod (the Shelf as the source):\n§8Crafter§r does crafting jobs right there.\n§bMessenger§r takes the input to the pattern\'s machine, waits and brings the output back.\n§6Keeper§r without the item requests it.\nEach step uses 200 FE from the Core.',
}
for name, d in (('pt_br', PT), ('en_us', EN)):
    p = LANG + name + '.json'
    data = json.load(open(p, encoding='utf-8'))
    data.update(d)
    dump(p, data)

BOOK = A + 'guide/book.json'
E = [{'id': 'archive_autocraft', 'icon': 'mistech:pattern_shelf', 'name': 'guide.mistech.entry.archive_autocraft',
      'deps': ['archive_network', 'golem_crafter'], 'task': {'type': 'item', 'items': ['mistech:pattern_shelf']}, 'x': 8.0, 'y': 2.0,
      'pages': [{'type': 'crafting', 'grid': ['minecraft:paper', 'minecraft:paper', 'minecraft:lapis_lazuli', 'minecraft:redstone', '', '', '', '', ''],
                 'result': 'mistech:pattern_scroll', 'text': 'guide.mistech.archive_autocraft.scroll'},
                {'type': 'crafting', 'grid': SHELF, 'result': 'mistech:pattern_shelf', 'text': 'guide.mistech.archive_autocraft.shelf'},
                {'type': 'text', 'text': 'guide.mistech.archive_autocraft.1'},
                {'type': 'text', 'text': 'guide.mistech.archive_autocraft.2'},
                {'type': 'text', 'text': 'guide.mistech.archive_autocraft.3'}]}]
book = json.load(open(BOOK, encoding='utf-8'))
cat = next(c for c in book['categories'] if c['id'] == 'automation')
ids = {e['id'] for e in E}
cat['entries'] = [e for e in cat['entries'] if e['id'] not in ids] + E
dump(BOOK, book, newline=False)
print('ok')
