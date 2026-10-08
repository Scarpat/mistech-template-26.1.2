package com.mistech.mistechmod.block;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Chaleira: esquenta sozinha (a Runa de Fogo vai na receita), então não precisa do suporte de poções nem de pó de blaze.
 * 1) Balde de água (6 xícaras) ou garrafas de água (2 xícaras cada).
 * 2) Um ingrediente: Pó de Café ou Bagas de Beladona. Em alguns segundos fica pronto.
 * 3) Pegue com Xícaras: 1 por dose.
 * Com água pura, a garrafa de vidro pega a água de volta (2 doses).
 */
public class KettleBlock extends BaseEntityBlock {
    public static final MapCodec<KettleBlock> CODEC = simpleCodec(KettleBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);

    public enum Stage implements StringRepresentable {
        EMPTY("empty"), WATER("water"), BREWING("brewing"), READY("ready");

        private final String name;

        Stage(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public KettleBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STAGE, Stage.EMPTY));
    }

    @Override
    public MapCodec<KettleBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KettleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, MistechBlockEntities.KETTLE.get(), KettleBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            // o cliente não conhece o conteúdo; o servidor decide
            return KettleBlockEntity.isKettleItem(stack) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof KettleBlockEntity kettle) || !kettle.canUse(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        kettle.use(player, hand, stack);
        return InteractionResult.SUCCESS;
    }

    /** Mão vazia: diz o que está na chaleira. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof KettleBlockEntity kettle) {
            player.sendOverlayMessage(kettle.statusMessage());
        }
        return InteractionResult.SUCCESS;
    }

    /** Fervendo: vapor forte saindo do bico. Pronto: um fiozinho de vapor. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Stage stage = state.getValue(STAGE);
        if (stage == Stage.EMPTY) {
            return;
        }
        // bico: lado leste do modelo (virado para o norte); gira junto com a chaleira
        Direction right = state.getValue(FACING).getClockWise();
        double x = pos.getX() + 0.5 + right.getStepX() * 0.42;
        double z = pos.getZ() + 0.5 + right.getStepZ() * 0.42;
        double y = pos.getY() + 0.42;
        if (stage == Stage.BREWING) {
            if (random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.CLOUD, x, y, z, right.getStepX() * 0.02, 0.06, right.getStepZ() * 0.02);
            }
            if (random.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 0.05,
                        pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.0, 0.0);
            }
        } else if (random.nextInt(stage == Stage.READY ? 4 : 10) == 0) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, y, z, 0.0, 0.03, 0.0);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof KettleBlockEntity kettle ? kettle.comparatorSignal() : 0;
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
        builder.add(FACING, STAGE);
    }
}
