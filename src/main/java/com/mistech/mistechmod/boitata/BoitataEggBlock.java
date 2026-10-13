package com.mistech.mistechmod.boitata;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.life.LifeContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Ovo do Boitatá: botão direito mostra o quanto falta; Runa da Vida adianta. */
public class BoitataEggBlock extends BaseEntityBlock {
    public static final MapCodec<BoitataEggBlock> CODEC = simpleCodec(BoitataEggBlock::new);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 12, 12);

    public BoitataEggBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
        return new BoitataEggBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, BoitataGear.EGG_BE.get(), BoitataEggBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof BoitataEggBlockEntity egg) {
            egg.setOwner(player.getUUID());
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!stack.is(LifeContent.LIFE_RUNE.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof BoitataEggBlockEntity egg) {
            egg.addProgress(BoitataEggBlockEntity.LIFE_BONUS);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            server.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.0);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.4F);
            status(player, egg);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BoitataEggBlockEntity egg) {
            status(player, egg);
        }
        return InteractionResult.SUCCESS;
    }

    private static void status(Player player, BoitataEggBlockEntity egg) {
        int pct = egg.progress() * 100 / BoitataEggBlockEntity.HATCH_TICKS;
        player.sendOverlayMessage(Component.translatable(egg.warm() ? "message.mistech.boitata_egg.warm" : "message.mistech.boitata_egg.cold", pct)
                .withStyle(egg.warm() ? ChatFormatting.GOLD : ChatFormatting.AQUA));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BoitataEggBlockEntity egg && egg.warm() && random.nextInt(3) == 0) {
            level.addParticle(random.nextInt(4) == 0 ? ParticleTypes.LAVA : ParticleTypes.SMALL_FLAME, pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 0.3 + random.nextDouble() * 0.6, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.01, 0.0);
        }
    }
}
