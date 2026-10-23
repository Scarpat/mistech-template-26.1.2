"""Entradas do Grimório da leva de Qualidade de Vida (19/10/2026).

Uso (na raiz do projeto):
  python3 tools/qol/guide_entries.py      # escreve as páginas no book.json e atualiza o journey.py
  python3 tools/guide/journey.py
  python3 tools/guide/views.py
Pode rodar de novo: substitui as entradas pelo id.
"""
import json
import re

BOOK = 'src/main/resources/assets/mistech/guide/book.json'
JOURNEY = 'tools/guide/journey.py'


def e(eid, icon, pages):
    return {'id': eid, 'icon': icon, 'name': 'guide.mistech.entry.' + eid, 'pages': pages}


def t(key):
    return {'type': 'text', 'text': key}


def craft(grid, result, text=None, count=None):
    p = {'type': 'crafting', 'grid': grid, 'result': result}
    if text:
        p['text'] = text
    if count:
        p['count'] = count
    return p


def write(ingredients, chalk, result):
    return {'type': 'writing', 'ingredients': ingredients, 'chalk': chalk, 'result': result}


M = '#c:ingots/mithril'
SHARD = 'mistech:impure_magic_shard'

NEW = {
    'utilities': [
        e('scroll_case', 'mistech:scroll_case', [
            craft(['minecraft:leather', 'minecraft:string', 'minecraft:leather', 'minecraft:leather', 'minecraft:paper', 'minecraft:leather',
                   'minecraft:leather', 'minecraft:gold_ingot', 'minecraft:leather'], 'mistech:scroll_case'),
            t('guide.mistech.scroll_case.1'), t('guide.mistech.scroll_case.2')]),
    ],
    'astrology': [
        e('guide_stone_projection', 'mistech:guide_stone', [
            {'type': 'spotlight', 'item': 'mistech:guide_stone', 'text': 'guide.mistech.guide_stone_projection.1'},
            t('guide.mistech.guide_stone_projection.2'), t('guide.mistech.guide_stone_projection.3')]),
    ],
    'machines2': [
        e('machine_config', 'minecraft:redstone_torch', [
            {'type': 'spotlight', 'item': 'minecraft:hopper', 'text': 'guide.mistech.machine_config.1'},
            t('guide.mistech.machine_config.2'), t('guide.mistech.machine_config.3')]),
        e('runic_upgrades', 'mistech:speed_upgrade', [
            craft(['minecraft:copper_ingot', 'minecraft:paper', 'minecraft:copper_ingot', 'minecraft:paper', 'minecraft:amethyst_shard',
                   'minecraft:paper', 'minecraft:copper_ingot', 'minecraft:paper', 'minecraft:copper_ingot'], 'mistech:runic_plate',
                  'guide.mistech.runic_upgrades.plate', 4),
            t('guide.mistech.runic_upgrades.1'),
            write(['mistech:runic_plate', 'mistech:time_rune', 'minecraft:sugar', 'minecraft:redstone'], 10, 'mistech:speed_upgrade'),
            write(['mistech:runic_plate', 'mistech:energy_rune', 'minecraft:lapis_lazuli', 'minecraft:gold_ingot'], 10, 'mistech:efficiency_upgrade'),
            write(['mistech:runic_plate', 'mistech:space_rune', 'minecraft:ender_pearl', 'minecraft:compass'], 10, 'mistech:range_upgrade'),
            t('guide.mistech.runic_upgrades.2')]),
    ],
    'machines': [
        e('arcane_furnace', 'mistech:arcane_furnace', [
            craft([M, SHARD, M, 'mistech:runic_furnace', 'mistech:fire_rune', 'mistech:runic_furnace', M, 'mistech:energy_rune', M],
                  'mistech:arcane_furnace'),
            t('guide.mistech.arcane_furnace.1')]),
    ],
    'essence': [
        e('arcane_extractor', 'mistech:arcane_extractor', [
            craft([M, 'mistech:runic_tank', M, SHARD, 'mistech:essence_extractor', SHARD, M, 'mistech:energy_rune', M],
                  'mistech:arcane_extractor'),
            t('guide.mistech.arcane_extractor.1')]),
        e('arcane_compressor', 'mistech:arcane_compressor', [
            craft([M, 'mistech:earth_rune', M, SHARD, 'mistech:magic_compressor', SHARD, M, 'mistech:energy_rune', M],
                  'mistech:arcane_compressor'),
            t('guide.mistech.arcane_compressor.1')]),
    ],
}

# páginas novas em entradas que já existem (id → páginas acrescentadas no fim)
EXTRA_PAGES = {
    'welcome': [t('guide.mistech.welcome.search')],
    'essence_extractor': [t('guide.mistech.essence_extractor.energy')],
    'magic_compressor': [t('guide.mistech.magic_compressor.reserve')],
}

# onde entram na ordem de cada capítulo (depois de qual entrada) e dependências
AFTER = {'scroll_case': 'runic_elevator', 'guide_stone_projection': 'guide_stone', 'machine_config': 'runic_crusher',
         'runic_upgrades': 'machine_config', 'arcane_furnace': 'condenser', 'arcane_extractor': 'essence_extractor',
         'arcane_compressor': 'magic_compressor'}
DEPS = {'scroll_case': ['writing_table'], 'guide_stone_projection': ['guide_stone'], 'machine_config': ['runic_crusher'],
        'runic_upgrades': ['machine_config', 'time_rune'], 'arcane_furnace': ['runic_furnace', 'realm_ores'],
        'arcane_extractor': ['essence_extractor', 'realm_ores'], 'arcane_compressor': ['magic_compressor', 'realm_ores']}
# ajustes de dependência (pontos de travamento): a bicicleta precisa de um gnomo; a primeira energia barata é o Gerador de Cinzas
FIX_DEPS = {'gnome_bicycle': ['energy_rune', 'gnomes'], 'essence_extractor': ['ash_generator', 'light_rune', 'darkness_rune']}
READ = ['guide_stone_projection', 'machine_config']
TASK = {'runic_upgrades': ['mistech:runic_plate', 'mistech:speed_upgrade', 'mistech:efficiency_upgrade', 'mistech:range_upgrade']}


def book():
    with open(BOOK, encoding='utf-8') as f:
        b = json.load(f)
    cats = {c['id']: c for c in b['categories']}
    new_ids = {x['id'] for lst in NEW.values() for x in lst}
    for c in b['categories']:
        c['entries'] = [x for x in c['entries'] if x['id'] not in new_ids]
    for cid, lst in NEW.items():
        cats[cid]['entries'].extend(lst)
    for c in b['categories']:
        for entry in c['entries']:
            if entry['id'] in EXTRA_PAGES:
                for page in EXTRA_PAGES[entry['id']]:
                    if page not in entry['pages']:
                        entry['pages'].append(page)
    with open(BOOK, 'w', encoding='utf-8') as f:
        json.dump(b, f, indent=2, ensure_ascii=False)
        f.write('\n')


def journey():
    with open(JOURNEY, encoding='utf-8') as f:
        s = f.read()
    for eid, after in AFTER.items():
        if "'" + eid + "'" in s.split('# ---------------------------------------------------------------- dependências')[0]:
            continue
        s = s.replace("'" + after + "'", "'" + after + "', '" + eid + "'", 1)
    block = ''.join("    '%s': %s,\n" % (k, v) for k, v in DEPS.items() if "    '%s':" % k not in s)
    if block:
        s = s.replace("    'primordial_heart': ['guardian', 'pale_heart', 'infernal_heart'],\n",
                      "    'primordial_heart': ['guardian', 'pale_heart', 'infernal_heart'],\n    # qualidade de vida (19/10)\n" + block, 1)
    for k, v in FIX_DEPS.items():
        s = re.sub(r"'%s': \[[^\]]*\]" % k, "'%s': %s" % (k, v), s, count=1)
    for r in READ:
        if "'" + r + "'" not in s.split('READ = {')[1].split('}')[0]:
            s = s.replace("READ = {", "READ = {'" + r + "', ", 1)
    for k, v in TASK.items():
        if "'" + k + "':" not in s.split('TASK = {')[1].split('}')[0]:
            s = s.replace("TASK = {", "TASK = {'%s': %s,\n        " % (k, v), 1)
    with open(JOURNEY, 'w', encoding='utf-8') as f:
        f.write(s)


if __name__ == '__main__':
    book()
    journey()
    print('ok')
