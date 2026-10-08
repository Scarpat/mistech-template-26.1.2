package com.mistech.mistechmod.purity.block;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.purity.PurityContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fonte do Santuário: centro do Santuário (rituais da magia pura).
 * Frasco de Essência Purificada = +1 nível, balde = +3 (máx. 8); frasco vazio tira 1. Outro item = catalisador;
 * mão vazia tira o catalisador (ou o resultado). Pedra Guia = começa o ritual.
 */
public class SanctuaryFontBlock extends BaseEntityBlock {
    public static final MapCodec<SanctuaryFontBlock> CODEC = simpleCodec(SanctuaryFontBlock::new);
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 2, 14),
            Block.box(5, 2, 5, 11, 9, 11),
            Block.box(1, 9, 1, 15, 13, 15));

    public SanctuaryFontBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FILLED, false).setValue(RUNNING, false));
    }

    @Override
    public MapCodec<SanctuaryFontBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SanctuaryFontBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, PurityContent.SANCTUARY_FONT_BE.get(), SanctuaryFontBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof SanctuaryFontBlockEntity font)) {
            return InteractionResult.PASS;
        }
        if (stack.is(Mistech.GUIDE_STONE.get())) {
            if (level instanceof ServerLevel server) {
                font.tryStart(server, player);
            }
            return InteractionResult.SUCCESS;
        }
        int add = stack.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) ? 1 : stack.is(PurityContent.PURIFIED_ESSENCE_BUCKET.get()) ? 3 : 0;
        if (add > 0) {
            if (font.getEssence() + add > SanctuaryFontBlockEntity.MAX_ESSENCE) {
                player.sendOverlayMessage(Component.translatable("message.mistech.altar.full"));
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide()) {
                font.setEssence(font.getEssence() + add);
                giveBack(player, stack, new ItemStack(add == 1 ? Items.GLASS_BOTTLE : Items.BUCKET));
                level.playSound(null, pos, add == 1 ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.2F);
                player.sendOverlayMessage(Component.translatable("message.mistech.sanctuary.essence_level", font.getEssence(), SanctuaryFontBlockEntity.MAX_ESSENCE));
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.GLASS_BOTTLE) && font.getEssence() > 0 && !font.isRunning()) {
            if (!level.isClientSide()) {
                font.setEssence(font.getEssence() - 1);
                giveBack(player, stack, new ItemStack(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        if (font.isRunning()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.already_running"));
            return InteractionResult.FAIL;
        }
        if (font.getStored().isEmpty() && !stack.isEmpty()) {
            if (!level.isClientSide()) {
                font.setStored(stack.split(1));
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof SanctuaryFontBlockEntity font)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (!font.isRunning() && !font.getStored().isEmpty()) {
                player.getInventory().placeItemBackInInventory(font.getStored());
                font.setStored(ItemStack.EMPTY);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.2F);
            } else {
                player.sendOverlayMessage(Component.translatable("message.mistech.sanctuary.essence_level", font.getEssence(), SanctuaryFontBlockEntity.MAX_ESSENCE));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void giveBack(Player player, ItemStack used, ItemStack back) {
        if (player.getAbilities().instabuild) {
            return;
        }
        used.shrink(1);
        player.getInventory().placeItemBackInInventory(back);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof SanctuaryFontBlockEntity font ? font.getEssence() * 15 / SanctuaryFontBlockEntity.MAX_ESSENCE : 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(FILLED) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.85,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.03, 0.0);
        }
        if (state.getValue(RUNNING)) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8, pos.getY() + 1.0 + random.nextDouble(),
                        pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.05, 0.0);
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED, RUNNING);
    }
}
