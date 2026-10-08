import sys; sys.path.insert(0,'/home/claude/gen')
from modelgen import Model
BLACK=0x1b1b24; SHEEN=0x2a3352; SHEEN2=0x3a2f52; BEAK=0x2c2c30; BEAK_HI=0x4a4a52; LEG=0x2e2e34
def eye(p):
    x = 1 if p.w > 2 else 0
    p.px(x,1,0x0a0a0a); p.px(x,0,0x4a5068); p.px(min(p.w-1,x+1),1,0xd8dce8)
def feathers(p):
    # penas pretas com reflexo azulado, pontas das rêmiges mais escuras
    for y in range(p.h):
        for x in range(p.w):
            c = SHEEN if (x*7+y*3)%9==0 else (0x222838 if y < p.h//2 else BLACK)
            if y >= p.h-3 and x%2==0: c = 0x101018
            p.px(x,y,c)
def tailfan(p):
    for x in range(p.w):
        for y in range(p.h):
            p.px(x,y,SHEEN if x%2 else BLACK)
def chest(p):
    for y in range(p.h):
        for x in range(p.w):
            if (x+y*2)%4==0: p.px(x,y,0x24243a)
def m():
    M=Model('raven',64,seed=3)
    M.part('body',pivot=(0,22,0),rot=(0.2,0,0)).box((-2.5,-7,-2.5),(5,7,5),BLACK,faces={'front':chest,'back':feathers})
    head=M.part('head',pivot=(0,15.5,-0.5)).box((-2.5,-5,-3),(5,5,5),BLACK,faces={'right':eye,'left':eye,'top':lambda p:p.fill(SHEEN,0.05)})
    head.child('beak',pivot=(0,-2.5,-3),rot=(0.15,0,0)).box((-1,-1,-3),(2,2,3),BEAK,faces={'top':lambda p:p.fill(BEAK_HI,0.03)}).box((-0.5,0,-4),(1,1,1),0x1e1e22)
    head.child('crest',pivot=(0,-5,0),rot=(-0.35,0,0)).box((-1,-1,-2),(2,1,4),SHEEN)
    M.part('left_wing',pivot=(2.5,15.5,0)).box((0,0,-2.5),(1,8,6),BLACK,faces={'left':feathers,'right':feathers})
    M.part('right_wing',pivot=(-2.5,15.5,0)).box((-1,0,-2.5),(1,8,6),BLACK,faces={'left':feathers,'right':feathers},mirror=True)
    M.part('tail',pivot=(0,21,2),rot=(-0.6,0,0)).box((-2,-0.5,0),(4,1,6),BLACK,faces={'top':tailfan,'bottom':tailfan})
    for side,sx in (('left_leg',1.2),('right_leg',-1.2)):
        M.part(side,pivot=(sx,22,0)).box((-0.5,0,-0.5),(1,2,1),LEG).box((-1,1.5,-2),(2,0.5,3),LEG)
    return M
if __name__=='__main__':
    M=m(); M.pack(); M.paint('/home/claude/gen/out/raven.png')
    import preview; preview.render(M,'/home/claude/gen/out/raven.png','/home/claude/gen/out/raven_view.png',scale=14,yaws=(-30,90),pitch=10)
