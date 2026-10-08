package com.mistech.mistechmod.block;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Chaleira: guarda a água em xícaras (0-6: balde = 6, garrafa = 2), a bebida que está sendo feita e o tempo de fervura.
 * A bebida pronta sai em Xícaras (1 por dose). Garrafa de vidro só pega água pura de volta (2 doses).
 * Fervura: {@link #BREW_TICKS} (5 s). O estágio fica no bloco (KettleBlock.STAGE) para as partículas.
 */
public class KettleBlockEntity extends BlockEntity {
    public static final int MAX_WATER = 6;
    /** Uma garrafa de água = 2 xícaras. */
    public static final int BOTTLE = 2;
    public static final int BREW_TICKS = 5 * 20;

    /** Bebidas que a chaleira sabe fazer. */
    public enum Drink {
        NONE, COFFEE, MYSTIC_SIGHT, MUSHROOM;

        static Drink byIngredient(ItemStack stack) {
            if (stack.is(Mistech.COFFEE_POWDER.get())) {
                return COFFEE;
            }
            if (stack.is(Mistech.BELLADONNA_BERRIES.get())) {
                return MYSTIC_SIGHT;
            }
            if (stack.is(com.mistech.mistechmod.registry.GnomeContent.YELLOW_MUSHROOM_ITEM.get())) {
                return MUSHROOM;
            }
            return NONE;
        }

        ItemStack cup() {
            return switch (this) {
                case COFFEE -> new ItemStack(Mistech.COFFEE_CUP.get());
                case MYSTIC_SIGHT -> new ItemStack(Mistech.BELLADONNA_TEA_CUP.get());
                case MUSHROOM -> new ItemStack(com.mistech.mistechmod.registry.DreamContent.MUSHROOM_TEA_CUP.get());
                case NONE -> ItemStack.EMPTY;
            };
        }
    }

    private int water;
    private Drink drink = Drink.NONE;
    private int brewTicks;

    public KettleBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.KETTLE.get(), pos, state);
    }

    private KettleBlock.Stage stage() {
        if (this.water <= 0) {
            return KettleBlock.Stage.EMPTY;
        }
        if (this.drink == Drink.NONE) {
            return KettleBlock.Stage.WATER;
        }
        return this.brewTicks < BREW_TICKS ? KettleBlock.Stage.BREWING : KettleBlock.Stage.READY;
    }

    private static boolean isWaterBottle(ItemStack stack) {
        return stack.is(Items.POTION)
                && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
    }

    /** Itens que a chaleira usa (água, ingredientes, garrafa vazia). */
    public static boolean isKettleItem(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || isWaterBottle(stack) || Drink.byIngredient(stack) != Drink.NONE
                || stack.is(Items.GLASS_BOTTLE) || stack.is(Mistech.CUP.get());
    }

    /** Esse item faz alguma coisa na chaleira agora? (servidor) */
    public boolean canUse(ItemStack stack) {
        KettleBlock.Stage stage = this.stage();
        if (stack.is(Items.WATER_BUCKET)) {
            return stage == KettleBlock.Stage.EMPTY || (stage == KettleBlock.Stage.WATER && this.water < MAX_WATER);
        }
        if (isWaterBottle(stack)) {
            return (stage == KettleBlock.Stage.EMPTY || stage == KettleBlock.Stage.WATER) && this.water + BOTTLE <= MAX_WATER;
        }
        if (Drink.byIngredient(stack) != Drink.NONE) {
            return stage == KettleBlock.Stage.WATER;
        }
        if (stack.is(Items.GLASS_BOTTLE)) {
            return stage == KettleBlock.Stage.WATER && this.water >= BOTTLE;
        }
        if (stack.is(Mistech.CUP.get())) {
            return stage == KettleBlock.Stage.READY;
        }
        return false;
    }

    /** Só no servidor, depois de {@link #canUse}. */
    public void use(Player player, InteractionHand hand, ItemStack stack) {
        Level level = this.level;
        if (level == null) {
            return;
        }
        BlockPos pos = this.worldPosition;
        if (stack.is(Items.WATER_BUCKET)) {
            this.water = MAX_WATER;
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (isWaterBottle(stack)) {
            this.water += BOTTLE;
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (Drink.byIngredient(stack) != Drink.NONE) {
            this.drink = Drink.byIngredient(stack);
            this.brewTicks = 0;
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.3F);
        } else if (stack.is(Items.GLASS_BOTTLE)) {
            this.water -= BOTTLE;
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player,
                    PotionContents.createItemStack(Items.POTION, Potions.WATER)));
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (stack.is(Mistech.CUP.get())) {
            ItemStack result = this.drink.cup();
            this.water--;
            if (this.water <= 0) {
                this.water = 0;
                this.drink = Drink.NONE;
                this.brewTicks = 0;
            }
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, result));
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        this.changed();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KettleBlockEntity kettle) {
        if (kettle.drink != Drink.NONE && kettle.water > 0 && kettle.brewTicks < BREW_TICKS) {
            kettle.brewTicks++;
            if (kettle.brewTicks >= BREW_TICKS) {
                level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.4F);
                kettle.changed();
            }
        }
    }

    /** Salva e atualiza o estágio do bloco (partículas e comparador). */
    private void changed() {
        this.setChanged();
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        BlockState state = this.getBlockState();
        KettleBlock.Stage stage = this.stage();
        if (state.hasProperty(KettleBlock.STAGE) && state.getValue(KettleBlock.STAGE) != stage) {
            this.level.setBlock(this.worldPosition, state.setValue(KettleBlock.STAGE, stage), Block.UPDATE_ALL);
        } else {
            this.level.updateNeighbourForOutputSignal(this.worldPosition, state.getBlock());
        }
    }

    public Component statusMessage() {
        return switch (this.stage()) {
            case EMPTY -> Component.translatable("message.mistech.kettle.empty");
            case WATER -> Component.translatable("message.mistech.kettle.water", this.water);
            case BREWING -> Component.translatable("message.mistech.kettle.brewing",
                    Component.translatable("message.mistech.kettle.drink." + this.drink.name().toLowerCase(java.util.Locale.ROOT)));
            case READY -> Component.translatable("message.mistech.kettle.ready", this.water,
                    Component.translatable("message.mistech.kettle.drink." + this.drink.name().toLowerCase(java.util.Locale.ROOT)));
        };
    }

    /** 0 vazia; 1-6 = xícaras de água; 7-12 = xícaras prontas. */
    public int comparatorSignal() {
        return this.stage() == KettleBlock.Stage.READY ? MAX_WATER + this.water : this.water;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.water = input.getIntOr("Water", 0);
        this.brewTicks = input.getIntOr("BrewTicks", 0);
        try {
            this.drink = Drink.valueOf(input.getStringOr("Drink", "NONE"));
        } catch (IllegalArgumentException e) {
            this.drink = Drink.NONE;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Water", this.water);
        output.putInt("BrewTicks", this.brewTicks);
        output.putString("Drink", this.drink.name());
    }
}
