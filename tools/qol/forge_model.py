"""Modelo da Forja Trituradora montada (2×2×2) — gera:
  models/block/crusher_forge_formed.json e _on.json  (desenhado a partir do núcleo; voltado para o norte)
  models/block/forge_part_hidden.json                 (peças escondidas enquanto montada)
Coordenadas do núcleo virado para o norte: frente em z=0, direita de quem olha = -x, trás = +z.
O cubo ocupa x -16..16, y 0..32, z 0..32 (limite dos modelos: -16..32).
Uso: python3 tools/qol/forge_model.py
"""
import json
import math
import os

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'mistech', 'models', 'block')
FACES = ['north', 'south', 'east', 'west', 'up', 'down']


def tiled(frm, to, tex, faces=FACES, extra=None):
    """Caixa dividida na grade de 16 para a textura repetir sem esticar (UV pela posição, como o jogo faz num bloco)."""
    els = []
    xs = cuts(frm[0], to[0])
    ys = cuts(frm[1], to[1])
    zs = cuts(frm[2], to[2])
    for (x0, x1) in xs:
        for (y0, y1) in ys:
            for (z0, z1) in zs:
                fc = {}
                for f in faces:
                    # só as faces de fora da caixa grande
                    if f == 'north' and z0 != frm[2] or f == 'south' and z1 != to[2] or f == 'west' and x0 != frm[0] \
                            or f == 'east' and x1 != to[0] or f == 'down' and y0 != frm[1] or f == 'up' and y1 != to[1]:
                        continue
                    fc[f] = {'texture': '#' + tex, 'uv': uv(f, x0, y0, z0, x1, y1, z1)}
                if fc:
                    e = {'from': [x0, y0, z0], 'to': [x1, y1, z1], 'faces': fc}
                    if extra:
                        e.update(extra)
                    els.append(e)
    return els


def cuts(a, b):
    out = []
    x = a
    while x < b:
        nxt = min(b, (math.floor(x / 16) + 1) * 16)
        out.append((x, nxt))
        x = nxt
    return out


def m16(v, base):
    return v - base


def uv(face, x0, y0, z0, x1, y1, z1):
    bx, by, bz = math.floor(x0 / 16) * 16, math.floor(y0 / 16) * 16, math.floor(z0 / 16) * 16
    x0, x1, y0, y1, z0, z1 = x0 - bx, x1 - bx, y0 - by, y1 - by, z0 - bz, z1 - bz
    return {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
            'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0], 'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}[face]


def box(frm, to, faces, extra=None):
    """Caixa pequena (até 16) com UV por face dado: faces = {face: (textura, [u0,v0,u1,v1])}."""
    e = {'from': frm, 'to': to, 'faces': {f: {'texture': '#' + t, 'uv': u} for f, (t, u) in faces.items()}}
    if extra:
        e.update(extra)
    return e


def model(on):
    """Embaixo a forja arcana (boca com fogo arcano, janelas laterais, mesa de ouro com a fenda brilhando);
    em cima a moldura aberta onde ficam as duas mós (desenhadas e giradas pelo CrusherForgeRenderer)."""
    els = []
    glow = {'light_emission': 15} if on else None
    rune_glow = {'light_emission': 15 if on else 6}
    sides4 = ['north', 'south', 'east', 'west']
    # ---- forja (y 0..16) ----
    els += tiled([-16, 0, 0], [16, 2, 32], 'casing')                                   # plinto
    els += tiled([-15, 2, 6], [15, 13, 31], 'brick', ['south', 'east', 'west', 'north'])  # corpo atrás da boca
    for x0, x1 in [(-15, -8), (8, 15)]:                                                # pilares da boca
        els += tiled([x0, 2, 1], [x1, 13, 6], 'brick', ['north', 'east', 'west'])
    els += tiled([-8, 11, 1], [8, 13, 6], 'brick', ['north', 'down'])                  # verga
    els.append(box([-8, 2, 1], [8, 3, 6], {'up': ('casing', [0, 0, 16, 5])}))           # soleira
    els.append(box([-8, 3, 5.9], [8, 11, 6], {'north': ('mouth', [0, 4, 16, 12])}, glow))  # fundo da boca (fogo)
    for x0, x1, face in [(-15.5, -15, 'west'), (15, 15.5, 'east')]:                    # janelas de fogo nas laterais
        els.append(box([x0, 4, 11], [x1, 10, 21], {face: ('mouth', [0, 8, 16, 14])}, glow))
    for x0, x1 in [(-14, -9), (9, 14)]:                                                # runas dos dois lados da boca
        els.append(box([x0, 4, 0.5], [x1, 10, 1], {'north': ('rune', [3, 2, 13, 14])}, rune_glow))
    els += tiled([-16, 13, 0], [16, 14, 32], 'trim', sides4)                           # cinta de ouro
    els += tiled([-16, 14, 0], [16, 16, 32], 'casing', sides4)                         # mesa
    els += tiled([-16, 14, 0], [16, 16, 32], 'deck', ['up'])
    els.append(box([-3, 16, 6], [3, 16.05, 26], {'up': ('mouth', [5, 0, 11, 16])}, glow))  # fenda onde cai o pó
    # ---- moldura das mós (y 16..32) ----
    for x0, x1 in [(-16, -13), (13, 16)]:
        for z0, z1 in [(0, 3), (29, 32)]:
            els.append(box([x0, 16, z0], [x1, 29, z1], {f: ('casing', [6, 1, 9, 14]) for f in sides4}))
    for x0, x1 in [(-16, -13), (13, 16)]:                                              # trilhos laterais (lados abertos: as mós aparecem)
        els += tiled([x0, 16, 3], [x1, 18, 29], 'casing', ['up', 'east', 'west'])
    els += tiled([-13, 16, 30], [13, 29, 32], 'casing', ['north', 'south'])           # fundo
    for cx in (-7.5, 7.5):                                                                 # mancais dos eixos
        els.append(box([cx - 2, 22, 29], [cx + 2, 26, 30], {f: ('gold', [0, 0, 4, 4]) for f in ['north', 'east', 'west', 'up', 'down']}))
    ring = ['north', 'south', 'east', 'west', 'down']
    els += tiled([-16, 29, 0], [16, 32, 3], 'casing', ring)                            # aro de cima (o topo é a cinta)
    els += tiled([-16, 29, 29], [16, 32, 32], 'casing', ring)
    for x0, x1 in [(-16, -13), (13, 16)]:
        els += tiled([x0, 29, 3], [x1, 32, 29], 'casing', ['up', 'down', 'east', 'west'])
    els += tiled([-16, 31.9, 0], [16, 32, 3], 'trim', ['up'])
    els += tiled([-16, 31.9, 29], [16, 32, 32], 'trim', ['up'])
    els.append(box([-3, 29.5, -0.5], [3, 31.5, 0], {'north': ('rune', [5, 6, 11, 10])}, rune_glow))
    tex = {
        'particle': 'mistech:block/arcane_casing',
        'casing': 'mistech:block/arcane_casing',
        'brick': 'minecraft:block/polished_blackstone_bricks',
        'deck': 'mistech:block/crusher_forge_top',
        'trim': 'mistech:block/arcane_crusher_trim',
        'side': 'mistech:block/arcane_furnace_side',
        'mouth': 'mistech:block/forge_mouth' + ('_on' if on else ''),
        'rune': 'mistech:block/arcane_furnace_top',
        'gold': 'minecraft:block/gold_block',
    }
    return {'ambientocclusion': False, 'textures': tex, 'elements': els}


def main():
    import importlib.util
    spec = importlib.util.spec_from_file_location('forge_textures', os.path.join(os.path.dirname(__file__), 'forge_textures.py'))
    ft = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(ft)
    ft.main()
    for on in (False, True):
        name = 'crusher_forge_formed' + ('_on' if on else '')
        with open(os.path.join(OUT, name + '.json'), 'w', encoding='utf-8') as f:
            json.dump(model(on), f, indent=1)
    with open(os.path.join(OUT, 'forge_part_hidden.json'), 'w', encoding='utf-8') as f:
        json.dump({'textures': {'particle': 'mistech:block/arcane_casing'}, 'elements': []}, f, indent=1)
    blockstates()
    print('ok', len(model(True)['elements']), 'elementos')


def blockstates():
    """Estados com "formed": núcleo = modelo grande; Fornalha/Triturador Arcanos e Blocos de Magia Impura = escondidos."""
    bs = os.path.join(OUT, '..', '..', 'blockstates')
    rot = [('north', 0), ('east', 90), ('south', 180), ('west', 270)]

    def write(name, variants):
        with open(os.path.join(bs, name + '.json'), 'w', encoding='utf-8') as f:
            json.dump({'variants': variants}, f, indent=2)

    for name in ['crusher_forge', 'arcane_furnace', 'arcane_crusher']:
        v = {}
        for facing, y in rot:
            for lit in ['false', 'true']:
                for formed in ['false', 'true']:
                    if formed == 'true':
                        model = 'mistech:block/crusher_forge_formed' + ('_on' if lit == 'true' else '') if name == 'crusher_forge' \
                            else 'mistech:block/forge_part_hidden'
                    else:
                        model = 'mistech:block/' + name + ('_on' if lit == 'true' else '')
                    m = {'model': model}
                    if y and model != 'mistech:block/forge_part_hidden':
                        m['y'] = y
                    v['facing=%s,formed=%s,lit=%s' % (facing, formed, lit)] = m
        write(name, v)
    write('impure_magic_block', {'formed=false': {'model': 'mistech:block/impure_magic_block'},
                                 'formed=true': {'model': 'mistech:block/forge_part_hidden'}})


if __name__ == '__main__':
    main()
