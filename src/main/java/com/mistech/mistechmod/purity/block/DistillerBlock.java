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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Destilador Alquímico: alambique de cobre. Clique com frasco/balde mexe nos tanques; sem item abre a tela. */
public class DistillerBlock extends MachineBlock {
    public static final MapCodec<DistillerBlock> CODEC = simpleCodec(DistillerBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 9, 15), Block.box(4, 9, 4, 12, 15, 12));

    public DistillerBlock(BlockBehaviour.Properties properties) {
        super(properties, PurityContent.DISTILLER_BE);
    }

    @Override
    public MapCodec<DistillerBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof DistillerBlockEntity distiller && PurityFluids.isContainer(stack)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return distiller.interact(player, hand) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.CLOUD, x + (random.nextDouble() - 0.5) * 0.3, y, z + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.03, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WITCH, x + (random.nextDouble() - 0.5) * 0.8, pos.getY() + 0.6, z + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(30) == 0) {
            level.playLocalSound(x, y, z, net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW, net.minecraft.sounds.SoundSource.BLOCKS, 0.4F, 1.2F, false);
        }
    }
}
