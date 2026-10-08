package com.mistech.mistechmod.item;

import com.mistech.mistechmod.menu.SpatialStorageMenu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Inventário da mochila: lê e grava direto no componente minecraft:container do item. */
public class BackpackContainer extends SimpleContainer {
    private final ItemStack backpack;
    private final Player owner;

    public BackpackContainer(ItemStack backpack, Player owner) {
        super(SpatialStorageMenu.SIZE);
        this.backpack = backpack;
        this.owner = owner;
        backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
    }

    @Override
    public boolean canPlaceItem(int slot, net.minecraft.world.item.ItemStack stack) {
        return SpatialStorageMenu.canStore(stack);
    }

    /** Continua valendo enquanto a mochila estiver com o jogador (costas ou inventário). */
    @Override
    public boolean stillValid(Player player) {
        if (player != this.owner || !player.isAlive() || this.backpack.isEmpty()) {
            return false;
        }
        if (player.getItemBySlot(EquipmentSlot.CHEST) == this.backpack || player.getOffhandItem() == this.backpack) {
            return true;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack == this.backpack) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void startOpen(ContainerUser user) {
        LivingEntity entity = user.getLivingEntity();
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BUNDLE_DROP_CONTENTS,
                SoundSource.PLAYERS, 0.6F, 0.8F);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        this.setChanged();
        LivingEntity entity = user.getLivingEntity();
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BUNDLE_INSERT,
                SoundSource.PLAYERS, 0.6F, 0.8F);
    }
}
