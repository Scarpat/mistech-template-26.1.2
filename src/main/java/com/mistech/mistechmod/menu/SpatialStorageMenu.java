package com.mistech.mistechmod.menu;

import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Tela de 162 espaços (18 x 9 = 3 baús duplos) usada pelo Barril Espacial e pela Mochila.
 * Não aceita itens que guardam itens (shulker, barril espacial, mochila...) para evitar "caixa dentro de caixa".
 */
public class SpatialStorageMenu extends AbstractContainerMenu {
    public static final int COLUMNS = 18;
    public static final int ROWS = 9;
    public static final int SIZE = COLUMNS * ROWS;

    // Layout da tela (textura 512x512, usada só a área imageWidth x imageHeight)
    public static final int IMAGE_WIDTH = 8 + COLUMNS * 18 + 8;     // 340
    public static final int GRID_Y = 18;
    public static final int INV_X = (IMAGE_WIDTH - 162) / 2;       // 89
    public static final int INV_Y = GRID_Y + ROWS * 18 + 14;       // 194
    public static final int IMAGE_HEIGHT = INV_Y + 58 + 18 + 6;    // 276

    private final Container container;
    /** Índice no inventário do jogador (0..35) da mochila aberta, que não pode ser mexida; -1 = nenhum. */
    private final int lockedInventorySlot;

    /** Construtor do lado do cliente (chamado pelo MenuType). */
    public SpatialStorageMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SIZE), -1);
    }

    public SpatialStorageMenu(int containerId, Inventory inventory, Container container, int lockedInventorySlot) {
        super(MistechMenus.SPATIAL_STORAGE.get(), containerId);
        checkContainerSize(container, SIZE);
        this.container = container;
        this.lockedInventorySlot = lockedInventorySlot;
        container.startOpen(inventory.player);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                this.addSlot(new Slot(container, col + row * COLUMNS, 8 + col * 18, GRID_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return canStore(stack);
                    }
                });
            }
        }

        // inventório do jogador (3 linhas) + barra de atalhos, com o espaço da mochila aberta travado
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                this.addSlot(this.playerSlot(inventory, index, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(this.playerSlot(inventory, col, INV_X + col * 18, INV_Y + 58));
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

    /** Itens que podem ir para dentro (nada de shulker/barril espacial/mochila). */
    public static boolean canStore(ItemStack stack) {
        return stack.canFitInsideContainerItems();
    }

    public Container getContainer() {
        return this.container;
    }

    public int getLockedInventorySlot() {
        return this.lockedInventorySlot;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        // trocar com a tecla numérica para dentro/fora do espaço travado também é bloqueado
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
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem() || !slot.mayPickup(player)) {
            return original;
        }
        ItemStack stack = slot.getItem();
        original = stack.copy();
        if (slotIndex < SIZE) {
            if (!this.moveItemStackTo(stack, SIZE, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!canStore(stack) || !this.moveItemStackTo(stack, 0, SIZE, false)) {
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
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
