package com.mistech.mistechmod.block;

import com.mistech.mistechmod.menu.WritingTableMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Guarda giz, papel e os 4 ingredientes da Mesa de Escrita (os itens ficam na mesa ao fechar). */
public class WritingTableBlockEntity extends BaseContainerBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.mistech.writing_table");

    private NonNullList<ItemStack> items = NonNullList.withSize(WritingTableMenu.INPUT_COUNT, ItemStack.EMPTY);

    public WritingTableBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.WRITING_TABLE.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    public int getContainerSize() {
        return WritingTableMenu.INPUT_COUNT;
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case WritingTableMenu.CHALK -> WritingTableMenu.isChalk(stack);
            case WritingTableMenu.PAPER -> WritingTableMenu.isPaper(stack);
            default -> true;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new WritingTableMenu(containerId, inventory, this,
                this.level != null ? ContainerLevelAccess.create(this.level, this.worldPosition) : ContainerLevelAccess.NULL);
    }
}
