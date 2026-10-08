package com.mistech.mistechmod.celestial;

import java.util.Arrays;
import java.util.Locale;

import com.mistech.mistechmod.Mistech;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos de teste (operador):
 * /mistech celestial start &lt;evento&gt; · stop &lt;evento&gt; · clear · list · meteor
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class CelestialCommands {
    private CelestialCommands() {}

    @SubscribeEvent
    static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mistech")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("celestial")
                        .then(Commands.literal("start").then(Commands.argument("event", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(CelestialEvent.values()).map(CelestialEvent::getSerializedName), b))
                                .executes(ctx -> start(ctx))))
                        .then(Commands.literal("stop").then(Commands.argument("event", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(CelestialEvent.values()).map(CelestialEvent::getSerializedName), b))
                                .executes(ctx -> stop(ctx))))
                        .then(Commands.literal("clear").executes(ctx -> {
                            Celestial.clear(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("Eventos Celestes limpos."), true);
                            return 1;
                        }))
                        .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                        .then(Commands.literal("meteor").executes(ctx -> {
                            CelestialEffects.launchMeteor(ctx.getSource().getServer().overworld(), ctx.getSource().getPlayerOrException());
                            return 1;
                        }))));
    }

    private static CelestialEvent parse(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "event").toLowerCase(Locale.ROOT);
        for (CelestialEvent e : CelestialEvent.values()) {
            if (e.getSerializedName().equals(name)) {
                return e;
            }
        }
        return null;
    }

    private static int start(CommandContext<CommandSourceStack> ctx) {
        CelestialEvent event = parse(ctx);
        if (event == null) {
            ctx.getSource().sendFailure(Component.literal("Evento desconhecido."));
            return 0;
        }
        MinecraftServer server = ctx.getSource().getServer();
        long t = Celestial.now(server);
        CelestialData.Window window = switch (event) {
            case LONG_NIGHT -> Celestial.nights(t, 3);
            case SOLSTICE -> Celestial.days(t, 3);
            case ECLIPSE -> Celestial.today(t);
            case BLESSED_RAIN -> {
                server.setWeatherParameters(0, 24000, true, false);
                yield new CelestialData.Window(t, t + 24000);
            }
            case SILVER_MOON -> {
                CelestialData.Window n = Celestial.tonight(t);
                yield new CelestialData.Window(n.start(), n.end() + 2000);
            }
            default -> Celestial.tonight(t);
        };
        Celestial.start(server, event, window);
        ctx.getSource().sendSuccess(() -> Component.translatable(event.translationKey()).append(" → " + window.start() + " … " + window.end()), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) {
        CelestialEvent event = parse(ctx);
        if (event == null) {
            ctx.getSource().sendFailure(Component.literal("Evento desconhecido."));
            return 0;
        }
        Celestial.stop(ctx.getSource().getServer(), event);
        ctx.getSource().sendSuccess(() -> Component.literal("Parado: ").append(Component.translatable(event.translationKey())), true);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        long t = Celestial.now(server);
        source.sendSuccess(() -> Component.literal("Relógio: " + t + " (hora " + Celestial.timeOfDay(t) + ")"), false);
        for (CelestialEvent e : Celestial.scheduled(server)) {
            CelestialData.Window w = Celestial.window(server, e);
            boolean on = Celestial.isActive(server.overworld(), e);
            source.sendSuccess(() -> Component.translatable(e.translationKey())
                    .append((on ? " [ATIVO] " : " [agendado] ") + (w == null ? "" : w.start() + " … " + w.end())), false);
        }
        return 1;
    }
}
