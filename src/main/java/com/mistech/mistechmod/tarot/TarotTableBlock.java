package com.mistech.mistechmod.tarot;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mesa de Tarologia: nasce de uma Mesa de Escrita + Pedra Guia sobre o Círculo de Tarologia ({@link TarotField}).
 * Abre a tela com as abas Invocação e Forja. Clicar com equipamento de Oricalco Puro vincula um arcano ({@link GearArcana}).
 */
public class TarotTableBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TarotTableBlock> CODEC = simpleCodec(TarotTableBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 12.0, 0.0, 16.0, 14.5, 16.0),
            Block.box(1.0, 0.0, 1.0, 3.0, 12.0, 3.0),
            Block.box(13.0, 0.0, 1.0, 15.0, 12.0, 3.0),
            Block.box(1.0, 0.0, 13.0, 3.0, 12.0, 15.0),
            Block.box(13.0, 0.0, 13.0, 15.0, 12.0, 15.0));

    public TarotTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<TarotTableBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            TarotService.open(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Cartas girando: partículas de feitiço e encantamento quando o círculo está completo. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0 && TarotField.isValid(level, pos)) {
            level.addParticle(ParticleTypes.WITCH,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.0, pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0.0, 0.02, 0.0);
            for (int[] o : TarotField.PEDESTAL_OFFSETS) {
                if (random.nextInt(6) == 0) {
                    level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                            o[0], -0.3, o[1]);
                }
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
