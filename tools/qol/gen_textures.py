"""Texturas da leva de Qualidade de Vida (outubro/2026).

Uso: python3 tools/qol/gen_textures.py <pasta com assets/minecraft/textures do jar do MC>
Gera em src/main/resources/assets/mistech/textures:
  block/arcane_furnace_*        (recolor da Fornalha Rúnica: pedra índigo, fogo de alma, rebites dourados)
  block/arcane_extractor_*      (recolor do Extrator: obsidiana com veios dourados)
  block/arcane_compressor_*     (pedra-negra polida índigo + ametista)
  item/runic_plate, item/*_upgrade, item/scroll_case
"""
import colorsys
import os
import shutil
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'mistech', 'textures')
VANILLA = sys.argv[1] if len(sys.argv) > 1 else 'vanilla/assets/minecraft/textures'


def load(path):
    return Image.open(path).convert('RGBA')


def recolor(im, fn):
    im = im.copy()
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            h, s, v = fn(h, s, v)
            r2, g2, b2 = colorsys.hsv_to_rgb(h % 1.0, max(0, min(1, s)), max(0, min(1, v)))
            px[x, y] = (int(r2 * 255), int(g2 * 255), int(b2 * 255), a)
    return im


def is_warm(h, s):
    return s > 0.45 and (h < 0.14 or h > 0.95)


def arcane_stone(h, s, v):
    """Pedra roxa → índigo escuro; fogo laranja → fogo de alma (ciano)."""
    if is_warm(h, s):
        return 0.50, s * 0.9, min(1.0, v * 1.05)
    if 0.70 < h < 0.85 and s > 0.35:  # runas roxas → douradas
        return 0.12, 0.75, min(1.0, v * 1.25)
    return 0.70, s * 0.8 + 0.08, v * 0.62


def gold_veins(h, s, v):
    """Veios roxos → dourados; fundo fica obsidiana."""
    if s > 0.35 and v > 0.18:
        return 0.11, 0.85, min(1.0, v * 1.6)
    return h, s, v


def indigo(h, s, v):
    return 0.70, 0.35, v * 0.9


def rivets(im, color=(232, 190, 80, 255), shade=(150, 108, 40, 255)):
    im = im.copy()
    px = im.load()
    for fy in range(im.height // 16):
        for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
            px[x, fy * 16 + y] = color
            px[x + (1 if x < 8 else -1), fy * 16 + y] = shade
    return im


def save(im, rel):
    path = os.path.join(ROOT, rel + '.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)
    print('ok', rel)


def blocks():
    b = os.path.join(ROOT, 'block')
    for part in ['front', 'front_on', 'side', 'top']:
        src = load(os.path.join(b, 'runic_furnace_' + part + '.png'))
        save(rivets(recolor(src, arcane_stone)), 'block/arcane_furnace_' + part)
    for part in ['side', 'top', 'bottom', 'inner', 'side_on', 'top_on', 'bottom_on', 'inner_on']:
        name = 'essence_extractor_' + part
        src = load(os.path.join(b, name + '.png'))
        out = recolor(src, gold_veins)
        if part.startswith('side'):
            out = rivets(out)
        save(out, 'block/arcane_extractor_' + part)
        meta = os.path.join(b, name + '.png.mcmeta')
        if os.path.exists(meta):
            shutil.copy(meta, os.path.join(b, 'arcane_extractor_' + part + '.png.mcmeta'))
    body = recolor(load(os.path.join(VANILLA, 'block', 'polished_blackstone.png')), indigo)
    save(rivets(body), 'block/arcane_compressor_body')
    top = recolor(load(os.path.join(VANILLA, 'block', 'chiseled_polished_blackstone.png')), indigo)
    am = load(os.path.join(VANILLA, 'block', 'amethyst_block.png'))
    px, apx = top.load(), am.load()
    for y in range(5, 11):
        for x in range(5, 11):
            px[x, y] = apx[x, y]
    save(top, 'block/arcane_compressor_top')


# ---------------------------------------------------------------- itens (pixel art)

def draw(rows, palette):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                px[x, y] = palette[ch]
    return im


PLATE = [
    '................',
    '................',
    '..oooooooooooo..',
    '.oCCCCCCCCCCCCo.',
    '.oCccccccccccdo.',
    '.oCcmmmmmmmmcdo.',
    '.oCcmmmmmmmmcdo.',
    '.oCcmmmmmmmmcdo.',
    '.oCcmmmmmmmmcdo.',
    '.oCcmmmmmmmmcdo.',
    '.oCcmmmmmmmmcdo.',
    '.oCccccccccccdo.',
    '.oCdddddddddddo.',
    '..oooooooooooo..',
    '................',
    '................',
]
PLATE_PAL = {'m': (92, 52, 40, 255), 'o': (58, 34, 24, 255), 'C': (231, 140, 98, 255), 'c': (196, 105, 72, 255), 'd': (150, 74, 52, 255)}

GLYPHS = {
    'runic_plate': (['....', '.xx.', '.xx.', '....'], None),
    'speed_upgrade': ([
        '.....xx.',
        '....xx..',
        '...xx...',
        '..xxxxx.',
        '....xx..',
        '...xx...',
        '..xx....',
        '.x......'], ((255, 236, 120, 255), (196, 150, 30, 255))),
    'efficiency_upgrade': ([
        '...x....',
        '...xx...',
        '..xxxx..',
        '.xxxxxx.',
        '.xxxxxx.',
        '.xxxxxx.',
        '..xxxx..',
        '...xx...'], ((120, 236, 140, 255), (40, 140, 70, 255))),
    'range_upgrade': ([
        '...xx...',
        '..x..x..',
        '.x.xx.x.',
        'x.x..x.x',
        'x.x..x.x',
        '.x.xx.x.',
        '..x..x..',
        '...xx...'], ((210, 150, 255, 255), (120, 60, 190, 255))),
}


def upgrades():
    for name, (glyph, colors) in GLYPHS.items():
        im = draw(PLATE, PLATE_PAL)
        px = im.load()
        if colors is None:
            # placa em branco: só um sulco no meio
            for y in range(6, 10):
                for x in range(5, 11):
                    if (x + y) % 3 == 0:
                        px[x, y] = PLATE_PAL['C']
        else:
            hi, lo = colors
            for gy, row in enumerate(glyph):
                for gx, ch in enumerate(row):
                    if ch == 'x':
                        px[4 + gx, 4 + gy] = hi
                        if 4 + gy + 1 < 12 and (gy + 1 >= len(glyph) or glyph[gy + 1][gx] != 'x'):
                            px[4 + gx, 5 + gy] = lo
        save(im, 'item/' + name)


SCROLL_CASE = [
    '.......pp.......',
    '......pPPp......',
    '.....GGGGGG.....',
    '.....GgggGG.....',
    '.....LLlLLL.....',
    '.....LLlLLL.....',
    '.....LLlLLL.....',
    '.....SSSSSS.....',
    '.....LLlLLL.....',
    '.....LLlLLL.....',
    '.....LLlLLL.....',
    '.....LLlLLL.....',
    '.....SSSSSS.....',
    '.....GGGGGG.....',
    '.....GgggGG.....',
    '......dddd......',
]
SCROLL_PAL = {
    'p': (226, 210, 168, 255), 'P': (246, 236, 204, 255),
    'G': (226, 182, 70, 255), 'g': (160, 118, 40, 255),
    'L': (124, 74, 46, 255), 'l': (96, 54, 32, 255),
    'S': (160, 110, 210, 255), 'd': (70, 40, 26, 255),
}


def scroll_case():
    save(draw(SCROLL_CASE, SCROLL_PAL), 'item/scroll_case')


if __name__ == '__main__':
    blocks()
    upgrades()
    scroll_case()


# ---------------------------------------------------------------- telas das máquinas arcanas

def gui():
    src = load(os.path.join(ROOT, 'gui', 'container', 'runic_crusher.png'))
    slot = src.crop((39, 24, 57, 42))
    gauge = src.crop((150, 15, 168, 71))
    patch = src.crop((12, 20, 28, 36))

    def erase(im, box):
        x0, y0, x1, y1 = box
        for y in range(y0, y1, 16):
            for x in range(x0, x1, 16):
                tile = patch.crop((0, 0, min(16, x1 - x), min(16, y1 - y)))
                im.paste(tile, (x, y))

    def tint(im):
        head = im.crop((0, 0, 176, 166))
        head = recolor(head, lambda h, s, v: (0.72, min(1, s * 0.6 + 0.12), v))
        out = im.copy()
        out.paste(head, (0, 0))
        return out

    layouts = {
        'arcane_furnace': ([(40, 17), (40, 35), (40, 53), (98, 17), (98, 35), (98, 53)], False),
        'arcane_extractor': ([(40, 35), (98, 26), (98, 44)], True),
        'arcane_compressor': ([(40, 35), (98, 17), (98, 35), (98, 53)], True),
    }
    for name, (slots, tank) in layouts.items():
        im = src.copy()
        erase(im, (39, 24, 57, 42))
        erase(im, (39, 52, 57, 70))
        erase(im, (97, 25, 135, 63))
        for (x, y) in slots:
            im.paste(slot, (x - 1, y - 1))
        if tank:
            im.paste(gauge, (122, 15))
        save(tint(im), 'gui/container/' + name)


if __name__ == '__main__':
    gui()
