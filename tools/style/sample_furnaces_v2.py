"""Amostra v2 (mais volume): Fornalha Rúnica (I) -> Fornalha Arcana (II) -> Forja Estelar (III).
Uso: python sample_furnaces_v2.py <pasta_saida>"""
import os, sys
from kit import *

STONE_MOTTLE = [
    "................",
    "....-.....+.....",
    "..+.....-....-..",
    "................",
    ".-...+.....-....",
    "......-.+.......",
    "..+.........+...",
    "................",
    "....+..-....-...",
    ".-.......+......",
    ".....-.......+..",
    "................",
    "..-...+...-.....",
    "......-.....+...",
    "...+.......-....",
    "................",
]
TILE_MOTTLE = [
    "........+.......",
    ".-..........-...",
    "......-.........",
    "................",
    "...+.......-....",
    "..........+.....",
    ".-....-.........",
    "................",
    "........-...+...",
    ".+..............",
    "......+....-....",
    "................",
    "..-.........+...",
    ".......-........",
    "....+...........",
    "................",
]


def frame(cv, metal, bracket, top=True):
    """Moldura do tier: faixas chanfradas em cima/embaixo, cantoneiras, suportes com parafuso."""
    for x in range(16):
        cv.put(x, 0, metal, 4); cv.put(x, 1, metal, 5); cv.put(x, 2, metal, 3); cv.put(x, 3, metal, 1)
        cv.shift(x, 4, -2); cv.shift(x, 5, -1)
        cv.put(x, 12, metal, 0); cv.put(x, 13, metal, 5); cv.put(x, 14, metal, 3); cv.put(x, 15, metal, 2)
    for y in range(4, 12):
        cv.put(0, y, metal, 5); cv.put(1, y, metal, 2); cv.shift(2, y, -1)
        cv.put(14, y, metal, 4); cv.put(15, y, metal, 1); cv.shift(13, y, -1)
    # desgaste/brilhos postos à mão
    cv.deltamap(0, 0, ["....+......-....", "..........#.....", ".....-..........", "................"])
    cv.deltamap(0, 12, ["................", ".......#....-...", "...-............", "........+......."])
    for (x, y) in ((0, 0), (13, 0), (0, 13), (13, 13)):
        cv.raised(x, y, x + 2, y + 2, bracket, base=3)
        cv.put(x + 1, y + 1, bracket, 6); cv.shift(x + 3, y + 1, -1); cv.shift(x + 1, y + 3, -1)
    for x in (6, 9):
        cv.rivet(x, 1, bracket); cv.rivet(x, 13, bracket)


def top_frame(cv, metal, bracket):
    for i in range(16):
        cv.put(i, 0, metal, 4); cv.put(i, 1, metal, 5); cv.put(i, 15, metal, 1); cv.put(i, 14, metal, 2)
        cv.put(0, i, metal, 5); cv.put(1, i, metal, 4); cv.put(15, i, metal, 1); cv.put(14, i, metal, 2)
    cv.put(0, 0, metal, 6); cv.put(15, 15, metal, 0)
    for i in range(2, 14):
        cv.shift(i, 2, -2); cv.shift(2, i, -2)   # sombra da moldura no tampo
    for (x, y) in ((0, 0), (13, 0), (0, 13), (13, 13)):
        cv.raised(x, y, x + 2, y + 2, bracket, base=3)
        cv.put(x + 1, y + 1, bracket, 6); cv.shift(x + 3, y + 1, -1); cv.shift(x + 1, y + 3, -1)


def gem(cv, x, y, ramp, lit):
    b = 4 if lit else 2
    cv.put(x, y, ramp, b + 2); cv.put(x + 1, y, ramp, b + 1)
    cv.put(x, y + 1, ramp, b); cv.put(x + 1, y + 1, ramp, b - 1)


FIRE = [".3...3..", "343.3543", "45654565"]


def furnace_front(body_fn, metal, bracket, plate, gem_ramp, fire_ramp, lit, mouth_metal=None):
    cv = Canvas()
    body_fn(cv)
    frame(cv, metal, bracket)
    mm = mouth_metal or metal
    # plaquinha com gema pendurada na faixa de cima
    cv.raised(6, 3, 9, 6, plate, base=3)
    cv.recess(7, 4, 8, 5, depth=1)
    gem(cv, 7, 4, gem_ramp, lit)
    cv.shift(6, 7, -1); cv.shift(7, 7, -2); cv.shift(8, 7, -2); cv.shift(9, 7, -2); cv.shift(10, 7, -1)
    # boca: moldura em relevo + interior rebaixado
    cv.raised(3, 7, 12, 11, mm, base=3)
    cv.fill(4, 8, 11, 10, VAZIO, 2)
    cv.recess(4, 8, 11, 10, depth=2)
    for x in range(4, 12):
        cv.put(x, 11, mm, 2)   # soleira
    if lit:
        cv.tonemap(4, 8, FIRE, fire_ramp)
        cv.glow(2, 6, 13, 12, fire_ramp, 7, 10, radius=5, strength=2)
        cv.put(7, 4, gem_ramp, 6)
    return cv


def side_t1():
    cv = Canvas()
    bricks(cv, 0, 0, 15, 15, PEDRA, mottle=STONE_MOTTLE)
    frame(cv, COBRE, LATAO)
    # respiradouro de cobre aparafusado
    cv.raised(4, 5, 11, 10, COBRE, base=3)
    for y in (6, 8):
        for x in range(5, 11): cv.put(x, y, VAZIO, 2)
        for x in range(5, 11): cv.put(x, y + 1, COBRE, 4)
    cv.put(5, 6, VAZIO, 1); cv.put(5, 8, VAZIO, 1)
    for x in range(5, 13): cv.shift(x, 11, -2)
    for y in range(6, 11): cv.shift(12, y, -2)
    return cv


def top_t1():
    cv = Canvas()
    bricks(cv, 0, 0, 15, 15, PEDRA, course=5, width=7, offset=3, mottle=STONE_MOTTLE)
    top_frame(cv, COBRE, LATAO)
    cv.raised(4, 4, 11, 11, COBRE, base=3)
    for (x, y) in ((5, 5), (9, 5), (5, 9), (9, 9)):
        cv.fill(x, y, x + 1, y + 1, VAZIO, 3); cv.put(x, y, VAZIO, 1); cv.put(x + 1, y, VAZIO, 1); cv.put(x, y + 1, VAZIO, 1)
    for i in range(5, 11): cv.put(i, 7, COBRE, 4); cv.put(i, 8, COBRE, 2); cv.put(7, i, COBRE, 4); cv.put(8, i, COBRE, 2)
    cv.put(7, 7, COBRE, 5); cv.put(8, 8, COBRE, 2)
    for i in range(5, 12): cv.shift(i, 12, -2); cv.shift(12, i, -2)
    return cv


def side_t2():
    cv = Canvas()
    tiles(cv, 0, 0, 15, 15, ARDOSIA, mottle=TILE_MOTTLE)
    frame(cv, LATAO, MITRIL)
    # manômetro de latão com mostrador de mitril
    ring = [".3443.", "35..32", "4....1", "4....1", "32..21", ".2111."]
    cv.tonemap(5, 4, ring, LATAO)
    cv.tonemap(6, 5, ["65", "5554"], MITRIL); cv.tonemap(6, 7, ["5443", ".33."], MITRIL)
    cv.put(8, 6, ADAMANTINA_ if False else MITRIL, 1)
    cv.tonemap(8, 5, ["3"], R('2a0610', '590c1c', '871629', 'b72a3c', 'e66869', 'ffb4a4', 'ffe0d8'))
    cv.tonemap(7, 6, ["3"], R('2a0610', '590c1c', '871629', 'b72a3c', 'e66869', 'ffb4a4', 'ffe0d8'))
    for x in range(6, 12): cv.shift(x, 10, -1)
    for y in range(5, 10): cv.shift(11, y, -1)
    return cv


def top_t2():
    cv = Canvas()
    tiles(cv, 0, 0, 15, 15, ARDOSIA, mottle=TILE_MOTTLE)
    top_frame(cv, LATAO, MITRIL)
    cv.raised(4, 4, 11, 11, LATAO, base=3)
    cv.tonemap(5, 5, ["234432", "345543", "456654", "456654", "345543", "234432"], ARCANO)
    cv.recess(5, 5, 10, 10, depth=1)
    cv.put(6, 6, ARCANO, 6)
    for i in range(5, 12): cv.shift(i, 12, -2); cv.shift(12, i, -2)
    return cv


# ---------------- Tier III: Forja Estelar (modelo próprio) ----------------
def lapis_field(cv, x0, y0, x1, y1, stars):
    cv.fill(x0, y0, x1, y1, LAPIS, 3)
    cv.deltamap(x0, y0, ["-..+....-..+....", "..-....+.....-..", "+....-....+.....", "...-.....-....+.", ".+....+.....-...",
                         "....-....+.....-", "-....+.....-....", "..+....-....+...", ".....-..+.....-.", "-.+.......-.....",
                         "....+..-....+...", ".-.....+....-...", "...-.+.....+....", "+......-.-......", "..-.+........+..", "....-...+....-.."])
    for (x, y) in stars:
        cv.put(x, y, CELESTE, 6); cv.shift(x + 1, y, +1); cv.shift(x - 1, y, +1); cv.shift(x, y - 1, +1); cv.shift(x, y + 1, +1)


def star_side():
    cv = Canvas()
    lapis_field(cv, 0, 0, 15, 15, [(5, 7), (10, 9), (8, 6)])
    cv.shift_rect(2, 5, 13, 5, -2)                       # sombra do tampo
    for y in range(6, 11): cv.put(2, y, OURO, 4); cv.put(13, y, OURO, 1)
    for x in range(2, 14): cv.put(x, 11, OURO, 2)
    cv.put(2, 11, OURO, 3)
    # base
    for x in range(16):
        cv.put(x, 12, OURO, 5); cv.put(x, 13, LAPIS, 4); cv.put(x, 14, LAPIS, 3); cv.put(x, 15, LAPIS, 1)
    cv.deltamap(0, 12, ["...+.......#....", ".-....+....-..-.", "....-....+......", "................"])
    for x in (2, 13):
        cv.bolt(x - 1, 13, CELESTE, shadow=False)
    return cv


def star_front(lit):
    cv = star_side()
    for y in range(6, 11):
        for x in range(3, 13): cv.put(x, y, LAPIS, 3)
    cv.raised(4, 6, 11, 10, OURO, base=3)
    cv.fill(5, 7, 10, 9, VAZIO, 2); cv.recess(5, 7, 10, 9, depth=2)
    if lit:
        cv.tonemap(5, 7, ["3.43.4", "454454", "566565"], CELESTE)
        cv.glow(3, 5, 12, 11, CELESTE, 7, 9, radius=4, strength=2)
    return cv


def star_top():
    cv = Canvas()
    lapis_field(cv, 0, 0, 15, 15, [(3, 3), (12, 4), (4, 12), (11, 11)])
    for i in range(16):
        cv.put(i, 0, OURO, 5); cv.put(0, i, OURO, 5); cv.put(i, 15, OURO, 1); cv.put(15, i, OURO, 1)
        cv.put(i, 1, OURO, 3); cv.put(1, i, OURO, 4); cv.put(i, 14, OURO, 2); cv.put(14, i, OURO, 2)
    for i in range(2, 14): cv.shift(i, 2, -2); cv.shift(2, i, -2)
    # anel de ouro embutido (canal com sombra embaixo)
    ring = [(6, 3), (7, 3), (8, 3), (9, 3), (5, 4), (10, 4), (4, 5), (11, 5), (4, 6), (11, 6), (4, 7), (11, 7), (4, 8), (11, 8),
            (4, 9), (11, 9), (5, 10), (10, 10), (6, 11), (7, 11), (8, 11), (9, 11)]
    for (x, y) in ring:
        t = 5 if (y < 7 and x < 8) else 4 if y < 8 else 3
        cv.put(x, y, OURO, t)
    for (x, y) in ring:
        if (x, y + 1) not in ring: cv.shift(x, y + 1, -1)
    for (x, y) in ((7, 5), (9, 7)): cv.put(x, y, CELESTE, 5)
    return cv


def star_trim():
    cv = Canvas()
    cv.fill(0, 0, 15, 15, OURO, 3)
    for x in range(16):
        cv.put(x, 2, OURO, 5); cv.put(x, 3, OURO, 5); cv.put(x, 4, OURO, 2)
    cv.deltamap(0, 2, ["..+.....#...-...", "....-......+....", ".......-........"])
    for x in (3, 8, 12):
        cv.rivet(x, 3, CELESTE)
    cv.raised(6, 6, 9, 9, OURO, base=3)
    cv.tonemap(7, 7, ["65", "54"], CELESTE)
    return cv


OUT = {
    'runic_furnace_side': side_t1(), 'runic_furnace_top': top_t1(),
    'runic_furnace_front': furnace_front(lambda c: bricks(c, 0, 0, 15, 15, PEDRA, mottle=STONE_MOTTLE), COBRE, LATAO, LATAO, AMETISTA, FOGO, False),
    'runic_furnace_front_on': furnace_front(lambda c: bricks(c, 0, 0, 15, 15, PEDRA, mottle=STONE_MOTTLE), COBRE, LATAO, LATAO, AMETISTA, FOGO, True),
    'arcane_furnace_side': side_t2(), 'arcane_furnace_top': top_t2(),
    'arcane_furnace_front': furnace_front(lambda c: tiles(c, 0, 0, 15, 15, ARDOSIA, mottle=TILE_MOTTLE), LATAO, MITRIL, MITRIL, ARCANO, ARCANO, False),
    'arcane_furnace_front_on': furnace_front(lambda c: tiles(c, 0, 0, 15, 15, ARDOSIA, mottle=TILE_MOTTLE), LATAO, MITRIL, MITRIL, ARCANO, ARCANO, True),
    'star_forge_side': star_side(), 'star_forge_front': star_front(False), 'star_forge_front_on': star_front(True),
    'star_forge_top': star_top(), 'star_forge_trim': star_trim(),
}
if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    for n, cv in OUT.items(): cv.image().save(os.path.join(out, n + '.png'))
    print('ok', len(OUT))
