"""Kit de desenho v2 do guia de estilo: volume à la Create/Thaumcraft.
Cada pixel guarda (rampa, tom); assim dá para sombrear/clarear sem sair da paleta.
Luz vem de cima-esquerda. Rampas de 7 tons: 0 = contorno, 6 = brilho especular."""
from PIL import Image

def _h(x): return (int(x[0:2], 16), int(x[2:4], 16), int(x[4:6], 16), 255)
def R(*hx): return [_h(c) for c in hx]

PEDRA   = R('1e1d1f', '333234', '48474a', '5c5b5e', '727174', '8a898c', 'a9a8ab')
ARDOSIA = R('0c0c0f', '16161a', '202025', '2c2c33', '3a3a43', '4c4c56', '64646f')
LAPIS   = R('060920', '0b1238', '111b50', '182669', '213385', '2e45a3', '4560c4')
COBRE   = R('3a1a10', '5a2b1c', '7d3f28', '9a5038', 'b8664a', 'd8805e', 'f2a882')
LATAO   = R('2e1f08', '553d10', '80601e', 'a8822e', 'c9a240', 'e6c562', 'fbe9a6')
OURO    = R('4a2e04', '7a500a', 'b07a16', 'd99e22', 'f5c22e', 'ffdc54', 'fff4a8')
MITRIL  = R('142238', '23405e', '34587c', '4c79a0', '6a9bc2', '98c4e2', 'd6effa')
CELESTE = R('1a1e5c', '2c48a0', '4f86d8', '7fb6f2', 'b0dcff', 'e2f3ff', 'ffffff')
FOGO    = R('4a1806', '8a3410', 'c35d1b', 'ff8f00', 'ffc020', 'ffe860', 'fffcc8')
AMETISTA= R('241040', '3e2270', '5e3a9e', '8560c8', 'ac84ec', 'd0b0ff', 'f2e4ff')
ARCANO  = R('06222c', '0b4252', '13697e', '1f97ad', '4fd2e2', '9ff0f6', 'effeff')
FERRO   = R('101013', '1c1c21', '2a2a30', '3a3a42', '55555e', '7c7c86', 'b4b4be')
VAZIO   = R('080808', '0e0e0e', '141414', '1a1a1a', '222222', '2a2a2a', '333333')


class Canvas:
    def __init__(self, w=16, h=16):
        self.w, self.h = w, h
        self.px = [[None] * w for _ in range(h)]

    def ok(self, x, y): return 0 <= x < self.w and 0 <= y < self.h

    def put(self, x, y, ramp, t):
        if self.ok(x, y): self.px[y][x] = [ramp, max(0, min(6, t))]

    def fill(self, x0, y0, x1, y1, ramp, t):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1): self.put(x, y, ramp, t)

    def shift(self, x, y, d):
        if self.ok(x, y) and self.px[y][x]:
            self.px[y][x][1] = max(0, min(6, self.px[y][x][1] + d))

    def shift_rect(self, x0, y0, x1, y1, d):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1): self.shift(x, y, d)

    def tonemap(self, x0, y0, rows, ramp):
        """rows: strings de dígitos 0-6 (tom) ou '.' (mantém)."""
        for j, r in enumerate(rows):
            for i, c in enumerate(r):
                if c != '.': self.put(x0 + i, y0 + j, ramp, int(c))

    def deltamap(self, x0, y0, rows):
        """'+'/'-' = ±1, '#'/'=' = ±2, '.' nada."""
        m = {'+': 1, '-': -1, '#': 2, '=': -2}
        for j, r in enumerate(rows):
            for i, c in enumerate(r):
                if c in m: self.shift(x0 + i, y0 + j, m[c])

    # ---- primitivas de volume ----
    def raised(self, x0, y0, x1, y1, ramp, base=3, outline=True):
        """Peça em relevo: miolo base, aresta de cima/esquerda clara, de baixo/direita escura."""
        self.fill(x0, y0, x1, y1, ramp, base)
        for x in range(x0, x1 + 1): self.put(x, y0, ramp, base + 2); self.put(x, y1, ramp, base - 2)
        for y in range(y0, y1 + 1): self.put(x0, y, ramp, base + 1); self.put(x1, y, ramp, base - 2)
        self.put(x0, y0, ramp, base + 3); self.put(x1, y1, ramp, base - 3)
        self.put(x1, y0, ramp, base); self.put(x0, y1, ramp, base - 1)

    def recess(self, x0, y0, x1, y1, depth=2):
        """Rebaixo: sombra na parede de cima/esquerda, luz na de baixo/direita."""
        for x in range(x0, x1 + 1): self.shift(x, y0, -depth)
        for y in range(y0 + 1, y1 + 1): self.shift(x0, y, -depth)
        if depth > 1:
            for x in range(x0 + 1, x1 + 1): self.shift(x, y0 + 1, -1)
        for x in range(x0 + 1, x1 + 1): self.shift(x, y1, +1)
        for y in range(y0 + 1, y1): self.shift(x1, y, +1)

    def bolt(self, x, y, ramp, shadow=True):
        """Parafuso 2x2 com brilho + sombra projetada embaixo/direita."""
        if shadow:
            self.shift(x + 2, y + 1, -1); self.shift(x + 1, y + 2, -1); self.shift(x + 2, y + 2, -2)
        self.put(x, y, ramp, 6); self.put(x + 1, y, ramp, 4)
        self.put(x, y + 1, ramp, 4); self.put(x + 1, y + 1, ramp, 1)

    def rivet(self, x, y, ramp):
        self.shift(x + 1, y, -1); self.shift(x, y + 1, -1)
        self.put(x, y, ramp, 6)

    def glow(self, x0, y0, x1, y1, ramp, cx, cy, radius=4, strength=2):
        """Luz espalhada (fogo, magia) tingindo o que está perto, por distância."""
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if not self.ok(x, y) or not self.px[y][x]: continue
                d = abs(x - cx) + abs(y - cy)
                if d <= radius:
                    old = self.px[y][x]
                    k = strength * (radius - d + 1) / (radius + 1)
                    self.px[y][x] = ['mix', (old, ramp, k)]

    def image(self):
        im = Image.new('RGBA', (self.w, self.h), (0, 0, 0, 0))
        for y in range(self.h):
            for x in range(self.w):
                p = self.px[y][x]
                if not p: continue
                im.putpixel((x, y), _color(p))
        return im


def _color(p):
    if p[0] == 'mix':
        old, ramp, k = p[1]
        c0 = _color(old); t = min(6, max(0, old[1] + int(k)))
        c1 = ramp[min(6, 2 + int(k * 1.5))]
        a = min(0.55, 0.18 * k)
        return tuple(int(c0[i] * (1 - a) + c1[i] * a) for i in range(3)) + (255,)
    return p[0][p[1]]


# ---------------- materiais (desenhados à mão: layout + manchas fixas) ----------------
def bricks(cv, x0, y0, x1, y1, ramp, course=4, width=8, offset=4, mottle=None):
    """Tijolos estilo Thaumcraft: rejunte fundo, aresta clara em cima/esquerda, escura embaixo/direita."""
    for y in range(y0, y1 + 1):
        row = (y - y0) // course
        ry = (y - y0) % course
        for x in range(x0, x1 + 1):
            if ry == course - 1:
                cv.put(x, y, ramp, 1); continue
            bx = (x - x0 + (offset if row % 2 else 0)) % width
            if bx == width - 1:
                cv.put(x, y, ramp, 1); continue
            t = 4
            if ry == 0: t = 5
            if bx == 0: t = 5
            if ry == 0 and bx == 0: t = 6
            if ry == course - 2: t = 3
            if bx == width - 2 and ry > 0: t = 3
            cv.put(x, y, ramp, t)
    if mottle: cv.deltamap(x0, y0, mottle)


def tiles(cv, x0, y0, x1, y1, ramp, size=4, mottle=None):
    """Ladrilhos (ardósia): cada ladrilho chanfrado, rejunte tom 0."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            ty = (y - y0) % size; tx = (x - x0 + ((y - y0) // size % 2) * (size // 2)) % size
            if ty == size - 1 or tx == size - 1: cv.put(x, y, ramp, 0); continue
            t = 3
            if ty == 0 or tx == 0: t = 4
            if ty == 0 and tx == 0: t = 5
            if ty == size - 2 and tx == size - 2: t = 2
            cv.put(x, y, ramp, t)
    if mottle: cv.deltamap(x0, y0, mottle)

# ---- rampas extras (v2.1) ----
ABETO   = R('24170a', '3b2814', '553a1f', '6b4c2a', '82613a', '9a7748', 'b8935e')
VERDETE = R('1c3a30', '2c5446', '3f6e5a', '4f8a6c', '64a077', '7ab799', 'a6dcc0')
GELO    = R('0e2a3a', '1e4a66', '3a7aa0', '6ab0d6', 'a0d8f0', 'd4f0fa', 'ffffff')
LINHO   = R('3a3220', '5e5236', '857650', 'a99a6c', 'c9bb8c', 'e2d6ac', 'f6efd2')
ADAMANTINA = R('2a0610', '590c1c', '871629', 'b72a3c', 'e66869', 'ffb4a4', 'ffe0d8')


def frame_box(cv, x0, y0, x1, y1, metal, bracket, band=4, post=2, brackets=True, rivets=True):
    """Moldura do tier dentro de uma caixa: faixas chanfradas em cima/embaixo, cantoneiras, suportes."""
    top = {4: [4, 5, 3, 1], 3: [5, 3, 1], 2: [5, 2]}[band]
    bot = {4: [0, 5, 3, 2], 3: [0, 4, 2], 2: [4, 1]}[band]
    for x in range(x0, x1 + 1):
        for i, t in enumerate(top): cv.put(x, y0 + i, metal, t)
        for i, t in enumerate(bot): cv.put(x, y1 - band + 1 + i, metal, t)
        cv.shift(x, y0 + band, -2); cv.shift(x, y0 + band + 1, -1)
    for y in range(y0 + band, y1 - band + 1):
        if post == 2:
            cv.put(x0, y, metal, 5); cv.put(x0 + 1, y, metal, 2); cv.put(x1 - 1, y, metal, 4); cv.put(x1, y, metal, 1)
        else:
            cv.put(x0, y, metal, 4); cv.put(x1, y, metal, 1)
        cv.shift(x0 + post, y, -1); cv.shift(x1 - post, y, -1)
    if brackets:
        s = 2 if band >= 3 else 1
        for (x, y) in ((x0, y0), (x1 - s, y0), (x0, y1 - s), (x1 - s, y1 - s)):
            cv.raised(x, y, x + s, y + s, bracket, base=3)
            if s == 2:
                cv.put(x + 1, y + 1, bracket, 6)
    if rivets and band >= 3:
        w = x1 - x0
        for x in (x0 + w // 3 + 1, x1 - w // 3 - 1):
            cv.rivet(x, y0 + 1, bracket); cv.rivet(x, y1 - band + 2, bracket)


def plates(cv, x0, y0, x1, y1, ramp, size=8, base=3):
    for py in range(y0, y1 + 1, size):
        for px in range(x0, x1 + 1, size):
            cv.raised(px, py, min(x1, px + size - 1), min(y1, py + size - 1), ramp, base=base)


def ring(cx, cy, r):
    """Pixels de um anel (círculo pixelado) — usado para escotilhas, rodas, runas."""
    pts = []
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if abs(d - r) < 0.5: pts.append((x, y))
    return pts


def disk(cx, cy, r):
    return [(x, y) for y in range(int(cy - r - 1), int(cy + r + 2)) for x in range(int(cx - r - 1), int(cx + r + 2))
            if ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5 <= r]


def strip(frames):
    """Empilha frames (Canvas) numa faixa vertical de animação."""
    ims = [f.image() for f in frames]
    out = Image.new('RGBA', (16, 16 * len(ims)), (0, 0, 0, 0))
    for i, im in enumerate(ims): out.paste(im, (0, 16 * i))
    return out
