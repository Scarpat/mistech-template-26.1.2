package com.mistech.mistechmod.fey;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.accessory.Accessory;
import com.mistech.mistechmod.accessory.AccessoryItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Coisas da Vila dos Duendes que viraram equipamento: Incenso de Fada (uso do Pó de Fada) e a Coroa Purificada
 * (a Coroa Corrompida fica registrada em {@link com.mistech.mistechmod.registry.GoblinContent}).
 */
public final class FeyContent {
    /** Incenso de Fada: incenso comum + Véu Feérico (duendes ignoram). */
    public static final DeferredItem<FairyIncenseItem> FAIRY_INCENSE = Mistech.ITEMS.registerItem("fairy_incense", FairyIncenseItem::new,
            p -> p.rarity(Rarity.UNCOMMON));

    /** Coroa Purificada (Santuário): como a Corrompida, sem Mácula e com recarga menor; os duendes chamados são "limpos". */
    public static final DeferredItem<AccessoryItem> PURIFIED_CROWN = Mistech.ITEMS.registerItem("purified_crown",
            p -> new AccessoryItem(p, Accessory.PURIFIED_CROWN), p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    /** Game time (do Mundo Superior) a partir do qual o Chamado da Rainha pode ser usado de novo. Mantido ao morrer. */
    public static final java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<Long>> CROWN_CALL_READY =
            com.mistech.mistechmod.registry.MistechAttachments.ATTACHMENT_TYPES.register("crown_call_ready",
                    () -> net.neoforged.neoforge.attachment.AttachmentType.builder(() -> 0L)
                            .serialize(com.mojang.serialization.Codec.LONG.fieldOf("ready")).copyOnDeath().build());

    private FeyContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(FAIRY_INCENSE, PURIFIED_CROWN);
    }

    public static void register(IEventBus modEventBus) {
        // tudo nos registros do Mistech (itens) e do MistechEffects (Véu Feérico)
    }
}
