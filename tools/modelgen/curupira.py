import sys; sys.path.insert(0,'/home/claude/gen')
from modelgen import Model
SKIN=0x9c6a44; SKIN_HI=0xb07a50; SKIN_LO=0x7a5034
HAIR=0xff6a14; HAIR_D=0xe0480c; HAIR_Y=0xffc030
LEAF=0x4a8a2c; LEAF_D=0x2f6020; LEAF_H=0x6aac3a
def face(p):
    # olhos verdes grandes com brilho
    for x0 in (1,5):
        p.rect(x0,3,2,2,0xf2f0e6); p.px(x0+ (1 if x0==1 else 0),4,0x1e8a2a); p.px(x0+(1 if x0==1 else 0),3,0x2aa83a)
    p.px(1,3,0xffffff); p.px(6,3,0xffffff)
    # sardas e sorriso de canto
    p.px(1,5,SKIN_LO); p.px(6,5,SKIN_LO); p.px(2,6,SKIN_LO)
    p.rect(3,6,3,1,0x6a2a18); p.px(5,5,0x6a2a18)
def flames(p):
    # pontas amarelas em cima, laranja embaixo
    for y in range(p.h):
        for x in range(p.w):
            c = HAIR_Y if y < max(1,p.h//3) else (HAIR if (x+y)%3 else HAIR_D)
            p.px(x,y,c,True)
def hairtop(p):
    for y in range(p.h):
        for x in range(p.w):
            p.px(x,y,HAIR_Y if (x*3+y*5)%7==0 else HAIR,True)
def leaves(p):
    for y in range(p.h):
        for x in range(p.w):
            c = LEAF_H if (x+2*y)%5==0 else (LEAF_D if (x+y)%4==0 else LEAF)
            p.px(x,y,c)
    for x in range(0,p.w,2): p.px(x,p.h-1,LEAF_D)
def bracelet(p):
    p.row(p.h-2,LEAF)
def m():
    M=Model('curupira',64,seed=11)
    # pés virados para trás
    for side,sx in (('right_leg',-1.2),('left_leg',1.2)):
        M.part(side,pivot=(sx,18,0)).box((-1,0,-1),(2,5,2),SKIN).box((-1,5,-1),(2,1,4),SKIN_LO)
    M.part('skirt',pivot=(0,15,0)).box((-3.5,0,-2.5),(7,4,5),LEAF,faces={k:leaves for k in ('front','back','left','right')})
    M.part('body',pivot=(0,10,0)).box((-2.5,0,-1.5),(5,5,3),SKIN,faces={'front':lambda p:(p.rect(1,0,3,1,SKIN_HI),p.px(2,3,SKIN_LO))})
    head=M.part('head',pivot=(0,10,0)).box((-4,-8,-4),(8,8,8),SKIN,faces={'front':face})
    head.child('nose',pivot=(0,-4,-4)).box((-0.5,0,-1),(1,1,1),SKIN_HI)
    hair=head.child('hair',pivot=(0,-8,0)).box((-4.5,-2,-4.5),(9,3,9),HAIR,glow=True,faces={'top':hairtop,'front':lambda p:p.row(p.h-1,HAIR_D,True)})
    hair.child('mane',pivot=(0,0,4)).box((-4.5,0,-0.5),(9,6,2),HAIR,glow=True,faces={'back':flames})
    hair.child('flame_a',pivot=(-2.5,-2,0),rot=(-0.2,0,-0.35)).box((-1,-4,-1),(2,4,2),HAIR,glow=True,faces={k:flames for k in ('front','back','left','right')})
    hair.child('flame_b',pivot=(0,-2,0.5),rot=(-0.25,0,0)).box((-1.5,-6,-1.5),(3,6,3),HAIR,glow=True,faces={k:flames for k in ('front','back','left','right')})
    hair.child('flame_c',pivot=(2.5,-2,0),rot=(-0.2,0,0.35)).box((-1,-4,-1),(2,4,2),HAIR,glow=True,faces={k:flames for k in ('front','back','left','right')})
    hair.child('flame_d',pivot=(0,-1,3),rot=(-0.7,0,0)).box((-1,-4,-1),(2,4,2),HAIR,glow=True,faces={k:flames for k in ('front','back','left','right')})
    M.part('right_arm',pivot=(-2.5,10.5,0)).box((-2,0,-1),(2,6,2),SKIN,faces={'front':bracelet,'back':bracelet,'left':bracelet,'right':bracelet})
    M.part('left_arm',pivot=(2.5,10.5,0)).box((0,0,-1),(2,6,2),SKIN,faces={'front':bracelet,'back':bracelet,'left':bracelet,'right':bracelet})
    return M
if __name__=='__main__':
    M=m(); M.pack(); M.paint('/home/claude/gen/out/curupira.png','/home/claude/gen/out/curupira_glow.png')
    import preview; preview.render(M,'/home/claude/gen/out/curupira.png','/home/claude/gen/out/curupira_view.png',scale=11,yaws=(-25,150),pitch=10)
