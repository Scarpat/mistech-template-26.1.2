package com.mistech.mistechmod.drink;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Barril de Fermentação (madeira turva): guarda até {@link #MAX} garrafas de uma coisa só.
 * Garapa → Garapa Fermentada em {@link #FERMENT_TICKS}; Cachaça → Cachaça Envelhecida em {@link #AGE_TICKS}.
 * O tempo conta pelo relógio do mundo (vale mesmo com o chunk descarregado). Pôr mais garrafas no meio do caminho
 * dilui o tempo já passado (média ponderada).
 */
public class FermentationBarrelBlockEntity extends BlockEntity {
    public static final int MAX = 16;
    public static final int FERMENT_TICKS = 12000;
    public static final int AGE_TICKS = 24000;

    private ItemStack contents = ItemStack.EMPTY;
    private long start;

    public FermentationBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(DrinkContent.FERMENTATION_BARREL_BE.get(), pos, state);
    }

    /** O que a garrafa vira aqui dentro (null = não entra). */
    public static @Nullable Item resultOf(ItemStack stack) {
        if (stack.is(DrinkContent.GARAPA.get())) {
            return DrinkContent.FERMENTED_GARAPA.get();
        }
        if (stack.is(DrinkContent.CACHACA.get())) {
            return DrinkContent.AGED_CACHACA.get();
        }
        return null;
    }

    public static int timeFor(ItemStack stack) {
        return stack.is(DrinkContent.CACHACA.get()) ? AGE_TICKS : FERMENT_TICKS;
    }

    public ItemStack contents() {
        return this.contents;
    }

    public long elapsed() {
        return this.level == null || this.contents.isEmpty() ? 0 : Math.max(0, this.level.getGameTime() - this.start);
    }

    public boolean done() {
        return !this.contents.isEmpty() && this.elapsed() >= timeFor(this.contents);
    }

    public float progress() {
        return this.contents.isEmpty() ? 0 : Math.min(1.0F, this.elapsed() / (float) timeFor(this.contents));
    }

    /** Põe até {@code count} garrafas; devolve quantas entraram. */
    public int insert(ItemStack stack, int count) {
        if (this.level == null || resultOf(stack) == null || this.done()) {
            return 0;
        }
        if (!this.contents.isEmpty() && !ItemStack.isSameItemSameComponents(this.contents, stack)) {
            return 0;
        }
        int room = MAX - this.contents.getCount();
        int n = Math.min(room, Math.min(count, stack.getCount()));
        if (n <= 0) {
            return 0;
        }
        long now = this.level.getGameTime();
        if (this.contents.isEmpty()) {
            this.contents = stack.copyWithCount(n);
            this.start = now;
        } else {
            int old = this.contents.getCount();
            long elapsed = this.elapsed() * old / (old + n);
            this.contents.grow(n);
            this.start = now - elapsed;
        }
        this.changed();
        return n;
    }

    /** Tira tudo: pronto = produto; não pronto = devolve o que entrou. */
    public ItemStack takeAll() {
        if (this.contents.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out;
        if (this.done()) {
            Item result = resultOf(this.contents);
            out = new ItemStack(result, this.contents.getCount());
        } else {
            out = this.contents.copy();
        }
        this.contents = ItemStack.EMPTY;
        this.start = 0;
        this.changed();
        return out;
    }

    private void changed() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.contents.isEmpty()) {
            output.store("Contents", ItemStack.CODEC, this.contents);
        }
        output.putLong("Start", this.start);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.contents = input.read("Contents", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.start = input.getLongOr("Start", 0L);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
