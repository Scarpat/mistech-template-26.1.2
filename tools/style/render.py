"""Prévia de vários modelos de bloco em grade, com rótulo. Uso: python tools/style/render.py saida.png [--cols N] [--yaw Y] modelo[+modelo] ..."""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))
import blockview2 as bv
from PIL import Image, ImageDraw, ImageFont
args = sys.argv[1:]; out = args.pop(0); cols = 4; yaw = -35; scale = 12
while args and args[0].startswith('--'):
    k = args.pop(0); v = args.pop(0)
    if k == '--cols': cols = int(v)
    elif k == '--yaw': yaw = float(v)
    elif k == '--scale': scale = float(v)
views = []
for a in args:
    qs = []
    for ref in a.split('+'):
        qs += bv.quads(bv.load(ref))
    views.append((a.split('+')[0].split('/')[-1], bv.zview.zdraw(qs, yaw, -25, scale)))
cw = max(v.width for _, v in views); ch = max(v.height for _, v in views) + 16
rows = (len(views) + cols - 1) // cols
S = Image.new('RGBA', (cols * cw, rows * ch), (70, 74, 82, 255)); d = ImageDraw.Draw(S)
for i, (n, v) in enumerate(views):
    x = (i % cols) * cw; y = (i // cols) * ch
    S.alpha_composite(v, (x + (cw - v.width) // 2, y))
    d.text((x + 6, y + ch - 14), n, fill=(235, 235, 235))
S.save(out)
