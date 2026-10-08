import sys; sys.path.insert(0,'/home/claude/gen')
from modelgen import Model
F1=0xf06a18; F2=0xffc63a; F3=0xc8301a; CORE=0x4a1206; BELLY=0xffd870
def eye(p):
    p.fill(0xfff4c0,0.02)
    p.rect(1 if p.w>2 else 0,1 if p.h>2 else 0,max(1,p.w-2),max(1,p.h-2),0xffffff)
    p.px(p.w//2,p.h//2,0xffa020)
def snoutfront(p):
    p.px(1,1,CORE); p.px(p.w-2,1,CORE)
    p.row(p.h-1,CORE)
def flametip(p):
    for y in range(p.h):
        for x in range(p.w):
            p.px(x,y,F2 if y < p.h//2 else F1)
def belly(p):
    for y in range(p.h):
        for x in range(p.w):
            p.px(x,y,BELLY if (x+y)%3 else F2)
def mouth(p):
    p.fill(CORE,0.05)
def head():
    M=Model('boitata',128,seed=31)
    M.part('skull',pivot=(0,24,0)).box((-5,-6,-7),(10,6,12),F1,pattern='scales',faces={'bottom':belly})
    M.part('brow',pivot=(0,18,-5)).box((-5.5,-1,-2),(11,1,4),F3)
    sn=M.part('snout',pivot=(0,24,0)).box((-3.5,-5,-12),(7,4,5),F1,pattern='scales',faces={'front':snoutfront,'bottom':mouth})
    sn.child('right_fang',pivot=(-2.5,-1,-11)).box((-0.5,0,-0.5),(1,2,1),0xfff8e8,noise=0.01)
    sn.child('left_fang',pivot=(2.5,-1,-11)).box((-0.5,0,-0.5),(1,2,1),0xfff8e8,noise=0.01)
    M.part('jaw',pivot=(0,22,-5),rot=(0.25,0,0)).box((-4,0,-7),(8,2,8),F3,pattern='scales',faces={'top':mouth,'bottom':belly})
    M.part('right_eye',pivot=(0,24,0)).box((-7,-6.5,-6.5),(3,3,3),0xfff0a0,faces={k:eye for k in ('right','front','top')})
    M.part('left_eye',pivot=(0,24,0)).box((4,-6.5,-6.5),(3,3,3),0xfff0a0,faces={k:eye for k in ('left','front','top')})
    for i,(z,h,r) in enumerate(((-4,6,-0.4),(-0.5,9,-0.6),(3,7,-0.8),(5,5,-1.0))):
        M.part('crest_'+'abcd'[i],pivot=(0,18,z),rot=(r,0,0)).box((-1,-h,-1.5),(2,h,3),F1,faces={k:flametip for k in ('front','back','left','right')})
    M.part('right_whisker',pivot=(-3.5,21,-11),rot=(0,0.6,0.25)).box((-7,-0.5,-0.5),(7,1,1),F2)
    M.part('left_whisker',pivot=(3.5,21,-11),rot=(0,-0.6,-0.25)).box((0,-0.5,-0.5),(7,1,1),F2)
    return M
def segment():
    M=Model('boitata_segment',64,seed=33)
    M.part('body',pivot=(0,0,0)).box((-5,-5,-5),(10,10,10),F1,pattern='scales',faces={'bottom':belly})
    M.part('fin',pivot=(0,-5,0),rot=(-0.4,0,0)).box((-1,-5,-3),(2,5,6),F1,faces={k:flametip for k in ('front','back','left','right')})
    return M
if __name__=='__main__':
    import preview
    H=head(); H.pack(); H.paint('/home/claude/gen/out/boitata.png')
    S=segment(); S.pack(); S.paint('/home/claude/gen/out/boitata_segment.png')
    preview.render(H,'/home/claude/gen/out/boitata.png','/home/claude/gen/out/boitata_view.png',scale=8,yaws=(-30,30,90),pitch=15)
    print(H.tex_w,H.tex_h)
