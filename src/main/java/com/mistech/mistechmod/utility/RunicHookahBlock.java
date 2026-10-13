package com.mistech.mistechmod.utility;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.IncenseItem;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.purity.PurityContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Narguilé Rúnico. Botão direito com:
 * <ul>
 * <li>garrafa d'água / balde d'água → base de água; Frasco de Essência Impura → base impura; Frasco Purificado → base pura;</li>
 * <li>Folha de Tabaco/Cânhamo Seca ou Folha de Arruda → enche o fornilho ({@link RunicHookahBlockEntity#PUFFS} tragadas);</li>
 * <li>uma runa → põe no fornilho (devolve a anterior); isqueiro ou Runa de Fogo → acende;</li>
 * <li>mão vazia → traga; agachado com mão vazia → tira a runa.</li>
 * </ul>
 */
public class RunicHookahBlock extends BaseEntityBlock {
    public static final MapCodec<RunicHookahBlock> CODEC = simpleCodec(RunicHookahBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** O que está dentro, para o modelo mostrar: base (0 vazio, 1 água, 2 impura, 3 pura), fumo (0 vazio, 1 tabaco, 2 cânhamo, 3 arruda)
     * e runa (0 = nenhuma; senão ordinal do {@link com.mistech.mistechmod.item.RuneElement} + 1, cor pela tinta). */
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty BASE =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("base", 0, 3);
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty LEAF =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("leaf", 0, 3);
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty RUNE =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("rune", 0, com.mistech.mistechmod.item.RuneElement.values().length);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public RunicHookahBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false).setValue(BASE, 0).setValue(LEAF, 0).setValue(RUNE, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, BASE, LEAF, RUNE);
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
        return new RunicHookahBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, UtilityContent.RUNIC_HOOKAH_BE.get(), RunicHookahBlockEntity::serverTick);
    }

    private static void say(Player player, String key) {
        if (!key.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RunicHookahBlockEntity hookah)) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // base
        RunicHookahBlockEntity.Base base = null;
        ItemStack back = ItemStack.EMPTY;
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (stack.is(Items.POTION) && potion != null && potion.is(Potions.WATER)) {
            base = RunicHookahBlockEntity.Base.WATER;
            back = new ItemStack(Items.GLASS_BOTTLE);
        } else if (stack.is(Items.WATER_BUCKET)) {
            base = RunicHookahBlockEntity.Base.WATER;
            back = new ItemStack(Items.BUCKET);
        } else if (stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get())) {
            base = RunicHookahBlockEntity.Base.IMPURE;
            back = new ItemStack(Items.GLASS_BOTTLE);
        } else if (stack.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get())) {
            base = RunicHookahBlockEntity.Base.PURE;
            back = new ItemStack(Items.GLASS_BOTTLE);
        }
        if (base != null) {
            hookah.setBase(base);
            consume(player, stack, back);
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        // fumo
        RunicHookahBlockEntity.Leaf leaf = stack.is(Mistech.DRIED_TOBACCO_LEAF.get()) ? RunicHookahBlockEntity.Leaf.TOBACCO
                : stack.is(Mistech.DRIED_HEMP_LEAF.get()) ? RunicHookahBlockEntity.Leaf.HEMP
                : stack.is(PurityContent.RUE_LEAF.get()) ? RunicHookahBlockEntity.Leaf.RUE : null;
        if (leaf != null) {
            hookah.fill(leaf);
            consume(player, stack, ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.8F, 1.2F);
            return InteractionResult.SUCCESS;
        }
        // acender (antes da runa: a Runa de Fogo acende)
        if (IncenseItem.isLighter(stack) && !hookah.lit()) {
            hookah.setLit(true);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        // runa
        if (RuneElement.ofRune(stack) != null) {
            ItemStack old = hookah.swapRune(stack.split(1));
            if (!old.isEmpty()) {
                player.getInventory().placeItemBackInInventory(old);
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.3F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static void consume(Player player, ItemStack stack, ItemStack back) {
        if (player.hasInfiniteMaterials()) {
            return;
        }
        stack.shrink(1);
        if (!back.isEmpty()) {
            player.getInventory().placeItemBackInInventory(back);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RunicHookahBlockEntity hookah)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            if (!hookah.rune().isEmpty()) {
                player.getInventory().placeItemBackInInventory(hookah.swapRune(ItemStack.EMPTY));
            } else {
                showStatus(player, hookah);
            }
            return InteractionResult.SUCCESS;
        }
        String error = hookah.puff(server, player);
        if (error != null) {
            say(player, error);
        }
        return InteractionResult.SUCCESS;
    }

    private static void showStatus(Player player, RunicHookahBlockEntity hookah) {
        RuneElement element = hookah.element();
        player.sendOverlayMessage(Component.translatable("block.mistech.runic_hookah.status",
                Component.translatable("block.mistech.runic_hookah.base." + hookah.base().name().toLowerCase()),
                Component.translatable("block.mistech.runic_hookah.leaf." + hookah.leaf().name().toLowerCase()), hookah.puffs(),
                element == null ? Component.translatable("block.mistech.runic_hookah.no_rune") : Component.translatable(element.translationKey()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RunicHookahBlockEntity hookah && !hookah.rune().isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, hookah.swapRune(ItemStack.EMPTY));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Cor da runa (tinta 0 do modelo). */
    public static int runeTint(BlockState state) {
        int r = state.getValue(RUNE);
        return r <= 0 ? 0xFFFFFFFF : 0xFF000000 | com.mistech.mistechmod.item.RuneElement.values()[r - 1].color();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(4) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.02, 0);
        }
        if (state.getValue(LIT) && random.nextInt(8) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + 0.98, pos.getZ() + 0.5, 0, 0.0, 0);
        }
    }

}
