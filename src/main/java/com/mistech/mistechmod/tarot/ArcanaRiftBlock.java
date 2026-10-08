package com.mistech.mistechmod.tarot;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
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
 * Fenda Arcana: portal de 2 blocos que a Carta Invertida abre. Qualquer jogador pode entrar; todos que entram pela
 * mesma fenda lutam juntos (ver {@link ArcanePlane}). Fecha depois de {@link ArcanaRiftBlockEntity#LIFETIME} ticks ou quando
 * a luta acaba — se ninguém entrou, a carta cai de volta.
 */
public class ArcanaRiftBlock extends BaseEntityBlock {
    public static final MapCodec<ArcanaRiftBlock> CODEC = simpleCodec(ArcanaRiftBlock::new);

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

    public ArcanaRiftBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, Half.LOWER).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<ArcanaRiftBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    /** Abre a fenda (2 blocos) em {@code pos}, virada para {@code facing}. */
    public static void open(ServerLevel level, BlockPos pos, Direction facing, TarotArcana arcana, ServerPlayer owner) {
        BlockState lower = TarotContent.ARCANA_RIFT.get().defaultBlockState().setValue(FACING, facing);
        level.setBlock(pos, lower, Block.UPDATE_ALL);
        level.setBlock(pos.above(), lower.setValue(HALF, Half.UPPER), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof ArcanaRiftBlockEntity rift) {
            rift.setup(arcana, owner);
        }
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6F, 1.6F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.5F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 80, 0.3, 0.8, 0.3, 0.15);
    }

    public static BlockPos baseOf(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == Half.UPPER ? pos.below() : pos;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == Half.LOWER ? new ArcanaRiftBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || state.getValue(HALF) != Half.LOWER ? null
                : createTickerHelper(type, TarotContent.ARCANA_RIFT_BE.get(), ArcanaRiftBlockEntity::serverTick);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier,
                                boolean isPrecise) {
        if (entity instanceof ServerPlayer player && level.getBlockEntity(baseOf(pos, state)) instanceof ArcanaRiftBlockEntity rift) {
            rift.tryEnter(player);
        }
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

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.PORTAL, x + (random.nextDouble() - 0.5) * 0.8, pos.getY() + random.nextDouble(),
                    z + (random.nextDouble() - 0.5) * 0.8, (random.nextDouble() - 0.5) * 0.5, -random.nextDouble() * 0.3,
                    (random.nextDouble() - 0.5) * 0.5);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(new DustParticleOptions(0xB04CFF, 1.2F), x + (random.nextDouble() - 0.5) * 0.9,
                    pos.getY() + random.nextDouble(), z + (random.nextDouble() - 0.5) * 0.9, 0.0, 0.02, 0.0);
        }
        if (random.nextInt(60) == 0) {
            level.playLocalSound(pos, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.3F, 1.5F + random.nextFloat() * 0.3F, false);
        }
    }
}
