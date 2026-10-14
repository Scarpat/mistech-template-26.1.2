package com.mistech.mistechmod.block;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.IncenseItem;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Endurece a essência em {@link #HARDEN_TICKS} ticks enquanto houver incenso aceso por perto.
 * Sem incenso, o progresso fica parado (não perde).
 * O fragmento pronto fica guardado em cima do compressor (até 1 pack) para golens/funis tirarem — sai por qualquer lado.
 * Se o que está guardado for outro fragmento ou estiver cheio, espera.
 */
public class MagicCompressorBlockEntity extends BlockEntity implements net.minecraft.world.WorldlyContainer {
    private static final int[] SLOTS = {0};
    private ItemStack output = ItemStack.EMPTY;
    public static final int HARDEN_TICKS = 15 * 20;
    /** Distância até o jogador com incenso aceso. */
    public static final double PLAYER_RANGE = 6.0;

    private int progress;
    /** Cheio de Essência Ígnea: endurece em Fragmento Choroso. */
    private boolean igneous;

    public void setIgneous(boolean igneous) {
        this.igneous = igneous;
        this.setChanged();
    }

    public MagicCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.MAGIC_COMPRESSOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicCompressorBlockEntity compressor) {
        if (!state.getValue(MagicCompressorBlock.FILLED) || level.getGameTime() % 20 != 0
                || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!hasIncenseNearby(serverLevel, pos)) {
            return;
        }
        compressor.progress += com.mistech.mistechmod.machine.GnomeBoost.isBoosted(level, pos) ? 30 : 20;
        serverLevel.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                4, 0.12, 0.05, 0.12, 0.0);
        if (compressor.progress >= HARDEN_TICKS) {
            ItemStack made = new ItemStack(compressor.igneous
                    ? com.mistech.mistechmod.beyond.PaleNetherContent.CRYING_SHARD.get() : Mistech.IMPURE_MAGIC_SHARD.get());
            if (!compressor.output.isEmpty() && (!ItemStack.isSameItemSameComponents(compressor.output, made)
                    || compressor.output.getCount() >= compressor.output.getMaxStackSize())) {
                compressor.progress = HARDEN_TICKS; // pronto, esperando alguém tirar o que está em cima
                return;
            }
            compressor.progress = 0;
            level.setBlock(pos, state.setValue(MagicCompressorBlock.FILLED, false), Block.UPDATE_ALL);
            if (compressor.output.isEmpty()) {
                compressor.output = made;
            } else {
                compressor.output.grow(1);
            }
            compressor.sync();
            compressor.igneous = false;
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    20, 0.2, 0.1, 0.2, 0.05);
        }
        compressor.setChanged();
    }

    /** Jogador perto com incenso aceso no inventário, ou Porta-Incenso aceso cuja área alcança este bloco. */
    public static boolean hasIncenseNearby(ServerLevel level, BlockPos pos) {
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(PLAYER_RANGE))) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (IncenseItem.isIncense(stack) && IncenseItem.isLit(stack)) {
                    return true;
                }
            }
        }

        double radius = IncenseHolderBlockEntity.MAX_EFFECT_RADIUS;
        int minCx = SectionPos.blockToSectionCoord(pos.getX() - (int) radius);
        int maxCx = SectionPos.blockToSectionCoord(pos.getX() + (int) radius);
        int minCz = SectionPos.blockToSectionCoord(pos.getZ() - (int) radius);
        int maxCz = SectionPos.blockToSectionCoord(pos.getZ() + (int) radius);
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof IncenseHolderBlockEntity holder
                            && be.getBlockState().hasProperty(IncenseHolderBlock.LIT)
                            && be.getBlockState().getValue(IncenseHolderBlock.LIT)
                            && be.getBlockPos().getCenter().distanceToSqr(pos.getCenter())
                                    <= holder.getEffectRadius() * holder.getEffectRadius()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public ItemStack getOutput() {
        return this.output;
    }

    /** Mão vazia no compressor: pega o que está pronto. */
    public ItemStack takeOutput() {
        ItemStack out = this.output;
        this.output = ItemStack.EMPTY;
        this.sync();
        return out;
    }

    private void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.progress = input.getIntOr("Progress", 0);
        this.igneous = input.getBooleanOr("Igneous", false);
        this.output = input.read("Output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", this.progress);
        output.putBoolean("Igneous", this.igneous);
        if (!this.output.isEmpty()) {
            output.store("Output", ItemStack.OPTIONAL_CODEC, this.output);
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    // ----- Container (só saída): golens, funis e canos tiram o fragmento -----

    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.@org.jspecify.annotations.Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.output;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack out = this.output.split(count);
        this.sync();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = this.output;
        this.output = ItemStack.EMPTY;
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.output = stack;
        this.sync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.output = ItemStack.EMPTY;
        this.sync();
    }
}
