package com.mistech.mistechmod.beyond;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CalibratedSculkSensorBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Bloco de Notas Rúnico: redstone sem fio pelo sculk. Tem uma <b>frequência</b> de 1 a 15 (botão direito sobe, agachado desce).
 * Quando recebe um pulso de redstone, toca a frequência: todo <b>Sensor de Sculk Calibrado</b> a até {@link #RANGE} blocos
 * (mesma dimensão, carregado) cuja entrada de trás estiver na <b>mesma força</b> ativa na hora e solta o sinal dele.
 */
public class RunicNoteBlock extends Block {
    public static final MapCodec<RunicNoteBlock> CODEC = simpleCodec(RunicNoteBlock::new);
    public static final IntegerProperty FREQUENCY = IntegerProperty.create("frequency", 1, 15);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final int RANGE = 64;

    public RunicNoteBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FREQUENCY, 1).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        int freq = state.getValue(FREQUENCY);
        freq = player.isShiftKeyDown() ? (freq <= 1 ? 15 : freq - 1) : (freq >= 15 ? 1 : freq + 1);
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(FREQUENCY, freq), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, pitch(freq));
            player.sendOverlayMessage(Component.translatable("block.mistech.runic_note_block.frequency", freq).withColor(0x29DFEB));
        }
        return InteractionResult.SUCCESS;
    }

    private static float pitch(int freq) {
        return (float) Math.pow(2.0, (freq * 1.5 - 12.0) / 12.0);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        boolean signal = level.hasNeighborSignal(pos);
        if (signal == state.getValue(POWERED)) {
            return;
        }
        if (signal && level instanceof ServerLevel server) {
            this.play(server, pos, state.getValue(FREQUENCY));
        }
        level.setBlock(pos, state.setValue(POWERED, signal), Block.UPDATE_ALL);
    }

    /** Toca a frequência: ativa os sensores calibrados sintonizados. */
    private void play(ServerLevel level, BlockPos pos, int frequency) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, pitch(frequency));
        level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, frequency / 24.0);
        level.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.02);
        int r = RANGE;
        for (int cx = (pos.getX() - r) >> 4; cx <= (pos.getX() + r) >> 4; cx++) {
            for (int cz = (pos.getZ() - r) >> 4; cz <= (pos.getZ() + r) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be.getType() != BlockEntityType.CALIBRATED_SCULK_SENSOR || be.getBlockPos().distSqr(pos) > (double) r * r) {
                        continue;
                    }
                    BlockPos sensorPos = be.getBlockPos();
                    BlockState sensor = level.getBlockState(sensorPos);
                    if (!(sensor.getBlock() instanceof SculkSensorBlock sensorBlock) || !SculkSensorBlock.canActivate(sensor)) {
                        continue;
                    }
                    Direction back = sensor.getValue(CalibratedSculkSensorBlock.FACING).getOpposite();
                    if (level.getSignal(sensorPos.relative(back), back) != frequency) {
                        continue;
                    }
                    sensorBlock.activate(null, level, sensorPos, sensor, 15, frequency);
                    level.sendParticles(ParticleTypes.NOTE, sensorPos.getX() + 0.5, sensorPos.getY() + 1.0, sensorPos.getZ() + 0.5,
                            1, 0.0, 0.0, 0.0, frequency / 24.0);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FREQUENCY, POWERED);
    }
}
