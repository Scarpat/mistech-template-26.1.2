"""Textos da leva 2 (20/10/2026). Uso: python3 tools/qol/lang2.py"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import lang  # noqa: E402

lang.T = {
    # ----- blocos e itens
    'block.mistech.arcane_crusher': ('Triturador Arcano', 'Arcane Crusher'),
    'container.mistech.arcane_crusher': ('Triturador Arcano', 'Arcane Crusher'),
    'block.mistech.liquefier': ('Liquefator Rúnico', 'Runic Liquefier'),
    'container.mistech.liquefier': ('Liquefator Rúnico', 'Runic Liquefier'),
    'block.mistech.crusher_forge': ('Forja Trituradora', 'Crusher Forge'),
    'container.mistech.crusher_forge': ('Forja Trituradora', 'Crusher Forge'),
    'item.mistech.mithril_dust': ('Pó de Mitril', 'Mithril Dust'),
    'item.mistech.adamantine_dust': ('Pó de Adamantina', 'Adamantine Dust'),
    'item.mistech.orichalcum_dust': ('Pó de Oricalco', 'Orichalcum Dust'),
    'item.mistech.mandrake_powder': ('Pó de Mandrágora', 'Mandrake Powder'),
    'item.mistech.mandrake_powder.desc': ('Adubo forte: faz crescer as plantas numa área 3×3 de uma vez',
                                          'Strong fertilizer: grows the plants in a 3×3 area at once'),
    'item.mistech.mandrake_root.desc': ('Botão direito: aperte a raiz e ela grita (atordoa criaturas por perto)',
                                        'Right-click: squeeze the root and it screams (stuns nearby creatures)'),
    'item.minecraft.tipped_arrow.effect.belladonna': ('Flecha de Beladona', 'Belladonna Arrow'),
    'effect.mistech.caffeinated': ('Cafeinado', 'Caffeinated'),

    # ----- mensagens / telas / Jade
    'message.mistech.caffeinated.no_sleep': ('Café demais: você está elétrico demais para dormir', 'Too much coffee: you are too wired to sleep'),
    'message.mistech.gnome_bicycle.coffee': ('O gnomo tomou o café e pedala mais rápido! (+50%% por 5 min)',
                                             'The gnome drank the coffee and pedals faster! (+50%% for 5 min)'),
    'info.mistech.gnome_bicycle.coffee': ('Café: %ss', 'Coffee: %ss'),
    'message.mistech.mandrake.root_spent': ('A raiz murchou depois do grito', 'The root withered after the scream'),
    'message.mistech.mandrake.stunned': ('%s criaturas atordoadas', '%s creatures stunned'),
    'info.mistech.arcane_furnace.powered': ('Com energia: rápida, +XP e chance de lingote extra', 'Powered: fast, +XP and bonus ingot chance'),
    'info.mistech.arcane_furnace.unpowered': ('Sem energia: velocidade de Fornalha Rúnica', 'Unpowered: Runic Furnace speed'),
    'info.mistech.forge.incomplete': ('Estrutura incompleta (Pedra Guia mostra)', 'Incomplete structure (Guide Stone shows it)'),
    'gui.mistech.forge.incomplete': ('Incompleta', 'Incomplete'),
    'jei.mistech.crushing.arcane_only': ('Só no Triturador Arcano e na Forja', 'Arcane Crusher and Forge only'),
    'jei.mistech.multiblock.crusher_forge': ('Cubo 2×2×2 atrás e à direita do Núcleo', '2×2×2 cube behind and to the right of the core'),

    # ----- Grimório
    'guide.mistech.entry.liquefier': ('Liquefator Rúnico', 'Runic Liquefier'),
    'guide.mistech.liquefier.1': (
        'Derrete a magia impura sólida de volta em §5Essência Impura§r: Fragmento = 250 mB, Pó de Magia Impura = 125 mB, Bloco de Magia Impura = 750 mB. 5 s por item, 25 FE/t.',
        'Melts solid impure magic back into §5Impure Essence§r: Shard = 250 mB, Impure Magic Dust = 125 mB, Impure Magic Block = 750 mB. 5 s per item, 25 FE/t.'),
    'guide.mistech.liquefier.2': (
        'O tanque guarda 8.000 mB. Frascos e baldes vazios no espaço de baixo saem cheios; canos e golens tiram direto, e um frasco na mão também.',
        'The tank holds 8,000 mB. Empty bottles and buckets in the lower slot come out full; pipes and golems drain it directly, and so does a bottle in hand.'),
    'guide.mistech.entry.arcane_crusher': ('Triturador Arcano', 'Arcane Crusher'),
    'guide.mistech.arcane_crusher.1': (
        'Tritura §5dois itens ao mesmo tempo§r, duas vezes mais rápido que o Triturador Rúnico, e é o único que aguenta os metais dos gnomos: §bMitril§r, §cAdamantina§r e §6Oricalco§r viram pó.\n\nA Runa da Terra já vem embutida (+1 em minério); com outra no espaço de runa, mais +1.',
        'Crushes §5two items at once§r, twice as fast as the Runic Crusher, and is the only one that handles the gnome metals: §bMithril§r, §cAdamantine§r and §6Orichalcum§r become dust.\n\nThe Earth Rune is built in (+1 on ores); with another in the rune slot, +1 more.'),
    'guide.mistech.arcane_crusher.dusts': ('Os pós derretem até na fornalha comum.', 'The dusts even melt in a normal furnace.'),
    'guide.mistech.entry.crusher_forge': ('Forja Trituradora', 'Crusher Forge'),
    'guide.mistech.crusher_forge.1': (
        'Multibloco 2×2×2. Olhando para a frente do §6Núcleo§r: embaixo, Núcleo e §5Fornalha Arcana§r à direita; em cima, §5Triturador Arcano§r sobre o Núcleo; o resto (5) é §5Bloco de Magia Impura§r. A Pedra Guia mostra o holograma.',
        '2×2×2 multiblock. Facing the §6Core§r front: bottom, Core and the §5Arcane Furnace§r to its right; top, the §5Arcane Crusher§r above the Core; the rest (5) is §5Impure Magic Block§r. The Guide Stone shows the hologram.'),
    'guide.mistech.crusher_forge.2': (
        'Tritura e funde de uma vez: §6minério vira lingote§r direto, com o bônus de minério (+1, mais 25%% de chance de outro). 3 itens ao mesmo tempo, 4 s cada, 60 FE/t por item.\n\nO que não tritura mas funde, ela só funde.',
        'Crushes and smelts at once: §6ore becomes ingots§r directly, with the ore bonus (+1, plus a 25%% chance of another). 3 items at once, 4 s each, 60 FE/t per item.\n\nThings that only smelt are just smelted.'),
    'guide.mistech.arcane_furnace.1': (
        '3 linhas ao mesmo tempo. §5Com energia§r (15 FE/t por linha): 4× mais rápida que a fornalha comum, experiência ×1,5 e 25%% de chance de um lingote extra em minérios. §7Sem energia§r ela continua, na velocidade da Fornalha Rúnica.\n\nFaz também as receitas do alto-forno.',
        '3 lanes at once. §5Powered§r (15 FE/t per lane): 4× faster than a normal furnace, ×1.5 experience and a 25%% chance of an extra ingot from ores. §7Unpowered§r it keeps going at Runic Furnace speed.\n\nIt also does blast furnace recipes.'),
    'guide.mistech.entry.belladonna_uses': ('Segredos da Beladona', 'Belladonna Secrets'),
    'guide.mistech.belladonna_uses.1': (
        'Sob a §5Visão Mística§r você vê o que se esconde: criaturas invisíveis e os seres do folclore que gostam de sumir (Saci, Curupira, Matinta, Iara...) ganham contorno. E os §5minérios§r por perto brilham através da pedra.',
        'Under §5Mystic Sight§r you see what hides: invisible creatures and the folklore beings that like to vanish (Saci, Curupira, Matinta, Iara...) get an outline. Nearby §5ores§r glow through the stone.'),
    'guide.mistech.belladonna_uses.arrow': ('8 flechas + bagas: Veneno, Náusea e Lentidão no alvo.', '8 arrows + berries: Poison, Nausea and Slowness on the target.'),
    'guide.mistech.belladonna_uses.2': (
        'As §5bagas§r também podem ser comidas... se você não se importar com o veneno. Dão um pouco de Visão Mística.',
        'The §5berries§r can be eaten too... if you do not mind the poison. They give a little Mystic Sight.'),
    'guide.mistech.entry.coffee_uses': ('O Poder do Café', 'The Power of Coffee'),
    'guide.mistech.coffee_uses.1': (
        'A xícara de café agora deixa você §6Cafeinado§r: os Phantoms não aparecem (não conta como noite sem dormir), você ganha +10%% de experiência... mas não consegue dormir até passar.',
        'A cup of coffee now makes you §6Caffeinated§r: Phantoms do not come (it counts as rest), you get +10%% experience... but you cannot sleep until it wears off.'),
    'guide.mistech.coffee_uses.2': (
        'Gnomos adoram café: clique na §6Bicicleta do Gnomo§r com uma xícara e ele pedala §e50%% mais forte§r por 5 minutos (até 15 minutos guardados).',
        'Gnomes love coffee: click the §6Gnome Bicycle§r with a cup and it pedals §e50%% harder§r for 5 minutes (up to 15 minutes stored).'),
    'guide.mistech.entry.mandrake_uses': ('Usos da Mandrágora', 'Mandrake Uses'),
    'guide.mistech.mandrake_uses.1': (
        '§7Botão direito§r com a §5Raiz de Mandrágora§r: ela grita. Criaturas a até 8 blocos ficam atordoadas (lentas, fracas e perdem o alvo); jogadores sem capacete ficam tontos. A raiz murcha em 1 de cada 3 gritos.',
        '§7Right-click§r with the §5Mandrake Root§r: it screams. Creatures within 8 blocks are stunned (slow, weak and lose their target); players without a helmet get dizzy. The root withers 1 in 3 screams.'),
    'guide.mistech.mandrake_uses.2': (
        'Triturada, a raiz vira §aPó de Mandrágora§r: um adubo que faz crescer tudo numa área 3×3 de uma vez. A §5Mandrágora Engarrafada§r agora também faz os filhotes de animais crescerem ~4× mais rápido.',
        'Crushed, the root becomes §aMandrake Powder§r: a fertilizer that grows everything in a 3×3 area at once. The §5Bottled Mandrake§r now also makes baby animals grow ~4× faster.'),
}

if __name__ == '__main__':
    lang.main()
