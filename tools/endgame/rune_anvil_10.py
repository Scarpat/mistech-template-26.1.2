"""Fase 9: a Bigorna Rúnica passa a ter 10 engastes (Celeste). Refaz a fonte da tela com 5×2 espaços
(x = 70 + 20·i, y = 24 / 46). Idempotente: parte de uma cópia da fonte antiga (rune_anvil_8.png).
Uso: python3 tools/endgame/rune_anvil_10.py e depois restyle('rune_anvil')."""
import os
from PIL import Image

S = 'tools/gui/source/'
if not os.path.exists(S + 'rune_anvil_8.png'):
    Image.open(S + 'rune_anvil.png').save(S + 'rune_anvil_8.png')
im = Image.open(S + 'rune_anvil_8.png').convert('RGBA')
FRAME = im.crop((71, 23, 89, 41))
PLAIN = Image.open(S + 'astral_generator.png').convert('RGBA').crop((20, 60, 38, 78))
# apaga os 8 espaços antigos (e os enfeites entre eles)
for x in range(69, 160, 18):
    for y in (21, 39, 47):
        im.paste(PLAIN.crop((0, 0, min(18, 160 - x), 18)), (x, y))
for i in range(10):
    im.paste(FRAME, (70 + 20 * (i % 5) - 1, 24 + 22 * (i // 5) - 1))
im.save(S + 'rune_anvil.png')
print('ok')
