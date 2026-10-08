# Grimório e textos da Rede Etérea. Idempotente.
# Uso: python3 tools/ether/guide_entries.py && python3 tools/guide/journey.py && python3 tools/guide/views.py
import json
BOOK = 'src/main/resources/assets/mistech/guide/book.json'
LANG = 'src/main/resources/assets/mistech/lang/'

def craft(grid, res):
    return {'type': 'crafting', 'grid': grid, 'result': res}
def txt(k):
    return {'type': 'text', 'text': k}

E = [
    ('transmission_pylon', 'mistech:transmission_pylon', [
        craft(['', 'mistech:mithril_ingot', '', '', 'mistech:energy_rune', 'mistech:mithril_ingot', 'minecraft:stick', '', ''], 'mistech:arcane_tuner'),
        txt('guide.mistech.transmission_pylon.tuner'),
        craft(['mistech:adamantine_ingot', 'mistech:space_rune', 'mistech:adamantine_ingot', 'minecraft:copper_ingot', 'mistech:impure_magic_block',
               'minecraft:copper_ingot', 'minecraft:polished_blackstone', 'minecraft:polished_blackstone', 'minecraft:polished_blackstone'], 'mistech:transmission_pylon'),
        txt('guide.mistech.transmission_pylon.1')]),
    ('ethereal_network', 'mistech:ethereal_nexus', [
        {'type': 'ritual', 'catalyst': 'mistech:impure_heart', 'ingredients': ['mistech:space_rune', 'mistech:void_rune', 'mistech:arcane_rune',
         'mistech:orichalcum_block', 'mistech:orichalcum_block', 'mistech:advanced_runic_battery', 'minecraft:ender_eye', 'mistech:impure_magic_shard'],
         'essence': 8, 'result': 'mistech:ethereal_nexus', 'min_tier': 3, 'title': 'guide.mistech.ritual.ethereal_nexus'},
        txt('guide.mistech.ethereal_network.1'),
        craft(['', 'minecraft:ender_eye', '', 'mistech:orichalcum_ingot', 'mistech:arcane_node', 'mistech:orichalcum_ingot', '', 'mistech:void_rune', ''], 'mistech:ethereal_node'),
        craft(['', 'minecraft:ender_eye', '', 'mistech:orichalcum_ingot', 'mistech:arcane_receiver', 'mistech:orichalcum_ingot', '', 'mistech:void_rune', ''], 'mistech:ethereal_receiver'),
        txt('guide.mistech.ethereal_network.2')]),
]
b = json.load(open(BOOK, encoding='utf-8'))
cat = next(c for c in b['categories'] if c['id'] == 'machines2')
have = {e['id']: e for c in b['categories'] for e in c['entries']}
for eid, icon, pages in E:
    if eid in have:
        have[eid]['pages'] = pages
    else:
        cat['entries'].append({'id': eid, 'icon': icon, 'name': 'guide.mistech.entry.' + eid, 'deps': [], 'task': {'type': 'read'}, 'x': 0, 'y': 0, 'pages': pages})
json.dump(b, open(BOOK, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

PT = {
    'item.mistech.arcane_tuner': 'Diapasão Arcano', 'block.mistech.transmission_pylon': 'Pilar de Transmissão',
    'block.mistech.ethereal_nexus': 'Nexo Etéreo', 'block.mistech.ethereal_node': 'Ponto Etéreo', 'block.mistech.ethereal_receiver': 'Receptor Etéreo',
    'item.mistech.arcane_tuner.desc': 'Liga Pilares; copia a sintonia de um Nexo e cola em Pontos/Receptores Etéreos',
    'item.mistech.arcane_tuner.network': 'Sintonia #%s', 'item.mistech.arcane_tuner.pylon': 'Pilar escolhido: %s',
    'message.mistech.tuner.copied': 'Sintonia copiada: %s', 'message.mistech.tuner.no_network': 'Copie a sintonia de um Nexo Etéreo primeiro',
    'message.mistech.tuner.tuned': 'Sintonizado em %s', 'message.mistech.tuner.pylon_first': 'Pilar escolhido; agora clique em outro pilar',
    'message.mistech.tuner.pylon_unlinked': 'Pilares desligados', 'message.mistech.tuner.pylon_linked': 'Pilares ligados (%s blocos)',
    'message.mistech.tuner.pylon_far': 'Longe demais: %s blocos (máx. %s)', 'message.mistech.tuner.pylon_full': 'Um dos pilares já tem %s ligações',
    'message.mistech.tuner.cleared': 'Diapasão limpo',
    'message.mistech.ether.nexus_info': 'Nexo Etéreo — %s (#%s)', 'message.mistech.ether.nexus_energy': 'Energia: %s / %s FE (%s células)',
    'message.mistech.ether.nexus_flow': 'Entrada: %s FE/t · Saída: %s FE/t · Pontos: %s · Receptores: %s',
    'guide.mistech.entry.transmission_pylon': 'Pilar de Transmissão',
    'guide.mistech.transmission_pylon.tuner': 'O §5Diapasão Arcano§r liga as peças das redes. §7Agachar + botão direito§r no ar limpa o que ele guardou.',
    'guide.mistech.transmission_pylon.1': 'Leva energia longe, sem perda. Com o Diapasão, clique num pilar e depois em outro a até §l48 blocos§r para ligá-los (até 4 ligações; clicar de novo desliga). Um feixe roxo aparece entre eles.\n\n§fBotão direito§r (mão vazia) troca o modo — o cristal muda de cor:\n§5Relé§r (roxo): só repassa.\n§aEntrada§r (verde): puxa energia do bloco §lembaixo§r dele (gerador, bateria).\n§6Saída§r (dourado): entrega no bloco §lembaixo§r dele (máquina, bateria).\n\nAté 10.000 FE/t por saída. Agachar + botão direito só mostra o estado da malha. Pontos/Receptores Arcanos perto dos pilares também continuam funcionando.',
    'message.mistech.pylon.status': 'Pilar: %s · %s ligações · malha de %s pilares · %s entradas · %s saídas',
    'message.mistech.pylon.mode.relay': '§5Relé§r', 'message.mistech.pylon.mode.input': '§aEntrada§r', 'message.mistech.pylon.mode.output': '§6Saída§r',
    'message.mistech.pylon.no_source': 'Nada para puxar energia embaixo deste pilar',
    'message.mistech.pylon.no_sink': 'Nada para receber energia embaixo deste pilar',
    'guide.mistech.entry.ethereal_network': 'Rede Etérea', 'guide.mistech.ritual.ethereal_nexus': 'Despertar o Nexo',
    'guide.mistech.ethereal_network.1': 'O §dNexo Etéreo§r guarda a energia de uma §dsintonia§r. Cada §5Bateria Rúnica Aprimorada§r encostada nele (as 8 em volta, na mesma altura) vira uma §lcélula§r: 500 mil FE + 1 milhão por célula.\n\nA energia fica na sintonia, não no bloco: funciona com o Nexo longe. Botão direito mostra a rede. Quebrado, o item guarda a sintonia.',
    'guide.mistech.ethereal_network.2': 'O §5Ponto Etéreo§r puxa do bloco onde está preso (até 50.000 FE/t) e o §5Receptor Etéreo§r entrega (até 20.000 FE/t) — a §lqualquer distância e em qualquer dimensão§r, sem perda.\n\nCom o Diapasão: clique no Nexo para copiar a sintonia e depois nos Pontos e Receptores.',
}
EN = {
    'item.mistech.arcane_tuner': 'Arcane Tuner', 'block.mistech.transmission_pylon': 'Transmission Pylon',
    'block.mistech.ethereal_nexus': 'Ethereal Nexus', 'block.mistech.ethereal_node': 'Ethereal Node', 'block.mistech.ethereal_receiver': 'Ethereal Receiver',
    'item.mistech.arcane_tuner.desc': 'Links Pylons; copies a Nexus tuning and pastes it on Ethereal Nodes/Receivers',
    'item.mistech.arcane_tuner.network': 'Tuning #%s', 'item.mistech.arcane_tuner.pylon': 'Chosen pylon: %s',
    'message.mistech.tuner.copied': 'Tuning copied: %s', 'message.mistech.tuner.no_network': 'Copy the tuning from an Ethereal Nexus first',
    'message.mistech.tuner.tuned': 'Tuned to %s', 'message.mistech.tuner.pylon_first': 'Pylon chosen; now click another pylon',
    'message.mistech.tuner.pylon_unlinked': 'Pylons unlinked', 'message.mistech.tuner.pylon_linked': 'Pylons linked (%s blocks)',
    'message.mistech.tuner.pylon_far': 'Too far: %s blocks (max %s)', 'message.mistech.tuner.pylon_full': 'One of the pylons already has %s links',
    'message.mistech.tuner.cleared': 'Tuner cleared',
    'message.mistech.ether.nexus_info': 'Ethereal Nexus — %s (#%s)', 'message.mistech.ether.nexus_energy': 'Energy: %s / %s FE (%s cells)',
    'message.mistech.ether.nexus_flow': 'In: %s FE/t · Out: %s FE/t · Nodes: %s · Receivers: %s',
    'guide.mistech.entry.transmission_pylon': 'Transmission Pylon',
    'guide.mistech.transmission_pylon.tuner': 'The §5Arcane Tuner§r links network parts. §7Sneak + right-click§r in the air clears what it holds.',
    'guide.mistech.transmission_pylon.1': 'Carries energy far, with no loss. With the Tuner, click a pylon and then another up to §l48 blocks§r away to link them (up to 4 links; clicking again unlinks). A purple beam appears between them.\n\n§fRight-click§r (empty hand) cycles the mode — the crystal changes color:\n§5Relay§r (purple): only passes along.\n§aInput§r (green): pulls energy from the block §lbelow§r it (generator, battery).\n§6Output§r (gold): delivers to the block §lbelow§r it (machine, battery).\n\nUp to 10,000 FE/t per output. Sneak + right-click only shows the mesh status. Arcane Nodes/Receivers near pylons still work too.',
    'message.mistech.pylon.status': 'Pylon: %s · %s links · mesh of %s pylons · %s inputs · %s outputs',
    'message.mistech.pylon.mode.relay': '§5Relay§r', 'message.mistech.pylon.mode.input': '§aInput§r', 'message.mistech.pylon.mode.output': '§6Output§r',
    'message.mistech.pylon.no_source': 'Nothing to pull energy from below this pylon',
    'message.mistech.pylon.no_sink': 'Nothing to receive energy below this pylon',
    'guide.mistech.entry.ethereal_network': 'Ethereal Network', 'guide.mistech.ritual.ethereal_nexus': 'Awaken the Nexus',
    'guide.mistech.ethereal_network.1': 'The §dEthereal Nexus§r stores the energy of a §dtuning§r. Each §5Advanced Runic Battery§r touching it (the 8 around, same height) becomes a §lcell§r: 500k FE + 1 million per cell.\n\nThe energy lives in the tuning, not the block: it works with the Nexus far away. Right-click shows the network. Broken, the item keeps the tuning.',
    'guide.mistech.ethereal_network.2': 'The §5Ethereal Node§r pulls from the block it is attached to (up to 50,000 FE/t) and the §5Ethereal Receiver§r delivers (up to 20,000 FE/t) — at §lany distance and in any dimension§r, lossless.\n\nWith the Tuner: click the Nexus to copy its tuning, then the Nodes and Receivers.',
}
for f, d in (('pt_br.json', PT), ('en_us.json', EN)):
    j = json.load(open(LANG + f, encoding='utf-8')); j.update(d)
    json.dump(j, open(LANG + f, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
assert set(PT) == set(EN)
print('ok')
