package com.mistech.mistechmod.realm;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.RealmContent;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos de teste (precisa de permissão de operador):
 * /mistech dimensao_gnomos [jogador] - leva para a Dimensão dos Gnomos (a volta é para onde o jogador estava)
 * /mistech sair_sonho [jogador]      - volta para casa
 * /mistech casa_do_rei               - constrói a Casa do Rei (se ainda não existir)
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class RealmCommands {
    private RealmCommands() {}

    @SubscribeEvent
    static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mistech")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("dimensao_gnomos")
                        .executes(ctx -> enter(ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> enter(EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("sair_sonho")
                        .executes(ctx -> leave(ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> leave(EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("casa_do_rei").executes(ctx -> {
                    ServerLevel realm = ctx.getSource().getServer().getLevel(RealmContent.REALM);
                    if (realm == null) {
                        return 0;
                    }
                    var pos = GnomeRealmHouse.ensureBuilt(realm);
                    ctx.getSource().sendSuccess(() -> Component.literal("Casa do Rei em " + pos.toShortString()), true);
                    return 1;
                })));
    }

    private static int enter(ServerPlayer player) {
        GnomeRealm.dreamInto(player, player.blockPosition());
        return 1;
    }

    private static int leave(ServerPlayer player) {
        GnomeRealm.sendHome(player);
        return 1;
    }
}
