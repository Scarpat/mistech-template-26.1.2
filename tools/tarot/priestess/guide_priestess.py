"""Entrada da Sacerdotisa no Grimório (categoria Tarô). Uso: python guide_priestess.py <assets/mistech>"""
import json, sys, os
A = sys.argv[1]
bp = os.path.join(A, 'guide/book.json'); b = json.load(open(bp, encoding='utf-8'))
entry = {"id": "the_high_priestess", "icon": "mistech:tarot_card_the_high_priestess", "name": "guide.mistech.entry.the_high_priestess",
         "deps": ["the_magician"], "task": {"type": "item", "items": ["mistech:tarot_card_the_high_priestess"]}, "x": 9.6, "y": 1.5, "pages": [
    {"type": "text", "text": "guide.mistech.the_high_priestess.1"},
    {"type": "text", "text": "guide.mistech.the_high_priestess.2"},
    {"type": "text", "text": "guide.mistech.the_high_priestess.3"},
    {"type": "spotlight", "item": "mistech:priestess_portal", "text": "guide.mistech.the_high_priestess.portal"},
    {"type": "spotlight", "item": "mistech:priestess_veil", "text": "guide.mistech.the_high_priestess.veil"}]}
cat = next(c for c in b['categories'] if c['id'] == 'tarot')
cat['entries'] = [e for e in cat['entries'] if e['id'] != 'the_high_priestess'] + [entry]
for view, cid in (('progress', 'p_tier3'), ('dimension', 'd_overworld_tier3')):
    for c in b['views'][view]:
        if c['id'] == cid:
            c['entries'] = [e for e in c['entries'] if e['id'] != 'the_high_priestess'] + [{"id": "the_high_priestess", "x": 12.8, "y": 3.0}]
json.dump(b, open(bp, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
PT = {
 'guide.mistech.entry.the_high_priestess': 'II · A Sacerdotisa',
 'guide.mistech.the_high_priestess.1': 'Instinto, introspecção e sabedoria. A Sacerdotisa fica no trono entre os pilares §8Boaz§r (preto) e §fJachin§r (branco) e alterna entre os dois. O topo do pilar do modo ativo acende.\n\n§8Boaz§r: você é jogado ao centro e fica §ccego§r. Ela não pode ser ferida. Seu papel é escutar e desviar.',
 'guide.mistech.the_high_priestess.2': '§6Presságios do Boaz§r\n- §bRajada Lunar§r: sussurro de um lado e brilho na borda da tela; saia da linha.\n- §cRomãs§r: anel vermelho nos pés e gotas pingando; saia do anel.\n- §9Maré§r: a água sobe; uma onda vem da borda até o centro — pule quando ela passar.\n- §3Silêncio§r: "Aquieta-te." Não se mova.\n\nCada presságio esquivado dá §dIntuição§r.',
 'guide.mistech.the_high_priestess.3': '§fJachin§r: a luz volta e surgem reflexos iguais a ela. §eSó a verdadeira tem sombra§r. Golpes mais rápidos e fracos. Estourar um reflexo solta um contra-ataque; bater na verdadeira causa +12% de dano por acúmulo de §dIntuição§r. De tempos em tempos todas trocam de lugar.\n\nAbaixo de 50% os presságios vêm em dupla; abaixo de 25% surge mais um reflexo.',
 'guide.mistech.the_high_priestess.portal': '§6O Portal§r: botão direito leva ao seu §dSantuário de Bolso§r, uma sala própria de tamanho fixo; de novo lá dentro, volta para onde você estava. A magnetita entre os pilares também leva de volta.\n\nForja: Runa do Espaço, Runa da Escuridão, olho do ender, obsidiana chorona e magnetita.',
 'guide.mistech.the_high_priestess.veil': '§6O Véu§r (pingente): invisível de verdade — armadura, item na mão e nome também somem, e os mobs quase não percebem você. Atacar rasga o véu por 3 s.\n\nForja: Amuleto em Branco, Runa da Escuridão, Runa da Água, membrana de phantom e olho de aranha fermentado.',
}
EN = {
 'guide.mistech.entry.the_high_priestess': 'II · The High Priestess',
 'guide.mistech.the_high_priestess.1': 'Instinct, introspection and wisdom. The High Priestess sits on her throne between the pillars §8Boaz§r (black) and §fJachin§r (white) and switches between the two. The active pillar lights up.\n\n§8Boaz§r: you are thrown to the center and §cblinded§r. She cannot be harmed. Your role is to listen and dodge.',
 'guide.mistech.the_high_priestess.2': '§6Omens of Boaz§r\n- §bLunar Gust§r: a whisper from one side and a glow on that screen edge; leave the line.\n- §cPomegranates§r: a red ring at your feet and dripping water; leave the ring.\n- §9Tide§r: the water rises; a wave rolls from the edge to the center — jump as it passes.\n- §3Silence§r: "Be still." Do not move.\n\nEvery dodged omen grants §dIntuition§r.',
 'guide.mistech.the_high_priestess.3': '§fJachin§r: light returns and reflections identical to her appear. §eOnly the true one casts a shadow§r. Faster, weaker attacks. Shattering a reflection triggers a counterattack; hitting the true one deals +12% damage per §dIntuition§r stack. Every so often they all change places.\n\nBelow 50% omens come in pairs; below 25% one more reflection appears.',
 'guide.mistech.the_high_priestess.portal': '§6The Portal§r: right click takes you to your §dPocket Sanctum§r, a fixed-size room of your own; use it again inside to return to where you were. The lodestone between the pillars also takes you back.\n\nForge: Space Rune, Darkness Rune, eye of ender, crying obsidian and lodestone.',
 'guide.mistech.the_high_priestess.veil': '§6The Veil§r (pendant): truly invisible — armor, held item and name vanish too, and mobs barely notice you. Attacking tears the veil for 3 s.\n\nForge: Blank Amulet, Darkness Rune, Water Rune, phantom membrane and fermented spider eye.',
}
for name, new in (('pt_br.json', PT), ('en_us.json', EN)):
    p = os.path.join(A, 'lang', name); d = json.load(open(p, encoding='utf-8')); d.update(new)
    json.dump(d, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
print('ok')
