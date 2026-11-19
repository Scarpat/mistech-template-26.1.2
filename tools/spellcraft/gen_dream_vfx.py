#!/usr/bin/env python3
"""Mistech — texturas dos efeitos oníricos das magias (partícula mistech:dream).
Uso: python3 gen_dream_vfx.py <pasta textures/particle> <arquivo particles/dream.json>
A ORDEM de NAMES é o índice usado em DreamSprites.java: não reordenar sem mudar lá."""
import json, math, random, sys
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else "out"
JSON = sys.argv[2] if len(sys.argv) > 2 else "out/dream.json"
NAMES = []

def save(name, img):
    img.save(f"{OUT}/dream_{name}.png")
    NAMES.append(f"mistech:dream_{name}")

def art(rows, pal):
    h, w = len(rows), len(rows[0])
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == w, (row, w)
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), pal[ch])
    return img

def hexa(s, a=255):
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)

# ---------------------------------------------------------------- mariposa de brasa (Fagulha)
MOTH = {"k": hexa("3a0d05"), "r": hexa("a3260b"), "o": hexa("f0701a"), "y": hexa("ffc23d"), "w": hexa("fff3c4"), "e": hexa("1c0503")}
save("moth_0", art([
    "................",
    ".kk....w....kk..",
    "krrk...w...krrk.",
    "kroork.y.kroork.",
    "kroyork.kroyork.",
    "kroeyorwroyeork.",
    ".kroyoywyoyork..",
    "..krooywyoork...",
    "...krrywyrrk....",
    "..krooywyoork...",
    ".kroyorwroyork..",
    ".kroork.kroork..",
    "..krrk...krrk...",
    "...kk.....kk....",
    "................",
    "................"], MOTH))
save("moth_1", art([
    "................",
    ".......w........",
    "...kk..w..kk....",
    "..krrk.y.krrk...",
    "..kroork.kroork.",
    "..kroyokwkoyork.",
    "...kreywyerk....",
    "...kroywyork....",
    "....krywyrk.....",
    "...kroywyork....",
    "...kroykwyork...",
    "...krok.kork....",
    "....kk...kk.....",
    "................",
    "................",
    "................"], MOTH))
save("ash", art([
    "........",
    "..gG....",
    ".gGoG...",
    ".GoyoG..",
    "..GoGg..",
    "...Gg...",
    "........",
    "........"], {"g": hexa("3b302c", 200), "G": hexa("6b5a50"), "o": hexa("f0701a"), "y": hexa("ffc23d")}))

# ---------------------------------------------------------------- olho (Penumbra)
def eye(open_h, pupil):
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    cx, cy, w = 7.5, 7.5, 7.5
    inside = set()
    for y in range(S):
        for x in range(S):
            t = 1 - ((x - cx) / w) ** 2
            if t > 0 and abs(y - cy) <= open_h * t ** 0.8:
                inside.add((x, y))
    line, sclera, shade = hexa("120a1c"), hexa("e9e2d0"), hexa("b3a6b8")
    iris, iris_d, pup, glint = hexa("8d3fd1"), hexa("4c1d7a"), hexa("05020a"), hexa("ffffff")
    for (x, y) in inside:
        d = math.hypot(x - cx, y - cy)
        c = sclera if y >= cy - 1 else shade
        if d <= 3.3:
            c = iris if d <= 2.4 else iris_d
        if pupil and abs(x - cx) <= 0.6 and abs(y - cy) <= 2.6:
            c = pup
        img.putpixel((x, y), c)
    # contorno: pixels de fora vizinhos dos de dentro (pálpebra)
    for y in range(S):
        for x in range(S):
            if (x, y) in inside:
                continue
            if any((x + dx, y + dy) in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), line)
    if open_h < 0.6:  # fechado: uma linha com cílios
        for x in range(1, 15):
            img.putpixel((x, 8 if 3 < x < 12 else 7), line)
        for x in (3, 6, 9, 12):
            img.putpixel((x, 9 if 3 < x < 12 else 8), line)
            img.putpixel((x, 10 if 3 < x < 12 else 9), hexa("120a1c", 150))
    elif pupil:
        img.putpixel((6, 6), glint)
    return img

for i, (h, p) in enumerate([(0.0, False), (1.4, False), (2.6, True), (4.2, True), (5.4, True)]):
    save(f"eye_{i}", eye(h, p))
save("drip", art([
    "...kk...",
    "...kk...",
    "..kppk..",
    "..kppk..",
    ".kpPppk.",
    ".kpppPk.",
    "..kppk..",
    "...kk..."], {"k": hexa("120a1c"), "p": hexa("3a1a5c"), "P": hexa("8d3fd1")}))

# ---------------------------------------------------------------- pálpebra no espaço (Piscar)
def slit(wmax):
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    rnd = random.Random(7)
    stars = [(rnd.randint(9, 22), rnd.randint(3, 28), rnd.choice(["ffffff", "9ee8ff", "f3b6ff"])) for _ in range(26)]
    cx, cy = 15.5, 15.5
    def half(y):
        t = 1 - ((y - cy) / 15.5) ** 2
        return wmax * t ** 0.7 if t > 0 else -1
    for y in range(S):
        hw = half(y)
        for x in range(S):
            d = abs(x - cx)
            if hw < 0:
                continue
            if d <= hw - 1:
                g = 1 - d / max(hw, 1)
                img.putpixel((x, y), (int(8 + 22 * g), int(4 + 8 * g), int(28 + 40 * g), 255))
            elif d <= hw:
                img.putpixel((x, y), hexa("f06bff"))
            elif d <= hw + 1:
                img.putpixel((x, y), hexa("a030d8", 170))
            elif d <= hw + 2:
                img.putpixel((x, y), hexa("5a1a9c", 70))
    for (x, y, c) in stars:
        if abs(x - cx) <= half(y) - 2:
            img.putpixel((x, y), hexa(c))
    return img

for i, w in enumerate([0.4, 2.0, 4.5, 7.0, 9.0]):
    save(f"slit_{i}", slit(w))
save("star", art([
    "...w....",
    "...w....",
    "..wWw...",
    "wwWWWww.",
    "..wWw...",
    "...w....",
    "...w....",
    "........"], {"w": hexa("d9a8ff", 210), "W": hexa("ffffff")}))

# ---------------------------------------------------------------- algarismos (Lentidão Temporal)
GLYPH = {"I": ["#", "#", "#", "#", "#", "#", "#"],
         "V": ["#...#", "#...#", "#...#", ".#.#.", ".#.#.", "..#..", "..#.."],
         "X": ["#...#", ".#.#.", ".#.#.", "..#..", ".#.#.", ".#.#.", "#...#"]}
ROMAN = ["I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"]
def numeral(text):
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    width = sum(len(GLYPH[c][0]) for c in text) + (len(text) - 1)
    x0 = (S - width) // 2
    gold, pale, dark = hexa("e8c66a"), hexa("fff4c9"), hexa("2a1d47", 130)
    cells = set()
    x = x0
    for c in text:
        g = GLYPH[c]
        for gy, row in enumerate(g):
            for gx, ch in enumerate(row):
                if ch == "#":
                    cells.add((x + gx, 5 + gy))
        x += len(g[0]) + 1
    for xx in range(x0 - 1, x0 + width + 1):  # serifas: barra em cima e embaixo
        cells.add((xx, 4))
        cells.add((xx, 12))
    for (cx_, cy_) in cells:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                p = (cx_ + dx, cy_ + dy)
                if p not in cells and 0 <= p[0] < S and 0 <= p[1] < S:
                    img.putpixel(p, dark)
    for (cx_, cy_) in cells:
        img.putpixel((cx_, cy_), pale if cy_ <= 7 else gold)
    return img
for i, t in enumerate(ROMAN):
    save(f"numeral_{i + 1}", numeral(t))
save("grain", art([
    "....",
    ".yw.",
    ".gy.",
    "...."], {"y": hexa("e8c66a"), "w": hexa("fff4c9"), "g": hexa("a8853a")}))

# ---------------------------------------------------------------- flor que respira (Floração)
def flower(stage):
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    stem, stem_d, leaf = hexa("3f8f5a"), hexa("1f5a3a"), hexa("6fc47a")
    top = [12, 10, 8, 7, 6][stage]
    for y in range(top, 16):
        x = 7 + (1 if (y // 3) % 2 == 0 else 0)
        img.putpixel((x, y), stem)
        img.putpixel((x - 1, y), stem_d)
    if stage >= 1:
        for p in ((9, 13), (10, 12), (5, 11), (4, 10)):
            img.putpixel(p, leaf)
    hx, hy = 7.5, top - 0.5
    if stage == 0:
        img.putpixel((7, top - 1), leaf)
        img.putpixel((8, top - 1), leaf)
        return img
    petal, petal_l, petal_d = hexa("c9a2f2"), hexa("f1e2ff"), hexa("7d52b8")
    core, core_l = hexa("d8ff6a"), hexa("ffffff")
    r = [0, 1.6, 2.6, 3.6, 4.6][stage]
    n = 5
    for y in range(S):
        for x in range(S):
            dx, dy = x - hx, y - hy
            d = math.hypot(dx, dy)
            if d > r + 0.4:
                continue
            a = math.atan2(dy, dx)
            lobe = abs(math.cos(a * n / 2 + 0.3))
            if stage >= 3 and d > r * (0.55 + 0.45 * lobe):
                continue
            c = petal
            if d > r - 0.9:
                c = petal_d
            elif lobe > 0.8:
                c = petal_l
            img.putpixel((x, y), c)
    if stage >= 2:
        for p in ((7, int(hy)), (8, int(hy)), (7, int(hy) + 1), (8, int(hy) + 1)):
            img.putpixel(p, core)
    if stage >= 4:
        img.putpixel((7, int(hy)), core_l)
    return img
for i in range(5):
    save(f"flower_{i}", flower(i))
save("petal", art([
    "........",
    "..dpp...",
    ".dplpp..",
    ".dpllp..",
    "..dppp..",
    "...dd...",
    "........",
    "........"], {"p": hexa("c9a2f2"), "l": hexa("f1e2ff"), "d": hexa("7d52b8")}))

# ---------------------------------------------------------------- roda de olhos (Luz Sagrada)
def wheel(open_eyes):
    S = 64
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    c = 31.5
    gold, gold_l, gold_d, line = hexa("f2c84b"), hexa("fff6c2"), hexa("b07a1e"), hexa("5a3a0c")
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - c, y - c)
            if 23.5 <= d <= 30.5:
                col = gold
                if d < 24.5 or d > 29.5:
                    col = line
                elif d < 25.5:
                    col = gold_d
                elif d > 28.3:
                    col = gold_l
                img.putpixel((x, y), col)
    for k in range(8):
        a = math.pi * 2 * k / 8
        ex, ey = c + math.cos(a) * 27, c + math.sin(a) * 27
        ta, tb = -math.sin(a), math.cos(a)  # tangente
        for y in range(int(ey) - 5, int(ey) + 7):
            for x in range(int(ex) - 5, int(ex) + 7):
                dx, dy = x - ex, y - ey
                u = dx * ta + dy * tb          # ao longo do aro
                v = dx * math.cos(a) + dy * math.sin(a)  # através do aro
                t = 1 - (u / 4.6) ** 2
                if t <= 0:
                    continue
                hh = (2.4 if open_eyes else 0.5) * t ** 0.7
                if abs(v) <= hh:
                    d = math.hypot(u, v)
                    col = hexa("ffffff")
                    if open_eyes and d <= 1.9:
                        col = hexa("3fa7ff")
                    if open_eyes and d <= 0.8:
                        col = hexa("06122b")
                    if not open_eyes:
                        col = line
                    img.putpixel((x, y), col)
                elif abs(v) <= hh + 1:
                    img.putpixel((x, y), line)
    return img
save("wheel_0", wheel(True))
save("wheel_1", wheel(False))
save("feather", art([
    "................",
    "............ww..",
    "..........wwWw..",
    ".........wWWWw..",
    "........wWWgWw..",
    ".......wWWgWw...",
    "......wWWgWWw...",
    ".....wWWgWWw....",
    "....wWWgWWw.....",
    "....wWgWWw......",
    "...wWgWww.......",
    "...wgWw.........",
    "..gww...........",
    ".g..............",
    "g...............",
    "................"], {"w": hexa("fff6c2", 220), "W": hexa("ffffff"), "g": hexa("f2c84b")}))

def glow():
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 7.5, y - 7.5) / 7.5
            if d < 1:
                img.putpixel((x, y), (255, 255, 255, int(255 * (1 - d) ** 1.6)))
    return img
save("glow", glow())

# ================================================================ lote 2: Fogo e Trevas (sempre no FIM: os índices antigos não mudam)

def bigmoth(fold):
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    k, r, o, y, w = MOTH["k"], MOTH["r"], MOTH["o"], MOTH["y"], MOTH["w"]
    def wing(cx, cy, rx, ry, spot):
        for py in range(S):
            for px in range(S):
                d = math.hypot((px - cx) / rx, (py - cy) / ry)
                if d <= 1.0:
                    c = y if d < 0.35 else o if d < 0.62 else r if d < 0.86 else k
                    img.putpixel((px, py), c)
        if spot:
            for dx, dy, c in ((0, 0, MOTH["e"]), (1, 0, MOTH["e"]), (0, 1, MOTH["e"]), (1, 1, MOTH["e"]), (0, 0, w)):
                img.putpixel((int(cx) + dx, int(cy) + dy), c)
            for dx, dy in ((-1, 0), (2, 0), (-1, 1), (2, 1), (0, -1), (1, -1), (0, 2), (1, 2)):
                img.putpixel((int(cx) + dx, int(cy) + dy), k)
    ux, uw_ = (8.5, 8.0) if not fold else (11.0, 5.0)
    lx, lw = (10.0, 5.8) if not fold else (12.0, 3.8)
    wing(15.5 - (15.5 - lx), 21.5, lw, 6.0, False)
    wing(15.5 + (15.5 - lx), 21.5, lw, 6.0, False)
    wing(ux, 11.0, uw_, 7.5, True)
    wing(31 - ux, 11.0, uw_, 7.5, True)
    for py in range(7, 26):
        img.putpixel((15, py), y if py % 3 else w)
        img.putpixel((16, py), w if py % 3 else y)
    for i in range(5):
        img.putpixel((14 - i // 2 - (1 if i > 2 else 0), 6 - i), w)
        img.putpixel((17 + i // 2 + (1 if i > 2 else 0), 6 - i), w)
    return img
save("bigmoth_0", bigmoth(False))
save("bigmoth_1", bigmoth(True))

FLAME = {"k": hexa("3a0d05"), "r": hexa("c23a0e"), "o": hexa("f0701a"), "y": hexa("ffc23d"), "w": hexa("fff3c4"), "e": hexa("1c0503")}
save("dancer_0", art([
    "................",
    ".......o........",
    "......oy........",
    "......oyo.......",
    ".....oywyo......",
    ".....oeweo......",
    ".....oywyo......",
    "o.....oyo.....o.",
    ".oo..oyyyo..oo..",
    "...oooyyyooo....",
    ".....oyyyo......",
    ".....oyoyo......",
    "....ro...or.....",
    "....r.....r.....",
    "...rr.....rr....",
    "................"], FLAME))
save("dancer_1", art([
    "................",
    "........o.......",
    "........yo......",
    ".......oyo......",
    "......oywyo.....",
    ".o....oeweo...o.",
    "..o...oywyo..o..",
    "..oo...oyo..oo..",
    "...oooyyyyooo...",
    ".....oyyyo......",
    ".....oyyyo......",
    "......oyyo......",
    ".....ro.or......",
    "....rr...r......",
    ".........rr.....",
    "................"], FLAME))

def sun(open_eye):
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    c = 15.5
    for py in range(S):
        for px in range(S):
            dx, dy = px - c, py - c
            d = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            ray = 11.0 + 4.2 * max(0.0, math.cos(a * 6)) ** 3
            if d <= 10.5:
                col = hexa("fff3c4") if d < 5 else hexa("ffc23d") if d < 8.3 else hexa("f0701a") if d < 9.6 else hexa("a3260b")
                img.putpixel((px, py), col)
            elif d <= ray:
                img.putpixel((px, py), hexa("f0701a") if d < ray - 1.2 else hexa("a3260b"))
    line = hexa("3a0d05")
    if open_eye:
        for py in range(S):
            for px in range(S):
                t = 1 - ((px - c) / 6.5) ** 2
                if t > 0 and abs(py - c) <= 3.6 * t ** 0.8:
                    d = math.hypot(px - c, py - c)
                    img.putpixel((px, py), hexa("1c0503") if d < 1.3 else hexa("c23a0e") if d < 2.8 else hexa("ffffff"))
                elif t > 0 and abs(py - c) <= 3.6 * t ** 0.8 + 1:
                    img.putpixel((px, py), line)
    else:
        for px in range(10, 22):
            img.putpixel((px, 16 if 12 < px < 19 else 15), line)
        for px in (11, 14, 17, 20):
            img.putpixel((px, 17 if 12 < px < 19 else 16), line)
    return img
save("sun_0", sun(False))
save("sun_1", sun(True))

EGG = {"k": hexa("3a0d05"), "s": hexa("f4d9a8"), "S": hexa("fff3c4"), "d": hexa("c9965a"), "o": hexa("f0701a"), "y": hexa("ffc23d"), "w": hexa("ffffff")}
save("egg_0", art([
    "................",
    "......kkkk......",
    ".....kSSssk.....",
    "....kSSssssk....",
    "...kSSsssssdk...",
    "...kSsssssodk...",
    "..kSssssssssdk..",
    "..kSssossssddk..",
    "..kssssssssddk..",
    "..ksssssossddk..",
    "..kssssssssddk..",
    "...ksssssdddk...",
    "...kdsssddddk...",
    "....kddddddk....",
    ".....kkkkkk.....",
    "................"], EGG))
save("egg_1", art([
    "................",
    "......kkkk......",
    ".....kSSssk.....",
    "....kSSsysssk...".ljust(16, ".")[:16],
    "...kSSsywssdk...",
    "...kSssyysodk...",
    "..kSsyysywssdk..",
    "..kSywwyyysddk..",
    "..kssyysswyddk..",
    "..ksssyysywddk..",
    "..kssssyyssddk..",
    "...kssssyyddk...",
    "...kdsssdyddk...",
    "....kddddddk....",
    ".....kkkkkk.....",
    "................"], EGG))
save("egg_2", art([
    "................",
    "....k......k....",
    "...ksk.yy.ksk...",
    "...kssywwyssk...",
    "..kSskywwyksdk..",
    "..kSk.yooy.kdk..",
    "..kk..oyyo..kk..",
    "......yooy......",
    "..k...oyyo...k..",
    "..kk.yowwoy.kk..",
    "..kskyowwoykdk..",
    "...kssoyyoddk...",
    "...kdsssddddk...",
    "....kddddddk....",
    ".....kkkkkk.....",
    "................"], EGG))

def hand(fist):
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    inside = set()
    def blob(cx, cy, r):
        for py in range(S):
            for px in range(S):
                if math.hypot(px - cx, py - cy) <= r:
                    inside.add((px, py))
    for py in range(17, 32):           # braço fino
        w = 2.4 + (py - 17) * 0.06
        for px in range(S):
            if abs(px - 15.5 - math.sin(py * 0.35) * 0.8) <= w:
                inside.add((px, py))
    blob(15.5, 14.5, 4.6)              # palma
    for i, ang in enumerate((-68, -32, -6, 20, 50)):
        length = (3.5 if fist else (7.5, 10.5, 11.5, 10.5, 8.0)[i])
        a = math.radians(ang - 90)
        for step in range(int(length * 2) + 1):
            t = step / 2.0
            bend = (t / max(length, 1)) ** 2 * (2.2 if not fist else 0.0) * (1 if ang > 0 else -1)
            blob(15.5 + math.cos(a) * (4 + t) + bend, 14.5 + math.sin(a) * (4 + t), 1.15 - 0.03 * t)
    body, rim, deep = hexa("140a22"), hexa("7a3fc0"), hexa("05020a")
    for (px, py) in inside:
        edge = any((px + dx, py + dy) not in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        img.putpixel((px, py), rim if edge and px <= 15 else deep if edge else body)
    return img
save("hand_0", hand(False))
save("hand_1", hand(True))

MOUTH = {"k": hexa("05020a"), "p": hexa("2a1040"), "r": hexa("8a1238"), "R": hexa("d23a5a"), "w": hexa("f1ead8"), "g": hexa("b3a6b8")}
save("mouth_0", art([
    "................",
    "....kkkkkkkk....",
    "..kkppppppppkk..",
    ".kppwpwpwpwpppk.",
    ".kpwwwwwwwwwwpk.",
    "kppwgwgwgwgwgppk",
    "kpprrrrrrrrrrppk",
    "kprrRRrrrrRRrrpk",
    "kprrrrRRRRrrrrpk",
    "kpprrrrrrrrrrppk",
    "kppwgwgwgwgwgppk",
    ".kpwwwwwwwwwwpk.",
    ".kppwpwpwpwpppk.",
    "..kkppppppppkk..",
    "....kkkkkkkk....",
    "................"], MOUTH))
save("mouth_1", art([
    "................",
    "................",
    "................",
    "................",
    "....kkkkkkkk....",
    "..kkppppppppkk..",
    ".kppwpwpwpwpppk.",
    "kppwwwwwwwwwwppk",
    "kppgwgwgwgwgwppk",
    ".kpppwpwpwpwppk.",
    "..kkppppppppkk..",
    "....kkkkkkkk....",
    "................",
    "................",
    "................",
    "................"], MOUTH))

def puddle():
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    for py in range(S):
        for px in range(S):
            dx, dy = px - 15.5, py - 15.5
            a = math.atan2(dy, dx)
            edge = 12.0 + 1.6 * math.sin(a * 3 + 0.7) + 1.1 * math.sin(a * 7 + 2.0)
            d = math.hypot(dx, dy)
            if d <= edge - 1.5:
                img.putpixel((px, py), hexa("05020a", 245))
            elif d <= edge:
                img.putpixel((px, py), hexa("2a1040", 230))
            elif d <= edge + 1.2:
                img.putpixel((px, py), hexa("5a1a9c", 110))
    return img
save("puddle", puddle())

def bigeye(open_h, pupil):
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    cx = cy = 15.5
    inside = set()
    for py in range(S):
        for px in range(S):
            t = 1 - ((px - cx) / 15.5) ** 2
            if t > 0 and abs(py - cy) <= open_h * t ** 0.8:
                inside.add((px, py))
    line = hexa("05020a")
    for (px, py) in inside:
        d = math.hypot(px - cx, py - cy)
        c = hexa("e9e2d0") if py >= cy - 2 else hexa("b3a6b8")
        if d <= 7.0:
            c = hexa("5a0f2a")
        if d <= 6.0:
            c = hexa("c2224a") if int(math.degrees(math.atan2(py - cy, px - cx)) // 20) % 2 else hexa("e0486a")
        if d <= 2.2:
            c = hexa("8a1238")
        if pupil and abs(px - cx) <= 1.1 and abs(py - cy) <= 5.4:
            c = line
        img.putpixel((px, py), c)
    for py in range(S):
        for px in range(S):
            if (px, py) not in inside and any((px + dx, py + dy) in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0), (0, 2), (0, -2))):
                img.putpixel((px, py), line)
    if open_h < 1:
        for px in range(2, 30):
            yy = 16 if 6 < px < 25 else 15
            img.putpixel((px, yy), line)
            img.putpixel((px, yy + 1), line)
        for px in (5, 10, 15, 20, 25):
            for k in (2, 3, 4):
                img.putpixel((px, (16 if 6 < px < 25 else 15) + k), line)
    elif pupil:
        img.putpixel((12, 12), hexa("ffffff"))
        img.putpixel((13, 12), hexa("ffffff"))
        img.putpixel((12, 13), hexa("ffffff"))
    return img
for i, (h, p) in enumerate([(0.0, False), (3.0, False), (5.5, True), (8.5, True), (11.0, True)]):
    save(f"bigeye_{i}", bigeye(h, p))


# ================================================================ lote 3: Gelo, Água e Terra
from PIL import ImageDraw

def outline(img, col, diag=False):
    """Contorno de 1 px em volta do que já está desenhado."""
    w, h = img.size
    src = img.copy()
    nb = ((1, 0), (-1, 0), (0, 1), (0, -1)) + (((1, 1), (-1, 1), (1, -1), (-1, -1)) if diag else ())
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3] == 0 and any(0 <= x + dx < w and 0 <= y + dy < h and src.getpixel((x + dx, y + dy))[3] > 0 for dx, dy in nb):
                img.putpixel((x, y), col)
    return img

# ---------- Gelo: cacos de espelho, máscaras de porcelana, geada
def shard(with_eye):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(7, 1), (12, 5), (10, 14), (5, 11), (3, 5)], fill=hexa("bfeaff"))
    d.polygon([(7, 1), (12, 5), (8, 7), (3, 5)], fill=hexa("f4fdff"))
    d.polygon([(8, 7), (12, 5), (10, 14)], fill=hexa("7fc4ea"))
    d.line([(7, 1), (8, 7), (10, 14)], fill=hexa("ffffff"))
    if with_eye:
        for x, y, c in ((5, 8, "1a2a4a"), (6, 8, "1a2a4a"), (7, 9, "1a2a4a"), (5, 9, "ffffff"), (6, 9, "3fa7ff"), (6, 10, "1a2a4a"), (5, 10, "1a2a4a")):
            img.putpixel((x, y), hexa(c))
    return outline(img, hexa("2a5a8a"))
save("shard_0", shard(True))
save("shard_1", shard(False))

def mask(S, awake):
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    k = S / 16.0
    cx = S / 2 - 0.5
    for y in range(S):
        for x in range(S):
            ny = (y - S * 0.47) / (S * 0.44)
            nx = (x - cx) / (S * 0.33 * (1.0 - 0.28 * max(0.0, ny)))
            if nx * nx + ny * ny <= 1.0:
                c = hexa("f6f8fb")
                if nx > 0.45:
                    c = hexa("c9d6e6")
                if ny > 0.7:
                    c = hexa("b4c4d8")
                img.putpixel((x, y), c)
    d = ImageDraw.Draw(img)
    dark, blue = hexa("0b1424"), hexa("7fc4ea")
    ey = int(S * 0.42)
    for sx in (-1, 1):
        ex = int(round(cx + sx * S * 0.15))
        if awake:
            d.ellipse([ex - int(1.2 * k), ey - int(1.0 * k), ex + int(1.2 * k), ey + int(1.0 * k)], fill=dark)
            d.line([(ex, ey + int(1.0 * k)), (ex, ey + int(3.2 * k))], fill=blue)  # lágrima congelada
        else:
            d.arc([ex - int(1.4 * k), ey - int(1.2 * k), ex + int(1.4 * k), ey + int(1.2 * k)], 20, 160, fill=dark)
    my = int(S * 0.68)
    if awake:
        d.ellipse([cx - 1.0 * k, my - 0.8 * k, cx + 1.0 * k, my + 1.4 * k], fill=dark)
        d.line([(cx + 0.5, S * 0.06), (cx + 1.5 * k, S * 0.2), (cx + 0.2 * k, S * 0.3)], fill=hexa("5a7a9a"))  # rachadura
    else:
        d.line([(cx - 1.0 * k, my), (cx + 1.0 * k, my)], fill=dark)
    return outline(img, hexa("2a5a8a"))
save("mask_0", mask(16, False))
save("mask_1", mask(16, True))

def frost():
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = 15.5
    def branch(x, y, ang, length, depth):
        x2, y2 = x + math.cos(ang) * length, y + math.sin(ang) * length
        d.line([(x, y), (x2, y2)], fill=hexa("dff6ff", 235 if depth > 1 else 170))
        if depth > 0:
            for t in (0.4, 0.7, 1.0):
                bx, by = x + (x2 - x) * t, y + (y2 - y) * t
                for s in (-1, 1):
                    branch(bx, by, ang + s * 1.0, length * 0.42 * (1.2 - t * 0.5), depth - 1)
    for i in range(6):
        branch(c, c, math.pi * 2 * i / 6 + 0.26, 9.5, 2)
    for y in range(S):
        for x in range(S):
            dd = math.hypot(x - c, y - c)
            if img.getpixel((x, y))[3] == 0 and dd < 15:
                img.putpixel((x, y), (190, 232, 255, int(70 * (1 - dd / 15))))
    return img
save("frost", frost())
save("bigmask_0", mask(32, False))
save("bigmask_1", mask(32, True))

# ---------- Água: peixes no ar, bolhas, águas-vivas, baleia
FISH = {"k": hexa("1b3a6b"), "b": hexa("5fb6e8"), "B": hexa("bfe6ff"), "w": hexa("ffffff"), "o": hexa("ff9a5a"), "e": hexa("0b1424"), "f": hexa("8fd0f4", 200)}
save("fish_0", art([
    "................",
    "................",
    "f...............",
    "ff.....kkkk.....",
    "fbf..kkBBBBkk...",
    ".fbkkBBwwoBBBk..",
    "..fbBBwwooBBeBk.",
    "..fbbBBwwBBBBBk.",
    ".fbkkbbBBBBbbk..",
    "fbf..kkbbbbkk...",
    "ff.....kfkk.....",
    "f.......ff......",
    "................",
    "................",
    "................",
    "................"], FISH))
save("fish_1", art([
    "................",
    "................",
    "................",
    ".......kkkk.....",
    ".ff..kkBBBBkk...",
    "ffbkkBBwwoBBBk..",
    ".fbbBBwwooBBeBk.",
    "..fbbBBwwBBBBBk.",
    "..fbkbbBBBBbbk..",
    ".fbf.kkbbbbkk...",
    ".ff....kkfk.....",
    "..f......ff.....",
    "................",
    "................",
    "................",
    "................"], FISH))
save("bubble", art([
    "..bbbb..",
    ".bw..Bb.",
    "bw....Bb",
    "b......b",
    "b......b",
    "bB....Bb",
    ".bB..Bb.",
    "..bbbb.."], {"b": hexa("bfe6ff", 230), "B": hexa("5fb6e8", 160), "w": hexa("ffffff")}))
def ripple():
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if 14.0 <= d <= 15.3:
                img.putpixel((x, y), hexa("dff6ff", 235))
            elif 12.8 <= d < 14.0:
                img.putpixel((x, y), hexa("5fb6e8", 120))
            elif 8.2 <= d <= 9.0:
                img.putpixel((x, y), hexa("bfe6ff", 150))
    return img
save("ripple", ripple())
JELLY = {"k": hexa("3a2a7a"), "p": hexa("b9a2ff", 220), "P": hexa("e6dcff", 235), "w": hexa("ffffff"), "t": hexa("8fd0f4", 200), "g": hexa("d8ff6a")}
save("jelly_0", art([
    "................",
    ".....kkkkkk.....",
    "...kkPPPPPPkk...",
    "..kPPwPPPPPPpk..",
    ".kPPwPPgPPgPPpk.",
    ".kPPPPPPPPPPppk.",
    ".kpPPPPPPPPpppk.",
    "..kkpkpkkpkpkk..",
    "...t.t.tt.t.t...",
    "...t.t.tt.t.t...",
    "..t..t.t..t..t..",
    "..t.t..t..t..t..",
    "....t...t..t....",
    "....t...t.......",
    "................",
    "................"], JELLY))
save("jelly_1", art([
    "................",
    "................",
    "......kkkk......",
    "....kkPPPPkk....",
    "...kPwPPPPPpk...",
    "...kPwPgPgPpk...",
    "...kPPPPPPPpk...",
    "...kpPPPPPppk...",
    "....kpkkkpkk....",
    ".....t.tt.t.....",
    ".....t.tt.t.....",
    ".....t.tt.t.....",
    "....t..tt..t....",
    "....t..t...t....",
    ".......t........",
    "................"], JELLY))
def whale(tail_up):
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    rnd = random.Random(11)
    for y in range(S):
        for x in range(S):
            nx = (x - 17.0) / 12.0
            top = 15.5 - 6.5 * math.sqrt(max(0.0, 1 - nx * nx)) * (0.75 + 0.25 * max(0, nx))
            bot = 15.5 + 5.0 * math.sqrt(max(0.0, 1 - nx * nx))
            if abs(nx) <= 1 and top <= y <= bot:
                img.putpixel((x, y), hexa("e8f4ff") if y > 16.5 + nx * 1.5 else hexa("1b3a6b") if y < top + 1.5 else hexa("2a5a9a"))
    d = ImageDraw.Draw(img)
    ty = 10 if tail_up else 20
    d.polygon([(6, 15), (2, ty - 3), (0, ty), (3, ty + 2), (6, 17)], fill=hexa("2a5a9a"))
    d.polygon([(16, 20), (13, 25 if tail_up else 23), (19, 21)], fill=hexa("1b3a6b"))  # nadadeira
    for _ in range(14):  # estrelas nas costas
        x, y = rnd.randint(8, 27), rnd.randint(9, 15)
        if img.getpixel((x, y))[:3] in (hexa("2a5a9a")[:3], hexa("1b3a6b")[:3]):
            img.putpixel((x, y), hexa("ffffff") if rnd.random() < 0.5 else hexa("9ee8ff"))
    img.putpixel((25, 15), hexa("0b1424"))
    img.putpixel((25, 14), hexa("ffffff"))
    for y in (18, 19, 20):
        d.line([(19, y), (27 - (y - 18), y)], fill=hexa("b4c4d8") if y % 2 == 0 else hexa("e8f4ff"))
    return outline(img, hexa("0b1a3a"))
save("whale_0", whale(True))
save("whale_1", whale(False))
save("drop", art([
    "...b....",
    "...b....",
    "..bBb...",
    "..bBb...",
    ".bBwBb..",
    ".bBBBb..",
    "..bbb...",
    "........"], {"b": hexa("1b6fb8"), "B": hexa("5fb6e8"), "w": hexa("ffffff")}))

# ---------- Terra: dentes de pedra, cabeças, seixos, rachadura
def stone_fill(img, pts, rnd):
    d = ImageDraw.Draw(img)
    d.polygon(pts, fill=hexa("8a8f96"))
    w, h = img.size
    xs = [p[0] for p in pts]
    mid = (min(xs) + max(xs)) / 2
    for y in range(h):
        for x in range(w):
            if img.getpixel((x, y))[3]:
                c = hexa("a9aeb4") if x < mid - 1 else hexa("6c7178") if x > mid + 2 else hexa("8a8f96")
                r = rnd.random()
                if r < 0.07:
                    c = hexa("585d64")
                elif r < 0.12:
                    c = hexa("c2c6cb")
                img.putpixel((x, y), c)
def tooth():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rnd = random.Random(3)
    stone_fill(img, [(15, 1), (19, 6), (21, 15), (22, 31), (9, 31), (10, 17), (12, 7)], rnd)
    for x, y in ((11, 30), (12, 29), (13, 30), (10, 28), (20, 30), (21, 29), (14, 27), (19, 28), (11, 26)):
        img.putpixel((x, y), hexa("5f8f4a"))  # musgo na base
    for y in range(8, 22, 3):
        img.putpixel((15, y), hexa("585d64"))
        img.putpixel((16, y + 1), hexa("585d64"))
    return outline(img, hexa("2e3238"))
save("tooth", tooth())
def head(awake):
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rnd = random.Random(5)
    stone_fill(img, [(10, 2), (22, 2), (25, 8), (25, 22), (21, 31), (11, 31), (7, 22), (7, 8)], rnd)
    d = ImageDraw.Draw(img)
    dark, deep = hexa("2e3238"), hexa("585d64")
    d.rectangle([9, 9, 22, 10], fill=deep)                 # testa pesada
    d.polygon([(15, 11), (17, 11), (19, 19), (13, 19)], fill=hexa("a9aeb4"))  # nariz
    d.line([(13, 19), (19, 19)], fill=dark)
    for ex in (10, 19):
        d.rectangle([ex, 11, ex + 3, 13], fill=dark)
        if awake:
            d.rectangle([ex + 1, 11, ex + 2, 13], fill=hexa("ffc23d"))
            img.putpixel((ex + 1, 12), hexa("fff3c4"))
    if awake:
        d.rectangle([12, 22, 20, 26], fill=dark)
        for x in (13, 15, 17, 19):
            img.putpixel((x, 22), hexa("c2c6cb"))
            img.putpixel((x, 26), hexa("c2c6cb"))
    else:
        d.line([(12, 24), (20, 24)], fill=dark)
    for x, y in ((8, 20), (9, 21), (24, 18), (23, 19), (12, 3), (13, 2), (20, 30)):
        img.putpixel((x, y), hexa("5f8f4a"))
    return outline(img, dark)
save("head_0", head(False))
save("head_1", head(True))
PEB = {"k": hexa("2e3238"), "s": hexa("8a8f96"), "l": hexa("c2c6cb"), "d": hexa("585d64"), "m": hexa("5f8f4a")}
save("pebble_0", art([
    "........",
    "..kkkk..",
    ".klssdk.",
    ".klsssdk",
    ".kssdddk",
    "..kdddk.",
    "...kkk..",
    "........"], PEB))
save("pebble_1", art([
    "........",
    "...kk...",
    "..klsk..",
    ".klssdk.",
    ".kmsddk.",
    "..kkkk..",
    "........",
    "........"], PEB))
def crack():
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(9)
    def run(x, y, base, length, width):
        pts = [(x, y)]
        for _ in range(int(length / 3)):
            ang = base + rnd.uniform(-0.7, 0.7)
            x, y = x + math.cos(ang) * 3, min(26, max(6, y + math.sin(ang) * 3))
            pts.append((x, y))
        d.line(pts, fill=hexa("15110e"), width=width)
        return pts
    main = run(0, 16, 0.0, 34, 3)
    for i in (3, 6, 8):
        if i < len(main):
            run(main[i][0], main[i][1], rnd.choice((-1, 1)) * 0.9, 9, 1)
    src = img.copy()
    for y in range(S):
        for x in range(S):
            if src.getpixel((x, y))[3] and all(0 <= x + dx < S and 0 <= y + dy < S and src.getpixel((x + dx, y + dy))[3] for dx, dy in ((0, 1), (0, -1))):
                img.putpixel((x, y), hexa("ffb24a"))  # brasa lá no fundo
    return outline(img, hexa("3a2c20", 150))
save("crack", crack())


# ================================================================ lote 4: Ar, Energia e Luz
# ---------- Ar: pássaros de papel, redemoinhos de traço, folhas de papel
CRANE = {"k": hexa("5a6a8a"), "w": hexa("ffffff"), "s": hexa("d5deee"), "d": hexa("a9b6cf"), "r": hexa("e0486a")}
save("crane_0", art([
    "................",
    "......kk........",
    ".....kwwk.......",
    ".....kwwsk......",
    "....kwwwsk......",
    "....kwwssk....kk",
    "kk..kwwssdk.kkwk",
    "kwkkkwwssdkkwwsk",
    ".kwwwwwsssswssk.",
    "..kssswwwsssdk..",
    "...kkdsssddkk...",
    ".....kddrkk.....",
    "......kkk.......",
    "................",
    "................",
    "................"], CRANE))
save("crane_1", art([
    "................",
    "................",
    "................",
    "................",
    "..............kk",
    "kk..........kkwk",
    "kwkkkkkkkkkkwwsk",
    ".kwwwwwwsssswssk.".ljust(16)[:16],
    "..kssswwwsssdk..",
    "...kkwwssddkk...",
    "....kwwssdk.....",
    "....kwssdrk.....",
    ".....kssdk......",
    ".....kkdk.......",
    "......kk........",
    "................"], CRANE))
def swirl():
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    for i in range(140):
        t = i / 139.0
        a = t * math.pi * 3.2
        r = 1.0 + t * 6.2
        x, y = int(round(7.5 + math.cos(a) * r)), int(round(7.5 + math.sin(a) * r))
        if 0 <= x < S and 0 <= y < S:
            img.putpixel((x, y), (255, 255, 255, int(90 + 150 * (1 - t))))
    return img
save("swirl", swirl())
save("paper", art([
    "........",
    ".kkkkk..",
    ".kwwwsk.",
    ".kwllsk.",
    ".kwwwsk.",
    ".kwllsk.",
    "..kkkkk.",
    "........"], {"k": hexa("5a6a8a"), "w": hexa("ffffff"), "s": hexa("d5deee"), "l": hexa("a9b6cf")}))

# ---------- Energia: agulha e linha
def needle():
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    steel, light, dark, glow = hexa("c9d2e0"), hexa("ffffff"), hexa("4a5670"), hexa("ffe66a")
    for x in range(1, 15):
        img.putpixel((x, 8), steel)
        img.putpixel((x, 7), light if x > 3 else steel)
    img.putpixel((15, 7), light)            # ponta
    for x, y in ((1, 6), (2, 6), (3, 6), (1, 9), (2, 9), (3, 9), (0, 7), (0, 8)):
        img.putpixel((x, y), steel)         # cabeça com o furo
    img.putpixel((2, 7), glow)
    img.putpixel((2, 8), glow)
    return outline(img, dark)
save("needle", needle())
def thread():
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    for x in range(S):
        for y, c in ((6, hexa("ffe66a", 70)), (7, hexa("fff6c2", 230)), (8, hexa("ffffff")), (9, hexa("ffe66a", 110))):
            img.putpixel((x, y), c)
    return img
save("thread", thread())
save("knot", art([
    "y......y",
    ".y....y.",
    "..yWWy..",
    "..WwwW..",
    "..WwwW..",
    "..yWWy..",
    ".y....y.",
    "y......y"], {"y": hexa("ffe66a", 220), "W": hexa("fff6c2"), "w": hexa("ffffff")}))


# ================================================================ lote 5: Tempo, Espaço e Vida
# ---------- Tempo: ampulheta, relógios de bolso, chave de corda
HG = {"k": hexa("3b2a5e"), "g": hexa("e8c66a"), "G": hexa("fff4c9"), "d": hexa("a8853a"), "c": hexa("cfe6ff", 150), "s": hexa("ffd76a")}
save("hourglass", art([
    "................",
    "...kkkkkkkkkk...",
    "...kGggggggdk...",
    "....kccccccdk...".ljust(16, ".")[:16],
    "....kcssssck....",
    ".....kcssck.....",
    "......kssk......",
    ".......ks.......",
    "......kcsk......",
    ".....kccsck.....",
    "....kccssssk....",
    "....kcsssssk....",
    "...kGggggggdk...",
    "...kkkkkkkkkk...",
    "................",
    "................"], HG))
def watch(hand):
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([2, 4, 13, 15], fill=hexa("e8c66a"))
    d.ellipse([3, 5, 12, 14], fill=hexa("fff9e6"))
    d.rectangle([7, 1, 8, 3], fill=hexa("a8853a"))
    d.ellipse([6, 0, 9, 2], outline=hexa("e8c66a"))
    for a in range(0, 360, 90):
        x, y = 7.5 + math.cos(math.radians(a)) * 4, 9.5 + math.sin(math.radians(a)) * 4
        img.putpixel((int(round(x)), int(round(y))), hexa("a8853a"))
    cx, cy = 7, 9
    hx, hy = [(0, -3), (3, 0), (-2, 3)][hand]
    d.line([(cx, cy), (cx + hx, cy + hy)], fill=hexa("3b2a5e"))
    d.line([(cx, cy), (cx + (1 if hand != 1 else 0), cy - (2 if hand != 0 else -2))], fill=hexa("8d3fd1"))
    img.putpixel((cx, cy), hexa("3b2a5e"))
    return outline(img, hexa("3b2a5e"))
for i in range(3):
    save(f"watch_{i}", watch(i))
save("key", art([
    "................",
    "..kkk....kkk....",
    ".kgGgk..kgGgk...",
    ".kg.gk..kg.gk...",
    ".kgggkkkkgggk...",
    "..kkkgGGgkkk....",
    ".....kggk.......",
    ".....kgGk.......",
    ".....kggk.......",
    ".....kgGk.......",
    ".....kggkkk.....",
    ".....kgGggk.....",
    ".....kggkk......",
    ".....kgGgk......",
    ".....kkkkk......",
    "................"], {"k": hexa("3b2a5e"), "g": hexa("e8c66a"), "G": hexa("fff4c9")}))

# ---------- Vida: cipó, folha, cogumelos, espinheiro, vagem, a árvore
def vine():
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    stem, dark, leaf, light = hexa("3f8f5a"), hexa("1f5a3a"), hexa("6fc47a"), hexa("b6f0a0")
    for x in range(S):
        y = 8 + int(round(math.sin(x / 16.0 * math.pi * 2) * 1.2))
        img.putpixel((x, y), stem)
        img.putpixel((x, y + 1), dark)
    for (x, y, up) in ((3, 6, True), (8, 11, False), (12, 5, True)):
        for dx, dy, c in ((0, 0, leaf), (1, 0, leaf), (0, -1 if up else 1, leaf), (1, -1 if up else 1, light), (2, -1 if up else 1, leaf), (1, -2 if up else 2, leaf)):
            img.putpixel((x + dx, y + dy), c)
    return img
save("vine", vine())
save("leaf", art([
    "........",
    "....dll.",
    "...dlLl.",
    "..dlLll.",
    ".dlLll..",
    ".dlll...",
    "d.dd....",
    "........"], {"d": hexa("1f5a3a"), "l": hexa("6fc47a"), "L": hexa("b6f0a0")}))
def shroom(stage):
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    h = [3, 6, 9][stage]
    capw = [2, 4, 6][stage]
    d.rectangle([7, 15 - h, 8, 15], fill=hexa("f1e9d6"))
    d.line([(8, 15 - h), (8, 15)], fill=hexa("c9bfa6"))
    top = 15 - h
    d.pieslice([7.5 - capw, top - capw, 7.5 + capw, top + capw], 180, 360, fill=hexa("b06ad8"))
    d.line([(7.5 - capw, top), (7.5 + capw, top)], fill=hexa("6a2f96"))
    if stage > 0:
        for x, y in ((7 - capw // 2, top - capw // 2), (8 + capw // 3, top - 1 - capw // 3), (7, top - capw + 1)):
            img.putpixel((int(x), int(max(0, y))), hexa("d8ff6a"))
    return outline(img, hexa("2a1040"))
for i in range(3):
    save(f"shroom_{i}", shroom(i))
def bramble():
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    def stem(x0, top, sway, phase):
        pts = []
        for y in range(31, top - 1, -1):
            t = (31 - y) / 30.0
            pts.append((x0 + math.sin(t * 6.0 + phase) * sway, y))
        d.line(pts, fill=hexa("2f6b3f"), width=2)
        for i, (x, y) in enumerate(pts):
            if i % 4 == 2:
                s_ = 1 if (i // 4) % 2 == 0 else -1
                d.line([(x + s_, y), (x + s_ * 4, y - 2)], fill=hexa("efe2c0"))
        return pts[-1]
    tips = [stem(13, 3, 3.0, 0.0), stem(20, 9, 2.5, 2.0)]
    for (x, y) in ((10, 22), (17, 15), (22, 24)):
        d.polygon([(x, y), (x + 4, y - 2), (x + 3, y + 2)], fill=hexa("6fc47a"))
    for (x, y) in tips:
        d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=hexa("c9a2f2"))
        img.putpixel((int(x), int(y)), hexa("d8ff6a"))
    return outline(img, hexa("143a22"))
save("bramble", bramble())
POD = {"k": hexa("143a22"), "g": hexa("3f8f5a"), "G": hexa("6fc47a"), "l": hexa("b6f0a0"), "y": hexa("d8ff6a"), "w": hexa("ffffff"), "b": hexa("7a5a3a")}
save("pod_0", art([
    "................",
    ".......kk.......",
    "......kGGk......",
    ".....kGlGgk.....",
    ".....kGlGgk.....",
    "....kGlGGggk....",
    "....kGlGGggk....",
    "....kGlGGggk....",
    "....kGGGgggk....",
    ".....kGgggk.....",
    ".....kGgggk.....",
    "......kggk......",
    "......kbbk......",
    ".....kbbbbk.....",
    "....kkkkkkkk....",
    "................"], POD))
save("pod_1", art([
    "................",
    "..kk........kk..",
    ".kGGk......kggk.",
    ".kGlGk.yy.kgggk.",
    ".kGlGk.yw.kgggk.",
    "..kGlGkywkgggk..",
    "..kGlGkyykgggk..",
    "...kGGGkkgggk...",
    "...kGGGggggk....",
    "....kGGgggk.....",
    "....kGGgggk.....",
    "......kggk......",
    "......kbbk......",
    ".....kbbbbk.....",
    "....kkkkkkkk....",
    "................"], POD))
def tree():
    S = 64
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(42)
    bark, bark_d, bark_l = hexa("7a5a3a"), hexa("4a3422"), hexa("a8845a")
    d.polygon([(27, 63), (37, 63), (35, 40), (34, 28), (30, 28), (29, 40)], fill=bark)
    d.polygon([(22, 63), (27, 63), (29, 52)], fill=bark)       # raízes
    d.polygon([(37, 63), (43, 63), (35, 52)], fill=bark)
    d.line([(32, 40), (22, 26)], fill=bark, width=3)            # galhos
    d.line([(32, 38), (43, 24)], fill=bark, width=3)
    d.line([(32, 34), (32, 18)], fill=bark, width=3)
    for y in range(28, 63, 3):
        d.line([(31, y), (31, y + 1)], fill=bark_d)
        img.putpixel((34, y + 1), bark_l)
    blobs = [(32, 15, 13), (20, 22, 10), (44, 21, 10), (26, 11, 8), (39, 10, 8), (32, 26, 9), (14, 28, 6), (50, 27, 6)]
    canopy = set()
    for cx, cy, r in blobs:
        for y in range(S):
            for x in range(S):
                if math.hypot(x - cx, y - cy) <= r:
                    canopy.add((x, y))
    for (x, y) in canopy:
        shade = (x - 20) * 0.5 + (y - 8)
        c = hexa("b6f0a0") if shade < 6 else hexa("6fc47a") if shade < 22 else hexa("3f8f5a")
        if rnd.random() < 0.08:
            c = hexa("1f5a3a")
        img.putpixel((x, y), c)
    for _ in range(11):                                         # frutos que brilham
        x, y = rnd.choice(sorted(canopy))
        if (x + 1, y + 1) in canopy and (x - 1, y) in canopy:
            for dx, dy, c in ((0, 0, "ffffff"), (1, 0, "d8ff6a"), (0, 1, "d8ff6a"), (1, 1, "9ad84a")):
                img.putpixel((x + dx, y + dy), hexa(c))
    for _ in range(9):                                          # flores
        x, y = rnd.choice(sorted(canopy))
        img.putpixel((x, y), hexa("f1e2ff"))
    return outline(img, hexa("143a22"))
save("tree", tree())


# ================================================================ lote 6: Vapor, Nuvem, Alma, Arcano, Vigília, Sorte
def puff(squash):
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    blobs = [(8, 8, 5.2), (4.5, 9.5, 3.2), (11.5, 9.5, 3.2), (6, 5.5, 3.0), (10, 5.5, 3.0)]
    for y in range(S):
        for x in range(S):
            yy = (y - 8) * (1.25 if squash else 1.0) + 8
            best = min((math.hypot(x - cx, yy - cy) / r for cx, cy, r in blobs))
            if best <= 1.0:
                img.putpixel((x, y), (255, 255, 255, 235) if best < 0.6 else (214, 226, 240, 215) if best < 0.88 else (170, 188, 210, 170))
    for ex in (6, 9):                      # dois olhinhos vazios
        img.putpixel((ex, 8), hexa("5a6a8a"))
        img.putpixel((ex, 9), hexa("5a6a8a"))
    img.putpixel((7, 11), hexa("5a6a8a"))
    img.putpixel((8, 11), hexa("5a6a8a"))
    return img
save("puff_0", puff(False))
save("puff_1", puff(True))

SHEEP = {"k": hexa("5a6a8a"), "w": hexa("ffffff"), "s": hexa("d5deee"), "f": hexa("3b3550"), "e": hexa("ffffff"), "l": hexa("3b3550")}
save("sheep_0", art([
    "................",
    "................",
    "....kkk.kkk.....",
    "...kwwwkwwwkk...",
    "..kwwwwwwwwwwk..",
    ".kwwwwwwwwwwsskk",
    ".kwwwwwwwwwssfff",
    ".kwwwwwwwwsssfef",
    "..kwwwwwwssssfff",
    "..ksswwsssssk.f.",
    "...kkssssskk....",
    "....l.l..l.l....",
    "....l.l..l.l....",
    "...ll.l..l.ll...",
    "................",
    "................"], SHEEP))
save("sheep_1", art([
    "................",
    "....kkk.kkk.....",
    "...kwwwkwwwkk...",
    "..kwwwwwwwwwwk..",
    ".kwwwwwwwwwwsskk",
    ".kwwwwwwwwwssfff",
    ".kwwwwwwwwsssfef",
    "..kwwwwwwssssfff",
    "..ksswwsssssk.f.",
    "...kkssssskk....",
    "...l.l....l.l...",
    "..l...l..l...l..",
    ".l.....ll.....l.",
    "................",
    "................",
    "................"], SHEEP))

CANDLE = {"k": hexa("1a2a4a"), "w": hexa("f1ead8"), "s": hexa("c9bfa6"), "b": hexa("3fd6e8"), "B": hexa("b8f6ff"), "W": hexa("ffffff"), "d": hexa("1b8fa8")}
save("candle_0", art([
    "................",
    ".......b........",
    "......bBb.......",
    "......bBb.......",
    ".....bBWBb......",
    ".....bBWBb......",
    "......dBd.......",
    ".......k........",
    ".....kkkkk......",
    ".....kwwsk......",
    ".....kwwsk......",
    ".....kwwsk......",
    ".....kwssk......",
    ".....kwwsk......",
    "....kkkkkkk.....",
    "................"], CANDLE))
save("candle_1", art([
    "................",
    "........b.......",
    ".......bb.......",
    "......bBBb......",
    ".....bBWBb......",
    "......bWBb......",
    "......dBd.......",
    ".......k........",
    ".....kkkkk......",
    ".....kwwsk......",
    ".....kwwsk......",
    ".....kwwsk......",
    ".....kwssk......",
    ".....kwwsk......",
    "....kkkkkkk.....",
    "................"], CANDLE))
save("soul", art([
    "..bBBb..",
    ".bBWWBb.",
    "bBkWWkBb",
    "bBWWWWBb",
    ".bBWWBb.",
    "..bBBb..",
    "...bb...",
    "....b..."], {"b": hexa("3fd6e8", 200), "B": hexa("b8f6ff"), "W": hexa("ffffff"), "k": hexa("1a2a4a")}))

def gear():
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    c = 7.5
    for y in range(S):
        for x in range(S):
            dx, dy = x - c, y - c
            d = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            tooth_ = math.cos(a * 8) > 0.2
            if d <= 2.0:
                continue
            if d <= 5.4 or (tooth_ and d <= 7.4):
                img.putpixel((x, y), hexa("f3b6ff") if d < 3.2 else hexa("d07cff") if (dx + dy) < 1 else hexa("9a4fd8"))
    return outline(img, hexa("3a1a5c"))
save("gear", gear())

OWL = {"k": hexa("2a2233"), "b": hexa("7a6a8a"), "B": hexa("a898b8"), "l": hexa("d9cfe6"), "y": hexa("ffd23d"), "Y": hexa("fff3a0"), "p": hexa("0b0810"), "o": hexa("e8963a")}
save("owl_0", art([
    "................",
    "..kk........kk..",
    "..kbk......kbk..",
    "..kbbkkkkkkbbk..",
    ".kbBBBBBBBBBBbk.",
    ".kBllllBBllllBk.",
    ".kBlkkklBlkkklk.".ljust(16, ".")[:16],
    ".kBllllBBllllBk.",
    ".kbBBBBooBBBBbk.",
    ".kbBbBBooBBbBbk.",
    ".kbbBbBBBBbBbbk.",
    "..kbbBbBBbBbbk..",
    "..kbbbBbbBbbbk..",
    "...kkbbbbbbkk...",
    ".....kkkkkk.....",
    "................"], OWL))
save("owl_1", art([
    "................",
    "..kk........kk..",
    "..kbk......kbk..",
    "..kbbkkkkkkbbk..",
    ".kbByyyBByyyBbk.",
    ".kByYYYyyYYYyBk.",
    ".kByYpYyyYpYyBk.",
    ".kByYYYyyYYYyBk.",
    ".kbByyyooyyyBbk.",
    ".kbBbBBooBBbBbk.",
    ".kbbBbBBBBbBbbk.",
    "..kbbBbBBbBbbk..",
    "..kbbbBbbBbbbk..",
    "...kkbbbbbbkk...",
    ".....kkkkkk.....",
    "................"], OWL))

def coin(frame):
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    g, G, dk, line = hexa("f2c84b"), hexa("fff6c2"), hexa("b07a1e"), hexa("5a3a0c")
    half = [6, 3, 0.6][frame]
    d.ellipse([7.5 - half, 2, 7.5 + half, 13], fill=g)
    if frame == 0:
        d.ellipse([3, 3.5, 12, 11.5], outline=dk)
        for x, y, c in ((6, 6, line), (9, 6, line), (6, 7, line), (9, 7, line), (5, 9, line), (6, 10, line), (7, 10, line), (8, 10, line), (9, 10, line), (10, 9, line), (4, 4, G), (5, 3, G)):
            img.putpixel((x, y), c)        # uma carinha sorrindo
    elif frame == 1:
        d.line([(6, 4), (6, 11)], fill=G)
        d.line([(9, 4), (9, 11)], fill=dk)
    else:
        d.line([(7, 2), (7, 13)], fill=G)
        d.line([(8, 2), (8, 13)], fill=dk)
    return outline(img, line)
for i in range(3):
    save(f"coin_{i}", coin(i))


with open(JSON, "w") as f:
    json.dump({"textures": NAMES}, f, indent=2)
print(len(NAMES), "sprites")
if "-v" in sys.argv:
    for i, n in enumerate(NAMES):
        print(i, n)
