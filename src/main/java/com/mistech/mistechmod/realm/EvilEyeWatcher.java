package com.mistech.mistechmod.realm;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.entity.EvilEyeEntity;
import com.mistech.mistechmod.registry.MurkyWood;
import com.mistech.mistechmod.registry.RealmContent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Faz o Mal Olhado aparecer para quem fica muito tempo no Pesadelo (anel 2 do Sonho, ver {@link DreamLayout}).
 * Primeira aparição depois de ~2 min lá dentro; depois ele volta mais rápido (40-80 s).
 * Aparece atrás do jogador, a 22-34 blocos, de preferência colado numa Árvore Turva.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class EvilEyeWatcher {
    public static final int REALM_RADIUS = 112;
    public static final int FIRST_APPEARANCE = 2 * 60 * 20;

    /** Ticks acumulados no Ermo (desce devagar fora dele). */
    private static final Map<UUID, Integer> EXPOSURE = new HashMap<>();
    /** Próxima aparição acontece quando a exposição passar disso. */
    private static final Map<UUID, Integer> NEXT = new HashMap<>();

    private EvilEyeWatcher() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || !GnomeRealm.isRealm(level) || player.isSpectator()) {
            return;
        }
        UUID id = player.getUUID();
        boolean inWastes = player.getY() > 48 && DreamLayout.ring(player.getBlockX(), player.getBlockZ()) == DreamLayout.RING_NIGHTMARE;
        int exposure = EXPOSURE.getOrDefault(id, 0);
        exposure = inWastes ? exposure + 1 : Math.max(0, exposure - 2);
        EXPOSURE.put(id, exposure);
        if (!inWastes || level.getGameTime() % 20 != 0) {
            return;
        }
        int next = NEXT.getOrDefault(id, FIRST_APPEARANCE);
        if (exposure < next || alreadyWatching(level, player)) {
            return;
        }
        if (spawnBehind(level, player, true) != null) {
            NEXT.put(id, exposure + 40 * 20 + player.getRandom().nextInt(40 * 20));
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EXPOSURE.remove(event.getEntity().getUUID());
        NEXT.remove(event.getEntity().getUUID());
    }

    public static boolean alreadyWatching(ServerLevel level, ServerPlayer player) {
        return !level.getEntitiesOfClass(EvilEyeEntity.class, new AABB(player.blockPosition()).inflate(64.0),
                e -> player.getUUID().equals(e.watched())).isEmpty();
    }

    /**
     * Cria um Mal Olhado atrás do jogador. {@code realm} = no Ermo Turvo (evita o prado, prefere Árvore Turva);
     * fora dele (Mácula no overworld) prefere qualquer tronco. @return o Mal Olhado ou null se não achou lugar
     */
    public static EvilEyeEntity spawnBehind(ServerLevel level, ServerPlayer player, boolean realm) {
        RandomSource random = player.getRandom();
        Vec3 look = player.getViewVector(1.0F);
        double baseAngle = Math.atan2(-look.z, -look.x); // atrás do jogador
        BlockPos best = null;
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = baseAngle + (random.nextDouble() - 0.5) * Math.toRadians(140);
            double dist = 22 + random.nextDouble() * 12;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            if ((realm && DreamLayout.ring(x, z) != DreamLayout.RING_NIGHTMARE) || !level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getBlockState(ground).isAir() || !level.getBlockState(ground.above(2)).isAir()) {
                continue;
            }
            best = ground;
            if (nextToTree(level, ground, realm)) {
                break; // atrás de uma árvore é o melhor lugar
            }
        }
        if (best == null) {
            return null;
        }
        EvilEyeEntity eye = RealmContent.EVIL_EYE.get().create(level, EntitySpawnReason.EVENT);
        if (eye == null) {
            return null;
        }
        eye.snapTo(best.getX() + 0.5, best.getY(), best.getZ() + 0.5, 0.0F, 0.0F);
        eye.setWatched(player.getUUID());
        level.addFreshEntity(eye);
        return eye;
    }

    private static boolean nextToTree(ServerLevel level, BlockPos pos, boolean realm) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 2, 2))) {
            if (realm ? level.getBlockState(p).is(MurkyWood.LOG.get()) : level.getBlockState(p).is(net.minecraft.tags.BlockTags.LOGS)) {
                return true;
            }
        }
        return false;
    }
}
