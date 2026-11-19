package com.mistech.mistechmod.machine.arcane;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.purity.block.PurityFluids;

import net.minecraft.core.BlockPos;
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
 * Extrator Arcano: versão avançada do Extrator de Essência. Produz {@link #BATCH} mB de Essência Impura a cada
 * {@link #SECONDS} s gastando {@link #FE_PER_TICK} FE/t, num tanque de {@link #TANK} mB (canos e golens tiram).
 * Frascos de vidro / baldes no espaço de entrada saem cheios na saída.
 * Espaços: 0 = recipientes vazios, 1..2 = saída. info1 = tanque (mB).
 */
public class ArcaneExtractorBlockEntity extends MachineBlockEntity implements ArcaneMachineBlock.EssenceTankHolder {
    public static final int TANK = 8000;
    public static final int BATCH = 250;
    public static final int SECONDS = 15;
    public static final int FE_PER_TICK = 20;
    public static final int SLOT_EMPTY = 0;

    private final PurityFluids.Tanks tank = new PurityFluids.Tanks(TANK, new FluidResource[] {PurityFluids.impure()},
            new boolean[] {false}, new boolean[] {true}, this::setChanged);

    public ArcaneExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneMachines.ARCANE_EXTRACTOR_BE.get(), pos, state, ArcaneMachines.EXTRACTOR_LAYOUT, 400, 0);
        this.maxProgress = SECONDS * 20 * 2;
    }

    public PurityFluids.Tanks getTank() {
        return this.tank;
    }

    @Override
    protected float taintPerMinute() {
        return com.mistech.mistechmod.Config.areaTaintEssenceMachines();
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        this.fillContainers();
        int cost = this.energyStep(FE_PER_TICK);
        boolean working = this.tank.space(0) >= BATCH && this.hasEnergy(cost);
        if (working) {
            this.drainEnergy(cost);
            this.progress += this.workStep();
            if (this.progress >= this.maxProgress) {
                this.progress = 0;
                this.tank.add(0, BATCH);
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5F, 0.6F);
            }
            this.setChanged();
        }
        this.info1 = this.tank.amount(0);
        this.setLit(level, pos, state, working);
    }

    /** Frasco vazio → Frasco de Essência (250 mB); balde → balde de essência (1000 mB). */
    private void fillContainers() {
        ItemStack in = this.items.get(SLOT_EMPTY);
        if (in.isEmpty()) {
            return;
        }
        boolean bottle = in.is(Items.GLASS_BOTTLE);
        int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        ItemStack full = new ItemStack(bottle ? Mistech.IMPURE_ESSENCE_BOTTLE.get() : Mistech.IMPURE_ESSENCE_BUCKET.get());
        if (this.tank.amount(0) >= amount && this.canOutput(full)) {
            this.tank.take(0, amount);
            in.shrink(1);
            this.insertOutput(full);
        }
    }

    @Override
    public boolean handles(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE) || stack.is(Items.BUCKET);
    }

    @Override
    public void interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        int out = held.is(Items.GLASS_BOTTLE) ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        if (!this.tank.take(0, out)) {
            player.sendOverlayMessage(Component.translatable("info.mistech.tank", Component.translatable("block.mistech.impure_essence"),
                    this.tank.amount(0), TANK));
            return;
        }
        PurityFluids.swap(player, hand, new ItemStack(out == PurityFluids.BOTTLE ? Mistech.IMPURE_ESSENCE_BOTTLE.get() : Mistech.IMPURE_ESSENCE_BUCKET.get()));
        player.level().playSound(null, this.worldPosition, out == PurityFluids.BOTTLE ? SoundEvents.BOTTLE_FILL : SoundEvents.BUCKET_FILL,
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
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Essence", this.tank.amount(0));
    }
}
