package com.mistech.mistechmod.archive;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.arcane.ArcaneAttachedBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Boca do Arquivo: presa num recipiente/máquina. Modo <b>puxar</b> (tira do recipiente e guarda no Arquivo) ou
 * <b>empurrar</b> (tira do Arquivo e põe no recipiente; só os itens do filtro).
 * Botão direito com item: põe/tira do filtro (até 9). Mão vazia: troca o modo. Agachado com mão vazia: limpa o filtro.
 */
public class ArchivePortBlock extends ArcaneAttachedBlock {
    public static final MapCodec<ArchivePortBlock> CODEC = simpleCodec(ArchivePortBlock::new);

    public ArchivePortBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ArchivePortBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArchivePortBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ArchiveContent.PORT_BE.get(), ArchivePortBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof ArchivePortBlockEntity port) {
            port.toggleFilter(player, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof ArchivePortBlockEntity port) {
            if (player.isSecondaryUseActive()) {
                port.clearFilter(player);
            } else {
                port.toggleMode(player);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
