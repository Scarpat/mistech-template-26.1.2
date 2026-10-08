package com.mistech.mistechmod.machine;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.LimitingEnergyHandler;

/**
 * Bateria Rúnica: guarda 200.000 FE. Recebe energia pelas 5 faces de trás/lados/cima/baixo e entrega pela
 * <b>frente</b> (FACING) até 1.000 FE/t — assim dá para encadear baterias sem a energia ficar indo e voltando.
 * Espaço de carga: carrega qualquer item com energia (Cajado do Gnomo, baterias de outros mods).
 * A energia acompanha o item ao quebrar (componente {@code mistech:battery_energy}).
 *
 * Tela: info0 = FE/t entrando, info1 = FE/t saindo.
 */
public class RunicBatteryBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 200_000;
    public static final int MAX_TRANSFER = 1_000;
    public static final int SLOT_CHARGE = 0;

    private final EnergyHandler inputOnly;
    private final EnergyHandler outputOnly;
    /** Energia no fim do tick anterior (para medir quanto entrou). */
    private int lastAmount = -1;
    /** Energia puxada ativamente neste tick (Bateria Aprimorada). */
    protected int pulledThisTick;

    public RunicBatteryBlockEntity(BlockPos pos, BlockState state) {
        this(MistechBlockEntities.RUNIC_BATTERY.get(), pos, state, MachineLayouts.RUNIC_BATTERY, MAX_TRANSFER, MAX_TRANSFER);
    }

    protected RunicBatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineLayout layout,
            int maxInput, int maxOutput) {
        super(type, pos, state, layout, maxInput, maxOutput);
        this.inputOnly = new LimitingEnergyHandler(this.energy, maxInput, 0);
        this.outputOnly = new LimitingEnergyHandler(this.energy, 0, maxOutput);
    }

    /** Capability por face: frente só tira, as outras só recebem; sem face = tudo. */
    public EnergyHandler handlerFor(@Nullable Direction side) {
        if (side == null) {
            return this.energy;
        }
        BlockState state = this.getBlockState();
        Direction front = state.hasProperty(MachineBlock.FACING) ? state.getValue(MachineBlock.FACING) : Direction.NORTH;
        return side == front ? this.outputOnly : this.inputOnly;
    }

    public int capacity() {
        return this.layout.capacity;
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        int now = this.storedEnergy();
        int in = this.lastAmount < 0 ? 0 : Math.max(0, now - this.lastAmount);
        this.pulledThisTick = 0;

        int out = this.distribute(level, pos, state);
        out += this.chargeItem();

        this.info0 = Math.min(Short.MAX_VALUE, in + this.pulledThisTick);
        this.info1 = Math.min(Short.MAX_VALUE, out);
        this.lastAmount = this.storedEnergy();
        this.setLit(level, pos, state, this.lastAmount > 0);
    }

    /** Entrega a energia; devolve quanto saiu. Básica: só para o bloco da frente. */
    protected int distribute(ServerLevel level, BlockPos pos, BlockState state) {
        if (this.storedEnergy() <= 0) {
            return 0;
        }
        Direction front = state.getValue(MachineBlock.FACING);
        EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, pos.relative(front), front.getOpposite());
        if (target == null || target == this.energy) {
            return 0;
        }
        return EnergyHandlerUtil.move(this.energy, target, MAX_TRANSFER, null);
    }

    /** Carrega o item do espaço de carga. */
    protected int chargeItem() {
        if (this.storedEnergy() <= 0) {
            return 0;
        }
        EnergyHandler item = GnomeBicycleBlockEntity.batteryHandler(this.items.get(SLOT_CHARGE));
        if (item == null) {
            return 0;
        }
        int moved = EnergyHandlerUtil.move(this.energy, item, MAX_TRANSFER, null);
        if (moved > 0) {
            this.setChanged();
        }
        return moved;
    }

    // ----- a energia acompanha o item (loot table: copy_components mistech:battery_energy) -----

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer stored = components.get(MistechComponents.BATTERY_ENERGY.get());
        this.energy.set(stored == null ? 0 : Math.max(0, Math.min(this.layout.capacity, stored)));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.storedEnergy() > 0) {
            components.set(MistechComponents.BATTERY_ENERGY.get(), this.storedEnergy());
        }
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.lastAmount = -1;
    }

    @Override
    protected void saveExtra(ValueOutput output) {}
}
