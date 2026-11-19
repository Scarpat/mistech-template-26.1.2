#!/bin/bash
# Gera tudo e monta a previa de todas as plantas em tools/plants/out/all.png
set -e
cd "$(dirname "$0")/../.."
mkdir -p tools/plants/out
python3 tools/plants/gen.py
for p in hemp tobacco coffee rue; do python3 tools/plants/preview.py tools/plants/out/$p.png $p 8 ${1:--30}; done
python3 tools/plants/preview.py tools/plants/out/mandrake.png mandrake 4 ${1:--30}
python3 - <<'E'
from PIL import Image
ims = [Image.open(f'tools/plants/out/{p}.png') for p in ['hemp', 'tobacco', 'coffee', 'rue', 'mandrake']]
W = max(i.width for i in ims); H = sum(i.height for i in ims)
S = Image.new('RGBA', (W, H), (70, 74, 82, 255)); y = 0
for i in ims:
    S.paste(i, (0, y)); y += i.height
S.save('tools/plants/out/all.png'); print(S.size)
E
