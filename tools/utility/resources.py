"""Recursos do Elevador Rúnico, Narguilé Rúnico, Cachaça (garapa, barril, alambique) e Infusor Arcano.
Rodar na raiz do projeto:  python tools/utility/resources.py   (NÃO rodar o views.py depois)."""
import json, sys
sys.path.insert(0, 'tools/pendencias2')
from res import A, D, wj, tag, lang, recipe, self_loot, flat_item


def signed(c):
    v = 0xFF000000 | c
    return v - (1 << 32)


def face(tex, uv=None, tint=None, cull=None):
    f = {'texture': tex}
    if uv:
        f['uv'] = uv
    if tint is not None:
        f['tintindex'] = tint
    if cull:
        f['cullface'] = cull
    return f


def box(frm, to, tex, uv=None, faces='nsewud', top=None):
    names = {'n': 'north', 's': 'south', 'e': 'east', 'w': 'west', 'u': 'up', 'd': 'down'}
    fs = {}
    for k in faces:
        t = top if (k == 'u' and top) else tex
        fs[names[k]] = face(t, uv)
    return {'from': frm, 'to': to, 'faces': fs}


# ---------- Elevador Rúnico: bloco + glifo tingido (17 cores) ----------
SIDES = ('north', 'south', 'east', 'west')
wj(A + 'models/block/runic_elevator.json', {'parent': 'minecraft:block/block', 'textures': {
    'particle': 'mistech:block/runic_elevator', 'base': 'mistech:block/runic_elevator',
    'glyph': 'mistech:block/runic_elevator_glyph', 'glyph_side': 'mistech:block/runic_elevator_glyph_side'},
    'elements': [
        {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {d: face('#base', cull=d) for d in SIDES + ('up', 'down')}},
        {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {**{d: face('#glyph_side', tint=0, cull=d) for d in SIDES},
                                                         'up': face('#glyph', tint=0, cull='up')}}]})
wj(A + 'blockstates/runic_elevator.json', {'variants': {'color=%d' % i: {'model': 'mistech:block/runic_elevator'} for i in range(17)}})
wj(A + 'items/runic_elevator.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:block/runic_elevator',
    'tints': [{'type': 'minecraft:constant', 'value': signed(0xA66BEB)}]}})
self_loot('runic_elevator')
recipe('runic_elevator', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['PAP', 'PRP', 'PPP'],
    'key': {'P': 'minecraft:polished_blackstone', 'A': 'minecraft:amethyst_shard', 'R': 'mistech:air_rune'},
    'result': {'id': 'mistech:runic_elevator', 'count': 2}})

# ---------- Narguilé Rúnico ----------
H = '#atlas'
hookah_elements = [
    box([5, 0, 5], [11, 6, 11], H, [0, 0, 6, 6]),                     # vaso de vidro
    box([6, 6, 6], [10, 7, 10], H, [8, 0, 12, 1]),                    # gargalo de latão
    box([7.5, 7, 7.5], [8.5, 11, 8.5], H, [9, 1, 10, 5]),             # haste
    box([4.5, 11, 4.5], [11.5, 11.5, 11.5], H, [8, 0, 15, 7]),        # bandeja
    box([6, 11.5, 6], [10, 14, 10], H, [0, 8, 4, 10.5], top='#ember'),  # fornilho com brasa
    box([3, 4, 7.5], [5, 5, 8.5], H, [8, 8, 10, 9]),                  # mangueiras
    box([2, 0, 7.5], [3, 5, 8.5], H, [8, 8, 9, 13]),
    box([11, 4, 7.5], [13, 5, 8.5], H, [8, 8, 10, 9]),
    box([13, 0, 7.5], [14, 5, 8.5], H, [8, 8, 9, 13]),
    box([7.5, 3, 3], [8.5, 4, 5], H, [10, 8, 12, 9]),
    box([7.5, 0, 2], [8.5, 4, 3], H, [10, 8, 11, 12]),
]
for lit in (False, True):
    name = 'runic_hookah_lit' if lit else 'runic_hookah'
    wj(A + 'models/block/%s.json' % name, {'parent': 'minecraft:block/block', 'textures': {
        'particle': 'mistech:block/runic_hookah', 'atlas': 'mistech:block/runic_hookah',
        'ember': 'mistech:block/runic_hookah_ember' + ('_lit' if lit else '')}, 'elements': hookah_elements})
wj(A + 'blockstates/runic_hookah.json', {'variants': {'lit=false': {'model': 'mistech:block/runic_hookah'},
                                                     'lit=true': {'model': 'mistech:block/runic_hookah_lit'}}})
flat_item('runic_hookah')
self_loot('runic_hookah')
recipe('runic_hookah', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' I ', 'GAG', 'HGH'],
    'key': {'I': 'minecraft:gold_ingot', 'G': 'minecraft:purple_stained_glass', 'A': 'minecraft:amethyst_shard', 'H': 'mistech:hemp_fiber'},
    'result': {'id': 'mistech:runic_hookah'}})

# ---------- Cachaça ----------
for n in ('garapa', 'fermented_garapa', 'cachaca', 'aged_cachaca', 'rue_cachaca', 'honey_cachaca'):
    flat_item(n)
wj(A + 'models/block/fermentation_barrel.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
    'top': 'mistech:block/fermentation_barrel_top', 'side': 'mistech:block/fermentation_barrel_side',
    'bottom': 'mistech:block/fermentation_barrel_bottom'}})
wj(A + 'blockstates/fermentation_barrel.json', {'variants': {'': {'model': 'mistech:block/fermentation_barrel'}}})
wj(A + 'items/fermentation_barrel.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:block/fermentation_barrel'}})
self_loot('fermentation_barrel')
for on in (False, True):
    wj(A + 'models/block/cane_still%s.json' % ('_on' if on else ''), {'parent': 'minecraft:block/orientable_with_bottom', 'textures': {
        'bottom': 'mistech:block/cane_still_bottom', 'front': 'mistech:block/cane_still_front' + ('_on' if on else ''),
        'side': 'mistech:block/cane_still_side', 'top': 'mistech:block/cane_still_top'}})
variants = {}
for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
    for lit in ('false', 'true'):
        v = {'model': 'mistech:block/cane_still' + ('_on' if lit == 'true' else '')}
        if rot:
            v['y'] = rot
        variants['facing=%s,lit=%s' % (facing, lit)] = v
wj(A + 'blockstates/cane_still.json', {'variants': variants})
wj(A + 'items/cane_still.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:block/cane_still'}})
self_loot('cane_still')

recipe('garapa', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
    'ingredients': ['minecraft:sugar_cane', 'minecraft:sugar_cane', 'minecraft:sugar_cane', 'minecraft:glass_bottle'],
    'result': {'id': 'mistech:garapa'}})
recipe('rue_cachaca', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
    'ingredients': ['mistech:cachaca', 'mistech:rue_leaf', 'mistech:rue_leaf'], 'result': {'id': 'mistech:rue_cachaca'}})
recipe('honey_cachaca', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
    'ingredients': ['mistech:cachaca', 'minecraft:honey_bottle'], 'result': {'id': 'mistech:honey_cachaca'}})
recipe('fermentation_barrel', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['PSP', 'PIP', 'PSP'],
    'key': {'P': 'mistech:murky_planks', 'S': 'mistech:murky_slab', 'I': 'minecraft:copper_ingot'},
    'result': {'id': 'mistech:fermentation_barrel'}})
recipe('cane_still', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['CGC', 'CKC', 'BFB'],
    'key': {'C': 'minecraft:copper_ingot', 'G': 'minecraft:glass_pane', 'K': 'mistech:kettle', 'B': 'minecraft:copper_block',
            'F': 'minecraft:furnace'}, 'result': {'id': 'mistech:cane_still'}})

# ---------- Infusor Arcano ----------
INF = ('arcane_infuser', 'greater_arcane_infuser', 'astral_arcane_infuser')
for n in INF:
    wj(A + 'models/block/%s.json' % n, {'parent': 'minecraft:block/block', 'textures': {
        'particle': 'mistech:block/%s_side' % n, 'top': 'mistech:block/%s_top' % n, 'side': 'mistech:block/%s_side' % n,
        'bottom': 'mistech:block/%s_bottom' % n, 'crystal': 'mistech:block/%s_crystal' % n},
        'elements': [
            {'from': [0, 0, 0], 'to': [16, 12, 16], 'faces': {
                'down': face('#bottom', [0, 0, 16, 16], cull='down'), 'up': face('#top', [0, 0, 16, 16]),
                **{d: face('#side', [0, 4, 16, 16], cull=d) for d in SIDES}}},
            {'from': [7, 13, 7], 'to': [9, 16, 9], 'rotation': {'angle': 45, 'axis': 'y', 'origin': [8, 14, 8]}, 'shade': False,
             'faces': {d: face('#crystal', [0, 0, 2, 3]) for d in SIDES + ('up', 'down')}},
            {'from': [6.5, 12, 6.5], 'to': [9.5, 12.5, 9.5], 'faces': {d: face('#crystal', [0, 0, 3, 1]) for d in SIDES + ('up',)}}]})
    wj(A + 'blockstates/%s.json' % n, {'variants': {'': {'model': 'mistech:block/' + n}}})
    wj(A + 'items/%s.json' % n, {'model': {'type': 'minecraft:model', 'model': 'mistech:block/' + n}})
    self_loot(n)
recipe('arcane_infuser', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' R ', 'AEA', 'OIO'],
    'key': {'R': 'mistech:arcane_rune', 'A': 'minecraft:amethyst_shard', 'E': 'minecraft:enchanting_table', 'O': 'minecraft:obsidian',
            'I': 'mistech:impure_magic_block'}, 'result': {'id': 'mistech:arcane_infuser'}})
GREATER = ['minecraft:lapis_block', 'minecraft:lapis_block', 'minecraft:diamond', 'minecraft:diamond', 'mistech:arcane_rune',
           'mistech:impure_magic_block']
ASTRAL = ['mistech:star_fragment', 'mistech:star_fragment', 'mistech:purified_essence_bottle', 'mistech:time_rune',
          'mistech:space_rune', 'minecraft:echo_shard']
recipe('ritual/greater_arcane_infuser', {'type': 'mistech:ritual', 'catalyst': 'mistech:arcane_infuser', 'ingredients': GREATER,
    'essence': 5, 'duration': 400, 'condition': 'none', 'min_tier': 2, 'result': {'id': 'mistech:greater_arcane_infuser'},
    'effect': 'arcane'})
recipe('ritual/astral_arcane_infuser', {'type': 'mistech:ritual', 'catalyst': 'mistech:greater_arcane_infuser', 'ingredients': ASTRAL,
    'essence': 8, 'duration': 600, 'condition': 'night', 'min_tier': 3, 'result': {'id': 'mistech:astral_arcane_infuser'},
    'effect': 'astral'})

# ---------- tags ----------
tag('minecraft/tags/block/mineable/pickaxe', 'mistech:runic_elevator', 'mistech:cane_still', *['mistech:' + n for n in INF])
tag('minecraft/tags/block/mineable/axe', 'mistech:fermentation_barrel')

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


put('runes', 'air_rune', {'id': 'runic_elevator', 'icon': 'mistech:runic_elevator', 'name': 'guide.mistech.entry.runic_elevator',
    'deps': ['air_rune'], 'task': {'type': 'item', 'items': ['mistech:runic_elevator']}, 'pages': [
    {'type': 'crafting', 'grid': ['minecraft:polished_blackstone', 'minecraft:amethyst_shard', 'minecraft:polished_blackstone',
     'minecraft:polished_blackstone', 'mistech:air_rune', 'minecraft:polished_blackstone', 'minecraft:polished_blackstone',
     'minecraft:polished_blackstone', 'minecraft:polished_blackstone'], 'result': 'mistech:runic_elevator',
     'text': 'guide.mistech.runic_elevator.1'},
    {'type': 'text', 'text': 'guide.mistech.runic_elevator.2'}]})
put('herbalism', 'runic_pipe', {'id': 'runic_hookah', 'icon': 'mistech:runic_hookah', 'name': 'guide.mistech.entry.runic_hookah',
    'deps': ['runic_pipe'], 'task': {'type': 'item', 'items': ['mistech:runic_hookah']}, 'pages': [
    {'type': 'crafting', 'grid': ['', 'minecraft:gold_ingot', '', 'minecraft:purple_stained_glass', 'minecraft:amethyst_shard',
     'minecraft:purple_stained_glass', 'mistech:hemp_fiber', 'minecraft:purple_stained_glass', 'mistech:hemp_fiber'],
     'result': 'mistech:runic_hookah', 'text': 'guide.mistech.runic_hookah.1'},
    {'type': 'text', 'text': 'guide.mistech.runic_hookah.2'},
    {'type': 'text', 'text': 'guide.mistech.runic_hookah.3'}]})
put('herbalism', 'kettle', {'id': 'cachaca', 'icon': 'mistech:cachaca', 'name': 'guide.mistech.entry.cachaca',
    'deps': ['kettle', 'murky_tree'], 'task': {'type': 'item', 'items': ['mistech:cachaca']}, 'pages': [
    {'type': 'crafting', 'grid': ['minecraft:sugar_cane', 'minecraft:sugar_cane', 'minecraft:sugar_cane', 'minecraft:glass_bottle',
     '', '', '', '', ''], 'result': 'mistech:garapa', 'shapeless': True, 'text': 'guide.mistech.cachaca.1'},
    {'type': 'crafting', 'grid': ['mistech:murky_planks', 'mistech:murky_slab', 'mistech:murky_planks', 'mistech:murky_planks',
     'minecraft:copper_ingot', 'mistech:murky_planks', 'mistech:murky_planks', 'mistech:murky_slab', 'mistech:murky_planks'],
     'result': 'mistech:fermentation_barrel', 'text': 'guide.mistech.cachaca.2'},
    {'type': 'crafting', 'grid': ['minecraft:copper_ingot', 'minecraft:glass_pane', 'minecraft:copper_ingot', 'minecraft:copper_ingot',
     'mistech:kettle', 'minecraft:copper_ingot', 'minecraft:copper_block', 'minecraft:furnace', 'minecraft:copper_block'],
     'result': 'mistech:cane_still', 'text': 'guide.mistech.cachaca.3'},
    {'type': 'spotlight', 'item': 'mistech:aged_cachaca', 'text': 'guide.mistech.cachaca.4'},
    {'type': 'text', 'text': 'guide.mistech.cachaca.5'}]})
put('essence', 'impure_magic_block', {'id': 'arcane_infuser', 'icon': 'mistech:arcane_infuser', 'name': 'guide.mistech.entry.arcane_infuser',
    'deps': ['impure_magic_block', 'arcane_rune'], 'task': {'type': 'item', 'items': ['mistech:arcane_infuser']}, 'pages': [
    {'type': 'crafting', 'grid': ['', 'mistech:arcane_rune', '', 'minecraft:amethyst_shard', 'minecraft:enchanting_table',
     'minecraft:amethyst_shard', 'minecraft:obsidian', 'mistech:impure_magic_block', 'minecraft:obsidian'],
     'result': 'mistech:arcane_infuser', 'text': 'guide.mistech.arcane_infuser.1'},
    {'type': 'text', 'text': 'guide.mistech.arcane_infuser.2'},
    {'type': 'ritual', 'catalyst': 'mistech:arcane_infuser', 'ingredients': GREATER, 'essence': 5, 'min_tier': 2,
     'result': 'mistech:greater_arcane_infuser', 'title': 'guide.mistech.ritual.greater_arcane_infuser'},
    {'type': 'ritual', 'catalyst': 'mistech:greater_arcane_infuser', 'ingredients': ASTRAL, 'essence': 8, 'min_tier': 3,
     'condition': 'night', 'result': 'mistech:astral_arcane_infuser', 'title': 'guide.mistech.ritual.astral_arcane_infuser'},
    {'type': 'text', 'text': 'guide.mistech.arcane_infuser.3'}]})

PLACE = {'runic_elevator': 'air_rune', 'runic_hookah': 'runic_pipe', 'cachaca': 'kettle', 'arcane_infuser': 'impure_magic_block'}
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
    'block.mistech.runic_elevator': 'Runic Elevator',
    'block.mistech.runic_hookah': 'Runic Hookah',
    'block.mistech.runic_hookah.base.none': 'empty', 'block.mistech.runic_hookah.base.water': 'water',
    'block.mistech.runic_hookah.base.impure': 'Impure Essence', 'block.mistech.runic_hookah.base.pure': 'Purified Essence',
    'block.mistech.runic_hookah.leaf.none': 'empty', 'block.mistech.runic_hookah.leaf.tobacco': 'tobacco',
    'block.mistech.runic_hookah.leaf.hemp': 'hemp', 'block.mistech.runic_hookah.leaf.rue': 'rue',
    'block.mistech.runic_hookah.no_base': 'The hookah needs a base: water, Impure or Purified Essence',
    'block.mistech.runic_hookah.no_leaf': 'The bowl is empty: dried tobacco, dried hemp or rue leaf',
    'block.mistech.runic_hookah.not_lit': 'Light the coal with a lighter or a Fire Rune',
    'block.mistech.runic_hookah.no_rune': 'no rune',
    'block.mistech.runic_hookah.status': 'Base: %s · Bowl: %s (%s puffs) · Flavor: %s',
    'item.mistech.garapa': 'Garapa', 'item.mistech.fermented_garapa': 'Fermented Garapa',
    'item.mistech.cachaca': 'Cachaça', 'item.mistech.aged_cachaca': 'Aged Cachaça',
    'item.mistech.rue_cachaca': 'Rue Cachaça', 'item.mistech.honey_cachaca': 'Honey Cachaça',
    'item.mistech.cachaca.tip.plain': 'Strength I + Resistance I (1:00)',
    'item.mistech.cachaca.tip.aged': 'Strength I + Resistance I (3:00), Regeneration — softer on the head',
    'item.mistech.cachaca.tip.rue': 'Strength I + Resistance I (1:00) and removes 5 Taint',
    'item.mistech.cachaca.tip.honey': 'Strength I + Resistance I (1:00), Regeneration and cures poison',
    'item.mistech.cachaca.tip.warning': 'Three in a row and you are drunk. The Saci loves it.',
    'item.mistech.cachaca.drunk': 'You are drunk!',
    'block.mistech.fermentation_barrel': 'Fermentation Barrel',
    'block.mistech.fermentation_barrel.cant': 'Only Garapa or Cachaça — one kind at a time, up to 16',
    'block.mistech.fermentation_barrel.empty': 'Empty barrel. Garapa ferments in half a day; Cachaça ages in one day.',
    'block.mistech.fermentation_barrel.status': '%s× %s · %s%% · ~%s min left',
    'block.mistech.cane_still': 'Cane Still', 'container.mistech.cane_still': 'Cane Still',
    'gui.mistech.cane_still.heat.0': 'Heat: energy', 'gui.mistech.cane_still.heat.1': 'Heat: fire below',
    'gui.mistech.cane_still.heat.2': 'Heat: Fire Rune',
    'message.mistech.saci.cachaca': 'The Saci loved the cachaça! Fed for %s min',
    'block.mistech.arcane_infuser': 'Arcane Infuser', 'block.mistech.greater_arcane_infuser': 'Greater Arcane Infuser',
    'block.mistech.astral_arcane_infuser': 'Astral Arcane Infuser',
    'gui.mistech.arcane_infuser.infuse': 'Infuse',
    'gui.mistech.arcane_infuser.put_item': 'Place an item or a book',
    'gui.mistech.arcane_infuser.nothing': 'Nothing to infuse',
    'gui.mistech.arcane_infuser.levels': 'Level %s of %s',
    'gui.mistech.arcane_infuser.beyond': 'Beyond the normal maximum (Star Fragment + Purified Essence)',
    'gui.mistech.arcane_infuser.cost_xp': '%s XP levels', 'gui.mistech.arcane_infuser.cost_fe': '%s FE',
    'gui.mistech.arcane_infuser.cost_lapis': '%s Lapis Lazuli', 'gui.mistech.arcane_infuser.cost_essence': '%s Impure Essence bottles',
    'gui.mistech.arcane_infuser.cost_star': '%s Star Fragment', 'gui.mistech.arcane_infuser.cost_purified': '%s Purified Essence bottle',
    'gui.mistech.arcane_infuser.need_xp': 'Not enough experience', 'gui.mistech.arcane_infuser.need_energy': 'Not enough energy',
    'gui.mistech.arcane_infuser.need_lapis': 'Not enough lapis', 'gui.mistech.arcane_infuser.need_essence': 'Not enough Impure Essence',
    'gui.mistech.arcane_infuser.need_star': 'Needs a Star Fragment', 'gui.mistech.arcane_infuser.need_purified': 'Needs Purified Essence',
    'gui.mistech.arcane_infuser.slot_target': 'Item or book', 'gui.mistech.arcane_infuser.slot_lapis': 'Lapis Lazuli',
    'gui.mistech.arcane_infuser.slot_essence': 'Impure Essence bottles', 'gui.mistech.arcane_infuser.slot_star': 'Star Fragment (beyond max)',
    'gui.mistech.arcane_infuser.slot_purified': 'Purified Essence bottle (beyond max)',
    'guide.mistech.entry.runic_elevator': 'Runic Elevator',
    'guide.mistech.runic_elevator.1': 'Stack two or more elevators in the same column, with any gap (up to 32 blocks) between them.',
    'guide.mistech.runic_elevator.2': 'Standing on one: §ljump§r goes to the next one above, §lsneak§r to the next one below. The arrival needs 2 free blocks.\n\nRight click with a §ldye§r to paint the glyph: elevators only link with the same color, so several shafts can share a column.',
    'guide.mistech.entry.runic_hookah': 'Runic Hookah',
    'guide.mistech.runic_hookah.1': 'A big pipe for the whole room. Fill it in this order: §lbase§r (water bottle/bucket, Impure or Purified Essence), §lbowl§r (dried tobacco, dried hemp or rue leaf — 32 puffs), §lflavor§r (any rune) and §llight it§r with a lighter.',
    'guide.mistech.runic_hookah.2': 'Right click with an empty hand: a puff, with the magic cigar effect of the rune (one use every 8 puffs). Anyone can use it, each with their own 1.5 s pause. Sneak + right click takes the rune out (or shows the status).\n\nThe smoke gives a weak aura of the effect to anyone within 6 blocks.',
    'guide.mistech.runic_hookah.3': '§9Water§r: normal.\n§5Impure Essence§r: the power of the joint, plus a bit of Taint.\n§bPurified Essence§r: effects last 1.5× and every puff cleanses Taint.\n§aHemp§r gives the joint\'s power; §2rue§r cleanses; no rune = Regeneration.',
    'guide.mistech.entry.cachaca': 'Cachaça',
    'guide.mistech.cachaca.1': '§lGarapa§r: three sugar canes in a glass bottle. Drinkable (a bit of food and Speed), but it is meant for the barrel.',
    'guide.mistech.cachaca.2': 'The §lFermentation Barrel§r (murky wood) holds up to 16 bottles of one kind. Garapa ferments in §lhalf a day§r; Cachaça ages in §lone day§r. Right click puts one (sneaking: the whole hand); empty hand shows the time or takes it out when ready.',
    'guide.mistech.cachaca.3': 'The §lCane Still§r distills Fermented Garapa into Cachaça (10 s each). Heat: §cfire, lit campfire, lava or magma below§r (free), a §cFire Rune§r in the rune slot, or 20 FE/t.',
    'guide.mistech.cachaca.4': '§6Aged Cachaça§r: one day in the barrel. Effects last 3 minutes and it hits softer.',
    'guide.mistech.cachaca.5': 'Every cachaça gives Strength I + Resistance I and some Nausea; three in a row = drunk (Slowness and Mining Fatigue).\n\n§2Rue Cachaça§r (+2 rue leaves): -5 Taint.\n§6Honey Cachaça§r (+honey): Regeneration, cures poison.\n\n§lSaci offering§r: right click your Saci with any cachaça — it doubles the time he stays fed and he gives a gift on the spot.',
    'guide.mistech.entry.arcane_infuser': 'Arcane Infuser',
    'guide.mistech.arcane_infuser.1': 'An enchanting table where you §lchoose§r. Put the item (or a book), lapis and Impure Essence bottles; it runs on energy (100,000 FE buffer).',
    'guide.mistech.arcane_infuser.2': 'The list shows the enchantments the item accepts. Pick one, choose the level with − / + and press §lInfuse§r.\n\nCost of level L: §a3 + 3L XP levels§r, §d5,000 × L FE§r, §9L lapis§r and §5L Impure Essence bottles§r (the empty bottles come back).',
    'guide.mistech.ritual.greater_arcane_infuser': 'Greater Infuser', 'guide.mistech.ritual.astral_arcane_infuser': 'Astral Infuser',
    'guide.mistech.arcane_infuser.3': 'Maximum level:\n§7Infuser§r up to II\n§5Greater§r the normal maximum\n§bAstral§r maximum + 1 and treasure enchantments (Mending, Frost Walker...)\n\nGoing beyond the maximum also costs a §eStar Fragment§r and a §bPurified Essence§r bottle. Curses never.',
}, {
    'block.mistech.runic_elevator': 'Elevador Rúnico',
    'block.mistech.runic_hookah': 'Narguilé Rúnico',
    'block.mistech.runic_hookah.base.none': 'vazio', 'block.mistech.runic_hookah.base.water': 'água',
    'block.mistech.runic_hookah.base.impure': 'Essência Impura', 'block.mistech.runic_hookah.base.pure': 'Essência Purificada',
    'block.mistech.runic_hookah.leaf.none': 'vazio', 'block.mistech.runic_hookah.leaf.tobacco': 'tabaco',
    'block.mistech.runic_hookah.leaf.hemp': 'cânhamo', 'block.mistech.runic_hookah.leaf.rue': 'arruda',
    'block.mistech.runic_hookah.no_base': 'O narguilé precisa de base: água, Essência Impura ou Purificada',
    'block.mistech.runic_hookah.no_leaf': 'Fornilho vazio: tabaco seco, cânhamo seco ou folha de arruda',
    'block.mistech.runic_hookah.not_lit': 'Acenda a brasa com um isqueiro ou uma Runa de Fogo',
    'block.mistech.runic_hookah.no_rune': 'sem runa',
    'block.mistech.runic_hookah.status': 'Base: %s · Fornilho: %s (%s tragadas) · Sabor: %s',
    'item.mistech.garapa': 'Garapa', 'item.mistech.fermented_garapa': 'Garapa Fermentada',
    'item.mistech.cachaca': 'Cachaça', 'item.mistech.aged_cachaca': 'Cachaça Envelhecida',
    'item.mistech.rue_cachaca': 'Cachaça de Arruda', 'item.mistech.honey_cachaca': 'Cachaça com Mel',
    'item.mistech.cachaca.tip.plain': 'Força I + Resistência I (1:00)',
    'item.mistech.cachaca.tip.aged': 'Força I + Resistência I (3:00), Regeneração — sobe menos',
    'item.mistech.cachaca.tip.rue': 'Força I + Resistência I (1:00) e tira 5 de Mácula',
    'item.mistech.cachaca.tip.honey': 'Força I + Resistência I (1:00), Regeneração e cura veneno',
    'item.mistech.cachaca.tip.warning': 'Três seguidas e você fica bêbado. O Saci adora.',
    'item.mistech.cachaca.drunk': 'Você está bêbado!',
    'block.mistech.fermentation_barrel': 'Barril de Fermentação',
    'block.mistech.fermentation_barrel.cant': 'Só Garapa ou Cachaça — um tipo por vez, até 16',
    'block.mistech.fermentation_barrel.empty': 'Barril vazio. Garapa fermenta em meio dia; Cachaça envelhece em um dia.',
    'block.mistech.fermentation_barrel.status': '%s× %s · %s%% · faltam ~%s min',
    'block.mistech.cane_still': 'Alambique', 'container.mistech.cane_still': 'Alambique',
    'gui.mistech.cane_still.heat.0': 'Calor: energia', 'gui.mistech.cane_still.heat.1': 'Calor: fogo embaixo',
    'gui.mistech.cane_still.heat.2': 'Calor: Runa de Fogo',
    'message.mistech.saci.cachaca': 'O Saci adorou a cachaça! Alimentado por %s min',
    'block.mistech.arcane_infuser': 'Infusor Arcano', 'block.mistech.greater_arcane_infuser': 'Infusor Arcano Maior',
    'block.mistech.astral_arcane_infuser': 'Infusor Arcano Astral',
    'gui.mistech.arcane_infuser.infuse': 'Infundir',
    'gui.mistech.arcane_infuser.put_item': 'Ponha um item ou livro',
    'gui.mistech.arcane_infuser.nothing': 'Nada para infundir',
    'gui.mistech.arcane_infuser.levels': 'Nível %s de %s',
    'gui.mistech.arcane_infuser.beyond': 'Acima do máximo normal (Fragmento Estelar + Essência Purificada)',
    'gui.mistech.arcane_infuser.cost_xp': '%s níveis de XP', 'gui.mistech.arcane_infuser.cost_fe': '%s FE',
    'gui.mistech.arcane_infuser.cost_lapis': '%s Lápis-lazúli', 'gui.mistech.arcane_infuser.cost_essence': '%s frascos de Essência Impura',
    'gui.mistech.arcane_infuser.cost_star': '%s Fragmento Estelar', 'gui.mistech.arcane_infuser.cost_purified': '%s frasco de Essência Purificada',
    'gui.mistech.arcane_infuser.need_xp': 'Falta experiência', 'gui.mistech.arcane_infuser.need_energy': 'Falta energia',
    'gui.mistech.arcane_infuser.need_lapis': 'Falta lápis-lazúli', 'gui.mistech.arcane_infuser.need_essence': 'Falta Essência Impura',
    'gui.mistech.arcane_infuser.need_star': 'Precisa de um Fragmento Estelar', 'gui.mistech.arcane_infuser.need_purified': 'Precisa de Essência Purificada',
    'gui.mistech.arcane_infuser.slot_target': 'Item ou livro', 'gui.mistech.arcane_infuser.slot_lapis': 'Lápis-lazúli',
    'gui.mistech.arcane_infuser.slot_essence': 'Frascos de Essência Impura', 'gui.mistech.arcane_infuser.slot_star': 'Fragmento Estelar (acima do máximo)',
    'gui.mistech.arcane_infuser.slot_purified': 'Frasco de Essência Purificada (acima do máximo)',
    'guide.mistech.entry.runic_elevator': 'Elevador Rúnico',
    'guide.mistech.runic_elevator.1': 'Ponha dois ou mais elevadores na mesma coluna, com qualquer vão (até 32 blocos) entre eles.',
    'guide.mistech.runic_elevator.2': 'Em cima de um: §lpular§r leva ao próximo de cima, §lagachar§r ao próximo de baixo. A chegada precisa de 2 blocos livres.\n\nBotão direito com um §lcorante§r pinta o glifo: elevadores só se ligam com a mesma cor, então dá para ter vários poços na mesma coluna.',
    'guide.mistech.entry.runic_hookah': 'Narguilé Rúnico',
    'guide.mistech.runic_hookah.1': 'Um cachimbo grande para o cômodo todo. Monte nesta ordem: §lbase§r (garrafa/balde de água, Essência Impura ou Purificada), §lfornilho§r (tabaco seco, cânhamo seco ou folha de arruda — 32 tragadas), §lsabor§r (qualquer runa) e §lacenda§r com um isqueiro.',
    'guide.mistech.runic_hookah.2': 'Botão direito com a mão vazia: uma tragada, com o efeito do charuto mágico daquela runa (1 uso a cada 8 tragadas). Qualquer um pode usar, cada um com sua pausa de 1,5 s. Agachar + botão direito tira a runa (ou mostra como está).\n\nA fumaça dá uma aura fraca do efeito a quem estiver a até 6 blocos.',
    'guide.mistech.runic_hookah.3': '§9Água§r: normal.\n§5Essência Impura§r: o poder do baseado, mais um pouco de Mácula.\n§bEssência Purificada§r: efeitos duram 1,5× e cada tragada limpa Mácula.\n§aCânhamo§r dá o poder do baseado; §2arruda§r limpa; sem runa = Regeneração.',
    'guide.mistech.entry.cachaca': 'Cachaça',
    'guide.mistech.cachaca.1': '§lGarapa§r: três canas numa garrafa de vidro. Dá para beber (um pouco de fome e Velocidade), mas o destino é o barril.',
    'guide.mistech.cachaca.2': 'O §lBarril de Fermentação§r (madeira turva) guarda até 16 garrafas de um tipo só. Garapa fermenta em §lmeio dia§r; Cachaça envelhece em §lum dia§r. Botão direito põe uma (agachado: a mão toda); mão vazia mostra o tempo ou tira quando está pronto.',
    'guide.mistech.cachaca.3': 'O §lAlambique§r destila Garapa Fermentada em Cachaça (10 s cada). Calor: §cfogo, fogueira acesa, lava ou magma embaixo§r (de graça), uma §cRuna de Fogo§r no espaço de runa, ou 20 FE/t.',
    'guide.mistech.cachaca.4': '§6Cachaça Envelhecida§r: um dia no barril. Efeitos de 3 minutos e sobe menos.',
    'guide.mistech.cachaca.5': 'Toda cachaça dá Força I + Resistência I e um pouco de Náusea; três seguidas = porre (Lentidão e Fadiga).\n\n§2Cachaça de Arruda§r (+2 folhas de arruda): -5 de Mácula.\n§6Cachaça com Mel§r (+mel): Regeneração, cura veneno.\n\n§lOferenda ao Saci§r: botão direito no seu Saci com qualquer cachaça — dobra o tempo que ele fica alimentado e ele dá um achado na hora.',
    'guide.mistech.entry.arcane_infuser': 'Infusor Arcano',
    'guide.mistech.arcane_infuser.1': 'Uma mesa de encantamento onde §lvocê escolhe§r. Ponha o item (ou um livro), lápis e frascos de Essência Impura; funciona com energia (guarda 100.000 FE).',
    'guide.mistech.arcane_infuser.2': 'A lista mostra os encantos que o item aceita. Escolha um, o nível com − / + e aperte §lInfundir§r.\n\nCusto do nível L: §a3 + 3L níveis de XP§r, §d5.000 × L FE§r, §9L lápis§r e §5L frascos de Essência Impura§r (os frascos vazios voltam).',
    'guide.mistech.ritual.greater_arcane_infuser': 'Infusor Maior', 'guide.mistech.ritual.astral_arcane_infuser': 'Infusor Astral',
    'guide.mistech.arcane_infuser.3': 'Nível máximo:\n§7Infusor§r até II\n§5Maior§r o máximo normal\n§bAstral§r máximo + 1 e encantos de tesouro (Remendo, Passos Gelados...)\n\nPassar do máximo custa também um §eFragmento Estelar§r e um frasco de §bEssência Purificada§r. Maldições nunca.',
})
print('ok')
