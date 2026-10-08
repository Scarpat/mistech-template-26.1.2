package com.mistech.mistechmod.menu;

import com.mistech.mistechmod.block.RunicFurnaceBlockEntity;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class RunicFurnaceMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 56, INPUT_Y = 35;
    public static final int OUTPUT_X = 116, OUTPUT_Y = 35;
    private static final int INV_START = 2, INV_END = INV_START + 27, HOTBAR_END = INV_END + 9;

    private final Container container;
    private final ContainerData data;
    private final Level level;

    public RunicFurnaceMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(2), new SimpleContainerData(2));
    }

    public RunicFurnaceMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(MistechMenus.RUNIC_FURNACE.get(), containerId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;
        this.level = inventory.player.level();

        this.addSlot(new Slot(container, RunicFurnaceBlockEntity.SLOT_INPUT, INPUT_X, INPUT_Y));
        this.addSlot(new Slot(container, RunicFurnaceBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                if (this.container instanceof RunicFurnaceBlockEntity furnace) {
                    furnace.awardExperience(player);
                }
                super.onTake(player, carried);
            }
        });
        this.addDataSlots(data);
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    /** Progresso de 0 a 1 para a seta da tela. */
    public float getProgress() {
        int total = this.data.get(1);
        return total <= 0 ? 0.0F : Math.min(1.0F, this.data.get(0) / (float) total);
    }

    public boolean isCooking() {
        return this.data.get(0) > 0;
    }

    private boolean isSmeltable(ItemStack stack) {
        // no cliente as receitas não existem: aceita qualquer coisa no shift-click
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return true;
        }
        return serverLevel.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), serverLevel).isPresent();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return original;
        }
        ItemStack stack = slot.getItem();
        original = stack.copy();

        if (slotIndex == RunicFurnaceBlockEntity.SLOT_OUTPUT) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (slotIndex == RunicFurnaceBlockEntity.SLOT_INPUT) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.isSmeltable(stack)) {
            if (!this.moveItemStackTo(stack, RunicFurnaceBlockEntity.SLOT_INPUT, RunicFurnaceBlockEntity.SLOT_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < INV_END) {
            if (!this.moveItemStackTo(stack, INV_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }
}
