package com.mistech.mistechmod.purity;

import com.mistech.mistechmod.Mistech;
import com.mojang.brigadier.arguments.FloatArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos de teste da Mácula (operador):
 * /mistech taint get [jogador] · /mistech taint set &lt;valor&gt; [jogador] · /mistech taint add &lt;valor&gt; [jogador]
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class TaintCommands {
    private TaintCommands() {}

    @SubscribeEvent
    static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mistech")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("taint")
                        .then(Commands.literal("get")
                                .executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0, Taint.MAX))
                                        .executes(ctx -> set(ctx.getSource(), ctx.getSource().getPlayerOrException(), FloatArgumentType.getFloat(ctx, "value"), false))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), FloatArgumentType.getFloat(ctx, "value"), false)))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("value", FloatArgumentType.floatArg(-Taint.MAX, Taint.MAX))
                                        .executes(ctx -> set(ctx.getSource(), ctx.getSource().getPlayerOrException(), FloatArgumentType.getFloat(ctx, "value"), true))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), FloatArgumentType.getFloat(ctx, "value"), true)))))));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        Component line = Component.empty().append(player.getDisplayName()).append(": ").append(Taint.describe(Taint.get(player)));
        source.sendSuccess(() -> line, false);
        return Math.round(Taint.get(player));
    }

    private static int set(CommandSourceStack source, ServerPlayer player, float value, boolean add) {
        Taint.set(player, add ? Taint.get(player) + value : value);
        return show(source, player);
    }
}
