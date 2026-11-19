"""Monta as fontes das telas da Fase 9 (tools/gui/source/*.png) a partir das telas que já existem.
Depois rode tools/gui/restyle.py restyle(<nome>) para cada uma.
Uso (na pasta do projeto): python3 tools/endgame/compose_gui.py"""
from PIL import Image

S = 'tools/gui/source/'


def src(name):
    return Image.open(S + name + '.png').convert('RGBA')


SLOT = src('pure_generator').crop((29, 49, 47, 67))
WELL = src('essence_washer').crop((123, 16, 139, 70))
ARROW_BG = src('essence_washer').crop((66, 34, 90, 51))
ARROW = src('essence_washer').crop((176, 0, 200, 17))
FLAME_BG = src('pure_generator').crop((31, 32, 45, 46))
FLAME = src('pure_generator').crop((176, 17, 190, 31))
PLAIN = src('astral_generator').crop((20, 60, 38, 78))  # pedaço de fundo liso para apagar um espaço


def slots(im, at):
    for x, y in at:
        im.paste(SLOT, (x - 1, y - 1))


# Forja Estelar: 3 poços de metal à esquerda, metal/fragmento, seta, saída, poço de essência
im = src('astral_generator')
for gx in (8, 26, 44, 134):
    im.paste(WELL, (gx - 1, 16))
slots(im, [(64, 17), (64, 51), (112, 34)])
im.paste(ARROW_BG, (84, 34))
im.paste(ARROW, (176, 0))
im.save(S + 'star_forge.png')

# Reator Onírico: combustível com chama
im = src('astral_generator')
slots(im, [(30, 50)])
im.paste(FLAME_BG, (31, 32))
im.paste(FLAME, (176, 17))
im.save(S + 'dream_reactor.png')

# Bateria Celeste: igual à Aprimorada
src('advanced_runic_battery').save(S + 'celestial_battery.png')

# Conjurador Celeste: cristal A (40,17), cristal B (40,35), essência (40,53)
im = src('runic_conjurer')
im.paste(PLAIN, (39, 24))
im.paste(src('astral_generator').crop((20, 40, 26, 72)), (33, 40))  # sobra do enfeite antigo
slots(im, [(40, 17), (40, 35)])
im.save(S + 'celestial_conjurer.png')
print('ok')
