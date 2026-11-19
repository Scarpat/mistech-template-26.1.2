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
    protected final MachineEnergy energy;
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

    // ----- engastes (MachineSockets) -----
    /** Runas engastadas que fazem efeito nesta máquina (recalculado a cada tick). */
    protected final java.util.EnumSet<com.mistech.mistechmod.item.RuneElement> sockets =
            java.util.EnumSet.noneOf(com.mistech.mistechmod.item.RuneElement.class);
    private int socketWork;
    private int idleTicks;
    private int vigilSignal = -1;
    private int vacuumTimer;

    /** Buffer de energia com entrada/saída que a Runa de Energia pode aumentar. */
    public static class MachineEnergy extends SimpleEnergyHandler {
        private final int baseInsert, baseExtract;
        private final Runnable onChange;

        public MachineEnergy(int capacity, int maxInsert, int maxExtract, Runnable onChange) {
            super(capacity, maxInsert, maxExtract);
            this.baseInsert = maxInsert;
            this.baseExtract = maxExtract;
            this.onChange = onChange;
        }

        public void setRate(double factor) {
            this.maxInsert = (int) Math.round(this.baseInsert * factor);
            this.maxExtract = (int) Math.round(this.baseExtract * factor);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            this.onChange.run();
        }
    }

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
        this.energy = new MachineEnergy(layout.capacity, maxInput, maxOutput, this::setChanged);
        java.util.List<Integer> in = new java.util.ArrayList<>();
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (int i = 0; i < layout.size(); i++) {
            switch (layout.kind(i)) {
                // RUNE entra pelos canos também (filtro, fragmento, matriz...): o layout decide o que cabe
                case INPUT, FUEL, RUNE -> in.add(i);
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

    /**
     * Padrão: todas as faces "entrada e saída" — canos colocam nos espaços de entrada e tiram só dos de saída, de qualquer lado.
     * (Antes era entrada nos lados e saída só embaixo, e canos nos lados não tiravam nada.)
     */
    private static SideMode[] defaultSides() {
        SideMode[] sides = new SideMode[6];
        java.util.Arrays.fill(sides, SideMode.BOTH);
        return sides;
    }

    /** A configuração antiga de fábrica (entrada nos lados, saída embaixo): quem nunca mexeu passa para o padrão novo. */
    private static boolean isOldDefault(int[] packed) {
        for (Direction d : Direction.values()) {
            SideMode old = d == Direction.DOWN ? SideMode.OUTPUT : SideMode.INPUT;
            if (Math.floorMod(packed[d.get3DDataValue()], SideMode.values().length) != old.ordinal()) {
                return false;
            }
        }
        return true;
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public static <T extends MachineBlockEntity> void serverTick(Level level, BlockPos pos, BlockState state, T typed) {
        MachineBlockEntity machine = typed;
        if (level instanceof ServerLevel server) {
            machine.refreshSockets();
            if (--machine.boostCheck <= 0) {
                machine.boostCheck = 20;
                machine.boosted = machine.canBeBoosted() && GnomeBoost.isBoosted(level, pos);
                machine.areaSpeed = com.mistech.mistechmod.purity.area.AreaTaint.machineSpeed(level, pos);
                machine.refreshSky(server);
            }
            boolean allowed = machine.redstoneAllows(level, pos);
            if (machine.redstoneBlocked == allowed) {
                machine.redstoneBlocked = !allowed;
            }
            if (!allowed) {
                machine.setLit(level, pos, state, false);
                machine.vigilTick(server, pos, false);
                return;
            }
            machine.computeStep();
            machine.tick(server, pos, state);
            machine.socketTick(server, pos);
            machine.emitTaint(server, pos);
            if (machine.ioTimer == 0) {
                machine.syncRadiusIfChanged();
            }
            int interval = com.mistech.mistechmod.Config.autoIoInterval();
            if (machine.socketActive(com.mistech.mistechmod.item.RuneElement.AIR)) {
                interval = Math.max(1, interval / com.mistech.mistechmod.celestial.MachineSky.airDivisor(machine.ownerMask));
            }
            if (++machine.ioTimer >= interval) {
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

    /** A entrada automática puxa fluido para esta máquina? (as bombas só mandam; senão o fluido iria e voltaria) */
    protected boolean autoFluidInput() {
        return true;
    }

    /** mB de fluido movidos por face a cada rodada da entrada/saída automática. */
    public static final int AUTO_FLUID = 1000;

    /** Empurra a saída para os blocos encostados nas faces de saída e puxa a entrada das faces de entrada (itens e fluidos). */
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
            if (neighbor != null) {
                var self = new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(this, direction);
                if (this.autoOutput && mode.out() && this.outputFaces.length > 0) {
                    net.neoforged.neoforge.transfer.ResourceHandlerUtil.moveStacking(self, neighbor, r -> true, amount, null);
                }
                if (this.autoInput && mode.in() && this.inputFaces.length > 0) {
                    net.neoforged.neoforge.transfer.ResourceHandlerUtil.moveStacking(neighbor, self, r -> true, amount, null);
                }
            }
            // fluidos também: o tanque da máquina (só sai o que o tanque deixa tirar; só entra o que ele aceita)
            var fluidNeighbor = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, other, direction.getOpposite());
            if (fluidNeighbor != null) {
                var fluidSelf = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, pos, direction);
                if (fluidSelf != null && fluidSelf != fluidNeighbor) {
                    int out = 0;
                    if (this.autoOutput && mode.out()) {
                        out = net.neoforged.neoforge.transfer.ResourceHandlerUtil.move(fluidSelf, fluidNeighbor, r -> true, AUTO_FLUID, null);
                    }
                    if (this.autoInput && mode.in() && out == 0 && this.autoFluidInput()) {
                        net.neoforged.neoforge.transfer.ResourceHandlerUtil.move(fluidNeighbor, fluidSelf, r -> true, AUTO_FLUID, null);
                    }
                }
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
        return this.upgrade(MachineUpgrade.RANGE) + (this.socketActive(com.mistech.mistechmod.item.RuneElement.CLOUD) ? 1 : 0)
                + com.mistech.mistechmod.celestial.MachineSky.range(this, this.ownerMask);
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
            if (this.celestialCore) {
                net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(),
                        new ItemStack(com.mistech.mistechmod.endgame.EndgameContent.CELESTIAL_CORE.get()));
                this.celestialCore = false;
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
        if (!this.sockets.isEmpty()) {
            net.minecraft.network.chat.MutableComponent names = Component.empty();
            boolean first = true;
            for (com.mistech.mistechmod.item.RuneElement e : this.sockets) {
                if (!first) {
                    names.append(", ");
                }
                names.append(Component.translatable(e.translationKey()).withColor(e.color()));
                first = false;
            }
            lines.add(Component.translatable("info.mistech.sockets", names));
        }
        if (this.celestialCore) {
            lines.add(Component.translatable("info.mistech.celestial_core"));
        }
        if (Math.abs(this.skySpeed - 1.0) > 0.001) {
            lines.add(Component.translatable("info.mistech.sky_speed", percent(this.skySpeed)));
        }
        if (Math.abs(this.skyGeneration - 1.0) > 0.001) {
            lines.add(Component.translatable("info.mistech.sky_generation", percent(this.skyGeneration)));
        }
        if (Math.abs(this.skyTaint - 1.0) > 0.001 && this.taintPerMinute() > 0) {
            lines.add(Component.translatable("info.mistech.sky_taint", percent(this.skyTaint)));
        }
        if (this.level != null) {
            float area = com.mistech.mistechmod.purity.area.AreaTaint.get(this.level, this.worldPosition);
            if (area >= 1.0F) {
                lines.add(Component.translatable("info.mistech.area_taint", com.mistech.mistechmod.purity.Taint.describe(area)));
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
                * (1.0 + this.upgrade(MachineUpgrade.SPEED) * com.mistech.mistechmod.Config.speedPerUpgrade())
                * (1.0 + com.mistech.mistechmod.machine.socket.MachineSockets.speedBonus(this.sockets))
                * this.areaSpeed * this.skySpeed;
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
        double penalty = com.mistech.mistechmod.machine.socket.MachineSockets.cooled(this.sockets) ? 0.0
                : this.upgrade(MachineUpgrade.SPEED) * com.mistech.mistechmod.Config.speedEnergyPenalty();
        double cost = perTick * this.workStep() / 2.0
                * (1.0 + penalty)
                * com.mistech.mistechmod.machine.socket.MachineSockets.energyFactor(this.sockets)
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
        double scaled = amount * com.mistech.mistechmod.Config.generatorOutput() * this.skyGeneration
                * com.mistech.mistechmod.machine.socket.MachineSockets.generationFactor(this.sockets) + this.energyCarry;
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
                EnergyHandlerUtil.move(this.energy, target,
                        (int) Math.round(maxPerTarget * com.mistech.mistechmod.machine.socket.MachineSockets.rateFactor(this.sockets)), null);
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
        this.wearRuneAt(slot);
    }

    /** Gasta 1 uso da runa no espaço (a selada no Santuário não gasta; a última some). */
    protected void wearRuneAt(int slot) {
        ItemStack rune = this.items.get(slot);
        if (rune.isEmpty() || com.mistech.mistechmod.purity.TaintEvents.isSealed(rune)) {
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

    // ----- engastes -----

    /** A runa está engastada e faz efeito nesta máquina? */
    public boolean socketActive(com.mistech.mistechmod.item.RuneElement element) {
        return this.sockets.contains(element);
    }

    /** Runas engastadas (com repetição e as que não valem aqui), na ordem dos espaços. */
    public java.util.List<com.mistech.mistechmod.item.RuneElement> socketed() {
        java.util.List<com.mistech.mistechmod.item.RuneElement> list = new java.util.ArrayList<>();
        for (int i = this.layout.firstSocket; i < this.layout.size(); i++) {
            com.mistech.mistechmod.item.RuneElement e = com.mistech.mistechmod.item.RuneElement.ofRune(this.items.get(i));
            if (e != null) {
                list.add(e);
            }
        }
        return list;
    }

    private void refreshSockets() {
        this.sockets.clear();
        if (this.layout.socketCount() > 0) {
            this.sockets.addAll(com.mistech.mistechmod.machine.socket.MachineSockets.active(this.socketed(), this.layout.sockets));
        }
        this.energy.setRate(com.mistech.mistechmod.machine.socket.MachineSockets.rateFactor(this.sockets));
    }

    // ----- Núcleo Celeste (Fase 9): os engastes param de se gastar -----
    private boolean celestialCore;

    public boolean hasCelestialCore() {
        return this.celestialCore;
    }

    /** Instala o Núcleo Celeste. @return false se a máquina já tem um ou não tem engastes */
    public boolean installCelestialCore() {
        if (this.celestialCore || (this.layout.socketCount() == 0 && !this.acceptsCelestialCoreWithoutSockets())) {
            return false;
        }
        this.celestialCore = true;
        this.setChanged();
        return true;
    }

    /** Máquinas sem engastes que ainda assim usam o Núcleo Celeste (o Prisma Astral faz 6 pós). */
    protected boolean acceptsCelestialCoreWithoutSockets() {
        return false;
    }

    // ----- céu e dono (Fase 6) -----
    /** Quem colocou a máquina (ou null em máquinas antigas). */
    private java.util.@Nullable UUID owner;
    /** Constelações completas do dono (bit por constelação), atualizadas enquanto ele está online. */
    protected int ownerMask;
    private double skySpeed = 1.0, skyGeneration = 1.0, skyTaint = 1.0;

    public void setOwner(java.util.UUID owner) {
        this.owner = owner;
        this.setChanged();
    }

    public java.util.@Nullable UUID owner() {
        return this.owner;
    }

    public int ownerMask() {
        return this.ownerMask;
    }

    public String layoutName() {
        return this.layout.name;
    }

    public com.mistech.mistechmod.machine.socket.MachineSockets.Category socketCategory() {
        return this.layout.sockets.category();
    }

    /** "+25%" / "−10%". */
    private static String percent(double factor) {
        long p = Math.round((factor - 1.0) * 100);
        return (p >= 0 ? "+" : "−") + Math.abs(p) + "%";
    }

    /** A cada segundo: constelações do dono (se ele estiver online) e os multiplicadores do céu. */
    private void refreshSky(ServerLevel level) {
        if (this.owner != null) {
            net.minecraft.world.entity.player.Player player = level.getServer().getPlayerList().getPlayer(this.owner);
            if (player != null) {
                int mask = com.mistech.mistechmod.celestial.MachineSky.completeMask(player);
                if (mask != this.ownerMask) {
                    this.ownerMask = mask;
                    this.setChanged();
                }
            }
        }
        this.skySpeed = com.mistech.mistechmod.celestial.MachineSky.speed(this, level, this.ownerMask);
        this.skyGeneration = com.mistech.mistechmod.celestial.MachineSky.generation(this, level, this.ownerMask);
        this.skyTaint = com.mistech.mistechmod.celestial.MachineSky.taint(this, level, this.ownerMask);
    }

    /** Velocidade pela Mácula do lugar (0,8 num chunk Profano). Atualizada a cada segundo. */
    private double areaSpeed = 1.0;

    /**
     * Mácula do lugar que a máquina solta por minuto enquanto está acesa (LIT). Negativo = limpa o próprio chunk.
     * Padrão 0; as máquinas impuras sobrescrevem com o valor do config.
     */
    protected float taintPerMinute() {
        return 0.0F;
    }

    private void emitTaint(ServerLevel level, BlockPos pos) {
        float perMinute = this.taintPerMinute();
        if (perMinute == 0) {
            return;
        }
        BlockState now = this.getBlockState();
        if (now.hasProperty(MachineBlock.LIT) && now.getValue(MachineBlock.LIT)) {
            float amount = perMinute > 0 ? (float) (perMinute * this.taintFactor() * this.skyTaint) : perMinute;
            com.mistech.mistechmod.purity.area.AreaTaint.add(level, pos, amount / 1200.0F);
        }
    }

    /** Multiplicador da Mácula que a máquina solta (Fase 4). */
    public double taintFactor() {
        return com.mistech.mistechmod.machine.socket.MachineSockets.taintFactor(this.sockets);
    }

    /** Desgaste, Vigília e o Ar puxando itens do chão. Depois do tick da máquina. */
    private void socketTick(ServerLevel level, BlockPos pos) {
        if (this.layout.socketCount() == 0) {
            return;
        }
        BlockState state = this.getBlockState();
        boolean lit = state.hasProperty(MachineBlock.LIT) && state.getValue(MachineBlock.LIT);
        if (lit && !this.celestialCore && !this.socketed().isEmpty() && ++this.socketWork >= com.mistech.mistechmod.Config.socketWearTicks()) {
            this.socketWork = 0;
            for (int i = this.layout.firstSocket; i < this.layout.size(); i++) {
                this.wearRuneAt(i);
            }
        }
        this.vigilTick(level, pos, lit);
        if (this.socketActive(com.mistech.mistechmod.item.RuneElement.AIR) && this.inputFaces.length > 0 && ++this.vacuumTimer >= 20) {
            this.vacuumTimer = 0;
            this.vacuum(level, pos);
        }
    }

    private void vigilTick(ServerLevel level, BlockPos pos, boolean working) {
        int signal = -1;
        if (this.socketActive(com.mistech.mistechmod.item.RuneElement.VIGIL)) {
            this.idleTicks = working ? 0 : Math.min(1000, this.idleTicks + 1);
            signal = this.idleTicks > 40 ? 15 : 0;
        }
        if (signal != this.vigilSignal) {
            this.vigilSignal = signal;
            level.updateNeighbourForOutputSignal(pos, this.getBlockState().getBlock());
        }
    }

    /** Sinal do comparador da Runa da Vigília (15 = parada há mais de 2 s), ou -1 sem a runa. */
    public int vigilSignal() {
        return this.vigilSignal;
    }

    /** Runa de Ar: recolhe itens soltos num raio de 2 para os espaços de entrada. */
    private void vacuum(ServerLevel level, BlockPos pos) {
        for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(2.0), e -> e.isAlive() && !e.getItem().isEmpty())) {
            ItemStack stack = item.getItem().copy();
            for (int slot : this.inputFaces) {
                if (stack.isEmpty()) {
                    break;
                }
                if (!this.canPlaceItem(slot, stack)) {
                    continue;
                }
                ItemStack there = this.items.get(slot);
                if (there.isEmpty()) {
                    this.items.set(slot, stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
                } else if (ItemStack.isSameItemSameComponents(there, stack)) {
                    int move = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
                    there.grow(move);
                    stack.shrink(move);
                }
            }
            if (stack.getCount() != item.getItem().getCount()) {
                if (stack.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(stack);
                }
                this.setChanged();
            }
        }
    }

    /** Quantos produtos extras sair desta vez (Terra, Eco, Sorte). */
    protected int bonusCount(net.minecraft.util.RandomSource random) {
        return com.mistech.mistechmod.machine.socket.MachineSockets.bonusRolls(this.sockets, random,
                this.level == null ? 1.0F : com.mistech.mistechmod.celestial.MachineSky.luck(this.level, this.ownerMask),
                this.level == null ? 1.0F : com.mistech.mistechmod.celestial.MachineSky.echo(this.level));
    }

    /** Multiplicador da experiência das receitas (Runa da Alma: 2×). */
    protected float xpMultiplier() {
        return this.socketActive(com.mistech.mistechmod.item.RuneElement.SOUL) ? 2.0F : 1.0F;
    }

    /** Código do canal Extraplanar da Runa do Espaço engastada (o nome dado na bigorna), ou null. */
    private @Nullable String spaceChannel() {
        if (!this.socketActive(com.mistech.mistechmod.item.RuneElement.SPACE)) {
            return null;
        }
        for (int i = this.layout.firstSocket; i < this.layout.size(); i++) {
            ItemStack rune = this.items.get(i);
            if (com.mistech.mistechmod.item.RuneElement.ofRune(rune) == com.mistech.mistechmod.item.RuneElement.SPACE) {
                Component name = rune.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
                if (name != null && !name.getString().isBlank()) {
                    String code = name.getString().trim();
                    return code.length() > com.mistech.mistechmod.extraplanar.ExtraplanarData.MAX_CODE_LENGTH
                            ? code.substring(0, com.mistech.mistechmod.extraplanar.ExtraplanarData.MAX_CODE_LENGTH) : code;
                }
            }
        }
        return null;
    }

    /** Põe no canal Extraplanar; devolve o que não coube. */
    private ItemStack sendToChannel(ServerLevel level, String code, ItemStack stack) {
        com.mistech.mistechmod.extraplanar.ExtraplanarData data = com.mistech.mistechmod.extraplanar.ExtraplanarData.get(level.getServer());
        NonNullList<ItemStack> slots = data.items(code);
        ItemStack rest = stack.copy();
        for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++) {
            for (int i = 0; i < slots.size() && !rest.isEmpty(); i++) {
                ItemStack there = slots.get(i);
                if (pass == 0 && !there.isEmpty() && ItemStack.isSameItemSameComponents(there, rest)) {
                    int move = Math.min(rest.getCount(), there.getMaxStackSize() - there.getCount());
                    there.grow(move);
                    rest.shrink(move);
                } else if (pass == 1 && there.isEmpty()) {
                    slots.set(i, rest.split(Math.min(rest.getCount(), rest.getMaxStackSize())));
                }
            }
        }
        if (rest.getCount() != stack.getCount()) {
            data.setDirty();
        }
        return rest;
    }

    /** Saída cheia e sem Vazio/Espaço para escoar (máquinas de área param). */
    protected boolean outputBlocked() {
        return this.outputFull() && !this.socketActive(com.mistech.mistechmod.item.RuneElement.VOID) && this.spaceChannel() == null;
    }

    /** Cabe o produto? Com Vazio (destrói o excedente) ou Espaço com canal, sempre cabe. */
    protected boolean canOutputProduct(ItemStack stack) {
        return this.socketActive(com.mistech.mistechmod.item.RuneElement.VOID) || this.spaceChannel() != null || this.canOutput(stack);
    }

    /**
     * Saída de um PRODUTO (não de recipientes vazios): Fogo funde, Terra/Eco/Sorte dão extra, Espaço manda para o
     * canal Extraplanar, Vazio destrói o que não couber. Devolve o que sobrou.
     */
    protected ItemStack insertProduct(ServerLevel level, ItemStack product) {
        if (product.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack p = product.copy();
        if (this.socketActive(com.mistech.mistechmod.item.RuneElement.FIRE)) {
            var smelted = com.mistech.mistechmod.machine.SmeltingLookup.find(level, p.copyWithCount(1));
            if (smelted.isPresent() && !smelted.get().output().isEmpty()) {
                p = smelted.get().output().copyWithCount(Math.min(smelted.get().output().getMaxStackSize(),
                        smelted.get().output().getCount() * p.getCount()));
            }
        }
        p.grow(this.bonusCount(level.getRandom()));
        String channel = this.spaceChannel();
        if (channel != null) {
            p = this.sendToChannel(level, channel, p);
        }
        ItemStack rest = p.isEmpty() ? ItemStack.EMPTY : this.insertOutput(p);
        if (!rest.isEmpty() && this.socketActive(com.mistech.mistechmod.item.RuneElement.VOID)) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, this.worldPosition.getX() + 0.5,
                    this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.02);
            return ItemStack.EMPTY;
        }
        if (!rest.isEmpty()) {
            // o extra dos engastes não coube: sai em cima da máquina em vez de sumir
            net.minecraft.world.Containers.dropItemStack(level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0,
                    this.worldPosition.getZ() + 0.5, rest);
        }
        return ItemStack.EMPTY;
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
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return this.layout.kind(slot) == MachineLayout.Kind.OUTPUT && (direction == null || this.side(direction).out());
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
        if (packedSides != null && packedSides.length == 6 && !isOldDefault(packedSides)) {
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
        this.owner = input.getString("Owner").map(s -> {
            try {
                return java.util.UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).orElse(null);
        this.ownerMask = input.getIntOr("OwnerSky", 0);
        this.celestialCore = input.getBooleanOr("CelestialCore", false);
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
        if (this.celestialCore) {
            output.putBoolean("CelestialCore", true);
        }
        if (this.owner != null) {
            output.putString("Owner", this.owner.toString());
            output.putInt("OwnerSky", this.ownerMask);
        }
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
