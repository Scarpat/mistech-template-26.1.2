package com.mistech.mistechmod.power;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * Item que carrega o inventário do jogador (Baterias de Bolso). O tick do jogador em
 * {@link com.mistech.mistechmod.item.PocketBatteryItem} junta todos e divide o teto por tipo de bateria.
 * Um carregador nunca carrega outro.
 */
public interface PocketCharger {
    /** Ligado e com energia para dar agora (só no servidor). */
    boolean canChargeNow(Player player, ItemStack stack);

    /** FE/t por item. */
    int perItemPerTick();

    /** FE/t somando todos os itens (vale para todas as baterias deste tipo juntas). */
    long totalPerTick();

    /** De onde sai a energia. */
    @Nullable EnergyHandler chargeSource(Player player, int slot, ItemStack stack);
}
