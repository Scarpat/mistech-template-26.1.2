"""Previa: python tools/style2/preview.py <pasta_out> <saida.png> <modelo>[,<yaw>] ... [--gif saida.gif modelo_prefixo N]"""
import sys, os, shutil, glob
ov = os.path.abspath(sys.argv[1])
for p in glob.glob(ov + '/textures/*.png'): shutil.copy(p, ov)
os.environ['STYLE_OVERRIDE'] = ov
root = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')); os.chdir(root)
sys.path.insert(0, 'tools/style'); sys.path.insert(0, 'tools/boitata')
import blockview2 as bv
from PIL import Image, ImageDraw
args = sys.argv[2:]
gif = None
if '--gif' in args:
    i = args.index('--gif'); gif = args[i + 1:i + 4]; args = args[:i]
out = args.pop(0)
def view(ref, yaw, sc=14, bg=(70, 74, 82, 255)):
    qs = []
    for r in ref.split('+'): qs += bv.quads(bv.load(r))
    v = bv.zview.zdraw(qs, yaw, -25, sc)
    return v
vs = []
for a in args:
    ref, yaw = (a.split(',') + ['-35'])[:2]
    vs.append((ref.split(':')[-1].split('/')[-1], view(ref, float(yaw))))
cw = max(v.width for _, v in vs); ch = max(v.height for _, v in vs) + 18
S = Image.new('RGBA', (cw * len(vs), ch), (70, 74, 82, 255)); d = ImageDraw.Draw(S)
for i, (n, v) in enumerate(vs):
    S.alpha_composite(v, (i * cw + (cw - v.width) // 2, 0)); d.text((i * cw + 6, ch - 15), n, fill=(230, 230, 230))
S.save(out)
if gif:
    path, pref, n = gif; fr = []
    for i in range(int(n)):
        v = view(f'{pref}{i}', -35)
        b = Image.new('RGBA', v.size, (70, 74, 82, 255)); b.alpha_composite(v); fr.append(b.convert('P', palette=Image.ADAPTIVE))
    fr[0].save(path, save_all=True, append_images=fr[1:], duration=70, loop=0)
