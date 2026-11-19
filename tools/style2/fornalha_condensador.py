"""Fornalha Runica e Condensador: 3 versoes de cada (so previa; nao mexe no mod).
Uso (raiz do projeto): python3 tools/style2/fornalha_condensador.py  -> tools/style2/out/fc/versoes_fornalha.png e versoes_condensador.png
Regras: materiais da receita, texturas vanilla sem repintar, textura presa a peca (uv pela posicao), sem frestas."""
import sys, os, json
from PIL import Image, ImageDraw
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '../..')); os.chdir(ROOT)
OUT = os.path.join(HERE, 'out/fc'); M = OUT + '/models'
os.makedirs(M, exist_ok=True)
os.environ['STYLE_OVERRIDE'] = OUT
sys.path.insert(0, 'tools/style'); sys.path.insert(0, 'tools/boitata')
import blockview2 as bv


def c16(v):
    return max(0, min(16, v))


def B(fr, to, tex, rot=None, **over):
    """Caixa com a textura mapeada pela posicao (cada pixel do bloco = 1 pixel da textura)."""
    faces = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        if d in ('north', 'south'):
            uv = [fr[0], 16 - to[1], to[0], 16 - fr[1]]
        elif d in ('east', 'west'):
            uv = [fr[2], 16 - to[1], to[2], 16 - fr[1]]
        else:
            uv = [fr[0], fr[2], to[0], to[2]]
        faces[d] = {'texture': tex, 'uv': [c16(v) for v in uv]}
    for d, t in over.items():
        if t is None:
            faces.pop(d, None)
        else:
            faces[d] = {'texture': t, 'uv': faces[d]['uv']}
    e = {'from': list(fr), 'to': list(to), 'faces': faces}
    if rot:
        e['rotation'] = rot
    return e


def tag(x, y, z, tex):
    """Etiqueta da runa da receita (3x5), pendurada na frente."""
    uv = [0, 0, 3, 5]
    f = {d: {'texture': tex, 'uv': uv} for d in ('north', 'south')}
    for d in ('east', 'west', 'up', 'down'):
        f[d] = {'texture': tex, 'uv': [0, 1, 1, 2]}
    return {'from': [x, y, z], 'to': [x + 3, y + 5, z + 0.5], 'faces': f}


def mouth_walls(x0, x1, ytop, depth, y0, y1, tex, lo=1, hi=15, soot='#soot'):
    """Corpo com a boca aberta de verdade na frente (norte): abertura x0..x1, de y0 ate ytop, com 'depth' de fundo."""
    return [
        B((lo, y0, lo), (x0, y1, hi), tex),
        B((x1, y0, lo), (hi, y1, hi), tex),
        B((x0, ytop, lo), (x1, y1, hi), tex, down=soot),
        B((x0, y0, lo + depth), (x1, ytop, hi), tex, north=soot),
    ]


# ------------------------------------------------------------------ FORNALHA RUNICA (fornalha + cobre + cinza de incenso + runa de fogo)
def fornalha_a(lit):
    """Forno com coifa e chamine de cobre."""
    els = [B((0, 0, 0), (16, 2, 16), '#cobble')]
    els += mouth_walls(5, 11, 7, 5, 2, 10, '#stone')
    els.append(B((5, 2, 1.5), (11, 3, 6), '#ember' if lit else '#ash'))
    els.append(B((0.5, 10, 0.5), (15.5, 11, 15.5), '#copper'))
    els.append(B((1, 11, 1), (15, 12.5, 15), '#cut'))
    els.append(B((3, 12.5, 3), (13, 14, 13), '#cut'))
    els.append(B((6, 14, 8), (10, 16, 12), '#copper', up='#ember' if lit else '#soot'))
    els.append(tag(1.5, 4, 0.5, '#tag'))
    return els


def fornalha_b(lit):
    """Fogao de chapa: o item sendo assado apareceria deitado na chapa (renderer)."""
    els = [B((0, 0, 0), (16, 2, 16), '#cobble')]
    els += mouth_walls(5, 11, 7, 5, 2, 10, '#stone')
    els.append(B((5, 2, 1.5), (11, 3, 6), '#ember' if lit else '#ash'))
    els.append(B((0, 10, 0), (16, 11.5, 16), '#cut'))
    for fr, to in (((0, 11.5, 0), (16, 12.5, 1)), ((0, 11.5, 15), (16, 12.5, 16)), ((0, 11.5, 1), (1, 12.5, 15)), ((15, 11.5, 1), (16, 12.5, 15))):
        els.append(B(fr, to, '#copper'))
    els.append(B((3, 11.5, 3), (10, 11.75, 10), '#ember' if lit else '#soot'))
    els.append(B((11, 11.5, 11), (14, 16, 14), '#copper', up='#soot'))
    els.append(tag(1.5, 4, 0.5, '#tag'))
    return els


def fornalha_c(lit):
    """Gaiola: brasa em cubo flutuando sobre a cinza (gira e pulsa quando ligada)."""
    els = [B((0, 0, 0), (16, 4, 16), '#stone', up='#ftop')]
    els.append(B((3, 4, 3), (13, 4.5, 13), '#ash'))
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        els.append(B((x, 4, z), (x + 2, 12, z + 2), '#cut'))
    for fr, to, faces in (((3, 4, 14), (13, 12, 14), ('north', 'south')), ((2, 4, 3), (2, 12, 13), ('east', 'west')), ((14, 4, 3), (14, 12, 13), ('east', 'west'))):
        e = B(fr, to, '#bars')
        e['faces'] = {d: {'texture': '#bars', 'uv': [3, 4, 13, 12]} for d in faces}
        els.append(e)
    els.append(B((0.5, 12, 0.5), (15.5, 14, 15.5), '#copper'))
    els.append(B((1.5, 14, 1.5), (14.5, 16, 14.5), '#stone', up='#ftop'))
    els.append(B((5.5, 6, 5.5), (10.5, 11, 10.5), '#ember' if lit else '#coal', rot={'origin': [8, 8, 8], 'axis': 'y', 'angle': 45}))
    els.append(tag(6.5, 9.5, 0, '#tag'))
    return els


F_TEX = {'cobble': 'minecraft:block/cobblestone', 'stone': 'minecraft:block/furnace_side', 'ftop': 'minecraft:block/furnace_top',
         'copper': 'minecraft:block/copper_block', 'cut': 'minecraft:block/cut_copper', 'ember': 'minecraft:block/magma',
         'ash': 'minecraft:block/light_gray_concrete_powder', 'soot': 'minecraft:block/blackstone', 'coal': 'minecraft:block/coal_block',
         'bars': 'minecraft:block/iron_bars', 'tag': 'mistech:block/fc_tag_fire', 'particle': 'minecraft:block/furnace_side'}


# ------------------------------------------------------------------ CONDENSADOR (fornalha + 4 runas de gelo)
def condensador_d(lit):
    """Forma: o bloco (gelo ou obsidiana) vai se formando dentro da boca, de baixo para cima."""
    els = [B((0, 0, 0), (16, 2, 16), '#cobble')]
    els += mouth_walls(4, 12, 10, 8, 2, 12, '#stone', soot='#packed')
    if lit:
        els.append(B((5, 2, 2), (11, 6, 8), '#ice'))
    for x, h in ((4.5, 2.5), (7.5, 1.5), (10.5, 2)):
        els.append(B((x, 10 - h, 1.5), (x + 1, 10, 2.5), '#packed'))
    els.append(B((0.5, 12, 0.5), (15.5, 13.5, 15.5), '#snow'))
    els.append(B((2, 13.5, 3), (9, 14.5, 9), '#snow'))
    els.append(tag(1, 5, 0.5, '#tag'))
    return els


def condensador_e(lit):
    """Pedestal com cristal de gelo flutuando (gira quando trabalha; fica azul vivo ligado)."""
    els = [B((0.5, 0, 0.5), (15.5, 4, 15.5), '#stone', up='#ftop')]
    els.append(B((2, 4, 2), (14, 5, 14), '#snow'))
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        els.append(B((x, 4, z), (x + 2, 8, z + 2), '#packed'))
        els.append(B((x + 0.5, 8, z + 0.5), (x + 1.5, 10, z + 1.5), '#packed'))
    rot = {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45}
    gem = '#blue' if lit else '#packed'
    els.append(B((5, 8, 5), (11, 13, 11), gem, rot=dict(rot)))
    els.append(B((6.5, 13, 6.5), (9.5, 15, 9.5), gem, rot=dict(rot)))
    els.append(B((6.5, 6, 6.5), (9.5, 8, 9.5), gem, rot=dict(rot)))
    els.append(tag(6.5, 0, 0, '#tag'))
    return els


def condensador_f(lit):
    """Torre de aletas: nucleo de pedra, aletas de gelo e um rotor em cima que gira quando trabalha."""
    els = [B((0, 0, 0), (16, 1.5, 16), '#cobble')]
    els.append(B((3, 1.5, 3), (13, 13, 13), '#stone'))
    for y in (2.5, 5.5, 8.5, 11.5):
        els.append(B((1, y, 1), (15, y + 1, 15), '#packed'))
    els.append(B((2, 13, 2), (14, 14, 14), '#ftop'))
    blade = '#blue' if lit else '#packed'
    rot = {'origin': [8, 15, 8], 'axis': 'y', 'angle': 22.5 if lit else 0}
    els.append(B((2, 14.5, 7), (14, 15.5, 9), blade, rot=dict(rot)))
    els.append(B((7, 14.5, 2), (9, 15.5, 14), blade, rot=dict(rot)))
    els.append(B((6.5, 14, 6.5), (9.5, 16, 9.5), '#stone'))
    els.append(tag(6.5, 6.5, 0.5, '#tag'))
    return els


C_TEX = {'cobble': 'minecraft:block/cobblestone', 'stone': 'minecraft:block/furnace_side', 'ftop': 'minecraft:block/furnace_top',
         'packed': 'minecraft:block/packed_ice', 'blue': 'minecraft:block/blue_ice', 'ice': 'minecraft:block/ice', 'snow': 'minecraft:block/snow',
         'tag': 'mistech:block/fc_tag_ice', 'particle': 'minecraft:block/packed_ice'}


# ================================================================== RODADA 2 (16/11): cubo cheio, materiais do proprio mod, mais mistico
def shell(tex, top, inner, sill_up, ceil):
    """Cubo cheio com uma camara aberta na frente (x3..13, y2..11, 8 de fundo). A silhueta continua sendo o bloco inteiro."""
    return [
        B((0, 0, 0), (3, 16, 16), tex, east=inner, up=top),
        B((13, 0, 0), (16, 16, 16), tex, west=inner, up=top),
        B((3, 0, 0), (13, 2, 16), tex, up=sill_up),
        B((3, 12, 0), (13, 16, 16), tex, up=top),
        B((3, 11, 0), (13, 12, 8), ceil),
        B((3, 2, 8), (13, 12, 16), tex, north=inner),
    ]


def face_cube(fr, to, tex, face_tex, n, rot=None):
    e = B(fr, to, tex, rot=rot)
    e['faces']['north'] = {'texture': face_tex, 'uv': [0, 0, n, n]}
    return e


def chalk(tex):
    return {'from': [2, 16, 2], 'to': [14, 16.05, 14], 'faces': {'up': {'texture': tex, 'uv': [0, 0, 16, 16]}}}


def gem(cx, cy, cz, tex):
    """Cristal: cubo girado 45 graus com duas pontas."""
    rot = {'origin': [cx, cy, cz], 'axis': 'y', 'angle': 45}
    return [B((cx - 2, cy - 2.5, cz - 2), (cx + 2, cy + 2.5, cz + 2), tex, rot=dict(rot)),
            B((cx - 1, cy + 2.5, cz - 1), (cx + 1, cy + 4, cz + 1), tex, rot=dict(rot)),
            B((cx - 1, cy - 4, cz - 1), (cx + 1, cy - 2.5, cz + 1), tex, rot=dict(rot))]


def fornalha_g(lit):
    """Espirito de brasa: mora na camara, dorme na cinza; ligada, acorda, acende e acompanha o jogador com o olho."""
    els = shell('#stone', '#top', '#inner', '#ash', '#copper')
    els.append(face_cube((5.5, 2, 2), (10.5, 7, 7), '#ember' if lit else '#ash', '#eye_on' if lit else '#eye_off', 5))
    els.append(chalk('#chalk_on' if lit else '#chalk'))
    return els


def fornalha_h(lit):
    """Chama em cristal: um cristal de chama roxa flutua na camara com dois estilhacos orbitando; apagado, vira cinza."""
    c = '#flame' if lit else '#ash'
    els = shell('#stone', '#top', '#inner', '#ash', '#copper')
    els += gem(8, 6.5, 4, c)
    els.append(B((4, 7.5, 2), (5, 8.5, 3), c)); els.append(B((11, 3.5, 5), (12, 4.5, 6), c))
    return els


def condensador_i(lit):
    """Olho congelado: um olho de gelo preso na camara; trabalhando, abre e segue o jogador, e o circulo de giz acende."""
    els = shell('#stone', '#top', '#inner', '#snow', '#packed')
    els.append(face_cube((5, 2, 1.5), (11, 8, 7.5), '#celestial' if lit else '#packed', '#eye_on' if lit else '#eye_off', 6))
    for x, h in ((3.5, 2), (11.5, 1.5)):
        els.append(B((x, 11 - h, 1), (x + 1, 11, 2), '#packed'))
    els.append(chalk('#chalk'))
    return els


def condensador_j(lit):
    """Cristal de gelo: flutua e gira na camara, com estilhacos em volta; brilha azul quando trabalha."""
    c = '#celestial' if lit else '#packed'
    els = shell('#stone', '#top', '#inner', '#snow', '#packed')
    els += gem(8, 6.5, 4, c)
    els.append(B((4, 3.5, 2), (5, 4.5, 3), c)); els.append(B((11, 8, 5), (12, 9, 6), c))
    for x, h in ((3.5, 2), (11.5, 1.5)):
        els.append(B((x, 11 - h, 1), (x + 1, 11, 2), '#packed'))
    return els


MB = 'mistech:block/'
F2_TEX = {'stone': MB + 'runic_furnace_side', 'top': MB + 'runic_furnace_top', 'inner': MB + 'rune_anvil', 'ash': MB + 'ash_block',
          'copper': 'minecraft:block/cut_copper', 'ember': MB + 'incense_ember', 'flame': MB + 'magic_incense_ember',
          'chalk': MB + 'chalk_mark', 'chalk_on': MB + 'impure_chalk_mark', 'eye_on': MB + 'fc_ember_eye', 'eye_off': MB + 'fc_ember_eye_closed',
          'particle': MB + 'runic_furnace_side'}
C2_TEX = {'stone': MB + 'condenser_side', 'top': MB + 'condenser_top', 'inner': MB + 'star_stone', 'snow': 'minecraft:block/snow',
          'packed': 'minecraft:block/packed_ice', 'celestial': MB + 'celestial_block', 'chalk': MB + 'chalk_mark',
          'eye_on': MB + 'fc_ice_eye', 'eye_off': MB + 'fc_ice_eye_closed', 'particle': MB + 'condenser_side'}


def aplicar():
    """Escreve no mod: H na Fornalha Runica e J no Condensador. O cristal e desenhado pelo FloatingCrystalRenderer (gira);
    os modelos *_item levam o cristal parado, para o inventario."""
    md = 'src/main/resources/assets/mistech/models/block/'
    f_tex = {k: F2_TEX[k] for k in ('stone', 'top', 'inner', 'ash', 'copper', 'particle')}
    c_tex = {k: C2_TEX[k] for k in ('stone', 'top', 'inner', 'snow', 'packed', 'particle')}
    f_shell = shell('#stone', '#top', '#inner', '#ash', '#copper')
    c_shell = shell('#stone', '#top', '#inner', '#snow', '#packed')
    for x, h in ((3.5, 2), (11.5, 1.5)):
        c_shell.append(B((x, 11 - h, 1), (x + 1, 11, 2), '#packed'))

    def dump(name, tex, els):
        json.dump({'parent': 'minecraft:block/block', 'textures': tex, 'elements': els}, open(md + name + '.json', 'w'), indent=2)
    for suffix in ('', '_on'):
        dump('runic_furnace' + suffix, f_tex, f_shell)
        dump('condenser' + suffix, c_tex, c_shell)
    fi = dict(f_tex); fi['gem'] = MB + 'magic_incense_ember'
    ci = dict(c_tex); ci['gem'] = MB + 'celestial_block'
    extra = [B((4, 7.5, 2), (5, 8.5, 3), '#gem'), B((11, 3.5, 5), (12, 4.5, 6), '#gem')]
    dump('runic_furnace_item', fi, f_shell + gem(8, 6.5, 4, '#gem') + extra)
    dump('condenser_item', ci, c_shell + gem(8, 6.5, 4, '#gem') + extra)
    for n in ('runic_furnace', 'condenser'):
        json.dump({'model': {'type': 'minecraft:model', 'model': f'mistech:block/{n}_item'}},
                  open(f'src/main/resources/assets/mistech/items/{n}.json', 'w'), indent=2)


def small(name, rows, pal):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            im.putpixel((x, y), pal[ch] + (255,))
    im.save(f'{OUT}/{name}.png')


def rodada2():
    fp = {'o': (255, 140, 30), 'r': (214, 70, 30), 'y': (255, 224, 110), 'k': (60, 20, 10), 'g': (150, 146, 140), 'h': (176, 172, 166), 'd': (84, 80, 78)}
    small('fc_ember_eye', ['rooor', 'oykyo', 'oykyo', 'oykyo', 'rooor'], fp)
    small('fc_ember_eye_closed', ['ghggh', 'ggghg', 'gdddg', 'hgggg', 'gghgg'], fp)
    ip = {'b': (150, 200, 245), 'B': (110, 165, 230), 'w': (235, 248, 255), 'k': (18, 36, 90), 'd': (70, 110, 180)}
    small('fc_ice_eye', ['BbwwbB', 'bwkkwb', 'wwkkww', 'wwkkww', 'bwkkwb', 'BbwwbB'], ip)
    small('fc_ice_eye_closed', ['BbbbbB', 'bbBbbb', 'bddddb', 'bbbbbb', 'bbbBbb', 'BbbbbB'], ip)
    for name, fn, tex in (('fr_g', fornalha_g, F2_TEX), ('fr_h', fornalha_h, F2_TEX), ('cd_i', condensador_i, C2_TEX), ('cd_j', condensador_j, C2_TEX)):
        for lit in (False, True):
            json.dump({'parent': 'minecraft:block/block', 'textures': tex, 'elements': fn(lit)}, open(f'{M}/{name}{"_on" if lit else ""}.json', 'w'), indent=1)
    sheet(OUT + '/r2_fornalha.png', 'FORNALHA RUNICA - rodada 2 (cubo cheio, pedra runica do mod)', 'mistech:block/runic_furnace',
          [('fr_g', 'G - espirito de brasa (olho segue o jogador) + circulo de giz'), ('fr_h', 'H - cristal de chama flutuando')])
    sheet(OUT + '/r2_condensador.png', 'CONDENSADOR - rodada 2 (cubo cheio, pedra do mod)', 'mistech:block/condenser',
          [('cd_i', 'I - olho congelado (abre e segue o jogador) + circulo de giz'), ('cd_j', 'J - cristal de gelo flutuando')])


def tag_texture(name, bar, sym):
    pal = {'b': bar, 'p': (236, 226, 198), 'q': (228, 217, 188), 'k': (70, 58, 50)}
    pal.update(sym[1])
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(['bbb'] + sym[0]):
        for x, ch in enumerate(row):
            im.putpixel((x, y), pal[ch] + (255,))
    im.save(f'{OUT}/{name}.png')


def view(ref, yaw, sc=13):
    for r in ref.split(':')[-1:]:
        pass
    return bv.zview.zdraw(bv.quads(bv.load(ref)), yaw, -25, sc)


def sheet(path, title, current, versions):
    rows = [('ATUAL: desligado | ligado', [(current, -35), (current + '_on', -35)])]
    for name, label in versions:
        rows.append((label + ': desligado | ligado | de tras', [(f'mistech:block/{name}', -35), (f'mistech:block/{name}_on', -35), (f'mistech:block/{name}_on', 145)]))
    imgs = [(lab, [view(r, y) for r, y in vs]) for lab, vs in rows]
    cw = max(v.width for _, vs in imgs for v in vs); ch = max(v.height for _, vs in imgs for v in vs)
    S = Image.new('RGBA', (cw * 3 + 20, (ch + 22) * len(imgs) + 26), (52, 54, 62, 255)); d = ImageDraw.Draw(S)
    d.text((10, 6), title, fill=(255, 220, 120))
    for i, (lab, vs) in enumerate(imgs):
        y = 26 + i * (ch + 22)
        d.text((10, y), lab, fill=(255, 220, 120))
        for k, v in enumerate(vs):
            S.alpha_composite(v, (10 + k * cw + (cw - v.width) // 2, y + 16 + (ch - v.height) // 2))
    S.save(path)


if __name__ == '__main__':
    if 'aplicar' in sys.argv:
        aplicar(); print('aplicado'); sys.exit()
    if 'r2' in sys.argv:
        rodada2(); print('ok r2'); sys.exit()
    tag_texture('fc_tag_fire', (214, 90, 40), (['pfp', 'fyf', 'fyf', 'qfp'], {'f': (214, 70, 30), 'y': (255, 190, 60)}))
    tag_texture('fc_tag_ice', (120, 190, 230), (['ipi', 'pwp', 'iwi', 'pip'], {'i': (70, 140, 210), 'w': (220, 245, 255)}))
    for name, fn, tex in (('fr_a', fornalha_a, F_TEX), ('fr_b', fornalha_b, F_TEX), ('fr_c', fornalha_c, F_TEX),
                          ('cd_d', condensador_d, C_TEX), ('cd_e', condensador_e, C_TEX), ('cd_f', condensador_f, C_TEX)):
        for lit in (False, True):
            json.dump({'parent': 'minecraft:block/block', 'textures': tex, 'elements': fn(lit)}, open(f'{M}/{name}{"_on" if lit else ""}.json', 'w'), indent=1)
    sheet(OUT + '/versoes_fornalha.png', 'FORNALHA RUNICA (fornalha + cobre + cinza de incenso + runa de fogo)', 'mistech:block/runic_furnace',
          [('fr_a', 'A - forno com coifa e chamine de cobre'), ('fr_b', 'B - fogao de chapa (o item aparece na chapa)'), ('fr_c', 'C - gaiola com brasa flutuante')])
    sheet(OUT + '/versoes_condensador.png', 'CONDENSADOR (fornalha + 4 runas de gelo)', 'mistech:block/condenser',
          [('cd_d', 'D - forma: o bloco se forma dentro da boca'), ('cd_e', 'E - pedestal com cristal flutuante'), ('cd_f', 'F - torre de aletas com rotor')])
    print('ok')
