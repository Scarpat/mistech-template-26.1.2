package com.mistech.mistechmod.block.goblin;

import java.util.ArrayList;
import java.util.List;

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
import net.minecraft.world.phys.Vec3;

/**
 * Totem Duende: pilar de madeira entalhado com um rosto. Botão direito gira 90°.
 * Puzzle: quando todos os totens do grupo (até 6 blocos um do outro) olham para o centro deles,
 * os olhos acendem e o Selo Corrompido mais perto some.
 */
public class GoblinTotemBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<GoblinTotemBlock> CODEC = simpleCodec(GoblinTotemBlock::new);
    public static final BooleanProperty SOLVED = BooleanProperty.create("solved");
    private static final int GROUP_RADIUS = 6;

    public GoblinTotemBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SOLVED, false));
    }

    @Override
    public MapCodec<GoblinTotemBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(SOLVED)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        server.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
        server.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7F, 0.6F + server.getRandom().nextFloat() * 0.2F);
        checkSolved(server, pos, player);
        return InteractionResult.SUCCESS;
    }

    private void checkSolved(ServerLevel level, BlockPos from, Player player) {
        List<BlockPos> group = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(from.offset(-GROUP_RADIUS * 2, -2, -GROUP_RADIUS * 2), from.offset(GROUP_RADIUS * 2, 2, GROUP_RADIUS * 2))) {
            if (level.getBlockState(p).is(this)) {
                group.add(p.immutable());
            }
        }
        if (group.size() < 2) {
            return;
        }
        Vec3 center = Vec3.ZERO;
        for (BlockPos p : group) {
            center = center.add(Vec3.atCenterOf(p));
        }
        center = center.scale(1.0 / group.size());
        int correct = 0;
        for (BlockPos p : group) {
            if (level.getBlockState(p).getValue(FACING) == towards(p, center)) {
                correct++;
            }
        }
        if (correct < group.size()) {
            if (correct > 0) {
                player.sendOverlayMessage(Component.translatable("message.mistech.goblin_totem.progress", correct, group.size()));
            }
            return;
        }
        for (BlockPos p : group) {
            level.setBlock(p, level.getBlockState(p).setValue(SOLVED, true), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX() + 0.5, p.getY() + 0.8, p.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        }
        BlockPos c = BlockPos.containing(center);
        level.playSound(null, c, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS, 1.0F, 0.7F);
        GoblinSealBlock.dissolveNearest(level, c, 10);
        player.sendOverlayMessage(Component.translatable("message.mistech.goblin_totem.solved"));
    }

    /** Direção (horizontal) do totem para o centro do grupo; null quando fica exatamente na diagonal. */
    static @Nullable Direction towards(BlockPos p, Vec3 center) {
        double dx = center.x - (p.getX() + 0.5);
        double dz = center.z - (p.getZ() + 0.5);
        if (Math.abs(Math.abs(dx) - Math.abs(dz)) < 0.01) {
            return null;
        }
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SOLVED);
    }
}
