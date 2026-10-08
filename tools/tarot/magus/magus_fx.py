"""Modelos pequenos do Mago: pedra rúnica, bolha d'água e feitiços (atlas 128x128)."""
from magus_lib import *
from magus_mats import *

def build():
    M = Model(128, 128, seed=5)
    M.part('runestone', None, (0, 0, 0)).box((-4, -4, -4), (8, 8, 8), rune_stone)
    b = M.part('bubble', None, (0, 0, 0))
    b.box((-8, -6, -6), (16, 12, 12), bubble_water).box((-6, -8, -6), (12, 16, 12), bubble_water).box((-6, -6, -8), (12, 12, 16), bubble_water)
    o = M.part('orb', None, (0, 0, 0))
    o.box((-3, -3, -3), (6, 6, 6), orb_white)
    o.child('orb_core', (0, 0, 0)).box((-1.5, -1.5, -1.5), (3, 3, 3), orb_core)
    M.part('boulder', None, (0, 0, 0)).box((-6, -6, -6), (12, 12, 12), stone(moss=0.3)).box((-4, -7, -5), (8, 2, 8), stone(moss=0.6))
    M.part('meteor', None, (0, 0, 0)).box((-5, -5, -5), (10, 10, 10), basalt(1.0, 3))
    M.part('blade', None, (0, 0, 0)).box((-8, -1, -1), (16, 2, 2), orb_white).box((-5, -0.5, -2.5), (10, 1, 5), orb_white)
    return M
