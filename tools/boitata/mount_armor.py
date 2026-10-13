"""Armadura de montaria do Boitatá (v2): peças 3D de verdade por cima do corpo — elmo com chifres, placas de bochecha e
focinheira; colares no pescoço; sela com arção, aba e estribos; placas de dorso com espinhos; anéis na cauda.
A malha repete a corrente do corpo (mesmos nomes e pivôs) e copia a pose dele a cada quadro.
Duas texturas com a mesma UV: escamas (aço enegrecido + ouro + couro vermelho) e oricalco (ouro polido + turquesa + tecido azul).
Uso (raiz do projeto): python tools/boitata/mount_armor.py   (depois de mount_model.py)"""
import sys
sys.path.insert(0, 'tools/modelgen')
sys.path.insert(0, 'tools/boitata')
from PIL import Image
from modelgen import Model, hexc, shade
import preview
import zview
import mount_model

T = 'src/main/resources/assets/mistech/textures/entity/'

PALETTES = {
    'scale': dict(metal=(0x2a2430, 0x4a4050, 0x6a5e72), rim=0xd8a640, rim_hi=0xffe8a0, gem=0xff5a1a, cloth=(0x6a1a14, 0x8a2a1e), stud=0xffd060),
    'orichalcum': dict(metal=(0xa86a20, 0xe0a040, 0xffd070), rim=0xfff4d0, rim_hi=0xffffff, gem=0x30d8c8, cloth=(0x1a4a6a, 0x2a6a8a), stud=0x30d8c8),
}


def lerp(a, b, t):
    a, b = hexc(a), hexc(b)
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def plate_painter(pal, gem=False, engrave=True):
    """Lamelas: fileiras de 2 px desencontradas (como escamas de metal), filete dourado em cima e rebites alternados."""
    lo, mid, hi = pal['metal']

    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                row = y // 2
                lx = (x + (row % 2) * 2) % 4
                t = 0.55 - 0.25 * (y / max(1, p.h - 1))
                if y % 2 == 0:
                    t += 0.22 if lx in (1, 2) else 0.08   # luz em cima da lamela
                else:
                    t -= 0.10 if lx in (0, 3) else 0.0     # sombra embaixo
                t = max(0.0, min(1.0, t))
                c = lerp(lo, mid, t * 2) if t < 0.5 else lerp(mid, hi, (t - 0.5) * 2)
                p.px(x, y, c)
        if p.w >= 2 and p.h >= 2:
            for x in range(p.w):
                p.px(x, 0, pal['rim_hi'] if x % 4 == 0 else pal['rim'])
        if engrave and p.h >= 4 and p.w >= 4:
            for x in range(1, p.w - 1, 3):
                p.px(x, p.h - 1, pal['stud'])
        if gem and p.w >= 3 and p.h >= 3:
            cx, cy = p.w // 2, p.h // 2
            p.px(cx, cy, pal['gem'], True)
            p.px(cx, cy - 1, pal['rim_hi'])
            p.px(cx, cy + 1, pal['rim'])
            if p.w >= 6:
                p.px(cx - 1, cy, pal['rim'])
                p.px(cx + 1, cy, pal['rim'])
    return paint


def rim_painter(pal):
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.px(x, y, pal['rim_hi'] if (x + y) % 4 == 0 else pal['rim'])
    return paint


def cloth_painter(pal):
    a, b = pal['cloth']

    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.px(x, y, hexc(a) if (x // 2 + y) % 2 else hexc(b))
        for x in range(p.w):  # costura dourada
            p.px(x, 0, pal['rim'])
            if p.h > 2:
                p.px(x, p.h - 1, pal['rim'])
    return paint


def horn_painter(pal):
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.px(x, y, lerp(0xf4e8d0, 0x8a7060, y / max(1, p.h - 1)) if pal is PALETTES['scale'] else lerp(0xffffff, 0xe0a040, y / max(1, p.h - 1)))
    return paint


ALLF = ('top', 'bottom', 'front', 'back', 'left', 'right')


def build(pal):
    M = Model('boitata_mount_armor', 128, seed=7)
    plate = plate_painter(pal)
    plate_gem = plate_painter(pal, gem=True)
    rim = rim_painter(pal)
    cloth = cloth_painter(pal)
    horn = horn_painter(pal)
    F = lambda fn: {k: fn for k in ALLF}
    C = 0x808080

    body = M.part('body', pivot=(0, 20, 0))
    # sela: assento de tecido, arção (frente) e patilha (trás) de metal, abas laterais, estribos
    body.box((-4, -5.2, -3.5), (8, 1.2, 7), C, faces=F(cloth))
    body.box((-2, -7.2, -4.2), (4, 2, 1.5), C, faces=F(plate_gem))
    body.box((-3.5, -7.5, 2.5), (7, 2.5, 1.5), C, faces=F(plate))
    body.box((-5.2, -4.2, -3), (0.8, 5, 6), C, faces=F(cloth))
    body.box((4.4, -4.2, -3), (0.8, 5, 6), C, faces=F(cloth))
    for s in (-1, 1):
        x = -5.6 if s < 0 else 5.0
        body.box((x, -3.5, -0.5), (0.6, 7, 1), C, faces=F(rim))
        body.box((x - 0.7 if s < 0 else x, 3.5, -1), (1.3, 0.8, 2), C, faces=F(rim))
    # testeira do corpo (placa de cima, à frente da sela)
    body.box((-4.8, -4.6, -4.8), (9.6, 0.8, 1.6), C, faces=F(plate))

    n0 = body.child('neck0', pivot=(0, -0.5, -4.5), rot=(-0.45, 0, 0))
    n0.box((-4, -4, -5.5), (8, 8, 2), C, faces=F(plate_gem))       # colar
    n0.box((-3.8, -4.4, -3.5), (7.6, 1, 3.5), C, faces=F(plate))    # placa de cima
    n0.box((-0.5, -6, -3), (1, 2, 2), C, faces=F(horn))             # espinho
    n1 = n0.child('neck1', pivot=(0, 0, -7), rot=(-0.25, 0, 0))
    n1.box((-3.5, -3.5, -4.5), (7, 7, 1.8), C, faces=F(plate))      # colar
    n1.box((-0.5, -5.2, -3.5), (1, 2, 2), C, faces=F(horn))
    head = n1.child('head', pivot=(0, 0, -6), rot=(0.6, 0, 0))
    head.box((-4.8, -5, -9.4), (9.6, 1.2, 10), C, faces=F(plate))   # elmo (topo)
    head.box((-3.5, -4.6, -9.9), (7, 3, 0.8), C, faces=F(plate_gem))  # testa
    head.box((-5.3, -3.6, -8), (0.8, 4, 7), C, faces=F(plate))      # bochechas
    head.box((4.5, -3.6, -8), (0.8, 4, 7), C, faces=F(plate))
    head.box((-0.6, -6.2, -8.5), (1.2, 1.2, 8), C, faces=F(rim))    # crista do elmo
    # chifres: base e ponta, para trás e para cima
    for s in (-1, 1):
        hb = head.child('horn_%s' % ('r' if s < 0 else 'l'), pivot=(s * 3.2, -4.6, -1), rot=(0.7, s * 0.35, 0))
        hb.box((-0.9, -4, -0.9), (1.8, 4, 1.8), C, faces=F(horn))
        hb.child('horn_tip_%s' % ('r' if s < 0 else 'l'), pivot=(0, -4, 0), rot=(0.45, 0, 0)).box((-0.5, -3, -0.5), (1, 3, 1), C, faces=F(horn))
    sn = head.child('snout', pivot=(0, 0, 0))
    sn.box((-3.4, -3.1, -13.6), (6.8, 0.8, 4.6), C, faces=F(plate))  # focinheira
    sn.box((-0.5, -3.9, -13.2), (1, 0.8, 4), C, faces=F(rim))

    # corpo e cauda: placas de dorso com espinho e placas laterais; anéis na cauda
    parent = body
    for name, _, pivot, (w, h, d) in mount_model.SEGMENTS:
        p = parent.child(name, pivot=pivot)
        if name.startswith('body'):
            p.box((-w / 2 - 0.3, -h / 2 - 0.9, 0.6), (w + 0.6, 1, d - 1.2), C, faces=F(plate_gem))
            p.box((-w / 2 - 0.6, -h / 2, 1.2), (0.6, h * 0.55, d - 2.4), C, faces=F(plate))
            p.box((w / 2, -h / 2, 1.2), (0.6, h * 0.55, d - 2.4), C, faces=F(plate))
            p.box((-0.5, -h / 2 - 2.8, d / 2 - 2), (1, 2, 2.5), C, faces=F(horn))
        elif name in ('tail0', 'tail1'):
            p.box((-w / 2 - 0.4, -h / 2 - 0.4, d / 2 - 1.2), (w + 0.8, h + 0.8, 2.4), C, faces=F(rim))
        parent = p
    return M


if __name__ == '__main__':
    models = {}
    for name, pal in PALETTES.items():
        M = build(pal)
        M.pack()
        M.paint(T + 'boitata_mount_armor_%s.png' % name, T + 'boitata_mount_armor_%s_glow.png' % name)
        models[name] = M
    open('build/boitata_mount_armor_layer.txt', 'w').write(models['scale'].java())
    # prévia: corpo + armadura
    body = mount_model.build()
    body.pack()
    for name, M in models.items():
        btex = Image.open(T + 'boitata_mount.png').convert('RGBA')
        atex = Image.open(T + 'boitata_mount_armor_%s.png' % name).convert('RGBA')
        sheets = []
        for yaw in (-40, 40, 90):
            quads = []
            for model, tex in ((body, btex), (M, atex)):
                for part in model.parts:
                    Mx, Tr = preview.world_transform(part, {})
                    for b in part.boxes:
                        quads += preview.box_quads(b, Mx, Tr, tex)
            sheets.append(zview.zdraw(quads, yaw, -25, 7))
        W = sum(s.width for s in sheets)
        H = max(s.height for s in sheets)
        out = Image.new('RGBA', (W, H), (70, 74, 82, 255))
        x = 0
        for s in sheets:
            out.alpha_composite(s, (x, 0))
            x += s.width
        out.save('build/boitata_mount_armor_%s_view.png' % name)
    print('ok', models['scale'].tex_h)
