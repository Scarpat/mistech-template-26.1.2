package com.mistech.mistechmod.purity.area;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Config;
import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.Taint;
import com.mistech.mistechmod.purity.block.TaintedSoilBlock;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Regras da Mácula do lugar: o minuto dos chunks sujos (espalhar, sumir e mudar blocos), a Mácula pessoal de quem
 * fica num lugar maculado, plantações mais lentas, mais monstros, e a cópia para o cliente (névoa e HUD).
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class AreaTaintEvents {
    public static final int MINUTE = 1200;
    public static final int SYNC_INTERVAL = 100;

    private record Extra(ServerLevel level, EntityType<?> type, BlockPos pos) {}

    /** Monstros extras dos lugares Corrompidos, nascidos no tick seguinte (fora do evento de spawn). */
    private static final List<Extra> EXTRAS = new ArrayList<>();

    private AreaTaintEvents() {}

    // ----- conjunto de chunks sujos -----

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && AreaTaint.get(event.getChunk()) > 0) {
            AreaTaint.dirty(level).add(event.getChunk().getPos().pack());
        }
    }

    @SubscribeEvent
    static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AreaTaint.forget(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AreaTaint.forget(level.dimension());
            EXTRAS.removeIf(e -> e.level() == level);
        }
    }

    // ----- o minuto -----

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!EXTRAS.isEmpty()) {
            for (Extra extra : new ArrayList<>(EXTRAS)) {
                if (extra.level() == level) {
                    EXTRAS.remove(extra);
                    extra.type().spawn(level, extra.pos(), EntitySpawnReason.EVENT);
                }
            }
        }
        if (level.getGameTime() % MINUTE != 37 || !Config.areaTaint()) {
            return;
        }
        LongOpenHashSet dirty = AreaTaint.dirty(level);
        if (dirty.isEmpty()) {
            return;
        }
        long[] chunks = dirty.toLongArray();
        boolean sun = level.isBrightOutside() && !level.isRaining();
        boolean silver = com.mistech.mistechmod.celestial.Celestial.isActive(level, com.mistech.mistechmod.celestial.CelestialEvent.SILVER_MOON);
        boolean blessed = com.mistech.mistechmod.celestial.Celestial.isActive(level, com.mistech.mistechmod.celestial.CelestialEvent.BLESSED_RAIN);
        for (long packed : chunks) {
            int cx = ChunkPos.getX(packed), cz = ChunkPos.getZ(packed);
            LevelChunk chunk = AreaTaint.loaded(level, cx, cz);
            if (chunk == null) {
                dirty.remove(packed);
                continue;
            }
            float value = AreaTaint.get(chunk);
            // espalha: acima de 50, 10% do excesso vai para os 4 vizinhos carregados
            if (value > AreaTaint.SPREAD_ABOVE) {
                float share = (value - AreaTaint.SPREAD_ABOVE) * AreaTaint.SPREAD_FRACTION / 4.0F;
                int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] side : sides) {
                    LevelChunk next = AreaTaint.loaded(level, cx + side[0], cz + side[1]);
                    if (next != null && AreaTaint.get(next) < value) {
                        AreaTaint.set(level, next, AreaTaint.get(next) + share);
                        value -= share;
                    }
                }
            }
            // some sozinha: só abaixo de 20, com sol
            if (value < AreaTaint.FADE_BELOW && sun) {
                value -= AreaTaint.FADE_PER_MINUTE;
            }
            // céu (Fase 6): Lua de Prata −1 em todo chunk; Chuva Bendita −2 onde a chuva cai
            if (silver) {
                value -= 1.0F;
            }
            if (blessed) {
                int mx = (cx << 4) + 8, mz = (cz << 4) + 8;
                if (level.isRainingAt(new BlockPos(mx, level.getHeight(Heightmap.Types.MOTION_BLOCKING, mx, mz), mz))) {
                    value -= 2.0F;
                }
            }
            AreaTaint.set(level, chunk, value);
            if (value > 0 && Config.areaTaintBlocks()) {
                changeBlocks(level, chunk, Taint.Stage.of(value));
            }
        }
    }

    /** Maculada: grama vira Grama Murcha. Profana: terra e grama viram Solo Maculado (que não espalha sozinho). */
    private static void changeBlocks(ServerLevel level, LevelChunk chunk, Taint.Stage stage) {
        if (stage.ordinal() < Taint.Stage.TAINTED.ordinal()) {
            return;
        }
        RandomSource random = level.getRandom();
        int tries = switch (stage) {
            case TAINTED -> 4;
            case CORRUPTED -> 6;
            default -> 8;
        };
        ChunkPos cp = chunk.getPos();
        for (int i = 0; i < tries; i++) {
            int x = cp.getMinBlockX() + random.nextInt(16);
            int z = cp.getMinBlockZ() + random.nextInt(16);
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
            BlockState state = level.getBlockState(pos);
            Block withered = com.mistech.mistechmod.registry.RealmContent.WITHERED_GRASS.get();
            if (stage == Taint.Stage.PROFANE && (TaintedSoilBlock.canTaint(state) || state.is(withered))) {
                level.setBlock(pos, PurityContent.TAINTED_SOIL.get().defaultBlockState()
                        .setValue(TaintedSoilBlock.STRENGTH, TaintedSoilBlock.MAX_STRENGTH), Block.UPDATE_ALL);
            } else if (state.is(Blocks.GRASS_BLOCK)) {
                level.setBlock(pos, withered.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    // ----- jogador -----

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.tickCount % 20 == 3 && Config.areaTaint()) {
            Taint.Stage stage = AreaTaint.stage(level, player.blockPosition());
            float perMinute = AreaTaint.personalPerMinute(stage);
            if (perMinute > 0) {
                Taint.add(player, perMinute / 60.0F);
            }
        }
        if (player.tickCount % SYNC_INTERVAL == 11) {
            float here = AreaTaint.get(level, player.blockPosition());
            float around = here;
            int cx = player.getBlockX() >> 4, cz = player.getBlockZ() >> 4;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    LevelChunk chunk = AreaTaint.loaded(level, cx + dx, cz + dz);
                    if (chunk != null) {
                        around = Math.max(around, AreaTaint.get(chunk));
                    }
                }
            }
            PacketDistributor.sendToPlayer(player, new AreaTaintPayload(Math.round(here * 10), Math.round(around * 10)));
        }
    }

    // ----- mundo -----

    /** Maculada ou pior: plantações crescem 25% mais devagar. */
    @SubscribeEvent
    static void onCropGrow(CropGrowEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level
                && AreaTaint.stage(level, event.getPos()).ordinal() >= Taint.Stage.TAINTED.ordinal()
                && level.getRandom().nextFloat() < 0.25F) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }

    /** Corrompida ou pior: cada monstro que nasce sozinho tem 50% de chance de vir com um igual. */
    @SubscribeEvent
    static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() != EntitySpawnReason.NATURAL || event.getEntity().getType().getCategory() != MobCategory.MONSTER) {
            return;
        }
        ServerLevel level = event.getLevel().getLevel();
        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        if (AreaTaint.stage(level, pos).ordinal() >= Taint.Stage.CORRUPTED.ordinal() && level.getRandom().nextFloat() < 0.5F
                && EXTRAS.size() < 64) {
            EXTRAS.add(new Extra(level, event.getEntity().getType(), pos));
        }
    }
}
