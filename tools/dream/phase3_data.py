"""Dados da fase 3 do Sonho: estruturas, loot, spawns, modelos de item, receitas, traduções e grimório.
Uso (raiz do projeto): python tools/dream/phase3_data.py — idempotente."""
import json, os

R = 'src/main/resources'
D = R + '/data/mistech'
A = R + '/assets/mistech'


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


# ---------------------------------------------------------------- estruturas
STRUCTS = {
    'fractal_temple': (['fractal_garden', 'cloud_sea', 'inverted_forest'], 34, 12, 51830275),
    'memory_library': (['memory_quilt', 'starry_void', 'abyssal_gaze'], 40, 14, 51830276),
}
for kind, (biomes, sp, sep, salt) in STRUCTS.items():
    w(f'{D}/worldgen/structure/{kind}.json', {'type': 'mistech:dream_structure', 'kind': kind, 'biomes': ['mistech:' + b for b in biomes],
                                              'spawn_overrides': {}, 'step': 'surface_structures', 'terrain_adaptation': 'none'})
    w(f'{D}/worldgen/structure_set/{kind}s.json', {'placement': {'type': 'minecraft:random_spread', 'salt': salt, 'separation': sep, 'spacing': sp},
                                                   'structures': [{'structure': 'mistech:' + kind, 'weight': 1}]})


# ---------------------------------------------------------------- loot
def entry(name, weight=1, lo=1, hi=1, extra=None):
    e = {'type': 'minecraft:item', 'name': name, 'weight': weight}
    fn = []
    if hi > 1:
        fn.append({'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}, 'add': False})
    if extra:
        fn += extra
    if fn:
        e['functions'] = fn
    return e


def pool(rolls, entries):
    return {'rolls': rolls if isinstance(rolls, (int, float)) else {'type': 'minecraft:uniform', 'min': rolls[0], 'max': rolls[1]},
            'bonus_rolls': 0.0, 'entries': entries}


def book(n, pages):
    return entry('minecraft:written_book', 1, extra=[
        {'function': 'minecraft:set_written_book_pages', 'pages': [{'translate': f'book.mistech.memory_{n}.page{p}'} for p in range(1, pages + 1)],
         'mode': 'replace_all'},
        {'function': 'minecraft:set_book_cover', 'title': f'Memória {n}', 'author': '???'},
        {'function': 'minecraft:set_name', 'name': {'translate': f'book.mistech.memory_{n}.title', 'italic': False}, 'target': 'item_name'}])


w(f'{D}/loot_table/chests/fractal_temple.json', {'type': 'minecraft:chest', 'pools': [
    pool((4, 7), [entry('mistech:dream_dust', 10, 2, 6), entry('mistech:dream_amber', 5, 1, 2), entry('minecraft:amethyst_shard', 10, 3, 8),
                  entry('mistech:star_fragment', 6, 1, 3), entry('mistech:mithril_ingot', 6, 1, 4), entry('mistech:raw_orichalcum', 3, 1, 2),
                  entry('mistech:fractal_sapling', 4, 1, 2), entry('mistech:kaleido_flower', 6, 1, 3), entry('minecraft:diamond', 4, 1, 3),
                  entry('mistech:space_rune', 2), entry('mistech:arcane_rune', 2)]),
    pool(1, [entry('mistech:lucid_dreamcatcher', 1), entry('mistech:dream_amber', 3, 2, 3)])]})
w(f'{D}/loot_table/chests/memory_library.json', {'type': 'minecraft:chest', 'pools': [
    pool((3, 6), [entry('mistech:memory_shard', 10, 1, 3), entry('minecraft:book', 8, 2, 5), entry('minecraft:candle', 6, 1, 4),
                  entry('mistech:watcher_eye', 4, 1, 2), entry('mistech:raw_adamantine', 4, 1, 2), entry('mistech:time_rune', 3),
                  entry('minecraft:echo_shard', 4, 1, 3), entry('minecraft:experience_bottle', 5, 2, 6), entry('mistech:memory_lily', 5, 1, 3)]),
    pool(1, [book(1, 3), book(2, 3), book(3, 3), book(4, 2)]),
    pool(1, [entry('mistech:awakening_bell', 1), {'type': 'minecraft:empty', 'weight': 3}])]})


def eloot(name, pools):
    w(f'{D}/loot_table/entities/{name}.json', {'type': 'minecraft:entity', 'pools': pools})


eloot('cloud_whale', [pool(1, [entry('mistech:dream_amber', 1, 1, 2)]), pool(1, [entry('mistech:cloud_wool', 1, 3, 8)])])
eloot('sky_fish', [pool(1, [entry('mistech:sky_fish_food', 1)])])
eloot('prism_moth', [pool(1, [entry('mistech:dream_dust', 1, 1, 2)])])
eloot('echo', [pool(1, [entry('minecraft:echo_shard', 1), {'type': 'minecraft:empty', 'weight': 4}])])
eloot('rem_watcher', [pool(1, [entry('mistech:watcher_eye', 1), {'type': 'minecraft:empty', 'weight': 1}])])
eloot('memory', [])
eloot('the_dreamer', [pool(1, [entry('mistech:dreamer_essence', 1, 1, 2)]), pool(1, [entry('mistech:memory_shard', 1, 3, 6)]),
                      pool(1, [entry('mistech:dream_amber', 1, 2, 4)])])

# ---------------------------------------------------------------- spawns
BIO = D + '/worldgen/biome/'
SPAWNS = {
    'cloud_sea': {'creature': [('mistech:cloud_whale', 3, 1, 1)], 'ambient': [('mistech:sky_fish', 12, 3, 6), ('mistech:prism_moth', 5, 1, 2)]},
    'fractal_garden': {'ambient': [('mistech:prism_moth', 14, 1, 3), ('mistech:sky_fish', 5, 3, 5)]},
    'inverted_forest': {'creature': [('mistech:cloud_whale', 2, 1, 1)], 'ambient': [('mistech:sky_fish', 10, 3, 6), ('mistech:prism_moth', 6, 1, 2)]},
    'starry_void': {'monster': [('mistech:echo', 20, 1, 1), ('mistech:rem_watcher', 10, 1, 1)], 'ambient': [('mistech:memory', 8, 1, 2)]},
    'memory_quilt': {'monster': [('mistech:echo', 10, 1, 1), ('mistech:rem_watcher', 6, 1, 1)], 'ambient': [('mistech:memory', 16, 1, 3)]},
    'abyssal_gaze': {'monster': [('mistech:rem_watcher', 25, 1, 2), ('mistech:echo', 20, 1, 2)]},
}
for name, cats in SPAWNS.items():
    p = BIO + name + '.json'
    b = json.load(open(p, encoding='utf-8'))
    for cat, lst in cats.items():
        mine = {t for t, *_ in lst}
        cur = [e for e in b['spawners'][cat] if e['type'] not in mine]
        b['spawners'][cat] = cur + [{'type': t, 'weight': wt, 'minCount': a, 'maxCount': c} for t, wt, a, c in lst]
    w(p, b)

# ---------------------------------------------------------------- modelos de item
ITEMS = ['dream_dust', 'dream_amber', 'sky_fish_food', 'memory_shard', 'watcher_eye', 'dreamer_essence', 'awakening_bell', 'lucid_dreamcatcher']
EGGS = ['cloud_whale', 'sky_fish', 'prism_moth', 'echo', 'rem_watcher', 'memory', 'the_dreamer']
for it in ITEMS + [e + '_spawn_egg' for e in EGGS]:
    w(f'{A}/models/item/{it}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mistech:item/{it}'}})
    w(f'{A}/items/{it}.json', {'model': {'type': 'minecraft:model', 'model': f'mistech:item/{it}'}})

# ---------------------------------------------------------------- receitas
w(f'{D}/recipe/lucid_dreamcatcher.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' D ', 'WXW', ' A '],
                                         'key': {'D': 'mistech:dream_dust', 'W': 'mistech:cloud_wool', 'X': 'mistech:dreamcatcher', 'A': 'mistech:dream_amber'},
                                         'result': {'id': 'mistech:lucid_dreamcatcher', 'count': 1}})
w(f'{D}/recipe/awakening_bell.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['MEM', 'ABA', 'MDM'],
                                     'key': {'M': 'mistech:memory_shard', 'E': 'mistech:watcher_eye', 'A': 'mistech:dream_amber', 'B': 'minecraft:bell',
                                             'D': 'mistech:dream_dust'},
                                     'result': {'id': 'mistech:awakening_bell', 'count': 1}})

# ---------------------------------------------------------------- traduções
PT = {
    'entity.mistech.cloud_whale': 'Baleia-Nuvem', 'entity.mistech.sky_fish': 'Peixe-do-Ar', 'entity.mistech.prism_moth': 'Mariposa-Vitral',
    'entity.mistech.echo': 'Eco', 'entity.mistech.rem_watcher': 'Vigia REM', 'entity.mistech.memory': 'Memória', 'entity.mistech.the_dreamer': 'O Sonhador',
    'item.mistech.cloud_whale_spawn_egg': 'Ovo de Baleia-Nuvem', 'item.mistech.sky_fish_spawn_egg': 'Ovo de Peixe-do-Ar',
    'item.mistech.prism_moth_spawn_egg': 'Ovo de Mariposa-Vitral', 'item.mistech.echo_spawn_egg': 'Ovo de Eco',
    'item.mistech.rem_watcher_spawn_egg': 'Ovo de Vigia REM', 'item.mistech.memory_spawn_egg': 'Ovo de Memória',
    'item.mistech.the_dreamer_spawn_egg': 'Ovo do Sonhador',
    'item.mistech.dream_dust': 'Pó de Sonho', 'item.mistech.dream_amber': 'Âmbar Onírico', 'item.mistech.sky_fish_food': 'Peixe-do-Ar',
    'item.mistech.memory_shard': 'Fragmento de Memória', 'item.mistech.watcher_eye': 'Olho Vigia', 'item.mistech.dreamer_essence': 'Essência do Sonhador',
    'item.mistech.awakening_bell': 'Sino do Despertar', 'item.mistech.lucid_dreamcatcher': 'Apanha-Sonho Lúcido',
    'ring.mistech.common': 'Sonho Comum', 'ring.mistech.nightmare': 'Pesadelo', 'ring.mistech.deep': 'Sonho Profundo', 'ring.mistech.rem': 'Sono REM',
    'tooltip.mistech.lucid_dreamcatcher.target': 'Destino: %s',
    'tooltip.mistech.lucid_dreamcatcher.help': 'Agachado + usar: escolhe a camada. Só leva a camadas já visitadas.',
    'message.mistech.lucid_dreamcatcher.selected': 'O Apanha-Sonho aponta para: %s',
    'message.mistech.lucid_dreamcatcher.not_dreaming': 'Ele só funciona dentro de um sonho.',
    'message.mistech.lucid_dreamcatcher.unknown': 'Você ainda não sonhou com essa camada.',
    'message.mistech.lucid_dreamcatcher.lost': 'O sonho escorrega... tente de novo.',
    'message.mistech.cloud_whale.full': 'A baleia canta, satisfeita. (O âmbar ainda não se formou.)',
    'message.mistech.echo.caught': 'Seus próprios passos te alcançaram.',
    'message.mistech.rem_watcher.alarm': 'O lustre de olhos te encontrou.',
    'message.mistech.awakening_bell.wrong_place': 'O sino não soa aqui. Só no Sono REM.',
    'message.mistech.awakening_bell.already': 'Ele já está acordando por perto.',
    'message.mistech.the_dreamer.summoned': 'O sino ecoa pelo sono. Algo enorme se mexe... O Sonhador vem ver quem o chamou.',
    'message.mistech.the_dreamer.awake': 'A máscara se abre. O SONHADOR ACORDOU.',
    'message.mistech.the_dreamer.defeated': 'O Sonhador volta a dormir — e o sonho fica um pouco mais seu.',
    'message.mistech.memory.0': '"...o cheiro de bolo saindo do forno da vó, num domingo de chuva."',
    'message.mistech.memory.1': '"...a primeira vez que vi o mar. Achei que ele não acabava."',
    'message.mistech.memory.2': '"...alguém chamando meu nome de longe. Nunca soube quem era."',
    'message.mistech.memory.3': '"...uma casa com um corredor a mais do que devia ter."',
    'message.mistech.memory.4': '"...o barulho da chuva no telhado de zinco, e eu sem sono."',
    'message.mistech.memory.5': '"...uma música que eu sei de cor, mas nunca ouvi acordado."',
    'message.mistech.memory.6': '"...um cachorro amarelo que me seguia até a escola."',
    'message.mistech.memory.7': '"...a escada da casa antiga tinha um degrau que rangia. Era o sétimo."',
    'message.mistech.memory.8': '"...eu caía, caía... e acordava antes do chão."',
    'message.mistech.memory.9': '"...uma porta no fundo do quintal que não estava lá ontem."',
    'message.mistech.memory.10': '"...o rosto de alguém que eu amava. Ele já está se apagando."',
    'message.mistech.memory.11': '"...uma baleia passou por cima da cidade e ninguém olhou para cima."',
    'message.mistech.memory.12': '"...o relógio da sala parou às 3:17. Todos os relógios pararam."',
    'message.mistech.memory.13': '"...eu sabia que estava sonhando. E mesmo assim tive medo."',
    'book.mistech.memory_1.title': 'Diário do Sonhador, I',
    'book.mistech.memory_1.page1': 'Dia 1.\n\nDormi e acordei aqui de novo. As baleias passam devagar lá em cima, como se o céu fosse água.',
    'book.mistech.memory_1.page2': 'Dia 9.\n\nAs ilhas mudam de lugar quando ninguém olha. Marquei uma com fita vermelha. Hoje a fita estava no meu travesseiro.',
    'book.mistech.memory_1.page3': 'Dia 40.\n\nJá não lembro qual é o lado de fora. Se alguém achar este diário: não toque o sino.',
    'book.mistech.memory_2.title': 'Catálogo da Biblioteca',
    'book.mistech.memory_2.page1': 'Seção A: lembranças de infância.\nSeção B: lembranças que nunca aconteceram.\nSeção C: (arrancada)',
    'book.mistech.memory_2.page2': 'Os livros desta casa são escritos por quem dorme. Cada noite, uma página. Cada vigília, uma página apagada.',
    'book.mistech.memory_2.page3': 'Aviso: os andares não ficam um em cima do outro. Ficam um em cima do que você lembra.',
    'book.mistech.memory_3.title': 'Sobre os Ecos',
    'book.mistech.memory_3.page1': 'O Eco não persegue você. Ele só faz o caminho que você fez.\n\nSe você parar, ele chega.',
    'book.mistech.memory_3.page2': 'Ande. Ande sempre. Não volte pelo mesmo caminho — ele está vindo por ali.',
    'book.mistech.memory_3.page3': 'O lustre de olhos chama os Ecos. Não deixe que ele te veja por muito tempo.',
    'book.mistech.memory_4.title': 'Canção de Ninar',
    'book.mistech.memory_4.page1': 'Dorme, dorme, que a máscara\nnão abre os olhos ainda.\nSe abrir, não é sonho —\né você que nunca acorda.',
    'book.mistech.memory_4.page2': 'Para acordar O Sonhador: memórias, um olho que vigia, âmbar das baleias e pó das mariposas, num sino.',
    'guide.mistech.gnome_realm.recipe_lucid': 'Apanha-Sonho Lúcido',
    'guide.mistech.gnome_realm.recipe_bell': 'Sino do Despertar',
    'guide.mistech.gnome_realm.12': 'No §bSonho Profundo§r vivem as §bBaleias-Nuvem§r, gigantes que nadam bem acima das ilhas. Com a §emão vazia§r você sobe nas costas dela e ela vai para onde você §eolha§r. Dê §fAlgodão-de-Nuvem§r e ela canta — e às vezes solta §6Âmbar Onírico§r.',
    'guide.mistech.gnome_realm.13': 'Cardumes de §dPeixes-do-Ar§r nadam pelo ar (comer dá Queda Lenta). A §dMariposa-Vitral§r tem asas de vitral que brilham e é atraída pela §eluz§r: perto de lanternas, de vez em quando solta §dPó de Sonho§r.\n\nO §5Templo Fractal§r é um cubo de Menger gigante: suba a rampa e entre pelo túnel até o meio oco.',
    'guide.mistech.gnome_realm.14': 'No §8Sono REM§r, o §8Eco§r faz §eexatamente o caminho que você fez§r, alguns segundos depois. Se você parar, ele te alcança. Um golpe o desfaz.\n\nO §8Vigia REM§r é um lustre de olhos: se te vê por 2 segundos, dá o alarme, chama os monstros e um Eco.',
    'guide.mistech.gnome_realm.15': 'As §7Memórias§r são figuras de vestido sépia com uma gaiola no lugar da cabeça. Clique nelas: contam uma lembrança e deixam um §7Fragmento de Memória§r.\n\nA §8Biblioteca das Memórias§r é uma torre torta: suba pelos andaimes até o baú do último andar.',
    'guide.mistech.gnome_realm.16': 'O §5Apanha-Sonho Lúcido§r leva entre as camadas que você já visitou (agachado + usar escolhe; usar viaja).\n\nO §6Sino do Despertar§r, tocado no Sono REM, chama §5O Sonhador§r: uma máscara de porcelana com mãos gigantes. Na metade da vida ele §cacorda§r. Solta a §5Essência do Sonhador§r.',
}
EN = {
    'entity.mistech.cloud_whale': 'Cloud Whale', 'entity.mistech.sky_fish': 'Sky Fish', 'entity.mistech.prism_moth': 'Stained-Glass Moth',
    'entity.mistech.echo': 'Echo', 'entity.mistech.rem_watcher': 'REM Watcher', 'entity.mistech.memory': 'Memory', 'entity.mistech.the_dreamer': 'The Dreamer',
    'item.mistech.cloud_whale_spawn_egg': 'Cloud Whale Spawn Egg', 'item.mistech.sky_fish_spawn_egg': 'Sky Fish Spawn Egg',
    'item.mistech.prism_moth_spawn_egg': 'Stained-Glass Moth Spawn Egg', 'item.mistech.echo_spawn_egg': 'Echo Spawn Egg',
    'item.mistech.rem_watcher_spawn_egg': 'REM Watcher Spawn Egg', 'item.mistech.memory_spawn_egg': 'Memory Spawn Egg',
    'item.mistech.the_dreamer_spawn_egg': 'The Dreamer Spawn Egg',
    'item.mistech.dream_dust': 'Dream Dust', 'item.mistech.dream_amber': 'Dream Amber', 'item.mistech.sky_fish_food': 'Sky Fish',
    'item.mistech.memory_shard': 'Memory Shard', 'item.mistech.watcher_eye': 'Watcher Eye', 'item.mistech.dreamer_essence': "Dreamer's Essence",
    'item.mistech.awakening_bell': 'Awakening Bell', 'item.mistech.lucid_dreamcatcher': 'Lucid Dreamcatcher',
    'ring.mistech.common': 'Common Dream', 'ring.mistech.nightmare': 'Nightmare', 'ring.mistech.deep': 'Deep Dream', 'ring.mistech.rem': 'REM Sleep',
    'tooltip.mistech.lucid_dreamcatcher.target': 'Destination: %s',
    'tooltip.mistech.lucid_dreamcatcher.help': 'Sneak + use: pick the layer. Only takes you to layers you have visited.',
    'message.mistech.lucid_dreamcatcher.selected': 'The Dreamcatcher points to: %s',
    'message.mistech.lucid_dreamcatcher.not_dreaming': 'It only works inside a dream.',
    'message.mistech.lucid_dreamcatcher.unknown': "You haven't dreamed of that layer yet.",
    'message.mistech.lucid_dreamcatcher.lost': 'The dream slips away... try again.',
    'message.mistech.cloud_whale.full': 'The whale sings, content. (The amber has not formed yet.)',
    'message.mistech.echo.caught': 'Your own footsteps caught up with you.',
    'message.mistech.rem_watcher.alarm': 'The chandelier of eyes has found you.',
    'message.mistech.awakening_bell.wrong_place': 'The bell does not ring here. Only in REM Sleep.',
    'message.mistech.awakening_bell.already': 'It is already stirring nearby.',
    'message.mistech.the_dreamer.summoned': 'The bell echoes through sleep. Something huge stirs... The Dreamer comes to see who called.',
    'message.mistech.the_dreamer.awake': 'The mask splits open. THE DREAMER IS AWAKE.',
    'message.mistech.the_dreamer.defeated': 'The Dreamer falls back asleep — and the dream becomes a little more yours.',
    'message.mistech.memory.0': '"...the smell of cake coming out of grandma\'s oven, on a rainy Sunday."',
    'message.mistech.memory.1': '"...the first time I saw the sea. I thought it never ended."',
    'message.mistech.memory.2': '"...someone calling my name from far away. I never knew who."',
    'message.mistech.memory.3': '"...a house with one more hallway than it should have."',
    'message.mistech.memory.4': '"...rain on the tin roof, and me wide awake."',
    'message.mistech.memory.5': '"...a song I know by heart, but never heard while awake."',
    'message.mistech.memory.6': '"...a yellow dog that followed me to school."',
    'message.mistech.memory.7': '"...the old house\'s stairs had one step that creaked. The seventh."',
    'message.mistech.memory.8': '"...I was falling, falling... and woke up before the ground."',
    'message.mistech.memory.9': '"...a door at the back of the yard that wasn\'t there yesterday."',
    'message.mistech.memory.10': '"...the face of someone I loved. It is already fading."',
    'message.mistech.memory.11': '"...a whale passed over the city and nobody looked up."',
    'message.mistech.memory.12': '"...the living-room clock stopped at 3:17. All the clocks stopped."',
    'message.mistech.memory.13': '"...I knew I was dreaming. I was scared anyway."',
    'book.mistech.memory_1.title': "Dreamer's Diary, I",
    'book.mistech.memory_1.page1': 'Day 1.\n\nI fell asleep and woke up here again. The whales drift slowly overhead, as if the sky were water.',
    'book.mistech.memory_1.page2': 'Day 9.\n\nThe islands move when nobody looks. I tied a red ribbon to one. Today the ribbon was on my pillow.',
    'book.mistech.memory_1.page3': "Day 40.\n\nI no longer remember which side is outside. If anyone finds this diary: don't ring the bell.",
    'book.mistech.memory_2.title': 'Library Catalogue',
    'book.mistech.memory_2.page1': 'Section A: childhood memories.\nSection B: memories that never happened.\nSection C: (torn out)',
    'book.mistech.memory_2.page2': 'The books in this house are written by sleepers. Each night, a page. Each waking, a page erased.',
    'book.mistech.memory_2.page3': "Warning: the floors don't sit on top of each other. They sit on top of what you remember.",
    'book.mistech.memory_3.title': 'On Echoes',
    'book.mistech.memory_3.page1': "The Echo doesn't chase you. It just walks the path you walked.\n\nIf you stop, it arrives.",
    'book.mistech.memory_3.page2': "Keep walking. Always. Don't go back the same way — it's coming that way.",
    'book.mistech.memory_3.page3': "The chandelier of eyes calls the Echoes. Don't let it see you for long.",
    'book.mistech.memory_4.title': 'Lullaby',
    'book.mistech.memory_4.page1': "Sleep, sleep, the mask\nwon't open its eyes yet.\nIf it does, it's not a dream —\nit's you who never wakes.",
    'book.mistech.memory_4.page2': 'To wake The Dreamer: memories, a watching eye, whale amber and moth dust, in a bell.',
    'guide.mistech.gnome_realm.recipe_lucid': 'Lucid Dreamcatcher',
    'guide.mistech.gnome_realm.recipe_bell': 'Awakening Bell',
    'guide.mistech.gnome_realm.12': 'In the §bDeep Dream§r live the §bCloud Whales§r, giants that swim high above the islands. With an §eempty hand§r you climb on its back and it goes where you §elook§r. Feed it §fCloud Cotton§r and it sings — and sometimes drops §6Dream Amber§r.',
    'guide.mistech.gnome_realm.13': 'Schools of §dSky Fish§r swim through the air (eating one gives Slow Falling). The §dStained-Glass Moth§r has glowing stained-glass wings and is drawn to §elight§r: near lanterns it now and then sheds §dDream Dust§r.\n\nThe §5Fractal Temple§r is a giant Menger cube: climb the ramp and walk the tunnel to the hollow middle.',
    'guide.mistech.gnome_realm.14': 'In §8REM Sleep§r, the §8Echo§r walks §eexactly the path you walked§r, a few seconds later. If you stop, it catches you. One hit shatters it.\n\nThe §8REM Watcher§r is a chandelier of eyes: if it sees you for 2 seconds it raises the alarm, calling monsters and an Echo.',
    'guide.mistech.gnome_realm.15': '§7Memories§r are figures in sepia dresses with a birdcage for a head. Click one: it tells a memory and leaves a §7Memory Shard§r.\n\nThe §8Library of Memories§r is a crooked tower: climb the scaffolding to the chest on the top floor.',
    'guide.mistech.gnome_realm.16': 'The §5Lucid Dreamcatcher§r travels between layers you have visited (sneak + use picks; use travels).\n\nThe §6Awakening Bell§r, rung in REM Sleep, calls §5The Dreamer§r: a porcelain mask with giant hands. At half health it §cwakes up§r. Drops §5Dreamer\'s Essence§r.',
}
for f, d in (('pt_br', PT), ('en_us', EN)):
    p = f'{A}/lang/{f}.json'
    l = json.load(open(p, encoding='utf-8'))
    l.update(d)
    w(p, l)


# ---------------------------------------------------------------- grimório
def find(o):
    if isinstance(o, dict):
        if o.get('id') == 'gnome_realm' and 'pages' in o:
            return o
        for v in o.values():
            r = find(v)
            if r:
                return r
    if isinstance(o, list):
        for v in o:
            r = find(v)
            if r:
                return r


p = f'{A}/guide/book.json'
b = json.load(open(p, encoding='utf-8'))
e = find(b)
new = [{'type': 'spotlight', 'item': 'mistech:cloud_whale_spawn_egg', 'text': 'guide.mistech.gnome_realm.12'},
       {'type': 'spotlight', 'item': 'mistech:dream_dust', 'text': 'guide.mistech.gnome_realm.13'},
       {'type': 'spotlight', 'item': 'mistech:echo_spawn_egg', 'text': 'guide.mistech.gnome_realm.14'},
       {'type': 'spotlight', 'item': 'mistech:memory_shard', 'text': 'guide.mistech.gnome_realm.15'},
       {'type': 'spotlight', 'item': 'mistech:lucid_dreamcatcher', 'text': 'guide.mistech.gnome_realm.16'},
       {'type': 'crafting', 'grid': ['', 'mistech:dream_dust', '', 'mistech:cloud_wool', 'mistech:dreamcatcher', 'mistech:cloud_wool', '',
                                      'mistech:dream_amber', ''], 'result': 'mistech:lucid_dreamcatcher', 'text': 'guide.mistech.gnome_realm.recipe_lucid'},
       {'type': 'crafting', 'grid': ['mistech:memory_shard', 'mistech:watcher_eye', 'mistech:memory_shard', 'mistech:dream_amber', 'minecraft:bell',
                                      'mistech:dream_amber', 'mistech:memory_shard', 'mistech:dream_dust', 'mistech:memory_shard'],
        'result': 'mistech:awakening_bell', 'text': 'guide.mistech.gnome_realm.recipe_bell'}]
keys = {json.dumps(n, sort_keys=True) for n in new}
e['pages'] = [x for x in e['pages'] if json.dumps(x, sort_keys=True) not in keys] + new
w(p, b)
print('ok fase 3')
