#!/usr/bin/env python3
"""Textos (pt_br/en_us) e página do Grimório dos Engastes de máquina (Fase 2 do plano magia × tech).
Idempotente. Depois rode tools/guide/views.py.
Uso (na pasta do projeto): python3 tools/sockets/gen_data.py"""
import json

LANG = 'src/main/resources/assets/mistech/lang/'
BOOK = 'src/main/resources/assets/mistech/guide/book.json'

EFFECTS_PT = {
    'fire': 'Funde o produto se ele tiver receita de forno (pó vira lingote). Gasta 10% mais energia.',
    'water': 'Máquinas de fluido trabalham 30% mais rápido.',
    'earth': '10% de chance de produto em dobro.',
    'air': 'Entrada e saída automáticas 2× mais rápidas; recolhe itens do chão num raio de 2.',
    'ice': 'Refrigera: as Melhorias de Velocidade param de gastar energia extra.',
    'energy': 'Entrada e saída de energia +50%. Geradores +10%.',
    'light': 'Geradores celestes (Astral, Purificado, Observatório) +15%.',
    'darkness': 'Reator Impuro +15% de energia.',
    'space': 'Manda o produto para o canal Extraplanar com o nome da runa (dê o nome na bigorna).',
    'time': '+25% de velocidade.',
    'magma': 'Geradores de calor (Cinzas, Blaze, Caldeira Ígnea) +25%.',
    'vapor': 'Refrigera como o Gelo e ainda dá +10% de velocidade.',
    'void': 'Com a saída cheia, destrói o excedente em vez de parar.',
    'luck': '5% de chance de produto extra.',
    'arcane': '−20% de energia.',
    'echo': '10% de chance de repetir a operação de graça (15% com a Runa do Tempo).',
    'vigil': 'O comparador dá 15 quando a máquina fica parada mais de 2 segundos.',
    'soul': 'Experiência das receitas em dobro. Gerador de Experiência +15%.',
    'cloud': 'Rega a terra arada da área e dá +1 de alcance.',
    'life': '+30% de velocidade nas máquinas de área.',
}
EFFECTS_EN = {
    'fire': 'Smelts the product if it has a furnace recipe (dust becomes ingot). Uses 10% more energy.',
    'water': 'Fluid machines work 30% faster.',
    'earth': '10% chance of double product.',
    'air': 'Automatic input and output 2× faster; picks up items on the ground within 2 blocks.',
    'ice': 'Cooling: Speed Upgrades stop costing extra energy.',
    'energy': 'Energy input and output +50%. Generators +10%.',
    'light': 'Celestial generators (Astral, Purified, Observatory) +15%.',
    'darkness': 'Impure Reactor +15% energy.',
    'space': 'Sends the product to the Extraplanar channel named like the rune (name it at an anvil).',
    'time': '+25% speed.',
    'magma': 'Heat generators (Ash, Blaze, Igneous Boiler) +25%.',
    'vapor': 'Cools like Ice and also gives +10% speed.',
    'void': 'When the output is full, destroys the excess instead of stopping.',
    'luck': '5% chance of an extra product.',
    'arcane': '−20% energy.',
    'echo': '10% chance to repeat the operation for free (15% with a Time Rune).',
    'vigil': 'The comparator outputs 15 when the machine has been idle for more than 2 seconds.',
    'soul': 'Double experience from recipes. Experience Generator +15%.',
    'cloud': 'Waters the farmland in its area and gives +1 range.',
    'life': '+30% speed for area machines.',
}
CATS_PT = {
    'processing': 'Máquina de processamento: Fogo, Terra, Eco, Sorte, Alma, Espaço, Vazio, Tempo, Arcano, Gelo, Vapor, Ar, Energia, Vigília',
    'generation': 'Gerador: Energia e a runa da família (Magma, Escuridão, Luz ou Alma)',
    'area': 'Máquina de área: Nuvem, Vida, Sorte, Espaço, Vazio, Tempo, Arcano, Gelo, Vapor, Ar, Energia, Vigília',
    'mining': 'Mineradora: Sorte, Tempo, Arcano, Gelo, Vapor, Ar, Energia, Vigília',
    'fluid': 'Máquina de fluido: Água, Tempo, Arcano, Gelo, Vapor, Ar, Energia, Vigília',
    'storage': 'Bateria: Energia',
    'spell': 'Conjurador: Tempo, Arcano, Gelo, Vapor, Ar, Energia, Vigília',
}
CATS_EN = {
    'processing': 'Processing machine: Fire, Earth, Echo, Luck, Soul, Space, Void, Time, Arcane, Ice, Vapor, Air, Energy, Vigil',
    'generation': 'Generator: Energy and the rune of its family (Magma, Darkness, Light or Soul)',
    'area': 'Area machine: Cloud, Life, Luck, Space, Void, Time, Arcane, Ice, Vapor, Air, Energy, Vigil',
    'mining': 'Miner: Luck, Time, Arcane, Ice, Vapor, Air, Energy, Vigil',
    'fluid': 'Fluid machine: Water, Time, Arcane, Ice, Vapor, Air, Energy, Vigil',
    'storage': 'Battery: Energy',
    'spell': 'Conjurer: Time, Arcane, Ice, Vapor, Air, Energy, Vigil',
}

PT = {
    'gui.mistech.socket.empty': 'Engaste vazio',
    'gui.mistech.socket.empty.help': 'Coloque qualquer runa: cada uma tem um efeito nesta máquina.',
    'gui.mistech.socket.cancelled': 'Fogo e Gelo se anulam: nenhum dos dois funciona.',
    'gui.mistech.socket.no_effect': 'Sem efeito nesta máquina.',
    'gui.mistech.socket.uses': 'Usos: %s/%s',
    'info.mistech.sockets': 'Engastes: %s',
    'guide.mistech.entry.machine_sockets': 'Engastes de Máquina',
    'guide.mistech.machine_sockets.1': '§lEngastes§r\nToda máquina com tela de energia tem uma coluna de §5engastes§r à esquerda: as Rúnicas têm 1, as Arcanas e os geradores avançados 2, a Forja Trituradora 3. Qualquer runa entra e dá um efeito universal, além do efeito próprio que a máquina já tem no espaço de runa.\n\nPasse o mouse no engaste para ver o efeito. Runa que não serve naquela máquina fica com a marca cinza.',
    'guide.mistech.machine_sockets.2': '§lDesgaste§r\nCada runa engastada perde §e1 uso a cada 4 minutos§r de máquina trabalhando. Runas seladas no Santuário não se gastam (em âmbar gastam normalmente nas máquinas). A mesma runa em dois engastes não soma.\n\n§lCombinações§r\n§cFogo + Gelo§r se anulam. §eTempo + Eco§r: o Eco sobe para 15%. §5Espaço + Vazio§r: o que não couber no canal é destruído.',
    'guide.mistech.machine_sockets.3': '§cFogo:§r funde o produto (pó → lingote), +10% energia.\n§6Terra:§r 10% de produto em dobro.\n§aSorte:§r 5% de produto extra.\n§3Eco:§r 10% de repetir de graça.\n§bAlma:§r XP das receitas em dobro.\n§eTempo:§r +25% de velocidade.\n§dArcano:§r −20% de energia.',
    'guide.mistech.machine_sockets.4': '§bGelo:§r Melhorias de Velocidade sem custo extra.\n§7Vapor:§r como o Gelo, +10% de velocidade.\n§fAr:§r entrada/saída 2× mais rápidas e recolhe itens do chão.\n§eEnergia:§r entrada/saída de FE +50%, geradores +10%.\n§8Vigília:§r comparador dá 15 com a máquina parada.\n§9Água:§r máquinas de fluido +30%.',
    'guide.mistech.machine_sockets.5': '§5Espaço:§r o produto vai para o canal Extraplanar com o §onome da runa§r (renomeie na bigorna).\n§0Vazio:§r saída cheia destrói o excedente.\n§fNuvem:§r rega a terra arada e +1 de alcance.\n§aVida:§r máquinas de área +30%.\n§6Magma, Escuridão, Luz:§r geradores de calor +25%, Reator Impuro +15%, geradores celestes +15%.',
}
EN = {
    'gui.mistech.socket.empty': 'Empty socket',
    'gui.mistech.socket.empty.help': 'Place any rune: each one has an effect on this machine.',
    'gui.mistech.socket.cancelled': 'Fire and Ice cancel each other: neither works.',
    'gui.mistech.socket.no_effect': 'No effect on this machine.',
    'gui.mistech.socket.uses': 'Uses: %s/%s',
    'info.mistech.sockets': 'Sockets: %s',
    'guide.mistech.entry.machine_sockets': 'Machine Sockets',
    'guide.mistech.machine_sockets.1': '§lSockets§r\nEvery machine with an energy screen has a column of §5sockets§r on the left: Runic ones have 1, Arcane machines and advanced generators 2, the Crusher Forge 3. Any rune fits and gives a universal effect, on top of the machine\'s own rune slot.\n\nHover a socket to see its effect. A rune that does nothing in that machine gets a gray mark.',
    'guide.mistech.machine_sockets.2': '§lWear§r\nEach socketed rune loses §e1 use every 4 minutes§r the machine is working. Runes sealed at the Sanctuary never wear (amber runes wear normally in machines). The same rune in two sockets does not stack.\n\n§lCombinations§r\n§cFire + Ice§r cancel out. §eTime + Echo§r: Echo goes up to 15%. §5Space + Void§r: whatever does not fit in the channel is destroyed.',
    'guide.mistech.machine_sockets.3': '§cFire:§r smelts the product (dust → ingot), +10% energy.\n§6Earth:§r 10% double product.\n§aLuck:§r 5% extra product.\n§3Echo:§r 10% free repeat.\n§bSoul:§r double recipe XP.\n§eTime:§r +25% speed.\n§dArcane:§r −20% energy.',
    'guide.mistech.machine_sockets.4': '§bIce:§r Speed Upgrades cost no extra energy.\n§7Vapor:§r like Ice, +10% speed.\n§fAir:§r input/output 2× faster and picks up ground items.\n§eEnergy:§r FE input/output +50%, generators +10%.\n§8Vigil:§r comparator outputs 15 when the machine stops.\n§9Water:§r fluid machines +30%.',
    'guide.mistech.machine_sockets.5': '§5Space:§r the product goes to the Extraplanar channel with the §orune\'s name§r (rename it at an anvil).\n§0Void:§r a full output destroys the excess.\n§fCloud:§r waters farmland and +1 range.\n§aLife:§r area machines +30%.\n§6Magma, Darkness, Light:§r heat generators +25%, Impure Reactor +15%, celestial generators +15%.',
}
for k, v in EFFECTS_PT.items():
    PT['socket.mistech.' + k] = v
for k, v in EFFECTS_EN.items():
    EN['socket.mistech.' + k] = v
for k, v in CATS_PT.items():
    PT['gui.mistech.socket.category.' + k] = v
for k, v in CATS_EN.items():
    EN['gui.mistech.socket.category.' + k] = v

for name, d in (('pt_br', PT), ('en_us', EN)):
    path = LANG + name + '.json'
    data = json.load(open(path, encoding='utf-8'))
    data.update(d)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write('\n')

ENTRY = {
    'id': 'machine_sockets', 'icon': 'mistech:time_rune', 'name': 'guide.mistech.entry.machine_sockets',
    'deps': ['machine_config'],
    'task': {'type': 'item', 'items': ['mistech:fire_rune', 'mistech:earth_rune', 'mistech:time_rune', 'mistech:air_rune']},
    'x': 6.4, 'y': 4.0,
    'pages': [{'type': 'text', 'text': 'guide.mistech.machine_sockets.%d' % i} for i in range(1, 6)],
}
book = json.load(open(BOOK, encoding='utf-8'))
cat = next(c for c in book['categories'] if c['id'] == 'machines2')
cat['entries'] = [e for e in cat['entries'] if e['id'] != ENTRY['id']] + [ENTRY]
with open(BOOK, 'w', encoding='utf-8') as f:
    json.dump(book, f, ensure_ascii=False, indent=2)
print('ok: %d chaves' % len(PT))
