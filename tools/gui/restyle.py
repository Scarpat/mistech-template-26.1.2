"""Refaz as texturas das telas do Mistech com o tema do bloco de cada uma (tools/gui/themes.py), mantendo o layout.
As texturas de origem (estilo místico antigo, usadas para achar espaços e enfeites) ficam em tools/gui/source/.
Também gera client/GuiThemes.java (cores de texto, contorno e luzinhas de cada tela).
Uso (na pasta do projeto): python3 tools/gui/restyle.py [nome ...]"""
import sys, os, math, random
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
from classify import classify, lum
from themes import T
from sizes import SIZES

SRC = 'tools/gui/source/'
OUT = 'src/main/resources/assets/mistech/textures/gui/container/'
VAN = 'vanilla/assets/minecraft/textures/block/'
JAVA = 'src/main/java/com/mistech/mistechmod/client/GuiThemes.java'

def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))

def grad(cols, t):
    cols = [rgb(c) if isinstance(c, str) else c for c in cols]
    t = max(0.0, min(1.0, t))
    f = t * (len(cols) - 1)
    i = min(int(f), len(cols) - 2)
    r = f - i
    return tuple(round(cols[i][k] * (1 - r) + cols[i + 1][k] * r) for k in range(3))

def mix(a, b, t):
    return tuple(round(a[k] * (1 - t) + b[k] * t) for k in range(3))

def material(name, cols, contrast=1.0):
    """Textura vanilla 16x16 (primeiro quadro) mapeada no degradê pela luminância (contrast < 1 suaviza)."""
    im = Image.open(VAN + name + '.png').convert('RGBA')
    im = im.crop((0, 0, 16, 16))
    ls = [lum(im.getpixel((x, y))) for y in range(16) for x in range(16)]
    a, b = min(ls), max(ls)
    out = [[None] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            t = (lum(im.getpixel((x, y))) - a) / ((b - a) or 1)
            out[y][x] = grad(cols, 0.5 + (t - 0.5) * contrast)
    return out

def components(cls, W, H, target):
    seen = set()
    comps = []
    for y in range(H):
        for x in range(W):
            if cls[y][x] == target and (x, y) not in seen:
                stack = [(x, y)]; seen.add((x, y)); comp = []
                while stack:
                    cx, cy = stack.pop(); comp.append((cx, cy))
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = cx + dx, cy + dy
                        if 0 <= nx < W and 0 <= ny < H and (nx, ny) not in seen and cls[ny][nx] == target:
                            seen.add((nx, ny)); stack.append((nx, ny))
                comps.append(comp)
    return comps

def restyle(name):
    th = T[name]
    W, H = SIZES[name]
    src = Image.open(SRC + name + '.png').convert('RGBA')
    cls = classify(src, W, H)
    # pontinhos soltos (estrelas) não são enfeite
    for comp in components(cls, W, H, 4):
        if len(comp) < 3:
            for x, y in comp:
                cls[y][x] = 0
    # fundo cercado por bordas de espaço = poço (miolo escuro)
    outside = set()
    stack = [(6, 6)]
    while stack:
        x, y = stack.pop()
        if (x, y) in outside or not (0 <= x < W and 0 <= y < H) or cls[y][x] != 0:
            continue
        outside.add((x, y))
        stack += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
    for y in range(6, H - 6):
        for x in range(6, W - 6):
            if cls[y][x] == 0 and (x, y) not in outside:
                cls[y][x] = 1
    bg = material(*th['bg'], contrast=th.get('contrast', 0.7))
    fr = material(*th['frame'])
    slot_fill, slot_dark, slot_light = (rgb(c) for c in th['slot'])
    g = round(lum(slot_light))
    slot_light = mix(slot_light, (g, g, g), 0.3)
    accent = th['accent'][::-1] if th.get('accent_dark') else th['accent']
    corner = rgb(th['corner'])
    f_dark, f_mid, f_hi = rgb(th['frame'][1][0]), rgb(th['frame'][1][1]), rgb(th['frame'][1][2])
    deco = [lum(src.getpixel((x, y))) for y in range(H) for x in range(W) if cls[y][x] == 4]
    dl, dh = (min(deco), max(deco)) if deco else (0, 1)
    out = src.copy()
    rnd = random.Random(name)
    light = th.get('light', False)
    for y in range(H):
        for x in range(W):
            c = cls[y][x]
            edge = min(x, y, W - 1 - x, H - 1 - y)
            if c == 0:
                if edge < 6:
                    continue  # moldura, logo abaixo
                p = bg[y % 16][x % 16]
                # vinheta suave: mais escuro perto da moldura (mais claro nas telas claras)
                v = max(0.0, 1.0 - edge / 22.0)
                p = mix(p, (255, 255, 255) if light else (0, 0, 0), 0.18 * v if not light else 0.10 * v)
                out.putpixel((x, y), p + (255,))
            elif c == 1:
                out.putpixel((x, y), slot_fill + (255,))
            elif c == 2:
                out.putpixel((x, y), slot_dark + (255,))
            elif c == 3:
                out.putpixel((x, y), slot_light + (255,))
            elif c == 4:
                t = (lum(src.getpixel((x, y))) - dl) / ((dh - dl) or 1)
                out.putpixel((x, y), grad(accent, 0.25 + 0.75 * t) + (255,))
    # aros de ferro (barril): faixas horizontais nas linhas que são quase só fundo
    if th.get('hoops'):
        runs, start = [], None
        for y in range(8, H - 8):
            free = sum(1 for x in range(6, W - 6) if cls[y][x] == 0) >= 0.9 * (W - 12)
            if free and start is None:
                start = y
            if not free and start is not None:
                runs.append((start, y)); start = None
        for a, b2 in runs:
            if b2 - a >= 5 and a > 14 and b2 < H - 14:
                mid = (a + b2) // 2
                for y in range(mid - 1, mid + 2):
                    for x in range(6, W - 6):
                        p = fr[y % 16][x % 16]
                        p = mix(p, f_hi, 0.35) if y == mid - 1 else (mix(p, f_dark, 0.5) if y == mid + 1 else p)
                        out.putpixel((x, y), p + (255,))
                for x in range(10, W - 10, 18):
                    out.putpixel((x, mid), mix(f_hi, (255, 255, 255), 0.3) + (255,))
    # moldura: contorno, chanfro, material, filete interno
    for y in range(H):
        for x in range(W):
            edge = min(x, y, W - 1 - x, H - 1 - y)
            if edge >= 6:
                continue
            if edge == 0:
                p = f_dark
            elif edge == 1:
                p = mix(f_hi, (255, 255, 255), 0.15) if (x == 1 or y == 1) and x < W - 2 and y < H - 2 else f_dark
            elif edge <= 4:
                p = fr[y % 16][x % 16]
                if edge == 2 and (x == 2 or y == 2):
                    p = mix(p, f_hi, 0.45)
                if edge == 4 and (x == W - 5 or y == H - 5):
                    p = mix(p, f_dark, 0.45)
            else:
                p = mix(f_dark, slot_dark, 0.5)
            out.putpixel((x, y), p + (255,))
    # cantos: pedra preciosa num engaste
    def gem(cx, cy):
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                d = abs(dx) + abs(dy)
                if d <= 3:
                    p = f_dark if d == 3 else (mix(corner, (255, 255, 255), 0.55) if (dx, dy) in ((-1, -1), (0, -1)) else
                                                  corner if d <= 1 else mix(corner, f_dark, 0.45))
                    out.putpixel((cx + dx, cy + dy), p + (255,))
    for cx, cy in ((3, 3), (W - 4, 3), (3, H - 4), (W - 4, H - 4)):
        gem(cx, cy)
    gem(W // 2, 3)
    # rebites ao longo da moldura (temas de metal)
    if th.get('rivets'):
        for x in range(18, W - 18, 22):
            for yy in (2, H - 4):
                if abs(x - W // 2) > 6:
                    for dx, dy, col in ((0, 0, f_hi), (1, 0, f_mid), (0, 1, f_mid), (1, 1, f_dark)):
                        out.putpixel((x + dx, yy + dy), col + (255,))
        for y in range(18, H - 18, 22):
            for xx in (2, W - 4):
                for dx, dy, col in ((0, 0, f_hi), (1, 0, f_mid), (0, 1, f_mid), (1, 1, f_dark)):
                    out.putpixel((xx + dx, y + dy), col + (255,))
    out.save(OUT + name + '.png')

def java():
    def argb(h):
        return '0xFF' + h.upper()
    lines = []
    for name in sorted(T):
        th = T[name]
        motes = ', '.join('0x' + m.upper() for m in th['motes'])
        lines.append('        put("%s", new Theme(%s, %s, 0x%s, new int[] {%s}, %s, %s, %s));' % (
            name, argb(th['label']), argb(th['text']), th['glow'].upper(), motes, argb(th['slot'][0]),
            argb(th['accent'][1]), 'true' if th.get('light') else 'false'))
    src = '''package com.mistech.mistechmod.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Tema de cada tela (cores de texto, contorno pulsando e luzinhas), combinando com a textura do bloco que a abre.
 * GERADO por tools/gui/restyle.py a partir de tools/gui/themes.py - edite lá e rode o script.
 */
public final class GuiThemes {
    /**
     * @param label  títulos
     * @param text   textos secundários
     * @param glow   cor (RGB) do contorno que pulsa em volta do painel
     * @param motes  cores (RGB) das luzinhas subindo
     * @param well   miolo dos espaços (ARGB), para o que a tela desenha por código
     * @param accent cor de destaque (ARGB)
     * @param light  tela clara (textos escuros)
     */
    public record Theme(int label, int text, int glow, int[] motes, int well, int accent, boolean light) {}

    /** O visual místico de antes (telas sem tema próprio). */
    public static final Theme MYSTIC = new Theme(0xFFE3C98A, 0xFFC9B8E8, 0xB78CF0, new int[] {0xF0C870, 0xC49CF5, 0x9B6FE0},
            0xFF180F22, 0xFFB98CF0, false);

    private static final Map<String, Theme> THEMES = new HashMap<>();

    private static void put(String name, Theme theme) {
        THEMES.put(name, theme);
    }

    static {
%s
    }

    private GuiThemes() {}

    public static Theme get(String name) {
        return THEMES.getOrDefault(name, MYSTIC);
    }
}
''' % '\n'.join(lines)
    open(JAVA, 'w', encoding='utf-8').write(src)

if __name__ == '__main__':
    names = sys.argv[1:] or sorted(T)
    for n in names:
        restyle(n)
        print('ok', n)
    java()
