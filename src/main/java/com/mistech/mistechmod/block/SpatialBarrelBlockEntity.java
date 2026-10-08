package com.mistech.mistechmod.block;

import java.util.List;

import com.mistech.mistechmod.menu.SpatialStorageMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Barril Espacial: 162 espaços. Ao quebrar, os itens ficam dentro do item (componente minecraft:container),
 * igual à shulker box.
 */
public class SpatialBarrelBlockEntity extends BaseContainerBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.mistech.spatial_barrel");

    private NonNullList<ItemStack> items = NonNullList.withSize(SpatialStorageMenu.SIZE, ItemStack.EMPTY);

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            SpatialBarrelBlockEntity.this.playSound(state, SoundEvents.BARREL_OPEN);
            SpatialBarrelBlockEntity.this.updateBlockState(state, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            SpatialBarrelBlockEntity.this.playSound(state, SoundEvents.BARREL_CLOSE);
            SpatialBarrelBlockEntity.this.updateBlockState(state, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof SpatialStorageMenu menu && menu.getContainer() == SpatialBarrelBlockEntity.this;
        }
    };

    public SpatialBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.SPATIAL_BARREL.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    public int getContainerSize() {
        return SpatialStorageMenu.SIZE;
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
        return SpatialStorageMenu.canStore(stack);
    }

    /** Quebrar não derruba o conteúdo: ele vai junto no item (loot table com copy_components). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
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
        return new SpatialStorageMenu(containerId, inventory, this, -1);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator()) {
            this.openersCounter.incrementOpeners(user.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState(),
                    user.getContainerInteractionRange());
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator()) {
            this.openersCounter.decrementOpeners(user.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return this.openersCounter.getEntitiesWithContainerOpen(this.getLevel(), this.getBlockPos());
    }

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    /** Tira todos os itens (usado ao montar o Confector de Runas). */
    public NonNullList<ItemStack> takeAllItems() {
        NonNullList<ItemStack> taken = this.items;
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        this.setChanged();
        return taken;
    }

    private void updateBlockState(BlockState state, boolean open) {
        if (this.level != null && state.hasProperty(SpatialBarrelBlock.OPEN)) {
            this.level.setBlock(this.getBlockPos(), state.setValue(SpatialBarrelBlock.OPEN, open), 3);
        }
    }

    private void playSound(BlockState state, SoundEvent event) {
        if (this.level == null || !state.hasProperty(SpatialBarrelBlock.FACING)) {
            return;
        }
        Vec3i direction = state.getValue(SpatialBarrelBlock.FACING).getUnitVec3i();
        double x = this.worldPosition.getX() + 0.5 + direction.getX() / 2.0;
        double y = this.worldPosition.getY() + 0.5 + direction.getY() / 2.0;
        double z = this.worldPosition.getZ() + 0.5 + direction.getZ() / 2.0;
        this.level.playSound(null, x, y, z, event, SoundSource.BLOCKS, 0.5F, this.level.getRandom().nextFloat() * 0.1F + 0.7F);
    }
}
