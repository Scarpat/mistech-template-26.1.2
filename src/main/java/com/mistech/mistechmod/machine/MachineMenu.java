package com.mistech.mistechmod.machine;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu genérico das máquinas: os espaços vêm do {@link MachineLayout}.
 * Dados sincronizados (cada um cabe num short):
 * 0 = progresso, 1 = progresso máximo, 2/3 = energia (15 bits baixos / altos), 4 e 5 = informações da máquina, 6 = impulso de gnomo (0/1),
 * 7 e 8 = informações extras da máquina.
 */
public class MachineMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 9;

    private final MachineLayout layout;
    private final Container container;
    private final ContainerData data;
    private final int machineSlots;

    /** Cliente. */
    public MachineMenu(MenuType<MachineMenu> type, MachineLayout layout, int containerId, Inventory inventory) {
        this(type, layout, containerId, inventory, new SimpleContainer(layout.size()), new SimpleContainerData(DATA_COUNT));
    }

    public MachineMenu(MenuType<MachineMenu> type, MachineLayout layout, int containerId, Inventory inventory,
            Container container, ContainerData data) {
        super(type, containerId);
        checkContainerSize(container, layout.size());
        checkContainerDataCount(data, DATA_COUNT);
        this.layout = layout;
        this.container = container;
        this.data = data;
        this.machineSlots = layout.size();

        for (int i = 0; i < layout.size(); i++) {
            MachineLayout.SlotSpec spec = layout.slots.get(i);
            final int index = i;
            this.addSlot(new Slot(container, i, spec.x(), spec.y()) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return layout.accepts(index, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return spec.kind() == MachineLayout.Kind.RUNE || spec.kind() == MachineLayout.Kind.FILTER ? 1 : super.getMaxStackSize();
                }

                @Override
                public boolean mayPickup(Player player) {
                    return spec.kind() != MachineLayout.Kind.FILTER && super.mayPickup(player);
                }
            });
        }
        this.addDataSlots(data);
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    public MachineLayout layout() {
        return this.layout;
    }

    public float progress() {
        int max = this.data.get(1);
        return max <= 0 ? 0.0F : Math.min(1.0F, this.data.get(0) / (float) max);
    }

    public int energy() {
        return (this.data.get(2) & 0x7FFF) | ((this.data.get(3) & 0x7FFF) << 15);
    }

    public boolean boosted() {
        return this.data.get(6) != 0;
    }

    /** Informação da máquina: 0 e 1 = dados 4 e 5; 2 e 3 = dados 7 e 8. */
    public int info(int i) {
        return this.data.get(i < 2 ? 4 + i : 5 + i);
    }

    /** Espaços de filtro: clicar com um item copia 1 como amostra; clicar com a mão vazia limpa. */
    @Override
    public void clicked(int slotIndex, int buttonNum, net.minecraft.world.inventory.ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < this.machineSlots && this.layout.kind(slotIndex) == MachineLayout.Kind.FILTER) {
            if (input == net.minecraft.world.inventory.ContainerInput.PICKUP || input == net.minecraft.world.inventory.ContainerInput.QUICK_MOVE) {
                Slot slot = this.slots.get(slotIndex);
                ItemStack carried = this.getCarried();
                slot.set(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            }
            return;
        }
        super.clicked(slotIndex, buttonNum, input, player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return !(target.index < this.machineSlots && this.layout.kind(target.index) == MachineLayout.Kind.FILTER)
                && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return this.container instanceof MachineBlockEntity machine && machine.onMenuButton(player, id);
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
        int invStart = this.machineSlots, invEnd = invStart + 27, hotbarEnd = invEnd + 9;

        if (slotIndex < this.machineSlots) {
            if (!this.moveItemStackTo(stack, invStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            // runa, combustível e entrada, nessa ordem
            for (MachineLayout.Kind kind : new MachineLayout.Kind[] {MachineLayout.Kind.RUNE, MachineLayout.Kind.FUEL, MachineLayout.Kind.INPUT}) {
                for (int i = 0; i < this.machineSlots && !stack.isEmpty(); i++) {
                    if (this.layout.kind(i) == kind && this.layout.accepts(i, stack)
                            && this.moveItemStackTo(stack, i, i + 1, false)) {
                        moved = true;
                    }
                }
            }
            if (!moved) {
                if (slotIndex < invEnd) {
                    if (!this.moveItemStackTo(stack, invEnd, hotbarEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, invStart, invEnd, false)) {
                    return ItemStack.EMPTY;
                }
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
