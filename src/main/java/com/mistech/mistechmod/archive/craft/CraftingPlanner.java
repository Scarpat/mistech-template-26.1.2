package com.mistech.mistechmod.archive.craft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.archive.ArchiveCoreBlockEntity;
import com.mistech.mistechmod.archive.ArchiveNet;
import com.mistech.mistechmod.archive.ArchiveRegistry;
import com.mistech.mistechmod.golem.GolemCrafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Monta a árvore de receitas de um pedido (em profundidade): usa o que já está no Arquivo, faz o que tem padrão e lista
 * o que falta. Os trabalhos saem na ordem certa (primeiro as etapas de baixo). Detecta ciclo e para em {@link #MAX_DEPTH}.
 * <p>
 * Vários padrões para o mesmo item (v2): tenta na ordem de prioridade (e das Estantes) e fica com o primeiro que não deixa nada
 * faltando; se todos faltarem algo, fica com o que falta menos. Padrões de processamento (máquina) entram na árvore igual aos de mesa.
 */
public final class CraftingPlanner {
    public static final int MAX_DEPTH = 12;
    public static final int MAX_JOBS = 64;

    public record Plan(List<CraftJob> jobs, Map<ItemResource, Long> missing) {
        public boolean ok() {
            return this.missing.isEmpty() && !this.jobs.isEmpty();
        }
    }

    private final ServerLevel level;
    private Map<ItemResource, Long> available;
    private final Map<ItemResource, List<Pattern>> patterns;
    private List<CraftJob> jobs = new ArrayList<>();
    private Map<ItemResource, Long> missing = new LinkedHashMap<>();
    private final Deque<ItemResource> path = new ArrayDeque<>();

    private CraftingPlanner(ServerLevel level, Map<ItemResource, Long> available, Map<ItemResource, List<Pattern>> patterns) {
        this.level = level;
        this.available = new HashMap<>(available);
        this.patterns = patterns;
    }

    /** Todos os padrões prontos nas Estantes do Arquivo, por saída, do mais prioritário para o menos (empate: ordem das Estantes). */
    public static Map<ItemResource, List<Pattern>> patterns(ServerLevel level, BlockPos core) {
        Map<ItemResource, List<Pattern>> map = new LinkedHashMap<>();
        for (BlockPos pos : ArchiveRegistry.shelvesOf(level, core)) {
            if (level.getBlockEntity(pos) instanceof PatternShelfBlockEntity shelf) {
                for (Pattern p : shelf.patterns()) {
                    List<Pattern> list = map.computeIfAbsent(ItemResource.of(p.output()), k -> new ArrayList<>());
                    if (list.stream().noneMatch(q -> Pattern.same(q, p))) {
                        list.add(p);
                    }
                }
            }
        }
        for (List<Pattern> list : map.values()) {
            list.sort(java.util.Comparator.comparingInt(Pattern::priority).reversed()); // estável: mantém a ordem das Estantes
        }
        return map;
    }

    public static Plan plan(ServerLevel level, ArchiveCoreBlockEntity core, ItemResource want, long count) {
        ArchiveNet net = ArchiveNet.of(level, core);
        Map<ItemResource, Long> available = new HashMap<>(net.snapshot());
        // o que já está prometido para a fila não conta como disponível para o pedido novo... mas a saída deles conta:
        // simples e seguro: o pedido novo usa só o que está guardado agora.
        CraftingPlanner planner = new CraftingPlanner(level, available, patterns(level, core.getBlockPos()));
        if (!planner.patterns.containsKey(want)) {
            planner.missing.put(want, count);
            return new Plan(List.of(), planner.missing);
        }
        planner.craft(want, count, 0);
        return new Plan(planner.jobs, planner.missing);
    }

    /** Precisa de {@code amount}: tira do disponível e faz o resto. */
    private void need(ItemResource resource, long amount, int depth) {
        long have = this.available.getOrDefault(resource, 0L);
        long use = Math.min(have, amount);
        if (use > 0) {
            this.available.put(resource, have - use);
            amount -= use;
        }
        if (amount <= 0) {
            return;
        }
        if (!this.patterns.containsKey(resource) || depth >= MAX_DEPTH || this.path.contains(resource) || this.jobs.size() >= MAX_JOBS) {
            this.missing.merge(resource, amount, Long::sum);
            return;
        }
        this.craft(resource, amount, depth);
    }

    /** Escolhe entre os padrões do item: o primeiro que não deixa faltar nada, senão o que falta menos. */
    private void craft(ItemResource resource, long amount, int depth) {
        List<Pattern> options = this.patterns.get(resource);
        if (options.size() == 1) {
            this.craftWith(options.get(0), resource, amount, depth);
            return;
        }
        Map<ItemResource, Long> available0 = new HashMap<>(this.available);
        List<CraftJob> jobs0 = new ArrayList<>(this.jobs);
        Map<ItemResource, Long> missing0 = new LinkedHashMap<>(this.missing);
        long missingBefore = total(missing0);
        Pattern best = null;
        long bestMissing = Long.MAX_VALUE;
        for (Pattern option : options) {
            this.available = new HashMap<>(available0);
            this.jobs = new ArrayList<>(jobs0);
            this.missing = new LinkedHashMap<>(missing0);
            this.craftWith(option, resource, amount, depth);
            long added = total(this.missing) - missingBefore;
            if (added <= 0) {
                return; // este serve: fica com o estado dele
            }
            if (added < bestMissing) {
                bestMissing = added;
                best = option;
            }
        }
        this.available = new HashMap<>(available0);
        this.jobs = new ArrayList<>(jobs0);
        this.missing = new LinkedHashMap<>(missing0);
        this.craftWith(best != null ? best : options.get(0), resource, amount, depth);
    }

    private static long total(Map<ItemResource, Long> map) {
        long n = 0;
        for (long v : map.values()) {
            n += v;
        }
        return n;
    }

    private void craftWith(Pattern pattern, ItemResource resource, long amount, int depth) {
        int per = Math.max(1, pattern.output().getCount());
        int times = (int) Math.min(Integer.MAX_VALUE, (amount + per - 1) / per);
        this.path.push(resource);
        for (Map.Entry<ItemResource, Integer> in : this.inputs(pattern).entrySet()) {
            this.need(in.getKey(), (long) in.getValue() * times, depth + 1);
        }
        this.path.pop();
        this.jobs.add(CraftJob.of(pattern, times));
        long surplus = (long) times * per - amount;
        if (surplus > 0) {
            this.available.merge(resource, surplus, Long::sum);
        }
    }

    /** Entradas de uma vez do padrão (na receita de mesa, escolhe o item de cada ingrediente). */
    private Map<ItemResource, Integer> inputs(Pattern pattern) {
        Map<ItemResource, Integer> map = new LinkedHashMap<>();
        if (!pattern.crafting()) {
            map.put(ItemResource.of(pattern.input()), pattern.input().getCount());
            return map;
        }
        CraftingRecipe recipe = GolemCrafting.get(this.level, pattern.recipe().orElse(null));
        List<Optional<Ingredient>> slots = recipe == null ? null : GolemCrafting.slots(recipe);
        if (slots == null) {
            return map;
        }
        for (Optional<Ingredient> slot : slots) {
            if (slot.isPresent()) {
                ItemResource pick = this.choose(slot.get());
                if (pick != null) {
                    map.merge(pick, 1, Integer::sum);
                }
            }
        }
        return map;
    }

    /** Item para um ingrediente: um que já está guardado, senão um que tem padrão, senão o primeiro. */
    private @Nullable ItemResource choose(Ingredient ingredient) {
        ItemResource first = null, craftable = null;
        for (Holder<Item> holder : ingredient.items().toList()) {
            ItemResource r = ItemResource.of(holder);
            if (this.available.getOrDefault(r, 0L) > 0) {
                return r;
            }
            if (craftable == null && this.patterns.containsKey(r)) {
                craftable = r;
            }
            if (first == null) {
                first = r;
            }
        }
        // também vale um item guardado com componentes (ex.: encantado) que o ingrediente aceite
        for (Map.Entry<ItemResource, Long> e : this.available.entrySet()) {
            if (e.getValue() > 0 && ingredient.test(e.getKey().toStack())) {
                return e.getKey();
            }
        }
        return craftable != null ? craftable : first;
    }
}
