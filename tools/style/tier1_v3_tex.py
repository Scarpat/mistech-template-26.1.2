"""Texturas v3 do Tier I: materiais CALMOS. Pouco contraste; o volume vem do modelo.
Regra: base num tom, variação de no máximo ±1 tom em ~10% dos pixels, nenhuma moldura pintada em peças."""
import math
from kit import *

MOTTLE = ["................", "..++......--....", "..+.......-.....", "........+.......", "......+++.......",
          "-.........-.....", "--.......---....", "...............+", "....--.......++.", "....-...........",
          "..........+.....", ".+.......+++....", ".++.............", "......-.........", ".....--......-..", "............--.."]
SPECK = ["....+.......-...", "..........+.....", ".-......+.......", "......-.......+.", "...+........-...",
         "..........-.....", "+.....+.........", ".........+...-..", "..-.............", ".......-....+...",
         ".+.........-....", "....+...........", "..........+...-.", ".-....-.........", "........+.......", "...+.......-...."]


def flat(ramp, t, speck=True):
    cv = Canvas(); cv.fill(0, 0, 15, 15, ramp, t)
    if speck: cv.deltamap(0, 0, MOTTLE)
    return cv


def stone():            # pedra lisa clara (tipo andesito)
    return flat(PEDRA, 4)


def stone_dark():       # base / pedra escura
    return flat(PEDRA, 2)


def bricks_soft():
    cv = Canvas()
    for y in range(16):
        c, ry = y // 4, y % 4
        for x in range(16):
            bx = (x + (4 if c % 2 else 0)) % 8
            t = 3 if (ry == 3 or bx == 7) else (5 if ry == 0 else 4)
            cv.put(x, y, PEDRA, t)
    cv.deltamap(0, 0, [r.replace('-', '.') for r in SPECK])
    return cv


def copper():
    cv = Canvas()
    for y in range(16):
        for x in range(16): cv.put(x, y, COBRE, 5 if y % 8 == 0 else 3 if y % 8 == 7 else 4)
    for (x, y) in ((3, 2), (11, 5), (6, 10), (13, 13)): cv.put(x, y, COBRE, 5)
    return cv


def iron():
    return flat(FERRO, 4)


def dark():
    cv = flat(VAZIO, 2, speck=False)
    cv.deltamap(0, 0, SPECK); return cv


def fire():
    cv = Canvas(); cv.fill(0, 0, 15, 15, VAZIO, 1)
    rows = {8: "......3.....3...", 9: "..3..343...343..", 10: ".343.3453.34543.", 11: "3454345653456543",
            12: "4565456665456654", 13: "5666566666566665", 14: "4555555555555554", 15: "2222222222222222"}
    for y, r in rows.items():
        for x, c in enumerate(r):
            if c != '.': cv.put(x, y, FOGO, int(c))
    return cv


def amethyst(lit):
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            t = 5 if x + y < 12 else 4 if x + y < 20 else 3
            if lit: t += 1
            cv.put(x, y, AMETISTA, t)
    for (x, y) in ((3, 2), (2, 3), (4, 4)): cv.put(x, y, AMETISTA, 6)
    return cv


def ice(lit):
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            t = 5 if x + y < 12 else 4 if x + y < 20 else 3
            cv.put(x, y, GELO, min(6, t + (1 if lit else 0)))
    for (x, y) in ((3, 2), (2, 3)): cv.put(x, y, GELO, 6)
    return cv


def glass():
    cv = Canvas()
    for i in range(16):
        for (x, y) in ((i, 0), (0, i)): cv.put(x, y, GELO, 5)
        for (x, y) in ((i, 15), (15, i)): cv.put(x, y, GELO, 3)
    for i in range(3): cv.put(3 + i, 5 - i, GELO, 6); cv.put(4 + i, 7 - i, GELO, 5)
    return cv


def water(lit):
    cv = flat(GELO, 3 if lit else 2)
    return cv


def roller():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv.put(x, y, PEDRA, 4 if (x // 2) % 2 == 0 else 3)
    for x in range(0, 16, 4):
        for y in range(16): cv.put(x, y, FERRO, 5 if y % 4 == 0 else 4)
    return cv


def rune_floor(lit):
    cv = flat(PEDRA, 2)
    for (x, y) in ring(8, 8, 4.5): cv.put(x, y, AMETISTA, 5 if lit else 2)
    for (x, y) in ((8, 3), (8, 12), (3, 8), (12, 8)): cv.put(x, y, AMETISTA, 6 if lit else 3)
    return cv


def plaque(lit):
    cv = copper()
    cv.fill(5, 6, 10, 9, LATAO, 4)
    cv.fill(7, 7, 8, 8, AMETISTA, 6 if lit else 3)
    return cv


def essence(lit):
    return flat(AMETISTA, 3 if lit else 2)


ALL = {
    't1s_stone': stone, 't1s_stone_dark': stone_dark, 't1s_bricks': bricks_soft, 't1s_copper': copper,
    't1s_iron': iron, 't1s_dark': dark, 't1s_fire': fire, 't1s_glass': glass, 't1s_roller': roller,
    't1s_amethyst': lambda: amethyst(False), 't1s_amethyst_on': lambda: amethyst(True),
    't1s_ice': lambda: ice(False), 't1s_ice_on': lambda: ice(True),
    't1s_water': lambda: water(False), 't1s_water_on': lambda: water(True),
    't1s_rune_floor': lambda: rune_floor(False), 't1s_rune_floor_on': lambda: rune_floor(True),
    't1s_plaque': lambda: plaque(False), 't1s_plaque_on': lambda: plaque(True),
}
