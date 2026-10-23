"""Textos (pt_br e en_us) da leva de Qualidade de Vida. Uso: python3 tools/qol/lang.py (mescla nas lang)."""
import json

LANG = 'src/main/resources/assets/mistech/lang/'

T = {
    # ----- itens e blocos
    'item.mistech.runic_plate': ('Placa Rúnica', 'Runic Plate'),
    'item.mistech.speed_upgrade': ('Melhoria de Velocidade', 'Speed Upgrade'),
    'item.mistech.efficiency_upgrade': ('Melhoria de Eficiência', 'Efficiency Upgrade'),
    'item.mistech.range_upgrade': ('Melhoria de Alcance', 'Range Upgrade'),
    'item.mistech.upgrade.speed.desc': ('+50%% de velocidade por melhoria (gasta um pouco mais por item)', '+50%% speed per upgrade (uses a bit more per item)'),
    'item.mistech.upgrade.efficiency.desc': ('-15%% de energia por melhoria', '-15%% energy per upgrade'),
    'item.mistech.upgrade.range.desc': ('+1 de raio nas máquinas de área (Colheitadeira, Plantadeira)', '+1 radius on area machines (Harvester, Planter)'),
    'item.mistech.upgrade.how': ('Botão direito numa máquina para instalar (máx. %s)', 'Right-click a machine to install (max %s)'),
    'block.mistech.arcane_furnace': ('Fornalha Arcana', 'Arcane Furnace'),
    'block.mistech.arcane_extractor': ('Extrator Arcano', 'Arcane Extractor'),
    'block.mistech.arcane_compressor': ('Compressor Arcano', 'Arcane Compressor'),
    'container.mistech.arcane_furnace': ('Fornalha Arcana', 'Arcane Furnace'),
    'container.mistech.arcane_extractor': ('Extrator Arcano', 'Arcane Extractor'),
    'container.mistech.arcane_compressor': ('Compressor Arcano', 'Arcane Compressor'),
    'item.mistech.scroll_case': ('Porta-Pergaminhos', 'Scroll Case'),
    'item.mistech.scroll_case.named': ('%s (%s)', '%s (%s)'),
    'item.mistech.scroll_case.help': ('Agachado + botão direito: abrir · Z: trocar de runa', 'Sneak + right-click: open · Z: switch rune'),
    'key.mistech.scroll_case_cycle': ('Trocar runa do Porta-Pergaminhos', 'Switch Scroll Case rune'),
    'message.mistech.scroll_case.empty': ('O Porta-Pergaminhos está vazio', 'The Scroll Case is empty'),
    'message.mistech.scroll_case.selected': ('Runa: %s (%s usos)', 'Rune: %s (%s uses)'),

    # ----- melhorias
    'message.mistech.upgrade.not_accepted': ('Esta máquina não aceita esta melhoria', 'This machine does not accept this upgrade'),
    'message.mistech.upgrade.full': ('Já está no máximo (%s)', 'Already at the maximum (%s)'),
    'message.mistech.upgrade.installed': ('%s instalada (%s/%s)', '%s installed (%s/%s)'),

    # ----- tela das máquinas
    'gui.mistech.config.redstone': ('Controle por redstone', 'Redstone control'),
    'gui.mistech.config.redstone.ignore': ('Ignorar redstone (sempre ligada)', 'Ignore redstone (always on)'),
    'gui.mistech.config.redstone.high': ('Só funciona COM sinal', 'Only works WITH a signal'),
    'gui.mistech.config.redstone.low': ('Só funciona SEM sinal', 'Only works WITHOUT a signal'),
    'gui.mistech.config.click_cycle': ('Clique para trocar', 'Click to change'),
    'gui.mistech.config.sides': ('Lados', 'Sides'),
    'gui.mistech.config.sides.help': ('O que cada face faz com funis, canos e blocos encostados', 'What each face does with hoppers, pipes and touching blocks'),
    'gui.mistech.config.upgrades': ('Melhorias', 'Upgrades'),
    'gui.mistech.config.upgrades.help': ('Clique numa melhoria para tirar 1', 'Click an upgrade to take 1 out'),
    'gui.mistech.config.upgrades.remove': ('Clique para tirar 1', 'Click to take 1 out'),
    'gui.mistech.config.auto_out': ('Saída auto.', 'Auto output'),
    'gui.mistech.config.auto_in': ('Entrada auto.', 'Auto input'),
    'gui.mistech.config.auto_out.help': ('Empurra a saída para baús e máquinas encostados nas faces de saída',
                                         'Pushes output into chests and machines touching the output faces'),
    'gui.mistech.config.auto_in.help': ('Puxa itens de baús e máquinas encostados nas faces de entrada',
                                        'Pulls items from chests and machines touching the input faces'),
    'gui.mistech.config.mode.none': ('Desligada', 'Disabled'),
    'gui.mistech.config.mode.input': ('Entrada', 'Input'),
    'gui.mistech.config.mode.output': ('Saída', 'Output'),
    'gui.mistech.config.mode.both': ('Entrada e saída', 'Input and output'),
    'gui.mistech.config.face.top': ('Cima', 'Top'),
    'gui.mistech.config.face.bottom': ('Baixo', 'Bottom'),
    'gui.mistech.config.face.front': ('Frente', 'Front'),
    'gui.mistech.config.face.back': ('Trás', 'Back'),
    'gui.mistech.config.face.left': ('Esquerda', 'Left'),
    'gui.mistech.config.face.right': ('Direita', 'Right'),
    'gui.mistech.config.face_short.top': ('C', 'T'),
    'gui.mistech.config.face_short.bottom': ('B', 'B'),
    'gui.mistech.config.face_short.front': ('F', 'F'),
    'gui.mistech.config.face_short.back': ('T', 'K'),
    'gui.mistech.config.face_short.left': ('E', 'L'),
    'gui.mistech.config.face_short.right': ('D', 'R'),

    # ----- Jade / informações
    'config.jade.plugin_mistech.info': ('Mistech: informações das máquinas', 'Mistech: machine info'),
    'info.mistech.progress': ('Progresso: %s%%', 'Progress: %s%%'),
    'info.mistech.energy': ('Energia: %s / %s FE', 'Energy: %s / %s FE'),
    'info.mistech.redstone_off': ('Desligada pela redstone', 'Turned off by redstone'),
    'info.mistech.upgrade.speed': ('Velocidade: %s', 'Speed: %s'),
    'info.mistech.upgrade.efficiency': ('Eficiência: %s', 'Efficiency: %s'),
    'info.mistech.upgrade.range': ('Alcance: %s', 'Range: %s'),
    'info.mistech.lane': ('Linha %s: %s%%', 'Lane %s: %s%%'),
    'info.mistech.tank': ('%s: %s / %s mB', '%s: %s / %s mB'),
    'info.mistech.incense.none': ('Sem incenso aceso', 'No incense burning'),
    'info.mistech.incense.burning': ('%s: %ss (raio %s)', '%s: %ss (radius %s)'),
    'info.mistech.incense.stock': ('Estoque: %s', 'Stock: %s'),
    'info.mistech.extractor.level': ('Essência: %s / %s', 'Essence: %s / %s'),
    'info.mistech.compressor.needs_incense': ('Parado: precisa de incenso aceso por perto', 'Paused: needs lit incense nearby'),
    'info.mistech.compressor.empty': ('Vazio (frasco ou balde de essência)', 'Empty (essence bottle or bucket)'),
    'info.mistech.compressor.reserve': ('Reserva: +%s frascos', 'Reserve: +%s bottles'),
    'message.mistech.compressor.full': ('Reserva cheia (%s/%s)', 'Reserve full (%s/%s)'),
    'info.mistech.stored_xp': ('Experiência guardada: %s', 'Stored experience: %s'),
    'info.mistech.altar.tier': ('Tier %s', 'Tier %s'),
    'info.mistech.altar.essence': ('Essência: %s / %s', 'Essence: %s / %s'),

    # ----- Pedra Guia: holograma e alcance
    'multiblock.mistech.astrology': ('Campo de Astrologia', 'Astrology Field'),
    'multiblock.mistech.tarot': ('Círculo de Tarologia', 'Tarot Circle'),
    'multiblock.mistech.confector_right': ('Confector de Runas (barril à direita)', 'Rune Confector (barrel on the right)'),
    'multiblock.mistech.confector_left': ('Confector de Runas (barril à esquerda)', 'Rune Confector (barrel on the left)'),
    'multiblock.mistech.astral_link': ('Pilares de Conexão Astral', 'Astral Connection Pillars'),
    'multiblock.mistech.altar': ('Círculo do Altar - Tier %s', 'Altar Circle - Tier %s'),
    'multiblock.mistech.optional': ('(opcional)', '(optional)'),
    'multiblock.mistech.piece.magic_incense_holder': ('Porta-Incenso com Incenso Mágico aceso', 'Incense Holder with lit Magic Incense'),
    'multiblock.mistech.piece.lit_brazier': ('Braseiro de Oricalco aceso', 'Lit Orichalcum Brazier'),
    'multiblock.mistech.piece.water_source': ('Fonte de água', 'Water source'),
    'message.mistech.projection.show': ('Holograma: %s - %s', 'Hologram: %s - %s'),
    'message.mistech.projection.complete': ('completo!', 'complete!'),
    'message.mistech.projection.missing': ('faltam %s', '%s missing'),
    'message.mistech.projection.cycle': ('  (%s/%s, clique de novo para trocar)', '  (%s/%s, click again to switch)'),
    'message.mistech.projection.off': ('Holograma desligado', 'Hologram off'),
    'gui.mistech.range.label': ('%s: raio %s', '%s: radius %s'),
    'gui.mistech.range.area': ('%s: área %s×%s', '%s: area %s×%s'),
    'gui.mistech.range.unlit': ('%s (apagado)', '%s (unlit)'),
    'gui.mistech.range.silver': ('Prata: lobisomens fogem a %s, não nascem a %s', 'Silver: werewolves flee within %s, none spawn within %s'),
    'gui.mistech.range.battery': ('Suga geradores a %s, energiza máquinas a %s', 'Drains generators within %s, powers machines within %s'),
    'gui.mistech.range.sanctuary': ('Pureza: nada impuro a %s blocos', 'Purity: nothing impure within %s blocks'),
    'gui.mistech.range.compressor': ('Incenso na mão a até %s blocos', 'Incense in hand within %s blocks'),
    'gui.mistech.range.astral_generator': ('Mesa de Astrologia a até %s blocos', 'Astrology Table within %s blocks'),

    # ----- Grimório: busca
    'guide.mistech.search': ('Buscar', 'Search'),
    'guide.mistech.search.help': ('Procura em todas as entradas (Ctrl+F). Enter abre a primeira; Esc fecha.',
                                  'Searches every entry (Ctrl+F). Enter opens the first one; Esc closes.'),
    'guide.mistech.search.hint': ('Digite para buscar...', 'Type to search...'),
    'guide.mistech.search.none': ('Nada encontrado (entradas trancadas não aparecem)', 'Nothing found (locked entries are hidden)'),

    # ----- Grimório: entradas
    'guide.mistech.welcome.search': (
        'Dica: a §6luneta§r no topo do mapa (ou §6Ctrl+F§r) busca em todas as entradas que você já liberou.',
        'Tip: the §6spyglass§r at the top of the map (or §6Ctrl+F§r) searches every entry you have unlocked.'),
    'guide.mistech.entry.scroll_case': ('Porta-Pergaminhos', 'Scroll Case'),
    'guide.mistech.scroll_case.1': (
        'Um estojo de couro para as suas runas: guarda até §59 runas§r.\n\n§7Agachado + botão direito§r abre o estojo. §7Botão direito§r usa a runa escolhida - no ar, num bloco ou numa criatura - como se ela estivesse na sua mão.',
        'A leather case for your runes: it holds up to §59 runes§r.\n\n§7Sneak + right-click§r opens it. §7Right-click§r uses the chosen rune - in the air, on a block or on a creature - as if it were in your hand.'),
    'guide.mistech.scroll_case.2': (
        'Troque a runa com a tecla §6Z§r (agachado = anterior) ou agachado + §6roda do mouse§r. O nome do estojo mostra a runa escolhida e a barra mostra os usos dela.\n\nCom a §cRuna de Fogo§r escolhida, o estojo na outra mão acende incenso, charuto e baseado.',
        'Switch runes with §6Z§r (sneak = previous) or sneak + §6mouse wheel§r. The case name shows the chosen rune and the bar shows its uses.\n\nWith the §cFire Rune§r chosen, the case in your other hand lights incense, cigars and joints.'),
    'guide.mistech.entry.guide_stone_projection': ('Holograma e Alcance', 'Hologram and Range'),
    'guide.mistech.guide_stone_projection.1': (
        '§7Agachado + botão direito§r com a Pedra Guia num bloco central mostra o §5holograma§r do multibloco: Mesa de Escrita, Mesa de Astrologia, Altar Impuro, Fonte do Santuário ou Bloco de Magia Impura.',
        '§7Sneak + right-click§r with the Guide Stone on a central block shows the multiblock §5hologram§r: Writing Table, Astrology Table, Impure Altar, Sanctuary Font or Impure Magic Block.'),
    'guide.mistech.guide_stone_projection.2': (
        'Clique de novo para ver a próxima planta (ex.: a Mesa de Escrita vira Astrologia, Tarologia ou Confector; o Altar mostra o tier atual e o próximo).\n\n§9Azul§r = falta, §cvermelho§r = bloco errado, §averde§r = certo, §6dourado§r = opcional. Agachado + botão direito no ar desliga.',
        'Click again for the next blueprint (e.g. the Writing Table becomes Astrology, Tarot or Confector; the Altar shows the current tier and the next).\n\n§9Blue§r = missing, §cred§r = wrong block, §agreen§r = right, §6gold§r = optional. Sneak + right-click in the air turns it off.'),
    'guide.mistech.guide_stone_projection.3': (
        'Segurando a Pedra Guia §7agachado§r, você vê o §5alcance§r dos blocos por perto: Porta-Incenso, Ponto Arcano, Colheitadeira, Plantadeira, Gnomo Engarrafado, Mandrágora, Bateria Aprimorada, Santuário e outros.',
        'Holding the Guide Stone while §7sneaking§r shows the §5range§r of nearby blocks: Incense Holder, Arcane Node, Harvester, Planter, Bottled Gnome, Mandrake, Advanced Battery, Sanctuary and more.'),
    'guide.mistech.entry.machine_config': ('Configurar Máquinas', 'Machine Settings'),
    'guide.mistech.machine_config.1': (
        'As máquinas com barra de energia têm §5abas§r no lado direito da tela.\n\n§cRedstone§r: clique para trocar entre ignorar, só com sinal e só sem sinal.',
        'Machines with an energy bar have §5tabs§r on the right side of their screen.\n\n§cRedstone§r: click to switch between ignore, only with a signal and only without one.'),
    'guide.mistech.machine_config.2': (
        '§6Lados§r: cada face pode ser §9entrada§r, §6saída§r, §aas duas§r ou desligada (funis, canos e golens respeitam). As faces aparecem em volta da §7frente§r da máquina.',
        '§6Sides§r: each face can be §9input§r, §6output§r, §aboth§r or disabled (hoppers, pipes and golems follow it). Faces are shown around the machine §7front§r.'),
    'guide.mistech.machine_config.3': (
        '§aSaída automática§r empurra o que sai para baús e máquinas encostados nas faces de saída. §aEntrada automática§r puxa de baús encostados nas faces de entrada.\n\nAssim dá para ligar máquinas umas nas outras sem funil.',
        '§aAuto output§r pushes products into chests and machines touching the output faces. §aAuto input§r pulls from chests touching the input faces.\n\nThis chains machines together without hoppers.'),
    'guide.mistech.entry.runic_upgrades': ('Melhorias Rúnicas', 'Runic Upgrades'),
    'guide.mistech.runic_upgrades.plate': ('A base de toda melhoria. Escreva nela na Mesa de Escrita.', 'The base of every upgrade. Write on it at the Writing Table.'),
    'guide.mistech.runic_upgrades.1': (
        '§7Botão direito§r numa máquina com a melhoria instala. Na tela, a aba §5Melhorias§r mostra o que está instalado; clique numa para tirar 1.\n\nQuebrar a máquina devolve as melhorias.',
        '§7Right-click§r a machine with the upgrade to install it. On its screen, the §5Upgrades§r tab shows what is installed; click one to take 1 out.\n\nBreaking the machine drops the upgrades.'),
    'guide.mistech.runic_upgrades.2': (
        '§eVelocidade§r: +50%% cada (até 4).\n§aEficiência§r: -15%% de energia cada (até 4).\n§dAlcance§r: +1 de raio nas máquinas de área (até 3).\n\nGeradores e baterias não aceitam melhorias.',
        '§eSpeed§r: +50%% each (up to 4).\n§aEfficiency§r: -15%% energy each (up to 4).\n§dRange§r: +1 radius on area machines (up to 3).\n\nGenerators and batteries do not take upgrades.'),
    'guide.mistech.entry.arcane_furnace': ('Fornalha Arcana', 'Arcane Furnace'),
    # 'guide.mistech.arcane_furnace.1' está no lang2.py (texto novo da leva 2)
    'guide.mistech.entry.arcane_extractor': ('Extrator Arcano', 'Arcane Extractor'),
    'guide.mistech.arcane_extractor.1': (
        'Produz §5250 mB de Essência Impura a cada 15 s§r (20 FE/t) num tanque de 8.000 mB - quatro vezes o Extrator comum.\n\nFrascos e baldes vazios no espaço de entrada saem cheios; canos e golens tiram direto do tanque.',
        'Makes §5250 mB of Impure Essence every 15 s§r (20 FE/t) into an 8,000 mB tank - four times the basic Extractor.\n\nEmpty bottles and buckets in the input slot come out full; pipes and golems drain the tank directly.'),
    'guide.mistech.entry.arcane_compressor': ('Compressor Arcano', 'Arcane Compressor'),
    'guide.mistech.arcane_compressor.1': (
        '§5Não precisa de incenso§r: com 30 FE/t endurece 250 mB de essência em 1 Fragmento de Magia Impura a cada 6 s.\n\nA essência entra por canos (tanque de 8.000 mB) ou por frascos/baldes; Essência Ígnea vira Fragmento Choroso.',
        '§5No incense needed§r: with 30 FE/t it hardens 250 mB of essence into 1 Impure Magic Shard every 6 s.\n\nEssence comes in through pipes (8,000 mB tank) or bottles/buckets; Igneous Essence becomes a Crying Shard.'),
    'guide.mistech.essence_extractor.energy': (
        'Qualquer energia serve. A mais fácil no começo é o §6Gerador de Cinzas§r: as cinzas do seu incenso viram energia. A Bicicleta do Gnomo é mais forte, mas precisa de um gnomo.',
        'Any energy works. The easiest early on is the §6Ash Generator§r: the ash from your incense becomes power. The Gnome Bicycle is stronger, but needs a gnome.'),
    'guide.mistech.magic_compressor.reserve': (
        '§5Reserva§r: com o compressor cheio, mais frascos ficam esperando a vez (até 3). Um §5balde§r de essência enche tudo de uma vez: 4 fragmentos sem precisar voltar.',
        '§5Reserve§r: while the compressor is full, more bottles wait in line (up to 3). A §5bucket§r of essence fills everything at once: 4 shards without coming back.'),
}


def main():
    for lang, idx in (('pt_br', 0), ('en_us', 1)):
        path = LANG + lang + '.json'
        with open(path, encoding='utf-8') as f:
            data = json.load(f)
        for k, v in T.items():
            data[k] = v[idx]
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')
    print('ok', len(T))


if __name__ == '__main__':
    main()
