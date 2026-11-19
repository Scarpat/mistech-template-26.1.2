"""Monta as fontes das telas da Fase 7 (tools/gui/source/*.png) a partir das telas que já existem:
espaços de item copiados do Gerador Purificado, poço de tanque copiado da Lavadora.
Depois rode tools/gui/restyle.py restyle(<nome>) para cada uma.
Uso (na pasta do projeto): python3 tools/folk_machines/compose_gui.py"""
from PIL import Image

S = 'tools/gui/source/'


def src(name):
    return Image.open(S + name + '.png').convert('RGBA')


SLOT = src('pure_generator').crop((29, 49, 47, 67))
WELL = src('essence_washer').crop((123, 16, 139, 70))


def compose(base, slots, well=False, out=None):
    im = src(base)
    for x, y in slots:
        im.paste(SLOT, (x - 1, y - 1))
    if well:
        im.paste(WELL, (123, 16))
    im.save(S + out + '.png')


# Moinho do Saci e Bomba da Iara usam as fontes da Bicicleta e do Reator do jeito que são
src('gnome_bicycle').save(S + 'saci_windmill.png')
src('impure_reactor').save(S + 'iara_pump.png')
# Toco do Curupira: cachaça, mudas e 3×3 de saída
compose('astral_generator', [(26, 17), (26, 39)] + [(62 + 18 * c, 17 + 18 * r) for r in range(3) for c in range(3)], out='curupira_stump')
# Alambique: frascos, 3 ingredientes, 4×2 de saída e tanque de água
compose('astral_generator', [(20, 17), (40, 17), (58, 17), (76, 17)] + [(40 + 18 * c, 36 + 18 * r) for r in range(2) for c in range(4)],
        well=True, out='potion_still')
# Impressora: a Lavadora (matriz, papel, 2 saídas, seta, tanque) + lápis-lazúli
compose('essence_washer', [(18, 53)], out='scroll_printer')
print('ok')
