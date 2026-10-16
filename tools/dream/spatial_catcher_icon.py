"""Ícone do Apanha-Sonho Espacial: parte do Apanha-Sonho Lúcido, aro em índigo, teia ciano, centro de vazio estrelado
com uma pérola verde-água (cores da runa do espaço). Uso: python tools/dream/spatial_catcher_icon.py"""
import colorsys
from PIL import Image

T = 'src/main/resources/assets/mistech/textures/item/'
src = Image.open(T + 'lucid_dreamcatcher.png').convert('RGBA')
out = src.copy()
sp, op = src.load(), out.load()
W, H = src.size


def hue(c):
    h, l, s = colorsys.rgb_to_hls(c[0] / 255, c[1] / 255, c[2] / 255)
    return h * 360, l, s


def tone(base, l):
    h, _, s = colorsys.rgb_to_hls(*[x / 255 for x in base])
    r, g, b = colorsys.hls_to_rgb(h, max(0.0, min(1.0, l)), s)
    return (int(r * 255), int(g * 255), int(b * 255), 255)


RING = (0x8a, 0x4f, 0xd8)   # violeta da runa do espaço
WEB = (0x7f, 0xe8, 0xe0)    # ciano
PEARL = (0x2f, 0xc0, 0x9a)  # verde-água
VOID = (0x1c, 0x12, 0x38, 255)
for y in range(H):
    for x in range(W):
        c = sp[x, y]
        if c[3] == 0:
            continue
        h, l, s = hue(c)
        if s < 0.15:
            continue
        if 250 <= h <= 300:      # aro roxo
            op[x, y] = tone(RING, min(0.72, l * 1.15))
        elif 30 <= h <= 65:      # teia / penas amarelas
            op[x, y] = tone(WEB, l)
        elif h > 300 or h < 20:  # centro e contas magenta
            op[x, y] = tone(PEARL, l)
# miolo: vazio estrelado nos buracos transparentes dentro do aro (linhas 2..9)
for y in range(2, 10):
    xs = [x for x in range(W) if sp[x, y][3] and 250 <= hue(sp[x, y])[0] <= 300]
    if len(xs) >= 2:
        for x in range(min(xs) + 1, max(xs)):
            if sp[x, y][3] == 0:
                op[x, y] = VOID
for (x, y) in ((5, 4), (10, 6), (6, 8)):
    if op[x, y] == VOID:
        op[x, y] = (0xf4, 0xf0, 0xff, 255)
out.save(T + 'spatial_dreamcatcher.png')
out.resize((160, 160), Image.NEAREST).save('build/dream/spatial_dreamcatcher_big.png')
