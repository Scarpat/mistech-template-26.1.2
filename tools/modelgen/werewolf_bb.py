import sys; sys.path.insert(0,'/home/claude/gen')
import bbparse
from modelgen import hexc
ORANGE=0xc4602a; ORANGE_D=0x9a481c; BLACK=0x1c1612; WHITE=0xe6dccb; CREAM=0xd8c8b0
def eyes(p):
    p.row(0,0x6a3014)
    p.px(0,1,0xffc838,True); p.px(p.w-1,1,0xffc838,True)
    p.px(0,2,0xff4a10,True); p.px(p.w-1,2,0xff4a10,True)
def snout_tip(p):
    p.fill(0x0c0a08,0.02)
def throat(p):
    p.fill(WHITE,0.04)
def mane(p):
    p.fill(BLACK,0.06)
COL={
 # pernas: coxa laranja, canela e pé pretos (as "meias" do lobo-guará)
 ('Perna',0):BLACK,('cube_r1',0):ORANGE,('cube_r2',0):BLACK,('cube_r3',0):BLACK,
 ('Perna2',0):BLACK,('cube_r4',0):ORANGE,('cube_r5',0):BLACK,('cube_r6',0):BLACK,
 # braços: alto laranja, antebraço e mão pretos
 ('cube_r7',0):ORANGE,('cube_r8',0):BLACK,('cube_r9',0):BLACK,('cube_r10',0):BLACK,
 ('cube_r11',0):ORANGE,('cube_r12',0):BLACK,('cube_r13',0):BLACK,('cube_r14',0):BLACK,
 # tronco
 ('cube_r15',0):ORANGE,('cube_r16',0):ORANGE,('cube_r17',0):ORANGE_D,
 # cabeça: focinho preto, cabeça laranja, pescoço/garganta claro
 ('cube_r18',0):BLACK,('cube_r18',1):ORANGE,('cube_r19',0):ORANGE,
 # orelhas: fora laranja, dentro creme
 ('cube_r20',0):ORANGE,('cube_r21',0):CREAM,('cube_r22',0):ORANGE,('cube_r23',0):CREAM,
 # cauda com ponta branca
 ('cube_r24',0):ORANGE,('cube_r25',0):ORANGE,('cube_r26',0):ORANGE,('cube_r27',0):WHITE,
}
FACES={
 ('cube_r18',1):{'top':eyes},
 ('cube_r18',0):{'top':snout_tip},
 ('cube_r15',0):{'back':mane,'top':mane},   # crina preta no dorso
 ('cube_r16',0):{'front':throat},
 ('cube_r19',0):{'front':throat},
}
def m():
    M=bbparse.parse('/home/claude/gen/bb/Lobisomem.java',64,64,'werewolf')
    for p in M.parts:
        for i,b in enumerate(p.boxes):
            c=COL.get((p.name,i),ORANGE)
            b.color=hexc(c); b.pattern='fur' if c in (ORANGE,ORANGE_D,BLACK) else None
            b.faces=FACES.get((p.name,i),{})
    return M
if __name__=='__main__':
    import preview
    M=m(); M.paint('/home/claude/gen/out/werewolf_bb.png','/home/claude/gen/out/werewolf_bb_eyes.png')
    preview.render(M,'/home/claude/gen/out/werewolf_bb.png','/home/claude/gen/out/ww_bb_tex.png',yaws=(-30,90,150),scale=12,pitch=10)
