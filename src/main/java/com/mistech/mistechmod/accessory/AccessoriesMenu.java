package com.mistech.mistechmod.accessory;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.registry.MistechAttachments;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Tela dos acessórios: amuleto em cima, dois anéis embaixo. */
public class AccessoriesMenu extends AbstractContainerMenu {
    public static final int AMULET_X = 80, AMULET_Y = 20;
    public static final int RING1_X = 62, RING1_Y = 48;
    public static final int RING2_X = 98, RING2_Y = 48;

    private final Container container;

    /** Cliente. */
    public AccessoriesMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(Accessories.SIZE));
    }

    public AccessoriesMenu(int containerId, Inventory inventory, Container container) {
        super(MistechMenus.ACCESSORIES.get(), containerId);
        this.container = container;
        this.addSlot(new AccessorySlot(container, 0, AMULET_X, AMULET_Y, false));
        this.addSlot(new AccessorySlot(container, 1, RING1_X, RING1_Y, true));
        this.addSlot(new AccessorySlot(container, 2, RING2_X, RING2_Y, true));
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    public static boolean fits(ItemStack stack, boolean ringSlot) {
        return stack.getItem() instanceof AccessoryItem item && item.accessory().ring == ringSlot;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < Accessories.SIZE) {
            if (!this.moveItemStackTo(stack, Accessories.SIZE, Accessories.SIZE + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else if (fits(stack, false)) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (fits(stack, true)) {
            if (!this.moveItemStackTo(stack, 1, 3, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    static class AccessorySlot extends Slot {
        private final boolean ring;

        AccessorySlot(Container container, int index, int x, int y, boolean ring) {
            super(container, index, x, y);
            this.ring = ring;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return fits(stack, this.ring);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** Container do servidor ligado ao attachment do jogador. */
    public static class PlayerAccessories extends SimpleContainer {
        private final Player player;

        public PlayerAccessories(Player player) {
            super(Accessories.SIZE);
            this.player = player;
            List<ItemStack> saved = player.getData(MistechAttachments.ACCESSORIES);
            for (int i = 0; i < Accessories.SIZE && i < saved.size(); i++) {
                this.setItem(i, saved.get(i).copy());
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();
            List<ItemStack> list = new ArrayList<>();
            for (int i = 0; i < Accessories.SIZE; i++) {
                list.add(this.getItem(i).copy());
            }
            this.player.setData(MistechAttachments.ACCESSORIES, List.copyOf(list));
            Accessories.refresh(this.player);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
