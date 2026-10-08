package com.mistech.mistechmod.purity.block;

import com.mistech.mistechmod.block.FluidInteraction;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.Taint;
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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bandeja de Evaporação (salina): garrafa de água = 1 porção, balde = 3 (bandeja vazia). Ao sol (dia, céu aberto, sem chuva)
 * cada porção vira 1 Sal Grosso com o tempo (random tick). Clique com a mão vazia recolhe o sal.
 */
public class EvaporationTrayBlock extends Block {
    public static final MapCodec<EvaporationTrayBlock> CODEC = simpleCodec(EvaporationTrayBlock::new);
    public static final int CAPACITY = 3;
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, CAPACITY);
    public static final IntegerProperty SALT = IntegerProperty.create("salt", 0, CAPACITY);
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);

    public EvaporationTrayBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATER, 0).setValue(SALT, 0));
    }

    @Override
    public MapCodec<EvaporationTrayBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private static int used(BlockState state) {
        return state.getValue(WATER) + state.getValue(SALT);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (FluidInteraction.isWaterBottle(stack) && used(state) < CAPACITY) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(WATER, state.getValue(WATER) + 1), Block.UPDATE_ALL);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.WATER_BUCKET) && used(state) == 0) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(WATER, CAPACITY), Block.UPDATE_ALL);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        int salt = state.getValue(SALT);
        if (salt <= 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(SALT, 0), Block.UPDATE_ALL);
            ItemStack out = new ItemStack(PurityContent.COARSE_SALT.get(), salt);
            if (!player.getInventory().add(out)) {
                player.drop(out, false);
            }
            level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.8F, 1.3F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(WATER) > 0 && Taint.inSunlight(level, pos.above())) {
            level.setBlock(pos, state.setValue(WATER, state.getValue(WATER) - 1).setValue(SALT, state.getValue(SALT) + 1), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 4, 0.3, 0.05, 0.3, 0.01);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(WATER) > 0 && level.isBrightOutside() && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.3,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATER, SALT);
    }
}
