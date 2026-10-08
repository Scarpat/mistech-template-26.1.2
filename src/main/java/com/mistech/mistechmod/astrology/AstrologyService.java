package com.mistech.mistechmod.astrology;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Regras (lado do servidor) para desbloquear estrelas. */
public final class AstrologyService {
    /** Distância máxima (ao quadrado) do jogador até a mesa. */
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private AstrologyService() {}

    public static int countItem(Player player, Item item) {
        Inventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** O jogador tem XP e itens suficientes? (criativo sempre pode) */
    public static boolean canAfford(Player player, Constellations.Constellation constellation, int star) {
        return canAfford(player, constellation, star, null);
    }

    /** Pontos de experiência de {@code levels} níveis contados do zero (fórmula vanilla). */
    public static int pointsForLevels(int levels) {
        int total = 0;
        for (int i = 0; i < levels; i++) {
            total += i >= 30 ? 112 + (i - 30) * 9 : i >= 15 ? 37 + (i - 15) * 5 : 7 + i * 2;
        }
        return total;
    }

    /** Coletores de Ecos perto da mesa pagam o XP desta estrela (só quando o jogador não tem os níveis)? */
    public static boolean collectorPays(Player player, int xpLevels, @org.jspecify.annotations.Nullable BlockPos tablePos) {
        return tablePos != null && player.experienceLevel < xpLevels
                && com.mistech.mistechmod.beyond.EchoCollectorBlockEntity.storedNear(player.level(), tablePos) >= pointsForLevels(xpLevels);
    }

    /** Igual ao de cima, mas com a mesa: Coletores de Ecos a até 8 blocos dela podem pagar o XP. */
    public static boolean canAfford(Player player, Constellations.Constellation constellation, int star, @org.jspecify.annotations.Nullable BlockPos tablePos) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        int xp = NatalService.xpCost(player, Constellations.ALL.indexOf(constellation), star);
        if (!hasStarFragment(player) && player.experienceLevel < xp && !collectorPays(player, xp, tablePos)) {
            return false;
        }
        for (Constellations.Cost cost : constellation.itemCost(star)) {
            if (countItem(player, cost.item().get()) < cost.count()) {
                return false;
            }
        }
        return true;
    }

    /** Um Fragmento Estelar no inventário paga o XP da próxima estrela. */
    public static boolean hasStarFragment(Player player) {
        return countItem(player, com.mistech.mistechmod.celestial.CelestialContent.STAR_FRAGMENT.get()) > 0;
    }

    public static void removeItem(Player player, Item item, int amount) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && amount > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                int taken = Math.min(amount, stack.getCount());
                stack.shrink(taken);
                amount -= taken;
            }
        }
        inventory.setChanged();
    }

    public static void tryUnlock(ServerPlayer player, BlockPos tablePos, int constellationIndex, int star) {
        if (!Constellations.isValid(constellationIndex, star)) {
            return;
        }
        ServerLevel level = player.level();
        if (player.distanceToSqr(tablePos.getCenter()) > MAX_DISTANCE_SQR
                || !level.getBlockState(tablePos).is(Mistech.ASTROLOGY_TABLE.get())) {
            return;
        }
        if (!AstrologyField.isValid(level, tablePos)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.astrology_field_broken"));
            return;
        }
        AstralPillars.refresh(player, tablePos);
        AstrologyData data = player.getData(MistechAttachments.ASTROLOGY);
        if (!data.isAvailable(constellationIndex, star)) {
            return;
        }
        Constellations.Constellation constellation = Constellations.get(constellationIndex);
        if (!canAfford(player, constellation, star, tablePos)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.astrology_cannot_afford"));
            return;
        }

        if (!player.hasInfiniteMaterials()) {
            if (hasStarFragment(player)) {
                // Fragmento Estelar (Chuva de Estrelas) paga o XP da estrela
                removeItem(player, com.mistech.mistechmod.celestial.CelestialContent.STAR_FRAGMENT.get(), 1);
            } else {
                int xp = NatalService.xpCost(player, constellationIndex, star);
                if (player.experienceLevel >= xp) {
                    player.giveExperienceLevels(-xp);
                } else {
                    // Coletores de Ecos perto da mesa pagam
                    com.mistech.mistechmod.beyond.EchoCollectorBlockEntity.takeNear(level, tablePos, pointsForLevels(xp));
                    level.sendParticles(ParticleTypes.SCULK_SOUL, tablePos.getX() + 0.5, tablePos.getY() + 1.2, tablePos.getZ() + 0.5,
                            20, 0.4, 0.3, 0.4, 0.03);
                }
            }
            List<Constellations.Cost> costs = constellation.itemCost(star);
            for (Constellations.Cost cost : costs) {
                removeItem(player, cost.item().get(), cost.count());
            }
        }

        player.setData(MistechAttachments.ASTROLOGY, data.with(constellationIndex, star));
        AstrologyPerks.apply(player);

        level.playSound(null, tablePos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, 0.6F + star * 0.12F);
        level.playSound(null, tablePos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.4F);
        level.sendParticles(ParticleTypes.END_ROD, tablePos.getX() + 0.5, tablePos.getY() + 1.3, tablePos.getZ() + 0.5,
                25, 0.3, 0.6, 0.3, 0.05);
        player.sendOverlayMessage(Component.translatable("message.mistech.astrology_unlocked", constellation.starName(star)));
    }
}
