"""Modelo do Boitatá montaria (corpo inteiro numa só malha: cabeça, pescoço, corpo e cauda em corrente), texturas
(corpo, brilho, armaduras de escama e de oricalco) e prévias.
Uso (raiz do projeto): python tools/boitata/mount_model.py
Saída: textures/entity/boitata_mount*.png e build/boitata_mount_layer.txt (corpo do createBodyLayer para o Java)."""
import os, sys
sys.path.insert(0, 'tools/modelgen')
sys.path.insert(0, 'tools/boitata')
from PIL import Image
from modelgen import Model, hexc, shade
import preview
import zview

T = 'src/main/resources/assets/mistech/textures/entity/'
F1 = 0xe2561a   # laranja-fogo
F2 = 0xffc63a   # amarelo
F3 = 0xa8261a   # vermelho escuro
CORE = 0x3a0c06
BELLY = 0xffd27a
FLAME = 0xffb030


def flametip(p):
    for y in range(p.h):
        for x in range(p.w):
            t = y / max(1, p.h - 1)
            p.px(x, y, 0xfff0a0 if t < 0.25 else 0xffc63a if t < 0.55 else 0xff7a1a, True)


def belly(p):
    for y in range(p.h):
        for x in range(p.w):
            p.px(x, y, BELLY if (y % 2) else 0xf0b050)


def eye(p):
    p.fill(0xffe060, 0.03)
    for y in range(p.h):
        for x in range(p.w):
            p.px(x, y, 0xfff4b0 if (x + y) % 3 else 0xffd040, True)
    p.px(p.w // 2, p.h // 2, 0xff5010, True)


def mouth(p):
    p.fill(CORE, 0.05)


def snout_front(p):
    p.px(1, 1, CORE)
    p.px(p.w - 2, 1, CORE)
    p.row(p.h - 1, CORE)


def stripe_top(p):
    # faixa dourada no dorso
    for y in range(p.h):
        p.px(p.w // 2, y, 0xffc63a)
        if p.w > 6:
            p.px(p.w // 2 - 1, y, 0xf09a2a)


SEGMENTS = [  # nome, pai, pivô, tamanho (w, h, d)
    ('body1', 'body', (0, 0, 4.5), (9, 8, 9)),
    ('body2', 'body1', (0, 0.5, 9), (8, 7, 8)),
    ('body3', 'body2', (0, 0.5, 8), (7, 6, 8)),
    ('tail0', 'body3', (0, 0.5, 8), (6, 5, 7)),
    ('tail1', 'tail0', (0, 0.5, 7), (5, 4, 7)),
    ('tail2', 'tail1', (0, 0.5, 7), (4, 3, 6)),
    ('tail3', 'tail2', (0, 0.5, 6), (3, 2, 6)),
]
ARMORED = {'body', 'body1', 'body2', 'body3', 'tail0', 'tail1', 'neck0', 'neck1', 'head', 'snout'}


def build():
    M = Model('boitata_mount', 128, seed=41)
    parts = {}
    body = M.part('body', pivot=(0, 20, 0)).box((-4.5, -4, -4.5), (9, 8, 9), F1, pattern='scales', faces={'bottom': belly, 'top': stripe_top})
    parts['body'] = body
    body.child('fin_body', pivot=(0, -4, 0), rot=(-0.5, 0, 0)).box((-0.5, -4, -1.5), (1, 4, 3), FLAME, faces={k: flametip for k in ('front', 'back', 'left', 'right')}, glow=True)
    # pescoço e cabeça (para a frente, -Z), levantando
    n0 = body.child('neck0', pivot=(0, -0.5, -4.5), rot=(-0.45, 0, 0)).box((-3.5, -3.5, -7), (7, 7, 7), F1, pattern='scales', faces={'bottom': belly, 'top': stripe_top})
    n0.child('fin_neck0', pivot=(0, -3.5, -3.5), rot=(-0.6, 0, 0)).box((-0.5, -3, -1), (1, 3, 2), FLAME, faces={k: flametip for k in ('front', 'back', 'left', 'right')}, glow=True)
    n1 = n0.child('neck1', pivot=(0, 0, -7), rot=(-0.25, 0, 0)).box((-3, -3, -6), (6, 6, 6), F1, pattern='scales', faces={'bottom': belly, 'top': stripe_top})
    head = n1.child('head', pivot=(0, 0, -6), rot=(0.6, 0, 0)).box((-4.5, -4, -9), (9, 6, 10), F1, pattern='scales', faces={'bottom': mouth})
    head.child('brow', pivot=(0, 0, 0)).box((-5, -5, -8.5), (10, 1, 4), F3)
    snout = head.child('snout', pivot=(0, 0, 0)).box((-3, -2.5, -14), (6, 4, 5), F1, pattern='scales', faces={'front': snout_front, 'bottom': mouth})
    snout.child('right_fang', pivot=(-2, 1.5, -13)).box((-0.5, 0, -0.5), (1, 2, 1), 0xfff8e8, noise=0.01)
    snout.child('left_fang', pivot=(2, 1.5, -13)).box((-0.5, 0, -0.5), (1, 2, 1), 0xfff8e8, noise=0.01)
    snout.child('right_whisker', pivot=(-3, 0, -12), rot=(0, 0.6, 0.25)).box((-7, -0.5, -0.5), (7, 1, 1), F2)
    snout.child('left_whisker', pivot=(3, 0, -12), rot=(0, -0.6, -0.25)).box((0, -0.5, -0.5), (7, 1, 1), F2)
    head.child('jaw', pivot=(0, 2, -2), rot=(0.15, 0, 0)).box((-3.5, 0, -11), (7, 2, 11), F3, pattern='scales', faces={'top': mouth, 'bottom': belly})
    head.child('right_eye', pivot=(0, 0, 0)).box((-6, -4.5, -8), (3, 3, 3), 0xffe060, faces={k: eye for k in ('right', 'front', 'top', 'back', 'bottom', 'left')}, glow=True)
    head.child('left_eye', pivot=(0, 0, 0)).box((3, -4.5, -8), (3, 3, 3), 0xffe060, faces={k: eye for k in ('left', 'front', 'top', 'back', 'bottom', 'right')}, glow=True)
    for i, (z, h, r) in enumerate(((-6, 4, -0.5), (-3, 6, -0.7), (0, 5, -0.9))):
        head.child('crest_' + 'abc'[i], pivot=(0, -4, z), rot=(r, 0, 0)).box((-1, -h, -1.5), (2, h, 3), FLAME,
                                                                        faces={k: flametip for k in ('front', 'back', 'left', 'right')}, glow=True)
    parts['head'] = head
    # corpo e cauda (para trás, +Z)
    for name, parent, pivot, (w, h, d) in SEGMENTS:
        p = parts[parent].child(name, pivot=pivot).box((-w / 2, -h / 2, 0), (w, h, d), F1, pattern='scales',
                                                       faces={'bottom': belly, 'top': stripe_top} if w >= 5 else {'bottom': belly})
        parts[name] = p
        if not name.startswith('tail') or name in ('tail0', 'tail1'):
            fh = max(2, h // 2)
            p.child('fin_' + name, pivot=(0, -h / 2, d / 2), rot=(-0.5, 0, 0)).box((-0.5, -fh, -1.5), (1, fh, 3), FLAME,
                                                                              faces={k: flametip for k in ('front', 'back', 'left', 'right')}, glow=True)
    flame = parts['tail3'].child('tail_flame', pivot=(0, 0, 6))
    flame.box((-2, -2, 0), (4, 4, 4), FLAME, faces={k: flametip for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}, glow=True)
    flame.box((-1, -1, 4), (2, 2, 4), FLAME, faces={k: flametip for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}, glow=True)
    return M


def armor_texture(M, path, plate, rim, gem):
    """Placas só nas caixas do corpo/pescoço/cabeça (sem a barriga); o resto transparente."""
    img = Image.new('RGBA', (M.tex_w, M.tex_h), (0, 0, 0, 0))
    px = img.load()
    for part in M.parts:
        if part.name not in ARMORED:
            continue
        b = part.boxes[0]
        w, h, d = [int(round(s)) for s in b.size]
        u, v = b.uv
        regions = {'top': (u + d, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
                   'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
        for face, (x0, y0, fw, fh) in regions.items():
            for yy in range(fh):
                if face != 'top' and yy >= fh - max(1, fh // 3):
                    continue  # deixa a parte de baixo das laterais aparecendo
                for xx in range(fw):
                    edge = xx in (0, fw - 1) or yy in (0, fh - 1) or (face != 'top' and yy == fh - max(1, fh // 3) - 1)
                    c = rim if edge else shade(plate, 1.0 + (0.08 if (xx + yy) % 4 == 0 else 0) - 0.12 * (yy / max(1, fh)))
                    px[x0 + xx, y0 + yy] = c[:3] + (255,)
            if face == 'top' and fw >= 5 and fh >= 3:
                px[x0 + fw // 2, y0 + fh // 2] = gem + (255,)
    img.save(path)


if __name__ == '__main__':
    M = build()
    M.pack()
    M.paint(T + 'boitata_mount.png', T + 'boitata_mount_glow.png')
    armor_texture(M, T + 'boitata_mount_armor_scale.png', hexc(0x241c22), hexc(0xd8a640), hexc(0xff6a20))
    armor_texture(M, T + 'boitata_mount_armor_orichalcum.png', hexc(0xe0a040), hexc(0xfff0b0), hexc(0x40e0d0))
    os.makedirs('build', exist_ok=True)
    open('build/boitata_mount_layer.txt', 'w').write(M.java())
    preview.draw = zview.zdraw
    preview.render(M, T + 'boitata_mount.png', 'build/boitata_mount_view.png', scale=6, yaws=(-40, 40, 90, 180 + 30), pitch=20)
    print(M.tex_w, M.tex_h)
