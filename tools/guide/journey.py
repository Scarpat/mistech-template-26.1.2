# Gera a "jornada" do Grimório: ordem dos capítulos, dependências, tarefas, posições no mapa e as ligações
# entre capítulos. É a ÚNICA fonte da organização do livro: não edite deps/x/y/task no book.json à mão.
# Uso (na pasta do projeto):  python3 tools/guide/journey.py          -> reescreve o book.json
#                             python3 tools/guide/journey.py --dry    -> só mostra o mapa em texto
# Depois rode tools/guide/views.py (organizações por progressão e por dimensão).
# Entrada nova = escrever as páginas no book.json (em qualquer capítulo), pôr o id em CHAPTERS e em DEPS e rodar.
import json, sys, math
from guide_lib import external_links, layout, place_links, overlaps

BOOK = 'src/main/resources/assets/mistech/guide/book.json'
b = json.load(open(BOOK, encoding='utf-8'))
entries = {}
for c in b['categories']:
    for e in c['entries']:
        assert e['id'] not in entries, e['id']
        entries[e['id']] = e
cats = {c['id']: c for c in b['categories']}


def meta(cid, icon):
    return {'icon': icon, 'name': 'guide.mistech.cat.' + cid, 'description': 'guide.mistech.cat.' + cid + '.desc'}


# ---------------------------------------------------------------- capítulos (ordem da jornada)
# (id, metadados se o capítulo for novo ou mudar de ícone, entradas na ordem de leitura)
CHAPTERS = [
    ('basics', None, ['welcome', 'path']),
    ('incense', None, ['incense', 'incense_holder', 'ash', 'ash_chalk', 'flower_incenses', 'fairy_incense']),
    ('herbalism', None, ['crops', 'cigar', 'joint', 'pipe', 'kettle', 'coffee', 'coffee_uses', 'mystic_sight', 'belladonna_uses', 'cachaca', 'mandrake', 'mandrake_uses', 'murky_tree',
                         'runic_ashtray', 'magic_smoke', 'rune_roller', 'runic_pipe', 'runic_hookah']),
    ('utilities', meta('utilities', 'mistech:growing_pot'), ['growing_pot', 'creature_jar', 'hammock', 'runic_elevator', 'scroll_case']),
    ('runes', None, ['writing_table', 'fire_rune', 'water_rune', 'earth_rune', 'air_rune', 'ice_rune', 'energy_rune', 'light_rune',
                     'darkness_rune', 'space_rune', 'time_rune', 'magma_rune', 'vapor_rune', 'void_rune', 'soul_rune', 'vigil_rune',
                     'amber_seal', 'luck_rune', 'arcane_rune', 'echo_rune', 'cloud_rune', 'life_rune']),
    ('machines', None, ['runic_furnace', 'bottled_lightning', 'gnome_bicycle', 'condenser', 'arcane_furnace', 'arcane_network']),
    ('essence', None, ['essence_extractor', 'arcane_extractor', 'impure_essence', 'magic_compressor', 'arcane_compressor', 'liquefier', 'magic_incense', 'magic_residue', 'impure_chalk',
                       'impure_magic_block', 'arcane_infuser']),
    ('realm', meta('realm', 'mistech:dreamcatcher'),
     ['gnomes', 'gnome_staff', 'dreams', 'gnome_realm', 'realm_ores', 'metal_blocks', 'dream_layers', 'common_dream', 'dream_fragments',
      'nightmare', 'goblin_village', 'goblin_crowns', 'deep_dream', 'lucid_dreamcatcher', 'spatial_dreamcatcher', 'rem_sleep',
      'the_dreamer']),
    ('rituals', None, ['impure_altar', 'ritual_circle', 'ritual_styles', 'familiar_rituals', 'world_rituals', 'impure_tools', 'mist_pipe',
                       'celestial_events', 'impure_heart', 'awakened_heart', 'altar_tier2', 'tier2_rituals', 'mithril_gear',
                       'adamantine_gear', 'rune_anvil', 'astral_heart', 'altar_tier3', 'tier3_rituals', 'orichalcum_gear',
                       'pure_orichalcum_gear', 'spatial_anchor', 'translocation_orb']),
    ('artifacts', None, ['runic_artifacts', 'wind_boots', 'frost_skates', 'storm_gauntlet', 'volcanic_hammer', 'tide_mask',
                         'beacon_crown', 'cloud_broom', 'shadow_cloak', 'tide_spear', 'void_magnet', 'pocket_hourglass', 'spatial_ring']),
    ('archery', meta('archery', 'mistech:murky_bow'), ['murky_bow', 'rune_arrows', 'gear_bows', 'arcane_crossbow']),
    ('spells', meta('spells', 'mistech:spell_scroll'), ['spell_scrolls', 'inscription_table', 'apprentice_tome']),
    ('astrology', None, ['guide_stone', 'guide_stone_projection', 'astrology_field', 'astrology_table', 'constellations', 'natal_chart', 'natal_realign', 'astral_pillar']),
    ('tarot', None, ['tarot_circle', 'tarot_table', 'tarot_cards', 'arcane_plane', 'the_fool', 'the_magician', 'the_high_priestess',
                     'arcana_bond']),
    ('wear', None, ['gnome_hat', 'mystic_robe', 'comfy_pants', 'slippers', 'magic_loom', 'mage_robes']),
    ('automation', None, ['spatial_barrel', 'runic_tank', 'carryable_barrel', 'rune_confector', 'archive_core', 'archive_terminal',
                          'archive_network', 'archive_mirror']),
    ('machines2', None, ['runic_crusher', 'machine_config', 'runic_upgrades', 'arcane_crusher', 'crusher_forge', 'ash_generator', 'astral_generator', 'runic_battery', 'advanced_runic_battery', 'pocket_battery',
                         'astral_pocket_battery', 'farming_machines', 'tunneler', 'miner', 'gnome_boost', 'transmission_pylon',
                         'ethereal_network', 'experience_generator', 'impure_reactor', 'igneous_boiler', 'stellar_observatory']),
    ('golems', None, ['clay_golem', 'familiars', 'golem_runes', 'runic_golem_wand', 'golem_sorter', 'golem_collector', 'golem_messenger',
                      'golem_crafter', 'golem_keeper']),
    ('accessories', None, ['accessory_bases', 'rings', 'amulets']),
    ('purity', None, ['taint', 'dowsing_pendulum', 'seeking_pendulum', 'rue', 'salt', 'tainted_soil', 'distiller', 'crystallizer',
                      'pure_generator', 'sanctuary', 'sanctuary_blessings', 'sanctuary_rituals', 'sanctuary_sky']),
    ('folklore', None, ['folklore_intro', 'saci', 'raven', 'curupira', 'werewolf', 'silver_dagger', 'headless_mule', 'boitata',
                        'boitata_armor', 'boitata_egg', 'boitata_mount_gear', 'ancestral_forest', 'matinta_pereira', 'cuca', 'caipora',
                        'iara', 'mapinguari']),
    ('beyond', None, ['beyond_intro', 'echo_collector', 'echo_compass', 'runic_note_block', 'silence_seal', 'echoling', 'guardian',
                      'pale_sentinel', 'pale_chalk', 'eyeblossom_incense', 'pale_heart', 'igneous_essence', 'infernal_heart', 'piglin_pact',
                      'runic_strider', 'infernal_tools', 'blaze_generator', 'portal_anchor', 'primordial_heart']),
]

# ---------------------------------------------------------------- dependências (o que precisa estar concluído antes)
DEPS = {
    # primeiros passos, incenso
    'welcome': [], 'path': ['welcome'],
    'incense': ['path'], 'incense_holder': ['incense'], 'ash': ['incense'], 'ash_chalk': ['ash'], 'flower_incenses': ['incense_holder'],
    'fairy_incense': ['incense_holder', 'goblin_village'],
    # herbologia
    'crops': ['path'], 'cigar': ['crops'], 'joint': ['crops'], 'pipe': ['crops'], 'kettle': ['crops', 'fire_rune'],
    'mystic_sight': ['kettle'], 'coffee': ['kettle'], 'cachaca': ['kettle', 'murky_tree'], 'mandrake': ['crops'],
    'murky_tree': ['mystic_sight'],
    'runic_ashtray': ['cigar', 'ice_rune'], 'magic_smoke': ['cigar', 'magic_residue'], 'rune_roller': ['magic_smoke', 'joint'],
    'runic_pipe': ['pipe', 'gnome_staff', 'impure_altar'], 'runic_hookah': ['runic_pipe'],
    # utilidades
    'growing_pot': ['path'], 'creature_jar': ['path'], 'hammock': ['crops'], 'runic_elevator': ['air_rune'],
    # runas
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
    # máquinas, essência
    'runic_furnace': ['fire_rune'], 'bottled_lightning': ['energy_rune'], 'gnome_bicycle': ['energy_rune', 'gnomes'],
    'condenser': ['runic_furnace', 'ice_rune'], 'arcane_network': ['gnome_bicycle', 'space_rune'],
    'essence_extractor': ['ash_generator', 'light_rune', 'darkness_rune'], 'impure_essence': ['essence_extractor'],
    'magic_compressor': ['impure_essence', 'incense_holder'],
    'magic_incense': ['magic_compressor', 'ash'], 'magic_residue': ['magic_incense'], 'impure_chalk': ['magic_residue'],
    'impure_magic_block': ['magic_compressor', 'magic_residue'], 'arcane_infuser': ['impure_magic_block', 'arcane_rune'],
    # gnomos e o sonho
    'gnomes': ['murky_tree'], 'gnome_staff': ['gnomes'],
    'spell_scrolls': ['gnome_staff'], 'inscription_table': ['spell_scrolls', 'writing_table'], 'apprentice_tome': ['spell_scrolls'],
    'dreams': ['murky_tree', 'magic_residue'], 'gnome_realm': ['dreams'], 'realm_ores': ['gnome_realm'], 'metal_blocks': ['realm_ores'],
    'dream_layers': ['gnome_realm'], 'common_dream': ['dream_layers'], 'dream_fragments': ['common_dream', 'ritual_circle'],
    'nightmare': ['common_dream'], 'goblin_village': ['nightmare'], 'goblin_crowns': ['goblin_village'],
    'deep_dream': ['nightmare'], 'lucid_dreamcatcher': ['deep_dream'], 'spatial_dreamcatcher': ['deep_dream', 'space_rune'],
    'rem_sleep': ['deep_dream'], 'the_dreamer': ['rem_sleep', 'lucid_dreamcatcher'],
    # rituais
    'impure_altar': ['impure_magic_block', 'murky_tree'], 'ritual_circle': ['impure_altar'], 'ritual_styles': ['ritual_circle'],
    'familiar_rituals': ['ritual_circle'], 'world_rituals': ['ritual_circle'], 'impure_tools': ['ritual_circle'],
    'mist_pipe': ['ritual_circle', 'vapor_rune'], 'celestial_events': ['world_rituals'],
    'impure_heart': ['ritual_circle', 'space_rune'], 'awakened_heart': ['impure_heart', 'luck_rune', 'realm_ores'],
    'altar_tier2': ['awakened_heart'], 'tier2_rituals': ['altar_tier2'],
    'mithril_gear': ['impure_tools', 'arcane_rune', 'realm_ores'], 'adamantine_gear': ['mithril_gear'], 'rune_anvil': ['adamantine_gear'],
    'astral_heart': ['awakened_heart', 'void_rune'], 'altar_tier3': ['astral_heart', 'altar_tier2'],
    'tier3_rituals': ['altar_tier3', 'celestial_events'], 'orichalcum_gear': ['adamantine_gear', 'tier3_rituals'],
    'pure_orichalcum_gear': ['orichalcum_gear', 'crystallizer'],
    'spatial_anchor': ['ritual_circle', 'space_rune'], 'translocation_orb': ['spatial_anchor', 'altar_tier2'],
    # artefatos, arquearia
    'runic_artifacts': ['mithril_gear'],
    'wind_boots': ['runic_artifacts', 'air_rune', 'altar_tier2'], 'frost_skates': ['runic_artifacts', 'ice_rune'],
    'storm_gauntlet': ['runic_artifacts', 'energy_rune', 'bottled_lightning'],
    'volcanic_hammer': ['runic_artifacts', 'adamantine_gear', 'magma_rune'], 'tide_mask': ['runic_artifacts', 'adamantine_gear', 'water_rune'],
    'beacon_crown': ['runic_artifacts', 'adamantine_gear', 'light_rune'],
    'cloud_broom': ['runic_artifacts', 'orichalcum_gear', 'cloud_rune'], 'shadow_cloak': ['runic_artifacts', 'orichalcum_gear', 'void_rune'],
    'tide_spear': ['runic_artifacts', 'orichalcum_gear', 'water_rune'],
    'void_magnet': ['runic_artifacts', 'accessory_bases', 'void_rune', 'altar_tier2'],
    'pocket_hourglass': ['runic_artifacts', 'accessory_bases', 'time_rune', 'altar_tier2'],
    'spatial_ring': ['runic_artifacts', 'accessory_bases', 'spatial_anchor', 'altar_tier2'],
    'murky_bow': ['murky_tree'], 'rune_arrows': ['murky_bow', 'writing_table'], 'gear_bows': ['murky_bow', 'mithril_gear'],
    'arcane_crossbow': ['gear_bows', 'runic_artifacts', 'adamantine_gear'],
    # astrologia, tarô
    'guide_stone': ['magic_compressor'], 'astrology_field': ['guide_stone'], 'astrology_table': ['astrology_field'],
    'constellations': ['astrology_table'], 'natal_chart': ['constellations', 'impure_heart'], 'astral_pillar': ['natal_chart'],
    'natal_realign': ['natal_chart', 'altar_tier2'],
    'tarot_circle': ['astrology_table'], 'tarot_table': ['tarot_circle'], 'tarot_cards': ['tarot_table'], 'arcane_plane': ['tarot_cards'],
    'the_fool': ['arcane_plane'], 'the_magician': ['the_fool'], 'the_high_priestess': ['the_magician'],
    'arcana_bond': ['arcane_plane', 'pure_orichalcum_gear'],
    # vestes
    'gnome_hat': ['magic_compressor', 'air_rune'], 'mystic_robe': ['gnome_hat', 'ice_rune'], 'comfy_pants': ['gnome_hat'],
    'slippers': ['mystic_robe'], 'magic_loom': ['gnome_hat', 'energy_rune'], 'mage_robes': ['magic_loom'],
    # automação
    'spatial_barrel': ['magic_compressor', 'space_rune'], 'runic_tank': ['spatial_barrel', 'water_rune'],
    'carryable_barrel': ['spatial_barrel'], 'rune_confector': ['runic_tank', 'guide_stone'],
    'archive_core': ['spatial_barrel', 'realm_ores'], 'archive_terminal': ['archive_core'], 'archive_network': ['archive_terminal'],
    'archive_mirror': ['archive_terminal'],
    # máquinas II
    'runic_crusher': ['runic_furnace', 'earth_rune'], 'ash_generator': ['runic_furnace'], 'astral_generator': ['ash_generator', 'light_rune'],
    'runic_battery': ['bottled_lightning', 'magic_residue'], 'advanced_runic_battery': ['runic_battery', 'void_rune'],
    'pocket_battery': ['advanced_runic_battery', 'arcane_rune'], 'astral_pocket_battery': ['pocket_battery', 'ethereal_network'],
    'farming_machines': ['runic_crusher', 'murky_tree'],
    'tunneler': ['runic_crusher'], 'miner': ['tunneler', 'spatial_barrel', 'guide_stone'], 'gnome_boost': ['runic_crusher', 'gnomes'],
    'transmission_pylon': ['arcane_network', 'adamantine_gear'],
    'ethereal_network': ['transmission_pylon', 'advanced_runic_battery', 'altar_tier3'],
    'experience_generator': ['ash_generator', 'mithril_gear'], 'impure_reactor': ['ash_generator', 'impure_heart'],
    'igneous_boiler': ['ash_generator', 'blaze_generator', 'magma_rune'], 'stellar_observatory': ['astral_generator', 'orichalcum_gear'],
    # golems, acessórios
    'clay_golem': ['time_rune'], 'familiars': ['clay_golem', 'gnomes'],
    'golem_runes': ['clay_golem'], 'runic_golem_wand': ['golem_runes', 'space_rune'], 'golem_sorter': ['golem_runes'],
    'golem_collector': ['golem_runes'], 'golem_messenger': ['golem_runes', 'realm_ores'], 'golem_crafter': ['golem_runes'],
    'golem_keeper': ['golem_runes', 'archive_terminal'],
    'accessory_bases': ['magic_compressor'], 'rings': ['accessory_bases'], 'amulets': ['accessory_bases'],
    # pureza
    'taint': ['impure_altar'], 'dowsing_pendulum': ['taint'], 'seeking_pendulum': ['dowsing_pendulum', 'goblin_village'],
    'rue': ['taint'], 'salt': ['taint'], 'tainted_soil': ['taint'],
    'distiller': ['salt', 'realm_ores'], 'crystallizer': ['distiller', 'light_rune'], 'pure_generator': ['crystallizer'],
    'sanctuary': ['crystallizer'], 'sanctuary_blessings': ['sanctuary'], 'sanctuary_rituals': ['sanctuary'],
    'sanctuary_sky': ['sanctuary', 'celestial_events'],
    # folclore
    'folklore_intro': ['ritual_circle'], 'saci': ['folklore_intro', 'bottled_lightning'], 'raven': ['folklore_intro'],
    'curupira': ['folklore_intro', 'sanctuary_blessings'], 'werewolf': ['folklore_intro'],
    'silver_dagger': ['werewolf', 'mithril_gear'], 'headless_mule': ['werewolf', 'celestial_events'],
    'boitata': ['folklore_intro', 'magma_rune', 'celestial_events'], 'boitata_armor': ['boitata'],
    'boitata_egg': ['boitata', 'altar_tier3'], 'boitata_mount_gear': ['boitata_egg'],
    'ancestral_forest': ['folklore_intro'], 'matinta_pereira': ['ancestral_forest'], 'cuca': ['matinta_pereira'],
    'caipora': ['ancestral_forest'], 'iara': ['ancestral_forest'], 'mapinguari': ['ancestral_forest'],
    # além do overworld
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
    # qualidade de vida (19/10)
    'liquefier': ['magic_compressor', 'fire_rune'],
    'arcane_crusher': ['runic_crusher', 'realm_ores'],
    'crusher_forge': ['arcane_crusher', 'arcane_furnace', 'guide_stone'],
    'belladonna_uses': ['mystic_sight'],
    'coffee_uses': ['coffee'],
    'mandrake_uses': ['mandrake'],
    # qualidade de vida (19/10)
    'scroll_case': ['writing_table'],
    'guide_stone_projection': ['guide_stone'],
    'machine_config': ['runic_crusher'],
    'runic_upgrades': ['machine_config', 'time_rune'],
    'arcane_furnace': ['runic_furnace', 'realm_ores'],
    'arcane_extractor': ['essence_extractor', 'realm_ores'],
    'arcane_compressor': ['magic_compressor', 'realm_ores'],
}

# ---------------------------------------------------------------- tarefas
# Padrão: ter no inventário o item do ícone (se for do Mistech). Entradas de leitura: basta abrir.
READ = {'mandrake_uses', 'coffee_uses', 'belladonna_uses', 'machine_config', 'guide_stone_projection', 'welcome', 'path', 'ritual_styles', 'realm_ores', 'astrology_table', 'rune_confector', 'miner', 'familiars', 'beyond_intro',
        'pale_sentinel', 'celestial_events', 'tier2_rituals', 'tier3_rituals', 'taint', 'dream_layers', 'nightmare',
        'tarot_circle', 'arcane_plane', 'arcana_bond', 'ancestral_forest', 'caipora'}

# itens que concluem a entrada, quando não são o ícone (vale antes de READ)
TASK = {'crusher_forge': ['mistech:crusher_forge'],
        'runic_upgrades': ['mistech:runic_plate', 'mistech:speed_upgrade', 'mistech:efficiency_upgrade', 'mistech:range_upgrade'],
        'rune_arrows': ['mistech:rune_arrow'], 'mandrake': ['mistech:mandrake_seeds'], 'tarot_cards': ['mistech:blank_tarot_card'],
        'mapinguari': ['mistech:mapinguari_claw'], 'magic_loom': ['mistech:magic_cloth'], 'mage_robes': ['mistech:hemp_mage_robe'],
        'deep_dream': ['mistech:dream_dust', 'mistech:dream_amber'], 'the_dreamer': ['mistech:dreamer_essence'],
        'ritual_circle': ['mistech:ritual_pedestal'], 'kettle': ['mistech:kettle']}


def results(eid):
    out = []
    for p in entries[eid]['pages']:
        for f in ('result', 'item'):
            v = p.get(f, '')
            if v.startswith('mistech:') and v not in out:
                out.append(v)
    return out


# qualquer um dos resultados das páginas conclui
ANY = {'metal_blocks', 'impure_essence', 'rings', 'amulets', 'flower_incenses', 'arcane_network', 'accessory_bases', 'impure_tools',
       'infernal_tools', 'sanctuary_rituals', 'sanctuary_blessings', 'farming_machines'}

# marcos da jornada (nós grandes)
BIG = {'runic_artifacts', 'welcome', 'incense', 'crops', 'writing_table', 'runic_furnace', 'magic_compressor', 'gnome_realm', 'impure_altar',
       'altar_tier2', 'altar_tier3', 'astrology_table', 'tarot_table', 'spatial_barrel', 'runic_crusher', 'clay_golem', 'accessory_bases',
       'sanctuary', 'folklore_intro', 'beyond_intro', 'primordial_heart', 'the_dreamer'}


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
    'beyond_intro': (0, 4.5), 'portal_anchor': (1.6, 6.2),
    'echo_collector': (1.6, 1.5), 'echo_compass': (3.2, 0), 'runic_note_block': (3.2, 1), 'silence_seal': (3.2, 2),
    'echoling': (3.2, 3), 'guardian': (4.8, 0),
    'pale_sentinel': (1.6, 4.5), 'pale_chalk': (3.2, 4.1), 'eyeblossom_incense': (3.2, 5.1), 'pale_heart': (4.8, 4.5),
    'igneous_essence': (1.6, 8.7), 'infernal_heart': (3.2, 6.7), 'piglin_pact': (3.2, 7.7), 'runic_strider': (3.2, 8.7),
    'infernal_tools': (3.2, 9.7), 'blaze_generator': (3.2, 10.7),
    'primordial_heart': (6.4, 4.5),
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
assert not missing and not extra, ('faltam em CHAPTERS', missing, 'não existem no book.json', extra)
assert len(order) == len(set(order)), [x for x in order if order.count(x) > 1]
for k, v in DEPS.items():
    for d in v:
        assert d in entries, (k, d)
assert set(DEPS) == set(entries), ('DEPS diferente das entradas', set(entries) ^ set(DEPS))
for k in list(TASK) + list(READ) + list(ANY) + list(BIG):
    assert k in entries, k
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

# ---------------------------------------------------------------- monta os capítulos
newcats = []
problems = []
for cid, m, ids in CHAPTERS:
    c = dict(cats.get(cid, {'id': cid}))
    if m:
        c.update(m)
    links = external_links(ids, DEPS)
    big = BIG - set(links)  # uma ligação é sempre um nó pequeno
    if cid in MANUAL:
        assert set(MANUAL[cid]) == set(ids), (cid, set(ids) ^ set(MANUAL[cid]))
        pos = place_links(MANUAL[cid], links, ids, DEPS, big)
    else:
        pos = layout(ids, DEPS, links, big)
    problems += [(cid,) + p for p in overlaps(pos, big)]
    es = []
    for eid in ids:
        e = dict(entries[eid])
        if eid in TASK:
            e['task'] = {'type': 'item', 'items': TASK[eid]}
        elif eid in READ or not e['icon'].startswith('mistech:'):
            e['task'] = {'type': 'read'}
        elif eid in ANY:
            e['task'] = {'type': 'item', 'items': results(eid)}
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
    out = {k: c[k] for k in ('id', 'icon', 'name', 'description')}
    out['links'] = [{'id': l, 'x': pos[l][0], 'y': pos[l][1]} for l in links]
    out['entries'] = es
    newcats.append(out)
b['categories'] = newcats

if problems:
    print('AVISO, nós sobrepostos:', problems)
if '--dry' in sys.argv:
    for c in newcats:
        print('\n==', c['id'], ' ligações:', ', '.join(l['id'] for l in c['links']))
        grid = {}
        for e in c['entries'] + [dict(l, id='<' + l['id']) for l in c['links']]:
            grid[(round(e['y'] * 2), round(e['x'] / 1.6))] = e['id'][:12] + ('*' if e.get('size') else '')
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
