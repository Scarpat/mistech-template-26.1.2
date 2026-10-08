package com.mistech.mistechmod.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pote de Vidro.
 * - Botão direito com item: guarda a pilha inteira (Shift = só 1).
 * - Botão direito com a mão vazia: tira 1 (Shift = tira tudo).
 * - Um raio caindo perto de um pote VAZIO transforma ele em Raio Engarrafado.
 */
public class GlassJarBlock extends BaseEntityBlock {
    public static final MapCodec<GlassJarBlock> CODEC = simpleCodec(GlassJarBlock::new);
    public static final VoxelShape SHAPE = Shapes.or(
            Block.box(3.0, 0.0, 3.0, 13.0, 11.0, 13.0),   // vidro
            Block.box(3.5, 11.0, 3.5, 12.5, 13.0, 12.5)); // tampa (laje de madeira)

    public GlassJarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<GlassJarBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GlassJarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof GlassJarBlockEntity jar) || jar.spaceFor(stack) <= 0) {
            return InteractionResult.TRY_WITH_EMPTY_HAND; // mão vazia / item diferente: tenta retirar
        }
        if (!level.isClientSide()) {
            int wanted = player.isShiftKeyDown() ? 1 : stack.getCount();
            int moved = jar.insert(stack, wanted);
            if (!player.getAbilities().instabuild) {
                stack.shrink(moved);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.3F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof GlassJarBlockEntity jar) || jar.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack taken = jar.take(player.isShiftKeyDown() ? jar.getStored().getCount() : 1);
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.3F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Isca de Gnomo dentro do pote: o servidor conta o tempo e às vezes um gnomo entra (ver GlassJarBlockEntity). */
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.@org.jspecify.annotations.Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, com.mistech.mistechmod.registry.MistechBlockEntities.GLASS_JAR.get(), GlassJarBlockEntity::serverTick);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof GlassJarBlockEntity jar ? jar.getComparatorSignal() : 0;
    }
}
