package com.mistech.mistechmod.block;

import com.mistech.mistechmod.registry.GnomeContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Muda de Árvore Turva: quando vira árvore, tem {@link #MUSHROOM_CHANCE} de nascer um Cogumelo Amarelado por perto. */
public class MurkySaplingBlock extends SaplingBlock {
    public static final float MUSHROOM_CHANCE = 0.10F;
    public static final int MUSHROOM_RADIUS = 4;

    public static final MapCodec<MurkySaplingBlock> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(
            i -> i.group(TreeGrower.CODEC.fieldOf("tree").forGetter(b -> b.grower), propertiesCodec()).apply(i, MurkySaplingBlock::new));

    private final TreeGrower grower;

    public MurkySaplingBlock(TreeGrower grower, BlockBehaviour.Properties properties) {
        super(grower, properties);
        this.grower = grower;
    }

    @Override
    public MapCodec<MurkySaplingBlock> codec() {
        return CODEC;
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        super.advanceTree(level, pos, state, random);
        // cresceu de verdade? (a muda virou tronco)
        if (level.getBlockState(pos).is(BlockTags.LOGS) && random.nextFloat() < MUSHROOM_CHANCE) {
            placeMushroom(level, pos, random);
        }
    }

    private static void placeMushroom(ServerLevel level, BlockPos center, RandomSource random) {
        BlockState mushroom = GnomeContent.YELLOW_MUSHROOM.get().defaultBlockState();
        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos pos = center.offset(random.nextInt(MUSHROOM_RADIUS * 2 + 1) - MUSHROOM_RADIUS,
                    random.nextInt(5) - 2, random.nextInt(MUSHROOM_RADIUS * 2 + 1) - MUSHROOM_RADIUS);
            if (!pos.equals(center) && level.getBlockState(pos).isAir() && mushroom.canSurvive(level, pos)) {
                level.setBlock(pos, mushroom, Block.UPDATE_ALL);
                return;
            }
        }
    }
}
