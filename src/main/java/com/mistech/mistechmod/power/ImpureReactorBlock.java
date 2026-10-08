package com.mistech.mistechmod.power;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Reator Impuro: núcleo roxo pulsando, faíscas reversas e um zumbido baixo enquanto queima. */
public class ImpureReactorBlock extends MachineBlock {
    public static final MapCodec<ImpureReactorBlock> CODEC = simpleCodec(ImpureReactorBlock::new);

    public ImpureReactorBlock(BlockBehaviour.Properties properties) {
        super(properties, PowerContent.IMPURE_REACTOR_BE);
    }

    @Override
    public MapCodec<ImpureReactorBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if ((stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) || stack.is(Mistech.IMPURE_ESSENCE_BUCKET.get()))
                && level.getBlockEntity(pos) instanceof ImpureReactorBlockEntity reactor) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return reactor.interact(player, hand) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, x + (random.nextDouble() - 0.5) * 1.1, y + (random.nextDouble() - 0.5) * 1.1,
                    z + (random.nextDouble() - 0.5) * 1.1, 0.0, 0.02, 0.0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.WITCH, x, pos.getY() + 1.05, z, 0.0, 0.05, 0.0);
        }
        if (random.nextInt(40) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.4F, 0.6F, false);
        }
    }
}
