package com.mistech.mistechmod.power;

import com.mistech.mistechmod.beyond.PaleNetherContent;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.purity.block.PurityFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Caldeira Ígnea (Gerador de Blaze avançado): ferve <b>lava</b> do tanque ({@link #TANK} mB; balde, clique ou canos)
 * e gera {@link #BASE_RATE} FE/t gastando {@link #LAVA_PER_TICK} mB/t (um balde = 200.000 FE).
 * Catalisador (espaço à direita): Vara de Blaze 1.200 ticks, Pó de Blaze 600, Frasco de Essência Ígnea 2.400 → ×2 enquanto dura.
 * No Nether ×1,25 (máximo 1.000 FE/t). Buffer {@link #CAPACITY} FE; envia até {@link #MAX_OUTPUT} FE/t para cada bloco encostado.
 * info0 = FE/t; info1 = lava (mB); info2 = ticks de catalisador que faltam.
 */
public class IgneousBoilerBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 400_000;
    public static final int BASE_RATE = 400;
    public static final int MAX_OUTPUT = 2_000;
    public static final int TANK = 8_000;
    public static final int LAVA_PER_TICK = 2;
    public static final int SLOT_BUCKET = 0, SLOT_OUTPUT = 1, SLOT_CATALYST = 2;

    private final PurityFluids.Tanks tank = new PurityFluids.Tanks(TANK, new FluidResource[] {FluidResource.of(Fluids.LAVA)},
            new boolean[] {true}, new boolean[] {false}, this::setChanged);

    private int catalystLeft;
    private int catalystTotal;

    public IgneousBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.IGNEOUS_BOILER_BE.get(), pos, state, PowerLayouts.IGNEOUS_BOILER, 0, MAX_OUTPUT);
    }

    public PurityFluids.Tanks getTank() {
        return this.tank;
    }

    public static int catalystTicks(ItemStack stack) {
        if (stack.is(Items.BLAZE_ROD)) return 1_200;
        if (stack.is(Items.BLAZE_POWDER)) return 600;
        if (stack.is(PaleNetherContent.IGNEOUS_ESSENCE_BOTTLE.get())) return 2_400;
        return 0;
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        // os dois espaços aceitam balde de lava e catalisador (funis/canos podem encher qualquer um)
        for (int slot : new int[] {SLOT_BUCKET, SLOT_CATALYST}) {
            ItemStack bucket = this.items.get(slot);
            if (bucket.is(Items.LAVA_BUCKET) && this.tank.space(0) >= PurityFluids.BUCKET && this.canOutput(new ItemStack(Items.BUCKET))) {
                bucket.shrink(1);
                this.tank.add(0, PurityFluids.BUCKET);
                this.insertOutput(new ItemStack(Items.BUCKET));
            }
        }

        int space = this.layout.capacity - this.storedEnergy();
        int generated = 0;
        int rate = rate(level, this.catalystLeft > 0);
        if (space >= rate && this.tank.amount(0) >= LAVA_PER_TICK) {
            // o catalisador só começa a queimar quando a caldeira está fervendo
            ItemStack catalyst = catalystTicks(this.items.get(SLOT_CATALYST)) > 0 ? this.items.get(SLOT_CATALYST) : this.items.get(SLOT_BUCKET);
            if (this.catalystLeft <= 0 && catalystTicks(catalyst) > 0) {
                this.catalystLeft = this.catalystTotal = catalystTicks(catalyst);
                boolean bottle = catalyst.is(PaleNetherContent.IGNEOUS_ESSENCE_BOTTLE.get());
                catalyst.shrink(1);
                if (bottle) {
                    ItemStack glass = new ItemStack(Items.GLASS_BOTTLE);
                    if (this.canOutput(glass)) this.insertOutput(glass);
                }
                rate = rate(level, true);
                this.setChanged();
            }
            this.tank.take(0, LAVA_PER_TICK);
            generated = rate;
            this.addEnergy(generated);
            if (this.catalystLeft > 0) {
                this.catalystLeft--;
            }
        }
        this.maxProgress = Math.max(1, this.catalystTotal);
        this.progress = this.catalystLeft;
        this.info0 = generated;
        this.info1 = this.tank.amount(0);
        this.info2 = this.catalystLeft;
        this.pushEnergy(level, pos, MAX_OUTPUT);
        this.setLit(level, pos, state, generated > 0);
    }

    private static int rate(Level level, boolean catalyst) {
        double rate = BASE_RATE * (catalyst ? 2 : 1);
        if (level.dimension() == Level.NETHER) {
            rate *= 1.25;
        }
        return (int) rate;
    }

    /** Clique com balde de lava enche o tanque. */
    public boolean interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.LAVA_BUCKET) || this.tank.space(0) < PurityFluids.BUCKET) {
            return false;
        }
        this.tank.add(0, PurityFluids.BUCKET);
        PurityFluids.swap(player, hand, new ItemStack(Items.BUCKET));
        player.level().playSound(null, this.worldPosition, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.tank.setAmount(0, input.getIntOr("Lava", 0));
        this.catalystLeft = input.getIntOr("Catalyst", 0);
        this.catalystTotal = input.getIntOr("CatalystTotal", 0);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Lava", this.tank.amount(0));
        output.putInt("Catalyst", this.catalystLeft);
        output.putInt("CatalystTotal", this.catalystTotal);
    }
}
