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
public abstract class MachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer,
        com.mistech.mistechmod.compat.InfoProvider {
    /** Controle por redstone: ignorar, só com sinal, só sem sinal. */
    public enum RedstoneMode { IGNORE, HIGH, LOW }

    /** O que cada face faz com itens (funis, canos e a entrada/saída automática). */
    public enum SideMode {
        NONE, INPUT, OUTPUT, BOTH;

        public boolean in() {
            return this == INPUT || this == BOTH;
        }

        public boolean out() {
            return this == OUTPUT || this == BOTH;
        }
    }

    /** Botões da tela (clickMenuButton) reservados para a configuração: 100 = redstone, 110+face = lado, 120/121 = automático, 130+tipo = tirar melhoria. */
    public static final int BUTTON_REDSTONE = 100, BUTTON_SIDE = 110, BUTTON_AUTO_OUT = 120, BUTTON_AUTO_IN = 121, BUTTON_UPGRADE = 130;

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
    private final int[] bothFaces;

    // ----- configuração (redstone, lados, automático, melhorias) -----
    protected RedstoneMode redstone = RedstoneMode.IGNORE;
    /** Por {@link Direction#get3DDataValue()}. Padrão: baixo = saída, o resto = entrada (como sempre foi). */
    protected final SideMode[] sides = defaultSides();
    protected boolean autoOutput, autoInput;
    protected final int[] upgrades = new int[MachineUpgrade.values().length];
    private boolean redstoneBlocked;
    private int ioTimer;
    /** Passo deste tick (calculado uma vez por tick) e a sobra fracionária. */
    private int currentStep = 2;
    private double stepCarry;
    private double energyCarry;

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
                case 9 -> be.redstone.ordinal() | (be.redstoneBlocked ? 4 : 0) | (be.facing2D() << 3);
                case 10 -> be.packSides();
                case 11 -> be.packFlags();
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
        java.util.List<Integer> both = new java.util.ArrayList<>(in);
        both.addAll(out);
        this.bothFaces = both.stream().mapToInt(Integer::intValue).toArray();
    }

    private static SideMode[] defaultSides() {
        SideMode[] sides = new SideMode[6];
        for (Direction d : Direction.values()) {
            sides[d.get3DDataValue()] = d == Direction.DOWN ? SideMode.OUTPUT : SideMode.INPUT;
        }
        return sides;
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public static <T extends MachineBlockEntity> void serverTick(Level level, BlockPos pos, BlockState state, T typed) {
        MachineBlockEntity machine = typed;
        if (level instanceof ServerLevel server) {
            if (--machine.boostCheck <= 0) {
                machine.boostCheck = 20;
                machine.boosted = machine.canBeBoosted() && GnomeBoost.isBoosted(level, pos);
            }
            boolean allowed = machine.redstoneAllows(level, pos);
            if (machine.redstoneBlocked == allowed) {
                machine.redstoneBlocked = !allowed;
            }
            if (!allowed) {
                machine.setLit(level, pos, state, false);
                return;
            }
            machine.computeStep();
            machine.tick(server, pos, state);
            if (machine.ioTimer == 0) {
                machine.syncRadiusIfChanged();
            }
            if (++machine.ioTimer >= com.mistech.mistechmod.Config.autoIoInterval()) {
                machine.ioTimer = 0;
                if (machine.autoOutput || machine.autoInput) {
                    machine.autoTransfer(server, pos);
                }
            }
        }
    }

    // ----- redstone -----

    public RedstoneMode redstoneMode() {
        return this.redstone;
    }

    protected boolean redstoneAllows(Level level, BlockPos pos) {
        if (this.redstone == RedstoneMode.IGNORE) {
            return true;
        }
        boolean powered = level.hasNeighborSignal(pos);
        return this.redstone == RedstoneMode.HIGH ? powered : !powered;
    }

    // ----- lados -----

    private int facing2D() {
        BlockState state = this.getBlockState();
        return state.hasProperty(MachineBlock.FACING) ? state.getValue(MachineBlock.FACING).get2DDataValue() : 2;
    }

    /** A máquina tem espaços que funis/canos usam? (sem isso a aba de lados nem aparece) */
    public boolean hasItemFaces() {
        return this.bothFaces.length > 0;
    }

    public SideMode side(Direction direction) {
        return this.sides[direction.get3DDataValue()];
    }

    private int packSides() {
        int packed = 0;
        for (int i = 0; i < 6; i++) {
            packed |= this.sides[i].ordinal() << (i * 2);
        }
        return packed;
    }

    private int packFlags() {
        int flags = (this.autoOutput ? 1 : 0) | (this.autoInput ? 2 : 0);
        flags |= Math.min(7, this.upgrades[MachineUpgrade.SPEED.ordinal()]) << 2;
        flags |= Math.min(7, this.upgrades[MachineUpgrade.EFFICIENCY.ordinal()]) << 5;
        flags |= Math.min(7, this.upgrades[MachineUpgrade.RANGE.ordinal()]) << 8;
        flags |= (this.acceptsUpgrade(MachineUpgrade.SPEED) ? 1 : 0) << 11;
        flags |= (this.acceptsUpgrade(MachineUpgrade.RANGE) ? 1 : 0) << 12;
        flags |= (this.hasItemFaces() ? 1 : 0) << 13;
        return flags;
    }

    /** Empurra a saída para os blocos encostados nas faces de saída e puxa a entrada das faces de entrada. */
    private void autoTransfer(ServerLevel level, BlockPos pos) {
        int amount = com.mistech.mistechmod.Config.autoIoAmount();
        for (Direction direction : Direction.values()) {
            SideMode mode = this.side(direction);
            if (mode == SideMode.NONE) {
                continue;
            }
            BlockPos other = pos.relative(direction);
            if (!level.isLoaded(other)) {
                continue;
            }
            var neighbor = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, other, direction.getOpposite());
            if (neighbor == null) {
                continue;
            }
            var self = new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(this, direction);
            if (this.autoOutput && mode.out() && this.outputFaces.length > 0) {
                net.neoforged.neoforge.transfer.ResourceHandlerUtil.moveStacking(self, neighbor, r -> true, amount, null);
            }
            if (this.autoInput && mode.in() && this.inputFaces.length > 0) {
                net.neoforged.neoforge.transfer.ResourceHandlerUtil.moveStacking(neighbor, self, r -> true, amount, null);
            }
        }
    }

    // ----- melhorias -----

    public int upgrade(MachineUpgrade type) {
        return this.upgrades[type.ordinal()];
    }

    /** Velocidade/Eficiência: máquinas que trabalham (não geradores nem baterias). Alcance: máquinas de área. */
    public boolean acceptsUpgrade(MachineUpgrade type) {
        return switch (type) {
            case SPEED, EFFICIENCY -> this.canBeBoosted() && this.layout.arrowX >= 0 || this.usesWorkStep();
            case RANGE -> this.usesRange();
        };
    }

    /** Máquinas sem seta de progresso que mesmo assim usam {@link #workStep()} (fazenda) dizem que sim. */
    protected boolean usesWorkStep() {
        return false;
    }

    protected boolean usesRange() {
        return false;
    }

    /** Raio extra das Melhorias de Alcance. */
    protected int rangeBonus() {
        return this.upgrade(MachineUpgrade.RANGE);
    }

    /** Coloca uma melhoria (devolve false se não aceita ou já está no máximo). Só no servidor. */
    public boolean installUpgrade(MachineUpgrade type) {
        if (!this.acceptsUpgrade(type) || this.upgrades[type.ordinal()] >= type.max()) {
            return false;
        }
        this.upgrades[type.ordinal()]++;
        this.upgradesChanged();
        return true;
    }

    private void upgradesChanged() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Os botões de configuração da tela. Só no servidor. */
    public boolean handleConfigButton(net.minecraft.world.entity.player.Player player, int id) {
        if (id == BUTTON_REDSTONE) {
            this.redstone = RedstoneMode.values()[(this.redstone.ordinal() + 1) % RedstoneMode.values().length];
        } else if (id >= BUTTON_SIDE && id < BUTTON_SIDE + 6) {
            int i = id - BUTTON_SIDE;
            this.sides[i] = SideMode.values()[(this.sides[i].ordinal() + 1) % SideMode.values().length];
            if (this.level != null) {
                this.level.invalidateCapabilities(this.worldPosition);
            }
        } else if (id == BUTTON_AUTO_OUT) {
            this.autoOutput = !this.autoOutput;
        } else if (id == BUTTON_AUTO_IN) {
            this.autoInput = !this.autoInput;
        } else if (id >= BUTTON_UPGRADE && id < BUTTON_UPGRADE + MachineUpgrade.values().length) {
            MachineUpgrade type = MachineUpgrade.values()[id - BUTTON_UPGRADE];
            if (this.upgrades[type.ordinal()] <= 0) {
                return false;
            }
            this.upgrades[type.ordinal()]--;
            player.getInventory().placeItemBackInInventory(new ItemStack(type.item()));
            this.upgradesChanged();
            return true;
        } else {
            return false;
        }
        this.setChanged();
        return true;
    }

    /** Ao quebrar: as melhorias caem junto com os itens. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            for (MachineUpgrade type : MachineUpgrade.values()) {
                int count = this.upgrades[type.ordinal()];
                if (count > 0) {
                    net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(type.item(), count));
                    this.upgrades[type.ordinal()] = 0;
                }
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    /** Raio mandado ao cliente (máquinas de área), -1 = nenhum. */
    protected int clientSyncRadius() {
        return -1;
    }

    private int lastSentRadius = Integer.MIN_VALUE;
    /** No cliente: o raio que o servidor mandou. */
    protected int clientRadius = -1;

    /** Chamar de vez em quando no servidor: se o raio mudou (runa, melhoria), avisa o cliente. */
    protected void syncRadiusIfChanged() {
        int r = this.clientSyncRadius();
        if (r != this.lastSentRadius && this.level != null) {
            this.lastSentRadius = r;
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putIntArray("Upgrades", this.upgrades.clone());
        tag.putInt("ClientRadius", this.clientSyncRadius());
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        int[] ups = input.getIntArray("Upgrades").orElse(null);
        if (ups != null) {
            for (int i = 0; i < Math.min(ups.length, this.upgrades.length); i++) {
                this.upgrades[i] = Math.max(0, ups[i]);
            }
        }
        this.clientRadius = input.getIntOr("ClientRadius", -1);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ValueInput input) {
        this.handleUpdateTag(input);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    // ----- Jade / informações -----

    @Override
    public void addInfo(java.util.List<Component> lines) {
        if (this.layout.arrowX >= 0 && this.maxProgress > 0 && this.progress > 0) {
            lines.add(com.mistech.mistechmod.compat.InfoProvider.progress(this.progress, this.maxProgress));
        }
        if (this.redstoneBlocked) {
            lines.add(Component.translatable("info.mistech.redstone_off"));
        }
        if (this.boosted) {
            lines.add(Component.translatable("gui.mistech.gnome_boost"));
        }
        for (MachineUpgrade type : MachineUpgrade.values()) {
            int n = this.upgrade(type);
            if (n > 0) {
                lines.add(Component.translatable("info.mistech.upgrade." + type.id(), n));
            }
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

    /** Quanto o progresso anda neste tick (2 = normal, 3 = com gnomo; mais com Melhorias de Velocidade e o config). */
    protected int workStep() {
        return this.currentStep;
    }

    private void computeStep() {
        double base = (this.boosted ? 3.0 : 2.0) * com.mistech.mistechmod.Config.machineSpeed()
                * (1.0 + this.upgrade(MachineUpgrade.SPEED) * com.mistech.mistechmod.Config.speedPerUpgrade());
        double total = base + this.stepCarry;
        int whole = (int) Math.floor(total);
        this.stepCarry = total - whole;
        this.currentStep = Math.max(0, whole);
    }

    /** Gasto de energia deste tick para uma máquina que gasta {@code perTick} FE/tick na velocidade normal. */
    protected int energyStep(int perTick) {
        if (perTick <= 0) {
            return 0;
        }
        double cost = perTick * this.workStep() / 2.0
                * (1.0 + this.upgrade(MachineUpgrade.SPEED) * com.mistech.mistechmod.Config.speedEnergyPenalty())
                * Math.max(0.1, 1.0 - this.upgrade(MachineUpgrade.EFFICIENCY) * com.mistech.mistechmod.Config.efficiencyPerUpgrade())
                * com.mistech.mistechmod.Config.machineEnergy();
        return (int) Math.ceil(cost);
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

    /** Energia gerada (geradores): passa pelo multiplicador do config. */
    protected void addEnergy(int amount) {
        double scaled = amount * com.mistech.mistechmod.Config.generatorOutput() + this.energyCarry;
        int whole = (int) Math.floor(scaled);
        this.energyCarry = scaled - whole;
        this.energy.set(Math.min(this.layout.capacity, this.energy.getAmountAsInt() + whole));
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

    private static final int[] NO_SLOTS = new int[0];

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return switch (this.side(direction)) {
            case NONE -> NO_SLOTS;
            case INPUT -> this.inputFaces;
            case OUTPUT -> this.outputFaces;
            case BOTH -> this.bothFaces;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return (direction == null || this.side(direction).in()) && this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return this.layout.kind(slot) == MachineLayout.Kind.OUTPUT && this.side(direction).out();
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
        int rs = input.getIntOr("Redstone", 0);
        this.redstone = RedstoneMode.values()[Math.floorMod(rs, RedstoneMode.values().length)];
        int[] packedSides = input.getIntArray("Sides").orElse(null);
        if (packedSides != null && packedSides.length == 6) {
            for (int i = 0; i < 6; i++) {
                this.sides[i] = SideMode.values()[Math.floorMod(packedSides[i], SideMode.values().length)];
            }
        }
        this.autoOutput = input.getBooleanOr("AutoOutput", false);
        this.autoInput = input.getBooleanOr("AutoInput", false);
        int[] ups = input.getIntArray("Upgrades").orElse(null);
        if (ups != null) {
            for (int i = 0; i < Math.min(ups.length, this.upgrades.length); i++) {
                this.upgrades[i] = Math.max(0, ups[i]);
            }
        }
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
        output.putInt("Redstone", this.redstone.ordinal());
        int[] packedSides = new int[6];
        for (int i = 0; i < 6; i++) {
            packedSides[i] = this.sides[i].ordinal();
        }
        output.putIntArray("Sides", packedSides);
        output.putBoolean("AutoOutput", this.autoOutput);
        output.putBoolean("AutoInput", this.autoInput);
        output.putIntArray("Upgrades", this.upgrades.clone());
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
