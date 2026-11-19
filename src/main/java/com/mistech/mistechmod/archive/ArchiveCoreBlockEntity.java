package com.mistech.mistechmod.archive;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Guarda os Pergaminhos (9 espaços; só pergaminhos contam) e a energia. Gasta {@link ScrollTier#energy} FE/t por pergaminho
 * (mínimo {@link #IDLE_COST}); sem energia o Arquivo fica desligado.
 */
public class ArchiveCoreBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 9;
    public static final int CAPACITY = 200_000;
    public static final int MAX_INPUT = 5_000;
    public static final int IDLE_COST = 1;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(CAPACITY, MAX_INPUT, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            ArchiveCoreBlockEntity.this.setChanged();
        }
    };
    private boolean powered;

    // ----- fila de fabricação (Fase 8) -----
    /** FE do Núcleo gastos a cada vez que um golem faz um passo de um trabalho. */
    public static final int JOB_ENERGY = 200;
    /** Um golem que pegou um trabalho e não voltou em tanto tempo perde a vez. */
    public static final int CLAIM_TIMEOUT = 2400;
    private final java.util.List<com.mistech.mistechmod.archive.craft.CraftJob> jobs = new java.util.ArrayList<>();
    private record Claim(UUID job, long time) {}
    private final java.util.Map<UUID, Claim> claims = new java.util.HashMap<>();

    public java.util.List<com.mistech.mistechmod.archive.craft.CraftJob> jobs() {
        return java.util.Collections.unmodifiableList(this.jobs);
    }

    public void enqueue(java.util.List<com.mistech.mistechmod.archive.craft.CraftJob> list) {
        this.jobs.addAll(list);
        this.setChanged();
    }

    /** Já tem trabalho na fila para essa saída? */
    public boolean queued(net.neoforged.neoforge.transfer.item.ItemResource output) {
        for (var job : this.jobs) {
            if (net.neoforged.neoforge.transfer.item.ItemResource.of(job.pattern.output()).equals(output)) {
                return true;
            }
        }
        return false;
    }

    /** Tira da fila os trabalhos dessa saída. @return quantos tirou */
    public int cancel(net.neoforged.neoforge.transfer.item.ItemResource output) {
        int before = this.jobs.size();
        this.jobs.removeIf(job -> net.neoforged.neoforge.transfer.item.ItemResource.of(job.pattern.output()).equals(output));
        this.setChanged();
        return before - this.jobs.size();
    }

    /** Um golem pega um passo do primeiro trabalho do tipo que {@code can} aceitar. Paga o FE do passo. */
    public com.mistech.mistechmod.archive.craft.@Nullable CraftJob claim(UUID worker, boolean crafting,
            java.util.function.Predicate<com.mistech.mistechmod.archive.craft.CraftJob> can) {
        this.release(worker);
        if (!this.powered || this.energy.getAmountAsInt() < JOB_ENERGY) {
            return null;
        }
        for (var job : this.jobs) {
            if (job.pattern.crafting() == crafting && job.free() > 0 && can.test(job)) {
                job.claimed++;
                this.claims.put(worker, new Claim(job.id, this.level == null ? 0 : this.level.getGameTime()));
                this.energy.set(this.energy.getAmountAsInt() - JOB_ENERGY);
                return job;
            }
        }
        return null;
    }

    private com.mistech.mistechmod.archive.craft.@Nullable CraftJob job(UUID id) {
        for (var job : this.jobs) {
            if (job.id.equals(id)) {
                return job;
            }
        }
        return null;
    }

    /** O golem terminou o passo ({@code done}) ou desistiu. */
    public void finish(UUID worker, boolean done) {
        Claim claim = this.claims.remove(worker);
        if (claim == null) {
            return;
        }
        var job = this.job(claim.job());
        if (job == null) {
            return;
        }
        job.claimed = Math.max(0, job.claimed - 1);
        if (done) {
            job.remaining--;
            if (job.remaining <= 0) {
                this.jobs.remove(job);
            }
        }
        this.setChanged();
    }

    public void release(UUID worker) {
        this.finish(worker, false);
    }

    private void expireClaims(long now) {
        java.util.List<UUID> old = new java.util.ArrayList<>();
        this.claims.forEach((worker, claim) -> {
            if (now - claim.time() > CLAIM_TIMEOUT) {
                old.add(worker);
            }
        });
        old.forEach(this::release);
    }

    public ArchiveCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ArchiveContent.CORE_BE.get(), pos, state);
    }

    public SimpleEnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public boolean isPowered() {
        return this.powered;
    }

    public int cost() {
        int cost = IDLE_COST;
        for (ItemStack stack : this.items) {
            if (stack.getItem() instanceof ArchiveScrollItem scroll) {
                cost += scroll.tier().energy;
            }
        }
        return cost;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArchiveCoreBlockEntity core) {
        int cost = core.cost();
        int have = core.energy.getAmountAsInt();
        boolean wasPowered = core.powered;
        core.powered = have >= cost;
        if (core.powered) {
            core.energy.set(have - cost);
        }
        if (level.getGameTime() % 20 == 0 && level instanceof ServerLevel server) {
            ArchiveCells cells = ArchiveCells.get(server.getServer());
            for (ItemStack stack : core.items) {
                if (stack.getItem() instanceof ArchiveScrollItem) {
                    UUID id = stack.get(ArchiveContent.CELL_ID.get());
                    if (id == null) {
                        id = UUID.randomUUID();
                        stack.set(ArchiveContent.CELL_ID.get(), id);
                    }
                    ArchiveCells.Cell cell = cells.cell(id);
                    ArchiveContent.Usage usage = new ArchiveContent.Usage(cell.items.size(), cell.total());
                    if (!usage.equals(stack.get(ArchiveContent.CELL_USAGE.get()))) {
                        stack.set(ArchiveContent.CELL_USAGE.get(), usage);
                        core.setChanged();
                    }
                }
            }
        }
        if (level.getGameTime() % 200 == 0 && !core.claims.isEmpty()) {
            core.expireClaims(level.getGameTime());
        }
        if (wasPowered != core.powered) {
            core.setChanged();
        }
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof ArchiveScrollItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.mistech.archive_core");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, id, inventory, this, 1);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            ArchiveRegistry.add(this.level, ArchiveRegistry.Kind.CORE, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            ArchiveRegistry.remove(this.level, ArchiveRegistry.Kind.CORE, this.worldPosition);
        }
        super.setRemoved();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.energy.deserialize(input);
        this.jobs.clear();
        this.jobs.addAll(input.read("CraftJobs", com.mistech.mistechmod.archive.craft.CraftJob.CODEC.listOf()).orElse(java.util.List.of()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        this.energy.serialize(output);
        if (!this.jobs.isEmpty()) {
            output.store("CraftJobs", com.mistech.mistechmod.archive.craft.CraftJob.CODEC.listOf(), java.util.List.copyOf(this.jobs));
        }
    }
}
