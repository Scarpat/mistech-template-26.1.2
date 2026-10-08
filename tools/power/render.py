import json, math, sys
from PIL import Image, ImageDraw
A='/home/claude/w/src/main/resources/assets/mistech/'
V='/home/claude/gw/vanilla/assets/minecraft/textures/'
def tex(ref, textures):
    while ref.startswith('#'): ref=textures[ref[1:]]
    ns,p=ref.split(':'); path=(V if ns=='minecraft' else A+'textures/')+p+'.png'
    im=Image.open(path).convert('RGBA'); return im.crop((0,0,im.width,im.width))
def _rot(v,axis,deg):
    x,y,z=v; a=math.radians(deg); c,s=math.cos(a),math.sin(a)
    if axis=='x': return [x, y*c-z*s, y*s+z*c]
    if axis=='y': return [x*c+z*s, y, -x*s+z*c]
    return [x*c-y*s, x*s+y*c, z]
def rotate(p, rot):
    if not rot: return p
    o=rot['origin']; v=[p[i]-o[i] for i in range(3)]
    if 'axis' in rot: v=_rot(v,rot['axis'],rot['angle'])
    else:
        v=_rot(v,'x',rot.get('x',0)); v=_rot(v,'y',rot.get('y',0)); v=_rot(v,'z',rot.get('z',0))
    return [v[i]+o[i] for i in range(3)]
# vista: de cima, pela frente (norte = -z) e oeste (-x)
YAW=[35]
yaw=math.radians(35); pitch=math.radians(-30)
def proj(p):
    x,y,z=p[0]-8,p[1]-8,p[2]-8
    # olhando do noroeste: girar para que -z,-x fiquem de frente
    yaw=math.radians(YAW[0])
    X= x*math.cos(yaw) - z*math.sin(yaw)
    Z= x*math.sin(yaw) + z*math.cos(yaw)
    Y= y*math.cos(pitch) - Z*math.sin(pitch)
    D= y*math.sin(pitch) + Z*math.cos(pitch)
    return (X, -Y, D)
def faces(e):
    (x0,y0,z0),(x1,y1,z1)=e['from'],e['to']
    F={'north':([x1,y1,z0],[x0,y1,z0],[x0,y0,z0],[x1,y0,z0], (16-x1,16-y1,16-x0,16-y0)),
       'south':([x0,y1,z1],[x1,y1,z1],[x1,y0,z1],[x0,y0,z1], (x0,16-y1,x1,16-y0)),
       'west': ([x0,y1,z0],[x0,y1,z1],[x0,y0,z1],[x0,y0,z0], (z0,16-y1,z1,16-y0)),
       'east': ([x1,y1,z1],[x1,y1,z0],[x1,y0,z0],[x1,y0,z1], (16-z1,16-y1,16-z0,16-y0)),
       'up':   ([x0,y1,z0],[x1,y1,z0],[x1,y1,z1],[x0,y1,z1], (x0,z0,x1,z1)),
       'down': ([x0,y0,z1],[x1,y0,z1],[x1,y0,z0],[x0,y0,z0], (x0,16-z1,x1,16-z0))}
    return F
def render(name, out, S=14):
    m=json.load(open(A+'models/block/%s.json'%name)); T=m['textures']
    polys=[]
    shade={'up':1.0,'north':0.8,'south':0.8,'west':0.6,'east':0.6,'down':0.5}
    for e in m['elements']:
        for f,(a,b,c,d,uv) in faces(e).items():
            if f not in e['faces']: continue
            fd=e['faces'][f]; uv=fd.get('uv',uv); im=tex(fd['texture'],T); k=im.width/16
            N=8
            for i in range(N):
                for j in range(N):
                    def P(u,v):
                        top=[a[t]+(b[t]-a[t])*u for t in range(3)]; bot=[d[t]+(c[t]-d[t])*u for t in range(3)]
                        return rotate([top[t]+(bot[t]-top[t])*v for t in range(3)], e.get('rotation'))
                    q=[P(i/N,j/N),P((i+1)/N,j/N),P((i+1)/N,(j+1)/N),P(i/N,(j+1)/N)]
                    pu=uv[0]+(uv[2]-uv[0])*(i+0.5)/N; pv=uv[1]+(uv[3]-uv[1])*(j+0.5)/N
                    col=im.getpixel((min(im.width-1,int(pu*k)),min(im.width-1,int(pv*k))))
                    if col[3]<20: continue
                    pts=[proj(p) for p in q]
                    ar=sum(pts[k][0]*pts[(k+1)%4][1]-pts[(k+1)%4][0]*pts[k][1] for k in range(4))
                    if ar>=0: continue
                    # normal pra ver se está virada pra câmera
                    polys.append((sum(p[2] for p in pts)/4,[(p[0],p[1]) for p in pts],tuple(int(cc*shade[f]) for cc in col[:3])))
    W=Image.new('RGB',(40*S,44*S),(60,64,72)); dr=ImageDraw.Draw(W)
    for dpt,pts,col in sorted(polys,key=lambda t:-t[0]):
        dr.polygon([(20*S+x*S,26*S+y*S) for x,y in pts],fill=col)
    W.save(out)
names=sys.argv[1:]
ims=[]
for n in names:
  for yv in (35,215):
    YAW[0]=yv
    render(n,'/tmp/claude-0/r_%s_%d.png'%(n,yv),9); ims.append(Image.open('/tmp/claude-0/r_%s_%d.png'%(n,yv)))
W=Image.new('RGB',(sum(i.width for i in ims),ims[0].height)); x=0
for i in ims: W.paste(i,(x,0)); x+=i.width
W.save('/tmp/claude-0/models.png')
