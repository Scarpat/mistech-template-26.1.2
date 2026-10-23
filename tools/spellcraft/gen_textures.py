#!/usr/bin/env python3
"""Texturas do sistema de magias (16x16, estilo vanilla): Grimório novo, Tomo do Aprendiz, pergaminho + selo, mesa de inscrição.
Uso: python3 tools/spellcraft/gen_textures.py   (escreve em src/main/resources/assets/mistech/textures/)"""
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'mistech', 'textures')


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def canvas():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def rect(im, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), c)


def px(im, pts, c):
    for x, y in pts:
        im.putpixel((x, y), c)


def tome(leather, gold, gem):
    """Livro de capa ornamentada (estilo tomo): lombada com cravos, cantoneiras e moldura douradas, joia central, páginas e marcador."""
    K = hexc('#140a1e')
    L0, L1, L2 = [hexc(c) for c in leather]  # escuro, médio, claro
    G0, G1, G2 = [hexc(c) for c in gold]     # sombra, base, brilho
    J0, J1, J2 = [hexc(c) for c in gem]
    P0, P1 = hexc('#cdbb8e'), hexc('#f4ead0')
    R0, R1 = hexc('#6e1634'), hexc('#b8305c')
    im = canvas()
    rect(im, 1, 1, 14, 14, K)               # contorno
    rect(im, 2, 2, 12, 13, L1)              # capa
    rect(im, 2, 2, 4, 13, L0)               # lombada
    px(im, [(4, y) for y in range(2, 14)], hexc('#000000', 70))
    for y in (3, 7, 12):                     # cravos dourados na lombada
        px(im, [(2, y), (3, y)], G1)
        im.putpixel((2, y), G2)
    rect(im, 13, 2, 13, 13, P1)             # páginas
    px(im, [(13, y) for y in range(3, 14, 2)], P0)
    px(im, [(12, y) for y in range(2, 14)], L0)  # sombra da borda da capa
    # moldura fina entre as cantoneiras
    px(im, [(x, 3) for x in range(7, 11)], G0)
    px(im, [(x, 12) for x in range(7, 11)], G0)
    px(im, [(5, y) for y in range(6, 10)], G0)
    px(im, [(11, y) for y in range(6, 10)], G0)
    # cantoneiras em L
    for (x, y, dx, dy) in [(5, 3, 1, 1), (11, 3, -1, 1), (5, 12, 1, -1), (11, 12, -1, -1)]:
        px(im, [(x + dx * i, y) for i in range(3)], G1)
        px(im, [(x, y + dy * j) for j in range(1, 3)], G1)
        im.putpixel((x, y), G2)
    # emblema: losango dourado com joia
    px(im, [(8, 5), (9, 5), (7, 6), (10, 6), (7, 9), (10, 9), (8, 10), (9, 10), (6, 7), (6, 8), (11, 7), (11, 8)], G1)
    px(im, [(8, 6), (9, 6), (7, 7), (10, 7), (7, 8), (10, 8), (8, 9), (9, 9)], G0)
    rect(im, 8, 7, 9, 8, J1)
    im.putpixel((8, 7), J2)
    im.putpixel((9, 8), J0)
    # brilho de couro
    px(im, [(6, 4), (6, 5), (7, 4)], L2)
    # marcador de página
    rect(im, 10, 14, 11, 15, R1)
    px(im, [(10, 15), (11, 15)], R0)
    return im


def grimoire():
    return tome(['#2b1245', '#4a2378', '#7a45b0'], ['#9a6a1c', '#e0ac3c', '#fff0a0'], ['#6a1fb0', '#b85cf0', '#f3d0ff'])


def forbidden():
    return tome(['#12060a', '#3a0c14', '#6a1624'], ['#4a4a4a', '#8a8a92', '#d0d0d8'], ['#3a0a4a', '#a01868', '#ff58a8'])


def apprentice():
    # capa verde-azulada, ferragens de bronze, joia ciano: mais simples que o Grimório
    return tome(['#0f3a44', '#1c6670', '#3fa0a4'], ['#6a4a22', '#b98544', '#e8c48a'], ['#0b6f8a', '#3fd6e6', '#d8fbff'])


def adept():
    return tome(['#3a1c0c', '#6a3a1c', '#a8683a'], ['#8a8a96', '#d0d4dc', '#ffffff'], ['#a8741a', '#f0b43c', '#fff2b8'])


def archmage():
    return tome(['#14102e', '#2a2468', '#5a4ab8'], ['#9a6a1c', '#f0c04a', '#fff6b0'], ['#7a1fb0', '#e070ff', '#ffe6ff'])


def flame():
    return tome(['#3a0c08', '#7a1c10', '#c04420'], ['#6a3a10', '#d88a2a', '#ffd880'], ['#c02808', '#ff7a1c', '#fff0a0'])


def wand(gem=None):
    im = canvas()
    wood = [hexc('#4a2f18'), hexc('#7a5230'), hexc('#a8794a')]
    for i in range(9):
        x, y = 3 + i, 12 - i
        px(im, [(x, y)], wood[1])
        px(im, [(x, y + 1)], wood[0])
        px(im, [(x + 1, y)], wood[2]) if i % 2 == 0 else None
    px(im, [(2, 13), (3, 13), (2, 12)], hexc('#b98544'))
    g = gem or ('#0b6f8a', '#3fd6e6', '#d8fbff')
    for pt, c in [((12, 3), g[1]), ((13, 2), g[2]), ((12, 2), g[0]), ((13, 3), g[1]), ((13, 4), g[0]), ((11, 3), g[0])]:
        px(im, [pt], hexc(c))
    return im


def staff(wood, metal, gem):
    """Cajado: haste diagonal longa com cabeça em garra segurando uma gema."""
    im = canvas()
    w = [hexc(c) for c in wood]
    for i in range(11):
        x, y = 2 + i, 14 - i
        px(im, [(x, y)], w[1])
        px(im, [(x, y + 1)], w[0])
        if i % 2 == 0:
            px(im, [(x + 1, y)], w[2])
    m = [hexc(c) for c in metal]
    px(im, [(1, 15), (2, 15), (1, 14)], m[0])
    px(im, [(7, 8), (8, 7)], m[1])  # anel de metal
    px(im, [(8, 8)], m[0])
    # garra
    px(im, [(12, 4), (13, 3), (11, 3), (13, 5), (14, 4), (12, 5)], m[1])
    px(im, [(11, 4), (14, 3)], m[0])
    g = [hexc(c) for c in gem]
    px(im, [(12, 3), (13, 4), (12, 4)], g[1])
    px(im, [(13, 2)], g[2])
    px(im, [(12, 2), (11, 5)], g[0])
    return im


def wooden_staff():
    return staff(['#3a2412', '#6a4626', '#9a6c40'], ['#5a4a3a', '#8a7a64'], ['#4a1a8a', '#a860e8', '#e8c8ff'])


def runic_staff():
    return staff(['#2a1c34', '#503a6a', '#7a5a9a'], ['#4a3a14', '#c89a2a'], ['#0b6f8a', '#3fd6e6', '#d8fbff'])


def mithril_staff():
    return staff(['#26323e', '#4a6478', '#7a9cb4'], ['#3a5a74', '#9ad0f0'], ['#1a4aa8', '#58a8ff', '#e0f0ff'])


def archmage_staff():
    return staff(['#2a0a3a', '#6a1a8a', '#b04ad0'], ['#7a5a10', '#ffd24a'], ['#a81860', '#ff58b8', '#fff0f8'])


def charger_side():
    im = canvas()
    base = [hexc(c) for c in ('#2a1c3a', '#3c2a52', '#4e3a6a')]
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), base[(x * 7 + y * 13) % 3 if (x + y) % 5 == 0 else 1])
    for x in range(16):
        im.putpixel((x, 0), base[0]); im.putpixel((x, 15), base[0])
    for y in range(16):
        im.putpixel((0, y), base[0]); im.putpixel((15, y), base[0])
    for pt in [(7, 7), (8, 7), (7, 8), (8, 8), (7, 6), (8, 9)]:
        im.putpixel(pt, hexc('#a860e8'))
    return im


def charger_top(on):
    im = charger_side()
    c = ['#3fd6e6', '#d8fbff'] if on else ['#5a4a7a', '#7a6a9a']
    for x in range(3, 13):
        for y in range(3, 13):
            if x in (3, 12) or y in (3, 12):
                im.putpixel((x, y), hexc(c[0]))
    for x in range(6, 10):
        for y in range(6, 10):
            im.putpixel((x, y), hexc(c[1]) if on else hexc('#2a1c3a'))
    return im


def charger_gold():
    im = canvas()
    cols = ['#6a4a10', '#c89a2a', '#ffe08a']
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), hexc(cols[1]))
    for x in range(16):
        im.putpixel((x, 0), hexc(cols[2])); im.putpixel((x, 15), hexc(cols[0]))
    for y in range(16):
        im.putpixel((0, y), hexc(cols[2])); im.putpixel((15, y), hexc(cols[0]))
    return im


def charger_glow(on):
    im = canvas()
    a, b, c = ('#1a8aa6', '#3fd6e6', '#e8ffff') if on else ('#2a2440', '#3a3258', '#4a4270')
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            im.putpixel((x, y), hexc(c if d < 2.5 else b if d < 5.5 else a))
    return im


ELEMENT_COLORS = {'ice': '9FE3FF', 'water': '3F8CFF', 'earth': '8A6A3A', 'air': 'DDF4FF', 'energy': 'FFE14A',
                  'light': 'FFF6C0', 'darkness': '5A2E78', 'time': '5FD6B0', 'space': '8A4FD8', 'life': '5CD65C'}


def shade(hexcolor, f):
    r, g, b = int(hexcolor[0:2], 16), int(hexcolor[2:4], 16), int(hexcolor[4:6], 16)
    r, g, b = [max(0, min(255, int(v * f))) for v in (r, g, b)]
    return '#%02x%02x%02x' % (r, g, b)


def element_tome(el):
    c = ELEMENT_COLORS[el]
    return tome([shade(c, 0.28), shade(c, 0.5), shade(c, 0.8)], ['#9a6a1c', '#e0ac3c', '#fff0a0'], [shade(c, 0.6), shade(c, 1.0), shade(c, 1.0 + 0.0) if False else '#ffffff'])


def mat_wand(gem):
    return wand(gem)


def void_wand():
    return wand(('#1a0a2a', '#7a2ad0', '#f0d0ff'))


def aether_crown():
    im = canvas()
    G = [hexc(c) for c in ('#7a5a10', '#e8b830', '#fff2a0')]
    B = [hexc(c) for c in ('#1a4aa8', '#58a8ff', '#e8f4ff')]
    rect(im, 2, 9, 13, 12, G[1])
    rect(im, 2, 12, 13, 12, G[0])
    rect(im, 2, 9, 13, 9, G[2])
    for x in (2, 5, 8, 11, 13):
        px(im, [(x, 8), (x, 7)], G[1])
        px(im, [(x, 6)], G[2])
    px(im, [(8, 5), (8, 4)], G[2])
    rect(im, 7, 10, 8, 11, B[1])
    px(im, [(7, 10)], B[2])
    px(im, [(8, 11)], B[0])
    for x in (3, 12):
        px(im, [(x, 10)], B[1])
    return im


def rune_altar():
    im = canvas()
    base = [hexc(c) for c in ('#4a4a54', '#62626e', '#7a7a88')]
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), base[1] if (x + y * 3) % 7 else base[2])
    for i in range(16):
        im.putpixel((i, 0), base[0]); im.putpixel((i, 15), base[0]); im.putpixel((0, i), base[0]); im.putpixel((15, i), base[0])
    c = hexc('#b98cff'); d = hexc('#e8d0ff')
    for pt in [(7, 3), (8, 3), (7, 4), (8, 4), (5, 6), (6, 7), (7, 8), (8, 8), (9, 7), (10, 6), (7, 9), (8, 9), (7, 10), (8, 10), (6, 12), (9, 12)]:
        im.putpixel(pt, c)
    for pt in [(7, 3), (8, 8)]:
        im.putpixel(pt, d)
    return im


def scroll():
    """Rolo de pergaminho na diagonal (mais grosso), com fita; o selo (camada 2) é separado, em tons de cinza, para receber a cor do elemento."""
    K = hexc('#2a1a0c')
    P0, P1, P2 = hexc('#b79f6e'), hexc('#e6d4a4'), hexc('#fbf0cc')
    R0, R1 = hexc('#7a2a2a'), hexc('#b84a4a')
    im = canvas()
    shade = {13: P2, 14: P1, 15: P1, 16: P1, 17: P0}
    for y in range(16):
        for x in range(16):
            if (x + y) in shade and abs(x - y) <= 10:
                im.putpixel((x, y), shade[x + y])
    base = im.copy()
    for y in range(16):
        for x in range(16):
            if base.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and base.getpixel((nx, ny))[3]:
                        im.putpixel((x, y), K)
                        break
    # pontas enroladas: miolo claro com anel escuro
    for (cx, cy) in ((3, 12), (12, 3)):
        for (dx, dy) in ((0, 0), (-1, -1), (1, 1), (-1, 0), (0, -1), (1, 0), (0, 1)):
            x, y = cx + dx, cy + dy
            if 0 <= x < 16 and 0 <= y < 16 and im.getpixel((x, y))[3]:
                im.putpixel((x, y), P0)
        im.putpixel((cx, cy), P2)
    # fita vermelha amarrando o meio
    px(im, [(7, 8), (8, 7), (6, 7), (9, 8), (7, 9), (8, 6)], R1)
    px(im, [(6, 8), (9, 7)], R0)
    return im


def seal():
    """Selo de cera em tons de cinza (multiplicado pela cor do elemento pelo item), sobre a fita do rolo."""
    im = canvas()
    W2, W1, W0, D = hexc('#ffffff'), hexc('#e2e2e2'), hexc('#b0b0b0'), hexc('#767676')
    rect(im, 6, 6, 9, 9, W1)
    px(im, [(6, 6), (9, 6), (6, 9), (9, 9)], (0, 0, 0, 0))
    px(im, [(7, 6), (6, 7), (7, 7)], W2)
    px(im, [(9, 7), (9, 8), (7, 9), (8, 9)], W0)
    px(im, [(8, 8)], D)
    return im


def inscription_cloth():
    im = canvas()
    c0, c1, c2 = hexc('#0f3a3c'), hexc('#16525a'), hexc('#1e6c74')
    gold = hexc('#d8b04a')
    rect(im, 0, 0, 15, 15, c1)
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0:
                im.putpixel((x, y), c2)
            elif (x * 3 + y) % 7 == 0:
                im.putpixel((x, y), c0)
    # bordado dourado
    for i in range(1, 15):
        for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
            im.putpixel((x, y), gold if i % 2 else hexc('#a07a2a'))
    return im


def inscription_paper():
    im = canvas()
    P0, P1, P2 = hexc('#cdb98a'), hexc('#ecdcae'), hexc('#fbf1cf')
    ink, inkd = hexc('#17636b'), hexc('#0c3a40')
    rect(im, 0, 0, 15, 15, P1)
    for x in range(16):
        im.putpixel((x, 0), P0)
        im.putpixel((x, 15), P0)
    for y in range(16):
        im.putpixel((0, y), P0)
        im.putpixel((15, y), P0)
    px(im, [(1, 1), (2, 1), (1, 2)], P2)
    # linhas de runas
    for row, y in enumerate((3, 6, 9, 12)):
        x = 2
        while x < 14:
            w = 2 if (x + row) % 3 else 1
            for k in range(w):
                if x + k < 14:
                    im.putpixel((x + k, y), ink)
                    if (x + k + row) % 2 == 0:
                        im.putpixel((x + k, y - 1), inkd)
            x += w + 1
    # selo no canto
    px(im, [(12, 12), (13, 12), (12, 13), (13, 13)], hexc('#b02a3a'))
    return im


def save(im, rel):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)
    print('ok', rel)


if __name__ == '__main__':
    save(grimoire(), 'item/guide_book.png')
    save(apprentice(), 'item/apprentice_tome.png')
    save(forbidden(), 'item/forbidden_tome.png')
    save(adept(), 'item/adept_tome.png')
    save(archmage(), 'item/archmage_tome.png')
    save(flame(), 'item/flame_tome.png')
    save(wand(), 'item/apprentice_wand.png')
    save(wooden_staff(), 'item/wooden_staff.png')
    save(runic_staff(), 'item/runic_staff.png')
    save(mithril_staff(), 'item/mithril_staff.png')
    save(archmage_staff(), 'item/archmage_staff.png')
    save(charger_side(), 'block/arcane_charger_side.png')
    save(charger_top(False), 'block/arcane_charger_top.png')
    save(charger_top(True), 'block/arcane_charger_top_on.png')
    save(charger_gold(), 'block/arcane_charger_gold.png')
    save(charger_glow(True), 'block/arcane_charger_glow_on.png')
    save(charger_glow(False), 'block/arcane_charger_glow_off.png')
    for el in ELEMENT_COLORS:
        save(element_tome(el), 'item/%s_tome.png' % el)
    save(mat_wand(('#7a3a14', '#d8803a', '#ffd0a0')), 'item/copper_wand.png')
    save(mat_wand(('#8a6a10', '#f0c030', '#fff4a0')), 'item/gold_wand.png')
    save(mat_wand(('#1a4aa8', '#58a8ff', '#e0f0ff')), 'item/mithril_wand.png')
    save(void_wand(), 'item/void_wand.png')
    save(aether_crown(), 'item/aether_crown.png')
    save(rune_altar(), 'block/rune_altar.png')
    save(scroll(), 'item/spell_scroll.png')
    save(seal(), 'item/spell_scroll_seal.png')
    save(inscription_cloth(), 'block/inscription_cloth.png')
    save(inscription_paper(), 'block/inscription_paper.png')
