package com.mistech.mistechmod.extraplanar;

import com.mistech.mistechmod.menu.SpatialStorageMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Barril Extraplanar: os 162 espaços não ficam no bloco, e sim no canal do código ({@link ExtraplanarData}).
 * Todos os barris com o mesmo código mostram e mexem no mesmo conteúdo. Sem código não guarda nada.
 */
public class ExtraplanarBarrelBlockEntity extends BaseContainerBlockEntity implements CodedBlockEntity {
    private String code = "";
    /** Usado só quando não há canal (cliente / sem código); nunca recebe itens de verdade. */
    private final NonNullList<ItemStack> idle = NonNullList.withSize(SpatialStorageMenu.SIZE, ItemStack.EMPTY);
    /** Aplicando componentes do item: o conteúdo (minecraft:container) nunca pode cair no canal. */
    private boolean applyingComponents;

    public ExtraplanarBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ExtraplanarContent.BARREL_BE.get(), pos, state);
    }

    @Override
    public String getCode() {
        return this.code;
    }

    /** Ligado a um canal de verdade (servidor e com código). */
    public boolean isLinked() {
        return !this.code.isEmpty() && this.level instanceof ServerLevel;
    }

    @Override
    public void setCode(String newCode) {
        String clean = CodedBlockEntity.clean(newCode);
        if (clean.equals(this.code)) {
            return;
        }
        this.code = clean;
        this.setChanged();
        this.invalidateCapabilities();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
        }
    }

    @Override
    protected Component getDefaultName() {
        return this.code.isEmpty() ? Component.translatable("container.mistech.extraplanar_barrel")
                : Component.translatable("container.mistech.extraplanar_barrel.code", this.code);
    }

    @Override
    public int getContainerSize() {
        return SpatialStorageMenu.SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        if (this.isLinked() && !this.applyingComponents) {
            return ExtraplanarData.get(((ServerLevel) this.level).getServer()).items(this.code);
        }
        return this.idle;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        NonNullList<ItemStack> target = this.getItems();
        for (int i = 0; i < target.size() && i < items.size(); i++) {
            target.set(i, items.get(i));
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return this.isLinked() && SpatialStorageMenu.canStore(stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.isLinked()) {
            ExtraplanarData.get(((ServerLevel) this.level).getServer()).setDirty();
        }
    }

    /** Quebrar nunca derruba o conteúdo: ele é do canal, não do bloco. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new SpatialStorageMenu(containerId, inventory, this, -1);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.code = CodedBlockEntity.clean(input.getStringOr("Code", ""));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Code", this.code);
    }

    // o código acompanha o item ao quebrar (loot table: copy_components mistech:extraplanar_code)
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyingComponents = true;
        try {
            super.applyImplicitComponents(components);
        } finally {
            this.applyingComponents = false;
        }
        String stored = components.get(ExtraplanarContent.CODE.get());
        if (stored != null) {
            this.code = CodedBlockEntity.clean(stored);
            this.invalidateCapabilities();
            if (this.level != null && !this.level.isClientSide()) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
            }
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        // o conteúdo é do canal: nunca vai copiado para o item (nem no "pegar bloco")
        components.set(DataComponents.CONTAINER, null);
        if (!this.code.isEmpty()) {
            components.set(ExtraplanarContent.CODE.get(), this.code);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Code");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
