package com.mistech.mistechmod.purity.block;

import com.mistech.mistechmod.purity.PurityContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Solo Maculado: terra contaminada pela Borra Impura, pelo Refluxo, pelo Destilador cheio ou por essência parada.
 * Mata plantas em cima, deixa monstros nascerem com luz até 7 e macula quem pisa.
 * Espalha para a terra/grama vizinha com força +1; com força 3 não espalha mais (a mancha tem tamanho limitado).
 */
public class TaintedSoilBlock extends Block {
    public static final MapCodec<TaintedSoilBlock> CODEC = simpleCodec(TaintedSoilBlock::new);
    public static final IntegerProperty STRENGTH = IntegerProperty.create("strength", 0, 3);
    public static final int MAX_STRENGTH = 3;

    public TaintedSoilBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STRENGTH, 0));
    }

    @Override
    public MapCodec<TaintedSoilBlock> codec() {
        return CODEC;
    }

    /** Terra/grama comum que pode virar Solo Maculado. */
    public static boolean canTaint(BlockState state) {
        return state.is(BlockTags.DIRT) && !state.is(PurityContent.TAINTED_SOIL.get());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // plantas em cima murcham
        BlockPos above = pos.above();
        BlockState plant = level.getBlockState(above);
        if (plant.getBlock() instanceof VegetationBlock) {
            level.destroyBlock(above, false);
            level.sendParticles(ParticleTypes.SQUID_INK, above.getX() + 0.5, above.getY() + 0.3, above.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.01);
        }
        // espalha
        int strength = state.getValue(STRENGTH);
        if (strength < MAX_STRENGTH && random.nextInt(3) == 0) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            if (canTaint(level.getBlockState(target))) {
                level.setBlock(target, this.defaultBlockState().setValue(STRENGTH, strength + 1), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0 && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ParticleTypes.WITCH, pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(),
                    0.0, 0.02, 0.0);
        }
    }

    /** Limpa (volta a ser terra). */
    public static void cleanse(Level level, BlockPos pos) {
        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.25, 0.1, 0.25, 0.01);
        }
    }

    /**
     * Macula até {@code count} blocos de terra/grama perto de {@code center} (raio horizontal {@code radius}, 3 de altura).
     * @return quantos blocos foram maculados
     */
    public static int contaminateNear(ServerLevel level, BlockPos center, int radius, int count, int strength) {
        int done = 0;
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < count * 8 && done < count; attempt++) {
            BlockPos p = center.offset(random.nextInt(radius * 2 + 1) - radius, random.nextInt(3) - 1 - (attempt % 2), random.nextInt(radius * 2 + 1) - radius);
            if (canTaint(level.getBlockState(p))) {
                level.setBlock(p, PurityContent.TAINTED_SOIL.get().defaultBlockState().setValue(STRENGTH, Math.min(MAX_STRENGTH, strength)), Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.SQUID_INK, p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 5, 0.3, 0.1, 0.3, 0.01);
                done++;
            }
        }
        if (done > 0) {
            level.playSound(null, center, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 0.8F, 0.7F);
        }
        return done;
    }

    /** Limpa até {@code count} blocos maculados num raio (procura ao acaso). @return quantos limpou */
    public static int cleanseNear(Level level, BlockPos center, int radius, int count) {
        int done = 0;
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < count * 6 && done < count; attempt++) {
            BlockPos p = center.offset(random.nextInt(radius * 2 + 1) - radius, random.nextInt(5) - 2, random.nextInt(radius * 2 + 1) - radius);
            if (level.getBlockState(p).is(PurityContent.TAINTED_SOIL.get())) {
                cleanse(level, p);
                done++;
            }
        }
        return done;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STRENGTH);
    }
}
