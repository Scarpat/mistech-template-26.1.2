# Gera as outras formas de organizar o Grimório ("views" no book.json): por progressão e por dimensão.
# Rodar DEPOIS do tools/guide/journey.py (usa as dependências e tarefas que ele grava).
# Uso: python3 tools/guide/views.py [--dry]
import json, sys
from guide_lib import external_links, layout as lib_layout, overlaps

BOOK = 'src/main/resources/assets/mistech/guide/book.json'
LANG = 'src/main/resources/assets/mistech/lang/'
b = json.load(open(BOOK, encoding='utf-8'))
entries = {}
order = []
for c in b['categories']:
    for e in c['entries']:
        entries[e['id']] = e
        order.append(e['id'])
DEPS = {k: e.get('deps', []) for k, e in entries.items()}

# ---------------------------------------------------------------- por progressão
# Etapa = a maior entre: a etapa mínima do marco (STAGE_OF), o tier de altar dos rituais da entrada e a etapa das dependências.
STAGES = [
    ('p_start', 'minecraft:oak_sapling'),     # 0 primeiros passos
    ('p_runes', 'mistech:writing_table'),     # 1 runas e máquinas simples
    ('p_essence', 'mistech:magic_compressor'),  # 2 essência impura
    ('p_tier1', 'mistech:impure_altar'),      # 3 rituais
    ('p_tier2', 'mistech:impure_altar_2'),    # 4 altar tier II
    ('p_tier3', 'mistech:impure_altar_3'),    # 5 tier III e fim de jogo
]
STAGE_OF = {'writing_table': 1, 'runic_furnace': 1, 'magic_compressor': 2, 'impure_altar': 3, 'altar_tier2': 4, 'tier2_rituals': 4,
            'altar_tier3': 5, 'tier3_rituals': 5, 'adamantine_gear': 4, 'rune_anvil': 4, 'orichalcum_gear': 5,
            'pure_orichalcum_gear': 5, 'primordial_heart': 5, 'sanctuary': 4, 'astral_heart': 5}
# entradas cujos rituais são extras (não definem quando a entrada aparece)
IGNORE_RITUALS = {'natal_chart'}
EARLY = {}
stage = {}


def st(n):
    if n not in stage:
        s = STAGE_OF.get(n, 0)
        for p in entries[n]['pages']:
            if p.get('type') == 'ritual' and not p.get('sanctuary') and n not in IGNORE_RITUALS:
                s = max(s, {1: 3, 2: 4, 3: 5}[p.get('min_tier', 1)])
        for d in DEPS[n]:
            s = max(s, st(d))
        stage[n] = EARLY.get(n, s)
    return stage[n]


for n in order:
    st(n)
progress = [(cid, icon, [n for n in order if stage[n] == i]) for i, (cid, icon) in enumerate(STAGES)]

# ---------------------------------------------------------------- por dimensão (onde você precisa ir)
WHERE = {
    'd_realm': ('mistech:dreamcatcher', ['dreams', 'gnome_realm', 'realm_ores', 'metal_blocks', 'dream_layers', 'common_dream', 'nightmare',
                                         'goblin_village', 'goblin_crowns', 'deep_dream', 'lucid_dreamcatcher', 'spatial_dreamcatcher',
                                         'rem_sleep', 'the_dreamer', 'fairy_incense']),
    'd_deep_dark': ('minecraft:sculk_catalyst', ['echo_collector', 'echo_compass', 'runic_note_block', 'silence_seal', 'echoling',
                                                 'guardian', 'echo_rune']),
    'd_pale': ('minecraft:pale_oak_sapling', ['pale_sentinel', 'pale_chalk', 'eyeblossom_incense', 'pale_heart', 'vigil_rune']),
    'd_nether': ('minecraft:netherrack', ['igneous_essence', 'infernal_heart', 'piglin_pact', 'runic_strider', 'infernal_tools',
                                          'blaze_generator', 'igneous_boiler', 'soul_rune', 'primordial_heart']),
    'd_arcane': ('mistech:tarot_card_the_star', ['arcane_plane', 'the_fool', 'the_magician', 'the_high_priestess', 'arcana_bond']),
    'd_between': ('mistech:spatial_anchor', ['spatial_anchor', 'translocation_orb', 'portal_anchor', 'spatial_ring']),
}
taken = {n for _, ids in WHERE.values() for n in ids}
assert taken <= set(entries), taken - set(entries)
# o Mundo Superior é grande demais para um mapa só: um capítulo por etapa da progressão
OVERWORLD_ICONS = ['minecraft:grass_block', 'minecraft:oak_log', 'minecraft:amethyst_cluster', 'minecraft:crying_obsidian',
                   'minecraft:lodestone', 'minecraft:beacon']
dimension = []
for i, (cid, _) in enumerate(STAGES):
    dimension.append(('d_overworld_' + cid[2:], OVERWORLD_ICONS[i], [n for n in order if n not in taken and stage[n] == i]))
for cid in ('d_between', 'd_realm', 'd_deep_dark', 'd_pale', 'd_nether', 'd_arcane'):
    icon, ids = WHERE[cid]
    dimension.append((cid, icon, [n for n in order if n in ids]))


# ---------------------------------------------------------------- layout (o mesmo do journey.py, com as ligações entre capítulos)
BIG = {k for k, e in entries.items() if e.get('size') == 'big'}
problems = []


def build(chapters):
    out = []
    for cid, icon, ids in chapters:
        if not ids:
            continue
        links = external_links(ids, DEPS)
        big = BIG - set(links)
        pos = lib_layout(ids, DEPS, links, big)
        problems.extend((cid,) + p for p in overlaps(pos, big))
        es = []
        for n in ids:
            ref = {'id': n, 'x': pos[n][0], 'y': pos[n][1]}
            if n in BIG:
                ref['size'] = 'big'
            es.append(ref)
        out.append({'id': cid, 'icon': icon, 'name': 'guide.mistech.cat.' + cid, 'description': 'guide.mistech.cat.' + cid + '.desc',
                    'links': [{'id': l, 'x': pos[l][0], 'y': pos[l][1]} for l in links], 'entries': es})
    return out


views = {'progress': build(progress), 'dimension': build(dimension)}
for v in views.values():
    seen = [e['id'] for c in v for e in c['entries']]
    assert sorted(seen) == sorted(order), set(order) ^ set(seen)

if problems:
    print('AVISO, nós sobrepostos:', problems)
if '--dry' in sys.argv:
    for name, v in views.items():
        print('\n#####', name)
        for c in v:
            print('==', c['id'], len(c['entries']), ' '.join(e['id'] for e in c['entries']), '| ligações:', len(c['links']))
else:
    b['views'] = views
    with open(BOOK, 'w', encoding='utf-8') as f:
        json.dump(b, f, ensure_ascii=False, indent=2)
        f.write('\n')
    print('ok:', {k: [len(c['entries']) for c in v] for k, v in views.items()})
