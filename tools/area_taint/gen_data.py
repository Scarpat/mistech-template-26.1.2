#!/usr/bin/env python3
"""Dados da Mácula do lugar (Fase 4): Filtro de Arruda (modelo, blockstate, loot, tags, receita), textos e páginas do Grimório.
Idempotente. Depois rode tools/guide/views.py.
Uso (na pasta do projeto): python3 tools/area_taint/gen_data.py"""
import json, os

R = 'src/main/resources/'
A = R + 'assets/mistech/'
D = R + 'data/'


def dump(path, obj, newline=True):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        if newline:
            f.write('\n')


# ---------------------------------------------------------------- receita, loot e tags
RECIPE = ['mistech:rue_leaf', 'mistech:coarse_salt', 'mistech:rue_leaf',
          'mistech:air_rune', 'minecraft:cauldron', 'mistech:air_rune',
          'mistech:rue_leaf', 'minecraft:copper_ingot', 'mistech:rue_leaf']
dump(D + 'mistech/recipe/rue_filter.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['ASA', 'RFR', 'ACA'],
    'key': {'A': 'mistech:rue_leaf', 'S': 'mistech:coarse_salt', 'R': 'mistech:air_rune', 'F': 'minecraft:cauldron', 'C': 'minecraft:copper_ingot'},
    'result': {'id': 'mistech:rue_filter', 'count': 1}})
dump(D + 'mistech/loot_table/blocks/rue_filter.json', {
    'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'mistech:rue_filter'}],
                                          'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
    'random_sequence': 'mistech:blocks/rue_filter'})
p = D + 'minecraft/tags/block/mineable/pickaxe.json'
j = json.load(open(p))
if 'mistech:rue_filter' not in j['values']:
    j['values'].append('mistech:rue_filter')
dump(p, j)


# ---------------------------------------------------------------- modelo: base de cobre, chaminé de tijolos, folhas de arruda
def faces(tex, uvs=None, skip=()):
    out = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        if d in skip:
            continue
        out[d] = {'texture': tex[d] if isinstance(tex, dict) else tex}
        if uvs and d in uvs:
            out[d]['uv'] = uvs[d]
    return out


def box(f, t, tex, uvs=None, skip=(), rot=None, light=None):
    e = {'from': f, 'to': t, 'faces': faces(tex, uvs, skip)}
    if rot:
        e['rotation'] = rot
    if light:
        e['light_emission'] = light
    return e


def side_uv(x0, y0, x1, y1):
    return {d: [x0, 16 - y1, x1, 16 - y0] for d in ('north', 'south', 'east', 'west')}


def rue_filter(on):
    els = [
        box([1, 0, 1], [15, 5, 15], '#copper', {**side_uv(1, 0, 15, 5), 'up': [1, 1, 15, 15], 'down': [1, 1, 15, 15]}),
        box([0.5, 3.5, 0.5], [15.5, 4.5, 15.5], '#dark_copper', side_uv(0, 11, 16, 12), skip=('up', 'down')),
        # corpo da chaminé (o topo é o fundo que brilha)
        box([4, 5, 4], [12, 13, 12], {'north': '#brick', 'south': '#brick', 'east': '#brick', 'west': '#brick', 'up': '#inside', 'down': '#brick'},
            {**side_uv(4, 5, 12, 13), 'up': [4, 4, 12, 12]}, light=8 if on else None),
        # boca da chaminé (aro em volta de um buraco)
        box([3, 13, 3], [13, 15, 5], '#copper', {**side_uv(3, 13, 13, 15), 'up': [3, 3, 13, 5], 'down': [3, 3, 13, 5]}),
        box([3, 13, 11], [13, 15, 13], '#copper', {**side_uv(3, 13, 13, 15), 'up': [3, 11, 13, 13], 'down': [3, 11, 13, 13]}),
        box([3, 13, 5], [5, 15, 11], '#copper', {**side_uv(5, 13, 11, 15), 'up': [3, 5, 5, 11], 'down': [3, 5, 5, 11]}),
        box([11, 13, 5], [13, 15, 11], '#copper', {**side_uv(5, 13, 11, 15), 'up': [11, 5, 13, 11], 'down': [11, 5, 13, 11]}),
        # cinta de cobre no meio da chaminé
        box([3.5, 9, 3.5], [12.5, 10, 12.5], '#dark_copper', side_uv(3, 6, 13, 7), skip=('up', 'down')),
    ]
    for x, z, a in ((2.5, 2.5, 45), (13.5, 2.5, -45), (2.5, 13.5, -45), (13.5, 13.5, 45)):
        for ang in (45, -45):
            els.append({'from': [x - 2.5, 5, z], 'to': [x + 2.5, 11, z], 'shade': False,
                        'rotation': {'origin': [x, 5, z], 'axis': 'y', 'angle': ang},
                        'faces': {'north': {'texture': '#rue', 'uv': [5, 9, 11, 16]}, 'south': {'texture': '#rue', 'uv': [5, 9, 11, 16]}}})
    t = {'particle': 'minecraft:block/cut_copper', 'copper': 'minecraft:block/cut_copper', 'dark_copper': 'minecraft:block/exposed_cut_copper',
         'brick': 'minecraft:block/mud_bricks', 'rue': 'mistech:block/rue_stage3',
         'inside': 'mistech:block/rue_filter_glow' if on else 'mistech:block/rue_filter_inside'}
    return {'parent': 'minecraft:block/block', 'textures': t, 'elements': els}


dump(A + 'models/block/rue_filter.json', rue_filter(False))
dump(A + 'models/block/rue_filter_on.json', rue_filter(True))
bs = {'variants': {}}
for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
    for lit in ('false', 'true'):
        v = {'model': 'mistech:block/rue_filter' + ('_on' if lit == 'true' else '')}
        if r:
            v['y'] = r
        bs['variants']['facing=%s,lit=%s' % (f, lit)] = v
dump(A + 'blockstates/rue_filter.json', bs)
dump(A + 'items/rue_filter.json', {'model': {'type': 'minecraft:model', 'model': 'mistech:block/rue_filter'}})
print('ok dados')

# ---------------------------------------------------------------- textos
LANG = A + 'lang/'
DIRS_PT = {'north': 'norte', 'northeast': 'nordeste', 'east': 'leste', 'southeast': 'sudeste', 'south': 'sul', 'southwest': 'sudoeste',
           'west': 'oeste', 'northwest': 'noroeste'}
DIRS_EN = {'north': 'north', 'northeast': 'northeast', 'east': 'east', 'southeast': 'southeast', 'south': 'south', 'southwest': 'southwest',
           'west': 'west', 'northwest': 'northwest'}
PT = {
    'block.mistech.rue_filter': 'Filtro de Arruda', 'container.mistech.rue_filter': 'Filtro de Arruda',
    'gui.mistech.rue_filter.status.working': 'Filtrando', 'gui.mistech.rue_filter.status.no_leaf': 'Sem arruda',
    'gui.mistech.rue_filter.status.no_energy': 'Sem energia', 'gui.mistech.rue_filter.status.clean': 'Ar limpo',
    'gui.mistech.rue_filter.here': 'Aqui: %s', 'gui.mistech.rue_filter.worst': 'Em volta: %s',
    'info.mistech.area_taint': 'Lugar: %s', 'hud.mistech.area_taint': 'Lugar: %s',
    'message.mistech.pendulum.place': 'lugar: %s',
    'message.mistech.pendulum.seek': 'O pêndulo puxa: %s, %s',
    'message.mistech.pendulum.seek_here': 'bem aqui',
    'message.mistech.pendulum.seek_dir': 'para o %s, a %s blocos',
    'message.mistech.pendulum.seek_clean': 'O pêndulo fica parado: nenhum lugar maculado por perto',
    'commands.mistech.area_taint': 'Chunk %s, %s: ',
    'spell.mistech.purify.desc': 'Limpa efeitos nocivos, fogo e gelo seus e dos aliados (6 blocos) e tira Mácula do lugar.',
    'guide.mistech.sanctuary_rituals.consecrate': 'Limpa todo o Solo Maculado e a Grama Murcha num raio de 16 blocos e zera a Mácula do lugar num raio de 2 chunks.',
    'guide.mistech.dowsing_pendulum.1': 'Radiestesia de bolso. Use no ar para medir a sua Mácula e a do lugar, num bloco para sentir o solo, ou em outro jogador para medir a dele. §5Agachado§r, ele procura o lugar mais maculado num raio de 4 chunks.\n\nSegurando, uma barra acima da hotbar mostra a sua Mácula e a do lugar.',
    'guide.mistech.pure_generator.1': 'Queima Essência Purificada: §5100 FE/t§r, 1 mB por tick (um frasco rende 25.000 FE). Tanque de 4.000 mB que aceita frascos, baldes e canos; envia até 400 FE/t para cada vizinho.\n\nLigado, tira §e1 de Mácula do lugar§r por minuto do chunk dele.',
    'guide.mistech.entry.area_taint': 'Mácula do Lugar',
    'guide.mistech.entry.rue_filter': 'Filtro de Arruda',
    'guide.mistech.area_taint.1': 'A Mácula também fica §5no lugar§r: cada chunk tem a sua, de 0 a 100, com as mesmas faixas. Quem solta, por minuto ligado:\n§5Reator Impuro§r +3\n§5Lavadora de Essência§r +1\n§5Extratores, Compressores e Liquefator§r +0,5\n§5Ritual no Altar Impuro§r +5 cada\n§5Conjurador§r com Escuridão ou Vazio +0,2 por magia',
    'guide.mistech.area_taint.2': '§eFaixas do lugar§r\n§d20+§r: você ganha Mácula aos poucos ali.\n§540+§r: grama murcha, plantas 25% mais lentas.\n§560+§r: mais monstros, névoa roxa, Presságios contam a faixa do lugar.\n§480+§r: terra vira Solo Maculado, máquinas −20%, magia sombria +20%.',
    'guide.mistech.area_taint.3': '§eEspalha e some§r\nAcima de 50, a cada minuto o chunk passa parte do excesso para os vizinhos. Abaixo de 20, some sozinha com sol. Entre os dois, só limpando.\n\nO §5Engaste de Luz§r corta pela metade o que uma máquina solta; o de §5Escuridão§r soma 50%.',
    'guide.mistech.area_taint.4': '§eComo limpar§r\n§5Filtro de Arruda§r: −2 por minuto em 3×3 chunks.\n§5Gerador Purificado§r ligado: −1 por minuto no chunk dele.\n§5Purificar§r (você ou o Conjurador): −10 de uma vez.\n§5Consagração do Solo§r no Santuário: zera num raio de 2 chunks.\n\nO §5Pêndulo§r agachado acha o pior lugar por perto.',
    'guide.mistech.rue_filter.recipe': 'O §2Filtro de Arruda§r queima folhas de arruda para limpar o ar da fábrica.',
    'guide.mistech.rue_filter.1': 'Tira §e2 de Mácula por minuto§r do chunk dele e dos 8 em volta. Uma §2Folha de Arruda§r dura 2 minutos, e ele gasta 20 FE/t.\n\nSó trabalha quando há Mácula por perto: com o ar limpo, não gasta nada. A tela mostra a Mácula do chunk dele e a pior em volta.',
}
EN = {
    'block.mistech.rue_filter': 'Rue Filter', 'container.mistech.rue_filter': 'Rue Filter',
    'gui.mistech.rue_filter.status.working': 'Filtering', 'gui.mistech.rue_filter.status.no_leaf': 'No rue',
    'gui.mistech.rue_filter.status.no_energy': 'No energy', 'gui.mistech.rue_filter.status.clean': 'Air is clean',
    'gui.mistech.rue_filter.here': 'Here: %s', 'gui.mistech.rue_filter.worst': 'Around: %s',
    'info.mistech.area_taint': 'Place: %s', 'hud.mistech.area_taint': 'Place: %s',
    'message.mistech.pendulum.place': 'place: %s',
    'message.mistech.pendulum.seek': 'The pendulum pulls: %s, %s',
    'message.mistech.pendulum.seek_here': 'right here',
    'message.mistech.pendulum.seek_dir': 'to the %s, %s blocks away',
    'message.mistech.pendulum.seek_clean': 'The pendulum stays still: no tainted place nearby',
    'commands.mistech.area_taint': 'Chunk %s, %s: ',
    'spell.mistech.purify.desc': 'Cleanses harmful effects, fire and frost from you and allies (6 blocks) and removes Taint from the place.',
    'guide.mistech.sanctuary_rituals.consecrate': 'Cleanses all Tainted Soil and Withered Grass within 16 blocks and clears the Taint of the place within 2 chunks.',
    'guide.mistech.dowsing_pendulum.1': 'Pocket dowsing. Use it in the air to measure your Taint and the place\'s, on a block to feel the soil, or on another player to measure theirs. §5Sneaking§r, it searches for the most tainted place within 4 chunks.\n\nWhile held, a bar above the hotbar shows your Taint and the place\'s.',
    'guide.mistech.pure_generator.1': 'Burns Purified Essence: §5100 FE/t§r, 1 mB per tick (a bottle yields 25,000 FE). 4,000 mB tank that takes bottles, buckets and pipes; sends up to 400 FE/t to each neighbour.\n\nWhile running, it removes §e1 Taint of the place§r per minute from its chunk.',
    'guide.mistech.entry.area_taint': 'Taint of the Place',
    'guide.mistech.entry.rue_filter': 'Rue Filter',
    'guide.mistech.area_taint.1': 'Taint also stays §5in the place§r: each chunk has its own, from 0 to 100, with the same stages. What releases it, per minute running:\n§5Impure Reactor§r +3\n§5Essence Washer§r +1\n§5Extractors, Compressors and Liquefier§r +0.5\n§5Impure Altar ritual§r +5 each\n§5Conjurer§r with Darkness or Void +0.2 per spell',
    'guide.mistech.area_taint.2': '§ePlace stages§r\n§d20+§r: you slowly gain Taint there.\n§540+§r: grass withers, crops 25% slower.\n§560+§r: more monsters, purple fog, Omens use the place\'s stage.\n§480+§r: dirt turns into Tainted Soil, machines −20%, dark magic +20%.',
    'guide.mistech.area_taint.3': '§eSpreading and fading§r\nAbove 50, every minute the chunk passes part of the excess to its neighbours. Below 20, it fades on its own in sunlight. In between, only cleaning helps.\n\nA §5Light socket§r halves what a machine releases; a §5Darkness§r one adds 50%.',
    'guide.mistech.area_taint.4': '§eHow to cleanse§r\n§5Rue Filter§r: −2 per minute in 3×3 chunks.\n§5Purified Generator§r running: −1 per minute in its chunk.\n§5Purify§r (you or the Conjurer): −10 at once.\n§5Soil Consecration§r at the Sanctuary: clears it within 2 chunks.\n\nThe §5Pendulum§r, sneaking, finds the worst place nearby.',
    'guide.mistech.rue_filter.recipe': 'The §2Rue Filter§r burns rue leaves to clean the factory air.',
    'guide.mistech.rue_filter.1': 'Removes §e2 Taint per minute§r from its chunk and the 8 around it. A §2Rue Leaf§r lasts 2 minutes, and it uses 20 FE/t.\n\nIt only works when there is Taint nearby: with clean air it uses nothing. The screen shows its chunk\'s Taint and the worst around.',
}
for k, v in DIRS_PT.items():
    PT['direction.mistech.' + k] = v
for k, v in DIRS_EN.items():
    EN['direction.mistech.' + k] = v
for name, d in (('pt_br', PT), ('en_us', EN)):
    p = LANG + name + '.json'
    data = json.load(open(p, encoding='utf-8'))
    data.update(d)
    dump(p, data)

# ---------------------------------------------------------------- Grimório (Pureza e Mácula)
BOOK = A + 'guide/book.json'
E = [
    {'id': 'area_taint', 'icon': 'mistech:withered_grass', 'name': 'guide.mistech.entry.area_taint',
     'deps': ['tainted_soil', 'dowsing_pendulum'], 'task': {'type': 'read'}, 'x': 4.8, 'y': 3.0,
     'pages': [{'type': 'text', 'text': 'guide.mistech.area_taint.%d' % i} for i in range(1, 5)]},
    {'id': 'rue_filter', 'icon': 'mistech:rue_filter', 'name': 'guide.mistech.entry.rue_filter',
     'deps': ['area_taint', 'rue'], 'task': {'type': 'item', 'items': ['mistech:rue_filter']}, 'x': 6.4, 'y': 3.2,
     'pages': [{'type': 'crafting', 'grid': RECIPE, 'result': 'mistech:rue_filter', 'text': 'guide.mistech.rue_filter.recipe'},
               {'type': 'text', 'text': 'guide.mistech.rue_filter.1'}]},
]
book = json.load(open(BOOK, encoding='utf-8'))
cat = next(c for c in book['categories'] if c['id'] == 'purity')
ids = {e['id'] for e in E}
cat['entries'] = [e for e in cat['entries'] if e['id'] not in ids] + E
dump(BOOK, book, newline=False)
print('ok textos e Grimório')
