import sys,os
from PIL import Image, ImageDraw
V=os.path.expanduser('~/van/assets/minecraft/textures/block/')
names=sys.argv[2:]; S=6; cols=8
W=16*S+8; H=16*S+20
im=Image.new('RGBA',(cols*W,((len(names)+cols-1)//cols)*H),(60,62,68,255)); d=ImageDraw.Draw(im)
for i,n in enumerate(names):
    t=Image.open(V+n+'.png').convert('RGBA'); t=t.crop((0,0,16,16)).resize((16*S,16*S),Image.NEAREST)
    x=(i%cols)*W+4; y=(i//cols)*H+2; im.alpha_composite(t,(x,y)); d.text((x,y+16*S+2),n[:17],fill=(230,230,230))
im.save(sys.argv[1])
