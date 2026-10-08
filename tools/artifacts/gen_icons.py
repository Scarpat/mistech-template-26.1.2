# Ícones dos Artefatos Rúnicos (16×16): contorno colorido, sombreado em 5 tons, luz de cima-esquerda.
# Uso (na pasta do projeto): python3 tools/artifacts/gen_icons.py [pasta_previa]
import sys, os, math
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
from pixelshade import shade, ramp, hexc

OUT = 'src/main/resources/assets/mistech/textures/item/'
PREV = sys.argv[1] if len(sys.argv) > 1 else None

# ------------------------------------------------------------------ paletas (contorno, escuro, médio, claro, brilho)
WIND = ramp('1F4A5A', '5E9FB2', 'A6DCE6', 'DDF5F8', 'FFFFFF')
FEATHER = ramp('3E6E86', 'B6D4E2', 'E2EFF5', 'F6FBFD', 'FFFFFF')
TEALD = ramp('0E2E38', '1E4F5E', '2F7086', '4A93A8', '7FC0D0')
GOLD = ramp('5A3204', 'A0640E', 'E0A41E', 'FFD54A', 'FFF6C0')
GOLDD = ramp('4A2A04', '7A4A0A', 'B07818', 'D8A030', 'F4CC70')
FROST = ramp('14245C', '2A4FA8', '3F7FD8', '7FB8F0', 'CFEAFF')
FUR = ramp('6E7F96', 'B4C4D6', 'DDE7F1', 'F4F8FC', 'FFFFFF')
STEEL = ramp('3C4450', '7A8696', 'B8C4D0', 'E4ECF2', 'FFFFFF')
TEAL = ramp('0A3238', '136B73', '1F9E9E', '52D3C6', 'C8FFF6')
AQUA = ramp('0E4A50', '2AA8A0', '5CDCCB', '9EF2E4', 'E6FFFA')
PEARL = ramp('8A9AA0', 'D8E4E6', 'F2F8F8', 'FFFFFF', 'FFFFFF')
CLOAK = ramp('120A1E', '2A1844', '422A66', '5E3E8C', '8462B4')
HOOD = ramp('06030A', '0B0612', '140A24', '1E1034', '2A1844')
LILAC = ramp('3A2060', '7A50B8', 'B08CE0', 'D8C4F6', 'FFFFFF')
EYE = ramp('B28CE0', 'D8C4F6', 'F0E4FF', 'FFFFFF', 'FFFFFF')
BASALT = ramp('140F0E', '2A2220', '3E3431', '574A45', '74645C')
MAGMA = ramp('6A1A06', 'D8480E', 'FF8A1E', 'FFC040', 'FFEE9A')
ADAM = ramp('3A0A12', '7A1A28', 'B02E40', 'E2566A', 'FFB0BA')
WOOD = ramp('2E1A0E', '5A3A20', '7E5430', 'A47444', 'C89868')
BIRCH = ramp('5A5048', 'B8AE9C', 'DCD4C2', 'F0ECE0', 'FFFFFF')
STRAW = ramp('5A3E0E', '9A7420', 'C99A3A', 'E8C46A', 'FFF0B0')
CLOUD = ramp('6A82A8', 'A8BCDA', 'D6E2F2', 'F2F7FD', 'FFFFFF')
VOID = ramp('12081F', '2E1652', '4E2A88', '7A4CC4', 'B48AF0')
VOIDTIP = ramp('3A2060', '9A78D8', 'D2BEF8', 'EEE4FF', 'FFFFFF')
CYAN = ramp('0E4A5A', '1E8EA8', '46D2E8', 'A6F4FF', 'FFFFFF')
SKY = ramp('1C3550', '3A6A96', '6AA0D0', 'A4CCEE', 'E4F4FF')
SKYD = ramp('14243A', '284A6E', '3E6A96', '5A8AB8', '86B0D8')
BOLT = ramp('7A5A00', 'D0A010', 'F8D838', 'FFF080', 'FFFFFF')
GLASS = ramp('4A6A78', '9CC6D4', 'C8EAF2', 'E6F8FB', 'FFFFFF')
SAND = ramp('7A5A20', 'B8904A', 'E0BA6A', 'F4D68E', 'FFF0C0')
ORI = ramp('5A2E08', 'A85E14', 'E8922A', 'FFC060', 'FFEBB0')

icons = {}


def save(name, im):
    im.save(OUT + name + '.png')
    icons[name] = im


# ------------------------------------------------------------------ utilitários para peças na diagonal
def rot_mask(ch, c, s_rng, w_fn, base=None):
    """Retângulo girado 45°: s = posição ao longo de (1,-1) (rumo ao canto de cima-direita), w = largura ao longo de (1,1)."""
    g = base or [['.'] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dx, dy = x - c[0], y - c[1]
            s = (dx - dy) / math.sqrt(2)
            w = (dx + dy) / math.sqrt(2)
            if s_rng[0] <= s <= s_rng[1] and abs(w) <= w_fn(s):
                g[y][x] = ch
    return g


def rows(g):
    return [''.join(r) for r in g]


def stick(x0, y0, x1, colors, outline):
    """Cabo diagonal (sobe para a direita) de x0 a x1 a partir de (x0,y0): 2 px (claro/escuro) + contorno por fora."""
    im = Image.new('RGBA', (16, 16))
    for i, x in enumerate(range(x0, x1 + 1)):
        y = y0 - (x - x0)
        light, dark = colors(i)
        if 0 <= y < 16:
            im.putpixel((x, y), light)
            if x + 1 < 16:
                im.putpixel((x + 1, y), dark)
    return outer_outline(im, outline)


def outer_outline(im, color):
    out = im.copy()
    for y in range(16):
        for x in range(16):
            if im.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and im.getpixel((nx, ny))[3] > 0:
                        out.putpixel((x, y), color)
                        break
    return out


def over(base, top):
    b = base.copy()
    b.alpha_composite(top)
    return b


# ------------------------------------------------------------------ Botas do Vento
save('wind_boots', shade([
    '................',
    '......GGGGGG....',
    '......GGGGGG....',
    'F.....WWWWWW....',
    'FF....WWWWWW....',
    'FFF...WWWWWW....',
    '.FFFF.WWWWWW....',
    'FFFFFFWWWWWW....',
    '.FFFFFWWWWWWW...',
    '..FFFFWWWWWWWWW.',
    '...FFFWWWWWWWWWW',
    '......WWWWWWWWWW',
    '......WWWWWWWWWW',
    '......SSSSSSSSSS',
    '.......SSSSSSSSS',
    '................'],
    {'W': WIND, 'G': GOLD, 'F': FEATHER, 'S': TEALD}, over=[
    '................',
    '................',
    '.......h........',
    '................',
    '.o..............',
    '..o.............',
    '................',
    '.ooo............',
    '................',
    '..oo.....h......',
    '..........d.d...',
    '.........d.d.d..',
    '................',
    '.......l........',
    '................',
    '................']))

# ------------------------------------------------------------------ Patins Gélidos
save('frost_skates', shade([
    '................',
    '....UUUUUUU.....',
    '...UUUUUUUUU....',
    '....BBBBBBB.....',
    '....BBBBBBB.....',
    '....BBBBBBB.....',
    '....BBBBBBB.....',
    '....BBBBBBBBB...',
    '....BBBBBBBBBBB.',
    '....BBBBBBBBBBBB',
    '....BBBBBBBBBBBB',
    '....BBBBBBBBBBBB',
    '..L..LL....LL...',
    '..LLLLLLLLLLLLL.',
    '...LLLLLLLLLLLL.',
    '................'],
    {'B': FROST, 'U': FUR, 'L': STEEL}, over=[
    '................',
    '.....h..........',
    '.......d..d.....',
    '................',
    '......h.........',
    '.....hlh........',
    '......h.........',
    '..........d.....',
    '...........d.d..',
    '............d...',
    '................',
    '................',
    '................',
    '...lllllllllll..',
    '................',
    '................']))

# ------------------------------------------------------------------ Máscara das Marés
save('tide_mask', shade([
    '................',
    '.......AA.......',
    '......AAAA......',
    '.....AAAAAA.....',
    '..TTTTAAAATTTT..',
    '.TTTTTTPPTTTTTT.',
    'TTTTTTTPPTTTTTTT',
    'TTTTTTTTTTTTTTTT',
    'TTT...TTTT...TTT',
    'TTT...TTTT...TTT',
    'TTTT.TTTTTT.TTTT',
    '.TTTTTTTTTTTTTT.',
    '..TTTTT..TTTTT..',
    '...TTT....TTT...',
    '................',
    '................'],
    {'T': TEAL, 'A': AQUA, 'P': PEARL}, over=[
    '................',
    '................',
    '.......h........',
    '................',
    '................',
    '..h.............',
    '................',
    '.d............d.',
    '................',
    '................',
    '................',
    '..d..........d..',
    '................',
    '................',
    '................',
    '................']))

# ------------------------------------------------------------------ Manto das Sombras
save('shadow_cloak', shade([
    '......CCCC......',
    '.....CCCCCC.....',
    '....CCCCCCCC....',
    '....CCKKKKCC....',
    '...CCKKKKKKCC...',
    '...CCKEKKEKCC...',
    '..CCCCKKKKCCCC..',
    '..CCCCCGGCCCCC..',
    '..CCCCCLLCCCCC..',
    '.CCCCCCKKCCCCCC.',
    '.CCCCCCKKCCCCCC.',
    '.CCCCCCKKCCCCCC.',
    'CCCCCCCKKCCCCCCC',
    'CCCCCCKKKKCCCCCC',
    'LLLLLLLLLLLLLLLL',
    '................'],
    {'C': CLOAK, 'K': HOOD, 'L': LILAC, 'G': CYAN, 'E': EYE}, flat=('K', 'E'), over=[
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '.......h........',
    '................',
    '...d........l...',
    '...d........l...',
    '..d..........l..',
    '..d..........l..',
    '.d............l.',
    'm.m.m.m.m.m.m.m.',
    '................']))

# ------------------------------------------------------------------ Marreta Vulcânica
def hammer():
    handle = stick(1, 14, 7, lambda i: (ADAM[3], ADAM[1]) if i % 3 else (GOLD[3], GOLD[1]), ADAM[0])
    g = rot_mask('B', (10.5, 5), (-2.9, 2.9), lambda s: 6.3)
    for y in range(16):
        for x in range(16):
            if g[y][x] == 'B':
                dx, dy = x - 10.5, y - 5
                w = (dx + dy) / math.sqrt(2)
                if abs(w) >= 4.9:
                    g[y][x] = 'R'
    head = shade(rows(g), {'B': BASALT, 'R': ADAM})
    # rachaduras de magma
    for (x, y) in ((9, 4), (10, 4), (10, 5), (11, 6), (12, 3), (12, 4), (8, 2), (13, 7), (11, 3)):
        if head.getpixel((x, y))[3] and head.getpixel((x, y)) != BASALT[0]:
            head.putpixel((x, y), MAGMA[3] if (x + y) % 2 else MAGMA[1])
    head.putpixel((11, 4), MAGMA[4])
    return over(handle, head)


save('volcanic_hammer', hammer())

# ------------------------------------------------------------------ Lança das Marés
def spear():
    shaft = stick(0, 15, 8, lambda i: (TEAL[3], TEAL[1]) if i in (3, 4) else (ORI[3], ORI[1]), ORI[0])
    im = Image.new('RGBA', (16, 16))
    P = im.putpixel
    for d in range(-2, 3):  # travessa ao longo de (1,1), centrada em (10,5)
        P((10 + d, 5 + d), GOLD[3] if d < 0 else GOLD[2])
    for (sx, sy), n in (((10, 5), 5), ((8, 3), 3), ((12, 7), 3)):
        for i in range(1, n + 1):
            x, y = sx + i, sy - i
            if 0 <= x < 16 and 0 <= y < 16:
                P((x, y), AQUA[4] if i == n else AQUA[3] if i > 1 else AQUA[2])
    head = outer_outline(im, TEAL[0])
    head.putpixel((10, 5), PEARL[2])
    return over(shaft, head)


save('tide_spear', spear())

# ------------------------------------------------------------------ Anel Espacial
save('spatial_ring', shade([
    '................',
    '.......VV.......',
    '......VVVV......',
    '.....VVVVVV.....',
    '......VVVV......',
    '....OOOVVOOO....',
    '...OOOOOOOOOO...',
    '..OOOO....OOOO..',
    '..OOO......OOO..',
    '..OOO......OOO..',
    '..OOO......OOO..',
    '..OOOO....OOOO..',
    '...OOOOOOOOOO...',
    '....OOOOOOOO....',
    '................',
    '................'],
    {'O': ORI, 'V': VOID}, over=[
    '................',
    '................',
    '.......h........',
    '......h.........',
    '................',
    '.....l..........',
    '....l...........',
    '...l............',
    '...l............',
    '...l............',
    '................',
    '................',
    '..........d.....',
    '................',
    '................',
    '................']))

# ------------------------------------------------------------------ Manopla da Tempestade
save('storm_gauntlet', shade([
    '...SS.SSS.SS....',
    '..SSSSSSSSSSS...',
    '..SSSSSSSSSSS...',
    '..SSSSSSSSSSSS..',
    'SS.SSSSSSSSSSS..',
    'SSSSSSSSSSSSSS..',
    '.SSSSSSSSSSSSS..',
    '..SSSSSBBSSSSS..',
    '...SSSSBBSSSSS..',
    '...SSSSSSSSSSS..',
    '..YYYYYYYYYYYY..',
    '..YYYYYYYYYYYY..',
    '...CCCCCCCCCC...',
    '...CCCCCCCCCC...',
    '..CCCCCCCCCCCC..',
    '..CCCCCCCCCCCC..'],
    {'S': SKY, 'B': BOLT, 'Y': GOLD, 'C': SKYD}, over=[
    '................',
    '.....o..o..o....',
    '.....o..o..o....',
    '.....o..o..o....',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '.....h..........',
    '................',
    '......l...l.....',
    '.....l...l......',
    '................']))

# ------------------------------------------------------------------ Coroa do Farol
save('beacon_crown', shade([
    '................',
    '................',
    '..C....CC....C..',
    '..G...GGGG...G..',
    '.GGG..GGGG..GGG.',
    '.GGGGGGGGGGGGGG.',
    '.GGGGGGGGGGGGGG.',
    '.GGGGGGCCGGGGGG.',
    '.GGGGGGCCGGGGGG.',
    '.GGGGGGGGGGGGGG.',
    '.BBBBBBBBBBBBBB.',
    '.BBBBBBBBBBBBBB.',
    '.BBBBBBBBBBBBBB.',
    '................',
    '................',
    '................'],
    {'G': GOLD, 'B': GOLDD, 'C': CYAN}, over=[
    '................',
    '................',
    '..h....hm....h..',
    '................',
    '................',
    '................',
    '................',
    '.......h........',
    '................',
    '................',
    '................',
    '...h...l..h.....',
    '................',
    '................',
    '................',
    '................']))

# ------------------------------------------------------------------ Ímã do Vazio
save('void_magnet', shade([
    '................',
    '..TTTT....TTTT..',
    '..TTTT....TTTT..',
    '..TTTT....TTTT..',
    '..VVVV....VVVV..',
    '..VVVV....VVVV..',
    '..VVVV....VVVV..',
    '..VVVV....VVVV..',
    '..VVVV....VVVV..',
    '..VVVVV..VVVVV..',
    '..VVVVVVVVVVVV..',
    '..VVVVVVVVVVVV..',
    '...VVVVVVVVVV...',
    '....VVVVVVVV....',
    '................',
    '................'],
    {'V': VOID, 'T': VOIDTIP}, over=[
    '.......*........',
    '................',
    '........*.......',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................']))

# ------------------------------------------------------------------ Ampulheta de Bolso
save('pocket_hourglass', shade([
    '...FFFFFFFFFF...',
    '...FFFFFFFFFF...',
    '....P.GGGG.P....',
    '....PGGGGGGP....',
    '....PGGGGGGP....',
    '....P.SSSS.P....',
    '....P..SS..P....',
    '....P..SS..P....',
    '....P..GG..P....',
    '....P.GSSG.P....',
    '....PGSSSSGP....',
    '....PSSSSSSP....',
    '...FFFFFFFFFF...',
    '...FFFFFFFFFF...',
    '................',
    '................'],
    {'F': GOLD, 'P': WOOD, 'G': GLASS, 'S': SAND}, over=[
    '................',
    '................',
    '................',
    '......h.........',
    '................',
    '................',
    '................',
    '........l.......',
    '........m.......',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................']))

# ------------------------------------------------------------------ Vassouras
def broom(cloud):
    from PIL import ImageDraw
    wood = BIRCH if cloud else WOOD
    handle = stick(8, 7, 14, lambda i: (wood[3], wood[1]), wood[0])
    m = Image.new('L', (16, 16), 0)
    d = ImageDraw.Draw(m)
    d.polygon([(8, 7), (0, 8), (0, 12), (4, 15), (8, 15)], fill=1)       # leque da palha
    g = [['S' if m.getpixel((x, y)) else '.' for x in range(16)] for y in range(16)]
    g = rot_mask('Y', (8.2, 7.6), (-0.9, 0.9), lambda s: 2.0, base=g)  # amarração
    br = CLOUD if cloud else STRAW
    band = ORI if cloud else ramp('3A2410', '6B4A2A', '8A6238', 'A87C4A', 'C8A070')
    head = shade(rows(g), {'S': br, 'Y': band})
    for y in range(16):  # fios na direção do cabo
        for x in range(16):
            px = head.getpixel((x, y))
            if px[3] and g[y][x] == 'S' and px != br[0] and (x + y) % 3 == 0:
                head.putpixel((x, y), br[1])
    for (x, y) in ((0, 8), (0, 12), (4, 15), (8, 15)):  # pontas soltas
        head.putpixel((x, y), (0, 0, 0, 0))
    im = over(handle, head)
    if cloud:
        im.putpixel((15, 0), CYAN[3])
        im.putpixel((14, 0), CYAN[1])
        im.putpixel((15, 1), CYAN[1])
    return im


save('broom', broom(False))
save('cloud_broom', broom(True))

if PREV:
    names = list(icons)
    sheet = Image.new('RGBA', (len(names) * 112, 112), (58, 62, 70, 255))
    for i, n in enumerate(names):
        sheet.alpha_composite(icons[n].resize((96, 96), Image.NEAREST), (i * 112 + 8, 8))
    sheet.save(PREV + '/icons2.png')
print('ícones ok')
