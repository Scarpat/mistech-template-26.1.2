package com.mistech.mistechmod.beyond;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * "Ouvir" criaturas: contorno (glowing) que <b>só um jogador vê</b>, por um tempo. Usado pela Ecolocalização (Baseado do Eco),
 * pelo Ecólito e pelo pulso do Cachimbo Rúnico com a Runa do Eco. O contorno é reenviado a cada 10 ticks (o servidor manda as
 * flags verdadeiras quando o mob muda de estado) e removido quando acaba.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class EchoSense {
    /** jogador → (id da entidade → tick do servidor em que o contorno acaba). */
    private static final Map<UUID, Map<Integer, Long>> REVEALED = new HashMap<>();
    private static long serverTicks;

    private EchoSense() {}

    public static void reveal(ServerPlayer player, Entity target, int ticks) {
        if (target == player || !target.isAlive()) {
            return;
        }
        Map<Integer, Long> map = REVEALED.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        long until = serverTicks + ticks;
        Long old = map.put(target.getId(), Math.max(until, map.getOrDefault(target.getId(), 0L)));
        if (old == null) {
            EcholingEntity.sendOutline(player, target, true);
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        serverTicks++;
        if (REVEALED.isEmpty() || serverTicks % 10 != 0) {
            return;
        }
        Iterator<Map.Entry<UUID, Map<Integer, Long>>> players = REVEALED.entrySet().iterator();
        while (players.hasNext()) {
            Map.Entry<UUID, Map<Integer, Long>> entry = players.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                players.remove();
                continue;
            }
            ServerLevel level = player.level();
            Iterator<Map.Entry<Integer, Long>> it = entry.getValue().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Long> reveal = it.next();
                Entity target = level.getEntity(reveal.getKey());
                if (target == null || !target.isAlive()) {
                    it.remove();
                } else if (reveal.getValue() <= serverTicks || target.distanceToSqr(player) > 64 * 64) {
                    EcholingEntity.sendOutline(player, target, false);
                    it.remove();
                } else {
                    EcholingEntity.sendOutline(player, target, true);
                }
            }
            if (entry.getValue().isEmpty()) {
                players.remove();
            }
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        REVEALED.remove(event.getEntity().getUUID());
    }
}
