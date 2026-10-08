package com.mistech.mistechmod.registry;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MistechEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Mistech.MODID);

    /**
     * Véu de Incenso: mortos-vivos listados na tag mistech:incense_ignores
     * (zumbis e esqueletos) não escolhem o jogador como alvo.
     * A lógica fica em {@link com.mistech.mistechmod.event.IncenseEvents}.
     */
    public static final DeferredHolder<MobEffect, MobEffect> INCENSE_VEIL =
            MOB_EFFECTS.register("incense_veil", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xB58CD9));

    /**
     * Véu Feérico (Incenso de Fada): duendes, trolls e aberrações (tag mistech:fey_ignores) não escolhem o jogador como alvo.
     * A Rainha Duende não é afetada. Lógica em {@link com.mistech.mistechmod.fey.FeyEvents}.
     */
    public static final DeferredHolder<MobEffect, MobEffect> FEY_VEIL =
            MOB_EFFECTS.register("fey_veil", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xF2A8E8));

    /** Foco do Tabaco: reduz o campo de visão (FOV), aplicado no cliente em MistechClient. */
    public static final DeferredHolder<MobEffect, MobEffect> TOBACCO_FOCUS =
            MOB_EFFECTS.register("tobacco_focus", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x8B5A2B));

    // ----- Charutos e baseados mágicos -----

    /** Toque Flamejante: seus golpes colocam fogo no alvo. */
    public static final DeferredHolder<MobEffect, MobEffect> FLAME_TOUCH =
            MOB_EFFECTS.register("flame_touch", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFF7A2A));
    /** Guarda Gélida: imune a congelamento; quem te bate de perto fica lento. */
    public static final DeferredHolder<MobEffect, MobEffect> FROST_WARD =
            MOB_EFFECTS.register("frost_ward", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x9FE3FF));
    /** Toque Gélido: seus golpes deixam o alvo lento e congelando. */
    public static final DeferredHolder<MobEffect, MobEffect> FROST_TOUCH =
            MOB_EFFECTS.register("frost_touch", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x6FC8F2));
    /** Toque Sombrio: seus golpes cegam o alvo. */
    public static final DeferredHolder<MobEffect, MobEffect> SHADOW_TOUCH =
            MOB_EFFECTS.register("shadow_touch", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x3A2450));
    /** Desvio Espacial: 25% de chance de escapar de um golpe se teleportando. */
    public static final DeferredHolder<MobEffect, MobEffect> SPACE_DODGE =
            MOB_EFFECTS.register("space_dodge", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x8A4FD8));
    /** Alcance Estendido: +3 blocos de alcance para blocos e criaturas. */
    public static final DeferredHolder<MobEffect, MobEffect> EXTENDED_REACH =
            MOB_EFFECTS.register("extended_reach", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xC77DFF)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(Mistech.MODID, "effect.extended_reach_block"), 3.0,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(Mistech.MODID, "effect.extended_reach_entity"), 3.0,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));

    /** Visão Mística (poção de Beladona): campo de visão maior, cores estranhas e a câmera balançando (no cliente). */
    public static final DeferredHolder<MobEffect, MobEffect> MYSTIC_SIGHT =
            MOB_EFFECTS.register("mystic_sight", () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 0x7A3FC8));

    /** Visão Psicodélica (Chá de Cogumelo): como a Visão Mística, só que muito mais forte (cores girando, tela ondulando). */
    public static final DeferredHolder<MobEffect, MobEffect> PSYCHEDELIC_SIGHT =
            MOB_EFFECTS.register("psychedelic_sight", () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 0xE8B83A));

    /**
     * Forma de Névoa (Cachimbo da Névoa): o jogador vira névoa — encolhe até caber em vãos de 0,5 × 0,5 bloco,
     * voa, fica invisível e intocável, mas ainda bate nos blocos e não interage com nada.
     * A lógica fica em {@link com.mistech.mistechmod.event.MistFormEvents}.
     */
    public static final DeferredHolder<MobEffect, MobEffect> MIST_FORM =
            MOB_EFFECTS.register("mist_form", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xD8E2EA)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.SCALE,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(Mistech.MODID, "effect.mist_form_scale"), -0.73,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private MistechEffects() {}

    /** Fortuna (Baseado da Sorte): 25% de chance de dobrar os drops de blocos quebrados e de criaturas mortas. */
    public static final DeferredHolder<MobEffect, MobEffect> FORTUNE =
            MOB_EFFECTS.register("fortune", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x4FC95B));

    /** Égide Arcana (Charuto de Energia Mágica): -50% de dano mágico. */
    public static final DeferredHolder<MobEffect, MobEffect> ARCANE_WARD =
            MOB_EFFECTS.register("arcane_ward", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x8E6BFF));

    /** Surto Arcano (Baseado de Energia Mágica): magias do cajado gastam metade e o equipamento de mitril não gasta energia. */
    public static final DeferredHolder<MobEffect, MobEffect> ARCANE_SURGE =
            MOB_EFFECTS.register("arcane_surge", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xD07CFF));

    /** Passo Silencioso (Charuto do Eco / Runa do Eco engastada): o jogador não gera vibrações (sculk e Warden não percebem). */
    public static final DeferredHolder<MobEffect, MobEffect> SILENT_STEP =
            MOB_EFFECTS.register("silent_step", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x0F5E5E));

    /** Ecolocalização (Baseado do Eco): criaturas que fazem barulho por perto ganham contorno só para quem tem o efeito. */
    public static final DeferredHolder<MobEffect, MobEffect> ECHOLOCATION =
            MOB_EFFECTS.register("echolocation", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x29DFEB));

    // ----- Runa da Vigília (Jardim Pálido) -----
    /** Vigília (Runa da Vigília): criaturas que você olha ficam travadas. */
    public static final DeferredHolder<MobEffect, MobEffect> VIGIL_GAZE =
            MOB_EFFECTS.register("vigil_gaze", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xE8E2D6));
    /** Olhar Pesado (Baseado da Vigília): criaturas que você olha ficam lentas e fracas. */
    public static final DeferredHolder<MobEffect, MobEffect> HEAVY_GAZE =
            MOB_EFFECTS.register("heavy_gaze", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xB9B4AE));
    /** Guarda Vigilante (Charuto da Vigília): quem mira em você fica lento. */
    public static final DeferredHolder<MobEffect, MobEffect> VIGIL_WARD =
            MOB_EFFECTS.register("vigil_ward", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xE07A1F));
    /** Vigiado: travado no lugar, sem conseguir atacar (pela Vigília). */
    public static final DeferredHolder<MobEffect, MobEffect> WATCHED =
            MOB_EFFECTS.register("watched", () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x6F6A66)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(Mistech.MODID, "effect.watched"), -1.0,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(Mistech.MODID, "effect.watched_fly"), -1.0,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    // ----- Runa das Almas (Nether) -----
    /** Véu das Almas: os mobs do Nether não te escolhem como alvo. */
    public static final DeferredHolder<MobEffect, MobEffect> SOUL_VEIL =
            MOB_EFFECTS.register("soul_veil", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x4FD7E6));
    /** Toque das Almas: seus golpes dão Murchar. */
    public static final DeferredHolder<MobEffect, MobEffect> SOUL_TOUCH =
            MOB_EFFECTS.register("soul_touch", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x2FA8C0));

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }

    /** MobEffect tem construtor protegido, então usamos uma subclasse simples. */
    private static final class SimpleEffect extends MobEffect {
        SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
