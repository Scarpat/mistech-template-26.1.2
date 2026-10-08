import sys
from magus_models import build
from magus_lib import render
M = build()
used = M.pack(); print('linhas usadas', used)
M.paint()
M.img.save('atlas.png'); M.glow.save('atlas_glow.png')
for f in (sys.argv[1:] or ['earth', 'air', 'water', 'fire', 'quint']):
    render(M, {f}, f'prev_{f}.png', scale=5, pitch=-18)
    print(f)
