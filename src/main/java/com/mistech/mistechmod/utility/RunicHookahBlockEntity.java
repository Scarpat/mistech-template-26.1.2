package com.mistech.mistechmod.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.purity.Taint;
import com.mistech.mistechmod.purity.TaintEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Narguilé Rúnico: base (água / Essência Impura / Essência Purificada), fornilho (fumo: tabaco, cânhamo ou arruda secos;
 * {@link #PUFFS} tragadas), runa (sabor) e brasa (acesa). Tragada = efeito do charuto mágico da runa (tabaco: defesa;
 * cânhamo: poder). A runa gasta 1 uso a cada {@link #PUFFS_PER_RUNE_USE} tragadas (Selada não gasta).
 */
public class RunicHookahBlockEntity extends BlockEntity {
    public static final int PUFFS = 32;
    public static final int PUFFS_PER_RUNE_USE = 8;
    public static final int PUFF_COOLDOWN = 30;
    public static final double AURA_RADIUS = 6.0;

    public enum Base { NONE, WATER, IMPURE, PURE }

    public enum Leaf { NONE, TOBACCO, HEMP, RUE }

    private Base base = Base.NONE;
    private Leaf leaf = Leaf.NONE;
    private int puffs;
    private ItemStack rune = ItemStack.EMPTY;
    private int runePuffs;
    private boolean lit;
    private final Map<UUID, Long> lastPuff = new HashMap<>();

    public RunicHookahBlockEntity(BlockPos pos, BlockState state) {
        super(UtilityContent.RUNIC_HOOKAH_BE.get(), pos, state);
    }

    public Base base() { return this.base; }
    public Leaf leaf() { return this.leaf; }
    public int puffs() { return this.puffs; }
    public ItemStack rune() { return this.rune; }
    public boolean lit() { return this.lit; }

    public void setBase(Base base) { this.base = base; this.changed(); }

    public void fill(Leaf leaf) {
        this.leaf = leaf;
        this.puffs = PUFFS;
        this.changed();
    }

    public ItemStack swapRune(ItemStack newRune) {
        ItemStack old = this.rune;
        this.rune = newRune;
        this.runePuffs = 0;
        this.changed();
        return old;
    }

    public void setLit(boolean lit) {
        this.lit = lit;
        this.changed();
    }

    private void changed() {
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            boolean on = this.lit && this.puffs > 0;
            RuneElement element = this.element();
            BlockState want = state.setValue(RunicHookahBlock.LIT, on)
                    .setValue(RunicHookahBlock.BASE, this.base.ordinal())
                    .setValue(RunicHookahBlock.LEAF, this.puffs > 0 ? this.leaf.ordinal() : 0)
                    .setValue(RunicHookahBlock.RUNE, element == null ? 0 : element.ordinal() + 1);
            if (want != state) {
                this.level.setBlock(this.worldPosition, want, 3);
                state = want;
            }
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    public @Nullable RuneElement element() {
        return RuneElement.ofRune(this.rune);
    }

    /** Uma tragada de {@code player}. @return mensagem de erro (chave de tradução) ou null se tragou. */
    public @Nullable String puff(ServerLevel level, Player player) {
        if (this.base == Base.NONE) return "block.mistech.runic_hookah.no_base";
        if (this.leaf == Leaf.NONE || this.puffs <= 0) return "block.mistech.runic_hookah.no_leaf";
        if (!this.lit) return "block.mistech.runic_hookah.not_lit";
        long now = level.getGameTime();
        Long last = this.lastPuff.get(player.getUUID());
        if (last != null && now - last < PUFF_COOLDOWN) {
            return "";
        }
        this.lastPuff.put(player.getUUID(), now);
        this.puffs--;
        apply(level, player, 1.0, false);
        // runa
        if (!this.rune.isEmpty() && !TaintEvents.isSealed(this.rune) && this.rune.isDamageableItem() && ++this.runePuffs >= PUFFS_PER_RUNE_USE) {
            this.runePuffs = 0;
            this.rune.setDamageValue(this.rune.getDamageValue() + 1);
            if (this.rune.getDamageValue() >= this.rune.getMaxDamage()) {
                this.rune = ItemStack.EMPTY;
                level.playSound(null, this.worldPosition, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 0.6F, 1.0F);
            }
        }
        if (this.puffs <= 0) {
            this.leaf = Leaf.NONE;
            this.lit = false;
        }
        BlockPos p = this.worldPosition;
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getEyeY(), player.getZ(), 3, 0.1, 0.05, 0.1, 0.01);
        level.sendParticles(ParticleTypes.BUBBLE_POP, p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5, 6, 0.1, 0.1, 0.1, 0.02);
        level.playSound(null, p, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.8F, 0.7F);
        this.changed();
        return null;
    }

    /** Aplica o efeito da tragada. {@code scale} = força (1 = tragada; aura = fraca e curta). */
    private void apply(ServerLevel level, Player player, double scale, boolean aura) {
        RuneElement element = this.element();
        boolean power = this.leaf == Leaf.HEMP || this.base == Base.IMPURE;
        int duration = (int) (RuneElement.DURATION_TICKS * scale * (this.base == Base.PURE ? 1.5 : 1.0));
        if (element != null) {
            for (RuneElement.Effect effect : element.effects(!power)) {
                player.addEffect(new MobEffectInstance(effect.effect(), duration, aura ? 0 : effect.amplifier(), true, true, true));
                if (aura) {
                    break; // a aura só dá o primeiro efeito, fraco
                }
            }
        } else if (!aura) {
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 60, 0, true, true, true));
        }
        if (aura) {
            return;
        }
        if (this.base == Base.IMPURE) {
            Taint.add(player, 0.5F);
        } else if (this.base == Base.PURE) {
            Taint.cleanse(player, 1.0F);
        }
        if (this.leaf == Leaf.RUE) {
            Taint.cleanse(player, 1.0F);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RunicHookahBlockEntity hookah) {
        if (!(level instanceof ServerLevel server) || !hookah.lit || hookah.puffs <= 0 || level.getGameTime() % 40 != 0) {
            return;
        }
        for (Player player : server.getEntitiesOfClass(Player.class, new AABB(pos).inflate(AURA_RADIUS),
                p -> p.distanceToSqr(pos.getCenter()) <= AURA_RADIUS * AURA_RADIUS)) {
            hookah.apply(server, player, 200.0 / RuneElement.DURATION_TICKS, true);
        }
        server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1, 0.05, 0.0, 0.05, 0.01);
    }

    // ----- salvar / sincronizar -----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Base", this.base.name());
        output.putString("Leaf", this.leaf.name());
        output.putInt("Puffs", this.puffs);
        output.putInt("RunePuffs", this.runePuffs);
        output.putBoolean("Lit", this.lit);
        if (!this.rune.isEmpty()) {
            output.store("Rune", ItemStack.CODEC, this.rune);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.base = enumOr(Base.class, input.getStringOr("Base", "NONE"), Base.NONE);
        this.leaf = enumOr(Leaf.class, input.getStringOr("Leaf", "NONE"), Leaf.NONE);
        this.puffs = input.getIntOr("Puffs", 0);
        this.runePuffs = input.getIntOr("RunePuffs", 0);
        this.lit = input.getBooleanOr("Lit", false);
        this.rune = input.read("Rune", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    private static <E extends Enum<E>> E enumOr(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
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
