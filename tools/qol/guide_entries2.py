"""Entradas do Grimório da leva 2 (20/10/2026). Uso igual ao guide_entries.py:
python3 tools/qol/guide_entries2.py && python3 tools/guide/journey.py && python3 tools/guide/views.py"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import guide_entries as g  # noqa: E402

M = '#c:ingots/mithril'
SHARD = 'mistech:impure_magic_shard'

g.NEW = {
    'essence': [
        g.e('liquefier', 'mistech:liquefier', [
            g.craft(['minecraft:obsidian', 'minecraft:cauldron', 'minecraft:obsidian', 'mistech:fire_rune', 'minecraft:blast_furnace',
                     'mistech:fire_rune', 'minecraft:obsidian', 'mistech:energy_rune', 'minecraft:obsidian'], 'mistech:liquefier'),
            g.t('guide.mistech.liquefier.1'), g.t('guide.mistech.liquefier.2')]),
    ],
    'machines2': [
        g.e('arcane_crusher', 'mistech:arcane_crusher', [
            g.craft([M, 'mistech:earth_rune', M, SHARD, 'mistech:runic_crusher', SHARD, M, 'mistech:energy_rune', M], 'mistech:arcane_crusher'),
            g.t('guide.mistech.arcane_crusher.1'),
            {'type': 'smelting', 'input': 'mistech:mithril_dust', 'result': 'mistech:mithril_ingot', 'text': 'guide.mistech.arcane_crusher.dusts'}]),
        g.e('crusher_forge', 'mistech:crusher_forge', [
            g.craft([M, 'mistech:energy_rune', M, 'mistech:fire_rune', 'minecraft:blast_furnace',
                     'mistech:earth_rune', M, 'mistech:impure_magic_block', M], 'mistech:crusher_forge'),
            g.t('guide.mistech.crusher_forge.1'), g.t('guide.mistech.crusher_forge.2')]),
    ],
    'herbalism': [
        g.e('belladonna_uses', 'mistech:belladonna_berries', [
            g.t('guide.mistech.belladonna_uses.1'),
            g.craft(['minecraft:arrow'] * 4 + ['mistech:belladonna_berries'] + ['minecraft:arrow'] * 4, 'minecraft:tipped_arrow',
                    'guide.mistech.belladonna_uses.arrow', 8),
            g.t('guide.mistech.belladonna_uses.2')]),
        g.e('coffee_uses', 'mistech:coffee_cup', [g.t('guide.mistech.coffee_uses.1'), g.t('guide.mistech.coffee_uses.2')]),
        g.e('mandrake_uses', 'mistech:mandrake_powder', [g.t('guide.mistech.mandrake_uses.1'), g.t('guide.mistech.mandrake_uses.2')]),
    ],
}
g.EXTRA_PAGES = {}
g.AFTER = {'liquefier': 'arcane_compressor', 'arcane_crusher': 'runic_upgrades', 'crusher_forge': 'arcane_crusher',
           'belladonna_uses': 'mystic_sight', 'coffee_uses': 'coffee', 'mandrake_uses': 'mandrake'}
g.DEPS = {'liquefier': ['magic_compressor', 'fire_rune'], 'arcane_crusher': ['runic_crusher', 'realm_ores'],
          'crusher_forge': ['arcane_crusher', 'arcane_furnace', 'guide_stone'], 'belladonna_uses': ['mystic_sight'],
          'coffee_uses': ['coffee'], 'mandrake_uses': ['mandrake']}
g.FIX_DEPS = {}
g.READ = ['belladonna_uses', 'coffee_uses', 'mandrake_uses']
g.TASK = {'crusher_forge': ['mistech:crusher_forge']}

if __name__ == '__main__':
    g.book()
    g.journey()
    print('ok')
