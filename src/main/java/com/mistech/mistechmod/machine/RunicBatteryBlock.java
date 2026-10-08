package com.mistech.mistechmod.machine;

import java.util.function.Supplier;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;

/**
 * Bateria Rúnica. A frente (para onde aponta a janelinha) é a saída de energia.
 * LIT = tem energia guardada (a janela acende). Comparador = quanto está cheia.
 */
public class RunicBatteryBlock extends MachineBlock {
    public static final MapCodec<RunicBatteryBlock> CODEC = simpleCodec(RunicBatteryBlock::new);

    public RunicBatteryBlock(BlockBehaviour.Properties properties) {
        this(properties, MistechBlockEntities.RUNIC_BATTERY);
    }

    protected RunicBatteryBlock(BlockBehaviour.Properties properties,
            Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type) {
        super(properties, type);
    }

    @Override
    public MapCodec<? extends RunicBatteryBlock> codec() {
        return CODEC;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof RunicBatteryBlockEntity battery
                ? EnergyHandlerUtil.getRedstoneSignalFromEnergyHandler(battery.getEnergyHandler())
                : 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(8) == 0) {
            // faísca entre os dois terminais do topo
            double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
            double z = pos.getZ() + 0.5;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 1.0, z, 0.0, 0.02, 0.0);
        }
    }
}
