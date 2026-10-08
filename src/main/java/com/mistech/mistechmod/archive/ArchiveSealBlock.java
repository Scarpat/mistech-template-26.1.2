package com.mistech.mistechmod.archive;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.arcane.ArcaneAttachedBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Selo do Arquivo: preso num recipiente (Barril Espacial, baú, Câmara da Mineradora...), faz o conteúdo dele aparecer no Arquivo
 * e receber itens do Arquivo — como um "storage bus".
 */
public class ArchiveSealBlock extends ArcaneAttachedBlock {
    public static final MapCodec<ArchiveSealBlock> CODEC = simpleCodec(ArchiveSealBlock::new);

    public ArchiveSealBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ArchiveSealBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArchiveMemberBlockEntity(ArchiveContent.SEAL_BE.get(), pos, state, ArchiveRegistry.Kind.SEAL);
    }
}
