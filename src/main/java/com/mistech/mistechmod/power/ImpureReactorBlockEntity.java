package com.mistech.mistechmod.power;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.block.PurityFluids;
import com.mistech.mistechmod.registry.MachineContent;

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
 * Reator Impuro: queima Magia Impura e gera {@link #RATE} FE/t.
 * <ul>
 * <li>Sólidos (espaço de combustível): Pó 10.000 FE, Fragmento 40.000 FE, Bloco 360.000 FE.</li>
 * <li>Essência Impura (tanque de {@link #TANK} mB; frasco, balde ou canos): 1 mB = 100 FE (10 mB/t).</li>
 * </ul>
 * A cada {@link #RESIDUE_EVERY} FE gerados sai 1 Borra Impura; com as saídas cheias o reator para.
 * Buffer {@link #CAPACITY} FE; envia até {@link #MAX_OUTPUT} FE/t para cada bloco encostado.
 * info0 = FE/t; info1 = tanque (mB); info2 = situação (0 ok, 1 sem combustível, 2 borra acumulada, 3 cheio).
 */
public class ImpureReactorBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 500_000;
    public static final int RATE = 1_000;
    public static final int MAX_OUTPUT = 2_000;
    public static final int TANK = 8_000;
    public static final int FE_PER_MB = 100;
    public static final int RESIDUE_EVERY = 200_000;
    public static final int SLOT_FUEL = 0;

    public static final int STATUS_OK = 0, STATUS_NO_FUEL = 1, STATUS_RESIDUE = 2, STATUS_FULL = 3;

    private final PurityFluids.Tanks tank = new PurityFluids.Tanks(TANK, new FluidResource[] {PurityFluids.impure()},
            new boolean[] {true}, new boolean[] {false}, this::setChanged);

    private int burnLeft;
    private int burnTotal;
    private int residueProgress;

    public ImpureReactorBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.IMPURE_REACTOR_BE.get(), pos, state, PowerLayouts.IMPURE_REACTOR, 0, MAX_OUTPUT);
    }

    public PurityFluids.Tanks getTank() {
        return this.tank;
    }

    /** FE de um combustível sólido (0 = não é combustível sólido). */
    public static int solidValue(ItemStack stack) {
        if (stack.is(Mistech.IMPURE_MAGIC_BLOCK_ITEM.get())) return 360_000;
        if (stack.is(Mistech.IMPURE_MAGIC_SHARD.get())) return 40_000;
        if (stack.is(MachineContent.IMPURE_MAGIC_DUST.get())) return 10_000;
        return 0;
    }

    public static boolean isFuel(ItemStack stack) {
        return solidValue(stack) > 0 || stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) || stack.is(Mistech.IMPURE_ESSENCE_BUCKET.get());
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack fuel = this.items.get(SLOT_FUEL);
        // frascos/baldes vão para o tanque
        if (!fuel.isEmpty() && (fuel.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) || fuel.is(Mistech.IMPURE_ESSENCE_BUCKET.get()))) {
            boolean bottle = fuel.is(Mistech.IMPURE_ESSENCE_BOTTLE.get());
            int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
            ItemStack empty = new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET);
            if (this.tank.space(0) >= amount && this.canOutput(empty)) {
                fuel.shrink(1);
                this.tank.add(0, amount);
                this.insertOutput(empty);
            }
        }

        int space = this.layout.capacity - this.storedEnergy();
        int status = STATUS_OK;
        int generated = 0;
        if (this.residueProgress >= RESIDUE_EVERY) {
            if (this.canOutput(new ItemStack(PurityContent.TAINTED_RESIDUE.get()))) {
                this.insertOutput(new ItemStack(PurityContent.TAINTED_RESIDUE.get()));
                this.residueProgress -= RESIDUE_EVERY;
            } else {
                status = STATUS_RESIDUE;
            }
        }
        if (status == STATUS_OK) {
            if (space <= 0) {
                status = STATUS_FULL;
            } else {
                if (this.burnLeft <= 0 && solidValue(fuel) > 0) {
                    this.burnLeft = this.burnTotal = solidValue(fuel);
                    fuel.shrink(1);
                    this.setChanged();
                }
                if (this.burnLeft > 0) {
                    generated = Math.min(Math.min(RATE, this.burnLeft), space);
                    this.burnLeft -= generated;
                } else {
                    int mb = Math.min(RATE / FE_PER_MB, Math.min(this.tank.amount(0), space / FE_PER_MB));
                    if (mb > 0 && this.tank.take(0, mb)) {
                        generated = mb * FE_PER_MB;
                    }
                }
                if (generated > 0) {
                    this.addEnergy(generated);
                    this.residueProgress += generated;
                } else {
                    status = STATUS_NO_FUEL;
                }
            }
        }
        this.maxProgress = Math.max(1, this.burnTotal);
        this.progress = this.burnLeft;
        this.info0 = generated;
        this.info1 = this.tank.amount(0);
        this.info2 = status;
        this.info3 = (int) (100L * Math.min(this.residueProgress, RESIDUE_EVERY) / RESIDUE_EVERY);
        this.pushEnergy(level, pos, MAX_OUTPUT);
        this.setLit(level, pos, state, generated > 0);
    }

    /** Clique com frasco/balde de Essência Impura enche o tanque. */
    public boolean interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        int in = held.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) ? PurityFluids.BOTTLE
                : held.is(Mistech.IMPURE_ESSENCE_BUCKET.get()) ? PurityFluids.BUCKET : 0;
        if (in <= 0 || this.tank.space(0) < in) {
            return false;
        }
        this.tank.add(0, in);
        PurityFluids.swap(player, hand, new ItemStack(in == PurityFluids.BOTTLE ? Items.GLASS_BOTTLE : Items.BUCKET));
        player.level().playSound(null, this.worldPosition, in == PurityFluids.BOTTLE ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY,
                SoundSource.BLOCKS, 1.0F, 0.8F);
        return true;
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.tank.setAmount(0, input.getIntOr("Essence", 0));
        this.burnLeft = input.getIntOr("BurnLeft", 0);
        this.burnTotal = input.getIntOr("BurnTotal", 0);
        this.residueProgress = input.getIntOr("Residue", 0);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Essence", this.tank.amount(0));
        output.putInt("BurnLeft", this.burnLeft);
        output.putInt("BurnTotal", this.burnTotal);
        output.putInt("Residue", this.residueProgress);
    }
}
