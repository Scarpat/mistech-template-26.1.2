package com.mistech.mistechmod.machine;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Regras comuns da Tuneladora e da Mineradora: o que pode ser quebrado e para onde vão os drops. */
public final class MiningHelper {
    private MiningHelper() {}

    /**
     * Dá para quebrar com a ferramenta virtual? Não quebra: ar, líquidos, blocos inquebráveis (bedrock, portais...),
     * blocos com block entity (baús, máquinas, spawners — nada de apagar o que é do jogador) e blocos que a ferramenta
     * não colhe (ex.: obsidiana com picareta de ferro), para não destruir sem drop.
     */
    public static boolean canMine(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.hasBlockEntity()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        return !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
    }

    /** Drops do bloco como se fosse quebrado com a ferramenta. */
    public static List<ItemStack> drops(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        return Block.getDrops(state, level, pos, null, null, tool);
    }

    /** Recipiente (qualquer bloco com capability de itens) em {@code pos}, visto pelo lado {@code side}. */
    public static ResourceHandler<ItemResource> container(ServerLevel level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.Item.BLOCK, pos, side);
    }

    /** Tenta guardar tudo; se algo não couber, não guarda nada e devolve false. */
    public static boolean insertAll(ResourceHandler<ItemResource> handler, List<ItemStack> stacks) {
        try (Transaction tx = Transaction.openRoot()) {
            for (ItemStack stack : stacks) {
                if (stack.isEmpty()) {
                    continue;
                }
                int inserted = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(stack), stack.getCount(), tx);
                if (inserted < stack.getCount()) {
                    return false; // sem commit: desfaz tudo
                }
            }
            tx.commit();
        }
        return true;
    }

    /** Guarda o que couber; devolve o que sobrou. */
    public static ItemStack insert(ResourceHandler<ItemResource> handler, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int inserted;
        try (Transaction tx = Transaction.openRoot()) {
            inserted = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
        }
        ItemStack rest = stack.copy();
        rest.shrink(inserted);
        return rest;
    }
}
