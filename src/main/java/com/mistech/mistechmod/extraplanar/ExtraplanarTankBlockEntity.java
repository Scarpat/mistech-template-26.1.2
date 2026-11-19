package com.mistech.mistechmod.extraplanar;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * Tanque Extraplanar: o fluido fica no canal do código ({@link ExtraplanarData}); todos os tanques com o mesmo
 * código têm o mesmo nível. O bloco guarda só uma cópia para o cliente desenhar o líquido.
 */
public class ExtraplanarTankBlockEntity extends BlockEntity implements CodedBlockEntity {
    private String code = "";
    private FluidStack view = FluidStack.EMPTY;
    private ExtraplanarData.@Nullable FluidChannel channel;

    public ExtraplanarTankBlockEntity(BlockPos pos, BlockState state) {
        super(ExtraplanarContent.TANK_BE.get(), pos, state);
    }

    @Override
    public String getCode() {
        return this.code;
    }

    /** O tanque do canal (só no servidor e com código); {@code null} se o bloco não está ligado. */
    public @Nullable FluidTank linkedTank() {
        this.attach();
        return this.channel == null ? null : this.channel.tank;
    }

    public FluidStack getView() {
        return this.view;
    }

    public float getFill() {
        return this.view.isEmpty() ? 0 : this.view.getAmount() / (float) ExtraplanarData.TANK_CAPACITY;
    }

    /** 0..15 para o comparador. */
    public int getComparatorLevel() {
        FluidTank tank = this.linkedTank();
        if (tank == null || tank.isEmpty()) {
            return 0;
        }
        return 1 + (int) (14L * tank.getAmount() / ExtraplanarData.TANK_CAPACITY);
    }

    private void attach() {
        if (this.channel != null || this.code.isEmpty() || !(this.level instanceof ServerLevel server) || this.isRemoved()) {
            return;
        }
        this.channel = ExtraplanarData.get(server.getServer()).fluid(this.code);
        this.channel.addListener(this);
        this.view = this.channel.tank.getFluid().copy();
    }

    private void detach() {
        if (this.channel != null) {
            this.channel.removeListener(this);
            this.channel = null;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.attach();
    }

    @Override
    public void setRemoved() {
        this.detach();
        super.setRemoved();
    }

    @Override
    public void setCode(String newCode) {
        String clean = CodedBlockEntity.clean(newCode);
        if (clean.equals(this.code)) {
            return;
        }
        this.detach();
        this.code = clean;
        this.view = FluidStack.EMPTY;
        this.attach();
        this.setChanged();
        this.invalidateCapabilities();
        this.pushToClients();
    }

    /** Chamado pelo canal quando o fluido muda. */
    void syncFromChannel() {
        if (this.channel == null) {
            return;
        }
        FluidStack now = this.channel.tank.getFluid();
        if (FluidStack.matches(now, this.view)) {
            return;
        }
        this.view = now.copy();
        this.setChanged();
        this.pushToClients();
    }

    private void pushToClients() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
            this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.code = CodedBlockEntity.clean(input.getStringOr("Code", ""));
        this.view = input.read("View", SimpleFluidContent.CODEC).map(SimpleFluidContent::copy).orElse(FluidStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Code", this.code);
        if (!this.view.isEmpty()) {
            output.store("View", SimpleFluidContent.CODEC, SimpleFluidContent.copyOf(this.view));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        String stored = components.get(ExtraplanarContent.CODE.get());
        if (stored != null) {
            // vem do item na hora de colocar: liga ao canal e mostra o nível já no cliente
            this.detach();
            this.code = CodedBlockEntity.clean(stored);
            this.attach();
            this.invalidateCapabilities();
            this.pushToClients();
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!this.code.isEmpty()) {
            components.set(ExtraplanarContent.CODE.get(), this.code);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Code");
        output.discard("View");
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
