package com.mistech.mistechmod.block;

import com.mistech.mistechmod.item.IncenseItem;
import com.mistech.mistechmod.item.SmokableItem;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cinzeiro Rúnico: clique com incenso, charuto ou baseado.
 * Aceso -> apaga (runa de gelo). Apagado -> acende (runa de fogo). Não gasta nada.
 */
public class RunicAshtrayBlock extends Block {
    public static final MapCodec<RunicAshtrayBlock> CODEC = simpleCodec(RunicAshtrayBlock::new);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 3.0, 13.0);

    public RunicAshtrayBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<RunicAshtrayBlock> codec() {
        return CODEC;
    }

    public static boolean isLightable(ItemStack stack) {
        return stack.getItem() instanceof IncenseItem || stack.getItem() instanceof SmokableItem;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!isLightable(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        // cachimbo vazio não acende
        if (!IncenseItem.isLit(stack) && stack.getItem() instanceof SmokableItem smokable && !smokable.canLight(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel serverLevel) {
            double x = pos.getX() + 0.5, y = pos.getY() + 0.25, z = pos.getZ() + 0.5;
            if (IncenseItem.isLit(stack)) {
                // apagar: runa de gelo
                IncenseItem.extinguish(stack);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.6F);
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 8, 0.15, 0.05, 0.15, 0.01);
                serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 4, 0.1, 0.05, 0.1, 0.01);
            } else {
                // acender: runa de fogo
                if (stack.getItem() instanceof SmokableItem smokable) {
                    smokable.lightInPlace(player, stack);
                } else {
                    IncenseItem.lightInPlace(player, stack);
                }
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
                serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 6, 0.12, 0.05, 0.12, 0.01);
                serverLevel.sendParticles(ParticleTypes.WITCH, x, y + 0.2, z, 3, 0.1, 0.05, 0.1, 0.0);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Brilhinho das duas runas: brasa de um lado, floco de gelo do outro. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.28, pos.getY() + 0.25, pos.getZ() + 0.5, 0.0, 0.005, 0.0);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SNOWFLAKE, pos.getX() + 0.72, pos.getY() + 0.25, pos.getZ() + 0.5, 0.0, 0.005, 0.0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
