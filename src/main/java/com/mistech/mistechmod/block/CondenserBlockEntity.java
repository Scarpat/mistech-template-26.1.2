package com.mistech.mistechmod.block;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.menu.CondenserMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Condensador. Slots: 0 = entrada (gelo para melhorar), 1 = saída.
 * Precisa de energia (recebe FE de geradores/cabos de qualquer mod) e de fluido encostado:
 *  - água + entrada vazia      -> gelo
 *  - água + gelo na entrada    -> gelo compacto
 *  - água + gelo compacto      -> gelo azul
 *  - lava                      -> obsidiana (quando não há gelo para melhorar)
 * Lento: {@link #WORK_TICKS} por item, gastando {@link #ENERGY_PER_TICK} FE/tick.
 * A água/lava do mundo NÃO é consumida.
 */
public class CondenserBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int CAPACITY = 10_000;
    public static final int MAX_INPUT_PER_TICK = 100;
    public static final int ENERGY_PER_TICK = 10;
    public static final int WORK_TICKS = 200; // 10 s por item

    private static final int[] SLOTS_UP_AND_SIDES = {SLOT_INPUT};
    private static final int[] SLOTS_DOWN = {SLOT_OUTPUT};
    private static final Component DEFAULT_NAME = Component.translatable("container.mistech.condenser");

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int progress;

    /** Buffer de energia: só aceita entrada. */
    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(CAPACITY, MAX_INPUT_PER_TICK, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            CondenserBlockEntity.this.setChanged();
        }
    };

    /** 0 = progresso, 1 = energia (sincronizados com a tela). */
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int id) {
            return id == 0 ? CondenserBlockEntity.this.progress : CondenserBlockEntity.this.energy.getAmountAsInt();
        }

        @Override
        public void set(int id, int value) {
            if (id == 0) {
                CondenserBlockEntity.this.progress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public CondenserBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.CONDENSER.get(), pos, state);
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CondenserBlockEntity condenser) {
        Item product = condenser.currentProduct(level, pos);
        boolean working = product != null
                && condenser.canOutput(product)
                && condenser.energy.getAmountAsInt() >= ENERGY_PER_TICK;

        if (working) {
            condenser.energy.set(condenser.energy.getAmountAsInt() - ENERGY_PER_TICK);
            condenser.progress++;
            if (com.mistech.mistechmod.machine.GnomeBoost.extraStep(level, pos)
                    && condenser.energy.getAmountAsInt() >= ENERGY_PER_TICK) {
                condenser.energy.set(condenser.energy.getAmountAsInt() - ENERGY_PER_TICK);
                condenser.progress++;
            }
            if (condenser.progress >= WORK_TICKS) {
                condenser.progress = 0;
                condenser.finish(product);
                level.playSound(null, pos, product == Items.OBSIDIAN ? SoundEvents.LAVA_EXTINGUISH : SoundEvents.GLASS_PLACE,
                        SoundSource.BLOCKS, 0.6F, 1.2F);
            }
            setChanged(level, pos, state);
        } else if (condenser.progress > 0 && product == null) {
            condenser.progress = 0; // perdeu o fluido: recomeça
            setChanged(level, pos, state);
        }

        if (state.getValue(CondenserBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(CondenserBlock.LIT, working), Block.UPDATE_ALL);
        }
    }

    /** O que o condensador vai produzir agora, ou null se não tem fluido adequado. */
    private @Nullable Item currentProduct(Level level, BlockPos pos) {
        boolean water = false;
        boolean lava = false;
        for (Direction dir : Direction.values()) {
            FluidState fluid = level.getFluidState(pos.relative(dir));
            if (fluid.is(FluidTags.WATER)) {
                water = true;
            } else if (fluid.is(FluidTags.LAVA)) {
                lava = true;
            }
        }
        ItemStack input = this.items.get(SLOT_INPUT);
        if (water) {
            if (input.is(Items.ICE)) {
                return Items.PACKED_ICE;
            }
            if (input.is(Items.PACKED_ICE)) {
                return Items.BLUE_ICE;
            }
        }
        if (lava) {
            return Items.OBSIDIAN;
        }
        if (water && input.isEmpty()) {
            return Items.ICE;
        }
        return null;
    }

    private boolean canOutput(Item product) {
        ItemStack output = this.items.get(SLOT_OUTPUT);
        return output.isEmpty() || (output.is(product) && output.getCount() < output.getMaxStackSize());
    }

    private void finish(Item product) {
        ItemStack input = this.items.get(SLOT_INPUT);
        if ((product == Items.PACKED_ICE && input.is(Items.ICE)) || (product == Items.BLUE_ICE && input.is(Items.PACKED_ICE))) {
            input.shrink(1);
        }
        ItemStack output = this.items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            this.items.set(SLOT_OUTPUT, new ItemStack(product));
        } else {
            output.grow(1);
        }
    }

    // ----- Container -----

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    public static boolean isUpgradeable(ItemStack stack) {
        return stack.is(Items.ICE) || stack.is(Items.PACKED_ICE);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && isUpgradeable(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return direction == Direction.DOWN ? SLOTS_DOWN : SLOTS_UP_AND_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.progress = input.getIntOr("Progress", 0);
        this.energy.deserialize(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("Progress", this.progress);
        this.energy.serialize(output);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CondenserMenu(containerId, inventory, this, this.dataAccess);
    }
}
