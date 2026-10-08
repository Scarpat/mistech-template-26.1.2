package com.mistech.mistechmod.purity.block;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.purity.PurityContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Gerador Purificado: queima Essência Purificada (100 FE/t). Clique com frasco/balde enche o tanque. */
public class PureGeneratorBlock extends MachineBlock {
    public static final MapCodec<PureGeneratorBlock> CODEC = simpleCodec(PureGeneratorBlock::new);

    public PureGeneratorBlock(BlockBehaviour.Properties properties) {
        super(properties, PurityContent.PURE_GENERATOR_BE);
    }

    @Override
    public MapCodec<PureGeneratorBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof PureGeneratorBlockEntity generator && PurityFluids.isContainer(stack)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return generator.interact(player, hand) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 1.0,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.03, 0.0);
        }
    }
}
