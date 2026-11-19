"""Previa: python3 tools/plants/preview.py saida.png planta N [yaw]  -> fileira com os N estagios sobre terra arada."""
import sys
sys.path.insert(0, 'tools/style'); sys.path.insert(0, 'tools/boitata')
import blockview2 as bv
out, plant, n = sys.argv[1], sys.argv[2], int(sys.argv[3])
yaw = float(sys.argv[4]) if len(sys.argv) > 4 else -30
qs = []
farm = bv.quads(bv.load('minecraft:block/farmland'))
for i in range(n):
    dx = i * 20
    for pts, c in farm:
        qs.append(([(x + dx, y + 16, z) for x, y, z in pts], c))
    for pts, c in bv.quads(bv.load(f'mistech:block/{plant}_age{i}')):
        qs.append(([(x + dx, y, z) for x, y, z in pts], c))
bv.zview.zdraw(qs, yaw, -20, 9).save(out)
