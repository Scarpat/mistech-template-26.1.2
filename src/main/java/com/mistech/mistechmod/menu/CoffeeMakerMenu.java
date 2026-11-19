package com.mistech.mistechmod.menu;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.MakerTanks;
import com.mistech.mistechmod.block.RunicCoffeeMakerBlockEntity;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.core.registries.BuiltInRegistries;
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
 * Tela da Cafeteira Rúnica: 3 linhas (café, beladona, cogumelo), cada uma com entrada, barra de progresso e saída,
 * mais as Xícaras vazias (dividas pelas linhas) e as barras dos 4 tanques.
 */
public class CoffeeMakerMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 212;
    public static final int IMAGE_HEIGHT = 184;
    public static final int[] COLUMN_X = {46, 102, 158};
    public static final int INPUT_Y = 24;
    public static final int OUTPUT_Y = 54;
    public static final int CUPS_X = 10;
    public static final int CUPS_Y = 39;
    public static final int INV_Y = 102;
    public static final int INV_X = (IMAGE_WIDTH - 162) / 2;

    private static final int MACHINE_SLOTS = RunicCoffeeMakerBlockEntity.SLOTS;
    private static final int INV_START = MACHINE_SLOTS;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_END = INV_END + 9;

    private final Container container;
    private final ContainerData data;

    public CoffeeMakerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOTS),
                new SimpleContainerData(RunicCoffeeMakerBlockEntity.LINES * 2 + MakerTanks.TANKS * 2));
    }

    public CoffeeMakerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(MistechMenus.COFFEE_MAKER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, RunicCoffeeMakerBlockEntity.LINES * 2 + MakerTanks.TANKS * 2);
        this.container = container;
        this.data = data;

        for (int line = 0; line < RunicCoffeeMakerBlockEntity.LINES; line++) {
            int l = line;
            this.addSlot(new Slot(container, line, COLUMN_X[line], INPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return RunicCoffeeMakerBlockEntity.acceptsIngredient(l, stack);
                }
            });
        }
        this.addSlot(new Slot(container, RunicCoffeeMakerBlockEntity.SLOT_CUPS, CUPS_X, CUPS_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Mistech.CUP.get());
            }
        });
        for (int line = 0; line < RunicCoffeeMakerBlockEntity.LINES; line++) {
            this.addSlot(new Slot(container, RunicCoffeeMakerBlockEntity.SLOT_OUTPUT + line, COLUMN_X[line], OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        this.addDataSlots(data);
        this.addStandardInventorySlots(inventory, INV_X, INV_Y);
    }

    /** Progresso (0 a 100) da linha; 0 = parada. */
    public int getProgress(int line) {
        return this.data.get(line);
    }

    public Fluid getTankFluid(int tank) {
        return BuiltInRegistries.FLUID.byId(this.data.get(RunicCoffeeMakerBlockEntity.LINES + tank));
    }

    /** Quantidade em mB (guardada ÷10 na sincronização). */
    public int getTankAmount(int tank) {
        return this.data.get(RunicCoffeeMakerBlockEntity.LINES + MakerTanks.TANKS + tank) * 10;
    }

    /** Doses guardadas no reservatório da linha. */
    public int getServings(int line) {
        return this.data.get(RunicCoffeeMakerBlockEntity.LINES + MakerTanks.TANKS * 2 + line);
    }

    /** Botão "Encher": id = linha; 100 + linha = encher quantas der. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.container instanceof RunicCoffeeMakerBlockEntity maker) {
            boolean all = id >= 100;
            int line = all ? id - 100 : id;
            if (line >= 0 && line < RunicCoffeeMakerBlockEntity.LINES) {
                return maker.fillCups(line, all) > 0;
            }
        }
        return false;
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

        if (slotIndex < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int line = RunicCoffeeMakerBlockEntity.lineOf(stack);
            if (line >= 0) {
                if (!this.moveItemStackTo(stack, line, line + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(Mistech.CUP.get())) {
                if (!this.moveItemStackTo(stack, RunicCoffeeMakerBlockEntity.SLOT_CUPS, RunicCoffeeMakerBlockEntity.SLOT_CUPS + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex < INV_END) {
                if (!this.moveItemStackTo(stack, INV_END, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
                return ItemStack.EMPTY;
            }
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
