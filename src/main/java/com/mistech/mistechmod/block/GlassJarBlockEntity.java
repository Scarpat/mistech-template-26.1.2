package com.mistech.mistechmod.block;

import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Pote de Vidro: guarda até 1 pack (pilha máxima) de UM tipo de item e mostra o item dentro.
 * Implementa Container (1 slot) para funcionar com funis; o conteúdo cai ao quebrar o bloco.
 */
public class GlassJarBlockEntity extends BlockEntity implements Container {
    private ItemStack stored = ItemStack.EMPTY;

    public GlassJarBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.GLASS_JAR.get(), pos, state);
    }

    /** A cada quantos ticks o gnomo pode aparecer (5 s) e a chance em cada tentativa. */
    public static final int GNOME_CHECK_TICKS = 100;
    public static final float GNOME_CHANCE = 0.30F;
    private int baitTicks;

    /**
     * Com Isca de Gnomo dentro: a cada 5 s há 30% de chance de um gnomo aparecer e entrar no pote.
     * Gasta 1 isca; o pote vira Gnomo Engarrafado e o resto das iscas cai no chão.
     */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, GlassJarBlockEntity jar) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)
                || !jar.stored.is(com.mistech.mistechmod.registry.GnomeContent.GNOME_BAIT.get())) {
            jar.baitTicks = 0;
            return;
        }
        jar.baitTicks++;
        if (jar.baitTicks % 20 == 0) {
            // cheirinho da isca saindo do pote
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    2, 0.1, 0.05, 0.1, 0.005);
        }
        if (jar.baitTicks % GNOME_CHECK_TICKS != 0 || server.getRandom().nextFloat() >= GNOME_CHANCE) {
            return;
        }
        jar.stored.shrink(1);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, x, y + 0.2, z, 16, 0.3, 0.3, 0.3, 0.02);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, x, y, z, 10, 0.3, 0.3, 0.3, 0.0);
        server.playSound(null, pos, net.minecraft.sounds.SoundEvents.VILLAGER_YES, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.8F);
        server.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOTTLE_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.0F);
        // trocar o bloco derruba o que sobrou no pote
        server.setBlock(pos, com.mistech.mistechmod.registry.GnomeContent.BOTTLED_GNOME.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    public ItemStack getStored() {
        return this.stored;
    }

    /** Quanto ainda cabe deste item (0 se for outro item). */
    public int spaceFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (this.stored.isEmpty()) {
            return stack.getMaxStackSize();
        }
        if (!ItemStack.isSameItemSameComponents(this.stored, stack)) {
            return 0;
        }
        return this.stored.getMaxStackSize() - this.stored.getCount();
    }

    /** Coloca até {@code max} itens da pilha dada no pote. @return quantos entraram. */
    public int insert(ItemStack from, int max) {
        int amount = Math.min(Math.min(max, from.getCount()), this.spaceFor(from));
        if (amount <= 0) {
            return 0;
        }
        if (this.stored.isEmpty()) {
            this.stored = from.copyWithCount(amount);
        } else {
            this.stored.grow(amount);
        }
        this.setChanged();
        return amount;
    }

    /** Retira até {@code amount} itens. */
    public ItemStack take(int amount) {
        if (this.stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = this.stored.split(amount);
        this.setChanged();
        return out;
    }

    /** Sinal de comparador: 0..15 conforme o quanto o pote está cheio. */
    public int getComparatorSignal() {
        if (this.stored.isEmpty()) {
            return 0;
        }
        return 1 + Math.round(14.0F * this.stored.getCount() / this.stored.getMaxStackSize());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            // avisa o cliente para redesenhar o item dentro do pote
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ----- salvar / sincronizar -----

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.stored = input.read("Item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.stored.isEmpty()) {
            output.store("Item", ItemStack.OPTIONAL_CODEC, this.stored);
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

    // ----- Container (1 slot) -----

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.stored.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.stored;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return this.take(count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = this.stored;
        this.stored = ItemStack.EMPTY;
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.stored = stack;
        this.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return this.stored.isEmpty() || ItemStack.isSameItemSameComponents(this.stored, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.stored = ItemStack.EMPTY;
        this.setChanged();
    }
}
