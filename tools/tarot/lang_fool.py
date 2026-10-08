"""Traduções do Louco (chefe e itens). Uso: python lang_fool.py <pasta lang>"""
import json, sys, os
L = sys.argv[1]
PT = {
 'entity.mistech.the_fool': 'O Louco',
 'entity.mistech.fool_tornado': 'Tornado do Louco',
 'item.mistech.the_fool_spawn_egg': 'Ovo Gerador do Louco',
 'item.mistech.fool_cap': 'Chapéu do Bobo',
 'item.mistech.fool_cap.desc': 'Os guizos ainda riem baixinho',
 'item.mistech.fool_cap.leap': 'Salto do Louco: agache um instante e pule para dar um grande salto, sem dano de queda',
 'item.mistech.fool_staff': 'Cajado do Louco',
 'item.mistech.fool_staff.desc': 'Leve como o vento, travesso como quem o empunhava',
 'item.mistech.fool_staff.tornado': 'Botão direito: lança um tornado que ergue e empurra as criaturas',
 'item.mistech.fool_staff.dash': 'Agachado + botão direito: Rajada, um impulso para onde você olha',
 'tarot.mistech.option.the_fool.cap': 'Chapéu do Bobo',
 'tarot.mistech.option.the_fool.staff': 'Cajado do Louco',
 'message.mistech.fool.caught': 'O vento te pega no ar e te devolve... rindo.',
 'message.mistech.fool.phase2': 'O Louco gargalha: "Agora a brincadeira fica boa!"',
}
EN = {
 'entity.mistech.the_fool': 'The Fool',
 'entity.mistech.fool_tornado': 'Fool\'s Tornado',
 'item.mistech.the_fool_spawn_egg': 'The Fool Spawn Egg',
 'item.mistech.fool_cap': 'Fool\'s Cap',
 'item.mistech.fool_cap.desc': 'The bells still giggle softly',
 'item.mistech.fool_cap.leap': 'Fool\'s Leap: crouch for a moment and jump for a great leap, without fall damage',
 'item.mistech.fool_staff': 'Fool\'s Staff',
 'item.mistech.fool_staff.desc': 'Light as the wind, mischievous as its former bearer',
 'item.mistech.fool_staff.tornado': 'Right click: throws a tornado that lifts and pushes creatures',
 'item.mistech.fool_staff.dash': 'Sneak + right click: Gust, a dash where you look',
 'tarot.mistech.option.the_fool.cap': 'Fool\'s Cap',
 'tarot.mistech.option.the_fool.staff': 'Fool\'s Staff',
 'message.mistech.fool.caught': 'The wind catches you mid-air and brings you back... laughing.',
 'message.mistech.fool.phase2': 'The Fool cackles: "Now the game gets fun!"',
}
for name, new in (('pt_br.json', PT), ('en_us.json', EN)):
    p = os.path.join(L, name); d = json.load(open(p, encoding='utf-8')); d.update(new)
    json.dump(d, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
print('ok')
