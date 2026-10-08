"""Texturas da Âncora Espacial e do Orbe de Translocação (recolor das vanilla + runas pintadas)."""
import sys; sys.path.insert(0, 'tools/pendencias2')
from PIL import Image
from gen_metal_tex import recolor, V, OUT
STONE = ['0d0814', '1a1126', '2a1d3b', '3b2b52', '50406a', '6a5a86']
CRYSTAL = ['1a0b33', '3d1a6e', '6b34b0', '9a5be0', 'c79bff', 'f0dcff']
PEARL = ['12081f', '34145a', '5e2a9a', '8e4fd0', 'c48bff', 'ffe9a8']
base = recolor(V + 'block/lodestone_side.png', STONE)
base.save(OUT + 'block/spatial_anchor_base.png')
top = recolor(V + 'block/lodestone_top.png', STONE)
top.save(OUT + 'block/spatial_anchor_top.png')
col = recolor(V + 'block/polished_blackstone.png', STONE, lo=0.1, hi=0.7)
# a runa vertical virou elementos próprios no modelo (tingidos pela cor da âncora); aqui ficam só os pontos de esmeralda
for (x, y) in ((5, 2), (10, 7), (5, 12)):
    col.putpixel((x, y), (60, 220, 130, 255))
    col.putpixel((x + 1, y), (30, 150, 90, 255))
col.save(OUT + 'block/spatial_anchor_column.png')
# cristal e runa em tons de cinza: o jogo tinge com a cor da âncora (violeta ou corante)
GRAY = ['3a3a3a', '6a6a6a', '9a9a9a', 'c4c4c4', 'e6e6e6', 'ffffff']
recolor(V + 'block/amethyst_block.png', GRAY).save(OUT + 'block/spatial_anchor_crystal.png')
rune = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        v = 255 if x % 2 == 0 else 190
        rune.putpixel((x, y), (v, v, v, 255))
rune.save(OUT + 'block/spatial_anchor_rune.png')
recolor(V + 'item/ender_pearl.png', PEARL).save(OUT + 'item/translocation_orb.png')
# ícone do item: pilar visto de frente
icon = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
def rect(img, src, x0, y0, x1, y1):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), src.getpixel((x % 16, y % 16)))
rect(icon, base, 2, 14, 14, 16)
rect(icon, col, 5, 6, 11, 14)
rect(icon, base, 4, 4, 12, 6)
crystal = recolor(V + 'block/amethyst_block.png', CRYSTAL)
for y in range(7, 13):
    icon.putpixel((7, y), (183, 120, 255, 255))
rect(icon, crystal, 6, 1, 10, 4)
icon.save(OUT + 'item/spatial_anchor.png')
print('ok')
