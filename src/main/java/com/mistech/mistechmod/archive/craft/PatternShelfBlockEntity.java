package com.mistech.mistechmod.archive.craft;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.archive.ArchiveRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Estante de Padrões: 18 Pergaminhos de Padrão. Faz parte do Arquivo do Núcleo que alcança (como o Relé). */
public class PatternShelfBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 18;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public PatternShelfBlockEntity(BlockPos pos, BlockState state) {
        super(CraftContent.SHELF_BE.get(), pos, state);
    }

    /** Padrões prontos guardados aqui. */
    public List<Pattern> patterns() {
        List<Pattern> list = new ArrayList<>();
        for (ItemStack stack : this.items) {
            Pattern p = PatternScrollItem.get(stack);
            if (p != null && p.complete()) {
                list.add(p);
            }
        }
        return list;
    }

    public int count() {
        int n = 0;
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                n++;
            }
        }
        return n;
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
        return stack.getItem() instanceof PatternScrollItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.mistech.pattern_shelf");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x2, id, inventory, this, 2);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            ArchiveRegistry.add(this.level, ArchiveRegistry.Kind.SHELF, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            ArchiveRegistry.remove(this.level, ArchiveRegistry.Kind.SHELF, this.worldPosition);
        }
        super.setRemoved();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }
}
