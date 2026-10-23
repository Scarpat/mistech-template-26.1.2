package com.mistech.mistechmod.spellcraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/** Guarda um Cajado e converte FE em mana na reserva dele: {@value #FE_PER_MANA} FE por ponto, até {@value #MANA_PER_TICK} por tick. */
public class ArcaneChargerBlockEntity extends BlockEntity {
    public static final int CAPACITY = 20_000;
    public static final int MAX_INPUT_PER_TICK = 200;
    public static final int FE_PER_MANA = 20;
    public static final int MANA_PER_TICK = 2;

    private ItemStack staff = ItemStack.EMPTY;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(CAPACITY, MAX_INPUT_PER_TICK, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            ArcaneChargerBlockEntity.this.setChanged();
        }
    };

    public ArcaneChargerBlockEntity(BlockPos pos, BlockState state) {
        super(SpellcraftContent.ARCANE_CHARGER_BE.get(), pos, state);
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public ItemStack getStaff() {
        return this.staff;
    }

    public void setStaff(ItemStack stack) {
        this.staff = stack;
        setChanged();
        sync();
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public ItemStack takeStaff() {
        ItemStack out = this.staff;
        this.staff = ItemStack.EMPTY;
        setChanged();
        sync();
        return out;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcaneChargerBlockEntity charger) {
        if (charger.staff.isEmpty()) {
            return;
        }
        int space = ManaService.reserveMax(charger.staff) - ManaService.reserveOf(charger.staff);
        int stored = charger.energy.getAmountAsInt();
        int mana = Math.min(MANA_PER_TICK, Math.min(space, stored / FE_PER_MANA));
        if (mana > 0) {
            ManaService.addReserve(charger.staff, mana);
            charger.energy.set(stored - mana * FE_PER_MANA);
            charger.setChanged();
            if (level instanceof net.minecraft.server.level.ServerLevel server && level.getGameTime() % 6 == 0) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                        2, 0.25, 0.15, 0.25, 0.4);
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.staff.isEmpty()) {
            Containers.dropItemStack(this.level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, this.staff);
            this.staff = ItemStack.EMPTY;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.staff = input.read("Staff", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.energy.deserialize(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.staff.isEmpty()) {
            output.store("Staff", ItemStack.OPTIONAL_CODEC, this.staff);
        }
        this.energy.serialize(output);
    }
}
