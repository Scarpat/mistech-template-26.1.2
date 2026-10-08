package com.mistech.mistechmod.archive;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mistech.mistechmod.block.arcane.ArcaneAttachedBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Visão de um Arquivo num instante: os Pergaminhos do Núcleo + os recipientes com Selo do Arquivo ao alcance.
 * Coloca primeiro nos pergaminhos que já têm o item, depois em pergaminhos com tipo livre, por último nos recipientes selados.
 * Tira primeiro dos pergaminhos.
 */
public final class ArchiveNet {
    private record ScrollRef(ArchiveCells.Cell cell, ScrollTier tier) {}

    private final ServerLevel level;
    private final ArchiveCells cells;
    private final List<ScrollRef> scrolls = new ArrayList<>();
    private final List<ResourceHandler<ItemResource>> externals = new ArrayList<>();

    private ArchiveNet(ServerLevel level, ArchiveCoreBlockEntity core) {
        this.level = level;
        this.cells = ArchiveCells.get(level.getServer());
        for (int i = 0; i < core.getContainerSize(); i++) {
            ItemStack stack = core.getItem(i);
            if (stack.getItem() instanceof ArchiveScrollItem scroll) {
                java.util.UUID id = stack.get(ArchiveContent.CELL_ID.get());
                if (id != null) {
                    this.scrolls.add(new ScrollRef(this.cells.cell(id), scroll.tier()));
                }
            }
        }
        Set<BlockPos> seen = new HashSet<>();
        for (BlockPos seal : ArchiveRegistry.sealsOf(level, core.getBlockPos())) {
            BlockState state = level.getBlockState(seal);
            if (!(state.getBlock() instanceof ArchiveSealBlock)) {
                continue;
            }
            BlockPos target = ArcaneAttachedBlock.attachedPos(seal, state);
            Direction side = state.getValue(ArcaneAttachedBlock.FACING);
            if (seen.add(target)) {
                ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, target, side);
                if (handler != null) {
                    this.externals.add(handler);
                }
            }
        }
    }

    public static ArchiveNet of(ServerLevel level, ArchiveCoreBlockEntity core) {
        return new ArchiveNet(level, core);
    }

    /** Tudo o que está guardado (tipo -> quantidade). */
    public Map<ItemResource, Long> snapshot() {
        Map<ItemResource, Long> all = new LinkedHashMap<>();
        for (ScrollRef ref : this.scrolls) {
            ref.cell.items.forEach((r, n) -> all.merge(r, n, Long::sum));
        }
        for (ResourceHandler<ItemResource> handler : this.externals) {
            for (int slot = 0; slot < handler.size(); slot++) {
                ItemResource r = handler.getResource(slot);
                long n = handler.getAmountAsLong(slot);
                if (!r.isEmpty() && n > 0) {
                    all.merge(r, n, Long::sum);
                }
            }
        }
        return all;
    }

    public long count(ItemResource resource) {
        Long n = this.snapshot().get(resource);
        return n == null ? 0 : n;
    }

    /** @return quanto entrou (ou entraria, simulando). */
    public long insert(ItemResource resource, long amount, boolean simulate) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        long left = amount;
        for (ScrollRef ref : this.scrolls) {
            if (left > 0 && ref.cell.items.containsKey(resource)) {
                left -= this.cells.insert(ref.cell, ref.tier, resource, left, simulate);
            }
        }
        for (ScrollRef ref : this.scrolls) {
            if (left > 0) {
                left -= this.cells.insert(ref.cell, ref.tier, resource, left, simulate);
            }
        }
        for (ResourceHandler<ItemResource> handler : this.externals) {
            if (left <= 0) {
                break;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int moved = ResourceHandlerUtil.insertStacking(handler, resource, (int) Math.min(Integer.MAX_VALUE, left), tx);
                if (!simulate) {
                    tx.commit();
                }
                left -= moved;
            }
        }
        return amount - left;
    }

    /** @return quanto saiu. */
    public long extract(ItemResource resource, long amount, boolean simulate) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        long left = amount;
        for (ScrollRef ref : this.scrolls) {
            if (left > 0) {
                left -= this.cells.extract(ref.cell, resource, left, simulate);
            }
        }
        for (ResourceHandler<ItemResource> handler : this.externals) {
            if (left <= 0) {
                break;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int moved = handler.extract(resource, (int) Math.min(Integer.MAX_VALUE, left), tx);
                if (!simulate) {
                    tx.commit();
                }
                left -= moved;
            }
        }
        return amount - left;
    }

    /** Coloca a pilha toda; devolve o que não coube. */
    public ItemStack insertStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        long moved = this.insert(ItemResource.of(stack), stack.getCount(), false);
        ItemStack rest = stack.copy();
        rest.shrink((int) moved);
        return rest;
    }

    /** Tira até {@code max} (limitado à pilha do item) como uma pilha. */
    public ItemStack extractStack(ItemResource resource, int max) {
        int want = Math.min(max, resource.getMaxStackSize());
        long moved = this.extract(resource, want, false);
        return moved <= 0 ? ItemStack.EMPTY : resource.toStack((int) moved);
    }
}
