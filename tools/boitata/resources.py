"""Recursos da Armadura do Boitatá e do Ovo do Boitatá (montaria).
Rodar na raiz do projeto:  python tools/boitata/resources.py   (NÃO rodar o views.py depois)."""
import json, sys
sys.path.insert(0, 'tools/pendencias2')
from res import A, D, wj, tag, lang, recipe, self_loot, flat_item

PIECES = ['helmet', 'chestplate', 'leggings', 'boots']
SHAPES = {'helmet': ['SSS', 'S S'], 'chestplate': ['S S', 'SES', 'SSS'], 'leggings': ['SSS', 'S S', 'S S'], 'boots': ['S S', 'S S']}

# ---------- armadura ----------
wj(A + 'equipment/boitata.json', {'layers': {'humanoid': [{'texture': 'mistech:boitata'}],
                                             'humanoid_leggings': [{'texture': 'mistech:boitata'}]}})
for piece in PIECES:
    flat_item('boitata_' + piece)
    key = {'S': 'mistech:boitata_scale'}
    if piece == 'chestplate':
        key['E'] = 'mistech:boitata_eye'
    recipe('boitata_' + piece, {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': SHAPES[piece], 'key': key,
        'result': {'id': 'mistech:boitata_' + piece}})
tag('mistech/tags/item/boitata_scales', 'mistech:boitata_scale')
for piece, slot in (('helmet', 'head'), ('chestplate', 'chest'), ('leggings', 'leg'), ('boots', 'foot')):
    tag('minecraft/tags/item/%s_armor' % slot, 'mistech:boitata_' + piece)

# ---------- ovo ----------
EGG_BOXES = [([5, 0, 5], [11, 1, 11]), ([4, 1, 4], [12, 8, 12]), ([4.5, 8, 4.5], [11.5, 10, 11.5]), ([5.5, 10, 5.5], [10.5, 11.5, 10.5]),
             ([6.5, 11.5, 6.5], [9.5, 12.5, 9.5])]


def egg_el(frm, to):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    side = [x0, 16 - y1, x1, 16 - y0]
    faces = {d: {'texture': '#shell', 'uv': side} for d in ('north', 'south', 'east', 'west')}
    faces['up'] = {'texture': '#shell', 'uv': [x0, z0, x1, z1]}
    faces['down'] = {'texture': '#shell', 'uv': [x0, z0, x1, z1]}
    return {'from': frm, 'to': to, 'faces': faces}


wj(A + 'models/block/boitata_egg.json', {'parent': 'minecraft:block/block', 'textures': {
    'particle': 'mistech:block/boitata_egg', 'shell': 'mistech:block/boitata_egg'}, 'elements': [egg_el(f, t) for f, t in EGG_BOXES]})
wj(A + 'blockstates/boitata_egg.json', {'variants': {'': {'model': 'mistech:block/boitata_egg'}}})
flat_item('boitata_egg')
self_loot('boitata_egg')
EGG = ['mistech:boitata_scale', 'mistech:boitata_scale', 'mistech:boitata_scale', 'mistech:boitata_scale', 'mistech:time_rune',
       'mistech:fire_rune', 'minecraft:magma_block', 'mistech:lunar_blood']
recipe('ritual/boitata_egg', {'type': 'mistech:ritual', 'catalyst': 'mistech:boitata_eye', 'ingredients': EGG, 'essence': 8, 'duration': 600,
    'condition': 'night', 'min_tier': 3, 'result': {'id': 'mistech:boitata_egg'}, 'effect': 'fire'})

# ---------- Grimório ----------
BOOK = 'src/main/resources/assets/mistech/guide/book.json'
b = json.load(open(BOOK, encoding='utf-8'))
cats = {c['id']: c for c in b['categories']}


def free_spot(entries, x, y):
    while any(abs(e.get('x', -99) - x) < 1.0 and abs(e.get('y', -99) - y) < 0.9 for e in entries):
        y += 1.0
    return round(x, 2), round(y, 2)


def put(cat, near, entry):
    es = cats[cat]['entries']
    es[:] = [e for e in es if e['id'] != entry['id']]
    ref = next(e for c in b['categories'] for e in c['entries'] if e['id'] == near)
    same = any(e['id'] == near for e in es)
    x, y = (ref['x'] + 1.6, ref['y']) if same else (max(e['x'] for e in es) + 1.6, 0.0)
    entry['x'], entry['y'] = free_spot(es, x, y)
    es.append(entry)


def grid(piece):
    rows = SHAPES[piece] + ['   '] * (3 - len(SHAPES[piece]))
    return ['mistech:boitata_scale' if ch == 'S' else 'mistech:boitata_eye' if ch == 'E' else '' for row in rows for ch in row]


put('folklore', 'boitata', {'id': 'boitata_armor', 'icon': 'mistech:boitata_chestplate', 'name': 'guide.mistech.entry.boitata_armor',
    'deps': ['boitata'], 'task': {'type': 'item', 'items': ['mistech:boitata_chestplate']}, 'pages': [
    {'type': 'crafting', 'grid': grid('chestplate'), 'result': 'mistech:boitata_chestplate', 'text': 'guide.mistech.boitata_armor.1'},
    {'type': 'text', 'text': 'guide.mistech.boitata_armor.2'},
    {'type': 'text', 'text': 'guide.mistech.boitata_armor.3'}]})
put('folklore', 'boitata_armor', {'id': 'boitata_egg', 'icon': 'mistech:boitata_egg', 'name': 'guide.mistech.entry.boitata_egg',
    'deps': ['boitata', 'altar_tier3'], 'task': {'type': 'item', 'items': ['mistech:boitata_egg']}, 'pages': [
    {'type': 'ritual', 'catalyst': 'mistech:boitata_eye', 'ingredients': EGG, 'essence': 8, 'min_tier': 3, 'condition': 'night',
     'result': 'mistech:boitata_egg', 'title': 'guide.mistech.ritual.boitata_egg'},
    {'type': 'text', 'text': 'guide.mistech.boitata_egg.1'},
    {'type': 'text', 'text': 'guide.mistech.boitata_egg.2'},
    {'type': 'text', 'text': 'guide.mistech.boitata_egg.3'}]})
PLACE = {'boitata_armor': 'boitata', 'boitata_egg': 'boitata_armor'}
for eid, near in PLACE.items():
    for view in ('progress', 'dimension'):
        chs = b['views'][view]
        for c in chs:
            c['entries'] = [e for e in c['entries'] if e['id'] != eid]
        chap = next((c for c in chs if any(e['id'] == near for e in c['entries'])), chs[-1])
        ref = next((e for e in chap['entries'] if e['id'] == near), chap['entries'][0])
        x, y = free_spot(chap['entries'], ref['x'] + 1.6, ref['y'])
        chap['entries'].append({'id': eid, 'x': x, 'y': y})
with open(BOOK, 'w', encoding='utf-8') as f:
    json.dump(b, f, ensure_ascii=False, indent=2)
    f.write('\n')

# ---------- traduções ----------
lang({
    'item.mistech.boitata_helmet': 'Boitatá Helm', 'item.mistech.boitata_chestplate': 'Boitatá Chestplate',
    'item.mistech.boitata_leggings': 'Boitatá Leggings', 'item.mistech.boitata_boots': 'Boitatá Boots',
    'item.mistech.boitata_armor.helmet': 'Sees inside lava',
    'item.mistech.boitata_armor.chestplate': 'Immune to fire; attackers catch fire',
    'item.mistech.boitata_armor.leggings': 'Sprinting leaves a trail of fire',
    'item.mistech.boitata_armor.boots': 'Walk on lava (sneak to sink)',
    'item.mistech.boitata_armor.set': 'Full set — Serpent Form: fire heals, swim fast in lava, Fire Breath (R)',
    'block.mistech.boitata_egg': 'Boitatá Egg',
    'entity.mistech.boitata_mount': 'Boitatá',
    'key.mistech.fire_breath': 'Fire Breath (Boitatá)',
    'message.mistech.fire_breath.cooldown': 'Fire Breath ready in %s s',
    'message.mistech.boitata_egg.warm': 'The egg is warm · %s%%',
    'message.mistech.boitata_egg.cold': 'The egg is cold — it needs lava, fire or magma beside or below · %s%%',
    'message.mistech.boitata_egg.hatched': 'A Boitatá hatched! It follows you now.',
    'message.mistech.boitata_mount.stage.0': 'hatchling', 'message.mistech.boitata_mount.stage.1': 'young',
    'message.mistech.boitata_mount.stage.2': 'adult',
    'message.mistech.boitata_mount.fed': 'Boitatá (%s) · %s%%',
    'message.mistech.boitata_mount.stay': 'The Boitatá stays here', 'message.mistech.boitata_mount.follow': 'The Boitatá follows you',
    'message.mistech.boitata_mount.too_young': 'Too young to breathe fire',
    'message.mistech.boitata_mount.grew.1': 'Your Boitatá grew! Now it can be ridden.',
    'message.mistech.boitata_mount.grew.2': 'Your Boitatá is an adult! It flies and breathes fire (R).',
    'guide.mistech.entry.boitata_armor': 'Boitatá Armor',
    'guide.mistech.boitata_armor.1': 'Armor made only from §6Boitatá Scales§r (helm 5, leggings 7, boots 4); the chestplate carries the §eBoitatá Eye§r in the center. Tougher than netherite, does not burn and is repaired with scales.',
    'guide.mistech.boitata_armor.2': 'Per piece:\n§6Helm§r: sees inside lava.\n§6Chestplate§r: immune to fire; whoever hits you catches fire.\n§6Leggings§r: sprinting leaves a trail of fire that burns others (not you or your pets).\n§6Boots§r: walk on lava (sneak to sink) and magma does not hurt.',
    'guide.mistech.boitata_armor.3': '§cSerpent Form§r — the four pieces: fire heals instead of hurting, you swim fast in lava and the §lR§r key breathes a cone of fire (once per minute).',
    'guide.mistech.entry.boitata_egg': 'Boitatá Egg',
    'guide.mistech.ritual.boitata_egg': 'Boitatá Egg',
    'guide.mistech.boitata_egg.1': 'The Eye awakens an egg. As many as you want — one Eye each. Place it §lnext to heat§r (lava, fire, magma or a lit campfire, below or beside): it hatches in §l3 days§r. Right click shows how it is going.\n\n§dLife Rune§r on the egg: +10%. The §dPocket Hourglass§r nearby: 2× faster.',
    'guide.mistech.boitata_egg.2': 'The hatchling is yours. It grows with time and faster if fed (cooked meat, blaze powder, magma cream, blaze rod):\n§7Hatchling§r follows you.\n§6Young§r (1 day): ground mount, walks on lava.\n§cAdult§r (+2 days): flies when ridden.',
    'guide.mistech.boitata_egg.3': 'Riding: empty hand. Adult: look where to go, jump climbs, §lR§r breathes fire. Sneak + right click: stay/follow. It fights for you and protects the rider from fire.\n\n§cIf it dies, it does not come back.§r',
}, {
    'item.mistech.boitata_helmet': 'Elmo do Boitatá', 'item.mistech.boitata_chestplate': 'Peitoral do Boitatá',
    'item.mistech.boitata_leggings': 'Calças do Boitatá', 'item.mistech.boitata_boots': 'Botas do Boitatá',
    'item.mistech.boitata_armor.helmet': 'Enxerga dentro da lava',
    'item.mistech.boitata_armor.chestplate': 'Imune a fogo; quem bate pega fogo',
    'item.mistech.boitata_armor.leggings': 'Correndo, deixa um rastro de fogo',
    'item.mistech.boitata_armor.boots': 'Anda sobre lava (agachar afunda)',
    'item.mistech.boitata_armor.set': 'Conjunto — Forma de Serpente: fogo cura, nada rápido na lava, Sopro de Fogo (R)',
    'block.mistech.boitata_egg': 'Ovo do Boitatá',
    'entity.mistech.boitata_mount': 'Boitatá',
    'key.mistech.fire_breath': 'Sopro de Fogo (Boitatá)',
    'message.mistech.fire_breath.cooldown': 'Sopro de Fogo pronto em %s s',
    'message.mistech.boitata_egg.warm': 'O ovo está quente · %s%%',
    'message.mistech.boitata_egg.cold': 'O ovo está frio — precisa de lava, fogo ou magma do lado ou embaixo · %s%%',
    'message.mistech.boitata_egg.hatched': 'Um Boitatá nasceu! Agora ele segue você.',
    'message.mistech.boitata_mount.stage.0': 'filhote', 'message.mistech.boitata_mount.stage.1': 'jovem',
    'message.mistech.boitata_mount.stage.2': 'adulto',
    'message.mistech.boitata_mount.fed': 'Boitatá (%s) · %s%%',
    'message.mistech.boitata_mount.stay': 'O Boitatá fica aqui', 'message.mistech.boitata_mount.follow': 'O Boitatá segue você',
    'message.mistech.boitata_mount.too_young': 'Novo demais para cuspir fogo',
    'message.mistech.boitata_mount.grew.1': 'Seu Boitatá cresceu! Agora dá para montar.',
    'message.mistech.boitata_mount.grew.2': 'Seu Boitatá virou adulto! Voa e cospe fogo (R).',
    'guide.mistech.entry.boitata_armor': 'Armadura do Boitatá',
    'guide.mistech.boitata_armor.1': 'Armadura feita só de §6Escamas do Boitatá§r (elmo 5, calças 7, botas 4); o peitoral leva o §eOlho do Boitatá§r no centro. Mais resistente que netherite, não queima e se conserta com escamas.',
    'guide.mistech.boitata_armor.2': 'Por peça:\n§6Elmo§r: enxerga dentro da lava.\n§6Peitoral§r: imune a fogo; quem bate em você pega fogo.\n§6Calças§r: correndo, deixa um rastro de fogo que queima os outros (não você nem seus bichos).\n§6Botas§r: anda sobre lava (agachar afunda) e o magma não machuca.',
    'guide.mistech.boitata_armor.3': '§cForma de Serpente§r — as quatro peças: o fogo cura em vez de ferir, você nada rápido na lava e a tecla §lR§r solta um cone de fogo (uma vez por minuto).',
    'guide.mistech.entry.boitata_egg': 'Ovo do Boitatá',
    'guide.mistech.ritual.boitata_egg': 'Ovo do Boitatá',
    'guide.mistech.boitata_egg.1': 'O Olho desperta um ovo. Quantos quiser — um Olho por ovo. Ponha §lperto de calor§r (lava, fogo, magma ou fogueira acesa, embaixo ou do lado): choca em §l3 dias§r. Botão direito mostra como está.\n\n§dRuna da Vida§r no ovo: +10%. A §dAmpulheta de Bolso§r por perto: 2× mais rápido.',
    'guide.mistech.boitata_egg.2': 'O filhote é seu. Cresce com o tempo e mais rápido comendo (carne assada, pó de blaze, creme de magma, vara de blaze):\n§7Filhote§r segue você.\n§6Jovem§r (1 dia): montaria de chão, anda sobre lava.\n§cAdulto§r (+2 dias): voa montado.',
    'guide.mistech.boitata_egg.3': 'Montar: mão vazia. Adulto: olhe para onde quer ir, pulo sobe, §lR§r cospe fogo. Agachar + botão direito: fica/segue. Ele luta por você e protege quem monta do fogo.\n\n§cSe morrer, não volta.§r',
})
print('ok')

# ================= v2 (12/10): armadura de montaria, apito, sopro contínuo =================
for n in ('boitata_mount_armor_scale', 'boitata_mount_armor_orichalcum', 'boitata_whistle'):
    flat_item(n)
recipe('boitata_mount_armor_scale', {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': ['S S', 'SLS', 'S S'],
    'key': {'S': 'mistech:boitata_scale', 'L': 'minecraft:leather'}, 'result': {'id': 'mistech:boitata_mount_armor_scale'}})
ORI = ['mistech:orichalcum_ingot', 'mistech:orichalcum_ingot', 'mistech:orichalcum_ingot', 'mistech:orichalcum_ingot', 'mistech:fire_rune',
       'mistech:earth_rune']
recipe('ritual/boitata_mount_armor_orichalcum', {'type': 'mistech:ritual', 'catalyst': 'mistech:boitata_mount_armor_scale', 'ingredients': ORI,
    'essence': 5, 'duration': 400, 'condition': 'none', 'min_tier': 2, 'result': {'id': 'mistech:boitata_mount_armor_orichalcum'},
    'effect': 'forge'})
recipe('boitata_whistle', {'type': 'minecraft:crafting_shapeless', 'category': 'tools',
    'ingredients': ['minecraft:bone', 'minecraft:gold_ingot', 'mistech:boitata_scale'], 'result': {'id': 'mistech:boitata_whistle'}})

b = json.load(open(BOOK, encoding='utf-8'))
cats = {c['id']: c for c in b['categories']}
put('folklore', 'boitata_egg', {'id': 'boitata_mount_gear', 'icon': 'mistech:boitata_whistle', 'name': 'guide.mistech.entry.boitata_mount_gear',
    'deps': ['boitata_egg'], 'task': {'type': 'item', 'items': ['mistech:boitata_whistle']}, 'pages': [
    {'type': 'crafting', 'grid': ['minecraft:bone', 'minecraft:gold_ingot', 'mistech:boitata_scale', '', '', '', '', '', ''],
     'result': 'mistech:boitata_whistle', 'shapeless': True, 'text': 'guide.mistech.boitata_mount_gear.1'},
    {'type': 'crafting', 'grid': ['mistech:boitata_scale', '', 'mistech:boitata_scale', 'mistech:boitata_scale', 'minecraft:leather',
     'mistech:boitata_scale', 'mistech:boitata_scale', '', 'mistech:boitata_scale'], 'result': 'mistech:boitata_mount_armor_scale',
     'text': 'guide.mistech.boitata_mount_gear.2'},
    {'type': 'ritual', 'catalyst': 'mistech:boitata_mount_armor_scale', 'ingredients': ORI, 'essence': 5, 'min_tier': 2,
     'result': 'mistech:boitata_mount_armor_orichalcum', 'title': 'guide.mistech.ritual.boitata_mount_armor_orichalcum'},
    {'type': 'text', 'text': 'guide.mistech.boitata_mount_gear.3'}]})
for view in ('progress', 'dimension'):
    chs = b['views'][view]
    for c in chs:
        c['entries'] = [e for e in c['entries'] if e['id'] != 'boitata_mount_gear']
    chap = next((c for c in chs if any(e['id'] == 'boitata_egg' for e in c['entries'])), chs[-1])
    ref = next(e for e in chap['entries'] if e['id'] == 'boitata_egg')
    x, y = free_spot(chap['entries'], ref['x'] + 1.6, ref['y'])
    chap['entries'].append({'id': 'boitata_mount_gear', 'x': x, 'y': y})
with open(BOOK, 'w', encoding='utf-8') as f:
    json.dump(b, f, ensure_ascii=False, indent=2)
    f.write('\n')

lang({
    'item.mistech.boitata_mount_armor_scale': 'Boitatá Scale Barding', 'item.mistech.boitata_mount_armor_orichalcum': 'Orichalcum Boitatá Barding',
    'item.mistech.boitata_mount_armor.stats': '+%s armor, +%s toughness',
    'item.mistech.boitata_mount_armor.orichalcum': 'Fire breath +25% damage',
    'item.mistech.boitata_mount_armor.help': 'Right click your Boitatá to put it on · shears take it off',
    'item.mistech.boitata_whistle': 'Boitatá Whistle',
    'item.mistech.boitata_whistle.selected': 'Order: %s',
    'item.mistech.boitata_whistle.used': '%s — %s Boitatá(s) heard',
    'item.mistech.boitata_whistle.help': 'Sneak + right click: change order · Right click: whistle',
    'item.mistech.boitata_whistle.mode.0': 'Follow', 'item.mistech.boitata_whistle.mode.1': 'Stay',
    'item.mistech.boitata_whistle.mode.2': 'Wander', 'item.mistech.boitata_whistle.mode.3': 'Call',
    'message.mistech.boitata_mount.command.0': 'The Boitatá follows you', 'message.mistech.boitata_mount.command.1': 'The Boitatá stays here',
    'message.mistech.boitata_mount.command.2': 'The Boitatá wanders around',
    'message.mistech.boitata_mount.out_of_breath': 'The Boitatá is out of breath',
    'key.mistech.fire_breath': 'Fire Breath (hold, Boitatá)',
    'guide.mistech.entry.boitata_mount_gear': 'Whistle and Barding',
    'guide.mistech.boitata_mount_gear.1': '§lBoitatá Whistle§r: sneak + right click picks the order (Follow, Stay, Wander, Call); right click whistles and every Boitatá of yours within 96 blocks obeys. §lCall§r brings the far ones (up to 512 blocks) to your side.',
    'guide.mistech.boitata_mount_gear.2': '§lScale Barding§r: +10 armor, +3 toughness. Right click your Boitatá to put it on; shears take it off.',
    'guide.mistech.ritual.boitata_mount_armor_orichalcum': 'Orichalcum Barding',
    'guide.mistech.boitata_mount_gear.3': '§6Orichalcum Barding§r: +16 armor, +5 toughness and the breath burns 25% more.\n\n§cFire Breath§r: ridden as an adult, §lhold R§r for a continuous jet of fire (14 blocks) that sets the ground on fire (if mob griefing is on). Breath lasts 10 s and recovers when it stops.',
}, {
    'item.mistech.boitata_mount_armor_scale': 'Armadura de Escamas do Boitatá', 'item.mistech.boitata_mount_armor_orichalcum': 'Armadura de Oricalco do Boitatá',
    'item.mistech.boitata_mount_armor.stats': '+%s de armadura, +%s de resistência',
    'item.mistech.boitata_mount_armor.orichalcum': 'Sopro de fogo +25% de dano',
    'item.mistech.boitata_mount_armor.help': 'Botão direito no seu Boitatá veste · tesoura tira',
    'item.mistech.boitata_whistle': 'Apito do Boitatá',
    'item.mistech.boitata_whistle.selected': 'Ordem: %s',
    'item.mistech.boitata_whistle.used': '%s — %s Boitatá(s) ouviram',
    'item.mistech.boitata_whistle.help': 'Agachar + botão direito: troca a ordem · Botão direito: apita',
    'item.mistech.boitata_whistle.mode.0': 'Seguir', 'item.mistech.boitata_whistle.mode.1': 'Ficar',
    'item.mistech.boitata_whistle.mode.2': 'Vagar', 'item.mistech.boitata_whistle.mode.3': 'Chamar',
    'message.mistech.boitata_mount.command.0': 'O Boitatá segue você', 'message.mistech.boitata_mount.command.1': 'O Boitatá fica aqui',
    'message.mistech.boitata_mount.command.2': 'O Boitatá anda por aí',
    'message.mistech.boitata_mount.out_of_breath': 'O Boitatá está sem fôlego',
    'key.mistech.fire_breath': 'Sopro de Fogo (segurar, Boitatá)',
    'guide.mistech.entry.boitata_mount_gear': 'Apito e Armadura',
    'guide.mistech.boitata_mount_gear.1': '§lApito do Boitatá§r: agachar + botão direito escolhe a ordem (Seguir, Ficar, Vagar, Chamar); botão direito apita e todos os seus Boitatás a até 96 blocos obedecem. §lChamar§r traz para perto os que estão longe (até 512 blocos).',
    'guide.mistech.boitata_mount_gear.2': '§lArmadura de Escamas§r: +10 de armadura, +3 de resistência. Botão direito no seu Boitatá veste; tesoura tira.',
    'guide.mistech.ritual.boitata_mount_armor_orichalcum': 'Armadura de Oricalco',
    'guide.mistech.boitata_mount_gear.3': '§6Armadura de Oricalco§r: +16 de armadura, +5 de resistência e o sopro queima 25% mais.\n\n§cSopro de Fogo§r: montado no adulto, §lsegure R§r para um jato de fogo contínuo (14 blocos) que põe fogo no chão (se o mobGriefing estiver ligado). O fôlego dura 10 s e volta quando ele para.',
})
print('ok v2')
