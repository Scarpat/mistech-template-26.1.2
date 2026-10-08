package com.mistech.mistechmod.machine;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Tuneladora (feita com Runa da Terra): broca virada para onde o jogador olhou ao colocar (6 direções).
 * Pulso de redstone → quebra o bloco à frente (igual ao ejetor: só na borda de subida). Clique mostra a energia.
 * Também é a peça dos lados do multibloco da Mineradora.
 */
public class TunnelerBlock extends BaseEntityBlock {
    public static final MapCodec<TunnelerBlock> CODEC = simpleCodec(TunnelerBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    /** Faz parte de uma Mineradora montada: vira uma Perna de Perfuração (outro modelo, ignora redstone). */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public TunnelerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, false)
                .setValue(FORMED, false));
    }

    @Override
    public MapCodec<TunnelerBlock> codec() {
        return CODEC;
    }

    /** A broca aponta para onde o jogador está olhando. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (state.getValue(FORMED)) {
            return; // na Mineradora, TRIGGERED mostra se a máquina está trabalhando
        }
        boolean powered = level.hasNeighborSignal(pos);
        boolean triggered = state.getValue(TRIGGERED);
        if (powered && !triggered) {
            level.scheduleTick(pos, this, 2);
            level.setBlock(pos, state.setValue(TRIGGERED, true), Block.UPDATE_CLIENTS);
        } else if (!powered && triggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, false), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FORMED) && level.getBlockEntity(pos) instanceof TunnelerBlockEntity tunneler) {
            tunneler.dig(level, pos, state);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(FORMED)) {
            // montada: abre a tela da Mineradora (núcleo fica em cima do barril, que está atrás da perna)
            BlockPos core = pos.relative(state.getValue(FACING).getOpposite()).above();
            if (!level.isClientSide() && level.getBlockEntity(core) instanceof MinerBlockEntity miner) {
                player.openMenu(miner);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TunnelerBlockEntity tunneler) {
            player.sendOverlayMessage(Component.translatable("message.mistech.tunneler_energy",
                    tunneler.getEnergy(), TunnelerBlockEntity.CAPACITY, TunnelerBlockEntity.ENERGY_PER_BLOCK));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TunnelerBlockEntity(pos, state);
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
        builder.add(FACING, TRIGGERED, FORMED);
    }
}
