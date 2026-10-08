import sys; sys.path.insert(0,'/home/claude/gen')
from modelgen import Model
SKIN=0x5e3b25; SKIN_HI=0x74492e; SKIN_LO=0x452a1a
CAP=0xc8231b; CAP_LO=0x8a1510; SHORTS=0xe2d6b8; ROPE=0x8a6a3a
WOOD=0x7a4a26; BOWL=0x5a3418
def face(p):
    # sobrancelhas marotas
    p.px(1,2,0x22140c); p.px(2,2,0x22140c); p.px(5,2,0x22140c); p.px(6,2,0x22140c)
    p.px(2,1,0x22140c); p.px(5,1,0x22140c)
    # olhos com brilho
    for x0,pup in ((1,2),(5,5)):
        p.rect(x0,3,2,2,0xf4efe4)
        p.px(pup,3,0x120a06); p.px(pup,4,0x120a06)
    p.px(1,3,0xffffff); p.px(6,3,0xffffff)
    # bochechas
    p.px(0,5,SKIN_HI); p.px(7,5,SKIN_HI)
    # sorriso largo de travesso
    p.px(1,5,0x2a140c); p.px(6,5,0x2a140c)
    p.rect(2,6,4,1,0xf2eee2); p.px(1,6,0x2a140c); p.px(6,6,0x2a140c)
    p.rect(2,7,4,1,0x5a1a14)
def capband(p):
    p.row(p.h-1,CAP_LO)
def ember(p):
    p.fill(0xff7a1a,0.1); p.px(0,0,0xffd060,True); p.px(1,1,0xffb030,True); p.px(1,0,0xff8a20,True); p.px(0,1,0xff6a10,True)
def torso(p):
    p.px(2,3,SKIN_LO); p.rect(1,0,3,1,SKIN_HI)
def shorts(p):
    p.row(0,ROPE); p.px(p.w//2,1,0xb8aa8a); p.px(p.w//2,2,0xb8aa8a)
def m():
    M=Model('saci',64,seed=7)
    M.part('leg',pivot=(0,18,0)).box((-1.5,0,-1.5),(3,5,3),SKIN).box((-1.5,5,-2.5),(3,1,4),SKIN_LO)
    M.part('shorts',pivot=(0,15,0)).box((-3,0,-2),(6,3,4),SHORTS,faces={'front':shorts,'back':shorts,'left':lambda p:p.row(0,ROPE),'right':lambda p:p.row(0,ROPE)})
    M.part('body',pivot=(0,10,0)).box((-2.5,0,-1.5),(5,5,3),SKIN,faces={'front':torso})
    head=M.part('head',pivot=(0,10,0)).box((-4,-8,-4),(8,8,8),SKIN,faces={'front':face})
    head.child('nose',pivot=(0,-4,-4)).box((-1,0,-1),(2,1,1),SKIN_HI)
    head.child('right_ear',pivot=(-4,-5,0)).box((-1,-1,-1),(1,2,2),SKIN_HI)
    head.child('left_ear',pivot=(4,-5,0)).box((0,-1,-1),(1,2,2),SKIN_HI)
    cap=head.child('cap',pivot=(0,-8,0)).box((-4.5,-2,-4.5),(9,2,9),CAP,faces={'front':capband,'back':capband,'left':capband,'right':capband})
    t1=cap.child('cap_tip',pivot=(0,-2,1),rot=(-0.3,0,0)).box((-3,-3,-3),(6,3,6),CAP)
    t2=t1.child('cap_tip2',pivot=(0,-3,1),rot=(-0.45,0,0)).box((-2,-3,-2),(4,3,4),CAP)
    t2.child('cap_tip3',pivot=(0,-3,0.5),rot=(-0.7,0,0)).box((-1,-3,-1),(2,3,2),CAP)
    pipe=head.child('pipe',pivot=(2,-1.5,-4),rot=(0.35,-0.55,0)).box((-0.5,-0.5,-3),(1,1,3),WOOD)
    pipe.child('bowl',pivot=(0,0,-3),rot=(-0.55,0,0)).box((-1,-3,-1.5),(2,3,2),BOWL,faces={'top':ember})
    M.part('right_arm',pivot=(-2.5,10.5,0)).box((-2,0,-1),(2,5,2),SKIN).box((-2,5,-1),(2,1,2),SKIN_HI)
    M.part('left_arm',pivot=(2.5,10.5,0)).box((0,0,-1),(2,5,2),SKIN).box((0,5,-1),(2,1,2),SKIN_HI)
    return M
if __name__=='__main__':
    M=m(); M.pack(); M.paint('/home/claude/gen/out/saci.png','/home/claude/gen/out/saci_glow.png')
    import preview; preview.render(M,'/home/claude/gen/out/saci.png','/home/claude/gen/out/saci_view.png',scale=12)
    print(M.tex_w,M.tex_h)
