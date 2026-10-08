package com.mistech.mistechmod.tarot.fool;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.tarot.ArcanePlane;
import com.mistech.mistechmod.tarot.ArenaLayout;
import com.mistech.mistechmod.tarot.ArenaTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Arena do Louco: um arquipélago de ilhas flutuantes no vazio.
 * <ul>
 * <li>Ilha central grande (onde o grupo chega) + 6 ilhas em volta, em alturas diferentes, e pedrinhas no caminho.</li>
 * <li><b>Correntes Ascendentes</b> (terracota azul-clara): quem pisa é jogado para o alto com Queda Lenta — dá para
 * "planar" até as ilhas mais altas.</li>
 * <li>Quem cai das ilhas não morre: o vento devolve para a ilha central, com um pouco de dano.</li>
 * </ul>
 */
public class FoolArena implements ArenaLayout {
    public static final FoolArena INSTANCE = new FoolArena();

    /** Uma ilha: centro do topo (bloco de grama) e raio. */
    public record Island(BlockPos top, int radius) {
        /** Posição para ficar em pé/flutuar no meio da ilha. */
        public Vec3 surface() {
            return Vec3.atBottomCenterOf(this.top.above());
        }
    }

    private static final int CENTRAL_RADIUS = 7;
    private static final double OUTER_DISTANCE = 17.0;
    private static final int[] OUTER_RADIUS = {4, 3, 4, 3, 4, 3};
    private static final int[] OUTER_HEIGHT = {4, -3, 7, 1, -2, 5};
    /** Ilha onde o Louco aparece (a mais alta). */
    private static final int SPAWN_ISLAND = 3;

    public static final double UPDRAFT_VELOCITY = 1.25;
    public static final int FALL_DAMAGE = 4;

    @Override
    public double leashRadius() {
        return 30.0;
    }

    @Override
    public double leashBelow() {
        return -24.0;
    }

    @Override
    public int clearRadius() {
        return ArenaTools.MAX_CLEAR_RADIUS;
    }

    @Override
    public double fallLimit() {
        return -22.0;
    }

    /** As ilhas: 0 = central, 1..6 = em volta. */
    public static List<Island> islands(BlockPos center) {
        List<Island> list = new ArrayList<>();
        list.add(new Island(center, CENTRAL_RADIUS));
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(15 + i * 60);
            list.add(new Island(center.offset((int) Math.round(Math.cos(angle) * OUTER_DISTANCE), OUTER_HEIGHT[i],
                    (int) Math.round(Math.sin(angle) * OUTER_DISTANCE)), OUTER_RADIUS[i]));
        }
        return list;
    }

    /** Pedrinhas entre a ilha central e cada ilha de fora. */
    private static List<Island> stones(BlockPos center) {
        List<Island> list = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(15 + i * 60);
            list.add(new Island(center.offset((int) Math.round(Math.cos(angle) * 11.5), Math.round(OUTER_HEIGHT[i] / 2.0F),
                    (int) Math.round(Math.sin(angle) * 11.5)), 1));
        }
        return list;
    }

    /** Correntes Ascendentes: 3 na ilha central e uma no meio de cada ilha de fora. */
    public static List<BlockPos> updrafts(BlockPos center) {
        List<BlockPos> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(45 + i * 120);
            list.add(center.offset((int) Math.round(Math.cos(angle) * 4.5), 0, (int) Math.round(Math.sin(angle) * 4.5)));
        }
        List<Island> islands = islands(center);
        for (int i = 1; i < islands.size(); i++) {
            list.add(islands.get(i).top());
        }
        return list;
    }

    @Override
    public Vec3 arrival(BlockPos center, int index) {
        // perto do meio da ilha central (as correntes ficam a 4,5 blocos do centro)
        double angle = Math.toRadians(195 + index * 50);
        return new Vec3(center.getX() + 0.5 + Math.cos(angle) * 2.5, center.getY() + 1.0, center.getZ() + 0.5 + Math.sin(angle) * 2.5);
    }

    @Override
    public Vec3 bossSpawn(BlockPos center) {
        return islands(center).get(SPAWN_ISLAND).surface().add(0.0, 1.0, 0.0);
    }

    // ----------------------------------------------------------------- construção

    @Override
    public void build(ServerLevel level, BlockPos center) {
        ArenaTools.clear(level, center, this);
        int seed = 0;
        for (Island island : islands(center)) {
            buildIsland(level, island, seed++, true);
        }
        for (Island stone : stones(center)) {
            buildIsland(level, stone, seed++, false);
        }
        for (BlockPos updraft : updrafts(center)) {
            level.setBlock(updraft, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(updraft.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        // nuvens de lã soltas (também servem de apoio)
        for (int i = 0; i < 10; i++) {
            double angle = i * 0.83 + 0.4;
            double dist = 8.0 + ArenaTools.noise(i, 7, 99) * 18.0;
            BlockPos cloud = center.offset((int) Math.round(Math.cos(angle) * dist), (int) Math.round(ArenaTools.noise(i, 3, 5) * 12.0 - 2.0),
                    (int) Math.round(Math.sin(angle) * dist));
            buildCloud(level, cloud, i);
        }
    }

    private static void buildIsland(ServerLevel level, Island island, int seed, boolean decorate) {
        BlockPos top = island.top();
        int r = island.radius();
        int flags = Block.UPDATE_CLIENTS;
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                double edge = r + ArenaTools.noise(top.getX() + dx, top.getZ() + dz, seed) * 1.2 - 0.3;
                if (d > edge) {
                    continue;
                }
                // cone irregular para baixo
                int depth = 1 + (int) Math.round((edge - d) * 1.4 + ArenaTools.noise(dx, dz, seed + 31) * 2.0);
                for (int dy = 0; dy < depth + 1; dy++) {
                    BlockState state = dy == 0 ? Blocks.GRASS_BLOCK.defaultBlockState()
                            : dy <= 2 ? Blocks.DIRT.defaultBlockState()
                            : dy == depth ? Blocks.CALCITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
                    level.setBlock(top.offset(dx, -dy, dz), state, flags);
                }
                if (!decorate || d > r - 1 || ArenaTools.noise(dx * 3, dz * 5, seed + 7) > 0.18) {
                    continue;
                }
                // flores brancas e amarelas (o Louco carrega uma rosa branca)
                double pick = ArenaTools.noise(dx, dz * 7, seed + 3);
                BlockState flower = pick < 0.3 ? Blocks.OXEYE_DAISY.defaultBlockState()
                        : pick < 0.55 ? Blocks.WHITE_TULIP.defaultBlockState()
                        : pick < 0.75 ? Blocks.DANDELION.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState();
                level.setBlock(top.offset(dx, 1, dz), flower, flags);
            }
        }
    }

    private static void buildCloud(ServerLevel level, BlockPos center, int seed) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 1 || ArenaTools.noise(dx, dz, seed + 50) < 0.15) {
                    continue;
                }
                BlockPos pos = center.offset(dx, 0, dz);
                if (level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.WHITE_WOOL.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    // ----------------------------------------------------------------- durante a luta

    @Override
    public void tick(ServerLevel level, ArcanePlane.Fight fight, List<ServerPlayer> players) {
        List<BlockPos> updrafts = updrafts(fight.center());
        if (fight.ticks() % 4 == 0) {
            for (BlockPos pos : updrafts) {
                level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 2, 0.25, 0.4, 0.25, 0.05);
            }
        }
        for (ServerPlayer player : players) {
            if (player.isSpectator() || player.isShiftKeyDown()) {
                continue; // agachado não é levado (para atravessar a corrente de propósito)
            }
            for (BlockPos pos : updrafts) {
                double dx = player.getX() - (pos.getX() + 0.5);
                double dz = player.getZ() - (pos.getZ() + 0.5);
                double dy = player.getY() - (pos.getY() + 1.0);
                if (dx * dx + dz * dz < 0.8 * 0.8 && dy >= -0.1 && dy < 1.0 && player.getDeltaMovement().y < 0.6) {
                    launch(level, player);
                }
            }
        }
    }

    private static void launch(ServerLevel level, ServerPlayer player) {
        player.setDeltaMovement(player.getDeltaMovement().x, UPDRAFT_VELOCITY, player.getDeltaMovement().z);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 70, 0, false, false, true));
        level.sendParticles(ParticleTypes.GUST, player.getX(), player.getY(), player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    /** Caiu das ilhas: o vento devolve para a ilha central (com um pouco de dano), rindo. */
    @Override
    public boolean catchFall(ServerLevel level, ArcanePlane.Fight fight, ServerPlayer player) {
        BlockPos center = fight.center();
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
        Vec3 back = new Vec3(center.getX() + 0.5 + Math.cos(angle) * 3.0, center.getY() + 6.0, center.getZ() + 0.5 + Math.sin(angle) * 3.0);
        player.teleportTo(back.x, back.y, back.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
        player.hurtServer(level, level.damageSources().magic(), FALL_DAMAGE);
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, back.x, back.y, back.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, BlockPos.containing(back), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.0F, 1.6F);
        player.sendOverlayMessage(Component.translatable("message.mistech.fool.caught"));
        return true;
    }
}
