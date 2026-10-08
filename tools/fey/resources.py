"""Recursos do Incenso de Fada, Coroas da Rainha e Runa da Vida (modelos, receitas, tags, Curios, traduções, Grimório).
Rodar na raiz do projeto:  python tools/fey/resources.py  (NÃO rodar o views.py depois: ele desmancha posições feitas à mão)
(As entradas já trazem deps/tarefa/posição; o journey.py está desatualizado em relação aos capítulos novos.)"""
import json, sys
sys.path.insert(0, 'tools/pendencias2')
from res import A, D, wj, tag, lang, flat_item, recipe

# ---------- modelos ----------
for n in ('fairy_incense', 'fairy_incense_lit'):
    wj(A + 'models/item/%s.json' % n, {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mistech:item/' + n}})
wj(A + 'items/fairy_incense.json', {'model': {'type': 'minecraft:condition', 'property': 'minecraft:has_component',
    'component': 'mistech:incense_lit', 'on_true': {'type': 'minecraft:model', 'model': 'mistech:item/fairy_incense_lit'},
    'on_false': {'type': 'minecraft:model', 'model': 'mistech:item/fairy_incense'}}})
flat_item('purified_crown')
flat_item('life_rune')

# ---------- receitas ----------
recipe('fairy_incense', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['  F', ' S ', 'S  '],
    'key': {'F': 'mistech:fairy_dust', 'S': 'minecraft:stick'}, 'result': {'id': 'mistech:fairy_incense', 'count': 1}})
recipe('ritual/life_rune', {'type': 'mistech:ritual', 'catalyst': 'mistech:time_rune',
    'ingredients': ['mistech:water_rune', 'mistech:earth_rune', 'minecraft:golden_apple', '#minecraft:saplings', 'mistech:impure_magic_shard'],
    'essence': 4, 'duration': 400, 'condition': 'day', 'min_tier': 2, 'result': {'id': 'mistech:life_rune'}, 'effect': 'nature'})
recipe('sanctuary/sealed_life_rune', {'type': 'mistech:ritual', 'catalyst': 'mistech:life_rune',
    'ingredients': ['mistech:pure_magic_shard', 'mistech:mithril_ingot', 'mistech:purified_essence_bottle', 'mistech:coarse_salt'],
    'essence': 2, 'duration': 300, 'condition': 'none',
    'result': {'id': 'mistech:life_rune', 'components': {'mistech:sealed': True, 'minecraft:unbreakable': {}}},
    'effect': 'mist', 'sanctuary': True, 'copy_components': True})
recipe('sanctuary/purified_crown', {'type': 'mistech:ritual', 'catalyst': 'mistech:corrupted_crown',
    'ingredients': ['mistech:pure_magic_shard', 'mistech:pure_magic_shard', 'mistech:purified_essence_bottle', 'mistech:light_rune',
                    'mistech:coarse_salt', 'minecraft:gold_block'],
    'essence': 4, 'duration': 600, 'condition': 'day', 'result': {'id': 'mistech:purified_crown'}, 'effect': 'light', 'sanctuary': True})

# ---------- tags ----------
tag('mistech/tags/entity_type/fey_ignores', 'mistech:corrupted_goblin', 'mistech:troll', 'mistech:aberration')
tag('curios/tags/item/head', {'id': 'mistech:corrupted_crown', 'required': False}, {'id': 'mistech:purified_crown', 'required': False})
p = D + 'mistech/curios/entities/mistech_player.json'
d = json.load(open(p, encoding='utf-8'))
if 'head' not in d['slots']:
    d['slots'].append('head')
wj(p, d)

# ---------- Grimório ----------
BOOK = 'src/main/resources/assets/mistech/guide/book.json'
b = json.load(open(BOOK, encoding='utf-8'))
cats = {c['id']: c for c in b['categories']}


def put(cat, entry):
    es = cats[cat]['entries']
    es[:] = [e for e in es if e['id'] != entry['id']] + [entry]


put('incense', {'id': 'fairy_incense', 'icon': 'mistech:fairy_incense', 'name': 'guide.mistech.entry.fairy_incense',
    'deps': ['incense_holder', 'goblin_village'], 'task': {'type': 'item', 'items': ['mistech:fairy_incense']}, 'x': 4.8, 'y': 0.0, 'pages': [
    {'type': 'crafting', 'grid': ['', '', 'mistech:fairy_dust', '', 'minecraft:stick', '', 'minecraft:stick', '', ''],
     'result': 'mistech:fairy_incense', 'text': 'guide.mistech.fairy_incense.1'},
    {'type': 'text', 'text': 'guide.mistech.fairy_incense.2'},
]})
put('realm', {'id': 'goblin_crowns', 'icon': 'mistech:corrupted_crown', 'name': 'guide.mistech.entry.goblin_crowns',
    'deps': ['goblin_village'], 'task': {'type': 'item', 'items': ['mistech:corrupted_crown']}, 'x': 3.2, 'y': 2.0, 'pages': [
    {'type': 'spotlight', 'item': 'mistech:corrupted_crown', 'title': 'guide.mistech.goblin_crowns.title', 'text': 'guide.mistech.goblin_crowns.1'},
    {'type': 'text', 'text': 'guide.mistech.goblin_crowns.2'},
    {'type': 'ritual', 'sanctuary': True, 'catalyst': 'mistech:corrupted_crown', 'ingredients': ['mistech:pure_magic_shard', 'mistech:pure_magic_shard',
     'mistech:purified_essence_bottle', 'mistech:light_rune', 'mistech:coarse_salt', 'minecraft:gold_block'], 'essence': 4, 'condition': 'day',
     'result': 'mistech:purified_crown', 'title': 'guide.mistech.ritual.purified_crown', 'text': 'guide.mistech.goblin_crowns.3'},
]})
put('runes', {'id': 'life_rune', 'icon': 'mistech:life_rune', 'name': 'guide.mistech.entry.life_rune',
    'deps': ['time_rune', 'water_rune', 'earth_rune', 'altar_tier2'], 'task': {'type': 'item', 'items': ['mistech:life_rune']},
    'x': 8.96, 'y': 8.4, 'pages': [
    {'type': 'ritual', 'catalyst': 'mistech:time_rune', 'ingredients': ['mistech:water_rune', 'mistech:earth_rune', 'minecraft:golden_apple',
     '#minecraft:saplings', 'mistech:impure_magic_shard'], 'essence': 4, 'min_tier': 2, 'condition': 'day', 'result': 'mistech:life_rune',
     'title': 'guide.mistech.ritual.life_rune'},
    {'type': 'text', 'text': 'guide.mistech.life_rune.1'},
    {'type': 'text', 'text': 'guide.mistech.life_rune.2'},
]})
with open(BOOK, 'w', encoding='utf-8') as f:
    json.dump(b, f, ensure_ascii=False, indent=2)
    f.write('\n')

# ---------- traduções ----------
lang({
    'item.mistech.fairy_incense': 'Fairy Incense',
    'item.mistech.fairy_incense.lit': 'Lit Fairy Incense',
    'item.mistech.purified_crown': 'Purified Crown',
    'item.mistech.life_rune': 'Life Rune',
    'effect.mistech.fey_veil': 'Fey Veil',
    'entity.mistech.corrupted_goblin.pure': 'Freed Goblin',
    'rune_element.mistech.life': 'Life',
    'rune_socket.mistech.life': 'Killing a creature heals 1 health per rune (max 3 per second)',
    'key.mistech.crown_call': 'Queen\'s Call (crown)',
    'tooltip.mistech.accessory.crown': 'Crown — goes in the amulet slot (J)',
    'tooltip.mistech.accessory.corrupted_crown': 'Goblins, trolls and aberrations stay neutral (unless you hit them)',
    'tooltip.mistech.accessory.purified_crown': 'Goblins, trolls and aberrations stay neutral; no Taint',
    'tooltip.mistech.crown_call': '%s: Queen\'s Call — 2 allied goblins for 3 min',
    'tooltip.mistech.corrupted_crown.taint': 'Slowly taints whoever wears it',
    'message.mistech.crown_call.no_crown': 'You need a crown of the Goblin Queen equipped.',
    'message.mistech.crown_call.cooldown': 'The crown is still weak: %s:%s',
    'message.mistech.crown_call.done': 'The goblins answer the crown\'s call!',
    'message.mistech.crown_call.pure': 'Freed goblins answer your call!',
    'guide.mistech.entry.fairy_incense': 'Fairy Incense',
    'guide.mistech.fairy_incense.1': 'Incense with §dFairy Dust§r on the tip. It burns like normal incense (Incense Veil, 2 minutes).',
    'guide.mistech.fairy_incense.2': 'The smoke also gives the §dFey Veil§r: Corrupted Goblins, Trolls and Aberrations §lignore§r you, and the ones already chasing you give up. Only those you hit fight back.\n\nIn the hand it protects only you; in the §6Incense Holder§r, every player in the area.\n\n§7The Goblin Queen is not fooled.',
    'guide.mistech.entry.goblin_crowns': 'Crowns of the Queen',
    'guide.mistech.goblin_crowns.title': 'Corrupted Crown',
    'guide.mistech.goblin_crowns.1': 'Dropped by the §5Goblin Queen§r. Equip it in the §lamulet slot§r (J) or in a Curios head slot.\n\nThe creatures of the village (goblins, trolls, aberrations) become §lneutral§r: they only attack if you hit them.',
    'guide.mistech.goblin_crowns.2': '§5Queen\'s Call§r (key §lG§r): 2 Corrupted Goblins come to your side for 3 minutes. They follow you, defend you and attack monsters nearby. Once per day.\n\n§cThe corruption of the crown slowly taints whoever wears it.',
    'guide.mistech.goblin_crowns.3': 'At the §6Sanctuary§r the crown is cleansed: no more Taint, the Call recharges in half a day, and the goblins come §6freed§r (golden, without the wither touch).',
    'guide.mistech.ritual.purified_crown': 'Purify the Crown',
    'guide.mistech.entry.life_rune': 'Life Rune',
    'guide.mistech.ritual.life_rune': 'Awaken Life',
    'guide.mistech.life_rune.1': 'Rune of §alife§r (Water + Earth), born from the §bTime Rune§r at a §lTier II§r altar, by §eday§r.\n\n• Right click: heals 6 health and gives Regeneration to you and allies within 4 blocks.\n• On a crop: it grows §lall the way§r at once. On a sapling: the tree grows at once.\n• On a zombie villager: cures it. On a friendly creature: heals it.',
    'guide.mistech.life_rune.2': '§7Cigar§r: Regeneration + Absorption.\n§7Joint§r: Saturation + Health Boost.\n§7Staff§r (2,500 FE): §aFountain of Life§r, an area of 4 blocks for 6 s that heals allies and grows plants.\n§7Runic Pipe§r: each puff heals 2 health (you and allies next to you).\n§7Socketed§r: killing a creature heals 1 health per rune (max 3 per second).\n\nAffinity: §9Water§r + §6Earth§r.',
}, {
    'item.mistech.fairy_incense': 'Incenso de Fada',
    'item.mistech.fairy_incense.lit': 'Incenso de Fada Aceso',
    'item.mistech.purified_crown': 'Coroa Purificada',
    'item.mistech.life_rune': 'Runa da Vida',
    'effect.mistech.fey_veil': 'Véu Feérico',
    'entity.mistech.corrupted_goblin.pure': 'Duende Liberto',
    'rune_element.mistech.life': 'Vida',
    'rune_socket.mistech.life': 'Matar uma criatura cura 1 de vida por runa (no máx. 3 por segundo)',
    'key.mistech.crown_call': 'Chamado da Rainha (coroa)',
    'tooltip.mistech.accessory.crown': 'Coroa — vai no espaço de amuleto (J)',
    'tooltip.mistech.accessory.corrupted_crown': 'Duendes, trolls e aberrações ficam neutros (se você não bater neles)',
    'tooltip.mistech.accessory.purified_crown': 'Duendes, trolls e aberrações ficam neutros; sem Mácula',
    'tooltip.mistech.crown_call': '%s: Chamado da Rainha — 2 duendes aliados por 3 min',
    'tooltip.mistech.corrupted_crown.taint': 'Macula aos poucos quem a usa',
    'message.mistech.crown_call.no_crown': 'Você precisa de uma coroa da Rainha Duende equipada.',
    'message.mistech.crown_call.cooldown': 'A coroa ainda está fraca: %s:%s',
    'message.mistech.crown_call.done': 'Os duendes atendem ao chamado da coroa!',
    'message.mistech.crown_call.pure': 'Duendes libertos atendem ao seu chamado!',
    'guide.mistech.entry.fairy_incense': 'Incenso de Fada',
    'guide.mistech.fairy_incense.1': 'Incenso com §dPó de Fada§r na ponta. Queima como o incenso comum (Véu de Incenso, 2 minutos).',
    'guide.mistech.fairy_incense.2': 'A fumaça também dá o §dVéu Feérico§r: Duendes Corrompidos, Trolls e Aberrações §lignoram§r você, e os que já te perseguiam desistem. Só revida quem apanhar de você.\n\nNa mão protege só você; no §6Porta-Incenso§r, todos os jogadores na área.\n\n§7A Rainha Duende não se deixa enganar.',
    'guide.mistech.entry.goblin_crowns': 'Coroas da Rainha',
    'guide.mistech.goblin_crowns.title': 'Coroa Corrompida',
    'guide.mistech.goblin_crowns.1': 'Cai da §5Rainha Duende§r. Equipe no §lespaço de amuleto§r (J) ou num espaço de cabeça do Curios.\n\nAs criaturas da vila (duendes, trolls, aberrações) ficam §lneutras§r: só atacam se você bater nelas.',
    'guide.mistech.goblin_crowns.2': '§5Chamado da Rainha§r (tecla §lG§r): 2 Duendes Corrompidos vêm para o seu lado por 3 minutos. Eles te seguem, te defendem e atacam os monstros por perto. Uma vez por dia.\n\n§cA corrupção da coroa macula aos poucos quem a usa.',
    'guide.mistech.goblin_crowns.3': 'No §6Santuário§r a coroa é limpa: sem Mácula, o Chamado recarrega em meio dia e os duendes vêm §6libertos§r (dourados, sem o toque de Murchamento).',
    'guide.mistech.ritual.purified_crown': 'Purificar a Coroa',
    'guide.mistech.entry.life_rune': 'Runa da Vida',
    'guide.mistech.ritual.life_rune': 'Despertar a Vida',
    'guide.mistech.life_rune.1': 'Runa da §avida§r (Água + Terra), nascida da §bRuna do Tempo§r num altar §lTier II§r, de §edia§r.\n\n• Botão direito: cura 6 de vida e dá Regeneração a você e aos aliados a até 4 blocos.\n• Numa plantação: cresce §laté o fim§r na hora. Numa muda: a árvore nasce na hora.\n• Num aldeão zumbi: cura. Numa criatura amigável: cura.',
    'guide.mistech.life_rune.2': '§7Charuto§r: Regeneração + Absorção.\n§7Baseado§r: Saturação + Vida Extra.\n§7Cajado§r (2.500 FE): §aFonte da Vida§r, uma área de 4 blocos por 6 s que cura os aliados e faz as plantas crescerem.\n§7Cachimbo Rúnico§r: cada tragada cura 2 de vida (você e os aliados colados).\n§7Engastada§r: matar uma criatura cura 1 de vida por runa (no máx. 3 por segundo).\n\nAfinidade: §9Água§r + §6Terra§r.',
})
print('ok')

# ---------- organizações (por progressão / por dimensão): põe as entradas novas sem refazer o resto ----------
# (o views.py refaz tudo e desmancha posições feitas à mão — tarô, geradores — por isso aqui é manual)
b = json.load(open(BOOK, encoding='utf-8'))
PLACE = {  # id: (capítulo na progressão, capítulo na dimensão, entrada vizinha)
    'fairy_incense': ('p_essence', 'd_realm', 'goblin_village'),
    'goblin_crowns': ('p_essence', 'd_realm', 'goblin_village'),
    'life_rune': ('p_tier2', 'd_overworld_tier2', 'cloud_rune'),
}
for eid, chapters in PLACE.items():
    near = chapters[2]
    for view, cid in (('progress', chapters[0]), ('dimension', chapters[1])):
        for c in b['views'][view]:
            c['entries'] = [e for e in c['entries'] if e['id'] != eid]
        chap = next(c for c in b['views'][view] if c['id'] == cid)
        ref = next((e for e in chap['entries'] if e['id'] == near), None)
        x, y = (ref['x'] + 1.6, ref['y']) if ref else (max([e['x'] for e in chap['entries']] + [0]) + 1.6, 0.0)
        while any(abs(e['x'] - x) < 1.0 and abs(e['y'] - y) < 0.9 for e in chap['entries']):
            y += 1.0
        chap['entries'].append({'id': eid, 'x': round(x, 2), 'y': round(y, 2)})
with open(BOOK, 'w', encoding='utf-8') as f:
    json.dump(b, f, ensure_ascii=False, indent=2)
    f.write('\n')
print('views ok')
