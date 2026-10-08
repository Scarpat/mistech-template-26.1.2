"""Separa uma textura de tela do Mistech (estilo místico antigo) em camadas: fundo, moldura, espaços e enfeites.
Classes: 0 fundo (substituído), 1 espaço-miolo, 2 espaço-borda escura, 3 espaço-borda clara, 4 enfeite violeta (recolorido),
5 cor própria (mantida), 6 fora do painel (mantido)."""
from PIL import Image
import colorsys
SLOT_FILL = (24, 18, 34)
SLOT_DARK = (11, 8, 17)
SLOT_LIGHT = (66, 52, 92)
FRAME_BAND = 6

def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]

def hsv(c):
    return colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)

def is_violet(c):
    h, s, v = hsv(c)
    return 0.62 <= h <= 0.92 or s < 0.18

def is_goldish(c):
    return c[0] > c[2] + 30 and c[0] >= c[1] >= c[2] - 5

def classify(im, W, H):
    im = im.convert('RGBA')
    w, h = im.size
    cls = [[6] * w for _ in range(h)]
    px = im.load()
    for y in range(H):
        for x in range(W):
            c = px[x, y][:3]
            if px[x, y][3] == 0:
                cls[y][x] = 6
                continue
            edge = min(x, y, W - 1 - x, H - 1 - y)
            if c == SLOT_FILL:
                cls[y][x] = 1
            elif c == SLOT_DARK and edge >= FRAME_BAND - 2:
                cls[y][x] = 2
            elif c == SLOT_LIGHT and edge >= FRAME_BAND - 2:
                cls[y][x] = 3
            elif edge < FRAME_BAND:
                cls[y][x] = 0
            else:
                cls[y][x] = -1  # decidir depois
    # enfeites: claros e não isolados
    def bright(x, y, t):
        return 0 <= x < W and 0 <= y < H and lum(px[x, y][:3]) >= t
    for y in range(H):
        for x in range(W):
            if cls[y][x] != -1:
                continue
            c = px[x, y][:3]
            l = lum(c)
            hh, ss, vv = hsv(c)
            near = sum(1 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if bright(x + dx, y + dy, 52))
            if not is_violet(c) and not is_goldish(c) and ss > 0.35 and vv > 0.35:
                cls[y][x] = 5
            elif is_goldish(c) and min(x, y, W - 1 - x, H - 1 - y) < 10:
                cls[y][x] = 0
            elif l >= 60 and near >= 1:
                cls[y][x] = 4
            else:
                cls[y][x] = 0
    return cls
