import sys; sys.path.insert(0, 'tools/pendencias2')
from res import *
NAMES = {'mithril': ('Mithril', 'Mithril', 'Bloco de Mithril', 'Isqueiro de Mithril'),
         'adamantine': ('Adamantine', 'Adamantina', 'Bloco de Adamantina', 'Isqueiro de Adamantina'),
         'orichalcum': ('Orichalcum', 'Oricalco', 'Bloco de Oricalco', 'Isqueiro de Oricalco')}
en, pt = {}, {}
for m, (en_n, pt_n, pt_block, pt_lighter) in NAMES.items():
    b = m + '_block'; f = m + '_flint_and_steel'
    simple_block(b)
    flat_item(f, parent='minecraft:item/handheld')
    recipe(b, {'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['###', '###', '###'],
               'key': {'#': '#c:ingots/' + m}, 'result': {'id': 'mistech:' + b, 'count': 1}})
    recipe(m + '_ingot_from_' + b, {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'group': m + '_ingot',
               'ingredients': ['mistech:' + b], 'result': {'id': 'mistech:' + m + '_ingot', 'count': 9}})
    recipe(f, {'type': 'minecraft:crafting_shapeless', 'category': 'tools',
               'ingredients': ['minecraft:flint', '#c:ores/' + m], 'result': {'id': 'mistech:' + f, 'count': 1}})
    tag('c/tags/block/storage_blocks/' + m, 'mistech:' + b)
    tag('c/tags/block/storage_blocks', '#c:storage_blocks/' + m)
    tag('c/tags/item/storage_blocks/' + m, 'mistech:' + b)
    tag('c/tags/item/storage_blocks', '#c:storage_blocks/' + m)
    tag('minecraft/tags/block/mineable/pickaxe', 'mistech:' + b)
    tag('minecraft/tags/block/beacon_base_blocks', 'mistech:' + b)
    tag('minecraft/tags/block/needs_iron_tool' if m == 'mithril' else 'minecraft/tags/block/needs_diamond_tool', 'mistech:' + b)
    tag('minecraft/tags/item/beacon_payment_items', 'mistech:' + m + '_ingot')
    tag('minecraft/tags/item/creeper_igniters', 'mistech:' + f)
    tag('c/tags/item/tools/igniter', 'mistech:' + f)
    en['block.mistech.' + b] = 'Block of ' + en_n
    en['item.mistech.' + f] = en_n + ' Flint and Steel'
    pt['block.mistech.' + b] = pt_block
    pt['item.mistech.' + f] = pt_lighter
lang(en, pt)
print('ok')
