package com.mistech.mistechmod.machine;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Base das máquinas novas do Mistech (Triturador, geradores, Colheitadeira, Plantadeira).
 * Cuida de: itens (espaços do {@link MachineLayout}), buffer de energia, dados da tela, Slot de Runa,
 * Impulso de Gnomo, automação por face (cima/lados = entrada, baixo = saída) e salvar/carregar.
 *
 * Progresso em "meios ticks": cada tick soma {@link #workStep()} (2 normal, 3 com gnomo = +50%).
 */
public abstract class MachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    protected final MachineLayout layout;
    protected NonNullList<ItemStack> items;
    protected final SimpleEnergyHandler energy;
    protected int progress;
    protected int maxProgress;
    /** Informações extras mostradas na tela (dados 4, 5, 7 e 8). */
    protected int info0, info1, info2, info3;
    /** Operações feitas desde o último uso gasto da runa. */
    protected int runeOps;
    protected boolean boosted;
    protected int boostCheck;

    private final int[] inputFaces;
    private final int[] outputFaces;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int id) {
            MachineBlockEntity be = MachineBlockEntity.this;
            int e = be.energy.getAmountAsInt();
            return switch (id) {
                case 0 -> be.progress;
                case 1 -> be.maxProgress;
                case 2 -> e & 0x7FFF;
                case 3 -> (e >> 15) & 0x7FFF;
                case 4 -> be.info0;
                case 5 -> be.info1;
                case 6 -> be.boosted ? 1 : 0;
                case 7 -> be.info2;
                case 8 -> be.info3;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            // só leitura
        }

        @Override
        public int getCount() {
            return MachineMenu.DATA_COUNT;
        }
    };

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineLayout layout,
            int maxInput, int maxOutput) {
        super(type, pos, state);
        this.layout = layout;
        this.items = NonNullList.withSize(layout.size(), ItemStack.EMPTY);
        this.energy = new SimpleEnergyHandler(layout.capacity, maxInput, maxOutput) {
            @Override
            protected void onEnergyChanged(int previousAmount) {
                MachineBlockEntity.this.setChanged();
            }
        };
        java.util.List<Integer> in = new java.util.ArrayList<>();
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (int i = 0; i < layout.size(); i++) {
            switch (layout.kind(i)) {
                case INPUT, FUEL -> in.add(i);
                case OUTPUT -> out.add(i);
                default -> { }
            }
        }
        this.inputFaces = in.stream().mapToInt(Integer::intValue).toArray();
        this.outputFaces = out.stream().mapToInt(Integer::intValue).toArray();
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public static <T extends MachineBlockEntity> void serverTick(Level level, BlockPos pos, BlockState state, T machine) {
        if (level instanceof ServerLevel server) {
            if (--machine.boostCheck <= 0) {
                machine.boostCheck = 20;
                machine.boosted = machine.canBeBoosted() && GnomeBoost.isBoosted(level, pos);
            }
            machine.tick(server, pos, state);
        }
    }

    protected abstract void tick(ServerLevel level, BlockPos pos, BlockState state);

    /** Geradores não recebem impulso de gnomo. */
    protected boolean canBeBoosted() {
        return true;
    }

    public boolean isBoosted() {
        return this.boosted;
    }

    /** Quanto o progresso anda neste tick (2 = normal, 3 = com gnomo). */
    protected int workStep() {
        return this.boosted ? 3 : 2;
    }

    /** Gasto de energia deste tick para uma máquina que gasta {@code perTick} FE/tick na velocidade normal. */
    protected int energyStep(int perTick) {
        return perTick * this.workStep() / 2;
    }

    protected int storedEnergy() {
        return this.energy.getAmountAsInt();
    }

    protected boolean hasEnergy(int amount) {
        return this.energy.getAmountAsInt() >= amount;
    }

    protected void drainEnergy(int amount) {
        this.energy.set(Math.max(0, this.energy.getAmountAsInt() - amount));
    }

    protected void addEnergy(int amount) {
        this.energy.set(Math.min(this.layout.capacity, this.energy.getAmountAsInt() + amount));
    }

    /** Envia energia para os 6 blocos encostados (geradores). */
    protected void pushEnergy(Level level, BlockPos pos, int maxPerTarget) {
        for (Direction direction : Direction.values()) {
            if (this.energy.getAmountAsInt() <= 0) {
                return;
            }
            EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, pos.relative(direction), direction.getOpposite());
            if (target != null && target != this.energy) {
                EnergyHandlerUtil.move(this.energy, target, maxPerTarget, null);
            }
        }
    }

    protected void setLit(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (state.hasProperty(MachineBlock.LIT) && state.getValue(MachineBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(MachineBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    // ----- Slot de Runa -----

    /** Primeiro espaço de runa (ou -1). */
    protected int runeSlot() {
        for (int i = 0; i < this.layout.size(); i++) {
            if (this.layout.kind(i) == MachineLayout.Kind.RUNE) {
                return i;
            }
        }
        return -1;
    }

    /** Runa que está no espaço de runa (vazio se não houver). */
    protected ItemStack rune() {
        int slot = this.runeSlot();
        return slot < 0 ? ItemStack.EMPTY : this.items.get(slot);
    }

    protected boolean hasRune(net.minecraft.world.item.Item rune) {
        return this.rune().is(rune);
    }

    /** Conta uma operação feita com a runa; a cada {@code opsPerUse} operações gasta 1 uso (a runa some no último). */
    protected void countRuneOp(int opsPerUse) {
        int slot = this.runeSlot();
        if (slot < 0 || this.items.get(slot).isEmpty()) {
            return;
        }
        if (++this.runeOps < opsPerUse) {
            return;
        }
        this.runeOps = 0;
        ItemStack rune = this.items.get(slot);
        if (com.mistech.mistechmod.purity.TaintEvents.isSealed(rune)) {
            return; // runa selada no Santuário: não gasta
        }
        if (rune.isDamageableItem() || com.mistech.mistechmod.beyond.AmberSeal.isAmber(rune)) {
            int damage = rune.getDamageValue() + 1;
            if (damage >= rune.getMaxDamage()) {
                this.items.set(slot, ItemStack.EMPTY);
            } else {
                rune.setDamageValue(damage);
            }
        } else {
            rune.shrink(1);
        }
        this.setChanged();
    }

    // ----- saída -----

    /** Cabe a pilha inteira nos espaços de saída? */
    protected boolean canOutput(ItemStack stack) {
        return this.simulateInsert(stack.copy()) == 0;
    }

    /** Quanto sobraria ao colocar a pilha nos espaços de saída (não altera nada). */
    private int simulateInsert(ItemStack stack) {
        int left = stack.getCount();
        for (int slot : this.outputFaces) {
            ItemStack there = this.items.get(slot);
            if (there.isEmpty()) {
                left -= Math.min(left, stack.getMaxStackSize());
            } else if (ItemStack.isSameItemSameComponents(there, stack)) {
                left -= Math.min(left, there.getMaxStackSize() - there.getCount());
            }
            if (left <= 0) {
                return 0;
            }
        }
        return left;
    }

    /** Coloca nos espaços de saída; devolve o que não coube. */
    protected ItemStack insertOutput(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int slot : this.outputFaces) {
            if (rest.isEmpty()) {
                break;
            }
            ItemStack there = this.items.get(slot);
            if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, rest)) {
                int move = Math.min(rest.getCount(), there.getMaxStackSize() - there.getCount());
                there.grow(move);
                rest.shrink(move);
            }
        }
        for (int slot : this.outputFaces) {
            if (rest.isEmpty()) {
                break;
            }
            if (this.items.get(slot).isEmpty()) {
                int move = Math.min(rest.getCount(), rest.getMaxStackSize());
                this.items.set(slot, rest.split(move));
            }
        }
        this.setChanged();
        return rest;
    }

    /** Todos os espaços de saída cheios (nada mais entra)? */
    protected boolean outputFull() {
        for (int slot : this.outputFaces) {
            ItemStack there = this.items.get(slot);
            if (there.isEmpty() || there.getCount() < there.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    // ----- Container -----

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.mistech." + this.layout.name);
    }

    @Override
    public int getContainerSize() {
        return this.layout.size();
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return this.layout.accepts(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return direction == Direction.DOWN ? this.outputFaces : this.inputFaces;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return direction != Direction.DOWN && this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return this.layout.kind(slot) == MachineLayout.Kind.OUTPUT;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.progress = input.getIntOr("Progress", 0);
        this.maxProgress = input.getIntOr("MaxProgress", 0);
        this.runeOps = input.getIntOr("RuneOps", 0);
        this.energy.deserialize(input);
        this.loadExtra(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("Progress", this.progress);
        output.putInt("MaxProgress", this.maxProgress);
        output.putInt("RuneOps", this.runeOps);
        this.energy.serialize(output);
        this.saveExtra(output);
    }

    /** Botão da tela (clickMenuButton). Só roda no servidor; true = tratado. */
    public boolean onMenuButton(net.minecraft.world.entity.player.Player player, int id) {
        return false;
    }

    protected void loadExtra(ValueInput input) {}

    protected void saveExtra(ValueOutput output) {}

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new MachineMenu(this.layout.menuType(), this.layout, containerId, inventory, this, this.dataAccess);
    }
}
