package com.mistech.mistechmod.block;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pilar de Conexão Astral: 3 blocos de altura (base de madeira turva, meio de lápis, ponta de magia impura) que vai
 * afinando até a ponta. Nasce do multibloco Tronco Turvo + Bloco de Lápis + Bloco de Magia Impura com a Pedra Guia.
 * Quebrar qualquer parte derruba o pilar inteiro (1 item). Em cima dos blocos de ouro dos pontos cardeais do
 * Campo de Astrologia, cada pilar aumenta os bônus da astrologia em 12,5%.
 */
public class AstralPillarBlock extends Block {
    public static final MapCodec<AstralPillarBlock> CODEC = simpleCodec(AstralPillarBlock::new);

    public enum Part implements StringRepresentable {
        BOTTOM("bottom", 0), MIDDLE("middle", 1), TOP("top", 2);

        private final String name;
        private final int index;

        Part(String name, int index) {
            this.name = name;
            this.index = index;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public int index() {
            return this.index;
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    private static final VoxelShape BOTTOM_SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 4, 15), Block.box(2, 4, 2, 14, 16, 14));
    private static final VoxelShape MIDDLE_SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 7, 13), Block.box(4, 7, 4, 12, 16, 12));
    private static final VoxelShape TOP_SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 6, 11), Block.box(6, 6, 6, 10, 11, 10),
            Block.box(7, 11, 7, 9, 16, 9));

    public AstralPillarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.BOTTOM));
    }

    @Override
    protected MapCodec<AstralPillarBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case BOTTOM -> BOTTOM_SHAPE;
            case MIDDLE -> MIDDLE_SHAPE;
            case TOP -> TOP_SHAPE;
        };
    }

    public static BlockPos bottomOf(BlockPos pos, BlockState state) {
        return pos.below(state.getValue(PART).index());
    }

    /** Coloca as 3 partes a partir da base (usado pelo item e pela transformação do multibloco). */
    public static void placePillar(Level level, BlockPos bottom, BlockState base, int flags) {
        level.setBlock(bottom, base.setValue(PART, Part.BOTTOM), flags);
        level.setBlock(bottom.above(), base.setValue(PART, Part.MIDDLE), flags);
        level.setBlock(bottom.above(2), base.setValue(PART, Part.TOP), flags);
    }

    // ----- colocar -----

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() + 2 < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)
                && level.getBlockState(pos.above(2)).canBeReplaced(context)) {
            return this.defaultBlockState();
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(PART, Part.MIDDLE), Block.UPDATE_ALL);
        level.setBlock(pos.above(2), state.setValue(PART, Part.TOP), Block.UPDATE_ALL);
    }

    // ----- quebrar: o pilar sai inteiro -----

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Part part = state.getValue(PART);
        boolean needsBelow = part != Part.BOTTOM;
        boolean needsAbove = part != Part.TOP;
        if (direction == Direction.DOWN && needsBelow && !isPart(neighborState, part.index() - 1)
                || direction == Direction.UP && needsAbove && !isPart(neighborState, part.index() + 1)) {
            return Blocks.AIR.defaultBlockState(); // a parte vizinha sumiu: esta some junto (sem drop)
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    private boolean isPart(BlockState state, int index) {
        return state.is(this) && state.getValue(PART).index() == index;
    }

    /** O item só sai da base (loot table). Quebrando o meio ou a ponta, a base solta o item aqui. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(PART) != Part.BOTTOM) {
            BlockPos bottom = bottomOf(pos, state);
            BlockState bottomState = level.getBlockState(bottom);
            if (bottomState.is(this)) {
                if (!player.preventsBlockDrops() && player.hasCorrectToolForDrops(bottomState, level, bottom)) {
                    dropResources(bottomState, level, bottom, null, player, player.getMainHandItem());
                }
                level.setBlock(bottom, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) == Part.TOP && random.nextInt(3) == 0) {
            level.addParticle(random.nextBoolean() ? ParticleTypes.END_ROD : ParticleTypes.WITCH,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 1.0, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3,
                    0.0, 0.03, 0.0);
        }
    }
}
