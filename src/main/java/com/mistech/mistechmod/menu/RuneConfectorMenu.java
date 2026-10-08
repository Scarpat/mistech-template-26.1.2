package com.mistech.mistechmod.menu;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.confector.RuneConfectorBlockEntity;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * Tela do Confector de Runas (276 x 278).
 * Esquerda: 27 ingredientes (3 x 9), 2 tanques com espaço para despejar garrafa/balde.
 * Meio: runas que dá para fazer. Direita: giz e saída (3 x 3). Canto de cima: botão de energia.
 */
public class RuneConfectorMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 276;
    public static final int IMAGE_HEIGHT = 278;

    public static final int STORAGE_X = 8, STORAGE_Y = 18;
    public static final int TANK0_X = 70, TANK1_X = 90, TANK_Y = 18, TANK_W = 16, TANK_H = 104;
    public static final int FLUID_SLOT_Y = 126;
    public static final int LIST_X = 114, LIST_Y = 18, LIST_COLS = 4, LIST_ROWS = 7, LIST_CELL = 20;
    public static final int CHALK_X = 216, CHALK_Y = 30;
    public static final int OUTPUT_X = 198, OUTPUT_Y = 72;
    public static final int POWER_X = IMAGE_WIDTH - 22, POWER_Y = 4, POWER_SIZE = 16;
    public static final int INV_X = (IMAGE_WIDTH - 162) / 2, INV_Y = 196;

    public static final int BUTTON_MANY = 100;
    public static final int BUTTON_POWER = 1000;

    private static final int BE_SLOTS = RuneConfectorBlockEntity.SIZE;
    private static final int INV_START = BE_SLOTS, INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;
    private final @Nullable RuneConfectorBlockEntity blockEntity;

    /** Construtor do lado do cliente. */
    public RuneConfectorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(BE_SLOTS), new SimpleContainerData(RuneConfectorBlockEntity.DATA_COUNT), null);
    }

    public RuneConfectorMenu(int containerId, Inventory inventory, RuneConfectorBlockEntity blockEntity, ContainerData data) {
        this(containerId, inventory, blockEntity, data, blockEntity);
    }

    private RuneConfectorMenu(int containerId, Inventory inventory, Container container, ContainerData data,
                              @Nullable RuneConfectorBlockEntity blockEntity) {
        super(MistechMenus.RUNE_CONFECTOR.get(), containerId);
        checkContainerSize(container, BE_SLOTS);
        checkContainerDataCount(data, RuneConfectorBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new FilteredSlot(container, col + row * 3, STORAGE_X + col * 18, STORAGE_Y + row * 18));
            }
        }
        this.addSlot(new FilteredSlot(container, RuneConfectorBlockEntity.CHALK_SLOT, CHALK_X, CHALK_Y) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < RuneConfectorBlockEntity.OUTPUT_SLOTS; i++) {
            this.addSlot(new FilteredSlot(container, RuneConfectorBlockEntity.OUTPUT_START + i,
                    OUTPUT_X + (i % 3) * 18, OUTPUT_Y + (i / 3) * 18));
        }
        this.addSlot(new FilteredSlot(container, RuneConfectorBlockEntity.FLUID_SLOT_0, TANK0_X, FLUID_SLOT_Y) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new FilteredSlot(container, RuneConfectorBlockEntity.FLUID_SLOT_1, TANK1_X, FLUID_SLOT_Y) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        this.addDataSlots(data);
        this.addStandardInventorySlots(inventory, INV_X, INV_Y);
        container.startOpen(inventory.player);
    }

    /** Slot que respeita Container#canPlaceItem (giz só giz, saída não aceita nada, espaço de fluido só cheio). */
    private static class FilteredSlot extends Slot {
        FilteredSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return RuneConfectorBlockEntity.isValidFor(this.getContainerSlot(), stack);
        }
    }

    // ----- dados para a tela -----

    /** Índice da runa escolhida (-1 = nenhuma). */
    public int getSelected() {
        return this.data.get(0) - 1;
    }

    public boolean isAutoMode() {
        return this.data.get(1) != 0;
    }

    public int getTankAmount(int tank) {
        int base = tank == 0 ? 2 : 5;
        return (this.data.get(base) & 0xFFFF) | (this.data.get(base + 1) << 16);
    }

    public Fluid getTankFluid(int tank) {
        return RuneConfectorBlockEntity.fluidFromId(this.data.get(tank == 0 ? 4 : 7));
    }

    public boolean isCraftable(int index) {
        if (index < 0 || index >= RuneConfectorBlockEntity.MAX_RECIPES) {
            return false;
        }
        return (this.data.get(8 + index / 16) & (1 << (index % 16))) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.blockEntity != null && player instanceof ServerPlayer serverPlayer) {
            this.blockEntity.handleButton(serverPlayer, id);
            this.broadcastChanges();
        }
        return true;
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
        if (slotIndex < BE_SLOTS) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (RuneConfectorBlockEntity.isValidFor(RuneConfectorBlockEntity.CHALK_SLOT, stack) && !this.slots.get(RuneConfectorBlockEntity.CHALK_SLOT).hasItem()) {
            if (!this.moveItemStackTo(stack, RuneConfectorBlockEntity.CHALK_SLOT, RuneConfectorBlockEntity.CHALK_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, RuneConfectorBlockEntity.STORAGE_SLOTS, false)) {
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
