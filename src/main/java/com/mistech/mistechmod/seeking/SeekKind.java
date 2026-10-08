package com.mistech.mistechmod.seeking;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.GoblinContent;
import com.mistech.mistechmod.registry.OreContent;
import com.mistech.mistechmod.folklore.FolkloreContent;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * O que o Pêndulo Localizador procura, escolhido pelo item na outra mão (agachar + botão direito).
 * Estruturas: tags {@code mistech:seek/<nome>} em {@code data/mistech/tags/worldgen/structure/seek/}.
 */
public enum SeekKind {
    GOBLIN_VILLAGE("goblin_village", 0xB05CFF, s -> s.is(Mistech.IMPURE_MAGIC_SHARD.get()), Type.STRUCTURE),
    ANCIENT_CITY("ancient_city", 0x0F8A8A, s -> s.is(Items.ECHO_SHARD), Type.STRUCTURE),
    NETHER_FORTRESS("nether_fortress", 0xE06030, s -> s.is(Items.BLAZE_ROD), Type.STRUCTURE),
    MITHRIL("mithril", 0x7FD8FF, s -> s.is(OreContent.RAW_MITHRIL.get()) || s.is(OreContent.MITHRIL_INGOT.get()), Type.ORE),
    ADAMANTINE("adamantine", 0xE8576A, s -> s.is(OreContent.RAW_ADAMANTINE.get()) || s.is(OreContent.ADAMANTINE_INGOT.get()), Type.ORE),
    ORICHALCUM("orichalcum", 0xFFB04A, s -> s.is(OreContent.RAW_ORICHALCUM.get()) || s.is(OreContent.ORICHALCUM_INGOT.get()), Type.ORE),
    WEREWOLF("werewolf", 0xC0C0D0, s -> s.is(FolkloreContent.WEREWOLF_FANG.get()), Type.CREATURE);

    public enum Type { STRUCTURE, ORE, CREATURE }

    public final String id;
    public final int color;
    private final Predicate<ItemStack> key;
    public final Type type;

    SeekKind(String id, int color, Predicate<ItemStack> key, Type type) {
        this.id = id;
        this.color = color;
        this.key = key;
        this.type = type;
    }

    public String translationKey() {
        return "seek.mistech." + this.id;
    }

    public TagKey<Structure> structureTag() {
        return TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(Mistech.MODID, "seek/" + this.id));
    }

    public TagKey<Block> oreTag() {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/" + this.id));
    }

    public static @Nullable SeekKind byKey(ItemStack stack) {
        for (SeekKind kind : values()) {
            if (!stack.isEmpty() && kind.key.test(stack)) {
                return kind;
            }
        }
        return null;
    }

    public static @Nullable SeekKind byId(@Nullable String id) {
        for (SeekKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return null;
    }
}
