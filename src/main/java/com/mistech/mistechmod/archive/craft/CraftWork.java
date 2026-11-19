package com.mistech.mistechmod.archive.craft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.archive.ArchiveCoreBlockEntity;
import com.mistech.mistechmod.archive.ArchiveNet;
import com.mistech.mistechmod.archive.ArchiveRegistry;
import com.mistech.mistechmod.golem.GolemCrafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** O trabalho dos golens na Estante de Padrões e os pedidos ao Arquivo (Fase 8). */
public final class CraftWork {
    private CraftWork() {}

    /** O Núcleo ligado e com energia que alcança a Estante. */
    public static @Nullable ArchiveCoreBlockEntity core(ServerLevel level, BlockPos shelf) {
        ArchiveCoreBlockEntity core = ArchiveRegistry.coreFor(level, shelf);
        return core != null && core.isPowered() ? core : null;
    }

    public static boolean isShelf(ServerLevel level, @Nullable BlockPos pos) {
        return pos != null && level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof PatternShelfBlock;
    }

    // ------------------------------------------------------------------ pedidos

    /** Planeja e, se não faltar nada, põe na fila. */
    public static CraftingPlanner.Plan request(ServerLevel level, ArchiveCoreBlockEntity core, ItemResource want, long count) {
        CraftingPlanner.Plan plan = CraftingPlanner.plan(level, core, want, count);
        if (plan.ok()) {
            core.enqueue(plan.jobs());
        }
        return plan;
    }

    // ------------------------------------------------------------------ Golem Artesão: trabalhos de mesa

    /** Escolhe, para cada ingrediente, um item do Arquivo (contando o que já foi escolhido). Null se faltar. */
    private static @Nullable List<ItemResource> pick(ServerLevel level, ArchiveNet net, CraftingRecipe recipe) {
        List<Optional<Ingredient>> slots = GolemCrafting.slots(recipe);
        if (slots == null) {
            return null;
        }
        Map<ItemResource, Long> left = new HashMap<>(net.snapshot());
        List<ItemResource> picked = new ArrayList<>();
        for (Optional<Ingredient> slot : slots) {
            if (slot.isEmpty()) {
                picked.add(ItemResource.EMPTY);
                continue;
            }
            ItemResource found = null;
            for (Map.Entry<ItemResource, Long> e : left.entrySet()) {
                if (e.getValue() > 0 && slot.get().test(e.getKey().toStack())) {
                    found = e.getKey();
                    break;
                }
            }
            if (found == null) {
                return null;
            }
            left.merge(found, -1L, Long::sum);
            picked.add(found);
        }
        return picked;
    }

    /** O Artesão, na Estante, faz um passo de um trabalho de mesa. @return true se fez */
    public static boolean craftAtShelf(ServerLevel level, UUID worker, BlockPos shelf) {
        ArchiveCoreBlockEntity core = core(level, shelf);
        if (core == null) {
            return false;
        }
        ArchiveNet net = ArchiveNet.of(level, core);
        CraftJob job = core.claim(worker, true, j -> {
            CraftingRecipe r = GolemCrafting.get(level, j.pattern.recipe().orElse(null));
            return r != null && pick(level, net, r) != null;
        });
        if (job == null) {
            return false;
        }
        CraftingRecipe recipe = GolemCrafting.get(level, job.pattern.recipe().orElse(null));
        List<ItemResource> picked = recipe == null ? null : pick(level, net, recipe);
        if (picked == null) {
            core.release(worker);
            return false;
        }
        List<ItemStack> used = new ArrayList<>();
        for (ItemResource r : picked) {
            if (r.isEmpty()) {
                used.add(ItemStack.EMPTY);
            } else {
                ItemStack got = net.extractStack(r, 1);
                if (got.isEmpty()) {
                    used.forEach(net::insertStack);
                    core.release(worker);
                    return false;
                }
                used.add(got);
            }
        }
        CraftingInput input = GolemCrafting.input(recipe, used);
        if (!recipe.matches(input, level)) {
            used.forEach(net::insertStack);
            core.release(worker);
            return false;
        }
        ItemStack out = recipe.assemble(input);
        NonNullList<ItemStack> rest = recipe.getRemainingItems(input);
        drop(level, shelf, net.insertStack(out));
        for (ItemStack r : rest) {
            drop(level, shelf, net.insertStack(r));
        }
        core.finish(worker, true);
        level.sendParticles(ParticleTypes.WAX_OFF, shelf.getX() + 0.5, shelf.getY() + 1.1, shelf.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.0);
        level.playSound(null, shelf, SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.BLOCKS, 0.6F, 1.2F);
        return true;
    }

    private static void drop(ServerLevel level, BlockPos pos, ItemStack rest) {
        if (!rest.isEmpty()) {
            net.minecraft.world.level.block.Block.popResource(level, pos.above(), rest);
        }
    }

    // ------------------------------------------------------------------ Golem Mensageiro: trabalhos de processamento

    /** O que o Mensageiro leva: o trabalho e a entrada já tirada do Arquivo. */
    public record Delivery(CraftJob job, ItemStack input) {}

    /** Pega um trabalho de processamento cuja entrada o Arquivo tem e cuja máquina existe. */
    public static @Nullable Delivery claimProcessing(ServerLevel level, UUID worker, BlockPos shelf) {
        ArchiveCoreBlockEntity core = core(level, shelf);
        if (core == null) {
            return null;
        }
        ArchiveNet net = ArchiveNet.of(level, core);
        CraftJob job = core.claim(worker, false, j -> j.pattern.machine().isPresent() && level.isLoaded(j.pattern.machine().get())
                && level.getCapability(Capabilities.Item.BLOCK, j.pattern.machine().get(), Direction.UP) != null
                && net.count(ItemResource.of(j.pattern.input())) >= j.pattern.input().getCount());
        if (job == null) {
            return null;
        }
        ItemResource r = ItemResource.of(job.pattern.input());
        long got = net.extract(r, job.pattern.input().getCount(), false);
        if (got <= 0) {
            core.release(worker);
            return null;
        }
        return new Delivery(job, r.toStack((int) got));
    }

    /** O Mensageiro voltou: guarda o que trouxe e encerra o passo. */
    public static void returnToShelf(ServerLevel level, UUID worker, BlockPos shelf, ItemStack carried, boolean done) {
        ArchiveCoreBlockEntity core = ArchiveRegistry.coreFor(level, shelf);
        if (core == null) {
            drop(level, shelf, carried);
            return;
        }
        drop(level, shelf, ArchiveNet.of(level, core).insertStack(carried));
        core.finish(worker, done);
    }

    public static void release(ServerLevel level, UUID worker, @Nullable BlockPos shelf) {
        if (shelf == null) {
            return;
        }
        ArchiveCoreBlockEntity core = ArchiveRegistry.coreFor(level, shelf);
        if (core != null) {
            core.release(worker);
        }
    }
}
