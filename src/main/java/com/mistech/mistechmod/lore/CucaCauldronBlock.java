package com.mistech.mistechmod.lore;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.drink.CaneStillBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Caldeirão da Cuca (alquimia avançada): precisa de fogo embaixo. Botão direito com poção = põe (frasco volta);
 * Essência Impura = +1 nível em tudo; frasco vazio (com 2+ poções) = tira a Poção da Cuca; agachar com a mão vazia = esvazia.
 */
public class CucaCauldronBlock extends BaseEntityBlock {
    public static final MapCodec<CucaCauldronBlock> CODEC = simpleCodec(CucaCauldronBlock::new);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 3);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public CucaCauldronBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FILL, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
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
        return new CucaCauldronBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CucaCauldronBlockEntity cauldron)) {
            return InteractionResult.PASS;
        }
        boolean potion = stack.is(Items.POTION) && stack.has(DataComponents.POTION_CONTENTS);
        boolean essence = stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get());
        boolean bottle = stack.is(Items.GLASS_BOTTLE);
        if (!potion && !essence && !bottle) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!CaneStillBlockEntity.heatedFromBelow(level, pos)) {
            player.sendOverlayMessage(Component.translatable("block.mistech.cuca_cauldron.cold").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        if (potion) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null || !cauldron.addPotion(contents)) {
                player.sendOverlayMessage(Component.translatable("block.mistech.cuca_cauldron.full").withStyle(ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
            consume(player, stack, new ItemStack(Items.GLASS_BOTTLE));
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 0.8F);
        } else if (essence) {
            if (!cauldron.empower()) {
                return InteractionResult.FAIL;
            }
            consume(player, stack, new ItemStack(Items.GLASS_BOTTLE));
            level.playSound(null, pos, SoundEvents.WITCH_CELEBRATE, SoundSource.BLOCKS, 1.0F, 0.7F);
        } else {
            ItemStack out = cauldron.take();
            if (out.isEmpty()) {
                player.sendOverlayMessage(Component.translatable("block.mistech.cuca_cauldron.need_two").withStyle(ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
            consume(player, stack, out);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    private static void consume(Player player, ItemStack stack, ItemStack back) {
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        player.getInventory().placeItemBackInInventory(back);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CucaCauldronBlockEntity cauldron) {
            if (player.isShiftKeyDown()) {
                cauldron.clear();
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 0.8F);
            } else {
                player.sendOverlayMessage(Component.translatable("block.mistech.cuca_cauldron.status", cauldron.potions(), CucaCauldronBlockEntity.MAX_POTIONS)
                        .withStyle(ChatFormatting.DARK_GREEN));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(FILL) > 0 && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.8,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.04, 0);
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0.05, 0);
            }
        }
    }
}
