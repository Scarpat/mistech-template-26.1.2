# Grimório e textos do Arquivo Espacial. Idempotente.
# Uso: python3 tools/archive/guide_entries.py && python3 tools/guide/journey.py && python3 tools/guide/views.py
import json
BOOK = 'src/main/resources/assets/mistech/guide/book.json'
LANG = 'src/main/resources/assets/mistech/lang/'


def craft(grid, res, shapeless=False):
    p = {'type': 'crafting', 'grid': grid, 'result': res}
    if shapeless:
        p['shapeless'] = True
    return p


def writing(ings, chalk, res):
    return {'type': 'writing', 'ingredients': ings, 'chalk': chalk, 'result': res}


def txt(k):
    return {'type': 'text', 'text': k}


E = [
    ('archive_core', 'mistech:archive_core', [
        craft(['mistech:mithril_ingot', 'mistech:space_rune', 'mistech:mithril_ingot', 'minecraft:amethyst_shard', 'mistech:spatial_barrel',
               'minecraft:amethyst_shard', 'mistech:mithril_ingot', 'mistech:space_rune', 'mistech:mithril_ingot'], 'mistech:archive_core'),
        txt('guide.mistech.archive_core.1'),
        writing(['mistech:space_rune'], 10, 'mistech:archive_scroll'),
        writing(['mistech:space_rune', 'mistech:mithril_ingot', 'mistech:mithril_ingot'], 15, 'mistech:mithril_archive_scroll'),
        writing(['mistech:space_rune', 'mistech:adamantine_ingot', 'mistech:adamantine_ingot'], 20, 'mistech:adamantine_archive_scroll'),
        writing(['mistech:space_rune', 'mistech:orichalcum_ingot', 'mistech:orichalcum_ingot'], 25, 'mistech:orichalcum_archive_scroll'),
        writing(['mistech:space_rune', 'mistech:void_rune'], 20, 'mistech:void_archive_scroll'),
        txt('guide.mistech.archive_core.2')]),
    ('archive_terminal', 'mistech:archive_terminal', [
        craft(['', 'minecraft:ender_eye', '', 'mistech:space_rune', 'minecraft:crafting_table', 'mistech:space_rune',
               'minecraft:dark_oak_log', 'minecraft:dark_oak_log', 'minecraft:dark_oak_log'], 'mistech:archive_terminal'),
        txt('guide.mistech.archive_terminal.1'), txt('guide.mistech.archive_terminal.2')]),
    ('archive_network', 'mistech:archive_relay', [
        craft(['', 'minecraft:amethyst_shard', '', 'mistech:mithril_ingot', 'mistech:space_rune', 'mistech:mithril_ingot',
               '', 'minecraft:polished_blackstone', ''], 'mistech:archive_relay'),
        txt('guide.mistech.archive_network.relay'),
        craft(['mistech:space_rune', 'minecraft:honeycomb', 'minecraft:paper', '', '', '', '', '', ''], 'mistech:archive_seal', True),
        txt('guide.mistech.archive_network.seal'),
        craft(['', 'minecraft:hopper', '', 'mistech:mithril_ingot', 'mistech:space_rune', 'mistech:mithril_ingot', '', '', ''], 'mistech:archive_port'),
        txt('guide.mistech.archive_network.port')]),
    ('archive_mirror', 'mistech:archive_mirror', [
        craft(['', 'minecraft:glass_pane', '', 'minecraft:glass_pane', 'minecraft:ender_eye', 'minecraft:glass_pane',
               '', 'mistech:mithril_ingot', ''], 'mistech:archive_mirror'),
        txt('guide.mistech.archive_mirror.1')]),
]

b = json.load(open(BOOK, encoding='utf-8'))
cat = next(c for c in b['categories'] if c['id'] == 'automation')
have = {e['id']: e for c in b['categories'] for e in c['entries']}
for eid, icon, pages in E:
    if eid in have:
        have[eid]['pages'] = pages
    else:
        cat['entries'].append({'id': eid, 'icon': icon, 'name': 'guide.mistech.entry.' + eid, 'deps': [], 'task': {'type': 'read'},
                               'x': 0, 'y': 0, 'pages': pages})
json.dump(b, open(BOOK, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

PT = {
    'block.mistech.archive_core': 'Núcleo do Arquivo', 'block.mistech.archive_terminal': 'Mesa do Arquivo',
    'block.mistech.archive_relay': 'Relé do Arquivo', 'block.mistech.archive_seal': 'Selo do Arquivo', 'block.mistech.archive_port': 'Boca do Arquivo',
    'item.mistech.archive_scroll': 'Pergaminho Espacial', 'item.mistech.mithril_archive_scroll': 'Pergaminho Espacial de Mitril',
    'item.mistech.adamantine_archive_scroll': 'Pergaminho Espacial de Adamantina', 'item.mistech.orichalcum_archive_scroll': 'Pergaminho Espacial de Oricalco',
    'item.mistech.void_archive_scroll': 'Pergaminho do Vazio', 'item.mistech.archive_mirror': 'Espelho do Arquivo',
    'item.mistech.archive_scroll.types': 'Tipos: %s / %s', 'item.mistech.archive_scroll.items': 'Itens: %s / %s',
    'item.mistech.archive_scroll.items_void': 'Itens: %s (sem limite)',
    'item.mistech.archive_scroll.desc': 'Vai no Núcleo do Arquivo. O conteúdo fica no pergaminho mesmo fora do Núcleo.',
    'item.mistech.archive_mirror.unbound': 'Sem vínculo (botão direito num Núcleo)', 'item.mistech.archive_mirror.bound': 'Vinculado a %s (%s)',
    'item.mistech.archive_mirror.desc': 'Botão direito no ar: abre o Arquivo de longe',
    'container.mistech.archive': 'Arquivo Espacial',
    'screen.mistech.archive.search': 'Buscar', 'screen.mistech.archive.sort': 'Ordem: nome (A), quantidade (#), mod (@)',
    'screen.mistech.archive.clear': 'Devolver a grade ao Arquivo', 'screen.mistech.archive.count': '%s tipos · %s',
    'screen.mistech.archive.offline': 'Sem energia', 'screen.mistech.archive.stored': 'Guardado: %s',
    'message.mistech.archive.no_core': 'Nenhum Núcleo do Arquivo alcança esta mesa (16 blocos, ou use Relés)',
    'message.mistech.archive_port.added': '%s no filtro', 'message.mistech.archive_port.removed': '%s fora do filtro',
    'message.mistech.archive_port.full': 'O filtro está cheio (9 itens)', 'message.mistech.archive_port.cleared': 'Filtro limpo',
    'message.mistech.archive_port.import': 'Modo: puxar para o Arquivo', 'message.mistech.archive_port.export': 'Modo: empurrar do Arquivo (só o filtro)',
    'message.mistech.archive_mirror.bound': 'Espelho vinculado ao Núcleo', 'message.mistech.archive_mirror.unbound': 'Vincule o espelho a um Núcleo primeiro',
    'message.mistech.archive_mirror.gone': 'O Núcleo vinculado não existe mais', 'message.mistech.archive_mirror.no_energy': 'O espelho precisa de %s FE',
    'guide.mistech.entry.archive_core': 'Núcleo do Arquivo',
    'guide.mistech.archive_core.1': 'O coração do §5Arquivo Espacial§r, um armazenamento central §lsem cabos§r. Guarda até 9 §dPergaminhos Espaciais§r e precisa de §benergia§r (1 FE/t + o custo de cada pergaminho; 5.000 FE/t de entrada).\n\nBotão direito abre os espaços dos pergaminhos.',
    'guide.mistech.archive_core.2': 'Pergaminhos (tipos / itens / FE por tick):\n§7Comum§r 27 / 4.096 / 2\n§bMitril§r 63 / 32.768 / 5\n§cAdamantina§r 63 / 262.144 / 10\n§6Oricalco§r 127 / 2.097.152 / 20\n§5Vazio§r 1 tipo, sem limite / 5\n\nO conteúdo fica no pergaminho: dá para tirar e levar para outro Núcleo.',
    'guide.mistech.entry.archive_terminal': 'Mesa do Arquivo',
    'guide.mistech.archive_terminal.1': 'Mostra §ltudo§r o que está guardado. Precisa estar a até 16 blocos do Núcleo (ou de um Relé).\n\n• Clique: pega uma pilha (direito: metade).\n• Shift + clique: direto para o inventário.\n• Clique com item na mão: guarda (direito: só 1).\n• Shift + clique no inventário: guarda a pilha.',
    'guide.mistech.archive_terminal.2': '§lBusca§r: nome, §7@mod§r ou §7#tag§r. O botão ao lado troca a ordem (nome, quantidade, mod).\n\nA §lmesa de trabalho 3×3§r puxa do Arquivo: depois de cada craft, os ingredientes que acabaram voltam sozinhos. Shift no resultado faz o máximo. O §7×§r devolve a grade ao Arquivo.',
    'guide.mistech.entry.archive_network': 'Relé, Selo e Boca',
    'guide.mistech.archive_network.relay': 'O §5Relé§r leva o alcance do Núcleo mais 16 blocos a partir dele. Relés encadeiam: cada um a até 16 de outro (ou do Núcleo) entra.',
    'guide.mistech.archive_network.seal': 'O §5Selo§r, preso num recipiente (Barril Espacial, baú, a Câmara da Mineradora...), põe o conteúdo dele no Arquivo: aparece na Mesa e recebe o que não cabe nos pergaminhos.',
    'guide.mistech.archive_network.port': 'A §5Boca§r, presa num recipiente ou máquina, move 64 itens a cada meio segundo. §7Mão vazia§r troca o modo: §lpuxar§r (do recipiente para o Arquivo) ou §lempurrar§r (do Arquivo para o recipiente, só o filtro). §7Botão direito com item§r: põe/tira do filtro (até 9). §7Agachado com mão vazia§r: limpa.',
    'guide.mistech.entry.archive_mirror': 'Espelho do Arquivo',
    'guide.mistech.archive_mirror.1': 'Botão direito num §5Núcleo§r vincula o espelho; botão direito no ar abre a Mesa do Arquivo de qualquer lugar.\n\nAté 64 blocos: de graça. Mais longe: 500 FE. Outra dimensão: 1.000 FE por abertura (energia do espelho; carrega na Bateria de Bolso).',
}
EN = {
    'block.mistech.archive_core': 'Archive Core', 'block.mistech.archive_terminal': 'Archive Table',
    'block.mistech.archive_relay': 'Archive Relay', 'block.mistech.archive_seal': 'Archive Seal', 'block.mistech.archive_port': 'Archive Port',
    'item.mistech.archive_scroll': 'Spatial Scroll', 'item.mistech.mithril_archive_scroll': 'Mithril Spatial Scroll',
    'item.mistech.adamantine_archive_scroll': 'Adamantine Spatial Scroll', 'item.mistech.orichalcum_archive_scroll': 'Orichalcum Spatial Scroll',
    'item.mistech.void_archive_scroll': 'Void Scroll', 'item.mistech.archive_mirror': 'Archive Mirror',
    'item.mistech.archive_scroll.types': 'Types: %s / %s', 'item.mistech.archive_scroll.items': 'Items: %s / %s',
    'item.mistech.archive_scroll.items_void': 'Items: %s (no limit)',
    'item.mistech.archive_scroll.desc': 'Goes in the Archive Core. Contents stay in the scroll even outside the Core.',
    'item.mistech.archive_mirror.unbound': 'Unbound (right-click a Core)', 'item.mistech.archive_mirror.bound': 'Bound to %s (%s)',
    'item.mistech.archive_mirror.desc': 'Right-click in the air: open the Archive from afar',
    'container.mistech.archive': 'Spatial Archive',
    'screen.mistech.archive.search': 'Search', 'screen.mistech.archive.sort': 'Order: name (A), count (#), mod (@)',
    'screen.mistech.archive.clear': 'Return the grid to the Archive', 'screen.mistech.archive.count': '%s types · %s',
    'screen.mistech.archive.offline': 'No energy', 'screen.mistech.archive.stored': 'Stored: %s',
    'message.mistech.archive.no_core': 'No Archive Core reaches this table (16 blocks, or use Relays)',
    'message.mistech.archive_port.added': '%s added to the filter', 'message.mistech.archive_port.removed': '%s removed from the filter',
    'message.mistech.archive_port.full': 'The filter is full (9 items)', 'message.mistech.archive_port.cleared': 'Filter cleared',
    'message.mistech.archive_port.import': 'Mode: pull into the Archive', 'message.mistech.archive_port.export': 'Mode: push from the Archive (filter only)',
    'message.mistech.archive_mirror.bound': 'Mirror bound to the Core', 'message.mistech.archive_mirror.unbound': 'Bind the mirror to a Core first',
    'message.mistech.archive_mirror.gone': 'The bound Core no longer exists', 'message.mistech.archive_mirror.no_energy': 'The mirror needs %s FE',
    'guide.mistech.entry.archive_core': 'Archive Core',
    'guide.mistech.archive_core.1': 'The heart of the §5Spatial Archive§r, a central storage §lwithout cables§r. Holds up to 9 §dSpatial Scrolls§r and needs §benergy§r (1 FE/t + each scroll cost; 5,000 FE/t input).\n\nRight-click opens the scroll slots.',
    'guide.mistech.archive_core.2': 'Scrolls (types / items / FE per tick):\n§7Common§r 27 / 4,096 / 2\n§bMithril§r 63 / 32,768 / 5\n§cAdamantine§r 63 / 262,144 / 10\n§6Orichalcum§r 127 / 2,097,152 / 20\n§5Void§r 1 type, unlimited / 5\n\nContents live in the scroll: you can take it out and move it to another Core.',
    'guide.mistech.entry.archive_terminal': 'Archive Table',
    'guide.mistech.archive_terminal.1': 'Shows §leverything§r stored. Must be within 16 blocks of the Core (or a Relay).\n\n• Click: take a stack (right: half).\n• Shift + click: straight to your inventory.\n• Click with an item held: store it (right: just 1).\n• Shift + click in your inventory: store the stack.',
    'guide.mistech.archive_terminal.2': '§lSearch§r: name, §7@mod§r or §7#tag§r. The button next to it changes the order (name, count, mod).\n\nThe §l3×3 crafting grid§r pulls from the Archive: after each craft, used-up ingredients come back by themselves. Shift on the result crafts as many as possible. The §7×§r returns the grid to the Archive.',
    'guide.mistech.entry.archive_network': 'Relay, Seal and Port',
    'guide.mistech.archive_network.relay': 'The §5Relay§r extends the Core reach by 16 more blocks from it. Relays chain: each one within 16 of another (or of the Core) joins.',
    'guide.mistech.archive_network.seal': 'The §5Seal§r, attached to a container (Spatial Barrel, chest, the Miner Chamber...), adds its contents to the Archive: they show on the Table and receive what does not fit in the scrolls.',
    'guide.mistech.archive_network.port': 'The §5Port§r, attached to a container or machine, moves 64 items every half second. §7Empty hand§r toggles the mode: §lpull§r (container into Archive) or §lpush§r (Archive into container, filter only). §7Right-click with an item§r: add/remove it from the filter (up to 9). §7Sneak with empty hand§r: clear.',
    'guide.mistech.entry.archive_mirror': 'Archive Mirror',
    'guide.mistech.archive_mirror.1': 'Right-click a §5Core§r to bind the mirror; right-click in the air opens the Archive Table from anywhere.\n\nUp to 64 blocks: free. Farther: 500 FE. Another dimension: 1,000 FE per opening (the mirror energy; charge it with the Pocket Battery).',
}
for f, d in (('pt_br.json', PT), ('en_us.json', EN)):
    j = json.load(open(LANG + f, encoding='utf-8'))
    j.update(d)
    json.dump(j, open(LANG + f, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
assert set(PT) == set(EN)
print('ok')
