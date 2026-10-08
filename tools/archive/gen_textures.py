# Texturas dos Pergaminhos Espaciais e do Espelho do Arquivo. Uso: python3 tools/archive/gen_textures.py
from PIL import Image
OUT = 'src/main/resources/assets/mistech/textures/item/'


def hexc(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def draw(rows, c, name):
    im = Image.new('RGBA', (16, 16))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in c:
                im.putpixel((x, y), c[ch])
    im.save(OUT + name + '.png')


SCROLL = [
    '................',
    '..oooooooooooo..',
    '.oRRRRRRRRRRRRo.',
    '..oppppppppppo..',
    '...opPPPPPPpo...',
    '...opPggPPPpo...',
    '...oPgPPgPPpo...',
    '...oPPgPPgPpo...',
    '...oPPPggPPpo...',
    '...opPPPPPPpo...',
    '...opppppppPo...',
    '..oBBBBBBBBBBo..',
    '..obbbbbbbbbbo..',
    '..oBBBBBBBBBBo..',
    '...oooooooooo...',
    '................']
base = {'o': hexc('3E2B1A'), 'p': hexc('D9C59A'), 'P': hexc('EEDFB8'), 'g': hexc('8A4FD8')}
tiers = {
    'archive_scroll': ('A07850', '6B4A2A'),
    'mithril_archive_scroll': ('7FD8FF', '3C8BB0'),
    'adamantine_archive_scroll': ('E8576A', '8E2433'),
    'orichalcum_archive_scroll': ('FFB04A', 'B86F16'),
    'void_archive_scroll': ('5A2E9A', '1E0F33'),
}
for name, (light, dark) in tiers.items():
    c = dict(base)
    c['R'] = hexc(light)
    c['B'] = hexc(light)
    c['b'] = hexc(dark)
    if name == 'void_archive_scroll':
        c['g'] = hexc('E2C8FF')
    draw(SCROLL, c, name)

draw([
    '................',
    '.....aaaaaa.....',
    '....aGGGGGGa....',
    '...aGsMMMMsGa...',
    '...aGMvMMMMGa...',
    '...aGMMvMMMGa...',
    '...aGMMMvMMGa...',
    '...aGMMMMMsGa...',
    '....aGGGGGGa....',
    '.....aaHHaa.....',
    '.......HH.......',
    '.......HH.......',
    '......aHHa......',
    '......aHHa......',
    '.......aa.......',
    '................'],
    {'a': hexc('1E2733'), 'G': hexc('7FD8FF'), 'M': hexc('3B1E66'), 'v': hexc('B28CE0'), 's': hexc('E2C8FF'), 'H': hexc('5A3A1E')},
    'archive_mirror')
print('ok')
