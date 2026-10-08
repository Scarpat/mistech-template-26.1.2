package com.mistech.mistechmod.tarot;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.mithril.MithrilGear;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Vínculo com o Tarô (equipamento de Oricalco Puro): clicar com a peça na Mesa de Tarologia grava o próximo arcano
 * DERROTADO (clicar de novo troca; Shift+clique desfaz) — igual ao vínculo de constelação na Mesa de Astrologia.
 * O efeito de cada arcano vinculado ainda será definido: use {@link #get(ItemStack)} para saber qual é.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class GearArcana {
    private GearArcana() {}

    /** Arcano vinculado à peça, ou null. */
    public static @Nullable TarotArcana get(ItemStack gear) {
        String id = gear.get(TarotContent.GEAR_ARCANA.get());
        return id == null ? null : TarotArcana.byId(id);
    }

    public static boolean canBind(ItemStack stack) {
        return MithrilGear.isMithril(stack) && MithrilGear.tier(stack).bindsTarot();
    }

    public static void tooltip(ItemStack gear, Consumer<Component> builder) {
        if (!canBind(gear)) {
            return;
        }
        TarotArcana arcana = get(gear);
        if (arcana == null) {
            builder.accept(Component.translatable("item.mistech.gear.arcana_none").withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }
        builder.accept(Component.translatable("item.mistech.gear.arcana", arcana.fullName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        builder.accept(Component.literal("  ").append(Component.translatable("tarot.mistech.bond." + arcana.id()))
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @SubscribeEvent
    static void onUseTable(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!canBind(stack) || !event.getLevel().getBlockState(event.getPos()).is(TarotContent.TAROT_TABLE.get())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Player player = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (player.isShiftKeyDown()) {
            if (get(stack) != null) {
                stack.remove(TarotContent.GEAR_ARCANA.get());
                player.sendOverlayMessage(Component.translatable("message.mistech.gear.arcana_cleared"));
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            return;
        }
        TarotData data = player.getData(TarotContent.TAROT);
        TarotArcana current = get(stack);
        int size = TarotArcana.values().length;
        int start = current == null ? -1 : current.ordinal();
        for (int step = 1; step <= size; step++) {
            TarotArcana next = TarotArcana.byIndex(start + step);
            if (data.hasDefeated(next) || player.hasInfiniteMaterials()) {
                if (next == current) {
                    break; // só tem um: já está nele
                }
                stack.set(TarotContent.GEAR_ARCANA.get(), next.id());
                player.sendOverlayMessage(Component.translatable("message.mistech.gear.arcana_bound", next.fullName()));
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.9F);
                level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.6F, 1.3F);
                level.sendParticles(new DustParticleOptions(next.color(), 1.5F), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        30, 0.3, 0.5, 0.3, 0.0);
                return;
            }
        }
        if (current != null) {
            player.sendOverlayMessage(Component.translatable("message.mistech.gear.arcana_bound", current.fullName()));
            return;
        }
        player.sendOverlayMessage(Component.translatable("message.mistech.gear.arcana_no_defeated"));
    }
}
