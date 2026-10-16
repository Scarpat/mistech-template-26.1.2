"""Gera JSONs de estruturas, structure sets e loot tables das estruturas do Sonho (fase 2)."""
import json, os, sys
R = 'src/main/resources'
D = R + '/data/mistech'
M = R + '/assets/mistech/models/item'

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2); f.write('\n')

STRUCTS = {
    # kind: (biomes, spacing, separation, salt, exclusion)
    'hamlet': (['gnome_meadow', 'sweet_mushroom_grove', 'pillow_hills'], 10, 4, 51830271, None),
    'ruined_hamlet': (['murky_wastes', 'hand_forest'], 26, 9, 51830272, 'mistech:goblin_villages'),
    'wrong_house': (['wrong_neighborhood'], 12, 4, 51830273, None),
    'endless_stair': (['murky_wastes', 'falling_chasms', 'hand_forest'], 36, 12, 51830274, 'mistech:goblin_villages'),
}
for kind, (biomes, sp, sep, salt, excl) in STRUCTS.items():
    w(f'{D}/worldgen/structure/{kind}.json', {
        'type': 'mistech:dream_structure', 'kind': kind,
        'biomes': ['mistech:' + b for b in biomes],
        'spawn_overrides': {}, 'step': 'surface_structures', 'terrain_adaptation': 'beard_thin' if kind != 'wrong_house' else 'beard_box'})
    placement = {'type': 'minecraft:random_spread', 'salt': salt, 'separation': sep, 'spacing': sp}
    if excl:
        placement['exclusion_zone'] = {'other_set': excl, 'chunk_count': 4}
    w(f'{D}/worldgen/structure_set/{kind}s.json', {'placement': placement, 'structures': [{'structure': 'mistech:' + kind, 'weight': 1}]})

def entry(name, weight, lo=1, hi=1):
    e = {'type': 'minecraft:item', 'name': name, 'weight': weight}
    if hi > 1:
        e['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}, 'add': False}]
    return e

def table(rolls, entries, bonus=None):
    pools = [{'rolls': {'type': 'minecraft:uniform', 'min': rolls[0], 'max': rolls[1]}, 'bonus_rolls': 0.0, 'entries': entries}]
    if bonus:
        pools.append({'rolls': {'type': 'minecraft:uniform', 'min': bonus[0], 'max': bonus[1]}, 'bonus_rolls': 0.0, 'entries': bonus[2]})
    return {'type': 'minecraft:chest', 'pools': pools}

LOOT = {
    'gnome_hamlet': table((3, 6), [
        entry('minecraft:bread', 12, 1, 4), entry('minecraft:mushroom_stew', 6), entry('minecraft:honey_bottle', 6, 1, 2),
        entry('mistech:cloud_wool', 10, 1, 4), entry('mistech:cloud_cotton', 10, 2, 6), entry('mistech:dreamflower', 8, 1, 3),
        entry('mistech:dream_poppy', 8, 1, 3), entry('mistech:gnome_bait', 5, 1, 2), entry('mistech:raw_mithril', 4, 1, 3),
        entry('minecraft:emerald', 6, 1, 3), entry('mistech:fairy_dust', 4, 1, 2), entry('mistech:gnome_hat', 1)]),
    'ruined_hamlet': table((3, 6), [
        entry('minecraft:rotten_flesh', 12, 1, 4), entry('minecraft:cobweb', 8, 1, 3), entry('mistech:sleepless_ivy', 8, 1, 4),
        entry('mistech:cloud_wool', 6, 1, 3), entry('mistech:raw_mithril', 6, 1, 4), entry('mistech:mithril_ingot', 3, 1, 2),
        entry('minecraft:emerald', 5, 1, 4), entry('mistech:darkness_rune', 3), entry('mistech:gnome_staff', 1),
        entry('mistech:dreamcatcher', 2)]),
    'wrong_house': table((4, 7), [
        entry('minecraft:clock', 6), entry('minecraft:candle', 8, 1, 4), entry('minecraft:book', 8, 1, 3),
        entry('mistech:memory_lily', 6, 1, 2), entry('mistech:sleepless_ivy', 6, 1, 3), entry('mistech:mithril_ingot', 5, 1, 3),
        entry('mistech:star_fragment', 4, 1, 2), entry('mistech:time_rune', 3), entry('mistech:dreamcatcher', 3),
        entry('minecraft:music_disc_11', 1), entry('minecraft:diamond', 3, 1, 2)],
        (1, 1, [entry('mistech:tarot_card_the_moon', 1), entry('mistech:star_fragment', 3, 1, 3)])),
    'endless_stair': table((4, 8), [
        entry('mistech:star_fragment', 8, 1, 3), entry('mistech:raw_adamantine', 4, 1, 2), entry('mistech:mithril_ingot', 6, 1, 4),
        entry('mistech:eye_flower', 5, 1, 2), entry('mistech:space_rune', 3), entry('mistech:time_rune', 3),
        entry('minecraft:ender_pearl', 6, 1, 3), entry('minecraft:diamond', 4, 1, 3), entry('minecraft:echo_shard', 3, 1, 2),
        entry('mistech:purified_dreamcatcher', 1)],
        (1, 1, [entry('mistech:tarot_card_the_star', 1), entry('minecraft:experience_bottle', 4, 3, 8)])),
}
missing = []
for name, t in LOOT.items():
    for p in t['pools']:
        for e in p['entries']:
            n = e['name']
            if n.startswith('mistech:') and not os.path.exists(f'{M}/{n[8:]}.json'):
                missing.append(n)
    w(f'{D}/loot_table/chests/{name}.json', t)
print('missing', missing)
