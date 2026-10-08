package com.mistech.mistechmod.block;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Porta-Incenso: bloco pequeno que queima incensos automaticamente e espalha o Véu de Incenso num raio de 10 blocos. */
public class IncenseHolderBlock extends BaseEntityBlock {
    public static final MapCodec<IncenseHolderBlock> CODEC = simpleCodec(IncenseHolderBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** true quando o incenso queimando é o Incenso Mágico (muda o modelo e as partículas). */
    public static final BooleanProperty MAGIC = BooleanProperty.create("magic");

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Tábua comprida (14 x 1.5 x 4 pixels) no estilo porta-incenso caseiro
    private static final VoxelShape SHAPE_X = Block.box(1.0, 0.0, 6.0, 15.0, 2.5, 10.0);
    private static final VoxelShape SHAPE_Z = Block.box(6.0, 0.0, 1.0, 10.0, 2.5, 15.0);

    /** Ponta acesa do incenso inclinado, relativa ao centro do bloco, na direção FACING (em blocos). */
    private static final double TIP_FORWARD = 0.34;
    private static final double TIP_HEIGHT = 0.50;

    public IncenseHolderBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false).setValue(MAGIC, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<IncenseHolderBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IncenseHolderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, MistechBlockEntities.INCENSE_HOLDER.get(), IncenseHolderBlockEntity::serverTick);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // o incenso aponta para longe de quem colocou
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IncenseHolderBlockEntity holder) {
            player.openMenu(holder);
        }
        return InteractionResult.SUCCESS;
    }

    /** Fumaça subindo da ponta do incenso (só visual, lado do cliente). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * TIP_FORWARD + (random.nextFloat() - 0.5) * 0.03;
        double y = pos.getY() + TIP_HEIGHT;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * TIP_FORWARD + (random.nextFloat() - 0.5) * 0.03;
        if (state.getValue(MAGIC)) {
            // Incenso Mágico: fumaça escura com faíscas roxas
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.01, 0.0);
            if (random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.WITCH, x, y + 0.15, z, 0.0, 0.0, 0.0);
            }
            if (random.nextInt(5) == 0) {
                level.addParticle(ParticleTypes.PORTAL, x, y + 0.1, z,
                        (random.nextFloat() - 0.5) * 0.4, random.nextFloat() * 0.3, (random.nextFloat() - 0.5) * 0.4);
            }
            return;
        }
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.015, 0.0);
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.WITCH, x, y + 0.2, z, 0.0, 0.0, 0.0); // toque "mágico"
        }
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
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, MAGIC, FACING);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
