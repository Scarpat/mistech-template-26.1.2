package com.mistech.mistechmod.husbandry;

import com.mistech.mistechmod.machine.GnomeBoost;
import com.mistech.mistechmod.registry.GnomeContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
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
 * Vaso de Cultivo: guarda 1 planta e o que já brotou (até 1 pack).
 * Tempo por cópia: cana 45 s, flores 60 s, cogumelos 90 s, Cogumelo Amarelado 2 min.
 * Água encostada (lados ou embaixo) = 2x mais rápido; Gnomo Engarrafado alimentado por perto = +50%.
 * Container de 2 espaços: 0 = o que brotou (sai por baixo), 1 = a planta (entra por cima/lados, só se vazio).
 */
public class GrowingPotBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int OUTPUT = 0;
    public static final int PLANT = 1;
    private static final int[] SLOTS_DOWN = {OUTPUT};
    private static final int[] SLOTS_OTHER = {PLANT};

    private ItemStack plant = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;

    public GrowingPotBlockEntity(BlockPos pos, BlockState state) {
        super(HusbandryContent.GROWING_POT_BE.get(), pos, state);
    }

    public static boolean isPlant(ItemStack stack) {
        return !stack.isEmpty() && stack.is(HusbandryContent.POT_PLANTS);
    }

    /** Ticks para brotar uma cópia desta planta. */
    public static int cycleTicks(ItemStack plant) {
        if (plant.is(Items.SUGAR_CANE)) {
            return 45 * 20;
        }
        if (plant.is(GnomeContent.YELLOW_MUSHROOM_ITEM.get())) {
            return 120 * 20;
        }
        if (plant.is(Items.BROWN_MUSHROOM) || plant.is(Items.RED_MUSHROOM)) {
            return 90 * 20;
        }
        return 60 * 20;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GrowingPotBlockEntity pot) {
        if (pot.plant.isEmpty() || !pot.hasRoom()) {
            return;
        }
        int step = 1;
        if (level.getGameTime() % 20 == 0) {
            pot.wet = pot.checkWater(level, pos);
        }
        if (pot.wet) {
            step++;
        }
        if (GnomeBoost.extraStep(level, pos)) {
            step++;
        }
        pot.progress += step;
        int cycle = cycleTicks(pot.plant);
        if (pot.progress >= cycle) {
            pot.progress = 0;
            pot.grow();
        } else if (pot.progress % 100 < step) {
            // sincroniza devagar o tamanho da planta (o renderizador usa o progresso)
            pot.sync();
        }
    }

    private boolean wet;

    private boolean checkWater(Level level, BlockPos pos) {
        for (Direction dir : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
            if (level.getFluidState(pos.relative(dir)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRoom() {
        return this.output.isEmpty() || this.output.getCount() < this.output.getMaxStackSize();
    }

    private void grow() {
        if (this.output.isEmpty()) {
            this.output = this.plant.copyWithCount(1);
        } else {
            this.output.grow(1);
        }
        this.sync();
    }

    /** Farinha de osso: brota uma cópia na hora. @return false se já está cheio. */
    public boolean bonemeal() {
        if (this.plant.isEmpty() || !this.hasRoom()) {
            return false;
        }
        this.progress = 0;
        this.grow();
        return true;
    }

    public ItemStack getPlant() {
        return this.plant;
    }

    public ItemStack getOutput() {
        return this.output;
    }

    /** 0..1: quanto falta para a próxima cópia (o renderizador faz a planta crescer). */
    public float growth() {
        return this.plant.isEmpty() ? 0.0F : Math.min(1.0F, this.progress / (float) cycleTicks(this.plant));
    }

    public void setPlant(ItemStack stack) {
        this.plant = stack;
        this.progress = 0;
        this.sync();
    }

    public ItemStack removePlant() {
        ItemStack out = this.plant;
        this.plant = ItemStack.EMPTY;
        this.progress = 0;
        this.sync();
        return out;
    }

    public ItemStack takeOutput() {
        ItemStack out = this.output;
        this.output = ItemStack.EMPTY;
        this.sync();
        return out;
    }

    public Component statusMessage() {
        if (this.plant.isEmpty()) {
            return Component.translatable("message.mistech.growing_pot.empty");
        }
        int seconds = Math.max(1, (cycleTicks(this.plant) - this.progress) / 20);
        return Component.translatable("message.mistech.growing_pot.status", this.plant.getHoverName(), seconds);
    }

    public int comparatorSignal() {
        return this.output.isEmpty() ? 0 : 1 + Math.round(14.0F * this.output.getCount() / this.output.getMaxStackSize());
    }

    private void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ----- salvar / sincronizar -----

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.plant = input.read("Plant", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.output = input.read("Output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.progress = input.getIntOr("Progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.plant.isEmpty()) {
            output.store("Plant", ItemStack.OPTIONAL_CODEC, this.plant);
        }
        if (!this.output.isEmpty()) {
            output.store("Output", ItemStack.OPTIONAL_CODEC, this.output);
        }
        output.putInt("Progress", this.progress);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ----- Container: 0 = colheita, 1 = planta -----

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_DOWN : SLOTS_OTHER;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == PLANT && this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return this.plant.isEmpty() && this.output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == OUTPUT ? this.output : this.plant;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack source = this.getItem(slot);
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = source.split(count);
        if (slot == PLANT) {
            this.progress = 0;
        }
        this.sync();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = this.getItem(slot);
        if (slot == OUTPUT) {
            this.output = ItemStack.EMPTY;
        } else {
            this.plant = ItemStack.EMPTY;
        }
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == OUTPUT) {
            this.output = stack;
        } else {
            if (!ItemStack.isSameItemSameComponents(this.plant, stack)) {
                this.progress = 0;
            }
            this.plant = stack;
        }
        this.sync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == PLANT && this.plant.isEmpty() && isPlant(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.plant = ItemStack.EMPTY;
        this.output = ItemStack.EMPTY;
        this.sync();
    }
}
