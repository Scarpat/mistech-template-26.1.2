"""Recursos do folclore novo (leva 2): Iara, Cuca e Mapinguari (+ Anel da Iara, Machado do Mapinguari, Caldeirão da Cuca).
Rodar na raiz do projeto DEPOIS do resources.py:  python tools/lore/resources2.py [pasta_vanilla]
pasta_vanilla = onde está assets/minecraft/textures (padrão: vanilla/)."""
import json, math, os, random, sys
from PIL import Image
sys.path.insert(0, 'tools/pendencias2')
sys.path.insert(0, 'tools/lore')
from res import A, D, wj, tag, lang, flat_item, recipe, self_loot

VAN = (sys.argv[1] if len(sys.argv) > 1 else 'vanilla').rstrip('/') + '/'
VT = VAN + 'assets/minecraft/textures/'
TEX = A + 'textures/'


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# ---------------------------------------------------------------- spawns
BIOME = D + 'mistech/worldgen/biome/ancestral_forest.json'
b = json.load(open(BIOME, encoding='utf-8'))
mon = [s for s in b['spawners']['monster'] if s['type'] not in ('mistech:mapinguari', 'mistech:iara')]
mon += [{'type': 'mistech:mapinguari', 'maxCount': 1, 'minCount': 1, 'weight': 1},
        {'type': 'mistech:iara', 'maxCount': 1, 'minCount': 1, 'weight': 6}]
b['spawners']['monster'] = mon
wj(BIOME, b)
tag('mistech/tags/worldgen/biome/iara_waters', '#minecraft:is_river', 'minecraft:swamp', 'minecraft:mangrove_swamp')
wj(D + 'mistech/neoforge/biome_modifier/iara_spawns.json', {'type': 'neoforge:add_spawns', 'biomes': '#mistech:iara_waters',
    'spawners': [{'type': 'mistech:iara', 'minCount': 1, 'maxCount': 1, 'weight': 6}]})

# ---------------------------------------------------------------- loot
def item(name, mn=1, mx=1, looting=0, weight=None):
    e = {'type': 'minecraft:item', 'name': name}
    f = []
    if mx > 1 or mn != 1:
        f.append({'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': mn, 'max': mx}})
    if looting:
        f.append({'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
                  'count': {'type': 'minecraft:uniform', 'min': 0, 'max': looting}})
    if f:
        e['functions'] = f
    if weight:
        e['weight'] = weight
    return e


def loot(name, pools):
    wj(D + 'mistech/loot_table/entities/%s.json' % name, {'type': 'minecraft:entity', 'pools': pools,
                                                         'random_sequence': 'mistech:entities/' + name})


PLAYER = [{'condition': 'minecraft:killed_by_player'}]
loot('iara', [
    {'rolls': 1, 'entries': [item('mistech:iara_scale', 1, 2, 1)], 'conditions': PLAYER},
    {'rolls': 1, 'entries': [item('minecraft:cod', 1, 3, weight=3), item('minecraft:salmon', 1, 2, weight=2),
                             item('minecraft:nautilus_shell', weight=1), {'type': 'minecraft:empty', 'weight': 2}]}])
loot('cuca', [
    {'rolls': 1, 'entries': [item('mistech:cuca_cauldron')]},
    {'rolls': 1, 'entries': [item('minecraft:emerald', 4, 9, 2)]},
    {'rolls': 2, 'entries': [item('minecraft:glowstone_dust', 1, 4), item('minecraft:redstone', 2, 6), item('minecraft:spider_eye', 1, 3),
                             item('minecraft:sugar', 1, 4), item('mistech:impure_essence_bottle', 1, 2)]}])
loot('mapinguari', [
    {'rolls': 1, 'entries': [item('mistech:mapinguari_claw', 2, 4, 1)]},
    {'rolls': 1, 'entries': [item('minecraft:leather', 4, 9, 2)]},
    {'rolls': 1, 'entries': [item('minecraft:bone', 2, 6)]}])

# ---------------------------------------------------------------- receitas e tags
recipe('writing/iara_ring', {'type': 'mistech:writing', 'ingredients': ['mistech:gold_ring_blank', 'mistech:water_rune', 'mistech:iara_scale',
       'mistech:iara_scale'], 'chalk_damage': 10, 'result': {'id': 'mistech:iara_ring', 'count': 1}})
recipe('mapinguari_axe', {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': ['CC', 'CS', ' S'],
       'key': {'C': 'mistech:mapinguari_claw', 'S': 'minecraft:stick'}, 'result': {'id': 'mistech:mapinguari_axe', 'count': 1}})
tag('mistech/tags/item/mapinguari_repair', 'mistech:mapinguari_claw')
tag('minecraft/tags/item/axes', 'mistech:mapinguari_axe')
tag('minecraft/tags/item/enchantable/mining', 'mistech:mapinguari_axe')
tag('minecraft/tags/item/enchantable/durability', 'mistech:mapinguari_axe')
tag('minecraft/tags/block/mineable/pickaxe', 'mistech:cuca_cauldron')
ring = D.replace('data/', 'data/') + 'curios/tags/item/ring.json'
rj = json.load(open(ring, encoding='utf-8'))
if not any(v.get('id') == 'mistech:iara_ring' for v in rj['values']):
    rj['values'].append({'id': 'mistech:iara_ring', 'required': False})
wj(ring, rj)
self_loot('cuca_cauldron')

# rituais (Altar T3, à noite)
recipe('ritual/awaken_cuca', {'type': 'mistech:ritual', 'catalyst': 'minecraft:cauldron', 'ingredients': [
    'mistech:matinta_feather', 'mistech:matinta_feather', 'mistech:darkness_rune', 'mistech:lunar_blood', 'minecraft:spider_eye',
    'minecraft:fermented_spider_eye', 'mistech:dried_tobacco_leaf', 'mistech:impure_magic_shard'],
    'essence': 8, 'duration': 600, 'condition': 'night', 'min_tier': 3, 'action': 'awaken_cuca', 'effect': 'shadow'})
recipe('ritual/awaken_mapinguari', {'type': 'mistech:ritual', 'catalyst': 'minecraft:rotten_flesh', 'ingredients': [
    'mistech:earth_rune', 'mistech:earth_rune', 'mistech:life_rune', 'mistech:lunar_blood', 'minecraft:leather', 'minecraft:leather',
    'minecraft:bone_block', 'mistech:murky_log'],
    'essence': 8, 'duration': 600, 'condition': 'night', 'min_tier': 3, 'action': 'awaken_mapinguari', 'effect': 'nature'})

# ---------------------------------------------------------------- caldeirão (modelo vanilla, textura própria)
def recolor(src, dst, tint, extra=None):
    im = Image.open(VT + 'block/%s.png' % src).convert('RGBA')
    p = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, bb, a = p[x, y]
            if not a:
                continue
            lum = (r * 0.3 + g * 0.59 + bb * 0.11) / 255
            c = tuple(min(255, int(v * (0.35 + lum * 1.5))) for v in tint)
            p[x, y] = c + (a,)
    if extra:
        extra(p, im)
    im.save(TEX + 'block/%s.png' % dst)


CAUL = hexc('3a2a44')


def side_deco(p, im):
    # crânios de osso e baba verde escorrendo da borda
    for cx in (4, 11):
        for (dx, dy, c) in ((-1, -1, 'e8dcc0'), (0, -1, 'f4ecd8'), (1, -1, 'e8dcc0'), (-2, 0, 'd0c4a8'), (-1, 0, '1a0e14'), (0, 0, 'e8dcc0'),
                            (1, 0, '1a0e14'), (2, 0, 'd0c4a8'), (-1, 1, 'd0c4a8'), (0, 1, '5a4a40'), (1, 1, 'd0c4a8'), (-1, 2, 'b8ac90'),
                            (1, 2, 'b8ac90'), (0, 2, '8a7c68')):
            p[cx + dx, 7 + dy] = hexc(c) + (255,)
    rnd = random.Random(7)
    for x in range(16):
        if rnd.random() < 0.35:
            for y in range(rnd.randint(1, 3)):
                p[x, y] = mix(hexc('5ac83a'), hexc('2a7a1e'), y / 3) + (255,)


recolor('cauldron_side', 'cuca_cauldron_side', CAUL, side_deco)
recolor('cauldron_top', 'cuca_cauldron_top', CAUL)
recolor('cauldron_bottom', 'cuca_cauldron_bottom', CAUL)
recolor('cauldron_inner', 'cuca_cauldron_inner', CAUL)
TX = {'bottom': 'mistech:block/cuca_cauldron_bottom', 'inside': 'mistech:block/cuca_cauldron_inner',
      'particle': 'mistech:block/cuca_cauldron_side', 'side': 'mistech:block/cuca_cauldron_side', 'top': 'mistech:block/cuca_cauldron_top'}
wj(A + 'models/block/cuca_cauldron.json', {'parent': 'minecraft:block/cauldron', 'textures': TX})
for lvl, tpl in ((1, 'template_cauldron_level1'), (2, 'template_cauldron_level2'), (3, 'template_cauldron_full')):
    wj(A + 'models/block/cuca_cauldron_level%d.json' % lvl, {'parent': 'minecraft:block/' + tpl,
                                                            'textures': dict(TX, content='minecraft:block/water_still')})
wj(A + 'blockstates/cuca_cauldron.json', {'variants': {'fill=0': {'model': 'mistech:block/cuca_cauldron'},
    **{'fill=%d' % i: {'model': 'mistech:block/cuca_cauldron_level%d' % i} for i in (1, 2, 3)}}})
wj(A + 'items/cuca_cauldron.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:block/cuca_cauldron'}})


# ---------------------------------------------------------------- itens
def canvas():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    return im, im.load()


def outline(p, col=(20, 16, 18, 255)):
    pts = [(x, y) for y in range(16) for x in range(16) if p[x, y][3]]
    s = set(pts)
    for x, y in pts:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if 0 <= q[0] < 16 and 0 <= q[1] < 16 and q not in s and not p[q][3]:
                p[q] = col


# Escama da Iara: escama de peixe grande (ponta em cima, borda arredondada), verde-água iridescente com faixas
im, p = canvas()
for y in range(16):
    for x in range(16):
        dx = (x + 0.5 - 8) / 6.0
        dy = (y + 0.5 - 9.5) / 5.5
        inside = dx * dx + dy * dy < 1.0 if dy > 0 else abs(dx) < (1 + dy * 0.55) * (1 + dy * 0.45) * 1.0 and dy > -1.6
        if not inside:
            continue
        band = (y + 0.5 - 1.5) / 14
        c = mix(hexc('b4fff0'), hexc('1e7a8a'), min(1, max(0, band)))
        ring = math.hypot(dx, dy + 0.9)
        if int(ring * 4) % 2 == 1:
            c = mix(c, hexc('3a9ab0'), 0.35)
        if abs(dx) < 0.12:
            c = mix(c, hexc('f0fff8'), 0.4)
        p[x, y] = c + (255,)
for (x, y) in ((6, 6), (6, 7), (7, 5)):
    p[x, y] = hexc('f8fffc') + (255,)
outline(p, hexc('0e3a42') + (255,))
im.save(TEX + 'item/iara_scale.png')
flat_item('iara_scale')

# Garra do Mapinguari: garra curva escura com tufo de pelo
im, p = canvas()
for i in range(40):
    t = i / 39
    x = 4 + 8 * math.sin(t * 1.4)
    y = 13 - 11 * t
    w = 2.2 * (1 - t) + 0.4
    for yy in range(16):
        for xx in range(16):
            if (xx + 0.5 - x) ** 2 + (yy + 0.5 - y) ** 2 < w * w:
                c = mix(hexc('5a4a40'), hexc('1a1412'), t * 0.8)
                if xx + 0.5 > x + w * 0.3:
                    c = mix(c, hexc('8a7a6a'), 0.35)
                p[xx, yy] = c + (255,)
rnd = random.Random(3)
for _ in range(30):
    x, y = rnd.randint(1, 7), rnd.randint(11, 15)
    if (x - 4) ** 2 + (y - 13.5) ** 2 < 9:
        p[x, y] = rnd.choice([hexc('6a3a22'), hexc('8a5030'), hexc('4a2414')]) + (255,)
outline(p)
im.save(TEX + 'item/mapinguari_claw.png')
flat_item('mapinguari_claw')

# Machado do Mapinguari: três garras curvas amarradas num cabo enrolado de pelo ruivo (pixel art à mão)
AXE = [
    '.....k..........',
    '....kWk.........',
    '...kWBk..k......',
    '...kWBk.kWk.....',
    '..kWBDk.kWBk....',
    '..kWBDkkWBDk.k..',
    '..kWBDDkWBDkkWk.',
    '...kBDDLLLDkWBDk',
    '....kDLLHLLkBDk.',
    '.....kkLHHkDDk..',
    '.......kFHkkk...',
    '......kFFk......',
    '.....kHHk.......',
    '....kHFk........',
    '...kHHk.........',
    '...kkk..........',
]
AXE_C = {'k': '1a1412', 'W': 'e8dcc0', 'B': 'b0a088', 'D': '4a3a30', 'L': '3a2414', 'H': '6a4428', 'F': '9a5a30'}
ax, ap = canvas()
for y, row in enumerate(AXE):
    for x, ch in enumerate(row):
        if ch in AXE_C:
            ap[x, y] = hexc(AXE_C[ch]) + (255,)
ax.save(TEX + 'item/mapinguari_axe.png')
flat_item('mapinguari_axe', parent='minecraft:item/handheld')

# Anel da Iara: aro de ouro com escama verde-água
src = Image.open(TEX + 'item/gold_ring_blank.png').convert('RGBA')
sp = src.load()
pts = [(x, y) for y in range(16) for x in range(16) if sp[x, y][3]]
top = min(y for _, y in pts)
cx = sum(x for x, _ in pts) / len(pts)
for (dx, dy, c) in ((0, -1, '7af0d8'), (-1, 0, '3ab8a8'), (0, 0, '7af0d8'), (1, 0, '1e7a8a'), (0, 1, '1e7a8a'), (-1, -1, 'b4fff0'),
                    (1, -1, '3ab8a8'), (-1, 1, '0e3a42'), (1, 1, '0e3a42'), (0, -2, '0e3a42'), (-2, 0, '0e3a42'), (2, 0, '0e3a42'),
                    (0, 2, '0e3a42')):
    x, y = int(round(cx)) + dx, top + 1 + dy
    if 0 <= x < 16 and 0 <= y < 16:
        sp[x, y] = hexc(c) + (255,)
src.save(TEX + 'item/iara_ring.png')
flat_item('iara_ring')

# ovos
from resources_egg import egg  # noqa
egg('iara_spawn_egg', '2ab08a', ['7af0d8', '4a3a2a'])
egg('cuca_spawn_egg', '4a6a2a', ['5a2e6a', 'ffd030'])
egg('mapinguari_spawn_egg', '6a3a22', ['3a2a24', 'ffe040'])
print('ok itens')

# ---------------------------------------------------------------- Grimório
import resources_book as rb  # noqa
rb.put('folklore', 'caipora', {'id': 'iara', 'icon': 'mistech:iara_scale', 'name': 'guide.mistech.entry.iara', 'deps': ['ancestral_forest'],
    'task': {'type': 'item', 'items': ['mistech:iara_scale']}, 'pages': [
    {'type': 'text', 'text': 'guide.mistech.iara.1'}, {'type': 'text', 'text': 'guide.mistech.iara.2'}]})
rb.put('folklore', 'iara', {'id': 'cuca', 'icon': 'mistech:cuca_cauldron', 'name': 'guide.mistech.entry.cuca', 'deps': ['matinta_pereira'],
    'task': {'type': 'item', 'items': ['mistech:cuca_cauldron']}, 'pages': [
    {'type': 'text', 'text': 'guide.mistech.cuca.1'}, {'type': 'text', 'text': 'guide.mistech.cuca.2'}, {'type': 'text', 'text': 'guide.mistech.cuca.3'}]})
rb.put('folklore', 'cuca', {'id': 'mapinguari', 'icon': 'mistech:mapinguari_axe', 'name': 'guide.mistech.entry.mapinguari', 'deps': ['ancestral_forest'],
    'task': {'type': 'item', 'items': ['mistech:mapinguari_claw']}, 'pages': [
    {'type': 'text', 'text': 'guide.mistech.mapinguari.1'}, {'type': 'text', 'text': 'guide.mistech.mapinguari.2'}]})
rb.place_views((('iara', 'caipora'), ('cuca', 'iara'), ('mapinguari', 'cuca')))
rb.save()

# ---------------------------------------------------------------- traduções
lang({
    'entity.mistech.iara': 'Iara', 'entity.mistech.cuca': 'Cuca', 'entity.mistech.mapinguari': 'Mapinguari',
    'item.mistech.iara_scale': 'Iara Scale', 'item.mistech.iara_ring': 'Iara Ring',
    'item.mistech.mapinguari_claw': 'Mapinguari Claw', 'item.mistech.mapinguari_axe': 'Mapinguari Axe',
    'item.mistech.mapinguari_axe.tip': 'Fells the whole tree at once',
    'item.mistech.mapinguari_axe.sneak': 'Sneak to cut a single log',
    'block.mistech.cuca_cauldron': 'Cuca\'s Cauldron',
    'block.mistech.cuca_cauldron.cold': 'The cauldron needs fire underneath.',
    'block.mistech.cuca_cauldron.full': 'The cauldron cannot take any more (or that potion has no effect).',
    'block.mistech.cuca_cauldron.need_two': 'Mix at least two potions first.',
    'block.mistech.cuca_cauldron.status': 'Brew: %s/%s potions',
    'item.minecraft.potion.effect.cuca': 'Cuca\'s Brew',
    'item.mistech.iara_spawn_egg': 'Iara Spawn Egg', 'item.mistech.cuca_spawn_egg': 'Cuca Spawn Egg',
    'item.mistech.mapinguari_spawn_egg': 'Mapinguari Spawn Egg',
    'message.mistech.iara.song': 'A sweet song calls you toward the water...',
    'message.mistech.cuca.lullaby': '"Sleep, little baby, or the Cuca will come get you..."',
    'message.mistech.cuca.apprentices': 'The Cuca cackles and calls her apprentices!',
    'message.mistech.cuca.already': 'The Cuca is already awake nearby.',
    'message.mistech.cuca.awaken': 'The cauldron bubbles... the Cuca has come!',
    'message.mistech.cuca.sleepless': 'Three nights without sleep... the Cuca has come to get you!',
    'message.mistech.mapinguari.already': 'The Mapinguari already roams nearby.',
    'message.mistech.mapinguari.awaken': 'A stench fills the forest... the Mapinguari has awoken!',
    'guide.mistech.entry.iara': 'Iara',
    'guide.mistech.iara.1': 'The mother of waters lives in rivers, swamps and the waters of the Ancestral Forest. At night she §bsings§r: anyone within 14 blocks, out of the water, is slowly pulled toward the river. In the water she grabs you, drags you to the bottom and steals your breath. Out of the water she dries out and gets hurt.',
    'guide.mistech.iara.2': 'She drops the §bIara Scale§r. Engrave a Gold Ring Blank with a Water Rune and 2 scales at the Writing Table to make the §bIara Ring§r: immune to her song, water breathing and, in the water, Dolphin\'s Grace, Conduit Power and night vision.',
    'guide.mistech.entry.cuca': 'Cuca',
    'guide.mistech.cuca.1': '§5Boss§r (400 health). The alligator witch comes for anyone who goes §l3 nights without sleeping§r and wanders the Ancestral Forest at night, or through the ritual (Tier 3 altar, at night, cauldron as catalyst, with 2 Matinta Feathers).',
    'guide.mistech.cuca.2': 'She throws potions, brews poison clouds, sings the lullaby (blindness and slowness) and teleports when hurt. Below half health she calls her witch apprentices. Poison does nothing to her.',
    'guide.mistech.cuca.3': 'She drops the §5Cuca\'s Cauldron§r: with fire underneath, put in up to 3 potions to mix their effects into one (an Impure Essence raises every level by +1). Take it out with a glass bottle; sneak with an empty hand to empty it.',
    'guide.mistech.entry.mapinguari': 'Mapinguari',
    'guide.mistech.mapinguari.1': '§2Boss§r (500 health). A hairy giant with a single eye, a mouth on its belly and backward feet. It appears rarely at night in the Ancestral Forest, or through the ritual (Tier 3 altar, at night). Arrows and projectiles barely hurt it (25%); it knocks down leaves as it walks.',
    'guide.mistech.mapinguari.2': 'It slams the ground (damage and knockback) and its stinking roar causes nausea, weakness and hunger. Its §2Claws§r make the §2Mapinguari Axe§r (3 claws + 2 sticks): it fells the whole tree at once, even the Ancestral Trees.',
}, {
    'entity.mistech.iara': 'Iara', 'entity.mistech.cuca': 'Cuca', 'entity.mistech.mapinguari': 'Mapinguari',
    'item.mistech.iara_scale': 'Escama da Iara', 'item.mistech.iara_ring': 'Anel da Iara',
    'item.mistech.mapinguari_claw': 'Garra do Mapinguari', 'item.mistech.mapinguari_axe': 'Machado do Mapinguari',
    'item.mistech.mapinguari_axe.tip': 'Derruba a árvore inteira de uma vez',
    'item.mistech.mapinguari_axe.sneak': 'Agache para cortar só um tronco',
    'block.mistech.cuca_cauldron': 'Caldeirão da Cuca',
    'block.mistech.cuca_cauldron.cold': 'O caldeirão precisa de fogo embaixo.',
    'block.mistech.cuca_cauldron.full': 'O caldeirão não aceita mais (ou essa poção não tem efeito).',
    'block.mistech.cuca_cauldron.need_two': 'Misture pelo menos duas poções antes.',
    'block.mistech.cuca_cauldron.status': 'Caldo: %s/%s poções',
    'item.minecraft.potion.effect.cuca': 'Poção da Cuca',
    'item.mistech.iara_spawn_egg': 'Ovo Gerador de Iara', 'item.mistech.cuca_spawn_egg': 'Ovo Gerador de Cuca',
    'item.mistech.mapinguari_spawn_egg': 'Ovo Gerador de Mapinguari',
    'message.mistech.iara.song': 'Um canto doce te chama para a água...',
    'message.mistech.cuca.lullaby': '"Nana neném, que a Cuca vem pegar..."',
    'message.mistech.cuca.apprentices': 'A Cuca gargalha e chama as aprendizes!',
    'message.mistech.cuca.already': 'A Cuca já está acordada aqui perto.',
    'message.mistech.cuca.awaken': 'O caldeirão borbulha... a Cuca chegou!',
    'message.mistech.cuca.sleepless': 'Três noites sem dormir... a Cuca veio te pegar!',
    'message.mistech.mapinguari.already': 'O Mapinguari já anda por aqui.',
    'message.mistech.mapinguari.awaken': 'Um fedor toma conta da mata... o Mapinguari acordou!',
    'guide.mistech.entry.iara': 'Iara',
    'guide.mistech.iara.1': 'A mãe-d\'água vive nos rios, pântanos e nas águas da Mata Ancestral. À noite ela §bcanta§r: quem estiver a até 14 blocos, fora d\'água, é puxado devagar para o rio. Na água ela agarra, puxa para o fundo e rouba o fôlego. Fora d\'água ela seca e se machuca.',
    'guide.mistech.iara.2': 'Solta a §bEscama da Iara§r. Grave um Aro de Ouro com Runa da Água e 2 escamas na Mesa de Escrita para fazer o §bAnel da Iara§r: imune ao canto, respira debaixo d\'água e, na água, ganha Graça do Golfinho, Poder do Conduíte e visão noturna.',
    'guide.mistech.entry.cuca': 'Cuca',
    'guide.mistech.cuca.1': '§5Chefe§r (400 de vida). A bruxa-jacaré vem atrás de quem passa §l3 noites sem dormir§r e anda pela Mata Ancestral à noite, ou pelo ritual (Altar Tier 3, à noite, caldeirão de catalisador, com 2 Penas da Matinta).',
    'guide.mistech.cuca.2': 'Ela joga poções, faz nuvens de veneno, canta a cantiga de ninar (cegueira e lentidão) e se teleporta quando apanha. Abaixo da metade da vida chama as bruxas aprendizes. Veneno não faz nada nela.',
    'guide.mistech.cuca.3': 'Solta o §5Caldeirão da Cuca§r: com fogo embaixo, ponha até 3 poções para misturar os efeitos numa só (uma Essência Impura sobe +1 nível em tudo). Tire com um frasco vazio; agache com a mão vazia para esvaziar.',
    'guide.mistech.entry.mapinguari': 'Mapinguari',
    'guide.mistech.mapinguari.1': '§2Chefe§r (500 de vida). Gigante peludo de um olho só, boca na barriga e pés virados para trás. Aparece raro à noite na Mata Ancestral, ou pelo ritual (Altar Tier 3, à noite). Flechas e projéteis quase não machucam (25%); derruba as folhas por onde passa.',
    'guide.mistech.mapinguari.2': 'Dá pancadas no chão (dano e arremesso) e o urro fedorento causa tontura, fraqueza e fome. As §2Garras§r fazem o §2Machado do Mapinguari§r (3 garras + 2 gravetos): derruba a árvore inteira de uma vez, até as Árvores Ancestrais.',
})
lang({'tooltip.mistech.accessory.iara_ring': "Immune to the Iara's song; water breathing; in water: dolphin's grace, conduit power and night vision"}, {'tooltip.mistech.accessory.iara_ring': 'Imune ao canto da Iara; respira na água; na água: graça do golfinho, poder do conduíte e visão noturna'})
print('ok')
