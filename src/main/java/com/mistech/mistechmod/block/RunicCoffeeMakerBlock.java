package com.mistech.mistechmod.block;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cafeteira Rúnica: a Chaleira avançada (ver {@link RunicCoffeeMakerBlockEntity}).
 * Botão direito com ingrediente, Xícaras vazias, balde, garrafa ou frasco; mão vazia pega as bebidas;
 * agachado + mão vazia devolve ingrediente e Xícaras; funis e canos funcionam.
 */
public class RunicCoffeeMakerBlock extends BaseEntityBlock {
    public static final MapCodec<RunicCoffeeMakerBlock> CODEC = simpleCodec(RunicCoffeeMakerBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    /** Nível do líquido de cada jarra (0 a 4): bebidas prontas + em preparo da linha. */
    public static final IntegerProperty[] JARS = {
            IntegerProperty.create("jar0", 0, 4), IntegerProperty.create("jar1", 0, 4), IntegerProperty.create("jar2", 0, 4)};
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public RunicCoffeeMakerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WORKING, false));
    }

    @Override
    public MapCodec<RunicCoffeeMakerBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RunicCoffeeMakerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, MistechBlockEntities.RUNIC_COFFEE_MAKER.get(), RunicCoffeeMakerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        boolean relevant = RunicCoffeeMakerBlockEntity.isIngredient(stack) || stack.is(com.mistech.mistechmod.Mistech.CUP.get())
                || FluidInteraction.isFluidItem(stack);
        if (level.isClientSide()) {
            // o cliente não conhece o conteúdo; o servidor decide
            return relevant ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!relevant || !(level.getBlockEntity(pos) instanceof RunicCoffeeMakerBlockEntity maker)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return maker.useItem(player, hand, stack, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RunicCoffeeMakerBlockEntity maker) {
            maker.useEmpty(player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Trabalhando: fumaça na cabeça, brasas embaixo e vapor no bico. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(WORKING)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.CLOUD, x + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 1.02, z + (random.nextDouble() - 0.5) * 0.4,
                    0.0, 0.04, 0.0);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5) * 0.5, pos.getY() + 0.08,
                    z + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 0.55, z + (random.nextDouble() - 0.5) * 0.3,
                    0.0, 0.02, 0.0);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof RunicCoffeeMakerBlockEntity maker ? maker.comparatorSignal() : 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WORKING, JARS[0], JARS[1], JARS[2]);
    }
}
