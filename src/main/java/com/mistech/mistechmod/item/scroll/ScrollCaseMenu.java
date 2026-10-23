package com.mistech.mistechmod.item.scroll;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Tela do Porta-Pergaminhos: 1 fileira de runas + inventário (o próprio estojo fica travado). */
public class ScrollCaseMenu extends AbstractContainerMenu {
    public static final int SLOTS = ScrollCaseItem.MAX_SLOTS;
    public static final int ROW_Y = 18, INV_Y = 49;

    private final Container container;
    private final int lockedInventorySlot;

    public ScrollCaseMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SLOTS), -1);
    }

    public ScrollCaseMenu(int containerId, Inventory inventory, Container container, int lockedInventorySlot) {
        super(ScrollCaseContent.MENU.get(), containerId);
        checkContainerSize(container, SLOTS);
        this.container = container;
        this.lockedInventorySlot = lockedInventorySlot;
        container.startOpen(inventory.player);
        for (int i = 0; i < SLOTS; i++) {
            final int index = i;
            this.addSlot(new Slot(container, i, 8 + i * 18, ROW_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return index < ScrollCaseItem.slots() && ScrollCaseItem.isRune(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isActive() {
                    return index < ScrollCaseItem.slots();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(this.playerSlot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(this.playerSlot(inventory, col, 8 + col * 18, INV_Y + 58));
        }
    }

    private Slot playerSlot(Inventory inventory, int index, int x, int y) {
        if (index != this.lockedInventorySlot) {
            return new Slot(inventory, index, x, y);
        }
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (this.lockedInventorySlot >= 0) {
            if (input == ContainerInput.SWAP && buttonNum == this.lockedInventorySlot) {
                return;
            }
            if (slotIndex >= 0 && slotIndex < this.slots.size()) {
                Slot slot = this.slots.get(slotIndex);
                if (slot.container == player.getInventory() && slot.getContainerSlot() == this.lockedInventorySlot) {
                    return;
                }
            }
        }
        super.clicked(slotIndex, buttonNum, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOTS) {
            if (!this.moveItemStackTo(stack, SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!ScrollCaseItem.isRune(stack) || !this.moveItemStackTo(stack, 0, ScrollCaseItem.slots(), false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
