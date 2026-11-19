"""Pendências pós-Fase 9: telas. Bomba do Boitatá (fonte da Bomba da Iara) e o segundo poço (mana líquida) no Poço de Mana.
Idempotente (o Poço parte de uma cópia da fonte antiga, mana_well_1.png). Depois rode restyle() de cada uma."""
import os
from PIL import Image

S = 'tools/gui/source/'


def src(name):
    return Image.open(S + name + '.png').convert('RGBA')


WELL = src('essence_washer').crop((123, 16, 139, 70))
src('iara_pump').save(S + 'boitata_pump.png')
if not os.path.exists(S + 'mana_well_1.png'):
    src('mana_well').save(S + 'mana_well_1.png')
im = src('mana_well_1')
im.paste(WELL, (7, 16))
im.save(S + 'mana_well.png')
print('ok')
