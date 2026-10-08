# Gera textures/gui/guide_map.png (molduras dos nós do mapa do Grimório).
from PIL import Image
import math, sys

W, H = 256, 128
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
px = img.load()


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def lerp(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(4))


STATES = {
    'avail': (hexc('d9a441'), hexc('ffe08a'), hexc('3a2c52'), hexc('1c1528')),
    'done': (hexc('4fae5a'), hexc('a8f0a0'), hexc('24452c'), hexc('13241a')),
    'locked': (hexc('4a4552'), hexc('6a6472'), hexc('1f1c24'), hexc('141218')),
}
OUT = hexc('0b0810')


def dist(kind, x, y, c):
    dx, dy = x + 0.5 - c, y + 0.5 - c
    if kind == 'circle':
        return math.hypot(dx, dy)
    ax, ay = abs(dx), abs(dy)
    return max(ax, ay, (ax + ay) / 1.38)  # octógono


def node(ox, oy, size, kind, st):
    ring, hi, fin, fout = STATES[st]
    c = size / 2
    R = c - 0.2
    for y in range(size):
        for x in range(size):
            d = dist(kind, x, y, c)
            if d > R:
                continue
            if d > R - 1:
                col = OUT
            elif d > R - 3:
                t = max(0, min(1, ((c - x) + (c - y)) / size * 1.2 + 0.35))
                col = lerp(ring, hi, t * 0.8)
            elif d > R - 4:
                col = OUT
            else:
                col = lerp(fin, fout, d / (R - 4))
            px[ox + x, oy + y] = col
    if kind == 'oct':
        for y in range(size):
            for x in range(size):
                d = dist(kind, x, y, c)
                if R - 7 < d <= R - 6:
                    px[ox + x, oy + y] = lerp(ring, (0, 0, 0, 255), 0.45)


def hover(ox, oy, size, kind):
    c = size / 2
    R = c - 0.2
    for y in range(size):
        for x in range(size):
            if R - 3 < dist(kind, x, y, c) <= R - 1:
                px[ox + x, oy + y] = (255, 255, 255, 200)


# círculo 26x26 em y=0: disponível, concluída, bloqueada, destaque
for i, st in enumerate(['avail', 'done', 'locked']):
    node(i * 26, 0, 26, 'circle', st)
hover(78, 0, 26, 'circle')
# octógono 34x34 em y=26
for i, st in enumerate(['avail', 'done', 'locked']):
    node(i * 34, 26, 34, 'oct', st)
hover(102, 26, 34, 'oct')

# ícones em y=64
# check 11x9 em (0,64)
check = ["........#", ".......##", "......##.", "#....##..", "##..##...", ".####....", "..##....."]
for y, row in enumerate(check):
    for x, ch in enumerate(row):
        if ch == '#':
            px[x + 1, 64 + y + 1] = hexc('7dff7a')
for (x, y) in [(x, y) for y in range(9) for x in range(11) if px[x, 64 + y][3] > 0 and px[x, 64 + y] != OUT]:
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        X, Y = x + dx, 64 + y + dy
        if 0 <= X < 11 and 64 <= Y < 73 and px[X, Y][3] == 0:
            px[X, Y] = OUT
# exclamação 5x10 em (12,64)
ex = [".###.", "#...#", "#.#.#", "#.#.#", "#.#.#", "#...#", "#.#.#", "#...#", ".###."]
for y, row in enumerate(ex):
    for x, ch in enumerate(row):
        if ch == '#':
            px[12 + x, 64 + y] = OUT
        elif 0 < x < 4 and 0 < y < 8:
            px[12 + x, 64 + y] = hexc('ffcf4a') if ch == '.' else hexc('3a2a00')
# o "." da linha do meio vira o corpo da exclamação: invertemos as cores
for y in range(1, 8):
    for x in range(1, 4):
        p = px[12 + x, 64 + y]
        px[12 + x, 64 + y] = hexc('2a1a00') if (x == 2 and y in (2, 3, 4, 6)) else hexc('ffcf4a')
# cadeado 9x10 em (18,64)
lock = ["..###..", ".#...#.", ".#...#.", "#######", "#######", "###.###", "###.###", "#######"]
for y, row in enumerate(lock):
    for x, ch in enumerate(row):
        if ch == '#':
            px[19 + x, 65 + y] = hexc('b0a9bc') if y < 3 else hexc('8a8396')
for (x, y) in [(x, y) for y in range(64, 75) for x in range(18, 28) if px[x, y][3] > 0 and px[x, y] != OUT]:
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        X, Y = x + dx, y + dy
        if 18 <= X < 28 and 64 <= Y < 75 and px[X, Y][3] == 0:
            px[X, Y] = OUT
# losango 5x5 em (28,64) (ornamento)
for y in range(5):
    for x in range(5):
        if abs(x - 2) + abs(y - 2) <= 2:
            px[28 + x, 64 + y] = (255, 255, 255, 255)

img.save(sys.argv[1] if len(sys.argv) > 1 else 'guide_map.png')
img.resize((W * 4, H * 4), Image.NEAREST).save('/tmp/preview.png')
