"""Texturas da Forja Trituradora montada (mós estilo britador + forja arcana embaixo).
  crusher_wheel_face.png   face da mó (mapeada pela posição: o centro da textura é o eixo; raio da mó = 6 px)
  crusher_wheel_rim.png    borda da mó (ranhuras na direção do eixo)
  crusher_wheel_tooth.png  dentes
  forge_mouth.png / forge_mouth_on.png (+ .mcmeta, 4 quadros)  boca da forja: brasa apagada / fogo arcano
Uso: python3 tools/qol/forge_textures.py
"""
import json
import math
import os
import random

from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'mistech', 'textures', 'block')

STONE = [(96, 98, 112), (108, 110, 124), (120, 122, 136), (88, 90, 104)]
DARK = (52, 50, 66)
GOLD = [(176, 122, 34), (226, 172, 58), (250, 214, 104)]
CYAN = [(40, 120, 150), (70, 200, 230), (150, 245, 255), (230, 255, 255)]


def wheel_face():
    """Pedra clara, aro escuro, anel de ouro e 4 raios de runa (dá para ver a mó girando)."""
    rnd = random.Random(7)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r = math.hypot(dx, dy)
            a = math.degrees(math.atan2(dy, dx)) % 90
            spoke = a < 14 or a > 76
            c = [(132, 134, 148), (144, 146, 160), (122, 124, 138), (150, 152, 166)][rnd.randrange(4)]
            if r > 5.7:
                c = [(84, 84, 98), (76, 76, 90)][rnd.randrange(2)]                  # aro
            elif r > 4.7:
                c = GOLD[1] if rnd.random() < 0.7 else GOLD[2]                      # anel de ouro
            elif r > 4.1:
                c = (96, 98, 112)
            elif r <= 1.8:
                c = DARK                                                            # cubo
            elif spoke:
                c = CYAN[2] if r < 3.2 else CYAN[1]                                 # raios
            im.putpixel((x, y), c + (255,))
    return im


def wheel_rim():
    rnd = random.Random(11)
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = STONE[rnd.randrange(4)]
            if x % 4 == 0:
                c = (70, 70, 84)
            elif x % 4 == 1:
                c = (134, 136, 150)
            im.putpixel((x, y), c + (255,))
    return im


def wheel_tooth():
    rnd = random.Random(13)
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = [(58, 58, 70), (66, 66, 80), (50, 50, 62)][rnd.randrange(3)]
            if y < 2 or x < 1:
                c = (104, 106, 122)
            if y > 13 or x > 14:
                c = (38, 38, 48)
            im.putpixel((x, y), c + (255,))
    return im


def mouth(frame, on):
    rnd = random.Random(100 + frame * 7 + (1000 if on else 0))
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            # fundo: tijolo escuro
            c = (34, 30, 44) if (y % 4 == 0 or (x + (y // 4) * 4) % 8 == 0) else (46, 40, 58)
            if on:
                # chamas subindo da grelha; altura varia por coluna e quadro
                h = 6 + 4 * math.sin(x * 0.9 + frame * 1.6) + 2 * math.sin(x * 2.3 - frame * 2.1) + rnd.random() * 2
                d = (15 - y)
                if d < h:
                    k = d / max(h, 1)
                    c = CYAN[3] if k < 0.25 else CYAN[2] if k < 0.5 else CYAN[1] if k < 0.8 else (120, 80, 200)
                if y >= 13:
                    c = [(255, 200, 90), (250, 150, 50), (200, 90, 40)][rnd.randrange(3)]       # brasas
            else:
                if y >= 13:
                    c = [(90, 40, 30), (120, 52, 36), (60, 34, 34), (150, 70, 40)][rnd.randrange(4)]
            if y == 12 and x % 3 != 1:
                c = (30, 30, 36)                                                                    # grelha
            im.putpixel((x, y), c + (255,))
    return im


def strip(frames):
    im = Image.new('RGBA', (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        im.paste(f, (0, 16 * i))
    return im


def main():
    wheel_face().save(os.path.join(OUT, 'crusher_wheel_face.png'))
    wheel_rim().save(os.path.join(OUT, 'crusher_wheel_rim.png'))
    wheel_tooth().save(os.path.join(OUT, 'crusher_wheel_tooth.png'))
    mouth(0, False).save(os.path.join(OUT, 'forge_mouth.png'))
    strip([mouth(i, True) for i in range(4)]).save(os.path.join(OUT, 'forge_mouth_on.png'))
    with open(os.path.join(OUT, 'forge_mouth_on.png.mcmeta'), 'w') as f:
        json.dump({'animation': {'frametime': 3, 'interpolate': True}}, f)
    print('ok')


if __name__ == '__main__':
    main()
