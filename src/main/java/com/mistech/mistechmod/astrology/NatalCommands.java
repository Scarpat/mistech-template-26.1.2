package com.mistech.mistechmod.astrology;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.astrology.NatalChart.Part;
import com.mistech.mistechmod.registry.MistechAttachments;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos de teste do Mapa Astral (operador):
 * /mistech natal show [jogador]              - mostra o mapa inteiro (inclusive o escondido)
 * /mistech natal reveal sun|moon|rising|all  - revela sem custo nem horário
 * /mistech natal reroll [jogador]            - sorteia de novo (tudo escondido)
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class NatalCommands {
    private NatalCommands() {}

    @SubscribeEvent
    static void onRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> reveal = Commands.literal("reveal");
        for (String name : new String[] {"sun", "moon", "rising", "all"}) {
            reveal.then(Commands.literal(name)
                    .executes(ctx -> reveal(ctx.getSource(), ctx.getSource().getPlayerOrException(), name))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> reveal(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), name))));
        }
        event.getDispatcher().register(Commands.literal("mistech")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("natal")
                        .then(Commands.literal("show")
                                .executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(reveal)
                        .then(Commands.literal("reroll")
                                .executes(ctx -> reroll(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> reroll(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        NatalChart chart = NatalService.ensureRolled(player);
        for (Part part : Part.values()) {
            int sign = chart.sign(part);
            Component line = Component.empty().append(part.displayName()).append(": ")
                    .append(NatalChart.glyph(sign) + " ").append(NatalChart.signName(sign))
                    .append(chart.isRevealed(part) ? "" : " (?)");
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int reveal(CommandSourceStack source, ServerPlayer player, String which) throws CommandSyntaxException {
        NatalChart chart = NatalService.ensureRolled(player);
        for (Part part : Part.values()) {
            if (which.equals("all") || which.equals(part.id())) {
                chart = chart.reveal(part);
            }
        }
        player.setData(MistechAttachments.NATAL_CHART, chart);
        NatalService.apply(player);
        return show(source, player);
    }

    private static int reroll(CommandSourceStack source, ServerPlayer player) {
        NatalService.rebirth(player);
        source.sendSuccess(() -> Component.literal("Mapa astral sorteado de novo para " + player.getName().getString()), true);
        return 1;
    }
}
