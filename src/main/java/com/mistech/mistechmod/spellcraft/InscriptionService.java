package com.mistech.mistechmod.spellcraft;

import com.mistech.mistechmod.astrology.AstrologyService;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Regras (servidor e tela) da Mesa de Inscrição. */
public final class InscriptionService {
    public static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private InscriptionService() {}

    public static void open(ServerPlayer player, BlockPos tablePos) {
        PacketDistributor.sendToPlayer(player, new OpenInscriptionPayload(tablePos));
    }

    /** Dá para inscrever agora? (só a lista de materiais e o XP; vale no cliente e no servidor) */
    public static boolean canAfford(Player player, Spell spell) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        if (player.experienceLevel < spell.xpLevels()) {
            return false;
        }
        for (Spell.Cost cost : spell.costs()) {
            if (AstrologyService.countItem(player, cost.item().get()) < cost.count()) {
                return false;
            }
        }
        return true;
    }

    /** A magia pode ser copiada pela mesa? (conhecida e não exclusiva do mundo) */
    public static boolean canInscribe(Player player, Spell spell) {
        return !spell.worldOnly() && SpellBook.of(player).knows(spell.id());
    }

    public static void tryInscribe(ServerPlayer player, BlockPos tablePos, String spellId) {
        Spell spell = Spells.byId(spellId);
        if (spell == null || player.distanceToSqr(tablePos.getCenter()) > MAX_DISTANCE_SQR
                || !player.level().getBlockState(tablePos).is(SpellcraftContent.INSCRIPTION_TABLE.get())) {
            return;
        }
        if (!canInscribe(player, spell) || !canAfford(player, spell)) {
            return;
        }
        if (!player.hasInfiniteMaterials()) {
            player.giveExperienceLevels(-spell.xpLevels());
            for (Spell.Cost cost : spell.costs()) {
                take(player, cost);
            }
        }
        player.getInventory().placeItemBackInInventory(spell.scroll());
        player.sendOverlayMessage(Component.translatable("message.mistech.spell.inscribed",
                Component.translatable(spell.nameKey()).withColor(spell.element().color())));
        player.level().playSound(null, tablePos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.9F);
        player.level().playSound(null, tablePos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
    }

    private static void take(Player player, Spell.Cost cost) {
        Inventory inventory = player.getInventory();
        int left = cost.count();
        for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(cost.item().get())) {
                int taken = Math.min(left, stack.getCount());
                stack.shrink(taken);
                left -= taken;
            }
        }
    }
}
