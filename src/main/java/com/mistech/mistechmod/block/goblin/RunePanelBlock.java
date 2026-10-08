package com.mistech.mistechmod.block.goblin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Pedra Rúnica (puzzle "apaga-acende"): tocar numa pedra inverte ela e as vizinhas de cima, de baixo e dos lados
 * (no plano da parede). Quando todas as pedras do painel ficam acesas, o Selo Corrompido mais perto some.
 */
public class RunePanelBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RunePanelBlock> CODEC = simpleCodec(RunePanelBlock::new);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final BooleanProperty SOLVED = BooleanProperty.create("solved");

    public RunePanelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(SOLVED, false));
    }

    @Override
    public MapCodec<RunePanelBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Vizinhos no plano da parede (cima, baixo e os dois lados). */
    public static List<BlockPos> planeNeighbors(BlockPos pos, Direction facing) {
        List<BlockPos> list = new ArrayList<>(4);
        list.add(pos.above());
        list.add(pos.below());
        list.add(pos.relative(facing.getClockWise()));
        list.add(pos.relative(facing.getCounterClockWise()));
        return list;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(SOLVED)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        Direction facing = state.getValue(FACING);
        toggle(server, pos, facing);
        for (BlockPos n : planeNeighbors(pos, facing)) {
            toggle(server, n, facing);
        }
        server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.5F + server.getRandom().nextFloat() * 0.4F);

        // painel inteiro aceso?
        Set<BlockPos> panel = collect(server, pos, facing);
        for (BlockPos p : panel) {
            if (!server.getBlockState(p).getValue(LIT)) {
                return InteractionResult.SUCCESS;
            }
        }
        for (BlockPos p : panel) {
            server.setBlock(p, server.getBlockState(p).setValue(SOLVED, true), Block.UPDATE_ALL);
            server.sendParticles(ParticleTypes.ENCHANT, p.getX() + 0.5 + facing.getStepX() * 0.6, p.getY() + 0.5,
                    p.getZ() + 0.5 + facing.getStepZ() * 0.6, 10, 0.2, 0.2, 0.2, 0.5);
        }
        server.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        GoblinSealBlock.dissolveNearest(server, pos, 10);
        player.sendOverlayMessage(Component.translatable("message.mistech.rune_panel.solved"));
        return InteractionResult.SUCCESS;
    }

    private void toggle(ServerLevel level, BlockPos p, Direction facing) {
        BlockState s = level.getBlockState(p);
        if (s.is(this) && s.getValue(FACING) == facing) {
            level.setBlock(p, s.setValue(LIT, !s.getValue(LIT)), Block.UPDATE_ALL);
        }
    }

    private Set<BlockPos> collect(ServerLevel level, BlockPos start, Direction facing) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty() && seen.size() < 64) {
            BlockPos p = queue.poll();
            for (BlockPos n : planeNeighbors(p, facing)) {
                BlockState s = level.getBlockState(n);
                if (s.is(this) && s.getValue(FACING) == facing && seen.add(n)) {
                    queue.add(n);
                }
            }
        }
        return seen;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, SOLVED);
    }
}
