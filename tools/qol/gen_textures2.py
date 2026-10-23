"""Texturas da leva 2 (20/10/2026): pós dos metais, Triturador Arcano, Liquefator, Revestimento Arcano, Forja Trituradora,
Pó de Mandrágora e as telas novas.

Uso: python3 tools/qol/gen_textures2.py <pasta assets/minecraft/textures do jar do MC>
"""
import colorsys
import os
import shutil
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from gen_textures import ROOT, load, recolor, rivets, save, arcane_stone  # noqa: E402

VANILLA = sys.argv[1] if len(sys.argv) > 1 else 'vanilla/assets/minecraft/textures'


def v(name):
    return load(os.path.join(VANILLA, 'block', name + '.png'))


def avg_color(im):
    px = [p for p in im.getdata() if p[3] > 0]
    r = sum(p[0] for p in px) / len(px)
    g = sum(p[1] for p in px) / len(px)
    b = sum(p[2] for p in px) / len(px)
    return colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)


def tint_like(src, ref, boost=1.15):
    """Mantém o brilho de src e usa o matiz/saturação médios de ref."""
    h, s, _ = avg_color(ref)
    return recolor(src, lambda _h, _s, val: (h, min(1.0, s * 1.2 + 0.05), min(1.0, val * boost)))


def indigo(h, s, val):
    return 0.70, 0.35, val * 0.8


def soul_fire(h, s, val):
    if s > 0.35 and (h < 0.16 or h > 0.92):
        return 0.50, s * 0.9, min(1.0, val * 1.05)
    return 0.70, 0.35, val * 0.75


def dusts():
    iron = load(os.path.join(ROOT, 'item', 'iron_dust.png'))
    for metal in ['mithril', 'adamantine', 'orichalcum']:
        ingot = load(os.path.join(ROOT, 'item', metal + '_ingot.png'))
        save(tint_like(iron, ingot), 'item/' + metal + '_dust')
    root = load(os.path.join(ROOT, 'item', 'mandrake_root.png'))
    pile = tint_like(iron, root, 1.25)
    px = pile.load()
    # pontinhos roxos (veios da folha)
    for (x, y) in [(5, 9), (9, 8), (7, 11), (11, 11)]:
        if px[x, y][3] > 0:
            px[x, y] = (150, 80, 190, 255)
    save(pile, 'item/mandrake_powder')


def casing():
    base = recolor(v('polished_deepslate'), indigo)
    px = base.load()
    gold, shade = (232, 190, 80, 255), (150, 108, 40, 255)
    for i in range(16):
        for (x, y) in [(i, 0), (i, 15), (0, i), (15, i)]:
            px[x, y] = shade if (x + y) % 2 else (70, 52, 96, 255)
    save(rivets(base, gold, shade), 'block/arcane_casing')
    save(rivets(recolor(v('blast_furnace_top'), indigo), gold, shade), 'block/crusher_forge_top')
    save(rivets(recolor(v('blast_furnace_front'), soul_fire), gold, shade), 'block/crusher_forge_front')
    on = recolor(v('blast_furnace_front_on'), soul_fire)
    save(on, 'block/crusher_forge_front_on')
    shutil.copy(os.path.join(VANILLA, 'block', 'blast_furnace_front_on.png.mcmeta'),
                os.path.join(ROOT, 'block', 'crusher_forge_front_on.png.mcmeta'))


def crusher():
    save(recolor(v('cut_copper'), lambda h, s, val: (0.12, 0.7, min(1.0, val * 1.1))), 'block/arcane_crusher_trim')
    for name in ['runic_crusher_plate', 'runic_crusher_plate_on']:
        path = os.path.join(ROOT, 'block', name + '.png')
        save(recolor(load(path), arcane_stone), 'block/' + name.replace('runic', 'arcane'))
        meta = path + '.mcmeta'
        if os.path.exists(meta):
            shutil.copy(meta, os.path.join(ROOT, 'block', name.replace('runic', 'arcane') + '.png.mcmeta'))


def liquefier():
    for part in ['side', 'top', 'bottom', 'inner']:
        src = v('cauldron_' + part)
        save(recolor(src, indigo), 'block/liquefier_' + part)
        hot = recolor(src, indigo)
        if part == 'side':
            px = hot.load()
            for x in range(2, 14):
                px[x, 13] = (255, 140, 40, 255)
                px[x, 14] = (200, 70, 20, 255)
            hot = rivets(hot)
        save(hot, 'block/liquefier_' + part + '_on')


def gui():
    from gen_textures import load as _load  # noqa
    src = load(os.path.join(ROOT, 'gui', 'container', 'runic_crusher.png'))
    slot = src.crop((39, 24, 57, 42))
    gauge = src.crop((150, 15, 168, 71))
    patch = src.crop((12, 20, 28, 36))

    def erase(im, box):
        x0, y0, x1, y1 = box
        for y in range(y0, y1, 16):
            for x in range(x0, x1, 16):
                im.paste(patch.crop((0, 0, min(16, x1 - x), min(16, y1 - y))), (x, y))

    def tint(im, hue):
        head = recolor(im.crop((0, 0, 176, 166)), lambda h, s, val: (hue, min(1, s * 0.6 + 0.12), val))
        out = im.copy()
        out.paste(head, (0, 0))
        return out

    layouts = {
        'arcane_crusher': ([(22, 25), (40, 25), (31, 53), (98, 17), (116, 17), (98, 35), (116, 35), (98, 53), (116, 53)], False, 0.72),
        'crusher_forge': ([(40, 17), (40, 35), (40, 53), (98, 26), (116, 26), (98, 44), (116, 44)], False, 0.05),
        'liquefier': ([(40, 25), (40, 53), (98, 26), (98, 44)], True, 0.80),
    }
    for name, (slots, tank, hue) in layouts.items():
        im = src.copy()
        erase(im, (39, 24, 57, 42))
        erase(im, (39, 52, 57, 70))
        erase(im, (97, 25, 135, 63))
        for (x, y) in slots:
            im.paste(slot, (x - 1, y - 1))
        if tank:
            im.paste(gauge, (122, 15))
        save(tint(im, hue), 'gui/container/' + name)


if __name__ == '__main__':
    dusts()
    casing()
    crusher()
    liquefier()
    gui()
