package com.mistech.mistechmod.archive;

import com.mistech.mistechmod.block.arcane.ArcaneAttachedBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Boca do Arquivo: a cada {@link #INTERVAL} ticks move até {@link #PER_OPERATION} itens. */
public class ArchivePortBlockEntity extends BlockEntity {
    public static final int INTERVAL = 10;
    public static final int PER_OPERATION = 64;
    public static final int FILTER_SIZE = 9;

    private boolean export;
    private NonNullList<ItemStack> filter = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);

    public ArchivePortBlockEntity(BlockPos pos, BlockState state) {
        super(ArchiveContent.PORT_BE.get(), pos, state);
    }

    private boolean filterEmpty() {
        return this.filter.stream().allMatch(ItemStack::isEmpty);
    }

    private boolean passes(ItemResource resource) {
        if (this.filterEmpty()) {
            return !this.export;
        }
        for (ItemStack f : this.filter) {
            if (!f.isEmpty() && resource.is(f.getItem())) {
                return true;
            }
        }
        return false;
    }

    public void toggleFilter(Player player, ItemStack stack) {
        for (int i = 0; i < FILTER_SIZE; i++) {
            if (!this.filter.get(i).isEmpty() && this.filter.get(i).is(stack.getItem())) {
                this.filter.set(i, ItemStack.EMPTY);
                this.setChanged();
                player.sendOverlayMessage(Component.translatable("message.mistech.archive_port.removed", stack.getHoverName()).withStyle(ChatFormatting.GRAY));
                return;
            }
        }
        for (int i = 0; i < FILTER_SIZE; i++) {
            if (this.filter.get(i).isEmpty()) {
                this.filter.set(i, stack.copyWithCount(1));
                this.setChanged();
                player.sendOverlayMessage(Component.translatable("message.mistech.archive_port.added", stack.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
                return;
            }
        }
        player.sendOverlayMessage(Component.translatable("message.mistech.archive_port.full").withStyle(ChatFormatting.RED));
    }

    public void clearFilter(Player player) {
        this.filter = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
        this.setChanged();
        player.sendOverlayMessage(Component.translatable("message.mistech.archive_port.cleared").withStyle(ChatFormatting.GRAY));
    }

    public void toggleMode(Player player) {
        this.export = !this.export;
        this.setChanged();
        player.sendOverlayMessage(Component.translatable(this.export ? "message.mistech.archive_port.export" : "message.mistech.archive_port.import")
                .withStyle(ChatFormatting.AQUA));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArchivePortBlockEntity port) {
        if (level.getGameTime() % INTERVAL != (pos.asLong() & 7) || !(level instanceof ServerLevel server)) {
            return;
        }
        ArchiveCoreBlockEntity core = ArchiveRegistry.coreFor(level, pos);
        if (core == null || !core.isPowered()) {
            return;
        }
        Direction side = state.getValue(ArcaneAttachedBlock.FACING);
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, ArcaneAttachedBlock.attachedPos(pos, state), side);
        if (handler == null) {
            return;
        }
        ArchiveNet net = ArchiveNet.of(server, core);
        int budget = PER_OPERATION;
        if (port.export) {
            for (ItemStack f : port.filter) {
                if (f.isEmpty() || budget <= 0) {
                    continue;
                }
                for (var entry : net.snapshot().entrySet()) {
                    if (budget <= 0 || !entry.getKey().is(f.getItem())) {
                        continue;
                    }
                    ItemResource r = entry.getKey();
                    int want = (int) Math.min(budget, entry.getValue());
                    int room;
                    try (Transaction sim = Transaction.openRoot()) {
                        room = ResourceHandlerUtil.insertStacking(handler, r, want, sim);
                    }
                    long got = room <= 0 ? 0 : net.extract(r, room, false);
                    if (got > 0) {
                        try (Transaction tx = Transaction.openRoot()) {
                            int put = ResourceHandlerUtil.insertStacking(handler, r, (int) got, tx);
                            tx.commit();
                            if (put < got) {
                                net.insert(r, got - put, false);
                            }
                            budget -= put;
                        }
                    }
                }
            }
        } else {
            for (int slot = 0; slot < handler.size() && budget > 0; slot++) {
                ItemResource r = handler.getResource(slot);
                if (r.isEmpty() || handler.getAmountAsLong(slot) <= 0 || !port.passes(r)) {
                    continue;
                }
                int want = (int) Math.min(budget, handler.getAmountAsLong(slot));
                long room = net.insert(r, want, true);
                if (room <= 0) {
                    continue;
                }
                int taken;
                try (Transaction tx = Transaction.openRoot()) {
                    taken = handler.extract(slot, r, (int) room, tx);
                    tx.commit();
                }
                if (taken > 0) {
                    long put = net.insert(r, taken, false);
                    budget -= taken;
                    if (put < taken) {
                        // não coube (mudou no meio): devolve
                        try (Transaction tx = Transaction.openRoot()) {
                            ResourceHandlerUtil.insertStacking(handler, r, (int) (taken - put), tx);
                            tx.commit();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Export", this.export);
        ContainerHelper.saveAllItems(output, this.filter);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.export = input.getBooleanOr("Export", false);
        this.filter = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.filter);
    }
}
