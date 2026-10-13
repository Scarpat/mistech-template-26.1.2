"""Texturas dos metais derretidos (recolor da lava vanilla). Uso: python3 tools/gen_molten_textures.py"""
import colorsys, os, shutil
from PIL import Image
SRC = "build/tmp/claude/molten/assets/minecraft/textures"
OUT = "src/main/resources/assets/mistech/textures"
# (escuro, base, claro) por metal
METALS = {
    "mithril": ((20, 60, 120), (110, 205, 255), (235, 250, 255)),
    "adamantine": ((70, 8, 20), (215, 60, 80), (255, 200, 205)),
    "orichalcum": ((110, 45, 5), (255, 165, 60), (255, 245, 200)),
}
def grad(t, c):
    d, b, l = c
    if t < 0.5:
        k = t / 0.5; a, z = d, b
    else:
        k = (t - 0.5) / 0.5; a, z = b, l
    return tuple(int(a[i] + (z[i] - a[i]) * k) for i in range(3))
def lum(r, g, b):
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255
def recolor(path, colors, only_hot=False):
    im = Image.open(path).convert("RGBA")
    px = im.load()
    lums = [lum(*px[x, y][:3]) for y in range(im.height) for x in range(im.width) if px[x, y][3]]
    lo, hi = min(lums), max(lums)
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            if only_hot:
                h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
                if s < 0.35 or not (h < 0.17 or h > 0.95):
                    continue
            t = (lum(r, g, b) - lo) / max(hi - lo, 1e-6)
            px[x, y] = grad(t, colors) + (a,)
    return im
for name, c in METALS.items():
    for kind in ("still", "flow"):
        src = f"{SRC}/block/lava_{kind}.png"
        dst = f"{OUT}/block/molten_{name}_{kind}.png"
        recolor(src, c).save(dst)
        shutil.copyfile(src + ".mcmeta", dst + ".mcmeta")
    recolor(f"{SRC}/item/lava_bucket.png", c, only_hot=True).save(f"{OUT}/item/molten_{name}_bucket.png")
    print("ok", name)
