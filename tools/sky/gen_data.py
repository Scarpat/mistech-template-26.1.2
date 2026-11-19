#!/usr/bin/env python3
"""Dados da Fase 6 (Céu e fábrica): Relógio Astral (modelos, blockstate, loot, tags, receita), textos e Grimório.
Idempotente. Depois rode tools/guide/views.py.
Uso (na pasta do projeto): python3 tools/sky/gen_data.py"""
import json, os

R = 'src/main/resources/'
A = R + 'assets/mistech/'
D = R + 'data/'
MODES = ['moon', 'event', 'day', 'taint']


def dump(path, obj, newline=True):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        if newline:
            f.write('\n')


# ---------------------------------------------------------------- bloco
for i, m in enumerate(MODES):
    dump(A + 'models/block/astral_clock_%s.json' % m, {
        'parent': 'minecraft:block/orientable',
        'textures': {'front': 'mistech:block/astral_clock_front_' + m, 'side': 'mistech:block/astral_clock_side',
                     'top': 'mistech:block/astral_clock_side'}})
bs = {'variants': {}}
for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
    for i, m in enumerate(MODES):
        v = {'model': 'mistech:block/astral_clock_' + m}
        if r:
            v['y'] = r
        bs['variants']['facing=%s,mode=%d' % (f, i)] = v
dump(A + 'blockstates/astral_clock.json', bs)
dump(A + 'items/astral_clock.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:block/astral_clock_moon'}})
dump(D + 'mistech/loot_table/blocks/astral_clock.json', {
    'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'mistech:astral_clock'}],
                                          'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
    'random_sequence': 'mistech:blocks/astral_clock'})
p = D + 'minecraft/tags/block/mineable/pickaxe.json'
j = json.load(open(p))
if 'mistech:astral_clock' not in j['values']:
    j['values'].append('mistech:astral_clock')
dump(p, j)
GRID = ['minecraft:lapis_lazuli', 'mistech:time_rune', 'minecraft:lapis_lazuli',
        'minecraft:clock', 'minecraft:comparator', 'minecraft:clock',
        'minecraft:lapis_lazuli', 'minecraft:quartz', 'minecraft:lapis_lazuli']
dump(D + 'mistech/recipe/astral_clock.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'redstone', 'pattern': ['LTL', 'CRC', 'LQL'],
    'key': {'L': 'minecraft:lapis_lazuli', 'T': 'mistech:time_rune', 'C': 'minecraft:clock', 'R': 'minecraft:comparator', 'Q': 'minecraft:quartz'},
    'result': {'id': 'mistech:astral_clock', 'count': 1}})

# ---------------------------------------------------------------- textos
LANG = A + 'lang/'
PT = {
    'block.mistech.astral_clock': 'Relógio Astral',
    'message.mistech.astral_clock.mode': 'Relógio Astral: %s (sinal %s)',
    'message.mistech.astral_clock.mode.moon': 'fase da lua', 'message.mistech.astral_clock.mode.event': 'Evento Celeste',
    'message.mistech.astral_clock.mode.day': 'dia e noite', 'message.mistech.astral_clock.mode.taint': 'Mácula do lugar',
    'info.mistech.sky_speed': 'Céu: velocidade %s', 'info.mistech.sky_generation': 'Céu: geração %s', 'info.mistech.sky_taint': 'Céu: Mácula %s',
    'guide.mistech.entry.sky_machines': 'O Céu e as Máquinas',
    'guide.mistech.entry.astral_clock': 'Relógio Astral',
    'guide.mistech.sky_machines.1': 'O céu também trabalha na fábrica. Enquanto um §9Evento Celeste§r está no céu:\n§eSolstício§r: Gerador Purificado +25%, Colheitadeira e Plantadeira +50%.\n§cLua de Sangue§r e §cNoite Eterna§r: Reator Impuro +50% de FE e 2× Mácula; Conjurador em torreta +25%.',
    'guide.mistech.sky_machines.2': '§fLua de Prata§r: Prisma Astral 1,5× (também na Noite Eterna) e −1 de Mácula do lugar por minuto.\n§8Eclipse§r: o Observatório rende como à noite; engaste de Escuridão +25%.\n§bChuva de Estrelas§r: Observatório 2×; o Prisma não gasta Fragmento.',
    'guide.mistech.sky_machines.3': '§aAurora§r: chance dos engastes de Sorte e Eco 2×; Gerador Astral +50%.\n§9Chuva Bendita§r: −2 de Mácula do lugar por minuto onde a chuva cai; engaste de Água +25%.\n§5Véu da Noite Longa§r: as máquinas noturnas já ganham com a noite dobrada.',
    'guide.mistech.sky_machines.4': '§lO mapa astral do dono§r\nA máquina lembra quem a colocou. Cada §6constelação completa§r dele dá um bônus, mesmo com ele fora do jogo:\n§6Áries§r Conjurador em torreta +15% · §6Touro§r Colheitadeira e Plantadeira +20% · §6Gêmeos§r engaste de Ar 3× · §6Câncer§r Filtro de Arruda +25%',
    'guide.mistech.sky_machines.5': '§6Leão§r processamento +10% · §6Virgem§r engaste de Sorte 1,5× · §6Libra§r geradores +10% · §6Escorpião§r −25% de Mácula solta · §6Sagitário§r máquinas de área +1 de alcance · §6Capricórnio§r Mineradora +20% · §6Aquário§r máquinas de fluido +25% · §6Peixes§r Bomba da Iara +50%\n\nO Jade mostra os bônus do céu em cada máquina.',
    'guide.mistech.astral_clock.recipe': 'O §9Relógio Astral§r transforma o céu em sinal de comparador.',
    'guide.mistech.astral_clock.2': '§lSinais dos eventos§r\n1 Véu da Noite Longa\n2 Solstício\n3 Lua de Sangue\n4 Lua de Prata\n5 Eclipse\n6 Noite Eterna\n7 Chuva de Estrelas\n8 Aurora\n9 Chuva Bendita\n\nSe houver dois, vale o de número menor.',
    'guide.mistech.astral_clock.1': 'Clique para trocar o modo (agachado volta); a face mostra qual:\n§fLua§r: 1 a 8 (cheia = 1, nova = 5).\n§eEvento§r: 1 a 9, um por Evento Celeste; 0 = nenhum.\n§6Dia§r: 15 de dia, 0 à noite.\n§5Mácula§r: faixa do chunk × 3 (0 a 12).\n\nEx.: ligar o Prisma só na lua nova, ou os Filtros quando a Mácula sobe.',
}
EN = {
    'block.mistech.astral_clock': 'Astral Clock',
    'message.mistech.astral_clock.mode': 'Astral Clock: %s (signal %s)',
    'message.mistech.astral_clock.mode.moon': 'moon phase', 'message.mistech.astral_clock.mode.event': 'Celestial Event',
    'message.mistech.astral_clock.mode.day': 'day and night', 'message.mistech.astral_clock.mode.taint': 'Taint of the place',
    'info.mistech.sky_speed': 'Sky: speed %s', 'info.mistech.sky_generation': 'Sky: generation %s', 'info.mistech.sky_taint': 'Sky: Taint %s',
    'guide.mistech.entry.sky_machines': 'The Sky and the Machines',
    'guide.mistech.entry.astral_clock': 'Astral Clock',
    'guide.mistech.sky_machines.1': 'The sky works in the factory too. While a §9Celestial Event§r is up:\n§eSolstice§r: Purified Generator +25%, Harvester and Planter +50%.\n§cBlood Moon§r and §cEternal Night§r: Impure Reactor +50% FE and 2× Taint; Conjurer in turret mode +25%.',
    'guide.mistech.sky_machines.2': '§fSilver Moon§r: Astral Prism 1.5× (also in the Eternal Night) and −1 Taint of the place per minute.\n§8Eclipse§r: the Observatory yields as if at night; Darkness socket +25%.\n§bStarfall§r: Observatory 2×; the Prism uses no Star Fragments.',
    'guide.mistech.sky_machines.3': '§aAurora§r: Luck and Echo socket chances 2×; Astral Generator +50%.\n§9Blessed Rain§r: −2 Taint of the place per minute where the rain falls; Water socket +25%.\n§5Veil of the Long Night§r: night machines already gain from the doubled night.',
    'guide.mistech.sky_machines.4': '§lThe owner\'s natal chart§r\nA machine remembers who placed it. Each of their §6complete constellations§r gives a bonus, even while they are offline:\n§6Aries§r turret Conjurer +15% · §6Taurus§r Harvester and Planter +20% · §6Gemini§r Air socket 3× · §6Cancer§r Rue Filter +25%',
    'guide.mistech.sky_machines.5': '§6Leo§r processing +10% · §6Virgo§r Luck socket 1.5× · §6Libra§r generators +10% · §6Scorpius§r −25% Taint released · §6Sagittarius§r area machines +1 range · §6Capricornus§r Miner +20% · §6Aquarius§r fluid machines +25% · §6Pisces§r Iara Pump +50%\n\nJade shows the sky bonuses on each machine.',
    'guide.mistech.astral_clock.recipe': 'The §9Astral Clock§r turns the sky into a comparator signal.',
    'guide.mistech.astral_clock.2': '§lEvent signals§r\n1 Veil of the Long Night\n2 Solstice\n3 Blood Moon\n4 Silver Moon\n5 Eclipse\n6 Eternal Night\n7 Starfall\n8 Aurora\n9 Blessed Rain\n\nIf two are up, the lower number wins.',
    'guide.mistech.astral_clock.1': 'Click to change the mode (sneaking goes back); the face shows which:\n§fMoon§r: 1 to 8 (full = 1, new = 5).\n§eEvent§r: 1 to 9, one per Celestial Event; 0 = none.\n§6Day§r: 15 by day, 0 at night.\n§5Taint§r: chunk stage × 3 (0 to 12).\n\nE.g. run the Prism only on the new moon, or the Filters when Taint rises.',
}
for name, d in (('pt_br', PT), ('en_us', EN)):
    p = LANG + name + '.json'
    data = json.load(open(p, encoding='utf-8'))
    data.update(d)
    dump(p, data)

BOOK = A + 'guide/book.json'
E = [
    {'id': 'sky_machines', 'icon': 'mistech:star_fragment', 'name': 'guide.mistech.entry.sky_machines',
     'deps': ['constellations', 'celestial_events'], 'task': {'type': 'read'}, 'x': 8.0, 'y': 2.0,
     'pages': [{'type': 'text', 'text': 'guide.mistech.sky_machines.%d' % i} for i in range(1, 6)]},
    {'id': 'astral_clock', 'icon': 'mistech:astral_clock', 'name': 'guide.mistech.entry.astral_clock',
     'deps': ['sky_machines'], 'task': {'type': 'item', 'items': ['mistech:astral_clock']}, 'x': 9.6, 'y': 2.5,
     'pages': [{'type': 'crafting', 'grid': GRID, 'result': 'mistech:astral_clock', 'text': 'guide.mistech.astral_clock.recipe'},
               {'type': 'text', 'text': 'guide.mistech.astral_clock.1'}, {'type': 'text', 'text': 'guide.mistech.astral_clock.2'}]},
]
book = json.load(open(BOOK, encoding='utf-8'))
cat = next(c for c in book['categories'] if c['id'] == 'astrology')
ids = {e['id'] for e in E}
cat['entries'] = [e for e in cat['entries'] if e['id'] not in ids] + E
dump(BOOK, book, newline=False)
print('ok')
