"""Texturas da Fase 8: Pergaminho de Padrão (o pergaminho de magia recolorido em azul de planta, com grade) e a frente da
Estante de Padrões (estante vanilla com os livros trocados por rolos azuis).
Uso (na pasta do projeto): python3 tools/autocraft/gen_textures.py"""
from PIL import Image

A = 'src/main/resources/assets/mistech/textures/'
VAN = 'vanilla/assets/minecraft/textures/block/'


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def ramp(cols, t):
    t = max(0.0, min(1.0, t)); f = t * (len(cols) - 1); i = min(int(f), len(cols) - 2); r = f - i
    return tuple(round(cols[i][k] * (1 - r) + cols[i + 1][k] * r) for k in range(3))


BLUE = [(14, 26, 70), (30, 60, 140), (70, 120, 210), (170, 205, 250)]
src = Image.open(A + 'item/spell_scroll.png').convert('RGBA')
ls = [lum(src.getpixel((x, y))) for y in range(16) for x in range(16) if src.getpixel((x, y))[3] > 0]
a, b = min(ls), max(ls)
out = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        p = src.getpixel((x, y))
        if p[3] == 0:
            continue
        c = ramp(BLUE, (lum(p) - a) / ((b - a) or 1))
        # linhas de grade claras por cima do papel
        if lum(p) > a + (b - a) * 0.55 and (x % 3 == 0 or y % 3 == 0):
            c = tuple(min(255, v + 45) for v in c)
        out.putpixel((x, y), c + (p[3],))
out.save(A + 'item/pattern_scroll.png')

shelf = Image.open(VAN + 'bookshelf.png').convert('RGBA').crop((0, 0, 16, 16))
SCROLL = [(20, 30, 70), (40, 70, 150), (90, 140, 220), (200, 225, 255)]
for y in list(range(1, 7)) + list(range(9, 15)):
    for x in range(1, 15):
        p = shelf.getpixel((x, y))
        t = lum(p) / 255.0
        c = ramp(SCROLL, t * 1.6)
        # ponta dourada dos rolos
        if y in (1, 9):
            c = (230, 190, 90) if x % 2 == 0 else c
        shelf.putpixel((x, y), c + (255,))
shelf.save(A + 'block/pattern_shelf_front.png')
print('ok')
