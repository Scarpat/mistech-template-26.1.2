"""Entradas novas do Grimório (metais, Âncora Espacial, Orbe, Realinhamento). Rodar antes do tools/guide/journey.py."""
import json, sys
sys.path.insert(0, 'tools/pendencias2')
from res import lang
BOOK = 'src/main/resources/assets/mistech/guide/book.json'
b = json.load(open(BOOK, encoding='utf-8'))
cats = {c['id']: c for c in b['categories']}
def put(cat, entry):
    es = cats[cat]['entries']
    es[:] = [e for e in es if e['id'] != entry['id']] + [entry]
put('realm', {'id': 'metal_blocks', 'icon': 'mistech:mithril_block', 'name': 'guide.mistech.entry.metal_blocks', 'pages': [
    {'type': 'crafting', 'grid': ['mistech:mithril_ingot'] * 9, 'result': 'mistech:mithril_block', 'text': 'guide.mistech.metal_blocks.1'},
    {'type': 'crafting', 'grid': ['mistech:adamantine_ingot'] * 9, 'result': 'mistech:adamantine_block'},
    {'type': 'crafting', 'grid': ['mistech:orichalcum_ingot'] * 9, 'result': 'mistech:orichalcum_block'},
    {'type': 'crafting', 'grid': ['minecraft:flint', '#c:ores/mithril', '', '', '', '', '', '', ''], 'result': 'mistech:mithril_flint_and_steel',
     'text': 'guide.mistech.metal_blocks.2'},
    {'type': 'crafting', 'grid': ['minecraft:flint', '#c:ores/adamantine', '', '', '', '', '', '', ''], 'result': 'mistech:adamantine_flint_and_steel'},
    {'type': 'crafting', 'grid': ['minecraft:flint', '#c:ores/orichalcum', '', '', '', '', '', '', ''], 'result': 'mistech:orichalcum_flint_and_steel'},
]})
put('rituals', {'id': 'spatial_anchor', 'icon': 'mistech:spatial_anchor', 'name': 'guide.mistech.entry.spatial_anchor', 'pages': [
    {'type': 'ritual', 'catalyst': 'minecraft:ender_eye', 'ingredients': ['mistech:space_rune', 'mistech:impure_magic_block',
     'mistech:impure_magic_block', 'minecraft:emerald'], 'essence': 3, 'result': 'mistech:spatial_anchor',
     'title': 'guide.mistech.ritual.spatial_anchor', 'text': 'guide.mistech.spatial_anchor.1'},
    {'type': 'text', 'text': 'guide.mistech.spatial_anchor.2'},
    {'type': 'text', 'text': 'guide.mistech.spatial_anchor.3'},
    {'type': 'text', 'text': 'guide.mistech.spatial_anchor.4'},
]})
put('rituals', {'id': 'translocation_orb', 'icon': 'mistech:translocation_orb', 'name': 'guide.mistech.entry.translocation_orb', 'pages': [
    {'type': 'ritual', 'catalyst': 'minecraft:ender_pearl', 'ingredients': ['mistech:space_rune', 'mistech:impure_magic_shard',
     'mistech:impure_magic_shard', 'mistech:mithril_ingot'], 'essence': 5, 'min_tier': 2, 'result': 'mistech:translocation_orb',
     'title': 'guide.mistech.ritual.translocation_orb', 'text': 'guide.mistech.translocation_orb.1'},
]})
def realign(part, extra, cond):
    return {'type': 'ritual', 'catalyst': 'mistech:natal_chart', 'ingredients': ['#mistech:zodiac_tokens'] + extra +
            ['mistech:impure_magic_shard', 'mistech:impure_magic_shard'], 'essence': 6, 'min_tier': 2, 'condition': cond,
            'result': 'mistech:natal_chart', 'title': 'guide.mistech.ritual.realign_' + part}
put('astrology', {'id': 'natal_realign', 'icon': 'mistech:natal_chart', 'name': 'guide.mistech.entry.natal_realign', 'pages': [
    {'type': 'text', 'text': 'guide.mistech.natal_realign.1'},
    {'type': 'text', 'text': 'guide.mistech.natal_realign.2'},
    realign('sun', ['mistech:light_rune', 'mistech:fire_rune', 'minecraft:gold_block'], 'day'),
    realign('moon', ['mistech:darkness_rune', 'mistech:water_rune', 'minecraft:glowstone'], 'full_moon'),
    realign('rising', ['mistech:time_rune', 'mistech:air_rune', 'minecraft:amethyst_block'], 'dawn'),
]})
with open(BOOK, 'w', encoding='utf-8') as f:
    json.dump(b, f, ensure_ascii=False, indent=2); f.write('\n')
lang({
 'guide.mistech.entry.metal_blocks': 'Metal Blocks and Lighters',
 'guide.mistech.metal_blocks.1': 'Nine ingots make a §lblock§r (and a block turns back into nine ingots). Mithril, Adamantine and Orichalcum blocks also work as a §lbeacon base§r, and their ingots can pay the beacon.',
 'guide.mistech.metal_blocks.2': '§lFlint§r + a §lraw ore block§r of the metal makes a lighter. It works just like Flint and Steel, but lasts much longer: Mithril 256 uses, Adamantine 640, Orichalcum 1280.',
 'guide.mistech.entry.spatial_anchor': 'Spatial Anchor',
 'guide.mistech.spatial_anchor.1': 'A pillar that folds space. Tier I ritual.',
 'guide.mistech.spatial_anchor.2': 'Place the anchor (2 blocks tall). Rename the item on an anvil first to give it a name.\n\n§lRight-click§r an anchor to §lattune§r to it: it joins your list. The list opens on every anchor you touch; pick one and you travel there.',
 'guide.mistech.spatial_anchor.3': 'The trip costs §lexperience levels§r: 1 level for every 1000 blocks (up to 5), and §l5 levels§r to another dimension. Short trips are free.\n\nIf an anchor is broken it disappears from everyone\'s list.',
 'guide.mistech.entry.translocation_orb': 'Translocation Orb',
 'guide.mistech.translocation_orb.1': 'A pocket anchor. Right-click to open your anchor list from anywhere. Costs the same levels as an anchor, spends 1 of its 48 uses and needs 15 s to recharge.',
 'guide.mistech.entry.natal_realign': 'Realigning the Stars',
 'guide.mistech.natal_realign.1': 'With an altar of §lTier II§r you can choose the sign of your §5Sun§r, §5Moon§r or §5Ascendant§r, one at a time. The chosen part is revealed at once.\n\nCatalyst: your §lNatal Chart§r.',
 'guide.mistech.natal_realign.2': 'To choose the sign, put the §lrare item§r of its constellation on a pedestal (the same one used at the Astrology Table): Aries blaze powder, Taurus golden apple, Gemini rabbit foot, Cancer nautilus shell, Leo blaze rod, Virgo emerald, Libra iron block, Scorpio fermented spider eye, Sagittarius ender pearl, Capricorn diamond, Aquarius glow ink sac, Pisces tropical fish.',
}, {
 'guide.mistech.entry.metal_blocks': 'Blocos de Metal e Isqueiros',
 'guide.mistech.metal_blocks.1': 'Nove lingotes fazem um §lbloco§r (e o bloco volta a ser nove lingotes). Os blocos de Mithril, Adamantina e Oricalco também servem de §lbase de sinalizador§r, e os lingotes pagam o sinalizador.',
 'guide.mistech.metal_blocks.2': '§lSílex§r + um §lminério§r do metal fazem um isqueiro. Funciona igual ao Isqueiro comum, mas dura bem mais: Mithril 256 usos, Adamantina 640, Oricalco 1280.',
 'guide.mistech.entry.spatial_anchor': 'Âncora Espacial',
 'guide.mistech.spatial_anchor.1': 'Um pilar que dobra o espaço. Ritual Tier I.',
 'guide.mistech.spatial_anchor.2': 'Coloque a âncora (2 blocos de altura). Renomeie o item na bigorna antes para dar um nome a ela.\n\n§lBotão direito§r numa âncora a §lsintoniza§r: ela entra na sua lista. A lista abre em toda âncora que você tocar; escolha uma e você viaja até lá.',
 'guide.mistech.spatial_anchor.3': 'A viagem custa §lníveis de experiência§r: 1 nível a cada 1000 blocos (até 5), e §l5 níveis§r para outra dimensão. Viagens curtas são grátis.\n\nSe a âncora for quebrada, ela some da lista de todo mundo.',
 'guide.mistech.entry.translocation_orb': 'Orbe de Translocação',
 'guide.mistech.translocation_orb.1': 'Uma âncora de bolso. Botão direito abre a sua lista de âncoras de qualquer lugar. Custa os mesmos níveis da âncora, gasta 1 dos 48 usos e precisa de 15 s para recarregar.',
 'guide.mistech.entry.natal_realign': 'Realinhar os Astros',
 'guide.mistech.natal_realign.1': 'Com um altar §lTier II§r você escolhe o signo do seu §5Sol§r, da sua §5Lua§r ou do seu §5Ascendente§r, um de cada vez. A parte escolhida já fica revelada.\n\nCatalisador: o seu §lMapa Astral§r.',
 'guide.mistech.natal_realign.2': 'Para escolher o signo, ponha num pedestal o §litem raro§r da constelação (o mesmo da Mesa de Astrologia): Áries pó de blaze, Touro maçã dourada, Gêmeos pé de coelho, Câncer concha de náutilo, Leão vara de blaze, Virgem esmeralda, Libra bloco de ferro, Escorpião olho fermentado, Sagitário pérola do Ender, Capricórnio diamante, Aquário bolsa de tinta brilhante, Peixes peixe tropical.',
})
print('ok')
