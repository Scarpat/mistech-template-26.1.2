package com.mistech.mistechmod.compat.jei;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

/**
 * "Receita" de uma máquina/bloco do Mistech que não usa o sistema de receitas do jogo (a lógica fica no código do bloco).
 * Só serve para o JEI mostrar: entradas, saídas, um bloco "estação" opcional (gerador, Pedra Guia...) e notas de texto.
 */
public final class MachineRecipe {
    /** Uma posição: vários itens (alternativas, ciclam) ou um fluido. */
    public record Entry(List<ItemStack> items, @Nullable Fluid fluid, long amount) {
        static Entry of(ItemStack... stacks) {
            return new Entry(List.of(stacks), null, 0);
        }

        static Entry of(List<ItemStack> stacks) {
            return new Entry(List.copyOf(stacks), null, 0);
        }

        static Entry fluid(Fluid fluid, long amount) {
            return new Entry(List.of(), fluid, amount);
        }
    }

    final List<Entry> inputs = new ArrayList<>();
    final List<Entry> outputs = new ArrayList<>();
    final List<Component> notes = new ArrayList<>();
    @Nullable Entry station;
    @Nullable Component label;
    /** Id sintético e estável (tipo/índice), para os favoritos do JEI. */
    net.minecraft.resources.@Nullable Identifier id;

    public static MachineRecipe create() {
        return new MachineRecipe();
    }

    public MachineRecipe in(ItemLike item) {
        return this.in(new ItemStack(item));
    }

    public MachineRecipe in(ItemLike item, int count) {
        return this.in(new ItemStack(item, count));
    }

    public MachineRecipe in(ItemStack... stacks) {
        this.inputs.add(Entry.of(stacks));
        return this;
    }

    public MachineRecipe in(List<ItemStack> stacks) {
        this.inputs.add(Entry.of(stacks));
        return this;
    }

    public MachineRecipe inFluid(Fluid fluid, long amount) {
        this.inputs.add(Entry.fluid(fluid, amount));
        return this;
    }

    public MachineRecipe out(ItemLike item) {
        return this.out(new ItemStack(item));
    }

    public MachineRecipe out(ItemLike item, int count) {
        return this.out(new ItemStack(item, count));
    }

    public MachineRecipe out(ItemStack... stacks) {
        this.outputs.add(Entry.of(stacks));
        return this;
    }

    public MachineRecipe outFluid(Fluid fluid, long amount) {
        this.outputs.add(Entry.fluid(fluid, amount));
        return this;
    }

    /** Bloco que faz o trabalho (mostrado à esquerda, como estação). */
    public MachineRecipe station(ItemLike... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemLike item : items) {
            stacks.add(new ItemStack(item));
        }
        this.station = Entry.of(stacks);
        return this;
    }

    /** Texto no lugar da saída (quando o resultado não é um item, ex.: a Mineradora). */
    public MachineRecipe label(Component label) {
        this.label = label;
        return this;
    }

    public MachineRecipe note(String key, Object... args) {
        this.notes.add(Component.translatable(key, args));
        return this;
    }
}
