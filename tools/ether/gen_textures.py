# Textura do Diapasão Arcano. Uso: python3 tools/ether/gen_textures.py
from PIL import Image
def hexc(h): h=h.lstrip('#'); return tuple(int(h[i:i+2],16) for i in (0,2,4))+(255,)
rows = [
    '...........a..a.',
    '..........aMa.aM',
    '..........aMa.aM',
    '.........aMMaaMa',
    '.........aMMMMa.',
    '........aMcMMa..',
    '.......aMMMMa...',
    '......aGGa......',
    '.....aGGa.......',
    '....aGGa........',
    '...aWWa.........',
    '..aWWa..........',
    '.aWWa...........',
    '.aWa............',
    '..a.............',
    '................']
c = {'a': hexc('1E2733'), 'M': hexc('7FD8FF'), 'c': hexc('E2C8FF'), 'G': hexc('8A4FD8'), 'W': hexc('5A3A1E')}
im = Image.new('RGBA', (16, 16))
for y, r in enumerate(rows):
    for x, ch in enumerate(r):
        if ch in c: im.putpixel((x, y), c[ch])
im.save('src/main/resources/assets/mistech/textures/item/arcane_tuner.png')
print('ok')
