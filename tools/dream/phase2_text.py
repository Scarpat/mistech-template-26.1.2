import json
B='src/main/resources/assets/mistech/'
def find(o):
    if isinstance(o,dict):
        if o.get('id')=='gnome_realm' and 'pages' in o: return o
        for v in o.values():
            r=find(v)
            if r: return r
    if isinstance(o,list):
        for v in o:
            r=find(v)
            if r: return r
p=B+'guide/book.json'; b=json.load(open(p,encoding='utf-8'))
e=find(b)
new=[{"type":"spotlight","item":"mistech:counting_sheep_spawn_egg","text":"guide.mistech.gnome_realm.6"},
     {"type":"text","text":"guide.mistech.gnome_realm.7"},
     {"type":"spotlight","item":"mistech:bogeyman_spawn_egg","text":"guide.mistech.gnome_realm.8"},
     {"type":"text","text":"guide.mistech.gnome_realm.9"},
     {"type":"text","text":"guide.mistech.gnome_realm.10"},
     {"type":"spotlight","item":"mistech:gnome_miner_spawn_egg","text":"guide.mistech.gnome_realm.11"}]
e['pages']=[x for x in e['pages'] if x.get('text') not in {n['text'] for n in new}]+new
open(p,'w',encoding='utf-8').write(json.dumps(b,indent=2,ensure_ascii=False)+'\n')

PT={
"entity.mistech.counting_sheep":"Ovelha-de-Contar","entity.mistech.gnome_merchant":"Gnomo Mercador","entity.mistech.corrupted_gnome":"Gnomo Corrompido",
"entity.mistech.bogeyman":"Bicho-Papão","entity.mistech.shade":"Sombra",
"item.mistech.counting_sheep_spawn_egg":"Ovo de Ovelha-de-Contar","item.mistech.gnome_merchant_spawn_egg":"Ovo de Gnomo Mercador",
"item.mistech.corrupted_gnome_spawn_egg":"Ovo de Gnomo Corrompido","item.mistech.bogeyman_spawn_egg":"Ovo de Bicho-Papão","item.mistech.shade_spawn_egg":"Ovo de Sombra",
"block.mistech.dream_anchor":"Âncora Onírica",
"entity.mistech.gnome_miner":"Gnomo Minerador","item.mistech.gnome_miner_spawn_egg":"Ovo de Gnomo Minerador",
"message.mistech.gnome_miner.found":"O minerador fareja o ar: \"%s! Uns %s blocos %s.\"",
"message.mistech.gnome_miner.none":"O minerador coça a barba: \"Nada que preste por aqui.\"",
"message.mistech.gnome_miner.tired":"O minerador ainda está recuperando o faro.",
"message.mistech.gnome_miner.dir.up":"para cima","message.mistech.gnome_miner.dir.down":"para baixo",
"message.mistech.gnome_miner.dir.north":"ao norte","message.mistech.gnome_miner.dir.south":"ao sul",
"message.mistech.gnome_miner.dir.east":"a leste","message.mistech.gnome_miner.dir.west":"a oeste",
"guide.mistech.gnome_realm.11":"Os §6Gnomos Mineradores§r trabalham na mina do Rei e nas Profundezas, de capacete com lanterna e picareta. Clique com a §emão vazia§r e ele fareja o §bminério mais próximo§r (20 blocos): diz qual é, a distância e a direção. Precisa de 30 s para recuperar o faro.",
"message.mistech.counting_sheep.count":"Ovelha número %s...",
"message.mistech.counting_sheep.rested":"...três ovelhas. Você se sente descansado.",
"message.mistech.wrong_house.loop":"Esse quarto... você já passou por aqui?",
"message.mistech.wrong_house.loop.last":"A casa cansa da brincadeira e te deixa passar.",
"message.mistech.wrong_house.bogeyman":"Tem alguma coisa embaixo da cama.",
"message.mistech.endless_stair.loop":"Os mesmos degraus. A mesma janela. De novo.",
"message.mistech.endless_stair.loop.last":"Dessa vez a escada parece ter fim.",
"guide.mistech.gnome_realm.6":"No §2Sonho Comum§r pastam as §fOvelhas-de-Contar§r. Tosquie para ganhar §fLã de Nuvem§r. Com a mão vazia você §econta§r uma ovelha — conte §e3 ovelhas diferentes§r em 30 segundos e você fica §adescansado§r (Regeneração e Saciedade).",
"guide.mistech.gnome_realm.7":"Os §6Vilarejos Gnomos§r ficam espalhados pelo Sonho Comum: casinhas de cogumelo em volta de uma fogueira, com baús, um cercado de ovelhas e um §3Gnomo Mercador§r (chapéu azul) que troca coisas do sonho por esmeraldas.\n\nNo Pesadelo existem §8Vilarejos Arruinados§r, tomados por §cGnomos Corrompidos§r.",
"guide.mistech.gnome_realm.8":"O §8Bicho-Papão§r vive no escuro do Pesadelo. Na sombra ele te cega, te puxa e some atrás de você. §eLuz forte§r o deixa lento, fraco e queimando — leve tochas!",
"guide.mistech.gnome_realm.9":"A §8Sombra§r só anda quando ninguém está olhando. §eOlhe para ela§r e ela congela; vire as costas e ela chega. Perto dela tudo fica lento.\n\nOs §cGnomos Corrompidos§r caçam jogadores e gnomos, e o golpe deles enfraquece.",
"guide.mistech.gnome_realm.10":"Lugares estranhos do Pesadelo:\n• §8Casa Errada§r (Bairro Errado): três quartos iguais... ou o mesmo quarto três vezes? No fundo, um quarto com camas — não chegue perto delas.\n• §8Escadaria Sem Fim§r: uma torre cuja escada repete as mesmas voltas. Persista e chegue ao baú do topo.",
}
EN={
"entity.mistech.counting_sheep":"Counting Sheep","entity.mistech.gnome_merchant":"Gnome Merchant","entity.mistech.corrupted_gnome":"Corrupted Gnome",
"entity.mistech.bogeyman":"Bogeyman","entity.mistech.shade":"Shade",
"item.mistech.counting_sheep_spawn_egg":"Counting Sheep Spawn Egg","item.mistech.gnome_merchant_spawn_egg":"Gnome Merchant Spawn Egg",
"item.mistech.corrupted_gnome_spawn_egg":"Corrupted Gnome Spawn Egg","item.mistech.bogeyman_spawn_egg":"Bogeyman Spawn Egg","item.mistech.shade_spawn_egg":"Shade Spawn Egg",
"block.mistech.dream_anchor":"Dream Anchor",
"entity.mistech.gnome_miner":"Gnome Miner","item.mistech.gnome_miner_spawn_egg":"Gnome Miner Spawn Egg",
"message.mistech.gnome_miner.found":"The miner sniffs the air: \"%s! About %s blocks %s.\"",
"message.mistech.gnome_miner.none":"The miner scratches his beard: \"Nothing worth digging around here.\"",
"message.mistech.gnome_miner.tired":"The miner is still getting his nose back.",
"message.mistech.gnome_miner.dir.up":"up","message.mistech.gnome_miner.dir.down":"down",
"message.mistech.gnome_miner.dir.north":"to the north","message.mistech.gnome_miner.dir.south":"to the south",
"message.mistech.gnome_miner.dir.east":"to the east","message.mistech.gnome_miner.dir.west":"to the west",
"guide.mistech.gnome_realm.11":"§6Gnome Miners§r work in the King's mine and in the Depths, with a lamp helmet and a pickaxe. Click one with an §eempty hand§r and he sniffs out the §bnearest ore§r (20 blocks): what it is, how far and which way. He needs 30 s to get his nose back.",
"message.mistech.counting_sheep.count":"Sheep number %s...",
"message.mistech.counting_sheep.rested":"...three sheep. You feel rested.",
"message.mistech.wrong_house.loop":"This room... haven't you been here before?",
"message.mistech.wrong_house.loop.last":"The house tires of the game and lets you through.",
"message.mistech.wrong_house.bogeyman":"There's something under the bed.",
"message.mistech.endless_stair.loop":"The same steps. The same window. Again.",
"message.mistech.endless_stair.loop.last":"This time the stairs seem to end.",
"guide.mistech.gnome_realm.6":"§fCounting Sheep§r graze in the §2Common Dream§r. Shear them for §fCloud Wool§r. With an empty hand you §ecount§r a sheep — count §e3 different sheep§r within 30 seconds and you feel §arested§r (Regeneration and Saturation).",
"guide.mistech.gnome_realm.7":"§6Gnome Hamlets§r are scattered across the Common Dream: mushroom cottages around a campfire, with chests, a sheep pen and a §3Gnome Merchant§r (blue hat) who trades dream goods for emeralds.\n\nIn the Nightmare there are §8Ruined Hamlets§r, overrun by §cCorrupted Gnomes§r.",
"guide.mistech.gnome_realm.8":"The §8Bogeyman§r lives in the Nightmare's dark. In the shadows it blinds you, drags you and vanishes behind you. §eBright light§r makes it slow, weak and burning — bring torches!",
"guide.mistech.gnome_realm.9":"The §8Shade§r only moves when nobody is looking. §eLook at it§r and it freezes; turn your back and it closes in. Everything slows down near it.\n\n§cCorrupted Gnomes§r hunt players and gnomes, and their hits weaken you.",
"guide.mistech.gnome_realm.10":"Strange places in the Nightmare:\n• §8Wrong House§r (Wrong Neighborhood): three identical rooms... or the same room three times? At the back, a bedroom — don't go near the beds.\n• §8Endless Stair§r: a tower whose stairs repeat the same laps. Keep going and reach the chest at the top.",
}
for f,d in (('pt_br',PT),('en_us',EN)):
    p=B+f'lang/{f}.json'; raw=open(p,encoding='utf-8').read(); l=json.loads(raw)
    l.update(d)
    open(p,'w',encoding='utf-8').write(json.dumps(l,indent=2,ensure_ascii=False)+'\n')
print('ok')
