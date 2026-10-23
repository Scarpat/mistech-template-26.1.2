"""Dados dos Fragmentos de Sonho: modelos, blockstate da Fenda Onírica, rituais, textos (pt/en) e entrada do Grimório.
Uso: python tools/dream/dream_fragments_data.py (a partir da raiz do projeto)."""
import json, os

A = 'src/main/resources/assets/mistech/'
D = 'src/main/resources/data/mistech/'


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w', encoding='utf-8').write(json.dumps(obj, indent=2, ensure_ascii=False) + '\n')


# id, nome pt, nome en, ingredientes próprios da região
REGIONS = [
    ('gnome_meadow', 'Prado dos Gnomos', 'Gnome Meadow', ['mistech:dream_poppy', 'mistech:dreamflower', 'mistech:yellow_mushroom']),
    ('sweet_mushroom_grove', 'Cogumelal Doce', 'Sweet Mushroom Grove',
     ['mistech:sweet_mycelium', 'mistech:pink_dream_mushroom_block', 'mistech:cyan_dream_mushroom_block']),
    ('pillow_hills', 'Colinas de Travesseiro', 'Pillow Hills', ['mistech:cloud_wool', 'mistech:cloud_wool', 'mistech:cloud_cotton']),
    ('murky_wastes', 'Ermo Turvo', 'Murky Wastes', ['mistech:withered_grass', 'mistech:murky_log', 'mistech:sleepless_ivy']),
    ('hand_forest', 'Floresta das Mãos', 'Forest of Hands', ['mistech:withered_grass', 'mistech:black_thorn', 'mistech:eye_flower']),
    ('falling_chasms', 'Abismos de Queda', 'Falling Chasms', ['mistech:black_thorn', 'mistech:sleepless_ivy', 'minecraft:feather']),
    ('wrong_neighborhood', 'Bairro Errado', 'Wrong Neighborhood', ['mistech:withered_grass', 'minecraft:lantern', 'minecraft:oak_door']),
    ('cloud_sea', 'Prados Flutuantes', 'Floating Meadows', ['mistech:cloud_cotton', 'mistech:puffball', 'mistech:dense_cloud']),
    ('fractal_garden', 'Jardim Fractal', 'Fractal Garden', ['mistech:prismatic_grass', 'mistech:fractal_sapling', 'mistech:kaleido_flower']),
    ('inverted_forest', 'Floresta Invertida', 'Inverted Forest', ['mistech:kaleido_flower', 'mistech:prism_mushroom', 'minecraft:cherry_sapling']),
    ('starry_void', 'Vazio Estrelado', 'Starry Void', ['mistech:star_stone', 'mistech:star_moss', 'mistech:memory_lily']),
    ('memory_quilt', 'Colcha de Memórias', 'Memory Quilt', ['mistech:memory_shard', 'mistech:memory_lily', 'minecraft:painting']),
    ('abyssal_gaze', 'Olhar Abissal', 'Abyssal Gaze', ['mistech:watcher_eye', 'mistech:eye_flower', 'mistech:memory_lily']),
]

# ----- modelos dos itens
items = ['dream_fragment', 'amplified_dream_fragment'] + ['dream_fragment_' + r[0] for r in REGIONS]
for it in items:
    dump(A + f'items/{it}.json', {"model": {"type": "minecraft:model", "model": f"mistech:item/{it}"}})
    dump(A + f'models/item/{it}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": f"mistech:item/{it}"}})

# ----- Fenda Onírica (mesmo formato da Fenda Arcana)
model = json.load(open(A + 'models/block/arcana_rift.json'))
model['textures'] = {"particle": "mistech:block/dream_rift", "rift": "mistech:block/dream_rift"}
dump(A + 'models/block/dream_rift.json', model)
variants = {}
for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
    for half in ('lower', 'upper'):
        v = {"model": "mistech:block/dream_rift"}
        if rot:
            v["y"] = rot
        variants[f'facing={facing},half={half}'] = v
dump(A + 'blockstates/dream_rift.json', {"variants": variants})

# ----- rituais
rituals = {}
rituals['dream_fragment'] = {
    "type": "mistech:ritual", "catalyst": "mistech:star_fragment",
    "ingredients": ["mistech:dreamcatcher", "mistech:dream_poppy", "mistech:dreamflower", "mistech:impure_magic_shard", "minecraft:amethyst_shard"],
    "essence": 2, "duration": 300, "condition": "night", "min_tier": 1,
    "result": {"id": "mistech:dream_fragment"}, "effect": "stars"}
rituals['amplified_dream_fragment'] = {
    "type": "mistech:ritual", "catalyst": "mistech:dream_fragment",
    "ingredients": ["mistech:star_fragment", "mistech:dream_dust", "mistech:dream_dust", "mistech:impure_magic_shard",
                    "mistech:impure_magic_shard", "minecraft:ender_eye"],
    "essence": 4, "duration": 400, "condition": "night", "min_tier": 2,
    "result": {"id": "mistech:amplified_dream_fragment"}, "effect": "stars"}
for rid, _, _, extra in REGIONS:
    rituals['dream_fragment_' + rid] = {
        "type": "mistech:ritual", "catalyst": "mistech:dream_fragment",
        "ingredients": ["mistech:star_fragment", "mistech:impure_magic_shard"] + extra,
        "essence": 3, "duration": 300, "condition": "night", "min_tier": 2,
        "result": {"id": "mistech:dream_fragment_" + rid}, "effect": "stars"}
for name, r in rituals.items():
    dump(D + f'recipe/ritual/{name}.json', r)

# ----- textos
PT = {
    "block.mistech.dream_rift": "Fenda Onírica",
    "item.mistech.dream_fragment": "Fragmento de Sonho",
    "item.mistech.amplified_dream_fragment": "Fragmento de Sonho Amplificado",
    "tooltip.mistech.dream_fragment.random": "Traz uma região aleatória do Sonho",
    "tooltip.mistech.dream_fragment.region": "Traz: %s",
    "tooltip.mistech.dream_fragment.radius": "Alcance: %s blocos",
    "tooltip.mistech.dream_fragment.use": "Botão direito num bloco da Superfície",
    "message.mistech.dream_fragment.overworld_only": "O sonho só consegue romper o mundo desperto (Superfície).",
    "message.mistech.dream_fragment.no_space": "Não há espaço para a fenda abrir aqui.",
    "message.mistech.dream_fragment.opened": "Uma fenda se abre... o mundo começa a sonhar com: %s",
    "message.mistech.dream_fragment.done": "A fenda se fecha. Este lugar agora faz parte do Sonho: %s",
    "guide.mistech.entry.dream_fragments": "Fragmentos de Sonho",
    "guide.mistech.dream_fragments.title": "Fragmento de Sonho",
    "guide.mistech.dream_fragments.1": "Um §dFragmento de Estrela§r banhado no ritual certo guarda um pedaço do §5Sonho§r. Clique com ele num bloco da §eSuperfície§r e uma §dFenda Onírica§r se abre: o sonho vaza por ela e §ereescreve o lugar§r em volta.",
    "guide.mistech.dream_fragments.2": "A fenda espalha o sonho em anéis: árvores e plantas somem, o chão vira o chão da região (grama, micélio doce, lã de nuvem, grama murcha...). Quando termina, a fenda fecha e o lugar §epassa a ser aquela região do Sonho§r: céu, névoa, plantas e as §ecriaturas§r de lá, que continuam aparecendo.\n\nSó chão natural muda — construções ficam como estão.",
    "guide.mistech.dream_fragments.3": "O §dFragmento de Sonho§r (Tier I) traz uma região §esorteada§r, num raio de cerca de 12 blocos.",
    "guide.mistech.dream_fragments.4": "O §dFragmento Amplificado§r (Tier II) também sorteia a região, mas alcança cerca de §e28 blocos§r.",
    "guide.mistech.dream_fragments.5": "Os §dFragmentos Sintonizados§r (Tier II) trazem §esempre a mesma região§r. Todos usam o Fragmento de Sonho como catalisador, um Fragmento de Estrela e um Fragmento de Magia Impura, mais três coisas da região escolhida:",
    "guide.mistech.dream_fragments.6": "Cuidado com as regiões do §cPesadelo§r e do §cSono REM§r: as criaturas delas também vêm junto.",
}
EN = {
    "block.mistech.dream_rift": "Dream Rift",
    "item.mistech.dream_fragment": "Dream Fragment",
    "item.mistech.amplified_dream_fragment": "Amplified Dream Fragment",
    "tooltip.mistech.dream_fragment.random": "Brings a random region of the Dream",
    "tooltip.mistech.dream_fragment.region": "Brings: %s",
    "tooltip.mistech.dream_fragment.radius": "Reach: %s blocks",
    "tooltip.mistech.dream_fragment.use": "Right-click a block in the Overworld",
    "message.mistech.dream_fragment.overworld_only": "The dream can only break into the waking world (Overworld).",
    "message.mistech.dream_fragment.no_space": "There is no room for the rift to open here.",
    "message.mistech.dream_fragment.opened": "A rift opens... the world starts dreaming of: %s",
    "message.mistech.dream_fragment.done": "The rift closes. This place is now part of the Dream: %s",
    "guide.mistech.entry.dream_fragments": "Dream Fragments",
    "guide.mistech.dream_fragments.title": "Dream Fragment",
    "guide.mistech.dream_fragments.1": "A §dStar Fragment§r bathed in the right ritual holds a piece of the §5Dream§r. Use it on a block in the §eOverworld§r and a §dDream Rift§r opens: the dream leaks through it and §erewrites the place§r around it.",
    "guide.mistech.dream_fragments.2": "The rift spreads the dream in rings: trees and plants vanish, the ground turns into the region's ground (grass, sweet mycelium, cloud wool, withered grass...). When it is done the rift closes and the place §ebecomes that region of the Dream§r: sky, fog, plants and its §ecreatures§r, which keep showing up.\n\nOnly natural ground changes — buildings are left alone.",
    "guide.mistech.dream_fragments.3": "The §dDream Fragment§r (Tier I) brings a §erandom§r region, in a radius of about 12 blocks.",
    "guide.mistech.dream_fragments.4": "The §dAmplified Fragment§r (Tier II) also picks a random region, but reaches about §e28 blocks§r.",
    "guide.mistech.dream_fragments.5": "The §dAttuned Fragments§r (Tier II) §ealways bring the same region§r. All of them use a Dream Fragment as catalyst, a Star Fragment and an Impure Magic Shard, plus three things from the chosen region:",
    "guide.mistech.dream_fragments.6": "Beware the §cNightmare§r and §cREM Sleep§r regions: their creatures come along too.",
}
for rid, pt, en, _ in REGIONS:
    PT[f"item.mistech.dream_fragment_{rid}"] = f"Fragmento de Sonho: {pt}"
    EN[f"item.mistech.dream_fragment_{rid}"] = f"Dream Fragment: {en}"
    PT[f"guide.mistech.ritual.dream_fragment_{rid}"] = pt
    EN[f"guide.mistech.ritual.dream_fragment_{rid}"] = en
PT["guide.mistech.ritual.dream_fragment"] = "Fragmento de Sonho"
EN["guide.mistech.ritual.dream_fragment"] = "Dream Fragment"
PT["guide.mistech.ritual.amplified_dream_fragment"] = "Fragmento Amplificado"
EN["guide.mistech.ritual.amplified_dream_fragment"] = "Amplified Fragment"

for lang, table in (('pt_br', PT), ('en_us', EN)):
    p = A + f'lang/{lang}.json'
    data = json.load(open(p, encoding='utf-8'))
    data.update(table)
    open(p, 'w', encoding='utf-8').write(json.dumps(data, indent=2, ensure_ascii=False) + '\n')

# ----- Grimório: entrada na categoria do Reino, depois do Sonho dos Gnomos
book_path = A + 'guide/book.json'
book = json.load(open(book_path, encoding='utf-8'))


def ritual_page(name, title, text=None):
    r = rituals[name]
    page = {"type": "ritual", "catalyst": r["catalyst"], "ingredients": r["ingredients"], "essence": r["essence"],
            "condition": r["condition"], "result": r["result"]["id"], "title": title}
    if text:
        page["text"] = text
    return page


pages = [
    {"type": "spotlight", "item": "mistech:dream_fragment", "title": "guide.mistech.dream_fragments.title", "text": "guide.mistech.dream_fragments.1"},
    {"type": "text", "text": "guide.mistech.dream_fragments.2"},
    ritual_page('dream_fragment', "guide.mistech.ritual.dream_fragment", "guide.mistech.dream_fragments.3"),
    ritual_page('amplified_dream_fragment', "guide.mistech.ritual.amplified_dream_fragment", "guide.mistech.dream_fragments.4"),
    {"type": "text", "text": "guide.mistech.dream_fragments.5"},
] + [ritual_page('dream_fragment_' + rid, f"guide.mistech.ritual.dream_fragment_{rid}") for rid, *_ in REGIONS] + [
    {"type": "text", "text": "guide.mistech.dream_fragments.6"}]
entry = {"id": "dream_fragments", "icon": "mistech:dream_fragment", "name": "guide.mistech.entry.dream_fragments",
         "deps": ["gnome_realm"], "task": {"type": "item", "items": ["mistech:dream_fragment"]}, "x": 1.6, "y": 3.0, "pages": pages}
realm = next(c for c in book['categories'] if c['id'] == 'realm')
realm['entries'] = [e for e in realm['entries'] if e['id'] != 'dream_fragments'] + [entry]


def free_spot(view, x, y):
    taken = {(e['x'], e['y']) for e in view['entries'] if e['id'] != 'dream_fragments'}
    while any(abs(tx - x) < 1.0 and abs(ty - y) < 0.9 for tx, ty in taken):
        y += 1.0
    return y


for kind, vid, x, y in (('progress', 'p_essence', 8.0, 2.5), ('dimension', 'd_realm', 3.2, 1.0)):
    view = next(v for v in book['views'][kind] if v['id'] == vid)
    view['entries'] = [e for e in view['entries'] if e['id'] != 'dream_fragments']
    view['entries'].append({"id": "dream_fragments", "x": x, "y": free_spot(view, x, y)})
open(book_path, 'w', encoding='utf-8').write(json.dumps(book, indent=2, ensure_ascii=False) + '\n')
print('ok', len(rituals), 'rituais')
