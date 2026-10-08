package com.mistech.mistechmod.ether;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.arcane.ArcaneAttachedBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ponto Etéreo ({@code output = false}: puxa do bloco onde está preso e manda para a sintonia) ou
 * Receptor Etéreo ({@code output = true}: tira da sintonia e entrega no bloco onde está preso).
 * Funciona a qualquer distância e em qualquer dimensão. Sintonizado com o Diapasão Arcano.
 */
public class EtherealEndpointBlock extends ArcaneAttachedBlock {
    public static final MapCodec<EtherealEndpointBlock> CODEC = simpleCodec(p -> new EtherealEndpointBlock(p, false));
    private final boolean output;

    public EtherealEndpointBlock(Properties properties, boolean output) {
        super(properties);
        this.output = output;
    }

    public boolean isOutput() {
        return this.output;
    }

    @Override
    public MapCodec<EtherealEndpointBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EtherealEndpointBlockEntity(this.output ? EtherContent.RECEIVER_BE.get() : EtherContent.NODE_BE.get(), pos, state, this.output);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, this.output ? EtherContent.RECEIVER_BE.get() : EtherContent.NODE_BE.get(),
                EtherealEndpointBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            Direction f = state.getValue(FACING);
            level.addParticle(this.output ? ParticleTypes.PORTAL : ParticleTypes.REVERSE_PORTAL,
                    pos.getX() + 0.5 - f.getStepX() * 0.2, pos.getY() + 0.5 - f.getStepY() * 0.2, pos.getZ() + 0.5 - f.getStepZ() * 0.2,
                    f.getStepX() * 0.03, f.getStepY() * 0.03, f.getStepZ() * 0.03);
        }
    }
}
