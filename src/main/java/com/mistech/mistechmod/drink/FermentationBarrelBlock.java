package com.mistech.mistechmod.drink;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Barril de Fermentação: botão direito com Garapa/Cachaça põe (agachado: a mão toda); mão vazia vê ou tira. */
public class FermentationBarrelBlock extends BaseEntityBlock {
    public static final MapCodec<FermentationBarrelBlock> CODEC = simpleCodec(FermentationBarrelBlock::new);

    public FermentationBarrelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FermentationBarrelBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (FermentationBarrelBlockEntity.resultOf(stack) == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof FermentationBarrelBlockEntity barrel)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            int n = barrel.insert(stack, player.isShiftKeyDown() ? stack.getCount() : 1);
            if (n <= 0) {
                player.sendOverlayMessage(Component.translatable("block.mistech.fermentation_barrel.cant").withStyle(ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(n);
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FermentationBarrelBlockEntity barrel)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (barrel.contents().isEmpty()) {
            player.sendOverlayMessage(Component.translatable("block.mistech.fermentation_barrel.empty").withStyle(ChatFormatting.GRAY));
        } else if (barrel.done() || player.isShiftKeyDown()) {
            boolean done = barrel.done();
            ItemStack out = barrel.takeAll();
            if (!player.getInventory().add(out)) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, out);
            }
            level.playSound(null, pos, done ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.8F, 1.0F);
        } else {
            long left = FermentationBarrelBlockEntity.timeFor(barrel.contents()) - barrel.elapsed();
            player.sendOverlayMessage(Component.translatable("block.mistech.fermentation_barrel.status", barrel.contents().getCount(),
                    barrel.contents().getHoverName(), Math.round(barrel.progress() * 100), (left + 1199) / 1200).withStyle(ChatFormatting.GOLD));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FermentationBarrelBlockEntity barrel) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, barrel.takeAll());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof FermentationBarrelBlockEntity barrel) || barrel.contents().isEmpty()) {
            return;
        }
        if (barrel.done()) {
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05,
                        pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
            }
        } else if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.02,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.03, 0);
        }
    }
}
