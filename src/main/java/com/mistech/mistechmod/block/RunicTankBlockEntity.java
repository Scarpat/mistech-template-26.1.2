package com.mistech.mistechmod.block;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;

/** Tanque Rúnico: guarda 100 baldes de um fluido. Ao quebrar, o fluido vai junto no item. */
public class RunicTankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 100_000; // 100 baldes

    private final FluidTank tank = new FluidTank(CAPACITY, this::onTankChanged);

    public RunicTankBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.RUNIC_TANK.get(), pos, state);
    }

    public FluidTank getTank() {
        return this.tank;
    }

    private void onTankChanged() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** 0..15 para o comparador. */
    public int getComparatorLevel() {
        if (this.tank.isEmpty()) {
            return 0;
        }
        return 1 + (int) (14L * this.tank.getAmount() / CAPACITY);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.serialize(output);
    }

    // o fluido acompanha o item quando o bloco é quebrado (loot table: copy_components mistech:tank_fluid)
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        net.neoforged.neoforge.fluids.SimpleFluidContent stored = components.get(MistechComponents.TANK_FLUID.get());
        this.tank.setFluid(stored != null ? stored.copy() : FluidStack.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!this.tank.isEmpty()) {
            components.set(MistechComponents.TANK_FLUID.get(), net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(this.tank.getFluid()));
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("stacks");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
