#!/usr/bin/env python3
"""Texturas da Fase 9 (endgame): Lingote/Bloco/Núcleo Celeste, equipamento Celeste (recolorido do Oricalco Puro),
peças da Forja Estelar e do Reator Onírico. Idempotente.
Uso (na pasta do projeto): python3 tools/endgame/gen_textures.py"""
import colorsys, math, os, random, shutil
from PIL import Image

A = 'src/main/resources/assets/mistech/textures/'
V = 'vanilla/assets/minecraft/textures/'
random.seed(9)


def img(p):
    return Image.open(p).convert('RGBA')


def hx(h, a=255):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def mix(a, b, t):
    return tuple(round(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (255,)


def lum(p):
    return (p[0] * 0.3 + p[1] * 0.59 + p[2] * 0.11) / 255


# degradê celeste: noite funda -> azul -> ciano pálido -> branco estelar
RAMP = [hx('120c3a'), hx('2a3a9a'), hx('4a8ae0'), hx('9fd8ff'), hx('f4fbff')]


def ramp(t):
    t = max(0.0, min(1.0, t)) * (len(RAMP) - 1)
    i = min(int(t), len(RAMP) - 2)
    return mix(RAMP[i], RAMP[i + 1], t - i)


def celestial(im, keep_gray=True, stars=0):
    """Mapeia a luminância para o degradê celeste; pixels quase cinza (cabos, contornos) só ganham um tom azulado."""
    out = im.copy()
    w, h = im.size
    ls = [lum(im.getpixel((x, y))) for y in range(h) for x in range(w)
          if im.getpixel((x, y))[3] and (not keep_gray or colorsys.rgb_to_hsv(*(c / 255 for c in im.getpixel((x, y))[:3]))[1] >= 0.10)]
    lo, hi = (min(ls), max(ls)) if ls else (0.0, 1.0)
    for y in range(h):
        for x in range(w):
            p = im.getpixel((x, y))
            if p[3] == 0:
                continue
            r, g, b = (c / 255 for c in p[:3])
            hh, s, v = colorsys.rgb_to_hsv(r, g, b)
            if keep_gray and s < 0.10:
                c = mix(p, ramp(lum(p)), 0.55)
                out.putpixel((x, y), c[:3] + (p[3],))
                continue
            c = ramp(0.12 + 0.86 * (lum(p) - lo) / ((hi - lo) or 1))
            out.putpixel((x, y), c[:3] + (p[3],))
    for _ in range(stars):
        for _ in range(40):
            x, y = random.randrange(w), random.randrange(h)
            p = out.getpixel((x, y))
            if p[3] and lum(p) > 0.45:
                out.putpixel((x, y), hx('ffffff'))
                break
    return out


def save(im, path):
    os.makedirs(os.path.dirname(A + path), exist_ok=True)
    im.save(A + path + '.png')


# ---------------------------------------------------------------- equipamento (recolor do Oricalco Puro)
GEAR = ['helmet', 'chestplate', 'leggings', 'boots', 'sword', 'spear', 'spear_in_hand', 'pickaxe', 'axe', 'shovel', 'hoe']
for g in GEAR:
    save(celestial(img(A + 'item/pure_orichalcum_%s.png' % g), stars=2), 'item/celestial_' + g)
for layer in ('humanoid', 'humanoid_leggings', 'humanoid_baby'):
    p = A + 'entity/equipment/%s/pure_orichalcum.png' % layer
    if os.path.exists(p):
        save(celestial(img(p), stars=6), 'entity/equipment/%s/celestial' % layer)

# ---------------------------------------------------------------- materiais
save(celestial(img(A + 'item/orichalcum_ingot.png'), keep_gray=False, stars=2), 'item/celestial_ingot')
blk = celestial(img(A + 'block/orichalcum_block.png'), keep_gray=False)
for (x, y) in ((3, 4), (11, 2), (7, 10), (13, 12), (2, 13)):
    blk.putpixel((x, y), hx('ffffff'))
save(blk, 'block/celestial_block')
# Núcleo Celeste: a estrela do Nether com o degradê celeste e um anel
core = celestial(img(V + 'item/nether_star.png'), keep_gray=False, stars=3)
save(core, 'item/celestial_core')


# ---------------------------------------------------------------- Forja Estelar
def van(n):
    return img(V + 'block/' + n + '.png').crop((0, 0, 16, 16))


def tint(im, dark, light, contrast=1.0):
    out = im.copy()
    ls = [lum(im.getpixel((x, y))) for y in range(16) for x in range(16)]
    a, b = min(ls), max(ls)
    for y in range(16):
        for x in range(16):
            t = (lum(im.getpixel((x, y))) - a) / ((b - a) or 1)
            t = 0.5 + (t - 0.5) * contrast
            out.putpixel((x, y), mix(hx(dark), hx(light), max(0, min(1, t))))
    return out


# lado: pedra-negra azulada com faixa celeste e runas
side = tint(van('polished_blackstone_bricks'), '0a0a1e', '3a3a6a', 0.9)
for x in range(16):
    side.putpixel((x, 0), hx('9fd8ff'))
    side.putpixel((x, 15), hx('2a3a9a'))
for (x, y) in ((4, 6), (5, 7), (4, 8), (11, 6), (10, 7), (11, 8)):
    side.putpixel((x, y), hx('9fd8ff'))
save(side, 'block/star_forge_side')
# boca da forja (frente): apagada e acesa
for on in (False, True):
    f = side.copy()
    for y in range(5, 13):
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 9)
            if on:
                c = mix(hx('f4fbff'), hx('4a8ae0'), min(1, d / 4.5))
            else:
                c = mix(hx('1a1a40'), hx('050510'), min(1, d / 4.5))
            f.putpixel((x, y), c)
    for x in range(3, 13):
        f.putpixel((x, 4), hx('e8c46a'))
    save(f, 'block/star_forge_front' + ('_on' if on else ''))
# topo: bigorna estelar com estrela no meio
top = tint(van('smooth_stone'), '14143a', '3a4a8a', 0.6)
for i in range(-3, 4):
    top.putpixel((7 + i, 7), hx('9fd8ff'))
    top.putpixel((7, 7 + i), hx('9fd8ff'))
top.putpixel((7, 7), hx('ffffff'))
for x in range(16):
    top.putpixel((x, 0), hx('e8c46a'))
    top.putpixel((x, 15), hx('e8c46a'))
    top.putpixel((0, x), hx('e8c46a'))
    top.putpixel((15, x), hx('e8c46a'))
save(top, 'block/star_forge_top')

# ---------------------------------------------------------------- Reator Onírico
shell = tint(van('amethyst_block'), '1a0e2e', '7a5ab0', 0.8)
for x in range(16):
    shell.putpixel((x, 0), hx('f0c8ff'))
    shell.putpixel((0, x), hx('f0c8ff'))
    shell.putpixel((x, 15), hx('0a0414'))
    shell.putpixel((15, x), hx('0a0414'))
save(shell, 'block/dream_reactor_shell')
for on in (False, True):
    eye = shell.copy()
    for y in range(3, 13):
        for x in range(2, 14):
            # olho fechado (linha) ou aberto (íris rosada)
            if on:
                d = math.hypot((x - 7.5) / 1.5, y - 7.5)
                if d < 4:
                    eye.putpixel((x, y), mix(hx('ffffff'), hx('ff7ad8'), d / 4) if d > 1.2 else hx('2a0a3a'))
            elif y == 8 and 3 <= x <= 12:
                eye.putpixel((x, y), hx('0a0414'))
            elif y == 9 and x in (4, 6, 9, 11):
                eye.putpixel((x, y), hx('0a0414'))
    save(eye, 'block/dream_reactor_eye' + ('_on' if on else ''))
print('ok')
