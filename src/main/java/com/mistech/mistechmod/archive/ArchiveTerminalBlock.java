package com.mistech.mistechmod.archive;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Mesa do Arquivo: abre a grade do Arquivo cujo Núcleo alcança esta mesa. */
public class ArchiveTerminalBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ArchiveTerminalBlock> CODEC = simpleCodec(ArchiveTerminalBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);

    public ArchiveTerminalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<ArchiveTerminalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            ArchiveCoreBlockEntity core = ArchiveRegistry.coreFor(level, pos);
            if (core == null) {
                player.sendOverlayMessage(Component.translatable("message.mistech.archive.no_core").withStyle(ChatFormatting.RED));
                return InteractionResult.SUCCESS;
            }
            open(player, server, core.getBlockPos(), p -> p.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0
                    && p.level() == level && level.getBlockState(pos).is(state.getBlock()));
        }
        return InteractionResult.SUCCESS;
    }

    public static void open(Player player, ServerLevel coreLevel, BlockPos corePos, java.util.function.Predicate<Player> valid) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ArchiveTerminalMenu(id, inventory, coreLevel, corePos, valid),
                Component.translatable("container.mistech.archive")));
    }
}
