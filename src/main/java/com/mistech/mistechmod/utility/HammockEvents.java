package com.mistech.mistechmod.utility;

import java.util.Optional;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.ClockAdjustment;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;

/**
 * Sono na Rede de Dormir.
 * <ul>
 * <li>Deitar: de dia o vanilla diria "só dá para dormir à noite"; aqui a rede libera (mas confere os monstros, que o vanilla
 * não confere de dia);</li>
 * <li>Ficar deitado: de dia continua; ao escurecer a rede acorda o jogador;</li>
 * <li>Todos dormindo: se alguém está numa rede (e é dia), o relógio vai para o anoitecer em vez da manhã;</li>
 * <li>Rede não vira ponto de renascimento.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class HammockEvents {
    private HammockEvents() {}

    private static boolean isHammock(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof HammockBlock;
    }

    private static boolean inHammock(LivingEntity entity) {
        Optional<BlockPos> bed = entity.getSleepingPos();
        return bed.isPresent() && isHammock(entity.level(), bed.get());
    }

    @SubscribeEvent
    static void onCanSleep(CanPlayerSleepEvent event) {
        if (!(event.getState().getBlock() instanceof HammockBlock)) {
            return;
        }
        ServerPlayer player = event.getEntity();
        Level level = player.level();
        BlockPos pos = event.getPos();
        if (!HammockBlock.worksHere(level, pos)) {
            event.setProblem(new Player.BedSleepingProblem(Component.translatable("message.mistech.hammock.cant_here")));
            return;
        }
        if (level.isDarkOutside()) {
            event.setProblem(new Player.BedSleepingProblem(Component.translatable("message.mistech.hammock.night")));
            return;
        }
        // de dia o único "problema" do vanilla é a regra da cama (não é noite): a rede ignora esse
        Player.BedSleepingProblem dayProblem = level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos).asProblem();
        if (event.getProblem() != null && !event.getProblem().equals(dayProblem)) {
            return; // longe demais, bloqueada...
        }
        if (!player.isCreative() && monstersNear(player, pos)) {
            event.setProblem(Player.BedSleepingProblem.NOT_SAFE);
            return;
        }
        event.setProblem(null);
    }

    /** Mesmo teste da cama (8 × 5 blocos em volta). */
    private static boolean monstersNear(ServerPlayer player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 c = Vec3.atBottomCenterOf(pos);
        return !level.getEntitiesOfClass(Monster.class, new AABB(c.x() - 8.0, c.y() - 5.0, c.z() - 8.0, c.x() + 8.0, c.y() + 5.0, c.z() + 8.0),
                monster -> monster.isPreventingPlayerRest(level, player)).isEmpty();
    }

    @SubscribeEvent
    static void onCanContinueSleeping(CanContinueSleepingEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !inHammock(entity)) {
            return;
        }
        // de dia continua; escureceu, acorda (a rede é para cochilo)
        event.setContinueSleeping(!entity.level().isDarkOutside());
    }

    /** Todos dormiram: com alguém numa rede, de dia, pula para o anoitecer. */
    @SubscribeEvent
    static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.isDarkOutside()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSleeping() && inHammock(player)) {
                event.setAdjustment(new ClockAdjustment.Marker(ClockTimeMarkers.NIGHT));
                return;
            }
        }
    }

    /** Deitar na rede não troca o ponto de renascimento. */
    @SubscribeEvent
    static void onSetSpawn(PlayerSetSpawnEvent event) {
        BlockPos spawn = event.getNewSpawn();
        if (spawn != null && !event.isForced() && event.getEntity().level().dimension().equals(event.getSpawnLevel())
                && isHammock(event.getEntity().level(), spawn)) {
            event.setCanceled(true);
        }
    }
}
