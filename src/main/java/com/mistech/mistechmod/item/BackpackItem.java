package com.mistech.mistechmod.item;

import com.mistech.mistechmod.menu.SpatialStorageMenu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Barril Carregável (mochila): Barril Espacial com alças de cânhamo. Guarda 162 espaços no próprio item.
 * Vai no lugar do peitoral (fica nas costas). Abre com a tecla B (vestida ou em qualquer lugar do inventário),
 * ou botão direito com ela na mão. Agachado + botão direito veste/tira.
 */
public class BackpackItem extends Item {
    public BackpackItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return super.use(level, player, hand); // veste/troca com o peitoral
        }
        if (player instanceof ServerPlayer serverPlayer) {
            int locked = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : -1;
            open(serverPlayer, player.getItemInHand(hand), locked);
        }
        return InteractionResult.SUCCESS;
    }

    public static void open(ServerPlayer player, ItemStack backpack, int lockedInventorySlot) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new SpatialStorageMenu(containerId, inventory,
                        new BackpackContainer(backpack, p), lockedInventorySlot),
                backpack.getHoverName()));
    }

    /** Tecla B: abre a mochila das costas, senão a primeira que achar (mãos e inventário). */
    public static boolean openFirst(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof BackpackItem) {
            open(player, chest, -1);
            return true;
        }
        if (player.getOffhandItem().getItem() instanceof BackpackItem) {
            open(player, player.getOffhandItem(), -1);
            return true;
        }
        Inventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        if (inventory.getItem(selected).getItem() instanceof BackpackItem) {
            open(player, inventory.getItem(selected), selected);
            return true;
        }
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            if (inventory.getItem(i).getItem() instanceof BackpackItem) {
                open(player, inventory.getItem(i), i);
                return true;
            }
        }
        return false;
    }
}
