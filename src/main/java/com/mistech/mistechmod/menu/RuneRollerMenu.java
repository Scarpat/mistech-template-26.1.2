package com.mistech.mistechmod.menu;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.roller.RuneRollerBlockEntity;
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

/** Tela da Enroladora Rúnica (214 x 188): runas e folhas à esquerda, opções no meio, saída à direita. */
public class RuneRollerMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 214, IMAGE_HEIGHT = 188;
    public static final int RUNES_X = 8, RUNES_Y = 20;
    public static final int LEAVES_Y = 66, TOBACCO_X = 8, HEMP_X = 26, RESIDUE_X = 44;
    public static final int GRID_X = 68, GRID_Y = 18, GRID_COLS = 5, GRID_ROWS = 4, CELL = 18;
    public static final int OUTPUT_X = 186, OUTPUT_Y = 30;
    public static final int POWER_X = IMAGE_WIDTH - 22, POWER_Y = 4, POWER_SIZE = 16;
    public static final int PROGRESS_X = 164, PROGRESS_Y = 48;
    public static final int INV_X = (IMAGE_WIDTH - 162) / 2, INV_Y = 106;

    public static final int BUTTON_MANY = 100;
    public static final int BUTTON_POWER = 1000;

    private static final int BE_SLOTS = RuneRollerBlockEntity.SIZE;
    private static final int INV_START = BE_SLOTS, INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;
    private final @Nullable RuneRollerBlockEntity blockEntity;

    public RuneRollerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(BE_SLOTS), new SimpleContainerData(RuneRollerBlockEntity.DATA_COUNT), null);
    }

    public RuneRollerMenu(int containerId, Inventory inventory, RuneRollerBlockEntity blockEntity, ContainerData data) {
        this(containerId, inventory, blockEntity, data, blockEntity);
    }

    private RuneRollerMenu(int containerId, Inventory inventory, Container container, ContainerData data,
                           @Nullable RuneRollerBlockEntity blockEntity) {
        super(MistechMenus.RUNE_ROLLER.get(), containerId);
        checkContainerSize(container, BE_SLOTS);
        checkContainerDataCount(data, RuneRollerBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;

        for (int i = 0; i < RuneRollerBlockEntity.RUNE_SLOTS; i++) {
            this.addSlot(new RollerSlot(container, i, RUNES_X + (i % 2) * 18, RUNES_Y + (i / 2) * 18, 1));
        }
        this.addSlot(new RollerSlot(container, RuneRollerBlockEntity.TOBACCO_SLOT, TOBACCO_X, LEAVES_Y, 64));
        this.addSlot(new RollerSlot(container, RuneRollerBlockEntity.HEMP_SLOT, HEMP_X, LEAVES_Y, 64));
        this.addSlot(new RollerSlot(container, RuneRollerBlockEntity.RESIDUE_SLOT, RESIDUE_X, LEAVES_Y, 64));
        for (int i = 0; i < RuneRollerBlockEntity.OUTPUT_SLOTS; i++) {
            this.addSlot(new RollerSlot(container, RuneRollerBlockEntity.OUTPUT_START + i, OUTPUT_X, OUTPUT_Y + i * 18, 64));
        }
        this.addDataSlots(data);
        this.addStandardInventorySlots(inventory, INV_X, INV_Y);
    }

    private static class RollerSlot extends Slot {
        private final int max;

        RollerSlot(Container container, int index, int x, int y, int max) {
            super(container, index, x, y);
            this.max = max;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return RuneRollerBlockEntity.isValidFor(this.getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return this.max;
        }
    }

    public int getSelected() {
        return this.data.get(0) - 1;
    }

    public boolean isAutoMode() {
        return this.data.get(1) != 0;
    }

    public boolean isCraftable(int option) {
        int mask = (this.data.get(2) & 0xFFFF) | ((this.data.get(3) & 0xFFFF) << 16);
        return (mask & (1 << option)) != 0;
    }

    public float getProgress() {
        return this.data.get(4) / (float) RuneRollerBlockEntity.AUTO_TICKS;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.blockEntity != null) {
            this.blockEntity.handleButton(id);
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
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < BE_SLOTS) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < RuneRollerBlockEntity.OUTPUT_START && !moved; i++) {
                if (RuneRollerBlockEntity.isValidFor(i, stack) && this.moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return stack.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }
}
