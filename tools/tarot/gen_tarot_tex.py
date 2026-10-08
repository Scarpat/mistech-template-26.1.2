"""Gera as texturas do Tarô (cartas, Carta em Branco, pano/carta da Mesa de Tarologia e a Fenda Arcana).
Uso: python gen_tarot_tex.py <pasta assets/mistech/textures>"""
import sys, os, math, random
from PIL import Image

OUT = sys.argv[1]
ITEM = os.path.join(OUT, 'item'); BLOCK = os.path.join(OUT, 'block')

ARCANA = [  # id, cor
 ('the_fool',0xF2D16B),('the_magician',0xD9493C),('the_high_priestess',0x4F6FD1),('the_empress',0x5DBB63),
 ('the_emperor',0xC4402F),('the_hierophant',0xE0B85A),('the_lovers',0xF07AA8),('the_chariot',0x6E8FB3),
 ('strength',0xE8913A),('the_hermit',0x8C8C9E),('wheel_of_fortune',0x9B5FD6),('justice',0xD8D8E8),
 ('the_hanged_man',0x3FA6A0),('death',0x3A3A44),('temperance',0x7FC8E8),('the_devil',0x8E1F2A),
 ('the_tower',0xB7A27A),('the_star',0x9FD8FF),('the_moon',0xB9B6E8),('the_sun',0xFFC93C),
 ('judgement',0xE86A5A),('the_world',0x4FC48A)]

# símbolos 7x7: 1 = cor do arcano, 2 = brilho
G = {
'the_fool':      ["..111..",".1...1.","1.....1","1..2..1","1.....1",".1...1.","..111.."],
'the_magician':  [".......",".11.11.","1..1..1","1..2..1","1..1..1",".11.11.","......."],
'the_high_priestess':["..111..",".11....","11.....","11..2..","11.....",".11....","..111.."],
'the_empress':   ["..111..",".1...1.",".1.2.1.","..111..","...1...",".11111.","...1..."],
'the_emperor':   ["1..1..1","11.1.11","1111111","1211121","1111111",".......","1111111"],
'the_hierophant':["...2...","..111..","...1...",".11111.","...1...","1111111","...1..."],
'the_lovers':    [".11.11.","1111111","1121111","1111111",".11111.","..111..","...1..."],
'the_chariot':   ["...1...","..111..",".1.2.1.","1..1..1","...1...","..1.1..",".1...1."],
'strength':      ["...1...","..11...","..121..",".11211.",".12221.","1122211",".11111."],
'the_hermit':    ["..111..","...1...",".11111.",".12221.",".12221.",".11111.","..111.."],
'wheel_of_fortune':["..111..",".1.1.1.","1..1..1","1112111","1..1..1",".1.1.1.","..111.."],
'justice':       ["...2...","1111111","1..1..1","1..1..1","11.1.11","...1...",".11111."],
'the_hanged_man':["1111111",".1...1.",".1...1.","..1.1..","..121..","...1...","......."],
'death':         [".11111.","1111111","1.111.1","1.121.1","1111111",".1.1.1.",".1.1.1."],
'temperance':    ["1111111","1222221",".11111.","..111..","...1...","...1...",".11111."],
'the_devil':     ["1.....1","11...11",".11111.","1.111.1","1112111",".11111.","..1.1.."],
'the_tower':     ["..121..","..111..",".11111.",".1.1.1.",".11111.",".1.1.1.","1111111"],
'the_star':      ["...1...","1..1..1",".11111.","1122211",".11111.","1..1..1","...1..."],
'the_moon':      ["..111..",".11122.","1112...","112....","1112...",".11122.","..111.."],
'the_sun':       ["1..1..1",".11111.",".12221.","1122211",".12221.",".11111.","1..1..1"],
'judgement':     ["1.....1","11...11","111.111","1112111",".11111.","..111..","...1..."],
'the_world':     ["..111..",".1...1.","1..2..1","1.222.1","1..2..1",".1...1.","..111.."],
}

def rgb(c): return ((c>>16)&255,(c>>8)&255,c&255,255)
def mix(a,b,t): return tuple(int(a[i]*(1-t)+b[i]*t) for i in range(3))+(255,)

def card(color, glyph, inverted=False, blank=False):
    im = Image.new('RGBA',(16,16),(0,0,0,0)); p = im.load()
    x0,x1,y0,y1 = 3,12,0,15
    if inverted:
        body,edge,trim = (46,16,52,255),(30,8,20,255),(142,31,42,255)
    else:
        body,edge,trim = (240,227,192,255),(90,62,26,255),(201,160,64,255)
    for x in range(x0,x1+1):
        for y in range(y0,y1+1):
            border = x in (x0,x1) or y in (y0,y1)
            corner = (x in (x0,x1)) and (y in (y0,y1))
            if corner: continue
            p[x,y] = edge if border else body
    # filete interno
    for x in range(x0+1,x1):
        p[x,y0+1]=trim; p[x,y1-1]=trim
    for y in range(y0+1,y1):
        p[x0+1,y]=trim; p[x1-1,y]=trim
    # sombra à direita/baixo
    for y in range(y0+1,y1+1):
        if p[x1,y][3]: p[x1,y]=mix(edge,(0,0,0),0.3)
    if blank:
        for (x,y) in [(7,6),(8,7),(7,8),(6,7),(7,7)]:
            p[x,y]=(220,200,150,255)
        return im
    c = rgb(color)
    if inverted:
        c = mix(mix(c,(150,30,50),0.3),(255,210,220),0.2)
    hi = mix(c,(255,255,255),0.6)
    panel = mix(c,body,0.85) if not inverted else mix(c,body,0.75)
    for x in range(x0+2,x1-1):
        for y in range(y0+3,y1-2):
            p[x,y]=panel
    rows = glyph[::-1] if inverted else glyph
    if inverted: rows=[r[::-1] for r in rows]
    gx,gy = 4,4
    for j,row in enumerate(rows):
        for i,ch in enumerate(row):
            if ch=='1': p[gx+i,gy+j]=c if not inverted else mix(c,(255,80,90),0.15)
            elif ch=='2': p[gx+i,gy+j]=hi
    # pontinhos dourados no topo/rodapé
    p[7,2]=trim if not inverted else (200,60,70,255); p[8,2]=p[7,2]
    p[7,13]=p[7,2]; p[8,13]=p[7,2]
    if inverted:  # rachadura
        for (x,y) in [(10,3),(9,4),(10,5)]:
            p[x,y]=(10,0,8,255)
    return im

os.makedirs(ITEM,exist_ok=True)
for aid,col in ARCANA:
    card(col,G[aid]).save(os.path.join(ITEM,f'tarot_card_{aid}.png'))
    card(col,G[aid],inverted=True).save(os.path.join(ITEM,f'inverted_tarot_card_{aid}.png'))
card(0,None,blank=True).save(os.path.join(ITEM,'blank_tarot_card.png'))

# ----- pano da Mesa de Tarologia (16x128, 8 quadros: estrelinhas douradas piscando)
random.seed(7)
frames=8; cloth=Image.new('RGBA',(16,16*frames)); cp=cloth.load()
stars=[(random.randint(4,11),random.randint(1,14),random.random()*6.28) for _ in range(7)]
for f in range(frames):
    for x in range(16):
        for y in range(16):
            n=random.random()*0.08
            base=(int(70+40*n*10),int(14+6*n*10),int(52+20*n*10),255)
            if x in (3,12): base=(201,160,64,255)
            elif x in (4,11): base=(120,30,70,255)
            cp[x,y+16*f]=base
    for (sx,sy,ph) in stars:
        b=0.5+0.5*math.sin(ph+f*6.28/frames)
        cp[sx,sy+16*f]=mix((120,40,90),(255,226,140),b)
    # lua crescente no meio
    for (x,y) in [(7,7),(8,6),(8,8),(9,6),(9,8)]:
        cp[x,y+16*f]=(240,220,150,255)
cloth.save(os.path.join(BLOCK,'tarot_cloth.png'))
open(os.path.join(BLOCK,'tarot_cloth.png.mcmeta'),'w').write('{"animation":{"frametime":5,"interpolate":true}}')

# ----- carta aberta sobre a mesa (região 5..11 x 4..11)
spread=Image.new('RGBA',(16,16),(0,0,0,0)); sp=spread.load()
for x in range(5,11):
    for y in range(4,11):
        edge = x in (5,10) or y in (4,10)
        sp[x,y]=(201,160,64,255) if edge else (60,20,80,255)
for (x,y) in [(7,6),(8,6),(7,7),(8,7),(7,8),(8,8)]:
    sp[x,y]=(240,220,150,255)
sp[7,5]=sp[8,9]=(176,76,255,255)
spread.save(os.path.join(BLOCK,'tarot_spread.png'))

# ----- Fenda Arcana (16x256, 16 quadros: redemoinho violeta translúcido)
frames=16; rift=Image.new('RGBA',(16,16*frames)); rp=rift.load()
for f in range(frames):
    t=f/frames*2*math.pi
    for x in range(16):
        for y in range(16):
            dx=(x-7.5)/7.5; dy=(y-7.5)/7.5
            r=math.sqrt(dx*dx+dy*dy)
            a=math.atan2(dy,dx)
            v=0.5+0.5*math.sin(a*3+r*9-t*2)
            w=0.5+0.5*math.sin(a*2-r*6+t)
            k=0.55*v+0.45*w
            col=mix((40,0,70),(200,90,255),k)
            col=mix(col,(255,220,255),max(0,k-0.85)*4)
            alpha=int(150+90*k)
            rp[x,y+16*f]=col[:3]+(alpha,)
rift.save(os.path.join(BLOCK,'arcana_rift.png'))
open(os.path.join(BLOCK,'arcana_rift.png.mcmeta'),'w').write('{"animation":{"frametime":2,"interpolate":true}}')
print('ok')
