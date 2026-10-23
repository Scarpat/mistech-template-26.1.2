package com.mistech.mistechmod.item.scroll;

import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Os espaços do Porta-Pergaminhos: lê e grava direto no item. Só aceita runas. */
public class ScrollCaseContainer extends SimpleContainer {
    private final ItemStack scrollCase;
    private final Player owner;

    public ScrollCaseContainer(ItemStack scrollCase, Player owner) {
        super(ScrollCaseItem.MAX_SLOTS);
        this.scrollCase = scrollCase;
        this.owner = owner;
        NonNullList<ItemStack> list = ScrollCaseItem.contents(scrollCase);
        for (int i = 0; i < list.size(); i++) {
            this.getItems().set(i, list.get(i));
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        NonNullList<ItemStack> list = NonNullList.withSize(ScrollCaseItem.MAX_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            list.set(i, this.getItem(i));
        }
        ScrollCaseItem.setContents(this.scrollCase, list);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < ScrollCaseItem.slots() && ScrollCaseItem.isRune(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player != this.owner || !player.isAlive() || this.scrollCase.isEmpty()) {
            return false;
        }
        if (player.getOffhandItem() == this.scrollCase) {
            return true;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack == this.scrollCase) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void startOpen(ContainerUser user) {
        LivingEntity entity = user.getLivingEntity();
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        this.setChanged();
        LivingEntity entity = user.getLivingEntity();
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
    }
}
