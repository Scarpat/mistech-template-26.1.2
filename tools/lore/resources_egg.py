import random
from PIL import Image
import sys
sys.path.insert(0, "tools/pendencias2")
from res import A, flat_item
TEX = A + "textures/"


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def egg(name, base, spots):
    src = Image.open(TEX + 'item/gnome_spawn_egg.png').convert('RGBA')
    out = Image.new('RGBA', src.size, (0, 0, 0, 0))
    sp, op = src.load(), out.load()
    rnd = random.Random(name)
    for y in range(src.height):
        for x in range(src.width):
            c = sp[x, y]
            if not c[3]:
                continue
            lum = (c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11) / 255
            col = hexc(base)
            k = 0.55 + lum * 0.7
            pix = tuple(max(0, min(255, int(v * k))) for v in col)
            if rnd.random() < 0.18:
                pix = hexc(rnd.choice(spots))
            op[x, y] = pix + (255,)
    out.save(TEX + 'item/%s.png' % name)
    flat_item(name)


