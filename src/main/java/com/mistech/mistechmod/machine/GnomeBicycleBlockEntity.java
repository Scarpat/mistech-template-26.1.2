package com.mistech.mistechmod.machine;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;

/**
 * Bicicleta do Gnomo: um gnomo montado pedala e gera {@link #RATE} FE/t enquanto tiver comida.
 * Maçã = 1 min de pedalada, maçã dourada = 10 min. Sem gnomo ou sem comida, não gera nada.
 * Com o buffer cheio o gnomo descansa (não gasta comida).
 * Espaço de bateria (tipo RUNE no layout): carrega qualquer item com energia (ex.: Cajado do Gnomo).
 * Buffer 20.000 FE; envia até 200 FE/t para cada bloco encostado. Sem Impulso de Gnomo.
 */
public class GnomeBicycleBlockEntity extends MachineBlockEntity {
    public static final int RATE = 20;
    public static final int MAX_OUTPUT_PER_TICK = 200;
    public static final int APPLE_TICKS = 60 * 20;
    public static final int GOLDEN_APPLE_TICKS = 10 * 60 * 20;
    public static final int SLOT_FOOD = 0;
    public static final int SLOT_BATTERY = 1;

    /** Situação mostrada na tela (info1). */
    public static final int STATUS_NO_GNOME = 0, STATUS_HUNGRY = 1, STATUS_PEDALING = 2, STATUS_RESTING = 3;

    /** Ticks de pedalada que o gnomo ainda tem da comida que já comeu. */
    private int fedTicks;

    public GnomeBicycleBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.GNOME_BICYCLE.get(), pos, state, MachineLayouts.GNOME_BICYCLE, 0, MAX_OUTPUT_PER_TICK);
    }

    /** Quanto tempo de pedalada a comida dá (0 = o gnomo não come isso). */
    public static int foodTicks(ItemStack stack) {
        if (stack.is(Items.APPLE)) {
            return APPLE_TICKS;
        }
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return GOLDEN_APPLE_TICKS;
        }
        return 0;
    }

    /** Capability de energia do item (bateria de qualquer mod), ou null se não for bateria. */
    public static @Nullable EnergyHandler batteryHandler(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forStack(stack));
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean hasGnome = state.hasProperty(GnomeBicycleBlock.GNOME) && state.getValue(GnomeBicycleBlock.GNOME);
        int space = this.layout.capacity - this.storedEnergy();

        // o gnomo come quando acaba a comida (e só se houver onde guardar a energia)
        if (hasGnome && this.fedTicks <= 0 && space > 0) {
            ItemStack food = this.items.get(SLOT_FOOD);
            int ticks = foodTicks(food);
            if (ticks > 0) {
                this.fedTicks = ticks;
                this.maxProgress = ticks;
                food.shrink(1);
                this.setChanged();
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GENERIC_EAT.value(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.6F);
            }
        }

        int generated = 0;
        if (hasGnome && this.fedTicks > 0 && space > 0) {
            generated = Math.min(RATE, space);
            this.addEnergy(generated);
            this.fedTicks--;
            if (this.fedTicks % 200 == 0) {
                this.setChanged();
            }
        }
        this.progress = this.fedTicks;
        this.info0 = generated;
        if (!hasGnome) {
            this.info1 = STATUS_NO_GNOME;
        } else if (generated > 0) {
            this.info1 = STATUS_PEDALING;
        } else if (this.fedTicks > 0 && space <= 0) {
            this.info1 = STATUS_RESTING;
        } else {
            this.info1 = STATUS_HUNGRY;
        }

        // carrega a bateria do espaço
        EnergyHandler battery = batteryHandler(this.items.get(SLOT_BATTERY));
        if (battery != null && EnergyHandlerUtil.move(this.energy, battery, MAX_OUTPUT_PER_TICK, null) > 0) {
            this.setChanged();
        }

        this.pushEnergy(level, pos, MAX_OUTPUT_PER_TICK);
        this.setLit(level, pos, state, generated > 0);
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.fedTicks = input.getIntOr("FedTicks", 0);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("FedTicks", this.fedTicks);
    }
}
