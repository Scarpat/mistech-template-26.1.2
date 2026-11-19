# Gera texturas, modelos, blockstates, itens, loot tables, receitas, tags e textos do
# Barril Extraplanar, Tanque Extraplanar e Cafeteira Rúnica.
# Uso (na raiz do projeto): python3 tools/extraplanar/gen.py
import colorsys
import json
import os
import random

from PIL import Image

RES = 'src/main/resources/'
TEX = RES + 'assets/mistech/textures/block/'
ASSETS = RES + 'assets/mistech/'
DATA = RES + 'data/mistech/'


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


# ------------------------------------------------------------------ texturas
def recolor(src, hue=None, shift=0.0, sat_mul=1.0, val_mul=1.0, sat_min=0.0, remap=()):
    im = Image.open(TEX + src + '.png').convert('RGBA')
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            mapped = None
            for lo, hi, new in remap:
                if lo <= h <= hi and s > 0.3:
                    mapped = new
            if mapped is not None:
                h = mapped
            else:
                h = hue if hue is not None else (h + shift) % 1.0
            s = min(1.0, max(sat_min, s * sat_mul))
            v = min(1.0, v * val_mul)
            r2, g2, b2 = colorsys.hsv_to_rgb(h, s, v)
            px[x, y] = (round(r2 * 255), round(g2 * 255), round(b2 * 255), a)
    return im


def stars(im, seed, count, colors):
    rnd = random.Random(seed)
    px = im.load()
    for _ in range(count):
        x, y = rnd.randrange(1, im.width - 1), rnd.randrange(1, im.height - 1)
        px[x, y] = rnd.choice(colors)
    return im


CYAN_SHIFT = -85 / 360  # roxo (275°) -> ciano (190°)
STAR = [(190, 245, 255, 255), (120, 220, 255, 255), (230, 255, 255, 255)]

stars(recolor('spatial_barrel_side', shift=CYAN_SHIFT, sat_mul=1.05), 11, 7, STAR).save(TEX + 'extraplanar_barrel_side.png')
stars(recolor('spatial_barrel_top', shift=CYAN_SHIFT, sat_mul=1.1, val_mul=1.05, remap=[(0.02, 0.2, 0.5)]), 12, 4, STAR).save(TEX + 'extraplanar_barrel_top.png')
recolor('spatial_barrel_bottom', shift=CYAN_SHIFT).save(TEX + 'extraplanar_barrel_bottom.png')

recolor('runic_tank_frame', hue=190 / 360, sat_min=0.42, val_mul=1.15).save(TEX + 'extraplanar_tank_frame.png')
recolor('runic_tank_top', hue=190 / 360, sat_min=0.42, val_mul=1.15, remap=[(0.7, 0.95, 0.5)]).save(TEX + 'extraplanar_tank_top.png')
recolor('runic_tank_glass', shift=-60 / 360, sat_mul=1.1).save(TEX + 'extraplanar_tank_glass.png')

# Cafeteira: faixa do painel (frente da cabeça), cabine e líquidos das 3 jarras (o resto reaproveita a Chaleira)
rnd = random.Random(7)
front = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
fp = front.load()
dark = [(0x34, 0x2c, 0x28), (0x3e, 0x34, 0x30), (0x48, 0x3e, 0x38)]
for y in range(5):
    for x in range(16):
        fp[x, y] = dark[rnd.randrange(3)] + (255,)
copper = [(0xc6, 0x6c, 0x4c), (0xd8, 0x7a, 0x56), (0xba, 0x62, 0x46)]
for x in range(16):
    fp[x, 0] = copper[x % 3] + (255,)
    fp[x, 4] = copper[(x + 1) % 3] + (255,)
# três lâmpadas, uma por linha: café, beladona, cogumelo
lamps = [((0xb8, 0x6a, 0x3a), (0xff, 0xb0, 0x70)), ((0x3f, 0xb8, 0x58), (0xb8, 0xff, 0xc0)), ((0xd8, 0xb0, 0x28), (0xff, 0xf0, 0x90))]
for i, (mid, hi) in enumerate(lamps):
    lx = 2 + i * 5
    fp[lx, 2] = mid + (255,)
    fp[lx + 1, 2] = mid + (255,)
    fp[lx, 1] = hi + (255,)
    fp[lx + 1, 1] = mid + (255,)
    fp[lx, 3] = (0x20, 0x1a, 0x16, 255)
    fp[lx + 1, 3] = (0x20, 0x1a, 0x16, 255)
shifted = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
shifted.paste(front.crop((0, 0, 16, 5)), (0, 1))
shifted.save(TEX + 'runic_coffee_maker_front.png')


def liquid(name, tones):
    im = Image.new('RGBA', (16, 16))
    p = im.load()
    for y in range(16):
        for x in range(16):
            p[x, y] = tones[rnd.randrange(len(tones))] + (255,)
    im.save(TEX + name + '.png')


liquid('runic_coffee_maker_coffee', [(0x3b, 0x24, 0x18), (0x4a, 0x2e, 0x1f), (0x2e, 0x1b, 0x12), (0x55, 0x36, 0x24)])
liquid('runic_coffee_maker_tea', [(0x3a, 0x7a, 0x3a), (0x46, 0x8a, 0x44), (0x2e, 0x66, 0x30), (0x56, 0x9a, 0x50)])
liquid('runic_coffee_maker_mushroom', [(0xd8, 0xb0, 0x30), (0xe6, 0xc0, 0x40), (0xc0, 0x98, 0x28), (0xf0, 0xd0, 0x58)])

# ------------------------------------------------------------------ modelos
FACES = ['north', 'south', 'east', 'west', 'up', 'down']


def el(frm, to, tex, name, per=None, skip=()):
    faces = {}
    for f in FACES:
        if f in skip:
            continue
        faces[f] = {'texture': (per or {}).get(f, tex)}
    return {'from': frm, 'to': to, 'faces': faces, 'name': name}


# Barril: cubo com topo/lados/fundo
write_json(ASSETS + 'models/block/extraplanar_barrel.json', {
    'parent': 'minecraft:block/cube_bottom_top',
    'textures': {'bottom': 'mistech:block/extraplanar_barrel_bottom', 'side': 'mistech:block/extraplanar_barrel_side',
                 'top': 'mistech:block/extraplanar_barrel_top'}})
barrel_states = {
    'facing=down': {'model': 'mistech:block/extraplanar_barrel', 'x': 180},
    'facing=up': {'model': 'mistech:block/extraplanar_barrel'},
    'facing=north': {'model': 'mistech:block/extraplanar_barrel', 'x': 90},
    'facing=south': {'model': 'mistech:block/extraplanar_barrel', 'x': 90, 'y': 180},
    'facing=west': {'model': 'mistech:block/extraplanar_barrel', 'x': 90, 'y': 270},
    'facing=east': {'model': 'mistech:block/extraplanar_barrel', 'x': 90, 'y': 90},
}
write_json(ASSETS + 'blockstates/extraplanar_barrel.json', {'variants': barrel_states})

# Tanque: o mesmo modelo do Tanque Rúnico com as texturas novas
tank = json.load(open(ASSETS + 'models/block/runic_tank.json', encoding='utf-8'))
tank['textures'] = {'particle': 'mistech:block/extraplanar_tank_frame', 'frame': 'mistech:block/extraplanar_tank_frame',
                    'top': 'mistech:block/extraplanar_tank_top', 'glass': 'mistech:block/extraplanar_tank_glass'}
write_json(ASSETS + 'models/block/extraplanar_tank.json', tank)
write_json(ASSETS + 'blockstates/extraplanar_tank.json', {'variants': {'': {'model': 'mistech:block/extraplanar_tank'}}})

# Cafeteira (frente = norte): cabine escura atrás, cabeça de cobre com 3 lâmpadas e 3 jarras de vidro lado a lado
maker = {
    'parent': 'minecraft:block/block',
    'textures': {
        'particle': 'mistech:block/kettle_body', 'body': 'mistech:block/kettle_body', 'dark': 'mistech:block/kettle_dark',
        'front': 'mistech:block/runic_coffee_maker_front', 'glass': 'mistech:block/runic_tank_glass',
        'coffee': 'mistech:block/runic_coffee_maker_coffee', 'tea': 'mistech:block/runic_coffee_maker_tea',
        'mushroom': 'mistech:block/runic_coffee_maker_mushroom'},
    'elements': [
        el([0, 0, 0], [16, 2, 16], '#dark', 'base'),
        el([1, 2, 11], [15, 11, 15], '#dark', 'cabine'),
        el([0, 11, 4], [16, 15, 15], '#body', 'cabeca', per={'north': '#front', 'up': '#body'}),
        el([0, 15, 4], [16, 16, 15], '#dark', 'tampa'),
        el([1, 2, 5], [15, 2.6, 11], '#dark', 'grade'),
    ],
}
for i, (liq, x0) in enumerate((('#coffee', 1.0), ('#tea', 5.75), ('#mushroom', 10.5))):
    maker['elements'].append(el([x0 + 1.5, 10.2, 6.5], [x0 + 3.0, 11, 8], '#dark', 'bico%d' % i))
    maker['elements'].append(el([x0, 2.6, 5], [x0 + 4.5, 10.2, 10], '#glass', 'jarra%d' % i, skip=('down',)))
write_json(ASSETS + 'models/block/runic_coffee_maker.json', maker)
# líquido de cada jarra: 4 níveis (propriedades jar0..jar2 do bloco), um modelo por linha e nível
parts = []
rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
for facing, y in rot.items():
    apply = {'model': 'mistech:block/runic_coffee_maker'}
    if y:
        apply['y'] = y
    parts.append({'when': {'facing': facing}, 'apply': apply})
for line, (liq, x0) in enumerate((('coffee', 1.0), ('tea', 5.75), ('mushroom', 10.5))):
    for level in range(1, 5):
        top = 2.7 + 6.9 * level / 4
        write_json(ASSETS + 'models/block/runic_coffee_maker_liquid%d_%d.json' % (line, level), {
            'textures': {'liquid': 'mistech:block/runic_coffee_maker_' + liq, 'particle': 'mistech:block/runic_coffee_maker_' + liq},
            'elements': [el([x0 + 0.5, 2.7, 5.5], [x0 + 4.0, top, 9.5], '#liquid', 'liquido', skip=('down',))]})
        for facing, y in rot.items():
            apply = {'model': 'mistech:block/runic_coffee_maker_liquid%d_%d' % (line, level)}
            if y:
                apply['y'] = y
            parts.append({'when': {'AND': [{'facing': facing}, {'jar%d' % line: str(level)}]}, 'apply': apply})
write_json(ASSETS + 'blockstates/runic_coffee_maker.json', {'multipart': parts})

for name in ('extraplanar_barrel', 'extraplanar_tank', 'runic_coffee_maker'):
    write_json(ASSETS + 'items/%s.json' % name, {'model': {'type': 'minecraft:model', 'model': 'mistech:block/%s' % name}})


# ------------------------------------------------------------------ loot tables
def loot(name, includes):
    entry = {'type': 'minecraft:item', 'name': 'mistech:' + name}
    if includes:
        entry['functions'] = [{'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': includes}]
    write_json(DATA + 'loot_table/blocks/%s.json' % name, {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': [entry],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': 'mistech:blocks/' + name})


loot('extraplanar_barrel', ['mistech:extraplanar_code', 'minecraft:custom_name'])
loot('extraplanar_tank', ['mistech:extraplanar_code'])
loot('runic_coffee_maker', None)

# ------------------------------------------------------------------ receitas
write_json(DATA + 'recipe/extraplanar_barrel.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc',
    'pattern': ['EME', 'RBR', 'EME'],
    'key': {'E': 'minecraft:ender_pearl', 'M': '#c:ingots/mithril', 'R': 'mistech:space_rune', 'B': 'mistech:spatial_barrel'},
    'result': {'id': 'mistech:extraplanar_barrel', 'count': 2}})
write_json(DATA + 'recipe/extraplanar_tank.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc',
    'pattern': ['EME', 'RTR', 'EME'],
    'key': {'E': 'minecraft:ender_pearl', 'M': '#c:ingots/mithril', 'R': 'mistech:space_rune', 'T': 'mistech:runic_tank'},
    'result': {'id': 'mistech:extraplanar_tank', 'count': 2}})
write_json(DATA + 'recipe/runic_coffee_maker.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc',
    'pattern': ['CHC', 'GKG', 'WFW'],
    'key': {'C': 'minecraft:copper_ingot', 'H': 'minecraft:hopper', 'G': '#c:glass_blocks', 'K': 'mistech:kettle',
            'W': 'mistech:water_rune', 'F': 'mistech:fire_rune'},
    'result': {'id': 'mistech:runic_coffee_maker', 'count': 1}})


# ------------------------------------------------------------------ tags de mineração
def add_tag(path, values):
    d = json.load(open(path, encoding='utf-8'))
    changed = False
    for v in values:
        if v not in d['values']:
            d['values'].append(v)
            changed = True
    if changed:
        write_json(path, d)


add_tag(RES + 'data/minecraft/tags/block/mineable/axe.json', ['mistech:extraplanar_barrel', 'mistech:extraplanar_tank'])
add_tag(RES + 'data/minecraft/tags/block/mineable/pickaxe.json', ['mistech:runic_coffee_maker'])

# ------------------------------------------------------------------ textos
PT = {
    'block.mistech.extraplanar_barrel': 'Barril Extraplanar',
    'container.mistech.extraplanar_barrel': 'Barril Extraplanar',
    'container.mistech.extraplanar_barrel.code': 'Barril Extraplanar [%s]',
    'block.mistech.extraplanar_tank': 'Tanque Extraplanar',
    'block.mistech.runic_coffee_maker': 'Cafeteira Rúnica',
    'tooltip.mistech.extraplanar.code': 'Código: %s',
    'tooltip.mistech.extraplanar.no_code': 'Sem código (Shift + botão direito com a mão vazia)',
    'message.mistech.extraplanar.no_code': 'Sem código. Shift + botão direito com a mão vazia para definir',
    'message.mistech.extraplanar.code_set': 'Código definido: %s',
    'message.mistech.extraplanar.code_cleared': 'Código removido',
    'message.mistech.extraplanar.tank': '[%1$s] %2$s: %3$s/%4$s baldes',
    'message.mistech.extraplanar.tank_empty': '[%1$s] Vazio',
    'screen.mistech.extraplanar.title': 'Código Extraplanar',
    'screen.mistech.extraplanar.code': 'Código',
    'screen.mistech.extraplanar.hint': 'Digite o código...',
    'screen.mistech.extraplanar.confirm': 'Confirmar',
    'screen.mistech.extraplanar.clear': 'Remover código',
    'screen.mistech.extraplanar.info': 'Blocos com o mesmo código dividem o conteúdo',
    'message.mistech.coffee_maker.tank': '%1$s: %2$s B',
    'message.mistech.coffee_maker.no_fluid': 'Sem líquidos',
    'message.mistech.coffee_maker.line': '%1$s: %2$s/%3$s',
    'gui.mistech.coffee_maker.fill': 'Encher',
    'gui.mistech.coffee_maker.servings': '%1$s: %2$s/%3$s doses',
        'gui.mistech.coffee_maker.line.0': 'Café',
    'gui.mistech.coffee_maker.line.1': 'Beladona',
    'gui.mistech.coffee_maker.line.2': 'Cogumelo',
    'gui.mistech.coffee_maker.cups': 'Xícaras',
    'gui.mistech.coffee_maker.fluids': 'Líquidos',
    'gui.mistech.coffee_maker.tank_water': 'Água (enche sozinha)',
    'gui.mistech.coffee_maker.tank_empty': 'Vazio',
}
EN = {
    'block.mistech.extraplanar_barrel': 'Extraplanar Barrel',
    'container.mistech.extraplanar_barrel': 'Extraplanar Barrel',
    'container.mistech.extraplanar_barrel.code': 'Extraplanar Barrel [%s]',
    'block.mistech.extraplanar_tank': 'Extraplanar Tank',
    'block.mistech.runic_coffee_maker': 'Runic Coffee Maker',
    'tooltip.mistech.extraplanar.code': 'Code: %s',
    'tooltip.mistech.extraplanar.no_code': 'No code (Shift + right-click with an empty hand)',
    'message.mistech.extraplanar.no_code': 'No code. Shift + right-click with an empty hand to set one',
    'message.mistech.extraplanar.code_set': 'Code set: %s',
    'message.mistech.extraplanar.code_cleared': 'Code removed',
    'message.mistech.extraplanar.tank': '[%1$s] %2$s: %3$s/%4$s buckets',
    'message.mistech.extraplanar.tank_empty': '[%1$s] Empty',
    'screen.mistech.extraplanar.title': 'Extraplanar Code',
    'screen.mistech.extraplanar.code': 'Code',
    'screen.mistech.extraplanar.hint': 'Type the code...',
    'screen.mistech.extraplanar.confirm': 'Confirm',
    'screen.mistech.extraplanar.clear': 'Remove code',
    'screen.mistech.extraplanar.info': 'Blocks with the same code share their contents',
    'message.mistech.coffee_maker.tank': '%1$s: %2$s B',
    'message.mistech.coffee_maker.no_fluid': 'No liquids',
    'message.mistech.coffee_maker.line': '%1$s: %2$s/%3$s',
    'gui.mistech.coffee_maker.fill': 'Fill',
    'gui.mistech.coffee_maker.servings': '%1$s: %2$s/%3$s servings',
        'gui.mistech.coffee_maker.line.0': 'Coffee',
    'gui.mistech.coffee_maker.line.1': 'Belladonna',
    'gui.mistech.coffee_maker.line.2': 'Mushroom',
    'gui.mistech.coffee_maker.cups': 'Cups',
    'gui.mistech.coffee_maker.fluids': 'Liquids',
    'gui.mistech.coffee_maker.tank_water': 'Water (fills itself)',
    'gui.mistech.coffee_maker.tank_empty': 'Empty',
}


def add_lang(path, entries):
    text = open(path, encoding='utf-8').read()
    have = json.loads(text)
    new = [(k, v) for k, v in entries.items() if k not in have]
    if not new:
        return
    body = text.rstrip()
    assert body.endswith('}')
    body = body[:-1].rstrip()
    lines = ',\n'.join('  %s: %s' % (json.dumps(k, ensure_ascii=False), json.dumps(v, ensure_ascii=False)) for k, v in new)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(body + ',\n' + lines + '\n}\n')


add_lang(ASSETS + 'lang/pt_br.json', PT)
add_lang(ASSETS + 'lang/en_us.json', EN)
print('ok')
