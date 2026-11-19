"""Rosto das estátuas rúnicas: o material da estátua com os olhos/boca do rosto da Estátua de Argila.
Antes elas herdavam o rosto cinza da argila. Idempotente.
Uso (na pasta do projeto): python3 tools/pendencias/statue_faces.py"""
import json
from PIL import Image

A = 'src/main/resources/assets/mistech/'
V = 'vanilla/assets/minecraft/textures/'


def tex(ref):
    ns, path = ref.split(':')
    p = (V if ns == 'minecraft' else A + 'textures/') + path + '.png'
    im = Image.open(p).convert('RGBA')
    return im.crop((0, 0, 16, 16))


face = Image.open(A + 'textures/block/clay_statue_face.png').convert('RGBA')
clay = tex('minecraft:block/clay')
features = []
for y in range(16):
    for x in range(16):
        a, b = face.getpixel((x, y)), clay.getpixel((x, y))
        if sum(abs(a[i] - b[i]) for i in range(3)) > 60:
            features.append((x, y, a))
for kind in ('collector', 'crafter', 'keeper', 'messenger', 'sorter'):
    path = A + 'models/block/%s_statue.json' % kind
    m = json.load(open(path))
    out = tex(m['textures']['clay']).copy()
    for x, y, c in features:
        out.putpixel((x, y), c)
    out.save(A + 'textures/block/%s_statue_face.png' % kind)
    m['textures']['face'] = 'mistech:block/%s_statue_face' % kind
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(m, f, ensure_ascii=False, indent=2)
        f.write('\n')
print(len(features), 'pixels de rosto')
