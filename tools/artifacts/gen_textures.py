# Gera as texturas dos Artefatos Rúnicos (itens + armadura vestida).
# Uso (na pasta do projeto): python3 tools/artifacts/gen_textures.py [pasta_vanilla]
# ATENÇÃO: depois deste, rode tools/artifacts/gen_designs.py e tools/artifacts/gen_icons.py (redesenham tudo).
# pasta_vanilla = onde estão assets/minecraft/textures (extraído do jar do MC); padrão: vanilla/
import sys, os
from PIL import Image

ROOT = 'src/main/resources/assets/mistech/textures/'
ITEM = ROOT + 'item/'
EQUIP = ROOT + 'entity/equipment/humanoid/'
VAN = (sys.argv[1] if len(sys.argv) > 1 else 'vanilla') + '/assets/minecraft/textures/'

def hexc(h):
    h = h.lstrip('#'); return tuple(int(h[i:i+2], 16) for i in (0, 2, 4)) + (255,)

def lum(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255.0

def ramp(palette, t):
    t = max(0.0, min(1.0, t)); n = len(palette) - 1
    i = min(int(t * n), n - 1); f = t * n - i
    a, b = palette[i], palette[i + 1]
    return tuple(int(a[k] + (b[k] - a[k]) * f) for k in range(3)) + (255,)

def recolor(src, palette, region=None, stretch=True):
    im = Image.open(src).convert('RGBA'); out = im.copy()
    px = [(x, y) for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3] > 0 and (region is None or region(x, y))]
    if not px: return out
    ls = [lum(im.getpixel(p)) for p in px]; lo, hi = min(ls), max(ls)
    for p, l in zip(px, ls):
        t = (l - lo) / (hi - lo) if stretch and hi > lo else l
        a = im.getpixel(p)[3]
        out.putpixel(p, ramp(palette, t)[:3] + (a,))
    return out

def draw(rows, colors, size=16):
    im = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colors: im.putpixel((x, y), colors[ch])
    return im

P = {
    'wind': [hexc('2E5566'), hexc('6FA9BE'), hexc('BFE9F2'), hexc('FFFFFF')],
    'frost': [hexc('1D3F7A'), hexc('3F7FD0'), hexc('9FD8FF'), hexc('EAF8FF')],
    'magma': [hexc('1C1412'), hexc('4A2A20'), hexc('B8401A'), hexc('FFB347')],
    'adam': [hexc('3A0F18'), hexc('8E2433'), hexc('E8576A'), hexc('FFC1C8')],
    'tide': [hexc('0B3A42'), hexc('17808A'), hexc('4FD2C9'), hexc('D8FFF8')],
    'shadow': [hexc('0B0612'), hexc('2A1840'), hexc('5A2E78'), hexc('B28CE0')],
    'ori': [hexc('5A3008'), hexc('B86F16'), hexc('FFB04A'), hexc('FFF1B8')],
    'aqua': [hexc('0D3B52'), hexc('2B8FB8'), hexc('7FE0FF'), hexc('E8FFFF')],
    'void': [hexc('140A24'), hexc('3B1E66'), hexc('8A4FD8'), hexc('E2C8FF')],
}

def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True); im.save(path)

# ----------------------------------------------------------------- itens (recolor)
save(recolor(ITEM + 'mithril_boots.png', P['wind']), ITEM + 'wind_boots.png')
sk = recolor(ITEM + 'mithril_boots.png', P['frost'])
for x in range(16):  # lâmina embaixo das botas
    for y in range(15, 3, -1):
        if sk.getpixel((x, y))[3] > 0:
            if y + 1 < 16: sk.putpixel((x, y + 1), hexc('DDE8F0'))
            break
save(sk, ITEM + 'frost_skates.png')
head = lambda x, y: x + (15 - y) >= 17
hm = recolor(VAN + 'item/mace.png', P['magma'], head)
hm2 = recolor(VAN + 'item/mace.png', P['adam'], lambda x, y: not head(x, y))
for y in range(16):
    for x in range(16):
        if not head(x, y) and hm2.getpixel((x, y))[3] > 0: hm.putpixel((x, y), hm2.getpixel((x, y)))
save(hm, ITEM + 'volcanic_hammer.png')
save(recolor(ITEM + 'adamantine_helmet.png', P['tide']), ITEM + 'tide_mask.png')
save(recolor(ITEM + 'orichalcum_chestplate.png', P['shadow']), ITEM + 'shadow_cloak.png')
ts = recolor(VAN + 'item/trident.png', P['ori'], lambda x, y: x + (15 - y) < 17)
ts2 = recolor(VAN + 'item/trident.png', P['aqua'], lambda x, y: x + (15 - y) >= 17)
for y in range(16):
    for x in range(16):
        if x + (15 - y) >= 17 and ts2.getpixel((x, y))[3] > 0: ts.putpixel((x, y), ts2.getpixel((x, y)))
save(ts, ITEM + 'tide_spear.png')
ring = Image.open(ITEM + 'breeze_ring.png').convert('RGBA')
ring = recolor(ITEM + 'breeze_ring.png', P['ori'], lambda x, y: y >= 5)
gem = recolor(ITEM + 'breeze_ring.png', P['void'], lambda x, y: y < 5)
for y in range(5):
    for x in range(16):
        if gem.getpixel((x, y))[3] > 0: ring.putpixel((x, y), gem.getpixel((x, y)))
save(ring, ITEM + 'spatial_ring.png')

# ----------------------------------------------------------------- itens (desenhados)
O = hexc('2B1D14')
save(draw([
    '................',
    '....a.a.a.......',
    '...aBaBaBa......',
    '...aBaBaBa.a....',
    '...aBBBBBaaBa...',
    '...aBbbcbBBBa...',
    '...aBbcbbBBa....',
    '...aBccccBBa....',
    '...aBbbcbBa.....',
    '....aBcbBBa.....',
    '....aBBBBBa.....',
    '....aYYYYYa.....',
    '....aYyYyYa.....',
    '....aYYYYYa.....',
    '.....aaaaa......',
    '................'],
    {'a': hexc('1E2733'), 'B': hexc('7FD8FF'), 'b': hexc('C9F0FF'), 'c': hexc('FFE14A'),
     'Y': hexc('E8C23A'), 'y': hexc('FFF2A0')}), ITEM + 'storm_gauntlet.png')
save(draw([
    '................',
    '................',
    '................',
    '..a....a....a...',
    '..aY..aYa..Ya...',
    '..aYa.aYa.aYa...',
    '..aYYaYcYaYYa...',
    '..aYYYYYYYYYa...',
    '..aYgYYwYYgYa...',
    '..aYYYYYYYYYa...',
    '..ayyyyyyyyya...',
    '...aaaaaaaaa....',
    '................',
    '................',
    '................',
    '................'],
    {'a': hexc('5A3A08'), 'Y': hexc('F2C744'), 'y': hexc('B8861E'), 'c': hexc('7FF0FF'),
     'g': hexc('FFF6C0'), 'w': hexc('BFFBFF')}), ITEM + 'beacon_crown.png')

def broom(bristle, light, band):
    return draw([
        '...............h',
        '..............hH',
        '.............hH.',
        '............hH..',
        '...........hH...',
        '..........hH....',
        '.........hH.....',
        '........hH......',
        '.......gG.......',
        '.....bbgGb......',
        '....bBBbBbb.....',
        '...bBlBBlBb.....',
        '..bBBlBBBb......',
        '.bBlBBBlb.......',
        '.bBBBBbb........',
        '..bbbb..........'],
        {'h': hexc('5A3A1E'), 'H': hexc('8A5E34'), 'g': band[0], 'G': band[1],
         'b': bristle[0], 'B': bristle[1], 'l': light})
save(broom((hexc('8A6A1E'), hexc('D8B04A')), hexc('F2D98A'), (hexc('6B4A2A'), hexc('9A7048'))), ITEM + 'broom.png')
save(broom((hexc('9FB8D8'), hexc('EAF2FF')), hexc('FFFFFF'), (hexc('B86F16'), hexc('FFB04A'))), ITEM + 'cloud_broom.png')
save(draw([
    '................',
    '................',
    '...aaaa..aaaa...',
    '...aVVa..aVVa...',
    '...appa..appa...',
    '...aMMa..aMMa...',
    '...aMMa..aMMa...',
    '...aMMa..aMMa...',
    '...aMMaaaaMMa...',
    '...aMMMMMMMMa...',
    '....aMmmmmMa....',
    '.....aaaaaa.....',
    '................',
    '................',
    '................',
    '................'],
    {'a': hexc('140A24'), 'V': hexc('E2C8FF'), 'p': hexc('8A4FD8'), 'M': hexc('3B1E66'), 'm': hexc('5A2E9A')}), ITEM + 'void_magnet.png')
save(draw([
    '................',
    '....aaaaaaaa....',
    '....aGGGGGGa....',
    '.....a....a.....',
    '.....as..sa.....',
    '......asssa.....',
    '.......ssa......',
    '........a.......',
    '.......asa......',
    '......a.s.a.....',
    '.....a..s..a....',
    '.....a.sss.a....',
    '....aGGGGGGa....',
    '....aaaaaaaa....',
    '................',
    '................'],
    {'a': hexc('3A2410'), 'G': hexc('E8C23A'), 's': hexc('5FD6B0')}), ITEM + 'pocket_hourglass.png')

# ----------------------------------------------------------------- armadura vestida
save(recolor(EQUIP + 'mithril.png', P['wind']), EQUIP + 'wind_boots.png')
save(recolor(EQUIP + 'mithril.png', P['frost']), EQUIP + 'frost_skates.png')
save(recolor(EQUIP + 'adamantine.png', P['tide']), EQUIP + 'tide_mask.png')
save(recolor(EQUIP + 'adamantine.png', P['ori']), EQUIP + 'beacon_crown.png')
save(recolor(EQUIP + 'orichalcum.png', P['shadow']), EQUIP + 'shadow_cloak.png')
print('ok')
