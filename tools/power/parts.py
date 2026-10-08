"""Texturas de peças dos geradores avançados (16x16, algumas animadas)."""
from PIL import Image
import math, random
V='/home/claude/gw/vanilla/assets/minecraft/textures/block/'
OUT='/home/claude/w/src/main/resources/assets/mistech/textures/block/'
def van(n): return Image.open(V+n+'.png').convert('RGBA').crop((0,0,16,16))
def hx(h,a=255): return tuple(int(h[i:i+2],16) for i in (0,2,4))+(a,)
def mix(a,b,t): return tuple(round(a[i]*(1-t)+b[i]*t) for i in range(3))+(255,)
def lum(p): return (p[0]*0.3+p[1]*0.59+p[2]*0.11)/255
def tint(im, dark, light, contrast=1.0):
    out=im.copy()
    ls=[lum(im.getpixel((x,y))) for y in range(16) for x in range(16)]
    a,b=min(ls),max(ls)
    for y in range(16):
        for x in range(16):
            t=(lum(im.getpixel((x,y)))-a)/((b-a) or 1); t=0.5+(t-0.5)*contrast
            out.putpixel((x,y),mix(hx(dark),hx(light),max(0,min(1,t))))
    return out
def save(im,name): im.save(OUT+name+'.png')
def anim(frames,name,ft=8):
    W=Image.new('RGBA',(16,16*len(frames)))
    for i,f in enumerate(frames): W.paste(f,(0,16*i))
    W.save(OUT+name+'.png')
    open(OUT+name+'.png.mcmeta','w').write('{\n  "animation": {\n    "interpolate": true,\n    "frametime": %d\n  }\n}\n'%ft)
def rivet(im,x,y,c='d8a05a'):
    im.putpixel((x,y),hx(c)); 
    if x+1<16 and y+1<16: im.putpixel((x+1,y+1),mix(hx(c),(0,0,0),0.6))

# ===== Caldeira Ígnea =====
shell=tint(van('polished_blackstone'),'16100f','4a3a36',0.6)
for y in range(16):
    for x in range(16):
        if y in (0,8): shell.putpixel((x,y),hx('0c0808'))
        if y in (1,9): shell.putpixel((x,y),mix(shell.getpixel((x,y)),hx('7a6a62'),0.4))
        if x==0 or (x==8 and y<8) or (x==4 and y>8) or (x==12 and y>8): shell.putpixel((x,y),hx('0c0808'))
for y in (2,10):
    for x in (2,6,10,14): rivet(shell,x,y)
save(shell,'igneous_boiler_shell')
# faixa de latão
band=tint(van('gold_block'),'5a3a10','f0c060',0.8)
for x in range(16): band.putpixel((x,0),hx('2a1a06')); band.putpixel((x,15),hx('2a1a06'))
for x in range(1,16,4): rivet(band,x,7,'fff0b0')
save(band,'igneous_boiler_band')
# manômetro
g=Image.new('RGBA',(16,16),(0,0,0,0))
for y in range(16):
    for x in range(16):
        d=math.hypot(x-7.5,y-7.5)
        if d<7.6: g.putpixel((x,y),hx('b88a30') if d>6.2 else hx('8a6420') if d>5.6 else mix(hx('f4ecd8'),hx('d8ccb0'),d/5.6))
for a in range(-135,136,45):
    r=math.radians(a-90); x=round(7.5+4.6*math.cos(r)); y=round(7.5+4.6*math.sin(r)); g.putpixel((x,y),hx('3a3028'))
for i in range(5):
    r=math.radians(-150); x=round(7.5+i*math.cos(r)); y=round(7.5+i*math.sin(r)); g.putpixel((x,y),hx('c81a10'))
g.putpixel((7,7),hx('2a2a2a')); g.putpixel((8,8),hx('2a2a2a'))
save(g,'igneous_boiler_gauge')
# volante da válvula (recorte)
v=Image.new('RGBA',(16,16),(0,0,0,0))
for y in range(16):
    for x in range(16):
        d=math.hypot(x-7.5,y-7.5)
        if 5.6<d<7.6: v.putpixel((x,y),hx('b02a1a') if (x+y)%5 else hx('6a140a'))
        elif d<1.8: v.putpixel((x,y),hx('d8b060'))
        elif d<6 and (abs(x-7.5)<1 or abs(y-7.5)<1): v.putpixel((x,y),hx('8a1a10'))
save(v,'igneous_boiler_valve')

# ===== Reator Impuro =====
def casing(on):
    c=tint(van('polished_deepslate'),'0e0a14','3a3048',0.7)
    for y in range(16):
        for x in range(16):
            if x in (0,15) or y in (0,15): c.putpixel((x,y),hx('08060c'))
            elif x in (1,14) or y in (1,14): c.putpixel((x,y),hx('4a3e5a'))
    # grade de ventilação
    for y in (4,6,8,10):
        for x in range(4,12):
            c.putpixel((x,y),hx('050308'))
            c.putpixel((x,y+1),hx('b05cff') if on else hx('2a2036'))
    for x,y in ((2,2),(13,2),(2,13),(13,13)): rivet(c,x,y,'8a6ab0')
    return c
save(casing(False),'impure_reactor_casing'); save(casing(True),'impure_reactor_casing_on')
def panel(on, ph):
    p=tint(van('polished_deepslate'),'120c18','3a3048',0.5)
    for x in range(16): p.putpixel((x,0),hx('8a6ab0')); p.putpixel((x,15),hx('08060c'))
    # botões e luzes
    cols=['ff4a6a','b05cff','5affd0','ffd05a']
    for i,x in enumerate((2,6,10)):
        on_i = on and ((i+ph)%3!=0)
        for dx in (0,1,2):
            for dy in (3,4):
                p.putpixel((x+dx,dy), hx(cols[i]) if on_i else mix(hx(cols[i]),(0,0,0),0.75))
    # mostrador de onda
    for x in range(2,14):
        p.putpixel((x,7),hx('050308')); p.putpixel((x,12),hx('050308'))
        for y in range(8,12): p.putpixel((x,y),hx('0a0612'))
        if on:
            y=9+round(1.6*math.sin((x+ph*2)/1.6)); p.putpixel((x,max(8,min(11,y))),hx('d07aff'))
    for y in range(7,13): p.putpixel((1,y),hx('050308')); p.putpixel((14,y),hx('050308'))
    return p
save(panel(False,0),'impure_reactor_panel'); anim([panel(True,i) for i in range(3)],'impure_reactor_panel_on',6)

# ===== Observatório =====
tube=tint(van('gold_block'),'4a2e0a','e8c46a',0.6)
for y in range(16):
    for x in range(16):
        sh=abs(x-7.5)/7.5
        tube.putpixel((x,y),mix(tube.getpixel((x,y)),hx('2a1a06'),sh*0.55))
for y in (2,3,12,13):
    for x in range(16): tube.putpixel((x,y),mix(hx('fff0b0'),hx('8a6420'),abs(x-7.5)/7.5))
save(tube,'stellar_observatory_tube')
base=tint(van('lapis_block'),'060a24','2a3e8a',0.7)
for x in range(16):
    base.putpixel((x,0),hx('e8c46a')); base.putpixel((x,15),hx('7a5a20'))
for x,y in ((3,4),(6,7),(10,5),(12,9),(4,11),(9,12)): base.putpixel((x,y),hx('c0d0ff'))
for (a,b) in (((3,4),(6,7)),((6,7),(10,5)),((10,5),(12,9))):
    n=8
    for i in range(1,n):
        x=round(a[0]+(b[0]-a[0])*i/n); y=round(a[1]+(b[1]-a[1])*i/n)
        base.putpixel((x,y),mix(base.getpixel((x,y)),hx('8aa0e0'),0.45))
save(base,'stellar_observatory_side')

# ===== Gerador de Experiência =====
def tank(on, ph):
    t=Image.new('RGBA',(16,16),(0,0,0,0))
    lvl=5 if on else 11
    for y in range(16):
        for x in range(16):
            edge = x in (0,15)
            if edge: t.putpixel((x,y),(200,255,220,150))
            elif y>=lvl:
                c=mix(hx('2a8a1a'),hx('a8f040'),0.35+0.4*math.sin((x+y+ph)/3)**2)
                t.putpixel((x,y),c[:3]+(235,))
            elif x in (1,2): t.putpixel((x,y),(230,255,240,90))
    if on:
        for bx,by in ((4,13),(9,10),(12,14),(6,8)):
            yy=lvl+((by-lvl)-ph)%(16-lvl)
            t.putpixel((bx,yy),hx('f0ffb0'))
    for x in range(16): t.putpixel((x,lvl),hx('d8ff5a') if on else hx('6aa83a'))
    return t
save(tank(False,0),'experience_generator_tank'); anim([tank(True,i) for i in range(4)],'experience_generator_tank_on',5)
def xcasing(on):
    c=tint(van('deepslate_tiles'),'041012','1e3e3e',0.7)
    for y in range(16):
        for x in range(16):
            if x in (0,15) or y in (0,15): c.putpixel((x,y),hx('020808'))
    for x in range(2,14): c.putpixel((x,7),hx('8af040') if on else hx('1a4a2a')); c.putpixel((x,8),hx('020808'))
    for y in range(2,14): c.putpixel((7,y),hx('8af040') if on else hx('1a4a2a'))
    c.putpixel((7,7),hx('f0ffb0') if on else hx('2a6a3a'))
    for x,y in ((2,2),(12,2),(2,12),(12,12)): rivet(c,x,y,'3ad8a0')
    return c
save(xcasing(False),'experience_generator_casing'); save(xcasing(True),'experience_generator_casing_on')
print('ok')
