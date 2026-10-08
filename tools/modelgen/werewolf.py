import sys; sys.path.insert(0,'/home/claude/gen')
from modelgen import Model
# lobo-guará: laranja-avermelhado, "meias" pretas, crina preta, garganta e ponta da cauda brancas
ORANGE=0xc4602a; ORANGE_D=0x9c4a1e; BLACK=0x1c1612; WHITE=0xe6dccb; CLAW=0xd8d0bc; EAR_IN=0xd8c8b0
def headfront(p):
    p.row(1,ORANGE_D)
    # olhos fundos e amarelos, com centro vermelho
    p.px(1,2,BLACK); p.px(5,2,BLACK)
    p.px(1,3,0xffc838,True); p.px(2,3,0xff5a1a,True); p.px(4,3,0xff5a1a,True); p.px(5,3,0xffc838,True)
    p.px(0,3,BLACK); p.px(6,3,BLACK); p.px(3,3,ORANGE_D)
    p.rect(2,4,3,2,0x2a1e16)
def snoutfront(p):
    p.fill(BLACK,0.03); p.rect(0,0,p.w,1,0x0c0a08); p.row(p.h-1,0xeeeae0)
def snouttop(p):
    p.fill(0x2a1e16,0.04)
def jawtop(p):
    p.fill(0x6a1818,0.05)
    for x in range(0,p.w,2): p.px(x,0,0xf2eee4)
def throat(p):
    for y in range(p.h):
        for x in range(1,p.w-1):
            if y<5: p.px(x,y,WHITE if (x+y)%4 else 0xcfc4b2)
def maneback(p):
    p.fill(BLACK,0.06)
def earfront(p):
    p.fill(EAR_IN,0.04); p.px(0,0,BLACK); p.px(p.w-1,0,BLACK)
def tailtip(p):
    for y in range(p.h):
        for x in range(p.w):
            pass
def claws(p):
    for x in range(0,p.w,2): p.px(x,p.h-1,CLAW)
def m():
    M=Model('werewolf',128,seed=23)
    torso=M.part('torso',pivot=(0,5,2),rot=(0.55,0,0)).box((-3.5,-12,-2.5),(7,12,5),ORANGE,pattern='fur',faces={'front':throat})
    torso.child('mane',pivot=(0,-12,1.5),rot=(0.15,0,0)).box((-1.5,-2,0),(3,13,2),BLACK,pattern='fur')
    torso.child('ruff',pivot=(0,-12,0)).box((-4,-1,-3),(8,4,6),BLACK,pattern='fur')
    head=torso.child('head',pivot=(0,-12,-1.5)).box((-3.5,-6,-5),(7,6,6),ORANGE,pattern='fur',faces={'front':headfront})
    head.child('snout',pivot=(0,-3.5,-5)).box((-1.5,0,-6),(3,2,6),BLACK,faces={'front':snoutfront,'top':snouttop})
    head.child('jaw',pivot=(0,-1,-4.5)).box((-1.5,0,-6),(3,1,6),BLACK,faces={'top':jawtop})
    head.child('right_ear',pivot=(-2.5,-6,-2),rot=(-0.1,0,-0.2)).box((-1.5,-6,-0.5),(3,6,1),ORANGE,faces={'front':earfront})
    head.child('left_ear',pivot=(2.5,-6,-2),rot=(-0.1,0,0.2)).box((-1.5,-6,-0.5),(3,6,1),ORANGE,faces={'front':earfront})
    for side,sx in (('right',-1),('left',1)):
        arm=torso.child(f'{side}_arm',pivot=(sx*4.5,-10.5,0),rot=(-0.35,0,-sx*0.1)).box((-1.5,0,-1.5),(3,9,3),ORANGE,pattern='fur')
        fore=arm.child(f'{side}_forearm',pivot=(0,9,0),rot=(-0.45,0,0)).box((-1,0,-1),(2,9,2),BLACK,pattern='fur')
        hand=fore.child(f'{side}_hand',pivot=(0,9,0)).box((-1.5,0,-2),(3,2,3),BLACK)
        for i,cx in enumerate((-1,0,1)):
            hand.child(f'{side}_claw{i}',pivot=(cx,2,-1.5),rot=(-0.5,0,0)).box((-0.5,0,-0.5),(1,4,1),CLAW,noise=0.02)
    for side,sx in (('right',-1),('left',1)):
        leg=M.part(f'{side}_leg',pivot=(sx*2.2,4,2),rot=(-0.35,0,0)).box((-2,0,-2.5),(4,9,5),ORANGE,pattern='fur')
        shin=leg.child(f'{side}_shin',pivot=(0,9,2),rot=(0.75,0,0)).box((-1,0,-1),(2,11,2),BLACK,pattern='fur')
        shin.child(f'{side}_paw',pivot=(0,11,0),rot=(-0.4,0,0)).box((-1.5,0,-4),(3,2,5),BLACK,faces={'front':claws})
    tail=M.part('tail',pivot=(0,4.5,4),rot=(-0.9,0,0)).box((-1.5,-1.5,0),(3,3,9),ORANGE,pattern='fur')
    tail.child('tail_tip',pivot=(0,0,9)).box((-1.5,-1.5,0),(3,3,3),WHITE)
    return M
if __name__=='__main__':
    M=m(); M.pack(); M.paint('/home/claude/gen/out/werewolf.png','/home/claude/gen/out/werewolf_eyes.png')
    import preview; preview.render(M,'/home/claude/gen/out/werewolf.png','/home/claude/gen/out/werewolf_view.png',scale=8,yaws=(-25,90,150),pitch=8)
    from preview import world_transform, mat_vec
    lo=0;hi=99
    for p in M.parts:
        Mx,T=world_transform(p,{})
        for b in p.boxes:
            x0,y0,z0=b.origin; w,h,d=b.size
            for dx in (0,w):
                for dy in (0,h):
                    for dz in (0,d):
                        v=mat_vec(Mx,[x0+dx,y0+dy,z0+dz]); lo=max(lo,T[1]+v[1]); hi=min(hi,T[1]+v[1])
    print('pés em y',round(lo,1),'topo',round(hi,1),'altura',round((lo-hi)/16,2),'blocos')
