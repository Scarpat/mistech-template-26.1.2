"""Desenho com z-buffer (cópia do zdraw de tools/mage/robe_design.py) para as prévias do modelgen."""
import math
from PIL import Image


def zdraw(quads, yaw, pitch, scale):
    import numpy as np
    ya, pa = math.radians(yaw), math.radians(pitch)
    proj = []
    for pts, c in quads:
        sp = []
        for x, y, z in pts:
            X, Y, Z = -x, -y, z
            xr = X * math.cos(ya) - Z * math.sin(ya)
            zr = X * math.sin(ya) + Z * math.cos(ya)
            yr = Y * math.cos(pa) - zr * math.sin(pa)
            zr2 = Y * math.sin(pa) + zr * math.cos(pa)
            sp.append((xr, yr, zr2))
        proj.append((sp, c))
    xs = [p[0] for q in proj for p in q[0]]
    ys = [p[1] for q in proj for p in q[0]]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    W = int((maxx - minx) * scale) + 20
    H = int((maxy - miny) * scale) + 20
    zbuf = np.full((H, W), np.inf)
    col = np.zeros((H, W, 4), dtype=np.uint8)
    col[..., :3] = (70, 74, 82)
    col[..., 3] = 255
    for sp, c in proj:
        P = [((x - minx) * scale + 10, (maxy - y) * scale + 10, z) for x, y, z in sp]
        for tri in ((P[0], P[1], P[2]), (P[0], P[2], P[3])):
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = tri
            bx0, bx1 = int(max(0, min(x0, x1, x2))), int(min(W - 1, max(x0, x1, x2)) + 1)
            by0, by1 = int(max(0, min(y0, y1, y2))), int(min(H - 1, max(y0, y1, y2)) + 1)
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-9:
                continue
            gx, gy = np.meshgrid(np.arange(bx0, bx1) + 0.5, np.arange(by0, by1) + 0.5)
            a = ((y1 - y2) * (gx - x2) + (x2 - x1) * (gy - y2)) / den
            b = ((y2 - y0) * (gx - x2) + (x0 - x2) * (gy - y2)) / den
            g = 1 - a - b
            inside = (a >= -1e-6) & (b >= -1e-6) & (g >= -1e-6)
            z = a * z0 + b * z1 + g * z2
            sub = zbuf[by0:by1, bx0:bx1]
            m = inside & (z < sub)
            sub[m] = z[m]
            col[by0:by1, bx0:bx1][m] = (c[0], c[1], c[2], 255)
    return Image.fromarray(col, 'RGBA')


