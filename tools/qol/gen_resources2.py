"""Recursos (JSON) da leva 2 (20/10/2026): Triturador Arcano, pós, Liquefator, Forja Trituradora, Pó de Mandrágora,
Flechas de Beladona. Uso: python3 tools/qol/gen_resources2.py (na raiz do projeto)."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_resources import A, D, add_tag, block_item, facing_states, loot, shaped, simple_item, write  # noqa: E402


def recipe(name, data):
    write(os.path.join(D, 'mistech', 'recipe', name + '.json'), data)


def cooking(name, kind, ingredient, result, time, xp):
    recipe(name, {'type': 'minecraft:' + kind, 'category': 'misc', 'ingredient': ingredient, 'result': {'id': result},
                  'experience': xp, 'cookingtime': time})


def crushing(name, ingredient, result, count, bonus, rune, tier=1):
    data = {'type': 'mistech:crushing', 'ingredient': ingredient, 'result': {'id': result, 'count': count}, 'bonus_chance': bonus,
            'rune_bonus': rune}
    if tier > 1:
        data['tier'] = tier
    recipe('crushing/' + name, data)


def machines():
    # Triturador Arcano: o modelo do Triturador Rúnico com pedra-negra, ouro e placa arcana
    for suffix in ['', '_on']:
        with open(os.path.join(A, 'models', 'block', 'runic_crusher' + suffix + '.json'), encoding='utf-8') as f:
            model = json.load(f)
        tex = model['textures']
        tex.update({'base': 'minecraft:block/polished_blackstone', 'copper': 'mistech:block/arcane_crusher_trim',
                    'cb': 'minecraft:block/gold_block', 'plate': 'mistech:block/arcane_crusher_plate' + suffix,
                    'particle': 'minecraft:block/polished_blackstone'})
        write(os.path.join(A, 'models', 'block', 'arcane_crusher' + suffix + '.json'), model)
    # Liquefator: caldeirão índigo cheio de essência
    for suffix in ['', '_on']:
        write(os.path.join(A, 'models', 'block', 'liquefier' + suffix + '.json'), {
            'parent': 'minecraft:block/template_cauldron_full', 'textures': {
                'particle': 'mistech:block/liquefier_side', 'top': 'mistech:block/liquefier_top' + suffix,
                'bottom': 'mistech:block/liquefier_bottom' + suffix, 'side': 'mistech:block/liquefier_side' + suffix,
                'inside': 'mistech:block/liquefier_inner' + suffix, 'content': 'mistech:block/impure_essence_still'}})
    with open(os.path.join(A, 'models', 'item', 'essence_extractor.json'), encoding='utf-8') as f:
        icon = json.load(f)
    icon['parent'] = 'minecraft:block/template_cauldron_full'
    icon['textures'] = {'particle': 'mistech:block/liquefier_side', 'top': 'mistech:block/liquefier_top_on',
                        'bottom': 'mistech:block/liquefier_bottom_on', 'side': 'mistech:block/liquefier_side_on',
                        'inside': 'mistech:block/liquefier_inner_on', 'content': 'mistech:block/impure_essence_still'}
    write(os.path.join(A, 'models', 'item', 'liquefier.json'), icon)
    write(os.path.join(A, 'items', 'liquefier.json'), {'model': {'type': 'minecraft:model', 'model': 'mistech:item/liquefier'}})
    # Núcleo da Forja (o revestimento do cubo é o Bloco de Magia Impura; a textura arcane_casing ficou só nos modelos)
    for suffix in ['', '_on']:
        write(os.path.join(A, 'models', 'block', 'crusher_forge' + suffix + '.json'), {'parent': 'minecraft:block/orientable', 'textures': {
            'top': 'mistech:block/crusher_forge_top', 'front': 'mistech:block/crusher_forge_front' + suffix, 'side': 'mistech:block/arcane_casing'}})
    for name in ['arcane_crusher', 'crusher_forge']:
        facing_states(name)
        block_item(name)
    facing_states('liquefier')
    names = ['arcane_crusher', 'liquefier', 'crusher_forge']
    for name in names:
        loot(name)
    add_tag(os.path.join(D, 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json'), ['mistech:' + n for n in names])
    add_tag(os.path.join(D, 'minecraft', 'tags', 'block', 'needs_iron_tool.json'), ['mistech:' + n for n in names])

    shaped('arcane_crusher', ['MTM', 'ACA', 'MEM'], {'M': '#c:ingots/mithril', 'T': 'mistech:earth_rune', 'A': 'mistech:impure_magic_shard',
                                                    'C': 'mistech:runic_crusher', 'E': 'mistech:energy_rune'})
    shaped('liquefier', ['OCO', 'FBF', 'OEO'], {'O': 'minecraft:obsidian', 'C': 'minecraft:cauldron', 'F': 'mistech:fire_rune',
                                               'B': 'minecraft:blast_furnace', 'E': 'mistech:energy_rune'})
    shaped('impure_magic_block', ['SRS', 'RSR', 'SRS'], {'S': 'mistech:impure_magic_shard', 'R': 'mistech:magic_residue'})
    shaped('crusher_forge', ['CEC', 'FBT', 'CAC'], {'C': '#c:ingots/mithril', 'E': 'mistech:energy_rune', 'F': 'mistech:fire_rune',
                                                   'B': 'minecraft:blast_furnace', 'T': 'mistech:earth_rune', 'A': 'mistech:impure_magic_block'})


def metals():
    times = {'mithril': (100, 0.7), 'adamantine': (200, 1.0), 'orichalcum': (300, 1.5)}
    values = []
    for metal, (time, xp) in times.items():
        dust = 'mistech:' + metal + '_dust'
        simple_item(metal + '_dust')
        add_tag(os.path.join(D, 'c', 'tags', 'item', 'dusts', metal + '.json'), [dust])
        values.append('#c:dusts/' + metal)
        cooking(metal + '_ingot_from_dust_smelting', 'smelting', dust, 'mistech:' + metal + '_ingot', time, xp)
        cooking(metal + '_ingot_from_dust_blasting', 'blasting', dust, 'mistech:' + metal + '_ingot', time // 2, xp)
        crushing(metal + '_ore', '#c:ores/' + metal, dust, 2, 0.25, True, 2)
        crushing('raw_' + metal, '#c:raw_materials/' + metal, dust, 1, 0.35, True, 2)
    add_tag(os.path.join(D, 'c', 'tags', 'item', 'dusts.json'), values)


def herbs():
    simple_item('mandrake_powder')
    crushing('mandrake_root', 'mistech:mandrake_root', 'mistech:mandrake_powder', 3, 0.25, False)
    recipe('belladonna_arrow', {
        'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': ['AAA', 'ABA', 'AAA'],
        'key': {'A': 'minecraft:arrow', 'B': 'mistech:belladonna_berries'},
        # flechas com poção duram 1/8 do efeito (POTION_DURATION_SCALE): 1600 ticks = 10 s no alvo
        'result': {'id': 'minecraft:tipped_arrow', 'count': 8, 'components': {'minecraft:potion_contents': {
            'custom_color': 0x7A3FC8, 'custom_name': 'belladonna',
            'custom_effects': [{'id': 'minecraft:poison', 'duration': 1600}, {'id': 'minecraft:nausea', 'duration': 1600},
                               {'id': 'minecraft:slowness', 'duration': 800}]}}}})


if __name__ == '__main__':
    machines()
    metals()
    herbs()
    print('ok')
    # os estados "formed" da Forja Trituradora (sobrescreve os blockstates simples gerados acima)
    import forge_model  # noqa: E402
    forge_model.main()
