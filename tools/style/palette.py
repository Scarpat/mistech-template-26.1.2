"""Paleta central do Mistech — fonte única de cores para TODAS as texturas novas.

Regras do guia de estilo (ver doc "Mistech — Guia de estilo visual" no Project):
- Cada material é uma rampa de 5-6 tons, do mais escuro (índice 0, usado como contorno/sombra) ao mais claro (brilho).
- Luz vem de cima e da esquerda: topo/esquerda de cada forma usam tons +1, base/direita tons -1.
- Contorno no tom 0/1 do próprio material, nunca preto puro (preto só no interior de bocas/buracos).
- Sem ruído aleatório: toda variação é posta à mão (tijolos, veios, rebites).
- Máquinas: corpo pelo que a máquina faz (pedra = calor, madeira = mecânica, vidro = líquidos),
  acabamento (faixas, cantoneiras, rebites) e brilho mágico pelo TIER.
"""

def h(x):
    x = x.lstrip('#')
    return (int(x[0:2], 16), int(x[2:4], 16), int(x[4:6], 16), 255)

def ramp(*hexes):
    return [h(c) for c in hexes]

# --- materiais base (amostrados do vanilla quando existem) ---
PEDRA    = ramp('2e2d2e', '4a494a', '5f5e5f', '747374', '8a898a', 'a3a2a3')
ARDOSIA  = ramp('141416', '1f1f23', '2b2b30', '38383e', '4a4a51', '5e5e66')
ABETO    = ramp('2f1f10', '553a1f', '614b2e', '70522e', '82613a', '9a7748')
FERRO    = ramp('1c1c20', '2e2e33', '45454b', '5e5e64', '8a8a90', 'c5c5c8')
LAPIS_NOITE = ramp('080c26', '0e1640', '15205a', '1e2c74', '2a3d92', '3a54ae')

# --- metais ---
COBRE    = ramp('5a2b1c', '7d3f28', '9a5038', 'b26247', 'd67b5b', 'eb9a7a')
VERDETE  = ramp('2c4c40', '3f6e5a', '64a077', '7ab799', 'a6dcc0')
LATAO    = ramp('3d2a0e', '6b4e17', '9a7426', 'c49d3c', 'e2c25e', 'f6e39a')
OURO     = ramp('5a3a08', 'a06a10', 'cc8e27', 'f9bd23', 'ffd83e', 'fff28a')
MITRIL   = ramp('1c2c44', '34567a', '4c79a0', '6090b6', '92c1e0', 'ceebf8')
ADAMANTINA = ramp('2a0610', '590c1c', '871629', 'b72a3c', 'e66869', 'ffb4a4')
ORICALCO = ramp('3a140a', '9b4118', 'de7829', 'ec943c', 'ffc05f', 'ffebae')
CELESTE  = ramp('1e2268', '3454b0', '67a5eb', 'a9dcff', 'e8f6ff', 'ffffff')

# --- luz / magia ---
FOGO     = ramp('6a2a0a', 'c35d1b', 'ff8f00', 'ffd800', 'ffff97', 'ffffff')
AMETISTA = ramp('2a1648', '4a2a7e', '7a5bb5', 'a678f1', 'd2a8ff', 'f4e4ff')   # magia impura
PURA     = ramp('6a5420', 'b89a48', 'e8d48a', 'fff2c0', 'fffbe8', 'ffffff')   # magia pura
ARCANO   = ramp('0a2f3c', '0e5a6e', '1f8fa8', '4fd6e8', 'b8f6ff', 'ffffff')   # brilho do tier II
VAZIO    = (17, 17, 17, 255)   # interior de bocas e buracos
TRANSP   = (0, 0, 0, 0)

# --- tiers de máquina ---
TIERS = {
    'I':   dict(nome='Oficina', corpo=PEDRA,   acabamento=COBRE, rebite=LATAO,  magia=AMETISTA),
    'II':  dict(nome='Arcano',  corpo=ARDOSIA, acabamento=LATAO, rebite=MITRIL, magia=ARCANO),
    'III': dict(nome='Astral',  corpo=LAPIS_NOITE, acabamento=OURO, rebite=CELESTE, magia=CELESTE),
}


def paint(grid, legend):
    """Pinta uma textura a partir de uma grade de caracteres. legend: char -> cor RGBA."""
    from PIL import Image
    hgt = len(grid); w = len(grid[0])
    im = Image.new('RGBA', (w, hgt), TRANSP)
    for y, row in enumerate(grid):
        assert len(row) == w, f'linha {y} tem {len(row)} colunas: {row!r}'
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            im.putpixel((x, y), legend[ch])
    return im


def legend_for(corpo, acab, rebite, extra=None):
    """Legenda padrão: a-e = corpo (escuro->claro), 1-5 = acabamento, r/R = rebite claro/escuro, K = vazio."""
    L = {'z': corpo[0], 'a': corpo[1], 'b': corpo[2], 'c': corpo[3], 'd': corpo[4], 'e': corpo[5],
         '0': acab[0], '1': acab[1], '2': acab[2], '3': acab[3], '4': acab[4], '5': acab[5],
         'r': rebite[4], 'R': rebite[2], 'K': VAZIO}
    if extra:
        L.update(extra)
    return L
