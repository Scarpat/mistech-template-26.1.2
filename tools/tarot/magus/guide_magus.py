"""Entrada do Mago no Grimório (categoria Tarô). Uso: python guide_magus.py <assets/mistech>"""
import json, sys, os
A = sys.argv[1]
bp = os.path.join(A, 'guide/book.json'); b = json.load(open(bp, encoding='utf-8'))
entry = {"id": "the_magician", "icon": "mistech:tarot_card_the_magician", "name": "guide.mistech.entry.the_magician", "deps": ["the_fool"],
         "task": {"type": "item", "items": ["mistech:tarot_card_the_magician"]}, "x": 8.0, "y": 1.5, "pages": [
    {"type": "text", "text": "guide.mistech.the_magician.1"},
    {"type": "text", "text": "guide.mistech.the_magician.2"},
    {"type": "text", "text": "guide.mistech.the_magician.3"},
    {"type": "text", "text": "guide.mistech.the_magician.4"},
    {"type": "spotlight", "item": "mistech:magician_staff", "text": "guide.mistech.the_magician.staff"},
    {"type": "spotlight", "item": "mistech:arcane_pendant", "text": "guide.mistech.the_magician.pendant"}]}
cat = next(c for c in b['categories'] if c['id'] == 'tarot')
cat['entries'] = [e for e in cat['entries'] if e['id'] != 'the_magician'] + [entry]
for view, cid in (('progress', 'p_tier3'), ('dimension', 'd_overworld_tier3')):
    for c in b['views'][view]:
        if c['id'] == cid:
            c['entries'] = [e for e in c['entries'] if e['id'] != 'the_magician'] + [{"id": "the_magician", "x": 11.2, "y": 3.0}]
json.dump(b, open(bp, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
PT = {
 'guide.mistech.entry.the_magician': 'I · O Mago',
 'guide.mistech.the_magician.1': 'Potencial e poder. O Mago muda de forma, e §dnas primeiras ele é um monstro§r: cada forma é um elemento, com a ferramenta do naipe.\n\n§aTerra§r: colosso de pedra (quase não sente dano; 3 §aPedras Rúnicas§r o protegem). §fAr§r: harpia de lâminas, rápida. §bÁgua§r: leviatã com a taça nas costas (bolha que cura). §cFogo§r: demônio do bastão (muito dano, mas leva +50%).',
 'guide.mistech.the_magician.2': '§6Fases§r\n§eI§r Um elemento por vez — ele escolhe o que contraria você.\n§eII§r (75%) Alquimia: dois de uma vez (Magma, Vapor, Tempestade, Vida, Areia, Chama Viva).\n§eIII§r (50%) Os Quatro Aspectos, em time.\n§eIV§r (25%) A Quintessência: só no fim ele chega ao ápice, humano e enorme.',
 'guide.mistech.the_magician.3': '§6Dicas§r\n- Na Terra, quebre as Pedras Rúnicas (picareta ajuda): cada uma tira um quarto da proteção; as três o atordoam.\n- Estoure a bolha da Água rápido: ela cura e dá §dCargas§r que fortalecem o próximo modo.\n- O chão vira o elemento do modo. Tudo tem aviso no chão.\n- Na Fase III derrube a §bÁgua§r primeiro: viva, ela revive quem cair.',
 'guide.mistech.the_magician.4': '§6Assim em cima, como embaixo§r\nNa Fase IV surge um plano de vidro no alto. De tempos em tempos todos §dtrocam de plano§r; golpes vêm do outro lado (o aviso aparece no seu chão), o §eRaio do ∞§r risca um oito no chão e o chão se transmuta.\n\nCair do salão não mata: o Mago devolve você, com um pouco de dano.',
 'guide.mistech.the_magician.staff': '§6Cajado do Mago§r (energia FE): Shift + roda troca o elemento. Botão direito lança a magia, agachado usa a de apoio, e segurando 2 s solta o §eRaio do ∞§r.\n\nForja: Runa Arcana, as 4 runas dos elementos e bloco de oricalco.',
 'guide.mistech.the_magician.pendant': '§6Pingente do Arcano§r (amuleto): mais dano mágico, menos dano mágico recebido, armadura, afinidade com os quatro elementos rendendo mais e recargas menores.\n\nForja: Amuleto em Branco, Runa Arcana, Fragmento de Magia Pura, bloco de lápis e ouro.',
}
EN = {
 'guide.mistech.entry.the_magician': 'I · The Magician',
 'guide.mistech.the_magician.1': 'Potential and power. The Magician changes form, and §din the first ones he is a monster§r: each form is an element, holding its suit\'s tool.\n\n§aEarth§r: a stone colossus (barely feels damage; 3 §aRunestones§r guard him). §fAir§r: a harpy of blades, fast. §bWater§r: a leviathan with a chalice on its back (a healing bubble). §cFire§r: the demon of the wand (huge damage, but takes +50%).',
 'guide.mistech.the_magician.2': '§6Phases§r\n§eI§r One element at a time — he picks the one that counters you.\n§eII§r (75%) Alchemy: two at once (Magma, Vapor, Storm, Life, Sand, Living Flame).\n§eIII§r (50%) The Four Aspects, as a team.\n§eIV§r (25%) Quintessence: only at the end he reaches his apex, human and huge.',
 'guide.mistech.the_magician.3': '§6Tips§r\n- In Earth, break the Runestones (a pickaxe helps): each removes a quarter of his protection; all three stun him.\n- Pop Water\'s bubble fast: it heals and builds §dCharges§r that empower the next mode.\n- The floor becomes the mode\'s element. Everything is telegraphed on the ground.\n- In Phase III bring §bWater§r down first: alive, she revives the fallen.',
 'guide.mistech.the_magician.4': '§6As above, so below§r\nIn Phase IV a glass plane appears above. Every so often everyone §dswaps planes§r; strikes come from the other side (the warning shows on your floor), the §eRay of ∞§r draws an eight on the floor and the floor transmutes.\n\nFalling out of the hall won\'t kill you: the Magician brings you back, with a little damage.',
 'guide.mistech.the_magician.staff': '§6Magician\'s Staff§r (FE energy): Shift + wheel changes the element. Right click casts its spell, sneaking casts the support one, and holding 2 s releases the §eRay of ∞§r.\n\nForge: Arcane Rune, the 4 element runes and an orichalcum block.',
 'guide.mistech.the_magician.pendant': '§6Pendant of the Arcane§r (amulet): more magic damage, less magic damage taken, armor, better affinity gains for the four elements and shorter cooldowns.\n\nForge: Blank Amulet, Arcane Rune, Pure Magic Shard, lapis block and gold.',
}
for name, new in (('pt_br.json', PT), ('en_us.json', EN)):
    p = os.path.join(A, 'lang', name); d = json.load(open(p, encoding='utf-8')); d.update(new)
    json.dump(d, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
print('ok')
