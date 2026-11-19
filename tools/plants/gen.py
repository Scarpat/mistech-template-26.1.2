"""Plantas do Mistech: gera texturas, modelos e blockstates com um formato proprio por planta.
Uso (na raiz do projeto): python3 tools/plants/gen.py   -> escreve em src/main/resources/assets/mistech"""
import json, math
from PIL import Image

A = 'src/main/resources/assets/mistech/'
T, M, B = A + 'textures/block/', A + 'models/block/', A + 'blockstates/'


def hx(s):
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


class Canvas:
    """y = 0 e o chao; cresce para cima. Largura 16, altura 32 (2 blocos)."""
    def __init__(self, pal):
        self.px = {}
        self.pal = {k: hx(v) for k, v in pal.items()}

    def p(self, x, y, ch):
        if ch not in '. ' and 0 <= x < 16 and 0 <= y < 32:
            self.px[(int(x), int(y))] = ch

    def stamp(self, x, y, rows, flip=False, anchor_row=None):
        """rows de cima para baixo; (x, y) = pixel da coluna 0 na linha anchor_row (padrao: ultima)."""
        ar = len(rows) - 1 if anchor_row is None else anchor_row
        for r, row in enumerate(rows):
            for c, ch in enumerate(row):
                self.p(x - c if flip else x + c, y + (ar - r), ch)

    def tile(self, top=False):
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        off = 16 if top else 0
        for (x, y), ch in self.px.items():
            if off <= y < off + 16:
                im.putpixel((x, 15 - (y - off)), self.pal[ch])
        return im

    def height(self):
        return max((y for _, y in self.px), default=0) + 1


def save(im, name):
    im.save(T + name + '.png')


def plane_x(tex, cx, cz, w, y0, y1, uv, rescale=False, angle=45):
    """Par de planos cruzados (X) centrado em (cx, cz), meia-largura w."""
    rot = {'origin': [cx, y0, cz], 'axis': 'y', 'angle': angle}
    if rescale:
        rot['rescale'] = True
    return [
        {'from': [cx - w, y0, cz], 'to': [cx + w, y1, cz], 'rotation': dict(rot), 'shade': False,
         'faces': {'north': {'uv': uv, 'texture': tex}, 'south': {'uv': uv, 'texture': tex}}},
        {'from': [cx, y0, cz - w], 'to': [cx, y1, cz + w], 'rotation': dict(rot), 'shade': False,
         'faces': {'west': {'uv': uv, 'texture': tex}, 'east': {'uv': uv, 'texture': tex}}},
    ]


def plane_plus(tex, y0, y1, uv):
    """Par de planos em + passando pelo centro do bloco."""
    return plane_x(tex, 8, 8, 8, y0, y1, uv, angle=0)


def plane_ring(tex, inset, y0, y1):
    """Quatro planos em # (como a plantacao vanilla), afastados 'inset' da borda."""
    a, b = inset, 16 - inset
    f, r = [0, 0, 16, 16], [16, 0, 0, 16]
    return [
        {'from': [a, y0, 0], 'to': [a, y1, 16], 'shade': False, 'faces': {'west': {'uv': f, 'texture': tex}, 'east': {'uv': r, 'texture': tex}}},
        {'from': [b, y0, 0], 'to': [b, y1, 16], 'shade': False, 'faces': {'west': {'uv': r, 'texture': tex}, 'east': {'uv': f, 'texture': tex}}},
        {'from': [0, y0, a], 'to': [16, y1, a], 'shade': False, 'faces': {'north': {'uv': f, 'texture': tex}, 'south': {'uv': r, 'texture': tex}}},
        {'from': [0, y0, b], 'to': [16, y1, b], 'shade': False, 'faces': {'north': {'uv': r, 'texture': tex}, 'south': {'uv': f, 'texture': tex}}},
    ]


def clean(e):
    if e.get('rotation', {}).get('angle') == 0:
        del e['rotation']
    return e


def model(name, textures, elements):
    tx = {k: 'mistech:block/' + v for k, v in textures.items()}
    json.dump({'parent': 'minecraft:block/block', 'ambientocclusion': False, 'textures': tx, 'elements': [clean(e) for e in elements]},
              open(M + name + '.json', 'w'), indent=2)


def blockstate(name, n):
    json.dump({'variants': {f'age={i}': {'model': f'mistech:block/{name}_age{i}'} for i in range(n)}}, open(B + name + '.json', 'w'), indent=2)


FULL = [0, 0, 16, 16]

# ---------------------------------------------------------------- CANHAMO: haste alta (2 blocos), folhas em leque
HEMP = {'s': '3c6a2c', 'S': '4e7834', 'd': '2c5e26', 'm': '3c8034', 'l': '52a03e', 'h': '86c84e', 'b': 'aabe50', 'B': 'cdd670'}
H_BIG = ["..mlh.", ".mml.h", "Smmllh", ".dmml.", "..d.ml", "....d."]
H_MED = [".ml.", "Smlh", ".dm.", "..d."]
H_SML = [".l", "Sm", ".d"]
# por estagio: altura da haste e nos (y, tamanho); os lados alternam 1px para nao ficar espelhado
HEMP_ST = [
    (2, [(1, 's')]),
    (5, [(2, 's'), (4, 's')]),
    (8, [(3, 'm'), (6, 's')]),
    (12, [(3, 'm'), (7, 'm'), (10, 's')]),
    (16, [(4, 'b'), (9, 'm'), (13, 's')]),
    (21, [(4, 'b'), (9, 'b'), (14, 'm'), (18, 's')]),
    (25, [(5, 'b'), (10, 'b'), (15, 'b'), (19, 'm'), (22, 's')]),
    (29, [(5, 'b'), (10, 'b'), (15, 'b'), (20, 'm'), (23, 's')]),
]


def hemp():
    for i, (h, nodes) in enumerate(HEMP_ST):
        c = Canvas(HEMP)
        for y in range(h):
            if i >= 3:
                c.p(7, y, 's')
            c.p(8, y, 'S' if i >= 3 else 's')
        for k, (y, size) in enumerate(nodes):
            st = {'b': H_BIG, 'm': H_MED, 's': H_SML}[size]
            ar = {'b': 2, 'm': 1, 's': 1}[size]
            c.stamp(9, y + (k % 2), st, anchor_row=ar)
            c.stamp(6 if i >= 3 else 7, y + 1 - (k % 2), st, flip=True, anchor_row=ar)
        # ponta: tufo de folhas novas; madura = cachos amarelados
        c.p(8, h, 'l'); c.p(7, h, 'm' if i >= 3 else '.')
        if i == 6:
            for y in (h - 1, h - 3):
                c.p(9, y, 'b'); c.p(6, y - 1, 'b')
            c.p(8, h + 1, 'b')
        if i == 7:
            for y in range(h - 6, h + 1):
                c.p(7, y, 'b' if y % 2 else 'B'); c.p(8, y, 'B' if y % 2 else 'b')
            for y in (h - 5, h - 3, h - 1):
                c.p(9, y, 'b'); c.p(6, y, 'b')
            c.p(8, h + 1, 'B'); c.p(7, h + 1, 'b'); c.p(8, h + 2, 'b')
            for y in (21, 16, 11):
                c.p(9, y + 1, 'b'); c.p(6, y + 2, 'b')
        save(c.tile(), f'hemp_{i}')
        el = plane_x('#plant', 8, 8, 7.2, 0, 16, FULL, rescale=True)
        tx = {'particle': f'hemp_{i}', 'plant': f'hemp_{i}'}
        if c.height() > 16:
            save(c.tile(True), f'hemp_{i}_top')
            tx['top'] = f'hemp_{i}_top'
            el += plane_x('#top', 8, 8, 7.2, 16, 32, FULL, rescale=True)
        model(f'hemp_age{i}', tx, el)
    blockstate('hemp', 8)


# ---------------------------------------------------------------- TABACO: roseta de folhas largas + pendao de flores
TOB = {'s': '56803a', 'S': '6c9a40', 'd': '4e7a32', 'm': '68963c', 'l': '8cbe56', 'h': 'b4dc80', 'p': 'dc78aa', 'P': 'ec96be', 'w': 'fac8dc', 'g': 'a0c86a'}
T_HUGE = ["...mll.", "..mllhl", ".mmlllh", "Smmmll.", ".ddmm..", "..dd..."]
T_BIG = ["..mll.", ".mllhl", "Smmll.", ".ddm.."]
T_MED = [".mll", "Smlh", ".dm."]
T_SML = [".l", "Sm"]
TOB_ST = [
    (1, [(0, 's')]),
    (3, [(1, 'm')]),
    (5, [(1, 'b'), (4, 's')]),
    (8, [(1, 'b'), (5, 'm')]),
    (11, [(1, 'h'), (6, 'b'), (9, 's')]),
    (14, [(1, 'h'), (6, 'b'), (10, 'm'), (13, 's')]),
    (18, [(1, 'h'), (6, 'h'), (11, 'b'), (15, 'm')]),
    (21, [(1, 'h'), (6, 'h'), (11, 'b'), (15, 'm')]),
]
T_STAMP = {'h': (T_HUGE, 3), 'b': (T_BIG, 2), 'm': (T_MED, 1), 's': (T_SML, 1)}


def tobacco():
    for i, (h, nodes) in enumerate(TOB_ST):
        c = Canvas(TOB)
        low = Canvas(TOB)
        for y in range(h):
            if i >= 4:
                c.p(7, y, 's')
            c.p(8, y, 'S' if i >= 4 else 's')
        for k, (y, size) in enumerate(nodes):
            st, ar = T_STAMP[size]
            c.stamp(9, y + (k % 2), st, anchor_row=ar)
            c.stamp(6 if i >= 4 else 7, y + 1 - (k % 2), st, flip=True, anchor_row=ar)
        c.p(8, h, 'l')
        if i == 6:  # botoes
            c.p(8, h, 'g'); c.p(7, h, 'g'); c.p(8, h + 1, 'h'); c.p(9, h - 1, 'g'); c.p(6, h - 1, 'g')
        if i == 7:  # flores cor-de-rosa em trombeta
            c.stamp(6, h + 2, ["wP.Pw", "Pp.pP", ".pSp.", "..S.."], anchor_row=3)
            c.p(8, h + 3, 'P'); c.p(8, h + 4, 'w'); c.p(7, h + 3, 'p')
            c.p(10, h - 2, 'p'); c.p(11, h - 1, 'P'); c.p(5, h - 1, 'p'); c.p(4, h, 'P')
        save(c.tile(), f'tobacco_{i}')
        tx = {'particle': f'tobacco_{i}', 'plant': f'tobacco_{i}'}
        el = plane_x('#plant', 8, 8, 7.2, 0, 16, FULL, rescale=True)
        if c.height() > 16:
            save(c.tile(True), f'tobacco_{i}_top')
            tx['top'] = f'tobacco_{i}_top'
            el += plane_x('#top', 8, 8, 7.2, 16, 32, FULL, rescale=True)
        if i >= 3:  # saia de folhas baixas em +, para a planta ficar cheia vista de qualquer lado
            n = {3: 1, 4: 1, 5: 2, 6: 2, 7: 2}[i]
            big = T_BIG if i < 5 else T_HUGE
            ar = 2 if i < 5 else 3
            for k in range(n):
                y = 2 + k * 5
                low.stamp(9, y, big, anchor_row=ar); low.stamp(6, y + 1, big, flip=True, anchor_row=ar)
            save(low.tile(), f'tobacco_{i}_low')
            tx['low'] = f'tobacco_{i}_low'
            el += plane_plus('#low', 0, 16, FULL)
        model(f'tobacco_age{i}', tx, el)
    blockstate('tobacco', 8)


# ---------------------------------------------------------------- CAFE: arbusto lenhoso; os frutos amadurecem verde -> amarelo -> vermelho
COF = {'t': '5a3c22', 'T': '7a5230', 'd': '1c5426', 'm': '216c2d', 'l': '328840', 'h': '4c9758', 'g': '78aa46', 'G': 'aad26e',
       'y': 'd8a838', 'Y': 'f0cc58', 'r': 'b21e24', 'R': 'ec5a50'}
COF_ST = [(0, 0), (0, 0), (2.6, 2.4), (4.0, 3.2), (5.0, 4.2), (6.0, 5.0), (7.0, 5.8), (7.6, 6.4)]  # (rx, ry) da copa


def hsh(x, y, s=0):
    n = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def coffee():
    for i, (rx, ry) in enumerate(COF_ST):
        cy = ry + 2.2
        th = int(cy) if rx else (2, 4)[i]
        c = Canvas(COF)      # com tronco: planos em X
        f = Canvas(COF)      # so folhas: anel de planos em volta
        for y in range(th):
            c.p(8, y, 'T' if i >= 3 else 'm')
            if i >= 4:
                c.p(7, y, 't')
        if i == 0:
            c.stamp(9, 1, [".l", "mm"]); c.stamp(7, 2, [".l", "mm"], flip=True)
        elif i == 1:
            c.stamp(9, 2, [".ml", "mmh", "d.."], anchor_row=1); c.stamp(7, 3, [".ml", "mmh", "d.."], flip=True, anchor_row=1); c.p(8, 4, 'l'); c.p(8, 5, 'h')
        else:
            for y in range(32):
                for x in range(16):
                    dx, dy = (x - 7.5) / rx, (y - cy) / ry
                    r = dx * dx + dy * dy
                    if r > 1.0 or (r > 0.72 and hsh(x, y, 3) < 0.38):
                        continue
                    # folhas em "tijolinho": cada folha 2x2 com luz em cima a direita
                    bx = x + (1 if (y // 2) % 2 else 0)
                    top, right = y % 2 == 1, bx % 2 == 1
                    ch = 'l' if top and right else ('d' if not top and not right else 'm')
                    if top and right and hsh(bx // 2, y // 2, 7) < 0.3:
                        ch = 'h'
                    if dy < -0.55 and ch != 'd':
                        ch = 'd' if ch == 'm' else 'm'
                    if hsh(bx // 2, y // 2, 11) < 0.10 and r < 0.6:
                        ch = 'd'
                    c.p(x, y, ch); f.p(x, y, ch)
            if i >= 5:  # frutos em cachinhos: verdes, depois amarelando, depois vermelhos
                pair = {5: ('g', 'G'), 6: ('y', 'Y'), 7: ('r', 'R')}[i]
                spots = [(x, y) for y in range(2, 16, 2) for x in range(2, 14, 2)
                         if ((x - 7.5) / rx) ** 2 + ((y - cy) / ry) ** 2 < 0.7 and hsh(x // 2, y // 2, 23) < 0.24]
                for n, (x, y) in enumerate(spots):
                    a, b = pair
                    if i == 6 and n % 3 == 0:
                        a, b = 'g', 'G'
                    if i == 6 and n % 3 == 1:
                        a, b = 'r', 'R'
                    for cv in (c, f):
                        cv.p(x, y, a); cv.p(x + 1, y, a); cv.p(x, y + 1, a); cv.p(x + 1, y + 1, b)
        save(c.tile(), f'coffee_{i}')
        tx = {'particle': f'coffee_{i}', 'plant': f'coffee_{i}'}
        el = plane_x('#plant', 8, 8, 7.2, 0, 16, FULL, rescale=True)
        if i >= 3:
            save(f.tile(), f'coffee_{i}_leaves')
            tx['leaves'] = f'coffee_{i}_leaves'
            el += plane_ring('#leaves', {3: 6, 4: 5, 5: 4.5, 6: 4, 7: 3.5}[i], 0, 16)
        model(f'coffee_age{i}', tx, el)
    blockstate('coffee', 8)


# ---------------------------------------------------------------- ARRUDA: touceira de raminhos, um nasce depois do outro
RUE = {'s': '446a46', 'd': '4b714c', 'm': '6c9068', 'l': 'aac29a', 'h': 'c3d7af', 'y': 'f0d73c', 'Y': 'fff08a', 'o': 'c8a020'}


def rue_tuft(h, flower):
    c = Canvas(RUE)
    if h <= 3:
        c.p(7, 0, 's'); c.p(7, 1, 'm'); c.p(8, 1, 'l'); c.p(6, 2, 'l'); c.p(7, 2, 'h')
        if h == 3:
            c.p(8, 2, 'm'); c.p(9, 2, 'l')
        return c.tile()
    for y in range(int(h * 0.5)):
        c.p(7, y, 's')
    rx, ry = min(4.6, 1.6 + h * 0.2), h * 0.42
    cy = h - ry - 0.4
    for y in range(h):
        for x in range(16):
            dx, dy = (x - 7.5) / rx, (y - cy) / ry
            r = dx * dx + dy * dy
            # folhagem miuda e rendada: buracos na borda e alguns por dentro
            if r > 1.0 or (r > 0.55 and hsh(x, y, h) < 0.42) or hsh(x, y, h + 40) < 0.13:
                continue
            v = hsh(x // 2 + y, y // 2, 5)
            ch = 'h' if (dy > 0.15 and v < 0.3) else 'l' if v < 0.55 else 'm' if v < 0.85 else 'd'
            if dy < -0.5:
                ch = {'h': 'm', 'l': 'm', 'm': 'd', 'd': 'd'}[ch]
            c.p(x, y, ch)
    top = h - 1
    if flower == 1:  # botoes
        c.p(7, top, 'o'); c.p(8, top, 'y'); c.p(5, top - 2, 'o')
    if flower == 2:  # florzinhas amarelas no alto da moita
        c.stamp(7, top - 1, ["yY", "oy"])
        if h > 8:
            c.stamp(4, top - 4, ["yY", "oy"]); c.stamp(10, top - 3, ["Yy", "yo"])
        else:
            c.p(5, top - 2, 'y'); c.p(10, top - 2, 'Y')
    return c.tile()


# por estagio: alturas dos tres raminhos (0 = ainda nao nasceu)
RUE_ST = [(3, 0, 0), (5, 0, 0), (7, 3, 0), (9, 5, 0), (11, 7, 3), (13, 9, 5), (15, 11, 7), (16, 12, 9)]
RUE_POS = [(6.5, 9.5, 45), (11.5, 5, 22.5), (4, 4.5, 45)]


def rue():
    done = set()
    for i, hs in enumerate(RUE_ST):
        tx, el = {}, []
        for k, h in enumerate(hs):
            if not h:
                continue
            fl = 2 if i == 7 else (1 if i == 6 and k == 0 else 0)
            name = f'rue_tuft_{h}' + ('_bud' if fl == 1 else '_flower' if fl == 2 else '')
            if name not in done:
                save(rue_tuft(h, fl), name); done.add(name)
            tx[f't{k}'] = name
            cx, cz, ang = RUE_POS[k]
            el += plane_x(f'#t{k}', cx, cz, 5, 0, 16, [2.5, 0, 12.5, 16], angle=ang)
        tx['particle'] = tx['t0']
        model(f'rue_age{i}', tx, el)
    blockstate('rue', 8)


# ---------------------------------------------------------------- MANDRAGORA: roseta rente ao chao e a raiz (com cara) saindo da terra
MAN = {'d': '264a1e', 'm': '3c6a2a', 'l': '5a8c3a', 'h': '7aa850', 'v': '6a3a7a', 'V': '8f5cb0', 'f': 'b98ad8', 'F': 'e0c0f0', 's': '4a7a34',
       't': 'c8b078', 'T': 'dcc890', 'k': '2a1c10', 'r': 'a08a58', 'R': '86703f'}


def leaf(c, ang, length, purple=True):
    """Folha lanceolada saindo do centro, curvando para baixo na ponta."""
    a = math.radians(ang)
    for t in range(length * 2 + 1):
        s = t / 2.0
        x = 7.5 + math.cos(a) * s
        y = 0.5 + math.sin(a) * s - (s * s) * 0.035
        f = s / length
        w = 1 if f < 0.2 or f > 0.85 else 2
        for o in range(w):
            ch = 'l' if o == w - 1 and w > 1 else 'm'
            if f > 0.8 and w == 1:
                ch = 'h'
            c.p(round(x), round(y) + o, ch)
        if w > 1 and f < 0.5:
            c.p(round(x), round(y), 'd')
        if purple and 0.25 < f < 0.8 and t % 4 == 0:
            c.p(round(x), round(y), 'v')


MAN_LEAVES = [
    ([60, 120], 4),
    ([25, 65, 115, 155], 6),
    ([12, 40, 75, 105, 140, 168], 8),
    ([8, 34, 62, 118, 146, 172], 9),
]


def mandrake():
    # raiz: laterais com a cara (6x5) em (0,0); topo (6x6) em (0,8); lateral baixa do estagio 2 (6x2) em (8,0)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    pal = {k: hx(v) for k, v in MAN.items()}
    face = ["tTttTt", "tkttkt", "ttrrtt", "trkkrt", "rRkkRr"]
    for y, row in enumerate(face):
        for x, ch in enumerate(row):
            im.putpixel((x, y), pal[ch])
    for y, row in enumerate(["TtTttT", "trttrt"]):
        for x, ch in enumerate(row):
            im.putpixel((8 + x, y), pal[ch])
    for y, row in enumerate(["rttttr", "tTttTt", "ttddtt", "ttddtt", "tTttTt", "rttttr"]):
        for x, ch in enumerate(row):
            im.putpixel((x, 8 + y), pal[ch])
    save(im, 'mandrake_root')
    for i, (angs, ln) in enumerate(MAN_LEAVES):
        c = Canvas(MAN)
        for a in angs:
            leaf(c, a, ln, purple=i >= 1)
        if i == 3:  # hastes com flores roxas
            for x, top in ((8, 12), (5, 9), (11, 10)):
                for y in range(1, top):
                    c.p(x, y, 's')
                c.stamp(x - 1, top, [".F.", "fVf", ".f."], anchor_row=2)
        save(c.tile(), f'mandrake_{i}')
        tx = {'particle': f'mandrake_{i}', 'plant': f'mandrake_{i}'}
        base = {0: 0, 1: 0, 2: 2, 3: 5}[i]
        el = plane_x('#plant', 8, 8, 7.2, base, base + 16, FULL, rescale=True)
        if i >= 1:
            el += plane_plus('#plant', base, base + 16, FULL)
        if i >= 2:
            tx['root'] = 'mandrake_root'
            side = [0, 0, 6, 5] if i == 3 else [8, 0, 14, 2]
            el.append({'from': [5, 0, 5], 'to': [11, base, 11], 'faces': {
                **{d: {'uv': side, 'texture': '#root'} for d in ('north', 'south', 'east', 'west')},
                'up': {'uv': [0, 8, 6, 14], 'texture': '#root'}}})
        model(f'mandrake_age{i}', tx, el)
    blockstate('mandrake', 4)


if __name__ == '__main__':
    hemp(); tobacco(); coffee(); rue(); mandrake()
    print('ok')
