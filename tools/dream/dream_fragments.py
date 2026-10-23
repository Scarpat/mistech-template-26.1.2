"""Ícones dos Fragmentos de Sonho e textura da Fenda Onírica (padrão do Minecraft: contorno escuro, luz no alto à esquerda).
Uso: python tools/dream/dream_fragments.py (a partir da raiz do projeto)."""
import colorsys
from PIL import Image

T = 'src/main/resources/assets/mistech/textures/'

# Caco de sonho: lasca irregular de vidro (# = corpo, . = vazio). 16x16.
SHARD = [
    "................",
    "...........#....",
    "..........##....",
    ".........####...",
    "........#####...",
    ".......######...",
    "......#######...",
    ".....#######....",
    "....########....",
    "...#######......",
    "...######.......",
    "..######........",
    "..#####.........",
    "...###..........",
    "...##...........",
    "................",
]
# Caco amplificado: maior e com uma lasca solta ao lado
BIG = [
    "............#...",
    "...........##...",
    "..........####..",
    ".........#####..",
    "........######..",
    ".......#######..",
    "......########..",
    ".....########...",
    "....#########...",
    "...########..#..",
    "...#######..###.",
    "..#######....#..",
    "..######........",
    ".######.........",
    ".####...........",
    "..##............",
]


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def hue_shift(c, dh):
    h, s, v = colorsys.rgb_to_hsv(*(k / 255 for k in c))
    r, g, b = colorsys.hsv_to_rgb((h + dh) % 1, s, v)
    return (int(r * 255), int(g * 255), int(b * 255))


def shard(mask, ramp, outline, sparkle, iris=0.0, rim=None):
    """ramp: escuro -> claro (5 cores). Luz vinda de cima/esquerda; borda de baixo/direita mais escura."""
    h, w = len(mask), len(mask[0])
    inside = lambda x, y: 0 <= x < w and 0 <= y < h and mask[y][x] == '#'
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    for y in range(h):
        for x in range(w):
            if not inside(x, y):
                continue
            edge = not (inside(x - 1, y) and inside(x + 1, y) and inside(x, y - 1) and inside(x, y + 1))
            if edge:
                c = rgb(outline)
                if rim and not inside(x + 1, y + 1) is False and (not inside(x - 1, y) or not inside(x, y - 1)):
                    c = rgb(rim)
            else:
                # gradiente diagonal + faixa de reflexo
                t = 1.0 - (x * 0.5 + (h - y) * 0.5) / max(w, h)
                t = 1.0 - t
                band = (x + y) % 7 in (0, 1) and x - y > -4
                k = int(t * 3.2) + (1 if band else 0)
                if not inside(x + 1, y + 1) or not inside(x, y + 1):
                    k -= 1
                if not inside(x - 1, y - 1):
                    k += 1
                c = rgb(ramp[max(0, min(len(ramp) - 1, k))])
            if iris:
                c = hue_shift(c, (x - y) * iris)
            im.putpixel((x, y), c + (255,))
    # brilho em cruz (estrela presa no caco)
    sx, sy = sparkle
    for dx, dy, a in ((0, 0, 255), (1, 0, 200), (-1, 0, 200), (0, 1, 200), (0, -1, 200)):
        if inside(sx + dx, sy + dy):
            im.putpixel((sx + dx, sy + dy), (255, 255, 255, 255) if (dx, dy) == (0, 0) else (255, 250, 230, 255))
    return im


# Fragmento comum: lilás iridescente
base_ramp = [0x4a3a8a, 0x7a62c8, 0xa88ae8, 0xd8b8f8, 0xf8e8ff]
shard(SHARD, base_ramp, 0x2a1a5a, (7, 8), iris=0.012).save(T + 'item/dream_fragment.png')
# Amplificado: mais vivo, contorno dourado e dois brilhos
amp = shard(BIG, [0x5a2a9a, 0x9a52e0, 0xd08af8, 0xf8b8f0, 0xfff0ff], 0x3a1a6a, (8, 8), iris=0.006, rim=0xf0c050)
for x, y in ((5, 11), (10, 4)):
    amp.putpixel((x, y), (255, 248, 200, 255))
amp.putpixel((14, 10), (255, 240, 160, 255))
amp.save(T + 'item/amplified_dream_fragment.png')

# Fragmentos sintonizados: um caco tingido com as cores da região
REGIONS = {
    'gnome_meadow': ([0x2a6a2a, 0x48a848, 0x7ad870, 0xb8f0a0, 0xf0ffe0], 0x163a16),
    'sweet_mushroom_grove': ([0x8a2a6a, 0xd05aa0, 0xf08ac8, 0xffbce0, 0xfff0f8], 0x4a1238),
    'pillow_hills': ([0x8a8aa8, 0xb8b8d0, 0xd8d8ec, 0xf0f0fa, 0xffffff], 0x4a4a6a),
    'murky_wastes': ([0x2a2a22, 0x4a4a38, 0x6a6a50, 0x8a8a6a, 0xb0b090], 0x121208),
    'hand_forest': ([0x3a1a1a, 0x6a3030, 0x9a5048, 0xc87a6a, 0xf0b0a0], 0x1a0808),
    'falling_chasms': ([0x0a0a1a, 0x1a1a3a, 0x30305a, 0x50507a, 0x8080a8], 0x000008),
    'wrong_neighborhood': ([0x3a3020, 0x6a5a30, 0xa08a40, 0xd8c060, 0xfff0a0], 0x1a1408),
    'cloud_sea': ([0x3a8ac8, 0x6ab0e8, 0xa0d4f8, 0xd8f0ff, 0xffffff], 0x1a4a7a),
    'fractal_garden': ([0x7a1a8a, 0xc030c0, 0xf060d8, 0xffa0e8, 0xffe0f8], 0x3a0a4a),
    'inverted_forest': ([0x2a6a6a, 0x48a8a0, 0x80d8c8, 0xf0a8c8, 0xfff0f8], 0x103a3a),
    'starry_void': ([0x0a0a3a, 0x1a2a6a, 0x3a4aa8, 0x7a8ae0, 0xd0d8ff], 0x02021a),
    'memory_quilt': ([0x5a3a20, 0x8a6a3a, 0xc0a070, 0xe8d0a0, 0xfff8e0], 0x2a1a08),
    'abyssal_gaze': ([0x1a061a, 0x4a1a5a, 0x8a3a9a, 0xd06ab0, 0xffc0d8], 0x080008),
}
for rid, (ramp, outline) in REGIONS.items():
    shard(SHARD, ramp, outline, (7, 8)).save(T + f'item/dream_fragment_{rid}.png')

# Fenda Onírica: a espiral da Fenda Arcana recolorida em tons de sonho (azul-noite -> lilás -> rosa -> ciano), animada
src = Image.open(T + 'block/arcana_rift.png').convert('RGBA')
lum = lambda p: 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]
vals = [lum(src.getpixel((x, y))) for y in range(src.height) for x in range(src.width) if src.getpixel((x, y))[3]]
lo, hi = min(vals), max(vals)
ramp = [0x1a1a4a, 0x3a2a7a, 0x7a52c0, 0xc080e0, 0xf0a8e0, 0xa8e8ff, 0xffffff]
out = Image.new('RGBA', src.size, (0, 0, 0, 0))
for y in range(src.height):
    for x in range(src.width):
        p = src.getpixel((x, y))
        if not p[3]:
            continue
        t = (lum(p) - lo) / max(1, hi - lo)
        c = rgb(ramp[min(len(ramp) - 1, int(t * len(ramp)))])
        c = hue_shift(c, ((y // 16) % 16 - 8) * 0.006 + (x - 8) * 0.004)
        out.putpixel((x, y), c + (p[3],))
out.save(T + 'block/dream_rift.png')
open(T + 'block/dream_rift.png.mcmeta', 'w').write('{"animation":{"frametime":2,"interpolate":true}}\n')
print('ok')
