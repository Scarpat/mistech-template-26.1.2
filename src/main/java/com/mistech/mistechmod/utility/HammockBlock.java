package com.mistech.mistechmod.utility;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rede de Dormir: ocupa 2 blocos como a cama (PART = cabeceira/pés, FACING aponta para a cabeceira).
 * Só deixa deitar de DIA; quando todos dormem (cama/rede), o relógio pula para o anoitecer (13000)
 * em vez da manhã. Não muda o ponto de renascimento. A lógica de sono fica em {@link HammockEvents}.
 */
public class HammockBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<HammockBlock> CODEC = simpleCodec(HammockBlock::new);
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    /** Pano + armação: dá para pisar por cima e não sufoca quem deita. */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);

    public HammockBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT)
                .setValue(OCCUPIED, false));
    }

    @Override
    public MapCodec<HammockBlock> codec() {
        return CODEC;
    }

    /** O mundo trata a rede como cama (deitar, ficar deitado, posição e direção do jogador). */
    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, LivingEntity sleeper) {
        return true;
    }

    /** Dimensões onde a cama funciona "só no escuro" (Mundo Superior e parecidas): é onde a rede funciona. */
    public static boolean worksHere(Level level, BlockPos pos) {
        return level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos).canSleep() == BedRule.Rule.WHEN_DARK
                && level.dimensionType().defaultClock().isPresent();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (state.getValue(PART) != BedPart.HEAD) {
            pos = pos.relative(state.getValue(FACING));
            state = level.getBlockState(pos);
            if (!state.is(this)) {
                return InteractionResult.CONSUME;
            }
        }
        if (state.getValue(OCCUPIED)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.hammock.occupied"));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!worksHere(level, pos)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.hammock.cant_here"));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (level.isDarkOutside()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.hammock.night"));
            return InteractionResult.SUCCESS_SERVER;
        }
        // o resto (distância, bloqueio, monstros, dia) é resolvido em HammockEvents#onCanSleep
        player.startSleepInBed(pos).ifLeft(problem -> {
            if (problem.message() != null) {
                player.sendOverlayMessage(problem.message());
            }
        });
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Rede amortece a queda como a cama (sem quicar). */
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance * 0.5);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == neighbourDirection(state.getValue(PART), state.getValue(FACING))) {
            return neighbourState.is(this) && neighbourState.getValue(PART) != state.getValue(PART)
                    ? state.setValue(OCCUPIED, neighbourState.getValue(OCCUPIED))
                    : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    private static Direction neighbourDirection(BedPart part, Direction facing) {
        return part == BedPart.FOOT ? facing : facing.getOpposite();
    }

    /** Criativo: quebrar os pés não derruba a rede pela cabeceira. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(PART) == BedPart.FOOT) {
            BlockPos headPos = pos.relative(neighbourDirection(BedPart.FOOT, state.getValue(FACING)));
            BlockState headState = level.getBlockState(headPos);
            if (headState.is(this) && headState.getValue(PART) == BedPart.HEAD) {
                level.setBlock(headPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, headPos, Block.getId(headState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos head = context.getClickedPos().relative(facing);
        Level level = context.getLevel();
        return level.getBlockState(head).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(head)
                ? this.defaultBlockState().setValue(FACING, facing)
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        if (!level.isClientSide()) {
            BlockPos head = pos.relative(state.getValue(FACING));
            level.setBlock(head, state.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED);
    }
}
