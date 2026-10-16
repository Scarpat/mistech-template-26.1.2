"""Sela da Baleia-Nuvem: pinta a UV da sela (y >= 150) na textura da baleia e gera uma prévia.
Rodar DEPOIS de mobs3.py (que repinta a baleia inteira). Os cubos estão à mão em CloudWhaleModel (parte "saddle").
Uso (raiz do projeto): python tools/dream/whale_saddle.py -> build/dream/cloud_whale_saddle_view.png"""
import os, sys
sys.path.insert(0, 'tools/modelgen')
sys.path.insert(0, 'tools/dream')
from PIL import Image
from modelgen import Box
import preview
import mobs3

T = 'src/main/resources/assets/mistech/textures/entity/'
# (uv, origem, tamanho, tipo) — igual ao Java
BOXES = [((0, 150), (-4.5, -11, -10), (9, 3, 7), 'seat'),
         ((32, 150), (-1.5, -13, -11), (3, 2, 2), 'pommel'),
         ((44, 150), (-11, -2, -8), (1, 6, 1), 'stirrup'),
         ((50, 150), (10, -2, -8), (1, 6, 1), 'stirrup'),
         # cilha: 4 tiras por fora do corpo (sem placa atravessando a baleia)
         ((0, 160), (-9, -9, -8.5), (18, 1, 2), 'strap'),
         ((0, 164), (-9, 8, -8.5), (18, 1, 2), 'strap'),
         ((40, 160), (-10, -9, -8.5), (1, 18, 2), 'side'),
         ((46, 160), (9, -9, -8.5), (1, 18, 2), 'side')]

# couro no tom da sela do jogo
L_DARK, L_MID, L_LIGHT, L_HI = (0x4a, 0x2a, 0x16), (0x6b, 0x40, 0x22), (0x8a, 0x56, 0x2e), (0xa3, 0x6c, 0x3c)
IRON, IRON_D = (0xc8, 0xc8, 0xd0), (0x7a, 0x7a, 0x88)


def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


def rect(px, x, y, w, h, c):
    for j in range(h):
        for i in range(w):
            px[x + i, y + j] = c + (255,)


def paint(img):
    px = img.load()
    for (u, v), o, (w, h, d), kind in BOXES:
        # limpa a área inteira da caixa
        for j in range(d + h):
            for i in range(2 * (w + d)):
                px[u + i, v + j] = (0, 0, 0, 0)
        F = faces(u, v, w, h, d)
        for name, (x, y, fw, fh) in F.items():
            base = {'top': L_LIGHT, 'bottom': L_DARK}.get(name, L_MID)
            if kind == 'stirrup':
                base = IRON_D if name != 'top' else IRON
            rect(px, x, y, fw, fh, base)
            if kind in ('seat', 'pommel') and name != 'bottom' and fw > 2 and fh > 2:
                # borda escura + costura clara em manchas largas
                for i in range(fw):
                    px[x + i, y + fh - 1] = L_DARK + (255,)
                if name == 'top':
                    for i in range(1, fw - 1):
                        px[x + i, y] = L_HI + (255,)
                    rect(px, x + 1, y + 1, fw - 2, fh - 2, L_LIGHT)
                    rect(px, x + 2, y + 2, fw - 4, fh - 3, L_HI if kind == 'pommel' else (0x96, 0x5f, 0x34))
            if kind == 'stirrup' and name in ('front', 'back', 'left', 'right'):
                px[x, y + fh - 1] = IRON + (255,)
        if kind == 'side':
            # fivela de ferro no meio da tira lateral (face de fora e de dentro)
            for name in ('right', 'left'):
                x, y, fw, fh = F[name]
                rect(px, x, y + fh // 2 - 2, fw, 4, IRON)
                px[x, y + fh // 2 + 1] = IRON_D + (255,)
        if kind in ('strap', 'side'):
            # borda escura nas faces largas
            for name in ('front', 'back', 'top', 'bottom'):
                x, y, fw, fh = F[name]
                if fw >= 3 and fh >= 3:
                    for i in range(fw):
                        px[x + i, y] = L_DARK + (255,)
                        px[x + i, y + fh - 1] = L_DARK + (255,)

def main():
    img = Image.open(T + 'cloud_whale.png').convert('RGBA')
    paint(img)
    img.save(T + 'cloud_whale.png')
    glow = Image.open(T + 'cloud_whale_glow.png').convert('RGBA')
    gp = glow.load()
    for y in range(150, 184):
        for x in range(64):
            gp[x, y] = (0, 0, 0, 0)
    glow.save(T + 'cloud_whale_glow.png')
    # prévia
    M = mobs3.whale()
    M.pack()
    body = [p for p in M.parts if p.name == 'body'][0]
    for (u, v), o, s, kind in BOXES:
        b = Box(o, s, (0, 0, 0))
        b.uv = (u, v)
        body.boxes.append(b)
    os.makedirs('build/dream', exist_ok=True)
    preview.render(M, T + 'cloud_whale.png', 'build/dream/cloud_whale_saddle_view.png', scale=7)


if __name__ == '__main__':
    main()
