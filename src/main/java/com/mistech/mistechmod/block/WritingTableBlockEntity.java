package com.mistech.mistechmod.block;

import com.mistech.mistechmod.menu.WritingTableMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Guarda giz, papel e os 4 ingredientes da Mesa de Escrita (os itens ficam na mesa ao fechar).
 * Enquanto alguém escreve, o que vai sair ({@link #getPreview()}) flutua girando em cima da mesa.
 */
public class WritingTableBlockEntity extends BaseContainerBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.mistech.writing_table");

    private NonNullList<ItemStack> items = NonNullList.withSize(WritingTableMenu.INPUT_COUNT, ItemStack.EMPTY);

    /** O que a mesa está escrevendo agora (prévia da saída, só para desenhar; não é um item de verdade). */
    private ItemStack preview = ItemStack.EMPTY;

    public WritingTableBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.WRITING_TABLE.get(), pos, state);
    }

    public ItemStack getPreview() {
        return this.preview;
    }

    /** Chamado pelo menu (servidor) sempre que a saída muda; vazio quando não há receita ou a mesa fecha. */
    public void setPreview(ItemStack stack) {
        ItemStack next = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (ItemStack.matches(this.preview, next)) {
            return;
        }
        this.preview = next;
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            this.preview = ItemStack.EMPTY; // ninguém está escrevendo numa mesa recém-carregada
        }
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
        this.preview = input.read("Preview", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        if (!this.preview.isEmpty()) {
            output.store("Preview", ItemStack.OPTIONAL_CODEC, this.preview);
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

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new WritingTableMenu(containerId, inventory, this,
                this.level != null ? ContainerLevelAccess.create(this.level, this.worldPosition) : ContainerLevelAccess.NULL);
    }
}
