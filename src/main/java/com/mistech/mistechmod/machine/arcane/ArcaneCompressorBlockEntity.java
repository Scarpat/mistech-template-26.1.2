package com.mistech.mistechmod.machine.arcane;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.beyond.PaleNetherContent;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.purity.block.PurityFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Compressor Arcano: versão avançada do Compressor de Magia. Não precisa de incenso: gasta {@link #FE_PER_TICK} FE/t e
 * endurece {@link #BATCH} mB de Essência Impura em 1 Fragmento de Magia Impura a cada {@link #SECONDS} s.
 * A essência entra por canos (tanque de {@link #TANK} mB) ou por frascos/baldes no espaço de entrada (os recipientes vazios
 * saem na saída). Frasco de Essência Ígnea no espaço de entrada vira Fragmento Choroso.
 * Espaços: 0 = entrada, 1..3 = saída. info1 = tanque (mB).
 */
public class ArcaneCompressorBlockEntity extends MachineBlockEntity implements ArcaneMachineBlock.EssenceTankHolder {
    public static final int TANK = 8000;
    public static final int BATCH = 250;
    public static final int SECONDS = 6;
    public static final int FE_PER_TICK = 30;
    public static final int SLOT_INPUT = 0;

    private final PurityFluids.Tanks tank = new PurityFluids.Tanks(TANK, new FluidResource[] {PurityFluids.impure()},
            new boolean[] {true}, new boolean[] {false}, this::setChanged);
    /** O lote atual é de Essência Ígnea (frasco já consumido). */
    private boolean igneousBatch;

    public ArcaneCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneMachines.ARCANE_COMPRESSOR_BE.get(), pos, state, ArcaneMachines.COMPRESSOR_LAYOUT, 400, 0);
        this.maxProgress = SECONDS * 20 * 2;
    }

    public PurityFluids.Tanks getTank() {
        return this.tank;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        this.pullInput();
        ItemStack made = new ItemStack(this.igneousBatch ? PaleNetherContent.CRYING_SHARD.get() : Mistech.IMPURE_MAGIC_SHARD.get());
        int cost = this.energyStep(FE_PER_TICK);
        boolean hasBatch = this.igneousBatch || this.tank.amount(0) >= BATCH;
        boolean working = hasBatch && this.canOutput(made) && this.hasEnergy(cost);
        if (working) {
            this.drainEnergy(cost);
            this.progress += this.workStep();
            if (this.progress >= this.maxProgress) {
                this.progress = 0;
                if (this.igneousBatch) {
                    this.igneousBatch = false;
                } else {
                    this.tank.take(0, BATCH);
                }
                this.insertOutput(made);
                level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.7F, 0.8F);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.2, 0.1, 0.2, 0.05);
            }
            this.setChanged();
        }
        this.info1 = this.tank.amount(0);
        this.setLit(level, pos, state, working);
    }

    /** Frasco/balde de Essência Impura vai para o tanque; Essência Ígnea vira o próximo lote. */
    private void pullInput() {
        ItemStack in = this.items.get(SLOT_INPUT);
        if (in.isEmpty()) {
            return;
        }
        if (in.is(PaleNetherContent.IGNEOUS_ESSENCE_BOTTLE.get())) {
            if (!this.igneousBatch && this.progress == 0 && this.canOutput(new ItemStack(Items.GLASS_BOTTLE))) {
                in.shrink(1);
                this.igneousBatch = true;
                this.insertOutput(new ItemStack(Items.GLASS_BOTTLE));
            }
            return;
        }
        boolean bottle = in.is(Mistech.IMPURE_ESSENCE_BOTTLE.get());
        int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        ItemStack empty = new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET);
        if (this.tank.space(0) >= amount && this.canOutput(empty)) {
            in.shrink(1);
            this.tank.add(0, amount);
            this.insertOutput(empty);
        }
    }

    public static boolean acceptsInput(ItemStack stack) {
        return stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) || stack.is(Mistech.IMPURE_ESSENCE_BUCKET.get())
                || stack.is(PaleNetherContent.IGNEOUS_ESSENCE_BOTTLE.get());
    }

    @Override
    public boolean handles(ItemStack stack) {
        return stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) || stack.is(Mistech.IMPURE_ESSENCE_BUCKET.get());
    }

    @Override
    public void interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        int in = held.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        if (this.tank.space(0) < in) {
            player.sendOverlayMessage(Component.translatable("message.mistech.altar.full"));
            return;
        }
        this.tank.add(0, in);
        PurityFluids.swap(player, hand, new ItemStack(in == PurityFluids.BOTTLE ? Items.GLASS_BOTTLE : Items.BUCKET));
        player.level().playSound(null, this.worldPosition, in == PurityFluids.BOTTLE ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY,
                SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.mistech.tank", Component.translatable("block.mistech.impure_essence"), this.tank.amount(0), TANK));
        super.addInfo(lines);
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.tank.setAmount(0, input.getIntOr("Essence", 0));
        this.igneousBatch = input.getBooleanOr("Igneous", false);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Essence", this.tank.amount(0));
        output.putBoolean("Igneous", this.igneousBatch);
    }
}
