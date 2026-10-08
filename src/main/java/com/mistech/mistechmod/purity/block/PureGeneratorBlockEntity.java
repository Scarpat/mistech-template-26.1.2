package com.mistech.mistechmod.purity.block;

import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.PurityLayouts;

import net.minecraft.core.BlockPos;
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
 * Gerador Purificado: queima 1 mB de Essência Purificada por tick e gera {@link #FE_PER_TICK} FE/t
 * (250 mB = 25.000 FE). Tanque de 4.000 mB (canos podem encher). Buffer 50.000 FE; envia até 400 FE/t para cada vizinho.
 */
public class PureGeneratorBlockEntity extends MachineBlockEntity {
    public static final int TANK = 4000;
    public static final int FE_PER_TICK = 100;
    public static final int MAX_OUTPUT = 400;
    public static final int SLOT_FUEL = 0;

    private final PurityFluids.Tanks tank = new PurityFluids.Tanks(TANK, new FluidResource[] {PurityFluids.pure()},
            new boolean[] {true}, new boolean[] {false}, this::setChanged);

    public PureGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(PurityContent.PURE_GENERATOR_BE.get(), pos, state, PurityLayouts.PURE_GENERATOR, 0, MAX_OUTPUT);
    }

    public PurityFluids.Tanks getTank() {
        return this.tank;
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack fuel = this.items.get(SLOT_FUEL);
        if (!fuel.isEmpty()) {
            boolean bottle = fuel.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get());
            int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
            ItemStack empty = new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET);
            if (this.tank.space(0) >= amount && this.canOutput(empty)) {
                fuel.shrink(1);
                this.tank.add(0, amount);
                this.insertOutput(empty);
            }
        }
        int generated = 0;
        int space = this.layout.capacity - this.storedEnergy();
        if (space >= FE_PER_TICK && this.tank.take(0, 1)) {
            generated = FE_PER_TICK;
            this.addEnergy(generated);
        }
        this.info0 = generated;
        this.info1 = this.tank.amount(0);
        this.maxProgress = TANK;
        this.progress = this.tank.amount(0);
        this.pushEnergy(level, pos, MAX_OUTPUT);
        this.setLit(level, pos, state, generated > 0);
    }

    public boolean interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        int in = held.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) ? PurityFluids.BOTTLE
                : held.is(PurityContent.PURIFIED_ESSENCE_BUCKET.get()) ? PurityFluids.BUCKET : 0;
        if (in <= 0 || this.tank.space(0) < in) {
            return false;
        }
        this.tank.add(0, in);
        PurityFluids.swap(player, hand, new ItemStack(in == PurityFluids.BOTTLE ? Items.GLASS_BOTTLE : Items.BUCKET));
        player.level().playSound(null, this.worldPosition, in == PurityFluids.BOTTLE ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY,
                SoundSource.BLOCKS, 1.0F, 1.2F);
        return true;
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
