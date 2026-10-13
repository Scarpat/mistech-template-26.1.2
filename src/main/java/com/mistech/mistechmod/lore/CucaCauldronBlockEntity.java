package com.mistech.mistechmod.lore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Caldeirão da Cuca: mistura até {@link #MAX_POTIONS} poções numa só. Efeitos iguais somam a duração (até 8 min) e ficam
 * com o nível maior; Essência Impura sobe 1 nível de tudo (uma vez). Com 2 ou mais poções, um frasco vazio tira a
 * <b>Poção da Cuca</b> com todos os efeitos.
 */
public class CucaCauldronBlockEntity extends BlockEntity {
    public static final int MAX_POTIONS = 3;
    public static final int MAX_DURATION = 20 * 60 * 8;

    private final List<MobEffectInstance> effects = new ArrayList<>();
    private int potions;
    private boolean empowered;

    public CucaCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(LoreContent.CUCA_CAULDRON_BE.get(), pos, state);
    }

    public int potions() {
        return this.potions;
    }

    public boolean empowered() {
        return this.empowered;
    }

    public int color() {
        return this.effects.isEmpty() ? 0x3a8a3a : PotionContents.getColorOptional(this.effects).orElse(0x3a8a3a);
    }

    /** Põe uma poção; false se não cabe ou não tem efeito. */
    public boolean addPotion(PotionContents contents) {
        if (this.potions >= MAX_POTIONS) {
            return false;
        }
        boolean any = false;
        for (MobEffectInstance e : contents.getAllEffects()) {
            any = true;
            this.merge(e);
        }
        if (!any) {
            return false;
        }
        this.potions++;
        this.changed();
        return true;
    }

    private void merge(MobEffectInstance e) {
        Holder<MobEffect> type = e.getEffect();
        for (int i = 0; i < this.effects.size(); i++) {
            MobEffectInstance old = this.effects.get(i);
            if (old.getEffect().equals(type)) {
                int duration = e.getEffect().value().isInstantenous() ? 1 : Math.min(MAX_DURATION, old.getDuration() + e.getDuration());
                this.effects.set(i, new MobEffectInstance(type, duration, Math.max(old.getAmplifier(), e.getAmplifier())));
                return;
            }
        }
        this.effects.add(new MobEffectInstance(type, e.getDuration(), e.getAmplifier()));
    }

    public boolean empower() {
        if (this.empowered || this.effects.isEmpty()) {
            return false;
        }
        this.empowered = true;
        for (int i = 0; i < this.effects.size(); i++) {
            MobEffectInstance e = this.effects.get(i);
            this.effects.set(i, new MobEffectInstance(e.getEffect(), e.getDuration(), Math.min(4, e.getAmplifier() + 1)));
        }
        this.changed();
        return true;
    }

    /** Tira a Poção da Cuca (esvazia o caldeirão). */
    public ItemStack take() {
        if (this.potions < 2) {
            return ItemStack.EMPTY;
        }
        ItemStack out = new ItemStack(Items.POTION);
        out.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.empty(), Optional.of(this.color()), List.copyOf(this.effects), Optional.of("cuca")));
        this.clear();
        return out;
    }

    public void clear() {
        this.effects.clear();
        this.potions = 0;
        this.empowered = false;
        this.changed();
    }

    private void changed() {
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            BlockState want = state.setValue(CucaCauldronBlock.FILL, Math.min(3, this.potions));
            if (want != state) {
                this.level.setBlock(this.worldPosition, want, 3);
            }
            this.level.sendBlockUpdated(this.worldPosition, want, want, 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Effects", MobEffectInstance.CODEC.listOf(), List.copyOf(this.effects));
        output.putInt("Potions", this.potions);
        output.putBoolean("Empowered", this.empowered);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.effects.clear();
        this.effects.addAll(input.read("Effects", MobEffectInstance.CODEC.listOf()).orElse(List.of()));
        this.potions = input.getIntOr("Potions", 0);
        this.empowered = input.getBooleanOr("Empowered", false);
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
