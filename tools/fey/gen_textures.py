"""Texturas do Incenso de Fada, Coroa Purificada, Runa da Vida, Véu Feérico e duende invocado "limpo".
Rodar na raiz do projeto: python tools/fey/gen_textures.py  (só usa texturas do próprio mod)."""
import colorsys
from PIL import Image

T = 'src/main/resources/assets/mistech/textures/'


def hsv(c):
    return colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)


def rgb(h, s, v, a=255):
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, max(0, min(1, s)), max(0, min(1, v)))
    return (round(r * 255), round(g * 255), round(b * 255), a)


def recolor(src, dst, fn):
    im = Image.open(T + src).convert('RGBA')
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            c = px[x, y]
            if c[3]:
                px[x, y] = fn(c, x, y)
    im.save(T + dst)
    return im


# ---------- Incenso de Fada: a parte verde (folha de arruda) vira rosa-lilás do Pó de Fada ----------
def fairy(c, x, y):
    h, s, v = hsv(c)
    if 0.2 < h < 0.5 and s > 0.15:  # verde
        return rgb(0.86, 0.35 + s * 0.3, min(1.0, v * 1.25 + 0.1), c[3])
    return c


for name in ('rue_incense', 'rue_incense_lit'):
    im = recolor('item/%s.png' % name, 'item/%s.png' % name.replace('rue', 'fairy'), fairy)
    px = im.load()
    # brilhinhos
    for (x, y) in ((11, 2), (8, 6)):
        if px[x, y][3] == 0:
            px[x, y] = (255, 240, 255, 255)
    im.save(T + 'item/%s.png' % name.replace('rue', 'fairy'))


# ---------- Coroa Purificada: sem os pingos roxos, gemas azul-claro, ouro mais claro ----------
def pure_crown(c, x, y):
    h, s, v = hsv(c)
    if 0.7 < h < 0.95 and s > 0.2:  # pingos roxos
        return (0, 0, 0, 0)
    if (h > 0.95 or h < 0.03) and s > 0.5:  # gemas vermelhas
        return rgb(0.52, 0.45, min(1.0, v + 0.45))
    if 0.08 < h < 0.18:  # ouro
        return rgb(h + 0.01, s * 0.75, min(1.0, v * 1.12 + 0.05), c[3])
    return c


recolor('item/corrupted_crown.png', 'item/purified_crown.png', pure_crown)


# ---------- Runa da Vida: pergaminho da Runa do Tempo, faixa Água | Terra, símbolo de broto em coração ----------
im = Image.open(T + 'item/time_rune.png').convert('RGBA')
px = im.load()
WATER, EARTH, BOTTOM = (0x3F, 0x8C, 0xFF, 255), (0x6E, 0x8C, 0x3C, 255), (0x2E, 0x6B, 0x2E, 255)
for x in range(3, 13):
    px[x, 1] = WATER if x < 8 else EARTH
    px[x, 14] = BOTTOM
# limpa o símbolo (deixa só o pergaminho)
paper = [(0xF2, 0xE9, 0xD0, 255), (0xEC, 0xE2, 0xC6, 255), (0xE4, 0xD9, 0xBC, 255)]
for y in range(3, 13):
    for x in range(3, 13):
        h, s, v = hsv(px[x, y])
        if v < 0.75 or s > 0.3:
            px[x, y] = paper[(x * 7 + y * 3) % 3]
L, G, D, S, M = (0x7F, 0xD8, 0x6A, 255), (0x3C, 0x9E, 0x3C, 255), (0x24, 0x6E, 0x2A, 255), (0x5A, 0x7A, 0x2C, 255), (0x78, 0x5A, 0x32, 255)
sym = [
    '..........',
    '...LL.LL..',
    '..LGGLGGD.',
    '..LGGGGGD.',
    '...LGGGD..',
    '....LGD...',
    '.....S....',
    '....SS....',
    '...MMMM...',
    '..........',
]
key = {'L': L, 'G': G, 'D': D, 'S': S, 'M': M}
for j, row in enumerate(sym):
    for i, ch in enumerate(row):
        if ch in key:
            px[3 + i, 3 + j] = key[ch]
im.save(T + 'item/life_rune.png')


# ---------- Véu Feérico (ícone do efeito): o Véu de Incenso em rosa ----------
def fey_icon(c, x, y):
    h, s, v = hsv(c)
    if s > 0.1 and (0.6 < h < 0.9):
        return rgb(0.88, s * 0.9, min(1.0, v * 1.1), c[3])
    if s < 0.15:
        return rgb(0.85, 0.15, v, c[3])
    return c


im = recolor('mob_effect/incense_veil.png', 'mob_effect/fey_veil.png', fey_icon)
px = im.load()
for (x, y) in ((3, 4), (14, 9), (5, 14)):
    px[x, y] = (255, 236, 255, 255)
im.save(T + 'mob_effect/fey_veil.png')


# ---------- Duende "limpo" (Coroa Purificada): pele dourada-clara, capuz creme, veias de ouro ----------
def pure_goblin(c, x, y):
    h, s, v = hsv(c)
    if 0.17 < h < 0.45 and s > 0.15:  # pele verde
        return rgb(0.115, s * 0.85, min(1.0, v * 1.3 + 0.08), c[3])
    if 0.7 < h < 0.95 and s > 0.15 and v > 0.35:  # veias/olhos roxos
        return rgb(0.13, 0.7, min(1.0, v * 1.4 + 0.15), c[3])
    if 0.02 < h < 0.12 and s > 0.2 and v < 0.5:  # couro marrom do capuz/roupa
        return rgb(0.12, s * 0.25, min(1.0, v * 1.9 + 0.2), c[3])
    return c


recolor('entity/corrupted_goblin.png', 'entity/corrupted_goblin_pure.png', pure_goblin)
print('ok')
