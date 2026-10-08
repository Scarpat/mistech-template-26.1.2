"""Recursos da Adaga de Prata, arcos, Flechas Rúnicas e Besta Mágica (modelos, receitas, tags, traduções, Grimório).
Rodar na raiz do projeto:  python tools/ranged/resources.py   (NÃO rodar o views.py depois)."""
import json, sys
sys.path.insert(0, 'tools/pendencias2')
from res import A, D, wj, tag, lang, recipe

BOWS = {'murky_bow': 1.0, 'mithril_bow': 1.15, 'adamantine_bow': 1.3, 'orichalcum_bow': 1.45, 'pure_orichalcum_bow': 1.6}

# ---------- modelos ----------
for name, speed in BOWS.items():
    wj(A + 'models/item/%s.json' % name, {'parent': 'minecraft:item/bow', 'textures': {'layer0': 'mistech:item/' + name}})
    for i in range(3):
        wj(A + 'models/item/%s_pulling_%d.json' % (name, i), {'parent': 'minecraft:item/bow',
            'textures': {'layer0': 'mistech:item/%s_pulling_%d' % (name, i)}})
    m = lambda s: {'type': 'minecraft:model', 'model': 'mistech:item/' + s}
    wj(A + 'items/%s.json' % name, {'model': {'type': 'minecraft:condition', 'property': 'minecraft:using_item',
        'on_false': m(name), 'on_true': {'type': 'minecraft:range_dispatch', 'property': 'minecraft:use_duration',
        'scale': round(0.05 * speed, 4), 'fallback': m(name + '_pulling_0'),
        'entries': [{'threshold': 0.65, 'model': m(name + '_pulling_1')}, {'threshold': 0.9, 'model': m(name + '_pulling_2')}]}}})

m = lambda s: {'type': 'minecraft:model', 'model': 'mistech:item/' + s}
wj(A + 'models/item/arcane_crossbow.json', {'parent': 'minecraft:item/crossbow', 'textures': {'layer0': 'mistech:item/arcane_crossbow_standby'}})
for i in range(3):
    wj(A + 'models/item/arcane_crossbow_pulling_%d.json' % i, {'parent': 'minecraft:item/crossbow',
        'textures': {'layer0': 'mistech:item/arcane_crossbow_pulling_%d' % i}})
wj(A + 'models/item/arcane_crossbow_bolt.json', {'parent': 'minecraft:item/crossbow', 'textures': {'layer0': 'mistech:item/arcane_crossbow_bolt'}})
wj(A + 'items/arcane_crossbow.json', {'model': {'type': 'minecraft:select', 'property': 'minecraft:charge_type',
    'cases': [{'when': 'arrow', 'model': m('arcane_crossbow_bolt')}],
    'fallback': {'type': 'minecraft:condition', 'property': 'minecraft:using_item', 'on_false': m('arcane_crossbow'),
        'on_true': {'type': 'minecraft:range_dispatch', 'property': 'minecraft:crossbow/pull', 'fallback': m('arcane_crossbow_pulling_0'),
            'entries': [{'threshold': 0.58, 'model': m('arcane_crossbow_pulling_1')}, {'threshold': 1.0, 'model': m('arcane_crossbow_pulling_2')}]}}}})

wj(A + 'models/item/silver_dagger.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': 'mistech:item/silver_dagger'}})
wj(A + 'items/silver_dagger.json', {'model': m('silver_dagger')})
wj(A + 'models/item/rune_arrow.json', {'parent': 'minecraft:item/generated',
    'textures': {'layer0': 'minecraft:item/tipped_arrow_head', 'layer1': 'minecraft:item/tipped_arrow_base'}})
wj(A + 'items/rune_arrow.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:item/rune_arrow',
    'tints': [{'type': 'minecraft:custom_model_data', 'index': 0, 'default': -5219072}]}})
wj(A + 'models/item/arcane_bolt.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mistech:item/arcane_bolt'}})
wj(A + 'items/arcane_bolt.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:item/arcane_bolt',
    'tints': [{'type': 'minecraft:custom_model_data', 'index': 0, 'default': -5219073}]}})

# ---------- receitas ----------
recipe('silver_dagger', {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': ['  M', ' F ', 'S  '],
    'key': {'M': 'mistech:mithril_ingot', 'F': 'mistech:werewolf_fang', 'S': 'minecraft:stick'}, 'result': {'id': 'mistech:silver_dagger'}})
recipe('murky_bow', {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': [' LS', 'A S', ' LS'],
    'key': {'L': '#mistech:murky_logs', 'S': 'minecraft:string', 'A': 'minecraft:amethyst_shard'}, 'result': {'id': 'mistech:murky_bow'}})
recipe('rune_arrow', {'type': 'mistech:rune_arrow'})
recipe('ritual/mithril_bow', {'type': 'mistech:ritual', 'catalyst': 'minecraft:bow', 'ingredients': ['mistech:mithril_ingot', 'mistech:mithril_ingot',
    'mistech:impure_magic_block', 'mistech:arcane_rune'], 'essence': 4, 'duration': 400, 'condition': 'none', 'min_tier': 1,
    'result': {'id': 'mistech:mithril_bow'}, 'copy_components': True, 'effect': 'forge'})
recipe('ritual/adamantine_bow', {'type': 'mistech:ritual', 'catalyst': 'mistech:mithril_bow', 'ingredients': ['mistech:adamantine_ingot',
    'mistech:adamantine_ingot', 'mistech:impure_magic_block', 'mistech:air_rune', 'mistech:arcane_rune'], 'essence': 5, 'duration': 500,
    'condition': 'none', 'min_tier': 2, 'result': {'id': 'mistech:adamantine_bow'}, 'copy_components': True, 'effect': 'forge'})
recipe('ritual/orichalcum_bow', {'type': 'mistech:ritual', 'catalyst': 'mistech:adamantine_bow', 'ingredients': ['mistech:orichalcum_ingot',
    'mistech:orichalcum_ingot', 'mistech:star_fragment', 'mistech:impure_magic_block', 'mistech:space_rune', 'mistech:time_rune'], 'essence': 6,
    'duration': 600, 'condition': 'night', 'min_tier': 3, 'result': {'id': 'mistech:orichalcum_bow'}, 'copy_components': True, 'effect': 'astral'})
recipe('sanctuary/pure_orichalcum_bow', {'type': 'mistech:ritual', 'sanctuary': True, 'catalyst': 'mistech:orichalcum_bow',
    'ingredients': ['mistech:pure_magic_shard', 'mistech:pure_magic_shard', 'mistech:purified_essence_bottle', 'mistech:light_rune'],
    'essence': 5, 'duration': 600, 'condition': 'day', 'effect': 'light', 'result': {'id': 'mistech:pure_orichalcum_bow'}, 'copy_components': True})
recipe('ritual/arcane_crossbow', {'type': 'mistech:ritual', 'catalyst': 'minecraft:crossbow', 'ingredients': ['mistech:energy_rune',
    'mistech:space_rune', 'mistech:adamantine_ingot', 'mistech:adamantine_ingot', 'mistech:bottled_lightning', 'mistech:impure_magic_shard'],
    'essence': 5, 'duration': 500, 'condition': 'none', 'min_tier': 2, 'result': {'id': 'mistech:arcane_crossbow'}, 'copy_components': True,
    'effect': 'arcane'})

# ---------- tags ----------
GEAR_BOWS = ['mistech:mithril_bow', 'mistech:adamantine_bow', 'mistech:orichalcum_bow', 'mistech:pure_orichalcum_bow']
tag('minecraft/tags/item/arrows', 'mistech:rune_arrow')
tag('minecraft/tags/item/swords', 'mistech:silver_dagger')
tag('minecraft/tags/item/enchantable/bow', 'mistech:murky_bow', *GEAR_BOWS)
tag('minecraft/tags/item/enchantable/durability', 'mistech:murky_bow')
tag('mistech/tags/item/silver_weapons', 'mistech:silver_dagger', *GEAR_BOWS)

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


def task(*items):
    return {'type': 'item', 'items': list(items)}


put('folklore', 'werewolf', {'id': 'silver_dagger', 'icon': 'mistech:silver_dagger', 'name': 'guide.mistech.entry.silver_dagger',
    'deps': ['werewolf', 'mithril_gear'], 'task': task('mistech:silver_dagger'), 'pages': [
    {'type': 'crafting', 'grid': ['', '', 'mistech:mithril_ingot', '', 'mistech:werewolf_fang', '', 'minecraft:stick', '', ''],
     'result': 'mistech:silver_dagger', 'text': 'guide.mistech.silver_dagger.1'}]})
put('herbalism', 'murky_tree', {'id': 'murky_bow', 'icon': 'mistech:murky_bow', 'name': 'guide.mistech.entry.murky_bow',
    'deps': ['murky_tree'], 'task': task('mistech:murky_bow'), 'pages': [
    {'type': 'crafting', 'grid': ['', '#mistech:murky_logs', 'minecraft:string', 'minecraft:amethyst_shard', '', 'minecraft:string',
     '', '#mistech:murky_logs', 'minecraft:string'], 'result': 'mistech:murky_bow', 'text': 'guide.mistech.murky_bow.1'}]})
put('runes', 'fire_rune', {'id': 'rune_arrows', 'icon': 'minecraft:tipped_arrow', 'name': 'guide.mistech.entry.rune_arrows',
    'deps': ['writing_table'], 'task': task('mistech:rune_arrow'), 'pages': [
    {'type': 'crafting', 'grid': ['minecraft:arrow', 'minecraft:arrow', 'minecraft:arrow', 'minecraft:arrow', 'mistech:fire_rune',
     'minecraft:arrow', 'minecraft:arrow', 'minecraft:arrow', 'minecraft:arrow'], 'result': 'mistech:rune_arrow', 'count': 8, 'shapeless': True,
     'text': 'guide.mistech.rune_arrows.1'},
    {'type': 'text', 'text': 'guide.mistech.rune_arrows.2'},
    {'type': 'text', 'text': 'guide.mistech.rune_arrows.3'}]})
put('rituals', 'mithril_gear', {'id': 'gear_bows', 'icon': 'mistech:mithril_bow', 'name': 'guide.mistech.entry.gear_bows',
    'deps': ['mithril_gear'], 'task': task('mistech:mithril_bow'), 'pages': [
    {'type': 'ritual', 'catalyst': 'minecraft:bow', 'ingredients': ['mistech:mithril_ingot', 'mistech:mithril_ingot', 'mistech:impure_magic_block',
     'mistech:arcane_rune'], 'essence': 4, 'result': 'mistech:mithril_bow', 'title': 'guide.mistech.ritual.mithril_bow', 'text': 'guide.mistech.gear_bows.1'},
    {'type': 'ritual', 'catalyst': 'mistech:mithril_bow', 'ingredients': ['mistech:adamantine_ingot', 'mistech:adamantine_ingot',
     'mistech:impure_magic_block', 'mistech:air_rune', 'mistech:arcane_rune'], 'essence': 5, 'min_tier': 2, 'result': 'mistech:adamantine_bow',
     'title': 'guide.mistech.ritual.adamantine_bow'},
    {'type': 'ritual', 'catalyst': 'mistech:adamantine_bow', 'ingredients': ['mistech:orichalcum_ingot', 'mistech:orichalcum_ingot',
     'mistech:star_fragment', 'mistech:impure_magic_block', 'mistech:space_rune', 'mistech:time_rune'], 'essence': 6, 'min_tier': 3,
     'condition': 'night', 'result': 'mistech:orichalcum_bow', 'title': 'guide.mistech.ritual.orichalcum_bow'},
    {'type': 'ritual', 'sanctuary': True, 'catalyst': 'mistech:orichalcum_bow', 'ingredients': ['mistech:pure_magic_shard', 'mistech:pure_magic_shard',
     'mistech:purified_essence_bottle', 'mistech:light_rune'], 'essence': 5, 'condition': 'day', 'result': 'mistech:pure_orichalcum_bow',
     'title': 'guide.mistech.ritual.pure_orichalcum_bow'},
    {'type': 'text', 'text': 'guide.mistech.gear_bows.2'}]})
put('artifacts', 'storm_gauntlet', {'id': 'arcane_crossbow', 'icon': 'mistech:arcane_crossbow', 'name': 'guide.mistech.entry.arcane_crossbow',
    'deps': ['runic_artifacts', 'adamantine_gear'], 'task': task('mistech:arcane_crossbow'), 'pages': [
    {'type': 'ritual', 'catalyst': 'minecraft:crossbow', 'ingredients': ['mistech:energy_rune', 'mistech:space_rune', 'mistech:adamantine_ingot',
     'mistech:adamantine_ingot', 'mistech:bottled_lightning', 'mistech:impure_magic_shard'], 'essence': 5, 'min_tier': 2,
     'result': 'mistech:arcane_crossbow', 'title': 'guide.mistech.ritual.arcane_crossbow', 'text': 'guide.mistech.arcane_crossbow.1'},
    {'type': 'text', 'text': 'guide.mistech.arcane_crossbow.2'},
    {'type': 'text', 'text': 'guide.mistech.arcane_crossbow.3'},
    {'type': 'text', 'text': 'guide.mistech.arcane_crossbow.4'}]})

# organizações (progressão / dimensão): perto da dependência, sem refazer o resto
PLACE = {'silver_dagger': 'werewolf', 'murky_bow': 'murky_tree', 'rune_arrows': 'writing_table', 'gear_bows': 'mithril_gear',
         'arcane_crossbow': 'adamantine_gear'}
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
ARROW_PT = {
    'fire': 'Incendeia o alvo e põe fogo onde cai', 'water': 'Apaga o fogo do alvo e empurra', 'earth': 'Empurrão forte',
    'air': 'Mais rápida e sem cair', 'ice': 'Lentidão e congelamento', 'energy': 'Raio no alvo (recarga de 2 s)',
    'light': 'Brilho; mortos-vivos levam dano extra', 'darkness': 'Cegueira', 'space': 'Teleporta você até onde ela cai',
    'time': 'Lentidão e Fadiga', 'magma': 'Incendeia mais e põe fogo onde cai', 'vapor': 'Nuvem de cegueira onde cai',
    'void': 'Dano extra que ignora armadura', 'luck': 'Chance de golpe crítico', 'arcane': 'Dano mágico extra',
    'echo': 'Persegue o inimigo mais perto', 'vigil': 'Brilho e Fraqueza', 'soul': 'Murchamento',
    'cloud': 'Cria uma nuvem onde cai; levita o alvo', 'life': 'Num aliado, cura em vez de ferir'}
ARROW_EN = {
    'fire': 'Sets the target ablaze and lights a fire where it lands', 'water': 'Douses the target and knocks it back', 'earth': 'Strong knockback',
    'air': 'Faster and does not drop', 'ice': 'Slowness and freezing', 'energy': 'Lightning on the target (2 s cooldown)',
    'light': 'Glowing; undead take extra damage', 'darkness': 'Blindness', 'space': 'Teleports you to where it lands',
    'time': 'Slowness and Mining Fatigue', 'magma': 'Burns longer and lights a fire where it lands', 'vapor': 'Cloud of blindness where it lands',
    'void': 'Extra damage that ignores armor', 'luck': 'Chance of a critical hit', 'arcane': 'Extra magic damage',
    'echo': 'Homes in on the nearest enemy', 'vigil': 'Glowing and Weakness', 'soul': 'Wither',
    'cloud': 'Makes a cloud where it lands; levitates the target', 'life': 'On an ally, heals instead of hurting'}
lang({
    'item.mistech.silver_dagger': 'Silver Dagger',
    'item.mistech.silver_dagger.desc': 'Back stabs and undead: double damage. Silver against werewolves.',
    'item.mistech.murky_bow': 'Murky Bow',
    'item.mistech.murky_bow.desc': 'Arrows carry a little of your strongest affinity element',
    'item.mistech.mithril_bow': 'Mithril Bow', 'item.mistech.adamantine_bow': 'Adamantine Bow',
    'item.mistech.orichalcum_bow': 'Orichalcum Bow', 'item.mistech.pure_orichalcum_bow': 'Pure Orichalcum Bow',
    'item.mistech.gear_bow.desc': 'With energy: draws %s%% faster, +%s arrow damage',
    'item.mistech.gear_bow.runes': 'Socketed runes change the arrows (Rune Anvil)',
    'item.mistech.rune_arrow': 'Rune Arrow', 'item.mistech.rune_arrow.of': 'Rune Arrow (%s)',
    'item.mistech.rune_arrow.blank': '8 arrows + 1 rune at a crafting table',
    'item.mistech.arcane_crossbow': 'Arcane Crossbow', 'item.mistech.arcane_bolt': 'Arcane Bolt',
    'entity.mistech.arcane_bolt': 'Arcane Bolt',
    'item.mistech.arcane_crossbow.desc': 'Shoots rune ammunition instead of arrows',
    'item.mistech.arcane_crossbow.drum': 'Drum:',
    'item.mistech.arcane_crossbow.slot_empty': '— empty —',
    'item.mistech.arcane_crossbow.help': 'Sneak + right click: load/unload a rune · Shift + wheel: choose',
    'item.mistech.arcane_crossbow.how': 'Sneak + right click with a rune in the other hand to load the drum',
    'item.mistech.arcane_crossbow.empty_drum': 'The drum is empty',
    'item.mistech.arcane_crossbow.selected': 'Ammo: %s (%s)',
    'item.mistech.arcane_crossbow.fizzle': 'The bolt fizzles…',
    **{'arrow_rune.mistech.' + k: v for k, v in ARROW_EN.items()},
    'guide.mistech.entry.silver_dagger': 'Silver Dagger',
    'guide.mistech.silver_dagger.1': 'Mithril and a §lWerewolf Fang§r make a light, very fast blade (4 damage). No energy needed.\n\n§6Back stab§r: hitting a creature from behind deals §ldouble damage§r. Undead also take double damage, and it counts as §fsilver§r against werewolves.',
    'guide.mistech.entry.murky_bow': 'Murky Bow',
    'guide.mistech.murky_bow.1': 'A sturdier bow of Murky Wood. Its arrows carry a little of the element you have the most §daffinity§r with (level 1 or more): a fire mage\'s arrows burn, an ice mage\'s slow down…',
    'guide.mistech.entry.rune_arrows': 'Rune Arrows',
    'guide.mistech.rune_arrows.1': '§l8 arrows + 1 rune§r, anywhere in the crafting grid, make 8 §dRune Arrows§r of that element. The rune only loses §l1 use§r (a Sealed rune loses none).\n\nThey work in any bow or crossbow.',
    'guide.mistech.rune_arrows.2': '§cFire§r burns · §9Water§r douses and pushes · §6Earth§r strong push · §fAir§r faster, no drop · §bIce§r slows and freezes · §eEnergy§r lightning · §eLight§r glowing, hurts undead · §8Darkness§r blindness · §5Space§r teleports you where it lands · §3Time§r slowness and fatigue.',
    'guide.mistech.rune_arrows.3': '§6Magma§r burns more · §7Vapor§r blinding cloud · §5Void§r ignores armor · §aLuck§r critical hits · §dArcane Energy§r magic damage · §3Echo§r homing · §fVigil§r glowing and weakness · §bSoul§r wither · §fClouds§r makes a cloud, levitates · §aLife§r heals allies instead of hurting.',
    'guide.mistech.entry.gear_bows': 'Coated Bows',
    'guide.mistech.ritual.mithril_bow': 'Mithril Bow', 'guide.mistech.ritual.adamantine_bow': 'Adamantine Bow',
    'guide.mistech.ritual.orichalcum_bow': 'Orichalcum Bow', 'guide.mistech.ritual.pure_orichalcum_bow': 'Pure Orichalcum Bow',
    'guide.mistech.gear_bows.1': 'The same path as the rest of the coated gear, from Mithril up to Pure Orichalcum. Energy replaces durability (50 FE per arrow).',
    'guide.mistech.gear_bows.2': 'With energy each level draws faster (+15% per level) and hits harder (+0.5 per level).\n\nFrom Adamantine up, the §drunes socketed§r at the Rune Anvil go into every arrow, just like a Rune Arrow but weaker (each rune counts 1, a Rune Arrow counts 3). §5Space§r only teleports you if you shot while sneaking.',
    'guide.mistech.entry.arcane_crossbow': 'Arcane Crossbow',
    'guide.mistech.ritual.arcane_crossbow': 'Arcane Crossbow',
    'guide.mistech.arcane_crossbow.1': 'Adamantine level (energy and 3 sockets). It does not use arrows: it shoots §drune ammunition§r.',
    'guide.mistech.arcane_crossbow.2': '§lSneak + right click§r with a rune in the other hand puts it in the §ldrum§r (3 runes). Sneak + right click with no rune takes the chosen one out. §lShift + wheel§r chooses.\n\nHold right click to load, click again to shoot. Each shot spends §l1 use§r of the rune (a Sealed one spends none) and 600 FE.',
    'guide.mistech.arcane_crossbow.3': '§cFire§r explodes · §9Water§r splash wave · §6Earth§r wall · §fAir§r wind charge and recoil · §bIce§r freezing area · §eEnergy§r chain lightning · §eLight§r flare · §8Darkness§r cloud · §5Space§r teleport · §3Time§r slows the area · §6Magma§r magma pool.',
    'guide.mistech.arcane_crossbow.4': '§7Vapor§r blinding cloud · §5Void§r beam through everything · §aLuck§r double damage · §dArcane Energy§r homing · §3Echo§r sonic boom · §fVigil§r weakness · §bSoul§r soul fire · §fClouds§r cloud bridge · §aLife§r heals allies.\n\nSocketed: §fAir§r = 3 bolts · §3Time§r = loads faster.',
}, {
    'item.mistech.silver_dagger': 'Adaga de Prata',
    'item.mistech.silver_dagger.desc': 'Pelas costas e contra mortos-vivos: dano em dobro. Prata contra lobisomens.',
    'item.mistech.murky_bow': 'Arco Turvo',
    'item.mistech.murky_bow.desc': 'As flechas levam um pouco do elemento da sua maior afinidade',
    'item.mistech.mithril_bow': 'Arco de Mitril', 'item.mistech.adamantine_bow': 'Arco de Adamantina',
    'item.mistech.orichalcum_bow': 'Arco de Oricalco', 'item.mistech.pure_orichalcum_bow': 'Arco de Oricalco Puro',
    'item.mistech.gear_bow.desc': 'Com energia: puxa %s%% mais rápido, +%s de dano na flecha',
    'item.mistech.gear_bow.runes': 'Runas engastadas mudam as flechas (Bigorna Rúnica)',
    'item.mistech.rune_arrow': 'Flecha Rúnica', 'item.mistech.rune_arrow.of': 'Flecha Rúnica (%s)',
    'item.mistech.rune_arrow.blank': '8 flechas + 1 runa na mesa de trabalho',
    'item.mistech.arcane_crossbow': 'Besta Mágica', 'item.mistech.arcane_bolt': 'Virote Arcano',
    'entity.mistech.arcane_bolt': 'Virote Arcano',
    'item.mistech.arcane_crossbow.desc': 'Dispara munição rúnica no lugar de flechas',
    'item.mistech.arcane_crossbow.drum': 'Tambor:',
    'item.mistech.arcane_crossbow.slot_empty': '— vazio —',
    'item.mistech.arcane_crossbow.help': 'Agachar + botão direito: põe/tira runa · Shift + roda: escolhe',
    'item.mistech.arcane_crossbow.how': 'Agache + botão direito com uma runa na outra mão para carregar o tambor',
    'item.mistech.arcane_crossbow.empty_drum': 'O tambor está vazio',
    'item.mistech.arcane_crossbow.selected': 'Munição: %s (%s)',
    'item.mistech.arcane_crossbow.fizzle': 'O virote se desfaz…',
    **{'arrow_rune.mistech.' + k: v for k, v in ARROW_PT.items()},
    'guide.mistech.entry.silver_dagger': 'Adaga de Prata',
    'guide.mistech.silver_dagger.1': 'Mitril e uma §lPresa de Lobisomem§r fazem uma lâmina leve e bem rápida (dano 4). Não precisa de energia.\n\n§6Pelas costas§r: acertar uma criatura por trás dá §ldano em dobro§r. Mortos-vivos também levam o dobro, e ela conta como §fprata§r contra lobisomens.',
    'guide.mistech.entry.murky_bow': 'Arco Turvo',
    'guide.mistech.murky_bow.1': 'Um arco mais resistente, de Madeira Turva. As flechas levam um pouco do elemento em que você tem mais §dafinidade§r (nível 1 ou mais): as do mago de fogo queimam, as do mago de gelo deixam lento…',
    'guide.mistech.entry.rune_arrows': 'Flechas Rúnicas',
    'guide.mistech.rune_arrows.1': '§l8 flechas + 1 runa§r, em qualquer lugar da grade, fazem 8 §dFlechas Rúnicas§r daquele elemento. A runa perde só §l1 uso§r (a Selada não perde nada).\n\nFuncionam em qualquer arco ou besta.',
    'guide.mistech.rune_arrows.2': '§cFogo§r incendeia · §9Água§r apaga e empurra · §6Terra§r empurrão forte · §fAr§r mais rápida, sem cair · §bGelo§r lentidão e congelamento · §eEnergia§r raio · §eLuz§r brilho, fere mortos-vivos · §8Escuridão§r cegueira · §5Espaço§r te leva até onde ela cai · §3Tempo§r lentidão e fadiga.',
    'guide.mistech.rune_arrows.3': '§6Magma§r queima mais · §7Vapor§r nuvem de cegueira · §5Vazio§r ignora armadura · §aSorte§r críticos · §dEnergia Mágica§r dano mágico · §3Eco§r teleguiada · §fVigília§r brilho e fraqueza · §bAlmas§r murchamento · §fNuvens§r cria nuvem, levita · §aVida§r cura aliados em vez de ferir.',
    'guide.mistech.entry.gear_bows': 'Arcos Revestidos',
    'guide.mistech.ritual.mithril_bow': 'Arco de Mitril', 'guide.mistech.ritual.adamantine_bow': 'Arco de Adamantina',
    'guide.mistech.ritual.orichalcum_bow': 'Arco de Oricalco', 'guide.mistech.ritual.pure_orichalcum_bow': 'Arco de Oricalco Puro',
    'guide.mistech.gear_bows.1': 'O mesmo caminho do resto do equipamento revestido, do Mitril ao Oricalco Puro. Energia no lugar da durabilidade (50 FE por flecha).',
    'guide.mistech.gear_bows.2': 'Com energia, cada nível puxa mais rápido (+15% por nível) e acerta mais forte (+0,5 por nível).\n\nDa Adamantina para cima, as §drunas engastadas§r na Bigorna Rúnica vão em todas as flechas, como uma Flecha Rúnica mais fraca (cada runa vale 1, a Flecha Rúnica vale 3). O §5Espaço§r só te teleporta se você atirou agachado.',
    'guide.mistech.entry.arcane_crossbow': 'Besta Mágica',
    'guide.mistech.ritual.arcane_crossbow': 'Besta Mágica',
    'guide.mistech.arcane_crossbow.1': 'Nível Adamantina (energia e 3 engastes). Não usa flechas: dispara §dmunição rúnica§r.',
    'guide.mistech.arcane_crossbow.2': '§lAgachar + botão direito§r com uma runa na outra mão põe a runa no §ltambor§r (3 runas). Agachar + botão direito sem runa tira a escolhida. §lShift + roda§r escolhe.\n\nSegure o botão direito para carregar e clique de novo para atirar. Cada tiro gasta §l1 uso§r da runa (a Selada não gasta) e 600 FE.',
    'guide.mistech.arcane_crossbow.3': '§cFogo§r explode · §9Água§r onda que apaga e empurra · §6Terra§r muralha · §fAr§r carga de vento e recuo · §bGelo§r área que congela · §eEnergia§r raio em cadeia · §eLuz§r clarão · §8Escuridão§r nuvem · §5Espaço§r teleporte · §3Tempo§r área lenta · §6Magma§r poça de magma.',
    'guide.mistech.arcane_crossbow.4': '§7Vapor§r nuvem de cegueira · §5Vazio§r raio que atravessa tudo · §aSorte§r dano em dobro · §dEnergia Mágica§r teleguiado · §3Eco§r estrondo sônico · §fVigília§r fraqueza · §bAlmas§r fogo de alma · §fNuvens§r ponte de nuvens · §aVida§r cura aliados.\n\nEngastadas: §fAr§r = 3 virotes · §3Tempo§r = carrega mais rápido.',
})
print('ok')
