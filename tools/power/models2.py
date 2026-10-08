"""Modelos detalhados dos geradores avançados (v2). Frente = norte (z = 0)."""
import json, os
A='/home/claude/w/src/main/resources/assets/mistech/'
FACES=['north','south','east','west','up','down']
def auto_uv(f, fr, to):
    x1,y1,z1=fr; x2,y2,z2=to
    u={'north':[16-x2,16-y2,16-x1,16-y1],'south':[x1,16-y2,x2,16-y1],'west':[z1,16-y2,z2,16-y1],
       'east':[16-z2,16-y2,16-z1,16-y1],'up':[x1,z1,x2,z2],'down':[x1,16-z2,x2,16-z1]}[f]
    # mantém dentro de 0..16 (fora disso o jogo pega pedaços de outras texturas do atlas)
    for a,b in ((0,2),(1,3)):
        size=min(16,u[b]-u[a])
        if u[a]<0: u[a],u[b]=0,size
        if u[b]>16: u[a],u[b]=16-size,16
        u[a]=max(0,u[a]); u[b]=min(16,u[b])
    return [round(v,3) for v in u]
def box(fr, to, tex, faces=None, rot=None, light=0, uv=None, shade=True):
    fs={}
    for f in (faces or FACES):
        t = tex.get(f, tex.get('*')) if isinstance(tex, dict) else tex
        if t:
            fs[f]={'texture':'#'+t}
            if uv and f in uv: fs[f]['uv']=uv[f]
            elif uv and '*' in uv: fs[f]['uv']=uv['*']
            elif isinstance(uv,list): fs[f]['uv']=uv
            else: fs[f]['uv']=auto_uv(f,fr,to)
    e={'from':[round(v,3) for v in fr],'to':[round(v,3) for v in to],'faces':fs}
    if rot: e['rotation']=rot
    if light: e['light_emission']=light
    if not shade: e['shade']=False
    return e
def ring(x0,z0,x1,z1,y0,y1,t,tex,**kw):
    """moldura retangular (4 barras) por fora de x0..x1 / z0..z1"""
    return [box([x0,y0,z0],[x1,y1,z0+t],tex,**kw), box([x0,y0,z1-t],[x1,y1,z1],tex,**kw),
            box([x0,y0,z0+t],[x0+t,y1,z1-t],tex,faces=['east','west','up','down'],**kw),
            box([x1-t,y0,z0+t],[x1,y1,z1-t],tex,faces=['east','west','up','down'],**kw)]
def cross(cx,cy,cz,w,h,tex,light=0,uv=None):
    """duas folhas cruzadas (flores, cristais)"""
    out=[]
    for ang in (45,-45):
        out.append(box([cx-w/2,cy,cz],[cx+w/2,cy+h,cz],tex,faces=['north','south'],light=light,uv=uv,shade=False,
                       rot={'angle':ang,'axis':'y','origin':[cx,cy,cz]}))
    return out
def model(name, textures, elements):
    disp={'gui':{'rotation':[30,225,0],'translation':[0,-1.25,0],'scale':[0.52,0.52,0.52]},
          'fixed':{'rotation':[0,0,0],'translation':[0,-1,0],'scale':[0.45,0.45,0.45]}}
    json.dump({'parent':'minecraft:block/block','textures':textures,'display':disp,'elements':elements},
              open(A+'models/block/%s.json'%name,'w'), indent=1)
def S(on,off,flag): return on if flag else off

# ==================== Reator Impuro ====================
def reactor(on):
    t={'tiles':'minecraft:block/deepslate_tiles','deep':'minecraft:block/polished_deepslate',
       'casing':'mistech:block/impure_reactor_casing'+S('_on','',on),'panel':'mistech:block/impure_reactor_panel'+S('_on','',on),
       'front':'mistech:block/impure_reactor_front'+S('_on','',on),
       'pillar':'mistech:block/adamantine_block','mith':'mistech:block/mithril_block','glass':'minecraft:block/purple_stained_glass',
       'core':S('mistech:block/impure_magic_block','minecraft:block/crying_obsidian',on),'amy':'minecraft:block/amethyst_block',
       'crying':'minecraft:block/crying_obsidian','cluster':'minecraft:block/amethyst_cluster','grate':'minecraft:block/oxidized_copper_grate',
       'particle':'minecraft:block/polished_deepslate'}
    L=12 if on else 0
    e=[box([0,0,0],[16,2,16],'tiles'),
       box([1,2,1],[15,3,15],{'*':'deep'}),
       # pilares com base, capitel e runa acesa
    ]
    for x,z in ((0.5,0.5),(12.5,0.5),(0.5,12.5),(12.5,12.5)):
        e.append(box([x,3,z],[x+3,14,z+3],'pillar'))
        e.append(box([x-0.4,3,z-0.4],[x+3.4,4.2,z+3.4],'mith'))
        e.append(box([x-0.4,12.8,z-0.4],[x+3.4,14,z+3.4],'mith'))
        e.append(box([x-0.4,8,z-0.4],[x+3.4,8.6,z+3.4],'mith'))
        # filete de magia nas faces de fora
        fx = 'west' if x<8 else 'east'; fz='north' if z<8 else 'south'
        ox = x-0.05 if x<8 else x+3.05
        e.append(box([min(ox,ox),5,z+1.2],[ox,11,z+1.8] if x>8 else [x,11,z+1.8],'core',faces=[fx],light=L) if False else
                 box([x-0.05 if x<8 else x+2.95, 5, z+1.2],[x+0.05 if x<8 else x+3.05, 7.5, z+1.8],'core',faces=[fx],light=L))
        e.append(box([x-0.05 if x<8 else x+2.95, 9.2, z+1.2],[x+0.05 if x<8 else x+3.05, 12, z+1.8],'core',faces=[fx],light=L))
        e.append(box([x+1.2,5,z-0.05 if z<8 else z+2.95],[x+1.8,7.5,z+0.05 if z<8 else z+3.05],'core',faces=[fz],light=L))
        e.append(box([x+1.2,9.2,z-0.05 if z<8 else z+2.95],[x+1.8,12,z+0.05 if z<8 else z+3.05],'core',faces=[fz],light=L))
    # câmara de contenção
    e.append(box([4.5,4,4.5],[11.5,12.5,11.5],'glass'))
    e.append(box([6,6.2,6],[10,10.2,10],'core',rot={'x':45,'y':45,'z':0,'origin':[8,8.2,8]},light=S(15,0,on)))
    e.append(box([7,7.2,7],[9,9.2,9],'amy',rot={'x':0,'y':45,'z':45,'origin':[8,8.2,8]},light=S(10,0,on)))
    e+=ring(3.8,3.8,12.2,12.2,5,5.8,0.9,'mith')
    e+=ring(3.8,3.8,12.2,12.2,10.6,11.4,0.9,'mith')
    for x,z in ((4,4),(11.2,4),(4,11.2),(11.2,11.2)):
        e.append(box([x,4,z],[x+0.8,12.5,z+0.8],'mith'))
    # condutos dos pilares até a câmara
    for (a,b) in (((3.5,8.6,2.1),(4.6,9.4,2.9)),((11.4,8.6,2.1),(12.5,9.4,2.9)),((3.5,8.6,13.1),(4.6,9.4,13.9)),((11.4,8.6,13.1),(12.5,9.4,13.9))):
        e.append(box(list(a),list(b),'crying',light=S(6,0,on)))
    for (a,b) in (((2.1,8.6,3.5),(2.9,9.4,4.6)),((13.1,8.6,3.5),(13.9,9.4,4.6)),((2.1,8.6,11.4),(2.9,9.4,12.5)),((13.1,8.6,11.4),(13.9,9.4,12.5))):
        e.append(box(list(a),list(b),'crying',light=S(6,0,on)))
    # console da frente (inclinado)
    e.append(box([3.5,3,1.2],[12.5,5,4],{'*':'casing'}))
    e.append(box([3.5,4.4,1.6],[12.5,8.4,2.6],{'north':'panel','*':'deep'},rot={'angle':-27,'axis':'x','origin':[8,4.4,2.6]},
                 uv={'north':[0,0,16,16]}))
    # janelinha do reator (frente da base) e grade de trás
    e.append(box([5,0.5,-0.1],[11,1.9,0.1],'front',faces=['north'],uv={'north':[3,6,13,10]},light=S(8,0,on)))
    e.append(box([4,4,13],[12,10,13.6],{'south':'grate','*':'deep'}))
    # capacitores laterais
    for x0,x1 in ((1.3,3.3),(12.7,14.7)):
        e.append(box([x0,4.5,5],[x1,10.5,11],{'*':'casing'},uv={'*':[2,2,14,14]}))
        e.append(box([x0-0.2,6.5,4.8],[x1+0.2,7,11.2],'mith'))
        e.append(box([x0-0.2,8,4.8],[x1+0.2,8.5,11.2],'mith'))
    # tampa e emissor
    e.append(box([0.6,14,0.6],[15.4,15.2,15.4],{'*':'deep','up':'tiles'}))
    e.append(box([3,15.2,3],[13,16.2,13],{'*':'casing'},uv={'*':[3,3,13,13]}))
    e+=ring(2.6,2.6,13.4,13.4,15.2,15.7,0.6,'mith')
    e.append(box([5.5,16.2,5.5],[10.5,17.6,10.5],'crying'))
    e.append(box([6.5,17.6,6.5],[9.5,18.2,9.5],'mith'))
    e+=cross(8,18.2,8,6,6,'cluster',light=S(12,2,on))
    return t,e

# ==================== Caldeira Ígnea ====================
def boiler(on):
    t={'bricks':'minecraft:block/polished_blackstone_bricks','stone':'minecraft:block/polished_blackstone','shell':'mistech:block/igneous_boiler_shell',
       'band':'mistech:block/igneous_boiler_band','door':'mistech:block/igneous_boiler_front'+S('_on','',on),'gauge':'mistech:block/igneous_boiler_gauge',
       'valve':'mistech:block/igneous_boiler_valve','copper':'minecraft:block/cut_copper','fire':S('minecraft:block/magma','minecraft:block/blackstone_top',on),
       'chim':'minecraft:block/nether_bricks','soot':'minecraft:block/coal_block','particle':'minecraft:block/polished_blackstone'}
    e=[box([0,0,0],[16,1.5,16],'bricks'),
       box([0.5,1.5,0.5],[15.5,2.5,15.5],'stone')]
    # berços
    for z in (2.5,10.5):
        e.append(box([1.5,2.5,z],[14.5,4.5,z+3],'stone'))
        e.append(box([1,2.5,z+0.5],[2,5.5,z+2.5],'band'))
        e.append(box([14,2.5,z+0.5],[15,5.5,z+2.5],'band'))
    # tambor (perfil arredondado de 3 camadas)
    cx,cy=8,9.8
    prof=[(6.4,2.4),(6.0,3.7),(5.2,4.9),(4.0,5.8),(2.5,6.3)]
    layers=[([cx-w,cy-h,1.6+i*0.05],[cx+w,cy+h,14.0-i*0.05]) for i,(w,h) in enumerate(prof)]
    for a,b in layers: e.append(box(a,b,{'*':'shell'}))
    # cintas
    for z in (4.2,10.6):
        for a,b in layers[::2]:
            e.append(box([a[0]-0.25,a[1]-0.25,z],[b[0]+0.25,b[1]+0.25,z+1.1],'band',faces=['north','south','east','west','up','down']))
    # fornalha: porta redonda, moldura, dobradiça, puxador
    e.append(box([3.5,5,1.0],[12.5,14,1.6],{'north':'door','*':'band'},uv={'north':[0,0,16,16]},light=S(10,0,on)))
    e+= [box([3,4.5,0.8],[13,5,1.7],'band'), box([3,14,0.8],[13,14.5,1.7],'band'),
         box([3,5,0.8],[3.5,14,1.7],'band'), box([12.5,5,0.8],[13,14,1.7],'band')]
    e.append(box([12.6,6.5,0.4],[13.6,8,1.6],'stone')); e.append(box([12.6,11,0.4],[13.6,12.5,1.6],'stone'))
    e.append(box([4,9,0.2],[5,10,1.0],'copper')); e.append(box([3.6,8.6,-0.2],[5.4,10.4,0.2],'copper'))
    # grelha de brasas embaixo da porta
    e.append(box([4.5,3.6,0.9],[11.5,4.4,1.7],{'north':'fire','up':'fire','*':'stone'},light=S(12,0,on)))
    # chaminé
    e.append(box([9.5,16,9],[13.5,17,13],'band'))
    e.append(box([10,17,9.5],[13,22,12.5],{'*':'stone','up':'soot'}))
    e.append(box([9.6,19,9.1],[13.4,19.6,12.9],'band'))
    e.append(box([9.3,22,8.8],[13.7,23,13.2],{'*':'chim','up':'soot'}))
    # válvula de segurança e apito
    e.append(box([3.5,16,4.5],[5.5,16.8,6.5],'band'))
    e.append(box([4,16.8,5],[5,19,6],'copper'))
    e.append(box([3.7,19,4.7],[5.3,19.6,6.3],'band'))
    # manômetro (lado leste)
    e.append(box([14.6,10.5,6],[15.4,11.3,7],'copper'))
    e.append(box([15.4,8.5,4.3],[16,12.5,8.3],{'east':'gauge','*':'band'},uv={'east':[0,0,16,16]}))
    # volante (lado oeste)
    e.append(box([0.8,9,8],[1.4,10,9],'copper'))
    e.append(box([0.2,6.5,6],[0.8,12.5,12],{'west':'valve','east':'valve'},faces=['west','east'],uv={'*':[0,0,16,16]}))
    # cano de cobre descendo para a base (leste, atrás)
    e.append(box([14.2,6,10.5],[15.6,7.4,11.9],'copper'))
    e.append(box([15.6,2.5,10.5],[16,7.4,11.9],'copper'))
    e.append(box([14.6,1.5,10.3],[16,2.5,12.1],'band'))
    return t,e

# ==================== Observatório Estelar ====================
def observ(on):
    t={'deep':'minecraft:block/polished_deepslate','side':'mistech:block/stellar_observatory_side','front':'mistech:block/stellar_observatory_front'+S('_on','',on),
       'gold':'mistech:block/orichalcum_block','tube':'mistech:block/stellar_observatory_tube','chart':'mistech:block/star_chart',
       'lens':'minecraft:block/light_blue_stained_glass','orb':S('minecraft:block/sea_lantern','minecraft:block/amethyst_block',on),
       'cluster':'minecraft:block/amethyst_cluster','dark':'minecraft:block/blackstone','particle':'minecraft:block/lapis_block'}
    e=[box([0,0,0],[16,1.5,16],'deep'),
       box([1,1.5,2],[15,8,14],{'*':'side'}),
       box([2,1.5,1],[14,7.95,15],{'north':'front','south':'side','*':'side','up':'side'},uv={'north':[0,0,16,16]}),
    ]
    # cantos chanfrados em ouro
    for x,z in ((1.2,1.2),(13.2,1.2),(1.2,13.2),(13.2,13.2)):
        e.append(box([x,1.5,z],[x+1.6,8.6,z+1.6],'gold'))
    e+=ring(0.8,0.8,15.2,15.2,8,8.8,1.0,'gold')
    e.append(box([1.8,8,1.8],[14.2,8.5,14.2],{'up':'chart','*':'dark'},uv={'up':[0,0,16,16]}))
    # mesa giratória e garfo
    e.append(box([4.5,8.5,4.5],[11.5,9.3,11.5],'tube'))
    e.append(box([5.5,9.3,5.5],[10.5,10,10.5],'dark'))
    for x in (5.2,10):
        e.append(box([x,10,7.4],[x+0.8,14.6,8.6],'gold'))
    e.append(box([4.8,13.4,7.5],[11.2,14.4,8.5],'tube'))
    # luneta (gira em volta do eixo)
    R={'angle':35,'axis':'x','origin':[8,13.9,8]}
    e.append(box([6.7,9.5,6.7],[9.3,19,9.3],'tube',rot=R))
    e.append(box([6.3,19,6.3],[9.7,23.5,9.7],'tube',rot=R))
    e.append(box([6.1,22.2,6.1],[9.9,23.6,9.9],'gold',rot=R))
    e.append(box([6.5,23.6,6.5],[9.5,23.8,9.5],'lens',rot=R,light=S(12,0,on)))
    e.append(box([7.3,7.2,7.3],[8.7,9.5,8.7],'dark',rot=R))
    e.append(box([7.1,6.6,7.1],[8.9,7.2,8.9],'gold',rot=R))
    e.append(box([9.3,14,8.4],[10.3,19,9.4],'tube',rot=R))
    e.append(box([9.4,19,8.5],[10.2,19.3,9.3],'lens',rot=R))
    # orbe estelar com anel armilar (frente-esquerda)
    e.append(box([2.2,8.8,2.2],[4.8,9.6,4.8],'gold'))
    e.append(box([3,9.6,3],[4,10.6,4],'dark'))
    e.append(box([2.6,10.6,2.6],[4.4,12.4,4.4],'orb',rot={'angle':45,'axis':'y','origin':[3.5,11.5,3.5]},light=S(15,0,on)))
    for ang in (0,60):
        e+= [box([1.8,11.4,3.4],[5.2,11.6,3.6],'gold',rot={'x':0,'y':ang,'z':30,'origin':[3.5,11.5,3.5]})]
    e.append(box([1.8,11.4,3.4],[5.2,11.6,3.6],'gold',rot={'x':0,'y':-60,'z':-30,'origin':[3.5,11.5,3.5]}))
    # cristal (fundo-direita)
    e.append(box([11.4,8.8,11.4],[14,9.6,14],'gold'))
    e+=cross(12.7,9.6,12.7,4,5,'cluster',light=S(10,2,on))
    return t,e

# ==================== Gerador de Experiência ====================
def xp(on):
    t={'tiles':'minecraft:block/deepslate_tiles','sculk':'minecraft:block/sculk','casing':'mistech:block/experience_generator_casing'+S('_on','',on),
       'front':'mistech:block/experience_generator_front'+S('_on','',on),'emerald':'minecraft:block/emerald_block','gold':'minecraft:block/gold_block',
       'tank':'mistech:block/experience_generator_tank'+S('_on','',on),'core':S('minecraft:block/verdant_froglight_side','minecraft:block/green_stained_glass',on),
       'tendril':'minecraft:block/sculk_sensor_tendril_'+S('active','inactive',on),'cat':'minecraft:block/sculk_catalyst_top'+S('_bloom','',on),
       'lapis':'minecraft:block/lapis_block','particle':'minecraft:block/deepslate_tiles'}
    L=S(10,0,on)
    e=[box([0,0,0],[16,1.5,16],'tiles'),
       box([0.4,1.5,0.4],[15.6,2.6,15.6],'sculk'),
       box([1.5,2.6,1.5],[14.5,10,14.5],{'north':'front','*':'casing','up':'cat'},uv={'north':[0,0,16,16]})]
    for x,z in ((0.8,0.8),(12.8,0.8),(0.8,12.8),(12.8,12.8)):
        e.append(box([x,1.5,z],[x+2.4,11.5,z+2.4],'emerald'))
        e.append(box([x-0.3,11.5,z-0.3],[x+2.7,12.3,z+2.7],'gold'))
        e.append(box([x+0.7,12.3,z+0.7],[x+1.7,13.3,z+1.7],'emerald',light=L))
    # faixa de lápis
    e+=ring(1.3,1.3,14.7,14.7,7.2,7.8,0.4,'lapis')
    e.append(box([2,10,2],[14,10.8,14],{'*':'tiles'}))
    # tanque central (cilindro) com núcleo
    e.append(box([5,10.8,4.2],[11,18,11.8],'tank',faces=['north','south','east','west'],uv={'*':[0,0,16,16]}))
    e.append(box([4.2,10.85,5],[11.8,17.95,11],'tank',faces=['north','south','east','west'],uv={'*':[0,0,16,16]}))
    e.append(box([6.6,12,6.6],[9.4,14.8,9.4],'core',rot={'x':45,'y':45,'z':0,'origin':[8,13.4,8]},light=S(14,0,on)))
    e.append(box([4.5,10.8,4.5],[11.5,11.6,11.5],'gold'))
    e.append(box([4,17.8,4],[12,18.8,12],'gold'))
    # funil coletor em cima
    e+=ring(2.8,2.8,13.2,13.2,18.8,19.8,1.0,'emerald')
    e.append(box([4,18.8,4],[12,19.2,12],{'up':'sculk','*':'emerald'}))
    e.append(box([7,19.2,7],[9,20.4,9],'gold'))
    # tentáculos de sculk nos cantos de trás
    e+=cross(2,12.3,14,3,6,'tendril',light=S(8,0,on),uv=[0,8,16,16])
    e+=cross(14,12.3,14,3,6,'tendril',light=S(8,0,on),uv=[0,8,16,16])
    # tubos laterais com XP
    for x0,x1 in ((0.2,1.6),(14.4,15.8)):
        e.append(box([x0,3.5,6.5],[x1,9.5,9.5],'tank',faces=['north','south','east','west'],uv={'*':[4,0,12,16]}))
        e.append(box([x0-0.1,3,6.2],[x1+0.1,3.6,9.8],'gold'))
        e.append(box([x0-0.1,9.4,6.2],[x1+0.1,10,9.8],'gold'))
    return t,e

for fn,name in ((reactor,'impure_reactor'),(boiler,'igneous_boiler'),(observ,'stellar_observatory'),(xp,'experience_generator')):
    for on in (False,True):
        t,e=fn(on)
        model(name+('_on' if on else ''), t, e)
        print(name, on, len(e))
V='/home/claude/gw/vanilla/assets/minecraft/textures/'
for fn in os.listdir(A+'models/block'):
    if any(fn.startswith(n) for n in ('impure_reactor','stellar_observatory','igneous_boiler','experience_generator')):
        d=json.load(open(A+'models/block/'+fn))
        for v in d['textures'].values():
            ns,p=v.split(':'); path=(V if ns=='minecraft' else A+'textures/')+p+'.png'
            if not os.path.exists(path): print('FALTA',fn,v)
        for el in d['elements']:
            for c in el['from']+el['to']:
                if not -16<=c<=32: print('FORA',fn,el)
