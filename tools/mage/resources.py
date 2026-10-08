"""Recursos das Vestes de Mago, Tear Mágico e Pêndulo Localizador (modelos, receitas, tags, traduções, Grimório).
Rodar na raiz do projeto:  python tools/mage/resources.py   (NÃO rodar o views.py depois)."""
import json, shutil, sys
sys.path.insert(0, 'tools/pendencias2')
from res import A, D, wj, tag, lang, recipe, self_loot

TIERS = [('hemp', 0x9C8A5A), ('initiate', 0x2E4F9C), ('adept', 0x6A2E8C), ('archmage', 0x2A1C48)]
PIECES = [('hood', 'helmet'), ('robe', 'chestplate'), ('pants', 'leggings'), ('shoes', 'boots')]


def argb(c):
    return c - (1 << 32) if c | 0xFF000000 else c


def signed(c):
    v = 0xFF000000 | c
    return v - (1 << 32)


# ---------- vestes: equipamento e modelos (texturas em tools/mage/robe_design.py) ----------
PIECE_ICON = {'hood': 'hood', 'robe': 'robe', 'pants': 'pants', 'shoes': 'shoes'}
for tier, color in TIERS:
    wj(A + 'equipment/mage_%s.json' % tier, {'layers': {
        'humanoid': [{'texture': 'mistech:mage_robe_base', 'dyeable': {'color_when_undyed': signed(color)}},
                     {'texture': 'mistech:mage_%s_trim' % tier}],
        'humanoid_leggings': [{'texture': 'mistech:mage_pants_base', 'dyeable': {'color_when_undyed': signed(color)}},
                              {'texture': 'mistech:mage_%s_trim' % tier}]}})
    for piece, van in PIECES:
        name = '%s_mage_%s' % (tier, piece)
        wj(A + 'models/item/%s.json' % name, {'parent': 'minecraft:item/generated',
            'textures': {'layer0': 'mistech:item/mage_%s_base' % piece, 'layer1': 'mistech:item/mage_%s_%s_trim' % (tier, piece)}})
        wj(A + 'items/%s.json' % name, {'model': {'type': 'minecraft:model', 'model': 'mistech:item/' + name,
            'tints': [{'type': 'minecraft:dye', 'default': signed(color)}]}})

# ---------- Tear Mágico ----------
wj(A + 'models/block/magic_loom.json', {'parent': 'minecraft:block/orientable_with_bottom', 'textures': {
    'bottom': 'mistech:block/magic_loom_bottom', 'front': 'mistech:block/magic_loom_front',
    'side': 'mistech:block/magic_loom_side', 'top': 'mistech:block/magic_loom_top'}})
variants = {}
for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
    for lit in ('false', 'true'):
        v = {'model': 'mistech:block/magic_loom'}
        if rot:
            v['y'] = rot
        variants['facing=%s,lit=%s' % (facing, lit)] = v
wj(A + 'blockstates/magic_loom.json', {'variants': variants})
wj(A + 'items/magic_loom.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:block/magic_loom'}})
self_loot('magic_loom')
shutil.copyfile(A + 'textures/gui/container/runic_crusher.png', A + 'textures/gui/container/magic_loom.png')
wj(A + 'models/item/magic_cloth.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mistech:item/magic_cloth'}})
wj(A + 'items/magic_cloth.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:item/magic_cloth'}})

# ---------- Pêndulo Localizador ----------
for n in ('seeking_pendulum', 'seeking_pendulum_found'):
    wj(A + 'models/item/%s.json' % n, {'parent': 'minecraft:item/handheld', 'textures': {'layer0': 'mistech:item/' + n}})
wj(A + 'items/seeking_pendulum.json', {'model': {'type': 'minecraft:condition', 'property': 'minecraft:has_component',
    'component': 'mistech:seek_target', 'on_true': {'type': 'minecraft:model', 'model': 'mistech:item/seeking_pendulum_found'},
    'on_false': {'type': 'minecraft:model', 'model': 'mistech:item/seeking_pendulum'}}})

# ---------- receitas ----------
recipe('magic_loom', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['ARA', 'FLF', 'CCC'],
    'key': {'A': 'minecraft:amethyst_shard', 'R': 'mistech:energy_rune', 'F': 'mistech:hemp_fiber', 'L': 'minecraft:loom',
            'C': 'minecraft:copper_ingot'}, 'result': {'id': 'mistech:magic_loom'}})
SHAPES = {'hood': ['CCC', 'C C'], 'robe': ['C C', 'CCC', 'CCC'], 'pants': ['CCC', 'C C', 'C C'], 'shoes': ['C C', 'C C']}
for piece, _ in PIECES:
    recipe('hemp_mage_' + piece, {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': SHAPES[piece],
        'key': {'C': 'mistech:magic_cloth'}, 'result': {'id': 'mistech:hemp_mage_' + piece}})
    recipe('ritual/initiate_mage_' + piece, {'type': 'mistech:ritual', 'catalyst': 'mistech:hemp_mage_' + piece,
        'ingredients': ['minecraft:gold_ingot', 'minecraft:gold_ingot', 'minecraft:lapis_lazuli', 'mistech:impure_magic_shard'],
        'essence': 3, 'duration': 300, 'condition': 'none', 'min_tier': 1, 'result': {'id': 'mistech:initiate_mage_' + piece},
        'copy_components': True, 'effect': 'arcane'})
    recipe('ritual/adept_mage_' + piece, {'type': 'mistech:ritual', 'catalyst': 'mistech:initiate_mage_' + piece,
        'ingredients': ['mistech:magic_cloth', 'mistech:magic_cloth', 'minecraft:amethyst_block', 'mistech:impure_magic_block',
                        'mistech:arcane_rune'], 'essence': 4, 'duration': 400, 'condition': 'none', 'min_tier': 2,
        'result': {'id': 'mistech:adept_mage_' + piece}, 'copy_components': True, 'effect': 'arcane'})
    recipe('ritual/archmage_mage_' + piece, {'type': 'mistech:ritual', 'catalyst': 'mistech:adept_mage_' + piece,
        'ingredients': ['mistech:magic_cloth', 'mistech:magic_cloth', 'mistech:star_fragment', 'mistech:arcane_rune',
                        'mistech:space_rune', 'mistech:time_rune'], 'essence': 6, 'duration': 600, 'condition': 'night', 'min_tier': 3,
        'result': {'id': 'mistech:archmage_mage_' + piece}, 'copy_components': True, 'effect': 'astral'})
recipe('seeking_pendulum', {'type': 'minecraft:crafting_shapeless', 'category': 'tools',
    'ingredients': ['mistech:dowsing_pendulum', 'minecraft:compass', 'mistech:fairy_dust'], 'result': {'id': 'mistech:seeking_pendulum'}})

# ---------- tags ----------
ROBES = ['mistech:%s_mage_%s' % (t, p) for t, _ in TIERS for p, _ in PIECES]
tag('minecraft/tags/item/dyeable', *ROBES)
for piece, slot in (('hood', 'head'), ('robe', 'chest'), ('pants', 'leg'), ('shoes', 'foot')):
    tag('minecraft/tags/item/%s_armor' % slot, *['mistech:%s_mage_%s' % (t, piece) for t, _ in TIERS])
tag('mistech/tags/item/magic_cloth', 'mistech:magic_cloth')
tag('minecraft/tags/block/mineable/axe', 'mistech:magic_loom')
tag('mistech/tags/worldgen/structure/seek/goblin_village', 'mistech:goblin_village')
tag('mistech/tags/worldgen/structure/seek/ancient_city', 'minecraft:ancient_city')
tag('mistech/tags/worldgen/structure/seek/nether_fortress', 'minecraft:fortress')

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


def grid(piece, item):
    rows = SHAPES[piece] + ['   '] * (3 - len(SHAPES[piece]))
    return [item if ch == 'C' else '' for row in rows for ch in row]


put('wear', 'slippers', {'id': 'magic_loom', 'icon': 'mistech:magic_loom', 'name': 'guide.mistech.entry.magic_loom',
    'deps': ['crops', 'energy_rune'], 'task': {'type': 'item', 'items': ['mistech:magic_cloth']}, 'pages': [
    {'type': 'crafting', 'grid': ['minecraft:amethyst_shard', 'mistech:energy_rune', 'minecraft:amethyst_shard', 'mistech:hemp_fiber',
     'minecraft:loom', 'mistech:hemp_fiber', 'minecraft:copper_ingot', 'minecraft:copper_ingot', 'minecraft:copper_ingot'],
     'result': 'mistech:magic_loom', 'text': 'guide.mistech.magic_loom.1'},
    {'type': 'spotlight', 'item': 'mistech:magic_cloth', 'text': 'guide.mistech.magic_loom.2'}]})
put('wear', 'magic_loom', {'id': 'mage_robes', 'icon': 'mistech:adept_mage_robe', 'name': 'guide.mistech.entry.mage_robes',
    'deps': ['magic_loom'], 'task': {'type': 'item', 'items': ['mistech:hemp_mage_robe']}, 'pages': [
    {'type': 'text', 'text': 'guide.mistech.mage_robes.1'},
    {'type': 'crafting', 'grid': grid('robe', 'mistech:magic_cloth'), 'result': 'mistech:hemp_mage_robe', 'text': 'guide.mistech.mage_robes.2'},
    {'type': 'ritual', 'catalyst': 'mistech:hemp_mage_robe', 'ingredients': ['minecraft:gold_ingot', 'minecraft:gold_ingot', 'minecraft:lapis_lazuli',
     'mistech:impure_magic_shard'], 'essence': 3, 'result': 'mistech:initiate_mage_robe', 'title': 'guide.mistech.ritual.initiate_robes'},
    {'type': 'ritual', 'catalyst': 'mistech:initiate_mage_robe', 'ingredients': ['mistech:magic_cloth', 'mistech:magic_cloth', 'minecraft:amethyst_block',
     'mistech:impure_magic_block', 'mistech:arcane_rune'], 'essence': 4, 'min_tier': 2, 'result': 'mistech:adept_mage_robe',
     'title': 'guide.mistech.ritual.adept_robes'},
    {'type': 'ritual', 'catalyst': 'mistech:adept_mage_robe', 'ingredients': ['mistech:magic_cloth', 'mistech:magic_cloth', 'mistech:star_fragment',
     'mistech:arcane_rune', 'mistech:space_rune', 'mistech:time_rune'], 'essence': 6, 'min_tier': 3, 'condition': 'night',
     'result': 'mistech:archmage_mage_robe', 'title': 'guide.mistech.ritual.archmage_robes'},
    {'type': 'text', 'text': 'guide.mistech.mage_robes.3'},
    {'type': 'text', 'text': 'guide.mistech.mage_robes.4'}]})
put('purity', 'dowsing_pendulum', {'id': 'seeking_pendulum', 'icon': 'mistech:seeking_pendulum', 'name': 'guide.mistech.entry.seeking_pendulum',
    'deps': ['dowsing_pendulum', 'goblin_village'], 'task': {'type': 'item', 'items': ['mistech:seeking_pendulum']}, 'pages': [
    {'type': 'crafting', 'grid': ['mistech:dowsing_pendulum', 'minecraft:compass', 'mistech:fairy_dust', '', '', '', '', '', ''],
     'result': 'mistech:seeking_pendulum', 'shapeless': True, 'text': 'guide.mistech.seeking_pendulum.1'},
    {'type': 'text', 'text': 'guide.mistech.seeking_pendulum.2'}]})

PLACE = {'magic_loom': 'slippers', 'mage_robes': 'slippers', 'seeking_pendulum': 'dowsing_pendulum'}
for eid, near in PLACE.items():
    for view in ('progress', 'dimension'):
        chs = b['views'][view]
        for c in chs:
            c['entries'] = [e for e in c['entries'] if e['id'] != eid]
        chap = next((c for c in chs if any(e['id'] == near for e in c['entries'])), chs[-1])
        ref = next(e for e in chap['entries'] if e['id'] == near)
        x, y = free_spot(chap['entries'], ref['x'] + 1.6, ref['y'])
        chap['entries'].append({'id': eid, 'x': x, 'y': y})
with open(BOOK, 'w', encoding='utf-8') as f:
    json.dump(b, f, ensure_ascii=False, indent=2)
    f.write('\n')

# ---------- traduções ----------
NAMES_PT = {'hemp': ('de Cânhamo', 'de Cânhamo', 'de Cânhamo', 'de Cânhamo'),
            'initiate': ('do Iniciado', 'do Iniciado', 'do Iniciado', 'do Iniciado'),
            'adept': ('do Adepto', 'do Adepto', 'do Adepto', 'do Adepto'),
            'archmage': ('do Arquimago', 'do Arquimago', 'do Arquimago', 'do Arquimago')}
PIECE_PT = {'hood': 'Capuz', 'robe': 'Manto', 'pants': 'Calças', 'shoes': 'Sapatos'}
PIECE_EN = {'hood': 'Hood', 'robe': 'Robe', 'pants': 'Trousers', 'shoes': 'Shoes'}
TIER_EN = {'hemp': 'Hemp', 'initiate': "Initiate's", 'adept': "Adept's", 'archmage': "Archmage's"}
pt_items = {'item.mistech.%s_mage_%s' % (t, p): '%s %s' % (PIECE_PT[p], NAMES_PT[t][0]) for t, _ in TIERS for p, _ in PIECES}
en_items = {'item.mistech.%s_mage_%s' % (t, p): '%s %s' % (TIER_EN[t], PIECE_EN[p]) for t, _ in TIERS for p, _ in PIECES}
SEEK_PT = {'goblin_village': 'Vila dos Duendes', 'ancient_city': 'Cidade Ancestral', 'nether_fortress': 'Fortaleza do Nether',
           'mithril': 'Minério de Mitril', 'adamantine': 'Minério de Adamantina', 'orichalcum': 'Minério de Oricalco', 'werewolf': 'Lobisomem'}
SEEK_EN = {'goblin_village': 'Goblin Village', 'ancient_city': 'Ancient City', 'nether_fortress': 'Nether Fortress',
           'mithril': 'Mithril Ore', 'adamantine': 'Adamantine Ore', 'orichalcum': 'Orichalcum Ore', 'werewolf': 'Werewolf'}
lang({
    **en_items,
    **{'seek.mistech.' + k: v for k, v in SEEK_EN.items()},
    'item.mistech.magic_cloth': 'Magic Cloth', 'block.mistech.magic_loom': 'Magic Loom', 'container.mistech.magic_loom': 'Magic Loom',
    'item.mistech.mage_robe.per_piece': 'Per piece worn (with energy):',
    'item.mistech.mage_robe.cost': ' -%s energy for spells', 'item.mistech.mage_robe.damage': ' +%s magic damage',
    'item.mistech.mage_robe.affinity': ' +%s affinity', 'item.mistech.mage_robe.taint': ' -%s Taint received',
    'item.mistech.mage_robe.set': 'Full set — Mana Shield: %s of the damage is paid with energy',
    'item.mistech.seeking_pendulum': 'Seeking Pendulum',
    'item.mistech.seeking_pendulum.target': 'Seeking: %s',
    'item.mistech.seeking_pendulum.keys': 'Other hand: impure shard, echo shard, blaze rod, mithril/adamantine/orichalcum, werewolf fang',
    'item.mistech.seeking_pendulum.help': 'Sneak + right click: choose target · Right click: search again',
    'item.mistech.seeking_pendulum.how': 'Sneak + right click with a key item in the other hand',
    'item.mistech.seeking_pendulum.nothing': 'The pendulum does not feel any %s nearby',
    'item.mistech.seeking_pendulum.found': '%s · %s m',
    'item.mistech.seeking_pendulum.hud': '%s · %s m %s',
    'item.mistech.seeking_pendulum.hud_none': 'Seeking Pendulum: no target',
    'item.mistech.seeking_pendulum.hud_nothing': '%s: nothing nearby',
    'item.mistech.seeking_pendulum.hud_elsewhere': '%s: in another dimension',
    'guide.mistech.entry.magic_loom': 'Magic Loom',
    'guide.mistech.magic_loom.1': 'A loom that runs on energy (10 FE/t). Put §lHemp Fiber§r in: every 4 fibers become 1 §dMagic Cloth§r (5 s).\n\nAn §fAir Rune§r in the rune slot makes 2 per weave (1 use every 8).',
    'guide.mistech.magic_loom.2': 'The base of the §dMage Robes§r — and what repairs them.',
    'guide.mistech.entry.mage_robes': 'Mage Robes',
    'guide.mistech.mage_robes.1': 'A second line of armor: §llittle protection§r, lots of magic. Hemp → §9Initiate§r → §5Adept§r → §8Archmage§r. Energy replaces durability (like mithril) and every piece can be §ldyed§r.\n\nEach piece worn §land charged§r makes spells (Gnome Staff, Magician Staff, Arcane Crossbow) cost less energy.',
    'guide.mistech.mage_robes.2': 'Hemp: Magic Cloth in the armor shapes. The higher levels are rituals, piece by piece (the robe is shown; the others are the same).',
    'guide.mistech.ritual.initiate_robes': "Initiate's Robes", 'guide.mistech.ritual.adept_robes': "Adept's Robes", 'guide.mistech.ritual.archmage_robes': "Archmage's Robes",
    'guide.mistech.mage_robes.3': 'Per piece:\n§7Hemp§r -5% energy\n§9Initiate§r -8% energy, +5% magic damage\n§5Adept§r -10% energy, +8% damage, +10% affinity, 3 sockets\n§8Archmage§r -12% energy, +12% damage, +20% affinity, -15% Taint, 6 sockets and constellation\n\nCaps: -50% energy, -60% Taint.',
    'guide.mistech.mage_robes.4': '§bMana Shield§r — the 4 pieces worn and charged: part of every hit is paid with the robes\' energy (500 FE per point) instead of health. Hemp 30%, Initiate 40%, Adept 50%, Archmage 60% (the lowest piece counts).\n\nRobes do not give the Mithril set bonus.',
    'guide.mistech.entry.seeking_pendulum': 'Seeking Pendulum',
    'guide.mistech.seeking_pendulum.1': 'The Amethyst Pendulum learns to look for things. §lSneak + right click§r with a key item in the other hand chooses the target and searches; §lright click§r searches again from where you are. 128 searches.',
    'guide.mistech.seeking_pendulum.2': '§dImpure Shard§r → Goblin Village (in the Gnome Realm)\n§3Echo Shard§r → Ancient City\n§6Blaze Rod§r → Nether Fortress\n§bMithril§r, §cAdamantine§r or §6Orichalcum§r (raw or ingot) → nearest ore (32 blocks)\n§7Werewolf Fang§r → nearest werewolf (follows it)\n\nHolding it, the bar above the hotbar shows the direction and distance.',
}, {
    **pt_items,
    **{'seek.mistech.' + k: v for k, v in SEEK_PT.items()},
    'item.mistech.magic_cloth': 'Tecido Mágico', 'block.mistech.magic_loom': 'Tear Mágico', 'container.mistech.magic_loom': 'Tear Mágico',
    'item.mistech.mage_robe.per_piece': 'Por peça vestida (com energia):',
    'item.mistech.mage_robe.cost': ' -%s de energia nas magias', 'item.mistech.mage_robe.damage': ' +%s de dano mágico',
    'item.mistech.mage_robe.affinity': ' +%s de afinidade', 'item.mistech.mage_robe.taint': ' -%s de Mácula recebida',
    'item.mistech.mage_robe.set': 'Conjunto — Escudo de Mana: %s do dano é pago com energia',
    'item.mistech.seeking_pendulum': 'Pêndulo Localizador',
    'item.mistech.seeking_pendulum.target': 'Procurando: %s',
    'item.mistech.seeking_pendulum.keys': 'Outra mão: fragmento impuro, fragmento de eco, vara de blaze, mitril/adamantina/oricalco, presa de lobisomem',
    'item.mistech.seeking_pendulum.help': 'Agachar + botão direito: escolhe o alvo · Botão direito: procura de novo',
    'item.mistech.seeking_pendulum.how': 'Agache + botão direito com um item-chave na outra mão',
    'item.mistech.seeking_pendulum.nothing': 'O pêndulo não sente nenhum(a) %s por perto',
    'item.mistech.seeking_pendulum.found': '%s · %s m',
    'item.mistech.seeking_pendulum.hud': '%s · %s m %s',
    'item.mistech.seeking_pendulum.hud_none': 'Pêndulo Localizador: sem alvo',
    'item.mistech.seeking_pendulum.hud_nothing': '%s: nada por perto',
    'item.mistech.seeking_pendulum.hud_elsewhere': '%s: em outra dimensão',
    'guide.mistech.entry.magic_loom': 'Tear Mágico',
    'guide.mistech.magic_loom.1': 'Um tear que funciona com energia (10 FE/t). Ponha §lFibra de Cânhamo§r: cada 4 fibras viram 1 §dTecido Mágico§r (5 s).\n\nUma §fRuna de Ar§r no espaço de runa faz 2 por vez (1 uso a cada 8).',
    'guide.mistech.magic_loom.2': 'A base das §dVestes de Mago§r — e o que conserta elas.',
    'guide.mistech.entry.mage_robes': 'Vestes de Mago',
    'guide.mistech.mage_robes.1': 'Uma segunda linha de armadura: §lprotege pouco§r, ajuda muito na magia. Cânhamo → §9Iniciado§r → §5Adepto§r → §8Arquimago§r. Energia no lugar da durabilidade (como o mitril) e todas as peças podem ser §ltingidas§r.\n\nCada peça vestida §le carregada§r deixa as magias (Cajado do Gnomo, Cajado do Mago, Besta Mágica) mais baratas.',
    'guide.mistech.mage_robes.2': 'Cânhamo: Tecido Mágico no formato das armaduras. Os níveis de cima são rituais, peça por peça (aparece o manto; as outras são iguais).',
    'guide.mistech.ritual.initiate_robes': 'Vestes do Iniciado', 'guide.mistech.ritual.adept_robes': 'Vestes do Adepto', 'guide.mistech.ritual.archmage_robes': 'Vestes do Arquimago',
    'guide.mistech.mage_robes.3': 'Por peça:\n§7Cânhamo§r -5% de energia\n§9Iniciado§r -8% de energia, +5% de dano mágico\n§5Adepto§r -10% de energia, +8% de dano, +10% de afinidade, 3 engastes\n§8Arquimago§r -12% de energia, +12% de dano, +20% de afinidade, -15% de Mácula, 6 engastes e constelação\n\nTetos: -50% de energia, -60% de Mácula.',
    'guide.mistech.mage_robes.4': '§bEscudo de Mana§r — as 4 peças vestidas e carregadas: parte de cada golpe é paga com a energia das vestes (500 FE por ponto) em vez de vida. Cânhamo 30%, Iniciado 40%, Adepto 50%, Arquimago 60% (vale a peça mais fraca).\n\nAs vestes não dão o bônus de conjunto do Mitril.',
    'guide.mistech.entry.seeking_pendulum': 'Pêndulo Localizador',
    'guide.mistech.seeking_pendulum.1': 'O Pêndulo de Ametista aprende a procurar coisas. §lAgachar + botão direito§r com um item-chave na outra mão escolhe o alvo e procura; §lbotão direito§r procura de novo a partir de onde você está. 128 buscas.',
    'guide.mistech.seeking_pendulum.2': '§dFragmento Impuro§r → Vila dos Duendes (na Dimensão dos Gnomos)\n§3Fragmento de Eco§r → Cidade Ancestral\n§6Vara de Blaze§r → Fortaleza do Nether\n§bMitril§r, §cAdamantina§r ou §6Oricalco§r (bruto ou lingote) → minério mais perto (32 blocos)\n§7Presa de Lobisomem§r → lobisomem mais perto (segue ele)\n\nNa mão, a linha acima da hotbar mostra a direção e a distância.',
})
print('ok')
