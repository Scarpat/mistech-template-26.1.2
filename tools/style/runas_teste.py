import os, sys
from estilos4 import V
from kit import COBRE, AMETISTA
import estilo_a4 as A
C = {
 'ingwaz': (6, 10, [".XX.", "X..X", "X..X", ".XX."]),
 'othala': (6, 9, [".XX.", "X..X", ".XX.", "X..X", "X..X"]),
 'dagaz': (5, 9, ["X...X", "XX.XX", "X.X.X", "XX.XX", "X...X"]),
 'algiz': (5, 9, ["X.X.X", ".XXX.", "..X..", "..X..", "..X.."]),
 'tiwaz': (5, 9, ["..X..", ".XXX.", "X.X.X", "..X..", "..X.."]),
 'sowilo': (6, 9, ["...X", "..X.", ".XX.", ".X..", "X..."]),
}
out = sys.argv[1]; os.makedirs(out, exist_ok=True)
for n, (x0, y0, g) in C.items():
    for on in (False, True):
        im = V('furnace_side'); A.strap(im)
        for j, r in enumerate(g):
            for i, c in enumerate(r):
                if c == 'X': im.putpixel((x0 + i, y0 + j), AMETISTA[5] if on else COBRE[1])
        im.save(f'{out}/r_{n}{"_on" if on else ""}.png')
