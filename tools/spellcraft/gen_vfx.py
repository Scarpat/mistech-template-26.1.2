#!/usr/bin/env python3
"""Texturas e JSON dos efeitos das magias: partículas próprias (brancas, tingidas no jogo pela cor do elemento) e os
itens de exibição (círculo mágico, anel de onda, esfera de luz, relógio). Uso: python3 tools/spellcraft/gen_vfx.py"""
import json, math, os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'mistech')
W = (255, 255, 255, 255)


def save(im, rel):
    path = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)


def soft_dot(n, core=0.35):
    im = Image.new('RGBA', (n, n))
    c = (n - 1) / 2
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - c, y - c) / (n / 2)
            a = 255 if d < core else int(255 * max(0.0, 1 - (d - core) / (1 - core)) ** 1.6)
            im.putpixel((x, y), (255, 255, 255, a))
    return im


def star(n):
    im = Image.new('RGBA', (n, n))
    c = n // 2
    for i in range(n):
        a = max(0, 255 - abs(i - c) * 255 // (c + 1))
        im.putpixel((i, c), (255, 255, 255, a))
        im.putpixel((c, i), (255, 255, 255, a))
    for d in (-1, 1):
        im.putpixel((c + d, c + d), (255, 255, 255, 170))
        im.putpixel((c + d, c - d), (255, 255, 255, 170))
    im.putpixel((c, c), W)
    return im


GLYPHS = [
    ["..#..", ".###.", "..#..", "..#..", ".#.#."],
    ["#...#", ".#.#.", "..#..", ".#.#.", "#...#"],
    ["###..", "#..#.", "###..", "#.#..", "#..#."],
    [".###.", "#....", ".##..", "...#.", "###.."],
    ["#.#.#", "#.#.#", "#####", "..#..", "..#.."],
    ["..#..", ".#.#.", "#...#", ".#.#.", "..#.."],
    ["#####", "..#..", ".###.", "..#..", "#####"],
    ["#....", "##...", "#.#..", "#..#.", "#####"],
]


def glyph(rows):
    im = Image.new('RGBA', (8, 8))
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == '#':
                im.putpixel((i + 1, j + 1), W)
    # halo suave
    out = im.copy()
    for y in range(8):
        for x in range(8):
            if im.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < 8 and 0 <= yy < 8 and im.getpixel((xx, yy))[3] > 0:
                        out.putpixel((x, y), (255, 255, 255, 70))
                        break
    return out


def shard():
    im = Image.new('RGBA', (8, 8))
    pts = [(3, 0), (4, 1), (5, 3), (4, 6), (3, 7), (2, 5), (2, 2)]
    d = ImageDraw.Draw(im)
    d.polygon(pts, fill=(235, 235, 235, 255))
    d.line([(3, 1), (3, 6)], fill=W)
    return im


def ring(n, width, soft=True):
    im = Image.new('RGBA', (n, n))
    c = (n - 1) / 2
    r = n / 2 - width
    for y in range(n):
        for x in range(n):
            d = abs(math.hypot(x - c, y - c) - r)
            if d < width:
                a = int(255 * (1 - d / width) ** (1.2 if soft else 0.3))
                im.putpixel((x, y), (255, 255, 255, a))
    return im


def magic_circle(n=64):
    """Círculo mágico: dois anéis, hexagrama, runas entre os anéis (só traços, para recorte nítido)."""
    im = Image.new('RGBA', (n, n))
    d = ImageDraw.Draw(im)
    c = n / 2
    for r, w in ((n / 2 - 1.5, 2), (n / 2 - 9, 1), (n / 2 - 22, 1)):
        d.ellipse([c - r, c - r, c + r, c + r], outline=W, width=w)
    r = n / 2 - 9
    for k in range(2):
        pts = [(c + r * math.cos(math.radians(90 + k * 60 + i * 120)), c + r * math.sin(math.radians(90 + k * 60 + i * 120))) for i in range(3)]
        d.polygon(pts, outline=W)
    # runas no anel externo
    for i in range(12):
        a = math.radians(i * 30 + 15)
        rr = n / 2 - 5.2
        x, y = c + rr * math.cos(a), c + rr * math.sin(a)
        g = GLYPHS[i % len(GLYPHS)]
        for j, row in enumerate(g[:4]):
            for k2, ch in enumerate(row[:3]):
                if ch == '#':
                    im.putpixel((int(x - 1 + k2), int(y - 2 + j)), W)
    d.ellipse([c - 2, c - 2, c + 2, c + 2], fill=W)
    return im


def clock(n=64):
    im = Image.new('RGBA', (n, n))
    d = ImageDraw.Draw(im)
    c = n / 2
    d.ellipse([1, 1, n - 2, n - 2], outline=W, width=2)
    d.ellipse([7, 7, n - 8, n - 8], outline=W, width=1)
    for i in range(12):
        a = math.radians(i * 30)
        r0, r1 = n / 2 - 8, n / 2 - (13 if i % 3 == 0 else 10)
        d.line([(c + r0 * math.cos(a), c + r0 * math.sin(a)), (c + r1 * math.cos(a), c + r1 * math.sin(a))], fill=W, width=2 if i % 3 == 0 else 1)
    d.line([(c, c), (c, c - 20)], fill=W, width=2)
    d.line([(c, c), (c + 13, c + 6)], fill=W, width=2)
    d.ellipse([c - 2, c - 2, c + 2, c + 2], fill=W)
    return im


def orb(n=16):
    im = soft_dot(n, 0.45)
    # núcleo sólido para o recorte do item (itens gerados usam alfa recortado)
    for y in range(n):
        for x in range(n):
            a = im.getpixel((x, y))[3]
            im.putpixel((x, y), (255, 255, 255, 255 if a > 90 else 0))
    return im


if __name__ == '__main__':
    parts = {
        'mote': [('mote', soft_dot(8))],
        'spark': [('spark', star(8))],
        'ember': [('ember', soft_dot(4, 0.5))],
        'shard': [('shard', shard())],
        'wave': [('wave', ring(32, 2.2))],
        'glyph': [('glyph_%d' % i, glyph(g)) for i, g in enumerate(GLYPHS)],
    }
    for name, frames in parts.items():
        for fname, im in frames:
            save(im, 'textures/particle/%s.png' % fname)
        path = os.path.join(ASSETS, 'particles', name + '.json')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        json.dump({'textures': ['mistech:%s' % f for f, _ in frames]}, open(path, 'w'), indent=2)
    items = {'vfx_circle': magic_circle(), 'vfx_ring': ring(64, 2.6, soft=False), 'vfx_orb': orb(), 'vfx_clock': clock()}
    for name, im in items.items():
        save(im, 'textures/item/%s.png' % name)
        json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mistech:item/' + name}},
                  open(os.path.join(ASSETS, 'models', 'item', name + '.json'), 'w'), indent=2)
        json.dump({'model': {'type': 'minecraft:model', 'model': 'mistech:item/' + name,
                             'tints': [{'type': 'minecraft:custom_model_data', 'index': 0, 'default': 16777215}]}},
                  open(os.path.join(ASSETS, 'items', name + '.json'), 'w'), indent=2)
    print('ok vfx')
