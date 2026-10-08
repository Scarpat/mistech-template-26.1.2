"""Adiciona a categoria Tarô ao Grimório (book.json + traduções). Uso: python guide_tarot.py <assets/mistech>"""
import json, sys, os
A = sys.argv[1]
bp = os.path.join(A, 'guide/book.json'); b = json.load(open(bp, encoding='utf-8'))
entries = [
 {"id":"tarot_circle","icon":"minecraft:crying_obsidian","name":"guide.mistech.entry.tarot_circle","deps":["astrology_table"],
  "task":{"type":"read"},"x":0.0,"y":0.5,"pages":[
   {"type":"multiblock","pattern":["00AAA00","0COAOC0","AOAAAOA","AAACAAA","AOAAAOA","0COAOC0","00AAA00"],
    "key":{"A":"minecraft:amethyst_block","O":"minecraft:obsidian","C":"minecraft:crying_obsidian"},"text":"guide.mistech.tarot_circle.legend"},
   {"type":"text","text":"guide.mistech.tarot_circle.1"},
   {"type":"text","text":"guide.mistech.tarot_circle.2"}]},
 {"id":"tarot_table","icon":"mistech:tarot_table","name":"guide.mistech.entry.tarot_table","deps":["tarot_circle"],
  "task":{"type":"item","items":["mistech:tarot_table"]},"x":1.6,"y":0.5,"size":"big","pages":[
   {"type":"spotlight","item":"mistech:tarot_table","text":"guide.mistech.tarot_table.1"},
   {"type":"text","text":"guide.mistech.tarot_table.2"}]},
 {"id":"tarot_cards","icon":"mistech:inverted_tarot_card_the_fool","name":"guide.mistech.entry.tarot_cards","deps":["tarot_table"],
  "task":{"type":"item","items":["mistech:blank_tarot_card"]},"x":3.2,"y":0.5,"pages":[
   {"type":"crafting","grid":["minecraft:paper","minecraft:paper","minecraft:amethyst_shard","minecraft:gold_nugget","","","","",""],
    "result":"mistech:blank_tarot_card","text":"guide.mistech.tarot_cards.recipe","shapeless":True},
   {"type":"text","text":"guide.mistech.tarot_cards.1"}]},
 {"id":"arcane_plane","icon":"minecraft:ender_eye","name":"guide.mistech.entry.arcane_plane","deps":["tarot_cards"],
  "task":{"type":"read"},"x":4.8,"y":0.5,"pages":[
   {"type":"text","text":"guide.mistech.arcane_plane.1"},
   {"type":"text","text":"guide.mistech.arcane_plane.2"}]},
 {"id":"arcana_bond","icon":"mistech:tarot_card_the_world","name":"guide.mistech.entry.arcana_bond","deps":["arcane_plane","pure_orichalcum_gear"],
  "task":{"type":"read"},"x":6.4,"y":0.5,"pages":[
   {"type":"text","text":"guide.mistech.arcana_bond.1"}]},
]
cat = {"id":"tarot","icon":"mistech:tarot_card_the_star","name":"guide.mistech.cat.tarot","description":"guide.mistech.cat.tarot.desc","entries":entries}
b['categories'] = [c for c in b['categories'] if c['id'] != 'tarot']
idx = next(i for i,c in enumerate(b['categories']) if c['id']=='astrology')
b['categories'].insert(idx+1, cat)
ids = [e['id'] for e in entries]
for view, cid in (('progress','p_tier3'),('dimension','d_overworld_tier3')):
    for c in b['views'][view]:
        if c['id'] == cid:
            c['entries'] = [e for e in c['entries'] if e['id'] not in ids]
            for i, eid in enumerate(ids):
                c['entries'].append({"id": eid, "x": 8.0, "y": float(i)})
json.dump(b, open(bp,'w',encoding='utf-8'), indent=2, ensure_ascii=False)

PT = {
 'guide.mistech.cat.tarot':'Tarô',
 'guide.mistech.cat.tarot.desc':'Vinte e dois arcanos esperam do outro lado das cartas. Invoque, enfrente e conquiste cada um.',
 'guide.mistech.entry.tarot_circle':'Círculo de Tarologia',
 'guide.mistech.tarot_circle.legend':'Ametista, obsidiana e obsidiana chorona. Espaços vazios podem ser qualquer coisa.',
 'guide.mistech.tarot_circle.1':'Construa esta base 7×7 e coloque uma §5Mesa de Escrita§r em cima da §dobsidiana chorona§r do centro.\n\nDepois use a §5Pedra Guia§r na mesa: ela vira uma §5Mesa de Tarologia§r. Se faltar um bloco, a pedra mostra onde.',
 'guide.mistech.tarot_circle.2':'Os ingredientes vão em §5Pedestais Rituais§r em cima do círculo, na altura da mesa: até §d8§r — quatro nos pontos cardeais (a 3 blocos da mesa) e quatro nas diagonais, sobre a obsidiana chorona.\n\nCada pedestal guarda 1 item. Coloque só o que a receita pede: itens a mais travam a mesa.',
 'guide.mistech.entry.tarot_table':'Mesa de Tarologia',
 'guide.mistech.tarot_table.1':'Mostra os §d22 Arcanos Maiores§r. A aba §6Invocação§r cria as Cartas Invertidas; a aba §6Forja§r transforma a carta de um arcano vencido em equipamento.',
 'guide.mistech.tarot_table.2':'Escolha um arcano em cima e veja embaixo o que pôr nos pedestais (verde = já está lá).\n\nClicar na mesa com uma peça de §6Oricalco Puro§r vincula um arcano derrotado a ela.',
 'guide.mistech.entry.tarot_cards':'Cartas de Tarô',
 'guide.mistech.tarot_cards.recipe':'Rende 2 cartas.',
 'guide.mistech.tarot_cards.1':'A §5Invocação§r gasta os itens dos pedestais, alguns níveis de experiência e 1 §5Carta em Branco§r, e cria a §cCarta Invertida§r do arcano.\n\nUsar a Carta Invertida abre uma §5Fenda Arcana§r à sua frente por 60 s. §dQualquer um pode entrar§r: todos que passam pela mesma fenda lutam juntos. Se ninguém entrar, ela se fecha e a carta cai de volta.',
 'guide.mistech.entry.arcane_plane':'Plano Arcano',
 'guide.mistech.arcane_plane.1':'A fenda leva a uma arena no §5Plano Arcano§r. Depois de alguns segundos o arcano surge no centro — a barra de vida dele aparece no topo da tela.\n\nLá não dá para quebrar nem colocar blocos.',
 'guide.mistech.arcane_plane.2':'Em grupo, o arcano fica mais forte (§d+50%§r de vida por jogador extra).\n\n§6Vitória§r: cai §6uma só§r Carta do Arcano (fica com quem abriu a fenda), mas todos que estão na arena contam como vencedores.\n\n§cDerrota§r: cair não mata — você volta com todos os itens e não pode voltar a essa luta. Se todos caírem, a Carta Invertida se perde.',
 'guide.mistech.entry.arcana_bond':'Vínculo com o Arcano',
 'guide.mistech.arcana_bond.1':'A §6Carta do Arcano§r, na aba Forja, vira uma de duas opções de equipamento daquele arcano.\n\nArcanos vencidos também podem ser §dvinculados§r ao equipamento de §6Oricalco Puro§r, como as constelações: clique na Mesa de Tarologia com a peça (de novo troca; agachado desfaz).',
}
EN = {
 'guide.mistech.cat.tarot':'Tarot',
 'guide.mistech.cat.tarot.desc':'Twenty-two arcana wait on the other side of the cards. Invoke, face and conquer each one.',
 'guide.mistech.entry.tarot_circle':'Tarot Circle',
 'guide.mistech.tarot_circle.legend':'Amethyst, obsidian and crying obsidian. Empty spaces can be anything.',
 'guide.mistech.tarot_circle.1':'Build this 7×7 base and place a §5Writing Table§r on the §dcrying obsidian§r in the middle.\n\nThen use the §5Guide Stone§r on the table: it becomes a §5Tarot Table§r. If a block is missing, the stone shows where.',
 'guide.mistech.tarot_circle.2':'Ingredients go on §5Ritual Pedestals§r on top of the circle, level with the table: up to §d8§r — four at the cardinal points (3 blocks from the table) and four on the diagonals, over the crying obsidian.\n\nEach pedestal holds 1 item. Only place what the recipe asks for: extra items lock the table.',
 'guide.mistech.entry.tarot_table':'Tarot Table',
 'guide.mistech.tarot_table.1':'Shows the §d22 Major Arcana§r. The §6Invocation§r tab creates Inverted Cards; the §6Forge§r tab turns the card of a defeated arcana into equipment.',
 'guide.mistech.tarot_table.2':'Pick an arcana at the top and see below what goes on the pedestals (green = already there).\n\nClicking the table with a §6Pure Orichalcum§r piece binds a defeated arcana to it.',
 'guide.mistech.entry.tarot_cards':'Tarot Cards',
 'guide.mistech.tarot_cards.recipe':'Makes 2 cards.',
 'guide.mistech.tarot_cards.1':'The §5Invocation§r consumes the pedestal items, a few experience levels and 1 §5Blank Card§r, creating the arcana\'s §cInverted Card§r.\n\nUsing the Inverted Card opens an §5Arcana Rift§r in front of you for 60 s. §dAnyone can enter§r: everyone who goes through the same rift fights together. If nobody enters, it closes and the card drops back.',
 'guide.mistech.entry.arcane_plane':'Arcane Plane',
 'guide.mistech.arcane_plane.1':'The rift leads to an arena in the §5Arcane Plane§r. After a few seconds the arcana emerges in the center — its health bar shows at the top of the screen.\n\nBlocks can\'t be broken or placed there.',
 'guide.mistech.arcane_plane.2':'In a group the arcana grows stronger (§d+50%§r health per extra player).\n\n§6Victory§r: only §6one§r Arcana Card drops (it goes to whoever opened the rift), but everyone in the arena counts as a victor.\n\n§cDefeat§r: falling doesn\'t kill you — you return with all your items and can\'t rejoin that fight. If everyone falls, the Inverted Card is lost.',
 'guide.mistech.entry.arcana_bond':'Arcana Bond',
 'guide.mistech.arcana_bond.1':'In the Forge tab, the §6Arcana Card§r becomes one of two equipment options of that arcana.\n\nDefeated arcana can also be §dbound§r to §6Pure Orichalcum§r gear, like constellations: click the Tarot Table with the piece (again to switch; sneaking removes it).',
}
for name,new in (('pt_br.json',PT),('en_us.json',EN)):
    p=os.path.join(A,'lang',name); d=json.load(open(p,encoding='utf-8')); d.update(new)
    json.dump(d,open(p,'w',encoding='utf-8'),indent=2,ensure_ascii=False)
print('ok')
