"""Tier I (Oficina): materiais compartilhados + faces próprias das máquinas.
Pedra/abeto no corpo, cobre no acabamento, latão nos suportes, ametista como brilho.
Uso: python tier1.py <pasta_saida>   (gera PNGs; animações saem como faixas verticais)"""
import os, sys, math
from kit import *
from sample_furnaces_v2 import STONE_MOTTLE, side_t1, top_t1, top_frame, gem

OUT = {}      # nome -> Image


def save(name, cv_or_im):
    OUT[name] = cv_or_im.image() if isinstance(cv_or_im, Canvas) else cv_or_im


def stone(cv, x0=0, y0=0, x1=15, y1=15):
    bricks(cv, x0, y0, x1, y1, PEDRA, mottle=STONE_MOTTLE)


# ======================= materiais =======================
def t1_copper():
    cv = Canvas()
    plates(cv, 0, 0, 15, 15, COBRE)
    for (px, py) in ((0, 0), (8, 0), (0, 8), (8, 8)):
        cv.rivet(px + 1, py + 1, LATAO); cv.rivet(px + 5, py + 5, LATAO)
    cv.deltamap(0, 0, ["................", "...+......-.....", ".....-.......+..", "..-.............",
                       "........+.......", ".+..........-...", "......-.........", "................",
                       "................", "...-.....+......", "..........-.....", ".+.....+........",
                       "....-...........", "...........+....", "......-.....-...", "................"])
    for (x, y, t) in ((7, 5, 2), (7, 6, 3), (8, 13, 3), (15, 3, 2), (2, 15, 2), (3, 15, 3)):
        cv.put(x, y, VERDETE, t)
    return cv


def t1_base():
    cv = Canvas()
    for y in range(16):
        c, ry = y // 4, y % 4
        for x in range(16):
            t = [3, 2, 2, 0][ry]
            sx = (x + (4 if c % 2 else 0)) % 8
            if ry < 3 and sx == 7: t = 1
            elif ry < 3 and sx == 0: t = 3 if ry else 4
            cv.put(x, y, PEDRA, t)
    cv.deltamap(0, 0, [r.replace('#', '+').replace('=', '-') for r in STONE_MOTTLE])
    return cv


def t1_iron():
    cv = Canvas()
    plates(cv, 0, 0, 15, 15, FERRO, base=4)
    for (px, py) in ((0, 0), (8, 0), (0, 8), (8, 8)):
        for (dx, dy) in ((1, 1), (5, 1), (1, 5), (5, 5)):
            cv.rivet(px + dx, py + dy, FERRO)
    return cv


def crate_frame(cv):
    cv.raised(0, 0, 15, 15, ABETO, base=3)
    for i in range(1, 15):
        cv.put(i, 1, ABETO, 4); cv.put(1, i, ABETO, 4); cv.put(i, 14, ABETO, 2); cv.put(14, i, ABETO, 2)


def crate_corners(cv):
    for (x, y) in ((0, 0), (13, 0), (0, 13), (13, 13)):
        cv.raised(x, y, x + 2, y + 2, COBRE, base=3); cv.put(x + 1, y + 1, LATAO, 6)


GRAIN_V = ["..-.......-.", "..-...+...-.", "......+.....", "..-.........", "....-.....+.", "....-.......",
           "+...-.......", "+.........-.", "......-...-.", "......-.....", "..+.........", "..+.....-..."]


def t1_crate():
    cv = Canvas(); crate_frame(cv)
    for y in range(2, 14):
        for x in range(2, 14):
            px = (x - 2) % 4
            cv.put(x, y, ABETO, 1 if px == 3 else 4 if px == 0 else 3)
    cv.deltamap(2, 2, GRAIN_V)
    cv.recess(2, 2, 13, 13, depth=1)
    for x in (3, 7, 11):
        cv.put(x, 3, FERRO, 5); cv.put(x, 12, FERRO, 5)
    crate_corners(cv)
    return cv


def t1_crate_top():
    cv = Canvas(); crate_frame(cv)
    for y in range(2, 14):
        for x in range(2, 14):
            py = (y - 2) % 4
            cv.put(x, y, ABETO, 1 if py == 3 else 4 if py == 0 else 3)
    cv.deltamap(2, 2, [r[::-1] for r in GRAIN_V[::-1]])
    cv.recess(2, 2, 13, 13, depth=1)
    crate_corners(cv)
    return cv


def t1_wood():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv.put(x, y, ABETO, [4, 3, 3, 1][y % 4])
    cv.deltamap(0, 0, ["................", "..--.....-...---", "......--........", "................",
                       "................", ".---....-.......", "..........--....", "................",
                       "................", "....-......--...", ".--.............", "................",
                       "................", "........---.....", "...-..........-.", "................"])
    for (x, y) in ((2, 1), (13, 5), (6, 9), (11, 13)):
        cv.put(x, y, FERRO, 5); cv.shift(x + 1, y, -1)
    return cv


def t1_crystal():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            if abs(dx) + abs(dy) < 4.5: t = 6 if dx + dy < -1 else 5
            elif dy < dx and dy < -dx: t = 5
            elif dx < dy and dx < -dy: t = 4
            elif dx > 0 and abs(dy) < dx: t = 3
            else: t = 2
            if abs(abs(dx) - abs(dy)) < 0.6 and abs(dx) > 2: t = min(6, t + 1)
            if x in (0, 15) or y in (0, 15): t -= 2
            cv.put(x, y, AMETISTA, t)
    for (x, y) in ((4, 3), (11, 6), (5, 11)):
        cv.put(x, y, AMETISTA, 6)
    return cv


def lamp(lit):
    cv = Canvas()
    for y in range(2, 14):
        for x in range(2, 14):
            if lit:
                d = abs(x - 7.5) + abs(y - 7.5)
                cv.put(x, y, FOGO, 6 if d < 2 else 5 if d < 4.5 else 4 if d < 7 else 3)
            else:
                cv.put(x, y, AMETISTA, 1 if (x + y) % 7 else 2)
    if not lit:
        for i in range(3): cv.put(3 + i, 5 - i, AMETISTA, 4); cv.put(3 + i, 6 - i, AMETISTA, 3)
    cv.raised(0, 0, 15, 1, COBRE, 3); cv.raised(0, 14, 15, 15, COBRE, 3)
    cv.raised(0, 0, 1, 15, COBRE, 3); cv.raised(14, 0, 15, 15, COBRE, 3)
    for i in range(2, 14):
        cv.put(7, i, COBRE, 4); cv.put(8, i, COBRE, 2); cv.put(i, 7, COBRE, 4); cv.put(i, 8, COBRE, 2)
    cv.put(7, 7, LATAO, 6); cv.put(8, 8, LATAO, 3); cv.put(7, 8, LATAO, 4); cv.put(8, 7, LATAO, 4)
    for (x, y) in ((0, 0), (14, 0), (0, 14), (14, 14)): cv.put(x, y, LATAO, 5)
    if lit: cv.glow(0, 0, 15, 15, FOGO, 7, 7, radius=8, strength=2)
    return cv


def t1_stone():
    cv = Canvas(); stone(cv); return cv


def t1_void():
    cv = Canvas()
    plates(cv, 0, 0, 15, 15, AMETISTA, base=1)
    for (x, y) in ((3, 2), (4, 3), (4, 4), (5, 5), (11, 9), (12, 10), (12, 11), (13, 12), (2, 12), (3, 11), (10, 3), (10, 4)):
        cv.put(x, y, AMETISTA, 5)
    for (x, y) in ((4, 2), (5, 4), (12, 9), (13, 11), (3, 12), (11, 3)):
        cv.put(x, y, AMETISTA, 3)
    return cv


def t1_casing():
    cv = Canvas(); stone(cv); frame_box(cv, 0, 0, 15, 15, COBRE, LATAO); return cv


# ======================= faces próprias =======================
def runic_crusher_plate(lit):
    cv = t1_copper()
    cv.raised(4, 5, 11, 9, LATAO, base=3)
    cv.recess(6, 6, 9, 8, depth=1)
    cv.fill(6, 6, 9, 8, AMETISTA, 4 if lit else 2)
    cv.put(7, 7, AMETISTA, 6 if lit else 3); cv.put(8, 7, AMETISTA, 6 if lit else 3)
    if lit: cv.glow(4, 5, 11, 9, AMETISTA, 7, 7, radius=3, strength=1)
    return cv


def magic_compressor_top():
    cv = Canvas()
    cv.fill(0, 0, 15, 15, COBRE, 3)
    for x in range(16):
        for i, t in enumerate([4, 5, 3, 1]): cv.put(x, i, COBRE, t)
    for x in (3, 7, 11): cv.rivet(x, 1, LATAO)
    cv.raised(2, 4, 13, 13, COBRE, base=3)
    cv.fill(6, 6, 9, 9, AMETISTA, 2); cv.recess(6, 6, 9, 9, depth=2)
    cv.put(7, 7, AMETISTA, 5); cv.put(8, 8, AMETISTA, 3)
    for (x, y) in ((3, 5), (12, 5), (3, 12), (12, 12)): cv.rivet(x, y, LATAO)
    return cv


def magic_compressor_body():
    cv = t1_base()
    cv.raised(4, 4, 11, 13, PEDRA, base=3)
    for x in range(4, 12):
        cv.put(x, 4, COBRE, 5); cv.put(x, 5, COBRE, 2); cv.put(x, 12, COBRE, 4); cv.put(x, 13, COBRE, 1)
    cv.fill(7, 7, 8, 10, AMETISTA, 2); cv.recess(7, 7, 8, 10, depth=1)
    cv.put(7, 8, AMETISTA, 5); cv.put(8, 9, AMETISTA, 4)
    for x in range(2, 14): cv.put(x, 14, PEDRA, 4); cv.put(x, 15, PEDRA, 1)
    return cv


def condenser_front(lit):
    cv = Canvas(); stone(cv); frame_box(cv, 0, 0, 15, 15, COBRE, LATAO)
    cv.raised(3, 4, 12, 11, COBRE, base=3)
    cv.fill(4, 5, 11, 10, GELO, 3 if lit else 1); cv.recess(4, 5, 11, 10, depth=2)
    pipe = [(x, 6) for x in range(5, 11)] + [(10, 7), (10, 8)] + [(x, 9) for x in range(5, 11)]
    for (x, y) in pipe: cv.put(x, y, COBRE, 4)
    for (x, y) in pipe:
        if (x, y + 1) not in pipe and y + 1 <= 10: cv.shift(x, y + 1, -1)
    for (x, y) in ((4, 5), (5, 5), (4, 6), (11, 10), (11, 5)): cv.put(x, y, GELO, 6 if lit else 5)
    if lit:
        for (x, y) in ((6, 7), (8, 8), (7, 10), (9, 10)): cv.put(x, y, GELO, 6)
        for x in range(5, 10): cv.put(x, 10, GELO, 4)
        cv.glow(2, 3, 13, 12, GELO, 7, 8, radius=5, strength=1)
    return cv


def frost(cv, pts):
    for (x, y) in pts: cv.put(x, y, GELO, 6 if (x + y) % 3 else 5)


def condenser_side():
    cv = t1_casing()
    cv.raised(5, 5, 10, 10, LATAO, base=3)
    cv.fill(6, 6, 9, 9, GELO, 3); cv.recess(6, 6, 9, 9, depth=1)
    cv.put(7, 7, GELO, 6); cv.put(6, 6, GELO, 5); cv.put(8, 8, GELO, 2)
    frost(cv, [(2, 4), (3, 4), (2, 5), (13, 4), (12, 4), (2, 11), (13, 11), (12, 11), (4, 4), (11, 4)])
    return cv


def condenser_top():
    cv = Canvas(); bricks(cv, 0, 0, 15, 15, PEDRA, course=5, width=7, offset=3, mottle=STONE_MOTTLE)
    top_frame(cv, COBRE, LATAO)
    cv.raised(4, 4, 11, 11, COBRE, base=3)
    cv.fill(5, 5, 10, 10, VAZIO, 2); cv.recess(5, 5, 10, 10, depth=2)
    for (x, y) in ((5, 5), (6, 6), (9, 9), (10, 10), (10, 5), (9, 6), (6, 9), (5, 10)): cv.put(x, y, COBRE, 4)
    cv.fill(7, 7, 8, 8, LATAO, 4); cv.put(7, 7, LATAO, 6)
    frost(cv, [(2, 2), (3, 2), (2, 3), (13, 2), (12, 13), (13, 13), (4, 4), (11, 11)])
    return cv


# ---- Extrator de Essência (forma de caldeirão) ----
def ee_side(k):
    cv = Canvas(); stone(cv)
    for x in range(16):
        for i, t in enumerate([5, 3, 1]): cv.put(x, i, COBRE, t)
        cv.shift(x, 3, -2)
        for i, t in enumerate([0, 4, 2]): cv.put(x, 13 + i, COBRE, t)
    for y in range(3, 13): cv.put(0, y, COBRE, 4); cv.put(15, y, COBRE, 1)
    for x in (2, 13): cv.rivet(x, 1, LATAO)
    for (x, y) in ((1, 14), (14, 14)): cv.put(x, y, LATAO, 6)
    cv.raised(6, 5, 9, 9, LATAO, base=3); cv.recess(7, 6, 8, 8, depth=1)
    lit = k > 0
    cv.fill(7, 6, 8, 8, AMETISTA, 2 + min(3, int(k * 1.5)))
    cv.put(7, 6, AMETISTA, 3 + min(3, int(k * 1.5)))
    if lit: cv.glow(4, 4, 11, 11, AMETISTA, 7, 7, radius=4, strength=k)
    return cv


def ee_top(k):
    cv = Canvas(); cv.fill(0, 0, 15, 15, COBRE, 3)
    cv.raised(0, 0, 15, 15, COBRE, base=3)
    for i in range(1, 15): cv.put(i, 1, COBRE, 4); cv.put(1, i, COBRE, 4)
    for i in range(2, 14):
        cv.put(i, 2, COBRE, 1); cv.put(2, i, COBRE, 1); cv.put(i, 13, COBRE, 5); cv.put(13, i, COBRE, 5)
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)): cv.put(x, y, LATAO, 6)
    if k > 0:
        for i in range(2, 14):
            for (x, y) in ((i, 2), (2, i)): cv.px[y][x] = ['mix', ([COBRE, 1], AMETISTA, k * 1.5)]
    return cv


def ee_inner(k):
    cv = t1_base()
    for (x, y) in ring(8, 8, 5):
        cv.put(x, y, AMETISTA, 2 + min(4, int(k * 2)))
    for (x, y) in ((8, 3), (8, 12), (3, 8), (12, 8), (8, 8)):
        cv.put(x, y, AMETISTA, 3 + min(3, int(k * 2)))
    if k > 0: cv.glow(0, 0, 15, 15, AMETISTA, 8, 8, radius=7, strength=k)
    return cv


def ee_bottom(k):
    cv = t1_base()
    for i in range(16): cv.put(i, 0, COBRE, 4); cv.put(0, i, COBRE, 4); cv.put(i, 15, COBRE, 1); cv.put(15, i, COBRE, 1)
    return cv


def pulse(fn, n=16):
    return strip([fn(1.0 + 1.0 * math.sin(2 * math.pi * i / n)) for i in range(n)])


# ---- Tear Mágico ----
def loom_frame(cv):
    for i in range(16):
        for (x, y, t) in ((i, 0, 5), (i, 1, 3), (0, i, 4), (1, i, 2), (i, 15, 1), (i, 14, 3), (15, i, 1), (14, i, 3)):
            cv.put(x, y, ABETO, t)
    for (x, y) in ((0, 0), (13, 0), (0, 13), (13, 13)):
        cv.raised(x, y, x + 2, y + 2, COBRE, base=3); cv.put(x + 1, y + 1, LATAO, 6)


def magic_loom_front():
    cv = Canvas(); cv.fill(0, 0, 15, 15, ABETO, 1)
    for y in range(2, 9):
        for x in range(2, 14):
            if x % 2 == 0: cv.put(x, y, LINHO, 5 if y < 5 else 4)
    for x in (3, 7, 11): cv.put(x, 4, AMETISTA, 5); cv.put(x, 5, AMETISTA, 4)
    for x in range(2, 14): cv.put(x, 6, COBRE, 4); cv.put(x, 7, COBRE, 2)
    cv.raised(6, 8, 9, 8, LATAO, base=4)
    for y in range(9, 13):
        for x in range(2, 14): cv.put(x, y, AMETISTA, 3 if (x + y) % 2 else 2)
    for x in range(2, 14): cv.put(x, 10, LATAO, 4 if x % 2 else 3)
    for x in range(2, 14): cv.put(x, 13, ABETO, 5); cv.shift(x, 12, -1)
    loom_frame(cv)
    return cv


def magic_loom_side():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            px = x % 4
            cv.put(x, y, ABETO, 1 if px == 3 else 4 if px == 0 else 3)
    cv.deltamap(2, 2, GRAIN_V)
    loom_frame(cv)
    for (x, y) in ring(8, 8, 4.2): cv.put(x, y, COBRE, 4 if y < 8 else 2)
    for i in range(-3, 4):
        cv.put(8 + i, 8, COBRE, 3); cv.put(8, 8 + i, COBRE, 3)
    cv.fill(7, 7, 8, 8, LATAO, 4); cv.put(7, 7, LATAO, 6)
    for (x, y) in ring(8, 8, 4.2): cv.shift(x + 1, y + 1, -1)
    return cv


def magic_loom_top():
    cv = Canvas()
    for y in range(16):
        for x in range(16): cv.put(x, y, ABETO, [4, 3, 3, 1][y % 4])
    loom_frame(cv)
    for x in range(2, 14): cv.put(x, 7, ABETO, 5); cv.put(x, 8, ABETO, 2)
    for (x0, ramp) in ((3, AMETISTA), (7, LINHO), (11, GELO)):
        cv.raised(x0, 5, x0 + 1, 10, ramp, base=4)
        cv.put(x0, 5, LATAO, 5); cv.put(x0 + 1, 5, LATAO, 4); cv.put(x0, 10, LATAO, 3); cv.put(x0 + 1, 10, LATAO, 2)
    return cv


def magic_loom_bottom():
    cv = t1_crate_top(); return cv


# ---- Escavadora (Tunneler) ----
def tunneler_body(cv):
    plates(cv, 0, 0, 15, 15, FERRO, base=3)
    frame_box(cv, 0, 0, 15, 15, COBRE, LATAO)


def tunneler_side():
    cv = Canvas(); tunneler_body(cv)
    for x in range(3, 13):
        cv.put(x, 7, LATAO, 4 if (x // 2) % 2 else 1); cv.put(x, 8, LATAO, 3 if (x // 2) % 2 else 0)
    return cv


def tunneler_front():
    cv = Canvas(); tunneler_body(cv)
    cv.fill(3, 4, 12, 11, VAZIO, 3); cv.recess(3, 4, 12, 11, depth=2)
    return cv


def tunneler_back():
    cv = Canvas(); tunneler_body(cv)
    for cx in (5, 11):
        for (x, y) in disk(cx, 8, 2.2): cv.put(x, y, VAZIO, 2)
        for (x, y) in ring(cx, 8, 2.6): cv.put(x, y, COBRE, 4 if y < 8 else 2)
    cv.put(8, 7, AMETISTA, 5); cv.put(8, 8, AMETISTA, 3)
    return cv


def drill(frame=0, lit=False):
    """Broca escalonada: cada degrau é um anel (regiões 3-13, 4-12 ... 7-9), com sulco em espiral."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = max(abs(dx), abs(dy)); ri = int(d)
            ramp = COBRE if ri == 3 else FERRO
            t = 4 if ri % 2 else 3
            if d - ri > 0.0 and (dx < 0 and abs(dx) >= abs(dy) or dy < 0 and abs(dy) >= abs(dx)): t += 1
            else: t -= 1
            a = (math.atan2(dy, dx) / (2 * math.pi)) % 1.0
            if (a * 4 + d * 0.35 + frame * 0.25) % 1.0 < 0.22 and ri >= 1: t = 1
            if d < 1: ramp, t = LATAO, 6
            if ri >= 5: ramp, t = FERRO, 2
            cv.put(x, y, ramp, t)
    if lit: cv.glow(3, 3, 12, 12, FOGO, 7, 7, radius=3, strength=1)
    return cv


# ---- Alambique de Cana ----
def cane_front(lit):
    cv = Canvas()
    plates(cv, 0, 0, 15, 8, COBRE)
    stone(cv, 0, 9, 15, 15)
    frame_box(cv, 0, 0, 15, 15, COBRE, LATAO)
    for (x, y) in ring(8, 6.5, 2.2): cv.put(x, y, LATAO, 5 if y < 6 else 2)
    for (x, y) in disk(8, 6.5, 1.6): cv.put(x, y, LINHO, 6)
    cv.put(8, 6, ADAMANTINA, 3); cv.put(9, 5, ADAMANTINA, 4)
    cv.raised(4, 9, 11, 12, FERRO, base=3)
    cv.fill(5, 10, 10, 11, VAZIO, 2); cv.recess(5, 10, 10, 11, depth=1)
    if lit:
        cv.tonemap(5, 10, ["3.43.4", "456545"], FOGO)
        cv.glow(3, 8, 12, 13, FOGO, 7, 11, radius=4, strength=2)
    return cv


def cane_side():
    cv = Canvas(); plates(cv, 0, 0, 15, 15, COBRE); frame_box(cv, 0, 0, 15, 15, COBRE, LATAO)
    for (x, y) in disk(8, 8, 3): cv.put(x, y, OURO, 2 if y > 8 else 3)
    for (x, y) in ((6, 6), (7, 6), (6, 7)): cv.put(x, y, OURO, 6)
    for (x, y) in ring(8, 8, 3.4): cv.put(x, y, LATAO, 5 if (y < 8 or x < 8) else 2)
    return cv


def cane_top():
    cv = Canvas(); plates(cv, 0, 0, 15, 15, COBRE); top_frame(cv, COBRE, LATAO)
    for (x, y) in disk(8, 8, 4.5): cv.put(x, y, COBRE, 4 if (x + y) < 15 else 2)
    for (x, y) in disk(8, 8, 1.6): cv.put(x, y, VAZIO, 2)
    for (x, y) in ring(8, 8, 1.9): cv.put(x, y, LATAO, 5)
    return cv


def cane_bottom():
    return t1_base()


# ---- Geradores (cinzas / blaze): área visível = colunas 1-14, linhas 4-13 ----
def gen_side():
    cv = Canvas(); stone(cv); frame_box(cv, 1, 4, 14, 13, COBRE, LATAO, band=2, post=1)
    for y in (7, 9):
        for x in range(5, 11): cv.put(x, y, VAZIO, 2); cv.put(x, y + 1, PEDRA, 5)
    return cv


def gen_front(kind, lit, flick=0):
    cv = Canvas(); stone(cv); frame_box(cv, 1, 4, 14, 13, COBRE, LATAO, band=2, post=1)
    cv.raised(4, 6, 11, 11, FERRO, base=3)
    cv.fill(5, 7, 10, 10, VAZIO, 2); cv.recess(5, 7, 10, 10, depth=2)
    if kind == 'ash':
        for x in range(5, 11): cv.put(x, 10, PEDRA, 5 if x % 2 else 4)
        for x in (6, 9): cv.put(x, 9, PEDRA, 4)
        if lit:
            for (x, y) in ((6, 10), (8, 10), (9, 9), (10, 10)): cv.put(x, y, FOGO, 5)
            cv.put(7, 9, FOGO, 3)
    else:
        for x in (6, 8, 10):
            for y in range(7, 11): cv.put(x, y, OURO, 5 if y == 7 else 4 if lit else 3)
        if lit:
            for (x, y) in ((5 + flick, 8), (7, 7 + flick), (9 - flick, 9)): cv.put(x, y, FOGO, 6)
    for x in (5, 7, 9): cv.put(x, 7, FERRO, 4)
    if lit: cv.glow(3, 5, 12, 12, FOGO, 7, 9, radius=4, strength=2)
    return cv


def build():
    save('t1_copper', t1_copper()); save('t1_base', t1_base()); save('t1_iron', t1_iron())
    save('t1_crate', t1_crate()); save('t1_crate_top', t1_crate_top()); save('t1_wood', t1_wood())
    save('t1_crystal', t1_crystal()); save('t1_lamp', lamp(False)); save('t1_lamp_on', lamp(True))
    save('t1_stone', t1_stone()); save('t1_void', t1_void()); save('t1_casing', t1_casing())
    save('t1_casing_vent', side_t1()); save('t1_top', top_t1())
    save('runic_crusher_plate', runic_crusher_plate(False)); save('runic_crusher_plate_on', runic_crusher_plate(True))
    save('magic_compressor_top', magic_compressor_top()); save('magic_compressor_body', magic_compressor_body())
    save('condenser_front', condenser_front(False)); save('condenser_front_on', condenser_front(True))
    save('condenser_side', condenser_side()); save('condenser_top', condenser_top())
    save('essence_extractor_side', ee_side(0)); save('essence_extractor_top', ee_top(0))
    save('essence_extractor_inner', ee_inner(0)); save('essence_extractor_bottom', ee_bottom(0))
    save('essence_extractor_side_on', pulse(ee_side)); save('essence_extractor_top_on', pulse(ee_top))
    save('essence_extractor_inner_on', pulse(ee_inner)); save('essence_extractor_bottom_on', pulse(ee_bottom))
    save('magic_loom_front', magic_loom_front()); save('magic_loom_side', magic_loom_side())
    save('magic_loom_top', magic_loom_top()); save('magic_loom_bottom', magic_loom_bottom())
    save('tunneler_side', tunneler_side()); save('tunneler_front', tunneler_front()); save('tunneler_back', tunneler_back())
    save('tunneler_drill', drill()); save('tunneler_drill_on', strip([drill(i, True) for i in range(4)]))
    save('cane_still_front', cane_front(False)); save('cane_still_front_on', cane_front(True))
    save('cane_still_side', cane_side()); save('cane_still_top', cane_top()); save('cane_still_bottom', cane_bottom())
    save('t1_generator_side', gen_side())
    save('ash_generator_front', gen_front('ash', False)); save('ash_generator_front_on', gen_front('ash', True))
    save('blaze_generator_front', gen_front('blaze', False))
    save('blaze_generator_front_on', strip([gen_front('blaze', True, 0), gen_front('blaze', True, 1)]))


if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    build()
    for n, im in OUT.items(): im.save(os.path.join(out, n + '.png'))
    print('ok', len(OUT))
