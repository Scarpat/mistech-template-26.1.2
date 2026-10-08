"""As cinco formas do Mago num atlas só (textura lógica 512x512, pintada em 1024x1024).
Coordenadas locais: chão em y = 0 (raiz de cada forma em (0, 24, 0)), frente = -Z."""
import math
from magus_lib import *
from magus_mats import *

def build():
    M = Model(512, 256, seed=11)
    m_earth(M); m_air(M); m_water(M); m_fire(M); m_quint(M)
    return M

# ---------------------------------------------------------------- TERRA
def m_earth(M):
    r = M.part('earth', None, (0, 24, 0))
    base = r.child('earth_base', (0, 0, 0))
    base.box((-9, -10, -9), (18, 10, 18), earth_mound)
    base.box((-12, -6, -6), (4, 6, 11), stone(moss=0.5))
    base.box((8, -7, -5), (4, 7, 10), stone(moss=0.5))
    base.box((-6, -5, -14), (12, 5, 4), stone(moss=0.6))
    base.box((-5, -4, 10), (9, 4, 4), stone(moss=0.4))
    waist = base.child('earth_waist', (0, -10, 0), (0.12, 0, 0))
    waist.box((-8, -12, -7), (16, 12, 14), earth)
    torso = waist.child('earth_torso', (0, -11, 1), (0.18, 0, 0))
    torso.box((-12, -20, -8), (24, 20, 16), chest_pentacle)
    torso.box((-10, -4, -9), (20, 4, 2), stone(moss=0.1, crack=1))
    for i, (px, py, pz, rx, rz, w, h) in enumerate(((-6, -19, 6, -0.55, -0.35, 3, 11), (5, -19, 5, -0.6, 0.35, 4, 14),
                                                    (0, -15, 8, -0.95, 0.0, 3, 9), (-10, -13, 5, -0.35, -0.85, 2, 7),
                                                    (10, -12, 5, -0.4, 0.9, 3, 8))):
        torso.child(f'earth_crystal_{i}', (px, py, pz), (rx, 0, rz)).box((-w / 2, -h, -w / 2), (w, h, w), amethyst)
    sr = torso.child('earth_shoulder_r', (-12, -18, 0), (0, 0, 0.12))
    sr.box((-7, -5, -8), (10, 10, 16), stone(moss=0.65))
    sl = torso.child('earth_shoulder_l', (12, -18, 0), (0, 0, -0.12))
    sl.box((-3, -5, -8), (10, 10, 16), stone(moss=0.65))
    head = torso.child('earth_head', (0, -15, -7), (-0.18, 0, 0))
    head.box((-9, -15, -12), (18, 15, 15), earth_head)
    head.box((-8, -18, -7), (5, 3, 5), stone(moss=0.8))
    head.box((3, -17, -9), (4, 2, 4), stone(moss=0.8))
    head.box((-2, -17, -2), (6, 2, 6), stone(moss=0.8))
    jaw = head.child('earth_jaw', (0, 0, -4), (0.12, 0, 0))
    jaw.box((-8, -1, -9), (16, 5, 12), earth_jaw)
    jaw.box((-7, -5, -9.5), (2, 5, 2), teeth).box((5, -5, -9.5), (2, 5, 2), teeth)
    for side, sx in (('r', -1), ('l', 1)):
        arm = torso.child(f'earth_arm_{side}', (sx * 18, -15, 0), (-0.2, 0, sx * -0.12))
        arm.box((-4.5, -3, -5), (9, 16, 10), stone(moss=0.3, veins=0.8))
        fore = arm.child(f'earth_fore_{side}', (0, 12, 0), (-0.25, 0, 0))
        fore.box((-5.5, 0, -6), (11, 15, 12), earth)
        fore.box((-6, 1, -6.5), (12, 3, 13), stone(moss=0.4, crack=1))
        fist = fore.child(f'earth_fist_{side}', (0, 15, 0), (0.35, 0, 0))
        fist.box((-6.5, 0, -7), (13, 9, 13), stone(moss=0.4))
        fist.box((-5.5, 9, -6), (11, 2, 11), stone(crack=1))

# ---------------------------------------------------------------- AR
def m_air(M):
    r = M.part('air', None, (0, 24, 0))
    body = r.child('air_body', (0, -32, 0))
    body.box((-5, -14, -3), (10, 14, 6), air_torso)
    body.box((-3.5, 0, -2.5), (7, 3, 5), pale_hide)
    body.box((-6, -15, -3.5), (12, 3, 7), pale_hide)
    neck = body.child('air_neck', (0, -14, -1), (0.65, 0, 0))
    neck.box((-2, -7, -2), (4, 7, 4), pale_hide)
    head = neck.child('air_head', (0, -6, 0), (-0.75, 0, 0))
    head.box((-4.5, -8, -5.5), (9, 8, 9), air_skull)
    head.child('air_beak', (0, -2.5, -5.5), (0.12, 0, 0)).box((-1.5, -2, -12), (3, 4, 12), beak)
    head.child('air_beak_low', (0, -0.5, -5.5), (0.35, 0, 0)).box((-1, 0, -8), (2, 1.5, 8), beak)
    for i, (px, rz) in enumerate(((-2.5, -0.25), (0, 0), (2.5, 0.25))):
        head.child(f'air_crest_{i}', (px, -6, 2), (-1.0 - abs(rz), 0, rz)).box((-0.5, -10 + abs(px), -1), (1, 10 - abs(px), 2), blade)
    for side, sx in (('r', -1), ('l', 1)):
        mant = body.child(f'air_mantle_{side}', (sx * 5, -14, 0), (0.25, 0, sx * -0.5))
        for k in range(3):
            mant.box((-1 + sx * k * 1.5 - (2 if sx < 0 else 0), -1, -3 + k * 2), (3, 9 - k, 1.5), blade)
        wing = body.child(f'air_wing_{side}', (sx * 5, -12, 1), (0, sx * -0.35, sx * -0.55))
        wing.box((-16 if sx < 0 else 0, -1.5, -1.5), (16, 3, 3), pale_hide)
        tip = wing.child(f'air_wingtip_{side}', (sx * 16, 0, 0), (0, sx * 0.2, sx * 0.9))
        tip.box((-13 if sx < 0 else 0, -1, -1), (13, 2, 2), pale_hide)
        for i, x in enumerate((3, 7, 11, 15)):
            wing.child(f'air_feather_{side}{i}', (sx * x, 1, 0), (0.35, 0, sx * (0.5 - i * 0.12))).box((-1.25, 0, -0.5), (2.5, 15 + i * 2, 1), blade)
        for i, x in enumerate((3, 7, 11)):
            tip.child(f'air_feather_{side}{4 + i}', (sx * x, 0.5, 0), (0.35, 0, sx * -(0.2 + i * 0.3))).box((-1.25, 0, -0.5), (2.5, 22 - i * 2, 1), blade)
        arm = body.child(f'air_arm_{side}', (sx * 5, -10, -1), (-0.35, 0, sx * -0.25))
        arm.box((-1.5, 0, -1.5), (3, 10, 3), pale_hide)
        fore = arm.child(f'air_fore_{side}', (0, 10, 0), (-0.7, 0, 0))
        fore.box((-1, 0, -1), (2, 11, 2), pale_hide)
        cl = fore.child(f'air_claws_{side}', (0, 11, 0), (0.3, 0, 0))
        cl.box((-1.5, 0, -1.2), (1, 6, 1), claw).box((0.5, 0, -1.2), (1, 6, 1), claw).box((-0.5, 0, 0.5), (1, 5, 1), claw)
    t1 = body.child('air_tail1', (0, 3, 0), (0.2, 0, 0))
    t1.box((-3, 0, -2.5), (6, 7, 5), wind_tail)
    t2 = t1.child('air_tail2', (0, 7, 0), (0.25, 0, 0))
    t2.box((-2, 0, -2), (4, 7, 4), wind_tail)
    t3 = t2.child('air_tail3', (0, 7, 0), (0.3, 0, 0))
    t3.box((-1.5, 0, -1.5), (3, 7, 3), wind_tail)
    t4 = t3.child('air_tail4', (0, 7, 0), (0.3, 0, 0))
    t4.box((-0.5, 0, -3), (1, 9, 6), blade)

# ---------------------------------------------------------------- ÁGUA
def m_water(M):
    r = M.part('water', None, (0, 24, 0))
    body = r.child('water_body', (0, -17, 0))
    body.box((-12, -12, -10), (24, 13, 22), shell)
    body.box((-10.5, 1, -9), (21, 4, 18), belly)
    body.box((-11, -10, -11), (22, 10, 2), sea_hide)
    for i, (x, z, h, rx) in enumerate(((-7, 8, 6, 0.5), (7, 8, 6, 0.5), (0, 10, 5, 0.8), (-9, -2, 5, 0.1), (9, -2, 5, 0.1))):
        body.child(f'water_spike_{i}', (x, -11, z), (rx, 0, 0.35 if x < 0 else -0.35 if x > 0 else 0)).box((-1.5, -h, -1.5), (3, h, 3), shell)
    cup = body.child('water_cup', (0, -12, 3))
    cup.box((-3, -3, -3), (6, 3, 6), chalice)
    bowl = cup.child('water_bowl', (0, -3, 0))
    bowl.box((-8, -6, -7), (16, 6, 14), chalice)
    bowl.box((-9, -7, -8), (18, 1, 1), chalice).box((-9, -7, 7), (18, 1, 1), chalice)
    bowl.box((-9, -7, -7), (1, 1, 14), chalice).box((8, -7, -7), (1, 1, 14), chalice)
    head = body.child('water_head', (0, -5, -10))
    head.box((-8, -6, -9), (16, 8, 10), angler_head)
    jaw = head.child('water_jaw', (0, 2, -1), (0.35, 0, 0))
    jaw.box((-8.5, 0, -9.5), (17, 5, 10), jaw_sea)
    l1 = head.child('water_lure', (0, -6, -4), (-0.25, 0, 0))
    l1.box((-0.5, -9, -0.5), (1, 9, 1), fin)
    l2 = l1.child('water_lure2', (0, -9, 0), (-1.3, 0, 0))
    l2.box((-0.5, -7, -0.5), (1, 7, 1), fin)
    l2.child('water_lure_tip', (0, -7, 0)).box((-1.5, -3, -1.5), (3, 3, 3), lure)
    for side, sx in (('r', -1), ('l', 1)):
        arm = body.child(f'water_arm_{side}', (sx * 12, -6, -4), (-0.2, 0, sx * -0.3))
        arm.box((-2, -1, -2.5), (4, 11, 5), sea_hide)
        fore = arm.child(f'water_fore_{side}', (0, 10, 0), (-0.7, 0, sx * 0.2))
        fore.box((-1.75, 0, -2), (3.5, 10, 4), sea_hide)
        hand = fore.child(f'water_hand_{side}', (0, 10, 0), (0.2, 0, 0))
        hand.box((-3, 0, -0.5), (6, 7, 1), fin)
        hand.box((-3, 6, -1), (1, 3, 1), teeth).box((-0.5, 6.5, -1), (1, 3, 1), teeth).box((2, 6, -1), (1, 3, 1), teeth)
    for i in range(6):
        a = math.radians(30 + i * 60)
        x, z = math.cos(a) * 7, math.sin(a) * 6.5
        # gira para fora: em volta do eixo perpendicular à direção radial
        t1 = body.child(f'water_tent{i}_1', (x, 4, z), (math.sin(a) * 0.55, 0, -math.cos(a) * 0.55))
        t1.box((-2, 0, -2), (4, 7, 4), tentacle)
        t2 = t1.child(f'water_tent{i}_2', (0, 7, 0), (-math.sin(a) * 0.35, 0, math.cos(a) * 0.35))
        t2.box((-1.5, 0, -1.5), (3, 6, 3), tentacle)
        t3 = t2.child(f'water_tent{i}_3', (0, 6, 0), (-math.sin(a) * 0.45, 0, math.cos(a) * 0.45))
        t3.box((-1, 0, -1), (2, 6, 2), tentacle)

# ---------------------------------------------------------------- FOGO
def m_fire(M):
    r = M.part('fire', None, (0, 24, 0))
    hips = r.child('fire_hips', (0, -23, 0))
    hips.box((-6, -3, -4), (12, 5, 8), basalt(0.9, 3))
    for side, sx in (('r', -1), ('l', 1)):
        th = hips.child(f'fire_thigh_{side}', (sx * 4.5, 1, 0), (-0.6, 0, sx * -0.08))
        th.box((-2.5, 0, -3), (5, 11, 6), basalt(1.0, 3))
        sh = th.child(f'fire_shin_{side}', (0, 10, -1), (1.25, 0, 0))
        sh.box((-2, 0, -2), (4, 11, 4), basalt(1.0, 2))
        ft = sh.child(f'fire_foot_{side}', (0, 11, 0), (-0.62, 0, 0))
        ft.box((-2.5, 0, -6), (5, 3, 8), basalt(0.6, 1))
        ft.box((-2.5, 1, -8), (1, 2, 2), horn).box((-0.5, 1, -8.5), (1, 2, 2.5), horn).box((1.5, 1, -8), (1, 2, 2), horn)
    torso = hips.child('fire_torso', (0, -3, 0), (0.28, 0, 0))
    torso.box((-3, -7, -2), (6, 7, 4), basalt(1.0, 2))
    ribs = torso.child('fire_ribs', (0, -7, 0))
    ribs.box((-7, -12, -5), (14, 12, 9), ribcage)
    ribs.box((-9.5, -14, -4.5), (19, 4, 9), basalt(0.8, 3))
    for i, (z, h, rx) in enumerate(((2, 5, -0.6), (3, 6, -0.7), (3.5, 5, -0.9))):
        ribs.child(f'fire_spike_{i}', (0, -12 + i * 4, z), (rx, 0, 0)).box((-1, -h, -1), (2, h, 2), horn)
    head = ribs.child('fire_head', (0, -13, -2), (-0.25, 0, 0))
    head.box((-5.5, -10, -6.5), (11, 10, 10), fire_skull)
    head.child('fire_jaw', (0, -1, -1), (0.2, 0, 0)).box((-4, 0, -5), (8, 3, 6), basalt(1.0, 2))
    for side, sx in (('r', -1), ('l', 1)):
        h1 = head.child(f'fire_horn_{side}1', (sx * 5, -8, 0), (0.2, 0, sx * 0.25))
        h1.box((-6 if sx < 0 else 0, -2, -2), (6, 4, 4), horn)
        h2 = h1.child(f'fire_horn_{side}2', (sx * 5.5, 0, 0), (0.25, 0, sx * 0.85))
        h2.box((-6 if sx < 0 else 0, -1.5, -1.5), (6, 3, 3), horn)
        h3 = h2.child(f'fire_horn_{side}3', (sx * 5.5, 0, 0), (0.3, 0, sx * 0.7))
        h3.box((-5 if sx < 0 else 0, -1, -1), (5, 2, 2), horn)
        h4 = h3.child(f'fire_horn_{side}4', (sx * 4.5, 0, 0), (0.2, 0, sx * 0.5))
        h4.box((-3 if sx < 0 else 0, -0.5, -0.5), (3, 1, 1), horn)
    crown = head.child('fire_crown', (0, -10, -1))
    crown.box((-4, -4, -1), (2, 4, 2), flame).box((-1, -7, -1), (2, 7, 2), flame).box((2, -4, -1), (2, 4, 2), flame)
    # braço direito: o Bastão fundido ao antebraço
    ar = ribs.child('fire_arm_r', (-9, -11, 0), (-0.3, 0, 0.25))
    ar.box((-3, -1, -2), (4, 11, 4), basalt(1.0, 2))
    fr = ar.child('fire_fore_r', (-1, 10, 0), (-0.8, 0, 0))
    fr.box((-1.5, 0, -1.5), (3, 18, 3), staff_wood)
    fr.box((-2, 2, -2), (4, 2, 4), gold)
    tip = fr.child('fire_staff_tip', (0, 18, 0))
    tip.box((-2.5, 0, -2.5), (5, 5, 5), flame)
    al = ribs.child('fire_arm_l', (9, -11, 0), (-0.3, 0, -0.25))
    al.box((-1, -1, -2), (4, 11, 4), basalt(1.0, 2))
    fl = al.child('fire_fore_l', (1, 10, 0), (-0.6, 0, 0))
    fl.box((-1.5, 0, -1.5), (3, 10, 3), basalt(1.0, 2))
    cl = fl.child('fire_claw_l', (0, 10, 0), (0.3, 0, 0))
    cl.box((-1.5, 0, -1.5), (1, 6, 1), horn).box((0.5, 0, -1.5), (1, 6, 1), horn).box((-0.5, 0, 0.5), (1, 5, 1), horn)
    t1 = hips.child('fire_tail1', (0, 0, 3), (0.9, 0, 0))
    t1.box((-1.5, 0, -1.5), (3, 9, 3), basalt(1.0, 2))
    t2 = t1.child('fire_tail2', (0, 9, 0), (0.45, 0, 0))
    t2.box((-1, 0, -1), (2, 9, 2), basalt(1.0, 1))
    t2.child('fire_tail3', (0, 9, 0), (0.3, 0, 0)).box((-1.5, 0, -1.5), (3, 4, 3), flame)

# ---------------------------------------------------------------- QUINTESSÊNCIA
def m_quint(M):
    r = M.part('quint', None, (0, 24, 0))
    sk = r.child('quint_skirt', (0, -34, 0))
    sk.box((-9.5, 0, -6.5), (19, 12, 13), robe_red)
    sk.box((-4, 0, -7), (8, 12, 1), tunic)
    sk2 = sk.child('quint_skirt2', (0, 12, 0))
    sk2.box((-11.5, 0, -8), (23, 18, 16), robe_red)
    sk2.box((-5, 0, -8.6), (10, 18, 1), tunic)
    body = r.child('quint_body', (0, -34, 0))
    body.box((-9, -24, -5), (18, 24, 10), tunic)
    body.box((-9.5, -3, -5.5), (19, 4, 11), belt)
    mantle = body.child('quint_mantle', (0, -24, 0))
    mantle.box((-11, -1, -6.5), (22, 7, 13), robe_red)
    mantle.box((-9, -9, 4), (18, 9, 2), collar)
    mantle.box((-14, -2, -6), (6, 5, 12), gold).box((8, -2, -6), (6, 5, 12), gold)
    body.box((-3, -24, -5.6), (6, 21, 1), stole)
    body.child('quint_cape', (0, -23, 6), (0.1, 0, 0)).box((-11, 0, 0), (22, 52, 1), cape)
    head = body.child('quint_head', (0, -25, 0))
    head.box((-7, -14, -7), (14, 14, 14), magus_face)
    head.box((-7.5, -14.5, -7.5), (15, 15, 15), hair)
    head.box((-7, -2, 5), (14, 18, 3), hair)
    halo = head.child('quint_halo', (0, -21, 0))
    for i in range(18):
        t = i / 18 * math.tau
        den = 1 + math.sin(t) ** 2
        x = 9 * math.cos(t) / den
        y = 9 * math.sin(t) * math.cos(t) / den * 1.2
        halo.box((x - 0.9, y - 0.9, -0.9), (1.8, 1.8, 1.8), gold_glow)
    for side, sx in (('r', -1), ('l', 1)):
        arm = body.child(f'quint_arm_{side}', (sx * 11, -21, 0), (0, 0, sx * -0.12))
        arm.box((-3, -2, -3.5), (6, 14, 7), robe_red)
        fore = arm.child(f'quint_fore_{side}', (0, 12, 0))
        fore.box((-3.5, 0, -4), (7, 12, 8), robe_red)
        hand = fore.child(f'quint_hand_{side}', (0, 12, 0))
        hand.box((-2, 0, -2), (4, 5, 4), skin)
        if side == 'r':
            hand.child('quint_wand', (0, 3, 0)).box((-1, -15, -1), (2, 30, 2), wand)
        else:
            hand.box((-0.5, 5, -1.5), (1, 4, 1), skin)
    orbit = r.child('quint_orbit', (0, -42, 0))
    t = orbit.child('quint_tool_pentacle', (26, 0, 0))
    t.box((-4.5, -4.5, -0.75), (9, 9, 1.5), pentacle_coin)
    t = orbit.child('quint_tool_sword', (-26, 0, 0))
    t.box((-1, -16, -0.5), (2, 16, 1), sword_blade).box((-4, 0, -1), (8, 1.5, 2), gold)
    t.box((-0.75, 1.5, -0.75), (1.5, 4.5, 1.5), staff_wood).box((-1.25, 6, -1.25), (2.5, 2.5, 2.5), gold)
    t = orbit.child('quint_tool_cup', (0, 0, 26))
    t.box((-4, -6, -4), (8, 5, 8), cup_gold).box((-1, -1, -1), (2, 4, 2), gold).box((-3, 3, -3), (6, 1.5, 6), gold)
    t = orbit.child('quint_tool_wand', (0, 0, -26))
    t.box((-0.75, -9, -0.75), (1.5, 18, 1.5), staff_wood).box((-1.75, -13, -1.75), (3.5, 4, 3.5), flame)
