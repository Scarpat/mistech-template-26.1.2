package com.mistech.mistechmod.menu;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.mithril.GearRunes;
import com.mistech.mistechmod.registry.MistechMenus;
import com.mistech.mistechmod.registry.MithrilContent;

import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Bigorna Rúnica. Espaço 0 = peça (Adamantina 3, Oricalco 6, Oricalco Puro 8 engastes); 1..8 = runas.
 * As runas mostradas são as que estão gravadas na peça: colocar/tirar uma runa grava na hora no componente
 * {@code mistech:gear_runes}; tirar a peça leva as runas junto (os espaços esvaziam sem devolver nada).
 */
public class RuneAnvilMenu extends AbstractContainerMenu {
    public static final int GEAR_X = 26, GEAR_Y = 35;
    public static final int RUNE_X0 = 72, RUNE_Y0 = 24, RUNE_STEP = 22;

    private static final int CONTAINER_SLOTS = 1 + GearRunes.MAX_SLOTS; // 9
    private static final int INV_START = CONTAINER_SLOTS;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_END = INV_END + 9;

    private final ContainerLevelAccess access;
    private final SimpleContainer container = new SimpleContainer(CONTAINER_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            RuneAnvilMenu.this.slotsChanged(this);
        }
    };
    /** A peça cujas runas estão nos espaços agora (comparação por objeto). */
    private ItemStack loaded = ItemStack.EMPTY;
    private boolean syncing;

    public RuneAnvilMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public RuneAnvilMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(MistechMenus.RUNE_ANVIL.get(), containerId);
        this.access = access;
        this.addSlot(new Slot(this.container, 0, GEAR_X, GEAR_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GearRunes.slots(stack) > 0;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < GearRunes.MAX_SLOTS; i++) {
            this.addSlot(new RuneSlot(i, RUNE_X0 + (i % 4) * RUNE_STEP, RUNE_Y0 + (i / 4) * RUNE_STEP));
        }
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    /** Engastes da peça que está na bigorna (0 = vazia). */
    public int capacity() {
        return GearRunes.slots(this.container.getItem(0));
    }

    public ItemStack gear() {
        return this.container.getItem(0);
    }

    @Override
    public void slotsChanged(Container changed) {
        super.slotsChanged(changed);
        if (changed != this.container || this.syncing) {
            return;
        }
        this.syncing = true;
        try {
            ItemStack gear = this.container.getItem(0);
            // (pegar a peça com o cursor deixa o MESMO objeto no espaço, só que com 0 itens: por isso o isEmpty)
            if (gear.isEmpty() || gear != this.loaded) {
                // peça nova (ou saiu): mostra as runas dela
                this.loaded = gear.isEmpty() ? ItemStack.EMPTY : gear;
                NonNullList<ItemStack> runes = gear.isEmpty() ? NonNullList.withSize(GearRunes.MAX_SLOTS, ItemStack.EMPTY) : GearRunes.load(gear);
                int slots = GearRunes.slots(gear);
                for (int i = 0; i < GearRunes.MAX_SLOTS; i++) {
                    this.container.setItem(1 + i, i < slots ? runes.get(i) : ItemStack.EMPTY);
                }
            } else if (!gear.isEmpty()) {
                // mexeram numa runa: grava na peça
                int slots = GearRunes.slots(gear);
                List<ItemStack> runes = new ArrayList<>();
                for (int i = 0; i < slots; i++) {
                    runes.add(this.container.getItem(1 + i));
                }
                int before = GearRunes.load(gear).stream().mapToInt(s -> s.isEmpty() ? 0 : 1).sum();
                GearRunes.store(gear, runes);
                int after = (int) runes.stream().filter(s -> !s.isEmpty()).count();
                this.access.execute((level, pos) -> {
                    if (after > before) {
                        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.3F);
                        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
                    } else if (after < before) {
                        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 1.4F);
                    }
                });
            }
        } finally {
            this.syncing = false;
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // as runas já estão gravadas na peça: só esvazia os espaços e devolve a peça
        this.syncing = true;
        for (int i = 1; i < CONTAINER_SLOTS; i++) {
            this.container.removeItemNoUpdate(i);
        }
        this.syncing = false;
        ItemStack gear = this.container.removeItemNoUpdate(0);
        if (!player.level().isClientSide()) {
            this.clearContainer(player, new SimpleContainer(gear));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, MithrilContent.RUNE_ANVIL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < CONTAINER_SLOTS) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (GearRunes.slots(stack) > 0 && this.container.getItem(0).isEmpty()) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (RuneElement.ofRune(stack) != null && capacity() > 0) {
            if (!this.moveItemStackTo(stack, 1, 1 + capacity(), false)) {
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

    /** Espaço de runa: só aparece/aceita até o número de engastes da peça. */
    private class RuneSlot extends Slot {
        private final int socket;

        RuneSlot(int socket, int x, int y) {
            super(RuneAnvilMenu.this.container, 1 + socket, x, y);
            this.socket = socket;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.socket < capacity() && RuneElement.ofRune(stack) != null;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return this.socket < capacity();
        }
    }
}
