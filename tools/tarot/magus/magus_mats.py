"""Materiais (pintura procedural por face) das formas do Mago."""
import math
import numpy as np
from magus_lib import *

def _base(ctx, c1, c2, cell=6.0, light=True, edge=0.16):
    W, H = ctx.W, ctx.H
    n = fbm(W, H, ctx.rng, cell * S / 2, 4)
    img = lerp(c1, c2, np.clip(n * 1.6 - 0.3, 0, 1))
    if light:
        if ctx.side:
            shade(img, grad_v(H, W, 1.06, 0.84))
        elif ctx.face == 'bottom':
            shade(img, np.full((H, W), 0.72, np.float32))
        else:
            shade(img, np.full((H, W), 1.08, np.float32))
    if edge and W > 4 and H > 4:
        shade(img, edge_dark(H, W, edge, 2))
    img[..., 3] = 255
    return img

# ---------------------------------------------------------------- TERRA (colosso de pedra e raiz)
STONE1, STONE2 = C(0x6B6862), C(0x8E8A82)
STONE_D = C(0x3C3A36)
EARTH1, EARTH2 = C(0x3E2A1A), C(0x6A4628)
STREAK = C(0x9B7A52)
MOSS1, MOSS2 = C(0x2E3F1E), C(0x56683A)
AMETHYST1, AMETHYST2, AMETHYST3 = C(0x5B2C8E), C(0x9A5FD6), C(0xE0C2FF)
RUNE_GOLD = C(0xF6D36B)
INF_WHITE = C(0xFFF6D8)

def stone(moss=0.0, crack=3, c1=STONE1, c2=STONE2, veins=0.0):
    def f(ctx):
        img = _base(ctx, c1, c2, 5.0)
        W, H = ctx.W, ctx.H
        if crack:
            m = dilate(cracks(H, W, ctx.rng, max(1, int(crack * (W * H) / 900)), wiggle=0.9), 0)
            img[m > 0, :3] = img[m > 0, :3] * 0.45
        if veins > 0 and ctx.side:
            m = cracks(H, W, ctx.rng, max(1, int(veins * (W * H) / 1400)), wiggle=0.5)
            g = ctx.g()
            vc = C(0xFFB347)
            img[m > 0, :3] = vc[:3]
            g[m > 0] = vc
        # pedrinhas claras
        spots = ctx.rng.random((H, W)) > 0.985
        img[spots, :3] = np.minimum(255, img[spots, :3] * 1.35)
        if moss > 0:
            mn = fbm(W, H, ctx.rng, 4.0, 3)
            if ctx.face == 'top':
                mask = mn > (1 - moss * 1.2)
            elif ctx.side:
                rows = np.linspace(1, 0, H)[:, None]
                mask = (mn + rows * 0.6) > (1.25 - moss)
            else:
                mask = np.zeros((H, W), bool)
            mc = lerp(MOSS1, MOSS2, mn)
            img[mask, :3] = mc[mask, :3]
        return img
    return f

def earth(ctx):
    """Terra/raiz escura com riscos verticais (como a referência)."""
    W, H = ctx.W, ctx.H
    img = _base(ctx, EARTH1, EARTH2, 7.0)
    rng = ctx.rng
    for _ in range(int(W * H / 40)):
        x = rng.integers(0, W); y = rng.integers(0, H); L = rng.integers(3, 9)
        c = STREAK if rng.random() < 0.5 else EARTH1 * 0.8
        dx = rng.choice([-1, 0, 0, 1]) * 0.25
        for k in range(L):
            yy = y + k; xx = int(x + dx * k)
            if 0 <= yy < H and 0 <= xx < W:
                img[yy, xx, :3] = img[yy, xx, :3] * 0.5 + c[:3] * 0.5
    return img

def amethyst(ctx):
    W, H = ctx.W, ctx.H
    t = np.repeat(np.linspace(0, 1, H)[:, None], W, 1).astype(np.float32)
    img = lerp(AMETHYST3, AMETHYST1, t * 0.9 + fbm(W, H, ctx.rng, 3, 2) * 0.2)
    for x in range(0, W, 3):
        img[:, x, :3] *= 0.85
    img[..., 3] = 255
    g = ctx.g()
    g[..., :] = img * 0.55
    g[..., 3] = 255
    return img

def earth_head(ctx):
    img = stone(moss=0.35, crack=2, c1=C(0x76726B), c2=C(0x9A958C))(ctx)
    if ctx.face == 'front':
        W, H = ctx.W, ctx.H
        g = ctx.g()
        # sobrancelha pesada (sombra)
        img[int(H * 0.30):int(H * 0.40), :, :3] *= 0.7
        # dois olhos fundos com brasas âmbar
        for ex in (W * 0.30, W * 0.70):
            eye(img, ex, H * 0.52, S * 1.4, C(0x141210), C(0xFFB347), g)
        # ∞ na testa
        infinity(img, W / 2, H * 0.18, W * 0.34, INF_WHITE, g, INF_WHITE, thick=S * 0.9)
        # rachadura vertical de cima
        line(img, W * 0.45, 0, W * 0.48, H * 0.12, C(0x2A2826))
    return img

def earth_jaw(ctx):
    img = stone(moss=0.0, crack=1, c1=C(0x5E5A54), c2=C(0x7A766E))(ctx)
    if ctx.face == 'front' or ctx.face == 'top':
        W, H = ctx.W, ctx.H
        # dentes de pedra
        n = max(3, W // (S * 2))
        for i in range(n):
            x = (i + 0.5) * W / n
            if ctx.face == 'front':
                for y in range(0, int(H * 0.45)):
                    if abs(x - (i + 0.5) * W / n) < S * 0.8 * (1 - y / (H * 0.45)) + 0.5:
                        pass
                disc(img, x, 0.8, S * 0.9, C(0xC9C2B0))
    return img

def chest_pentacle(ctx):
    img = stone(moss=0.2, crack=3, veins=1.0)(ctx)
    if ctx.face == 'front':
        W, H = ctx.W, ctx.H
        g = ctx.g()
        pentagram(img, W / 2, H * 0.48, min(W, H) * 0.26, RUNE_GOLD * 0.9 + 20, g, RUNE_GOLD)
    return img

def earth_mound(ctx):
    img = earth(ctx)
    if ctx.face == 'top':
        img = stone(moss=0.7, crack=1, c1=C(0x5A4A32), c2=C(0x7A6844))(ctx)
    elif ctx.side:
        # pedras incrustadas
        W, H = ctx.W, ctx.H
        for _ in range(int(W * H / 160)):
            x, y, r = ctx.rng.random() * W, ctx.rng.random() * H, 1 + ctx.rng.random() * S * 1.5
            disc(img, x, y, r, C(0x77736C) * (0.8 + ctx.rng.random() * 0.4), soft=False)
    return img

def rune_stone(ctx):
    img = stone(crack=1, c1=C(0x55524C), c2=C(0x7C786F))(ctx)
    if ctx.side:
        W, H = ctx.W, ctx.H
        g = ctx.g()
        pentagram(img, W / 2, H / 2, min(W, H) * 0.32, RUNE_GOLD, g, RUNE_GOLD, ring=False)
    return img

# ---------------------------------------------------------------- AR (harpia de lâminas)
PALE1, PALE2 = C(0x6E7C8C), C(0x9AA8B6)
PALE_D = C(0x7F8C99)
STEEL1, STEEL2 = C(0x8C99A8), C(0xE8F2FA)
CYAN = C(0x8FF0FF)

def pale_hide(ctx):
    img = _base(ctx, PALE1, PALE2, 4.0)
    W, H = ctx.W, ctx.H
    # estrias como fibras de vento
    for y in range(0, H, 3):
        off = int(ctx.rng.random() * 3)
        img[y, off::5, :3] *= 0.9
    return img

def air_torso(ctx):
    img = pale_hide(ctx)
    if ctx.face in ('front', 'back'):
        W, H = ctx.W, ctx.H
        # costelas aparentes
        for k in range(5):
            y = H * (0.18 + k * 0.13)
            for x in range(W):
                dx = abs(x + 0.5 - W / 2) / (W / 2)
                if dx > 0.12:
                    yy = int(y + dx * dx * S * 2)
                    if 0 <= yy < H:
                        img[yy, x, :3] *= 0.62
                        if yy + 1 < H:
                            img[yy + 1, x, :3] = np.minimum(255, img[yy + 1, x, :3] * 1.1)
        if ctx.face == 'front':
            g = ctx.g()
            # núcleo de vento no peito
            disc(img, W / 2, H * 0.32, S * 1.6, CYAN, g, CYAN)
            disc(img, W / 2, H * 0.32, S * 0.7, C(0xFFFFFF), g, C(0xFFFFFF))
    return img

def blade(ctx):
    W, H = ctx.W, ctx.H
    t = np.repeat(np.linspace(0, 1, W)[None, :], H, 0).astype(np.float32)
    img = lerp(STEEL2, STEEL1, np.abs(t - 0.35) * 1.4)
    img[..., 3] = 255
    if ctx.side and H > 8:
        # base escura (pena) que vira lâmina na ponta
        rows = np.linspace(0, 1, H)[:, None]
        dark = lerp(C(0x2A3340), C(0x46546A), np.repeat(rows, W, 1))
        sel = np.repeat(rows < 0.35, W, 1)
        img[sel] = dark[sel]
    if ctx.side and H > 4:
        # gume brilhante na borda
        img[:, 0, :3] = 250
        g = ctx.g()
        g[:, 0] = C(0xC8F8FF)
        g[..., 3] = np.where(g[..., 3] > 0, 255, 0)
    if ctx.face == 'bottom':
        img[..., :3] = 245
    return img

def air_skull(ctx):
    img = _base(ctx, C(0xD9DCD2), C(0xF2F1E8), 3.0)
    W, H = ctx.W, ctx.H
    if ctx.face == 'front':
        g = ctx.g()
        for ex in (W * 0.27, W * 0.73):
            eye(img, ex, H * 0.55, S * 1.2, C(0x1A2230), CYAN, g)
        infinity(img, W / 2, H * 0.2, W * 0.42, C(0xFFFFFF), g, CYAN, thick=S * 0.8)
    elif ctx.side:
        # sulcos laterais
        for y in range(2, H, 4):
            img[y, :, :3] *= 0.86
    return img

def beak(ctx):
    W, H = ctx.W, ctx.H
    img = _base(ctx, C(0x3A4250), C(0x6F7C8C), 3.0)
    if ctx.face in ('right', 'left'):
        img[int(H * 0.55), :, :3] = 20
    return img

def wind_tail(ctx):
    W, H = ctx.W, ctx.H
    img = _base(ctx, C(0xC9E6F2), C(0xF4FBFF), 4.0, edge=0.08)
    for y in range(H):
        for x in range(W):
            if (x + y * 0.7) % 7 < 1.2:
                img[y, x, :3] *= 0.88
    g = ctx.g()
    g[..., :3] = img[..., :3] * 0.35
    g[..., 3] = 255
    return img

def claw(ctx):
    W, H = ctx.W, ctx.H
    t = np.repeat(np.linspace(0, 1, H)[:, None], W, 1).astype(np.float32)
    img = lerp(C(0x4A5260), C(0x15181E), t)
    img[..., 3] = 255
    return img

# ---------------------------------------------------------------- ÁGUA (leviatã da taça)
SEA1, SEA2 = C(0x123B55), C(0x1F6C8C)
SEA_D = C(0x0A1E2E)
BELLY1, BELLY2 = C(0x8FB8B0), C(0xC9E2D4)
BIO = C(0x5FF2E0)
SHELL1, SHELL2 = C(0x2D4F5E), C(0x5A8C8F)

def sea_hide(ctx):
    img = _base(ctx, SEA1, SEA2, 5.0)
    W, H = ctx.W, ctx.H
    # escamas
    for y in range(0, H, 3):
        for x in range((y // 3) % 2 * 2, W, 4):
            img[y, x, :3] *= 0.72
            if y + 1 < H:
                img[y + 1, x, :3] *= 1.1
    if ctx.side or ctx.face == 'top':
        g = ctx.g()
        for _ in range(int(W * H / 140)):
            x, y = ctx.rng.random() * W, ctx.rng.random() * H
            disc(img, x, y, 0.9, BIO, g, BIO)
    return img

def shell(ctx):
    img = _base(ctx, SHELL1, SHELL2, 6.0)
    W, H = ctx.W, ctx.H
    # placas de casco
    for y in range(0, H, S * 4):
        img[y:y + 1, :, :3] *= 0.6
    for x in range(0, W, S * 5):
        img[:, x:x + 1, :3] *= 0.7
    # cracas
    for _ in range(int(W * H / 120)):
        x, y = ctx.rng.random() * W, ctx.rng.random() * H
        disc(img, x, y, 0.8 + ctx.rng.random(), C(0xBFC8B8))
    return img

def belly(ctx):
    img = _base(ctx, BELLY1, BELLY2, 4.0)
    W, H = ctx.W, ctx.H
    for y in range(0, H, S * 2):
        img[y, :, :3] *= 0.82
    return img

def chalice(ctx):
    """Taça de ouro velho incrustada nas costas; o topo é água brilhando."""
    W, H = ctx.W, ctx.H
    if ctx.face == 'top':
        n = fbm(W, H, ctx.rng, 3.0, 3)
        img = lerp(C(0x1E8FB8), C(0x8FF6FF), n)
        img[..., 3] = 255
        g = ctx.g()
        g[..., :3] = img[..., :3] * 0.8
        g[..., 3] = 255
        # borda de ouro
        img[:S, :, :3] = RUNE_GOLD[:3]; img[-S:, :, :3] = RUNE_GOLD[:3]
        img[:, :S, :3] = RUNE_GOLD[:3]; img[:, -S:, :3] = RUNE_GOLD[:3]
        g[:S, :, 3] = 0; g[-S:, :, 3] = 0; g[:, :S, 3] = 0; g[:, -S:, 3] = 0
        return img
    img = _base(ctx, C(0x8E6A22), C(0xD9AE4A), 3.0)
    if ctx.side:
        for x in range(0, W, S * 3):
            img[:, x, :3] *= 0.7
        img[int(H * 0.25), :, :3] = C(0xFFE08A)[:3]
    return img

def teeth(ctx):
    img = _base(ctx, C(0xE8E2CC), C(0xFFFBEA), 2.0, edge=0.05)
    return img

def jaw_sea(ctx):
    img = sea_hide(ctx)
    W, H = ctx.W, ctx.H
    if ctx.face == 'top':
        # boca por dentro + fileira de dentes na borda da frente
        img[...] = lerp(C(0x3A0E1A), C(0x6A1E2A), fbm(W, H, ctx.rng, 3, 2))
        img[..., 3] = 255
        n = max(4, W // (S * 2))
        for i in range(n):
            x = (i + 0.5) * W / n
            disc(img, x, H - 1.5, S * 0.9, C(0xF2ECD8))
            disc(img, x, 1.5, S * 0.7, C(0xF2ECD8))
    if ctx.face == 'front':
        n = max(4, W // (S * 2))
        for i in range(n):
            x0 = i * W / n
            for y in range(int(H * 0.55)):
                half = (W / n) * 0.45 * (1 - y / (H * 0.55))
                for xx in range(int(x0 + W / n / 2 - half), int(x0 + W / n / 2 + half) + 1):
                    if 0 <= xx < W:
                        img[y, xx, :3] = C(0xF2ECD8)[:3]
    return img

def angler_head(ctx):
    img = sea_hide(ctx)
    W, H = ctx.W, ctx.H
    if ctx.face == 'front':
        g = ctx.g()
        for ex in (W * 0.2, W * 0.8):
            eye(img, ex, H * 0.45, S * 1.3, C(0x061018), C(0xD8FF5A), g)
        infinity(img, W / 2, H * 0.22, W * 0.3, C(0xBFFFF8), g, BIO, thick=S * 0.8)
        # boca de cima com dentes caindo
        img[-S * 2:, :, :3] = C(0x3A0E1A)[:3]
        n = max(4, W // (S * 2))
        for i in range(n):
            x = (i + 0.5) * W / n
            for y in range(S * 3):
                half = S * 0.8 * (1 - y / (S * 3))
                for xx in range(int(x - half), int(x + half) + 1):
                    if 0 <= xx < W and H - 1 - (S * 3 - 1 - y) < H:
                        img[H - S * 3 + y, xx, :3] = C(0xF2ECD8)[:3]
    if ctx.face == 'bottom':
        img[..., :3] = C(0x3A0E1A)[:3]
    return img

def lure(ctx):
    W, H = ctx.W, ctx.H
    img = solid(H, W, C(0xE8FFF8))
    disc(img, W / 2, H / 2, min(W, H) * 0.35, C(0xFFFFFF))
    g = ctx.g()
    g[...] = img
    g[..., 3] = 255
    return img

def tentacle(ctx):
    img = sea_hide(ctx)
    W, H = ctx.W, ctx.H
    if ctx.face == 'front':
        # ventosas
        for y in range(S, H, S * 3):
            disc(img, W / 2, y, S * 0.8, C(0xC9A8B8))
            disc(img, W / 2, y, S * 0.35, C(0x6A3A4A))
    return img

def fin(ctx):
    W, H = ctx.W, ctx.H
    img = _base(ctx, C(0x2B8AA8), C(0x7FD6E8), 3.0, edge=0.1)
    for x in range(0, W, S * 2):
        img[:, x, :3] *= 0.7
    return img

# ---------------------------------------------------------------- FOGO (demônio do bastão)
CHAR1, CHAR2 = C(0x1A1412), C(0x3A2C26)
LAVA1, LAVA2, LAVA3 = C(0xB8300A), C(0xFF7A1A), C(0xFFE07A)
HORN1, HORN2 = C(0x2A2420), C(0xD8CCB0)

def basalt(glow_amt=1.0, ncr=4):
    def f(ctx):
        W, H = ctx.W, ctx.H
        img = _base(ctx, CHAR1, CHAR2, 4.0)
        # textura de basalto: estrias verticais
        for x in range(0, W, 3):
            img[:, x, :3] *= 0.85
        m = cracks(H, W, ctx.rng, max(1, int(ncr * W * H / 1100)), wiggle=0.7)
        g = ctx.g()
        if glow_amt > 0:
            lav = lerp(LAVA1, LAVA3, np.clip(m * 1.0, 0, 1))
            sel = m > 0
            img[sel, :3] = lav[sel, :3]
            g[sel, :3] = lav[sel, :3] * glow_amt
            g[sel, 3] = 255
        return img
    return f

def ribcage(ctx):
    W, H = ctx.W, ctx.H
    if ctx.face in ('front', 'right', 'left'):
        n = fbm(W, H, ctx.rng, 3, 3)
        img = lerp(LAVA2, LAVA3, n)
        img[..., 3] = 255
        g = ctx.g()
        g[...] = img
        g[..., 3] = 255
        # costelas de osso escuro por cima
        bone = C(0x2B211C)
        for k in range(5):
            y0 = int(H * (0.1 + k * 0.18))
            img[y0:y0 + S + 1, :, :3] = bone[:3]
            g[y0:y0 + S + 1, :, 3] = 0
        if ctx.face == 'front':
            img[:, int(W / 2 - S):int(W / 2 + S), :3] = bone[:3]
            g[:, int(W / 2 - S):int(W / 2 + S), 3] = 0
        return img
    return basalt(0.8, 2)(ctx)

def fire_skull(ctx):
    img = _base(ctx, C(0x2A221E), C(0x4A3A30), 3.0)
    W, H = ctx.W, ctx.H
    if ctx.face == 'front':
        g = ctx.g()
        for ex in (W * 0.28, W * 0.72):
            eye(img, ex, H * 0.5, S * 1.3, C(0x0A0605), LAVA3, g)
        infinity(img, W / 2, H * 0.2, W * 0.4, LAVA3, g, LAVA2, thick=S * 0.9)
        # fendas brilhantes nas maçãs
        line(img, W * 0.1, H * 0.75, W * 0.3, H * 0.65, LAVA2, g, LAVA2)
        line(img, W * 0.9, H * 0.75, W * 0.7, H * 0.65, LAVA2, g, LAVA2)
    return img

def horn(ctx):
    W, H = ctx.W, ctx.H
    t = np.repeat(np.linspace(0, 1, W)[None, :], H, 0).astype(np.float32)
    img = lerp(HORN1, HORN2, t)
    for x in range(0, W, 3):
        img[:, x, :3] *= 0.8
    img[..., 3] = 255
    return img

def flame(ctx):
    W, H = ctx.W, ctx.H
    t = np.repeat(np.linspace(0, 1, H)[:, None], W, 1).astype(np.float32)
    img = lerp(LAVA3, LAVA1, t * 0.8 + fbm(W, H, ctx.rng, 2, 2) * 0.3)
    img[..., 3] = 255
    g = ctx.g()
    g[...] = img
    g[..., 3] = 255
    return img

def staff_wood(ctx):
    W, H = ctx.W, ctx.H
    img = _base(ctx, C(0x2A1A10), C(0x4E3220), 3.0, edge=0.08)
    for y in range(0, H, S * 3):
        img[y, :, :3] = C(0xC8902A)[:3]
    m = cracks(H, W, ctx.rng, 1, wiggle=0.4)
    g = ctx.g()
    img[m > 0, :3] = LAVA2[:3]
    g[m > 0] = LAVA2
    return img

# ---------------------------------------------------------------- QUINTESSÊNCIA (o Mago)
RED1, RED2, RED_D = C(0x8E1C1C), C(0xC8352A), C(0x4E0C10)
WHITE1, WHITE2 = C(0xD8D4CA), C(0xF6F3EA)
GOLD1, GOLD2 = C(0xB8862A), C(0xF7D26A)
SKIN1, SKIN2 = C(0xD9A27E), C(0xF0C7A4)
HAIR1, HAIR2 = C(0xC9C6D6), C(0xF4F2FA)
NIGHT1, NIGHT2 = C(0x120A2E), C(0x2C1458)

def cloth(c1, c2, folds=True, trim=None, trim_rows=0):
    def f(ctx):
        W, H = ctx.W, ctx.H
        img = _base(ctx, c1, c2, 8.0, edge=0.1)
        if folds and ctx.side:
            ph = ctx.rng.random() * 6
            k = 0.88 + 0.12 * np.sin(np.arange(W) / (S * 1.6) + ph)
            img[..., :3] *= k[None, :, None]
        if trim is not None and ctx.side and trim_rows:
            img[H - trim_rows * S:, :, :3] = trim[:3]
            img[H - trim_rows * S, :, :3] = (trim * 1.15)[:3].clip(0, 255)
        return img
    return f

def robe_red(ctx):
    img = cloth(RED1, RED2, trim=GOLD2, trim_rows=1)(ctx)
    W, H = ctx.W, ctx.H
    if ctx.side and H > 20:
        # bordado de símbolos dourados na barra
        for i in range(int(W / (S * 5))):
            x = (i + 0.5) * S * 5
            disc(img, x, H - S * 2.5, S * 0.6, GOLD2)
    return img

def mantle_inner(ctx):
    """Forro do manto: céu estrelado (brilha)."""
    W, H = ctx.W, ctx.H
    img = lerp(NIGHT1, NIGHT2, fbm(W, H, ctx.rng, 6, 3))
    img[..., 3] = 255
    g = ctx.g()
    for _ in range(int(W * H / 50)):
        x, y = ctx.rng.random() * W, ctx.rng.random() * H
        c = C(0xFFFFFF) if ctx.rng.random() < 0.7 else C(0xFFE08A)
        put(img, x, y, c, g, c)
    return img

def cape(ctx):
    if ctx.face == 'back':
        return robe_red(ctx)
    if ctx.face == 'front':
        return mantle_inner(ctx)
    return robe_red(ctx)

def tunic(ctx):
    img = cloth(WHITE1, WHITE2)(ctx)
    W, H = ctx.W, ctx.H
    if ctx.face == 'front':
        # gola em V dourada
        for y in range(int(H * 0.35)):
            for d in (-1, 1):
                x = W / 2 + d * (W * 0.32) * (1 - y / (H * 0.35))
                put(img, x, y, GOLD2)
    return img

def belt(ctx):
    """Cinto ouroboros: cobra dourada mordendo a própria cauda."""
    W, H = ctx.W, ctx.H
    img = _base(ctx, GOLD1, GOLD2, 2.0, edge=0.12)
    for x in range(0, W, S * 2):
        img[:, x, :3] *= 0.8
    if ctx.face == 'front':
        g = ctx.g()
        disc(img, W / 2, H / 2, H * 0.45, C(0x2A7A3A))
        put(img, W / 2 - 1, H / 2 - 1, C(0xFF4040), g, C(0xFF4040))
    return img

def skin(ctx):
    return _base(ctx, SKIN1, SKIN2, 4.0, edge=0.06)

def magus_face(ctx):
    img = skin(ctx)
    W, H = ctx.W, ctx.H
    if ctx.face == 'front':
        g = ctx.g()
        # olhos dourados serenos, sobrancelhas, boca
        for ex in (W * 0.3, W * 0.7):
            img[int(H * 0.42), int(ex - S * 1.2):int(ex + S * 1.2), :3] = C(0x6A5A4A)[:3]
            disc(img, ex, H * 0.53, S * 0.9, C(0xFFFFFF))
            disc(img, ex, H * 0.53, S * 0.55, C(0xFFC83A), g, C(0xFFC83A))
        img[int(H * 0.78), int(W * 0.4):int(W * 0.6), :3] = C(0xA0605A)[:3]
        # tiara/faixa dourada com gema
        img[int(H * 0.2):int(H * 0.2) + S, :, :3] = GOLD2[:3]
        disc(img, W / 2, H * 0.2 + S * 0.5, S * 0.9, C(0xE04040), g, C(0xFF6060))
    return img

def hair(ctx):
    W, H = ctx.W, ctx.H
    img = _base(ctx, HAIR1, HAIR2, 3.0, edge=0.06)
    for x in range(W):
        if (x * 7) % 5 == 0:
            img[:, x, :3] *= 0.86
    if ctx.face == 'front':
        img[int(H * 0.18):, int(W * 0.2):int(W * 0.8), 3] = 0  # rosto aparece (cabelo é uma casca)
    if ctx.face == 'bottom':
        img[..., 3] = 0
    return img

def gold(ctx):
    img = _base(ctx, GOLD1, GOLD2, 2.0, edge=0.1)
    return img

def gold_glow(ctx):
    img = _base(ctx, C(0xF2C04A), C(0xFFF0B0), 2.0, edge=0.0)
    g = ctx.g()
    g[...] = img
    g[..., 3] = 255
    return img

def wand(ctx):
    W, H = ctx.W, ctx.H
    img = solid(H, W, C(0xF6F3EA))
    img[:S * 2, :, :3] = GOLD2[:3]
    img[-S * 2:, :, :3] = GOLD2[:3]
    g = ctx.g()
    g[:S, :] = C(0xFFFFFF); g[-S:, :] = C(0xFFFFFF)
    return img

def pentacle_coin(ctx):
    img = gold(ctx)
    if ctx.face in ('front', 'back'):
        W, H = ctx.W, ctx.H
        g = ctx.g()
        pentagram(img, W / 2, H / 2, min(W, H) * 0.36, C(0x7A4A10), g, C(0xFFE08A))
    return img

def sword_blade(ctx):
    W, H = ctx.W, ctx.H
    img = solid(H, W, C(0xDDE8F2))
    img[:, :max(1, W // 2), :3] = C(0xB8C8D8)[:3]
    g = ctx.g()
    g[..., :3] = img[..., :3] * 0.4
    g[..., 3] = 255
    return img

def cup_gold(ctx):
    img = gold(ctx)
    if ctx.face == 'top':
        W, H = ctx.W, ctx.H
        g = ctx.g()
        disc(img, W / 2, H / 2, min(W, H) * 0.4, C(0x3AB8F0), g, C(0x8FF6FF))
    return img

def runic_glow(color):
    def f(ctx):
        img = solid(ctx.H, ctx.W, C(color))
        g = ctx.g()
        g[...] = img
        return img
    return f

def collar(ctx):
    img = cloth(RED_D * 1.4, RED1, folds=False)(ctx)
    W, H = ctx.W, ctx.H
    if ctx.face in ('front', 'back'):
        img[:S, :, :3] = GOLD2[:3]
        g = ctx.g()
        for i in range(5):
            disc(img, (i + 0.5) * W / 5, H * 0.4, S * 0.7, GOLD2, g, C(0xFFE08A))
    return img

def stole(ctx):
    """Estola dourada com os quatro símbolos dos elementos."""
    W, H = ctx.W, ctx.H
    img = cloth(GOLD1, GOLD2, folds=False)(ctx)
    if ctx.face == 'front':
        g = ctx.g()
        cols = (C(0x7AB84A), C(0xBFEFFF), C(0x3AB8F0), C(0xFF7A1A))
        for i, c in enumerate(cols):
            disc(img, W / 2, H * (0.15 + i * 0.22), min(W, H) * 0.3, c, g, c)
    return img

def bubble_water(ctx):
    W, H = ctx.W, ctx.H
    n = fbm(W, H, ctx.rng, 4, 3)
    img = lerp(C(0x3AA8E0, 70), C(0xBFF4FF, 140), n)
    img[:S, :, 3] = 190; img[-S:, :, 3] = 190; img[:, :S, 3] = 190; img[:, -S:, 3] = 190
    for _ in range(int(W * H / 90)):
        x, y = ctx.rng.random() * W, ctx.rng.random() * H
        put(img, x, y, C(0xFFFFFF, 220))
    return img

def orb_white(ctx):
    W, H = ctx.W, ctx.H
    n = fbm(W, H, ctx.rng, 2, 2)
    img = lerp(C(0xC8C8C8, 150), C(0xFFFFFF, 210), n)
    return img

def orb_core(ctx):
    img = solid(ctx.H, ctx.W, C(0xFFFFFF))
    g = ctx.g(); g[...] = img
    return img
