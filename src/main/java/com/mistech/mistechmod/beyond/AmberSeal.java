package com.mistech.mistechmod.beyond;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Selamento em Resina (Mesa de Escrita: qualquer runa + Grumo de Resina, 10 de giz). A runa fica "em âmbar": não gasta usos
 * (componente {@code minecraft:unbreakable}), mas depois de cada uso fica em recarga — {@link #COOLDOWN_BASIC} nas runas básicas e
 * {@link #COOLDOWN_STRONG} nas avançadas/complexas. O uso é detectado pelo ponto de afinidade que toda runa dá (Affinity.grant).
 * Em máquinas e no fumo a runa em âmbar gasta normalmente.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class AmberSeal {
    public static final int COOLDOWN_BASIC = 30 * 20;
    public static final int COOLDOWN_STRONG = 60 * 20;
    public static final int CHALK_COST = 10;

    private record Pending(UUID player, Item item, int ticks) {}

    private static final List<Pending> PENDING = new ArrayList<>();

    private AmberSeal() {}

    public static boolean isAmber(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(PaleNetherContent.AMBER_SEALED.get()));
    }

    /** Pode ser selada em resina? (runa, ainda não selada de nenhum jeito) */
    public static boolean canSeal(ItemStack stack) {
        return RuneElement.ofRune(stack) != null && !isAmber(stack) && !stack.has(DataComponents.UNBREAKABLE)
                && !com.mistech.mistechmod.purity.TaintEvents.isSealed(stack);
    }

    /** Resultado da Mesa de Escrita para runa + resina (vazio se a combinação não for essa). */
    public static ItemStack sealResult(List<ItemStack> inputs) {
        ItemStack rune = ItemStack.EMPTY;
        int resin = 0;
        int count = 0;
        for (ItemStack stack : inputs) {
            if (stack.isEmpty()) {
                continue;
            }
            count++;
            if (stack.is(Items.RESIN_CLUMP)) {
                resin++;
            } else if (canSeal(stack)) {
                rune = stack;
            }
        }
        if (count != 2 || resin != 1 || rune.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = rune.copyWithCount(1);
        result.set(PaleNetherContent.AMBER_SEALED.get(), true);
        result.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        return result;
    }

    /** Uma runa deu afinidade: se for uma runa em âmbar na mão, agenda a recarga (no fim do tick, depois da recarga da própria runa). */
    public static void onRuneUse(Player player, RuneElement element) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (isAmber(held) && RuneElement.ofRune(held) == element) {
                int ticks = element.isAdvanced() ? COOLDOWN_STRONG : COOLDOWN_BASIC;
                PENDING.add(new Pending(player.getUUID(), held.getItem(), ticks));
                return;
            }
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        for (Pending pending : PENDING) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(pending.player());
            if (player != null) {
                player.getCooldowns().addCooldown(new ItemStack(pending.item()), pending.ticks());
            }
        }
        PENDING.clear();
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        if (isAmber(event.getItemStack())) {
            event.getToolTip().add(1, Component.translatable("tooltip.mistech.amber_sealed").withStyle(ChatFormatting.GOLD));
        }
    }
}
