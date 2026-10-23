#!/usr/bin/env python3
"""Modelos e texturas das invocações (Golem de Pedra, Guardião de Madeira) e textura do Lobo Sombrio.
A geometria é definida aqui uma vez: o script empacota as UVs, gera SummonModels.java (LayerDefinition com as mesmas
partes do Golem de Ferro — head, body, right_arm, left_arm, right_leg, left_leg — para reaproveitar a animação dele)
e pinta as texturas 128x128 face por face. Uso: python3 tools/spellcraft/gen_summons.py"""
import io, os, random, zipfile, glob
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
TEX = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'mistech', 'textures', 'entity')
JAVA = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'mistech', 'mistechmod', 'spellcraft', 'client', 'SummonModels.java')
SIZE = 128


def hx(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


# ------------------------------------------------------------------ materiais (cor por pixel, estável)
def mat_stone(face, x, y, w, h, rnd):
    base = ['#5c5c62', '#6b6b72', '#7a7a82', '#55555b']
    c = base[(x * 7 + y * 13 + rnd.randint(0, 3)) % 4]
    if face == 'up' and rnd.random() < 0.55:
        c = ['#3f6b2a', '#4f8034', '#5f9440'][rnd.randint(0, 2)]
    elif face != 'down' and y <= 1 and rnd.random() < 0.35:
        c = '#4f8034'
    elif rnd.random() < 0.06:
        c = '#3c3c42'  # rachadura
    return hx(c)


def mat_stone_dark(face, x, y, w, h, rnd):
    return hx(['#3a3a40', '#46464c', '#505056'][(x + y * 3 + rnd.randint(0, 2)) % 3])


def mat_rune(face, x, y, w, h, rnd):
    if face in ('north',) and (x in (1, w - 2) or y in (1, h - 2) or x == w // 2 or y == h // 2):
        return hx('#7ff0ff' if (x + y) % 3 else '#d8ffff')
    return mat_stone_dark(face, x, y, w, h, rnd)


def mat_bark(face, x, y, w, h, rnd):
    if face in ('up', 'down'):
        r = ((x - w / 2) ** 2 + (y - h / 2) ** 2) ** 0.5
        return hx(['#9a7448', '#b08a58', '#86603a'][int(r) % 3])
    stripe = ['#4a3220', '#5a3e28', '#6b4a30', '#3e2a1a'][(x * 3 + rnd.randint(0, 1)) % 4]
    if rnd.random() < 0.08:
        stripe = '#3b6e28'  # musgo
    return hx(stripe)


def mat_leaf(face, x, y, w, h, rnd):
    c = ['#2f6a24', '#3c8030', '#4b963a', '#285a1e'][rnd.randint(0, 3)]
    if rnd.random() < 0.07:
        c = ['#f2a8d8', '#ffd6ee', '#e070b0'][rnd.randint(0, 2)]
    return hx(c)


def mat_moss(face, x, y, w, h, rnd):
    return hx(['#3f6b2a', '#4f8034', '#5f9440', '#36592a'][rnd.randint(0, 3)])


MATS = {'stone': mat_stone, 'stone_dark': mat_stone_dark, 'rune': mat_rune, 'bark': mat_bark, 'leaf': mat_leaf, 'moss': mat_moss}


# ------------------------------------------------------------------ geometria
# parte: (nome, offset (x,y,z), [caixas (x,y,z,w,h,d, material, detalhe)])
STONE_GOLEM = [
    ('head', (0, -7, -2), [(-4, -11, -4.5, 8, 9, 8, 'stone', 'eyes_cyan'), (-4.5, -9, -5.5, 9, 2, 2, 'stone_dark', None),
                           (-3, -12, -3, 6, 1, 6, 'moss', None)]),
    ('body', (0, -7, 0), [(-10, -3, -6.5, 20, 13, 12, 'stone', None), (-5, 10, -3.5, 10, 6, 7, 'stone_dark', None),
                          (-3, 1, -7.5, 6, 6, 1, 'rune', None), (-12, -5, -4, 5, 5, 8, 'stone', None), (7, -5, -4, 5, 5, 8, 'stone', None)]),
    ('right_arm', (0, -7, 0), [(-15, -3, -3.5, 5, 22, 7, 'stone', None), (-16, 19, -4.5, 7, 8, 9, 'stone_dark', None)]),
    ('left_arm', (0, -7, 0), [(10, -3, -3.5, 5, 22, 7, 'stone', None), (9, 19, -4.5, 7, 8, 9, 'stone_dark', None)]),
    ('right_leg', (-4, 11, 0), [(-3.5, -3, -3, 7, 16, 6, 'stone', None)]),
    ('left_leg', (5, 11, 0), [(-3.5, -3, -3, 7, 16, 6, 'stone', None)]),
]
WOOD_GUARDIAN = [
    ('head', (0, -9, -1), [(-3.5, -10, -3.5, 7, 10, 7, 'bark', 'eyes_amber'), (-5.5, -15, -5.5, 11, 6, 11, 'leaf', None),
                           (-6, -19, -0.5, 2, 6, 1, 'bark', None), (4, -19, -0.5, 2, 6, 1, 'bark', None)]),
    ('body', (0, -7, 0), [(-6, -5, -4, 12, 18, 8, 'bark', None), (-6.5, 5, -4.5, 13, 3, 9, 'moss', None),
                          (-7, -6, -5, 14, 3, 10, 'leaf', None)]),
    ('right_arm', (0, -7, 0), [(-10, -3, -2, 4, 24, 4, 'bark', None), (-11, 21, -2.5, 6, 6, 5, 'bark', None),
                               (-11, -4, -3, 6, 4, 6, 'leaf', None)]),
    ('left_arm', (0, -7, 0), [(6, -3, -2, 4, 24, 4, 'bark', None), (5, 21, -2.5, 6, 6, 5, 'bark', None),
                              (5, -4, -3, 6, 4, 6, 'leaf', None)]),
    ('right_leg', (-3.5, 11, 0), [(-2.5, -2, -2.5, 5, 13, 5, 'bark', None), (-3.5, 11, -4, 7, 2, 8, 'bark', None)]),
    ('left_leg', (3.5, 11, 0), [(-2.5, -2, -2.5, 5, 13, 5, 'bark', None), (-3.5, 11, -4, 7, 2, 8, 'bark', None)]),
]


def pack(parts):
    """Prateleiras: devolve [(parte, caixa, u, v)]. Cada caixa ocupa 2(d+w) x (d+h)."""
    out, x, y, row = [], 0, 0, 0
    boxes = [(p, b) for p in parts for b in p[2]]
    for p, b in sorted(boxes, key=lambda pb: -(int(pb[1][5]) + int(pb[1][4]))):
        w, h, d = int(round(b[3])), int(round(b[4])), int(round(b[5]))
        bw, bh = 2 * (d + w), d + h
        if x + bw > SIZE:
            x, y, row = 0, y + row, 0
        if y + bh > SIZE:
            raise SystemExit('não coube na textura 128x128')
        out.append((p, b, x, y))
        x += bw
        row = max(row, bh)
    return out


def paint(packed, name, seed):
    im = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    rnd = random.Random(seed)
    for p, b, u, v in packed:
        w, h, d = int(round(b[3])), int(round(b[4])), int(round(b[5]))
        mat = MATS[b[6]]
        faces = {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
                 'west': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}
        for face, (fx, fy, fw, fh) in faces.items():
            for yy in range(fh):
                for xx in range(fw):
                    im.putpixel((fx + xx, fy + yy), mat(face, xx, yy, fw, fh, rnd))
        if b[7] in ('eyes_cyan', 'eyes_amber'):
            fx, fy, fw, fh = faces['north']
            glow, core = ('#3fd6e6', '#e0ffff') if b[7] == 'eyes_cyan' else ('#ffb020', '#fff0a0')
            ey = fy + max(2, fh * 2 // 5)
            for ex in (fx + fw // 2 - 3, fx + fw // 2 + 1):
                for dx in range(2):
                    im.putpixel((ex + dx, ey), hx(glow))
                im.putpixel((ex, ey), hx(core))
                im.putpixel((ex, ey - 1), hx('#202024'))
                im.putpixel((ex + 1, ey - 1), hx('#202024'))
            if b[7] == 'eyes_cyan':  # boca de fenda
                for dx in range(-2, 2):
                    im.putpixel((fx + fw // 2 + dx, ey + 3), hx('#1c1c20'))
    os.makedirs(TEX, exist_ok=True)
    im.save(os.path.join(TEX, name + '.png'))
    print('ok textura', name)


def jf(v):
    return ('%.1fF' % v).replace('.0F', '.0F')


def java_layer(method, packed, parts):
    lines = ['    public static LayerDefinition %s() {' % method,
             '        MeshDefinition mesh = new MeshDefinition();',
             '        PartDefinition root = mesh.getRoot();']
    for name, off, _ in parts:
        cubes = [(b, u, v) for p, b, u, v in packed if p[0] == name]
        chain = 'CubeListBuilder.create()' + ''.join(
            '\n                .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)' % (u, v, jf(b[0]), jf(b[1]), jf(b[2]),
                                                                              jf(int(round(b[3]))), jf(int(round(b[4]))), jf(int(round(b[5]))))
            for b, u, v in cubes)
        lines.append('        root.addOrReplaceChild("%s", %s,\n                PartPose.offset(%s, %s, %s));' % (name, chain, jf(off[0]), jf(off[1]), jf(off[2])))
    lines.append('        return LayerDefinition.create(mesh, %d, %d);' % (SIZE, SIZE))
    lines.append('    }')
    return '\n'.join(lines)


def shadow_wolf():
    jar = sorted(glob.glob(os.path.join(ROOT, 'cp', 'minecraft-patched-*-merged.jar')))
    if not jar:
        print('sem jar do Minecraft: textura do lobo não gerada')
        return
    with zipfile.ZipFile(jar[0]) as z:
        src = Image.open(io.BytesIO(z.read('assets/minecraft/textures/entity/wolf/wolf_tame.png'))).convert('RGBA')
    im = Image.new('RGBA', src.size, (0, 0, 0, 0))
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            r, g, b, a = src.getpixel((x, y))
            if a == 0:
                continue
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255.0
            # pelagem de sombra: preto-arroxeado, mais claro nas partes claras do lobo original
            im.putpixel((x, y), (int(18 + 60 * lum), int(10 + 30 * lum), int(34 + 90 * lum), a))
    # olhos violeta brilhantes (rosto do lobo: frente da cabeça em (4,4) 6x6)
    for ex in (4, 8):
        im.putpixel((ex, 6), hx('#e8b0ff'))
        im.putpixel((ex + 1, 6), hx('#b05cff'))
    im.save(os.path.join(TEX, 'shadow_wolf.png'))
    print('ok textura shadow_wolf')



def recolor(src_path, out_name, fn):
    jar = sorted(glob.glob(os.path.join(ROOT, 'cp', 'minecraft-patched-*-merged.jar')))
    if not jar:
        print('sem jar do Minecraft:', out_name, 'não gerada')
        return
    with zipfile.ZipFile(jar[0]) as z:
        src = Image.open(io.BytesIO(z.read(src_path))).convert('RGBA')
    im = Image.new('RGBA', src.size, (0, 0, 0, 0))
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            r, g, b, a = src.getpixel((x, y))
            if a:
                im.putpixel((x, y), fn((0.3 * r + 0.59 * g + 0.11 * b) / 255.0, a))
    im.save(os.path.join(TEX, out_name + '.png'))
    print('ok textura', out_name)


def soul(l, a):
    return (int(40 + 120 * l), int(150 + 100 * l), int(170 + 85 * l), min(a, 200))


def fire(l, a):
    if l < 0.35:
        return (int(120 + 200 * l), int(20 + 60 * l), 10, a)
    if l < 0.7:
        return (255, int(90 + 180 * (l - 0.35)), int(20 + 40 * l), a)
    return (255, int(200 + 70 * (l - 0.7)), int(80 + 200 * (l - 0.7)), a)


if __name__ == '__main__':
    recolor('assets/minecraft/textures/entity/illager/vex.png', 'spirit_guardian', soul)
    recolor('assets/minecraft/textures/entity/illager/vex_charging.png', 'spirit_guardian_charging', soul)
    recolor('assets/minecraft/textures/entity/parrot/parrot_red_blue.png', 'phoenix', fire)
    sg, wg = pack(STONE_GOLEM), pack(WOOD_GUARDIAN)
    paint(sg, 'stone_golem', 4101)
    paint(wg, 'wood_guardian', 4102)
    shadow_wolf()
    java = '''package com.mistech.mistechmod.spellcraft.client;

import com.mistech.mistechmod.Mistech;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * GERADO por tools/spellcraft/gen_summons.py — não editar à mão.
 * Geometria das invocações. As partes têm os mesmos nomes do Golem de Ferro para o IronGolemModel animar.
 */
public final class SummonModels {
    public static final ModelLayerLocation STONE_GOLEM = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "stone_golem"), "main");
    public static final ModelLayerLocation WOOD_GUARDIAN = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "wood_guardian"), "main");

    private SummonModels() {}

%s

%s
}
''' % (java_layer('stoneGolem', sg, STONE_GOLEM), java_layer('woodGuardian', wg, WOOD_GUARDIAN))
    open(JAVA, 'w', encoding='utf-8').write(java)
    print('ok java')
