"""Entrada do Louco no Grimório (categoria Tarô). Uso: python guide_fool.py <assets/mistech>"""
import json, sys, os
A = sys.argv[1]
bp = os.path.join(A, 'guide/book.json'); b = json.load(open(bp, encoding='utf-8'))
entry = {"id": "the_fool", "icon": "mistech:tarot_card_the_fool", "name": "guide.mistech.entry.the_fool", "deps": ["arcane_plane"],
         "task": {"type": "item", "items": ["mistech:tarot_card_the_fool"]}, "x": 6.4, "y": 1.5, "pages": [
    {"type": "text", "text": "guide.mistech.the_fool.1"},
    {"type": "text", "text": "guide.mistech.the_fool.2"},
    {"type": "spotlight", "item": "mistech:fool_cap", "text": "guide.mistech.the_fool.cap"},
    {"type": "spotlight", "item": "mistech:fool_staff", "text": "guide.mistech.the_fool.staff"}]}
cat = next(c for c in b['categories'] if c['id'] == 'tarot')
cat['entries'] = [e for e in cat['entries'] if e['id'] != 'the_fool'] + [entry]
for view, cid in (('progress', 'p_tier3'), ('dimension', 'd_overworld_tier3')):
    for c in b['views'][view]:
        if c['id'] == cid:
            c['entries'] = [e for e in c['entries'] if e['id'] != 'the_fool'] + [{"id": "the_fool", "x": 9.6, "y": 3.0}]
json.dump(b, open(bp, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
PT = {
 'guide.mistech.entry.the_fool': '0 · O Louco',
 'guide.mistech.the_fool.1': 'O primeiro arcano é um §dgênio do vento§r de chapéu de bobo. A arena dele é um §barquipélago de ilhas flutuantes§r.\n\nEle não anda: §dsalta de ilha em ilha§r, lança §dtornados§r que erguem e empurram, e dá golpes de cajado que arremessam longe.',
 'guide.mistech.the_fool.2': '§6Dicas§r\n- As §bCorrentes Ascendentes§r (terracota azul) jogam você para o alto com Queda Lenta. Agachado, você não é levado.\n- Depois de alguns pousos ele fica §etonto§r no chão: é a hora de bater (leva mais dano).\n- Abaixo de metade da vida ele gira puxando todos e estoura.\n- Cair das ilhas não mata: o vento devolve você, com um pouco de dano.',
 'guide.mistech.the_fool.cap': '§6Chapéu do Bobo§r: defesa de diamante, metade do dano de queda e passos mais leves. §dSalto do Louco§r: agache um instante e pule.\n\nForja: Runa do Ar, couro, lã amarela, lã branca e pepita de ouro.',
 'guide.mistech.the_fool.staff': '§6Cajado do Louco§r: arma leve que empurra muito. Botão direito lança um tornado; agachado, dá uma §dRajada§r para onde você olha, sem dano de queda.\n\nForja: Runa do Ar, 2 varas de breeze, pena e diamante.',
}
EN = {
 'guide.mistech.entry.the_fool': '0 · The Fool',
 'guide.mistech.the_fool.1': 'The first arcana is a §dwind genie§r in a jester\'s cap. His arena is an §barchipelago of floating islands§r.\n\nHe doesn\'t walk: he §dleaps from island to island§r, throws §dtornadoes§r that lift and push, and swings a staff that sends you flying.',
 'guide.mistech.the_fool.2': '§6Tips§r\n- §bUpdrafts§r (light blue terracotta) launch you up with Slow Falling. Sneaking keeps you grounded.\n- After a few landings he gets §edizzy§r on the ground: hit him then (he takes more damage).\n- Below half health he spins, pulling everyone in, then bursts.\n- Falling off the islands won\'t kill you: the wind brings you back, with a little damage.',
 'guide.mistech.the_fool.cap': '§6Fool\'s Cap§r: diamond defense, half fall damage and lighter steps. §dFool\'s Leap§r: crouch for a moment and jump.\n\nForge: Air Rune, leather, yellow wool, white wool and a gold nugget.',
 'guide.mistech.the_fool.staff': '§6Fool\'s Staff§r: a light weapon with big knockback. Right click throws a tornado; sneaking, a §dGust§r dash where you look, without fall damage.\n\nForge: Air Rune, 2 breeze rods, a feather and a diamond.',
}
for name, new in (('pt_br.json', PT), ('en_us.json', EN)):
    p = os.path.join(A, 'lang', name); d = json.load(open(p, encoding='utf-8')); d.update(new)
    json.dump(d, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
print('ok')
