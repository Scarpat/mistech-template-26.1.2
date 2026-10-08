package com.mistech.mistechmod.block.ritual;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

/** Pedestal Ritual: botão direito com item coloca 1; mão vazia tira. Funil também coloca/tira. */
public class RitualPedestalBlock extends BaseEntityBlock {
    public static final MapCodec<RitualPedestalBlock> CODEC = simpleCodec(RitualPedestalBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 2, 14),
            Block.box(4, 2, 4, 12, 11, 12),
            Block.box(2, 11, 2, 14, 13, 14));

    /** Variante de Carvalho Pálido (receita com tronco de carvalho pálido no lugar do Tronco Turvo). */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty PALE =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("pale");

    public RitualPedestalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PALE, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(PALE);
    }

    @Override
    public MapCodec<RitualPedestalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitualPedestalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof RitualPedestalBlockEntity pedestal) || !pedestal.getStored().isEmpty() || stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (pedestal.locked()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.already_running"));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            pedestal.setStored(stack.split(1));
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof RitualPedestalBlockEntity pedestal) || pedestal.getStored().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (pedestal.locked()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.already_running"));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            player.getInventory().placeItemBackInInventory(pedestal.getStored());
            pedestal.setStored(ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
