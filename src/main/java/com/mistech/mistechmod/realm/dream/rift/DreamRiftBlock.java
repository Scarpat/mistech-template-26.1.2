package com.mistech.mistechmod.realm.dream.rift;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.DreamRiftContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fenda Onírica: "quebra" de 2 blocos que o Fragmento de Sonho abre na Superfície. Enquanto aberta, espalha o Sonho em volta
 * ({@link DreamRiftBlockEntity}); quando a região termina de mudar, a fenda some. Não dá para quebrar nem atravessar.
 */
public class DreamRiftBlock extends BaseEntityBlock {
    public static final MapCodec<DreamRiftBlock> CODEC = simpleCodec(DreamRiftBlock::new);

    public enum Half implements StringRepresentable {
        LOWER("lower"), UPPER("upper");

        private final String name;

        Half(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE_NS = Block.box(1, 0, 7, 15, 16, 9);
    private static final VoxelShape SHAPE_EW = Block.box(7, 0, 1, 9, 16, 15);

    public DreamRiftBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, Half.LOWER).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<DreamRiftBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == Half.LOWER ? new DreamRiftBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || state.getValue(HALF) != Half.LOWER ? null
                : createTickerHelper(type, DreamRiftContent.DREAM_RIFT_BE.get(), DreamRiftBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** Metade sem a outra some (sem drop). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Half half = state.getValue(HALF);
        if (half == Half.LOWER && direction == Direction.UP && !(neighborState.is(this) && neighborState.getValue(HALF) == Half.UPPER)
                || half == Half.UPPER && direction == Direction.DOWN && !(neighborState.is(this) && neighborState.getValue(HALF) == Half.LOWER)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    private static final int[] DREAM_COLORS = {0xC080E0, 0xF0A8E0, 0xA8E8FF, 0xFFF4C8, 0x9A8AF0};

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        // pó de sonho escapando da fenda para os lados
        for (int i = 0; i < 2; i++) {
            int color = DREAM_COLORS[random.nextInt(DREAM_COLORS.length)];
            level.addParticle(new DustParticleOptions(color, 1.0F + random.nextFloat()), x + (random.nextDouble() - 0.5) * 0.6,
                    pos.getY() + random.nextDouble(), z + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.END_ROD, x, pos.getY() + random.nextDouble(), z, (random.nextDouble() - 0.5) * 0.15,
                    random.nextDouble() * 0.05, (random.nextDouble() - 0.5) * 0.15);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, x + (random.nextDouble() - 0.5) * 0.8, pos.getY() + random.nextDouble(),
                    z + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(50) == 0) {
            level.playLocalSound(pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 0.5F + random.nextFloat() * 0.4F, false);
        }
    }
}
