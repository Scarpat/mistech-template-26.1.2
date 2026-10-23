"""Recursos (JSON) da leva de Qualidade de Vida: máquinas arcanas, melhorias, Porta-Pergaminhos.

Uso: python3 tools/qol/gen_resources.py   (na raiz do projeto)
"""
import json
import os

RES = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources')
A = os.path.join(RES, 'assets', 'mistech')
D = os.path.join(RES, 'data')


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


def add_tag(path, values):
    data = {'values': []}
    if os.path.exists(path):
        with open(path, encoding='utf-8') as f:
            data = json.load(f)
    for v in values:
        if v not in data['values']:
            data['values'].append(v)
    write(path, data)


def facing_states(name, on_suffix='_on'):
    variants = {}
    for facing, rot in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]:
        for lit in ['false', 'true']:
            m = {'model': 'mistech:block/' + name + (on_suffix if lit == 'true' else '')}
            if rot:
                m['y'] = rot
            variants['facing=%s,lit=%s' % (facing, lit)] = m
    write(os.path.join(A, 'blockstates', name + '.json'), {'variants': variants})


def block_item(name):
    write(os.path.join(A, 'items', name + '.json'), {'model': {'type': 'minecraft:model', 'model': 'mistech:block/' + name}})


def simple_item(name):
    write(os.path.join(A, 'items', name + '.json'), {'model': {'type': 'minecraft:model', 'model': 'mistech:item/' + name}})
    write(os.path.join(A, 'models', 'item', name + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mistech:item/' + name}})


def loot(name):
    write(os.path.join(D, 'mistech', 'loot_table', 'blocks', name + '.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'mistech:' + name}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': 'mistech:blocks/' + name})


def shaped(name, pattern, key, count=1):
    write(os.path.join(D, 'mistech', 'recipe', name + '.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
        'result': {'id': 'mistech:' + name, 'count': count}})


def writing(name, ingredients, chalk):
    write(os.path.join(D, 'mistech', 'recipe', 'writing', name + '.json'), {
        'type': 'mistech:writing', 'ingredients': ingredients, 'chalk_damage': chalk,
        'result': {'id': 'mistech:' + name, 'count': 1}})


def machines():
    # Fornalha Arcana: orientable recolorida
    write(os.path.join(A, 'models', 'block', 'arcane_furnace.json'), {'parent': 'minecraft:block/orientable', 'textures': {
        'top': 'mistech:block/arcane_furnace_top', 'front': 'mistech:block/arcane_furnace_front', 'side': 'mistech:block/arcane_furnace_side'}})
    write(os.path.join(A, 'models', 'block', 'arcane_furnace_on.json'), {'parent': 'minecraft:block/orientable', 'textures': {
        'top': 'mistech:block/arcane_furnace_top', 'front': 'mistech:block/arcane_furnace_front_on', 'side': 'mistech:block/arcane_furnace_side'}})
    # Extrator Arcano: caldeirão cheio com veios dourados
    for suffix, on in [('', ''), ('_on', '_on')]:
        write(os.path.join(A, 'models', 'block', 'arcane_extractor' + suffix + '.json'), {
            'parent': 'minecraft:block/template_cauldron_full', 'textures': {
                'particle': 'mistech:block/arcane_extractor_side',
                'top': 'mistech:block/arcane_extractor_top' + on, 'bottom': 'mistech:block/arcane_extractor_bottom' + on,
                'side': 'mistech:block/arcane_extractor_side' + on, 'inside': 'mistech:block/arcane_extractor_inner' + on,
                'content': 'mistech:block/impure_essence_still'}})
    # Compressor Arcano: mesmo desenho do Compressor de Magia (cheio), pedra índigo
    for suffix in ['', '_on']:
        write(os.path.join(A, 'models', 'block', 'arcane_compressor' + suffix + '.json'), {
            'parent': 'mistech:block/magic_compressor_filled', 'textures': {
                'particle': 'mistech:block/arcane_compressor_body', 'body': 'mistech:block/arcane_compressor_body',
                'top': 'mistech:block/arcane_compressor_top',
                'essence': 'mistech:block/impure_essence_still' if suffix else 'minecraft:block/amethyst_block'}})
    for name in ['arcane_furnace', 'arcane_extractor', 'arcane_compressor']:
        facing_states(name)
        block_item(name)
        loot(name)
    # caldeirão não tem "display" de item: ícone com as posições do Extrator de Essência
    with open(os.path.join(A, 'models', 'item', 'essence_extractor.json'), encoding='utf-8') as f:
        icon = json.load(f)
    icon['parent'] = 'minecraft:block/template_cauldron_full'
    icon['textures'] = {k: v.replace('essence_extractor', 'arcane_extractor') for k, v in icon['textures'].items()}
    icon['textures']['content'] = 'mistech:block/impure_essence_still'
    write(os.path.join(A, 'models', 'item', 'arcane_extractor.json'), icon)
    write(os.path.join(A, 'items', 'arcane_extractor.json'), {'model': {'type': 'minecraft:model', 'model': 'mistech:item/arcane_extractor'}})
    add_tag(os.path.join(D, 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json'),
            ['mistech:arcane_furnace', 'mistech:arcane_extractor', 'mistech:arcane_compressor'])
    add_tag(os.path.join(D, 'minecraft', 'tags', 'block', 'needs_iron_tool.json'),
            ['mistech:arcane_furnace', 'mistech:arcane_extractor', 'mistech:arcane_compressor'])

    shaped('arcane_furnace', ['MAM', 'FRF', 'MEM'], {
        'M': '#c:ingots/mithril', 'A': 'mistech:impure_magic_shard', 'F': 'mistech:runic_furnace',
        'R': 'mistech:fire_rune', 'E': 'mistech:energy_rune'})
    shaped('arcane_extractor', ['MTM', 'AXA', 'MEM'], {
        'M': '#c:ingots/mithril', 'T': 'mistech:runic_tank', 'A': 'mistech:impure_magic_shard',
        'X': 'mistech:essence_extractor', 'E': 'mistech:energy_rune'})
    shaped('arcane_compressor', ['MWM', 'ACA', 'MEM'], {
        'M': '#c:ingots/mithril', 'W': 'mistech:earth_rune', 'A': 'mistech:impure_magic_shard',
        'C': 'mistech:magic_compressor', 'E': 'mistech:energy_rune'})


def upgrades():
    for name in ['runic_plate', 'speed_upgrade', 'efficiency_upgrade', 'range_upgrade']:
        simple_item(name)
    shaped('runic_plate', ['CPC', 'PAP', 'CPC'], {'C': 'minecraft:copper_ingot', 'P': 'minecraft:paper', 'A': 'minecraft:amethyst_shard'}, 4)
    writing('speed_upgrade', ['mistech:runic_plate', 'mistech:time_rune', 'minecraft:sugar', 'minecraft:redstone'], 10)
    writing('efficiency_upgrade', ['mistech:runic_plate', 'mistech:energy_rune', 'minecraft:lapis_lazuli', 'minecraft:gold_ingot'], 10)
    writing('range_upgrade', ['mistech:runic_plate', 'mistech:space_rune', 'minecraft:ender_pearl', 'minecraft:compass'], 10)


def scroll_case():
    simple_item('scroll_case')
    shaped('scroll_case', ['LSL', 'LPL', 'LGL'], {
        'L': 'minecraft:leather', 'S': 'minecraft:string', 'P': 'minecraft:paper', 'G': 'minecraft:gold_ingot'})


if __name__ == '__main__':
    machines()
    upgrades()
    scroll_case()
    print('ok')
    # os estados "formed" da Forja Trituradora (sobrescreve os blockstates simples gerados acima)
    import forge_model  # noqa: E402
    forge_model.main()
