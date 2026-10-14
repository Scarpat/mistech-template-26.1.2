package com.mistech.mistechmod.husbandry;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Vaso de Cultivo (early game): planta uma cana, flor pequena ou cogumelo (tag mistech:pot_plants).
 * - Clique com a planta: planta (só 1).
 * - Mão vazia: pega o que brotou (ou mostra quanto falta). Shift + mão vazia: tira a planta.
 * - Farinha de osso: brota na hora.
 * - Funil por baixo tira o que brotou; funil por cima/lados pode colocar a planta.
 */
public class GrowingPotBlock extends BaseEntityBlock {
    public static final MapCodec<GrowingPotBlock> CODEC = simpleCodec(GrowingPotBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2.0, 0.0, 2.0, 14.0, 7.0, 14.0),
            Block.box(1.0, 7.0, 1.0, 15.0, 9.0, 15.0));

    public GrowingPotBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<GrowingPotBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrowingPotBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, HusbandryContent.GROWING_POT_BE.get(), GrowingPotBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof GrowingPotBlockEntity pot)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (pot.getPlant().isEmpty() && GrowingPotBlockEntity.isPlant(stack)) {
            if (!level.isClientSide()) {
                pot.setPlant(stack.copyWithCount(1));
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.BONE_MEAL) && !pot.getPlant().isEmpty()) {
            if (!level.isClientSide()) {
                if (pot.bonemeal()) {
                    stack.consume(1, player);
                    level.levelEvent(net.minecraft.world.level.block.LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos.above(), 15);
                } else {
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mistech.growing_pot.full"));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof GrowingPotBlockEntity pot)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && !pot.getPlant().isEmpty()) {
            give(player, pot.takeOutput());
            give(player, pot.removePlant());
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        ItemStack out = pot.takeOutput();
        if (!out.isEmpty()) {
            give(player, out);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.2F);
        } else {
            player.sendOverlayMessage(pot.statusMessage());
        }
        return InteractionResult.SUCCESS;
    }

    private static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /** Folhinhas verdes saindo de vez em quando quando tem planta. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (random.nextInt(14) == 0 && level.getBlockEntity(pos) instanceof GrowingPotBlockEntity pot && !pot.getPlant().isEmpty()) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.7,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof GrowingPotBlockEntity pot ? pot.comparatorSignal() : 0;
    }
}
