package com.mistech.mistechmod.affinity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.registry.RitualContent;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos de teste da afinidade (operador). Ignoram a exclusividade e o teto total.
 * <ul>
 * <li>/mistech affinity get &lt;jogador&gt;</li>
 * <li>/mistech affinity set &lt;jogador&gt; &lt;elemento|all&gt; &lt;pontos 0-1000&gt;</li>
 * <li>/mistech affinity level &lt;jogador&gt; &lt;elemento|all&gt; &lt;nível 0-7&gt;</li>
 * <li>/mistech affinity reset &lt;jogador&gt;</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class AffinityCommands {
    private static final List<String> IDS = java.util.Arrays.stream(RuneElement.values()).filter(e -> !e.isAdvanced()).map(RuneElement::id).toList();

    private AffinityCommands() {}

    @SubscribeEvent
    static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mistech")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("affinity")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            p.setData(RitualContent.AFFINITY, AffinityData.EMPTY);
                                            return show(ctx.getSource(), p);
                                        })))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(withAll(), b))
                                                .then(Commands.argument("points", IntegerArgumentType.integer(0, Affinity.MAX_POINTS))
                                                        .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "element"), IntegerArgumentType.getInteger(ctx, "points")))))))
                        .then(Commands.literal("level")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(withAll(), b))
                                                .then(Commands.argument("level", IntegerArgumentType.integer(0, Affinity.MAX_LEVEL))
                                                        .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "element"),
                                                                Affinity.THRESHOLDS[IntegerArgumentType.getInteger(ctx, "level")]))))))));
    }

    private static List<String> withAll() {
        List<String> list = new java.util.ArrayList<>(IDS);
        list.add("all");
        return list;
    }

    private static int set(CommandSourceStack source, ServerPlayer player, String element, int points) {
        Map<String, Integer> map = new HashMap<>(Affinity.get(player).points());
        if (element.equals("all")) {
            for (String id : IDS) {
                map.put(id, points);
            }
        } else if (IDS.contains(element)) {
            map.put(element, points);
        } else {
            source.sendFailure(Component.literal("Elemento inválido: " + element + " (use " + String.join(", ", IDS) + " ou all)"));
            return 0;
        }
        map.values().removeIf(v -> v <= 0);
        player.setData(RitualContent.AFFINITY, new AffinityData(map));
        return show(source, player);
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        AffinityData data = Affinity.get(player);
        StringBuilder sb = new StringBuilder();
        for (String id : IDS) {
            int points = data.points().getOrDefault(id, 0);
            if (points > 0) {
                sb.append(id).append(' ').append(points).append(" (nv ").append(Affinity.level(points)).append(")  ");
            }
        }
        Component line = Component.empty().append(player.getDisplayName()).append(": ").append(sb.length() == 0 ? "sem afinidade" : sb.toString().trim());
        source.sendSuccess(() -> line, false);
        return data.total();
    }
}
