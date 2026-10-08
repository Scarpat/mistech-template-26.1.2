package com.mistech.mistechmod.registry;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class MistechTags {
    /** Mobs que ignoram o jogador sob o efeito do incenso (por padrão: #minecraft:zombies e #minecraft:skeletons). */
    /** Danos mágicos (resistidos pela armadura de mitril completa e pela Égide Arcana). */
    public static final TagKey<net.minecraft.world.damagesource.DamageType> MAGIC_DAMAGE =
            TagKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Mistech.MODID, "magic"));

    /** Materiais de conserto/tier do mitril. */
    public static final TagKey<net.minecraft.world.item.Item> MITHRIL_TOOL_MATERIALS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mistech.MODID, "mithril_tool_materials"));

    public static final TagKey<EntityType<?>> INCENSE_IGNORES =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Mistech.MODID, "incense_ignores"));

    /** Criaturas da Vila dos Duendes que ignoram quem tem o Véu Feérico (Incenso de Fada). */
    public static final TagKey<EntityType<?>> FEY_IGNORES =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Mistech.MODID, "fey_ignores"));

    /** Gizes aceitos pela Mesa de Escrita (Giz de Cinzas e Giz Impuro). */
    public static final TagKey<net.minecraft.world.item.Item> CHALKS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mistech.MODID, "chalks"));

    /** Combustíveis do Gerador de Cinzas. */
    public static final TagKey<net.minecraft.world.item.Item> ASH_FUELS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mistech.MODID, "ash_fuels"));

    private MistechTags() {}
}
