package com.mistech.mistechmod.block;

import com.mistech.mistechmod.machine.GnomeBoost;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Gnomo Engarrafado: guarda quanto tempo de comida o gnomo ainda tem.
 * Maçã = 5 min, maçã dourada = 30 min, máximo 1 h. Enquanto tem comida, dá Impulso de Gnomo às máquinas
 * num cubo de raio 3 (registrado em {@link GnomeBoost}).
 * É um Container de 1 espaço só para funis colocarem maçãs (o gnomo come na hora; nada fica guardado).
 */
public class BottledGnomeBlockEntity extends BlockEntity implements Container {
    public static final int APPLE_TICKS = 5 * 60 * 20;
    public static final int GOLDEN_APPLE_TICKS = 30 * 60 * 20;
    public static final int MAX_TICKS = 60 * 60 * 20;

    private int fedTicks;
    private boolean registered;

    public BottledGnomeBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.BOTTLED_GNOME.get(), pos, state);
    }

    /** Quanto tempo (ticks) a comida dá; 0 se o gnomo não come isso. */
    public static int foodTicks(ItemStack stack) {
        if (stack.is(Items.APPLE)) {
            return APPLE_TICKS;
        }
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return GOLDEN_APPLE_TICKS;
        }
        return 0;
    }

    /** Cabe mais essa comida? (maçã dourada entra se ainda não estiver cheio.) */
    public boolean canEat(ItemStack stack) {
        int ticks = foodTicks(stack);
        if (ticks <= 0) {
            return false;
        }
        return ticks >= GOLDEN_APPLE_TICKS ? this.fedTicks < MAX_TICKS : this.fedTicks + ticks <= MAX_TICKS;
    }

    public void feed(ItemStack stack) {
        this.fedTicks = Math.min(MAX_TICKS, this.fedTicks + foodTicks(stack));
        this.setChanged();
        if (this.level instanceof ServerLevel server) {
            BlockPos pos = this.worldPosition;
            server.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.BLOCKS, 0.7F, 1.6F);
            server.playSound(null, pos, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 0.6F, 1.8F);
            server.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.0);
            this.updateState();
        }
    }

    public Component statusMessage() {
        if (this.fedTicks <= 0) {
            return Component.translatable("message.mistech.bottled_gnome.hungry");
        }
        int minutes = (this.fedTicks + 1199) / 1200;
        return Component.translatable("message.mistech.bottled_gnome.fed", minutes);
    }

    public int comparatorSignal() {
        return this.fedTicks <= 0 ? 0 : 1 + Math.round(14.0F * this.fedTicks / MAX_TICKS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BottledGnomeBlockEntity gnome) {
        if (gnome.fedTicks > 0) {
            gnome.fedTicks--;
            if (gnome.fedTicks % 200 == 0) {
                gnome.setChanged();
            }
            if (level.getGameTime() % 40 == 0 && level instanceof ServerLevel server) {
                sparkToMachines(server, pos);
            }
        }
        gnome.updateState();
    }

    /** Brilhos dourados saindo do pote em direção às máquinas impulsionadas. */
    private static void sparkToMachines(ServerLevel level, BlockPos pos) {
        int r = GnomeBoost.RADIUS;
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
            BlockEntity be = level.getBlockEntity(target);
            if (be instanceof MachineBlockEntity || be instanceof RunicFurnaceBlockEntity || be instanceof CondenserBlockEntity
                    || be instanceof EssenceExtractorBlockEntity || be instanceof MagicCompressorBlockEntity) {
                double dx = target.getX() - pos.getX(), dy = target.getY() - pos.getY(), dz = target.getZ() - pos.getZ();
                // count 0 = a partícula anda na direção dada
                level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                        0, dx, dy, dz, 0.12);
            }
        }
    }

    /** Liga/desliga o impulso e a aparência (FED) conforme a comida. */
    private void updateState() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        boolean fed = this.fedTicks > 0;
        if (fed != this.registered) {
            GnomeBoost.setActive(this.level, this.worldPosition, fed);
            this.registered = fed;
        }
        BlockState state = this.getBlockState();
        if (state.hasProperty(BottledGnomeBlock.FED) && state.getValue(BottledGnomeBlock.FED) != fed) {
            this.level.setBlock(this.worldPosition, state.setValue(BottledGnomeBlock.FED, fed), Block.UPDATE_ALL);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.registered = false;
        this.updateState();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            GnomeBoost.setActive(this.level, this.worldPosition, false);
            this.registered = false;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.fedTicks = input.getIntOr("FedTicks", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("FedTicks", this.fedTicks);
    }

    // ----- Container: funil coloca maçã e o gnomo come -----

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        for (int i = 0; i < stack.getCount() && this.canEat(stack); i++) {
            this.feed(stack);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return this.canEat(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
    }
}
