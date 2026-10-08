package com.mistech.mistechmod.power;

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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Caldeira Ígnea: vapor pela chaminé, borbulhas de lava e chiado. Clique com balde de lava enche o tanque. */
public class IgneousBoilerBlock extends MachineBlock {
    public static final MapCodec<IgneousBoilerBlock> CODEC = simpleCodec(IgneousBoilerBlock::new);

    public IgneousBoilerBlock(BlockBehaviour.Properties properties) {
        super(properties, PowerContent.IGNEOUS_BOILER_BE);
    }

    @Override
    public MapCodec<IgneousBoilerBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.LAVA_BUCKET) && level.getBlockEntity(pos) instanceof IgneousBoilerBlockEntity boiler) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return boiler.interact(player, hand) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.CLOUD, x + (random.nextDouble() - 0.5) * 0.2, y + 0.45, z + (random.nextDouble() - 0.5) * 0.2, 0.0, 0.08, 0.0);
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.LAVA, x + (random.nextDouble() - 0.5) * 0.6, y, z + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(25) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.4F, 0.9F + random.nextFloat() * 0.2F, false);
        }
        if (random.nextInt(60) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 1.6F, false);
        }
    }
}
