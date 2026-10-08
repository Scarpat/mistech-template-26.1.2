package com.mistech.mistechmod.tarot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.astrology.AstrologyService;
import com.mistech.mistechmod.block.ritual.RitualPedestalBlockEntity;
import com.mistech.mistechmod.network.OpenTarotPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** Regras (lado do servidor) da Mesa de Tarologia: abrir, invocar Cartas Invertidas e forjar equipamentos. */
public final class TarotService {
    public static final double MAX_DISTANCE_SQR = 8.0 * 8.0;
    public static final int ACTION_INVOKE = 0;
    public static final int ACTION_FORGE = 1;

    private TarotService() {}

    public static void open(ServerPlayer player, BlockPos tablePos) {
        if (!TarotField.isValid(player.level(), tablePos)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.tarot.field_broken"));
            return;
        }
        PacketDistributor.sendToPlayer(player, new OpenTarotPayload(tablePos));
    }

    // ----- conferência dos pedestais (usada também pela tela) -----

    /** Resultado da conferência: quais itens pedidos estão nos pedestais e se sobrou algo. */
    public record Match(boolean[] found, int extras, int pedestals) {
        public boolean complete() {
            for (boolean f : this.found) {
                if (!f) {
                    return false;
                }
            }
            return this.extras == 0 && this.pedestals >= this.found.length;
        }
    }

    /** Confere os pedestais contra a lista pedida (1 de cada, sem sobras). Funciona no cliente. */
    public static Match match(Level level, BlockPos tablePos, List<Supplier<Item>> required) {
        List<ItemStack> present = new ArrayList<>(TarotField.pedestalItems(level, tablePos));
        boolean[] found = new boolean[required.size()];
        for (int i = 0; i < required.size(); i++) {
            Item item = required.get(i).get();
            for (int j = 0; j < present.size(); j++) {
                if (present.get(j).is(item)) {
                    found[i] = true;
                    present.remove(j);
                    break;
                }
            }
        }
        return new Match(found, present.size(), TarotField.pedestals(level, tablePos).size());
    }

    private static boolean checkTable(ServerPlayer player, BlockPos tablePos) {
        ServerLevel level = player.level();
        if (player.distanceToSqr(tablePos.getCenter()) > MAX_DISTANCE_SQR || !level.getBlockState(tablePos).is(TarotContent.TAROT_TABLE.get())) {
            return false;
        }
        if (!TarotField.isValid(level, tablePos)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.tarot.field_broken"));
            return false;
        }
        for (RitualPedestalBlockEntity pedestal : TarotField.pedestals(level, tablePos)) {
            if (pedestal.locked()) {
                player.sendOverlayMessage(Component.translatable("message.mistech.ritual.already_running"));
                return false;
            }
        }
        return true;
    }

    private static void consumePedestals(ServerLevel level, BlockPos tablePos, int color) {
        for (RitualPedestalBlockEntity pedestal : TarotField.pedestals(level, tablePos)) {
            if (!pedestal.getStored().isEmpty()) {
                pedestal.setStored(ItemStack.EMPTY);
                BlockPos p = pedestal.getBlockPos();
                level.sendParticles(new DustParticleOptions(color, 1.4F), p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.0);
                level.sendParticles(ParticleTypes.WITCH, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.0);
            }
        }
    }

    private static void effects(ServerLevel level, BlockPos tablePos, int color) {
        level.playSound(null, tablePos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, tablePos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.7F);
        level.sendParticles(new DustParticleOptions(color, 2.0F), tablePos.getX() + 0.5, tablePos.getY() + 1.3, tablePos.getZ() + 0.5,
                40, 0.3, 0.5, 0.3, 0.0);
        level.sendParticles(ParticleTypes.ENCHANT, tablePos.getX() + 0.5, tablePos.getY() + 1.0, tablePos.getZ() + 0.5, 80, 2.5, 0.5, 2.5, 0.6);
    }

    public static int countItem(Player player, Item item) {
        return AstrologyService.countItem(player, item);
    }

    // ----- aba Invocação -----

    /** Pode invocar agora? (para a tela; o servidor confere de novo) */
    public static boolean canInvoke(Player player, Level level, BlockPos tablePos, TarotArcana arcana) {
        if (!match(level, tablePos, arcana.invocation()).complete()) {
            return false;
        }
        return player.hasInfiniteMaterials()
                || player.experienceLevel >= arcana.xpCost() && countItem(player, TarotContent.BLANK_CARD.get()) > 0;
    }

    public static void tryInvoke(ServerPlayer player, BlockPos tablePos, TarotArcana arcana) {
        if (!checkTable(player, tablePos)) {
            return;
        }
        ServerLevel level = player.level();
        if (!canInvoke(player, level, tablePos, arcana)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.tarot.cannot_invoke"));
            return;
        }
        if (!player.hasInfiniteMaterials()) {
            player.giveExperienceLevels(-arcana.xpCost());
            AstrologyService.removeItem(player, TarotContent.BLANK_CARD.get(), 1);
        }
        consumePedestals(level, tablePos, arcana.color());
        effects(level, tablePos, arcana.color());
        level.playSound(null, tablePos, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS, 0.8F, 0.8F);
        player.getInventory().placeItemBackInInventory(new ItemStack(TarotContent.invertedCard(arcana)));
        player.sendOverlayMessage(Component.translatable("message.mistech.tarot.invoked", arcana.displayName()));
    }

    // ----- aba Forja -----

    public static boolean canForge(Player player, Level level, BlockPos tablePos, TarotArcana arcana, int optionIndex) {
        TarotArcana.Option option = arcana.option(optionIndex);
        if (option == null || !match(level, tablePos, option.materials()).complete()) {
            return false;
        }
        return player.hasInfiniteMaterials() || countItem(player, TarotContent.card(arcana)) > 0;
    }

    public static void tryForge(ServerPlayer player, BlockPos tablePos, TarotArcana arcana, int optionIndex) {
        if (!checkTable(player, tablePos)) {
            return;
        }
        ServerLevel level = player.level();
        TarotArcana.Option option = arcana.option(optionIndex);
        if (option == null || !canForge(player, level, tablePos, arcana, optionIndex)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.tarot.cannot_forge"));
            return;
        }
        if (!player.hasInfiniteMaterials()) {
            AstrologyService.removeItem(player, TarotContent.card(arcana), 1);
        }
        consumePedestals(level, tablePos, arcana.color());
        effects(level, tablePos, arcana.color());
        level.playSound(null, tablePos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        ItemStack result = option.result().get().copy();
        player.getInventory().placeItemBackInInventory(result);
        player.sendOverlayMessage(Component.translatable("message.mistech.tarot.forged", result.getHoverName()));
    }
}
