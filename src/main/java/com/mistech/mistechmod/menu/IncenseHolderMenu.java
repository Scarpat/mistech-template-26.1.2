package com.mistech.mistechmod.menu;

import com.mistech.mistechmod.block.IncenseHolderBlockEntity;
import com.mistech.mistechmod.item.IncenseItem;
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

public class IncenseHolderMenu extends AbstractContainerMenu {
    // Posições dos slots na textura da tela (176x166)
    public static final int LIGHTER_X = 26, STOCK_X = 62, ACTIVE_X = 98, ASH_X = 142, SLOT_Y = 35;

    private static final int CONTAINER_SLOTS = IncenseHolderBlockEntity.SLOT_COUNT; // 0..3
    private static final int INV_START = CONTAINER_SLOTS;          // 4
    private static final int INV_END = INV_START + 27;             // 31
    private static final int HOTBAR_END = INV_END + 9;             // 40

    private final Container container;
    private final ContainerData data;

    /** Construtor do lado do cliente (chamado pelo MenuType). */
    public IncenseHolderMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(CONTAINER_SLOTS), new SimpleContainerData(2));
    }

    public IncenseHolderMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(MistechMenus.INCENSE_HOLDER.get(), containerId);
        checkContainerSize(container, CONTAINER_SLOTS);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;

        this.addSlot(new FilteredSlot(container, IncenseHolderBlockEntity.SLOT_LIGHTER, LIGHTER_X, SLOT_Y, 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return IncenseHolderBlockEntity.isLighter(stack);
            }
        });
        this.addSlot(new FilteredSlot(container, IncenseHolderBlockEntity.SLOT_STOCK, STOCK_X, SLOT_Y, 64) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return IncenseHolderBlockEntity.isStockIncense(stack);
            }
        });
        // Incenso ativo e cinzas: só retirada
        this.addSlot(new FilteredSlot(container, IncenseHolderBlockEntity.SLOT_ACTIVE, ACTIVE_X, SLOT_Y, 1));
        this.addSlot(new FilteredSlot(container, IncenseHolderBlockEntity.SLOT_ASH, ASH_X, SLOT_Y, 64));

        this.addDataSlots(data);
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    /** Segundos restantes do incenso ativo (0 = nenhum queimando). */
    public int getRemainingSeconds() {
        return this.data.get(0);
    }

    public float getBurnProgress() {
        int max = this.data.get(1);
        return max <= 0 ? 0 : this.getRemainingSeconds() / (float) max;
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

        if (slotIndex < CONTAINER_SLOTS) {
            // do porta-incenso para o inventário
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (IncenseHolderBlockEntity.isLighter(stack)) {
            if (!this.moveItemStackTo(stack, IncenseHolderBlockEntity.SLOT_LIGHTER, IncenseHolderBlockEntity.SLOT_LIGHTER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (IncenseHolderBlockEntity.isStockIncense(stack)) {
            if (!this.moveItemStackTo(stack, IncenseHolderBlockEntity.SLOT_STOCK, IncenseHolderBlockEntity.SLOT_STOCK + 1, false)) {
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
        slot.onTake(player, original);
        return original;
    }

    /** Slot que por padrão não aceita nada (só retirada) e tem limite de pilha próprio. */
    private static class FilteredSlot extends Slot {
        private final int maxStack;

        FilteredSlot(Container container, int index, int x, int y, int maxStack) {
            super(container, index, x, y);
            this.maxStack = maxStack;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return this.maxStack;
        }
    }
}
