package com.mistech.mistechmod.block.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Block entity que guarda 1 item (1 unidade), sincronizado com o cliente para desenhar o item flutuando. */
public abstract class SingleItemBlockEntity extends BlockEntity implements Container {
    protected ItemStack stored = ItemStack.EMPTY;

    protected SingleItemBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStack getStored() {
        return this.stored;
    }

    public void setStored(ItemStack stack) {
        this.stored = stack;
        this.setChanged();
    }

    /** Pode mexer no item agora? (ex.: não durante um ritual) */
    protected boolean isLocked() {
        return false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.stored = input.read("Item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.stored.isEmpty()) {
            output.store("Item", ItemStack.OPTIONAL_CODEC, this.stored);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ----- Container (1 espaço, 1 item) -----

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.stored.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.stored;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (this.stored.isEmpty() || amount <= 0 || this.isLocked()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = this.stored;
        this.stored = ItemStack.EMPTY;
        this.setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = this.stored;
        this.stored = ItemStack.EMPTY;
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.stored = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        this.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return this.stored.isEmpty() && !this.isLocked();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.stored = ItemStack.EMPTY;
        this.setChanged();
    }
}
