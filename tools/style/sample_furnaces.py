"""Amostra do guia de estilo: a linha das fornalhas nos 3 tiers.
Fornalha Rúnica (I, Oficina) -> Fornalha Arcana (II, Arcano) -> Forja Estelar (III, Astral).
Tudo desenhado à mão em grades de 16x16; cores só da palette.py.
Uso: python sample_furnaces.py <pasta_saida>"""
import os, sys
from palette import *

def over(base, patch, x0, y0):
    """Sobrepõe 'patch' (lista de strings, '.' = mantém) na grade 'base' a partir de (x0,y0)."""
    g = [list(r) for r in base]
    for j, row in enumerate(patch):
        for i, ch in enumerate(row):
            if ch != '.':
                g[y0 + j][x0 + i] = ch
    return [''.join(r) for r in g]

BAND_TOP = ["3444444444444443",
            "2r333333333333r2",
            "1111111111111111"]
BAND_BOT = ["3444444444444443",
            "2r333333333333r2",
            "1111111111111111"]

# ---------------- Tier I: Fornalha Rúnica (pedra + cobre + latão, runa de ametista) ----------------
STONE_BODY = [  # linhas 3..12, colunas 0..15 (0 e 15 = cantoneiras de cobre)
    "2ddddddcadddddc1",
    "2dcccccbadccccb1",
    "2dcccccbadcbccb1",
    "2aaaaaaaaaaaaaa1",
    "2ddcaddddddcadd1",
    "2ccbadcccccbadc1",
    "2cbbadccbccbadc1",
    "2aaaaaaaaaaaaaa1",
    "2ddddddcadddddc1",
    "2cccbccbadccccb1",
]
t1_side = BAND_TOP + over(STONE_BODY, ["ddadddc", "dcaaacb", "dccacbb"], 5, 4) + BAND_BOT

t1_front = BAND_TOP + [
    "2dddc433332dddc1",
    "2cccb3wvvw1cccb1",
    "2cbbb2wvvw1cbbb1",
    "2aaaa111111aaaa1",
    "2dc3444444442cb1",
    "2dc3KKKKKKKK1cb1",
    "2cb3KKKKKKKK1bb1",
    "2cb3KKKKKKKK1bb1",
    "2aa2111111111aa1",
    "2dccccccccccccb1",
] + BAND_BOT
t1_front_on = over(t1_front, ["KxKKKKxK", "xyxKKxyx", "yuyxxyty"], 4, 8)

t1_top = [
    "3444444444444442",
    "4rddddddddddddr1",
    "4dccccccccccccb1",
    "4dcaaaaaaaaaacb1",
    "4dca34444442acb1",
    "4dca3K2KK2K1acb1",
    "4dca3K2KK2K1acb1",
    "4dca32222221acb1",
    "4dca3K2KK2K1acb1",
    "4dca3K2KK2K1acb1",
    "4dca21111111acb1",
    "4dcaaaaaaaaaacb1",
    "4dccccccccccccb1",
    "4cbbbbbbbbbbbbb1",
    "4rbbbbbbbbbbbbr1",
    "2111111111111111",
]
L1 = legend_for(PEDRA, COBRE, LATAO, {'v': AMETISTA[1], 'w': COBRE[2],
                                       'x': FOGO[1], 'y': FOGO[2], 'u': FOGO[3], 't': FOGO[4]})
L1_on = dict(L1, v=AMETISTA[4])

# ---------------- Tier II: Fornalha Arcana (ardósia + latão + mitril, brilho arcano) ----------------
TILES = [
    "2ddczddczddczdd1",
    "2dcbzdcbzdcbzdc1",
    "2cbbzcbbzcbbzcb1",
    "2zzzzzzzzzzzzzz1",
    "2czddczddczddcz1",
    "2bzdcbzdcbzdcbz1",
    "2bzcbbzcbbzcbbz1",
    "2zzzzzzzzzzzzzz1",
    "2ddczddczddczdd1",
    "2dcbzdcbzdcbzdc1",
]
GAUGE = [".3333.",
         "3mmmn2",
         "3mmnm1",
         "3mzmm1",
         ".2111."]
t2_side = BAND_TOP + over(TILES, GAUGE, 5, 3) + BAND_BOT
# frente do tier II desenhada inteira:
t2_front = BAND_TOP + [
    "2dddz433332zddc1",
    "2dcbz3wvvw1zdcb1",
    "2cbbz2wvvw1zcbb1",
    "2zzzzz1111zzzzz1",
    "2dc3444444442dc1",
    "2cb3KKKKKKKK1cb1",
    "2cb3KKKKKKKK1cb1",
    "2bz3KKKKKKKK1bz1",
    "2zz2111111111zz1",
    "2ddczddczddczdd1",
] + BAND_BOT
t2_front_on = over(t2_front, ["KxKKKKxK", "xyxKKxyx", "yuyxxyty"], 4, 8)
t2_top = [
    "3444444444444442",
    "4rddczddczddczr1",
    "4dcbzdcbzdcbzdc1",
    "4cbzzzzzzzzzzcb1",
    "4zzz34444442zzz1",
    "4ddz3mmmmmm1zdd1",
    "4dcz3mxyyxm1zdc1",
    "4cbz3myuuym1zcb1",
    "4ddz3myuuym1zdd1",
    "4dcz3mxyyxm1zdc1",
    "4cbz3mmmmmm1zcb1",
    "4zzz21111111zzz1",
    "4ddczddczddczdd1",
    "4dcbzdcbzdcbzdc1",
    "4rbbzcbbzcbbzbr1",
    "2111111111111111",
]
L2 = legend_for(ARDOSIA, LATAO, MITRIL, {'v': ARCANO[1], 'w': MITRIL[2], 'm': MITRIL[2], 'n': ADAMANTINA[3],
                                         'x': ARCANO[1], 'y': ARCANO[2], 'u': ARCANO[3], 't': ARCANO[4]})
L2_top = dict(L2, m=MITRIL[1], x=MITRIL[2], y=MITRIL[3], u=MITRIL[4])
L2_on = dict(L2, v=ARCANO[4])

# ---------------- Tier III: Forja Estelar (lápis-noite + ouro + celeste) ----------------
t3_side = [
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "zzzzzzzzzzzzzzzz",
    "zz3bbbbbebbb1zzz",
    "zz3bebbbbbbb1zzz",
    "zz3bbbbbbbeb1zzz",
    "zz3bbbebbbbb1zzz",
    "zz3bbbbbbbbb1zzz",
    "zz2111111111zzzz",
    "3444444444444443",
    "2ccrccccccccrcc1",
    "2bbbbbbbbbbbbbb1",
    "1000000000000001",
]
t3_front = [
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "zzzzzzzzzzzzzzzz",
    "zz3b34444442b1zz",
    "zz3b3KKKKKK1b1zz",
    "zz3b3KKKKKK1b1zz",
    "zz3b3KKKKKK1b1zz",
    "zz3b21111111b1zz",
    "zz2111111111zzzz",
    "3444444444444443",
    "2ccrccccccccrcc1",
    "2bbbbbbbbbbbbbb1",
    "1000000000000001",
]
t3_front_on = over(t3_front, ["xKKKKx", "yxKKxy", "uyxxyu"], 5, 7)
t3_top = [
    "3444444444444443",
    "4aaaaaaaaaaaaaa1",
    "4abbebbbbbbbbba1",
    "4abbbb3333bbbba1",
    "4abbb3bbbb3beba1",
    "4abb3bbbbbb3bba1",
    "4ab3bbbbbbbb3ba1",
    "4ab3bbbbbbbb3ba1",
    "4ab3bbbbbbbb3ba1",
    "4ab3bbbbbbbb3ba1",
    "4abb3bbbbbb3bba1",
    "4abeb3bbbb3bbba1",
    "4abbbb3333bbbba1",
    "4abbbbbbbbbbeba1",
    "4aaaaaaaaaaaaaa1",
    "2111111111111111",
]
t3_trim = [
    "3333333333333333",
    "2222222222222222",
    "3333333333333333",
    "3r333333333333r3",
    "1111111111111111",
    "3333333333333333",
    "333333r33r333333",
    "3333332ee2333333",
    "3333332ee2333333",
    "333333r11r333333",
    "3333333333333333",
    "3333333333333333",
    "3333333333333333",
    "3333333333333333",
    "2222222222222222",
    "1111111111111111",
]
L3 = legend_for(LAPIS_NOITE, OURO, CELESTE, {'e': CELESTE[4], 'x': CELESTE[2], 'y': CELESTE[3], 'u': CELESTE[5]})

OUT = {
    'runic_furnace_side': (t1_side, L1), 'runic_furnace_front': (t1_front, L1),
    'runic_furnace_front_on': (t1_front_on, L1_on), 'runic_furnace_top': (t1_top, L1),
    'arcane_furnace_side': (t2_side, L2), 'arcane_furnace_front': (t2_front, L2),
    'arcane_furnace_front_on': (t2_front_on, L2_on), 'arcane_furnace_top': (t2_top, L2_top),
    'star_forge_side': (t3_side, L3), 'star_forge_front': (t3_front, L3),
    'star_forge_front_on': (t3_front_on, L3), 'star_forge_top': (t3_top, L3), 'star_forge_trim': (t3_trim, L3),
}
if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    for name, (g, L) in OUT.items():
        paint(g, L).save(os.path.join(out, name + '.png'))
    print('ok', len(OUT))
