package com.mistech.mistechmod.extraplanar;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.FluidInteraction;
import com.mistech.mistechmod.block.FluidTank;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tanque Extraplanar: igual ao Tanque Rúnico, mas o líquido é dividido com todos os tanques de mesmo código.
 * Shift + botão direito com a mão vazia define o código; mão vazia mostra o código e o nível.
 */
public class ExtraplanarTankBlock extends BaseEntityBlock {
    public static final MapCodec<ExtraplanarTankBlock> CODEC = simpleCodec(ExtraplanarTankBlock::new);
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public ExtraplanarTankBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ExtraplanarTankBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtraplanarTankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!FluidInteraction.isFluidItem(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ExtraplanarTankBlockEntity tank) {
            FluidTank linked = tank.linkedTank();
            if (linked == null) {
                player.sendOverlayMessage(Component.translatable("message.mistech.extraplanar.no_code"));
                return InteractionResult.SUCCESS;
            }
            if (FluidInteraction.interact(player, hand, level, pos, linked)) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isSecondaryUseActive()) {
            if (level.isClientSide()) {
                com.mistech.mistechmod.extraplanar.client.ExtraplanarClient.openCodeScreen(pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ExtraplanarTankBlockEntity tank) {
            FluidTank linked = tank.linkedTank();
            if (linked == null) {
                player.sendOverlayMessage(Component.translatable("message.mistech.extraplanar.no_code"));
            } else if (linked.isEmpty()) {
                player.sendOverlayMessage(Component.translatable("message.mistech.extraplanar.tank_empty", tank.getCode()));
            } else {
                player.sendOverlayMessage(Component.translatable("message.mistech.extraplanar.tank", tank.getCode(),
                        linked.getFluid().getHoverName(), String.format(java.util.Locale.ROOT, "%.1f", linked.getAmount() / 1000.0),
                        ExtraplanarData.TANK_CAPACITY / 1000));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof ExtraplanarTankBlockEntity tank ? tank.getComparatorLevel() : 0;
    }
}
