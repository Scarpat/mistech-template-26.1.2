package com.mistech.mistechmod.archive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Mesa do Arquivo: a grade com tudo o que está guardado (lista mandada pelo servidor em {@link ArchiveNetwork.SyncPayload}),
 * mesa de trabalho 3×3 que se reabastece do Arquivo e o inventário do jogador.
 * Slots: 0 = resultado, 1–9 = grade de craft, 10–36 = inventário, 37–45 = barra.
 */
public class ArchiveTerminalMenu extends AbstractContainerMenu {
    public static final int WIDTH = 194, HEIGHT = 266;
    public static final int GRID_X = 8, GRID_Y = 20, COLS = 9, ROWS = 5, CELL = 18;
    public static final int CRAFT_X = 30, CRAFT_Y = 118, RESULT_X = 124, RESULT_Y = 136;
    public static final int INV_X = 8, INV_Y = 184, HOTBAR_Y = 242;

    public static final int TAKE = 0, TAKE_HALF = 1, TAKE_SHIFT = 2, PUT_ALL = 3, PUT_ONE = 4, CLEAR_GRID = 5;

    private final Player player;
    private final CraftingContainer craft = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer result = new ResultContainer();
    // servidor
    private final @Nullable ServerLevel coreLevel;
    private final @Nullable BlockPos corePos;
    private final Predicate<Player> valid;
    private List<ArchiveNetwork.Entry> lastSent = null;
    private boolean lastPowered;
    private int ticks;
    // cliente
    public List<ArchiveNetwork.Entry> entries = List.of();
    public boolean powered = true;

    /** Cliente. */
    public ArchiveTerminalMenu(int id, Inventory inventory) {
        this(id, inventory, null, null, p -> true);
    }

    /** Servidor. */
    public ArchiveTerminalMenu(int id, Inventory inventory, @Nullable ServerLevel coreLevel, @Nullable BlockPos corePos, Predicate<Player> valid) {
        super(ArchiveContent.TERMINAL_MENU.get(), id);
        this.player = inventory.player;
        this.coreLevel = coreLevel;
        this.corePos = corePos;
        this.valid = valid;
        this.addSlot(new RefillResultSlot(inventory.player, this.craft, this.result, 0, RESULT_X, RESULT_Y));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                this.addSlot(new Slot(this.craft, c + r * 3, CRAFT_X + c * 18, CRAFT_Y + r * 18));
            }
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(inventory, c + r * 9 + 9, INV_X + c * 18, INV_Y + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(inventory, c, INV_X + c * 18, HOTBAR_Y));
        }
    }

    // ------------------------------------------------------------------ servidor: Arquivo

    private @Nullable ArchiveCoreBlockEntity core() {
        if (this.coreLevel == null || this.corePos == null) {
            return null;
        }
        this.coreLevel.getChunk(this.corePos); // carrega se estiver longe (Espelho)
        return this.coreLevel.getBlockEntity(this.corePos) instanceof ArchiveCoreBlockEntity core ? core : null;
    }

    private @Nullable ArchiveNet net() {
        ArchiveCoreBlockEntity core = this.core();
        return core != null && core.isPowered() ? ArchiveNet.of(this.coreLevel, core) : null;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(this.player instanceof ServerPlayer serverPlayer) || this.coreLevel == null) {
            return;
        }
        if (this.ticks++ % 10 != 0 && this.lastSent != null) {
            return;
        }
        ArchiveNet net = this.net();
        List<ArchiveNetwork.Entry> list = new ArrayList<>();
        if (net != null) {
            for (Map.Entry<ItemResource, Long> e : net.snapshot().entrySet()) {
                list.add(new ArchiveNetwork.Entry(e.getKey(), e.getValue()));
            }
        }
        boolean powered = net != null;
        if (!list.equals(this.lastSent) || powered != this.lastPowered) {
            this.lastSent = list;
            this.lastPowered = powered;
            PacketDistributor.sendToPlayer(serverPlayer, new ArchiveNetwork.SyncPayload(this.containerId, list, powered));
        }
    }

    private void resync() {
        this.lastSent = null;
    }

    /** Clique na grade do Arquivo (mandado pelo cliente). */
    public void handleAction(int action, ItemResource resource) {
        ArchiveNet net = this.net();
        if (net == null) {
            return;
        }
        ItemStack carried = this.getCarried();
        switch (action) {
            case TAKE, TAKE_HALF -> {
                if (resource.isEmpty()) {
                    return;
                }
                if (carried.isEmpty()) {
                    long have = net.count(resource);
                    int want = action == TAKE ? resource.getMaxStackSize() : (int) Math.min(resource.getMaxStackSize(), (have + 1) / 2);
                    this.setCarried(net.extractStack(resource, want));
                } else if (resource.matches(carried) && carried.getCount() < carried.getMaxStackSize()) {
                    ItemStack more = net.extractStack(resource, action == TAKE ? carried.getMaxStackSize() - carried.getCount() : 1);
                    carried.grow(more.getCount());
                    this.setCarried(carried);
                }
            }
            case TAKE_SHIFT -> {
                if (resource.isEmpty()) {
                    return;
                }
                ItemStack stack = net.extractStack(resource, resource.getMaxStackSize());
                if (!stack.isEmpty()) {
                    this.player.getInventory().add(stack);
                    if (!stack.isEmpty()) {
                        net.insertStack(stack);
                    }
                }
            }
            case PUT_ALL -> this.setCarried(net.insertStack(carried));
            case PUT_ONE -> {
                if (!carried.isEmpty() && net.insert(ItemResource.of(carried), 1, false) == 1) {
                    carried.shrink(1);
                    this.setCarried(carried);
                }
            }
            case CLEAR_GRID -> {
                for (int i = 0; i < this.craft.getContainerSize(); i++) {
                    ItemStack rest = net.insertStack(this.craft.getItem(i));
                    this.craft.setItem(i, ItemStack.EMPTY);
                    if (!rest.isEmpty()) {
                        this.player.getInventory().placeItemBackInInventory(rest);
                    }
                }
            }
            default -> {
            }
        }
        this.resync();
    }

    // ------------------------------------------------------------------ craft

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == this.craft && this.player instanceof ServerPlayer serverPlayer) {
            ServerLevel level = serverPlayer.level();
            CraftingInput input = this.craft.asCraftInput();
            Optional<RecipeHolder<CraftingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
            ItemStack out = recipe.map(h -> h.value().assemble(input)).orElse(ItemStack.EMPTY);
            this.result.setRecipeUsed(recipe.orElse(null));
            this.result.setItem(0, out);
        }
    }

    /** Depois de pegar o resultado, repõe na grade os itens que acabaram, tirando do Arquivo. */
    private class RefillResultSlot extends ResultSlot {
        RefillResultSlot(Player player, CraftingContainer craft, Container result, int index, int x, int y) {
            super(player, craft, result, index, x, y);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            List<ItemStack> before = new ArrayList<>();
            for (int i = 0; i < ArchiveTerminalMenu.this.craft.getContainerSize(); i++) {
                before.add(ArchiveTerminalMenu.this.craft.getItem(i).copy());
            }
            super.onTake(player, stack);
            ArchiveNet net = ArchiveTerminalMenu.this.net();
            if (net == null) {
                return;
            }
            for (int i = 0; i < before.size(); i++) {
                ItemStack was = before.get(i);
                if (!was.isEmpty() && ArchiveTerminalMenu.this.craft.getItem(i).isEmpty()) {
                    ItemStack refill = net.extractStack(ItemResource.of(was), 1);
                    if (!refill.isEmpty()) {
                        ArchiveTerminalMenu.this.craft.setItem(i, refill);
                    }
                }
            }
            ArchiveTerminalMenu.this.resync();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            stack.getItem().onCraftedBy(stack, player);
            if (!this.moveItemStackTo(stack, 10, 46, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index < 10) {
            if (!this.moveItemStackTo(stack, 10, 46, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            ArchiveNet net = this.net();
            if (net != null) {
                slot.setByPlayer(net.insertStack(stack));
                this.resync();
            }
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        if (index == 0) {
            player.drop(stack, false);
        }
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.result && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            ArchiveNet net = this.net();
            for (int i = 0; i < this.craft.getContainerSize(); i++) {
                ItemStack stack = this.craft.removeItemNoUpdate(i);
                ItemStack rest = net == null ? stack : net.insertStack(stack);
                if (!rest.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(rest);
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.valid.test(player);
    }
}
