package com.mistech.mistechmod.infuser;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Tela do Infusor Arcano (176 x 206): materiais à esquerda, lista de encantos à direita. */
public class ArcaneInfuserMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176, IMAGE_HEIGHT = 206;
    public static final int TARGET_X = 17, TARGET_Y = 22;
    public static final int LAPIS_X = 8, ESSENCE_X = 26, ROW1_Y = 48, STAR_X = 8, PURIFIED_X = 26, ROW2_Y = 70;
    public static final int BAR_X = 48, BAR_Y = 18, BAR_W = 6, BAR_H = 68;
    public static final int INV_X = 8, INV_Y = 124;

    private static final int INV_START = InfuserRules.SIZE, INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;
    private final @Nullable ArcaneInfuserBlockEntity blockEntity;

    public ArcaneInfuserMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(InfuserRules.SIZE), new SimpleContainerData(ArcaneInfuserBlockEntity.DATA_COUNT), null);
    }

    public ArcaneInfuserMenu(int containerId, Inventory inventory, ArcaneInfuserBlockEntity blockEntity, ContainerData data) {
        this(containerId, inventory, blockEntity, data, blockEntity);
    }

    private ArcaneInfuserMenu(int containerId, Inventory inventory, Container container, ContainerData data,
            @Nullable ArcaneInfuserBlockEntity blockEntity) {
        super(InfuserContent.ARCANE_INFUSER_MENU.get(), containerId);
        checkContainerSize(container, InfuserRules.SIZE);
        checkContainerDataCount(data, ArcaneInfuserBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;
        this.addSlot(new InfuserSlot(container, InfuserRules.SLOT_TARGET, TARGET_X, TARGET_Y, 1));
        this.addSlot(new InfuserSlot(container, InfuserRules.SLOT_LAPIS, LAPIS_X, ROW1_Y, 64));
        this.addSlot(new InfuserSlot(container, InfuserRules.SLOT_ESSENCE, ESSENCE_X, ROW1_Y, 64));
        this.addSlot(new InfuserSlot(container, InfuserRules.SLOT_STAR, STAR_X, ROW2_Y, 64));
        this.addSlot(new InfuserSlot(container, InfuserRules.SLOT_PURIFIED, PURIFIED_X, ROW2_Y, 64));
        this.addDataSlots(data);
        this.addStandardInventorySlots(inventory, INV_X, INV_Y);
    }

    private static class InfuserSlot extends Slot {
        private final int max;

        InfuserSlot(Container container, int index, int x, int y, int max) {
            super(container, index, x, y);
            this.max = max;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return InfuserRules.validFor(this.getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return this.max;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(this.max, stack.getMaxStackSize());
        }
    }

    public ItemStack target() {
        return this.container.getItem(InfuserRules.SLOT_TARGET);
    }

    public int count(int slot) {
        return this.container.getItem(slot).getCount();
    }

    public int energy() {
        return (this.data.get(0) & 0x7FFF) | ((this.data.get(1) & 0x7FFF) << 15);
    }

    public int tier() {
        return Math.max(1, this.data.get(2));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.blockEntity != null) {
            this.blockEntity.infuse(player, id);
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
        if (slotIndex < InfuserRules.SIZE) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 1; i < InfuserRules.SIZE && !moved; i++) {
                if (InfuserRules.validFor(i, stack) && this.moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                }
            }
            if (!moved && !this.slots.get(InfuserRules.SLOT_TARGET).hasItem()) {
                moved = this.moveItemStackTo(stack, InfuserRules.SLOT_TARGET, InfuserRules.SLOT_TARGET + 1, false);
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
