package com.mistech.mistechmod.registry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.astrology.AstrologyData;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Dados guardados nos jogadores. */
public final class MistechAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Mistech.MODID);

    /** Estrelas desbloqueadas na Mesa de Astrologia (por jogador; mantém ao morrer; sincroniza só com o dono). */
    public static final Supplier<AttachmentType<AstrologyData>> ASTROLOGY = ATTACHMENT_TYPES.register("astrology",
            () -> AttachmentType.builder(() -> AstrologyData.EMPTY)
                    .serialize(AstrologyData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, AstrologyData.STREAM_CODEC)
                    .build());

    /** Já ganhou o Grimório Mistech ao entrar no mundo pela primeira vez? */
    public static final Supplier<AttachmentType<Boolean>> RECEIVED_GUIDE = ATTACHMENT_TYPES.register("received_guide",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL.fieldOf("received"))
                    .copyOnDeath()
                    .build());

    /**
     * Conexão Astral: quantos Pilares de Conexão Astral (0 a 4) havia nos pontos cardeais da última Mesa de Astrologia
     * que o jogador usou. Cada um aumenta os bônus da astrologia em 12,5%.
     */
    public static final Supplier<AttachmentType<Integer>> ASTRAL_LINK = ATTACHMENT_TYPES.register("astral_link",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.fieldOf("pillars"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
                    .build());

    /** Acessórios equipados: [amuleto, anel, anel]. Mantidos ao morrer. */
    public static final Supplier<AttachmentType<java.util.List<net.minecraft.world.item.ItemStack>>> ACCESSORIES = ATTACHMENT_TYPES.register("accessories",
            () -> AttachmentType.<java.util.List<net.minecraft.world.item.ItemStack>>builder(() -> java.util.List.of(
                            net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.item.ItemStack.EMPTY))
                    .serialize(net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items"))
                    .copyOnDeath()
                    .build());

    /**
     * Mapa astral (Sol, Lua, Ascendente), sorteado escondido na primeira entrada. Mantido ao morrer.
     * Sincroniza só com o dono e só o que já foi revelado ({@link com.mistech.mistechmod.astrology.NatalChart#SYNC_CODEC}).
     */
    public static final Supplier<AttachmentType<com.mistech.mistechmod.astrology.NatalChart>> NATAL_CHART = ATTACHMENT_TYPES.register("natal_chart",
            () -> AttachmentType.builder(() -> com.mistech.mistechmod.astrology.NatalChart.UNROLLED)
                    .serialize(com.mistech.mistechmod.astrology.NatalChart.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, com.mistech.mistechmod.astrology.NatalChart.SYNC_CODEC)
                    .build());

    /**
     * Em Forma de Névoa? Espelho do efeito mistech:mist_form, sincronizado com todos que veem o jogador
     * (o cliente usa para não desenhar o corpo). Não é salvo: o tick do servidor refaz a partir do efeito.
     */
    public static final Supplier<AttachmentType<Boolean>> MIST_FORM = ATTACHMENT_TYPES.register("mist_form",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .sync((holder, to) -> true, net.minecraft.network.codec.ByteBufCodecs.BOOL)
                    .build());

    /** O voo foi ligado pela Forma de Névoa (para desligar só o que a névoa ligou). */
    public static final Supplier<AttachmentType<Boolean>> MIST_FLIGHT = ATTACHMENT_TYPES.register("mist_flight",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL.fieldOf("granted"))
                    .build());

    private MistechAttachments() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
