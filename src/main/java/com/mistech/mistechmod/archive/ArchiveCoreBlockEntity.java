package com.mistech.mistechmod.archive;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Guarda os Pergaminhos (9 espaços; só pergaminhos contam) e a energia. Gasta {@link ScrollTier#energy} FE/t por pergaminho
 * (mínimo {@link #IDLE_COST}); sem energia o Arquivo fica desligado.
 */
public class ArchiveCoreBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 9;
    public static final int CAPACITY = 200_000;
    public static final int MAX_INPUT = 5_000;
    public static final int IDLE_COST = 1;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(CAPACITY, MAX_INPUT, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            ArchiveCoreBlockEntity.this.setChanged();
        }
    };
    private boolean powered;

    public ArchiveCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ArchiveContent.CORE_BE.get(), pos, state);
    }

    public SimpleEnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public boolean isPowered() {
        return this.powered;
    }

    public int cost() {
        int cost = IDLE_COST;
        for (ItemStack stack : this.items) {
            if (stack.getItem() instanceof ArchiveScrollItem scroll) {
                cost += scroll.tier().energy;
            }
        }
        return cost;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArchiveCoreBlockEntity core) {
        int cost = core.cost();
        int have = core.energy.getAmountAsInt();
        boolean wasPowered = core.powered;
        core.powered = have >= cost;
        if (core.powered) {
            core.energy.set(have - cost);
        }
        if (level.getGameTime() % 20 == 0 && level instanceof ServerLevel server) {
            ArchiveCells cells = ArchiveCells.get(server.getServer());
            for (ItemStack stack : core.items) {
                if (stack.getItem() instanceof ArchiveScrollItem) {
                    UUID id = stack.get(ArchiveContent.CELL_ID.get());
                    if (id == null) {
                        id = UUID.randomUUID();
                        stack.set(ArchiveContent.CELL_ID.get(), id);
                    }
                    ArchiveCells.Cell cell = cells.cell(id);
                    ArchiveContent.Usage usage = new ArchiveContent.Usage(cell.items.size(), cell.total());
                    if (!usage.equals(stack.get(ArchiveContent.CELL_USAGE.get()))) {
                        stack.set(ArchiveContent.CELL_USAGE.get(), usage);
                        core.setChanged();
                    }
                }
            }
        }
        if (wasPowered != core.powered) {
            core.setChanged();
        }
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof ArchiveScrollItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.mistech.archive_core");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, id, inventory, this, 1);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            ArchiveRegistry.add(this.level, ArchiveRegistry.Kind.CORE, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            ArchiveRegistry.remove(this.level, ArchiveRegistry.Kind.CORE, this.worldPosition);
        }
        super.setRemoved();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.energy.deserialize(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        this.energy.serialize(output);
    }
}
