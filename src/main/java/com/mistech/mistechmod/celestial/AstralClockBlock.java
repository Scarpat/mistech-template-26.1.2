package com.mistech.mistechmod.celestial;

import com.mistech.mistechmod.purity.Taint;
import com.mistech.mistechmod.purity.area.AreaTaint;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Relógio Astral (Fase 6): transforma o céu em sinal de comparador. Clique troca o modo (a face mostra qual):
 * 0 = fase da lua (1 a 8, lua cheia = 1), 1 = Evento Celeste ativo (1 a 9, pela ordem dos eventos; 0 = nenhum),
 * 2 = dia/noite (15 de dia, 0 de noite), 3 = Mácula do chunk (faixa × 3: 0, 3, 6, 9, 12).
 * Confere a cada segundo e avisa os vizinhos quando o sinal muda.
 */
public class AstralClockBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AstralClockBlock> CODEC = simpleCodec(AstralClockBlock::new);
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 3);
    public static final IntegerProperty SIGNAL = IntegerProperty.create("signal", 0, 15);
    public static final String[] MODES = {"moon", "event", "day", "taint"};

    public AstralClockBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODE, 0).setValue(SIGNAL, 0));
    }

    /** Torrezinha: base, corpo com o mostrador, pilares de ouro, telhado e o orbe. */
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(1, 0, 1, 15, 2, 15), Block.box(2, 2, 2, 14, 13, 14), Block.box(4, 13, 4, 12, 14, 12),
            Block.box(6.5, 14, 6.5, 9.5, 17, 9.5));

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<AstralClockBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE, SIGNAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Sinal do modo agora. */
    public static int compute(Level level, BlockPos pos, int mode) {
        return switch (mode) {
            case 0 -> (level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos).index() & 7) + 1;
            case 1 -> {
                int signal = 0;
                for (CelestialEvent event : CelestialEvent.values()) {
                    if (Celestial.isActive(level, event)) {
                        signal = event.ordinal() + 1;
                        break;
                    }
                }
                yield signal;
            }
            case 2 -> level.isBrightOutside() ? 15 : 0;
            default -> Taint.Stage.of(AreaTaint.get(level, pos)).ordinal() * 3;
        };
    }

    private void refresh(BlockState state, ServerLevel level, BlockPos pos) {
        int signal = compute(level, pos, state.getValue(MODE));
        if (signal != state.getValue(SIGNAL)) {
            level.setBlock(pos, state.setValue(SIGNAL, signal), Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(pos, this);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            this.refresh(state, server, pos);
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.refresh(state, level, pos);
        level.scheduleTick(pos, this, 20);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel server) {
            int mode = (state.getValue(MODE) + (player.isShiftKeyDown() ? 3 : 1)) & 3;
            BlockState next = state.setValue(MODE, mode);
            server.setBlock(pos, next, Block.UPDATE_ALL);
            this.refresh(next, server, pos);
            player.sendOverlayMessage(Component.translatable("message.mistech.astral_clock.mode",
                    Component.translatable("message.mistech.astral_clock.mode." + MODES[mode]), compute(server, pos, mode))
                    .withStyle(ChatFormatting.AQUA));
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 0.8F + mode * 0.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(SIGNAL);
    }
}
