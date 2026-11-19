"""Temas das telas do Mistech: cada tela com o visual do bloco que a abre.
bg/frame: textura vanilla + degradê (escuro -> claro) para onde a luminância dela é mapeada.
slot: (miolo, borda escura, borda clara). accent: degradê dos enfeites (setas, molduras internas).
label/text: cores dos textos; glow: contorno pulsando; motes: luzinhas subindo (vazio = nenhuma)."""
T = {}
def theme(names, **kw):
    for n in names:
        T[n] = kw

theme(['spatial_storage'], bg=('barrel_side', ['1c120a', '3a2614', '563a20']), frame=('iron_block', ['16161a', '4e4e58', '9a9aa6']),
      corner='9b5fd6', slot=('1e140b', '0c0805', '6e4c2c'), accent=['5a3c20', 'b07a3c', 'f0c070'],
      label='f3d9a4', text='e0c9a6', glow='8a5a2a', motes=['9b6fe0', 'c49cf5', 'f0c870'], hoops=True)
theme(['rune_anvil'], bg=('anvil', ['111115', '26262d', '3e3e48']), frame=('iron_block', ['0b0b0e', '3c3c46', '7e7e8c']),
      corner='e8c46a', slot=('141419', '07070a', '585864'), accent=['6b3fa8', 'b78cf0', 'e8c46a'],
      label='e8c46a', text='c8c8d8', glow='b78cf0', motes=['b78cf0', 'e8c46a', 'ff9a4a'], rivets=True)
theme(['runic_furnace'], bg=('cobblestone', ['141210', '2c2824', '46403a']), frame=('stone_bricks', ['0e0c0a', '4a4038', '8a7a68']),
      corner='ff7a2a', slot=('181411', '0a0806', '5c4c40'), accent=['7a2a10', 'ff7a2a', 'ffd27a'],
      label='ffcf8a', text='e6d2bc', glow='ff6a1a', motes=['ff7a2a', 'ffb04a', 'ffe07a'])
theme(['writing_table'], bg=('dark_oak_planks', ['150d07', '2c1c10', '46301c']), frame=('stripped_dark_oak_log', ['0c0704', '4a3220', '8a6440']),
      corner='eae6dc', slot=('1a110a', '0b0704', '6b4c30'), accent=['8a8478', 'd8d2c4', 'ffffff'],
      label='f0e2c0', text='d8c8a8', glow='d8d2c4', motes=['ffffff', 'e0dcd2', 'bdb6a8'])
theme(['incense_holder'], bg=('packed_mud', ['16110d', '2e241c', '463828']), frame=('gold_block', ['1a1206', '6a4c18', 'c8a050']),
      corner='a8a2b0', slot=('17110c', '0a0705', '5e4a36'), accent=['5a5560', 'a8a2b0', 'e8e2f0'],
      label='f0d890', text='d8ccc0', glow='c8a050', motes=['d8d2dc', 'a8a2b0', 'f0d890'])
theme(['condenser'], bg=('packed_ice', ['081420', '12283c', '1e3e58']), frame=('packed_ice', ['060c14', '4a6a86', 'a8d8f0']),
      corner='9fe3ff', slot=('0a1622', '040a10', '3a6080'), accent=['2a6a9a', '7fd0ff', 'e0f8ff'],
      label='c8f0ff', text='a8d0e8', glow='7fd0ff', motes=['ffffff', '9fe3ff', '7fd0ff'])
theme(['accessories'], bg=('red_wool', ['1a0608', '36101a', '521a28']), frame=('gold_block', ['1a1206', '7a5a20', 'e0b860']),
      corner='3ccf80', slot=('1e080c', '0c0305', '6a2a38'), accent=['7a5a20', 'e0b860', 'fff0b0'],
      label='f0d080', text='e8c8c8', glow='e0b860', motes=['fff0b0', 'e0b860', '3ccf80'])
theme(['rune_confector'], bg=('spruce_planks', ['130d08', '2a1d12', '3e2c1c']), frame=('copper_block', ['140a06', '7a4228', 'd88a5a']),
      corner='b78cf0', slot=('170f09', '0a0604', '5e4430'), accent=['6b3fa8', 'b78cf0', 'f0d8ff'],
      label='f0c890', text='dcc8b0', glow='d88a5a', motes=['b78cf0', 'd88a5a', 'f0c870'], hoops=True)
theme(['rune_roller'], bg=('jungle_planks', ['10140a', '223018', '344a24']), frame=('stripped_jungle_log', ['0a0c06', '3a4a20', '7a9048']),
      corner='7ad04a', slot=('101608', '060a04', '465c2c'), accent=['3a6a20', '7ad04a', 'd8f0a0'],
      label='e0f0b0', text='c8d8b0', glow='7ad04a', motes=['d8d2dc', '7ad04a', 'b8e080'])
theme(['runic_crusher'], bg=('deepslate', ['101012', '222226', '36363c']), frame=('chiseled_deepslate', ['08080a', '3a3a42', '7a7a86']),
      corner='b78cf0', slot=('121214', '060608', '4e4e58'), accent=['4a4a58', '9a9ab0', 'e0e0f0'],
      label='e0dcf0', text='c0bcd0', glow='9a9ab0', motes=['9a9ab0', 'b78cf0', 'e0e0f0'], rivets=True)
theme(['ash_generator'], bg=('tuff', ['121110', '2a2826', '403c38']), frame=('polished_blackstone', ['060506', '3a3438', '6a6068']),
      corner='ff7a2a', slot=('141312', '070606', '4e4a46'), accent=['7a2a10', 'ff8a3a', 'ffd27a'],
      label='ffd8a8', text='d8ccc4', glow='ff7a2a', motes=['a8a4a0', 'ff7a2a', 'd8d2cc'])
theme(['astral_generator'], bg=('lapis_block', ['060a1e', '0e1838', '182a58']), frame=('gold_block', ['1a1206', '7a5a20', 'e8c46a']),
      corner='fff6c0', slot=('08102a', '030614', '2e4a88'), accent=['3a5ab0', '8ab0ff', 'fff6c0'],
      label='fff0b0', text='c8d4f0', glow='8ab0ff', motes=['ffffff', 'fff6c0', '8ab0ff'])
theme(['runic_battery'], bg=('cut_copper', ['120a06', '2a1810', '42281a']), frame=('copper_block', ['140a06', '8a4a2a', 'e89a62']),
      corner='ffe14a', slot=('140c07', '080402', '5c3a26'), accent=['8a6a10', 'ffe14a', 'fff8c0'],
      label='ffe8a0', text='e8d0b8', glow='ffe14a', motes=['ffe14a', 'fff8c0', 'e89a62'], rivets=True)
theme(['advanced_runic_battery'], bg=('oxidized_cut_copper', ['06120e', '10261e', '1a3a2e']), frame=('oxidized_copper', ['040a08', '2a6a58', '70c8a8']),
      corner='b05cff', slot=('08140f', '030806', '2a5a48'), accent=['3a2a7a', 'b05cff', 'f0d8ff'],
      label='c8fff0', text='b0d8cc', glow='b05cff', motes=['b05cff', '70c8a8', 'f0d8ff'], rivets=True)
theme(['gnome_bicycle'], bg=('birch_planks', ['16120c', '2e2618', '4a3c26']), frame=('stripped_birch_log', ['1a0605', '8a2a20', 'e06048']),
      corner='f0c870', slot=('18130c', '0a0805', '5e4c32'), accent=['8a2a20', 'e06048', 'ffd0a0'],
      label='ffe0b0', text='e8d8c0', glow='e06048', motes=['f0c870', 'e06048', 'ffe0b0'])
theme(['runic_harvester'], bg=('hay_block_side', ['181206', '34280c', '4e3c14']), frame=('stripped_oak_log', ['0e0904', '5a4224', 'a88050']),
      corner='7ad04a', slot=('1a1407', '0a0803', '5e4a1c'), accent=['5a6a20', 'c8d04a', 'fff0a0'],
      label='fff0b0', text='e8dcb0', glow='c8d04a', motes=['fff0a0', 'c8d04a', 'e8c870'])
theme(['runic_planter'], bg=('rooted_dirt', ['140e08', '2a1e12', '40301c']), frame=('moss_block', ['08100a', '3a5a28', '7aa850']),
      corner='f08ac0', slot=('160f09', '090604', '544030'), accent=['3a6a20', '7ad04a', 'd8f0a0'],
      label='d8f0b0', text='d0c8b0', glow='7aa850', motes=['b8e080', '7ad04a', 'f08ac0'])
theme(['miner'], bg=('deepslate_bricks', ['0e0d0e', '222024', '36323a']), frame=('iron_block', ['0a0a0c', '4a4a54', '9a9aa8']),
      corner='f0c040', slot=('100f11', '060506', '4a464e'), accent=['5a4a2a', 'c8a050', 'ffe0a0'],
      label='ffe0a0', text='d0ccd8', glow='c8a050', motes=['a89880', 'c8a050', '8a8a96'], rivets=True)
theme(['blaze_generator'], bg=('nether_bricks', ['140606', '2a0c0c', '421414']), frame=('blackstone', ['0a0606', '5a2a1a', 'e07a2a']),
      corner='ffcf4a', slot=('160707', '0a0303', '5a2020'), accent=['8a2a0a', 'ff8a2a', 'ffe07a'],
      label='ffd27a', text='e8c8b8', glow='ff8a2a', motes=['ff8a2a', 'ffcf4a', 'ff5a1a'])
# telas claras (Pureza): fundo claro, textos escuros, enfeites escuros
theme(['distiller'], bg=('calcite', ['a8a49c', 'c8c4bc', 'e2ded6']), frame=('quartz_pillar', ['2a2620', 'a8a090', 'f8f4ec']),
      corner='5aa8e0', slot=('5a564e', '2e2b26', 'f4f0e8'), accent=['2a5a88', '4a88c0', '8ac0e8'], accent_dark=True,
      label='3a3028', text='4a4036', glow='ffffff', motes=['ffffff', '8ac0e8', 'f0e0a0'], light=True)
theme(['pure_generator'], bg=('quartz_block_side', ['b8ae90', 'd4ccae', 'ece6d0']), frame=('gold_block', ['3a2a0a', 'b08a30', 'fff0b0']),
      corner='5ac8ff', slot=('5e5644', '302a1e', 'fffaf0'), accent=['5a4010', '8a6a20', 'b08a30'], accent_dark=True,
      label='4a3a10', text='5a4a20', glow='fff0b0', motes=['fff0b0', 'ffffff', '5ac8ff'], light=True)
# ----- geradores avançados (08/10/2026) -----
theme(['impure_reactor'], bg=('crying_obsidian', ['0c0614', '1e0e30', '34184e']), frame=('polished_deepslate', ['050407', '3a3046', '8a6ab0']),
      corner='d06aff', slot=('120a1c', '06030a', '4e3070'), accent=['4a1a7a', 'b05cff', 'f0c8ff'],
      label='e8c8ff', text='c8b0e0', glow='b05cff', motes=['b05cff', '8a3ad8', 'f0c8ff'], rivets=True)
theme(['stellar_observatory'], bg=('lapis_block', ['04061a', '0a1030', '141e4a']), frame=('gold_block', ['1a1206', '8a6a24', 'fff0b0']),
      corner='ffffff', slot=('060a22', '020410', '2a3e7a'), accent=['3a4aa0', 'a8c0ff', 'ffffff'],
      label='fff6d0', text='c8d0f0', glow='c0d0ff', motes=['ffffff', 'fff6c0', 'a8c0ff'])
theme(['igneous_boiler'], bg=('magma', ['160604', '34100a', '5a1e0e']), frame=('polished_blackstone_bricks', ['080505', '4a2a22', 'ff9a3a']),
      corner='ffe07a', slot=('1a0806', '0a0303', '6a2414'), accent=['9a2a0a', 'ff9a3a', 'fff0a0'],
      label='ffe0a0', text='f0c8b0', glow='ff7a1a', motes=['ff9a3a', 'ffe07a', 'ff5a1a'], rivets=True)
theme(['experience_generator'], bg=('sculk', ['020a0a', '06181a', '0c2a2c']), frame=('emerald_block', ['04140a', '1a7a3a', '8af0a0']),
      corner='d8ff5a', slot=('041012', '010607', '1a4a4a'), accent=['3a8a1a', 'a8f040', 'f0ffb0'],
      label='d8ffa0', text='b0e0c8', glow='8af040', motes=['d8ff5a', '8af040', '3ad8c8'])
# ----- Conjurador Rúnico (25/10/2026) -----
theme(['runic_conjurer'], bg=('polished_blackstone', ['0e0a14', '1e1628', '30243e']), frame=('amethyst_block', ['140a20', '5a3a8a', 'c8a0f0']),
      corner='f0c870', slot=('120c1a', '06040a', '4a3a60'), accent=['5a2a9a', 'b78cf0', 'f0d8ff'],
      label='f0d8ff', text='cfc0e8', glow='c08cff', motes=['c08cff', '7fd0ff', 'f0c870'])
# ----- cadeia de minério (25/10/2026) -----
theme(['essence_washer'], bg=('polished_deepslate', ['0c0a10', '1c1824', '2c2636']), frame=('cut_copper', ['140a06', '8a4a2a', 'e89a62']),
      corner='b05cff', slot=('120e18', '06050a', '4a3a5a'), accent=['5a2a8a', 'b05cff', 'f0d0ff'],
      label='f0d8ff', text='d0c0e0', glow='b05cff', motes=['b05cff', '8a3ad8', 'e89a62'], rivets=True)
theme(['runic_crystallizer'], bg=('quartz_block_side', ['b8ae90', 'd4ccae', 'ece6d0']), frame=('gold_block', ['3a2a0a', 'b08a30', 'fff0b0']),
      corner='fff6c0', slot=('5e5644', '302a1e', 'fffaf0'), accent=['5a4010', '8a6a20', 'b08a30'], accent_dark=True,
      label='4a3a10', text='5a4a20', glow='fff0b0', motes=['fff6c0', 'ffffff', 'f0c870'], light=True)
theme(['astral_prism'], bg=('lapis_block', ['04061a', '0a1030', '141e4a']), frame=('gold_block', ['1a1206', '8a6a24', 'fff0b0']),
      corner='ffffff', slot=('060a22', '020410', '2a3e7a'), accent=['3a4aa0', 'a8c0ff', 'ffffff'],
      label='fff6d0', text='c8d0f0', glow='a8c0ff', motes=['ffffff', 'fff6c0', 'a8c0ff'])
# ----- Mácula do lugar (26/10/2026) -----
theme(['rue_filter'], bg=('mud_bricks', ['141008', '2a2014', '3e3020']), frame=('cut_copper', ['140a06', '8a4a2a', 'e89a62']),
      corner='8ad870', slot=('16120a', '080604', '4a5a36'), accent=['2a5a20', '6fcf5a', 'd0f8c0'],
      label='e0f8d0', text='c8dcb8', glow='6fcf5a', motes=['6fcf5a', 'a8f090', 'e89a62'], rivets=True)
# ----- Poço de Mana e Sangria Arcana (26/10/2026) -----
theme(['mana_well'], bg=('quartz_block_side', ['b4b0c0', 'd2cede', 'ece8f4']), frame=('gold_block', ['3a2a0a', 'b08a30', 'fff0b0']),
      corner='a8c8ff', slot=('5a5870', '2c2a3a', 'f8f6ff'), accent=['1e2a6a', '3a5ab0', '6a8ad8'], accent_dark=True,
      label='1e2a6a', text='2e3a6a', glow='6a8ad8', motes=['a8c8ff', 'ffffff', 'f0c870'], light=True)
theme(['mana_siphon'], bg=('polished_blackstone', ['0c0a10', '1a1820', '2a2632']), frame=('cut_copper', ['140a06', '8a4a2a', 'e89a62']),
      corner='6ed2ff', slot=('120e18', '06050a', '3a4a5a'), accent=['1e4a6a', '6ed2ff', 'd8f4ff'],
      label='d8f4ff', text='b8d0e0', glow='6ed2ff', motes=['6ed2ff', 'a8c8ff', 'e89a62'], rivets=True)
# ----- máquinas de folclore (26/10/2026) -----
theme(['saci_windmill'], bg=('spruce_planks', ['1a1008', '2e2012', '42301c']), frame=('stripped_spruce_log', ['1a0e06', '6a4a2a', 'c89a62']),
      corner='e04040', slot=('1a120a', '0a0604', '5a4030'), accent=['7a1a1a', 'e04040', 'ffd0b0'],
      label='ffe8d0', text='e0c8b0', glow='e04040', motes=['e04040', 'ffd0b0', 'c8c8c8'], rivets=True)
theme(['iara_pump'], bg=('prismarine_bricks', ['06141a', '0e262e', '183a42']), frame=('copper_block', ['140a06', '8a4a2a', 'e89a62']),
      corner='6ef0e0', slot=('0a1a1e', '04090c', '2e5a5a'), accent=['1a5a5a', '6ef0e0', 'd8fff8'],
      label='d8fff8', text='b0e0e0', glow='6ef0e0', motes=['6ef0e0', 'a8d8ff', 'ffffff'], rivets=True)
theme(['curupira_stump'], bg=('oak_planks', ['140e06', '261a0e', '3a2a16']), frame=('oak_log', ['140c06', '5a4428', 'a88a5a']),
      corner='ff7a2a', slot=('1a120a', '0a0604', '4a5a2a'), accent=['2a5a1a', '8ad860', 'ff7a2a'],
      label='f0f8d8', text='d8e0b8', glow='ff7a2a', motes=['8ad860', 'ff7a2a', 'f0c870'], rivets=True)
theme(['potion_still'], bg=('bricks', ['160a08', '2a1410', '3e2018']), frame=('copper_block', ['140a06', '8a4a2a', 'e89a62']),
      corner='c070ff', slot=('1a0e0c', '0a0504', '5a3a4a'), accent=['5a2a8a', 'c070ff', 'f0d0ff'],
      label='f8e0f0', text='e0c8d8', glow='c070ff', motes=['c070ff', 'e89a62', '70d070'], rivets=True)
theme(['scroll_printer'], bg=('dark_oak_planks', ['0e0804', '1c120a', '2a1c10']), frame=('lapis_block', ['0a1030', '2a4aa0', '8ab0ff']),
      corner='f0e0b0', slot=('140e08', '060402', '4a4a6a'), accent=['2a4aa0', '8ab0ff', 'f0f4ff'],
      label='f0f4ff', text='d0d8f0', glow='8ab0ff', motes=['8ab0ff', 'f0e0b0', 'ffffff'])
# Fase 9 (endgame)
theme(['star_forge'], bg=('polished_blackstone_bricks', ['06061a', '10123a', '1c2058']), frame=('gold_block', ['1a1206', '7a5a20', 'e8c46a']),
      corner='9fd8ff', slot=('08102a', '030614', '2e4a88'), accent=['2a3a9a', '9fd8ff', 'f4fbff'],
      label='f4fbff', text='c8d8f0', glow='9fd8ff', motes=['ffffff', '9fd8ff', 'e8c46a'])
theme(['dream_reactor'], bg=('amethyst_block', ['10081c', '1e1034', '30184e']), frame=('purpur_block', ['140a1a', '7a4a8a', 'f0c8ff']),
      corner='ff7ad8', slot=('140a20', '06030c', '5a3a7a'), accent=['5a2a8a', 'ff7ad8', 'ffe0f8'],
      label='ffe0f8', text='e0c8f0', glow='ff7ad8', motes=['ff7ad8', 'f0c8ff', 'ffffff'])
theme(['celestial_battery'], bg=('lapis_block', ['060a1e', '0e1838', '182a58']), frame=('gold_block', ['1a1206', '7a5a20', 'e8c46a']),
      corner='9fd8ff', slot=('08102a', '030614', '2e4a88'), accent=['2a3a9a', '9fd8ff', 'f4fbff'],
      label='f4fbff', text='c8d8f0', glow='9fd8ff', motes=['ffffff', '9fd8ff', 'fff6c0'], rivets=True)
theme(['celestial_conjurer'], bg=('lapis_block', ['080a20', '121a40', '1e2a60']), frame=('polished_blackstone', ['050407', '3a3a6a', '9fd8ff']),
      corner='e8c46a', slot=('0a1028', '040612', '3a4a8a'), accent=['2a3a9a', '9fd8ff', 'f4fbff'],
      label='f4fbff', text='c8d8f0', glow='9fd8ff', motes=['9fd8ff', 'ffffff', 'e8c46a'])
theme(['boitata_pump'], bg=('nether_bricks', ['120606', '2a0c0a', '421410']), frame=('gold_block', ['1a1206', '7a5a20', 'e8c46a']),
      corner='ff7a2a', slot=('180806', '080302', '5a2a1a'), accent=['7a2a10', 'ff7a2a', 'ffd27a'],
      label='ffd8a8', text='e8c8b8', glow='ff7a2a', motes=['ff7a2a', 'ffb04a', 'ffe07a'])
