"""Ícones dos itens da fase 3 no padrão do Minecraft: cada um parte do sombreado de um item do jogo (mesma forma, luz e contorno)
e troca a rampa de cores. Uso: python tools/dream/items3.py (precisa das texturas do jogo em /home/claude/mcsrc)."""
import colorsys
from PIL import Image

T = 'src/main/resources/assets/mistech/textures/'
VAN = '/home/claude/mcsrc/assets/minecraft/textures/item/'


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def lum(p):
    return 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]


def recolor(src, ramp, keep=None, extra=None):
    """Troca cada pixel pelo degrau da rampa (escuro -> claro) correspondente à luminância relativa dele no sprite.
    {@code keep(p)} = True mantém o pixel original; {@code extra(x, y, cor)} pode ajustar a cor final."""
    im = Image.open(VAN + src + '.png').convert('RGBA')
    px = [(x, y, im.getpixel((x, y))) for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3] > 0]
    ls = [lum(p) for _, _, p in px if not (keep and keep(p))]
    lo, hi = min(ls), max(ls)
    out = Image.new('RGBA', im.size, (0, 0, 0, 0))
    for x, y, p in px:
        if keep and keep(p):
            out.putpixel((x, y), p)
            continue
        t = (lum(p) - lo) / max(1.0, hi - lo)
        c = rgb(ramp[min(len(ramp) - 1, int(t * len(ramp)))])
        if extra:
            c = extra(x, y, c)
        out.putpixel((x, y), c + (p[3],))
    return out


def hue_shift(c, dh, ds=0.0):
    h, s, v = colorsys.rgb_to_hsv(*(k / 255 for k in c))
    r, g, b = colorsys.hsv_to_rgb((h + dh) % 1, max(0, min(1, s + ds)), v)
    return (int(r * 255), int(g * 255), int(b * 255))


# Pó de Sonho: montinho de pó (forma do pó de glowstone) iridescente — lilás puxando para rosa e ciano conforme a posição
dust = recolor('glowstone_dust', [0x3a2a6a, 0x6a52a8, 0x9a7ad8, 0xc8a8f0, 0xf0d8ff, 0xffffff],
               extra=lambda x, y, c: hue_shift(c, (x - 8) * 0.025))
dust.save(T + 'item/dream_dust.png')

# Âmbar Onírico: gema (forma da esmeralda) em âmbar, com uma nuvenzinha presa no meio
amber = recolor('emerald', [0x4a1e04, 0x8a3a08, 0xc86a10, 0xf0a030, 0xffd070, 0xfff0b8])
for x, y in ((7, 7), (8, 7), (7, 8), (8, 8), (6, 8), (9, 8)):
    if amber.getpixel((x, y))[3]:
        amber.putpixel((x, y), (255, 248, 236, 255))
amber.save(T + 'item/dream_amber.png')

# Peixe-do-Ar: peixe tropical em tons pastel (rosa -> lilás), listras brancas mantidas
fish = recolor('tropical_fish', [0x5a2a6a, 0x9a4a9a, 0xe080c0, 0xffa8d8, 0xffd0ec, 0xfff4fa],
               keep=lambda p: p[0] > 220 and p[1] > 220 and p[2] > 220,
               extra=lambda x, y, c: hue_shift(c, -(x - 8) * 0.012))
fish.save(T + 'item/sky_fish_food.png')

# Fragmento de Memória: caco (forma do fragmento de eco) em sépia de foto antiga, brilho dourado
shard = recolor('echo_shard', [0x4a3420, 0x7a5a3a, 0xa88a60, 0xd0b488, 0xeedcb4, 0xfff8e0])
shard.save(T + 'item/memory_shard.png')

# Olho Vigia: olho (forma do olho do ender) — branco do olho com íris violeta e pupila escura
eye = recolor('ender_eye', [0x12061a, 0x4a1a7a, 0x9050d0, 0xc8b8c0, 0xece4dc, 0xffffff])
eye.save(T + 'item/watcher_eye.png')

# Essência do Sonhador: estrela (forma da estrela do Nether) de porcelana e vazio violeta, miolo dourado
ess = recolor('nether_star', [0x1a1040, 0x3a2a7a, 0x7a6ab8, 0xc8c0e8, 0xf4f0fc, 0xfff4c8])
ess.save(T + 'item/dreamer_essence.png')

# Sino do Despertar: o sino do jogo, dourado com sombras violeta
bell = recolor('bell', [0x2a1a3a, 0x5a3a5a, 0xa07a40, 0xd8b048, 0xf8e080, 0xfff8d0])
bell.save(T + 'item/awakening_bell.png')

# Apanha-Sonho Lúcido: o Apanha-Sonho do mod recolorido em violeta e ouro
src = Image.open(T + 'block/dreamcatcher.png').convert('RGBA')
out = Image.new('RGBA', src.size, (0, 0, 0, 0))
for y in range(src.height):
    for x in range(src.width):
        r, g, b, a = src.getpixel((x, y))
        if not a:
            continue
        h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
        if s < 0.15:
            nh, ns, nv = 0.13, 0.55, min(1.0, v * 1.1)
        else:
            nh, ns, nv = (0.78 + h * 0.15) % 1, min(1.0, s * 1.1 + 0.2), v
        rr, gg, bb = colorsys.hsv_to_rgb(nh, ns, nv)
        out.putpixel((x, y), (int(rr * 255), int(gg * 255), int(bb * 255), a))
out.save(T + 'item/lucid_dreamcatcher.png')
print('ok itens')
