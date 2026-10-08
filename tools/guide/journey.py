# Gera a "jornada" do Grimório: ordem dos capítulos, dependências, tarefas e posições no mapa.
# Uso (na pasta do projeto):  python3 tools/guide/journey.py          -> reescreve o book.json
#                             python3 tools/guide/journey.py --dry    -> só mostra o mapa em texto
# Para mudar a jornada, edite CHAPTERS / DEPS / MANUAL abaixo e rode de novo.
import json, sys

BOOK = 'src/main/resources/assets/mistech/guide/book.json'
b = json.load(open(BOOK, encoding='utf-8'))
entries = {}
for c in b['categories']:
    for e in c['entries']:
        assert e['id'] not in entries, e['id']
        entries[e['id']] = e
cats = {c['id']: c for c in b['categories']}

# ---------------------------------------------------------------- capítulos (ordem da jornada)
CHAPTERS = [
    ('basics', None, ['welcome', 'path']),
    ('incense', None, ['incense', 'incense_holder', 'ash', 'ash_chalk', 'flower_incenses', 'magic_incense', 'magic_residue', 'impure_chalk',
                         'fairy_incense']),
    ('herbalism', None, ['crops', 'cigar', 'joint', 'pipe', 'kettle', 'coffee', 'mystic_sight', 'murky_tree', 'dreams',
                         'runic_ashtray', 'magic_smoke', 'rune_roller', 'runic_pipe']),
    ('runes', None, ['writing_table', 'fire_rune', 'water_rune', 'earth_rune', 'air_rune', 'ice_rune', 'energy_rune', 'light_rune',
                     'darkness_rune', 'space_rune', 'time_rune', 'magma_rune', 'vapor_rune', 'void_rune', 'soul_rune', 'vigil_rune',
                     'amber_seal', 'luck_rune', 'arcane_rune', 'echo_rune', 'cloud_rune', 'life_rune']),
    ('machines', None, ['runic_furnace', 'bottled_lightning', 'gnome_bicycle', 'condenser', 'arcane_network']),
    ('essence', None, ['essence_extractor', 'impure_essence', 'magic_compressor', 'impure_magic_block']),
    ('realm', {'icon': 'mistech:withered_grass', 'name': 'guide.mistech.cat.realm', 'description': 'guide.mistech.cat.realm.desc'},
     ['gnomes', 'gnome_staff', 'gnome_realm', 'realm_ores', 'metal_blocks', 'goblin_village', 'goblin_crowns']),
    ('rituals', None, ['impure_altar', 'ritual_circle', 'ritual_styles', 'familiar_rituals', 'world_rituals', 'impure_tools', 'mist_pipe',
                       'celestial_events', 'impure_heart', 'awakened_heart', 'altar_tier2', 'tier2_rituals', 'mithril_gear',
                       'adamantine_gear', 'rune_anvil', 'astral_heart', 'altar_tier3', 'tier3_rituals', 'orichalcum_gear',
                       'pure_orichalcum_gear', 'spatial_anchor', 'translocation_orb']),
    ('artifacts', None, ['runic_artifacts', 'wind_boots', 'frost_skates', 'storm_gauntlet', 'volcanic_hammer', 'tide_mask',
                         'beacon_crown', 'cloud_broom', 'shadow_cloak', 'tide_spear', 'void_magnet', 'pocket_hourglass', 'spatial_ring']),
    ('astrology', None, ['guide_stone', 'astrology_field', 'astrology_table', 'constellations', 'natal_chart', 'natal_realign', 'astral_pillar']),
    ('wear', None, ['gnome_hat', 'mystic_robe', 'comfy_pants', 'slippers']),
    ('automation', None, ['spatial_barrel', 'runic_tank', 'carryable_barrel', 'rune_confector', 'archive_core', 'archive_terminal',
                          'archive_network', 'archive_mirror']),
    ('machines2', None, ['runic_crusher', 'ash_generator', 'astral_generator', 'runic_battery', 'advanced_runic_battery', 'pocket_battery',
                         'farming_machines', 'tunneler', 'miner', 'gnome_boost', 'transmission_pylon',
                         'ethereal_network']),
    ('golems', None, ['clay_golem', 'familiars', 'golem_runes', 'golem_sorter', 'golem_collector', 'golem_messenger', 'golem_crafter',
                      'golem_keeper']),
    ('accessories', None, ['accessory_bases', 'rings', 'amulets']),
    ('purity', None, ['taint', 'dowsing_pendulum', 'rue', 'salt', 'tainted_soil', 'distiller', 'crystallizer', 'pure_generator', 'sanctuary',
                      'sanctuary_blessings', 'sanctuary_rituals', 'sanctuary_sky']),
    ('folklore', None, ['folklore_intro', 'saci', 'raven', 'curupira', 'werewolf', 'boitata', 'headless_mule']),
    ('beyond', None, ['beyond_intro', 'echo_collector', 'echo_compass', 'runic_note_block', 'silence_seal', 'echoling', 'guardian',
                      'pale_sentinel', 'pale_chalk', 'eyeblossom_incense', 'pale_heart', 'igneous_essence', 'infernal_heart', 'piglin_pact',
                      'runic_strider', 'infernal_tools', 'blaze_generator', 'portal_anchor', 'primordial_heart']),
]

# ---------------------------------------------------------------- dependências (o que precisa estar concluído antes)
DEPS = {
    'welcome': [], 'path': ['welcome'],
    'incense': ['path'], 'incense_holder': ['incense'], 'ash': ['incense'], 'ash_chalk': ['ash'], 'flower_incenses': ['incense_holder'],
    'magic_incense': ['ash', 'magic_compressor'], 'magic_residue': ['magic_incense'], 'impure_chalk': ['magic_residue'],
    'crops': ['path'], 'cigar': ['crops'], 'joint': ['crops'], 'pipe': ['crops'], 'kettle': ['crops', 'fire_rune'],
    'mystic_sight': ['kettle'], 'coffee': ['kettle'], 'murky_tree': ['mystic_sight'], 'dreams': ['murky_tree', 'magic_residue'],
    'runic_ashtray': ['cigar', 'ice_rune'], 'magic_smoke': ['cigar', 'magic_residue'], 'rune_roller': ['magic_smoke', 'joint'],
    'runic_pipe': ['pipe', 'gnome_staff', 'impure_altar'],
    'writing_table': ['ash_chalk'],
    'fire_rune': ['writing_table'], 'water_rune': ['writing_table'], 'earth_rune': ['writing_table'], 'air_rune': ['writing_table'],
    'ice_rune': ['writing_table'], 'energy_rune': ['writing_table'], 'light_rune': ['writing_table'], 'darkness_rune': ['writing_table'],
    'space_rune': ['writing_table'], 'time_rune': ['writing_table'],
    'magma_rune': ['fire_rune', 'earth_rune'], 'vapor_rune': ['fire_rune', 'water_rune'], 'void_rune': ['space_rune', 'darkness_rune'],
    'soul_rune': ['fire_rune'], 'vigil_rune': ['darkness_rune'], 'amber_seal': ['fire_rune'],
    'luck_rune': ['time_rune', 'impure_altar'], 'arcane_rune': ['energy_rune', 'impure_altar'],
    'echo_rune': ['air_rune', 'darkness_rune', 'impure_altar'],
    'cloud_rune': ['vapor_rune', 'air_rune', 'altar_tier2'],
    'life_rune': ['time_rune', 'water_rune', 'earth_rune', 'altar_tier2'],
    'fairy_incense': ['incense_holder', 'goblin_village'],
    'goblin_crowns': ['goblin_village'],
    'runic_artifacts': ['mithril_gear'],
    'golem_runes': ['clay_golem'], 'golem_sorter': ['golem_runes'], 'golem_collector': ['golem_runes'],
    'golem_messenger': ['golem_runes', 'realm_ores'], 'golem_crafter': ['golem_runes'], 'golem_keeper': ['golem_runes', 'archive_terminal'],
    'archive_core': ['spatial_barrel', 'realm_ores'], 'archive_terminal': ['archive_core'], 'archive_network': ['archive_terminal'],
    'archive_mirror': ['archive_terminal'],
    'transmission_pylon': ['arcane_network', 'adamantine_gear'], 'ethereal_network': ['transmission_pylon', 'advanced_runic_battery', 'altar_tier3'],
    'wind_boots': ['runic_artifacts', 'air_rune', 'altar_tier2'], 'frost_skates': ['runic_artifacts', 'ice_rune'],
    'storm_gauntlet': ['runic_artifacts', 'energy_rune', 'bottled_lightning'],
    'volcanic_hammer': ['runic_artifacts', 'adamantine_gear', 'magma_rune'], 'tide_mask': ['runic_artifacts', 'adamantine_gear', 'water_rune'],
    'beacon_crown': ['runic_artifacts', 'adamantine_gear', 'light_rune'],
    'cloud_broom': ['runic_artifacts', 'orichalcum_gear', 'cloud_rune'], 'shadow_cloak': ['runic_artifacts', 'orichalcum_gear', 'void_rune'],
    'tide_spear': ['runic_artifacts', 'orichalcum_gear', 'water_rune'],
    'void_magnet': ['runic_artifacts', 'accessory_bases', 'void_rune', 'altar_tier2'],
    'pocket_hourglass': ['runic_artifacts', 'accessory_bases', 'time_rune', 'altar_tier2'],
    'spatial_ring': ['runic_artifacts', 'accessory_bases', 'spatial_anchor', 'altar_tier2'],
    'runic_furnace': ['fire_rune'], 'bottled_lightning': ['energy_rune'], 'gnome_bicycle': ['energy_rune'],
    'condenser': ['runic_furnace', 'ice_rune'], 'arcane_network': ['gnome_bicycle', 'space_rune'],
    'essence_extractor': ['gnome_bicycle', 'light_rune', 'darkness_rune'], 'impure_essence': ['essence_extractor'],
    'magic_compressor': ['impure_essence', 'incense_holder'], 'impure_magic_block': ['magic_compressor', 'magic_residue'],
    'gnomes': ['murky_tree'], 'gnome_staff': ['gnomes'], 'gnome_realm': ['dreams'], 'realm_ores': ['gnome_realm'],
    'goblin_village': ['gnome_realm'], 'metal_blocks': ['realm_ores'],
    'spatial_anchor': ['ritual_circle', 'space_rune'], 'translocation_orb': ['spatial_anchor', 'altar_tier2'],
    'natal_realign': ['natal_chart', 'altar_tier2'],
    'impure_altar': ['impure_magic_block', 'murky_tree'], 'ritual_circle': ['impure_altar'], 'ritual_styles': ['ritual_circle'],
    'familiar_rituals': ['ritual_circle'], 'world_rituals': ['ritual_circle'], 'impure_tools': ['ritual_circle'],
    'mist_pipe': ['ritual_circle', 'vapor_rune'], 'celestial_events': ['world_rituals'],
    'impure_heart': ['ritual_circle', 'space_rune'], 'awakened_heart': ['impure_heart', 'luck_rune', 'realm_ores'],
    'altar_tier2': ['awakened_heart'], 'tier2_rituals': ['altar_tier2'],
    'mithril_gear': ['impure_tools', 'arcane_rune', 'realm_ores'], 'adamantine_gear': ['mithril_gear'], 'rune_anvil': ['adamantine_gear'],
    'astral_heart': ['awakened_heart', 'void_rune'], 'altar_tier3': ['astral_heart', 'altar_tier2'],
    'tier3_rituals': ['altar_tier3', 'celestial_events'], 'orichalcum_gear': ['adamantine_gear', 'tier3_rituals'],
    'pure_orichalcum_gear': ['orichalcum_gear', 'crystallizer'],
    'guide_stone': ['magic_compressor'], 'astrology_field': ['guide_stone'], 'astrology_table': ['astrology_field'],
    'constellations': ['astrology_table'], 'natal_chart': ['constellations', 'impure_heart'], 'astral_pillar': ['natal_chart'],
    'gnome_hat': ['magic_compressor', 'air_rune'], 'mystic_robe': ['gnome_hat', 'ice_rune'], 'comfy_pants': ['gnome_hat'],
    'slippers': ['mystic_robe'],
    'spatial_barrel': ['magic_compressor', 'space_rune'], 'runic_tank': ['spatial_barrel', 'water_rune'],
    'carryable_barrel': ['spatial_barrel'], 'rune_confector': ['runic_tank', 'guide_stone'],
    'runic_crusher': ['runic_furnace', 'earth_rune'], 'ash_generator': ['runic_furnace'], 'astral_generator': ['ash_generator', 'light_rune'],
    'runic_battery': ['bottled_lightning', 'magic_residue'], 'advanced_runic_battery': ['runic_battery', 'void_rune'],
    'pocket_battery': ['advanced_runic_battery', 'arcane_rune'], 'farming_machines': ['runic_crusher', 'murky_tree'],
    'tunneler': ['runic_crusher'], 'miner': ['tunneler', 'spatial_barrel', 'guide_stone'], 'gnome_boost': ['runic_crusher', 'gnomes'],
    'clay_golem': ['time_rune'], 'familiars': ['clay_golem', 'gnomes'],
    'accessory_bases': ['magic_compressor'], 'rings': ['accessory_bases'], 'amulets': ['accessory_bases'],
    'taint': ['impure_altar'], 'dowsing_pendulum': ['taint'], 'rue': ['taint'], 'salt': ['taint'], 'tainted_soil': ['taint'],
    'distiller': ['salt', 'realm_ores'], 'crystallizer': ['distiller', 'light_rune'], 'pure_generator': ['crystallizer'],
    'sanctuary': ['crystallizer'], 'sanctuary_blessings': ['sanctuary'], 'sanctuary_rituals': ['sanctuary'],
    'sanctuary_sky': ['sanctuary', 'celestial_events'],
    'folklore_intro': ['ritual_circle'], 'saci': ['folklore_intro', 'bottled_lightning'], 'raven': ['folklore_intro'],
    'curupira': ['folklore_intro', 'sanctuary_blessings'], 'werewolf': ['folklore_intro'],
    'boitata': ['folklore_intro', 'magma_rune', 'celestial_events'], 'headless_mule': ['werewolf', 'celestial_events'],
    'beyond_intro': ['impure_heart'], 'echo_collector': ['beyond_intro'], 'echo_compass': ['echo_collector'],
    'runic_note_block': ['echo_collector'], 'silence_seal': ['echo_collector'], 'echoling': ['echo_collector'],
    'guardian': ['echo_compass', 'echo_rune'],
    'pale_sentinel': ['beyond_intro'], 'pale_chalk': ['pale_sentinel'], 'eyeblossom_incense': ['pale_sentinel'],
    'pale_heart': ['pale_sentinel', 'vigil_rune'],
    'igneous_essence': ['beyond_intro'], 'infernal_heart': ['igneous_essence', 'soul_rune', 'magma_rune'],
    'piglin_pact': ['igneous_essence', 'soul_rune'], 'runic_strider': ['igneous_essence', 'soul_rune'],
    'infernal_tools': ['igneous_essence', 'impure_tools'], 'blaze_generator': ['igneous_essence', 'runic_furnace'],
    'portal_anchor': ['beyond_intro', 'space_rune'],
    'primordial_heart': ['guardian', 'pale_heart', 'infernal_heart'],
}

# ---------------------------------------------------------------- tarefas
# Padrão: ter no inventário o item do ícone (se for do Mistech). Entradas de leitura: basta abrir.
READ = {'welcome', 'path', 'ritual_styles', 'realm_ores', 'astrology_table', 'rune_confector', 'miner', 'familiars', 'beyond_intro',
        'pale_sentinel', 'celestial_events', 'tier2_rituals', 'tier3_rituals', 'taint'}


def results(eid):
    out = []
    for p in entries[eid]['pages']:
        for f in ('result', 'item'):
            v = p.get(f, '')
            if v.startswith('mistech:') and v not in out:
                out.append(v)
    return out


# qualquer um destes itens conclui (None = todos os resultados das páginas)
ANY = {'metal_blocks': None, 'impure_essence': None, 'rings': None, 'amulets': None, 'flower_incenses': None, 'arcane_network': None,
       'accessory_bases': None, 'impure_tools': None, 'infernal_tools': None, 'sanctuary_rituals': None,
       'sanctuary_blessings': None, 'farming_machines': None,
       'ritual_circle': ['mistech:ritual_pedestal'], 'kettle': ['mistech:kettle']}

# marcos da jornada (nós grandes)
BIG = {'runic_artifacts', 'welcome', 'incense', 'crops', 'writing_table', 'runic_furnace', 'magic_compressor', 'gnome_realm', 'impure_altar', 'altar_tier2',
       'altar_tier3', 'astrology_table', 'spatial_barrel', 'runic_crusher', 'clay_golem', 'accessory_bases', 'sanctuary',
       'folklore_intro', 'beyond_intro', 'primordial_heart'}

import math


def _polar(angle, radius):
    r = math.radians(angle)
    return (round(math.cos(r) * radius, 2), round(math.sin(r) * radius, 2))


# Runas: um círculo arcano em volta da Mesa de Escrita; as runas compostas ficam no anel de fora.
_RING = ['earth_rune', 'fire_rune', 'water_rune', 'ice_rune', 'time_rune', 'light_rune', 'energy_rune', 'air_rune',
         'darkness_rune', 'space_rune']
_runes = {'writing_table': (0, 0)}
for _i, _r in enumerate(_RING):
    _runes[_r] = _polar(-126 + _i * 36, 2.3)
_runes.update({
    'magma_rune': _polar(-110, 4.2), 'soul_rune': _polar(-90, 4.2), 'vapor_rune': _polar(-70, 4.2), 'amber_seal': _polar(-90, 5.6),
    'luck_rune': _polar(18, 4.2), 'arcane_rune': _polar(90, 4.2), 'echo_rune': _polar(144, 4.2),
    'vigil_rune': _polar(168, 4.2), 'void_rune': _polar(194, 4.2), 'cloud_rune': _polar(-50, 5.6), 'life_rune': _polar(30, 5.6),
})

# posições feitas à mão (x, y em células); capítulos fora daqui são distribuídos automaticamente
# Rituais: o círculo à esquerda; em cima a linha do céu, no meio as armas e armaduras, embaixo os corações e altares.
_rituals = {
    'impure_altar': (0, 3), 'ritual_circle': (1.6, 3),
    'ritual_styles': (3.2, 0), 'familiar_rituals': (3.2, 1), 'world_rituals': (3.2, 2), 'impure_tools': (3.2, 3),
    'mist_pipe': (3.2, 4), 'impure_heart': (3.2, 5.5), 'spatial_anchor': (1.6, 4.4),
    'mithril_gear': (4.8, 3), 'awakened_heart': (4.8, 5.5),
    'adamantine_gear': (6.4, 3), 'rune_anvil': (6.4, 4.2), 'altar_tier2': (6.4, 5.5), 'astral_heart': (6.4, 6.9),
    'celestial_events': (8, 1.5), 'tier2_rituals': (8, 5.5), 'altar_tier3': (8, 6.9), 'translocation_orb': (8, 4.2),
    'tier3_rituals': (9.6, 4.4), 'orichalcum_gear': (11.2, 3), 'pure_orichalcum_gear': (12.8, 3),
}

# Além: três ramos (Escuro Profundo, Jardim Pálido, Nether) que se juntam no Coração Primordial.
_beyond = {
    'beyond_intro': (0, 4), 'portal_anchor': (0, 6),
    'echo_collector': (1.6, 1), 'echo_compass': (3.2, -0.5), 'runic_note_block': (3.2, 0.5), 'silence_seal': (3.2, 1.5),
    'echoling': (3.2, 2.5), 'guardian': (4.8, -0.5),
    'pale_sentinel': (1.6, 4), 'pale_chalk': (3.2, 3.2), 'eyeblossom_incense': (3.2, 4.8), 'pale_heart': (4.8, 4),
    'igneous_essence': (1.6, 8.2), 'infernal_heart': (3.2, 6.2), 'piglin_pact': (3.2, 7.2), 'runic_strider': (3.2, 8.2),
    'infernal_tools': (3.2, 9.2), 'blaze_generator': (3.2, 10.2),
    'primordial_heart': (6.4, 3.5),
}

# Artefatos: uma coluna por base (mitril, adamantina, oricalco) e os acessórios no fim.
_artifacts = {
    'runic_artifacts': (0, 2),
    'wind_boots': (1.6, 1), 'frost_skates': (1.6, 2), 'storm_gauntlet': (1.6, 3),
    'volcanic_hammer': (3.2, 1), 'tide_mask': (3.2, 2), 'beacon_crown': (3.2, 3),
    'cloud_broom': (4.8, 1), 'shadow_cloak': (4.8, 2), 'tide_spear': (4.8, 3),
    'void_magnet': (6.4, 1), 'pocket_hourglass': (6.4, 2), 'spatial_ring': (6.4, 3),
}

MANUAL = {'runes': _runes, 'rituals': _rituals, 'beyond': _beyond, 'artifacts': _artifacts}

# ---------------------------------------------------------------- validação
order = [eid for _, _, ids in CHAPTERS for eid in ids]
missing = set(entries) - set(order)
extra = set(order) - set(entries)
assert not missing and not extra, (missing, extra)
assert len(order) == len(set(order))
for k, v in DEPS.items():
    for d in v:
        assert d in entries, (k, d)
assert set(DEPS) == set(entries), set(entries) ^ set(DEPS)
state = {}


def visit(n, path=()):
    if state.get(n) == 2:
        return
    assert state.get(n) != 1, 'ciclo: ' + ' > '.join(path + (n,))
    state[n] = 1
    for d in DEPS[n]:
        visit(d, path + (n,))
    state[n] = 2


for n in entries:
    visit(n)


# ---------------------------------------------------------------- layout automático por camadas
def layout(ids):
    inside = set(ids)
    depth = {}

    def dep(n):
        if n not in depth:
            ps = [p for p in DEPS[n] if p in inside]
            depth[n] = 0 if not ps else 1 + max(dep(p) for p in ps)
        return depth[n]

    for n in ids:
        dep(n)
    pos = {}
    for d in range(max(depth.values()) + 1):
        col = [n for n in ids if depth[n] == d]

        def want(n):
            ps = [pos[p][1] for p in DEPS[n] if p in pos]
            return sum(ps) / len(ps) if ps else None

        col.sort(key=lambda n: (want(n) if want(n) is not None else -1000, ids.index(n)))
        wants = [want(n) for n in col]
        placed = []
        for i, n in enumerate(col):
            w = wants[i] if wants[i] is not None else (placed[-1] + 1 if placed else 0)
            if placed and w < placed[-1] + 1:
                w = placed[-1] + 1
            placed.append(w)
        known = [w for w in wants if w is not None]
        target = sum(known) / len(known) if known else 0
        shift = target - sum(placed) / len(placed)
        # só sobe o grupo (nunca empurra para baixo do que já foi resolvido)
        if shift < 0:
            placed = [y + shift for y in placed]
        for n, y in zip(col, placed):
            pos[n] = (d * 1.6, y)
    return pos


newcats = []
for cid, meta, ids in CHAPTERS:
    c = dict(cats[cid]) if cid in cats else dict({'id': cid}, **meta)
    pos = dict(MANUAL[cid]) if cid in MANUAL else layout(ids)
    assert set(pos) == set(ids), set(ids) ^ set(pos)
    mx = min(x for x, _ in pos.values())
    my = min(y for _, y in pos.values())
    pos = {k: (round(x - mx, 2), round(y - my, 2)) for k, (x, y) in pos.items()}
    es = []
    for eid in ids:
        e = dict(entries[eid])
        if eid in READ or not e['icon'].startswith('mistech:'):
            e['task'] = {'type': 'read'}
        elif eid in ANY:
            e['task'] = {'type': 'item', 'items': ANY[eid] or results(eid)}
        else:
            e['task'] = {'type': 'item', 'items': [e['icon']]}
        e['deps'] = DEPS[eid]
        e['x'], e['y'] = pos[eid]
        e.pop('size', None)
        if eid in BIG:
            e['size'] = 'big'
        ordered = {k: e[k] for k in ('id', 'icon', 'name', 'deps', 'task', 'x', 'y', 'size') if k in e}
        ordered['pages'] = e['pages']
        es.append(ordered)
    c['entries'] = es
    newcats.append({k: c[k] for k in ('id', 'icon', 'name', 'description', 'entries')})
b['categories'] = newcats

if '--dry' in sys.argv:
    for c in newcats:
        print('\n==', c['id'])
        grid = {}
        for e in c['entries']:
            grid[(round(e['y'] * 2), round(e['x'] / 1.6 * 1))] = e['id'][:12] + ('*' if e.get('size') else '')
        rows = [r for r, _ in grid]
        cols = [k for _, k in grid]
        for r in range(min(rows), max(rows) + 1):
            line = ''.join((grid.get((r, k), '')).ljust(14) for k in range(min(cols), max(cols) + 1))
            print(line.rstrip() if line.strip() else '')
else:
    with open(BOOK, 'w', encoding='utf-8') as f:
        json.dump(b, f, ensure_ascii=False, indent=2)
        f.write('\n')
    print('ok:', len(order), 'entradas em', len(newcats), 'capítulos')
