package com.mistech.mistechmod.golem;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Estátua de um golem de trabalho: botão direito com a Runa do Tempo (gasta 1 uso) e ela ganha vida. */
public class RunicStatueBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RunicStatueBlock> CODEC = simpleCodec(p -> new RunicStatueBlock(p, GolemKind.SORTER));
    private static final VoxelShape SHAPE = Block.box(2.5, 0.0, 3.0, 13.5, 16.0, 13.0);
    private final GolemKind kind;

    public RunicStatueBlock(Properties properties, GolemKind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<RunicStatueBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Mistech.TIME_RUNE.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        WorkGolemEntity golem = GolemRegistry.WORK_GOLEM.get().create(server, EntitySpawnReason.MOB_SUMMONED);
        if (golem == null) {
            return InteractionResult.FAIL;
        }
        golem.setKind(this.kind);
        float yaw = state.getValue(FACING).toYRot();
        golem.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
        golem.setYHeadRot(yaw);
        golem.setYBodyRot(yaw);
        server.removeBlock(pos, false);
        server.addFreshEntity(golem);
        stack.hurtAndBreak(1, server, (LivingEntity) null, item -> {});
        server.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15);
        server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 20, 0.3, 0.4, 0.3, 0.3);
        server.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
