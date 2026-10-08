import sys; sys.path.insert(0, 'tools/pendencias2')
from res import *
tag('mistech/tags/item/zodiac_tokens', 'minecraft:blaze_powder', 'minecraft:golden_apple', 'minecraft:rabbit_foot', 'minecraft:nautilus_shell',
    'minecraft:blaze_rod', 'minecraft:emerald', 'minecraft:iron_block', 'minecraft:fermented_spider_eye', 'minecraft:ender_pearl',
    'minecraft:diamond', 'minecraft:glow_ink_sac', 'minecraft:tropical_fish')
R = {
    'sun': (['mistech:light_rune', 'mistech:fire_rune', 'minecraft:gold_block'], 'day'),
    'moon': (['mistech:darkness_rune', 'mistech:water_rune', 'minecraft:glowstone'], 'full_moon'),
    'rising': (['mistech:time_rune', 'mistech:air_rune', 'minecraft:amethyst_block'], 'dawn'),
}
for part, (extra, cond) in R.items():
    recipe('ritual/realign_' + part, {'type': 'mistech:ritual', 'catalyst': 'mistech:natal_chart',
        'ingredients': ['#mistech:zodiac_tokens'] + extra + ['mistech:impure_magic_shard', 'mistech:impure_magic_shard'],
        'essence': 6, 'duration': 600, 'condition': cond, 'min_tier': 2,
        'result': {'id': 'mistech:natal_chart'}, 'copy_components': True, 'action': 'realign_' + part, 'effect': 'astral'})
lang({
 'ritual.mistech.condition.dawn': 'Only at dawn',
 'message.mistech.natal.realign_token': 'Put the rare item of the chosen constellation on a pedestal (just one sign).',
 'message.mistech.natal.realign_same': 'Your %s is already in %s.',
 'message.mistech.natal.realigned': 'The stars bend to your will: your %s is now in %s!',
 'guide.mistech.ritual.realign_sun': 'Realign the Sun',
 'guide.mistech.ritual.realign_moon': 'Realign the Moon',
 'guide.mistech.ritual.realign_rising': 'Realign the Ascendant',
}, {
 'ritual.mistech.condition.dawn': 'Só no amanhecer',
 'message.mistech.natal.realign_token': 'Ponha num pedestal o item raro da constelação escolhida (só de um signo).',
 'message.mistech.natal.realign_same': '%s já está em %s.',
 'message.mistech.natal.realigned': 'Os astros se curvam: %s agora em %s!',
 'guide.mistech.ritual.realign_sun': 'Realinhar o Sol',
 'guide.mistech.ritual.realign_moon': 'Realinhar a Lua',
 'guide.mistech.ritual.realign_rising': 'Realinhar o Ascendente',
})
print('ok')
