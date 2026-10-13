package com.mistech.mistechmod.infuser;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Infusor Arcano (nível 1, 2 ou 3): abre a tela de escolher encanto. */
public class ArcaneInfuserBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 12, 16);

    private final int tier;
    private final MapCodec<ArcaneInfuserBlock> codec;

    public ArcaneInfuserBlock(int tier, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
        this.codec = simpleCodec(p -> new ArcaneInfuserBlock(tier, p));
    }

    public int tier() {
        return this.tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return this.codec;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneInfuserBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ArcaneInfuserBlockEntity infuser) {
            player.openMenu(infuser);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int chance = 4 - this.tier;
        if (random.nextInt(Math.max(1, chance)) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5 + (random.nextDouble() - 0.5), pos.getY() + 1.4 + random.nextDouble() * 0.4,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5), 0.0, -0.6, 0.0);
        }
        if (this.tier >= 3 && random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 0.0, 0.03, 0.0);
        }
    }
}
