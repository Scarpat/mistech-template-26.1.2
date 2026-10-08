# Entradas do Grimório e textos (pt_br/en_us) dos Artefatos Rúnicos. Idempotente.
# Uso (na pasta do projeto): python3 tools/artifacts/guide_entries.py && python3 tools/guide/journey.py && python3 tools/guide/views.py
import json

BOOK = 'src/main/resources/assets/mistech/guide/book.json'
LANG = 'src/main/resources/assets/mistech/lang/'
S = 'mistech:impure_magic_shard'

def ritual(cat, ing, ess, res, tier, title):
    return {'type': 'ritual', 'catalyst': cat, 'ingredients': ing, 'essence': ess, 'result': res, 'min_tier': tier, 'title': title}

def txt(k):
    return {'type': 'text', 'text': k}

E = [
    ('runic_artifacts', 'minecraft:enchanted_book', [txt('guide.mistech.runic_artifacts.1'), txt('guide.mistech.runic_artifacts.2')]),
    ('wind_boots', 'mistech:wind_boots', [
        ritual('mistech:mithril_boots', ['mistech:air_rune', 'mistech:air_rune', 'minecraft:wind_charge', 'minecraft:wind_charge', 'minecraft:phantom_membrane', S], 4, 'mistech:wind_boots', 2, 'guide.mistech.ritual.wind_boots'),
        txt('guide.mistech.wind_boots.1'),
        ritual('mistech:wind_boots', ['mistech:cloud_rune', 'mistech:cloud_rune', 'minecraft:feather', 'minecraft:feather', 'minecraft:phantom_membrane', S], 4, 'mistech:wind_boots', 2, 'guide.mistech.ritual.wind_boots_cloud_step'),
        txt('guide.mistech.wind_boots.2')]),
    ('frost_skates', 'mistech:frost_skates', [
        ritual('mistech:mithril_boots', ['mistech:ice_rune', 'mistech:ice_rune', 'minecraft:blue_ice', 'minecraft:packed_ice'], 3, 'mistech:frost_skates', 1, 'guide.mistech.ritual.frost_skates'),
        txt('guide.mistech.frost_skates.1')]),
    ('storm_gauntlet', 'mistech:storm_gauntlet', [
        ritual('mistech:mithril_sword', ['mistech:energy_rune', 'mistech:energy_rune', 'mistech:bottled_lightning', 'minecraft:lightning_rod'], 3, 'mistech:storm_gauntlet', 1, 'guide.mistech.ritual.storm_gauntlet'),
        txt('guide.mistech.storm_gauntlet.1')]),
    ('volcanic_hammer', 'mistech:volcanic_hammer', [
        ritual('mistech:adamantine_pickaxe', ['mistech:magma_rune', 'mistech:magma_rune', 'mistech:fire_rune', 'mistech:fire_rune', 'minecraft:magma_block', 'mistech:adamantine_block', S], 5, 'mistech:volcanic_hammer', 2, 'guide.mistech.ritual.volcanic_hammer'),
        txt('guide.mistech.volcanic_hammer.1')]),
    ('tide_mask', 'mistech:tide_mask', [
        ritual('mistech:adamantine_helmet', ['mistech:water_rune', 'mistech:water_rune', 'minecraft:nautilus_shell', 'minecraft:heart_of_the_sea', 'minecraft:prismarine_crystals', S], 4, 'mistech:tide_mask', 2, 'guide.mistech.ritual.tide_mask'),
        txt('guide.mistech.tide_mask.1')]),
    ('beacon_crown', 'mistech:beacon_crown', [
        ritual('mistech:adamantine_helmet', ['mistech:light_rune', 'mistech:light_rune', 'minecraft:glowstone', 'minecraft:gold_block', 'minecraft:spyglass', S], 4, 'mistech:beacon_crown', 2, 'guide.mistech.ritual.beacon_crown'),
        txt('guide.mistech.beacon_crown.1')]),
    ('cloud_broom', 'mistech:cloud_broom', [
        {'type': 'crafting', 'grid': ['', '', 'minecraft:stick', '', 'minecraft:string', '', 'minecraft:hay_block', '', ''], 'result': 'mistech:broom'},
        ritual('mistech:broom', ['mistech:cloud_rune', 'mistech:cloud_rune', 'mistech:air_rune', 'mistech:air_rune', 'minecraft:phantom_membrane', 'minecraft:phantom_membrane', 'mistech:orichalcum_block', S], 6, 'mistech:cloud_broom', 3, 'guide.mistech.ritual.cloud_broom'),
        txt('guide.mistech.cloud_broom.1'), txt('guide.mistech.cloud_broom.2')]),
    ('shadow_cloak', 'mistech:shadow_cloak', [
        ritual('mistech:orichalcum_chestplate', ['mistech:darkness_rune', 'mistech:darkness_rune', 'mistech:void_rune', 'minecraft:phantom_membrane', 'minecraft:phantom_membrane', 'minecraft:echo_shard', S], 6, 'mistech:shadow_cloak', 3, 'guide.mistech.ritual.shadow_cloak'),
        txt('guide.mistech.shadow_cloak.1')]),
    ('tide_spear', 'mistech:tide_spear', [
        ritual('minecraft:trident', ['mistech:water_rune', 'mistech:water_rune', 'mistech:air_rune', 'mistech:air_rune', 'mistech:orichalcum_block', 'minecraft:heart_of_the_sea', S], 6, 'mistech:tide_spear', 3, 'guide.mistech.ritual.tide_spear'),
        txt('guide.mistech.tide_spear.1')]),
    ('void_magnet', 'mistech:void_magnet', [
        ritual('mistech:amulet_blank', ['mistech:void_rune', 'mistech:space_rune', 'minecraft:lodestone', 'minecraft:ender_pearl', S], 4, 'mistech:void_magnet', 2, 'guide.mistech.ritual.void_magnet'),
        txt('guide.mistech.void_magnet.1')]),
    ('pocket_hourglass', 'mistech:pocket_hourglass', [
        ritual('mistech:amulet_blank', ['mistech:time_rune', 'mistech:time_rune', 'minecraft:clock', 'minecraft:sand', 'minecraft:glass', S], 4, 'mistech:pocket_hourglass', 2, 'guide.mistech.ritual.pocket_hourglass'),
        txt('guide.mistech.pocket_hourglass.1')]),
    ('spatial_ring', 'mistech:spatial_ring', [
        ritual('mistech:gold_ring_blank', ['mistech:space_rune', 'mistech:space_rune', 'minecraft:ender_eye', 'minecraft:ender_pearl', 'mistech:mithril_ingot', S], 4, 'mistech:spatial_ring', 2, 'guide.mistech.ritual.spatial_ring'),
        txt('guide.mistech.spatial_ring.1')]),
]

b = json.load(open(BOOK, encoding='utf-8'))
cat = next((c for c in b['categories'] if c['id'] == 'artifacts'), None)
if cat is None:
    cat = {'id': 'artifacts', 'icon': 'mistech:wind_boots', 'name': 'guide.mistech.cat.artifacts',
           'description': 'guide.mistech.cat.artifacts.desc', 'entries': []}
    b['categories'].append(cat)
have = {e['id'] for c in b['categories'] for e in c['entries']}
for eid, icon, pages in E:
    entry = {'id': eid, 'icon': icon, 'name': 'guide.mistech.entry.' + eid, 'deps': [], 'task': {'type': 'read'}, 'x': 0, 'y': 0, 'pages': pages}
    if eid in have:
        for c in b['categories']:
            for e in c['entries']:
                if e['id'] == eid:
                    e['pages'] = pages
    else:
        cat['entries'].append(entry)
json.dump(b, open(BOOK, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

PT = {
    'item.mistech.wind_boots': 'Botas do Vento', 'item.mistech.frost_skates': 'Patins Gélidos', 'item.mistech.storm_gauntlet': 'Manopla da Tempestade',
    'item.mistech.volcanic_hammer': 'Marreta Vulcânica', 'item.mistech.tide_mask': 'Máscara das Marés', 'item.mistech.beacon_crown': 'Coroa do Farol',
    'item.mistech.broom': 'Vassoura', 'item.mistech.cloud_broom': 'Vassoura das Nuvens', 'item.mistech.shadow_cloak': 'Manto das Sombras',
    'item.mistech.tide_spear': 'Lança das Marés', 'item.mistech.void_magnet': 'Ímã do Vazio', 'item.mistech.pocket_hourglass': 'Ampulheta de Bolso',
    'item.mistech.spatial_ring': 'Anel Espacial', 'entity.mistech.cloud_broom': 'Vassoura das Nuvens',
    'item.mistech.wind_boots.desc': 'Pulo duplo no ar (200 FE por pulo)',
    'item.mistech.wind_boots.cloud_step': 'Passo de Nuvem: pulo triplo, e cada pulo extra deixa uma nuvem',
    'item.mistech.frost_skates.desc': 'Congelam a água em volta; muito rápidos no gelo',
    'item.mistech.storm_gauntlet.desc': 'Golpes soltam um raio em cadeia (3 alvos, 400 FE)',
    'item.mistech.tide_mask.desc': 'Debaixo d\'água: fôlego infinito, visão clara e mineração normal',
    'item.mistech.beacon_crown.desc': 'Visão Noturna; monstros à vista brilham (Eco engastado: através das paredes)',
    'item.mistech.shadow_cloak.desc': 'Agachado e parado: invisível, e os monstros perdem você de vista',
    'item.mistech.volcanic_hammer.desc': 'Minera 3×3 (agachado: 1×1). Agachar + botão direito: liga/desliga o derreter',
    'item.mistech.volcanic_hammer.smelt_on': 'Derreter: ligado', 'item.mistech.volcanic_hammer.smelt_off': 'Derreter: desligado',
    'item.mistech.cloud_broom.desc': 'Botão direito: montar e voar. Agachar + botão direito na vassoura: recolher',
    'item.mistech.cloud_broom.empty': 'A vassoura está sem energia',
    'item.mistech.tide_spear.desc': 'Sempre volta. Agachado: correnteza na água, na chuva ou sobre nuvens',
    'item.mistech.tide_spear.needs_water': 'Precisa de água, chuva ou uma nuvem embaixo',
    'item.mistech.void_magnet.added': '%s será apagado pelo ímã', 'item.mistech.void_magnet.removed': '%s não será mais apagado',
    'item.mistech.void_magnet.full': 'A lista do ímã está cheia (9 itens)',
    'item.mistech.spatial_ring.missing': 'Você não está usando um Anel Espacial (tecla J)',
    'item.mistech.spatial_ring.no_energy': 'O Anel Espacial precisa de %s FE',
    'tooltip.mistech.artifact.on': 'Ligado', 'tooltip.mistech.artifact.off': 'Desligado',
    'tooltip.mistech.accessory.void_magnet': 'Puxa os itens do chão (8 blocos) e apaga os da lista',
    'tooltip.mistech.accessory.pocket_hourglass': 'Acelera plantas e fornalhas a até 5 blocos',
    'tooltip.mistech.accessory.spatial_ring': 'Tecla V: viajar para uma Âncora conhecida, pagando em energia',
    'key.mistech.spatial_ring': 'Anel Espacial',
    'guide.mistech.cat.artifacts': 'Artefatos Rúnicos', 'guide.mistech.cat.artifacts.desc': 'Equipamento ligado a uma runa.',
    'guide.mistech.entry.runic_artifacts': 'Artefatos Rúnicos',
    'guide.mistech.runic_artifacts.1': 'Um §dartefato§r é uma peça ligada a uma runa: o poder dela está sempre ativo, sem precisar de engaste.\n\nComo o equipamento revestido, os artefatos usam §benergia§r no lugar da durabilidade — carregue na §5Bateria de Bolso§r ou nas baterias.',
    'guide.mistech.runic_artifacts.2': 'A base muda:\n\n§bMitril§r — simples e forte, sem engastes.\n§cAdamantina§r — 3 engastes na Bigorna Rúnica.\n§6Oricalco§r — 6 engastes.\n\nAlgumas runas engastadas mudam o poder do artefato. Os acessórios-artefato vão nos espaços da tecla §lJ§r.',
    'guide.mistech.entry.wind_boots': 'Botas do Vento', 'guide.mistech.ritual.wind_boots': 'Ligar ao Vento',
    'guide.mistech.wind_boots.1': '§bMitril§r. Aperte §lpulo§r de novo no ar para um §bpulo duplo§r (200 FE). Cada pulo zera a queda.\n\nAs botas contam como mitril para o bônus da armadura completa.',
    'guide.mistech.ritual.wind_boots_cloud_step': 'Passo de Nuvem',
    'guide.mistech.wind_boots.2': 'Com duas §fRunas das Nuvens§r as botas ganham o §fPasso de Nuvem§r: §lpulo triplo§r, e cada pulo extra deixa uma nuvem embaixo dos pés.',
    'guide.mistech.entry.frost_skates': 'Patins Gélidos', 'guide.mistech.ritual.frost_skates': 'Lâminas de Gelo',
    'guide.mistech.frost_skates.1': '§bMitril§r. Andando, a água parada num raio de 3 blocos §bcongela§r. Em cima de §bgelo§r você fica §l60% mais rápido§r.\n\nGasta 10 FE por segundo enquanto anda.',
    'guide.mistech.entry.storm_gauntlet': 'Manopla da Tempestade', 'guide.mistech.ritual.storm_gauntlet': 'Carga da Tempestade',
    'guide.mistech.storm_gauntlet.1': '§bMitril§r. Uma arma de soco. Cada golpe (1 por segundo) solta um §eraio em cadeia§r que pula para até §l3§r criaturas a 6 blocos, 5 de dano em cada (400 FE).\n\nNão acerta jogadores nem os seus animais.',
    'guide.mistech.entry.volcanic_hammer': 'Marreta Vulcânica', 'guide.mistech.ritual.volcanic_hammer': 'Forja Vulcânica',
    'guide.mistech.volcanic_hammer.1': '§cAdamantina§r (3 engastes). Minera em §l3×3§r (agachado: 1×1) e §6derrete§r o que cai (minério vira lingote, areia vira vidro), com a experiência da fornalha.\n\n§7Agachar + botão direito§r liga/desliga o derreter.\n§7Runa da Terra engastada§r: área §l5×5§r.',
    'guide.mistech.entry.tide_mask': 'Máscara das Marés', 'guide.mistech.ritual.tide_mask': 'Fôlego das Marés',
    'guide.mistech.tide_mask.1': '§cAdamantina§r (3 engastes). Com a cabeça debaixo d\'água: §3Força do Conduíte§r (fôlego infinito, visão clara, mineração normal). 20 FE/s submerso.\n\n§7Runa de Água engastada§r: Graça do Golfinho.',
    'guide.mistech.entry.beacon_crown': 'Coroa do Farol', 'guide.mistech.ritual.beacon_crown': 'Coroa de Luz',
    'guide.mistech.beacon_crown.1': '§cAdamantina§r (3 engastes). §eVisão Noturna§r sempre, e monstros a até 24 blocos que você §lvê§r ficam brilhando. 20 FE/s.\n\n§7Runa do Eco engastada§r: brilham mesmo atrás das paredes.',
    'guide.mistech.entry.cloud_broom': 'Vassoura das Nuvens', 'guide.mistech.ritual.cloud_broom': 'Encantar a Vassoura',
    'guide.mistech.cloud_broom.1': '§6Oricalco§r (6 engastes). §lBotão direito§r solta a vassoura e você monta.\n\n• Frente/trás: voa para onde você olha.\n• Pulo: sobe.\n• Agachar: desce da vassoura (e cai devagar).\n\nGasta 200 FE por segundo montada; sem energia, ela desce planando.',
    'guide.mistech.cloud_broom.2': 'Parada, ela fica flutuando. §7Botão direito§r monta de novo; §7agachar + botão direito§r (ou bater nela) recolhe.\n\nRunas engastadas: §fAr§r = mais rápida, §eEnergia§r = gasta menos, §fNuvens§r = rastro de nuvem e queda lenta mais longa, §eLuz§r = brilha, §cFogo§r/§bGelo§r = rastro.',
    'guide.mistech.entry.shadow_cloak': 'Manto das Sombras', 'guide.mistech.ritual.shadow_cloak': 'Tecer as Sombras',
    'guide.mistech.shadow_cloak.1': '§6Oricalco§r (6 engastes). Ritual à noite. §7Agachado e parado§r por 1 s: você fica §8invisível§r e os monstros que te perseguiam perdem o alvo. 10 FE/s.\n\n§7Runa do Vazio engastada§r: 15% por runa (até 45%) de §5sumir e reaparecer§r perto quando leva dano (recarga 5 s).',
    'guide.mistech.entry.tide_spear': 'Lança das Marés', 'guide.mistech.ritual.tide_spear': 'Chamar as Marés',
    'guide.mistech.tide_spear.1': '§6Oricalco§r (6 engastes). Ritual na chuva, sobre um tridente (os encantamentos passam). Tem §lLealdade III§r embutida: sempre volta.\n\n§7Agachado§r, soltar = §3correnteza§r na água, na chuva ou em cima de uma §fnuvem§r — sem precisar do encantamento (400 FE).',
    'guide.mistech.entry.void_magnet': 'Ímã do Vazio', 'guide.mistech.ritual.void_magnet': 'Magnetizar o Vazio',
    'guide.mistech.void_magnet.1': 'Amuleto (tecla §lJ§r). Puxa os itens do chão a até 8 blocos (2 FE por item). Agachado, não puxa.\n\n§7Botão direito§r na mão: liga/desliga.\n§7Agachar + botão direito§r com um item na outra mão: põe/tira esse item da §5lista de apagar§r (até 9) — o ímã some com ele em vez de puxar.',
    'guide.mistech.entry.pocket_hourglass': 'Ampulheta de Bolso', 'guide.mistech.ritual.pocket_hourglass': 'Prender o Tempo',
    'guide.mistech.pocket_hourglass.1': 'Amuleto (tecla §lJ§r). Plantas a até 5 blocos crescem mais rápido e §lfornalhas§r ali trabalham em dobro. 20 FE/s.\n\n§7Botão direito§r na mão: liga/desliga.',
    'guide.mistech.entry.spatial_ring': 'Anel Espacial', 'guide.mistech.ritual.spatial_ring': 'Dobrar o Espaço',
    'guide.mistech.spatial_ring.1': 'Anel (tecla §lJ§r). Aperte §lV§r para abrir a lista de §5Âncoras Espaciais§r que você conhece e viajar de qualquer lugar.\n\nNo lugar dos níveis, paga §benergia§r: 5.000 FE (10.000 para outra dimensão).',
}
EN = {
    'item.mistech.wind_boots': 'Wind Boots', 'item.mistech.frost_skates': 'Frost Skates', 'item.mistech.storm_gauntlet': 'Storm Gauntlet',
    'item.mistech.volcanic_hammer': 'Volcanic Hammer', 'item.mistech.tide_mask': 'Tide Mask', 'item.mistech.beacon_crown': 'Beacon Crown',
    'item.mistech.broom': 'Broom', 'item.mistech.cloud_broom': 'Cloud Broom', 'item.mistech.shadow_cloak': 'Shadow Cloak',
    'item.mistech.tide_spear': 'Tide Spear', 'item.mistech.void_magnet': 'Void Magnet', 'item.mistech.pocket_hourglass': 'Pocket Hourglass',
    'item.mistech.spatial_ring': 'Spatial Ring', 'entity.mistech.cloud_broom': 'Cloud Broom',
    'item.mistech.wind_boots.desc': 'Double jump in midair (200 FE per jump)',
    'item.mistech.wind_boots.cloud_step': 'Cloud Step: triple jump, and each extra jump leaves a cloud',
    'item.mistech.frost_skates.desc': 'Freeze nearby water; very fast on ice',
    'item.mistech.storm_gauntlet.desc': 'Hits release chain lightning (3 targets, 400 FE)',
    'item.mistech.tide_mask.desc': 'Underwater: endless breath, clear sight and normal mining',
    'item.mistech.beacon_crown.desc': 'Night Vision; monsters in sight glow (Echo socketed: through walls)',
    'item.mistech.shadow_cloak.desc': 'Sneaking still: invisible, and monsters lose track of you',
    'item.mistech.volcanic_hammer.desc': 'Mines 3×3 (sneaking: 1×1). Sneak + right-click: toggle smelting',
    'item.mistech.volcanic_hammer.smelt_on': 'Smelting: on', 'item.mistech.volcanic_hammer.smelt_off': 'Smelting: off',
    'item.mistech.cloud_broom.desc': 'Right-click: mount and fly. Sneak + right-click the broom: pick it up',
    'item.mistech.cloud_broom.empty': 'The broom is out of energy',
    'item.mistech.tide_spear.desc': 'Always returns. Sneaking: riptide in water, rain or on clouds',
    'item.mistech.tide_spear.needs_water': 'Needs water, rain or a cloud underfoot',
    'item.mistech.void_magnet.added': '%s will be voided by the magnet', 'item.mistech.void_magnet.removed': '%s will no longer be voided',
    'item.mistech.void_magnet.full': 'The magnet list is full (9 items)',
    'item.mistech.spatial_ring.missing': 'You are not wearing a Spatial Ring (J key)',
    'item.mistech.spatial_ring.no_energy': 'The Spatial Ring needs %s FE',
    'tooltip.mistech.artifact.on': 'On', 'tooltip.mistech.artifact.off': 'Off',
    'tooltip.mistech.accessory.void_magnet': 'Pulls dropped items (8 blocks) and voids listed ones',
    'tooltip.mistech.accessory.pocket_hourglass': 'Speeds up plants and furnaces within 5 blocks',
    'tooltip.mistech.accessory.spatial_ring': 'V key: travel to a known Anchor, paying with energy',
    'key.mistech.spatial_ring': 'Spatial Ring',
    'guide.mistech.cat.artifacts': 'Runic Artifacts', 'guide.mistech.cat.artifacts.desc': 'Gear bound to a rune.',
    'guide.mistech.entry.runic_artifacts': 'Runic Artifacts',
    'guide.mistech.runic_artifacts.1': 'An §dartifact§r is a piece bound to a rune: its power is always on, no socket needed.\n\nLike coated gear, artifacts use §benergy§r instead of durability — charge them with the §5Pocket Battery§r or batteries.',
    'guide.mistech.runic_artifacts.2': 'The base varies:\n\n§bMithril§r — simple and strong, no sockets.\n§cAdamantine§r — 3 sockets on the Rune Anvil.\n§6Orichalcum§r — 6 sockets.\n\nSome socketed runes change the artifact power. Artifact accessories go in the §lJ§r slots.',
    'guide.mistech.entry.wind_boots': 'Wind Boots', 'guide.mistech.ritual.wind_boots': 'Bind to the Wind',
    'guide.mistech.wind_boots.1': '§bMithril§r. Press §ljump§r again in midair for a §bdouble jump§r (200 FE). Each jump resets your fall.\n\nThe boots count as mithril for the full-set bonus.',
    'guide.mistech.ritual.wind_boots_cloud_step': 'Cloud Step',
    'guide.mistech.wind_boots.2': 'With two §fCloud Runes§r the boots gain §fCloud Step§r: §ltriple jump§r, and each extra jump leaves a cloud under your feet.',
    'guide.mistech.entry.frost_skates': 'Frost Skates', 'guide.mistech.ritual.frost_skates': 'Ice Blades',
    'guide.mistech.frost_skates.1': '§bMithril§r. Walking §bfreezes§r still water within 3 blocks. On §bice§r you are §l60% faster§r.\n\nUses 10 FE per second while walking.',
    'guide.mistech.entry.storm_gauntlet': 'Storm Gauntlet', 'guide.mistech.ritual.storm_gauntlet': 'Storm Charge',
    'guide.mistech.storm_gauntlet.1': '§bMithril§r. A punching weapon. Each hit (once per second) releases §echain lightning§r that jumps to up to §l3§r creatures within 6 blocks, 5 damage each (400 FE).\n\nIt never hits players or your pets.',
    'guide.mistech.entry.volcanic_hammer': 'Volcanic Hammer', 'guide.mistech.ritual.volcanic_hammer': 'Volcanic Forge',
    'guide.mistech.volcanic_hammer.1': '§cAdamantine§r (3 sockets). Mines §l3×3§r (sneaking: 1×1) and §6smelts§r what drops (ore into ingots, sand into glass), with the furnace experience.\n\n§7Sneak + right-click§r toggles smelting.\n§7Socketed Earth Rune§r: §l5×5§r area.',
    'guide.mistech.entry.tide_mask': 'Tide Mask', 'guide.mistech.ritual.tide_mask': 'Breath of the Tides',
    'guide.mistech.tide_mask.1': '§cAdamantine§r (3 sockets). With your head underwater: §3Conduit Power§r (endless breath, clear sight, normal mining). 20 FE/s while submerged.\n\n§7Socketed Water Rune§r: Dolphin\'s Grace.',
    'guide.mistech.entry.beacon_crown': 'Beacon Crown', 'guide.mistech.ritual.beacon_crown': 'Crown of Light',
    'guide.mistech.beacon_crown.1': '§cAdamantine§r (3 sockets). §eNight Vision§r always, and monsters within 24 blocks that you can §lsee§r glow. 20 FE/s.\n\n§7Socketed Echo Rune§r: they glow even behind walls.',
    'guide.mistech.entry.cloud_broom': 'Cloud Broom', 'guide.mistech.ritual.cloud_broom': 'Enchant the Broom',
    'guide.mistech.cloud_broom.1': '§6Orichalcum§r (6 sockets). §lRight-click§r releases the broom and you mount it.\n\n• Forward/back: fly where you look.\n• Jump: rise.\n• Sneak: get off (and fall slowly).\n\nUses 200 FE per second while mounted; out of energy, it glides down.',
    'guide.mistech.cloud_broom.2': 'Left alone, it floats in place. §7Right-click§r to mount again; §7sneak + right-click§r (or hit it) to pick it up.\n\nSocketed runes: §fAir§r = faster, §eEnergy§r = cheaper, §fCloud§r = cloud trail and longer slow fall, §eLight§r = glows, §cFire§r/§bIce§r = trail.',
    'guide.mistech.entry.shadow_cloak': 'Shadow Cloak', 'guide.mistech.ritual.shadow_cloak': 'Weave the Shadows',
    'guide.mistech.shadow_cloak.1': '§6Orichalcum§r (6 sockets). Night ritual. §7Sneaking still§r for 1 s: you turn §8invisible§r and chasing monsters lose their target. 10 FE/s.\n\n§7Socketed Void Rune§r: 15% per rune (up to 45%) to §5vanish and reappear§r nearby when hurt (5 s cooldown).',
    'guide.mistech.entry.tide_spear': 'Tide Spear', 'guide.mistech.ritual.tide_spear': 'Call the Tides',
    'guide.mistech.tide_spear.1': '§6Orichalcum§r (6 sockets). Rain ritual, on a trident (enchantments carry over). Built-in §lLoyalty III§r: it always returns.\n\n§7Sneaking§r, releasing = §3riptide§r in water, rain or standing on a §fcloud§r — no enchantment needed (400 FE).',
    'guide.mistech.entry.void_magnet': 'Void Magnet', 'guide.mistech.ritual.void_magnet': 'Magnetize the Void',
    'guide.mistech.void_magnet.1': 'Amulet (§lJ§r key). Pulls dropped items within 8 blocks (2 FE per item). Not while sneaking.\n\n§7Right-click§r in hand: on/off.\n§7Sneak + right-click§r with an item in the other hand: add/remove it from the §5void list§r (up to 9) — the magnet deletes it instead of pulling.',
    'guide.mistech.entry.pocket_hourglass': 'Pocket Hourglass', 'guide.mistech.ritual.pocket_hourglass': 'Bind Time',
    'guide.mistech.pocket_hourglass.1': 'Amulet (§lJ§r key). Plants within 5 blocks grow faster and §lfurnaces§r there work twice as fast. 20 FE/s.\n\n§7Right-click§r in hand: on/off.',
    'guide.mistech.entry.spatial_ring': 'Spatial Ring', 'guide.mistech.ritual.spatial_ring': 'Fold Space',
    'guide.mistech.spatial_ring.1': 'Ring (§lJ§r key). Press §lV§r to open the list of §5Spatial Anchors§r you know and travel from anywhere.\n\nInstead of levels, it pays §benergy§r: 5,000 FE (10,000 to another dimension).',
}
for f, d in (('pt_br.json', PT), ('en_us.json', EN)):
    j = json.load(open(LANG + f, encoding='utf-8'))
    j.update(d)
    json.dump(j, open(LANG + f, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
assert set(PT) == set(EN), set(PT) ^ set(EN)
print('ok', len(E), 'entradas', len(PT), 'textos')
