package com.mistech.mistechmod.archive;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Núcleo do Arquivo: 9 espaços para Pergaminhos Espaciais. Precisa de energia. */
public class ArchiveCoreBlock extends BaseEntityBlock {
    public static final MapCodec<ArchiveCoreBlock> CODEC = simpleCodec(ArchiveCoreBlock::new);

    public ArchiveCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ArchiveCoreBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArchiveCoreBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ArchiveContent.CORE_BE.get(), ArchiveCoreBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof ArchiveCoreBlockEntity be) {
            player.openMenu(be);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 1.2, random.nextDouble() * 0.5, (random.nextDouble() - 0.5) * 1.2);
        }
    }
}
