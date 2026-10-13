"""Recursos do folclore novo (leva 1): bioma Mata Ancestral (+ árvore gigante), Matinta Pereira e Caipora.
Rodar na raiz do projeto:  python tools/lore/resources.py [pasta_vanilla_data]   (NÃO rodar o views.py depois)
pasta_vanilla_data = onde está data/minecraft/worldgen/biome/jungle.json (padrão: vanilla/)."""
import copy, json, os, random, sys
from PIL import Image
sys.path.insert(0, 'tools/pendencias2')
from res import A, D, wj, tag, lang, flat_item

VAN = (sys.argv[1] if len(sys.argv) > 1 else 'vanilla').rstrip('/') + '/'
TEX = A + 'textures/'

# ---------------------------------------------------------------- bioma
jungle = json.load(open(VAN + 'data/minecraft/worldgen/biome/jungle.json'))
b = copy.deepcopy(jungle)
b['attributes'] = {
    'minecraft:audio/background_music': {'default': {'max_delay': 24000, 'min_delay': 12000, 'sound': 'minecraft:music.overworld.jungle'}},
    'minecraft:gameplay/increased_fire_burnout': True,
    'minecraft:visual/sky_color': '#6fa88a',
    'minecraft:visual/fog_color': '#9ec8a6',
    'minecraft:visual/water_fog_color': '#1e3a32',
    'minecraft:visual/ambient_particles': [{'particle': {'type': 'minecraft:spore_blossom_air'}, 'probability': 0.006}],
}
b['effects'] = {'water_color': '#2f6a5a', 'grass_color': '#3c8a2e', 'foliage_color': '#2a7a1e', 'dry_foliage_color': '#5a6a2a'}
b['temperature'] = 0.85
b['downfall'] = 0.95
veg = b['features'][9]
veg.insert(veg.index('minecraft:trees_jungle'), 'mistech:ancestral_tree')
veg.remove('minecraft:bamboo_light')
creature = [s for s in b['spawners']['creature'] if s['type'] not in ('minecraft:panda',)]
creature += [{'type': 'mistech:matinta_pereira', 'maxCount': 1, 'minCount': 1, 'weight': 8},
             {'type': 'mistech:caipora', 'maxCount': 1, 'minCount': 1, 'weight': 5}]
b['spawners']['creature'] = creature
wj(D + 'mistech/worldgen/biome/ancestral_forest.json', b)
wj(D + 'mistech/worldgen/configured_feature/ancestral_tree.json', {'type': 'mistech:ancestral_tree', 'config': {}})
wj(D + 'mistech/worldgen/placed_feature/ancestral_tree.json', {'feature': 'mistech:ancestral_tree', 'placement': [
    {'type': 'minecraft:count', 'count': {'type': 'minecraft:weighted_list', 'distribution': [
        {'data': 0, 'weight': 3}, {'data': 1, 'weight': 5}, {'data': 2, 'weight': 2}]}},
    {'type': 'minecraft:in_square'},
    {'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0},
    {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'},
    {'type': 'minecraft:biome'}]})
tag('minecraft/tags/worldgen/biome/is_overworld', 'mistech:ancestral_forest')
tag('minecraft/tags/worldgen/biome/is_forest', 'mistech:ancestral_forest')
tag('minecraft/tags/worldgen/biome/is_jungle', 'mistech:ancestral_forest')
tag('mistech/tags/worldgen/biome/folklore_wilds', '#minecraft:is_forest', '#minecraft:is_jungle', '#minecraft:is_taiga')
# os bichos do folclore também aparecem fora da Mata Ancestral (bem mais raros)
wj(D + 'mistech/neoforge/biome_modifier/folklore_wild_spawns.json', {'type': 'neoforge:add_spawns', 'biomes': '#mistech:folklore_wilds',
    'spawners': [{'type': 'mistech:matinta_pereira', 'minCount': 1, 'maxCount': 1, 'weight': 2},
                 {'type': 'mistech:caipora', 'minCount': 1, 'maxCount': 1, 'weight': 1}]})

# ---------------------------------------------------------------- loot
wj(D + 'mistech/loot_table/entities/matinta_pereira.json', {'type': 'minecraft:entity', 'pools': [
    {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'mistech:matinta_feather', 'functions': [
        {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}},
        {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}],
     'conditions': [{'condition': 'minecraft:killed_by_player'}]},
    {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:glass_bottle', 'weight': 2},
                             {'type': 'minecraft:item', 'name': 'minecraft:stick', 'weight': 1}]}],
    'random_sequence': 'mistech:entities/matinta_pereira'})

# ---------------------------------------------------------------- itens (texturas)
def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
p = im.load()
import math
ax, ay, bx, by = 2.5, 13.5, 12.5, 2.5  # haste em diagonal
L = math.hypot(bx - ax, by - ay)
ux, uy = (bx - ax) / L, (by - ay) / L
for y in range(16):
    for x in range(16):
        px_, py_ = x + 0.5 - ax, y + 0.5 - ay
        t = (px_ * ux + py_ * uy) / L
        d = abs(px_ * -uy + py_ * ux)
        if t < -0.02 or t > 1.0:
            continue
        w = 0.6 if t < 0.18 else 2.8 * math.sin(math.pi * min(1.0, (t - 0.1) / 0.92)) + 0.4
        if d > w:
            continue
        if d < 0.55:
            c = hexc('8a7a64') if t > 0.15 else hexc('a89880')   # haste
        elif d > w - 0.8:
            c = hexc('1a1412')                                     # borda
        else:
            c = hexc('3a2e28') if (x + y) % 3 else hexc('4a3c32')
        p[x, y] = c + (255,)
# "olho" amarelo perto da ponta
for (x, y, c) in ((10, 6, 'ffd040'), (11, 5, 'ffd040'), (11, 6, '1a1008'), (10, 5, 'ffe890')):
    p[x, y] = hexc(c) + (255,)
im.save(TEX + 'item/matinta_feather.png')
flat_item('matinta_feather')


def egg(name, base, spots):
    src = Image.open(TEX + 'item/gnome_spawn_egg.png').convert('RGBA')
    out = Image.new('RGBA', src.size, (0, 0, 0, 0))
    sp, op = src.load(), out.load()
    rnd = random.Random(name)
    for y in range(src.height):
        for x in range(src.width):
            c = sp[x, y]
            if not c[3]:
                continue
            lum = (c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11) / 255
            col = hexc(base)
            k = 0.55 + lum * 0.7
            pix = tuple(max(0, min(255, int(v * k))) for v in col)
            if rnd.random() < 0.18:
                pix = hexc(rnd.choice(spots))
            op[x, y] = pix + (255,)
    out.save(TEX + 'item/%s.png' % name)
    flat_item(name)


egg('matinta_pereira_spawn_egg', '2c2420', ['ffd040', '8a7a64'])
egg('caipora_spawn_egg', '9a5a34', ['c8341a', '3a3430'])

# ---------------------------------------------------------------- Grimório
BOOK = 'src/main/resources/assets/mistech/guide/book.json'
bk = json.load(open(BOOK, encoding='utf-8'))
cats = {c['id']: c for c in bk['categories']}


def free_spot(entries, x, y):
    while any(abs(e.get('x', -99) - x) < 1.0 and abs(e.get('y', -99) - y) < 0.9 for e in entries):
        y += 1.0
    return round(x, 2), round(y, 2)


def put(cat, near, entry):
    es = cats[cat]['entries']
    es[:] = [e for e in es if e['id'] != entry['id']]
    ref = next(e for c in bk['categories'] for e in c['entries'] if e['id'] == near)
    same = any(e['id'] == near for e in es)
    x, y = (ref['x'] + 1.6, ref['y']) if same else (max(e['x'] for e in es) + 1.6, 0.0)
    entry['x'], entry['y'] = free_spot(es, x, y)
    es.append(entry)


put('folklore', 'folklore_intro', {'id': 'ancestral_forest', 'icon': 'minecraft:jungle_sapling', 'name': 'guide.mistech.entry.ancestral_forest',
    'deps': ['folklore_intro'], 'pages': [{'type': 'text', 'text': 'guide.mistech.ancestral_forest.1'},
                                         {'type': 'text', 'text': 'guide.mistech.ancestral_forest.2'}]})
put('folklore', 'ancestral_forest', {'id': 'matinta_pereira', 'icon': 'mistech:matinta_feather', 'name': 'guide.mistech.entry.matinta_pereira',
    'deps': ['ancestral_forest'], 'task': {'type': 'item', 'items': ['mistech:matinta_feather']}, 'pages': [
    {'type': 'text', 'text': 'guide.mistech.matinta_pereira.1'}, {'type': 'text', 'text': 'guide.mistech.matinta_pereira.2'}]})
put('folklore', 'matinta_pereira', {'id': 'caipora', 'icon': 'mistech:caipora_spawn_egg', 'name': 'guide.mistech.entry.caipora',
    'deps': ['ancestral_forest'], 'pages': [
    {'type': 'text', 'text': 'guide.mistech.caipora.1'}, {'type': 'text', 'text': 'guide.mistech.caipora.2'}]})
for eid, near in (('ancestral_forest', 'folklore_intro'), ('matinta_pereira', 'ancestral_forest'), ('caipora', 'matinta_pereira')):
    for view in ('progress', 'dimension'):
        chs = bk['views'][view]
        for c in chs:
            c['entries'] = [e for e in c['entries'] if e['id'] != eid]
        chap = next((c for c in chs if any(e['id'] == near for e in c['entries'])), chs[-1])
        ref = next((e for e in chap['entries'] if e['id'] == near), chap['entries'][0])
        x, y = free_spot(chap['entries'], ref['x'] + 1.6, ref['y'])
        chap['entries'].append({'id': eid, 'x': x, 'y': y})
with open(BOOK, 'w', encoding='utf-8') as f:
    json.dump(bk, f, ensure_ascii=False, indent=2)
    f.write('\n')

# ---------------------------------------------------------------- traduções
lang({
    'biome.mistech.ancestral_forest': 'Ancestral Forest',
    'entity.mistech.matinta_pereira': 'Matinta Pereira', 'entity.mistech.caipora': 'Caipora',
    'item.mistech.matinta_feather': 'Matinta Feather',
    'item.mistech.matinta_pereira_spawn_egg': 'Matinta Pereira Spawn Egg', 'item.mistech.caipora_spawn_egg': 'Caipora Spawn Egg',
    'effect.mistech.hunt_blessing': 'Hunting Blessing', 'effect.mistech.caipora_curse': 'Caipora\'s Curse',
    'message.mistech.matinta.reveal': 'The bird twists in the smoke... the Matinta Pereira shows her true face!',
    'message.mistech.matinta.gift': 'The Matinta Pereira accepts the offering and leaves you a gift.',
    'message.mistech.caipora.warning': 'You feel watched from the forest... too many animals have died by your hand.',
    'message.mistech.caipora.coming': 'A whistle cuts through the forest. The Caipora is coming for you!',
    'message.mistech.caipora.lost': 'The forest spins around you... you are lost!',
    'message.mistech.caipora.calmed': 'The Caipora accepts the offering. Hunt with respect: she blesses your hunt.',
    'message.mistech.caipora.blessing': 'The Caipora smiles and blesses your hunt.',
    'message.mistech.caipora.already': 'The Caipora has already blessed you.',
    'message.mistech.caipora.curse': 'The Caipora vanishes into the forest, laughing. The animals will give you nothing...',
    'guide.mistech.entry.ancestral_forest': 'Ancestral Forest',
    'guide.mistech.ancestral_forest.1': 'A forest older than any other: giant trees with trunks 3 or 4 blocks wide, roots like walls, vines and moss everywhere. It replaces part of the wettest jungles and dark forests.',
    'guide.mistech.ancestral_forest.2': 'It is the home of the folklore creatures: the §5Matinta Pereira§r and the §2Caipora§r appear much more often here (and only rarely in other forests and jungles).',
    'guide.mistech.entry.matinta_pereira': 'Matinta Pereira',
    'guide.mistech.matinta_pereira.1': 'A dark bird with yellow eyes that flies through the forest and §owhistles§r at night. Right click it with §lDried Tobacco§r or §lCoffee§r: she leaves a gift (often a §5Matinta Feather§r), gives you Luck for 5 minutes and flies away.',
    'guide.mistech.matinta_pereira.2': 'Hit her and the bird becomes the §5old witch§r (70 health): she strikes with her cane, curses (blindness, weakness), screams (nausea and push), calls spirits and vanishes in smoke. Lose her for 30 s and she turns back into a bird.',
    'guide.mistech.entry.caipora': 'Caipora',
    'guide.mistech.caipora.1': 'The guardian of the animals rides a white-lipped peccary through the forest. Killing too many animals (about 12 in a short time) in a forest makes her come §cafter you§r: she charges, and her whistle dizzies, blinds and sometimes leaves you lost in the forest.',
    'guide.mistech.caipora.2': 'Offer §lDried Tobacco§r or §lCachaça§r: she calms down, forgives your hunt and gives the §aHunting Blessing§r (animals drop double for 10 min). Nobody kills the Caipora: she vanishes laughing and leaves the §cCaipora\'s Curse§r (animals drop nothing for 20 min).',
}, {
    'biome.mistech.ancestral_forest': 'Mata Ancestral',
    'entity.mistech.matinta_pereira': 'Matinta Pereira', 'entity.mistech.caipora': 'Caipora',
    'item.mistech.matinta_feather': 'Pena da Matinta',
    'item.mistech.matinta_pereira_spawn_egg': 'Ovo Gerador de Matinta Pereira', 'item.mistech.caipora_spawn_egg': 'Ovo Gerador de Caipora',
    'effect.mistech.hunt_blessing': 'Bênção da Caça', 'effect.mistech.caipora_curse': 'Maldição da Caipora',
    'message.mistech.matinta.reveal': 'O pássaro se retorce na fumaça... a Matinta Pereira mostra seu verdadeiro rosto!',
    'message.mistech.matinta.gift': 'A Matinta Pereira aceita a oferenda e te deixa um presente.',
    'message.mistech.caipora.warning': 'Você se sente observado pela mata... bicho demais morreu na sua mão.',
    'message.mistech.caipora.coming': 'Um assobio corta a mata. A Caipora está vindo atrás de você!',
    'message.mistech.caipora.lost': 'A mata gira em volta de você... você se perdeu!',
    'message.mistech.caipora.calmed': 'A Caipora aceita a oferenda. Cace com respeito: ela abençoa sua caça.',
    'message.mistech.caipora.blessing': 'A Caipora sorri e abençoa sua caça.',
    'message.mistech.caipora.already': 'A Caipora já te abençoou.',
    'message.mistech.caipora.curse': 'A Caipora some na mata rindo. Os bichos não vão te dar nada...',
    'guide.mistech.entry.ancestral_forest': 'Mata Ancestral',
    'guide.mistech.ancestral_forest.1': 'Uma mata mais velha que todas: árvores gigantes de tronco com 3 ou 4 blocos de largura, sapopemas que parecem paredes, cipó e musgo por todo lado. Aparece no lugar de parte das selvas mais úmidas e das florestas escuras.',
    'guide.mistech.ancestral_forest.2': 'É a casa dos bichos do folclore: a §5Matinta Pereira§r e a §2Caipora§r aparecem muito mais aqui (e só de vez em quando nas outras florestas e selvas).',
    'guide.mistech.entry.matinta_pereira': 'Matinta Pereira',
    'guide.mistech.matinta_pereira.1': 'Um pássaro escuro de olhos amarelos que voa pela mata e §oassobia§r à noite. Botão direito com §lTabaco Seco§r ou §lCafé§r: ela deixa um presente (muitas vezes uma §5Pena da Matinta§r), dá Sorte por 5 minutos e vai embora.',
    'guide.mistech.matinta_pereira.2': 'Bata nela e o pássaro vira a §5bruxa velha§r (70 de vida): bate com a bengala, amaldiçoa (cegueira, fraqueza), grita (tontura e empurrão), chama espíritos e some na fumaça. Se ela te perder por 30 s, volta a ser pássaro.',
    'guide.mistech.entry.caipora': 'Caipora',
    'guide.mistech.caipora.1': 'A guardiã dos bichos anda pela mata montada num queixada. Matar animal demais (uns 12 em pouco tempo) numa mata faz ela vir §catrás de você§r: ela investe, e o assobio dela deixa tonto, cego e às vezes perdido na mata.',
    'guide.mistech.caipora.2': 'Ofereça §lTabaco Seco§r ou §lCachaça§r: ela acalma, perdoa a caça e dá a §aBênção da Caça§r (animais soltam o dobro por 10 min). Ninguém mata a Caipora: ela some rindo e deixa a §cMaldição da Caipora§r (animais não soltam nada por 20 min).',
})
print('ok')
