package com.mistech.mistechmod.menu;

import com.mistech.mistechmod.block.CondenserBlockEntity;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CondenserMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 56, INPUT_Y = 35;
    public static final int OUTPUT_X = 116, OUTPUT_Y = 35;
    private static final int INV_START = 2, INV_END = INV_START + 27, HOTBAR_END = INV_END + 9;

    private final Container container;
    private final ContainerData data;

    public CondenserMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(2), new SimpleContainerData(2));
    }

    public CondenserMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(MistechMenus.CONDENSER.get(), containerId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;

        this.addSlot(new Slot(container, CondenserBlockEntity.SLOT_INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CondenserBlockEntity.isUpgradeable(stack);
            }
        });
        this.addSlot(new Slot(container, CondenserBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addDataSlots(data);
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    public float getProgress() {
        return Math.min(1.0F, this.data.get(0) / (float) CondenserBlockEntity.WORK_TICKS);
    }

    public int getEnergy() {
        return this.data.get(1);
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

        if (slotIndex < 2) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (CondenserBlockEntity.isUpgradeable(stack)) {
            if (!this.moveItemStackTo(stack, CondenserBlockEntity.SLOT_INPUT, CondenserBlockEntity.SLOT_INPUT + 1, false)) {
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
