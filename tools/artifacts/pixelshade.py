# Sombreamento automático de ícones 16×16 (estilo "almofadado": contorno colorido, luz de cima-esquerda, brilho).
# Uma silhueta = linhas de letras (uma letra por material). Um overlay opcional força tons:
#   o = contorno, d = escuro, m = médio, l = claro, h = brilho, * = branco puro, x = apaga o pixel.
from PIL import Image


def hexc(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def ramp(*hs):
    """contorno, escuro, médio, claro, brilho"""
    return [hexc(h) for h in hs]


TONE = {'o': 0, 'd': 1, 'm': 2, 'l': 3, 'h': 4}


def shade(rows, mats, over=None, size=16, flat=()):
    assert len(rows) == size and all(len(r) == size for r in rows), [len(r) for r in rows]
    g = [list(r) for r in rows]

    def at(x, y):
        return g[y][x] if 0 <= x < size and 0 <= y < size else '.'

    def run(x, y, dx, dy, m):
        n = 0
        while at(x, y) == m:
            n += 1
            x += dx
            y += dy
        return n

    im = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            m = g[y][x]
            if m == '.' or m == ' ':
                continue
            if m == '*':
                im.putpixel((x, y), (255, 255, 255, 255))
                continue
            r = mats[m]
            empty = [at(x + dx, y + dy) in '. ' for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if any(empty):
                t = 0
            elif m in flat:
                t = 2
            else:
                up, left = run(x, y, 0, -1, m), run(x, y, -1, 0, m)
                down, right = run(x, y, 0, 1, m), run(x, y, 1, 0, m)
                dul, ddr = min(up, left), min(down, right)
                if up == 2 and left == 2:
                    t = 4
                elif dul == 2 and ddr == 2:
                    t = 2
                elif dul <= 2:
                    t = 3
                elif ddr <= 2:
                    t = 1
                else:
                    t = 2
                # borda interna com outro material embaixo/à direita: escurece
                if at(x + 1, y) not in (m, '.', ' ') or at(x, y + 1) not in (m, '.', ' '):
                    t = min(t, 1)
            im.putpixel((x, y), r[t])
    if over:
        for y, row in enumerate(over):
            for x, ch in enumerate(row):
                m = g[y][x] if y < size and x < size else '.'
                if ch == '*':
                    im.putpixel((x, y), (255, 255, 255, 255))
                elif ch == 'x':
                    im.putpixel((x, y), (0, 0, 0, 0))
                elif ch in TONE and m in mats:
                    im.putpixel((x, y), mats[m][TONE[ch]])
    return im
